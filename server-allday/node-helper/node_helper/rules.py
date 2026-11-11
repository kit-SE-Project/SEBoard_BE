import copy
import ipaddress

from .errors import HelperError

FAMILY = "inet"
TABLE = "allday"
DEFAULT_LEASE_SECONDS = 60
MAX_LEASE_SECONDS = 120


def meta(key):
    return {"meta": {"key": key}}


def payload(protocol, field):
    return {"payload": {"protocol": protocol, "field": field}}


def ct(key):
    return {"ct": {"key": key}}


def match(left, right, op="=="):
    return {"match": {"op": op, "left": left, "right": right}}


def is_dnat():
    return match(ct("status"), "dnat", "in")


def prefix(network):
    address, length = network.split("/")
    return {"prefix": {"addr": address, "len": int(length)}}


def base_objects(config):
    objects = [{"table": {"family": FAMILY, "name": TABLE}}]
    objects += [{"set": {"family": FAMILY, "table": TABLE, "name": "active_vm", "type": "ipv4_addr"}},
                {"set": {"family": FAMILY, "table": TABLE, "name": "access_lease", "type": "ipv4_addr",
                         "flags": ["timeout"], "timeout": DEFAULT_LEASE_SECONDS}},
                {"map": {"family": FAMILY, "table": TABLE, "name": "ingress_target", "type": "inet_service", "map": "ipv4_addr"}}]
    for name, kind, hook, priority in (("ingress", "nat", "prerouting", -100), ("egress", "nat", "postrouting", 100),
                                        ("input", "filter", "input", -10), ("forward", "filter", "forward", -10)):
        objects.append({"chain": {"family": FAMILY, "table": TABLE, "name": name, "type": kind,
                                  "hook": hook, "prio": priority, "policy": "accept"}})

    def rule(chain, name, *expressions):
        objects.append({"rule": {"family": FAMILY, "table": TABLE, "chain": chain,
                                 "comment": f"allday:{name}", "expr": list(expressions)}})

    incoming = [match(meta("iifname"), config.uplink), match(payload("ip", "daddr"), config.node_ip),
                match(payload("tcp", "dport"), config.ingress_port)]
    rule("ingress", "dnat", *incoming, {"dnat": {"family": "ip", "addr": {
        "map": {"key": payload("tcp", "dport"), "data": "@ingress_target"}}, "port": 22}})
    rule("egress", "internet-nat", match(payload("ip", "saddr"), prefix(str(config.vm_subnet))),
         match(meta("oifname"), config.uplink), {"masquerade": None})
    rule("input", "never-host-login", *incoming, {"drop": None})
    rule("input", "host-probe-reply", match(meta("iifname"), config.vm_bridge),
         match(ct("direction"), "reply"), match(ct("state"), {"set": ["established", "related"]}), {"accept": None})
    if str(config.vm_gateway) in config.nameservers:
        rule("input", "gateway-dns", match(meta("iifname"), config.vm_bridge),
             match(payload("ip", "saddr"), "@active_vm"), match(payload("ip", "daddr"), str(config.vm_gateway)),
             match(payload("ip", "saddr"), "@access_lease"),
             match(meta("l4proto"), {"set": ["tcp", "udp"]}), match(payload("th", "dport"), 53), {"accept": None})
    rule("input", "vm-host-isolation", match(meta("iifname"), config.vm_bridge), {"drop": None})
    rule("forward", "vm-no-ipv6-in", match(meta("iifname"), config.vm_bridge), match(meta("nfproto"), "ipv6"), {"drop": None})
    rule("forward", "vm-no-ipv6-out", match(meta("oifname"), config.vm_bridge), match(meta("nfproto"), "ipv6"), {"drop": None})
    rule("forward", "inactive-source", match(meta("iifname"), config.vm_bridge),
         match(payload("ip", "saddr"), "@active_vm", "!="), {"drop": None})
    rule("forward", "inactive-destination", match(meta("oifname"), config.vm_bridge),
         match(payload("ip", "daddr"), "@active_vm", "!="), {"drop": None})
    # Check on EVERY forwarded packet, before established/reply accepts. Kernel expiry
    # therefore blocks existing sessions too, without a running manager or helper.
    rule("forward", "expired-source", match(meta("iifname"), config.vm_bridge),
         match(payload("ip", "saddr"), "@access_lease", "!="), {"drop": None})
    rule("forward", "expired-destination", match(meta("oifname"), config.vm_bridge),
         match(payload("ip", "daddr"), "@access_lease", "!="), {"drop": None})
    rule("forward", "ssh-in", match(meta("iifname"), config.uplink), match(meta("oifname"), config.vm_bridge),
         is_dnat(), match(payload("tcp", "dport"), 22), {"accept": None})
    # Replies to incoming SSH may legitimately go to a private-address client or school SNAT.
    rule("forward", "ssh-reply", match(meta("iifname"), config.vm_bridge), match(meta("oifname"), config.uplink),
         match(ct("direction"), "reply"), is_dnat(), match(payload("tcp", "sport"), 22), {"accept": None})
    rule("forward", "internet-reply", match(meta("iifname"), config.uplink), match(meta("oifname"), config.vm_bridge),
         match(ct("direction"), "reply"), match(ct("state"), {"set": ["established", "related"]}), {"accept": None})
    # nftables emits a one-address anonymous set as a scalar when listing JSON.
    # Generate that same form so strict verification does not report false drift.
    dns_target = config.nameservers[0] if len(config.nameservers) == 1 else {"set": list(config.nameservers)}
    rule("forward", "approved-dns", match(meta("iifname"), config.vm_bridge), match(meta("oifname"), config.uplink),
         match(payload("ip", "daddr"), dns_target),
         match(meta("l4proto"), {"set": ["tcp", "udp"]}), match(payload("th", "dport"), 53), {"accept": None})
    for index, network in enumerate(config.blocked_networks):
        rule("forward", f"blocked-{index}", match(meta("iifname"), config.vm_bridge),
             match(payload("ip", "daddr"), prefix(network)), {"drop": None})
    rule("forward", "internet-out", match(meta("iifname"), config.vm_bridge), match(meta("oifname"), config.uplink), {"accept": None})
    rule("forward", "vm-out-deny", match(meta("iifname"), config.vm_bridge), {"drop": None})
    rule("forward", "vm-in-deny", match(meta("oifname"), config.vm_bridge), {"drop": None})
    return objects


