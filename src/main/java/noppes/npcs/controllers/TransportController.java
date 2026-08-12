package noppes.npcs.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.data.TransportCategory;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSyncUpdate;
import noppes.npcs.roles.RoleTransporter;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TransportController {

   protected static TransportController instance;

   protected final TreeMap<Integer, TransportLocation> locations = new TreeMap<>();
   protected final TreeMap<Integer, TransportCategory> categories = new TreeMap<>();
   protected int lastUsedID = 0;

   public static TransportController getInstance() {
      if (instance == null) { instance = new TransportController(); }
      return instance;
   }

   public TransportController() {
      instance = this;
      loadCategories();
      if (categories.isEmpty()) {
         TransportCategory cat = new TransportCategory();
         cat.id = 1;
         cat.title = "Default";
         categories.put(cat.id, cat);
      }
   }

   private void loadCategories() {
      File saveDir = CustomNpcs.getLevelSaveDirectory();
      if (saveDir != null) {
         try {
            File file = new File(saveDir, "transport.dat");
            if (file.exists()) {
               load(NbtIo.readCompressed(new FileInputStream(file)));
            }
         } catch (IOException var5) {
            try {
               File file = new File(saveDir, "transport.dat_old");
               if (file.exists()) {
                  load(NbtIo.readCompressed(new FileInputStream(file)));
               }
            } catch (IOException ignored) {
            }
         }
      }
   }

   public void load(CompoundTag compound) {
      clear();
      lastUsedID = compound.getInt("lastID");
      ListTag list = compound.getList("NPCTransportCategories", 10);
      for (int i = 0; i < list.size(); ++i) { loadCategory(list.getCompound(i)); }
   }

   public void loadCategory(CompoundTag compound) {
      TransportCategory category = new TransportCategory();
      category.load(compound);
      for (TransportLocation location : category.locations.values()) { locations.put(location.id, location); }
      categories.put(category.id, category);
   }

   public void clear() {
      locations.clear();
      categories.clear();
   }

   public CompoundTag getNBT() {
      ListTag list = new ListTag();
      for (TransportCategory category : categories.values()) {
         CompoundTag compound = new CompoundTag();
         category.save(compound);
         list.add(compound);
      }
      CompoundTag compound = new CompoundTag();
      compound.putInt("lastID", lastUsedID);
      compound.put("NPCTransportCategories", list);
      return compound;
   }

   public void saveCategories() {
      try {
         File saveDir = CustomNpcs.getLevelSaveDirectory();
         File file = new File(saveDir, "transport.dat_new");
         File file1 = new File(saveDir, "transport.dat_old");
         File file2 = new File(saveDir, "transport.dat");
         NbtIo.writeCompressed(getNBT(), new FileOutputStream(file));
         if (file1.exists() && !file1.delete()) { LogWriter.debug("Error delete \"" + file1.getName() + "\" file"); }
         if (!file2.renameTo(file1) || (file2.exists() && !file2.delete())) { LogWriter.debug("Error delete or rename \"" + file2.getName() + "\" file"); }
         if (!file.renameTo(file2) || (file.exists() && !file.delete())) { LogWriter.debug("Error delete or rename \"" + file.getName() + "\" file"); }
      } catch (Exception var5) {
         LogWriter.except(var5);
      }
   }

   public @Nonnull TransportCategory getCategory(@Nullable TransportLocation forLocation, int categoryId) {
      if (categories.containsKey(categoryId)) { return categories.get(categoryId); }
      TransportCategory category = new TransportCategory();
      if (forLocation != null) { category.locations.put(forLocation.id, forLocation); }
      return category;
   }

   public @Nullable TransportCategory getCategory(int categoryId) {
      if (categories.containsKey(categoryId)) { return categories.get(categoryId); }
      return null;
   }

   public @Nullable TransportLocation getTransport(int transportId) { return locations.get(transportId); }

   @SuppressWarnings("unused")
   public @Nullable TransportLocation getTransport(String name) {
      for (TransportLocation loc : new ArrayList<>(locations.values())) {
         if (loc.name.equals(name)) { return loc; }
      }
      return null;
   }

   private int getUniqueIdLocation() {
      if (lastUsedID == 0) {
          for (int catId : locations.keySet()) {
              if (catId > lastUsedID) {
                  lastUsedID = catId;
              }
          }
      }
      ++lastUsedID;
      return lastUsedID;
   }

   private int getUniqueIdCategory() {
      int id = 0;
      for (int catId : categories.keySet()) {
         if (catId > id) {
            id = catId;
         }
      }
      ++id;
      return id;
   }

   public void setLocation(TransportLocation location) {
      if (locations.containsKey(location.id)) {
         for (TransportCategory cat : categories.values()) {
            cat.locations.remove(location.id);
         }
      }
      locations.put(location.id, location);
      location.category.locations.put(location.id, location);
   }

   public TransportLocation removeLocation(int location) {
      TransportLocation loc = locations.get(location);
      if (loc == null) {
         return null;
      } else {
         loc.category.locations.remove(location);
         locations.remove(location);
         saveCategories();
         return loc;
      }
   }

   public void saveCategory(CompoundTag compound) {
      int id = compound.getInt("CategoryId");
      if (id < 0) { id = getUniqueIdCategory(); }
      if (categories.containsKey(id)) {
         categories.get(id).load(compound);
         if (CustomNpcs.Server != null) {
            for (int locID : categories.get(id).locations.keySet()) {
               TransportLocation loc = categories.get(id).locations.get(locID);
               if (loc.npc != null) {
                  ServerLevel level = CustomNpcs.Server.getLevel(loc.dimension);
                  if (level != null) {
                     Entity entity = level.getEntity(loc.npc);
                     if (entity instanceof EntityNPCInterface
                             && ((EntityNPCInterface) entity).role instanceof RoleTransporter
                             && ((RoleTransporter) ((EntityNPCInterface) entity).role).transportId == locID
                             && !((RoleTransporter) ((EntityNPCInterface) entity).role).name
                             .equals(loc.name)) {
                        ((RoleTransporter) ((EntityNPCInterface) entity).role).name = loc.name;
                     }
                  }
               }
            }
         }
      } else {
         TransportCategory category = new TransportCategory();
         category.load(compound);
         category.id = id;
         categories.put(id, category);
      }
      saveCategories();
   }

   public void removeCategory(int id) {
      if (categories.size() != 1) {
         TransportCategory cat = categories.get(id);
         if (cat != null) {
            for (int i : cat.locations.keySet()) { locations.remove(i); }
            categories.remove(id);
            saveCategories();
         }
      }
   }

   public boolean containsLocationName(String name) {
      name = name.toLowerCase();
      for (TransportLocation loc : new ArrayList<>(locations.values())) {
         if (loc.name.equalsIgnoreCase(name)) { return true; }
      }
      return false;
   }

   public TransportLocation saveLocation(int categoryId, CompoundTag compound, EntityNPCInterface npc) {
      TransportCategory category = categories.get(categoryId);
      if (category != null && npc.role.getType() == 4) {
         RoleTransporter role = (RoleTransporter)npc.role;
         TransportLocation location = new TransportLocation();
         location.load(compound);
         location.category = category;
         if (role.hasTransport()) { location.id = role.transportId; }
         if (location.id < 0 || !locations.get(location.id).name.equals(location.name)) {
            while(containsLocationName(location.name)) { location.name = location.name + "_"; }
         }
         if (location.id < 0) { location.id = getUniqueIdLocation(); }
         category.locations.put(location.id, location);
         locations.put(location.id, location);
         saveCategories();
         return location;
      }
      return null;
   }

   public void sendTo(@Nonnull ServerPlayer player) {
      if (categories.isEmpty()) {
         TransportCategory cat = new TransportCategory();
         cat.id = 1;
         cat.title = "Default";
         categories.put(cat.id, cat);
      }
      List<TransportCategory> list = getCategories();
      Packets.send(player, new PacketSyncUpdate(-1, 14, getNBT()));
      for (TransportCategory cat : list) {
         CompoundTag compound = new CompoundTag();
         cat.save(compound);
         Packets.send(player, new PacketSyncUpdate(cat.id, 14, compound));
      }
   }

   public List<TransportCategory> getCategories() { return new ArrayList<>(categories.values()); }

}
