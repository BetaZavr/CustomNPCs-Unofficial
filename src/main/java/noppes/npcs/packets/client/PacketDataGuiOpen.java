package noppes.npcs.packets.client;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.shared.common.PacketBasic;
import noppes.npcs.shared.common.util.LogWriter;

public class PacketDataGuiOpen extends PacketBasic {

    protected static int channelId;
    private final EnumGuiType gui;
    private final CompoundTag data;

    public PacketDataGuiOpen(EnumGuiType guiIn, CompoundTag dataIn) {
        gui = guiIn;
        data = dataIn;
    }

    public static void encode(PacketDataGuiOpen msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.gui);
        buf.writeNbt(msg.data);
    }

    public static PacketDataGuiOpen decode(FriendlyByteBuf buf) {
        return new PacketDataGuiOpen(buf.readEnum(EnumGuiType.class),buf.readAnySizeNbt());
    }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        try {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeNbt(data);
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(ClientProxy.getGui(gui, NoppesUtilServer.getEditingNpc(player), buffer));
        }
        catch (Exception e) { LogWriter.error("Error in gui: " + gui, e); }
        CustomNpcs.debugData.end("Packets");
    }

}
