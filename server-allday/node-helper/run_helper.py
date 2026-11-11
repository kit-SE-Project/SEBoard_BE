#!/usr/bin/env python3
import sys
from pathlib import Path

# Also works with Python isolated mode in the root-owned production installation.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from node_helper.cli import main

if __name__ == "__main__":
    raise SystemExit(main())
