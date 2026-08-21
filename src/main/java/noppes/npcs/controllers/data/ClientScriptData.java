package noppes.npcs.controllers.data;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.wrapper.data.Data;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.NBTJsonUtil;
import noppes.npcs.util.Util;

import java.io.File;
import java.util.Objects;

public class ClientScriptData
        extends BaseScriptData {

    public boolean loadDefault = false;
    public final Data storedData = new Data();

    @Override
    public boolean isClient() { return true; }

    @Override
    public MutableComponent noticeString(String type, Object event) {
        return Component.literal("Client Scripts ").withStyle(ChatFormatting.DARK_GRAY)
                .append(super.noticeString(type, event));
    }

    @Override
    public void runScript(String type, Event event) {
        if (!isEnabled()) { return; }
        try {
            if (ScriptController.Instance.lastLoaded > lastInited) {
                lastInited = ScriptController.Instance.lastLoaded;
                if (!type.equalsIgnoreCase(EnumScriptType.INIT.function)) {
                    IPlayer<?> iPlayer = null;
                    if (CustomNpcs.proxy.getPlayer() != null) { iPlayer =  (IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(CustomNpcs.proxy.getPlayer()); }
                    runScript(EnumScriptType.INIT.function, new PlayerEvent.InitEvent(iPlayer));
                }
            }
            for (ScriptContainer script : scripts) {
                if (script.run(type, event)) { LogWriter.info("Client script executed: " + type + "; Event: " + event + "..."); }
            }
        } catch (Exception e) { LogWriter.error("Error:", e); }
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        IPlayer<?> iPlayer = null;
        if (CustomNpcs.proxy.getPlayer() != null) { iPlayer =  (IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(CustomNpcs.proxy.getPlayer()); }
        runScript(EnumScriptType.INIT.function, new PlayerEvent.InitEvent(iPlayer));
    }

    public void loadDefaultScripts() {
        if (loadDefault) { return; }
        // Stored Data
        File saveDir = new File(CustomNpcs.Dir, "client_default");
        if (saveDir.exists() || saveDir.mkdirs()) {
            CompoundTag compound = new CompoundTag();
            File sData = new File(saveDir, "world_data.json");
            try {
                if (!sData.exists()) { Util.instance.saveFile(sData, NBTJsonUtil.Convert(new CompoundTag())); }
                else { compound = NBTJsonUtil.LoadFile(sData); }
                LogWriter.debug("Load default client stored data - done");
            }
            catch (Exception e) { LogWriter.error("Error Default loading: " + sData.getName(), e); }
            if (compound.contains("IsMap", 3) && compound.contains("Content", 10)) { storedData.setNbt(compound); }
            else {
                for (String key : compound.getAllKeys()) {
                    storedData.put(key, Util.instance.readObjectFromNbt(compound.get(key)));
                }
            } // OLD
        }
        // Modules
        String language = getLanguage().toLowerCase();
        saveDir = new File(saveDir, language);
        if (saveDir.exists() || saveDir.mkdirs()) {
            ScriptController.Instance.clients.clear();
            ScriptController.Instance.clientSizes.clear();
            ScriptController.Instance.loadDir(saveDir, "", ScriptController.Instance.languages.get(Util.instance.deleteColor(getLanguage())), false, true);
            LogWriter.debug("Load default client modules - "+ScriptController.Instance.clients.size());
            // Main tab
            saveDir = new File(CustomNpcs.Dir, "client_default");
            File file = new File(saveDir, "client_scripts.json");
            try {
                if (!file.exists()) {
                    Util.instance.saveFile(file, NBTJsonUtil.Convert(save(new CompoundTag())));
                    LogWriter.debug("Create default client scripts - done");
                }
                else {
                    CompoundTag nbt = NBTJsonUtil.LoadFile(file);
                    if (nbt.contains("Constants", 10) || nbt.contains("Functions", 9)) {
                        CompoundTag constants = new CompoundTag();
                        constants.put("Constants", nbt.getCompound("Constants"));
                        constants.put("Functions", nbt.getList("Functions", 8));
                        ScriptController.Instance.constants = constants;
                    }
                    ScriptController.reloadConstants();
                    load(nbt);
                    LogWriter.debug("Load default client scripts - done: " + nbt.getCompound("Scripts").toString().length() + " size.");
                }
                EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.INIT, new PlayerEvent.InitEvent(null));
            }
            catch (Exception e) { LogWriter.error("Error Default loading: " + file.getName(), e); }
        }
        loadDefault = true;
    }

    public void saveDefaultScripts() {
        // Stored Data
        File saveDir = new File(CustomNpcs.Dir, "client_default");
        if (saveDir.exists() || saveDir.mkdirs()) {
            try {
                Util.instance.saveFile(new File(saveDir, "world_data.json"), NBTJsonUtil.Convert(storedData.getNbt().getMCNBT()));
                LogWriter.debug("Save Default Client stored data - done");
            } catch (Exception e) {
                LogWriter.error("Error Default saving: \"world_data.json\"", e);
            }
        }
        // Modules
        if (!ScriptController.Instance.clients.isEmpty()) {
            String language = getLanguage().toLowerCase();
            saveDir = new File(saveDir, language);
            if (saveDir.exists() || saveDir.mkdirs()) {
                for (String name : ScriptController.Instance.clients.keySet()) {
                    try {
                        File f = new File(saveDir, name);
                        if (!f.getParentFile().exists() && !f.getParentFile().mkdirs()) { continue; }
                        Util.instance.saveFile(new File(saveDir, name), ScriptController.Instance.clients.get(name));
                    } catch (Exception e) {
                        LogWriter.error("Error Default saving: " + name, e);
                    }
                }
                LogWriter.debug("Save Default Client modules - done");
            }
        }
        // Main tabs
        try {
            CompoundTag nbt = save(new CompoundTag());
            CompoundTag constants = new CompoundTag();
            ListTag functions = new ListTag();
            if (!ScriptController.Instance.constants.isEmpty()) {
                for (String key : ScriptController.Instance.constants.getCompound("Constants").getAllKeys()) {
                    Tag tag = ScriptController.Instance.constants.getCompound("Constants").get(key);
                    if (tag != null) { constants.put(key, tag); }
                }
                functions.addAll(ScriptController.Instance.constants.getList("Functions", 8));
            }
            nbt.put("Constants", constants);
            nbt.put("Functions", functions);
            Util.instance.saveFile(new File(saveDir, "client_scripts.json"), NBTJsonUtil.Convert(nbt));
            LogWriter.debug("Save Default Client scripts - done");
        }
        catch (Exception e) { LogWriter.error("Error Default saving: \"client_scripts.json\"", e); }
        loadDefault = false;
    }

}
