package runtimechecks;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Native MTEPump world removal/output/energy tests; oil drill productive loop probes. */
public final class PumpChecks extends NativeChecks {
    static int fixture;
    static class PumpBase extends Base {
        final int x = 4000 + 32 * fixture++, y = 160, z = 4000;
        long timer;
        PumpBase below;
        PumpBase() throws Exception {
            allowed = true; energy = 1000000000L;
            Object provider = call(world,"getChunkProvider","func_72863_F",new Class<?>[]{});
            call(provider,"loadChunk","func_73158_c",new Class<?>[]{int.class,int.class},x>>4,z>>4);
        }
        Object block(int bx,int by,int bz)throws Exception {
            return call(world,"getBlock","func_147439_a",new Class<?>[]{int.class,int.class,int.class},bx,by,bz);
        }
        @Override public Object invoke(Object unused,Method m,Object[] a)throws Throwable {
            String n=m.getName();
            if(n.equals("getXCoord"))return x;
            if(n.equals("getYCoord"))return (short)y;
            if(n.equals("getZCoord"))return z;
            if(n.equals("getTimer"))return timer;
            if(n.equals("isUniversalEnergyStored"))return energy>=((Number)a[0]).longValue();
            if(n.equals("getBlock"))return block((Integer)a[0],(Integer)a[1],(Integer)a[2]);
            if(n.equals("getBlockOffset"))return block(x+(Integer)a[0],y+(Integer)a[1],z+(Integer)a[2]);
            if(n.equals("getMetaID")||n.equals("getMetaIDOffset"))return call(world,"getBlockMetadata","func_72805_g",new Class<?>[]{int.class,int.class,int.class},
                (n.endsWith("Offset")?x:0)+(Integer)a[0],(n.endsWith("Offset")?y:0)+(Integer)a[1],(n.endsWith("Offset")?z:0)+(Integer)a[2]);
            if(n.equals("getIGregTechTileEntityAtSideAndDistance"))return below!=null&&(Integer)a[1]==1?below.proxy:null;
            if(n.equals("addStackToSlot")&&a[1]==null)return false;
            return super.invoke(unused,m,a);
        }
    }
    static void place(PumpBase b,int dx,int dy,Object block,int metadata)throws Exception {
        call(b.world,"setBlock","func_147465_d",new Class<?>[]{int.class,int.class,int.class,c("net.minecraft.block.Block"),int.class,int.class},
            b.x+dx,b.y+dy,b.z,block,metadata,2);
    }
    static void config(int speed,String mode)throws Exception {
        Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");
        cfg.getField("enabled").setBoolean(null,!mode.equals("disabled"));
        cfg.getField("gregTech").setBoolean(null,!mode.equals("gtechoff"));
        cfg.getField("pumping").setBoolean(null,!mode.equals("off"));
        cfg.getField("multiplier").setInt(null,speed);
    }
    static Object pump(PumpBase b,int tier,String fluid,boolean flowing)throws Exception {
        Object p=cloneMachine(prototype("gregtech.common.tileentities.machines.basic.MTEPump",tier),b);
        Object primary=get(c("net.minecraft.init.Blocks"),fluid,fluid.equals("water")?"field_150355_j":"field_150353_l");
        Object secondary=get(c("net.minecraft.init.Blocks"),"flowing_"+fluid,fluid.equals("water")?"field_150358_i":"field_150356_k");
        set(p,"mPrimaryPumpedBlock",primary);set(p,"mSecondaryPumpedBlock",secondary);
        place(b,0,-1,get(c("gregtech.common.misc.DrillingLogicDelegate"),"MINING_PIPE_TIP_BLOCK"),0);
        place(b,0,-2,get(c("net.minecraft.init.Blocks"),"air","field_150350_a"),0);
        Collection queue=(Collection)get(p,"mPumpList");
        for(int i=5;i>=1;i--) {
            place(b,i,-1,flowing?secondary:primary,flowing?1:0);
            queue.add(c("net.minecraft.world.ChunkPosition").getConstructor(int.class,int.class,int.class).newInstance(b.x+i,b.y-1,b.z));
        }
        return p;
    }
    static void post(Object p,PumpBase b,int tick)throws Exception {
        b.timer=200+tick;
        try {p.getClass().getMethod("onPostTick",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity"),long.class).invoke(p,b.proxy,b.timer);}
        catch(InvocationTargetException e){throw new RuntimeException("Pump tick failed: "+b.calls,e.getCause());}
    }
    static Object fluid(Object p)throws Exception{return p.getClass().getMethod("getDrainableStack").invoke(p);}
    static void clearFluid(Object p)throws Exception {p.getClass().getMethod("setDrainableStack",c("net.minecraftforge.fluids.FluidStack")).invoke(p,new Object[]{null});}
    static Map<String,Object> basic(int tier,int speed,String mode,String fluidName)throws Exception {
        config(speed,mode);PumpBase b=new PumpBase();boolean flowing=mode.equals("flowing");
        Object p=pump(b,tier,fluidName,flowing);
        int nativePeriod=Math.max(1,(160>>tier)/(flowing?4:1));
        int effective=mode.equals("off")||mode.equals("disabled")||mode.equals("gtechoff")?1:speed;
        int period=(nativePeriod+effective-1)/effective;
        long cost=((Number)p.getClass().getMethod("getEuUsagePerTier",int.class).invoke(null,tier)).longValue()/(flowing?4:1);
        if(mode.equals("unpowered"))b.energy=0;
        if(mode.equals("lowpower"))b.energy=(cost+effective-1)/effective-1;
        if(mode.equals("stopped"))b.allowed=false;
        if(mode.equals("full")) {
            Object tank=get(p,"fluidOutputTank");int cap=(Integer)tank.getClass().getMethod("getCapacity").invoke(tank);
            Object water=c("net.minecraftforge.fluids.FluidRegistry").getMethod("getFluidStack",String.class,int.class).invoke(null,fluidName,cap);
            p.getClass().getMethod("setDrainableStack",c("net.minecraftforge.fluids.FluidStack")).invoke(p,water);
        }
        boolean stalled=Arrays.asList("unpowered","lowpower","stopped","full").contains(mode);
        List<Integer> events=new ArrayList<Integer>();List<Long> charges=new ArrayList<Long>();int totalFluid=0,previous=5;
        int limit=nativePeriod*6+20;
        for(int t=1;t<=limit;t++) {
            long before=b.spent;post(p,b,t);
            int left=((Collection)get(p,"mPumpList")).size();
            if(left<previous) {
                check(previous-left==1,"Multiple fluid blocks consumed per real tick");events.add(t);charges.add(b.spent-before);
                Object out=fluid(p);int amount=out==null?0:((Number)get(out,"amount")).intValue();
                check(amount==(flowing?0:1000),"Wrong fluid quantity "+amount+" "+mode);totalFluid+=amount;clearFluid(p);
                check(b.block(b.x+events.size(),b.y-1,b.z)==get(c("net.minecraft.init.Blocks"),"air","field_150350_a"),"Fluid source was not removed");
            }
            previous=left;
            if(!stalled&&events.size()==5)break;
        }
        if(stalled) {check(events.isEmpty(),"Pump worked across "+mode+" gate");check(b.spent==0,"Pump spent EU while stalled");}
        else {
            check(events.size()==5,"Pump lost extraction "+tier+" "+mode+" events="+events+" queue="+get(p,"mPumpList"));
            for(int i=1;i<events.size();i++)check(events.get(i)-events.get(i-1)==period,"Wrong pump interval "+tier+" "+speed+" "+mode+" "+events);
            check(b.spent==5*cost/effective,"Wrong divided action energy "+charges+" total="+b.spent+" expected="+(5*cost/effective));
            check(totalFluid==(flowing?0:5000),"Duplicated/lost fluid");
        }
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("tier",tier);r.put("speed",speed);r.put("mode",mode);r.put("fluid",fluidName);
        r.put("native_interval",nativePeriod);r.put("effective_interval",period);r.put("extraction_ticks",events);r.put("action_EU",charges);r.put("total_EU",b.spent);r.put("output_mB",totalFluid);return r;
    }
    static Map<String,Object> persistence()throws Exception {
        config(5,"normal");PumpBase b=new PumpBase();Object p=pump(b,1,"lava",false);post(p,b,1);
        check(b.spent==12,"First action should carry 4/5 EU");
        Object nbt=c("net.minecraft.nbt.NBTTagCompound").newInstance();p.getClass().getMethod("saveNBTData",nbt.getClass()).invoke(p,nbt);
        int rem=(Integer)call(nbt,"getInteger","func_74762_e",new Class<?>[]{String.class},"CutCornersPumpRemainder");
        check(rem==4,"Fractional debt not saved");
        PumpBase b2=new PumpBase();Object loaded=pump(b2,1,"lava",false);
        loaded.getClass().getMethod("loadNBTData",nbt.getClass()).invoke(loaded,nbt);
        // Native load schedules queue rediscovery. Keep this focused on real energy carry after load.
        set(loaded,"wasPumping",false);set(loaded,"mPumpTimer",0);post(loaded,b2,1);
        check(b2.spent==13,"Reload lost fractional EU: "+b2.spent);
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("saved_remainder",rem);r.put("first_action_EU",b.spent);r.put("after_load_action_EU",b2.spent);return r;
    }
    static Map<String,Object> stacked(int speed)throws Exception {
        config(speed,"normal");PumpBase lower=new PumpBase(),upper=new PumpBase();Object p=pump(lower,1,"lava",false),top=pump(upper,1,"lava",false);upper.below=lower;
        set(p,"mPumpTimer",80);set(p,"mProgresstime",0);set(p,"mMaxProgresstime",80);
        post(top,upper,1);check(((Number)get(p,"mPumpTimer")).intValue()==80-speed,"Stack boost not scaled");
        check(((Number)get(p,"mProgresstime")).intValue()==speed,"Stack progress wrong");
        check(upper.spent==0&&((Collection)get(top,"mPumpList")).size()==5,"Upper pump extracted fluid");
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("speed",speed);r.put("lower_timer_after_upper_tick",get(p,"mPumpTimer"));r.put("upper_extracts",false);return r;
    }
    static Map<String,Object> pipeMovement(int speed,boolean retract)throws Exception {
        config(speed,"normal");PumpBase b=new PumpBase();Object p=pump(b,1,"lava",false);
        ((Collection)get(p,"mPumpList")).clear();set(p,"mPrimaryPumpedBlock",null);set(p,"mSecondaryPumpedBlock",null);
        Object air=get(c("net.minecraft.init.Blocks"),"air","field_150350_a");
        Object tip=get(c("gregtech.common.misc.DrillingLogicDelegate"),"MINING_PIPE_TIP_BLOCK");
        Object pipe=get(c("gregtech.common.misc.DrillingLogicDelegate"),"MINING_PIPE_BLOCK");
        for(int i=1;i<=5;i++)place(b,i,-1,air,0);
        place(b,0,-1,air,0);place(b,0,-2,air,0);
        Object input=copyStack(get(c("gregtech.common.misc.DrillingLogicDelegate"),"MINING_PIPE_STACK"));count.setInt(input,4);
        int slot=(Integer)p.getClass().getMethod("getInputSlot").invoke(p);
        ((Object[])get(p,"mInventory"))[slot]=input;
        if(retract) {b.allowed=false;for(int y=-1;y>=-4;y--)place(b,0,y,y==-4?tip:pipe,0);}
        int nativePeriod=retract?5:100,period=(nativePeriod+speed-1)/speed,first=0;
        for(int t=1;t<=nativePeriod+1;t++) {
            post(p,b,t);
            if(retract?b.added>0:b.block(b.x,b.y-1,b.z)==tip){first=t;break;}
        }
        int expected=retract?((period-(200%period))%period):period;
        if(expected==0)expected=period;
        check(first==expected,"Pipe "+(retract?"retraction":"descent")+" speed="+speed+" first="+first+" expected="+expected);
        check(retract?b.added==1:count.getInt(input)==3,"Wrong pipe item count");
        check(b.spent==0,"Empty-space pipe movement charged fluid EU");
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("speed",speed);r.put("retract",retract);r.put("first_action_tick",first);r.put("pipe_count_changed_by",1);return r;
    }
    static Map<String,Object> oilClock(int speed,String mode)throws Exception {
        config(speed,mode);Object[] all=(Object[])get(c("gregtech.api.GregTechAPI"),"METATILEENTITIES");List<Object> rows=new ArrayList<Object>();
        Class<?> oil=c("gregtech.common.tileentities.machines.multi.MTEOilDrillBase");
        Set<Class<?>> seen=new HashSet<Class<?>>();
        for(Object proto:all)if(proto!=null&&oil.isInstance(proto)&&seen.add(proto.getClass())) {
            Base b=new Base();Object p=cloneMachine(proto,b);stage(p,200,true);Base power=new Base();power.energy=mode.equals("unpowered")?0:1000000;
            Object hatch=cloneMachine(prototype("hatch.energy",1),power);((Collection)get(p,"mEnergyHatches")).add(hatch);tick(p,b,true,201);
            int progress=((Number)get(p,"mProgresstime")).intValue();int expected=mode.equals("unpowered")?0:mode.equals("off")||mode.equals("disabled")||mode.equals("gtechoff")?1:speed;
            check(mode.equals("unpowered")?progress<=0:progress==expected,"Oil drill clock "+p.getClass()+" "+progress+" expected "+expected);
            check(power.spent==(mode.equals("unpowered")?0:2),"Oil drill EU/t changed");
            Map<String,Object> row=new LinkedHashMap<String,Object>();row.put("class",p.getClass().getName());row.put("progress",progress);row.put("EU",power.spent);rows.add(row);
        }
        check(!rows.isEmpty(),"No oil drill tested");Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("speed",speed);r.put("mode",mode);r.put("drills",rows);return r;
    }
    public static void run(String output)throws Exception {
        Map<String,Object> r=new LinkedHashMap<String,Object>();Map<Field,Object> saved=new LinkedHashMap<Field,Object>();Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");
        for(String n:new String[]{"enabled","gregTech","pumping","multiplier"}){Field f=cfg.getField(n);saved.put(f,f.get(null));}
        try {
            snapshot();List<Object> pumps=new ArrayList<Object>();r.put("basic_pumps",pumps);
            Set<Integer> tiers=new TreeSet<Integer>();
            Class<?> pumpClass=c("gregtech.common.tileentities.machines.basic.MTEPump");
            for(Object proto:(Object[])get(c("gregtech.api.GregTechAPI"),"METATILEENTITIES"))if(proto!=null&&pumpClass.isInstance(proto))tiers.add(((Number)get(proto,"mTier")).intValue());
            check(!tiers.isEmpty(),"No registered pumps");r.put("registered_pump_tiers",tiers);
            for(int tier:tiers)for(int speed:new int[]{1,5})pumps.add(basic(tier,speed,"normal","lava"));
            pumps.add(basic(1,5,"normal","water"));
            for(String mode:new String[]{"off","disabled","gtechoff","unpowered","lowpower","stopped","full","flowing"})pumps.add(basic(1,5,mode,"lava"));
            pumps.add(basic(1,1000,"normal","lava"));
            List<Object> movement=new ArrayList<Object>();r.put("pipe_movement",movement);
            for(int speed:new int[]{1,5,1000})for(boolean retract:new boolean[]{false,true})movement.add(pipeMovement(speed,retract));
            r.put("saved_energy_carry",persistence());r.put("stacked",Arrays.asList(stacked(1),stacked(5)));
            List<Object> oils=new ArrayList<Object>();r.put("oil_drill_native_clock",oils);
            for(int speed:new int[]{1,5})oils.add(oilClock(speed,"normal"));
            for(String mode:new String[]{"off","disabled","gtechoff","unpowered"})oils.add(oilClock(5,mode));
            r.put("limits","Real basic-pump source removal, fluid output and EU. Oil drills use native productive ticks; physical structure assembly, world oil depletion and every output-hatch topology are not exercised.");r.put("status","passed");
        } catch(Throwable e){e.printStackTrace();r.put("status","failed");r.put("error",e.toString());}
        finally {for(Map.Entry<Field,Object> e:saved.entrySet())e.getKey().set(null,e.getValue());}
        Object gson=c("com.google.gson.Gson").newInstance();Files.write(Paths.get(output),((String)gson.getClass().getMethod("toJson",Object.class).invoke(gson,r)).getBytes("UTF-8"));
    }
}
