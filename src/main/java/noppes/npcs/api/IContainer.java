package noppes.npcs.api;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.item.IItemStack;

@SuppressWarnings("unused")
public interface IContainer {

   int getSize();

   ISlot getSlot(@ParamName("slotId") int slotId);

   IItemStack getItem(@ParamName("slotId") int slotId);

   void setItem(@ParamName("slotId") int slotId, @ParamName("item") IItemStack item);

   Container getMCInventory();

   AbstractContainerMenu getMCContainer();

   int count(@ParamName("item") IItemStack item, @ParamName("ignoreDamage") boolean ignoreDamage, @ParamName("ignoreNBT") boolean ignoreNBT);

   IItemStack[] getItems();

   boolean isEmpty();

}
