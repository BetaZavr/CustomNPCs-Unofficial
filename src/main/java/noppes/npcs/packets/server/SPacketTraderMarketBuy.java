package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
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
import noppes.npcs.mixin.world.entity.IEntityMixin;
import noppes.npcs.packets.client.PacketOpenCase;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketUpdateMarcetGui;
import noppes.npcs.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SPacketTraderMarketBuy extends PacketServerBasic {

    protected static int channelId;
    private final int marcetID;
    private final int dealID;
    private final int npcID;
    private final int count;

    public SPacketTraderMarketBuy(int marcetIDIn, int dealIDIn, int npcIDIn, int countIn) {
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

    public static void encode(SPacketTraderMarketBuy msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.marcetID);
        buf.writeInt(msg.dealID);
        buf.writeInt(msg.npcID);
        buf.writeInt(msg.count);
    }

    public static SPacketTraderMarketBuy decode(FriendlyByteBuf buf) {
        return new SPacketTraderMarketBuy(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
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
        if (deal == null || deal.getType() == 1) {
            Packets.send(player, new PacketUpdateMarcetGui());
            return;
        }
        PlayerData data = PlayerData.get(player);
        DealMarkup dm = MarcetController.getInstance().getBuyData(marcet, deal, data.game.getMarcetLevel(marcet.getId()), count);
        boolean notGiveItem = !deal.availability.isAvailable(player) ||
                dm.buyMoney > data.game.getMoney() || // has money
                dm.buyDonat > data.game.getDonat() || // has donal
                !Util.instance.canRemoveItems(player.getInventory().items, dm.buyItems, dm.ignoreDamage, dm.ignoreNBT); // has items
        if (marcet.isLimited) {
            boolean notBuy = false;
            Map<ItemStack, Integer> mainItem = new LinkedHashMap<>();
            mainItem.put(dm.main, dm.count);
            if (!deal.isCase() && !Util.instance.canRemoveItems(marcet.inventory, mainItem, dm.ignoreDamage, dm.ignoreNBT)) { notBuy = true; }
            if (notBuy) {
                marcet.detectAndSendChanges();
                Packets.send(player, new PacketUpdateMarcetGui());
                player.sendSystemMessage(Component.translatable("marcet.message.not.deal"));
                return;
            }
            marcet.money += dm.sellMoney;
            if (!deal.isCase()) { marcet.removeInventoryItems(mainItem); }
        }
        npc = null;
        Entity entity = player.level().getEntity(npcID);
        if (entity instanceof EntityNPCInterface npcIn) { npc = npcIn; }

        if (!player.isCreative()) {
            if (notGiveItem) {
                if (npc != null) { EventHooks.onNPCRole(npc, new RoleEvent.TradeFailedEvent(player, npc.wrappedNPC, dm.main, dm.buyItems)); }
                Packets.send(player, new PacketUpdateMarcetGui());
                return;
            }
            data.game.addMoney(-1 * dm.buyMoney); // remove money
            data.game.addDonat(-1 * dm.buyDonat); // remove donat
            // Del Items
            for (ItemStack st : dm.buyItems.keySet()) { Util.instance.removeItem(player, st, dm.buyItems.get(st), dm.ignoreDamage, dm.ignoreNBT); }
        }
        boolean bo;
        if (deal.isCase()) {
            List<ItemStack> addedTo = new ArrayList<>();
            double baseChance = 1.0d;
            AttributeInstance l = player.getAttribute(Attributes.LUCK);
            if (l != null) {
                double luck = l.getValue();
                if (luck != 0.0d) {
                    if (luck < 0) {
                        luck *= -1;
                        baseChance -= luck * luck * -0.005555d + luck * 0.255555d; // 1lv = 25%$ 10lv = 200%
                    }
                    else { baseChance += luck * luck * -0.005555d + luck * 0.255555d; } // 1lv = 25%$ 10lv = 200%
                }
            }
            for (int i = 0; i < count; i++) { addedTo.addAll(deal.createCaseItems(baseChance)); }
            for (ItemStack stack : addedTo) { spawnItem(stack); }
            bo = !addedTo.isEmpty();
            if (bo) { Packets.send(player, new PacketOpenCase(dealID, addedTo)); }
        }
        else {
            bo = true;
            ItemStack stack = dm.main.copy();
            if (dm.count > dm.main.getMaxStackSize()) {
                while (dm.count > 0) {
                    stack = dm.main.copy();
                    stack.setCount(Math.min(dm.count, dm.main.getMaxStackSize()));
                    dm.count -= dm.main.getMaxStackSize();
                    spawnItem(stack);
                }
            }
            else {
                stack.setCount(dm.count);
                spawnItem(stack);
            }
        }
        if (bo) {
            if (deal.getMaxCount() != 0) { deal.setAmount(deal.getAmount() - dm.count); }
            if (CustomNpcs.SendMarcetInfo) { player.sendSystemMessage(Component.translatable("mes.market.buy", dm.main.getDisplayName() + " x" + dm.count)); }
            NoppesUtilServer.playSound(player, SoundEvents.ITEM_PICKUP, 0.2f, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7f + 1.0f) * 2.0f);
            Util.instance.updatePlayerInventory(player);
            data.game.addMarkupXP(marcet.getId(), 5 * count);
            if (npc != null) { EventHooks.onNPCRole(npc, new RoleEvent.TraderEvent(player, npc.wrappedNPC, dm.main, dm.buyItems)); }
            marcet.detectAndSendChanges();
        }
        else if (npc != null) { EventHooks.onNPCRole(npc, new RoleEvent.TradeFailedEvent(player, npc.wrappedNPC, dm.main, dm.buyItems)); }
        Packets.send(player, new PacketUpdateMarcetGui());
        CustomNpcs.debugData.end("Packets");
    }

    private void spawnItem(ItemStack stack) {
        ItemEntity ie = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack, 0, 0, 0);
        ie.lifespan = 100;
        ((IEntityMixin) ie).setRemoval(null);
        player.level().addFreshEntity(ie);
    }

}