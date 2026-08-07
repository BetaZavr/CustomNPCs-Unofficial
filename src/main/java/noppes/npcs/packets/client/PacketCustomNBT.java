package noppes.npcs.packets.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.shared.common.PacketBasic;

public class PacketCustomNBT extends PacketBasic {

    protected static int channelId;
    private final CompoundTag data;

    public PacketCustomNBT(CompoundTag dataIn) { data = dataIn; }

    public static void encode(PacketCustomNBT msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static PacketCustomNBT decode(FriendlyByteBuf buf) { return new PacketCustomNBT(buf.readAnySizeNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.PACKAGE_FROM,
                new PlayerEvent.PlayerPackage(CustomNpcs.proxy.getPlayerData(player).scriptData.getPlayer(), data));
        CustomNpcs.debugData.end("Packets");
    }
}
