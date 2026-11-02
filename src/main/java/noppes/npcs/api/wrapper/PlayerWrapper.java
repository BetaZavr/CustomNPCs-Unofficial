package noppes.npcs.api.wrapper;

import java.lang.reflect.Method;
import java.util.*;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldSettings;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.EventHooks;
import noppes.npcs.api.entity.data.IPlayerMiniMap;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.data.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.*;
import noppes.npcs.packets.server.SPacketCustomNBT;
import noppes.npcs.packets.server.SPacketDimensionTeleport;
import noppes.npcs.packets.server.SPacketGuiOpen;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.*;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.IPixelmonPlayerData;
import noppes.npcs.api.entity.data.IPlayerMail;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.overlay.IOverlay;
import noppes.npcs.api.wrapper.gui.CustomGuiWrapper;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerCustomGui;
import noppes.npcs.controllers.*;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.util.Util;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.ValueUtil;

public class PlayerWrapper<T extends EntityPlayer> extends EntityLivingBaseWrapper<T> implements IPlayer<T> {

	private PlayerData data;
	private IContainer inventory;
	private Object pixelmonPartyStorage;

	private Object pixelmonPCStorage;

	public PlayerWrapper(T player) { super(player); }

	@Override
	public String getName() { return entity.getName(); }

	@Override
	public String getDisplayName() { return entity.getDisplayName().getFormattedText(); }

	@Override
	public int getHunger() { return entity.getFoodStats().getFoodLevel(); }

	@Override
	public void setHunger(int level) { entity.getFoodStats().setFoodLevel(level); }

	@Override
	public boolean hasFinishedQuest(int id) { return getData().questData.hasFinishedQuest(id); }

	@Override
	public boolean hasActiveQuest(int id) {
		PlayerQuestData data = getData().questData;
		return data.activeQuests.containsKey(id);
	}

	@Override
	public IQuest[] getActiveQuests() {
		PlayerQuestData data = getData().questData;
		List<IQuest> quests = new ArrayList<>();
		for (int id : data.activeQuests.keySet()) {
			IQuest quest = QuestController.instance.quests.get(id);
			if (quest != null) {
				quests.add(quest);
			}
		}
		return quests.toArray(new IQuest[0]);
	}

	@Override
	public IQuest[] getFinishedQuests() {
		PlayerQuestData data = getData().questData;
		List<IQuest> quests = new ArrayList<>();
		for (int id : data.getFinishedQuest()) {
			IQuest quest = QuestController.instance.quests.get(id);
			if (quest != null) { quests.add(quest); }
		}
		return quests.toArray(new IQuest[0]);
	}

	@Override
	public void startQuest(int id) {
		Quest quest = QuestController.instance.quests.get(id);

		if (quest == null) { return; }
		PlayerQuestController.addActiveQuest(quest, entity, true);
	}

	@Override
	public void sendNotification(String title, String msg, int type) {
		if (type < 0 || type > 3) { throw new CustomNPCsException("Wrong type value given " + type); }
		if (entity instanceof EntityPlayerMP) {
			Packets.send((EntityPlayerMP) entity, new PacketAchievement(Component.translatable(title), Component.translatable(msg), type, new NBTTagCompound()));
		}
	}

	@Override
	public void finishQuest(int id) {
		Quest quest = QuestController.instance.quests.get(id);
		if (quest != null && entity instanceof EntityPlayerMP && data.questData.finish(quest, entity)) {
			Packets.send((EntityPlayerMP) entity, new PacketAchievement(Component.translatable("quest.completed"), Component.translatable(quest.title), 2, new NBTTagCompound()));
			Packets.send((EntityPlayerMP) entity, new PacketChat(Component.translatable("quest.completed").append(": ").append(Component.translatable(quest.title))));
		}
	}

	@Override
	public void stopQuest(int id) {
		Quest quest = QuestController.instance.quests.get(id);
		if (quest != null) {
			PlayerData data = getData();
			data.questData.activeQuests.remove(id);
			data.updateClient = true;
		}
	}

