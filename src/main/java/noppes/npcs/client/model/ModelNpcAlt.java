package noppes.npcs.client.model;

import java.util.*;

import moe.plushie.armourers_workshop.api.ArmourersWorkshopApi;
import moe.plushie.armourers_workshop.api.common.capability.IEntitySkinCapability;
import moe.plushie.armourers_workshop.api.common.skin.data.ISkin;
import moe.plushie.armourers_workshop.api.common.skin.data.ISkinDescriptor;
import moe.plushie.armourers_workshop.api.common.skin.type.ISkinType;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import noppes.npcs.api.mixin.entity.player.IEntityPlayerMixin;
import noppes.npcs.api.util.IModelRenderer;
import noppes.npcs.client.model.animation.*;
import noppes.npcs.client.util.aw.ArmourersWorkshopUtil;
import noppes.npcs.mixin.client.model.IModelPlayerMixin;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.client.model.part.AnimData;
import noppes.npcs.client.model.part.head.ModelHeadwear;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPC64x32;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityNpcAlex;
import noppes.npcs.entity.EntityNpcClassicPlayer;
import noppes.npcs.entity.data.DataAnimation;

import javax.annotation.Nonnull;

/* Used as an NPC model */
public class ModelNpcAlt extends ModelPlayer {

    public static final AnimData editAnimDataSelect = new AnimData();
    public static final Map<Integer, Map<Integer, List<ModelRendererAlt>>> animAddedChildren = new HashMap<>(); // { animation ID, [part ID, list<ModelRender>]}

    public static void copyModelAngles(ModelRendererAlt source, ModelRendererAlt dest) {
        dest.copyModelAngles(source);
    }

    protected ModelHeadwear bipedHeadwear_64;
    protected ModelHeadwear bipedHeadwear_128;
    protected ModelHeadwear bipedHeadwear_256;
    protected ModelHeadwear bipedHeadwear_512;
    protected ModelHeadwear bipedHeadwear_1024;
    protected ModelHeadwear bipedHeadwear_2048;
    protected ModelHeadwear bipedHeadwear_4096;

    protected ModelHeadwear bipedHeadwear_64_old;
    protected ModelHeadwear bipedHeadwear_128_old;
    protected ModelHeadwear bipedHeadwear_256_old;
    protected ModelHeadwear bipedHeadwear_512_old;
    protected ModelHeadwear bipedHeadwear_1024_old;
    protected ModelHeadwear bipedHeadwear_2048_old;
    protected ModelHeadwear bipedHeadwear_4096_old;
    protected ModelRendererAlt bipedCape;
    public AnimationStack rightStackData = new AnimationStack(7);
    public AnimationStack leftStackData = new AnimationStack(6);
    public boolean smallArmsIn;
    public boolean isClassicPlayer;

    public static void loadAnimationModel(AnimationConfig animation) {
        if (animation == null) { return; }
        animAddedChildren.remove(animation.id);
        if (animation.addParts.isEmpty()) {
            // this model
            Render<Entity> render = Minecraft.getMinecraft().getRenderManager().getEntityClassRenderObject(EntityCustomNpc.class);
            if (render instanceof RenderLiving) {
                ModelBase thisModel = ((RenderLiving<?>) render).getMainModel();
                // parts
                if (!animAddedChildren.containsKey(animation.id)) { animAddedChildren.put(animation.id, new TreeMap<>()); }
                Map<Integer, List<ModelRendererAlt>> map = animAddedChildren.get(animation.id);
                map.clear();
                // create all
                for (int partID : animation.addParts.keySet()) {
                    for (AddedPartConfig addedPartConfig : animation.addParts.get(partID)) {
                        if (!map.containsKey(addedPartConfig.parentPart)) {
                            map.put(addedPartConfig.parentPart, new ArrayList<>());
                        }
                        map.get(addedPartConfig.parentPart).add(new ModelRendererAlt(thisModel, addedPartConfig));
                    }
                }
                // put children
            }
        }
    }

    private static void addChildren(ModelRendererAlt modelRender, Map<Integer, List<ModelRendererAlt>> map) {
        List<ModelRendererAlt> children = map.get(modelRender.parentPartId);
        if (children == null || children.isEmpty()) {
            return;
        }
        for (ModelRendererAlt child : children) {
            modelRender.addChild(child);
            addChildren(child, map);
        }
    }

    public ModelNpcAlt(float modelSize, boolean smallArms, boolean classicPlayer) {
        super(modelSize, smallArms);
        smallArmsIn = smallArms;
        isClassicPlayer = classicPlayer;
        init(modelSize);
    }

