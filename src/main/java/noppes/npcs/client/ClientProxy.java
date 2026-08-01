package noppes.npcs.client;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import micdoodle8.mods.galacticraft.api.client.tabs.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.recipebook.RecipeList;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.resources.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.RecipeBookClient;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.client.settings.KeyBindingMap;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.registries.IRegistryDelegate;
import noppes.npcs.*;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.client.IMinecraft;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.ClientEvent;
import noppes.npcs.api.handler.data.IKeySetting;
import noppes.npcs.api.handler.data.INpcRecipe;
import noppes.npcs.api.item.IItemScripted;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.client.WrapperMinecraft;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.controllers.PresetController;
import noppes.npcs.client.gui.yellow_de.GuiYellowDialogEditor;
import noppes.npcs.client.particles.EntityEnderFX;
import noppes.npcs.client.gui.*;
import noppes.npcs.client.gui.availability.SubGuiNpcAvailabilityItemStacks;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.client.gui.dimentions.GuiCreateDimension;
import noppes.npcs.client.gui.drop.SubGuiDropEdit;
import noppes.npcs.client.gui.global.*;
import noppes.npcs.client.gui.mainmenu.GuiNpcGlobalMainMenu;
import noppes.npcs.client.gui.mainmenu.GuiNpcInv;
import noppes.npcs.client.gui.mainmenu.GuiNpcAI;
import noppes.npcs.client.gui.select.ResourceSelection;
import noppes.npcs.client.gui.mainmenu.GuiNpcAdvanced;
import noppes.npcs.client.gui.mainmenu.GuiNpcDisplay;
import noppes.npcs.client.gui.mainmenu.GuiNpcStats;
import noppes.npcs.client.gui.model.GuiCreationParts;
import noppes.npcs.client.gui.player.*;
import noppes.npcs.client.gui.player.companion.GuiNpcCompanionInv;
import noppes.npcs.client.gui.player.companion.GuiNpcCompanionStats;
import noppes.npcs.client.gui.player.companion.GuiNpcCompanionTalents;
import noppes.npcs.client.gui.questtypes.SubGuiNpcQuestTypeItem;
import noppes.npcs.client.gui.roles.GuiNpcBankSetup;
import noppes.npcs.client.gui.roles.GuiNpcFollowerSetup;
import noppes.npcs.client.gui.roles.GuiNpcItemGiver;
import noppes.npcs.client.gui.roles.GuiNpcTransporter;
import noppes.npcs.client.gui.script.*;
import noppes.npcs.client.model.ModelBipedAlt;
import noppes.npcs.client.model.ModelNPCGolem;
import noppes.npcs.client.model.ModelNpcAlt;
import noppes.npcs.client.model.ModelNpcCrystal;
import noppes.npcs.client.model.ModelNpcDragon;
import noppes.npcs.client.model.ModelNpcSlime;
import noppes.npcs.client.model.animation.AnimationConfig;
import noppes.npcs.client.model.part.ModelData;
import noppes.npcs.client.parts.ModelPartData;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.client.renderer.RenderNPCPony;
import noppes.npcs.client.renderer.RenderNpcCrystal;
import noppes.npcs.client.renderer.RenderNpcDragon;
import noppes.npcs.client.renderer.RenderNpcSlime;
import noppes.npcs.client.renderer.RenderProjectile;
import noppes.npcs.client.util.CustomNpcsLangPack;
import noppes.npcs.client.util.aw.ArmourersWorkshopUtil;
import noppes.npcs.mixin.minecraftforge.client.IItemModelMesherForgeMixin;
import noppes.npcs.mixin.client.util.IRecipeBookClientMixin;
import noppes.npcs.shared.client.gui.util.TrueTypeFont;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.containers.*;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.KeyConfig;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerScriptData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPC64x32;
import noppes.npcs.entity.EntityNPCGolem;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityNpcAlex;
import noppes.npcs.entity.EntityNpcClassicPlayer;
import noppes.npcs.entity.EntityNpcCrystal;
import noppes.npcs.entity.EntityNpcDragon;
import noppes.npcs.entity.EntityNpcPony;
import noppes.npcs.entity.EntityNpcSlime;
import noppes.npcs.entity.EntityProjectile;
import noppes.npcs.entity.data.DataAnimation;
import noppes.npcs.items.custom.CustomArmor;
import noppes.npcs.items.ItemScripted;
import noppes.npcs.api.mixin.client.particle.IParticleFlameMixin;
import noppes.npcs.api.mixin.client.particle.IParticleSmokeNormalMixin;
import noppes.npcs.mixin.client.settings.IKeyBindingMixin;
import noppes.npcs.client.particles.CustomParticleSettings;
import noppes.npcs.potions.PotionData;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;
import noppes.npcs.util.TempFile;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nullable;

