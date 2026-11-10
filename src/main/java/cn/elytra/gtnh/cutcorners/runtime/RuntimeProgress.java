package cn.elytra.gtnh.cutcorners.runtime;

import cn.elytra.gtnh.cutcorners.config.RuntimeConfig;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.common.tileentities.machines.basic.MTEMiner;
import gregtech.common.tileentities.machines.multi.purification.MTEPurificationPlant;
import gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitBase;
import tectech.thing.metaTileEntity.multi.MTEResearchStation;

/** Called only where the original machine has already accepted a productive tick. */
public final class RuntimeProgress {
    private RuntimeProgress() {}
    public static int speed() { return RuntimeConfig.enabled ? Math.max(1, Math.min(1000, RuntimeConfig.multiplier)) : 1; }

    /** Class-name policy avoids making optional extraction mods required dependencies. */
    private static final java.util.Set<String> EXTRACTION = new java.util.HashSet<String>(java.util.Arrays.asList(
        "gregtech.common.tileentities.machines.basic.MTEMiner",
        "gregtech.common.tileentities.machines.basic.MTEPump",
        "gregtech.common.tileentities.machines.basic.MTEIndustrialApiary",
        "gregtech.common.tileentities.machines.multi.MTEDrillerBase",
        "gregtech.common.tileentities.machines.multi.steam.MTESteamWaterPump",
        "bwcrossmod.galacticgreg.MTEVoidMinerBase",
        "com.sinthoras.hydroenergy.blocks.HEHydroPumpTileEntity",
        "gtnhintergalactic.tile.multi.elevatormodules.TileEntityModuleMiner",
        "gtnhintergalactic.tile.multi.elevatormodules.TileEntityModulePump",
        "kubatech.tileentity.gregtech.multiblock.MTEMegaIndustrialApiary",
        "com.gtnewhorizon.cropsnh.tileentity.multi.MTEIndustrialFarm",
        "gregtech.common.tileentities.machines.multi.MTEIndustrialFishingPond"));
    private static final java.util.Set<String> HEAT_EXCHANGERS = new java.util.HashSet<String>(java.util.Arrays.asList(
        "gregtech.common.tileentities.machines.multi.MTEHeatExchanger",
        "goodgenerator.blocks.tileEntity.MTEExtremeHeatExchanger",
        "gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.advanced.MTEAdvHeatExchanger"));
    private static final java.util.Set<String> MINERS = new java.util.HashSet<String>(java.util.Arrays.asList(
        "gregtech.common.tileentities.machines.basic.MTEMiner",
        "gregtech.common.tileentities.machines.multi.MTEOreDrillingPlantBase",
        "bwcrossmod.galacticgreg.MTEVoidMinerBase",
        "gtnhintergalactic.tile.multi.elevatormodules.TileEntityModuleMiner"));
    private static final java.util.Set<String> PUMPS = new java.util.HashSet<String>(java.util.Arrays.asList(
        "gregtech.common.tileentities.machines.basic.MTEPump",
        "gregtech.common.tileentities.machines.multi.MTEOilDrillBase"));
    private static final ClassValue<Boolean> PUMP_CLASS = new ClassValue<Boolean>() {
        @Override protected Boolean computeValue(Class<?> type) { return matches(type, PUMPS); }
    };
    private static final ClassValue<Boolean> MINER_CLASS = new ClassValue<Boolean>() {
        @Override protected Boolean computeValue(Class<?> type) { return matches(type, MINERS); }
    };
    private static final ClassValue<Boolean> EXTRACTION_CLASS = new ClassValue<Boolean>() {
        @Override protected Boolean computeValue(Class<?> type) { return matches(type, EXTRACTION); }
    };
    private static final ClassValue<Boolean> HEAT_CLASS = new ClassValue<Boolean>() {
        @Override protected Boolean computeValue(Class<?> type) { return matches(type, HEAT_EXCHANGERS); }
    };
    private static boolean matches(Class<?> type, java.util.Set<String> names) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) if (names.contains(c.getName())) return true;
        return false;
    }
    public static int processingSpeed() { return RuntimeConfig.gregTech ? speed() : 1; }
    private static boolean blacklisted(RecipeMap<?> map) {
        if (map != null && RuntimeConfig.recipeMapBlacklist != null)
            for (String excluded : RuntimeConfig.recipeMapBlacklist) if (map.unlocalizedName.equals(excluded)) return true;
        return false;
    }
    /** A connected purification plant must use one clock, including blacklist exclusions. */
    public static int purificationSpeed(Object machine) {
        if (processingSpeed() <= 1) return 1;
        MTEPurificationPlant plant = machine instanceof MTEPurificationPlant ? (MTEPurificationPlant) machine : null;
        if (machine instanceof MTEPurificationUnitBase) {
            MTEPurificationUnitBase<?> unit = (MTEPurificationUnitBase<?>) machine;
            if (blacklisted(unit.getRecipeMap())) return 1;
            plant = unit.getController();
        }
        if (plant != null) {
            for (gregtech.common.tileentities.machines.multi.purification.LinkedPurificationUnit link : plant.getLinkedUnits())
                if (blacklisted(link.metaTileEntity().getRecipeMap())) return 1;
        }
        return processingSpeed();
    }
    public static int otherSpeed() { return RuntimeConfig.otherMachines ? speed() : 1; }
    public static int miningSpeed() { return RuntimeConfig.mining ? processingSpeed() : 1; }
    public static int pumpingSpeed() { return RuntimeConfig.pumping ? processingSpeed() : 1; }
    public static int miningDuration(int original) { return ProgressMath.duration(original, miningSpeed()); }
    /** The native delegate divides this value by five before using it as a modulo divisor. */
    public static int miningRetractionClock(Object owner, int original) {
        if (!(owner instanceof MTEMiner) || miningSpeed() <= 1) return original;
        return ProgressMath.duration(Math.max(1, original / 5), miningSpeed()) * 5;
    }

    public static int increment(Object machine, int progress, int maximum) {
        if (!RuntimeConfig.gregTech || !RuntimeConfig.enabled) return 1;
        // Basic miners perform extraction at duration-1, so they use a shorter
        // native interval and MUST retain the original +1 counter step.
        if (machine instanceof MTEMiner) return 1;
        if (MINER_CLASS.get(machine.getClass())) {
            if (!RuntimeConfig.mining) return 1;
        } else if (PUMP_CLASS.get(machine.getClass())) {
            if (!RuntimeConfig.pumping) return 1;
        } else if (EXTRACTION_CLASS.get(machine.getClass())) return 1;
        // Purification's controller and native unit phases share a separate
        // accelerated clock; do not also apply the common counter adapter.
        // Research completion is governed by computation, not its progress bar.
        if (machine instanceof MTEPurificationPlant || machine instanceof MTEPurificationUnitBase
            || machine instanceof MTEResearchStation) return 1;
        RecipeMap<?> map = null;
        if (machine instanceof MTEBasicMachine) {
            map = ((MTEBasicMachine) machine).getRecipeMap();
        } else if (machine instanceof MTEMultiBlockBase) {
            MTEMultiBlockBase multi = (MTEMultiBlockBase) machine;
            // Positive EU/t means generation in the multiblock processing loop.
            if (multi.mEUt > 0 && !HEAT_CLASS.get(machine.getClass())) return 1;
            if (machine instanceof MTEExtendedPowerMultiBlockBase && ((MTEExtendedPowerMultiBlockBase<?>) machine).lEUt > 0 && !HEAT_CLASS.get(machine.getClass())) return 1;
            map = multi.getRecipeMap();
        } else if (machine instanceof RecipeMapWorkable) {
            map = ((RecipeMapWorkable) machine).getRecipeMap();
        }
        if (blacklisted(map)) return 1;
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
