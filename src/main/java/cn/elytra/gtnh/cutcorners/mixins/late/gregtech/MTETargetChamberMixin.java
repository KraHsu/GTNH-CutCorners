package cn.elytra.gtnh.cutcorners.mixins.late.gregtech;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets = "gtnhlanth.common.tileentity.MTETargetChamber", remap = false)
public abstract class MTETargetChamberMixin extends MTEMultiBlockBase {
    protected MTETargetChamberMixin(String name) { super(name); }
    @ModifyConstant(method = "incrementProgressTime", constant = @Constant(intValue = 1), require = 2, allow = 2)
    private int cc$productiveIncrement(int original) { return RuntimeProgress.increment(this, mProgresstime, mMaxProgresstime); }
}