def base_batch(config, exists):
    commands = []
    if exists:
        commands.append({"delete": {"table": {"family": FAMILY, "name": TABLE}}})
    commands += [{"add": item} for item in base_objects(config)]
    return {"nftables": commands}


def lease_element(address, seconds):
    return {"elem": {"val": address, "timeout": seconds}}


def lease_batch(address, seconds):
    return {"nftables": [
        {"delete": {"element": {"family": FAMILY, "table": TABLE, "name": "access_lease", "elem": [address]}}},
        {"add": {"element": {"family": FAMILY, "table": TABLE, "name": "access_lease",
                            "elem": [lease_element(address, seconds)]}}},
    ]}


def target_batch(config, address=None, lease_seconds=DEFAULT_LEASE_SECONDS):
    commands = [{"flush": {kind: {"family": FAMILY, "table": TABLE, "name": name}}}
                for kind, name in (("map", "ingress_target"), ("set", "active_vm"), ("set", "access_lease"))]
    if address:
        for name, elements in (("ingress_target", [[config.ingress_port, address]]), ("active_vm", [address])):
            commands.append({"add": {"element": {"family": FAMILY, "table": TABLE, "name": name, "elem": elements}}})
        commands.append(lease_batch(address, lease_seconds)["nftables"][1])
    return {"nftables": commands}


