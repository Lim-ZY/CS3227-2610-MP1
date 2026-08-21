# Timey User Guide

## Planning a commute

Enter a plan and choose a route:

```
plan /from "COM3" /to "VivoCity" /by 1830 /buf 10m
choose 1
```

Choosing a route automatically schedules a console notification at its
recommended leave-by time. If that time has already passed today, Timey
schedules it for tomorrow instead. Reminders remain active while Timey is
running; listing, cancellation, and virtual-event reminders will be added in
later iterations.
