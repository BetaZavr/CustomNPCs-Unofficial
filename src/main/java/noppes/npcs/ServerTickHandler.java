package noppes.npcs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import noppes.npcs.api.event.WorldEvent;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataScenes;
import noppes.npcs.items.ItemBuilder;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSync;
import noppes.npcs.packets.client.PacketSyncUpdate;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.util.BuilderData;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

import java.util.ArrayList;
import java.util.List;

public class ServerTickHandler {

   public int ticks = 0;

   @SubscribeEvent
   public void cnpcPlayerTick(PlayerTickEvent event) {
      if (event.side != LogicalSide.SERVER || event.phase != Phase.START) { return; }
      CustomNpcs.debugData.start(event.player);
      ServerPlayer player = (ServerPlayer) event.player;
      // fixed a bug where NPCs wouldn't attack if they were less than 0.5
      if (player.getHealth() > 0 && player.getHealth() < 1.0f) { player.setHealth(1.0f); }
      PlayerData data = PlayerData.get(player);
      if (player.getCommandSenderWorld().getDayTime() % 24000L == 1L || player.getCommandSenderWorld().getDayTime() % 240000L == 12001L ||
              (data.prevHeldItem != player.getMainHandItem() && (data.prevHeldItem.getItem() == CustomItems.wand || player.getMainHandItem().getItem() == CustomItems.wand))) {
         VisibilityController.instance.onUpdate(player);
      }
      // New from Unofficial (BetaZavr)
      long resTime = player.getName().getString().codePointAt(0);
      if (player.level().getGameTime() % 20L == resTime % 20L) {
         // minimap
         data.minimap.update(player);
         // op player
         boolean isOP = CommonUtil.isOp(player);
         if (data.game.op != isOP) {
            data.game.op = isOP;
            data.game.updateClient = true;
         }
         // Fixed a bug where NPC followers would end up far away or in other dimensions
         List<PlayerGameData.FollowerSet> del = new ArrayList<>();
         for (PlayerGameData.FollowerSet fs : data.game.getFollowers()) {
            EntityNPCInterface npc = null;
            if (fs.npc != null) { npc = fs.npc; }
            if (npc == null) {
               if (Util.instance.getEntityByUUID(fs.id, player.level(), false) instanceof EntityNPCInterface cNpc) { npc = cNpc; }
            }
            if (npc == null || !npc.isAlive() || !(npc.role instanceof RoleFollower)) { del.add(fs); }
            else {
               Player owner = ((RoleFollower) npc.role).getOwner();
               if (owner == null || !owner.equals(player)) { del.add(fs); }
               else if (fs.npc == null) { fs.npc = npc; }
            }
            if (npc != null && npc.role instanceof RoleFollower follower) {
               if (!player.level().dimension().equals(npc.level().dimension())) {
                  npc = (EntityNPCInterface) Util.instance.teleportEntity(player.getServer(), npc, player.level().dimension(), player.getX(), player.getY(), player.getZ());
                  fs.dimId = npc.level().dimension().location();
                  fs.id = npc.getUUID();
                  npc.getNavigation().moveTo(player, npc.ais.canSprint ? 1.3 : 1.0d);
               }
               else if (player.distanceTo(npc) > follower.getRange()) {
                  npc.setPos(player.getX(), player.getY(), player.getZ());
               }
            }
         }
         for (PlayerGameData.FollowerSet fs : del) { data.game.removeFollower(fs); }
      }
      // mail and quest check
      if (player.level().getGameTime() % 200L == resTime % 200L) {
         if (data.overlay.currentGUI.equalsIgnoreCase("chatscreen") || data.overlay.currentGUI.equalsIgnoreCase("guiingame")) {
            CustomNPCsScheduler.runTack(() -> {
               for (QuestData questData : data.questData.activeQuests.values()) {
                  data.questData.checkQuestCompletion(player, questData);
               }
            });
         }
         if (!data.mailData.playerMails.isEmpty()) {
            boolean needSend = false;
            long time = System.currentTimeMillis();
            long timeToRemove = -1L;
            if (CustomNpcs.MailTimeWhenLettersWillBeDeleted > 0) { timeToRemove = CustomNpcs.MailTimeWhenLettersWillBeDeleted * 86400000L; }
            List<PlayerMail> del = new ArrayList<>();
            for (PlayerMail mail : data.mailData.playerMails) {
               if (player.isCreative() && mail.timeWillCome > 0L) {
                  mail.timeWillCome = 0L;
                  needSend = true;
               }
               long timeWhenReceived = time - mail.timeWhenReceived - mail.timeWillCome;
               if (timeToRemove > 0L && timeWhenReceived > timeToRemove) {
                  del.add(mail);
                  needSend = true;
               }
               if (mail.beenRead || timeWhenReceived < 0L) {
                  continue;
               }
               needSend = true;
            }
            for (PlayerMail mail : del) {
               data.mailData.playerMails.remove(mail);
            }
            if (needSend) { Packets.send(player, new PacketSyncUpdate(0, 12, data.mailData.save(new CompoundTag()))); }
         }
      }
      // updates
      if (data.updateClient) {
         Packets.send(player, new PacketSync(8, data.getSyncNBT(), true));
         data.updateClient = false;
      }
      if (data.questData.updateClient) {
         Packets.send(player, new PacketSync(4, data.questData.save(new CompoundTag()), false));
         data.questData.updateClient = false;
      }
      if (data.game.updateClient) {
         Packets.send(player, new PacketSync(2, data.game.save(new CompoundTag()), false));
         data.game.updateClient = false;
      }
      data.bankData.update(player);
      data.prevHeldItem = player.getMainHandItem();
      // New from Unofficial (GoodBird)
      if (data.overlay.updateClient) {
         Packets.send(player, new PacketSync(6, data.overlay.save(new CompoundTag()), false));
         data.overlay.updateClient = false;
      }
      CustomNpcs.debugData.end(event.player);
   }

