package runtimechecks;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Read-only inventory of actually registered machines in an isolated pack. */
public class CoverageProbe extends NativeChecks {
    public static void run(String output)throws Exception {
        Map<String,Object> report=new LinkedHashMap<String,Object>();
        Object[] machines=(Object[])get(c("gregtech.api.GregTechAPI"),"METATILEENTITIES");
        Class<?> meta=c("gregtech.api.interfaces.metatileentity.IMetaTileEntity");
        Class<?> workable=c("gregtech.api.interfaces.tileentity.RecipeMapWorkable");
        List<Map<String,Object>> rows=new ArrayList<Map<String,Object>>();
        for(int id=0;id<machines.length;id++) {
            Object machine=machines[id];if(machine==null)continue;
            Map<String,Object> row=new LinkedHashMap<String,Object>();row.put("id",id);row.put("class",machine.getClass().getName());
            row.put("name",meta.getMethod("getMetaName").invoke(machine));
            try{row.put("display",meta.getMethod("getLocalName").invoke(machine));}catch(Throwable ignored){}
            try{Class<?> tiered=c("gregtech.api.metatileentity.implementations.MTETieredMachineBlock");if(tiered.isInstance(machine))row.put("tier",tiered.getField("mTier").get(machine));}catch(Throwable error){row.put("tierError",error.getClass().getSimpleName());}
            List<String> maps=new ArrayList<String>();
            if(workable.isInstance(machine)) {
                try {
                    Collection<?> available=(Collection<?>)workable.getMethod("getAvailableRecipeMaps").invoke(machine);
                    if(available!=null)for(Object map:available)if(map!=null)maps.add((String)get(map,"unlocalizedName"));
                    Object active=workable.getMethod("getRecipeMap").invoke(machine);
                    if(active!=null)row.put("activeMap",get(active,"unlocalizedName"));
                }catch(Throwable error){row.put("mapError",error instanceof InvocationTargetException?((InvocationTargetException)error).getCause().toString():error.toString());}
            }
            row.put("maps",maps);rows.add(row);
        }
        report.put("machines",rows);
        List<Map<String,String>> tiles=new ArrayList<Map<String,String>>();
        Map<?,?> registered=(Map<?,?>)get(c("net.minecraft.tileentity.TileEntity"),"nameToClassMap","field_145855_i");
        for(Map.Entry<?,?> e:registered.entrySet()) {
            if(!(e.getValue() instanceof Class<?>))continue;
            Map<String,String> row=new LinkedHashMap<String,String>();row.put("id",String.valueOf(e.getKey()));row.put("class",((Class<?>)e.getValue()).getName());tiles.add(row);
        }
        report.put("tiles",tiles);report.put("java",System.getProperty("java.version"));report.put("status","passed");
        Object gson=c("com.google.gson.Gson").newInstance();String json=(String)gson.getClass().getMethod("toJson",Object.class).invoke(gson,report);
        Files.write(Paths.get(output),json.getBytes("UTF-8"));System.out.println("COVERAGE_INVENTORY_DONE machines="+rows.size()+" tiles="+tiles.size());
    }
}
