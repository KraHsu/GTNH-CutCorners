# CutCorners Runtime — GTNH 2.9.0

Fork of [ElytraServers/GTNH-CutCorners](https://github.com/ElytraServers/GTNH-CutCorners/tree/version/2.9), based on v1.3.30 (`c615976`). Upstream authors and MIT license are retained.

This fork accelerates successful machine processing ticks instead of changing GT recipe registrations. Derived recipes keep their original durations, eliminating repeated scaling during recipe copying. The industrial chisel is covered through the common multiblock processing loop even though it chooses its processing time at runtime.

## Behavior

* Default: 10× processing, world stays at 20 TPS.
* GT single-block and common multiblock processing advance after the native power check. Inputs, outputs, parallel recipe counts, voltage checks and EU/t are unchanged. A job takes `ceil(native processing ticks / multiplier)`, with a minimum of one real tick. It cannot produce multiple batches in one real tick.
* **Total EU per batch falls with processing time.** This is faster/cheaper processing, not an energy-conserving overclock. It also is not a 10× CPU/tickrate boost.
* GT bricked blast furnaces and GT coke ovens have their own progress loops and are integrated from `v1.4.0-runtime.3`; recipes and per-batch solid fuel/input amounts remain unchanged.
* GT recipe definitions and NEI recipe durations stay original. The machine progress bar advances faster; NEI does not show the effective time.
* Positive-EU electrical generation is excluded. The research station accelerates actual computation consumption, leaves computation requirements unchanged and retains the native finalization tick; its cosmetic progress clock is excluded. Purification keeps its shared controller/unit clock and shortens the common cycle instead of independently accelerating each unit.
* Vanilla/EFR/Thaumcraft furnace processing and Railcraft blast/coke ovens have small runtime hooks because they do not all use the GT base classes. Coke ovens retain Railcraft's batched work granularity and visual cooldown; their work interval and step are scaled together.
* Upstream instant-machine, recipe-tier, crop, bee, mining, logistics, node and harvest cheats are **not enabled by this fork**. Legacy code/API remains for upstream source compatibility but is not selected by the loader.

Machines with completely independent processing loops need a shared-loop integration of their own; this is not a promise to speed up every mod in GTNH. This version targets GTNH **2.9.0 / GT5 5.09.54.265**. Do not assume compatibility with other pack versions.

## Verified locally

GTNH 2.9.0 / GT5 5.09.54.265, headless Java 25: eight unit tests and the isolated native processing checks pass. Forge accepts a client mod map without CutCorners or `conditional_mixin`, while still rejecting a map missing required GregTech. The server starts without `conditional_mixin` installed; no classes in the built jar reference that library.

| Check | 1× | 5× | 10× |
| --- | ---: | ---: | ---: |
| LV macerator, 200-tick staged job | 200 | 40 | 20 |
| Industrial centrifuge (current and legacy), 200-tick staged job | 200 | 40 | 20 |
| Alloy blast smelter, 200-tick staged job | 200 | 40 | 20 |
| Industrial chisel, 200-tick staged job | 200 | 40 | 20 |
| Research computation 500 / native step 10 (finalization excluded) | 50 | 10 | 5 |
| GT bricked blast furnace, native iron + coal recipe | 7200 | 1440 | 720 |
| GT coke oven, native coal recipe | 1800 | 360 | 180 |

The primitive machines also pass disabled/blacklist, broken structure, missing input, blocked output, in-progress job, one-tick and rounding checks. Coke oven creosote amounts remain exactly 500/1000/1500 mB after consecutive coal jobs. Primitive structure flags are staged; physical structure assembly is not tested.

Three consecutive batches produce exactly three outputs. Unpowered machine checks produce none. The real vanilla furnace cooks a potato in 20 ticks at 10×. Disabled processing returns to 200 ticks; an on-disk reload to 3× gives 67 ticks. All 187,204 loaded GT recipe rows remain identical across runtime multiplier switches. This is not a physical assembly test of every machine or a graphical client test. See [verification details and limits](integration/verified-2.9.0.json).

## Configuration

Install this jar **instead of** the upstream CutCorners jar. From `v1.4.0-runtime.2`, no external `conditional_mixin` dependency is used. Remove `conditional_mixin-1.1.0.jar` if it was installed only for CutCorners; retain it if another mod independently requires it. **Clients do not need CutCorners or its former dependency to join.** Back up your world and test your own machines before deploying. The old `CutCorners-1.cfg` is not used.

The generated `config/CutCornersRuntime.cfg` contains:

```properties
general {
    B:enabled=true
    I:multiplier=10
    B:gregTech=true
    B:furnaces=true
    B:railcraft=true
    S:recipeMapBlacklist <
        gg.recipe.naquadah_reactor
        gt.recipe.largeboilerfakefuels
    >
}
```

Edit this file, then run `/cutcorners reload` as an operator (or `cutcorners reload` in the console). These fields are reloadable; initial mod installation/removal still requires a restart. Disabling acceleration returns future processing ticks to normal; it does not undo already completed jobs.

`/cutcorners getMachineInfo` shows the targeted machine class and available recipe maps. Its displayed multiplier is the configured global rate; generator/blacklist exclusions still apply.

## Build and isolated verification

```sh
./gradlew test build
# Optional: use installed Temurin toolchains instead of upstream Azul defaults:
./gradlew -PbuildJvmVendor=adoptium test build
```

The project uses Java 25 for compilation and the upstream multi-release Java 8/21/25 packaging. Java 8 is also required by the Minecraft development tooling. `buildJvmVendor` is optional and does not change the default upstream CI toolchain.

`integration/run-local.py` copies a supplied GTNH server template to a fresh directory, binds only loopback on an ephemeral port and creates a fresh flat test world. It refuses to overwrite a directory or silently accept the EULA. Use `--accept-eula` only after agreeing to the Minecraft EULA. It never connects to or changes your production server.

```sh
python3 integration/run-local.py --template /path/to/GTNH-2.9.0-server \
  --mod /path/to/built-CutCorners.jar \
  --java /path/to/java25/bin/java --work /tmp/cutc-runtime-test
python3 integration/run-local.py --work /tmp/cutc-runtime-test --request
# native-results.json, recipes.tsv.gz and server.log are written in that directory
python3 integration/run-local.py --work /tmp/cutc-runtime-test --stop
```

The harness runs real registered GT machine classes and their native processing methods, with isolated power/output base-tile doubles; it does not claim to assemble every multiblock or simulate a player client. It first checks the real Forge network validators with a client map omitting CutCorners and the former dependency, including a negative control that missing required GregTech is still rejected. It also checks consecutive batches at 1×/10×/disabled, short jobs, missing power, and a real vanilla furnace. Helpers can be rebuilt/reloaded in the same test JVM without restarting the pack.

[Original upstream documentation](UPSTREAM_README.md).
