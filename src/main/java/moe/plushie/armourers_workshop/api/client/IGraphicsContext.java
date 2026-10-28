package moe.plushie.armourers_workshop.api.client;

import moe.plushie.armourers_workshop.api.core.math.IPoseStack;
import moe.plushie.armourers_workshop.api.core.math.IQuaternionf;
import moe.plushie.armourers_workshop.api.core.math.IVector3d;
import moe.plushie.armourers_workshop.api.core.math.IVector3f;

@SuppressWarnings("unused")
public interface IGraphicsContext {

    void draw(IGraphicsElement var1);

    IPoseStack ctm();

    default void saveGraphicsState() {
        ctm().pushPose();
    }

    default void restoreGraphicsState() {
        ctm().popPose();
    }

    default void translateCTM(float x, float y, float z) {
        ctm().translate(x, y, z);
    }

    default void translateCTM(double x, double y, double z) {
        ctm().translate(x, y, z);
    }

    default void translateCTM(IVector3f translation) {
        ctm().translate(translation);
    }

    default void translateCTM(IVector3d translation) {
        ctm().translate(translation);
    }

    default void scaleCTM(float x, float y, float z) {
        ctm().scale(x, y, z);
    }

    default void scaleCTM(double x, double y, double z) {
        ctm().scale(x, y, z);
    }

    default void scaleCTM(IVector3f scalar) {
        ctm().scale(scalar);
    }

    default void scaleCTM(IVector3d scalar) {
        ctm().scale(scalar);
    }

    default void rotateCTM(IQuaternionf quaternion) {
        ctm().rotate(quaternion);
    }

    default void concatenateCTM(IPoseStack.Pose pose) {
        ctm().multiply(pose.pose());
        ctm().multiply(pose.normal());
    }

}

