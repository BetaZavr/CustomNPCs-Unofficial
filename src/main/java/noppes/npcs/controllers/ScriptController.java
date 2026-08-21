package noppes.npcs.controllers;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.LevelEvent.Save;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.wrapper.WorldWrapper;
import noppes.npcs.api.wrapper.WrapperNpcAPI;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.blocks.tiles.TileScriptedDoor;
import noppes.npcs.controllers.data.*;
import noppes.npcs.controllers.scripts.IScriptExecutor;
import noppes.npcs.controllers.scripts.Jsr223Executor;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.*;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.NBTJsonUtil;
import noppes.npcs.util.TempFile;
import noppes.npcs.util.Util;

public class ScriptController {

   public static ScriptController Instance;
   public static boolean HasStart = false;
   public Map<String, String> languages = new HashMap<>();
   public Map<String, ScriptEngineFactory> factories = new HashMap<>();
   public Map<String, String> scripts = new HashMap<>();
   public PlayerScriptData playerScripts = new PlayerScriptData(null);
   public ForgeScriptData forgeScripts = new ForgeScriptData();
   public long lastLoaded = 0L;
   public long lastPlayerUpdate = 0L;
   public File dir;
   public File localDir;
   public CompoundTag compound = new CompoundTag();
   public boolean shouldSave = false;

   // New fields from Unofficial (GoodBird)
   public Map<String, Supplier<IScriptExecutor>> executorProviders = new HashMap<>();

   // New fields from Unofficial (BetaZavr)
   private static final boolean isClient = Util.instance.getSide().isClient();

   public final Map<String, Long> sizes = new TreeMap<>();
   public final Map<String, Long> clientSizes = new TreeMap<>();
   public final Map<String, File> encrypts = new TreeMap<>();
   public Map<String, String> clients = new HashMap<>();
   public File clientDir;

   public ClientScriptData clientScripts = new ClientScriptData();
   public PotionScriptData potionScripts = new PotionScriptData();
   public NpcScriptData npcsScripts = new NpcScriptData();
   public CompoundTag constants = new CompoundTag();

   private static String currentAgreement;
   private final List<String> agreements = new ArrayList<>(); // net.minecraft.client.gui.screens.multiplayer.SafetyScreen
   private final List<ScriptContainer> errors = new ArrayList<>();
   private final Map<Integer,List<Object>> elements = new TreeMap<>();

   public boolean isLoad = false;

   @SuppressWarnings("unchecked")
   public ScriptController() {
      Instance = this;
      if (!CustomNpcs.NashorArguments.isEmpty()) { System.setProperty("nashorn.args", CustomNpcs.NashorArguments); }
      LogWriter.info("Script Engines Available:");
      ScriptEngineManager manager = new ScriptEngineManager();
      Class<?> c;
      ScriptEngineFactory factory;
      Constructor<?> constructor;
      String language = "";
      List<String> extensions;
      // Rhino
      try {
         c = Class.forName("org.mozilla.javascript.engine.RhinoScriptEngineFactory");
         constructor = c.getDeclaredConstructor();
         factory = (ScriptEngineFactory) constructor.newInstance();
         factory.getScriptEngine();
         manager.registerEngineName("rhino", factory);
         manager.registerEngineExtension("js", factory);
         manager.registerEngineMimeType("application/rhino", factory);
         language = factory.getLanguageName();
         languages.put(language, ".js");
         factories.put(language.toLowerCase(), factory);
         executorProviders.put(language.toLowerCase(), Jsr223Executor::new);
         LogWriter.info("Added script Library: \"" + language + "\"; type: \"RhinoScriptEngineFactory\"; files index: \".js\"");
      } catch (Exception e) { LogWriter.debug("Rhino JS is missed"); }
      // Groovy
      try {
         c = Class.forName("org.codehaus.groovy.jsr223.GroovyScriptEngineFactory");
         constructor = c.getDeclaredConstructor();
         factory = (ScriptEngineFactory) constructor.newInstance();
         factory.getScriptEngine();
         manager.registerEngineName("groovy", factory);
         manager.registerEngineExtension("groovy", factory);
         manager.registerEngineMimeType("application/groovy", factory);
         language = factory.getLanguageName();
         languages.put(language, ".groovy");
         factories.put(language.toLowerCase(), factory);
         executorProviders.put(language.toLowerCase(), Jsr223Executor::new);
         LogWriter.info("Added script Library: \"" + language + "\"; type: \"GroovyScriptEngineFactory\"; files index: \".groovy\"");
      } catch (Exception e) { LogWriter.debug("Groovy JS is missed"); }
      // Kotlin
      try {
         c = Class.forName("org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory");
         constructor = c.getDeclaredConstructor();
         factory = (ScriptEngineFactory) constructor.newInstance();
         factory.getScriptEngine();
         manager.registerEngineName("kotlin", factory);
         manager.registerEngineExtension("kt", factory);
         manager.registerEngineMimeType("application/kotlin", factory);
         language = factory.getLanguageName();
         languages.put(language, ".kt");
         factories.put(language.toLowerCase(), factory);
         executorProviders.put(language.toLowerCase(), Jsr223Executor::new);
         LogWriter.info("Added script Library: \"" + language + "\"; type: \"KotlinJsr223JvmLocalScriptEngineFactory\"; files index: \".kt\"");
      } catch (Exception e) { LogWriter.debug("Kotlin JS is missed"); }
      // In Noppes Mod
      try {
         c = Class.forName("noppes.scriptengines.ScriptEngines");
         List<ScriptEngineFactory> seFactories = (List<ScriptEngineFactory>) c.getDeclaredField("factories").get(null);
         Iterator<ScriptEngineFactory> var3 = seFactories.iterator();
         label87:
         while(true) {
            do {
               do {
                  do {
                     if (!var3.hasNext()) {
                        break label87;
                     }
                     factory = var3.next();
                  } while(factory.getExtensions().isEmpty());
               } while(languages.containsKey(factory.getLanguageName()));
            } while(!(factory.getScriptEngine() instanceof Invocable) && !factory.getLanguageName().equals("lua"));
            extensions = factory.getExtensions();
            String ext = "." + extensions.get(0).toLowerCase();
            language = factory.getLanguageName();
            languages.put(language, ext);
            factories.put(language.toLowerCase(), factory);
            executorProviders.put(language.toLowerCase(), Jsr223Executor::new);
            LogWriter.info("Added script Library: \"" + language + "\"; type: \"" + factory.getClass().getSimpleName() + "\"; files index: \"" + ext + "\"");
         }
      } catch (Exception e) { LogWriter.debug("\"" + language + "\" is missed"); }
      // Any
      for (ScriptEngineFactory scriptEngineFactory : manager.getEngineFactories()) {
         factory = scriptEngineFactory;
         language = factory.getLanguageName();
         try {
            if (!factory.getExtensions().isEmpty() && !languages.containsKey(language) && (factory.getScriptEngine() instanceof Invocable || language.equals("lua"))) {
               extensions = factory.getExtensions();
               String ext = "." + extensions.get(0).toLowerCase();
               languages.put(language, ext);
               factories.put(language.toLowerCase(), factory);
               executorProviders.put(language.toLowerCase(), Jsr223Executor::new);
               LogWriter.info("Added script Library: \"" + language + "\"; type: \"" + factory.getClass().getSimpleName() + "\"; files index: \"" + ext + "\"");
            }
         } catch (Exception e) {
            LogWriter.error("Error Added Script Library: \"" + factory.getLanguageName() + "\": " + e);
         }
      }
      // ECMAScript Nashorn
      try {
         LogWriter.debug("Try create Nashorn Script Engine");
         c = Class.forName("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory");
         constructor = c.getDeclaredConstructor();
         factory = (ScriptEngineFactory) constructor.newInstance();
         factory.getScriptEngine();
         language = factory.getLanguageName(); // ECMAScript
         boolean isNotRegister = true;
         if (languages.containsKey(language)) {
            String ext = languages.get(language);
            ScriptEngineFactory fac = factories.get(language.toLowerCase());
            if (fac != null) {
               String newName = fac.getClass().getSimpleName().replace("EngineFactory", "");
               languages.put(newName, ext);
               factories.put(newName.toLowerCase(), fac);
               executorProviders.put(newName.toLowerCase(), Jsr223Executor::new);
               manager.registerEngineName(newName.toLowerCase(), fac);
               manager.registerEngineMimeType("application/" + newName.toLowerCase(), factory);
               isNotRegister = !ext.equals(".js");
            }
         }
         manager.registerEngineName(language.toLowerCase(), factory);
         manager.registerEngineMimeType("application/" + language.toLowerCase(), factory);
         if (isNotRegister) { manager.registerEngineExtension("js", factory); }
         languages.put(language, ".js");
         factories.put(language.toLowerCase(), factory);
         executorProviders.put(language.toLowerCase(), Jsr223Executor::new);
         LogWriter.info("Added script Library: \"" + language + "\"; type: \"" + factory.getClass().getSimpleName() + "\"; files index: \".js\"");
      } catch (Exception e) { LogWriter.debug("Nashorn JS is missed"); }
      if (isClient) { loadAgreements(); }
   }

