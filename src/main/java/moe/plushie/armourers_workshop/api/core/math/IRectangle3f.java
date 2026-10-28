package moe.plushie.armourers_workshop.api.core.math;

@SuppressWarnings("unused")
public interface IRectangle3f {

    float x();

    float y();

    float z();

    float width();

    float height();

    float depth();

    default float minX() { return x(); }

    default float minY() { return y(); }

    default float minZ() { return z(); }

    default float midX() { return x() + width() / 2.0f; }

    default float midY() { return y() + height() / 2.0f; }

    default float midZ() { return z() + depth() / 2.0f; }

    default float maxX() { return x() + width(); }

    default float maxY() { return y() + height(); }

    default float maxZ() { return z() + depth(); }

}

