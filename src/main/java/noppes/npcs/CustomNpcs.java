package noppes.npcs;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

import net.minecraft.block.Block;
import net.minecraft.block.BlockIce;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockVine;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.SPacketScoreboardObjective;
import net.minecraft.network.play.server.SPacketUpdateScore;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import nikedemos.markovnames.generators.MarkovAncientGreek;
import nikedemos.markovnames.generators.MarkovAztec;
import nikedemos.markovnames.generators.MarkovCustomNPCsClassic;
import nikedemos.markovnames.generators.MarkovGenerator;
import nikedemos.markovnames.generators.MarkovJapanese;
import nikedemos.markovnames.generators.MarkovOldNorse;
import nikedemos.markovnames.generators.MarkovRoman;
import nikedemos.markovnames.generators.MarkovSaami;
import nikedemos.markovnames.generators.MarkovSlavic;
import nikedemos.markovnames.generators.MarkovSpanish;
import nikedemos.markovnames.generators.MarkovWelsh;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.CustomParticleEvent;
import noppes.npcs.api.event.CustomPotionEvent;
import noppes.npcs.api.handler.capability.*;
import noppes.npcs.api.wrapper.*;
import noppes.npcs.capability.*;
import noppes.npcs.command.CmdMoney;
import noppes.npcs.command.CmdSchematics;
import noppes.npcs.command.CommandNoppes;
import noppes.npcs.config.ConfigLoader;
import noppes.npcs.config.ConfigProp;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.dimensions.CustomWorldProvider;
import noppes.npcs.dimensions.DimensionHandler;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemScripted;
import noppes.npcs.mixin.entity.ai.attributes.IRangedAttributeMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;
import noppes.npcs.util.DataDebug;

import javax.annotation.Nullable;

@Mod(modid = CustomNpcs.MODID,
		name = CustomNpcs.MODNAME,
		version = "2.4",
		acceptedMinecraftVersions = "1.12, 1.12.1, 1.12.2",
		guiFactory = "noppes.npcs.config.CustomNpcsGuiFactory")
public class CustomNpcs {
	@ConfigProp(info = "Number of chunk loading npcs that can be active at the same time", def = "20")
	public static int ChuckLoaders = 20;
	@ConfigProp(info = "Default interact line. Leave empty to not have one", def = "Hello @p")
	public static String DefaultInteractLine = "Hello @p";
	@ConfigProp(info = "If you are running sponge and you want to disable the permissions set this to true", def = "false")
	public static boolean DisablePermissions = false;
	@ConfigProp(info = "Enable Chat Bubbles from npcs", def = "true")
	public static boolean EnableChatBubbles = true;
	@ConfigProp(info = "For some it works, for others it doesnt, so Im disabling by default", def = "false")
	public static boolean EnableInvisibleNpcs = false;
	@ConfigProp(info = "Whether scripting is enabled or not", def = "true")
	public static boolean EnableScripting = true;
	@ConfigProp(info = "Enables CustomNpcs startup update message", def = "true")
	public static boolean EnableUpdateChecker = true;
	@ConfigProp(info = "Font size for custom fonts (doesn't work with minecraft fonts)", def = "18", min = "6", max = "36")
	public static int FontSize = 18;
	@ConfigProp(info = "Font for writing scripts. When set to \"Minecraft\" it will use minecraft fonts, when \"Default\" it will use \"JetBrainsMono\". Can only use fonts installed on <you game dir>/customnpcs/assets/customnpcs/fonts/*.ttf", def = "Default")
	public static String FontType = "Default";
	@ConfigProp(info = "Type 0=Normal; 1=Solid; 2=Not show", def = "1", min = "0", max = "1")
	public static int HeadWearType = 1;
	@ConfigProp(info = "Enables Ice Melting", def = "true")
	public static boolean IceMeltsEnabled = true;
	@ConfigProp(info = "Enables leaves decay", def = "true")
	public static boolean LeavesDecayEnabled = true;
	@ConfigProp(info = "Arguments given to the Nashorn scripting library", def = "-strict")
	public static String NashornArguments = "-strict";
	@ConfigProp(info = "Navigation search range for NPCs. Not recommended to increase if you have a slow pc or on a server", def = "32", min = "16", max = "64")
	public static int NpcNavRange = 32;
	@ConfigProp(info = "Set to true if you want the dialog command option to be able to use op commands like tp etc", def = "false")
	public static boolean NpcUseOpCommands = false;
	@ConfigProp(info = "Only ops can create and edit npcs", def = "false")
	public static boolean OpsOnly = false;
	@ConfigProp(info = "Normal players can use soulstone on animals", def = "true")
	public static boolean SoulStoneAnimals = true;
	@ConfigProp(info = "Normal players can use soulstone on all npcs", def = "false")
	public static boolean SoulStoneNPCs = false;
	@ConfigProp(info = "Show Debug", def = "false")
	public static boolean VerboseDebug = false;
	@ConfigProp(info = "Enables Vine Growth", def = "true")
	public static boolean VineGrowthEnabled = true;
	@ConfigProp(info = "Displaying mod toolbox hitboxes in the world", def = "true")
	public static boolean ShowHitboxBlockTools = true;
	@ConfigProp(info = "Cancel the creation of variables in each Forge event (saves FPS)", def = "false")
	public static boolean SceneButtonsEnabled = true;
	@ConfigProp(info = "NPC speech can trigger a chat event", def = "false")
	public static boolean NpcSpeachTriggersChatEvent = false;
	@ConfigProp(info = "Show faction, quest and compass tabs in player inventory", def = "true")
	public static boolean InventoryGuiEnabled = true;

