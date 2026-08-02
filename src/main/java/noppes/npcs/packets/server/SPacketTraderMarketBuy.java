package noppes.npcs.packets.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
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
    private int marcetID;
    private int dealID;
    private int npcID;
    private int count;

    public SPacketTraderMarketBuy() { }

    public SPacketTraderMarketBuy(int marcetIDIn, int dealIDIn, int npcIDIn, int countIn) {
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
    @SuppressWarnings("ConstantConditions")
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Marcet marcet = MarcetController.getInstance().getMarcet(marcetID);
        if (marcet == null || marcet.notHasListener(player)) {
            Packets.send(player, new PacketUpdateMarcetGui());
            CustomNpcs.debugData.end("Packets");
            return;
        }
        Deal deal = marcet.getDeal(dealID);
        if (deal == null || deal.getType() == 1) {
            Packets.send(player, new PacketUpdateMarcetGui());
            CustomNpcs.debugData.end("Packets");
            return;
        }
        PlayerData data = PlayerData.get(player);
        DealMarkup dm = MarcetController.getInstance().getBuyData(marcet, deal, data.game.getMarcetLevel(marcet.getId()), count);
        if (!deal.isCase() && (dm.main.isEmpty() || dm.count <= 0)) {
            Packets.send(player, new PacketUpdateMarcetGui());
            CustomNpcs.debugData.end("Packets");
            return;
        }
        boolean notGiveItem = !deal.availability.isAvailable(player) ||
                dm.buyMoney > data.game.getMoney() || // has money
                dm.buyDonat > data.game.getDonat() || // has donal
                !Util.instance.canRemoveItems(player.inventory.mainInventory, dm.buyItems, dm.ignoreDamage, dm.ignoreNBT); // has items
        Map<ItemStack, Integer> mainItem = new LinkedHashMap<>();
        mainItem.put(dm.main, dm.count);
        if (marcet.isLimited && !deal.isCase() && !Util.instance.canRemoveItems(marcet.inventory, mainItem, dm.ignoreDamage, dm.ignoreNBT)) {
            marcet.detectAndSendChanges();
            Packets.send(player, new PacketUpdateMarcetGui());
            player.sendMessage(Component.translatable("marcet.message.not.deal").getParent());
            CustomNpcs.debugData.end("Packets");
            return;
        }
        npc = null;
        Entity entity = player.world.getEntityByID(npcID);
        if (entity instanceof EntityNPCInterface) { npc = (EntityNPCInterface) entity; }

        if (!player.isCreative()) {
            if (notGiveItem) {
                if (npc != null) { EventHooks.onNPCRole(npc, new RoleEvent.TradeFailedEvent(player, npc.wrappedNPC, dm.main, dm.buyItems)); }
                Packets.send(player, new PacketUpdateMarcetGui());
                CustomNpcs.debugData.end("Packets");
                return;
            }
            data.game.addMoney(-1 * dm.buyMoney); // remove money
            data.game.addDonat(-1 * dm.buyDonat); // remove donat
            // Del Items
            for (ItemStack st : dm.buyItems.keySet()) { Util.instance.removeItem(player, st, dm.buyItems.get(st), dm.ignoreDamage, dm.ignoreNBT); }
        }
        if (marcet.isLimited) {
            marcet.money += dm.buyMoney;
            if (!deal.isCase()) { marcet.removeInventoryItems(mainItem); }
        }
        boolean bo;
        if (deal.isCase()) {
            List<ItemStack> addedTo = new ArrayList<>();
            double baseChance = 1.0d;
            IAttributeInstance l = player.getEntityAttribute(SharedMonsterAttributes.LUCK);
            if (l != null) {
                double luck = l.getAttributeValue();
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
            if (CustomNpcs.SendMarcetInfo) { player.sendMessage(Component.translatable("mes.market.buy", dm.main.getDisplayName() + " x" + dm.count).getParent()); }
            NoppesUtilServer.playSound(player, SoundEvents.ENTITY_ITEM_PICKUP, 0.2f, ((player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.7f + 1.0f) * 2.0f);
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
        if (stack == null || stack.isEmpty()) { return; }
        ItemStack rest = stack.copy();
        player.inventory.addItemStackToInventory(rest);
        if (!rest.isEmpty()) {
            EntityItem ie = new EntityItem(player.world, player.posX, player.posY, player.posZ, rest);
            ie.setPickupDelay(0);
            ie.isDead = false;
            player.world.spawnEntity(ie);
        }
        Util.instance.updatePlayerInventory(player);
    }

}