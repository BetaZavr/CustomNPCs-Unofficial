package noppes.npcs;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.AbortableIterationConsumer.Continuation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import noppes.npcs.controllers.SpawnController;
import noppes.npcs.controllers.data.SpawnData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.mixin.server.level.IChunkMapMixin;
import noppes.npcs.mixin.server.level.IServerLevelMixin;
import noppes.npcs.mixin.world.level.entity.IPersistentEntitySectionManagerMixin;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;

public class NPCSpawning {

   // Is called when the world has the ability to summon an entity
   public static void performLevelGenSpawning(ServerLevelAccessor level, Biome biome, int x, int z, RandomSource rand) {
      if (!(biome.getMobSettings().getCreatureProbability() >= 1.0F) && !(biome.getMobSettings().getCreatureProbability() < 0.0F) &&
              SpawnController.instance.hasSpawnList(level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome))) {
         int tries = 0;
         while(rand.nextFloat() < biome.getMobSettings().getCreatureProbability()) {
            ++tries;
            if (tries > 20) {
               break;
            }
            SpawnData data = SpawnController.instance.getRandomSpawnData(level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome));
            int size = 16;
            int j1 = x + rand.nextInt(size);
            int k1 = z + rand.nextInt(size);
            int l1 = j1;
            int i2 = k1;
            for(int k2 = 0; k2 < 4; ++k2) {
               BlockPos pos = getTopNonCollidingPos(level, j1, k1);
               if (canCreatureTypeSpawnAtLocation(data, level, pos)) {
                  if (spawnData(data, level, pos)) { break; }
               }
               else {
                  j1 += rand.nextInt(5) - rand.nextInt(5);
                  for(k1 += rand.nextInt(5) - rand.nextInt(5); j1 < x || j1 >= x + size || k1 < z || k1 >= z + size; k1 = i2 + rand.nextInt(5) - rand.nextInt(5)) {
                     j1 = l1 + rand.nextInt(5) - rand.nextInt(5);
                  }
               }
            }
         }
      }
   }

   // Called every tick
   @SuppressWarnings({"rawtypes", "unchecked"})
   public static void findChunksForSpawning(ServerLevel level) {
      if (!SpawnController.instance.data.isEmpty() && level.getGameTime() % 400L == 0L) {
         EntitySectionStorage sectionManager = ((IPersistentEntitySectionManagerMixin) ((IServerLevelMixin) level).getEntityManager()).getSectionStorage();
         ChunkMap chunkManager = level.getChunkSource().chunkMap;
         List<ChunkHolder> list = new ArrayList<>(((IChunkMapMixin) chunkManager).getVisibleChunkMap().values());
         Collections.shuffle(list);
         for (ChunkHolder chunkHolder : list) {
            LevelChunk levelchunk = chunkHolder.getTickingChunk();
            if (levelchunk == null) { break; }
            ChunkPos pos = levelchunk.getPos();
            Biome biome = level.getBiome(pos.getWorldPosition()).value();
            if (SpawnController.instance.hasSpawnList(level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome))) {
               AABB bb = new AABB(pos.getMinBlockX(), 0.0D, pos.getMinBlockZ(), pos.getMaxBlockX(), level.getMaxBuildHeight(), pos.getMaxBlockZ());
               List<Entity> entities = Lists.newArrayList();
               sectionManager.getEntities(EntityType.PLAYER, bb.inflate(4.0D), (e) -> {
                  if (e instanceof Entity entity) { entities.add(entity); }
                  return Continuation.CONTINUE;
               });
               if (entities.isEmpty()) {
                  sectionManager.getEntities(CustomEntities.entityCustomNpc, bb, (e) -> {
                     if (e instanceof Entity entity) { entities.add(entity); }
                     return Continuation.CONTINUE;
                  });
                  if (entities.size() < CustomNpcs.NpcNaturalSpawningChunkLimit) { spawnChunk(level, levelchunk); }
               }
            }
         }
      }
   }

   private static void spawnChunk(ServerLevel level, LevelChunk chunk) {
      BlockPos chunkposition = getChunk(level, chunk);
      int j1 = chunkposition.getX();
      int k1 = chunkposition.getY();
      int l1 = chunkposition.getZ();

      for(int i = 0; i < 3; ++i) {
         byte b1 = 6;
         int x = j1 + (level.random.nextInt(b1) - level.random.nextInt(b1));
         int z = l1 + (level.random.nextInt(b1) - level.random.nextInt(b1));
         BlockPos pos = new BlockPos(x, k1, z);
         ResourceLocation name = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(level.getBiome(pos).value());
         SpawnData data = SpawnController.instance.getRandomSpawnData(name);
         if (data != null && !data.getCompound().isEmpty() && canCreatureTypeSpawnAtLocation(data, level, pos)) {
            spawnData(data, level, pos);
         }
      }
   }

   private static BlockPos getChunk(Level level, LevelChunk chunk) {
      ChunkPos chunkpos = chunk.getPos();
      int i = chunkpos.getMinBlockX() + level.random.nextInt(16);
      int j = chunkpos.getMinBlockZ() + level.random.nextInt(16);
      int k = chunk.getHeight(Types.WORLD_SURFACE, i, j) + 1;
      int l = level.random.nextIntBetweenInclusive(-64, k + 1);
      return new BlockPos(i, l, j);
   }

   private static boolean spawnData(SpawnData data, ServerLevelAccessor level, BlockPos pos) {
      try {
         CompoundTag nbt = data.getCompound();
         if (!nbt.isEmpty()) {
            Entity entity = EntityType.create(nbt, level.getLevel()).orElse(null);
            if (entity instanceof Mob entityLiving) {
               if (entity instanceof EntityCustomNpc npc) {
                  npc.stats.spawnCycle = 4;
                  npc.stats.respawnTime = 0;
                  npc.ais.returnToStart = false;
                  npc.ais.setStartPos(pos);
               }
               entity.moveTo((double)pos.getX() + 0.5D, pos.getY(), (double)pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
               if (!ForgeEventFactory.checkSpawnPosition(entityLiving, level, MobSpawnType.NATURAL)) { return false; }
               CustomNPCsScheduler.runTack(() -> level.addFreshEntity(entityLiving));
               return true;
            }
            else { return false; }
         }
      }
      catch (Exception e) { LogWriter.error(e); }
      return false;
   }

   public static float getLightLevel(LevelReader level, BlockPos pos) {
      int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
      int skyLight = level.getBrightness(LightLayer.SKY, pos);
      int skyDarken = level.getSkyDarken();
      float skyLightValue = (11.0F - (float)skyDarken) * 15.0F / 11.0F;
      return Math.max((float)blockLight, (float)skyLight / 15.0F * skyLightValue);
   }

   @SuppressWarnings("deprecation")
   public static boolean canCreatureTypeSpawnAtLocation(SpawnData data, LevelReader level, BlockPos pos) {
      if (level.getWorldBorder().isWithinBounds(pos) && level.noCollision(CustomEntities.entityCustomNpc.getAABB(pos.getX(), pos.getY(), pos.getZ()))) {
         if (data.type == 1 && getLightLevel(level, pos) > 8.0F || data.type == 2 && getLightLevel(level, pos) <= 8.0F) {
            return false;
         } else {
            BlockState state = level.getBlockState(pos);
            Block block;
            if (data.liquid) {
               return state.liquid() && level.getBlockState(pos.below()).liquid() && !level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above());
            } else {
               BlockPos blockpos1 = pos.below();
               BlockState state1 = level.getBlockState(blockpos1);
               block = state1.getBlock();
               boolean flag = block != Blocks.BEDROCK && block != Blocks.BARRIER;
               BlockPos down = blockpos1.below();
               flag |= level.getBlockState(down).getBlock().isValidSpawn(level.getBlockState(down), level, down, Type.ON_GROUND, CustomEntities.entityCustomNpc);
               return flag && !state.isSignalSource() && !state.liquid() && !level.getBlockState(pos.above()).isSignalSource();
            }
         }
      }
      return false;
   }

   private static BlockPos getTopNonCollidingPos(LevelReader levelReader, int x, int z) {
      int i = levelReader.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      MutableBlockPos blockpos$mutable = new MutableBlockPos(x, i, z);
      if (levelReader.dimensionType().hasCeiling()) {
         do {
            blockpos$mutable.move(Direction.DOWN);
         } while(!levelReader.getBlockState(blockpos$mutable).isAir());

         do {
            blockpos$mutable.move(Direction.DOWN);
         } while(levelReader.getBlockState(blockpos$mutable).isAir() && blockpos$mutable.getY() > 0);
      }
      BlockPos blockpos = blockpos$mutable.below();
      return levelReader.getBlockState(blockpos).isPathfindable(levelReader, blockpos, PathComputationType.LAND) ? blockpos : blockpos$mutable.immutable();
   }

}