	// New Unofficial (Goodbird)
	@ConfigProp(info = "Limit too how many npcs can be in one chunk for natural spawning", def = "4", min = "0", max = "16")
	public static int NpcNaturalSpawningChunkLimit = 4;
	@ConfigProp(info = "If set to true only opped people can use the /noppes command", def = "false")
	public static boolean NoppesCommandOpOnly = false;
	@ConfigProp(info = "When set to \"Minecraft\" it will use minecraft fonts, when \"Default\" it will use \"JetBrainsMono\". Can only use fonts installed on <you game dir>/customnpcs/assets/customnpcs/fonts/*.ttf", def = "JetBrainsMono")
	public static String LogFontType = "JetBrainsMono";
	@ConfigProp(info = "Font size for custom fonts (doesn't work with minecraft fonts)", def = "14", min = "6", max = "18")
	public static int LogFontSize = 14;
	@ConfigProp(info = "Type 0 = standard CNPC (client side despawn), type 1 = old CNPC (disable renderer only), type 2 = modern (disable rendering, hitboxes and interactions)", def = "2", min = "0", max = "2")
	public static int InvisibilityAlgorithm = 2;
	@ConfigProp(info = "Enable it if you want all your global data (quests and dialogs) to be saved externally (not per world, but per .minecraft folder)", def = "false")
	public static boolean EnableExternalSaving = false;
	@ConfigProp(info = "The lifetime for the projectiles, after which they get despawned (used for removing stuck projectiles)", def = "1200", min = "10", max = "90000")
	public static int ProjectileLifespan = 1200;

