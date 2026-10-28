package moe.plushie.armourers_workshop.api.client;

import moe.plushie.armourers_workshop.api.core.math.IPoseStack;

@SuppressWarnings("unused")
public interface IGraphicsRenderable {

    IRenderType renderType();

    void render(IPoseStack.Pose var1, IVertexConsumer var2);

    default boolean shouldRender(IGraphicsContext context) { return true; }

}

