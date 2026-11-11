import argparse
import json
import os
import signal
import sqlite3
import sys

from .config import Config
from .errors import HelperError
from .host import SimulationHost, SystemHost
from .rules import base_batch
from .service import NodeHelper
from .validation import MAX_REQUEST_BYTES, parse_request


def read_input(production):
    previous = None
    if production:
        def expired(signum, frame):
            raise HelperError("INPUT_TIMEOUT", "The JSON input deadline expired.")
        previous = signal.signal(signal.SIGALRM, expired)
        signal.alarm(10)
    try:
        return parse_request(sys.stdin.buffer.read(MAX_REQUEST_BYTES + 1))
    finally:
        if production:
            signal.alarm(0)
            signal.signal(signal.SIGALRM, previous)


def main(argv=None):
    parser = argparse.ArgumentParser(description="Server Allday host helper: JSON over stdin/stdout")
    parser.add_argument("--config", required=True)
    parser.add_argument("action", choices=("serve", "bootstrap", "render-firewall"))
    args = parser.parse_args(argv)
    helper = None
    try:
        os.umask(0o077)
        config = Config.load(args.config)
        if args.action == "render-firewall":
            result = base_batch(config, exists=False)
        else:
            host = SystemHost(config) if config.production else SimulationHost(config)
            helper = NodeHelper(config, host)
            result = helper.handle(read_input(config.production)) if args.action == "serve" else {
                "schemaVersion": 1, "ok": True, "code": "OK", "result": helper.bootstrap()}
        print(json.dumps(result, ensure_ascii=True, separators=(",", ":")))
        return 0 if result.get("ok", True) else 1
    except HelperError as error:
        print(json.dumps({"schemaVersion": 1, "ok": False, "code": error.code, "message": str(error)}))
        return 1
    except (OSError, ValueError, TypeError, KeyError, sqlite3.Error):
        print(json.dumps({"schemaVersion": 1, "ok": False, "code": "CONFIG_OR_IO_ERROR", "message": "Configuration or local storage could not be accessed."}))
        return 1
    finally:
        if helper:
            helper.close()
