package noppes.npcs.dimensions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.timers.TimerCallbacks;
import net.minecraft.world.level.timers.TimerQueue;
import net.minecraftforge.common.ForgeHooks;
import noppes.npcs.api.INbt;
import noppes.npcs.api.handler.data.IWorldInfo;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.mixin.world.level.ILevelSettingsMixin;
import noppes.npcs.mixin.world.level.levelgen.IWorldOptionsMixin;
import noppes.npcs.mixin.world.level.storage.IPrimaryLevelDataMixin;
import noppes.npcs.util.ValueUtil;
import org.slf4j.Logger;

import java.util.*;

public class CustomWorldInfo extends PrimaryLevelData implements IWorldInfo {

    private static final Logger LOGGER = LogUtils.getLogger();
    private String dimensionId = "";

    @SuppressWarnings("deprecation")
    public CustomWorldInfo(CompoundTag nbt) {
        super(buildLevelSettings(nbt), buildWorldOptions(nbt), SpecialWorldProperty.NONE, Lifecycle.stable());
        load(nbt);
    }

    private static LevelSettings buildLevelSettings(CompoundTag nbt) {
        GameType gameType = nbt.contains("GameType", 99) ? GameType.byId(nbt.getInt("GameType")) : GameType.SURVIVAL;
        GameRules rules;
        if (nbt.contains("GameRules", 10)) { rules = new GameRules(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("GameRules"))); }
        else { rules = new GameRules(); }
        return new LevelSettings(
                nbt.contains("LevelName", 8) ? nbt.getString("LevelName") : "custom_dimension",
                gameType,
                nbt.getBoolean("hardcore"),
                nbt.contains("Difficulty", 99) ? Difficulty.byId(nbt.getByte("Difficulty")) : Difficulty.NORMAL,
                nbt.contains("allowCommands", 99) ? nbt.getBoolean("allowCommands") : (gameType == GameType.CREATIVE),
                rules,
                new WorldDataConfiguration(
                        new DataPackConfig(List.of(), List.of()),
                        FeatureFlags.DEFAULT_FLAGS
                )
        );
    }

    private static WorldOptions buildWorldOptions(CompoundTag nbt) {
        return new WorldOptions(nbt.getLong("RandomSeed"),
                !nbt.contains("MapFeatures", 99) || nbt.getBoolean("MapFeatures"),
                false); // generateBonusChest
    }

    public void load(CompoundTag nbt) {
        dimensionId = nbt.getString("DimensionId");
        if (nbt.contains("playerDataVersion", Tag.TAG_ANY_NUMERIC)) { setMCPlayerDataVersion(nbt.getInt("playerDataVersion")); }
        // LevelSettings
        LevelSettings settings = ((IPrimaryLevelDataMixin) this).getSettings();
        nbt.putInt("GameType", settings.gameType().getId());
        nbt.putString("LevelName", settings.levelName());
        nbt.putBoolean("hardcore", settings.hardcore());
        nbt.putBoolean("allowCommands", settings.allowCommands());
        nbt.putByte("Difficulty", (byte) settings.difficulty().getId());
        nbt.put("GameRules", settings.gameRules().createTag());
        nbt.putString("forgeLifecycle", ForgeHooks.encodeLifecycle(settings.getLifecycle()));
        WorldDataConfiguration.CODEC.encodeStart(NbtOps.INSTANCE, settings.getDataConfiguration())
                .resultOrPartial((error) -> LOGGER.warn("Failed to decode WorldDataConfiguration: {}", error))
                .ifPresent((encodedTag) -> nbt.put("DataConfiguration", encodedTag));
        // WorldOptions
        nbt.putLong("RandomSeed", worldGenOptions().seed());
        nbt.putBoolean("MapFeatures", worldGenOptions().generateStructures());
        // Lifecycle
        nbt.putBoolean("worldGenSettingsLifecycle", worldGenSettingsLifecycle() == Lifecycle.stable());
        // SpecialWorldProperty
        nbt.putInt("specialWorldProperty", getMCSpecialWorldProperty());
        // WorldBorder.Settings

        if (nbt.contains("WorldBorderSettings", Tag.TAG_COMPOUND)) {
            setMCWorldBorder(WorldBorder.Settings.read(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("WorldBorderSettings")),
                    getMCWorldBorder()));
        }
        // EndDragonFightData
        if (nbt.contains("WorldBorderSettings", Tag.TAG_COMPOUND)) {
            setMCEndDragonFightData(Util.getOrThrow(
                    EndDragonFight.Data.CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("DragonFight")),
                    IllegalStateException::new
            ));
        }
        // knownServerBrands
        if (nbt.contains("ServerBrands", Tag.TAG_LIST) && ((ListTag) Objects.requireNonNull(nbt.get("ServerBrands"))).getElementType() == Tag.TAG_STRING) {
            ListTag known = nbt.getList("ServerBrands", Tag.TAG_STRING);
            String[] knownIn = new String[known.size()];
            for (int i = 0; i < known.size(); i++) { knownIn[i] = known.getString(i); }
            setMCKnownServerBrands(knownIn);
        }
        // removedFeatureFlags
        if (nbt.contains("removed_features", Tag.TAG_LIST) && ((ListTag) Objects.requireNonNull(nbt.get("removed_features"))).getElementType() == Tag.TAG_STRING) {
            ListTag flags = nbt.getList("removed_features", Tag.TAG_STRING);
            String[] flagIn = new String[flags.size()];
            for (int i = 0; i < flags.size(); i++) { flagIn[i] = flags.getString(i); }
            setMCRemovedFeatureFlags(flagIn);
        }
        // any
        if (nbt.contains("SpawnX", Tag.TAG_ANY_NUMERIC)) { setXSpawn(nbt.getInt("SpawnX")); }
        if (nbt.contains("SpawnY", Tag.TAG_ANY_NUMERIC)) { setYSpawn(nbt.getInt("SpawnY")); }
        if (nbt.contains("SpawnZ", Tag.TAG_ANY_NUMERIC)) { setZSpawn(nbt.getInt("SpawnZ")); }
        if (nbt.contains("spawnAngle", Tag.TAG_FLOAT)) { setSpawnAngle(nbt.getFloat("SpawnAngle")); }
        if (nbt.contains("Time", Tag.TAG_ANY_NUMERIC)) { setGameTime(nbt.getLong("Time")); }
        if (nbt.contains("DayTime", Tag.TAG_ANY_NUMERIC)) { setDayTime(nbt.getLong("DayTime")); }
        if (nbt.contains("Player", Tag.TAG_COMPOUND)) {
            ((IPrimaryLevelDataMixin) this).setLoadedPlayerTag(nbt.getCompound("Player"));
        }
        if (nbt.contains("version", Tag.TAG_ANY_NUMERIC)) { setMCVersionId(nbt.getInt("version")); }
        setMCClearWeatherTime(nbt.getInt("clearWeatherTime"));
        setMCRaining(nbt.getBoolean("raining"));
        setMCRainTime(nbt.getInt("rainTime"));
        setMCThundering(nbt.getBoolean("thundering"));
        setMCThunderTime(nbt.getInt("thunderTime"));
        setMCInitialized(!nbt.contains("initialized", Tag.TAG_BYTE) || nbt.getBoolean("initialized"));
        if (nbt.contains("difficultyLocked", Tag.TAG_BYTE)) { setMCDifficultyLocked(nbt.getBoolean("difficultyLocked")); }
        if (nbt.contains("customBossEvents", Tag.TAG_COMPOUND)) {
            setMCCustomBossEvents(new NBTWrapper(nbt.getCompound("customBossEvents")));
        }
        if (nbt.contains("WanderingTraderSpawnDelay", Tag.TAG_ANY_NUMERIC)) { setMCWanderingTraderSpawnDelay(nbt.getInt("WanderingTraderSpawnDelay")); }
        if (nbt.contains("WanderingTraderSpawnChance", Tag.TAG_ANY_NUMERIC)) { setMCWanderingTraderSpawnChance(nbt.getInt("WanderingTraderSpawnChance")); }
        setMCWanderingTraderId(nbt.contains("WanderingTraderId") ? nbt.getUUID("WanderingTraderId") : null);
        setMCWasModded(nbt.getBoolean("WasModded"));
        Tag scheduledTag = nbt.get("ScheduledEvents");
        if (scheduledTag != null) {
            ((IPrimaryLevelDataMixin) this).setScheduledEvents(
                    new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS,
                            new Dynamic<>(NbtOps.INSTANCE, scheduledTag).asStream())
            );
        }
        setMCConfirmedExperimentalWarning(nbt.getBoolean("confirmedExperimentalSettings"));
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("DimensionId", dimensionId);
        nbt.putInt("playerDataVersion", getMCPlayerDataVersion());
        // --- LevelSettings ---
        LevelSettings settings = ((IPrimaryLevelDataMixin) this).getSettings();
        nbt.putInt("GameType", getGameType().getId());
        nbt.putString("LevelName", getLevelName());
        nbt.putBoolean("hardcore", isHardcore());
        nbt.putBoolean("allowCommands", getAllowCommands());
        nbt.putByte("Difficulty", (byte) getDifficulty().getId());
        nbt.put("GameRules", getGameRules().createTag());
        nbt.putString("forgeLifecycle", ForgeHooks.encodeLifecycle(settings.getLifecycle()));
        WorldDataConfiguration.CODEC.encodeStart(NbtOps.INSTANCE, settings.getDataConfiguration())
                .resultOrPartial((error) -> LOGGER.warn("Failed to encode WorldDataConfiguration: {}", error))
                .ifPresent((encodedTag) -> nbt.put("DataConfiguration", encodedTag));
        // --- WorldOptions ---
        nbt.putLong("RandomSeed", worldGenOptions().seed());
        nbt.putBoolean("MapFeatures", worldGenOptions().generateStructures());
        // --- Lifecycle ---
        nbt.putBoolean("worldGenSettingsLifecycle", worldGenSettingsLifecycle() == Lifecycle.stable());
        // --- SpecialWorldProperty ---
        nbt.putInt("specialWorldProperty", getMCSpecialWorldProperty());
        // --- WorldBorder.Settings ---
        WorldBorder.Settings border = getMCWorldBorder();
        if (border != null) {
            CompoundTag borderTag = new CompoundTag();
            borderTag.putDouble("BorderCenterX", border.getCenterX());
            borderTag.putDouble("BorderCenterZ", border.getCenterZ());
            borderTag.putDouble("BorderSize", border.getSize());
            borderTag.putDouble("BorderSizeLerpTarget", border.getSizeLerpTarget());
            borderTag.putLong("BorderSizeLerpTime", border.getSizeLerpTime());
            borderTag.putDouble("BorderSafeZone", border.getSafeZone());
            borderTag.putDouble("BorderDamagePerBlock", border.getDamagePerBlock());
            borderTag.putDouble("BorderWarningBlocks", border.getWarningBlocks());
            borderTag.putDouble("BorderWarningTime", border.getWarningTime());
            nbt.put("WorldBorderSettings", borderTag);
        }
        // --- EndDragonFight.Data ---
        EndDragonFight.Data dragonData = getMCEndDragonFightData();
        if (dragonData != null) {
            EndDragonFight.Data.CODEC.encodeStart(NbtOps.INSTANCE, dragonData)
                    .resultOrPartial((error) -> LOGGER.warn("Failed to encode EndDragonFight.Data: {}", error))
                    .ifPresent((tag) -> nbt.put("DragonFight", tag));
        }
        // --- knownServerBrands ---
        String[] brands = getMCKnownServerBrands();
        if (brands != null && brands.length > 0) {
            ListTag list = new ListTag();
            for (String brand : brands) { list.add(net.minecraft.nbt.StringTag.valueOf(brand)); }
            nbt.put("ServerBrands", list);
        }
        // --- removedFeatureFlags ---
        String[] flags = getMCRemovedFeatureFlags();
        if (flags != null && flags.length > 0) {
            ListTag list = new ListTag();
            for (String flag : flags) { list.add(net.minecraft.nbt.StringTag.valueOf(flag)); }
            nbt.put("removed_features", list);
        }
        // --- Spawn ---
        nbt.putInt("SpawnX", getXSpawn());
        nbt.putInt("SpawnY", getYSpawn());
        nbt.putInt("SpawnZ", getZSpawn());
        nbt.putFloat("spawnAngle", getSpawnAngle());
        // --- Time ---
        nbt.putLong("Time", getGameTime());
        nbt.putLong("DayTime", getDayTime());
        // --- Player (loadedPlayerTag) ---
        CompoundTag playerTag = ((IPrimaryLevelDataMixin) this).getLoadedPlayerTag();
        if (playerTag != null) { nbt.put("Player", playerTag); }
        // --- Version ---
        nbt.putInt("version", getMCVersionId());
        // --- Weather ---
        nbt.putInt("clearWeatherTime", getMCClearWeatherTime());
        nbt.putBoolean("raining", isMCRaining());
        nbt.putInt("rainTime", getMCRainTime());
        nbt.putBoolean("thundering", getMCThundering());
        nbt.putInt("thunderTime", getMCThunderTime());
        // --- Init / difficultyLocked ---
        nbt.putBoolean("initialized", isMCInitialized());
        nbt.putBoolean("difficultyLocked", isMCDifficultyLocked());
        // --- customBossEvents ---
        INbt bossEvents = getMCCustomBossEvents();
        if (bossEvents != null) {
            nbt.put("customBossEvents", bossEvents.getMCNBT());
        }
        // --- WanderingTrader ---
        nbt.putInt("WanderingTraderSpawnDelay", getMCWanderingTraderSpawnDelay());
        nbt.putInt("WanderingTraderSpawnChance", getMCWanderingTraderSpawnChance());
        UUID traderId = getMCWanderingTraderId();
        if (traderId != null) {
            nbt.putUUID("WanderingTraderId", traderId);
        }
        // --- WasModded ---
        nbt.putBoolean("WasModded", getMCWasModded());
        // --- ScheduledEvents ---
        TimerQueue<MinecraftServer> scheduledEvents = ((IPrimaryLevelDataMixin) this).getScheduledEvents();
        if (scheduledEvents != null) {
            nbt.put("ScheduledEvents", scheduledEvents.store());
        }
        // --- Experimental warning ---
        nbt.putBoolean("confirmedExperimentalSettings", getMCConfirmedExperimentalWarning());
        return nbt;
    }

    @Override
    public String getId() { return dimensionId; }

    @Override
    public INbt getNbt() { return new NBTWrapper(save()); }

    @Override
    public void setNbt(INbt inbt) { if (inbt != null) load(inbt.getMCNBT()); }

    @Override
    public boolean getMCConfirmedExperimentalWarning() { return hasConfirmedExperimentalWarning(); }
    @Override
    public void setMCConfirmedExperimentalWarning(boolean settings) { withConfirmedWarning(settings); }

    @Override
    public boolean getMCWasModded() { return wasModded(); }
    @Override
    public void setMCWasModded(boolean wasModded) { ((IPrimaryLevelDataMixin) this).setWasModded(wasModded); }

    @Override
    public String[] getMCKnownServerBrands() { return ((IPrimaryLevelDataMixin) this).getKnownServerBrands().toArray(new String[0]); }
    @Override
    public void setMCKnownServerBrands(String[] known) {
        Set<String> knownServerBrands = ((IPrimaryLevelDataMixin) this).getKnownServerBrands();
        knownServerBrands.clear();
        knownServerBrands.addAll(List.of(known));
    }

    @Override
    public String[] getMCRemovedFeatureFlags() { return ((IPrimaryLevelDataMixin) this).getRemovedFeatureFlags().toArray(new String[0]); }
    @Override
    public void setMCRemovedFeatureFlags(String[] flags) {
        Set<String> knownServerBrands = ((IPrimaryLevelDataMixin) this).getRemovedFeatureFlags();
        knownServerBrands.clear();
        knownServerBrands.addAll(List.of(flags));
    }

    @Override
    public UUID getMCWanderingTraderId() { return getWanderingTraderId(); }
    @Override
    public void setMCWanderingTraderId(UUID uuid) { setWanderingTraderId(uuid); }

    @Override
    public INbt getMCCustomBossEvents() {
        CompoundTag nbt = getCustomBossEvents();
        return nbt == null ? null : new NBTWrapper(nbt);
    }
    @Override
    public void setMCCustomBossEvents(INbt nbt) { setCustomBossEvents(nbt == null ? null : nbt.getMCNBT()); }

    @Override
    public EndDragonFight.Data getMCEndDragonFightData() { return endDragonFightData(); }
    @Override
    public void setMCEndDragonFightData(EndDragonFight.Data data) { setEndDragonFightData(data); }

    @Override
    public WorldBorder.Settings getMCWorldBorder() { return getWorldBorder(); }
    @Override
    public void setMCWorldBorder(WorldBorder.Settings worldBorder) {
        if (worldBorder != null) { setWorldBorder(worldBorder); }
    }

    @Override
    public int getMCClearWeatherTime() { return getClearWeatherTime(); }
    @Override
    public void setMCClearWeatherTime(int time) { setClearWeatherTime(ValueUtil.correctInt(time, 0, Integer.MAX_VALUE)); }

    @Override
    public Lifecycle getMCWorldGenSettingsLifecycle() { return worldGenSettingsLifecycle(); }
    @Override
    public void setMCWorldGenSettingsLifecycle(boolean isStable) {
        ((IPrimaryLevelDataMixin) this).setWorldGenSettingsLifecycle(isStable ? Lifecycle.stable() : Lifecycle.experimental());
    }

    @Override
    public Lifecycle getMCLifecycle() { return ((IPrimaryLevelDataMixin) this).getSettings().getLifecycle(); }
    @Override
    public void setMCLifecycle(String lifecycle) {
        LevelSettings settings = ((IPrimaryLevelDataMixin) this).getSettings();
        ((ILevelSettingsMixin) (Object) settings).setLifecycle(ForgeHooks.parseLifecycle(lifecycle));
    }

    @Override
    public int getMCSpecialWorldProperty() { return ((IPrimaryLevelDataMixin) this).getSpecialWorldProperty().ordinal();  }
    @Override
    @SuppressWarnings("deprecation")
    public void setMCSpecialWorldProperty(int type) {
        ((IPrimaryLevelDataMixin) this).setSpecialWorldProperty(SpecialWorldProperty.values()[type % SpecialWorldProperty.values().length]);
    }

    @Override
    public int getMCPlayerDataVersion() { return ((IPrimaryLevelDataMixin) this).getPlayerDataVersion(); }
    @Override
    public void setMCPlayerDataVersion(int id) { ((IPrimaryLevelDataMixin) this).setPlayerDataVersion(ValueUtil.correctInt(id, 0, Integer.MAX_VALUE));}

    @Override
    public int getMCWanderingTraderSpawnDelay() { return getWanderingTraderSpawnDelay(); }
    @Override
    public void setMCWanderingTraderSpawnDelay(int delay) { setWanderingTraderSpawnDelay(ValueUtil.correctInt(delay, 0, Integer.MAX_VALUE)); }

    @Override
    public int getMCWanderingTraderSpawnChance() { return getWanderingTraderSpawnChance(); }
    @Override
    public void setMCWanderingTraderSpawnChance(int chance) { setWanderingTraderSpawnChance(ValueUtil.correctInt(chance, 0, Integer.MAX_VALUE)); }

    @Override
    public GameRules getMCGameRules() { return getGameRules(); }

    @Override
    public long getMCSeed() { return worldGenOptions().seed(); }
    @Override
    public void setMCSeed(long seed) { ((IWorldOptionsMixin) worldGenOptions()).setSeed(seed); }

    @Override
    public String getMCLevelName() { return getLevelName(); }
    @Override
    public void setMCLevelName(String name) {
        if (name != null && !name.isEmpty()) {
            LevelSettings settings = ((IPrimaryLevelDataMixin) this).getSettings();
            ((ILevelSettingsMixin) (Object) settings).setLevelName(name);
        }
    }

    @Override
    public int getMCSpawnX() { return getXSpawn(); }
    @Override
    public void setMCSpawnX(int x) { setXSpawn(x); }

    @Override
    public int getMCSpawnY() { return getYSpawn(); }
    @Override
    public void setMCSpawnY(int y) { setYSpawn(y); }

    @Override
    public int getMCSpawnZ() { return getZSpawn(); }
    @Override
    public void setMCSpawnZ(int z) { setZSpawn(z); }

    @Override
    public float getMCSpawnAngle() { return getSpawnAngle(); }
    @Override
    public void setMCSpawnAngle(float angle) { setSpawnAngle(angle); }

    @Override
    public GameType getMCGameType() { return getGameType(); }
    @Override
    public void setMCGameType(GameType type) { setGameType(type); }
    @Override
    public void setMCGameType(int type) { setGameType(GameType.byId(type)); }

    @Override
    public boolean isMCMapFeaturesEnabled() { return worldGenOptions().generateStructures(); }
    @Override
    public void setMCMapFeaturesEnabled(boolean enabled) {
        ((IWorldOptionsMixin) worldGenOptions()).setGenerateStructures(enabled);
    }

    @Override
    public boolean isMCHardcore() { return isHardcore(); }
    @Override
    public void setMCHardcore(boolean hardcore) {
        LevelSettings settings = ((IPrimaryLevelDataMixin) this).getSettings();
        ((ILevelSettingsMixin) (Object) settings).setHardcore(hardcore);
    }

    @Override
    public boolean isMCAllowCommands() { return getAllowCommands(); }
    @Override
    public void setMCAllowCommands(boolean allow) {
        LevelSettings settings = ((IPrimaryLevelDataMixin) this).getSettings();
        ((ILevelSettingsMixin) (Object) settings).setAllowCommands(allow);
    }

    @Override
    public boolean isMCInitialized() { return isInitialized(); }
    @Override
    public void setMCInitialized(boolean init) { setInitialized(init); }

    @Override
    public Difficulty getMCDifficulty() { return getDifficulty(); }
    @Override
    public void setMCDifficulty(Difficulty diff) { setDifficulty(diff); }
    @Override
    public void setDifficulty(int diff) { setDifficulty(Difficulty.values()[diff % Difficulty.values().length]); }

    @Override
    public boolean isMCDifficultyLocked() { return isDifficultyLocked(); }
    @Override
    public void setMCDifficultyLocked(boolean locked) { setDifficultyLocked(locked); }

    @Override
    public boolean isMCRaining() { return isRaining(); }
    @Override
    public void setMCRaining(boolean raining) { setRaining(raining); }

    @Override
    public int getMCRainTime() { return getRainTime(); }
    @Override
    public void setMCRainTime(int time) { setRainTime(time); }

    @Override
    public boolean getMCThundering() { return isThundering(); }
    @Override
    public void setMCThundering(boolean thundering) { setThundering(thundering); }

    @Override
    public int getMCThunderTime() { return getThunderTime(); }
    @Override
    public void setMCThunderTime(int time) { setThunderTime(time); }

    @Override
    public long getMCGameTime() { return getGameTime(); }
    @Override
    public void setMCGameTime(long time) { setGameTime(time); }

    @Override
    public long getMCDayTime() { return getDayTime(); }
    @Override
    public void setMCDayTime(long time) { setDayTime(time); }

    @Override
    public int getMCVersionId() { return getVersion(); }
    @Override
    public void setMCVersionId(int versionId) { ((IPrimaryLevelDataMixin) this).setVersion(versionId); }

    @Override
    public void update() {
        DimensionHandler.getInstance().setDirty();
    }

}