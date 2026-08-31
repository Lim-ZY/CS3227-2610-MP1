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

<puml src="UiClassDiagram.puml" width="800" />

`ConsoleUi` owns terminal input/output. `CommandLineApp` owns the shared command
session and returns immutable `DashboardState` snapshots, so it does not depend on
JavaFX controls or FXML.

The JavaFX dashboard is organised under `timey.ui.dashboard`:

- `TimeyDashboardApp` is a thin JavaFX lifecycle adapter. It receives a shared
  `CommandLineApp` from the composition root and gives it to `MainWindow`.
- `MainWindow` owns the stage, composes dashboard parts into the placeholders in
  `view/MainWindow.fxml`, coordinates command tasks, and propagates resulting
  state to the cards and panels.
- `DashboardHeader`, `CommandBar`, `CommandOutput`, `NextEventCard`,
  `CommuteStatusCard`, and `RouteAlternativesPanel` each have one `UiPart`
  subclass and one matching FXML view in
  `src/main/resources/timey/ui/dashboard/view`. Components keep controls
  private and expose small rendering or event APIs.
- `UiPart` supplies the concrete component as the FXML controller. Consequently,
  dashboard FXML uses `fx:id` and event handlers as needed but does not declare
  `fx:controller`.

Potentially slow command work runs in a JavaFX `Task`. Its success and failure
handlers update components on the JavaFX application thread; planners, parsers,
and domain logic remain outside the presentation package.

Successful command execution produces a `DashboardState` containing the current
plan, route alternatives, planning messages, selected recommendation, and next
saved plan. `MainWindow` passes that immutable snapshot to each component’s
rendering method. A failed or superseded task does not overwrite the latest
rendered state; the command output and failure text are handled by the relevant
presentation components.

`DashboardCommandExecutionGate` serializes command-session access on worker
threads. This prevents an overlapping or cancelled dashboard request from
mixing terminal output or model state with a later request.

### Command and logic flow

The command layer separates text interpretation from application behaviour:

1. `CommandLineApp.executeCommand()` passes the raw input to `Parser`.
2. `Parser` identifies the command name and delegates option-bearing commands
   to `PlanCommandParser` or `AddCommandParser`. It creates simple commands
   such as `HelpCommand`, `ListCommand`, `RemoveCommand`, `ChooseCommand`, or
   `ThanksCommand` directly.
3. `CommandOptionParser` checks command boundaries, extracts quoted or
   unquoted option values, rejects duplicate or unrecognised text, and enforces
   required options. The specialised parsers validate times, buffers, and
   durations before constructing command objects.
4. The resulting `Command` executes against the session’s `TimeyModel` and
   returns an immutable `CommandResult` containing display messages.
5. `CommandLineApp` sends those messages to `ConsoleUi` and returns a
   `CommandExecutionResult` containing the session-ended flag and a fresh
   `DashboardState` snapshot for the dashboard.

`plan` and `choose` form a stateful two-step workflow. `PlanCommand` asks the
model’s planner to resolve locations and create route alternatives, replacing
the pending plan only after a valid planning result is assembled. `ChooseCommand`
then validates that a pending plan exists and that its one-based route number is
valid before calculating and saving the departure recommendation. Selecting a
route clears the possibility of selecting another route for that plan; a new
`plan` request creates a new selection context.

Invalid input is reported as an `IllegalArgumentException` during parsing or
command construction and is converted by `CommandLineApp` into user-facing
feedback. Unexpected runtime failures use the shared safe recovery message,
while commands that are valid but cannot complete return an ordinary
`CommandResult` explaining the actionable next step.

### Model and domain components

`TimeyModel` owns mutable state for one command session. It keeps the pending
`PlanCommand`, the current list of `RouteAlternative` objects, planning and
route-selection messages, the selected `DepartureRecommendation`, and locally
loaded `SavedPlan` objects. Commands interact with this state through model
operations rather than reaching into provider or UI classes.

The model’s planning state follows this sequence:

1. `plan` resolves locations and asks `Planner` for a `PlanningResult`.
2. A successful result replaces the pending plan and stores immutable route and
   message lists. A result that cannot create a plan clears the pending route
   selection while retaining its explanatory messages.
