package noppes.npcs.packets.server;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiOpen;
import noppes.npcs.packets.client.PacketNpcInitData;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.item.ISpecBuilder;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerTransportData;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.roles.RoleTransporter;
import noppes.npcs.util.CustomNPCsScheduler;

public class SPacketGuiOpen extends PacketServerBasic {

   protected static int channelId;
   private EnumGuiType type;
   private BlockPos pos;

   public SPacketGuiOpen() { }

   public SPacketGuiOpen(EnumGuiType typeIn, BlockPos posIn) {
      type = typeIn;
      pos = posIn;
   }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public List<CustomNpcsPermissions.Permission> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) {
      return item.getItem() instanceof INPCToolItem || (item.getItem() instanceof ISpecBuilder && player.isCreative());
   }

   @Override
   public void encode(FriendlyByteBuf buf) {
      buf.writeEnum(type);
      buf.writeBlockPos(pos);
   }

   @Override
   public void decode(FriendlyByteBuf buf) {
      type = buf.readEnum(EnumGuiType.class);
      pos = buf.readBlockPos();
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      sendOpenGui(player, type, npc, pos);
      CustomNpcs.debugData.end("Packets");
   }

   public static void sendOpenGui(EntityPlayerMP player, EnumGuiType gui, EntityNPCInterface npc, BlockPos pos) {
      NoppesUtilServer.setEditingNpc(player, npc);
      if (npc != null && isNpcEditMenu(gui)) {
         NBTTagCompound full = new NBTTagCompound();
         npc.writeToNBT(full);
         Packets.send(player, new PacketNpcInitData(npc.getEntityId(), full));
      }
      NoppesUtilServer.sendExtraData(player, npc, gui);
      CustomNPCsScheduler.runTack(() -> {
         if (player.getServer() != null) {
            try {
               if (!gui.hasContainer) { Packets.send(player, new PacketGuiOpen(gui, pos)); }
               else if (NoppesUtilServer.openContainerGui(player, gui, (buffer) -> {
                  buffer.writeInt(npc != null ? npc.getEntityId() : -1);
                  buffer.writeBlockPos(pos);
               })) {
                  ArrayList<String> list = getScrollData(player, gui, npc);
                  if (list != null && !list.isEmpty()) { NoppesUtilServer.sendScrollData(player, list); }
               }
            }
            catch (Exception e) { LogWriter.error(e); }
         }
      }, 100);
   }

   private static boolean isNpcEditMenu(EnumGuiType gui) {
      switch (gui) {
         case MainMenuDisplay:
         case MainMenuStats:
         case MainMenuAI:
         case MainMenuInv:
         case MainMenuAdvanced:
         case MainMenuGlobal: return true;
         default: return false;
      }
   }

   private static ArrayList<String> getScrollData(EntityPlayerMP player, EnumGuiType gui, EntityNPCInterface npc) {
      if (gui == EnumGuiType.PlayerTransporter) {
         RoleTransporter role = (RoleTransporter)npc.role;
         ArrayList<String> list = new ArrayList<>();
         TransportLocation location = role.getLocation();
         if (location != null) {
            String name = role.getLocation().name;
            for (TransportLocation loc : location.category.getDefaultLocations()) {
               if (!list.contains(loc.name)) { list.add(loc.name); }
            }
            PlayerTransportData playerdata = PlayerData.get(player).transportData;
            for (int i : playerdata.transports) {
               TransportLocation loc = TransportController.getInstance().getTransport(i);
               if (loc != null && location.category.locations.containsKey(loc.id) && !list.contains(loc.name)) { list.add(loc.name); }
            }
            list.remove(name);
         }
         return list;
      }
      return null;
   }

}
