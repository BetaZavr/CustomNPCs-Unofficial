package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleFollower;

import javax.annotation.Nonnull;

public class ContainerNPCFollowerHire extends ContainerNpcInterface {

	public final RoleFollower role;

	public ContainerNPCFollowerHire(EntityPlayer player, int entityId, boolean isHired) {
		super(player);
		EntityNPCInterface npc = (EntityNPCInterface) player.world.getEntityByID(entityId);
		if (npc != null) { role = (RoleFollower) npc.role; }
		else { role = new RoleFollower(null); }
		if (role.inventory.getSizeInventory() > 0 || isHired) {
			int invSize = role.inventory.getSizeInventory();
			int u = invSize < 4 ? 1 : invSize < 5 ? 2 : 3;
			int v = invSize < 4 ? 3 : invSize < 5 ? 2 : 3;
			int yPos = isHired ? 151 : 84;
			if (isHired) {
				if (role.infiniteDays) { yPos -= 16 * 4; }
				else {
					int index = 0;
					for (int slot = 0; slot < role.rentalItems.getSizeInventory(); ++slot) {
						if (NoppesUtilServer.isItemStackNull(role.rentalItems.getStackInSlot(slot))) { index++; }
					}
					if (!role.rates.containsKey(3) || role.rentalMoney == 0) { index += 1; }
					yPos -= 16 * index;
				}
				for (int y = 0; y < v; ++y) {
					for (int x = 0; x < u; ++x) {
						addSlotToContainer(new Slot(role.inventory, x + y * u, 174 + x * 18, yPos + y * 18));
					}
				}
			}
			for (int y = 0; y < 3; ++y) {
				for (int x = 0; x < 9; ++x) {
					addSlotToContainer(new Slot(player.inventory, x + y * 9 + 9, 8 + x * 18, yPos + y * 18));
				}
			}
			for (int x = 0; x < 9; ++x) {
				addSlotToContainer(new Slot(player.inventory, x, 8 + x * 18, yPos + 58));
			}
		}
	}

	@Override
	public void onContainerClosed(@Nonnull EntityPlayer entityplayer) { super.onContainerClosed(entityplayer); }

	@Override
	public @Nonnull ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = inventorySlots.get(index);
		if (slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();
			if (index < role.rentalItems.getSizeInventory()) {
				if (!mergeItemStack(itemstack1, role.rentalItems.getSizeInventory(), inventorySlots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!mergeItemStack(itemstack1, 0, role.rentalItems.getSizeInventory(), false)) {
				return ItemStack.EMPTY;
			}
			if (itemstack1.isEmpty()) {
				slot.putStack(ItemStack.EMPTY);
			} else {
				slot.onSlotChanged();
			}
		}
		return itemstack;
	}

}