3. `choose` validates the pending plan and one-based alternative number. It
   calculates a recommendation, persists a future `SavedPlan` when needed, and
   records the selected recommendation.
4. A later `plan` replaces the previous planning context and clears the prior
   route selection.

The `timey.domain` types represent provider-independent concepts:

- `RouteAlternative` combines walking and transit durations, transfer count,
  and itemised `RouteStep` values. Its `totalDuration()` is the sum of the two
  duration components.
- `DepartureCalculator` computes `arrivalAt - travelDuration - buffer`, and
  `DepartureRecommendation` validates that its stored departure time matches
  that formula.
- `FixedCommute` stores a reusable user-provided duration between two
  locations.
- `SavedPlan` stores the date, target arrival, locations, and leave-by time for
  a selected future route.
- `LiveRouteLookup` distinguishes available routes from an empty successful
  response, an HTTP failure, or an unreachable live-data service.

`RouteSelectionResult` gives the command layer explicit outcomes such as
`NO_PLAN`, `MISSING_NUMBER`, `INVALID_NUMBER`, `NO_ALTERNATIVES`,
`ALREADY_SELECTED`, `LEAVE_NOW`, and `ROUTE_SELECTED`. This keeps validation
and user-facing branching in `ChooseCommand` while keeping the underlying
recommendation calculation in the planner and domain layers.

### Ports and infrastructure

Ports define the boundaries that the application uses without depending on a
specific provider or persistence mechanism:

- `LocationResolver` converts a user query into a `LocationResolution` and,
  when successful, a `ResolvedLocation`.
- `RailTransitPlanner` returns a `LiveRouteLookup` for resolved locations,
  while `TransitPlanner` provides the simpler route-planning abstraction used
  by deterministic planning services.
- `FixedCommuteStore` provides save, find, list, and remove operations for
  reusable commute durations.
- `PlanStore` loads and replaces the list of selected saved plans.

The production adapters implement these ports as follows:

- `OneMapLocationResolver` calls the configured live-data endpoint to resolve
  Singapore locations. `OneMapRailTransitPlanner` maps the endpoint response
  into `RouteAlternative` and `RouteStep` domain values.
- `MockTransitPlanner` supplies a deterministic, network-free planner for
  tests and local composition paths that do not need live routes.
- `FileFixedCommuteStore` and `FilePlanStore` persist local text records. The
  in-memory fixed-commute store is used by isolated test or development
  sessions.
- `JdkHttpRequester` is the concrete HTTP client. `RateLimitedHttpRequester`
  handles a rate-limit retry, while `RetryingHttpRequester` handles bounded
  retries for transient server responses and request failures. All are exposed
  behind the small `HttpRequester` boundary so API adapters can use test
  doubles instead of live network calls.

This separation lets planner and model tests use fixed clocks, mock ports, and
fixtures. Replacing a provider or storage format therefore changes the adapter
and its composition rather than the command or domain rules.

### Storage design

Production storage is local and relative to the directory from which Timey is
run. `ApplicationFactory` supplies `FileFixedCommuteStore` for
`data/fixed-commutes.txt` and `FilePlanStore` for `data/plans.txt`. Missing files
are treated as empty stores, and parent directories are created when a write is
first needed.

The fixed-commute format stores one journey per line:

```text
Origin -> Destination = 45m
```

Origins and destinations are compared case-insensitively. Saving the same
journey and duration is a no-op; saving the same journey with a new duration
replaces the previous timing. Reads discard blank or malformed lines and return
valid timings in case-insensitive origin/destination order.

The saved-plan format stores one selected plan per line:

```text
31-08-2026 | 1830 | Origin -> Destination | leave by 1745
```

`FilePlanStore` parses dates strictly, removes duplicate records, and retains
the stored order of valid plans. Invalid individual records are skipped without
preventing other plans from loading. `TimeyModel` removes plans whose leave-by
time has passed and writes the remaining list back to the store.

