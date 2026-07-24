# Runtime service extraction

## Objective

Keep `Bosses` as the registered Rising World listener and lifecycle composition root. Move threat, spawning/group lifecycle, combat, and loot behavior into thematic classes under `bosses/`.

## Constraints

- `registerEventListener(this)` remains in `Bosses.onEnable()`.
- All `@EventMethod` annotations remain in `Bosses` and delegate to initialized services.
- No behavioral change to runtime JSON, persistence, or player/admin UI contracts.
- Migrate the legacy inner `Settings` record to the template-standard external `bosses.PluginSettings` class while preserving every existing `settings.properties` key and default.

## Checklist

- [x] Extract threat calculation and player-world event behavior.
- [x] Extract active-group administration and inspection behavior.
- [x] Extract loot creation and delivery behavior.
- [x] Extract spawn and active-group lifecycle behavior.
- [x] Keep combat behavior in `BossCombatHandler` and remove its `Bosses` state access.
- [x] Migrate configuration to template-standard `bosses.PluginSettings`.
- [x] Move schema initialization and migration SQL into `BossStateRepository`.
- [x] Move shared stateless helpers into `BossUtils`.
- [x] Move state models, view models, settings metadata, and debug state out of `Bosses`.
- [x] Localize all administrator debug output in German and English.
- [x] Build and verify the packaged listener surface locally.
- [ ] Deploy to the development server and verify listener registration after reload.

## Result

`Bosses` now contains only lifecycle callbacks and the registered delegated
`@EventMethod` methods. It no longer exposes feature methods, static plugin
state, persistence SQL, view records, or utility logic. `BossRuntime` composes
the focused services, while persistence, threat, state, settings, view data,
debug output, and utilities have explicit owners.

Local validation completed with a writable temporary Maven repository and clean
temporary build copy:

- `mvn -B test` (no test sources)
- `mvn -B -DskipTests package`
- `scripts/verify-plugin-api.sh --summary`
- bytecode inspection confirmed `Bosses` is the only `Listener` and exposes no
  feature bridge methods
- packaged ZIP inspection confirmed both localized debug catalogues

The development-server deployment remains intentionally open because the
external deployment action was not authorized in this session.
