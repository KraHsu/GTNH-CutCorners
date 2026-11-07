package runtimechecks;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;

/** Coupled-clock, migration and native heat-exchanger processing checks. */
public class CoveragePipelineChecks extends CoverageExtraChecks {
    static Object plant(Base base)throws Exception {
        Object plant=cloneMachine(prototype("gregtech.common.tileentities.machines.multi.purification.MTEPurificationPlant",-1),base);stage(plant,2400,true);set(plant,"mMachine",true);
        Base energy=new Base();((Collection)get(plant,"mEnergyHatches")).add(cloneMachine(prototype("hatch.energy",1),energy));return plant;
    }
    static Map<String,Object> purification(int speed,boolean migration,boolean blacklist)throws Exception {
        Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");cfg.getField("multiplier").setInt(null,speed);Object old=cfg.getField("recipeMapBlacklist").get(null);
        try {
            Base base=new Base();Object plant=plant(base),unit=cloneMachine(prototype("MTEPurificationUnitPlasmaHeater",-1),new Base());stage(unit,2400,true);set(unit,"controller",plant);
            Object link=c("gregtech.common.tileentities.machines.multi.purification.LinkedPurificationUnit").getConstructor(c("gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitBase")).newInstance(unit);link.getClass().getMethod("setActive",boolean.class).invoke(link,true);((List)get(plant,"linkedUnits")).add(link);
            if(blacklist)cfg.getField("recipeMapBlacklist").set(null,new String[]{"gt.recipe.purificationplantplasmaheating"});
            if(migration){set(plant,"mMaxProgresstime",480);set(plant,"mProgresstime",120);set(unit,"mMaxProgresstime",480);set(unit,"mProgresstime",120);}else{set(plant,"mProgresstime",10);set(unit,"mProgresstime",10);}
            declared(c("gregtech.common.tileentities.machines.multi.purification.MTEPurificationPlant"),"runMachine",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity"),long.class).invoke(plant,base.proxy,20L);
            int expected=(migration?600:10)+(blacklist?1:speed),p=((Number)get(plant,"mProgresstime")).intValue(),u=((Number)get(unit,"mProgresstime")).intValue();check(p==expected&&u==expected,"Purification shared clock "+p+" / "+u+" expected "+expected);check(((Number)get(plant,"mMaxProgresstime")).intValue()==2400,"Native purification maximum lost");
            Object runtime=c("cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress");int step=(Integer)((Class<?>)runtime).getMethod("purificationSpeed",Object.class).invoke(null,unit);check(step==(blacklist?1:speed),"Purification blacklist phase clock differs");
            Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("speed",speed);r.put("migrated_from_480_tick_cycle",migration);r.put("unit_map_blacklisted",blacklist);r.put("controller_progress",p);r.put("unit_progress",u);r.put("unit_phase_speed",step);return r;
        }finally{cfg.getField("recipeMapBlacklist").set(null,old);}
    }
    static Map<String,Object> uv(int speed)throws Exception {
        c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig").getField("multiplier").setInt(null,speed);Base base=new Base();Object u=cloneMachine(prototype("MTEPurificationUnitUVTreatment",-1),base);stage(u,2400,true);Object lenses=get(c("gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitUVTreatment"),"LENS_ITEMS");set(u,"lensCycle",c("gregtech.common.tileentities.machines.multi.purification.UVTreatmentLensCycle").getConstructor(List.class).newInstance(lenses));set(u,"timeUntilNextSwap",100);Object hatch=cloneMachine(prototype("MTEHatchLensHousing",-1),new Base());set(u,"lensInputBus",hatch);declared(u.getClass(),"runMachine",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity"),long.class).invoke(u,base.proxy,1L);int wait=((Number)get(u,"timeUntilNextSwap")).intValue();check(wait==100-speed,"UV phase countdown "+wait);Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("speed",speed);r.put("wait_after_native_call",wait);return r;
    }
    static Class<?> exchanger(String target)throws Exception {
        String owner="runtimechecks/generated/"+target.substring(target.lastIndexOf('.')+1),parent=target.replace('.','/');ClassWriter w=new ClassWriter(0);w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,owner,null,parent,null);fields(w,"steam","J","waterUsed","J");
        MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","(Ljava/lang/String;)V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitVarInsn(Opcodes.ALOAD,1);m.visitMethodInsn(Opcodes.INVOKESPECIAL,parent,"<init>","(Ljava/lang/String;)V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(2,2);m.visitEnd();
        for(String name:new String[]{"addOutput","addOutputPartial","depleteInput"}){boolean partial=name.equals("addOutputPartial");m=w.visitMethod(Opcodes.ACC_PUBLIC,name,"(Lnet/minecraftforge/fluids/FluidStack;)"+(partial?"V":"Z"),null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitInsn(Opcodes.DUP);m.visitFieldInsn(Opcodes.GETFIELD,owner,name.equals("depleteInput")?"waterUsed":"steam","J");m.visitVarInsn(Opcodes.ALOAD,1);m.visitFieldInsn(Opcodes.GETFIELD,"net/minecraftforge/fluids/FluidStack","amount","I");m.visitInsn(Opcodes.I2L);m.visitInsn(Opcodes.LADD);m.visitFieldInsn(Opcodes.PUTFIELD,owner,name.equals("depleteInput")?"waterUsed":"steam","J");if(partial)m.visitInsn(Opcodes.RETURN);else{m.visitInsn(Opcodes.ICONST_1);m.visitInsn(Opcodes.IRETURN);}m.visitMaxs(5,2);m.visitEnd();}w.visitEnd();return new Fixtures(c(target).getClassLoader()).make(owner.replace('/','.'),w.toByteArray());
    }
    static Map<String,Object> exchangerCase(String type,int speed)throws Exception {
        c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig").getField("multiplier").setInt(null,speed);Object m=exchanger(type).getConstructor(String.class).newInstance("isolated-heat-fixture");Base base=new Base();base.tile=m;m.getClass().getMethod("setBaseMetaTileEntity",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity")).invoke(m,base.proxy);stage(m,20,true);set(m,"mMaxProgresstime",0);
        Object hot=cloneMachine(prototype("gregtech.api.metatileentity.implementations.MTEHatchInput",1),new Base()),cold=cloneMachine(prototype("gregtech.api.metatileentity.implementations.MTEHatchOutput",1),new Base());boolean extreme=type.contains("Extreme");set(m,extreme?"mHotFluidHatch":"mInputHotFluidHatch",hot);set(m,extreme?"mCooledFluidHatch":"mOutputColdFluidHatch",cold);
        Object hotFluid;
        if(extreme){Object map=get(c("goodgenerator.api.recipe.GoodGeneratorRecipeMaps"),"extremeHeatExchangerFuels");Collection recipes=(Collection)map.getClass().getMethod("getAllRecipes").invoke(map);Object recipe=recipes.iterator().next();hotFluid=copyFluid(((Object[])get(recipe,"mFluidInputs"))[0]);}
        else hotFluid=c("net.minecraftforge.fluids.FluidRegistry").getMethod("getFluidStack",String.class,int.class).invoke(null,"lava",1000);
        set(hotFluid,"amount",1000);long coldAmount=0;int batches=0;
        for(int t=0;t<20;t++){
            if(((Number)get(m,"mMaxProgresstime")).intValue()==0){set(hot,"mFluid",copyFluid(hotFluid));set(cold,"mFluid",null);Object result=declared(c(type),extreme?"checkProcessing_EM":"checkProcessing").invoke(m);check((Boolean)result.getClass().getMethod("wasSuccessful").invoke(result),"Exchanger native recipe failed "+type);Object out=get(cold,"mFluid");check(out!=null,"Exchanger cold output missing");coldAmount+=((Number)get(out,"amount")).intValue();batches++;set(m,"mProgresstime",0);}
            declared(c(type),"onRunningTick",c("net.minecraft.item.ItemStack")).invoke(m,new Object[]{null});declared(c("gregtech.api.metatileentity.implementations.MTEMultiBlockBase"),"incrementProgressTime").invoke(m);if(((Number)get(m,"mProgresstime")).intValue()>=20)set(m,"mMaxProgresstime",0);
        }
        check(batches==speed,"Exchanger completed intervals "+batches);Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("class",type);r.put("speed",speed);r.put("real_ticks",20);r.put("batches_started",batches);r.put("cold_output",coldAmount);r.put("steam_output",get(m,"steam"));r.put("distilled_water",get(m,"waterUsed"));return r;
    }
    static Object copyFluid(Object fluid)throws Exception{return fluid.getClass().getMethod("copy").invoke(fluid);}
    public static void run(String output)throws Exception {
        Map<String,Object> report=new LinkedHashMap<String,Object>();Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");int prior=cfg.getField("multiplier").getInt(null);
        try{snapshot();List<Object> p=new ArrayList<Object>(),v=new ArrayList<Object>(),h=new ArrayList<Object>();for(int speed:new int[]{1,5}){p.add(purification(speed,false,false));p.add(purification(speed,true,false));p.add(purification(speed,false,true));v.add(uv(speed));}report.put("purification",p);report.put("UV",v);
            for(String type:new String[]{"gregtech.common.tileentities.machines.multi.MTEHeatExchanger","goodgenerator.blocks.tileEntity.MTEExtremeHeatExchanger","gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.advanced.MTEAdvHeatExchanger"}){Map<String,Object> one=exchangerCase(type,1),five=exchangerCase(type,5);check(one.get("steam_output").equals(five.get("steam_output")),"Steam rate changed "+type);check(one.get("distilled_water").equals(five.get("distilled_water")),"Water/tick changed "+type);check(((Number)five.get("cold_output")).longValue()==5*((Number)one.get("cold_output")).longValue(),"Cooling rate "+type);h.add(one);h.add(five);}report.put("heatExchangers",h);report.put("status","passed");
        }catch(Throwable e){e.printStackTrace();report.put("status","failed");report.put("error",e.toString());}finally{cfg.getField("multiplier").setInt(null,prior);}
        Object gson=c("com.google.gson.Gson").newInstance();Files.write(Paths.get(output),((String)gson.getClass().getMethod("toJson",Object.class).invoke(gson,report)).getBytes("UTF-8"));
    }
}
