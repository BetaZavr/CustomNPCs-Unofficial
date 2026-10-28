package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.AnimationController;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;

public class SPacketAnimationChange extends PacketServerBasic {

    protected static int channelId;
    private final int type;
    private final CompoundTag data;

    /**
     * @param typeIn - 0: clear; 1: delete; 2: save all; 3: load
     * @param dataIn - animation nbt tags
     */
    public SPacketAnimationChange(int typeIn, CompoundTag dataIn) {
        type = typeIn;
        data = dataIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.NPC_ADVANCED); }

    public static void encode(SPacketAnimationChange msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.type);
        buf.writeNbt(msg.data);
    }

    public static SPacketAnimationChange decode(FriendlyByteBuf buf) { return new SPacketAnimationChange(buf.readInt(), buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        AnimationController aData = AnimationController.getInstance();
        switch (type) {
            case 0: {
                aData.clearAnimations();
                aData.sendAnimationToAll(-1);
                break;
            } // clear
            case 1: {
                aData.removeAnimation(data.getInt("ID"));
                aData.sendAnimationToAll(data.getInt("ID"));
                break;
            } // delete
            case 2: {
                aData.save();
                break;
            } // save all
            case 3: {
                aData.loadAnimation(data);
                aData.sendAnimationToAll(data.getInt("ID"));
                break;
            } // load
        }
        CustomNpcs.debugData.end("Packets");
    }

}
