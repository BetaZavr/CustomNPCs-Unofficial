package noppes.npcs.api.event;

import net.minecraftforge.event.TickEvent;
import noppes.npcs.api.interfaces.EventFunction;
import noppes.npcs.api.interfaces.EventName;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.constants.EnumScriptType;

public class WorldEvent extends CustomNPCsEvent {

   public final IWorld world;

   public WorldEvent(IWorld worldIn) {
      super();
      world = worldIn;
   }

   @EventName(EnumScriptType.SCRIPT_TRIGGER)
   public static class ScriptTriggerEvent extends WorldEvent {
      public final Object[] arguments;
      public final IPos pos;
      public final IEntity<?> entity;
      public final int id;

      public ScriptTriggerEvent(int idIn, IWorld level, IPos posIn, IEntity<?> entityIn, Object[] argumentsIn) {
         super(level);
         id = idIn;
         arguments = argumentsIn;
         pos = posIn;
         entity = entityIn;
      }
   }

   // New from Unofficial (BetaZavr)
   @EventName(EnumScriptType.SCRIPT_COMMAND)
   public static class ScriptCommandEvent extends WorldEvent {
      public String[] arguments;
      public IPos pos;

      public ScriptCommandEvent(IWorld world, IPos posIn, String[] argumentsIn) {
         super(world);
         arguments = argumentsIn;
         pos = posIn;
      }
   }

   @EventFunction("worldtick")
   public static class ServerTickEvent extends WorldEvent {

      public TickEvent.ServerTickEvent event;

      public ServerTickEvent(TickEvent.ServerTickEvent eventIn) {
         super(null);
         event = eventIn;
      }

   }

}
