package noppes.npcs.api.event;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraftforge.eventbus.api.Cancelable;
import noppes.npcs.api.interfaces.EventName;
import noppes.npcs.constants.EnumScriptType;

@Cancelable
@EventName(EnumScriptType.PACKAGE_RECEIVED)
public class PackageReceived extends CustomNPCsEvent {

    public final Packet<PacketListener> message;

    public PackageReceived(Packet<PacketListener> msg) {
        super();
        message = msg;
    }

}
