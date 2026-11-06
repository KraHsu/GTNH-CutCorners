package cn.elytra.gtnh.cutcorners.mixinloader;

import net.minecraft.launchwrapper.Launch;

public final class DevelopmentEnvironmentTester {
    private DevelopmentEnvironmentTester() {}
    public static boolean isDevEnvironment() {
        return Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"));
    }
}
