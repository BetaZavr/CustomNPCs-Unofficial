package noppes.npcs.containers;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomContainer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.controllers.data.Quest;
import org.jetbrains.annotations.NotNull;

public class ContainerNpcQuestTypeItem extends AbstractContainerMenu {

   public final int slotID;
   public final QuestObjective task;
   public final Slot slot;

   public ContainerNpcQuestTypeItem(int containerId, Inventory playerInventory, int slotIdIn) {
      super(CustomContainer.container_questtypeitem, containerId);
      Quest quest = NoppesUtilServer.getEditingQuest(playerInventory.player);
      slotID = slotIdIn;
      task = quest.questInterface.tasks[slotIdIn];

      NpcMiscInventory inv = new NpcMiscInventory(1);
      inv.setItem(0, task.getItemStack());
      addSlot(slot = new Slot(inv, 0, 8, 92) {
         @Override
         public void setChanged() { task.setItem(getItem()); }
      });

      for(int y = 0; y < 3; ++y) {
         for(int x = 0; x < 9; ++x) { addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 113 + y * 18)); }
      }
      for(int x = 0; x < 9; ++x) {
         addSlot(new Slot(playerInventory, x, 8 + x * 18, 171));
      }
   }

   @Override
   public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int i) { return ItemStack.EMPTY; }

   @Override
   public boolean stillValid(@NotNull Player playerIn) { return true; }

}
