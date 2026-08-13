package noppes.npcs;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiClose;
import noppes.npcs.packets.client.PacketSync;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.NBTJsonUtil;
import noppes.npcs.util.Util;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import javax.annotation.Nullable;

public class CustomNpcsPermissions {

	public static CustomNpcsPermissions getInstance() {
		if (instance == null) { instance = new CustomNpcsPermissions(); }
		return instance;
	}

	public static class Permission {

		protected boolean defaultValue = true;
		protected final String name;

		public Permission(String nameIn) { name = nameIn; }

		public Permission(String nameIn, boolean defaultValueIn) {
			this(nameIn);
			defaultValue = defaultValueIn;
		}

		public String getNodeName() { return name; }

	}

	protected static CustomNpcsPermissions instance;
	public static final Map<Permission, List<String>> permissions;

	public static final Permission NPC_DELETE = new Permission(CustomNpcs.MODID + ".npc.delete");
	public static final Permission NPC_CREATE = new Permission(CustomNpcs.MODID + ".npc.create");
	public static final Permission NPC_GUI = new Permission(CustomNpcs.MODID + ".npc.gui");
	public static final Permission NPC_FREEZE = new Permission(CustomNpcs.MODID + ".npc.freeze");
	public static final Permission NPC_RESET = new Permission(CustomNpcs.MODID + ".npc.reset");
	public static final Permission NPC_ADVANCED = new Permission(CustomNpcs.MODID + ".npc.advanced");
	public static final Permission NPC_DISPLAY = new Permission(CustomNpcs.MODID + ".npc.display");
	public static final Permission NPC_INVENTORY = new Permission(CustomNpcs.MODID + ".npc.inventory");
	public static final Permission NPC_STATS = new Permission(CustomNpcs.MODID + ".npc.stats");
	public static final Permission NPC_CLONE = new Permission(CustomNpcs.MODID + ".npc.clone");
	public static final Permission GLOBAL_LINKED = new Permission(CustomNpcs.MODID + ".global.linked");
	public static final Permission GLOBAL_PLAYERDATA = new Permission(CustomNpcs.MODID + ".global.playerdata");
	public static final Permission GLOBAL_BANK = new Permission(CustomNpcs.MODID + ".global.bank");
	public static final Permission GLOBAL_DIALOG = new Permission(CustomNpcs.MODID + ".global.dialog");
	public static final Permission GLOBAL_QUEST = new Permission(CustomNpcs.MODID + ".global.quest");
	public static final Permission GLOBAL_FACTION = new Permission(CustomNpcs.MODID + ".global.faction");
	public static final Permission GLOBAL_TRANSPORT = new Permission(CustomNpcs.MODID + ".global.transport");
	public static final Permission GLOBAL_RECIPE = new Permission(CustomNpcs.MODID + ".global.recipe");
	public static final Permission GLOBAL_NATURALSPAWN = new Permission(CustomNpcs.MODID + ".global.naturalspawn");
	public static final Permission SPAWNER_MOB = new Permission(CustomNpcs.MODID + ".spawner.mob");
	public static final Permission SPAWNER_CREATE = new Permission(CustomNpcs.MODID + ".spawner.create");
	public static final Permission TOOL_MOUNTER = new Permission(CustomNpcs.MODID + ".tool.mounter");
	public static final Permission TOOL_PATHER = new Permission(CustomNpcs.MODID + ".tool.pather");
	public static final Permission TOOL_SCRIPTER = new Permission(CustomNpcs.MODID + ".tool.scripter");
	public static final Permission TOOL_NBTBOOK = new Permission(CustomNpcs.MODID + ".tool.nbtbook");
	public static final Permission EDIT_VILLAGER = new Permission(CustomNpcs.MODID + ".edit.villager");
	public static final Permission EDIT_BLOCKS = new Permission(CustomNpcs.MODID + ".edit.blocks");
	public static final Permission SOULSTONE_ALL = new Permission(CustomNpcs.MODID + ".soulstone.all");
	public static final Permission SCENES = new Permission(CustomNpcs.MODID + ".scenes");

