# Timey

Timey is a Java desktop commute planner for in-person events. It provides a
command-line interface and JavaFX dashboard for planning a journey, comparing
route alternatives, calculating a leave-by time, and managing saved timings.

Live OneMap route and location lookups are used when available. When live data
cannot be retrieved, Timey retains valid prior state and offers a deterministic
offline estimate using a one-hour travel buffer.

See `docs/UserGuide.md` for commands and setup, and `docs/DeveloperGuide.md`
for architecture and test guidance.
