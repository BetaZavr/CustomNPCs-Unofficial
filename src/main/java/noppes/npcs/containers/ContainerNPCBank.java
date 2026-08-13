package noppes.npcs.containers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomContainer;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.controllers.BankController;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.data.Bank;
import noppes.npcs.controllers.data.BankData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketBankSetPlayer;

import javax.annotation.Nonnull;
import java.util.Objects;

public class ContainerNPCBank extends AbstractContainerMenu {

   public static String editPlayerBankData;

   public final @Nonnull NpcMiscInventory items;
   public final @Nonnull BankData data;
   public final int scrollY;
   public final int ceilPos;
   public final int ceilsUpdate;
   public final int ceil;

   public ContainerNPCBank(int containerId, Inventory playerInventory, @Nonnull CompoundTag nbtBD) {
      super(CustomContainer.container_bank, containerId);
      ceil = nbtBD.getInt("GuiCeil");
      scrollY = nbtBD.getInt("GuiScrollY");
      ceilPos = nbtBD.getInt("GuiCeilPos");
      ceilsUpdate = nbtBD.getInt("GuiCeilsUpdate");
      Bank bank = BankController.getInstance().getBank(nbtBD.getInt("id"));
      if (bank == null) { bank = new Bank(); }
      // Server
      BankData bd = new BankData(bank, "");
      if (playerInventory.player instanceof ServerPlayer sPlayer) {
         PlayerData pd = PlayerDataController.instance.getDataFromUsername(sPlayer.getServer(), ContainerNPCBank.editPlayerBankData);
         if (pd == null) {
            ContainerNPCBank.editPlayerBankData = null;
            Packets.send(sPlayer, new PacketBankSetPlayer(""));
            pd = PlayerDataController.instance.getDataFromUsername(sPlayer.getServer(), sPlayer.getName().getString());
         }
         if (pd != null) {
            bd = pd.bankData.get(bank.id);
            bd.addListener(sPlayer);
         }
      }
      else { bd = PlayerData.get(playerInventory.player).bankData.get(bank.id); }
      bd.load(nbtBD);
      data = bd;
      items = Objects.requireNonNull(data.get(ceil));
      for (int i = 0; i < items.getContainerSize(); i++) {
         addSlot(new Slot(items, i, -5000, -5000));
      }
      // player Inventory
      int h = items.getContainerSize() > 0 ? 95 : 0;
      for (int r = 0; r < 3; ++r) {
         for (int p = 0; p < 9; ++p) {
            addSlot(new Slot(playerInventory, p + r * 9 + 9, 9 + p * 18, 40 + r * 18 + h));
         }
      }
      for (int p = 0; p < 9; ++p) {
         addSlot(new Slot(playerInventory, p, 9 + p * 18, 98 + h));
      }
   }

   @Override
   public boolean stillValid(@Nonnull Player playerIn) { return true; }

   @Override
   public @Nonnull ItemStack quickMoveStack(@Nonnull Player playerIn, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = slots.get(index);
      if (slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (index < items.getContainerSize()) {
            if (!moveItemStackTo(itemstack1, items.getContainerSize(), slots.size(), true)) { return ItemStack.EMPTY; }
         }
         else if (!moveItemStackTo(itemstack1, 0, items.getContainerSize(), false)) { return ItemStack.EMPTY; }
         if (itemstack1.isEmpty()) { slot.set(ItemStack.EMPTY); }
         else { slot.setChanged(); }
      }
      return itemstack;
   }

   @Override
   public void removed(@Nonnull Player playerIn) {
      super.removed(playerIn);
      if (playerIn instanceof ServerPlayer sPlayer) {
         data.removeListener(sPlayer);
         if (!data.bank.isPublic) { data.save(); }
      }
   }

}
