# Reflections on AI-Assisted Software Engineering (using LLM & Prompting)

This document records key insights I got from this 3 week journey of building Timey.
It includes prompts, suggestions, and lessons learned throughout Timey's development.

## Key Insights

1. [An iterative workflow is incredibly beneficial in reviewing and guiding the agent's work.](#iterative-workflow)
2. [Test-Driven Development seems to guide the agent better.](#test-driven-development)
3. [Plan > Develop > Test > Review > Repeat](#plan--develop--test--review--repeat)
4. ["Checklist-driven development" seems to improve accuracy of work produced (and reduce hallucination).](#checklist-driven-development-seems-to-improve-accuracy-of-work-produced-and-reduce-hallucination)

## Iterative Workflow

When I first started to prompt the agent, the agent generated too much code too quickly. 
I realised that it tends to lose context and often widens the scope of the project unintentionally.
So I tried breaking down steps for the agent, and clearly defined goals so the agent can focus on one small step at a time.
This not only provided guardrails for the agent, but also allowed me to review the code generated in small sprints instead of reviewing many large files at once.

Here is the prompt structure I used:

```
Great. Let's implement ... In each iteration, do the following steps:

1. Decide the next natural stand-alone increment that moves the code closer to the target. 
2. Implement that increment.
3. Test it to ensure there are no regressions, using the JUnit tests and checkstyle tests.
4. Commit the changes with a detailed commit message. You have my permission to commit in this repo.
5. Generate a visual diff using the /present-changes-visually skill. Also explain the rationale for the change and its pros and cons.
6. Briefly outline the next increment to be done in the next iteration. If there are no more increments worth doing, say so and stop.

Go ahead and do the first iteration.
```

This iterative cycle allowed me to guide the agent's actions better, and make just-in-time adjustments to the code
before too much technical debt piles up.


## Test-Driven Development

After several iterations, I came to realise that the agent worked better when I provided example outputs. It gave the agent
something to work towards and break down the steps to attain the goal by itself.

Here is a prompt I used for example:

```
Check that the fixed commute saving is case-insensitive. If previous fixed commutes are exactly the same as new commutes except for the casing, do not add the fixed commute, unless the duration is different.

Example:
> add /from "com3" /to "home" /dur 90m
_______________________________________________________
Saved fixed timing from com3 to home: 90 minutes.
It will appear as a route option in your next matching plan.
_______________________________________________________
> ls saved
_______________________________________________________
Saved timings:
1. com3 → home — 90 minutes
_______________________________________________________
> add /from "COM3" /to "Home" /dur 90m
_______________________________________________________
This route has already been saved for you! Do check it out
using `ls saved`.
_______________________________________________________
> add /from "COM3" /to "Home" /dur 100m
_______________________________________________________
Changed fixed timing from COM3 to Home: 100 minutes.
It will appear as a route option in your next matching plan.
_______________________________________________________
```

Giving multiple cases to the agent allows it to consider more hidden cases, thereby reducing bugs.
I also asked the agent to maintain the following testing policy:

```
Goal provided to Codex:
* AI-assisted changes must preserve a minimum 80% coverage target for core
business logic and critical integration boundaries. 
* Test effort focuses first on the highest-value approximately 50% of methods: complex calculations,
parsing, state transitions, provider-response mapping, and failure or fallback
paths. 
* After every code change, the related JUnit tests must be added or
updated and the full test suite run before the change is treated as complete.
```

This allowed prevented multiple regressions along the way, and saved me lots of time.

## Plan > Develop > Test > Review > Repeat

Eventually I settled on this iterative process. Before asking the agent to do anything, I ...

1. gave it the requirement to implement, ...
2. asked it to produce a plan for implementing it, ...
3. save the plan (as a checklist) into a `.md` file in a temporary folder in the project which is not committed.

Then I asked it to implement it iteratively, before testing the implementation personally, and reviewing the code.
If there were deviations from my expectations, I could tell the agent to fix it or give it examples for it to work towards.

## "Checklist-driven development" seems to improve accuracy of work produced (and reduce hallucination).

The culmination of the above insights resulted in this 2-part prompt I used in the second half of my project implementation.

Part 1:

```
Implement this requirement:
[=====
<insert description of requirement and test cases>
=====]

Suggest a plan to implement it. Put this plan in `<_tempDir/file.md>` as a checklist where each bullet point is a step to implement.
Do not implement any changes to the codebase yet.
```

After reviewing and altering the plan if needed, use the iterative workflow prompt.

Part 2:

```
Great. Go ahead and implement the requirement according to the plan iteratively. In each iteration, do the following steps:
1. Decide the next natural stand-alone increment that moves the code closer to the target. Follow the checklist in `<_tempDir/file.md>` closely. If a behaviour has not been finished implementing, continue implementing it before moving to the next feature in the checklist.
2. Implement that increment.
3. Test it to ensure there are no regressions, using the JUnit tests and checkstyle tests.
4. Commit the changes with a detailed commit message. You have my permission to commit in this repo.
5. Generate a visual diff using the /present-changes-visually skill. Also explain the rationale for the change and its pros and cons.
6. If an iteration in the checklist of `<_tempDir/file.md>` is completed in this iteration, check the box of the item in the checklist. 
7. Briefly outline the next increment to be done in the next iteration. If there are no more increments worth doing, say so and stop.

Go ahead and do the first iteration.
```

## Examples of Interesting Prompts Used

Here are some other interesting prompts I tried/used to make documentation/coding less painful to do by myself.
But some of which I realised were too broad in scope it jepardised the project, so I reset the commit.

| # | User Prompt Intent / Topic               | Key Constraints & Instructions Given                                                             |
|---|------------------------------------------|--------------------------------------------------------------------------------------------------|
| 1 | Requirements: Functional (FRs)           | Initiate structured step-by-step planning starting with functional requirements.                 |
| 2 | Requirements: Non-Functional (NFRs)      | Formulate NFRs mapped to CS3227 criteria (performance, offline fallback, testability).           |
| 3 | Architecture Strategy (Tree-of-Thoughts) | Apply Tree-of-Thoughts (2 paths, pros/cons, comparison, selection) to decide the build sequence. |
| 4 | Implementation Prompt Roadmap            | Generate a phased sequence of prompts mapped to functional requirements and order 1.             |

## When was prompting less effective than manual work?

Personally, I think the designing of software architecture is still lacking in the agent. It usually broadens the scope too quickly
and under-delivers in the end. So I think much of the work of designing the structure and component interactions will still be done 
by humans for now (or maybe I am just not educated enough yet to be certain on this).
I also think that the design and decision of algorithms could be better in the agents, and for now it is better to supervise 
the agent to give options or just give it a rough implementation to implement concretely.

## What would I do differently next time

I would definitely try to limit the scope of the project especially when just beginning. 
I would also plan the project the traditional way (paper and pen). This includes the requirements, software architecture, algorithms to use,
other high-level ideas and testing strategies. This would greatly reduce the time needed to "redirect" the effort of the agent,
and allow more efficiency in the work done.