   public static String getLevelKey() { return currentAgreement; }

   public static void setLevelKey(String levelKey) { currentAgreement = levelKey != null ? levelKey : ""; }

   public void loadCategories() {
      dir = CustomNpcs.getLevelSaveDirectory("scripts");
      localDir = CustomNpcs.getLevelSaveDirectory("scripts", true);
      if (!dir.exists() && !dir.mkdirs()) { return; }
      clientDir = new File(dir, "client");
      if (!clientDir.exists() && !clientDir.mkdirs()) { return; }
      if (!worldDataFile().exists()) { shouldSave = true; }
      WorldWrapper.clearTempdata();
      scripts.clear();
      sizes.clear();
      clients.clear();
      clientSizes.clear();
      for (String language : languages.keySet()) {
         String ext = languages.get(Util.instance.deleteColor(language));
         File scriptDir = new File(dir, language.toLowerCase());
         if (!scriptDir.exists() && !scriptDir.mkdir()) { continue; }
         else {
            loadDir(scriptDir, "", ext, false, false);
            loadDir(scriptDir, "", ext.replace(".", ".p"), true, false);
         }
         scriptDir = new File(clientDir, language.toLowerCase());
         if (scriptDir.exists() || scriptDir.mkdir()) { loadDir(scriptDir, "", ext, false, true); }
      }
      lastLoaded = System.currentTimeMillis();
      isLoad = true;
   }

   public void loadDir(File dir, String name, String ext, boolean encrypt, boolean isClient) {
      File[] files = dir.listFiles();
      if (files != null) {
         for (File file : files) {
            String filename = name + file.getName().toLowerCase();
            if (file.isDirectory()) { loadDir(file, filename + "/", ext, encrypt, isClient); }
            else if (filename.endsWith(ext)) {
               if (encrypt) {
                  if (!isClient) { encrypts.put(filename, file); }
               }
               else {
                  String code = Util.instance.loadFile(file);
                  if (isClient) { clients.put(filename, code); } else { scripts.put(filename, code); }
               }
               if (isClient) { clientSizes.put(filename, file.length()); } else { sizes.put(filename, file.length()); }
            }
         }
      }
   }

   public boolean loadStoredData() {
      compound = new CompoundTag();
      File file = worldDataFile();
      try {
         if (file.exists()) {
            compound = NBTJsonUtil.LoadFile(file);
            WrapperNpcAPI.resetScriptControllerData(compound);
            shouldSave = false;
            return true;
         }
      }
      catch (Exception var3) { LogWriter.error("Error loading: " + file.getAbsolutePath(), var3); }
      return false;
   }

