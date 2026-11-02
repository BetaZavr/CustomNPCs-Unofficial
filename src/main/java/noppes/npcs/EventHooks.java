package noppes.npcs;

import java.util.Map;
import java.util.Objects;

import noppes.npcs.api.event.*;
import noppes.npcs.api.gui.IButton;
import noppes.npcs.api.gui.IItemSlot;
import noppes.npcs.api.gui.IScroll;
import noppes.npcs.controllers.data.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketBankSetPlayer;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IEntityItem;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.NpcEvent.CustomNpcTeleport;
import noppes.npcs.api.event.PlayerEvent.CustomTeleport;
import noppes.npcs.api.event.WorldEvent.ScriptTriggerEvent;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.handler.IFactionHandler;
import noppes.npcs.api.handler.IRecipeHandler;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.api.wrapper.ContainerCustomChestWrapper;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.api.wrapper.PlayerWrapper;
import noppes.npcs.api.wrapper.WrapperNpcAPI;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.containers.ContainerNPCBank;
import noppes.npcs.controllers.CustomGuiController;
import noppes.npcs.controllers.scripts.IScriptBlockHandler;
import noppes.npcs.controllers.scripts.IScriptHandler;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityProjectile;
import noppes.npcs.entity.data.DataScript;

import javax.annotation.Nonnull;

public class EventHooks {

	public static void onCustomChestClicked(CustomContainerEvent.SlotClickedEvent event) {
		ContainerCustomChestWrapper container = (ContainerCustomChestWrapper) event.container;
		if (!container.script.isValid()) { return; }
		onEvent(container.script, EnumScriptType.CUSTOM_CHEST_CLICKED, event);
	}

	public static void onCustomChestClosed(CustomContainerEvent.CloseEvent event) {
		ContainerCustomChestWrapper container = (ContainerCustomChestWrapper) event.container;
		if (!container.script.isValid()) { return; }
		onEvent(container.script, EnumScriptType.CUSTOM_CHEST_CLOSED, event);
	}

	public static void onCustomGuiButton(PlayerWrapper<?> player, ICustomGui gui, IButton button) {
		CustomGuiController.onButton(new CustomGuiEvent.ButtonEvent(player, gui, button));
	}

	public static void onCustomGuiClose(PlayerWrapper<?> player, ICustomGui gui) {
		CustomGuiController.onClose(new CustomGuiEvent.CloseEvent(player, gui));
	}

	public static void onCustomGuiScrollClick(PlayerWrapper<?> player, ICustomGui gui, IScroll scroll, int scrollIndex, String[] selection, boolean doubleClick) {
		CustomGuiController.onScrollClick(new CustomGuiEvent.ScrollEvent(player, gui, scroll, scrollIndex, selection, doubleClick));
	}

	public static void onCustomGuiSlot(PlayerWrapper<?> player, ICustomGui gui, IItemSlot slot, IItemStack heldItem) {
		CustomGuiController.onQuickCraft(new CustomGuiEvent.SlotEvent(player, gui, slot, heldItem));
	}

	public static boolean onCustomGuiSlotClicked(PlayerWrapper<?> player, ICustomGui gui, int slotId, int dragType, String clickType, IItemStack heldItem, Slot slot) {
		return CustomGuiController.onSlotClick(new CustomGuiEvent.SlotClickEvent(player, gui, slotId, player.getOpenContainer().getItem(slotId), dragType, clickType, heldItem, slot));
	}

	public static void onCustomPotionEvent(CustomPotionEvent event, EnumScriptType type) {
		onEvent(ScriptController.Instance.potionScripts, type, event);
	}

	public static boolean onEvent(IScriptHandler handler, EnumScriptType enumFunction, Event event) {
		if (enumFunction == null) { return false; }
		return onEvent(handler, enumFunction.function, event);
	}

	public static boolean onEvent(IScriptHandler handler, String enumFunction, Event event) {
		if (handler == null || !handler.getEnabled() || event == null || enumFunction == null || enumFunction.isEmpty()) { return false; }
		if (handler instanceof DataScript) {
			if (((DataScript) handler).getNPC().ais.aiDisabled) { return false; }
			ScriptController.Instance.npcsScripts.runScript(enumFunction, event);
		}
		handler.runScript(enumFunction, event);
		return WrapperNpcAPI.EVENT_BUS.post(event) && event.isCanceled();
	}

