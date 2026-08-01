package noppes.npcs.client.layer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import noppes.npcs.CustomItems;
import noppes.npcs.client.parts.ModelPartData;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.client.model.part.ModelData;
import noppes.npcs.entity.EntityCustomNpc;

import javax.annotation.Nonnull;

public abstract class LayerInterface<T extends EntityLivingBase>
implements LayerRenderer<T> {

	public ModelBiped model;
	protected EntityCustomNpc npc;
	protected ModelData playerdata;
	protected RenderLiving<?> render;

	public LayerInterface(RenderLiving<?> renderIn) {
		render = renderIn;
		model = (ModelBiped) render.getMainModel();
	}

	private int blend(int color1, int color2) {
        int aR = (color1 & 0xFF0000) >> 16;
		int aG = (color1 & 0xFF00) >> 8;
		int aB = color1 & 0xFF;
		int bR = (color2 & 0xFF0000) >> 16;
		int bG = (color2 & 0xFF00) >> 8;
		int bB = color2 & 0xFF;
		int R = (int) (aR + (bR - aR) * (float) 0.5);
		int G = (int) (aG + (bG - aG) * (float) 0.5);
		int B = (int) (aB + (bB - aB) * (float) 0.5);
		return R << 16 | G << 8 | B;
	}

	public void doRenderLayer(@Nonnull EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
							  float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		npc = (EntityCustomNpc) entity;
		if (npc.isInvisibleToPlayer(Minecraft.getMinecraft().player)) { return; }
		playerdata = npc.modelData;
		if (!(render.getMainModel() instanceof ModelBiped)) { return; }
		model = (ModelBiped) render.getMainModel();
		rotate(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
		GlStateManager.pushMatrix();
		if (entity.isInvisible()) {
			GlStateManager.color(1.0f, 1.0f, 1.0f, 0.15f);
			GlStateManager.enableBlend();
			GlStateManager.blendFunc(770, 771);
			GlStateManager.alphaFunc(516, 0.003921569f);
		}
		if (!npc.animation.isAnimated(AnimationKind.DIES) && npc.hurtTime > 0 || npc.deathTime > 0) {
			GlStateManager.color(1.0f, 0.0f, 0.0f, 0.3f);
		}
		if (npc.isSneaking()) { GlStateManager.translate(0.0f, 0.2f, 0.0f); }
		GlStateManager.enableRescaleNormal();
		render(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		if (entity.isInvisible()) {
			GlStateManager.disableBlend();
			GlStateManager.alphaFunc(516, 0.1f);
			GlStateManager.depthMask(true);
		}
		GlStateManager.popMatrix();
	}

	public void preRender(ModelPartData data) {
		if (data == null) { return; }
		if (data.playerTexture) { Minecraft.getMinecraft().getTextureManager().bindTexture(npc.textureLocation); }
		else { Minecraft.getMinecraft().getTextureManager().bindTexture(data.getResource()); }
		if (!npc.animation.isAnimated(AnimationKind.DIES) && npc.hurtTime > 0 || npc.deathTime > 0) { return; }

		int color = data.color;
		if (npc.display.getTint() != 16777215) {
			if (data.color != 16777215) { color = blend(data.color, npc.display.getTint()); }
			else { color = npc.display.getTint(); }
		}
		float red = (color >> 16 & 0xFF) / 255.0f;
		float green = (color >> 8 & 0xFF) / 255.0f;
		float blue = (color & 0xFF) / 255.0f;

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

	public abstract void render(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale);

	public abstract void rotate(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale);

	public void setColor(ModelPartData ignoredData, EntityLivingBase ignoredEntity) {
	}

	public void setRotation(ModelRenderer model, float x, float y, float z) {
		model.rotateAngleX = x;
		model.rotateAngleY = y;
		model.rotateAngleZ = z;
	}

	public boolean shouldCombineTextures() { return false; }

}
