package noppes.npcs.mixin.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiScreen.class)
public class GuiScreenBackgroundMixin {

    @Unique
    private static final ResourceLocation NPCS$OPTIONS_BACKGROUND =
            new ResourceLocation(CustomNpcs.MODID, "textures/gui/options_background.png");

    @Redirect(
            method = "drawBackground",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V")
    )
    private void npcs$bindBackground(TextureManager manager, ResourceLocation vanilla) {
        manager.bindTexture(CustomNpcs.ReplaceCustomBackground ? NPCS$OPTIONS_BACKGROUND : vanilla);
    }

}
