# AGENTS.md

## Agent Operating Rules
This repository is meant for agentic development. Work in small, verifiable steps.

Always:
- keep the project compiling
- add tests alongside core logic
- update docs when architecture changes
- prefer stable APIs and standard libraries
- avoid unnecessary complexity

Never:
- put UI logic inside transport code
- put Bluetooth code inside composables
- rely on a server for core features
- ignore local persistence
- ship placeholder routing logic without clearly labeling it

## Repo Responsibilities
- app/: Android application entry point and navigation shell
- core/: shared business logic and infrastructure
- feature-*/: UI features for discovery, chat, files, broadcast, and settings
- desktop/: optional PC companion
- web/: promotional Next.js site
- docs/: source of truth for architecture and implementation intent
- .github/workflows/: CI and artifact automation

## Change Policy
Safe changes:
- add models
- add tests
- add UI screens
- add repositories
- add workflows
- add documentation

Higher-risk changes:
- transport refactors
- protocol changes
- persistence migrations
- encryption changes
- routing algorithm changes

For higher-risk changes:
- make them in small commits
- keep compatibility in mind
- update protocol docs
- add regression tests

## Engineering Priorities
1. compile success
2. protocol correctness
3. message reliability
4. secure transport
5. clean UI
6. demo quality
7. optional desktop support
8. polished promotional website

## Testing Expectations
Add tests for packet serialization, deduplication, routing decisions, ACK handling, file chunk reassembly, encryption/decryption, database persistence behavior.

## GitHub Workflow Expectations
The repo should include workflows that build the Android app, run tests, build the website, upload APK artifacts, and upload reports.

## Desktop Support Rule
Desktop support is optional. It may be used to show incoming messages, act as a relay hub, and help during the demo. The mobile app must remain fully useful without it.

## Promotional Website Rule
The `web/` folder should contain a separate Next.js project that explains the problem, explains the solution, shows architecture, shows screenshots, and gives the hackathon pitch.

## Completion Standard
A task is complete only if code compiles, tests pass, docs are updated, and behavior is demonstrated end to end where relevant.
