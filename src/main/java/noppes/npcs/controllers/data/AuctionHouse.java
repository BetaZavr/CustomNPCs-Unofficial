package noppes.npcs.controllers.data;

import java.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.handler.data.IAuction;
import noppes.npcs.api.handler.data.IAuctionLot;
import noppes.npcs.api.handler.data.IForbiddenItem;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.ValueUtil;

public class AuctionHouse implements IAuction {

    protected final int id;
    protected final List<ForbiddenItem> forbiddenItems = new ArrayList<>();
    protected final Set<ServerPlayer> listener = new HashSet<>();
    protected final Map<Long, AuctionLot> lots = new LinkedHashMap<>();
    protected int textureType = 0;

    public String name = "Default Auction";
    /** min - > max */
    public long[] delays = new long[] { 30L * 60L * 1000L, 24L * 60L * 60L * 1000L }; // 10 minutes <-> 3 days
    /** min - > max */
    public double[] commission = new double[] { 6.0, 2.5 }; // 0.5% <-> 15%
    public double bidRaise = 4.0;   // 2 - 10
    public double initialBid = 10.0; // 5 - 50
    public int maxLotsInPlayer = 50; // >= 0
    public int currencyType = 0;  // 0 - all; 1 = money, 2 = donat
    public Availability lotAvailability = new Availability();
    public Availability bidBuyoutAvailability = new Availability();

    public boolean clientAvailabilityLot = true;
    public boolean clientAvailabilityBuyout = true;

    public AuctionHouse(int idIn) { id = idIn; }

    @Override
    public int getId() { return id; }

    @Override
    public AuctionLot addLot() {
        long time = System.currentTimeMillis();
        while (lots.containsKey(time)) { time++; }
        AuctionLot lot = new AuctionLot(System.currentTimeMillis());
        addLot(lot);
        return lot;
    }

    public void addLot(AuctionLot lot) {
        if (lot != null) { lots.put(lot.getCreationDate(), lot); }
    }

    @Override
    public String getName() { return name; }

    @Override
    public void setName(String nameIn) { name = nameIn != null && !nameIn.isEmpty() ? nameIn : "Auction House"; }

    @Override
    public double[] getCommission() { return commission; }

    @Override
    public void setCommission(double percentMin, double percentMax) {
        double min = percentMin;
        percentMin = Math.min(percentMin, percentMax);
        percentMax = Math.max(min, percentMax);
        commission[0] = ValueUtil.correctDouble(percentMax, 0.5, 15.0);
        commission[1] = ValueUtil.correctDouble(percentMin, 0.5, 15.0);
    }

    @Override
    public long[] getDelays() { return delays; }

    @Override
    public void setDelays(long delayMin, long delayMax) {
        long min = delayMin;
        delayMin = Math.min(delayMin, delayMax);
        delayMax = Math.max(min, delayMax);
        delays[0] = ValueUtil.correctLong(delayMin, 10L * 60L * 1000L, 3L * 24L * 60L * 60L * 1000L);
        delays[1] = ValueUtil.correctLong(delayMax, 10L * 60L * 1000L, 3L * 24L * 60L * 60L * 1000L);
    }

    @Override
    public double getBidRaise() { return bidRaise; }

    @Override
    public void setBidRaise(double percent) { bidRaise = ValueUtil.correctDouble(percent, 2.0, 10.0); }

    @Override
    public double getInitialBid() { return initialBid; }

    @Override
    public void setInitialBid(double percent) { initialBid = ValueUtil.correctDouble(percent, 5.0, 50.0); }

    @Override
    public int getMaxLotsInPlayer() { return maxLotsInPlayer; }

    @Override
    public void setMaxLotsInPlayer(int max) { maxLotsInPlayer = ValueUtil.onlyPositiveInt(max, Integer.MAX_VALUE); }

    @Override
    public IForbiddenItem[] getForbiddenItems() {
        NpcAPI api = NpcAPI.Instance();
        if (api == null) { return new IForbiddenItem[0]; }
        return forbiddenItems.toArray(new IForbiddenItem[0]);
    }

    @Override
    public void addForbiddenItem(IItemStack item, boolean ignoreDamage, boolean ignoreNBT) {
        if (item != null) { addForbiddenItem(item.getMCItemStack(), ignoreDamage, ignoreNBT); }
    }

    public void addForbiddenItem(ItemStack stack, boolean ignoreDamage, boolean ignoreNBT) {
        forbiddenItems.add(new ForbiddenItem(stack, ignoreDamage, ignoreNBT));
    }

    @Override
    public void setForbiddenItems(IItemStack[] items) {
        forbiddenItems.clear();
        for (IItemStack iStack : items) { addForbiddenItem(iStack, false, false); }
    }

