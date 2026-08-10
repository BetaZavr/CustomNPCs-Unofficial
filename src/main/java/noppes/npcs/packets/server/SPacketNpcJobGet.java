package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.roles.JobSpawner;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiData;

import java.util.Collections;
import java.util.List;

public class SPacketNpcJobGet extends PacketServerBasic {

   protected static int channelId;

   @Override
   public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand; }

   @Override
   public boolean requiresNpc() { return true; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.NPC_GUI); }

   public static void encode(SPacketNpcJobGet ignoredMsg, FriendlyByteBuf ignoredBuf) { }

   public static SPacketNpcJobGet decode(FriendlyByteBuf ignoredBuf) { return new SPacketNpcJobGet(); }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      if (npc.job.getType() != 0) {
         CompoundTag compound = new CompoundTag();
         compound.putBoolean("JobData", true);
         npc.job.save(compound);
         if (npc.job instanceof JobSpawner spawner) { spawner.cleanCompound(compound); }
         Packets.send(player, new PacketGuiData(compound));
      }
      CustomNpcs.debugData.end("Packets");
   }

}
