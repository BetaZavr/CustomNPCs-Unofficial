package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.mainmenu.GuiNpcGlobalMainMenu;
import noppes.npcs.shared.common.PacketBasic;

public class PacketPermissionGlobal extends PacketBasic {

    protected static int channelId;
    private final boolean banks;
    private final boolean factions;
    private final boolean dialogs;
    private final boolean quests;
    private final boolean transports;
    private final boolean players_data;
    private final boolean recipes;
    private final boolean natural_spawns;
    private final boolean linkeds;
    private final boolean markets;
    private final boolean auctions;
    private final boolean mails;
    private final boolean elements;
    private final boolean dungeons;

    public PacketPermissionGlobal(boolean banksIn, boolean factionsIn, boolean dialogsIn, boolean questsIn, boolean transportsIn,
                                  boolean players_dataIn, boolean recipesIn, boolean natural_spawnsIn, boolean linkedsIn, boolean marketsIn,
                                  boolean auctionsIn, boolean mailsIn, boolean elementsIn, boolean dungeonsIn) {
        banks = banksIn;
        factions = factionsIn;
        dialogs = dialogsIn;
        quests = questsIn;
        transports = transportsIn;
        players_data = players_dataIn;
        recipes = recipesIn;
        natural_spawns = natural_spawnsIn;
        linkeds = linkedsIn;
        markets = marketsIn;
        auctions = auctionsIn;
        mails = mailsIn;
        elements = elementsIn;
        dungeons = dungeonsIn;
    }

    public static void encode(PacketPermissionGlobal msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.banks);
        buf.writeBoolean(msg.factions);
        buf.writeBoolean(msg.dialogs);
        buf.writeBoolean(msg.quests);
        buf.writeBoolean(msg.transports);
        buf.writeBoolean(msg.players_data);
        buf.writeBoolean(msg.recipes);
        buf.writeBoolean(msg.natural_spawns);
        buf.writeBoolean(msg.linkeds);
        buf.writeBoolean(msg.markets);
        buf.writeBoolean(msg.auctions);
        buf.writeBoolean(msg.mails);
        buf.writeBoolean(msg.elements);
        buf.writeBoolean(msg.dungeons);
    }

    public static PacketPermissionGlobal decode(FriendlyByteBuf buf) {
        return new PacketPermissionGlobal(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (Minecraft.getInstance().screen instanceof GuiNpcGlobalMainMenu gui) {
            gui.setMenuData(banks, factions, dialogs, quests, transports, players_data, recipes,
                    natural_spawns, linkeds, markets, auctions, mails, elements, dungeons);
        }
        CustomNpcs.debugData.end("Packets");
    }

}