def normal(value):
    if isinstance(value, dict):
        if "prefix" in value and value["prefix"]["len"] in {32, 128}:
            return value["prefix"]["addr"]
        result = {k: normal(v) for k, v in value.items() if k not in {"handle", "index"}}
        if "match" in result:
            right = result["match"]["right"]
            if isinstance(right, dict) and "set" in right:
                right["set"] = sorted(right["set"], key=str)
        return result
    if isinstance(value, list):
        return [normal(x) for x in value]
    return value


def inspect_rules(config, listing):
    try:
        items = listing["nftables"]
        require_objects = [x for x in items if "metainfo" not in x]
        objects = base_objects(config)
        found = {}
        for item in require_objects:
            kind, value = next(iter(item.items()))
            if kind == "rule":
                key = (kind, value.get("comment"))
            else:
                key = (kind, value.get("name"))
            if key in found:
                raise ValueError("Duplicate object")
            found[key] = value
        if len(found) != len(objects):
            raise ValueError("Unexpected rule count")
        for item in objects:
            kind, expected = next(iter(item.items()))
            key = (kind, expected.get("comment") if kind == "rule" else expected.get("name"))
            actual = found[key]
            for field, value in expected.items():
                if normal(actual.get(field)) != normal(value):
                    raise ValueError("Rules differ")
        mapping = found[("map", "ingress_target")].get("elem", [])
        active = found[("set", "active_vm")].get("elem", [])
        lease = found[("set", "access_lease")].get("elem", [])
        active = [x.get("elem", {}).get("val") if isinstance(x, dict) and "elem" in x else x for x in active]
        mapping = [x.get("elem", {}).get("val") if isinstance(x, dict) and "elem" in x else x for x in mapping]
        if len(mapping) > 1 or len(active) > 1:
            raise ValueError("Multiple active VM targets")
        if mapping:
            if not isinstance(mapping[0], list) or len(mapping[0]) != 2 or mapping[0][0] != config.ingress_port:
                raise ValueError("Invalid ingress map")
            target = mapping[0][1]
            address = ipaddress.IPv4Address(target)
            if address not in config.vm_subnet or address in {config.vm_gateway, config.vm_subnet.network_address, config.vm_subnet.broadcast_address}:
                raise ValueError("Target outside the usable VM subnet")
            if active != [target]:
                raise ValueError("Map and guard disagree")
        else:
            target = None
            if active:
                raise ValueError("Guard remains open")
        if len(lease) > 1:
            raise ValueError("Multiple access leases")
        remaining = 0
        if lease:
            timed = lease[0]["elem"]
            # nft omits per-element timeout when it equals the set default.
            timeout = timed.get("timeout", found[("set", "access_lease")]["timeout"])
            remaining = timed.get("expires", timeout)
            if timed["val"] != target or type(timeout) not in (int, float) or not 0 < timeout <= MAX_LEASE_SECONDS:
                raise ValueError("Invalid access lease")
            if type(remaining) not in (int, float) or not 0 <= remaining <= timeout:
                raise ValueError("Invalid lease expiry")
        return {"target": target, "ingressRuleCount": 1, "mappingCount": len(mapping),
                "guardCount": len(active), "leaseValid": bool(lease and remaining > 0),
                "leaseRemainingSeconds": remaining, "ruleCount": sum("rule" in x for x in objects)}
    except (KeyError, ValueError, TypeError, StopIteration):
        raise HelperError("RULES_DRIFT", "The managed nftables table differs from the expected rules.") from None


def simulated_listing(config, address=None, lease_seconds=DEFAULT_LEASE_SECONDS):
    items = copy.deepcopy(base_objects(config))
    for item in items:
        if "map" in item:
            item["map"]["elem"] = [[config.ingress_port, address]] if address else []
        if "set" in item:
            item["set"]["elem"] = ([lease_element(address, lease_seconds)] if item["set"]["name"] == "access_lease"
                                    else [address]) if address else []
    return {"nftables": items}
