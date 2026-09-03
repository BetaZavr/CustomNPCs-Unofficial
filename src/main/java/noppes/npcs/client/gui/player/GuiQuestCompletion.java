package noppes.npcs.client.gui.player;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.TextBlockClient;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketQuestCompletionCheck;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

import javax.annotation.Nonnull;

public class GuiQuestCompletion extends GuiNPCInterface {

	protected static final ResourceLocation resource = getResource("smallbg.png");
	protected final Quest quest;

	// New from Unofficial (BetaZavr)
	protected static final ResourceLocation bookGuiTextures = new ResourceLocation("textures/gui/book.png");
	protected TextBlockClient textBlockClient;
	protected int maxLine;
	protected int currentPage = 0;
	protected int hover;

	public GuiQuestCompletion(@Nonnull Quest questIn) {
		super();
		imageWidth = 176;
		imageHeight = 222;
		drawDefaultBackground = false;

		quest = questIn;
	}

	@Override
	public void initGui() {
		super.initGui();
		addLabel(0, guiLeft + 4, guiTop + 4, Component.translatable("questlog.completed")
				.append(" ")
				.append(Component.translatable(quest.getName())))
				.setSize(imageWidth - 8, 10);
		textBlockClient = new TextBlockClient(Component.translatable(quest.getCompleteText()).getFormattedText(), 170, true, npc, player);
		maxLine = 180 / fontRenderer.FONT_HEIGHT;
		addButton(0, guiLeft + 28 + (textBlockClient.lines.size() > maxLine ? 0 : 20), guiTop + imageHeight - 20, "quest.complete")
				.setSize(80, 16);
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		if (button.id == 0) {
			Packets.sendServer(new SPacketQuestCompletionCheck(quest.getId(), ItemStack.EMPTY));
			onClose();
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		minecraft.getTextureManager().bindTexture(resource);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, imageWidth, imageHeight);
		drawHorizontalLine(guiLeft + 4, guiLeft + 170, guiTop + 13, CustomNpcResourceListener.DefaultTextColor | 0xFF000000);
		drawQuestText();
		hover = -1;
		if (textBlockClient.lines.size() * fontRenderer.FONT_HEIGHT > maxLine) {
			String page = (currentPage + 1) + "/" + ((int) Math.ceil((double) textBlockClient.lines.size() / (double) maxLine));
			fontRenderer.drawString(page, guiLeft + 150 - fontRenderer.getStringWidth(page), guiTop + imageHeight - 20, CustomNpcResourceListener.DefaultTextColor);
			if (currentPage > 0) {
				GlStateManager.pushMatrix();
				GlStateManager.translate(guiLeft + 6, guiTop + imageHeight - 20, 0.0f);
				if (isMouseHover(mouseX, mouseY, guiLeft + 6, guiTop + imageHeight - 20, 18, 10)) { hover = 0; }
				minecraft.getTextureManager().bindTexture(GuiQuestCompletion.bookGuiTextures);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				drawTexturedModalRect(0, 0, hover == 0 ? 26 : 3, 207, 18, 10);
				GlStateManager.popMatrix();
			}
			if ((currentPage + 1) * maxLine < textBlockClient.lines.size()) {
				GlStateManager.pushMatrix();
				GlStateManager.translate(guiLeft + imageWidth - 24, guiTop + imageHeight - 20, 0.0f);
				if (isMouseHover(mouseX, mouseY, guiLeft + imageWidth - 24, guiTop + imageHeight - 20, 18, 10)) { hover = 1; }
				minecraft.getTextureManager().bindTexture(GuiQuestCompletion.bookGuiTextures);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				drawTexturedModalRect(0, 0, hover == 1 ? 26 : 3, 194, 18, 10);
				GlStateManager.popMatrix();
			}
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	// New from Unofficial (BetaZavr)
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (!hasSubGui() && hover != -1) {
			if (hover == 1) { currentPage++; }
			else { currentPage--; }
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	private void drawQuestText() {
		for (int i = currentPage * maxLine, j = 0; j < maxLine && i < textBlockClient.lines.size(); ++i, ++j) {
			fontRenderer.drawString(textBlockClient.lines.get(i).getFormattedText(),
					guiLeft + 4, guiTop + 16 + j * fontRenderer.FONT_HEIGHT, CustomNpcResourceListener.DefaultTextColor, false);
		}
	}

}
