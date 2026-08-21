package noppes.npcs.items.custom;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.mixin.world.item.IArmorItemMixin;
import noppes.npcs.mixin.world.item.IItemMixin;
import noppes.npcs.util.Util;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.*;

public class CustomArmor extends ArmorItem implements ICustomElement {

    public static @Nonnull ArmorMaterials getMaterialArmor(@Nonnull String materialName) {
        return switch (materialName.toLowerCase()) {
            case "diamond" -> ArmorMaterials.DIAMOND;
            case "chain" -> ArmorMaterials.CHAIN;
            case "iron" -> ArmorMaterials.IRON;
            case "gold" -> ArmorMaterials.GOLD;
            case "turtle" -> ArmorMaterials.TURTLE;
            case "netherite" -> ArmorMaterials.NETHERITE;
            default -> ArmorMaterials.LEATHER;
        };
    }

    public static @Nonnull Type getSlotEquipment(@Nonnull String slotName) {
        return switch (slotName.toLowerCase()) {
            case "head" -> Type.HELMET;
            case "chest" -> Type.CHESTPLATE;
            case "legs" -> Type.LEGGINGS;
            default -> Type.BOOTS;
        };
    }


    protected final Multimap<Attribute, AttributeModifier> defaultModifiers;
    protected final Map<EnumParts, List<String>> parts = new HashMap<>();
    protected final Map<ItemDisplayContext, ItemTransform> cameraData = new HashMap<>();
    protected final @Nonnull CompoundTag nbtData;
    protected final ItemStack repairItemStack;
    protected final int enchantability;

    public ResourceLocation objModel = null;

