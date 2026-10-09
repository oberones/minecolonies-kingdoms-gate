# Verification record — 0.2.0 growth update

## Makefile entry points — 2026-10-09

- `make help` and dry runs of every action target passed.
- Six temporary command-stub checks passed for default/filtered tests, default/alternate dependency import, headless preparation, and ordered rebuild. These checked argument forwarding and the exported cache path, including executable and directory paths containing spaces.
- `make build GRADLE_FLAGS='--offline --console=plain'` passed with reference checksum verification; compilation, packaging, and JUnit tasks were up-to-date. Gradle required local socket access outside the restricted sandbox.
- `git diff --check` passed. No gameplay code changed; Minecraft clients, servers, and GameTests were not launched for this Makefile change. Preparation and asset generation were dry-run only.

## Cult leader book requirement — 2026-10-09

`GRADLE_USER_HOME="$PWD/.gradle-home" ./gradlew build runGameTestServer --offline --console=plain` passed: **14 JUnit tests and all 9 required GameTests**. Evidence: [build and native server log](evidence/books-2026-10-09/build-gametest.log) and JUnit XML files in the same directory. Gradle required local socket access outside the restricted sandbox. Tests used a fresh disposable `run-gametest/world`; earlier test worlds were archived under ignored `.tools/`. Reference installations, dependency locks, and player saves were untouched.

The leader requires a carried ordinary `minecraft:book` throughout recruitment. Partial speech credits consume nothing; each new permanent convert consumes exactly one. Native worker requests replenish exhausted stock, with native open/completed-request deduplication and hut pickup. Existing carried stock is used before requesting more. Recruitment pauses without books and resumes without losing saved progress. Simultaneous listeners retain citizen-ID ordering. The custom hut now exposes its item-handler capability so native worker/courier inventory lookups can access its storage.

Native checks cover rejection of writable books, no progress without an ordinary book, repeated request deduplication, automatic fulfillment from hut inventory, worker pickup, one conversion per delivered book, replacement requests and a second delivery/conversion, four simultaneous conversions consuming four books, no consumption while capacity is full, and no consumption for partial daily credits. Existing recruitment, capacity, upgrade, persistence, and ceremony checks pass. Development failures exposed missing worker idle/work transitions and the missing hut inventory capability; both were fixed before the passing run.

These tests position stationary actors, seed pending speech progress, insert delivered books into hut storage, and accelerate the calendar. They do not establish natural courier/leader pathfinding or interactive acceptance. The copied full modpack was not rerun for this change.

- [ ] In a disposable client world, inspect the leader's book request and fulfill it through player delivery and a real courier; observe natural return to work.
- [ ] Restart a disposable world with an outstanding book request and verify native request recovery and pickup.
- [ ] Confirm existing members can still attend a ceremony when the leader has no book.

---


**2026-10-09 (America/New_York): build and automated checks passed; interactive gameplay acceptance remains OPEN.**

The delivered artifact is `build/libs/minecolonies-cult-0.2.0.jar`, SHA-256 `0e60703b3b0a4a00a759e9691efbcc6018287101e93de55f5c09c1797612b33d`. Its runtime classes, version metadata, structure-pack version 2, and all five packaged blueprint checksums were inspected. Client and server must use this same build. Reference installations and player saves were not modified; no commit, tag, or publication was performed.

## Growth-update automated evidence

| Check | Result | Evidence |
| --- | --- | --- |
| Gradle build and reference dependency verification | Pass | `evidence/growth-0.2.0/modpack-server.txt` |
| JUnit domain tests | 14 passed, zero failures | Both `TEST-*.xml` files in `evidence/growth-0.2.0/` |
| Minimal dependency GameTest server | All 8 required tests passed | `evidence/growth-0.2.0/minimal-server.txt` |
| Copied 134-mod GameTest server | All 8 required tests passed with documented headless adjustments | `evidence/growth-0.2.0/modpack-server.txt` |
| Blueprints | Native reader, correct growing dimensions/corners, fixed world alignment, supported entrance-connected slots; all 40 level/orientation combinations passed | `fiveBlueprintsAndEveryOrientation` in both servers |
| Asset regeneration | All 12 generated files match two isolated regenerations byte-for-byte | `evidence/growth-0.2.0/asset-reproduction.json` |
| Minimal client startup | Resources and sound engine initialized with no addon startup failure | `evidence/growth-0.2.0/client-startup.txt` |
| Generated architectural preview | Inspected footprints and growth in a render of the generated block data | `evidence/growth-blueprints.png`; this is **not** an in-game screenshot |

New checks cover all recruit limits, final-place contention in stable citizen-ID order, paused progress, vacancies and completed upgrades, role-only versus permanent leadership, overfull retention, v1 migration/v2 round trips, shared client/server status decoding, and capacity-limited ceremonies with separate standing positions. A native pending upgrade work order does not unlock capacity; completion does. Role loss cancels attendance and removes only temporary membership benefits.

