package noppes.npcs.packets.server;

import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import noppes.npcs.*;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.data.Deal;
import noppes.npcs.controllers.data.DealMarkup;
import noppes.npcs.controllers.data.Marcet;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketUpdateMarcetGui;
import noppes.npcs.util.Util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SPacketTraderMarketSell extends PacketServerBasic {

    protected static int channelId;
    private int marcetID;
    private int dealID;
    private int npcID;
    private int count;

    public SPacketTraderMarketSell() { }

    public SPacketTraderMarketSell(int marcetIDIn, int dealIDIn, int npcIDIn, int countIn) {
        marcetID = marcetIDIn;
        dealID = dealIDIn;
        npcID = npcIDIn;
        count = countIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<CustomNpcsPermissions.Permission> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(marcetID);
        buf.writeInt(dealID);
        buf.writeInt(npcID);
        buf.writeInt(count);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        marcetID = buf.readInt();
        dealID = buf.readInt();
        npcID = buf.readInt();
        count = buf.readInt();
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Marcet marcet = MarcetController.getInstance().getMarcet(marcetID);
        if (marcet == null || marcet.notHasListener(player)) {
            Packets.send(player, new PacketUpdateMarcetGui());
            CustomNpcs.debugData.end("Packets");
            return;
        }
        Deal deal = marcet.getDeal(dealID);
        if (deal == null || deal.getType() == 0 ||
                ((deal.isCase() && deal.getCaseItems().length == 0) || (!deal.isCase() && deal.getProduct().isEmpty()))) {
            Packets.send(player, new PacketUpdateMarcetGui());
            CustomNpcs.debugData.end("Packets");
            return;
        }
        PlayerData data = PlayerData.get(player);
        DealMarkup dm = MarcetController.getInstance().getBuyData(marcet, deal, data.game.getMarcetLevel(marcet.getId()), count);
        Entity entity = player.world.getEntityByID(npcID);
        if (entity instanceof EntityNPCInterface) { npc = (EntityNPCInterface) entity; }
        Map<ItemStack, Integer> mainItem = new LinkedHashMap<>();
        mainItem.put(dm.main, dm.count);
        if (marcet.isLimited) {
            boolean notSell = marcet.money < dm.sellMoney;
            if (!notSell && !dm.sellItems.isEmpty() &&
                    !Util.instance.canRemoveItems(marcet.inventory, dm.sellItems, dm.ignoreDamage, dm.ignoreNBT)) { notSell = true; }
            if (notSell) {
                marcet.detectAndSendChanges();
                Packets.send(player, new PacketUpdateMarcetGui());
                player.sendMessage(Component.translatable("marcet.message.not.deal").getParent());
                CustomNpcs.debugData.end("Packets");
                return;
            }
        }
        if (player.isCreative() ||
                (Util.instance.canRemoveItems(player.inventory.mainInventory, mainItem, dm.ignoreDamage, dm.ignoreNBT)
                        && Util.instance.removeItem(player, dm.main, dm.count, dm.ignoreDamage, dm.ignoreNBT))) {
            if (marcet.isLimited) {
                marcet.addInventoryItems(mainItem);
                marcet.removeInventoryItems(dm.sellItems);
            }
            // Add Items
            if (!dm.sellItems.isEmpty()) {
                boolean change = false;
                for (ItemStack st : dm.sellItems.keySet()) {
                    int c = dm.sellItems.get(st);
                    while (c > 0) {
                        ItemStack stc = st.copy();
                        stc.setCount(Math.min(c, st.getMaxStackSize()));
                        c -= st.getMaxStackSize();
                        if (player.inventory.addItemStackToInventory(stc)) { change = true; }
                    }
                }
                if (change) {
                    NoppesUtilServer.playSound(player, SoundEvents.ENTITY_ITEM_PICKUP, 0.2f, ((player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                    Util.instance.updatePlayerInventory(player);
                }
            }
            // Add Money
            if (dm.sellMoney > 0) {
                data.game.addMoney(dm.sellMoney);
                marcet.money -= dm.sellMoney;
            }
            if (deal.getMaxCount() != 0) { deal.setAmount(deal.getAmount() + dm.count); }
            if (CustomNpcs.SendMarcetInfo) { player.sendMessage(Component.translatable("mes.market.sell", dm.main.getDisplayName() + " x" + dm.count).getParent()); }
            data.game.addMarkupXP(marcet.getId(), count);
            if (npc != null) { EventHooks.onNPCRole(npc, new RoleEvent.TraderEvent(player, npc.wrappedNPC, dm.main, dm.sellItems)); }
            marcet.detectAndSendChanges();
        }
        else if (npc != null) { EventHooks.onNPCRole(npc, new RoleEvent.TradeFailedEvent(player, npc.wrappedNPC, dm.main, dm.sellItems)); }
        Packets.send(player, new PacketUpdateMarcetGui());
        CustomNpcs.debugData.end("Packets");
    }

}