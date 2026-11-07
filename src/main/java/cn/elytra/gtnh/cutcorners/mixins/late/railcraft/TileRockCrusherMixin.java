package cn.elytra.gtnh.cutcorners.mixins.late.railcraft;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets = "mods.railcraft.common.blocks.machine.alpha.TileRockCrusher", remap = false)
public class TileRockCrusherMixin {
    @Shadow private int processTime;
    @Redirect(method = {"updateEntity", "func_145845_h"}, at = @At(value = "FIELD", target = "Lmods/railcraft/common/blocks/machine/alpha/TileRockCrusher;processTime:I", opcode = Opcodes.PUTFIELD), require = 1)
    private void cc$poweredProgress(@Coerce Object owner, int value) {
        processTime = value > processTime ? (int)Math.min(100L, processTime + (long)(value-processTime)*RuntimeProgress.railcraftSpeed()) : value;
    }
}
