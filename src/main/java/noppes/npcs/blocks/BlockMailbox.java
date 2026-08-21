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
import noppes.npcs.CustomNpcs;
import noppes.npcs.blocks.tiles.TileMailbox;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiOpen;

import javax.annotation.Nonnull;

public class BlockMailbox extends BlockInterface {

   public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);
   public final int type;

   public BlockMailbox(int type) {
      super(Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).strength(5.0F, 10.0F));
      this.type = type;
   }

   @Override
   public @Nonnull String getDescriptionId() { return "block." + CustomNpcs.MODID + ".npcmailbox"; }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull VoxelShape getOcclusionShape(@Nonnull BlockState state, @Nonnull BlockGetter getter, @Nonnull BlockPos pos) {
      return Shapes.empty();
   }

   @Override
   @SuppressWarnings("deprecation")
   public boolean isPathfindable(@Nonnull BlockState state, @Nonnull BlockGetter getter, @Nonnull BlockPos pos, @Nonnull PathComputationType pathType) {
      return false;
   }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player playerIn,
                                         @Nonnull InteractionHand hand, @Nonnull BlockHitResult ray) {
      if (!level.isClientSide && playerIn instanceof ServerPlayer player) {
         Packets.send(player, new PacketGuiOpen(EnumGuiType.PlayerMailbox, pos));
      }
      return InteractionResult.SUCCESS;
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) { builder.add(ROTATION); }

   @Override
   public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
      return this.defaultBlockState().setValue(ROTATION, (Mth.floor((context.getRotation() * 4.0F / 360.0F) + 0.5F) & 3) % 4);
   }

   @Override
   public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
      return (new TileMailbox(pos, state)).setModel(type);
   }

}
