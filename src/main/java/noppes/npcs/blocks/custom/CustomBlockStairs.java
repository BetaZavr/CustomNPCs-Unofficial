package noppes.npcs.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;

import javax.annotation.Nonnull;

public class CustomBlockStairs extends StairBlock implements ICustomElement {

    protected final @Nonnull CompoundTag nbtData;

    public CustomBlockStairs(@Nonnull BlockState statePlanks, @Nonnull Properties properties, @Nonnull CompoundTag nbtBlock) {
        super(() -> statePlanks, properties);
        nbtData = nbtBlock;
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
    public String getCustomName() { return nbtData.getString("RegistryName"); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() {
        if (nbtData.contains("BlockType", 1)) { return nbtData.getByte("BlockType"); }
        return 3;
    }

    @Override
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

}
