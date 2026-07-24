# OZ Bosses

Sector-based boss events for Rising World. Player activities increase persistent sector threat; eligible sectors spawn configurable boss groups. `/ozboss` shows the ranking; admins can use `/ozboss spawn` in their current sector.

`settings.properties` controls thresholds, NPC IDs, scaling, and optional Wallet bounty. The packaged
`names.default.json`, `groups.default.json`, and `loot.default.json` are overwritten on every plugin update.
On first start, the plugin copies each one to its editable runtime counterpart: `names.json`, `groups.json`, and
`loot.json`.

Each entry in `loot.json` has an item definition name, a relative `weight`, and `minStack`/`maxStack`. `groups.json`
maps each NPC type to its name catalogue and loot table. This keeps group composition, names, and loot editable without
changing plugin code.

## Runtime architecture

- `Bosses` is the only Rising World `Listener`. It contains lifecycle wiring and
  delegated `@EventMethod` entry points only.
- `BossRuntime` composes the feature services without exposing a public API from
  the plugin entry class.
- Boss state, persistence, threat handling, spawning, combat, rewards, settings,
  overlays, and localized diagnostics are owned by focused classes under
  `de.omegazirkel.risingworld.bosses`.
- `BossUtils` contains the small stateless helpers shared across those services.

Use this repository as template for new Rising World Plugins.

## Files included

- [.github/workflows/ci.yml](.github/workflows/ci.yml)
  - for GitHub Action, you have to change `Devidian/rw-plugin-maven-template` here (2x)
- [src/assembly/rw-plugin-maven-template.xml](src/assembly/rw-plugin-maven-template.xml)
  - this is needed to pack you plugin as zip, change atleast directory/outputDirectory here
  - change name to your `pom.project.artifactId`
  - i use `pom.project.name` for directory/outputDirectory
- [src/resources/plugin.yml](src/resources/plugin.yml)
  - your plugin definition file, change as you need
- [src/de/omegazirkel/risingworld/MavenTemplate.java](src/de/omegazirkel/risingworld/MavenTemplate.java)
  - sample main file for your plugin, change name and path as you need (dont forget to change it in plugin.yml too)
- [pom.xml](pom.xml)
  - maven file, change as you need it
- [HISTORY.md](HISTORY.md)
  - for your changelog
- [README.md](README.md)
  - this file, override it as you like
- [DESIGN.md](DESIGN.md)
  - synchronized portfolio UI/design baseline; keep it aligned with the root copy

## Baseline behavior

- Requires `rw-plugin-oz-tools`.
- Uses the shared file watcher path by implementing `FileChangeListener`; changes
  to `settings.properties` reload plugin settings.
- Defaults `reloadOnChange=true` in `settings.default.properties`.
- Registers a shared inventory overlay button through `InventoryOverlayButtons`
  so players get a compact entrypoint below the inventory.
- Registers a default-visible shortcut visibility provider through
  `PluginShortcutVisibility`; real plugins should connect this to a persisted
  player setting when they expose player preferences.
- Registers a `SharedIndicators` provider stub. The template returns `false` by
  default; real plugins should only show indicators when they have meaningful
  player-specific state.
- Registers a `PluginInfoStatusProvider` with generic RichText info/status
  content for the shared Tools Info/Status panel.
- Adds an `Info / Status` action to the plugin-owned radial menu. It uses the
  Tools-registered `info-status` asset key; generated plugins should not
  register a duplicate copy of that shared icon.
- Registers player settings, player data, and admin-only `PluginSettings`
  metadata with `PlayerPluginSettingsOverlay`.
- Includes an optional reflection-based `WalletBridge` scaffold for economy
  integrations. It covers Wallet availability, default currency, currency
  listing, currency registration, deposit, withdraw, balance, and default
  currency convenience calls without a compile-time Wallet dependency.
- Includes grouped sample admin settings metadata for booleans and strings, plus
  a hidden sensitive value example that should be replaced or removed in real
  plugins.
- Uses one main plugin logger name. Helper classes should call the main plugin
  logger instead of creating subsystem logger names.

## Shared Tools conventions

Future plugins generated from this template should route shared infrastructure
through `rw-plugin-oz-tools`:

- UI entrypoints: use `InventoryOverlayButtons` for compact inventory actions and
  `PluginMenuManager` for the `/ozt` main plugin menu. Register
  `PluginShortcutVisibility` with a default-visible per-player predicate when a
  plugin lets players hide shared shortcuts.
- Indicators: use `SharedIndicators` for reusable HUD indicator slots. Return an
  `AssetManager` icon key from the provider, not a file path.
- Info/status: expose player-facing RichText through `PluginInfoStatusProvider`
  and open it with `PluginInfoStatusProviders.show(player, pluginName)` from
  plugin-owned buttons, menu items, or commands when appropriate. Use the shared
  `info-status` icon key for radial Info/Status buttons.
- Wallet: use the template `WalletBridge` pattern for optional economy
  integrations. Keep feature-specific spending and fulfillment rules inside the
  generated plugin, and disable economy features when Wallet is unavailable.
- Settings: register admin metadata through `PlayerPluginSettingsOverlay`; use
  `AdminSettingsEntry.group(...)` for sections and `AdminSettingsType.INTEGER`
  for numeric settings so Tools can apply shared numeric input filtering.
- i18n: load the plugin i18n instance once during `onEnable()` with
  `I18n.getInstance(this)`. Other classes may use `I18n.getInstance(pluginName)`
  after enable.
- Persistence: use `SQLiteConnectionFactory.open(this)` and repository-local
  stores for runtime data. Do not use the deprecated Tools `SQLite` class in new
  plugins.
- Escape behavior: rely on explicit close controls until the Rising World API
  provides its planned custom-overlay Escape layer.
- Common UI helpers and runtime watchers should use Tools contracts instead of
  duplicating helper code in feature plugins.

## Contributor Workflow

- Review `AGENTS.md`, `PLANS.md`, `.codex/agents.toml`, and `.codex/skills/` before making structural changes.
- Verify Rising World API usage with `scripts/verify-plugin-api.sh` when adding or changing API calls.
- Run `mvn -B -DskipTests package` and `mvn -B test` before release-facing changes are merged.
- Use `RUNTIME_TESTING.md` and `scripts/docker-runtime-smoke.sh <PluginFolderName>` for runtime smoke tests when behavior changes need server validation.
- Keep `README.md` and `HISTORY.md` current and use Conventional Commit titles for commits and PRs.
