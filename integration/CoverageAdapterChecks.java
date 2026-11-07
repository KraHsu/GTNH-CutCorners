package runtimechecks;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Narrow checks of adapters on transformed pack classes; not full machine throughput tests. */
public class CoverageAdapterChecks extends NativeChecks {
    static Method handler(Object tile,String marker)throws Exception {
        for(Class<?> k=tile.getClass();k!=null;k=k.getSuperclass())for(Method m:k.getDeclaredMethods())if(m.getName().contains(marker)){m.setAccessible(true);return m;}
        throw new NoSuchMethodException(tile.getClass()+" "+marker);
    }
    static List<Object> adapters(int configuredSpeed,int speed)throws Exception {
        Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");cfg.getField("multiplier").setInt(null,configuredSpeed);List<Object> results=new ArrayList<Object>();
        for(String type:new String[]{"mods.railcraft.common.blocks.machine.alpha.TileRockCrusher","mods.railcraft.common.blocks.machine.alpha.TileRollingMachine"}){
            Object tile=c(type).newInstance();String field=type.endsWith("TileRockCrusher")?"processTime":"progress";set(tile,field,10);handler(tile,"cc$poweredProgress").invoke(tile,tile,11);check(((Number)get(tile,field)).intValue()==10+speed,"Railcraft work step");handler(tile,"cc$poweredProgress").invoke(tile,tile,0);check(((Number)get(tile,field)).intValue()==0,"Railcraft reset lost");Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("class",type);r.put("speed",speed);r.put("step",speed);r.put("reset",0);results.add(r);
        }
        Object oven=c("mods.railcraft.common.blocks.machine.alpha.TileSteamOven").newInstance();int duration=(Integer)handler(oven,"cc$cycleDuration").invoke(oven,256);check(duration==(256+speed-1)/speed,"Steam oven duration");results.add("Steam oven duration="+duration+" (native 16-tick polling retained)");
        Object tank=get(oven,"tank"),steam=c("net.minecraftforge.fluids.FluidRegistry").getMethod("getFluidStack",String.class,int.class).invoke(null,"steam",8000);tank.getClass().getMethod("fill",c("net.minecraftforge.fluids.FluidStack"),boolean.class).invoke(tank,steam,true);check((Boolean)declared(oven.getClass(),"drainSteam").invoke(oven),"Steam oven startup");int remaining=(Integer)tank.getClass().getMethod("getFluidAmount").invoke(tank);check(remaining==8000-(8000+speed-1)/speed,"Steam oven prepaid operating steam");results.add("Steam oven actual prepaid steam="+(8000-remaining));
        for(String type:new String[]{"TileCarpenter","TileCentrifuge","TileFermenter","TileSqueezer","TileStill","TileFabricator","TileBottler"}){
            Object tile=c("forestry.factory.tiles."+type).newInstance();CoverageExtraChecks.attach(tile);set(tile,"workCounter",10);set(tile,"ticksPerWorkCycle",100);int step=(Integer)handler(tile,"cc$paidWork").invoke(tile,1);check(step==speed,"Forestry work step "+type+"="+step);results.add(type+" work step="+step);
        }
        Object moist=c("forestry.factory.tiles.TileMoistener").newInstance();set(moist,"productionTime",100);handler(moist,"cc$processTime").invoke(moist,moist,99);check(((Number)get(moist,"productionTime")).intValue()==100-speed,"Moistener work countdown");results.add("Moistener countdown="+(100-speed));
        Object centrifuge=c("thaumcraft.common.tiles.TileCentrifuge").newInstance();set(centrifuge,"process",39);int decrement=(Integer)handler(centrifuge,"cc$separationTime").invoke(centrifuge,1);check(decrement==speed,"Thaumcraft centrifuge decrement");results.add("Thaumcraft centrifuge decrement="+decrement);
        Object infusion=c("thaumcraft.common.tiles.TileInfusionMatrix").newInstance();int interval=(Integer)handler(infusion,"cc$craftInterval").invoke(infusion,10);check(interval==(10+speed-1)/speed,"Infusion scheduler");results.add("Infusion interval="+interval);
        Object induction=c("ic2.core.block.machine.tileentity.TileEntityInduction").newInstance();set(induction,"progress",(short)100);handler(induction,"cc$poweredWork").invoke(induction,induction,(short)101);check(((Number)get(induction,"progress")).intValue()==100+speed,"Induction clock");handler(induction,"cc$poweredWork").invoke(induction,induction,(short)0);check(((Number)get(induction,"progress")).intValue()==0,"Induction reset lost");results.add("Induction step="+speed+", reset=0");return results;
    }
    static List<Object> scopeInventory()throws Exception {
        Object[] all=(Object[])get(c("gregtech.api.GregTechAPI"),"METATILEENTITIES");Set<Class<?>> seen=new HashSet<Class<?>>();List<Object> out=new ArrayList<Object>();Class<?> runtime=c("cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress");Field policy=runtime.getDeclaredField("EXTRACTION_CLASS");policy.setAccessible(true);ClassValue<?> cv=(ClassValue<?>)policy.get(null);
        for(Object m:all){if(m==null||!seen.add(m.getClass())||!Boolean.TRUE.equals(cv.get(m.getClass())))continue;int step=(Integer)runtime.getMethod("increment",Object.class,int.class,int.class).invoke(null,m,10,200);check(step==1,"Extraction class accelerated "+m.getClass());out.add(m.getClass().getName());}check(out.size()>=25,"Extraction inventory unexpectedly small "+out.size());return out;
    }
    public static void run(String output)throws Exception {
        Map<String,Object> r=new LinkedHashMap<String,Object>();Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");int prior=cfg.getField("multiplier").getInt(null);boolean enabled=cfg.getField("enabled").getBoolean(null);
        try{cfg.getField("enabled").setBoolean(null,true);r.put("one",adapters(1,1));r.put("five",adapters(5,5));cfg.getField("enabled").setBoolean(null,false);r.put("disabled",adapters(5,1));cfg.getField("enabled").setBoolean(null,true);cfg.getField("multiplier").setInt(null,5);r.put("excluded_registered_classes",scopeInventory());r.put("status","passed");}catch(Throwable e){e.printStackTrace();r.put("status","failed");r.put("error",e.toString());}finally{cfg.getField("multiplier").setInt(null,prior);cfg.getField("enabled").setBoolean(null,enabled);}
        Object gson=c("com.google.gson.Gson").newInstance();Files.write(Paths.get(output),((String)gson.getClass().getMethod("toJson",Object.class).invoke(gson,r)).getBytes("UTF-8"));
    }
}
