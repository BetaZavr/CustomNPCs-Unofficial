package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerScriptData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketCustomNBT extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag data;

    public SPacketCustomNBT(CompoundTag dataIn) { data = dataIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item){ return true; }

    public static void encode(SPacketCustomNBT msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static SPacketCustomNBT decode(FriendlyByteBuf buf) { return new SPacketCustomNBT(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        PlayerScriptData handler = PlayerData.get(player).scriptData;
        EventHooks.onEvent(handler, EnumScriptType.PACKAGE_FROM, new PlayerEvent.PlayerPackage(handler.getPlayer(), data));
        CustomNpcs.debugData.end("Packets");
    }

}