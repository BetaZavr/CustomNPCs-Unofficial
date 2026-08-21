package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketDimensionRecreate extends PacketServerBasic {

    protected static int channelId;
    private final int dimensionId;

    public SPacketDimensionRecreate(int dimensionIdIn) { dimensionId = dimensionIdIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketDimensionRecreate msg, FriendlyByteBuf buf) { buf.writeInt(msg.dimensionId); }

    public static SPacketDimensionRecreate decode(FriendlyByteBuf buf) { return new SPacketDimensionRecreate(buf.readInt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        //DimensionHandler.getInstance().recreateDimension(player, dimensionId);
        SPacketDimensionsGet.sendDimensionIDs(player);
        CustomNpcs.debugData.end("Packets");
    }

}