package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.controllers.data.TransportCategory;
import noppes.npcs.controllers.data.TransportLocation;

import javax.annotation.Nonnull;

public class ContainerNPCTransports extends Container {

	protected final InventoryPlayer inventory;
	public TransportLocation location;

	public ContainerNPCTransports(EntityPlayer player, BlockPos pos) {
		inventory = player.inventory;
		TransportController tData = TransportController.getInstance();
		location = tData.getTransport(pos.getX());
		if (pos.getX() < 0 || location == null) {
			int catId = pos.getY();
			TransportCategory cat = tData.getCategory(catId);
			if (catId < 0 || cat == null || cat.locations.isEmpty()) {
				for (TransportCategory tCat : tData.getCategories()) {
					if (!tCat.locations.isEmpty()) {
						location = tCat.locations.firstEntry().getValue();
						if (location != null) {
							catId = tCat.id;
							break;
						}
					}
				}
			}
			else { location = cat.locations.firstEntry().getValue(); }
			if (location == null) {
				location = new TransportLocation();
				location.id = pos.getX();
				location.category = TransportController.getInstance().getCategory(location, catId);
			}
		}
		if (player.world.isRemote) { location = location.copy(); }
		resetStacks();
	}

	@Override
	public boolean canInteractWith(@Nonnull EntityPlayer entityplayer) { return true; }

	@Override
	public @Nonnull ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int i) { return ItemStack.EMPTY; }

	public NBTTagCompound saveTransport(TransportCategory category) {
		NBTTagCompound compound = new NBTTagCompound();
		if (category != null) {
			for (int i = 0; i < 9; i++) {
				location.inventory.setInventorySlotContents(i, getSlot(i).getStack());
			}
			TransportController tData = TransportController.getInstance();
			if (location.id < 0 || tData.getTransport(location.id) == null) { tData.loadLocation(category, location.save()); }
			category.save(compound);
		}
		return compound;
	}

	public void resetStacks() {
		inventorySlots.clear();
		inventoryItemStacks.clear();
		for (int v = 0; v < 3; ++v) {
			for (int u = 0; u < 3; ++u) {
				addSlotToContainer(new Slot(location.inventory, u + v * 3,
						(location.id < 0 ? -5000 : 0) + 215 + u * 18, (location.id < 0 ? -5000 : 0) + 20 + v * 18));
			}
		}
		// player inventory
		for(int y = 0; y < 3; ++y) {
			for(int x = 0; x < 9; ++x) { addSlotToContainer(new Slot(inventory, x + y * 9 + 9, x * 18 + 48, 137 + y * 18)); }
		}
		// player hotbar
		for(int x = 0; x < 9; ++x) { addSlotToContainer(new Slot(inventory, x, x * 18 + 48, 195)); }
	}

}
