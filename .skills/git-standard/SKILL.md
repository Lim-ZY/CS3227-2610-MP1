---
name: git-standard
description: Apply the Timey project's Git conventions when creating, reviewing, amending, or proposing commits and branches. Use for every future Git commit, commit-message review, and branch-name decision in this repository.
---

# Git Standard

Follow the [SE-EDU Git conventions](https://se-education.org/guides/conventions/git.html).

## Commit subjects

- Write an imperative, capitalized subject that describes the change.
- Aim for 50 characters; never exceed 72 characters.
- Do not end the subject with a period.
- Add a concise scope or category prefix only when it improves clarity, for example `Parser: Reject blank input` or `chore: Update release date`.

## Commit bodies

- Add a body for every non-trivial commit, separated from the subject by one blank line.
- Wrap body lines at 72 characters and use blank lines to separate paragraphs.
- Explain what changed and why; leave implementation detail to the diff.
- Describe the current situation in present tense, then state why it needs changing, what the commit does, why that approach is appropriate, and relevant follow-up information.
- Use bullets when they make several related changes easier to scan. Split an overlong explanation into smaller, coherent commits instead.

## Branch names

- Use meaningful, kebab-case branch names made from relevant keywords, such as `refactor-ui-tests`.
- For issue work, use `issueNumber-keywords-from-title`, such as `1234-ui-freeze-error`.

## Before committing

1. Inspect the staged diff and confirm the commit contains one coherent change.
2. Verify the subject and, when needed, body against these rules.
3. Run relevant checks before creating the commit.
4. Do not create a commit when the user has only requested a message or review.
