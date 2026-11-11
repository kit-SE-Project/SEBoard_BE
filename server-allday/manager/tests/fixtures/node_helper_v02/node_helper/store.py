from contextlib import contextmanager
import json
import os
import sqlite3
import time

from .errors import HelperError
from .files import check_path


class OperationStore:
    def __init__(self, config):
        self.config = config
        config.state_dir.mkdir(parents=True, exist_ok=True)
        check_path(config.state_dir, config.production)
        if os.name == "posix":
            os.chmod(config.state_dir, 0o700)
        self.path = check_path(config.state_dir / "operations.sqlite3", config.production)
        self.db = sqlite3.connect(self.path, timeout=config.lock_timeout, isolation_level=None)
        self.db.row_factory = sqlite3.Row
        self.db.executescript("""
            PRAGMA journal_mode=WAL;
            PRAGMA synchronous=FULL;
            CREATE TABLE IF NOT EXISTS operations (
                command_id TEXT PRIMARY KEY, request_hash TEXT NOT NULL,
                status TEXT NOT NULL, response TEXT, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            CREATE TABLE IF NOT EXISTS node_state (id INTEGER PRIMARY KEY CHECK(id=1), value TEXT NOT NULL);
            INSERT OR IGNORE INTO node_state VALUES (1, '{"generation":0,"owner":null,"active":null,"pendingCleanup":[],"needsRecovery":false,"bootId":null}');
            CREATE TABLE IF NOT EXISTS resources (
                rental_id INTEGER PRIMARY KEY, vm_id INTEGER NOT NULL, vm_ip TEXT NOT NULL,
                value TEXT NOT NULL, released INTEGER NOT NULL DEFAULT 0
            );
            CREATE UNIQUE INDEX IF NOT EXISTS live_vm ON resources(vm_id) WHERE released=0;
            CREATE UNIQUE INDEX IF NOT EXISTS live_ip ON resources(vm_ip) WHERE released=0;
            CREATE TABLE IF NOT EXISTS audit (
                id INTEGER PRIMARY KEY, command_id TEXT, operation TEXT NOT NULL, code TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
        """)

    def close(self):
        self.db.close()

    @contextmanager
    def locked(self):
        path = check_path(self.config.state_dir / "node.lock", self.config.production)
        stream = open(path, "a+b")
        deadline = time.monotonic() + self.config.lock_timeout
        try:
            while True:
                try:
                    if os.name == "posix":
                        import fcntl
                        fcntl.flock(stream, fcntl.LOCK_EX | fcntl.LOCK_NB)
                    else:
                        import msvcrt
                        stream.seek(0)
                        msvcrt.locking(stream.fileno(), msvcrt.LK_NBLCK, 1)
                    break
                except (BlockingIOError, OSError):
                    if time.monotonic() >= deadline:
                        raise HelperError("NODE_BUSY", "Another node operation is still running.") from None
                    time.sleep(0.05)
            yield
        finally:
            stream.close()

    def state(self):
        return json.loads(self.db.execute("SELECT value FROM node_state WHERE id=1").fetchone()[0])

    def save_state(self, state):
        self.db.execute("UPDATE node_state SET value=? WHERE id=1", (json.dumps(state),))

    def receipt(self, command_id):
        return self.db.execute("SELECT * FROM operations WHERE command_id=?", (command_id,)).fetchone()

    def begin(self, command_id, digest):
        self.db.execute("INSERT INTO operations(command_id,request_hash,status) VALUES (?,?,'RUNNING')", (command_id, digest))

    def finish(self, command_id, response):
        self.db.execute("UPDATE operations SET status=?,response=? WHERE command_id=?",
                        ("SUCCEEDED" if response["ok"] else "FAILED", json.dumps(response), command_id))

    def audit(self, command_id, operation, code):
        self.db.execute("INSERT INTO audit(command_id,operation,code) VALUES (?,?,?)", (command_id, operation, code))

    def resource(self, rental_id, include_released=False):
        row = self.db.execute("SELECT * FROM resources WHERE rental_id=?", (rental_id,)).fetchone()
        if not row or row["released"] and not include_released:
            raise HelperError("RESOURCE_NOT_FOUND", "No live resource manifest exists for this rental.")
        return json.loads(row["value"])

    def register(self, value):
        existing = self.db.execute("SELECT * FROM resources WHERE rental_id=?", (value["rentalId"],)).fetchone()
        if existing:
            old = json.loads(existing["value"])
            if existing["released"] or any(old[k] != value[k] for k in ("vmId", "vmIp")) or not set(old["volumeIds"]) <= set(value["volumeIds"]):
                raise HelperError("RESOURCE_CONFLICT", "A rental manifest cannot be replaced.")
            old["volumeIds"] = value["volumeIds"]
            self.save_resource(old)
            return old
        try:
            self.db.execute("INSERT INTO resources(rental_id,vm_id,vm_ip,value) VALUES (?,?,?,?)",
                            (value["rentalId"], value["vmId"], value["vmIp"], json.dumps(value)))
        except sqlite3.IntegrityError:
            raise HelperError("RESOURCE_CONFLICT", "The VM ID or IP belongs to another live rental.") from None
        return value

    def save_resource(self, value):
        self.db.execute("UPDATE resources SET value=? WHERE rental_id=?", (json.dumps(value), value["rentalId"]))

    def release(self, rental_id):
        self.db.execute("UPDATE resources SET released=1 WHERE rental_id=?", (rental_id,))
