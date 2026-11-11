"""Translate manager intents without confusing their revisions with helper epochs."""
import ipaddress
import json
from pathlib import Path
import subprocess

from node_helper.errors import HelperError
from node_helper.validation import fields, identifier, integer, parse_request


class SshTransport:
    def __init__(self, host, key_file, known_hosts_file, port=22, timeout=45):
        self.host = str(ipaddress.ip_address(host))
        self.port = integer(port, "management SSH port", 1, 65535)
        self.key = str(Path(key_file).resolve(strict=True))
        self.known_hosts = str(Path(known_hosts_file).resolve(strict=True))
        self.timeout = integer(timeout, "SSH timeout", 1, 120)

    def __call__(self, request):
        args = ["ssh", "-T", "-p", str(self.port), "-i", self.key,
                "-o", "BatchMode=yes", "-o", "IdentitiesOnly=yes", "-o", "StrictHostKeyChecking=yes",
                "-o", f"UserKnownHostsFile={self.known_hosts}", "-o", "ConnectTimeout=5",
                "-o", "ServerAliveInterval=10", "-o", "ServerAliveCountMax=2",
                f"allday-helper@{self.host}"]
        try:
            result = subprocess.run(args, input=json.dumps(request).encode("utf-8"),
                                    capture_output=True, timeout=self.timeout, shell=False)
        except (OSError, subprocess.TimeoutExpired):
            raise HelperError("TRANSPORT_UNCERTAIN", "SSH failed or timed out. Check actual state; do not blindly repeat a VM operation.") from None
        if result.returncode not in (0, 1):
            raise HelperError("TRANSPORT_UNCERTAIN", "SSH transport did not return a helper result.")
        response = parse_request(result.stdout)
        if not isinstance(response, dict) or response.get("commandId") != request["commandId"] or response.get("serverId") != request["serverId"]:
            raise HelperError("TRANSPORT_RESPONSE_INVALID", "The helper response does not match this request.")
        if (response.get("mode") != "system" or response.get("schemaVersion") != 1
                or response.get("operation") != request["operation"] or type(response.get("ok")) is not bool
                or not isinstance(response.get("code"), str)
                or result.returncode != (0 if response["ok"] else 1)):
            raise HelperError("TRANSPORT_RESPONSE_INVALID", "The helper response schema or exit status is invalid; simulation is not production.")
        return response


class ManagerAdapter:
    """Use one serialized queue per server, including PVE tasks and heartbeats.

    helperGeneration MUST come from the saved claim result for THIS workflow,
    never from a fresh check just to make an old open/close succeed.
    """
    def __init__(self, server_id, transport):
        self.server_id = integer(server_id, "serverId", 1)
        self.transport = transport

    def handle(self, intent):
        fields(intent, {"action", "commandId", "serverId"},
               {"controlRevision", "helperGeneration", "rentalId", "leaseSeconds"})
        identifier(intent["commandId"], "commandId")
        integer(intent["serverId"], "serverId", 1)
        if intent["serverId"] != self.server_id:
            raise HelperError("WRONG_SERVER", "Intent belongs to another server.")
        operations = {"open": "APPLY_INGRESS", "close": "DISABLE_INGRESS", "check": "READ_STATUS",
                      "renew": "RENEW_INGRESS", "claim": "ADVANCE_GENERATION"}
        action = intent["action"]
        if not isinstance(action, str) or action not in operations:
            raise HelperError("INVALID_REQUEST", "Expected open, close, check, claim or renew.")
        required = {"action", "commandId", "serverId"}
        if action != "check":
            required.update({"controlRevision", "helperGeneration"})
        if action in {"open", "renew", "claim"}:
            required.add("rentalId")
        optional = {"leaseSeconds"} if action in {"open", "renew"} else set()
        fields(intent, required, optional)
        payload = {}
        request = {"schemaVersion": 1, "commandId": intent["commandId"], "serverId": self.server_id,
                   "operation": operations[action], "payload": payload}
        if action != "check":
            request["controlRevision"] = integer(intent["controlRevision"], "controlRevision")
            request["generation"] = integer(intent["helperGeneration"], "helperGeneration")
        if action in {"open", "renew", "claim"}:
            payload["rentalId"] = integer(intent["rentalId"], "rentalId", 1)
        if action in {"open", "renew"}:
            payload["leaseSeconds"] = integer(intent.get("leaseSeconds", 60), "leaseSeconds", 5, 120)
        if action == "claim":
            payload["expectedGeneration"] = request["generation"]
            request["generation"] += 1
        response = self.transport(request)
        return {"action": action, "ok": response["ok"], "code": response["code"],
                "commandId": response["commandId"], "serverId": self.server_id, "helperResponse": response}
