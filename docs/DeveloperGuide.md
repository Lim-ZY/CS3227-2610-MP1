---
layout: default.md
title: "Developer Guide"
pageNav: 3
---

# Timey Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

## Architecture

Timey is organised into domain, application, ports, infrastructure,
presentation, command, and configuration packages. The domain and application
layers should remain independent of JavaFX and external API response formats.

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

To be completed with all reused ideas, code, libraries, and documentation.
