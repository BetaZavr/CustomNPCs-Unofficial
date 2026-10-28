package moe.plushie.armourers_workshop.api.core.math;

@SuppressWarnings("unused")
public interface IVoxelShape {

    IRectangle3f bounds();

    void visit(LineConsumer var1);

    interface LineConsumer {
        void accept(float var1, float var2, float var3, float var4, float var5, float var6);
    }

}

