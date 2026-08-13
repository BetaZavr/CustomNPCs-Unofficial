package noppes.npcs.packets.server;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.DimensionController;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketDimensionRestore extends PacketServerBasic {

    protected static int channelId;
    private final ResourceKey<Level> dimension;

    public SPacketDimensionRestore(ResourceKey<Level> dimensionIn) { dimension = dimensionIn; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    public static void encode(SPacketDimensionRestore msg, FriendlyByteBuf buf) { buf.writeResourceKey(msg.dimension); }

    public static SPacketDimensionRestore decode(FriendlyByteBuf buf) { return new SPacketDimensionRestore(buf.readResourceKey(Registries.DIMENSION)); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        DimensionController.restoreDimension(player, dimension);
        SPacketDimensionsGet.sendDimensionIDs(player);
        CustomNpcs.debugData.end("Packets");
    }

}