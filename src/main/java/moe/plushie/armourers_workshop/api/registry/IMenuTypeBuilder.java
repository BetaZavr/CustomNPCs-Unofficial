package moe.plushie.armourers_workshop.api.registry;

import java.util.function.Supplier;
import moe.plushie.armourers_workshop.api.common.IMenuType;
import net.minecraft.world.inventory.AbstractContainerMenu;

@SuppressWarnings("unused")
public interface IMenuTypeBuilder<T extends AbstractContainerMenu> extends IRegistryBuilder<IMenuType<T>> {

    @SuppressWarnings("rawtypes")
    <U> IMenuTypeBuilder<T> bind(Supplier var1);

}

