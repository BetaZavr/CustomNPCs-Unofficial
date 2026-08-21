package noppes.npcs.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.blocks.tiles.TileBlockAnvil;
import noppes.npcs.constants.EnumGuiType;
import org.jetbrains.annotations.NotNull;

public class BlockCarpentryBench extends BlockInterface {

   public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);

   public BlockCarpentryBench() {
      super(Properties.copy(Blocks.CRAFTING_TABLE).sound(SoundType.WOOD).strength(5.0F, 10.0F));
   }

   @Override
   @SuppressWarnings("deprecation")
   public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player,
                                         @NotNull InteractionHand hand, @NotNull BlockHitResult ray) {
      if (!level.isClientSide) {
         NoppesUtilServer.openContainerGui((ServerPlayer)player, EnumGuiType.PlayerAnvil, (buffer) -> buffer.writeBlockPos(pos));
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   @SuppressWarnings("deprecation")
   public @NotNull VoxelShape getOcclusionShape(@NotNull BlockState state, @NotNull BlockGetter getter, @NotNull BlockPos pos) {
      return Shapes.empty();
   }

   @Override
   @SuppressWarnings("deprecation")
   public boolean isPathfindable(@NotNull BlockState state, @NotNull BlockGetter getter, @NotNull BlockPos pos, @NotNull PathComputationType pathType) {
      return false;
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(ROTATION);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return this.defaultBlockState().setValue(ROTATION, Mth.floor((context.getRotation() / 90.0F) + 0.5D) & 3);
   }

   @Override
   public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
      return new TileBlockAnvil(pos, state);
   }

}
