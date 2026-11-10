package runtimechecks;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Real isolated-world GT miner extraction plus native multi-miner progress checks. */
public final class MiningChecks extends NativeChecks {
    static int fixture = 0;
    static Object family(String type) throws Exception {
        Class<?> parent=c(type);
        for(Object p:(Object[])get(c("gregtech.api.GregTechAPI"),"METATILEENTITIES"))if(p!=null&&parent.isInstance(p))return p;
        throw new IllegalStateException("No registered miner family "+type);
    }
    static final class MiningBase extends Base {
        final int x = 1000 + 32 * fixture++, y = 160, z = 1000;
        long timer;
        MiningBase() throws Exception {
            allowed = true;
            Object provider=call(world,"getChunkProvider","func_72863_F",new Class<?>[]{});
            call(provider,"loadChunk","func_73158_c",new Class<?>[]{int.class,int.class},x>>4,z>>4);
            Object air=get(c("net.minecraft.init.Blocks"),"air","field_150350_a");
            for(int dx=0;dx<=4;dx++)for(int dy=-5;dy<=0;dy++)place(this,dx,dy,0,air);
        }
        Object block(int x,int y,int z) throws Exception {
            return call(world,"getBlock","func_147439_a",new Class<?>[]{int.class,int.class,int.class},x,y,z);
        }
        @Override public Object invoke(Object unused,Method method,Object[] args) throws Throwable {
            String n = method.getName();
            if(n.equals("getXCoord")) return x;
            if(n.equals("getYCoord")) return (short)y;
            if(n.equals("getZCoord")) return z;
            if(n.equals("getTimer")) return timer;
            if(n.equals("disableWorking")) { allowed = false; return null; }
            if(n.equals("enableWorking")) { allowed = true; return null; }
            if(n.equals("isUniversalEnergyStored")) return energy >= ((Number)args[0]).longValue();
            if(n.equals("getBlockOffset")) return block(x+(Integer)args[0],y+(Integer)args[1],z+(Integer)args[2]);
            if(n.equals("getBlock")) return block((Integer)args[0],(Integer)args[1],(Integer)args[2]);
            if(n.equals("getMetaIDOffset")) return call(world,"getBlockMetadata","func_72805_g",
                new Class<?>[]{int.class,int.class,int.class},x+(Integer)args[0],y+(Integer)args[1],z+(Integer)args[2]);
            if(n.equals("addStackToSlot") && args[1] == null) return false;
            return super.invoke(unused,method,args);
        }
    }
    static void place(MiningBase b,int dx,int dy,int dz,Object block) throws Exception {
        call(b.world,"setBlock","func_147465_d",new Class<?>[]{int.class,int.class,int.class,c("net.minecraft.block.Block"),int.class,int.class},
            b.x+dx,b.y+dy,b.z+dz,block,0,2);
    }
    static int oresLeft(MiningBase b,Object ore) throws Exception {
        int count=0;for(int x=1;x<=4;x++)if(b.block(b.x+x,b.y-1,b.z)==ore)count++;return count;
    }
    static Map<String,Object> basic(int tier,int speed,String mode) throws Exception {
        Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");
        cfg.getField("enabled").setBoolean(null,!mode.equals("disabled"));cfg.getField("gregTech").setBoolean(null,!mode.equals("gtechoff"));
        cfg.getField("multiplier").setInt(null,speed);cfg.getField("mining").setBoolean(null,!mode.equals("off"));
        MiningBase b=new MiningBase();b.energy=mode.equals("unpowered")?0:10000000;
        Object m=cloneMachine(prototype("gregtech.common.tileentities.machines.basic.MTEMiner",tier),b);
        Object pipe=get(m,"pipe"),ore=get(c("net.minecraft.init.Blocks"),"iron_ore","field_150366_p");
        Object pipeBlock=get(c("gregtech.common.misc.DrillingLogicDelegate"),"MINING_PIPE_TIP_BLOCK");
        int nativeSpeed=((Number)get(m,"mSpeed")).intValue();
        boolean originalClock=mode.equals("off")||mode.equals("disabled")||mode.equals("gtechoff");
        int duration=originalClock?nativeSpeed:(nativeSpeed+speed-1)/speed;
        Object[] inventory=(Object[])get(m,"mInventory");
        int input=(Integer)m.getClass().getMethod("getInputSlot").invoke(m);
        int output=(Integer)m.getClass().getMethod("getOutputSlot").invoke(m);
        Object pipes=copyStack(get(c("gregtech.common.misc.DrillingLogicDelegate"),"MINING_PIPE_STACK"));
        count.setInt(pipes,4);if(!mode.equals("missing"))inventory[input]=pipes;
        boolean descend=mode.equals("descent")||mode.equals("missing");
        if(descend)place(b,0,-1,0,get(c("net.minecraft.init.Blocks"),"stone","field_150348_b"));
        else {place(b,0,-1,0,pipeBlock);for(int x=1;x<=4;x++)place(b,x,-1,0,ore);}
        if(mode.equals("blocked"))inventory[output]=outputStack();
        m.getClass().getMethod("onFirstTick",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity")).invoke(m,b.proxy);
        Method tick=m.getClass().getMethod("onPostTick",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity"),long.class);
        List<Integer> extracted=new ArrayList<Integer>();Map<String,Integer> outputs=new TreeMap<String,Integer>();
        int previous=descend?0:oresLeft(b,ore),ticks=0;
        if(!descend)check(previous==4,"Fixture ores missing before tick: "+previous+" blocks="+b.block(b.x+1,b.y-1,b.z)+" expected="+ore);
        int limit=mode.equals("unpowered")||mode.equals("blocked")||mode.equals("missing")?nativeSpeed*2+20:nativeSpeed*7+200;
        while(ticks<limit) {
            ticks++;b.timer=200+ticks;
            if(mode.equals("switch") && ticks==80) cfg.getField("multiplier").setInt(null,5);
            if(mode.equals("switchback") && ticks==33) cfg.getField("multiplier").setInt(null,1);
            try {tick.invoke(m,b.proxy,b.timer);}catch(InvocationTargetException e){throw new RuntimeException("basic "+tier+" "+mode+" tick "+ticks+" calls="+b.calls,e.getCause());}
            if(!mode.equals("blocked"))for(int slot=output;slot<output+2;slot++)if(inventory[slot]!=null) {
                Object stack=inventory[slot];String key=items(new Object[]{stack});
                outputs.put(key,outputs.getOrDefault(key,0)+1);inventory[slot]=null;
            }
            if(descend) {
                if(((Number)get(pipe,"tipDepth")).intValue()<0)break;
            } else {
                int left=oresLeft(b,ore);if(left<previous){check(left==previous-1,"More than one ore per action tier="+tier+" speed="+speed+" mode="+mode+" tick="+ticks+" before="+previous+" after="+left+" outputs="+outputs);extracted.add(ticks);}previous=left;
                if(left==0)break;
            }
        }
        if(mode.equals("normal")||originalClock||mode.equals("switch")||mode.equals("switchback")) {
            check(extracted.size()==4,"Missing ores "+tier+" speed="+speed+" "+mode+" extracted="+extracted+" max="+get(m,"mMaxProgresstime")+" progress="+get(m,"mProgresstime"));
            if(!mode.equals("switch")&&!mode.equals("switchback"))for(int i=1;i<extracted.size();i++)check(extracted.get(i)-extracted.get(i-1)==duration,"Wrong native extraction period "+extracted);
            if(mode.equals("switchback"))for(int i=1;i<extracted.size();i++)check(extracted.get(i)-extracted.get(i-1)==nativeSpeed,"Repeated extraction after disabling "+extracted);
            check(outputs.size()==1&&outputs.values().iterator().next()==4,"Ore outputs duplicated/lost: "+outputs);
            check(count.getInt(pipes)==4,"Existing pipe was consumed while mining ore");
        } else if(mode.equals("descent")) {
            check(((Number)get(pipe,"tipDepth")).intValue()==-1,"Pipe did not descend");check(count.getInt(pipes)==3,"Wrong mining-pipe consumption");
        } else {
            check(extracted.isEmpty(),"Unsafe mining through a supply gate: "+mode);
            check(((Number)get(pipe,"tipDepth")).intValue()==(descend?0:-1),"Pipe changed during supply stall");
        }
        if(mode.equals("unpowered")||mode.equals("blocked"))check(b.spent==0,"EU spent during a stall");
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("tier",tier);r.put("speed",speed);r.put("mode",mode);
        r.put("native_duration",nativeSpeed);r.put("duration",get(m,"mMaxProgresstime"));r.put("extraction_ticks",extracted);r.put("real_ticks",ticks);r.put("energy_spent",b.spent);r.put("outputs",outputs);r.put("pipes_remaining",inventory[input]==null?0:count.getInt(inventory[input]));
        return r;
    }
    static Map<String,Object> retract(int tier,int speed) throws Exception {
        Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");cfg.getField("mining").setBoolean(null,true);cfg.getField("multiplier").setInt(null,speed);
        MiningBase b=new MiningBase();b.allowed=false;
        Object m=cloneMachine(prototype("gregtech.common.tileentities.machines.basic.MTEMiner",tier),b),pipe=get(m,"pipe");
        Object block=get(c("gregtech.common.misc.DrillingLogicDelegate"),"MINING_PIPE_BLOCK"),tip=get(c("gregtech.common.misc.DrillingLogicDelegate"),"MINING_PIPE_TIP_BLOCK");
        for(int y=-1;y>=-4;y--)place(b,0,y,0,y==-4?tip:block);set(pipe,"tipDepth",-4);
        Method tick=m.getClass().getMethod("onPostTick",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity"),long.class);
        int nativeInterval=((Number)get(m,"mSpeed")).intValue()/5,interval=(nativeInterval+speed-1)/speed;
        List<Integer> events=new ArrayList<Integer>();int before=-4;
        for(int t=1;t<=nativeInterval*5;t++) {
            tick.invoke(m,b.proxy,(long)t);int now=((Number)get(pipe,"tipDepth")).intValue();
            if(now!=before){check(now==before+1,"Retraction skipped pipes");events.add(t);before=now;}
            if(now==0)break;
        }
        check(events.size()==4,"Not all pipes retracted");for(int t:events)check(t%interval==0,"Retraction interval "+events);
        int returned=0;for(Object stack:(Object[])get(m,"mInventory"))if(stack!=null)returned+=count.getInt(stack);
        check(returned==4,"Retraction duplicated/lost pipes "+returned);
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("tier",tier);r.put("speed",speed);r.put("events",events);r.put("returned_pipes",returned);return r;
    }
    static Map<String,Object> multiClock(String type,int speed,boolean powered) throws Exception {
        Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");cfg.getField("multiplier").setInt(null,speed);
        Base b=new Base();Object m=cloneMachine(family(type),b);stage(m,200,true);
        Base energy=new Base();energy.energy=powered?1000000:0;
        Object hatch=cloneMachine(prototype("hatch.energy",1),energy);((Collection)get(m,"mEnergyHatches")).add(hatch);
        Object orbitalBase=null;
        if(type.contains("TileEntityModuleMiner")) {
            orbitalBase=c("gregtech.api.metatileentity.BaseMetaTileEntity").newInstance();
            fld(orbitalBase.getClass(),"worldObj","field_145850_b").set(orbitalBase,b.world);
            orbitalBase.getClass().getMethod("setOwnerUuid",UUID.class).invoke(orbitalBase,UUID.fromString("00000000-0000-0000-0000-000000000001"));
            orbitalBase.getClass().getMethod("setOwnerName",String.class).invoke(orbitalBase,"RuntimeChecks");
            orbitalBase.getClass().getMethod("setMetaTileEntity",c("gregtech.api.interfaces.metatileentity.IMetaTileEntity")).invoke(orbitalBase,m);
            m.getClass().getMethod("setBaseMetaTileEntity",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity")).invoke(m,orbitalBase);
            set(orbitalBase,"mStoredEnergy",powered?1000000L:0L);
        }
        tick(m,b,true,201);
        int progress=((Number)get(m,"mProgresstime")).intValue();
        check(powered?progress==speed:progress<=0,"Multi-miner productive clock "+type+" power="+powered+" progress="+progress);
        long spent=orbitalBase==null?energy.spent:(powered?1000000L:0L)-((Number)get(orbitalBase,"mStoredEnergy")).longValue();
        check(spent==(powered?2:0),"Multi-miner EU/t changed "+type+" spent="+spent);
        Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("class",m.getClass().getName());r.put("speed",speed);r.put("powered",powered);r.put("progress",progress);r.put("EU_spent",spent);r.put("scope","native productive processing tick; physical structure and world extraction are not exercised for this multiblock");return r;
    }
    public static void run(String output) throws Exception {
        Map<String,Object> r=new LinkedHashMap<String,Object>();Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");
        Map<Field,Object> saved=new LinkedHashMap<Field,Object>();for(String n:new String[]{"enabled","gregTech","mining","pumping","multiplier"}){Field f=cfg.getField(n);saved.put(f,f.get(null));}
        try {
            snapshot();List<Object> basic=new ArrayList<Object>();r.put("basic_miners",basic);
            for(int tier=1;tier<=3;tier++)for(int speed:new int[]{1,5})basic.add(basic(tier,speed,"normal"));
            for(String mode:new String[]{"off","disabled","gtechoff","unpowered","blocked","missing","descent"})basic.add(basic(1,5,mode));
            basic.add(basic(1,1,"switch"));basic.add(basic(3,1000,"normal"));
            basic.add(basic(1,5,"switchback"));
            List<Object> retraction=new ArrayList<Object>();r.put("retraction",retraction);for(int speed:new int[]{1,5,1000})retraction.add(retract(3,speed));
            cfg.getField("multiplier").setInt(null,5);cfg.getField("mining").setBoolean(null,true);
            List<Object> multi=new ArrayList<Object>();r.put("multiblock_native_progress",multi);
            for(String type:new String[]{"gregtech.common.tileentities.machines.multi.MTEOreDrillingPlant1","bwcrossmod.galacticgreg.MTEVoidMinerBase","gtnhintergalactic.tile.multi.elevatormodules.TileEntityModuleMiner"}) {
                multi.add(multiClock(type,1,true));
                multi.add(multiClock(type,5,true));
                multi.add(multiClock(type,5,false));
            }
            r.put("multiblock_native_progress",multi);r.put("status","passed");
            cfg.getField("multiplier").setInt(null,5);cfg.getField("pumping").setBoolean(null,true);
            Class<?> runtime=c("cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress");
            Field extracted=runtime.getDeclaredField("EXTRACTION_CLASS"),mined=runtime.getDeclaredField("MINER_CLASS");extracted.setAccessible(true);mined.setAccessible(true);
            ClassValue<?> extraction=(ClassValue<?>)extracted.get(null),miners=(ClassValue<?>)mined.get(null);
            List<Object> policies=new ArrayList<Object>();Set<Class<?>> seen=new HashSet<Class<?>>();
            for(Object m:(Object[])get(c("gregtech.api.GregTechAPI"),"METATILEENTITIES")) {
                if(m==null||!seen.add(m.getClass())||!Boolean.TRUE.equals(extraction.get(m.getClass())))continue;
                boolean pump=c("gregtech.common.tileentities.machines.basic.MTEPump").isInstance(m)||c("gregtech.common.tileentities.machines.multi.MTEOilDrillBase").isInstance(m);
                int expected=pump||(Boolean.TRUE.equals(miners.get(m.getClass()))&&!c("gregtech.common.tileentities.machines.basic.MTEMiner").isInstance(m))?5:1;
                int actual=(Integer)runtime.getMethod("increment",Object.class,int.class,int.class).invoke(null,m,10,200);
                check(actual==expected,"Mining/extraction scope changed unexpectedly "+m.getClass());
                Map<String,Object> row=new LinkedHashMap<String,Object>();row.put("class",m.getClass().getName());row.put("step",actual);policies.add(row);
            }
            r.put("registered_extraction_policy_with_mining_enabled",policies);
        } catch(Throwable e) {e.printStackTrace();r.put("status","failed");r.put("error",e.toString());}
        finally {for(Map.Entry<Field,Object> entry:saved.entrySet())entry.getKey().set(null,entry.getValue());}
        Object gson=c("com.google.gson.Gson").newInstance();Files.write(Paths.get(output),((String)gson.getClass().getMethod("toJson",Object.class).invoke(gson,r)).getBytes("UTF-8"));
    }
}
