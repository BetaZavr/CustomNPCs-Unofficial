package noppes.npcs.mixin.world.item.trading;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MerchantOffer.class, priority = 499)
public interface IMerchantOfferMixin {

    @Mutable @Accessor void setBaseCostA(ItemStack newBaseCostA);

    @Mutable @Accessor void setCostB(ItemStack newCostA);

    @Mutable @Accessor void setResult(ItemStack newResult);

}
