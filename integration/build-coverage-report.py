#!/usr/bin/env python3
"""Curated GTNH 2.9.0 coverage findings, backed by scanner and runtime probes."""
import argparse,json,datetime
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('directory',type=Path);a=p.parse_args();base=a.directory
inventory=json.loads((base/'machine-inventory.json').read_text());rows=json.loads((base/'registered/call-chains.json').read_text());probes=json.loads((base/'runtime-probes.json').read_text());byclass={r['class']:r for r in rows}
assert probes['status']=='completed'
def reject_errors(value):
 if isinstance(value,dict):
  assert not {'fixture_error','error'} & value.keys(), value
  for item in value.values():reject_errors(item)
 elif isinstance(value,list):
  for item in value:reject_errors(item)
reject_errors(probes)
assert len(probes['fusion'])==20 and all(r['progress_delta']==1 for r in probes['fusion'])
assert len(probes['target'])==2 and all(r['progress_delta']==1 for r in probes['target'])
assert len(probes['cooling'])==7 and all(r['progress_delta']==5 for r in probes['cooling'])
assert len(probes['heatExchangers'])==3 and all(r['progress_step']==1 for r in probes['heatExchangers'])
assert [(r['controller_cycle_ticks'],r['thermal_cycles_completed']) for r in probes['thermal']]==[(2400,4),(480,0)]
assert all(r['configured_progress_step']==5 for r in probes['scopePolicy'])
clock=[]
for r in rows:
 c=r['class']
 if any(s in c for s in ['.MTEFusionComputer1','.MTEFusionComputer2','.MTEFusionComputer3','.MTELargeFusionComputer','.MTEAdvFusionMk4','.MTEAdvFusionMk5']):clock.append(c)
clock+=['gtnhlanth.common.tileentity.MTETargetChamber','tectech.thing.metaTileEntity.multi.bec.MTEBECIONode']
assert len(clock)==12
thermal='gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitPlasmaHeater'
heat=['gregtech.common.tileentities.machines.multi.MTEHeatExchanger','goodgenerator.blocks.tileEntity.MTEExtremeHeatExchanger','gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.advanced.MTEAdvHeatExchanger']
scope=[r['class'] for r in probes['scopePolicy']]
primitive=['gregtech.common.tileentities.machines.multi.MTEBrickedBlastFurnace','gregtech.common.tileentities.machines.multi.MTECokeOven']
beam='gregtech.common.tileentities.machines.multi.beamcrafting.MTEBeamCrafter'
phase_rows=json.loads((base/'purification-phases.json').read_text());phase_review=[r['class'] for r in phase_rows if r['own_runMachine'] and r['class']!=thermal]
notes={}
for c in clock:notes[c]=('配方计时漏项','独立进度写入/重写 incrementProgressTime，未到达通用倍率入口。')
notes[thermal]=('冷却子阶段漏项','主周期已缩短，升温/降温仍每 20 个真实 tick 执行；探针中完整热循环由 4 轮变 0 轮。')
for c in heat:notes[c]=('产汽耦合冷却待适配','原生换热用正 mEUt 表示热功率，现有发电过滤使进度保持 1；需分离冷却与产汽速率。')
for c in scope:notes[c]=('范围偏差：采集被加速','按用户最新范围应保持采集原速，但运行时进度策略返回 5。')
for c in primitive:notes[c]=('runtime.3 已修复待切换','线上 runtime.2 尚无该入口；已准备的 runtime.3 通过真实配方测试。')
for c in phase_review:notes[c]=('净化子阶段需进一步核对','共享主周期已缩短，但有独立工艺子状态；不能仅凭主进度判定完整兼容。')
notes[beam]=('受供料限制，并非固定耗时漏项','原生方法在两种粒子均足量时单次调用即完成；空缓冲不推进，不能凭倍率凭空产生粒子。')
excluded=['gregtech.common.tileentities.machines.basic.MTEIndustrialApiary','gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchAirIntake','gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchAirIntakeAtmosphere','gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchAirIntakeExtreme','gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchReservoir']
for c in excluded:notes[c]=('按范围不加速','工业蜂房/集气/集水等非配方生产不纳入本次倍率。')
compact=[]
for r in rows:
 c=r['class'];maps=sorted(set(m for x in r['instances'] for m in x['maps']));hooks=sorted(set(h['hook'] for h in r['hooks']))
 status,note=notes.get(c,('',''))
 if not status:
  if '.generators.' in c or 'MTEAcidGenerator' in c:status,note='能源系统保持原速','发电不纳入本次倍率；不能把燃料配方页当加工计时覆盖。'
  elif '.purification.MTEPurificationUnit' in c:status,note='随净化主周期运行','不是独立推进 common incrementProgressTime 的机器。'
  elif hooks:status,note='可达加速入口（静态）','发现调用入口不等价于完整机器功能已实测；发电、黑名单和模块开关仍会排除。'
  else:status,note='辅助组件或未判定独立工艺','不能因继承进度字段或显示配方页就判定需要加速。'
 compact.append({'class':c,'jar':r['jar'],'status':status,'note':note,'maps':maps,'hooks':hooks,'instances':[{'id':i['id'],'name':i.get('display',i['name'])} for i in r['instances']]})
