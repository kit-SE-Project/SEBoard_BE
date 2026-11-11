import json
import os
from pathlib import Path
import socket
import subprocess
import time
import xml.etree.ElementTree as ET

from .errors import HelperError
from .files import check_path, write_json
from .rules import DEFAULT_LEASE_SECONDS, FAMILY, TABLE, base_batch, inspect_rules, lease_batch, simulated_listing, target_batch


class SystemHost:
    def __init__(self, config):
        self.config = config

    def run(self, executable, args, data=None, allowed=(0,)):
        try:
            result = subprocess.run([executable, *args], input=data, capture_output=True, text=True,
                                    encoding="utf-8", timeout=self.config.command_timeout, shell=False,
                                    env={"PATH": "/usr/sbin:/usr/bin:/sbin:/bin", "LC_ALL": "C", "LANG": "C"})
        except subprocess.TimeoutExpired:
            raise HelperError("HOST_TIMEOUT", "A host command timed out; its outcome must be observed before retrying.") from None
        except OSError:
            raise HelperError("HOST_TOOL_UNAVAILABLE", "A required host tool could not be executed.") from None
        if result.returncode not in allowed:
            # stdout/stderr and input can contain secrets or privileged host details.
            raise HelperError("HOST_COMMAND_FAILED", "A host command failed.", {"tool": Path(executable).name, "exitCode": result.returncode})
        return result

    def boot_id(self):
        return Path("/proc/sys/kernel/random/boot_id").read_text().strip()

    def nft_listing(self):
        result = self.run("/usr/sbin/nft", ["-j", "list", "tables"])
        try:
            exists = any(x.get("table", {}).get("family") == FAMILY and x.get("table", {}).get("name") == TABLE
                         for x in json.loads(result.stdout)["nftables"])
            if not exists:
                return None
            return json.loads(self.run("/usr/sbin/nft", ["-j", "list", "table", FAMILY, TABLE]).stdout)
        except (ValueError, KeyError, TypeError):
            raise HelperError("HOST_RESPONSE_INVALID", "nft returned an invalid response.") from None

    def apply_batch(self, batch):
        text = json.dumps(batch)
        self.run("/usr/sbin/nft", ["-j", "-c", "-f", "-"], text)
        self.run("/usr/sbin/nft", ["-j", "-f", "-"], text)

    def bootstrap(self):
        self.apply_batch(base_batch(self.config, self.nft_listing() is not None))
        return self.ingress()

    def ingress(self):
        listing = self.nft_listing()
        if listing is None:
            raise HelperError("BOOTSTRAP_REQUIRED", "The managed firewall table is missing.")
        return inspect_rules(self.config, listing)

    def set_target(self, address=None, lease_seconds=DEFAULT_LEASE_SECONDS):
        self.apply_batch(target_batch(self.config, address, lease_seconds))
        observed = self.ingress()
        if observed["target"] != address:
            raise HelperError("APPLY_VERIFY_FAILED", "The applied ingress target does not match the request.")
        return observed

    def renew_lease(self, address, seconds):
        self.apply_batch(lease_batch(address, seconds))
        observed = self.ingress()
        if observed["target"] != address or not observed["leaseValid"]:
            raise HelperError("APPLY_VERIFY_FAILED", "The access lease could not be verified.")
        return observed

    def pve_get(self, path, *args):
        result = self.run("/usr/bin/pvesh", ["get", path, "--output-format", "json", *args])
        try:
            return json.loads(result.stdout)
        except ValueError:
            raise HelperError("HOST_RESPONSE_INVALID", "Proxmox returned an invalid JSON response.") from None

    def vms(self):
        items = self.pve_get(f"/nodes/{self.config.node_name}/qemu")
        if not isinstance(items, list) or any(not isinstance(x, dict) or type(x.get("vmid")) is not int or x.get("status") not in {"running", "stopped", "paused", "suspended"} for x in items):
            raise HelperError("HOST_RESPONSE_INVALID", "VM state could not be established.")
        if len({x["vmid"] for x in items}) != len(items):
            raise HelperError("HOST_RESPONSE_INVALID", "Duplicate VM IDs were observed.")
        return items

    def tasks(self):
        items = self.pve_get(f"/nodes/{self.config.node_name}/tasks", "--source", "active", "--limit", "1000")
        if not isinstance(items, list) or any(not isinstance(x, dict) or not x.get("upid") for x in items):
            raise HelperError("HOST_RESPONSE_INVALID", "Active tasks could not be established.")
        return [x for x in items if x.get("status") == "RUNNING" or not x.get("endtime") and x.get("status") not in {"OK", "ERROR", "WARNINGS"}]

    def volumes(self, storage):
        items = self.pve_get(f"/nodes/{self.config.node_name}/storage/{storage}/content")
        if not isinstance(items, list) or any(not isinstance(x, dict) or not isinstance(x.get("volid"), str) for x in items):
            raise HelperError("HOST_RESPONSE_INVALID", "Storage contents could not be established.")
        return items

    def ssh_ready(self, address):
        try:
            with socket.create_connection((address, 22), timeout=3) as stream:
                stream.settimeout(3)
                # A TCP listener alone is insufficient: require an SSH protocol identification.
                banner = stream.recv(255)
                return banner.startswith(b"SSH-2.0-") or banner.startswith(b"SSH-1.99-")
        except OSError:
            return False

    def connection_args(self, address, zone):
        return ["-p", "tcp", "--orig-dst", self.config.node_ip,
                "--dport", str(self.config.ingress_port), "--reply-src", address,
                "--reply-port-src", "22", "--zone", str(zone)]

    def connections(self, address):
        count = 0
        for zone in self.config.conntrack_zones:
            result = self.run("/usr/sbin/conntrack", ["-L", "-f", "ipv4", *self.connection_args(address, zone), "-o", "xml"])
            # conntrack emits no XML at all for an empty selection; require its explicit zero summary.
            if not result.stdout.strip() and "0 flow entries have been shown." in result.stderr:
                continue
            try:
                root = ET.fromstring(result.stdout)
                if root.tag.split("}")[-1] != "conntrack":
                    raise ValueError()
                count += sum(element.tag.split("}")[-1] == "flow" for element in root.iter())
            except (ET.ParseError, ValueError):
                raise HelperError("HOST_RESPONSE_INVALID", "Connection state could not be established.") from None
        return count

    def clear_connections(self, address):
        for zone in self.config.conntrack_zones:
            result = self.run("/usr/sbin/conntrack", ["-D", *self.connection_args(address, zone)], allowed=(0, 1))
            if result.returncode == 1 and "0 flow entries have been deleted" not in result.stderr:
                raise HelperError("CONNECTION_CLEANUP_FAILED", "Old connections could not be deleted.")
        if self.connections(address):
            raise HelperError("CONNECTION_CLEANUP_FAILED", "Old connection entries remain.")


