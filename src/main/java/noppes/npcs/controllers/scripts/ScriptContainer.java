package noppes.npcs.controllers.scripts;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NBTTags;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.constants.*;
import noppes.npcs.api.event.BlockEvent;
import noppes.npcs.api.event.ItemEvent;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.handler.IDataObject;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.api.wrapper.DataObject;
import noppes.npcs.api.wrapper.data.Data;
import noppes.npcs.blocks.tiles.TileNpcEntity;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataScript;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.ScriptEncryption;
import noppes.npcs.util.Util;

import javax.script.ScriptEngine;

public class ScriptContainer {

   public static final Map<String, Object> DATA = new HashMap<>();
   public static ScriptContainer Current;
   public String fullscript = "";
   public String script = ""; // Storage of more than 30 000+ characters is now allowed
   public TreeMap<Long, String> console = new TreeMap<>();
   public List<String> scripts = new ArrayList<>();
   public long lastCreated = 0L;
   private String currentScriptLanguage = null;
   private final IScriptHandler handler;

   // New from Unofficial (GoodBird)
   private static final String lock = "cnpcslock";
   private IScriptExecutor executor = null;

   // New from Unofficial (BetaZavr)
   public boolean canEncryptCode = false;

   static {
      FillMap(AlignmentType.class);
      FillMap(AnimationKind.class);
      FillMap(AnimationType.class);
      FillMap(EntityType.class);
      FillMap(GuiComponentType.class);
      FillMap(RoleType.class);
      FillMap(JobType.class);
      FillMap(SideType.class);
      FillMap(PotionEffectType.class);
      FillMap(ParticleType.class);
      FillMap(OptionType.class);
      FillMap(MarkType.class);
      FillMap(ItemType.class);
      FillMap(TacticalType.class);
      DATA.put("api", NpcAPI.Instance());
      DATA.put("API", NpcAPI.Instance());
      DATA.put("cnpcs", CustomNpcs.instance);
      DATA.put("PosZero", BlockPosWrapper.ZERO);
   }

   private static void FillMap(Class<?> c) {
      if (!c.isEnum()) { return; }
      DATA.put(c.getSimpleName(), c.getDeclaringClass() != null ? c.getDeclaringClass() : c);
      for (Object e : c.getEnumConstants()) {
         try {
            Method m = e.getClass().getMethod("get");
            if (m.getReturnType() != int.class) { continue; }
            DATA.put(c.getSimpleName() + "_" + ((Enum<?>) e).name(), m.invoke(e));
         } catch (Throwable error) { LogWriter.error(error); }
      }
   }

   public ScriptContainer(IScriptHandler handlerIn) { handler = handlerIn; }

   public void load(CompoundTag compound) {
      console = NBTTags.getLongStringMap(compound.getList("Console", 10));
      scripts = NBTTags.getStringList(compound.getList("ScriptList", 10));
      lastCreated = 0L;

      // New from Unofficial (BetaZavr)
      if (compound.contains("Script", 9)) {
         ListTag list = compound.getList("Script", 8);
         StringBuilder sb = new StringBuilder();
         for (int i = 0; i < list.size(); i++) { sb.append(list.getString(i)); }
         script = sb.toString();
      }
      else { script = compound.getString("Script"); }
      if (console.isEmpty()) { ScriptController.Instance.tryRemoveErrored(this); }
      else { ScriptController.Instance.tryAddErrored(this); }
      fullscript = null;
      canEncryptCode = compound.getBoolean("HasNoEncryptScriptCode");
      if (handler.isClient() && executor != null) { executor.setErrored(false); }
   }

   public CompoundTag save(CompoundTag compound) {
      compound.put("Console", NBTTags.nbtLongStringMap(console));
      compound.put("ScriptList", NBTTags.nbtStringList(scripts));

      // New from Unofficial (BetaZavr)
      if (script.length() < 25000) { compound.putString("Script", script); }
      else {
         ListTag list = new ListTag();
         int length = script.length();
         for (int start = 0; start < length; start += 25000) {
            int end = Math.min(start + 25000, length);
            list.add(StringTag.valueOf(script.substring(start, end)));
         }
         compound.put("Script", list);
      }
      compound.putBoolean("isClient", handler.isClient());
      compound.putBoolean("HasNoEncryptScriptCode", canEncryptCode);
      return compound;
   }

   public boolean isErrored() { return executor != null && executor.isErrored(); }