	// New from Unofficial (BetaZavr)
	@ConfigProp(info = "Are scripts enabled for Forge events or not", def = "true")
	public static boolean EnableForgeScripting = true;
	@ConfigProp(info = "Currency symbol displayed in stores (unicode)", def = "20AC")
	public static String CharCurrencies = "20AC";
	public static String displayCurrencies = "" + ((char) 8364); // 20AC
	@ConfigProp(info = "Donation currency symbol (unicode)", def = "20B1")
	public static String CharDonation = "20B1";
	public static String displayDonation = "" + ((char) 8383); // 20BF
	@ConfigProp(info = "Minimum and maximum melle and range Damage of NPCs for 1 and Maximum level, respectively (rarity Boss)", def = "8,52,6,26", min = "0,0,0,0")
	public static int[] DamageBoss = new int[] { 8, 52, 6, 26 };
	@ConfigProp(info = "Minimum and maximum melle and range Damage of NPCs for 1 and Maximum level, respectively (rarity Elite)", def = "6,32,3,16", min = "0,0,0,0")
	public static int[] DamageElite = new int[] { 6, 32, 3, 16 };
	@ConfigProp(info = "Minimum and maximum melle and range Damage of NPCs for 1 and Maximum level, respectively (rarity Normal)", def = "4,22,2,11", min = "0,0,0,0")
	public static int[] DamageNormal = new int[] { 4, 22, 2, 11 };
	@ConfigProp(info = "Enable chat bubbles from players", def = "true")
	public static boolean EnablePlayerChatBubbles = true;
	@ConfigProp(info = "Script password. Necessary for decrypting scripts", def = "00bb7f7647ca389196fe03177d2fac78")
	public static String ScriptPassword = UUID.randomUUID().toString().replace("-", "");
	@ConfigProp(info = "Maximum and minimum amount of experience dropped from the NPC for the minimum and maximum level (Elite x1.75; Boss x4.75)", def = "2,3,100,115", min = "0,0,0,0")
	public static int[] Experience = new int[] { 2, 3, 100, 115 };
	@ConfigProp(info = "Main text color of elements in GUI modification", def = "FFFFFF", type = "client")
	public static Color MainColor = new Color(0xFFFFFF);
	@ConfigProp(info = "Text color of labels in GUI mod", def = "505050", type = "client")
	public static Color LableColor = new Color(0x505050);
	@ConfigProp(info = "Text color of labels in GUI mod", def = "FCFCFC", type = "client")
	public static Color ButtonColor = new Color(0xFCFCFC);
	@ConfigProp(info = "Text color for inactive elements in modification GUI", def = "A0A0A0", type = "client")
	public static Color NotEnableColor = new Color(0xA0A0A0);
	@ConfigProp(info = "Text color of elements in modification GUI when the element is held down by the mouse cursor", def = "FFFFA0", type = "client")
	public static Color HoverColor = new Color(0xFFFFA0);
	@ConfigProp(info = "Text Color for GUI Quest Log", def = "404060", type = "client")
	public static Color QuestLogColor = new Color(0x404060);
	@ConfigProp(info = "Color of message bubbles above NPC head [text, frame, base]", def = "000000,000000,FFFFFF", type = "client")
	public static Color[] ChatNpcColors = new Color[] {
			new Color(0x000000),
			new Color(0x000000),
			new Color(0xFFFFFF) };
	@ConfigProp(info = "Color of message bubbles above Player head [text, frame, base]", def = "000000,2C4C00,E0FFB0", type = "client")
	public static Color[] ChatPlayerColors = new Color[] {
			new Color(0x000000),
			new Color(0x2C4C00),
			new Color(0xE0FFB0) };
	@ConfigProp(info = "Minimum and maximum health of NPCs for 1 and Maximum level, respectively (rarity Boss)", def = "250,20000", min = "1,1")
	public static int[] HealthBoss = new int[] { 250, 20000 };
	@ConfigProp(info = "Minimum and maximum health of NPCs for 1 and Maximum level, respectively (rarity Elite)", def = "60,1200", min = "1,1")
	public static int[] HealthElite = new int[] { 60, 1200 };
	@ConfigProp(info = "Minimum and maximum health of NPCs for 1 and Maximum level, respectively (rarity Normal)", def = "20,500", min = "1,1")
	public static int[] HealthNormal = new int[] { 20, 500 };
	@ConfigProp(info = "Maximum NPC level", def = "45", min = "1", max = "10000")
	public static int MaxLv = 45;
	@ConfigProp(info = "Resizes the model for rarity. (Normal, Elite, Boss)", def = "5,6,7", min = "1,2,3")
	public static int[] ModelRaritySize = new int[] { 5, 6, 7 };
	@ConfigProp(info = "Whether to recalculate Stats when setting Level and Rarity", def = "true")
	public static boolean RecalculateLR = true;
	@ConfigProp(info = "Parameters for calculating NPC Resistances (0=-100%, 1=0%, 2=100% [melee, arrow, explosion, knockback] rarity Boss)", def = "110,125,175,195", min = "0,0,0,0", max = "200,200,200,200")
	public static int[] ResistanceBoss = new int[] { 110, 125, 175, 195 };
	@ConfigProp(info = "Parameters for calculating NPC Resistances (0=-100%, 1=0%, 2=100% [melee, arrow, explosion, knockback] rarity Elite)", def = "105,110,130,150", min = "0,0,0,0", max = "200,200,200,200")
	public static int[] ResistanceElite = new int[] { 105, 110, 130, 150 };
	@ConfigProp(info = "Parameters for calculating NPC Resistances (0=-100%, 1=0%, 2=100% [melee, arrow, explosion, knockback] rarity Normal)", def = "100,100,100,100", min = "0,0,0,0", max = "200,200,200,200")
	public static int[] ResistanceNormal = new int[] { 100, 100, 100, 110 };
	@ConfigProp(info = "Whether to display Level and Rarity. If 1 then it will be installed on all clients", def = "true")
	public static boolean ShowLR = true;
	@ConfigProp(info = "Display player balance in inventory", def = "true")
	public static boolean ShowMoney = true;
	@ConfigProp(info = "Display player donat balance in inventory", def = "false")
	public static boolean ShowDonat = false;
	@ConfigProp(info = "Type display player Quest Compass", def = "0", min = "0", max = "4")
	public static int TypeShowQuestCompass = 0;
	@ConfigProp(info = "Display hitbox of nearby NPCs when holding mod tools", def = "true")
	public static boolean ShowHitboxWhenHoldTools = true;
	@ConfigProp(info = "Show description when hovering cursor on over GUI elements", def = "true", type = "client")
	public static boolean ShowDescriptions = true;
	@ConfigProp(info = "Maximum blocks to install per second with the Builder item", def = "10000", min = "100", max = "100000000")
	public static int MaxBuilderBlocks = 10000;
	@ConfigProp(info = "Maximum number of items in one Drop group", def = "32", min = "1", max = "64")
	public static int MaxItemInDropsNPC = 32;
	@ConfigProp(info = "Cancel the creation of variables in each Forge event (saves FPS)", def = "false")
	public static boolean SimplifiedForgeEvents = false;
	@ConfigProp(info = "Summon a new NPC with random custom eyes", def = "true")
	public static boolean EnableDefaultEyes = true;
	@ConfigProp(info = "Time in real days when the letter will be deleted from the player (-1 = never, at least 1 day, max 60)", def = "30")
	public static int MailTimeWhenLettersWillBeDeleted = 30;
	@ConfigProp(info = "Time in seconds when a player can receive a letter [min not less than 10, max not more than 3600]", def = "120,300", min = "10,10", max = "3600,3600")
	public static int[] MailTimeWhenLettersWillBeReceived = new int[] { 120, 300 };
	@ConfigProp(info = "Cost for sending a letter in game currency. [base send, one page, one stack of item, percentage of currency, redemption percentage]", def = "10,5,30,2,4", min = "0,0,0,0,0")
	public static int[] MailCostSendingLetter = new int[] { 10, 5, 30, 2, 4 };
	@ConfigProp(info = "Can players send themselves letters?", def = "false")
	public static boolean MailSendToYourself = false;
	@ConfigProp(info = "Position on the screen of the icon indicating the presence of new messages (-1 = do not show, then from 0 to 3)", def = "1", min = "-1", max = "3")
	public static int MailWindow = 1;
	@ConfigProp(info = "Maximum number of tabs for scripts (from 1 to 40) Recommended: 5", def = "40", min = "1", max = "40")
	public static int ScriptMaxTabs = 40;
	@ConfigProp(info = "The speed for dialogs that show individual letters. (number per second from 10 to 100)", def = "30", min = "10", max = "100", type = "client")
	public static int DialogShowFitsSpeed = 30;
	@ConfigProp(info = "When a player's dimension changes, their home position will change to portal position", def = "true")
	public static boolean SetHomeDimension = true;
	@ConfigProp(info = "Displaying joints on an NPC model", def = "true", type = "client")
	public static boolean ShowJoints = true;
	@ConfigProp(info = "Display custom NPC animations. Disable it if you have a weak computer", def = "true", type = "client")
	public static boolean ShowCustomAnimation = true;
	@ConfigProp(info = "Send a message to the player's chat about a completed transaction", def = "false", type = "client")
	public static boolean SendMarcetInfo = false;
	@ConfigProp(info = "Percentage of knockback power of all entities in the game when dealing damage or blocking", def = "100", min = "0", max = "200")
	public static int KnockBackBasePower = 100;
	@ConfigProp(info = "Percentage of knockback power of all entities in the game when dealing or blocking ranged damage", def = "100", min = "0", max = "200")
	public static int KnockBackBasePowerRanged = 100;
	@ConfigProp(info = "Shows the rarity of the item in the inventory slot", def = "true", type = "client")
	public static boolean ShowRarityItem = true;
	@ConfigProp(info = "Percentage of knockback power of all entities in the game when dealing damage or blocking", def = "10", min = "0", max = "100")
	public static int DefaultHurtResistantTime = 10;
	@ConfigProp(info = "When NPCs self-heal, particles will appear above their heads", def = "true")
	public static boolean ShowHealingParticles = true;
	@ConfigProp(info = "To display script errors in chat or not", def = "true", type = "client")
	public static boolean DisplayErrorInChat= true;
	@ConfigProp(info = "Hide the quest compass while the camera is in first person view", def = "false", type = "client")
	public static boolean HideCompassInFirstPerson = false;
	@ConfigProp(info = "Show additional buttons in the GUI menu or not", def = "false", type = "client")
	public static boolean ShowButtonsInGuiMenu = false;
	@ConfigProp(info = "Replace background in menu", def = "true", type = "client")
	public static boolean ReplaceCustomBackground = true;
	@ConfigProp(info = "Commission for transferring coins between players through a team", def = "5", min = "0", max = "500")
	public static int CoinCommission = 5;
	@ConfigProp(info = "The animation of the GUI mod will be fast", def = "false", type = "client")
	public static boolean IsFastAnimationGUI = false;

