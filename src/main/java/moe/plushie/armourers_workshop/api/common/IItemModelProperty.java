package moe.plushie.armourers_workshop.api.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface IItemModelProperty {

    float getValue(ItemStack var1, @Nullable Level var2, @Nullable LivingEntity var3, int var4);

}

