package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.DropController;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;

public class SPacketDropTemplateRemove extends PacketServerBasic {

    protected static int channelId;
    private final String name;

    public SPacketDropTemplateRemove(String nameIn) { name = nameIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.NPC_INVENTORY); }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketDropTemplateRemove msg, FriendlyByteBuf buf) { buf.writeUtf(msg.name); }

    public static SPacketDropTemplateRemove decode(FriendlyByteBuf buf) { return new SPacketDropTemplateRemove(buf.readUtf()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (!name.isEmpty()) {
            DropController dData = DropController.getInstance();
            dData.templates.remove(name);
            dData.save();
            dData.sendTo(player);
        }
        CustomNpcs.debugData.end("Packets");
    }

}