	@SidedProxy(clientSide = "noppes.npcs.client.ClientProxy", serverSide = "noppes.npcs.CommonProxy")
	public static CommonProxy proxy;
	public static final String MODID = "customnpcs";
	public static final String MODNAME = "CustomNpcs";
	public static CustomNpcs instance;
	public static CommandNoppes NoppesCommand = new CommandNoppes();
	public static MarkovGenerator[] MARKOV_GENERATOR = new MarkovGenerator[10];
	public static MinecraftServer Server;
	public static DataDebug debugData = new DataDebug();
	public static boolean FreezeNPCs = false;
	public static File Dir;
	public static ConfigLoader Config;
	public static ITextComponent prefix = new TextComponentString(((char) 167) + "e[" + ((char) 167) + "2" + MODNAME + ((char) 167) + "e]" + ((char) 167) + "r: ");
	public static DimensionType customDimensionType;
	public static ModContainer mod;
	
	public static int colorAnimHoverPart = new Color(0xFFFA7800).getRGB();
    public static int PanoramaNumbers = 4;

    static { FluidRegistry.enableUniversalBucket(); }

	public static @Nullable File getWorldSaveDirectory() { return getWorldSaveDirectory(null); }

	public static @Nullable File getWorldSaveDirectory(String s) { return getWorldSaveDirectory(s, true); }

