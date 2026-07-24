# OZ Bosses

Sector-based boss events for Rising World. Player activities increase persistent sector threat; eligible sectors spawn configurable boss groups. `/ozboss` shows the ranking; admins can use `/ozboss spawn` in their current sector.

`settings.properties` controls thresholds, NPC IDs, scaling, and optional Wallet bounty. The packaged
`names.default.json`, `groups.default.json`, and `loot.default.json` are overwritten on every plugin update.
On first start, the plugin copies each one to its editable runtime counterpart: `names.json`, `groups.json`, and
`loot.json`.

Each entry in `loot.json` has an item definition name, a relative `weight`, and `minStack`/`maxStack`. `groups.json`
maps a stable group `key` to its administrator-facing `name`, boss `npc`, name catalogue, and loot table. The `name`
is shown in the administrator spawn dropdown. Existing definitions without a `name` remain valid and use the boss NPC
definition name as their label.

Group definitions may optionally override individual global boss settings:

```json
{
  "key": "bandit_elite",
  "name": "Elite bandits",
  "weight": 5,
  "npc": "bandit",
  "followerNpc": "barbarian",
  "nameType": "dummy",
  "lootTable": "default",
  "bossBaseHealth": 1800,
  "bossHealthPerLevel": 300,
  "followerBaseHealth": 450,
  "followerHealthPerLevel": 150,
  "minSpawnDistance": 120
}
```

Every override is independent. Missing health fields use `boss.baseHealth`, `boss.healthPerLevel`, or
`boss.followerHealth`; missing `followerHealthPerLevel` also uses `boss.healthPerLevel`. Missing `followerNpc` uses the
boss NPC type, and missing `minSpawnDistance` uses `boss.minSpawnDistance`. Base-health overrides must be positive;
per-level and distance overrides must be non-negative. Invalid values are ignored with a warning.

`weight` is a non-negative relative integer used only for random spawns. A group with weight `10` is selected twice as
often as a group with weight `5`; an omitted weight defaults to `1`. Weight `0` excludes the group from random spawns
while keeping it available for explicit administrator selection. If every configured group has weight `0`, no random
group is spawned.

The packaged defaults use the following level-one balance. Boss health grows by 25% of its base value per level;
follower health grows by 10% (the wolf boss increment is rounded to 188):

| Group | Random weight | Boss health / level | Follower health / level |
| --- | ---: | ---: | ---: |
| Skeleton | 200 | 500 / 125 | 250 / 25 |
| Wolf | 140 | 750 / 188 | 350 / 35 |
| Ghoul | 125 | 800 / 200 | 400 / 40 |
| Training dummy | 100 | 1000 / 250 | 500 / 50 |
| Bandit | 75 | 1200 / 300 | 600 / 60 |
| Wild boar | 40 | 1500 / 375 | 1000 / 100 |
| Barbarian | 30 | 1800 / 450 | 900 / 90 |
| Lion | 20 | 2000 / 500 | 1250 / 125 |
| Fire wolf | 10 | 2500 / 625 | 1500 / 150 |

Every default group has separate male/female name catalogues and its own loot table. Animal tables favor food and
leather; humanoid tables favor ores and ingots. Stronger groups provide more valuable choices and larger stacks.
Runtime `groups.json`, `names.json`, and `loot.json` remain update-safe and are not overwritten; merge changed packaged
defaults manually or remove the corresponding runtime file to recreate it on startup.

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
