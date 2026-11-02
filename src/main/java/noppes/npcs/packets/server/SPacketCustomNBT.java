package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketCustomNBT extends PacketServerBasic {

    protected static int channelId;
    private NBTTagCompound data;

    public SPacketCustomNBT() { }

    public SPacketCustomNBT(NBTTagCompound dataIn) { data = dataIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<CustomNpcsPermissions.Permission> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item){ return true; }

    @Override
    public void encode(FriendlyByteBuf buf) {buf.writeNbt(data); }

    @Override
    public void decode(FriendlyByteBuf buf) { data = buf.readNbt(); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        EventHooks.onEvent(EventHooks.getPlayerHandler(), EnumScriptType.PACKAGE_FROM,
                new PlayerEvent.PlayerPackage(PlayerData.get(player).getIPlayer(), data));
        CustomNpcs.debugData.end("Packets");
    }

}