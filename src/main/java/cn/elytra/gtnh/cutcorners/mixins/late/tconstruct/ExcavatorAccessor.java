package cn.elytra.gtnh.cutcorners.mixins.late.tconstruct;

import cn.elytra.gtnh.cutcorners.util.ModConst;
import net.minecraft.block.material.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import tconstruct.items.tools.Excavator;

@Mixin(value = Excavator.class, remap = false)
public interface ExcavatorAccessor {

    @Accessor("materials")
    static Material[] getMaterials() {
        throw new AssertionError();
    }

}
