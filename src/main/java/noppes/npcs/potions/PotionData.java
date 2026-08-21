package noppes.npcs.potions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;

public class PotionData implements ICustomElement {

    public final @Nonnull ResourceLocation location;
    public final @Nonnull CompoundTag nbtData;

    public final @Nonnull CustomMobEffect EFFECT;
    public final @Nonnull CustomPotion POTION;
    public final @Nonnull CustomPotion LONG;
    public final @Nonnull CustomPotion STRONG;

    public PotionData(@Nonnull ResourceLocation locationIn, @Nonnull CompoundTag nbtPotionIn) {
        location = locationIn;
        nbtData = nbtPotionIn;
        EFFECT = new CustomMobEffect(getMobEffectCategory(nbtData), location, nbtData);

        int delay = nbtData.getBoolean("IsInstant") ? 0 :
                nbtData.contains("BaseDelay", 3) ? nbtData.getInt("BaseDelay") : 200;
        POTION = new CustomPotion(location.getPath(), nbtData, new CustomMobEffectInstance(EFFECT, ValueUtil.correctInt(delay, 20, Integer.MAX_VALUE), location, nbtData));
        LONG = new CustomPotion(location.getPath(), nbtData, new CustomMobEffectInstance(EFFECT, ValueUtil.correctInt(delay * 3, 60, Integer.MAX_VALUE), location, nbtData));
        STRONG = new CustomPotion(location.getPath(), nbtData, new CustomMobEffectInstance(EFFECT, ValueUtil.correctInt(delay / 2, 20, Integer.MAX_VALUE), location, nbtData));

        /*
        // PotionBrewing
        addMix(Potions.AWKWARD, Items.BLAZE_POWDER, POTION);
        addMix(POTION, Items.REDSTONE, LONG);
        addMix(POTION, Items.GLOWSTONE_DUST, STRONG);
        */
    }

    private MobEffectCategory getMobEffectCategory(CompoundTag nbtPotion) {
        return switch (nbtPotion.getString("Category").toLowerCase()) {
            case "beneficial" -> MobEffectCategory.BENEFICIAL;
            case "harmful" -> MobEffectCategory.HARMFUL;
            default -> MobEffectCategory.NEUTRAL;
        };
    }

    @Override
    public String getCustomName() { return nbtData.getString("RegistryName"); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() {
        if (nbtData.contains("ItemType", 1)) { return nbtData.getByte("ItemType"); }
        return 7;
    }

    @Override
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

}
