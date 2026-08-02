package noppes.npcs.controllers.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import noppes.npcs.api.handler.data.IPlayerData;
import noppes.npcs.api.wrapper.ScreenSize;
import noppes.npcs.client.controllers.OverlayController;
import noppes.npcs.client.overlay.Overlay;
import noppes.npcs.util.Util;

import java.util.*;

public class PlayerOverlayData implements IPlayerData {

    protected static final String dataName = "OverlayData";

    protected final Map<RenderGameOverlayEvent.ElementType, Boolean> showElementTypes = new HashMap<>();
    protected final Set<Integer> overlays = new HashSet<>();
    public final ScreenSize screenSize = new ScreenSize(0, 0);
    public final Set<Integer> keyPress = new HashSet<>();
    public final Set<Integer> mousePress = new HashSet<>();

    public String currentGUI = "";
    public boolean isMoved;

    public boolean updateClient; // ServerTickHandler.cnpcPlayerTick()

    public PlayerOverlayData() {
        for (RenderGameOverlayEvent.ElementType et : RenderGameOverlayEvent.ElementType.values()) { showElementTypes.put(et, true); }
    }

    @Override
    public NBTTagCompound save(NBTTagCompound compound) {
        NBTTagCompound overlayNBT = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (Map.Entry<RenderGameOverlayEvent.ElementType, Boolean> entry : showElementTypes.entrySet()) {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setInteger("K", entry.getKey().ordinal());
            nbt.setBoolean("V", entry.getValue());
            list.appendTag(nbt);
        }
        overlayNBT.setTag("ShowElementTypes", list);
        overlayNBT.setIntArray("KeyPress", getKeyPressed());
        overlayNBT.setIntArray("MousePress", getMousePressed());
        overlayNBT.setDouble("ScreenWidth", screenSize.getWidth());
        overlayNBT.setDouble("ScreenHeight", screenSize.getHeight());
        compound.setTag(dataName, overlayNBT);

        if (Util.instance.getSide() == Side.SERVER) {
            OverlayController qData = OverlayController.getInstance();
            List<Integer> lIDs = new ArrayList<>();
            list = new NBTTagList();
            for (int id : overlays) {
                Overlay overlay = qData.get(id);
                if (overlay != null) {
                    lIDs.add(id);
                    list.appendTag(overlay.getNBT());
                }
            }
            int[] data = new int[lIDs.size()];
            int i = 0;
            for (int id : lIDs) { data[i++] = id; }
            overlayNBT.setIntArray("OverlayIDs", data);
            overlayNBT.setTag("Overlays", list);
        }
        else { overlayNBT.setIntArray("OverlayIDs", overlays.stream().mapToInt(Integer::intValue).toArray()); }
        return compound;
    }

    @Override
    public void load(NBTTagCompound compound) {
        if (compound == null || (!compound.hasKey(dataName, 10) && !compound.hasKey("HUDData", 10))) { return; }
        NBTTagCompound overlayNBT = compound.hasKey(dataName, 10) ? compound.getCompoundTag(dataName) : compound.getCompoundTag("HUDData");
        int[] iK = overlayNBT.getIntArray("KeyPress");
        int[] iM = overlayNBT.getIntArray("MousePress");
        keyPress.clear();
        mousePress.clear();
        for (int key : iK) { keyPress.add(key); }
        for (int key : iM) { mousePress.add(key); }
        showElementTypes.clear();
        NBTTagList list = compound.getTagList("ShowElementTypes", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound nbt = list.getCompoundTagAt(i);
            int id = nbt.getInteger("K");
            if (id < 0) { id *= -1; }
            showElementTypes.put(RenderGameOverlayEvent.ElementType.values()[id % RenderGameOverlayEvent.ElementType.values().length], nbt.getBoolean("V"));
        }
        updateClient = false;
    }

    public int[] getKeyPressed() {
        int[] ids = new int[keyPress.size()];
        int i = 0;
        for (int key : keyPress) {
            ids[i] = key;
            i++;
        }
        return ids;
    }

    public int[] getMousePressed() {
        int[] ids = new int[mousePress.size()];
        int i = 0;
        for (int key : mousePress) {
            ids[i] = key;
            i++;
        }
        return ids;
    }

    public boolean hasMousePress(int key) {
        for (int k : mousePress) {
            if (k == key) {
                return true;
            }
        }
        return mousePress.contains(key);
    }

    public boolean hasOrKeysPressed(int... keys) {
        for (int key : keys) {
            for (int k : keyPress) {
                if (k == key) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isPressedCtrl() { return hasOrKeysPressed(29, 157); }

    public boolean isPressedShift() { return hasOrKeysPressed(42, 54); }

    public ScreenSize getWindowSize() { return screenSize; }

    public boolean isShowElementType(RenderGameOverlayEvent.ElementType type) {
        Boolean value = showElementTypes.get(RenderGameOverlayEvent.ElementType.ALL);
        if (value == null || !value) { return false; }
        value = showElementTypes.get(type);
        if (value == null) { value = showElementTypes.computeIfAbsent(type, k -> Boolean.TRUE); }
        return value;
    }

    public void add(int overlayId) { overlays.add(overlayId); updateClient = true; }

    public void remove(int overlayId) { overlays.remove(overlayId); updateClient = true; }

    public void clearOverlays() { overlays.clear(); updateClient = true; }

}
