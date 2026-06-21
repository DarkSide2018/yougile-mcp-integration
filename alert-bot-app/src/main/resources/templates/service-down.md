---
type: service
priority: critical
category: application
tags: [service, availability, outage]
---
# Service Down

## Description
Service @service@ on @host@ is DOWN. Health check failed with: @error@.

## Impact
- Complete service outage for @service@
- Users unable to access affected functionality
- Potential revenue loss
- SLA breach risk

## Recommended Actions
1. Check service status: `systemctl status @service@`
2. View recent logs: `journalctl -u @service@ --since "5 min ago" --no-pager`
3. Attempt restart: `systemctl restart @service@`
4. Check dependencies: `systemctl list-dependencies @service@`
5. Escalate to @team@ if restart fails

## Task Details
- **Service:** @service@
- **Host:** @host@
- **Error:** @error@
- **Downtime Started:** @timestamp@
