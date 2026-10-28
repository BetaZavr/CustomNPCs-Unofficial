package moe.plushie.armourers_workshop.api.core;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("unused")
public interface IDataCodec<A> {

    IDataCodec<Boolean> BOOL = IDataCodec.wrap(Codec.BOOL);
    IDataCodec<Byte> BYTE = IDataCodec.wrap(Codec.BYTE);
    IDataCodec<Short> SHORT = IDataCodec.wrap(Codec.SHORT);
    IDataCodec<Integer> INT = IDataCodec.wrap(Codec.INT);
    IDataCodec<Long> LONG = IDataCodec.wrap(Codec.LONG);
    IDataCodec<Float> FLOAT = IDataCodec.wrap(Codec.FLOAT);
    IDataCodec<Double> DOUBLE = IDataCodec.wrap(Codec.DOUBLE);
    IDataCodec<String> STRING = IDataCodec.wrap(Codec.STRING);
    IDataCodec<ByteBuffer> BYTE_BUFFER = IDataCodec.wrap(Codec.BYTE_BUFFER);

    Codec<A> codec();

    static <T> IDataCodec<T> wrap(Codec<T> codec) {
        return () -> codec; }

    static <F, S> IDataCodec<Pair<F, S>> pair(IDataCodec<F> first, IDataCodec<S> second) {
        return IDataCodec.wrap(Codec.pair(first.codec(), second.codec()));
    }

    static <F, S> IDataCodec<Either<F, S>> either(IDataCodec<F> first, IDataCodec<S> second) {
        return IDataCodec.wrap(Codec.either(first.codec(), second.codec()));
    }

    static <O> IDataCodec<O> create(Function<RecordCodecBuilder.Instance<O>, ? extends App<RecordCodecBuilder.Mu<O>, O>> builder) {
        return IDataCodec.wrap(RecordCodecBuilder.create(builder));
    }

    default <S> IDataCodec<S> xmap(Function<? super A, ? extends S> to, Function<? super S, ? extends A> from) {
        return IDataCodec.wrap(codec().xmap(to, from));
    }

    default <S> IDataCodec<S> flatXmap(Function<? super A, ? extends DataResult<? extends S>> to, Function<? super S, ? extends DataResult<? extends A>> from) {
        return IDataCodec.wrap(codec().flatXmap(to, from));
    }

    @SuppressWarnings("unchecked")
    default <E> IDataCodec<E> dispatch(Function<? super E, ? extends A> type, Function<? super A, ? extends IDataMapCodec<? extends E>> targetCodec) {
        try {
            Class<?> clazz = Class.forName("moe.plushie.armourers_workshop.compat.core.data.AbstractDataCodec");
            return IDataCodec.wrap((Codec<E>) clazz.getDeclaredMethod("dispatch", Codec.class, String.class, Function.class, Function.class)
                    .invoke(null, codec(), "type", type, targetCodec));
        }
        catch (Exception ignored) { }
        return null;
    }

    @SuppressWarnings("unchecked")
    default <E> IDataCodec<E> dispatch(String key, Function<? super E, ? extends A> type, Function<? super A, ? extends IDataMapCodec<? extends E>> targetCodec) {
        try {
            Class<?> clazz = Class.forName("moe.plushie.armourers_workshop.compat.core.data.AbstractDataCodec");
            return IDataCodec.wrap((Codec<E>) clazz.getDeclaredMethod("dispatch", Codec.class, String.class, Function.class, Function.class)
                    .invoke(null, codec(), key, type, targetCodec));
        }
        catch (Exception ignored) { }
        return null;
    }

    default <T> IDataCodec<A> alternative(IDataCodec<T> alternative, Function<T, A> converter) {
        return IDataCodec.either(this, alternative).xmap(either -> either.map(it -> it, converter), Either::left);
    }

    default IDataCodec<A> validate(Function<A, DataResult<A>> checker) {
        return flatXmap(checker, checker);
    }

    default IDataCodec<List<A>> listOf() {
        return IDataCodec.wrap(codec().listOf());
    }

    default IDataMapCodec<A> fieldOf(String name) {
        return IDataMapCodec.wrap(codec().fieldOf(name));
    }

    default IDataMapCodec<Optional<A>> optionalFieldOf(String name) {
        return IDataMapCodec.wrap(codec().optionalFieldOf(name));
    }

    default IDataMapCodec<A> optionalFieldOf(String name, A defaultValue) {
        return IDataMapCodec.wrap(codec().optionalFieldOf(name, defaultValue));
    }

}

