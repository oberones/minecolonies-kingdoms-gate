#!/usr/bin/env python3
"""Copy only checksum-verified reference dependencies into the local build cache."""
import argparse
import hashlib
import json
import shutil
import tempfile
import urllib.request
from pathlib import Path

root = Path(__file__).resolve().parents[1]
lock = json.loads((root / 'reference-lock.json').read_text())
parser = argparse.ArgumentParser()
source_group = parser.add_mutually_exclusive_group()
source_group.add_argument('--mods', type=Path, default=Path(lock['referenceDirectory']))
source_group.add_argument('--download', action='store_true', help='Download pinned public artifacts instead of reading a local installation.')
args = parser.parse_args()
# Stage downloads and verify the entire set before replacing any local JARs.
with tempfile.TemporaryDirectory(prefix='kingdoms-gate-dependencies-') as directory:
    downloads = json.loads((root / 'tools/reference-downloads.json').read_text()) if args.download else {}
    verified = []
    for artifact in lock['artifacts']:
        source = args.mods / artifact['file']
        if args.download:
            source = Path(directory) / artifact['file']
            request = urllib.request.Request(downloads[artifact['file']], headers={'User-Agent': 'kingdoms-gate-build/1.0'})
            with urllib.request.urlopen(request, timeout=120) as response, source.open('wb') as output:
                shutil.copyfileobj(response, output)
        with source.open('rb') as stream:
            actual = hashlib.file_digest(stream, 'sha256').hexdigest()
        if actual != artifact['sha256']:
            raise SystemExit(f'Checksum mismatch for {source.name}: {actual}; reference target was not changed.')
        verified.append(source)
    (root / 'libs').mkdir(exist_ok=True)
    for source in verified:
        shutil.copy2(source, root / 'libs' / source.name)
        print(f'Verified {source.name}')
