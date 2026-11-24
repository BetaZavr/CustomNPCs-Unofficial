package noppes.npcs.containers;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomContainer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleFollower;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class ContainerNPCFollowerHire extends ContainerNpcInterface {

   public final RoleFollower role;

   public ContainerNPCFollowerHire(int containerId, Inventory playerInventory, int entityId, boolean isHired) {
      super(isHired ? CustomContainer.container_follower : CustomContainer.container_followerhire, containerId, playerInventory);
      EntityNPCInterface npc = (EntityNPCInterface)player.level().getEntity(entityId);
      if (npc != null) { role = (RoleFollower) npc.role; }
      else { role = new RoleFollower(null); }
      if (role.inventory.getContainerSize() > 0 || isHired) {
         int invSize = role.inventory.getContainerSize();
         int u = invSize < 4 ? 1 : invSize < 5 ? 2 : 3;
         int v = invSize < 4 ? 3 : invSize < 5 ? 2 : 3;
         int yPos = isHired ? 151 : 84;
         if (isHired) {
            if (role.infiniteDays) { yPos -= 16 * 4; }
            else {
               int index = 0;
               for (int slot = 0; slot < role.rentalItems.getContainerSize(); ++slot) {
                  if (NoppesUtilServer.isItemStackNull(role.rentalItems.getItem(slot))) { index++; }
               }
               if (!role.rates.containsKey(3) || role.rentalMoney == 0) { index += 1; }
               yPos -= 16 * index;
            }
            for (int y = 0; y < v; ++y) {
               for (int x = 0; x < u; ++x) {
                  addSlot(new Slot(role.inventory, x + y * u, 174 + x * 18, yPos + y * 18));
               }
            }
         }
         for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
               addSlot(new Slot(player.getInventory(), x + y * 9 + 9, 8 + x * 18, yPos + y * 18));
            }
         }
         for (int x = 0; x < 9; ++x) {
            addSlot(new Slot(player.getInventory(), x, 8 + x * 18, yPos + 58));
         }
      }
   }

   @Override
   public void removed(@NotNull Player playerIn) { super.removed(playerIn); }

   @Override
   @SuppressWarnings("ConstantConditions")
   public @Nonnull ItemStack quickMoveStack(@Nonnull Player playerIn, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = slots.get(index);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (index < role.rentalItems.getContainerSize()) {
            if (!moveItemStackTo(itemstack1, role.rentalItems.getContainerSize(), slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!moveItemStackTo(itemstack1, 0, role.rentalItems.getContainerSize(), false)) {
            return ItemStack.EMPTY;
         }
         if (itemstack1.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }
      return itemstack;
   }

}
