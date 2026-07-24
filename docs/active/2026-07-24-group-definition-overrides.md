# Group definition overrides

## Objective

Allow each `groups.json` entry to override boss/follower health scaling,
follower NPC type, and minimum player distance, and expose a configurable
group name in the administrator spawn dropdown. Allow relative group weights
to control random spawns.

## Ownership and dependencies

- Owner: `rw-plugin-oz-bosses`
- Dependencies: existing Gson and OZ Tools UI contracts; no new dependency
- Affected repositories/plugins: `rw-plugin-oz-bosses` only

## Checklist

- [x] Extend group JSON parsing with optional, validated override fields.
- [x] Select spawns by stable group key and show the configured group name.
- [x] Apply group-specific boss/follower health, follower NPC type, and player distance.
- [x] Persist the selected group-definition key for active-group rehydration.
- [x] Update packaged defaults and configuration documentation.
- [x] Apply optional relative group weights to random spawns only.
- [x] Balance every packaged group with explicit health progression and inverse difficulty weights.
- [x] Give every packaged group separate male/female names and a tiered loot table.
- [x] Use the API-compatible 256-chunks-per-axis sector conversion during live-NPC rehydration.
- [x] Run architecture, API, Maven, packaging, and migration validation.
- [x] Runtime-smoke mixed NPC types, health scaling, rewards, cleanup, and the admin dropdown.
- [ ] Re-test live-NPC sector assignment after deploying the rehydration fix.

## Risks and rollback

- Existing `groups.json` files must remain valid; absent fields retain global
  behavior.
- Packaged JSON changes do not overwrite editable runtime JSON. Existing
  installations must merge the new defaults or recreate the runtime files.
- Rehydration and spawn boundaries derive sectors from chunk coordinates using
  256 chunks per sector axis instead of a hard-coded 512-world-unit grid.
- The SQLite migration is additive (`definition_key`) and existing rows fall
  back to definition lookup/global settings.
- Rolling back the plugin leaves the extra SQLite column unused. Removing the
  new JSON fields restores global behavior without deleting runtime data.

## Validation

Local validation completed:

- Entry-point architecture and PluginAPI verification passed.
- `mvn -B test` passed (the repository currently has no test sources).
- `mvn -B -DskipTests package` passed from a writable temporary copy.
- Packaged `groups.default.json` is valid and contains all nine named groups.
- Packaged names and loot references cover every configured group; all loot
  item keys exist in the current item catalogue snapshot.
- Name-catalog smoke validation confirmed male/female generation, combined
  ungendered-NPC followers, and boss-name re-identification for all nine groups.
- Boundary validation confirmed the 8192-block (256x256 chunk) sector conversion
  for positive and negative coordinates.
- A deterministic 20,000-selection smoke test confirmed `0:1:3` weighting:
  the zero-weight group was never selected randomly, while explicit selection
  remained available.
- An additive `definition_key` migration retained a legacy row and stored a
  newly keyed row in a temporary SQLite database.

Development-server spawn, health progression, distance, and dropdown behavior
were accepted on 2026-07-24. The corrected sector assignment remains to be
re-tested after deployment.
