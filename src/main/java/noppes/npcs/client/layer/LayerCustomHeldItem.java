package noppes.npcs.client.layer;

import moe.plushie.armourers_workshop.api.ArmourersWorkshopApi;
import moe.plushie.armourers_workshop.api.common.capability.IEntitySkinCapability;
import moe.plushie.armourers_workshop.api.common.capability.IWardrobeCap;
import moe.plushie.armourers_workshop.api.common.skin.data.ISkin;
import moe.plushie.armourers_workshop.api.common.skin.data.ISkinDescriptor;
import moe.plushie.armourers_workshop.api.common.skin.data.ISkinDye;
import moe.plushie.armourers_workshop.api.common.skin.type.ISkinType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.mixin.client.renderer.entity.IRenderLivingBaseMixin;
import noppes.npcs.client.model.ModelBipedAlt;
import noppes.npcs.client.model.ModelNpcAlt;
import noppes.npcs.client.model.ModelRendererAlt;
import noppes.npcs.client.util.aw.ArmourersWorkshopUtil;
import noppes.npcs.client.util.aw.CustomSkinModelRenderHelper;
import noppes.npcs.constants.EnumParts;

@SideOnly(Side.CLIENT)
public class LayerCustomHeldItem<T extends EntityLivingBase> extends LayerInterface<T> {

	public LayerCustomHeldItem(RenderLiving<?> livingEntityRendererIn) {
		super(livingEntityRendererIn);
	}

	@Override
	public void render(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		boolean flag = npc.getPrimaryHand() == EnumHandSide.RIGHT;
		ItemStack mainhand = flag ? npc.getHeldItemMainhand() : npc.getHeldItemOffhand();
		ItemStack offhand = flag ? npc.getHeldItemOffhand() : npc.getHeldItemMainhand();
		boolean showMain = npc.animation.showParts.get(flag ? EnumParts.ARM_RIGHT : EnumParts.ARM_LEFT);
		boolean showOff = npc.animation.showParts.get(flag ? EnumParts.ARM_LEFT : EnumParts.ARM_RIGHT);
		boolean isAWShow = ArmourersWorkshopApi.isAvailable();
		if (!isAWShow && mainhand.isEmpty() && offhand.isEmpty()) { return; }
		boolean isAnimated = npc.animation.isAnimated();
		if (isAnimated) {
			mainhand = npc.animation.getCurrentHeldStack(flag);
			offhand = npc.animation.getCurrentHeldStack(!flag);
		}
		double distance = Minecraft.getMinecraft().player.getDistance(npc.posX, npc.posY, npc.posZ);
		IEntitySkinCapability skinCapability = null;
		if (isAWShow) {
			try {
				Object value = ArmourersWorkshopUtil.getInstance().renderDistanceSkin.get(ArmourersWorkshopUtil.getInstance().configHandlerClient);
				if (value instanceof Integer) { isAWShow = distance <= (int) value; }
				else { isAWShow = distance <= (double) value; }
				if (isAWShow) { skinCapability = ArmourersWorkshopApi.getEntitySkinCapability(npc); }
			}
			catch (Exception ignored) { }
		}
		GlStateManager.pushMatrix();
		if (npc.isChild()) {
			GlStateManager.translate(0.0F, 0.75F, 0.0F);
			GlStateManager.scale(0.5F, 0.5F, 0.5F);
		}
		// ArmourersWorkshop add to skin
		if (skinCapability != null) {
			for (ISkinType skinType : skinCapability.getValidSkinTypes()) {
				if (skinType.getName().equals("Sword") || skinType.getName().equals("bow") || skinType.getName().equals("Shield")) {
					IInventory inv = ArmourersWorkshopUtil.getInstance().getSkinTypeInv(skinCapability.getSkinInventoryContainer(), skinType);
					for (int j = 0; j < inv.getSizeInventory(); j++) {
						if (ArmourersWorkshopApi.getSkinNBTUtils().hasSkinDescriptor(inv.getStackInSlot(j))) {
							EnumHandSide side = flag ? EnumHandSide.RIGHT : EnumHandSide.LEFT;
							boolean show;
							if (skinType.getName().equals("Shield")) {
								side = flag ? EnumHandSide.LEFT : EnumHandSide.RIGHT;
								if (flag) { showOff = false; }
								else { showMain = false; }
								show = !offhand.isEmpty();
							}
							else {
								if (flag) { showMain = false; } else { showOff = false; }
								show = !mainhand.isEmpty();
							}
							if (show) { renderHeldAWItem(ArmourersWorkshopApi.getSkinNBTUtils().getSkinDescriptor(inv.getStackInSlot(j)), npc.isSneaking(), side, distance, scale); }
						}
					}
				}
			}
		}
		// mainhand
		if (!showMain) {
			EnumParts ep = flag ? EnumParts.ARM_RIGHT : EnumParts.ARM_LEFT;
			EnumParts es = flag ? EnumParts.RIGHT_STACK : EnumParts.LEFT_STACK;
			if (ModelNpcAlt.editAnimDataSelect.isNPC && ModelNpcAlt.editAnimDataSelect.part == ep.patterns || ModelNpcAlt.editAnimDataSelect.part == es.patterns) {
				showMain = true;
			}
		}
		if (showMain) {
			if (model instanceof ModelBipedAlt) { showMain = flag ? ((ModelBipedAlt) model).rightStackData.showModel : ((ModelBipedAlt) model).leftStackData.showModel; }
			else if (model instanceof ModelNpcAlt) { showMain = flag ? ((ModelNpcAlt) model).rightStackData.showModel : ((ModelNpcAlt) model).leftStackData.showModel; }
			if (showMain) {
				renderHeldItem(npc, mainhand, ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, EnumHandSide.RIGHT, scale, distance, isAWShow, isAnimated);
			}
		}
		// offhand
		if (!showOff) {
			EnumParts ep = flag ? EnumParts.ARM_LEFT : EnumParts.ARM_RIGHT;
			EnumParts es = flag ? EnumParts.LEFT_STACK : EnumParts.RIGHT_STACK;
			if (ModelNpcAlt.editAnimDataSelect.isNPC && ModelNpcAlt.editAnimDataSelect.part == ep.patterns || ModelNpcAlt.editAnimDataSelect.part == es.patterns) {
				showOff = true;
			}
		}
		if (showOff) {
			if (model instanceof ModelBipedAlt) { showOff = flag ? ((ModelBipedAlt) model).leftStackData.showModel : ((ModelBipedAlt) model).rightStackData.showModel; }
			else if (model instanceof ModelNpcAlt) { showOff = flag ? ((ModelNpcAlt) model).leftStackData.showModel : ((ModelNpcAlt) model).rightStackData.showModel; }
			if (showOff) {
				renderHeldItem(npc, offhand, ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, EnumHandSide.LEFT, scale, distance, isAWShow, isAnimated);
			}
		}
		GlStateManager.popMatrix();
	}

