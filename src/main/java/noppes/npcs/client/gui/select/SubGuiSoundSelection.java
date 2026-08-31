package noppes.npcs.client.gui.select;

import java.util.*;

import net.minecraft.client.audio.*;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.ClientTickHandler;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.util.MusicData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.client.audio.ISoundEventAccessorMixin;
import noppes.npcs.mixin.client.audio.ISoundHandlerMixin;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

public class SubGuiSoundSelection extends ResourceSelection {

	protected static final Map<String, List<Component>> hoversData = new HashMap<>();
	protected static final Map<String, SoundEventAccessor> eventsData = new HashMap<>();

	protected SoundRegistry handler;
	protected GuiLabel name;
	protected GuiLabel time;
	protected MusicData musicData;
	protected Component error;
	protected boolean isPlay;
	protected long delay;
	protected long wait;
	protected int option = 0;

	protected static List<Sound> getSounds(ISoundEventAccessor<Sound> accessor) {
		List<Sound> sounds = new ArrayList<>();
		if (accessor instanceof Sound) { sounds.add((Sound) accessor); }
		else if (accessor instanceof ISoundEventAccessorMixin) {
			for (ISoundEventAccessor<Sound> sub : ((ISoundEventAccessorMixin) accessor).getAccessorList()) {
				if (sub != accessor) { sounds.addAll(getSounds(sub)); }
			}
		}
		return sounds;
	}

	public SubGuiSoundSelection(GuiScreen parentIn, int idIn, EntityNPCInterface npcIn, String startIn) {
		super(parentIn, idIn, npcIn, startIn, ".ogg");
		loadFiles();
	}

