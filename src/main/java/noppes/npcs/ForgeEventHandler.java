package noppes.npcs;

import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.scripts.IScriptHandler;
import noppes.npcs.shared.common.util.LogWriter;
import org.apache.commons.lang3.StringUtils;

public class ForgeEventHandler {

   public static final Map<Class<?>, String> eventNames = new HashMap<>();
   public static final Map<Class<?>, String> clientEventNames = new HashMap<>();

   @SubscribeEvent
   public void forgeEntity(Event event) {
      if (event == null) { return; }
      IScriptHandler handler = getHandler();
      if (handler != null) {
         CustomNpcs.debugData.start("Mod");
         try { EventHooks.onForgeEvent(handler, event); }
         catch (Throwable t) { LogWriter.error("Error in " + event.getClass().getName(), t); }
         CustomNpcs.debugData.end("Mod");
      }
   }

   protected IScriptHandler getHandler() {
      IScriptHandler handler = ScriptController.Instance.forgeScripts;
      if (handler.isClient()) { handler = ScriptController.Instance.clientScripts; }
      return handler.isEnabled() ? handler : null;
   }

   public static String getEventName(Class<?> c) {
      String eventName = c.getName();
      int i = eventName.lastIndexOf(".");
      return StringUtils.uncapitalize(eventName.substring(i + 1).replace("$", ""));
   }

}
