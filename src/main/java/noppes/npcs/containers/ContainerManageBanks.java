package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.controllers.data.Bank;

import javax.annotation.Nonnull;

public class ContainerManageBanks extends Container {

	protected int bankId = -1;
	public int ceil = -1;

    public ContainerManageBanks(EntityPlayer player) {
        IInventory inv = new NpcMiscInventory(2);
		addSlotToContainer(new Slot(inv, 0, -5000, -5000));
		addSlotToContainer(new Slot(inv, 1, -5000, -5000));
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				addSlotToContainer(new Slot(player.inventory, j + i * 9 + 9, j * 18 + 8, 113 + i * 18));
			}
		}
		for (int j = 0; j < 9; ++j) {
			addSlotToContainer(new Slot(player.inventory, j, j * 18 + 8, 171));
		}
	}

	@Override
	public boolean canInteractWith(@Nonnull EntityPlayer playerIn) {
		if (playerIn instanceof EntityPlayerMP) {
			return CustomNpcsPermissions.hasPermission((EntityPlayerMP) playerIn, CustomNpcsPermissions.GLOBAL_BANK);
		}
		return true;
	}

	// Server
	public void setBank(@Nonnull Bank bank, int ceilIn) {
		if (bank.ceilSettings.containsKey(ceilIn)) {
			bankId = bank.id;
			ceil = ceilIn;
			getSlot(0).putStack(bank.ceilSettings.get(ceil).openStack);
			getSlot(1).putStack(bank.ceilSettings.get(ceil).upgradeStack);
			detectAndSendChanges();
		}
	}

	@Override
	public @Nonnull ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int i) { return ItemStack.EMPTY; }

	public boolean isBank(int bankIdIn) { return bankId == bankIdIn; }

}