	@Override
	public void initGui() {
		super.initGui();
		if (scroll == null) { scroll = addScroll(0); }
		scroll.setSize(scrollWidth, 168);
		int h = guiTop + imageHeight - 25;
		List<Object> options = new ArrayList<>();
		options.add("spawner.random");
		if (!select.getFormattedText().isEmpty() && selectDir != null) {
			String key = selectDir.getResourceDomain() + ":" + select.getString();
			if (eventsData.containsKey(key)) {
				for (Sound sound : getSounds(eventsData.get(key))) {
					options.add(Component.literal(sound.getSoundLocation().getResourcePath().replaceAll("/", ".")));
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
		if (selectDir != null && !selectDir.getResourceDomain().isEmpty()) {
			LinkedHashMap<Integer, List<Component>> map = new LinkedHashMap<>();
			int i = 0;
			for (String line : scroll.getList()) {
				map.put(i++, hoversData.computeIfAbsent(selectDir.getResourceDomain() + ":" + line, k -> new ArrayList<>()));
			}
			scroll.setHoverTexts(map);
		}
		name = new GuiLabel(this, 1, "", guiLeft + 6, guiTop + imageHeight - 36)
				.setSize(imageWidth - 93, 10)
				.setColor(CustomNpcs.MainColor.getRGB());
		if (musicData != null) {
			name.setMessage(Component.literal(musicData.name)
					.append(Component.literal(" / ").withStyle(TextFormatting.GRAY))
					.append(Component.literal(musicData.resource.getResourcePath().replaceAll("/", ".")).withStyle(TextFormatting.GRAY)));
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
				MusicController.Instance.stopSounds();
				SoundEventAccessor event = eventsData.get(resource.toString());
				isPlay = true;
				error = null;
				delay = 0L;
				wait = System.currentTimeMillis();
				ResourceLocation played = resource;
				ISound playing = null;
				List<Sound> sounds = getSounds(event);
				if (sounds.isEmpty()) {
					MusicController.Instance.playSound(SoundCategory.MASTER, played.toString(), player.posX, player.posY, player.posZ, 1.0F, 1.0F);
				}
				else {
					int i = (int) (Math.random() * (double) sounds.size());
					GuiButtonNop b = getButton(5);
					if (b != null && b.getValue() > 0) {
						for (int j = 0; j < sounds.size(); j++) {
							if (b.getMessage().getString().equals(sounds.get(j).getSoundLocation().getResourcePath().replaceAll("/", "."))) { i = j; }
						}
					}
					final Sound chosen = sounds.get(Math.max(0, Math.min(i, sounds.size() - 1)));
					PositionedSoundRecord playingSong = new PositionedSoundRecord(event.getLocation(), SoundCategory.MASTER, 1.0f, 1.0f, false, 0,
							ISound.AttenuationType.NONE, (float) player.posX, (float) player.posY, (float) player.posZ) {
						@Override
						public SoundEventAccessor createAccessor(@Nonnull SoundHandler handlerIn) {
							SoundEventAccessor accessor = super.createAccessor(handlerIn);
							this.sound = chosen;
							return accessor;
						}
					};
					minecraft.getSoundHandler().playSound(playingSong);
					playing = playingSong;
				}
				name.setMessage(Component.literal(played.getResourcePath()));
				time.setMessage(Component.empty());
				// Create MusicData synchronously so the progress bar works even if the Forge event doesn't fire
				try {
					java.lang.reflect.Field soundSystemField = null;
					net.minecraft.client.audio.SoundHandler sh = minecraft.getSoundHandler();
					for (java.lang.reflect.Field f : sh.getClass().getDeclaredFields()) {
						if (f.getType().getName().contains("SoundSystem")) {
							f.setAccessible(true);
							soundSystemField = f;
							break;
						}
					}
					if (soundSystemField != null) {
						java.lang.reflect.Field soundManagerField = null;
						for (java.lang.reflect.Field f : sh.getClass().getDeclaredFields()) {
							if (f.getType().getName().contains("SoundManager") || f.getType().getSimpleName().equals("sndManager")) {
								f.setAccessible(true);
								soundManagerField = f;
								break;
							}
						}
						if (soundManagerField != null) {
							net.minecraft.client.audio.SoundManager manager = (net.minecraft.client.audio.SoundManager) soundManagerField.get(sh);
							if (manager != null && playing != null) {
								musicData = new noppes.npcs.client.util.MusicData(playing, playing.getSound().getSoundLocation().toString(), manager);
							}
						}
					}
				}
				catch (Exception ignored) { }
				if (musicData == null) {
					// Fallback: try to find it from the Forge event within 2.5s
					CustomNPCsScheduler.runTack(() -> {
						long n = System.currentTimeMillis() + 2500L;
						while (n >= System.currentTimeMillis()) {
							for (noppes.npcs.client.util.MusicData md : ClientTickHandler.musics) {
								if (md != null && md.resource.equals(played)) {
									musicData = md;
									name.setMessage(Component.literal(musicData.name)
											.append(Component.literal(" / ").withStyle(TextFormatting.GRAY))
											.append(Component.literal(musicData.resource.getResourcePath().replaceAll("/", ".")).withStyle(TextFormatting.GRAY)));
									error = null;
									LogWriter.info("Sound found via Forge event: "+md.name+"; option: "+md.resource);
									return;
								}
							}
						}
						if (musicData == null) {
							error = Component.literal(resource.getResourcePath())
									.append(" - ")
									.append(Component.translatable("quest.task.location.1"));
						}
					}, 100);
				}
				else {
					name.setMessage(Component.literal(musicData.name)
							.append(Component.literal(" / ").withStyle(TextFormatting.GRAY))
							.append(Component.literal(musicData.resource.getResourcePath().replaceAll("/", ".")).withStyle(TextFormatting.GRAY)));
					LogWriter.info("Sound created synchronously: "+musicData.name+"; duration: "+musicData.duration);
				}
				break;
			} // play
			case 4: if (resource != null) { NoppesStringUtils.setClipboardContents(resource.toString()); } break; // copy
			case 5: {
				if (button.getValue() > 0) {
					if (button.getMessage().getContents() instanceof TextComponentTranslation) {
						button.setHoverTexts(Component.translatable("selection.sound.hover.options")
								.append("<br>")
								.append(Component.translatable("gui.name").append(": ").withStyle(TextFormatting.GRAY))
								.append(((TextComponentTranslation) button.getMessage().getContents()).getKey()));
					}
				}
				break;
			}
			default: super.buttonEvent(button);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		int left = guiLeft + 5;
		int right = guiLeft + imageWidth - 5;
		int top = guiTop + imageHeight - 37;
		int bottom = guiTop + imageHeight - 27;
		drawRect(left, top, right, bottom, 0xFF808080);
		if (isPlay) {
			int alpha = 0x80;
			if (delay > 0L) {
				alpha = Math.min(128, Math.max(0, (int) ((double) (delay - System.currentTimeMillis()) * 0.064d)));
			}
			drawRect(left, top, right, bottom, alpha << 24);
			if (error != null) {
				if (delay == 0L) { delay = System.currentTimeMillis() + 4000L; }
				drawCenteredString(font, error.getFormattedText(), (left+right)/2, top + 1, CustomNpcs.MainColor.getRGB());
				if (delay < System.currentTimeMillis()) { isPlay = false; }
				return;
			}
			if (musicData != null && musicData.duration > 0) {
				long currentTime = Math.min(musicData.getCurrentTime(), musicData.duration);
				double track = (double) currentTime / (double) musicData.duration;
				int len = left + (int) (track * ((double) imageWidth - 10.0d));
				time.setMessage(Component.literal(Util.instance.ticksToElapsedTime(currentTime / 50L, false, false, false))
						.append(Component.literal("/").withStyle(TextFormatting.GRAY))
						.append(Component.literal(Util.instance.ticksToElapsedTime(musicData.duration / 50L, false, false, false)).withStyle(TextFormatting.GRAY)));
				drawRect(left, top, len, bottom, 0xF000 | alpha << 24);
				if (musicData.stopped()) {
					if (delay == 0L) { delay = System.currentTimeMillis() + 2000L; }
					if (delay < System.currentTimeMillis()) { isPlay = false; }
				}
			} else if (musicData == null) {
				// Waiting for sound data to be available
				time.setMessage(Component.literal("...").withStyle(TextFormatting.GRAY));
			}
			name.render(mouseX, mouseY, partialTicks);
			time.render(mouseX, mouseY, partialTicks);
		}
	}

	@Override
	public void onClose() {
		MusicController.Instance.stopSounds();
		super.onClose();
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (!scroll.getSelected().isEmpty() && scroll.getSelected().equals(select.getFormattedText())) { return; }
		option = 0;
		super.scrollClicked(scroll);
		if (getButton(3) != null) {
			getButton(3).setIsEnabled(selectDir != null && resource != null && scroll.hasSelected());
		}
		if (getButton(4) != null) {
			getButton(4).setIsEnabled(selectDir != null && resource != null && scroll.hasSelected());
		}
		initGui();
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
		handler = ((ISoundHandlerMixin) minecraft.getSoundHandler()).getSoundRegistry();
		for (ResourceLocation location : handler.getKeys()) { addFile(location); }
	}

	@Override
	protected void addFile(ResourceLocation location) {
		SoundEventAccessor event = handler.getObject(location);
		if (event == null) { return; }
		String path = location.getResourcePath();
		String domain = location.getResourceDomain();
		if (!data.containsKey(domain)) { data.put(domain, new TreeMap<>()); }
		else {
			for (ResourceLocation r : data.get(domain).keySet()) {
				if (r.getResourcePath().equals(path)) { return; }
			}
		}
		List<Component> hovers = new ArrayList<>();
		hovers.add(Component.empty()
				.append(Component.translatable("gui.name").append(": ").withStyle(TextFormatting.GRAY))
				.append(location.toString()).withStyle(TextFormatting.GOLD));
		if (event.getSubtitle() != null) {
			hovers.add(Component.empty()
					.append(Component.translatable("gui.title").append(": ").withStyle(TextFormatting.GRAY))
					.append(event.getSubtitle()).withStyle(TextFormatting.RESET));
		}
		List<Sound> sounds = getSounds(event);
		hovers.add(Component.translatable("gui.options").append(" (" + sounds.size() + "):").withStyle(TextFormatting.GRAY));
		for (Sound sound : sounds) {
			if (hovers.size() > 8) {
				hovers.add(Component.literal("..."));
				break;
			}
			hovers.add(Component.empty()
					.append(Component.literal(sound.getSoundLocation().getResourceDomain() + ":").withStyle(TextFormatting.DARK_GRAY))
					.append(Component.literal(sound.getSoundLocation().getResourcePath())));
		}
		hoversData.put(location.toString(), hovers);
		eventsData.put(location.toString(), event);
		data.get(domain).put(location, 0L);
	}

}
