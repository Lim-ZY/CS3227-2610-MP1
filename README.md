# Timey

Timey is a Java desktop commute planner for commuters, workers, and students
travelling to in-person events. It combines a fast command-line interface (CLI)
with a JavaFX dashboard: use the CLI for entering commands quickly, or use the
dashboard to review plans, route alternatives, and departure recommendations at
a glance.

Timey helps you:

- Plan a journey for the current day to reach a destination by a target arrival
  time.
- Compare live public-transport alternatives and their walking, bus, rail, and
  transfer details.
- Calculate a recommended leave-by time with a personal buffer.
- Save fixed commute timings for frequently used routes.
- Review upcoming saved plans and the next departure recommendation.

![Timey JavaFX dashboard](docs/images/TimeyDashboard.png)

Timey can only plan routes for the current day. It uses its deployed service to
request live OneMap location and public-transport route lookups when available.
These routes can combine walking, bus, and rail legs, allowing locations away
from rail stations to be reached. Users do not need to configure a URL, token,
or OneMap account. When live data cannot be retrieved or no suitable live route
is found, Timey retains valid prior state and offers a deterministic offline
estimate using a one-hour travel buffer.

Saved fixed commute timings and selected future plans are stored locally in the
`data/` directory as `fixed-commutes.txt` and `plans.txt`. The files are
created relative to the directory from which Timey is run; detailed storage
and recovery behavior is documented in the [User Guide](docs/UserGuide.md).
Saved plans cannot be removed manually for now; they expire automatically when
their leave-by time passes. Manual saved-plan removal is a future enhancement.

See the [User Guide](docs/UserGuide.md) for commands and setup, and the
[Developer Guide](docs/DeveloperGuide.md) for architecture and test guidance.

## Quick start

1. Install Java 25 or a compatible JDK.
2. From the project root, build the executable fat JAR:

   ```bash
   ./gradlew shadowJar
   ```

3. Launch the JavaFX dashboard:

   ```bash
   java -jar ./release/Timey-0.1.0-all.jar
   ```

4. To use the terminal CLI instead, run:

   ```bash
   ./gradlew run
   ```

   Type `help` to see the available commands. See the
   [User Guide](docs/UserGuide.md) for the complete command reference.
