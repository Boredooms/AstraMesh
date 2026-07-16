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
| 1 | Domain + protocol models + serialization tests | ✅ 17 tests |
| 2 | Room persistence | ✅ done |
| 3 | BLE discovery | ✅ done |
| 4 | Transport abstraction | ✅ discovery; GATT send stubbed |
| 5 | Routing / relay / store-and-forward | ✅ 3 mesh integration tests |
| 6 | Chat UI | ✅ conversation list + thread + peer picker + diagnostics |
| 6.1 | End-to-end messaging demo (handshake, ACK, store-and-forward, relay) | ✅ 9 mesh integration tests |
| 7 | File transfer | planned |
| 8 | Emergency broadcast | planned |
| 9 | Optional PC companion | planned |
| 10 | Security hardening | planned |
| 11 | GitHub workflows | ✅ scaffolded |
| 12 | Promotional website (`/web`) | ✅ Next.js + Tailwind + shadcn-style UI |

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
core-mesh/           MeshCoordinator + SessionKeyManager, wires transport/   (Android)
                     routing/security/persistence into send+receive, DI'd
core-ui/             shared black-monochrome design tokens + components     (Android/Compose)
                     (theme, spacing/radius scale, MessageBubble, chips)
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

## Download the APK

- **Tagged releases** (permanent): [GitHub Releases](../../releases) — push a `v*` tag (e.g.
  `v0.1.0`) to trigger [`release.yml`](.github/workflows/release.yml), which builds, tests, and
  attaches `AstraMesh-<tag>.apk` to a new GitHub Release.
- **Every push to `main`** (expires after 90 days): the `astramesh-debug-apk` artifact on the
  latest successful run of [`android.yml`](.github/workflows/android.yml), under the
  [Actions](../../actions) tab.

The APK is debug-signed (Android's default debug keystore) so it installs directly on a device
after enabling "install from unknown sources" — no separate signing step required.

## Principles

- Offline-first — no server required for core messaging.
- Encryption by default — no plaintext on the wire.
- Every node can relay — store-and-forward keeps messages alive across disconnects.
- Local ownership — data stays on device unless intentionally shared.
- PC support is optional and never blocks the mobile MVP.

## License

TBD.
