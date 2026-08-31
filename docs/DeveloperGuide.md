---
layout: default.md
title: "Developer Guide"
pageNav: 3
---

# Timey Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

## Setting up, getting started

Timey requires Java 25. The Gradle toolchain also targets Java 25, and the
JavaFX modules used by the dashboard are downloaded by Gradle as part of the
build. From the repository root, run:

```bash
./gradlew clean test checkstyleMain checkstyleTest
```

This compiles the application, runs the JUnit test suite, and checks the main
and test source sets with Checkstyle. To build the executable fat JAR, run:

```bash
./gradlew shadowJar
```

The JAR is written to `release/Timey-0.1.0-all.jar`. Launch the JavaFX
dashboard with:

```bash
java -jar ./release/Timey-0.1.0-all.jar
```

To use the terminal CLI during development, run:

```bash
./gradlew run
```

The dashboard and CLI share the same command workflow and local data files.
See the [User Guide](UserGuide.md) for the complete command reference and a
first-use planning example.

The main project directories are organised as follows:

- `src/main/java/timey/domain`: domain objects and business rules.
- `src/main/java/timey/command`: command objects and command results.
- `src/main/java/timey/parser`: command parsing and option validation.
- `src/main/java/timey/planner`: commute planning services.
- `src/main/java/timey/ports`: interfaces for replaceable integrations.
- `src/main/java/timey/infrastructure`: HTTP, storage, location, and transit
  adapters.
- `src/main/java/timey/ui`: console and JavaFX presentation code.
- `src/main/resources/timey/ui/dashboard`: dashboard FXML views and styles.
- `src/test/java`: tests matching the production package hierarchy.
- `data`: local fixed-commute and saved-plan files created at runtime.

## Architecture

<puml src="ClassDiagram.puml" width="900" />

The class diagram above shows the main production types and their relationships.
Timey follows a ports-and-adapters structure: the command and planning flow
depends on stable interfaces, while HTTP, OneMap, mock, and file-backed
implementations are composed at the application boundary.

The main packages have the following responsibilities:

- `timey`: application entry points and dependency composition.
- `timey.command`: commands that operate on the application model and return
  displayable command results.
- `timey.parser`: conversion of user input into validated command objects.
- `timey.model`: mutable session state, pending route alternatives, selected
  recommendations, and saved-plan coordination.
- `timey.domain`: provider-independent value objects and business calculations
  for locations, transit routes, departure recommendations, and saved plans.
- `timey.planner`: orchestration of route planning and departure calculations
  through planner services.
- `timey.ports`: interfaces that isolate the application from replaceable
  location, transit, and persistence implementations.
- `timey.infrastructure`: adapters for OneMap, HTTP requests, mock transit,
  and local file storage.
- `timey.ui`: console presentation and immutable state snapshots shared with
  the dashboard.
- `timey.ui.dashboard`: JavaFX lifecycle, FXML-backed components, and dashboard
  rendering and command execution.
- `timey.config`: application-wide configuration such as the live-data base
  URI and fixed application time zone.

The dependency direction is intentionally inward. Domain types do not depend
on JavaFX or external response formats. Commands use `TimeyModel`; the model
uses planner services and port interfaces; infrastructure adapters implement
those ports. The UI invokes the command session and renders its results without
making HTTP or storage calls directly.

At runtime, `Timey` or `DashboardLauncher` starts the appropriate presentation
entry point. The composition layer supplies the concrete dependencies, the
command session parses and executes user input, and the UI renders the returned
result and current session state.

### Application bootstrap and dependency composition

The CLI entry point is `timey.Timey`. It creates a `ConsoleUi`, asks
`ApplicationFactory.createCommandLineApp()` for the shared command session,
and calls `run()`. The session prints the welcome message, reads commands,
executes them against one `TimeyModel`, and closes the model when the session
ends.

The dashboard entry point is `timey.ui.dashboard.DashboardLauncher`. JavaFX
launches `TimeyDashboardApp`, whose `init()` method creates the same kind of
`CommandLineApp` with an in-memory console input and captured output. Its
`start()` method passes that session to `MainWindow`, allowing dashboard
controls to use the command workflow without duplicating application logic.

`ApplicationFactory` is the composition root for the production application.
It supplies:

- `PlanCommandParser` and `CommutePlanningService` for command interpretation
  and deterministic commute calculations.
- `OneMapLocationResolver` and `OneMapRailTransitPlanner` for live lookups,
  each backed by a rate-limited HTTP requester.
