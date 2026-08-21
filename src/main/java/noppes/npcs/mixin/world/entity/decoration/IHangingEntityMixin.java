package noppes.npcs.mixin.world.entity.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.HangingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = HangingEntity.class, priority = 502)
public interface IHangingEntityMixin {

    @Accessor BlockPos getPos();

    @Accessor void setDirection(Direction newDirection);

}
