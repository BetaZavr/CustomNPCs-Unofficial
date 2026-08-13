package noppes.npcs.packets.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.DimensionController;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SPacketDimensionsGet extends PacketServerBasic {

   protected static int channelId;

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.teleporter; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.TOOL_TELEPORTER); }

   public static void encode(SPacketDimensionsGet ignoredMsg, FriendlyByteBuf ignoredBuf) { }

   public static SPacketDimensionsGet decode(FriendlyByteBuf ignoredBuf) { return new SPacketDimensionsGet(); }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      sendDimensionIDs(player);
      CustomNpcs.debugData.end("Packets");
   }

   public static void sendDimensionIDs(ServerPlayer player) {
      DimensionController.load();
      CompoundTag compound = new CompoundTag();
      ListTag list = new ListTag();
      List<ServerLevel> all = new ArrayList<>();
      for (String id : DimensionController.getLineKeys()) {
         ServerLevel level = CustomNpcs.Server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(id)));
         CompoundTag nbt = new CompoundTag();
         nbt.putBoolean("deleted", level == null);
         nbt.putBoolean("loaded", level != null && level.isLoaded(BlockPos.ZERO));
         nbt.putString("name", level != null ? level.dimension().location().toString() : id);
         list.add(nbt);
         if (level != null) { all.add(level); }
      }
      for (ServerLevel level : CustomNpcs.Server.getAllLevels()) {
         if (!all.contains(level)) {
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("deleted", false);
            nbt.putBoolean("loaded", level.isLoaded(BlockPos.ZERO));
            nbt.putString("name", level.dimension().location().toString());
            list.add(nbt);
            all.add(level);
         }
      }
      compound.put("Data", list);
      Packets.send(player, new PacketSync(9, compound, false));
   }

}
