package noppes.npcs.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.IPos;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.interfaces.EventName;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.wrapper.BlockWrapper;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.data.Zone3D;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Cancelable
public class ForgeEvent extends CustomNPCsEvent {

   public final Event event;

   // New from Unofficial (BetaZavr)
   public final @Nullable IWorld world;
   public final @Nullable IPos pos;
   public final @Nullable IBlock block;
   public final @Nullable IPlayer<?> player;
   public final @Nullable IEntity<?> entity;

   public ForgeEvent(Event eventIn) {
      super();
      event = eventIn;
      IWorld iWorld = null;
      IPos iPos = null;
      IBlock iBlock = null;
      IEntity<?> iEntity = null;
      IPlayer<?> iPlayer = null;
      if (eventIn instanceof net.minecraftforge.event.entity.EntityEvent ev) {
         iEntity = ev.getEntity() != null ? API.getIEntity(ev.getEntity()) : null;
      }
      if (eventIn instanceof net.minecraftforge.event.level.LevelEvent ev && ev.getLevel() instanceof Level level) {
         iWorld = API.getIWorld(level);
      }
      if (!CustomNpcs.SimplifiedForgeEvents && eventIn != null) {
         List<Field> fields = new ArrayList<>(Arrays.asList(eventIn.getClass().getDeclaredFields()));
         for (Field field : eventIn.getClass().getFields()) {
            if (!fields.contains(field)) { fields.add(field); }
         }
         List<Method> methods = new ArrayList<>(Arrays.asList(eventIn.getClass().getDeclaredMethods()));
         for (Method method : eventIn.getClass().getMethods()) {
            if (!methods.contains(method)) { methods.add(method); }
         }
         if (iEntity == null) {
            for (Field field : fields) {
               try {
                  if (Entity.class.isAssignableFrom(field.getType())) {
                     Object obj = field.get(eventIn);
                     if (obj instanceof Entity e) {
                        iEntity = API.getIEntity(e);
                        break;
                     }
                  }
               }
               catch (Exception ignored) { }
            }
         }
         if (iEntity == null) {
            for (Method method : methods) {
               if (method.getParameterCount() == 0 && Entity.class.isAssignableFrom(method.getReturnType())) {
                  try {
                     Object obj = method.invoke(eventIn);
                     if (obj instanceof Entity e) {
                        iEntity = API.getIEntity(e);
                        break;
                     }
                  }
                  catch (Exception ignored) { }
               }
            }
         }
         if (iEntity instanceof IPlayer<?> iPl) { iPlayer = iPl; }
         if (iPlayer == null) {
            for (Field field : fields) {
               try {
                  if (Player.class.isAssignableFrom(field.getType())) {
                     Object obj = field.get(eventIn);
                     if (obj instanceof Player pl) {
                        iPlayer = (IPlayer<?>) API.getIEntity(pl);
                        break;
                     }
                  }
               }
               catch (Exception ignored) { }
            }
         }
         if (iPlayer == null) {
            for (Method method : methods) {
               if (method.getParameterCount() == 0 && Player.class.isAssignableFrom(method.getReturnType())) {
                  try {
                     Object obj = method.invoke(eventIn);
                     if (obj instanceof Player pl) {
                        iPlayer = (IPlayer<?>) API.getIEntity(pl);
                        break;
                     }
                  }
                  catch (Exception ignored) { }
               }
            }
         }
         if (iWorld == null) {
            for (Field field : fields) {
               try {
                  if (Level.class.isAssignableFrom(field.getType())) {
                     Object obj = field.get(eventIn);
                     if (obj instanceof Level level) {
                        iWorld = API.getIWorld(level);
                        break;
                     }
                  }
               }
               catch (Exception ignored) { }
            }
         }
         if (iWorld == null) {
            for (Method method : methods) {
               if (method.getParameterCount() == 0 && Level.class.isAssignableFrom(method.getReturnType())) {
                  try {
                     Object obj = method.invoke(eventIn);
                     if (obj instanceof Level level) {
                        iWorld = API.getIWorld(level);
                        break;
                     }
                  }
                  catch (Exception ignored) { }
               }
            }
         }
         if (iWorld == null) {
            if (iEntity != null) { iWorld = iEntity.getWorld(); }
            else if (iPlayer != null) { iWorld = iPlayer.getWorld(); }
         }
         if (iWorld != null) {
            for (Field field : fields) {
               try {
                  if (BlockPos.class.isAssignableFrom(field.getType())) {
                     Object obj = field.get(eventIn);
                     if (obj instanceof BlockPos p) {
                        iPos = API.getIPos(p);
                        break;
                     }
                  }
               }
               catch (Exception ignored) { }
            }
            if (iPos == null) {
               for (Method method : methods) {
                  if (method.getParameterCount() == 0 && BlockPos.class.isAssignableFrom(method.getReturnType())) {
                     try {
                        Object obj = method.invoke(eventIn);
                        if (obj instanceof BlockPos p) {
                           iPos = API.getIPos(p);
                           break;
                        }
                     }
                     catch (Exception ignored) { }
                  }
               }
            }
            if (iPos == null) {
               if (iEntity != null) { iPos = iEntity.getPos(); }
               else if (iPlayer != null) { iPos = iPlayer.getPos(); }
            }
            Level level = iWorld.getMCLevel();
            for (Field field : fields) {
               try {
                  if (BlockState.class.isAssignableFrom(field.getType())) {
                     Object obj = field.get(eventIn);
                     if (obj instanceof BlockState bs) {
                        iBlock = BlockWrapper.createNew(level, iPos == null ? BlockPos.ZERO : iPos.getMCBlockPos(), bs);
                        break;
                     }
                  }
               }
               catch (Exception ignored) { }
            }
            if (iBlock == null) {
               for (Field field : fields) {
                  try {
                     if (Block.class.isAssignableFrom(field.getType())) {
                        Object obj = field.get(eventIn);
                        if (obj instanceof Block b) {
                           iBlock = BlockWrapper.createNew(level, iPos == null ? BlockPos.ZERO : iPos.getMCBlockPos(), b.defaultBlockState());
                           break;
                        }
                     }
                  }
                  catch (Exception ignored) { }
               }

            }
            if (iBlock == null) {
               for (Method method : methods) {
                  if (method.getParameterCount() == 0 && BlockState.class.isAssignableFrom(method.getReturnType())) {
                     try {
                        Object obj = method.invoke(eventIn);
                        if (obj instanceof BlockState bs) {
                           iBlock = BlockWrapper.createNew(level, iPos == null ? BlockPos.ZERO : iPos.getMCBlockPos(), bs);
                           break;
                        }
                     }
                     catch (Exception ignored) { }
                  }
               }
            }
            if (iBlock == null) {
               for (Method method : methods) {
                  if (method.getParameterCount() == 0 && Block.class.isAssignableFrom(method.getReturnType())) {
                     try {
                        Object obj = method.invoke(eventIn);
                        if (obj instanceof Block b) {
                           iBlock = BlockWrapper.createNew(level, iPos == null ? BlockPos.ZERO : iPos.getMCBlockPos(), b.defaultBlockState());
                           break;
                        }
                     }
                     catch (Exception ignored) { }
                  }
               }
            }
         }
      }
      world = iWorld;
      pos = iPos;
      block = iBlock;
      player = iPlayer;
      entity = iEntity;
   }

