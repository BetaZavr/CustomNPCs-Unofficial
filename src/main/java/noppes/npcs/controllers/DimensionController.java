package noppes.npcs.controllers;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.gui.IDimensionGetter;
import noppes.npcs.controllers.data.DimensionData;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

public class DimensionController {

    private static boolean isLoad = false;
    private static final Map<String, DimensionData> data = new LinkedHashMap<>();

    public static void load() {
        if (isLoad) { return; }
        File dir = CustomNpcs.getLevelSaveDirectory();
        if (dir != null) {
            File file = new File(dir, "dimensions.dat");
            if (file.exists()) {
                try { load(NbtIo.readCompressed(file)); } catch (Exception e) { LogWriter.error(e); }
            }
            isLoad = true;
        }
    }

    public static void load(CompoundTag compound) {
        if (compound == null) { return; }
        data.clear();
        for (int i = 0; i < compound.getList("Data", 10).size(); i++) {
            CompoundTag nbt = compound.getList("Data", 10).getCompound(i);
            data.put(nbt.getString("id"), new DimensionData(nbt));
        }
        if (Minecraft.getInstance().screen instanceof IDimensionGetter gui) { gui.resetDimension(); }
    }

    public static void save() {
        File file = new File(CustomNpcs.getLevelSaveDirectory(), "dimensions.dat");
        CompoundTag compound = new CompoundTag();
        ListTag list = new ListTag();
        for (String location : data.keySet()) {
            CompoundTag nbt = data.get(location).save();
            nbt.putString("name", location);
            list.add(nbt);
        }
        compound.put("Data", list);
        try { NbtIo.writeCompressed(compound, file); } catch (Exception e) { LogWriter.error(e); }
    }

    public static List<String> getLineKeys() {
        return new ArrayList<>(data.keySet());
    }

    public static boolean has(ResourceKey<Level> location) {
        if (location == null) { return false; }
        for (String line : data.keySet()) {
            if (line.equals(location.location().toString())) { return true; }
        }
        return false;
    }

    public static void setSpawn(Level level, BlockPos pos, float angle) {
        if (level == null || pos == null) { return; }
        String key = level.dimension().location().toString();
        if (!data.containsKey(key)) { data.put(key, new DimensionData(level)); }
        data.get(key).spawnPos = pos;
        data.get(key).spawnAngle = angle;
        save();
    }

    public static @Nonnull DimensionData get(ServerLevel level) {
        if (level == null) { return new DimensionData(); }
        String key = level.dimension().location().toString();
        if (data.containsKey(key)) { return data.get(key); }
        data.put(key, new DimensionData(level));
        return data.get(key);
    }

    public static @Nullable DimensionData get(String dimensionId) {
        if (data.containsKey(dimensionId)) { return data.get(dimensionId); }
        return null;
    }


    public static void deleteDimension(ServerPlayer player, ResourceKey<Level> dimensionID) {
        /*
        if (dimensionID <= 100 || !dimensionInfo.containsKey(dimensionID)) {
            if (sender != null) {
                if (toBeDeleted.containsKey(dimensionID)) {
                    sender.sendMessage(new TextComponentTranslation("message.dimensions.err.del"));
                } else if (!dimensionInfo.containsKey(dimensionID)) {
                    sender.sendMessage(new TextComponentTranslation("message.dimensions.err.notmod"));
                }
            }
            return;
        }
        World worldObj = DimensionManager.getWorld(dimensionID);
        if (!worldObj.playerEntities.isEmpty()) {
            WorldServer world = Objects.requireNonNull(sender.getServer()).getWorld(0);
            BlockPos coords = world.getSpawnCoordinate();
            if (coords == null) {
                coords = world.getSpawnPoint();
                if (!world.isAirBlock(coords)) {
                    coords = world.getTopSolidOrLiquidBlock(coords);
                } else {
                    while (world.isAirBlock(coords) && coords.getY() > 0) {
                        coords = coords.down();
                    }
                    if (coords.getY() == 0) {
                        coords = world.getTopSolidOrLiquidBlock(coords);
                    }
                }
            }
            List<EntityPlayerMP> players = new ArrayList<>();
            for (EntityPlayer player : worldObj.playerEntities) {
                if (!(player instanceof EntityPlayerMP)) {
                    continue;
                }
                player.sendMessage(new TextComponentTranslation("message.dimensions.tp.isdelete"));
                players.add((EntityPlayerMP) player);
            }
            for (EntityPlayerMP player : players) {
                SPacketDimensionTeleport.teleportPlayer(player, 0, coords.getX(), coords.getY(), coords.getZ(),
                        player.rotationYaw, player.rotationPitch);
            }
        }
        Entity entitySender = null;
        if (sender != null) { entitySender = sender.getCommandSenderEntity(); }
        toBeDeleted.put(dimensionID, entitySender != null ? entitySender.getUniqueID() : null);
        DimensionManager.unloadWorld(dimensionID);
        List<WorldServer> list = new ArrayList<>();
        for (WorldServer w : CustomNpcs.Server.worlds) {
            if (w.provider.getDimension() != dimensionID) {
                list.add(w);
            }
        }
        if (CustomNpcs.Server.worlds.length != list.size()) { CustomNpcs.Server.worlds = list.toArray(new WorldServer[0]); }
        */
    }

    public static void restoreDimension(ServerPlayer player, ResourceKey<Level> dimension) {

    }

    public static boolean isDelete(ResourceKey<Level> id) {
        return false;
    }

}
