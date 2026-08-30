# Timey Requirements

## Product Overview

Timey is an intelligent desk-side transition and commute assistant for hybrid
workers and students. It combines calendar events, commute planning, weather
conditions, real-time bus information, countdowns, and desktop alerts to help
users transition between virtual and physical commitments.

Requirements marked **Future Planned Enhancement** are specified for a later
iteration and are not part of the currently implemented application.

## Functional Requirements

### Module 1: On-Demand Commute Planning

| ID | Requirement | Priority |
|---|---|---|
| FR-1.1 | The system shall parse structured text commands or input forms containing an origin, destination, target arrival time, and optional buffer settings. Example: `plan from:"COM3" to:"VivoCity" by:18:30 buffer:10m`. | Must have |
| FR-1.2 | The system shall resolve postal codes, landmarks, building names, and bus stop IDs into coordinates via the OneMap Search API. | Must have |
| FR-1.3 | The system shall fetch transit routes and display at least two route alternatives, such as Fastest Transit, Least Transfers, or Direct Bus. | Must have |
| FR-1.4 | The system shall provide an itemised breakdown for each route, detailing walking legs, boarding stops, service numbers, and transfer points. | Must have |
| FR-1.5 | **Future Planned Enhancement:** The system shall allow users to store, view, and delete named frequent locations, such as Home, Office, or Campus. | Should have |

### Module 2: Calendar and Event Ingestion

| ID | Requirement | Priority |
|---|---|---|
| FR-2.1 | **Future Planned Enhancement:** The system shall import and parse standard iCalendar (`.ics`) files according to RFC 5545. | Must have |
| FR-2.2 | **Future Planned Enhancement:** The system shall automatically classify imported events as virtual or physical. Virtual events contain video-conference links, such as Zoom, Google Meet, or Microsoft Teams, in their location or description. Physical events contain a physical address, postal code, or meeting-room name. | Must have |
| FR-2.3 | **Future Planned Enhancement:** The system shall list all upcoming events for the current day in chronological order. | Must have |
| FR-2.4 | **Future Planned Enhancement:** The system shall allow users to manually add, edit, or delete events through the UI or CLI. | Should have |

### Module 3: Departure and Buffer Calculation Engine

| ID | Requirement | Priority |
|---|---|---|
| FR-3.1 | For physical events, the system shall compute the departure time using: `Departure Time = Target Arrival Time - (Transit Duration + Walking Leg + User Buffer)`. | Must have |
| FR-3.2 | **Future Planned Enhancement:** For virtual events, the system shall compute a pre-meeting preparation buffer and countdown, such as an alert three minutes before the meeting starts. | Must have |
| FR-3.3 | **Future Planned Enhancement:** The system shall query the Data.gov.sg 2-Hour Weather Forecast API. If rain is detected at the origin or destination, it shall automatically append a configurable weather buffer, such as five minutes. | Should have |
| FR-3.4 | **Future Planned Enhancement:** If the departure window is within 30 minutes, the system shall query LTA DataMall `/BusArrivalv2` to verify the first bus leg's real-time arrival and adjust the departure alert accordingly. | Should have |

### Module 4: UI, HUD, and Desktop Alerts

| ID | Requirement | Priority |
|---|---|---|
| FR-4.1 | The GUI shall display an active visual countdown card for the immediate next commitment, such as `Leave for Marina Bay in 14 mins` or `Zoom call in 4 mins`. | Must have |
| FR-4.2 | **Future Planned Enhancement:** The system shall trigger native desktop notifications at configurable milestones, such as T-10 minutes for preparation and T-2 minutes for leaving. | Must have |
| FR-4.3 | **Future Planned Enhancement:** The system shall provide a single-click action to open virtual meeting URLs in the default web browser. | Must have |
| FR-4.4 | **Future Planned Enhancement:** The system shall persist user preferences, including the default origin, walking speed, custom buffers, and cached route data, locally in JSON or SQLite files. | Must have |

## Non-Functional Requirements

