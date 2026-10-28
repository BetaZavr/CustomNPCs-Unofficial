package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import noppes.npcs.api.entity.data.IDropNbtSet;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.ValueUtil;

public class DropNbtSet implements IDropNbtSet {

    protected final DropSet parent;
    protected double chance = 100.0d;
    protected String path = "";
    protected int type = 0;
    protected int typeList = 0;
    protected String[] values = new String[0];
    protected Random rnd = new Random();

    public DropNbtSet(DropSet ds) { parent = ds; }

    public String checkValue(String value, int typeIn) {
        switch (typeIn) {
            case 0: {
                if (value.equalsIgnoreCase("true")) { return "true"; }
                else if (value.equalsIgnoreCase("false")) { return "false"; }
                break;
            } // boolean
            case 1: {
                try {
                    byte b = Byte.parseByte(value);
                    return String.valueOf(b);
                } catch (Exception e) { LogWriter.error(e); }
                break;
            } // byte
            case 2: {
                try {
                    short s = Short.parseShort(value);
                    return String.valueOf(s);
                } catch (Exception e) { LogWriter.error(e); }
                break;
            } // short
            case 3: {
                try {
                    int b = Integer.parseInt(value);
                    return String.valueOf(b);
                } catch (Exception e) { LogWriter.error(e); }
                break;
            } // integer
            case 4: {
                try {
                    long l = Long.parseLong(value);
                    return String.valueOf(l);
                } catch (Exception e) { LogWriter.error(e); }
                break;
            } // long
            case 5: {
                try {
                    float f = Float.parseFloat(value);
                    return String.valueOf(f);
                } catch (Exception e) { LogWriter.error(e); }
                break;
            } // float
            case 6: {
                try {
                    double d = Double.parseDouble(value);
                    return String.valueOf(d);
                } catch (Exception e) { LogWriter.error(e); }
                break;
            } // double
            case 8: { return value == null ? "" : value; } // string
            case 7: // byte array
            case 9: // list
            case 11: // integer array
            case 12: {
                if (value.startsWith("[") && value.endsWith("]")) {
                    StringBuilder text = new StringBuilder();
                    for (String str : value.substring(1, value.length() - 1).split(",")) {
                        String sc = checkValue(str, typeIn == 9 ? typeList : typeIn);
                        if (sc != null) {
                            if (!text.isEmpty()) { text.append(","); }
                            text.append(sc);
                        }
                    }
                    if (!text.isEmpty()) { return text.toString(); }
                }
                break;
            } // or long array
        }
        return null;
    }

    @Override
    public double getChance() {
        return Math.round(chance * 10000.0d) / 10000.0d;
    }

