package moe.plushie.armourers_workshop.api.common;

import moe.plushie.armourers_workshop.api.network.IFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("unused")
public interface IMenuSerializer<T> {

    void write(IFriendlyByteBuf var1, Player var2, T var3);

    T read(IFriendlyByteBuf var1, Player var2);

}

