"""Only known interactive console tasks are exempt from VM-operation fencing."""

TASK_POLICY = "CONSOLE_ONLY_EXEMPT_V1"
CONSOLE_TASK_TYPES = frozenset({"vncshell", "vncproxy", "spiceproxy"})


def blocking_tasks(tasks):
    # Missing and new task types remain blocking. A shell can still run a real
    # mutation; that mutation's separate PVE task must pass this same check.
    return [task for task in tasks if not isinstance(task, dict)
            or task.get("type") not in CONSOLE_TASK_TYPES]
