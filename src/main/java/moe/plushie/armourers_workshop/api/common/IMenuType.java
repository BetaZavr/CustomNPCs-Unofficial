package moe.plushie.armourers_workshop.api.common;

import java.util.function.Supplier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

@SuppressWarnings("unused")
public interface IMenuType<C extends AbstractContainerMenu> extends Supplier<MenuType<C>> {
}

