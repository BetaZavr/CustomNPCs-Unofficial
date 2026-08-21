package noppes.npcs.items.custom;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.fluids.CustomFluid;

import javax.annotation.Nonnull;

public class CustomBottleItem extends Item implements ICustomElement {

    private final CustomFluid fluid;

    public CustomBottleItem(@Nonnull CustomFluid fluidIn, @Nonnull Properties properties) {
        super(properties);
        fluid = fluidIn;
    }

    @Override
    public @Nonnull ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (stack.isEmpty()) { return bottle; }
            if (!player.getInventory().add(bottle)) { player.drop(bottle, false); }
        }
        return stack;
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack) { return 32; }

    @Override
    public @Nonnull UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    public CustomFluid getFluid() { return fluid; }

    @Override
    public String getCustomName() { return fluid.getCustomName() + "_bottle"; }

    @Override
    public INbt getCustomNbt() { return fluid.getCustomNbt(); }

    @Override
    public int getElementType() { return fluid.getElementType(); }

    @Override
    public boolean showInCreative() { return fluid.showInCreative(); }

}