package noppes.npcs.items.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;

import javax.annotation.Nonnull;

public class CustomShield extends ShieldItem implements ICustomElement {

    protected final @Nonnull CompoundTag nbtData;
    protected final ItemStack repairItemStack;
    protected final int enchantability;

    public CustomShield(@Nonnull Item.Properties properties, @Nonnull CompoundTag nbtItem) {
        super(properties);
        nbtData = nbtItem;
        if (nbtItem.contains("Enchantability", 3) && nbtItem.getInt("Enchantability") > 0) {
            enchantability = nbtItem.getInt("Enchantability");
        }
        else { enchantability = 0; }
        if (nbtItem.contains("RepairItem", 10)) { repairItemStack = ItemStack.of(nbtItem.getCompound("RepairItem")); }
        else { repairItemStack = null; }
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
    public String getCustomName() { return nbtData.getString("RegistryName"); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() {
        if (nbtData.contains("ItemType", 1)) { return nbtData.getByte("ItemType"); }
        return 4;
    }

    @Override
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

}
