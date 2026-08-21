package noppes.npcs.mixin.client.sounds;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = SoundEngine.class, priority = 498)
public interface ISoundEngineMixin {

    @Accessor boolean getLoaded();

    @Accessor int getTickCount();

    @Accessor Map<SoundInstance, Integer> getSoundDeleteTime();

    @Accessor Map<SoundInstance, ChannelAccess.ChannelHandle> getInstanceToChannel();

}
