package noppes.npcs.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickEmpty;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.handler.data.IKeySetting;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.gui.player.GuiLog;
import noppes.npcs.client.util.MusicData;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.KeyController;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.PlayerSkinController;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.mixin.client.gui.screens.IConfirmScreenMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.util.ResourceDownloader;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.TempFile;

import java.util.*;

public class ClientTickHandler {

   private Level prevLevel;
   private boolean otherContainer = false;

   // New from Unofficial (BetaZavr)
   public static List<MusicData> musics = new ArrayList<>();
   public static boolean checkMails = false;
   public static boolean inGame = false;
   public static long ticks = 0L;

   public static void loadFiles() {
      if (!ClientProxy.loadFiles.isEmpty()) {
         String isDel = "";
         for (String key : new ArrayList<>(ClientProxy.loadFiles.keySet())) {
            TempFile file = ClientProxy.loadFiles.get(key);
            if (file.lastLoad == 0) {
               Packets.sendServer(new SPacketGetFilePart(file.getNextPart(), key));
               file.lastLoad = System.currentTimeMillis();
            }
            else if (file.lastLoad + 12000L < System.currentTimeMillis()) {
               file.tryLoads++;
               if (file.tryLoads > 9) {
                  LogWriter.error("Failed to load file after 10 attempts: \"" + key + "\"");
                  isDel = key;
               } else {
                  Packets.sendServer(new SPacketGetFilePart(file.getNextPart(), key));
                  file.lastLoad = System.currentTimeMillis();
               }
            }
            break;
         }
         if (!isDel.isEmpty()) {
            ClientProxy.loadFiles.remove(isDel);
            Packets.sendServer(new SPacketRemoveLoadFile(isDel));
            loadFiles();
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void cnpcClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) { return; }
      CustomNpcs.debugData.start("Mod");
      Minecraft mc = Minecraft.getInstance();
      PlayerData data = PlayerData.get(mc.player);
      if (mc.player != null && mc.player.containerMenu instanceof InventoryMenu) {
         if (otherContainer) {
            Packets.sendServer(new SPacketQuestCompletionCheckAll());
            otherContainer = false;
         }
      }
      else { otherContainer = true; }
      ++ticks;
      ResourceDownloader.resetTickTaskBalance();
      if (prevLevel != mc.level) {
         prevLevel = mc.level;
         MusicController.Instance.stopSounds();
      }
      // New from Unofficial (BetaZavr)
      // Set in game
      if (mc.player == null) {
         if (inGame) {
            LogWriter.debug("Client Player: Exit game");
            inGame = false;
            ClientEventHandler.clearSchemes();
            PlayerSkinController.unload();
            ScriptController.Instance.clientScripts.saveDefaultScripts();
            EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.LOGOUT, new PlayerEvent.LogoutEvent((IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(mc.player)));
         }
         else if (!ScriptController.Instance.clientScripts.loadDefault) { ScriptController.Instance.clientScripts.loadDefaultScripts(); }
      }
      // client scripts tick
      if (ScriptController.Instance.clientScripts.isEnabled()) {
         EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.TICK, new PlayerEvent.UpdateEvent(ClientProxy.mcWrapper.getPlayer()));
      }
      // sounds
      try {
         Iterator<MusicData> it = musics.iterator();
         while (it.hasNext()) {
            MusicData md = it.next();
            if (md == null) {
               it.remove();
               continue;
            }
            if (md.channel.playing()) {
               md.createClientEvent(event, mc.player, 1);
            } else if (md.channel.stopped()) {
               md.createClientEvent(event, mc.player, 2);
               it.remove();
               if (mc.level != null && mc.getConnection() != null) { Packets.sendServer(new SPacketPlayerSound(false, md)); }
            }
         }
      } catch (Exception ignored) { }
       // any 0.5 sec
      if (ticks % 10 == 0) {
         // markets update
         MarcetController.getInstance().updateTime();
         // window size
         double h = data.overlay.getWindowSize().getHeight();
         double w = data.overlay.getWindowSize().getWidth();
         if (w != mc.getWindow().getGuiScaledWidth() || h != mc.getWindow().getGuiScaledHeight()) {
            w = mc.getWindow().getGuiScaledWidth();
            h = mc.getWindow().getGuiScaledHeight();
            if (mc.player != null) {
               Packets.sendServer(new SPacketWindowSize(w, h));
               data.overlay.getWindowSize().setSize(w, h);
            }
         }
         // files
         loadFiles();
         // hand distance
         if (mc.player != null) {
            double reachDistance = mc.player.getBlockReach();
            if (reachDistance != data.game.blockReachDistance) {
               data.game.blockReachDistance = reachDistance;
               data.game.updateClient = true;
            }
            double renderDistanceMeters = mc.options.renderDistance().get() * 16.0;
            if (renderDistanceMeters != data.game.renderDistance) {
               data.game.renderDistance = renderDistanceMeters;
               data.game.updateClient = true;
            }
         }
         else {
            data.game.blockReachDistance = 0.0d;
            data.game.renderDistance = 0.0d;
            data.game.updateClient = true;
         }
         // music bards
         if (mc.player != null) { MusicController.Instance.checkBards(mc.player); }
      }
      // mails
      if (checkMails || CustomNpcs.MailWindow != -1 && ticks % 100 == 0) {
         boolean hasNewMail = false;
         long time = System.currentTimeMillis();
         for (PlayerMail mail : data.mailData.playerMails) {
            if (!mail.beenRead && time - mail.timeWhenReceived >= mail.timeWillCome) {
               hasNewMail = true;
               break;
            }
         }
         if (hasNewMail != ClientEventHandler.hasNewMail) {
            ClientEventHandler.hasNewMail = hasNewMail;
            if (hasNewMail) {
               ClientEventHandler.showNewMail = 0L;
               ClientEventHandler.startMail = 0L;
               mc.player.sendSystemMessage(Component.translatable("mailbox.new.letters.received"));
            }
         }
         checkMails = false;
      }
      // clear keys in gui
      if (mc.screen != null) {
         if (!data.overlay.keyPress.isEmpty()) {
            Packets.sendServer(new SPacketPlayerKeyPressed(-1, false, false, false, false, false, mc.screen.getClass().getSimpleName()));
            data.overlay.keyPress.clear();
         }
      }
      CustomNpcs.debugData.end("Mod");
   }

