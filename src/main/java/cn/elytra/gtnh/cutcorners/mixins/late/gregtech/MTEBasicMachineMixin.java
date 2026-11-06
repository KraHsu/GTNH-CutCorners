package cn.elytra.gtnh.cutcorners.mixins.late.gregtech;

import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value = MTEBasicMachine.class, remap = false)
public class MTEBasicMachineMixin {
    @Shadow public int mProgresstime;
    @Shadow public int mMaxProgresstime;

    // Replace the increment before both the field write AND its completion
    // comparison. Merely wrapping PUTFIELD leaves the duplicated old value on
    // the operand stack and delays every completed batch by one tick.
    @ModifyConstant(method = "onPostTick", constant = @Constant(intValue = 1), require = 1,
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;setActive(Z)V", ordinal = 0),
            to = @At(value = "FIELD", target = "Lgregtech/api/metatileentity/implementations/MTEBasicMachine;mProgresstime:I", opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int cc$processingIncrement(int original) {
        return RuntimeProgress.increment(this, mProgresstime, mMaxProgresstime);
    }
}
