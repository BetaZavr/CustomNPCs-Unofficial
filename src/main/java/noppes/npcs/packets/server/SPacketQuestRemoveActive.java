package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
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
    private final int questId;

    public SPacketQuestRemoveActive(int questIdIn) { questId = questIdIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item){ return true; }

    public static void encode(SPacketQuestRemoveActive msg, FriendlyByteBuf buf) { buf.writeInt(msg.questId); }

    public static SPacketQuestRemoveActive decode(FriendlyByteBuf buf) { return new SPacketQuestRemoveActive(buf.readInt()); }

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
                player.sendSystemMessage(Component.translatable("quest.removequest", quest.getTitle()));
                Packets.send(player, new PacketGuiUpdate());
            }
            else { player.sendSystemMessage(Component.translatable("quest.removequest.not", quest.getTitle())); }
        }
        CustomNpcs.debugData.end("Packets");
    }

}