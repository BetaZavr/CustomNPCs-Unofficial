package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;

public class SPacketTransportCategorySave extends PacketServerBasic {

   protected static int channelId;
   private final CompoundTag compound;

   public SPacketTransportCategorySave(CompoundTag compoundIn) { compound = compoundIn; }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_TRANSPORT); }

   public static void encode(SPacketTransportCategorySave msg, FriendlyByteBuf buf) { buf.writeNbt(msg.compound); }

   public static SPacketTransportCategorySave decode(FriendlyByteBuf buf) { return new SPacketTransportCategorySave(buf.readNbt()); }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      TransportController.getInstance().saveCategory(compound);
      CustomNpcs.debugData.end("Packets");
   }

}
