package noppes.npcs.api.wrapper;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.ISlot;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;

import java.util.Objects;

public class ContainerWrapper implements IContainer {

   private Container inventory;
   private AbstractContainerMenu container;

   public ContainerWrapper(Container inventory) {
      this.inventory = inventory;
   }

   public ContainerWrapper(AbstractContainerMenu container) {
      this.container = container;
   }

   public int getSize() {
      return this.inventory != null ? this.inventory.getContainerSize() : this.container.slots.size();
   }

   @Override
   public ISlot getSlot(int slotId) {
      if (slotId >= 0 && slotId < this.getSize()) {
         return new WrapperSlot(container.getSlot(slotId));
      } else {
         throw new CustomNPCsException("Slot is out of range " + slotId);
      }
   }

   @Override
   public IItemStack getItem(int slotId) {
      if (slotId >= 0 && slotId < this.getSize()) {
         return this.inventory != null ? Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(this.inventory.getItem(slotId)) : Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(this.container.getSlot(slotId).getItem());
      } else {
         throw new CustomNPCsException("Slot is out of range " + slotId);
      }
   }

   public void setItem(int slot, IItemStack item) {
      if (slot >= 0 && slot < this.getSize()) {
         ItemStack itemstack = item == null ? ItemStack.EMPTY : item.getMCItemStack();
         if (this.inventory != null) {
            this.inventory.setItem(slot, itemstack);
         } else {
            this.container.setItem(slot, this.container.getStateId(), itemstack);
            this.container.broadcastChanges();
         }

      } else {
         throw new CustomNPCsException("Slot is out of range " + slot);
      }
   }

   public int count(IItemStack item, boolean ignoreDamage, boolean ignoreNBT) {
      int count = 0;

      for(int i = 0; i < this.getSize(); ++i) {
         IItemStack toCompare = this.getItem(i);
         if (NoppesUtilPlayer.compareItems(item.getMCItemStack(), toCompare.getMCItemStack(), ignoreDamage, ignoreNBT)) {
            count += toCompare.getStackSize();
         }
      }

      return count;
   }

   public Container getMCInventory() {
      return this.inventory;
   }

   public AbstractContainerMenu getMCContainer() {
      return this.container;
   }

   public IItemStack[] getItems() {
      IItemStack[] items = new IItemStack[this.getSize()];
      for(int i = 0; i < this.getSize(); ++i) {
         items[i] = this.getItem(i);
      }
      return items;
   }

   @Override
   public boolean isEmpty() { return inventory == null || inventory.isEmpty(); }

}
