package moe.plushie.armourers_workshop.api.common;

import java.util.Collections;
import java.util.Set;

import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public interface ILootItemFunction {

    ItemStack apply(ItemStack var1, ILootContext var2);

    default Set<? extends IContextKey<?>> getReferencedContextParams() { return Collections.emptySet(); }

}

