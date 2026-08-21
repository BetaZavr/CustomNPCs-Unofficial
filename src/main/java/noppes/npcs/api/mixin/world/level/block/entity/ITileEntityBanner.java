package noppes.npcs.api.mixin.world.level.block.entity;

import net.minecraft.resources.ResourceLocation;

public interface ITileEntityBanner {

    int npcs$getFactionId();

    void npcs$setFactionId(int newFactionId);

    ResourceLocation npcs$getResourceFlag();

    @SuppressWarnings("unused")
    void npcs$setResourceFlag(ResourceLocation newFactionId);

}