   @SubscribeEvent
   public void cnpcKey(InputEvent.Key event) {
      CustomNpcs.debugData.start("Players");
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.getConnection() != null) {
         if (CustomNpcs.SceneButtonsEnabled) {
            if (ClientProxy.Scene1.isDown()) { Packets.sendServer(new SPacketSceneStart(1)); }
            if (ClientProxy.Scene2.isDown()) { Packets.sendServer(new SPacketSceneStart(2)); }
            if (ClientProxy.Scene3.isDown()) { Packets.sendServer(new SPacketSceneStart(3)); }
            if (ClientProxy.SceneReset.isDown()) { Packets.sendServer(new SPacketSceneReset()); }
         }
         if (ClientProxy.QuestLog.isDown()) {
            if (mc.screen == null) { NoppesUtil.openGUI(mc.player, new GuiLog(0)); }
            else if (mc.screen instanceof GuiLog) { mc.mouseHandler.grabMouse(); }
         }
         if (event.getAction() == 1 || event.getAction() == 0) {
            int key = event.getKey();
            if (event.getAction() == 1) {
               if (mc.screen instanceof ConfirmScreen gui) {
                  if (key == InputConstants.KEY_ESCAPE) {
                     ((IConfirmScreenMixin) gui).getCallback().accept(false);
                  } // ESC
                  if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
                     ((IConfirmScreenMixin) gui).getCallback().accept(true);
                  } // Enter
               }
            }
            long winID = mc.getWindow().getWindow();
            boolean isCtrlPressed = InputConstants.isKeyDown(winID, InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(winID, InputConstants.KEY_RCONTROL);
            boolean isShiftPressed = InputConstants.isKeyDown(winID, InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(winID, InputConstants.KEY_RSHIFT);
            boolean isAltPressed = InputConstants.isKeyDown(winID, InputConstants.KEY_LALT) || InputConstants.isKeyDown(winID, InputConstants.KEY_RALT);
            boolean isMetaPressed = InputConstants.isKeyDown(winID, InputConstants.KEY_LWIN) || InputConstants.isKeyDown(winID, InputConstants.KEY_RWIN);
            String openGui = mc.screen == null ? "" : mc.screen.getClass().getName();
            Packets.sendServer(new SPacketPlayerKeyPressed(key, isCtrlPressed, isShiftPressed, isAltPressed, isMetaPressed, event.getAction() == 1, openGui));
            if (mc.screen == null) {
               PlayerData data = PlayerData.get(mc.player);
               if (event.getAction() == 1) { data.overlay.keyPress.add(key); }
               else { data.overlay.keyPress.remove(key); }
            }
            if (event.getAction() == 1) {
               for (IKeySetting ks : KeyController.getInstance().getKeySettings()) {
                  if (ks.getKeyId() == key) {
                     boolean send = switch (ks.getModiferType()) {
                        case 1 -> isShiftPressed;
                        case 2 -> isCtrlPressed;
                        case 3 -> isAltPressed;
                        default -> true;
                     };
                     if (send) { Packets.sendServer(new SPacketKeyActive(ks.getId())); }
                  }
               }
            }
         }
      }
      CustomNpcs.debugData.end("Players");
   }

