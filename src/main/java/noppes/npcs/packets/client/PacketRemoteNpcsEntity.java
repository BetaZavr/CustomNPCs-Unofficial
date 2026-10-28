package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.GuiNpcRemoteEditor;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.PacketBasic;

import java.util.Optional;

public class PacketRemoteNpcsEntity extends PacketBasic {

    protected static int channelId;
    private final CompoundTag data;

    public PacketRemoteNpcsEntity(CompoundTag compound) {
        data = compound;
    }

    public static void encode(PacketRemoteNpcsEntity msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static PacketRemoteNpcsEntity decode(FriendlyByteBuf buf) { return new PacketRemoteNpcsEntity(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Optional<Entity> type = EntityType.create(data, player.level());
        if (type.isPresent()) {
            Entity entity = type.get();
            if (entity instanceof EntityNPCInterface npc) { npc.ais.setStartPos(npc.blockPosition()); }
            entity.load(data);
            entity.setPos(0, 0, 0);
            if (Minecraft.getInstance().screen instanceof GuiNpcRemoteEditor gui) {
                gui.selectEntity = entity;
                gui.init();
            }
        }
        CustomNpcs.debugData.end("Packets");
    }

}