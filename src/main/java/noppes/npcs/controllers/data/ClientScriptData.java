package noppes.npcs.controllers.data;

import java.io.File;
import java.util.Objects;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.wrapper.data.Data;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.util.Util;
import noppes.npcs.util.NBTJsonUtil;

public class ClientScriptData
		extends BaseScriptData {

	public boolean loadDefault = false;
	public final Data storedData = new Data();

	@Override
	public boolean isClient() { return true; }

	@Override
	public boolean isEnabled() { return super.isEnabled() && CustomNpcs.EnableClientScripting; }

	@Override
	public Component noticeString(String type, Object event) {
		return Component.literal("Client Scripts ").withStyle(TextFormatting.DARK_GRAY)
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
				if (script.run(type, event)) {
					LogWriter.info("Client script executed: " + type + "; Event: " + event + "...");
				}
			}
		} catch (Exception e) { LogWriter.error("Error:", e); }
	}

	@Override
	public void load(NBTTagCompound compound) {
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
			NBTTagCompound compound = new NBTTagCompound();
			File sData = new File(saveDir, "world_data.json");
			try {
				if (!sData.exists()) { Util.instance.saveFile(sData, NBTJsonUtil.Convert(new NBTTagCompound())); }
				else { compound = NBTJsonUtil.LoadFile(sData); }
				LogWriter.debug("Load default client stored data - done");
			}
			catch (Exception e) { LogWriter.error("Error Default loading: " + sData.getName(), e); }
			if (compound.hasKey("IsMap", 3) && compound.hasKey("Content", 10)) { storedData.setNbt(compound); }
			else {
				for (String key : compound.getKeySet()) {
					storedData.put(key, Util.instance.readObjectFromNbt(compound.getTag(key)));
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
					Util.instance.saveFile(file, NBTJsonUtil.Convert(save(new NBTTagCompound())));
					LogWriter.debug("Create default client scripts - done");
				}
				else {
					NBTTagCompound nbt = NBTJsonUtil.LoadFile(file);
					if (nbt.hasKey("Constants", 10) || nbt.hasKey("Functions", 9)) {
						NBTTagCompound constants = new NBTTagCompound();
						constants.setTag("Constants", nbt.getCompoundTag("Constants"));
						constants.setTag("Functions", nbt.getTagList("Functions", 8));
						ScriptController.Instance.constants = constants;
					}
					ScriptController.reloadConstants();
					load(nbt);
					LogWriter.debug("Load default client scripts - done: " + nbt.getCompoundTag("Scripts").toString().length() + " size.");
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
			NBTTagCompound nbt = save(new NBTTagCompound());
			NBTTagCompound constants = new NBTTagCompound();
			NBTTagList functions = new NBTTagList();
			if (!ScriptController.Instance.constants.hasNoTags()) {
				for (String key : ScriptController.Instance.constants.getCompoundTag("Constants").getKeySet()) {
					constants.setTag(key, ScriptController.Instance.constants.getCompoundTag("Constants").getTag(key));
				}
				for (NBTBase tag : ScriptController.Instance.constants.getTagList("Functions", 8)) {
					functions.appendTag(tag);
				}
			}
			nbt.setTag("Constants", constants);
			nbt.setTag("Functions", functions);
			Util.instance.saveFile(new File(saveDir, "client_scripts.json"), NBTJsonUtil.Convert(nbt));
			LogWriter.debug("Save Default Client scripts - done");
		}
		catch (Exception e) { LogWriter.error("Error Default saving: \"client_scripts.json\"", e); }
		loadDefault = false;
	}

}
