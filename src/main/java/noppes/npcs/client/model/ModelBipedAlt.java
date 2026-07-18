package noppes.npcs.client.model;

import java.util.HashMap;
import java.util.Map;

import noppes.npcs.mixin.client.model.IModelPlayerMixin;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import noppes.npcs.client.model.animation.AnimationStack;
import noppes.npcs.client.model.part.head.ModelHeadwear;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.custom.CustomArmor;

import javax.annotation.Nonnull;

/* Used to create simple models
 * like a player. Texture type 64x32
 * Example - layers of armor, Classic Player, Golem, etc.
 */
public class ModelBipedAlt extends ModelNpcAlt {

	private final boolean isArmorModel;
	private float armorColorR = 1.0f;
	private float armorColorG = 1.0f;
	private float armorColorB = 1.0f;
	public final AnimationStack rightStackData = new AnimationStack(7);
	public final AnimationStack leftStackData = new AnimationStack(6);
	protected EntityEquipmentSlot slot;

	public ModelBipedAlt(float modelSize, boolean isArmorModelIn, boolean smallArmsIn, boolean isClassicPlayer) {
		super(modelSize, smallArmsIn, isClassicPlayer);
		isArmorModel = isArmorModelIn;
		textureWidth = 64;
		textureHeight = 32;
		init(modelSize);
	}

