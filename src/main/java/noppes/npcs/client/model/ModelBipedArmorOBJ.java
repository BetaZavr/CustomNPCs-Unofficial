package noppes.npcs.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import noppes.npcs.client.model.part.ModelRendererPartOBJ;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.items.custom.CustomArmorOBJ;
import noppes.npcs.mixin.client.renderer.entity.IRenderPlayerMixin;

import javax.annotation.Nonnull;

public class ModelBipedArmorOBJ extends ModelBiped {

    protected final EntityEquipmentSlot slot;

    protected final ModelRenderer bipedBelt;
    protected final ModelRenderer bipedRightFeet;
    protected final ModelRenderer bipedLeftFeet;

    protected final ModelRendererPartOBJ headChild;
    protected final ModelRendererPartOBJ bodyChild;
    protected final ModelRendererPartOBJ leftArmChild;
    protected final ModelRendererPartOBJ rightArmChild;
    protected final ModelRendererPartOBJ leftLegChild;
    protected final ModelRendererPartOBJ rightLegChild;
    protected final ModelRendererPartOBJ beltChild;
    protected final ModelRendererPartOBJ leftFeetChild;
    protected final ModelRendererPartOBJ rightFeetChild;

    public ModelBipedArmorOBJ(EntityEquipmentSlot slotIn) {
        super(0, 0, 128, 128);
        slot = slotIn;
        // head
        bipedHeadwear.cubeList.clear();
        bipedHeadwear.showModel = false;
        bipedHeadwear.isHidden = true;
        bipedHead.cubeList.clear();
        bipedHead.addChild(headChild = new ModelRendererPartOBJ(this, EnumParts.HEAD, 0.0f, 1.5f, 0.0f));

        bipedBody.cubeList.clear();
        bipedBody.addChild(bodyChild = new ModelRendererPartOBJ(this, EnumParts.BODY, 0.0f, 1.5f, 0.0f));
        bipedLeftArm.cubeList.clear();
        bipedLeftArm.addChild(leftArmChild = new ModelRendererPartOBJ(this, EnumParts.ARM_LEFT_FULL, -0.3175f, 1.375f, 0.0f));
        bipedRightArm.cubeList.clear();
        bipedRightArm.addChild(rightArmChild = new ModelRendererPartOBJ(this, EnumParts.ARM_RIGHT_FULL, 0.3175f, 1.375f, 0.0f));

        bipedLeftLeg.cubeList.clear();
        bipedLeftLeg.addChild(leftLegChild = new ModelRendererPartOBJ(this, EnumParts.LEG_LEFT_FULL, -0.115f, 0.75f, 0.0f));
        bipedRightLeg.cubeList.clear();
        bipedRightLeg.addChild(rightLegChild = new ModelRendererPartOBJ(this, EnumParts.LEG_RIGHT_FULL, 0.125f, 0.75f, 0.0f));
        bipedBelt = new ModelRenderer(this);
        bipedBelt.addChild(beltChild = new ModelRendererPartOBJ(this, EnumParts.BELT, 0.0f, 1.5f, 0.0f));

        bipedLeftFeet = new ModelRenderer(this);
        bipedLeftFeet.addChild(leftFeetChild = new ModelRendererPartOBJ(this, EnumParts.FEET_LEFT, -0.125f, 1.0f, 0.0f));
        bipedRightFeet = new ModelRenderer(this);
        bipedRightFeet.addChild(rightFeetChild = new ModelRendererPartOBJ(this, EnumParts.FEET_RIGHT, 0.125f, 1.0f, 0.0f));
    }

    @Override
    public void render(@Nonnull Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (!(entityIn instanceof EntityLivingBase)) { return; }
        EntityLivingBase living = (EntityLivingBase) entityIn;
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, living);

