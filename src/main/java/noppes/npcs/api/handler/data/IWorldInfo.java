package noppes.npcs.api.handler.data;

import com.mojang.serialization.Lifecycle;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import noppes.npcs.api.INbt;
import noppes.npcs.api.interfaces.ParamName;

import java.util.UUID;

@SuppressWarnings("unused")
public interface IWorldInfo {

    boolean getMCConfirmedExperimentalWarning();

    void setMCConfirmedExperimentalWarning(boolean confirmedExperimentalSettings);

    boolean getMCWasModded();

    void setMCWasModded(boolean wasModded);

    String[] getMCKnownServerBrands();

    void setMCKnownServerBrands(String[] known);

    String[] getMCRemovedFeatureFlags();

    void setMCRemovedFeatureFlags(String[] flags);

    UUID getMCWanderingTraderId();

    void setMCWanderingTraderId(UUID uuid);

    INbt getMCCustomBossEvents();

    void setMCCustomBossEvents(INbt nbt);

    EndDragonFight.Data getMCEndDragonFightData();

    void setMCEndDragonFightData(EndDragonFight.Data data);

    WorldBorder.Settings getMCWorldBorder();

    void setMCWorldBorder(WorldBorder.Settings worldBorder);

    int getMCClearWeatherTime();

    void setMCClearWeatherTime(int time);

    Lifecycle getMCWorldGenSettingsLifecycle();

    void setMCWorldGenSettingsLifecycle(boolean isStable);

    Lifecycle getMCLifecycle();

    void setMCLifecycle(String lifecycle);

    int getMCSpecialWorldProperty();

    void setMCSpecialWorldProperty(int type);

    int getMCPlayerDataVersion();

    void setMCPlayerDataVersion(int id);

    String getId();

    INbt getNbt();

    void setNbt(@ParamName("nbt") INbt nbt);

    int getMCWanderingTraderSpawnDelay();

    void setMCWanderingTraderSpawnDelay(int delay);

    int getMCWanderingTraderSpawnChance();

    void setMCWanderingTraderSpawnChance(int chance);

    GameRules getMCGameRules();

    long getMCSeed();

    void setMCSeed(long seed);

    String getMCLevelName();

    void setMCLevelName(String name);

    int getMCSpawnX();

    void setMCSpawnX(int x);

    int getMCSpawnY();

    void setMCSpawnY(int y);

    int getMCSpawnZ();

    void setMCSpawnZ(int z);

    float getMCSpawnAngle();

    void setMCSpawnAngle(float angle);

    GameType getMCGameType();

    void setMCGameType(GameType type);

    void setMCGameType(int type);

    boolean isMCMapFeaturesEnabled();

    void setMCMapFeaturesEnabled(boolean enabled);

    boolean isMCHardcore();

    void setMCHardcore(boolean hardcore);

    boolean isMCAllowCommands();

    void setMCAllowCommands(boolean allow);

    boolean isMCInitialized();

    void setMCInitialized(boolean init);

    Difficulty getMCDifficulty();

    void setMCDifficulty(Difficulty diff);

    void setDifficulty(int diff);

    boolean isMCDifficultyLocked();

    void setMCDifficultyLocked(boolean locked);

    boolean isMCRaining();

    void setMCRaining(boolean raining);

    int getMCRainTime();

    void setMCRainTime(int time);

    boolean getMCThundering();

    void setMCThundering(boolean thundering);

    int getMCThunderTime();

    void setMCThunderTime(int time);

    long getMCGameTime();

    void setMCGameTime(long time);

    long getMCDayTime();

    void setMCDayTime(long time);

    int getMCVersionId();

    void setMCVersionId(int versionId);

    void update();
}
