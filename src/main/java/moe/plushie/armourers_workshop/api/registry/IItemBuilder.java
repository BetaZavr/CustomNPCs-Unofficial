package moe.plushie.armourers_workshop.api.registry;

import moe.plushie.armourers_workshop.api.core.IRegistryHolder;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

@SuppressWarnings("unused")
public interface IItemBuilder<T extends Item> extends IRegistryBuilder<T> {

    IItemBuilder<T> stacksTo(int var1);

    IItemBuilder<T> durability(int var1);

    IItemBuilder<T> craftRemainder(Item var1);

    IItemBuilder<T> group(IRegistryHolder<CreativeModeTab> var1);

    IItemBuilder<T> rarity(Rarity var1);

    IItemBuilder<T> fireResistant();

    IItemBuilder<T> overrideDescription(String var1);

    IItemBuilder<T> useDescriptionPrefix(String var1);

}

