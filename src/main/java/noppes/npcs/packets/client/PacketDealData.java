package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.common.PacketBasic;

public class PacketDealData extends PacketBasic {

    protected static int channelId;
    private final CompoundTag data;

    public PacketDealData(CompoundTag dataIn) { data = dataIn; }

    public static void encode(PacketDealData msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static PacketDealData decode(FriendlyByteBuf buf) { return new PacketDealData(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        MarcetController.getInstance().loadDeal(data);
        if (Minecraft.getInstance().screen instanceof IGuiData gui) { gui.setGuiData(new CompoundTag()); }
        CustomNpcs.debugData.end("Packets");
    }

}
