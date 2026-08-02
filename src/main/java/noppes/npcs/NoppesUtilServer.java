package noppes.npcs;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.blocks.custom.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerDialogData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.custom.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.*;
import noppes.npcs.packets.server.SPacketGuiOpen;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;
import noppes.npcs.util.CustomNPCsScheduler;

import javax.annotation.Nonnull;

public class NoppesUtilServer {

	private static final HashMap<UUID, Quest> editingQuests = new HashMap<>();
	private static final HashMap<UUID, Quest> editingQuestsClient = new HashMap<>();

	public static void setEditingNpc(EntityPlayer player, EntityNPCInterface npc) {
		PlayerData data = PlayerData.get(player);
		data.editingNpc = npc;
		if (player instanceof EntityPlayerMP) { Packets.send((EntityPlayerMP) player, new PacketNpcEdit(npc == null ? -1 : npc.getEntityId())); }
	}

	public static EntityNPCInterface getEditingNpc(EntityPlayer player) { return PlayerData.get(player).editingNpc; }

	public static void setEditingQuest(@Nonnull EntityPlayer player, @Nonnull Quest quest) {
		if (player.world.isRemote) {
			NoppesUtilServer.editingQuestsClient.put(player.getUniqueID(), quest);
		} else {
			NoppesUtilServer.editingQuests.put(player.getUniqueID(), quest);
		}
	}

	public static Quest getEditingQuest(@Nonnull EntityPlayer player) {
		return player.world.isRemote ? editingQuestsClient.get(player.getUniqueID()) : editingQuests.get(player.getUniqueID());
	}

	public static void openDialog(EntityPlayer player, EntityNPCInterface npc, Dialog dia) {
		if (dia == null) { return; }
		Dialog dialog = dia.copy(player);
		PlayerData playerdata = PlayerData.get(player);
		if (EventHooks.onNPCDialog(npc, player, dialog)) {
			playerdata.dialogId = -1;
			return;
		}
		playerdata.dialogId = dialog.id;
		if (!(npc instanceof EntityDialogNpc) && dia.id >= 0) { Packets.sendDelayed((EntityPlayerMP) player, new PacketDialog(npc.getEntityId(), dialog.id), 100); }
		else {
			dialog.hideNPC = true;
			Packets.send((EntityPlayerMP) player, new PacketDialogDummy(npc.getName(), dialog.save(new NBTTagCompound())));
		}
		dia.factionOptions.addPoints(player);
		if (dialog.hasQuest()) { PlayerQuestController.addActiveQuest(dialog.getQuest(), player, false); }
		if (!dialog.command.isEmpty()) { runCommand(npc, npc.getName(), dialog.command, player); }
		if (dialog.mail.isValid()) { PlayerDataController.instance.addPlayerMessage(player.getServer(), player.getName(), dialog.mail); }
		// Change from Unofficial (BetaZavr)
		PlayerDialogData data = playerdata.dialogData;
		if (!data.has(dialog.id) && dialog.id >= 0) {
			data.read(dialog.id);
			playerdata.updateClient = true;
		}
		setEditingNpc(player, npc);
		// New from Unofficial (BetaZavr)
		CustomNPCsScheduler.runTack(() -> {
			for (QuestData qData : playerdata.questData.activeQuests.values()) {
				for (IQuestObjective obj : qData.quest.getObjectives(playerdata.scriptData.getIPlayer())) {
					if (obj.getType() != EnumQuestTask.DIALOG.ordinal()) { continue; }
					playerdata.questData.checkQuestCompletion(player, qData);
				}
			}
		});
	}

	public static String runCommand(ICommandSender sender, String name, String command, EntityPlayer player) {
		return runCommand(sender.getEntityWorld(), sender.getPosition(), name, command, player, sender);
	}

	public static String runCommand(World world, BlockPos pos, String name, String command, EntityPlayer player, ICommandSender sender) {
		if (!Objects.requireNonNull(world.getMinecraftServer()).isCommandBlockEnabled()) {
			CommonUtil.NotifyOPs("Cant run commands if CommandBlocks are disabled");
			LogWriter.warn("Cant run commands if CommandBlocks are disabled");
			return "Cant run commands if CommandBlocks are disabled";
		}
		if (player != null) { command = command.replace("@dp", player.getName()); }
		command = command.replace("@npc", name);
		TextComponentString output = new TextComponentString("");
		ICommandSender icommandsender = getCommandSource(world, pos, name, output, sender);
		ICommandManager icommandmanager = world.getMinecraftServer().getCommandManager();
		icommandmanager.executeCommand(icommandsender, command);
		if (output.getUnformattedText().isEmpty()) { return null; }
		return output.getUnformattedText();
	}