	@Override
	public void removeQuest(int id) {
		Quest quest = QuestController.instance.quests.get(id);
		if (quest != null) {
			PlayerData data = getData();
			data.questData.activeQuests.remove(id);
			data.questData.removeFinishedQuest(id);
			data.updateClient = true;
		}
	}

	@Override
	public boolean hasReadDialog(int id) {
		PlayerDialogData data = getData().dialogData;
		return data.has(id);
	}

	@Override
	public void showDialog(int id, String name) {
		Dialog dialog = DialogController.instance.dialogs.get(id);
		if (dialog == null) {
			throw new CustomNPCsException("Unknown Dialog id: " + id);
		}
		if (!dialog.availability.isAvailable(entity)) { return; }
		EntityDialogNpc npc = new EntityDialogNpc(getWorld().getMCWorld());
		npc.display.setName(name);
		EntityUtil.Copy(entity, npc);
		npc.dialogs = new int[] { id };
		NoppesUtilServer.openDialog(entity, npc, dialog);
	}

	@Override
	public void addFactionPoints(int faction, int points) {
		PlayerData data = getData();
		data.factionData.increasePoints(entity, faction, points);
		data.updateClient = true;
	}

	@Override
	public int getFactionPoints(int faction) { return getData().factionData.getFactionPoints(entity, faction); }

	@Override
	public float getRotation() { return entity.rotationYaw; }

	@Override
	public void setRotation(float rotation) { entity.rotationYaw = rotation; }

	@Override
	public void message(String message) { entity.sendMessage(Component.translatable(NoppesStringUtils.formatText(message, entity)).getParent()); }

	@Override
	public int getGamemode() {
		if (entity instanceof EntityPlayerMP) {
			return ((EntityPlayerMP) entity).interactionManager.getGameType().getID();
		}
		if (entity.isCreative()) { return 1; }
		if (entity.isSpectator()) { return 3; }
		return 0;
	}

	@Override
	public void setGamemode(int type) {
		if (entity instanceof EntityPlayerMP) { entity.setGameType(WorldSettings.getGameTypeById(type)); }
	}

	@Override
	public int inventoryItemCount(IItemStack item) { return inventoryItemCount(item.getMCItemStack()); }

	@Override
	public int inventoryItemCount(String id) {
		Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
		if (item == null) { throw new CustomNPCsException("Unknown item id: " + id); }
		return inventoryItemCount(new ItemStack(item, 1));
	}

	@Override
	public IContainer getInventory() {
		if (inventory == null) { inventory = new ContainerWrapper(entity.inventory); }
		return inventory;
	}

	@Override
	public IItemStack getInventoryHeldItem() {
		return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(entity.inventory.getItemStack());
	}

	@Override
	public boolean removeItem(IItemStack item, int amount) {
		int count = inventoryItemCount(item.getMCItemStack());
		if (amount > count) { return false; }
		if (count == amount) { removeAllItems(item); }
		else {
			for (int i = 0; i < entity.inventory.getSizeInventory(); ++i) {
				ItemStack is = entity.inventory.getStackInSlot(i);
				if (isItemEqual(item.getMCItemStack(), is)) {
					if (amount < is.getCount()) {
						is.splitStack(amount);
						break;
					}
					entity.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
					amount -= is.getCount();
				}
			}
		}
		updatePlayerInventory();
		return true;
	}

	@Override
	public boolean removeItem(String id, int amount) { return removeItem(id, amount, 0); }

	@Override
	public boolean removeItem(String id, int damage, int amount) {
		Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
		if (item == null) { throw new CustomNPCsException("Unknown item id: " + id); }
		return removeItem(Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(new ItemStack(item, 1, damage)), amount);
	}

