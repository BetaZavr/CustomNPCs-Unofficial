package noppes.npcs.api.event;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Cancelable;
import noppes.npcs.api.IPos;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.interfaces.EventName;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.data.Zone3D;

import javax.annotation.Nonnull;

@Cancelable
public class ForgeEvent extends CustomNPCsEvent {

   @EventName(EnumScriptType.INIT)
   public static class InitEvent extends ForgeEvent {
      public InitEvent() { super(); }
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
      public ClientSoundPlayEvent(IPlayer<?> playerIn, String nameIn, String resourceIn, IPos posIn,
                                  float volumeIn, float pitchIn, double milliSecondsIn, double totalSecondIn,
                                  @Nonnull Channel channelIn) {
         super(playerIn, nameIn, resourceIn, posIn, volumeIn, pitchIn, milliSecondsIn, totalSecondIn, channelIn);
      }
   }

   @Cancelable
   @EventName(EnumScriptType.SOUND_TICK_EVENT)
   public static class ClientSoundTickEvent extends ClientSoundEvent {
      public ClientSoundTickEvent(IPlayer<?> playerIn, String nameIn, String resourceIn, IPos posIn,
                                  float volumeIn, float pitchIn, double milliSecondsIn, double totalSecondIn,
                                  @Nonnull Channel channelIn) {
         super(playerIn, nameIn, resourceIn, posIn, volumeIn, pitchIn, milliSecondsIn, totalSecondIn, channelIn);
      }
   }

   @EventName(EnumScriptType.STOP_SOUND)
   public static class ClientSoundStopEvent extends ClientSoundEvent {
      public ClientSoundStopEvent(IPlayer<?> playerIn, String nameIn, String resourceIn, IPos posIn,
                                  float volumeIn, float pitchIn, double milliSecondsIn, double totalSecondIn,
                                  @Nonnull Channel channelIn) {
         super(playerIn, nameIn, resourceIn, posIn, volumeIn, pitchIn, milliSecondsIn, totalSecondIn, channelIn);
      }
   }

   private static class ClientSoundEvent extends ForgeEvent {

      protected final Channel channel;
      public double currentTime;
      public double duration;
      public float volume;
      public float pitch;
      public String name;
      public String resource;
      public IPlayer<?> player;
      public IPos pos;

      @SuppressWarnings("unused")
      public ClientSoundEvent(IPlayer<?> playerIn, String nameIn, String resourceIn, IPos posIn,
                              float volumeIn, float pitchIn, double currentTimeIn, double durationIn,
                              @Nonnull Channel channelIn) {
         super();
         channel = channelIn;
         currentTime = currentTimeIn;
         duration = durationIn;
         name = nameIn;
         resource = resourceIn;
         volume = volumeIn;
         pitch = pitchIn;
         pos = posIn;
         player = playerIn;
      }

      public void stop() { channel.stop(); }

      public boolean isPaused() { return !channel.playing(); }

      public void pause() {
         if (!isPaused()) { channel.pause(); }
      }

      public void unpause() {
         if (isPaused()) { channel.unpause(); }
      }

   }

}
