package noppes.npcs.items.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;

import javax.annotation.Nonnull;

public class CustomFishingRod extends FishingRodItem implements ICustomElement, Vanishable {

    protected final @Nonnull CompoundTag nbtData;
    protected final ItemStack repairItemStack;
    protected final int enchantability;
    protected final int fishingLineColor;
    protected final ResourceLocation fishingLineTexture;

    public CustomFishingRod(@Nonnull Item.Properties properties, @Nonnull CompoundTag nbtItem) {
        super(properties);
        nbtData = nbtItem;
        if (nbtItem.contains("Enchantability", 3) && nbtItem.getInt("Enchantability") > 0) { enchantability = nbtItem.getInt("Enchantability"); }
        else { enchantability = 1; }
        if (nbtItem.contains("RepairItem", 10)) { repairItemStack = ItemStack.of(nbtItem.getCompound("RepairItem")); }
        else { repairItemStack = null; }

        fishingLineColor = nbtData.contains("FishingLineColor", 3) ? nbtData.getInt("FishingLineColor") : 0;
        fishingLineTexture = nbtData.contains("FishingHookTexture", 8) ?
                new ResourceLocation(CustomNpcs.MODID, "textures/entity/" + nbtData.getString("FishingHookTexture") + ".png") : null;
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (player.fishing != null) {
            if (!level.isClientSide) {
                int i = player.fishing.retrieve(itemstack);
                itemstack.hurtAndBreak(i, player, (pl) -> pl.broadcastBreakEvent(hand));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        }
        else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            if (!level.isClientSide) {
                int speedBonus = EnchantmentHelper.getFishingSpeedBonus(itemstack) + nbtData.getInt("AddSpeedBonus");
                int luckBonus = EnchantmentHelper.getFishingLuckBonus(itemstack) + nbtData.getInt("AddLuckBonus");
                level.addFreshEntity(new FishingHook(player, level, luckBonus, speedBonus));
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            player.gameEvent(GameEvent.ITEM_INTERACT_START);
        }
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public int getEnchantmentValue() { return enchantability; }

    @Override
    public boolean isValidRepairItem(@Nonnull ItemStack armorStack, @Nonnull ItemStack repairStack) {
        if (repairItemStack != null) {
            return NoppesUtilPlayer.compareItems(repairItemStack, repairStack, false, false);
        }
        return super.isValidRepairItem(armorStack, repairStack);
    }

    @Override
    public String getCustomName() { return nbtData.getString("RegistryName"); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() {
        if (nbtData.contains("ItemType", 1)) { return nbtData.getByte("ItemType"); }
        return 8;
    }

    @Override
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

    public int getFishingLineColor() { return fishingLineColor; }

    public ResourceLocation getFishingHookTexture() { return fishingLineTexture; }

}
