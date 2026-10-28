package moe.plushie.armourers_workshop.api.common;

import moe.plushie.armourers_workshop.api.network.IFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("unused")
public interface IPlayerDataSerializer<T> extends IEntityDataSerializer<T> {

    void write(IFriendlyByteBuf var1, Player var2, T var3);

    T read(IFriendlyByteBuf var1, Player var2);

    @Override
    default void write(IFriendlyByteBuf buffer, T value) { write(buffer, null, value); }

    @Override
    default T read(IFriendlyByteBuf buffer) { return read(buffer, null); }

    default T copy(T value) { return value; }

}

