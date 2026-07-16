# AstraMesh

## 1. Project Idea

AstraMesh is a decentralized, offline-first communication system for Android phones and PCs that allows nearby devices to discover each other, exchange messages, relay data across hops, and persist content locally without requiring internet, cellular networks, or a central server.

The system is designed for disaster scenarios, infrastructure collapse, remote regions, campus/local-area communication, and hackathon demonstrations where the network must keep working even when traditional online services are unavailable.

## 2. Core Goal

Build a working end-to-end distributed communication platform where every device can act as:

- a user device
- a discovery node
- a message relay node
- a local storage node
- an offline sync node

The app should support:

- peer discovery over Bluetooth Low Energy
- optional Wi-Fi Direct or local network fallback
- encrypted peer-to-peer messaging
- store-and-forward delivery
- file sharing for images, text, and small documents
- emergency broadcasts
- local message history using SQLite / Room
- Android and desktop support where possible

## 3. What Makes It Different

Most chat apps assume internet connectivity and a central backend. AstraMesh does not.

Its main idea is:

- every device becomes part of the network
- messages travel through nearby devices hop by hop
- a message can still arrive later even if the recipient is offline right now
- the network self-organizes through peer discovery and relay logic

This makes it a distributed systems project, not just a chat app.

## 4. Target Platforms

### Primary target
- Android phones

### Secondary target
- Desktop PCs and laptops

### Why both matter
- phones are the most important field devices
- PCs can act as stronger relay hubs, local servers, or community nodes
- a laptop can store more messages, cache files, and run a larger local UI

## 5. Product Vision

AstraMesh should feel like:

- a secure offline messenger
- a local emergency communication system
- a self-healing mesh relay network
- a distributed data exchange layer for nearby devices

The user should be able to install the app and immediately start discovering nearby nodes, sending messages, and relaying data without logging into any online account.

## 6. Key Principles

1. Offline first
   - The app must work without internet.

2. No central server
   - The network should function peer to peer.

3. Every node can relay
   - Any device can forward messages for others.

4. Persistent delivery
   - Messages are stored until delivered or expired.

5. Encryption by default
   - Messages must be protected end to end.

6. Local ownership
   - User data stays on the device unless intentionally shared.

7. Graceful degradation
   - If Bluetooth is weak, use other local transports where available.

## 7. System Overview

AstraMesh is built as layered distributed software:

### Layer 1: Discovery
Find nearby devices and establish adjacency.

### Layer 2: Transport
Create actual peer connections for message exchange.

### Layer 3: Routing
Decide how messages move across multiple hops.

### Layer 4: Storage
Persist messages, peers, retries, and delivery state.

### Layer 5: Security
Encrypt messages and authenticate peers.

### Layer 6: Application
Chat UI, file sharing, emergency broadcast, and status screens.

## 8. How the Network Works

### Step 1: Discovery
Each device advertises its presence and scans for others.

Recommended discovery methods:
- Bluetooth Low Energy advertisements
- Bluetooth GATT services
- Optional Wi-Fi Direct discovery
- Optional local network discovery when available

### Step 2: Pairing / Session Setup
When two devices detect each other, they establish a session.

The session should include:
- node identifier exchange
- capability exchange
- protocol version exchange
- public key exchange
- trust / handshake confirmation

### Step 3: Message Exchange
Messages are sent in small packets so they can move through limited-bandwidth links.

### Step 4: Relay
If the destination is not directly reachable, the message is forwarded to another node.

### Step 5: Store and Forward
If no next hop is available, the node stores the message locally and retries later.

### Step 6: Delivery Confirmation
When the receiver gets the message, it sends an acknowledgment back through the network.

## 9. Bluetooth Role in the System

Bluetooth is the first transport layer because it is widely available on phones and laptops.

Important design note:
- Bluetooth is not treated like a single central server.
- Every device can expose services and consume services.
- One device may advertise, another may scan, and both may relay.

This creates a peer-to-peer distributed topology.

### Bluetooth responsibilities
- peer discovery
- short-range transport
- initial handshake
- message relay
- low-power fallback connectivity

### Bluetooth limitations
- short range
- low bandwidth
- device-specific OS restrictions
- background execution limits on mobile platforms

Because of these limits, the architecture should allow optional fallback transports.

## 10. Optional Transport Fallbacks

The project should be designed so the transport layer can support multiple backends.

Recommended transport priority:

1. Bluetooth Low Energy
2. Wi-Fi Direct
3. Local Wi-Fi / hotspot mode
4. USB / desktop relay mode where possible

This does not mean every platform must use every transport in the same way. The system should choose whatever the device supports.

## 11. Distributed Systems Design

AstraMesh should follow distributed systems patterns:

### Gossip membership
Nodes periodically exchange who they know about.

### Epidemic / flooding relay
Messages are forwarded to reachable peers with deduplication.

### Store-and-forward delivery
Messages survive temporary disconnections.

### Acknowledgment-based reliability
Delivery state is tracked using ACKs.

### Deduplication
Each message must have a unique ID so the same message is not relayed forever.

### TTL / hop count
Messages must expire after a certain number of hops or time.

## 12. Routing Model

The routing engine should be simple enough to implement and reliable enough to demo.

### Recommended routing strategy for MVP
- epidemic relay for nearby propagation
- store-and-forward for offline recipient delivery
- message deduplication by message ID
- TTL to prevent infinite loops
- ACK packets for confirmed delivery

### Future routing upgrades
- encounter-based routing
- trust-aware routing
- social-aware routing
- priority-based emergency routing
- CRDT-based shared state sync

## 13. Message Types

The protocol should support multiple packet types.

