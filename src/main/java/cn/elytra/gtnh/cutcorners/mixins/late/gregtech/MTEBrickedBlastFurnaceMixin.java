package cn.elytra.gtnh.cutcorners.mixins.late.gregtech;

import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import gregtech.common.tileentities.machines.multi.MTEBrickedBlastFurnace;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

/** This primitive machine has its own productive progress loop. */
@Mixin(value = MTEBrickedBlastFurnace.class, remap = false)
public class MTEBrickedBlastFurnaceMixin {
    @Shadow public int mProgresstime;
    @Shadow public int mMaxProgresstime;
    // Change the increment before its duplicated value is used for completion.
    // Leave structure checks, inventory consumption and output logic native.
    @ModifyConstant(method = "onPostTick", constant = @Constant(intValue = 1), require = 1,
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lgregtech/common/tileentities/machines/multi/MTEBrickedBlastFurnace;mProgresstime:I", opcode = Opcodes.GETFIELD, ordinal = 0),
            to = @At(value = "FIELD", target = "Lgregtech/common/tileentities/machines/multi/MTEBrickedBlastFurnace;mProgresstime:I", opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int cc$productiveIncrement(int original) {
        return RuntimeProgress.increment(this, mProgresstime, mMaxProgresstime);
    }
}
