package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerMailData;
import noppes.npcs.packets.client.PacketSyncUpdate;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiData;

import java.util.List;

public class SPacketPlayerMailDelete extends PacketServerBasic {

   protected static int channelId;
   private final int type;
   private final long time;
   private final String username;

   public SPacketPlayerMailDelete(int typeIn, long timeIn, String usernameIn) {
      type = typeIn;
      time = timeIn;
      username = usernameIn;
   }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) { return true; }

   public static void encode(SPacketPlayerMailDelete msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.type);
      buf.writeLong(msg.time);
      buf.writeUtf(msg.username);
   }

   public static SPacketPlayerMailDelete decode(FriendlyByteBuf buf) { return new SPacketPlayerMailDelete(buf.readInt(), buf.readLong(), buf.readUtf()); }

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
            Packets.send(player, new PacketSyncUpdate(0, 12, mailData.save(new CompoundTag())));
            break;
         } // delete all only read letters
         default: {
            mailData.playerMails.clear();
            Packets.send(player, new PacketSyncUpdate(0, 12, mailData.save(new CompoundTag())));
            break;
         } // delete all letters
      }
      Packets.send(player, new PacketGuiData(mailData.save(new CompoundTag())));
      CustomNpcs.debugData.end("Packets");
   }

}
