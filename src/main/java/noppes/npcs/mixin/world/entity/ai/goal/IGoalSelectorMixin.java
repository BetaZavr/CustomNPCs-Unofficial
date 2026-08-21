package noppes.npcs.mixin.world.entity.ai.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.EnumSet;
import java.util.Map;

@Mixin(value = GoalSelector.class, priority = 502)
public interface IGoalSelectorMixin {

    @Accessor int getNewGoalRate();

    @Accessor Map<Goal.Flag, WrappedGoal> getLockedFlags();

    @Accessor EnumSet<Goal.Flag> getDisabledFlags();

}
