package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.constants.EnumPlayerData;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;

public class SPacketPlayerDataSet extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag data;
    private final EnumPlayerData type;
    private final String name;
    private final int id;

    public SPacketPlayerDataSet(EnumPlayerData typeIn, String nameIn, int idIn, CompoundTag dataIn) {
        type = typeIn;
        name = nameIn;
        id = idIn;
        data = dataIn == null ? new CompoundTag() : dataIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand; }

    @Override
    public List<PermissionNode<Boolean>>  getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_PLAYERDATA); }

    public static void encode(SPacketPlayerDataSet msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeUtf(msg.name);
        buf.writeInt(msg.id);
        buf.writeNbt(msg.data);
    }

    public static SPacketPlayerDataSet decode(FriendlyByteBuf buf) {
        return new SPacketPlayerDataSet(buf.readEnum(EnumPlayerData.class), buf.readUtf(), buf.readInt(), buf.readNbt());
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (name != null && !name.isEmpty() && player.getServer() != null) {
            ServerPlayer pl = player.getServer().getPlayerList().getPlayerByName(name);
            PlayerData playerdata = PlayerDataController.instance.getDataFromUsername(player.getServer(), name);
            if (playerdata != null) {
                switch (type) {
                    case Quest: {
                        if (id < 0) {
                            int newId = -1 * id;
                            Quest q = QuestController.instance.quests.get(newId);
                            if (q != null) {
                                if (playerdata.questData.hasFinishedQuest(newId)) {
                                    playerdata.questData.removeFinishedQuest(newId);
                                    playerdata.questData.activeQuests.put(newId, new QuestData(q));
                                }
                                else {
                                    playerdata.questData.finish(q, player);
                                }
                            }
                        } else {
                            Quest q = QuestController.instance.quests.get(id);
                            if (q != null && !playerdata.questData.hasFinishedQuest(id)) { playerdata.questData.finish(q, player); }
                        }
                        break;
                    }
                    case Dialog: {
                        Dialog d = DialogController.instance.dialogs.get(id);
                        if (d != null && !playerdata.dialogData.has(id)) { playerdata.dialogData.read(id); }
                        break;
                    }
                    case Transport: {
                        TransportLocation l = TransportController.getInstance().getTransport(id);
                        if (l != null) { playerdata.transportData.transports.add(id); }
                        break;
                    }
                    case Bank: {
                        if (playerdata.bankData.lastBank != null && playerdata.bankData.lastBank.bank.id != id) {
                            playerdata.bankData.lastBank.save();
                        }
                        break;
                    }
                    case Factions: {
                        Faction f = FactionController.instance.factions.get(id);
                        if (f != null && !playerdata.factionData.factionData.containsKey(id)) {
                            playerdata.factionData.factionData.put(id, f.defaultPoints);
                        } // add
                        if (data.contains("value", Tag.TAG_INT)) {
                            playerdata.factionData.factionData.put(id, data.getInt("value"));
                        } // set value
                        break;
                    }
                    case Game: {
                        playerdata.game.load(data);
                        break;
                    }
                    default: { return; }
                }
                playerdata.save(true);
                if (pl != null) { pl.sendSystemMessage(Component.translatable("message.change.mod.data")); }
            }
            SPacketPlayerDataGet.sendPlayerData(type, player, name);
        }
        CustomNpcs.debugData.end("Packets");
    }
}