	@Override
	public boolean giveItem(IItemStack item) {
		ItemStack mcItem = item.getMCItemStack();
		if (mcItem.isEmpty()) { return false; }
		boolean bo = entity.inventory.addItemStackToInventory(mcItem.copy());
		if (bo) {
			NoppesUtilServer.playSound(entity, SoundEvents.ENTITY_ITEM_PICKUP, 0.2f,
					((entity.getRNG().nextFloat() - entity.getRNG().nextFloat()) * 0.7f + 1.0f) * 2.0f);
			updatePlayerInventory();
		}
		return bo;
	}

	@Override
	public boolean giveItem(String id, int amount) { return giveItem(id, amount, 0); }

	@Override
	public boolean giveItem(String id, int damage, int amount) {
		Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
		if (item != null) {
			ItemStack mcStack = new ItemStack(item);
			IItemStack itemStack = Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(mcStack);
			itemStack.setStackSize(amount);
			itemStack.setItemDamage(damage);
			return giveItem(itemStack);
		}
		return false;
	}

	@Override
	public void updatePlayerInventory() {
		entity.inventoryContainer.detectAndSendChanges();
		if (entity instanceof EntityPlayerMP) {
			PlayerQuestData questData = getData().questData;
			CustomNPCsScheduler.runTack(() -> {
				for (QuestData data : questData.activeQuests.values()) {
					for (QuestObjective obj : data.quest.getObjectives(entity)) {
						if (obj.getEnumType() != EnumQuestTask.ITEM) { continue; }
						questData.checkQuestCompletion(entity, data);
					}
				}
			});
		}
	}

	@Override
	public IBlock getSpawnPoint() {
		return Objects.requireNonNull(NpcAPI.Instance()).getIBlock(entity.world, entity.getBedLocation());
	}

	@Override
	public void setSpawnPoint(IBlock block) {
		entity.setSpawnPoint(block.getPos().getMCBlockPos(), false);
	}

	@Override
	public void setSpawnpoint(int x, int y, int z) {
		if (entity instanceof EntityPlayerMP) {
			x = ValueUtil.correctInt(x, -30000000, 30000000);
			z = ValueUtil.correctInt(z, -30000000, 30000000);
			y = ValueUtil.correctInt(y, 0, 256);
			entity.setSpawnPoint(new BlockPos(ValueUtil.correctInt(x, -30000000, 30000000),
					ValueUtil.correctInt(y, 0, 256),
					ValueUtil.correctInt(z, -30000000, 30000000)), false);
		}
	}

	@Override
	public void resetSpawnpoint() {
		entity.setSpawnPoint(entity.world.getSpawnPoint(), false);
	}

	@Override
	public void removeAllItems(IItemStack item) {
		for (int i = 0; i < entity.inventory.getSizeInventory(); ++i) {
			ItemStack is = entity.inventory.getStackInSlot(i);
			if (is.isItemEqual(item.getMCItemStack())) {
				entity.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
			}
		}
	}

	@Override
	public boolean hasAchievement(String achievement) {
		StatBase statbase = StatList.getOneShotStat(achievement);
		return statbase != null && statbase.isIndependent;
	}

	@SuppressWarnings("unused")
	public boolean hasAdvancement(String achievement) { return hasAchievement(achievement); }

	@Override
	public int getExpLevel() { return entity.experienceLevel; }

	@Override
	public void setExpLevel(int level) {
		entity.experienceLevel = level;
		entity.addExperienceLevel(0);
	}

	@Override
	public void setPosition(double x, double y, double z) {
		if (entity instanceof EntityPlayerMP) {
			SPacketDimensionTeleport.teleportPlayer((EntityPlayerMP) entity, entity.dimension, x, y, z,
					entity.rotationYaw, entity.rotationPitch);
		}
	}

	@Override
	public void setPos(IPos pos) { setPosition(pos.getX(), pos.getY(), pos.getZ()); }

	@Override
	public int getType() { return 1; }

	@Override
	public boolean typeOf(int type) {
		return type == 1 || super.typeOf(type);
	}

