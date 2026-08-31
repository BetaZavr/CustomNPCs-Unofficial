package noppes.npcs.client.util;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.EventHooks;
import noppes.npcs.api.IPos;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.ForgeEvent;
import noppes.npcs.api.mixin.com.mojang.blaze3d.audio.IChannelMixin;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.mixin.client.resources.sounds.IAbstractSoundInstanceMixin;
import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MusicData {

    protected static final Map<ResourceLocation, Long> durations = new HashMap<>();
    public final long duration;
    public final Channel channel;
    public final SoundInstance sound;
    public final String name;
    public final ResourceLocation resource;
    public final SoundSource category;
    private final ClientLevel level;

    public MusicData(String nameIn, SoundInstance soundIn, Channel channelIn, ClientLevel levelIn) {
        name = nameIn;
        sound = soundIn;
        resource = sound.getSound().getLocation();
        category = sound.getSource();
        channel = channelIn;
        level = levelIn;
        if (!durations.containsKey(resource)) {
            try {
                Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(sound.getSound().getPath());
                if (res.isPresent()) {
                    long rate = -1;
                    long length = -1;
                    InputStream stream = res.get().open();
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
            }
            catch (Exception ignored) { }
        }
        duration = durations.getOrDefault(resource, 0L);
    }

    public long getCurrentTime() { return Math.min(((IChannelMixin) channel).npcs$getCurrentTime(), duration); }

    /**
     * @param player player in game
     * @param type 0:start; 1:tick; 2:stop
     */
    public void createClientEvent(Event eventIn, Player player, int type) {
        IPlayer<?> iPlayer = null;
        NpcAPI api = NpcAPI.Instance();
        IPos pos = BlockPosWrapper.ZERO;
        if (api != null) {
            IEntity<?> iEntity = api.getIEntity(player);
            if (iEntity instanceof IPlayer<?> p) { iPlayer = p; }
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
        if (EventHooks.onEvent(ScriptController.Instance.clientScripts, sType, ev)) {
            try { channel.stopped(); } catch (Exception ignored) { }
        }
    }

    public IPos getPos() {
        double x, y, z;
        if (channel != null) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(3);
                AL10.alGetSourcefv(((IChannelMixin) channel).npcs$getSource(), AL10.AL_POSITION, buffer);
                x = buffer.get(0);
                y = buffer.get(1);
                z = buffer.get(2);
            }
            catch (Exception e) { x = 0; y = 0; z = 0; }
        }
        else {
            x = sound.getX();
            y = sound.getY();
            z = sound.getZ();
        }
        return new BlockPosWrapper(level, x, y, z);
    }

    public void setPos(double x, double y, double z) {
        if (channel != null) {
            channel.setSelfPosition(new Vec3(x, y, z));
        }
        if (sound instanceof IAbstractSoundInstanceMixin posMixSound) {
            posMixSound.setX(x);
            posMixSound.setY(y);
            posMixSound.setZ(z);
        }
    }

}
