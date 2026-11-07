package cn.elytra.gtnh.cutcorners.mixins.late.gregtech;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets = "goodgenerator.blocks.tileEntity.base.MTELargeFusionComputer", remap = false)
public abstract class MTELargeFusionComputerMixin extends MTEMultiBlockBase {
    protected MTELargeFusionComputerMixin(String name) { super(name); }
    @ModifyConstant(method = "onPostTick", constant = @Constant(intValue = 1), require = 1, allow = 1, slice = @Slice(
        from = @At(value = "FIELD", target = "Lgoodgenerator/blocks/tileEntity/base/MTELargeFusionComputer;mProgresstime:I", opcode = Opcodes.GETFIELD, ordinal = 0),
        to = @At(value = "FIELD", target = "Lgoodgenerator/blocks/tileEntity/base/MTELargeFusionComputer;mProgresstime:I", opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int cc$productiveIncrement(int original) { return RuntimeProgress.increment(this, mProgresstime, mMaxProgresstime); }
}
