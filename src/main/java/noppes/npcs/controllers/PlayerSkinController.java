package noppes.npcs.controllers;

import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.data.SkinData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSkin;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class PlayerSkinController {

    protected static PlayerSkinController instance = new PlayerSkinController();
    protected static final String filename = "player_skins";
    public static PlayerSkinController getInstance() {
        if (instance == null) { instance = new PlayerSkinController(); }
        return instance;
    }
    private static Type getType(int type) {
        if (type < 0) { type *= -1; }
        return Type.values()[type % Type.values().length];
    }
    public static void unload() {
        if (instance != null) {
            instance.playerNames.clear();
            instance.data.clear();
            instance = null;
        }
    }
    private final Map<UUID, String> playerNames = new HashMap<>();
    private final Map<UUID, Map<Type, SkinData>> data = new HashMap<>();

    public PlayerSkinController() { loadPlayerSkins(); }

    public void update(SkinData skinDataIn) {
        if (skinDataIn == null || CustomNpcs.Server == null) { return; }
        for (UUID uuid : data.keySet()) {
            for (SkinData skinData : data.get(uuid).values()) {
                if (skinDataIn.equals(skinData)) {
                    ServerPlayer player = CustomNpcs.Server.getPlayerList().getPlayer(uuid);
                    if (player != null) { sendToAll(uuid); } // online
                    break;
                }
            }
        }
    }

    private void loadPlayerSkins() {
        CustomNpcs.debugData.start("Mod");
        try {
            File saveDir = CustomNpcs.getLevelSaveDirectory();
            CompoundTag compound = NbtIo.readCompressed(new File(saveDir, filename + ".dat"));
            loadPlayerSkins(compound);
        } catch (Exception e) { save(); }
        CustomNpcs.debugData.end("Mod");
    }

    public void loadPlayerSkins(CompoundTag compound) {
        playerNames.clear();
        data.clear();
        ListTag list = compound.getList("Data", 10);
        for (int i = 0; i < list.size(); ++i) { loadPlayerSkin(list.getCompound(i)); }
    }

    public UUID loadPlayerSkin(CompoundTag nbtSkin) {
        if (nbtSkin == null) { return null; }
        UUID uuid = nbtSkin.getUUID("UUID");
        ListTag list = nbtSkin.getList("Textures", 10);
        if (list.isEmpty()) {
            playerNames.remove(uuid);
            data.remove(uuid);
            return null;
        }
        playerNames.put(uuid, nbtSkin.getString("Player"));
        Map<Type, SkinData> skins = new EnumMap<>(Type.class);
        for (int i = 0; i < nbtSkin.getList("Textures", 10).size(); i++) {
            SkinData sd = new SkinData();
            sd.load(nbtSkin.getList("Textures", 10).getCompound(i));
            if (sd.isValid()) { skins.put(sd.type(), sd); }
        }
        data.put(uuid, skins);
        return uuid;
    }

    public void logged(ServerPlayer player) {
        UUID uuid = player.getUUID();
        String name = player.getName().getString();
        if (data.containsKey(uuid)) {
            playerNames.put(uuid, name);
            sendToAll(uuid);
        }
        else if (playerNames.containsValue(name)) {
            for (UUID id : playerNames.keySet()) {
                if (playerNames.get(id).equals(name)) {
                    Map<Type, SkinData> map = new EnumMap<>(Type.class);
                    for (Type type : data.get(id).keySet()) { map.put(type, data.get(id).get(type).copy()); }
                    data.put(uuid, map);
                    sendToAll(uuid);
                    break;
                }
            }
        }
        else { Packets.send(player, new PacketSkin(0, new CompoundTag())); }
        if (player.getServer() != null) {
            for (ServerPlayer pl : player.getServer().getPlayerList().getPlayers()) {
                if (pl.equals(player) || !data.containsKey(pl.getUUID())) { continue; }
                Packets.send(player, new PacketSkin(1, getNBT(pl.getUUID())));
            }
        }
    }

    public CompoundTag getNBT() {
        CompoundTag compound = new CompoundTag();
        ListTag listUUIDs = new ListTag();
        for (UUID uuid : data.keySet()) {
            if (uuid == null) { continue; }
            listUUIDs.add(getNBT(uuid));
        }
        compound.put("Data", listUUIDs);
        return compound;
    }

    public CompoundTag getNBT(UUID uuid) {
        CompoundTag nbtPlayer = new CompoundTag();
        nbtPlayer.putUUID("UUID", uuid);
        ListTag textures = new ListTag();
        for (Type type : data.get(uuid).keySet()) {
            SkinData sd = data.get(uuid).get(type);
            if (sd == null) { continue; }
            textures.add(sd.save());
        }
        nbtPlayer.put("Textures", textures);
        nbtPlayer.putString("Player", playerNames.get(uuid));
        return nbtPlayer;
    }

    @SuppressWarnings("unused")
    public void sendToAll() {
        if (CustomNpcs.Server != null) {
            for (ServerPlayer player : CustomNpcs.Server.getPlayerList().getPlayers()) { sendToAll(player.getUUID()); }
        }
    }

    public void sendToAll(UUID uuid) {
        if (data.containsKey(uuid)) {
            CompoundTag nbtPlayer = getNBT(uuid);
            Packets.sendAll(new PacketSkin(1, nbtPlayer));
        }
    }

    public @Nonnull SkinData getData(UUID uuid, Type type) {
        if (!data.containsKey(uuid) || type == null) { return new SkinData(); }
        return data.get(uuid).get(type);
    }

    public @Nonnull SkinData getData(UUID uuid, int type) { return getData(uuid, getType(type)); }

    public void save() {
        try {
            File saveDir = CustomNpcs.getLevelSaveDirectory();
            File file = new File(saveDir, filename + ".dat_new");
            File file1 = new File(saveDir, filename + ".dat_old");
            File file2 = new File(saveDir, filename + ".dat");
            NbtIo.writeCompressed(getNBT(), new FileOutputStream(file));
            if (file1.exists() && !file1.delete()) { LogWriter.debug("Error delete \"" + file1.getName() + "\" file"); }
            if (!file2.renameTo(file1) || (file2.exists() && !file2.delete())) { LogWriter.debug("Error delete or rename \"" + file2.getName() + "\" file"); }
            if (!file.renameTo(file2) || (file.exists() && !file.delete())) { LogWriter.debug("Error delete or rename \"" + file.getName() + "\" file"); }
        }
        catch (Exception e) { LogWriter.except(e); }
    }

    public void set(String uuid, String location, int slot) {
        UUID id;
        try { id = UUID.fromString(uuid); } catch (Exception ignored) { return; }
        SkinData sd = getData(id, slot);
        sd.reset(location);
        if (data.containsKey(id)) { data.put(id, new EnumMap<>(Type.class)); }
        data.get(id).put(sd.type(), sd);
        save();
        update(sd);
    }

    public void set(String uuid, int type, int gender, int body, int bodyColor, int hair, int hairColor, int face, int eyesColor, int leg, int jacket, int shoes, int... peculiarities) {
        UUID id;
        try { id = UUID.fromString(uuid); } catch (Exception ignored) { return; }
        SkinData sd = getData(id, type);
        sd.setGender(gender);
        sd.setBodyType(body);
        sd.setBodyColor(bodyColor);
        sd.setHairType(hair);
        sd.setHairColor(hairColor);
        sd.setFaceType(face);
        sd.setEyesColor(eyesColor);
        sd.setPantsType(leg);
        sd.setJacketType(jacket);
        sd.setShoesType(shoes);
        sd.setPeculiarities(Arrays.stream(peculiarities).boxed().toList());
        if (data.containsKey(id)) { data.put(id, new EnumMap<>(Type.class)); }
        data.get(id).put(sd.type(), sd);
        save();
        update(sd);
    }

    public String get(Player player, int type) {
        SkinData sd = getData(player.getUUID(), type);
        return sd.isUrl() ? sd.getUrl() : sd.getLocation().toString();
    }

    public Map<Type, SkinData> get(UUID uuid) { return data.get(uuid); }

    /**
     * @param playerNameOrUUID name or uuid of player
     * @param type 0:SKIN, 1:CAPE, 2:ELYTRA;
     * @return location string
     */
    public SkinData get(String playerNameOrUUID, int type) {
        if (playerNameOrUUID == null || playerNameOrUUID.isEmpty()) { return null; }
        SkinData sd = null;
        if (playerNames.containsValue(playerNameOrUUID)) {
            for (UUID uuid : playerNames.keySet()) {
                if (playerNames.get(uuid).equals(playerNameOrUUID)) {
                    if (data.containsKey(uuid)) { sd = data.get(uuid).get(getType(type)); }
                    break;
                }
            }
        }
        else {
            try {
                UUID uuid = UUID.fromString(playerNameOrUUID);
                if (data.containsKey(uuid)) { sd = data.get(uuid).get(getType(type)); }
            }
            catch (Exception ignored) {}
        }
        return sd;
    }

    public boolean hasData(UUID uuid) { return data.containsKey(uuid); }

    public void clear(String uuid, int type) {
        Type t = getType(type);
        for (UUID id : data.keySet()) {
            if (uuid == null || id.toString().equals(uuid)) {
                SkinData sd = data.get(id).get(t);
                if (!sd.remove()) { data.get(id).remove(t); }
                if (uuid != null) {
                    if (CustomNpcs.Server != null) {
                        ServerPlayer player = CustomNpcs.Server.getPlayerList().getPlayer(id);
                        if (player != null) { // online
                            save();
                            sendToAll(player.getUUID());
                            return;
                        }
                    }
                    break;
                }
            }
        }
        save();
    }

    public SkinData create(UUID uuid, String player, int slot, int type, String location) {
        Type t = getType(slot);
        SkinData skinData = SkinData.create(t, null);
        if (type == 0) { skinData.setUrl(location); }
        else if (type == 1) { skinData.setLocation(location); }
        else { skinData.reset(location); }
        if (!data.containsKey(uuid)) { data.put(uuid, new EnumMap<>(Type.class)); }
        data.get(uuid).put(t, skinData);
        playerNames.put(uuid, player);
        save();
        sendToAll(uuid);
        return skinData;
    }

    public String getName(UUID uuid) {
        String name = playerNames.get(uuid);
        return name == null ? "" : name;
    }

}
