package noppes.npcs.controllers.data;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import noppes.npcs.CustomEntities;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.api.handler.ICustomPlayerData;
import noppes.npcs.api.mixin.world.entity.IEntityIMixin;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAnimation;
import noppes.npcs.entity.data.DataTimers;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.NBTJsonUtil;
import noppes.npcs.util.TempFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerData
implements ICapabilityProvider, ICustomPlayerData {

   private static final ResourceLocation key = new ResourceLocation(CustomNpcs.MODID, "playerdata");
   public static Capability<PlayerData> PLAYERDATA_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() { });

   private final LazyOptional<PlayerData> instance;

   private EntityNPCInterface activeCompanion;

   public final PlayerDialogData dialogData = new PlayerDialogData();
   public final PlayerBankData bankData = new PlayerBankData(this);
   public final PlayerQuestData questData = new PlayerQuestData();
   public final PlayerTransportData transportData = new PlayerTransportData();
   public final PlayerFactionData factionData = new PlayerFactionData();
   public final PlayerItemGiverData itemgiverData = new PlayerItemGiverData();
   public final PlayerMailData mailData = new PlayerMailData();

   public PlayerScriptData scriptData;
   public BlockPos scriptBlockPos = BlockPos.ZERO;
   public ItemStack prevHeldItem = ItemStack.EMPTY;
   public EntityNPCInterface editingNpc;
   public DataTimers timers;
   public CompoundTag cloned;
   public Player player;
   public Entity mounted;
   public String name = "";
   public String uuid = "";
   public boolean updateClient;
   public int companionID = 0;
   public int playerLevel = 0;
   public int dialogId = -1;

   // New data from Unofficial (BetaZavr)
   public DataAnimation animation;
   public PlayerGameData game = new PlayerGameData();
   public PlayerCompassData compass = new PlayerCompassData();
   public PlayerMiniMapData minimap = new PlayerMiniMapData();
   public PlayerOverlayData overlay = new PlayerOverlayData();
   public final Map<String, TempFile> clientScriptFiles = new HashMap<>();

   public PlayerData() {
      instance = LazyOptional.of(() -> this);
      timers = new DataTimers(this);
   }

   public void setNBT(CompoundTag data) {
      dialogData.load(data);
      bankData.load(data);
      questData.load(data);
      transportData.load(data);
      factionData.load(data);
      itemgiverData.load(data);
      mailData.load(data);
      timers.load(data);

      // New data from Unofficial (BetaZavr)
      game.load(data);
      compass.load(data);
      minimap.load(data);
      overlay.load(data);
      if (player != null && !(player instanceof FakePlayer)) {
         name = player.getName().getString();
         uuid = player.getUUID().toString();
      } else {
         name = data.getString("PlayerName");
         uuid = data.getString("UUID");
      }
      companionID = data.getInt("PlayerCompanionId");
      if (data.contains("PlayerCompanion") && !hasCompanion() && player != null) {
         EntityCustomNpc npc = new EntityCustomNpc(CustomEntities.entityCustomNpc, player.level());
         npc.readAdditionalSaveData(data.getCompound("PlayerCompanion"));
         npc.setPos(player.getX(), player.getY(), player.getZ());
         if (npc.role.getEnumType() != RoleType.COMPANION) {
            ((RoleCompanion)npc.role).setSitting(false);
            player.level().addFreshEntity(npc);
            setCompanion(npc);
         }
      }
      if (player != null) {
         ((IEntityIMixin) player).npcs$getStoredData().setNbt(data.getCompound("ScriptStoreddata"));
      }
   }

   public CompoundTag getSyncNBT() {
      CompoundTag compound = new CompoundTag();
      dialogData.save(compound);
      questData.save(compound);
      mailData.save(compound);
      factionData.save(compound);

      game.save(compound);
      compass.save(compound);
      minimap.save(compound);
      overlay.save(compound);
      return compound;
   }

   public CompoundTag getNBT() {
      if (player != null && !(player instanceof FakePlayer)) {
         name = player.getName().getString();
         uuid = player.getUUID().toString();
      }
      CompoundTag compound = new CompoundTag();
      dialogData.save(compound);
      questData.save(compound);
      transportData.save(compound);
      factionData.save(compound);
      itemgiverData.save(compound);
      mailData.save(compound);
      timers.save(compound);

      game.save(compound);
      compass.save(compound);
      minimap.save(compound);
      overlay.save(compound);

      compound.putString("PlayerName", name);
      compound.putString("UUID", uuid);
      compound.putInt("PlayerCompanionId", companionID);
      if (player != null) { compound.put("ScriptStoreddata", ((IEntityIMixin) player).npcs$getStoredData().getNbt().getMCNBT()); }
      if (hasCompanion()) {
         CompoundTag nbt = new CompoundTag();
         if (activeCompanion.saveAsPassenger(nbt)) { compound.put("PlayerCompanion", nbt); }
      }

      return compound;
   }

   public boolean hasCompanion() { return activeCompanion != null && !activeCompanion.isRemoved(); }

   public void setCompanion(EntityNPCInterface npc) {
      if (npc == null || npc.role.getType() == 6) {
         ++companionID;
         activeCompanion = npc;
         if (npc != null) { ((RoleCompanion)npc.role).companionID = companionID; }
         save(false);
      }
   }

   public void updateCompanion(Level level) {
      if (hasCompanion() && level != activeCompanion.level()) {
         RoleCompanion role = (RoleCompanion) activeCompanion.role;
         role.owner = player;
         if (role.isFollowing()) {
            CompoundTag nbt = new CompoundTag();
            activeCompanion.saveAsPassenger(nbt);
            activeCompanion.discard();
            EntityCustomNpc npc = new EntityCustomNpc(CustomEntities.entityCustomNpc, level);
            npc.readAdditionalSaveData(nbt);
            npc.setPos(player.getX(), player.getY(), player.getZ());
            setCompanion(npc);
            ((RoleCompanion) npc.role).setSitting(false);
            level.addFreshEntity(npc);
         }
      }
   }

   @Override
   public <T> @Nonnull LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
      return capability == PLAYERDATA_CAPABILITY ? instance.cast() : LazyOptional.empty();
   }

   public static void register(AttachCapabilitiesEvent<Entity> event) {
      if (event.getObject() instanceof Player) { event.addCapability(key, new PlayerData()); }
   }

   public synchronized void save(boolean update) {
      CustomNPCsScheduler.runTack(() -> {
         try {
            if (uuid.isEmpty()) { uuid = "noplayeruuid"; }
            if (name.isEmpty()) { name = "noplayername"; }
            File saveDir = CustomNpcs.getLevelSaveDirectory("playerdata/" + uuid);
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
      questData.clear();
      transportData.clear();
      factionData.clear();
      itemgiverData.clear();
      mailData.clear();
      timers.clear();
      game.clear();
      minimap.clear();
   }

   public static CompoundTag loadPlayerData(String uuid, String name) {
      if (name.isEmpty()) { name = "noplayername"; }
      File saveDir = CustomNpcs.getLevelSaveDirectory("playerdata/"+uuid);
      if (saveDir != null && (saveDir.exists() || saveDir.mkdirs())) {
         File file = new File(saveDir, name + ".json");
         File oldVersionFile = new File(saveDir.getParentFile(), uuid + ".json");
         if (!oldVersionFile.exists()) { oldVersionFile = new File(saveDir.getParentFile(), uuid + ".dat"); }
         if (!file.exists() && oldVersionFile.exists() && oldVersionFile.isFile()) {
            try {
               CompoundTag nbt = NBTJsonUtil.LoadFile(oldVersionFile);
               if (oldVersionFile.delete()) { NBTJsonUtil.SaveFile(file, nbt); }
               return nbt;
            }
            catch (Exception e) { LogWriter.error("Error old loading: " + oldVersionFile.getAbsolutePath(), e); }
            return new CompoundTag();
         }
         else if (file.exists() && file.isFile()) {
            try {
               if (!oldVersionFile.exists() || oldVersionFile.delete()) { return NBTJsonUtil.LoadFile(file); }
            }
            catch (Exception e) { LogWriter.error("Error loading: " + file.getAbsolutePath(), e); }
         }
      }
      return new CompoundTag();
   }

   public static @Nonnull PlayerData get(@Nullable Player player) {
      if (player == null || player.level().isClientSide()) {
         return ClientProxy.getPlayerData();
      }
      LazyOptional<PlayerData> liz = player.getCapability(PLAYERDATA_CAPABILITY, null);
      if (!liz.isPresent()) { LogWriter.warn("Hmmm. Why is a new \"PlayerData\" being created?"); }
      PlayerData data = liz.orElse(new PlayerData());
      if (data.player == null) {
         data.player = player;
         data.playerLevel = player.experienceLevel;
         data.animation = new DataAnimation(player);
         data.scriptData = new PlayerScriptData(player);
         data.setNBT(loadPlayerData(player.getUUID().toString(), player.getName().getString()));
      }
      return data;
   }

}