    @Override
    public void setForbiddenItems(IForbiddenItem[] items) {
        forbiddenItems.clear();
        if (items != null) {
            for (IForbiddenItem forbiddenItem : items) {
                if (forbiddenItem instanceof ForbiddenItem) { forbiddenItems.add((ForbiddenItem) forbiddenItem); }
            }
        }
    }

    @Override
    public Availability getLotAvailability() { return lotAvailability; }

    @Override
    public Availability getBidBuyoutAvailability() { return bidBuyoutAvailability; }

    @Override
    public int getCurrencyType() { return currencyType; }

    @Override
    public void setCurrencyType(int currencyTypeIn) { currencyType = ValueUtil.correctInt(currencyTypeIn, 0, 2); }

    @Override
    public void removeLot(long lotId) {
        AuctionLot lot = lots.remove(lotId);
        if (lot != null) {
            AuctionController.getInstance().updateToAll(lot.getCreationDate(), this, lot);
        }
    }

    @Override
    public IAuctionLot getLot(long lotId) { return lots.get(lotId); }

    @Override
    public IAuctionLot[] getLots() { return lots.values().toArray(new IAuctionLot[0]); }

    public Collection<AuctionLot> getRawLots() { return lots.values(); }

    @Override
    public int getCountPlayerLots(String name) {
        int i = 0;
        for (AuctionLot lot : lots.values()) {
            if (lot.getOwnerName().equals(name)) { i++;}
        }
        return i;
    }

    @Override
    public int getTextureType() { return textureType; }

    @Override
    public void setTextureType(int type) { textureType = ValueUtil.correctInt(type, 0, 2); }

    public int calculateCommission(double prise, long duration) {
        if (prise <= 0) { return 1; }
        double timePercent = (double) (duration - delays[0]) / (double) (delays[1] - delays[0]);
        double commissionPercent = (1.0 +(commission[0] + (commission[1] - commission[0]) * timePercent)) / 100.0;
        return ValueUtil.correctInt((int) (prise * commissionPercent), 1, Integer.MAX_VALUE);
    }

    public int calculateNextBid(int prise) {
        if (prise <= 0) { return 1; }
        int nextPrise = ValueUtil.correctInt((int) ((double) prise * (1.0 + bidRaise / 100.0)), 1, Integer.MAX_VALUE);
        return nextPrise == prise ? nextPrise + 1 : nextPrise;
    }

    public int calculateInitialBid(int prise) {
        if (prise <= 0) { return 1; }
        return ValueUtil.correctInt((int) ((double) prise * initialBid / 100.0), 1, Integer.MAX_VALUE);
    }

    public void load(CompoundTag compound, boolean loadLots) {
        setName(compound.getString("Name"));
        setDelays(compound.getLong("DelaysMin"), compound.getLong("DelaysMax"));
        setCommission(compound.getDouble("CommissionMin"), compound.getDouble("CommissionMax"));
        setCurrencyType(compound.getInt("CurrencyType"));
        setBidRaise(compound.getDouble("BidRaise"));
        setInitialBid(compound.getDouble("InitialBid"));
        setMaxLotsInPlayer(compound.getInt("MaxLotsInPlayer"));
        setTextureType(compound.getInt("TextureType"));
        if (compound.contains("LotAvailability", 10)) { lotAvailability.load(compound.getCompound("LotAvailability")); }
        if (compound.contains("BidBuyoutAvailability", 10)) { bidBuyoutAvailability.load(compound.getCompound("BidBuyoutAvailability")); }
        forbiddenItems.clear();
        if (compound.contains("ForbiddenItems", 9)) {
            ListTag list = compound.getList("ForbiddenItems", 10);
            for (int i = 0; i < list.size(); i++) {
                forbiddenItems.add(new ForbiddenItem(list.getCompound(i)));
            }
        }
        if (loadLots) {
            lots.clear();
            if (compound.contains("Lots", 9)) {
                ListTag list = compound.getList("Lots", 10);
                for (int i = 0; i < list.size(); i++) {
                    AuctionLot lot = new AuctionLot(0);
                    lot.load(list.getCompound(i));
                    lots.put(lot.getCreationDate(), lot);
                }
            }
        }
    }

    public CompoundTag save() {
        CompoundTag compound = new CompoundTag();
        compound.putInt("ID", id);
        compound.putInt("TextureType", textureType);
        compound.putString("Name", name);
        compound.putLong("DelaysMin", delays[0]);
        compound.putLong("DelaysMax", delays[1]);
        compound.putDouble("CommissionMin", commission[0]);
        compound.putDouble("CommissionMax", commission[1]);
        compound.putDouble("BidRaise", bidRaise);
        compound.putDouble("InitialBid", initialBid);
        compound.putInt("CurrencyType", currencyType);
        compound.putInt("MaxLotsInPlayer", maxLotsInPlayer);
        compound.put("LotAvailability", lotAvailability.save(new CompoundTag()));
        compound.put("BidBuyoutAvailability", bidBuyoutAvailability.save(new CompoundTag()));
        ListTag list = new ListTag();
        for (ForbiddenItem forbiddenItem : forbiddenItems) { list.add(forbiddenItem.save()); }
        compound.put("ForbiddenItems", list);
        list = new ListTag();
        for (AuctionLot lot : lots.values()) { list.add(lot.save()); }
        compound.put("Lots", list);
        return compound;
    }

