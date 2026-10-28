package noppes.npcs.api.handler.data;

import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.item.IItemStack;

@SuppressWarnings("unused")
public interface IAuction {

    int getId();

    IAuctionLot addLot();

    String getName();

    void setName(@ParamName("name") String name);

    double[] getCommission();

    void setCommission(@ParamName("percentMin") double percentMin, @ParamName("percentMax") double percentMax);

    long[] getDelays();

    void setDelays(@ParamName("delayMin") long delayMin, @ParamName("delayMax") long delayMax);

    double getBidRaise();

    void setBidRaise(@ParamName("percent") double percent);

    double getInitialBid();

    void setInitialBid(@ParamName("percent") double percent);

    int getMaxLotsInPlayer();

    void setMaxLotsInPlayer(@ParamName("max") int max);

    IForbiddenItem[] getForbiddenItems();

    void addForbiddenItem(@ParamName("item") IItemStack item,
                          @ParamName("ignoreDamage") boolean ignoreDamage, @ParamName("ignoreNBT") boolean ignoreNBT);

    void setForbiddenItems(@ParamName("items") IItemStack[] items);

    void setForbiddenItems(IForbiddenItem[] items);

    IAvailability getLotAvailability();

    IAvailability getBidBuyoutAvailability();

    int getCurrencyType();

    void setCurrencyType(@ParamName("currencyType") int currencyType);

    void removeLot(@ParamName("lotId") long lotId);

    IAuctionLot getLot(@ParamName("lotId") long lotId);

    IAuctionLot[] getLots();

    int getCountPlayerLots(String name);

    int getTextureType();

    void setTextureType(int type);

}