Both file stores use `AtomicFileWriter`: content is written to a temporary
sibling file first, then moved over the destination with an atomic replace when
the filesystem supports it. A regular replace is used as the fallback when an
atomic move is unavailable. If writing or replacing fails, the temporary file is
cleaned up and the in-memory model updates its state only after the store call
succeeds, preserving the previous valid destination and model state as far as
the filesystem allows.

## Implementation

### Commute planning

`Planner.findAlternatives()` first resolves the origin and destination through
`LocationResolver`. A successful resolution produces coordinates and a display
address without exposing the provider response to the rest of the application.
If either location is not found, the planner returns an explanatory result. A
service failure, rate limit, or unavailable live endpoint is mapped to a safe
message and the deterministic fallback path.

For resolved locations, `LiveRailPlanningService` performs a bounded two-step
lookup through `RailTransitPlanner`:

1. It probes for routes at the requested target-arrival time.
2. If routes are returned, it calculates the leave-by time for the first route
   using the requested personal buffer, then refreshes the lookup at that
   departure time so the alternatives are aligned with the arrival target.

`OneMapRailTransitPlanner` maps each returned itinerary into a
`RouteAlternative`, preserving walking duration, transit duration, transfer
count, and displayable `RouteStep` values. The planner reports live routes and
their source messages to the command layer. The model then prepends a matching
saved fixed timing as `Saved timing` route 1 when one exists.

When live data is unavailable or returns no usable routes, `Planner` creates a
single `Offline estimate` route. This route is explicitly labelled and uses a
deterministic one-hour travel estimate during route selection. The application
does not present that fallback as a live measurement, allowing users and tests
to distinguish degraded service from a successful live lookup.

### Plan saving and lifecycle

Fixed commute timings and selected future plans are separate persisted concepts.
The `add` command constructs a `FixedCommute` and delegates it to
`FixedCommuteStore`; the `ls saved` and `rm` commands read the store’s stable
list order and remove by its one-based display number. A matching fixed timing
is offered as a route option during a later `plan` request.

After a route is selected, `TimeyModel` creates a `SavedPlan` from the target
date, arrival time, journey, and calculated leave-by time. It does not persist
plans whose leave-by time is no longer in the future, and it does not add an
equal plan twice. The store is written before the new plan is published to the
model, so a failed write does not make the in-memory session claim that the
plan was saved.

At model construction, `PlanStore.loadAll()` restores saved plans and immediately
prunes expired or duplicate records. `getNextSavedPlan()` selects the upcoming
plan with the earliest leave-by datetime, including the previous calendar date
when an overnight journey’s leave-by time is later than its arrival time. The
same pruning runs when `ls plans` is executed and when the session closes.

`FileFixedCommuteStore` sorts timings by origin and destination for stable
numbering. `FilePlanStore` preserves the order of valid stored plans and
deduplicates them. Both stores serialize through `AtomicFileWriter`, which
writes a temporary sibling and replaces the destination only after the complete
new content has been written.

### Departure calculation and route selection

`PlanCommand` rejects a target arrival time that has already passed in the
current `Asia/Singapore` clock context before asking the model to plan. For a
selected route, `DepartureCalculator` applies the rule:

```text
leave-by = target arrival - route travel duration - personal buffer
```

`DepartureRecommendation` validates this relationship when it is constructed,
so presentation code can safely display the stored departure time, route, total
travel duration, and buffer. The planner uses the same injected `Clock` for
target-date and current-time decisions, keeping the calculation deterministic
in tests.

`ChooseCommand` delegates route validation to `TimeyModel.selectRoute()`. The
model reports distinct outcomes when there is no pending plan, no route
number, an out-of-range number, no alternatives, or an existing selection.
After a valid selection it chooses the ordinary live calculation or the
one-hour offline calculation, depending on the route source. A recommendation
whose departure time is now or earlier returns `LEAVE_NOW` and adds urgent
guidance; a future recommendation returns `ROUTE_SELECTED`.

The model persists a future selected plan before publishing the selected
recommendation. This preserves the previous valid state if the plan store
fails, while duplicate plans remain a no-op. A subsequent `plan` request clears
the previous selection and starts a new route-selection context.

## Design considerations

The following choices keep the implementation small while preserving safe
extension points:

