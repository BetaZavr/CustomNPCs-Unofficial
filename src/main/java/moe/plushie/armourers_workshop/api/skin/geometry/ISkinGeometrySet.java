package moe.plushie.armourers_workshop.api.skin.geometry;

import java.util.Collection;
import java.util.Iterator;
import moe.plushie.armourers_workshop.api.core.math.IVoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface ISkinGeometrySet<T extends ISkinGeometry> extends Iterable<T> {

    int size();

    T get(int var1);

    IVoxelShape shape();

    @Nullable
    Collection<? extends ISkinGeometryType> supportedTypes();

    @Override
    @NotNull
    default Iterator<T> iterator() {

        return new Iterator<>(){
            int index = 0;
            final int count = size();

            @Override
            public boolean hasNext() {
                return index < count;
            }

            @Override
            public T next() {
                T value = get(index);
                ++index;
                return value;
            }
        };
    }

    default boolean isEmpty() { return size() == 0; }

}

