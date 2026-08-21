package noppes.npcs.packets.server;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.*;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.containers.ContainerNPCBank;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.controllers.data.Bank;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPacketBankBuy extends PacketServerBasic {

   protected static int channelId;
   private final int bankId;
   private final int ceil;
   private final int size;
   private final int scrollY;
   private final int ceilPos;

   public SPacketBankBuy(int bankIdIn, int ceilIn, int sizeIn, int scrollYIn, int ceilPosIn) {
      bankId = bankIdIn;
      ceil = ceilIn;
      size = sizeIn;
      scrollY = scrollYIn;
      ceilPos = ceilPosIn;
   }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public boolean toolAllowed(ItemStack item) { return true; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return null; }

   public static void encode(SPacketBankBuy msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.bankId);
      buf.writeInt(msg.ceil);
      buf.writeInt(msg.size);
      buf.writeInt(msg.scrollY);
      buf.writeInt(msg.ceilPos);
   }

   public static SPacketBankBuy decode(FriendlyByteBuf buf) {
      return new SPacketBankBuy(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      if (player.containerMenu instanceof ContainerNPCBank cont) {
         boolean isOwner = player.isCreative() || !cont.data.bank.isPublic || cont.data.bank.owner.isEmpty() || player.getName().getString().equals(cont.data.bank.owner);
         boolean update = false;
         if (!isOwner) {
            player.sendSystemMessage(Component.translatable("bank.hover.changed.false").withStyle(ChatFormatting.RED));
         }
         else {
            NpcMiscInventory inv = cont.data.get(ceil);
            if (cont.data.bank.ceilSettings.containsKey(ceil) && inv != null) {
               PlayerData data = PlayerData.get(player);
               Bank.CeilSettings cs = cont.data.bank.ceilSettings.get(ceil);
               if (inv.getContainerSize() == 0) {
                  NpcMiscInventory invPre = cont.data.get(ceil - 1);
                  boolean open = player.isCreative() || ceil == 0 ||
                          cont.data.bank.ceilSettings.get(ceil - 1).isFree ||
                          (invPre != null && invPre.getContainerSize() == cont.data.bank.ceilSettings.get(ceil - 1).maxCells);
                  if (open & !player.isCreative()) {
                     if (!cs.openStack.isEmpty()) {
                        Map<ItemStack, Integer> items = new HashMap<>();
                        items.put(cs.openStack, cs.openStack.getCount());
                        open = Util.instance.canRemoveItems(player.inventoryMenu.getItems(), items, false, false);
                        if (!open) { player.sendSystemMessage(Component.translatable("hover.operation.not.items")); }
                     }
                     if (cs.openMoney > 0) {
                        if (open) { open = data.game.getMoney() >= cs.openMoney; }
                        if (!open) { player.sendSystemMessage(Component.translatable("hover.operation.not.money")); }
                     }
                     if (cs.openDonat > 0) {
                        if (open) { open = data.game.getDonat() >= cs.openDonat; }
                        if (!open) { player.sendSystemMessage(Component.translatable("hover.operation.not.donat")); }
                     }
                     if (open) {
                        if (!cs.openStack.isEmpty()) { Util.instance.removeItem(player, cs.openStack, false, false); }
                        if (cs.openMoney > 0) { data.game.addMoney(-cs.openMoney); }
                        if (cs.openDonat > 0) { data.game.addDonat(-cs.openDonat); }
                     }
                  }
                  if (open) {
                     int slot = Math.min(cs.maxCells, inv.getContainerSize() + size);
                     RoleEvent.BankUnlockedEvent event = new RoleEvent.BankUnlockedEvent(player, npc.wrappedNPC, cont.data.bank, slot);
                     update = !event.isCanceled() && cont.data.openNew(ceil);
                  }
               } // open
               else {
                  boolean upgrade = true;
                  if (!player.isCreative()) {
                     if (!cs.upgradeStack.isEmpty()) {
                        Map<ItemStack, Integer> items = new HashMap<>();
                        items.put(cs.upgradeStack, cs.upgradeStack.getCount() * size);
                        upgrade = Util.instance.canRemoveItems(player.inventoryMenu.getItems(), items, false, false);
                        if (!upgrade) { player.sendSystemMessage(Component.translatable("hover.operation.not.items")); }
                     }
                     if (cs.upgradeMoney > 0) {
                        if (upgrade) { upgrade = data.game.getMoney() >= (long) cs.upgradeMoney * (long) size; }
                        if (!upgrade) { player.sendSystemMessage(Component.translatable("hover.operation.not.money")); }
                     }
                     if (cs.upgradeDonat > 0) {
                        if (upgrade) { upgrade = data.game.getDonat() >= cs.upgradeDonat; }
                        if (!upgrade) { player.sendSystemMessage(Component.translatable("hover.operation.not.donat")); }
                     }
                     if (upgrade) {
                        if (!cs.openStack.isEmpty()) { Util.instance.removeItem(player, cs.upgradeStack, cs.upgradeStack.getCount() * size, false, false); }
                        if (cs.upgradeMoney > 0) { data.game.addMoney((long) -cs.upgradeMoney * (long) size); }
                        if (cs.upgradeDonat > 0) { data.game.addDonat((long) -cs.upgradeDonat * (long) size); }
                     }
                  }
                  if (upgrade) {
                     int slot = Math.min(cs.maxCells, inv.getContainerSize() + size);
                     RoleEvent.BankUpgradedEvent event = new RoleEvent.BankUpgradedEvent(player, npc.wrappedNPC, cont.data.bank, slot);
                     update = !event.isCanceled();
                     if (update) {
                        inv.setNewSize(slot);
                        cont.data.setChanged();
                     }
                  }
               } // upgrade
            }
         }
         if (!update) { cont.data.openToPlayer(player, ceil, scrollY, ceilPos, size); }
      }
      CustomNpcs.debugData.end("Packets");
   }

}