- **Ports and adapters:** Provider and persistence concerns are accessed
  through interfaces, so domain and model rules do not depend on OneMap, HTTP,
  or a particular file format. The trade-off is a few additional adapter and
  composition classes for otherwise simple operations.
- **Live routes with deterministic fallback:** Live OneMap routes provide useful
  current alternatives, while the labelled one-hour fallback keeps the core
  workflow usable and testable when external services fail. The fallback is
  less accurate and must not be presented as a live measurement.
- **Injected clocks:** A single Singapore timezone and injectable `Clock` make
  departure and expiry rules consistent across the CLI, dashboard, and tests.
  Production code uses the system clock, while tests use fixed clocks.
- **Atomic local persistence:** Temporary-file write-and-replace reduces the
  risk of leaving a partially written store. Filesystems without atomic-move
  support use a regular replacement as a compatibility fallback.
- **Shared CLI/dashboard session:** The dashboard reuses `CommandLineApp` and
  renders `DashboardState` instead of duplicating command logic. Background
  tasks and `DashboardCommandExecutionGate` serialize access so overlapping or
  cancelled requests cannot mix output or state.

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

## Documentation, logging, testing, configuration, dev-ops

Contributor-facing development records are kept in `logs/`, while
[`Reflections.md`](Reflections.md) records the AI-assisted development process,
verification, and lessons learned. Runtime failures are surfaced through
user-safe messages; the application does not expose raw exception details in
the CLI or dashboard.

`ApplicationConfiguration` is the single source for the fixed
`Asia/Singapore` timezone, the built-in departure buffer, and the configured
live-data endpoint. API credentials and user-specific secrets must not be
committed to the repository. Runtime stores remain relative to the launch
directory under `data/`.

The HTTP adapters use a bounded request timeout, a single delayed retry for a
rate-limit response, and bounded exponential retries for transient server
responses or request failures. Dashboard command work runs in JavaFX `Task`
instances and is serialized by `DashboardCommandExecutionGate`; stale or
cancelled results are ignored by the command tracker.

### Testing policy

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

## Appendix: Requirements

### Product scope

Timey is a desktop commute assistant for students and workers travelling to
physical commitments. The implemented scope covers command-driven commute
planning, Singapore location resolution, live rail-route alternatives, route
selection, personal departure buffers, reusable fixed commute timings, saved
future plans, a JavaFX dashboard, and a deterministic offline estimate when live
data is unavailable. The dashboard and CLI use the same command session and
local stores.

Calendar and virtual-event ingestion, weather and LTA lookups, native
notifications, cached routes, walking-speed preferences, and named saved
locations remain planned enhancements. The full prioritized requirements and
acceptance measures are maintained in [`Requirements.md`](Requirements.md).

### User stories

| Priority | User | Goal | Benefit |
| --- | --- | --- | --- |
| Must have | Commuter | plan a journey from an origin to a destination by a target arrival time | know when to leave |
| Must have | Commuter | compare live route alternatives and their steps | choose an informed route |
| Must have | Commuter | add a personal departure buffer | account for transition time |
| Must have | Commuter | select a route and see a leave-by recommendation | reach the destination on time |
| Should have | Frequent traveller | save, list, reuse, and remove a fixed commute timing | avoid repeating known timing work |
| Should have | Planner | view and revisit future selected plans | keep upcoming journeys visible |
| Should have | User without connectivity | receive a labelled deterministic estimate | continue planning during service failure |

### Use cases

For the use cases below, the actor is the user and the system is Timey.

**Use case: UC1 - Plan a commute**

**MSS**

1. User supplies an origin, destination, target arrival time, and optional
   buffer.
2. Timey resolves both locations and obtains aligned route alternatives.
3. Timey displays the alternatives and their route steps.

**Extensions**

* 1a. A required option is missing or malformed.
  * 1a1. Timey displays an input error and does not change the pending plan.
* 2a. A location is not found.
  * 2a1. Timey displays a correction prompt.
* 2b. Live routing is unavailable or returns no usable route.
  * 2b1. Timey displays a labelled `Offline estimate` when fallback planning is
    possible.

**Use case: UC2 - Select a route and receive a departure recommendation**

**MSS**