- `FileFixedCommuteStore` and `FilePlanStore` for local persistence under
  `data/`.
- A system `Clock` configured with `ApplicationConfiguration.TIME_ZONE`.

The factory also applies the routing request timeout and the configured live
data endpoint. Tests bypass this production composition root and inject parser,
planner, port implementations, stores, and fixed clocks directly into
`CommandLineApp`. This keeps external services out of unit tests and makes
time-dependent behaviour deterministic.

## Presentation architecture

`ConsoleUi` owns terminal input/output. `CommandLineApp` owns the shared command
session and returns immutable `DashboardState` snapshots, so it does not depend on
JavaFX controls or FXML.

The JavaFX dashboard is organised under `Timey.ui.dashboard`:

- `TimeyDashboardApp` is a thin JavaFX lifecycle and dependency-composition adapter.
- `MainWindow` owns the stage, composes dashboard parts into the placeholders in
  `view/MainWindow.fxml`, coordinates command tasks, and propagates resulting state.
- Each major dashboard region has one `UiPart` subclass and one matching FXML view
  in `src/main/resources/Timey/ui/dashboard/view`. Components keep controls private
  and expose small rendering or event APIs.
- `UiPart` supplies the concrete component as the FXML controller. Consequently,
  dashboard FXML must use `fx:id` and event handlers as needed but must not declare
  `fx:controller`.

Potentially slow command work runs in a JavaFX `Task`. Its success and failure
handlers update components on the JavaFX application thread; planners, parsers,
and domain logic remain outside the presentation package.

`DashboardCommandExecutionGate` serializes command-session access on worker
threads. This prevents an overlapping or cancelled dashboard request from
mixing terminal output or model state with a later request.

## Reliability behaviour

All application-facing clocks use the fixed `Asia/Singapore` timezone and can
be injected in tests. The planner uses OneMap data when available; otherwise it
publishes a deterministic `Offline estimate` with a one-hour travel buffer.
The fallback is explicitly labelled and is not a live duration measurement.

Planning assembles its routes, messages, and matching saved timing before
replacing model state. Selecting a route writes a candidate saved-plan list
before publishing the local selection. These ordering rules preserve a
previous valid plan when dependent storage operations fail.

`FileFixedCommuteStore` and `FilePlanStore` write a temporary file and replace
the destination atomically where the filesystem supports it. Readers discard
malformed individual records while retaining valid ones. Runtime failures use a
shared, user-safe recovery message rather than exposing exception details.

## Testing policy

JUnit tests are kept under `src/test/java` in the same package hierarchy as
the production classes in `src/main/java`. Test names use the form
`featureUnderTest_testScenario_expectedBehavior` when a descriptive name would
otherwise be long.

The project targets at least 80% coverage for core business logic and critical
integration boundaries. Prioritise the highest-value roughly 50% of methods:
complex calculations, command parsing, state transitions, external-response
mapping, and error or fallback handling. Every production code change must
update or add JUnit tests as needed to preserve this target; the full suite
must pass before the change is considered complete.

FXML resource availability and the controller-ownership convention are covered
without starting a JavaFX toolkit. Pure dashboard display helpers are tested
directly; manual smoke testing remains appropriate for control interaction,
route selection, and visual layout changes.

Run the complete automated verification from the repository root:

```
./gradlew clean test checkstyleMain checkstyleTest
```

Tests use injected clocks, HTTP request doubles, and OneMap response fixtures;
they do not require live internet access.

## Development status

The current implementation provides an interactive CLI and JavaFX dashboard,
route selection and departure calculation, saved timings and plans, OneMap
location and rail lookup, deterministic offline fallback, preferences, and
bounded HTTP retries. Calendar import, virtual events, weather and LTA data,
native notifications, cached routes, walking-speed preferences, and full saved
location management remain planned enhancements.

## Acknowledgements

Timey is developed for the CS3227 project. The project uses the following
open-source libraries and tools:

- [OpenJFX](https://openjfx.io/) for the JavaFX dashboard and FXML views.
- [Jackson](https://github.com/FasterXML/jackson) for structured data handling
  where required by the application.
- [JUnit 5](https://junit.org/junit5/) for automated tests.
- [Gradle](https://gradle.org/) for building, testing, packaging, and
  Checkstyle verification.

AI-assisted development was used to help review documentation, suggest test
cases and edge cases, troubleshoot build or implementation issues, and refine
wording. The project’s implementation decisions, source changes, and final
verification remain the responsibility of the project team. AI assistance is
recorded in [`Reflections.md`](Reflections.md) where applicable.