    protected void init(float modelSize) {
        float wear = 0.25f;
        bipedHead = new ModelRendererAlt(this, EnumParts.HEAD, 0, 0, true);
        ((ModelRendererAlt) bipedHead).setBox(-4.0F, -8.0F, -4.0F, 8, 3 , 3, 2, 8, modelSize);
        bipedHead.setRotationPoint(0.0F, 0.0F, 0.0F);

        bipedHeadwear = new ModelRendererAlt(this, EnumParts.HEAD, 32, 0, true);
        ((ModelRendererAlt) bipedHeadwear).setBox(-4.0F, -8.0F, -4.0F, 8, 3 , 3, 2, 8, modelSize + 2.0f * wear);
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
        bipedBodyWear = new ModelRendererAlt(this, EnumParts.BODY, 16, 32, false);
        ((ModelRendererAlt) bipedBodyWear).setBox(-4.0F, 0.0F, -2.0F, 8, 5.5f, 4.0f, 2.5f, 4, modelSize + wear);
        bipedBodyWear.setRotationPoint(0.0F, 0.0F, 0.0F);

        bipedCape = new ModelRendererAlt(this, EnumParts.BODY, 0, 0, true);
        bipedCape.setTextureSize(64, 32);
        bipedCape.setBox(-5.0F, 0.0F, -1.0F, 10, 9, 5, 2, 1, modelSize);
        bipedCape.setRotationPoint(0.0F, 0.0F, 0.0F);
        ((IModelPlayerMixin) this).setBipedCape(bipedCape);

        float handWidth = smallArmsIn ? 3.0f : 4.0f;
        bipedRightArm = new ModelRendererAlt(this, EnumParts.ARM_RIGHT, 40, 16, false);
        ((ModelRendererAlt) bipedRightArm).setBox(smallArmsIn ? -2.0F : -3.0F, -2.0F, -2.0F, handWidth, 5.5f, 3.5f, 3.0f, 4, modelSize);
        bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
        bipedRightArmwear = new ModelRendererAlt(this, EnumParts.ARM_RIGHT, 40, 32, false);
        ((ModelRendererAlt) bipedRightArmwear).setBox(smallArmsIn ? -2.0F : -3.0F, -2.0F, -2.0F, handWidth, 5.5f, 3.5f, 3.0f, 4, modelSize + wear);
        bipedRightArmwear.setRotationPoint(-5.0F, 2.0F, 10.0F);

        bipedRightLeg = new ModelRendererAlt(this, EnumParts.LEG_RIGHT, 0, 16, false);
        ((ModelRendererAlt) bipedRightLeg).setBox(-2.0F, 0.0F, -2.1F, 4, 5.5f, 3.5f, 3.0f, 4, modelSize);
        bipedRightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);
        bipedRightLegwear = new ModelRendererAlt(this, EnumParts.LEG_RIGHT, 0, 32, false);
        ((ModelRendererAlt) bipedRightLegwear).setBox(-2.0F, 0.0F, -2.1F, 4, 5.5f, 3.5f, 3.0f, 4, modelSize + wear);
        bipedRightLegwear.setRotationPoint(-2.0F, 12.0F, 0.0F);

        bipedLeftArm = new ModelRendererAlt(this, EnumParts.ARM_LEFT, 32, 48, false);
        ((ModelRendererAlt) bipedLeftArm).setBox(-1.0F, -2.0F, -2.0F, handWidth, 5.5f, 3.5f, 3.0f, 4, modelSize);
        bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
        bipedLeftArmwear = new ModelRendererAlt(this, EnumParts.ARM_LEFT, 48, 48, false);
        ((ModelRendererAlt) bipedLeftArmwear).setBox(-1.0F, -2.0F, -2.0F, handWidth, 5.5f, 3.5f, 3.0f, 4, modelSize + wear);
        bipedLeftArmwear.setRotationPoint(5.0F, 2.0F, 0.0F);