	public static @Nullable File getWorldSaveDirectory(String s, boolean local) {
		try {
			File dir = new File(".");
			if (EnableExternalSaving && !local) { dir = Dir; }
			if (Server != null) {
				if (!Server.isDedicatedServer()) { dir = new File(Minecraft.getMinecraft().mcDataDir, "saves"); }
				dir = new File(new File(dir, Server.getFolderName()), MODID);
			}
			if (s != null) { dir = new File(dir, s); }
			if (dir.exists() || dir.mkdirs()) { return dir; }
			return null;
		}
		catch (Exception e) { LogWriter.error("Error getting world save", e); }
		return null;
	}

	@Mod.EventHandler
	public static void postload(FMLPostInitializationEvent ev) {
		new Util();
		for (ModContainer modIn : Loader.instance().getModList()) {
			if (modIn.getModId().equals(MODID)) { mod = modIn; }
		}
		ForgeEventHandler.clientEventNames.put(CustomParticleEvent.CreateEvent.class, CustomParticleEvent.CREATE);
		ForgeEventHandler.clientEventNames.put(CustomParticleEvent.RenderEvent.class, CustomParticleEvent.RENDER);
		ForgeEventHandler.clientEventNames.put(CustomParticleEvent.UpdateEvent.class, CustomParticleEvent.TICK);
		ForgeEventHandler.clientEventNames.put(CustomPotionEvent.AffectEntity.class, EnumScriptType.POTION_IS_READY.function);
		ForgeEventHandler.clientEventNames.put(CustomPotionEvent.PerformEffect.class, EnumScriptType.POTION_PERFORM.function);
		ForgeEventHandler.clientEventNames.put(CustomPotionEvent.IsReadyEvent.class, EnumScriptType.POTION_IS_READY.function);
		ForgeEventHandler.clientEventNames.put(CustomPotionEvent.EndEffect.class, EnumScriptType.POTION_IS_READY.function);

		proxy.postload();
		LogWriter.info("Mod loaded ^_^ Have a good game!");
	}

