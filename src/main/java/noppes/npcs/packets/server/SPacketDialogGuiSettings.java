package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSync;

import java.util.Collections;
import java.util.List;

public class SPacketDialogGuiSettings extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag compound;

    public SPacketDialogGuiSettings(CompoundTag compoundIn) { compound = compoundIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_DIALOG); }

    public static void encode(SPacketDialogGuiSettings msg, FriendlyByteBuf buf) {buf.writeNbt(msg.compound); }

    public static SPacketDialogGuiSettings decode(FriendlyByteBuf buf) { return new SPacketDialogGuiSettings(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        DialogController.instance.getGuiSettings().load(compound);
        DialogController.instance.saveSettings();
        Packets.sendAll(new PacketSync(10, compound, true));
        CustomNpcs.debugData.end("Packets");
    }

}
