package noppes.npcs.packets.server;

import java.io.File;
import java.util.Collections;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.constants.EnumPlayerData;
import noppes.npcs.controllers.BankController;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.data.*;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.util.Util;

public class SPacketPlayerDataRemove extends PacketServerBasic {

   protected static int channelId;
   private final EnumPlayerData type;
   private final String name;
   private final int id;

   public SPacketPlayerDataRemove(EnumPlayerData typeIn, String nameIn, int idIn) {
      type = typeIn;
      name = nameIn;
      id = idIn;
   }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_PLAYERDATA); }

   public static void encode(SPacketPlayerDataRemove msg, FriendlyByteBuf buf) {
      buf.writeEnum(msg.type);
      buf.writeUtf(msg.name);
      buf.writeInt(msg.id);
   }

   public static SPacketPlayerDataRemove decode(FriendlyByteBuf buf) {
      return new SPacketPlayerDataRemove(buf.readEnum(EnumPlayerData.class), buf.readUtf(), buf.readInt());
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      MinecraftServer server = player.getServer();
      if (server == null) { server = CustomNpcs.Server; }
      if (name != null && !name.isEmpty() && server != null) {
         ServerPlayer pl = server.getPlayerList().getPlayerByName(name);
         PlayerData playerdata = PlayerDataController.instance.getDataFromUsername(player.getServer(), name);
         switch (type) {
            case Players: {
               if (playerdata != null) {
                  File playerDir = new File(CustomNpcs.getLevelSaveDirectory("playerdata"), playerdata.uuid);
                  if (playerDir.exists()) { Util.instance.removeFile(playerDir); }
                  playerdata.clear();
                  player.sendSystemMessage(Component.translatable("command.player.data.remove", playerdata.name, (pl != null ? "online" : "offline")));
               }
               break;
            }
            case Quest: {
               if (playerdata != null) {
                  PlayerQuestData data = playerdata.questData;
                  if (id < 0) {
                     playerdata.questData.clearFinishedQuests();
                     playerdata.questData.activeQuests.clear();
                  }
                  else {
                     data.activeQuests.remove(id);
                     data.removeFinishedQuest(id);
                  }
               }
               break;
            }
            case Dialog: {
               if (playerdata != null) {
                  PlayerDialogData data = playerdata.dialogData;
                  if (id < 0) { playerdata.dialogData.dialogsRead.clear(); }
                  else { data.dialogsRead.remove(id); }
               }
               break;
            }
            case Transport: {
               if (playerdata != null) {
                  PlayerTransportData data = playerdata.transportData;
                  if (id < 0) { data.transports.clear(); }
                  else { data.transports.remove(id); }
               }
               break;
            }
            case Bank: {
               if (playerdata != null) {
                  if (id < 0) {
                     if (!playerdata.uuid.isEmpty()) {
                        Util.instance.removeFile(CustomNpcs.getLevelSaveDirectory("playerdata/"+playerdata.uuid+"/banks"));
                     }
                  }
                  else {
                     Bank bank = BankController.getInstance().getBank(id);
                     if (bank != null && !bank.isPublic && !playerdata.uuid.isEmpty()) {
                        Util.instance.removeFile(CustomNpcs.getLevelSaveDirectory("playerdata/"+playerdata.uuid+"/banks/"+id+".dat"));
                     }
                  }
               }
               break;
            }
            case Factions: {
               if (playerdata != null) {
                  PlayerFactionData data = playerdata.factionData;
                  if (id < 0) { data.factionData.clear(); }
                  else { data.factionData.remove(id); }
               }
               break;
            }
            case Game: {
               if (playerdata != null) {
                  playerdata.game.setMoney(0L);
                  playerdata.game.marketData.clear();
               }
               break;
            }
            case Wipe: {
               List<String> list = PlayerDataController.instance.getPlayerNames();
               Collections.addAll(list, server.getPlayerList().getPlayerNamesArray());
               for (String name : list) {
                  ServerPlayer p = server.getPlayerList().getPlayerByName(name);
                  PlayerData pData = PlayerDataController.instance.getDataFromUsername(player.getServer(), name);
                  if (pData != null) {
                     pData.clear();
                     if (p != null) {
                        pData.save(true);
                        p.sendSystemMessage(Component.translatable("message.change.mod.data"));
                     }
                  }
               }
               SPacketPlayerDataCleaning.delete(CustomNpcs.getLevelSaveDirectory("playerdata"));
               SPacketPlayerDataGet.sendPlayerData(EnumPlayerData.Players, player, player.getName().getString());
               return;
            }
         }
         if (playerdata != null) {
            if (id >= 0) { playerdata.save(true); }
            if (pl != null) { pl.sendSystemMessage(Component.translatable("message.change.mod.data")); }
         }
         SPacketPlayerDataGet.sendPlayerData(type, player, name);
      }
      CustomNpcs.debugData.end("Packets");
   }

}
