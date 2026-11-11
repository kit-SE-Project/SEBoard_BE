#!/usr/bin/env python3
"""Install root-owned files on a test Proxmox node; firewall activation is a separate step."""
import argparse
import base64
import ipaddress
import json
import os
from pathlib import Path
import pwd
import shutil
import socket
import stat
import subprocess
import sys
import tempfile

PROJECT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PROJECT))
from node_helper.config import Config
from node_helper.errors import HelperError
from node_helper.files import atomic_write, check_path

CODE_DIR = Path("/opt/server-allday/node-helper")
CONFIG_DIR = Path("/etc/server-allday/node-helper")
LOGIN_DIR = Path("/var/lib/server-allday/helper-login")
SUDOERS = b'allday-helper ALL=(root) NOPASSWD: /usr/local/sbin/allday-node-helper ""\n'


def command(args):
    return subprocess.run(args, check=True, capture_output=True, text=True, timeout=20,
                          env={"PATH": "/usr/sbin:/usr/bin:/sbin:/bin", "LC_ALL": "C"})


def directory(path, mode=0o755):
    check_path(path, True)
    created = not path.exists()
    path.mkdir(parents=True, exist_ok=True)
    if created:
        os.chmod(path, mode)
    check_path(path, True)


def copy_file(source, target, mode=0o644):
    if source.is_symlink():
        raise HelperError("UNSAFE_PATH", "Installation source files must not be symbolic links.")
    directory(target.parent)
    atomic_write(target, source.read_bytes(), production=True, mode=mode)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", required=True)
    parser.add_argument("--public-key", required=True)
    parser.add_argument("--manager-ip", required=True)
    parser.add_argument("--check-only", action="store_true")
    args = parser.parse_args()
    os.umask(0o077)
    config_path = Path(args.config).resolve()
    config = Config.load(config_path)
    if not config.production:
        raise HelperError("INVALID_CONFIG", "Installation requires a system-mode config.")
    manager = ipaddress.IPv4Address(args.manager_ip)
    if str(manager) == config.node_ip or manager in config.vm_subnet:
        raise HelperError("INVALID_CONFIG", "The manager must be a separate host outside the VM subnet.")
    if config.node_name != socket.gethostname().split(".")[0]:
        raise HelperError("INVALID_CONFIG", "nodeName must match this Proxmox node's actual name.")
    for name in ("python3", "nft", "conntrack", "pvesh", "sudo", "visudo", "ssh-keygen", "ip", "ss"):
        if shutil.which(name) is None:
            raise HelperError("HOST_TOOL_UNAVAILABLE", f"Install the required tool first: {name}")
    tables = json.loads(command(["/usr/sbin/nft", "-j", "list", "tables"]).stdout)["nftables"]
    if any(x.get("table", {}).get("name") == "allday" and x["table"].get("family") != "inet" for x in tables):
        raise HelperError("LEGACY_FIREWALL_PRESENT", "Review and remove legacy ip/ip6 allday rules before using the inet helper.")
    for interface, address in ((config.uplink, config.node_ip), (config.vm_bridge, str(config.vm_gateway))):
        interfaces = json.loads(command(["/usr/sbin/ip", "-j", "address", "show", "dev", interface]).stdout)
        matches = [x for item in interfaces for x in item.get("addr_info", []) if x.get("local") == address]
        if not matches:
            raise HelperError("INVALID_CONFIG", "Configured bridge addresses are not present on this host.")
        if interface == config.vm_bridge and matches[0].get("prefixlen") != config.vm_subnet.prefixlen:
            raise HelperError("INVALID_CONFIG", "VM bridge prefix length differs from vmSubnet.")
    if Path("/proc/sys/net/ipv4/ip_forward").read_text().strip() != "1":
        raise HelperError("INVALID_CONFIG", "Enable IPv4 forwarding as described in INSTALL.md first.")
    storage = json.loads(command(["/usr/bin/pvesh", "get", f"/storage/{config.snippet_storage}", "--output-format", "json"]).stdout)
    if storage.get("type") != "dir" or "snippets" not in storage.get("content", "").split(","):
        raise HelperError("INVALID_CONFIG", "Use a local directory storage with snippets content enabled.")
    if Path(storage["path"]).absolute() / "snippets" != config.snippet_dir:
        raise HelperError("INVALID_CONFIG", "snippetDirectory differs from the actual Proxmox storage path.")
    listeners = command(["/usr/bin/ss", "-H", "-lnt"]).stdout.splitlines()
    if any(line.split()[3].rsplit(":", 1)[-1] == str(config.ingress_port) for line in listeners if len(line.split()) > 3):
        raise HelperError("INGRESS_PORT_IN_USE", "A host service is listening on the student ingress port. Preserve a separate management SSH path first.")
    key_path = Path(args.public_key).resolve()
    lines = key_path.read_text().strip().splitlines()
    if len(lines) != 1:
        raise HelperError("INVALID_KEY", "Supply exactly one manager public key.")
    parts = lines[0].split()
    if len(parts) < 2 or parts[0] != "ssh-ed25519":
        raise HelperError("INVALID_KEY", "Supply a plain ssh-ed25519 public key without authorized_keys options.")
    base64.b64decode(parts[1], validate=True)
    command(["/usr/bin/ssh-keygen", "-l", "-f", str(key_path)])
    with tempfile.NamedTemporaryFile() as stream:
        stream.write(SUDOERS)
        stream.flush()
        command(["/usr/sbin/visudo", "-c", "-f", stream.name])
    try:
        account = pwd.getpwnam("allday-helper")
        if account.pw_uid == 0 or account.pw_uid >= 1000 or account.pw_dir != str(LOGIN_DIR) or account.pw_shell != "/bin/sh":
            raise HelperError("ACCOUNT_CONFLICT", "An unrelated account already uses the allday-helper name.")
    except KeyError:
        account = None
    for target in (CODE_DIR, CONFIG_DIR, LOGIN_DIR, config.state_dir, config.snippet_dir):
        check_path(target, True)
    if args.check_only:
        print(json.dumps({"ok": True, "installed": False, "serverId": config.server_id, "mode": "preflight"}))
        return
    if account is None:
        command(["/usr/sbin/useradd", "--system", "--home-dir", str(LOGIN_DIR), "--shell", "/bin/sh", "allday-helper"])
    command(["/usr/sbin/usermod", "--lock", "allday-helper"])
    directory(CODE_DIR)
    for source in (PROJECT / "node_helper").glob("*.py"):
        copy_file(source, CODE_DIR / "node_helper" / source.name)
    copy_file(PROJECT / "run_helper.py", CODE_DIR / "run_helper.py")
    directory(CONFIG_DIR, 0o700)
    atomic_write(CONFIG_DIR / "config.json", config_path.read_bytes(), True)
    # sshd reads authorized_keys as allday-helper. With umask 077, creating the
    # state directory first used to leave its shared parent at root-only 0700.
    # Permit traversal only; keep the state directory and its files private.
    directory(LOGIN_DIR.parent, 0o711)
    os.chmod(LOGIN_DIR.parent, stat.S_IMODE(LOGIN_DIR.parent.stat().st_mode) | 0o011)
    directory(config.state_dir, 0o700)
    config.snippet_dir.mkdir(parents=True, exist_ok=True)
    check_path(config.snippet_dir, True)
    directory(LOGIN_DIR)
    directory(LOGIN_DIR / ".ssh")
    key = f'from="{manager}",restrict,command="/usr/bin/sudo -n /usr/local/sbin/allday-node-helper" {parts[0]} {parts[1]} server-allday-manager\n'
    atomic_write(LOGIN_DIR / ".ssh/authorized_keys", key.encode(), True, mode=0o644)
    copy_file(PROJECT / "deploy/allday-node-helper", Path("/usr/local/sbin/allday-node-helper"), 0o755)
    atomic_write(Path("/etc/sudoers.d/server-allday-helper"), SUDOERS, True, mode=0o440)
    copy_file(PROJECT / "deploy/allday-node-helper-bootstrap.service", Path("/etc/systemd/system/allday-node-helper-bootstrap.service"))
    command(["/usr/bin/systemctl", "daemon-reload"])
    print(json.dumps({"ok": True, "installed": True, "firewallActivated": False,
                      "next": "Follow INSTALL.md to initialize closed ingress and enable the boot service."}))


if __name__ == "__main__":
    try:
        main()
    except (HelperError, OSError, ValueError, subprocess.SubprocessError) as error:
        print(json.dumps({"ok": False, "message": str(error)}))
        raise SystemExit(1)
