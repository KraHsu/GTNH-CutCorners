package cn.elytra.gtnh.cutcorners.mixins.late.gregtech;

import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public class MTEMultiBlockBaseMixin {
    @Shadow public int mProgresstime;
    @Shadow public int mMaxProgresstime;

    @ModifyConstant(method = "incrementProgressTime", constant = @Constant(intValue = 1), require = 1)
    private int cc$processingIncrement(int original) {
        return RuntimeProgress.increment(this, mProgresstime, mMaxProgresstime);
    }
}
