package moe.plushie.armourers_workshop.api.core.math;

import java.nio.FloatBuffer;

@SuppressWarnings("unused")
public interface IMatrix3f {

    void set(IMatrix3f var1);

    void load(FloatBuffer var1);

    void store(FloatBuffer var1);

    void scale(float var1, float var2, float var3);

    void rotate(IQuaternionf var1);

    void multiply(IMatrix3f var1);

    void multiply(float[] var1);

    void invert();

    void transpose();

    default void scale(double x, double y, double z) {
        this.scale((float)x, (float)y, (float)z);
    }

    default void scale(IVector3f vector) {
        this.scale(vector.x(), vector.y(), vector.z());
    }

    default void scale(IVector3d vector) {
        this.scale(vector.x(), vector.y(), vector.z());
    }

}

