# AstraMesh Implementation Plan

## Objective
Implement AstraMesh as a modular, compile-safe, phone-first offline mesh network with optional PC companion support and a separate Next.js promotional website.

## Recommended Final Folder Structure
```text
AstraMesh/
├─ app/
├─ core/
│  ├─ domain/
│  ├─ protocol/
│  ├─ routing/
│  ├─ security/
│  ├─ transport/
│  └─ persistence/
├─ feature-discovery/
├─ feature-chat/
├─ feature-files/
├─ feature-broadcast/
├─ feature-settings/
├─ desktop/
├─ web/
├─ docs/
├─ screenshots/
└─ .github/workflows/
```

## Implementation Principles
- build the protocol before the UI
- persist state before adding relay logic
- keep transport behind interfaces
- keep desktop optional
- keep the web app separate
- make every stage compile
- add tests as soon as core logic exists

## Stage-by-Stage Implementation
Stage 0: Bootstrap. Create Gradle config, app shell, module structure, navigation shell, initial GitHub workflow, empty website scaffold.
Stage 1: Protocol models. Create Node, Packet, Message, Broadcast, FileTransfer, serialization tests.
Stage 2: Persistence. Add Room, entities, database, repositories, local queue.
Stage 3: Discovery. Add BLE discovery, advertising, peer list, capability exchange, peer status UI.
Stage 4: Transport. Add transport abstraction, GATT session path, send/receive pipeline, connection health.
Stage 5: Routing. Add deduplication cache, TTL rules, relay selection, ACK flow, retry queue.
Stage 6: Chat UI. Build chat list, thread view, delivery states, compose box, sent/pending/delivered indicators.
Stage 7: File sharing. Add file picker, chunking, checksum verification, transfer progress, reassembly.
Stage 8: Broadcast. Add broadcast composer, broadcast relay, broadcast history, priority queue support.
Stage 9: PC companion. Add desktop message viewer, optional relay mode, local logs, network dashboard.
Stage 10: Security hardening. Add handshake, session keys, payload encryption, integrity checks, replay protection.
Stage 11: GitHub workflows. Add Android build, test, website build, artifact upload workflows.
Stage 12: Promotional website. Build landing page, feature sections, architecture section, screenshots section, demo section.

## Implementation Order
1. repo bootstrap
2. protocol models
3. persistence
4. discovery
5. transport
6. routing
7. chat UI
8. file sharing
9. broadcast
10. desktop companion
11. security hardening
12. GitHub workflows
13. website

## Code Ownership by Module
core/domain: entities and use cases
core/protocol: packet models and serialization
core/routing: relay logic, deduplication, retry, TTL
core/security: encryption, keys, signatures, verification
core/transport: BLE and transport abstractions
core/persistence: Room entities, DAOs, repositories
feature modules: Compose UI and ViewModels
desktop: optional PC node and dashboard
web: promotional website

## CI / Workflow Targets
GitHub Actions should run tests, assemble APK, build website, upload artifacts.

## Screenshot Plan
Store screenshots in screenshots/. Recommended screenshots: nearby peers, chat thread, message delivered state, file transfer state, emergency broadcast state, desktop companion dashboard, website homepage.

## Demo Plan
Open app on two or three phones, show discovery, send a message from phone A to phone C through phone B, disconnect one device and show store-and-forward, show file transfer, show broadcast, show PC companion if available, show promotional website.

## Quality Bar
The implementation is acceptable only if it compiles, is modular, has tests, is explained in docs, and demonstrates real message flow end to end.
