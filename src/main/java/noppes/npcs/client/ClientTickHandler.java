package noppes.npcs.client;

import java.util.*;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.*;
import noppes.npcs.api.handler.data.IKeySetting;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.constants.*;
import noppes.npcs.controllers.KeyController;
import noppes.npcs.controllers.PlayerSkinController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.mixin.client.gui.IGuiYesNoMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketRemoveLoadFile;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.common.util.LogWriter;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.gui.player.GuiLog;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.client.util.MusicData;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.util.TempFile;
import org.lwjgl.input.Mouse;

public class ClientTickHandler {

	private boolean otherContainer = false;
	private World prevWorld;

	// New from Unofficial (BetaZavr)
	public static List<MusicData> musics = new ArrayList<>();
	public static boolean checkMails = false;
	public static boolean inGame = false;
	public static long ticks = 0L;

	public static void loadFiles() {
		if (!ClientProxy.loadFiles.isEmpty()) {
			String isDel = "";
			for (String key : ClientProxy.loadFiles.keySet()) {
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
	public void cnpcClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.END) { return; }
		CustomNpcs.debugData.start(null);
		Minecraft mc = Minecraft.getMinecraft();
		PlayerData data = CustomNpcs.proxy.getPlayerData(mc.player);
		if (mc.player != null && mc.player.openContainer instanceof ContainerPlayer) {
			if (otherContainer) {
				Packets.sendServer(new SPacketQuestCompletionCheckAll());
				otherContainer = false;
			}
		}
		else { otherContainer = true; }

		++ticks;
		++RenderNPCInterface.LastTextureTick;
		if (prevWorld != mc.world) {
			prevWorld = mc.world;
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
			EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.TICK, new PlayerEvent.UpdateEvent((IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(mc.player)));
		}
		// sounds
		try	{
			for (MusicData md : new ArrayList<>(musics)) {
				if (md != null) {
					if (md.playing()) { md.createClientEvent(event, mc.player, 1); }
					else if (md.stopped()) {
						md.createClientEvent(event, mc.player, 2);
						musics.remove(md);
						if (mc.world != null && mc.getConnection() != null) { Packets.sendServer(new SPacketPlayerSound(false, md)); }
					}
				}
				else { musics.remove(null); }
			}
		}
		catch (Exception e) { musics.clear(); }
        // any 0.5 sec
		if (ticks % 10 == 0) {
			// markets update
			MarcetController.getInstance().updateTime();
			// window size
			double h = data.overlay.getWindowSize().getHeight();
			double w = data.overlay.getWindowSize().getWidth();
			ScaledResolution sw = new ScaledResolution(mc);
			if (w != sw.getScaledWidth() || h != sw.getScaledHeight()) {
				w = sw.getScaledWidth();
				h = sw.getScaledHeight();
				if (mc.player != null) {
					Packets.sendServer(new SPacketWindowSize(w, h));
					data.overlay.getWindowSize().setSize(w, h);
				}
			}
			// files
			loadFiles();
			// hand distance
			if (mc.playerController != null) {
				double reachDistance = mc.playerController.getBlockReachDistance();
				if (mc.playerController.extendedReach()) { reachDistance = 6.0; }
				if (data.game.blockReachDistance != reachDistance) {
					data.game.blockReachDistance = reachDistance;
					data.game.updateClient = true;
				}
				double renderDistanceMeters = mc.gameSettings.getOptionFloatValue(GameSettings.Options.RENDER_DISTANCE) * 16.0;
				if (data.game.renderDistance != renderDistanceMeters) {
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
		// clear hash
		if (ticks % 60 == 0) {
			ModelBuffer.clear();
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
					mc.player.sendMessage(Component.translatable("mailbox.new.letters.received").getParent());
				}
			}
			checkMails = false;
		}
		// clear keys in gui
		if (mc.currentScreen != null) {
			if (!data.overlay.keyPress.isEmpty()) {
				Packets.sendServer(new SPacketPlayerKeyPressed(-1, false, false, false, false, false, mc.currentScreen.getClass().getSimpleName()));
				data.overlay.keyPress.clear();
			}
		}
		CustomNpcs.debugData.end(null);
	}

	@SubscribeEvent
	public void cnpcKey(InputEvent.KeyInputEvent event) {
		CustomNpcs.debugData.start(null);
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.world != null && mc.getConnection() != null) {
			if (CustomNpcs.SceneButtonsEnabled) {
				if (ClientProxy.Scene1.isPressed()) { Packets.sendServer(new SPacketSceneStart(1)); }
				if (ClientProxy.Scene2.isPressed()) { Packets.sendServer(new SPacketSceneStart(2)); }
				if (ClientProxy.Scene3.isPressed()) { Packets.sendServer(new SPacketSceneStart(3)); }
				if (ClientProxy.SceneReset.isPressed()) { Packets.sendServer(new SPacketSceneReset()); }
			}
			if (ClientProxy.QuestLog.isPressed()) {
				if (mc.currentScreen == null) { NoppesUtil.openGUI(mc.player, new GuiLog(0)); }
				else if (mc.currentScreen instanceof GuiLog) { mc.setIngameFocus(); }
			}
			int key = Keyboard.getEventKey();
			boolean isDown = Keyboard.getEventKeyState();
			if (isDown) {
				if (mc.currentScreen instanceof GuiYesNo) {
					if (key == Keyboard.KEY_ESCAPE) {
						GuiYesNoCallback parentScreen = ((IGuiYesNoMixin) mc.currentScreen).getParentScreen();
						parentScreen.confirmClicked(false, ((IGuiYesNoMixin) mc.currentScreen).getParentButtonClickedId());
					} // ESC
					if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
						GuiYesNoCallback parentScreen = ((IGuiYesNoMixin) mc.currentScreen).getParentScreen();
						parentScreen.confirmClicked(true, ((IGuiYesNoMixin) mc.currentScreen).getParentButtonClickedId());
					} // Enter
				}
			}
			boolean isCtrlPressed = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
			boolean isShiftPressed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
			boolean isAltPressed = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
			boolean isMetaPressed = Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA);
			String openGui = mc.currentScreen == null ? "" : mc.currentScreen.getClass().getName();
			Packets.sendServer(new SPacketPlayerKeyPressed(key, isCtrlPressed, isShiftPressed, isAltPressed, isMetaPressed, isDown, openGui));
			PlayerData data = CustomNpcs.proxy.getPlayerData(mc.player);
			if (mc.currentScreen == null) {
				if (isDown) { data.overlay.keyPress.add(key); }
				else { data.overlay.keyPress.remove(key); }
			}
			if (isDown) {
				for (IKeySetting ks : KeyController.getInstance().getKeySettings()) {
					if (ks.getKeyId() == key) {
						boolean send;
						switch (ks.getModiferType()) {
							case 1: send = isShiftPressed; break;
							case 2: send = isCtrlPressed; break;
							case 3: send = isAltPressed; break;
							default: send = true;
						}
						if (send) { Packets.sendServer(new SPacketKeyActive(ks.getId())); }
					}
				}
			}
		}
		CustomNpcs.debugData.end(null);
	}

	@SubscribeEvent
	public void cnpcLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
		CustomNpcs.debugData.start(null);
		if (event.getHand() == EnumHand.MAIN_HAND) { Packets.sendServer(new SPacketPlayerLeftClicked()); }
		CustomNpcs.debugData.end(null);
	}

	// New from Unofficial (BetaZavr)
	@SubscribeEvent
	public void cnpcScreenMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
		cnpcMouseInput(Mouse.getEventButton(), Mouse.getEventX(), Mouse.getEventY(), Mouse.getEventDX(), Mouse.getEventDY(), Mouse.getEventDWheel(), Mouse.getEventButtonState());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void cnpcMouseInput(MouseEvent event) {
		cnpcMouseInput(event.getButton(), event.getX(), event.getY(), event.getDx(), event.getDy(), event.getDwheel(), event.isButtonstate());
	}

	public void cnpcMouseInput(int key, int x, int y, int dx, int dy, int dWheel, boolean isDown) {
		CustomNpcs.debugData.start(null);
		boolean isCtrlPressed = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
		boolean isShiftPressed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
		boolean isAltPressed = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
		boolean isMetaPressed = Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA); // Windows and Command
		if (Minecraft.getMinecraft().player == null || Minecraft.getMinecraft().world == null) {
			CustomNpcs.debugData.end(null);
			return;
		}
		IPlayer<?> iPlayer = (IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(Minecraft.getMinecraft().player);
		if (key < 0) {
			Event event = new PlayerEvent.MouseMoveEvent(iPlayer, x, y, dx, dy, dWheel, isCtrlPressed, isShiftPressed, isAltPressed, isMetaPressed);
			EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.MOUSE_MOVE, event);
		}
		else {
			Event event = new PlayerEvent.KeyPressedEvent(iPlayer, key, isCtrlPressed, isAltPressed, isShiftPressed, isMetaPressed, "");
			EventHooks.onEvent(ScriptController.Instance.clientScripts, isDown ? EnumScriptType.MOUSE_PRESSED : EnumScriptType.MOUSE_RELEASED, event);
			Minecraft mc = Minecraft.getMinecraft();
			Packets.sendServer(new SPacketPlayerMousePressed(key, isDown, dWheel, isCtrlPressed, isShiftPressed, isAltPressed, isMetaPressed,
					mc.currentScreen == null ? "" : mc.currentScreen.getClass().getName()));
			PlayerData data = CustomNpcs.proxy.getPlayerData(mc.player);
			if (isDown) { data.overlay.mousePress.add(key); }
			else { data.overlay.mousePress.remove(key); }
		}
		CustomNpcs.debugData.end(null);
	}

}
