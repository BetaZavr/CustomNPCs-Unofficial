package noppes.npcs.mixin.server;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.concurrent.Executor;

@Mixin(value = MinecraftServer.class, priority = 502)
public interface IMinecraftServerMixin {

    @Accessor("levels")
    Map<ResourceKey<Level>, ServerLevel> getLevels();

    @Accessor("storageSource")
    LevelStorageSource.LevelStorageAccess getStorageSource();

    @Accessor("executor")
    Executor getExecutor();
}