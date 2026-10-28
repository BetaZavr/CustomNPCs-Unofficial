package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.DropController;
import noppes.npcs.controllers.data.DropsTemplate;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.shared.common.util.LogWriter;

import java.util.Collections;
import java.util.List;

public class SPacketTemplateDropSetSave extends PacketServerBasic {

    protected static int channelId;
    private final String templateId;
    private final String groupId;
    private final CompoundTag compound;

    public SPacketTemplateDropSetSave(String templateIdIn, String groupIdIn, CompoundTag compoundIn) {
        templateId = templateIdIn;
        groupId = groupIdIn;
        compound = compoundIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_DROP_TEMPLATE); }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketTemplateDropSetSave msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.templateId);
        buf.writeUtf(msg.groupId);
        buf.writeNbt(msg.compound);
    }

    public static SPacketTemplateDropSetSave decode(FriendlyByteBuf buf) {
        return new SPacketTemplateDropSetSave(buf.readUtf(), buf.readUtf(), buf.readNbt());
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (templateId == null || templateId.isEmpty()) {
            LogWriter.warn("SaveDropsName is empty for template drop type, ignoring");
        } // Validate saveDropsName for template drops
        else {
            DropController dData = DropController.getInstance();
            DropsTemplate template = dData.templates.get(templateId);
            if (template != null) {
                int slot = compound.getInt("Slot");
                ItemStack stack = ItemStack.of(compound.getCompound("Item"));
                if (stack.isEmpty()) {
                    if (slot < 0) { template.removeGroup(groupId); }
                    else { template.removeDrop(groupId, slot); }
                } // remove
                else if (slot == -1) {
                    DropSet drop = template.addDropItem(groupId, stack, 85.0d);
                    if (drop != null) { drop.load(compound); }
                } // add new
                else if (template.groups.containsKey(groupId) && template.groups.get(groupId).containsKey(slot)) {
                    template.groups.get(groupId).get(slot).load(compound);
                } // change
                dData.save();
                dData.sendTo(player);
            }
        }
        CustomNpcs.debugData.end("Packets");
    }

}