   private File worldDataFile() {
      return new File(localDir, "world_data.json");
   }

   private File playerScriptsFile() {
      return new File(dir, "player_scripts.json");
   }

   private File forgeScriptsFile() {
      return new File(dir, "forge_scripts.json");
   }

   public boolean loadPlayerScripts() {
      playerScripts.clear();
      File file = playerScriptsFile();
      try {
         if (file.exists()) {
            playerScripts.load(NBTJsonUtil.LoadFile(file));
            return true;
         }
      }
      catch (Exception e) { LogWriter.error("Error loading: " + file.getAbsolutePath(), e); }
      return false;
   }

   public void setPlayerScripts(CompoundTag compound) {
      playerScripts.load(compound);
      File file = playerScriptsFile();
      try {
         NBTJsonUtil.SaveFile(file, compound);
         lastPlayerUpdate = System.currentTimeMillis();
      } catch (Exception e) {
         LogWriter.error(e);
      }
   }

   public boolean loadForgeScripts() {
      forgeScripts.clear();
      File file = forgeScriptsFile();
      try {
         if (file.exists()) {
            forgeScripts.load(NBTJsonUtil.LoadFile(file));
            return true;
         }
      }
      catch (Exception e) { LogWriter.error("Error loading: " + file.getAbsolutePath(), e); }
      return false;
   }

   public void setForgeScripts(CompoundTag compound) {
      forgeScripts.load(compound);
      File file = forgeScriptsFile();
      try {
         NBTJsonUtil.SaveFile(file, compound);
         forgeScripts.lastInited = -1L;
      } catch (Exception e) { LogWriter.error(e); }
   }

   public ScriptEngine getEngineByName(String language) {
      ScriptEngineFactory factory = factories.get(language.toLowerCase());
      if (factory == null) { return null; }
      if (factory.getClass().getSimpleName().equals("GraalJSEngineFactory")) { return getNewGraalEngine(); }
      return factory.getScriptEngine();
   }

   public ListTag nbtLanguages(boolean isClient) {
      ListTag list = new ListTag();
      for (String language : languages.keySet()) {
         String ext = languages.get(Util.instance.deleteColor(language));
         CompoundTag compound = new CompoundTag();
         ListTag scripts = new ListTag();
         Map<String, Long> map = getScripts(language, isClient);
         long[] cs = new long[map.size()];
         int i = 0;
         for (String script : map.keySet()) {
            scripts.add(StringTag.valueOf(script));
            cs[i++] = map.get(script) * (!script.endsWith(ext) ? -1 : 1);
         }
         compound.put("Scripts", scripts);
         compound.putString("Language", language);
         compound.putString("FileSfx", ext);
         compound.put("sizes", new LongArrayTag(cs));
         list.add(compound);
      }
      return list;
   }

   private TreeMap<String, Long> getScripts(String language, boolean isClient) {
      TreeMap<String, Long> map = new TreeMap<>();
      String ext = languages.get(Util.instance.deleteColor(language));
      if (ext == null) { return map; }
      for (String script : (isClient ? clients : scripts).keySet()) {
         if (script.endsWith(ext)) { map.put(script, (isClient ? clientSizes : sizes).get(script)); }
      }
      if (!isClient) {
         ext = ext.replace(".", ".p");
         for (String script : encrypts.keySet()) {
            if (script.endsWith(ext)) { map.put(script, sizes.get(script)); }
         }
      }
      return map;
   }

   @SubscribeEvent
   public void saveLevel(Save event) {
      if (!shouldSave || !(event.getLevel() instanceof ServerLevel) || ((ServerLevel)event.getLevel()).dimension() != Level.OVERWORLD) { return; }
      CustomNpcs.debugData.start("Mod");
      try { NBTJsonUtil.SaveFile(worldDataFile(), compound.copy()); }
      catch (Exception e) { LogWriter.except(e); }
      try { NBTJsonUtil.SaveFile(constantScriptsFile(), constants.copy()); }
      catch (Exception e) { LogWriter.except(e); }
      shouldSave = false;
      CustomNpcs.debugData.end("Mod");
   }

   // New from Unofficial (BetaZavr)
   public static void reloadConstants() { ScriptContainer.DATA.remove("dump"); }

   public void load() {
      CustomNpcs.debugData.start(null);
      ScriptController sData = ScriptController.Instance;
      sData.loadCategories();
      sData.loadStoredData();
      sData.loadPlayerScripts();
      sData.loadForgeScripts();
      sData.loadNPCsScripts();
      sData.loadPotionScripts();
      sData.loadConstantData();
      if (isClient) { sData.loadClientScripts(); }
      checkExampleModules();
      ScriptController.HasStart = true;
      CustomNpcs.debugData.end(null);
   }

