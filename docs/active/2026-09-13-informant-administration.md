# Informant administration and boss-group inspection

## Objective

Replace the broad named-NPC administrator view with a boss-group-only view and
let administrators inspect, rename, and dissolve persistent Headhunter
Informants.

## Ownership and dependencies

`rw-plugin-oz-bosses` owns the UI, NPC lifecycle, and its SQLite migration.
`OZ - Wallet` remains an optional reflection-only dependency: each Informant
uses a Bosses-owned system account for intelligence sales and its balance is
shown when Wallet is available.

## Risks and rollback

The additive `account_id` migration preserves existing Informants. Accounts
for legacy entries are created lazily. Dissolving an Informant transfers every
positive balance to Wallet's world account before archiving its account, then
removes its NPC and database entry; a failed settlement leaves the Informant
intact. Restore the preceding plugin and database backup to roll back that
administrative action.

## Validation

- [x] Verify Maven test/package and plugin architecture/API checks.
- [x] Verify packaged i18n assets.
- [x] Upload only OZ Bosses to Development and inspect reload/startup logs.
- [x] Manually test the two administrator tabs, rename, and dissolve actions.

## Checklist

- [x] Scope ownership and persistence migration.
- [x] Implement the focused views and Informant actions.
- [x] Validate and deploy to Development.
