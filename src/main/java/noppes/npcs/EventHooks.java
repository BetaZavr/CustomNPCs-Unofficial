package noppes.npcs;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.*;
import noppes.npcs.api.event.*;
import noppes.npcs.api.gui.IButton;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IItemSlot;
import noppes.npcs.api.gui.IScroll;
import noppes.npcs.api.handler.IFactionHandler;
import noppes.npcs.api.handler.IRecipeHandler;
import noppes.npcs.api.handler.data.IKeySetting;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.api.wrapper.PlayerWrapper;
import noppes.npcs.api.wrapper.WrapperNpcAPI;
import noppes.npcs.api.wrapper.gui.CustomGuiWrapper;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.containers.ContainerNPCBank;
import noppes.npcs.controllers.CustomGuiController;
import noppes.npcs.controllers.IScriptBlockHandler;
import noppes.npcs.controllers.IScriptHandler;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityProjectile;
import noppes.npcs.entity.data.DataScript;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketBankSetPlayer;
import noppes.npcs.shared.common.util.LogWriter;
import org.apache.commons.lang3.StringUtils;

public class EventHooks {

   private static final Map<String, Long> clientMap = new HashMap<>();

   public static boolean onNPCAttacksMelee(EntityNPCInterface npc, NpcEvent.MeleeAttackEvent event) {
      if (npc.script.isClient()) { return false; }
      return onEvent(npc.script, EnumScriptType.ATTACK_MELEE, event);
   }

   public static void onNPCRangedLaunched(EntityNPCInterface npc, NpcEvent.RangedLaunchedEvent event) {
      if (npc.script.isClient()) { return; }
      onEvent(npc.script, EnumScriptType.RANGED_LAUNCHED, event);
   }

   public static boolean onNPCTarget(EntityNPCInterface npc, NpcEvent.TargetEvent event) {
      if (npc.script.isClient()) { return false; }
      return onEvent(npc.script, EnumScriptType.TARGET, event);
   }

   public static boolean onNPCTargetLost(EntityNPCInterface npc, LivingEntity prevtarget) {
      if (npc.script.isClient()) { return false; }
      return onEvent(npc.script, EnumScriptType.TARGET_LOST,  new NpcEvent.TargetLostEvent(npc.wrappedNPC, prevtarget));
   }

   public static boolean onNPCInteract(EntityNPCInterface npc, Player player) {
      if (npc.script.isClient()) { return false; }
      NpcEvent.InteractEvent event = new NpcEvent.InteractEvent(npc.wrappedNPC, player);
      event.setCanceled(npc.isAttacking() || npc.isKilled() || npc.faction.isAggressiveToPlayer(player));
      return onEvent(npc.script, EnumScriptType.INTERACT, event);
   }

   public static boolean onNPCDamaged(EntityNPCInterface npc, NpcEvent.DamagedEvent event) {
      if (npc.script.isClient()) { return false; }
      event.setCanceled(npc.isKilled());
      return onEvent(npc.script, EnumScriptType.DAMAGED, event);
   }

   public static void onNPCInit(EntityNPCInterface npc) {
      if (npc.script.isClient()) { return; }
      onEvent(npc.script, EnumScriptType.INIT, new NpcEvent.InitEvent(npc.wrappedNPC));
   }

