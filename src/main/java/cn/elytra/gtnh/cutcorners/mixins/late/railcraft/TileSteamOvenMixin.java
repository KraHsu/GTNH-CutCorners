package cn.elytra.gtnh.cutcorners.mixins.late.railcraft;
import cn.elytra.gtnh.cutcorners.runtime.RuntimeProgress;
import cn.elytra.gtnh.cutcorners.runtime.ProgressMath;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets = "mods.railcraft.common.blocks.machine.alpha.TileSteamOven", remap = false)
public class TileSteamOvenMixin {
    // The oven prepays an entire cycle's operating steam. Keep its original
    // 8000 mB startup buffer check, but charge only the shortened cycle's share.
    @ModifyConstant(method = "drainSteam", constant = @Constant(intValue = 8000, ordinal = 2), require = 1, allow = 1)
    private int cc$cycleSteam(int original) { return ProgressMath.duration(original, RuntimeProgress.railcraftSpeed()); }
    @ModifyConstant(method = {"updateEntity", "func_145845_h"}, constant = @Constant(intValue = 256), require = 1, allow = 1)
    private int cc$cycleDuration(int original) { return ProgressMath.duration(original, RuntimeProgress.railcraftSpeed()); }
}
