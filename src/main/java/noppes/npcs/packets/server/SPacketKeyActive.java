package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
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
    private int id;

    public SPacketKeyActive() { }

    public SPacketKeyActive(int idIn) { id = idIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<CustomNpcsPermissions.Permission> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public void encode(FriendlyByteBuf buf) { buf.writeInt(id); }

    @Override
    public void decode(FriendlyByteBuf buf) { id = buf.readInt(); }

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
