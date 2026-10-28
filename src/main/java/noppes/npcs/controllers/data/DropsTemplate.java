package noppes.npcs.controllers.data;

import java.util.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.entity.data.DropSet;

public class DropsTemplate {

    public final Map<String, Map<Integer, DropSet>> groups = new TreeMap<>(); // <id, <pos, drop>>
    private boolean allDropsFromGroup = false; // or random
    private final Random rnd = new Random();

    public DropsTemplate() { groups.put("default", new TreeMap<>()); }

    public DropsTemplate(CompoundTag nbtTemplate) {
        this();
        load(nbtTemplate);
    }

    public DropSet addDropItem(String id, ItemStack item, double chance) {
        if (id == null ||id.isEmpty()) { id = "Group #" + groups.size(); }
        if (!groups.containsKey(id)) { groups.put(id, new TreeMap<>()); }
        DropSet ds = new DropSet(null);
        ds.item = item;
        ds.setChance(chance);
        ds.pos = groups.get(id).size();
        groups.get(id).put(ds.pos, ds);
        return ds;
    }

    public List<DropSet> getDrops() {
        List<DropSet> allDrops = new ArrayList<>();
        for (String groupId : groups.keySet()) {
            ArrayList<DropSet> preList = new ArrayList<>(groups.get(groupId).values());
            if (preList.isEmpty()) { continue; }
            if (allDropsFromGroup) { allDrops.addAll(preList); }
            else { allDrops.add(preList.get(rnd.nextInt(preList.size()))); }
        }
        return allDrops;
    }

    public CompoundTag getNBT() {
        CompoundTag nbtTemplate = new CompoundTag();
        nbtTemplate.putBoolean("DropType", allDropsFromGroup);
        for (String id : groups.keySet()) {
            ListTag list = new ListTag();
            for (DropSet ds : groups.get(id).values()) { list.add(ds.save()); }
            nbtTemplate.put("Group_" + id, list);
        }
        return nbtTemplate;
    }

    public void load(CompoundTag nbtTemplate) {
        if (nbtTemplate.contains("DropType", 3)) { allDropsFromGroup = nbtTemplate.getInt("DropType") == 3; }
        else if (nbtTemplate.contains("DropType", 1)) { allDropsFromGroup = nbtTemplate.getBoolean("DropType"); }
        groups.clear();
        Set<String> keys = nbtTemplate.getAllKeys();
        for (String id : keys) {
            if (!id.isEmpty()) {
                if (nbtTemplate.getList(id, 10).isEmpty()) { groups.put(id, new TreeMap<>()); }
                else {
                    for (int j = 0; j < nbtTemplate.getList(id, 10).size(); j++) {
                        DropSet ds = new DropSet(null);
                        ds.load(nbtTemplate.getList(id, 10).getCompound(j));
                        ds.pos = j;
                        if (!groups.containsKey(id)) { groups.put(id, new TreeMap<>()); }
                        groups.get(id).put(ds.pos, ds);
                    }
                }
            }
        }
    }

    public void removeDrop(String groupId, int slot) {
        if (!groups.containsKey(groupId) || !groups.get(groupId).containsKey(slot)) { return; }
        if (groups.get(groupId).remove(slot) != null) {
            int j = 0;
            for (int s : groups.get(groupId).keySet()) { groups.get(groupId).get(s).pos = j++; }
        }
    }

    public void removeGroup(String groupId) { groups.remove(groupId); }

    public static DropsTemplate from(DropsTemplate dropTemplate) {
        DropsTemplate dt = new DropsTemplate();
        if (dropTemplate != null) { dt.load(dropTemplate.getNBT()); }
        return dt;
    }

}
