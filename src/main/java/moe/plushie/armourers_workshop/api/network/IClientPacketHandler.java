package moe.plushie.armourers_workshop.api.network;

import net.minecraft.world.entity.player.Player;

@SuppressWarnings("unused")
public interface IClientPacketHandler {

    Player player();

    void enqueueWork(Runnable var1);

}