   @SubscribeEvent
   public void cnpcServerTick(ServerTickEvent event) {
      if (event.side != LogicalSide.SERVER || event.phase != Phase.START) { return; }
      CustomNpcs.debugData.start("Mod");
      ticks++;
      // New from Unofficial (BetaZavr)
      if (ticks % 5 == 0) { BorderController.getInstance().update(); }
      if (ticks % 20 == 0) {
         SchematicController.Instance.updateBuilding();
         MarcetController.getInstance().update();
         MassBlockController.Update();
         for (DataScenes.SceneState state : DataScenes.StartedScenes.values()) {
            if (!state.paused) { ++state.ticks; }
         }
         for (DataScenes.SceneContainer entry : DataScenes.ScenesToRun) { entry.update(); }
         DataScenes.ScenesToRun.clear();
         // Deleting a construction date from the database every 5 min, for dates without a player
         if (ticks % 6000 == 0) {
            List<Integer> del = new ArrayList<>();
            for (int id : SyncController.dataBuilder.keySet()) {
               BuilderData bd = SyncController.dataBuilder.get(id);
               if (bd.player == null) {
                  del.add(id);
                  continue;
               }
               ItemStack stack = null;
               for (ItemStack s : bd.player.getInventory().items) {
                  if (ItemBuilder.isBuilder(s, bd)) {
                     stack = s;
                     break;
                  }
               }
               if (stack == null) { del.add(id); }
            }
            for (Integer id : del) { SyncController.dataBuilder.remove(id); }
         }
      }
      if (ticks % 10 == 0 && CustomNpcs.Server != null && !CustomNpcs.Server.getPlayerList().getPlayers().isEmpty()) {
         ServerPlayer player = CustomNpcs.Server.getPlayerList().getPlayers().get(0);
         if (player != null) { EventHooks.onEvent(PlayerData.get(player).scriptData, "worldtick", new WorldEvent.ServerTickEvent(event)); }
      }
      if (ticks % 1200 == 0) {
         BankController.getInstance().update();
         Packets.clearDelaySendMap();
      }
      CustomNpcs.debugData.end("Mod");
   }

   @SubscribeEvent
   public void cnpcLevelTick(LevelTickEvent event) {
      if (event.side == LogicalSide.SERVER && event.phase == Phase.START) {
         CustomNpcs.debugData.start("Mod");
         NPCSpawning.findChunksForSpawning((ServerLevel)event.level);
         CustomNpcs.debugData.end("Mod");
      }
   }

}
