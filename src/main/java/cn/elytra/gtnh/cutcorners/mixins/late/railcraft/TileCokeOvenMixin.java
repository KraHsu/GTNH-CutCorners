package cn.elytra.gtnh.cutcorners.mixins.late.railcraft;

import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import mods.railcraft.common.blocks.machine.alpha.TileCokeOven;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = TileCokeOven.class, remap = false)
public class TileCokeOvenMixin {
    // The first 50 is the visual cooldown; only change the work interval.
    @ModifyConstant(method = {"updateEntity", "func_145845_h"}, constant = @Constant(intValue = 50, ordinal = 1), require = 1)
    private int cc$workInterval(int original) { return RuntimeProgress.railcraftInterval(original); }

    // Preserve the requested speed for multipliers that do not divide 50.
    @ModifyConstant(method = {"updateEntity", "func_145845_h"}, constant = @Constant(intValue = 50, ordinal = 2), require = 1)
    private int cc$workStep(int original) {
        return RuntimeProgress.railcraftInterval(original) * RuntimeProgress.railcraftSpeed();
    }
}
