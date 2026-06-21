---
type: memory
priority: high
category: infrastructure
tags: [memory, performance, jvm]
---
# Memory Leak / High Memory Usage

## Description
Memory usage on @host@ has reached @value@% (@used@/@total@). Rate of increase: @rate@ MB/min.

## Impact
- Application may become unstable or OOM
- Potential service restarts required
- Data loss risk if process terminates unexpectedly

## Recommended Actions
1. Capture heap dump: `jmap -dump:live,format=b,file=/tmp/heap.hprof @pid@`
2. Analyze memory with: `jstack @pid@` for thread analysis
3. Check GC logs: `jstat -gcutil @pid@ 1000`
4. Review recent code changes for leaks

## Task Details
- **Host:** @host@
- **Memory:** @used@ / @total@
- **Growth Rate:** @rate@ MB/min
