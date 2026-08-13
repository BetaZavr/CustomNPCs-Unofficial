package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.roles.RoleTransporter;

import java.util.Collections;
import java.util.List;

public class SPacketTransportLocationSave extends PacketServerBasic {

   protected static int channelId;
   private int category;
   private NBTTagCompound data;

   public SPacketTransportLocationSave() { }

   public SPacketTransportLocationSave(int categoryIn, NBTTagCompound dataIn) {
      data = dataIn;
      category = categoryIn;
   }

   @Override
   public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand; }

   @Override
   public boolean requiresNpc() { return true; }

   @Override
   public List<CustomNpcsPermissions.Permission> getPermission() { return Collections.singletonList(CustomNpcsPermissions.NPC_ADVANCED); }

   @Override
   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(category);
      buf.writeNbt(data);
   }

   @Override
   public void decode(FriendlyByteBuf buf) {
      category = buf.readInt();
      data = buf.readNbt();
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      TransportLocation location = TransportController.getInstance().saveLocation(category, data, npc);
      if (location != null && npc.role.getType() == 4) {
         RoleTransporter role = (RoleTransporter) npc.role;
         role.setTransport(location);
      }
      CustomNpcs.debugData.end("Packets");
   }
}