	@Override
	public boolean hasPermission(String permission) {
		if (entity instanceof EntityPlayerMP) { return CustomNpcsPermissions.hasPermission((EntityPlayerMP) entity, permission); }
		return false;
	}

	@Override
	public IPixelmonPlayerData getPixelmonData() {
		if (!PixelmonHelper.Enabled) { throw new CustomNPCsException("Pixelmon not installed"); }
		return new IPixelmonPlayerData() {
			@Override
			public Object getParty() {
				if (pixelmonPartyStorage == null) { pixelmonPartyStorage = PixelmonHelper .getParty((EntityPlayerMP) entity); }
				return pixelmonPartyStorage;
			}
			@Override
			public Object getPC() {
				if (pixelmonPCStorage == null) { pixelmonPCStorage = PixelmonHelper.getPc((EntityPlayerMP) entity); }
				return pixelmonPCStorage;
			}
		};
	}

	@Override
	public ITimers getTimers() { return getData().timers; }

	@Override
	public void removeDialog(int id) {
		PlayerData data = getData();
		data.dialogData.dialogsRead.remove(id);
		data.updateClient = true;
	}

	@Override
	public void addDialog(int id) {
		PlayerData data = getData();
		data.dialogData.read(id);
		data.updateClient = true;
	}

	@Override
	public void closeGui() {
		if (entity instanceof EntityPlayerMP) {
			((EntityPlayerMP) entity).closeContainer();
			Packets.send((EntityPlayerMP) entity, new PacketGuiClose(new NBTTagCompound()));
		}
	}

	@Override
	public int factionStatus(int factionId) {
		Faction faction = FactionController.instance.getFaction(factionId);
		if (faction == null) { throw new CustomNPCsException("Unknown faction: " + factionId); }
		return faction.playerStatus(this);
	}

	@Override
	public void kick(String message) {
		if (entity instanceof EntityPlayerMP) {
			((EntityPlayerMP) entity).connection.disconnect(Component.translatable(message).getParent());
		}
	}

	@Override
	public boolean canQuestBeAccepted(int questId) { return PlayerQuestController.canQuestBeAccepted(entity, questId); }

	@Override
	public void showCustomGui(ICustomGui gui) {
		if (entity instanceof EntityPlayerMP && gui != null) {
			NoppesUtilServer.openContainerGui((EntityPlayerMP) entity, EnumGuiType.CustomGui, (buf) -> buf.writeNbt(((CustomGuiWrapper) gui).toNBT()));
			((ContainerCustomGui) entity.openContainer).setGui((CustomGuiWrapper)gui, entity);
		}
	}

	@Override
	public ICustomGui getCustomGui() {
		return entity.openContainer instanceof ContainerCustomGui ? ((ContainerCustomGui) entity.openContainer).customGui : null;
	}

	@Override
	public void clearData() {
		PlayerData data = getData();
		data.setNBT(new NBTTagCompound());
		data.save(true);
	}

	@Override
	public IContainer getOpenContainer() {
		return Objects.requireNonNull(NpcAPI.Instance()).getIContainer(entity.openContainer);
	}

	@Override
	public void playSound(String sound, float volume, float pitch) {
		if (sound != null && !sound.isEmpty()) {
			if (entity instanceof EntityPlayerMP) {
				Packets.send((EntityPlayerMP) entity, new PacketPlaySound(sound, SoundCategory.PLAYERS,
						entity.posX, entity.posY, entity.posZ, volume, pitch));
			}
			else {
				CustomNpcs.proxy.playSound(SoundCategory.PLAYERS, sound, entity.posX, entity.posY, entity.posZ, volume, pitch, false, false);
			}
		}
	}

