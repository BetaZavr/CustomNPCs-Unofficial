package noppes.npcs.client.util;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.EventHooks;
import noppes.npcs.api.IPos;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.event.ForgeEvent;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.mixin.client.audio.ILibraryMixin;
import noppes.npcs.mixin.client.audio.IPositionedSoundMixin;
import noppes.npcs.mixin.client.audio.ISoundSystemMixin;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import paulscode.sound.Library;
import paulscode.sound.SoundSystem;
import paulscode.sound.Source;

public class MusicData {

	protected static final Map<ResourceLocation, Long> durations = new HashMap<>();
	protected static Field soundSystem;

	public @Nonnull ISound sound;
	public String uuid;
	public @Nullable Source source;
	public String name;
	public final ResourceLocation resource;
	public final SoundCategory category;
	public long duration;

	protected SoundSystem sndSystem;

	public MusicData(@Nonnull ISound soundIn, String uuidIn, SoundManager manager) {
		sound = soundIn;
		uuid = uuidIn;
		name = sound.getSound().getSoundLocation().toString();
		resource = sound.getSoundLocation();
		category = soundIn.getCategory();

		if (soundSystem == null) {
			for (Field f : manager.getClass().getDeclaredFields()) {
				if (f.getType().getName().contains("SoundSystem")) {
					soundSystem = f;
					break;
				}
			}
		}
		if (soundSystem != null) {
			try {
				soundSystem.setAccessible(true);
				sndSystem = (SoundSystem) soundSystem.get(manager);
				if (sndSystem != null) {
					Library soundLibrary = ((ISoundSystemMixin) sndSystem).getSoundLibrary();
					HashMap<String, Source> sourceMap = ((ILibraryMixin) soundLibrary).getSourceMap() ;
					if (sourceMap != null && sourceMap.containsKey(uuidIn)) { source = sourceMap.get(uuidIn); }
				}
			}
			catch (IllegalAccessException e) { LogWriter.debug(e.toString()); }
		}
		if (!durations.containsKey(resource)) {
			try {
				IResource res = Minecraft.getMinecraft().getResourceManager().getResource(sound.getSound().getSoundAsOggLocation());
				long rate = -1;
				long length = -1;
				InputStream stream = res.getInputStream();
				int size = stream.available();
				byte[] t = new byte[size];
				int offset = 0;
				int remaining = size;
				while (remaining > 0) {
					int count = stream.read(t, offset, remaining);
					if (count == -1) { break; }
					offset += count;
					remaining -= count;
				}
				// 4 bytes for "OggS", 2 unused bytes, 8 bytes for length
				for (int i = size-1-8-2-4; i >= 0 && length < 0; i--) {
					// Looking for length (value after last "OggS")
					if (t[i]==(byte)'O' && t[i+1]==(byte)'g' && t[i+2]==(byte)'g' && t[i+3]==(byte)'S') {
						byte[] byteArray = new byte[]{t[i+6],t[i+7],t[i+8],t[i+9],t[i+10],t[i+11],t[i+12],t[i+13]};
						ByteBuffer bb = ByteBuffer.wrap(byteArray);
						bb.order(ByteOrder.LITTLE_ENDIAN);
						length = bb.getInt(0);
					}
				}
				for (int i = 0; i<size-8-2-4 && rate<0; i++) {
					// Looking for rate (first value after "vorbis")
					if (t[i]==(byte)'v' && t[i+1]==(byte)'o' && t[i+2]==(byte)'r' && t[i+3]==(byte)'b' && t[i+4]==(byte)'i' && t[i+5]==(byte)'s') {
						byte[] byteArray = new byte[]{t[i+11],t[i+12],t[i+13],t[i+14]};
						ByteBuffer bb = ByteBuffer.wrap(byteArray);
						bb.order(ByteOrder.LITTLE_ENDIAN);
						rate = bb.getInt(0);
					}
				}
				stream.close();
				durations.put(resource, Math.max(0L, length * 1000L / rate));
			}
			catch (Exception ignored) { }
		}
		duration = durations.getOrDefault(resource, 0L);
	}

