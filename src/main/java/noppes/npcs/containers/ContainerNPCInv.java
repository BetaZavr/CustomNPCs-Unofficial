package noppes.npcs.containers;

import moe.plushie.armourers_workshop.api.ArmourersWorkshopApi;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import noppes.npcs.CustomContainer;
import noppes.npcs.containers.slots.AWSlotOutfit;
import noppes.npcs.containers.slots.AWSlotWings;
import noppes.npcs.containers.slots.SlotNPCArmor;
import noppes.npcs.containers.slots.SlotNPCHand;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import org.jetbrains.annotations.NotNull;

public class ContainerNPCInv extends AbstractContainerMenu {

   public ContainerNPCInv(int containerId, Inventory playerInventory, int entityId) {
      super(CustomContainer.container_inv, containerId);
      EntityNPCInterface npc = (EntityNPCInterface) playerInventory.player.level().getEntity(entityId);
      DataInventory inv = npc != null ? npc.inventory : new DataInventory(null);
      addSlot(new SlotNPCArmor(inv, 0, 9, 22, EquipmentSlot.HEAD));
      addSlot(new SlotNPCArmor(inv, 1, 9, 40, EquipmentSlot.CHEST));
      addSlot(new SlotNPCArmor(inv, 2, 9, 58, EquipmentSlot.LEGS));
      addSlot(new SlotNPCArmor(inv, 3, 9, 76, EquipmentSlot.FEET));
      addSlot(new SlotNPCHand(inv, 4, 81, 22, "weapon"));
      addSlot(new SlotNPCHand(inv, 5, 81, 40, "arrow"));
      addSlot(new SlotNPCHand(inv, 6, 81, 58, "shield"));
      // AW mod
      if (ArmourersWorkshopApi.isAvailable()) {
         addSlot(new AWSlotOutfit(inv, 7, 27, 4));
         addSlot(new AWSlotWings(inv, 8, 45, 4));
      }
      // player inventory
      for(int x = 0; x < 3; ++x) {
         for(int y = 0; y < 9; ++y) { addSlot(new Slot(playerInventory, y + x * 9 + 9, y * 18 + 8, 113 + x * 18)); }
      }
      // player hotbar
      for(int x = 0; x < 9; ++x) { addSlot(new Slot(playerInventory, x, x * 18 + 8, 171)); }
   }

   @Override
   public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
      Slot slot = slots.get(index);
      if (slot.hasItem()) {
         ItemStack inSlot = slot.getItem();
         if (index >= 16) { // player inventory
            if (inSlot.getItem() instanceof ArmorItem armorItem) {
               Slot armSlot = switch (armorItem.getEquipmentSlot()) {
                  case CHEST -> slots.get(1);
                  case LEGS -> slots.get(2);
                  case FEET -> slots.get(3);
                  default -> slots.get(0);
               };
               ItemStack armorStack = armSlot.getItem().copy();
               armSlot.set(inSlot);
               slot.set(armorStack);
            }
            else if (inSlot.getItem() instanceof SwordItem || inSlot.getItem() instanceof BowItem) {
               Slot wpnSlot = slots.get(4);
               ItemStack wpnStack = wpnSlot.getItem().copy();
               wpnSlot.set(inSlot);
               slot.set(wpnStack);
            }
            else if (inSlot.getItem() instanceof ShieldItem) {
               Slot sldSlot = slots.get(6);
               ItemStack offStack = sldSlot.getItem().copy();
               sldSlot.set(inSlot);
               slot.set(offStack);
            }
            else {
               Slot pjcSlot = slots.get(5);
               ItemStack pjcStack = pjcSlot.getItem().copy();
               pjcSlot.set(inSlot);
               slot.set(pjcStack);
            } // any projective
         }
         else if (playerIn.getInventory().add(inSlot)) { // equipment
            slot.set(ItemStack.EMPTY);
            playerIn.inventoryMenu.broadcastChanges();
            if (playerIn instanceof ServerPlayer player) {
               player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, player.getInventory().selected, player.getInventory().getItem(player.getInventory().selected)));
            }
         }
      }
      return ItemStack.EMPTY;
   }

   @Override
   public boolean stillValid(@NotNull Player playerIn) { return true; }

}
