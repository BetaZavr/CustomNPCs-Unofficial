package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.QuestEvent;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiUpdate;

import java.util.List;

public class SPacketQuestRemoveActive extends PacketServerBasic {

    protected static int channelId;
    private int questId;

    public SPacketQuestRemoveActive() { }

    public SPacketQuestRemoveActive(int questIdIn) { questId = questIdIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<CustomNpcsPermissions.Permission> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item){ return true; }

    @Override
    public void encode(FriendlyByteBuf buf) { buf.writeInt(questId); }

    @Override
    public void decode(FriendlyByteBuf buf) { questId = buf.readInt(); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Quest quest = QuestController.instance.get(questId);
        if (quest != null) {
            PlayerData data = PlayerData.get(player);
            if (!EventHooks.onQuestCanceled(new QuestEvent.QuestCanceledEvent(data.getIPlayer(), quest)) &&
                    PlayerQuestController.getRemoveActiveQuest(player, questId)) {
                player.sendMessage(Component.translatable("quest.removequest", quest.getTitle()).getParent());
                Packets.send(player, new PacketGuiUpdate());
            }
            else { player.sendMessage(Component.translatable("quest.removequest.not", quest.getTitle()).getParent()); }
        }
        CustomNpcs.debugData.end("Packets");
    }

}