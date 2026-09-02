package noppes.npcs.controllers;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.command.CommandException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.containers.NpcMiscInventory;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.util.Util;
import noppes.npcs.util.NBTJsonUtil;

import javax.annotation.Nullable;

public class PlayerDataController {

	public static PlayerDataController instance;

	public PlayerDataController() {
		CustomNpcs.debugData.start(null);
		PlayerDataController.instance = this;
		File dir = CustomNpcs.getWorldSaveDirectory("playerdata");
		if (dir == null) { return; }
        for (File playerDir : Objects.requireNonNull(dir.listFiles())) {
			// OLD
			if (!playerDir.isDirectory() && playerDir.getName().endsWith(".json")) {
				try {
					NBTTagCompound nbt = NBTJsonUtil.LoadFile(playerDir);
					String uuid = "nouuidplayer", name = "nonameplayer";
					if (nbt.hasKey("PlayerName", 8) && !nbt.getString("PlayerName").isEmpty()) {
						name = nbt.getString("PlayerName");
					}
					if (nbt.hasKey("UUID", 8) && !nbt.getString("UUID").isEmpty()) {
						uuid = nbt.getString("UUID");
					}

					// banks
					File banksDirTemp = CustomNpcs.getWorldSaveDirectory("playerdata/" + uuid + "/banks");
					if (banksDirTemp == null) {
						CustomNpcs.debugData.end(null);
						return;
					}
					if ((banksDirTemp.exists() || banksDirTemp.mkdirs()) && nbt.hasKey("BankData", 9)) {
						for (int i = 0; i < nbt.getTagList("BankData", 10).tagCount(); i++) {
							NBTTagCompound nbtOldBank = nbt.getTagList("BankData", 10).getCompoundTagAt(i);
							NBTTagCompound nbtBD = new NBTTagCompound();
							int bankID = nbtOldBank.getInteger("DataBankId");
							nbtBD.setInteger("id", bankID);
							int maxCells = nbtOldBank.getInteger("UnlockedSlots");
							NBTTagList list = new NBTTagList();
							for (int c = 0; c < nbtOldBank.getTagList("BankInv", 10).tagCount(); c++) {
								NBTTagCompound nbtOldCeil = nbtOldBank.getTagList("BankInv", 10).getCompoundTagAt(c);
								int ceilID = nbtOldCeil.getInteger("Slot");
								if (ceilID >= maxCells) {
									continue;
								}
								NBTTagCompound nbtCeil = new NBTTagCompound();
								int slots = 27;
								for (int u = 0; u < nbtOldBank.getTagList("UpdatedSlots", 10).tagCount(); u++) {
									if (nbtOldBank.getTagList("UpdatedSlots", 10).getCompoundTagAt(u)
											.getInteger("Slot") == ceilID) {
										if (nbtOldBank.getTagList("UpdatedSlots", 10).getCompoundTagAt(u)
												.getBoolean("Boolean")) {
											slots = 54;
										}
										break;
									}
								}
								NpcMiscInventory inv = new NpcMiscInventory(slots);
								inv.load(nbtOldCeil.getCompoundTag("BankItems"));
								nbtCeil.setInteger("ceil", ceilID);
								nbtCeil.setInteger("slots", slots);
								NBTTagCompound invNbt = inv.save();
								nbtCeil.setTag("NpcMiscInv", invNbt.getTag("NpcMiscInv"));
								list.appendTag(nbtCeil);
							}
							nbtBD.setTag("ceils", list);
							File bankFile = new File(banksDirTemp, bankID + ".dat");
							if (!bankFile.exists() && !bankFile.createNewFile()) {
								LogWriter.error("Not create player bank data ");
							}
							CompressedStreamTools.writeCompressed(nbtBD, Files.newOutputStream(bankFile.toPath()));
						}
					}

					// main
					File playerDirTemp = new File(dir, uuid);
					if (playerDirTemp.exists() || playerDirTemp.mkdirs()) {
						File tempFile = new File(playerDirTemp, name + ".json");
						if (tempFile.exists() || tempFile.createNewFile()) {
							nbt.removeTag("BankData");
							Util.instance.saveFile(tempFile, nbt);
						}
						Util.instance.removeFile(playerDir);
					}
				} catch (Exception e) {
					LogWriter.error("Error loading Old file: " + playerDir.getAbsolutePath(), e);
				}
			}
		}
		CustomNpcs.debugData.end(null);
	}

