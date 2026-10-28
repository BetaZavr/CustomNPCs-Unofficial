package noppes.npcs.api.handler;

import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.data.IAuction;
import noppes.npcs.api.handler.data.IAuctionLot;
import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.item.IItemStack;

import java.util.List;

@SuppressWarnings("unused")
public interface IAuctionHandler {

    IAuction addAuction();

    void removeAuction(@ParamName("auctionId") int auctionId);

    IAuction getAuction(@ParamName("auctionId") int auctionId);

    List<IAuction> getAuctions();

    int getAveragePrice(@ParamName("item") IItemStack lotItem);

    void addAveragePrice(@ParamName("item") IItemStack lotItem, @ParamName("lotBuyout") double lotBuyout);

    void createLots(IPlayer<?> player, IAuctionLot lotIn, int auctionId, int count);

}