	// in 1.20.1
	public static final Permission NPC_AI = new Permission(CustomNpcs.MODID + ".npc.ai");

	// New from Unofficial (BetaZavr)
	public static final Permission TOOL_BUILDERS = new Permission(CustomNpcs.MODID + ".tool.builders");
	public static final Permission TOOL_TELEPORTER = new Permission(CustomNpcs.MODID + ".tool.teleporter");
	public static final Permission EDIT_PERMISSION = new Permission(CustomNpcs.MODID + ".edit.permission", false);
	public static final Permission EDIT_CLIENT_SCRIPT = new Permission(CustomNpcs.MODID + ".edit.client.script", false);
	public static final Permission EDIT_ELEMENTS = new Permission(CustomNpcs.MODID + ".edit.elements", false);
	public static final Permission GLOBAL_MARKETS = new Permission(CustomNpcs.MODID + ".global.markets");
	public static final Permission GLOBAL_AUCTIONS = new Permission(CustomNpcs.MODID + ".global.auctions");
	public static final Permission GLOBAL_MAIL = new Permission(CustomNpcs.MODID + ".global.mail");
	public static final Permission MONEY_MANAGER = new Permission(CustomNpcs.MODID + ".money.manager");
	public static final Permission DONAT_MANAGER = new Permission(CustomNpcs.MODID + ".donat.manager", false);

	public static boolean hasPermission(EntityPlayerMP player, Permission permission) {
		if (permission == null) { return true; }
		if (CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) { return false; }
		return CustomNpcs.DisablePermissions ?
				PermissionAPI.hasPermission(player, permission.name) :
				inData(permission.name, player);
	}

	public static boolean hasPermission(EntityPlayerMP player, String permission) {
		if (permission == null) { return true; }
		for (Permission p : new ArrayList<>(permissions.keySet())) {
			if (p.name.equalsIgnoreCase(permission)) { return hasPermission(player, p); }
		}
		return false;
	}

	private CustomNpcsPermissions() {
		load();
	}

	private void load() {
		if (!CustomNpcs.DisablePermissions) {
			CustomNpcs.debugData.start("Mod");
			LogManager.getLogger(CustomNpcsPermissions.class).info(CustomNpcs.MODNAME + " Permissions available:");
			ArrayList<Permission> nodes = new ArrayList<>(permissions.keySet());
			for (Permission p : nodes) {
				PermissionAPI.registerNode(p.name, p.defaultValue ? DefaultPermissionLevel.ALL : DefaultPermissionLevel.OP, p.name);
				LogManager.getLogger(CustomNpcsPermissions.class).info(p.name);
			}
			// New from Unofficial (BetaZavr)
			// load data
			File file = new File(CustomNpcs.getWorldSaveDirectory(), "permissions.json");
			boolean needSave = !file.exists();
			if (!needSave) {
				try {
					NBTTagCompound compound = NBTJsonUtil.LoadFile(file);
					List<Permission> hasInFile = new ArrayList<>();
					for (String nodeName :  compound.getKeySet()) {
						boolean found = false;
						for (Permission node : nodes) {
							if (node.name.equals(nodeName)) {
								NBTTagList list = compound.getTagList(nodeName, 8);
								permissions.get(node).clear();
								for (int i = 0; i < list.tagCount(); i++) { permissions.get(node).add(list.getStringTagAt(i)); }
								hasInFile.add(node);
								found = true;
								break;
							}
						}
						if (!found) { needSave = true; }
					}
					if (!needSave) {
						for (Permission node : nodes) {
							if (!hasInFile.contains(node)) {
								needSave = true;
								break;
							}
						}
					}
				}
				catch (Exception e) { LogWriter.error(e); }
			}
			if (needSave) { save(); }
			CustomNpcs.debugData.end("Mod");
		}
	}

