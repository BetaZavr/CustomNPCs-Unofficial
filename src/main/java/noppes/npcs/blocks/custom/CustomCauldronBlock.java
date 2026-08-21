package noppes.npcs.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.fluids.CustomFluid;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

public class CustomCauldronBlock extends AbstractCauldronBlock implements ICustomElement {

    private final @Nonnull CustomFluid fluid;
    private final @Nonnull CompoundTag nbtData;

    public CustomCauldronBlock(@Nonnull Properties property, @Nonnull Map<Item, CauldronInteraction> interactions,
                               @Nonnull CustomFluid fluidIn, @Nonnull CompoundTag nbtBlock) {
        super(property, interactions);
        nbtData = nbtBlock;
        fluid = fluidIn;
        SoundEvent fillingSound;
        SoundEvent emptySound;
        if (nbtData.contains("SoundBucketFill", 8)) {
            SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(nbtData.getString("SoundBucketFill")));
            fillingSound = Objects.requireNonNullElse(event, SoundEvents.BUCKET_FILL_LAVA);
        }
        else { fillingSound = SoundEvents.BUCKET_FILL_LAVA; }
        if (nbtData.contains("SoundBucketEmpty", 8)) {
            SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(nbtData.getString("SoundBucketEmpty")));
            emptySound = Objects.requireNonNullElse(event, SoundEvents.BUCKET_EMPTY_LAVA);
        }
        else { emptySound = SoundEvents.BUCKET_EMPTY_LAVA; }
        // 1. Taking liquid from the boiler (empty bucket)
        interactions.put(Items.BUCKET, (state, level, pos, player, hand, stack) -> {
                    if (!level.isClientSide) {
                        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(fluid.getBucket())));
                        player.awardStat(Stats.USE_CAULDRON);
                        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                        int value = state.getValue(LayeredCauldronBlock.LEVEL);
                        level.setBlockAndUpdate(pos, value == LayeredCauldronBlock.MIN_FILL_LEVEL ?
                                Blocks.CAULDRON.defaultBlockState() :
                                state.setValue(LayeredCauldronBlock.LEVEL, value - 1));
                        level.playSound(null, pos, fillingSound, SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                });
        // 2. FILLING the boiler (bucket with liquid → empty bucket)
        CauldronInteraction fillingBucket = (state, level, pos, player, hand, stack) -> {
            if (state.getBlock() == Blocks.CAULDRON || defaultBlockState().getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL) {
                int value = state.getBlock() == Blocks.CAULDRON ? 0 : state.getValue(LayeredCauldronBlock.LEVEL);
                if (!level.isClientSide && value < LayeredCauldronBlock.MAX_FILL_LEVEL) {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                    player.awardStat(Stats.FILL_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                    level.setBlockAndUpdate(pos, defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, value + 1));
                    level.playSound(null, pos, emptySound, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        };
        interactions.put(fluid.getBucket(), fillingBucket);
        CauldronInteraction.EMPTY.put(fluid.getBucket(), fillingBucket);
        // 3. Taking liquid from the boiler (empty bottle) → custom bottle
        interactions.put(Items.GLASS_BOTTLE, (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                LogWriter.info("[DEBUG] "+fluid.getBottle());
                ItemStack filledBottle = new ItemStack(fluid.getBottle());
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filledBottle));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                int value = state.getValue(LayeredCauldronBlock.LEVEL);
                BlockState blockstate = value == LayeredCauldronBlock.MIN_FILL_LEVEL ?
                        Blocks.CAULDRON.defaultBlockState() :
                        state.setValue(LayeredCauldronBlock.LEVEL, value - 1);
                level.setBlockAndUpdate(pos, blockstate);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        });
        // 4. FILLING the boiler (custom bottle → empty bottle)
        CauldronInteraction fillingBottle = (state, level, pos, player, hand, stack) -> {
            if (stack.getItem() != fluid.getBottle()) return InteractionResult.PASS;
            if (state.getBlock() == Blocks.CAULDRON || defaultBlockState().getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL) {
                int value = state.getBlock() == Blocks.CAULDRON ? 0 : state.getValue(LayeredCauldronBlock.LEVEL);
                if (!level.isClientSide && value < LayeredCauldronBlock.MAX_FILL_LEVEL) {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                    player.awardStat(Stats.FILL_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                    level.setBlockAndUpdate(pos, defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, value + 1));
                    level.playSound(null, pos, emptySound, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        };
        interactions.put(fluid.getBottle(), fillingBottle);
        CauldronInteraction.EMPTY.put(fluid.getBottle(), fillingBottle);

        registerDefaultState(stateDefinition.any().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MIN_FILL_LEVEL));
    }

    @Override
    public boolean isFull(BlockState state) { return state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL; }

    @Override
    protected boolean canReceiveStalactiteDrip(@Nonnull Fluid fluidIn) { return fluidIn == fluid; }

    @Override
    protected double getContentHeight(BlockState state) {
        return (6.0D + (double) state.getValue(LayeredCauldronBlock.LEVEL) * (double) LayeredCauldronBlock.MAX_FILL_LEVEL) / 16.0D;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Entity entityIn) {
        if (!level.isClientSide && entityIn.isOnFire() && isEntityInsideContent(state, pos, entityIn)) {
            entityIn.clearFire();
            if (entityIn.mayInteract(level, pos)) {
                lowerFillLevel(state, level, pos);
            }
        }
    }

    public static void lowerFillLevel(BlockState state, Level level, BlockPos pos) {
        int i = state.getValue(LayeredCauldronBlock.LEVEL) - LayeredCauldronBlock.MIN_FILL_LEVEL;
        BlockState blockstate = i == 0 ? Blocks.CAULDRON.defaultBlockState() : state.setValue(LayeredCauldronBlock.LEVEL, i);
        level.setBlockAndUpdate(pos, blockstate);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(blockstate));
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos) { return state.getValue(LayeredCauldronBlock.LEVEL); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateDefinition) { stateDefinition.add(LayeredCauldronBlock.LEVEL); }

    @Override
    protected void receiveStalactiteDrip(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Fluid fluidIn) {
        if (!isFull(state)) {
            BlockState blockstate = state.setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL) + 1);
            level.setBlockAndUpdate(pos, blockstate);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(blockstate));
            level.levelEvent(1047, pos, 0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(Items.CAULDRON);
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
    public boolean showInCreative() { return false; }

    public @Nonnull CustomFluid getFluid() { return fluid; }

}
