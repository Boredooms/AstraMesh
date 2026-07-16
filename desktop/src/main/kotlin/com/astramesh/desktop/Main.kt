package com.astramesh.desktop

/**
 * Optional AstraMesh PC companion node.
 *
 * The desktop companion is NOT required for the phone-first MVP. It reuses the shared
 * pure-Kotlin protocol/routing/security modules so a laptop can observe and relay the
 * same mesh packets on a larger screen during demos.
 *
 * Real transport + dashboard wiring lands in Stage 9.
 */
fun main() {
    println("AstraMesh desktop companion — protocol v${com.astramesh.protocol.ProtocolVersion.CURRENT}")
    println("This is an optional relay/observer node. The mobile app works without it.")
}