    @Override
    public void createAndPut(IItemStack item) {
        CompoundTag compound = item.getNbt().getMCNBT();
        String key = path;
        // create path:
        if (key.contains(".")) {
            String keyName;
            while (key.contains(".")) {
                keyName = key.substring(0, key.indexOf("."));
                key = key.substring(key.indexOf(".") + 1);
                if (compound instanceof CompoundTag) {
                    if (keyName.contains("[") || keyName.endsWith("]")) {
                        try {
                            int pos = Integer.parseInt(keyName.substring(keyName.indexOf("[") + 1, keyName.length() - 1));
                            if (pos < 0) { pos = 0;}
                            keyName = keyName.substring(0, keyName.indexOf("["));
                            if (!compound.contains(keyName, 9)) { compound.put(keyName, new ListTag()); }
                            ListTag list = compound.getList(keyName, 9);
                            if (pos >= list.size()) {
                                while (pos >= list.size()) {
                                    Tag tag = switch (list.getId() == 0 ? key.contains(".") ? 10 : typeList : list.getId()) {
                                        case 1 -> ByteTag.valueOf((byte) 0);
                                        case 2 -> ShortTag.valueOf((short) 0);
                                        case 3 -> IntTag.valueOf(0);
                                        case 4 -> LongTag.valueOf(0);
                                        case 5 -> FloatTag.valueOf(0.0f);
                                        case 6 -> DoubleTag.valueOf(0.0d);
                                        case 8 -> StringTag.valueOf("");
                                        case 9 -> new ListTag();
                                        case 11 -> new IntArrayTag(new int[0]);
                                        case 12 -> new LongArrayTag(new long[0]);
                                        default -> new CompoundTag();
                                    };
                                    list.add(tag);
                                }
                            }
                            if (list.get(pos) instanceof CompoundTag) { compound = (CompoundTag) list.get(pos); }
                        }
                        catch (Exception e) {
                            LogWriter.error("Failed create list tag: " + keyName + " from path: " + path);
                            return;
                        }
                    }
                    else {
                        if (!compound.contains(keyName, 10)) { compound.put(keyName, new CompoundTag()); }
                        compound = compound.getCompound(keyName);
                    }
                }
                else {
                    LogWriter.error("Failed create compound tag: " + keyName + " from path: " + path);
                    return;
                } // error create path
            }
        }
        // set value:
        String rndValue = values[rnd.nextInt(values.length)];
        try {
            switch (type) {
                case 0: compound.putBoolean(key, Boolean.parseBoolean(rndValue)); break;
                case 1: compound.putByte(key, Byte.parseByte(rndValue)); break;
                case 2: compound.putShort(key, Short.parseShort(rndValue)); break;
                case 3: compound.putInt(key, Integer.parseInt(rndValue)); break;
                case 4: compound.putLong(key, Long.parseLong(rndValue)); break;
                case 5: compound.putFloat(key, Float.parseFloat(rndValue)); break;
                case 6: compound.putDouble(key, Double.parseDouble(rndValue)); break;
                case 7: {
                    String[] variants = rndValue.split(",");
                    byte[] array = new byte[variants.length];
                    for (int i = 0; i < variants.length; i++) { array[i] = Byte.parseByte(variants[i]); }
                    compound.putByteArray(key, array);
                    break;
                } // byte array
                case 8: compound.putString(key, rndValue); break;
                case 9: { // list
                    String[] variants = rndValue.split(",");
                    ListTag list = new ListTag();
                    for (String value : variants) {
                        switch (typeList) {
                            case 1: {
                                if (value.equalsIgnoreCase("false")) { list.add(ByteTag.valueOf((byte) 0)); }
                                else if (value.equalsIgnoreCase("true")) { list.add(ByteTag.valueOf((byte) 1)); }
                                else { list.add(ByteTag.valueOf(Byte.parseByte(value))); }
                                break;
                            }
                            case 2: list.add(ShortTag.valueOf(Short.parseShort(value))); break;
                            case 3: list.add(IntTag.valueOf(Integer.parseInt(value))); break;
                            case 4: list.add(LongTag.valueOf(Long.parseLong(value))); break;
                            case 5: list.add(FloatTag.valueOf(Float.parseFloat(value))); break;
                            case 6: list.add(DoubleTag.valueOf(Double.parseDouble(value))); break;
                            case 7: {
                                String[] ints = value.split(";");
                                byte[] array = new byte[ints.length];
                                for (int i = 0; i < ints.length; i++) { array[i] = Byte.parseByte(variants[i]); }
                                list.add(new ByteArrayTag(array));
                                break;
                            }
                            case 8: list.add(StringTag.valueOf(value)); break;
                            case 11: {
                                String[] ints = value.split(";");
                                int[] array = new int[ints.length];
                                for (int i = 0; i < ints.length; i++) { array[i] = Integer.parseInt(variants[i]); }
                                list.add(new IntArrayTag(array));
                                break;
                            }
                            case 12: {
                                String[] ints = value.split(";");
                                long[] array = new long[ints.length];
                                for (int i = 0; i < ints.length; i++) { array[i] = Long.parseLong(ints[i]); }
                                list.add(new LongArrayTag(array));
                                break;
                            }
                        }
                    }
                    compound.put(key, list);
                    break;
                }
                case 11: {
                    String[] variants = rndValue.split(",");
                    int[] array = new int[variants.length];
                    for (int i = 0; i < variants.length; i++) { array[i] = Integer.parseInt(variants[i]); }
                    compound.putIntArray(key, array);
                    break;
                } // integer array
                case 12: {
                    String[] variants = rndValue.split(",");
                    long[] array = new long[variants.length];
                    for (int i = 0; i < variants.length; i++) { array[i] = Long.parseLong(variants[i]); }
                    compound.put(key, new LongArrayTag(array));
                    break;
                } // long array
            }
        }
        catch (Exception e) { LogWriter.error("Failed set key: " + key + " with value: " + rndValue + " to path: " + path); }
    }

