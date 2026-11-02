package noppes.npcs.controllers.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.IPos;
import noppes.npcs.api.gui.ICompassData;
import noppes.npcs.api.handler.data.IPlayerData;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.util.ValueUtil;

public class PlayerCompassData implements ICompassData, IPlayerData {

    protected static final String dataName = "CompassData";

    public String npc = "";
    public String name = "";
    public String title = "";
    public BlockPos pos = BlockPos.ZERO;
    public boolean showQuestName = true;
    public boolean showTaskProgress = true;
    public boolean showOfPlayer = true;
    public boolean showDial = true;
    public boolean isCustomPoint = false;
    public boolean questLogIsFast = false;
    public boolean isFlat = false;
    public ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Level.OVERWORLD.location());
    public int questID;
    public int color = (int) (Math.random() * 16777215.0);
    private int range = 5;
    private int taskType = 0;
    public final float[] screenPos = new float[] { 0.145f, 0.765f };
    public float scale = 1.0f;
    public float incline = 0.0f;
    public float rot = 0.0f;

    @Override
    public CompoundTag save(CompoundTag compound) {
        CompoundTag compassNbt = new CompoundTag();
        compassNbt.putString("Name", name);
        compassNbt.putString("Title", title);
        compassNbt.putString("NPCName", npc);
        compassNbt.putString("DimensionID", dimension.location().toString());
        compassNbt.putIntArray("BlockPos", new int[] { pos.getX(), pos.getY(), pos.getZ() });
        compassNbt.putInt("Range", range);
        compassNbt.putInt("Type", taskType);
        compassNbt.putBoolean("ShowOfPlayer", showOfPlayer);
        compassNbt.putBoolean("IsCustomPoint", isCustomPoint);
        compassNbt.putBoolean("QuestLogIsFast", questLogIsFast);
        compassNbt.putBoolean("IsShowDial", showDial);
        compassNbt.putBoolean("IsFlat", isFlat);
        compassNbt.putFloat("Scale", scale);
        compassNbt.putFloat("Rotation", rot);
        compassNbt.putFloat("Incline", incline);
        compassNbt.putByteArray("Showed", new byte[] { (byte) (showQuestName ? 1 : 0), (byte) (showTaskProgress ? 1 : 0) });
        ListTag scP = new ListTag();
        scP.add(FloatTag.valueOf(screenPos[0]));
        scP.add(FloatTag.valueOf(screenPos[1]));
        compassNbt.put("ScreenPos", scP);
        compound.put(dataName, compassNbt);
        return compound;
    }

    @Override
    public void load(CompoundTag compound) {
        if (compound == null || !compound.contains(dataName, 10)) { return; }
        CompoundTag compassNbt = compound.getCompound(dataName);
        name = compassNbt.getString("Name");
        title = compassNbt.getString("Title");
        npc = compassNbt.getString("NPCName");
        int[] p = compassNbt.getIntArray("BlockPos");
        if (p.length >= 3) { pos = new BlockPos(p[0], p[1], p[2]); }
        if (compassNbt.contains("DimensionID", 3)) { // in 1.12.2
            switch (compassNbt.getInt("DimensionID")) {
                case -1: setDimensionID(Level.NETHER.location().toString()); break;
                case 1: setDimensionID(Level.END.location().toString()); break;
                default: setDimensionID(Level.OVERWORLD.location().toString()); break;
            }
        }
        else { setDimensionID(compassNbt.getString("DimensionID")); }
        setRange(compassNbt.getInt("Range"));
        setTaskType(compassNbt.getInt("Type"));
        isCustomPoint = compassNbt.getBoolean("IsCustomPoint");
        questLogIsFast = compassNbt.getBoolean("QuestLogIsFast");
        isFlat = compassNbt.getBoolean("IsFlat");
        if (compassNbt.contains("IsShowDial", 1)) { showDial = compassNbt.getBoolean("IsShowDial"); }
        if (compassNbt.contains("ShowOfPlayer", 1)) { showOfPlayer = compassNbt.getBoolean("ShowOfPlayer"); }
        scale = compassNbt.getFloat("Scale");
        rot = compassNbt.getFloat("Rotation");
        incline = compassNbt.getFloat("Incline");
        ListTag list = compassNbt.getList("ScreenPos", 5);
        if (list.size() >= 2) {
            screenPos[0] = list.getFloat(0);
            screenPos[1] = list.getFloat(1);
        }
        byte[] s = compassNbt.getByteArray("Showed");
        if (s.length >= 2) {
            showQuestName = s[0] == (byte) 1;
            showTaskProgress = s[1] == (byte) 1;
        }
    }

    @Override
    public String getDimensionID() { return dimension.location().toString(); }

    @Override
    public String getName() { return name; }

    @Override
    public String getNPCName() { return npc; }

    @Override
    public IPos getPos() {
        Level level = null;
        if (CustomNpcs.Server != null) { level = CustomNpcs.Server.getLevel(dimension); }
        return new BlockPosWrapper(level, pos);
    }

    @Override
    public int getRange() { return range; }

    @Override
    public String getTitle() { return title; }

    @Override
    public int getTaskType() { return taskType; }

    @Override
    public boolean isCustomPoint() { return isCustomPoint; }

    @Override
    public boolean getShowOfPlayer() { return showOfPlayer; }

    @Override
    public boolean isShowDial() { return showDial; }

    @Override
    public boolean isFlat() { return isFlat; }

    @Override
    public void setDimensionID(String dimensionId) {
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionId));
        if (CustomNpcs.Server == null || CustomNpcs.Server.getLevel(dim) != null) { dimension = dim; }
    }

    @Override
    public void setName(String nameIn) {
        if (nameIn != null) { name = nameIn; }
    }

    @Override
    public void setNPCName(String npcName) {
        if (npcName != null) { npc = npcName; }
    }

    @Override
    public void setPos(int x, int y, int z) { pos = new BlockPos(x, y, z); }

    @Override
    public void setPos(IPos posIn) { pos = posIn.getMCBlockPos(); }

    @Override
    public void setRange(int rangeIn) {
        if (rangeIn < 0) { rangeIn *= -1; }
        range = ValueUtil.correctInt(rangeIn, 3, 64);
    }

    @Override
    public void setIsCustomPoint(boolean showIn) { isCustomPoint = showIn; }

    @Override
    public void setShowOfPlayer(boolean show) { showOfPlayer = show; }

    @Override
    public void setShowDial(boolean show) { showDial = show; }

    @Override
    public void setIsFlat(boolean flat) { isFlat = flat; }

    @Override
    public void setTitle(String titleIn) {
        if (titleIn != null) { title = titleIn; }
    }

    @Override
    public void setTaskType(int typeIn) {
        if (typeIn < 0) { typeIn *= -1; }
        taskType = typeIn % EnumQuestTask.values().length;
    }

}