    public Set<ServerPlayer> getPlayers() { return listener; }

    public void addPlayer(ServerPlayer player) { listener.add(player); }

    public void removePlayer(ServerPlayer player) { listener.remove(player); }

    public AuctionHouse copy() {
        AuctionHouse auction = new AuctionHouse(id);
        auction.load(save(), true);
        return auction;
    }

    @OnlyIn(Dist.CLIENT)
    public Component getKey() {
        return Component.empty()
                .append(Component.literal("ID:" + id + " - ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable(name).withStyle(ChatFormatting.RESET));
    }

    public ForbiddenItem getForbiddenItem(int pos) {
        if (pos >= 0 && pos < forbiddenItems.size()) { return forbiddenItems.get(pos); }
        return null;
    }

    public boolean isForbiddenItem(ItemStack stack) {
        for (ForbiddenItem forbiddenItem : forbiddenItems) {
            if (NoppesUtilPlayer.compareItems(forbiddenItem.inventory.getItem(0), stack, forbiddenItem.ignoreDamage, forbiddenItem.ignoreNBT)) {
                return true;
            }
        }
        return false;
    }

    public void update() {
        CustomNPCsScheduler.runTack(() -> {
            List<Long> lotsToRemove = new ArrayList<>();
            for (Map.Entry<Long, AuctionLot> entry : lots.entrySet()) {
                if (entry.getValue().getCreationDate() + entry.getValue().getDuration() <= System.currentTimeMillis()) { // check time
                    lotsToRemove.add(entry.getKey());
                    processExpiredLot(entry.getValue());
                }
            }
            if (!lotsToRemove.isEmpty()) {
                for (long lotId : lotsToRemove) { removeLot(lotId); }
                AuctionController.getInstance().updateToAll(id, this);
                AuctionController.getInstance().save(id);
            }
        });
    }

    private void processExpiredLot(AuctionLot lot) {
        ItemStack stack = lot.getItem().getMCItemStack();
        if (lot.getCurrentBidderName() != null && !lot.getCurrentBidderName().isEmpty()) {
            AuctionController.getInstance().sendItemToPlayerByName(name, lot.getCurrentBidderName(), stack.copy(),
                    Component.translatable("auction.lot.buyout", lot.getOwnerName(), stack.getDisplayName()));
        }
        else {
            AuctionController.getInstance().sendItemToPlayerByName(name, lot.getOwnerName(), stack.copy(),
                    Component.translatable("auction.lot.not.sold"));
        }
    }

    public static class ForbiddenItem implements IForbiddenItem {

        public boolean ignoreDamage;
        public boolean ignoreNBT;
        public NpcMiscInventory inventory = new NpcMiscInventory(1);

        public ForbiddenItem(ItemStack stack, boolean ignoreDamageIn, boolean ignoreNBTIn) {
            setItem(stack);
            ignoreDamage = ignoreDamageIn;
            ignoreNBT = ignoreNBTIn;
        }

        public ForbiddenItem(CompoundTag compound) { load(compound); }

        @Override
        public IItemStack getItem() { return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(inventory.getItem(0)); }

        @Override
        public void setItem(IItemStack iStack) {
            setItem(iStack == null ? ItemStack.EMPTY: iStack.getMCItemStack());
        }

        public void setItem(ItemStack stack) {
            inventory.setItem(0, stack == null ? ItemStack.EMPTY: stack);
        }

        @Override
        public boolean isIgnoreDamage() { return ignoreDamage; }

        @Override
        public void setIsIgnoreDamage(boolean ignore) { ignoreDamage = ignore; }

        @Override
        public boolean isIgnoreNBT() { return ignoreNBT; }

        @Override
        public void setIsIgnoreNBT(boolean ignore) { ignoreNBT = ignore; }

        public void load(CompoundTag compound) {
            if (compound != null) {
                setItem(ItemStack.of(compound.getCompound("Item")));
                ignoreDamage = compound.getBoolean("IgnoreDamage");
                ignoreNBT = compound.getBoolean("IgnoreNBT");
            }
        }

        public CompoundTag save() {
            CompoundTag compound = new CompoundTag();
            compound.put("Item", inventory.getItem(0).save(new CompoundTag()));
            compound.putBoolean("IgnoreDamage", ignoreDamage);
            compound.putBoolean("IgnoreNBT", ignoreNBT);
            return compound;
        }

    }

}
