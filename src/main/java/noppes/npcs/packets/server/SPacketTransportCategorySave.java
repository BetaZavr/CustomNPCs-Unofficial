package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;

public class SPacketTransportCategorySave extends PacketServerBasic {

   protected static int channelId;
   private NBTTagCompound compound;

   public SPacketTransportCategorySave() { }

   public SPacketTransportCategorySave(NBTTagCompound compoundIn) { compound = compoundIn; }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand; }

   @Override
   public List<CustomNpcsPermissions.Permission> getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_TRANSPORT); }

   @Override
   public void encode(FriendlyByteBuf buf) { buf.writeNbt(compound); }

   @Override
   public void decode(FriendlyByteBuf buf) { compound = buf.readAnySizeNbt(); }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      TransportController.getInstance().saveCategory(compound);
      // Sync the updated categories back to the client immediately
      TransportController.getInstance().sendTo(player);
      Packets.send(player, new PacketGuiData(new NBTTagCompound()));
      CustomNpcs.debugData.end("Packets");
   }

}
