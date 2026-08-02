package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.data.Marcet;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

import javax.annotation.Nonnull;

public class ContainerNPCTrader extends ContainerNpcInterface {

	protected final IInventory inv;
	public Marcet marcet;

	public ContainerNPCTrader(EntityPlayer player, EntityNPCInterface npc, int marcetId) {
		super(player);
		inv = player.inventory;
		if (npc != null && npc.role instanceof RoleTrader) { marcet = (Marcet) ((RoleTrader) npc.role).getMarket(); }
		else if (marcetId > -1) { marcet = MarcetController.getInstance().getMarcet(marcetId); }
		else { marcet = null; }
		if (marcet != null && player instanceof EntityPlayerMP) { marcet.addListener(player, true); }
		for (int i2 = 0; i2 < 3; ++i2) {
			for (int l1 = 0; l1 < 9; ++l1) {
				addSlotToContainer(new Slot(inv, l1 + i2 * 9 + 9, 32 + l1 * 18, 140 + i2 * 18));
			}
		}
		for (int j1 = 0; j1 < 9; ++j1) {
			addSlotToContainer(new Slot(inv, j1, 32 + j1 * 18, 198));
		}
	}

	@Override
	public boolean canInteractWith(@Nonnull EntityPlayer entityplayer) { return true; }

	@Override
	public void onContainerClosed(@Nonnull EntityPlayer playerIn) {
		super.onContainerClosed(playerIn);
		if (marcet != null && playerIn instanceof EntityPlayerMP) { marcet.removeListener(playerIn, true); }
	}

	private void reAddSlot(Slot slot) {
		slot.slotNumber = inventorySlots.size();
		inventorySlots.add(slot);
	}

	public void reset(int width, int height) {
		inventorySlots.clear();
		int offsetX = width - 169;
		int offsetY = height - 79;
		for (int i2 = 0; i2 < 3; ++i2) {
			for (int l1 = 0; l1 < 9; ++l1) {
				reAddSlot(new Slot(inv, l1 + i2 * 9 + 9, offsetX + l1 * 18, offsetY + i2 * 18 - 4));
			}
		}
		for (int j1 = 0; j1 < 9; ++j1) {
			reAddSlot(new Slot(inv, j1, offsetX + j1 * 18, offsetY + 54));
		}
	}

}
