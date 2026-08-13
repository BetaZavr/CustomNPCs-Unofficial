package noppes.npcs.controllers.data;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.EventHooks;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;

public class ForgeScriptData
extends BaseScriptData {

   @Override
   public MutableComponent noticeString(String type, Object event) {
      return Component.literal("Forge Scripts ").withStyle(ChatFormatting.DARK_GRAY)
              .append(super.noticeString(type, event));
   }

   @Override
   public void runScript(String type, Event event) {
      if (!isEnabled()) { return; }
      CustomNPCsScheduler.runTack(() -> {
         try {
            if (ScriptController.Instance.lastLoaded > lastInited) {
               lastInited = ScriptController.Instance.lastLoaded;
               if (!type.equalsIgnoreCase(EnumScriptType.INIT.function)) { EventHooks.onForgeInit(this); }
            }
            for (ScriptContainer script : scripts) { script.run(type, event); }
         } catch (Exception e) { LogWriter.error("Error:", e); }
      });
   }

   public void load(CompoundTag compound) {
      super.load(compound);
      EventHooks.onForgeInit(this);
   }

}
