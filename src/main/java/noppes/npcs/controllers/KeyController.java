package noppes.npcs.controllers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.handler.IKeyBinding;
import noppes.npcs.api.handler.data.IKeySetting;
import noppes.npcs.controllers.data.KeyConfig;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSyncRemove;
import noppes.npcs.packets.client.PacketSyncUpdate;
import noppes.npcs.shared.common.util.LogWriter;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

public class KeyController implements IKeyBinding {

    protected static KeyController instance;
    protected final TreeMap<Integer, IKeySetting> keybindings = new TreeMap<>();

    public static KeyController getInstance() {
        if (instance == null) { instance = new KeyController(); }
        return instance;
    }

    @Override
    public KeyConfig createKeySetting() {
        KeyConfig ac = new KeyConfig(getUnusedId());
        keybindings.put(ac.getId(), ac);
        update(ac.getId());
        return ac;
    }

    @Override
    public KeyConfig getKeySetting(int id) { return (KeyConfig) keybindings.get(id); }

    public KeyConfig getKeySetting(String name, String category, int keyId, String modifier) {
        for (IKeySetting kb : keybindings.values()) {
            if (kb.getKeyId() != keyId) { continue; }
            KeyConfig kc = (KeyConfig) kb;
            if (!kc.name.equals(name) || !kc.category.equals(category)) { continue; }
            switch (modifier.toLowerCase()) {
                case "shift": {
                    if (kc.modifer == 1) { return kc; }
                    break;
                }
                case "control": {
                    if (kc.modifer == 2) { return kc; }
                    break;
                }
                case "alt": {
                    if (kc.modifer == 3) { return kc; }
                    break;
                }
                default: return kc;
            }
        }
        return null;
    }

    @Override
    public IKeySetting[] getKeySettings() { return keybindings.values().toArray(new IKeySetting[0]); }

    public CompoundTag getNBT() {
        ListTag list = new ListTag();
        for (int id : keybindings.keySet()) {
            CompoundTag nbtKey = ((KeyConfig) keybindings.get(id)).save();
            nbtKey.putInt("ID", id);
            list.add(nbtKey);
        }
        CompoundTag compound = new CompoundTag();
        compound.put("Data", list);
        return compound;
    }

    public int getUnusedId() {
        int id = 0;
        for (int i : keybindings.keySet()) {
            if (i >= id) { id = i + 1; }
        }
        return id;
    }

    private void loadDefaultKeys() {
        KeyConfig ac = new KeyConfig(0);
        keybindings.put(0, ac);
        save();
    }

    public void loadKey(CompoundTag nbtKey) {
        if (nbtKey == null || !nbtKey.contains("ID", 3) || nbtKey.getInt("ID") < 0) { return; }
        int id = nbtKey.getInt("ID");
        KeyConfig ac;
        if (keybindings.containsKey(id)) {
            ((KeyConfig) keybindings.get(id)).load(nbtKey);
            keybindings.get(id);
            return;
        }
        ac = new KeyConfig(id);
        ac.load(nbtKey);
        keybindings.put(id, ac);
        keybindings.get(id);
    }

    public void load() {
        CustomNpcs.debugData.start(null);
        File saveDir = CustomNpcs.Dir;
        if (saveDir == null) { return; }
        try {
            File file = new File(saveDir, "keys.dat");
            if (file.exists()) { load(file); }
            else { loadDefaultKeys(); }
        }
        catch (Exception e) { loadDefaultKeys(); }
        CustomNpcs.debugData.end(null);
    }

    private void load(File file) throws IOException { load(NbtIo.readCompressed(file)); }

    public void load(CompoundTag compound) {
        keybindings.clear();
        if (compound != null) {
            if (compound.contains("Data", 9)) {
                for (int i = 0; i < compound.getList("Data", 10).size(); ++i) {
                    loadKey(compound.getList("Data", 10).getCompound(i));
                }
            }
        }
    }

    @Override
    public void removeKeySetting(int id) { keybindings.remove(id); }

    public void save() {
        CustomNpcs.debugData.start(null);
        try { NbtIo.writeCompressed(getNBT(), new File(CustomNpcs.Dir, "keys.dat")); }
        catch (Exception e) { LogWriter.error(e); }
        CustomNpcs.debugData.end(null);
    }

    public void update(int id) {
        IKeySetting kb = keybindings.get(id);
        if (kb != null) { Packets.sendAll(new PacketSyncUpdate(0, 9, kb.getNbt().getMCNBT())); } // change or add
        else { Packets.sendAll(new PacketSyncRemove(id, 8)); } // remove
    }

}