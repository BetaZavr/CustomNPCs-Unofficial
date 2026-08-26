package noppes.npcs.api.constants;

public enum AnimationKind {

    ATTACKING(0, false, true),
    DIES(1, false, true),
    FLY_STAND(2, true, false),
    FLY_WALK(3, true, false),
    INIT(4, false, true),
    JUMP(5, false, true),
    STANDING(6, true, false),
    WALKING(7, true, false),
    WATER_STAND(8, true, false),
    WATER_WALK(9, true, false),
    REVENGE_STAND(10, true, false),
    REVENGE_WALK(11, true, false),
    HIT(12, false, true),
    BASE(13, true, false),
    SHOOT(14, false, true),
    AIM(15, true, false),
    SWING(16, false, true),
    INTERACT(17, false, false),
    BLOCKED(18, false, true),
    EDITING_All(19, false, true),
    EDITING_PART(20, false, true),
    SITTING(21, true, false),
    SLEEPING(22, true, false),
    SNEAK_STAND(24, true, false),
    SNEAK_WALK(25, true, false),
    DIALOG(26, true, false);

    public static AnimationKind get(int type) {
        for (AnimationKind ak : AnimationKind.values()) {
            if (ak.type == type) { return ak; }
        }
        return AnimationKind.STANDING;
    }

    public static String[] getNames() {
        String[] list = new String[AnimationKind.values().length];
        for (AnimationKind enm : AnimationKind.values()) {
            list[enm.type] = "puppet." + enm.name().toLowerCase().replace("_", "");
        }
        return list;
    }

    boolean isMovement;
    boolean isQuick;
    final int type;
    AnimationKind parent = null;

    AnimationKind(int typeIn, boolean movement, boolean quickStart) {
        type = typeIn;
        isMovement = movement;
        isQuick = quickStart;
    }

    public int get() { return type; }

    public boolean isMovement() { return isMovement; }

    public boolean isQuick() { return isQuick; }

    public void setEditingBooleans(AnimationKind parentEnum) {
        if (this != AnimationKind.EDITING_All && this != AnimationKind.EDITING_PART) { return; }
        parent = parentEnum;
        isMovement = parentEnum.isMovement;
        isQuick = parentEnum.isQuick;
    }

    public AnimationKind getParentEnum() { return parent; }

}
