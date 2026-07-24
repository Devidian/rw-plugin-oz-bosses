# Phase Archive

Summarize completed phases and closed task groups here.

## Template

```md
## YYYY-MM - <Phase Name>

Repositories affected:
- `<repo>`

Summary:
- <what was completed>

Validation:
- <commands/checks run>

Follow-ups:
- <remaining work or none>
```

## 2026-07 - Boss runtime extraction and configuration

Repositories affected:
- `rw-plugin-oz-bosses`

Summary:
- Reduced the plugin entry point to lifecycle wiring and delegated event intake.
- Extracted runtime composition, persistence, threat, spawning, combat, loot,
  settings, view, and configuration responsibilities into focused classes.
- Replaced embedded names, groups, and loot defaults with packaged JSON
  defaults and editable runtime copies.

Validation:
- Maven tests and packaging passed from writable temporary copies.
- Entry-point architecture and PluginAPI verification passed.
- Development-server reload registered only the descriptor entry point and
  restored persisted active-group state without a Bosses exception.

Follow-ups:
- Group balancing and rehydration validation continue in
  `docs/active/2026-07-24-group-definition-overrides.md`.
