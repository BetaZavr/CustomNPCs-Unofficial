package noppes.npcs.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.blocks.tiles.TileBuilder;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.server.SPacketGuiOpen;

import javax.annotation.Nonnull;

public class BlockBuilder extends BlockInterface {

    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);

    public BlockBuilder() { super(Properties.copy(Blocks.BARRIER).sound(SoundType.STONE)); }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) { builder.add(ROTATION); }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) { return RenderShape.MODEL; }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                          @Nonnull InteractionHand hand, @Nonnull BlockHitResult ray) {
        if (!level.isClientSide) {
            ItemStack currentItem = player.getInventory().getSelected();
            if (currentItem.getItem() == CustomItems.wand || currentItem.getItem() == CustomBlocks.builder_item) {
                SPacketGuiOpen.sendOpenGui((ServerPlayer) player, EnumGuiType.BuilderBlock, null, pos);
            }

        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int rotation = Mth.floor((context.getRotation() / 90.0F) + 0.5D) & 3;
        if (!context.getLevel().isClientSide) {
            SPacketGuiOpen.sendOpenGui((ServerPlayer) context.getPlayer(), EnumGuiType.BuilderBlock, null, context.getClickedPos());
        }
        return this.defaultBlockState().setValue(ROTATION, rotation);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new TileBuilder(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return createTickerHelper(type, CustomBlocks.tile_builder, TileBuilder::tick);
    }

}
