#!/usr/bin/env python3
"""Run coverage regression helpers in one already-running isolated GTNH JVM.

Requires the CompatHarness from run-local.py. Never connects to the production VM.
"""
import argparse
import json
from pathlib import Path
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--work', type=Path, required=True)
parser.add_argument('--java', type=Path, required=True, help='JDK java executable')
parser.add_argument('--asm', type=Path, required=True, help='ASM jar for compiling fixture helpers')
parser.add_argument('--run-native', action='store_true', help='Also run the existing primitive/common-machine suite')
args = parser.parse_args()
work = args.work.resolve()
source = Path(__file__).resolve().parent
assert (work/'checks/ready').exists(), 'Start the isolated pack with run-local.py first'
assert not (work/'checks/request.properties').exists(), 'Another helper request is pending'
build = work/'coverage-classes'
build.mkdir(exist_ok=True)
names = ['NativeChecks', 'CoverageRuntimeChecks', 'CoverageFixChecks', 'CoverageExtraChecks', 'CoveragePipelineChecks', 'CoverageAdapterChecks', 'MiningChecks']
subprocess.run([str(args.java.resolve().with_name('javac')), '--release', '8', '-Xlint:-options', '-cp', str(args.asm.resolve()), '-d', str(build)] + [str(source/(name+'.java')) for name in names], check=True)
helper = work/'coverage-suite.jar'
subprocess.run([str(args.java.resolve().with_name('jar')), 'cf', str(helper), '-C', str(build), '.'], check=True)
checks = ['CoverageFixChecks', 'CoverageExtraChecks', 'CoveragePipelineChecks', 'CoverageAdapterChecks', 'MiningChecks']
if args.run_native:
    checks.append('NativeChecks')
results = {}
for name in checks:
    output = work/(name+'.json')
    output.unlink(missing_ok=True)
    Path(str(output)+'.error').unlink(missing_ok=True)
    request = work/'checks/request.tmp'
    request.write_text(f'jar={helper}\noutput={output}\nclass=runtimechecks.{name}\n')
    request.replace(work/'checks/request.properties')
    deadline = time.monotonic()+120
    while not output.exists():
        if Path(str(output)+'.error').exists():
            raise SystemExit(Path(str(output)+'.error').read_text())
        if time.monotonic()>deadline:
            raise SystemExit('Timed out: '+name)
        time.sleep(.25)
    result = json.loads(output.read_text())
    if result.get('status')!='passed':
        raise SystemExit(f'{name} failed: {result.get("error")}')
    results[name] = result
    print(name+': passed', flush=True)
(work/'coverage-suite-results.json').write_text(json.dumps(results, ensure_ascii=False, indent=2)+'\n')
print('Saved '+str(work/'coverage-suite-results.json'))
