package noppes.npcs.mixin.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractContainerMenu.class, priority = 499)
public interface IAbstractContainerMenuMixin {

   @Accessor NonNullList<ItemStack> getLastSlots();

   @Accessor NonNullList<ItemStack> getRemoteSlots();

}
