package noppes.npcs.packets.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.PacketBasic;

public class PacketNpcInitData extends PacketBasic {

    protected static int channelId;
    private final int npcId;
    private final CompoundTag compound;

    public PacketNpcInitData(int npcIdIn, CompoundTag compoundIn) {
        npcId = npcIdIn;
        compound = compoundIn;
    }

    public static void encode(PacketNpcInitData msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.npcId);
        buf.writeNbt(msg.compound);
    }

    public static PacketNpcInitData decode(FriendlyByteBuf buf) {
        return new PacketNpcInitData(buf.readInt(), buf.readNbt());
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Entity e = player.level().getEntity(npcId);
        if (e instanceof EntityNPCInterface) { e.load(compound); }
        CustomNpcs.debugData.end("Packets");
    }

}