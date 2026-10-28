package moe.plushie.armourers_workshop.api.data;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface IAssociatedContainer {

    <T> T getAssociatedObject(Key<T> var1);

    <T> void setAssociatedObject(Key<T> var1, @Nullable T var2);

    interface Key<T> {
        @Nullable
        default T defaultValue() { return null; }
    }

}