The entity fixtures deliberately disable free movement, accelerate time, seed five prior speech credits in the capacity test, and position actors at known ceremony slots. They exercise the real MineColonies citizen/job/AI and building APIs, but do not establish builder construction or natural navigation. The preparation script excludes previously installed `minecolonies-cult-*.jar` files because Gradle supplies the addon under test; all 134 reference dependency filenames/checksums still match the pinned manifest. Existing upstream modpack warnings and the documented chunk-loading overrides described below still apply.

Earlier runs exposed fixture spawning/metadata/hiring issues and interference from retained test worlds; they were corrected before the passing runs above. Previous disposable worlds were moved under `.tools/` instead of deleted. The runtime also now replaces transient state when a colony ID is reused by a different native colony object. Repository ignore rules were narrowed to root-level `/run*/` so the previously ignored Java `runtime/` package is included in source changes.

Reproduce from disposable test worlds (archive an existing `run-gametest/world` or `run-modpack-gametest/world` before a fresh run):

```sh
python3 tools/prepare_modpack_test.py --server --headless-adjustments
GRADLE_USER_HOME="$PWD/.gradle-home" ./gradlew build runGameTestServer runModpackGameTestServer --offline --console=plain
```

Use `--offline` only after dependencies and game assets are cached. The passing minimal-server and full-modpack executions were separate invocations recorded in the evidence files.

## Growth-update interactive acceptance — OPEN

The development client launched, but the available computer-use inventory did not expose its Java window, and `cua.getApp('java')` returned `Invalid app: java`. The client was closed after startup verification. No world was opened through the UI, and no in-game appearance, construction, or navigation result is claimed.

- [ ] In disposable worlds, build each level with a real builder, checking expansion space, material requests, roof/entrance clearance, and all rotations/mirrors. Confirm the hut stays fixed.
- [ ] Fill level one with four recruits plus the leader. Inspect full/over-capacity, leader, progress, inactive, and grace labels, including long names. Queue an upgrade, then observe completion and resumed recruitment.
- [ ] Observe natural gathering and departure at each capacity. Block gathering positions, replace the leader, and confirm no crowding or stuck AI.
- [ ] Load a backup-derived v1 test world, preserve overfull membership/progress, dismiss a role-only leader, and perform a full client/server restart. Install matching 0.2.0 builds on both sides for this check.
- [ ] Repair/upgrade an old large Gate to a new smaller early-tier design and inspect remaining blocks outside the new bounds before manual cleanup.

The earlier interactive checklist below remains open as well. This artifact is an automated-verified test build, not completed interactive or release acceptance.

---

## Historical initial-build verification — 2026-10-08 (America/New_York)

**Initial test build: automated verification passed; interactive gameplay acceptance remains open.**

The build compiled against the actual SHA-256-verified MineColonies **1.1.1343-1.21.1-snapshot** binary from MC-TechColonies, using Minecraft 1.21.1, NeoForge 21.1.218, and the downloaded Java 21 toolchain. No reference-instance files or player saves were modified. No commit or publication was performed.

## Reproducible automated result

Final command:

```sh
python3 tools/prepare_modpack_test.py --server --headless-adjustments
GRADLE_USER_HOME="$PWD/.gradle-home" ./gradlew build runGameTestServer runModpackGameTestServer --offline --console=plain
```

Result: **BUILD SUCCESSFUL**. Seven JUnit tests passed; six GameTests passed on the minimal dependency server and the same six passed on the copied reference modpack server. The six reference dependencies were checksum-verified, and the final run enforced Gradle dependency verification and dependency locks.

Evidence is preserved in `evidence/final-server-verification.log` and `evidence/unit-tests.xml`. Earlier failed fixtures were corrected and rerun: the client happiness constructor needed an explicit mixin descriptor; a directly placed hut needed a default blueprint path; the entity fixture needed clean records, a supported floor, deterministic weather, and explicit stationary spawn positions. The final results supersede those earlier runs.

