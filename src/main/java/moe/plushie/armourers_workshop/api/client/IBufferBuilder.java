package moe.plushie.armourers_workshop.api.client;

@SuppressWarnings("unused")
public interface IBufferBuilder extends IVertexConsumer {

    void begin(IRenderType var1);

    IMeshData end();

}

