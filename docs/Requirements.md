# Timey Requirements

## Product Overview

Timey is an intelligent desk-side transition and commute assistant for hybrid
workers and students. It combines calendar events, commute planning, weather
conditions, real-time bus information, countdowns, and desktop alerts to help
users transition between virtual and physical commitments.

## Functional Requirements

### Module 1: On-Demand Commute Planning

| ID | Requirement | Priority |
|---|---|---|
| FR-1.1 | The system shall parse structured text commands or input forms containing an origin, destination, target arrival time, and optional buffer settings. Example: `plan from:"COM3" to:"VivoCity" by:18:30 buffer:10m`. | Must have |
| FR-1.2 | The system shall resolve postal codes, landmarks, building names, and bus stop IDs into coordinates via the OneMap Search API. | Must have |
| FR-1.3 | The system shall fetch transit routes and display at least two route alternatives, such as Fastest Transit, Least Transfers, or Direct Bus. | Must have |
| FR-1.4 | The system shall provide an itemised breakdown for each route, detailing walking legs, boarding stops, service numbers, and transfer points. | Must have |
| FR-1.5 | The system shall allow users to store, view, and delete named frequent locations, such as Home, Office, or Campus. | Should have |

### Module 2: Calendar and Event Ingestion

| ID | Requirement | Priority |
|---|---|---|
| FR-2.1 | The system shall import and parse standard iCalendar (`.ics`) files according to RFC 5545. | Must have |
| FR-2.2 | The system shall automatically classify imported events as virtual or physical. Virtual events contain video-conference links, such as Zoom, Google Meet, or Microsoft Teams, in their location or description. Physical events contain a physical address, postal code, or meeting-room name. | Must have |
| FR-2.3 | The system shall list all upcoming events for the current day in chronological order. | Must have |
| FR-2.4 | The system shall allow users to manually add, edit, or delete events through the UI or CLI. | Should have |

### Module 3: Departure and Buffer Calculation Engine

| ID | Requirement | Priority |
|---|---|---|
| FR-3.1 | For physical events, the system shall compute the departure time using: `Departure Time = Target Arrival Time - (Transit Duration + Walking Leg + User Buffer)`. | Must have |
| FR-3.2 | For virtual events, the system shall compute a pre-meeting preparation buffer and countdown, such as an alert three minutes before the meeting starts. | Must have |
| FR-3.3 | The system shall query the Data.gov.sg 2-Hour Weather Forecast API. If rain is detected at the origin or destination, it shall automatically append a configurable weather buffer, such as five minutes. | Should have |
| FR-3.4 | If the departure window is within 30 minutes, the system shall query LTA DataMall `/BusArrivalv2` to verify the first bus leg's real-time arrival and adjust the departure alert accordingly. | Should have |

### Module 4: UI, HUD, and Desktop Alerts

| ID | Requirement | Priority |
|---|---|---|
| FR-4.1 | The GUI shall display an active visual countdown card for the immediate next commitment, such as `Leave for Marina Bay in 14 mins` or `Zoom call in 4 mins`. | Must have |
| FR-4.2 | The system shall trigger native desktop notifications at configurable milestones, such as T-10 minutes for preparation and T-2 minutes for leaving. | Must have |
| FR-4.3 | The system shall provide a single-click action to open virtual meeting URLs in the default web browser. | Must have |
| FR-4.4 | The system shall persist user preferences, including the default origin, walking speed, custom buffers, and cached route data, locally in JSON or SQLite files. | Must have |

## Non-Functional Requirements

| ID | Category | Requirement | Acceptance Measure |
|---|---|---|---|
| NFR-1 | Performance | Local operations such as loading saved places, viewing events, and calculating a departure time shall feel responsive. | Complete within 1 second for a normal local dataset. |
| NFR-2 | Performance | Network requests shall not block the JavaFX application thread. | The UI remains responsive while calendar imports, geocoding, weather, or transit requests are running. |
| NFR-3 | Performance | The active countdown shall remain accurate while the application is open. | Displayed countdown differs from the system clock by no more than 1 second during normal operation. |
| NFR-4 | Reliability | Timeouts, malformed responses, unavailable APIs, and invalid calendar files shall be handled without crashing the application. | The user receives a clear error or fallback message and unsaved local data is preserved. |
| NFR-5 | Reliability | Time-dependent calculations shall use an explicit timezone and injectable clock. | Automated tests cover Asia/Singapore timezone behaviour and produce deterministic results. |
| NFR-6 | Availability | Timey shall remain useful when external services are unavailable. | Saved places, imported events, cached routes, and local calculations remain accessible offline; live-data features show an appropriate unavailable state. |
| NFR-7 | Usability | A first-time user shall be able to perform a commute query and understand the resulting departure plan without consulting the developer guide. | A peer tester can complete the basic workflow using only the User Guide. |
| NFR-8 | Usability | Countdown cards and alerts shall clearly distinguish virtual meetings from physical events. | The event type, action, destination or meeting link, and relevant time are visible in the primary view. |
| NFR-9 | Usability | User input errors shall identify the invalid field and explain the expected format. | Invalid commands and forms produce actionable messages rather than generic errors. |
| NFR-10 | Security | API credentials and user-specific configuration shall not be committed to the repository. | Credentials are loaded from local configuration or environment variables, and secret files are excluded by `.gitignore`. |
| NFR-11 | Privacy | Calendar data and saved locations shall be stored locally unless the user explicitly initiates an external API request. | No calendar or location data is sent to an unrelated third-party service. |
| NFR-12 | Maintainability | The domain and application layers shall not depend directly on JavaFX or external API response classes. | External services are accessed through interfaces in the `Timey.ports` package. |
| NFR-13 | Maintainability | External integrations shall be replaceable independently. | OneMap, LTA, weather, and notification adapters can be replaced with test doubles without changing departure-calculation logic. |
| NFR-14 | Testability | Core parsing, classification, route selection, buffer calculation, persistence, and failure handling shall be automatically testable. | Tests run offline using fixture files and stub providers. |
| NFR-15 | Portability | The application shall run on supported desktop operating systems with a documented Java runtime. | A clean checkout can be built and launched using the documented Gradle commands on the supported JDK version. |
| NFR-16 | Code quality | Source code shall follow consistent Java naming, formatting, documentation, and package conventions. | The project builds without compiler warnings introduced by the application code, and public APIs have appropriate documentation. |
| NFR-17 | Documentation | Documentation shall match the released product and explain setup, usage, architecture, testing, acknowledgements, and AI-assisted development. | `UserGuide.md`, `DeveloperGuide.md`, and `Reflections.md` are updated before each release. |

## Constraints

1. The application shall be an individual Java desktop application.
2. The project shall not replicate the CS2103/T individual or team project as a
   to-do manager or equivalent chat-based task manager.
3. The repository shall contain the required user guide, developer guide,
   reflection document, and verified AI interaction summaries.
4. API-dependent functionality shall have offline test fixtures or mock
   providers so that automated tests do not require live network access.
5. All times used by the application shall be interpreted consistently using
   the configured timezone, with `Asia/Singapore` as the default.

## Assumptions and Open Questions

- The initial release targets Singapore locations and public-transport data.
- API rate limits, authentication details, and exact response schemas will be
  verified against the current official API documentation during implementation.
- The supported desktop operating systems and JavaFX distribution will be
  finalised in the Developer Guide before the first release.
- If an event cannot be confidently classified, Timey should ask the user to
  classify it manually rather than silently making a potentially unsafe
  departure recommendation.
