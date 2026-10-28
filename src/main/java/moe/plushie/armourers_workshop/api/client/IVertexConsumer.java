package moe.plushie.armourers_workshop.api.client;

import moe.plushie.armourers_workshop.api.core.math.IPoseStack;

@SuppressWarnings("unused")
public interface IVertexConsumer {

    IVertexConsumer vertex(float x, float y, float z);

    IVertexConsumer color(int red, int green, int blue, int alpha);

    IVertexConsumer uv(float u, float v);

    IVertexConsumer overlayCoords(int u, int v);

    IVertexConsumer uv2(int u, int v);

    IVertexConsumer normal(float nx, float ny, float nz);

    void endVertex();

    default void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        vertex(x, y, z);
        color(color);
        uv(u, v);
        overlayCoords(overlay);
        uv2(light);
        normal(nx, ny, nz);
        endVertex();
    }

    default IVertexConsumer color(float r, float g, float b, float a) {
        return color((int)(r * 255.0f), (int)(g * 255.0f), (int)(b * 255.0f), (int)(a * 255.0f));
    }

    default IVertexConsumer color(int color) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        int alpha = color >> 24 & 0xFF;
        return color(red, green, blue, alpha);
    }

    default IVertexConsumer uv2(int i) {
        return uv2(i & 0xFFFF, i >> 16 & 0xFFFF);
    }

    default IVertexConsumer overlayCoords(int i) {
        return overlayCoords(i & 0xFFFF, i >> 16 & 0xFFFF);
    }

    default IVertexConsumer vertex(IPoseStack.Pose pose, float x, float y, float z) {
        float[] values = new float[]{x, y, z, 1.0f};
        pose.transformPose(values);
        return vertex(values[0], values[1], values[2]);
    }

    default IVertexConsumer normal(IPoseStack.Pose pose, float x, float y, float z) {
        float[] values = new float[]{x, y, z};
        pose.transformNormal(values);
        return normal(values[0], values[1], values[2]);
    }

}

