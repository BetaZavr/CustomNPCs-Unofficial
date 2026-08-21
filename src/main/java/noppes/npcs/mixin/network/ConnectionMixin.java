package noppes.npcs.mixin.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.PackageReceived;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

@Mixin(value = Connection.class, priority = 498)
public class ConnectionMixin {

    // Packets not allowed to be processed:
    @Unique private static final List<String> npcs$notAllowed = Arrays.asList("SPacketTabComplete", "CPacketSpectate", "CPacketKeepAlive");

    @Final @Shadow private PacketFlow receiving;

    @Shadow private Channel channel;

    @Shadow private PacketListener packetListener;

    /**
     * @author BetaZavr
     * @reason Processing packets with scripts
     */
    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void npcs$channelRead0(ChannelHandlerContext context, Packet<PacketListener> packet, CallbackInfo ci) {
        if (channel.isOpen() && !npcs$notAllowed.contains(packet.getClass().getSimpleName())) {
            PackageReceived event = new PackageReceived(packet);
            EventHooks.onPackageReceived(event, receiving == PacketFlow.SERVERBOUND);
            if (event.isCanceled()) { ci.cancel(); }
            else if (event.message != null && event.message.getClass() == packet.getClass()) {
                ci.cancel();
                try { event.message.handle(packetListener); } catch (Exception ignored) { }
            }
        }
    }

}
