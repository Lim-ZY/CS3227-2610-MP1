---
name: java-coding-standard
description: Apply the Timey project's Java coding standard when creating, editing, reviewing, or refactoring Java production or test code. Use for all .java changes in this repository, including style-only cleanups and code review.
---

# Java Coding Standard

Follow the [SE-EDU Java coding standard (basic + intermediate)](https://se-education.org/guides/conventions/java/intermediate.html). Use the Google Java Style Guide for matters it does not cover.

## Apply the standard

- Keep packages lower-case; use nouns in PascalCase for class and enum names, camelCase verbs for methods, camelCase for variables, and `SCREAMING_SNAKE_CASE` for constants.
- Use English names and comments. Use lower-case acronym forms inside names (`Html`, `Dvd`), boolean names that read as booleans (`is`, `has`, `can`, `should`, `was`), and plural names for collections.
- Use 4 spaces, never tabs. Keep lines at 120 characters or fewer (aim for 110). Wrap continuations 8 spaces relative to the parent; break after commas and before operators or chained dots when it improves readability.
- Use K&R braces. Always put braces around loop and conditional bodies, even when they have a single statement. Put `else`, `catch`, and `finally` on the closing-brace line.
- Put every class in a package. Keep imports explicit, minimal, and consistently ordered; do not use wildcard imports. Attach array brackets to the type.
- Declare variables in their smallest practical scope and initialize them at declaration where a valid initial value exists. Do not expose mutable public fields outside behavior-free data classes.
- Separate logical units with a blank line. Surround binary operators with spaces; place a space after control keywords, commas, and `for` semicolons. Mark deliberate traditional-switch fallthroughs with `// Fallthrough`.
- Write descriptive Javadoc for public classes and public methods, except getters/setters, exact inherited overrides, and test code. Start method summaries with a third-person verb such as “Returns” or “Adds”; use complete, punctuated `@param`, `@return`, and `@throws` descriptions when useful.

## Review workflow

1. Inspect changed Java code and nearby context before editing.
2. Apply the rules above without changing behavior solely for style.
3. Check for tabs, lines over 120 characters, wildcard imports, brace-less control flow, naming problems, and missing required Javadoc.
4. Run the relevant Gradle tests after code changes.

For existing code, correct violations in the requested scope; do not use the standard as a reason for unrelated behavioral refactors.
