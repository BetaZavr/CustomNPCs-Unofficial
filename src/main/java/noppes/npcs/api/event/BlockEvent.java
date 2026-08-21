package noppes.npcs.api.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import noppes.npcs.api.interfaces.EventName;
import noppes.npcs.api.IPos;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.constants.EnumScriptType;

public class BlockEvent extends CustomNPCsEvent {
   public IBlock block;

   public BlockEvent(IBlock blockIn) {
      super();
      block = blockIn;
   }

   @EventName(EnumScriptType.TIMER)
   public static class TimerEvent extends BlockEvent {
      public final int id;

      public TimerEvent(IBlock block, int idIn) {
         super(block);
         id = idIn;
      }
   }

   @EventName(EnumScriptType.COLLIDE)
   public static class CollidedEvent extends BlockEvent {
      public final IEntity<?> entity;

      public CollidedEvent(IBlock block, Entity entityIn) {
         super(block);
         entity = API.getIEntity(entityIn);
      }
   }

   @Cancelable
   @EventName(EnumScriptType.HARVESTED)
   public static class HarvestedEvent extends BlockEvent {
      public final IPlayer<?> player;

      public HarvestedEvent(IBlock block, Player playerIn) {
         super(block);
         player = (IPlayer<?>) API.getIEntity(playerIn);
      }
   }

   @EventName(EnumScriptType.CLICKED)
   public static class ClickedEvent extends BlockEvent {
      public final IPlayer<?> player;

      public ClickedEvent(IBlock block, Player playerIn) {
         super(block);
         player = (IPlayer<?>) API.getIEntity(playerIn);
      }
   }

   @EventName(EnumScriptType.TICK)
   public static class UpdateEvent extends BlockEvent {
      public UpdateEvent(IBlock block) { super(block); }
   }

   @EventName(EnumScriptType.INIT)
   public static class InitEvent extends BlockEvent {
      public InitEvent(IBlock block) { super(block); }
   }

   @EventName(EnumScriptType.NEIGHBOR_CHANGED)
   public static class NeighborChangedEvent extends BlockEvent {
      public final IPos changedPos;

      public NeighborChangedEvent(IBlock block, IPos changedPosIn) {
         super(block);
         changedPos = changedPosIn;
      }
   }

   @EventName(EnumScriptType.RAIN_FILLED)
   public static class RainFillEvent extends BlockEvent {
      public RainFillEvent(IBlock block) {
         super(block);
      }
   }

   @Cancelable
   @EventName(EnumScriptType.EXPLODED)
   public static class ExplodedEvent extends BlockEvent {
      public ExplodedEvent(IBlock block) {
         super(block);
      }
   }

   @EventName(EnumScriptType.BROKEN)
   public static class BreakEvent extends BlockEvent {
      public BreakEvent(IBlock block) {
         super(block);
      }
   }

   @Cancelable
   @EventName(EnumScriptType.DOOR_TOGGLE)
   public static class DoorToggleEvent extends BlockEvent {
      public DoorToggleEvent(IBlock block) {
         super(block);
      }
   }

   @EventName(EnumScriptType.REDSTONE)
   public static class RedstoneEvent extends BlockEvent {
      public final int prevPower;
      public final int power;

      public RedstoneEvent(IBlock block, int prevPowerIn, int powerIn) {
         super(block);
         power = powerIn;
         prevPower = prevPowerIn;
      }
   }

   @Cancelable
   @EventName(EnumScriptType.INTERACT)
   public static class InteractEvent extends BlockEvent {
      public final IPlayer<?> player;
      public final float hitX;
      public final float hitY;
      public final float hitZ;
      public final int side;

      public InteractEvent(IBlock block, Player playerIn, int sideIn, float hitXIn, float hitYIn, float hitZIn) {
         super(block);
         player = (IPlayer<?>) API.getIEntity(playerIn);
         hitX = hitXIn;
         hitY = hitYIn;
         hitZ = hitZIn;
         side = sideIn;
      }
   }

   @Cancelable
   @EventName(EnumScriptType.FALLEN_UPON)
   public static class EntityFallenUponEvent extends BlockEvent {
      public final IEntity<?> entity;
      public float distanceFallen;

      public EntityFallenUponEvent(IBlock block, Entity entityIn, float distanceIn) {
         super(block);
         distanceFallen = distanceIn;
         entity = API.getIEntity(entityIn);
      }
   }

}