	public long getCurrentTime() {
		if (source == null && sndSystem != null) {
			Library soundLibrary = ((ISoundSystemMixin) sndSystem).getSoundLibrary();
			HashMap<String, Source> sourceMap = ((ILibraryMixin) soundLibrary).getSourceMap();
			if (sourceMap != null && sourceMap.containsKey(uuid)) { source = sourceMap.get(uuid); }
		}
		return source == null || duration < source.millisecondsPlayed() ? duration : (long) source.millisecondsPlayed(); }

	/**
	 * @param player player in game
	 * @param type   0:start; 1:tick; 2:stop
	 */
	public void createClientEvent(Event eventIn, EntityPlayer player, int type) {
		IPlayer<?> iPlayer = null;
		NpcAPI api = NpcAPI.Instance();
		IPos pos = BlockPosWrapper.ORIGIN;
		if (api != null) {
			IEntity<?> iEntity = api.getIEntity(player);
			if (iEntity instanceof IPlayer<?>) { iPlayer = (IPlayer<?>) iEntity; }
			pos = getPos();
		}
		EnumScriptType sType;
		ForgeEvent ev;
		switch (type) {
			case 1: {
				sType = EnumScriptType.SOUND_TICK_EVENT;
				ev = new ForgeEvent.ClientSoundTickEvent(eventIn, iPlayer, name, resource.toString(), pos, sound.getVolume(), sound.getPitch(), getCurrentTime(), duration);
				break;
			}
			case 2: {
				sType = EnumScriptType.STOP_SOUND;
				ev = new ForgeEvent.ClientSoundStopEvent(eventIn, iPlayer, name, resource.toString(), pos, sound.getVolume(), sound.getPitch(), getCurrentTime(), duration);
				break;
			}
			default: {
				sType = EnumScriptType.PLAY_SOUND;
				ev = new ForgeEvent.ClientSoundPlayEvent(eventIn, iPlayer, name, resource.toString(), pos, sound.getVolume(), sound.getPitch(), getCurrentTime(), duration);
				break;
			}
		}
		if (EventHooks.onEvent(ScriptController.Instance.clientScripts, sType, ev) && source != null) {
			source.stop();
		}
	}

	public IPos getPos() {
		double x, y, z;
		if (source != null) {
			x = source.position.x;
			y = source.position.y;
			z = source.position.z;
		}
		else {
			x = sound.getXPosF();
			y = sound.getYPosF();
			z = sound.getZPosF();
		}
		return new BlockPosWrapper(null, x, y, z);
	}

	public boolean playing() {
		try {
			if (source == null && sndSystem != null) {
				Library soundLibrary = ((ISoundSystemMixin) sndSystem).getSoundLibrary();
				HashMap<String, Source> sourceMap = ((ILibraryMixin) soundLibrary).getSourceMap();
				if (sourceMap != null && sourceMap.containsKey(uuid)) { source = sourceMap.get(uuid); }
			}
			return source != null && source.playing();
		} catch (Exception ignored) { }
		return false;
	}

	public boolean stopped() {
		try {
			if (source == null && sndSystem != null) {
				Library soundLibrary = ((ISoundSystemMixin) sndSystem).getSoundLibrary();
				HashMap<String, Source> sourceMap = ((ILibraryMixin) soundLibrary).getSourceMap();
				if (sourceMap != null && sourceMap.containsKey(uuid)) { source = sourceMap.get(uuid); }
			}
			return source == null || source.stopped();
		} catch (Exception ignored) { }
		return false;
	}

	public void setPos(float x, float y, float z) {
		if (source != null) {
			source.position.x = x;
			source.position.y = y;
			source.position.z = z;
		}
		if (sound instanceof PositionedSound) {
			IPositionedSoundMixin posMixSound = (IPositionedSoundMixin) sound;
			posMixSound.setXPosF(x);
			posMixSound.setYPosF(y);
			posMixSound.setZPosF(z);
		}
	}

}
