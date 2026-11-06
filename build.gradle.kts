plugins {
    id("com.gtnewhorizons.gtnhconvention")
    id("com.github.ElytraServers.elytra-conventions") version "v1.1.2"
}

// Reuse an installed JDK without downloading a second vendor's toolchain.
// Omit -PbuildJvmVendor to retain the upstream vendor selection.
providers.gradleProperty("buildJvmVendor").orNull?.let { requestedVendor ->
    java.toolchain.vendor.set(if (requestedVendor.equals("adoptium", ignoreCase = true)) JvmVendorSpec.ADOPTIUM
        else JvmVendorSpec.matching(requestedVendor))
}

elytraModpackVersion {
    gtnhVersion = "2.9.0"
    attachManifestVersionToJar = true
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    fun ModuleDependency.dev(transitive: Boolean = true) {
        artifact { classifier = "dev" }
        if (!transitive) isTransitive = false
    }

    // JSpecify
    compileOnly("org.jspecify:jspecify:1.0.0")
    // JUnit
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mod dependencies

    devOnlyNonPublishable(nh.gt5Unofficial) { dev() }
    devOnlyNonPublishable(nh.galaxySpaceGtnh) { dev() }

    devOnlyNonPublishable(nh.botania) { dev(false) }
    devOnlyNonPublishable(nh.railcraft) { dev(false) }
    devOnlyNonPublishable(nh.tinkersConstruct) { dev() }
    devOnlyNonPublishable(nh.newHorizonsCoreMod) { dev() }
    devOnlyNonPublishable(nh.cropsNh) { dev() }
    runtimeOnlyNonPublishable(nh.notEnoughItems) { dev() }
    runtimeOnlyNonPublishable(nh.waila) { dev() }
    runtimeOnlyNonPublishable(nh.enderCore) { dev() }
    runtimeOnlyNonPublishable(nh.enderIo) { dev() }
    runtimeOnlyNonPublishable(nh.appleCore) { dev() }
    runtimeOnlyNonPublishable(nh.betterBuildersWands) { dev() }
    runtimeOnlyNonPublishable(nh.forestryMc) { dev() }
    devOnlyNonPublishable("ganymedes01.etfuturum:Et-Futurum-Requiem:" + nh.versions.etFuturumRequiem.get()) {
        artifact { classifier = "dev" }
    }
    implementation("thaumcraft:Thaumcraft:1.7.10-4.2.3.5:dev") { isTransitive = false }

}

tasks.test {
    // GTNHGradle 2.0.26's downgraded FileCollection is not a class-root
    // directory for Gradle 9.4 (silently yields NO-SOURCE). Test native
    // compiled classes with the project JDK; the isolated pack checks the jar.
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
}