	public CustomNpcs() { instance = this; }

	@Mod.EventHandler
	public void load(FMLInitializationEvent ev) {
		PixelmonHelper.load();
		Packets.register();
		ScriptController controller = new ScriptController();
		if (EnableScripting && !controller.languages.isEmpty()) {
			MinecraftForge.EVENT_BUS.register(controller);
			MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
			MinecraftForge.EVENT_BUS.register(new ScriptPlayerEventHandler().registerForgeEvents(ev.getSide()));
			MinecraftForge.EVENT_BUS.register(new ScriptItemEventHandler());
		}
		ForgeModContainer.fullBoundingBoxLadders = true;
		RecipeController.getInstance();
		MARKOV_GENERATOR[0] = new MarkovRoman(3);
		MARKOV_GENERATOR[1] = new MarkovJapanese(4);
		MARKOV_GENERATOR[2] = new MarkovSlavic(3);
		MARKOV_GENERATOR[3] = new MarkovWelsh(3);
		MARKOV_GENERATOR[4] = new MarkovSaami(3);
		MARKOV_GENERATOR[5] = new MarkovOldNorse(4);
		MARKOV_GENERATOR[6] = new MarkovAncientGreek(3);
		MARKOV_GENERATOR[7] = new MarkovAztec(3);
		MARKOV_GENERATOR[8] = new MarkovCustomNPCsClassic(3);
		MARKOV_GENERATOR[9] = new MarkovSpanish(3);
		proxy.load();
	}

