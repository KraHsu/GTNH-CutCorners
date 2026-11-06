package cn.elytra.gtnh.cutcorners.mixinloader;

import cpw.mods.fml.common.Loader;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/** Native Mixin conditions; no extra FML mod or client dependency is needed. */
public final class MixinConfigPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public List<String> getMixins() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}

    @Override public boolean shouldApplyMixin(String target, String mixin) {
        boolean allowed = true;
        if (mixin.endsWith(".thaumcraft.ShapedArcaneRecipeNullFix")) {
            allowed = DevelopmentEnvironmentTester.isDevEnvironment();
        } else if (mixin.contains(".crops_nh.")) {
            allowed = Loader.isModLoaded("cropsnh");
        } else if (mixin.contains(".tconstruct.")) {
            allowed = Loader.isModLoaded("TConstruct");
        } else if (mixin.contains(".forestry.")) {
            allowed = Loader.isModLoaded("Forestry");
        }
        if (!allowed) CutCornersMixinRecorder.notApplied(mixin, "Native runtime/environment condition not met");
        return allowed;
    }
}