# Registered non-GT processing examples whose classes have no current adapter.
want={
 'ic2.core.block.machine.tileentity.':['TileEntityElectricFurnace','TileEntityInduction','TileEntityMacerator','TileEntityCompressor','TileEntityExtractor','TileEntityCentrifuge','TileEntityOreWashing','TileEntityMetalFormer','TileEntityCanner','TileEntitySolidCanner','TileEntityFluidBottler','TileEntityBlockCutter','TileEntityLathe','TileEntityRecycler'],
 'crazypants.enderio.machine.':['TileAlloySmelter','TileCrusher','TileSliceAndSplice','TileEntityPainter'],
 'forestry.factory.tiles.':['TileCarpenter','TileCentrifuge','TileFermenter','TileSqueezer','TileStill','TileFabricator','TileBottler','TileMoistener'],
 'mods.railcraft.common.blocks.machine.':['TileRockCrusher','TileRollingMachine','TileSteamOven'],
 'thaumcraft.common.tiles.':['TileCentrifuge','TileInfusionMatrix'],
}
non_gt=[]
for r in inventory['tiles']:
 if any(r['class'].startswith(prefix) and r['class'].rsplit('.',1)[-1] in names for prefix,names in want.items()):non_gt.append({**r,'status':'当前 CutCorners 无相应适配','note':'已注册的加工族示例。未额外应用本次 5 倍；未核验其合成可得性、玩家升级件或整合包自身倍率。'})
