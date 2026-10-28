package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.BorderController;
import noppes.npcs.controllers.data.Zone3D;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;

public class SPacketRegionSave extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag data;

    public SPacketRegionSave(CompoundTag dataIn) { data = dataIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.TOOL_BUILDERS); }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketRegionSave msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static SPacketRegionSave decode(FriendlyByteBuf buf) { return new SPacketRegionSave(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Zone3D reg = BorderController.getInstance().loadRegion(data);
        if (reg != null) {
            BorderController.getInstance().save();
            BorderController.getInstance().update(reg.getId());
        }
        CustomNpcs.debugData.end("Packets");
    }

}