package moe.plushie.armourers_workshop.api.core.math;

import java.nio.FloatBuffer;

@SuppressWarnings("unused")
public interface IMatrix4f {

    void set(IMatrix4f var1);

    void load(FloatBuffer var1);

    void store(FloatBuffer var1);

    void scale(float var1, float var2, float var3);

    void translate(float var1, float var2, float var3);

    void rotate(IQuaternionf var1);

    void multiply(IMatrix4f var1);

    void multiply(float[] var1);

    void invert();

    void transpose();

    default void translate(double x, double y, double z) { translate((float)x, (float)y, (float)z); }

    default void scale(double x, double y, double z) { scale((float)x, (float)y, (float)z); }

    default void translate(IVector3f translation) { translate(translation.x(), translation.y(), translation.z()); }

    default void scale(IVector3f vector) { scale(vector.x(), vector.y(), vector.z()); }

    default void translate(IVector3d translation) { translate(translation.x(), translation.y(), translation.z()); }

    default void scale(IVector3d vector) { scale(vector.x(), vector.y(), vector.z()); }

}

