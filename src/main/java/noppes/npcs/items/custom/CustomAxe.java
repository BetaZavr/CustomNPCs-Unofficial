package noppes.npcs.items.custom;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class CustomAxe extends AxeItem implements ICustomElement {

    protected final @Nonnull CompoundTag nbtData;
    protected final Map<Block, Float> collectionBlocks = new HashMap<>();
    protected final Map<TagKey<Block>, Float> collectionBlockTags = new HashMap<>();

    public CustomAxe(@Nonnull Tier tier, @Nonnull Properties properties, @Nonnull CompoundTag nbtItem) {
        super(tier, 0, nbtItem.contains("SpeedAttack", 6) ? (float) nbtItem.getDouble("SpeedAttack") : -2.8f, properties);
        nbtData = nbtItem;

        if (nbtItem.contains("CollectionBlocks", 9)) {
            ListTag list = nbtItem.getList("CollectionBlocks", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag nbt = list.getCompound(i);
                if (nbt.contains("Name", 8)) {
                    Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(nbt.getString("Name")));
                    if (block != null && nbt.contains("Speed", 5)) {
                        collectionBlocks.put(block, nbt.getFloat("Speed"));
                    }
                }
            }
        }
        if (nbtItem.contains("CollectionBlockTags", 9)) {
            ListTag list = nbtItem.getList("CollectionBlockTags", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag nbt = list.getCompound(i);
                if (nbt.contains("Name", 8) && nbt.contains("Speed", 5)) {
                    collectionBlockTags.put(TagKey.create(Registries.BLOCK, new ResourceLocation(nbt.getString("Name"))),
                            nbt.getFloat("Speed"));
                }
            }
        }
    }

    @Override
    public float getDestroySpeed(@Nonnull ItemStack stack, @Nonnull BlockState state) {
        for (Map.Entry<Block, Float> entry : collectionBlocks.entrySet()) {
            if (state.is(entry.getKey())) {
                return entry.getValue();
            }
        }
        for (Map.Entry<TagKey<Block>, Float> entry : collectionBlockTags.entrySet()) {
            if (state.is(entry.getKey())) {
                return entry.getValue();
            }
        }
        // vanilla
        return super.getDestroySpeed(stack, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isCorrectToolForDrops(@Nonnull BlockState state) {
        for (Map.Entry<Block, Float> entry : collectionBlocks.entrySet()) {
            if (state.is(entry.getKey())) {
                return true;
            }
        }
        // vanilla
        return super.isCorrectToolForDrops(state);
    }

    @Override
    public String getCustomName() { return nbtData.getString("RegistryName"); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() {
        if (nbtData.contains("ItemType", 1)) { return nbtData.getByte("ItemType"); }
        return 2;
    }

    @Override
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

}
