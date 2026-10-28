package moe.plushie.armourers_workshop.api.network;

import net.minecraft.server.level.ServerPlayer;

@SuppressWarnings("unused")
public interface IServerPacketHandler {

    ServerPlayer player();

    void enqueueWork(Runnable var1);

}