   public void sendClientTo(ServerPlayer player) {
      if (!isLoad) { loadCategories(); }
      CompoundTag compound = new CompoundTag();
      clientScripts.save(compound);
      compound.put("Languages", ScriptController.Instance.nbtLanguages(true));
      compound.putString("DirPath", ScriptController.Instance.dir.getAbsolutePath());
      // collect and clear scripts and consoles
      Map<Integer, String> mapScripts = new TreeMap<>();
      Map<Integer, Map<Long, String>> mapConsoles = new TreeMap<>();
      ListTag scripts = compound.getList("Scripts", 10);
      for (int i = 0; i < scripts.size(); i++) {
         mapScripts.put(i, scripts.getCompound(i).getString("Script"));
         scripts.getCompound(i).putString("Script", "");
         ListTag consoles = scripts.getCompound(i).getList("Console", 10);
         for (int j = 0; j < consoles.size(); j++) {
            if (!mapConsoles.containsKey(i)) { mapConsoles.put(i, new LinkedHashMap<>()); }
            mapConsoles.get(i).put(consoles.getCompound(j).getLong("Long"), consoles.getCompound(j).getString("String"));
            consoles.getCompound(j).putString("String", "");
         }
      }
      Packets.send(player, new PacketClientScripts(compound));
      for (int tab : mapScripts.keySet()) {
         List<String> scriptStrings = Util.instance.getStringData(mapScripts.get(tab));
         int i = 0;
         for (String part : scriptStrings) {
            Packets.send(player, new PacketScriptText(tab, i++, scriptStrings.size(), part, true));
         }
      }
      for (int tab : mapConsoles.keySet()) {
         for (long time : mapConsoles.get(tab).keySet()) {
            List<String> consoleStrings = Util.instance.getStringData(mapConsoles.get(tab).get(time));
            int i = 0;
            for (String part : consoleStrings) {
               Packets.send(player, new PacketScriptConsole(tab, time, i++, consoleStrings.size(), part, true));
            }
         }
      }

      ListTag list = new ListTag();
      for (String key : clients.keySet()) {
         PlayerData data = PlayerData.get(player);
         if (!data.clientScriptFiles.containsKey(key)) {
            data.clientScriptFiles.put(key, new TempFile(key, 0, 1, clientSizes.get(key)));
         }
         TempFile file = data.clientScriptFiles.get(key);
         if (!file.isLoad()) {
            file.size = -1;
            file.saveType = 1;
            file.reset(clients.get(key));
         }
         list.add(file.getTitle());
      }
      CompoundTag fileList = new CompoundTag();
      fileList.put("FileList", list);
      Packets.send(player, new PacketSendFileList(fileList));
   }

   public boolean loadNPCsScripts() {
      npcsScripts.clear();
      File file = npcsScriptsFile();
      try {
         if (file.exists()) {
            npcsScripts.load(NBTJsonUtil.LoadFile(file));
            return true;
         }
      }
      catch (Exception e) { LogWriter.error("Error loading: " + file.getAbsolutePath(), e); }
      return false;
   }

   public void setNPCsScripts(CompoundTag compound) {
      npcsScripts.load(compound);
      File file = npcsScriptsFile();
      try {
         NBTJsonUtil.SaveFile(file, compound.copy());
         npcsScripts.lastInited = -1L;
      } catch (Exception e) { LogWriter.error("Error:", e); }
   }

   private File npcsScriptsFile() { return new File(dir, "npc_scripts.json"); }

   public boolean loadPotionScripts() {
      potionScripts.clear();
      File file = potionScriptsFile();
      try {
         if (file.exists()) {
            potionScripts.load(NBTJsonUtil.LoadFile(file));
            return true;
         }
      }
      catch (Exception e) { LogWriter.error("Error loading: " + file.getAbsolutePath(), e); }
      return false;
   }

   public void setPotionScripts(CompoundTag compound) {
      potionScripts.load(compound);
      File file = potionScriptsFile();
      try {
         NBTJsonUtil.SaveFile(file, compound.copy());
         potionScripts.lastInited = -1L;
      } catch (Exception e) { LogWriter.error("Error:", e); }
   }

   private File potionScriptsFile() { return new File(dir, "potion_scripts.json"); }

   public boolean loadClientScripts() {
      clientScripts.clear();
      File file = clientScriptsFile();
      try {
         if (!file.exists()) {
            clientScripts.load(NBTJsonUtil.LoadFile(file));
            return true;
         }
      }
      catch (Exception e) { LogWriter.error("Error loading: " + file.getAbsolutePath(), e); }
      return false;
   }

   public void setClientScripts(CompoundTag compound) { clientScripts.load(compound); }

   public File clientScriptsFile() {
      if (isClient && clientDir == null) {
         return new File(CustomNpcs.Dir, "client_default/client_scripts.json");
      }
      return new File(dir, "client_scripts.json");
   }

