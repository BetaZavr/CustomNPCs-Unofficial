package noppes.npcs.controllers;

import com.google.common.collect.Lists;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.IAuctionHandler;
import noppes.npcs.api.handler.data.IAuction;
import noppes.npcs.api.handler.data.IAuctionLot;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.controllers.data.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAuctionHouseAddLots;
import noppes.npcs.packets.client.PacketAuctionHouseSet;
import noppes.npcs.packets.client.PacketAuctionLotUpdate;
import noppes.npcs.packets.client.PacketGuiUpdate;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class AuctionController implements IAuctionHandler {

    protected static AuctionController instance;

    public static AuctionController getInstance() {
        if (newInstance()) { AuctionController.instance = new AuctionController(); }
        return AuctionController.instance;
    }
    protected static boolean newInstance() {
        if (AuctionController.instance == null) { return true; }
        File file = CustomNpcs.getLevelSaveDirectory("auctions");
        return file != null && !AuctionController.instance.filePath.equals(file.getAbsolutePath());
    }

    protected String filePath;
    public final Map<Integer, AuctionHouse> auctionHouses = new TreeMap<>();
    public final Map<ItemStack, Double[]> statistics = new HashMap<>();

    @Override
    public @Nullable AuctionHouse addAuction() {
        if (auctionHouses.size() >= 16) { return null; }
        AuctionHouse auction = new AuctionHouse(getUnusedHouseId());
        auction.setName("Auction House #" + (auction.getId() + 1));
        auctionHouses.put(auction.getId(), auction);
        return auction;
    }

    @Override
    public void removeAuction(int auctionId) {
        if (auctionId >= 0 && auctionHouses.containsKey(auctionId) && auctionHouses.size() > 1) {
            AuctionHouse auction = auctionHouses.get(auctionId);
            for (ServerPlayer player : auction.getPlayers()) {
                player.closeContainer();
                Packets.send(player, new PacketAuctionHouseSet(auction.getId(), new CompoundTag()));
            }
            auctionHouses.remove(auction.getId());
            Util.instance.removeFile(new File(filePath, auction.getId() + ".dat"));
            updateToAll(auctionId, auction);
        }
    }

    @Override
    public AuctionHouse getAuction(int auctionId) {
        if (auctionId < 0 || !auctionHouses.containsKey(auctionId)) { return null; }
        return auctionHouses.get(auctionId);
    }

    @Override
    public List<IAuction> getAuctions() { return new ArrayList<>(auctionHouses.values()); }

    @SuppressWarnings("unused")
    public int[] getAuctionIDs() {
        int[] arr = new int[auctionHouses.size()];
        int i = 0;
        for (AuctionHouse auction : auctionHouses.values()) {
            arr[i] = auction.getId();
            i++;
        }
        return arr;
    }

    public int getUnusedHouseId() {
        int id = 0;
        while (auctionHouses.containsKey(id)) { id++; }
        return id;
    }

    @Override
    public int getAveragePrice(IItemStack lotItem) {
        return lotItem != null ? getAveragePrice(lotItem.getMCItemStack()) : 0;
    }

    public int getAveragePrice(ItemStack stack) {
        if (NoppesUtilServer.isItemStackNull(stack)) { return 0; }
        for (ItemStack key : statistics.keySet()) {
            if (NoppesUtilPlayer.compareItems(key, stack, false, false)) {
                return (int) (statistics.get(key)[0] * (double) stack.getCount());
            }
        }
        return (int) (100.0d / (double) stack.getMaxStackSize() * (double) stack.getCount());
    }

    @Override
    public void addAveragePrice(IItemStack lotItem, double lotBuyout) {
        if (lotItem != null) { addAveragePrice(lotItem.getMCItemStack(), lotBuyout); }
    }

    public void addAveragePrice(ItemStack stack, double lotBuyout) {
        ItemStack key = null;
        for (ItemStack k : statistics.keySet()) {
            if (NoppesUtilPlayer.compareItems(k, stack, false, false)) {
                key = k;
                break;
            }
        }
        double cost = lotBuyout / (double) stack.getCount();
        if (key == null) {
            key = stack.copy();
            key.setCount(1);
            statistics.put(key, new Double[] { 1.0d, cost });
        }
        else {
            Double[] stats = statistics.get(key);
            stats[0]++;
            stats[1] += (cost - stats[1]) / (stats[0] - 1);
        }
    }

    public void bid(@Nonnull ServerPlayer player, int auctionId, long lotId) {
        AuctionHouse auction = getAuction(auctionId);
        if (auction == null) { return; }
        AuctionLot lot = (AuctionLot) auction.getLot(lotId);
        if (lot == null) { return; }
        // Check bid/buyout availability
        if (!auction.getBidBuyoutAvailability().isAvailable(player)) {
            NoppesUtilServer.sendGuiError(player, 0);
            return;
        }
        PlayerGameData gameData = PlayerData.get(player).game;
        int currentBid = lot.getCurrentBidPrice();
        int nextBid = auction.calculateNextBid(currentBid);
        // Check currency type
        if (player.isCreative() && (lot.getCurrencyType() ? gameData.getMoney() : gameData.getDonat()) < nextBid) {
            player.sendSystemMessage(Component.translatable("hover.operation.not.money"));
            return;
        }
        // Check if already the highest bidder
        if (lot.getCurrentBidderName() != null && lot.getCurrentBidderName().equals(player.getName())) {
            player.sendSystemMessage(Component.translatable("auction.message.bid.already.highest"));
            return;
        }
        // Deduct money
        if (lot.getCurrencyType()) { gameData.addMoney(-nextBid); } else { gameData.addDonat(-nextBid); }
        // Refund previous bidder via mail
        if (lot.getCurrentBidderName() != null && !lot.getCurrentBidderName().isEmpty()) {
            sendBidOutBideLot(auction.getName(), lot.getCurrentBidderName(), lot);
        }
        // Update lot
        lot.setCurrentBidderName(player.getName().getString());
        lot.setCurrentBidPrice(nextBid);
        // Auto buyout if new bid >= buyout price
        if (lot.getBuyoutPrice() > 0 && nextBid >= lot.getBuyoutPrice() && lot.canBuyout()) {
            buyout(player, auctionId, lotId, true);
            return;
        }
        // Notify listeners
        updateToAll(lot.getCreationDate(), auction, lot);
        save(auctionId);
        player.sendSystemMessage(Component.translatable("auction.message.bid.success", lot.getItem().getDisplayName(),
                Util.instance.getTextReducedNumber(nextBid, true, true, false),
                lot.getCurrencyType() ? CustomNpcs.displayCurrencies : CustomNpcs.displayDonation ));
    }

    public void buyout(@Nonnull ServerPlayer player, int auctionId, long lotId, boolean alreadyPaidFor) {
        AuctionHouse auction = getAuction(auctionId);
        if (auction == null) { return; }
        AuctionLot lot = (AuctionLot) auction.getLot(lotId);
        if (lot == null) { return; }
        // Check buyout availability
        if (!auction.getBidBuyoutAvailability().isAvailable(player)) {
            NoppesUtilServer.sendGuiError(player, 0);
            return;
        }
        // Check can buyout
        if (!lot.canBuyout()) {
            NoppesUtilServer.sendGuiError(player, 1);
            return;
        }
        PlayerGameData gameData = PlayerData.get(player).game;
        long buyoutPrice = lot.getBuyoutPrice();
        // Check currency
        if (!alreadyPaidFor && !player.isCreative()) {
            if ((lot.getCurrencyType() ? gameData.getMoney() : gameData.getDonat()) < buyoutPrice) {
                player.sendSystemMessage(Component.translatable("hover.operation.not.money"));
                return;
            }
            if (lot.getCurrencyType()) { gameData.addMoney(-buyoutPrice); } else { gameData.addDonat(-buyoutPrice); }
        }
        // Refund previous bidder
        if (lot.getCurrentBidderName() != null && !lot.getCurrentBidderName().isEmpty()) {
            sendItemToPlayerByName(auction.getName(), lot.getCurrentBidderName(), null,
                    Component.translatable("auction.lot.buyout", lot.getOwnerName(), lot.getItem().getDisplayName()));
        }
        // Send item to buyer via mail
        sendItemToPlayerByName(auction.getName(), lot.getCurrentBidderName(), lot.getItem().getMCItemStack().copy(),
                Component.translatable("auction.lot.buyout", lot.getOwnerName(), lot.getItem().getDisplayName()));
        // Remove lot
        auction.removeLot(lotId);
        updateToAll(lot.getCreationDate(), auction, lot);
        save(auctionId);
    }

    @Override
    public void createLots(IPlayer<?> iPlayer, IAuctionLot lotIn, int auctionId, int count) {
        if (iPlayer == null) { return; }
        createLots((ServerPlayer) iPlayer.getMCEntity(), lotIn, auctionId, count);
    }

    public void createLots(ServerPlayer player, IAuctionLot lotIn, int auctionId, int count) {
        if (player == null) { return; }
        AuctionHouse auction = getAuction(auctionId);
        if (auction == null) { return; }
        // Check lot availability
        if (!auction.getLotAvailability().isAvailable(player)) { return; }
        ItemStack stack = lotIn.getItem().getMCItemStack();
        if (stack.isEmpty()) { return; }
        int maxLots = auction.getMaxLotsInPlayer();
        if (!player.isCreative() && maxLots > 0 && auction.getCountPlayerLots(player.getName().getString()) + count >= maxLots) { return; }
        // Check money only
        int type = auction.getCurrencyType();
        if (type != 0 && (type == 1) == lotIn.getCurrencyType()) { return; }
        // Check forbidden items
        if (auction.isForbiddenItem(stack)) { return; }
        // pay
        if (!player.isCreative()) {
            // remove items
            int countMaxStacks = Util.instance.inventoryItemCount(player, stack, null, false, false);
            if (countMaxStacks < stack.getCount() * count) { return; }
            Util.instance.removeItem(player, stack, stack.getCount() * count, false, false);

            // Check currency <- commission
            PlayerGameData gameData = PlayerData.get(player).game;
            int commission = auction.calculateCommission(lotIn.getBuyoutPrice(), lotIn.getDuration()) * count;
            if ((lotIn.getCurrencyType() ? gameData.getMoney() : gameData.getDonat()) < commission) { return; } // Not money for commission
            if (lotIn.getCurrencyType()) { gameData.addMoney(-commission); } else { gameData.addDonat(-commission); }
        }
        // Statistics
        addAveragePrice(stack, lotIn.getBuyoutPrice());
        // add new lot
        lotIn.setCurrentBidPrice(auction.calculateInitialBid(lotIn.getBuyoutPrice()));
        CompoundTag nbtLot = ((AuctionLot) lotIn).save();
        while (count > 0) {
            AuctionLot lot = auction.addLot();
            long time = lot.getCreationDate();
            lot.load(nbtLot);
            lot.setCreationDate(time);
            // Notify listeners
            updateToAll(lot.getCreationDate(), auction, lot);
            count--;
        }
        save(auctionId);
    }

    public void sendItemToPlayerByName(@Nonnull String houseName, @Nonnull String playerName, @Nullable ItemStack item, Component message) {
        PlayerMail mail = new PlayerMail();
        mail.setSender(Util.instance.getOldFormattedText(Component.translatable("auction.house",
                Util.instance.getOldFormattedText(Component.translatable(houseName)))));
        mail.setSubject(playerName);
        mail.setText(Util.instance.getOldFormattedText(message));
        if (item != null && !item.isEmpty()) { mail.setItem(0, item.copy()); }
        PlayerDataController.instance.addPlayerMessage(null, playerName, mail);
    }

    private void sendBidOutBideLot(@Nonnull String houseName, @Nonnull String playerName, @Nonnull AuctionLot lot) {
        PlayerMail mail = new PlayerMail();
        mail.setSender(Util.instance.getOldFormattedText(Component.translatable("auction.house",
                Util.instance.getOldFormattedText(Component.translatable(houseName)))));
        mail.setSubject(playerName);
        mail.setText(Util.instance.getOldFormattedText(Component.translatable("auction.house", lot.getItem().getDisplayName())));
        if (lot.getCurrencyType()) { mail.money = lot.getCurrentBidPrice(); }
        else { mail.donat = lot.getCurrentBidPrice(); }
        PlayerDataController.instance.addPlayerMessage(null, playerName, mail);
    }

    public void updateToAll(int auctionId, @Nonnull AuctionHouse auction) {
        if (Util.instance.getSide().isDedicatedServer()) {
            for (ServerPlayer player : auction.getPlayers()) { sendAllTo(player, auctionId, auction); }
        }
    }

    public void sendAllTo(@Nonnull ServerPlayer player) {
        for (AuctionHouse auction : auctionHouses.values()) { sendAllTo(player, auction.getId(), auction); }
    }

    private void sendAllTo(@Nonnull ServerPlayer player, int auctionId, @Nonnull AuctionHouse auction) {
        CompoundTag compound = auction.save();
        compound.remove("Lots");
        compound.putBoolean("AvailabilityLot", auction.lotAvailability.isAvailable(player));
        compound.putBoolean("AvailabilityBuyout", auction.bidBuyoutAvailability.isAvailable(player));
        List<PacketAuctionHouseAddLots> lotPackets = new ArrayList<>();
        for (List<IAuctionLot> chunk : Lists.partition(Arrays.asList(auction.getLots()), 100)) {
            ListTag list = new ListTag();
            for (IAuctionLot lot : chunk) {
                list.add(((AuctionLot) lot).save());
            }
            CompoundTag lotCompound = new CompoundTag();
            lotCompound.put("Lots", list);
            lotPackets.add(new PacketAuctionHouseAddLots(auction.getId(), lotCompound));
        }
        Packets.send(player, new PacketAuctionHouseSet(auctionId, compound)); // send auction data
        for (PacketAuctionHouseAddLots lotPacket : lotPackets) { Packets.send(player, lotPacket); } // send all lots
        Packets.send(player, new PacketGuiUpdate());
    }

    public void updateToAll(long lotId, @Nonnull AuctionHouse auction, @Nullable AuctionLot lot) {
        if (Util.instance.getSide().isDedicatedServer()) {
            CompoundTag compound = lot == null ? new CompoundTag() : lot.save();
            for (ServerPlayer player : auction.getPlayers()) {
                Packets.send(player, new PacketAuctionLotUpdate(auction.getId(), lotId, compound));
            }
        }
    }

    public void load() {
        File saveDir = CustomNpcs.getLevelSaveDirectory("auctions");
        if (saveDir == null || (!saveDir.exists() && !saveDir.mkdirs())) { return; }
        filePath = saveDir.getAbsolutePath();
        auctionHouses.clear();
        statistics.clear();
        File[] list = saveDir.listFiles();
        if (list != null) {
            for (File file : list) {
                if (file.getName().endsWith(".dat")) {
                    try {
                        CompoundTag compound = NbtIo.readCompressed(file);
                        if (file.getName().equals("statistics.dat")) {
                            ListTag nbtList = compound.getList("Statistics", 10);
                            for (int i = 0; i < nbtList.size(); i++) {
                                CompoundTag stat = nbtList.getCompound(i);
                                statistics.put(ItemStack.of(stat.getCompound("Key")),
                                        new Double[] { stat.getDouble("Count"), stat.getDouble("Cost") });
                            }
                        } else {
                            AuctionHouse auction = new AuctionHouse(compound.getInt("ID"));
                            auction.load(compound, true);
                            auctionHouses.put(auction.getId(), auction);
                        }
                    } catch (Exception e) {
                        LogWriter.error("Error loading auction house file: " + file.getName(), e);
                    }
                }
            }
        }
        if (auctionHouses.isEmpty()) {
            AuctionHouse auction = addAuction();
            if (auction != null) {
                auction.setName("Default Auction");
                auction.addForbiddenItem(new ItemStack(Blocks.COBBLESTONE), false, false);
                auction.addForbiddenItem(new ItemStack(Blocks.STONE), false, false);
            }
        }
    }

    public void save() {
        for (int auctionId : auctionHouses.keySet()) { save(auctionId); }
        try {
            File saveDir = CustomNpcs.getLevelSaveDirectory("auctions");
            CompoundTag nbt = new CompoundTag();
            ListTag list = new ListTag();
            for (Map.Entry<ItemStack, Double[]> entry : statistics.entrySet()) {
                CompoundTag stat = new CompoundTag();
                stat.put("Key", entry.getKey().save(new CompoundTag()));
                stat.putDouble("Count", entry.getValue()[0]);
                stat.putDouble("Cost", entry.getValue()[1]);
            }
            nbt.put("Statistics", list);
            NbtIo.writeCompressed(nbt, new File(saveDir, "statistics.dat"));
        } catch (Exception e) {
            LogWriter.error("Error saving auction statistics", e);
        }
    }

    public void save(int auctionId) {
        if (CustomNpcs.Server == null || !auctionHouses.containsKey(auctionId)) { return; }
        try {
            File saveDir = CustomNpcs.getLevelSaveDirectory("auctions");
            AuctionHouse auction = auctionHouses.get(auctionId);
            NbtIo.writeCompressed(auction.save(), new File(saveDir, auction.getId() + ".dat"));
        } catch (Exception e) {
            LogWriter.error("Error saving auction house ID: " + auctionId, e);
        }
    }

    public void update() {
        if (CustomNpcs.Server != null) {
            for (AuctionHouse auction : new ArrayList<>(auctionHouses.values())) { auction.update(); }
        }
    }

}