	@SuppressWarnings("all")
	private void renderHeldItem(EntityLivingBase entity, ItemStack stack, ItemCameraTransforms.TransformType transform, EnumHandSide handSide, float scale, double distance, boolean isAWShow, boolean isAnimated) {
		if (stack == null || stack.isEmpty()) { return; }
		GlStateManager.pushMatrix();
		if (isAWShow && ArmourersWorkshopApi.getSkinNBTUtils().hasSkinDescriptor(stack)) {
			renderHeldAWItem(ArmourersWorkshopApi.getSkinNBTUtils().getSkinDescriptor(stack), entity.isSneaking(), handSide, distance, scale);
			GlStateManager.popMatrix();
			return;
		}
		model.postRenderArm(scale, handSide);
		boolean isLeft = handSide == EnumHandSide.LEFT;
		GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
		GlStateManager.translate((isLeft ? -1.0F : 1.0F) / 16.0F, 0.125f, -0.625f);
		Minecraft mc = Minecraft.getMinecraft();
		if (isAnimated && stack.getItem() instanceof ItemArmor && render != null) {
			ItemArmor itemarmor = (ItemArmor) stack.getItem();
			EntityEquipmentSlot slot = itemarmor.getEquipmentSlot();
			LayerRenderer<?> layer = ((IRenderLivingBaseMixin) render).npcs$getLayer(LayerArmorBase.class);
			ModelBiped modelArmor = null;
			ResourceLocation location = null;
			ResourceLocation locationColor = null;
			if (layer instanceof LayerArmorBase) {
				location = ((LayerArmorBase<?>) layer).getArmorResource(entity, stack, slot, null);
				locationColor = ((LayerArmorBase<?>) layer).getArmorResource(entity, stack, slot, "overlay");
				ModelBase armorModel = ((LayerArmorBase<?>) layer).getModelFromSlot(slot);
				if (armorModel instanceof ModelBiped) {
					modelArmor = (ModelBiped) armorModel;
					ModelBiped armorModelNext = itemarmor.getArmorModel(entity, stack, slot, (ModelBiped) armorModel);
					if (armorModelNext != null) { modelArmor = armorModelNext; }
				}
			}
			if (modelArmor != null) {
				if (modelArmor instanceof ModelBipedAlt) { ((ModelBipedAlt) modelArmor).setSlot(slot); }
				mc.getTextureManager().bindTexture(location);
				if (itemarmor.hasOverlay(stack)) {
					int i = itemarmor.getColor(stack);
					float f = (float)(i >> 16 & 255) / 255.0F;
					float f1 = (float)(i >> 8 & 255) / 255.0F;
					float f2 = (float)(i & 255) / 255.0F;
					GlStateManager.color(f, f1, f2, 1.0f);
				}
				GlStateManager.rotate(-90.0f, 1.0f, 0.0f, 0.0f);
				GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f);
				switch (slot) {
					case HEAD: {
						GlStateManager.translate(0.0f, 0.5F, -0.1875f);
						if (modelArmor.bipedHead instanceof ModelRendererAlt) {
							((ModelRendererAlt) modelArmor.bipedHead).clearRotations();
						}
						modelArmor.bipedHead.isHidden = false;
						modelArmor.bipedHead.showModel = true;
						if (itemarmor.hasOverlay(stack)) {
							int i = itemarmor.getColor(stack);
							float f = (float) (i >> 16 & 255) / 255.0F;
							float f1 = (float) (i >> 8 & 255) / 255.0F;
							float f2 = (float) (i & 255) / 255.0F;
							if (modelArmor.bipedHead instanceof ModelRendererAlt) { ((ModelRendererAlt) modelArmor.bipedHead).checkBacklightColor(f, f1, f2); }
							GlStateManager.color(f, f1, f2, 1.0f);
							modelArmor.bipedHead.render(scale);
							mc.getTextureManager().bindTexture(locationColor);
						}
						if (modelArmor.bipedHead instanceof ModelRendererAlt) { ((ModelRendererAlt) modelArmor.bipedHead).checkBacklightColor(1.0f, 1.0f, 1.0f); }
						GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
						modelArmor.bipedHead.render(scale);
						break;
					}
					case CHEST: {
						GlStateManager.translate(0.0F, 0.0F, -0.0625F);
						if (modelArmor.bipedBody instanceof ModelRendererAlt) {
							((ModelRendererAlt) modelArmor.bipedRightArm).clearRotations();
							((ModelRendererAlt) modelArmor.bipedLeftArm).clearRotations();
							((ModelRendererAlt) modelArmor.bipedBody).clearRotations();
						}
						modelArmor.bipedRightArm.isHidden = false;
						modelArmor.bipedRightArm.showModel = true;
						modelArmor.bipedLeftArm.isHidden = false;
						modelArmor.bipedLeftArm.showModel = true;
						modelArmor.bipedBody.isHidden = false;
						modelArmor.bipedBody.showModel = true;
						if (itemarmor.hasOverlay(stack)) {
							int i = itemarmor.getColor(stack);
							float f = (float) (i >> 16 & 255) / 255.0F;
							float f1 = (float) (i >> 8 & 255) / 255.0F;
							float f2 = (float) (i & 255) / 255.0F;
							if (modelArmor.bipedRightArm instanceof ModelRendererAlt) { ((ModelRendererAlt) modelArmor.bipedRightArm).checkBacklightColor(f, f1, f2); }
							if (modelArmor.bipedLeftArm instanceof ModelRendererAlt) { ((ModelRendererAlt) modelArmor.bipedLeftArm).checkBacklightColor(f, f1, f2); }
							if (modelArmor.bipedBody instanceof ModelRendererAlt) { ((ModelRendererAlt) modelArmor.bipedBody).checkBacklightColor(f, f1, f2); }
							GlStateManager.color(f, f1, f2, 1.0f);
							modelArmor.bipedRightArm.render(scale);
							modelArmor.bipedLeftArm.render(scale);
							modelArmor.bipedBody.render(scale);
							mc.getTextureManager().bindTexture(locationColor);
						}
						if (modelArmor.bipedRightArm instanceof ModelRendererAlt) { ((ModelRendererAlt) modelArmor.bipedRightArm).checkBacklightColor(1.0f, 1.0f, 1.0f); }
						if (modelArmor.bipedLeftArm instanceof ModelRendererAlt) { ((ModelRendererAlt) modelArmor.bipedLeftArm).checkBacklightColor(1.0f, 1.0f, 1.0f); }
						if (modelArmor.bipedBody instanceof ModelRendererAlt) { ((ModelRendererAlt) modelArmor.bipedBody).checkBacklightColor(1.0f, 1.0f, 1.0f); }
						GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
						modelArmor.bipedRightArm.render(scale);
						modelArmor.bipedLeftArm.render(scale);
						modelArmor.bipedBody.render(scale);
						break;
					}
					case LEGS:
					case FEET: {
						if (slot == EntityEquipmentSlot.LEGS) { GlStateManager.translate(0.0F, -0.78125F, -0.0625F); }
						else { GlStateManager.translate(0.0F, -1.1875F, -0.0625F); }
						if (modelArmor.bipedBody instanceof ModelRendererAlt) {
							((ModelRendererAlt) modelArmor.bipedRightLeg).clearRotations();
							((ModelRendererAlt) modelArmor.bipedLeftLeg).clearRotations();
						}
						modelArmor.bipedRightLeg.isHidden = false;
						modelArmor.bipedRightLeg.showModel = true;
						modelArmor.bipedLeftLeg.isHidden = false;
						modelArmor.bipedLeftLeg.showModel = true;
						if (itemarmor.hasOverlay(stack)) {
							int i = itemarmor.getColor(stack);
							float f = (float) (i >> 16 & 255) / 255.0F;
							float f1 = (float) (i >> 8 & 255) / 255.0F;
							float f2 = (float) (i & 255) / 255.0F;
							if (modelArmor.bipedRightLeg instanceof ModelRendererAlt) { ((ModelRendererAlt) modelArmor.bipedRightLeg).checkBacklightColor(f, f1, f2); }
							if (modelArmor.bipedLeftLeg instanceof ModelRendererAlt) { ((ModelRendererAlt) modelArmor.bipedLeftLeg).checkBacklightColor(f, f1, f2); }
							modelArmor.bipedRightLeg.render(scale);
							modelArmor.bipedLeftLeg.render(scale);
							mc.getTextureManager().bindTexture(locationColor);
						}
						if (modelArmor.bipedRightLeg instanceof ModelRendererAlt) { ((ModelRendererAlt) modelArmor.bipedRightLeg).checkBacklightColor(1.0f, 1.0f, 1.0f); }
						if (modelArmor.bipedLeftLeg instanceof ModelRendererAlt) { ((ModelRendererAlt) modelArmor.bipedLeftLeg).checkBacklightColor(1.0f, 1.0f, 1.0f); }
						GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
						modelArmor.bipedRightLeg.render(scale);
						modelArmor.bipedLeftLeg.render(scale);
						break;
					}
				}
				GlStateManager.popMatrix();
				return;
			}
		}
		mc.getItemRenderer().renderItemSide(entity, stack, transform, isLeft);
		GlStateManager.popMatrix();
	}

	private void renderHeldAWItem(ISkinDescriptor skinDescriptor, boolean isSneaking, EnumHandSide handSide, double distance, float scale) {
		ArmourersWorkshopUtil awu  = ArmourersWorkshopUtil.getInstance();
		CustomSkinModelRenderHelper modelRenderer = CustomSkinModelRenderHelper.getInstance();
		IWardrobeCap wardrobe = ArmourersWorkshopApi.getEntityWardrobeCapability(npc);
		GlStateManager.pushMatrix();
		model.postRenderArm(scale, handSide);
		boolean isLeft = handSide == EnumHandSide.LEFT;
		try {
			ISkin skin = (ISkin) awu.getSkin.invoke(awu.clientSkinCache, skinDescriptor);
			if (skin != null) {
				ISkinDye dye = (ISkinDye) awu.skinDyeConstructor.newInstance(wardrobe.getDye());
				for (int dyeIndex = 0; dyeIndex < 8; dyeIndex++) {
					if (skinDescriptor.getSkinDye().haveDyeInSlot(dyeIndex)) {
						dye.addDye(dyeIndex, skinDescriptor.getSkinDye().getDyeColour(dyeIndex));
					}
				}
				ResourceLocation texture = DefaultPlayerSkin.getDefaultSkinLegacy();
				Object renderData = awu.skinRenderDataConstructor.newInstance(scale, dye, awu.extraColours, distance, true, true, false, texture);
				GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
				GlStateManager.translate((isLeft ? 1.0F : -1.0F) / 16.0F, 0.0F, -0.5F);
				if (isLeft) {
					String type = skinDescriptor.getIdentifier().getSkinType().getName();
					if (type.equalsIgnoreCase("shield")) {
						GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
						GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
						GlStateManager.translate(0.0f, 0.0f, -0.125f);
					} else if (type.equalsIgnoreCase("bow")) {
						GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
						GlStateManager.rotate(220.0F, 1.0F, 0.0F, 0.0F);
						GlStateManager.translate(0.0f, 0.1875f, -0.0625f);
					}
				}
				modelRenderer.renderEquipmentPart(skin, renderData, this.npc, (ModelBiped) this.render.getMainModel(), scale, null);
			}
		} catch (Exception e) {
			LogWriter.error(e);
			GlStateManager.popMatrix();
			return;
		}
		GlStateManager.popMatrix();
	}

	@Override
	public void rotate(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
	}

}