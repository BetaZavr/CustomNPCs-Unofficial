package noppes.npcs.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.handler.ICloneHandler;
import noppes.npcs.packets.server.SPacketToolMobSpawner;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.NBTJsonUtil;
import noppes.npcs.util.Util;

import javax.annotation.Nullable;

public class ServerCloneController implements ICloneHandler {

   public long lastLoaded = System.currentTimeMillis();
   public static ServerCloneController Instance;

   public ServerCloneController() { loadClones(); }

   private void loadClones() {
      try {
         File dir = new File(getDir(), "..");
         File file = new File(dir, "clonednpcs.dat");
         if (file.exists()) {
            Map<Integer, Map<String, CompoundTag>> clones = loadOldClones(file);
            if (file.delete()) {
               file = new File(dir, "clonednpcs.dat_old");
               if (!file.exists() || file.delete()) {
                  for (int tab : clones.keySet()) {
                     Map<String, CompoundTag> map = clones.get(tab);
                     for (String name : map.keySet()) {
                        saveClone(tab, name, map.get(name));
                     }
                  }
               }
            }
         }
      }
      catch (Exception e) { LogWriter.except(e); }
   }

   public @Nullable File getDir() {
      File dir = CustomNpcs.getLevelSaveDirectory();
      if (dir != null) {
         try {
            // Normalize the path to resolve '..' and symbolic links
            dir = new File(dir.getCanonicalFile(), "clones");
         }
         catch (Exception e) {
            // Fallback to absolute path if canonicalization fails
            dir = new File(dir.getAbsoluteFile(), "clones");
         }
         if (dir.exists() || dir.mkdir()) { return dir; }
      }
      return dir;
   }

   private Map<Integer, Map<String, CompoundTag>> loadOldClones(File file) throws Exception {
      Map<Integer, Map<String, CompoundTag>> clones = new HashMap<>();
      CompoundTag compound = NbtIo.readCompressed(new FileInputStream(file));
      ListTag list = compound.getList("Data", 10);
      for (int i = 0; i < list.size(); ++i) {
         CompoundTag nbt = list.getCompound(i);
         if (!nbt.contains("ClonedTab")) { nbt.putInt("ClonedTab", 1); }
         Map<String, CompoundTag> tab = clones.computeIfAbsent(nbt.getInt("ClonedTab"), k -> new HashMap<>());
         String name = nbt.getString("ClonedName");
         for (int number = 1; tab.containsKey(name); name = String.format("%s%s", nbt.getString("ClonedName"), number)) { ++number; }
         nbt.remove("ClonedName");
         nbt.remove("ClonedTab");
         nbt.remove("ClonedDate");
         cleanTags(nbt);
         tab.put(name, nbt);
      }
      return clones;
   }

   public @Nullable CompoundTag getCloneData(@Nullable CommandSourceStack player, String name, int tab) {
      File dir = getDir();
      if (dir == null || name == null || name.isEmpty()) { return null; }
      File file = new File(dir, tab + "/" + name + ".json");
      if (!file.exists()) {
         if (player != null) { player.sendFailure(Component.translatable("message.clone.not.found.file", tab, name)); }
         return null;
      }
      try { return NBTJsonUtil.LoadFile(file); }
      catch (Exception e) {
         LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
         if (player != null) { player.sendFailure(Component.literal(e.getMessage())); }
         return null;
      }
   }

   public void saveClone(int tab, String name, CompoundTag compound) {
      try {
         File dir = new File(getDir(), "" + tab);
         if (!dir.exists() && !dir.mkdirs()) {
            LogWriter.error("Error save server clone: Directory not created!");
            return;
         }
         name = Util.instance.sanitizeFilename(name);
         File file = new File(dir, name + ".json_new");
         File file1 = new File(dir, name + ".json");
         Util.instance.saveFile(file, compound);
         if (file1.exists() && !file1.delete() || !file.renameTo(file1)) {
            LogWriter.error("Error save server clone: Delete or rename " + file1 + "!");
         }
         lastLoaded = System.currentTimeMillis();
      }
      catch (Exception e) { LogWriter.except(e); }
   }

