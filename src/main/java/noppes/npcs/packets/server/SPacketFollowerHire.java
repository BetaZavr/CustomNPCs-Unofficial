package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.containers.ContainerNPCFollowerHire;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerGameData;
import noppes.npcs.mixin.inv.IInventoryBasicMixin;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPacketFollowerHire extends PacketServerBasic {

   protected static int channelId;
   private int pos;

   public SPacketFollowerHire() { }

   public SPacketFollowerHire(int posIn) { pos = posIn; }

   @Override
   public List<CustomNpcsPermissions.Permission> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) { return true; }

   @Override
   public boolean requiresNpc() { return true; }

   @Override
   public void encode(FriendlyByteBuf buf) { buf.writeInt(pos); }

   @Override
   public void decode(FriendlyByteBuf buf) { pos = buf.readInt(); }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      if (npc.role instanceof RoleFollower && player.openContainer instanceof ContainerNPCFollowerHire) {
         RoleFollower role = (RoleFollower) npc.role;
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
                     ItemStack currency = role.rentalItems.getStackInSlot(0);
                     if (!currency.isEmpty()) {
                        Map<ItemStack, Integer> map = new HashMap<>();
                        map.put(currency, currency.getCount());
                        if (!Util.instance.canRemoveItems(((IInventoryBasicMixin) role.rentalItems).getItems(), map, false, false)) {
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
