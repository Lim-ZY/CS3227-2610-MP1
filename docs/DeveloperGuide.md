# Timey Developer Guide

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

## Development status

The current implementation provides an interactive CLI, deterministic route
alternatives, departure calculation, and OneMap-backed rail route lookup with
offline fallback. This document will be updated as further features and design
decisions are implemented.

## Acknowledgements

To be completed with all reused ideas, code, libraries, and documentation.
