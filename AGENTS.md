# AGENTS.md

## Repository Purpose
This repository is the canonical Maven baseline for new Rising World Unity Java plugins in this workspace.

It must remain usable as a standalone template repository. Workspace-root orchestration is optional and must never be required for normal use.

## Ownership
Owns:
- baseline Maven project layout
- Java 20 plugin runtime defaults
- GitHub tag-release workflow conventions
- baseline documentation, policy, and agent workflow structure
- baseline examples for shared Tools UI, settings metadata, logging, and plugin
  info/status conventions

Does not own:
- feature-plugin business logic
- shared runtime helpers that belong in `rw-plugin-oz-tools`
- workspace-root orchestration rules

## Mandatory Workflow Rules
- Preserve the Java 20 baseline.
- Preserve Maven-based build and packaging behavior.
- Preserve GitHub tag-release compatibility.
- Keep generated plugin repositories autonomous.
- Route shared UI, i18n, settings, persistence, file-watching, and logger
  conventions through `rw-plugin-oz-tools` unless a repository-local need is
  explicitly documented.
- Follow `.codex/agents.toml` for local agent roles, task classes, context loading, and escalation.
- Follow `docs/policies/repository-policy.md` for reusable governance rules.
- Keep `README.md`, `HISTORY.md`, and `PLANS.md` aligned with structural changes.

## Validation
- Run `mvn -B -DskipTests package` for build-impacting changes.
- Run `mvn -B test` when tests exist.
- Review `.github/workflows/*` when release behavior or artifact names change.
- Update this template whenever a convention should apply to future plugin repositories.
