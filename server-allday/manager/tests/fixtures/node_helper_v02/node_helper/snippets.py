import hashlib
import json

from .errors import HelperError
from .files import atomic_write, check_path


class SnippetManager:
    def __init__(self, config):
        self.config = config
        config.snippet_dir.mkdir(parents=True, exist_ok=True)
        check_path(config.snippet_dir, config.production)

    def paths(self, rental_id):
        return {kind: check_path(self.config.snippet_dir / f"allday-r{rental_id}-{kind}.yaml", self.config.production)
                for kind in ("user", "network", "meta")}

    def contents(self, resource, payload):
        user = {"hostname": payload["hostname"], "manage_etc_hosts": True, "disable_root": True,
                "ssh_pwauth": True, "users": [{"name": payload["username"], "shell": "/bin/bash",
                                               "lock_passwd": False, "passwd": payload["passwordHash"]}],
                "chpasswd": {"expire": False}}
        network = {"version": 2, "ethernets": {self.config.vm_interface: {
            "dhcp4": False, "dhcp6": False, "accept-ra": False,
            "addresses": [f"{resource['vmIp']}/{self.config.vm_subnet.prefixlen}"],
            "routes": [{"to": "default", "via": str(self.config.vm_gateway)}],
            "nameservers": {"addresses": list(self.config.nameservers)},
        }}}
        metadata = {"instance-id": f"server-allday-r{resource['rentalId']}", "local-hostname": payload["hostname"]}
        # JSON is valid YAML for cloud-init; a serializer prevents YAML/shell interpolation.
        return {"user": ("#cloud-config\n" + json.dumps(user, indent=2) + "\n").encode(),
                "network": (json.dumps(network, indent=2) + "\n").encode(),
                "meta": (json.dumps(metadata, indent=2) + "\n").encode()}

    def put(self, resource, payload):
        hashes = {}
        paths = self.paths(resource["rentalId"])
        for kind, content in self.contents(resource, payload).items():
            atomic_write(paths[kind], content, self.config.production)
            hashes[kind] = hashlib.sha256(content).hexdigest()
        self.verify_hashes(resource["rentalId"], hashes)
        return hashes

    def verify_hashes(self, rental_id, hashes):
        for kind, path in self.paths(rental_id).items():
            if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != hashes[kind]:
                raise HelperError("SNIPPET_VERIFY_FAILED", "A snippet file is missing or differs from the generated content.")

    def remove(self, rental_id):
        for path in self.paths(rental_id).values():
            if path.exists():
                if not path.is_file():
                    raise HelperError("UNSAFE_PATH", "A managed snippet path is not a regular file.")
                path.unlink()
        if self.remaining(rental_id):
            raise HelperError("SNIPPET_VERIFY_FAILED", "Managed snippet files remain after deletion.")

    def remaining(self, rental_id):
        return [p.name for p in self.paths(rental_id).values() if p.exists()]

    def references(self, rental_id):
        return {kind: f"{self.config.snippet_storage}:snippets/{path.name}" for kind, path in self.paths(rental_id).items()}

