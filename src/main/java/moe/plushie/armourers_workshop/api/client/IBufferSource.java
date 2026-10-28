package moe.plushie.armourers_workshop.api.client;

@SuppressWarnings("unused")
public interface IBufferSource {

    IVertexConsumer getBuffer(IRenderType var1);

    default void endBatch() { }

}

