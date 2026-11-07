#!/usr/bin/env python3
"""Read-only call-chain audit of scanner TSVs and registered GT prototypes.

Static reachability is evidence of an injection path, not a functional test.
"""
import argparse,csv,json,collections
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--bytecode',type=Path,required=True);p.add_argument('--inventory',type=Path);p.add_argument('--out',type=Path,required=True);a=p.parse_args();a.out.mkdir(exist_ok=True,parents=True)
def read(name):
 with (a.bytecode/name).open() as f:return list(csv.DictReader(f,delimiter='\t'))
classes={r['class']:r for r in read('classes.tsv')};methods={(r['owner'],r['name'],r['desc']):r for r in read('methods.tsv')};calls=collections.defaultdict(list);writes=collections.defaultdict(list)
for r in read('calls.tsv'):calls[(r['owner'],r['method'],r['desc'])].append(r)
for r in read('writes.tsv'):writes[(r['owner'],r['method'],r['desc'])].append(r)
def chain(c):
 result=[]
 while c and c!='null' and c not in result:
  result.append(c);c=classes.get(c,{}).get('super')
 return result
def resolve(owner,name,desc):
 for c in chain(owner):
  if (c,name,desc) in methods:return (c,name,desc)
 return None
hooks={
 ('gregtech/api/metatileentity/implementations/MTEBasicMachine','onPostTick'):'shared-singleblock',
 ('gregtech/api/metatileentity/implementations/MTEMultiBlockBase','incrementProgressTime'):'shared-multiblock',
 ('gregtech/common/tileentities/machines/multi/MTEBrickedBlastFurnace','onPostTick'):'primitive-bricked-v3',
 ('gregtech/common/tileentities/machines/multi/MTECokeOven','checkRecipeProgress'):'primitive-coke-v3',
 ('gregtech/common/tileentities/machines/multi/purification/MTEPurificationPlant','startCycle'):'purification-cycle',
 ('tectech/thing/metaTileEntity/multi/MTEResearchStation','onRunningTick'):'research-computation',
 ('net/minecraft/tileentity/TileEntityFurnace','func_145845_h'):'vanilla-furnace',
 ('ganymedes01/etfuturum/tileentities/TileEntityBlastFurnace','func_145845_h'):'efr-furnace',
 ('ganymedes01/etfuturum/tileentities/TileEntitySmoker','func_145845_h'):'efr-furnace',
 ('thaumcraft/common/tiles/TileAlchemyFurnace','canSmelt'):'thaumcraft-alchemy-furnace',
 ('mods/railcraft/common/blocks/machine/alpha/TileBlastFurnace','func_145845_h'):'railcraft-blast',
 ('mods/railcraft/common/blocks/machine/alpha/TileCokeOven','func_145845_h'):'railcraft-coke',
}
rootdesc='(Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;J)V'
def audit(c):
 ancestry=chain(c);family=set(ancestry);todo=[]
 for name,desc in [('onPreTick',rootdesc),('onPostTick',rootdesc),('func_145845_h','()V'),('updateEntity','()V')]:
  found=resolve(c,name,desc)
  if found:todo.append(found)
 seen=set();paths={k:[k] for k in todo};foundhooks=[];advances=[]
 while todo:
  m=todo.pop()
  if m in seen:continue
  seen.add(m)
  if (m[0],m[1]) in hooks:foundhooks.append({'hook':hooks[(m[0],m[1])],'method':list(m),'path':[list(x) for x in paths[m]]})
  for w in writes.get(m,[]):
   if w['reads_same_field']!='true' or not any(op in w['arithmetic'] for op in ['96','97','98','99']):continue
   window=w['window'].split(' | ')
   if len(window)>1 and window[-2].split(' ')[0] in ['2','3','9','11','14']:continue
   if 'max' in w['field'].lower():continue
   advances.append({'owner':m[0],'method':m[1],'field':w['field'],'index':w['index'],'window':w['window'],'hooked_method':hooks.get((m[0],m[1])),'path':[list(x) for x in paths[m]]})
  for call in calls.get(m,[]):
   target=call['target'];opcode=int(call['opcode'])
   if target not in family:continue
   dispatch=c if opcode in (182,185) else target
   dest=resolve(dispatch,call['name'],call['target_desc'])
   if dest and dest not in seen:
    if dest not in paths:paths[dest]=paths[m]+[dest]
    todo.append(dest)
 return {'class':c.replace('/','.'),'jar':classes.get(c,{}).get('jar'),'ancestry':[x.replace('/','.') for x in ancestry],'reachable_methods':len(seen),'hooks':foundhooks,'candidate_counter_advances':advances,'has_progress_field':any('mProgresstime:' in classes.get(x,{}).get('fields','') for x in ancestry)}
if a.inventory:
 inv=json.loads(a.inventory.read_text());byclass=collections.defaultdict(list)
 for row in inv['machines']:byclass[row['class']].append(row)
 rows=[]
 for c,instances in sorted(byclass.items()):
  row=audit(c.replace('.','/'));row['instances']=instances;row['registered_count']=len(instances);rows.append(row)
else:
 rows=[audit(c) for c,r in classes.items() if r['selected']=='true' and int(r['access'])&0x0600==0]
(a.out/'call-chains.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n')
summary={'classes':len(rows),'with_hooks':sum(bool(r['hooks']) for r in rows),'progress_without_hooks':[r['class'] for r in rows if r['has_progress_field'] and not r['hooks']]}
(a.out/'summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n');print(json.dumps(summary,ensure_ascii=False))