1. User enters `choose <route-number>` after planning.
2. Timey validates the pending plan and route number.
3. Timey displays the route, travel duration, buffer, and recommended departure.

**Extensions**

* 1a. No plan exists, the number is missing, or the number is out of range.
  * 1a1. Timey explains the required next action.
* 1b. The recommended departure time has arrived.
  * 1b1. Timey returns `LEAVE_NOW` and tells the user to leave immediately.
* 1c. A route has already been selected for the pending plan.
  * 1c1. Timey asks the user to create a new plan before selecting again.

**Use case: UC3 - Manage saved timings and plans**

**MSS**

1. User adds, lists, removes, or reuses a fixed commute timing, or selects a
   future route.
2. Timey persists the valid change and displays the resulting stable list.

**Extensions**

* 1a. The timing or plan is a duplicate.
  * 1a1. Timey keeps one record and reports that it is already saved.
* 1b. A stored line is malformed or expired.
  * 1b1. Timey skips or prunes that record while retaining valid records.
* 1c. Storage cannot be written.
  * 1c1. Timey keeps the previous valid state and displays a safe failure
    message.

### Non-functional requirements

1. **Portability:** Timey must run on supported macOS, Windows, and Linux
   configurations with Java 25 and the bundled JavaFX modules.
2. **Responsiveness:** Network work must provide loading feedback and use
   bounded timeouts/retries; it must not block the JavaFX application thread.
3. **Reliability:** Invalid input, unavailable services, malformed responses,
   and malformed local records must not crash the application or discard valid
   local state.
4. **Determinism:** Time-dependent logic must use `Asia/Singapore` and injected
   clocks; network-dependent tests must use fixtures or test doubles.
5. **Usability:** CLI errors and dashboard failures must identify an actionable
   next step without exposing raw stack traces.
6. **Maintainability:** Domain and application logic must remain independent of
   JavaFX, provider response classes, and concrete storage formats.
7. **Distribution:** The Gradle build must produce an executable fat JAR without
   requiring an external database or runtime service.
8. **Privacy and security:** Local plan data remains under the launch
   directory’s `data/` folder, and credentials or user-specific secrets must not
   be committed to the repository.

### Glossary

- **Commute:** A journey from an origin to a destination that must be completed
  by a requested target arrival time.
- **Fixed timing:** A user-recorded duration for an exact origin/destination
  pair. It is reusable as a labelled route option during planning.
- **Saved plan:** A selected future commute containing its date, target arrival,
  journey, and calculated leave-by time.
- **Route alternative:** One candidate way to travel between two resolved
  locations, including walking time, transit time, transfer count, and steps.
- **Route step:** One human-readable leg of a route, such as walking between
  locations or taking a named rail service.
- **Transfer:** A change between transit services within a route alternative.
- **Personal buffer:** Extra time requested by the user and subtracted from the
  target arrival time when calculating departure.
- **Leave-by time:** The latest calculated time at which the user should begin
  the journey to arrive on time, including travel duration and buffer.
- **Pending plan:** The current plan and route alternatives awaiting a
  `choose` command. A new `plan` replaces it.
- **Offline estimate:** A deterministic, explicitly labelled one-hour fallback
  used when live route data is unavailable; it is not a live measurement.
- **Session:** The period during which one `CommandLineApp` and its
  `TimeyModel` remain active.
- **OneMap:** The live-data service used by Timey’s adapters for Singapore
  location resolution and rail-route lookup.

## Appendix: Instructions for manual testing

These checks supplement the automated suite. Run them from a clean temporary
working directory when testing persistence so that existing `data/` files do
not affect the expected results. Build the application first with
`./gradlew shadowJar`. The detailed command syntax is maintained in the
[User Guide](UserGuide.md).

### Launch and shutdown

1. Run `java -jar ./release/Timey-0.1.0-all.jar`.
   Expected: the JavaFX dashboard opens with its header, commute cards, route
   panel, and command bar.
2. Run `./gradlew run` in a terminal.
   Expected: the CLI prints its welcome message and accepts `help`.
3. Enter `thx` in the CLI, or close the dashboard window.
   Expected: the session ends cleanly and no error details are printed.

