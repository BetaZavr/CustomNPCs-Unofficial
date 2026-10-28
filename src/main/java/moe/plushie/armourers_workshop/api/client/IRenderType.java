package moe.plushie.armourers_workshop.api.client;

import java.util.Optional;

@SuppressWarnings("unused")
public interface IRenderType {

    String name();

    int ordinal();

    boolean isEmissive();

    boolean isTranslucent();

    boolean isOutline();

    IVertexFormat format();

    Optional<IRenderType> outline();

}

