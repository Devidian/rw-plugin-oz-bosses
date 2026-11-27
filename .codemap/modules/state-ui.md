# Boss state persistence, rehydration, settings and inspection
<!-- codemap-id: rw-plugin-oz-bosses/state-ui -->

## Responsibility

Boss state persistence, rehydration, settings and inspection. Preserve editable runtime JSON and rehydration behavior. State belongs to Bosses.

## Source and tests

- [`BossStateRepository`](../../src/de/omegazirkel/risingworld/bosses/BossStateRepository.java)
- [`BossGroupPersistence`](../../src/de/omegazirkel/risingworld/bosses/BossGroupPersistence.java) atomically replaces group, member and damage snapshots. An incomplete load blocks replacement until a complete load succeeds.
- [`BossGroupPersistenceTest`](../../src-test/de/omegazirkel/risingworld/bosses/BossGroupPersistenceTest.java) exercises write rollback, incomplete load and transaction ownership.
- [`BossGroupRehydrationHandler`](../../src/de/omegazirkel/risingworld/bosses/BossGroupRehydrationHandler.java)
- [`BossOverlay`](../../src/de/omegazirkel/risingworld/bosses/ui/BossOverlay.java)
- [`PluginSettings`](../../src/de/omegazirkel/risingworld/bosses/PluginSettings.java)

See the [repository index](../README.md) for lifecycle, build and cross-repository dependencies.
