package noppes.npcs.packets.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomTeleporter;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketDimensionTeleport extends PacketServerBasic {

   protected static int channelId;
   private final ResourceKey<Level> id;

   public SPacketDimensionTeleport(ResourceKey<Level> idIn) { id = idIn; }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.teleporter; }

   public static void encode(SPacketDimensionTeleport msg, FriendlyByteBuf buf) { buf.writeResourceKey(msg.id); }

   public static SPacketDimensionTeleport decode(FriendlyByteBuf buf) { return new SPacketDimensionTeleport(buf.readResourceKey(Registries.DIMENSION)); }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      if (player.getServer() != null) {
         ServerLevel level = player.getServer().getLevel(id);
         if (level != null) {
            BlockPos coords = level.getSharedSpawnPos();
            if (!level.isEmptyBlock(coords)) { coords = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, coords);}
            else {
               while(level.isEmptyBlock(coords) && coords.getY() > 0) { coords = coords.below(); }
               if (coords.getY() == 0) { coords = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, coords); }
            }
            teleportPlayer(player, id, coords.getX() + 0.5d, coords.getY(), coords.getZ() + 0.5d, player.getYRot(), player.getXRot());
         }
      }
      CustomNpcs.debugData.end("Packets");
   }

   public static void teleportPlayer(ServerPlayer player, ResourceKey<Level> dimension,
                                     double x, double y, double z, float yaw, float pitch) {
      if (player.level().dimension() != dimension) {
         MinecraftServer server = player.getServer();
         ServerLevel level = null;
         if (server != null) { level = server.getLevel(dimension); }
         if (level == null) {
            player.sendSystemMessage(Component.literal("Broken transporter. Dimension does not exist"));
            return;
         }
         player.moveTo(x, y, z, yaw, pitch);
         player.changeDimension(level, new CustomTeleporter(level, new Vec3(x, y, z), yaw, pitch));
      } else {
         player.connection.teleport(x, y, z, yaw, pitch);
      }
   }

}
