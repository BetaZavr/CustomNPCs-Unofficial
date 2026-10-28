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

public class SPacketDropTemplateRename extends PacketServerBasic {

    protected static int channelId;
    private final String oldName;
    private final String name;

    public SPacketDropTemplateRename(String oldNameIn, String nameIn) {
        oldName = oldNameIn;
        name = nameIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_DROP_TEMPLATE); }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketDropTemplateRename msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.oldName);
        buf.writeUtf(msg.name);
    }

    public static SPacketDropTemplateRename decode(FriendlyByteBuf buf) { return new SPacketDropTemplateRename(buf.readUtf(), buf.readUtf()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (!name.isEmpty() && !oldName.isEmpty() && !name.equals(oldName)) {
            DropController dData = DropController.getInstance();
            String newName = name;
            while (dData.templates.containsKey(newName)) { newName += "_"; }
            dData.templates.put(newName, dData.templates.get(oldName));
            dData.templates.remove(oldName);
            dData.save();
            dData.sendTo(player);
        }
        CustomNpcs.debugData.end("Packets");
    }

}