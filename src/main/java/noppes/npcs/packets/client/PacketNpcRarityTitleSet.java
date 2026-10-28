package noppes.npcs.packets.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.PacketBasic;

public class PacketNpcRarityTitleSet extends PacketBasic {

    protected static int channelId;
    private final int npcId;
    private final CompoundTag compound;

    public PacketNpcRarityTitleSet(int npcIdIn, CompoundTag compoundIn) {
        npcId = npcIdIn;
        compound = compoundIn;
    }

    public static void encode(PacketNpcRarityTitleSet msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.npcId);
        buf.writeNbt(msg.compound);
    }

    public static PacketNpcRarityTitleSet decode(FriendlyByteBuf buf) { return new PacketNpcRarityTitleSet(buf.readInt(), buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Entity entity = player.level().getEntity(npcId);
        if (entity instanceof EntityNPCInterface cnpc) {
            cnpc.stats.setLevel(compound.getInt("NPCLevel"));
            cnpc.stats.setRarity(compound.getInt("NPCRarity"));
            cnpc.stats.setRarityTitle(compound.getString("RarityTitle"));
        }
        CustomNpcs.debugData.end("Packets");
    }

}