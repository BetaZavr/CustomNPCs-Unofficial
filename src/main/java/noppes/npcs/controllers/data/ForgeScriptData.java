package noppes.npcs.controllers.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;

public class ForgeScriptData
extends BaseScriptData {

	@Override
	public boolean isEnabled() { return super.isEnabled() && CustomNpcs.EnableForgeScripting; }

	@Override
	public Component noticeString(String type, Object event) {
		return Component.literal("Forge Scripts ").withStyle(TextFormatting.DARK_GRAY)
				.append(super.noticeString(type, event));
	}
	
	@Override
	public void runScript(String type, Event event) {
		if (!isEnabled()) { return; }
		if (ScriptController.Instance.lastLoaded > lastInited) {
			lastInited = ScriptController.Instance.lastLoaded;
			if (!type.equalsIgnoreCase(EnumScriptType.INIT.function)) { EventHooks.onForgeInit(this); }
		}
		for (ScriptContainer script : scripts) { script.run(type, event); }
	}

	@Override
	public void load(NBTTagCompound compound) {
		super.load(compound);
		EventHooks.onForgeInit(this);
	}

}
