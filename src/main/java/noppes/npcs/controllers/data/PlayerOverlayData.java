package noppes.npcs.controllers.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import noppes.npcs.api.handler.data.IPlayerData;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.api.wrapper.OverlayWrapper;
import noppes.npcs.api.wrapper.ScreenSize;
import noppes.npcs.client.controllers.OverlayController;
import noppes.npcs.client.overlay.Overlay;
import noppes.npcs.util.Util;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerOverlayData implements IPlayerData {

    protected static final String dataName = "OverlayData";
    protected static final ResourceLocation ALL_ELEMENTS = new ResourceLocation("all");

    protected final Map<ResourceLocation, Boolean> showElementTypes = new HashMap<>();
    protected final Set<Integer> overlays = new HashSet<>();
    public final ScreenSize screenSize = new ScreenSize(0, 0);
    public final Set<Integer> keyPress = new HashSet<>();
    public final Set<Integer> mousePress = new HashSet<>();

    public String currentGUI = "";
    public boolean isMoved;

    public boolean updateClient; // ServerTickHandler.onPlayerTick()

    public PlayerOverlayData() {
        showElementTypes.put(ALL_ELEMENTS, true);
        showElementTypes.put(new ResourceLocation("vignette"), true);
        showElementTypes.put(new ResourceLocation("spyglass"), true);
        showElementTypes.put(new ResourceLocation("helmet"), true);
        showElementTypes.put(new ResourceLocation("frostbite"), true);
        showElementTypes.put(new ResourceLocation("portal"), true);
        showElementTypes.put(new ResourceLocation("hotbar"), true);
        showElementTypes.put(new ResourceLocation("crosshair"), true);
        showElementTypes.put(new ResourceLocation("boss_event_progress"), true);
        showElementTypes.put(new ResourceLocation("player_health"), true);
        showElementTypes.put(new ResourceLocation("armor_level"), true);
        showElementTypes.put(new ResourceLocation("food_level"), true);
        showElementTypes.put(new ResourceLocation("air_level"), true);
        showElementTypes.put(new ResourceLocation("mount_health"), true);
        showElementTypes.put(new ResourceLocation("jump_bar"), true);
        showElementTypes.put(new ResourceLocation("experience_bar"), true);
        showElementTypes.put(new ResourceLocation("item_name"), true);
        showElementTypes.put(new ResourceLocation("sleep_fade"), true);
        showElementTypes.put(new ResourceLocation("potion_icons"), true);
        showElementTypes.put(new ResourceLocation("debug_text"), true);
        showElementTypes.put(new ResourceLocation("fps_graph"), true);
        showElementTypes.put(new ResourceLocation("record_overlay"), true);
        showElementTypes.put(new ResourceLocation("title_text"), true);
        showElementTypes.put(new ResourceLocation("subtitles"), true);
        showElementTypes.put(new ResourceLocation("scoreboard"), true);
        showElementTypes.put(new ResourceLocation("chat_panel"), true);
        showElementTypes.put(new ResourceLocation("player_list"), true);
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        CompoundTag overlayNBT = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, Boolean> entry : showElementTypes.entrySet()) {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("K", entry.getKey().toString());
            nbt.putBoolean("V", entry.getValue());
            list.add(nbt);
        }
        overlayNBT.put("ShowElementTypes", list);
        int[] iK = overlayNBT.getIntArray("KeyPress");
        int[] iM = overlayNBT.getIntArray("MousePress");
        keyPress.clear();
        mousePress.clear();
        for (int key : iK) { keyPress.add(key); }
        for (int key : iM) { mousePress.add(key); }
        overlayNBT.putDouble("ScreenWidth", screenSize.getWidth());
        overlayNBT.putDouble("ScreenHeight", screenSize.getHeight());
        compound.put(dataName, overlayNBT);

        if (Util.instance.getSide() == Dist.DEDICATED_SERVER) {
            OverlayController qData = OverlayController.getInstance();
            List<Integer> lIDs = new ArrayList<>();
            list = new ListTag();
            for (int id : overlays) {
                Overlay overlay = qData.get(id);
                if (overlay != null) {
                    lIDs.add(id);
                    list.add(overlay.getNBT());
                }
            }
            overlayNBT.putIntArray("OverlayIDs", lIDs);
            overlayNBT.put("Overlays", list);
        }
        else { overlayNBT.putIntArray("OverlayIDs", overlays.stream().mapToInt(Integer::intValue).toArray()); }
        return compound;
    }

    @Override
    public void load(CompoundTag compound) {
        if (compound == null || !compound.contains(dataName, 10) || !compound.contains("HUDData", 10)) { return; }
        CompoundTag overlayNBT = compound.contains(dataName, 10) ? compound.getCompound(dataName) : compound.getCompound("HUDData");
        int[] iK = overlayNBT.getIntArray("KeyPress");
        int[] iM = overlayNBT.getIntArray("MousePress");
        keyPress.clear();
        mousePress.clear();
        for (int key : iK) { keyPress.add(key); }
        for (int key : iM) { mousePress.add(key); }
        showElementTypes.clear();
        ListTag list = compound.getList("ShowElementTypes", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag nbt = list.getCompound(i);
            String location = nbt.getString("K");
            if (nbt.contains("K", 3)) { // 1.12.2
                location = switch (nbt.getInt("K")) {
                    case 0 -> ALL_ELEMENTS.toString();
                    case 1 -> "helmet";
                    case 2 -> "portal";
                    case 3 -> "crosshair";
                    case 4, 5 -> "boss_event_progress";
                    case 6 -> "armor_level";
                    case 7 -> "player_health";
                    case 8 -> "food_level";
                    case 9 -> "air_level";
                    case 10 -> "hotbar";
                    case 11 -> "experience_bar";
                    case 12 -> "title_text";
                    case 13 -> "mount_health";
                    case 14 -> "jump_bar";
                    case 15 -> "chat_panel";
                    case 16 -> "player_list";
                    case 17 -> "debug_text";
                    case 18 -> "potion_icons";
                    case 19 -> "subtitles";
                    case 20 -> "fps_graph";
                    case 21 -> "vignette";
                    default -> "scoreboard";
                };
            }
            showElementTypes.put(new ResourceLocation(location), nbt.getBoolean("V"));
        }

        overlays.clear();
        overlays.addAll(Arrays.stream(overlayNBT.getIntArray("OverlayIDs")).boxed().collect(Collectors.toSet()));
        if (Util.instance.getSide() == Dist.CLIENT && overlayNBT.contains("Overlays", 9)) {
            OverlayController qData = OverlayController.getInstance();
            list = overlayNBT.getList("Overlays", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag nbt = list.getCompound(i);
                if (overlays.contains(nbt.getInt("id"))) {
                    OverlayWrapper wrapper = new OverlayWrapper(0);
                    wrapper.load(new NBTWrapper(compound));
                    qData.addOverlay(wrapper);
                }
            }
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
            if (k == key) { return true; }
        }
        return mousePress.contains(key);
    }

    public boolean hasOrKeysPressed(int... keys) {
        for (int key : keys) {
            for (int k : keyPress) {
                if (k == key) { return true; }
            }
        }
        return false;
    }

    public boolean isPressedCtrl() { return hasOrKeysPressed(341, 345); }

    public boolean isPressedShift() { return hasOrKeysPressed(340, 344); }

    public ScreenSize getWindowSize() { return screenSize; }

    public boolean isShowElementType(ResourceLocation id) {
        if (showElementTypes.containsKey(ALL_ELEMENTS) &&
                !showElementTypes.get(ALL_ELEMENTS)) { return false; }
        Boolean value = showElementTypes.get(id);
        if (value == null) { value = showElementTypes.computeIfAbsent(id, k -> Boolean.TRUE); }
        return value;
    }

    public boolean isMoved() { return isMoved; }

    public void add(int overlayId) { overlays.add(overlayId); updateClient = true; }

    public void remove(int overlayId) { overlays.remove(overlayId); updateClient = true; }

    public void clearOverlays() { overlays.clear(); updateClient = true; }
}
