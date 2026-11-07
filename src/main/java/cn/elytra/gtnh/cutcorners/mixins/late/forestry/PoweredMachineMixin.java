package cn.elytra.gtnh.cutcorners.mixins.late.forestry;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets = "forestry.core.tiles.TilePowered", remap = false)
public abstract class PoweredMachineMixin {
    @Shadow private int workCounter;
    @Shadow public abstract int getTicksPerWorkCycle();
    @Shadow public abstract boolean hasWork();
    @Shadow protected abstract boolean workCycle();
    @Unique private int cc$speed() {
        String n = getClass().getName();
        return n.equals("forestry.factory.tiles.TileCarpenter") || n.equals("forestry.factory.tiles.TileCentrifuge")
            || n.equals("forestry.factory.tiles.TileFermenter") || n.equals("forestry.factory.tiles.TileSqueezer")
            || n.equals("forestry.factory.tiles.TileStill") || n.equals("forestry.factory.tiles.TileFabricator")
            || n.equals("forestry.factory.tiles.TileBottler") ? RuntimeProgress.otherSpeed() : 1;
    }
    @ModifyConstant(method = "updateServerSide", constant = @Constant(intValue = 1), require = 1, allow = 1,
        slice = @Slice(from = @At(value = "FIELD", target = "Lforestry/core/tiles/TilePowered;workCounter:I", opcode = Opcodes.GETFIELD, ordinal = 1),
                       to = @At(value = "FIELD", target = "Lforestry/core/tiles/TilePowered;workCounter:I", opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int cc$paidWork(int original) { return ProgressMath.increment(workCounter, getTicksPerWorkCycle(), cc$speed()); }
    @Redirect(method = "updateServerSide", at = @At(value = "INVOKE", target = "Lforestry/core/tiles/TilePowered;workCycle()Z"), require = 1)
    private boolean cc$processSubsteps(@Coerce Object owner) {
        for (int i = 0, n = cc$speed(); i < n; i++) {
            if (i > 0 && !hasWork()) return false;
            if (workCycle()) return true; // One completed batch; never consume the next recipe here.
        }
        return false;
    }
}
