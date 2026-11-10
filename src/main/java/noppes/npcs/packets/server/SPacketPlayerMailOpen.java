package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerMail;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.controllers.data.PlayerMailData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketPlayerMailOpen extends PacketServerBasic {

   protected static int channelId;
   private boolean canEdit;
   private boolean canSend;
   private long time;
   private String username;

   public SPacketPlayerMailOpen() { }

   public SPacketPlayerMailOpen(boolean canEditIn, boolean canSendIn, long timeIn, String usernameIn) {
      canEdit = canEditIn;
      canSend = canSendIn;
      time = timeIn;
      username = usernameIn;
   }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public List<CustomNpcsPermissions.Permission> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) { return true; }

   @Override
   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(canEdit);
      buf.writeBoolean(canSend);
      buf.writeLong(time);
      buf.writeUtf(username);
   }

   @Override
   public void decode(FriendlyByteBuf buf) {
      canEdit = buf.readBoolean();
      canSend = buf.readBoolean();
      time = buf.readLong();
      username = buf.readUtf();
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      player.closeContainer();
      if (canEdit && canSend) {
         NoppesUtilServer.openContainerGui(player, EnumGuiType.PlayerMailOpen, (buf) -> {
            buf.writeBoolean(true);
            buf.writeBoolean(true);
         });
         return;
      }
      PlayerMailData mailData  = PlayerData.get(player).mailData;
      for (PlayerMail mail : mailData.playerMails) {
         if (mail.timeWhenReceived == time && mail.sender.equals(username)) {
            ContainerMail.staticMail = mail;
            NoppesUtilServer.openContainerGui(player, EnumGuiType.PlayerMailOpen, (buf) -> {
               buf.writeBoolean(false);
               buf.writeBoolean(false);
            });
            break;
         }
      }
      CustomNpcs.debugData.end("Packets");
   }

}
