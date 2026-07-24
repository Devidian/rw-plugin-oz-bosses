# Handler and runtime configuration refactor

## Objective

Move Rising World event intake out of `Bosses` into dedicated handler classes and replace embedded boss names, groups, and loot defaults with packaged JSON defaults plus editable runtime copies.

## Ownership and dependencies

- Owner: `rw-plugin-oz-bosses`
- Dependencies: existing Gson transitively supplied by OZ Tools; no new dependency
- Affected deployment helper: root `dev-upload.sh`

## Checklist

- [x] Add packaged `names.default.json`, `groups.default.json`, and `loot.default.json`.
- [x] Copy defaults to `names.json`, `groups.json`, and `loot.json` only when the runtime files are absent.
- [x] Route player action, combat, and command/object events through handler classes.
- [x] Remove embedded default configuration data from the main plugin class.
- [x] Preserve runtime JSON during development uploads.
- [x] Build and validate packaged files and runtime configuration creation.

## Risks and rollback

Runtime JSON is intentionally preserved across updates. Deleting one runtime JSON restores its packaged default at next plugin start. Existing legacy JSON files remain untouched as rollback references.

## Validation

Compile with Maven, inspect the ZIP for all default JSON files, deploy to development, and confirm the runtime files are created only when missing.

## Result

`mvn package` completed successfully from a clean temporary build copy. The development server reloaded all plugins
after deployment without Bosses errors; its editable runtime JSON files remain separate from the update-overwritten
default JSON files.
