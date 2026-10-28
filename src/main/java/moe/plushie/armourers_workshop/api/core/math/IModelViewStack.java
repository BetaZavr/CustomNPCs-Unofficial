package moe.plushie.armourers_workshop.api.core.math;

@SuppressWarnings("unused")
public interface IModelViewStack {

    void pushMatrix();

    void popMatrix();

    IMatrix4f last();

}