   public boolean loadConstantData() {
      constants = new CompoundTag();
      File file = constantScriptsFile();
      boolean isLoad = true;
      try {
         boolean isOLDValues = !file.exists();
         if (file.exists()) {
            constants = NBTJsonUtil.LoadFile(file);
            boolean needResave = false;
            // Main
            Tag tag = constants.get("Constants");
            if (!(tag instanceof CompoundTag)) {
               needResave = true;
               constants.put("Constants", new CompoundTag());
            }
            // Check OLD data:
            CompoundTag nbtC = new CompoundTag();
            CompoundTag cons = constants.getCompound("Constants");
            isOLDValues = cons.contains("TextComponentTranslation", 8);
            if (!isOLDValues) {
               Set<String> keys = new HashSet<>(cons.getAllKeys());
               for (String key : keys) {
                  if (!cons.contains(key)) { continue; }
                  tag = cons.get(key);
                  if (tag != null && (!(tag instanceof CompoundTag) || !factories.containsKey(key))) {
                     nbtC.put(key, tag);
                     cons.remove(key);
                     needResave = true;
                  }
               }
               if (needResave) {
                  cons.put("ecmascript", nbtC);
                  constants.put("Constants", cons);
               }
               tag = constants.get("Functions");
               if (!(tag instanceof CompoundTag)) {
                  // Check OLD data:
                  if (tag instanceof ListTag && ((ListTag) tag).getElementType() == 8) {
                     ListTag list = ((ListTag) tag).copy();
                     constants.put("Functions", new CompoundTag());
                     constants.getCompound("Functions").put("ecmascript", list);
                  } else {
                     constants.put("Functions", new CompoundTag());
                  }
                  needResave = true;
               }
               // Check contains all languages:
               for (String key : factories.keySet()) {
                  tag = constants.getCompound("Constants").get(key);
                  if (!(tag instanceof CompoundTag)) {
                     needResave = true;
                     constants.getCompound("Constants").put(key, new CompoundTag());
                  }
                  tag = constants.getCompound("Functions").get(key);
                  if (!(tag instanceof ListTag)) {
                     needResave = true;
                     constants.getCompound("Functions").put(key, new ListTag());
                  }
               }
               if (needResave) {
                  try {
                     Util.instance.saveFile(file, NBTJsonUtil.Convert(constants));
                     MutableComponent message = Component.literal("Constants have been rewritten for all scripts to ").withStyle(ChatFormatting.GRAY);
                     CommonUtil.NotifyOPs(message.append(file.getName()), true);
                  }
                  catch (Exception e) { LogWriter.except(e); }
               }
            }
         }
         if (isOLDValues) {
            for (String key : factories.keySet()) {
               if (!constants.getCompound("Constants").contains(key, 10)) {
                  constants.getCompound("Constants").put(key, new CompoundTag());
               }
               if (!constants.getCompound("Functions").contains(key, 10)) {
                  constants.getCompound("Functions").put(key, new CompoundTag());
               }
            }
            CompoundTag nbtC = getNbtDefaultConstants();
            ListTag list = new ListTag();
            list.add(StringTag.valueOf("function getField(key,object) { try { var f = dump(object).getField(key); if (f) { return f.getValue(); } } catch (error) { log('Error: \"'+key+'\" is not a Field or not found in \"'+object.getClass().getName()+'\"');} return null; }"));
            list.add(StringTag.valueOf("function setField(value,object,key) { try { var f = dump(object).getField(key); if (f) { return f.setValue(value); } } catch (error) { log('Error: \"'+key+'\" is not a Field or not found, or not type mismatch in \"'+object.getClass().getName()+'\". Error: ' + error); } return false; }"));
            list.add(StringTag.valueOf("function invoke(value,object,key) { try { var m = dump(object).getMethod(key); if (m) { var jo = Java.type('java.lang.Object[]'); if (value!=jo) { try { if (value.length>=0) { var v = new jo(value.length); for (var i=0; i<value.length; i++) { v[i] = value[i]; } return m.invoke(v); } } catch (err) { } var v = new jo(1); v[0] = value; return m.invoke(v); } else { return m.invoke(value); } } } catch (error) { log('Error: \"'+key+'\" is not a Method or not found, or not type mismatch in \"'+object.getClass().getName()+'\"'); } return null; }"));
            list.add(StringTag.valueOf("var HashMap = Java.type('java.util.HashMap'); var EMPTY_FUNCTION = new Function('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z', 'return;'); function getFunction(name) { var error; var key = 'custom_functions'; try { var fhm = api.getTempdata().get(key); if (fhm instanceof HashMap && fhm.containsKey(name)) { return fhm.get(name); } var dir = api.getLevelDir().toPath().resolve('scripts').resolve('functions'); var file = dir.resolve('example.json'); if (!file.toFile().exists()) { api.getMethods().saveFile(file.toFile(), 'args=agr0, agr1\\nbody=agr0 += 4.2;\\nreturn agr0 * agr1;'); } file = dir.resolve(name+'.json'); if (dir.toFile().exists()) { if (file.toFile().exists()) { var context = api.getMethods().loadFile(file.toFile()); if (context.contains('args=') && context.contains('body=')) { var i = context.indexOf('args='); var args = context.substring(i + 5, context.indexOf('\\n', i)).replace('\t', '').replace(' ', '').split(','); i = context.indexOf('body='); var body = context.substring(i + 5); var func = new Function(args, body); if (!(fhm instanceof HashMap)) { fhm = new HashMap(); } fhm.put(name, func); api.getTempdata().put(key, fhm); return func; } } } } catch (e) { error = e; } if (error) { log(error); } else { log('Error: Custom function \"' + name + '\" - not found'); } return EMPTY_FUNCTION; }"));
            constants.put("Constants", new CompoundTag());
            constants.put("Functions", new CompoundTag());
            constants.getCompound("Constants").put("ecmascript", nbtC);
            constants.getCompound("Functions").put("ecmascript", list);
            try {
               Util.instance.saveFile(file, NBTJsonUtil.Convert(constants));
               isLoad = false;
            }
            catch (Exception e) { LogWriter.except(e); }
         }
         reloadConstants();
      } catch (Exception e) {
         LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
         return false;
      }
      return isLoad;
   }

   public File constantScriptsFile() { return new File(dir, "constant_scripts.json"); }