class SimulationHost:
    """File-backed fake hardware; never executes system commands or opens a VM socket."""

    def __init__(self, config):
        self.config = config
        self.path = check_path(config.state_dir / "simulation.json")
        if not self.path.exists():
            write_json(self.path, {"bootId": "simulation-boot-1", "nft": None, "vms": [], "tasks": [],
                                   "volumes": {x: [] for x in config.storage_ids}, "sshReady": [], "connections": {}})

    def read(self):
        return json.loads(self.path.read_text())

    def change(self, **values):
        state = self.read()
        state.update(values)
        write_json(self.path, state)

    def boot_id(self):
        return self.read()["bootId"]

    def bootstrap(self):
        self.change(nft=simulated_listing(self.config))
        return self.ingress()

    def ingress(self):
        state = self.read()
        listing = state["nft"]
        if listing is None:
            raise HelperError("BOOTSTRAP_REQUIRED", "The simulated firewall has not been bootstrapped.")
        deadline = state.get("leaseDeadline")
        if deadline is not None:
            for item in listing["nftables"]:
                if item.get("set", {}).get("name") == "access_lease":
                    remaining = max(0, deadline - time.monotonic())
                    if not remaining:
                        item["set"]["elem"] = []
                    elif item["set"].get("elem"):
                        item["set"]["elem"][0]["elem"]["expires"] = remaining
        return inspect_rules(self.config, listing)

    def set_target(self, address=None, lease_seconds=DEFAULT_LEASE_SECONDS):
        self.change(nft=simulated_listing(self.config, address, lease_seconds),
                    leaseDeadline=time.monotonic() + lease_seconds if address else None)
        return self.ingress()

    def renew_lease(self, address, seconds):
        return self.set_target(address, seconds)

    def vms(self):
        return self.read()["vms"]

    def tasks(self):
        return self.read()["tasks"]

    def volumes(self, storage):
        return self.read()["volumes"][storage]

    def ssh_ready(self, address):
        return address in self.read()["sshReady"]

    def connections(self, address):
        return self.read()["connections"].get(address, 0)

    def clear_connections(self, address):
        values = self.read()["connections"]
        values[address] = 0
        self.change(connections=values)
