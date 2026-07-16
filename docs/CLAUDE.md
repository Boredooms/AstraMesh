# CLAUDE.md

## Mission
Build AstraMesh as a phone-first offline mesh communication system with optional PC companion support and a separate promotional website.

The project must compile end to end and remain buildable from a clean clone.

## Core Expectations
- phone-to-phone communication is the primary MVP
- Bluetooth LE is the primary discovery and transport path
- every message must be encrypted before transmission
- local persistence must be used for all important state
- routing must support relay and store-and-forward
- PC support is optional and must not block the mobile MVP
- the promotional website belongs in `web/`
- GitHub Actions must compile and test the project

## Technology Stack
Android: Kotlin, Jetpack Compose, Material 3, Coroutines, Flow, Hilt, Room, Kotlin Serialization
Networking: Bluetooth LE, GATT sessions, optional Wi-Fi Direct fallback, optional desktop companion transport
Security: authenticated encryption, public key handshake, local key storage, deduplication and replay protection
Desktop: optional Kotlin/JVM desktop companion or relay node
Web: Next.js, React, Tailwind CSS
Build and CI: Gradle, GitHub Actions, APK artifact generation, test reports

## Architectural Rules
- UI must not talk directly to Bluetooth APIs
- routing logic must not live inside composables
- transport must be abstracted behind interfaces
- all packets must be versioned
- every packet must be stored before send when needed
- all important state must be persisted locally
- no backend server is required for the MVP

## Required Folder Structure
```text
AstraMesh/
├─ app/
├─ core/
├─ feature-discovery/
├─ feature-chat/
├─ feature-files/
├─ feature-broadcast/
├─ feature-settings/
├─ desktop/
├─ web/
├─ docs/
└─ .github/workflows/
```

## Build Rules for Claude Code
1. prefer small, compile-safe changes
2. do not leave placeholder methods unless explicitly marked as future stub
3. add tests for protocol and routing logic
4. keep modules independent and clearly named
5. update docs whenever architecture changes
6. keep the web app separate from the mobile app
7. ensure GitHub Actions stay green

## Implementation Strategy
First: project skeleton, protocol models, persistence entities, navigation shell.
Then: discovery, handshake, transport abstraction, routing and relay logic.
Then: chat, files, broadcasts, optional desktop companion.
Then: security, CI, website, screenshots.

## Definition of Done
- Android app compiles
- Bluetooth discovery works
- messages can be relayed
- offline delivery is supported
- files can transfer
- broadcasts can propagate
- PC companion can show messages if included
- website is present
- GitHub Actions builds the repo successfully
