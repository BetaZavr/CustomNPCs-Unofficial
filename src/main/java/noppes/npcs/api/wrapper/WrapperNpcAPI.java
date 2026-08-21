package noppes.npcs.api.wrapper;

import java.io.File;
import java.util.*;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import nikedemos.markovnames.generators.MarkovGenerator;
import noppes.npcs.*;
import noppes.npcs.api.*;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.entity.data.INpcAttribute;
import noppes.npcs.api.entity.data.IPlayerMail;
import noppes.npcs.api.gui.IComponent;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.handler.*;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.overlay.IOverlay;
import noppes.npcs.api.wrapper.data.AttributeWrapper;
import noppes.npcs.api.wrapper.gui.ComponentWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiWrapper;
import noppes.npcs.containers.ContainerNpcInterface;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.world.entity.IEntityMixin;
import noppes.npcs.shared.client.gui.util.ResourceData;
import noppes.npcs.shared.common.util.LRUHashMap;
import noppes.npcs.util.NBTJsonUtil;
import noppes.npcs.util.Util;

public class WrapperNpcAPI extends NpcAPI {

   private static NpcAPI instance = null;

   public static final LRUHashMap<ResourceKey<Level>, WorldWrapper> worldCache = new LRUHashMap<>(300);
   public static final IEventBus EVENT_BUS = BusBuilder.builder().build();
   private final List<Level> levels = Lists.newArrayList();

   public static void clearCache() {
      worldCache.clear();
      BlockWrapper.clearCache();
   }

   public static void resetScriptControllerData(CompoundTag compound) {
      WorldWrapper.getStoredData().setNbt(new NBTWrapper(compound));
   }

   @Override
   public IEntity<?> getIEntity(Entity entity) {
      if (entity == null) { return null; }
      if (entity instanceof EntityNPCInterface) { return ((EntityNPCInterface) entity).wrappedNPC; }
      return WrapperEntityData.get(entity);
   }

   @Override
   public ICustomNpc<?> createNPC(Level level) {
      if (level.isClientSide) {
         return null;
      }
      EntityCustomNpc npc = new EntityCustomNpc(CustomEntities.entityCustomNpc, level);
      return npc.wrappedNPC;
   }

   @SuppressWarnings("unused")
   public void registerPermissionNode(String permission, int defaultType) {
      if (defaultType != 0) { throw new CustomNPCsException("There is only one default type available, 0: a boolean value"); }
      if (hasPermissionNode(permission)) { throw new CustomNPCsException("Permission \"" + permission + "\" already exists"); }
      //throw new CustomNPCsException("RegisterPermissionNode is no longer supported");
      CustomNpcsPermissions.register(permission);
   }

   @Override
   public boolean hasPermissionNode(String permission) {
      for (PermissionNode<?> node : PermissionAPI.getRegisteredNodes()) {
         if (node.getNodeName().equals(permission)) { return true; }
      }
      return CustomNpcsPermissions.hasPermission(permission);
   }

   @Override
   public ICustomNpc<?> spawnNPC(Level level, int x, int y, int z) {
      if (level.isClientSide) {
         return null;
      }
      EntityCustomNpc npc = new EntityCustomNpc(CustomEntities.entityCustomNpc, level);
      npc.absMoveTo((double)x + 0.5D, y, (double)z + 0.5D, 0.0F, 0.0F);
      npc.ais.setStartPos(x, y, z);
      npc.setHealth(npc.getMaxHealth());
      level.addFreshEntity(npc);
      return npc.wrappedNPC;
   }

   public static NpcAPI Instance() {
      if (instance == null) { instance = new WrapperNpcAPI(); }
      return instance;
   }

   @Override
   public IEventBus events() { return EVENT_BUS; }

   @Override
   public IBlock getIBlock(Level level, BlockPos pos) {
      return BlockWrapper.createNew(level, pos, level.getBlockState(pos));
   }

   @Override
   public IItemStack getIItemStack(ItemStack stackMC) {
      return stackMC != null && !stackMC.isEmpty() ?
              stackMC.getCapability(ItemStackWrapper.ITEMSCRIPTEDDATA_CAPABILITY, null).orElse(ItemStackWrapper.AIR) :
              ItemStackWrapper.AIR;
   }

