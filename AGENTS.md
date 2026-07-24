# AGENTS.md

## Repository Purpose

This repository owns configurable boss encounters for Rising World Unity,
including boss-group spawning, threat, combat scaling, loot, persistence, and
player/admin inspection.

It remains usable as a standalone Java 20 Maven plugin. Workspace-root
orchestration is optional.

## Ownership

Owns:

- boss group definitions, names, spawn rules, and lifecycle
- threat, combat scaling, rewards, and loot delivery
- active-group persistence and rehydration
- boss-specific commands, UI, settings, and debug output

Does not own:

- Wallet balances or Discord transport internals
- reusable settings, i18n, persistence, logging, or UI helpers from OZ Tools
- workspace-root orchestration

## Mandatory Workflow Rules

- Preserve Java 20, Maven packaging, and GitHub tag-release compatibility.
- Keep runtime JSON files editable and copy packaged defaults only when absent.
- Preserve boss persistence and public/player-facing contracts.
- Use reflection-only bridges for optional Wallet and Discord integrations.
- Keep `Bosses`, the class declared by `plugin.yml`, as the sole Rising World
  `Listener` and sole `registerEventListener(...)` target.
- Keep `Bosses` limited to lifecycle wiring, settings delegation, and one-line
  event dispatch. Feature workflows belong in focused classes under `bosses/`;
  those classes must not implement Rising World's `Listener`.
- Follow `.codex/agents.toml` and `docs/policies/repository-policy.md`.
- Keep `README.md`, `HISTORY.md`, `PLANS.md`, and active plans aligned with
  structural or behavior changes.

## Validation

- Run `scripts/verify-entrypoint-architecture.sh`.
- Run `scripts/verify-plugin-api.sh --summary`.
- Run `mvn -B test` and `mvn -B -DskipTests package`.
- Inspect packaged default JSON and both i18n catalogues when those assets
  change.
- Runtime-smoke spawning, combat, persistence rehydration, loot, settings
  reload, and missing optional integrations before release.
