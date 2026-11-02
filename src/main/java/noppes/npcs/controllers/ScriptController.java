package noppes.npcs.controllers;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.Component;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.*;
import noppes.npcs.api.wrapper.WorldWrapper;
import noppes.npcs.api.wrapper.WrapperNpcAPI;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.blocks.tiles.TileScriptedDoor;
import noppes.npcs.controllers.data.*;
import noppes.npcs.controllers.scripts.IScriptExecutor;
import noppes.npcs.controllers.scripts.Jsr223Executor;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemScripted;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketClientScripts;
import noppes.npcs.packets.client.PacketScriptConsole;
import noppes.npcs.packets.client.PacketScriptText;
import noppes.npcs.packets.client.PacketSendFileList;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.*;

public class ScriptController {

	public static boolean HasStart = false;
	public static ScriptController Instance;

	public boolean shouldSave = false;

	public long lastLoaded = 0L;
	public NBTTagCompound compound = new NBTTagCompound();
	public File dir;
	
	public final Map<String, ScriptEngineFactory> factories = new TreeMap<>();
	public final Map<String, String> languages = new TreeMap<>();
	public final Map<String, String> scripts = new TreeMap<>();

	public ForgeScriptData forgeScripts = new ForgeScriptData();
	public PlayerScriptData playerScripts = new PlayerScriptData();
	public File localDir;

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
	public NBTTagCompound constants = new NBTTagCompound();

	private static String currentAgreement;
	private final List<String> agreements = new ArrayList<>(); // net.minecraft.client.gui.screens.multiplayer.SafetyScreen
	private final List<ScriptContainer> errors = new ArrayList<>();
	private final Map<Integer,List<Object>> elements = new TreeMap<>();

	public boolean isLoad = false;

