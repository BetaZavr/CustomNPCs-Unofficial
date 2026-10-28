package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiData;

import java.util.Collections;
import java.util.List;

public class SPacketNpcInvDropSetSave extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag compound;

    public SPacketNpcInvDropSetSave(CompoundTag compoundIn) {
        compound = compoundIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.NPC_INVENTORY); }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketNpcInvDropSetSave msg, FriendlyByteBuf buf) { buf.writeNbt(msg.compound); }

    public static SPacketNpcInvDropSetSave decode(FriendlyByteBuf buf) {
        return new SPacketNpcInvDropSetSave(buf.readNbt());
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        int slot = compound.getInt("Slot");
        ItemStack stack = ItemStack.of(compound.getCompound("Item"));
        if (stack.isEmpty()) { npc.inventory.removeDrop(slot); } // remove
        else if (slot == -1) {
            DropSet drop = (DropSet) npc.inventory.addDropItem(stack, 1.0d);
            if (drop != null) { drop.load(compound); }
        } // add new
        else if (npc.inventory.drops.containsKey(slot)) { npc.inventory.drops.get(slot).load(compound); } // change
        npc.updateAI = true;
        npc.updateClient = true;
        Packets.send(player, new PacketGuiData(npc.inventory.save(new CompoundTag())));
        CustomNpcs.debugData.end("Packets");
    }

}
