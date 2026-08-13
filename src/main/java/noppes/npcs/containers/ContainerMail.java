package noppes.npcs.containers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomContainer;
import noppes.npcs.containers.slots.SlotValid;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.controllers.data.PlayerMailData;
import org.jetbrains.annotations.NotNull;

public class ContainerMail extends ContainerNpcInterface {

   public static PlayerMail staticMail = new PlayerMail();
   public final boolean canEdit;
   public final boolean canSend;
   public boolean sendMail = false;
   public PlayerMail mail;

   public ContainerMail(int containerId, Inventory playerInventory, boolean canEditIn, boolean canSendIn) {
      super(CustomContainer.container_mail, containerId, playerInventory);
      mail = ContainerMail.staticMail;
      ContainerMail.staticMail = new PlayerMail();
      canEdit = canEditIn;
      canSend = canSendIn;
      playerInventory.startOpen(player);
      for (int k = 0; k < 4; ++k) {
         addSlot(new SlotValid(mail, k, 199 + (k % 2) * 18, 190 + (k / 2) * 18, canEdit));
      }
      for (int j = 0; j < 3; ++j) {
         for (int k = 0; k < 9; ++k) {
            addSlot(new Slot(playerInventory, k + j * 9 + 9, 7 + k * 18, 168 + j * 18));
         }
      }
      for (int j = 0; j < 9; ++j) {
         addSlot(new Slot(playerInventory, j, 7 + j * 18, 223));
      }
   }

   @Override
   public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int slotId) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = slots.get(slotId);
      if (slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (slotId < 4) {
            if (!moveItemStackTo(itemstack1, 4, slots.size(), true)) { return ItemStack.EMPTY; }
         }
         else if (!canEdit || !moveItemStackTo(itemstack1, 0, 4, false)) { return ItemStack.EMPTY; }
         if (itemstack1.getCount() == 0) { slot.set(ItemStack.EMPTY); }
         else { slot.setChanged(); }
      }
      return itemstack;
   }

   @Override
   public void removed(@NotNull Player playerIn) {
      super.removed(player);
      if (playerIn instanceof ServerPlayer) {
         if (!canEdit) {
            PlayerMailData data = PlayerData.get(player).mailData;
            for (PlayerMail m : data.playerMails) {
               if (m.timeWhenReceived == mail.timeWhenReceived && m.sender.equals(mail.sender)) {
                  m.load(mail.save());
                  break;
               }
            }
         }
         else if (!sendMail) {
            for (int i = 0; i < 4; i++) {
               Slot slot = getSlot(i);
               if (!slot.hasItem()) { continue;}
               ItemStack itemstack = slot.getItem();
               if (playerIn.isAlive() && !((ServerPlayer) playerIn).hasDisconnected()) { playerIn.getInventory().placeItemBackInInventory(itemstack); }
               else { playerIn.drop(itemstack, false); }
            }
         }
      }
   }

}
