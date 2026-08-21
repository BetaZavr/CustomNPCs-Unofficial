package noppes.npcs.api.event;

import noppes.npcs.api.interfaces.EventName;
import noppes.npcs.api.entity.IProjectile;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.entity.EntityProjectile;

public class ProjectileEvent extends CustomNPCsEvent {

   public IProjectile<?> projectile;

   public ProjectileEvent(EntityProjectile projectileIn) {
      super();
      projectile = (IProjectile<?>) API.getIEntity(projectileIn);
   }

   @EventName(EnumScriptType.PROJECTILE_IMPACT)
   public static class ImpactEvent extends ProjectileEvent {
      public final int type;
      public final Object target;

      public ImpactEvent(EntityProjectile projectile, int typeIn, Object targetIn) {
         super(projectile);
         type = typeIn;
         target = targetIn;
      }
   }

   @EventName(EnumScriptType.TICK)
   public static class UpdateEvent extends ProjectileEvent {
      public UpdateEvent(EntityProjectile projectile) { super(projectile); }
   }

}
