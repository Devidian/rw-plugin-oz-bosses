# History / Changelog / Commitlog

<https://www.conventionalcommits.org/en/v1.0.0/>

## [0.1.3] - 2026-08-05 | Debug safety

- fix: keep administrator debug messages null-safe when unnamed NPCs are hit

## [0.1.2] - 2026-07-31 | Configurable spawn boundaries

- feat: optionally prevent automatic boss spawns inside Rising World Areas
- feat: configure a maximum number of active boss groups per sector
- fix: expose the new spawn settings in PluginSettings with German and English labels
- fix: remove the redundant overlay command-status hint

## [0.1.1] - 2026-07-25 | Mining drill threat

- fix: count mining drill terrain changes with the configured pickaxe threat
- build: publish canonical GitHub release metadata for OZ Tools update checks

## [0.1.0] - 2026-07-24 | Initial boss encounters

- fix: derive live-NPC rehydration and spawn boundaries from 256 chunks per sector axis
- balance: add per-group health progression and inverse random-spawn weights
- feat: add separate male/female names and tiered loot tables for every default group
- feat: add relative per-group weighting for random boss spawns
- feat: support per-group boss/follower health scaling, follower NPC, minimum-distance overrides, and admin dropdown names
- refactor: reduce `Bosses` to lifecycle wiring and delegated Rising World event methods
- refactor: move runtime composition, state models, persistence migrations, threat rules, settings metadata, and overlay data into feature-owned classes
- refactor: remove the unused Mail bridge and static plugin/UI access paths
- i18n: localize administrator debug diagnostics, loot fallback messages, bounty feedback, and admin permission errors
- refactor: route event logic through feature handlers while retaining all Rising World event bindings in `Bosses`
- feat: package overwriteable default boss names, groups, and loot JSON files; create editable runtime copies on first start
- fix: derive automatic and manual spawn choices from `groups.json`, including groups added after deployment
- fix: retain active boss-group identity and configured name/loot keys across plugin reloads
- build: update the shared OZ Tools dependency to version 0.23.8

## [0.0.1] - 2026-07-21

- change: rename template and shared menu icon keys to their final semantic names
- feat: adopt Plan 04 shortcut visibility and runtime standard conventions
- build: update the OZTools baseline dependency and CI checkout reference to `0.21.0`
- build: update the OZ Tools baseline dependency to `0.23.1`

- feat: add canonical optional WalletBridge scaffold and shared Info/Status radial-menu action
- refactor: route the template status command to the shared Tools Info/Status panel
- feat: scaffold shared Tools UI, indicator, info/status, grouped settings, and logger conventions
- refactor: remove feature-plugin integration helper stubs from the generic template baseline
- build: align template Tools dependency with 0.18.0 shared settings baseline
- feat: add admin-only `PluginSettings` metadata registration example
- feat: default `reloadOnChange` to true in template settings
- docs: add synchronized portfolio `DESIGN.md` baseline
- fix: restore colored one-line plugin welcome message
- docs: standardize agent prompts, PR checklist, and runtime smoke-test guidance
- build: add API verification helper and stricter CI/release validation flow
- build: package only `README.md` and `HISTORY.md` into release artifacts
