# Gender-specific informant icons

## Objective

Give male and female Headhunter Informant radial-menu actions their own plugin
asset keys instead of the generic Bosses icon.

## Ownership

Owning repository/plugin: `rw-plugin-oz-bosses`
Supporting repositories/plugins: `rw-plugin-oz-tools` asset loading only

## Dependencies

- Runtime: existing `AssetManager.loadIconFromPlugin` convention.
- Build: Java 20 and current Maven packaging.

## Risks

Missing packaged assets would make the menu entry fall back or fail to render.
Both classic and modern asset paths must therefore carry the same named files.

## Validation Strategy

- [x] Verify both PNG assets and menu asset keys.
- [x] `scripts/verify-entrypoint-architecture.sh`
- [x] `scripts/verify-plugin-api.sh --summary`
- [x] Isolated `mvn -B test` and `mvn -B clean package`.
- [x] Upload only OZ Bosses to Development and inspect reload logs.

## Affected Repositories/Plugins

- `rw-plugin-oz-bosses`

## Rollback Considerations

Restore the previous OZ Bosses artifact; no persistent data or external API
changes are involved.

## Implementation Checklist

- [x] Add gender-specific informant PNG assets for both icon styles.
- [x] Bind male/female radial actions to their respective icon keys.
- [x] Validate and deploy.