	@Override
	public void playMusic(String sound, boolean background, boolean loops) {
		if (sound != null && !sound.isEmpty()) {
			if (entity instanceof EntityPlayerMP) {
				Packets.send((EntityPlayerMP) entity, new PacketPlayMusic(new ResourceLocation(NoppesUtilServer.validLocation(sound)), !background, loops));
			}
			else {
				CustomNpcs.proxy.playSound(SoundCategory.MUSIC, sound, entity.posX, entity.posY, entity.posZ, 1.0f, 1.0f, background, loops);
			}
		}
	}

	@Override
	public void sendMail(IPlayerMail mail) {
		PlayerDataController.instance.addPlayerMessage(entity.getServer(), entity.getName(), (PlayerMail) mail);
	}

	@Override
	public void trigger(int id, Object... arguments) {
		EventHooks.onScriptTriggerEvent(EventHooks.getPlayerHandler(), id, getWorld(), getPos(), null, arguments);
	}

	private int inventoryItemCount(ItemStack stack) {
		int count = 0;
		for (int i = 0; i < entity.inventory.getSizeInventory(); ++i) {
			ItemStack is = entity.inventory.getStackInSlot(i);
			if (isItemEqual(stack, is)) { count += is.getCount(); }
		}
		return count;
	}

	private boolean isItemEqual(ItemStack stack, ItemStack other) {
		return !other.isEmpty() && stack.getItem() == other.getItem();
	}

	private PlayerData getData() {
		if (data == null) { data = PlayerData.get(entity); }
		return data;
	}

	// New from Unofficial (Goodbird)
	@Override
	public void showOverlay(IOverlay overlay) {
		if (overlay != null) {
			data.overlay.add(overlay.getId());
			if (entity instanceof EntityPlayerMP) {
				Packets.send((EntityPlayerMP) entity, new PacketOverlayShow(overlay.save().getMCNBT()));
			}
		}
	}

	@Override
	public void showSoundSelectionGUI() {
		if (entity instanceof EntityPlayerMP) { Packets.send((EntityPlayerMP) entity, new PacketSoundGUIOpen()); }
	}

	@Override
	public void hideOverlay(int id) {
		data.overlay.remove(id);
		if (entity instanceof EntityPlayerMP) { Packets.send((EntityPlayerMP) entity, new PacketOverlayHide(id)); }
	}

	@Override
	public void hideAllOverlays() {
		data.overlay.clearOverlays();
		if (entity instanceof EntityPlayerMP) { Packets.send((EntityPlayerMP) entity, new PacketHideAllOverlays()); }
	}

	@Override
	public IPlayerSkin getSkin() { return PlayerSkinController.getInstance().getData(entity.getUniqueID(), 0); }

	@Override
	public IPlayerSkin getSkin(int type) { return PlayerSkinController.getInstance().getData(entity.getUniqueID(), type); }

	@Override
	public void addMoney(long value) {
		getData().game.addMoney(value);
	}

	@Override
	public void cameraShakingPlay(int time, int amplitude, int type, boolean isFading) {
		if (time <= 1 || time > 1200) {
			throw new CustomNPCsException("Camera shake time should be between 1 and 1200 ticks. You have: " + time);
		}
		if (amplitude <= 1 || amplitude > 25) {
			throw new CustomNPCsException("Amplitude should be between 1 and 25 value. You have: " + amplitude);
		}
		if (type < 0 || type > 5) {
			throw new CustomNPCsException("Type should be between 0 and 5 value. You have: " + type);
		}
		//Server.sendData((EntityPlayerMP) entity, EnumPacketClient.PLAY_CAMERA_SHAKING, time, amplitude, type, isFading);
	}

	@Override
	public void cameraShakingStop() {
		//Server.sendData((EntityPlayerMP) entity, EnumPacketClient.STOP_CAMERA_SHAKING);
	}