        bipedLeftLeg = new ModelRendererAlt(this, EnumParts.LEG_LEFT, 16, 48, false);
        ((ModelRendererAlt) bipedLeftLeg).setBox(-2.2F, 0.0F, -2.1F, 4, 5.5f, 3.5f, 3.0f, 4, modelSize);
        bipedLeftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);
        bipedLeftLegwear = new ModelRendererAlt(this, EnumParts.LEG_LEFT, 0, 48, false);
        ((ModelRendererAlt) bipedLeftLegwear).setBox(-2.2F, 0.0F, -2.1F, 4, 5.5f, 3.5f, 3.0f, 4, modelSize + wear);
        bipedLeftLegwear.setRotationPoint(2.0F, 12.0F, 0.0F);
    }

    protected @Nonnull EnumHandSide getMainHand(@Nonnull Entity entityIn) {
        if (!(entityIn instanceof EntityLivingBase) || !((EntityLivingBase) entityIn).isSwingInProgress) {
            return super.getMainHand(entityIn);
        }
        EntityLivingBase living = (EntityLivingBase) entityIn;
        if (living.swingingHand == EnumHand.MAIN_HAND) {
            return EnumHandSide.RIGHT;
        }
        return EnumHandSide.LEFT;
    }

    @Override
    public void render(@Nonnull Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (Minecraft.getMinecraft().currentScreen == null && editAnimDataSelect.part != -1) { editAnimDataSelect.clear(); }
        Map<EnumParts, Boolean> ba = new HashMap<>();
        Map<EnumParts, Boolean> bAW = new HashMap<>();
        editAnimDataSelect.isNPC = entityIn.equals(editAnimDataSelect.displayNpc);
        float r = 1.0f, g = 1.0f, b = 1.0f;
        int animID = -1;
        boolean showWear = true;
        if (entityIn instanceof EntityPlayer) {
            ba.putAll(PlayerData.get((EntityPlayer) entityIn).animation.showParts);
            IEntityPlayerMixin playerMixin = (IEntityPlayerMixin) entityIn;
            if (playerMixin.npcs$getAnimation().isAnimated()) {
                animID = playerMixin.npcs$getAnimation().getAnimation().id;
            }
        }
        else if (entityIn instanceof EntityCustomNpc) {
            EntityCustomNpc npc = (EntityCustomNpc) entityIn;
            showWear = !npc.modelData.isDisableLayer("LayerWear");
            ba.putAll(npc.animation.showParts);
            EnumParts ep = EnumParts.get(editAnimDataSelect.part);
            if (editAnimDataSelect.isNPC && ba.containsKey(ep) && !ba.get(ep)) {
                ba.put(ep, true);
            }
            if (npc.animation.isAnimated()) {
                animID = npc.animation.getAnimation().id;
            }
            // possible disabling of body parts rendering from the AW mod:
            if (ArmourersWorkshopApi.isAvailable()) {
                npc.animation.resetShowAWParts();
                Map<EnumParts, List<ISkinDescriptor>> map = new HashMap<>();
                map.put(EnumParts.HEAD, new ArrayList<>());
                map.put(EnumParts.BODY, new ArrayList<>());
                map.put(EnumParts.ARM_LEFT, new ArrayList<>());
                map.put(EnumParts.ARM_RIGHT, new ArrayList<>());
                map.put(EnumParts.LEG_LEFT, new ArrayList<>());
                map.put(EnumParts.LEG_RIGHT, new ArrayList<>());
                for (int i = 0; i < 4; i++) {
                    if (npc.inventory.getArmor(i) == null) { continue; }
                    ItemStack stack = npc.inventory.getArmor(i).getMCItemStack();
                    if (stack != null && ArmourersWorkshopApi.getSkinNBTUtils().hasSkinDescriptor(stack)) {
                        ISkinDescriptor skinDescriptor = ArmourersWorkshopApi.getSkinNBTUtils().getSkinDescriptor(stack);
                        if (i == 0) {
                            map.get(EnumParts.HEAD).add(skinDescriptor);
                        } else if (i == 1) {
                            map.get(EnumParts.BODY).add(skinDescriptor);
                            map.get(EnumParts.ARM_RIGHT).add(skinDescriptor);
                            map.get(EnumParts.ARM_LEFT).add(skinDescriptor);
                        } else {
                            map.get(EnumParts.LEG_RIGHT).add(skinDescriptor);
                            map.get(EnumParts.LEG_LEFT).add(skinDescriptor);
                        }
                    }
                }
                if (npc.inventory.awItems.get(0) != null && ArmourersWorkshopApi.getSkinNBTUtils().hasSkinDescriptor(npc.inventory.awItems.get(0).getMCItemStack())) {
                    ISkinDescriptor skinDescriptor = ArmourersWorkshopApi.getSkinNBTUtils().getSkinDescriptor(npc.inventory.awItems.get(0).getMCItemStack());
                    map.get(EnumParts.HEAD).add(skinDescriptor);
                    map.get(EnumParts.BODY).add(skinDescriptor);
                    map.get(EnumParts.ARM_RIGHT).add(skinDescriptor);
                    map.get(EnumParts.ARM_LEFT).add(skinDescriptor);
                    map.get(EnumParts.LEG_RIGHT).add(skinDescriptor);
                    map.get(EnumParts.LEG_LEFT).add(skinDescriptor);
                }
                IEntitySkinCapability skinCapability = ArmourersWorkshopApi.getEntitySkinCapability(entityIn);
                ISkinType[] skinTypes = skinCapability.getValidSkinTypes();
                for (ISkinType skinType : skinTypes) {
                    if (skinType.getName().equals("Outfit") ||
                            skinType.getName().equals("Head") ||
                            skinType.getName().equals("Chest") ||
                            skinType.getName().equals("Legs") ||
                            skinType.getName().equals("Feet")) {
                        IInventory inv = ArmourersWorkshopUtil.getInstance().getSkinTypeInv(skinCapability.getSkinInventoryContainer(), skinType);
                        for (int j = 0; j < inv.getSizeInventory(); j++) {
                            if (ArmourersWorkshopApi.getSkinNBTUtils().hasSkinDescriptor(inv.getStackInSlot(j))) {
                                ISkinDescriptor skinDescriptor = ArmourersWorkshopApi.getSkinNBTUtils().getSkinDescriptor(inv.getStackInSlot(j));
                                switch (skinType.getName()) {
                                    case "Head": {
                                        map.get(EnumParts.HEAD).add(skinDescriptor);
                                        break;
                                    }
                                    case "Chest": {
                                        map.get(EnumParts.BODY).add(skinDescriptor);
                                        map.get(EnumParts.ARM_RIGHT).add(skinDescriptor);
                                        map.get(EnumParts.ARM_LEFT).add(skinDescriptor);
                                        break;
                                    }
                                    case "Outfit": {
                                        map.get(EnumParts.HEAD).add(skinDescriptor);
                                        map.get(EnumParts.BODY).add(skinDescriptor);
                                        map.get(EnumParts.ARM_RIGHT).add(skinDescriptor);
                                        map.get(EnumParts.ARM_LEFT).add(skinDescriptor);
                                        map.get(EnumParts.LEG_RIGHT).add(skinDescriptor);
                                        map.get(EnumParts.LEG_LEFT).add(skinDescriptor);
                                        break;
                                    }
                                    default: {
                                        map.get(EnumParts.LEG_RIGHT).add(skinDescriptor);
                                        map.get(EnumParts.LEG_LEFT).add(skinDescriptor);
                                    }
                                }
                            }
                        }
                    }
                }
                ArmourersWorkshopUtil awu = ArmourersWorkshopUtil.getInstance();
                for (EnumParts slot : map.keySet()) {
                    for (ISkinDescriptor skinDescriptor : map.get(slot)) {
                        try {
                            ISkin skin = (ISkin) awu.getSkin.invoke(awu.clientSkinCache, skinDescriptor);
                            if (skin != null) { awu.setShowPartFromArmor(slot, skin, npc); }
                        }
                        catch (Exception ignore) { }
                    }
                }
            }
            bAW.putAll(npc.animation.showAWParts);
            if (((EntityCustomNpc) entityIn).display.getTint() != 0xFFFFFF) {
                r = (float)(((EntityCustomNpc) entityIn).display.getTint() >> 16 & 255) / 255.0F;
                g = (float)(((EntityCustomNpc) entityIn).display.getTint() >> 8 & 255) / 255.0F;
                b = (float)(((EntityCustomNpc) entityIn).display.getTint() & 255) / 255.0F;
            }
        }
        Map<Integer, List<ModelRendererAlt>> animatedMap = animAddedChildren.get(animID);
        int entitySkinTextureID = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager.pushMatrix();
        if (!isChild && entityIn.isSneaking()) { GlStateManager.translate(0.0F, 0.2F, 0.0F); }
        // Custom parts
        if (animatedMap != null && animatedMap.containsKey(-1)) {
            for (ModelRendererAlt modelRenderer : animatedMap.get(-1)) {
                modelRenderer.checkBacklightColor(r,g,b);
                modelRenderer.render(scale);
            }
        }
        if (ba.get(EnumParts.HEAD) && bAW.get(EnumParts.HEAD) && bipedHead.showModel) {
            ((ModelRendererAlt) bipedHead).checkBacklightColor(r, g, b);
            if (isChild) {
                GlStateManager.pushMatrix();
                GlStateManager.scale(0.75F, 0.75F, 0.75F);
                GlStateManager.translate(0.0F, 16.0F * scale, 0.0F);

                List<ModelRendererAlt> list = null;
                if (animatedMap != null) { list = animatedMap.get(0); }
                if (list != null) {
                    for (ModelRendererAlt child : list) {
                        child.checkBacklightColor(r,g,b);
                        bipedHead.addChild(child);
                    }
                }
                bipedHead.render(scale);
                if (list != null) {
                    for (ModelRendererAlt child : list) { bipedHead.childModels.remove(child); }
                }

                if (((ModelRendererAlt) bipedHead).notOBJModel() && showWear) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID);
                    renderHeadWear(scale);
                }
                GlStateManager.popMatrix();
            }
            else {
                List<ModelRendererAlt> list = null;
                if (animatedMap != null) { list = animatedMap.get(0); }
                if (list != null) {
                    for (ModelRendererAlt child : list) {
                        child.checkBacklightColor(r,g,b);
                        bipedHead.addChild(child);
                    }
                }
                bipedHead.render(scale);
                if (list != null) {
                    for (ModelRendererAlt child : list) { bipedHead.childModels.remove(child); }
                }

                if (((ModelRendererAlt) bipedHead).notOBJModel() && showWear) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID);
                    renderHeadWear(scale);
                }
            }
        }
        if (isChild) {
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
        }
        if (ba.get(EnumParts.BODY) && bAW.get(EnumParts.BODY) && bipedBody.showModel) {
            ((ModelRendererAlt) bipedBody).checkBacklightColor(r, g, b);
            if (((ModelRendererAlt) bipedBody).notOBJModel()) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID); }

            List<ModelRendererAlt> list = null;
            if (animatedMap != null) { list = animatedMap.get(3); }
            if (list != null) {
                for (ModelRendererAlt child : list) {
                    child.checkBacklightColor(r,g,b);
                    bipedBody.addChild(child);
                }
            }
            bipedBody.render(scale);
            if (list != null) {
                for (ModelRendererAlt child : list) { bipedBody.childModels.remove(child); }
            }

            if (bipedBodyWear != null && ((ModelRendererAlt) bipedBody).notOBJModel() && showWear) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID);
                bipedBodyWear.render(scale);
            }
        }
        if (ba.get(EnumParts.ARM_RIGHT) && bAW.get(EnumParts.ARM_RIGHT) && bipedRightArm.showModel) {
            ((ModelRendererAlt) bipedRightArm).checkBacklightColor(r, g, b);
            if (((ModelRendererAlt) bipedRightArm).notOBJModel()) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID); }

            List<ModelRendererAlt> list = null;
            if (animatedMap != null) { list = animatedMap.get(2); }
            if (list != null) {
                for (ModelRendererAlt child : list) {
                    child.checkBacklightColor(r,g,b);
                    bipedRightArm.addChild(child);
                }
            }
            bipedRightArm.render(scale);
            if (list != null) {
                for (ModelRendererAlt child : list) { bipedRightArm.childModels.remove(child); }
            }
            if (bipedRightArmwear != null && ((ModelRendererAlt) bipedLeftArm).notOBJModel() && showWear) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID);
                bipedRightArmwear.render(scale);
            }
        }
        if (ba.get(EnumParts.ARM_LEFT) && bAW.get(EnumParts.ARM_LEFT) && bipedLeftArm.showModel) {
            ((ModelRendererAlt) bipedLeftArm).checkBacklightColor(r, g, b);
            if (((ModelRendererAlt) bipedLeftArm).notOBJModel()) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID); }

            List<ModelRendererAlt> list = null;
            if (animatedMap != null) { list = animatedMap.get(1); }
            if (list != null) {
                for (ModelRendererAlt child : list) {
                    child.checkBacklightColor(r,g,b);
                    bipedLeftArm.addChild(child);
                }
            }
            bipedLeftArm.render(scale);
            if (list != null) {
                for (ModelRendererAlt child : list) { bipedLeftArm.childModels.remove(child); }
            }
            if (bipedLeftArmwear != null && ((ModelRendererAlt) bipedLeftArm).notOBJModel() && showWear) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID);
                bipedLeftArmwear.render(scale);
            }
        }
        if (ba.get(EnumParts.LEG_RIGHT) && bAW.get(EnumParts.LEG_RIGHT) && bipedRightLeg.showModel) {
            ((ModelRendererAlt) bipedRightLeg).checkBacklightColor(r, g, b);
            if (((ModelRendererAlt) bipedRightLeg).notOBJModel()) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID); }

            List<ModelRendererAlt> list = null;
            if (animatedMap != null) { list = animatedMap.get(5); }
            if (list != null) {
                for (ModelRendererAlt child : list) {
                    child.checkBacklightColor(r,g,b);
                    bipedRightLeg.addChild(child);
                }
            }
            bipedRightLeg.render(scale);
            if (list != null) {
                for (ModelRendererAlt child : list) { bipedRightLeg.childModels.remove(child); }
            }

            if (bipedRightLegwear != null && ((ModelRendererAlt) bipedRightLeg).notOBJModel() && showWear) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID);
                bipedRightLegwear.render(scale);
            }
        }
        if (ba.get(EnumParts.LEG_LEFT) && bAW.get(EnumParts.LEG_LEFT) && bipedLeftLeg.showModel) {
            ((ModelRendererAlt) bipedLeftLeg).checkBacklightColor(r, g, b);
            if (((ModelRendererAlt) bipedLeftLeg).notOBJModel()) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID); }

            List<ModelRendererAlt> list = null;
            if (animatedMap != null) { list = animatedMap.get(4); }
            if (list != null) {
                for (ModelRendererAlt child : list) {
                    child.checkBacklightColor(r,g,b);
                    bipedLeftLeg.addChild(child);
                }
            }
            bipedLeftLeg.render(scale);
            if (list != null) {
                for (ModelRendererAlt child : list) { bipedLeftLeg.childModels.remove(child); }
            }

            if (bipedLeftLegwear != null && ((ModelRendererAlt) bipedLeftLeg).notOBJModel() && showWear) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, entitySkinTextureID);
                bipedLeftLegwear.render(scale);
            }
        }
        GlStateManager.popMatrix();
    }

    protected void renderHeadWear(float scale) {
        if (CustomNpcs.HeadWearType == 2) { return; }
        ModelRenderer bipedHead = bipedHeadwear;
        if (CustomNpcs.HeadWearType == 1) {
            int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            boolean isOld = w > h;
            if (w >= 4096) {
                if (isOld && bipedHeadwear_4096_old != null) {
                    bipedHead = bipedHeadwear_4096_old;
                }
                else if (!isOld && bipedHeadwear_4096 != null) {
                    bipedHead = bipedHeadwear_4096;
                }
            }
            else if (w >= 2048) {
                if (isOld && bipedHeadwear_2048_old != null) {
                    bipedHead = bipedHeadwear_2048_old;
                }
                else if (!isOld && bipedHeadwear_2048 != null) {
                    bipedHead = bipedHeadwear_2048;
                }
            }
            else if (w >= 1024) {
                if (isOld && bipedHeadwear_1024_old != null) {
                    bipedHead = bipedHeadwear_1024_old;
                }
                else if (!isOld && bipedHeadwear_1024 != null) {
                    bipedHead = bipedHeadwear_1024;
                }
            }
            else if (w >= 512) {
                if (isOld && bipedHeadwear_512_old != null) {
                    bipedHead = bipedHeadwear_512_old;
                }
                else if (!isOld && bipedHeadwear_512 != null) {
                    bipedHead = bipedHeadwear_512;
                }
            }
            else if (w >= 256) {
                if (isOld && bipedHeadwear_256_old != null) {
                    bipedHead = bipedHeadwear_256_old;
                }
                else if (!isOld && bipedHeadwear_256 != null) {
                    bipedHead = bipedHeadwear_256;
                }
            }
            else if (w >= 128) {
                if (isOld && bipedHeadwear_128_old != null) {
                    bipedHead = bipedHeadwear_128_old;
                }
                else if (!isOld && bipedHeadwear_128 != null) {
                    bipedHead = bipedHeadwear_128;
                }
            }
            else {
                if (isOld && bipedHeadwear_64_old != null) {
                    bipedHead = bipedHeadwear_64_old;
                } else if (!isOld && bipedHeadwear_64 != null) {
                    bipedHead = bipedHeadwear_64;
                }
            }
        }
        bipedHead.render(scale);
    }

    @Override
    public void renderCape(float scale) {
        bipedCape.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, @Nonnull Entity entityIn) {
        DataAnimation animation = null;
        if (entityIn instanceof EntityPlayer) {
            PlayerData data = PlayerData.get((EntityPlayer) entityIn);
            animation = data.animation;
            isSneak = entityIn.isSneaking();
            if (isSneak && ((EntityPlayer) entityIn).isPlayerSleeping()) { isSneak = false; }
        }
        else if (entityIn instanceof EntityCustomNpc) {
            if (entityIn instanceof EntityNPC64x32 || entityIn instanceof EntityNpcAlex || entityIn instanceof EntityNpcClassicPlayer) {
                for (Entity e : entityIn.world.getLoadedEntityList()) {
                    if (e.posX == entityIn.posX && e.posY == entityIn.posY && e.posZ == entityIn.posZ && e.getName().equals(entityIn.getName())) {
                        entityIn = e;
                        break;
                    }
                }
            }
            EntityCustomNpc npc = (EntityCustomNpc) entityIn;
            animation = npc.animation;
            ((IModelPlayerMixin) this).setBipedCape(bipedCape);
            if (!isRiding) { isRiding = npc.currentAnimation == 1; }
            if (npc.currentAnimation == 6 || (npc.inventory.getProjectile() != null && npc.isAttacking() && npc.stats.ranged.getHasAimAnimation())) {
                rightArmPose = ModelBiped.ArmPose.BOW_AND_ARROW;
            }
            isSneak = entityIn.isSneaking();
            if (isSneak && npc.isPlayerSleeping()) { isSneak = false; }

            // Standard Rotation
            if (npc.hurtTime != 0 && animation.isAnimated(AnimationKind.HIT)) {
                limbSwingAmount = 0.0f;
            }
            else if (npc.isSwingInProgress && animation.isAnimated(AnimationKind.SWING, AnimationKind.ATTACKING)) {
                swingProgress = 0.0f;
                npc.swingProgress = 0.0f;
                npc.swingProgressInt = 0;
                npc.prevSwingProgress = 0.0f;
                npc.isSwingInProgress = false;
            }
        }
        clearAllRotations();
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        if (isClassicPlayer) {
            AniClassicPlayer.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn, this);
        }
        // Parts scales
        if (entityIn instanceof EntityCustomNpc) {
            EntityCustomNpc npc = (EntityCustomNpc) entityIn;
            if (npc.isAttacking() && npc.inventory.getProjectile() != null && npc.getDataManager().get(EntityNPCInterface.AimRotationYaw) != npc.rotationYawHead) {
                if (!CustomNpcs.ShowCustomAnimation || !npc.animation.isAnimated(AnimationKind.ATTACKING, AnimationKind.INIT, AnimationKind.INTERACT, AnimationKind.DIES)) {
                    npc.rotationYawHead = updateRotation(npc.rotationYawHead, npc.getDataManager().get(EntityNPCInterface.AimRotationYaw));
                }
            }
            animation.resetShowParts();
            bipedHead.showModel = true;
            bipedBody.showModel = true;
            bipedLeftArm.showModel = true;
            bipedRightArm.showModel = true;
            bipedLeftLeg.showModel = true;
            bipedRightLeg.showModel = true;
            ((ModelRendererAlt) bipedHead).setBaseData(npc.modelData.getPartConfig(EnumParts.HEAD));
            ((ModelRendererAlt) bipedBody).setBaseData(npc.modelData.getPartConfig(EnumParts.BODY));
            ((ModelRendererAlt) bipedLeftArm).setBaseData(npc.modelData.getPartConfig(EnumParts.ARM_LEFT));
            ((ModelRendererAlt) bipedRightArm).setBaseData(npc.modelData.getPartConfig(EnumParts.ARM_RIGHT));
            ((ModelRendererAlt) bipedLeftLeg).setBaseData(npc.modelData.getPartConfig(EnumParts.LEG_LEFT));
            ((ModelRendererAlt) bipedRightLeg).setBaseData(npc.modelData.getPartConfig(EnumParts.LEG_RIGHT));
            boolean isAttacking = npc.isAttacking();
            if (!isAttacking && npc.currentAnimation == 0 && npc.isPlayerSleeping()) {
                // Vanilla animation
                if (bipedHead.rotateAngleX < 0.0f) {
                    bipedHead.rotateAngleX = 0.0f;
                }
            }
            if (npc.currentAnimation != 0) {
                if (npc.currentAnimation == 3) {
                    AniHug.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn, this);
                } else if (npc.currentAnimation == 5) {
                    AniDancing.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn, this);
                } else if (npc.currentAnimation == 7) {
                    AniCrawling.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn, this);
                } else if (npc.currentAnimation == 8) {
                    AniPoint.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn, this);
                } else if (npc.currentAnimation == 9) {
                    bipedHead.rotateAngleX = 0.7f;
                } else if (npc.currentAnimation == 10) {
                    AniWaving.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn, this);
                } else if (npc.currentAnimation == 11) {
                    AniBow.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn, this);
                } else if (npc.currentAnimation == 12) {
                    AniNo.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn, this);
                } else if (npc.currentAnimation == 13) {
                    AniYes.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn, this);
                }
            }
            // Mod base animation
            if (npc.currentAnimation != 2 && !npc.hasPath() && npc.ais.getStandingType() == 4 && npc.lookAt != null && !npc.isAttacking()) {
                double d0 = entityIn.posX - npc.lookAt.posX;
                double d1 = (entityIn.posY + (double) entityIn.getEyeHeight()) - (npc.lookAt.posY + (double) npc.lookAt.getEyeHeight());
                double d2 = entityIn.posZ - npc.lookAt.posZ;
                double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
                float yaw = MathHelper.wrapDegrees((float) npc.ais.orientation - (float)(MathHelper.atan2(d2, d0) * (180D / Math.PI)) - 90.0F);
                float pitch = MathHelper.wrapDegrees((float)(-(MathHelper.atan2(d1, d3) * (180D / Math.PI))));
                if (yaw < -45.0f) { yaw = -45.0f; }
                else if (yaw > 45.0f) { yaw = 45.0f; }
                bipedHead.rotateAngleY = (float) ((-yaw * Math.PI) / 180D);
                if (pitch < -45.0f) { pitch = -45.0f; }
                else if (pitch > 45.0f) { pitch = 45.0f; }
                bipedHead.rotateAngleX = (float) ((-pitch * Math.PI) / 180D);
                npc.rotationPitch = pitch;
            }
        }
        if (CustomNpcs.ShowCustomAnimation && animation != null) {
            if (animation.canSetBaseRotationAngles()) {
                ((ModelRendererAlt) bipedHead).putAnimation(animation);
                ((ModelRendererAlt) bipedBody).putAnimation(animation);
                ((ModelRendererAlt) bipedRightArm).putAnimation(animation);
                ((ModelRendererAlt) bipedLeftArm).putAnimation(animation);
                ((ModelRendererAlt) bipedRightLeg).putAnimation(animation);
                ((ModelRendererAlt) bipedLeftLeg).putAnimation(animation);
                rightStackData.putAnimation(animation);
                leftStackData.putAnimation(animation);
            }
            float partialTicks = 0.0f;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null || mc.currentScreen.isFocused()) { partialTicks = mc.getRenderPartialTicks(); }
            if (animation.isEmoted()) {
                animation.setRotationAnglesEmotion(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, partialTicks);
            }
            if (animation.canBeAnimated()) {
                animation.setRotationAnglesAnimation(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, partialTicks);
                if (animation.isAnimated()) {
                    if (animation.showParts.get(EnumParts.HEAD)) { ((ModelRendererAlt) bipedHead).setAnimation(animation); }
                    if (animation.showParts.get(EnumParts.BODY)) { ((ModelRendererAlt) bipedBody).setAnimation(animation); }
                    if (animation.showParts.get(EnumParts.ARM_RIGHT)) {
                        ((ModelRendererAlt) bipedRightArm).setAnimation(animation);
                        rightStackData.setAnimation(animation, EnumParts.RIGHT_STACK.patterns);
                    }
                    if (animation.showParts.get(EnumParts.ARM_LEFT)) {
                        ((ModelRendererAlt) bipedLeftArm).setAnimation(animation);
                        leftStackData.setAnimation(animation, EnumParts.LEFT_STACK.patterns);
                    }
                    if (animation.showParts.get(EnumParts.LEG_RIGHT)) { ((ModelRendererAlt) bipedRightLeg).setAnimation(animation); }
                    if (animation.showParts.get(EnumParts.LEG_LEFT)) { ((ModelRendererAlt) bipedLeftLeg).setAnimation(animation); }
                    if (animAddedChildren.containsKey(animation.getAnimation().id)) {
                        for (int partId : animAddedChildren.get(animation.getAnimation().id).keySet()) {
                            if (partId < 8) { continue; }
                            for (ModelRendererAlt renderModel : animAddedChildren.get(animation.getAnimation().id).get(partId)) {
                                renderModel.clearRotations();
                                renderModel.setAnimation(animation);
                            }
                        }
                    }
                }
            }
        }
        if (CustomNpcs.HeadWearType != 2) {
            copyModelAngles((ModelRendererAlt) bipedHead, (ModelRendererAlt) bipedHeadwear);
            if (bipedHeadwear_64 != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_64); }
            if (bipedHeadwear_128 != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_128); }
            if (bipedHeadwear_256 != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_256); }
            if (bipedHeadwear_512 != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_512); }
            if (bipedHeadwear_1024 != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_1024); }
            if (bipedHeadwear_2048 != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_2048); }
            if (bipedHeadwear_4096 != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_4096); }
            if (bipedHeadwear_64_old != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_64_old); }
            if (bipedHeadwear_128_old != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_128_old); }
            if (bipedHeadwear_256_old != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_256_old); }
            if (bipedHeadwear_512_old != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_512_old); }
            if (bipedHeadwear_1024_old != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_1024_old); }
            if (bipedHeadwear_2048_old != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_2048_old); }
            if (bipedHeadwear_4096_old != null) { copyModelAngles((ModelRendererAlt) bipedHead, bipedHeadwear_4096_old); }
        }
        if (bipedBody != null && bipedBodyWear != null)  {
            if (bipedBody instanceof ModelRendererAlt && bipedBodyWear instanceof ModelRendererAlt) { copyModelAngles((ModelRendererAlt) bipedBody, (ModelRendererAlt) bipedBodyWear); }
            else { copyModelAngles(bipedBody, bipedBodyWear); }
        }
        if (bipedRightArm != null && bipedRightArmwear != null)  {
            if (bipedRightArm instanceof ModelRendererAlt && bipedRightArmwear instanceof ModelRendererAlt) { copyModelAngles((ModelRendererAlt) bipedRightArm, (ModelRendererAlt) bipedRightArmwear); }
            else { copyModelAngles(bipedRightArm, bipedRightArmwear); }
        }
        if (bipedLeftArm != null && bipedLeftArmwear != null)  {
            if (bipedLeftArm instanceof ModelRendererAlt && bipedLeftArmwear instanceof ModelRendererAlt) { copyModelAngles((ModelRendererAlt) bipedLeftArm, (ModelRendererAlt) bipedLeftArmwear); }
            else { copyModelAngles(bipedLeftArm, bipedLeftArmwear); }
        }
        if (bipedRightLeg != null && bipedRightLegwear != null)  {
            if (bipedRightLeg instanceof ModelRendererAlt && bipedRightLegwear instanceof ModelRendererAlt) { copyModelAngles((ModelRendererAlt) bipedRightLeg, (ModelRendererAlt) bipedRightLegwear); }
            else { copyModelAngles(bipedRightLeg, bipedRightLegwear); }
        }
        if (bipedLeftLeg != null && bipedLeftLegwear != null)  {
            if (bipedLeftLeg instanceof ModelRendererAlt && bipedLeftLegwear instanceof ModelRendererAlt) { copyModelAngles((ModelRendererAlt) bipedLeftLeg, (ModelRendererAlt) bipedLeftLegwear); }
            else { copyModelAngles(bipedLeftLeg, bipedLeftLegwear); }
        }
    }

    @Override
    public void postRenderArm(float scale, @Nonnull EnumHandSide side) { // for ItemStacks
        super.postRenderArm(scale, side);
        ModelRendererAlt modelRenderer = (ModelRendererAlt) this.bipedRightArm;
        AnimationStack hundData = this.rightStackData;
        if (side == EnumHandSide.LEFT) {
            modelRenderer = (ModelRendererAlt) this.bipedLeftArm;
            hundData = this.leftStackData;
        }
        if (!hundData.showModel) { return; }
        if (!modelRenderer.isNormal) {
            if (modelRenderer.rotateAngleX1 != 0.0f) {
                float ofsY = modelRenderer.dy2 - modelRenderer.dy0;
                GlStateManager.translate(0.0f, 0.625f, 0.0f);
                float ofsZ = modelRenderer.rotateAngleX1 * (modelRenderer.dz / -2.0f) / (float) -Math.PI;
                GlStateManager.translate(0.0f, ofsY * -0.0625f, ofsZ * 0.0625f);
                GlStateManager.rotate(modelRenderer.rotateAngleX1 * 180.0f / (float) Math.PI, 1.0f, 0.0f, 0.0f);
                GlStateManager.translate(0.0f, ofsY * 0.0625f, ofsZ * -0.0625f);
                GlStateManager.translate(0.0f, -0.625f, 0.0f);
            }
            if (modelRenderer.rotateAngleY1 != 0.0f) {
                float ofs = (side == EnumHandSide.RIGHT ? -1.0f : 1.0f) * 0.0625f;
                GlStateManager.translate(ofs, 0.0f, 0.0f);
                GlStateManager.rotate(modelRenderer.rotateAngleY1 * -180.0f / (float) Math.PI, 0.0f, 1.0f, 0.0f);
                GlStateManager.translate(-ofs, 0.0f, 0.0f);
            }
        }
        if (hundData.partSets == null) { return; }
        if (hundData.partSets[3] != 0.0f || hundData.partSets[4] != 0.0f || hundData.partSets[5] != 0.0f) {
            GlStateManager.translate(hundData.partSets[3], hundData.partSets[4], hundData.partSets[5]);
        }
        if (hundData.partSets[2] != 0.0F) {
            GlStateManager.rotate(hundData.partSets[2] * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
        }
        if (hundData.partSets[1] != 0.0F) {
            GlStateManager.rotate(hundData.partSets[1] * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
        }
        if (hundData.partSets[0] != 0.0F) {
            GlStateManager.rotate(hundData.partSets[0] * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
        }
        if (hundData.partSets[6] != 1.0f || hundData.partSets[7] != 1.0f || hundData.partSets[8] != 1.0f) {
            GlStateManager.scale(hundData.partSets[6], hundData.partSets[7], hundData.partSets[7]);
        }
    }

    public void postRenderLeg(float scale, @Nonnull EnumHandSide side) {
        ModelRendererAlt modelRenderer = (ModelRendererAlt) (side == EnumHandSide.LEFT ? bipedLeftLeg : bipedRightLeg);
        if (modelRenderer.isHidden || !modelRenderer.showModel) { return; }
        if (smallArmsIn)  {
            float f = 0.5F * (float)(side == EnumHandSide.RIGHT ? 1 : -1);
            modelRenderer.rotationPointX += f;
            modelRenderer.postRender(scale);
            modelRenderer.rotationPointX -= f;
        }  else  { modelRenderer.postRender(scale); }
        if (!modelRenderer.isNormal) {
            rotateExtraPart(modelRenderer, side);
        }
    }

    public void postRenderBelt(float scale) {
        ModelRendererAlt modelRenderer = (ModelRendererAlt) bipedBody;
        if (modelRenderer.isHidden || !modelRenderer.showModel) { return; }
        modelRenderer.postRender(scale);
        if (!modelRenderer.isNormal) {
            rotateExtraPart(modelRenderer, null);
        }
    }

    private void rotateExtraPart(ModelRendererAlt modelRenderer, EnumHandSide side) {
        if (modelRenderer.rotateAngleX1 != 0.0f) {
            float ofsY = modelRenderer.dy2 - modelRenderer.dy0;
            float ofsZ = modelRenderer.rotateAngleX1 * (modelRenderer.dz / -2.0f) / (float) -Math.PI;
            float f = 0.0625f;
            GlStateManager.translate(0.0f, 0.725f - ofsY * f, ofsZ * f);
            GlStateManager.rotate(modelRenderer.rotateAngleX1 * 180.0f / (float) Math.PI, 1.0f, 0.0f, 0.0f);
            GlStateManager.translate(0.0f, ofsY * f - 0.725f, ofsZ * -f - f / 10.0f);
        }
        if (modelRenderer.rotateAngleY1 != 0.0f) {
            float ofs = side == null ? 0.0f : (side == EnumHandSide.RIGHT ? -1.0f : 1.0f) * 0.0625f;
            GlStateManager.translate(ofs, 0.0f, 0.0f);
            GlStateManager.rotate(modelRenderer.rotateAngleY1 * -180.0f / (float) Math.PI, 0.0f, 1.0f, 0.0f);
            GlStateManager.translate(-ofs, 0.0f, 0.0f);
        }
    }

    private float updateRotation(float base, float value) {
        float f = MathHelper.wrapDegrees(value - base);
        if (f > (float) 1.5) { f = (float) 1.5; }
        if (f < -(float) 1.5) { f = -(float) 1.5; }
        return base + f;
    }

    private void clearAllRotations() {
        // Head
        if (bipedHead.showModel) {
            ((ModelRendererAlt) bipedHead).clearRotations();
            bipedHeadwear.rotateAngleX = 0.0f;
            bipedHeadwear.rotateAngleZ = 0.0f;
        }
        // Body
        if (bipedBody.showModel) { ((ModelRendererAlt) bipedBody).clearRotations(); }
        if (bipedCape != null && bipedCape.showModel) { bipedCape.clearRotations(); }
        // Arm left
        if (bipedLeftArm.showModel) { ((ModelRendererAlt) bipedLeftArm).clearRotations(); }
        leftStackData.clear();
        // Arm right
        if (bipedRightArm.showModel) { ((ModelRendererAlt) bipedRightArm).clearRotations(); }
        rightStackData.clear();
        // Leg left
        if (bipedLeftLeg.showModel) {
            ((ModelRendererAlt) bipedLeftLeg).clearRotations();
            bipedLeftLeg.rotationPointX = 2.0f;
            bipedLeftLeg.rotationPointZ = 0.0f;
        }
        // Leg right
        if (bipedRightLeg.showModel) {
            ((ModelRendererAlt) bipedRightLeg).clearRotations();
            bipedRightLeg.rotationPointX = -2.0f;
            bipedRightLeg.rotationPointZ = 0.0f;
        }
    }

    public IModelRenderer getPart(int partId) {
        switch(partId) {
            case 0: return (IModelRenderer) bipedHead;
            case 1: return (IModelRenderer) bipedLeftArm;
            case 2: return (IModelRenderer) bipedRightArm;
            case 3: return (IModelRenderer) bipedBody;
            case 4: return (IModelRenderer) bipedLeftLeg;
            case 5: return (IModelRenderer) bipedRightLeg;
            case 6: return leftStackData;
            case 7: return rightStackData;
        }
        for (int i = 0; i < 6; i++) {
            ModelRendererAlt biped = (ModelRendererAlt) getPart(i);
            if (biped.childModels == null) { continue; }
            ModelRendererAlt child = getChildPart(biped.childModels, partId);
            if (child != null) { return child; }
        }
        return null;
    }

    private ModelRendererAlt getChildPart(List<ModelRenderer> childModels, int partId) {
        for (ModelRenderer mr : childModels) {
            if (mr instanceof ModelRendererAlt) {
                if (((ModelRendererAlt) mr).partId == partId) {
                    return (ModelRendererAlt) mr;
                }
                if (mr.childModels == null) { continue; }
                ModelRendererAlt child = getChildPart(mr.childModels, partId);
                if (child != null) { return child; }
            }
        }
        return null;
    }

}
