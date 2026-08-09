package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;

public class SPacketCloneSet extends PacketServerBasic {

    protected static int channelId;
    private NBTTagCompound data;

    public SPacketCloneSet() { }

    public SPacketCloneSet(NBTTagCompound dataIn) { data = dataIn; }

    @Override
    public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand || item.getItem() == CustomItems.cloner || item.getItem() == CustomItems.mount; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<CustomNpcsPermissions.Permission>  getPermission() { return Collections.singletonList(CustomNpcsPermissions.NPC_CLONE); }

    @Override
    public void encode(FriendlyByteBuf buf) { buf.writeNbt(data); }

    @Override
    public void decode(FriendlyByteBuf buf) { data = buf.readAnySizeNbt(); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        PlayerData.get(player).cloned = data;
        CustomNpcs.debugData.end("Packets");
    }

}