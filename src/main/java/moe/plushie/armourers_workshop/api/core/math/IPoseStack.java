package moe.plushie.armourers_workshop.api.core.math;

public interface IPoseStack {

    void pushPose();

    void popPose();

    void translate(float var1, float var2, float var3);

    void scale(float var1, float var2, float var3);

    void rotate(IQuaternionf var1);

    void multiply(IMatrix3f var1);

    void multiply(IMatrix4f var1);

    void setIdentity();

    Pose last();

    default void translate(double x, double y, double z) { translate((float)x, (float)y, (float)z); }

    default void scale(double x, double y, double z) { scale((float)x, (float)y, (float)z); }

    default void translate(IVector3f translation) { translate(translation.x(), translation.y(), translation.z()); }

    default void scale(IVector3f scalar) { scale(scalar.x(), scalar.y(), scalar.z()); }

    default void translate(IVector3d translation) { translate(translation.x(), translation.y(), translation.z()); }

    default void scale(IVector3d scalar) { scale(scalar.x(), scalar.y(), scalar.z()); }

    interface Pose {
        int properties();

        IMatrix4f pose();

        IMatrix3f normal();

        void set(Pose var1);

        void transformPose(float[] var1);

        void transformNormal(float[] var1);

    }

}

