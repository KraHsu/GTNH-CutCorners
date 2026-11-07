package cn.elytra.gtnh.cutcorners.mixins.late.enderio;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets = "crazypants.enderio.machine.AbstractPoweredTaskEntity", remap = false)
public class PoweredTaskMixin {
    @ModifyArg(method = "usePower(I)I", at = @At(value = "INVOKE", target = "Lcrazypants/enderio/machine/IPoweredTask;update(F)V"), index = 0, require = 1)
    private float cc$taskWork(float paidWork) { return paidWork * RuntimeProgress.otherSpeed(); }
}
