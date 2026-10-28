package noppes.npcs.packets.client;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.mixin.entity.player.IPlayerMixin;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.PacketBasic;

import java.util.UUID;

public class PacketCustomAnimationSet extends PacketBasic {

    protected static int channelId;
    private final boolean isPlayer;
    private final ResourceKey<Level> dimension;
    private final int id;
    private final UUID uuid;
    private final CompoundTag data;

    public PacketCustomAnimationSet(boolean isPlayerIn, ResourceKey<Level> dimensionIn, int idIn, UUID uuidIn, CompoundTag dataIn) {
        isPlayer = isPlayerIn;
        dimension = dimensionIn;
        id = idIn;
        uuid = uuidIn;
        data = dataIn;
    }

    public static void encode(PacketCustomAnimationSet msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isPlayer);
        buf.writeResourceKey(msg.dimension);
        buf.writeInt(msg.id);
        buf.writeUUID(msg.uuid);
        buf.writeNbt(msg.data);
    }

    public static PacketCustomAnimationSet decode(FriendlyByteBuf buf) {
        return new PacketCustomAnimationSet(buf.readBoolean(), buf.readResourceKey(Registries.DIMENSION), buf.readInt(), buf.readUUID(), buf.readNbt());
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (player.level().dimension().equals(dimension)) {
            if (isPlayer) { // is Player
                IPlayerMixin pl = (IPlayerMixin) player.level().getPlayerByUUID(uuid);
                if (pl != null) { pl.npcs$getAnimation().load(data); }
            }
            else { // is NPC
                Entity entity = player.level().getEntity(id);
                if (entity instanceof EntityNPCInterface npc) { npc.animation.load(data); }
            }
        }
        CustomNpcs.debugData.end("Packets");
    }

}