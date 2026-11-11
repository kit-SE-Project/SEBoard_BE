import hashlib
import json
import sqlite3

from .errors import HelperError
from .snippets import SnippetManager
from .store import OperationStore
from .validation import READ_OPERATIONS, validate_request


class NodeHelper:
    def __init__(self, config, host):
        self.config = config
        self.host = host
        self.store = OperationStore(config)
        self.snippets = SnippetManager(config)

    def close(self):
        self.store.close()

    def response(self, request, result=None, error=None):
        value = {"schemaVersion": 1, "commandId": request.get("commandId"), "serverId": self.config.server_id,
                 "operation": request.get("operation"), "mode": self.config.mode, "ok": error is None, "replayed": False}
        if error:
            value.update(code=error.code, message=str(error), details=error.details)
        else:
            value.update(code="OK", result=result)
        return value

    def handle(self, request):
        claimed = False
        try:
            validate_request(request, self.config)
            digest = hashlib.sha256(json.dumps(request, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
            with self.store.locked():
                op = request["operation"]
                if op not in READ_OPERATIONS:
                    old = self.store.receipt(request["commandId"])
                    if old:
                        if old["request_hash"] != digest:
                            raise HelperError("COMMAND_ID_CONFLICT", "This commandId has already been used with different input.")
                        if old["status"] == "RUNNING":
                            raise HelperError("OPERATION_UNCERTAIN", "The previous process ended without a recorded result. Observe and recover before issuing a new command.")
                        response = json.loads(old["response"])
                        response["replayed"] = True
                        response["historicalResult"] = True
                        return response
                    self.store.begin(request["commandId"], digest)
                    claimed = True
                try:
                    result = self.dispatch(request)
                    response = self.response(request, result=result)
                except HelperError as error:
                    response = self.response(request, error=error)
                except (OSError, ValueError, KeyError, TypeError, sqlite3.Error):
                    response = self.response(request, error=HelperError("INTERNAL_ERROR", "The operation could not be completed. Inspect host state before retrying."))
                if claimed:
                    self.store.finish(request["commandId"], response)
                self.store.audit(request["commandId"], op, response["code"])
                return response
        except HelperError as error:
            return self.response(request if isinstance(request, dict) else {}, error=error)

    def bootstrap(self):
        with self.store.locked():
            state = self.store.state()
            previous = list(state["pendingCleanup"])
            try:
                target = self.host.ingress()["target"]
                if target:
                    previous.append(target)
            except HelperError:
                pass
            if state["active"]:
                previous.append(state["active"]["vmIp"])
            # Persist a closed/recovery intent before touching the kernel.
            state["needsRecovery"] = True
            state["pendingCleanup"] = list(dict.fromkeys(previous))
            self.store.save_state(state)
            observed = self.host.bootstrap()
            for address in state["pendingCleanup"]:
                self.host.clear_connections(address)
            state.update(active=None, owner=None, pendingCleanup=[], needsRecovery=False, bootId=self.host.boot_id())
            self.store.save_state(state)
            self.store.audit(None, "BOOTSTRAP", "OK")
            return {"mode": self.config.mode, "generation": state["generation"], "ingress": observed}

    def require_boot(self, state):
        if state["bootId"] != self.host.boot_id():
            raise HelperError("BOOTSTRAP_REQUIRED", "Initialize closed ingress for this boot before accepting commands.")

    def require_generation(self, request, state):
        expected = request["payload"]["expectedGeneration"] if request["operation"] == "ADVANCE_GENERATION" else request["generation"]
        if expected != state["generation"]:
            raise HelperError("STALE_GENERATION", "Command generation does not match the host generation.", {"currentGeneration": state["generation"]})

    def require_control_revision(self, request, state):
        current, incoming = state.get("controlRevision"), request.get("controlRevision")
        if current is not None and incoming is None:
            raise HelperError("CONTROL_REVISION_REQUIRED", "This node requires the manager control revision on every mutation.")
        if incoming is not None:
            if current is not None and incoming < current:
                raise HelperError("STALE_CONTROL_REVISION", "An older manager intent must not be rebased onto a newer helper generation.")
            state["controlRevision"] = incoming
            self.store.save_state(state)

    def quiescent(self, vm_id=None):
        if self.host.tasks():
            raise HelperError("TASK_IN_PROGRESS", "A Proxmox task is still active. Recheck its outcome before continuing.")
        vms = self.host.vms()
        relevant = [x for x in vms if x["vmid"] == vm_id] if vm_id else [
            x for x in vms if self.config.vm_id_min <= x["vmid"] <= self.config.vm_id_max]
        if any(x["status"] != "stopped" for x in relevant):
            raise HelperError("VM_NOT_STOPPED", "The previous student VM has not been confirmed stopped.")
        return vms

    def dispatch(self, request):
        state = self.store.state()
        self.require_boot(state)
        op, p = request["operation"], request["payload"]
        if op not in READ_OPERATIONS:
            self.require_generation(request, state)
            self.require_control_revision(request, state)
        if op == "READ_STATUS":
            ingress = self.host.ingress()
            return {"generation": state["generation"], "controlRevision": state.get("controlRevision"),
                    "ownerRentalId": state["owner"], "accessEnabled": ingress["leaseValid"],
                    "needsRecovery": state["needsRecovery"], "pendingCleanup": state["pendingCleanup"],
                    "ingress": ingress, "vms": self.host.vms(),
                    "activeTaskCount": len(self.host.tasks())}
        if op == "REGISTER_RESOURCE":
            resource = self.store.register({**p, "snippetHashes": {}})
            return {"rentalId": resource["rentalId"], "vmId": resource["vmId"], "vmIp": resource["vmIp"]}
        if op == "DISABLE_INGRESS":
            return self.disable(state)
        resource = self.store.resource(p["rentalId"], include_released=op == "VERIFY_CLEANUP")
        if op == "PUT_SNIPPET":
            self.quiescent(resource["vmId"])
            if self.host.ingress()["target"] == resource["vmIp"]:
                raise HelperError("INGRESS_STILL_ACTIVE", "Close student access before changing initial settings.")
            resource["snippetHashes"] = self.snippets.put(resource, p)
            self.store.save_resource(resource)
            return {"rentalId": resource["rentalId"], "references": self.snippets.references(resource["rentalId"]), "verified": True}
        if op == "REMOVE_SNIPPET":
            self.require_vm_deleted(resource)
            self.snippets.remove(resource["rentalId"])
            resource["snippetHashes"] = {}
            self.store.save_resource(resource)
            return {"removed": True, "rentalId": resource["rentalId"]}
        if op == "ADVANCE_GENERATION":
            if state["needsRecovery"] or state["pendingCleanup"]:
                raise HelperError("RECOVERY_REQUIRED", "Old access cleanup is not complete.")
            if self.host.ingress()["target"] is not None or state["active"] is not None:
                raise HelperError("SWITCH_REQUIRES_DISABLE", "Disable ingress and clear old connections first.")
            self.quiescent()
            state.update(generation=request["generation"], owner=resource["rentalId"])
            self.store.save_state(state)
            return {"generation": state["generation"], "ownerRentalId": state["owner"], "stopVerified": True}
        if op == "APPLY_INGRESS":
            return self.enable(state, resource, p.get("leaseSeconds", 60))
        if op == "RENEW_INGRESS":
            return self.enable(state, resource, p.get("leaseSeconds", 60), renew=True)
        if op == "VERIFY_CLEANUP":
            return self.verify_cleanup(resource)
        if op == "RELEASE_RESOURCE":
            result = self.verify_cleanup(resource)
            if result["result"] != "PASS":
                raise HelperError("CLEANUP_INCOMPLETE", "Resources are still present or could not be verified.", result)
            self.store.release(resource["rentalId"])
            if state["owner"] == resource["rentalId"]:
                state["owner"] = None
                self.store.save_state(state)
            return {"released": True, "rentalId": resource["rentalId"], "verification": result}
        raise HelperError("INVALID_REQUEST", "Unsupported operation.")

    def disable(self, state):
        observed = self.host.ingress()
        previous = [*state["pendingCleanup"]]
        if observed["target"]:
            previous.append(observed["target"])
        if state["active"]:
            previous.append(state["active"]["vmIp"])
        state.update(needsRecovery=True, pendingCleanup=list(dict.fromkeys(previous)))
        self.store.save_state(state)
        # Block both new and established VM traffic before deleting old conntrack entries.
        closed = self.host.set_target(None)
        state["active"] = None
        self.store.save_state(state)
        for address in state["pendingCleanup"]:
            self.host.clear_connections(address)
        state.update(needsRecovery=False, pendingCleanup=[])
        self.store.save_state(state)
        return {"enabled": False, "ingress": closed, "oldConnectionsCleared": True}

    def enable(self, state, resource, lease_seconds=60, renew=False):
        if state["needsRecovery"] or state["pendingCleanup"]:
            raise HelperError("RECOVERY_REQUIRED", "A previous host change has an unconfirmed outcome.")
        if state["owner"] != resource["rentalId"]:
            raise HelperError("NOT_LEASE_OWNER", "This rental does not own the current host generation.")
        observed = self.host.ingress()
        current = observed["target"]
        if current not in {None, resource["vmIp"]}:
            raise HelperError("SWITCH_REQUIRES_DISABLE", "Another VM is still connected.")
        if (current is not None or state["active"] is not None) and not observed["leaseValid"]:
            raise HelperError("LEASE_EXPIRED", "Access expired. Disable and reconcile current intent before reopening; renewal cannot reopen expired access.")
        if renew and (current != resource["vmIp"] or not state["active"]):
            raise HelperError("LEASE_NOT_ACTIVE", "Renewal cannot open closed ingress.")
        if self.host.tasks():
            raise HelperError("TASK_IN_PROGRESS", "A Proxmox task is still active.")
        vms = self.host.vms()
        running = [x for x in vms if self.config.vm_id_min <= x["vmid"] <= self.config.vm_id_max and x["status"] != "stopped"]
        if len(running) != 1 or running[0]["vmid"] != resource["vmId"] or running[0]["status"] != "running":
            raise HelperError("VM_NOT_EXCLUSIVE", "Exactly the requested student VM must be running.")
        if not resource["snippetHashes"]:
            raise HelperError("SNIPPET_REQUIRED", "Initial settings have not been verified.")
        self.snippets.verify_hashes(resource["rentalId"], resource["snippetHashes"])
        if not self.host.ssh_ready(resource["vmIp"]):
            raise HelperError("SSH_NOT_READY", "The VM did not provide an SSH protocol identification.")
        if current is None:
            state.update(needsRecovery=True, active={k: resource[k] for k in ("rentalId", "vmId", "vmIp")})
            self.store.save_state(state)
            try:
                self.host.set_target(resource["vmIp"], lease_seconds)
            except HelperError:
                try:
                    self.host.set_target(None)
                except HelperError:
                    pass
                raise
            state["needsRecovery"] = False
            self.store.save_state(state)
        elif renew:
            # Recheck immediately before the transaction; expired leases never get
            # revived merely because a heartbeat was delayed in the network.
            if not self.host.ingress()["leaseValid"]:
                raise HelperError("LEASE_EXPIRED", "Access expired during health checks.")
            self.host.renew_lease(resource["vmIp"], lease_seconds)
        # Repeated APPLY never deletes conntrack for an already-active student.
        return {"enabled": True, "rentalId": resource["rentalId"], "generation": state["generation"],
                "leaseSeconds": lease_seconds, "sshReady": True, "ingress": self.host.ingress()}

    def require_vm_deleted(self, resource):
        vms = self.quiescent(resource["vmId"])
        if any(x["vmid"] == resource["vmId"] for x in vms):
            raise HelperError("VM_STILL_EXISTS", "Delete the VM through Proxmox before removing its settings.")
        if self.host.ingress()["target"] == resource["vmIp"]:
            raise HelperError("INGRESS_STILL_ACTIVE", "The rental is still an ingress target.")

    def verify_cleanup(self, resource):
        checks = []

        def check(kind, observe):
            try:
                remaining = observe()
                checks.append({"kind": kind, "status": "FAIL" if remaining else "PASS", "remaining": remaining})
            except (HelperError, OSError, ValueError, KeyError, TypeError):
                checks.append({"kind": kind, "status": "UNKNOWN"})

        check("VM", lambda: [x["vmid"] for x in self.host.vms() if x["vmid"] == resource["vmId"]])
        check("TASKS", lambda: [x["upid"] for x in self.host.tasks()])
        for storage in self.config.storage_ids:
            def volumes(storage=storage):
                return [x["volid"] for x in self.host.volumes(storage)
                        if x["volid"] in resource["volumeIds"] or str(x.get("vmid")) == str(resource["vmId"])]
            check(f"VOLUMES:{storage}", volumes)
        check("SNIPPETS", lambda: self.snippets.remaining(resource["rentalId"]))
        check("INGRESS", lambda: [resource["vmIp"]] if self.host.ingress()["target"] == resource["vmIp"] else [])
        check("CONNECTIONS", lambda: self.host.connections(resource["vmIp"]))
        state = self.store.state()
        if state["needsRecovery"]:
            checks.append({"kind": "LOCAL_STATE", "status": "UNKNOWN"})
        else:
            check("LOCAL_STATE", lambda: [resource["vmIp"]] if resource["vmIp"] in state["pendingCleanup"] or
                  state["active"] and state["active"]["rentalId"] == resource["rentalId"] else [])
        statuses = {x["status"] for x in checks}
        result = "FAIL" if "FAIL" in statuses else "UNKNOWN" if "UNKNOWN" in statuses else "PASS"
        return {"rentalId": resource["rentalId"], "result": result, "checks": checks}

