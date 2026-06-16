---
type: disk
priority: high
category: infrastructure
tags: [storage, disk, capacity]
---
# Disk Space Alert

## Description
Disk {mount} on {host} is at {value}% capacity ({used}/{total}). Only {free}GB remaining.

## Impact
- Applications may fail to write logs or data
- Database operations may halt
- System may become unstable

## Recommended Actions
1. Check disk usage: `du -sh /* 2>/dev/null | sort -rh | head -10`
2. Clean old logs: `journalctl --vacuum-size=500M`
3. Remove temporary files: `find /tmp -type f -atime +7 -delete`
4. Consider adding storage or clearing old backups

## Task Details
- **Mount:** {mount}
- **Usage:** {used} / {total}
- **Free:** {free}GB
