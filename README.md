# CutCorners Runtime — GTNH 2.9.0

Fork of [ElytraServers/GTNH-CutCorners](https://github.com/ElytraServers/GTNH-CutCorners/tree/version/2.9), based on v1.3.30 (`c615976`). Upstream authors and MIT license are retained.

This fork accelerates successful machine processing ticks instead of changing GT recipe registrations. Derived recipes keep their original durations, eliminating repeated scaling during recipe copying. The industrial chisel is covered through the common multiblock processing loop even though it chooses its processing time at runtime.

## Behavior

* Default: 10× processing, world stays at 20 TPS.
* GT single-block and common multiblock processing advance after the native power check. Inputs, outputs, parallel recipe counts, voltage checks and EU/t are unchanged. A job takes `ceil(native processing ticks / multiplier)`, with a minimum of one real tick. It cannot produce multiple batches in one real tick.
* **Total EU per batch falls with processing time.** This is faster/cheaper processing, not an energy-conserving overclock. It also is not a 10× CPU/tickrate boost.
* GT bricked blast furnaces and GT coke ovens have their own progress loops and are integrated from `v1.4.0-runtime.3`; recipes and per-batch solid fuel/input amounts remain unchanged.
* From `v1.4.0-runtime.4`, ten fusion variants and the Target Chamber have productive counter adapters; BEC nodes replay native gated substeps instead of multiplying material quantities.
* GT recipe definitions and NEI recipe durations stay original. The machine progress bar advances faster; NEI does not show the effective time.
* Positive-EU electrical generation is excluded. The research station accelerates actual computation consumption, leaves computation requirements unchanged and retains the native finalization tick; its cosmetic progress clock is excluded. Purification retains the original cycle length and advances the shared controller/unit progress plus six native process-phase bodies on the same accelerated clock. Old shortened cycles are converted once, retaining completion fraction and phase state.
* Vanilla/EFR/Thaumcraft furnace processing and Railcraft blast/coke ovens have small runtime hooks because they do not all use the GT base classes. Coke ovens retain Railcraft's batched work granularity and visual cooldown; their work interval and step are scaled together.
* Thermal exchangers are explicit exceptions to the positive-power filter: cold-fluid processing accelerates, while native per-tick steam production and distilled-water use remain unchanged. Total steam recovered per hot-fluid batch consequently falls.
* IC2 standard machines/induction, EnderIO powered tasks, seven Forestry factory classes plus the moistener, Railcraft crusher/rolling/steam oven and Thaumcraft centrifuge/infusion now have processing adapters. Native polling/batch granularity limits short jobs; this is not always an exact wall-clock 5×. The steam oven retains its 8000 mB startup buffer gate while charging a shortened cycle’s share of prepaid operating steam. The manual IC2 lathe is unchanged.
* From `v1.4.0-runtime.5`, GT basic miners, ore drilling plants, void miners and orbital mining modules use the configured multiplier when `mining=true`. Basic miners shorten their native extraction clock rather than skipping its extraction-at-duration-1 boundary. Their pipe retraction interval stays at least one tick. Pumps, oil drills, industrial bees, world farming/fishing and orbital pumps remain excluded.
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

## Coverage fixes in runtime.4

The [earlier audit](integration/coverage-audit-2.9.0.json) documents the runtime.2/.3 gaps. **runtime.4 implements those missing processing adapters and the extraction exclusions.** The archived audit is evidence of the earlier state, not an open-issue list for runtime.4.

Native checks in an isolated Java 25 pack now pass:

| Check | 1× | 5× | Preserved behaviour |
| --- | ---: | ---: | --- |
| Ten fusion implementations, one productive tick | +1 | +5 | 2 EU spent in either case |
| Target Chamber, no-beam-required branch | +1 | +5 | Native input/beam gates remain in method |
| Plasma heater, full native 2400-tick cycle | 2400 real ticks | 480 real ticks | 4 full heat/cool loops, 400 L plasma and 8000 L coolant |
| Large/Extreme exchanger, 20 real ticks | 1000 cold / 80000 steam | 5000 cold / 80000 steam | 500 distilled water in both cases |
| XL exchanger, 20 real ticks | 1000 cold / 160000 steam | 5000 cold / 160000 steam | 1000 distilled water in both cases |
| IC2 furnace, powered productive tick | +1 | +5 | 2 EU spent; no progress without power |
| EnderIO task, actual 10 RF consumed | 10 work | 50 work | Actual RF withdrawal stays 10; no work with zero RF |

BEC native substep tests preserve no-power/no-nanite stalls, nanite tier boundaries, immediate/step pauses and exactly 12 units of fixture condensate at either speed. Controller/unit clock sharing, unit-map blacklist propagation, UV countdown and conversion of old shortened purification cycles also pass. Conversion preserves existing phase state; it does not invent work missed by the old code.

The previous 22 primitive-machine, 23 common-machine and 4 research cases pass, as do all 8 unit tests, client compatibility checks and recipe-snapshot comparison. [Full results and precise limits](integration/verified-runtime.4-2.9.0.json) distinguish native processing probes from the narrower transformed-adapter checks for other mods. Physical multiblock assembly, every supply schedule and graphical clients are not validated by this suite.

The Beam Crafter consumes particles as they arrive and can finish in one call when fully supplied; its particle count is not a timer to multiply. Industrial bees, non-mining world extraction and the manually operated IC2 lathe remain unchanged.

## Mining in runtime.5

`mining=true` enables the configured processing multiplier for GT mining families.
It does not change drilling-pipe counts, drop quantities, radius, voltage tier,
EU/t, or per-cycle fluid inputs. IC2 miners, Ender Quarries and mining vehicles
are outside this GT mining integration.

The isolated GTNH 2.9.0 / Java 25 world tests execute the real basic miner,
pipe delegate and ore-breaking code. Four iron ore blocks yield four identical
outputs, with the following extraction intervals and energy totals:

| Basic miner | Native interval | 5× interval | EU for four ores, 1× / 5× |
| --- | ---: | ---: | ---: |
| LV | 160 ticks | 32 ticks | 5120 / 1024 |
| MV | 80 ticks | 16 ticks | 10240 / 2048 |
| HV | 40 ticks | 8 ticks | 20480 / 4096 |

Sixteen basic-miner cases cover feature/global/GT disabling, no power, blocked
outputs, missing pipes, real pipe descent, active-job clock changes in both
directions, and the one-tick limit at 1000×. Three retraction cases return all
four physical pipes once, including at 1000×. Ore drilling, void mining and
orbital mining additionally pass nine native productive-tick/power checks:
progress advances by 1 or 5 while consuming the same 2 EU, and no progress is
made without power. These multiblock checks stage controller state; they do
not assemble their structures or test their complete resource-extraction
throughput. All 43 registered extraction-family classes are also checked to
keep non-mining extraction at its original rate.

Clock conversion preserves pending versus already-performed extraction when
the multiplier changes. A saved interval is converted once and the tier's
original speed field is never overwritten. [Verification results and limits](integration/verified-runtime.5-2.9.0.json).

To rerun these checks without restarting the isolated pack for each helper:

```sh
python3 integration/run-coverage.py --work /tmp/cutc-runtime-test \
  --java /path/to/java25/bin/java --asm /path/to/asm-9.9.1.jar --run-native
```

The audit tools are separate from runtime code:

1. `CoverageScan.java` uses ASM 9.9.1 (`asm` and `asm-tree`) and Java 25 to inventory effective multi-release JAR classes and timer writes. Arguments: `MODS_DIRECTORY OUTPUT_DIRECTORY [SRG_MINECRAFT_JAR]`.
2. `CoverageProbe.java` and `CoverageRuntimeChecks.java` compile alongside `NativeChecks.java` with `javac --release 8`. The isolated harness can run their static `run(String output)` entry points through `checks/request.properties` (`jar`, `class`, `output`), without relaunching the pack. The runtime probes stage native machine state; they do not physically assemble factories.
3. `coverage-audit.py --bytecode SCAN_DIRECTORY --inventory INVENTORY_JSON --out OUTPUT_DIRECTORY` follows class/parent dispatch from tick methods. It does not resolve arbitrary member-object calls, reflection or every processing engine.
4. `build-coverage-report.py AUDIT_DIRECTORY` produces the searchable standalone HTML and JSON from `machine-inventory.json`, `registered/call-chains.json`, `runtime-probes.json` and `purification-phases.json`. The last file is curated phase inspection data. This report generator is intentionally specific to these 2.9.0 findings and rejects missing/failed probe evidence. Raw server JARs and large bytecode tables are not committed.

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
    B:mining=true
    B:otherMachines=true
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
