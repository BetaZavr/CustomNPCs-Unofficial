package noppes.npcs.entity.data;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.DamageSource;
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
		for (Field f : DamageSource.class.getDeclaredFields()) {
			if (f.getType() != DamageSource.class) { continue; }
			try {
				if (!f.isAccessible()) { f.setAccessible(true); }
				DamageSource source = (DamageSource) f.get(DamageSource.class);
				if (source != null && source.damageType != null) {
					if (source.damageType.equals("generic") || source.damageType.equals("outOfWorld")) { continue; }
					allDamageNames.add(source.damageType);
				}
			}
			catch (Exception e) { LogWriter.error(e); }
		}
		File saveDir = CustomNpcs.getWorldSaveDirectory();
		if (saveDir == null) { return; }
		try {
			File file = new File(saveDir, "resistances.dat");
			if (file.exists()) {
				NBTTagCompound compound = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
				if (compound.hasKey("names", 9)) {
					for (int i = 0; i < compound.getTagList("names", 8).tagCount(); ++i) {
						String name = compound.getTagList("Data", 10).getStringTagAt(i);
						if (!name.isEmpty()) { allDamageNames.add(name); }
					}
				}
			} else { saveAll(); }
		} catch (Exception e) {
			LogWriter.error(e);
			saveAll();
		}
		CustomNpcs.debugData.end("Mod");
	}

	private static void saveAll() {
		CustomNpcs.debugData.start("Mod");
		File saveDir = CustomNpcs.getWorldSaveDirectory();
		if (saveDir == null) { return; }
		try {
			File file = new File(saveDir, "resistances.dat");
			NBTTagCompound compound = new NBTTagCompound();
			NBTTagList list = new NBTTagList();
			for (String name : allDamageNames) { list.appendTag(new NBTTagString(name)); }
			compound.setTag("names", list);
			CompressedStreamTools.writeCompressed(compound, Files.newOutputStream(file.toPath()));
		}
		catch (Exception e) { LogWriter.error(e); }
		CustomNpcs.debugData.end("Mod");
	}

	public static void add(String damageType) {
		if (damageType == null || damageType.isEmpty()) { return; }
		if (allDamageNames.isEmpty()) { loadAllDamages(); }
		if (allDamageNames.contains(damageType) ||
				damageType.equals("null") || damageType.equals("thrown") ||
				damageType.equals("player") ||damageType.equals("explosion.player") ||
				damageType.equals("generic") || damageType.equals("outOfWorld")) { return; }
		allDamageNames.add(damageType);
		Collections.sort(allDamageNames);
	}

	public Resistances() {
		data.put("arrow", 1.0f);
		data.put("mob", 1.0f);
		data.put("knockback", 1.0f);
		data.put("explosion", 1.0f);
		if (allDamageNames.isEmpty()) { loadAllDamages(); }
	}

	public final Map<String, Float> data = new HashMap<>();

	public float applyResistance(DamageSource source, float damage) {
		if (source.damageType.equals("arrow") || source.damageType.equals("thrown") || source.isProjectile()) {
			damage *= 2.0f - data.get("arrow");
		} else if (source.damageType.equals("player") || source.damageType.equals("mob")) {
			damage *= 2.0f - data.get("mob");
		} else if (source.damageType.equals("explosion") || source.damageType.equals("explosion.player")) {
			damage *= 2.0f - data.get("explosion");
		} else if (data.containsKey(source.damageType)) {
			damage *= 2.0f - data.get(source.damageType);
		}
		return damage;
	}

	public void load(NBTTagList list) {
		data.clear();
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound nbt = list.getCompoundTagAt(i);
			String key = nbt.getString("K");
			data.put(key, nbt.getFloat("V"));
			if (!allDamageNames.contains(key)) { allDamageNames.add(key); }
		}
	}
	
	public void oldLoad(NBTTagCompound compound) {
		data.put("arrow", compound.getFloat("Arrow"));
		data.put("mob", compound.getFloat("Melee"));
		data.put("knockback", compound.getFloat("Knockback"));
		data.put("explosion", compound.getFloat("Explosion"));
	}

	public NBTTagList save() {
		NBTTagList list = new NBTTagList();
		for (String key : data.keySet()) {
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setString("K", key);
			nbt.setFloat("V", data.get(key));
			list.appendTag(nbt);
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
