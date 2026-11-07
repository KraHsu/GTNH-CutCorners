package cn.elytra.gtnh.cutcorners.strate.impl.conf;

import cn.elytra.gtnh.cutcorners.config.RuntimeConfig;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import cn.elytra.gtnh.cutcorners.strate.ICutCornerStrategy;

/** Recipe mutation callbacks deliberately inherit their no-op implementations. */
public final class RuntimeStrategy implements ICutCornerStrategy {
    public RuntimeStrategy() { RuntimeConfig.init(); }
    @Override public int getMaxFurnaceSmeltingTime(int original) { return RuntimeProgress.furnaceDuration(original); }
    @Override public int getMaxSpecialFurnaceSmeltingTime(int original) { return RuntimeProgress.furnaceDuration(original); }
    @Override public int getThaumcraftFurnaceSmeltingTime(int original) { return RuntimeProgress.furnaceDuration(original); }
    @Override public int getWaterPurificationCycleTime(int original) {
        return original; // Controller and unit phases now use the same accelerated native clock.
    }
}
