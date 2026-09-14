# Threat, announcements, weapon variance, and loot verification

## Objective

Add configurable player-death threat (default 500), independently suppressible
spawn, level-up, defeat, and remaining-follower announcements, and vary NPC
weapon quality from Merciless through Godlike. Verify the existing loot-sack
delivery path and make any defect found locally reproducible.

## Ownership

Owning repository/plugin: `rw-plugin-oz-bosses`
Supporting repositories/plugins: `rw-plugin-oz-tools` (existing player settings and i18n only)

## Dependencies

- Runtime: existing PlayerDeath event, player-settings SQLite helper, boss loot sack storage.
- Build: Java 20 and the existing PluginAPI.
- Optional integrations: Discord remains unaffected by player-local announcement preferences.

## Risks

- New settings must preserve existing behaviour: death threat is additive and all announcements remain enabled by default.
- Weapon modifiers must be selected only from supported API modifiers.
- Loot tables are editable runtime data; validation must not overwrite them.

## Validation Strategy

- [ ] Add focused unit tests for announcement filtering, modifier selection, and loot-table parsing/rolls.
- [ ] `scripts/verify-entrypoint-architecture.sh`
- [ ] `scripts/verify-plugin-api.sh --summary`
- [ ] `mvn -B test` and `mvn -B -DskipTests package`
- [ ] Inspect packaged settings, loot defaults, and DE/EN catalogues.
- [ ] Scoped Development upload and reload/log verification.

## Affected Repositories/Plugins

- `rw-plugin-oz-bosses`

## Rollback Considerations

Set `threat.playerDeath=0` or restore the preceding artifact. Player preference
keys are additive and harmless to an older artifact; no boss persistence schema
changes are required.

## Implementation Checklist

- [x] Add death-threat setting and event handling.
- [x] Add four default-on player announcement preferences and filter only in-game delivery.
- [x] Select supported weapon modifiers with bounded variance.
- [x] Add loot validation and documentation.
- [x] Validate and deploy only Bosses to Development.
