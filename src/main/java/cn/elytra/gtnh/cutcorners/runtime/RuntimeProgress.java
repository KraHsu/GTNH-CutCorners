package cn.elytra.gtnh.cutcorners.runtime;

import cn.elytra.gtnh.cutcorners.config.RuntimeConfig;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.common.tileentities.machines.multi.purification.MTEPurificationPlant;
import gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitBase;
import tectech.thing.metaTileEntity.multi.MTEResearchStation;

/** Called only where the original machine has already accepted a productive tick. */
public final class RuntimeProgress {
    private RuntimeProgress() {}
    public static int speed() { return RuntimeConfig.enabled ? Math.max(1, Math.min(1000, RuntimeConfig.multiplier)) : 1; }

    public static int increment(Object machine, int progress, int maximum) {
        if (!RuntimeConfig.gregTech || !RuntimeConfig.enabled) return 1;
        // Purification uses a shared cycle; its controller's cycle duration is
        // adjusted instead, so units and controller must keep the same clock.
        // Research completion is governed by computation, not its progress bar.
        if (machine instanceof MTEPurificationPlant || machine instanceof MTEPurificationUnitBase
            || machine instanceof MTEResearchStation) return 1;
        RecipeMap<?> map = null;
        if (machine instanceof MTEBasicMachine) {
            map = ((MTEBasicMachine) machine).getRecipeMap();
        } else if (machine instanceof MTEMultiBlockBase) {
            MTEMultiBlockBase multi = (MTEMultiBlockBase) machine;
            // Positive EU/t means generation in the multiblock processing loop.
            if (multi.mEUt > 0) return 1;
            if (machine instanceof MTEExtendedPowerMultiBlockBase && ((MTEExtendedPowerMultiBlockBase<?>) machine).lEUt > 0) return 1;
            map = multi.getRecipeMap();
        }
        if (map != null && RuntimeConfig.recipeMapBlacklist != null) {
            for (String excluded : RuntimeConfig.recipeMapBlacklist) if (map.unlocalizedName.equals(excluded)) return 1;
        }
        return ProgressMath.increment(progress, maximum, speed());
    }

    public static int furnaceDuration(int original) {
        return RuntimeConfig.furnaces ? ProgressMath.duration(original, speed()) : original;
    }
    public static int railcraftSpeed() { return RuntimeConfig.railcraft ? speed() : 1; }
    public static int railcraftInterval(int original) {
        return RuntimeConfig.railcraft ? ProgressMath.duration(original, speed()) : original;
    }
}
