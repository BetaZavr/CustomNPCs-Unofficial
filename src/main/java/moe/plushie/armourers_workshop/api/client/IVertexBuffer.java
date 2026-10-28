package moe.plushie.armourers_workshop.api.client;

@SuppressWarnings("unused")
public interface IVertexBuffer {

    IMeshData slice(int var1, int var2, IVertexFormat var3);

    void retain();

    void release();

}

