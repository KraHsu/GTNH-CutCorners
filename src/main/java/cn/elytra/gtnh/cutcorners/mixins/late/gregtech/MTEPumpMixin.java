package cn.elytra.gtnh.cutcorners.mixins.late.gregtech;

import cn.elytra.gtnh.cutcorners.runtime.PumpEnergyLedger;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gregtech.common.tileentities.machines.basic.MTEPump;
import net.minecraft.nbt.NBTTagCompound;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MTEPump.class, remap = false)
public class MTEPumpMixin {
    @Unique private final PumpEnergyLedger cc$pumpEnergy = new PumpEnergyLedger();

    // Accelerate only the two countdown decrements, not timer resets or BFS scans.
    @WrapOperation(method = "onPostTick", at = @At(value = "FIELD",
        target = "Lgregtech/common/tileentities/machines/basic/MTEPump;mPumpTimer:I",
        opcode = Opcodes.PUTFIELD, ordinal = 0), require = 1)
    private void cc$ownClock(MTEPump pump, int value, Operation<Void> original) {
        cc$decrement(pump, value, original);
    }

    @WrapOperation(method = "onPostTick", at = @At(value = "FIELD",
        target = "Lgregtech/common/tileentities/machines/basic/MTEPump;mPumpTimer:I",
        opcode = Opcodes.PUTFIELD, ordinal = 1), require = 1)
    private void cc$stackedClock(MTEPump pump, int value, Operation<Void> original) {
        cc$decrement(pump, value, original);
    }

    @Unique private static void cc$decrement(MTEPump pump, int nativeValue, Operation<Void> original) {
        int speed = RuntimeProgress.pumpingSpeed();
        original.call(pump, speed <= 1 ? nativeValue
            : (int) Math.max(Integer.MIN_VALUE, (long) pump.mPumpTimer - speed));
    }

    @WrapOperation(method = "onPostTick", at = @At(value = "FIELD",
        target = "Lgregtech/common/tileentities/machines/basic/MTEPump;mProgresstime:I",
        opcode = Opcodes.PUTFIELD, ordinal = 0), require = 1)
    private void cc$stackedDisplay(MTEPump pump, int value, Operation<Void> original) {
        int speed = RuntimeProgress.pumpingSpeed();
        original.call(pump, speed <= 1 ? value : pump.mProgresstime
            + ProgressMath.increment(pump.mProgresstime, pump.mMaxProgresstime, speed));
    }

    // Initial pipe descent and retraction, without increasing fluid-queue scan frequency.
    @ModifyConstant(method = "onPostTick", constant = @Constant(longValue = 100L, ordinal = 0), require = 1)
    private long cc$descentInterval(long original) {
        return ProgressMath.duration((int) original, RuntimeProgress.pumpingSpeed());
    }

    @ModifyConstant(method = "onPostTick", constant = @Constant(longValue = 5L), require = 1)
    private long cc$retractionInterval(long original) {
        return ProgressMath.duration((int) original, RuntimeProgress.pumpingSpeed());
    }

    @ModifyArg(method = "onPostTick", at = @At(value = "INVOKE",
        target = "Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;isUniversalEnergyStored(J)Z"), index = 0, require = 1)
    private long cc$energyGate(long original) {
        return PumpEnergyLedger.gate(original, RuntimeProgress.pumpingSpeed());
    }

    @ModifyArg(method = "consumeFluid", at = @At(value = "INVOKE",
        target = "Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;decreaseStoredEnergyUnits(JZ)Z"), index = 0, require = 1)
    private long cc$actionEnergy(long original) {
        return cc$pumpEnergy.charge(original, RuntimeProgress.pumpingSpeed());
    }

    @Inject(method = "saveNBTData", at = @At("TAIL"))
    private void cc$saveEnergy(NBTTagCompound nbt, CallbackInfo ci) {
        nbt.setInteger("CutCornersPumpRemainder", cc$pumpEnergy.remainder());
        nbt.setInteger("CutCornersPumpDivisor", cc$pumpEnergy.divisor());
    }

    @Inject(method = "loadNBTData", at = @At("TAIL"))
    private void cc$loadEnergy(NBTTagCompound nbt, CallbackInfo ci) {
        cc$pumpEnergy.restore(nbt.getInteger("CutCornersPumpRemainder"), nbt.getInteger("CutCornersPumpDivisor"));
    }
}
