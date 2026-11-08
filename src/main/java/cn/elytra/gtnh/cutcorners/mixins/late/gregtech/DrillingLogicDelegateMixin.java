package cn.elytra.gtnh.cutcorners.mixins.late.gregtech;

import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gregtech.common.misc.DrillingLogicDelegate;
import gregtech.common.misc.IDrillingLogicDelegateOwner;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = DrillingLogicDelegate.class, remap = false)
public class DrillingLogicDelegateMixin {
    @Shadow @Final private IDrillingLogicDelegateOwner owner;

    @ModifyExpressionValue(method = "onPostTickRetract", at = @At(value = "INVOKE",
        target = "Lgregtech/common/misc/IDrillingLogicDelegateOwner;getMachineSpeed()I"), require = 1)
    private int cc$retractionInterval(int original) {
        return RuntimeProgress.miningRetractionClock(owner, original);
    }
}