	@Override
	public void completeQuest(int id) {
		Quest quest = QuestController.instance.quests.get(id);
		if (quest == null) { return; }
		PlayerData data = getData();
		data.questData.finish(quest, entity);
		data.questData.activeQuests.remove(id);
		if (entity instanceof EntityPlayerMP) {
			Packets.send((EntityPlayerMP) entity, new PacketAchievement(Component.translatable("quest.completed"), Component.translatable(quest.title), 2, new NBTTagCompound()));
			Packets.send((EntityPlayerMP) entity, new PacketChat(Component.translatable("quest.completed").append(": ").append(Component.translatable(quest.title))));
			QuestData qData = new QuestData(quest);
			if (data.questData.activeQuests.containsKey(id)) { qData = data.questData.activeQuests.get(id); }
			quest.complete(entity, qData);
		}
	}

	@Override
	public IContainer getBubblesInventory() {
		IContainer invBubbles = null;
		try {
			Class<?> apiBubbles = Class.forName("baubles.api.BaublesApi");
			for (Method m : apiBubbles.getDeclaredMethods()) {
				if (m.getName().equals("getBaubles")) {
					if (!m.isAccessible()) { m.setAccessible(true); }
					invBubbles = new ContainerWrapper((IInventory) m.invoke(apiBubbles, entity));
					break;
				}
			}
		} catch (Exception e) {
			throw new CustomNPCsException("Mod \"Bubbles\" - not found");
		}
		return invBubbles;
	}

	@Override
	public int[] getKeyPressed() {
		return getData().overlay.getKeyPressed();
	}

	@Override
	public String getLanguage() {
		return CustomNpcs.proxy.getLanguage(entity);
	}

	@Override
	public long getMoney() {
		return getData().game.getMoney();
	}

	@Override
	public int[] getMousePressed() {
		return getData().overlay.getMousePressed();
	}

	@Override
	public String getSkinType(int type) { return PlayerSkinController.getInstance().get(entity, type); }

	@Override
	public IScreenSize getWindowSize() {
		return data.overlay.getWindowSize();
	}

	@Override
	public boolean hasMousePress(int key) {
		return getData().overlay.hasMousePress(key);
	}

	@Override
	public boolean hasOrKeyPressed(int[] key) {
		return getData().overlay.hasOrKeysPressed(key);
	}

	@Override
	public int inventoryItemCount(IItemStack stack, boolean ignoreDamage, boolean ignoreNBT) {
		return Util.instance.inventoryItemCount(entity, stack.getMCItemStack(), null, ignoreDamage, ignoreNBT);
	}

	@Override
	public int inventoryItemCount(String id, int amount) {
		Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
		if (item == null) { throw new CustomNPCsException("Unknown item id: " + id); }
		return inventoryItemCount(new ItemStack(item, 1));
	}

	@Override
	public boolean isCompleteQuest(int id) {
		PlayerQuestData data = getData().questData;
		if (data.hasFinishedQuest(id)) { return true; }
		if (!data.activeQuests.containsKey(id)) { return false; }
		QuestData qData = data.activeQuests.get(id);
		if (qData.isCompleted) { return true; }
		Quest quest = (Quest) Objects.requireNonNull(NpcAPI.Instance()).getQuests().get(id);
		return quest.questInterface.isCompleted(getMCEntity());
	}

	@Override
	public boolean isMoved() { return getData().overlay.isMoved; }

	@Override
	public void playSound(int category, IPos pos, String sound, float volume, float pitch) {
		if (sound != null && !sound.isEmpty()) {
			if (entity instanceof EntityPlayerMP) {
				Packets.send((EntityPlayerMP) entity, new PacketPlaySound(sound, SoundCategory.PLAYERS, pos.getX(), pos.getY(), pos.getZ(), volume, pitch));
			}
			else {
				CustomNpcs.proxy.playSound(SoundCategory.PLAYERS, sound, pos.getX(), pos.getY(), pos.getZ(), volume, pitch, false, false);
			}
		}
	}

	@Override
	public void sendTo(INbt nbt) {
		CustomNPCsScheduler.runTack(() -> {
			if (entity instanceof EntityPlayerMP) { Packets.send((EntityPlayerMP) entity, new PacketCustomNBT(nbt.getMCNBT())); }
			else { Packets.sendServer(new SPacketCustomNBT(nbt.getMCNBT())); }
		}, 10);
	}