	private static void onEvent(ScriptContainer script, EnumScriptType enumFunction, Event event) {
		if (script != null && event != null && enumFunction != null) {
			script.run(enumFunction.function, event);
			WrapperNpcAPI.EVENT_BUS.post(event);
		}
	}

	public static void onForgeInit(ForgeScriptData handler) {
		onEvent(handler, EnumScriptType.INIT, new ForgeEvent.InitEvent());
	}

	public static void onForgeEvent(@Nonnull IScriptHandler handler, @Nonnull Event event) {
		if (!handler.isEnabled()) { return; }
		Map<Class<?>, String> names = handler.isClient() ? ForgeEventHandler.clientEventNames : ForgeEventHandler.eventNames;
		String eventName = names.get(event.getClass());
		if (eventName == null) {
			eventName = event.getClass().getName();
			int i = eventName.lastIndexOf(".");
			eventName = StringUtils.uncapitalize(eventName.substring(i + 1).replace("$", ""));
			names.put(event.getClass(), eventName);
			LogWriter.info("Found new Forge Event \"" + eventName + "\" to event: "+event.getClass().getName());
		}
		handler.runScript(eventName, event);
	}

	public static void onNPCsInit(NpcScriptData handler) {
		onEvent(handler, EnumScriptType.INIT, new NpcEvent.InitEvent(null));
	}

	public static void onGlobalFactionsLoaded(IFactionHandler handler) {
		HandlerEvent.FactionsLoadedEvent event = new HandlerEvent.FactionsLoadedEvent(handler);
		WrapperNpcAPI.EVENT_BUS.post(event);
	}

	public static boolean onNPCAttacksMelee(EntityNPCInterface npc, NpcEvent.MeleeAttackEvent event) {
		if (npc.script.isClient()) { return false; }
		return onEvent(npc.script, EnumScriptType.ATTACK_MELEE, event);
	}

	public static void onNPCCollide(EntityNPCInterface npc, Entity entity) {
		if (npc.script.isClient()) { return; }
		onEvent(npc.script, EnumScriptType.COLLIDE, new NpcEvent.CollideEvent(npc.wrappedNPC, entity));
	}

	public static void onNPCNeedBlockDamage(EntityNPCInterface npc, NpcEvent.NeedBlockDamage event) {
		if (npc.script.isClient()) { return; }
		onEvent(npc.script, EnumScriptType.NEED_BLOCK_DAMAGED, event);
	}

	public static boolean onNPCDamaged(EntityNPCInterface npc, NpcEvent.DamagedEvent event) {
		if (npc.script.isClient()) { return false; }
		event.setCanceled(npc.isKilled());
		return onEvent(npc.script, EnumScriptType.DAMAGED, event);
	}

	public static boolean onNPCDialog(EntityNPCInterface npc, EntityPlayer player, Dialog dialog) {
		if (npc.script.isClient()) { return false; }
		DialogEvent.OpenEvent event = new DialogEvent.OpenEvent(npc.wrappedNPC, player, dialog);
		if (npc.script != null && !(npc instanceof EntityDialogNpc)) { onEvent(npc.script, EnumScriptType.DIALOG, event); }
		return onEvent(getPlayerHandler(), EnumScriptType.DIALOG, event);
	}

	public static void onNPCDialogClose(EntityNPCInterface npc, EntityPlayerMP player, Dialog dialog) {
		if (npc.script.isClient()) { return; }
		DialogEvent.CloseEvent event = new DialogEvent.CloseEvent(npc.wrappedNPC, player, dialog);
		if (npc.script != null && !(npc instanceof EntityDialogNpc)) { onEvent(npc.script, EnumScriptType.DIALOG_CLOSE, event); }
		onEvent(getPlayerHandler(), EnumScriptType.DIALOG_CLOSE, event);
	}

	public static boolean onNPCDialogOption(EntityNPCInterface npc, EntityPlayerMP player, Dialog dialog, DialogOption option) {
		if (npc.script.isClient()) { return false; }
		DialogEvent.OptionEvent event = new DialogEvent.OptionEvent(npc.wrappedNPC, player, dialog, option);
		if (npc.script != null && !(npc instanceof EntityDialogNpc)) { onEvent(npc.script, EnumScriptType.DIALOG_OPTION, event); }
		return onEvent(getPlayerHandler(), EnumScriptType.DIALOG_OPTION, event);
	}

