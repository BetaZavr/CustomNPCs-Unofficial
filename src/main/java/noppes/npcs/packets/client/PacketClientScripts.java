package noppes.npcs.packets.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.shared.common.PacketBasic;

public class PacketClientScripts extends PacketBasic {

    protected static int channelId;
    private final CompoundTag compound;

    public PacketClientScripts(CompoundTag compoundIn) { compound = compoundIn; }

    public static void encode(PacketClientScripts msg, FriendlyByteBuf buf) { buf.writeNbt(msg.compound); }

    public static PacketClientScripts decode(FriendlyByteBuf buf) { return new PacketClientScripts(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        ScriptController.HasStart = true;
        ScriptController.Instance.setClientScripts(compound);
        CustomNpcs.debugData.end("Packets");
    }

}