   private String getFullCode() {
      if (executor == null || !executor.isInit()) {
         canEncryptCode = script != null && !script.isEmpty();
         fullscript = script;
         if (!fullscript.isEmpty()) { fullscript = fullscript + "\n"; }
         ScriptController sData = ScriptController.Instance;
         Map<String, String> map = handler.isClient() ? sData.clients : sData.scripts;
         StringBuilder sbCode = new StringBuilder();
         for (String loc : scripts) {
            String code;
            if (!handler.isClient() && sData.encrypts.containsKey(loc)) { code = ScriptEncryption.decryptScriptFromFile(sData.encrypts.get(loc)); }
            else {
               code =  map.get(loc);
               if (code != null) { canEncryptCode = true; }
            }
            if (code != null && !code.isEmpty()) {
               sbCode.append(code).append("\n");
            }
         }
         fullscript += sbCode.toString();
         if (map.containsKey("all.js")) { fullscript = map.get("all.js") + "\n" + fullscript; }
      }
      return fullscript;
   }

   public void run(EnumScriptType type, Event event) { run(type.function, event); }

   /**
    * WARNING: Removed try catching itself - possible behaviour change.
    */
   public boolean run(String functionName, Object event) {
      Object key = event instanceof BlockEvent ? "Block"
              : event instanceof PlayerEvent ? "Player"
              : event instanceof ItemEvent ? "Item" : event instanceof NpcEvent ? "Npc" : null;
      CustomNpcs.debugData.start(key, functionName);
      if (executor == null || !currentScriptLanguage.equals(handler.getLanguage())) { fillEngine(handler.getLanguage()); }
      if (executor != null && !executor.isErrored() && hasCode() && !executor.isUnknownFunction(functionName) && CustomNpcs.EnableScripting) {
         ScriptEngine engine = executor.getEngine();
         if (handler.isClient() && (engine.get("mc") == null || engine.get("storedData") == null)) { fillEngineClient(engine); }
         if (!executor.isErrored()) {
            if (ScriptController.Instance.lastLoaded > lastCreated) {
               canEncryptCode = false;
               lastCreated = ScriptController.Instance.lastLoaded;
               executor.setScript(getFullCode());
            }
            synchronized (lock) {
               Current = this;
               String consoleOutput = "";
               try { consoleOutput = executor.run(handler, functionName, event); }
               finally {
                  appendConsole(consoleOutput.trim());
                  Current = null;
               }
            }
         }
      }
      CustomNpcs.debugData.end(key, functionName);
      return executor != null && executor.isErrored();
   }

   public boolean hasCode() { return !getFullCode().isEmpty(); }

   public boolean isValid() { return !executor.isErrored(); }

   // Change or added new from Unofficial (BetaZavr)
   public boolean hasHandler() {
      if (handler == null) { return false; }
      if (handler instanceof DataScript) {
         EntityNPCInterface npc = ((DataScript) handler).getNPC();
         return npc != null && npc.getId() > 0 && npc.equals(npc.level().getEntity(npc.getId()));
      }
      if (handler instanceof TileNpcEntity) {
         Level level = ((TileNpcEntity) handler).getLevel();
         if (level == null) { return false; }
         BlockPos pos = ((TileNpcEntity) handler).getBlockPos();
         return level.getBlockEntity(pos) instanceof IScriptBlockHandler;
      }
      return true;
   }

