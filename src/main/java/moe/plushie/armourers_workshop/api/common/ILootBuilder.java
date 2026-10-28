package moe.plushie.armourers_workshop.api.common;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface ILootBuilder {

    <T> T getParameter(IContextKey<T> var1);

    @Nullable <T> T getOptionalParameter(IContextKey<T> var1);

    <T> ILootBuilder withParameter(IContextKey<T> var1, T var2);

    <T> ILootBuilder withOptionalParameter(IContextKey<T> var1, @Nullable T var2);

    ServerLevel level();

}

