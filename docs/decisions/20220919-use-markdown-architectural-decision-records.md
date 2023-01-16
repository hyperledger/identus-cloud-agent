# Use Markdown Architectural Decision Records

- Status: accepted
- Date: 2022-09-19
- Tags: doc

## Context and Problem Statement

We want to record architectural decisions made in this project.
Which format and structure should these records follow?

## Decision Drivers

- We want to improve the information and technical documentation of our software engineering projects
- We want to create an immutable log of important architectural decisions we have made during the software development
- We recognise the need for a complement to RFCs that typically documents the process before a decision has been reached (and not after)
- We want this decision log to offer a a standardised, lightweight, and extensible manner to increase consistency across systems
- We want this decision log to live as close as possible to the relevant code-base
- We want this decision log to be easily readable, discoverable and meaningfully searchable

## Considered Options

- [MADR](https://github.com/adr/madr/compare/3.0.0-beta...3.0.0-beta.2) 3.0.0-beta.2
- [MADR](https://adr.github.io/madr/) 2.1.2 with Log4brains patch
- [MADR](https://adr.github.io/madr/) 2.1.2 – The original Markdown Architectural Decision Records
- [Michael Nygard's template](http://thinkrelevance.com/blog/2011/11/15/documenting-architecture-decisions) – The first incarnation of the term "ADR"

## Decision Outcome

Chosen option: "MADR 2.1.2 with Log4brains patch", because

- The MADR format is lean and fits our development style.
- The MADR structure is comprehensible and facilitates usage & maintenance.
- The Log4brains patch adds more features, like tags.
- This format is compatible with Log4brains and allows us to run a portal with a timeline of ADRs

The "Log4brains patch" performs the following modifications to the original template:

- Change the ADR filenames format (`NNN-adr-name` becomes `YYYYMMDD-adr-name`), to avoid conflicts during Git merges.
- Add a `Tags` field.

### Additional Information

We will implement Architectural Decision Records (ADRs) with immediate effect;

- ADRs are to be authored and published with (at minimum) 1 TA as decider;
- ADRs will be formatted using MADR 2.12 with log4Brains Patches format;
- ADRs are to be used to log system-wide decisions;
- Should the system consist of multiple code-repositories, ADRs should live in the main system repository;
- ADRs are to be stored in a subfolder docs/decisions/ of the repository for the software affected;
- ADRs will follow a flat filename convention with relevant components in their filename

## Links

- Relates to [RFC-0016](https://input-output.atlassian.net/wiki/spaces/ATB/pages/3580559403/RFC+0016+-+Use+Architectural+Design+Records)
