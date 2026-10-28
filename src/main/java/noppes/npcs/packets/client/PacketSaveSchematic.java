package noppes.npcs.packets.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.schematics.Schematic;
import noppes.npcs.schematics.SchematicWrapper;
import noppes.npcs.shared.common.PacketBasic;

public class PacketSaveSchematic extends PacketBasic {

    protected static int channelId;
    private final CompoundTag data;

    public PacketSaveSchematic(CompoundTag dataIn) { data = dataIn; }

    public static void encode(PacketSaveSchematic msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static PacketSaveSchematic decode(FriendlyByteBuf buf) { return new PacketSaveSchematic(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Schematic schema = new Schematic("");
        schema.load(data);
        schema.save(player);
        SchematicController.Instance.map.put(schema.getName(), new SchematicWrapper(schema));
        CustomNpcs.debugData.end("Packets");
    }

}