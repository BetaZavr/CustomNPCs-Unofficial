package noppes.npcs.client.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import moe.plushie.armourers_workshop.api.ArmourersWorkshopApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.NPCRendererHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomItems;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.client.layer.*;
import noppes.npcs.client.model.ModelNpcAlt;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.controllers.PixelmonHelper;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

public class RenderCustomNpc<T extends EntityCustomNpc> extends RenderNPCInterface<T> {

	private EntityLivingBase entity;
	public ModelBiped npcmodel;
	private float partialTicks;
	private RenderLivingBase<EntityLivingBase> renderEntity;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public RenderCustomNpc(ModelBiped model) {
		super(model, 0.5f);
		npcmodel = (ModelBiped) mainModel;
		addLayer(new LayerEyes(this));
		addLayer(new LayerHead(this));
		addLayer(new LayerArms(this));
		addLayer(new LayerLegs(this));
		addLayer(new LayerBody(this));
		addLayer(new LayerGlow<>(this));
		addLayer(new LayerNpcCloak(this));
		addLayer(new LayerCustomModels(this));
		addLayer(new LayerCustomHead(npcmodel.bipedHead));
		addLayer(new LayerCustomHeldItem(this));
		boolean smallArmsIn = model instanceof ModelNpcAlt && ((ModelNpcAlt) model).smallArmsIn;
		boolean isClassicPlayer = model instanceof ModelNpcAlt && ((ModelNpcAlt) model).isClassicPlayer;
		addLayer(new LayerCustomArmor(this, false, smallArmsIn, isClassicPlayer));
	}

