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

## First-use workflow

Use the following sequence to plan a commute:

1. Enter a `plan` command with your origin, destination, target arrival time,
   and optional personal buffer:

   ```text
   plan /from "Kent Ridge MRT" /to "Harbourfront MRT" /by 1830
   ```

2. Review the numbered route alternatives. Each live alternative includes its
   total travel time, walking time, transit time, transfer count, and steps.
   If live data is unavailable, Timey labels the one-hour fallback as
   `Offline estimate`.

3. Select an alternative by its number:

   ```text
   choose 1
   ```

4. Timey displays the selected route, total travel time, personal buffer, and
   recommended departure time. For example:

   ```text
   Chosen route: Offline estimate
   Total travel time: 60 minutes
   Personal buffer: 10 minutes
   Recommended departure: 17:20
   Please leave your desk by 17:20.
   ```

   The exact route and departure time depend on the requested journey, current
   time, live service availability, and selected buffer.

## Features

### 1. Command format

Timey commands use the following conventions:

- Words in angle brackets, such as `<origin>` and `<duration>`, are
  placeholders that must be replaced with your own values.
- Items in square brackets are optional. For example, `/buf Nm` can be omitted
  from a `plan` command to use the default buffer.
- Options may be entered in any order after the command name. Do not provide
  the same option more than once.
- Quote values that contain spaces, such as `/from "Kent Ridge MRT"`. Quotes
  are optional for single-word values.
- Command names are case-insensitive: `PLAN` and `plan` are equivalent.
- Route and saved-timing numbers are one-based positive integers, such as
  `choose 1` or `rm 1`.
- Extra text that does not match the command format is rejected and reported
  as an input error.

### 2. Command overview

| Action                       | Format and Examples |
|------------------------------| --- |
| Plan                         | `plan /from "<origin>" /to "<destination>" /by <HHmm> [/buf <duration>]`<br>e.g. `plan /from "Kent Ridge MRT" /to "Harbourfront MRT" /by 2000` |
| Choose                       | `choose <route-number>`<br>e.g. `choose 1` |
| Saving Fixed Commute Timings | `add /from "<origin>" /to "<destination>" /dur <duration>`<br>e.g. `add /from "COM3" /to "VivoCity" /dur 1h30m` |
| List Saved Commute Timings   | `ls saved` |
| List Current Plans           | `ls plans` |
| Remove Saved Commute Timing  | `rm <timing-number>`<br>e.g. `rm 1` |
| Help                         | `help` |
| Exit the Program             | `thx` |

### 3. Planning a commute: `plan`

Plans a commute and displays numbered route alternatives.

Format: `plan /from "<origin>" /to "<destination>" /by <HHmm> [/buf <duration>]`

Example:

```
plan /from "Kent Ridge MRT" /to "Harbourfront MRT" /by 1830 /buf 10m
```

- `/from`, `/to`, and `/by` are required. Quote locations containing spaces.
- `/by` uses 24-hour `HHmm` format, such as `1830` for 6:30 pm. The requested
  arrival time must not have passed today.
- `/buf` is optional and accepts a whole number of minutes with the `m` suffix.
  It defaults to 10 minutes when omitted and cannot be negative.
- A new `plan` replaces the currently pending plan and clears any previous
  route selection.

When live data is available, Timey resolves the locations and shows rail-route
alternatives with total, walking, and transit durations, transfer counts, and
individual route steps. If a location cannot be found, correct the location or
try a postal code. If the live service is unavailable or returns no suitable
route, Timey may show an `Offline estimate` using a fixed one-hour travel
buffer. This fallback is not a measured route duration, and a failed update
leaves the current plan unchanged.

Use `choose <route-number>` after reviewing the alternatives. Timey displays
the selected route’s recommended leave-by time. If that time has already
arrived, Timey tells you to leave immediately; automatic departure reminders
are not available.

### 4. Selecting a route: `choose`

Selects one of the alternatives displayed by the most recent `plan` command.

Format: `choose <route-number>`

Example:

```
choose 1
```

- The route number is a one-based positive integer matching the displayed list.
- A plan must be created successfully before a route can be selected.
- Select a route only once for the current plan. Create a new plan if you want
  to choose a different route.
