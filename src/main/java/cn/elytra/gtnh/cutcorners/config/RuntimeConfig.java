package cn.elytra.gtnh.cutcorners.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

/** Pure processing acceleration. The upstream cheat options are not used. */
@Config(modid = "CutCornersRuntime")
public final class RuntimeConfig {
    static { ConfigurationManager.registerConfig(RuntimeConfig.class); }
    private RuntimeConfig() {}
    public static void init() {}

    @Config.Comment("Enable processing acceleration. World tick rate and recipe definitions are unchanged.")
    @Config.DefaultBoolean(true)
    @Config.Reloadable("runtime")
    public static boolean enabled;

    @Config.Comment("Processing progress per successful powered tick. Duration rounds up; minimum one tick.")
    @Config.DefaultInt(10)
    @Config.RangeInt(min = 1, max = 1000)
    @Config.Reloadable("runtime")
    public static int multiplier;

    @Config.DefaultBoolean(true)
    @Config.Reloadable("runtime")
    public static boolean gregTech;
    @Config.DefaultBoolean(true)
    @Config.Reloadable("runtime")
    public static boolean furnaces;
    @Config.DefaultBoolean(true)
    @Config.Reloadable("runtime")
    public static boolean railcraft;

    @Config.Comment("Accelerate GT basic/ore-drilling/void/orbital miners.")
    @Config.DefaultBoolean(true)
    @Config.Reloadable("runtime")
    public static boolean mining;

    @Config.Comment("Accelerate GT basic pumps and underground oil/fluid drills. Basic pumps divide action EU by the multiplier with persistent fractional carry; oil drills keep native EU/t.")
    @Config.DefaultBoolean(true)
    @Config.Reloadable("runtime")
    public static boolean pumping;

    @Config.Comment("Recipe-processing adapters for IC2, EnderIO, Forestry and Thaumcraft. Not world production.")
    @Config.DefaultBoolean(true)
    @Config.Reloadable("runtime")
    public static boolean otherMachines;

    @Config.Comment("Exact GT recipe-map names to exclude. Electrical generators are always excluded.")
    @Config.DefaultStringList({"gg.recipe.naquadah_reactor", "gt.recipe.largeboilerfakefuels"})
    @Config.Reloadable("runtime")
    public static String[] recipeMapBlacklist;
}
