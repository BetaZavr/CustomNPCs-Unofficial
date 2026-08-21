package noppes.npcs.dimensions;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.INbt;
import noppes.npcs.api.handler.IDimensionHandler;
import noppes.npcs.api.handler.data.IWorldInfo;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.mixin.server.IMinecraftServerMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSync;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.Executor;

public class DimensionHandler extends SavedData implements IDimensionHandler {

    private static final String NAME = "customnpcs_dimensions";
    private final Map<ResourceKey<Level>, CustomWorldInfo> dimensionInfo = new LinkedHashMap<>();
    private final Set<ResourceKey<Level>> toBeDeleted = new HashSet<>();

    public DimensionHandler() {}

    public static DimensionHandler getInstance(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                DimensionHandler::load,
                DimensionHandler::new,
                NAME
        );
    }

    public static DimensionHandler getInstance() {
        if (CustomNpcs.Server == null) return new DimensionHandler();
        return getInstance(CustomNpcs.Server.overworld());
    }

    @Override
    public IWorldInfo createDimension() { return new CustomWorldInfo(new CompoundTag()); }

    public void createDimension(ServerPlayer player, String name, CustomWorldInfo info) {
        ResourceLocation location = new ResourceLocation(CustomNpcs.MODID, name);
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, location);
        dimensionInfo.put(levelKey, info);
        setDirty();
        loadDimension(levelKey, info);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.dimensions.created", info.getMCLevelName(), levelKey.location()));
        }
        syncWithClients();
    }

    @Override
    public void deleteDimension(String dimensionId) {
        ResourceLocation location;
        try {
            location = new ResourceLocation(dimensionId);
        } catch (Exception e) { return; }
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, location);
        deleteDimension(null, levelKey);
    }

    public void deleteDimension(ServerPlayer player, ResourceKey<Level> levelKey) {
        if (!dimensionInfo.containsKey(levelKey)) return;
        ServerLevel level = CustomNpcs.Server.getLevel(levelKey);
        if (level != null && !level.players().isEmpty()) {
            ServerLevel overworld = CustomNpcs.Server.overworld();
            BlockPos coords = overworld.getSharedSpawnPos();
            if (!overworld.isEmptyBlock(coords)) {
                coords = overworld.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, coords);
            } else {
                while (overworld.isEmptyBlock(coords) && coords.getY() > 0) { coords = coords.below(); }
                if (coords.getY() == 0) {
                    coords = overworld.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, coords);
                }
            }
            final BlockPos target = coords;
            new ArrayList<>(level.players()).forEach(p -> {
                p.sendSystemMessage(Component.translatable("message.dimensions.tp.isdelete"));
                noppes.npcs.packets.server.SPacketDimensionTeleport.teleportPlayer(p, Level.OVERWORLD, target.getX() + 0.5d, target.getY(), target.getZ() + 0.5d, p.getYRot(), p.getXRot());
            });
        }
        toBeDeleted.add(levelKey);
        unloadDimension(levelKey);
        setDirty();
        syncWithClients();
    }

    public void restoreDimension(ServerPlayer player, ResourceKey<Level> levelKey) {
        if (!dimensionInfo.containsKey(levelKey) || !toBeDeleted.contains(levelKey)) return;
        toBeDeleted.remove(levelKey);
        setDirty();
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.dimensions.restored", levelKey.location()));
        }
        syncWithClients();
    }

    @Override
    public int[] getAllIDs() {
        // Legacy API compatibility; ResourceKey-based dimensions do not use int IDs
        return new int[0];
    }

    public List<ResourceKey<Level>> getAllDimensionKeys() {
        List<ResourceKey<Level>> list = new ArrayList<>();
        for (ResourceKey<Level> key : dimensionInfo.keySet()) {
            if (!toBeDeleted.contains(key)) list.add(key);
        }
        return list;
    }

    @Override
    public IWorldInfo getMCWorldInfo(String dimensionId) {
        ResourceLocation location;
        try { location = new ResourceLocation(dimensionId); }
        catch (Exception e) { return null; }
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, location);
        return dimensionInfo.get(levelKey);
    }

    public IWorldInfo getMCWorldInfo(ResourceKey<Level> levelKey) {
        return dimensionInfo.get(levelKey);
    }

    @Override
    public INbt getNbt() {
        return new NBTWrapper(save(new CompoundTag()));
    }

    @Override
    public void setNbt(INbt nbt) {
        if (nbt != null) load(nbt.getMCNBT());
    }

    @Override
    @Nonnull
    public CompoundTag save(@Nonnull CompoundTag compound) {
        ListTag list = new ListTag();
        for (Map.Entry<ResourceKey<Level>, CustomWorldInfo> entry : dimensionInfo.entrySet()) {
            if (toBeDeleted.contains(entry.getKey())) continue;
            CompoundTag nbt = new CompoundTag();
            nbt.putString("dimensionID", entry.getKey().location().toString());
            nbt.put("worldInfo", entry.getValue().save());
            list.add(nbt);
        }
        compound.put("dimensionInfo", list);
        ListTag delList = new ListTag();
        for (ResourceKey<Level> key : toBeDeleted) {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("dimensionID", key.location().toString());
            delList.add(nbt);
        }
        compound.put("toBeDeleted", delList);
        return compound;
    }

    public static DimensionHandler load(CompoundTag compound) {
        DimensionHandler handler = new DimensionHandler();
        ListTag list = compound.getList("dimensionInfo", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag nbt = list.getCompound(i);
            ResourceLocation location = new ResourceLocation(nbt.getString("dimensionID"));
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
            handler.dimensionInfo.put(key, new CustomWorldInfo(nbt.getCompound("worldInfo")));
        }
        ListTag delList = compound.getList("toBeDeleted", 10);
        for (int i = 0; i < delList.size(); i++) {
            CompoundTag nbt = delList.getCompound(i);
            ResourceLocation location = new ResourceLocation(nbt.getString("dimensionID"));
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
            handler.toBeDeleted.add(key);
        }
        return handler;
    }

    private void loadDimension(ResourceKey<Level> levelKey, CustomWorldInfo info) {
        MinecraftServer server = CustomNpcs.Server;
        if (server == null) return;
        if (server.getLevel(levelKey) != null) return; // Already loaded

        try {
            RegistryAccess registryAccess = server.registryAccess();
            Registry<DimensionType> dimTypeReg = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);
            Holder<DimensionType> dimType = dimTypeReg.getHolderOrThrow(
                    ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation("overworld")));

            ChunkGenerator chunkGenerator = buildChunkGenerator(server, info, registryAccess);
            LevelStem stem = new LevelStem(dimType, chunkGenerator);

            PrimaryLevelData levelData = new PrimaryLevelData(
                    info.getLevelSettings(),
                    info.worldGenOptions(),
                    PrimaryLevelData.SpecialWorldProperty.NONE,
                    Lifecycle.stable()
            );

            Executor executor = ((IMinecraftServerMixin) server).getExecutor();
            LevelStorageSource.LevelStorageAccess storage = ((IMinecraftServerMixin) server).getStorageSource();
            /*
            ServerLevel newLevel = new ServerLevel(
                    server,
                    executor,
                    storage,
                    levelData,
                    levelKey,
                    stem,
                    new LoggingChunkProgressListener(11),
                    false,
                    info.getMCSeed(),
                    Collections.emptyList(),
                    false,
                    new net.minecraft.world.level.levelgen.RandomSequences(levelKey)
            );
            ((IMinecraftServerMixin) server).getLevels().put(levelKey, newLevel);
            MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(newLevel));
            /**/
            LogWriter.debug("Loaded custom dimension: " + levelKey.location());
        } catch (Exception e) {
            LogWriter.error("Error loading custom dimension: " + levelKey.location(), e);
        }
    }

    private ChunkGenerator buildChunkGenerator(MinecraftServer server, CustomWorldInfo info, RegistryAccess registryAccess) {
        /*
        String type = info.getMCGeneratorType();
        if ("flat".equalsIgnoreCase(type)) {
            return new FlatLevelSource(FlatLevelGeneratorSettings.getDefault(
                    registryAccess.registryOrThrow(Registries.BIOME),
                    registryAccess.registryOrThrow(Registries.STRUCTURE_SET)));
        }
        // Default to overworld-like noise generation
        Registry<LevelStem> stemReg = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
        LevelStem overworldStem = stemReg.get(LevelStem.OVERWORLD);
        if (overworldStem != null) return overworldStem.generator();

        return new NoiseBasedChunkGenerator(
                MultiNoiseBiomeSource.Preset.OVERWORLD.source(registryAccess.lookupOrThrow(Registries.BIOME)),
                registryAccess.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD)
        );
        /**/
        return null;
    }

    public void unloadDimension(ResourceKey<Level> levelKey) {
        MinecraftServer server = CustomNpcs.Server;
        if (server == null) return;
        ServerLevel level = server.getLevel(levelKey);
        if (level != null) {
            level.save(null, false, false);
            ((IMinecraftServerMixin) server).getLevels().remove(levelKey);
            MinecraftForge.EVENT_BUS.post(new LevelEvent.Unload(level));
        }
    }

    public void ensureDimensionLoaded(ResourceKey<Level> levelKey) {
        if (dimensionInfo.containsKey(levelKey) && CustomNpcs.Server.getLevel(levelKey) == null) {
            loadDimension(levelKey, dimensionInfo.get(levelKey));
        }
    }

    public boolean isDelete(ResourceKey<Level> id) {
        return toBeDeleted.contains(id);
    }

    private void syncWithClients() {
        CompoundTag compound = new CompoundTag();
        ListTag list = new ListTag();
        for (ServerLevel level : CustomNpcs.Server.getAllLevels()) {
            ResourceKey<Level> dimId = level.dimension();
            if (dimId == Level.OVERWORLD || dimId == Level.NETHER || dimId == Level.END) {
                CompoundTag nbt = new CompoundTag();
                nbt.putBoolean("loaded", true);
                nbt.putString("name", dimId.location().toString());
                nbt.putBoolean("deleted", false);
                list.add(nbt);
            }
        }
        for (Map.Entry<ResourceKey<Level>, CustomWorldInfo> entry : dimensionInfo.entrySet()) {
            ResourceKey<Level> dimId = entry.getKey();
            boolean isDeleted = toBeDeleted.contains(dimId);
            ServerLevel level = CustomNpcs.Server.getLevel(dimId);
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("loaded", level != null);
            nbt.putString("name", entry.getValue().getMCLevelName());
            nbt.putBoolean("deleted", isDeleted);
            list.add(nbt);
        }
        compound.put("Data", list);
        Packets.sendAll(new PacketSync(9, compound, true));
    }

    public void loadDimensions() {
        for (Map.Entry<ResourceKey<Level>, CustomWorldInfo> entry : dimensionInfo.entrySet()) {
            if (!toBeDeleted.contains(entry.getKey())) {
                loadDimension(entry.getKey(), entry.getValue());
            }
        }
    }
}