   public List<String> getClones(int tab) {
      List<String> list = new ArrayList<>();
      File dir = new File(getDir(), "" + tab);
      if (dir.exists() && dir.isDirectory()) {
         String[] files = dir.list();
         if (files != null) {
            for (String file : files) {
               if (file.endsWith(".json")) { list.add(file.substring(0, file.length() - 5)); }
            }
         }
      }
      return list;
   }

   public boolean removeClone(String name, int tab) {
      File file = new File(getDir(), tab + "/" + name + ".json");
      return file.exists() && file.delete();
   }

   public void addClone(CompoundTag compound, String name, int tab) {
      cleanTags(compound);
      saveClone(tab, name, compound);
   }

   public void cleanTags(CompoundTag compound) {
      if (compound.contains("ItemGiverId")) { compound.putInt("ItemGiverId", 0); }
      if (compound.contains("TransporterId")) { compound.putInt("TransporterId", -1); }
      compound.remove("HomeDimensionId");
      compound.remove("StartPosNew");
      compound.remove("StartPos");
      compound.remove("MovingPathNew");
      compound.remove("Pos");
      compound.remove("Riding");
      compound.remove("UUID");
      compound.remove("UUIDMost");
      compound.remove("UUIDLeast");
      if (!compound.contains("ModRev")) { compound.putInt("ModRev", 1); }
      CompoundTag adv;
      if (compound.contains("TransformRole")) {
         adv = compound.getCompound("TransformRole");
         adv.putInt("TransporterId", -1);
         compound.put("TransformRole", adv);
      }
      if (compound.contains("TransformJob")) {
         adv = compound.getCompound("TransformJob");
         adv.putInt("ItemGiverId", 0);
         compound.put("TransformJob", adv);
      }
      if (compound.contains("TransformAI")) {
         adv = compound.getCompound("TransformAI");
         adv.remove("StartPosNew");
         adv.remove("StartPos");
         adv.remove("MovingPathNew");
         compound.put("TransformAI", adv);
      }
      if (compound.contains("id")) {
         String id = compound.getString("id");
         if (!CustomNpcs.FixUpdateFromPre_1_12) {
            id = id.replace(CustomNpcs.MODID + ".", CustomNpcs.MODID + ":");
         }
         compound.putString("id", id);
      }
   }

   @Override
   public IEntity<?> spawn(double x, double y, double z, int tab, String name, IWorld level) {
      if (level == null || level.getMCLevel().isClientSide()) {
         LogWriter.debug("CloneHandler summoning Error: World is Client: "
                 + (level == null ? "null" : level.getMCLevel().isClientSide() + " - " + level));
         return null;
      }
      CompoundTag compound = getCloneData(null, name, tab);
      if (compound == null) { throw new CustomNPCsException("Unknown clone tab:" + tab + " name:" + name); }
      Entity entity = SPacketToolMobSpawner.spawnClone(compound, x, y, z, level.getMCLevel());
      if (entity == null) {
         LogWriter.debug(
                 "CloneHandler summoning error: Failed to create an entity based on tab: " + tab + "; name: \""
                         + name + "\"; compound:" + compound.toString().length());
         return null;
      }
      return Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity);
   }

   @Override
   public IEntity<?> get(int tab, String name, IWorld level) {
      CompoundTag compound = getCloneData(null, name, tab);
      if (compound == null) { throw new CustomNPCsException("Unknown clone tab:" + tab + " name:" + name); }
      cleanTags(compound);
      Entity entity = EntityType.create(compound, level.getMCLevel()).orElse(null);
      return entity == null ? null : Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity);
   }

   @Override
   public void set(int tab, String name, IEntity<?> entity) {
      CompoundTag compound = new CompoundTag();
      if (!entity.getMCEntity().saveAsPassenger(compound)) { throw new CustomNPCsException("Cannot save dead entities"); }
      cleanTags(compound);
      saveClone(tab, name, compound);
   }

   @Override
   public void remove(int tab, String name) { removeClone(name, tab); }

   public boolean hasClone(int tab, String name) { return getCloneData(null, name, tab) != null; }

}