| ID | Category | Requirement | Acceptance Measure |
|---|---|---|---|
| NFR-1 | Performance | **Future Planned Enhancement:** All network I/O and potentially large disk I/O operations shall execute asynchronously outside the JavaFX application thread. | The UI remains responsive while calendar imports, geocoding, weather, or transit requests are running. |
| NFR-2 | Performance | **Future Planned Enhancement:** Local calculations and cached calendar operations shall be completed quickly for the expected project dataset. | Operations complete within 100 ms under normal conditions. |
| NFR-3 | Performance | External route requests shall provide timely feedback without assuming that public APIs are always available. | A loading state appears immediately; the UI updates within approximately 1.5 seconds under normal network conditions or shows a timeout/fallback state. |
| NFR-4 | Performance | Timey shall start quickly enough for normal desk-side use. | The primary dashboard should render within 2.5 seconds on standard student hardware. |
| NFR-5 | Performance | Timey should maintain smooth visual updates during normal use. | The UI should target 60 FPS without freezing; this is a performance target rather than a hard cross-platform guarantee. |
| NFR-6 | Reliability | **Future Planned Enhancement:** Timeouts, malformed responses, unavailable APIs, invalid calendar files, and invalid user input shall be handled without crashing the application. | The user receives a clear error or fallback message and unsaved local data is preserved. |
| NFR-7 | Reliability | Time-dependent calculations shall use the fixed Singapore timezone and an injectable clock. | Automated tests cover Asia/Singapore timezone behaviour and produce deterministic results. |
| NFR-8 | Reliability | **Future Planned Enhancement:** Timey shall remain useful when external services are unavailable. | Saved places, imported events, cached routes, local calculations, and supported mock routes remain accessible offline. |
| NFR-9 | Reliability | **Future Planned Enhancement:** The system shall fall back to cached or internal mock transit data when OneMap or LTA is unreachable, rate-limited, or unavailable during peer testing. | The application does not crash and deterministic route workflows remain testable without internet access. |
| NFR-10 | Reliability | Local persistence shall minimise the risk of data loss and recover gracefully from malformed files. | Storage uses atomic write-and-replace behaviour; valid records load even when an individual entry is malformed. |
| NFR-11 | Reliability | The HTTP layer shall handle rate limits, server errors, connection failures, and timeouts using bounded retries. | HTTP 429, 5xx, and timeout failures use exponential backoff with a maximum of two retries before notifying the user. |
| NFR-12 | Usability | A first-time user shall be able to perform a commute query and understand the resulting departure plan without consulting the Developer Guide. | A peer tester can complete the basic workflow using only the User Guide. |
| NFR-13 | Usability | **Future Planned Enhancement:** Countdown cards and alerts shall clearly distinguish virtual meetings from physical events. | The event type, action, destination or meeting link, and relevant time are visible in the primary view. |
| NFR-14 | Usability | User input errors and operational failures shall provide actionable feedback. | The UI or CLI identifies the invalid input or failure and does not expose raw stack traces. |
| NFR-15 | Security | API credentials and user-specific configuration shall not be committed to the repository. | Credentials are loaded from local configuration or environment variables, and secret files are excluded by `.gitignore`. |
| NFR-16 | Privacy | **Future Planned Enhancement:** Calendar data and saved locations shall be stored locally unless the user explicitly initiates an external API request. | No calendar or location data is sent to an unrelated third-party service. |
| NFR-17 | Maintainability | The domain and application layers shall not depend directly on JavaFX or external API response classes. | No `javafx.*` dependencies exist in domain or application code, and UI controllers make no direct HTTP calls. |
| NFR-18 | Maintainability | **Future Planned Enhancement:** External integrations shall be replaceable independently. | Transit providers, calendar importers, weather providers, and notification adapters are accessed through interfaces in `Timey.ports`. |
| NFR-19 | Maintainability | Source code shall follow consistent Java naming, formatting, documentation, and package conventions. | Static analysis reports no critical Checkstyle violations; methods and nesting remain reasonably sized, with justified exceptions documented. |
| NFR-20 | Testability | **Future Planned Enhancement:** Core business logic shall be automatically testable and sufficiently covered. | Departure calculation, command parsing, `.ics` parsing, event classification, buffer logic, route selection, persistence, and fallback behaviour target at least 80% branch coverage. |
| NFR-21 | Testability | Time-sensitive engines shall use dependency-injected `java.time.Clock` instances. | Tests never depend on the actual current time and are deterministic. |
| NFR-22 | Testability | Network-dependent tests shall use static fixtures and test doubles. | `gradle test` and CI runs do not make live internet calls. |
| NFR-23 | Portability | Timey shall run on supported desktop operating systems using Java 25 and bundled JavaFX modules. | Equivalent core functionality is available on the documented macOS, Windows, and Linux configurations. |
| NFR-24 | Distribution | Timey shall be distributed as an executable fat JAR requiring no external database, container, or runtime service. | The documented Gradle build produces a runnable fat JAR containing the application dependencies. |
| NFR-25 | Documentation | Documentation shall match the released product and explain setup, usage, architecture, testing, acknowledgements, and AI-assisted development. | `UserGuide.md`, `DeveloperGuide.md`, and `Reflections.md` are updated before each release. |

## Constraints

1. The application shall be an individual Java desktop application.
2. The project shall not replicate the CS2103/T individual or team project as a
   to-do manager or equivalent chat-based task manager.
3. The repository shall contain the required user guide, developer guide,
   reflection document, and verified AI interaction summaries.
4. API-dependent functionality shall have offline test fixtures or mock
   providers so that automated tests do not require live network access.
5. All times used by the application shall be interpreted consistently using
   the fixed `Asia/Singapore` timezone.

## Assumptions and Open Questions

- The initial release targets Singapore locations and public-transport data.
- API rate limits, authentication details, and exact response schemas will be
  verified against the current official API documentation during implementation.
- The supported desktop operating systems and bundled JavaFX distribution will
  be documented in the Developer Guide before the first release.
- Java 25 is the required Java runtime and toolchain version.
- The release build must produce an executable fat JAR.
- If an event cannot be confidently classified, Timey should ask the user to
  classify it manually rather than silently making a potentially unsafe
  departure recommendation.
