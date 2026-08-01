package noppes.npcs.client.layer;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumHandSide;
import noppes.npcs.CustomItems;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.client.model.ModelNpcAlt;
import noppes.npcs.client.model.part.LayerModel;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityCustomNpc;

import javax.annotation.Nonnull;
import java.util.List;

public class LayerCustomModels<T extends EntityLivingBase> extends LayerInterface<T> {

    private static BlockRendererDispatcher dispatcher;

    public LayerCustomModels(RenderLiving<?> render) { super(render); }

    @Override
    public void doRenderLayer(@Nonnull EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        npc = (EntityCustomNpc) entity;
        if (npc.isInvisibleToPlayer(Minecraft.getMinecraft().player)) { return; }
        playerdata = npc.modelData;
        if (!(render.getMainModel() instanceof ModelBiped)) { return; }
        model = (ModelBiped) render.getMainModel();

        Minecraft mc = Minecraft.getMinecraft();
        boolean isInvisible = false;
        if (npc.display.getVisible() == 1) { isInvisible = npc.display.getAvailability().isAvailable(Minecraft.getMinecraft().player); }
        else if (npc.display.getVisible() == 2) { isInvisible = Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() != CustomItems.wand; }
        float red, green, blue;
        if (!npc.animation.isAnimated(AnimationKind.DIES) && npc.hurtTime > 0 || npc.deathTime > 0) {
            red = 1.0f;
            green = 0.0f;
            blue = 0.0f;
        }
        else {
            int color = npc.display.getTint();
            red = (color >> 16 & 0xFF) / 255.0f;
            green = (color >> 8 & 0xFF) / 255.0f;
            blue = (color & 0xFF) / 255.0f;
        }
        GlStateManager.pushMatrix();
        if (npc.isSneaking()) { GlStateManager.translate(0.0f, 0.2f, 0.0f); }
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        if (isInvisible) {
            GlStateManager.color(1.0f, 1.0f, 1.0f, 0.15f);
            GlStateManager.blendFunc(770, 771);
            GlStateManager.alphaFunc(516, 0.003921569f);
        }

        renderPart(model.bipedHead, playerdata.getRenderLayers(EnumParts.HEAD), mc, red, green, blue);
        renderPart(model.bipedBody, playerdata.getRenderLayers(EnumParts.BODY), mc, red, green, blue);
        renderPart(model.bipedRightArm, playerdata.getRenderLayers(EnumParts.ARM_RIGHT), mc, red, green, blue);
        renderPart(model.bipedLeftArm, playerdata.getRenderLayers(EnumParts.ARM_LEFT), mc, red, green, blue);
        renderPart(model.bipedRightLeg, playerdata.getRenderLayers(EnumParts.LEG_RIGHT), mc, red, green, blue);
        renderPart(model.bipedLeftLeg, playerdata.getRenderLayers(EnumParts.LEG_LEFT), mc, red, green, blue);
        renderPartNext(2, null, playerdata.getRenderLayers(EnumParts.BELT), mc, red, green, blue);
        renderPartNext(0, EnumHandSide.RIGHT, playerdata.getRenderLayers(EnumParts.WRIST_RIGHT), mc, red, green, blue);
        renderPartNext(0, EnumHandSide.LEFT, playerdata.getRenderLayers(EnumParts.WRIST_LEFT), mc, red, green, blue);
        renderPartNext(1, EnumHandSide.RIGHT, playerdata.getRenderLayers(EnumParts.FEET_RIGHT), mc, red, green, blue);
        renderPartNext(1, EnumHandSide.LEFT, playerdata.getRenderLayers(EnumParts.FEET_LEFT), mc, red, green, blue);

        if (isInvisible) {
            GlStateManager.disableBlend();
            GlStateManager.alphaFunc(516, 0.1f);
            GlStateManager.depthMask(true);
        }

        GlStateManager.popMatrix();
    }

    @Override
    public void render(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) { }

    @Override
    public void rotate(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) { }

    @Override
    public boolean shouldCombineTextures() {
        return true;
    }

    public void preRender(float red, float green, float blue) {
        if (dispatcher == null) { dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher(); }

        if (!npc.animation.isAnimated(AnimationKind.DIES) && npc.hurtTime > 0 || npc.deathTime > 0) { return; }

        boolean isInvisible = false;
        if (npc.display.getVisible() == 1) { isInvisible = npc.display.getAvailability().isAvailable(Minecraft.getMinecraft().player); }
        else if (npc.display.getVisible() == 2) { isInvisible = Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() != CustomItems.wand; }
        if (isInvisible) {
            GlStateManager.color(red, green, blue, 0.15f);
            GlStateManager.enableBlend();
        } else {
            GlStateManager.color(red, green, blue, 1.0f);
            GlStateManager.disableBlend();
        }
    }

