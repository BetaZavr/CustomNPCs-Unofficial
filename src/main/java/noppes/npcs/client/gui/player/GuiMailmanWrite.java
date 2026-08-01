package noppes.npcs.client.gui.player;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNextPage;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.*;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.ClientTickHandler;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.containers.ContainerMail;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class GuiMailmanWrite extends GuiContainerNPCInterface<ContainerMail>
		implements ITextfieldListener, ITextChangeListener, IGuiError, IGuiClose {

	protected static final ResourceLocation mEnvelope = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/envelope.png");
	protected static final ResourceLocation mList = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/list.png");
	protected static final ResourceLocation mTable = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/table.png");
	protected static final ResourceLocation mSendBox = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/send_box.png");
	protected static final ResourceLocation widgets = new ResourceLocation("textures/gui/widgets.png");
	public static final ResourceLocation icons = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/icons.png");
	public static PlayerMail mail = new PlayerMail();
	public static GuiScreen parent;

	protected GuiButtonNextPage buttonNextPage;
	protected GuiButtonNextPage buttonPreviousPage;
	protected NBTTagList bookPages;
	protected GuiLabel error;
	protected boolean hasStacks;
	protected boolean hasSend;
	protected int bookTotalPages;
	protected int currPage;
	protected int updateCount;
	protected int type;
	protected long totalCost;
	protected final Map<Integer, Long> cost = new TreeMap<>();
	protected String username;
	// Animations
	protected int step;
	protected int tick;
	protected int mTick;
	protected int aType;
	protected long errTick;
	protected final Random rnd = new Random();

	public GuiMailmanWrite(ContainerMail container) {
		super(null, container, Component.empty());
		drawDefaultBackground = false;
		closeOnEsc = true;
		xSize = 306;
		ySize = 248;

		bookTotalPages = 1;
		hasSend = false;
		username = "";
		type = 0;
		errTick = 0;
		if (mail.message.hasKey("pages")) { bookPages = mail.message.getTagList("pages", 8); }
		if (bookPages != null) {
			bookPages = bookPages.copy();
			bookTotalPages = bookPages.tagCount();
			if (bookTotalPages < 1) { bookTotalPages = 1; }
		} else {
			(bookPages = new NBTTagList()).appendTag(new NBTTagString(""));
			bookTotalPages = 1;
		}
		ClientTickHandler.checkMails = true;
		tick = 30;
		mTick = 30;
		step = 0;
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		if (!button.active) { return; }
		switch (button.id) {
			case 0: { // send
				mail.message.setTag("pages", bookPages);
				if (((ContainerMail) inventorySlots).canSend) {
					if (!hasSend) {
						hasSend = true;
						((ContainerMail) inventorySlots).sendMail = true;
						Packets.sendServer(new SPacketPlayerMailSend(username, totalCost, mail.save()));
					}
				} else {
					aType = 0;
					animClose();
				}
				break;
			}
			case 1: {
				if (currPage < bookTotalPages - 1) { ++currPage; }
				else if (((ContainerMail) inventorySlots).canEdit) {
					addNewPage();
					if (currPage < bookTotalPages - 1) { ++currPage; }
				}
				break;
			}
			case 2: {
				if (currPage > 0) { --currPage; }
				break;
			}
			case 3: {
				aType = 0;
				animClose();
				break;
			} // back/exit
			case 4: {
				ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
					if (agree) {
						Packets.sendServer(new SPacketPlayerMailDelete(0, mail.timeWhenReceived, mail.sender));
						CustomNpcs.proxy.getPlayerData(player).mailData.playerMails.remove(mail);
						aType = 2;
						animClose();
					}
					NoppesUtil.openGUI(player, this);
				},
						Component.empty().getParent(),
						Component.translatable("message.delete").getParent());
				setScreen(guiYesNo);
				break;
			} // delete
			case 6: {
				if (mail.ransom > 0) { Packets.sendServer(new SPacketPlayerMailRansom(mail.timeWhenReceived)); }
				else if (mail.money > 0) { Packets.sendServer(new SPacketPlayerMailTakeMoney(mail.timeWhenReceived)); }
				break;
			} // ransom
			case 7: {
				Packets.sendServer(new SPacketPlayerMailReturn(mail.timeWhenReceived));
				CustomNpcs.proxy.getPlayerData(player).mailData.playerMails.remove(mail);
				aType = 1;
				animClose();
				break;
			} // return letter
		}
		updateButtons();
	}

	@Override
	public void onClose() {
		Keyboard.enableRepeatEvents(false);
		GuiTextFieldNop.unfocus();
		ClientTickHandler.checkMails = true;
		mc.displayGuiScreen(parent);
		if (parent != null) { parent.initGui(); }
		parent = null;
		mail = new PlayerMail();
	}

	private void drawPlace(float u, float v, int mouseX, int mouseY) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(u, v, 0.0f);
		mc.getTextureManager().bindTexture(mTable);
		drawTexturedModalRect(0, -5, 0, 0, 174, 248);
		GlStateManager.popMatrix();
		if (step == 5) {
			// envelope
			GlStateManager.pushMatrix();
			GlStateManager.translate(guiLeft + 142.0f, guiTop - 10.0f, 0.0f);
			mc.getTextureManager().bindTexture(mEnvelope);
			drawTexturedModalRect(5, 40, 0, 0, 164, 137);
			GlStateManager.popMatrix();
			// list
			GlStateManager.pushMatrix();
			GlStateManager.translate(guiLeft + 9.0f, guiTop + 12.0f, 0.0f);
			mc.getTextureManager().bindTexture(mList);
			drawTexturedModalRect(0, 0, 0, 0, 164, 134);
			GlStateManager.popMatrix();
			// handle
			GlStateManager.pushMatrix();
			GlStateManager.translate(guiLeft + 50.0f, guiTop + 8.0f, 0.0f);
			mc.getTextureManager().bindTexture(mTable);
			drawTexturedModalRect(0, 0, 174, 0, 74, 17);
			GlStateManager.popMatrix();
			// box
			if (hasStacks || ((ContainerMail) inventorySlots).canEdit) {
				GlStateManager.pushMatrix();
				GlStateManager.translate(guiLeft + 180.0f, guiTop + 179.0f, 0.0f);
				mc.getTextureManager().bindTexture(mSendBox);
				drawTexturedModalRect(0, 0, 0, 0, 74, 54);
				GlStateManager.popMatrix();
			}
			// slots
			GlStateManager.pushMatrix();
			GlStateManager.translate(guiLeft + 6.0f, guiTop + 167.0f, 0.0f);
			mc.getTextureManager().bindTexture(GuiNPCInterface.RESOURCE_SLOT);
			for (int j = 0; j < 9; j++) {
				for (int k = 0; k < 4; k++) {
					drawTexturedModalRect(18 * j, k * 18 + (k == 3 ? 2 : 0), 0, 0, 18, 18);
				}
			}
			GlStateManager.popMatrix();
			for (int slotId = 0; slotId < inventorySlots.inventorySlots.size(); slotId++) {
				Slot slot = inventorySlots.getSlot(slotId);
                if (slotId < 4) {
					boolean show = !((ContainerMail) inventorySlots).canEdit && !((ContainerMail) inventorySlots).canSend ? mail.ransom <= 0 && slot.getHasStack() : ((ContainerMail) inventorySlots).canEdit;
					if (show && slot.xPos == 250000) {
						slot.xPos = 197 + (slotId % 2) * 23;
						slot.yPos = 187 + (slotId / 2) * 23;
					} else if (!show && slot.xPos != 250000) {
						slot.xPos = 250000;
						slot.yPos = 250000;
					}
				} else if (slot.xPos == 250000) { // Inventory
					slot.xPos = 7 + ((slotId - 4) % 9) * 18;
					slot.yPos = 168 + ((slotId - 4) / 9) * 18 + (slotId >= 31 ? 2 : 0);
				}
			}
			int x = guiLeft + 170;
			int y = guiTop + 48;
			// player name
			GuiLabel l = getLabel(0);
			GuiTextFieldNop tf;
			GuiButtonNop b;
			if (l != null && l.getX() == 250000) {
				l.setX(x);
				l.setY(y);
			}
			if (((ContainerMail) inventorySlots).canEdit) {
				tf = getTextField(!((ContainerMail) inventorySlots).canSend ? 2 : 0);
				if (tf != null && tf.getX() == 250000) {
					tf.setX(x);
					tf.setY(y += 10);
				}
			} else {
				l = getLabel(10);
				if (l != null && l.getX() == 250000) {
					l.setX(x + 2);
					l.setY(y += 10);
				}
			}
			// title
			l = getLabel(1);
			if (l != null && l.getX() == 250000) {
				l.setX(x);
				l.setY(y += 18);
			}
			if (((ContainerMail) inventorySlots).canEdit) {
				tf = getTextField(1);
				if (tf != null && tf.getX() == 250000) {
					tf.setX(x);
					tf.setY(y += 10);
				}
			} else {
				l = getLabel(11);
				if (l != null && l.getX() == 250000) {
					l.setX(x);
					l.setY(y += 10);
				}
			}
			// ransom
			if (getLabel(7) != null) { getLabel(7).setBackColor(0); }
			if (getLabel(8) != null) { getLabel(8).setBackColor(0); }
			if (getButton(6) != null) {
				getButton(6).layerColor = 0;
				if (!((ContainerMail) inventorySlots).canEdit && !((ContainerMail) inventorySlots).canSend) {
					if (mail.ransom > 0) {
						getButton(6).setIsEnabled(player.isCreative() || CustomNpcs.proxy.getPlayerData(player).game.getMoney() >= mail.ransom);
					}
					else { getButton(6).setIsEnabled(mail.money > 0); }
				}
			}
			if (!((ContainerMail) inventorySlots).canEdit) {
				if (mail.ransom > 0 || mail.money > 0) {
					l = getLabel(7);
					if (l != null && l.getX() == 250000) {
						l.setX(x);
						l.setY(y + 18);
					}
					l = getLabel(8);
					if (l != null && l.getX() == 250000) {
						l.setX(x + 2);
						l.setY(y + 28);
					}
				}
			}
			l = error;
			if (l != null && l.getX() == 250000) {
				l.setX(x);
				l.setY(guiTop + 145);
			}
			// Moneys
			if (((ContainerMail) inventorySlots).canEdit) {
				l = getLabel(3);
				if (l != null && l.getX() == 250000) {
					l.setX(x);
					l.setY((y += 19) + 4);
				}
				l = getLabel(6);
				if (l != null && l.getX() == 250000) {
					l.setY(x + 102);
					l.setY(y + 4);
				}
				GuiTextFieldNop tf3 = getTextField(3);
				GuiTextFieldNop tf4 = getTextField(4);
				if (tf3 != null) {
					if (tf3.getX() == 250000) {
						tf3.setX(x + 48);
						tf3.setY(y);
					}
					int tfV = tf3.isEmpty() ? (int) tf3.def : tf3.isInteger() ? tf3.getInteger() : 0;
					if (tfV > 0) {
						if (tf4 != null) { tf4.setIsVisible(false); }
						if (getLabel(7) != null) { getLabel(7).setIsEnabled(false); }
						if (getLabel(8) != null) { getLabel(8).setIsEnabled(false); }
					} else {
						if (tf4 != null) { tf4.setIsVisible(true); }
						if (getLabel(7) != null) { getLabel(7).setIsEnabled(true); }
						if (getLabel(8) != null) { getLabel(8).setIsEnabled(true); }
					}
				}
				l = getLabel(7);
				if (l != null && l.getX() == 250000) {
					l.setX(x);
					l.setY((y += 19) + 4);
				}
				l = getLabel(8);
				if (l != null && l.getX() == 250000) {
					l.setX(x + 102);
					l.setY(y + 4);
				}
				if (tf4 != null) {
					if (tf4.getX() == 250000) {
						tf4.setX(x + 48);
						tf4.setY(y);
					}
					int tfV = tf4.isEmpty() ? (int) tf4.def : tf4.isInteger() ? tf4.getInteger() : 0;
					if (tfV > 0) {
						if (tf3 != null) { tf3.setIsVisible(false); }
						if (getLabel(3) != null) { getLabel(3).setIsEnabled(false); }
						if (getLabel(6) != null) { getLabel(6).setIsEnabled(false); }
					} else {
						if (tf3 != null) { tf3.setIsVisible(true); }
						if (getLabel(3) != null) { getLabel(3).setIsEnabled(true); }
						if (getLabel(6) != null) { getLabel(6).setIsEnabled(true); }
					}
				}
			}
			x = guiLeft + 7;
			y = guiTop + 149;
			if (((ContainerMail) inventorySlots).canEdit && !((ContainerMail) inventorySlots).canSend) {
				b = getButton(0);
				if (b != null && b.getX() == 250000) {
					b.setX(x + 52);
					b.setY(y);
				}
			} // dialog/quest add to
			else if (((ContainerMail) inventorySlots).canEdit) {
				b = getButton(0);
				if (b != null && b.getX() == 250000) {
					b.setX(x + 52);
					b.setY(y);
				}
			} // write
			if (!((ContainerMail) inventorySlots).canEdit && !((ContainerMail) inventorySlots).canSend) {
				if (mail.ransom > 0 || mail.money > 0) {
					b = getButton(6);
					if (b != null && b.getX() == 250000) {
						b.setX(x + 220);
						b.setY(y - 42);
					}
				}
				b = getButton(4);
				if (b != null && b.getX() == 250000) {
					b.setX(x);
					b.setY(y);
				}
				b = getButton(7);
				if (b != null && b.getX() == 250000) {
					b.setX(x + 52);
					b.setY(y);
				}
			} // read -> delete
			if (!((ContainerMail) inventorySlots).canEdit || ((ContainerMail) inventorySlots).canSend) {
				b = getButton(3);
				if (b != null && b.getX() == 250000) {
					b.setX(x + 104);
					b.setY(y);
				}
			} // write -> cancel
			if (buttonNextPage != null && buttonNextPage.getX() == 250000 && (((ContainerMail) inventorySlots).canEdit || mail.ransom == 0)) {
				buttonNextPage.setX(x + 135);
				buttonNextPage.setY(y - 16);
			}
			if (buttonPreviousPage != null && buttonPreviousPage.getX() == 250000) {
				buttonPreviousPage.setX(x);
				buttonPreviousPage.setY(y - 16);
			}
			// Text
			String s = (currPage + 1) + "/" + bookTotalPages;
			String totalText = "", drawEnd = "";
			if (bookPages != null && currPage >= 0 && currPage < bookPages.tagCount()) { totalText = bookPages.getStringTagAt(currPage); }
			if (((ContainerMail) inventorySlots).canEdit) {
				if (mc.fontRenderer.getWordWrappedHeight(totalText + TextFormatting.BLACK + "_", 152) > 108) {
					if (updateCount / 6 % 2 == 0) { drawEnd = TextFormatting.BLACK + "_"; }
					else { drawEnd = TextFormatting.GRAY + "_"; }
				}
				else if (mc.fontRenderer.getBidiFlag()) { totalText += "_"; }
				else if (updateCount / 6 % 2 == 0) { totalText = totalText + TextFormatting.BLACK + "_"; }
				else { totalText = totalText + TextFormatting.GRAY + "_"; }
			} else if (mail.ransom > 0) {
				StringBuilder newText = new StringBuilder();
				char g = ((char) 167);
				boolean rnd = false;
				for (int i = 0; i < totalText.length(); i++) {
					char c = totalText.charAt(i);
					if (mc.fontRenderer.getWordWrappedHeight(newText + "" + c, 152) > 9) {
						newText.append(g).append("k");
						rnd = true;
					}
					if (!rnd || c == ((char) 10) || c == ' ') { newText.append(c); }
					else { newText.append(' '); }
				}
				totalText = newText.toString();
			}
			int ls = mc.fontRenderer.getStringWidth(s);
			mc.fontRenderer.drawString(s, guiLeft + 163 - ls, guiTop + 18, 0, false);
			if (!drawEnd.isEmpty()) { mc.fontRenderer.drawString(drawEnd, guiLeft + 159, guiTop + 127, 0, false); }
			mc.fontRenderer.drawSplitString(totalText, guiLeft + 12, guiTop + 28, 152, 0);
			if (!((ContainerMail) inventorySlots).canEdit && !((ContainerMail) inventorySlots).canSend && mail.ransom > 0) {
				int borderC = new Color(0x40000000).getRGB();
				drawGradientRect(guiLeft + 11, guiTop + 36, guiLeft + 164, guiTop + 144, borderC, borderC);
				if (isMouseHover(mouseX, mouseY, guiLeft + 11, guiTop + 36, 152, 108)) {
					setHoverText("mailbox.hover.ransom.sell");
					getLabel(7).setBackColor(new Color(0x80FF0000).getRGB());
					getLabel(8).setBackColor(new Color(0x80FF0000).getRGB());
					getButton(6).layerColor = new Color(0xFFF00000).getRGB();
				}
			}
			// add slots
			for (int slotId = 0; slotId < 4; slotId++) {
				Slot slot = inventorySlots.getSlot(slotId);
                boolean show = !((ContainerMail) inventorySlots).canEdit && !((ContainerMail) inventorySlots).canSend ? slot.getHasStack() : slot.xPos != 250000;
				if (show) {
					int px = guiLeft + 193 + 23 * (slotId % 2);
					int py = guiTop + 183 + 23 * (slotId / 2);
					GlStateManager.pushMatrix();
					GlStateManager.translate(px, py, 0.0f);
					drawGradientRect(3, 3, 21, 21,
							new Color(0xC0101010).getRGB(),
							new Color(0xD0101010).getRGB());
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
					mc.getTextureManager().bindTexture(widgets);
					drawTexturedModalRect(0, 0, 0, 22, 24, 24);
					GlStateManager.popMatrix();
					if (!((ContainerMail) inventorySlots).canEdit && !((ContainerMail) inventorySlots).canSend && mail.ransom > 0) {
						GlStateManager.pushMatrix();
						GlStateManager.translate(px + 4, py + 4, 0.0f);
						mc.getRenderItem().renderItemAndEffectIntoGUI(slot.getStack(), 0, 0);
						GlStateManager.translate(0.0f, 0.0f, 200.0f);
						int count = slot.getStack().getCount();
						drawString(mc.fontRenderer, "" + count, 17 - mc.fontRenderer.getStringWidth("" + count), 9, new Color(0xFFFFFFFF).getRGB());
						if (isMouseHover(mouseX, mouseY, px, py, 18, 18)) {
							List<Component> list = new ArrayList<>();
							list.add(Component.translatable("mailbox.hover.ransom.sell"));
							for (String line : slot.getStack().getTooltip(mc.player,
									mc.gameSettings.advancedItemTooltips ? TooltipFlags.ADVANCED
											: TooltipFlags.NORMAL)) {
								list.add(Component.literal(line));
							}
							setHoverText(list);
							getLabel(7).setBackColor(new Color(0x80FF0000).getRGB());
							getLabel(8).setBackColor(new Color(0x80FF0000).getRGB());
							getButton(6).layerColor = new Color(0xFFF00000).getRGB();
						}
						mc.getTextureManager().bindTexture(icons);
						drawTexturedModalRect(-2, -2, 0, 32, 20, 20);
						GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
						GlStateManager.popMatrix();
					}
				}
			}
		}
		else {
			for (int slotId = 0; slotId < inventorySlots.inventorySlots.size(); slotId++) {
				Slot slot = inventorySlots.getSlot(slotId);
				if (slot.xPos == 250000) { continue; }
				slot.xPos = 250000;
				slot.yPos = 250000;
			}
			if (buttonNextPage != null && buttonNextPage.getX() != 250000) {
				buttonNextPage.setX(250000);
				buttonNextPage.setY(250000);
			}
			if (buttonPreviousPage != null && buttonPreviousPage.getX() != 250000) {
				buttonPreviousPage.setX(250000);
				buttonPreviousPage.setY(250000);
			}
			for (int i = 0; i < 12; i++) {
				GuiLabel l = getLabel(i);
				if (l != null && l.getX() != 250000) {
					l.setX(250000);
					l.setY(250000);
				}
				GuiButtonNop b = getButton(i);
				if (b != null && b.getX() != 250000) {
					b.setX(250000);
					b.setY(250000);
				}
				GuiTextFieldNop tf = getTextField(i);
				if (tf != null && tf.getX() != 250000) {
					tf.setX(250000);
					tf.setY(250000);
				}
			}
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawWorldBackground(0);
		if (errTick != 0L && errTick < mc.world.getTotalWorldTime()) {
			errTick = 0L;
			if (error != null) { error.setMessage(""); }
		}
		// Info
		if (!((ContainerMail) inventorySlots).canEdit && mail.ransom > 0) {
			mc.getTextureManager().bindTexture(icons);
			drawTexturedModalRect(guiLeft + 33, guiTop + 43, 0, 126, 120, 130);
			mc.fontRenderer.drawString(
					new TextComponentTranslation("mailbox.hover.ransom.sell").getFormattedText(), guiLeft + 36,
					guiTop + 100, 0xFFFFFFFF);
		}
		boolean hasMail = false;
		cost.clear();
		long c = CustomNpcs.MailCostSendingLetter[0];
		cost.put(0, c);
		totalCost = c;
		if (bookPages != null) {
			c = 0;
			for (int i = 0; i < bookPages.tagCount(); ++i) {
				if (bookPages.getStringTagAt(i).isEmpty()) { continue; }
				c += CustomNpcs.MailCostSendingLetter[1];
				hasMail = true;
			}
			cost.put(1, c);
			totalCost += c;
		}
		c = 0;
		hasStacks = false;
		for (int i = 0; i < 4; i++) {
			Slot slot = inventorySlots.getSlot(i);
			if (slot.getStack().isEmpty()) { continue; }
			c += (int) ((float) CustomNpcs.MailCostSendingLetter[2] * (float) slot.getStack().getCount()
					/ (float) slot.getStack().getMaxStackSize());
			hasStacks = true;
			hasMail = true;
		}
		cost.put(3, c);
		totalCost += c;
		if (mail.money > 0) {
			c = (long) ((float) mail.money * (float) CustomNpcs.MailCostSendingLetter[3] / 100.0f);
			cost.put(2, c);
			totalCost += c;
			totalCost += mail.money;
			hasMail = true;
		}
		if (mail.ransom > 0) {
			c = (long) (int) ((float) mail.ransom * (float) CustomNpcs.MailCostSendingLetter[4]
					/ 100.0f);
			cost.put(4, c);
			totalCost += c;
		}
		if (getLabel(5) != null) {
			getLabel(5).setMessage(Component.translatable("mailbox.cost.send",
							"" + (totalCost == 0L ? 0
									: Util.instance.getTextReducedNumber(totalCost, true, false, false)),
							CustomNpcs.displayCurrencies));
		}
		if (((ContainerMail) inventorySlots).canEdit && ((ContainerMail) inventorySlots).canSend && getButton(0) != null) {
			type = 0;
			type = !player.isCreative() && username.equals(player.getName()) && !CustomNpcs.MailSendToYourself ? 3 : 0;
			if (type == 0) { type = getTextField(0) != null && getTextField(0).getValue().isEmpty() ? 1 : 0; } // player
			if (type == 0) { type = mail.title.isEmpty() ? 4 : 0; } // title
			if (type == 0 && !player.isCreative()) { type = CustomNpcs.proxy.getPlayerData(player).game.getMoney() < totalCost ? 2 : 0; } // money
			if (type == 0 && !hasMail) { type = 5; } // empty
			getButton(0).setIsEnabled(type == 0);
		}
		// Animations
		GlStateManager.pushMatrix();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		if (tick >= 0) {
			if (tick == 0) { partialTicks = 0.0f; }
			float part = (float) tick + partialTicks;
			float cos = ValueUtil.correctFloat((float) Math.cos(90.0d * part / (double) mTick * Math.PI / 180.0d), 0.0f, 1.0f);
			switch (step) {
				case 0: {
					float u = guiLeft + (1.0f - cos) * 174.0f;
					float v = guiTop + (1.0f - cos) * 248.0f;
					drawPlace(u, v, mouseX, mouseY);
					GlStateManager.pushMatrix();
					GlStateManager.translate(u, v, 2.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					drawTexturedModalRect(5, 40, 0, 0, 164, 137);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(53, 168, 0, 54, 68, 74);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 1;
						tick = 21;
						mTick = 20;
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.down",
								player.posX, player.posY, player.posZ, 1.0f,
								0.75f + 0.25f * rnd.nextFloat());
						GlStateManager.disableBlend();
					}
					break;
				} // open
				case 1: {
					drawPlace(guiLeft, guiTop, mouseX, mouseY);
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft, guiTop + cos * 150.0f, 2.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					drawTexturedModalRect(5, 40, 0, 0, 164, 137);
					GlStateManager.popMatrix();
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + cos * 183.0f, guiTop + cos * 169.0f, 2.0f);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(0, 0, 0, 54, 68, 74);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 2;
						tick = 21;
						mTick = 20;
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.sheet",
								player.posX, player.posY, player.posZ, 1.0f,
								0.8f + 0.4f * rnd.nextFloat());
						GlStateManager.disableBlend();
					}
					break;
				} // box and envelope_0 offset
				case 2: {
					drawPlace(guiLeft, guiTop, mouseX, mouseY);
					// envelope_0
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 5, guiTop + 190.0f, 2.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					drawTexturedModalRect(0, 0, 0, 0, 164, 137);
					GlStateManager.popMatrix();
					// list
					float v = guiTop + 150.0f - cos * 134.0f;
					int h = 186 - (int) v;
					if (h > 134) {
						h = 134;
					}
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 9, v, 2.0f);
					mc.getTextureManager().bindTexture(mList);
					drawTexturedModalRect(0, 0, 0, 0, 156, h);
					GlStateManager.popMatrix();
					// handle
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 50.0f, guiTop + 8.0f, 2.0f);
					mc.getTextureManager().bindTexture(mTable);
					drawTexturedModalRect(0, 0, 174, 0, 74, 17);
					GlStateManager.popMatrix();
					// box
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 183.0f, guiTop + 169.0f, 2.0f);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(0, 0, 0, 54, 68, 74);
					mc.getTextureManager().bindTexture(GuiNPCInterface.RESOURCE_SLOT);
					drawTexturedModalRect(25, 27, 0, 0, 18, 18);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(0, 41, 0, 95, 68, 33);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 3;
						tick = 16;
						mTick = 15;
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.sheet",
								player.posX, player.posY, player.posZ, 1.0f,
								0.8f + 0.4f * rnd.nextFloat());
						GlStateManager.disableBlend();
					}
					break;
				} // box added slots and show list
				case 3: {
					drawPlace(guiLeft, guiTop, mouseX, mouseY);
					// envelope_1
					float u = guiLeft + cos * 142.0f;
					float v = guiTop + 150.0f - cos * 160.0f;
					GlStateManager.pushMatrix();
					GlStateManager.translate(u, v, 2.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					drawTexturedModalRect(5, 40, 0, 0, 164, 137);
					GlStateManager.popMatrix();
					// list
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 9.0f, guiTop + 12.0f, 2.0f);
					mc.getTextureManager().bindTexture(mList);
					drawTexturedModalRect(0, 0, 0, 0, 156, 134);
					GlStateManager.popMatrix();
					// handle
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 50.0f, guiTop + 8.0f, 2.0f);
					mc.getTextureManager().bindTexture(mTable);
					drawTexturedModalRect(0, 0, 174, 0, 74, 17);
					GlStateManager.popMatrix();
					// box
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 183.0f, guiTop + 169.0f, 2.0f);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(0, 0, 0, 54, 68, 74);
					mc.getTextureManager().bindTexture(GuiNPCInterface.RESOURCE_SLOT);
					drawTexturedModalRect(25, 27, 0, 0, 18, 18);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(0, 41, 0, 95, 68, 33);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 4;
						tick = 36;
						mTick = 35;
						GlStateManager.disableBlend();
					}
					break;
				} // envelope_1 offset
				case 4: {
					drawPlace(guiLeft, guiTop, mouseX, mouseY);
					// envelope
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 142.0f, guiTop - 10.0f, 2.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					drawTexturedModalRect(5, 40, 0, 0, 164, 137);
					GlStateManager.popMatrix();
					// list
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 9.0f, guiTop + 12.0f, 2.0f);
					mc.getTextureManager().bindTexture(mList);
					drawTexturedModalRect(0, 0, 0, 0, 156, 134);
					GlStateManager.popMatrix();
					// handle
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 50.0f, guiTop + 8.0f, 2.0f);
					mc.getTextureManager().bindTexture(mTable);
					drawTexturedModalRect(0, 0, 174, 0, 74, 17);
					GlStateManager.popMatrix();
					// box
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 183.0f, guiTop + 169.0f, 2.0f);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(0, 0, 0, 54, 68, 74);
					GlStateManager.popMatrix();
					// slots
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 208.0f, guiTop + 196.0f, 2.0f);
					mc.getTextureManager().bindTexture(GuiNPCInterface.RESOURCE_SLOT);
					drawTexturedModalRect(0, 0, 0, 0, 18, 18);
					double h = 36.0d;
					int x0 = 0, y0 = 0;
					for (int s = 0; s < 36; s++) {
						int t = tick + s - 35;
						int x = s % 9;
						int y = (int) Math.floor((double) s / 9.0d);
						int x1 = x * 18 - 202;
						int y1 = y * 18 + (y == 3 ? 2 : 0) - 29;
						if (t < 0) {
							drawTexturedModalRect(x1, y1, 0, 0, 18, 18);
						} else {
							cos = (float) Math.cos(90.0d * ((double) t + partialTicks) / 35.0d * Math.PI / 180.0d);
							if (cos < 0.0f) {
								cos = 0.0f;
							} else if (cos > 1.0f) {
								cos = 1.0f;
							}
							double px = x0 - x1, py = y0 - y1;
							double dist = Math.sqrt(px * px + py * py);
							double r = (Math.pow(dist / 2.0d, 2.0d) + Math.pow(h, 2.0d)) / (2.0d * h);
							double tg = Math.abs(py / px);
							double angle = Math.atan(tg) * 180.0d / Math.PI;
							double cx = (x0 - x1) / 2.0d + x1;
							double cy = (y0 - y1) / 2.0d + y1;
							double rx = cx + Math.sin(angle * Math.PI / 180) * (r - h) * -1;
							double ry = cy + Math.cos(angle * Math.PI / 180) * (r - h);
							px = x0 - rx;
							py = y0 - ry;
							tg = Math.abs(py / px);
							double startAngle = Math.atan(tg) * 180.0d / Math.PI;
							px = x1 - rx;
							py = y1 - ry;
							tg = Math.abs(py / px);
							angle = 180.0d - startAngle - Math.atan(tg) * 180.0d / Math.PI;
							double nowAngle = cos * angle + startAngle;
							x1 = (int) (rx + Math.cos(nowAngle * Math.PI / 180) * r);
							y1 = (int) (ry + Math.sin(nowAngle * Math.PI / 180) * r * -1);
							drawTexturedModalRect(x1, y1, 0, 0, 18, 18);
						}
					}
					GlStateManager.popMatrix();
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 183.0f, guiTop + 169.0f, 2.0f);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(0, 41, 0, 95, 68, 33);
					GlStateManager.popMatrix();
					if (tick % 4 == 0) {
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":mail.slot",
								player.posX, player.posY, player.posZ, 1.0f,
								0.7f + 0.4f * rnd.nextFloat());
					}
					if (tick == 0) {
						step = 5;
						mTick = 0;
						GlStateManager.disableBlend();
					}
					break;
				} // slots
				case 6: {
					drawPlace(guiLeft, guiTop, mouseX, mouseY);
					if (tick == 0) {
						step = 7;
						tick = 36;
						mTick = 36;
					}
				} // simple close/back
				case 7: {
					// table
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft, guiTop, 0.0f);
					mc.getTextureManager().bindTexture(mTable);
					drawTexturedModalRect(0, -5, 0, 0, 174, 248);
					GlStateManager.popMatrix();
					// envelope
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 142.0f, guiTop - 10.0f, 2.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					drawTexturedModalRect(5, 40, 0, 0, 164, 137);
					GlStateManager.popMatrix();
					// list
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 9.0f, guiTop + 12.0f, 2.0f);
					mc.getTextureManager().bindTexture(mList);
					drawTexturedModalRect(0, 0, 0, 0, 156, 134);
					GlStateManager.popMatrix();
					// handle
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 50.0f, guiTop + 8.0f, 2.0f);
					mc.getTextureManager().bindTexture(mTable);
					drawTexturedModalRect(0, 0, 174, 0, 74, 17);
					GlStateManager.popMatrix();
					// box
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 183.0f, guiTop + 169.0f, 2.0f);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(0, 0, 0, 54, 68, 74);
					GlStateManager.popMatrix();
					// slots
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 208.0f, guiTop + 196.0f, 2.0f);
					mc.getTextureManager().bindTexture(GuiNPCInterface.RESOURCE_SLOT);
					drawTexturedModalRect(0, 0, 0, 0, 18, 18);
					int x0 = 0, y0 = 0;
					for (int s = 0; s < 36; s++) {
						int x = s % 9;
						int y = (int) Math.floor((double) s / 9.0d);
						int x1 = x * 18 - 202;
						int y1 = y * 18 + (y == 3 ? 2 : 0) - 29;
						double px = x0 - x1, py = y0 - y1;
						drawTexturedModalRect((int) (x1 + px * cos), (int) (y1 + py * cos), 0, 0, 18, 18);
					}
					GlStateManager.popMatrix();
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 183.0f, guiTop + 169.0f, 2.0f);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(0, 41, 0, 95, 68, 33);
					GlStateManager.popMatrix();
					if (tick % 6 == 0) {
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":mail.slot",
								player.posX, player.posY, player.posZ, 1.0f,
								0.9f + 0.3f * rnd.nextFloat());
					}
					if (tick == 0) {
						step = aType == 0 ? 8 : aType == 1 ? 10 : 13;
						tick = 31;
						mTick = 30;
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.sheet",
								player.posX, player.posY, player.posZ, 1.0f,
								0.8f + 0.4f * rnd.nextFloat());
					}
					break;
				} // 0 _ simple close/back
				case 8: {
					// table
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft, guiTop, 0.0f);
					mc.getTextureManager().bindTexture(mTable);
					drawTexturedModalRect(0, -5, 0, 0, 174, 248);
					GlStateManager.popMatrix();
					float u = guiLeft + 9.0f;
					float v = guiTop + 12.0f + cos * 53.0f;
					// list
					GlStateManager.pushMatrix();
					GlStateManager.translate(u, v, 2.0f);
					mc.getTextureManager().bindTexture(mList);
					drawTexturedModalRect(0, 0, 0, 0, 156, 134);
					GlStateManager.popMatrix();
					// handle
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 50.0f, guiTop + 8.0f, 2.0f);
					mc.getTextureManager().bindTexture(mTable);
					drawTexturedModalRect(0, 0, 174, 0, 74, 17);
					GlStateManager.popMatrix();
					u = guiLeft + 142.0f - cos * 142.0f;
					v = guiTop - 10.0f + cos * 34.0f;
					// envelope
					GlStateManager.pushMatrix();
					GlStateManager.translate(u, v, 2.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					drawTexturedModalRect(5, 40, 0, 0, 164, 137);
					GlStateManager.popMatrix();
					// box
					u = guiLeft + 183.0f - cos * 130.0f;
					v = guiTop + 169.0f - cos * 75.0f;
					GlStateManager.pushMatrix();
					GlStateManager.translate(u, v, 2.0f);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(0, 0, 0, 54, 68, 74);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 9;
						tick = 31;
						mTick = 30;
					}
					break;
				} // 1 _ simple close/back
				case 9: {
					// table
					float u = guiLeft + cos * 174.0f;
					float v = guiTop + cos * 248.0f;
					GlStateManager.pushMatrix();
					GlStateManager.translate(u, v, 2.0f);
					mc.getTextureManager().bindTexture(mTable);
					drawTexturedModalRect(0, -5, 0, 0, 174, 248);
					GlStateManager.popMatrix();
					// envelope
					GlStateManager.pushMatrix();
					GlStateManager.translate(u, v + 24.0f, 2.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					drawTexturedModalRect(5, 40, 0, 0, 164, 137);
					GlStateManager.popMatrix();
					// box
					GlStateManager.pushMatrix();
					GlStateManager.translate(u + 53.0f, v + 94.0f, 2.0f);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(0, 0, 0, 54, 68, 74);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 0;
						mTick = 0;
						onClose();
					}
					break;
				} // 2 _ simple close/back
				case 10: {
					// box
					if (hasStacks) {
						GlStateManager.pushMatrix();
						GlStateManager.translate(guiLeft + 183.0f, guiTop + 169.0f, 0.0f);
						mc.getTextureManager().bindTexture(mSendBox);
						drawTexturedModalRect(0, 0, 74, 0, 74, 54);
						GlStateManager.popMatrix();
					} else {
						GlStateManager.pushMatrix();
						GlStateManager.translate(guiLeft + 183.0f, guiTop + 169.0f, 0.0f);
						mc.getTextureManager().bindTexture(mSendBox);
						drawTexturedModalRect(0, 0, 0, 54, 68, 74);
						GlStateManager.popMatrix();
					}
					// table
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft, guiTop, 0.0f);
					mc.getTextureManager().bindTexture(mTable);
					drawTexturedModalRect(0, -5, 0, 0, 174, 248);
					GlStateManager.popMatrix();
					// list
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 9.0f, guiTop + 12.0f, 0.0f);
					mc.getTextureManager().bindTexture(mList);
					drawTexturedModalRect(0, 0, 0, 0, 156, 134);
					GlStateManager.popMatrix();
					// envelope
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 142.0f - cos * 142.0f, guiTop - 10.0f + cos * 160.0f,
							1.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					drawTexturedModalRect(5, 40, 0, 0, 164, 137);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 11;
						tick = 16;
						mTick = 15;
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.sheet",
								player.posX, player.posY, player.posZ, 1.0f,
								0.8f + 0.4f * rnd.nextFloat());
					}
					break;
				} // 1 _ send
				case 11: {
					// table
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft, guiTop, 0.0f);
					mc.getTextureManager().bindTexture(mTable);
					drawTexturedModalRect(0, -5, 0, 0, 174, 248);
					GlStateManager.popMatrix();
					// envelope
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft, guiTop + 150.0f, 0.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					drawTexturedModalRect(5, 40, 0, 0, 164, 137);
					GlStateManager.popMatrix();
					// list
					float v = guiTop + 150.0f - (1.0f - cos) * 134.0f;
					int h = 186 - (int) v;
					if (h > 134) {
						h = 134;
					}
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 9, v, 2.0f);
					mc.getTextureManager().bindTexture(mList);
					drawTexturedModalRect(0, 0, 0, 0, 156, h);
					GlStateManager.popMatrix();
					if (hasStacks) {
						GlStateManager.pushMatrix();
						GlStateManager.translate(guiLeft + 183.0f, guiTop + 169.0f, 0.0f);
						mc.getTextureManager().bindTexture(mSendBox);
						drawTexturedModalRect(0, 0, 74, 0, 74, 54);
						GlStateManager.popMatrix();
					}
					if (tick == 0) {
						step = 12;
						tick = 31;
						mTick = 30;
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.sheet",
								player.posX, player.posY, player.posZ, 1.0f,
								0.8f + 0.4f * rnd.nextFloat());
					}
					break;
				} // 2 _ send
				case 12: {
					// table
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + cos * 174.0f, guiTop + cos * 248.0f, 0.0f);
					mc.getTextureManager().bindTexture(mTable);
					drawTexturedModalRect(0, -5, 0, 0, 174, 248);
					GlStateManager.popMatrix();
					// envelope
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + cos * 300.0f, guiTop + 150.0f - cos * 248.0f, 1.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					drawTexturedModalRect(5, 40, 0, 0, 164, 137);
					GlStateManager.popMatrix();
					if (hasStacks) {
						GlStateManager.pushMatrix();
						GlStateManager.translate(guiLeft + 183.0f + cos * 300.0f, guiTop + 169.0f - cos * 248.0f,
								0.0f);
						mc.getTextureManager().bindTexture(mSendBox);
						drawTexturedModalRect(0, 0, 74, 0, 74, 54);
						GlStateManager.popMatrix();
					}
					if (tick == 0) {
						step = 0;
						mTick = 0;
						onClose();
					}
					break;
				} // 3 _ send
				case 13: {
					// table
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + cos * 174.0f, guiTop + cos * 248.0f, 0.0f);
					mc.getTextureManager().bindTexture(mTable);
					drawTexturedModalRect(0, -5, 0, 0, 174, 248);
					GlStateManager.popMatrix();
					// box
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 183.0f - cos * 41.0f, guiTop + 169.0f - cos * 74.0f, 1.0f);
					mc.getTextureManager().bindTexture(mSendBox);
					drawTexturedModalRect(0, 0, 0, 54, 68, 74);
					GlStateManager.popMatrix();
					// envelope
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 147.0f - cos * 54.0f, guiTop + 30.0f + cos * 33.0f, 2.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					drawTexturedModalRect(0, 0, 0, 0, 164, 137);
					GlStateManager.popMatrix();
					// list
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 9.0f + cos * 88.0f, guiTop + 12.0f + cos * 52.0f, 2.0f);
					mc.getTextureManager().bindTexture(mList);
					drawTexturedModalRect(0, 0, 0, 0, 156, 134);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 14;
						tick = 16;
						mTick = 15;
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":mail.delete",
								player.posX, player.posY, player.posZ, 1.0f,
								0.8f + 0.4f * rnd.nextFloat());
					}
					break;
				} // 1 _ delete
				case 14: {
					GlStateManager.enableAlpha();
					GlStateManager.enableBlend();
					// envelope
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 93.0f, guiTop + 63.0f, 1.0f);
					mc.getTextureManager().bindTexture(mEnvelope);
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f - cos);
					drawTexturedModalRect(0, 0, 0, 0, 164, 137);
					GlStateManager.popMatrix();
					// list
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 97.0f, guiTop + 64.0f, 1.0f);
					mc.getTextureManager().bindTexture(mList);
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f - cos);
					drawTexturedModalRect(0, 0, 0, 0, 156, 134);
					GlStateManager.popMatrix();
					// list
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 97.0f + cos * 28.0f, guiTop + 64.0f + cos * 19.5f, 2.0f);
					GlStateManager.scale(1.31092f - 0.47059f * cos, 1.09836f - 0.31967f * cos, 1.0f);
					mc.getTextureManager().bindTexture(mList);
					GlStateManager.color(1.0f, 1.0f, 1.0f, cos);
					drawTexturedModalRect(0, 0, 0, 134, 119, 122);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 15;
						tick = 31;
						mTick = 30;
					}
					GlStateManager.disableAlpha();
					GlStateManager.disableBlend();
					break;
				} // 2 _ delete
				case 15: {
					GlStateManager.enableAlpha();
					GlStateManager.enableBlend();
					// list
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 97.0f + 28.0f, guiTop + 64.0f + 19.5f, 1.0f);
					GlStateManager.scale(1.31092f - 0.47059f, 1.09836f - 0.31967f, 1.0f);
					mc.getTextureManager().bindTexture(mList);
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f - cos);
					drawTexturedModalRect(0, 0, 0, 134, 119, 122);
					GlStateManager.popMatrix();

					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 130.0f + cos * 7.5f, guiTop + 88.0f + cos * 7.125f, 2.0f);
					GlStateManager.scale(0.85f - 0.15f * cos, 0.85f - 0.15f * cos, 1.0f);
					mc.getTextureManager().bindTexture(mList);
					GlStateManager.color(1.0f, 1.0f, 1.0f, cos);
					drawTexturedModalRect(0, 0, 156, 0, 100, 95);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 16;
						tick = 31;
						mTick = 30;
					}
					GlStateManager.disableAlpha();
					GlStateManager.disableBlend();
					break;
				} // 3 _ delete
				case 16: {
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 137.5f - cos * 300.0f, guiTop + 95.125f - cos * 150.0f, 2.0f);
					GlStateManager.scale(0.6f, 0.6f, 1.0f);
					mc.getTextureManager().bindTexture(mList);
					drawTexturedModalRect(0, 0, 156, 0, 100, 95);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 0;
						mTick = 0;
						onClose();
					}
					break;
				} // 4 _ delete
			}
			tick--;
		}
		else { drawPlace(guiLeft, guiTop, mouseX, mouseY); }
		GlStateManager.popMatrix();
		if (step != 5) { return; }
		if (getButton(0) != null) {
			if (!((ContainerMail) inventorySlots).canSend) { getButton(0).setHoverTexts("mailbox.hover.done"); } // done
			else {
				if (type == 0) {
					Component mes = Component.translatable("mailbox.hover.send.0",
							Util.instance.ticksToElapsedTime(CustomNpcs.MailTimeWhenLettersWillBeReceived[1] * 20L, false, true, true),
							"" + totalCost,
							CustomNpcs.displayCurrencies);
					for (int i : cost.keySet()) {
						if (cost.get(i) > 0L) {
							String p0 = "" + cost.get(i);
							String p1 = "", p2 = "";
							switch (i) {
								case 1:
									p1 = "" + CustomNpcs.MailCostSendingLetter[1];
									break;
								case 2:
									p0 = "" + mail.money;
									p1 = "" + cost.get(i);
									p2 = "" + CustomNpcs.MailCostSendingLetter[3];
									break;
								case 3:
									p1 = "" + CustomNpcs.MailCostSendingLetter[2];
									break;
								case 4:
									p1 = "" + CustomNpcs.MailCostSendingLetter[4];
									break;
							}
							mes.append(Component.translatable("mailbox.hover.send.2." + i,
									p0, CustomNpcs.displayCurrencies, p1, CustomNpcs.displayCurrencies, p2));
						}
					}
					getButton(0).setHoverTexts(mes);
				}
				else if (type == 2) {
					Component mes = Component.translatable("mailbox.hover.send.2",
							"" + totalCost, CustomNpcs.displayCurrencies);
					for (int i : cost.keySet()) {
						if (cost.get(i) > 0L) {
							String p0 = "" + cost.get(i);
							String p1 = "", p2 = "";
							switch (i) {
								case 1:
									p1 = "" + CustomNpcs.MailCostSendingLetter[1];
									break;
								case 2:
									p0 = "" + mail.money;
									p1 = "" + cost.get(i);
									p2 = "" + CustomNpcs.MailCostSendingLetter[3];
									break;
								case 3:
									p1 = "" + CustomNpcs.MailCostSendingLetter[2];
									break;
								case 4:
									p1 = "" + CustomNpcs.MailCostSendingLetter[4];
									break;
								default:
									break;
							}
							mes.append(Component.translatable("mailbox.hover.send.2." + i, p0,
									CustomNpcs.displayCurrencies, p1, CustomNpcs.displayCurrencies, p2));
						}
					}
					getButton(0).setHoverTexts(mes);
				}
				else {
					getButton(0).setHoverTexts(Component.translatable("mailbox.hover.send." + type));
				}
			}
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
		// Player Money
		if (mc != null && ((ContainerMail) inventorySlots).canSend) {
			String text = Util.instance.getTextReducedNumber(CustomNpcs.proxy.getPlayerData(player).game.getMoney(), true, true,
					false) + CustomNpcs.displayCurrencies;
			int x = guiLeft + 166;
			int y = guiTop + 150;
			GlStateManager.pushMatrix();
			GlStateManager.color(2.0f, 2.0f, 2.0f, 1.0f);
			GlStateManager.translate(x, y, 0.0f);
			mc.getTextureManager().bindTexture(GuiBasic.MONEY);
			float sc = 16.0f / 250.f;
			GlStateManager.scale(sc, sc, sc);
			drawTexturedModalRect(0, 0, 0, 0, 256, 256);
			GlStateManager.popMatrix();

			GlStateManager.pushMatrix();
			mc.fontRenderer.drawString(text, x + 15, y + 8.0f / 2.0f, new Color(0x404040).getRGB(), false);
			GlStateManager.popMatrix();
		}
	}

	private String getText() {
		return bookPages != null && currPage >= 0 && currPage < bookPages.tagCount() ? bookPages.getStringTagAt(currPage) : "";
	}

	@Override
	public void initGui() {
		super.initGui();
		Keyboard.enableRepeatEvents(true);
		int x = guiLeft + 170;
		int y = guiTop + 48;
		// Text area

		// player name
		addLabel(0, x, y, "mailbox." + (!((ContainerMail) inventorySlots).canEdit || !((ContainerMail) inventorySlots).canSend ? "sender" : "username"));
		if (((ContainerMail) inventorySlots).canEdit) {
			if (!((ContainerMail) inventorySlots).canSend) {
				addTextField(2, x, y += 10, 112, 14, mail.sender)
					.setHoverTexts("mailbox.hover.to");
			} else {
				addTextField(0, x, y += 10, 112, 14, username)
						.setHoverTexts("mailbox.hover.to");
			}
		} else {
			addLabel(10, x + 2, y += 10, "\"" + mail.sender + "\"");
		}
		// title
		addLabel(1, x, y += 18, "mailbox.subject");
		if (((ContainerMail) inventorySlots).canEdit) {
			addTextField(1, x, y += 10, 112, 14, mail.title)
					.setHoverTexts("mailbox.hover.title");
		} else {
			addLabel(11, x, y += 10, "\"" + mail.title + "\"");
		}
		// ransom
		if (!((ContainerMail) inventorySlots).canEdit) {
			if (mail.ransom > 0) {
				addLabel(7, x, y + 18, Component.translatable("mailbox.ransom").withStyle(TextFormatting.DARK_RED, TextFormatting.BOLD));
				addLabel(8,x + 2, y + 28,
						Util.instance.getTextReducedNumber(mail.ransom, true, false, false)
								+ " " + CustomNpcs.displayCurrencies);
			}
			else if (mail.money > 0) {
				addLabel(7, x, y + 18, "market.currency");
				addLabel(8, x + 2, y + 28,
						Util.instance.getTextReducedNumber(mail.money, true, false, false)
								+ " " + CustomNpcs.displayCurrencies);
			}
		}
		error = addLabel(2, x - 10, guiTop + 145, "")
				.setColor(new Color(0xFFFF0000).getRGB());
		// Moneys
		if (((ContainerMail) inventorySlots).canEdit) {
			addLabel(3, x, (y += 19) + 4, "market.currency");
			addLabel(6, x + 102, y + 4, CustomNpcs.displayCurrencies);
			addTextField(3, x + 48, y, 50, 16, "" + mail.money)
					.setMinMaxDefault(0,
							(int) (player.isCreative() ? Integer.MAX_VALUE : CustomNpcs.proxy.getPlayerData(player).game.getMoney()),
							mail.money)
					.setHoverTexts("mailbox.hover.money");
			addLabel(7, x, (y += 19) + 4, "mailbox.ransom");
			addLabel(8, x + 102, y + 4, CustomNpcs.displayCurrencies);
			addTextField(4, x + 48, y, 50, 16, "" + mail.ransom)
					.setMinMaxDefault(0, Integer.MAX_VALUE, mail.ransom)
					.setHoverTexts("mailbox.hover.ransom");
		}

		x = guiLeft + 7;
		y = guiTop + 149;
		if (((ContainerMail) inventorySlots).canEdit && !((ContainerMail) inventorySlots).canSend) { // dialog/quest add to
			addButton(0, x + 52, y, "gui.done")
					.setSize(50, 14)
					.setTexture(icons)
					.setUV(0, 176, 0, 0);
		}
		else if (((ContainerMail) inventorySlots).canEdit) { // write
			addButton(0, x + 52, y, "mailbox.send")
					.setSize(50, 14)
					.setTexture(icons)
					.setUV(0, 176, 0, 0);
		}
		if (!((ContainerMail) inventorySlots).canEdit && !((ContainerMail) inventorySlots).canSend) { // read -> delete
			if (mail.ransom > 0) {
				addButton(6, x + 220, y - 42, "gui.pay")
						.setSize(50, 14)
						.setTexture(icons)
						.setUV(0, 176, 0, 0);
			}
			else if (mail.money > 0) {
				addButton(6, x + 220, y - 42, "gui.take")
						.setSize(50, 14)
						.setTexture(icons)
						.setUV(0, 176, 0, 0)
						.setHoverTexts("display.hover.X");
			}
			addButton(4, x, y, "gui.remove")
					.setSize(50, 14)
					.setTexture(icons)
					.setUV(0, 176, 0, 0);
			if (!mail.isReturned()) {
				addButton(7, x + 52, y, "mailbox.back")
						.setSize(50, 14)
						.setTexture(icons)
						.setUV(0, 176, 0, 0);
			}
		}
		if (!((ContainerMail) inventorySlots).canEdit || ((ContainerMail) inventorySlots).canSend) { // write -> cancel
			addButton(3, x + 104, y, !((ContainerMail) inventorySlots).canEdit ? "gui.back" : "gui.cancel")
					.setSize(50, 14)
					.setTexture(icons)
					.setUV(0, 176, 0, 0)
					.setHoverTexts("display.hover.X");
		}
		add(buttonNextPage = new GuiButtonNextPage(this, 1, x + 135, y - 16, true, null));
		add(buttonPreviousPage = new GuiButtonNextPage(this, 2, x, y - 16, false, null));
		updateButtons();
	}

	@Override
	public boolean keyPressed(char typedChar, int keyCode) {
		if (!hasSubGui()) {
			if (keyCode == Keyboard.KEY_ESCAPE) {
				aType = 0;
				animClose();
				return true;
			}
			if (((ContainerMail) inventorySlots).canEdit && keyTypedInBook(typedChar, keyCode)) { return true; }
		}
		return super.keyPressed(typedChar, keyCode);
	}

	private boolean keyTypedInBook(char typedChar, int keyCode) {
		if (step != 5 || GuiTextFieldNop.getActive() != null) { return false; }
        if (typedChar == '\u0016') {
            addString(GuiScreen.getClipboardString());
			return true;
        }
		if (GuiScreen.isKeyComboCtrlC(keyCode)) {
			GuiScreen.setClipboardString(getText());
			return true;
		}
		else if (GuiScreen.isKeyComboCtrlV(keyCode)) {
			if (((ContainerMail) inventorySlots).canEdit) {
				setText("");
				addString(GuiScreen.getClipboardString());
			}
			return true;
		}
		else if (GuiScreen.isKeyComboCtrlX(keyCode)) {
			GuiScreen.setClipboardString(getText());
			if (((ContainerMail) inventorySlots).canEdit) { setText(""); }
			return true;
		}
		else {
			switch (keyCode) {
				case Keyboard.KEY_RETURN:
				case Keyboard.KEY_NUMPADENTER: {
					addString("\n");
					return true;
				} // enter
				case Keyboard.KEY_BACK: {
					if (((ContainerMail) inventorySlots).canEdit) {
						String s = getText();
						if (!s.isEmpty()) { setText(s.substring(0, s.length() - 1)); }
					}
					return true;
				} // backspace
				case Keyboard.KEY_PRIOR: {
					buttonPreviousPage.onPress();
					return true;
				} // page up
				case Keyboard.KEY_NEXT: {
					buttonNextPage.onPress();
					return true;
				} // page down
				default: {
					if (((ContainerMail) inventorySlots).canEdit && ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
						addString(Character.toString(typedChar));
						return true;
					}
				}
			}
			return false;
		}
    }

    @Override
	public void setClose(NBTTagCompound data) {
		player.sendMessage(Component.translatable("mailbox.success", data.getString("username")).getParent());
		aType = 1;
		animClose();
	}

	@Override
	public void setError(int id, NBTTagCompound data) {
		hasSend = false;
		if (error == null) { return; }
		switch (id) {
			case 0: error.setMessage("mailbox.error.username"); break;
			case 1: error.setMessage("mailbox.error.subject"); break;
			case 2: error.setMessage("mailbox.error.yourself"); break;
			case 3: error.setMessage("mailbox.error.nomoney"); break;
		}
		errTick = mc.world.getTotalWorldTime() + 200L;
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 0: username = textField.getValue(); break;
			case 1: mail.title = textField.getValue(); break;
			case 2: mail.sender = textField.getValue(); break;
			case 3: {
				mail.money = textField.getInteger();
				textField.setValue("" + mail.money);
				textField.setMinMaxDefault(textField.min, textField.max, mail.money);
				break;
			}
			case 4: {
				mail.ransom = textField.getInteger();
				textField.setValue("" + mail.ransom);
				textField.setMinMaxDefault(textField.min, textField.max, mail.ransom);
				break;
			}
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		++updateCount;
		if (getLabel(4) != null) { getLabel(4).setIsEnabled(false); }
		if (((ContainerMail) inventorySlots).canEdit) {
			if (getLabel(4) != null) { getLabel(4).setIsEnabled(true); }
		} else {
			if (!((ContainerMail) inventorySlots).canSend && mail.money > 0) {
				if (getLabel(4) != null) { getLabel(4).setIsEnabled(true); }
			}
		}
	}

	@Override
	public void textUpdate(IComponentGui component, String text) { }

	private void addNewPage() {
		if (bookPages != null && bookPages.tagCount() < 50) {
			bookPages.appendTag(new NBTTagString(""));
			++bookTotalPages;
		}
	}

	private void addString(String str) {
		String text = getText();
		String totalText = text + str;
		int textHeight = mc.fontRenderer.getWordWrappedHeight(totalText, 152);
		if (totalText.length() < 1024 && textHeight <= 108) {
			setText(totalText);
			textUpdate(null, totalText);
		}
	}

	private void animClose() {
		if (step != 5) { return; }
		step = 6;
		tick = 5;
		mTick = 5;
	}

	private void setText(String str) {
		if (bookPages != null && currPage >= 0 && currPage < bookPages.tagCount()) {
			bookPages.set(currPage, new NBTTagString(str));
		}
	}

	private void updateButtons() {
		if (!((ContainerMail) inventorySlots).canEdit && mail.ransom > 0) {
			buttonNextPage.setIsVisible(false);
			buttonPreviousPage.setIsVisible(false);
			return;
		}
		buttonNextPage.setIsVisible(currPage < bookTotalPages - 1 || ((ContainerMail) inventorySlots).canEdit);
		buttonPreviousPage.setIsVisible(currPage > 0);
	}

}