	public static void onNPCDied(EntityNPCInterface npc, NpcEvent.DiedEvent event) {
		if (npc.script.isClient()) { return; }
		onEvent(npc.script, EnumScriptType.DIED, event);
	}

	public static void onNPCInit(EntityNPCInterface npc) {
		if (npc.script.isClient()) { return; }
		onEvent(npc.script, EnumScriptType.INIT, new NpcEvent.InitEvent(npc.wrappedNPC));
	}

	public static boolean onNPCInteract(EntityNPCInterface npc, EntityPlayer player) {
		if (npc.script.isClient()) { return false; }
		NpcEvent.InteractEvent event = new NpcEvent.InteractEvent(npc.wrappedNPC, player);
		event.setCanceled(npc.isAttacking() || npc.isKilled() || npc.faction.isAggressiveToPlayer(player));
		return onEvent(npc.script, EnumScriptType.INTERACT, event);
	}

	public static void onNPCKills(EntityNPCInterface npc, EntityLivingBase entityLiving) {
		if (npc.script.isClient()) { return; }
		onEvent(npc.script, EnumScriptType.KILL, new NpcEvent.KilledEntityEvent(npc.wrappedNPC, entityLiving));
	}

	public static void onNPCRangedLaunched(EntityNPCInterface npc, NpcEvent.RangedLaunchedEvent event) {
		if (npc.script.isClient()) { return; }
		onEvent(npc.script, EnumScriptType.RANGED_LAUNCHED, event);
	}

	public static boolean onNPCRole(EntityNPCInterface npc, RoleEvent event) {
		if (npc.script.isClient()) { return false; }
		return onEvent(npc.script, EnumScriptType.ROLE, event);
	}

	public static boolean onNPCTarget(EntityNPCInterface npc, NpcEvent.TargetEvent event) {
		if (npc.script.isClient()) { return false; }
		return onEvent(npc.script, EnumScriptType.TARGET, event);
	}

	public static boolean onNPCTargetLost(EntityNPCInterface npc, EntityLivingBase prevtarget) {
		if (npc.script.isClient()) { return false; }
		return onEvent(npc.script, EnumScriptType.TARGET_LOST,  new NpcEvent.TargetLostEvent(npc.wrappedNPC, prevtarget));
	}

	public static CustomNpcTeleport onNpcTeleport(EntityNPCInterface npc, BlockPos to, BlockPos portal, int dimId) {
		NpcAPI api = NpcAPI.Instance();
		if (api != null) {
			CustomNpcTeleport event = new NpcEvent.CustomNpcTeleport(npc == null ? null : npc.wrappedNPC, api.getIPos(portal), api.getIPos(to), dimId);
			if (npc != null) {
				DataScript handler = npc.script;
				if (!handler.getEnabled()) { onEvent(handler, EnumScriptType.CUSTOM_TELEPORT, event); }
			}
			return event;
		}
		return new NpcEvent.CustomNpcTeleport(npc == null ? null : npc.wrappedNPC,
				new BlockPosWrapper(npc == null ? null : npc.world, portal),
				new BlockPosWrapper(npc == null ? null : npc.world, to),
				dimId);
	}

	public static void onNPCTick(EntityNPCInterface npc) {
		if (!npc.script.isEnabled()) { return; }
		ScriptController.Instance.tryAdd(2, npc);
		if (npc.script.isClient()) {
			onEvent(ScriptController.Instance.clientScripts, EnumScriptType.TICK, new NpcEvent.UpdateEvent(npc.wrappedNPC));
			return;
		}
		onEvent(npc.script, EnumScriptType.TICK, new NpcEvent.UpdateEvent(npc.wrappedNPC));
	}

	public static void onNPCTimer(EntityNPCInterface npc, int id) {
		onEvent(npc.script, EnumScriptType.TIMER, new NpcEvent.TimerEvent(npc.wrappedNPC, id));
	}

	public static BaseScriptData getPlayerHandler() {
		if (Util.instance.getSide().isClient()) { return ScriptController.Instance.clientScripts; }
		return ScriptController.Instance.playerScripts;
	}

	public static boolean onPlayerAttack(PlayerEvent.AttackEvent event) { return onEvent(getPlayerHandler(), EnumScriptType.ATTACK, event); }

