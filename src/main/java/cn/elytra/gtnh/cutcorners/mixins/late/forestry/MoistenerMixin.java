package cn.elytra.gtnh.cutcorners.mixins.late.forestry;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets = "forestry.factory.tiles.TileMoistener", remap = false)
public class MoistenerMixin {
    @Shadow private int productionTime;
    @Redirect(method = "updateServerSide", at = @At(value = "FIELD", target = "Lforestry/factory/tiles/TileMoistener;productionTime:I", opcode = Opcodes.PUTFIELD), require = 1, allow = 1)
    private void cc$processTime(@Coerce Object owner, int value) {
        productionTime = value < productionTime ? (int)Math.max(0L, productionTime - (long)(productionTime-value)*RuntimeProgress.otherSpeed()) : value;
    }
}
