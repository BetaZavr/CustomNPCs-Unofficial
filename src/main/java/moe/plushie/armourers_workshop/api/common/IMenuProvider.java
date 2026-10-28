package moe.plushie.armourers_workshop.api.common;

import net.minecraft.world.entity.player.Inventory;

@FunctionalInterface
@SuppressWarnings("unused")
public interface IMenuProvider<C, T> {

    C createMenu(IMenuType<?> var1, int var2, Inventory var3, T var4);

}

