# History / Changelog / Commitlog

<https://www.conventionalcommits.org/en/v1.0.0/>

## Unreleased

- refactor: reduce `Bosses` to lifecycle wiring and delegated Rising World event methods
- refactor: move runtime composition, state models, persistence migrations, threat rules, settings metadata, and overlay data into feature-owned classes
- refactor: remove the unused Mail bridge and static plugin/UI access paths
- i18n: localize administrator debug diagnostics, loot fallback messages, bounty feedback, and admin permission errors
- refactor: route event logic through feature handlers while retaining all Rising World event bindings in `Bosses`
- feat: package overwriteable default boss names, groups, and loot JSON files; create editable runtime copies on first start
- fix: derive automatic and manual spawn choices from `groups.json`, including groups added after deployment
- fix: retain active boss-group identity and configured name/loot keys across plugin reloads

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
