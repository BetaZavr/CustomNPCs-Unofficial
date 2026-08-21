package noppes.npcs.mixin.world.level;

import com.mojang.serialization.Lifecycle;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = LevelSettings.class, priority = 502)
public interface ILevelSettingsMixin {

    @Mutable @Accessor void setLevelName(String newLevelName);

    @Mutable @Accessor void setHardcore(boolean newHardcore);

    @Mutable @Accessor void setAllowCommands(boolean newAllowCommands);

    @Mutable @Accessor void setGameRules(GameRules newGameRules);

    @Mutable @Accessor(remap = false) void setLifecycle(Lifecycle newLifecycle);

    @Mutable @Accessor void setDataConfiguration(WorldDataConfiguration newDataConfiguration);

}