	public static boolean onPlayerBreak(PlayerEvent.BreakEvent event) { return onEvent(getPlayerHandler(), EnumScriptType.BROKEN, event); }

	public static boolean onPlayerChat(PlayerEvent.ChatEvent event) { return onEvent(getPlayerHandler(), EnumScriptType.CHAT, event); }

	public static void onPlayerContainerClose(PlayerEvent.ContainerClosed event) { onEvent(getPlayerHandler(), EnumScriptType.CONTAINER_CLOSED, event); }

	public static void onPlayerContainerOpen(PlayerEvent.ContainerOpen event) { onEvent(getPlayerHandler(), EnumScriptType.CONTAINER_OPEN, event); }

	public static boolean onPlayerDamaged(PlayerEvent.DamagedEvent event) { return onEvent(getPlayerHandler(), EnumScriptType.DAMAGED, event); }

	public static boolean onPlayerDamagedEntity(PlayerEvent.DamagedEntityEvent event) { return onEvent(getPlayerHandler(), EnumScriptType.DAMAGED_ENTITY, event); }

	public static void onPlayerDeath(PlayerEvent.DiedEvent event) { onEvent(getPlayerHandler(), EnumScriptType.DIED, event); }

	public static void onPlayerFactionChange(PlayerEvent.FactionUpdateEvent event) {
		BaseScriptData handler = getPlayerHandler();
		if (handler.isClient()) { return; }
		onEvent(handler, EnumScriptType.FACTION_UPDATE, event);
	}

	public static void onPlayerInit(PlayerEvent.InitEvent event) { onEvent(getPlayerHandler(), EnumScriptType.INIT, event); }

	public static boolean onPlayerInteract(boolean hadInteractIn, PlayerEvent.InteractEvent event) {
		BaseScriptData handler = getPlayerHandler();
		handler.hadInteract = hadInteractIn;
		return onEvent(handler, EnumScriptType.INTERACT, event);
	}

	public static void onPlayerKeyActive(PlayerEvent.KeyActive event) { onEvent(getPlayerHandler(), EnumScriptType.KEY_ACTIVE, event); }

	public static void onPlayerKeyEvent(EntityPlayerMP player, int button, boolean isCtrlPressed, boolean isShiftPressed, boolean isAltPressed, boolean isMetaPressed, boolean pressed, String openGui) {
		onEvent(getPlayerHandler(), pressed ? EnumScriptType.KEY_PRESSED : EnumScriptType.KEY_RELEASED,
				new PlayerEvent.KeyPressedEvent(PlayerData.get(player).getIPlayer(), button, isCtrlPressed, isAltPressed, isShiftPressed, isMetaPressed, openGui));
	}

	public static void onPlayerKills(PlayerEvent.KilledEntityEvent event) { onEvent(getPlayerHandler(), EnumScriptType.KILL, event); }

	public static void onPlayerLevelUp(PlayerEvent.LevelUpEvent event) { onEvent(getPlayerHandler(), EnumScriptType.LEVEL_UP, event); }

	public static void onPlayerLogin(PlayerEvent.LoginEvent event) { onEvent(getPlayerHandler(), EnumScriptType.LOGIN, event); }

	public static void onPlayerLogout(PlayerEvent.LogoutEvent event) { onEvent(getPlayerHandler(), EnumScriptType.LOGOUT, event); }

	public static void onPlayerMouseEvent(EntityPlayerMP player, int button, boolean isDown, double scrolledIn,
											boolean isCtrlPressed, boolean isShiftPressed, boolean isAltPressed, boolean isMetaPressed,
										  String openGui) {
		if (CustomNpcs.EnableScripting && !ScriptController.Instance.languages.isEmpty()) { return; }
		onEvent(getPlayerHandler(), scrolledIn != 0.0d ? EnumScriptType.MOUSE_SCROLLED :
				isDown ? EnumScriptType.MOUSE_PRESSED : EnumScriptType.MOUSE_RELEASED,
				new PlayerEvent.KeyPressedEvent(PlayerData.get(player).getIPlayer(), button, isCtrlPressed, isAltPressed, isShiftPressed, isMetaPressed, openGui));
	}

	public static boolean onPlayerPickUp(PlayerEvent.PickUpEvent event) { return onEvent(getPlayerHandler(), EnumScriptType.PICKUP, event); }

