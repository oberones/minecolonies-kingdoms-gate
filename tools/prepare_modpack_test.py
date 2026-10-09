#!/usr/bin/env python3
"""Prepare an isolated modpack test instance; never copy or open the player's saves."""
import hashlib
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
lock = json.loads((root/'reference-lock.json').read_text())
source = Path(lock['referenceDirectory']).parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--server', action='store_true')
parser.add_argument('--headless-adjustments', action='store_true', help='Apply recorded no-player GameTest chunk optimizer settings; server only.')
args = parser.parse_args()
if args.headless_adjustments and not args.server: parser.error('--headless-adjustments requires --server')
subprocess.run([sys.executable, str(root/'tools/import_reference.py')], check=True)
destination = root/('run-modpack-gametest' if args.server else 'run-modpack-client')
(destination/'mods').mkdir(parents=True, exist_ok=True)
baseline = {entry['file'] for entry in lock['artifacts']}
inventory = []
for jar in sorted((source/'mods').glob('*.jar')):
    with jar.open('rb') as stream:
        digest = hashlib.file_digest(stream, 'sha256').hexdigest()
    inventory.append({'file':jar.name, 'sha256':digest})
manifest = {'minecraft':lock['minecraft'],'neoforge':lock['neoforge'],'mods':inventory}
manifest_path = root/'validation/reference-modpack.json'
if manifest_path.exists() and json.loads(manifest_path.read_text()) != manifest:
    raise SystemExit('Reference modpack inventory changed. Review validation/reference-modpack.json explicitly; test target was not changed.')
for jar in sorted((source/'mods').glob('*.jar')):
    if jar.name not in baseline: shutil.copy2(jar, destination/'mods'/jar.name)
for folder in ['config','defaultconfigs','datapacks','global_packs']:
    if (source/folder).is_dir(): shutil.copytree(source/folder, destination/folder, dirs_exist_ok=True)
(root/'validation').mkdir(exist_ok=True)
if not manifest_path.exists(): manifest_path.write_text(json.dumps(manifest,indent=2)+'\n')
if args.headless_adjustments:
    settings = {
        'betterchunkloading.json': {key:False for key in ['enablePrediction','enableSmartChunkLoading','enableSmartPostProcessing','optimizeWaiting']},
        'limitedchunks.json': {'excludedtickets':['forced','gametest','start','unknown']},
    }
    for filename, entries in settings.items():
        config_path = destination/'config'/filename
        config = json.loads(config_path.read_text())
        for key, value in entries.items(): config[key][key] = value
        config_path.write_text(json.dumps(config,indent=2)+'\n')
    print('Applied documented headless GameTest chunk-ticket adjustments.')
print(f'Prepared {len(inventory)} reference mods; {len(baseline)} supplied through pinned Gradle dependencies.')
print(f'Isolated instance: {destination}')