	// New from Unofficial (BetaZavr)
	static {
		List<Permission> list = new ArrayList<>();
		try {
			for (Field field : CustomNpcsPermissions.class.getDeclaredFields()) {
				if (field.getType() == Permission.class) { list.add((Permission) field.get(CustomNpcsPermissions.class)); }
			}
		}
		catch (Exception ignored) { }
		list.sort((o1, o2) -> o1.name.compareToIgnoreCase(o2.name));
		Map<Permission, List<String>> map = new LinkedHashMap<>();
		for (Permission node : list) {
			map.put(node, new ArrayList<>());
			if (node.defaultValue) {
				map.get(node).add("All");
				map.get(node).add("Command Block");
			}
		}
		permissions = ImmutableMap.copyOf(map);
	}

	private static Boolean inData(String nodeName, @Nullable EntityPlayerMP player) {
		for (Permission permission : new ArrayList<>(permissions.keySet())) {
			if (permission.name.equals(nodeName)) {
				if (player == null) { return permissions.get(permission).contains("Command Block"); }
				return permissions.get(permission).contains("All") ||
						permissions.get(permission).contains(player.getName());
			}
		}
		return false;
	}

	private static void save() {
		Util.instance.saveFile(new File(CustomNpcs.getWorldSaveDirectory(), "permissions.json"), getNBT());
	}

	private static NBTTagCompound getNBT() {
		NBTTagCompound compound = new NBTTagCompound();
		for (Permission node : new ArrayList<>(permissions.keySet())) {
			NBTTagList list = new NBTTagList();
			for (String player :  permissions.get(node)) { list.appendTag(new NBTTagString(player)); }
			compound.setTag(node.name, list);
		}
		return compound;
	}

	@SideOnly(Side.CLIENT)
	public static void putToData(Map<Component, List<Component>> data) {
		data.clear();
		for (Permission node : new ArrayList<>(permissions.keySet())) {
			List<Component> players = new ArrayList<>();
			for (String name : permissions.get(node)) { players.add(Component.literal(name)); }
			data.put(Component.translatable("permission." + node.getNodeName()), players);
		}
	}

	@SideOnly(Side.CLIENT)
	public static void set(NBTTagCompound compound) {
		ArrayList<Permission> nodes = new ArrayList<>(permissions.keySet());
		for (String nodeName : compound.getKeySet()) {
			for (Permission node : nodes) {
				if (node.name.equals(nodeName)) {
					NBTTagList list = compound.getTagList(nodeName, 8);
					permissions.get(node).clear();
					for (int i = 0; i < list.tagCount(); i++) { permissions.get(node).add(list.getStringTagAt(i)); }
					break;
				}
			}
		}
	}

	public static void add(String nodeName, String playerName, EntityPlayerMP player) {
		if (!hasPermission(player, EDIT_PERMISSION)) {
			Packets.send(player, new PacketGuiClose());
			return;
		}
		if (playerName == null) { playerName = "Command Block"; }
		else if (playerName.isEmpty()) { playerName = "All"; }
		for (Permission permission : new ArrayList<>(permissions.keySet())) {
			if (permission.name.equals(nodeName)) {
				if (!permissions.get(permission).contains(playerName)) {
					permissions.get(permission).add(playerName);
					save();
				}
				break;
			}
		}
		sendTo(player);
	}

	public static void remove(String nodeName, String playerName, EntityPlayerMP player) {
		if (!hasPermission(player, EDIT_PERMISSION)) {
			Packets.send(player, new PacketGuiClose());
			return;
		}
		if (playerName == null) { playerName = "Command Block"; }
		else if (playerName.isEmpty()) { playerName = "All"; }
		for (Permission permission : new ArrayList<>(permissions.keySet())) {
			if (permission.name.equals(nodeName)) {
				if (permissions.get(permission).contains(playerName)) {
					permissions.get(permission).remove(playerName);
					save();
				}
				break;
			}
		}
		sendTo(player);
	}

	public static void sendTo(EntityPlayerMP player) {
		if (player == null) { return; }
		if (!hasPermission(player, EDIT_PERMISSION)) { Packets.send(player, new PacketGuiClose()); }
		else { Packets.send(player, new PacketSync(14, getNBT(), true)); }
	}

}