	public static boolean onPlayerRanged(PlayerEvent.RangedLaunchedEvent event) { return onEvent(getPlayerHandler(), EnumScriptType.RANGED_LAUNCHED, event); }

	public static CustomTeleport onPlayerTeleport(EntityPlayerMP player, BlockPos to, BlockPos portal, int dimId) {
		NpcAPI api = NpcAPI.Instance();
		if (api != null) {
			CustomTeleport event = new PlayerEvent.CustomTeleport((IPlayer<?>) api.getIEntity(player), api.getIPos(portal), api.getIPos(to), dimId);
			if (player != null) {
				if (getPlayerHandler().getEnabled()) {
					onEvent(getPlayerHandler(), EnumScriptType.CUSTOM_TELEPORT, event);
				}
			}
			return event;
		}
		return new PlayerEvent.CustomTeleport(null,
				new BlockPosWrapper(player == null ? null : player.world, portal),
				new BlockPosWrapper(player == null ? null : player.world, to),
				dimId);
	}

	public static void onPlayerTick(PlayerEvent.UpdateEvent event) { onEvent(getPlayerHandler(), EnumScriptType.TICK, event); }

	public static void onPlayerTimer(PlayerEvent.TimerEvent event) { onEvent(getPlayerHandler(), EnumScriptType.TIMER, event); }

	public static boolean onPlayerToss(PlayerEvent.TossEvent event) { return onEvent(getPlayerHandler(), EnumScriptType.TOSS, event); }

	public static void onPlayerScreen(EntityPlayerMP player, String newGUI, String oldGUI) {
		if (player == null) { return; }
		if (newGUI.equals("GuiNPCBankChest") && oldGUI.equals("GuiIngame")) {
			ContainerNPCBank.editPlayerBankData = null;
			Packets.send(player, new PacketBankSetPlayer(""));
		}
		PlayerData data = PlayerData.get(player);
		data.overlay.currentGUI = newGUI;
		onEvent(getPlayerHandler(), EnumScriptType.GUI_OPEN, new PlayerEvent.OpenGUI(data.getIPlayer(), newGUI, oldGUI));
	}

	public static boolean onPlayerPlace(PlayerEvent.PlaceEvent event) { return onEvent(getPlayerHandler(), EnumScriptType.PLEASED, event); }

	public static void onPotionInit(PotionScriptData handler) {
		onEvent(handler, EnumScriptType.INIT, new ForgeEvent.InitEvent());
	}

	public static void onProjectileImpact(EntityProjectile projectile, ProjectileEvent.ImpactEvent event) {
		for (ScriptContainer script : projectile.scripts) {
			if (script.isValid()) { script.run(EnumScriptType.PROJECTILE_IMPACT.function, event); }
		}
		WrapperNpcAPI.EVENT_BUS.post(event);
	}

	public static void onProjectileTick(EntityProjectile projectile) {
		ProjectileEvent.UpdateEvent event = new ProjectileEvent.UpdateEvent(projectile);
		for (ScriptContainer script : projectile.scripts) {
			if (script.isValid()) {
				script.run(EnumScriptType.PROJECTILE_TICK.function, event);
			}
		}
		WrapperNpcAPI.EVENT_BUS.post(event);
	}

	public static boolean onQuestCanceled(QuestEvent.QuestCanceledEvent event) { return onEvent(getPlayerHandler(), EnumScriptType.QUEST_CANCELED, event); }

	public static void onQuestFinished(QuestEvent.QuestCompletedEvent event) { onEvent(getPlayerHandler(), EnumScriptType.QUEST_COMPLETED, event); }

	public static boolean onQuestStarted(QuestEvent.QuestStartEvent event) { return onEvent(getPlayerHandler(), EnumScriptType.QUEST_START, event); }

	public static void onQuestTurnedIn(QuestEvent.QuestTurnedInEvent event) { onEvent(EventHooks.getPlayerHandler(), EnumScriptType.QUEST_TURNING, event); }

	public static void onGlobalRecipesLoaded(IRecipeHandler handler) {
		HandlerEvent.RecipesLoadedEvent event = new HandlerEvent.RecipesLoadedEvent(handler);
		WrapperNpcAPI.EVENT_BUS.post(event);
	}

	public static void onScriptBlockBreak(IScriptBlockHandler handler) {
		if (handler.isClient()) { return; }
		onEvent(handler, EnumScriptType.BROKEN, new BlockEvent.BreakEvent(handler.getBlock()));
	}

