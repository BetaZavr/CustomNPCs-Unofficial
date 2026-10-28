package noppes.npcs.packets.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.DropController;
import noppes.npcs.controllers.data.DropsTemplate;
import noppes.npcs.shared.common.PacketBasic;

public class PacketDropTemplateSave extends PacketBasic {

    protected static int channelId;
    private final CompoundTag data;

    public PacketDropTemplateSave(CompoundTag dataIn) {
        data = dataIn;
    }

    public static void encode(PacketDropTemplateSave msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static PacketDropTemplateSave decode(FriendlyByteBuf buf) { return new PacketDropTemplateSave(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (data.contains("Name", 8)) {
            DropsTemplate template = new DropsTemplate(data.getCompound("Groups"));
            DropController.getInstance().templates.put(data.getString("Name"), template);
        }
        CustomNpcs.debugData.end("Packets");
    }

}