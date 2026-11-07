package runtimechecks;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Counter/phase probes on real classes; never assembles machines or changes the live server. */
public class CoverageRuntimeChecks extends NativeChecks {
    static Map<String,Object> fusion(String type,int speed)throws Exception {
        Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");cfg.getField("multiplier").setInt(null,speed);
        Base base=new Base();base.energy=1000000000L;Object machine=cloneMachine(prototype(type,-1),base);
        stage(machine,200,true);set(machine,"mProgresstime",10);set(machine,"mMachine",true);set(machine,"mRunningOnLoad",false);set(machine,"mUpdated",false);set(machine,"mUpdate",-10);set(machine,"mStartUpCheck",-10);
        String owner=c("gregtech.common.tileentities.machines.multi.MTEFusionComputer").isInstance(machine)?"gregtech.common.tileentities.machines.multi.MTEFusionComputer":"goodgenerator.blocks.tileEntity.base.MTELargeFusionComputer";
        declared(c(owner),"onPostTick",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity"),long.class).invoke(machine,base.proxy,201L);
        int delta=((Number)get(machine,"mProgresstime")).intValue()-10;
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("class",type);r.put("multiplier",speed);r.put("progress_delta",delta);r.put("energy_spent",base.spent);return r;
    }
    static Map<String,Object> cold(String type)throws Exception {
        Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");cfg.getField("multiplier").setInt(null,5);
        Base base=new Base();Object machine=cloneMachine(prototype(type,-1),base);stage(machine,200,true);set(machine,"mProgresstime",10);
        declared(c("gregtech.api.metatileentity.implementations.MTEMultiBlockBase"),"incrementProgressTime").invoke(machine);
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("class",type);r.put("multiplier",5);r.put("progress_delta",((Number)get(machine,"mProgresstime")).intValue()-10);r.put("scope","native progress clock only; no structure or full coolant recipe validation");return r;
    }
    static Map<String,Object> target(int speed)throws Exception {
        c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig").getField("multiplier").setInt(null,speed);
        Base base=new Base();Object machine=cloneMachine(prototype("gtnhlanth.common.tileentity.MTETargetChamber",-1),base);stage(machine,200,true);set(machine,"mProgresstime",10);set(machine,"lastTCRecipeInputParticle",-1);
        declared(c("gtnhlanth.common.tileentity.MTETargetChamber"),"incrementProgressTime").invoke(machine);
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("class",machine.getClass().getName());r.put("multiplier",speed);r.put("progress_delta",((Number)get(machine,"mProgresstime")).intValue()-10);r.put("scope","native no-beam-required branch; not a full assembled recipe");return r;
    }
    static Map<String,Object> beam(boolean filled)throws Exception {
        Object machine=cloneMachine(prototype("gregtech.common.tileentities.machines.multi.beamcrafting.MTEBeamCrafter",-1),new Base());
        stage(machine,200,true);set(machine,"currentRecipeParticleIDA",1);set(machine,"currentRecipeParticleIDB",2);set(machine,"currentRecipeMaxAmountA",100);set(machine,"currentRecipeMaxAmountB",100);set(machine,"currentRecipeCurrentAmountA",0);set(machine,"currentRecipeCurrentAmountB",0);
        Object buffer=get(machine,"bufferMap");Method put=buffer.getClass().getMethod("put",int.class,int.class);put.invoke(buffer,1,filled?100:0);put.invoke(buffer,2,filled?100:0);
        declared(c("gregtech.common.tileentities.machines.multi.beamcrafting.MTEBeamCrafter"),"contributeToProgress").invoke(machine);
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("buffer_full",filled);r.put("progress_after_one_call",get(machine,"mProgresstime"));r.put("required",200);r.put("amountA",get(machine,"currentRecipeCurrentAmountA"));r.put("amountB",get(machine,"currentRecipeCurrentAmountB"));return r;
    }
    static Map<String,Object> thermal(int speed)throws Exception {
        c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig").getField("multiplier").setInt(null,speed);
        Class<?> type=c("gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitPlasmaHeater");
        Object machine=cloneMachine(prototype(type.getName(),-1),new Base());stage(machine,2400,true);
        Object strategy=c("cn.elytra.gtnh.cutcorners.CutCorners").getMethod("getStrategy").invoke(null);int duration=(Integer)strategy.getClass().getMethod("getWaterPurificationCycleTime",int.class).invoke(strategy,2400);set(machine,"mMaxProgresstime",duration);
        Object plasmaHatch=cloneMachine(prototype("gregtech.api.metatileentity.implementations.MTEHatchInput",1),new Base()),coolantHatch=cloneMachine(prototype("gregtech.api.metatileentity.implementations.MTEHatchInput",1),new Base());
        set(machine,"plasmaInputHatch",plasmaHatch);set(machine,"coolantInputHatch",coolantHatch);
        Object plasma=get(type,"plasmaMaterial"),coolant=get(type,"coolantMaterial");Field fluid=fld(c("gregtech.api.metatileentity.implementations.MTEBasicTank"),"mFluid");Method tick=declared(type,"runMachine",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity"),long.class);
        Object base=machine.getClass().getMethod("getBaseMetaTileEntity").invoke(machine);long plasmaUsed=0,coolantUsed=0;
        for(int t=1;t<=duration;t++){
            boolean heating=get(machine,"state").toString().equals("Heating");
            Object p=heating?plasma.getClass().getMethod("getPlasma",long.class).invoke(plasma,10L):null;
            Object q=!heating?coolant.getClass().getMethod("getFluid",long.class).invoke(coolant,100L):null;
            fluid.set(plasmaHatch,p);fluid.set(coolantHatch,q);tick.invoke(machine,base,(long)t);
            Object leftP=fluid.get(plasmaHatch),leftQ=fluid.get(coolantHatch);if(heating)plasmaUsed+=10-(leftP==null?0:((Number)get(leftP,"amount")).intValue());else coolantUsed+=100-(leftQ==null?0:((Number)get(leftQ,"amount")).intValue());
        }
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("multiplier",speed);r.put("controller_cycle_ticks",duration);r.put("thermal_cycles_completed",get(machine,"cyclesCompleted"));r.put("temperature",get(machine,"currentTemperature"));r.put("state",get(machine,"state").toString());r.put("plasma_litres",plasmaUsed);r.put("coolant_litres",coolantUsed);r.put("scope","native thermal phase with replenished fixture hatches; no physical plant or power controller assembly");return r;
    }

    static Map<String,Object> scope(String type)throws Exception {
        c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig").getField("multiplier").setInt(null,5);
        Object machine=cloneMachine(prototype(type,-1),new Base());
        try{set(machine,"mEUt",-2);}catch(NoSuchFieldException ignored){}
        try{set(machine,"lEUt",-2L);}catch(NoSuchFieldException ignored){}
        int step=(Integer)c("cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress").getMethod("increment",Object.class,int.class,int.class).invoke(null,machine,10,200);
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("class",type);r.put("multiplier",5);r.put("configured_progress_step",step);r.put("scope","runtime policy on a staged working machine; not a world-extraction throughput test");return r;
    }

    static Map<String,Object> heatExchanger(String type)throws Exception {
        c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig").getField("multiplier").setInt(null,5);
        Object machine=cloneMachine(prototype(type,-1),new Base());stage(machine,200,true);set(machine,"mEUt",100);
        int step=(Integer)c("cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress").getMethod("increment",Object.class,int.class,int.class).invoke(null,machine,10,200);
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("class",type);r.put("positive_thermal_power",100);r.put("multiplier",5);r.put("progress_step",step);r.put("scope","runtime filtering with positive mEUt used by native heat-exchanger processing; no full fluid/steam plant test");return r;
    }
    public static void run(String output)throws Exception {
        Map<String,Object> report=new LinkedHashMap<String,Object>();List<Map<String,Object>> results=new ArrayList<Map<String,Object>>();
        Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");int prior=cfg.getField("multiplier").getInt(null);
        try {
            // Staging outputs uses the same item reflection helpers as native integration tests.
            snapshot();
            for(String type:new String[]{"gregtech.common.tileentities.machines.multi.MTEFusionComputer1","gregtech.common.tileentities.machines.multi.MTEFusionComputer2","gregtech.common.tileentities.machines.multi.MTEFusionComputer3","goodgenerator.blocks.tileEntity.MTELargeFusionComputer1","goodgenerator.blocks.tileEntity.MTELargeFusionComputer2","goodgenerator.blocks.tileEntity.MTELargeFusionComputer3","goodgenerator.blocks.tileEntity.MTELargeFusionComputer4","goodgenerator.blocks.tileEntity.MTELargeFusionComputer5","gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.advanced.MTEAdvFusionMk4","gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.advanced.MTEAdvFusionMk5"})for(int speed:new int[]{1,5}){
                try{results.add(fusion(type,speed));}catch(Throwable error){Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("class",type);r.put("speed",speed);r.put("fixture_error",error.toString());results.add(r);}
            }
            report.put("fusion",results);report.put("target",Arrays.asList(target(1),target(5)));report.put("beam",Arrays.asList(beam(false),beam(true)));report.put("thermal",Arrays.asList(thermal(1),thermal(5)));
            List<Map<String,Object>> cooling=new ArrayList<Map<String,Object>>();
            for(String type:new String[]{"gregtech.common.tileentities.machines.multi.MTEVacuumFreezer","gregtech.common.tileentities.machines.multi.MTECryogenicFreezer","bartworks.common.tileentities.multis.mega.MTEMegaVacuumFreezerLegacy","gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.MTEIndustrialVacuumFreezerLegacy","gregtech.common.tileentities.machines.multi.MTEMassSolidifier","goodgenerator.blocks.tileEntity.MTECoolantTower","gregtech.common.tileentities.machines.multi.pcb.MTEPCBCoolingTower"})try{cooling.add(cold(type));}catch(Throwable error){Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("class",type);r.put("fixture_error",error.toString());cooling.add(r);}
            report.put("cooling",cooling);
            List<Map<String,Object>> scopes=new ArrayList<Map<String,Object>>();for(String type:new String[]{"gregtech.common.tileentities.machines.basic.MTEMiner","gregtech.common.tileentities.machines.basic.MTEPump","gregtech.common.tileentities.machines.multi.MTEOreDrillingPlant1","gregtech.common.tileentities.machines.multi.MTEOilDrill1","gregtech.common.tileentities.machines.multi.steam.MTESteamWaterPump"})scopes.add(scope(type));report.put("scopePolicy",scopes);List<Map<String,Object>> hx=new ArrayList<Map<String,Object>>();for(String type:new String[]{"gregtech.common.tileentities.machines.multi.MTEHeatExchanger","goodgenerator.blocks.tileEntity.MTEExtremeHeatExchanger","gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.advanced.MTEAdvHeatExchanger"})hx.add(heatExchanger(type));report.put("heatExchangers",hx);report.put("status","completed");
        }catch(Throwable error){error.printStackTrace();report.put("error",error.toString());report.put("status","failed");}finally{cfg.getField("multiplier").setInt(null,prior);}
        Object gson=c("com.google.gson.Gson").newInstance();String json=(String)gson.getClass().getMethod("toJson",Object.class).invoke(gson,report);Files.write(Paths.get(output),json.getBytes("UTF-8"));
    }
}
