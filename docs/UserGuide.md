# Timey User Guide

## Quick start

1. Install Java 25 or a compatible JDK.
2. From the project root, build the executable fat JAR:

   ```bash
   ./gradlew shadowJar
   ```

   The JAR is generated at `release/Timey-0.1.0-all.jar`.

3. Launch the JavaFX dashboard:

   ```bash
   java -jar ./release/Timey-0.1.0-all.jar
   ```

4. To use the terminal CLI instead, run:

   ```bash
   ./gradlew run
   ```

   Enter `help` to display the available commands. The dashboard and CLI use
   the same command workflow and locally saved data.

## Planning a commute

Enter a plan and choose a route:

```
plan /from "COM3" /to "VivoCity" /by 1830 /buf 10m
choose 1
```

Timey uses live OneMap location and rail-route data when it is available. If a
live lookup cannot be completed, it presents an `Offline estimate` that uses a
one-hour travel buffer before your requested arrival time. This estimate is a
safe fallback rather than a measured route duration. A failed update leaves
your current plan unchanged.

Choosing a route displays its recommended leave-by time. If that time has
already passed today, Timey tells you to leave immediately. Automatic
departure reminders are reserved for a future enhancement.

## If a command cannot complete

Input errors identify the affected command option. For operational failures,
Timey keeps the current plan unchanged and asks you to check your internet
connection or saved data before retrying. Internal exception details are not
shown in the CLI or dashboard.

## Application defaults

The application uses a built-in 10-minute default buffer whenever a `plan` command omits `/buf`. An explicit `/buf`
always takes precedence for that command. The application includes the deployed Cloudflare Worker endpoint, and the
Worker holds the OneMap credentials, so users do not need to configure a URL, token, or OneMap account.

## Fixed commute timings

Save a known duration for an exact origin and destination pair:

```
add /from "COM3" /to "VivoCity" /dur 1h30m
```

The timing is stored locally and appears as the first route alternative for later matching `plan` commands. Durations
may use hours, minutes, or both: `1h`, `30m`, and `1h30m`.

Review saved timings with `ls saved`. Use `ls plans` to review saved plans whose
departure time is still in the future. Remove a saved timing by its list number
with `rm 1`.

Saved timings and plans are written through a temporary replacement file to
reduce the risk of losing an existing file during a failed write. Malformed
stored entries are ignored where a valid record can still be recovered.
