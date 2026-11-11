from dataclasses import dataclass
import ipaddress
import json
import os
from pathlib import Path
import re

from .errors import HelperError
from .files import check_path
from .validation import fields, integer, ipv4, require


@dataclass(frozen=True)
class Config:
    mode: str
    server_id: int
    node_name: str
    node_ip: str
    ingress_port: int
    uplink: str
    vm_bridge: str
    vm_subnet: ipaddress.IPv4Network
    vm_gateway: ipaddress.IPv4Address
    vm_interface: str
    nameservers: tuple
    blocked_networks: tuple
    storage_ids: tuple
    snippet_storage: str
    snippet_dir: Path
    state_dir: Path
    vm_id_min: int
    vm_id_max: int
    conntrack_zones: tuple
    command_timeout: int = 15
    lock_timeout: int = 20

    @property
    def production(self):
        return self.mode == "system"

    @classmethod
    def load(cls, filename):
        filename = Path(filename).absolute()
        raw = json.loads(filename.read_text(encoding="utf-8"))
        fields(raw, {"mode", "serverId", "nodeName", "nodeIp", "nodeIngressPort", "uplinkBridge",
                     "vmBridge", "vmSubnet", "vmGateway", "vmInterface", "nameservers",
                     "blockedNetworks", "storageIds", "snippetStorage", "snippetDirectory",
                     "stateDirectory", "vmIdMin", "vmIdMax", "conntrackZones"},
               {"commandTimeoutSeconds", "lockTimeoutSeconds"})
        require(raw["mode"] in {"simulation", "system"}, "Invalid mode.", "INVALID_CONFIG")
        production = raw["mode"] == "system"
        if production:
            require(os.name == "posix" and hasattr(os, "geteuid") and os.geteuid() == 0,
                    "System mode requires root on Linux.", "INVALID_CONFIG")
            require(Path("/etc/pve").is_dir(), "System mode must run on a Proxmox host.", "INVALID_CONFIG")
            check_path(filename, True)
        for key in ("nodeName", "uplinkBridge", "vmBridge", "vmInterface", "snippetStorage"):
            require(isinstance(raw[key], str) and re.fullmatch(r"[A-Za-z0-9_-]{1,32}", raw[key]), f"Invalid {key}.")
        require(raw["uplinkBridge"] != raw["vmBridge"], "School and VM bridges must be different.")
        subnet = ipaddress.IPv4Network(raw["vmSubnet"], strict=True)
        require(any(subnet.subnet_of(ipaddress.IPv4Network(x)) for x in ("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16")),
                "VM subnet must be an RFC1918 private network.")
        gateway = ipv4(raw["vmGateway"])
        require(gateway in subnet and gateway not in {subnet.network_address, subnet.broadcast_address}, "Invalid VM gateway.")
        node_ip = ipv4(raw["nodeIp"])
        require(node_ip not in subnet, "VM subnet overlaps the node ingress address.")
        require(isinstance(raw["nameservers"], list) and 1 <= len(raw["nameservers"]) <= 4, "Invalid nameservers.")
        dns = tuple(str(ipv4(x)) for x in raw["nameservers"])
        required_blocks = ["10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "127.0.0.0/8", "169.254.0.0/16", "224.0.0.0/4", "0.0.0.0/8"]
        require(isinstance(raw["blockedNetworks"], list), "Invalid blockedNetworks.")
        blocked = tuple(dict.fromkeys(str(ipaddress.IPv4Network(x, strict=True)) for x in [*required_blocks, *raw["blockedNetworks"], f"{node_ip}/32"]))
        require(isinstance(raw["storageIds"], list) and 1 <= len(raw["storageIds"]) <= 16, "Invalid storageIds.")
        for storage in raw["storageIds"]:
            require(isinstance(storage, str) and re.fullmatch(r"[A-Za-z0-9_-]{1,32}", storage), "Invalid storage ID.")
        require(raw["snippetStorage"] in raw["storageIds"], "Snippet storage must be in storageIds.")
        zones = raw["conntrackZones"]
        require(isinstance(zones, list) and 1 <= len(zones) <= 16, "Invalid conntrackZones.")
        zones = tuple(integer(x, "conntrack zone", 0, 65535) for x in zones)
        paths = []
        for name in ("snippetDirectory", "stateDirectory"):
            path = Path(raw[name])
            if production:
                require(path.is_absolute(), "Production paths must be absolute.")
            elif not path.is_absolute():
                path = filename.parent / path
            paths.append(check_path(path, production))
        if production:
            require(paths[1] == Path("/var/lib/server-allday/node-helper"), "Use the dedicated production state directory.")
            require(paths[0].name == "snippets", "Production snippetDirectory must be a snippets directory.")
        require(paths[0] != paths[1] and paths[0] not in paths[1].parents and paths[1] not in paths[0].parents,
                "Snippet and state paths must not overlap.")
        lower = integer(raw["vmIdMin"], "vmIdMin", 100, 999999999)
        upper = integer(raw["vmIdMax"], "vmIdMax", lower, 999999999)
        return cls(raw["mode"], integer(raw["serverId"], "serverId", 1), raw["nodeName"], str(node_ip),
                   integer(raw["nodeIngressPort"], "nodeIngressPort", 1024, 65535), raw["uplinkBridge"], raw["vmBridge"],
                   subnet, gateway, raw["vmInterface"], dns, blocked, tuple(raw["storageIds"]), raw["snippetStorage"],
                   paths[0], paths[1], lower, upper, zones,
                   integer(raw.get("commandTimeoutSeconds", 15), "command timeout", 1, 60),
                   integer(raw.get("lockTimeoutSeconds", 20), "lock timeout", 1, 60))
