# Reflections on AI-Assisted Software Engineering

This document will record prompts, assumptions, verification steps, rejected
suggestions, and lessons learned throughout Timey's development.

## AI-assisted testing policy

AI-assisted changes must preserve a minimum 80% coverage target for core
business logic and critical integration boundaries. Test effort focuses first
on the highest-value approximately 50% of methods: complex calculations,
parsing, state transitions, provider-response mapping, and failure or fallback
paths. After every code change, the related JUnit tests must be added or
updated and the full test suite run before the change is treated as complete.
