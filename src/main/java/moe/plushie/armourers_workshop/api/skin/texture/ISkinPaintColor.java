package moe.plushie.armourers_workshop.api.skin.texture;

@SuppressWarnings("unused")
public interface ISkinPaintColor {

    int argb();

    int rawValue();

    ISkinPaintType paintType();

    default int red() {
        return this.argb() >> 16 & 0xFF;
    }

    default int green() {
        return this.argb() >> 8 & 0xFF;
    }

    default int blue() {
        return this.argb() & 0xFF;
    }

    ISkinPaintColor withPaintType(ISkinPaintType var1);

    ISkinPaintColor withColor(int var1);

    default ISkinPaintColor withColor(int red, int green, int blue) {
        return this.withColor(red << 16 | green << 8 | blue);
    }

}

