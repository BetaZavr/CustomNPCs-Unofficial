package noppes.npcs.controllers.data;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.wrapper.PlayerWrapper;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.handler.ICustomPlayerData;
import noppes.npcs.api.handler.capability.IPlayerDataHandler;
import noppes.npcs.api.mixin.entity.IEntityIMixin;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAnimation;
import noppes.npcs.entity.data.DataTimers;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.NBTJsonUtil;
import noppes.npcs.util.TempFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerData implements IPlayerDataHandler, ICapabilityProvider, ICustomPlayerData {

	@CapabilityInject(IPlayerDataHandler.class)
	public static Capability<IPlayerDataHandler> PLAYERDATA_CAPABILITY;

	protected static final ResourceLocation key = new ResourceLocation(CustomNpcs.MODID, "playerdata");

	protected EntityNPCInterface activeCompanion = null;
	public EntityNPCInterface editingNpc;
	public int companionID = 0;
	public int dialogId = -1;

	public final PlayerBankData bankData = new PlayerBankData(this);
	public final PlayerDialogData dialogData = new PlayerDialogData();
	public final PlayerFactionData factionData = new PlayerFactionData();
	public final PlayerItemGiverData itemgiverData = new PlayerItemGiverData();
	public final PlayerMailData mailData = new PlayerMailData();
	public final PlayerQuestData questData = new PlayerQuestData();
	public final PlayerTransportData transportData = new PlayerTransportData();

	public EntityPlayer player;
	public String name = "";
	public String uuid = "";
	public int playerLevel = 0;
	public ItemStack prevHeldItem = ItemStack.EMPTY;
	public DataTimers timers = new DataTimers(this);
	public boolean updateClient = false; // send to -> ServerTickHandler.cnpcPlayerTick() 112

	// New data from Unofficial (Goodbird)
	public BlockPos scriptBlockPos = BlockPos.ORIGIN;
	public Entity mounted;

	// New data from Unofficial (BetaZavr)
	protected IPlayer<?> playerAPI;
	public NBTTagCompound cloned;
	public DataAnimation animation;
	public final PlayerGameData game = new PlayerGameData();
	public final PlayerCompassData compass = new PlayerCompassData();
	public final PlayerMiniMapData minimap = new PlayerMiniMapData();
	public final PlayerOverlayData overlay = new PlayerOverlayData();
	public final Map<String, TempFile> clientScriptFiles = new HashMap<>();

	@Override
	public void setNBT(NBTTagCompound compound) {
		if (player != null) {
			name = player.getName();
			uuid = player.getPersistentID().toString();
		} else {
			name = compound.getString("PlayerName");
			uuid = compound.getString("UUID");
		}
		dialogData.load(compound);
		bankData.load(compound);
		questData.load(compound);
		transportData.loadNBTData(compound);
		factionData.load(compound);
		itemgiverData.load(compound);
		mailData.load(compound);
		timers.readFromNBT(compound);
		game.load(compound);
		compass.load(compound);
		minimap.load(compound);
		overlay.load(compound);

		companionID = compound.getInteger("PlayerCompanionId");
		if (compound.hasKey("PlayerCompanion") && !hasCompanion()) {
			EntityCustomNpc npc = new EntityCustomNpc(player.world);
			npc.readEntityFromNBT(compound.getCompoundTag("PlayerCompanion"));
			npc.setPosition(player.posX, player.posY, player.posZ);
			if (npc.role instanceof RoleCompanion) {
				setCompanion(npc);
				((RoleCompanion) npc.role).setSitting(false);
				player.world.spawnEntity(npc);
			}
		}
		if (player != null) { ((IEntityIMixin) player).npcs$getStoredData().setNbt(compound.getCompoundTag("ScriptStoreddata")); }
	}

	public NBTTagCompound getSyncNBT() { // Only Display Datas
		NBTTagCompound compound = new NBTTagCompound();
		dialogData.save(compound);
		questData.save(compound);
		factionData.save(compound);
		return compound;
	}

	@Override
	public NBTTagCompound getNBT() {
		CustomNpcs.debugData.start(this);
		if (player != null) {
			name = player.getName();
			uuid = player.getPersistentID().toString();
		}
		NBTTagCompound compound = new NBTTagCompound();
		dialogData.save(compound);
		questData.save(compound);
		transportData.saveNBTData(compound);
		factionData.save(compound);
		itemgiverData.save(compound);
		mailData.save(compound);
		timers.writeToNBT(compound);

		game.save(compound);
		compass.save(compound);
		minimap.save(compound);
		overlay.save(compound);

		if (animation != null) { animation.save(compound); }
		compound.setInteger("PlayerCompanionId", companionID);
		if (name != null && !name.isEmpty()) { compound.setString("PlayerName", name); }
		if (uuid != null && !uuid.isEmpty()) { compound.setString("UUID", uuid); }
		if (player != null) { compound.setTag("ScriptStoreddata", ((IEntityIMixin) player).npcs$getStoredData().getNbt().getMCNBT()); }
		if (hasCompanion()) {
			NBTTagCompound nbt = new NBTTagCompound();
			if (activeCompanion.writeToNBTAtomically(nbt)) { compound.setTag("PlayerCompanion", nbt); }
		}
		CustomNpcs.debugData.end(this);
		return compound;
	}

	public boolean hasCompanion() { return activeCompanion != null && !activeCompanion.isDead; }

	public void setCompanion(EntityNPCInterface npc) {
		if (npc == null || !(npc.role instanceof RoleCompanion)) { return; }
		++companionID;
		activeCompanion = npc;
		((RoleCompanion) npc.role).companionID = companionID;
		save(false);
	}

	public void updateCompanion(World world) {
		if (!hasCompanion() || world == activeCompanion.world) { return; }
		RoleCompanion role = (RoleCompanion) activeCompanion.role;
		role.owner = player;
		if (!role.isFollowing()) { return; }
		NBTTagCompound nbt = new NBTTagCompound();
		activeCompanion.writeToNBTAtomically(nbt);
		activeCompanion.isDead = true;
		EntityCustomNpc npc = new EntityCustomNpc(world);
		npc.readEntityFromNBT(nbt);
		npc.setPosition(player.posX, player.posY, player.posZ);
		setCompanion(npc);
		((RoleCompanion) npc.role).setSitting(false);
		world.spawnEntity(npc);
	}

	public boolean hasCapability(@Nullable Capability<?> capability, EnumFacing facing) {
		return capability != null && capability == PLAYERDATA_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing facing) {
		if (hasCapability(capability, facing)) { return (T) this; }
		return null;
	}

	public static void register(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof EntityPlayer) { event.addCapability(PlayerData.key, new PlayerData()); }
	}

	public synchronized void save(boolean update) {
		CustomNPCsScheduler.runTack(() -> {
			try {
				if (uuid.isEmpty()) { uuid = "noplayeruuid"; }
				if (name.isEmpty()) { name = "noplayername"; }
				File saveDir = CustomNpcs.getWorldSaveDirectory("playerdata/" + uuid);
				if (saveDir != null && (saveDir.exists() || saveDir.mkdirs())) {
					File file = new File(saveDir, name + ".json_new");
					File file1 = new File(saveDir, name + ".json");
					NBTJsonUtil.SaveFile(file, getNBT());
					if (file1.exists() && !file1.delete()) { LogWriter.warn("Error delete file: " + file1); }
					if (!file.renameTo(file1)) { LogWriter.warn("Error rename file: " + file + " to: " + file1); }
				}
				else {
					LogWriter.warn("Error not exists playerdata directory:" + saveDir);
				}
			}
			catch (Exception e) { LogWriter.error("Error save PlayerData to file", e); }
			if (update) { updateClient = true; }
		});
	}

	public void clear() {
		dialogData.clear();
		factionData.clear();
		itemgiverData.clear();
		mailData.clear();
		questData.clear();
		transportData.clear();
		timers.clear();
		game.clear();
		minimap.clear();
	}

	public static NBTTagCompound loadPlayerData(String uuid, String name) {
		if (name.isEmpty()) { name = "noplayername"; }
		File saveDir = CustomNpcs.getWorldSaveDirectory("playerdata/"+uuid);
		if (saveDir != null && (saveDir.exists() || saveDir.mkdirs())) {
			File file = new File(saveDir, name + ".json");
			File oldVersionFile = new File(saveDir.getParentFile(), uuid + ".json");
			if (!oldVersionFile.exists()) { oldVersionFile = new File(saveDir.getParentFile(), uuid + ".dat"); }
			if (!file.exists() && oldVersionFile.exists() && oldVersionFile.isFile()) {
				try {
					NBTTagCompound nbt = NBTJsonUtil.LoadFile(oldVersionFile);
					if (oldVersionFile.delete()) { NBTJsonUtil.SaveFile(file, nbt); }
					return nbt;
				}
				catch (Exception e) { LogWriter.error("Error old loading: " + oldVersionFile.getAbsolutePath(), e); }
				return new NBTTagCompound();
			}
			else if (file.exists() && file.isFile()) {
				try {
					if (!oldVersionFile.exists() || oldVersionFile.delete()) { return NBTJsonUtil.LoadFile(file); }
				}
				catch (Exception e) { LogWriter.error("Error loading: " + file.getAbsolutePath(), e); }
			}
		}
		return new NBTTagCompound();
	}

	public static @Nonnull PlayerData get(@Nullable EntityPlayer player) {
		if (player == null || player.world == null || player.world.isRemote) {
			return ClientProxy.getPlayerData();
		}
		PlayerData data = (PlayerData) player.getCapability(PLAYERDATA_CAPABILITY, null);
		if (data == null) {
			LogWriter.warn("Hmmm. Why is a new \"PlayerData\" being created?");
			data = new PlayerData();
		}
		if (data.player == null) {
			data.player = player;
			data.playerLevel = player.experienceLevel;
			data.animation = new DataAnimation(player);
			data.setNBT(loadPlayerData(player.getUniqueID().toString(), player.getName()));
		}
		return data;
	}

	public IPlayer<?> getIPlayer() {
		if (playerAPI == null && player != null) { playerAPI = new PlayerWrapper<>(player); }
		return playerAPI;
	}

}
