# Level up on overflow

- Objective: optionally convert boss-threat overflow at a positive sector cap into a level-up for a random active group.
- Ownership: `rw-plugin-oz-bosses` only; no shared-plugin or persistence-schema change.
- Dependencies: existing threat roll, active-group state, level-up, persistence, and announcement services.
- Risks: the flag must preserve the legacy capped-spawn behaviour by default; explicit administrator group selection must stay blocked at the cap.
- Validation: Maven tests/package, PluginAPI and sole-listener checks, archive inspection, focused Dev upload and reload-log review.
- Rollback: set `boss.levelUpOnOverflow=false`, or restore the prior plugin artifact.

- [x] Add the backward-compatible setting and localized admin metadata.
- [x] Reuse the existing level-up mechanics for overflow and random-admin spawning.
- [x] Deduct one threat threshold only after a successful overflow level-up.
- [x] Validate and deploy to Development.