summary={'audit_time':datetime.datetime.now().astimezone().isoformat(),'live_version':'v1.4.0-runtime.2','staged_version':'v1.4.0-runtime.3','tested_version':'v1.4.0-runtime.3','desired_multiplier':5,'scope':'配方加工与工艺冷却；不加速发电、蜂房或无常规配方的采集生产。','registered_mte_entries':len(inventory['machines']),'registered_mte_classes':len(rows),'tile_registry_entries':len(inventory['tiles']),'tile_registry_classes':len({r['class'] for r in inventory['tiles']}),'exact_live_jars':221,'class_entries_scanned':94631,'timer_gap_classes':clock,'thermal_phase_gap':thermal,'energy_coupled_cooling':heat,'confirmed_scope_mismatch_samples':scope,'purification_phase_review':phase_review,'non_gt_unadapted_processing_examples':non_gt,'runtime_probes':probes,'production_changed':False,'limits':['Static method-chain analysis follows class/parent dispatch, not arbitrary member-object, reflection or all network callbacks.','Live-pack jar set is matched byte-for-byte. Runtime registry/probes come from an isolated GTNH template; extra live mods do not add GT machine classes.','Native tick/counter/phase probes stage working/structure/input conditions; no physical assembly of every machine, no player client or full factory throughput validation.','Known non-GT examples are not a full enumeration of every modded processing system.','Cold machine counter probes verify processing progress, not all auxiliary coolant/upgrade behaviour.']}
(base/'findings.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n');(base/'machine-status.json').write_text(json.dumps(compact,ensure_ascii=False,indent=2)+'\n')
data=json.dumps({'summary':summary,'machines':compact,'non_gt':non_gt},ensure_ascii=False).replace('<','\\u003c').replace('&','\\u0026')
page=r'''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>GTNH 2.9.0 加速覆盖审计</title><style>
:root{color-scheme:light dark;font-family:system-ui,-apple-system,"Noto Sans CJK SC",sans-serif}body{max-width:1320px;margin:32px auto;padding:0 24px;line-height:1.6}h1{font-size:28px}h2{font-size:20px;margin-top:30px}small,.muted{opacity:.7}section{padding:18px;border:1px solid #8885;border-radius:12px;margin:14px 0}table{border-collapse:collapse;width:100%;font-size:14px}td,th{text-align:left;border-bottom:1px solid #8884;padding:10px;vertical-align:top}th{position:sticky;top:0;background:Canvas}code{font-size:12px;overflow-wrap:anywhere}input,select,button{font:inherit;padding:8px 12px;margin:4px;border:1px solid #8887;border-radius:7px;background:Canvas;color:CanvasText}.warning{border-left:5px solid #df8c25}.good{border-left:5px solid #258879}.cards{display:flex;gap:12px;flex-wrap:wrap}.cards section{flex:1;min-width:170px}.big{font-size:27px;font-weight:650}.tag{font-size:12px;display:inline-block;border:1px solid #8885;border-radius:5px;padding:1px 5px;margin:2px}pre{white-space:pre-wrap;font-size:12px}details{margin-top:6px}#count{margin:12px 0}.scroll{overflow:auto;max-height:720px}a{color:#428fd2}</style>
<h1>GTNH 2.9.0 · 加速覆盖审计</h1><p id="versions"></p><p>目标：配方加工与工艺冷却 5 倍；发电、蜂房和无常规配方的采集生产保持原速。本轮只检查，没有切换或重启正式服。</p><div class="cards"><section><div class="big">3412 / 568</div>GT 注册项 / 实现类</section><section><div class="big">12</div>确认未接入倍率的 GT 配方计时实现</section><section><div class="big">4 → 0</div>净化热循环完整轮数（1× → 5×主周期）</section><section><div class="big">221</div>按线上 SHA-256 核对的模组 JAR</section></div>
<section class="warning"><h2>新增确认项</h2><ul><li>10 款聚变：GT Mk-I–III、Compact Mk-I–V、FusionTech Mk-IV/V；5 倍配置下原生 tick 仍只推进 1。</li><li>靶室、BEC 凝聚态传送节点：覆盖了原生进度方法，没有调用通用倍率入口。靶室已做计数器探针；BEC 为调用链与方法体确认。</li><li>净化等离子升降温：主周期 2400 → 480 tick，但加热/冷却仍每 20 个真实 tick 执行。相同供料探针中完整循环 4 → 0，终温为 3000，不能把主周期缩短视为完整适配。</li><li>大型/极限/XL 换热器：正 mEUt 是热功率，也触发了发电排除；冷却本身未 5 倍，需要与产汽速率分开处理。</li><li>范围反向偏差：基础矿机、泵、钻井/钻油与蒸汽水泵的进度策略仍返回 5，按最新范围应排除。未做实地开采吞吐测试。</li></ul></section>
<section class="good"><h2>已核实的覆盖与非遗漏</h2><p>真空冷冻、低温冷冻、新旧大型冷冻、凝固等 7 类的原生进度计数器在 5 倍配置下推进 5。它不代表所有辅助冷却剂或联动工艺均完成了功能测试。</p><p>束流合成器的进度是粒子到料量：满缓冲时单次调用即可完成，空缓冲不推进，不应为倍率凭空生成粒子。工业蜂房、集气/集水仓按用户确认范围不加速。</p><p>runtime.3 的砖高炉和 GT 焦炉修复已本机验证，但线上仍为 runtime.2，尚未切换。</p></section>
<h2>GT 注册实现明细</h2><input id="search" placeholder="搜索名称、中文结论、ID、类名或配方表" size="45"><select id="status"><option value="">全部判断</option></select><span id="count"></span><div class="scroll"><table><thead><tr><th>机器 / 注册数量</th><th>判断与范围</th><th>配方表 / 调用入口</th></tr></thead><tbody id="body"></tbody></table></div>
<h2>其他模组尚未适配的加工示例</h2><p class="muted">这些是已注册的加工族，不表示在当前整合包中均可合成，也不排除玩家升级件或整合包自身改变了速度。当前模组没有给它们统一叠加本次 5 倍。</p><table><thead><tr><th>注册名</th><th>类</th></tr></thead><tbody id="other"></tbody></table>
<h2>证据与限制</h2><p>静态扫描包含 Java 25 生效的多版本 JAR 类、继承和本类/父类调用链；再用隔离 JVM 导出实际注册表与原生方法探针。未逐台搭建结构或测试图形客户端，静态可达不等价于完整功能已验证。其他净化子工艺也列入需进一步核对项。</p><p><a href="findings.json">结构化结论与探针数据</a> · <a href="runtime-probes.json">原生探针原始数据</a> · <a href="machine-status.json">完整 GT 判断列表</a></p>
<script id="data" type="application/json">DATA</script><script>
const data=JSON.parse(document.getElementById('data').textContent),$=id=>document.getElementById(id);
$('versions').textContent=`线上 ${data.summary.live_version} · 已备 ${data.summary.staged_version} · 审计 ${data.summary.audit_time}`;
for(const status of [...new Set(data.machines.map(r=>r.status))].sort()){const o=document.createElement('option');o.value=o.textContent=status;$('status').append(o)}
function el(tag,text){const e=document.createElement(tag);if(text!==undefined)e.textContent=text;return e}
function render(){const q=$('search').value.trim().toLowerCase(),status=$('status').value,rows=data.machines.filter(r=>(!status||r.status===status)&&(!q||JSON.stringify(r).toLowerCase().includes(q)));$('body').replaceChildren();$('count').textContent=`${rows.length} / ${data.machines.length} 类`;
for(const r of rows){const tr=el('tr'),name=el('td'),judgment=el('td'),path=el('td');name.append(el('strong',r.instances[0]?.name||r.class),el('div',`${r.instances.length} 个注册项`));const d=el('details');d.append(el('summary','显示 ID 与名称'),el('pre',r.instances.map(x=>`${x.id}  ${x.name}`).join('\n')));name.append(d);judgment.append(el('strong',r.status),el('p',r.note));path.append(el('code',r.class),el('div',r.maps.join(', ')||'未报告普通配方表'));for(const h of r.hooks){const t=el('span',h);t.className='tag';path.append(t)}tr.append(name,judgment,path);$('body').append(tr)}}
$('search').addEventListener('input',render);$('status').addEventListener('change',render);render();for(const r of data.non_gt){const tr=el('tr');tr.append(el('td',r.id),el('td',r.class));$('other').append(tr)}
</script></html>'''.replace('DATA',data)
(base/'report.html').write_text(page)
print(json.dumps({'timer_gaps':len(clock),'heat_exchangers':len(heat),'purification_review':len(phase_review),'scope_mismatch_samples':len(scope),'non_gt_examples':len(non_gt),'report':str(base/'report.html')},ensure_ascii=False))
