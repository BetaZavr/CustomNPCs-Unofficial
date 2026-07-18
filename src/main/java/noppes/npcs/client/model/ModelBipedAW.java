package noppes.npcs.client.model;

import moe.plushie.armourers_workshop.api.ArmourersWorkshopApi;
import moe.plushie.armourers_workshop.api.common.capability.IEntitySkinCapability;
import moe.plushie.armourers_workshop.api.common.capability.IWardrobeCap;
import moe.plushie.armourers_workshop.api.common.skin.data.ISkin;
import moe.plushie.armourers_workshop.api.common.skin.data.ISkinDescriptor;
import moe.plushie.armourers_workshop.api.common.skin.data.ISkinDye;
import moe.plushie.armourers_workshop.api.common.skin.type.ISkinType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.client.util.aw.ArmourersWorkshopUtil;
import noppes.npcs.client.util.aw.CustomSkinModelRenderHelper;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityNPCInterface;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelBipedAW extends ModelBipedAlt {

    public ModelBipedAW(float modelSize, boolean isArmorModel, boolean smallArmsIn, boolean isClassicPlayer) {
        super(modelSize, isArmorModel, smallArmsIn, isClassicPlayer);
    }

    @Override
    public void render(@Nonnull Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (!ArmourersWorkshopApi.isAvailable()) { return; }
        if (entityIn.equals(ModelNpcAlt.editAnimDataSelect.displayNpc) && !ModelNpcAlt.editAnimDataSelect.showArmor) { return; }
        ArmourersWorkshopUtil awu = ArmourersWorkshopUtil.getInstance();
        if (awu == null) { return; }
        double d = 0.0d;
        double distance = Minecraft.getMinecraft().player.getDistance(entityIn.posX, entityIn.posY, entityIn.posZ);
        try { d = (int) awu.renderDistanceSkin.get(awu.configHandlerClient); }
        catch (Exception e) { LogWriter.error(e); }
        if (distance > d) { return; }

        GlStateManager.pushMatrix();
        if (!isChild && entityIn.isSneaking()) { GlStateManager.translate(0.0f, 0.2f, 0.0f); }
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);

        IWardrobeCap wardrobe = ArmourersWorkshopApi.getEntityWardrobeCapability(entityIn);
        CustomSkinModelRenderHelper modelRenderer = CustomSkinModelRenderHelper.getInstance();

        EntityNPCInterface npc = (EntityNPCInterface) entityIn;
        Map<EnumParts, Boolean> ba = new HashMap<>(npc.animation.showParts);
        Map<EnumParts, Boolean> baArmor = new HashMap<>(npc.animation.showArmorParts);
        for (EnumParts ep : ba.keySet()) {
            if (!ba.get(ep)) { baArmor.put(ep, false); }
        }
        EnumParts ep = EnumParts.get(editAnimDataSelect.part);
        if (editAnimDataSelect.isNPC && !ba.get(ep)) {
            ba.put(ep, true);
            baArmor.put(ep, true);
        }

        String type = slot.getName().toLowerCase();
        // moe.plushie.armourers_workshop.common.skin.data.SkinDescriptor
        List<ISkinDescriptor> list = new ArrayList<>();
        List<ISkinDescriptor> outfits = new ArrayList<>();
        List<ISkinDescriptor> wings = new ArrayList<>();
        ItemStack stack = ItemStack.EMPTY;
        if (slot == EntityEquipmentSlot.HEAD && npc.inventory.getArmor(0) != null) {
            stack = npc.inventory.getArmor(0).getMCItemStack();
        }
        else if (slot == EntityEquipmentSlot.CHEST) {
            if (npc.inventory.awItems.get(0) != null && ArmourersWorkshopApi.getSkinNBTUtils().hasSkinDescriptor(npc.inventory.awItems.get(0).getMCItemStack())) {
                outfits.add(ArmourersWorkshopApi.getSkinNBTUtils().getSkinDescriptor(npc.inventory.awItems.get(0).getMCItemStack()));
            }
            if (npc.inventory.awItems.get(1) != null && ArmourersWorkshopApi.getSkinNBTUtils().hasSkinDescriptor(npc.inventory.awItems.get(1).getMCItemStack())) {
                wings.add(ArmourersWorkshopApi.getSkinNBTUtils().getSkinDescriptor(npc.inventory.awItems.get(1).getMCItemStack()));
            }
            if (npc.inventory.getArmor(1) != null) { stack = npc.inventory.getArmor(1).getMCItemStack(); }
        }
        else if (slot == EntityEquipmentSlot.LEGS && npc.inventory.getArmor(2) != null) {
            stack = npc.inventory.getArmor(2).getMCItemStack();
        }
        else if (slot == EntityEquipmentSlot.FEET && npc.inventory.getArmor(3) != null) {
           stack = npc.inventory.getArmor(3).getMCItemStack();
        }
        if (stack != null && ArmourersWorkshopApi.getSkinNBTUtils().hasSkinDescriptor(stack)) {
            list.add(ArmourersWorkshopApi.getSkinNBTUtils().getSkinDescriptor(stack));
        }
        IEntitySkinCapability skinCapability = ArmourersWorkshopApi.getEntitySkinCapability(entityIn);
        ISkinType[] skinTypes = skinCapability.getValidSkinTypes();
        for (ISkinType skinType : skinTypes) {
            String name = skinType.getName().toLowerCase();
            if (!name.contains(type) && !(slot == EntityEquipmentSlot.CHEST && (name.equals("wings") || name.equals("outfit")))) {
                continue;
            }
            IInventory inv = ArmourersWorkshopUtil.getInstance().getSkinTypeInv(skinCapability.getSkinInventoryContainer(), skinType);
            for (int j = 0; j < inv.getSizeInventory(); j++) {
                if (ArmourersWorkshopApi.getSkinNBTUtils().hasSkinDescriptor(inv.getStackInSlot(j))) {
                    ISkinDescriptor skinDescriptor = ArmourersWorkshopApi.getSkinNBTUtils().getSkinDescriptor(inv.getStackInSlot(j));
                    if (name.equals("outfit")) { outfits.add(skinDescriptor); }
                    if (name.equals("wings")) { wings.add(skinDescriptor); }
                    else { list.add(skinDescriptor); }
                }
            }
        }

        bipedHead.showModel = ba.get(EnumParts.HEAD) && baArmor.get(EnumParts.HEAD) && slot == EntityEquipmentSlot.HEAD;
        if (!list.isEmpty() && bipedHead.showModel) {
            for (ISkinDescriptor skinDescriptor : list) {
                try {
                    ISkin skin = (ISkin) awu.getSkin.invoke(awu.clientSkinCache, skinDescriptor);
                    if (skin != null) {
                        ISkinDye dye = (ISkinDye) awu.skinDyeConstructor.newInstance(wardrobe.getDye());
                        Object renderData = awu.skinRenderDataConstructor.newInstance(scale, dye, awu.extraColours, distance, true, true, false, DefaultPlayerSkin.getDefaultSkinLegacy());
                        modelRenderer.renderEquipmentPart(skin, renderData, npc, this, scale, baArmor);
                    }
                }
                catch (Exception ignore) { }
            }
        }

        bipedBody.showModel = ba.get(EnumParts.BODY) && baArmor.get(EnumParts.BODY) && slot == EntityEquipmentSlot.CHEST;
        bipedRightArm.showModel = ba.get(EnumParts.ARM_RIGHT) && baArmor.get(EnumParts.ARM_RIGHT) && slot == EntityEquipmentSlot.CHEST;
        bipedLeftArm.showModel = ba.get(EnumParts.ARM_LEFT) && baArmor.get(EnumParts.ARM_LEFT) && slot == EntityEquipmentSlot.CHEST;
        if (slot == EntityEquipmentSlot.CHEST) {
            if (!outfits.isEmpty()) {
                for (ISkinDescriptor skinDescriptor : outfits) {
                    try {
                        ISkin skin = (ISkin) awu.getSkin.invoke(awu.clientSkinCache, skinDescriptor);
                        if (skin != null) {
                            ISkinDye dye = (ISkinDye) awu.skinDyeConstructor.newInstance(wardrobe.getDye());
                            Object renderData = awu.skinRenderDataConstructor.newInstance(scale, dye, awu.extraColours, distance, true, true, false, DefaultPlayerSkin.getDefaultSkinLegacy());
                            modelRenderer.renderEquipmentPart(skin, renderData, npc, this, scale, baArmor);
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
            if (ba.get(EnumParts.BODY) && !wings.isEmpty()) {
                for (ISkinDescriptor skinDescriptor : wings) {
                    try {
                        ISkin skin = (ISkin) awu.getSkin.invoke(awu.clientSkinCache, skinDescriptor);
                        if (skin != null) {
                            ISkinDye dye = (ISkinDye) awu.skinDyeConstructor.newInstance(wardrobe.getDye());
                            Object renderData = awu.skinRenderDataConstructor.newInstance(scale, dye, awu.extraColours, distance, true, true, false, DefaultPlayerSkin.getDefaultSkinLegacy());
                            modelRenderer.renderEquipmentPart(skin, renderData, npc, this, scale, baArmor);
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        }
        if (!list.isEmpty() && bipedBody.showModel || bipedRightArm.showModel || bipedLeftArm.showModel) {
            for (ISkinDescriptor skinDescriptor : list) {
                try {
                    ISkin skin = (ISkin) awu.getSkin.invoke(awu.clientSkinCache, skinDescriptor);
                    if (skin != null) {
                        ISkinDye dye = (ISkinDye) awu.skinDyeConstructor.newInstance(wardrobe.getDye());
                        Object renderData = awu.skinRenderDataConstructor.newInstance(scale, dye, awu.extraColours, distance, true, true, false, DefaultPlayerSkin.getDefaultSkinLegacy());
                        modelRenderer.renderEquipmentPart(skin, renderData, npc, this, scale, baArmor);
                    }
                }
                catch (Exception ignore) { }
            }
        }

        bipedRightLeg.showModel = ba.get(EnumParts.LEG_RIGHT) && baArmor.get(EnumParts.LEG_RIGHT) && (slot == EntityEquipmentSlot.LEGS || slot == EntityEquipmentSlot.FEET);
        bipedLeftLeg.showModel = ba.get(EnumParts.LEG_LEFT) && baArmor.get(EnumParts.LEG_LEFT) && (slot == EntityEquipmentSlot.LEGS || slot == EntityEquipmentSlot.FEET);
        if (!list.isEmpty() && bipedRightLeg.showModel || bipedLeftLeg.showModel) {
            for (ISkinDescriptor skinDescriptor : list) {
                try {
                    ISkin skin = (ISkin) awu.getSkin.invoke(awu.clientSkinCache, skinDescriptor);
                    if (skin != null) {
                        ISkinDye dye = (ISkinDye) awu.skinDyeConstructor.newInstance(wardrobe.getDye());
                        Object renderData = awu.skinRenderDataConstructor.newInstance(scale, dye, awu.extraColours, distance, true, true, false, DefaultPlayerSkin.getDefaultSkinLegacy());
                        modelRenderer.renderEquipmentPart(skin, renderData, npc, this, scale, baArmor);
                    }
                }
                catch (Exception ignore) { }
            }
        }
        GlStateManager.popMatrix();
    }

}