   /**
    * GraalJSScriptEngine.create((Engine)null, Context.newBuilder("js")
    * .allowExperimentalOptions(true)
    * .allowHostClassLookup((s) -> true)
    * .allowCreateProcess(true)
    * .allowHostClassLoading(true)
    * .allowNativeAccess(true)
    * .allowAllAccess(true)
    * .allowIO(true)
    * .allowHostAccess(ScriptConstants.hostAccess)
    * .allowCreateProcess(true)
    * .option("js.ecmascript-version", "2022")
    * .option("js.nashorn-compat", "true"));
    */
   private ScriptEngine getNewGraalEngine() {
      try {
         Class<?> graal = Class.forName("com.oracle.truffle.js.scriptengine.GraalJSScriptEngine");
         Method create = null;
         for (Method m : graal.getMethods()) {
            if (m.getName().equals("create") && m.getParameterCount() == 2) {
               create = m;
               break;
            }
         }
         if (create == null) { return null; }

         Class<?> cnt = Class.forName("org.graalvm.polyglot.Context");
         Class<?> hostA = Class.forName("org.graalvm.polyglot.HostAccess");
         Object contextBuilder; // org.graalvm.polyglot.Context.Builder
         contextBuilder = cnt.getDeclaredMethod("newBuilder", String[].class).invoke(cnt,
                 (Object) new String[] { "js" });
         if (contextBuilder != null) {
            for (Method m : contextBuilder.getClass().getDeclaredMethods()) {
               switch (m.getName()) {
                  case "allowExperimentalOptions":
                  case "allowHostClassLoading":
                  case "allowNativeAccess":
                  case "allowIO":
                  case "allowCreateProcess":
                     contextBuilder = m.invoke(contextBuilder, true);
                     break;
                  case "allowHostClassLookup":
                     contextBuilder = m.invoke(contextBuilder, (Predicate<String>) (s -> true));
                     break;
                  case "allowHostAccess": {
                     if (m.getParameters()[0].getType() == Boolean.class
                             || m.getParameters()[0].getType() == boolean.class) {
                        continue;
                     }
                     Field f = hostA.getDeclaredField("ALL");
                     Method nb = hostA.getMethod("newBuilder", f.getType());
                     Object hostAccessBuilder = nb.invoke(hostA, f.get(hostA)); // org.graalvm.polyglot.HostAccess
                     Method ttm = null, b = null;
                     for (Method d : hostAccessBuilder.getClass().getMethods()) {
                        if (d.getName().equals("targetTypeMapping") && d.getParameterCount() == 4) {
                           ttm = d;
                        }
                        if (d.getName().equals("build") && d.getParameterCount() == 0) {
                           b = d;
                        }
                     }
                     // Double to
                     if (ttm != null) {
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Byte.class, null, (Function<Double, Byte>) (Double::byteValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Float.class, null, (Function<Double, Float>) (Double::floatValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Integer.class, null, (Function<Double, Integer>) (Double::intValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Long.class, null, (Function<Double, Long>) (Double::longValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, String.class, null, (Function<Double, String>) (Object::toString));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Boolean.class, null, (Function<Double, Boolean>) (n -> n != 0.0d));
                        // Float to
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, Byte.class, null, (Function<Float, Byte>) (Float::byteValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, Double.class, null, (Function<Float, Double>) (Float::doubleValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, Integer.class, null, (Function<Float, Integer>) (Float::intValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, Long.class, null, (Function<Float, Long>) (Float::longValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, String.class, null, (Function<Float, String>) (Object::toString));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, Boolean.class, null, (Function<Float, Boolean>) (n -> n != 0.0f));
                        // Integer to
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, Byte.class, null, (Function<Integer, Byte>) (Integer::byteValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, Double.class, null, (Function<Integer, Double>) (Integer::doubleValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, Float.class, null, (Function<Integer, Float>) (Integer::floatValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, Long.class, null, (Function<Integer, Long>) (Integer::longValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, String.class, null, (Function<Integer, String>) (Object::toString));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, Boolean.class, null, (Function<Integer, Boolean>) (n -> n != 0));
                        // Long to
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Byte.class, null, (Function<Long, Byte>) (Long::byteValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Long.class, Double.class, null, (Function<Long, Double>) (Long::doubleValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Long.class, Float.class, null, (Function<Long, Float>) (Long::floatValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Long.class, Integer.class, null, (Function<Long, Integer>) (Long::intValue));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Long.class, String.class, null, (Function<Long, String>) (Object::toString));
                        hostAccessBuilder = ttm.invoke(hostAccessBuilder, Long.class, Boolean.class, null, (Function<Long, Boolean>) (n -> n != 0L));
                        if (b != null) { hostAccessBuilder = b.invoke(hostAccessBuilder); }
                        // invoke to main
                        contextBuilder = m.invoke(contextBuilder, hostAccessBuilder);
                     }
                     break;
                  }
                  case "allowAllAccess": {
                     contextBuilder = m.invoke(contextBuilder, true);
                     break;
                  }
                  case "option": {
                     contextBuilder = m.invoke(contextBuilder, "js.ecmascript-version", "2022");
                     contextBuilder = m.invoke(contextBuilder, "js.nashorn-compat", "true");
                     break;
                  }
                  default:
               }
            }
            return (ScriptEngine) create.invoke(graal, null, contextBuilder);
         }
      }
      catch (Exception e) { LogWriter.error("Error:", e); }
      return null;
   }

   private void loadAgreements() {
      agreements.clear();
      LogWriter.error("Load player script agreements");
      File file = new File(CustomNpcs.Dir, "agreements.dat");
      boolean err = false;
      if (file.exists()) {
         try {
            CompoundTag compound = NbtIo.read(file);
            if (compound != null) {
               for (int i = 0; i < compound.getList("Agreements", 8).size(); i++) {
                  agreements.add(compound.getList("Agreements", 8).getString(i));
               }
            }
            else { err = true; }
         }
         catch (Exception e) {
            err = true;
            LogWriter.error("Error load agreements:", e);
         }
      }
      if (!file.exists() || err) {
         try { NbtIo.write(new CompoundTag(), file); }
         catch (Exception e) { LogWriter.error("Error default save agreements:", e); }
      }
      LogWriter.debug("Found "+agreements.size()+" agreements");
   }

   private void saveAgreements() {
      LogWriter.error("Save player script agreements");
      File file = new File(CustomNpcs.Dir, "agreements.dat");
      try {
         CompoundTag compound = new CompoundTag();
         ListTag list = new ListTag();
         for (String agreement : agreements) { list.add(StringTag.valueOf(agreement)); }
         compound.put("Agreements", list);
         NbtIo.write(compound, file);
      }
      catch (Exception e) { LogWriter.error("Error save agreements:", e); }
      LogWriter.debug("Save "+agreements.size()+" agreements");
   }

   public void setAgreement(String agreementName, boolean isAgree) {
      boolean bo;
      if (isAgree) { bo = agreements.add(agreementName); }
      else { bo = agreements.remove(agreementName); }
      if (bo) { saveAgreements(); }
   }

   public boolean notAgreement(String agreementName) { return !agreements.contains(agreementName); }

   @SuppressWarnings("unused")
   public void checkAgreements(List<String> checkList) {
      if (checkList == null) { return; }
      boolean bo = false;
      List<String> worldAgreements = new ArrayList<>(agreements);
      for (String key : worldAgreements) {
         if (key.split(";").length>2) { continue; }
         if (!checkList.contains(key)) {
            if (agreements.remove(key)) { bo = true; }
            checkList.remove(key);
         }
      }
      if (bo) { saveAgreements(); }
   }

   public void tryAddErrored(ScriptContainer container) {
      if (container != null && container.hasHandler() && !container.console.isEmpty() && !errors.contains(container)) {
         boolean found = false;
         for (ScriptContainer cont : errors) {
            if (cont.getHandler().equals(container.getHandler())) { found = true; break; }
         }
         if (!found) {
            errors.add(container);
            if (CustomNpcs.Server != null) {
               CommonUtil.NotifyOPs(Component.translatable("command.script.logs.view"), true);
            }
         }
      }
   }

   public void tryRemoveErrored(ScriptContainer container) {
      boolean found = false;
      for (ScriptContainer cont : errors) {
         if (cont.getHandler().equals(container.getHandler())) {
            found = true;
            errors.remove(cont);
            break;
         }
      }
      if (!found) { errors.remove(container); }
   }

   public List<ScriptContainer> getErrored() {
      List<ScriptContainer> list = new ArrayList<>();
      for (ScriptContainer container : errors) {
         if (container == null ||
                 !container.hasHandler() ||
                 container.console.isEmpty()) { continue; }
         list.add(container);
      }
      errors.clear();
      errors.addAll(list);
      return errors;
   }

   public void tryAdd(int type, Object obj) {
      CustomNPCsScheduler.runTack(() -> {
         if (!elements.containsKey(type)) { elements.put(type, new ArrayList<>()); }
         if (elements.get(type).contains(obj)) { return; }
         elements.get(type).add(obj);
      });
   }

   public Component getElements(int type) {
      if (!elements.containsKey(type)) { return null; }
      List<String> list = new ArrayList<>();
      List<Object> objs = new ArrayList<>(elements.get(type));
      for (Object obj : objs) {
         BlockPos pos = null;
         String dimId = "overworld";
         if (type == 0 && obj instanceof TileScripted tile) {
            pos = tile.getBlockPos();
            if (tile.getLevel() == null || tile.getLevel().getBlockEntity(pos) != tile) {
               pos = null;
               elements.get(type).remove(obj);
            }
            else { dimId = tile.getLevel().dimension().toString(); }
         }
         else if (type == 1 && obj instanceof TileScriptedDoor tile) {
            pos = tile.getBlockPos();
            if (tile.getLevel() == null || tile.getLevel().getBlockEntity(pos) != tile) {
               pos = null;
               elements.get(type).remove(obj);
            }
            else { dimId = tile.getLevel().dimension().toString(); }
         }
         else if (type == 2 && obj instanceof EntityNPCInterface npc) {
            pos = npc.blockPosition();
            if (npc.level().getEntity(npc.getId()) != npc) {
               pos = null;
               elements.get(type).remove(obj);
            }
            else { dimId = npc.level().dimension().toString(); }
         }
         if (pos != null) {
            String key = ":[DimID: "+dimId+"; X:"+pos.getX()+", Y:"+pos.getY()+", Z:"+pos.getZ()+"]";
            if (!list.contains(key)) { list.add(key); }
         }
      }
      StringBuilder positions = new StringBuilder();
      int i = 0;
      for (String pos : list) {
         positions.append(i + 1).append(pos);
         if (i < list.size() - 1) { positions.append(", "); }
         i++;
      }
      return Component.literal(positions.toString());
   }

   private static CompoundTag getNbtDefaultConstants() {
      CompoundTag nbtC = new CompoundTag();
      nbtC.putInt("value", 0);
      nbtC.putString("Lists", "Java.type(\"com.google.common.collect.Lists\")");
      nbtC.putString("System", "Java.type(\"java.lang.System\")");
      nbtC.putString("List", "Java.type(\"java.util.ArrayList\")");
      nbtC.putString("Collections", "Java.type(\"java.util.Collections\")");
      nbtC.putString("UUID", "Java.type(\"java.util.UUID\")");
      nbtC.putString("HashMap", "Java.type(\"java.util.HashMap\")");
      nbtC.putString("HashSet", "Java.type(\"java.util.HashSet\")");

      nbtC.putString("Files", "Java.type(\"java.nio.file.Files\")");
      nbtC.putString("File", "Java.type(\"java.io.File\")");
      nbtC.putString("FileOutputStream", "Java.type(\"java.io.FileOutputStream\")");
      nbtC.putString("FileInputStream", "Java.type(\"java.io.FileInputStream\")");

      nbtC.putString("String", "Java.type(\"java.lang.String\")");
      nbtC.putString("StringArray", "Java.type(\"java.lang.String[]\")");
      nbtC.putString("sData", "Java.type(\"noppes.npcs.api.NpcAPI\").Instance().getStoreddata()");
      nbtC.putString("tData", "Java.type(\"noppes.npcs.api.NpcAPI\").Instance().getTempdata()");

      nbtC.putString("TagParser", "Java.type(\"net.minecraft.nbt.TagParser\")");
      nbtC.putString("ByteTag", "Java.type(\"net.minecraft.nbt.ByteTag\")");
      nbtC.putString("IntTag", "Java.type(\"net.minecraft.nbt.IntTag\")");
      nbtC.putString("FloatTag", "Java.type(\"net.minecraft.nbt.FloatTag\")");
      nbtC.putString("DoubleTag", "Java.type(\"net.minecraft.nbt.DoubleTag\")");
      nbtC.putString("ByteArrayTag", "Java.type(\"net.minecraft.nbt.ByteArrayTag\")");
      nbtC.putString("StringTag", "Java.type(\"net.minecraft.nbt.StringTag\")");
      nbtC.putString("ListTag", "Java.type(\"net.minecraft.nbt.ListTag\")");
      nbtC.putString("CompoundTag", "Java.type(\"net.minecraft.nbt.CompoundTag\")");
      nbtC.putString("IntArrayTag", "Java.type(\"net.minecraft.nbt.IntArrayTag\")");
      nbtC.putString("NbtIo", "Java.type(\"net.minecraft.nbt.NbtIo\")");
      nbtC.putString("ParticleTypes", "Java.type(\"net.minecraft.core.particles.ParticleTypes\")");

      nbtC.putString("Serializer", "Java.type(\"net.minecraft.network.chat.Component.Serializer\")");
      nbtC.putString("Component", "Java.type(\"net.minecraft.network.chat.Component\")");
      nbtC.putString("Style", "Java.type(\"net.minecraft.network.chat.Style\")");
      nbtC.putString("ClickEvent", "Java.type(\"net.minecraft.network.chat.ClickEvent\")");
      nbtC.putString("HoverEvent", "Java.type(\"net.minecraft.network.chat.HoverEvent\")");
      nbtC.putString("ClickEventAction", "Java.type(\"net.minecraft.network.chat.ClickEvent.Action\")");
      nbtC.putString("HoverEventAction", "Java.type(\"net.minecraft.network.chat.HoverEvent.Action\")");

      nbtC.putString("BlockPos", "Java.type(\"net.minecraft.core.BlockPos\")");
      nbtC.putString("Block", "Java.type(\"net.minecraft.world.level.block.Block\")");
      nbtC.putString("Item", "Java.type(\"net.minecraft.world.item.Item\")");
      nbtC.putString("ItemStack", "Java.type(\"net.minecraft.world.item.ItemStack\")");
      nbtC.putString("ResourceLocation", "Java.type(\"net.minecraft.resources.ResourceLocation\")");
      nbtC.putString("AttributeModifier", "Java.type(\"net.minecraft.world.entity.ai.attributes.AttributeModifier\")");
      nbtC.putString("RangedAttribute", "Java.type(\"net.minecraft.world.entity.ai.attributes.RangedAttribute\")");
      nbtC.putString("Attributes", "Java.type(\"net.minecraft.world.entity.ai.attributes.Attributes\")");
      nbtC.putString("SimpleContainer", "Java.type(\"net.minecraft.world.SimpleContainer\")");
      nbtC.putString("MobEffectInstance", "Java.type(\"net.minecraft.world.effect.MobEffectInstance\")");
      nbtC.putString("ForgeRegistries", "Java.type(\"net.minecraftforge.registries.ForgeRegistries\")");
      nbtC.putString("ScriptController", "Java.type(\"noppes.npcs.controllers.ScriptController\").Instance");
      nbtC.putString("ScriptContainer", "Java.type(\"noppes.npcs.controllers.ScriptContainer\")");
      nbtC.putString("TransportController", "Java.type(\"noppes.npcs.controllers.TransportController\").instance");
      return nbtC;
   }

   private void checkExampleModules() {
      for (String fName : languages.keySet()) {
         String factoryName = Util.instance.deleteColor(fName).toLowerCase();
         String code = "";
         switch (factoryName) {
            case "ecmascript":
            case "rhino": {
               code = "var hi = \"Hello\";" + ((char) 10) +
                       "function init(ev) {" + ((char) 10) +
                       ((char) 9) + "var id = \"overworld\";" + ((char) 10) +
                       ((char) 9) + "ev.API.getIWorld(id).broadcast(hi + \" world ID:\" + id);" + ((char) 10) +
                       "}" + ((char) 10);
               break;
            }
            case "graaljs": {
               code = "var hi = \"Hello\";" + ((char) 10) +
                       "function init(ev) {" + ((char) 10) +
                       ((char) 9) + "let id = \"overworld\";" + ((char) 10) +
                       ((char) 9) + "ev.API.getIWorld(id).broadcast(hi + \" world ID: \" + id);" + ((char) 10) +
                       "}" + ((char) 10);
               break;
            }
            case "groovy": {
               code = "binding.setVariable('hi', 'Hello')" + ((char) 10) +
                       "void init(def ev) {" + ((char) 10) +
                       ((char) 9) + "def id = \"overworld\"" + ((char) 10) +
                       ((char) 9) + "ev.API.getIWorld(id).broadcast(hi + \" world ID:\" + id)" + ((char) 10) +
                       "}" + ((char) 10);
               break;
            }
            case "jruby": {
               code = "hi = \"Hello\"" + ((char) 10) +
                       "def init(ev)" + ((char) 10) +
                       ((char) 9) + "id = \"overworld\"" + ((char) 10) +
                       ((char) 9) + "ev.API.getIWorld(id).broadcast(hi + \" world ID:\" + id.to_s)" + ((char) 10) +
                       "end" + ((char) 10);
               break;
            }
            case "jython": {
               code = "hi = \"Hello\"" + ((char) 10) +
                       "def init(ev):" + ((char) 10) +
                       ((char) 9) + "id = \"overworld\"" + ((char) 10) +
                       ((char) 9) + "ev.API.getIWorld(id).broadcast(hi + \" world ID:\" + str(id))" + ((char) 10);
               break;
            }
            case "luaj": {
               code = "local hi = \"Hello\"" + ((char) 10) +
                       "function init(ev)" + ((char) 10) +
                       ((char) 9) + "local id = \"overworld\"" + ((char) 10) +
                       ((char) 9) + "ev.API:getIWorld(id):broadcast(hi .. \" world ID:\" .. tostring(id))" + ((char) 10) +
                       "end" + ((char) 10);
               break;
            }
         }
         if (code.isEmpty()) { continue; }
         String ext = Util.instance.deleteColor(languages.get(fName)).toLowerCase();
         if (ext.equals(".js")) {
            ext = switch (factoryName) {
               case "rhino" -> "_rh.js";
               case "graaljs" -> "_gr.js";
               default -> ext;
            };
         }
         File file = new File(dir, factoryName + "/example" + ext);
         if (!file.exists()) { Util.instance.saveFile(file, code); }
      }
   }

}
