package noppes.npcs.blocks;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.EventHooks;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.server.SPacketGuiOpen;

public class BlockScripted
        extends BlockInterface {

   public static final VoxelShape AABB = Shapes.create(new AABB(0.001D, 0.001D, 0.001D, 0.998D, 0.998D, 0.998D));

   // New Unofficial (Goodbird)
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

   public BlockScripted() {
      super(Properties.copy(Blocks.STONE).sound(SoundType.STONE).strength(5.0F, 10.0F));
      registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
   }

   @Override
   public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return new TileScripted(pos, state); }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
      return AABB;
   }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull VoxelShape getCollisionShape(@Nonnull BlockState blockState, BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
      TileScripted tile = (TileScripted)level.getBlockEntity(pos);
      return tile != null && tile.isPassable ? Shapes.empty() : AABB;
   }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult ray) {
      if (level.isClientSide) { return InteractionResult.SUCCESS; }
      ItemStack currentItem = player.getInventory().getSelected();
      if (currentItem.getItem() != CustomItems.wand && currentItem.getItem() != CustomItems.scripter) {
         Vec3 vec = ray.getLocation();
         float x = (float)(vec.x - (double)pos.getX());
         float y = (float)(vec.y - (double)pos.getY());
         float z = (float)(vec.z - (double)pos.getZ());
         TileScripted tile = (TileScripted)level.getBlockEntity(pos);
         if (tile == null) { return InteractionResult.FAIL; }
         return EventHooks.onScriptBlockInteract(tile, player, ray.getDirection().get3DDataValue(), x, y, z) ? InteractionResult.FAIL : InteractionResult.SUCCESS;
      } else {
         PlayerData.get(player).scriptBlockPos = pos;
         SPacketGuiOpen.sendOpenGui((ServerPlayer) player, EnumGuiType.ScriptBlock, null, pos);
         return InteractionResult.SUCCESS;
      }
   }

   @Override
   public void setPlacedBy(Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity entity, @Nonnull ItemStack item) {
      if (!level.isClientSide && entity instanceof Player player) {
         PlayerData.get(player).scriptBlockPos = pos;
         SPacketGuiOpen.sendOpenGui((ServerPlayer) player, EnumGuiType.ScriptBlock, null, pos);
      }
   }

   @Override
   @SuppressWarnings("deprecation")
   public void entityInside(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Entity entityIn) {
      if (!level.isClientSide) {
         TileScripted tile = (TileScripted)level.getBlockEntity(pos);
         if (tile != null) { EventHooks.onScriptBlockCollide(tile, entityIn); }
      }
   }

   @Override
   public void handlePrecipitation(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Precipitation type) {
      if (!level.isClientSide && type == Precipitation.RAIN) {
         TileScripted tile = (TileScripted)level.getBlockEntity(pos);
         if (tile != null) { EventHooks.onScriptBlockRainFill(tile); }
      }
   }

   @Override
   public void fallOn(Level level, @Nonnull BlockState state, @Nonnull BlockPos pos, @Nonnull Entity entity, float fallDistance) {
      if (!level.isClientSide) {
         TileScripted tile = (TileScripted)level.getBlockEntity(pos);
         if (tile != null) { fallDistance = EventHooks.onScriptBlockFallenUpon(tile, entity, fallDistance); }
         super.fallOn(level, state, pos, entity, fallDistance);
      }
   }

   @Override
   @SuppressWarnings("deprecation")
   public void attack(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player player) {
      if (!level.isClientSide) {
         TileScripted tile = (TileScripted)level.getBlockEntity(pos);
         if (tile != null) { EventHooks.onScriptBlockClicked(tile, player); }
      }
   }

   @Override
   @SuppressWarnings("deprecation")
   public void onRemove(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
      if (!level.isClientSide) {
         TileScripted tile = (TileScripted)level.getBlockEntity(pos);
         if (tile != null) { EventHooks.onScriptBlockBreak(tile); }
      }
      super.onRemove(state, level, pos, newState, isMoving);
   }

   @Override
   public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
      if (!level.isClientSide) {
         TileScripted tile = (TileScripted)level.getBlockEntity(pos);
         if (tile != null && EventHooks.onScriptBlockHarvest(tile, player)) {
            return false;
         }
      }
      return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
   }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull Builder builder) {
      return Collections.emptyList();
   }

   @Override
   public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
      if (!level.isClientSide) {
         TileScripted tile = (TileScripted)level.getBlockEntity(pos);
         if (tile != null && EventHooks.onScriptBlockExploded(tile)) {
            return;
         }
      }

      super.onBlockExploded(state, level, pos, explosion);
   }

   @Override
   @SuppressWarnings("deprecation")
   public void neighborChanged(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Block neighborBlock, @Nonnull BlockPos pos2, boolean isMoving) {
      if (!level.isClientSide) {
         TileScripted tile = (TileScripted)level.getBlockEntity(pos);
         if (tile == null) { return; }
         EventHooks.onScriptBlockNeighborChanged(tile, pos2);
         int power = 0;
         for (Direction direction : Direction.values()) {
            int p = level.getSignal(pos.relative(direction), direction);
            if (p > power) { power = p; }
         }
         if (tile.prevPower != power && tile.powering <= 0) {
            tile.newPower = power;
         }
      }
   }

   @Override
   @SuppressWarnings("deprecation")
   public boolean isSignalSource(@Nonnull BlockState state) { return true; }

   @Override
   @SuppressWarnings("deprecation")
   public int getSignal(@Nonnull BlockState state, @Nonnull BlockGetter worldIn, @Nonnull BlockPos pos, @Nonnull Direction side) {
      return this.getDirectSignal(state, worldIn, pos, side);
   }

   @Override
   @SuppressWarnings("deprecation")
   public int getDirectSignal(@Nonnull BlockState state, BlockGetter level, @Nonnull BlockPos pos, @Nonnull Direction side) {
      TileScripted tile = (TileScripted)level.getBlockEntity(pos);
      return tile != null ? tile.activePowering : 0;
   }

   @Override
   public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
      TileScripted tile = (TileScripted)level.getBlockEntity(pos);
      return tile != null && tile.isLadder;
   }

   @Override
   public boolean isValidSpawn(BlockState state, BlockGetter level, BlockPos pos, Type type, @Nullable EntityType<?> entityType) {
      return true;
   }

   @Override
   public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
      TileScripted tile = (TileScripted)level.getBlockEntity(pos);
      return tile == null ? 0 : tile.lightValue;
   }

   @Override
   @SuppressWarnings("deprecation")
   public boolean isPathfindable(@Nonnull BlockState state, BlockGetter level, @Nonnull BlockPos pos, @Nonnull PathComputationType type) {
      TileScripted tile = (TileScripted)level.getBlockEntity(pos);
      return tile != null && tile.isPassable;
   }

   @Override
   public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
      return super.canEntityDestroy(state, level, pos, entity);
   }

   @Override
   @SuppressWarnings("deprecation")
   public float getDestroyProgress(@Nonnull BlockState state, @Nonnull Player player, BlockGetter level, @Nonnull BlockPos pos) {
      TileScripted tile = (TileScripted)level.getBlockEntity(pos);
      float f = -1.0F;
      if (tile != null) {
         f = tile.blockHardness;
      }

      if (f == -1.0F) {
         return 0.0F;
      } else {
         int i = ForgeHooks.isCorrectToolForDrops(state, player) ? 30 : 100;
         return player.getDigSpeed(state, pos) / f / (float)i;
      }
   }

   @Override
   public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
      TileScripted tile = (TileScripted)level.getBlockEntity(pos);
      return tile != null ? tile.blockResistance : 0.0F;
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
      return createTickerHelper(type, CustomBlocks.tile_scripted, TileScripted::tick);
   }

   // New Unofficial (Goodbird)
   @Override
   protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> p_54447_) {
      p_54447_.add(WATERLOGGED);
   }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull FluidState getFluidState(@Nonnull BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull BlockState updateShape(BlockState state_0, @Nonnull Direction side, @Nonnull BlockState state_1, @Nonnull LevelAccessor level, @Nonnull BlockPos pos_0, @Nonnull BlockPos pos_1) {
      if (state_0.getValue(WATERLOGGED)) {
         level.scheduleTick(pos_0, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }
      return state_0;
   }

   @Override
   public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }

}
