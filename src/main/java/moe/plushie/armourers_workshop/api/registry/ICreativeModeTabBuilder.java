package moe.plushie.armourers_workshop.api.registry;

import java.util.function.Supplier;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public interface ICreativeModeTabBuilder<T extends CreativeModeTab> extends IRegistryBuilder<T> {

    ICreativeModeTabBuilder<T> icon(Supplier<Supplier<ItemStack>> var1);

}

