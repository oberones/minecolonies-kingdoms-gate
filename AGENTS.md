# Repository Guidelines

## Project Structure & Module Organization

MineColonies: Kingdom’s Gate is a Java 21 addon targeting Minecraft 1.21.1, NeoForge 21.1.218, and MineColonies 1.1.1343-1.21.1-snapshot.

- `src/main/java/com/kingdomsgate/cult/` contains the mod. Keep deterministic rules in `domain/`, building integration in `building/`, UI in `client/`, and narrowly scoped hooks in `mixin/`.
- `src/main/resources/` contains loader metadata, textures, models, translations, recipes, and five building blueprints.
- `src/test/java/` contains unit tests; `src/main/java/com/kingdomsgate/cult/gametest/` contains Minecraft integration tests.
- `tools/` contains dependency import, asset generation, and modpack preparation scripts. `assets/provenance.json` records generated assets; `validation/` holds verification records and acceptance checklists.

## Build, Test, and Development Commands

Run commands from the repository root. Use the Gradle wrapper and set `export GRADLE_USER_HOME="$PWD/.gradle-home"` to follow the documented local-cache convention.

- `python3 tools/import_reference.py --mods /path/to/mods` imports six checksum-verified dependencies into `libs/`; omit `--mods` for the recorded reference directory.
- `./gradlew build` compiles, runs unit tests, and creates JARs in `build/libs/`.
- `./gradlew test` runs JUnit tests only.
- `./gradlew runGameTestServer` runs integration tests in a disposable world.
- `./gradlew runClient` launches the development client; `./gradlew runServer` launches a dedicated server requiring operator EULA acceptance.
- `python3 tools/generate_assets.py` regenerates textures, blueprints, and provenance.

See `README.md` for copied-modpack preparation and launch commands.

## Coding Style & Naming Conventions

Follow existing four-space indentation in Java, Python, and Gradle files. Use `PascalCase` Java types, `camelCase` methods/fields, and `UPPER_SNAKE_CASE` constants. Resource identifiers use the `minecolonies_cult` namespace and snake_case paths. Keep domain rules independent of Minecraft APIs. No formatter or lint task is configured; match neighboring code and avoid unrelated reformatting.

## Testing Guidelines

Use JUnit Jupiter 5 for domain behavior, naming classes `*Test` and methods descriptively, such as `backwardsTimeDoesNotRepeatRecruitmentOrExtendGrace`. Add regression cases for changed rules and GameTests for native integration. No numeric coverage threshold is configured. Run `build` and relevant GameTests before submitting behavioral changes. Record commands and results; track visual, pathfinding, and live-restart checks separately in `validation/VERIFICATION.md`.

## Commit & Pull Request Guidelines

Commitizen configures Conventional Commits; history includes `feat: initial mvp` and `bump: version …`. Use concise, scoped messages such as `fix: prevent duplicate ritual scheduling`. PRs should describe behavior changes, link related issues, list validation results and remaining manual checks, and include screenshots for UI or asset changes.

## Dependency & World Safety

Preserve `reference-lock.json`, Gradle locks, and checksum verification. Keep dependency JARs and local run directories untracked. Use disposable worlds; modpack preparation must leave reference installations and player saves untouched.
