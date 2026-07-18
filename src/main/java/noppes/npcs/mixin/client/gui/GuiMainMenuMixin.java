package noppes.npcs.mixin.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.CustomNpcs;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Mixin(value = GuiMainMenu.class, priority = 498)
public abstract class GuiMainMenuMixin extends GuiScreen {

    @Final @Shadow private static ResourceLocation[] TITLE_PANORAMA_PATHS;
    @Shadow private float panoramaTimer;

    @Shadow private void drawPanorama(int mouseX, int mouseY, float partialTicks) { }

    @Unique private static final List<String> cnpc$names = Arrays.asList("MC", "1", "2", "3", "4");
    @Unique public int cnpc$variant = new Random().nextInt(CustomNpcs.PanoramaNumbers);

    @Inject(method = "initGui", at = @At("TAIL"))
    public void npcs$initGui(CallbackInfo ci) {
        CustomNpcs.resetChars(CustomNpcs.CharCurrencies, CustomNpcs.CharDonation);
        if (CustomNpcs.ReplaceCustomBackground && CustomNpcs.ShowButtonsInGuiMenu) {
            buttonList.add(new GuiButton(150, 3, 3, 20, 20, cnpc$names.get(cnpc$variant)));
        }
        if (!CustomNpcs.ReplaceCustomBackground && cnpc$variant > 0) { cnpc$variant = 0; }
        for(int i = 0; i < 6; ++i) {
            if (cnpc$variant == 0) { TITLE_PANORAMA_PATHS[i] = new ResourceLocation("textures/gui/title/background/panorama_" + i + ".png"); }
            else { TITLE_PANORAMA_PATHS[i] = new ResourceLocation(CustomNpcs.MODID, "textures/gui/title/background/" + (cnpc$variant - 1) + "/panorama_" + i + ".png"); }
        }
    }

    @Inject(method = "actionPerformed", at = @At("TAIL"))
    protected void npcs$actionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 150 && CustomNpcs.ReplaceCustomBackground && CustomNpcs.ShowButtonsInGuiMenu) {
            cnpc$variant++;
            cnpc$variant = cnpc$variant % cnpc$names.size();
            button.displayString = cnpc$names.get(cnpc$variant);
            for(int i = 0; i < 6; ++i) {
                if (cnpc$variant == 0) { TITLE_PANORAMA_PATHS[i] = new ResourceLocation("textures/gui/title/background/panorama_" + i + ".png"); }
                else { TITLE_PANORAMA_PATHS[i] = new ResourceLocation(CustomNpcs.MODID, "textures/gui/title/background/" + (cnpc$variant - 1) + "/panorama_" + i + ".png"); }
            }
        }
    }

    @Inject(method = "renderSkybox", at = @At("HEAD"), cancellable = true)
    private void npcs$renderSkybox(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!CustomNpcs.ReplaceCustomBackground) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
        drawPanorama(mouseX, mouseY, partialTicks);
        ci.cancel();
    }

    @Inject(method = "drawPanorama", at = @At("HEAD"), cancellable = true)
    private void npcs$drawPanorama(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!CustomNpcs.ReplaceCustomBackground) { return; }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();

        float aspect = (float)mc.displayWidth / (float)mc.displayHeight;

        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(85.0F, aspect, 0.05F, 10.0F);

        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);

        GlStateManager.rotate(MathHelper.sin(panoramaTimer / 400.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-panoramaTimer * 0.1F, 0.0F, 1.0F, 0.0F);

        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        for (int k = 0; k < 6; ++k) {
            mc.getTextureManager().bindTexture(TITLE_PANORAMA_PATHS[k]);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

            if (k == 0) {
                bufferbuilder.pos(-1.0D, -1.0D, 1.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(-1.0D, 1.0D, 1.0D).tex(0.0D, 1.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(1.0D, 1.0D, 1.0D).tex(1.0D, 1.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(1.0D, -1.0D, 1.0D).tex(1.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            }
            else if (k == 1) {
                bufferbuilder.pos(1.0D, -1.0D, 1.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(1.0D, 1.0D, 1.0D).tex(0.0D, 1.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(1.0D, 1.0D, -1.0D).tex(1.0D, 1.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(1.0D, -1.0D, -1.0D).tex(1.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            }
            else if (k == 2) {
                bufferbuilder.pos(1.0D, -1.0D, -1.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(1.0D, 1.0D, -1.0D).tex(0.0D, 1.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(-1.0D, 1.0D, -1.0D).tex(1.0D, 1.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(-1.0D, -1.0D, -1.0D).tex(1.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            }
            else if (k == 3) {
                bufferbuilder.pos(-1.0D, -1.0D, -1.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(-1.0D, 1.0D, -1.0D).tex(0.0D, 1.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(-1.0D, 1.0D, 1.0D).tex(1.0D, 1.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(-1.0D, -1.0D, 1.0D).tex(1.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            }
            else if (k == 4) {
                bufferbuilder.pos(-1.0D, -1.0D, -1.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(-1.0D, -1.0D, 1.0D).tex(0.0D, 1.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(1.0D, -1.0D, 1.0D).tex(1.0D, 1.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(1.0D, -1.0D, -1.0D).tex(1.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            }
            else {
                bufferbuilder.pos(-1.0D, 1.0D, 1.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(-1.0D, 1.0D, -1.0D).tex(0.0D, 1.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(1.0D, 1.0D, -1.0D).tex(1.0D, 1.0D).color(255, 255, 255, 255).endVertex();
                bufferbuilder.pos(1.0D, 1.0D, 1.0D).tex(1.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            }

            tessellator.draw();
        }

        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.matrixMode(5889);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();

        ci.cancel();
    }

    @Inject(method = "rotateAndBlurSkybox", at = @At("HEAD"), cancellable = true)
    private void npcs$rotateAndBlurSkybox(CallbackInfo ci) {
        if (!CustomNpcs.ReplaceCustomBackground) { return; }
        ci.cancel();
    }

}