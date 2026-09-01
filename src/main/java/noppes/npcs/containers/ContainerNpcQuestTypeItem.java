package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.controllers.data.Quest;

import javax.annotation.Nonnull;

public class ContainerNpcQuestTypeItem extends Container {

	public final int slotID;
	public final QuestObjective task;
	public final Slot slot;

	public ContainerNpcQuestTypeItem(EntityPlayer player, int slotIDIn) { // Change
		Quest quest = NoppesUtilServer.getEditingQuest(player);
		slotID = slotIDIn;
		task = quest.questInterface.tasks[slotIDIn];

		NpcMiscInventory inv = new NpcMiscInventory(1);
		inv.setInventorySlotContents(0, task.getItemStack().copy());
		addSlotToContainer(slot = new Slot(inv, 0, 8, 92) {
			@Override
			public void onSlotChanged() { task.setItem(getStack()); }
		});

		for(int y = 0; y < 3; ++y) {
			for(int x = 0; x < 9; ++x) { addSlotToContainer(new Slot(player.inventory, x + y * 9 + 9, 8 + x * 18, 113 + y * 18)); }
		}
		for(int x = 0; x < 9; ++x) {
			addSlotToContainer(new Slot(player.inventory, x, 8 + x * 18, 171));
		}
	}

	@Override
	public boolean canInteractWith(@Nonnull EntityPlayer entityplayer) { return true; }

	@Override
	public @Nonnull ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int i) { return ItemStack.EMPTY; }

}
