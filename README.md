# Timey

Timey is a Java desktop commute planner for in-person events. It provides a
command-line interface and JavaFX dashboard for planning a journey, comparing
route alternatives, calculating a leave-by time, and managing saved timings.

Live OneMap route and location lookups are used when available. When live data
cannot be retrieved, Timey retains valid prior state and offers a deterministic
offline estimate using a one-hour travel buffer.

See `docs/UserGuide.md` for commands and setup, and `docs/DeveloperGuide.md`
for architecture and test guidance.

## Build and run the fat JAR

Build the executable fat JAR with the Gradle wrapper:

```bash
./gradlew shadowJar
```

The generated file is placed at:

```text
./release/Timey-0.1.0-all.jar
```

Run the JavaFX dashboard with:

```bash
java -jar ./release/Timey-0.1.0-all.jar
```

The JAR includes the project’s runtime dependencies, so no separate dependency
JARs are needed. The machine running it must still have a compatible Java
runtime installed.
