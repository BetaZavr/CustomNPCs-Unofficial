package noppes.npcs.client.gui.select;

import java.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.ClientTickHandler;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.util.MusicData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.client.sounds.IWeighedSoundEventsMixin;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;
import org.jetbrains.annotations.NotNull;

public class SubGuiSoundSelection extends ResourceSelection {

   protected static final Map<String, List<Component>> hoversData = new HashMap<>();
   protected static final Map<String, WeighedSoundEvents> eventsData = new HashMap<>();

   protected SoundManager handler;
   protected GuiLabel name;
   protected GuiLabel time;
   protected MusicData musicData;
   protected Component error;
   protected boolean isPlay;
   protected long delay;
   protected long wait;
   protected int option = 0;

   public SubGuiSoundSelection(Screen parentIn, int idIn, EntityNPCInterface npcIn, String startIn) {
      super(parentIn, idIn, npcIn, startIn, ".ogg");
   }

   @Override
   public void init() {
      super.init();
      if (scroll == null) { scroll = addScroll(0); }
      scroll.setSize(scrollWidth, 168);
      int h = guiTop + imageHeight - 25;
      List<Object> options = new ArrayList<>();
      options.add("spawner.random");
      if (!select.getString().isEmpty() && selectDir != null) {
         String key = selectDir.getNamespace() + ":" + select.getString();
         if (eventsData.containsKey(key)) {
            for (Weighted<Sound> weighted : ((IWeighedSoundEventsMixin) eventsData.get(key)).getList()) {
               if (weighted instanceof Sound sound) {
                  options.add(Component.literal(sound.getLocation().getPath().replaceAll("/", ".")));
               }
               else {
                  for (int i = 0; i < weighted.getWeight(); i++) {
                     options.add(Component.literal(weighted.getSound(getIntSource(i)).getLocation().getPath().replaceAll("/", ".")));
                  }
               }
            }
         }
      }
      addButton(5, guiLeft + 216, h, options.size() > 1, option, options.toArray(new Object[0]))
              .setSize(78, 20)
              .setIsEnabled(options.size() > 1)
              .setHoverTexts("selection.sound.hover.options");
      addButton(3, guiLeft + 144, h, "gui.play")
              .setSize(70, 20)
              .setIsEnabled(selectDir != null && resource != null && scroll.hasSelected())
              .hasSound = false;
      addButton(4, guiLeft + 72, h, "gui.copy")
              .setSize(70, 20)
              .setIsEnabled(selectDir != null && resource != null && scroll.hasSelected());
      if (selectDir != null && !selectDir.getNamespace().isEmpty()) {
         LinkedHashMap<Integer, List<Component>> map = new LinkedHashMap<>();
         int i = 0;
         for (String line : scroll.getList()) {
            map.put(i++, hoversData.computeIfAbsent(selectDir.getNamespace() + ":" + line, k -> new ArrayList<>()));
         }
         scroll.setHoverTexts(map);
      }
      name = new GuiLabel(this, 1, "", guiLeft + 6, guiTop + imageHeight - 36)
              .setSize(imageWidth - 93, 10)
              .setColor(CustomNpcs.MainColor.getRGB());
      if (musicData != null) {
         name.setMessage(Component.literal(musicData.name)
                 .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                 .append(Component.literal(musicData.resource.getPath().replaceAll("/", ".")).withStyle(ChatFormatting.GRAY)));
      }
      time = new GuiLabel(this, 2, "", guiLeft + imageWidth - 85, guiTop + imageHeight - 36)
              .setSize(80, 10)
              .setColor(CustomNpcs.MainColor.getRGB())
              .setCentered(true);
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 3: {
            if (minecraft == null) { minecraft = Minecraft.getInstance(); }
            MusicController.Instance.stopSounds();
            WeighedSoundEvents event = eventsData.get(resource.toString());
            isPlay = true;
            musicData = null;
            error = null;
            delay = 0L;
            wait = System.currentTimeMillis();
            if (event == null) {
               MusicController.Instance.playSound(SoundSource.NEUTRAL, resource.toString(), player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F);
            }
            else {
               int i = (int) (Math.random() * (double) event.getWeight());
               GuiButtonNop b = getButton(5);
               if (event.getWeight() > 0 && b != null && b.getValue() > 0) {
                  for (int j = 0; j < event.getWeight(); j++) {
                     Sound s = event.getSound(getIntSource(j));
                     if (b.getMessage().getString().equals(s.getLocation().getPath().replaceAll("/", "."))) { i = j; }
                  }
               }
               RandomSource randomSource = getIntSource(i);
               Sound sound = event.getSound(randomSource);
               minecraft.getSoundManager().play(new SimpleSoundInstance(resource, SoundSource.NEUTRAL, 1.0F, 1.0F,
                       randomSource, false, 0, SoundInstance.Attenuation.LINEAR,
                       player.getX(), player.getY(), player.getZ(), false));
               name.setMessage(Component.literal(resource.getPath()));
               time.setMessage(Component.empty());
               CustomNPCsScheduler.runTack(() -> {
                  long n = System.currentTimeMillis() + 2500L;
                  while (n >= System.currentTimeMillis()) {
                     for (MusicData md : ClientTickHandler.musics) {
                        if (md.channel.playing() && md.name.equals(resource.getPath()) && md.resource.equals(sound.getLocation())) {
                           musicData = md;
                           name.setMessage(Component.literal(musicData.name)
                                   .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                                   .append(Component.literal(musicData.resource.getPath().replaceAll("/", ".")).withStyle(ChatFormatting.GRAY)));
                           error = null;
                           LogWriter.info("Sound found and is played: "+md.name+"; option: "+md.resource);
                           return;
                        }
                     }
                  }
                  error = Component.literal(resource.getPath())
                          .append(" - ")
                          .append(Component.translatable("quest.task.location.1"));
               }, 100);
            }
            break;
         }
         case 4: if (resource != null) { NoppesStringUtils.setClipboardContents(resource.toString()); } break;
         case 5: {
            if (button.getValue() > 0) {
               if (button.getMessage().getContents() instanceof TranslatableContents tr) {
                  button.setHoverTexts(Component.translatable("selection.sound.hover.options")
                          .append("<br>")
                          .append(Component.translatable("gui.name").append(": ").withStyle(ChatFormatting.GRAY))
                          .append(tr.getKey()));
               }
            }
            break;
         }
         default: super.buttonEvent(button);
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      int left = guiLeft + 5;
      int right = guiLeft + imageWidth - 5;
      int top = guiTop + imageHeight - 37;
      int bottom = guiTop + imageHeight - 27;
      graphics.fill(left, top, right, bottom, 0xFF808080);
      if (isPlay) {
         if (minecraft == null) { minecraft = Minecraft.getInstance(); }
         int alpha = 0x80;
         if (delay > 0L) {
            alpha = Math.min(128, Math.max(0, (int) ((double) (delay - System.currentTimeMillis()) * 0.064d)));
         }
         graphics.fill(left, top, right, bottom, alpha << 24);
         if (error != null) {
            if (delay == 0L) { delay = System.currentTimeMillis() + 4000L; }
            graphics.drawCenteredString(minecraft.font, error, (right-left)/2, top + 1, CustomNpcs.MainColor.getRGB());
            if (delay < System.currentTimeMillis()) { isPlay = false; }
            return;
         }
         if (musicData != null) {
            double track = (double) musicData.getCurrentTime() / (double) musicData.duration;
            int len = left + (int) (track * ((double) imageWidth - 10.0d));
            time.setMessage(Component.literal(Util.instance.ticksToElapsedTime(musicData.getCurrentTime() / 50L, false, false, false))
                    .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(Util.instance.ticksToElapsedTime(musicData.duration / 50L, false, false, false)).withStyle(ChatFormatting.GRAY)));
            graphics.fill(left, top, len, bottom, 0xF000 | alpha << 24);
            if (musicData.channel.stopped()) {
               if (delay == 0L) { delay = System.currentTimeMillis() + 2000L; }
               if (delay < System.currentTimeMillis()) { isPlay = false; }
            }
         } else {
            time.setMessage(Component.literal(Util.instance.ticksToElapsedTime((2500L - Math.min(2500L, Math.max(0L, System.currentTimeMillis() - wait))) / 50, false, false, false)).withStyle(ChatFormatting.GRAY));
         }
         name.render(graphics, mouseX, mouseY, partialTicks);
         time.render(graphics, mouseX, mouseY, partialTicks);
      }
   }

   @Override
   public void onClose() {
      MusicController.Instance.stopSounds();
      super.onClose();
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      if (!scroll.getSelected().isEmpty() && scroll.getSelected().equals(select.getString())) { return; }
      option = 0;
      super.scrollClicked(scroll);
      if (getButton(3) != null) {
         getButton(3).setIsEnabled(selectDir != null && resource != null && scroll.hasSelected());
      }
      if (getButton(4) != null) {
         getButton(4).setIsEnabled(selectDir != null && resource != null && scroll.hasSelected());
      }
      init();
   }

   @Override
   protected void loadFiles() {
      Map<String, TreeMap<ResourceLocation, Long>> cache = resourcesData.get(suffix);
      if (cache != null) { data.putAll(cache); return; }
      resetFiles();
      resourcesData.put(suffix, new TreeMap<>(data));
   }

   @Override
   protected void resetFiles() {
      data.clear();
      handler = Minecraft.getInstance().getSoundManager();
      Collection<ResourceLocation> set = handler.getAvailableSounds();
      for (ResourceLocation location : set) { addFile(location); }
   }

   @Override
   protected void addFile(ResourceLocation location) {
      WeighedSoundEvents event = handler.getSoundEvent(location);
      if (event == null) { return; }
      String path = location.getPath();
      String domain = location.getNamespace();
      if (!data.containsKey(domain)) { data.put(domain, new TreeMap<>()); }
      else {
         for (ResourceLocation r : data.get(domain).keySet()) {
            if (r.getPath().equals(path)) { return; }
         }
      }
      List<Component> hovers = new ArrayList<>();
      hovers.add(Component.empty()
              .append(Component.translatable("gui.name").append(": ").withStyle(ChatFormatting.GRAY))
              .append(location.toString()).withStyle(ChatFormatting.GOLD));
      if (event.getSubtitle() != null) {
         hovers.add(Component.empty()
                 .append(Component.translatable("gui.title").append(": ").withStyle(ChatFormatting.GRAY))
                 .append(event.getSubtitle()).withStyle(ChatFormatting.RESET));
      }
      int size = 0;
      for (Weighted<Sound> weighted : ((IWeighedSoundEventsMixin) event).getList()) {
         if (weighted instanceof Sound) { size++; }
         else { size += weighted.getWeight(); }
      }
      hovers.add(Component.translatable("gui.options").append(" (" + size + "):").withStyle(ChatFormatting.GRAY));
      for (Weighted<Sound> weighted : ((IWeighedSoundEventsMixin) event).getList()) {
         boolean bo = false;
         if (weighted instanceof Sound sound) {
            if (hovers.size() > 8) {
               hovers.add(Component.literal("..."));
               break;
            }
            hovers.add(Component.empty()
                    .append(Component.literal(sound.getLocation().getNamespace() + ":").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(sound.getLocation().getPath())));
         }
         else {
            for (int i = 0; i < weighted.getWeight(); i++) {
               if (hovers.size() > 8) {
                  hovers.add(Component.literal("..."));
                  bo = true;
                  break;
               }
               Sound sound = weighted.getSound(getIntSource(i));
               hovers.add(Component.empty()
                       .append(Component.literal(sound.getLocation().getNamespace() + ":").withStyle(ChatFormatting.DARK_GRAY))
                       .append(Component.literal(sound.getLocation().getPath())));
            }
         }
         if (bo) { break; }
      }
      hoversData.put(location.toString(), hovers);
      eventsData.put(location.toString(), event);
      data.get(domain).put(location, 0L);
   }

   private RandomSource getIntSource(int i) {
      return new RandomSource() {
         @Override
         public @NotNull RandomSource fork() { return RandomSource.create().fork(); }

         @Override
         public @NotNull PositionalRandomFactory forkPositional() { return RandomSource.create().forkPositional(); }

         @Override
         public void setSeed(long seed) { }

         @Override
         public int nextInt() { return i; }

         @Override
         public int nextInt(int max) { return i; }

         @Override
         public long nextLong() { return i; }

         @Override
         public boolean nextBoolean() { return false; }

         @Override
         public float nextFloat() { return i; }

         @Override
         public double nextDouble() { return i; }

         @Override
         public double nextGaussian() { return i; }

      };
   }

}