	@Mod.EventHandler
	public void preload(FMLPreInitializationEvent ev) {
		Dir = new File(new File(ev.getModConfigurationDirectory(), ".."), MODID);
		if (!Dir.exists() && !Dir.mkdir()) {
			throw new RuntimeException("Impossible error: Failed to create sections important for the " + MODNAME + " mod!");
		}
		Config = new ConfigLoader(ev.getModConfigurationDirectory());
		if (NpcNavRange < 16) { NpcNavRange = 16; }
		MinecraftForge.EVENT_BUS.register(new CustomBlocks());
		MinecraftForge.EVENT_BUS.register(new CustomItems());
		MinecraftForge.EVENT_BUS.register(new CustomEntities());
		MinecraftForge.EVENT_BUS.register(new CustomPotions());
		CustomParticles.registerParticle();
		// Capabilities
		CapabilityManager.INSTANCE.register(IPlayerDataHandler.class, new PlayerDataStorage(), PlayerData::new);
		CapabilityManager.INSTANCE.register(IMarkDataHandler.class, new MarkDataStorage(), MarkData::new);
		CapabilityManager.INSTANCE.register(IWrapperEntityDataHandler.class, new WrapperEntityDataStorage(), WrapperEntityData::new);
		CapabilityManager.INSTANCE.register(IItemStackWrapperHandler.class, new ItemStackWrapperStorage(), ItemStackWrapper::new);

		NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);