public class ClientProxy extends CommonProxy {

    public static void removeKeyFromMAP(Object parent) {
		if (parent instanceof KeyBinding) {
			((IKeyBindingMixin) parent).getMap().removeKey((KeyBinding) parent);
		}
	}

	public static void addKeyToAll(String name, Object parent) {
		if (parent instanceof KeyBinding) {
			((IKeyBindingMixin) parent).getAll().put(name, (KeyBinding) parent);
		}
	}

	public static void tryAddKeyToMap(Object parent) {
		if (parent instanceof KeyBinding) {
			KeyBinding keyBinding = (KeyBinding) parent;
			KeyBindingMap map = ((IKeyBindingMixin) parent).getMap();
			if (!map.lookupAll(keyBinding.getKeyCode()).contains(keyBinding)) { map.addKey(keyBinding.getKeyCode(), keyBinding); }
		}
	}

	public static class FontContainer {
		private TrueTypeFont textFont;
		public boolean useCustomFont = true;

		private FontContainer() { }

		public FontContainer(String fontType, int fontSize) {
			textFont = new TrueTypeFont(new Font(fontType, java.awt.Font.PLAIN, fontSize), 1.0f);
			useCustomFont = !fontType.equalsIgnoreCase("minecraft");
			try {
				if (!useCustomFont || fontType.isEmpty() || fontType.equalsIgnoreCase("default")) {
					textFont = new TrueTypeFont(new ResourceLocation(CustomNpcs.MODID, "fonts/jetbrainsmono.ttf"), fontSize, 1.0f);
				}
			} catch (Exception e) {
				LogWriter.info("Failed loading font so using Arial");
			}
		}

		public void clear() {
			if (textFont != null) { textFont.dispose(); }
		}

		public FontContainer copy() {
			FontContainer font = new FontContainer();
			font.textFont = textFont;
			font.useCustomFont = useCustomFont;
			return font;
		}

		public void draw(String text, float x, float y, int color) {
			if (useCustomFont && textFont.hasFont()) { textFont.draw(text, x, y, color); }
			else { Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, x, y, color); }
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		}

		public void draw(Component component, float x, float y, int color) { draw(component.getFormattedText(), x, y, color); }

		public void draw(ITextComponent component, float x, float y, int color) { draw(component.getFormattedText(), x, y, color); }

		public String getName() {
			if (!useCustomFont) { return "Minecraft"; }
			return textFont.getFontName();
		}

		public int height(String text) {
			if (useCustomFont) { return textFont.height(text); }
			return Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
		}

		public int width(String text) {
			if (useCustomFont) { return textFont.width(text); }
			return Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
		}

		public int width(Component component) { return width(component.getParent()); }

		public int width(ITextComponent component) { return width(component.getFormattedText()); }

        public int getHeight() { return height("|"); }

