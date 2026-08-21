# Timey Developer Guide

## Architecture

Timey is organised into domain, application, ports, infrastructure,
presentation, command, and configuration packages. The domain and application
layers should remain independent of JavaFX and external API response formats.

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

## Development status

The current implementation provides an interactive CLI, deterministic route
alternatives, departure calculation, and OneMap-backed rail route lookup with
offline fallback. This document will be updated as further features and design
decisions are implemented.

## Acknowledgements

To be completed with all reused ideas, code, libraries, and documentation.
