package cn.elytra.gtnh.cutcorners.mixins.late.thaumcraft;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ShapedArcaneRecipe;

/// TC throws exception when the recipe has something missing in dev.
/// This mixin replaces the null slots to sticks, so that the game won't be fucked.
@Mixin(value = ShapedArcaneRecipe.class, remap = false)
public class ShapedArcaneRecipeNullFix {

    @Inject(method = "<init>(Ljava/lang/String;Lnet/minecraft/item/ItemStack;Lthaumcraft/api/aspects/AspectList;[Ljava/lang/Object;)V", at = @At("HEAD"))
    private static void gtnhcc$handleNullItems(String research, ItemStack result, AspectList aspects, Object[] recipe, CallbackInfo ci) {
        for (int i = 0; i < recipe.length; i++) {
            if (recipe[i] == null) {
                recipe[i] = new ItemStack(Items.stick);
            }
        }
    }

}
