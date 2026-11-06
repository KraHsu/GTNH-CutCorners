#!/usr/bin/env python3
"""One isolated JVM; helpers can be rebuilt/reloaded without restarting GTNH.

Usage: python3 integration/run-local.py --template /path/to/GTNH-server \
 --mod build/libs/CutCorners-xxx.jar \
 --java /path/to/java25 --work /tmp/cutc-runtime-test

The template is copied; its world, properties and configs are never modified.
The template must already accept the EULA, or the operator must pass --accept-eula.
"""
import argparse,json,os,shutil,subprocess,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--template',type=Path);p.add_argument('--mod',type=Path);p.add_argument('--java',type=Path);p.add_argument('--work',type=Path,required=True);p.add_argument('--stop',action='store_true');p.add_argument('--request',action='store_true');p.add_argument('--accept-eula',action='store_true',help='Explicitly accept Minecraft EULA in this isolated copy only');a=p.parse_args()
a.work=a.work.resolve()
if a.stop:(a.work/'checks/stop').touch();raise SystemExit()
if a.request:
 props=f'jar={a.work}/native-checks.jar\noutput={a.work}/native-results.json\n'
 (a.work/'checks/request.tmp').write_text(props);(a.work/'checks/request.tmp').replace(a.work/'checks/request.properties');print('Queued native checks');raise SystemExit()
assert all([a.template,a.mod,a.java]),'Provide --template, --mod and --java'
assert a.accept_eula or 'eula=true' in (a.template/'eula.txt').read_text(),'Provide an accepted template or explicitly --accept-eula'
assert not a.work.exists(),'Refusing to overwrite an existing test directory'
shutil.copytree(a.template,a.work)
if a.accept_eula:(a.work/'eula.txt').write_text('eula=true\n')
for mod in (a.work/'mods').glob('*'):
 if mod.name=='seedroller-probe.jar' or mod.name.lower().startswith(('cutcorners-','conditional_mixin-')):mod.unlink()
for path in [a.mod]:shutil.copy2(path,a.work/'mods'/path.name)
(a.work/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nwhite-list=true\nmax-players=1\nlevel-name=RuntimeTestWorld\nlevel-seed=1\nlevel-type=FLAT\ngenerator-settings=2;7,2x3,2;1;\nview-distance=2\nmax-tick-time=-1\nenable-rcon=false\nenable-query=false\n')
java=a.java.resolve();javac=java.with_name('javac');jar=java.with_name('jar');build=a.work/'checks-build';build.mkdir()
forge=a.work/'forge-1.7.10-10.13.4.1614-1.7.10-universal.jar'
subprocess.run([str(javac),'--release','8','-Xlint:-options','-cp',str(forge),'-d',str(build),str(ROOT/'integration/CompatHarness.java')],check=True)
subprocess.run([str(jar),'cf',str(a.work/'mods/runtime-harness.jar'),'-C',str(build),'.'],check=True)
subprocess.run([str(javac),'--release','8','-Xlint:-options','-d',str(build),str(ROOT/'integration/NativeChecks.java')],check=True)
subprocess.run([str(jar),'cf',str(a.work/'native-checks.jar'),'-C',str(build),'runtimechecks/NativeChecks.class','-C',str(build),'runtimechecks/NativeChecks$Base.class'],check=True)
with (a.work/'server.log').open('w') as log:
 proc=subprocess.Popen([str(java),'-Xms1G','-Xmx6G','-XX:ActiveProcessorCount=4','-Djava.awt.headless=true','-Duser.language=en','@java9args.txt','-jar','lwjgl3ify-forgePatches.jar','nogui'],cwd=a.work,stdin=subprocess.DEVNULL,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
 (a.work/'pid').write_text(str(proc.pid))
 deadline=time.monotonic()+240
 while not (a.work/'checks/ready').exists():
  if proc.poll() is not None:raise SystemExit('Server exited; see '+str(a.work/'server.log'))
  if time.monotonic()>deadline:proc.terminate();raise SystemExit('Startup timed out')
  time.sleep(.5)
print('Ready',proc.pid,'work',a.work)
