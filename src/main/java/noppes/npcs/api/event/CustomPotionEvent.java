package noppes.npcs.api.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.interfaces.EventName;
import noppes.npcs.constants.EnumScriptType;

public class CustomPotionEvent extends CustomNPCsEvent {

    public ICustomElement potion;

    public CustomPotionEvent(ICustomElement potionIn) {
        super();
        potion = potionIn;
    }

    @EventName(EnumScriptType.POTION_AFFECT)
    public static class AffectEntity extends CustomPotionEvent {

        public IEntity<?> source, indirectSource, entity;
        public int amplifier;
        public double health;

        public AffectEntity(ICustomElement potion, Entity sourceIn, Entity indirectSourceIn,
                            LivingEntity entityIn, int amplifierIn, double healthIn) {
            super(potion);
            source = sourceIn != null ? API.getIEntity(sourceIn) : null;
            indirectSource = indirectSource != null ? API.getIEntity(indirectSourceIn) : null;
            entity = entityIn != null ? API.getIEntity(entityIn) : null;
            amplifier = amplifierIn;
            health = healthIn;
        }

    }

    @EventName(EnumScriptType.POTION_END)
    public static class EndEffect extends CustomPotionEvent {

        public IEntity<?> entity;
        public int amplifier;

        public EndEffect(ICustomElement potion, LivingEntity entityIn, int amplifierIn) {
            super(potion);
            entity = entityIn != null ? API.getIEntity(entityIn) : null;
            amplifier = amplifierIn;
        }

    }

    @EventName(EnumScriptType.POTION_IS_READY)
    public static class IsReadyEvent extends CustomPotionEvent {

        public boolean isReady;
        public int duration;
        public int amplifier;

        public IsReadyEvent(ICustomElement potion, boolean isReadyIn, int durationIn, int amplifierIn) {
            super(potion);
            isReady = isReadyIn;
            duration = durationIn;
            amplifier = amplifierIn;
        }

    }

    @EventName(EnumScriptType.POTION_PERFORM)
    public static class PerformEffect extends CustomPotionEvent {

        public IEntity<?> entity;
        public int amplifier;

        public PerformEffect(ICustomElement potion, LivingEntity entityIn, int amplifierIn) {
            super(potion);
            entity = entityIn != null ? API.getIEntity(entityIn) : null;
            amplifier = amplifierIn;
        }

    }

}