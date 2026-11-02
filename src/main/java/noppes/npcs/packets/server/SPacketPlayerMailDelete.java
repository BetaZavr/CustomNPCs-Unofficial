package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerMailData;
import noppes.npcs.packets.client.PacketSyncUpdate;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiData;

import java.util.List;

public class SPacketPlayerMailDelete extends PacketServerBasic {

   protected static int channelId;
   private int type;
   private long time;
   private String username;

   public SPacketPlayerMailDelete() { }

   public SPacketPlayerMailDelete(int typeIn, long timeIn, String usernameIn) {
      type = typeIn;
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
      buf.writeInt(type);
      buf.writeLong(time);
      buf.writeUtf(username);
   }

   @Override
   public void decode(FriendlyByteBuf buf) {
      type = buf.readInt();
      time = buf.readLong();
      username = buf.readUtf();
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      PlayerMailData mailData = PlayerData.get(player).mailData;
      switch (type) {
         case 0: {
            mailData.playerMails.removeIf(mail -> mail.timeWhenReceived == time && mail.sender.equals(username));
            break;
         } // delete specific
         case 1: {
            mailData.playerMails.removeIf(mail -> mail.beenRead);
            Packets.send(player, new PacketSyncUpdate(0, 12, mailData.save(new NBTTagCompound())));
            break;
         } // delete all only read letters
         default: {
            mailData.playerMails.clear();
            Packets.send(player, new PacketSyncUpdate(0, 12, mailData.save(new NBTTagCompound())));
            break;
         } // delete all letters
      }
      Packets.send(player, new PacketGuiData(mailData.save(new NBTTagCompound())));
      CustomNpcs.debugData.end("Packets");
   }

}
