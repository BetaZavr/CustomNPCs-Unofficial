package noppes.npcs.controllers.data;

import java.util.TreeMap;
import java.util.Vector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class TransportCategory {

   public final TreeMap<Integer, TransportLocation> locations = new TreeMap<>();
   public String title = "";
   public int id = -1;

   public Vector<TransportLocation> getDefaultLocations() {
      Vector<TransportLocation> list = new Vector<>();
      for (TransportLocation loc : locations.values()) {
         if (loc.isDefault()) {
            list.add(loc);
         }
      }
      return list;
   }

   public void load(CompoundTag compound) {
      id = compound.getInt("CategoryId");
      title = compound.getString("CategoryTitle");
      ListTag locs = compound.getList("CategoryLocations", 10);
      if (!locs.isEmpty()) {
         for(int ii = 0; ii < locs.size(); ++ii) {
            TransportLocation location = new TransportLocation();
            location.load(locs.getCompound(ii));
            location.category = this;
            locations.put(location.id, location);
         }
      }
   }

   public void save(CompoundTag compound) {
      compound.putInt("CategoryId", id);
      compound.putString("CategoryTitle", title);
      ListTag locs = new ListTag();
      for (TransportLocation location : locations.values()) {
         locs.add(location.save());
      }
      compound.put("CategoryLocations", locs);
   }

}
