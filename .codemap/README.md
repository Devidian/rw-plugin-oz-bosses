# rw-plugin-oz-bosses
<!-- codemap-id: rw-plugin-oz-bosses -->

## Purpose

Configurable boss groups, sector threat, combat, loot and persistence. The [plugin descriptor](../src/resources/plugin.yml) names [Bosses](../src/de/omegazirkel/risingworld/Bosses.java) as its lifecycle entry.

## Modules

- [encounters](modules/encounters.md) — Encounter spawning, combat and reward handling.
- [state-ui](modules/state-ui.md) — Boss state persistence, rehydration, settings and inspection.

## Build and tests

[Maven](../pom.xml) defines the Java build. Follow [repository validation](../AGENTS.md) for packaging, API checks and runtime validation. Relevant automated tests are under [src-test](../src-test/).

## Dependencies

- `rw-plugin-oz-tools/shared-api` — build and runtime shared infrastructure. [Evidence](../pom.xml)
- `rw-plugin-oz-wallet/ledger` — optional Wallet integration. [Evidence](../src/de/omegazirkel/risingworld/bosses/WalletBridge.java)
- `rw-plugin-oz-discord-connect/discord` — optional Discord integration. [Evidence](../src/de/omegazirkel/risingworld/bosses/DiscordBridge.java)
