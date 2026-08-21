package noppes.npcs.mixin.world.scores;

import java.util.Map;
import java.util.Set;

import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Scoreboard.class, priority = 502)
public interface IScoreboardMixin {

   @Accessor Map<String, Map<Objective, Score>> getPlayerScores();

}