    public CustomArmor(@Nonnull ArmorMaterial armorMaterial, @Nonnull Type itemType, @Nonnull Item.Properties properties,
                       int maxStDam, int damReAmt, int enchantabilityIn, float knockback, float tough, @Nonnull CompoundTag nbtItem) {
        super(armorMaterial, itemType, properties);
        nbtData = nbtItem;

        if (maxStDam > 1) { ((IItemMixin) this).setMaxDamage(maxStDam); }
        if (damReAmt > 0) { ((IArmorItemMixin) this).setDefense(damReAmt); }
        if (tough > 0.0f) { ((IArmorItemMixin) this).setToughness(tough); }
        if (enchantabilityIn > 0) { enchantability = enchantabilityIn; }
        else { enchantability = armorMaterial.getEnchantmentValue(); }
        if (nbtItem.contains("RepairItem", 10)) { repairItemStack = ItemStack.of(nbtItem.getCompound("RepairItem")); }
        else { repairItemStack = null; }
        if (knockback > 0) { ((IArmorItemMixin) this).setKnockbackResistance(knockback); }
        if (nbtData.contains("OBJData", 10)) {
            CompoundTag data = nbtData.getCompound("OBJData");
            ListTag tagList = data.getList("Head Mesh Names", 8);
            List<String> listHead = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listHead.add(tagList.getString(i)); }
            parts.put(EnumParts.HEAD, listHead);
            tagList = data.getList("Body Mesh Names", 8);
            List<String> listBody = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listBody.add(tagList.getString(i)); }
            parts.put(EnumParts.BODY, listBody);
            tagList = data.getList("Arm Right Mesh Names", 8);
            List<String> listArmRight = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listArmRight.add(tagList.getString(i)); }
            parts.put(EnumParts.ARM_RIGHT, listArmRight);
            tagList = data.getList("Wrist Right Mesh Names", 8);
            List<String> listWristRight = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listWristRight.add(tagList.getString(i)); }
            parts.put(EnumParts.WRIST_RIGHT, listWristRight);
            tagList = data.getList("Arm Left Mesh Names", 8);
            List<String> listArmLeft = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listArmLeft.add(tagList.getString(i)); }
            parts.put(EnumParts.ARM_LEFT, listArmLeft);
            tagList = data.getList("Wrist Left Mesh Names", 8);
            List<String> listWristLeft = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listWristLeft.add(tagList.getString(i)); }
            parts.put(EnumParts.WRIST_LEFT, listWristLeft);
            tagList = data.getList("Belt Mesh Names", 8);
            List<String> listBelt = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listBelt.add(tagList.getString(i)); }
            parts.put(EnumParts.BELT, listBelt);
            tagList = data.getList("Leg Right Mesh Names", 8);
            List<String> listLegRight = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listLegRight.add(tagList.getString(i)); }
            parts.put(EnumParts.LEG_RIGHT, listLegRight);
            tagList = data.getList("Foot Right Mesh Names", 8);
            List<String> listFootRight = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listFootRight.add(tagList.getString(i)); }
            parts.put(EnumParts.FOOT_RIGHT, listFootRight);
            tagList = data.getList("Leg Left Mesh Names", 8);
            List<String> listLegLeft = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listLegLeft.add(tagList.getString(i)); }
            parts.put(EnumParts.LEG_LEFT, listLegLeft);
            tagList = data.getList("Foot Left Mesh Names", 8);
            List<String> listFootLeft = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listFootLeft.add(tagList.getString(i)); }
            parts.put(EnumParts.FOOT_LEFT, listFootLeft);
            tagList = data.getList("Boot Right Mesh Names", 8);
            List<String> listBootRight = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listBootRight.add(tagList.getString(i)); }
            parts.put(EnumParts.FEET_RIGHT, listBootRight);
            tagList = data.getList("Boot Left Mesh Names", 8);
            List<String> listBootLeft = new ArrayList<>();
            for (int i = 0; i < tagList.size(); i++) { listBootLeft.add(tagList.getString(i)); }
            parts.put(EnumParts.FEET_LEFT, listBootLeft);
            objModel = new ResourceLocation(CustomNpcs.MODID, "models/armor/" + nbtItem.getString("RegistryName") + ".obj");
            if (Util.instance.getSide() == Dist.CLIENT) { createCameraData(); }
        }

        // vanilla
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        UUID uuid = IArmorItemMixin.getArmorModifiers().get(itemType);
        builder.put(Attributes.ARMOR, new AttributeModifier(uuid, "Armor modifier", ((IArmorItemMixin) this).defense(), AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(uuid, "Armor toughness", ((IArmorItemMixin) this).toughness(), AttributeModifier.Operation.ADDITION));
        if (knockbackResistance > 0) {
            builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(uuid, "Armor knockback resistance", knockbackResistance, AttributeModifier.Operation.ADDITION));
        }
        defaultModifiers = builder.build();
    }

    public List<String> getMeshNames(EnumParts slot) {
        if (parts.containsKey(slot)) { return parts.get(slot); }
        return new ArrayList<>();
    }

    private void createCameraData() {
        cameraData.clear();
        CompoundTag display = nbtData.contains("Display", 10) ? nbtData.getCompound("Display") : new CompoundTag();
        CompoundTag head = display.contains("HEAD", 10) ? nbtData.getCompound("HEAD") : new CompoundTag();
        CompoundTag chest = display.contains("CHEST", 10) ? nbtData.getCompound("CHEST") : new CompoundTag();
        CompoundTag legs = display.contains("LEGS", 10) ? nbtData.getCompound("LEGS") : new CompoundTag();
        CompoundTag feet = display.contains("FEET", 10) ? nbtData.getCompound("FEET") : new CompoundTag();
        for (ItemDisplayContext transformType : ItemDisplayContext.values()) {
            Vector3f rotation = new Vector3f();
            Vector3f translation = new Vector3f();
            Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);
            switch(transformType) {
                case THIRD_PERSON_LEFT_HAND: {
                    switch(getType()) {
                        case CHESTPLATE: {
                            if (!chest.contains("thirdperson_lefthand", 10)) {
                                translation.z = 0.5f;
                                scale.x = 0.5f;
                                scale.y = 0.5f;
                                scale.z = 0.5f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompound("thirdperson_lefthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case LEGGINGS: {
                            if (!legs.contains("thirdperson_lefthand", 10)) {
                                translation.x = -0.15f;
                                translation.y = 0.35f;
                                translation.z = 0.5f;
                                scale.x = 0.65f;
                                scale.y = 0.65f;
                                scale.z = 0.65f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompound("thirdperson_lefthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case BOOTS: {
                            if (!feet.contains("thirdperson_lefthand", 10)) {
                                rotation.x = 90.0f;
                                rotation.y = 180.0f;
                                translation.x = 1.15f;
                                translation.y = 0.5f;
                                translation.z = 0.5f;
                                scale.x = 0.65f;
                                scale.y = 0.65f;
                                scale.z = 0.65f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompound("thirdperson_lefthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        default: {
                            if (!head.contains("thirdperson_lefthand", 10)) {
                                rotation.y = 180.0f;
                                translation.x = 1.0f;
                                translation.y = -0.375f;
                                translation.z = 0.5f;
                                scale.x = 0.5f;
                                scale.y = 0.5f;
                                scale.z = 0.5f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, head.getCompound("thirdperson_lefthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                    }
                    break;
                }
                case THIRD_PERSON_RIGHT_HAND: {
                    switch(getType()) {
                        case CHESTPLATE: {
                            if (!chest.contains("thirdperson_righthand", 10)) {
                                translation.x = 0.5f;
                                translation.z = 0.5f;
                                scale.x = 0.5f;
                                scale.y = 0.5f;
                                scale.z = 0.5f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompound("thirdperson_righthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case LEGGINGS: {
                            if (!legs.contains("thirdperson_righthand", 10)) {
                                translation.x = 0.5f;
                                translation.y = 0.35f;
                                translation.z = 0.5f;
                                scale.x = 0.65f;
                                scale.y = 0.65f;
                                scale.z = 0.65f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompound("thirdperson_righthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case BOOTS: {
                            if (!feet.contains("firstperson_righthand", 10)) {
                                rotation.x = 90.0f;
                                rotation.y = 180.0f;
                                translation.x = 0.5f;
                                translation.y = 0.5f;
                                translation.z = 0.5f;
                                scale.x = 0.65f;
                                scale.y = 0.65f;
                                scale.z = 0.65f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompound("firstperson_righthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        default: {
                            if (!head.contains("thirdperson_righthand", 10)) {
                                rotation.y = 180.0f;
                                translation.x = 0.5f;
                                translation.y = -0.375f;
                                translation.z = 0.5f;
                                scale.x = 0.5f;
                                scale.y = 0.5f;
                                scale.z = 0.5f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, head.getCompound("thirdperson_righthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                    }
                    break;
                }
                case FIRST_PERSON_LEFT_HAND: {
                    switch(getType()) {
                        case CHESTPLATE: {
                            if (!chest.contains("firstperson_lefthand", 10)) {
                                rotation.y = 280.0f;
                                translation.x = 0.57f;
                                translation.y = 0.1f;
                                translation.z = -0.085f;
                                scale.x = 0.5f;
                                scale.y = 0.5f;
                                scale.z = 0.5f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompound("firstperson_lefthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case LEGGINGS: {
                            if (!legs.contains("firstperson_lefthand", 10)) {
                                rotation.y = 280.0f;
                                translation.x = 0.65f;
                                translation.y = 0.4f;
                                translation.z = -0.085f;
                                scale.x = 0.5f;
                                scale.y = 0.5f;
                                scale.z = 0.5f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompound("firstperson_lefthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case BOOTS: {
                            if (!feet.contains("firstperson_lefthand", 10)) {
                                rotation.y = 280.0f;
                                translation.x = 0.72f;
                                translation.y = 0.435f;
                                translation.z = -0.585f;
                                scale.x = 0.85f;
                                scale.y = 0.85f;
                                scale.z = 0.85f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompound("firstperson_lefthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        default: {
                            if (!head.contains("firstperson_lefthand", 10)) {
                                rotation.y = 280.0f;
                                translation.x = 0.57f;
                                translation.y = -0.225f;
                                translation.z = -0.085f;
                                scale.x = 0.5f;
                                scale.y = 0.5f;
                                scale.z = 0.5f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, head.getCompound("firstperson_lefthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                    }
                    break;
                }
                case FIRST_PERSON_RIGHT_HAND: {
                    switch(getType()) {
                        case CHESTPLATE: {
                            if (!chest.contains("firstperson_righthand", 10)) {
                                rotation.y = 280.0f;
                                translation.x = 0.85f;
                                translation.y = -0.1f;
                                translation.z = 0.2f;
                                scale.x = 0.6f;
                                scale.y = 0.6f;
                                scale.z = 0.6f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompound("firstperson_righthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case LEGGINGS: {
                            if (!legs.contains("firstperson_righthand", 10)) {
                                rotation.y = 280.0f;
                                translation.x = 0.95f;
                                translation.y = 0.25f;
                                translation.z = 0.2f;
                                scale.x = 0.6f;
                                scale.y = 0.6f;
                                scale.z = 0.6f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompound("firstperson_righthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case BOOTS: {
                            if (!feet.contains("firstperson_righthand", 10)) {
                                rotation.y = 280.0f;
                                translation.x = 0.95f;
                                translation.y = 0.4f;
                                translation.z = 0.2f;
                                scale.x = 0.85f;
                                scale.y = 0.85f;
                                scale.z = 0.85f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompound("firstperson_righthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        default: {
                            if (!head.contains("firstperson_righthand", 10)) {
                                rotation.y = 280.0f;
                                translation.x = 0.85f;
                                translation.y = -0.5f;
                                translation.z = 0.2f;
                                scale.x = 0.6f;
                                scale.y = 0.6f;
                                scale.z = 0.6f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, head.getCompound("firstperson_righthand"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                    }
                    break;
                }
                case HEAD: {
                    switch(getType()) {
                        case CHESTPLATE: {
                            if (!chest.contains("head", 10)) {
                                rotation.x = 270.0f;
                                translation.x = 0.5f;
                                translation.y = 1.0f;
                                translation.z = 1.65f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompound("head"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case LEGGINGS: {
                            if (!legs.contains("head", 10)) {
                                rotation.x = 270.0f;
                                translation.x = 0.5f;
                                translation.y = 1.0f;
                                translation.z = 1.0f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompound("head"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case BOOTS: {
                            if (!feet.contains("head", 10)) {
                                rotation.y = 180.0f;
                                translation.x = 0.5f;
                                translation.y = 0.925f;
                                translation.z = 0.4f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompound("head"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        default: { break; }
                    }
                    break;
                }
                case GUI: {
                    switch(getType()) {
                        case CHESTPLATE: {
                            if (!chest.contains("gui", 10)) {
                                rotation.x = 30.0f;
                                rotation.y = 45.0f;
                                translation.x = 0.49f;
                                translation.y = -0.41f;
                                scale.x = 0.9f;
                                scale.y = 0.9f;
                                scale.z = 0.9f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompound("gui"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case LEGGINGS: {
                            if (!legs.contains("gui", 10)) {
                                rotation.x = 30.0f;
                                rotation.y = 45.0f;
                                translation.x = 0.5f;
                                translation.y = 0.05f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompound("gui"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case BOOTS: {
                            if (!feet.contains("gui", 10)) {
                                rotation.x = 30.0f;
                                rotation.y = 45.0f;
                                translation.x = 0.5f;
                                translation.y = 0.3f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompound("gui"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        default: {
                            if (!head.contains("gui", 10)) {
                                rotation.x = 30.0f;
                                rotation.y = 45.0f;
                                translation.x = 0.5f;
                                translation.y = -1.0f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, head.getCompound("gui"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                    }
                    break;
                }
                case GROUND: {
                    switch(getType()) {
                        case CHESTPLATE: {
                            if (!chest.contains("ground", 10)) {
                                translation.x = 0.5f;
                                translation.z = 0.5f;
                                scale.x = 0.5f;
                                scale.y = 0.5f;
                                scale.z = 0.5f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompound("ground"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case LEGGINGS: {
                            if (!legs.contains("ground", 10)) {
                                translation.x = 0.5f;
                                translation.y = 0.25f;
                                translation.z = 0.5f;
                                scale.x = 0.6f;
                                scale.y = 0.6f;
                                scale.z = 0.6f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompound("ground"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case BOOTS: {
                            if (!feet.contains("ground", 10)) {
                                translation.x = 0.5f;
                                translation.y = 0.35f;
                                translation.z = 0.5f;
                                scale.x = 0.65f;
                                scale.y = 0.65f;
                                scale.z = 0.65f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompound("ground"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        default: {
                            if (!head.contains("ground", 10)) {
                                translation.x = 0.5f;
                                translation.y = -0.375f;
                                translation.z = 0.5f;
                                scale.x = 0.5f;
                                scale.y = 0.5f;
                                scale.z = 0.5f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, head.getCompound("ground"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                    }
                    break;
                }
                case FIXED: {
                    switch(getType()) {
                        case CHESTPLATE: {
                            if (!chest.contains("fixed", 10)) {
                                rotation.y = 180.0f;
                                translation.x = 0.5f;
                                translation.y = -0.65f;
                                translation.z = 0.45f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompound("fixed"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case LEGGINGS: {
                            if (!legs.contains("fixed", 10)) {
                                rotation.y = 180.0f;
                                translation.x = 0.5f;
                                translation.y = 0.05f;
                                translation.z = 0.475f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompound("fixed"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        case BOOTS: {
                            if (!feet.contains("fixed", 10)) {
                                rotation.y = 180.0f;
                                translation.x = 0.5f;
                                translation.y = 0.2f;
                                translation.z = 0.475f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompound("fixed"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                        default: {
                            if (!head.contains("fixed", 10)) {
                                rotation.y = 180.0f;
                                translation.x = 0.5f;
                                translation.y = -0.85f;
                                translation.z = 0.4f;
                                scale.x = 0.75f;
                                scale.y = 0.75f;
                                scale.z = 0.75f;
                            } else {
                                Vector3f[] data = setOptional(rotation, translation, scale, head.getCompound("fixed"));
                                rotation = data[0];
                                translation = data[1];
                                scale = data[2];
                            }
                            break;
                        }
                    }
                    break;
                }
                default: { break; } // NONE
            }
            cameraData.put(transformType, new ItemTransform(rotation, translation, scale));
        }
    }

    private Vector3f[] setOptional(Vector3f rotation, Vector3f translation, Vector3f scale, CompoundTag compound) {
        if (compound.contains("rotation", 9)) {
            ListTag list = compound.getList("rotation", 5);
            if (!list.isEmpty()) { rotation.x = list.getFloat(0); }
            if (list.size() > 1) { rotation.y = list.getFloat(1); }
            if (list.size() > 2) { rotation.z = list.getFloat(2); }
        }
        if (compound.contains("translation", 9)) {
            ListTag list = compound.getList("translation", 5);
            if (!list.isEmpty()) { translation.x = list.getFloat(0); }
            if (list.size() > 1) { translation.y = list.getFloat(1); }
            if (list.size() > 2) { translation.z = list.getFloat(2); }
        }
        if (compound.contains("scale", 9)) {
            ListTag list = compound.getList("scale", 5);
            if (!list.isEmpty()) { scale.x = list.getFloat(0); }
            if (list.size() > 1) { scale.y = list.getFloat(1); }
            if (list.size() > 2) { scale.z = list.getFloat(2); }
        }
        return new Vector3f[] { rotation, translation, scale };
    }

    public ItemTransform getOptional(ItemDisplayContext transformType) { return cameraData.get(transformType); }

    @Override
    public int getEnchantmentValue() { return enchantability; }

    @Override
    public @Nonnull Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@Nonnull EquipmentSlot slot) {
        return slot == type.getSlot() ? defaultModifiers : ImmutableMultimap.of();
    }

    @Override
    public boolean isValidRepairItem(@Nonnull ItemStack armorStack, @Nonnull ItemStack repairStack) {
        if (repairItemStack != null) {
            return NoppesUtilPlayer.compareItems(repairItemStack, repairStack, false, false);
        }
        return super.isValidRepairItem(armorStack, repairStack);
    }

    @Override
    public String getCustomName() { return nbtData.getString("RegistryName"); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() {
        if (nbtData.contains("ItemType", 1)) { return nbtData.getByte("ItemType"); }
        return 3;
    }

    @Override
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

}
