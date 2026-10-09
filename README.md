# MineColonies: Kingdom’s Gate

A Minecraft 1.21.1 / NeoForge addon that adds a Cult Leader, five gothic Kingdom’s Gate building levels, persistent recruitment, native happiness, and weekly midnight gatherings.

## Exact compatibility

This release targets **MineColonies 1.1.1343-1.21.1-snapshot**, **NeoForge 21.1.218**, Minecraft **1.21.1**, and **Java 21**. It is not built for 1.1.1403. The loader requires the exact Minecraft, NeoForge, and MineColonies versions.

`reference-lock.json` records the six dependency versions, original filenames, and SHA-256 checksums. The authoritative MineColonies checksum is:

```text
5237cb14b86e2774070d4b825c1e52a29cf058216e9625f6dfb2258e5553ab1d
```

| Dependency | Development/test version |
| --- | --- |
| MineColonies | 1.1.1343-1.21.1-snapshot |
| Structurize | 1.0.831-1.21.1 |
| BlockUI | 1.0.211-1.21.1-snapshot |
| Domum Ornamentum | 1.0.234-snapshot |
| Multi-Piston | 1.2.58-1.21.1 |
| TownTalk | 1.2.0 |

The dependencies are not bundled or redistributed inside the addon. A future change in the reference folder does not update the target: importing or compiling with a changed reference JAR fails checksum verification.

## Install and play

1. Back up the intended world and begin with a disposable test world.
2. Install the pinned dependencies and put `minecolonies-cult-0.2.0.jar` in `mods` on **both the client and server**. Single-player also needs the addon in its client installation.
3. Craft Kingdom’s Gate with a book surrounded by eight planks. Its block is also available in the Kingdom’s Gate creative tab.
4. Use the MineColonies build tool, select the **Kingdom’s Gate** structure pack, and choose `mysticism/kingdoms_gate1`. The five blueprints grow from 9 × 13 to 29 × 35 blocks around the same hut position. Leave the final 29 × 35 footprint clear: in the unrotated preview it extends 14 blocks to each side of the hut, 3 in front, and 31 behind it. Rotation/mirroring rotates this reserved area too. Standard construction, upgrade, repair, inventory, hiring, and minimum-stock controls are provided by MineColonies.
5. Construct level 1 and assign one adult citizen as Cult Leader through the hut’s worker controls. Only one Gate can be placed per colony. The leader receives cult membership benefits while assigned, occupying a separate place from recruited members.
6. Open the hut’s congregation tab to see recruit occupancy/capacity, the leader, each adult’s speech progress, the next gathering, and any faith countdown.

During the native work period, a healthy leader approaches adult colony citizens, including guards, and gives five-second speeches. A listener must stay within range and line of sight for all 100 ticks. Nearby adults may listen together; children, visitors, sleeping listeners, and unavailable citizens do not receive credit. Each citizen can receive one credit per Minecraft day, joining after six distinct days. Members retain their jobs. The leader must carry an ordinary Minecraft book to give speeches. Each successful conversion consumes one book; partial daily progress consumes none. When none remain, the leader requests a replacement through MineColonies’ normal worker resource system and resumes after supply arrives. Writable, written, and enchanted books do not qualify. With limited books, simultaneous conversions use citizen-ID order and unfunded listeners retain their progress.

| Building level | Recruit capacity (+1 leader) | Footprint | Hearing radius | Native happiness factor, weight 1 |
| --- | --- | --- | --- | --- |
| 1 | 4 | 9 × 13 | 8 | 1.25 |
| 2 | 8 | 13 × 17 | 10 | 1.50 |
| 3 | 16 | 17 × 23 | 12 | 1.75 |
| 4 | 24 | 23 × 29 | 14 | 2.00 |
| 5 | 32 | 29 × 35 | 16 | 2.25 |

Capacity is fixed by the **completed** level. Queuing an upgrade does not unlock recruits. When full, the leader pauses recruitment speeches without changing partial progress. Recruitment resumes after an upgrade or a vacancy; citizens still need a complete speech to earn another daily credit. Simultaneous final credits are processed in citizen-ID order.

Automatic leader membership ends on dismissal; previously earned speech progress remains. A previously recruited leader keeps permanent membership and is counted once. Existing overfull congregations and members returning from leadership keep their membership and bonuses, but further recruitment is blocked until there is space.

These are weighted factors in MineColonies’ existing calculation, **not additional happiness points**. MineColonies’ cap of 10 remains in effect. Losing the completed Gate or its leader begins a seven-day grace period. Members keep their last active level’s bonus; restoring operation cancels the countdown. Expiry clears membership and all speech progress. Entity/chunk unloading, illness, rain, raids, and missed gatherings do not themselves start faith decay.

The calendar begins when construction and staffing first become operational. Every seventh Minecraft day, members gather at tick 16000, perform the ritual at 18000–18600, and then resume native AI. Native sleep handling wakes sleeping attendees and controls their return to their homes; guards resume their native duty schedule. The ritual uses turns, synchronized arm gestures, low-pitched wordless sounds, and purple particles. The addon neither teleports attendees nor requests chunk tickets. The leader has priority, followed by citizen ID. Each attendee has a distinct safe position; attendance is limited to available positions and the level’s capacity plus the leader. Over-capacity members keep their bonuses even when they cannot attend. Unreachable/unavailable attendees drop out. An unavailable leader, invalid Gate, unloaded gathering area, or relevant vanilla/MineColonies raid cancels the event.

Missed nights/weeks are skipped. Each occurrence is saved as consumed before gathering begins; restarting during a ceremony cancels it instead of resuming it. Backward time changes cannot grant duplicate daily speeches or replay an already consumed ceremony; the saved calendar waits for world time to catch up.