   @Override
   public IWorld getIWorld(Level level) {
      WorldWrapper w = worldCache.get(level.dimension());
      if (w != null) {
         w.level = level;
      } else {
         worldCache.put(level.dimension(), w = WorldWrapper.createNew(level));
      }
      return w;
   }

   @Override
   public IWorld getIWorld(DimensionType dimensionTypeMC) {
      if (CustomNpcs.Server != null && levels.isEmpty()) {
         for (Level level : CustomNpcs.Server.getAllLevels()) { levels.add(level); }
      }
      for (Level level : levels) {
         if (level.dimensionType() == dimensionTypeMC) { return getIWorld(level); }
      }
      throw new CustomNPCsException((Thread.currentThread().getName().toLowerCase().contains("client") ? "Not found" : "Unknown") + " dimension: \"" + dimensionTypeMC + "\"");
   }

   @Override
   public IWorld getIWorld(String dimensionName) {
      if (CustomNpcs.Server == null) {
         Player player = CustomNpcs.proxy.getPlayer();
         if (player != null && !levels.contains(player.level())) { levels.add(player.level()); }
      }
      else if (levels.isEmpty()) { for (Level level : CustomNpcs.Server.getAllLevels()) { levels.add(level); } }
      ResourceLocation loc = ResourceLocation.tryParse(dimensionName);
      for (Level level : levels) {
         if (level.dimension().location().equals(loc)) { return getIWorld(level); }
      }
      throw new CustomNPCsException((Thread.currentThread().getName().toLowerCase().contains("client") ? "Not found" : "Unknown") + " dimension: \"" + loc + "\"");
   }

   @Override
   public IContainer getIContainer(AbstractContainerMenu container) {
      return new ContainerWrapper(container);
   }

   @Override
   public IContainer getIContainer(Container inventory) {
      return inventory instanceof ContainerNpcInterface container ? ContainerNpcInterface.getOrCreateIContainer(container) : new ContainerWrapper(inventory);
   }

   @Override
   public IFactionHandler getFactions() {
      checkLevel();
      return FactionController.instance;
   }

   private void checkLevel() {
      if (CustomNpcs.Server != null && CustomNpcs.Server.isStopped()) {
         throw new CustomNPCsException("No world is loaded right now");
      }
   }

   @Override
   public IRecipeHandler getRecipes() {
      checkLevel();
      return RecipeController.getInstance();
   }

   @Override
   public IQuestHandler getQuests() {
      checkLevel();
      return QuestController.instance;
   }

   @Override
   public IWorld[] getIWorlds() {
      checkLevel();
      IWorld[] worlds;
      if (CustomNpcs.Server == null) {
         worlds = new IWorld[levels.size()];
      } else {
         if (levels.isEmpty()) {
            for (Level level : CustomNpcs.Server.getAllLevels()) { levels.add(level); }
         }
         int i = 0;
         worlds = new IWorld[levels.size()];
         for (Level level : levels) { worlds[i++] = getIWorld(level); }
      }
      return worlds;
   }

   @Override
   public File getGlobalDir() {
      return CustomNpcs.Dir;
   }

   @Override
   public File getLevelDir() {
      return CustomNpcs.getLevelSaveDirectory();
   }

   @Override
   public INbt getINbt(CompoundTag nbtMC) {
      return nbtMC == null ? new NBTWrapper(new CompoundTag()) : new NBTWrapper(nbtMC);
   }

   @Override
   public INbt stringToNbt(String str) {
      if (str != null && !str.isEmpty()) {
         try {
            return new NBTWrapper(NBTJsonUtil.Convert(str));
         } catch (NBTJsonUtil.JsonException var3) {
            throw new CustomNPCsException(var3, "Failed converting " + str);
         }
      } else {
         throw new CustomNPCsException("Cant cast empty string to nbt");
      }
   }

   @Override
   public IDamageSource getIDamageSource(DamageSource damageMC) {
      return new DamageSourceWrapper(damageMC);
   }

   @Override
   public IDialogHandler getDialogs() { return DialogController.instance; }

   @Override
   public ICloneHandler getClones() { return ServerCloneController.Instance; }

   @Override
   public String executeCommand(IWorld level, String command) {
      FakePlayer player = EntityNPCInterface.CommandPlayer;
      ((IEntityMixin) player).setLevel(level.getMCLevel());
      player.setPos(0.0D, 0.0D, 0.0D);
      return NoppesUtilServer.runCommand(level.getMCLevel(), BlockPos.ZERO, "API", command, null, player);
   }

