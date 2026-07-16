# AstraMesh Tasks

## 1. Goal
Break AstraMesh into buildable stages so each step compiles, can be tested, and moves the project toward a complete offline mesh communication system.

The project is phone-first, with optional PC companion support and a separate promotional website.

## 2. Repository Structure
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

## 3. Stage Plan
### Stage 0: Repository bootstrap
- initialize Gradle project
- set Kotlin and Compose versions
- create app module
- create core modules
- add navigation shell
- add docs folder
- add GitHub Actions skeleton

Done when: project syncs, build succeeds, empty app launches.

### Stage 1: Domain and protocol foundation
- define Node, Packet, Message, Broadcast, FileTransfer, DeliveryState
- define packet serialization
- define protocol versioning
- add unit tests for models

### Stage 2: Local persistence
- configure Room
- add entities and DAOs
- implement repositories
- persist drafts and pending packets
- persist received packets

### Stage 3: Discovery engine
- implement BLE advertisement wrapper
- implement BLE scanning wrapper
- exchange capability payloads
- register discovered peers in DB
- show nearby peers in UI

### Stage 4: Session and transport layer
- implement transport abstraction
- implement BLE GATT transport
- create session handshake flow
- exchange public metadata
- create send and receive pipelines

### Stage 5: Routing engine
- implement deduplication cache
- implement TTL enforcement
- implement epidemic relay logic
- implement best-neighbor relay selection
- implement store-and-forward queue
- implement ACK forwarding

### Stage 6: Chat UI
- chat list screen
- chat thread screen
- delivery states
- reply support
- pending and failed states

### Stage 7: File sharing
- file picker
- chunk files
- encrypt chunks
- relay chunks
- reassemble chunks
- verify hashes

### Stage 8: Emergency broadcast
- broadcast composer
- priority queue behavior
- broadcast relay
- broadcast feed

### Stage 9: PC companion support
- desktop module
- show incoming messages
- show nearby device state
- allow desktop to act as relay node

### Stage 10: Security hardening
- key generation
- handshake key exchange
- encrypt payloads end to end
- sign or verify metadata
- store private keys locally

### Stage 11: GitHub workflows
- Android CI workflow
- desktop build workflow if needed
- website build workflow
- upload APK artifacts
- upload test reports

### Stage 12: Promotional website
- create Next.js app in `web/`
- landing page
- feature sections
- architecture summary
- screenshots section
- demo/video section

## 4. Priority Order
1. repository bootstrap
2. protocol models
3. local storage
4. discovery
5. session transport
6. routing
7. chat UI
8. file sharing
9. broadcast
10. PC companion
11. security hardening
12. GitHub workflows
13. promotional website