	@Override
	protected void applyRotations(@Nonnull T npc, float handleRotation, float rotationYaw, float partialTicks) {
		if (renderEntity != null && !(renderEntity instanceof RenderCustomNpc)) {
			NPCRendererHelper.applyRotations(renderEntity, entity, handleRotation, rotationYaw, partialTicks);
			return;
		}
		if (npc.isEntityAlive()) {
			super.applyRotations(npc, handleRotation, rotationYaw, partialTicks);
			return;
		}
		GlStateManager.rotate(180.0F - rotationYaw, 0.0F, 1.0F, 0.0F);
		if (npc.deathTime > 0) {
			if (npc.animation.hasAnimation(AnimationKind.DIES)) {
				if (!npc.animation.isAnimated(AnimationKind.DIES)) { npc.animation.tryRunAnimation(AnimationKind.DIES); }
				return;
			}
			float f = ((float) npc.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
			f = MathHelper.sqrt(f);
			if (f > 1.0F) {
				f = 1.0F;
			}
			GlStateManager.rotate(f * getDeathMaxRotation(npc), 0.0F, 0.0F, 1.0F);
		} else {
			String s = TextFormatting.getTextWithoutFormattingCodes(npc.getName());
			if (("Dinnerbone".equals(s) || "Grumm".equals(s))) {
				GlStateManager.translate(0.0F, npc.height + 0.1F, 0.0F);
				GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void doRender(@Nonnull T npc, double d, double d1, double d2, float f, float partialTicksIn) {
		partialTicks = partialTicksIn;
		entity = npc.modelData.getEntity(npc);
		if (entity != null) {
			Render<?> render = renderManager.getEntityRenderObject(entity);
			if (render instanceof RenderLivingBase) {
				renderEntity = (RenderLivingBase<EntityLivingBase>) render;
			} else {
				renderEntity = null;
				entity = null;
			}
		}
		else {
			renderEntity = null;
			List<LayerRenderer<T>> list = layerRenderers;
			for (LayerRenderer<T> layer : list) {
				if (layer instanceof LayerPreRender) { ((LayerPreRender)layer).preRender(npc); }
			}
		}
		npcmodel.rightArmPose = getPose(npc, npc.getHeldItemMainhand());
		npcmodel.leftArmPose = getPose(npc, npc.getHeldItemOffhand());
		super.doRender(npc, d, d1, d2, f, partialTicks);
	}

	public ModelBiped.ArmPose getPose(T npc, ItemStack item) {
		if (NoppesUtilServer.isItemStackNull(item)) {
			return ModelBiped.ArmPose.EMPTY;
		}
		if (npc.getItemInUseCount() > 0) {
			EnumAction enumaction = item.getItemUseAction();
			if (enumaction == EnumAction.BLOCK) {
				return ModelBiped.ArmPose.BLOCK;
			}
			if (enumaction == EnumAction.BOW) {
				return ModelBiped.ArmPose.BOW_AND_ARROW;
			}
		}
		return ModelBiped.ArmPose.ITEM;
	}

	@Override
	protected float handleRotationFloat(@Nonnull T par1EntityLivingBase, float partialTicks) {
		if (renderEntity != null) {
			return NPCRendererHelper.handleRotationFloat(entity, partialTicks, renderEntity);
		}
		return super.handleRotationFloat(par1EntityLivingBase, partialTicks);
	}

	@Override
	protected void preRenderCallback(@Nonnull T npc, float f) {
		if (renderEntity != null) {
			//renderColor(npc);
			int size = npc.display.getSize();
			if (entity instanceof EntityNPCInterface) {
				((EntityNPCInterface) entity).display.setSize(5);
			}
			NPCRendererHelper.preRenderCallback(entity, f, renderEntity);
			npc.display.setSize(size);
			GlStateManager.scale(0.2f * npc.display.getSize(), 0.2f * npc.display.getSize(), 0.2f * npc.display.getSize());
		} else {
			super.preRenderCallback(npc, f);
		}
	}

	@Override
	protected void renderLayers(@Nonnull T npc, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scaleIn) {
		if (entity != null && renderEntity != null) {
			NPCRendererHelper.drawLayers(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scaleIn, renderEntity);
		} else {
			Map<EnumParts, Boolean> sp = npc.animation.showParts;
			for (LayerRenderer<T> layerrenderer : layerRenderers) {
				String layerName = layerrenderer.getClass().getSimpleName();
				if (npc.modelData.isDisableLayer(layerName)) { continue; }
				if ((layerrenderer instanceof LayerEyes || layerrenderer instanceof LayerHead || layerName.equals("LayerCustomHead")) && !sp.get(EnumParts.HEAD)) { continue; }
				if (ArmourersWorkshopApi.isAvailable() && layerName.equals("LayerCustomHead") && ArmourersWorkshopApi.getSkinNBTUtils().hasSkinDescriptor(npc.getItemStackFromSlot(EntityEquipmentSlot.HEAD))) { continue; }
				if ((layerrenderer instanceof LayerBody || layerrenderer instanceof LayerNpcCloak) && !sp.get(EnumParts.BODY)) { continue; }
				if (layerName.equals("SkinLayerRendererCustomNPC")) { continue; }
				boolean flag = setBrightness(npc, partialTicks, layerrenderer.shouldCombineTextures());
				layerrenderer.doRenderLayer(npc, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scaleIn);
				if (flag) { unsetBrightness(); }
			}
		}
	}

	@Override
	protected void renderModel(@Nonnull T npc, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
		if (renderEntity != null) {
			boolean isTransparent = npc.isInvisible();
			Minecraft mc = Minecraft.getMinecraft();
			if (npc.display.getVisible() == 1) { isTransparent = npc.display.getAvailability().isAvailable(mc.player); }
			else if (npc.display.getVisible() == 2) { isTransparent = mc.player.getHeldItemMainhand().getItem() != CustomItems.wand; }
			boolean wasBlendEnabled = false;
			if (isTransparent) {
				wasBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
				GlStateManager.enableBlend();
				GlStateManager.tryBlendFuncSeparate(
						GlStateManager.SourceFactor.SRC_ALPHA,
						GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
						GlStateManager.SourceFactor.ONE,
						GlStateManager.DestFactor.ZERO
				);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 0.3f);
				GlStateManager.depthMask(false);
			}

			try {
				ModelBase model = renderEntity.getMainModel();
				if (PixelmonHelper.isPixelmon(entity)) {
					ModelBase pixModel = (ModelBase) PixelmonHelper.getModel(entity);
					if (pixModel != null) {
						model = pixModel;
						PixelmonHelper.setupModel(entity, pixModel);
					}
				}
				model.swingProgress = mainModel.swingProgress;
				model.isRiding = (entity.isRiding() && entity.getRidingEntity() != null && entity.getRidingEntity().shouldRiderSit());
				model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
				model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity);
				model.isChild = entity.isChild();
				NPCRendererHelper.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, renderEntity, model, Objects.requireNonNull(getEntityTexture(npc)));
				if (!npc.display.getOverlayTexture().isEmpty()) {
					GlStateManager.depthFunc(515);
					if (npc.textureGlowLocation == null) { npc.textureGlowLocation = new ResourceLocation(npc.display.getOverlayTexture()); }
					float f1 = 1.0f;
					GlStateManager.enableBlend();
					GlStateManager.blendFunc(1, 1);
					GlStateManager.disableLighting();
					GlStateManager.depthMask(!npc.isInvisible());
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
					GlStateManager.pushMatrix();
					GlStateManager.scale(1.001f, 1.001f, 1.001f);
					NPCRendererHelper.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, renderEntity, model, npc.textureGlowLocation);
					GlStateManager.popMatrix();
					GlStateManager.enableLighting();
					GlStateManager.color(1.0f, 1.0f, 1.0f, f1);
					GlStateManager.depthFunc(515);
					GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
					GlStateManager.disableBlend();
				}
			}
			finally {
				if (isTransparent) {
					GlStateManager.depthMask(true);
					if (!wasBlendEnabled) { GlStateManager.disableBlend(); }
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				}
			}
		}
		else { super.renderModel(npc, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor); }
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setLightmap(@Nonnull EntityCustomNpc npc) { super.setLightmap((T) npc); }

	public List<LayerRenderer<T>> getLayers() { return new ArrayList<>(layerRenderers); }

}
