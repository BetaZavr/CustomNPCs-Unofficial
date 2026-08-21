package noppes.npcs.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import noppes.npcs.CustomBlocks;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.blocks.BlockInterface;
import noppes.npcs.blocks.custom.tiles.CustomTileEntityChest;
import noppes.npcs.containers.ContainerChestCustom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CustomChest extends BlockInterface implements SimpleWaterloggedBlock, ICustomElement, EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected static final VoxelShape CHEST_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    protected static VoxelShape SHAPE = Shapes.block();

    protected final @Nonnull CompoundTag nbtData;
    public final boolean isChest;

    public CustomChest(@Nonnull Properties properties, @Nonnull CompoundTag nbtBlock) {
        super(properties);
        nbtData = nbtBlock;
        isChest = nbtBlock.contains("IsChest", 1) && nbtBlock.getBoolean("IsChest");

        if (nbtBlock.get("AABB") instanceof ListTag tagList && tagList.getElementType() == (byte) 6 && tagList.size() > 5) {
            SHAPE = Shapes.create(new AABB(tagList.getDouble(0), tagList.getDouble(1), tagList.getDouble(2),
                    tagList.getDouble(3), tagList.getDouble(4), tagList.getDouble(5)));
        }
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.FALSE));
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return nbtData.contains("IsLadder", 1) ? nbtData.getBoolean("IsLadder") : super.isLadder(state, level, pos, entity);
    }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) { return RenderShape.MODEL; }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState nextStage, @Nonnull LevelAccessor level, @Nonnull BlockPos pos, @Nonnull BlockPos nextPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        if (isChest) {
            return CHEST_SHAPE;
        }
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection().getOpposite();
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState().setValue(FACING, direction).setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull FluidState getFluidState(@Nonnull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity placer, @Nonnull ItemStack item) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CustomTileEntityChest tile) {
            if (item.hasTag() && item.getTag() != null && item.getTag().contains("BlockEntityTag")) {
                tile.load(item.getTag().getCompound("BlockEntityTag"));
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState nextState, boolean isMoving) {
        if (!state.is(nextState.getBlock())) {
            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity instanceof net.minecraft.world.Container container) {
                Containers.dropContents(level, pos, container);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, nextState, isMoving);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        BlockEntity tile = level.getBlockEntity(pos);
        if (!(tile instanceof CustomTileEntityChest chest)) {
            return InteractionResult.CONSUME;
        }

        // Check lock
        if (chest.isLocked() && !chest.canUnlock(player)) {
            return InteractionResult.CONSUME;
        }

        // Open GUI using NetworkHooks to pass BlockPos to client
        MenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, playerInventory, playerEntity) -> new ContainerChestCustom(containerId, playerInventory, chest),
                chest.getDisplayName()
        );
        net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, menuProvider, pos);

        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CustomTileEntityChest chest) {
            return new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new ContainerChestCustom(containerId, playerInventory, chest),
                    chest.getDisplayName()
            );
        }
        return null;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new CustomTileEntityChest(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return level.isClientSide() && isChest ?
                createTickerHelper(type, CustomBlocks.tile_custom_chest, CustomTileEntityChest::clientTick) :
                null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateDefinition) {
        stateDefinition.add(FACING, WATERLOGGED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(@Nonnull BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof net.minecraft.world.Container container) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer(container);
        }
        return 0;
    }

    @Override
    public String getCustomName() { return nbtData.getString("RegistryName"); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() {
        if (nbtData.contains("BlockType", 1)) { return nbtData.getByte("BlockType"); }
        return 2;
    }

    @Override
    public boolean showInCreative() {
        return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative");
    }

}