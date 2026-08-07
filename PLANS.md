# OZ Bosses plan

- [x] Scaffold standalone plugin from the current Maven baseline.
- [x] Implement persisted threat, boss groups, combat ranking, scaling, corpse rewards, and optional Wallet bounty.
- [x] Add per-group health/weight defaults, separate name catalogues, and tiered loot tables.
- [ ] Validate NPC/item IDs and balance on the development server.
- [ ] Make clothing and weapon loadouts configurable once equipment handling is finalized.

Planning is stored in repository-local docs.

- Active implementation tasks: [docs/active/](docs/active/)
- Roadmaps and larger plans: [docs/roadmaps/](docs/roadmaps/)
- Completed phase summaries: [docs/phase-archive.md](docs/phase-archive.md)
- Planning and documentation standards: [docs/policies/repository-policy.md](docs/policies/repository-policy.md)

Every implementation plan must include objective, ownership, dependencies, risks, validation strategy, affected repositories/plugins, rollback considerations, and a markdown checkbox checklist.

## 2026-08-07 Dummy appearance follow-up

- Objective: increase default dummy-clothing variety and give compatible hats a 25% spawn probability.
- Ownership: `rw-plugin-oz-bosses`; no persistence or public API changes.
- Risk and rollback: unknown clothing definitions remain skipped; rollback is the previous plugin artifact.
- Validation: Maven test/package, API and sole-listener checks, then Dev reload/log review.
- [x] Implement and validate the random outfit and hat selection.
