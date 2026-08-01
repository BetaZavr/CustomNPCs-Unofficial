package noppes.npcs.client.layer;

import moe.plushie.armourers_workshop.api.ArmourersWorkshopApi;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import noppes.npcs.client.model.ModelBipedAW;
import noppes.npcs.client.model.ModelBipedAlt;
import noppes.npcs.entity.EntityNPCInterface;

import javax.annotation.Nonnull;

public class LayerCustomArmor<T extends ModelBiped> extends LayerArmorBase<T> {

	private final RenderLivingBase<?> renderer;
	private final boolean skipRenderGlint;
	private final boolean smallArms;
	private final boolean isClassicPlayer;
	protected ModelBipedAW modelAW;

	@SuppressWarnings("unchecked")
	public LayerCustomArmor(RenderLivingBase<?> rendererIn, boolean skipRenderGlintIn, boolean smallArmsIn, boolean isClassicPlayerIn) {
		super(rendererIn);
		renderer = rendererIn;
        skipRenderGlint = skipRenderGlintIn;
        smallArms = smallArmsIn;
		isClassicPlayer = isClassicPlayerIn;
		modelLeggings = (T) new ModelBipedAlt(0.5F, true, smallArmsIn, isClassicPlayer);
		modelArmor = (T) new ModelBipedAlt(1.0F, true, smallArmsIn, isClassicPlayer);
        modelAW = new ModelBipedAW(1.0F, true, smallArmsIn, isClassicPlayer);
	}

	@Override
	public void doRenderLayer(@Nonnull EntityLivingBase entityIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		GlStateManager.enableRescaleNormal();
		renderArmorLayer(entityIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale, EntityEquipmentSlot.CHEST);
		renderArmorLayer(entityIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale, EntityEquipmentSlot.LEGS);
		renderArmorLayer(entityIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale, EntityEquipmentSlot.FEET);
		renderArmorLayer(entityIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale, EntityEquipmentSlot.HEAD);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected @Nonnull T getArmorModelHook(@Nonnull EntityLivingBase entity, @Nonnull ItemStack itemStack, @Nonnull EntityEquipmentSlot slot, @Nonnull ModelBiped model) {
		return (T) ForgeHooksClient.getArmorModel(entity, itemStack, slot, model);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void initArmor() {
		modelLeggings = (T) new ModelBipedAlt(0.5F, true, smallArms, isClassicPlayer);
		modelArmor = (T) new ModelBipedAlt(1.0F, true, smallArms, isClassicPlayer);
		modelAW = new ModelBipedAW(1.0F, true, smallArms, isClassicPlayer);
	}

	@SuppressWarnings("all")
	protected void renderArmorLayer(EntityLivingBase entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, EntityEquipmentSlot slotIn) {
		boolean isAWLoad = ArmourersWorkshopApi.isAvailable();
		if (isAWLoad && entityLivingBaseIn instanceof EntityNPCInterface) {
			modelAW.setSlot(slotIn);
			modelAW.setModelAttributes(renderer.getMainModel());
			modelAW.setLivingAnimations(entityLivingBaseIn, limbSwing, limbSwingAmount, partialTicks);
			setModelSlotVisible(modelAW, slotIn);
			modelAW.render(entityLivingBaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
		}
		ItemStack itemstack = entityLivingBaseIn.getItemStackFromSlot(slotIn);
		if (!(itemstack.getItem() instanceof ItemArmor)) { return; }
		if (isAWLoad && ArmourersWorkshopApi.getSkinNBTUtils().hasSkinDescriptor(itemstack)) { return; }
		ItemArmor itemarmor = (ItemArmor) itemstack.getItem();
		if (itemarmor.getEquipmentSlot() != slotIn) { return; }

		T t = (T) getModelFromSlot(slotIn);
		t = (T) getArmorModelHook(entityLivingBaseIn, itemstack, slotIn, (ModelBiped) t);
		t.setModelAttributes(renderer.getMainModel());
		t.setLivingAnimations(entityLivingBaseIn, limbSwing, limbSwingAmount, partialTicks);
		setModelSlotVisible((ModelBiped) t, slotIn);
		renderer.bindTexture(getArmorResource(entityLivingBaseIn, itemstack, slotIn, null));
		if (t instanceof ModelBipedAlt) { ((ModelBipedAlt) t).setSlot(slotIn); }
		if (itemarmor.hasOverlay(itemstack)) { // Allow this for anything, not only cloth
			int i = itemarmor.getColor(itemstack);
			float f = (float) (i >> 16 & 255) / 255.0F;
			float f1 = (float) (i >> 8 & 255) / 255.0F;
			float f2 = (float) (i & 255) / 255.0F;
			GlStateManager.color(f, f1, f2, 1.0f);
			if (t instanceof ModelBipedAlt) { ((ModelBipedAlt) t).setArmorColor(f, f1, f2); }
			t.render(entityLivingBaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
			renderer.bindTexture(getArmorResource(entityLivingBaseIn, itemstack, slotIn, "overlay"));
		}
		// Non-colored
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		if (t instanceof ModelBipedAlt) { ((ModelBipedAlt) t).setArmorColor(1.0f, 1.0f, 1.0f); }
		t.render(entityLivingBaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

		// Default
		if (!skipRenderGlint && itemstack.hasEffect()) {
			renderEnchantedGlint(renderer, entityLivingBaseIn, t, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
		}
	}

	protected void setModelSlotVisible(@Nonnull ModelBiped modelBiped, @Nonnull EntityEquipmentSlot slotIn) {
		setModelVisible(modelBiped);
		switch (slotIn) {
		case HEAD:
			modelBiped.bipedHead.showModel = true;
			modelBiped.bipedHeadwear.showModel = true;
			break;
		case CHEST:
			modelBiped.bipedBody.showModel = true;
			modelBiped.bipedRightArm.showModel = true;
			modelBiped.bipedLeftArm.showModel = true;
			break;
		case LEGS:
			modelBiped.bipedBody.showModel = true;
			modelBiped.bipedRightLeg.showModel = true;
			modelBiped.bipedLeftLeg.showModel = true;
			break;
		case FEET:
			modelBiped.bipedRightLeg.showModel = true;
			modelBiped.bipedLeftLeg.showModel = true;
		}
	}

	protected void setModelVisible(ModelBiped model) {
		model.bipedHead.showModel = false;
		model.bipedHeadwear.showModel = false;
		model.bipedBody.showModel = false;
		model.bipedRightArm.showModel = false;
		model.bipedLeftArm.showModel = false;
		model.bipedRightLeg.showModel = false;
		model.bipedLeftLeg.showModel = false;
	}

}