### Chat message
Normal user-to-user message.

### Relay packet
A packet forwarded through intermediate nodes.

### ACK packet
Confirms that a message was received.

### Presence packet
Signals that a node is available.

### File chunk packet
Carries a fragment of a file.

### Broadcast packet
Sends an announcement to many nodes.

### Health packet
Reports battery, storage, or connection state if allowed.

## 14. Message Format

Every message should include:

- unique message ID
- sender node ID
- destination node ID or broadcast flag
- timestamp
- TTL
- hop count
- message type
- encrypted payload
- optional signature
- optional checksum

## 15. Encryption and Security

All content should be encrypted end to end.

### Security goals
- confidentiality
- integrity
- authenticity
- replay protection
- deduplication without exposing plaintext

### Security approach
- public key exchange during handshake
- session key generation for message exchange
- encrypted payloads using a modern authenticated encryption scheme
- digital signatures for verification where needed

### Security policy
- no login required
- no cloud account required
- no server-side storage required
- no plaintext message relay

## 16. Local Storage

Use local persistent storage so messages remain available when devices disconnect.

### Recommended storage
- SQLite / Room on Android
- SQLite or a local embedded store on desktop

### Store locally
- peer list
- routing cache
- pending messages
- delivered messages
- failed messages
- file metadata
- ACK status
- trust metadata if used later

## 17. File Sharing

The app should support sending small files offline.

### Supported file types
- images
- PDFs
- text files
- short documents
- small media attachments if bandwidth allows

### File transfer design
- split files into chunks
- send chunks through the same relay layer
- reassemble on the destination device
- verify integrity using checksums or hashes

## 18. Chat UX

The user interface should stay simple.

### Main screens
- device discovery screen
- chat screen
- nearby nodes screen
- pending messages screen
- emergency broadcast screen
- file sharing screen
- settings screen

### Chat UX expectations
- show nearby peers
- show connection quality
- show delivered / pending / relayed states
- show hop count or relay path if useful
- show offline status clearly

## 19. Emergency Broadcast Mode

This is one of the strongest demo features.

Broadcast messages should:
- propagate to multiple nodes
- survive temporary disconnection
- be marked as high priority
- optionally bypass lower-priority queue items

Example use cases:
- medical emergency
- shelter location
- food distribution
- rescue coordination

## 20. Demo Flow

A strong demo should show the full system working end to end.

### Demo 1: Discovery
Open the app on multiple devices and show nearby nodes appearing automatically.

### Demo 2: Direct message
Send a message between two nearby devices.

### Demo 3: Multi-hop relay
Send a message from Device A to Device C through Device B.

### Demo 4: Offline delivery
Turn off one recipient device, send a message, then reconnect and show the message arriving later.

### Demo 5: File sharing
Send a small image or PDF offline.

### Demo 6: Emergency broadcast
Send a broadcast and show it reaching multiple devices.

### Demo 7: Encryption story
Show that messages are encrypted and stored locally, not on a server.

## 21. What Must Be Built First

The order matters.

### Phase 1
- project bootstrap
- Gradle setup
- app structure
- local database
- basic UI

### Phase 2
- discovery engine
- peer list
- session handshake

### Phase 3
- message protocol
- relay engine
- deduplication
- ACKs

### Phase 4
- store-and-forward persistence
- pending queue
- retries

### Phase 5
- encryption
- signature verification
- file chunking

### Phase 6
- emergency broadcast
- UI polishing
- demo support
- GitHub Actions CI

## 22. Recommended Technology Stack

### Android
- Kotlin
- Jetpack Compose
- Material 3
- Coroutines
- Flow
- Room
- Hilt

### Desktop / shared logic
- Kotlin Multiplatform if time allows
- or separate desktop app with shared protocol library

### Protocol and routing
- pure Kotlin modules for packet handling and routing logic
- no UI dependencies inside protocol code

### Testing
- unit tests for protocol, routing, and storage
- UI tests for the main flows
- integration tests for discovery and relay logic where feasible

### CI/CD
- GitHub Actions
- build checks
- test checks
- APK artifact generation

## 23. Suggested Open Source Building Blocks

Use proven open source components where they fit the architecture.

### Android app structure
- Jetpack Compose
- Room
- Hilt
- Kotlin Coroutines
- Kotlin Serialization

### Bluetooth / peer connectivity
- Android Bluetooth LE APIs
- Android Nearby Connections API where applicable
- platform Bluetooth APIs on desktop where possible

### Security
- modern crypto library such as libsodium-compatible tooling where supported
- standard JVM crypto APIs where sufficient

### Desktop / local relay support
- Kotlin/JVM
- a small desktop service or companion app
- optional web-based local dashboard if time permits

The project should prefer stable official APIs first, then open source libraries where they reduce risk.

## 24. Repository Expectations

The repository should compile from a clean clone.

It should include:
- README
- idea.md
- architecture.md
- protocol.md
- routing.md
- tasks.md
- GitHub Actions workflow
- source code
- tests
- screenshots

## 25. Definition of Done

The project is complete when:

- the app builds successfully
- the app runs on Android
- nearby devices are discovered
- messages can be sent and relayed
- offline messages are stored and later delivered
- encrypted payloads are used
- files can be transferred in chunks
- the GitHub workflow produces build artifacts
- the demo clearly shows distributed message relay

## 26. Hackathon Story

AstraMesh is the communication layer for a world where the internet is unavailable.

It turns every device into a node in a resilient local network so people can still coordinate, share files, and send emergency updates even when central infrastructure fails.

## 27. One-Line Summary

AstraMesh is a secure offline mesh communication platform where every device becomes a relay node for chat, file sharing, and emergency coordination.
