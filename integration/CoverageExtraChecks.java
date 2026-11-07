package runtimechecks;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;

/** Native non-GT processing probes and BEC gate fixtures; no production access. */
public class CoverageExtraChecks extends NativeChecks {
    static Object world() throws Exception {return c("net.minecraftforge.common.DimensionManager").getMethod("getWorld",int.class).invoke(null,0);}
    static void attach(Object tile)throws Exception {set(tile,"field_145850_b",world());set(tile,"field_145851_c",0);set(tile,"field_145848_d",250);set(tile,"field_145849_e",0);}
    static Object stack(String name)throws Exception {
        Object i=call(registry,"getObject","func_82594_a",new Class<?>[]{String.class},name);
        return c("net.minecraft.item.ItemStack").getConstructor(c("net.minecraft.item.Item"),int.class,int.class).newInstance(i,1,0);
    }
    static Map<String,Object> ic2(int speed,boolean powered)throws Exception {
        c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig").getField("multiplier").setInt(null,speed);
        Object tile=c("ic2.core.block.machine.tileentity.TileEntityElectricFurnace").newInstance();attach(tile);
        Object input=get(tile,"inputSlot");input.getClass().getMethod("put",c("net.minecraft.item.ItemStack")).invoke(input,stack("minecraft:potato"));
        set(tile,"progress",(short)10);set(tile,"operationLength",200);set(tile,"energyConsume",2);set(tile,"active",true);set(tile,"energy",powered?10000.0:0.0);
        declared(c("ic2.core.block.machine.tileentity.TileEntityStandardMachine"),"updateEntityServer").invoke(tile);
        int step=((Number)get(tile,"progress")).intValue()-10;double energy=((Number)get(tile,"energy")).doubleValue();
        check(step==(powered?speed:0),"IC2 native step "+step+" speed "+speed+" powered "+powered);check(energy==(powered?9998.0:0.0),"IC2 per-tick energy changed");
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("speed",speed);r.put("powered",powered);r.put("progress_delta",step);r.put("energy_after",energy);return r;
    }
    static Map<String,Object> ender(int speed,boolean powered)throws Exception {
        c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig").getField("multiplier").setInt(null,speed);
        Object tile=c("crazypants.enderio.machine.alloy.TileAlloySmelter").newInstance();final float[] credited={0};
        Class<?> iface=c("crazypants.enderio.machine.IPoweredTask");Object task=Proxy.newProxyInstance(iface.getClassLoader(),new Class<?>[]{iface},new InvocationHandler(){public Object invoke(Object proxy,Method method,Object[] args){if(method.getName().equals("update")){credited[0]+=((Number)args[0]).floatValue();return null;}if(method.getReturnType()==boolean.class)return false;if(method.getReturnType()==float.class)return 0f;return null;}});
        set(tile,"currentTask",task);tile.getClass().getMethod("setEnergyStored",int.class).invoke(tile,powered?100:0);
        int used=(Integer)tile.getClass().getMethod("usePower",int.class).invoke(tile,10);int remaining=(Integer)tile.getClass().getMethod("getEnergyStored").invoke(tile);
        check(used==(powered?10:0)&&remaining==(powered?90:0),"EnderIO actual RF changed");check(credited[0]==(powered?10*speed:0),"EnderIO work credit "+credited[0]);
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("speed",speed);r.put("powered",powered);r.put("actual_rf_used",used);r.put("task_work",credited[0]);return r;
    }
    static class Fixtures extends ClassLoader {Fixtures(ClassLoader parent){super(parent);}Class<?> make(String name,byte[] bytes){return defineClass(name,bytes,0,bytes.length);}}
    static Class<?> assemblerFixture,losFixture,pauseFixture;
    static void fields(ClassWriter w,String... pairs){for(int i=0;i<pairs.length;i+=2)w.visitField(Opcodes.ACC_PUBLIC,pairs[i],pairs[i+1],null,null).visitEnd();}
    static void getter(ClassWriter w,String owner,String method,String desc,String field,String fd,int opcode){MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,method,desc,null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,owner,field,fd);m.visitInsn(opcode);m.visitMaxs(2,3);m.visitEnd();}
    static void fixtures()throws Exception {
        if(assemblerFixture!=null)return;Fixtures loader=new Fixtures(c("tectech.thing.metaTileEntity.multi.bec.MTEBECAssembler").getClassLoader());
        String a="runtimechecks/generated/Assembler",base="tectech/thing/metaTileEntity/multi/bec/MTEBECAssembler";ClassWriter w=new ClassWriter(0);w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,a,null,base,null);
        fields(w,"nanites","I","tier","Lgregtech/api/enums/NaniteTier;","slowdowns","I","drained","J","provide","Z");
        getter(w,a,"getAvailableNanites","()I","nanites","I",Opcodes.IRETURN);getter(w,a,"getCurrentNaniteTier","()Lgregtech/api/enums/NaniteTier;","tier","Lgregtech/api/enums/NaniteTier;",Opcodes.ARETURN);getter(w,a,"getSlowdowns","(Ljava/util/Collection;)I","slowdowns","I",Opcodes.IRETURN);
        MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"drainCondensate","(Lappeng/api/storage/data/IAEFluidStack;)V",null,null);m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD,0);m.visitInsn(Opcodes.DUP);m.visitFieldInsn(Opcodes.GETFIELD,a,"drained","J");m.visitVarInsn(Opcodes.ALOAD,1);m.visitMethodInsn(Opcodes.INVOKEINTERFACE,"appeng/api/storage/data/IAEFluidStack","getStackSize","()J",true);m.visitInsn(Opcodes.LADD);m.visitFieldInsn(Opcodes.PUTFIELD,a,"drained","J");m.visitVarInsn(Opcodes.ALOAD,1);m.visitInsn(Opcodes.LCONST_0);m.visitMethodInsn(Opcodes.INVOKEINTERFACE,"appeng/api/storage/data/IAEFluidStack","setStackSize","(J)Lappeng/api/storage/data/IAEStack;",true);m.visitInsn(Opcodes.POP);m.visitInsn(Opcodes.RETURN);m.visitMaxs(5,2);m.visitEnd();w.visitEnd();assemblerFixture=loader.make(a.replace('/','.'),w.toByteArray());
        String l="runtimechecks/generated/LoS",h="tectech/thing/metaTileEntity/hatch/bec/MTEHatchLoS";w=new ClassWriter(0);w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,l,null,h,null);fields(w,"assembler","Lgregtech/api/metatileentity/MetaTileEntity;");getter(w,l,"getOwner","()Lgregtech/api/metatileentity/MetaTileEntity;","assembler","Lgregtech/api/metatileentity/MetaTileEntity;",Opcodes.ARETURN);m=w.visitMethod(Opcodes.ACC_PUBLIC,"getConnectedHatch","()L"+h+";",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(1,1);m.visitEnd();w.visitEnd();losFixture=loader.make(l.replace('/','.'),w.toByteArray());
        l="runtimechecks/generated/Pause";h="tectech/thing/metaTileEntity/hatch/bec/MTEHatchIONodeController";w=new ClassWriter(0);w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,l,null,h,null);fields(w,"mode","L"+h+"$Mode;");getter(w,l,"getMode","()L"+h+"$Mode;","mode","L"+h+"$Mode;",Opcodes.ARETURN);m=w.visitMethod(Opcodes.ACC_PUBLIC,"receivingSignal","()Z",null,null);m.visitCode();m.visitInsn(Opcodes.ICONST_1);m.visitInsn(Opcodes.IRETURN);m.visitMaxs(1,1);m.visitEnd();w.visitEnd();pauseFixture=loader.make(l.replace('/','.'),w.toByteArray());
    }
    static Object allocate(Class<?> type)throws Exception {Class<?> u=c("sun.misc.Unsafe");return u.getMethod("allocateInstance",Class.class).invoke(get(u,"theUnsafe"),type);}
    static Map<String,Object> bec(int speed,String mode)throws Exception {
        fixtures();c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig").getField("multiplier").setInt(null,speed);
        Object node=cloneMachine(prototype("tectech.thing.metaTileEntity.multi.bec.MTEBECIONode",-1),new Base());stage(node,200,true);set(node,"mProgresstime",mode.equals("tier-gate")||mode.equals("pause-step")?18:10);int before=((Number)get(node,"mProgresstime")).intValue();
        Object assembler=allocate(assemblerFixture);set(assembler,"mMaxProgresstime",200);set(assembler,"nanites",mode.equals("no-nanites")?0:1);set(assembler,"tier",get(c("gregtech.api.enums.NaniteTier"),"Carbon"));
        Object hatch=allocate(losFixture);set(hatch,"assembler",assembler);set(node,"losHatch",hatch);set(node,"powered",!mode.equals("unpowered"));set(node,"parallelRecipesInProgress",1);
        Class<?> tier=c("gregtech.api.enums.NaniteTier"),step=c("tectech.thing.metaTileEntity.multi.bec.MTEBECIONode$RecipeStep");Constructor<?> ctor=step.getDeclaredConstructor(tier,int.class,int.class,int.class);ctor.setAccessible(true);
        List<Object> steps=new ArrayList<Object>();steps.add(ctor.newInstance(get(tier,"Carbon"),0,20,0));steps.add(ctor.newInstance(get(tier,mode.equals("tier-gate")?"Silver":"Carbon"),20,200,1));set(node,"recipeSteps",steps);
        Object required=c("tectech.mechanics.boseEinsteinCondensate.CondensateList").newInstance(),consumed=required.getClass().newInstance();Object water=c("net.minecraftforge.fluids.FluidRegistry").getMethod("getFluid",String.class).invoke(null,"water");required.getClass().getMethod("addTo",Object.class,long.class).invoke(required,water,12L);set(node,"requiredCondensate",required);set(node,"consumedCondensate",consumed);
        if(mode.startsWith("pause")){Object pause=allocate(pauseFixture);set(pause,"mode",get(c("tectech.thing.metaTileEntity.hatch.bec.MTEHatchIONodeController$Mode"),mode.equals("pause-step")?"PAUSE_STEP":"PAUSE_INSTANT"));((List)get(node,"controllerHatches")).add(pause);}
        declared(c("tectech.thing.metaTileEntity.multi.bec.MTEBECIONode"),"incrementProgressTime").invoke(node);
        int after=((Number)get(node,"mProgresstime")).intValue();int expected=(mode.equals("unpowered")||mode.equals("no-nanites")||mode.equals("pause-instant"))?before:mode.equals("tier-gate")||mode.equals("pause-step")?Math.min(20,before+speed):before+speed;
        check(after==expected,"BEC "+mode+" progress "+after+" expected "+expected);long drained=((Number)get(assembler,"drained")).longValue();check(drained==0||drained==12,"BEC duplicated condensate "+drained);
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("speed",speed);r.put("mode",mode);r.put("before",before);r.put("after",after);r.put("condensate_consumed",drained);r.put("state",String.valueOf(get(node,"state")));return r;
    }
    public static void run(String output)throws Exception {
        Map<String,Object> report=new LinkedHashMap<String,Object>();Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");int prior=cfg.getField("multiplier").getInt(null);
        try {snapshot();List<Object> a=new ArrayList<Object>(),b=new ArrayList<Object>(),d=new ArrayList<Object>();
            for(int speed:new int[]{1,5})for(boolean powered:new boolean[]{true,false}){a.add(ic2(speed,powered));b.add(ender(speed,powered));}report.put("ic2",a);report.put("enderIO",b);
            for(int speed:new int[]{1,5})for(String mode:new String[]{"running","unpowered","no-nanites","tier-gate","pause-instant","pause-step"})d.add(bec(speed,mode));report.put("bec",d);report.put("status","passed");
        }catch(Throwable e){e.printStackTrace();report.put("status","failed");report.put("error",e.toString());}finally{cfg.getField("multiplier").setInt(null,prior);}
        Object gson=c("com.google.gson.Gson").newInstance();Files.write(Paths.get(output),((String)gson.getClass().getMethod("toJson",Object.class).invoke(gson,report)).getBytes("UTF-8"));
    }
}