        GlStateManager.pushMatrix();
        if (slot == EntityEquipmentSlot.HEAD) {
            ItemStack stack = living.getItemStackFromSlot(slot);
            if (stack.getItem() instanceof CustomArmorOBJ) {
                headChild.setOBJModel(((CustomArmorOBJ) stack.getItem()).getParameterizedModel(EnumParts.HEAD, stack, null, false));
                if (isChild) {
                    GlStateManager.scale(0.75F, 0.75F, 0.75F);
                    GlStateManager.translate(0.0F, 16.0F * scale, 0.0F);
                    bipedHead.render(scale);
                    GlStateManager.popMatrix();

                    GlStateManager.pushMatrix();
                    GlStateManager.scale(0.5F, 0.5F, 0.5F);
                    GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
                }
                else {
                    if (entityIn.isSneaking()) { GlStateManager.translate(0.0F, 0.2F, 0.0F); }
                    bipedHead.render(scale);
                }
            }
        }
        if (slot == EntityEquipmentSlot.CHEST) {
            ItemStack stack = living.getItemStackFromSlot(slot);
            if (stack.getItem() instanceof CustomArmorOBJ) {
                bodyChild.setOBJModel(((CustomArmorOBJ) stack.getItem()).getParameterizedModel(EnumParts.BODY, stack, null, false));
                leftArmChild.setOBJModel(((CustomArmorOBJ) stack.getItem()).getParameterizedModel(EnumParts.ARM_LEFT_FULL, stack, null, false));
                rightArmChild.setOBJModel(((CustomArmorOBJ) stack.getItem()).getParameterizedModel(EnumParts.ARM_RIGHT_FULL, stack, null, false));

                boolean smallArms = false;
                if (entityIn instanceof EntityPlayer) {
                    Minecraft mc = Minecraft.getMinecraft();
                    Render<?> rp = mc.getRenderManager().getEntityRenderObject(entityIn);
                    if (rp instanceof RenderPlayer) { smallArms = ((IRenderPlayerMixin) rp).getSmallArms(); }
                }
                leftArmChild.smallArms = smallArms;
                rightArmChild.smallArms = smallArms;

                bipedBody.render(scale);
                bipedRightArm.render(scale);
                bipedLeftArm.render(scale);
            }
        }
        if (slot == EntityEquipmentSlot.LEGS) {
            ItemStack stack = living.getItemStackFromSlot(slot);
            if (stack.getItem() instanceof CustomArmorOBJ) {
                leftLegChild.setOBJModel(((CustomArmorOBJ) stack.getItem()).getParameterizedModel(EnumParts.LEG_LEFT_FULL, stack, null, false));
                rightLegChild.setOBJModel(((CustomArmorOBJ) stack.getItem()).getParameterizedModel(EnumParts.LEG_RIGHT_FULL, stack, null, false));
                beltChild.setOBJModel(((CustomArmorOBJ) stack.getItem()).getParameterizedModel(EnumParts.BELT, stack, null, false));

                bipedRightLeg.render(scale);
                bipedLeftLeg.render(scale);
                bipedBelt.render(scale);
            }
        }
        if (slot == EntityEquipmentSlot.FEET) {
            ItemStack stack = living.getItemStackFromSlot(slot);
            if (stack.getItem() instanceof CustomArmorOBJ) {
                leftFeetChild.setOBJModel(((CustomArmorOBJ) stack.getItem()).getParameterizedModel(EnumParts.FEET_LEFT, stack, null, false));
                rightFeetChild.setOBJModel(((CustomArmorOBJ) stack.getItem()).getParameterizedModel(EnumParts.FEET_RIGHT, stack, null, false));

                bipedRightFeet.render(scale);
                bipedLeftFeet.render(scale);
            }
        }
        GlStateManager.popMatrix();
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, @Nonnull Entity entityIn) {
        if (entityIn instanceof EntityLivingBase) {
            Render<?> re = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(entityIn);
            ModelBiped source = null;
            if (re instanceof RenderLivingBase) {
                ModelBase model = ((RenderLivingBase<?>) re).getMainModel();
                if (model instanceof ModelBiped) { source = (ModelBiped) model; }
            }
            isSneak = entityIn.isSneaking();
            isRiding = entityIn.isRiding();
            isChild = ((EntityLivingBase) entityIn).isChild();
            if (source != null) {
                resetPos(bipedHead, source.bipedHead);
                resetPos(bipedBody, source.bipedBody);
                resetPos(bipedBelt, source.bipedBody);
                resetPos(bipedLeftArm, source.bipedLeftArm);
                resetPos(bipedRightArm, source.bipedRightArm);
                resetPos(bipedLeftLeg, source.bipedLeftLeg);
                resetPos(bipedRightLeg, source.bipedRightLeg);
                resetPos(bipedBelt, source.bipedBody);
                resetPos(bipedLeftFeet, source.bipedLeftLeg);
                resetPos(bipedRightFeet, source.bipedRightLeg);
            }
        }
        setVisible(true);
    }

    private void resetPos(ModelRenderer part, ModelRenderer source) {
        part.offsetX = source.offsetX;
        part.offsetY = source.offsetY;
        part.offsetZ = source.offsetZ;
        copyModelAngles(source, part);
    }

}
