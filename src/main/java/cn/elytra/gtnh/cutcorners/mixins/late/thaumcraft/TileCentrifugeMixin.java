package cn.elytra.gtnh.cutcorners.mixins.late.thaumcraft;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets = "thaumcraft.common.tiles.TileCentrifuge", remap = false)
public class TileCentrifugeMixin {
    @Shadow int process;
    @ModifyConstant(method = {"updateEntity", "func_145845_h"}, constant = @Constant(intValue = 1), require = 1, allow = 1,
        slice = @Slice(from = @At(value = "FIELD", target = "Lthaumcraft/common/tiles/TileCentrifuge;process:I", opcode = Opcodes.GETFIELD, ordinal = 1),
                       to = @At(value = "FIELD", target = "Lthaumcraft/common/tiles/TileCentrifuge;process:I", opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int cc$separationTime(int original) { return Math.min(process, RuntimeProgress.otherSpeed()); }
}