	public void addPlayerMessage(MinecraftServer server, String username, PlayerMail mail) {
		PlayerData data = getDataFromUsername(server, username);
		if (data != null) {
			data.mailData.addMail(mail);
			data.save(false);
		}
	}

	@SuppressWarnings("ConstantConditions")
	public @Nullable PlayerData getDataFromUsername(@Nullable MinecraftServer server, @Nullable String userPartNameOrUUID) {
		if (userPartNameOrUUID == null || userPartNameOrUUID.isEmpty()) { return null; }
		if (server == null) { server = CustomNpcs.Server; }
		if (server != null) {
			EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(userPartNameOrUUID);
			if (player != null) { return PlayerData.get(player); }
			try {
				player = server.getPlayerList().getPlayerByUUID(UUID.fromString(userPartNameOrUUID));
				if (player != null) { return PlayerData.get(player); }
			}
			catch (Exception ignored) { }
		}
		File dir = CustomNpcs.getWorldSaveDirectory("playerdata");
		if (dir != null && dir.exists()) {
			File[] dirs = dir.listFiles();
			if (dirs != null) {
				for (File playerDir : dirs) {
					if (!playerDir.isDirectory()) { continue; }
					File[] files = playerDir.listFiles();
					if (files != null) {
						for (File file : files) {
							if (file.isFile() && file.getName().endsWith(".json")) {
								String uuid = playerDir.getName();
								String name = file.getName().replace(".json", "");
								if (name.toLowerCase().contains(userPartNameOrUUID.toLowerCase()) || uuid.equalsIgnoreCase(userPartNameOrUUID)) {
									if (server != null) {
										EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(name);
										if (player != null) { return PlayerData.get(player); }
									}
									PlayerData data = new PlayerData();
									data.setNBT(PlayerData.loadPlayerData(uuid, name));
									return data;
								}
								break;
							}
						}
					}
				}
			}
		}
		return null;
	}

	private File getPlayerDirectory(String user_name_or_uuid) {
		for (File playerDir : Objects.requireNonNull(Objects.requireNonNull(CustomNpcs.getWorldSaveDirectory("playerdata")).listFiles())) {
			if (!playerDir.isDirectory()) {
				continue;
			}
			if (playerDir.getName().equalsIgnoreCase(user_name_or_uuid)) {
				return playerDir;
			}
			File[] files = playerDir.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile() && file.getName().endsWith(".json")
							&& file.getName().replace(".json", "").equalsIgnoreCase(user_name_or_uuid)) {
						return playerDir;
					}
				}
			}
		}
		return null;
	}

	public List<String> getPlayerNames() {
		List<String> list = new ArrayList<>();
		for (File playerDir : Objects.requireNonNull(Objects.requireNonNull(CustomNpcs.getWorldSaveDirectory("playerdata")).listFiles())) {
			if (!playerDir.isDirectory()) { continue; }
			File[] files = playerDir.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile() && file.getName().endsWith(".json")) {
						list.add(file.getName().replace(".json", ""));
						break;
					}
				}
			}
		}
		return list;
	}

	public List<PlayerData> getPlayersData(ICommandSender sender, String username) throws CommandException {
		ArrayList<PlayerData> list = new ArrayList<>();
		List<EntityPlayerMP> players = EntitySelector.matchEntities(sender, username, EntityPlayerMP.class);
		if (players.isEmpty()) {
			MinecraftServer server = CustomNpcs.Server;
			if (server == null) { server = sender.getServer(); }
			PlayerData data = getDataFromUsername(Objects.requireNonNull(server), username);
			if (data != null) {
				list.add(data);
			}
		} else {
			for (EntityPlayer player : players) {
				list.add(PlayerData.get(player));
			}
		}
		return list;
	}

	public String hasPlayer(String user_name_or_uuid) {
		File playerDir = getPlayerDirectory(user_name_or_uuid);
		String realName = "";
		if (playerDir != null) {
			File[] files = playerDir.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile() && file.getName().endsWith(".json")
							&& file.getName().replace(".json", "").equalsIgnoreCase(user_name_or_uuid)) {
						realName = file.getName().replace(".json", "");
						break;
					}
				}
			}
		}
		return realName;
	}

}
