package noppes.npcs.entity.data;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.common.util.LogWriter;

public class Resistances {

    public static final List<String> allDamageNames = new ArrayList<>();

    private static void loadAllDamages() {
        CustomNpcs.debugData.start("Mod");
        allDamageNames.add("arrow");
        allDamageNames.add("mob");
        allDamageNames.add("knockback");
        allDamageNames.add("explosion");
        File saveDir = CustomNpcs.getLevelSaveDirectory();
        if (saveDir == null) { return; }
        try {
            File file = new File(saveDir, "resistances.dat");
            if (file.exists()) {
                DataInputStream stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))));
                CompoundTag compound = NbtIo.read(stream);
                if (compound.contains("names", 9)) {
                    for (int i = 0; i < compound.getList("names", 8).size(); ++i) {
                        String name = compound.getList("Data", 10).getString(i);
                        if (!name.isEmpty()) { allDamageNames.add(name); }
                    }
                }
                stream.close();
            } else { saveAll(); }
        } catch (Exception e) {
            LogWriter.error(e);
            saveAll();
        }
        CustomNpcs.debugData.end("Mod");
    }

    private static void saveAll() {
        CustomNpcs.debugData.start("Mod");
        File saveDir = CustomNpcs.getLevelSaveDirectory();
        if (saveDir == null) { return; }
        try {
            File file = new File(saveDir, "resistances.dat");
            CompoundTag compound = new CompoundTag();
            ListTag list = new ListTag();
            for (String name : allDamageNames) { list.add(StringTag.valueOf(name)); }
            compound.put("names", list);
            NbtIo.writeCompressed(compound, new FileOutputStream(file));
        }
        catch (Exception e) { LogWriter.error(e); }
        CustomNpcs.debugData.end("Mod");
    }

    public static void add(String damageType) {
        if (damageType == null || damageType.isEmpty()) { return; }
        if (allDamageNames.isEmpty()) { loadAllDamages(); }
        if (allDamageNames.contains(damageType) ||
                damageType.equals("null") || damageType.equals("thrown") ||
                damageType.equals("player") || damageType.equals("explosion.player") ||
                damageType.equals("generic") || damageType.equals("outOfWorld")) { return; }
        allDamageNames.add(damageType);
        Collections.sort(allDamageNames);
        saveAll();
    }

    public final Map<String, Float> data = new HashMap<>();

    public Resistances() {
        data.put("arrow", 1.0f);
        data.put("mob", 1.0f);
        data.put("knockback", 1.0f);
        data.put("explosion", 1.0f);
        if (allDamageNames.isEmpty()) { loadAllDamages(); }
    }

    public float applyResistance(DamageSource source, float damage) {
        if (source.getMsgId().equals("arrow") || source.getMsgId().equals("thrown") || source.is(DamageTypeTags.IS_PROJECTILE)) {
            damage *= 2.0f - data.get("arrow");
        } else if (source.getMsgId().equals("player") || source.getMsgId().equals("mob")) {
            damage *= 2.0f - data.get("mob");
        } else if (source.getMsgId().equals("explosion") || source.getMsgId().equals("explosion.player")) {
            damage *= 2.0f - data.get("explosion");
        } else if (data.containsKey(source.getMsgId())) {
            damage *= 2.0f - data.get(source.getMsgId());
        }
        return damage;
    }

    public void load(ListTag list) {
        data.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag nbt = list.getCompound(i);
            String key = nbt.getString("K");
            data.put(key, nbt.getFloat("V"));
            if (!allDamageNames.contains(key)) { allDamageNames.add(key); }
        }
    }

    public void oldLoad(CompoundTag compound) {
        data.put("arrow", compound.getFloat("Arrow"));
        data.put("mob", compound.getFloat("Melee"));
        data.put("knockback", compound.getFloat("Knockback"));
        data.put("explosion", compound.getFloat("Explosion"));
    }

    public ListTag save() {
        ListTag list = new ListTag();
        for (String key : data.keySet()) {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("K", key);
            nbt.putFloat("V", data.get(key));
            list.add(nbt);
        }
        return list;
    }

    public float get(String damageName) {
        if (data.containsKey(damageName)) { return data.get(damageName); }
        if (damageName.equals("explosion.player") && data.containsKey("explosion")) { return data.get("explosion"); }
        if (damageName.equals("player") && data.containsKey("mob")) { return data.get("mob"); }
        if (damageName.equals("thrown") && data.containsKey("arrow")) { return data.get("arrow"); }
        return 1.0f;
    }

}
