package cn.elytra.gtnh.cutcorners.mixins.late.thaumcraft;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
@Mixin(targets = "thaumcraft.common.tiles.TileInfusionMatrix", remap = false)
public class TileInfusionMatrixMixin {
    @ModifyExpressionValue(method = {"updateEntity", "func_145845_h"}, at = @At(value = "FIELD", target = "Lthaumcraft/common/tiles/TileInfusionMatrix;countDelay:I", opcode = Opcodes.GETFIELD), require = 1, allow = 1)
    private int cc$craftInterval(int original) { return ProgressMath.duration(original, RuntimeProgress.otherSpeed()); }
}
