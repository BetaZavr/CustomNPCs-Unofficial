package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.containers.ContainerNPCFollowerHire;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerGameData;
import noppes.npcs.mixin.world.ISimpleContainerMixin;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPacketFollowerHire extends PacketServerBasic {

   protected static int channelId;
   private final int pos;

   public SPacketFollowerHire(int posIn) { pos = posIn; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) { return true; }

   @Override
   public boolean requiresNpc() { return true; }

   public static void encode(SPacketFollowerHire msg, FriendlyByteBuf buf) { buf.writeInt(msg.pos); }

   public static SPacketFollowerHire decode(FriendlyByteBuf buf) { return new SPacketFollowerHire(buf.readInt()); }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      if (npc.role instanceof RoleFollower role && player.containerMenu instanceof ContainerNPCFollowerHire) {
         if (pos >= 0 && pos < 4 && role.rates.containsKey(pos)) {
            RoleEvent.FollowerHireEvent event = new RoleEvent.FollowerHireEvent(player, npc.wrappedNPC, role.rates.get(pos));
            if (!EventHooks.onNPCRole(npc, event) && event.days > 0) {
               if (!player.isCreative()) {
                  if (pos == 3) {
                     PlayerGameData game = PlayerData.get(player).game;
                     if ((role.rentalIsDonat ? game.getDonat() : game.getMoney()) < role.rentalMoney) {
                        CustomNpcs.debugData.end("Packets");
                        return;
                     }
                     game.addMoney(role.rentalMoney * -1);
                  } // money / donate
                  else {
                     ItemStack currency = role.rentalItems.getItem(pos);
                     if (!currency.isEmpty()) {
                        Map<ItemStack, Integer> map = new HashMap<>();
                        map.put(currency, currency.getCount());
                        if (!Util.instance.canRemoveItems(((ISimpleContainerMixin) role.rentalItems).getItems(), map, false, false)) {
                           CustomNpcs.debugData.end("Packets");
                           return;
                        }
                        Util.instance.removeItem(player, currency, false, false);
                     }
                  } // stack
               } // payment
               npc.say(player, new Line(NoppesStringUtils.formatText(role.dialogHire.replace("{days}", event.days + ""), player, npc)));
               role.setOwner(player);
               role.addDays(event.days);
            }
         }
      }
      CustomNpcs.debugData.end("Packets");
   }

}
