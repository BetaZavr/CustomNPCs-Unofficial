package noppes.npcs.controllers.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.eventbus.api.Event;
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
   public MutableComponent noticeString(String type, Object event) {
      return Component.literal("Forge Scripts ").withStyle(ChatFormatting.DARK_GRAY)
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

}
