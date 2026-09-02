package noppes.npcs;

import com.google.common.util.concurrent.ListenableFutureTask;
import com.mojang.brigadier.context.CommandContext;

import java.util.*;
import java.util.concurrent.Executors;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.SaveToFile;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerEvent.StopTracking;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.api.wrapper.WrapperEntityData;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.controllers.VisibilityController;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemSoulstoneEmpty;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.packets.client.PacketGuiCloneOpen;
import noppes.npcs.packets.client.PacketGuiOpen;
import noppes.npcs.packets.client.PacketMarkData;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.util.CustomNPCsScheduler;

public class ServerEventsHandler {

   private void doFactionPoints(Player player, EntityNPCInterface npc) { npc.advanced.factions.addPoints(player); }

   // Change from Unofficial (BetaZavr)
   private void doKillQuest(Player player, LivingEntity entity, boolean forAll) {
      PlayerData pdata = PlayerData.get(player);
      PlayerQuestData playerQuestData = pdata.questData;
      String entityName = entity.getClass().getSimpleName();
      if (entity instanceof Player) { entityName = "Player"; }
      // found in quests
      for (QuestData questData : new ArrayList<>(playerQuestData.activeQuests.values())) {
         // check quest step
         if (questData.quest.step == 2 && questData.quest.questInterface.isCompleted(player)) { continue; }
         boolean bo = questData.quest.step == 1;
         // found tacks
         for (QuestObjective questObjective : questData.quest.getObjectives(player)) {
            if (questData.quest.step == 1 && !bo) { break; }
            bo = questObjective.isCompleted();
            // dimension
            if (!questObjective.dimension.toString().equals("minecraft:any") && !player.level().dimension().location().equals(questObjective.dimension)) { continue; }
            // kill only
            if (questObjective.getEnumType() != EnumQuestTask.KILL && questObjective.getEnumType() != EnumQuestTask.AREAKILL) { continue; }
            // get real name
            String objectiveName = null;
            if (questObjective.getTargetName().equals(entity.getName().getString())) { objectiveName = entity.getName().getString(); } // entity name
            else if (questObjective.getTargetName().equals(entityName)) { objectiveName = entityName; } // entity type
            else if (questObjective.isPartName() || questObjective.isAndTitle()) { // part or title -> name
               if (questObjective.isPartName()) {
                  if (entity.getName().getString().contains(questObjective.getTargetName())) { objectiveName = questObjective.getTargetName(); } // part in entity name
                  else if (entityName.contains(questObjective.getTargetName())) { objectiveName = questObjective.getTargetName(); } // part in entity type
               }
               // part in npc title
               if (objectiveName == null && questObjective.isAndTitle() && entity instanceof EntityNPCInterface npc) {
                  String title = npc.display.getTitle();
                  if (title.equals(questObjective.getTargetName())) { objectiveName = entity.getName().getString(); }
                  else if (title.equals(entityName)) { objectiveName = entityName; }
                  if (objectiveName == null && questObjective.isPartName()) {
                     if (title.contains(questObjective.getTargetName())) { objectiveName = questObjective.getTargetName(); }
                     else if (title.contains(questObjective.getTargetName())) { objectiveName = questObjective.getTargetName(); }
                  }
               }
            }
            else { continue; }
            if (objectiveName == null) { continue; }
            // search players around
            if (questObjective.getType() == EnumQuestTask.AREAKILL.ordinal() && forAll) {
               int range = questObjective.getAreaRange();
               List<Player> list = player.level().getEntitiesOfClass(Player.class, entity.getBoundingBox().inflate(range, range, range));
               for (Player pl : list) {
                  if (pl != player && pl.isAlive()) { doKillQuest(pl, entity, false); }
               }
            }
            HashMap<String, Integer> killed = questObjective.getKilled(questData); // in Data
            if (killed.containsKey(objectiveName) && killed.get(objectiveName) >= questObjective.getMaxProgress()) { continue; } // is complete
            // add progress
            int amount = 0;
            if (killed.containsKey(objectiveName)) { amount = killed.get(objectiveName); }
            amount++;
            killed.put(objectiveName, amount);
            questObjective.setKilled(questData, killed);
            // sends message to player
            if (questData.quest.showProgressInWindow) {
               CompoundTag compound = new CompoundTag();
               compound.putInt("QuestID", questData.quest.id);
               compound.putString("Type", "kill");
               compound.putIntArray("Progress", new int[] { amount, questObjective.getMaxProgress() });
               compound.putString("TargetName", Component.translatable("script.killed").getString() + ": \"" + entity.getName().getString() + "\"");
               Packets.send((ServerPlayer) player, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
            }
            if (questData.quest.showProgressInChat) {
               if (amount >= questObjective.getMaxProgress()) { player.sendSystemMessage(Component.translatable("quest.message.kill.1", entity.getName().getString(), questData.quest.getTitle().getString())); }
               else { player.sendSystemMessage(Component.translatable("quest.message.kill.0", entity.getName().getString(), "" + amount, "" + questObjective.getMaxProgress(), questData.quest.getTitle().getString())); }
            }
            playerQuestData.checkQuestCompletion(player, questData);
            playerQuestData.updateClient = true;
         }
      }
   }

   @SubscribeEvent
   public void cnpcEntityInteract(EntityInteract event) {
      CustomNpcs.debugData.start(event.getEntity());
      ItemStack item = event.getEntity().getMainHandItem();
      if (!item.isEmpty() && event.getHand() == InteractionHand.MAIN_HAND && !event.getEntity().level().isClientSide()) {
         ServerPlayer player = (ServerPlayer) event.getEntity();
         if (!CustomNpcs.OpsOnly || CommonUtil.isOp(player)) {
            if (item.getItem() == CustomItems.soulstoneEmpty && event.getTarget() instanceof LivingEntity) {
               ((ItemSoulstoneEmpty) item.getItem()).store((LivingEntity) event.getTarget(), item, player);
               event.setCanceled(true);
            }
            else if (item.getItem() == CustomItems.wand) {
               if (event.getTarget() instanceof Villager) {
                  if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.EDIT_VILLAGER)) {
                     event.setCanceled(true);
                     NoppesUtilServer.setEditingNpc(player, null);
                     NoppesUtilServer.openContainerGui(player, EnumGuiType.MerchantAdd, (buffer) -> buffer.writeInt(event.getTarget().getId()));
                  }
               }
               else if (event.getTarget() instanceof EntityNPCInterface npc) {
                  if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_GUI)) {
                     event.setCanceled(true);
                     NoppesUtilServer.setEditingNpc(player, npc);
                     NoppesUtilServer.sendOpenGui(player, EnumGuiType.MainMenuDisplay, npc);
                  }
               }
            }
            else if (item.getItem() == CustomItems.cloner && !(event.getTarget() instanceof Player)) {
               CompoundTag compound = new CompoundTag();
               if (event.getTarget().saveAsPassenger(compound)) {
                  PlayerData data = PlayerData.get(player);
                  ServerCloneController.Instance.cleanTags(compound);
                  Packets.send(player, new PacketGuiCloneOpen(compound));
                  data.cloned = compound;
                  event.setCanceled(true);
               }
            }
            else if (item.getItem() == CustomItems.scripter && event.getTarget() instanceof EntityNPCInterface npc) {
               if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_GUI)) {
                  event.setCanceled(true);
                  NoppesUtilServer.setEditingNpc(player, npc);
                  Packets.send(player, new PacketGuiOpen(EnumGuiType.Script, BlockPos.ZERO));
               }
            }
            else if (item.getItem() == CustomItems.mount) {
               if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_MOUNTER)) {
                  event.setCanceled(true);
                  PlayerData.get(player).mounted = event.getTarget();
                  Packets.send(player, new PacketGuiOpen(EnumGuiType.MobSpawnerMounter, BlockPos.ZERO));
               }
            }
         }
      }
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcLivingDeath(LivingDeathEvent event) {
      if (event.getEntity().level().isClientSide) { return; }
      CustomNpcs.debugData.start(event.getEntity());
      Entity source = NoppesUtilServer.getDamageSource(event.getSource());
      if (source != null) {
         if (source instanceof EntityNPCInterface npc && event.getEntity() != null) {
            Line line = npc.advanced.getKillLine();
            if (line != null) { npc.saySurrounding(Line.formatTarget(line, event.getEntity())); }
            EventHooks.onNPCKills(npc, event.getEntity());
         }
         Player player;
         if (source instanceof Player) { player = (Player) source; }
         else if (source instanceof EntityNPCInterface && ((EntityNPCInterface) source).getOwner() instanceof Player) { player = (Player) ((EntityNPCInterface) source).getOwner(); }
         else if (source instanceof TamableAnimal && ((TamableAnimal)source).getOwner() instanceof Player) { player = (Player)((TamableAnimal) source).getOwner(); }
         else { player = null;}
         if (player != null && player.getServer() != null) {
            CustomNPCsScheduler.runTack(() -> doKillQuest(player, event.getEntity(), true));
            if (event.getEntity() instanceof EntityNPCInterface) {
               CustomNPCsScheduler.runTack(() -> doFactionPoints(player, (EntityNPCInterface)event.getEntity()));
            }
         }
      }
      if (event.getEntity() instanceof Player player) { PlayerData.get(player).save(false); }
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcEntityJoinLevel(EntityJoinLevelEvent event) {
      if (event.getLevel().isClientSide || !(event.getEntity() instanceof Player)) { return; }
      CustomNpcs.debugData.start(event.getEntity());
      PlayerData.get((Player)event.getEntity()).updateCompanion(event.getLevel());
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent(priority = EventPriority.LOW)
   public void cnpcAttachCapabilitiesEntity(AttachCapabilitiesEvent<Entity> event) {
      CustomNpcs.debugData.start(event.getObject());
      if (event.getObject() instanceof Player) { PlayerData.register(event); }
      if (event.getObject() instanceof LivingEntity) { MarkData.register(event); }
       WrapperEntityData.register(event);
       CustomNpcs.debugData.end(event.getObject());
   }

   @SubscribeEvent
   public void cnpcAttachCapabilitiesItem(AttachCapabilitiesEvent<ItemStack> event) {
      CustomNpcs.debugData.start("Item");
      ItemStackWrapper.register(event);
      CustomNpcs.debugData.end("Item");
   }

   @SubscribeEvent
   public void cnpcSaveToFile(SaveToFile event) {
      CustomNpcs.debugData.start(event.getEntity());
      PlayerData.get(event.getEntity()).save(false);
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcStartTracking(StartTracking event) {
      if (event.getTarget() instanceof LivingEntity && !event.getTarget().level().isClientSide) {
         CustomNpcs.debugData.start(event.getEntity());
         if (event.getTarget() instanceof EntityNPCInterface npc) {
            npc.tracking.add(event.getEntity().getId());
            VisibilityController.checkIsVisible(npc, (ServerPlayer)event.getEntity());
         }
         MarkData data = MarkData.get((LivingEntity)event.getTarget());
         if (!data.marks.isEmpty()) {
            Packets.send((ServerPlayer)event.getEntity(), new PacketMarkData(event.getTarget().getId(), data.getNBT()));
         }
         CustomNpcs.debugData.end(event.getEntity());
      }
   }

   @SubscribeEvent
   public void cnpcStopTracking(StopTracking event) {
      CustomNpcs.debugData.start(event.getEntity());
      if (event.getTarget() instanceof EntityNPCInterface npc) {
         npc.tracking.remove(event.getEntity().getId());
      }
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcCommandEvent(CommandEvent event) {
      CustomNpcs.debugData.start(event.getParseResults().getContext().getSource());
      String command = event.getParseResults().getReader().getString().replace("/", "");
      ServerPlayer player = event.getParseResults().getContext().getSource().getPlayer();
      if (player != null) {
         PlayerData data = PlayerData.get(player);
         String[] parts = command.split("\\s+", 2);
         String rawParams = parts.length > 1 ? parts[1].trim() : "";
         String[] parameters = rawParams.isEmpty() ? new String[0] : rawParams.split("\\s+");
         noppes.npcs.api.event.PlayerEvent.CommandEvent ev = new noppes.npcs.api.event.PlayerEvent.CommandEvent(
                 data.scriptData.getPlayer(),
                 command,
                 parameters
         );
         EventHooks.onEvent(PlayerData.get(player).scriptData, EnumScriptType.SEND_COMMAND, ev);
         if (ev.isCanceled()) {
            event.setCanceled(true);
            CustomNpcs.debugData.end(event.getParseResults().getContext().getSource());
            return;
         }
      }
      if (command.startsWith("give ")) {
         try {
            CommandContext<CommandSourceStack> context = event.getParseResults().getContext().build(event.getParseResults().getReader().getString());
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
             for (ServerPlayer pl : players) {
                if (pl.getServer() == null) { continue; }
                pl.getServer().execute(ListenableFutureTask.create(Executors.callable(() -> {
                    PlayerQuestData playerdata = PlayerData.get(pl).questData;
                    for (QuestData data : playerdata.activeQuests.values()) {
                       for (QuestObjective obj : data.quest.getObjectives(pl)) {
                          if (obj.getType() != EnumQuestTask.ITEM.ordinal()) { continue; }
                          playerdata.checkQuestCompletion(pl, data);
                          playerdata.updateClient = true;
                       }
                    }
                 })));
             }
         } catch (Throwable ignored) { }
      }
      else if (command.startsWith("time ")) {
         CustomNPCsScheduler.runTack(() -> {
            try {
               List<ServerPlayer> players = CustomNpcs.Server.getPlayerList().getPlayers();
               for (ServerPlayer pl : players) { VisibilityController.instance.onUpdate(pl); }
            }
            catch (Throwable ignored) { }
         });
      }
      CustomNpcs.debugData.end(event.getParseResults().getContext().getSource());
   }

}
