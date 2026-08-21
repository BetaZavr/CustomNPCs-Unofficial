package noppes.npcs.items.custom;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class CustomItem extends Item implements ICustomElement {

    protected final @Nonnull CompoundTag nbtData;
    protected final int enchantability;
    protected final ItemStack repairItemStack;
    protected final Map<Block, Float> collectionBlocks = new HashMap<>();
    protected final Map<TagKey<Block>, Float> collectionBlockTags = new HashMap<>();
    protected final Multimap<Attribute, AttributeModifier> defaultModifiers;
    protected final double attackDamage;
    protected final double attackSpeed;

    public CustomItem(@Nonnull Item.Properties properties, @Nonnull CompoundTag nbtItem) {
        super(properties);
        nbtData = nbtItem;

        if (nbtItem.contains("SpeedAttack", 6)) { attackSpeed = nbtItem.getDouble("SpeedAttack"); }
        else { attackSpeed = -2.4d; }
        if (nbtItem.contains("EntityDamage", 6)) { attackDamage = nbtItem.getDouble("EntityDamage"); }
        else { attackDamage = 0.0f; }

        if (nbtItem.contains("CollectionBlocks", 9)) {
            ListTag list = nbtItem.getList("CollectionBlocks", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag nbt = list.getCompound(i);
                if (nbt.contains("Name", 8)) {
                    Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(nbt.getString("Name")));
                    if (block != null && nbt.contains("Speed", 5)) {
                        collectionBlocks.put(block, nbt.getFloat("Speed"));
                    }
                }
            }
        }
        if (nbtItem.contains("CollectionBlockTags", 9)) {
            ListTag list = nbtItem.getList("CollectionBlockTags", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag nbt = list.getCompound(i);
                if (nbt.contains("Name", 8) && nbt.contains("Speed", 5)) {
                    collectionBlockTags.put(TagKey.create(Registries.BLOCK, new ResourceLocation(nbt.getString("Name"))),
                            nbt.getFloat("Speed"));
                }
            }
        }
        if (nbtItem.contains("Enchantability", 3)) { enchantability = nbtItem.getInt("Enchantability"); }
        else { enchantability = 10; }
        if (nbtItem.contains("RepairItem", 10)) { repairItemStack = ItemStack.of(nbtItem.getCompound("RepairItem")); }
        else { repairItemStack = ItemStack.EMPTY; }

        if (attackDamage > 0.0d) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", attackDamage, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", nbtItem.contains("SpeedAttack", 6) ? (float) nbtItem.getDouble("SpeedAttack") : -2.4f, AttributeModifier.Operation.ADDITION));
            defaultModifiers = builder.build();
        }
        else { defaultModifiers = ImmutableMultimap.of(); }
    }

    @Override
    public float getDestroySpeed(@Nonnull ItemStack stack, @Nonnull BlockState state) {
        for (Map.Entry<Block, Float> entry : collectionBlocks.entrySet()) {
            if (state.is(entry.getKey())) { return entry.getValue(); }
        }
        for (Map.Entry<TagKey<Block>, Float> entry : collectionBlockTags.entrySet()) {
            if (state.is(entry.getKey())) { return entry.getValue(); }
        }
        // vanilla
        return super.getDestroySpeed(stack, state);
    }

    @Override
    @Deprecated
    public boolean isCorrectToolForDrops(@Nonnull BlockState state) {
        for (Map.Entry<Block, Float> entry : collectionBlocks.entrySet()) {
            if (state.is(entry.getKey())) { return true; }
        }
        // vanilla
        return super.isCorrectToolForDrops(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getEnchantmentValue() { return enchantability; }

    @Override
    public boolean isValidRepairItem(@Nonnull ItemStack armorStack, @Nonnull ItemStack repairStack) {
        if (repairItemStack != null) {
            return NoppesUtilPlayer.compareItems(repairItemStack, repairStack, false, false);
        }
        return super.isValidRepairItem(armorStack, repairStack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@Nonnull EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? defaultModifiers : ImmutableMultimap.of();
    }

    @Override
    public String getCustomName() { return nbtData.getString("RegistryName"); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() {
        if (nbtData.contains("ItemType", 1)) { return nbtData.getByte("ItemType"); }
        return 0;
    }

    @Override
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

}