	@Override
	public void setMoney(long value) { getData().game.setMoney(value); }

	@Override
	public void setSkin(int type, int gender, int body, int bodyColor, int hair, int hairColor, int face, int eyesColor, int leg, int jacket, int shoes, int... peculiarities) {
		PlayerSkinController.getInstance().set(entity.getUniqueID().toString(), type, gender, body, bodyColor, hair, hairColor, face, eyesColor, leg, jacket, shoes, peculiarities);
	}

	@Override
	public void setSkinType(int type, String location) {
		PlayerSkinController.getInstance().set(entity.getUniqueID().toString(), location, type);
	}

	@Override
	public void stopSound(int category, String sound) {
		if (entity instanceof EntityPlayerMP) {
			Packets.send((EntityPlayerMP) entity, new PacketStopSound(new ResourceLocation(sound == null ? "" : sound), category < 0 ? -1 : category));
		}
		else { CustomNpcs.proxy.stopSound(category, sound); }
	}

	@Override
	public IEntity<?> getRidingEntity() {
		if (entity.getRidingEntity() == null) { return null; }
		return Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity.getRidingEntity());
	}

	@Override
	public IEntity<?> getLookingEntity() {
		Entity target = Util.instance.getLookEntity(entity, null, true);
		return target == null ? null : Objects.requireNonNull(NpcAPI.Instance()).getIEntity(target);
	}

	@Override
	public IBlock getLookingBlock() {
		IRayTrace rt = rayTraceBlock(data.game.blockReachDistance, false, false);
		if (rt.getBlock() == null) { return null; }
		return rt.getBlock();
	}

	@Override
	public double getBlockReachDistance() { return data.game.blockReachDistance; }

	@Override
	public void showMarket(int marcetId) {
		if (entity instanceof EntityPlayerMP) {
			Marcet market = MarcetController.getInstance().getMarcet(marcetId);
			if (market != null) { NoppesUtilServer.openContainerGui((EntityPlayerMP) entity, EnumGuiType.PlayerTrader, (buffer) -> buffer.writeInt(marcetId)); }
		}
	}

	@Override
	public void showAuction(int auctionId) {
		if (entity instanceof EntityPlayerMP) {
			AuctionHouse auction = AuctionController.getInstance().getAuction(auctionId);
			if (auction != null) {
				auction.addPlayer((EntityPlayerMP) entity);
				SPacketGuiOpen.sendOpenGui((EntityPlayerMP) entity, EnumGuiType.PlayerAuction,
						null, new BlockPos(auctionId, 0, 0));
			}
		}
	}

	@Override
	public IScreenSize getScreenSize() {
		PlayerData data = PlayerData.get(entity);
		return data.overlay.screenSize;
	}

	@Override
	public void showBank(int bankId) {
		if (entity instanceof EntityPlayerMP) {
			Bank bank = BankController.getInstance().getBank(bankId);
			if (bank != null) {
				Packets.send((EntityPlayerMP) entity, new PacketBankClearPos());
				data.bankData.get(bankId).openToPlayer((EntityPlayerMP) entity, 0, 0, 0, 1);
			}
		}
	}

	@Override
	public IPlayerMiniMap getMiniMapData() { return data.minimap; }

	@Deprecated
	@Override
	public IContainer showChestGui(int rows) {
		ScriptContainer current = ScriptContainer.Current;
		entity.closeScreen();
		entity.openGui(CustomNpcs.instance, EnumGuiType.CustomChest.ordinal(), entity.world, rows, 0, 0);
		ContainerCustomChestWrapper container = (ContainerCustomChestWrapper) Objects.requireNonNull(NpcAPI.Instance()).getIContainer(entity.openContainer);
		container.script = current;
		return container;
	}

}