## Updating older worlds

Back up worlds before installing this version on both client and server. Version 0.2.0 writes cult save format 2 and reads format 1 without deleting memberships or speech progress. Historical memberships remain permanent because their recruitment origin was not recorded. Downgrading the mod requires restoring the backup; the older version cannot read format 2. Client and server must use the same addon build because congregation synchronization has changed.

Structure-pack version 2 replaces the old designs at their existing paths. Installing it does not rebuild world blocks. Use normal builder upgrade/repair work to adopt the new design. Old lower-level Gates occupied larger plots: blocks outside the new blueprint can remain and need manual cleanup. There is no automatic demolition or legacy design pack.

## Server configuration

The world’s `serverconfig/minecolonies_cult-server.toml` exposes:

| Setting | Default |
| --- | --- |
| `speechesRequired` | 6 |
| `faithGraceDays` | 7 |
| `ritualIntervalDays` | 7 |
| `baseHearingRadius` | 8 |
| `hearingRadiusPerLevel` | 2 |
| `baseHappinessFactor` | 1.25 |
| `happinessFactorPerLevel` | 0.25 |

Change configuration with the server stopped. Threshold/range/bonus changes apply to subsequent gameplay. Existing members stay members, and an established next ceremony remains scheduled; later intervals use the new setting. The grace duration is applied to the saved time at which operation was lost.

## Build and test

The root Makefile wraps the commands below. Run `make` or `make help` for the
complete target list. It defaults to the repository-local `.gradle-home` cache
and uses the Gradle wrapper. Python tooling requires Python 3.11 or newer.

```sh
make setup                          # Import the six checksum-pinned dependencies
make build                          # Compile, run unit tests, package JARs
make test TEST='*CultStateTest'       # Run a selected JUnit class/method pattern
make verify                         # Build plus minimal-dependency GameTests
make client                         # Launch the development client
make assets                         # Regenerate tracked assets and provenance
make build GRADLE_FLAGS='--offline --console=plain'
```

Use `make setup MODS='/path/to/mods'` for an alternate dependency source.
`GRADLE`, `PYTHON`, and `GRADLE_USER_HOME` can also be overridden. `make clean`
removes Gradle build outputs while preserving dependency caches and run worlds;
`make rebuild` runs clean followed by build in one ordered Gradle invocation.
Run cleanup separately from other targets, rather than `make -j clean build`.
Build outputs are in `build/libs/`, and unit-test reports are in
`build/reports/tests/test/index.html`.

For the copied modpack, run `make prepare-modpack-client` followed by
`make modpack-client`, or `make prepare-modpack-headless` followed by
`make modpack-gametest`. `make prepare-modpack-server` prepares the server copy
without the headless adjustments. Preparation always uses the recorded reference
installation (`MODS` applies only to dependency import), and recopies its
configuration. Launch targets do not repeat preparation. GameTests use the
disposable `run-gametest/` and `run-modpack-gametest/` directories; Make does not
erase existing test worlds. `make server` requires operator EULA acceptance.

Equivalent direct commands:

```sh
python3 tools/import_reference.py
GRADLE_USER_HOME="$PWD/.gradle-home" ./gradlew build
GRADLE_USER_HOME="$PWD/.gradle-home" ./gradlew runGameTestServer
GRADLE_USER_HOME="$PWD/.gradle-home" ./gradlew runClient
```

The importer defaults to the original MC-TechColonies mods directory recorded in the manifest. It reads that installation without modifying it. Use `python3 tools/import_reference.py --help` for an alternate source directory. Gradle uses a Java 21 toolchain and downloads it when necessary. The wrapper/plugin versions are pinned. `verifyReference` checks all six local JARs before compilation; Gradle dependency verification checks downloaded build dependencies.

`runServer` starts a regular dedicated development server. Its EULA must be accepted by the person operating it. The automated checks use Minecraft’s separate GameTest server in a disposable world.

For a copy of the reference modpack:

```sh
python3 tools/prepare_modpack_test.py
GRADLE_USER_HOME="$PWD/.gradle-home" ./gradlew runModpackClient
python3 tools/prepare_modpack_test.py --server --headless-adjustments
GRADLE_USER_HOME="$PWD/.gradle-home" ./gradlew runModpackGameTestServer
```

The preparation script copies mods and configuration, never the player’s saves. It excludes installed `minecolonies-cult-*.jar` releases because Gradle supplies the addon under test; all 134 reference dependencies remain checksum-pinned. Some chunk optimizers require the documented test-only adjustments in `validation/headless-config-overrides.json` for a headless server with no real players. See `validation/VERIFICATION.md` for the exact evidence and remaining interactive acceptance checks.

`python3 tools/generate_assets.py` reproducibly rebuilds the original textures, five blueprints, and their provenance manifest. Gathering markers are connected to the entrance; the runtime checks their support and clearance again in the actual world. The GameTest suite validates all eight rotation/mirror combinations for each level.

## Implementation boundaries

`domain/` contains pure calendar, faith, and hearing rules. `CultSavedData` stores versioned records per dimension and colony, keyed by citizen UUID, independently of the hut. `ColonyRuntime` owns temporary navigation and participants; none of that AI ownership is serialized. Two narrowly scoped mixins integrate the exact pinned citizen state machine and both native happiness deserialization paths. The status tab uses MineColonies’ existing building-view synchronization.

This is an initial implementation. Automated checks and successful client startup do not establish builder pathfinding, robe/UI appearance, sound quality, or an observed return to an actual residence bed. The acceptance checklist records those separately.
