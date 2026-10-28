package moe.plushie.armourers_workshop.api.common;

import com.google.gson.JsonObject;
import moe.plushie.armourers_workshop.api.network.IFriendlyByteBuf;

@SuppressWarnings("unused")
public interface IArgumentSerializer<T extends IArgumentType<?>> {

    T deserializeFromNetwork(IFriendlyByteBuf var1);

    void serializeToNetwork(T var1, IFriendlyByteBuf var2);

    void serializeToJson(T var1, JsonObject var2);

    Class<T> type();

}

