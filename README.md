# MineColonies: Kingdom’s Gate

Give your colony a darker sense of belonging. **Kingdom’s Gate** brings gothic architecture, a devoted congregation, and midnight ceremonies to MineColonies. Appoint a Cult Leader, grow a following among your citizens, and watch a modest gathering place become the heart of a thriving faith.

## A new calling for your colony

At the center stands **Kingdom’s Gate**, a building with five gothic designs to unlock through MineColonies’ familiar construction and upgrade system. Begin with room for four recruits and expand to a congregation of 32, plus its leader. Each completed upgrade welcomes more followers, carries the leader’s voice farther, and strengthens the happiness benefit of membership.

Your **Cult Leader** wins followers through patient persuasion. With an ordinary book in hand, the leader visits adult citizens and delivers short speeches. After listening on six different Minecraft days, a citizen joins the congregation. Each conversion uses one book, and the leader requests fresh supplies through MineColonies when stocks run low. Followers keep their existing jobs—including guards—so faith becomes part of colony life alongside its everyday work.

Every seventh Minecraft day, the congregation is called to the Gate for a **midnight ritual**. Synchronized gestures, low wordless sounds, and purple particles give the gathering its own eerie atmosphere. Members travel to the ceremony and return to their normal routines afterward; dangerous or unavailable conditions can cancel a gathering.

Membership contributes to MineColonies’ existing happiness system, with greater benefits at higher building levels. Keep the Gate built and staffed to sustain that faith. Losing the building or its leader starts a seven-day grace period: restore operations in time to preserve the congregation, or let faith fade and begin recruiting anew. Missing a ceremony alone does not cost membership.

The hut’s **congregation tab** brings it all together: see your leader, available recruit spaces, each adult’s speech progress, the next gathering, and any active faith countdown.

These are the default rules; server owners can tune recruitment, ceremony timing, and other values in the world’s `serverconfig/minecolonies_cult-server.toml`.

## Install and get started

Kingdom’s Gate requires **Minecraft 1.21.1**, **NeoForge 21.1.218**, **MineColonies 1.1.1343-1.21.1-snapshot**, and **Java 21**. These Minecraft, NeoForge, and MineColonies versions are required exactly.

Install these companion dependencies alongside MineColonies:

| Mod | Version |
| --- | --- |
| Structurize | 1.0.831-1.21.1 |
| BlockUI | 1.0.211-1.21.1-snapshot |
| Domum Ornamentum | 1.0.234-snapshot |
| Multi-Piston | 1.2.58-1.21.1 |
| TownTalk | 1.2.0 |

Dependencies are not bundled with the addon.

1. Back up your world and try the mod in a disposable world first. Put the addon’s `minecolonies-cult-<version>.jar` in `mods` on both client and server, using the same build. For single-player, install it in your client’s `mods` folder.
2. Craft Kingdom’s Gate with a **book surrounded by eight planks**, or find it in the Kingdom’s Gate creative tab.
3. Use the MineColonies build tool, select the **Kingdom’s Gate** structure pack, and choose `mysticism/kingdoms_gate1`. Reserve space for the final **29 × 35** footprint using the build preview.
4. Complete level 1, then assign an adult citizen as **Cult Leader** through the hut’s worker controls. You can place one Gate per colony.
5. Supply ordinary Minecraft books and open the congregation tab to follow your colony’s new faith.

Upgrading from an older release? Save format 2, introduced in 0.2.0, preserves older memberships and speech progress, but downgrading requires restoring a backup. Updated blueprints take effect through normal builder upgrade or repair work; older, larger plots may need manual cleanup. See [migration details](AGENTS.md#save-and-structure-migrations).

## Build

You’ll need Java 21 and Python 3.11 or newer. From the repository root, download the pinned dependencies and build with the included Gradle wrapper:

```sh
export GRADLE_USER_HOME="$PWD/.gradle-home"
python3 tools/import_reference.py --download
./gradlew build
```

The build runs unit tests and writes JARs to `build/libs/`. To import dependencies from an existing installation instead, replace the download command with `python3 tools/import_reference.py --mods /path/to/mods`.

## Test

With dependencies installed, run these commands from the repository root:

```sh
export GRADLE_USER_HOME="$PWD/.gradle-home"
./gradlew test                 # Unit tests
./gradlew runGameTestServer    # Minecraft integration tests in a disposable world
./gradlew runClient            # Launch the development client for hands-on checks
```

Unit-test reports are written to `build/reports/tests/test/index.html`. Automated tests do not establish visual or pathfinding acceptance; current evidence and remaining hands-on checks are recorded in [validation/VERIFICATION.md](validation/VERIFICATION.md).

For Make shortcuts, copied-modpack testing, configuration details, architecture, and release procedures, see [AGENTS.md](AGENTS.md).
