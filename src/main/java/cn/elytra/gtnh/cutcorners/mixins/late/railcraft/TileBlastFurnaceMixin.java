package cn.elytra.gtnh.cutcorners.mixins.late.railcraft;

import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mods.railcraft.common.blocks.machine.alpha.TileBlastFurnace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = TileBlastFurnace.class, remap = false)
public class TileBlastFurnaceMixin {
    @ModifyExpressionValue(method = {"updateEntity", "func_145845_h"}, at = @At(value = "INVOKE",
        target = "Lmods/railcraft/api/crafting/IBlastFurnaceRecipe;getCookTime()I"), require = 1)
    private int cc$cookDuration(int original) {
        return ProgressMath.duration(original, RuntimeProgress.railcraftSpeed());
    }
}
