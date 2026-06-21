---
type: cpu
priority: critical
category: infrastructure
tags: [performance, database, server]
---
# CPU Overload

## Description
Server @host@ CPU usage is at @value@% (threshold: @threshold@%).

## Impact
- Service @service@ may become unresponsive or degraded
- Potential cascading failures to dependent services
- Increased latency for all requests

## Recommended Actions
1. Connect to the server: `ssh @host@`
2. Identify top CPU consumers: `top -b -n 1 | head -20`
3. Check for recent deployments or configuration changes
4. Consider scaling up or distributing load

## Task Details
- **Service:** @service@
- **Severity:** Critical
- **Response Time:** Immediate action required
