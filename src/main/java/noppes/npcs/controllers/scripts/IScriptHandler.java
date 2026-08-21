package noppes.npcs.controllers.scripts;

import java.util.List;
import java.util.Map;

import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.api.interfaces.ParamName;

public interface IScriptHandler {

   void runScript(@ParamName("type") String type, @ParamName("event") Event event);

   boolean isClient();

   boolean getEnabled();

   void setEnabled(@ParamName("isEnabled") boolean isEnabled);

   String getLanguage();

   void setLanguage(@ParamName("language") String language);

   List<ScriptContainer> getScripts();

   MutableComponent noticeString(@ParamName("type") String type, @ParamName("event") Object event);

   Map<Long, String> getConsoleText();

   void clearConsole();

   boolean isEnabled();

   // New from Unofficial (BetaZavr)
   void clearConsoleText(@ParamName("key") Long key);

   void setLastInited(@ParamName("timeMC") long timeMC);

   void init();

}