	private static @Nonnull ICommandSender getCommandSource(World world, BlockPos pos, String name, TextComponentString output, ICommandSender sender) {
		return new RConConsoleSource(Objects.requireNonNull(world.getMinecraftServer())) {

			@Override
			public boolean canUseCommand(int permLevel, @Nonnull String commandName) {
				return CustomNpcs.NpcUseOpCommands || permLevel <= 2;
			}

			@Override
			public Entity getCommandSenderEntity() {
				if (sender == null) { return null; }
				return sender.getCommandSenderEntity();
			}

			@Override
			public @Nonnull ITextComponent getDisplayName() { return new TextComponentString(this.getName()); }

			@Override
			public @Nonnull World getEntityWorld() { return world; }

			@Override
			public @Nonnull String getName() { return "@CustomNPCs-" + name; }

			@Override
			public @Nonnull BlockPos getPosition() { return pos; }

			@Override
			public @Nonnull Vec3d getPositionVector() { return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5); }

			@Override
			public boolean sendCommandFeedback() { return Objects.requireNonNull(this.getServer()).worlds[0].getGameRules().getBoolean("commandBlockOutput"); }

			@Override
			public void sendMessage(@Nonnull ITextComponent component) { output.appendSibling(component); }

		};
	}

	public static void sendOpenGui(EntityPlayerMP player, EnumGuiType gui, EntityNPCInterface npc) {
		SPacketGuiOpen.sendOpenGui(player, gui, npc, BlockPos.ORIGIN);
	}

	public static boolean openContainerGui(EntityPlayerMP player, EnumGuiType gui, Consumer<FriendlyByteBuf> extraDataWriter) {
		if (!gui.hasContainer) { return false; }
		try {
			final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
			extraDataWriter.accept(buffer);
			player.getNextWindowId();
			player.closeContainer();
			Container container = CommonProxy.getContainer(gui, player, buffer.copy());
			if (container != null) {
				int windowId = player.currentWindowId;
				player.openContainer = container;
				player.openContainer.windowId = windowId;
				net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerContainerEvent.Open(player, player.openContainer));
				Packets.send(player, new PacketGuiOpen(gui, buffer, windowId));
				player.openContainer.addListener(player);
				player.openContainer.detectAndSendChanges();
				return true;
			}
		}
		catch (Exception e) { LogWriter.error(e); }
		return false;
	}

	public static void sendScrollData(EntityPlayerMP player, Map<String, Integer> map) {
		UUID id = UUID.randomUUID();
		TreeMap<Integer, Map<String, Integer>> content = new TreeMap<>();
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeUUID(id);
		buf.writeInt(0);
		buf.writeInt(map.size());
		buf.writeInt(map.size());
		Map<String, Integer> part = new HashMap<>();
		for (Map.Entry<String, Integer> e : map.entrySet()) {
			buf.writeUtf(e.getKey());
			buf.writeInt(e.getValue());
			if (buf.writerIndex() > 65536) {
				content.put(content.size(), part);
				buf.clear();
				buf.writeInt(content.size() + 1);
				buf.writeInt(map.size());
				buf.writeInt(map.size());
				buf.writeUtf(e.getKey());
				buf.writeInt(e.getValue());
				part = new HashMap<>();
			}
			part.put(e.getKey(), e.getValue());
		}
		if (!part.isEmpty()) { content.put(content.size(), part); }
		if (content.isEmpty()) { Packets.send(player, new PacketGuiScrollData(new HashMap<>(), id, 0, 0)); }
		else {
			for (Map.Entry<Integer, Map<String, Integer>> e : content.entrySet()) {
				Packets.send(player, new PacketGuiScrollData(e.getValue(), id, e.getKey(), content.size() - 1));
			}
		}
	}

	public static void sendScrollData(EntityPlayerMP player, List<String> list) {
		UUID id = UUID.randomUUID();
		TreeMap<Integer, Vector<String>> content = new TreeMap<>();
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeUUID(id);
		buf.writeInt(0);
		buf.writeInt(list.size());
		buf.writeInt(list.size());
		Vector<String> part = new Vector<>();
		for (String s : list) {
			buf.writeUtf(s);
			if (buf.writerIndex() > 65536) {
				content.put(content.size(), part);
				buf.clear();
				buf.writeInt(content.size() + 1);
				buf.writeInt(list.size());
				buf.writeInt(list.size());
				buf.writeUtf(s);
				part = new Vector<>();
			}
			part.add(s);
		}
		if (!part.isEmpty()) { content.put(content.size(), part); }
		if (content.isEmpty()) { Packets.send(player, new PacketGuiScrollList(new Vector<>(), id, 0, 0)); }
		else {
			for (Map.Entry<Integer, Vector<String>> e : content.entrySet()) {
				Packets.send(player, new PacketGuiScrollList(e.getValue(), id, e.getKey(), content.size() - 1));
			}
		}
	}

	public static void sendGuiError(EntityPlayerMP player, int i) {
		Packets.send(player, new PacketGuiError(i, new NBTTagCompound()));
	}

	public static void sendGuiClose(EntityPlayerMP player, NBTTagCompound comp) {
		Packets.send(player, new PacketGuiClose(comp));
	}

	public static void givePlayerItem(Entity entity, EntityPlayer player, ItemStack item) {
		if (!entity.world.isRemote && item != null && !item.isEmpty()) {
			item = item.copy();
			float f = 0.7F;
			double d = (double)(entity.world.rand.nextFloat() * f) + (double)(1.0F - f);
			double d1 = (double)(entity.world.rand.nextFloat() * f) + (double)(1.0F - f);
			double d2 = (double)(entity.world.rand.nextFloat() * f) + (double)(1.0F - f);
			EntityItem entityItem = new EntityItem(entity.world, entity.posX + d, entity.posY + d1, entity.posZ + d2, item);
			entityItem.setPickupDelay(2);
			entity.world.spawnEntity(entityItem);
			if (player.inventory.addItemStackToInventory(item)) {
				entity.world.playSound(null, player.posX, player.posY, player.posZ,
						SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2f,
						((player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.7f + 1.0f) * 2.0f);
				player.onItemPickup(entityItem, item.getCount());
				PlayerQuestData playerdata = PlayerData.get(player).questData;
				CustomNPCsScheduler.runTack(() -> {
					for (QuestData data : playerdata.activeQuests.values()) {
						for (IQuestObjective obj : data.quest.getObjectives(player)) {
							if (obj.getType() != EnumQuestTask.ITEM.ordinal()) { continue; }
							playerdata.checkQuestCompletion(player, data);
						}
					}
				});
				if (item.getCount() <= 0) { entityItem.setDead(); }
			}
		}
	}

	public static BlockPos getClosePos(BlockPos origin, World world) {
		for (int x = -1; x < 2; ++x) {
			for (int z = -1; z < 2; ++z) {
				for (int y = 2; y >= -2; --y) {
					BlockPos pos = origin.add(x, y, z);
					if (world.isSideSolid(pos, EnumFacing.UP) && world.isAirBlock(pos.up()) && world.isAirBlock(pos.up(2))) {
						return pos.up();
					}
				}
			}
		}
		return world.getTopSolidOrLiquidBlock(origin);
	}

	public static void playSound(EntityLivingBase entity, SoundEvent sound, float volume, float pitch) {
		entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, sound, SoundCategory.NEUTRAL, volume, pitch);
	}

	public static void playSound(World world, BlockPos pos, SoundEvent sound, SoundCategory cat, float volume, float pitch) {
		world.playSound(null, pos, sound, cat, volume, pitch);
	}

	public static EntityPlayer getPlayer(MinecraftServer minecraftserver, UUID id) {
		List<EntityPlayerMP> list = minecraftserver.getPlayerList().getPlayers();
		for (EntityPlayer player : list) {
			if (id.equals(player.getUniqueID())) { return player; }
		}
		return null;
	}

	public static boolean isItemStackNull(ItemStack is) { return is == null || is.isEmpty(); }

	public static Entity getDamageSource(DamageSource damagesource) {
		Entity entity = damagesource.getTrueSource();
		if (entity == null) { entity = damagesource.getImmediateSource(); }
		if (entity instanceof EntityArrow && ((EntityArrow) entity).shootingEntity instanceof EntityLivingBase) { entity = ((EntityArrow) entity).shootingEntity; }
		else if (entity instanceof EntityThrowable) { entity = ((EntityThrowable) entity).getThrower(); }
		if (entity == null && damagesource.getTrueSource() != null) { entity = damagesource.getTrueSource(); }
		return entity;
	}

	// New from Unofficial BetaZavr
	public static NBTTagCompound setNpcDialog(int slot, int dialogId, EntityPlayer player) {
		EntityNPCInterface npc = getEditingNpc(player);
		if (npc == null || !DialogController.instance.hasDialog(dialogId)) { return new NBTTagCompound(); }
		if (slot >= 0 && slot < npc.dialogs.length) { npc.dialogs[slot] = dialogId; } // change
		else {
			int[] newIDs = new int[npc.dialogs.length + 1];
            System.arraycopy(npc.dialogs, 0, newIDs, 0, npc.dialogs.length);
			slot = npc.dialogs.length;
			newIDs[slot] = dialogId;
			npc.dialogs = newIDs;
		} // add
		Dialog dialog = DialogController.instance.get(dialogId);
		dialog.addNpc(slot, npc);
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("Id", dialog.id);
		compound.setInteger("Slot", slot);
		compound.setString("Category", dialog.category.title);
		compound.setString("Title", dialog.title);
		return compound;
	}

    public static void createItemFiles(ICustomElement customitem) {
		String name = customitem.getCustomName().toLowerCase();
		String fileName = "custom_" + customitem.getCustomName().toLowerCase();

		File modelsDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/models/item");
		File modelsObjDir = new File(modelsDir, "obj");
		File armorDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/models/armor");
		File armorObjDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/models/armor");
		if ((modelsDir.exists() || modelsDir.mkdirs()) &&
				(armorDir.exists() || armorDir.mkdirs()) &&
				(armorObjDir.exists() || armorObjDir.mkdirs()) &&
				(modelsObjDir.exists() || modelsObjDir.mkdirs())) {
            // Models
			File itemModel = new File(modelsDir, fileName + ".json");
			Map<File, String> modelDatas = new HashMap<>();
			if (customitem.getCustomNbt().getBoolean("IsOBJModel")) {
				File objFile = new File(modelsObjDir, name + ".obj");
				File mtlFile = new File(modelsObjDir, name + ".mtl");
				if (!itemModel.exists() || !objFile.exists() || !mtlFile.exists()) {
					modelDatas.put(itemModel, getDataFile("imas.dat", fileName, name));
					modelDatas.put(objFile, getDataFile("ima_o.dat", fileName, name));
					modelDatas.put(mtlFile, getDataFile("ima_m.dat", fileName, name));
				}
			}
			else {
				switch (customitem.getElementType()) {
					case (byte) 1: {
						if (!itemModel.exists()) { modelDatas.put(itemModel, getDataFile("imw.dat", fileName, name)); }
						break;
					} // Weapon
					case (byte) 2: {
						if (!itemModel.exists()) { modelDatas.put(itemModel, getDataFile("imt.dat", fileName, name)); }
						break;
					} // Tool
					case (byte) 3: {
						String slot = ((CustomArmor) customitem).getEquipmentSlot().getName().toLowerCase();
						if (((CustomArmor) customitem).objModel != null) {
							File objFile = new File(armorObjDir, name + ".obj");
							File mtlFile = new File(armorObjDir, name + ".mtl");
							if (!itemModel.exists() || !objFile.exists() || !mtlFile.exists()) {
								modelDatas.put(itemModel, getDataFile("imro.dat", fileName, name + "_" + slot));
								modelDatas.put(objFile, getDataFile("am_o.dat", fileName, name));
								modelDatas.put(mtlFile, getDataFile("am_m.dat", fileName, name));
							}
						}
						else {
							File ironDarkerTrimFile = new File(modelsDir, fileName + "_" + slot + "_iron_darker_trim.json");
							File quartzTrimFile = new File(modelsDir, fileName + "_" + slot + "_quartz_trim.json");
							File netheriteTrimFile = new File(modelsDir, fileName + "_" + slot + "_netherite_trim.json");
							File redstoneTrimFile = new File(modelsDir, fileName + "_" + slot + "_redstone_trim.json");
							File amethystTrimFile = new File(modelsDir, fileName + "_" + slot + "_amethyst_trim.json");
							File goldTrimFile = new File(modelsDir, fileName + "_" + slot + "_gold_trim.json");
							File lapisTrimFile = new File(modelsDir, fileName + "_" + slot + "_lapis_trim.json");
							File emeraldTrimFile = new File(modelsDir, fileName + "_" + slot + "_emerald_trim.json");
							File copperTrimFile = new File(modelsDir, fileName + "_" + slot + "_copper_trim.json");
							if (!itemModel.exists() ||
									!ironDarkerTrimFile.exists() || !quartzTrimFile.exists() ||
									!netheriteTrimFile.exists() || !redstoneTrimFile.exists() ||
									!amethystTrimFile.exists() || !goldTrimFile.exists() ||
									!lapisTrimFile.exists() || !emeraldTrimFile.exists() || !copperTrimFile.exists()) {
								modelDatas.put(itemModel, getDataFile("imr.dat", fileName, name).replace("{slot}", slot));
								String data = getDataFile("imrp.dat", fileName, null);
								modelDatas.put(ironDarkerTrimFile, data.replace("{name}", "iron_darker").replace("{slot}", slot));
								modelDatas.put(quartzTrimFile, data.replace("{name}", "quartz").replace("{slot}", slot));
								modelDatas.put(netheriteTrimFile, data.replace("{name}", "netherite").replace("{slot}", slot));
								modelDatas.put(redstoneTrimFile, data.replace("{name}", "redstone").replace("{slot}", slot));
								modelDatas.put(amethystTrimFile, data.replace("{name}", "amethyst").replace("{slot}", slot));
								modelDatas.put(goldTrimFile, data.replace("{name}", "gold").replace("{slot}", slot));
								modelDatas.put(lapisTrimFile, data.replace("{name}", "lapis").replace("{slot}", slot));
								modelDatas.put(emeraldTrimFile, data.replace("{name}", "emerald").replace("{slot}", slot));
								modelDatas.put(copperTrimFile, data.replace("{name}", "copper").replace("{slot}", slot));
							}
						}
						break;
					} // Armor
					case (byte) 4: {
						File blockingFile = new File(modelsDir, fileName + "_blocking.json");
						if (!itemModel.exists() || !blockingFile.exists()) {
							modelDatas.put(itemModel, getDataFile("imsb.dat", fileName, name));
							modelDatas.put(blockingFile, getDataFile("ims.dat", fileName, name));
						}
						break;
					} // Shield
					case (byte) 5: {
						File pulling_0_File = new File(modelsDir, fileName + "_pulling_0.json");
						File pulling_1_File = new File(modelsDir, fileName + "_pulling_1.json");
						File pulling_2_File = new File(modelsDir, fileName + "_pulling_2.json");
						if (!itemModel.exists() || !pulling_0_File.exists() ||
								!pulling_1_File.exists() || !pulling_2_File.exists()) {
							modelDatas.put(itemModel, getDataFile("imb.dat", fileName, name));
							String jsonModel = getDataFile("imbp.dat", fileName, name);
							modelDatas.put(pulling_0_File, jsonModel.replace("{num}", "0"));
							modelDatas.put(pulling_1_File, jsonModel.replace("{num}", "1"));
							modelDatas.put(pulling_2_File, jsonModel.replace("{num}", "2"));
						}
						break;
					} // Bow
					case (byte) 7: {
						break;
					} // Potion
					case (byte) 8: {
						File castFile = new File(modelsDir, fileName + "_cast.json");
						if (!itemModel.exists() || !castFile.exists()) {
							modelDatas.put(itemModel, getDataFile("imf.dat", fileName, name));
							modelDatas.put(castFile, getDataFile("imfc.dat", fileName, name));
						}
						break;
					} // Fishing Rod
					default: {
						if (!itemModel.exists()) { modelDatas.put(itemModel, getDataFile("im.dat", fileName, name)); }
						break;
					} // 0: Simple
				}
			}
			// Write
			for (Map.Entry<File, String> entry: modelDatas.entrySet()) {
				if (Util.instance.saveFile(entry.getKey(), entry.getValue())) {
					LogWriter.debug("Create Default Item Model for \"" + name + "\" item. File: " + entry.getKey().getName());
				}
			}
		}
    }

	public static void createBlockFiles(ICustomElement customblock) {
		String name = customblock.getCustomName();
		String fileName = "custom_" + name.toLowerCase();
		File blockStatesDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/blockstates");
		File blockModelsDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/models/block");
		File blockObjModelsDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/models/block/obj");
		File itemModelsDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/models/item");

		if ((blockStatesDir.exists() || blockStatesDir.mkdirs()) &&
				(blockModelsDir.exists() || blockModelsDir.mkdirs()) &&
				(itemModelsDir.exists() || itemModelsDir.mkdirs()) &&
				(blockObjModelsDir.exists() || blockObjModelsDir.mkdirs())) {
            // Standard orientable base block:
			File orientable = new File(blockModelsDir, "orientable.json");
			if (!orientable.exists() && Util.instance.saveFile(orientable, Util.instance.getDataFile("ort.dat"))) {
				LogWriter.debug("Create Orientable Block Model for \"orientable\" block");
			}
			// Standard chest base block:
			File chestFile = new File(blockModelsDir, "chest.json");
			if (!chestFile.exists() && Util.instance.saveFile(chestFile, Util.instance.getDataFile("jch.dat"))) {
				LogWriter.debug("Create Chest Block Model for \"custom chest\" block");
			}

			File blockstate = new File(blockStatesDir, fileName + ".json"); // state
			File blockModel = new File(blockModelsDir, fileName + ".json"); // block model
			File itemFile = new File(itemModelsDir, fileName + ".json"); // item model
			Map<File, String> stateDatas = new HashMap<>();
			Map<File, String> modelDatas = new HashMap<>();
			if (customblock.getCustomNbt().getBoolean("IsOBJModel")) {
				File objFile = new File(blockObjModelsDir, fileName + ".obj");
				File mtlFile = new File(blockObjModelsDir, fileName + ".mtl");
				if (!blockstate.exists() || !itemFile.exists() || !blockModel.exists() || !objFile.exists() || !mtlFile.exists()) {
					stateDatas.put(blockstate, getDataFile("jbo.dat", fileName, name));
					modelDatas.put(blockModel, getDataFile("bmo.dat", fileName, name));
					modelDatas.put(objFile, getDataFile("bmc_o.dat", fileName, name));
					modelDatas.put(mtlFile, getDataFile("bmc_m.dat", fileName, name));
					modelDatas.put(itemFile, getDataFile("bmio.dat", fileName, name));
				}
			}
			else {
				switch (customblock.getElementType()) {
					case 1: {
						blockstate = new File(blockStatesDir, fileName + ".json");
						File bucketFile = new File(itemModelsDir, fileName + "_bucket.json");
						File bottleFile = new File(itemModelsDir, "custom_bottle_" + name + ".json");
						if (!blockstate.exists() || !blockModel.exists() ||
								!bucketFile.exists() || !bottleFile.exists()) {
							stateDatas.put(blockstate, getDataFile("jlq.dat", fileName, name));
							modelDatas.put(blockModel, getDataFile("bml.dat", fileName, name));
							String fileData = getDataFile("iml.dat", fileName, name);
							modelDatas.put(bucketFile, fileData.replace("{part}", "bucket"));
							modelDatas.put(bottleFile, fileData.replace("{part}", "bottle"));
						}
						if (customblock.getCustomNbt().getBoolean("AddCauldron")) {
							File cauldronStateFile = new File(blockStatesDir, "custom_cauldron_" + name + ".json");
							File level1File = new File(blockModelsDir, "custom_cauldron_" + name + "_level1.json");
							File level2File = new File(blockModelsDir, "custom_cauldron_" + name + "_level2.json");
							File level3File = new File(blockModelsDir, "custom_cauldron_" + name + "_level3.json");
							itemFile = new File(itemModelsDir, "custom_cauldron_" + name + ".json");
							if (!cauldronStateFile.exists() || !level3File.exists() || !level1File.exists() || !level2File.exists() || !itemFile.exists()) {
								stateDatas.put(cauldronStateFile, getDataFile("jlqc.dat", fileName, name));
								modelDatas.put(level1File, getDataFile("bmlc.dat", fileName, name).replace("{level}", "9"));
								modelDatas.put(level2File, getDataFile("bmlc.dat", fileName, name).replace("{level}", "12"));
								modelDatas.put(level3File, getDataFile("bmlc.dat", fileName, name).replace("{level}", "15"));
								modelDatas.put(itemFile, getDataFile("imc.dat", fileName, name));
							}
						}
						break;
					} // Liquid
					case 2: {
						if (!blockstate.exists() || !blockModel.exists() || !itemFile.exists()) {
							boolean isChest = ((CustomChest) customblock).isChest;
							stateDatas.put(blockstate, getDataFile("jb" + (isChest ? "h" : "") + ".dat", fileName, name));
							modelDatas.put(blockModel, getDataFile("bm" + (isChest ? "h" : "") + ".dat", fileName, name));
							modelDatas.put(itemFile, getDataFile("bmi.dat", fileName, name));
						}
						break;
					} // Chest
					case 3: {
						File innerFile = new File(blockModelsDir, fileName + "_inner.json");
						File outerFile = new File(blockModelsDir, fileName + "_outer.json");
						if (!blockstate.exists() || !blockModel.exists() || !itemFile.exists() || !innerFile.exists() || !outerFile.exists()) {
							stateDatas.put(blockstate, getDataFile("jbs.dat", fileName, name));
							String data = getDataFile("bms.dat", fileName, name);
							modelDatas.put(blockModel, data.replace("{type}", ""));
							modelDatas.put(innerFile, data.replace("{type}", "inner_"));
							modelDatas.put(outerFile, data.replace("{type}", "outer_"));
							modelDatas.put(itemFile, getDataFile("bmi.dat", fileName, name));
						}
						break;
					} // Stairs
					case 4: {
						File doubleState = new File(blockStatesDir, "custom_double_" + name + ".json");
						File doubleModel = new File(blockModelsDir, "custom_double_" + name + ".json");
						File doubleTopModel = new File(blockModelsDir, "custom_double_" + name + "_top.json");
						File slabFile = new File(blockModelsDir, "bottom_" + fileName + ".json");
						File topFile = new File(blockModelsDir, "upper_" + fileName + ".json");
						if (!blockstate.exists() || !doubleState.exists() ||
								!blockModel.exists() || !doubleModel.exists() || !doubleTopModel.exists() ||
								!slabFile.exists() || !topFile.exists() ||
								!itemFile.exists()) {
							stateDatas.put(blockstate, getDataFile("jss.dat", fileName, name));
							stateDatas.put(doubleState, getDataFile("jsd.dat", fileName, name));

							modelDatas.put(doubleModel, getDataFile("bmsd.dat", fileName, name));
							modelDatas.put(doubleTopModel, getDataFile("bmsdt.dat", fileName, name));

							String data = getDataFile("bmss.dat", fileName, name);
							modelDatas.put(blockModel, getDataFile("bmfc.dat", fileName, name));
							modelDatas.put(slabFile, data.replace("{type}", "half"));
							modelDatas.put(topFile, data.replace("{type}", "upper"));
							modelDatas.put(itemFile, getDataFile("bmi.dat", fileName + "_slab", name));
						}
						break;
					} // Slab
					case 5: {
						if (!blockstate.exists() || !itemFile.exists()) {
							stateDatas.put(blockstate, getDataFile("jbp.dat", fileName, name));
							//modelDatas.put(blockModel, getDataFile("bmp.dat", fileName, name));
							modelDatas.put(itemFile, getDataFile("imp.dat", fileName, name));
						}
						break;
					} // Portal
					case 6: {
						File bottomFile = new File(blockModelsDir, fileName + "_bottom.json");
						File bottomRHFile = new File(blockModelsDir, fileName + "_bottom_rh.json");
						File topFile = new File(blockModelsDir, fileName + "_top.json");
						File topRHFile = new File(blockModelsDir, fileName + "_top_rh.json");
						if (!blockstate.exists() || !itemFile.exists() ||
								!bottomFile.exists() || !bottomRHFile.exists() ||
								!topFile.exists() || !topRHFile.exists()) {
							stateDatas.put(blockstate, getDataFile("jbd.dat", fileName, name));
							String data = getDataFile("bmd.dat", fileName, name);
							modelDatas.put(blockModel, data.replace("{type}", "door_bottom"));
							modelDatas.put(bottomFile, data.replace("{type}", "door_bottom"));
							modelDatas.put(bottomRHFile, data.replace("{type}", "door_bottom_rh"));
							modelDatas.put(topFile, data.replace("{type}", "door_top"));
							modelDatas.put(topRHFile, data.replace("{type}", "door_top_rh"));
							modelDatas.put(itemFile, getDataFile("bmid.dat", fileName, name));
						}
						break;
					} // Door
					default: {
						if (!blockstate.exists() || !blockModel.exists() || !itemFile.exists()) {
							if (customblock instanceof CustomBlock && ((CustomBlock) customblock).hasProperty()) {
								CustomBlock block = (CustomBlock) customblock;
								NBTTagCompound data = customblock.getCustomNbt().getMCNBT().getCompoundTag("Property");
								String state = getDataFile("jpr.dat", fileName, name);
								StringBuilder variants = new StringBuilder();
								if (block.BO != null) {
									variants.append("    \"").append(data.getString("Name")).append("=true\": { \"model\": \"").append(CustomNpcs.MODID).append(":").append(fileName).append("_true\" },").append((char) 10);
									variants.append("    \"").append(data.getString("Name")).append("=false\": { \"model\": \"").append(CustomNpcs.MODID).append(":").append(fileName).append("_false\" }");
									stateDatas.put(blockstate, state.replace("{type}", "Boolean").replace("{variants}", variants.toString()));
									modelDatas.put(blockModel, getDataFile("bm.dat", fileName, name));
								} // boolean
								else if (block.INT != null) {
									for (int i = data.getInteger("Min"); i <= data.getInteger("Max"); i++) {
										variants.append("    \"").append(data.getString("Name")).append("=").append(i).append("\": { \"model\": \"").append(CustomNpcs.MODID).append(":").append(fileName).append("_").append(i).append("\" }");
										if (i < data.getInteger("Max") - 1) { variants.append(",").append((char) 10); }
									}
									stateDatas.put(blockstate, state.replace("{type}", "Integer").replace("{variants}", variants.toString()));
									modelDatas.put(blockModel, getDataFile("bm.dat", fileName, name));
								} // int
								else if (block.FACING != null) {
									int i = 0;
									for (EnumFacing ef : EnumFacing.values()) {
										if (ef == EnumFacing.DOWN || ef == EnumFacing.UP) { continue; }
										variants.append("    \"").append(data.getString("Name")).append("=").append(ef.getName()).append("\": { \"model\": \"").append(CustomNpcs.MODID).append(":").append(fileName).append("\"");
										if (ef == EnumFacing.SOUTH) { variants.append(", \"y\": 180"); }
										else if (ef == EnumFacing.WEST) { variants.append(", \"y\": 270"); }
										else if (ef == EnumFacing.EAST) { variants.append(", \"y\": 90"); }
										variants.append(" }");
										if (i < 3) { variants.append(",").append((char) 10); }
										i++;
									}
									stateDatas.put(blockstate, state.replace("{type}", "Fasing").replace("{variants}", variants.toString()));
									modelDatas.put(blockModel, getDataFile("bmf.dat", fileName, name));
								} // facing
							}
							else {
								stateDatas.put(blockstate, getDataFile("jb.dat", fileName, name));
								modelDatas.put(blockModel, getDataFile("bm.dat", fileName, name));
							}
							modelDatas.put(itemFile, getDataFile("bmi.dat", fileName, name));
						}
					}
				}
			}
			// Write
			for (Map.Entry<File, String> entry : stateDatas.entrySet()) {
				if (Util.instance.saveFile(entry.getKey(), entry.getValue())) {
					LogWriter.debug("Create Default Blockstate for \"" + entry.getKey().getName() + "\" block");
				}
			}
			for (Map.Entry<File, String> entry : modelDatas.entrySet()) {
				if (Util.instance.saveFile(entry.getKey(), entry.getValue())) {
					LogWriter.debug("Create Default Block Model for \"" + entry.getKey().getName() + "\" variant");
				}
			}
		}
	}

	public static String getDataFile(String data, String fileName, String name) {
		String fileData = Util.instance.getDataFile(data).replace("{mod_id}", CustomNpcs.MODID);
		if (fileName != null && !fileName.isEmpty()) { fileData = fileData.replace("{file_name}", fileName); }
		if (name != null && !name.isEmpty()) { fileData = fileData.replace("{name}", name); }
		return fileData;
	}

    public static BlockPos getSafeTpPos(World world, BlockPos tpPos, int yMax, int yMin) {
		Function<BlockPos, Boolean> isSafeSpot = (p) -> {
			IBlockState s = world.getBlockState(p);
			BlockPos pu = p.up();
			IBlockState su = world.getBlockState(pu);
			return (world.isAirBlock(p) && world.isAirBlock(pu)) || (s.getMaterial().isOpaque() && su.getMaterial().isOpaque());
		};

		BlockPos pos = new BlockPos(tpPos.getX(), tpPos.getY(), tpPos.getZ());
		while (pos.getY() <= yMax && !isSafeSpot.apply(pos)) { pos = pos.up(); }
		if (isSafeSpot.apply(pos)) { return pos; }

		pos = new BlockPos(tpPos.getX(), tpPos.getY(), tpPos.getZ());
		while (pos.getY() > yMin && !isSafeSpot.apply(pos)) { pos = pos.down(); }
		if (isSafeSpot.apply(pos)) { return pos; }

		return tpPos;
    }

	public static String validLocation(String location) {
		if (location.contains(":")) {
			String domain = validNamespace(location.substring(0, location.indexOf(":")));
			String path = validPath(location.substring(location.indexOf(":") + 1));
			location = domain + ":" + path;
		}
		else { location = validPath(location.substring(location.indexOf(":") + 1)); }
		return location;
	}

	public static String validNamespace(String path) {
		StringBuilder valid = new StringBuilder();
		boolean isChange = false;
		for (char ch : path.toCharArray()) {
			if (ch == '_' || ch == '-' || ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9' || ch == '.') { valid.append(ch); }
			else {
				if (Character.isUpperCase(ch)) {
					char lowerCh = Character.toLowerCase(ch);
					if (lowerCh >= 'a' && lowerCh <= 'z') { valid.append(lowerCh); }
					else { valid.append('_'); }
				}
				else { valid.append('_'); }
				isChange = true;
			}
		}
		while (valid.length() < 2) {
			valid.append("_");
			isChange = true;
		}
		if (isChange) { return valid.toString(); }
		return path;
	}

	public static String validPath(String path) {
		StringBuilder valid = new StringBuilder();
		boolean isChange = false;
		for (char ch : path.toCharArray()) {
			if (ch == '_' || ch == '-' || ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9' || ch == '/' || ch == '.') { valid.append(ch); }
			else {
				if (Character.isUpperCase(ch)) {
					char lowerCh = Character.toLowerCase(ch);
					if (lowerCh >= 'a' && lowerCh <= 'z') { valid.append(lowerCh); }
					else { valid.append('_'); }
				}
				else { valid.append('_'); }
				isChange = true;
			}
		}
		while (valid.length() < 2) {
			valid.append("_");
			isChange = true;
		}
		if (isChange) { return valid.toString(); }
		return path;
	}

	public static void sendExtraData(EntityPlayerMP player, EntityNPCInterface npc, EnumGuiType gui) {
		if (npc != null && npc.role.getEnumType() != RoleType.NONE &&
				(gui == EnumGuiType.PlayerFollower ||
						gui == EnumGuiType.PlayerFollowerHire ||
						gui == EnumGuiType.PlayerTrader ||
						gui == EnumGuiType.PlayerTransporter)) {
			NBTTagCompound comp = new NBTTagCompound();
			npc.role.save(comp);
			Packets.send(player, new PacketNpcRole(npc.getEntityId(), comp));
		}
	}

	public static void sendNpcDialogs(EntityPlayerMP player) {
		EntityNPCInterface npc = getEditingNpc(player);
		if (npc != null) {
			int slot = 0;
			for (int dialogId : npc.dialogs) {
				if (!DialogController.instance.hasDialog(dialogId)) { continue; }
				Dialog d = DialogController.instance.get(dialogId);
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("Id", d.id);
				compound.setInteger("Slot", slot);
				compound.setString("Category", d.category.title);
				compound.setString("Title", d.title);
				Packets.send(player, new PacketGuiData(compound));
				slot++;
			}
		}
	}

}
