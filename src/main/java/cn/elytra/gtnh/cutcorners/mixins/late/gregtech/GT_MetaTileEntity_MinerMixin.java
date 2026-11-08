package cn.elytra.gtnh.cutcorners.mixins.late.gregtech;

import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gregtech.common.tileentities.machines.basic.MTEMiner;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

@Mixin(value = MTEMiner.class, remap = false)
public class GT_MetaTileEntity_MinerMixin {
    @Shadow @Final private int mSpeed;

    @Inject(method = "onPostTick", at = @At("HEAD"))
    private void cc$convertMiningClock(IGregTechTileEntity base, long tick, CallbackInfo ci) {
        if (!base.isServerSide()) return;
        MTEMiner self = (MTEMiner) (Object) this;
        int duration = RuntimeProgress.miningDuration(mSpeed);
        if (self.mMaxProgresstime > 0 && self.mMaxProgresstime != duration) {
            self.mProgresstime = ProgressMath.miningProgress(self.mProgresstime, self.mMaxProgresstime, duration);
            self.mMaxProgresstime = duration;
        }
    }

    /// Shortens the mining cycle by scaling {@code mSpeed} where it is read, rather than writing it back into the
    /// field.
    ///
    /// Writing the scaled value back every tick compounds it: with the default 0.5 multiplier an IV miner decays
    /// 20 -> 10 -> 5 -> 2 -> 1 within four ticks. Once {@code mSpeed} drops below 5, the integer division in
    /// {@code DrillingLogicDelegate#onPostTickRetract} turns {@code getMachineSpeed() / 5} into 0, and then
    /// {@code aTick % 0} throws {@link ArithmeticException}.
    @ModifyExpressionValue(
        method = "onPostTick",
        at = @At(
            value = "FIELD",
            target = "Lgregtech/common/tileentities/machines/basic/MTEMiner;mSpeed:I",
            opcode = Opcodes.GETFIELD))
    private int gtnhcc$modifyMiningSpeed(int original) {
        return RuntimeProgress.miningDuration(original);
    }
}
