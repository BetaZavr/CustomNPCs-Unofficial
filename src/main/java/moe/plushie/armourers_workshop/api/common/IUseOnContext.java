package moe.plushie.armourers_workshop.api.common;

import moe.plushie.armourers_workshop.api.core.utils.IDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface IUseOnContext {

    Level level();

    IDirection clickedFace();

    Object clickLocation();

    BlockPos clickedPos();

    Player player();

    Object hand();

    ItemStack itemInHand();

    @Nullable BlockHitResult hitResult();

}

