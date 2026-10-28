package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;

public class SPacketMarcetSave extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag data;

    public SPacketMarcetSave(CompoundTag dataIn) { data = dataIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_MARKETS); }

    public static void encode(SPacketMarcetSave msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static SPacketMarcetSave decode(FriendlyByteBuf buf) { return new SPacketMarcetSave(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        MarcetController mData = MarcetController.getInstance();
        if (data.contains("MarcetID", 3)) {
            mData.loadMarcet(data);
            mData.save();
        }
        CustomNpcs.debugData.end("Packets");
    }

}