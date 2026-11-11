import ipaddress
import json
import re

from .errors import HelperError

MAX_REQUEST_BYTES = 65536
OPERATIONS = {
    "REGISTER_RESOURCE", "PUT_SNIPPET", "REMOVE_SNIPPET", "ADVANCE_GENERATION",
    "APPLY_INGRESS", "DISABLE_INGRESS", "VERIFY_CLEANUP", "RELEASE_RESOURCE", "READ_STATUS",
    "RENEW_INGRESS",
}
READ_OPERATIONS = {"READ_STATUS", "VERIFY_CLEANUP"}


def require(condition, message, code="INVALID_REQUEST"):
    if not condition:
        raise HelperError(code, message)


def fields(value, required, optional=()):
    require(isinstance(value, dict), "Expected a JSON object.")
    require(set(required) <= value.keys(), "Required fields are missing.")
    require(value.keys() <= set(required) | set(optional), "Unknown fields are not allowed.")


def integer(value, name, minimum=0, maximum=2**63 - 1):
    require(type(value) is int and minimum <= value <= maximum, f"Invalid {name}.")
    return value


def identifier(value, name, pattern=r"[A-Za-z0-9][A-Za-z0-9_.:-]{0,95}"):
    require(isinstance(value, str) and re.fullmatch(pattern, value) is not None, f"Invalid {name}.")
    return value


def ipv4(value):
    try:
        return ipaddress.IPv4Address(value)
    except (ValueError, TypeError, ipaddress.AddressValueError):
        raise HelperError("INVALID_REQUEST", "Expected an IPv4 address.") from None


def unique_object(pairs):
    result = {}
    for name, value in pairs:
        require(name not in result, "Duplicate JSON fields are not allowed.")
        result[name] = value
    return result


def parse_request(data):
    require(len(data) <= MAX_REQUEST_BYTES, "Request is too large.")
    try:
        return json.loads(data.decode("utf-8"), object_pairs_hook=unique_object,
                          parse_constant=lambda _: require(False, "Non-finite JSON numbers are not allowed."))
    except (UnicodeDecodeError, json.JSONDecodeError):
        raise HelperError("INVALID_REQUEST", "Expected one UTF-8 JSON document.") from None


def validate_request(request, config):
    fields(request, {"schemaVersion", "commandId", "serverId", "operation", "payload"}, {"generation", "controlRevision"})
    require(type(request["schemaVersion"]) is int and request["schemaVersion"] == 1, "Unsupported schemaVersion.")
    identifier(request["commandId"], "commandId")
    integer(request["serverId"], "serverId", 1)
    require(request["serverId"] == config.server_id, "This command belongs to another server.", "WRONG_SERVER")
    op = request["operation"]
    if "controlRevision" in request:
        integer(request["controlRevision"], "controlRevision")
    require(isinstance(op, str) and op in OPERATIONS, "Unsupported operation.")
    if op not in READ_OPERATIONS:
        integer(request.get("generation"), "generation")
    elif "generation" in request:
        integer(request["generation"], "generation")
    p = request["payload"]
    if op == "REGISTER_RESOURCE":
        fields(p, {"rentalId", "vmId", "vmIp", "volumeIds"})
        rental_id(p)
        integer(p["vmId"], "vmId", config.vm_id_min, config.vm_id_max)
        address = ipv4(p["vmIp"])
        require(address in config.vm_subnet and address not in {
            config.vm_subnet.network_address, config.vm_subnet.broadcast_address, config.vm_gateway,
        }, "VM IP is outside the usable node subnet.")
        require(isinstance(p["volumeIds"], list) and len(p["volumeIds"]) <= 64, "Invalid volumeIds.")
        require(len(set(map(str, p["volumeIds"]))) == len(p["volumeIds"]), "Duplicate volumeIds.")
        for volume in p["volumeIds"]:
            require(isinstance(volume, str) and re.fullmatch(r"[A-Za-z0-9_-]+:[A-Za-z0-9_./-]{1,240}", volume), "Invalid volume ID.")
            require(".." not in volume and volume.split(":", 1)[0] in config.storage_ids, "Volume storage is not allowed.")
    elif op == "PUT_SNIPPET":
        fields(p, {"rentalId", "username", "passwordHash", "hostname"})
        rental_id(p)
        identifier(p["username"], "username", r"[a-z_][a-z0-9_-]{0,31}")
        require(p["username"] not in {"root", "daemon", "nobody", "allday-helper"}, "Reserved username.")
        identifier(p["hostname"], "hostname", r"[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")
        require(isinstance(p["passwordHash"], str) and re.fullmatch(
            r"\$6\$(?:rounds=[0-9]{4,9}\$)?[./A-Za-z0-9]{1,16}\$[./A-Za-z0-9]{86}", p["passwordHash"]),
            "passwordHash must be a SHA-512 crypt hash, never a plaintext password.")
    elif op == "ADVANCE_GENERATION":
        fields(p, {"rentalId", "expectedGeneration"})
        rental_id(p)
        integer(p["expectedGeneration"], "expectedGeneration")
        require(request["generation"] == p["expectedGeneration"] + 1, "Generation must advance by exactly one.")
    elif op in {"APPLY_INGRESS", "RENEW_INGRESS"}:
        fields(p, {"rentalId"}, {"leaseSeconds"})
        rental_id(p)
        integer(p.get("leaseSeconds", 60), "leaseSeconds", 5, 120)
    elif op in {"REMOVE_SNIPPET", "VERIFY_CLEANUP", "RELEASE_RESOURCE"}:
        fields(p, {"rentalId"})
        rental_id(p)
    else:
        fields(p, set())
    return request


def rental_id(payload):
    return integer(payload["rentalId"], "rentalId", 1)

