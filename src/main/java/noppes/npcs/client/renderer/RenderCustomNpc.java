package noppes.npcs.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.layer.LayerGlow;
import noppes.npcs.client.layer.LayerHeadwear;
import noppes.npcs.client.layer.LayerNpcCloak;
import noppes.npcs.client.layer.LayerParts;
import noppes.npcs.client.layer.LayerPreRender;
import noppes.npcs.client.layer.LayerNpcElytra;
import noppes.npcs.client.parts.ModelData;
import noppes.npcs.constants.BodyPart;
import noppes.npcs.controllers.CobblemonHelper;
import noppes.npcs.controllers.PixelmonHelper;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.client.renderer.entity.ILivingRendererMixin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RenderCustomNpc<T extends EntityCustomNpc, M extends HumanoidModel<T>>
		extends RenderNPCInterface<T, M> {

	private float partialTicks;
	private LivingEntity entity;
	private @Nullable T npc;
	public M npcModel;
	public Model otherModel;
	public HumanoidArmorLayer<T, M, M> armorLayer;

	@SuppressWarnings("rawtypes")
	public List npcLayers = new ArrayList<>();

	@SuppressWarnings("rawtypes")
	private LivingEntityRenderer renderEntity;

	@SuppressWarnings({"rawtypes", "unchecked"})
	private final RenderLayer customRenderLayer = new RenderLayer(this) {
		public void render(@Nonnull PoseStack mStack, @Nonnull MultiBufferSource typeBuffer, int lightMapUV, @Nonnull Entity entityIn,
						   float limbSwing, float limbSwingAmount, float partialTicks, float age, float netHeadYaw, float headPitch) {
			for (Object o : ((ILivingRendererMixin) renderEntity).getLayers()) {
				((RenderLayer) o).render(mStack, typeBuffer, lightMapUV, entity, limbSwing, limbSwingAmount, partialTicks, age, netHeadYaw, headPitch);
			}
		}
	};
	private final HumanoidModel<T> renderModel;

	@SuppressWarnings("unchecked")
	public RenderCustomNpc(Context manager, M model) {
		super(manager, model, 0.5F);
		npcModel = model;
		addLayer(new CustomHeadLayer<>(this, manager.getModelSet(), manager.getItemInHandRenderer()));
		addLayer(new LayerHeadwear<>(this));
		addLayer(new LayerNpcCloak<>(this));
		addLayer(new LayerNpcElytra<>(manager, this));
		addLayer(new LayerParts<>(this));
		addLayer(new ItemInHandLayer<>(this, manager.getItemInHandRenderer()));
		addLayer(new LayerGlow<>(this));
		armorLayer = new HumanoidArmorLayer<>(this,
				(M) new HumanoidModel<T>(manager.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
				(M) new HumanoidModel<T>(manager.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
				manager.getModelManager());
		addLayer(armorLayer);
		renderModel = new HumanoidModel<>(manager.bakeLayer(ModelLayers.PLAYER)) {

			@Override
			public void renderToBuffer(@Nonnull PoseStack mStack, @Nonnull VertexConsumer iVertex, int lightMapUV, int packedOverlayIn, float red, float green, float blue, float alpha) {
				if (npc != null) {
					int color = npc.display.getTint();
					if (color < new Color(0xFFFFFF).getRGB()) {
						red = (float)(color >> 16 & 255) / 255.0F;
						green = (float)(color >> 8 & 255) / 255.0F;
						blue = (float)(color & 255) / 255.0F;
					}
				}
				otherModel.renderToBuffer(mStack, iVertex, lightMapUV, packedOverlayIn, red, green, blue, alpha);
			}

			@Override
			@SuppressWarnings("rawtypes")
			public void setupAnim(@Nonnull T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				if (otherModel instanceof EntityModel em) {
					em.setupAnim(entity, limbSwing, limbSwingAmount, ((ILivingRendererMixin) renderEntity).callGetBob(entity, Minecraft.getInstance().getPartialTick()), netHeadYaw, headPitch);
				}
			}

			@Override
			@SuppressWarnings("rawtypes")
			public void prepareMobModel(@Nonnull T npc, float animationPos, float animationSpeed, float partialTicks) {
				if (PixelmonHelper.isPixelmon(entity)) {
					Model pixModel = (Model)PixelmonHelper.getModel(entity);
					if (pixModel != null) {
						otherModel = pixModel;
						PixelmonHelper.setupModel(entity, pixModel);
					}
				}
				if (otherModel instanceof HumanoidModel) {
					HumanoidModel<T> bm = (HumanoidModel<T>)otherModel;
					bm.swimAmount = npc.getSwimAmount(partialTicks);
					bm.crouching = npcModel.crouching;
				}
				if (otherModel instanceof EntityModel em) {
					em.riding = entity.isPassenger() && entity.getVehicle() != null && entity.getVehicle().shouldRiderSit();
					em.young = entity.isBaby();
					em.attackTime = getAttackAnim(npc, partialTicks);
					em.prepareMobModel(entity, animationPos, animationSpeed, partialTicks);
				}
			}
		};
	}

	void hideParts() {
		if (npc instanceof EntityCustomNpc) {
			ModelData data = ModelData.get(npc);
			npcModel.leftLeg.visible = !data.hiddenParts.contains(BodyPart.LEFT_LEG);
			npcModel.rightLeg.visible = !data.hiddenParts.contains(BodyPart.RIGHT_LEG);
			npcModel.leftArm.visible = !data.hiddenParts.contains(BodyPart.LEFT_ARM);
			npcModel.rightArm.visible = !data.hiddenParts.contains(BodyPart.RIGHT_ARM);
			npcModel.body.visible = !data.hiddenParts.contains(BodyPart.BODY);
			npcModel.head.visible = !data.hiddenParts.contains(BodyPart.HEAD);
			npcModel.hat.visible = !data.hiddenParts.contains(BodyPart.HEAD);
			if (npcModel instanceof PlayerModel) {
				@SuppressWarnings("unchecked")
				PlayerModel<T> playerModel = (PlayerModel<T>) npcModel;
				playerModel.jacket.visible = !data.hiddenParts.contains(BodyPart.BODY);
				playerModel.leftSleeve.visible = !data.hiddenParts.contains(BodyPart.LEFT_ARM);
				playerModel.rightSleeve.visible = !data.hiddenParts.contains(BodyPart.RIGHT_ARM);
				playerModel.leftPants.visible = !data.hiddenParts.contains(BodyPart.LEFT_LEG);
				playerModel.rightPants.visible = !data.hiddenParts.contains(BodyPart.RIGHT_LEG);
			}
		}

	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void render(@Nullable T npcIn, float entityYaw, float partialTicksIn, @Nonnull PoseStack matrixStack, @Nonnull MultiBufferSource buffer, int packedLight) {
		if (npcIn == null) { return; }
		npc = npcIn;
		if (CustomNpcs.EnableInvisibleNpcs && CustomNpcs.InvisibilityAlgorithm > 0) {
			Player player = Minecraft.getInstance().player;
			if (player != null &&
					!npc.display.isVisibleTo(player) &&
					!player.isSpectator() &&
					player.getMainHandItem().getItem() != CustomItems.wand) { return; }
		}
		partialTicks = partialTicksIn;
		Entity prevEntity = entity;
		entity = npc.modelData.getEntity(npc);
		if (prevEntity != null && entity == null) {
			model = npcModel;
			renderEntity = null;
			layers.clear();
			layers.addAll(npcLayers);
		}
		Iterator<RenderLayer<T, M>> var9;
		if (entity != null) {
			EntityRenderer<? super LivingEntity> render = entityRenderDispatcher.getRenderer(entity);
			if (npc.modelData.simpleRender) {
				renderEntity = null;
				matrixStack.pushPose();
				render.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
				renderNameTag(npc, Component.empty(), matrixStack, buffer, packedLight);
				matrixStack.popPose();
				return;
			}
			if (render instanceof LivingEntityRenderer) {
				renderEntity = (LivingEntityRenderer) render;
				otherModel = renderEntity.getModel();
				if (CobblemonHelper.Enabled && CobblemonHelper.isPokemon(entity)) { otherModel = CobblemonHelper.getPokemonModel(entity); }
				model = (M) renderModel;
				layers.clear();
				layers.add(customRenderLayer);
				layers.add(new LayerGlow<>(this));
				if (render instanceof RenderCustomNpc) {
					List<RenderLayer<T, M>> listLayers = ((ILivingRendererMixin) renderEntity).getLayers();
					for (RenderLayer<T, M> layer : new ArrayList<>(listLayers)) {
						if (layer instanceof LayerPreRender) {
							((LayerPreRender)layer).preRender((EntityCustomNpc) entity);
						}
					}
				}
			} else {
				renderEntity = null;
				entity = null;
				model = npcModel;
				layers.clear();
				layers.addAll(npcLayers);
			}
		} else {
			hideParts();
			var9 = layers.iterator();
			while(var9.hasNext()) {
				RenderLayer<T, M> layer = var9.next();
				if (layer instanceof LayerPreRender) {
					((LayerPreRender)layer).preRender(npc);
				}
			}
		}
		npcModel.rightArmPose = getPose(npc, npc.getMainHandItem());
		npcModel.leftArmPose = getPose(npc, npc.getOffhandItem());
		super.render(npc, entityYaw, partialTicks, matrixStack, buffer, packedLight);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	protected RenderType getRenderType(@Nonnull T entityIn, boolean isNorman, boolean isTranslucent, boolean isOutline) {
		ResourceLocation resourcelocation = getTextureLocation(entityIn);
		if (isNorman && model == renderModel) {
			return otherModel.renderType(resourcelocation);
		} else {
			return entity == null ? model.renderType(resourcelocation) : super.getRenderType(entityIn, isNorman, isTranslucent, isOutline);
		}
	}

	public ArmPose getPose(T npc, ItemStack item) {
		if (NoppesUtilServer.isItemStackNull(item)) {
			return ArmPose.EMPTY;
		} else {
			if (npc.getUseItemRemainingTicks() > 0) {
				UseAnim enumAction = item.getUseAnimation();
				if (enumAction == UseAnim.BLOCK) {
					return ArmPose.BLOCK;
				}
				if (enumAction == UseAnim.BOW) {
					return ArmPose.BOW_AND_ARROW;
				}
			}
			return ArmPose.ITEM;
		}
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected void scale(@Nonnull T npcIn, @Nonnull PoseStack matrixScale, float f) {
		if (renderEntity != null) {
			renderColor(npcIn);
			float size = npcIn.display.getSize();
			if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface)entity).display.setSize(5); }
			EntityRenderer<? super LivingEntity> render = entityRenderDispatcher.getRenderer(entity);
			if (!npcIn.modelData.simpleRender && render instanceof LivingEntityRenderer standardRender) {
				// Prevent infinite recursion when the rendered entity uses RenderCustomNpc
				if (standardRender instanceof RenderCustomNpc) {
					super.scale(npcIn, matrixScale, f);
				} else {
					matrixScale.pushPose();
					((ILivingRendererMixin) standardRender).callScale(entity, matrixScale, partialTicks);
					if (matrixScale.last().normal().isFinite()) {
						matrixScale.popPose();
						((ILivingRendererMixin) standardRender).callScale(entity, matrixScale, partialTicks);
					} else {
						matrixScale.popPose();
					}
				}
			}
			npcIn.display.setSize(size);
			size *= 0.2F;
			matrixScale.scale(size, size, size);
		} else {
			super.scale(npcIn, matrixScale, f);
		}
	}

}
