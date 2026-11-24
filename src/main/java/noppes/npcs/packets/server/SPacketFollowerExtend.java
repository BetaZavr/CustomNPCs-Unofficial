package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.containers.ContainerNPCFollowerHire;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.world.ISimpleContainerMixin;
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
   private final int pos;

   public SPacketFollowerExtend(int posIn) { pos = posIn; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) { return true; }

   @Override
   public boolean requiresNpc() { return true; }

   public static void encode(SPacketFollowerExtend msg, FriendlyByteBuf buf) { buf.writeInt(msg.pos); }

   public static SPacketFollowerExtend decode(FriendlyByteBuf buf) { return new SPacketFollowerExtend(buf.readInt()); }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      if (npc != null && npc.role.getEnumType() == RoleType.FOLLOWER && player.containerMenu instanceof ContainerNPCFollowerHire) {
         RoleFollower role = (RoleFollower) npc.role;
         followerBuy(role, pos, player, npc);
         Packets.send(player, new PacketGuiData(npc.role.save(new CompoundTag())));
      }
      CustomNpcs.debugData.end("Packets");
   }

   public static void followerBuy(RoleFollower role, int pos, ServerPlayer player, EntityNPCInterface npc) {
      if (pos < 0 || pos > 3 || !role.rates.containsKey(pos)) { return; }
      if (pos == 3) {
         if (!player.isCreative()) {
            PlayerData data = PlayerData.get(player);
            if ((role.rentalIsDonat ? data.game.getDonat() : data.game.getMoney()) < role.rentalMoney) { return; }
            data.game.addMoney(role.rentalMoney * -1);
         }
      }
      else {
         ItemStack currency = role.rentalItems.getItem(pos);
         if (currency.isEmpty()) { return; }
         if (!player.isCreative()) {
            Map<ItemStack, Integer> map = new HashMap<>();
            map.put(currency, currency.getCount());
            if (!Util.instance.canRemoveItems(((ISimpleContainerMixin) role.rentalItems).getItems(), map, false, false)) {
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
