package noppes.npcs.controllers.data;

import java.util.*;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.controllers.TransportController;

public class TransportCategory {

	public TreeMap<Integer, TransportLocation> locations = new TreeMap<>();
	public String title = "";
	public int id = -1;

	public Vector<TransportLocation> getDefaultLocations() {
		Vector<TransportLocation> list = new Vector<>();
		TransportController tData = TransportController.getInstance();
		for (TransportLocation location : locations.values()) {
			if (tData.getCategory(location.id) != null && location.isDefault()) { list.add(location); }
		}
		return list;
	}

	public void load(NBTTagCompound compound) {
		id = compound.getInteger("CategoryId");
		title = compound.getString("CategoryTitle");
		if (title.isEmpty()) { title = "Default"; }
		NBTTagList locs = compound.getTagList("CategoryLocations", 10);
		if (locs.tagCount() > 0) {
			TransportController tData = TransportController.getInstance();
			for (int i = 0; i < locs.tagCount(); ++i) {
				NBTTagCompound nbt = locs.getCompoundTagAt(i);
				if (tData.getTransport(nbt.getInteger("Id")) == null) { tData.loadLocation(this, nbt); }
			}
		}
	}

	public void save(NBTTagCompound compound) {
		compound.setInteger("CategoryId", id);
		compound.setString("CategoryTitle", title);
		NBTTagList locs = new NBTTagList();
		for (TransportLocation location : locations.values()) {
			// Fixed: removed incorrect tData.getCategory(location.id) check
			// that compared a location ID against a category ID.
			// The locations map already belongs to this category.
			locs.appendTag(location.save());
		}
		compound.setTag("CategoryLocations", locs);
	}
}
