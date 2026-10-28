package noppes.npcs.packets.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.common.PacketBasic;

public class PacketDetectHeldItem extends PacketBasic {

    protected static int channelId;
    private final int slotID;
    private final CompoundTag data;

    public PacketDetectHeldItem(int slotIDIn, CompoundTag dataIn) {
        slotID = slotIDIn;
        data = dataIn;
    }

    public static void encode(PacketDetectHeldItem msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slotID);
        buf.writeNbt(msg.data);
    }

    public static PacketDetectHeldItem decode(FriendlyByteBuf buf) { return new PacketDetectHeldItem(buf.readInt(), buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        ItemStack stack = ItemStack.of(data);
        if (slotID >= 0) { player.getInventory().setItem(slotID, stack); }
        else { player.containerMenu.setCarried(stack); }
        CustomNpcs.debugData.end("Packets");
    }

}