		public TrueTypeFont getFont() { return textFont; }

    }

	protected static PlayerData playerData = new PlayerData();
	public static KeyBinding QuestLog = new KeyBinding("key.quest.log", 38, "key.categories.gameplay"), Scene1, Scene2, Scene3, SceneReset;
	public static FontContainer Font;
	public static FontContainer LogFont;

	public static final Map<String, TempFile> loadFiles = new TreeMap<>();
	public static IMinecraft mcWrapper = null;

	private void createFolders() {
		File dir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID);
		if (!dir.exists() && !dir.mkdirs()) {
			LogWriter.error("Failed to create directory " + dir.getAbsolutePath());
			return;
		}
		File sounds = new File(dir, "sounds");
		if (!sounds.exists() && !sounds.mkdirs()) { LogWriter.error("Failed to create directory " + sounds.getAbsolutePath()); }

		File json = new File(dir, "sounds.json");
		if (!json.exists()) {
			try {
				if (!json.createNewFile()) { LogWriter.error("Failed to create file " + json.getAbsolutePath()); }
				BufferedWriter writer = new BufferedWriter(new FileWriter(json));
				writer.write("{\n\n}");
				writer.close();
			} catch (IOException e) { LogWriter.error(e); }
		}
		File textures = new File(dir, "textures");
		if (!textures.exists() && !textures.mkdirs()) { LogWriter.error("Failed to create directory " + textures.getAbsolutePath()); }

		File fonts = new File(dir, "fonts");
		if (!fonts.exists() && !fonts.mkdir()) { LogWriter.error("Failed to create directory " + fonts.getAbsolutePath()); }
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if (ID > EnumGuiType.values().length) { return null; }
		FriendlyByteBuf buffer = new FriendlyByteBuf();
		buffer.writeBlockPos(new BlockPos(x, y, z));
		return getGui(EnumGuiType.values()[ID], NoppesUtilServer.getEditingNpc(player), buffer);
	}

	public static GuiScreen getGui(EnumGuiType gui, EntityNPCInterface npc, FriendlyByteBuf buffer) {
		ClientEvent.PreGetGuiCustomNpcs preEvent = new ClientEvent.PreGetGuiCustomNpcs(npc, gui, buffer);
		MinecraftForge.EVENT_BUS.post(preEvent);
		if (preEvent.isCanceled()) { return null; }
		if (preEvent.returnGui != null) { return preEvent.returnGui; }
		GuiScreen returnGui = null;
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		Container container = getContainer(gui, player, buffer.copy());
		switch (gui) {
			case AvailabilityStack: {
				returnGui = new SubGuiNpcAvailabilityItemStacks((ContainerNpcAvailabilityItem) container);
				break;
			}
			case CustomContainer: {
				returnGui = new GuiCustomContainer((ContainerChestCustom) container);
				break;
			}
			case CustomChest: {
				returnGui = new GuiCustomChest((ContainerCustomChest) container);
				break;
			}
			case MainMenuDisplay: {
				if (npc != null) {
					returnGui = new GuiNpcDisplay(npc);
					break;
				}
				player.sendMessage(new TextComponentString(Util.instance.translateGoogle(player, "Unable to find npc")));
				break;
			}
			case MainMenuStats: {
				returnGui = npc == null ? null : new GuiNpcStats(npc);
				break;
			}
			case MainMenuInv: {
				returnGui = new GuiNpcInv(npc, (ContainerNPCInv) container);
				break;
			}
			case SetupDrop: {
				returnGui = new SubGuiDropEdit(npc, (ContainerNPCDropSetup) container);
				break;
			}
			case MainMenuAdvanced: {
				returnGui = new GuiNpcAdvanced(npc);
				break;
			}
			case QuestChooseReward: {
				Quest quest = QuestController.instance.get(preEvent.buffer.readInt());
				if (quest != null) {
					int size = preEvent.buffer.readInt();
					Map<Integer, ItemStack> rewardItems = new TreeMap<>();
					for (int i = 0; i < size; i++) { rewardItems.put(i, preEvent.buffer.readItem()); }
					returnGui = new GuiNpcQuestChooseReward(quest, rewardItems);
				}
				break;
			}
			case QuestTypeItem: {
				Quest quest = NoppesUtilServer.getEditingQuest(player);
				int id = buffer.readInt();
				if (quest != null && quest.questInterface.tasks[id].getEnumType() == EnumQuestTask.ITEM || Objects.requireNonNull(quest).questInterface.tasks[id].getEnumType() == EnumQuestTask.CRAFT) {
					returnGui = new SubGuiNpcQuestTypeItem((ContainerNpcQuestTypeItem) container);
				}
				break;
			}
			case MovingPath: {
				returnGui = npc == null ? null : new GuiNpcPather(npc);
				break;
			}
			case ManageFactions: {
				returnGui = new GuiNpcManageFactions(npc);
				break;
			}
			case ManageLinked: {
				returnGui = new GuiNpcManageLinkedNpc(npc);
				break;
			}
			case ManageMail: returnGui = new GuiNpcManageMail(npc); break;
			case ManageGame: returnGui = new GuiYellowDialogEditor(); break;
			case BuilderBlock: {
				returnGui = new GuiBlockBuilder(buffer.readBlockPos());
				break;
			}
			case ManageTransport: {
				returnGui = new GuiNpcManageTransporters(npc, (ContainerNPCTransports) container);
				break;
			}
			case ManageRecipes: {
				returnGui = new GuiNpcManageRecipes(npc, (ContainerManageRecipes) container);
				break;
			}
			case ManageDialogs: {
				returnGui = new GuiNpcManageDialogs(npc);
				break;
			}
			case ManageQuests: {
				returnGui = new GuiNpcManageQuest(npc);
				break;
			}
			case ManageBanks: {
				returnGui = new GuiNpcManageBanks(npc, (ContainerManageBanks) container);
				break;
			}
			case MainMenuGlobal: {
				returnGui = new GuiNpcGlobalMainMenu(npc);
				break;
			}
			case MainMenuAI: {
				returnGui = new GuiNpcAI(npc);
				break;
			}
			case PlayerAnvil: {
				returnGui = new GuiNpcCarpentryBench((ContainerCarpentryBench) container);
				break;
			}
			case PlayerFollower: {
				returnGui = new GuiNpcFollower(npc, (ContainerNPCFollowerHire) container);
				break;
			}
			case PlayerFollowerHire: {
				returnGui = new GuiNpcFollowerHire(npc, (ContainerNPCFollowerHire) container);
				break;
			}
			case PlayerTrader: {
				returnGui = new GuiNPCTrader((ContainerNPCTrader) container);
				break;
			}
			case PlayerBank: {
				returnGui = new GuiNPCBankChest(npc, (ContainerNPCBank) container);
				break;
			}
			case PlayerTransporter: {
				returnGui = new GuiTransportSelection(npc);
				break;
			}
			case Script: {
				returnGui = new GuiScriptNpc(npc);
				break;
			}
			case ScriptBlock: {
				returnGui = new GuiScriptBlock(buffer.readBlockPos());
				break;
			}
			case ScriptItem: {
				returnGui = new GuiScriptItem();
				break;
			}
			case ScriptDoor: {
				returnGui = new GuiScriptDoor(buffer.readBlockPos());
				break;
			}
			case ScriptPlayers: {
				returnGui = new GuiScriptGlobal();
				break;
			}
			case SetupFollower: {
				returnGui = new GuiNpcFollowerSetup(npc, (ContainerNPCFollowerSetup) container);
				break;
			}
			case SetupItemGiver: {
				returnGui = new GuiNpcItemGiver(npc, (ContainerNpcItemGiver) container);
				break;
			}
			case SetupTrader: {
				int marcetId = buffer.readInt();
				int dealId = buffer.readInt();
				if (marcetId >= 0) { GuiNpcManageMarkets.marcetId = marcetId; }
				if (dealId >= 0) { GuiNpcManageMarkets.dealId = dealId; }
				returnGui = new GuiNpcManageMarkets(npc);
				break;
			}
			case SetupTraderDeal: {
				returnGui = new SubGuiNPCManageDeal((ContainerNPCTraderSetup) container);
				break;
			}
			case SetupTransporter: {
				returnGui = new GuiNpcTransporter(npc);
				break;
			}
			case SetupBank: {
				returnGui = new GuiNpcBankSetup(npc);
				break;
			}
			case NpcRemote: {
				returnGui = Minecraft.getMinecraft().currentScreen == null ? new GuiNpcRemoteEditor() : null;
				break;
			}
			case PlayerMailbox: {
				returnGui = new GuiMailbox();
				break;
			}
			case PlayerMailOpen: {
				returnGui = new GuiMailmanWrite((ContainerMail) container);
				break;
			}
			case MerchantAdd: {
				returnGui = new GuiMerchantAdd((ContainerMerchantAdd) container);
				break;
			}
			case NpcDimensions: {
				returnGui = new GuiNpcDimension();
				break;
			}
			case Border: {
				returnGui = new GuiBorderBlock(buffer.readBlockPos());
				break;
			}
			case Portal: {
				returnGui = new GuiPortalBlock(buffer.readBlockPos());
				break;
			}
			case RedstoneBlock: {
				returnGui = new GuiNpcRedstoneBlock(buffer.readBlockPos());
				break;
			}
			case MobSpawner: {
				returnGui = new GuiNpcMobSpawner(buffer.readBlockPos());
				break;
			}
			case CopyBlock: {
				returnGui = new GuiBlockCopy(buffer.readBlockPos());
				break;
			}
			case MobSpawnerMounter: {
				returnGui = new GuiNpcMobSpawnerMounter();
				break;
			}
			case Waypoint: {
				returnGui = new GuiNpcWaypoint(buffer.readBlockPos());
				break;
			}
			case Companion: {
				returnGui = new GuiNpcCompanionStats(npc);
				break;
			}
			case CompanionTalent: {
				returnGui = new GuiNpcCompanionTalents(npc);
				break;
			}
			case CompanionInv: {
				returnGui = new GuiNpcCompanionInv(npc, (ContainerNPCCompanion) container);
				break;
			}
			case NbtBook: {
				returnGui = new GuiNbtBook(buffer.readBlockPos());
				break;
			}
			case CustomGui: {
				returnGui = new GuiCustom((ContainerCustomGui) container);
				break;
			}
			case QuestCompleteText: returnGui = new GuiQuestCompletion(preEvent.buffer.readInt()); break;
			case QuestLog: {
				returnGui = new GuiLog(buffer.readInt());
				break;
			}
            case DimensionSetting: {
				returnGui = new GuiCreateDimension(buffer.readInt());
				break;
			}
			case DeadInventory: {
				returnGui = new GuiNPCDeadInventory(npc, (ContainerDead) container);
				break;
			}
			case CreationParts: {
				returnGui = new GuiCreationParts(npc, (ContainerLayer) container);
				break;
			}
			// New from Unofficial (BetaZavr)
			case EditClientScript: returnGui = new GuiScriptClient(); break;
			case PermissionsEdit: returnGui = new GuiPermissionsEdit(); break;
			case BoundarySetting: returnGui = new GuiBoundarySetting(preEvent.buffer.readBlockPos()); break;
			case PlacerTool:
			case SaverTool: {
				BlockPos pos = preEvent.buffer.readBlockPos();
				returnGui = new GuiBuilderSchematic(pos.getX(), pos.getY());
				break;
			}
			case BuilderTool:
			case RemoverTool:
			case ReplaceTool: {
				returnGui = new GuiBuilderTools((ContainerBuilderSettings) container);
				break;
			}
			default: { break; }
		}
		ClientEvent.PostGetGuiCustomNpcs postEvent = new ClientEvent.PostGetGuiCustomNpcs(preEvent.npc, preEvent.guiType, preEvent.buffer, returnGui);
		MinecraftForge.EVENT_BUS.post(postEvent);
		if (postEvent.isCanceled()) { return null; }
		return postEvent.returnGui;
	}

	@Override
	public EntityPlayer getPlayer() { return Minecraft.getMinecraft().player; }

	@Override
	public PlayerData getPlayerData(EntityPlayer player) {
		if (playerData.player == null) {
			if (player == null) { player = Minecraft.getMinecraft().player; }
			if (player != null) {
				playerData.player = player;
				playerData.playerLevel = player.experienceLevel;
				playerData.animation = new DataAnimation(player);
				playerData.scriptData = new PlayerScriptData(player);
			}
		}
		return playerData;
	}

	@SuppressWarnings({ "unchecked", "rawtypes"})
	@Deprecated
	@Override
	public void load() {
		Minecraft mc = Minecraft.getMinecraft();
		MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
		MinecraftForge.EVENT_BUS.register(new ClientRegisterEvents());
		if (CustomNpcs.InventoryGuiEnabled) {
			MinecraftForge.EVENT_BUS.register(new TabRegistry());
			if (TabRegistry.getTabList().isEmpty()) {
				TabRegistry.registerTab(new InventoryTabVanilla());
				TabRegistry.registerTab(new InventoryTabFactions());
				TabRegistry.registerTab(new InventoryTabQuests());
			}
		}
		RenderingRegistry.registerEntityRenderingHandler(EntityNpcPony.class, (Render) new RenderNPCPony());
		RenderingRegistry.registerEntityRenderingHandler(EntityNpcCrystal.class, new RenderNpcCrystal(new ModelNpcCrystal()));
		RenderingRegistry.registerEntityRenderingHandler(EntityNpcDragon.class, new RenderNpcDragon(new ModelNpcDragon(), 0.5f));
		RenderingRegistry.registerEntityRenderingHandler(EntityNpcSlime.class, new RenderNpcSlime(new ModelNpcSlime(16), new ModelNpcSlime(0), 0.25f));
		RenderingRegistry.registerEntityRenderingHandler(EntityProjectile.class, new RenderProjectile());

		// Human Models
		RenderingRegistry.registerEntityRenderingHandler(EntityNPCGolem.class, new RenderNPCInterface(new ModelNPCGolem(0.0f), 0.0f));
		RenderingRegistry.registerEntityRenderingHandler(EntityNpcClassicPlayer.class, new RenderCustomNpc(new ModelNpcAlt(0.0f, false, true)));
		RenderingRegistry.registerEntityRenderingHandler(EntityNPC64x32.class, new RenderCustomNpc(new ModelBipedAlt(0.0f, false, false, false)));
		RenderingRegistry.registerEntityRenderingHandler(EntityCustomNpc.class, new RenderCustomNpc(new ModelNpcAlt(0.0f, false, false)));
		RenderingRegistry.registerEntityRenderingHandler(EntityNpcAlex.class, new RenderCustomNpc(new ModelNpcAlt(0.0f, true, false)));

		mc.getItemColors().registerItemColorHandler((stack, tintIndex) -> 9127187, CustomItems.mount, CustomItems.cloner, CustomItems.moving, CustomItems.scripter, CustomItems.wand, CustomItems.teleporter);
		mc.getItemColors().registerItemColorHandler((stack, tintIndex) -> {
			IItemStack item = Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stack);
			if (stack.getItem() == CustomItems.scripter_item) {
				return ((IItemScripted) item).getColor();
			}
			return -1;
		}, CustomItems.scripter_item);
		ClientRegisterEvents.load();
	}

	@Override
	public void openGui(EntityNPCInterface npc, EnumGuiType gui, @Nullable FriendlyByteBuf buffer) {
		Minecraft minecraft = Minecraft.getMinecraft();
		if (minecraft.player != null && minecraft.player.world.isRemote) {
			GuiScreen guiscreen = getGui(gui,
					npc != null ? npc : NoppesUtilServer.getEditingNpc(minecraft.player),
					buffer != null ? buffer : new FriendlyByteBuf(Unpooled.buffer()));
			if (guiscreen != null) { minecraft.displayGuiScreen(guiscreen); }
		}
	}

	@Override
	public void openGui(EntityPlayer player, Object guiscreen) {
		Minecraft minecraft = Minecraft.getMinecraft();
		if (player.world.isRemote) {
			if (guiscreen instanceof GuiScreen) {
				ClientEvent.NextToGuiCustomNpcs event = new ClientEvent.NextToGuiCustomNpcs(NoppesUtilServer.getEditingNpc(player), minecraft.currentScreen, (GuiScreen) guiscreen);
				MinecraftForge.EVENT_BUS.post(event);
				if (event.returnGui != null && !event.isCanceled()) { minecraft.displayGuiScreen(event.returnGui); }
			}
			else if (guiscreen instanceof EnumGuiType) {
				openGui(null, (EnumGuiType) guiscreen, null);
			}
		}
    }

	@Override
	public void postload() {
		// Set fields and methods in ArmourersWorkshop
		ArmourersWorkshopUtil.getInstance();

		// OBJ ItemStack Model Replace
		Minecraft mc = Minecraft.getMinecraft();
		RenderItem ri = mc.getRenderItem();
		Map<IRegistryDelegate<Item>, Int2ObjectMap<IBakedModel>> models = ((IItemModelMesherForgeMixin) ri.getItemModelMesher()).getModels();
		if (models != null) {
			for (IRegistryDelegate<Item> key : models.keySet()) {
				if (!(key.get() instanceof CustomArmor) || ((CustomArmor) key.get()).objModel == null) { continue; }
				IBakedModel ibm = ModelBuffer.getIBakedModel((CustomArmor) key.get());
				if (ibm == null) { continue; }
				models.get(key).put(0, ibm);
			}
		}
		mcWrapper = new WrapperMinecraft(mc);

		ResourceSelection.preload(".png");
	}

	@Override
	public void preload() {
		Font = new FontContainer(CustomNpcs.FontType, CustomNpcs.FontSize);
		LogFont = new FontContainer(CustomNpcs.LogFontType, CustomNpcs.LogFontSize);
		createFolders();

		((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new CustomNpcResourceListener());
		new MusicController();
		MinecraftForge.EVENT_BUS.register(new ClientTickHandler());
		MinecraftForge.EVENT_BUS.register(new OverlayEventHandler());
		// registerKeys
		if (CustomNpcs.SceneButtonsEnabled) {
			Scene1 = new KeyBinding("key.scene.s.e.0", 79, "key.categories.gameplay");
			Scene2 = new KeyBinding("key.scene.s.e.1", 80, "key.categories.gameplay");
			Scene3 = new KeyBinding("key.scene.s.e.2", 81, "key.categories.gameplay");
			SceneReset = new KeyBinding("key.scene.reset", 82, "key.categories.gameplay");
			ClientRegistry.registerKeyBinding(Scene1);
			ClientRegistry.registerKeyBinding(Scene2);
			ClientRegistry.registerKeyBinding(Scene3);
			ClientRegistry.registerKeyBinding(SceneReset);
		}
		ClientRegistry.registerKeyBinding(QuestLog);
		for (IKeySetting ks : KeyController.getInstance().getKeySettings()) {
			ClientRegistry.registerKeyBinding((KeyBinding) ((KeyConfig) ks).getMCKeyBinding());
		}
		//
		new PresetController(CustomNpcs.Dir);
		if (CustomNpcs.EnableUpdateChecker) {
			VersionChecker checker = new VersionChecker();
			checker.start();
		}
		PixelmonHelper.loadClient();
		OBJLoader.INSTANCE.addDomain(CustomNpcs.MODID);
		CustomNpcsLangPack.load();
	}

	@Override
	public void reloadItemTextures() {
		for (Map.Entry<Integer, String> entry : ItemScripted.Resources.entrySet()) {
			ModelResourceLocation mrl = new ModelResourceLocation(entry.getValue(), "inventory");
			Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CustomItems.scripter_item,
					entry.getKey(), mrl);
			ModelLoader.setCustomModelResourceLocation(CustomItems.scripter_item, entry.getKey(), mrl);
		}
	}

	@Deprecated
	@Override
	public void spawnParticle(EntityLivingBase player, String string, Object... ob) {
		if (string.equals("Block")) {
			BlockPos pos = (BlockPos) ob[0];
			int id = (int) ob[1];
			Block block = Block.getBlockById(id & 0xFFF);
			Minecraft.getMinecraft().effectRenderer.addBlockDestroyEffects(pos, block.getStateFromMeta(id >> 12 & 0xFF));
		} else if (string.equals("ModelData")) {
			ModelData data = (ModelData) ob[0];
			ModelPartData particles = (ModelPartData) ob[1];
			EntityCustomNpc npc = (EntityCustomNpc) player;
			Minecraft minecraft = Minecraft.getMinecraft();
			double height = npc.getYOffset() + data.getBodyY();
			Random rand = npc.getRNG();
			for (int i = 0; i < 2; ++i) {
				EntityEnderFX fx = new EntityEnderFX(npc, (rand.nextDouble() - 0.5) * player.width,
						rand.nextDouble() * player.height - height - 0.25, (rand.nextDouble() - 0.5) * player.width,
						(rand.nextDouble() - 0.5) * 2.0, -rand.nextDouble(), (rand.nextDouble() - 0.5) * 2.0,
						particles);
				minecraft.effectRenderer.addEffect(fx);
			}
		}
	}

	@Override
	public void spawnParticle(EnumParticleTypes particle, double x, double y, double z, double motionX, double motionY, double motionZ, float scale) {
		Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
		if (entity == null) { return; }
		double xx = entity.posX - x;
		double yy = entity.posY - y;
		double zz = entity.posZ - z;
		if (xx * xx + yy * yy + zz * zz > 256.0) {
			return;
		}
		Particle fx = Minecraft.getMinecraft().effectRenderer.spawnEffectParticle(particle.getParticleID(), x, y, z, motionX, motionY, motionZ);
		if (fx == null) {
			return;
		}
		if (particle == EnumParticleTypes.FLAME) { ((IParticleFlameMixin) fx).npcs$setFlameScale(scale); }
		else if (particle == EnumParticleTypes.SMOKE_NORMAL) { ((IParticleSmokeNormalMixin) fx).npcs$setSmokeParticleScale(scale); }
	}

	// New from Unofficial (BetaZavr)
	@Override
	public void updateKeys() {
		List<KeyBinding> keyBindings = new ArrayList<>(Arrays.asList(Minecraft.getMinecraft().gameSettings.keyBindings));
		for (IKeySetting ks : KeyController.getInstance().getKeySettings()) {
			KeyModifier modifier;
			switch (ks.getModiferType()) {
				case 1:
					modifier = KeyModifier.SHIFT;
					break;
				case 2:
					modifier = KeyModifier.CONTROL;
					break;
				case 3:
					modifier = KeyModifier.ALT;
					break;
				default:
					modifier = KeyModifier.NONE;
					break;
			}
			boolean added = true;
			for (KeyBinding kbD : keyBindings) {
				if (kbD.getKeyModifier() == modifier &&
						kbD.getKeyDescription().equals(ks.getName()) &&
						kbD.getKeyCodeDefault() == ks.getKeyId() &&
						kbD.getKeyCategory().equals(ks.getCategory())) {
					added = false;
					break;
				}
			}
			if (added) { keyBindings.add((KeyBinding) ((KeyConfig) ks).getMCKeyBinding()); }
		}
		Minecraft.getMinecraft().gameSettings.keyBindings = keyBindings.toArray(new KeyBinding[0]);
	}

	@Override
	public String getLanguage(EntityPlayer entity) {
		return Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
	}

	public void loadAnimationModel(AnimationConfig animation) {
		ModelNpcAlt.loadAnimationModel(animation);
	}

	@Override
	public void updatePlayerPos() {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (player != null) {
			player.connection.sendPacket(new CPacketPlayer.PositionRotation(player.posX, player.getEntityBoundingBox().minY, player.posZ, player.rotationYaw, player.rotationPitch, player.onGround));
		}
	}

	@Override
	public void createAllFiles(ICustomElement customElement) {
		super.createAllFiles(customElement);
		if (customElement instanceof Block) { NoppesUtil.createAllBlockFiles(customElement); }
		else if (customElement instanceof Item) { NoppesUtil.createAllItemFiles(customElement); }
		else if (customElement instanceof CustomParticleSettings) { NoppesUtil.createParticleFiles((CustomParticleSettings) customElement); }
		else if (customElement instanceof PotionData) { NoppesUtil.createAllPotionFiles((PotionData) customElement); }
	}

	@Override
	public void playSound(SoundCategory category, String sound, double x, double y, double z, float volume, float pitch, boolean streaming, boolean looping) {
		if (category != SoundCategory.MUSIC) {
			if (streaming) { MusicController.Instance.playStreaming(new ResourceLocation(sound), getPlayer(), looping); }
			else { MusicController.Instance.playMusic(new ResourceLocation(sound), getPlayer(), looping); }
		}
		else {
			MusicController.Instance.playSound(category, sound, x, y, z, volume, pitch);
		}
	}

	@Override
	public void stopSound(int category, String sound) {
		SoundCategory source = SoundCategory.values()[ValueUtil.onlyPositiveInt(category, SoundCategory.values().length)];
		if (sound == null || sound.isEmpty()) { Minecraft.getMinecraft().getSoundHandler().stop("", source); }
		else { MusicController.Instance.stopSound(new ResourceLocation(NoppesUtilServer.validLocation(sound)), source); }
	}

	@Override
	public @Nullable World overworld() { return Minecraft.getMinecraft().world; }

	@Override
	public void syncRecipeManager() {
		super.syncRecipeManager();
		EntityPlayerSP player = (EntityPlayerSP) getPlayer();
		if (player != null) { syncRecipe(player.getRecipeBook()); }
	}

	@Override
	protected void syncRecipe(RecipeBook book) {
		super.syncRecipe(book);
		EntityPlayer player = getPlayer();
		if (player != null && book instanceof RecipeBookClient) {
			RecipeBookClient cBook = (RecipeBookClient) book;
			Map<CreativeTabs, List<RecipeList>> RECIPES_BY_TAB = ((IRecipeBookClientMixin) cBook).getCollectionsByTab();
			RecipeController rData = RecipeController.getInstance();
			for (int i = 0; i < 2; i++) {
				boolean isGlobal = i == 0;
				List<RecipeList> list = new ArrayList<>();
				for (String group : rData.getGroups(isGlobal)) {
					List<IRecipe> recipes = new ArrayList<>();
					for (INpcRecipe recipe : isGlobal ? rData.getGlobalRecipes(group) : rData.getAnvilRecipes(group)) {
						if (recipe.isValid()) { recipes.add((IRecipe) recipe); }
					}
					if (!recipes.isEmpty()) {
						RecipeList recipeCollection = new RecipeList();
						for (IRecipe recipe : recipes) { recipeCollection.add(recipe); }
						recipeCollection.updateKnownRecipes(book);
						list.add(recipeCollection);
					}
				}
				RECIPES_BY_TAB.put(isGlobal ? ClientRegisterEvents.CRAFTING_CUSTOM_GLOBAL_CATEGORY : ClientRegisterEvents.CRAFTING_CUSTOM_ANVIL_CATEGORY, list);
			}
		}
	}

	@Override
	public @Nullable World getOverWorld() {
		if (Minecraft.getMinecraft().world != null) { return Minecraft.getMinecraft().world; }
		return null;
	}

}