	public static void onScriptBlockClicked(IScriptBlockHandler handler, EntityPlayer player) {
		if (handler.isClient()) { return; }
		onEvent(handler, EnumScriptType.CLICKED, new BlockEvent.ClickedEvent(handler.getBlock(), player));
	}

	public static void onScriptBlockCollide(IScriptBlockHandler handler, Entity entityIn) {
		if (handler.isClient()) { return; }
		onEvent(handler, EnumScriptType.COLLIDE, new BlockEvent.CollidedEvent(handler.getBlock(), entityIn));
	}

	public static boolean onScriptBlockDoorToggle(IScriptBlockHandler handler) {
		if (handler.isClient()) { return false; }
		return onEvent(handler, EnumScriptType.DOOR_TOGGLE, new BlockEvent.DoorToggleEvent(handler.getBlock()));
	}

	public static boolean onScriptBlockExploded(IScriptBlockHandler handler) {
		if (handler.isClient()) { return false; }
		return onEvent(handler, EnumScriptType.EXPLODED, new BlockEvent.ExplodedEvent(handler.getBlock()));
	}

	public static float onScriptBlockFallenUpon(IScriptBlockHandler handler, Entity entity, float distance) {
		if (handler.isClient()) { return distance; }
		BlockEvent.EntityFallenUponEvent event = new BlockEvent.EntityFallenUponEvent(handler.getBlock(), entity, distance);
		if (onEvent(handler, EnumScriptType.FALLEN_UPON, event)) { return 0.0f; }
		return event.distanceFallen;
	}

	public static boolean onScriptBlockHarvest(IScriptBlockHandler handler, EntityPlayer player) {
		if (handler.isClient()) { return false; }
		return onEvent(handler, EnumScriptType.HARVESTED, new BlockEvent.HarvestedEvent(handler.getBlock(), player));
	}

	public static void onScriptBlockInit(IScriptBlockHandler handler) {
		if (handler.isClient()) { return; }
		onEvent(handler, EnumScriptType.INIT, new BlockEvent.InitEvent(handler.getBlock()));
	}

	public static boolean onScriptBlockInteract(IScriptBlockHandler handler, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (handler.isClient()) { return false; }
		return onEvent(handler, EnumScriptType.INTERACT, new BlockEvent.InteractEvent(handler.getBlock(), player, side, hitX, hitY, hitZ));
	}

	public static void onScriptBlockNeighborChanged(IScriptBlockHandler handler, BlockPos changedPos) {
		if (handler.isClient()) { return; }
		onEvent(handler, EnumScriptType.NEIGHBOR_CHANGED,
				new BlockEvent.NeighborChangedEvent(handler.getBlock(),
						new BlockPosWrapper(handler.getBlock() == null ? null : handler.getBlock().getWorld().getMCWorld(), changedPos)));
	}

	public static void onScriptBlockRainFill(IScriptBlockHandler handler) {
		if (handler.isClient()) { return; }
		onEvent(handler, EnumScriptType.RAIN_FILLED, new BlockEvent.RainFillEvent(handler.getBlock()));
	}

	public static void onScriptBlockRedstonePower(IScriptBlockHandler handler, int prevPower, int power) {
		if (handler.isClient()) { return; }
		onEvent(handler, EnumScriptType.REDSTONE, new BlockEvent.RedstoneEvent(handler.getBlock(), prevPower, power));
	}

	public static void onScriptBlockTimer(IScriptBlockHandler handler, int id) {
		if (handler.isClient()) { return; }
		onEvent(handler, EnumScriptType.TIMER, new BlockEvent.TimerEvent(handler.getBlock(), id));
	}

	public static void onScriptBlockUpdate(IScriptBlockHandler handler) {
		if (handler.isClient()) {
			onEvent(ScriptController.Instance.clientScripts, EnumScriptType.TICK, new BlockEvent.UpdateEvent(handler.getBlock()));
			return;
		}
		onEvent(handler, EnumScriptType.TICK, new BlockEvent.UpdateEvent(handler.getBlock()));
	}

	public static boolean onScriptItemAttack(ItemScriptedWrapper handler, ItemEvent.AttackEvent event) {
		return onEvent(handler, EnumScriptType.ATTACK, event);
	}

