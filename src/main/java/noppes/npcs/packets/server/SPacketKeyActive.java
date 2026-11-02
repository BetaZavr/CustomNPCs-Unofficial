package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.handler.data.IKeySetting;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;
import java.util.Objects;

public class SPacketKeyActive extends PacketServerBasic {

    protected static int channelId;
    private final int id;

    public SPacketKeyActive(int idIn) { id = idIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketKeyActive msg, FriendlyByteBuf buf) { buf.writeInt(msg.id); }

    public static SPacketKeyActive decode(FriendlyByteBuf buf) { return new SPacketKeyActive(buf.readInt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    public void handle() {
        CustomNpcs.debugData.start("Packets");
        IKeySetting kb = Objects.requireNonNull(NpcAPI.Instance()).getIKeyBinding().getKeySetting(id);
        if (kb != null) {
            EventHooks.onPlayerKeyActive(new PlayerEvent.KeyActive(PlayerData.get(player).getIPlayer(), kb));
        }
        CustomNpcs.debugData.end("Packets");
    }

}