    public Component getKey() {
        MutableComponent keyName = Component.empty();
        double ch = Math.round(chance * 10.0d) / 10.d;
        String chance = String.valueOf(ch).replace(".", ",");
        if (ch == (int) ch) { chance = String.valueOf((int) ch); }
        chance += "%";
        keyName.append(Component.literal(chance).withStyle(ChatFormatting.YELLOW));
        keyName.append(getPathToKey().withStyle(ChatFormatting.RESET));
        if (values.length == 0) {
            keyName.append(Component.literal("=").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("|").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("NULL").withStyle(ChatFormatting.RED))
                    .append(Component.literal("|").withStyle(ChatFormatting.GRAY));
        } else if (values.length == 1) {
            keyName.append(Component.literal("=").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("|").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(values[0]).withStyle(ChatFormatting.RED))
                    .append(Component.literal("|").withStyle(ChatFormatting.GRAY));
        } else {
            keyName.append(Component.literal("=").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("|").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("" + values.length).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("|").withStyle(ChatFormatting.GRAY));
        }
        return keyName;
        //return keyName.append(Component.literal(" #" + toString().substring(toString().indexOf("@") + 1)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private MutableComponent getPathToKey() {
        String key = path;
        if (path.contains(".")) {
            List<String> keys = new ArrayList<>();
            String preKey = "";
            while (key.contains(".")) {
                preKey = key.substring(0, key.indexOf("."));
                keys.add(preKey);
                key = key.substring(key.indexOf(".") + 1);
            }
            String lastKey = key;
            keys.add(key);
            key = preKey + "." + lastKey;
            if (keys.size() > 2) {
                key = "..." + key;
            }
        }
        return Component.literal(key);
    }

    public CompoundTag getNBT() {
        CompoundTag nbtDS = new CompoundTag();
        nbtDS.putInt("Type", type);
        nbtDS.putInt("TypeList", typeList);
        nbtDS.putString("Path", path);
        nbtDS.putDouble("Chance", chance);
        ListTag vs = new ListTag();
        for (String s : values) {
            if (s != null) { vs.add(StringTag.valueOf(s)); }
        }
        nbtDS.put("Values", vs);
        return nbtDS;
    }

    @Override
    public String getPath() { return path; }

    @Override
    public int getType() { return type; }

    @Override
    public int getTypeList() { return typeList; }

    @Override
    public String[] getValues() { return values; }

    public void load(CompoundTag nbtDS) {
        type = nbtDS.getInt("Type");
        typeList = nbtDS.getInt("TypeList");
        path = nbtDS.getString("Path");
        chance = nbtDS.getDouble("Chance");
        String[] vs = new String[nbtDS.getList("Values", 8).size()];
        for (int i = 0; i < nbtDS.getList("Values", 8).size(); i++) {
            String ch = checkValue(nbtDS.getList("Values", 8).getString(i), type);
            if (ch != null) { vs[i] = ch; }
        }
        values = vs;
    }

    @Override
    public void remove() { parent.removeDropNbt(this); }

    @Override
    public void setChance(double chanceIn) { chance = Math.round(ValueUtil.correctDouble(chanceIn, 0.0001d, 100.0d) * 10000.0d) / 10000.0d; }

    @Override
    public void setPath(String pathIn) {
        if (pathIn == null) { pathIn = ""; }
        path = pathIn;
    }

    @Override
    public void setType(int typeIn) {
        if ((typeIn >= 0 && typeIn <= 9) || typeIn == 11 || typeIn == 12) { type = typeIn; }
    }

    @Override
    public void setTypeList(int typeIn) {
        if ((typeIn >= 0 && typeIn <= 8) || typeIn == 11 || typeIn == 12) { typeList = typeIn; }
    }

    @Override
    public void setValues(String valuesIn) {
        if (valuesIn.contains("|")) {
            List<String> variants = new ArrayList<>();
            String variant;
            while (valuesIn.contains("|")) {
                variant = checkValue(valuesIn.substring(0, valuesIn.indexOf("|")), type); // check part
                if (variant != null) { variants.add(variant); }
                valuesIn = valuesIn.substring(valuesIn.indexOf("|") + 1);
            }
            variant = checkValue(valuesIn, type); // check value or end part
            if (variant != null) { variants.add(variant); }
            values = variants.toArray(new String[0]);
        }
        else {
            String ch = checkValue(valuesIn, type);
            if (ch != null) { values = new String[] { ch }; }
        }
    }

    @Override
    public void setValues(String[] valuesIn) {
        List<String> nal = new ArrayList<>();
        for (String str : valuesIn) {
            String key = checkValue(str, type);
            if (key != null) { nal.add(key); }
        }
        String[] svs = new String[nal.size()];
        for (int i = 0; i < nal.size(); i++) { svs[i] = nal.get(i); }
        values = svs;
    }

}
