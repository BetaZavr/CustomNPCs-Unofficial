package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilServer;
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
    private final int marcetID;
    private final int dealID;
    private final int npcID;
    private final int count;

    public SPacketTraderMarketSell(int marcetIDIn, int dealIDIn, int npcIDIn, int countIn) {
        marcetID = marcetIDIn;
        dealID = dealIDIn;
        npcID = npcIDIn;
        count = countIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketTraderMarketSell msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.marcetID);
        buf.writeInt(msg.dealID);
        buf.writeInt(msg.npcID);
        buf.writeInt(msg.count);
    }

    public static SPacketTraderMarketSell decode(FriendlyByteBuf buf) {
        return new SPacketTraderMarketSell(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Marcet marcet = MarcetController.getInstance().getMarcet(marcetID);
        if (marcet == null || marcet.notHasListener(player)) {
            Packets.send(player, new PacketUpdateMarcetGui());
            return;
        }
        Deal deal = marcet.getDeal(dealID);
        if (deal == null || deal.getType() == 0 ||
                ((deal.isCase() && deal.getCaseItems().length == 0) || (!deal.isCase() && deal.getProduct().isEmpty()))) {
            Packets.send(player, new PacketUpdateMarcetGui());
            return;
        }
        PlayerData data = PlayerData.get(player);
        DealMarkup dm = MarcetController.getInstance().getBuyData(marcet, deal, data.game.getMarcetLevel(marcet.getId()), count);
        Entity entity = player.level().getEntity(npcID);
        if (entity instanceof EntityNPCInterface npcIn) { npc = npcIn; }
        if (marcet.isLimited) {
            boolean notSell = marcet.money < dm.sellMoney;
            if (!notSell && !dm.sellItems.isEmpty() &&
                    !Util.instance.canRemoveItems(marcet.inventory, dm.sellItems, dm.ignoreDamage, dm.ignoreNBT)) { notSell = true; }
            if (notSell) {
                marcet.detectAndSendChanges();
                Packets.send(player, new PacketUpdateMarcetGui());
                player.sendSystemMessage(Component.translatable("marcet.message.not.deal"));
                return;
            }
            marcet.money -= dm.sellMoney;
            Map<ItemStack, Integer> mainItem = new LinkedHashMap<>();
            mainItem.put(dm.main, dm.count);
            marcet.addInventoryItems(mainItem);
            marcet.removeInventoryItems(dm.sellItems);
        }
        if (player.isCreative() || Util.instance.removeItem(player, dm.main, dm.ignoreDamage, dm.ignoreNBT)) {
            // Add Items
            if (!dm.sellItems.isEmpty()) {
                boolean change = false;
                for (ItemStack st : dm.sellItems.keySet()) {
                    int c = dm.sellItems.get(st);
                    while (c > 0) {
                        ItemStack stc = st.copy();
                        stc.setCount(Math.min(c, st.getMaxStackSize()));
                        c -= st.getMaxStackSize();
                        if (player.getInventory().add(stc)) { change = true; }
                    }
                }
                if (change) {
                    NoppesUtilServer.playSound(player, SoundEvents.ITEM_PICKUP, 0.2f, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                    Util.instance.updatePlayerInventory(player);
                }
            }
            // Add Money
            if (dm.sellMoney > 0) { data.game.addMoney(dm.sellMoney); }
            if (deal.getMaxCount() != 0) { deal.setAmount(deal.getAmount() + dm.count); }
            if (CustomNpcs.SendMarcetInfo) { player.sendSystemMessage(Component.translatable("mes.market.sell", dm.main.getDisplayName() + " x" + dm.count)); }
            data.game.addMarkupXP(marcet.getId(), count);
            if (npc != null) { EventHooks.onNPCRole(npc, new RoleEvent.TraderEvent(player, npc.wrappedNPC, dm.main, dm.sellItems)); }
            marcet.detectAndSendChanges();
        }
        else if (npc != null) { EventHooks.onNPCRole(npc, new RoleEvent.TradeFailedEvent(player, npc.wrappedNPC, dm.main, dm.sellItems)); }
        Packets.send(player, new PacketUpdateMarcetGui());
        CustomNpcs.debugData.end("Packets");
    }

}