	public ScriptController() {
		CustomNpcs.debugData.start(null);
		ScriptController.Instance = this;
		if (!CustomNpcs.NashornArguments.isEmpty()) { System.setProperty("nashorn.args", CustomNpcs.NashornArguments); }
        ScriptEngineManager manager = new ScriptEngineManager();
		LogWriter.info("Script Engines Available:");
		// Rhino
		try {
			Class<?> c = Class.forName("org.mozilla.javascript.engine.RhinoScriptEngineFactory");
			ScriptEngineFactory factory = (ScriptEngineFactory) c.newInstance();
			factory.getScriptEngine();
			manager.registerEngineName("rhino", factory);
			manager.registerEngineExtension("js", factory);
			manager.registerEngineMimeType("application/rhino", factory);
			LogWriter.info("Added script Library: \"rhino\"; type: \"RhinoScriptEngineFactory\"; files index: \".js\"");
			String language = Util.instance.deleteColor(factory.getLanguageName());
			languages.put(language, ".js");
			factories.put(language.toLowerCase(), factory);
			executorProviders.put(language.toLowerCase(), Jsr223Executor::new);
		}
		catch (Exception e) { LogWriter.info("Rhino JS is missed"); }
		// Groovy
		try {
			Class<?> c = Class.forName("org.codehaus.groovy.jsr223.GroovyScriptEngineFactory");
			ScriptEngineFactory factory = (ScriptEngineFactory) c.newInstance();
			factory.getScriptEngine();
			manager.registerEngineName("groovy", factory);
			manager.registerEngineExtension("groovy", factory);
			manager.registerEngineMimeType("application/groovy", factory);
			LogWriter.info("Added script Library: \"groovy\"; type: \"GroovyScriptEngineFactory\"; files index: \".groovy\"");
			String language = Util.instance.deleteColor(factory.getLanguageName());
			languages.put(language, ".groovy");
			factories.put(language.toLowerCase(), factory);
			executorProviders.put(language.toLowerCase(), Jsr223Executor::new);
		}
		catch (Exception e) { LogWriter.info("Groovy JS is missed"); }
		// Kotlin
		try {
			Class<?> c = Class.forName("org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory");
			ScriptEngineFactory factory = (ScriptEngineFactory) c.newInstance();
			factory.getScriptEngine();
			manager.registerEngineName("kotlin", factory);
			manager.registerEngineExtension("kt", factory);
			manager.registerEngineMimeType("application/kotlin", factory);
			LogWriter.info("Added script Library: \"kotlin\"; type: \"KotlinJsr223JvmLocalScriptEngineFactory\"; files index: \".kt\"");
			String language = Util.instance.deleteColor(factory.getLanguageName());
			languages.put(language, ".kt");
			factories.put(language.toLowerCase(), factory);
			executorProviders.put(language.toLowerCase(), Jsr223Executor::new);
		}
		catch (Exception e) { LogWriter.info("Kotlin JS is missed"); }
		// Any (Lua, Python, Ruby ...)
		for (ScriptEngineFactory factory : manager.getEngineFactories()) {
			try {
				if (factory.getExtensions().isEmpty()) {
					LogWriter.debug("Library: \"" + factory.getLanguageName() + "\"; type: \"" + factory.getClass().getSimpleName() + "\" Extensions isEmpty ");
					continue;
				}
				if (!(factory.getScriptEngine() instanceof Invocable) && !factory.getLanguageName().equals("lua")) {
					LogWriter.debug("Library: \"" + factory.getLanguageName() + "\"; type: \"" + factory.getClass().getSimpleName() + "\" Engine is not Invocable or not Lua");
					continue;
				}
				String ext = "." + factory.getExtensions().get(0).toLowerCase();
				LogWriter.info("Added script Library: \"" + factory.getLanguageName() + "\"; type: \"" + factory.getClass().getSimpleName() + "\"; files index: \"" + ext + "\"");
				String language = Util.instance.deleteColor(factory.getLanguageName());
				if (language.toLowerCase().startsWith("lua")) { language = "LuaJ"; }
				else if (language.toLowerCase().startsWith("python") || language.toLowerCase().startsWith("jython")) { language = "Jython"; }
				else if (language.toLowerCase().startsWith("ruby")) { language = "JRuby"; }
				languages.put(language, ext);
				factories.put(language.toLowerCase(), factory);
				executorProviders.put(language.toLowerCase(), Jsr223Executor::new);
			} catch (Throwable t3) {
				LogWriter.error("Error Added Script Library: \"" + factory.getLanguageName() + "\": " + t3);
			}
		}
		// ECMAScript Nashorn
		try {
			LogWriter.debug("Try create Nashorn Script Engine");
			Class<?> c = ClassLoader.getSystemClassLoader().loadClass("jdk.nashorn.api.scripting.NashornScriptEngineFactory");
			ScriptEngineFactory factory = (ScriptEngineFactory) c.newInstance();
			factory.getScriptEngine();
			try {
				Class<?> require = Class.forName("com.coveo.nashorn_modules.Require");
				Method methodEnable = require.getMethod("enable");
				MethodHandle handle = MethodHandles.lookup().unreflect(methodEnable);
				handle.invoke(factory, CustomNpcs.getWorldSaveDirectory("scripts/common_js"));
			}
			catch (Throwable t) { LogWriter.info("Nashorn Require is missed:"); }
			String language = Util.instance.deleteColor(factory.getLanguageName()); // ECMAScript
			boolean isNotRegister = true;
			if (languages.containsKey(language)) {
				String ext = languages.get(language);
				ScriptEngineFactory fac = factories.get(Util.instance.deleteColor(language).toLowerCase());
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
			LogWriter.info("Added script Library: \"" + factory.getLanguageName() + "\"; type: \"" + factory.getClass().getSimpleName() + "\"; files index: \".js\"");
			languages.put(language, ".js");
			factories.put(language.toLowerCase(), factory);
			executorProviders.put(language.toLowerCase(), Jsr223Executor::new);
			manager.registerEngineName(language.toLowerCase(), factory);
			manager.registerEngineMimeType("application/" + language.toLowerCase(), factory);
			if (isNotRegister) { manager.registerEngineExtension("js", factory); }
		} catch (Exception e) { LogWriter.error("Nashorn JS is missed", e); }
		if (isClient) { loadAgreements(); }
		CustomNpcs.debugData.end(null);
	}


	public static String getLevelKey() { return currentAgreement; }

	public static void setLevelKey(String levelKey) { currentAgreement = levelKey != null ? levelKey : ""; }

	public File clientScriptsFile() {
		boolean isClient = Thread.currentThread().getName().toLowerCase().contains("client");
		if (isClient && clientDir == null) { return new File(CustomNpcs.Dir, "client_default/client_scripts.json"); }
		return new File(dir, "client_scripts.json");
	}

	public File constantScriptsFile() { return new File(dir, "constant_scripts.json"); }

	private File npcsScriptsFile() { return new File(dir, "npc_scripts.json"); }
	
	private File forgeScriptsFile() { return new File(dir, "forge_scripts.json"); }

	private File playerScriptsFile() { return new File(dir, "player_scripts.json"); }

	private File potionScriptsFile() { return new File(dir, "potion_scripts.json"); }

	private File worldDataFile() { return new File(dir, "world_data.json"); }
	
	public ScriptEngine getEngineByName(String language) {
		ScriptEngineFactory factory = factories.get(Util.instance.deleteColor(language).toLowerCase());
		if (factory == null) { return null; }
		if (factory.getClass().getSimpleName().equals("GraalJSEngineFactory")) { return getNewGraalEngine(); }
		return factory.getScriptEngine();
	}

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
							if (ttm != null) {
								// Double to
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Byte.class, null, (Function<Double, Byte>) (Double::byteValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Short.class, null, (Function<Double, Short>) (Double::shortValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Integer.class, null, (Function<Double, Integer>) (Double::intValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Long.class, null, (Function<Double, Long>) (Double::longValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Float.class, null, (Function<Double, Float>) (Double::floatValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Double.class, null, (Function<Double, Short>) (Double::shortValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, String.class, null, (Function<Double, String>) (Object::toString));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Double.class, Boolean.class, null, (Function<Double, Boolean>) (n -> n != 0.0d));
								// Float to
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, Byte.class, null, (Function<Float, Byte>) (Float::byteValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, Short.class, null, (Function<Float, Short>) (Float::shortValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, Integer.class, null, (Function<Float, Integer>) (Float::intValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, Long.class, null, (Function<Float, Long>) (Float::longValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, Double.class, null, (Function<Float, Double>) (Float::doubleValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, String.class, null, (Function<Float, String>) (Object::toString));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Float.class, Boolean.class, null, (Function<Float, Boolean>) (n -> n != 0.0f));
								// Integer to
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, Byte.class, null, (Function<Integer, Byte>) (Integer::byteValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, Short.class, null, (Function<Integer, Short>) (Integer::shortValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, Long.class, null, (Function<Integer, Long>) (Integer::longValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, Float.class, null, (Function<Integer, Float>) (Integer::floatValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, Double.class, null, (Function<Integer, Double>) (Integer::doubleValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, String.class, null, (Function<Integer, String>) (Object::toString));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Integer.class, Boolean.class, null, (Function<Integer, Boolean>) (n -> n != 0));
								// Long to
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Long.class, Byte.class, null, (Function<Long, Byte>) (Long::byteValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Long.class, Short.class, null, (Function<Long, Short>) (Long::shortValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Long.class, Integer.class, null, (Function<Long, Integer>) (Long::intValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Long.class, Float.class, null, (Function<Long, Float>) (Long::floatValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Long.class, Double.class, null, (Function<Long, Double>) (Long::doubleValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Long.class, String.class, null, (Function<Long, String>) (Object::toString));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Long.class, Boolean.class, null, (Function<Long, Boolean>) (n -> n != 0L));

								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Number.class, Byte.class, null, (Function<Number, Byte>) (Number::byteValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Number.class, Short.class, null, (Function<Number, Short>) (Number::shortValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Number.class, Integer.class, null, (Function<Number, Integer>) (Number::intValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Number.class, Long.class, null, (Function<Number, Long>) (Number::longValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Number.class, Float.class, null, (Function<Number, Float>) (Number::floatValue));
								hostAccessBuilder = ttm.invoke(hostAccessBuilder, Number.class, Double.class, null, (Function<Number, Double>) (Number::doubleValue));
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
		catch (Exception e) { LogWriter.error(e); }
		return null;
	}

	private Map<String, Long> getScripts(String language, boolean isClient) {
		Map<String, Long> map = new TreeMap<>();
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

	public void loadCategories() {
		dir = new File(CustomNpcs.getWorldSaveDirectory(), "scripts");
		localDir = CustomNpcs.getWorldSaveDirectory("scripts", true);
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

	public boolean loadConstantData() {
		constants = new NBTTagCompound();
		File file = constantScriptsFile();
		boolean isLoad = true;
		try {
			if (file.exists()) {
				constants = NBTJsonUtil.LoadFile(file);
				boolean needResave = false;
				// Main
				NBTBase tag = constants.getTag("Constants");
				if (!(tag instanceof NBTTagCompound)) {
					needResave = true;
					constants.setTag("Constants", new NBTTagCompound());
				}
				// Check OLD data:
				NBTTagCompound nbtC = new NBTTagCompound();
				NBTTagCompound cons = constants.getCompoundTag("Constants");
				Set<String> keys = new HashSet<>(cons.getKeySet());
				for (String key : keys) {
					if (!cons.hasKey(key)) { continue; }
					tag = cons.getTag(key);
					if (!(tag instanceof NBTTagCompound) || !factories.containsKey(key)) {
						nbtC.setTag(key, tag);
						cons.removeTag(key);
						needResave = true;
					}
				}
				if (needResave) {
					cons.setTag("ecmascript", nbtC);
					constants.setTag("Constants", cons);
				}
				tag = constants.getTag("Functions");
				if (!(tag instanceof NBTTagCompound)) {
					// Check OLD data:
					if (tag instanceof NBTTagList && ((NBTTagList) tag).getTagType() == 8) {
						NBTTagList list = ((NBTTagList) tag).copy();
						constants.setTag("Functions", new NBTTagCompound());
						constants.getCompoundTag("Functions").setTag("ecmascript", list);
					} else {
						constants.setTag("Functions", new NBTTagCompound());
					}
					needResave = true;
				}
				// Check contains all languages:
				for (String key : factories.keySet()) {
					tag = constants.getCompoundTag("Constants").getTag(key);
					if (!(tag instanceof NBTTagCompound)) {
						needResave = true;
						constants.getCompoundTag("Constants").setTag(key, new NBTTagCompound());
					}
					tag = constants.getCompoundTag("Functions").getTag(key);
					if (!(tag instanceof NBTTagList)) {
						needResave = true;
						constants.getCompoundTag("Functions").setTag(key, new NBTTagList());
					}
				}
				if (needResave) {
					try {
						Util.instance.saveFile(file, constants.copy());
						CommonUtil.NotifyOPs(Component.literal("Constants have been rewritten for all scripts to ")
								.withStyle(TextFormatting.GRAY)
								.append(file.getName()), false);
					}
					catch (Exception e) { LogWriter.except(e); }
				}
			}
			else {
				for (String key : factories.keySet()) {
					if (!constants.getCompoundTag("Constants").hasKey(key, 10)) {
						constants.getCompoundTag("Constants").setTag(key, new NBTTagCompound());
					}
					if (!constants.getCompoundTag("Functions").hasKey(key, 10)) {
						constants.getCompoundTag("Functions").setTag(key, new NBTTagCompound());
					}
				}
				NBTTagCompound nbtC = getNbtDefaultConstants();
				NBTTagList list = new NBTTagList();
				list.appendTag(new NBTTagString("function getField(key,object) { try { var f = dump(object).getField(key); if (f) { return f.getValue(); } } catch (error) { log('Error: \"'+key+'\" is not a Field or not found in \"'+object.getClass().getName()+'\"');} return null; }"));
				list.appendTag(new NBTTagString("function setField(value,object,key) { try { var f = dump(object).getField(key); if (f) { return f.setValue(value); } } catch (error) { log('Error: \"'+key+'\" is not a Field or not found, or not type mismatch in \"'+object.getClass().getName()+'\". Error: ' + error); } return false; }"));
				list.appendTag(new NBTTagString("function invoke(value,object,key) { try { var m = dump(object).getMethod(key); if (m) { var jo = Java.type('java.lang.Object[]'); if (value!=jo) { try { if (value.length>=0) { var v = new jo(value.length); for (var i=0; i<value.length; i++) { v[i] = value[i]; } return m.invoke(v); } } catch (err) { } var v = new jo(1); v[0] = value; return m.invoke(v); } else { return m.invoke(value); } } } catch (error) { log('Error: \"'+key+'\" is not a Method or not found, or not type mismatch in \"'+object.getClass().getName()+'\"'); } return null; }"));
				list.appendTag(new NBTTagString("var HashMap = Java.type('java.util.HashMap'); var EMPTY_FUNCTION = new Function('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z', 'return;'); function getFunction(name) { var error; var key = 'custom_functions'; try { var fhm = api.getTempdata().get(key); if (fhm instanceof HashMap && fhm.containsKey(name)) { return fhm.get(name); } var dir = api.getLWorldDir().toPath().resolve('scripts').resolve('functions'); var file = dir.resolve('example.json'); if (!file.toFile().exists()) { api.getMethods().saveFile(file.toFile(), 'args=agr0, agr1\\nbody=agr0 += 4.2;\\nreturn agr0 * agr1;'); } file = dir.resolve(name+'.json'); if (dir.toFile().exists()) { if (file.toFile().exists()) { var context = api.getMethods().loadFile(file.toFile()); if (context.contains('args=') && context.contains('body=')) { var i = context.indexOf('args='); var args = context.substring(i + 5, context.indexOf('\\n', i)).replace('\t', '').replace(' ', '').split(','); i = context.indexOf('body='); var body = context.substring(i + 5); var func = new Function(args, body); if (!(fhm instanceof HashMap)) { fhm = new HashMap(); } fhm.put(name, func); api.getTempdata().put(key, fhm); return func; } } } } catch (e) { error = e; } if (error) { log(error); } else { log('Error: Custom function \"' + name + '\" - not found'); } return EMPTY_FUNCTION; }"));
				constants.setTag("Constants", new NBTTagCompound());
				constants.setTag("Functions", new NBTTagCompound());
				constants.getCompoundTag("Constants").setTag("ecmascript", nbtC);
				constants.getCompoundTag("Functions").setTag("ecmascript", list);
				try {
					Util.instance.saveFile(file, constants.copy());
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

	private static NBTTagCompound getNbtDefaultConstants() {
		NBTTagCompound nbtC = new NBTTagCompound();
		nbtC.setInteger("value", 0);
		nbtC.setString("Lists", "Java.type(\"com.google.common.collect.Lists\")");
		nbtC.setString("List", "Java.type(\"java.util.ArrayList\")");
		nbtC.setString("Collections", "Java.type(\"java.util.Collections\")");
		nbtC.setString("UUID", "Java.type(\"java.util.UUID\")");
		nbtC.setString("HashMap", "Java.type(\"java.util.HashMap\")");
		nbtC.setString("HashSet", "Java.type(\"java.util.HashSet\")");

		nbtC.setString("Files", "Java.type(\"java.nio.file.Files\")");
		nbtC.setString("File", "Java.type(\"java.io.File\")");
		nbtC.setString("FileOutputStream", "Java.type(\"java.io.FileOutputStream\")");
		nbtC.setString("FileInputStream", "Java.type(\"java.io.FileInputStream\")");

		nbtC.setString("String", "Java.type(\"java.lang.String\")");
		nbtC.setString("StringArray", "Java.type(\"java.lang.String[]\")");
		nbtC.setString("sData", "Java.type(\"noppes.npcs.api.NpcAPI\").Instance().getStoreddata()");
		nbtC.setString("tData", "Java.type(\"noppes.npcs.api.NpcAPI\").Instance().getTempdata()");

		nbtC.setString("JsonToNBT", "Java.type(\"net.minecraft.nbt.JsonToNBT\")");
		nbtC.setString("NBTTagByte", "Java.type(\"net.minecraft.nbt.NBTTagByte\")");
		nbtC.setString("NBTTagInt", "Java.type(\"net.minecraft.nbt.NBTTagInt\")");
		nbtC.setString("NBTTagFloat", "Java.type(\"net.minecraft.nbt.NBTTagFloat\")");
		nbtC.setString("NBTTagDouble", "Java.type(\"net.minecraft.nbt.NBTTagDouble\")");
		nbtC.setString("NBTTagByteArray", "Java.type(\"net.minecraft.nbt.NBTTagByteArray\")");
		nbtC.setString("NBTTagString", "Java.type(\"net.minecraft.nbt.NBTTagString\")");
		nbtC.setString("NBTTagList", "Java.type(\"net.minecraft.nbt.NBTTagList\")");
		nbtC.setString("NBTTagCompound", "Java.type(\"net.minecraft.nbt.NBTTagCompound\")");
		nbtC.setString("NBTTagIntArray", "Java.type(\"net.minecraft.nbt.NBTTagIntArray\")");
		nbtC.setString("CompressedStreamTools", "Java.type(\"net.minecraft.nbt.CompressedStreamTools\")");
		nbtC.setString("EnumParticleTypes", "Java.type(\"net.minecraft.util.EnumParticleTypes\")");

		nbtC.setString("Serializer", "Java.type(\"net.minecraft.util.text.ITextComponent.Serializer\")");
		nbtC.setString("TextComponentTranslation", "Java.type(\"net.minecraft.util.text.TextComponentTranslation\")");
		nbtC.setString("TextComponentString", "Java.type(\"net.minecraft.util.text.TextComponentString\")");
		nbtC.setString("TextComponentSelector", "Java.type(\"net.minecraft.util.text.TextComponentSelector\")");
		nbtC.setString("Style", "Java.type(\"net.minecraft.util.text.Style\")");
		nbtC.setString("ClickEvent", "Java.type(\"net.minecraft.util.text.event.ClickEvent\")");
		nbtC.setString("HoverEvent", "Java.type(\"net.minecraft.util.text.event.HoverEvent\")");
		nbtC.setString("ClickEventAction", "Java.type(\"net.minecraft.util.text.event.ClickEvent.Action\")");
		nbtC.setString("HoverEventAction", "Java.type(\"net.minecraft.util.text.event.HoverEvent.Action\")");

		nbtC.setString("BlockPos", "Java.type(\"net.minecraft.util.math.BlockPos\")");
		nbtC.setString("Block", "Java.type(\"net.minecraft.block.Block\")");
		nbtC.setString("Item", "Java.type(\"net.minecraft.item.Item\")");
		nbtC.setString("ItemStack", "Java.type(\"net.minecraft.item.ItemStack\")");
		nbtC.setString("ResourceLocation", "Java.type(\"net.minecraft.util.ResourceLocation\")");
		nbtC.setString("AttributeModifier", "Java.type(\"net.minecraft.entity.ai.attributes.AttributeModifier\")");
		nbtC.setString("RangedAttribute", "Java.type(\"net.minecraft.entity.ai.attributes.RangedAttribute\")");
		nbtC.setString("SharedMonsterAttributes", "Java.type(\"net.minecraft.entity.SharedMonsterAttributes\")");
		nbtC.setString("InventoryBasic", "Java.type(\"net.minecraft.inventory.InventoryBasic\")");
		nbtC.setString("PotionEffect", "Java.type(\"net.minecraft.potion.PotionEffect\")");
		nbtC.setString("ForgeRegistries", "Java.type(\"net.minecraftforge.fml.common.registry.ForgeRegistries\")");
		nbtC.setString("ScriptController", "Java.type(\"noppes.npcs.controllers.ScriptController\").Instance");
		nbtC.setString("ScriptContainer", "Java.type(\"noppes.npcs.controllers.ScriptContainer\")");
		nbtC.setString("TransportController", "Java.type(\"noppes.npcs.controllers.TransportController\").instance");
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
							((char) 9) + "var id = 0;" + ((char) 10) +
							((char) 9) + "ev.API.getIWorld(id).broadcast(hi  + \" world ID:\" + id);" + ((char) 10) +
							"}" + ((char) 10);
					break;
				}
				case "graaljs": {
					code = "var hi = \"Hello\";" + ((char) 10) +
							"function init(ev) {" + ((char) 10) +
							((char) 9) + "let id = 0;" + ((char) 10) +
							((char) 9) + "ev.API.getIWorld(\"overworld\").broadcast(hi + \" world ID: \" + id);" + ((char) 10) +
							"}" + ((char) 10);
					break;
				}
				case "groovy": {
					code = "binding.setVariable('hi', 'Hello')" + ((char) 10) +
							"void init(def ev) {" + ((char) 10) +
							((char) 9) + "def id = 0" + ((char) 10) +
							((char) 9) + "ev.API.getIWorld(id).broadcast(hi + \" world ID:\" + id)" + ((char) 10) +
							"}" + ((char) 10);
					break;
				}
                case "jruby": {
					code = "hi = \"Hello\"" + ((char) 10) +
							"def init(ev)" + ((char) 10) +
							((char) 9) + "id = 0" + ((char) 10) +
							((char) 9) + "ev.API.getIWorld(id).broadcast(hi + \" world ID:\" + id.to_s)" + ((char) 10) +
							"end" + ((char) 10);
					break;
				}
				case "jython": {
					code = "hi = \"Hello\"" + ((char) 10) +
							"def init(ev):" + ((char) 10) +
							((char) 9) + "id = 0" + ((char) 10) +
							((char) 9) + "ev.API.getIWorld(id).broadcast(hi + \" world ID:\" + str(id))" + ((char) 10);
					break;
				}
				case "luaj": {
					code = "local hi = \"Hello\"" + ((char) 10) +
							"function init(ev)" + ((char) 10) +
							((char) 9) + "local id = 0" + ((char) 10) +
							((char) 9) + "ev.API:getIWorld(id):broadcast(hi .. \" world ID:\" .. tostring(id))" + ((char) 10) +
							"end" + ((char) 10);
					break;
				}
			}
			if (code.isEmpty()) { continue; }
			String ext = Util.instance.deleteColor(languages.get(fName)).toLowerCase();
			if (ext.equals(".js")) {
				switch (factoryName) {
					case "rhino": ext = "_rh.js"; break;
					case "graaljs": ext = "_gr.js"; break;
				}
			}
			File file = new File(dir, factoryName + "/example" + ext);
			if (!file.exists()) { Util.instance.saveFile(file, code); }
		}
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

	public void loadItemTextures() {
		ItemScripted.Resources.clear();
		File file = new File(dir, "item_models.dat");
		if (file.exists()) {
			try {
				NBTTagCompound compound = NBTJsonUtil.LoadFile(file);
				for (NBTBase nbt : compound.getTagList("Models", 10)) {
					ItemScripted.Resources.put(((NBTTagCompound) nbt).getInteger("meta"), ((NBTTagCompound) nbt).getString("model"));
				}
				CustomNpcs.proxy.reloadItemTextures();
			}
			catch (Exception e) { LogWriter.error("File: \""+file+"\" error:", e); }
		}
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

	public boolean loadStoredData() {
		compound = new NBTTagCompound();
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

	public void loadDir(File dir, String name, String ext, boolean encrypt, boolean isClient) {
		if (ext == null) { ext = ".js"; }
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
	
	public NBTTagList nbtLanguages(boolean isClient) {
		NBTTagList list = new NBTTagList();
		for (String language : languages.keySet()) {
			String ext = languages.get(Util.instance.deleteColor(language));
			NBTTagCompound compound = new NBTTagCompound();
			NBTTagList scripts = new NBTTagList();
			Map<String, Long> map = getScripts(language, isClient);
			long[] cs = new long[map.size()];
			int i = 0;
			for (String script : map.keySet()) {
				scripts.appendTag(new NBTTagString(script));
				cs[i++] = map.get(script) * (!script.endsWith(ext) ? -1 : 1);
			}
			compound.setTag("Scripts", scripts);
			compound.setString("Language", language);
			compound.setString("FileSfx", ext);
			compound.setTag("sizes", new NBTTagLongArray(cs));
			list.appendTag(compound);
		}
		return list;
	}

	public void saveItemTextures() {
		File file = new File(dir, "item_models.dat");
		if (!file.exists()) {
			try {
				if (!file.createNewFile()) { return; }
			}
			catch (Exception e) { LogWriter.error(e); }
		}
		if (!file.exists()) {
			return;
		}
		NBTTagCompound compound = new NBTTagCompound();
		NBTTagList list = new NBTTagList();
		for (int meta : ItemScripted.Resources.keySet()) {
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setInteger("meta", meta);
			nbt.setString("model", ItemScripted.Resources.get(meta));
			list.appendTag(nbt);
		}
		compound.setTag("Models", list);
		Util.instance.saveFile(file, compound);
	}

	@SubscribeEvent
	public void saveWorld(WorldEvent.Save event) {
		if (!shouldSave || event.getWorld().isRemote || event.getWorld() != Objects.requireNonNull(event.getWorld().getMinecraftServer()).worlds[0]) { return; }
		CustomNpcs.debugData.start("Mod");
		try { NBTJsonUtil.SaveFile(worldDataFile(), compound.copy()); }
		catch (Exception e) { LogWriter.except(e); }
		try { NBTJsonUtil.SaveFile(constantScriptsFile(), constants.copy()); }
		catch (Exception e) { LogWriter.except(e); }
		shouldSave = false;
		CustomNpcs.debugData.end("Mod");
	}

	public void sendClientTo(EntityPlayerMP player) {
		if (!isLoad) { loadCategories(); }
		NBTTagCompound compound = new NBTTagCompound();
		clientScripts.save(compound);
		compound.setTag("Languages", ScriptController.Instance.nbtLanguages(true));
		compound.setString("DirPath", ScriptController.Instance.dir.getAbsolutePath());
		// collect and clear scripts and consoles
		Map<Integer, String> mapScripts = new TreeMap<>();
		Map<Integer, Map<Long, String>> mapConsoles = new TreeMap<>();
		NBTTagList scripts = compound.getTagList("Scripts", 10);
		for (int i = 0; i < scripts.tagCount(); i++) {
			mapScripts.put(i, scripts.getCompoundTagAt(i).getString("Script"));
			scripts.getCompoundTagAt(i).setString("Script", "");
			NBTTagList consoles = scripts.getCompoundTagAt(i).getTagList("Console", 10);
			for (int j = 0; j < consoles.tagCount(); j++) {
				if (!mapConsoles.containsKey(i)) { mapConsoles.put(i, new LinkedHashMap<>()); }
				mapConsoles.get(i).put(consoles.getCompoundTagAt(j).getLong("Long"), consoles.getCompoundTagAt(j).getString("String"));
				consoles.getCompoundTagAt(j).setString("String", "");
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

		NBTTagList list = new NBTTagList();
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
			list.appendTag(file.getTitle());
		}
		NBTTagCompound fileList = new NBTTagCompound();
		fileList.setTag("FileList", list);
		Packets.send(player, new PacketSendFileList(fileList));
	}

	public void saveNPCsScripts() {
		NBTTagCompound compound = new NBTTagCompound();
		npcsScripts.save(compound);
		npcsScripts.init();
		try { NBTJsonUtil.SaveFile(npcsScriptsFile(), compound.copy()); } catch (Exception e) { LogWriter.error(e); }
	}
	
	public void saveForgeScripts() {
		NBTTagCompound compound = new NBTTagCompound();
		forgeScripts.save(compound);
		forgeScripts.init();
		try { Util.instance.saveFile(forgeScriptsFile(), compound); } catch (Exception e) { LogWriter.error(e); }
	}

	public void savePlayerScripts() {
		NBTTagCompound compound = new NBTTagCompound();
		playerScripts.save(compound);
		playerScripts.init();
		try { Util.instance.saveFile(playerScriptsFile(), compound); } catch (Exception e) { LogWriter.error(e); }
	}

	public void saveClientScripts() {
		NBTTagCompound compound = new NBTTagCompound();
		clientScripts.save(compound);
		clientScripts.init();
		try { Util.instance.saveFile(clientScriptsFile(), compound); } catch (Exception e) { LogWriter.error(e); }
	}

	public void savePotionScripts() {
		NBTTagCompound compound = new NBTTagCompound();
		potionScripts.save(compound);
		potionScripts.init();
		try { Util.instance.saveFile(potionScriptsFile(), compound); } catch (Exception e) { LogWriter.error(e); }
	}

	// New from Unofficial (BetaZavr)
	public static void reloadConstants() { ScriptContainer.DATA.remove("dump"); }

	private void loadAgreements() {
		agreements.clear();
		LogWriter.error("Load player script agreements");
		File file = new File(CustomNpcs.Dir, "agreements.dat");
		boolean err = false;
		if (file.exists()) {
			try {
				NBTTagCompound compound = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
				for (int i = 0; i < compound.getTagList("Agreements", 8).tagCount(); i++) {
					agreements.add(compound.getTagList("Agreements", 8).getStringTagAt(i));
				}
			}
			catch (Exception e) {
				err = true;
				LogWriter.error("Error load agreements:", e);
			}
		}
		if (!file.exists() || err) {
			try { CompressedStreamTools.writeCompressed(new NBTTagCompound(), Files.newOutputStream(file.toPath())); }
			catch (Exception e) { LogWriter.error("Error default save agreements:", e); }
		}
		LogWriter.debug("Found "+agreements.size()+" agreements");
	}

	private void saveAgreements() {
		LogWriter.error("Save player script agreements");
		File file = new File(CustomNpcs.Dir, "agreements.dat");
		try {
			NBTTagCompound compound = new NBTTagCompound();
			NBTTagList list = new NBTTagList();
			for (String agreement : agreements) {
				list.appendTag(new NBTTagString(agreement));
			}
			compound.setTag("Agreements", list);
			CompressedStreamTools.writeCompressed(compound, Files.newOutputStream(file.toPath()));
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
			int dimId = 0;
			if (type == 0 && obj instanceof TileScripted) {
				TileScripted tile = ((TileScripted) obj);
				pos = tile.getPos();
                if (tile.getWorld().getTileEntity(pos) != tile) {
					pos = null;
					elements.get(type).remove(obj);
				}
				else { dimId = tile.getWorld().provider.getDimension(); }
			}
			else if (type == 1 && obj instanceof TileScriptedDoor) {
				TileScriptedDoor tile = ((TileScriptedDoor) obj);
				pos = tile.getPos();
				if (tile.getWorld().getTileEntity(pos) != tile) {
					pos = null;
					elements.get(type).remove(obj);
				}
				else { dimId = tile.getWorld().provider.getDimension(); }
			}
			else if (type == 2 && obj instanceof EntityNPCInterface) {
				EntityNPCInterface npc = (EntityNPCInterface) obj;
				pos = npc.getPosition();
				if (npc.world == null || npc.world.getEntityByID(npc.getEntityId()) != npc) {
					pos = null;
					elements.get(type).remove(obj);
				}
				else { dimId = npc.world.provider.getDimension(); }
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

}
