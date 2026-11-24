package noppes.npcs.packets.server;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.EventHooks;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.containers.ContainerNPCFollowerHire;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.inv.IInventoryBasicMixin;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiData;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPacketFollowerExtend extends PacketServerBasic {

   protected static int channelId;
   private int pos;

   public SPacketFollowerExtend() { }

   public SPacketFollowerExtend(int posIn) { pos = posIn; }

   @Override
   public List<CustomNpcsPermissions.Permission> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) { return true; }

   @Override
   public boolean requiresNpc() { return true; }

   @Override
   public void encode(FriendlyByteBuf buf) { }

   @Override
   public void decode(FriendlyByteBuf buf) { }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      if (npc != null && npc.role.getEnumType() == RoleType.FOLLOWER && player.openContainer instanceof ContainerNPCFollowerHire) {
         RoleFollower role = (RoleFollower) npc.role;
         followerBuy(role, pos, player, npc);
         Packets.send(player, new PacketGuiData(npc.role.save(new NBTTagCompound())));
      }
      CustomNpcs.debugData.end("Packets");
   }

   public static void followerBuy(RoleFollower role, int pos, EntityPlayerMP player, EntityNPCInterface npc) {
      if (pos < 0 || pos > 3 || !role.rates.containsKey(pos)) { return; }
      if (pos == 3) {
         if (!player.isCreative()) {
            PlayerData data = PlayerData.get(player);
            if ((role.rentalIsDonat ? data.game.getDonat() : data.game.getMoney()) < role.rentalMoney) { return; }
            data.game.addMoney(role.rentalMoney * -1);
         }
      }
      else {
         ItemStack currency = role.rentalItems.getStackInSlot(pos);
         if (currency.isEmpty()) { return; }
         if (!player.isCreative()) {
            Map<ItemStack, Integer> map = new HashMap<>();
            map.put(currency, currency.getCount());
            if (!Util.instance.canRemoveItems(((IInventoryBasicMixin) role.rentalItems).getItems(), map, false, false)) {
               return;
            }
            Util.instance.removeItem(player, currency, false, false);
         }
      }
      int days = role.rates.get(pos);
      RoleEvent.FollowerHireEvent event = new RoleEvent.FollowerHireEvent(player, npc.wrappedNPC, days);
      if (EventHooks.onNPCRole(npc, event) || event.days == 0) { return; }
      npc.say(player, new Line(NoppesStringUtils.formatText(role.dialogHire.replace("{days}", days + ""), player, npc)));
      role.setOwner(player);
      role.addDays(days);
   }

}