    @SuppressWarnings("all")
    private void renderPart(ModelRenderer modelRenderer, List<LayerModel> layers, Minecraft mc, float red, float green, float blue) {
        if (layers.isEmpty()) { return; }
        GlStateManager.pushMatrix();
        modelRenderer.postRender(0.0625f);

        for (LayerModel lm : layers) {
            GlStateManager.pushMatrix();
            // rotate
            GlStateManager.translate(lm.offset[0], lm.offset[1], lm.offset[2]);
            GlStateManager.rotate(180.0f, 1.0f, 0.0f, 0.0f);
            if (lm.rotation[1] != 0.0f) { GlStateManager.rotate(lm.rotation[1], 0.0f, 1.0f, 0.0f); }
            if (lm.rotation[0] != 0.0f) { GlStateManager.rotate(lm.rotation[0], 1.0f, 0.0f, 0.0f); }
            if (lm.rotation[2] != 0.0f) { GlStateManager.rotate(lm.rotation[2], 0.0f, 0.0f, 1.0f); }
            if (lm.scale[0] != 0.0f || lm.scale[1] != 0.0f || lm.scale[2] != 0.0f) {
                GlStateManager.scale(lm.scale[0], lm.scale[1], lm.scale[2]);
            }
            // render
            preRender(red, green, blue);
            GlStateManager.enableRescaleNormal();
            if (lm.getOBJ() != null) { ModelBuffer.render(lm.getOBJ(), null, null, 0, true); }
            else {
                boolean isRender = true;
                Block block = Block.getBlockFromItem(lm.getStack().getItem());
                if (block != Blocks.AIR) {
                    GlStateManager.scale(0.5f, 0.5f,0.5f);
                    IBlockState state = block.getStateFromMeta(lm.getStack().getMetadata());
                    if (!block.hasTileEntity(state)) {
                        dispatcher.renderBlockBrightness(state, 1.0f);
                        isRender = false;
                    }
                }
                if (isRender) {
                    mc.getRenderItem().renderItem(lm.getStack(), ItemCameraTransforms.TransformType.FIXED);
                }
            }
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
    }

    @SuppressWarnings("all")
    private void renderPartNext(int modelType, EnumHandSide handSide, List<LayerModel> layers, Minecraft mc, float red, float green, float blue) {
        if (layers.isEmpty()) { return; }
        GlStateManager.pushMatrix();
        if (modelType == 1) {
            if (model instanceof ModelNpcAlt) { ((ModelNpcAlt) model).postRenderLeg(0.0625f, handSide); }
            else { (handSide == EnumHandSide.LEFT ? model.bipedLeftLeg : model.bipedRightLeg).postRender(0.0625f); }
        } // Legs
        else if (modelType == 2) {
            if (model instanceof ModelNpcAlt) { ((ModelNpcAlt) model).postRenderBelt(0.0625f); }
            else { model.bipedBody.postRender(0.0625f); }
        } // Belt
        else {
            model.postRenderArm(0.0625f, handSide);
        }
        GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(handSide == null ? 0.0f : (handSide == EnumHandSide.LEFT ? -0.0625f : 0.0625f), 0.125f, -0.625f);

        for (LayerModel lm : layers) {
            GlStateManager.pushMatrix();
            // rotate
            GlStateManager.translate(lm.offset[0], lm.offset[1], lm.offset[2]);
            GlStateManager.rotate(90.0f, 1.0f, 0.0f, 0.0f);
            if (lm.rotation[1] != 0.0f) { GlStateManager.rotate(lm.rotation[1], 0.0f, 1.0f, 0.0f); }
            if (lm.rotation[0] != 0.0f) { GlStateManager.rotate(lm.rotation[0], 1.0f, 0.0f, 0.0f); }
            if (lm.rotation[2] != 0.0f) { GlStateManager.rotate(lm.rotation[2], 0.0f, 0.0f, 1.0f); }

            if (lm.scale[0] != 0.0f || lm.scale[1] != 0.0f || lm.scale[2] != 0.0f) {
                GlStateManager.scale(lm.scale[0], lm.scale[1], lm.scale[2]);
            }
            // render
            preRender(red, green, blue);
            GlStateManager.enableRescaleNormal();
            if (lm.getOBJ() != null) { ModelBuffer.render(lm.getOBJ(), null, null, 0, true); }
            else {
                Block block = Block.getBlockFromItem(lm.getStack().getItem());
                if (block != Blocks.AIR) {
                    GlStateManager.scale(0.5f, 0.5f,0.5f);
                    dispatcher.renderBlockBrightness(block.getStateFromMeta(lm.getStack().getMetadata()), 1.0f);
                }
                else {
                    mc.getRenderItem().renderItem(lm.getStack(), ItemCameraTransforms.TransformType.FIXED);
                }
            }
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
    }

}
