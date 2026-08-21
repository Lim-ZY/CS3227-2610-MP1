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
running. Use `reminders` to view active reminders. Entries whose trigger time
has already passed are discarded automatically. Use `cancel 1` to remove the
first active reminder and prevent its notification. Virtual-event reminders
will be added in later iterations.
