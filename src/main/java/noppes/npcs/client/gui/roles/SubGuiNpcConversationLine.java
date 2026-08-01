package noppes.npcs.client.gui.roles;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.gui.select.SubGuiSoundSelection;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class SubGuiNpcConversationLine extends GuiBasic implements ITextfieldListener {

	public String line;
	public ResourceLocation sound;

	public SubGuiNpcConversationLine(String lineIn, String soundIn) {
		super();
		setBackground("menubg.png");
		imageWidth = 212;
		imageHeight = 119;

		line = lineIn;
		sound = new ResourceLocation(soundIn);
	}

	@Override
	public void initGui() {
		super.initGui();
		int x = guiLeft + 5;
		int y = guiTop + 6;
		// message
		addLabel(0, x + 1, y, Component.translatable("conversation.line").append(":"));
		addTextField(0, x + 1, y += 11, 200, 18, line);
		// sound
		addLabel(1, x + 1, y += 22, Component.translatable("stats.firesound").append(":"));
		addTextField(1, x + 1, y += 11, 200, 18, sound == null ? "" : sound.toString())
				.setResourceLocationType(1);
		addButton(1, x, y += 22, "gui.selectSound")
				.setSize(90, 20);
		addButton(2, x + 96, y, "X")
				.setSize(20, 20);
		// exit
		addButton(66, guiLeft + imageWidth - 96, y + 22, "gui.done")
				.setSize(90, 20);
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 1: setSubGui(new SubGuiSoundSelection(this, 0, null, sound == null ? "" : sound.toString())); break;
			case 2: sound = null; initGui(); break;
			case 66: onClose(); break;
		}
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiSoundSelection && ((SubGuiSoundSelection) subgui).resource != null) {
			sound = ((SubGuiSoundSelection) subgui).resource;
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 0: line = textField.getValue(); break;
			case 1: {
				if (textField.isEmpty() || textField.getValue().equals("minecraft:")) {
					sound = null;
					textField.setValue("");
				}
				else { sound = textField.getResourceLocation(null); }
				break;
			}
		}
	}

}