		MinecraftForge.EVENT_BUS.register(new ServerEventsHandler());
		MinecraftForge.EVENT_BUS.register(new ServerTickHandler());
		MinecraftForge.EVENT_BUS.register(proxy);
		Objects.requireNonNull(NpcAPI.Instance()).events().register(new AbilityEventHandler());
		ForgeChunkManager.setForcedChunkLoadingCallback(this,
                new ChunkController());
		customDimensionType = DimensionType.register("CustomDimensions", "CustomNpcs", "CustomDimensions".hashCode(), CustomWorldProvider.class, false);
		proxy.preload();
		((IRangedAttributeMixin) SharedMonsterAttributes.MAX_HEALTH).setMaximumValue(Double.MAX_VALUE);
		DataObject.load();
	}

	@Mod.EventHandler
	public void serverStart(FMLServerStartingEvent event) {
		LogWriter.info("Mod starting");
		event.registerServerCommand(NoppesCommand);
		event.registerServerCommand(new CmdMoney()); // New from Unofficial (BetaZavr)
		EntityNPCInterface.ChatEventPlayer = new FakePlayer(event.getServer().getWorld(0), EntityNPCInterface.ChatEventProfile);
		EntityNPCInterface.CommandPlayer = new FakePlayer(event.getServer().getWorld(0), EntityNPCInterface.CommandProfile);
		EntityNPCInterface.GenericPlayer = new FakePlayer(event.getServer().getWorld(0), EntityNPCInterface.GenericProfile);
		for (WorldServer world : Server.worlds) {
			ServerScoreboard board = (ServerScoreboard) world.getScoreboard();
			board.addDirtyRunnable(() -> {
                for (String s : Availability.scores) {
                    ScoreObjective so = board.getObjective(s);
                    if (so != null) {
                        for (EntityPlayerMP player : Server.getPlayerList().getPlayers()) {
                            if (!board.entityHasObjective(player.getName(), so) && board.getObjectiveDisplaySlotCount(so) == 0) {
                                player.connection.sendPacket(new SPacketScoreboardObjective(so, 0));
                            }
                            player.connection.sendPacket(new SPacketUpdateScore(board.getOrCreateScore(player.getName(), so)));
                        }
                    }
                }
            });
		}
		DimensionHandler.getInstance().loadDimensions();
	}

	@Mod.EventHandler
	public void setAboutToStart(FMLServerAboutToStartEvent event) {
		LogWriter.info("Load map_world datas");
		Server = event.getServer();
		Availability.scores.clear();
		MarkovGenerator.load();
		ChunkController.instance.clear();
		FactionController.instance.load();
		ScriptController.Instance.load();
		ScriptController.HasStart = false;
		new PlayerDataController();
		new TransportController();
		new GlobalDataController();
		new SpawnController();
		new LinkedNpcController();
		WrapperNpcAPI.clearCache();
		CmdSchematics.names.clear();
		CmdSchematics.names.addAll(SchematicController.Instance.list());
		// New from Unofficial (BetaZavr)
		PlayerSkinController.getInstance();
		DropController.getInstance().loadFile();
		KeyController.getInstance().loadKeys();
		AnimationController.getInstance().loadAnimations();
		CustomNpcsPermissions.getInstance();
		Set<ResourceLocation> names = Block.REGISTRY.getKeys();
		for (ResourceLocation name : names) {
			Block block = Block.REGISTRY.getObject(name);
			if (block instanceof BlockLeaves) { block.setTickRandomly(LeavesDecayEnabled); }
			if (block instanceof BlockVine) { block.setTickRandomly(VineGrowthEnabled); }
			if (block instanceof BlockIce) { block.setTickRandomly(IceMeltsEnabled); }
		}
		// Remove old Entities
		File dir = getWorldSaveDirectory();
		if (dir != null) {
			File level = new File(dir.getParentFile(), "level.dat");
			if (level.exists()) {
				try {
					NBTTagCompound nbt = CompressedStreamTools.readCompressed(Files.newInputStream(level.toPath()));
					NBTTagList list = nbt.getCompoundTag("FML").getCompoundTag("Registries").getCompoundTag("minecraft:entities").getTagList("ids", 10);
					NBTTagList newList = new NBTTagList();
					boolean resave = false;
					for (int i = 0; i < list.tagCount(); i++) {
						String name = list.getCompoundTagAt(i).getString("K");
						if (name.indexOf("minecraft:customnpcs.") == 0) {
							resave = true;
							continue;
						}
						newList.appendTag(list.getCompoundTagAt(i));
					}
					if (resave) {
						nbt.getCompoundTag("FML").getCompoundTag("Registries").getCompoundTag("minecraft:entities")
								.setTag("ids", newList);
						CompressedStreamTools.writeCompressed(nbt, Files.newOutputStream(level.toPath()));
					}
				}
				catch (Exception e) { LogWriter.error(e); }
			}
		}
	}

	@Mod.EventHandler
	public void started(FMLServerStartedEvent event) {
		if (Server != null) {
			debugData.started = System.currentTimeMillis();
			debugData.startedTicks = Server.getWorld(0).getTotalWorldTime();
		}
		new BankController();
		new MarcetController();
		new BorderController();
		DialogController.instance.load();
		QuestController.instance.load();
		ScriptController.HasStart = true;
		ServerCloneController.Instance = new ServerCloneController();
		ScriptController.Instance.loadItemTextures();
	}

	@Mod.EventHandler
	public void stopped(FMLServerStoppedEvent event) {
		debugData.start("Mod");
		Config.config.save();
		ServerCloneController.Instance = null;

		// New from Unofficial (BetaZavr)
		BankController.getInstance().update();
		AnimationController.getInstance().save();
		ScriptController.Instance.saveItemTextures();
		ItemScripted.Resources.clear();
		PlayerSkinController.getInstance().save();
		PlayerSkinController.unload();
		KeyController.getInstance().save();
		DropController.getInstance().save();
		MarcetController.getInstance().save();
		WrapperNpcAPI.clearCache();
		Server = null;
		debugData.end("Mod");
	}

	public static void resetChars(String currencies, String donations) {
		try { displayCurrencies = "" + ((char) Integer.parseInt(currencies, 16)); }
		catch (Exception e) { displayCurrencies = "" + currencies.charAt(0); }
		try { displayDonation = "" + ((char) Integer.parseInt(donations, 16)); }
		catch (Exception e) { displayDonation = "" + donations.charAt(0); }
	}

}
