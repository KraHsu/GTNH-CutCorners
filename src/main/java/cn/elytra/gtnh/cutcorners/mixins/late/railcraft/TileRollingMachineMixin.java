package cn.elytra.gtnh.cutcorners.mixins.late.railcraft;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets = "mods.railcraft.common.blocks.machine.alpha.TileRollingMachine", remap = false)
public class TileRollingMachineMixin {
    @Shadow private int progress;
    @Redirect(method = {"updateEntity", "func_145845_h"}, at = @At(value = "FIELD", target = "Lmods/railcraft/common/blocks/machine/alpha/TileRollingMachine;progress:I", opcode = Opcodes.PUTFIELD), require = 1)
    private void cc$poweredProgress(@Coerce Object owner, int value) {
        progress = value > progress ? (int)Math.min(100L, progress + (long)(value-progress)*RuntimeProgress.railcraftSpeed()) : value;
    }
}
