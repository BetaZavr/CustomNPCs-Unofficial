package noppes.npcs.api.wrapper;

import com.google.common.collect.Lists;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.EventHooks;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IDimension;
import noppes.npcs.api.INbt;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IScoreboard;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.data.Data;
import noppes.npcs.controllers.PixelmonHelper;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityProjectile;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketPlaySound;
import noppes.npcs.shared.common.util.LogWriter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class WorldWrapper implements IWorld {

   public Level level;
   public IDimension dimension;

   private static final Data storeddata = new Data();
   private static final Data tempdata = new Data();
   @SuppressWarnings("deprecation")
   private final RandomSource rnd = RandomSource.createThreadSafe();

   private WorldWrapper(Level levelIn) {
      level = levelIn;
      dimension = new DimensionWrapper(levelIn.dimension().location(), levelIn.dimensionType());
   }

   public static void clearTempdata() { tempdata.clear(); }

   public Level getMCLevel() {
      return level;
   }

   public IEntity<?>[] getNearbyEntities(int x, int y, int z, double range, int type) {
      return getNearbyEntities(new BlockPosWrapper(level, new BlockPos(x, y, z)), range, type);
   }

   @SuppressWarnings("unchecked")
   public IEntity<?>[] getNearbyEntities(IPos pos, double range, int type) {
      AABB bb = (new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D)).move(pos.getMCBlockPos()).inflate(range, range, range);
      List<Entity> entities = level.getEntitiesOfClass((Class<Entity>) getClassForType(type), bb);
      List<IEntity<?>> list = new ArrayList<>();
      for (Entity living : entities) {
         list.add(Objects.requireNonNull(NpcAPI.Instance()).getIEntity(living));
      }
      return list.toArray(new IEntity<?>[0]);
   }

   public IEntity<?>[] getAllEntities(int type) {
      List<Entity> entities = getEntities(getClassForType(type), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
      List<IEntity<?>> list = new ArrayList<>();
       for (Entity living : entities) {
           list.add(Objects.requireNonNull(NpcAPI.Instance()).getIEntity(living));
       }
      return list.toArray(new IEntity<?>[0]);
   }

   public List<Entity> getEntities(Class<?> entityTypeIn, Predicate<? super Entity> predicateIn) {
      List<Entity> list = Lists.newArrayList();
      ChunkSource chunkSource = level.getChunkSource();
      Iterator<Entity> allEntities = getAllEntitiesInLevel();
      while(allEntities.hasNext()) {
         Entity entity = allEntities.next();
         if (entityTypeIn.isAssignableFrom(entity.getClass()) && chunkSource.hasChunk(Mth.floor(entity.getX()) >> 4, Mth.floor(entity.getZ()) >> 4) && predicateIn.test(entity)) {
            list.add(entity);
         }
      }
      return list;
   }

   public IEntity<?> getClosestEntity(int x, int y, int z, double range, int type) {
      return getClosestEntity(new BlockPosWrapper(level, new BlockPos(x, y, z)), range, type);
   }

   @SuppressWarnings("unchecked")
   public IEntity<?> getClosestEntity(IPos pos, double range, int type) {
      AABB bb = (new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D)).move(pos.getMCBlockPos()).inflate(range, range, range);
      Iterator<Entity> allEntities = level.getEntitiesOfClass((Class<Entity>) getClassForType(type), bb).iterator();
      double distance = range * range * range;
      Entity entity = null;
      while(allEntities.hasNext()) {
         Entity e = allEntities.next();
         double r = pos.getMCBlockPos().distSqr(e.blockPosition());
         if (entity == null) {
            distance = r;
            entity = e;
         } else if (r < distance) {
            distance = r;
            entity = e;
         }
      }
      return Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity);
   }

   public IEntity<?> getEntity(String uuid) {
      try {
         Entity e = getEntityInLevel(UUID.fromString(uuid));
         if (e == null) { e = level.getPlayerByUUID(UUID.fromString(uuid)); }
         return e == null ? null : Objects.requireNonNull(NpcAPI.Instance()).getIEntity(e);
      } catch (Exception var4) {
         throw new CustomNPCsException("Given uuid was invalid " + uuid);
      }
   }

   public IEntity<?> createEntityFromNBT(INbt nbt) {
      Entity entity = EntityType.create(nbt.getMCNBT(), level).orElse(null);
      if (entity == null) {
         throw new CustomNPCsException("Failed to create an entity from given NBT");
      } else {
         return Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity);
      }
   }

   public IEntity<?> createEntity(String id) {
      EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(id));
      if (type == null) { return null; }
      Entity entity = type.create(level);
      if (entity == null) {
         throw new CustomNPCsException("Failed to create an entity from given id: " + id);
      } else {
         entity.setPos(0.0D, 1.0D, 0.0D);
         return Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity);
      }
   }

   public IPlayer<?> getPlayer(String name) {
      Iterator<? extends Player> var2 = level.players().iterator();
      Player entityplayer;
      do {
         if (!var2.hasNext()) {
            return null;
         }
         entityplayer = var2.next();
      } while(!name.equals(entityplayer.getName().getString()));
      return (IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entityplayer);
   }

   private Class<?> getClassForType(int type) {
      return switch (type) {
         case 1 -> Player.class;
         case 2 -> EntityNPCInterface.class;
         case 3 -> Monster.class;
         case 4 -> Animal.class;
         case 5 -> LivingEntity.class;
         case 6 -> ItemEntity.class;
         case 7 -> EntityProjectile.class;
         case 8 -> PixelmonHelper.getPixelmonClass();
         case 9 -> Villager.class;
         case 10 -> AbstractArrow.class;
         case 11 -> ThrowableProjectile.class;
         default -> Entity.class;
      };
   }

   public long getTime() {
      return level.getDayTime();
   }

   public void setTime(long time) {
      if (level instanceof ServerLevel) {
         ((ServerLevel) level).setDayTime(time);
      }
      else if (level != null) { // ClientLevel
         Method setDayTime = null;
         try { setDayTime = level.getClass().getMethod("m_104746_", long.class); } catch (Exception ignored) {}
         if (setDayTime == null) {
            try { setDayTime = level.getClass().getMethod("setDayTime", long.class); } catch (Exception ignored) {}
         }
         if (setDayTime != null) {
            try {
               setDayTime.setAccessible(true);
               setDayTime.invoke(level, time);
            }
            catch (Exception ignored) {}
         }
      }
   }

   public long getTotalTime() {
      return level.getGameTime();
   }

   public IBlock getBlock(int x, int y, int z) {
      return Objects.requireNonNull(NpcAPI.Instance()).getIBlock(level, new BlockPos(x, y, z));
   }

   public IBlock getBlock(IPos pos) {
      return Objects.requireNonNull(NpcAPI.Instance()).getIBlock(level, pos.getMCBlockPos());
   }

   public boolean isChunkLoaded(int x, int z) {
      return level.getChunkSource().hasChunk(x >> 4, z >> 4);
   }

   public void setBlock(int x, int y, int z, String name) {
      setBlock(Objects.requireNonNull(NpcAPI.Instance()).getIPos(x, y, z), name);
   }

   public IBlock setBlock(IPos pos, String name) {
      Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse(name));
      if (block == null) {
         throw new CustomNPCsException("There is no such block: %s", name);
      } else {
         level.setBlock(pos.getMCBlockPos(), block.defaultBlockState(), 2);
         return Objects.requireNonNull(NpcAPI.Instance()).getIBlock(level, pos.getMCBlockPos());
      }
   }

   public void removeBlock(int x, int y, int z) {
      level.removeBlock(new BlockPos(x, y, z), false);
   }

   public void removeBlock(IPos pos) {
      level.removeBlock(pos.getMCBlockPos(), false);
   }

   public float getLightValue(int x, int y, int z) {
      return (float)level.getLightEmission(new BlockPos(x, y, z)) / 16.0F;
   }

   public IBlock getSpawnPoint() {
      return Objects.requireNonNull(NpcAPI.Instance()).getIBlock(level, level.getSharedSpawnPos());
   }

   public void setSpawnPoint(IBlock block) {
      ServerLevelData info = (ServerLevelData)level.getLevelData();
      info.setSpawn(new BlockPos(block.getX(), block.getY(), block.getZ()), 0.0F);
   }

   public boolean isDay() {
      return level.getDayTime() % 24000L < 12000L;
   }

   public boolean isRaining() {
      return level.getLevelData().isRaining();
   }

   public void setRaining(boolean bo) {
      ServerLevelData data = (ServerLevelData)level.getLevelData();
      if (bo) {
         data.setRaining(true);
         data.setRainTime(120000000);
      } else {
         data.setRaining(false);
         data.setRainTime(0);
      }

   }

   public void thunderStrike(double x, double y, double z) {
      LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
      if (bolt != null) {
         bolt.moveTo(x, y, z);
         bolt.setVisualOnly(false);
         level.addFreshEntity(bolt);
      }
   }

   public void spawnParticle(String particle, double x, double y, double z, double dx, double dy, double dz, double speed, int count) {
      ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(ResourceLocation.tryParse(particle));
      if (type == null) {
         throw new CustomNPCsException("Unknown particle type: " + particle);
      } else {
         if (level instanceof ServerLevel) {
            ((ServerLevel) level).sendParticles((ParticleOptions) type, x, y, z, count, dx, dy, dz, speed);
         }
         else if (level != null) { // ClientLevel
            if (count == 0) {
               double d0 = speed * dx;
               double d2 = speed * dy;
               double d4 = speed * dz;
               try {
                  level.addParticle((ParticleOptions) type, false, x, y, z, d0, d2, d4);
               }
               catch (Throwable t) { LogWriter.warn("Could not spawn particle effect " + type); }
            } else {
               for(int i = 0; i < count; ++i) {
                  double d1 = rnd.nextGaussian() * dx;
                  double d3 = rnd.nextGaussian() * dy;
                  double d5 = rnd.nextGaussian() * dz;
                  double d6 = rnd.nextGaussian() * speed;
                  double d7 = rnd.nextGaussian() * speed;
                  double d8 = rnd.nextGaussian() * speed;
                  try {
                     level.addParticle((ParticleOptions) type, false, x + d1, y + d3, z + d5, d6, d7, d8);
                  }
                  catch (Throwable t) { LogWriter.warn("Could not spawn particle effect " + type); }
               }
            }
         }
      }
   }

   public IData getTempdata() { return tempdata; }

   public IData getStoreddata() { return storeddata; }

   public static IData getTempData() { return tempdata; }

   public static IData getStoredData() { return storeddata; }

   public IItemStack createItem(String name, int size) {
      Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(name));
      if (item == null) {
         throw new CustomNPCsException("Unknown item id: " + name);
      } else {
         return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(new ItemStack(item, size));
      }
   }

   public IItemStack createItemFromNbt(INbt nbt) {
      ItemStack item = ItemStack.of(nbt.getMCNBT());
      if (item.isEmpty()) {
         throw new CustomNPCsException("Failed to create an item from given NBT");
      } else {
         return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(item);
      }
   }

   public void explode(double x, double y, double z, float range, boolean fire, boolean grief) {
      level.explode(null, x, y, z, range, fire, grief ? ExplosionInteraction.TNT : ExplosionInteraction.NONE);
   }

   public IPlayer<?>[] getAllPlayers() {
      return (IPlayer<?>[]) getAllEntities(1);
      /*List<ServerPlayer> list = Objects.requireNonNull(level.getServer()).getPlayerList().getPlayers();
      IPlayer<?>[] arr = new IPlayer[list.size()];
      for(int i = 0; i < list.size(); ++i) {
         arr[i] = (IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(list.get(i));
      }
      return arr;*/
   }

   public String getBiomeName(int x, int z) {
      try {
         Optional<ResourceKey<Biome>> t = level.getBiome(new BlockPos(x, 0, z)).unwrapKey();
         if (t.isPresent()) { return t.get().location().toString(); }
      }
      catch (Exception ignored) {}
      return "";
   }

   public IEntity<?> spawnClone(double x, double y, double z, int tab, String name) {
      return Objects.requireNonNull(NpcAPI.Instance()).getClones().spawn(x, y, z, tab, name, this);
   }

   public void spawnEntity(IEntity<?> entity) {
      if (entity == null) {
         throw new CustomNPCsException("Entity given was null");
      } else {
         Entity entityMC = entity.getMCEntity();
         List<Entity> entities = getEntities(getClassForType(-1), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
         for (Entity e : entities) {
            if (e.getUUID().equals(entityMC.getUUID())) {
               throw new CustomNPCsException("Entity with this UUID already exists");
            }
         }
         entityMC.setPos(entityMC.getX(), entityMC.getY(), entityMC.getZ());
         level.addFreshEntity(entityMC);
      }
   }

   public IEntity<?> getClone(int tab, String name) {
      return Objects.requireNonNull(NpcAPI.Instance()).getClones().get(tab, name, this);
   }

   public IScoreboard getScoreboard() {
      return new ScoreboardWrapper(Objects.requireNonNull(level.getServer()));
   }

   @SuppressWarnings("unchecked")
   public void broadcast(String message) {
      Component text = Component.literal(message);
      if (level instanceof ServerLevel serverLevel) {
         for (ServerPlayer serverPlayer : serverLevel.getPlayers((e) -> true)) {
            serverPlayer.sendSystemMessage(text);
         }
      }
      else { // ClientLevel
         Field players = null;
         try { players = level.getClass().getDeclaredField("f_104566_"); } catch (Exception ignored) {}
         if (players == null) {
            try { players = level.getClass().getDeclaredField("players"); } catch (Exception ignored) {}
         }
         if (players != null) {
            try {
               players.setAccessible(true);
               for (Object player : (List<Object>) players.get(level)) {
                  ((Player) player).sendSystemMessage(text);
               }
            }
            catch (Exception ignored) {}
         }
      }
   }

   public int getRedstonePower(int x, int y, int z) {
      return level.getDirectSignalTo(new BlockPos(x, y, z));
   }

   public static WorldWrapper createNew(Level level) { return new WorldWrapper(level); }

   public IDimension getDimension() { return dimension; }

   public String getName() {
      return ((ServerLevelData)level.getLevelData()).getLevelName();
   }

   public BlockPos getMCBlockPos(int x, int y, int z) {
      return new BlockPos(x, y, z);
   }

   public void playSoundAt(IPos pos, String sound, float volume, float pitch) {
      Packets.sendNearby(level, pos.getMCBlockPos(), 16, new PacketPlaySound(sound, SoundSource.PLAYERS, pos.getX(), pos.getY(), pos.getZ(), volume, pitch));
   }

   public void trigger(int id, Object... arguments) {
      EventHooks.onScriptTriggerEvent(ScriptController.Instance.forgeScripts, id, this, BlockPosWrapper.ZERO, null, arguments);
   }

   // New from Unofficial (BetaZavr)
   @SuppressWarnings("unchecked")
   private @Nonnull Iterator<Entity> getAllEntitiesInLevel() {
      if (level instanceof ServerLevel) { return ((ServerLevel) level).getEntities().getAll().iterator(); }
      else if (level != null) { // ClientLevel
         Method getEntities = null;
         try { getEntities = level.getClass().getMethod("m_142646_", long.class); } catch (Exception ignored) {}
         if (getEntities == null) {
            try { getEntities = level.getClass().getMethod("getEntities", long.class); } catch (Exception ignored) {}
         }
         if (getEntities != null) {
            try {
               getEntities.setAccessible(true);
               return ((LevelEntityGetter<Entity>) getEntities.invoke(level)).getAll().iterator();
            }
            catch (Exception ignored) {}
         }
      }
      return Collections.emptyIterator();
   }

   @SuppressWarnings("unchecked")
   private @Nullable Entity getEntityInLevel(UUID uuid) {
      if (level instanceof ServerLevel) {
         return ((ServerLevel) level).getEntity(uuid);
      }
      else if (level != null) { // ClientLevel

         Method method = null;
         try { method = level.getClass().getMethod("m_104735_", int.class); } catch (Exception ignored) {}
         if (method == null) {
            try { method = level.getClass().getMethod("entitiesForRendering", int.class); } catch (Exception ignored) {}
         }
         if (method != null) {
            try {
               method.setAccessible(true);
               for (Entity e : (Iterable<Entity>) method.invoke(level, uuid)) {
                  if (e != null && e.getUUID().equals(uuid)) {
                     return e;
                  }
               }
               return null;
            }
            catch (Exception ignored) {}
         }
      }
      return null;
   }

}
