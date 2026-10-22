package noppes.npcs.controllers;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.util.EnumHelper;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.gui.IDimensionGetter;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.controllers.data.DimensionData;
import noppes.npcs.dimensions.CustomWorldInfo;
import noppes.npcs.dimensions.CustomWorldProvider;
import noppes.npcs.dimensions.WorldCustom;
import noppes.npcs.mixin.minecraftforge.common.IDimensionManagerMixin;
import noppes.npcs.packets.server.SPacketDimensionTeleport;
import noppes.npcs.packets.server.SPacketDimensionsGet;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.INbt;
import noppes.npcs.api.handler.IDimensionHandler;
import noppes.npcs.api.handler.data.IWorldInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DimensionController extends WorldSavedData implements IDimensionHandler {

	protected static String NAME = "cnpcs_dimensions";
	protected static boolean update = false;

	public static DimensionController getInstance() {
		DimensionController INSTANCE;
		MapStorage mapStorage = null;
		try { mapStorage = CustomNpcs.Server != null ? CustomNpcs.Server.getEntityWorld().getMapStorage() : null; } catch (Exception ignored) { }
        if (mapStorage != null) {
			INSTANCE = (DimensionController) mapStorage.getOrLoadData(DimensionController.class, NAME);
			if (INSTANCE == null) {
				INSTANCE = (DimensionController) mapStorage.getOrLoadData(DimensionController.class, "CustomNpcsHandler");
				update = true;
			} // OLD
			if (INSTANCE == null) {
				INSTANCE = new DimensionController(NAME);
				mapStorage.setData(NAME, INSTANCE);
				update = true;
			} // not register
		}
		else { INSTANCE = new DimensionController(NAME); }
		return INSTANCE;
	}

	public DimensionController(String mapName) { super(mapName); }

	protected static final Map<Integer, DimensionData> data = new LinkedHashMap<>();
	protected static final Map<Integer, DimensionType> ownTypes = new HashMap<>();
	protected final Map<Integer, CustomWorldInfo> dimensionInfo = new TreeMap<>();
	protected final Map<Integer, NBTTagCompound> providerInfo = new TreeMap<>();
	protected final Map<Integer, UUID> toBeDeleted = new TreeMap<>();

	@Override
	public IWorldInfo createNewDimension() {
		CustomWorldInfo cwi = new CustomWorldInfo(new NBTTagCompound());
		createNewDimension(null, cwi, true);
		return cwi;
	}

	@Override
	public void deleteDimension(int dimId) { deleteDimension(null, dimId); }

	@Override
	public int copyDimension(int dimId) { return copyDimension(null, dimId); }

	@Override
	public List<Integer> getAllIDs() {
		List<Integer> list = new ArrayList<>();
		for (DimensionType dt : DimensionType.values()) { list.add(dt.getId()); }
		for (int id : ownTypes.keySet()) {
			if (!list.contains(id)) { list.add(id); }
		}
		for (int id : dimensionInfo.keySet()) {
			if (!list.contains(id)) { list.add(id); }
		}
		for (int id : toBeDeleted.keySet()) {
			if (!list.contains(id)) { list.add(id); }
		}
		return list;
	}

	@Override
	public IWorldInfo getMCWorldInfo(int dimId) { return dimensionInfo.get(dimId); }

	@Override
	public INbt getProviderInfo(int id) {
		return new NBTWrapper(providerInfo.containsKey(id) ? providerInfo.get(id) : new NBTTagCompound());
	}

	@Override
	public INbt getNbt() {
		NBTTagCompound nbt = new NBTTagCompound();
		writeToNBT(nbt);
		return new NBTWrapper(nbt);
	}

	@Override
	public void setNbt(INbt nbt) { readFromNBT(nbt.getMCNBT()); }

	@Override
	public boolean isDirty() { return update; }

	/**
	 * File path: .../<world name>/data/cnpcs_dimensions.dat
	 * @param nbt - compound tag (name: "data") from file "cnpcs_dimensions.dat"
	 */
	@Override
	public void readFromNBT(@Nonnull NBTTagCompound nbt) {
		dimensionInfo.clear();
		providerInfo.clear();
		NBTTagList nbtList = nbt.getTagList("dimensions", 10);
		if (nbtList.hasNoTags()) { nbtList = nbt.getTagList("dimensionInfo", 10); } // OLD
		for (int i = 0; i < nbtList.tagCount(); i++) {
			NBTTagCompound compound = nbtList.getCompoundTagAt(i);
			int id = compound.getInteger("dimensionID");
			dimensionInfo.put(id, new CustomWorldInfo(compound.getCompoundTag("worldInfo")));
			providerInfo.put(id, compound.getCompoundTag("providerInfo"));
		}
		checkExample();
		update = false;
	}

	@Override
	public @Nonnull NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt) {
		NBTTagList nbtList = new NBTTagList();
		for (Entry<Integer, CustomWorldInfo> entry : dimensionInfo.entrySet()) {
			if (!toBeDeleted.containsKey(entry.getKey())) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("dimensionID", entry.getKey());
				compound.setTag("worldInfo", entry.getValue().cloneNBTCompound(entry.getValue().getPlayerNBTTagCompound()));
				compound.setTag("providerInfo", providerInfo.containsKey(entry.getKey()) ? providerInfo.get(entry.getKey()) : new NBTTagCompound());
				nbtList.appendTag(compound);
			}
		}
		nbt.setTag("dimensions", nbtList);
		return nbt;
	}

	// example dimension
	private void checkExample() {
		if (!dimensionInfo.containsKey(100)) {
			CustomWorldInfo cwi = new CustomWorldInfo(new NBTTagCompound());
			cwi.dimensionId = 100;
			cwi.setDimensionName("example");
			cwi.setDisplayName("Example Dimension");
			createNewDimension(null, cwi, false);
		}
	}

	public void createNewDimension(EntityPlayerMP player, CustomWorldInfo worldInfo, boolean isUpdating) {
		int id = worldInfo.dimensionId != 100 ? findFreeDimensionID() : 100;
		worldInfo.dimensionId = id;
		if (worldInfo.getDimensionName().startsWith("default_") && !worldInfo.getDimensionName().contains("_copy")) {
			worldInfo.setDimensionName("default_" + id);
			worldInfo.setDisplayName("default_" + id);
		}
		Function<String, Boolean> getFromName = name -> {
			for (CustomWorldInfo customWorldInfo : dimensionInfo.values()) {
				if (customWorldInfo.getDisplayName().equals(name)) { return true; }
			}
            return false;
        };
		String name = Util.instance.deleteColor(worldInfo.getDisplayName());
		while (getFromName.apply(name)) { name += "_"; }
		worldInfo.setDisplayName(name);
		dimensionInfo.put(id, worldInfo);
		providerInfo.put(id, new NBTTagCompound());
		DimensionType dimensionType = getDimensionType(id, worldInfo.getDimensionName());
		if (!DimensionManager.isDimensionRegistered(id) && !registerDimension(id, dimensionType)) {
			dimensionInfo.remove(id);
			providerInfo.remove(id);
			return;
		}
		worldInfo.setDimensionName(dimensionType.getName());
		if (player != null) {
			player.sendMessage(Component.translatable("message.dimensions.created", id, name).getParent());
		}
		if (isUpdating) { syncWithClients(); }
	}

	private static boolean registerDimension(int id, DimensionType type) {
		try {
			DimensionManager.registerDimension(id, type);
			return true;
		}
		catch (IllegalArgumentException e) {
			if (ownTypes.get(type.getId()) != type || DimensionManager.isDimensionRegistered(id)) {
				LogWriter.error("Cannot register dimension " + id, e);
				return false;
			}
			LogWriter.error("Dimension type " + type.getId() + " is not visible to DimensionType.getById, registering dimension " + id + " directly");
			return registerDirect(id, type);
		}
	}

	private static boolean reflectionReady;
	private static Constructor<?> dimensionConstructor;
	private static Field dimensionsField;
	private static Field usedIdsField;

	private static void initReflection() {
		if (reflectionReady) { return; }
		reflectionReady = true;
		Class<?> dimensionClass = null;
		for (Class<?> inner : DimensionManager.class.getDeclaredClasses()) {
			for (Constructor<?> constructor : inner.getDeclaredConstructors()) {
				Class<?>[] params = constructor.getParameterTypes();
				if (params.length == 1 && params[0] == DimensionType.class && (dimensionConstructor == null || inner.getSimpleName().equals("Dimension"))) {
					dimensionConstructor = constructor;
					dimensionClass = inner;
				}
			}
		}
		if (dimensionClass == null) { return; }
		dimensionConstructor.setAccessible(true);
		for (Field field : DimensionManager.class.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers())) { continue; }
			if (Map.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType) {
				for (Type arg : ((ParameterizedType) field.getGenericType()).getActualTypeArguments()) {
					if (arg == dimensionClass && (dimensionsField == null || field.getName().equals("dimensions"))) { dimensionsField = field; }
				}
			}
			if (Set.class.isAssignableFrom(field.getType()) && field.getName().equals("usedIds")) { usedIdsField = field; }
		}
		if (dimensionsField != null) { dimensionsField.setAccessible(true); }
		if (usedIdsField != null) { usedIdsField.setAccessible(true); }
	}

	@SuppressWarnings("unchecked")
	private static boolean registerDirect(int id, DimensionType type) {
		try {
			initReflection();
			if (dimensionConstructor == null || dimensionsField == null) {
				LogWriter.error("Cannot register dimension " + id + ": DimensionManager structure is not recognized");
				return false;
			}
			((Map<Integer, Object>) dimensionsField.get(null)).put(id, dimensionConstructor.newInstance(type));
			if (id >= 0) {
				if (usedIdsField != null) { ((Set<Integer>) usedIdsField.get(null)).add(id); }
				else { LogWriter.warn("DimensionManager used ids are not found, dimension " + id + " is registered without reserving its id"); }
			}
			return true;
		}
		catch (Throwable e) {
			LogWriter.error("Cannot register dimension " + id, e);
			return false;
		}
	}

	public DimensionType getDimensionType(int id, String name) {
		DimensionType dimensionType = ownTypes.get(id);
		if (dimensionType != null) { return dimensionType; }
		for (DimensionType dt : DimensionType.values()) {
			if (dt.getId() == id) {
				dimensionType = dt;
				break;
			}
		}
		if (id >= 100 && dimensionType == null) {
			DimensionType dimensionTemp = EnumHelper.addEnum(DimensionType.class, "custom_dimension_" + id,
					new Class<?>[] { int.class, String.class, String.class, Class.class }, id,
					name != null && !name.isEmpty() ? NoppesUtilServer.validPath(Util.instance.deleteColor(name)) : "default_" + id,
					"_cnpc",
					CustomWorldProvider.class);
			dimensionType = dimensionTemp != null ? dimensionTemp.setLoadSpawn(false) : DimensionType.OVERWORLD;
			if (dimensionTemp != null) { ownTypes.put(id, dimensionTemp); }
		} // register
		return dimensionType;
	}

	public void deleteDimension(EntityPlayerMP player, int dimId) {
		if (dimId <= 100 || !dimensionInfo.containsKey(dimId)) {
			if (player != null) {
				if (toBeDeleted.containsKey(dimId)) {
					player.sendMessage(Component.translatable("message.dimensions.err.del").getParent());
				} else if (!dimensionInfo.containsKey(dimId)) {
					player.sendMessage(Component.translatable("message.dimensions.err.notmod").getParent());
				}
			}
			return;
		}
		// remove all players
		MinecraftServer server = player == null ? CustomNpcs.Server : player.getServer();
		World world = DimensionManager.getWorld(dimId);
		if (world != null && server != null) {
			WorldServer overworld = server.getWorld(0);
			BlockPos pos = overworld.getSpawnPoint();
			if (!overworld.isAirBlock(pos)) { pos = overworld.getTopSolidOrLiquidBlock(pos); }
			else {
				while (overworld.isAirBlock(pos) && pos.getY() > 0) { pos = pos.down(); }
				if (pos.getY() == 0) { pos = overworld.getTopSolidOrLiquidBlock(pos); }
			}
			pos = pos.up();
			for (EntityPlayerMP p : CustomNpcs.Server.getPlayerList().getPlayers()) {
				if (p.world.provider.getDimension() == dimId) {
					p.sendMessage(Component.translatable("message.dimensions.tp.isdelete").getParent());
					SPacketDimensionTeleport.teleportPlayer(p, 0,
							pos.getX(), pos.getY(), pos.getZ(),
							p.rotationYaw, p.rotationPitch);
				}
			}
		}
		// remove
		toBeDeleted.put(dimId, player != null ? player.getUniqueID() : null);
		DimensionManager.unloadWorld(dimId);
		if (DimensionManager.isDimensionRegistered(dimId)) { DimensionManager.unregisterDimension(dimId); }
		IDimensionManagerMixin.getUnloadQueue().remove(dimId);
		syncWithClients();
	}

	private int findFreeDimensionID() {
		int id = 100;
		while (dimensionInfo.containsKey(id) || toBeDeleted.containsKey(id)) { id++; }
		return id;
	}

	public boolean isDelete(int id) { return toBeDeleted.containsKey(id); }

	private void loadDimension(int dimId, CustomWorldInfo worldInfo) {
		WorldServer overworld = (WorldServer) CustomNpcs.Server.getEntityWorld();
		try {
			DimensionManager.getProviderType(dimId);
		} catch (Exception e) {
			LogWriter.error("Cannot Hot-load Dim: " + e);
			return;
		}
		MinecraftServer mcServer = overworld.getMinecraftServer();
		ISaveHandler saveHandler = overworld.getSaveHandler();
		assert mcServer != null;
		EnumDifficulty difficulty = mcServer.getEntityWorld().getDifficulty();
		WorldServer world = (WorldServer) (new WorldCustom(worldInfo, mcServer, saveHandler, dimId, overworld,
				mcServer.profiler).init());
		world.addEventListener(new ServerWorldEventHandler(mcServer, world));
		LogWriter.debug("Try Load World: " + dimId + "; world = " + world);
		try {
			Class.forName("org.orecruncher.dsurround.server.services.AtmosphereService");
		} catch (ClassNotFoundException e) {
			MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(world));
		}
		if (!mcServer.isSinglePlayer()) {
			world.getWorldInfo().setGameType(mcServer.getGameType());
		}
		mcServer.setDifficultyForAllWorlds(difficulty);
	}

	public void loadDimensions() {
		for (Entry<Integer, CustomWorldInfo> entry : dimensionInfo.entrySet()) {
			if (!DimensionManager.isDimensionRegistered(entry.getKey())) {
				registerDimension(entry.getKey(), getDimensionType(entry.getKey(), entry.getValue().getDimensionName()));
			}
		}
	}

	private void syncWithClients() {
		if (CustomNpcs.Server != null && !CustomNpcs.Server.getPlayerList().getPlayers().isEmpty()) {
			for (EntityPlayerMP player : CustomNpcs.Server.getPlayerList().getPlayers()) {
				SPacketDimensionsGet.sendDimensionIDs(player);
			}
		}
		update = true;
	}

	/**
	 * Only unregister the dimension if it is actually being deleted.
	 * Do NOT unregister on normal world unload (lazy loading).
	 */
	public void unload() {
        for (int dimId : toBeDeleted.keySet()) {
            if (dimensionInfo.containsKey(dimId) && DimensionManager.isDimensionRegistered(dimId)) { DimensionManager.unregisterDimension(dimId); }
            dimensionInfo.remove(dimId);
            providerInfo.remove(dimId);
        }
		toBeDeleted.clear();
		File saveRoot = CustomNpcs.getWorldSaveDirectory();
		if (saveRoot != null) { Util.instance.removeFile(new File(saveRoot.getParentFile(), "data/CustomNpcsHandler.dat")); }
	}

	/**
	 * Public accessor for lazy-loading a custom dimension.
	 */
	public void ensureDimensionLoaded(int dimId) {
		if (dimensionInfo.containsKey(dimId) && DimensionManager.getWorld(dimId) == null) {
			loadDimension(dimId, dimensionInfo.get(dimId));
		}
	}

	@SuppressWarnings("ConstantConditions")
	public void recreateDimension(EntityPlayerMP player, int dimId) {
		List<Integer> allIDs = getAllIDs();
		if (dimId == 0 ||
				!allIDs.contains(dimId) ||
				toBeDeleted.containsKey(dimId) ||
				!DimensionManager.isDimensionRegistered(dimId)) {
			if (player != null) {
				if (toBeDeleted.containsKey(dimId)) { player.sendMessage(Component.translatable("message.dimensions.err.del").getParent()); }
				else if (!allIDs.contains(dimId) || !DimensionManager.isDimensionRegistered(dimId)) {
					player.sendMessage(Component.translatable("message.dimensions.err.not.found").getParent());
				}
			}
			return;
		}
		if (player != null) { player.sendMessage(Component.translatable("message.dimensions.recreate", "" + dimId).getParent()); }
		// Determine evacuation target: Nether for Overworld, Overworld for others
		int targetDim = (dimId == 0) ? -1 : 0;
		WorldServer world = CustomNpcs.Server.getWorld(targetDim);
		BlockPos pos = BlockPos.ORIGIN.up(60);
		if (world != null) {
			pos = world.getSpawnCoordinate();
			if (pos == null) {
				pos = world.getSpawnPoint();
				if (!world.isAirBlock(pos)) { pos = world.getTopSolidOrLiquidBlock(pos); }
				else {
					while (world.isAirBlock(pos) && pos.getY() > 0) { pos = pos.down(); }
					pos = pos.up();
					if (pos.getY() == 0) { pos = world.getTopSolidOrLiquidBlock(pos); }
				}
			}
		}
		if (CustomNpcs.Server != null) {
			for (EntityPlayerMP p : CustomNpcs.Server.getPlayerList().getPlayers()) {
				if (p.world.provider.getDimension() == dimId) {
					p.sendMessage(Component.translatable("message.dimensions.tp.isdelete").getParent());
					SPacketDimensionTeleport.teleportPlayer(p, targetDim,
							pos.getX(), pos.getY(), pos.getZ(),
							p.rotationYaw, p.rotationPitch);
				}
			}
		}
		DimensionManager.unloadWorld(dimId);
		CustomNPCsScheduler.runTack(() -> {
			// files
			File saveRoot = DimensionManager.getCurrentSaveRootDirectory();
			if (!Util.instance.removeFile(new File(saveRoot, "DIM" + dimId))) {
				if (player != null) { player.sendMessage(Component.translatable("message.dimensions.err.recreated", dimId).getParent()); }
				return;
			}
			if (player != null) { player.sendMessage(Component.translatable("message.dimensions.recreated", dimId).getParent()); }
			syncWithClients();
		}, 500);
	}

	public int copyDimension(EntityPlayerMP player, int dimId) {
		List<Integer> allIDs = getAllIDs();
		if (!allIDs.contains(dimId) ||
				toBeDeleted.containsKey(dimId) ||
				!DimensionManager.isDimensionRegistered(dimId)) {
			if (player != null) {
				if (toBeDeleted.containsKey(dimId)) { player.sendMessage(Component.translatable("message.dimensions.err.del").getParent()); }
				else if (!allIDs.contains(dimId) || !DimensionManager.isDimensionRegistered(dimId)) {
					player.sendMessage(Component.translatable("message.dimensions.err.not.found").getParent());
				}
			}
			return dimId;
		}
		if (player != null) {
			player.sendMessage(Component.translatable("message.dimensions.copy", "" + dimId).getParent());
		}
		WorldInfo parent = dimensionInfo.get(dimId);
		// need to create a measurement directory if it doesn't exist yet.
		WorldServer world = DimensionManager.getWorld(dimId);
		boolean unload = world == null;
		if (unload) {
			DimensionManager.initDimension(dimId);
			world = DimensionManager.getWorld(dimId);
		}
		if (parent == null) {
			if (world != null) { parent = world.getWorldInfo(); }
		}
		if (parent == null) {
			if (player != null) {
				player.sendMessage(Component.translatable("message.dimensions.err.copy", "" + dimId).getParent());
			}
			return dimId;
		}
		// copy
		CustomWorldInfo cwi = new CustomWorldInfo(parent.cloneNBTCompound(parent.getPlayerNBTTagCompound()));
		cwi.setDimensionName(cwi.getDimensionName() + "_copy_" + dimId);
		cwi.setDisplayName("Copy from " + dimId);
		if (parent instanceof CustomWorldInfo) {
			CustomWorldInfo pCwi = (CustomWorldInfo) parent;
			if (!pCwi.getDimensionName().startsWith("default_")) { cwi.setDimensionName(pCwi.getDimensionName() + "_copy"); }
			if (!pCwi.getDisplayName().isEmpty()) { cwi.setDisplayName(pCwi.getDisplayName() + " (copy from " + dimId + ")"); }
		}
		createNewDimension(player, cwi, true);
		providerInfo.put(cwi.dimensionId, providerInfo.containsKey(dimId) ? providerInfo.get(dimId).copy() : new NBTTagCompound());

		// files
		File saveRoot = DimensionManager.getCurrentSaveRootDirectory();
		if (!Util.instance.copyDirectory(new File(saveRoot, "DIM" + dimId), new File(saveRoot, "DIM" + cwi.dimensionId))) {
			if (player != null) {
				player.sendMessage(Component.translatable("message.dimensions.err.copy", "" + dimId).getParent());
			}
			return dimId;
		}
		if (unload) { DimensionManager.unloadWorld(dimId); }
		syncWithClients();
		return cwi.dimensionId;
	}

	public void restoreDimension(EntityPlayerMP player, int dimId) {
		if (!toBeDeleted.containsKey(dimId)) {
			if (player != null) {
				player.sendMessage(Component.translatable("message.dimensions.err.restore").getParent());
			}
			return;
		}
		if (!DimensionManager.isDimensionRegistered(dimId) && !registerDimension(dimId, getDimensionType(dimId, null))) { return; }
		IDimensionManagerMixin.getUnloadQueue().remove(dimId);
		toBeDeleted.remove(dimId);
		syncWithClients();
	}

	public static void loadData(NBTTagCompound compound) {
		data.clear();
		if (compound != null) {
			for (int i = 0; i < compound.getTagList("Data", 10).tagCount(); i++) {
				NBTTagCompound nbt = compound.getTagList("Data", 10).getCompoundTagAt(i);
				data.put(nbt.getInteger("id"), new DimensionData(nbt));
			}
			if (Minecraft.getMinecraft().currentScreen instanceof IDimensionGetter) {
				((IDimensionGetter) Minecraft.getMinecraft().currentScreen).resetDimension();
			}
		}
	}

	public static boolean hasDimensionData(int id) { return data.containsKey(id); }

	public static @Nullable DimensionData getDimensionData(Integer id) { return data.get(id); }

	public static List<DimensionData> getDimensionsData() { return new ArrayList<>(data.values()); }

	public static void clearDimensionsData() { data.clear(); }

	public static void addDimensionData(DimensionData dd) { data.put(dd.dimensionId, dd); }
}