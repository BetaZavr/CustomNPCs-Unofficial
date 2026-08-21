package noppes.npcs.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import noppes.npcs.CustomBlocks;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.mixin.world.level.block.state.properties.IIntegerPropertyMixin;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class CustomBlock extends Block implements ICustomElement {

    public VoxelShape FULL_BLOCK_AABB = Shapes.create(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));
    public VoxelShape EAST_BLOCK_AABB = Shapes.create(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));
    public VoxelShape SOUTH_BLOCK_AABB = Shapes.create(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));
    public VoxelShape WEST_BLOCK_AABB = Shapes.create(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));

    public DirectionProperty FACING;
    public IntegerProperty INT;
    public BooleanProperty BO;

    protected final @Nonnull CompoundTag nbtData;

    public CustomBlock(@Nonnull Properties property, @Nonnull CompoundTag nbtBlock) {
        super(property);
        nbtData = nbtBlock;

        if (nbtBlock.get("AABB") instanceof ListTag tagList && tagList.getElementType() == (byte) 6 && tagList.size() > 5) {
            double[] v = new double[] { 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D };
            for (int i = 0; i < 6; i++) {
                double s = i < 3 ? 0.0d : 1.0d;
                if (i < tagList.size()) { s = tagList.getDouble(i); }
                v[i] = s;
            }
            FULL_BLOCK_AABB = Shapes.create(new AABB(v[0], v[1], v[2], v[3], v[4], v[5]));
            WEST_BLOCK_AABB = Shapes.create(new AABB(v[2], v[1], 1 - v[3], v[5], v[4], 1 - v[0]));
            SOUTH_BLOCK_AABB = Shapes.create(new AABB(1 - v[3], v[1], 1 - v[5], 1 - v[0], v[4], 1 - v[2]));
            EAST_BLOCK_AABB = Shapes.create(new AABB(1 - v[5], v[1], v[0], 1 - v[2], v[4], v[3]));
        }
        if (BO != null) { registerDefaultState(stateDefinition.any().setValue(BO, false)); }
        else if (INT != null) { registerDefaultState(stateDefinition.any().setValue(INT, ((IIntegerPropertyMixin) INT).getMin())); }
        else if (FACING != null) { registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    }

    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity entity, @Nonnull ItemStack item) {
        if (FACING != null && entity != null) {
            level.setBlock(pos, state.setValue(FACING, entity.getDirection()), 2);
        }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        if (FACING != null) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }
        return defaultBlockState();
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rotation) {
        if (FACING != null) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirror) {
        if (FACING != null) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        if (FACING != null) {
            return switch (state.getValue(FACING)) {
                case EAST -> EAST_BLOCK_AABB;
                case SOUTH -> SOUTH_BLOCK_AABB;
                case WEST -> WEST_BLOCK_AABB;
                default -> FULL_BLOCK_AABB;
            };
        }
        return FULL_BLOCK_AABB;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull VoxelShape getCollisionShape(@Nonnull BlockState blockState, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        if (nbtData.getBoolean("IsPassable")) { return Shapes.empty(); }
        if (FACING != null) {
            return switch (blockState.getValue(FACING)) {
                case EAST -> EAST_BLOCK_AABB;
                case SOUTH -> SOUTH_BLOCK_AABB;
                case WEST -> WEST_BLOCK_AABB;
                default -> FULL_BLOCK_AABB;
            };
        }
        return super.getCollisionShape(blockState, level, pos, context);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return nbtData.contains("IsLadder", 1) ? nbtData.getBoolean("IsLadder") : super.isLadder(state, level, pos, entity);
    }

    @Override
    public boolean isValidSpawn(BlockState state, BlockGetter level, BlockPos pos, SpawnPlacements.Type type, @javax.annotation.Nullable EntityType<?> entityType) {
        return nbtData.contains("IsValidSpawn", 1) ? nbtData.getBoolean("IsValidSpawn") : super.isValidSpawn(state, level, pos, type, entityType);
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> stateDefinition) {
        if (CustomBlocks.registryNbt.contains("Property", 10)) {
            CompoundTag nbtProperty = CustomBlocks.registryNbt.getCompound("Property");
            switch (nbtProperty.getByte("Type")) {
                case (byte) 1: {
                    BO = BooleanProperty.create(nbtProperty.getString("Name"));
                    stateDefinition.add(BO);
                    break;
                }
                case (byte) 3: {
                    INT = IntegerProperty.create(nbtProperty.getString("Name"), nbtProperty.getInt("Min"), nbtProperty.getInt("Max"));
                    stateDefinition.add(INT);
                    break;
                }
                case (byte) 4: {
                    FACING = DirectionProperty.create(nbtProperty.getString("Name"), Direction.Plane.HORIZONTAL);
                    stateDefinition.add(FACING);
                    break;
                }
            }
        }
    }

    public boolean hasProperty() { return BO != null || INT != null || FACING != null; }

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
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

}
