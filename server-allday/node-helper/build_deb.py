"""Build a Debian package containing the reviewed helper and a separate setup command."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    if os.name != "posix" or shutil.which("dpkg-deb") is None:
        raise SystemExit("Build the .deb on Linux with dpkg-deb.")
    output = Path(args.output).absolute()
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="allday-deb-") as directory:
        stage = Path(directory)
        destination = stage / "opt/server-allday/node-helper"
        groups = {"node_helper", "deploy", "config", "examples", "docs", "tests", "integration"}
        for source in ROOT.rglob("*"):
            relative = source.relative_to(ROOT)
            if not source.is_file() or source.is_symlink() or "__pycache__" in relative.parts or source.suffix == ".pyc":
                continue
            if relative.parts[0] not in groups and str(relative) not in {"README.md", "run_helper.py", "run_demo.py"}:
                continue
            target = destination / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
            target.chmod(0o644)
        control = stage / "DEBIAN"
        control.mkdir()
        size = sum(file.stat().st_size for file in destination.rglob("*") if file.is_file()) // 1024 + 1
        (control / "control").write_text(
            "Package: server-allday-node-helper\nVersion: 0.2.0\nSection: admin\nPriority: optional\n"
            "Architecture: all\nMaintainer: Server Allday Team\n"
            "Depends: python3 (>= 3.11), nftables (>= 1.0.6), conntrack, sudo\n"
            f"Installed-Size: {size}\n"
            "Description: Restricted host operations for Server Allday\n"
            " JSON-over-SSH cloud-init files, atomic ingress switching and cleanup verification.\n"
            " Configure node values and manager key with deploy/install.py after installation.\n"
            " No firewall or SSH configuration is activated by this package installation.\n", encoding="utf-8")
        (control / "control").chmod(0o644)
        for entry in stage.rglob("*"):
            if entry.is_dir():
                entry.chmod(0o755)
        stage.chmod(0o755)
        subprocess.run(["dpkg-deb", "--root-owner-group", "--build", str(stage), str(output)], check=True)
    print(output)


if __name__ == "__main__":
    main()
