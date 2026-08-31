package noppes.npcs.client.layer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class LayerGlow<T extends EntityLivingBase> extends LayerInterface<T> {

    public LayerGlow(RenderLiving<?> renderIn) { super(renderIn); }

    @Override
    public void render(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (!npc.display.getOverlayTexture().isEmpty()) {
            if (npc.textureGlowLocation == null) {
                npc.textureGlowLocation = new ResourceLocation(npc.display.getOverlayTexture());
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(npc.textureGlowLocation);
            if (npc.display.isOverlayGlowing()) {
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
                GlStateManager.disableLighting();
                GlStateManager.pushMatrix();
            }
            model.render(npc, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            if (npc.display.isOverlayGlowing()) {
                GlStateManager.popMatrix();
                GlStateManager.enableLighting();
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ZERO);
                GlStateManager.disableBlend();
            }
        }
    }

    @Override
    public void rotate(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {

    }
}
