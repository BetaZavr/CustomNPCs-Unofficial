package noppes.npcs.api.constants;

import javax.annotation.Nonnull;

public enum AlignmentType {

    NONE(0, 0, 0),
    TOP_LEFT(1, 0, 0),
    TOP_CENTER(2, 1, 0),
    TOP_RIGHT(3 , 2, 0),
    CENTER_LEFT(4, 0, 1),
    CENTER(5, 0, 1),
    CENTER_RIGHT(6, 0, 1),
    BOTTOM_LEFT(7, 0, 2),
    BOTTOM_CENTER(8, 1, 2),
    BOTTOM_RIGHT(9, 2, 2);

    final int alignment;
    final int widthStep;
    final int heightStep;

    AlignmentType(int alignmentIn, int widthStepIn, int heightStepIn) {
        alignment = alignmentIn;
        widthStep = widthStepIn % 3;
        heightStep = heightStepIn % 3;
    }

    public int get() { return alignment; }

    public int getOffsetX(int halfWidth) { return widthStep * halfWidth; }

    public int getOffsetY(int halfHeight) { return heightStep * halfHeight; }

    public static @Nonnull AlignmentType get(int alignment) {
        for (AlignmentType at : values()) {
            if (at.alignment == alignment) { return at; }
        }
        return AlignmentType.NONE;
    }

}
