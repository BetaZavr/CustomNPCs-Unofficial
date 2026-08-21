package noppes.npcs.api;

import java.io.File;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import noppes.npcs.CustomNpcs;
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
import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.overlay.IOverlay;
import noppes.npcs.api.wrapper.WrapperNpcAPI;
import noppes.npcs.shared.client.gui.util.ResourceData;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nullable;

public abstract class NpcAPI {

   private static NpcAPI instance = null;

   public static @Nullable NpcAPI Instance() {
      if (instance != null) { return instance;}
      if (!IsAvailable()) { return null; }
      try { NpcAPI.instance = WrapperNpcAPI.Instance(); }
      catch (Exception e) { LogWriter.error(e); }
      return instance;
   }

   public static boolean IsAvailable() { return ModList.get().isLoaded(CustomNpcs.MODID); }

   @SuppressWarnings("unused")
   public abstract ICustomNpc<?> createNPC(@ParamName("levelMC") Level levelMC);

   @SuppressWarnings("unused")
   public abstract ICustomNpc<?> spawnNPC(@ParamName("levelMC") Level levelMC, @ParamName("x") int x, @ParamName("y") int y, @ParamName("z") int z);

   public abstract IEntity<?> getIEntity(@ParamName("entityMC") Entity entityMC);

   public abstract IBlock getIBlock(@ParamName("levelMC") Level levelMC, @ParamName("posMC") BlockPos posMC);

   public abstract IContainer getIContainer(@ParamName("containerMC") AbstractContainerMenu containerMC);

   public abstract IContainer getIContainer(@ParamName("inventoryMC") Container inventoryMC);

   public abstract IItemStack getIItemStack(@ParamName("stackMC") ItemStack stackMC);

   public abstract IWorld getIWorld(@ParamName("levelMC") Level levelMC);

   public abstract IWorld getIWorld(@ParamName("dimensionName") String dimensionName);

   @SuppressWarnings("unused")
   public abstract IWorld getIWorld(@ParamName("dimensionTypeMC") DimensionType dimensionTypeMC);

   @SuppressWarnings("unused")
   public abstract IWorld[] getIWorlds();

   @SuppressWarnings("unused")
   public abstract INbt getINbt(@ParamName("nbtMC") CompoundTag nbtMC);

   public abstract IPos getIPos(@ParamName("x") double x, @ParamName("y") double y, @ParamName("z") double z);

   public abstract IFactionHandler getFactions();

   public abstract IRecipeHandler getRecipes();

   public abstract IQuestHandler getQuests();

   public abstract IDialogHandler getDialogs();

   public abstract ICloneHandler getClones();

   public abstract IDamageSource getIDamageSource(@ParamName("damageMC") DamageSource damageMC);

   @SuppressWarnings("unused")
   public abstract INbt stringToNbt(@ParamName("str") String str);

   @SuppressWarnings("unused")
   public abstract IPlayerMail createMail(@ParamName("sender") String sender, @ParamName("title") String title);

   @SuppressWarnings("unused")
   public abstract ICustomGui createCustomGui(@ParamName("id") int id, @ParamName("width") int width, @ParamName("height") int height,
                                              @ParamName("pauseGame") boolean pauseGame, @ParamName("player") IPlayer<?> player);

   @SuppressWarnings("unused")
   public abstract IOverlay createOverlay(@ParamName("id") int id);

   public abstract IEventBus events();

   @SuppressWarnings("unused")
   public abstract File getGlobalDir();

   @SuppressWarnings("unused")
   public abstract File getLevelDir();

   public abstract boolean hasPermissionNode(@ParamName("permission") String permission);

   public abstract String executeCommand(@ParamName("world") IWorld level, @ParamName("command") String command);

   public abstract String getRandomName(@ParamName("dictionary") int dictionary, @ParamName("gender") int gender);

   // New from Unofficial (BetaZavr)
   public abstract IPlayer<?>[] getAllPlayers();

   @SuppressWarnings("unused")
   public abstract IBorderHandler getBorders();

   @SuppressWarnings("unused")
   public abstract IDimensionHandler getCustomDimension();

   public abstract INpcAttribute getIAttribute(@ParamName("attributeMC") AttributeInstance attributeMC);

   @SuppressWarnings("unused")
   public abstract IEntityDamageSource getIDamageSource(@ParamName("name") String name, @ParamName("entity") IEntity<?> entity);

   public abstract IKeyBinding getIKeyBinding();

   @SuppressWarnings("unused")
   public abstract IPlayer<?> getIPlayer(@ParamName("nameOrUUID") String nameOrUUID);

   public abstract IPos getIPos(@ParamName("posMC") BlockPos posMC);

   public abstract IMarcetHandler getMarkets();

   @SuppressWarnings("unused")
   public abstract IMethods getMethods();

   @SuppressWarnings("unused")
   public abstract INbt getRawPlayerData(@ParamName("uuid") String uuid, @ParamName("name") String name);

   @SuppressWarnings("unused")
   public abstract ICustomPlayerData getPlayerData(@ParamName("player") IPlayer<?> player);

   @SuppressWarnings("unused")
   public abstract ResourceData getResourceData(@ParamName("texture") ResourceLocation texture, @ParamName("u") int u, @ParamName("v") int v, @ParamName("width") int width, @ParamName("height") int height);

   public abstract IData getTempdata();

   public abstract IData getStoreddata();

   @SuppressWarnings("unused")
   public abstract IComponent getIComponent(String text);

   @SuppressWarnings("unused")
   public abstract List<?> createList();

   public abstract Map<?, ?> createMap();

   @SuppressWarnings("unused")
   public abstract Map<?, ?> createTreeMap();

}