- A successful selection calculates and saves the route’s departure
  recommendation for future reference.
- If the route number is missing or outside the displayed range, Timey reports
  the valid usage or range instead of changing the plan.

### 11. Handling errors

Timey reports the first error it finds and leaves the current plan unchanged
when an operation cannot complete.

Common input errors include:

- Missing required options, such as `/by` in a `plan` command.
- Invalid arrival times, such as `/by 18:30` instead of 24-hour `HHmm` format.
- Malformed or zero-length durations, such as `/dur 90` or `/dur 0m`.
- Missing, non-numeric, or out-of-range route numbers in `choose`.
- Using `choose` before creating a plan, or choosing again after a route has
  already been selected.
- Requesting an arrival time that has already passed today.

For live-data or storage failures, check the internet connection or saved data
and retry. Timey does not show internal exception details in the CLI or
dashboard, and a failed operational update does not replace the current plan.

### Persistence and application defaults

The application uses a built-in 10-minute default buffer whenever a `plan` command omits `/buf`. An explicit `/buf`
always takes precedence for that command. The application includes the deployed Cloudflare Worker endpoint, and the
Worker holds the OneMap credentials, so users do not need to configure a URL, token, or OneMap account.

Timey stores data relative to the directory from which it is run:

- `data/fixed-commutes.txt` stores saved fixed commute timings created by
  `add`.
- `data/plans.txt` stores selected plans whose recommended departure time is
  still in the future.

Fixed timings are saved when `add` succeeds. A selected route is saved as a
plan when its leave-by time is still in the future. Expired plans are pruned
when Timey starts, when `ls plans` is run, and when the session closes.

Timey writes these files through temporary replacement files to reduce the
risk of losing an existing file if a write fails. When loading data, malformed
individual records are ignored where valid records can still be recovered.
Back up the relevant file before editing it manually, and close Timey first so
the application does not overwrite your changes.

### 5. Saving fixed commute timings: `add`

Saves a known duration for a frequently used journey.

Format: `add /from "<origin>" /to "<destination>" /dur <duration>`

Example:

```
add /from "COM3" /to "VivoCity" /dur 1h30m
```

- `/from`, `/to`, and `/dur` are required. Quote locations containing spaces.
- A duration must be greater than zero and may use hours, minutes, or both:
  `1h`, `30m`, or `1h30m`.
- Timey matches a saved timing by origin and destination without regard to
  case or surrounding whitespace.
- A matching saved timing appears as route 1 in a later `plan` command.
- Saving an existing journey with a new duration updates its timing. Saving
  the same duration again leaves the existing timing unchanged.

### 6. Listing saved timings: `ls saved`

Lists all saved fixed commute timings and assigns each one a one-based number.

Format: `ls saved`

Example:

```
ls saved
```

Each result shows the timing number, origin, destination, and duration in
minutes. The list is sorted by origin and then destination without regard to
case, so use the displayed number when removing a timing with `rm`.

### 7. Listing future plans: `ls plans`

Lists selected commute plans whose recommended departure time is still in the
future.

Format: `ls plans`

Example:

```
ls plans
```

Each result shows a one-based number, date (`dd-MM-yyyy`), target arrival time
(`HHmm`), origin, destination, and recommended leave-by time (`HHmm`). Plans
whose leave-by time has passed are removed automatically and do not appear in
the list.

### 8. Removing a saved timing: `rm`

Removes a saved fixed commute timing by its number in the `ls saved` list.

Format: `rm <timing-number>`

Example:

```
rm 1
```

- The timing number must be a positive integer between 1 and the number of
  entries currently shown by `ls saved`.
- If the number is missing or is not an integer, Timey displays the command’s
  usage instead of removing a timing.
- If the number is outside the current list, Timey reports that no saved timing
  has that number.

### 9. Viewing command help: `help`

Displays the commands supported by the current Timey session.

Format: `help`

Example:

```
help
```

Use this command when you need a quick reminder of the available command
formats. It does not change the current plan or saved data.

### 10. Ending a session: `thx`

Ends the current terminal CLI session.

Format: `thx`

Example:

```
thx
```

Timey closes the session after displaying a short acknowledgement. The
dashboard is closed separately using the window controls.
