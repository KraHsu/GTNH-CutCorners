package runtimechecks;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
/** Regression checks for the coverage fixes, staged on native registered implementations. */
public class CoverageFixChecks extends CoverageRuntimeChecks {
    static Map<String,Object> thermal(int speed)throws Exception {
        c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig").getField("multiplier").setInt(null,speed);
        Class<?> type=c("gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitPlasmaHeater");
        Object machine=cloneMachine(prototype(type.getName(),-1),new Base());stage(machine,2400,true);
        Object strategy=c("cn.elytra.gtnh.cutcorners.CutCorners").getMethod("getStrategy").invoke(null);int duration=(Integer)strategy.getClass().getMethod("getWaterPurificationCycleTime",int.class).invoke(strategy,2400);set(machine,"mMaxProgresstime",duration);
        Object plasmaHatch=cloneMachine(prototype("gregtech.api.metatileentity.implementations.MTEHatchInput",1),new Base()),coolantHatch=cloneMachine(prototype("gregtech.api.metatileentity.implementations.MTEHatchInput",1),new Base());
        set(machine,"plasmaInputHatch",plasmaHatch);set(machine,"coolantInputHatch",coolantHatch);
        Object plasma=get(type,"plasmaMaterial"),coolant=get(type,"coolantMaterial");Field fluid=fld(c("gregtech.api.metatileentity.implementations.MTEBasicTank"),"mFluid");Method tick=declared(type,"runMachine",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity"),long.class);
        Object base=machine.getClass().getMethod("getBaseMetaTileEntity").invoke(machine);long plasmaUsed=0,coolantUsed=0;
        for(int t=1;t<=(duration+speed-1)/speed;t++){
            boolean heating=get(machine,"state").toString().equals("Heating");
            Object p=heating?plasma.getClass().getMethod("getPlasma",long.class).invoke(plasma,10L):null;
            Object q=!heating?coolant.getClass().getMethod("getFluid",long.class).invoke(coolant,100L):null;
            fluid.set(plasmaHatch,p);fluid.set(coolantHatch,q);tick.invoke(machine,base,(long)t);
            Object leftP=fluid.get(plasmaHatch),leftQ=fluid.get(coolantHatch);if(heating)plasmaUsed+=10-(leftP==null?0:((Number)get(leftP,"amount")).intValue());else coolantUsed+=100-(leftQ==null?0:((Number)get(leftQ,"amount")).intValue());
        }
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("multiplier",speed);r.put("controller_cycle_ticks",duration);r.put("real_cycle_ticks",(duration+speed-1)/speed);r.put("thermal_cycles_completed",get(machine,"cyclesCompleted"));r.put("temperature",get(machine,"currentTemperature"));r.put("state",get(machine,"state").toString());r.put("plasma_litres",plasmaUsed);r.put("coolant_litres",coolantUsed);r.put("scope","native thermal phase with replenished fixture hatches; no physical plant or power controller assembly");return r;
    }


    public static void run(String output)throws Exception {
        Map<String,Object> report=new LinkedHashMap<String,Object>();
        Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");int prior=cfg.getField("multiplier").getInt(null);boolean mining=cfg.getField("mining").getBoolean(null);cfg.getField("mining").setBoolean(null,false);report.put("mining_disabled_for_legacy_scope_checks",true);
        try {
            report.put("network",network());snapshot();
            List<Map<String,Object>> f=new ArrayList<Map<String,Object>>();
            for(String type:new String[]{"gregtech.common.tileentities.machines.multi.MTEFusionComputer1","gregtech.common.tileentities.machines.multi.MTEFusionComputer2","gregtech.common.tileentities.machines.multi.MTEFusionComputer3","goodgenerator.blocks.tileEntity.MTELargeFusionComputer1","goodgenerator.blocks.tileEntity.MTELargeFusionComputer2","goodgenerator.blocks.tileEntity.MTELargeFusionComputer3","goodgenerator.blocks.tileEntity.MTELargeFusionComputer4","goodgenerator.blocks.tileEntity.MTELargeFusionComputer5","gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.advanced.MTEAdvFusionMk4","gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.advanced.MTEAdvFusionMk5"})for(int speed:new int[]{1,5}){
                Map<String,Object> r=fusion(type,speed);check(((Number)r.get("progress_delta")).intValue()==speed,"Fusion step "+r);check(((Number)r.get("energy_spent")).longValue()==2,"Fusion energy "+r);f.add(r);
            }report.put("fusion",f);
            List<Map<String,Object>> t=new ArrayList<Map<String,Object>>();for(int speed:new int[]{1,5}){Map<String,Object> r=target(speed);check(((Number)r.get("progress_delta")).intValue()==speed,"Target step "+r);t.add(r);}report.put("target",t);
            List<Map<String,Object>> thermalRows=new ArrayList<Map<String,Object>>();for(int speed:new int[]{1,5}){Map<String,Object> r=thermal(speed);check(((Number)r.get("thermal_cycles_completed")).intValue()==4,"Thermal cycles "+r);check(((Number)r.get("plasma_litres")).intValue()==400,"Plasma inputs "+r);check(((Number)r.get("coolant_litres")).intValue()==8000,"Coolant inputs "+r);thermalRows.add(r);}report.put("thermal",thermalRows);
            List<Map<String,Object>> scopes=new ArrayList<Map<String,Object>>();for(String type:new String[]{"gregtech.common.tileentities.machines.basic.MTEMiner","gregtech.common.tileentities.machines.basic.MTEPump","gregtech.common.tileentities.machines.multi.MTEOreDrillingPlant1","gregtech.common.tileentities.machines.multi.MTEOilDrill1","gregtech.common.tileentities.machines.multi.steam.MTESteamWaterPump"}){Map<String,Object> r=scope(type);check(((Number)r.get("configured_progress_step")).intValue()==1,"Extraction scope "+r);scopes.add(r);}report.put("scope",scopes);
            List<Map<String,Object>> hx=new ArrayList<Map<String,Object>>();for(String type:new String[]{"gregtech.common.tileentities.machines.multi.MTEHeatExchanger","goodgenerator.blocks.tileEntity.MTEExtremeHeatExchanger","gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.advanced.MTEAdvHeatExchanger"}){Map<String,Object> r=heatExchanger(type);check(((Number)r.get("progress_step")).intValue()==5,"Exchanger step "+r);hx.add(r);}report.put("heatExchangers",hx);
            report.put("status","passed");
        }catch(Throwable error){error.printStackTrace();report.put("status","failed");report.put("error",error.toString());}finally{cfg.getField("mining").setBoolean(null,mining);cfg.getField("multiplier").setInt(null,prior);}
        Object gson=c("com.google.gson.Gson").newInstance();Files.write(Paths.get(output),((String)gson.getClass().getMethod("toJson",Object.class).invoke(gson,report)).getBytes("UTF-8"));
    }
}