   public void fillEngine(String language) {
      if (executor == null || !currentScriptLanguage.equals(language)) {
         if (executor != null) { executor.close(); }
         Supplier<IScriptExecutor> provider = ScriptController.Instance.executorProviders.computeIfAbsent(language.toLowerCase(), l -> Jsr223Executor::new);
         executor = provider.get();
         currentScriptLanguage = language;
         HashMap<String, Object> globals = new HashMap<>(DATA);
         globals.put("dump", new Dump());
         globals.put("log", new Log());
         executor.initialize(language, globals);
         ScriptEngine scriptEngine = executor.getEngine();
         if (scriptEngine == null) { return; }
         executor.setScript(getFullCode());

         CompoundTag constants = ScriptController.Instance.constants;
         String side = handler.isClient() ? "Client" : "Server";
         boolean needResave= false;
         //LogWriter.debug("Fill main classes to data");
         try { globals.put("Date", scriptEngine.eval("Java.type('" + Date.class.getName() + "')")); } catch (Throwable ignored) { LogWriter.error("Not put key \"Date\" to engine"); }
         try { globals.put("Calendar", scriptEngine.eval("Java.type('" + Calendar.class.getName() + "')")); } catch (Throwable ignored) { LogWriter.error("Not put key \"Calendar\" to engine"); }
         try { globals.put("System", scriptEngine.eval("Java.type('" + System.class.getName() + "')")); } catch (Throwable ignored) { LogWriter.error("Not put key \"System\" to engine"); }
         // Custom Functions
         //LogWriter.debug("Fill custom functions to data");
         ListTag functions = constants.getCompound("Functions").getList(language, 8);
         for (int i = 0; i < functions.size(); i++) {
            String body = functions.getString(i);
            try {
               if (!body.contains(" ") || !body.contains("(")) { continue; }
               String key = body.substring(body.indexOf(" ") + 1, body.indexOf("("));
               if (key.isEmpty()) { continue; }
               try {
                  //LogWriter.debug("Put function to data key: " + key + "; value: " + body);
                  DATA.put(key, scriptEngine.eval(body));
               }
               catch (Throwable e) {
                  LogWriter.error("Not add function key: \"" + body + "\"; body: \"" + body + "\" to data");
                  // save error
                  if (!constants.getCompound("Functions").contains("EvalIsError", 10)) {
                     constants.getCompound("Functions").put("EvalIsError", new CompoundTag());
                  }
                  if (!constants.getCompound("Functions").getCompound("EvalIsError").contains(side, 10)) {
                     constants.getCompound("Functions").getCompound("EvalIsError").put(side, new CompoundTag());
                  }
                  ListTag errors = constants.getCompound("Functions").getCompound("EvalIsError").getCompound(side).getList(language, 8);
                  boolean has = false;
                  for (int j = 0; j < errors.size(); j++) {
                     if (errors.getString(i).equals(body)) { has = true; break; }
                  }
                  if (!has) {
                     errors.add(StringTag.valueOf(e.getCause().getClass().getSimpleName() + ": " + body));
                     constants.getCompound("Functions").getCompound("EvalIsError").getCompound(side).put(language, errors);
                     needResave = true;
                  }
               }
            }
            catch (Throwable e) { constants.getList("Functions", 10).getCompound(i).putBoolean("EvalIsError", true); }
         }
         // Custom Constants
         //LogWriter.debug("Fill custom constants to data");
         CompoundTag cons = constants.getCompound("Constants").getCompound(language);
         for (String key : cons.getAllKeys()) {
            Object value = getNBTValue(Objects.requireNonNull(cons.get(key)));
            String err = "";
            if (value == null) { err = "NullPointerException"; }
            else {
               try {
                  //LogWriter.debug("Put constant to data key: " + key + "; value: " + value);
                  if (value instanceof String) {
                     try { DATA.put(key, scriptEngine.eval((String) value)); }
                     catch (Throwable e) { DATA.put(key, value); }
                  }
                  else { DATA.put(key, value); }
               }
               catch (Throwable t) { err = t.getCause().getClass().getSimpleName(); }
            }
            if (!err.isEmpty()) {
               LogWriter.error("Not add constant key: \"" + key + "\"; value: \"" + value + "\" to data");
               // save error
               if (!constants.getCompound("Constants").contains("EvalIsError", 10)) {
                  constants.getCompound("Constants").put("EvalIsError", new CompoundTag());
               }
               if (!constants.getCompound("Constants").getCompound("EvalIsError").contains(side, 10)) {
                  constants.getCompound("Constants").getCompound("EvalIsError").put(side, new CompoundTag());
               }
               CompoundTag errors = constants.getCompound("Constants").getCompound("EvalIsError").getCompound(side).getCompound(language);
               errors.putString(key, err);
               constants.getCompound("Constants").getCompound("EvalIsError").getCompound(side).put(language, errors);
               needResave = true;
            }
         }
         // Try to put all
         //LogWriter.debug("Fill data to engine");
         for (Map.Entry<String, Object> entry : DATA.entrySet()) {
            try {
               //LogWriter.debug("Put to engine key: " + entry.getKey() + "; value: " + entry.getValue());
               scriptEngine.put(entry.getKey(), entry.getValue());
            } catch (Throwable ignored) { LogWriter.error("Not put data key \"" + entry.getKey() + "\" to engine"); }
         }
         if (handler.isClient()) { fillEngineClient(scriptEngine); }
         // Main Constants
         //LogWriter.debug("Fill mod fields and methods to engine");
         try { scriptEngine.put("currentThread", Thread.currentThread().getName()); } catch (Throwable ignored) { LogWriter.error("Not put key \"currentThread\" to engine"); }
         try { scriptEngine.put("main", scriptEngine); } catch (Throwable ignored) { LogWriter.error("Not put key \"main\" to engine"); }
         try { scriptEngine.put("currentScriptContainer", this); } catch (Throwable ignored) { LogWriter.error("Not put key \"currentScriptContainer\" to engine"); }
         try { scriptEngine.put("tempData", new Data()); } catch (Throwable ignored) { LogWriter.error("Not put key \"tempData\" to engine"); }
         // resave constants file
         if (needResave) { Util.instance.saveFile(ScriptController.Instance.constantScriptsFile(), constants.copy()); }
         //LogWriter.debug("Done fill engine");
      }
   }

