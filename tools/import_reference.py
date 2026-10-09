#!/usr/bin/env python3
"""Copy only checksum-verified reference dependencies into the local build cache."""
import argparse
import hashlib
import json
import shutil
from pathlib import Path

root = Path(__file__).resolve().parents[1]
lock = json.loads((root / 'reference-lock.json').read_text())
parser = argparse.ArgumentParser()
parser.add_argument('--mods', type=Path, default=Path(lock['referenceDirectory']))
args = parser.parse_args()
verified = []
for artifact in lock['artifacts']:
    source = args.mods / artifact['file']
    with source.open('rb') as stream:
        actual = hashlib.file_digest(stream, 'sha256').hexdigest()
    if actual != artifact['sha256']:
        raise SystemExit(f'Checksum mismatch for {source.name}: {actual}; reference target was not changed.')
    verified.append(source)
(root / 'libs').mkdir(exist_ok=True)
for source in verified:
    shutil.copy2(source, root / 'libs' / source.name)
    print(f'Verified {source.name}')