| Requirement | Evidence | Result |
| --- | --- | --- |
| Exact MineColonies binary | Six reference JAR checksum checks before compile | Pass |
| Exact dependency declaration | Actual loader-parsed range accepts 1343, rejects 1342 and superseded 1403 | Pass; unsupported JAR launch itself was not performed |
| Registration and dedicated environment | Block, custom block entity, building and job registries in both real headless servers | Pass |
| Normal hiring and one-per-colony placement | Native worker assignment and server placement check | Pass |
| Level upgrades | Native completion hooks and factors for levels 1–5; level-five building NBT reload | Pass; builder construction paths remain unobserved |
| Six recruitment days | Native citizen AI with six accelerated calendar days; one credit each day | Pass |
| Child exclusion | Child next to the stationary listeners never receives a credit | Pass |
| Continuous hearing | Pure 100-tick, per-listener continuity/reset checks; native close-range speech fixture | Pass within fixture scope |
| Happiness | Native modifier exists on server and client-deserialization paths, factors scale, native result stays within cap | Pass |
| Faith grace | Native unhire/reassign/expiry flow plus exact-boundary rule tests | Pass |
| Save persistence | Domain snapshots and native NBT save/reload for membership, progress, grace and calendar; colony isolation | Pass round trips; live restart acceptance remains open |
| Weekly schedule | Due day, gathering window, missed weeks/nights, backward time and consumed occurrence checks | Pass |
| Ritual behavior | Native AI transition, sleeping flag cleared by native wake handler, active ceremony phase, release to normal AI | Pass in controlled fixture |
| Raid interruption | Native MineColonies passing-raid flag cancels participants, restores normal AI, and does not cause faith decay | Pass |
| Five blueprints | Native Structurize reader, anchor and gathering-marker checks, all eight rotations/mirrors at all five levels | Pass, 40 combinations |
| Minimal client startup | Resource atlases and sound engine initialized; no addon loading failure | Pass startup only |
| Reference client startup | Copied 134 reference JARs/configuration; resource loading completed | Pass startup only, with upstream warnings |

The entity fixture deliberately disables free wandering, advances the calendar, and moves its stationary actors into known ceremony positions. It exercises real MineColonies data, jobs, citizen AI, happiness, sleep hooks and addon runtime, but does **not** prove natural walking, audible timing, visual quality, or reaching an actual bed. Chunk tickets used by the GameTest fixture belong to the test harness; production addon code does not request them.

## Full-modpack harness and warnings

`reference-modpack.json` records all 134 reference JAR names and checksums. The preparation script rejects a changed inventory. The reference **client** startup retained the original configuration.

The original no-player headless setup stalled before tests could initialize while chunk optimizers unloaded test chunks. For the **headless test copy only**, four Better Chunk Loading options were disabled and Limited Chunks was configured to preserve the harness’s chunk ticket types. Exact changes are in `headless-config-overrides.json`; `--headless-adjustments` reproduces them. This means the full-modpack GameTest pass is qualified by those harness adjustments; it is not a claim that the unmodified no-player server completed the suite.

The modpack also logs upstream problems, including Oracle Index's unavailable ONNX native library on this machine, recipe/tag errors, and missing sound/subtitle assets. The automated suite passed despite these messages. No attempt was made to modify unrelated mods.

Client launch evidence is in `evidence/minimal-client-startup.log` and `evidence/reference-client-startup.log`. The available native-app control surface could not attach to the Java game window, so no rendered scene or UI screenshot is claimed as inspected.

## Interactive acceptance checklist — OPEN

Use a **disposable world**, first with minimal dependencies and then with the copied MC-TechColonies instance. Keep the addon on both sides for a dedicated-server client test.

- [ ] Install into an existing test colony and verify normal citizen/building views synchronize after reconnecting.
- [ ] Craft the hut; place through the build tool; refuse a second Gate; inspect inventory, hiring, build and congregation tabs.
- [ ] Have a builder construct levels 1–5, upgrade and repair; repeat rotated/mirrored placement. Check entrances, arch clearance, roof/towers, material requests and all gathering routes.
- [ ] Inspect male/female leader robe, hut texture/icon, translated text, scrolling status list, sounds, and purple particles.
- [ ] Observe a leader travel between working adults and guards. Break range and line of sight mid-speech. Confirm only six distinct credited days grant membership and bonuses.
- [ ] Save and fully restart the client/server during partial recruitment, membership, grace, and an active ceremony. Confirm an interrupted ceremony never resumes/replays.
- [ ] Observe loaded members and guards gather, sleeping residents wake, the midnight performance, and residents walk home and lie in their actual beds. Guards should resume their native duty schedule.
- [ ] Block paths and gathering slots; unload the building/citizens; create sick, hungry, injured or fighting participants. Confirm individual dropouts, leader cancellation, and no stuck ritual AI.
- [ ] Trigger a real MineColonies invasion and a relevant vanilla raid during gathering and chanting; verify emergency behavior resumes.
- [ ] Skip a ritual night, advance/backtrack time, and miss multiple weeks; verify no deferred or duplicate performance.
- [ ] Demolish the Gate, dismiss/replace the leader, restore operation before expiry, then allow a full grace period to expire. Verify member happiness and progress after reconnecting.
- [ ] Confirm colony/dimension isolation, citizen permanent-removal cleanup, and colony deletion across a full restart.

These unchecked items are not release acceptance. The delivered JAR is suitable for this testing; automated success is not presented as observed in-game acceptance.
