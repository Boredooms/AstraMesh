# AstraMesh

**A phone-first, offline mesh communication system.** Every Android device becomes a node that
discovers nearby peers over Bluetooth LE, exchanges end-to-end encrypted packets, and relays
them hop-by-hop — with no internet, cellular, or central server. Messages are stored locally and
delivered via store-and-forward when a recipient is temporarily offline.

> Built for disaster zones, infrastructure collapse, remote regions, and local-area coordination.

---

## Status

| Stage | Scope | State |
|------|-------|-------|
| 0 | Gradle multi-module skeleton, CI, app shell | ✅ builds |
| 1 | Domain + protocol models + serialization tests | in progress |
| 2 | Room persistence | planned |
| 3 | BLE discovery | planned |
| 4 | Transport abstraction (BLE GATT) | planned |
| 5 | Routing / relay / store-and-forward | planned |
| 6 | Chat UI | planned |
| 7 | File transfer | planned |
| 8 | Emergency broadcast | planned |
| 9 | Optional PC companion | planned |
| 10 | Security hardening | planned |
| 11 | GitHub workflows | ✅ scaffolded |
| 12 | Promotional website (`/web`) | planned |

## Architecture

Layered, modular, and transport-agnostic. Pure-Kotlin core (protocol/domain/routing/security) is
JVM-unit-testable and shared with the optional desktop companion; Android-specific concerns
(persistence, transport, UI) live in their own modules.

```
app/                 Android entry point, navigation shell, theme
core-domain/         entities, use cases, repository + routing interfaces   (pure Kotlin)
core-protocol/       packet envelope, packet types, serialization           (pure Kotlin)
core-routing/        epidemic relay, dedup, TTL, retry, ACK                 (pure Kotlin)
core-security/       keys, handshake, authenticated encryption              (pure Kotlin)
core-transport/      transport abstraction + BLE GATT                       (Android)
core-persistence/    Room entities, DAOs, repositories                      (Android)
feature-discovery/   nearby peers UI
feature-chat/        chat UI
feature-files/       file transfer UI
feature-broadcast/   emergency broadcast UI
feature-settings/    identity, keys, diagnostics UI
desktop/             optional PC companion node                            (Kotlin/JVM)
web/                 promotional site                                       (Next.js, separate)
docs/                source of truth
```

See [`docs/architecture.md`](docs/architecture.md), [`docs/protocol.md`](docs/protocol.md),
and [`docs/workflow.md`](docs/workflow.md).

## Build

Requires JDK 17 and the Android SDK (compileSdk 35). The Gradle wrapper is committed.

```bash
./gradlew assembleDebug   # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew test            # JVM unit tests
```

Minimums: `minSdk = 26`, `targetSdk = 35`.

## Principles

- Offline-first — no server required for core messaging.
- Encryption by default — no plaintext on the wire.
- Every node can relay — store-and-forward keeps messages alive across disconnects.
- Local ownership — data stays on device unless intentionally shared.
- PC support is optional and never blocks the mobile MVP.

## License

TBD.