   @SubscribeEvent
   public void cnpcLeftClick(LeftClickEmpty event) {
      CustomNpcs.debugData.start("Players");
      if (event.getHand() == InteractionHand.MAIN_HAND) { Packets.sendServer(new SPacketPlayerLeftClicked()); }
      CustomNpcs.debugData.end("Players");
   }

   // New from Unofficial (BetaZavr)
   @SubscribeEvent
   public void cnpcScreenMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
      MouseHandler mh = Minecraft.getInstance().mouseHandler;
      cnpcMouseInput(-1, event.getMouseX(), event.getMouseY(), mh.getXVelocity(), mh.getYVelocity(), event.getScrollDelta(), false);
   }

   @SubscribeEvent
   public void cnpcScreenMouseInput(ScreenEvent.MouseButtonPressed.Pre event) {
      MouseHandler mh = Minecraft.getInstance().mouseHandler;
      cnpcMouseInput(event.getButton(), event.getMouseX(), event.getMouseY(), mh.getXVelocity(), mh.getYVelocity(), 0.0d, false);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void cnpcMouseInput(InputEvent.MouseButton event) {
      MouseHandler mh = Minecraft.getInstance().mouseHandler;
      cnpcMouseInput(event.getButton(), mh.xpos(), mh.ypos(),
              mh.getXVelocity(), mh.getYVelocity(),
              0.0d, event.getAction() == 1);
   }

   public void cnpcMouseInput(int key, double x, double y, double dx, double dy, double scrolled, boolean isDown) {
      CustomNpcs.debugData.start(null);
      Minecraft mc = Minecraft.getInstance();
      long winID = mc.getWindow().getWindow();
      boolean isCtrlPressed = InputConstants.isKeyDown(winID, InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(winID, InputConstants.KEY_RCONTROL);
      boolean isShiftPressed = InputConstants.isKeyDown(winID, InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(winID, InputConstants.KEY_RSHIFT);
      boolean isAltPressed = InputConstants.isKeyDown(winID, InputConstants.KEY_LALT) || InputConstants.isKeyDown(winID, InputConstants.KEY_RALT);
      boolean isMetaPressed = InputConstants.isKeyDown(winID, InputConstants.KEY_LWIN) || InputConstants.isKeyDown(winID, InputConstants.KEY_RWIN);
      IPlayer<?> iPlayer = (IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(mc.player);
      String openGui = mc.screen == null ? "" : mc.screen.getClass().getName();
      if (key < 0) {
         Event event = new PlayerEvent.MouseMoveEvent(iPlayer, x, y, dx, dy, scrolled, isCtrlPressed, isShiftPressed, isAltPressed, isMetaPressed);
         EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.MOUSE_MOVE, event);
      }
      else {
         Event event = new PlayerEvent.KeyPressedEvent(iPlayer, key, isCtrlPressed, isAltPressed, isShiftPressed, isMetaPressed, "");
         EventHooks.onEvent(ScriptController.Instance.clientScripts, isDown ? EnumScriptType.MOUSE_PRESSED : EnumScriptType.MOUSE_RELEASED, event);
         Packets.sendServer(new SPacketPlayerMousePressed(key, isDown, scrolled, isCtrlPressed, isShiftPressed, isAltPressed, isMetaPressed, openGui));
         PlayerData data = PlayerData.get(mc.player);
         if (isDown) { data.overlay.mousePress.add(key); }
         else { data.overlay.mousePress.remove(key); }
      }
      CustomNpcs.debugData.end(null);
   }

}
