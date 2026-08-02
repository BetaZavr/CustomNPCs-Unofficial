package noppes.npcs.mixin.client.gui.recipebook;

import net.minecraft.client.gui.recipebook.GuiRecipeOverlay;
import net.minecraft.client.gui.recipebook.RecipeBookPage;
import noppes.npcs.client.gui.recipebook.CustomGuiRecipeOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RecipeBookPage.class, priority = 498)
public class RecipeBookPageMixin {

    @Final @Shadow @Mutable private GuiRecipeOverlay overlay;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void npcs$replaceOverlay(CallbackInfo ci) { overlay = new CustomGuiRecipeOverlay(); }

}
