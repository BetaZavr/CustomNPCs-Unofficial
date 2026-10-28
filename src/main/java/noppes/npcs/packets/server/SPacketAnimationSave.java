package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;

public class SPacketAnimationSave extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag data;

    public SPacketAnimationSave(CompoundTag dataIn) { data = dataIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.NPC_ADVANCED); }

    public static void encode(SPacketAnimationSave msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static SPacketAnimationSave decode(FriendlyByteBuf buf) { return new SPacketAnimationSave(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        npc.animation.load(data);
        npc.updateClient = true;
        CustomNpcs.debugData.end("Packets");
    }

}
