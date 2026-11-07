package cn.elytra.gtnh.cutcorners.mixins.late.ic2;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets = "ic2.core.block.machine.tileentity.TileEntityStandardMachine", remap = false)
public class StandardMachineMixin {
    @Shadow protected short progress;
    @Shadow public int operationLength;
    @ModifyConstant(method = "updateEntityServer", constant = @Constant(intValue = 1), require = 1, allow = 1,
        slice = @Slice(from = @At(value = "FIELD", target = "Lic2/core/block/machine/tileentity/TileEntityStandardMachine;progress:S", opcode = Opcodes.GETFIELD, ordinal = 1),
                       to = @At(value = "FIELD", target = "Lic2/core/block/machine/tileentity/TileEntityStandardMachine;progress:S", opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int cc$poweredStep(int original) { return ProgressMath.increment(progress, Math.min(32767, operationLength), RuntimeProgress.otherSpeed()); }
}
