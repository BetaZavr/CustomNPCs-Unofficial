package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.QuestEvent;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;
import java.util.Objects;

public class SPacketScriptRun extends PacketServerBasic {

    protected static int channelId;
    private final EnumScriptType type;
    private final int data;

    public SPacketScriptRun(EnumScriptType typeIn, int dataIn) {
        type = typeIn;
        data = dataIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item){ return true; }

    public static void encode(SPacketScriptRun msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeInt(msg.data);
    }

    public static SPacketScriptRun decode(FriendlyByteBuf buf) { return new SPacketScriptRun(buf.readEnum(EnumScriptType.class), buf.readInt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (Objects.requireNonNull(type) == EnumScriptType.QUEST_LOG_BUTTON) {
            EventHooks.onEvent(EventHooks.getPlayerHandler(), type,
                    new QuestEvent.QuestExtraButtonEvent(PlayerData.get(player).getIPlayer(), QuestController.instance.get(data)));
        } // player compass data
        CustomNpcs.debugData.end("Packets");
    }

}
