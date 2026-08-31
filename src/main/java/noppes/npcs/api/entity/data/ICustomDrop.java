package noppes.npcs.api.entity.data;

import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.handler.data.IAvailability;
import noppes.npcs.api.item.IItemStack;

@SuppressWarnings("unused")
public interface ICustomDrop {

    IAttributeSet addAttribute(@ParamName("name") String attributeName);

    IDropNbtSet addDropNbtSet(@ParamName("name") int type, @ParamName("name") double chance, @ParamName("name") String path, @ParamName("name") String[] values);

    IEnchantSet addEnchant(@ParamName("name") int enchantId);

    IEnchantSet addEnchant(@ParamName("name") String enchantName);

    IItemStack createLoot(@ParamName("name") double addChance);

    IAttributeSet[] getAttributeSets();

    double getChance();

    IDropNbtSet[] getDropNbtSets();

    IEnchantSet[] getEnchantSets();

    IItemStack getItem();

    ItemStack getMCItemStack();

    int getLootMode();

    int getMaxAmount();

    int getMinAmount();

    IAvailability getAvailability();

    boolean getTiedToLevel();

    void remove();

    void removeAttribute(@ParamName("attribute") IAttributeSet attribute);

    void removeDropNbt(@ParamName("nbt") IDropNbtSet nbt);

    void removeEnchant(@ParamName("enchant") IEnchantSet enchant);

    void resetTo(@ParamName("item") IItemStack item);

    void setAmount(@ParamName("min") int min, @ParamName("max") int max);

    void setChance(@ParamName("chance") double chance);

    void setItem(@ParamName("item") IItemStack item);

    void setLootMode(@ParamName("lootMode") int lootMode);

    void setTiedToLevel(@ParamName("tiedToLevel") boolean tiedToLevel);

}
