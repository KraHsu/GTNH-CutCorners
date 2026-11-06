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
            mixins.add("gregtech.MTEMultiBlockBaseMixin");
            mixins.add("gregtech.MTEBrickedBlastFurnaceMixin");
            mixins.add("gregtech.MTECokeOvenMixin");
            mixins.add("gregtech.MTEPurificationPlantMixin");
            mixins.add("tectech.MTEResearchStationMixin");
        }
        if (Mods.Thaumcraft.isLoaded()) mixins.add("thaumcraft.TileAlchemyFurnaceMixin");
        if (Mods.Railcraft.isLoaded()) { mixins.add("railcraft.TileCokeOvenMixin"); mixins.add("railcraft.TileBlastFurnaceMixin"); }
        if (Mods.EtFuturumRequiem.isLoaded()) mixins.add("et_futurum_requiem.BlastFurnaceAndSmoker_Mixin");
        return mixins;
    }
}