   @EventName(EnumScriptType.INIT)
   public static class InitEvent extends ForgeEvent {
      public InitEvent() { super(null); }
   }

   // New from Unofficial (BetaZavr)
   @Cancelable
   @EventName(EnumScriptType.REGION_ENTER)
   public static class EnterToRegion extends CustomNPCsEvent {
      public final Entity entity;
      public final Zone3D region;
      public EnterToRegion(Entity entityIn, Zone3D zone) {
         super();
         entity = entityIn;
         region = zone;
      }
   }

   @Cancelable
   @EventName(EnumScriptType.REGION_LEAVE)
   public static class LeaveRegion extends CustomNPCsEvent {
      public final Entity entity;
      public final Zone3D region;
      public LeaveRegion(Entity entityIn, Zone3D zone) {
         super();
         entity = entityIn;
         region = zone;
      }
   }

   @Cancelable
   @EventName(EnumScriptType.PLAY_SOUND)
   public static class ClientSoundPlayEvent extends ClientSoundEvent {
      public ClientSoundPlayEvent(Event eventIn, IPlayer<?> playerIn, String nameIn, String resourceIn, IPos posIn, float volumeIn, float pitchIn, double milliSecondsIn, double totalSecondIn) {
         super(eventIn, playerIn, nameIn, resourceIn, posIn, volumeIn, pitchIn, milliSecondsIn, totalSecondIn);
      }
   }

   @Cancelable
   @EventName(EnumScriptType.SOUND_TICK_EVENT)
   public static class ClientSoundTickEvent extends ClientSoundEvent {
      public ClientSoundTickEvent(Event eventIn, IPlayer<?> playerIn, String nameIn, String resourceIn, IPos posIn, float volumeIn, float pitchIn, double milliSecondsIn, double totalSecondIn) {
         super(eventIn, playerIn, nameIn, resourceIn, posIn, volumeIn, pitchIn, milliSecondsIn, totalSecondIn);
      }
   }

   @EventName(EnumScriptType.STOP_SOUND)
   public static class ClientSoundStopEvent extends ClientSoundEvent {
      public ClientSoundStopEvent(Event eventIn, IPlayer<?> playerIn, String nameIn, String resourceIn, IPos posIn, float volumeIn, float pitchIn, double milliSecondsIn, double totalSecondIn) {
         super(eventIn, playerIn, nameIn, resourceIn, posIn, volumeIn, pitchIn, milliSecondsIn, totalSecondIn);
      }
   }

   private static class ClientSoundEvent extends ForgeEvent {

      public double currentTime;
      public double duration;
      public float volume;
      public float pitch;
      public String name;
      public String resource;
      public IPlayer<?> player;
      public IPos pos;

      public ClientSoundEvent(Event eventIn, IPlayer<?> playerIn, String nameIn, String resourceIn, IPos posIn,
                              float volumeIn, float pitchIn, double currentTimeIn, double durationIn) {
         super(eventIn);
         currentTime = currentTimeIn;
         duration = durationIn;
         name = nameIn;
         resource = resourceIn;
         volume = volumeIn;
         pitch = pitchIn;
         pos = posIn;
         player = playerIn;
      }

   }

}
