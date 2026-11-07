package cn.elytra.gtnh.cutcorners.mixins.late.gregtech;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets = {"gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitPlasmaHeater","gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitDegasser","gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitFlocculation","gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitPhAdjustment","gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitUVTreatment","gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitBaryonicPerfection"}, remap = false)
public abstract class MTEPurificationPhasesMixin extends MTEMultiBlockBase {
    protected MTEPurificationPhasesMixin(String name) { super(name); }
    @Shadow(remap = false) protected abstract void runMachine(IGregTechTileEntity base, long tick);
    @Unique private boolean cc$virtualPhase;
    @Inject(method = "runMachine", at = @At("HEAD"), cancellable = true, require = 1)
    private void cc$phaseTicks(IGregTechTileEntity base, long tick, CallbackInfo ci) {
        int speed = RuntimeProgress.purificationSpeed(this);
        if (cc$virtualPhase || speed <= 1 || mMaxProgresstime <= 0) return;
        cc$virtualPhase = true;
        try {
            // Continuous virtual tick sequence preserves every modulo-20 phase, even at >20x.
            for (int i = 0; i < speed && mMaxProgresstime > 0; i++) runMachine(base, tick * speed + i);
        } finally { cc$virtualPhase = false; }
        ci.cancel();
    }
}
