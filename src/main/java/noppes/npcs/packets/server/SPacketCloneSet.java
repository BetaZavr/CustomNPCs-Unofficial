package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;

public class SPacketCloneSet extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag data;

    public SPacketCloneSet(CompoundTag dataIn) { data = dataIn; }

    @Override
    public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.cloner; }

    @Override
    public boolean requiresNpc() { return true; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.NPC_CLONE); }

    public static void encode(SPacketCloneSet msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static SPacketCloneSet decode(FriendlyByteBuf buf) { return new SPacketCloneSet(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        PlayerData.get(player).cloned = data;
        CustomNpcs.debugData.end("Packets");
    }

}