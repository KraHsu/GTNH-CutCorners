package cn.elytra.gtnh.cutcorners.mixinloader;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@LateMixin
public class CCMixinLoader implements ILateMixinLoader {
    @Override public String getMixinConfig() { return "mixins.CutCorners.late.json"; }
    @Override public List<String> getMixins(Set<String> loadedMods) {
        Mods.init(loadedMods);
        List<String> mixins = new ArrayList<String>();
        if (Mods.GregTech.isLoaded()) {
            mixins.add("gregtech.MTEBasicMachineMixin");
            mixins.add("gregtech.MTEFusionComputerMixin");
            mixins.add("gregtech.MTELargeFusionComputerMixin");
            mixins.add("gregtech.MTETargetChamberMixin");
            mixins.add("gregtech.MTEPurificationPhasesMixin");
            mixins.add("tectech.MTEBECIONodeMixin");
            mixins.add("gregtech.MTEMultiBlockBaseMixin");
            mixins.add("gregtech.MTEBrickedBlastFurnaceMixin");
            mixins.add("gregtech.MTECokeOvenMixin");
            mixins.add("gregtech.MTEPurificationPlantMixin");
            mixins.add("tectech.MTEResearchStationMixin");
        }
        if (loadedMods.contains("IC2")) { mixins.add("ic2.StandardMachineMixin"); mixins.add("ic2.InductionMixin"); }
        if (loadedMods.contains("EnderIO")) mixins.add("enderio.PoweredTaskMixin");
        if (loadedMods.contains("Forestry")) { mixins.add("forestry.PoweredMachineMixin"); mixins.add("forestry.MoistenerMixin"); }
        if (Mods.Thaumcraft.isLoaded()) { mixins.add("thaumcraft.TileAlchemyFurnaceMixin"); mixins.add("thaumcraft.TileCentrifugeMixin"); mixins.add("thaumcraft.TileInfusionMatrixMixin"); }
        if (Mods.Railcraft.isLoaded()) { mixins.add("railcraft.TileCokeOvenMixin"); mixins.add("railcraft.TileBlastFurnaceMixin"); mixins.add("railcraft.TileRockCrusherMixin"); mixins.add("railcraft.TileRollingMachineMixin"); mixins.add("railcraft.TileSteamOvenMixin"); }
        if (Mods.EtFuturumRequiem.isLoaded()) mixins.add("et_futurum_requiem.BlastFurnaceAndSmoker_Mixin");
        return mixins;
    }
}
