package noppes.npcs.client.gui.player;

import java.util.*;
import java.util.List;

import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.ClientTickHandler;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.controllers.data.PlayerCompassData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketPlayerMailDelete;
import noppes.npcs.packets.server.SPacketPlayerMailGet;
import noppes.npcs.packets.server.SPacketPlayerMailOpen;
import noppes.npcs.packets.server.SPacketPlayerMailRead;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.util.ResourceData;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.util.Util;

public class GuiMailbox extends GuiNPCInterface
		implements IGuiData, ICustomScrollListener, GuiYesNoCallback {

	protected static final ResourceLocation mBox = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/box_empty.png");
	protected static final ResourceLocation mDoor = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/box_door.png");
	protected static final ResourceLocation mList = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/box_list.png");
	public static final ResourceLocation icons = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/icons.png");

	protected final Map<Component, PlayerMail> scrollData = new HashMap<>();
	protected GuiCustomScrollNop scroll;
	protected PlayerMail selected;

	// Animations
	protected int closeType;
	protected int step;
	protected int tick;
	protected int milliTick;
	protected final Random rnd = new Random();
	protected PlayerCompassData compassData;

	public GuiMailbox() {
		super();
		imageWidth = 192;
		imageHeight = 236;

		ClientTickHandler.checkMails = true;
		// Animations
		setNextTick(30);
		step = 0;
		closeType = 0;
		Packets.sendServer(new SPacketPlayerMailGet());
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		GuiMailmanWrite.parent = this;
		switch (button.id) {
			case 0: {
				if (selected == null) { return; }
				GuiMailmanWrite.mail = selected;
				step = 4;
				setNextTick(15);
				closeType = 2;
				break;
			} // select
			case 1: {
				step = 4;
				setNextTick(15);
				closeType = 1;
				break;
			} // close 1
			case 2: {
				if (selected == null) { return; }
				ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
					if (agree) {
						Packets.sendServer(new SPacketPlayerMailDelete(0, selected.timeWhenReceived, selected.sender));
						selected = null;
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":mail.delete",
								player.posX, player.posY, player.posZ, 1.0f,
								0.9f + 0.2f * rnd.nextFloat());
					}
					NoppesUtil.openGUI(player, this);
				},
						scroll.getNormalSelected().getParent(),
						Component.translatable("message.delete").getParent());
				setScreen(guiYesNo);
				break;
			} // delete specific
			case 3: {
				if (PlayerData.get(player).mailData.playerMails.isEmpty()) { return; }
				ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
					if (agree && selected != null) {
						Packets.sendServer(new SPacketPlayerMailDelete(1, selected.timeWhenReceived, selected.sender));
						selected = null;
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":mail.delete",
								player.posX, player.posY, player.posZ, 1.0f,
								0.9f + 0.2f * rnd.nextFloat());
					}
					NoppesUtil.openGUI(player, this);
				},
						Component.translatable("mailbox.name").append(":").getParent(),
						Component.translatable("message.delete").getParent());
				setScreen(guiYesNo);
				break;
			} // delete all only read letters
			case 4: {
				if (PlayerData.get(player).mailData.playerMails.isEmpty()) { return; }
				ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
					if (agree && selected != null) {
						Packets.sendServer(new SPacketPlayerMailDelete(2, selected.timeWhenReceived, selected.sender));
						selected = null;
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":mail.delete",
								player.posX, player.posY, player.posZ, 1.0f,
								0.9f + 0.2f * rnd.nextFloat());
					}
					NoppesUtil.openGUI(player, this);
				},
						Component.translatable("mailbox.name").append(":").getParent(),
						Component.translatable("message.delete").getParent());
				setScreen(guiYesNo);
				break;
			} // delete all letters
			case 5: {
				step = 4;
				setNextTick(15);
				closeType = 0;
				break;
			} // close 0
		}
	}

	private void drawMailBox(float u, float v) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(u, v, 0.0f);
		mc.getTextureManager().bindTexture(mBox);
		drawTexturedModalRect(0, 0, 0, 0, 192, 236); // Box
		if (!scrollData.isEmpty()) {
			mc.getTextureManager().bindTexture(mList);
			drawTexturedModalRect(8, 45, 0, 0, 176, 156); // list
		}
		if (step == 3) {
			mc.getTextureManager().bindTexture(mDoor);
			drawTexturedModalRect(-5, 44, 181, 0, 7, 158); // door
		}
		GlStateManager.popMatrix();
		if (scroll != null) { scroll.setPos((int) u + 9, (int) v + 45); }
		if (getLabel(0) != null && getLabel(0).enabled) {
			GuiLabel l = getLabel(0);
			l.setX((int) u + 95 - (l.getWidth() / 2));
			l.setY((int) v + 11);
		}
		for (int i = 0; i < 6; i++) {
			if (getButton(i) == null) { return; }
			GuiButtonNop b = getButton(i);
			b.setIsEnabled(step == 3);
			switch (i) {
				case 0: {
					b.setX((int) u + 8);
					b.setY((int) v + 202);
					b.setIsEnabled(step == 3 && selected != null);
					break;
				} // read
				case 1: {
					b.setX((int) u + 67);
					b.setY((int) v + 202);
					b.setIsEnabled(step == 3);
					break;
				} // write
				case 2: {
					b.setX((int) u + 126);
					b.setY((int) v + 202);
					b.setIsEnabled(step == 3 && selected != null);
					break;
				} // remove
				case 3: {
					b.setX((int) u + 8);
					b.setY((int) v + 218);
					b.setIsEnabled(step == 3 && scroll != null && !scroll.getList().isEmpty());
					break;
				} // remove all
				case 4: {
					b.setX((int) u + 67);
					b.setY((int) v + 218);
					b.setIsEnabled(step == 3 && scroll != null && !scroll.getList().isEmpty());
					break;
				} // clear
				case 5: {
					b.setX((int) u + 126);
					b.setY((int) v + 218);
					b.setIsEnabled(step == 3);
					break;
				} // exit
			}
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		// Animations
		GlStateManager.pushMatrix();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		if (tick >= 0) {
			if (tick == 0) { partialTicks = 0.0f; }
			float part = (float) tick + partialTicks;
			float cos = (float) Math.cos(90.0d * part / (double) milliTick * Math.PI / 180.0d);
			if (cos < 0.0f) { cos = 0.0f; }
			else if (cos > 1.0f) { cos = 1.0f; }
			switch (step) {
				case 0: {
					if (tick == milliTick) {
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":mail.movement",
								player.posX, player.posY, player.posZ, 1.0f,
								0.75f + 0.25f * rnd.nextFloat());
					}
					drawMailBox(guiLeft, guiTop + (1.0f - cos) * 236.0f);
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft, guiTop + (1.0f - cos) * 236.0f, 2.0f);
					mc.getTextureManager().bindTexture(mDoor);
					drawTexturedModalRect(8, 44, 0, 0, 178, 158);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 1;
						setNextTick(20);
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":mail.open.door",
								player.posX, player.posY, player.posZ, 1.0f,
								0.75f + 0.25f * rnd.nextFloat());
						GlStateManager.disableBlend();
					}
					break;
				} // box appears
				case 1: {
					drawMailBox(guiLeft, guiTop);
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft - (cos * 193.0f), guiTop, 2.0f);
					mc.getTextureManager().bindTexture(mDoor);
					drawTexturedModalRect(8, 44, 0, 0, 178, 158);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 2;
						setNextTick(15);
						GlStateManager.disableBlend();
					}
					break;
				} // opening the door
				case 2: {
					drawMailBox(guiLeft, guiTop);
					float s = 1.0f - cos;
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft - 7.0f - (186.0f) * s, guiTop, 2.0f);
					mc.getTextureManager().bindTexture(mDoor);
					GlStateManager.scale(s, 1.0f, 1.0f);
					drawTexturedModalRect(8, 44, 0, 0, 178, 158);
					drawTexturedModalRect(183, 44, 178, 0, 3, 158);
					GlStateManager.popMatrix();
					s = cos;
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft - 7.0f, guiTop, 2.0f);
					mc.getTextureManager().bindTexture(mDoor);
					GlStateManager.scale(s, 1.0f, 1.0f);
					drawTexturedModalRect(0, 44, 181, 0, 7, 158);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 3;
						milliTick = 0;
						GlStateManager.disableBlend();
					}
					break;
				} // turning the door
				case 4: {
					if (tick == milliTick) {
						MusicController.Instance.playSound(SoundCategory.PLAYERS,
								CustomNpcs.MODID + ":mail.close.door",
								player.posX, player.posY, player.posZ, 1.0f, 0.75f + 0.25f * rnd.nextFloat());
					}
					drawMailBox(guiLeft, guiTop);
					float s = cos;
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft - 7.0f - (186.0f) * s, guiTop, 2.0f);
					mc.getTextureManager().bindTexture(mDoor);
					GlStateManager.scale(s, 1.0f, 1.0f);
					drawTexturedModalRect(8, 44, 0, 0, 178, 158);
					drawTexturedModalRect(183, 44, 178, 0, 3, 158);
					GlStateManager.popMatrix();
					s = 1.0f - cos;
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft - 7.0f, guiTop, 2.0f);
					mc.getTextureManager().bindTexture(mDoor);
					GlStateManager.scale(s, 1.0f, 1.0f);
					drawTexturedModalRect(0, 44, 181, 0, 7, 158);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 5;
						setNextTick(20);
						GlStateManager.disableBlend();
					}
					break;
				} // back turning the door
				case 5: {
					drawMailBox(guiLeft, guiTop);
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft - ((1.0f - cos) * 193.0f), guiTop, 2.0f);
					mc.getTextureManager().bindTexture(mDoor);
					drawTexturedModalRect(8, 44, 0, 0, 178, 158);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 6;
						setNextTick(30);
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":mail.movement",
								player.posX, player.posY, player.posZ, 1.0f,
								0.75f + 0.25f * rnd.nextFloat());
						GlStateManager.disableBlend();
					}
					break;
				} // close the door
				case 6: {
					drawMailBox(guiLeft, guiTop + cos * 236.0f);
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft, guiTop + cos * 236.0f, 2.0f);
					mc.getTextureManager().bindTexture(mDoor);
					drawTexturedModalRect(8, 44, 0, 0, 178, 158);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 0;
						setNextTick(30);
						if (closeType == 1) {
							Packets.sendServer(new SPacketPlayerMailOpen(true, true,0L, ""));
						} else if (closeType == 2 && selected != null) {
							if (!selected.beenRead) {
								selected.beenRead = true;
								PlayerMail mail = PlayerData.get(player).mailData.get(selected);
								if (mail != null) {
									mail.beenRead = true;
									ClientTickHandler.checkMails = true;
								}
								Packets.sendServer(new SPacketPlayerMailRead(selected.timeWhenReceived, selected.sender));
							}
							Packets.sendServer(new SPacketPlayerMailOpen(false, false, selected.timeWhenReceived, selected.sender));
							selected = null;
							scroll.setSelected(-1);
						}
						GlStateManager.disableBlend();
						GlStateManager.popMatrix();
						onClose();
						return;
					}
					break;
				} // box hidden
			}
			tick--;
		}
		else { drawMailBox(guiLeft, guiTop); }
		GlStateManager.popMatrix();
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (step != 3 || hasSubGui() || !CustomNpcs.ShowDescriptions) { return; }
		List<Component> hover = new ArrayList<>();
		if (scroll != null && scroll.hasSelected()) {
			PlayerMail mail = scrollData.get(scroll.getNormalSelected());
			hover.add(Component.empty()
					.append(Component.translatable("mailbox.sender").withStyle(TextFormatting.GRAY))
					.append(Component.literal(" \"").withStyle(TextFormatting.GRAY))
					.append(Component.literal(mail.sender).withStyle(TextFormatting.RESET))
					.append(Component.literal("\"").withStyle(TextFormatting.GRAY))
			);
			long timeWhenReceived = System.currentTimeMillis() - mail.timeWhenReceived - mail.timeWillCome;
			if (CustomNpcs.MailTimeWhenLettersWillBeDeleted > 0) {
				long timeToRemove = CustomNpcs.MailTimeWhenLettersWillBeDeleted * 86400000L - timeWhenReceived;
				if (timeToRemove < 0L) {
					Packets.sendServer(new SPacketPlayerMailDelete(0, mail.timeWhenReceived, mail.sender));
                    return;
				}
				hover.add(Component.translatable("mailbox.when.removed",
								Util.instance.ticksToElapsedTime(timeToRemove / 50, false, true, false))
						.withStyle(TextFormatting.GRAY));
			}
			if (mail.beenRead) {
				hover.add(Component.translatable("mailbox.when.read").withStyle(TextFormatting.GREEN));
			} else {
				hover.add(Component.translatable("mailbox.when.received",
								Util.instance.ticksToElapsedTime(timeWhenReceived / 50, false, true, false))
						.withStyle(TextFormatting.GRAY));
			}
		}
		if (!hover.isEmpty()) {
			setHoverText(hover);
			drawHoverText(null);
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		ClientTickHandler.checkMails = true;
		if (scroll == null) { scroll = addScroll(0).setSize(165, 154); }
		String select = scroll.getSelected();
		scrollData.clear();
		List<PlayerMail> listR = new ArrayList<>();
		List<PlayerMail> listN = new ArrayList<>();
		long time = System.currentTimeMillis();
		for (PlayerMail mail : PlayerData.get(player).mailData.playerMails) {
			if (time - mail.timeWhenReceived < mail.timeWillCome) { continue; }
			if (mail.beenRead) { listR.add(mail); }
			else { listN.add(mail); }
		}
		listR.sort((o1, o2) -> {
			if (o1.timeWhenReceived == o2.timeWhenReceived) { return 0; }
			else { return (o1.timeWhenReceived > o2.timeWhenReceived) ? -1 : 1; }
		});
		List<Component> list = new ArrayList<>();
		List<ResourceData> prefixes = new ArrayList<>();
		int i = 1;
		for (PlayerMail mail : listN) {
			Component key = Component.empty()
					.append(Component.literal(i + ": ").withStyle(TextFormatting.DARK_GRAY))
					.append(Component.literal("\"").withStyle(TextFormatting.RESET))
					.append(Component.translatable(mail.title))
					.append(Component.literal("\""));
			list.add(key);
			scrollData.put(key, mail);
			ResourceData rd = new ResourceData(icons, mail.getRansom() > 0 ? 96 : mail.returned ? 128 : 0, 0, 32, 32);
			rd.tH = -3.0f;
			prefixes.add(rd);
			i++;
		}
		for (PlayerMail mail : listR) {
			Component key = Component.empty()
					.append(Component.literal(i + ": ").withStyle(TextFormatting.DARK_GRAY))
					.append(Component.literal("\"").withStyle(TextFormatting.RESET))
					.append(Component.translatable(mail.title))
					.append(Component.literal("\""));
			list.add(key);
			scrollData.put(key, mail);
			boolean isEmpty = true;
			for (ItemStack stack : mail.items) {
				if (!stack.isEmpty()) {
					isEmpty = false;
					break;
				}
			}
			ResourceData rd = new ResourceData(icons, mail.getRansom() > 0 ? 96 : isEmpty ? 64 : 32, 0, 32, 32);
			rd.tH = -3.0f;
			prefixes.add(rd);
			i++;
		}
		scroll.clear();
		scroll.setUnsortedList(list).setPrefixes(prefixes);
		scroll.colorBackS = 0x00000000;
		scroll.colorBackE = 0x00000000;
		if (!select.isEmpty()) { scroll.setSelected(select); }
		add(scroll.setPos(guiLeft + 9, guiTop + 45));
		Component title = Component.translatable("mailbox.name");
		int x = (imageWidth - font.getStringWidth(title.getFormattedText())) / 2;
		addLabel(0, guiLeft + x, guiTop + 11, title)
				.setColor(CustomNpcs.MainColor.getRGB());
		x = guiLeft + 8;
		int y = guiTop + 202;
		addButton(0, x, y, "mailbox.read")
				.setSize(58, 14)
				.setTexture(icons)
				.setUV(0, 96, 0, 0)
				.setIsEnabled(selected != null)
				.setHoverTexts("mailbox.hover.read");
		addButton(1, x + 59, y, "mailbox.write")
				.setSize(58, 14)
				.setTexture(icons)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("mailbox.hover.write");
		addButton(2, x + 118, y, "gui.remove")
				.setSize(58, 14)
				.setTexture(icons)
				.setUV(0, 96, 0, 0)
				.setIsEnabled(selected != null)
				.setHoverTexts("mailbox.hover.del");
		addButton(3, x, y += 16, "gui.remove.all")
				.setSize(58, 14)
				.setTexture(icons)
				.setUV(0, 96, 0, 0)
				.setIsEnabled(!list.isEmpty())
				.setHoverTexts("mailbox.hover.delall");
		addButton(4, x + 59, y, "gui.clear")
				.setSize(58, 14)
				.setTexture(icons)
				.setUV(0, 96, 0, 0)
				.setIsEnabled(!list.isEmpty())
				.setHoverTexts("mailbox.hover.clear");
		addButton(5, x + 118, y, "display.hover.X")
				.setSize(58, 14)
				.setTexture(icons)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("hover.exit");
	}

	@Override
	public boolean keyPressed(char typedChar, int keyCode) {
		if (!hasSubGui() && step == 3 && (isEscKey(keyCode) || isInventoryKey(keyCode))) {
			step = 4;
			setNextTick(15);
			closeType = 0;
			return true;
		}
		return super.keyPressed(typedChar, keyCode);
	}

    @Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		selected = scrollData.get(scroll.getNormalSelected());
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (selected == null) { return; }
		GuiMailmanWrite.parent = this;
		GuiMailmanWrite.mail = selected;
		step = 4;
		setNextTick(15);
		closeType = 2;
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		PlayerData.get(player).mailData.load(compound);
		selected = null;
		initGui();
	}

	private void setNextTick(int time) {
		if (compassData == null) { compassData = PlayerData.get(player).compass; }
		tick = (int) (time / (compassData.questLogIsFast ? 3.0f : 1.0f));
		milliTick = tick;
	}

}
