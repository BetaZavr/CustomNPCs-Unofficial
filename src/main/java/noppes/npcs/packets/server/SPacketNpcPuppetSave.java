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
import java.util.Objects;
import java.util.Set;

public class SPacketNpcPuppetSave extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag data;

    public SPacketNpcPuppetSave(CompoundTag dataIn) { data = dataIn; }

    @Override
    public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand; }

    @Override
    public boolean requiresNpc() { return true; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.NPC_ADVANCED); }

    public static void encode(SPacketNpcPuppetSave msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static SPacketNpcPuppetSave decode(FriendlyByteBuf buf) { return new SPacketNpcPuppetSave(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        CompoundTag original = npc.puppet.save(new CompoundTag());
        Set<String> names = data.getAllKeys();
        for (String name : names) { original.put(name, Objects.requireNonNull(data.get(name))); }
        npc.puppet.load(original);
        npc.updateClient = true;
        CustomNpcs.debugData.end("Packets");
    }

}