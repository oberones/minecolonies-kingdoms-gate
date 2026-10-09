# Verification record — 2026-10-08 (America/New_York)

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
