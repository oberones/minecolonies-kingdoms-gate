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
2. Install the pinned dependencies and put `minecolonies-cult-0.1.0.jar` in `mods` on **both the client and server**. Single-player also needs the addon in its client installation.
3. Craft Kingdom’s Gate with a book surrounded by eight planks. Its block is also available in the Kingdom’s Gate creative tab.
4. Use the MineColonies build tool, select the **Kingdom’s Gate** structure pack, and choose `mysticism/kingdoms_gate1`. The five blueprints share a 29 × 35 block footprint and a fixed anchor. Standard construction, upgrade, repair, inventory, hiring, and minimum-stock controls are provided by MineColonies.
5. Construct level 1 and assign one adult citizen as Cult Leader through the hut’s worker controls. Only one Gate can be placed per colony. The leader immediately joins the cult.
6. Open the hut’s congregation tab to see members, each adult’s speech progress, the next gathering, and any faith countdown.

During the native work period, a healthy leader approaches adult colony citizens, including guards, and gives five-second speeches. A listener must stay within range and line of sight for all 100 ticks. Nearby adults may listen together; children, visitors, sleeping listeners, and unavailable citizens do not receive credit. Each citizen can receive one credit per Minecraft day, joining after six distinct days. Members retain their jobs.

| Building level | Hearing radius | Native happiness factor, weight 1 |
| --- | --- | --- |
| 1 | 8 | 1.25 |
| 2 | 10 | 1.50 |
| 3 | 12 | 1.75 |
| 4 | 14 | 2.00 |
| 5 | 16 | 2.25 |

These are weighted factors in MineColonies’ existing calculation, **not additional happiness points**. MineColonies’ cap of 10 remains in effect. Losing the completed Gate or its leader begins a seven-day grace period. Members keep their last active level’s bonus; restoring operation cancels the countdown. Expiry clears membership and all speech progress. Entity/chunk unloading, illness, rain, raids, and missed gatherings do not themselves start faith decay.

The calendar begins when construction and staffing first become operational. Every seventh Minecraft day, members gather at tick 16000, perform the ritual at 18000–18600, and then resume native AI. Native sleep handling wakes sleeping attendees and controls their return to their homes; guards resume their native duty schedule. The ritual uses turns, synchronized arm gestures, low-pitched wordless sounds, and purple particles. The addon neither teleports attendees nor requests chunk tickets. Unreachable/unavailable attendees drop out. An unavailable leader, invalid Gate, unloaded gathering area, or relevant vanilla/MineColonies raid cancels the event.

Missed nights/weeks are skipped. Each occurrence is saved as consumed before gathering begins; restarting during a ceremony cancels it instead of resuming it. Backward time changes cannot grant duplicate daily speeches or replay an already consumed ceremony; the saved calendar waits for world time to catch up.

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

The preparation script copies mods and configuration, never the player’s saves. Some chunk optimizers require the documented test-only adjustments in `validation/headless-config-overrides.json` for a headless server with no real players. See `validation/VERIFICATION.md` for the exact evidence and remaining interactive acceptance checks.

`python3 tools/generate_assets.py` reproducibly rebuilds the original textures, five blueprints, and their provenance manifest. Gathering markers are connected to the entrance; the runtime checks their support and clearance again in the actual world. The GameTest suite validates all eight rotation/mirror combinations for each level.

## Implementation boundaries

`domain/` contains pure calendar, faith, and hearing rules. `CultSavedData` stores versioned records per dimension and colony, keyed by citizen UUID, independently of the hut. `ColonyRuntime` owns temporary navigation and participants; none of that AI ownership is serialized. Two narrowly scoped mixins integrate the exact pinned citizen state machine and both native happiness deserialization paths. The status tab uses MineColonies’ existing building-view synchronization.

This is an initial implementation. Automated checks and successful client startup do not establish builder pathfinding, robe/UI appearance, sound quality, or an observed return to an actual residence bed. The acceptance checklist records those separately.
