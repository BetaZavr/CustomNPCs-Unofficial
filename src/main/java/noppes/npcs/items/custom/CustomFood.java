package noppes.npcs.items.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.util.ValueUtil;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class CustomFood extends Item implements ICustomElement {

    protected final @Nonnull CompoundTag nbtData;
    protected final int itemUseDuration;

    public CustomFood(@Nonnull Item.Properties properties, @Nonnull CompoundTag nbtItem) {
        super(properties);
        nbtData = nbtItem;

        if (nbtItem.contains("UseDuration", 3)) { itemUseDuration = ValueUtil.correctInt(nbtItem.getInt("UseDuration"), 16, 1200); }
        else { itemUseDuration = 32; }

    }

    @Override
    public int getUseDuration(@Nonnull ItemStack foodStack) {
        @Nullable FoodProperties food = getFoodProperties(foodStack, null);
        return itemUseDuration / (food != null && food.isFastFood() ? 2 : 1);
    }

    @Override
    public String getCustomName() { return nbtData.getString("RegistryName"); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() {
        if (nbtData.contains("ItemType", 1)) { return nbtData.getByte("ItemType"); }
        return 6;
    }

    @Override
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

}
