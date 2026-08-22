# Timey User Guide

## Planning a commute

Enter a plan and choose a route:

```
plan /from "COM3" /to "VivoCity" /by 1830 /buf 10m
choose 1
```

Choosing a route automatically schedules a console notification at its
recommended leave-by time. If that time has already passed today, Timey
instead tells you to leave immediately and does not create a reminder.
Reminders remain active while Timey is running. Use `reminders` to view active
reminders. Entries whose trigger time has already passed are discarded
automatically. Use `cancel 1` to remove the
first active reminder and prevent its notification. Virtual-event reminders
will be added in later iterations.

## Local preferences

Copy `config/application.example.properties` to `config/application.properties` to configure settings that persist
between launches. `timezone`, `departure-buffer-minutes`, and comma-separated `saved-locations` are loaded by both
the terminal CLI and JavaFX dashboard. The default buffer applies when a `plan` command omits `/buf`; an explicit
`/buf` always takes precedence. Keep the optional OneMap token in this local file or set `ONEMAP_ACCESS_TOKEN`.
