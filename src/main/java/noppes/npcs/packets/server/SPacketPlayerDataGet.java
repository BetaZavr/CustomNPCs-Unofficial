package noppes.npcs.packets.server;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.constants.EnumPlayerData;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiData;
import noppes.npcs.packets.client.PacketOverworldTime;
import noppes.npcs.shared.common.PacketServerBasic;

public class SPacketPlayerDataGet extends PacketServerBasic {

   protected static int channelId;
   private final EnumPlayerData type;
   private final String name;

   public SPacketPlayerDataGet(EnumPlayerData typeIn, String nameIn) {
      type = typeIn;
      name = nameIn;
   }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_PLAYERDATA); }

   public static void encode(SPacketPlayerDataGet msg, FriendlyByteBuf buf) {
      buf.writeEnum(msg.type);
      buf.writeUtf(msg.name);
   }

   public static SPacketPlayerDataGet decode(FriendlyByteBuf buf) {
      return new SPacketPlayerDataGet(buf.readEnum(EnumPlayerData.class), buf.readUtf());
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      sendPlayerData(type, player, name);
      CustomNpcs.debugData.start("Packets");
   }

   public static void sendPlayerData(EnumPlayerData type, ServerPlayer player, String name) {
      // time
      long time;
      MinecraftServer server = player.getServer();
      if (server == null) { server = CustomNpcs.Server; }
      if (server != null) { time = server.overworld().getGameTime(); }
      else { time = player.level().getGameTime(); }
      Packets.send(player, new PacketOverworldTime(time));
      // data
      Map<String, Integer> map = new HashMap<>();
      PlayerData playerdata = PlayerDataController.instance.getDataFromUsername(player.getServer(), name);
      if (playerdata == null && type != EnumPlayerData.Players) {
         NoppesUtilServer.sendScrollData(player, map);
         return;
      }
      switch (type) {
         case Dialog: {
            PlayerDialogData data = playerdata.dialogData;
            for (int dialogId : data.dialogsRead.keySet()) {
               Dialog dialog = DialogController.instance.dialogs.get(dialogId);
               if (dialog != null) {
                  map.put(dialog.category.title + ": "
                       + Component.translatable(dialog.title).getString(), dialogId);
               }
               else { map.put(Component.translatable("gui.deleted").append("" + dialogId).getString(), dialogId); }
            }
            break;
         }
         case Quest: {
            PlayerQuestData data = playerdata.questData;
            for (int questId : data.activeQuests.keySet()) {
               Quest quest = QuestController.instance.quests.get(questId);
               if (quest != null) {
                  map.put(quest.category.title + ": "
                          + Component.translatable(quest.title).getString() + "(Active quest)", questId);
               }
               else { map.put(Component.translatable("gui.deleted").append(questId + "(Active quest)").getString(), questId); }
            }
            for (int questId : data.getFinishedQuest()) {
               Quest quest = QuestController.instance.quests.get(questId);
               if (quest != null) {
                  map.put(quest.category.title + ": "
                       + Component.translatable(quest.title).getString() + "("+data.getFinishedTime(questId)+")(Finished quest)", questId);
               }
               else {
                  map.put(Component.translatable("gui.deleted").append(questId +
                       "("+data.getFinishedTime(questId)+")(Finished quest)").getString(), questId);
               }
            }
            break;
         }
         case Transport: {
            PlayerTransportData data = playerdata.transportData;
            for (int transportId : data.transports) {
               TransportLocation location = TransportController.getInstance().getTransport(transportId);
               if (location != null) { map.put(location.category.title + ": " + location.name, transportId); }
               else { map.put(Component.translatable("gui.deleted").append(" ID:" + transportId).getString(), transportId); }
            }
            break;
         }
         case Factions: {
            PlayerFactionData data = playerdata.factionData;
            for (int factionId : data.factionData.keySet()) {
               Faction faction = FactionController.instance.factions.get(factionId);
               if (faction != null) { map.put(faction.name + ";" + data.getFactionPoints(player, factionId), factionId); }
               else { map.put(Component.translatable("gui.deleted").append("" + factionId).getString(), factionId); }
            }
            break;
         }
         case Bank: {
            if (!playerdata.uuid.isEmpty()) {
               for (Bank bank : BankController.getInstance().getBanks()) {
                  if (bank == null || bank.isPublic) { continue; }
                  File bankFile = CustomNpcs.getLevelSaveDirectory("playerdata/"+playerdata.uuid+"/banks/"+bank.id+".dat");
                  if (bankFile != null && bankFile.exists()) { map.put(bank.name, bank.id); }
               }
            }
            break;
         }
         case Game: {
            Packets.send(player, new PacketGuiData(playerdata.game.save(new CompoundTag())));
            break;
         }
         case Players: {
            for (String username : PlayerDataController.instance.getPlayerNames()) { map.put(username, 0); }
            if (server != null) {
               for (String username : server.getPlayerList().getPlayerNamesArray()) { map.put(username, 1); }
            }
            break;
         }
      }
      NoppesUtilServer.sendScrollData(player, map);
   }

}
