package noppes.npcs.api.handler.data;

import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.item.IItemStack;

@SuppressWarnings("unused")
public interface IAuctionLot {

    long getCreationDate();

    long getDuration();

    void setDuration(@ParamName("delay") long delay);

    String getOwnerName();

    void setOwnerName(@ParamName("name") String name);

    IItemStack getItem();

    void setItem(@ParamName("stack") IItemStack stack);

    int getStartingPrice();

    void setStartingPrice(@ParamName("price") int price);

    int getBuyoutPrice();

    void setBuyoutPrice(@ParamName("price") int price);

    boolean getCurrencyType();

    void setCurrencyType(@ParamName("currencyType") boolean currencyType);

    boolean canBuyout();

    void setCanBuyout(@ParamName("can") boolean can);

    String getDescription();

    void setDescription(@ParamName("description") String description);

    String getCurrentBidderName();

    void setCurrentBidderName(@ParamName("name") String name);

    int getCurrentBidPrice();

    void setCurrentBidPrice(@ParamName("price") int price);

}
