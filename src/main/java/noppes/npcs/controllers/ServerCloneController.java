package noppes.npcs.controllers;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.packets.server.SPacketToolMobSpawner;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.handler.ICloneHandler;
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
				Map<Integer, Map<String, NBTTagCompound>> clones = loadOldClones(file);
				if (file.delete()) {
					file = new File(dir, "clonednpcs.dat_old");
					if (!file.exists() || file.delete()) {
						for (int tab : clones.keySet()) {
							Map<String, NBTTagCompound> map = clones.get(tab);
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

	public File getDir() {
		File dir = CustomNpcs.getWorldSaveDirectory();
		if (dir != null) {
			dir = new File(dir, "clones");
			if (dir.exists() || dir.mkdir()) { return dir; }
		}
		return dir;
	}

	private Map<Integer, Map<String, NBTTagCompound>> loadOldClones(File file) throws Exception {
		Map<Integer, Map<String, NBTTagCompound>> clones = new HashMap<>();
		NBTTagCompound nbt = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
		NBTTagList list = nbt.getTagList("Data", 10);
		for (int i = 0; i < list.tagCount(); ++i) {
			NBTTagCompound compound = list.getCompoundTagAt(i);
			if (!compound.hasKey("ClonedTab")) { compound.setInteger("ClonedTab", 1); }
			Map<String, NBTTagCompound> tab = clones.computeIfAbsent(compound.getInteger("ClonedTab"), k -> new HashMap<>());
			String name = compound.getString("ClonedName");
			for (int number = 1; tab.containsKey(name); name = String.format("%s%s", compound.getString("ClonedName"), number)) { ++number; }
			compound.removeTag("ClonedName");
			compound.removeTag("ClonedTab");
			compound.removeTag("ClonedDate");
			cleanTags(compound);
			tab.put(name, compound);
		}
		return clones;
	}

	public @Nullable NBTTagCompound getCloneData(@Nullable ICommandSender player, String name, int tab) {
		File dir = getDir();
		if (dir == null || name == null || name.isEmpty()) { return null; }
		name = Util.instance.sanitizeFilename(name);
		File file = new File(dir, tab + "/" + name + ".json");
		if (!file.exists()) {
			if (player != null) {
				player.sendMessage(Component.translatable("message.clone.not.found.file", tab, name).getParent());
			}
			return null;
		}
		try { return NBTJsonUtil.LoadFile(file); }
		catch (Exception e) {
			LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
			if (player != null) { player.sendMessage(Component.literal(e.getMessage()).getParent()); }
			return null;
		}
	}

	public void saveClone(int tab, String name, NBTTagCompound compound) {
		try {
			File dir = new File(getDir(), tab + "");
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
		File dir = new File(getDir(), tab + "");
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
		File file = new File(getDir(), tab + "/" + Util.instance.sanitizeFilename(name) + ".json");
		return file.exists() && file.delete();
	}

	public void addClone(NBTTagCompound nbttagcompound, String name, int tab) {
		cleanTags(nbttagcompound);
		saveClone(tab, name, nbttagcompound);
	}

	public void cleanTags(NBTTagCompound compound) {
		if (compound.hasKey("ItemGiverId")) { compound.setInteger("ItemGiverId", 0); }
		if (compound.hasKey("TransporterId")) { compound.setInteger("TransporterId", -1); }
		compound.removeTag("StartPosNew");
		compound.removeTag("StartPos");
		compound.removeTag("MovingPathNew");
		compound.removeTag("Pos");
		compound.removeTag("Riding");
		compound.removeTag("UUID");
		compound.removeTag("UUIDMost");
		compound.removeTag("UUIDLeast");
		if (!compound.hasKey("ModRev")) { compound.setInteger("ModRev", 1); }
		if (compound.hasKey("TransformRole")) {
			NBTTagCompound adv = compound.getCompoundTag("TransformRole");
			adv.setInteger("TransporterId", -1);
			compound.setTag("TransformRole", adv);
		}
		if (compound.hasKey("TransformJob")) {
			NBTTagCompound adv = compound.getCompoundTag("TransformJob");
			adv.setInteger("ItemGiverId", 0);
			compound.setTag("TransformJob", adv);
		}
		if (compound.hasKey("TransformAI")) {
			NBTTagCompound adv = compound.getCompoundTag("TransformAI");
			adv.removeTag("StartPosNew");
			adv.removeTag("StartPos");
			adv.removeTag("MovingPathNew");
			compound.setTag("TransformAI", adv);
		}
		if (compound.hasKey("id")) {
			String id = compound.getString("id");
			id = id.replace(CustomNpcs.MODID + ".", CustomNpcs.MODID + ":");
			compound.setString("id", id);
		}
	}

	@Override
	public IEntity<?> spawn(double x, double y, double z, int tab, String name, IWorld world) {
		if (world == null || world.getMCWorld().isRemote) {
			LogWriter.debug("CloneHandler summoning Error: World is Client: "
					+ (world == null ? "null" : world.getMCWorld().isRemote + " - " + world));
			return null;
		}
		NBTTagCompound compound = getCloneData(null, name, tab);
		if (compound == null) { throw new CustomNPCsException("Unknown clone tab:" + tab + " name:" + name); }
		Entity entity = SPacketToolMobSpawner.spawnClone(compound, x, y, z, world.getMCWorld());
		if (entity == null) {
			LogWriter.debug(
					"CloneHandler summoning error: Failed to create an entity based on tab: " + tab + "; name: \""
							+ name + "\"; compound:" + compound.toString().length());
			return null;
		}
		return Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity);
	}

	@Override
	public IEntity<?> get(int tab, String name, IWorld world) {
		NBTTagCompound compound = getCloneData(null, name, tab);
		if (compound == null) { throw new CustomNPCsException("Unknown clone tab:" + tab + " name:" + name); }
		cleanTags(compound);
		Entity entity = EntityList.createEntityFromNBT(compound, world.getMCWorld());
		return entity == null ? null : Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity);
	}

	@Override
	public void set(int tab, String name, IEntity<?> entity) {
		NBTTagCompound compound = new NBTTagCompound();
		if (!entity.getMCEntity().writeToNBTAtomically(compound)) { throw new CustomNPCsException("Cannot save dead entities"); }
		cleanTags(compound);
		saveClone(tab, name, compound);
	}

	@Override
	public void remove(int tab, String name) { removeClone(name, tab); }

	public boolean hasClone(int tab, String name) { return getCloneData(null, name, tab) != null; }

}
