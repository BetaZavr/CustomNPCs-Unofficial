package noppes.npcs.util;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.*;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import noppes.npcs.CustomEntities;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.IMethods;
import noppes.npcs.api.INbt;
import noppes.npcs.api.IPos;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.api.util.IRayTraceResults;
import noppes.npcs.api.util.IRayTraceRotate;
import noppes.npcs.api.util.IRayTraceVec;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.client.TranslateUtil;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.world.level.entity.IEntitySectionStorageMixin;
import noppes.npcs.mixin.world.level.entity.ILevelEntityGetterAdapterMixin;
import noppes.npcs.packets.server.SPacketDimensionTeleport;
import noppes.npcs.shared.common.util.LogWriter;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Util implements IMethods {

    private static final TreeMap<Integer, String> ROMAN_DIGITS = new TreeMap<>() {{
        put(1, "I");
        put(5, "V");
        put(10, "X");
        put(50, "L");
        put(100, "C");
        put(500, "D");
        put(1000, "M");
    }};
    private static final Gson gson = new Gson();
    public static final Util instance = new Util();
    protected static Field entityStorage;
    public static Object temp;

    public static List<String> splitString(String input) {
        List<String> result = new ArrayList<>();
        if (input != null && !input.isEmpty()) {
            byte[] abyte = input.getBytes(StandardCharsets.UTF_8);
            StringBuilder temp = new StringBuilder();
            int size = 0;
            for (int i = 0; i < abyte.length; i++) {
                if (size + abyte[i] > 32768) {
                    result.add(temp.toString());
                    temp = new StringBuilder("" + input.charAt(i));
                    size = 0;
                }
                else {
                    temp.append(input.charAt(i));
                    size += abyte[i];
                }
            }
            if (!temp.toString().isEmpty()) { result.add(temp.toString()); }
        }
        return result;
    }

    //public static boolean hasInternet = true;

    /** Correct deletion of folders */
    @Override
    public boolean removeFile(File directory) {
        if (directory == null || !directory.exists()) { return false; }
        LogWriter.debug("Trying remove file \"" + directory + "\"");
        if (!directory.isDirectory()) { return directory.delete(); }
        File[] list = directory.listFiles();
        if (list != null) {
            for (File tempFile : list) {
                if (!removeFile(tempFile)) { LogWriter.info("Not removing file \"" + tempFile + "\""); }
            }
        }
        return directory.delete();
    }

    @Override
    public String loadFile(File file) {
        LogWriter.debug("Load file \"" + file.getAbsolutePath() + "\"");
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append((char) 10);
            }
            reader.close();
        }
        catch (Exception e) { LogWriter.error("Error load file \"" + file.getAbsolutePath() + "\"", e); }
        return text.toString();
    }

    @Override
    public boolean saveFile(File file, String text) {
        if (file == null || text == null) { return false; }
        LogWriter.debug("Save text to file \"" + file.getAbsolutePath() + "\"");
        if (file.getParentFile() != null && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) { // create directories
            LogWriter.debug("Error creating directories from file path \"" + file.getAbsolutePath() + "\"");
            return false;
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
            writer.write(text);
        } catch (IOException e) {
            LogWriter.debug("Error Save Default Item File \"" + file.getAbsolutePath() + "\"");
            return false;
        }
        return true;
    }

    public boolean saveFile(File file, CompoundTag nbt) {
        return saveFile(file, NBTJsonUtil.Convert(nbt));
    }

    @Override
    public boolean saveFile(File file, INbt nbt) {
        if (nbt == null || nbt.getMCNBT() == null) { return false; }
        return saveFile(file, NBTJsonUtil.Convert(nbt.getMCNBT()));
    }

    @SuppressWarnings("unused")
    public String translateGoogle(Player player, String originalText) {
        return translateGoogle("en", CustomNpcs.proxy.getTranslateLanguage(player), originalText);
    }

    @Override
    public String translateGoogle(String textLanguageKey, String translationLanguageKey, String originalText) {
        return TranslateUtil.translate(textLanguageKey, translationLanguageKey, originalText);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Tag writeObjectToNbt(Object value) {
        if (value == null) { return null; }
        if (value instanceof Tag || value instanceof INbt) {
            CompoundTag compound = new CompoundTag();
            compound.putBoolean("IsNBT", value instanceof Tag);
            if (value instanceof Tag) { compound.put("V", (Tag) value); }
            else { compound.put("V", ((INbt) value).getMCNBT());}
            return compound;
        }
        else if (value.getClass().isArray()) {
            Object[] values = (Object[]) value;
            ListTag list = new ListTag();
            for (Object v : values) {
                Tag tag = writeObjectToNbt(v);
                if (tag != null) { list.add(tag); }
            }
            return list;
        }
        else if (value instanceof Boolean) {
            CompoundTag compound = new CompoundTag();
            compound.putBoolean("IsBoolean", true);
            compound.putBoolean("V", (Boolean) value);
            return compound;
        }
        else if (value instanceof Byte) { return ByteTag.valueOf((byte) value); }
        else if (value instanceof Short) { return ShortTag.valueOf((short) value); }
        else if (value instanceof Integer) { return IntTag.valueOf((int) value); }
        else if (value instanceof Color) {
            CompoundTag compound = new CompoundTag();
            compound.putBoolean("IsColor", true);
            compound.putInt("V", ((Color) value).getRGB());
            return compound;
        }
        else if (value instanceof Long) { return LongTag.valueOf((long) value); }
        else if (value instanceof Float) { return FloatTag.valueOf((float) value); }
        else if (value instanceof Double) { return DoubleTag.valueOf((double) value); }
        else if (value instanceof String) { return StringTag.valueOf((String) value); }
        else if (value instanceof Number) { return DoubleTag.valueOf(((Number) value).doubleValue()); }
        else if (value instanceof Bindings) {
            String clazz = value.toString();
            if (!clazz.equals("[object Array]") && !clazz.equals("[object Object]")) { return null; }
            boolean isArray = clazz.equals("[object Array]");
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("IsArray", isArray);
            nbt.putBoolean("IsBindings", true);
            for (Map.Entry<String, Object> scopeEntry : ((Bindings) value).entrySet()) {
                Object v = scopeEntry.getValue();
                if (v.getClass().isArray()) {
                    Object[] vs = (Object[]) v;
                    if (vs.length == 0) { nbt.put(scopeEntry.getKey(), new ListTag()); }
                    else if (vs[0] instanceof Byte) {
                        List<Byte> l = new ArrayList<>();
                        for (Object va : vs) {
                            if (va instanceof Byte) { l.add((Byte) va); }
                        }
                        byte[] arr = new byte[l.size()];
                        int i = 0;
                        for (byte d : l) {
                            arr[i] = d;
                            i++;
                        }
                        nbt.putByteArray(scopeEntry.getKey(), arr);
                    }
                    else if (vs[0] instanceof Integer) {
                        List<Integer> l = new ArrayList<>();
                        for (Object va : vs) {
                            if (va instanceof Integer) { l.add((Integer) va); }
                        }
                        int[] arr = new int[l.size()];
                        int i = 0;
                        for (int d : l) {
                            arr[i] = d;
                            i++;
                        }
                        nbt.putIntArray(scopeEntry.getKey(), arr);
                    }
                    else if (vs[0] instanceof Long) {
                        List<Long> l = new ArrayList<>();
                        for (Object va : vs) {
                            if (va instanceof Long) { l.add((Long) va); }
                        }
                        long[] arr = new long[l.size()];
                        int i = 0;
                        for (long d : l) {
                            arr[i] = d;
                            i++;
                        }
                        nbt.put(scopeEntry.getKey(), new LongArrayTag(arr));
                    }
                    else if (vs[0] instanceof String) {
                        ListTag list = new ListTag();
                        for (Object va : vs) { list.add(StringTag.valueOf((String) va)); }
                        nbt.put(scopeEntry.getKey(), list);
                    }
                    else if (vs[0] instanceof Short || vs[0] instanceof Float || vs[0] instanceof Double || vs[0] instanceof Number) {
                        ListTag list = new ListTag();
                        for (Object va : vs) {
                            double d;
                            if (va instanceof Short) { d = (double) (Short) va; }
                            else if (va instanceof Float) { d = (double) (Float) va; }
                            else if (va instanceof Double) { d = (Double) va; }
                            else if (va instanceof Number) { d = ((Number) va).doubleValue(); }
                            else { continue; }
                            list.add(DoubleTag.valueOf(d));
                        }
                        nbt.put(scopeEntry.getKey(), list);
                    }
                    else { nbt.put(scopeEntry.getKey(), new ListTag()); }
                }
                else if (v instanceof Byte) { nbt.putByte(scopeEntry.getKey(), (Byte) v); }
                else if (v instanceof Short) { nbt.putShort(scopeEntry.getKey(), (Short) v); }
                else if (v instanceof Integer) { nbt.putInt(scopeEntry.getKey(), (Integer) v); }
                else if (v instanceof Long) { nbt.putLong(scopeEntry.getKey(), (Long) v); }
                else if (v instanceof Float) { nbt.putFloat(scopeEntry.getKey(), (Float) v); }
                else if (v instanceof Double) { nbt.putDouble(scopeEntry.getKey(), (Double) v); }
                else if (v instanceof Number) {nbt.putDouble(scopeEntry.getKey(), ((Number) v).doubleValue()); }
                else if (v instanceof String) { nbt.putString(scopeEntry.getKey(), (String) v); }
                else {
                    Tag n = writeObjectToNbt(v);
                    if (n != null) { nbt.put(scopeEntry.getKey(), n); }
                }
            }
            return nbt;
        }
        else if (value instanceof Map) {
            try {
                Map<Object, Object> map = (Map<Object, Object>) value;
                CompoundTag compound = new CompoundTag();
                int type = 0; // HashMap
                if (value instanceof TreeMap) { type = 1; }
                else if (value instanceof LinkedHashMap) { type = 2; }
                else if (value instanceof LinkedTreeMap) { type = 3; }
                compound.putInt("IsMap", type);
                CompoundTag content = new CompoundTag();
                int i = 0;
                for (Object key : map.keySet()) {
                    Tag k = writeObjectToNbt(key);
                    Tag v = writeObjectToNbt(map.get(key));
                    if (k != null && v != null) {
                        CompoundTag nbt = new CompoundTag();
                        nbt.put("K", k);
                        nbt.put("V", v);
                        content.put("Slot_"+i, nbt);
                    }
                    i++;
                }
                compound.put("Content", content);
                return compound;
            }
            catch (Exception ignored) { }
        }
        else if (value instanceof List) {
            try {
                List<Object> list = (List<Object>) value;
                CompoundTag compound = new CompoundTag();
                compound.putBoolean("IsList", true);
                int i = 0;
                for (Object obj : list) {
                    Tag tag = writeObjectToNbt(obj);
                    if (tag == null) { continue; }
                    compound.put("K" + i, tag);
                    i++;
                }
                return compound;
            }
            catch (Exception ignored) { }
        }
        try {
            String jsonString = gson.toJson(value);
            Object obj = gson.fromJson(jsonString, value.getClass());
            if (obj != null) {
                CompoundTag compound = new CompoundTag();
                compound.putBoolean("IsJSON", true);
                compound.putString("Class", value.getClass().getName());
                compound.putString("Content", jsonString);
                return compound;
            }
        }
        catch (Exception ignored) {  }
        LogWriter.warn("Not write object: \""+value+"\" to NBT");
        return null;
    }

    @Override
    public Object readObjectFromNbt(@Nullable Tag tag) {
        if (tag == null) { return null; }
        if (tag instanceof CompoundTag compound) {
            if (compound.isEmpty()) { return null; }
            if (compound.getBoolean("IsBindings")) {
                ScriptEngine engine = ScriptController.Instance.getEngineByName("ECMAScript");
                if (engine == null) { return null; }
                boolean isArray = compound.getBoolean("IsArray");
                try {
                    StringBuilder str = new StringBuilder("JSON.parse('" + (isArray ? "[" : "{"));
                    Set<String> sets = ((CompoundTag) tag).getAllKeys();
                    Map<String, Object> map = new TreeMap<>();
                    for (String k : sets) {
                        if (k.equals("IsArray") || k.equals("IsBindings")) { continue; }
                        Object v = readObjectFromNbt(((CompoundTag) tag).get(k));
                        if (v != null) { map.put(k, v); }
                    }
                    for (String k : map.keySet()) {
                        String s = getJSONStringFromObject(map.get(k));
                        if (isArray) { str.append(s).append(", "); }
                        else { str.append("\"").append(k).append("\":").append(s).append(", "); }
                    }
                    if (!map.isEmpty()) { str = new StringBuilder(str.substring(0, str.length() - 2)); }
                    str.append(isArray ? "]" : "}").append("')");
                    try { return engine.eval("" +str); }
                    catch (Exception e) {
                        LogWriter.error("Error parse \""+str+"\"", e);
                    }
                    return null;
                } catch (Exception e) { LogWriter.error(e); }
            }
            else if (compound.getBoolean("IsList")) {
                Map<Integer, Object> map = new TreeMap<>();
                for (String k : compound.getAllKeys()) {
                    try { map.put(Integer.parseInt(k.replace("K", "")), readObjectFromNbt(Objects.requireNonNull(compound.get(k)))); }
                    catch (Exception e) { map.put(map.size(), null); }
                }
                return new ArrayList<>(map.values());
            }
            else if (compound.contains("IsMap", 3)) {
                Map<Object, Object> map = switch (compound.getInt("IsMap")) {
                    case 1 -> new TreeMap<>();
                    case 2 -> new LinkedHashMap<>();
                    case 3 -> new LinkedTreeMap<>();
                    default -> new HashMap<>();
                };
                CompoundTag content = compound.getCompound("Content");
                Map<Integer, CompoundTag> keys = new TreeMap<>();
                for (String key : content.getAllKeys()) {
                    try	{ keys.put( Integer.parseInt(key.replace("Slot_", "")), content.getCompound(key)); } catch (Exception ignored) { }
                }
                for (CompoundTag nbt : keys.values()) {
                    Object k = readObjectFromNbt(nbt.get("K"));
                    Object v = readObjectFromNbt(nbt.get("V"));
                    if (k != null && v != null) { map.put(k, v); }
                }
                return map;
            }
            else if (compound.contains("IsNBT", 1)) {
                Tag nbt = compound.get("V");
                if (!compound.getBoolean("IsNBT") && nbt instanceof CompoundTag) { return new NBTWrapper((CompoundTag) nbt); }
                return nbt;
            }
            else if (compound.getBoolean("IsJSON")) {
                try {
                    Class<?> clss = Class.forName(compound.getString("Class"));
                    return gson.fromJson(compound.getString("Content"), clss);
                } catch (Exception ignored) { }
            }
            else if (compound.getBoolean("IsColor")) { return new Color(compound.getInt("V")); }
            else if (compound.getBoolean("IsBoolean")) { return compound.getBoolean("V"); }
        }
        else if (tag instanceof EndTag) { return null; }
        else if (tag instanceof ByteTag) { return ((ByteTag) tag).getAsByte(); }
        else if (tag instanceof ShortTag) { return ((ShortTag) tag).getAsShort(); }
        else if (tag instanceof IntTag) { return ((IntTag) tag).getAsInt(); }
        else if (tag instanceof LongTag) { return ((LongTag) tag).getAsLong(); }
        else if (tag instanceof FloatTag) { return ((FloatTag) tag).getAsFloat(); }
        else if (tag instanceof DoubleTag) { return ((DoubleTag) tag).getAsDouble(); }
        else if (tag instanceof StringTag) { return tag.getAsString(); }
        else if (tag instanceof ByteArrayTag) { return ((ByteArrayTag) tag).getAsByteArray(); }
        else if (tag instanceof IntArrayTag) { return ((IntArrayTag) tag).getAsIntArray(); }
        else if (tag instanceof LongArrayTag) { return ((LongArrayTag) tag).getAsLongArray(); }
        else if (tag instanceof ListTag list) {
            Object[] arr = new Object[list.size()];
            int i = 0;
            for (Tag listTag : list) {
                arr[i] = readObjectFromNbt(listTag);
                i++;
            }
            return arr;
        }
        LogWriter.warn("Not read tag: \"" + tag + "\"; type: " + tag.getId() + " to Object");
        return null;
    }

    @Override
    public @NotNull IEntity<?> transferEntity(IEntity<?> entity, String dimensionId, IPos pos) {
        if (entity.getWorld().getMCLevel().isClientSide() || entity.getWorld().getMCLevel().getServer() == null) { return entity; }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionId));
        ServerLevel level = entity.getWorld().getMCLevel().getServer().getLevel(dimension);
        if (level == null) { return entity; }
        return Objects.requireNonNull(NpcAPI.Instance()).getIEntity(transferEntity(entity.getMCEntity(), level, pos.getX(), pos.getY(), pos.getZ(), entity.getRotation(), entity.getPitch()));
    }

    public @NotNull Entity transferEntity(Entity entity, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        if (entity.level().isClientSide() || level == null) { return entity; }
        if (entity instanceof ServerPlayer player) {
            SPacketDimensionTeleport.teleportPlayer(player, level.dimension(), x, y, z, yaw, pitch);
        }
        else {
            // e.teleportTo()
            if (level == entity.level()) {
                entity.moveTo(x, y, z, yaw, pitch);
                // teleportPassengers()
                entity.getSelfAndPassengers().forEach((passenger) -> {
                    Entity.MoveFunction moveFunction = Entity::moveTo;
                    for (Entity newPass : passenger.getPassengers()) {
                        if (newPass.hasPassenger(newPass)) {
                            double d0 = entity.getY() + entity.getPassengersRidingOffset() + newPass.getMyRidingOffset();
                            moveFunction.accept(newPass, entity.getX(), d0, entity.getZ());
                        }
                    }
                });
                entity.setYHeadRot(yaw);
            } else {
                entity.unRide();
                Entity newE = entity.getType().create(level);
                if (newE == null) { return entity; }
                newE.restoreFrom(entity);
                newE.moveTo(x, y, z, yaw, pitch);
                newE.setYHeadRot(yaw);
                newE.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
                level.addDuringTeleport(newE);
                return newE;
            }
        }
        return entity;
    }

    @Override
    public @Nonnull String getJSONStringFromObject(Object obj) {
        if (obj == null) { return ""; }
        LogWriter.debug("Write object \"" + obj.getClass().getName() + "\" to JSON string");
        StringBuilder str = new StringBuilder();
        if (obj.getClass().isArray()) {
            str = new StringBuilder("[");
            for (Object value : (Object[]) obj) {
                String s = getJSONStringFromObject(value);
                if (!str.isEmpty()) { str.append(", "); }
                str.append(s);
            }
            str.append("]");
        }
        else if (obj instanceof Number) { str = new StringBuilder(obj.toString()); }
        else if (obj instanceof String) { str = new StringBuilder("'" + obj + "'"); }
        else if (obj instanceof Bindings) {
            ScriptEngine engine = ScriptController.Instance.getEngineByName("ECMAScript");
            if (engine != null) {
                engine.put("temp", obj);
                try { str = new StringBuilder((String) engine.eval("JSON.stringify(temp)")); }
                catch (ScriptException e) { LogWriter.error("Error:", e); }
            }
        }
        return str.toString();
    }

    public boolean canMoveEntityToEntity(EntityNPCInterface entity, LivingEntity entityTo) {
        if (entity == null || entityTo == null) { return false; }
        Path path = entity.getNavigation().createPath(entityTo, 1);
        if (path == null) { return false; }
        Node pos = path.getEndNode();
        if (pos == null) { return false; }
        return Math.abs(entityTo.getX() - (double) pos.x) <= 1.0 && Math.abs(entityTo.getY() - (double) pos.y) < 2.0d && Math.abs(entityTo.getZ() - (double) pos.z) <= 1.0d;
    }

    @Override
    public String ticksToElapsedTime(long ticks, boolean isMilliSeconds, boolean colored, boolean upped) {
        String time = isMilliSeconds ? "0.000" : "--/--";
        String chr = "" + ((char) 167);
        if (ticks < 0) {
            return (colored ? chr + "8" : "") + time;
        }
        long timeSeconds = (isMilliSeconds ? ticks : ticks * 50L) / 1000L;
        int ms = (int) ((isMilliSeconds ? ticks : ticks * 50L) % 1000L);
        int sec = (int) (timeSeconds % 60L);
        int min = (int) (timeSeconds % 3600L) / 60;
        int hour = (int) (timeSeconds % 86400L) / 3600;
        int day = (int) (timeSeconds % 2592000L) / 86400;
        int month = (int) (timeSeconds % 31449600L) / 2620800;
        int year = (int) (timeSeconds / 31449600L);
        String mins, secs;
        if (min < 10) {
            mins = "0" + min;
        } else {
            mins = "" + min;
        }
        if (sec < 10) {
            secs = "0" + sec;
        } else {
            secs = "" + sec;
        }
        time = "";
        if (year > 0) {
            if (colored) {
                time += chr + "r" + year + chr + "6y ";
            } else {
                time += year + "y ";
            }
        }
        if (upped && !time.isEmpty()) {
            return time;
        }
        if (month > 0) {
            if (colored) {
                time += chr + "r" + month + chr + "1m ";
            } else {
                time += month + "m ";
            }
        }
        if (upped && !time.isEmpty()) {
            return time;
        }
        if (day > 0) {
            if (colored) {
                time += chr + "r" + day + chr + "2d ";
            } else {
                time += day + "d ";
            }
        }
        if (upped && !time.isEmpty()) {
            return time;
        }
        if (hour > 0 || year > 0 || month > 0 || day > 0) {
            if (colored) {
                time += chr + "r" + hour + ":";
            } else {
                time += hour + ":";
            }
        }
        time += (colored ? chr + "r" : "") + mins + ":" + secs;
        if (isMilliSeconds) {
            StringBuilder mss = new StringBuilder("" + ms);
            while (mss.length() < 3) { mss.insert(0, "0"); }
            time += (colored ? chr + "8" : "") + "." + mss;
        }
        return time;
    }

    @Override
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    public String deleteColor(String input) {
        if (input == null || input.isEmpty()) { return input; }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // Check for Minecraft color/format prefix: §, &, or U+FFFF
            if (c == '\u00A7' || c == '&' || c == '\uFFFF') {
                // If next char exists and is a valid format code, skip both
                if (i + 1 < input.length()) {
                    char next = input.charAt(i + 1);
                    if ((next >= '0' && next <= '9') ||
                            (next >= 'a' && next <= 'f') ||
                            (next >= 'A' && next <= 'F') ||
                            (next >= 'k' && next <= 'o') ||
                            (next >= 'K' && next <= 'O') ||
                            next == 'r' || next == 'R') {
                        i++; // skip the format code character
                        continue;
                    }
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    @Override
    public double distanceTo(double x0, double y0, double z0, double x1, double y1, double z1) {
        double d0 = x0 - x1;
        double d1 = y0 - y1;
        double d2 = z0 - z1;
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }

    public double distanceTo(Entity entity, Entity target) {
        if (entity == null || target == null) { return 0.0d; }
        return distanceTo(entity.getX(), entity.getY(), entity.getZ(), target.getX(), target.getY(), target.getZ());
    }

    @Override
    public double distanceTo(IEntity<?> entity, IEntity<?> target) {
        if (entity == null || target == null) { return 0.0d; }
        return distanceTo(entity.getX(), entity.getY(), entity.getZ(), target.getX(), target.getY(), target.getZ());
    }

    public IRayTraceRotate getAngles3D(Entity entity, Entity target) {
        if (entity == null || target == null) { return RayTraceRotate.EMPTY; }
        return Util.instance.getAngles3D(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
    }

    @Override
    public @Nonnull IRayTraceRotate getAngles3D(double x0, double y0, double z0, double x1, double y1, double z1) {
        RayTraceRotate rtr = new RayTraceRotate();
        rtr.calculate(x0, y0, z0, x1, y1, z1);
        return rtr;
    }

    @Override
    public IRayTraceRotate getAngles3D(IEntity<?> entity, IEntity<?> target) {
        if (entity == null || target == null) { return RayTraceRotate.EMPTY; }
        return getAngles3D(entity.getMCEntity(), target.getMCEntity());
    }

    @Override
    public List<File> getFiles(File directory, String index) {
        if (CustomNpcs.VerboseDebug && temp == null) {
            temp = new Object[] { directory, System.currentTimeMillis(), 0, 1 };
        }
        List<File> list = new ArrayList<>();
        if (directory != null && directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                if (temp instanceof Object[] objs && objs.length > 3 && objs[0] == directory) {
                    int i = 0;
                    for (File f : files) {
                        if (f.isDirectory()) { i++; }
                    }
                    objs[3] = i;
                }
                for (File f : files) {
                    if (f.isDirectory()) {
                        list.addAll(getFiles(f, index));
                        if (temp instanceof Object[] objs && objs.length > 3 && objs[0] == directory) {
                            objs[2] = ((int) objs[2]) + 1;
                            LogWriter.debug(ticksToElapsedTime(System.currentTimeMillis() - (long) objs[1], true, false, false) +
                                    " ... process found files["+objs[2]+"/"+objs[3]+"] in \"" + objs[0] + "\"; now: \""+f+"\"");
                        }
                    }
                    else {
                        if (!f.isFile() || (index != null && !index.isEmpty() && !f.getName().toLowerCase().endsWith(index.toLowerCase()))) { continue; }
                        list.add(f);
                    }
                }
            }
        }
        if (temp instanceof Object[] objs && objs.length > 0 && objs[0] == directory) { temp = null; }
        return list;
    }

    @Override
    public String getTextNumberToRoman(int value) {
        if (value > 3999) { return "" + value; }
        StringBuilder sb = new StringBuilder();
        for (int key : ROMAN_DIGITS.descendingKeySet()) {
            while (value >= key) {
                sb.append(ROMAN_DIGITS.get(key));
                value -= key;
            }
        }
        String total = sb.toString();
        if (total.contains("IIII")) {
            if (total.contains("VIIII")) { total = total.replace("VIIII", "IX"); }
            else { total = total.replace("IIII", "IV"); }
        }
        return total;
    }

    @Override
    public String getTextReducedNumber(double value, boolean isInteger, boolean color, boolean notPfx) {
        if (value == 0.0d) {
            return isInteger ? "0" : String.valueOf(value).replace(".", ",");
        }
        String chr = "" + ((char) 167);
        String chrPR= "" + ((char) 8776);
        String type = "";
        String sufc = "";
        double corr = value;
        int exp;
        boolean negatively = false;

        if (value <= 0) {
            negatively = true;
            value *= -1.0d;
        }
        if (value < Math.pow(10, 3)) { // xxxx,x hecto
            corr = Math.round(value * 10.0d) / 10.0d;
        } else if (value < Math.pow(10, 6)) { // xxx,xxK kilo
            corr = Math.round(value / 100.0d) / 10.0d;
            if (color) {
                type = chr + "e";
            }
            type += "K";
            if (corr * Math.pow(10, 3) != value) {
                sufc = chrPR;
            }
        } else if (value < Math.pow(10, 9)) { // xxx,xxM mega
            corr = Math.round(value / Math.pow(10, 3)) / 10.0d;
            if (color) {
                type = chr + "a";
            }
            type += "M";
            if (corr * Math.pow(10, 6) != value) {
                sufc = chrPR;
            }
        } else if (value < Math.pow(10, 12)) { // xxx,xxG giga
            corr = Math.round(value / Math.pow(10, 6)) / 10.0d;
            if (color) {
                type = chr + "2";
            }
            type += "G";
            if (corr * Math.pow(10, 9) != value) {
                sufc = chrPR;
            }
        } else if (value < Math.pow(10, 15)) { // xxx,xxT tera
            corr = Math.round(value / Math.pow(10, 9)) / 10.0d;
            if (color) {
                type = chr + "b";
            }
            type += "T";
            if (corr * Math.pow(10, 12) != value) {
                sufc = chrPR;
            }
        } else if (value < Math.pow(10, 18)) { // xxx, xxP peta
            corr = Math.round(value / Math.pow(10, 12)) / 10.0d;
            if (color) {
                type = chr + "3";
            }
            type += "P";
            if (corr * Math.pow(10, 15) != value) {
                sufc = chrPR;
            }
        } else if (value < Math.pow(10, 21)) { // xxx, xxE hexa
            corr = Math.round(value / Math.pow(10, 15)) / 10.0d;
            if (color) {
                type = chr + "9";
            }
            type += "E";
            if (corr * Math.pow(10, 18) != value) {
                sufc = chrPR;
            }
        } else if (value < Math.pow(10, 24)) { // xxx, xxZ zetta
            corr = Math.round(value / Math.pow(10, 18)) / 10.0d;
            if (color) {
                type = chr + "d";
            }
            type += "Z";
            if (corr * Math.pow(10, 21) != value) {
                sufc = chrPR;
            }
        } else if (value < Math.pow(10, 27)) { // xxx, xxY yotta
            corr = Math.round(value / Math.pow(10, 21)) / 10.0d;
            if (color) {
                type = chr + "5";
            }
            type += "Y";
            if (corr * Math.pow(10, 24) != value) {
                sufc = chrPR;
            }
        } else { // x, xxxe + exp
            if (String.valueOf(value).contains("e+") || String.valueOf(value).contains("E+")) {
                String index = "e+";
                if (String.valueOf(value).contains("E+")) {
                    index = "E+";
                }
                exp = Integer.parseInt(String.valueOf(value).substring(String.valueOf(value).indexOf(index) + 2));
                corr = Math
                        .round(Integer.parseInt(String.valueOf(value).substring(0, String.valueOf(value).indexOf(index)))
                                * 1000.0d)
                        / 1000.0d;
            } else {
                exp = String.valueOf(corr).length();
                corr = value;
            }
            type = "E+" + exp;
        }
        if (negatively) { // negative or zero
            if (color) {
                sufc = chr + "c";
            }
            if (corr != 0.0d) {
                sufc += "-";
            }
        }
        String end = "";
        if (color) {
            end = chr + "r";
        }
        if (notPfx) {
            sufc = "";
        }
        String num = isInteger ? ("" + (long) corr) : ("" + corr).replace(".", ",");
        return sufc + num + type + end;
    }

    public Dist getSide() {
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            return Dist.DEDICATED_SERVER;
        }
        if ("Server thread".equals(Thread.currentThread().getName())) {
            return Dist.DEDICATED_SERVER;
        }
        return Dist.CLIENT;
    }

    @Override
    public String getDataFile(String fileName) {
        if (fileName == null) { return ""; }
        LogWriter.info("Get text from mod data file \"" + fileName + "\"");
        InputStream inputStream = getModInputStream(fileName);
        String text = "";
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = inputStream.read(buffer)) != -1; ) { result.write(buffer, 0, length); }
            text = result.toString(StandardCharsets.UTF_8);
        }
        catch (Exception e) { LogWriter.error("Error get text from mod data file: \"" + fileName + "\"; InputStream: " + inputStream, e); }
        return text;
    }

    public IRayTraceVec getPosition(BlockPos pos, double yaw, double pitch, double radius) {
        if (pos == null) { return RayTraceVec.EMPTY; }
        return Util.instance.getPosition(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, yaw, pitch, radius);
    }

    @Override
    public IRayTraceVec getPosition(double x, double y, double z, double yaw, double pitch, double radius) {
        RayTraceVec rtv = new RayTraceVec();
        rtv.calculatePos(x, y, z, yaw, pitch, radius);
        return rtv;
    }

    @Override
    public IRayTraceVec getPosition(IEntity<?> entity, double yaw, double pitch, double radius) {
        if (entity == null) { return RayTraceVec.EMPTY; }
        return getPosition(entity.getMCEntity().getX(), entity.getMCEntity().getY(), entity.getMCEntity().getZ(), yaw, pitch, radius);
    }

    @Override
    public IRayTraceVec getVector3D(double x0, double y0, double z0, double x1, double y1, double z1) {
        RayTraceVec rtv = new RayTraceVec();
        rtv.calculateVec(x0, y0, z0, x1, y1, z1);
        return rtv;
    }

    @Override
    public IRayTraceVec getVector3D(IEntity<?> entity, IEntity<?> target) {
        if (entity == null || target == null) { return RayTraceVec.EMPTY; }
        return getVector3D(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
    }

    @Override
    public IRayTraceVec getVector3D(IEntity<?> entity, IPos pos) {
        if (entity == null || pos == null) { return RayTraceVec.EMPTY; }
        return getVector3D(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public IRayTraceResults rayTraceBlocksAndEntitys(Entity entity, double yaw, double pitch, double distance) {
        if (entity == null || distance <= 0.0d) { return RayTraceResults.EMPTY; }
        RayTraceResults rtrs = new RayTraceResults();

        Vec3 vecStart = entity.getEyePosition(1.0f);
        double rad = Math.PI / 180.0d;
        double f = Math.cos(-yaw * rad - Math.PI);
        double f1 = Math.sin(-yaw * rad - Math.PI);
        double f2 = -Math.cos(-pitch * rad);
        double f3 = Math.sin(-pitch * rad);
        Vec3 vecLook = new Vec3(f1 * f2, f3, f * f2);
        Vec3 vecEnd = vecStart.add(vecLook.x * distance, vecLook.y * distance, vecLook.z * distance);
        rtrs.add(entity, distance, vecStart, vecEnd);

        int x0 = (int) Math.floor(vecStart.x);
        int y0 = (int) Math.floor(vecStart.y);
        int z0 = (int) Math.floor(vecStart.z);
        int x1 = (int) Math.floor(vecEnd.x);
        int y1 = (int) Math.floor(vecEnd.y);
        int z1 = (int) Math.floor(vecEnd.z);

        BlockPos pos = new BlockPos(x0, y0, z0);
        BlockState state = entity.level().getBlockState(pos);
        rtrs.add(entity.level(), pos, state);

        int k1 = 200;
        while (k1-- >= 0) {
            if (x0 == x1 && y0 == y1 && z0 == z1) { return rtrs; }

            boolean butEqualX = true;
            boolean butEqualY = true;
            boolean butEqualZ = true;
            double d0 = 999.0D;
            double d1 = 999.0D;
            double d2 = 999.0D;

            if (x1 > x0) {
                d0 = (double) x0 + 1.0D;
            } else if (x1 < x0) {
                d0 = (double) x0 + 0.0D;
            } else {
                butEqualX = false;
            }

            if (y1 > y0) {
                d1 = (double) y0 + 1.0D;
            } else if (y1 < y0) {
                d1 = (double) y0 + 0.0D;
            } else {
                butEqualY = false;
            }

            if (z1 > z0) {
                d2 = (double) z0 + 1.0D;
            } else if (z1 < z0) {
                d2 = (double) z0 + 0.0D;
            } else {
                butEqualZ = false;
            }

            double d3 = 999.0D;
            double d4 = 999.0D;
            double d5 = 999.0D;
            double d6 = vecEnd.x - vecStart.x;
            double d7 = vecEnd.y - vecStart.y;
            double d8 = vecEnd.z - vecStart.z;

            if (butEqualX) {
                d3 = (d0 - vecStart.x) / d6;
            }
            if (butEqualY) {
                d4 = (d1 - vecStart.y) / d7;
            }
            if (butEqualZ) {
                d5 = (d2 - vecStart.z) / d8;
            }

            if (d3 == -0.0D) {
                d3 = -1.0E-4D;
            }
            if (d4 == -0.0D) {
                d4 = -1.0E-4D;
            }
            if (d5 == -0.0D) {
                d5 = -1.0E-4D;
            }

            Direction direction;
            if (d3 < d4 && d3 < d5) {
                direction = x1 > x0 ? Direction.WEST : Direction.EAST;
                vecStart = new Vec3(d0, vecStart.y + d7 * d3, vecStart.z + d8 * d3);
            } else if (d4 < d5) {
                direction = y1 > y0 ? Direction.DOWN : Direction.UP;
                vecStart = new Vec3(vecStart.x + d6 * d4, d1, vecStart.z + d8 * d4);
            } else {
                direction = z1 > z0 ? Direction.NORTH : Direction.SOUTH;
                vecStart = new Vec3(vecStart.x + d6 * d5, vecStart.y + d7 * d5, d2);
            }

            x0 = (int) Math.floor(vecStart.x) - (direction == Direction.EAST ? 1 : 0);
            y0 = (int) Math.floor(vecStart.y) - (direction == Direction.UP ? 1 : 0);
            z0 = (int) Math.floor(vecStart.z) - (direction == Direction.SOUTH ? 1 : 0);
            pos = new BlockPos(x0, y0, z0);
            state = entity.level().getBlockState(pos);
            rtrs.add(entity.level(), pos, state);
        }
        return rtrs;
    }

    @Override
    public IRayTraceResults rayTraceBlocksAndEntitys(IEntity<?> entity, double yaw, double pitch, double distance) {
        if (entity == null) { return RayTraceResults.EMPTY; }
        return rayTraceBlocksAndEntitys(entity.getMCEntity(), yaw, pitch, distance);
    }

    public InputStream getModInputStream(String fileName) {
        if (fileName == null || fileName.isEmpty() || fileName.lastIndexOf(".") == -1) { return null; }
        LogWriter.info("Getting a list of mod files by key \"" + fileName + "\"");
        InputStream inputStream = null;
        Optional<? extends ModContainer> modContainer = ModList.get().getModContainerById(CustomNpcs.MODID);
        if (modContainer.isPresent()) {
            File source = modContainer.get().getModInfo().getOwningFile().getFile().getFilePath().toFile();
            if (source.exists()) {
                if (!source.isDirectory() && (source.getName().toLowerCase().endsWith(".jar") || source.getName().toLowerCase().endsWith(".zip"))) {
                    try {
                        ZipFile zip = new ZipFile(source);
                        Enumeration<? extends ZipEntry> entries = zip.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry zipentry = entries.nextElement();
                            if (zipentry.isDirectory() || !zipentry.getName().endsWith(fileName)) {
                                continue;
                            }
                            inputStream = zip.getInputStream(zipentry);
                            break;
                        }
                        // java.util.zip.ZipFile.ZipFileInflaterInputStream -> java.io.ByteArrayInputStream
                        if (inputStream != null) {
                            InputStream copyStream = new ByteArrayInputStream(IOUtils.toByteArray(inputStream));
                            IOUtils.closeQuietly(inputStream);
                            inputStream = copyStream;
                        }
                        zip.close();
                    } catch (Exception e) { LogWriter.error("Error:", e); }
                }
                else if (source.isDirectory()) {
                    List<File> list = getFiles(source, fileName.substring(fileName.lastIndexOf(".")));
                    for (File file : list) {
                        if (!file.isFile() || !file.getName().equals(fileName)) { continue; }
                        try { inputStream = Files.newInputStream(file.toPath()); }
                        catch (Exception e) { LogWriter.error("Error:", e); }
                        break;
                    }
                }
            }
        }
        return inputStream;
    }

    public boolean equalsDeleteColor(String str0, String str1, boolean ignoreCase) {
        str0 = Util.instance.deleteColor(str0);
        str1 = Util.instance.deleteColor(str1);
        return ignoreCase ? str0.equalsIgnoreCase(str1) : str0.equals(str1);
    }

    public Entity getLookEntity(Entity entity, Double distance, boolean aliveOnly) {
        Entity target = null;
        if (distance == null) {
            distance = 32.0;
            if (entity instanceof Player) { distance = PlayerData.get((Player) entity).game.blockReachDistance; }
        }
        Vec3 vec3d = entity.getEyePosition(1.0F);
        Vec3 vec3d1 = entity.getViewVector(1.0F);
        Vec3 vec3d2 = vec3d.add(vec3d1.x * distance, vec3d1.y * distance, vec3d1.z * distance);
        BlockHitResult result = entity.level().clip(new ClipContext(vec3d, vec3d2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (result.getType() != HitResult.Type.MISS) { vec3d2 = result.getLocation(); }
        List<Entity> list = entity.level().getEntities(entity, entity.getBoundingBox().inflate(distance));
        List<Entity> results = new ArrayList<>();
        for (Entity e : list) {
            if (e != entity) {
                AABB axisAlignedBB = e.getBoundingBox().inflate(e.getPickRadius());
                Optional<Vec3> optional = axisAlignedBB.clip(vec3d, vec3d2);
                if (optional.isPresent() && (!aliveOnly || e.isAlive())) { results.add(e); }
            }
        }
        results.sort((o1, o2) -> {
            double d1 = entity.distanceToSqr(o1);
            double d2 = entity.distanceToSqr(o2);
            if (d1 == d2) { return 0; }
            else { return d1 > d2 ? 1 : -1; }
        });
        if (!results.isEmpty()) { target = results.toArray(new Entity[0])[0]; }
        return target;
    }

    public EntityNPCInterface copyToGUI(EntityNPCInterface npcParent, Level level, boolean copyRotation) {
        CompoundTag npcNbt = new CompoundTag();
        if (npcParent == null) { npcParent = CustomEntities.entityCustomNpc.create(level); }
        if (npcParent == null) { return null; }
        npcParent.saveAsPassenger(npcNbt);
        Optional<Entity> type = EntityType.create(npcNbt, level);
        EntityNPCInterface npc = null;
        if (type.isPresent()) { npc = (EntityNPCInterface) type.get(); }
        if (npc == null) { return npcParent; }
        MarkData.get(npc).marks.clear();
        npc.display.setShowName(1);
        npc.display.setVisible(0);
        npc.setHealth(npc.getMaxHealth());
        npc.deathTime = 0;
        npc.xRotO = 0;
        npc.yRotO = 0;
        npc.yBodyRot = 0;
        npc.yHeadRotO = 0;
        npc.ais.orientation = 0;
        if (copyRotation) {
            npc.xRotO = npcParent.xRotO;
            npc.yRotO = npcParent.yRotO;
            npc.yBodyRot = npcParent.yBodyRot;
            npc.yHeadRotO = npcParent.yHeadRotO;
            npc.ais.orientation = npcParent.ais.orientation;
            if (npcParent.ais.getStandingType() != 0 && npcParent.ais.getStandingType() != 2) {
                npc.xRotO = npcParent.ais.orientation;
                npc.yBodyRot = npcParent.ais.orientation;
            }
        }
        npc.ais.setStandingType(1);
        npc.tickCount = 100;
        if (npc instanceof EntityCustomNpc n0 && npcParent instanceof EntityCustomNpc n1) { n0.modelData.entity = n1.modelData.entity; }
        return npc;
    }

    public List<String> getStringData(String str) {
        int maxLength = 32767;
        List<String> list = new ArrayList<>();
        for (int i = 0; i < str.length(); i += maxLength) {
            int endIndex = Math.min(i + maxLength, str.length());
            String part = str.substring(i, endIndex);
            list.add(part);
        }
        return list;
    }

    public String getLastColor(String color, String str) {
        char c = (char) 167;
        if (str.lastIndexOf(c) != -1) {
            if (str.lastIndexOf(c) + 1 < str.length()) {
                int start = str.lastIndexOf(c);
                int end = start + 2;
                while (start - 2 >= 0 && str.charAt(start - 2) == c) {
                    start -= 2;
                }
                color = str.substring(start, end);
            } else {
                color = getLastColor(color, str.substring(0, str.length() - 1));
            }
        }
        return color;
    }

    public boolean canRemoveItems(NonNullList<ItemStack> inventory, ItemStack stack, boolean ignoreDamage, boolean ignoreNBT) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Map<ItemStack, Integer> items = new HashMap<>();
        items.put(stack, stack.getCount());
        return canRemoveItems(inventory, items, ignoreDamage, ignoreNBT);
    }

    public boolean canRemoveItems(NonNullList<ItemStack> inventory, Map<ItemStack, Integer> items, boolean ignoreDamage, boolean ignoreNBT) {
        if (inventory == null) { return false; }
        if (items == null || items.isEmpty()) { return true; }
        Map<ItemStack, Integer> inv = new HashMap<>();
        for (ItemStack stack : inventory) {
            if (NoppesUtilServer.isItemStackNull(stack) || stack.isEmpty()) { continue; }
            boolean found = false;
            for (ItemStack st : inv.keySet()) {
                if (NoppesUtilServer.isItemStackNull(st) || st.isEmpty()) { continue; }
                if (NoppesUtilPlayer.compareItems(stack, st, false, false)) {
                    inv.put(st, inv.get(st) + stack.getCount());
                    found = true;
                    break;
                }
            }
            if (!found) { inv.put(stack, stack.getCount()); }
        }
        return canRemoveItems(inv, items, ignoreDamage, ignoreNBT);
    }

    public boolean canRemoveItems(Map<ItemStack, Integer> inventory, Map<ItemStack, Integer> items, boolean ignoreDamage, boolean ignoreNBT) {
        if (inventory == null || items == null || items.isEmpty()) { return false; }
        for (ItemStack stack : items.keySet()) {
            int count = items.get(stack);
            if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
            for (ItemStack is : inventory.keySet()) {
                if (!NoppesUtilServer.isItemStackNull(is) && NoppesUtilPlayer.compareItems(stack, is, ignoreDamage, ignoreNBT)) {
                    count -= inventory.get(is);
                }
                if (count <= 0) { break; }
            }
            if (count > 0) { return false; }
        }
        return true;
    }

    public boolean removeItem(ServerPlayer player, ItemStack stack, boolean ignoreDamage, boolean ignoreNBT) {
        if (player == null || stack == null || stack.isEmpty()) { return false; }
        return removeItem(player, stack, stack.getCount(), ignoreDamage, ignoreNBT);
    }

    public boolean removeItem(ServerPlayer player, ItemStack stack, int count, boolean ignoreDamage, boolean ignoreNBT) {
        if (player == null || stack == null || stack.isEmpty()) { return false; }
        for (int i = 0; i < player.getInventory().items.size(); ++i) {
            ItemStack is = player.getInventory().items.get(i);
            if (NoppesUtilServer.isItemStackNull(is)) { continue; }
            if (NoppesUtilPlayer.compareItems(stack, is, ignoreDamage, ignoreNBT)) {
                if (count < is.getCount()) {
                    is.split(count);
                    updatePlayerInventory(player);
                    return true;
                }
                count -= is.getCount();
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        return count <= 0;
    }

    @SuppressWarnings("unused")
    public boolean copyDirectory(File sourceDir, File targetDir) {
        if (sourceDir == null || targetDir == null) return false;
        java.nio.file.Path sourcePath = sourceDir.toPath();
        java.nio.file.Path targetPath = targetDir.toPath();
        LogWriter.debug("Trying copy directory \"" + sourceDir + "\" to \"" + targetDir + "\"");
        try {
            Files.createDirectories(targetPath);
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs) throws IOException {
                    java.nio.file.Path relativePath = sourcePath.relativize(dir);
                    java.nio.file.Path destinationDir = targetPath.resolve(relativePath);
                    Files.createDirectories(destinationDir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) throws IOException {
                    java.nio.file.Path relativePath = sourcePath.relativize(file);
                    java.nio.file.Path destinationFile = targetPath.resolve(relativePath);
                    Files.copy(file, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (IOException e) {
            LogWriter.error("Failed to copy directory from " + sourceDir + " to " + targetDir, e);
            return false;
        }
    }

    public void updatePlayerInventory(ServerPlayer player) {
        PlayerQuestData playerdata = PlayerData.get(player).questData;
        for (QuestData data : playerdata.activeQuests.values()) {
            for (IQuestObjective obj : data.quest.getObjectives((IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player))) {
                if (obj.getType() != 0) { continue; }
                playerdata.checkQuestCompletion(player, data);
            }
        }
    }

    public int inventoryItemCount(Player player, ItemStack stack, Availability availability, boolean ignoreDamage, boolean ignoreNBT) {
        if (player == null || (availability != null && !availability.isAvailable(player)) || stack.isEmpty()) { return 0; }
        int count = 0;
        for (ItemStack is : player.getInventory().items) {
            if (!NoppesUtilServer.isItemStackNull(is) && NoppesUtilPlayer.compareItems(stack, is, ignoreDamage, ignoreNBT)) {
                count += is.getCount();
            }
        }
        return count;
    }

    public boolean canAddItemAfterRemoveItems(NonNullList<ItemStack> inventory, ItemStack addStack, Map<ItemStack, Integer> items, boolean ignoreDamage, boolean ignoreNBT) {
        if (inventory == null || addStack.isEmpty()) { return false; }
        NonNullList<ItemStack> inv = NonNullList.withSize(inventory.size(), ItemStack.EMPTY);
        for (int i = 0; i < inventory.size(); ++i) {
            if (NoppesUtilServer.isItemStackNull(inventory.get(i))) { continue; }
            inv.set(i, inventory.get(i).copy());
        }
        if (items != null && !items.isEmpty()) {
            for (ItemStack stack : items.keySet()) {
                if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
                int count = items.get(stack);
                for (int i = 0; i < inv.size(); ++i) {
                    ItemStack is = inv.get(i);
                    if (NoppesUtilServer.isItemStackNull(is)) { continue; }
                    if (NoppesUtilPlayer.compareItems(stack, is, ignoreDamage, ignoreNBT)) {
                        if (count < is.getCount()) {
                            is.split(count);
                            inv.set(i, is);
                            count = 0;
                        } else {
                            count -= is.getCount();
                            inv.set(i, ItemStack.EMPTY);
                        }
                        if (count <= 0) { break; }
                    }
                }
            }
        }
        for (ItemStack itemStack : inv) {
            if (itemStack.isEmpty() || NoppesUtilPlayer.compareItems(addStack, itemStack, ignoreDamage, ignoreNBT)) { return true; }
        }
        return false;
    }

    public String getOldFormattedText(Component component) {
        return parseJson(gson.fromJson(Component.Serializer.toJson(component), JsonElement.class).getAsJsonObject());
    }

    private String parseJson(JsonObject js) {
        StringBuilder temp = new StringBuilder();
        if (js.has("color")) {
            ChatFormatting c = ChatFormatting.getByName(js.get("color").getAsString());
            if (c != null) { temp.append(c); }
        }
        if (js.has("translate")) {
            Object[] with = new Object[0];
            if (js.has("with")) {
                with = new Object[js.getAsJsonArray("with").size()];
                for (int i = 0; i < with.length; i++) {
                    JsonElement element = js.getAsJsonArray("with").get(i);
                    if (element.isJsonObject()) { with[i] = parseJson((JsonObject) element); }
                    else { with[i] = element.getAsString(); }
                }
            }
            temp.append(Component.translatable(js.get("translate").getAsString(), with).getString());
        }
        else if (js.has("text")) { temp.append(js.get("text").getAsString()); }
        if (js.has("extra")) {
            for (JsonElement element : js.getAsJsonArray("extra").asList()) {
                if (element.isJsonObject()) { temp.append(parseJson((JsonObject) element)); }
                temp.append(ChatFormatting.RESET);
            }
        }
        return temp.toString();
    }

    @OnlyIn(Dist.CLIENT)
    public void putHovers(List<Component> hoverText, Object... components) {
        if (hoverText == null || components == null) { return; }
        for (Object component : components) {
            if (component == null) { continue; }
            if (component instanceof List<?> list) {
                putHovers(hoverText, list.toArray());
                continue;
            }
            String text = null;
            if (component instanceof String lines) {
                if (!lines.contains("%")) { text = getOldFormattedText(Component.translatable(lines)); }
                else { hoverText.add(Component.literal(lines)); }
            }
            else if (component instanceof Component c) { text = getOldFormattedText(c); }
            if (text != null) {
                if (text.contains("~~~")) { text = text.replaceAll("~~~", "%"); }
                while (text.contains("<br>")) {
                    hoverText.add(Component.literal(text.substring(0, text.indexOf("<br>"))));
                    text = text.substring(text.indexOf("<br>") + 4);
                }
                if (!text.isEmpty()) { hoverText.add(Component.literal(text)); }
            }
        }
    }

    public <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(Map<K, V> map) {
        if (map == null || map.isEmpty()) { return new LinkedHashMap<>(); }
        Comparator<Map.Entry<K, V>> comparator = null;
        V value = map.values().iterator().next();
        if (value instanceof String) { comparator = Map.Entry.comparingByValue(); }
        else if (value instanceof Integer) { comparator = Comparator.comparingInt(e -> (Integer) e.getValue()); }
        else if (value instanceof Long) { comparator = Comparator.comparingLong(e -> (Long) e.getValue()); }
        else if (value instanceof Double || value instanceof Float) { comparator = Comparator.comparingDouble(e -> ((Number) e.getValue()).doubleValue()); }
        if (comparator != null) {
            return map.entrySet()
                    .stream()
                    .sorted(comparator)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));
        }
        return new LinkedHashMap<>(map);
    }

    public Map<Component, Integer> convertStringMap(Map<String, Integer> parent) {
        Map<Component, Integer> map = new HashMap<>();
        for (Map.Entry<String, Integer> entry : parent.entrySet()) {
            map.put(Component.literal(entry.getKey()), entry.getValue());
        }
        return map;
    }

    public Entity teleportEntity(MinecraftServer server, Entity entity, String dimensionIn, double x, double y, double z) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionIn));
        return teleportEntity(server, entity, dimension, x, y, z);
    }

    public @Nonnull Entity teleportEntity(MinecraftServer server, @Nonnull Entity entity, ResourceKey<Level> dimension, double x, double y, double z) {
        Entity newEntity = entity;
        if (server != null) {
            y = ValueUtil.correctDouble(y, -4096, 4096);
            ServerLevel level = server.getLevel(dimension);
            if (level != null && !entity.level().dimension().location().equals(dimension.location())) {
                if (entity instanceof ServerPlayer player) {
                    SPacketDimensionTeleport.teleportPlayer(player, dimension, x, y, z, player.getYRot(), player.getXRot());
                    return player;
                }
                else { newEntity = entity.changeDimension(level); }
            }
            if (newEntity == null) { newEntity = entity; }
            newEntity.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
        }
        return newEntity;
    }


    public float getCurrentXZSpeed(LivingEntity entity) {
        AttributeInstance movementAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        float speed = 1.0f;
        if (movementAttribute != null) {
            if (movementAttribute.getBaseValue() != 0.0d) {
                speed = (float) (movementAttribute.getValue() / movementAttribute.getBaseValue());
            }
        }
        return ValueUtil.correctFloat(speed, 0.25f, 1.0f);
    }

    public boolean isMoving(LivingEntity entity) {
        if (entity instanceof Mob mob && !mob.getNavigation().isDone()) { return true; }
        AttributeInstance movementAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        double speed = 0.004d;
        if (movementAttribute != null) {
            speed = movementAttribute.getBaseValue() / 5.0d;
        }
        return Math.sqrt(Math.pow(entity.getDeltaMovement().x, 2.0d) + Math.pow(entity.getDeltaMovement().z, 2.0d)) > speed;
    }

    @SuppressWarnings({"JavaReflectionMemberAccess", "unchecked"})
    public @Nullable Entity getEntityByUUID(UUID uuid, Level startWorld, boolean onlyInLevel) {
        Entity entity = null;
        if (startWorld != null) {
            if (startWorld instanceof ServerLevel serverLevel) { entity = getEntityInWorld(uuid, serverLevel.getEntities()); }
            else {
                if (entityStorage == null) {
                    try {
                        Class<?> clientLevel = Class.forName("net.minecraft.client.multiplayer.ClientLevel");
                        try { entityStorage = clientLevel.getDeclaredField("f_171631_"); } catch (Exception ignored) { }
                        if (entityStorage == null) {
                            try { entityStorage = clientLevel.getDeclaredField("entityStorage"); } catch (Exception ignored) { }
                        }
                    }
                    catch (Exception ignored) {}
                }
                if (entityStorage != null) {
                    try {
                        entityStorage.trySetAccessible();
                        entity = getEntityInWorld(uuid,
                                ((TransientEntitySectionManager<Entity>) entityStorage.get(startWorld)).getEntityGetter());
                    }
                    catch (Exception ignored) {}
                }
            }
            if (entity == null && !onlyInLevel) {
                Player player = CustomNpcs.proxy.getPlayer();
                MinecraftServer server = CustomNpcs.Server != null ? CustomNpcs.Server
                        : startWorld.getServer() != null ? startWorld.getServer()
                        : player != null && player.level().getServer() != null ? player.level().getServer()
                        : null;
                if (server != null) {
                    for (ServerLevel level : server.getAllLevels()) {
                        if (!level.dimension().equals(startWorld.dimension())) {
                            entity = getEntityInWorld(uuid, level.getEntities());
                            if (entity != null) { break; }
                        }
                    }
                }
            }
        }
        return entity;
    }

    @SuppressWarnings("unchecked")
    public <T extends EntityAccess> Entity getEntityInWorld(UUID uuid, LevelEntityGetter<T> getter) {
        if (uuid != null && getter != null) {
            EntityAccess entityAccess = getter.get(uuid);
            if (entityAccess == null && getter instanceof LevelEntityGetterAdapter<T> getterAdapter) {
                EntitySectionStorage<T> ess = ((ILevelEntityGetterAdapterMixin<T>) getterAdapter).getSectionStorage();
                Long2ObjectMap<EntitySection<T>> sections = ((IEntitySectionStorageMixin<T>) ess).getSections();
                for (EntitySection<T> section : sections.values()) {
                    if (section == null || section.isEmpty()) { continue; }
                    T found = section.getEntities()
                            .filter(e -> e instanceof Entity && e.getUUID().equals(uuid))
                            .findFirst()
                            .orElse(null);
                    if (found != null) {
                        entityAccess = found;
                        break;
                    }
                }
            } // unloaded
            if (entityAccess instanceof Entity entity) { return entity; }
        }
        return null;
    }

    public String sanitizeFilename(String name) {
        String forbiddenChars = new String(SharedConstants.ILLEGAL_FILE_CHARACTERS);
        char[] chars = name.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (forbiddenChars.indexOf(chars[i]) >= 0) { chars[i] = '_'; }
        }
        String newName = new String(chars);
        while (newName.contains("__")) { newName = newName.replace("__", "_"); }
        return newName;
    }

    public Map<ItemStack, Boolean> getInventoryItemCount(Player player, Container inventory) {
        Map<ItemStack, Integer> counts = new HashMap<>();
        Map<ItemStack, ItemStack> base = new HashMap<>();
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
            boolean has = false;
            if (stack.getMaxStackSize() > 1) {
                for (ItemStack s : list) {
                    if (NoppesUtilPlayer.compareItems(stack, s, false, false)) {
                        if (s.getCount() != s.getMaxStackSize()) {
                            if (stack.getCount() + s.getCount() > stack.getMaxStackSize()) {
                                ItemStack c = stack.copy();
                                c.setCount((stack.getCount() + s.getCount()) % s.getMaxStackSize());
                                s.setCount(s.getMaxStackSize());
                                list.add(c);
                            } else {
                                s.setCount(stack.getCount() + s.getCount());
                            }
                            has = true;
                            break;
                        }
                    }
                }
            }
            if (!has) {
                list.add(stack.copy());
            }
        }
        list.sort((st_0, st_1) -> Integer.compare(st_1.getCount(), st_0.getCount()));
        for (ItemStack stack : list) {
            for (ItemStack s : counts.keySet()) {
                if (NoppesUtilPlayer.compareItems(stack, s, false, false)) {
                    counts.put(s, counts.get(s) + stack.getCount());
                    base.put(stack, s);
                    stack = s;
                    break;
                }
            }
            if (!counts.containsKey(stack)) {
                counts.put(stack, stack.getCount());
                base.put(stack, stack);
            }
        }
        Map<ItemStack, Boolean> map = new HashMap<>();
        for (ItemStack stack : counts.keySet()) {
            int count = 0;
            for (int i = 0; i < player.getInventory().items.size(); ++i) {
                ItemStack s = player.getInventory().items.get(i);
                if (NoppesUtilServer.isItemStackNull(s)) {
                    continue;
                }
                if (NoppesUtilPlayer.compareItems(stack, s, false, false)) {
                    count += s.getCount();
                }
            }
            boolean has = count >= counts.get(stack);
            for (ItemStack inInvStack : base.keySet()) {
                if (base.get(inInvStack) == stack) {
                    map.put(inInvStack, has);
                }
            }
        }
        Map<ItemStack, Boolean> total = new LinkedHashMap<>();
        for (ItemStack stack : list) {
            total.put(stack, map.get(stack));
        }
        return total;
    }

    @SuppressWarnings("unused")
    public void jumpTowards(IEntity<?> iEntity, IPos iPos) { jumpTowards(1.3f, iEntity.getMCEntity(), iPos.getMCVec3()); }

    public void jumpTowards(float speed, Entity entity, Vec3 vec) {
        double x = vec.x - entity.getX();
        double y = vec.y - entity.getBoundingBox().minY;
        double z = vec.z - entity.getZ();
        float varF = (float) Math.sqrt(x * x + z * z);
        float pitch = getPitch(speed, y, varF);
        float yaw = (float)(Math.atan2(x, z) * 180.0D / Math.PI);
        float f0 = (float) Math.PI;
        Vec3 motion = new Vec3(Mth.sin(yaw / 180.0F * f0) * Mth.cos(pitch / 180.0F * f0),
                Mth.sin((pitch + 1.0F) / 180.0F * f0),
                Mth.cos(yaw / 180.0F * f0) * Mth.cos(pitch / 180.0F * f0));
        motion.scale(speed);
        entity.setDeltaMovement(motion);
        entity.hurtMarked = true;
    }

    public float getPitch(float speed, double y, double horizontalDist) {
        float f0 = 0.2F;
        float f1 = speed * speed;
        double f2 = (double) f0 * horizontalDist;
        double f3 = (double) f0 * horizontalDist * horizontalDist + 2.0D * y * (double) f1;
        double f4 = (double) (f1 * f1) - (double) f0 * f3;
        if (f4 < 0.0D) { return 90.0F; }
        float f5 = f1 - (float) Math.sqrt(f4);
        return (float) (Math.atan2(f5, f2) * 180.0D / Math.PI);
    }

}
