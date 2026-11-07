package cn.elytra.gtnh.cutcorners.mixins.late.gregtech;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.tileentities.machines.multi.purification.MTEPurificationPlant;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = MTEPurificationPlant.class, remap = false)
public abstract class MTEPurificationPlantMixin extends MTEMultiBlockBase {
    protected MTEPurificationPlantMixin(String name) { super(name); }
    @Inject(method = "updateCycleProgress", at = @At("HEAD"), require = 1)
    private void cc$restoreNativeCycle(CallbackInfo ci) {
        // runtime.2/.3 persisted a shortened duration. Convert its completion fraction
        // once, without restarting the recipe, refunding inputs or repeating outputs.
        if (mMaxProgresstime <= 0) return;
        MTEPurificationPlant plant = (MTEPurificationPlant)(Object)this;
        int nativeMaximum = plant.debugModeOn() ? 600 : 2400;
        if (mMaxProgresstime == nativeMaximum) return;
        int progress = (int)Math.min(nativeMaximum, (long)Math.max(0, mProgresstime) * nativeMaximum / mMaxProgresstime);
        mMaxProgresstime = nativeMaximum;
        mProgresstime = progress;
        for (gregtech.common.tileentities.machines.multi.purification.LinkedPurificationUnit link : plant.getLinkedUnits()) {
            if (!link.isActive()) continue;
            link.metaTileEntity().mMaxProgresstime = nativeMaximum;
            link.metaTileEntity().mProgresstime = progress;
        }
    }
    @ModifyConstant(method = "updateCycleProgress", constant = @Constant(intValue = 1), require = 1, allow = 1,
        slice = @Slice(from = @At(value = "FIELD", target = "Lgregtech/common/tileentities/machines/multi/purification/MTEPurificationPlant;mProgresstime:I", opcode = Opcodes.GETFIELD, ordinal = 0),
                       to = @At(value = "FIELD", target = "Lgregtech/common/tileentities/machines/multi/purification/MTEPurificationPlant;mProgresstime:I", opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int cc$cycleStep(int original) { return ProgressMath.increment(mProgresstime, mMaxProgresstime, RuntimeProgress.purificationSpeed(this)); }
}