	public static void onScriptItemInit(ItemScriptedWrapper handler) {
		if (handler.isClient()) { return; }
		onEvent(handler, EnumScriptType.INIT, new ItemEvent.InitEvent(handler));
	}

	public static boolean onScriptItemInteract(ItemScriptedWrapper handler, ItemEvent.InteractEvent event) {
		return onEvent(handler, EnumScriptType.INTERACT, event);
	}

	public static void onScriptItemPickedUp(ItemScriptedWrapper handler, EntityPlayer player, EntityItem entity) {
		if (handler.isClient()) { return; }
		onEvent(handler, EnumScriptType.PICKEDUP, new ItemEvent.PickedUpEvent(handler,
				PlayerData.get(player).getIPlayer(), (IEntityItem<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity)));
	}

	public static boolean onScriptItemSpawn(ItemScriptedWrapper handler, EntityItem entity) {
		if (handler.isClient()) { return false; }
		return onEvent(handler, EnumScriptType.SPAWN, new ItemEvent.SpawnEvent(handler, (IEntityItem<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity)));
	}

	public static boolean onScriptItemTossed(ItemScriptedWrapper handler, EntityPlayer player, EntityItem entity) {
		if (handler.isClient()) { return false; }
		return onEvent(handler, EnumScriptType.TOSSED, new ItemEvent.TossedEvent(handler,
				PlayerData.get(player).getIPlayer(), (IEntityItem<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity)));
	}

	public static void onScriptItemUpdate(ItemScriptedWrapper handler, EntityPlayer player) {
		if (handler.isClient()) {
			onEvent(ScriptController.Instance.clientScripts, EnumScriptType.TICK, new ItemEvent.UpdateEvent(handler, PlayerData.get(player).getIPlayer()));
			return;
		}
		onEvent(handler, EnumScriptType.TICK, new ItemEvent.UpdateEvent(handler, PlayerData.get(player).getIPlayer()));
	}

	public static void onScriptTriggerEvent(int id, IWorld level, IPos pos, IEntity<?> entity, Object[] arguments) {
		ScriptTriggerEvent event = new ScriptTriggerEvent(id, level, pos, entity, arguments);
		if (event.entity != null && event.world != null && !(event.entity.getMCEntity() instanceof FakePlayer)) {
			if (event.entity.getType() == 1) {
				onEvent(getPlayerHandler(), EnumScriptType.SCRIPT_TRIGGER, event);
			} else if (event.entity.getType() == 2) {
				onEvent(((EntityNPCInterface) event.entity.getMCEntity()).script, EnumScriptType.SCRIPT_TRIGGER, event);
			} else {
				TileEntity tile = event.world.getMCWorld().getTileEntity(event.pos.getMCBlockPos());
				if (tile instanceof IScriptBlockHandler) {
					onEvent((IScriptBlockHandler) tile, EnumScriptType.SCRIPT_TRIGGER, event);
				}
			}
		}
		if (ScriptController.Instance.forgeScripts.isClient()) {
			onEvent(ScriptController.Instance.clientScripts, EnumScriptType.SCRIPT_TRIGGER, event);
		} else {
			onEvent(ScriptController.Instance.forgeScripts, EnumScriptType.SCRIPT_TRIGGER, event);
		}
	}

	public static void onScriptTriggerEvent(IScriptHandler handler, int id, IWorld world, IPos pos, IEntity<?> entity, Object... arguments) {
		ScriptTriggerEvent event = new ScriptTriggerEvent(id, world, pos, entity, arguments);
		onEvent(handler, EnumScriptType.SCRIPT_TRIGGER, event);
	}

	public static void onWorldScriptEvent(noppes.npcs.api.event.WorldEvent.ScriptCommandEvent event) {
		onEvent(ScriptController.Instance.playerScripts, EnumScriptType.SCRIPT_COMMAND, event);
	}

	public static void onCustomGuiKeyPressed(PlayerWrapper<?> player, ICustomGui gui, int keyId) {
		CustomGuiController.onKeyPressed(new CustomGuiEvent.KeyPressedEvent(player, gui, keyId));
	}

	public static void onPackageReceived(PackageReceived event, boolean isServerSide) {
		onEvent(isServerSide ?
				ScriptController.Instance.forgeScripts :
				ScriptController.Instance.clientScripts, EnumScriptType.PACKAGE_RECEIVED, event);
	}

}