### Commute planning and route selection

1. Start the CLI and enter:
   `plan /from "Kent Ridge MRT" /to "Harbourfront MRT" /by 1830 /buf 10m`.
   Expected: Timey displays resolved locations and live route alternatives, or
   an explicitly labelled `Offline estimate` when live data is unavailable.
2. Enter `choose 1`.
   Expected: the selected route, travel duration, buffer, and recommended
   departure time are displayed.
3. Try `choose`, `choose 0`, `choose 999`, and `choose 1` again.
   Expected: Timey reports missing, invalid, or already-selected route state
   without replacing the current selection.
4. Try `plan /from "unknown place" /to "Harbourfront MRT" /by 1830`.
   Expected: Timey gives a location correction message and does not replace a
   valid existing plan.

### Fixed timings and saved plans

1. Enter `add /from "COM3" /to "VivoCity" /dur 1h30m`, then `ls saved`.
   Expected: one fixed timing appears in stable order.
2. Repeat the same `add` command.
   Expected: no duplicate timing is created. Repeat with `/dur 2h` and verify
   the existing journey is updated.
3. Enter `rm 1`, then `ls saved`.
   Expected: the displayed timing is removed.
4. Plan and select a future route, then enter `ls plans`.
   Expected: the selected plan appears with its date, arrival time, journey,
   and leave-by time. Restart Timey and verify it is loaded again.
5. Create a plan whose leave-by time is in the past, then enter `ls plans`.
   Expected: the expired plan is pruned and does not appear.

### Error, fallback, and storage recovery

1. Try malformed inputs such as `plan /from A /to B`,
   `plan /from A /to B /by 18:30`, `add /from A /to B /dur 0m`, and
   `choose abc`.
   Expected: each command returns actionable feedback without crashing or
   changing valid state.
2. Disconnect the network or use an environment where the live service cannot
   be reached, then repeat a valid `plan` command.
   Expected: a labelled deterministic offline estimate is shown and the CLI or
   dashboard remains usable.
3. With Timey closed, add one valid and one malformed line to each file under
   `data/`, then restart and run `ls saved` or `ls plans`.
   Expected: valid records load, malformed individual records are ignored, and
   the application starts without exposing a stack trace.
4. Back up a data file, make its parent directory unwritable, and attempt an
   `add` or route selection that writes data. Restore permissions afterwards.
   Expected: Timey reports a safe persistence failure and retains the previous
   valid in-memory state.

### Dashboard loading and concurrency

1. Launch the dashboard and submit a valid `plan` command.
   Expected: the commute and route panels show a loading state, then update on
   completion or show a safe failure message.
2. While a slow command is running, submit another command. Close the window
   while a command is running as a separate check.
   Expected: the active work is cancelled or superseded, stale output does not
   overwrite the latest dashboard state, and the window closes cleanly.

## Appendix: Planned Enhancements

The following items are deliberately outside the currently implemented scope.
They should be treated as future requirements rather than assumptions about
current behavior. Each enhancement should preserve the existing domain and
ports boundaries and add offline test doubles before introducing live service
dependencies.

1. **Calendar and event ingestion:** Import `.ics` calendars, classify events as
   physical or virtual, list upcoming commitments, and support manual event
   management.
2. **Weather-aware planning:** Query the Data.gov.sg weather forecast and add a
   configurable weather buffer when rain affects the journey.
3. **Real-time bus verification:** Query LTA DataMall bus arrivals for imminent
   bus legs and adjust departure guidance when appropriate.
4. **Virtual-event support:** Calculate preparation buffers, show virtual
   meeting countdowns, and offer a one-click action to open meeting links.
5. **Native notifications:** Notify users at configurable preparation and
   departure milestones.
6. **Cached route data:** Cache valid route responses and use them, together
   with local calculations, when live services are unavailable.
7. **Walking-speed preferences:** Let users configure walking speed and use it
   when estimating walking legs and departure times.
8. **Named saved locations:** Allow users to create, view, edit, and delete
   labels such as Home, Office, or Campus for frequent locations.
9. **Persistent preferences:** Store default origins, walking speed, custom
   buffers, and other user preferences locally.
