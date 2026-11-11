import json
import os
from pathlib import Path
import stat
import tempfile
import time

from .errors import HelperError


def check_path(path, production=False):
    path = Path(path).absolute()
    for entry in [*reversed(path.parents), path]:
        if entry.is_symlink():
            raise HelperError("UNSAFE_PATH", "Symbolic links are not allowed in managed paths.")
        if production and entry.exists():
            info = entry.stat()
            if info.st_uid != 0 or info.st_mode & (stat.S_IWGRP | stat.S_IWOTH):
                raise HelperError("UNSAFE_PATH", "Managed paths must be root-owned and not writable by other users.")
    return path


def atomic_write(path, content, production=False, mode=0o600):
    path = check_path(path, production)
    path.parent.mkdir(parents=True, exist_ok=True)
    check_path(path.parent, production)
    fd, temporary = tempfile.mkstemp(prefix=".allday-", dir=path.parent)
    try:
        with os.fdopen(fd, "wb") as stream:
            os.chmod(temporary, mode)
            stream.write(content)
            stream.flush()
            os.fsync(stream.fileno())
        deadline = time.monotonic() + 2
        while True:
            try:
                os.replace(temporary, path)
                break
            except PermissionError:
                # Windows indexers can briefly hold a new file without delete sharing.
                if os.name != "nt" or time.monotonic() >= deadline:
                    raise
                time.sleep(0.05)
        if os.name == "posix":
            directory = os.open(path.parent, os.O_RDONLY | os.O_DIRECTORY)
            try:
                os.fsync(directory)
            finally:
                os.close(directory)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def write_json(path, value, production=False):
    atomic_write(path, (json.dumps(value, sort_keys=True, indent=2) + "\n").encode(), production)
