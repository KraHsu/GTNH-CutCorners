package runtimechecks;
import java.lang.reflect.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.security.MessageDigest;
public class NativeChecks {
 static Field fld(Class<?> c,String... names)throws Exception {for(Class<?> k=c;k!=null;k=k.getSuperclass())for(String n:names)try{Field f=k.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException ignore){}throw new NoSuchFieldException(c+Arrays.toString(names));}
 static Object get(Object o,String... names)throws Exception{return fld(o instanceof Class?(Class<?>)o:o.getClass(),names).get(o instanceof Class?null:o);}
 static Method meth(Class<?> c,String a,String b,Class<?>... types)throws Exception{try{return c.getMethod(a,types);}catch(NoSuchMethodException e){return c.getMethod(b,types);}}
 static Object call(Object o,String a,String b,Class<?>[] types,Object... args)throws Exception{return meth(o instanceof Class?(Class<?>)o:o.getClass(),a,b,types).invoke(o instanceof Class?null:o,args);}
 static Class<?> c(String name)throws Exception{return Class.forName(name);}
 static String clean(Object value){return String.valueOf(value).replace('\t',' ').replace('\n',' ');}
 static Object registry; static Method itemGet,damage,nameGet;static Field count,tag;
 static String items(Object array)throws Exception{
  if(array==null)return "null";StringBuilder s=new StringBuilder();
  for(Object stack:(Object[])array){if(stack==null){s.append("null;");continue;}Object item=itemGet.invoke(stack);s.append(nameGet.invoke(registry,item)).append(':').append(damage.invoke(stack)).append('x').append(count.get(stack)).append('[').append(tag.get(stack)).append("]; ");}return s.toString();
 }
 static String fluids(Object array)throws Exception{
  if(array==null)return "null";StringBuilder s=new StringBuilder();for(Object stack:(Object[])array){if(stack==null){s.append("null;");continue;}Object fluid=stack.getClass().getMethod("getFluid").invoke(stack);s.append(fluid.getClass().getMethod("getName").invoke(fluid)).append('x').append(get(stack,"amount")).append(';');}return s.toString();
 }
 static String hash(String text)throws Exception{byte[] digest=MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8"));StringBuilder s=new StringBuilder();for(byte b:digest){s.append("0123456789abcdef".charAt((b&255)>>>4));s.append("0123456789abcdef".charAt(b&15));}return s.toString();}
 static Map<String,Object> snapshot()throws Exception{
  Class<?> item=c("net.minecraft.item.Item"), stack=c("net.minecraft.item.ItemStack");registry=get(item,"itemRegistry","field_150901_e");
  itemGet=meth(stack,"getItem","func_77973_b");damage=meth(stack,"getItemDamage","func_77960_j");count=fld(stack,"stackSize","field_77994_a");tag=fld(stack,"stackTagCompound","field_77990_d");nameGet=meth(registry.getClass(),"getNameForObject","func_148750_c",Object.class);
  Map<String,Object> result=new LinkedHashMap<String,Object>();Map<String,Integer> counts=new TreeMap<String,Integer>();
  Map<?,?> maps=(Map<?,?>)get(c("gregtech.api.recipe.RecipeMap"),"ALL_RECIPE_MAPS");int total=0;
  try(PrintWriter out=new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream("recipes.tsv.gz")),"UTF-8"))){
   out.println("map\tkey\tduration\teut\tinputs\toutputs");
   for(Object map:maps.values()){
    String name=(String)get(map,"unlocalizedName");Collection<?> recipes=(Collection<?>)map.getClass().getMethod("getAllRecipes").invoke(map);counts.put(name,recipes.size());
    for(Object recipe:recipes){String in=items(get(recipe,"mInputs")),output=items(get(recipe,"mOutputs"));String key=in+"|"+output+"|"+fluids(get(recipe,"mFluidInputs"))+"|"+fluids(get(recipe,"mFluidOutputs"))+"|"+get(recipe,"mSpecialValue")+"|"+Arrays.toString((int[])get(recipe,"mOutputChances"));
     out.println(clean(name)+"\t"+hash(key)+"\t"+get(recipe,"mDuration")+"\t"+get(recipe,"mEUt")+"\t"+clean(in)+"\t"+clean(output));total++;
    }
   }
  }
  result.put("recipe_maps",counts);result.put("recipe_count",total);result.put("java",System.getProperty("java.version"));
  return result;
 }
 static Map<String,Object> furnace()throws Exception{
  if(itemGet==null)snapshot();
  Map<String,Object> result=new LinkedHashMap<String,Object>();
  Object world=c("net.minecraftforge.common.DimensionManager").getMethod("getWorld",int.class).invoke(null,0);
  Class<?> block=c("net.minecraft.block.Block"),stack=c("net.minecraft.item.ItemStack"),item=c("net.minecraft.item.Item");
  Object blockreg=get(block,"blockRegistry","field_149771_c");Object furnace=call(blockreg,"getObject","func_82594_a",new Class<?>[]{String.class},"minecraft:furnace");
  call(world,"setBlock","func_147465_d",new Class<?>[]{int.class,int.class,int.class,block,int.class,int.class},0,250,0,furnace,0,3);
  Object tile=call(world,"getTileEntity","func_147438_o",new Class<?>[]{int.class,int.class,int.class},0,250,0);
  Object recipes=call(c("net.minecraft.item.crafting.FurnaceRecipes"),"smelting","func_77602_a",new Class<?>[]{});
  Map<?,?> smelting=(Map<?,?>)call(recipes,"getSmeltingList","func_77599_b",new Class<?>[]{});
  Object input=null;List<String> choices=new ArrayList<String>();
  for(Object candidate:smelting.keySet()){
   if(candidate==null)continue;
   String desc=items(new Object[]{candidate});
   if(choices.size()<20)choices.add(desc);
   if(input==null)input=candidate;
   if(desc.startsWith("minecraft:potato:")){input=candidate;break;}
  }
  if(input==null)throw new IllegalStateException("No vanilla furnace input");
  input=call(input,"copy","func_77946_l",new Class<?>[]{});count.setInt(input,1);
  Object coal=call(registry,"getObject","func_82594_a",new Class<?>[]{String.class},"minecraft:coal");
  Constructor<?> ctor=stack.getConstructor(item,int.class,int.class);
  result.put("input",items(new Object[]{input}));result.put("available_smelting_recipes",smelting.size());
  result.put("fuel_ticks",call(tile.getClass(),"getItemBurnTime","func_145952_a",new Class<?>[]{stack},ctor.newInstance(coal,1,0)));
  call(tile,"setInventorySlotContents","func_70299_a",new Class<?>[]{int.class,stack},0,input);
  call(tile,"setInventorySlotContents","func_70299_a",new Class<?>[]{int.class,stack},1,ctor.newInstance(coal,1,0));
  int ticks=0;Object output=null;
  while(ticks<2000){long now=((Number)call(world,"getTotalWorldTime","func_82737_E",new Class<?>[]{})).longValue();Object info=call(world,"getWorldInfo","func_72912_H",new Class<?>[]{});call(info,"incrementTotalWorldTime","func_82572_b",new Class<?>[]{long.class},now+1);call(tile,"updateEntity","func_145845_h",new Class<?>[]{});ticks++;output=call(tile,"getStackInSlot","func_70301_a",new Class<?>[]{int.class},2);if(output!=null)break;}
  result.put("cook_progress",get(tile,"furnaceCookTime","field_145961_j"));result.put("ticks",ticks);result.put("completed",output!=null);result.put("output",output==null?null:items(new Object[]{output}));return result;
 }

 public static void run(String output)throws Exception {
  Map<String,Object> report=new LinkedHashMap<String,Object>();
  try { report.put("network",network());report.putAll(snapshot());Files.move(Paths.get("recipes.tsv.gz"),Paths.get("recipes-before.tsv.gz"),StandardCopyOption.REPLACE_EXISTING);
   report.put("furnace",furnace()); report.put("nativeMachines",machines());report.put("researchStation",research());report.put("diskReload",reload());snapshot();report.put("status","passed"); }
  catch(Throwable error){error.printStackTrace();report.put("status","failed");report.put("error",error.toString());}
  Object gson=c("com.google.gson.Gson").newInstance();String json=(String)gson.getClass().getMethod("toJson",Object.class).invoke(gson,report);Files.write(Paths.get(output),json.getBytes("UTF-8"));
 }
 static Object network()throws Exception {
  Class<?> registryType=c("cpw.mods.fml.common.network.NetworkRegistry"),containerType=c("cpw.mods.fml.common.ModContainer"),holderType=c("cpw.mods.fml.common.network.internal.NetworkModHolder"),sideType=c("cpw.mods.fml.relauncher.Side");
  Object registry=get(registryType,"INSTANCE"),side=get(sideType,"CLIENT");
  Map<?,?> holders=(Map<?,?>)registryType.getMethod("registry").invoke(registry);
  Map<String,String> client=new TreeMap<String,String>();Map<String,Object> byId=new HashMap<String,Object>();
  for(Map.Entry<?,?> entry:holders.entrySet()){
   String id=(String)containerType.getMethod("getModId").invoke(entry.getKey());String version=(String)containerType.getMethod("getVersion").invoke(entry.getKey());
   client.put(id,version);byId.put(id,entry.getValue());
  }
  boolean noDependency=!client.containsKey("conditional_mixin");
  client.remove("CutCorners");client.remove("conditional_mixin");client.remove("runtimecompatchecks");
  Method checkRemote=holderType.getMethod("check",Map.class,sideType);
  List<String> rejected=new ArrayList<String>();
  for(Map.Entry<String,Object> entry:byId.entrySet())if(!(Boolean)checkRemote.invoke(entry.getValue(),client,side))rejected.add(entry.getKey());
  boolean cutCornersAccepted=(Boolean)checkRemote.invoke(byId.get("CutCorners"),client,side);
  Map<String,String> missingRequired=new TreeMap<String,String>(client);missingRequired.remove("gregtech");
  boolean requiredRejected=!(Boolean)checkRemote.invoke(byId.get("gregtech"),missingRequired,side);
  Map<String,Object> result=new LinkedHashMap<String,Object>();result.put("conditional_mixin_absent_from_server",noDependency);result.put("client_omits",Arrays.asList("CutCorners","conditional_mixin","runtimecompatchecks"));result.put("cutcorners_accepts_missing_client_mod",cutCornersAccepted);result.put("all_registered_server_checkers_rejected",rejected);result.put("missing_gregtech_still_rejected",requiredRejected);
  check(noDependency,"External dependency unexpectedly loaded");check(cutCornersAccepted && rejected.isEmpty(),"Clean client rejected by "+rejected);check(requiredRejected,"Required GregTech network check bypassed");
  return result;
 }

 static void set(Object target,String field,Object value)throws Exception{fld(target.getClass(),field).set(target,value);}
 static Method declared(Class<?> type,String name,Class<?>...args)throws Exception{
  for(Class<?> k=type;k!=null;k=k.getSuperclass())try{Method m=k.getDeclaredMethod(name,args);m.setAccessible(true);return m;}catch(NoSuchMethodException ignored){}
  throw new NoSuchMethodException(type.getName()+"."+name);
 }
 static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
 static Object prototype(String text,int tier)throws Exception{
  Object[] all=(Object[])get(c("gregtech.api.GregTechAPI"),"METATILEENTITIES");
  for(Object value:all){if(value==null)continue;String name=(String)c("gregtech.api.interfaces.metatileentity.IMetaTileEntity").getMethod("getMetaName").invoke(value);
   if(!(name+" "+value.getClass().getName()).toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT)))continue;
   if(text.equals("macerator") && value.getClass().getName().contains("Steam"))continue;
   if(tier>=0){try{if(((Number)get(value,"mTier")).intValue()!=tier)continue;}catch(NoSuchFieldException ignored){continue;}}
   return value;
  }
  throw new IllegalStateException("Missing registered prototype: "+text+" tier "+tier);
 }
 static class Base implements InvocationHandler {
  Object tile,proxy,world;long energy=1000000,spent;boolean allowed=false,active=false;int added=0;
  Set<String> calls=new TreeSet<String>();
  Base()throws Exception{
   world=c("net.minecraftforge.common.DimensionManager").getMethod("getWorld",int.class).invoke(null,0);
   Class<?> iface=c("gregtech.api.interfaces.tileentity.IGregTechTileEntity");
   proxy=Proxy.newProxyInstance(iface.getClassLoader(),new Class<?>[]{iface},this);
  }
  public Object invoke(Object unused,Method method,Object[] args)throws Throwable{
   String n=method.getName();calls.add(n);
   if(n.equals("toString"))return "IsolatedBase";
   if(n.equals("hashCode"))return System.identityHashCode(proxy);
   if(n.equals("equals"))return proxy==args[0];
   if(n.equals("getMetaTileEntity"))return tile;
   if(n.equals("setMetaTileEntity")){tile=args[0];return null;}
   if(n.equals("getWorld"))return world;
   if(n.equals("getOwnerUuid"))return UUID.fromString("00000000-0000-0000-0000-000000000001");
   if(n.equals("getOwnerName"))return "RuntimeChecks";
   if(n.equals("getXCoord")||n.equals("getZCoord"))return 0;
   if(n.equals("getYCoord"))return (short)250;
   if(n.equals("getOffsetX")||n.equals("getOffsetZ"))return 0;
   if(n.equals("getOffsetY"))return (short)250;
   if(n.equals("isServerSide"))return true;
   if(n.equals("isClientSide")||n.equals("isDead")||n.equals("isInvalid"))return false;
   if(n.equals("isAllowedToWork"))return allowed;
   if(n.equals("isActive"))return active;
   if(n.equals("setActive")){active=(Boolean)args[0];return null;}
   if(n.equals("getFrontFacing"))return get(c("net.minecraftforge.common.util.ForgeDirection"),"NORTH");
   if(n.equals("getBackFacing"))return get(c("net.minecraftforge.common.util.ForgeDirection"),"SOUTH");
   if(n.equals("getStoredEU")||n.equals("getUniversalEnergyStored")||n.equals("getStoredEUuncapped"))return energy;
   if(n.equals("getInputVoltage"))return 32L;
   if(n.equals("getEUCapacity")||n.equals("getUniversalEnergyCapacity"))return 10000000L;
   if(n.equals("getTimer"))return 200L;
   if(n.equals("decreaseStoredEnergyUnits")||n.equals("decreaseStoredEU")){
    long cost=((Number)args[0]).longValue();if(energy<cost)return false;energy-=cost;spent+=cost;return true;
   }
   if(n.equals("getSizeInventory"))return ((Object[])get(tile,"mInventory")).length;
   if(n.equals("getStackInSlot"))return ((Object[])get(tile,"mInventory"))[((Number)args[0]).intValue()];
   if(n.equals("setInventorySlotContents")){((Object[])get(tile,"mInventory"))[((Number)args[0]).intValue()]=args[1];return null;}
   if(n.equals("addStackToSlot")){Object stack=args[1];added+=((Number)count.get(stack)).intValue();return true;}
   if(n.equals("getInventoryStackLimit"))return 64;
   Class<?> r=method.getReturnType();
   if(r==boolean.class)return false;if(r==int.class)return 0;if(r==short.class)return (short)0;if(r==byte.class)return (byte)0;
   if(r==long.class)return 0L;if(r==double.class)return 0D;if(r==float.class)return 0F;if(r==char.class)return (char)0;
   if(r==String.class)return "";return null;
  }
 }
 static Object cloneMachine(Object prototype,Base base)throws Exception{
  Object machine=c("gregtech.api.interfaces.metatileentity.IMetaTileEntity").getMethod("newMetaEntity",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity")).invoke(prototype,base.proxy);
  base.tile=machine;machine.getClass().getMethod("setBaseMetaTileEntity",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity")).invoke(machine,base.proxy);return machine;
 }
 static Object outputStack()throws Exception{
  Object item=call(registry,"getObject","func_82594_a",new Class<?>[]{String.class},"minecraft:baked_potato");
  return c("net.minecraft.item.ItemStack").getConstructor(c("net.minecraft.item.Item"),int.class,int.class).newInstance(item,1,0);
 }
 static void stage(Object machine,int duration,boolean multi)throws Exception{
  set(machine,"mProgresstime",0);set(machine,"mMaxProgresstime",duration);set(machine,"mEUt",multi?-2:2);
  if(multi){
   for(String flag:new String[]{"mWrench","mScrewdriver","mSoftMallet","mHardHammer","mSolderingTool","mCrowbar"})set(machine,flag,true);
   set(machine,"mEfficiency",10000);set(machine,"hasMaintenanceChecks",false);
   try{set(machine,"lEUt",-2L);}catch(NoSuchFieldException ignored){}
   Object outputs=Array.newInstance(c("net.minecraft.item.ItemStack"),1);Array.set(outputs,0,outputStack());set(machine,"mOutputItems",outputs);
  }else{Object[] outputs=(Object[])get(machine,"mOutputItems");Arrays.fill(outputs,null);outputs[0]=outputStack();}
 }
 static void tick(Object machine,Base base,boolean multi,long tick)throws Exception{
  Method m=declared(c(multi?"gregtech.api.metatileentity.implementations.MTEMultiBlockBase":"gregtech.api.metatileentity.implementations.MTEBasicMachine"),multi?"runMachine":"onPostTick",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity"),long.class);
  try{m.invoke(machine,base.proxy,tick);}catch(InvocationTargetException error){throw new RuntimeException("Native tick failed; methods="+base.calls,error.getCause());}
 }
 static Map<String,Object> machineCase(String name,Object proto,int duration,int speed,boolean enabled,boolean power,boolean multi)throws Exception{
  Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");cfg.getField("multiplier").setInt(null,speed);cfg.getField("enabled").setBoolean(null,enabled);
  Base base=new Base();base.energy=power?1000000:0;Object machine=cloneMachine(proto,base);Base energy=null,output=null;
  if(multi){
   energy=new Base();energy.energy=base.energy;Object hatch=cloneMachine(prototype("hatch.energy",1),energy);((Collection)get(machine,"mEnergyHatches")).add(hatch);
   output=new Base();Object bus=cloneMachine(prototype("output_bus",1),output);((Collection)get(machine,"mOutputBusses")).add(bus);
  }
  List<Integer> tickCounts=new ArrayList<Integer>();List<Integer> outputCounts=new ArrayList<Integer>();
  for(int batch=0;batch<(power?3:1);batch++){
   stage(machine,duration,multi);int ticks=0;
   while(((Number)get(machine,"mMaxProgresstime")).intValue()>0 && ticks<duration+200){tick(machine,base,multi,201+ticks);ticks++;if(!power)break;}
   tickCounts.add(ticks);
   int produced=base.added;
   if(multi){produced=0;for(Object stack:(Object[])get(output.tile,"mInventory"))if(stack!=null)produced+=((Number)count.get(stack)).intValue();}
   outputCounts.add(produced);
   if(power){int expected=(duration+(enabled?speed:1)-1)/(enabled?speed:1);check(ticks==expected,name+" ticks="+ticks+" expected="+expected);check(produced==batch+1,name+" duplicated/missing output "+produced);}
   else check(produced==0,name+" produced while unpowered");
  }
  Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("name",name);r.put("class",machine.getClass().getName());r.put("duration",duration);r.put("speed",speed);r.put("enabled",enabled);r.put("powered",power);r.put("ticks_per_batch",tickCounts);r.put("outputs_after_batches",outputCounts);r.put("energy_spent",multi?energy.spent:base.spent);r.put("final_progress",get(machine,"mProgresstime"));return r;
 }
 static Object research()throws Exception{
  Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");int prior=cfg.getField("multiplier").getInt(null);
  List<Map<String,Object>> results=new ArrayList<Map<String,Object>>();
  try{for(int speed:new int[]{1,5,10,1000}){
   cfg.getField("multiplier").setInt(null,speed);Base base=new Base();
   Object controller=c("gregtech.api.metatileentity.BaseMetaTileEntity").newInstance();
   fld(controller.getClass(),"worldObj","field_145850_b").set(controller,base.world);
   Object machine=c("gregtech.api.interfaces.metatileentity.IMetaTileEntity").getMethod("newMetaEntity",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity")).invoke(prototype("MTEResearchStation",-1),controller);
   c("gregtech.api.metatileentity.MetaTileEntity").getMethod("setBaseMetaTileEntity",c("gregtech.api.interfaces.tileentity.IGregTechTileEntity")).invoke(machine,controller);
   set(controller,"mStoredEnergy",1000000L);set(controller,"mOwnerUuid",UUID.fromString("00000000-0000-0000-0000-000000000001"));set(controller,"mOwnerName","RuntimeChecks");
   Base energy=new Base();Object hatch=cloneMachine(prototype("hatch.energy",1),energy);((Collection)get(machine,"mEnergyHatches")).add(hatch);
   Base holderBase=new Base();Object holder=cloneMachine(prototype("MTEHatchObjectHolder",-1),holderBase);
   ((Object[])get(holder,"mInventory"))[0]=outputStack();((Collection)get(machine,"eHolders")).add(holder);
   stage(machine,20,true);set(machine,"maxEUinputMin",32L);set(machine,"eMaxAmpereFlow",1L);set(machine,"eAmpereFlow",1L);set(machine,"eAvailableData",10L);set(machine,"eRequiredData",1L);set(machine,"computationRemaining",500L);set(machine,"computationRequired",500L);
   Method on=declared(c("tectech.thing.metaTileEntity.multi.MTEResearchStation"),"onRunningTick",c("net.minecraft.item.ItemStack"));
   Method increment=declared(c("gregtech.api.metatileentity.implementations.MTEMultiBlockBase"),"incrementProgressTime");
   int ticks=0;while(((Number)get(machine,"computationRemaining")).longValue()>0 && ticks<100){
    boolean ok=(Boolean)on.invoke(machine,new Object[]{null});check(ok,"research power gate failed");increment.invoke(machine);ticks++;
    check(((Number)get(machine,"mProgresstime")).intValue()==2,"research cosmetic clock accelerated");
   }
   check(ticks==Math.max(1,(50+speed-1)/speed),"research did not consume computation at configured rate");
   Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("speed",speed);r.put("computation",500);r.put("native_step",10);r.put("powered_work_ticks",ticks);r.put("remaining",get(machine,"computationRemaining"));r.put("energy_spent",1000000L-((Number)get(controller,"mStoredEnergy")).longValue());results.add(r);
  }return results;}finally{cfg.getField("multiplier").setInt(null,prior);}
 }
 static Object reload()throws Exception{
  Path path=Paths.get("config/CutCornersRuntime.cfg");byte[] original=Files.readAllBytes(path);Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");
  try{
   String content=new String(original,"UTF-8").replace("I:multiplier=10","I:multiplier=3");Files.write(path,content.getBytes("UTF-8"));c("cn.elytra.gtnh.cutcorners.api.CutCornersAPI").getMethod("reloadConfig").invoke(null);
   check(cfg.getField("multiplier").getInt(null)==3,"reload did not reread on-disk multiplier");
   return machineCase("disk reload 3x",prototype("macerator",1),200,3,true,true,false);
  }finally{Files.write(path,original);c("cn.elytra.gtnh.cutcorners.api.CutCornersAPI").getMethod("reloadConfig").invoke(null);}
 }

 static Object machines()throws Exception{
  Class<?> cfg=c("cn.elytra.gtnh.cutcorners.config.RuntimeConfig");int prior=cfg.getField("multiplier").getInt(null);boolean enabled=cfg.getField("enabled").getBoolean(null);
  List<Map<String,Object>> results=new ArrayList<Map<String,Object>>();
  try{
   Object basic=prototype("macerator",1);
   for(int speed:new int[]{1,5,10})results.add(machineCase("LV macerator",basic,200,speed,true,true,false));
   results.add(machineCase("LV short recipe",basic,1,10,true,true,false));
   results.add(machineCase("LV fractional tick boundary",basic,198,10,true,true,false));
   results.add(machineCase("LV disabled",basic,200,10,false,true,false));
   results.add(machineCase("LV power failure",basic,200,10,true,false,false));
   for(String name:new String[]{"industrialcentrifuge","gregtech.common.tileentities.machines.multi.MTEIndustrialCentrifuge","alloyblastsmelter","industrialchisel"}){
    Object proto=prototype(name,-1);results.add(machineCase(name,proto,200,1,true,true,true));results.add(machineCase(name,proto,200,5,true,true,true));results.add(machineCase(name,proto,200,10,true,true,true));results.add(machineCase(name+" power failure",proto,200,10,true,false,true));
   }
   return results;
  }finally{cfg.getField("multiplier").setInt(null,prior);cfg.getField("enabled").setBoolean(null,enabled);}
 }

}
