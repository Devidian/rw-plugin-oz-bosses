# Encounter spawning, combat and reward handling
<!-- codemap-id: rw-plugin-oz-bosses/encounters -->

## Responsibility

Encounter spawning, combat and reward handling. BossRuntime wires handlers. The plugin owns boss-group rules and uses optional bridges for Wallet and Discord.

## Source and tests

- [`BossRuntime`](../../src/de/omegazirkel/risingworld/bosses/BossRuntime.java)
- [`BossSpawnHandler`](../../src/de/omegazirkel/risingworld/bosses/BossSpawnHandler.java)
- [`BossCombatHandler`](../../src/de/omegazirkel/risingworld/bosses/BossCombatHandler.java)
- [`BossThreatService`](../../src/de/omegazirkel/risingworld/bosses/BossThreatService.java)
- [`BossCombatHandlerTest`](../../src-test/de/omegazirkel/risingworld/bosses/BossCombatHandlerTest.java)
- [`WalletBridge`](../../src/de/omegazirkel/risingworld/bosses/WalletBridge.java) preserves the local bounty API and delegates optional lookup to OZ Tools. [`WalletBridgeTest`](../../src-test/de/omegazirkel/risingworld/bosses/WalletBridgeTest.java) covers missing and replaced providers.

See the [repository index](../README.md) for lifecycle, build and cross-repository dependencies.
