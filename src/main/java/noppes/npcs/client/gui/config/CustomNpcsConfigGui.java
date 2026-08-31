package noppes.npcs.client.gui.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.GuiConfigEntries.ArrayEntry;
import net.minecraftforge.fml.client.config.GuiConfigEntries.ButtonEntry;
import net.minecraftforge.fml.client.config.GuiConfigEntries.IConfigEntry;
import net.minecraftforge.fml.client.config.GuiConfigEntries.StringEntry;
import net.minecraftforge.fml.client.config.GuiEditArray;
import net.minecraftforge.fml.client.config.GuiEditArrayEntries;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.client.config.IConfigElement;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.util.Util;

public class CustomNpcsConfigGui extends GuiConfig {

	public static class ColorEntry extends ButtonEntry {

		protected final String beforeValue;
		public String currentValue;

		public ColorEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
			super(owningScreen, owningEntryList, configElement);
			String value = configElement.get().toString();
			if (value.length() > 6) { value = value.substring(0, 6); }
			beforeValue = value;
			currentValue = value;
			btnValue.enabled = enabled();
			btnValue.width = 120;
			updateValueButtonText();
		}

		@Override
		public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY,
				boolean isSelected, float partial) {
			btnValue.visible = false;
			super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected, partial);
			btnValue.width = (int) ((float) (owningEntryList.controlWidth - 2) * 0.75f);
			btnValue.x = owningScreen.entryList.controlX;
			btnValue.y = y;
			btnValue.enabled = enabled();
			btnValue.visible = true;
			btnValue.drawButton(mc, mouseX, mouseY, partial);
			int left = btnValue.x + btnValue.width + 2;
			int right = left + (owningEntryList.controlWidth - btnValue.width) - 2;
			GuiUtils.drawGradientRect(0, left, y, right, y + 18, 0xFF000000, 0xFF000000);
			int color = (int) Long.parseLong(currentValue, 16);
			float alpha = (float) (color >> 24 & 255) / 255.0F;
			if (alpha == 0.0f) { color += 0xFF000000; }
			GuiUtils.drawGradientRect(0, left + 1, y + 1, right - 1, y + 17, color, color);
		}

		public GuiButtonExt getButton() { return btnValue; }

		@Override
		public Object getCurrentValue() { return currentValue; }

		@Override
		public Object[] getCurrentValues() { return new Object[] { getCurrentValue() }; }

		@Override
		public boolean isChanged() { return beforeValue != null ? !beforeValue.equals(currentValue) : currentValue.trim().isEmpty(); }

		@Override
		public boolean isDefault() { return configElement.getDefault() != null ? configElement.getDefault().toString().equals(currentValue) : currentValue.trim().isEmpty(); }

		@Override
		public boolean saveConfigElement() {
			if (enabled()) {
				if (isChanged() && isValidValue) {
					configElement.set(currentValue);
					return configElement.requiresMcRestart();
				} else if (isChanged() && !isValidValue) {
					configElement.setToDefault();
					return configElement.requiresMcRestart() && beforeValue != null
							? beforeValue.equals(configElement.getDefault())
							: configElement.getDefault() == null;
				}
			}
			return false;
		}

		@Override
		public void setToDefault() {
			if (enabled()) {
				currentValue = configElement.getDefault().toString();
				keyTyped((char) Keyboard.CHAR_NONE, Keyboard.KEY_HOME);
				btnValue.displayString = currentValue;
			}
		}

		@Override
		public void undoChanges() {
			if (enabled()) {
				currentValue = beforeValue;
				btnValue.displayString = currentValue;
			}
		}

		@Override
		public void updateValueButtonText() { btnValue.displayString = I18n.format(String.valueOf(currentValue)); }

		@Override
		public void valueButtonPressed(int slotIndex) {
			if (enabled()) {
				subGui = new SubGuiColorSelector((int) Long.parseLong(currentValue, 16));
				subGui.width = owningEntryList.width;
				subGui.height = owningEntryList.height;
				subGui.initGui();
				subGui.object = this;
				subGui.background = null;
				subGui.wrapper.parent = owningScreen;
			}
		}
	}

	public static class CustomArrayEntry extends ArrayEntry {

		public CustomArrayEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
			super(owningScreen, owningEntryList, configElement);
		}

		@Override
		public void valueButtonPressed(int slotIndex) {
			mc.displayGuiScreen(new CustomGuiEditArray(owningScreen, configElement, slotIndex, currentValues, enabled()));
		}
	}

	public static class CustomGuiEditArray extends GuiEditArray {

		public static SubGuiColorSelector subGui;

		public CustomGuiEditArray(GuiScreen parentScreen, IConfigElement configElement, int slotIndex, Object[] currentValues, boolean enabled) {
			super(parentScreen, configElement, slotIndex, currentValues, enabled);
		}

		@Override
		protected GuiEditArrayEntries createEditArrayEntries() { return new CustomGuiEditArrayEntries(this, mc, configElement, beforeValues, currentValues); }

		@Override
		public void drawScreen(int mouseX, int mouseY, float partialTicks) {
			if (subGui != null) {
				drawDefaultBackground();
				subGui.drawScreen(mouseX, mouseY, partialTicks);
				drawCenteredString(fontRenderer, title, width / 2, 8, 16777215);
				if (titleLine2 != null) { drawCenteredString(fontRenderer, titleLine2, width / 2, 18, 16777215); }
				if (titleLine3 != null) { drawCenteredString(fontRenderer, titleLine3, width / 2, 28, 16777215); }
				return;
			}
			super.drawScreen(mouseX, mouseY, partialTicks);
		}

		@Override
		public void initGui() {
			super.initGui();
			if (subGui != null) {
				subGui.width = width;
				subGui.height = height;
				subGui.initGui();
			}
		}

		@Override
		public void keyTyped(char eventChar, int eventKey) {
			if (subGui != null) {
				subGui.keyPressed(eventChar, eventKey);
				return;
			}
			super.keyTyped(eventChar, eventKey);
		}

		@Override
		public void mouseClicked(int x, int y, int mouseEvent) throws IOException {
			if (subGui != null) {
				subGui.mouseClicked(x, y, mouseEvent);
				return;
			}
			super.mouseClicked(x, y, mouseEvent);
		}

		@Override
		public void mouseReleased(int x, int y, int mouseEvent) {
			if (subGui != null) {
				subGui.mouseReleased(x, y, mouseEvent);
				return;
			}
			super.mouseReleased(x, y, mouseEvent);
		}

		public void subGuiClosed(SubGuiColorSelector subgui) {
			String color = subgui.getColor().toUpperCase();
			if (subgui.getObject() instanceof CustomGuiEditArrayEntries.ColorEntry) {
				ConfigElement element = (ConfigElement) ((CustomGuiEditArrayEntries.ColorEntry) subgui.getObject()).configElement;
				((CustomGuiEditArrayEntries.ColorEntry) subgui.getObject()).getButton().displayString = color;
				((CustomGuiEditArrayEntries.ColorEntry) subgui.getObject()).value = color;
				element.set(color);
			}
			subGui = null;
		}
	}

	public static SubGuiColorSelector subGui = null;

	public CustomNpcsConfigGui(GuiScreen parentScreen, List<IConfigElement> elementsList, String modName) {
		super(parentScreen, elementsList, modName, false, false, modName);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (subGui != null) {
			drawDefaultBackground();
			subGui.drawScreen(mouseX, mouseY, partialTicks);
			drawCenteredString(fontRenderer, title, width / 2, 8, 0xFFFFFFFF);
			if (subGui.object instanceof ColorEntry) { drawCenteredString(fontRenderer, ((ColorEntry) subGui.object).getName(), width / 2, 18, 0xFFA0A0A0); }
			String title2 = titleLine2;
			if (title2 != null) {
				int strWidth = mc.fontRenderer.getStringWidth(title2);
				int ellipsisWidth = mc.fontRenderer.getStringWidth("...");
				if (strWidth > width - 6 && strWidth > ellipsisWidth) { title2 = mc.fontRenderer.trimStringToWidth(title2, width - 6 - ellipsisWidth).trim() + "..."; }
				drawCenteredString(fontRenderer, title2, width / 2, 18, 0xFFFFFF);
			}
			btnUndoAll.enabled = entryList.areAnyEntriesEnabled(chkApplyGlobally.isChecked()) && entryList.hasChangedEntry(chkApplyGlobally.isChecked());
			btnDefaultAll.enabled = entryList.areAnyEntriesEnabled(chkApplyGlobally.isChecked())
					&& !entryList.areAllEntriesDefault(chkApplyGlobally.isChecked());

			if (undoHoverChecker.checkHover(mouseX, mouseY)) {
				drawToolTip(Arrays.asList(I18n.format("fml.configgui.tooltip.undoAll").split("\n")), mouseX, mouseY);
			}
			if (resetHoverChecker.checkHover(mouseX, mouseY)) {
				drawToolTip(Arrays.asList(I18n.format("fml.configgui.tooltip.resetAll").split("\n")), mouseX, mouseY);
			}
			if (checkBoxHoverChecker.checkHover(mouseX, mouseY)) {
				drawToolTip(Arrays.asList(I18n.format("fml.configgui.tooltip.applyGlobally").split("\n")), mouseX, mouseY);
			}
			return;
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void drawToolTip(List<String> stringList, int x, int y) {
		List<String> newToolTip = new ArrayList<>();
		String name = "";
		for (String str : stringList) {
			if (name != null) {
				if (name.isEmpty()) { name = Util.instance.deleteColor(str.toLowerCase()); }
				else {
					String c = "";
					if (name.contains("charcurrencies") || name.contains("chardonation")) {
						for (IConfigEntry configEntry : entryList.listEntries) {
							if (configEntry.getName().equalsIgnoreCase(name)) {
								try { c = "" + ((char) Integer.parseInt((String) configEntry.getCurrentValue(), 16)); }
								catch (Exception e) { c = "" + ((String) configEntry.getCurrentValue()).charAt(0); }
								break;
							}
						}
					}
					newToolTip.add(((char) 167) + "e" + new TextComponentTranslation("property." + name + ".hover", c).getFormattedText());
					name = null;
					continue;
				}
			}
			newToolTip.add(str);
		}
		GuiUtils.drawHoveringText(newToolTip, x, y, width, height, 300, fontRenderer);
	}

	@Override
	public void initGui() {
		super.initGui();
		if (subGui != null) {
			subGui.width = width;
			subGui.height = height;
			subGui.initGui();
		}
		int i = -1;
		List<IConfigEntry> list = new ArrayList<>(entryList.listEntries);
		for (IConfigEntry entry : list) {
			i++;
			if (entry.getConfigElement().getType() != ConfigGuiType.COLOR) { continue; }
			if (entry.getClass() == StringEntry.class) {
				entryList.listEntries.remove(entry);
				entryList.listEntries.add(i, new ColorEntry(this, entryList, entry.getConfigElement()));
			}
			if (entry.getClass() == ArrayEntry.class) {
				entryList.listEntries.remove(entry);
				entryList.listEntries.add(i, new CustomArrayEntry(this, entryList, entry.getConfigElement()));
			}
		}
	}

	@Override
	public void keyTyped(char eventChar, int eventKey) {
		if (subGui != null) {
			subGui.keyPressed(eventChar, eventKey);
			return;
		}
		super.keyTyped(eventChar, eventKey);

	}

	@Override
	public void mouseClicked(int x, int y, int mouseEvent) throws IOException {
		if (subGui != null) {
			subGui.mouseClicked(x, y, mouseEvent);
			return;
		}
		super.mouseClicked(x, y, mouseEvent);
	}

	@Override
	public void mouseReleased(int x, int y, int mouseEvent) {
		if (subGui != null) {
			subGui.mouseReleased(x, y, mouseEvent);
			return;
		}
		super.mouseReleased(x, y, mouseEvent);
	}

	@Override
	public void onGuiClosed() {
		CustomNpcs.Config.resetData();
		CustomNpcs.Config.config.save();
		CustomNpcs.resetChars(CustomNpcs.CharCurrencies, CustomNpcs.CharDonation);
		super.onGuiClosed();
	}

	public void subGuiClosed(SubGuiColorSelector subgui) {
		String color = subgui.getColor().toUpperCase();
		if (subgui.getObject() instanceof ColorEntry) {
			ConfigElement element = (ConfigElement) ((ColorEntry) subgui.getObject()).getConfigElement();
			((ColorEntry) subgui.getObject()).getButton().displayString = color;
			((ColorEntry) subgui.getObject()).currentValue = color;
			element.set(color);
		}
		subGui = null;
	}

}