	@Override
	protected void init(float modelSize) {
		float handWidth = smallArmsIn ? 3.0f : 4.0f;
		bipedHead = new ModelRendererAlt(this, EnumParts.HEAD, 0, 0, true);
		((ModelRendererAlt) bipedHead).setBox(-4.0F, -8.0F, -4.0F, 8, 3 , 3, 2, 8, modelSize);
		bipedHead.setRotationPoint(0.0F, 0.0F, 0.0F);
		bipedHeadwear = new ModelRendererAlt(this, EnumParts.HEAD, 32, 0, true);
		((ModelRendererAlt) bipedHeadwear).setBox(-4.0F, -8.0F, -4.0F, 8, 3 , 3, 2, 8, modelSize + 0.5f);
		bipedHeadwear.setRotationPoint(0.0F, 0.0F, 0.0F);

		bipedHeadwear_64 = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 64, false);
		bipedHeadwear_128 = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 128, false);
		bipedHeadwear_256 = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 256, false);
		bipedHeadwear_512 = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 512, false);
		bipedHeadwear_1024 = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 1024, false);
		bipedHeadwear_2048 = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 2048, false);
		bipedHeadwear_4096 = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 4096, false);

		bipedHeadwear_64_old = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 64, true);
		bipedHeadwear_128_old = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 128, true);
		bipedHeadwear_256_old = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 256, true);
		bipedHeadwear_512_old = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 512, true);
		bipedHeadwear_1024_old = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 1024, true);
		bipedHeadwear_2048_old = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 2048, true);
		bipedHeadwear_4096_old = new ModelHeadwear(this, EnumParts.HEAD, 32, 0, 4096, true);

		bipedBody = new ModelRendererAlt(this, EnumParts.BODY, 16, 16, false);
		((ModelRendererAlt) bipedBody).setBox(-4.0F, 0.0F, -2.0F, 8, 5.5f, 4.0f, 2.5f, 4, modelSize);
		bipedBody.setRotationPoint(0.0F, 0.0F, 0.0F);

		bipedRightArm = new ModelRendererAlt(this, EnumParts.ARM_RIGHT, 40, 16, false);
		((ModelRendererAlt) bipedRightArm).setBox(smallArmsIn ? -2.0F : -3.0F, -2.0F, -2.0F, handWidth, 5.5f, 3.5f, 3.0f, 4, modelSize);
		bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
		((ModelRendererAlt) bipedRightArm).smallArms = smallArmsIn;
		((ModelRendererAlt) bipedRightArm).isArmor = true;
		bipedRightLeg = new ModelRendererAlt(this, EnumParts.LEG_RIGHT, 0, 16, false);
		((ModelRendererAlt) bipedRightLeg).setBox(-2.0F, 0.0F, -2.1F, 4.0f, 5.5f, 3.5f, 3.0f, 4, modelSize);
		bipedRightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);
		((ModelRendererAlt) bipedRightLeg).isArmor = true;

		bipedLeftArm = new ModelRendererAlt(this, EnumParts.ARM_LEFT, 40, 16, false);
		bipedLeftArm.mirror = true;
		((ModelRendererAlt) bipedLeftArm).setBox(-1.0F, -2.0F, -2.0F, handWidth, 5.5f, 3.5f, 3.0f, 4, modelSize);
		bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
		((ModelRendererAlt) bipedLeftArm).smallArms = smallArmsIn;
		((ModelRendererAlt) bipedLeftArm).isArmor = true;
		bipedLeftLeg = new ModelRendererAlt(this, EnumParts.LEG_LEFT, 0, 16, false);
		bipedLeftLeg.mirror = true;
		((ModelRendererAlt) bipedLeftLeg).setBox(-2.2F, 0.0F, -2.1F, 4.0f, 5.5f, 3.5f, 3.0f, 4, modelSize);
		bipedLeftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);
		((ModelRendererAlt) bipedLeftLeg).isArmor = true;

		bipedCape = new ModelRendererAlt(this, EnumParts.BODY, 0, 0, true);
		bipedCape.setTextureSize(64, 64);
		bipedCape.setBox(0.0F, 0.0F, 0.0F, 0, 0, 0, 0, 0, modelSize);
		bipedCape.setRotationPoint(0.0F, 0.0F, 0.0F);
		((IModelPlayerMixin) this).setBipedCape(bipedCape);
	}

	@Override
	public void render(@Nonnull Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		if (entityIn.equals(ModelNpcAlt.editAnimDataSelect.displayNpc) && !ModelNpcAlt.editAnimDataSelect.showArmor) { return; }
		setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
		Map<EnumParts, Boolean> ba = new HashMap<>();
		Map<EnumParts, Boolean> baArmor = new HashMap<>();

		float r = 1.0f, g = 1.0f, b = 1.0f;
		if (isArmorModel) {
			r = armorColorR;
			g = armorColorG;
			b = armorColorB;
		}
		if (entityIn instanceof EntityPlayer) {
			PlayerData data = PlayerData.get((EntityPlayer) entityIn);
			ba = data.animation.showParts;
			baArmor = data.animation.showArmorParts;
		}
		else if (entityIn instanceof EntityNPCInterface) {
			EntityNPCInterface npc = (EntityNPCInterface) entityIn;
			ba = npc.animation.showParts;
			baArmor = npc.animation.showArmorParts;
			if (!isArmorModel && npc.display.getTint() != 0xFFFFFF) {
				r = (float)(npc.display.getTint() >> 16 & 255) / 255.0F;
				g = (float)(npc.display.getTint() >> 8 & 255) / 255.0F;
				b = (float)(npc.display.getTint() & 255) / 255.0F;
			}
		}

		ItemStack stack;
		int entitySkinTextureID = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		GlStateManager.pushMatrix();
		if (!isChild && entityIn.isSneaking()) { GlStateManager.translate(0.0f, 0.2f, 0.0f); }
		GlStateManager.enableBlend();

		bipedHead.showModel = ba.get(EnumParts.HEAD) && (!isArmorModel || (baArmor.get(EnumParts.HEAD) && slot == EntityEquipmentSlot.HEAD));
		if (bipedHead.showModel && entityIn instanceof EntityLivingBase) {
			((ModelRendererAlt) bipedHead).checkBacklightColor(r, g, b);
			if (isChild) {
				GlStateManager.scale(0.75F, 0.75F, 0.75F);
				GlStateManager.translate(0.0F, 16.0F * scale, 0.0F);
				if (renderHead((EntityLivingBase) entityIn, scale)) {
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID);
					renderHeadWear(scale);
				}
				GlStateManager.popMatrix();
				GlStateManager.pushMatrix();
				GlStateManager.scale(0.5F, 0.5F, 0.5F);
				GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
			}
			else {
				if (renderHead((EntityLivingBase) entityIn, scale)) {
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID);
					renderHeadWear(scale);
				}
			}
		}
		bipedRightArm.showModel = ba.get(EnumParts.ARM_RIGHT) && (!isArmorModel || (baArmor.get(EnumParts.ARM_RIGHT) && slot == EntityEquipmentSlot.CHEST));
		bipedLeftArm.showModel = ba.get(EnumParts.ARM_LEFT) && (!isArmorModel || (baArmor.get(EnumParts.ARM_LEFT) && slot == EntityEquipmentSlot.CHEST));
		if (bipedRightArm.showModel || bipedLeftArm.showModel) {
			if (isArmorModel && slot == EntityEquipmentSlot.CHEST) {
				stack = ((EntityLivingBase) entityIn).getItemStackFromSlot(EntityEquipmentSlot.CHEST);
				bipedRightArm.showModel = stack.getItem() instanceof ItemArmor && baArmor.get(EnumParts.ARM_RIGHT);
				bipedLeftArm.showModel = stack.getItem() instanceof ItemArmor && baArmor.get(EnumParts.ARM_LEFT);
                ((ModelRendererAlt) bipedRightArm).clearOBJ();
                ((ModelRendererAlt) bipedLeftArm).clearOBJ();
                if (entityIn instanceof EntityCustomNpc) {
                    String m = ((EntityCustomNpc) entityIn).display.getModel();
                    boolean smallArms = m != null && m.contains("customnpcalex");
					((ModelRendererAlt) bipedRightArm).smallArms = smallArms;
                    ((ModelRendererAlt) bipedLeftArm).smallArms = smallArms;
                }
                if (stack.getItem() instanceof CustomArmor && ((CustomArmor) stack.getItem()).objModel != null) {
                    ((ModelRendererAlt) bipedRightArm).setOBJModel(stack, null);
                    ((ModelRendererAlt) bipedLeftArm).setOBJModel(stack, null);
                }
            }
            GlStateManager.pushMatrix();
            if (bipedRightArm.showModel) {
                ((ModelRendererAlt) bipedRightArm).checkBacklightColor(r, g, b);
                if (((ModelRendererAlt) bipedRightArm).notOBJModel()) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID); }
                bipedRightArm.render(scale);
            }
            if (bipedLeftArm.showModel) {
                ((ModelRendererAlt) bipedLeftArm).checkBacklightColor(r, g, b);
                if (((ModelRendererAlt) bipedLeftArm).notOBJModel()) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID); }
                bipedLeftArm.render(scale);
            }
            GlStateManager.popMatrix();
        }

		bipedRightLeg.showModel = ba.get(EnumParts.LEG_RIGHT) && (!isArmorModel || (baArmor.get(EnumParts.LEG_RIGHT) && (slot == EntityEquipmentSlot.LEGS || slot == EntityEquipmentSlot.FEET)));
		bipedLeftLeg.showModel = ba.get(EnumParts.LEG_LEFT) && (!isArmorModel || (baArmor.get(EnumParts.LEG_LEFT) && (slot == EntityEquipmentSlot.LEGS || slot == EntityEquipmentSlot.FEET)));
		if ((ba.get(EnumParts.LEG_RIGHT) && bipedRightLeg.showModel) || (ba.get(EnumParts.LEG_LEFT) && bipedLeftLeg.showModel)) {
			boolean legsRender = true;
			if (isArmorModel && (slot == EntityEquipmentSlot.LEGS || slot == EntityEquipmentSlot.FEET)) {
				if (slot == EntityEquipmentSlot.LEGS) {
					bipedRightLeg.showModel = baArmor.get(EnumParts.LEG_RIGHT);
					bipedLeftLeg.showModel = baArmor.get(EnumParts.LEG_LEFT);
				}
				else {
					bipedRightLeg.showModel = baArmor.get(EnumParts.FEET_RIGHT);
					bipedLeftLeg.showModel = baArmor.get(EnumParts.FEET_LEFT);
					baArmor.put(EnumParts.LEG_RIGHT, bipedRightLeg.showModel);
					baArmor.put(EnumParts.LEG_LEFT, bipedLeftLeg.showModel);
				}
				legsRender = ((EntityLivingBase) entityIn).getItemStackFromSlot(slot).getItem() instanceof ItemArmor;
				((ModelRendererAlt) bipedBody).clearOBJ();
				((ModelRendererAlt) bipedRightLeg).clearOBJ();
				((ModelRendererAlt) bipedLeftLeg).clearOBJ();
				if (slot == EntityEquipmentSlot.FEET && legsRender) {
					stack = ((EntityLivingBase) entityIn).getItemStackFromSlot(EntityEquipmentSlot.FEET);
					if (stack.getItem() instanceof CustomArmor && ((CustomArmor) stack.getItem()).objModel != null) {
						if (bipedRightLeg.showModel) {
							((ModelRendererAlt) bipedRightLeg).checkBacklightColor(r, g, b);
							((ModelRendererAlt) bipedRightLeg).setOBJModel(stack, EnumParts.FEET_RIGHT);
							bipedRightLeg.render(scale);
						}
						if (bipedLeftLeg.showModel) {
							((ModelRendererAlt) bipedLeftLeg).checkBacklightColor(r, g, b);
							((ModelRendererAlt) bipedLeftLeg).setOBJModel(stack, EnumParts.FEET_LEFT);
							bipedLeftLeg.render(scale);
						}
					}
				}
				if (slot == EntityEquipmentSlot.LEGS && legsRender) {
					stack = ((EntityLivingBase) entityIn).getItemStackFromSlot(EntityEquipmentSlot.LEGS);
					if (stack.getItem() instanceof CustomArmor && ((CustomArmor) stack.getItem()).objModel != null) {
						if (bipedRightLeg.showModel) { ((ModelRendererAlt) bipedRightLeg).setOBJModel(stack, null); }
						if (bipedLeftLeg.showModel) { ((ModelRendererAlt) bipedLeftLeg).setOBJModel(stack, null); }
						if (ba.get(EnumParts.BODY) && bipedBody.showModel) {
							((ModelRendererAlt) bipedBody).checkBacklightColor(r, g, b);
							((ModelRendererAlt) bipedBody).setOBJModel(stack, EnumParts.BELT);
							bipedBody.render(scale);
						}
					}
				}
			}
			if (legsRender) {
				GlStateManager.pushMatrix();
				if (bipedRightLeg.showModel) {
					((ModelRendererAlt) bipedRightLeg).checkBacklightColor(r, g, b);
					if (((ModelRendererAlt) bipedRightLeg).notOBJModel()) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID); }
					bipedRightLeg.render(scale);
				}
				if (bipedLeftLeg.showModel) {
					((ModelRendererAlt) bipedLeftLeg).checkBacklightColor(r, g, b);
					if (((ModelRendererAlt) bipedLeftLeg).notOBJModel()) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID); }
					bipedLeftLeg.render(scale);
				}
				GlStateManager.popMatrix();
			}
		}

		bipedBody.showModel = ba.get(EnumParts.BODY) && (!isArmorModel || (baArmor.get(EnumParts.BODY) && slot == EntityEquipmentSlot.CHEST));
		if (bipedBody.showModel) {
			if (isArmorModel) {
				((ModelRendererAlt) bipedBody).clearOBJ();
				stack = ((EntityLivingBase) entityIn).getItemStackFromSlot(EntityEquipmentSlot.CHEST);
				if (stack.getItem() instanceof CustomArmor && ((CustomArmor) stack.getItem()).objModel != null) { ((ModelRendererAlt) bipedBody).setOBJModel(stack, null); }
			}
			GlStateManager.pushMatrix();
			((ModelRendererAlt) bipedBody).checkBacklightColor(r, g, b);
			if (((ModelRendererAlt) bipedBody).notOBJModel()) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID); }
			bipedBody.render(scale);
			GlStateManager.popMatrix();
		}
		GlStateManager.popMatrix();
	}

	private boolean renderHead(EntityLivingBase living, float scale) {
		ItemStack stack = living.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		((ModelRendererAlt) bipedHead).clearOBJ();
		if (stack.getItem() instanceof CustomArmor && ((CustomArmor) stack.getItem()).objModel != null) {
			((ModelRendererAlt) bipedHead).setOBJModel(stack, null);
		}
		bipedHead.render(scale);
		return ((ModelRendererAlt) bipedHead).notOBJModel();
	}

	public void setSlot(EntityEquipmentSlot slotIn) { slot = slotIn; }

	public void setArmorColor(float r, float g, float b) {
		armorColorR = r;
		armorColorG = g;
		armorColorB = b;
	}

}
