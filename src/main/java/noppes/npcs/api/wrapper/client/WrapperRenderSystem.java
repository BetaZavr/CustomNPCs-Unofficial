package noppes.npcs.api.wrapper.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.api.client.IRenderSystem;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiEntityDisplay;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.util.ValueUtil;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class WrapperRenderSystem implements IRenderSystem {

    private final Minecraft minecraft;

    public WrapperRenderSystem(Minecraft mc) { this.minecraft = mc; }

    @Override
    public void enableBlend() { RenderSystem.enableBlend(); }

    @Override
    public void disableBlend() { RenderSystem.disableBlend(); }

    @Override
    public PoseStack pushPose(GuiGraphics graphics) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        return matrixStack;
    }

    @Override
    public PoseStack popPose(GuiGraphics graphics) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.popPose();
        return matrixStack;
    }

    @Override
    public void color(float red, float green, float blue, float alpha) { RenderSystem.setShaderColor(red, green, blue, alpha); }

    @Override
    public void translate(GuiGraphics graphics, float x, float y, float z) { graphics.pose().translate(x, y, z); }

    @Override
    public void scale(GuiGraphics graphics, float x, float y, float z) { graphics.pose().scale(x, y, z); }

    @Override
    public void rotate(GuiGraphics graphics, float angle, float axisX, float axisY, float axisZ) {
        if (angle != 0.0f) {
            PoseStack matrixStack = graphics.pose();
            if (axisX != 0.0f) { matrixStack.mulPose(Axis.XP.rotationDegrees(axisX)); }
            if (axisY != 0.0f) { matrixStack.mulPose(Axis.YP.rotationDegrees(axisY)); }
            if (axisZ != 0.0f) { matrixStack.mulPose(Axis.ZP.rotationDegrees(axisZ)); }
        }
    }

    @Override
    public void drawString(GuiGraphics graphics, String text, float x, float y, int color, boolean dropShadow) {
        if (text == null || !text.isEmpty()) { return; }
        graphics.drawString(minecraft.font, text, x, y, color, dropShadow);
    }

    @Override
    public void draw(GuiGraphics graphics, int left, int top, int width, int height, int color) {
        graphics.fill(left, top, left + width, top + height, color);
    }

    @Override
    public void draw(GuiGraphics graphics, int left, int top, int width, int height, float red, float green, float blue, float alpha) {
        if (alpha <= 0.0f) { return; } else if (alpha > 1.0f) { alpha = 1.0f; }
        if (red < 0.0f) { red = 0.0f; } else if (red > 1.0f) { red = 1.0f; }
        if (green < 0.0f) { green = 0.0f; } else if (green > 1.0f) { green = 1.0f; }
        if (blue < 0.0f) { blue = 0.0f; } else if (blue > 1.0f) { blue = 1.0f; }
        int color = Mth.ceil(red * 255.0F) | Mth.ceil(green * 255.0F) << 8 | Mth.ceil(blue * 255.0F) << 16 | Mth.ceil(alpha * 255.0F) << 24;
        graphics.fill(left, top, left + width, top + height, color);
    }

    @Override
    public void drawTexture(GuiGraphics graphics, String resourceLocation, float x, float y, int u, int v, float width, float height, boolean revers) {
        if (resourceLocation == null || resourceLocation.isEmpty()) { return; }
        ResourceLocation location = new ResourceLocation(resourceLocation);

        RenderSystem.setShaderTexture(0, location);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        float w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        float h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (w > 256.0f) {
            w = 256.0f;
            width *= 256.0f / w;
        }
        if (h > 256.0f) {
            h = 256.0f;
            height *= 256.0f / h;
        }
        float us = (revers ? u + width : u) / 256.0f;
        float ue = (revers ? u : u + width) / 256.0f;

        graphics.pose().translate(x ,y, 0.0f);
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f, 0.0f, 0.0f, 0.0f).uv(us, v / 256.0f).endVertex();
        bufferbuilder.vertex(matrix4f, 0.0f, height, 0.0f).uv(us, (v +height) / 256.0f).endVertex();
        bufferbuilder.vertex(matrix4f, width, height, 0.0f).uv(ue, (v +height) / 256.0f).endVertex();
        bufferbuilder.vertex(matrix4f, width, 0.0f, 0.0f).uv(ue, v / 256.0f).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());

    }

    @Override
    public void renderEntity(GuiGraphics graphics, Entity entity, int x, int y, float scale, int yaw, int pitch, int followCursor) {
        Entity e = null;
        if (entity instanceof Entity) { e = entity; }
        if (entity instanceof IEntity<?> iEntity) { e = iEntity.getMCEntity(); }
        if (e == null) { return; }
        CustomGuiEntityDisplay.drawEntity(graphics, e, 0, 0, scale, yaw, pitch,
                (int) minecraft.mouseHandler.xpos(), (int) minecraft.mouseHandler.ypos(),
                x, y, followCursor, true);
    }

    @Override
    public void drawOBJ(GuiGraphics graphics, String resourceLocation, List<String> visibleMeshes, Map<String, ResourceLocation> materialTextures, int lightMap, int overlay) {
        if (resourceLocation == null || !resourceLocation.isEmpty()) { return; }
        ModelBuffer.render(graphics.pose(), graphics.bufferSource(), new ResourceLocation(resourceLocation), visibleMeshes, materialTextures,
                ValueUtil.correctInt(lightMap, 0, LightTexture.FULL_BRIGHT),
                ValueUtil.correctInt(overlay, 0, OverlayTexture.NO_OVERLAY), 0);
    }

}
