package noppes.npcs.client;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import io.netty.buffer.Unpooled;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.RecipeBook;
import net.minecraft.util.RandomSource;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.settings.KeyMappingLookup;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import noppes.npcs.*;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.client.IMinecraft;
import noppes.npcs.api.event.ClientEvent;
import noppes.npcs.api.handler.data.IKeySetting;
import noppes.npcs.api.handler.data.INpcRecipe;
import noppes.npcs.api.item.IItemScripted;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.client.WrapperMinecraft;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.controllers.PresetController;
import noppes.npcs.client.gui.*;
import noppes.npcs.client.gui.availability.SubGuiNpcAvailabilityItemStacks;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.client.gui.dimensions.GuiCreateDimension;
import noppes.npcs.client.gui.drop.SubGuiDropEdit;
import noppes.npcs.client.gui.elements.GuiManageCustomElements;
import noppes.npcs.client.gui.global.*;
import noppes.npcs.client.gui.mainmenu.*;
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
import noppes.npcs.client.gui.select.ResourceSelection;
import noppes.npcs.client.gui.yellow_de.GuiYellowDialogEditor;
import noppes.npcs.client.model.ModelNpcAlt;
import noppes.npcs.client.model.animation.AnimationConfig;
import noppes.npcs.client.particles.EntityEnderFX;
import noppes.npcs.client.particles.CustomParticleType;
import noppes.npcs.client.parts.ModelData;
import noppes.npcs.client.parts.ModelPartData;
import noppes.npcs.client.util.ClientRecipeRegister;
import noppes.npcs.client.util.CustomNpcsLangPack;
import noppes.npcs.config.CustomNpcsGuiFactory;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerCustomGui;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAnimation;
import noppes.npcs.items.custom.CustomBow;
import noppes.npcs.items.custom.CustomFishingRod;
import noppes.npcs.items.custom.CustomShield;
import noppes.npcs.mixin.client.IClientRecipeBookMixin;
import noppes.npcs.mixin.client.IKeyMappingMixin;
import noppes.npcs.potions.PotionData;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.util.TrueTypeFont;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.TempFile;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ClientProxy extends CommonProxy {

   private static final PlayerData playerData = new PlayerData();
   public static KeyMapping QuestLog;
   public static KeyMapping Scene1;
   public static KeyMapping SceneReset;
   public static KeyMapping Scene2;
   public static KeyMapping Scene3;
   public static FontContainer Font;
   public static FontContainer LogFont;
   public static ModelData data;
   public static PlayerModel<LivingEntity> playerModel;
   public static HumanoidArmorLayer<EntityCustomNpc, HumanoidModel<EntityCustomNpc>, HumanoidModel<EntityCustomNpc>> armorLayer;

   // New from Unofficial (BetaZavr)
   public static final Map<String, TempFile> loadFiles = new TreeMap<>();
   public static IMinecraft mcWrapper = null;

   public static void removeKeyFromMAP(Object parent) {
      if (parent instanceof KeyMapping keyMapping) {
         IKeyMappingMixin.getMap().remove(keyMapping);
      }
   }

   public static void addKeyToAll(String name, Object parent) {
      if (parent instanceof KeyMapping keyMapping) {
         IKeyMappingMixin.getAll().put(name, keyMapping);
      }
   }

   public static void tryAddKeyToMap(Object parent) {
      if (parent instanceof KeyMapping keyMapping) {
         KeyMappingLookup map = IKeyMappingMixin.getMap();
         if (!map.getAll(keyMapping.getKey()).contains(keyMapping)) { map.put(keyMapping.getKey(), keyMapping); }
      }
   }

   public void enqueueWork(Runnable runnable) {
      BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.CLIENT);
      if (!executor.isSameThread()) {
         executor.submitAsync(runnable);
      }
      else {
         runnable.run();
         CompletableFuture.completedFuture(null);
      }
   }

   @Override
   public void load() {
      enqueueWork(() -> {
         Font = new FontContainer(CustomNpcs.FontType, CustomNpcs.FontSize);
         LogFont = new FontContainer(CustomNpcs.LogFontType, CustomNpcs.LogFontSize);
         createFolders();

         CustomNpcResourceListener listener = new CustomNpcResourceListener();
         ((ReloadableResourceManager)Minecraft.getInstance().getResourceManager()).registerReloadListener(listener);
         listener.onResourceManagerReload(Minecraft.getInstance().getResourceManager());
         MenuScreens.register(CustomContainer.container_carpentrybench, GuiNpcCarpentryBench::new);
         MenuScreens.register(CustomContainer.container_mail, GuiMailmanWrite::new);
         MenuScreens.register(CustomContainer.container_managebanks, GuiNpcManageBanks::new);
         MenuScreens.register(CustomContainer.container_managerecipes, GuiNpcManageRecipes::new);
         MenuScreens.register(CustomContainer.container_merchantadd, GuiMerchantAdd::new);
         MenuScreens.register(CustomContainer.container_bank, GuiNPCBankChest::new);
         MenuScreens.register(CustomContainer.container_companion, GuiNpcCompanionInv::new);
         MenuScreens.register(CustomContainer.container_follower, GuiNpcFollower::new);
         MenuScreens.register(CustomContainer.container_followerhire, GuiNpcFollowerHire::new);
         MenuScreens.register(CustomContainer.container_followersetup, GuiNpcFollowerSetup::new);
         MenuScreens.register(CustomContainer.container_inv, GuiNpcInv::new);
         MenuScreens.register(CustomContainer.container_itemgiver, GuiNpcItemGiver::new);
         MenuScreens.register(CustomContainer.container_trader, GuiNPCTrader::new);
         MenuScreens.register(CustomContainer.container_tradersetup, SubGuiNPCManageDeal::new);
         MenuScreens.register(CustomContainer.container_customgui, (ContainerCustomGui container, Inventory inv, Component comp) -> {
            GuiCustom gui = new GuiCustom(container, inv, comp);
            gui.setGuiData(container.data);
            return gui;
         });
         new MusicController();
         MinecraftForge.EVENT_BUS.register(new ClientTickHandler());
         new PresetController(CustomNpcs.Dir);
         if (CustomNpcs.EnableUpdateChecker) {
            VersionChecker checker = new VersionChecker();
            checker.start();
         }
         PixelmonHelper.loadClient();

         // New from Unofficial (BetaZavr)
         MenuScreens.register(CustomContainer.container_questtypeitem, SubGuiNpcQuestTypeItem::new);
         MenuScreens.register(CustomContainer.container_managetransport, GuiNpcManageTransporters::new);
         MenuScreens.register(CustomContainer.container_availability_item, SubGuiNpcAvailabilityItemStacks::new);
         MenuScreens.register(CustomContainer.container_builder, GuiBuilderTools::new);
         MenuScreens.register(CustomContainer.container_custom_chest, GuiCustomContainer::new);
         MenuScreens.register(CustomContainer.container_dropsetup, SubGuiDropEdit::new);
         MenuScreens.register(CustomContainer.container_npc_dead, GuiNPCDeadInventory::new);

         Optional<? extends ModContainer> cont = ModList.get().getModContainerById(CustomNpcs.MODID);
         cont.ifPresent(modContainer -> modContainer.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, CustomNpcsGuiFactory.FACTORY));

         for (ICustomElement element : CustomItems.customitems) {
            if (element instanceof CustomShield shield) {
               ItemProperties.register(shield, new ResourceLocation("blocking"),
                       (stack, level, entity, option) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            }
            else if (element instanceof CustomBow bow) {
               ItemProperties.register(bow, new ResourceLocation("pull"),
                       (stack, level, entity, option) -> entity == null || entity.getUseItem() != stack ? 0.0F : (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / bow.getSpeed());
               ItemProperties.register(bow, new ResourceLocation("pulling"),
                       (stack, level, entity, option) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            }
            else if (element instanceof CustomFishingRod fishingRod) {
               ItemProperties.register(fishingRod, new ResourceLocation("cast"), (stack, level, entity, option) -> {
                  if (entity == null) { return 0.0F; }
                  boolean isMainStack = entity.getMainHandItem() == stack;
                  boolean isOffStack = !isMainStack && entity.getOffhandItem() == stack;
                  return (isMainStack || isOffStack) && entity instanceof Player player && player.fishing != null ? 1.0F : 0.0F;
               });
            }
         }
         CustomNpcsLangPack.load();
      });
   }

   public static PlayerData getPlayerData() {
      if (playerData.player == null) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            playerData.player = player;
            playerData.playerLevel = player.experienceLevel;
            playerData.animation = new DataAnimation(player);
            playerData.scriptData = new PlayerScriptData(player);
         }
      }
      return playerData;
   }

   @Deprecated
   public void postload() {
      MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
      Minecraft mc = Minecraft.getInstance();
      mc.getItemColors().register((stack, tintIndex) -> new Color(0x8B4513).getRGB(), CustomItems.mount);
      mc.getItemColors().register((stack, tintIndex) -> {
         if (stack.getItem() == CustomItems.scripted_item) {
            IItemStack item = Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stack);
            if (!item.isEmpty()) { return ((IItemScripted)item).getColor(); }
         }
         return -1;
      }, CustomItems.scripted_item);
      mcWrapper = new WrapperMinecraft(mc);
      ArmorersWorkshopHelper.register();

      ResourceSelection.preload(".png");
   }

   private void createFolders() {
      File dir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID);
      if (!dir.exists() && !dir.mkdirs()) {
         LogWriter.error("Failed to create directory " + dir.getAbsolutePath());
         return;
      }
      File sounds = new File(dir, "sounds");
      if (!sounds.exists() && !sounds.mkdir()) { LogWriter.error("Failed to create directory " + sounds.getAbsolutePath()); }

      File json = new File(dir, "sounds.json");
      try {
         if (!json.exists() && !json.createNewFile()) { return; }
         BufferedWriter writer = new BufferedWriter(new FileWriter(json));
         writer.write("{\n\n}");
         writer.close();
      }
      catch (IOException ignored) {}

      File meta = new File(CustomNpcs.Dir, "pack.mcmeta");
      try {
         if (!meta.exists() && !meta.createNewFile()) { return; }
         BufferedWriter writer = new BufferedWriter(new FileWriter(meta));
         writer.write("{\n    \"pack\": {\n        \"description\": \"" + CustomNpcs.MODID + " map resource pack\",\n        \"pack_format\": 6\n    }\n}");
         writer.close();
      }
      catch (IOException ignored) {}

      File textures = new File(dir, "textures");
      if (!textures.exists() && !textures.mkdir()) { LogWriter.error("Failed to create directory " + textures.getAbsolutePath()); }

      File fonts = new File(dir, "fonts");
      if (!fonts.exists() && !fonts.mkdir()) { LogWriter.error("Failed to create directory " + fonts.getAbsolutePath()); }
   }

   public static Screen getGui(EnumGuiType gui, EntityNPCInterface npc, FriendlyByteBuf buf) {
      ClientEvent.PreGetGuiCustomNpcs preEvent = new ClientEvent.PreGetGuiCustomNpcs(npc, gui, buf);
      MinecraftForge.EVENT_BUS.post(preEvent);
      if (preEvent.isCanceled()) { return null; }
      if (preEvent.returnGui != null) { return preEvent.returnGui; }
      Minecraft mc = Minecraft.getInstance();
      Screen returnGui = null;
      switch (preEvent.guiType) {
         case MainMenuDisplay: {
            if (preEvent.npc != null) { returnGui = new GuiNpcDisplay(preEvent.npc); }
            else if (mc.player != null) { mc.player.sendSystemMessage(Component.literal(Util.instance.translateGoogle(mc.player,"Unable to find npc"))); }
            break;
         }
         case MainMenuStats: returnGui = new GuiNpcStats(preEvent.npc); break;
         case MainMenuAdvanced: returnGui = new GuiNpcAdvanced(preEvent.npc); break;
         case MovingPath: {
            if (preEvent.npc != null) { returnGui = new GuiNpcPather(preEvent.npc); }
            break;
         }
         case ManageFactions: returnGui = new GuiNpcManageFactions(preEvent.npc); break;
         case ManageLinked: returnGui = new GuiNpcManageLinkedNpc(preEvent.npc); break;
         case BuilderBlock: returnGui = new GuiBlockBuilder(preEvent.buffer.readBlockPos()); break;
         case ManageDialogs: returnGui = new GuiNpcManageDialogs(preEvent.npc); break;
         case ManageQuests: returnGui = new GuiNpcManageQuest(preEvent.npc); break;
         case Companion: returnGui = new GuiNpcCompanionStats(preEvent.npc); break;
         case CompanionTalent: returnGui = new GuiNpcCompanionTalents(preEvent.npc); break;
         case MainMenuGlobal: returnGui = new GuiNpcGlobalMainMenu(preEvent.npc); break;
         case MainMenuAI: returnGui = new GuiNpcAI(preEvent.npc); break;
         case PlayerTransporter: returnGui = new GuiTransportSelection(preEvent.npc); break;
         case Script: {
            if (preEvent.npc != null) { returnGui = new GuiScriptNpc(preEvent.npc); }
            break;
         }
         case ScriptBlock: returnGui = new GuiScriptBlock(preEvent.buffer.readBlockPos()); break;
         case ScriptItem: returnGui = new GuiScriptItem(); break;
         case ScriptDoor: returnGui = new GuiScriptDoor(preEvent.buffer.readBlockPos()); break;
         case ScriptPlayers: returnGui = new GuiScriptGlobal(); break;
         case SetupTransporter: returnGui = new GuiNpcTransporter(preEvent.npc); break;
         case SetupBank: returnGui = new GuiNpcBankSetup(preEvent.npc); break;
         case NpcRemote: {
            if (mc.screen == null) { returnGui = new GuiNpcRemoteEditor(); }
            break;
         }
         case PlayerMailbox: returnGui = new GuiMailbox(); break;
         case NpcDimensions: returnGui = new GuiNpcDimension(); break;
         case Border: returnGui = new GuiBorderBlock(preEvent.buffer.readBlockPos()); break;
         case Portal: returnGui = new GuiPortalBlock(preEvent.buffer.readBlockPos()); break;
         case RedstoneBlock: returnGui = new GuiNpcRedstoneBlock(preEvent.buffer.readBlockPos()); break;
         case MobSpawner: returnGui = new GuiNpcMobSpawner(preEvent.buffer.readBlockPos()); break;
         case CopyBlock: returnGui = new GuiBlockCopy(preEvent.buffer.readBlockPos()); break;
         case MobSpawnerMounter: returnGui = new GuiNpcMobSpawnerMounter(); break;
         case Waypoint: returnGui = new GuiNpcWaypoint(preEvent.buffer.readBlockPos()); break;
         case NbtBook: returnGui = new GuiNbtBook(preEvent.buffer.readBlockPos()); break;
         // New from Unofficial (BetaZavr)
         case DimensionSetting: {
            if (preEvent.buffer.readBoolean()) {
               returnGui = new GuiCreateDimension(preEvent.buffer.readResourceKey(Registries.DIMENSION));
            } else {
               returnGui = new GuiCreateDimension(null);
            }
            break;
         }
         case QuestCompleteText: returnGui = new GuiQuestCompletion(preEvent.buffer.readInt()); break;
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
         //case ManageMail: returnGui = new GuiNpcManageLinkedNpc(preEvent.npc); break;
         case ManageGame: returnGui = new GuiYellowDialogEditor(); break;
         case EditClientScript: returnGui = new GuiScriptClient(); break;
         case PermissionsEdit: returnGui = new GuiPermissionsEdit(); break;
         case BoundarySetting: returnGui = new GuiBoundarySetting(preEvent.buffer.readBlockPos()); break;
         case PlacerTool:
         case SaverTool: {
            BlockPos pos = preEvent.buffer.readBlockPos();
            returnGui = new GuiBuilderSchematic(pos.getX(), pos.getY());
            break;
         }
         case ManageCustomElements: returnGui = new GuiManageCustomElements(); break;
      }
      ClientEvent.PostGetGuiCustomNpcs postEvent = new ClientEvent.PostGetGuiCustomNpcs(preEvent.npc, preEvent.guiType, preEvent.buffer, returnGui);
      MinecraftForge.EVENT_BUS.post(postEvent);
      if (postEvent.buffer != null) { postEvent.buffer.release(); }
      if (postEvent.isCanceled()) { return null; }
      return postEvent.returnGui;
   }

   @Override
   public void openGui(EntityNPCInterface npc, EnumGuiType gui, @Nullable FriendlyByteBuf buffer) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && minecraft.player.level().isClientSide()) {
         Screen guiscreen = getGui(gui,
                 npc != null ? npc : NoppesUtilServer.getEditingNpc(minecraft.player),
                 buffer != null ? buffer : new FriendlyByteBuf(Unpooled.buffer()));
         if (guiscreen != null) { minecraft.setScreen(guiscreen); }
      }
   }

   @Override
   public void openGui(Player player, Object guiscreen) {
      Minecraft minecraft = Minecraft.getInstance();
      if (player.level().isClientSide()) {
         if (guiscreen instanceof Screen screen) {
            ClientEvent.NextToGuiCustomNpcs event = new ClientEvent.NextToGuiCustomNpcs(NoppesUtilServer.getEditingNpc(player), minecraft.screen, screen);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.returnGui != null && !event.isCanceled()) { minecraft.setScreen(event.returnGui); }
         }
         else if (guiscreen instanceof EnumGuiType guiType) {
            openGui(null, guiType, null);
         }
      }
   }

   @Override
   public void spawnParticle(LivingEntity player, String string, Object... ob) {
      Minecraft mc = Minecraft.getInstance();
      if (string.equals("Block")) {
         BlockPos pos = (BlockPos)ob[0];
         BlockState state = (BlockState)ob[1];
         mc.particleEngine.destroy(pos, state);
      }
      else if (string.equals("ModelData")) {
         ModelData data = (ModelData)ob[0];
         ModelPartData particles = (ModelPartData)ob[1];
         EntityCustomNpc npc = (EntityCustomNpc)player;
         double height = npc.getMyRidingOffset() + (double)data.getBodyY();
         RandomSource rand = npc.getRandom();
         for (int i = 0; i < 2; ++i) {
            mc.particleEngine.add(new EntityEnderFX(npc, (rand.nextDouble() - 0.5) * player.getBbWidth(),
                    rand.nextDouble() * player.getBbHeight() - height - 0.25, (rand.nextDouble() - 0.5) * player.getBbWidth(),
                    (rand.nextDouble() - 0.5) * 2.0, -rand.nextDouble(), (rand.nextDouble() - 0.5) * 2.0,
                    particles));
         }
      }
   }

   @Override
   public @Nullable Player getPlayer() { return Minecraft.getInstance().player; }

   @Override
   public void spawnParticle(ParticleOptions particle, double x, double y, double z, double motionX, double motionY, double motionZ, float scale) {
      Minecraft mc = Minecraft.getInstance();
      double xx = Objects.requireNonNull(mc.getCameraEntity()).getX() - x;
      double yy = mc.getCameraEntity().getY() - y;
      double zz = mc.getCameraEntity().getZ() - z;
      if (!(xx * xx + yy * yy + zz * zz > 256.0D)) {
         Particle fx = mc.particleEngine.createParticle(particle, x, y, z, motionX, motionY, motionZ);
         if (fx != null) {
            if (particle == ParticleTypes.FLAME) {
               fx.scale(1.0E-5F);
            } else if (particle == ParticleTypes.SMOKE) {
               fx.scale(1.0E-5F);
            }
         }
      }
   }

   public static class FontContainer {
      private TrueTypeFont textFont = null;
      public boolean useCustomFont = true;

      private FontContainer() { }

      public FontContainer(String fontType, int fontSize) {
         try {
            textFont = new TrueTypeFont(new Font(fontType, java.awt.Font.PLAIN, fontSize), 1.0F);
            useCustomFont = !fontType.equalsIgnoreCase("minecraft");
            if (!useCustomFont || fontType.isEmpty() || fontType.equalsIgnoreCase("default")) {
               textFont = new TrueTypeFont(new ResourceLocation(CustomNpcs.MODID, "fonts/jetbrainsmono.ttf"), fontSize, 1.0F);
            }
         } catch (Throwable t) {
            LogWriter.except(t);
            useCustomFont = false;
         }
      }

      public int height(String text) {
         if (useCustomFont) { return textFont.height(text); }
         else { return Objects.requireNonNull(Minecraft.getInstance().font).lineHeight; }
      }

      public int width(String text) {
         return useCustomFont ? textFont.width(text) : Minecraft.getInstance().font.width(text);
      }

      public int width(Component component) {
         return useCustomFont ? textFont.width(component.getString()) : Minecraft.getInstance().font.width(component);
      }

      public FontContainer copy() {
         FontContainer font = new FontContainer();
         font.textFont = textFont;
         font.useCustomFont = useCustomFont;
         return font;
      }

      public int draw(PoseStack posestack, MultiBufferSource.BufferSource bufferSource, Object obj, float x, float y, int color) {
         String text = obj instanceof Component component ? Util.instance.getOldFormattedText(component) : String.valueOf(obj);
         if (useCustomFont && textFont.hasFont()) { return textFont.draw(posestack, text, x, y, color); }
         else {
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            return font.drawInBatch(text, x, y, color, true, posestack.last().pose(), bufferSource,
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, font.isBidirectional());
         }
      }

      public int draw(@Nonnull GuiGraphics graphics, Object obj, float x, float y, int color) {
         return draw(graphics.pose(), graphics.bufferSource(), obj, x, y, color);
      }

      public String getName() {
         return !useCustomFont ? "Minecraft" : textFont.getFontName();
      }

      public void clear() {
         if (textFont != null) { textFont.dispose(); }
      }

      public int getHeight() { return height("|"); }

      public TrueTypeFont getFont() { return textFont; }

   }

   // New from Unofficial (BetaZavr)
   @Override
   public String getLanguage(Player entity) {
      return Minecraft.getInstance().getLanguageManager().getSelected();
   }

   @Override
   public void init() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.screen instanceof IGuiInterface) { mc.screen.init(mc, mc.screen.width, mc.screen.height); }
   }

   @Override
   public void updateKeys() {
      List<KeyMapping> keyBindings = new ArrayList<>(Arrays.asList(Minecraft.getInstance().options.keyMappings));
      for (IKeySetting ks : KeyController.getInstance().getKeySettings()) {
         KeyModifier modifier = switch (ks.getModiferType()) {
             case 1 -> KeyModifier.SHIFT;
             case 2 -> KeyModifier.CONTROL;
             case 3 -> KeyModifier.ALT;
             default -> KeyModifier.NONE;
         };
         boolean added = true;
         for (KeyMapping kbD : keyBindings) {
            if (kbD.getKeyModifier() == modifier &&
                    kbD.getName().equals(ks.getName()) &&
                    kbD.getDefaultKey().getValue() == ks.getKeyId() &&
                    kbD.getCategory().equals(ks.getCategory())) {
               added = false;
               break;
            }
         }
         if (added) { keyBindings.add((KeyMapping) ((KeyConfig) ks).getMCKeyBinding()); }
      }
      Minecraft.getInstance().options.keyMappings = keyBindings.toArray(new KeyMapping[0]);
   }

   @Override
   public void loadAnimationModel(AnimationConfig animation) {
      ModelNpcAlt.loadAnimationModel(animation);
   }

   @Override
   public void createAllFiles(ICustomElement customElement) {
      super.createAllFiles(customElement);
      if (customElement instanceof Block) { NoppesUtil.createAllBlockFiles(customElement); }
      else if (customElement instanceof Item) { NoppesUtil.createAllItemFiles(customElement); }
      else if (customElement instanceof CustomParticleType particle) { NoppesUtil.createAllParticleFiles(particle); }
      else if (customElement instanceof PotionData potionData) { NoppesUtil.createAllPotionFiles(potionData); }
   }

   @Override
   public void playSound(SoundSource category, String sound, double x, double y, double z, float volume, float pitch, boolean streaming, boolean looping) {
      if (category != SoundSource.MUSIC) {
         if (streaming) { MusicController.Instance.playStreaming(new ResourceLocation(sound), getPlayer(), looping); }
         else { MusicController.Instance.playMusic(new ResourceLocation(sound), getPlayer(), looping); }
      }
      else {
         MusicController.Instance.playSound(category, sound, x, y, z, volume, pitch);
      }
   }

   @Override
   public void stopSound(int category, String sound) {
      SoundSource source = SoundSource.values()[ValueUtil.onlyPositiveInt(category, SoundSource.values().length)];
      if (sound == null || sound.isEmpty()) { Minecraft.getInstance().getSoundManager().stop(null, source); }
      else { MusicController.Instance.stopSound(new ResourceLocation(NoppesUtilServer.validLocation(sound)), source); }
   }

   @Override
   public @Nullable Level overworld() { return Minecraft.getInstance().level; }

   @Override
   public RecipeManager getRecipeManager() {
      if (CustomNpcs.Server != null) {
         return CustomNpcs.Server.getRecipeManager();
      } else if (Minecraft.getInstance().level != null) {
         return Minecraft.getInstance().level.getRecipeManager();
      }
      return null;
   }

   @Override
   public void syncRecipeManager() {
      super.syncRecipeManager();
      LocalPlayer player = (LocalPlayer) getPlayer();
      if (player != null) { syncRecipe(player.getRecipeBook()); }
   }

   @Override
   protected void syncRecipe(RecipeBook book) {
      super.syncRecipe(book);
      Player player = getPlayer();
      if (player != null && book instanceof ClientRecipeBook cBook) {
         Map<RecipeBookCategories, List<RecipeCollection>> collectionsByTab = new HashMap<>(((IClientRecipeBookMixin) cBook).getCollectionsByTab());
         RegistryAccess registryAccess = player.level().registryAccess();
         RecipeController rData = RecipeController.getInstance();
         for (int i = 0; i < 2; i++) {
            boolean isGlobal = i == 0;
            List<RecipeCollection> list = new ArrayList<>();
            for (String group : rData.getGroups(isGlobal)) {
               List<Recipe<?>> recipes = new ArrayList<>();
               for (INpcRecipe recipe : isGlobal ? rData.getGlobalRecipes(group) : rData.getAnvilRecipes(group)) {
                  if (recipe.isValid()) { recipes.add((Recipe<?>) recipe); }
               }
               if (!recipes.isEmpty()) {
                  RecipeCollection recipeCollection = new RecipeCollection(registryAccess, recipes);
                  recipeCollection.updateKnownRecipes(book);
                  list.add(recipeCollection);
               }
            }
            collectionsByTab.put(isGlobal ?
                    ClientRecipeRegister.CRAFTING_CUSTOM_GLOBAL_CATEGORY :
                    ClientRecipeRegister.CRAFTING_CUSTOM_ANVIL_CATEGORY, list);
         }
         ((IClientRecipeBookMixin) cBook).setCollectionsByTab(ImmutableMap.copyOf(collectionsByTab));
      }
   }

   @Override
   public @Nullable Level getOverWorld() {
      if (Minecraft.getInstance().level != null) { return Minecraft.getInstance().level; }
      return null;
   }

}
