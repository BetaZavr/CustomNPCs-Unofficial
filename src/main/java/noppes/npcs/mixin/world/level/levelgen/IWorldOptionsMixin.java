package noppes.npcs.mixin.world.level.levelgen;

import net.minecraft.world.level.levelgen.WorldOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = WorldOptions.class, priority = 502)
public interface IWorldOptionsMixin {

    @Mutable @Accessor void setSeed(long newSeed);

    @Mutable @Accessor void setGenerateStructures(boolean newGenerateStructures);

}