   private void fillEngineClient(ScriptEngine scriptEngine) {
      if (handler.isClient()) {
         // Try to put MC
         try { scriptEngine.put("mc", ClientProxy.mcWrapper); }
         catch (Throwable ignored) { LogWriter.error("Not put key \"mc\" to engine"); }
         try { scriptEngine.put("storedData", ScriptController.Instance.clientScripts.storedData); }
         catch (Throwable ignored) { LogWriter.error("Not put key \"storedData\" to engine"); }
      }
   }

   public boolean canEncryptCode() {
      if (canEncryptCode) { return true; }
      if (!(script == null || script.isEmpty())) {
         String tempScript = script.replace(" ", "")
                 .replace("" + ((char) 9), "")
                 .replace("" + ((char) 10), "");
         return !tempScript.isEmpty();
      }
      return false;
   }

   public void appendConsole(String message) {
      if (message != null && !message.isEmpty()) {
         long time = System.currentTimeMillis();
         if (console.containsKey(time)) { message = console.get(time) + "\n" + message; }
         console.put(time, message);
         while (console.size() > 40) { console.remove(console.firstKey()); }
         ScriptController.Instance.tryAddErrored(this);
      }
   }

   public IScriptHandler getHandler() { return handler; }

   private static Object getNBTValue(Tag tag) {
      Object value = null;
      switch (tag.getId()) {
         case 1:
            value = ((ByteTag) tag).getAsByte();
            break;
         case 2:
            value = ((ShortTag) tag).getAsShort();
            break;
         case 3:
            value = ((IntTag) tag).getAsInt();
            break;
         case 4:
            value = ((LongTag) tag).getAsLong();
            break;
         case 5:
            value = ((FloatTag) tag).getAsFloat();
            break;
         case 6:
            value = ((DoubleTag) tag).getAsDouble();
            break;
         case 7:
            value = ((ByteArrayTag) tag).getAsByteArray();
            break;
         case 8:
            value = tag.getAsString();
            break;
         case 9: // List
            List<Object> list = new ArrayList<>();
            for (Tag obj : (ListTag) tag) {
               Object v = getNBTValue(obj);
               if (v != null) {
                  list.add(v);
               }
            }
            value = list.toArray(new Object[0]);
            break;
         case 10: // Compound
            Map<String, Object> comp = new TreeMap<>();
            for (String key : ((CompoundTag) tag).getAllKeys()) {
               Object v = getNBTValue(Objects.requireNonNull(((CompoundTag) tag).get(key)));
               if (v != null) {
                  comp.put(key, v);
               }
            }
            value = comp;
            break;
         case 11:
            value = ((IntArrayTag) tag).getAsIntArray();
            break;
         case 12:
            value = ((LongArrayTag) tag).getAsLongArray();
            break;
      }
      return value;
   }

   public void setInit(boolean isInit) {
      executor.setInit(isInit);
      lastCreated = 0L;
      canEncryptCode = false;
   }

   public ScriptEngine getEngine() { return executor != null ? executor.getEngine() : null; }

   public Component noticeString() {
      return hasHandler() ? handler.noticeString(null, null) : null;
   }

   private static class Dump implements Function<Object, IDataObject> {

      public IDataObject apply(Object object) { return new DataObject(object); }

   }

   private class Log implements Function<Object, Void> {

      public Void apply(Object o) {
         String message = o == null ? "null" : o.toString();
         appendConsole(message);
         LogWriter.info(message);
         return null;
      }
   }

}
