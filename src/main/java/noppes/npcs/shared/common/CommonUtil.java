package noppes.npcs.shared.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketScriptError;
import noppes.npcs.shared.common.util.LogWriter;

public class CommonUtil {

   private static final List<Component> errorMessagesToAdmin = new ArrayList<>();

   public static void NotifyOPs(String message, Object... obs) {
      NotifyOPs(Component.translatable(message, obs).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
   }

   public static void NotifyOPs(Component message, boolean isScriptError) {
      Component component = Component.literal("[")
              .append(Component.literal(CustomNpcs.MODNAME).withStyle(ChatFormatting.DARK_GREEN))
              .append(Component.literal("]").withStyle(ChatFormatting.WHITE))
              .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
              .append(message);
      boolean isSend = false;
      if (CustomNpcs.Server != null) {
          for (ServerPlayer player : CustomNpcs.Server.getPlayerList().getPlayers()) {
              if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.ADMIN) ||
                      (player.shouldInformAdmins() && isOp(player))) {
                 if (isScriptError) { Packets.send(player, new PacketScriptError(component)); }
                 else if (CustomNpcs.DisplayErrorInChat) { player.sendSystemMessage(component); }
                 isSend = true;
              }
          }
         ServerLevel level = CustomNpcs.Server.getLevel(Level.OVERWORLD);
         if (level != null && level.getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS)) {
            LogWriter.info(component.getString());
         }
      }
      if (!isSend) {
         boolean found = false;
         for (Component mes : errorMessagesToAdmin) {
            if (mes.getString().equals(component.getString())) {
               found = true;
               break;
            }
         }
         if (!found) { errorMessagesToAdmin.add(component); }
      }
   }

   public static boolean isOp(Player player) {
      if (player == null || player.getServer() == null) { return false; }
      return player.getServer().getPlayerList().isOp(player.getGameProfile());
   }

   // New from Unofficial (BetaZavr)
   public static void sendScriptErrorsTo(Player player) {
      if (!errorMessagesToAdmin.isEmpty() && player != null && player.isCreative() && isOp(player)) {
         for (Component component : errorMessagesToAdmin) {
            if (player instanceof ServerPlayer sPlayer) { Packets.send(sPlayer, new PacketScriptError(component)); }
            else if (CustomNpcs.DisplayErrorInChat) { player.sendSystemMessage(component); }
         }
         errorMessagesToAdmin.clear();
      }
   }

}
