#!/usr/bin/env python3
"""Offline recovery of a PREPARE rejected before clone; no VM or resource mutations."""
from recover_return import run_cli


if __name__=='__main__':run_cli(prepare=True)
