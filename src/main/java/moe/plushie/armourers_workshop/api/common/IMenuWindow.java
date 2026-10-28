package moe.plushie.armourers_workshop.api.common;

import net.minecraft.world.inventory.AbstractContainerMenu;

@SuppressWarnings("unused")
public interface IMenuWindow<T extends AbstractContainerMenu> {

    default void menuDidChange() { }

}