   @Override
   public INbt getRawPlayerData(String uuid, String name) {
      if  (CustomNpcs.Server != null) {
         UUID uuidMC;
         try { uuidMC = UUID.fromString(uuid); }
         catch (Exception e) { throw new CustomNPCsException("Invalid UUID string: \"" + uuid + "\""); }
         ServerPlayer player = CustomNpcs.Server.getPlayerList().getPlayer(uuidMC);
         if (player != null && player.getName().toString().equals(name)) {
            return new NBTWrapper(PlayerData.get(player).getNBT());
         }
      }
      return new NBTWrapper(PlayerData.loadPlayerData(uuid, name));
   }

   @Override
   public IPlayerMail createMail(String sender, String title) {
      PlayerMail mail = new PlayerMail();
      mail.sender = sender;
      mail.title = title;
      return mail;
   }

   @Override
   public ICustomGui createCustomGui(int id, int width, int height, boolean pauseGame, IPlayer<?> player) {
      return new CustomGuiWrapper(player, id, width, height, pauseGame);
   }

   @Override
   public IOverlay createOverlay(int id) { return new OverlayWrapper(id); }

   @Override
   public String getRandomName(int dictionary, int gender) {
      return MarkovGenerator.fetch(dictionary, gender);
   }

   @Override
   public IPlayer<?>[] getAllPlayers() {
      List<IPlayer<?>> list = Lists.newArrayList();
      if (CustomNpcs.Server != null) {
         for (ServerPlayer player : CustomNpcs.Server.getPlayerList().getPlayers()) {
            if (player == null) { continue; }
            list.add((IPlayer<?>) this.getIEntity(player));
         }
      } else { // Client Side
         Player player = CustomNpcs.proxy.getPlayer();
         if (player != null) { return getIWorld(player.level()).getAllPlayers(); }
      }
      return list.toArray(new IPlayer<?>[0]);
   }

   @Override
   public IPlayer<?> getIPlayer(String nameOrUUID) {
      IPlayer<?>[] iPlayers = getAllPlayers();
      for (IPlayer<?> iPlayer : iPlayers) {
         if (iPlayer.getName().equals(nameOrUUID) || iPlayer.getUUID().equals(nameOrUUID)) { return iPlayer; }
      }
      return null;
   }

   @Override
   public IPos getIPos(BlockPos posMC) { return new BlockPosWrapper(null, posMC); }

   @Override
   public IPos getIPos(double x, double y, double z) { return new BlockPosWrapper(null, x, y, z); }

   @Override
   public ICustomPlayerData getPlayerData(IPlayer<?> player) {
      if (player == null) { return null; }
      return PlayerDataController.instance.getDataFromUsername(CustomNpcs.Server, player.getName());
   }

   @Override
   public IData getTempdata() { return WorldWrapper.getTempData(); }

   @Override
   public IData getStoreddata() { return WorldWrapper.getStoredData(); }

   @Override
   public IComponent getIComponent(String text) { return ComponentWrapper.of(text); }

   @Override
   public IBorderHandler getBorders() { return BorderController.getInstance(); }

   @Override
   public INpcAttribute getIAttribute(AttributeInstance attributeMC) { return new AttributeWrapper(attributeMC); }

   @Override
   public IEntityDamageSource getIDamageSource(String name, IEntity<?> entity) {
      if (entity == null) { return null; }
      return NpcEntityDamageSource.create(name, entity.getMCEntity());
   }

   @Override
   public IMethods getMethods() { return Util.instance; }

   @Override
   public IMarcetHandler getMarkets() { return MarcetController.getInstance(); }

   @Override
   public IKeyBinding getIKeyBinding() { return KeyController.getInstance(); }

   @Override
   public IDimensionHandler getCustomDimension() { return null; }

   @Override
   public ResourceData getResourceData(ResourceLocation texture, int u, int v, int width, int height) { return new ResourceData(texture, u, v, width, height); }

   @Override
   public List<?> createList() { return new ArrayList<>(); }

   @Override
   public Map<?, ?> createMap() { return new LinkedHashMap<>(); }

   @Override
   public Map<?, ?> createTreeMap() { return new TreeMap<>(); }

}
