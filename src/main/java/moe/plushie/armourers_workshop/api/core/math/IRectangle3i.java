package moe.plushie.armourers_workshop.api.core.math;

@SuppressWarnings("unused")
public interface IRectangle3i {

    int x();

    int y();

    int z();

    int width();

    int height();

    int depth();

    default int minX() { return this.x(); }

    default int minY() { return this.y(); }

    default int minZ() { return this.z(); }

    default int midX() { return this.x() + this.width() / 2; }

    default int midY() { return this.y() + this.height() / 2; }

    default int midZ() { return this.z() + this.depth() / 2; }

    default int maxX() { return this.x() + this.width(); }

    default int maxY() { return this.y() + this.height(); }

    default int maxZ() { return this.z() + this.depth(); }

}

