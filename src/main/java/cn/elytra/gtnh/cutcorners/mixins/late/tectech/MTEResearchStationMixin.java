package cn.elytra.gtnh.cutcorners.mixins.late.tectech;

import cn.elytra.gtnh.cutcorners.config.RuntimeConfig;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tectech.thing.metaTileEntity.multi.MTEResearchStation;
import tectech.thing.metaTileEntity.multi.base.TTMultiblockBase;

@Mixin(value = MTEResearchStation.class, remap = false)
public abstract class MTEResearchStationMixin extends TTMultiblockBase {
    protected MTEResearchStationMixin(String name) { super(name); }
    @Shadow public long computationRemaining;

    @Inject(method = "onRunningTick", at = @At("RETURN"), require = 1)
    private void cc$computeAfterPowerCheck(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !RuntimeConfig.gregTech) return;
        computationRemaining = ProgressMath.afterExtraWork(
            computationRemaining, eAvailableData, RuntimeProgress.speed());
    }
}
