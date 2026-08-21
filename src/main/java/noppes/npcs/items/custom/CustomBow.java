package noppes.npcs.items.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;

import javax.annotation.Nonnull;

public class CustomBow extends BowItem implements ICustomElement, Vanishable {

    protected final @Nonnull CompoundTag nbtData;
    protected final ItemStack repairItemStack;
    protected final int enchantability;

    protected final ItemStack itemArrow;
    protected final boolean isFlame;
    protected final float critChance;
    protected final double attackDamage;
    protected final float speed;

    public CustomBow(@Nonnull Item.Properties properties, @Nonnull CompoundTag nbtItem) {
        super(properties);
        nbtData = nbtItem;
        itemArrow = nbtItem.contains("Bullet", 10) ? ItemStack.of(nbtItem.getCompound("Bullet")) : ItemStack.EMPTY;
        isFlame = nbtItem.getBoolean("SetFlame");

        if (nbtItem.contains("Enchantability", 3) && nbtItem.getInt("Enchantability") > 0) { enchantability = nbtItem.getInt("Enchantability"); }
        else { enchantability = 1; }
        if (nbtItem.contains("RepairItem", 10)) { repairItemStack = ItemStack.of(nbtItem.getCompound("RepairItem")); }
        else { repairItemStack = null; }
        if (nbtItem.contains("CritChance", 5)) { critChance = nbtItem.getFloat("CritChance"); }
        else { critChance = 0.0f; }
        if (nbtItem.contains("EntityDamage", 6)) { attackDamage = nbtItem.getDouble("EntityDamage"); }
        else { attackDamage = 2.0d; }
        if (nbtItem.contains("DrawstringSpeed", 5)) { speed = nbtItem.getFloat("DrawstringSpeed"); }
        else { speed = 30.0f; }
    }

    @Override
    public void releaseUsing(@Nonnull ItemStack bowStack, @Nonnull Level level, @Nonnull LivingEntity entityIn, int timeLeft) {
        if (entityIn instanceof Player player) {
            boolean flag = player.getAbilities().instabuild || EnchantmentHelper.getTagEnchantmentLevel(Enchantments.INFINITY_ARROWS, bowStack) > 0;
            ItemStack itemstack = player.getProjectile(bowStack);

            int i = getUseDuration(bowStack) - timeLeft;
            i = ForgeEventFactory.onArrowLoose(bowStack, level, player, i, !itemstack.isEmpty() || flag);
            if (i < 0 || (itemstack.isEmpty() && !flag))  { return; }
            if (itemstack.isEmpty()) { itemstack = new ItemStack(Items.ARROW); }
            float f = getPowerForTime(i);
            if ((double) f < 0.1D) { return; }
            boolean flag1 = player.getAbilities().instabuild || (itemstack.getItem() instanceof ArrowItem &&
                    ((ArrowItem) itemstack.getItem()).isInfinite(itemstack, bowStack, player));
            if (!level.isClientSide) {
                ArrowItem arrowitem = (ArrowItem)(itemstack.getItem() instanceof ArrowItem ? itemstack.getItem() : Items.ARROW);
                AbstractArrow abstractarrow = arrowitem.createArrow(level, itemstack, player);
                abstractarrow = customArrow(abstractarrow);
                abstractarrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, f * 3.0F, 1.0F);
                // crit
                if (f == 1.0F) { abstractarrow.setCritArrow(!(critChance > 0.0f) || !(critChance <= 1.0f) || level.getRandom().nextFloat() < critChance); }
                // ench power
                int j = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.POWER_ARROWS, bowStack);
                double damage = (attackDamage > 0.0d ? attackDamage : abstractarrow.getBaseDamage()) * (i > 40 ? 1.0d : (double) i / 40.0d);
                abstractarrow.setBaseDamage(damage);
                if (j > 0) { abstractarrow.setBaseDamage(abstractarrow.getBaseDamage() + (double) j * 0.5D + 0.5D); }
                // ench punch
                int k = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.PUNCH_ARROWS, bowStack);
                if (k > 0) { abstractarrow.setKnockback(k); }
                // ench flame
                if (isFlame || EnchantmentHelper.getTagEnchantmentLevel(Enchantments.FLAMING_ARROWS, bowStack) > 0) { abstractarrow.setSecondsOnFire(100); }
                bowStack.hurtAndBreak(1, player, pl -> pl.broadcastBreakEvent(player.getUsedItemHand()));
                if (flag1 || player.getAbilities().instabuild && (itemstack.is(Items.SPECTRAL_ARROW) || itemstack.is(Items.TIPPED_ARROW))) {
                    abstractarrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }
                level.addFreshEntity(abstractarrow);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F);
            if (!flag1 && !player.getAbilities().instabuild) {
                itemstack.shrink(1);
                if (itemstack.isEmpty()) { player.getInventory().removeItem(itemstack); }
            }
            player.awardStat(Stats.ITEM_USED.get(this));
        }
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
        return 5;
    }

    @Override
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

    public float getSpeed() { return speed; }

}
