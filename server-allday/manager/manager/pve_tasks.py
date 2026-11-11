"""Match the C Helper's explicit console-only exception; unknown tasks block."""

TASK_POLICY = "CONSOLE_ONLY_EXEMPT_V1"
CONSOLE_TASK_TYPES = frozenset({"vncshell", "vncproxy", "spiceproxy"})


def blocking_tasks(tasks):
    return [task for task in tasks if not isinstance(task, dict)
            or task.get("type") not in CONSOLE_TASK_TYPES]


def blocking_task_count(helper):
    # An older Helper has not adopted the exception: retain its stricter check.
    total = helper.get("activeTaskCount")
    blocked = helper.get("blockingTaskCount")
    observed = helper.get("observationalTaskCount")
    if (helper.get("taskPolicy") == TASK_POLICY and type(total) is int
            and type(blocked) is int and type(observed) is int
            and 0 <= blocked <= total and observed == total - blocked):
        return blocked
    return total
