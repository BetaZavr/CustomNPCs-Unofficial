package noppes.npcs.mixin.world.level.storage;

import com.mojang.serialization.Lifecycle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.timers.TimerQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(value = PrimaryLevelData.class, priority = 502)
public interface IPrimaryLevelDataMixin {

    @Accessor int getPlayerDataVersion();

    @Mutable @Accessor void setPlayerDataVersion(int newPlayerDataVersion);

    @Accessor LevelSettings getSettings();

    @SuppressWarnings("deprecation")
    @Accessor PrimaryLevelData.SpecialWorldProperty getSpecialWorldProperty();

    @SuppressWarnings("deprecation")
    @Mutable @Accessor void setSpecialWorldProperty(PrimaryLevelData.SpecialWorldProperty value);

    @Mutable @Accessor void setWorldGenSettingsLifecycle(Lifecycle lifecycle);

    @Accessor CompoundTag getLoadedPlayerTag();

    @Accessor void setLoadedPlayerTag(CompoundTag player);

    @Mutable @Accessor void setVersion(int versionId);

    @Accessor Set<String> getKnownServerBrands();

    @Accessor void setWasModded(boolean newWasModded);

    @Accessor Set<String> getRemovedFeatureFlags();

    @Accessor TimerQueue<MinecraftServer> getScheduledEvents();

    @Accessor void setScheduledEvents(TimerQueue<MinecraftServer> newScheduledEvents);

}
