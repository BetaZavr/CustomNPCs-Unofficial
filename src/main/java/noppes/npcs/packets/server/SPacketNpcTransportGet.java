package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiData;
import noppes.npcs.packets.client.PacketGuiScrollSelected;
import noppes.npcs.roles.RoleTransporter;

import java.util.Collections;
import java.util.List;

public class SPacketNpcTransportGet extends PacketServerBasic {

   protected static int channelId;

   @Override
   public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand; }

   @Override
   public boolean requiresNpc() { return true; }

   @Override
   public List<CustomNpcsPermissions.Permission> getPermission() { return Collections.singletonList(CustomNpcsPermissions.NPC_GUI); }

   @Override
   public void encode(FriendlyByteBuf buf) { }

   @Override
   public void decode(FriendlyByteBuf buf) { }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      if (npc.role instanceof RoleTransporter && ((RoleTransporter) npc.role).hasTransport()) {
         RoleTransporter role = (RoleTransporter) npc.role;
         TransportLocation location = role.getLocation();
         if (location != null) {
            Packets.send(player, new PacketGuiData(location.save()));
            Packets.send(player, new PacketGuiScrollSelected(location.category.title));
         }
      }
      CustomNpcs.debugData.end("Packets");
   }

}
