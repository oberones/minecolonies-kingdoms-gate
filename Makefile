# Run from the repository root (or use make -C /path/to/repository).
# Keep Gradle responsible for incremental builds and dependency verification.
SHELL := /bin/sh
.DEFAULT_GOAL := help
.DELETE_ON_ERROR:

GRADLE ?= ./gradlew
GRADLE_FLAGS ?=
PYTHON ?= python3
GRADLE_USER_HOME ?= $(CURDIR)/.gradle-home
MODS ?=
TEST ?=
export GRADLE_USER_HOME MODS TEST

.PHONY: help setup import-reference verify-reference build assemble jar sources \
        test check gametest verify client server prepare-modpack-client \
        prepare-modpack-server prepare-modpack-headless modpack-client \
        modpack-gametest assets clean rebuild tasks dependencies java-toolchains

help: ## Show targets and configurable variables (default; no build).
	@printf '%s\n' 'MineColonies: Kingdom’s Gate' '' 'Usage: make <target> [VARIABLE=value]' '' 'Targets:'
	@awk 'BEGIN { FS = ":.*## " } /^[a-zA-Z0-9_-]+:.*## / { printf "  %-27s %s\n", $$1, $$2 }' Makefile
	@printf '%s\n' '' 'Variables:' \
		'  GRADLE             Gradle executable (default: ./gradlew)' \
		'  GRADLE_FLAGS       Extra Gradle arguments, e.g. --offline --console=plain' \
		'  GRADLE_USER_HOME   Gradle cache (default: repository .gradle-home)' \
		'  PYTHON             Python 3.11+ executable (default: python3)' \
		'  MODS               Alternate mods directory for setup/import-reference only' \
		'  TEST               JUnit class/method pattern for test, e.g. *CultStateTest' \
		'' 'Prepare modpack copies explicitly before launching them.' \
		'GameTests use disposable run directories; server EULA acceptance is manual.' \
		'clean/rebuild remove Gradle build outputs only, preserving caches and worlds.'

setup: import-reference ## Import the six pinned dependencies for a new checkout.

import-reference: ## Import checksum-verified JARs; optionally set MODS=/path/to/mods.
	@if [ -n "$$MODS" ]; then \
		"$(PYTHON)" tools/import_reference.py --mods "$$MODS"; \
	else \
		"$(PYTHON)" tools/import_reference.py; \
	fi

verify-reference: ## Verify local dependency JARs against reference-lock.json.
	"$(GRADLE)" $(GRADLE_FLAGS) verifyReference

build: ## Compile, run unit checks, and package binary/source JARs.
	"$(GRADLE)" $(GRADLE_FLAGS) build

assemble: ## Package binary/source JARs without running unit tests.
	"$(GRADLE)" $(GRADLE_FLAGS) assemble

jar: ## Package the runtime mod JAR in build/libs/.
	"$(GRADLE)" $(GRADLE_FLAGS) jar

sources: ## Package the source JAR in build/libs/.
	"$(GRADLE)" $(GRADLE_FLAGS) sourcesJar

test: ## Run JUnit tests; optionally set TEST to a class/method pattern.
	@if [ -n "$$TEST" ]; then \
		"$(GRADLE)" $(GRADLE_FLAGS) test --tests "$$TEST"; \
	else \
		"$(GRADLE)" $(GRADLE_FLAGS) test; \
	fi

check: ## Run Gradle checks, including unit tests and reference verification.
	"$(GRADLE)" $(GRADLE_FLAGS) check

gametest: ## Run the minimal-dependency Minecraft integration server.
	"$(GRADLE)" $(GRADLE_FLAGS) runGameTestServer

verify: ## Build and run minimal-dependency GameTests in one Gradle invocation.
	"$(GRADLE)" $(GRADLE_FLAGS) build runGameTestServer

client: ## Launch the development client in run-client/.
	"$(GRADLE)" $(GRADLE_FLAGS) runClient

server: ## Launch the dedicated development server; operator accepts its EULA.
	"$(GRADLE)" $(GRADLE_FLAGS) runServer

# Preparation reads the recorded reference installation and copies configuration.
# Keep it explicit so launching an existing copy preserves local test settings.
prepare-modpack-client: ## Prepare run-modpack-client/ from the recorded reference.
	"$(PYTHON)" tools/prepare_modpack_test.py

prepare-modpack-server: ## Prepare run-modpack-gametest/ with original configuration.
	"$(PYTHON)" tools/prepare_modpack_test.py --server

prepare-modpack-headless: ## Prepare the GameTest copy with documented headless adjustments.
	"$(PYTHON)" tools/prepare_modpack_test.py --server --headless-adjustments

modpack-client: ## Launch the previously prepared reference-modpack client.
	"$(GRADLE)" $(GRADLE_FLAGS) runModpackClient

modpack-gametest: ## Run GameTests in the previously prepared reference-modpack copy.
	"$(GRADLE)" $(GRADLE_FLAGS) runModpackGameTestServer

assets: ## Regenerate tracked textures, five blueprints, and asset provenance.
	"$(PYTHON)" tools/generate_assets.py

clean: ## Remove Gradle build outputs; keep dependencies, caches, and run worlds.
	"$(GRADLE)" $(GRADLE_FLAGS) clean

# One invocation orders clean before build even when make is invoked with -j.
rebuild: ## Clean and build in order, including unit checks.
	"$(GRADLE)" $(GRADLE_FLAGS) clean build

tasks: ## List all Gradle tasks, including development run tasks.
	"$(GRADLE)" $(GRADLE_FLAGS) tasks --all

dependencies: ## Display Gradle dependency reports without changing lockfiles.
	"$(GRADLE)" $(GRADLE_FLAGS) dependencies

java-toolchains: ## Display Java toolchains detected by Gradle.
	"$(GRADLE)" $(GRADLE_FLAGS) javaToolchains
