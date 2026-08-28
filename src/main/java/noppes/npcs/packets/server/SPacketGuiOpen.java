package noppes.npcs.packets.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.api.item.ISpecBuilder;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerTransportData;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiOpen;
import noppes.npcs.roles.RoleTransporter;
import noppes.npcs.util.CustomNPCsScheduler;

public class SPacketGuiOpen extends PacketServerBasic {

   protected static int channelId;
   private final EnumGuiType type;
   private final BlockPos pos;

   public SPacketGuiOpen(EnumGuiType typeIn, BlockPos posIn) {
      type = typeIn;
      pos = posIn;
   }

   @Override
   public boolean requiresNpc() { return false; }


   @Override
   public List<PermissionNode<Boolean>> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) {
      return item.getItem() instanceof INPCToolItem || (item.getItem() instanceof ISpecBuilder && player.isCreative());
   }

   public static void encode(SPacketGuiOpen msg, FriendlyByteBuf buf) {
      buf.writeEnum(msg.type);
      buf.writeBlockPos(msg.pos);
   }

   public static SPacketGuiOpen decode(FriendlyByteBuf buf) {
      return new SPacketGuiOpen(buf.readEnum(EnumGuiType.class), buf.readBlockPos());
   }

   @Override
   public int getChannelId() { return channelId; }

   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      sendOpenGui(player, type, npc, pos);
      CustomNpcs.debugData.end("Packets");
   }

   public static void sendOpenGui(ServerPlayer player, EnumGuiType gui, EntityNPCInterface npc, BlockPos pos) {
      NoppesUtilServer.setEditingNpc(player, npc);
      NoppesUtilServer.sendExtraData(player, npc, gui);
      CustomNPCsScheduler.runTack(() -> {
         if (!gui.hasContainer) { Packets.send(player, new PacketGuiOpen(gui, pos)); }
         else {
            NoppesUtilServer.openContainerGui(player, gui, (buffer) -> {
               buffer.writeInt(npc != null ? npc.getId() : -1);
               buffer.writeBlockPos(pos);
            });
         }
         Map<String, Integer> map = getScrollData(player, gui, npc);
         if (map != null && !map.isEmpty()) { NoppesUtilServer.sendScrollData(player, map); }
      });
   }

   private static Map<String, Integer> getScrollData(Player player, EnumGuiType gui, EntityNPCInterface npc) {
      if (gui == EnumGuiType.PlayerTransporter) {
         RoleTransporter role = (RoleTransporter)npc.role;
         Map<String, Integer> map = new HashMap<>();
         TransportLocation location = role.getLocation();
         if (location != null) {
            PlayerTransportData playerdata = PlayerData.get(player).transportData;
            for (TransportLocation loc : location.category.locations.values()) {
               if (!map.containsKey(loc.name) && (loc.isDefault() || playerdata.transports.contains(loc.id))) {
                  map.put(loc.name, loc.id);
               }
            }
            map.remove(location.name);
         }
         return map;
      }
      return null;
   }

}
