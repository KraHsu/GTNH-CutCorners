package cn.elytra.gtnh.cutcorners.mixins.late.tectech;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets = "tectech.thing.metaTileEntity.multi.bec.MTEBECIONode", remap = false)
public abstract class MTEBECIONodeMixin extends MTEMultiBlockBase {
    protected MTEBECIONodeMixin(String name) { super(name); }
    @Shadow protected abstract void incrementProgressTime();
    @Unique private boolean cc$substep;
    @Inject(method = "incrementProgressTime", at = @At("HEAD"), cancellable = true, require = 1)
    private void cc$nativeSubsteps(CallbackInfo ci) {
        int speed = RuntimeProgress.increment(this, 0, Integer.MAX_VALUE);
        if (cc$substep || speed <= 1 || mProgresstime < 0 || mMaxProgresstime <= 0) return;
        cc$substep = true;
        try {
            // Each step rechecks power, nanite tier, pause gates and actual condensate.
            for (int i = 0; i < speed && mMaxProgresstime > 0 && mProgresstime < mMaxProgresstime; i++) incrementProgressTime();
        } finally { cc$substep = false; }
        ci.cancel();
    }
}
