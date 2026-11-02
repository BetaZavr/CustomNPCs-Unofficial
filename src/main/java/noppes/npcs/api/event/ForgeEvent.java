package noppes.npcs.api.event;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import noppes.npcs.api.interfaces.EventName;
import noppes.npcs.api.IPos;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.data.Zone3D;
import paulscode.sound.Channel;
import paulscode.sound.Source;

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
									@Nonnull Source sourceIn) {
			super(playerIn, nameIn, resourceIn, posIn, volumeIn, pitchIn, milliSecondsIn, totalSecondIn, sourceIn);
		}
	}

	@Cancelable
	@EventName(EnumScriptType.SOUND_TICK_EVENT)
	public static class ClientSoundTickEvent extends ClientSoundEvent {
		public ClientSoundTickEvent(IPlayer<?> playerIn, String nameIn, String resourceIn, IPos posIn,
									float volumeIn, float pitchIn, double milliSecondsIn, double totalSecondIn,
									@Nonnull Source sourceIn) {
			super(playerIn, nameIn, resourceIn, posIn, volumeIn, pitchIn, milliSecondsIn, totalSecondIn, sourceIn);
		}
	}

	@EventName(EnumScriptType.STOP_SOUND)
	public static class ClientSoundStopEvent extends ClientSoundEvent {
		public ClientSoundStopEvent(IPlayer<?> playerIn, String nameIn, String resourceIn, IPos posIn,
									float volumeIn, float pitchIn, double milliSecondsIn, double totalSecondIn,
									@Nonnull Source sourceIn) {
			super(playerIn, nameIn, resourceIn, posIn, volumeIn, pitchIn, milliSecondsIn, totalSecondIn, sourceIn);
		}
	}

	@SuppressWarnings("unused")
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

		public ClientSoundEvent(IPlayer<?> playerIn, String nameIn, String resourceIn, IPos posIn,
								float volumeIn, float pitchIn, double currentTimeIn, double durationIn,
								@Nonnull Source sourceIn) {
			super();
			channel = sourceIn.channel;
			currentTime = currentTimeIn;
			duration = durationIn;
			name = nameIn;
			resource = resourceIn;
			volume = volumeIn;
			pitch = pitchIn;
			pos = posIn;
			player = playerIn;
		}

		public void stop() {
			if (channel != null) { channel.stop(); }
		}

		public boolean isPaused() { return channel != null &&!channel.playing(); }

		public void pause() {
			if (channel != null && !isPaused()) { channel.pause(); }
		}

		public void unpause() {
			if (channel != null && isPaused()) { channel.play(); }
		}

	}

}
