package noppes.npcs.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.SoundActions;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.blocks.custom.CustomBlockLiquid;
import noppes.npcs.items.custom.CustomBottleItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;

// ForgeFlowingFluid
public abstract class CustomFluid extends FlowingFluid implements ICustomElement {

    protected final @Nonnull ResourceLocation location;
    protected final @Nonnull CompoundTag nbtData;
    protected final CustomFluidType fluidType;
    protected CustomFluid flowing;
    protected CustomFluid source;
    protected @Nullable BucketItem bucket;
    protected @Nullable CustomBottleItem bottle;
    protected @Nullable CustomBlockLiquid block;

    protected final int tickRate;
    protected final int slopeFindDistance;
    protected final int levelDecreasePerBlock;
    protected final float explosionResistance;

    protected CustomFluid(@Nonnull ResourceLocation locationIn, @Nonnull CompoundTag nbtBlock, @Nonnull CustomFluidType fluidTypeIn,
                          int slopeFindDistanceIn, int levelDecreasePerBlockIn, float explosionResistanceIn, int tickRateIn) {
        nbtData = nbtBlock;
        location = locationIn;
        fluidType = fluidTypeIn;
        slopeFindDistance = slopeFindDistanceIn;
        levelDecreasePerBlock = levelDecreasePerBlockIn;
        explosionResistance = explosionResistanceIn;
        tickRate = tickRateIn;
    }

    public @Nonnull ResourceLocation getLocation() { return location; }

    @Override
    public @Nonnull CustomFluidType getFluidType() { return fluidType; }

    @Override
    public @Nonnull Fluid getFlowing() { return flowing; }

    @Override
    public @Nonnull Fluid getSource() { return source; }

    @Override
    public @Nonnull Item getBucket() { return bucket != null ? bucket : Items.AIR; }

    public @Nonnull Item getBottle() { return bottle != null ? bottle : Items.AIR; }

    public void setLinks(@Nonnull CustomFluid sourceIn, @Nonnull CustomFluid fluidIn, @Nonnull CustomBlockLiquid blockIn,
                         @Nonnull BucketItem bucketIn, @Nonnull CustomBottleItem bottleIn) {
        if (source == null) { source = sourceIn; }
        if (flowing == null) { flowing = fluidIn; }
        if (block == null) { block = blockIn; }
        if (bucket == null) { bucket = bucketIn; }
        if (bottle == null) { bottle = bottleIn; }
    }

    @Override
    protected @Nonnull BlockState createLegacyBlock(@Nonnull FluidState state) {
        if (block != null) { return block.defaultBlockState().setValue(CustomBlockLiquid.LEVEL, getLegacyLevel(state)); }
        return Blocks.AIR.defaultBlockState();
    }

    public @Nullable CustomBlockLiquid getBlock() { return block; }

    @Override
    protected boolean canConvertToSource(@Nonnull Level level) { return false; }

    @Override
    public boolean canConvertToSource(@Nonnull FluidState state, @Nonnull Level level, @Nonnull BlockPos pos) {
        return getFluidType().canConvertToSource(state, level, pos);
    }

    @Override
    protected void beforeDestroyingBlock(@Nonnull LevelAccessor level, @Nonnull BlockPos pos, BlockState state) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockEntity);
    }

    @Override
    protected int getSlopeFindDistance(@Nonnull LevelReader level) { return slopeFindDistance; }

    @Override
    protected int getDropOff(@Nonnull LevelReader level) { return levelDecreasePerBlock; }

    @Override
    protected boolean canBeReplacedWith(@Nonnull FluidState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull Fluid fluidIn, @Nonnull Direction direction) {
        return direction == Direction.DOWN && !isSame(fluidIn);
    }

    @Override
    public int getTickDelay(@Nonnull LevelReader level) { return tickRate; }

    @Override
    protected float getExplosionResistance() { return explosionResistance; }

    @Override
    public boolean isSame(@Nonnull Fluid fluidIn) { return fluidIn == source || fluidIn == flowing; }

    @NotNull
    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.ofNullable(getFluidType().getSound(SoundActions.BUCKET_FILL));
    }

    @Override
    public String getCustomName() { return nbtData.getString("RegistryName"); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() {
        if (nbtData.contains("BlockType", 1)) { return nbtData.getByte("BlockType"); }
        return 1;
    }

    @Override
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

    public static class Flowing extends CustomFluid {

        public Flowing(@Nonnull ResourceLocation location, @Nonnull CompoundTag nbtBlock, @Nonnull CustomFluidType fluidTypeIn,
                       int slopeFindDistanceIn, int levelDecreasePerBlockIn, float explosionResistanceIn, int tickRateIn) {
            super(location, nbtBlock, fluidTypeIn, slopeFindDistanceIn, levelDecreasePerBlockIn, explosionResistanceIn, tickRateIn);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(@Nonnull StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) { return state.getValue(LEVEL); }

        @Override
        public boolean isSource(@Nonnull FluidState state) { return false; }

    }

    public static class Source extends CustomFluid {

        public Source(@Nonnull ResourceLocation location, @Nonnull CompoundTag nbtBlock, @Nonnull CustomFluidType fluidTypeIn,
                      int slopeFindDistanceIn, int levelDecreasePerBlockIn, float explosionResistanceIn, int tickRateIn) {
            super(location, nbtBlock, fluidTypeIn, slopeFindDistanceIn, levelDecreasePerBlockIn, explosionResistanceIn, tickRateIn);
        }

        @Override
        public int getAmount(@Nonnull FluidState state) { return 8; }

        @Override
        public boolean isSource(@Nonnull FluidState state) { return true; }

    }

}
