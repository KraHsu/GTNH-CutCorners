package cn.elytra.gtnh.cutcorners.mixins.late.ic2;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets = "ic2.core.block.machine.tileentity.TileEntityInduction", remap = false)
public class InductionMixin {
    @Shadow public short progress;
    @Redirect(method = "updateEntityServer", at = @At(value = "FIELD", target = "Lic2/core/block/machine/tileentity/TileEntityInduction;progress:S", opcode = Opcodes.PUTFIELD), require = 1)
    private void cc$poweredWork(@Coerce Object owner, short value) {
        progress = value > progress ? (short)Math.min(4000L, progress + (long)(value-progress)*RuntimeProgress.otherSpeed()) : value;
    }
}