   public static void onNPCCollide(EntityNPCInterface npc, Entity entity) {
      if (npc.script.isClient()) { return; }
      onEvent(npc.script, EnumScriptType.COLLIDE, new NpcEvent.CollideEvent(npc.wrappedNPC, entity));
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

   public static void onNPCDied(EntityNPCInterface npc, NpcEvent.DiedEvent event) {
      if (npc.script.isClient()) { return; }
      onEvent(npc.script, EnumScriptType.DIED, event);
   }

   public static boolean onNPCDialogOption(EntityNPCInterface npc, ServerPlayer player, Dialog dialog, DialogOption option) {
      if (npc.script.isClient()) { return false; }
      DialogEvent.OptionEvent event = new DialogEvent.OptionEvent(npc.wrappedNPC, player, dialog, option);
      if (!(npc instanceof EntityDialogNpc)) { onEvent(npc.script, EnumScriptType.DIALOG_OPTION, event); }
      return onEvent(PlayerData.get(player).scriptData, EnumScriptType.DIALOG_OPTION, event);
   }

   public static boolean onNPCDialog(EntityNPCInterface npc, Player player, Dialog dialog) {
      if (npc.script.isClient()) { return false; }
      DialogEvent.OpenEvent event = new DialogEvent.OpenEvent(npc.wrappedNPC, player, dialog);
      if (!(npc instanceof EntityDialogNpc)) { onEvent(npc.script, EnumScriptType.DIALOG, event); }
      return onEvent(PlayerData.get(player).scriptData, EnumScriptType.DIALOG, event);
   }

   public static void onNPCDialogClose(EntityNPCInterface npc, ServerPlayer player, Dialog dialog) {
      if (npc.script.isClient()) { return; }
      DialogEvent.CloseEvent event = new DialogEvent.CloseEvent(npc.wrappedNPC, player, dialog);
      if (!(npc instanceof EntityDialogNpc)) { onEvent(npc.script, EnumScriptType.DIALOG_CLOSE, event); }
      onEvent(PlayerData.get(player).scriptData, EnumScriptType.DIALOG_CLOSE, event);
   }

   public static void onNPCKills(EntityNPCInterface npc, LivingEntity entityLiving) {
      if (npc.script.isClient()) { return; }
      onEvent(npc.script, EnumScriptType.KILL, new NpcEvent.KilledEntityEvent(npc.wrappedNPC, entityLiving));
   }

   public static boolean onNPCRole(EntityNPCInterface npc, RoleEvent event) {
      if (npc.script.isClient()) { return false; }
      return onEvent(npc.script, EnumScriptType.ROLE, event);
   }

   public static void onNPCTimer(EntityNPCInterface npc, int id) {
      onEvent(npc.script, EnumScriptType.TIMER, new NpcEvent.TimerEvent(npc.wrappedNPC, id));
   }

   public static boolean onScriptBlockInteract(IScriptBlockHandler handler, Player player, int side, float hitX, float hitY, float hitZ) {
      if (handler.isClient()) { return false; }
      return onEvent(handler, EnumScriptType.INTERACT, new BlockEvent.InteractEvent(handler.getBlock(), player, side, hitX, hitY, hitZ));
   }

   public static void onScriptBlockCollide(IScriptBlockHandler handler, Entity entityIn) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.COLLIDE, new BlockEvent.CollidedEvent(handler.getBlock(), entityIn));
   }

   public static void onScriptBlockRainFill(IScriptBlockHandler handler) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.RAIN_FILLED, new BlockEvent.RainFillEvent(handler.getBlock()));
   }

   public static float onScriptBlockFallenUpon(IScriptBlockHandler handler, Entity entity, float distance) {
      if (handler.isClient()) { return distance; }
      BlockEvent.EntityFallenUponEvent event = new BlockEvent.EntityFallenUponEvent(handler.getBlock(), entity, distance);
      if (onEvent(handler, EnumScriptType.FALLEN_UPON, event)) { return 0.0f; }
      return event.distanceFallen;
   }

   public static void onScriptBlockClicked(IScriptBlockHandler handler, Player player) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.CLICKED, new BlockEvent.ClickedEvent(handler.getBlock(), player));
   }

   public static void onScriptBlockBreak(IScriptBlockHandler handler) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.BROKEN, new BlockEvent.BreakEvent(handler.getBlock()));
   }

   public static boolean onScriptBlockHarvest(IScriptBlockHandler handler, Player player) {
      if (handler.isClient()) { return false; }
      return onEvent(handler, EnumScriptType.HARVESTED, new BlockEvent.HarvestedEvent(handler.getBlock(), player));
   }

   public static boolean onScriptBlockExploded(IScriptBlockHandler handler) {
      if (handler.isClient()) { return false; }
      return onEvent(handler, EnumScriptType.EXPLODED, new BlockEvent.ExplodedEvent(handler.getBlock()));
   }

   public static void onScriptBlockNeighborChanged(IScriptBlockHandler handler, BlockPos changedPos) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.NEIGHBOR_CHANGED,
              new BlockEvent.NeighborChangedEvent(handler.getBlock(),
                      new BlockPosWrapper(handler.getBlock() == null ? null : handler.getBlock().getWorld().getMCLevel(), changedPos)));
   }

   public static void onScriptBlockRedstonePower(IScriptBlockHandler handler, int prevPower, int power) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.REDSTONE, new BlockEvent.RedstoneEvent(handler.getBlock(), prevPower, power));
   }

   public static void onScriptBlockInit(IScriptBlockHandler handler) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.INIT, new BlockEvent.InitEvent(handler.getBlock()));
   }

   public static void onScriptBlockUpdate(IScriptBlockHandler handler) {
      if (handler.isClient()) {
         onEvent(ScriptController.Instance.clientScripts, EnumScriptType.TICK, new BlockEvent.UpdateEvent(handler.getBlock()));
         return;
      }
      onEvent(handler, EnumScriptType.TICK, new BlockEvent.UpdateEvent(handler.getBlock()));
   }

   public static boolean onScriptBlockDoorToggle(IScriptBlockHandler handler) {
      if (handler.isClient()) { return false; }
      return onEvent(handler, EnumScriptType.DOOR_TOGGLE, new BlockEvent.DoorToggleEvent(handler.getBlock()));
   }

   public static void onScriptBlockTimer(IScriptBlockHandler handler, int id) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.TIMER, new BlockEvent.TimerEvent(handler.getBlock(), id));
   }

   public static void onGlobalRecipesLoaded(IRecipeHandler handler) {
      HandlerEvent.RecipesLoadedEvent event = new HandlerEvent.RecipesLoadedEvent(handler);
      WrapperNpcAPI.EVENT_BUS.post(event);
   }

   public static void onGlobalFactionsLoaded(IFactionHandler handler) {
      HandlerEvent.FactionsLoadedEvent event = new HandlerEvent.FactionsLoadedEvent(handler);
      WrapperNpcAPI.EVENT_BUS.post(event);
   }

   public static void onPlayerInit(PlayerScriptData handler) {
      onEvent(handler, EnumScriptType.INIT, new PlayerEvent.InitEvent(handler.getPlayer()));
   }

   public static void onPlayerTick(PlayerScriptData handler) {
      if (handler.isClient()) {
         onEvent(ScriptController.Instance.clientScripts, EnumScriptType.TICK, new PlayerEvent.UpdateEvent(handler.getPlayer()));
         return;
      }
      onEvent(handler, EnumScriptType.TICK, new PlayerEvent.UpdateEvent(handler.getPlayer()));
   }

   public static boolean onPlayerInteract(PlayerScriptData handler, PlayerEvent.InteractEvent event) {
      return onEvent(handler, EnumScriptType.INTERACT, event);
   }

   public static boolean onPlayerAttack(PlayerScriptData handler, PlayerEvent.AttackEvent event) {
      return onEvent(handler, EnumScriptType.ATTACK, event);
   }

   public static boolean onPlayerBreak(PlayerScriptData handler, PlayerEvent.BreakEvent event) {
      return onEvent(handler, EnumScriptType.BROKEN, event);
   }

   public static boolean onPlayerToss(PlayerScriptData handler, ItemEntity entityItem) {
      return onEvent(handler, EnumScriptType.TOSS, new PlayerEvent.TossEvent(handler.getPlayer(), Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(entityItem.getItem())));
   }

   public static void onPlayerLevelUp(PlayerScriptData handler, int change) {
      onEvent(handler, EnumScriptType.LEVEL_UP, new PlayerEvent.LevelUpEvent(handler.getPlayer(), change));
   }

   public static boolean onPlayerPickUp(PlayerScriptData handler, ItemEntity entityItem) {
      return onEvent(handler, EnumScriptType.PICKUP, new PlayerEvent.PickUpEvent(handler.getPlayer(),
              Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(entityItem.getItem())));
   }

   public static void onPlayerContainerOpen(PlayerScriptData handler, AbstractContainerMenu container) {
      onEvent(handler, EnumScriptType.CONTAINER_OPEN, new PlayerEvent.ContainerOpen(handler.getPlayer(), Objects.requireNonNull(NpcAPI.Instance()).getIContainer(container)));
   }

   public static void onPlayerContainerClose(PlayerScriptData handler, AbstractContainerMenu container) {
      onEvent(handler, EnumScriptType.CONTAINER_CLOSED, new PlayerEvent.ContainerClosed(handler.getPlayer(), Objects.requireNonNull(NpcAPI.Instance()).getIContainer(container)));
   }

   public static void onPlayerDeath(PlayerScriptData handler, DamageSource source, Entity entity) {
      onEvent(handler, EnumScriptType.DIED, new PlayerEvent.DiedEvent(handler.getPlayer(), source, entity));
   }

   public static void onPlayerKills(PlayerScriptData handler, LivingEntity entityLiving) {
      onEvent(handler, EnumScriptType.KILL, new PlayerEvent.KilledEntityEvent(handler.getPlayer(), entityLiving));
   }

   public static void onPlayerTimer(PlayerData data, int id) {
      onEvent(data.scriptData, EnumScriptType.TIMER, new PlayerEvent.TimerEvent(data.scriptData.getPlayer(), id));
   }

   public static boolean onPlayerDamaged(PlayerScriptData handler, PlayerEvent.DamagedEvent event) {
      return onEvent(handler, EnumScriptType.DAMAGED, event);
   }

   public static void onPlayerLogin(PlayerScriptData handler) {
      onEvent(handler, EnumScriptType.LOGIN, new PlayerEvent.LoginEvent(handler.getPlayer()));
   }

   public static void onPlayerLogout(PlayerScriptData handler) {
      onEvent(handler, EnumScriptType.LOGOUT, new PlayerEvent.LogoutEvent(handler.getPlayer()));
   }

   public static void onPlayerChat(PlayerScriptData handler, PlayerEvent.ChatEvent event) {
      onEvent(handler, EnumScriptType.CHAT, event);
   }

   public static boolean onPlayerRanged(PlayerScriptData handler, PlayerEvent.RangedLaunchedEvent event) {
      return onEvent(handler, EnumScriptType.RANGED_LAUNCHED, event);
   }

   public static boolean onPlayerDamagedEntity(PlayerScriptData handler, PlayerEvent.DamagedEntityEvent event) {
      return onEvent(handler, EnumScriptType.DAMAGED_ENTITY, event);
   }

   public static void onPlayerFactionChange(PlayerScriptData handler, PlayerEvent.FactionUpdateEvent event) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.FACTION_UPDATE, event);
   }

   public static void onPlayerKeyEvent(ServerPlayer player, int button, boolean isCtrlPressed, boolean isShiftPressed, boolean isAltPressed, boolean isMetaPressed, boolean pressed, String openGui) {
      PlayerScriptData handler = PlayerData.get(player).scriptData;
      Event event = new PlayerEvent.KeyPressedEvent(handler.getPlayer(), button, isCtrlPressed, isAltPressed, isShiftPressed, isMetaPressed, openGui);
      onEvent(handler, pressed ? EnumScriptType.KEY_PRESSED : EnumScriptType.KEY_RELEASED, event);
   }

   public static void onPlayerMouseEvent(ServerPlayer player, int button, boolean isDown, double scrolledIn,
                                         boolean isCtrlPressed, boolean isShiftPressed, boolean isAltPressed, boolean isMetaPressed,
                                         String openGui) {
      if (CustomNpcs.EnableScripting && !ScriptController.Instance.languages.isEmpty()) { return;}
      PlayerScriptData handler = PlayerData.get(player).scriptData;
      Event event = new PlayerEvent.KeyPressedEvent(handler.getPlayer(), button, isCtrlPressed, isAltPressed, isShiftPressed, isMetaPressed, openGui);
      onEvent(handler, scrolledIn != 0.0d ? EnumScriptType.MOUSE_SCROLLED :
              isDown ? EnumScriptType.MOUSE_PRESSED : EnumScriptType.MOUSE_RELEASED, event);
   }

   public static void onPlayerKeyActive(ServerPlayer player, int id) {
      if (player == null) { return; }
      IKeySetting kb = Objects.requireNonNull(NpcAPI.Instance()).getIKeyBinding().getKeySetting(id);
      if (kb != null) {
         PlayerScriptData handler = PlayerData.get(player).scriptData;
         if (handler.getEnabled()) {
            onEvent(handler, EnumScriptType.KEY_ACTIVE, new PlayerEvent.KeyActive(handler.getPlayer(), kb));
         }
      }
   }

   public static void onForgeInit(ForgeScriptData handler) {
      onEvent(handler, EnumScriptType.INIT, new ForgeEvent.InitEvent());
   }

   public static void onForgeEvent(ForgeEvent ev) {
      ForgeScriptData handler = ScriptController.Instance.forgeScripts;
      String eventName;
      if (!handler.isClient() && handler.isEnabled()) {
         if (!ForgeEventHandler.eventNames.containsKey(ev.event.getClass())) {
            eventName = ev.event.getClass().getName();
            int i = eventName.lastIndexOf(".");
            eventName = StringUtils.uncapitalize(eventName.substring(i + 1).replace("$", ""));
            ForgeEventHandler.eventNames.put(ev.event.getClass(), eventName);
            LogWriter.info("Found new Forge Event \"" + eventName + "\" to event: "+ev.event.getClass().getName());
         }
         else { eventName = ForgeEventHandler.eventNames.get(ev.event.getClass()); }
         try {
            handler.runScript(eventName, ev);
            if (ev.isCanceled() && ev.event.isCancelable()) {
               ev.event.setCanceled(true);
            }
            WrapperNpcAPI.EVENT_BUS.post(ev.event);
            if (ev.isCancelable()) { ev.setCanceled(ev.event.isCanceled()); }
         } catch (Exception e) {
            LogWriter.error("Error:", e);
         }
      }
      if (handler.isClient()) {
         ClientScriptData handlerClient = ScriptController.Instance.clientScripts;
         if (!handlerClient.isClient() || !handlerClient.isEnabled()) { return; }
         if (!ForgeEventHandler.clientEventNames.containsKey(ev.event.getClass())) {
            eventName = ev.event.getClass().getName();
            int i = eventName.lastIndexOf(".");
            eventName = StringUtils.uncapitalize(eventName.substring(i + 1).replace("$", ""));
            ForgeEventHandler.clientEventNames.put(ev.event.getClass(), eventName);
            LogWriter.info("Found new Forge Event \"" + eventName + "\" to event: "+ev.event.getClass().getName());
         } else {
            eventName = ForgeEventHandler.clientEventNames.get(ev.event.getClass());
         }
         if (eventName.isEmpty() || (clientMap.containsKey(eventName) && clientMap.get(eventName) == System.currentTimeMillis())) {
            return;
         }
         clientMap.put(eventName, System.currentTimeMillis());
         try {
            handlerClient.runScript(eventName, ev);
            if (ev.isCanceled() && ev.event.isCancelable()) { ev.event.setCanceled(true); }
            WrapperNpcAPI.EVENT_BUS.post(ev.event);
         } catch (Exception e) { LogWriter.error("Error:", e); }
      }
   }

   public static boolean onQuestStarted(PlayerScriptData handler, Quest quest) {
      if (handler.isClient()) { return false; }
      return onEvent(handler, EnumScriptType.QUEST_START,  new QuestEvent.QuestStartEvent(handler.getPlayer(), quest));
   }

   public static void onQuestFinished(PlayerScriptData handler, Quest quest) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.QUEST_COMPLETED,  new QuestEvent.QuestCompletedEvent(handler.getPlayer(), quest));
   }

   public static void onQuestTurnedIn(PlayerScriptData handler, QuestEvent.QuestTurnedInEvent event) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.QUEST_TURNING, event);
   }

   public static void onScriptItemInit(ItemScriptedWrapper handler) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.INIT, new ItemEvent.InitEvent(handler));
   }

   public static void onScriptItemUpdate(ItemScriptedWrapper handler, Player player) {
      if (handler.isClient()) {
         onEvent(ScriptController.Instance.clientScripts, EnumScriptType.TICK, new ItemEvent.UpdateEvent(handler, PlayerData.get(player).scriptData.getPlayer()));
         return;
      }
      onEvent(handler, EnumScriptType.TICK, new ItemEvent.UpdateEvent(handler, PlayerData.get(player).scriptData.getPlayer()));
   }

   public static boolean onScriptItemTossed(ItemScriptedWrapper handler, Player player, ItemEntity entity) {
      if (handler.isClient()) { return false; }
      return onEvent(handler, EnumScriptType.TOSSED, new ItemEvent.TossedEvent(handler,
              PlayerData.get(player).scriptData.getPlayer(), (IEntityItem<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity)));
   }

   public static void onScriptItemPickedUp(ItemScriptedWrapper handler, Player player, ItemEntity entity) {
      if (handler.isClient()) { return; }
      onEvent(handler, EnumScriptType.PICKEDUP, new ItemEvent.PickedUpEvent(handler,
              PlayerData.get(player).scriptData.getPlayer(), (IEntityItem<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity)));
   }

   public static boolean onScriptItemSpawn(ItemScriptedWrapper handler, ItemEntity entity) {
      if (handler.isClient()) { return false; }
      return onEvent(handler, EnumScriptType.SPAWN, new ItemEvent.SpawnEvent(handler, (IEntityItem<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity)));
   }

   public static boolean onScriptItemInteract(ItemScriptedWrapper handler, ItemEvent.InteractEvent event) {
      return onEvent(handler, EnumScriptType.INTERACT, event);
   }

   public static boolean onScriptItemAttack(ItemScriptedWrapper handler, ItemEvent.AttackEvent event) {
      return onEvent(handler, EnumScriptType.ATTACK, event);
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

   public static void onProjectileImpact(EntityProjectile projectile, ProjectileEvent.ImpactEvent event) {
      for (ScriptContainer script : projectile.scripts) {
         if (script.isValid()) {
            script.run(EnumScriptType.PROJECTILE_IMPACT.function, event);
         }
      }
      WrapperNpcAPI.EVENT_BUS.post(event);
   }

   public static void onScriptTriggerEvent(int id, IWorld level, IPos pos, IEntity<?> entity, Object[] arguments) {
      WorldEvent.ScriptTriggerEvent event = new WorldEvent.ScriptTriggerEvent(id, level, pos, entity, arguments);
      if (event.entity != null && event.world != null && !(event.entity.getMCEntity() instanceof FakePlayer)) {
         if (event.entity.getType() == 1) {
            onEvent(PlayerData.get((Player) event.entity.getMCEntity()).scriptData, EnumScriptType.SCRIPT_TRIGGER, event);
         } else if (event.entity.getType() == 2) {
            onEvent(((EntityNPCInterface) event.entity.getMCEntity()).script, EnumScriptType.SCRIPT_TRIGGER, event);
         } else {
            BlockEntity tile = event.world.getMCLevel().getBlockEntity(event.pos.getMCBlockPos());
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

   public static void onScriptTriggerEvent(IScriptHandler handler, int id, IWorld level, IPos pos, IEntity<?> entity, Object[] arguments) {
      WorldEvent.ScriptTriggerEvent event = new WorldEvent.ScriptTriggerEvent(id, level, pos, entity, arguments);
      onEvent(handler, EnumScriptType.SCRIPT_TRIGGER, event);
   }

   public static void onCustomGuiButton(PlayerWrapper<?> player, ICustomGui gui, IButton button) {
      CustomGuiController.onButton(new CustomGuiEvent.ButtonEvent(player, gui, button));
   }

   public static void onCustomGuiSlot(PlayerWrapper<?> player, ICustomGui gui, IItemSlot slot, IItemStack heldItem) {
      CustomGuiController.onQuickCraft(new CustomGuiEvent.SlotEvent(player, gui, slot, heldItem));
   }

   public static void onCustomGuiScrollClick(PlayerWrapper<?> player, ICustomGui gui, IScroll scroll, int scrollIndex, String[] selection, boolean doubleClick) {
      CustomGuiController.onScrollClick(new CustomGuiEvent.ScrollEvent(player, gui, scroll, scrollIndex, selection, doubleClick));
   }

   public static void onCustomGuiClose(PlayerWrapper<?> player, ICustomGui gui) {
      CustomGuiController.onClose(new CustomGuiEvent.CloseEvent(player, gui));
   }

   public static boolean onCustomGuiSlotClicked(PlayerWrapper<?> player, ICustomGui gui, IItemSlot slot, int dragType, String clickType, IItemStack heldItem) {
      return CustomGuiController.onSlotClick(new CustomGuiEvent.SlotClickEvent(player, gui, slot, heldItem, dragType, clickType));
   }

   // New from Unofficial (BetaZavr)
   public static void onNPCsInit(NpcScriptData handler) {
      onEvent(handler, EnumScriptType.INIT, new NpcEvent.InitEvent(null));
   }

   public static void onNPCNeedBlockDamage(EntityNPCInterface npc, NpcEvent.NeedBlockDamage event) {
      if (npc.script.isClient()) { return; }
      onEvent(npc.script, EnumScriptType.NEED_BLOCK_DAMAGED, event);
   }

   public static void onPlayerScreen(ServerPlayer player, String newGUI, String oldGUI) {
      if (player == null) { return; }
      if (newGUI.equals("GuiNPCBankChest") && oldGUI.equals("GuiIngame")) {
         ContainerNPCBank.editPlayerBankData = null;
         Packets.send(player, new PacketBankSetPlayer(""));
      }
      PlayerData data = PlayerData.get(player);
      data.overlay.currentGUI = newGUI;
      if (!data.scriptData.getEnabled()) { return; }
      onEvent(data.scriptData, EnumScriptType.GUI_OPEN, new PlayerEvent.OpenGUI(data.scriptData.getPlayer(), newGUI, oldGUI));
   }

   public static void onPotionInit(PotionScriptData handler) {
      onEvent(handler, EnumScriptType.INIT, new ForgeEvent.InitEvent());
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

   @SuppressWarnings("unused")
   private static void onEvent(ScriptContainer script, EnumScriptType enumFunction, Event event) {
      if (script != null && event != null && enumFunction != null) {
         script.run(enumFunction.function, event);
         WrapperNpcAPI.EVENT_BUS.post(event);
      }
   }

   public static void onWorldScriptEvent(WorldEvent.ScriptCommandEvent event) {
      onEvent(ScriptController.Instance.playerScripts, EnumScriptType.SCRIPT_COMMAND, event);
   }

   public static boolean onQuestCanceled(PlayerScriptData handler, Quest quest) {
      if (handler.isClient()) {
         return false;
      }
      return onEvent(handler, EnumScriptType.QUEST_CANCELED,
              new QuestEvent.QuestCanceledEvent(handler.getPlayer(), quest));
   }

   public static PlayerEvent.CustomTeleport onPlayerTeleport(ServerPlayer player, BlockPos to, BlockPos portal, ResourceKey<Level> dimId) {
       NpcAPI api = NpcAPI.Instance();
       if (api != null) {
          PlayerEvent.CustomTeleport event = new PlayerEvent.CustomTeleport((IPlayer<?>) api.getIEntity(player), api.getIPos(portal), api.getIPos(to), dimId);
          if (player != null) {
             PlayerScriptData handler = PlayerData.get(player).scriptData;
             if (handler.getEnabled()) { onEvent(handler, EnumScriptType.CUSTOM_TELEPORT, event); }
          }
       }
       return new PlayerEvent.CustomTeleport(null,
               new BlockPosWrapper(player == null ? null : player.level(), portal),
               new BlockPosWrapper(player == null ? null : player.level(), to),
               dimId);
    }

   public static NpcEvent.CustomNpcTeleport onNpcTeleport(EntityNPCInterface npc, BlockPos portal, BlockPos to, ResourceKey<Level> dimId) {
      NpcAPI api = NpcAPI.Instance();
      if (api != null) {
         NpcEvent.CustomNpcTeleport event = new NpcEvent.CustomNpcTeleport(npc == null ? null : npc.wrappedNPC, api.getIPos(portal), api.getIPos(to), dimId);
         if (npc != null) {
            DataScript handler = npc.script;
            if (!handler.getEnabled()) { onEvent(handler, EnumScriptType.CUSTOM_TELEPORT, event); }
         }
         return event;
      }
      return new NpcEvent.CustomNpcTeleport(npc == null ? null : npc.wrappedNPC,
              new BlockPosWrapper(npc == null ? null : npc.level(), portal),
              new BlockPosWrapper(npc == null ? null : npc.level(), to),
              dimId);
   }

   public static boolean onPlayerPlace(PlayerScriptData handler, PlayerEvent.PlaceEvent event) {
      return onEvent(handler, EnumScriptType.PLEASED, event);
   }

   public static void onCustomGuiKeyPressed(PlayerWrapper<?> player, CustomGuiWrapper gui, int keyId) {
      CustomGuiController.onKeyPressed(new CustomGuiEvent.KeyPressedEvent(player, gui, keyId));
   }

   public static void onPackageReceived(PackageReceived event, boolean isServerSide) {
      onEvent(isServerSide ?
              ScriptController.Instance.forgeScripts :
              ScriptController.Instance.clientScripts, EnumScriptType.PACKAGE_RECEIVED, event);
   }

}
