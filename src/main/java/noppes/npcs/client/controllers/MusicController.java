package noppes.npcs.client.controllers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.client.ClientTickHandler;
import noppes.npcs.client.TranslateUtil;
import noppes.npcs.client.util.MusicData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.client.sounds.ISoundEngineMixin;
import noppes.npcs.mixin.client.sounds.ISoundManagerMixin;
import noppes.npcs.roles.JobBard;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class MusicController {

   public static MusicController Instance;

   // New Unofficial (Goodbird)
   SimpleSoundInstance dialogSound = null;

   // New from Unofficial (BetaZavr)
   public @Nullable ResourceLocation song;
   public @Nullable ResourceLocation music;
   public @Nullable Entity songBard;
   public @Nullable Entity musicBard;
   public @Nullable SoundInstance playingSong;
   public @Nullable SoundInstance playingMusic;
   public boolean unloadMusicBard = false;
   public boolean unloadSongBard = false;

   public MusicController() { Instance = this; }

   public boolean isPlaying(ResourceLocation resource) {
      SoundEngine soundEngine = ((ISoundManagerMixin) Minecraft.getInstance().getSoundManager()).getSoundEngine();
      if (soundEngine instanceof ISoundEngineMixin mix && mix.getLoaded()) {
         Map<SoundInstance, Integer> soundDeleteTime = mix.getSoundDeleteTime();
         Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel = mix.getInstanceToChannel();
         for (SoundInstance sound : soundDeleteTime.keySet()) {
            if (sound.getLocation().equals(resource) &&
                    soundDeleteTime.containsKey(sound) && soundDeleteTime.get(sound) <= mix.getTickCount() ||
                    instanceToChannel.containsKey(sound)) { return true; }
         }
      }
      return false;
   }

   public void playSound(SoundSource category, String sound, double x, double y, double z, float volume, float pitch) {
      if (category == null || sound == null || sound.isEmpty()) { return; }
      ResourceLocation res = new ResourceLocation(sound);
      if (category == SoundSource.MUSIC && isPlaying(res)) { return; }
      Attenuation aType = Attenuation.LINEAR;
      Minecraft mc = Minecraft.getInstance();
      if (category == SoundSource.MUSIC) {
         mc.getSoundManager().stop(music, SoundSource.MUSIC);
         aType = Attenuation.NONE;
         x = mc.player != null ? (float) mc.player.getX() : 0.0f;
         y = mc.player != null ? (float) mc.player.getY() + 0.5f : 0.0f;
         z = mc.player != null ? (float) mc.player.getZ() : 0.0f;
      }
      Minecraft.getInstance().getSoundManager().play(
              new SimpleSoundInstance(res, category, volume, pitch,
                      SoundInstance.createUnseededRandom(), false, 0, aType, x, y, z, false));
   }

   public void stopSound(@Nullable ResourceLocation soundIn, SoundSource category) {
      Minecraft.getInstance().getSoundManager().stop(soundIn, category);
      if (category == SoundSource.AMBIENT && soundIn == null || Objects.equals(soundIn, song)) {
         song = null;
         playingSong = null;
         songBard = null;
      }
      else if (category == SoundSource.MUSIC && soundIn == null || Objects.equals(soundIn, music)) {
         music = null;
         musicBard = null;
         playingMusic = null;
      }
   }

   public void stopSounds() {
      SoundManager handler = Minecraft.getInstance().getSoundManager();
      for (SoundSource soundSource : SoundSource.values()) { handler.stop(null, soundSource); }
      song = null;
      music = null;
      songBard = null;
      musicBard = null;
      playingSong = null;
      playingMusic = null;
   }

   public void playStreaming(ResourceLocation sound, Entity entity, boolean isLooping) {
      if (!isPlaying(sound)) {
         stopSounds();
         song = sound;
         if (song != null) {
            songBard = entity;
            Minecraft.getInstance().getSoundManager().play(playingSong = new SimpleSoundInstance(sound, SoundSource.RECORDS, 4.0F, 1.0F,
                    SoundInstance.createUnseededRandom(), isLooping, 0,
                    Attenuation.LINEAR, entity.getX(), entity.getY(), entity.getZ(), false));
         }
      }
   }

   public void playMusic(ResourceLocation sound, Entity entity, boolean isLooping) {
      if (!isPlaying(sound)) {
         stopMusics();
         music = sound;
         if (music != null) {
            musicBard = entity;
            Minecraft.getInstance().getSoundManager().play(playingSong = new SimpleSoundInstance(sound, SoundSource.MUSIC, 1.0F, 1.0F,
                    SoundInstance.createUnseededRandom(), isLooping, 0,
                    Attenuation.NONE, 0, 0, 0, false));
         }
      }
   }

   public void speak(String languageKey, String text, float volume) {
      CustomNPCsScheduler.runTack(() -> {
         try {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) { return; }
            URLConnection connection = new URL(String.format(TranslateUtil.AudioUrl, URLEncoder.encode(text, StandardCharsets.UTF_8), languageKey)).openConnection();
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("User-Agent", "Chrome/99.0.4844.51");
            connection.setConnectTimeout(10000);
            InputStream stream = connection.getInputStream();

            // Reading all bytes from a stream
            byte[] audioBytes = stream.readAllBytes();
            // Using SourceDataLine to Play Sound
            AudioFormat format = new AudioFormat(
                    16000f, // Sampling frequency
                    16, // Bit depth
                    1, // Number of channels (mono)
                    true, // Signed PCM
                    false // Little-endian
            );
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceDataLine.open(format);
            sourceDataLine.start();

            // Applying Volume and Pitch
            // Adjusting Volume by Changing Signal Amplitude
            float adjustedVolume = Math.min(Math.max(volume, 0f), 1f);  // Limit the volume value within [0, 1]
            for (int i = 0; i < audioBytes.length; i++) {
               // Apply a loudness factor to each sample
               audioBytes[i] = (byte) ((float) audioBytes[i] * adjustedVolume);
            }

            // Let's start playback
            sourceDataLine.write(audioBytes, 0, audioBytes.length);
            sourceDataLine.drain();
            sourceDataLine.stop();
            sourceDataLine.close();

            // Closing Streams
            stream.close();
         } catch (Exception e) {
            LogWriter.error("Error while playing translation \"" + languageKey + "\"", e);
         }
      });
   }

   // New from Unofficial (Goodbird)
   public void playSoundDialog(SoundSource category, ResourceLocation sound, BlockPos pos, float volume, float pitch) {
      if (dialogSound != null) { Minecraft.getInstance().getSoundManager().stop(dialogSound); }
      Minecraft.getInstance().getSoundManager().play(dialogSound = new SimpleSoundInstance(sound,
              category, volume, pitch, SoundInstance.createUnseededRandom(),
              false, 0, Attenuation.LINEAR,
              (float)pos.getX() + 0.5F, pos.getY(), (float)pos.getZ() + 0.5F, false));
   }

   // New from Unofficial (BetaZavr)
   public void bardPlaySound(ResourceLocation sound, boolean isStreamer, EntityNPCInterface npc) {
      stopSound(sound, isStreamer ? SoundSource.AMBIENT : SoundSource.MUSIC);
      Attenuation aType = Attenuation.LINEAR;
      float x = (float) npc.getX();
      float y = (float) npc.getY();
      float z = (float) npc.getZ();
      if (isStreamer) {
         song = sound;
         songBard = npc;
      }
      else {
         music = sound;
         musicBard = npc;
         aType = Attenuation.NONE;
         x = 0.0f;
         y = 0.0f;
         z = 0.0f;
         for (MusicData md : ClientTickHandler.musics) {
            if (!md.name.isEmpty() && md.name.indexOf("minecraft") == 0) {
               Minecraft.getInstance().getSoundManager().stop(md.resource, SoundSource.MUSIC);
            }
         }
      }
      Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(sound, isStreamer ? SoundSource.AMBIENT : SoundSource.MUSIC, 1.0f, 1.0f,
              SoundInstance.createUnseededRandom(), false, 0, aType, x, y, z, false));
   }

   public boolean isBardPlaying(ResourceLocation soundIn, boolean isStreamer) { // check Any Bards
      return isPlaying(soundIn) || (isStreamer ? song != null && isPlaying(song) : music != null && isPlaying(music));
   }

   @SuppressWarnings("unused")
   public void checkBards(Player player) {
      if (music == null) {
         if (musicBard != null) { musicBard = null; }
         if (playingMusic != null) { playingMusic = null; }
      }
      else {
         if (!(musicBard instanceof EntityNPCInterface cnpc) || !(cnpc.job instanceof JobBard)) {
            stopSound(music, SoundSource.MUSIC);
         }
         else {
            Entity entity = player.level().getEntity(musicBard.getId());
            if (entity == null) {
               unloadMusicBard = true;
               JobBard job = (JobBard) cnpc.job;
               if (job.hasOffRange) {
                  int x = job.range[1], y = job.range[1], z = job.range[1];
                  if (!job.isRange) {
                     x = job.maxPos[0];
                     y = job.maxPos[1];
                     z = job.maxPos[2];
                  }
                  int xD = (int) Math.abs(player.getX() - musicBard.getX());
                  int yD = (int) Math.abs(player.getY() - musicBard.getY());
                  int zD = (int) Math.abs(player.getZ() - musicBard.getZ());
                  if (xD > x || yD > y || zD > z) { stopSound(song, SoundSource.MUSIC); }
               }
            }
         }
      }
      if (song == null) {
         if (songBard != null) { songBard = null; }
         if (playingSong != null) { playingSong = null; }
      }
      else {
         if (!(songBard instanceof EntityNPCInterface) || !(((EntityNPCInterface) songBard).job instanceof JobBard)) {
            stopSound(song, SoundSource.AMBIENT);
         } else {
            Entity entity = player.level().getEntity(songBard.getId());
            if (entity == null) {
               unloadSongBard = true;
               JobBard job = (JobBard) ((EntityNPCInterface) songBard).job;
               if (job != null && job.hasOffRange) {
                  int x = job.range[1], y = job.range[1], z = job.range[1];
                  if (!job.isRange) {
                     x = job.maxPos[0];
                     y = job.maxPos[1];
                     z = job.maxPos[2];
                  }
                  int xD = (int) Math.abs(player.getX() - songBard.getX());
                  int yD = (int) Math.abs(player.getY() - songBard.getY());
                  int zD = (int) Math.abs(player.getZ() - songBard.getZ());
                  if (xD > x || yD > y || zD > z) { stopSound(song, SoundSource.AMBIENT); }
               }
            }
         }
      }
   }

   public void stopMusics() {
      Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
      music = null;
      musicBard = null;
      playingMusic = null;
   }

   @SuppressWarnings("unused")
   public void checkBards(LocalPlayer player) { }

   public void setNewPosSong(ResourceLocation resource, double x, double y, double z) {
      if (resource != null) {
         for (MusicData music : new ArrayList<>(ClientTickHandler.musics)) {
            if (music.resource.equals(resource) || music.name.equals(resource.toString())) {
               music.setPos(x, y, z);
            }
         }
      }
   }

}
