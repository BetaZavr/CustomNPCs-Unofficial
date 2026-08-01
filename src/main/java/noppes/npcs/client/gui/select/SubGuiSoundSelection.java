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
import noppes.npcs.mixin.client.audio.IPositionedSoundMixin;
import noppes.npcs.mixin.client.audio.ISoundEventAccessorMixin;
import noppes.npcs.mixin.client.audio.ISoundHandlerMixin;
import noppes.npcs.mixin.client.audio.ISoundRegistryMixin;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

public class SubGuiSoundSelection extends ResourceSelection {

	protected static final Map<String, List<Component>> hoversData = new HashMap<>();
	protected static final Map<String, SoundEventAccessor> eventsData = new HashMap<>();

	protected ISoundRegistryMixin handler;
	protected GuiLabel name;
	protected GuiLabel time;
	protected MusicData musicData;
	protected Component error;
	protected boolean isPlay;
	protected long delay;
	protected long wait;
	protected int option = 0;

	public SubGuiSoundSelection(GuiScreen parentIn, int idIn, EntityNPCInterface npcIn, String startIn) {
		super(parentIn, idIn, npcIn, startIn, ".ogg");
	}

	@Override
	public void initGui() {
		super.initGui();
		if (scroll == null) { scroll = addScroll(0).setSize(scrollWidth, 152); }
		int h = guiTop + imageHeight - 25;
		List<Object> options = new ArrayList<>();
		options.add("spawner.random");
		if (!select.getFormattedText().isEmpty() && selectDir != null) {
			String key = selectDir.getResourceDomain() + ":" + select.getString();
			if (eventsData.containsKey(key)) {
				for (ISoundEventAccessor<Sound> event : ((ISoundEventAccessorMixin) eventsData.get(key)).getAccessorList()) {
					if (event instanceof Sound) {
						options.add(Component.literal(((Sound) event).getSoundLocation().getResourcePath().replaceAll("/", ".")));
					}
					else if (event instanceof ISoundEventAccessorMixin) {
						for (int i = 0; i < event.getWeight(); i++) {
							Sound sound = (Sound) ((ISoundEventAccessorMixin) event).getAccessorList().get(i);
							options.add(Component.literal(sound.getSoundLocation().getResourcePath().replaceAll("/", ".")));
						}
					}
				}
			}
		}
		addButton(5, guiLeft + 199, h - 28, options.size() > 1, option, options.toArray(new Object[0]))
				.setSize(162, 14)
				.setIsEnabled(options.size() > 1)
				.setHoverTexts("selection.sound.hover.options");
		addButton(3, guiLeft + 199, h, "gui.play")
				.setSize(70, 20)
				.setIsEnabled(selectDir != null && resource != null && scroll.hasSelected())
				.hasSound = false;
		addButton(4, guiLeft + 127, h, "gui.copy")
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
				musicData = null;
				error = null;
				delay = 0L;
				wait = System.currentTimeMillis();
				if (event == null) {
					MusicController.Instance.playSound(SoundCategory.NEUTRAL, resource.toString(), player.posX, player.posY, player.posZ, 1.0F, 1.0F);
				}
				else if (event instanceof ISoundEventAccessorMixin) {
					int i = (int) (Math.random() * (double) event.getWeight());
					GuiButtonNop b = getButton(5);
					if (event.getWeight() > 0 && b != null && b.getValue() > 0) {
						for (int j = 0; j < event.getWeight(); j++) {
							Sound sound = (Sound) ((ISoundEventAccessorMixin) event).getAccessorList().get(j);
							if (b.getMessage().getString().equals(sound.getSoundLocation().getResourcePath().replaceAll("/", "."))) { i = j; }
						}
					}
					Sound sound = (Sound) ((ISoundEventAccessorMixin) event).getAccessorList().get(i);
					PositionedSoundRecord playingSong = new PositionedSoundRecord(event.getLocation(), SoundCategory.MUSIC, 1.0f, 1.0f, false, 0,
							ISound.AttenuationType.NONE, (float) player.posX, (float) player.posY, (float) player.posZ);
					((IPositionedSoundMixin) playingSong).setSound(sound);
					minecraft.getSoundHandler().playSound(playingSong);
					name.setMessage(Component.literal(resource.getResourcePath()));
					time.setMessage(Component.empty());
					CustomNPCsScheduler.runTack(() -> {
						long n = System.currentTimeMillis() + 2500L;
						while (n >= System.currentTimeMillis()) {
							for (MusicData md : ClientTickHandler.musics) {
								if (md.playing() && md.name.equals(resource.getResourcePath()) && md.resource.equals(sound.getSoundLocation())) {
									musicData = md;
									name.setMessage(Component.literal(musicData.name)
											.append(Component.literal(" / ").withStyle(TextFormatting.GRAY))
											.append(Component.literal(musicData.resource.getResourcePath().replaceAll("/", ".")).withStyle(TextFormatting.GRAY)));
									LogWriter.info("Sound found and is played: "+md.name+"; option: "+md.resource);
									return;
								}
							}
						}
						error = Component.literal(resource.getResourcePath()).append(" -").append(Component.translatable("quest.task.location.1"));
					}, 100);
				}
				break;
			}
			case 4: if (resource != null) { NoppesStringUtils.setClipboardContents(resource.toString()); } break;
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
		if (isPlay) {
			int alpha = 0x80;
			if (delay > 0L) {
				alpha = Math.min(128, Math.max(0, (int) ((double) (delay - System.currentTimeMillis()) * 0.064d)));
			}
			int left = guiLeft + 5;
			int right = guiLeft + imageWidth - 5;
			int top = guiTop + imageHeight - 37;
			int bottom = guiTop + imageHeight - 27;
			drawRect(left, top, right, bottom, alpha << 24);
			if (error != null) {
				if (delay == 0L) { delay = System.currentTimeMillis() + 4000L; }
				drawCenteredString(font, error.getFormattedText(), (right-left)/2, top + 1, CustomNpcs.MainColor.getRGB());
				if (delay < System.currentTimeMillis()) { isPlay = false; }
				return;
			}
			if (musicData != null) {
				double track = (double) musicData.getCurrentTime() / (double) musicData.duration;
				int len = left + (int) (track * ((double) imageWidth - 10.0d));
				time.setMessage(Component.literal(Util.instance.ticksToElapsedTime(musicData.getCurrentTime() / 50L, false, false, false))
						.append(Component.literal("/").withStyle(TextFormatting.GRAY))
						.append(Component.literal(Util.instance.ticksToElapsedTime(musicData.duration / 50L, false, false, false)).withStyle(TextFormatting.GRAY)));
				drawRect(left, top, len, bottom, 0xF000 | alpha << 24);
				if (musicData.stopped()) {
					if (delay == 0L) { delay = System.currentTimeMillis() + 2000L; }
					if (delay < System.currentTimeMillis()) { isPlay = false; }
				}
			} else {
				time.setMessage(Component.literal(Util.instance.ticksToElapsedTime((2500L - Math.min(2500L, Math.max(0L, System.currentTimeMillis() - wait))) / 50, false, false, false)).withStyle(TextFormatting.GRAY));
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
		handler = (ISoundRegistryMixin) ((ISoundHandlerMixin) minecraft.getSoundHandler()).getSoundRegistry();
		for (ResourceLocation location : handler.getSoundRegistry().keySet()) { addFile(location); }
	}

	@Override
	protected void addFile(ResourceLocation location) {
		SoundEventAccessor event = handler.getSoundRegistry().get(location);
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
		int size = 0;
		for (ISoundEventAccessor<Sound> accessor : ((ISoundEventAccessorMixin) event).getAccessorList()) {
			if (accessor instanceof Sound) { size++; }
			else { size += accessor.getWeight(); }
		}
		hovers.add(Component.translatable("gui.options").append(" (" + size + "):").withStyle(TextFormatting.GRAY));
		for (ISoundEventAccessor<Sound> accessor : ((ISoundEventAccessorMixin) event).getAccessorList()) {
			boolean bo = false;
			if (accessor instanceof Sound) {
				if (hovers.size() > 8) {
					hovers.add(Component.literal("..."));
					break;
				}
				hovers.add(Component.empty()
						.append(Component.literal(((Sound) accessor).getSoundLocation().getResourceDomain() + ":").withStyle(TextFormatting.DARK_GRAY))
						.append(Component.literal(((Sound) accessor).getSoundLocation().getResourcePath())));
			}
			else if (accessor instanceof ISoundEventAccessorMixin)  {
				for (int i = 0; i < accessor.getWeight(); i++) {
					if (hovers.size() > 8) {
						hovers.add(Component.literal("..."));
						bo = true;
						break;
					}
					Sound sound = (Sound) ((ISoundEventAccessorMixin) event).getAccessorList().get(i);
					hovers.add(Component.empty()
							.append(Component.literal(sound.getSoundLocation().getResourceDomain() + ":").withStyle(TextFormatting.DARK_GRAY))
							.append(Component.literal(sound.getSoundLocation().getResourcePath())));
				}
			}
			if (bo) { break; }
		}
		hoversData.put(location.toString(), hovers);
		eventsData.put(location.toString(), event);
		data.get(domain).put(location, 0L);
	}

}
