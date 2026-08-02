package noppes.npcs.client.gui.global;

import net.minecraft.init.Blocks;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.api.constants.OptionType;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.client.gui.availability.SubGuiNpcAvailability;
import noppes.npcs.client.gui.player.GuiDialogInteract;
import noppes.npcs.client.gui.select.SubGuiDialogSelection;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubGuiNpcDialogOption
		extends GuiBasic
		implements ITextfieldListener, ICustomScrollListener {

	protected final DialogOption option;
	public static int LastColor = new Color(0xE0E0E0).getRGB();

	// New from Unofficial (BetaZavr)
	public static final Object[] options = new Object[] { "gui.close", "dialog.dialog", "gui.disabled", "menu.role", Blocks.COMMAND_BLOCK.getUnlocalizedName() + ".name" };
	public final GuiScreen parent;
	private final Map<Component, DialogOption.OptionDialogID> data = new HashMap<>(); // {scrollTitle, dialogID}
	private GuiCustomScrollNop scroll;
	private Component select = Component.empty();

	public SubGuiNpcDialogOption(DialogOption optionIn, GuiScreen gui) {
		setBackground("menubg.png");
		imageWidth = 256;
		imageHeight = 216;

		option = optionIn;
		parent = gui;
	}

	@Override
	public void initGui() {
		super.initGui();
		int x0 = guiLeft + 4;
		int x1 = x0 + 58;
		int x2 = x0 + 145;
		int x3 = x2 + 52;
		addLabel(66, guiLeft, guiTop + 4, "dialog.editoption");
		getLabel(66).setCenter(imageWidth);

		addLabel(0, x0, guiTop + 20, "gui.title");
		addTextField(0, x0 + 10, guiTop + 15, 196, 20, option.title)
				.setHoverTexts("dialog.option.hover.name");
		StringBuilder color = new StringBuilder(Integer.toHexString(option.optionColor));
		while (color.length() < 6) { color.insert(0, 0); }
		addLabel(2, x0, guiTop + 45, "gui.color");
		addButton(2, x1, guiTop + 40, color.toString())
				.setSize(92, 20)
				.setColor(option.optionColor)
				.setHoverTexts("color.hover");
		List<Object> list = new ArrayList<>();
		list.add("");
		for (ResourceLocation res : GuiDialogInteract.icons.values()) {
			list.add(res.getResourcePath().substring(res.getResourcePath().lastIndexOf("/") + 1, res.getResourcePath().lastIndexOf(".")));
		}
		addLabel(9, x2 + 10, guiTop + 61, "dialog.icon");
		addButton(9, x3 + 10, guiTop + 45, false, option.iconId, list.toArray(new Object[0]))
				.setSize(32, 32)
				.setTexture(GuiDialogInteract.icons.get(option.iconId))
				.setDefBack(true)
				.setUV(0, 0, 256, 256)
				.setHoverTexts("dialog.option.hover.name");
		addLabel(1, x0, guiTop + 67, "dialog.optiontype");
		addButton(1, x1, guiTop + 62, false, option.optionType.get(), options)
				.setSize(92, 20)
				.setHoverTexts("dialog.option.hover.type." + option.optionType.get());


		data.clear();
		List<Component> keys = new ArrayList<>();
		int pos = -1, i = 0;
		DialogOption.OptionDialogID del = null;
		for (DialogOption.OptionDialogID od : option.dialogs) {
			if (od.dialogId <= 0) { del = od; }
			Component key;
			Dialog d = DialogController.instance.get(od.dialogId);
			if (d == null) {
				key = Component.empty()
						.append(Component.literal("ID: " + od.dialogId).withStyle(TextFormatting.GRAY))
						.append(Component.literal(" Dialog Not Found!").withStyle(TextFormatting.RED));
			}
			else { key = d.getKey(); }
			data.put(key, od);
			keys.add(key);
			if (key.getString().equals(select.getString())) { pos = i; }
			i++;
		}
		if (del != null) { option.dialogs.remove(del); }

		if (scroll == null) { scroll = addScroll(0).setSize(141, 116); }
		scroll.setList(new ArrayList<>())
				.setUnsortedList(keys);
		if (!select.getString().isEmpty()) { scroll.setSelected(select); }
		add(scroll.setPos(x0, guiTop + 96));
		if (!data.containsKey(select)) { select = Component.empty(); }

		if (option.optionType == OptionType.DIALOG_OPTION) { // next dialog
			addLabel(4, x0, guiTop + 84, "gui.options");
			addButton(3, x2, guiTop + 96, "gui.add")
					.setSize(50, 20)
					.setHoverTexts("dialog.option.hover.add");
			addButton(4, x3, guiTop + 96, "gui.remove")
					.setSize(50, 20)
					.setIsEnabled(!select.getString().isEmpty())
					.setHoverTexts("dialog.option.hover.del");
			addButton(5, x2, guiTop + 118, "gui.edit")
					.setSize(80, 20)
					.setHoverTexts("dialog.option.hover.edit");
			addButton(6, x2, guiTop + 140, "type.up")
					.setSize(50, 20)
					.setIsEnabled(!select.getString().isEmpty() && pos != 0)
					.setHoverTexts("dialog.option.hover.up");
			addButton(7, x3, guiTop + 140, "type.down")
					.setSize(50, 20)
					.setIsEnabled(!select.getString().isEmpty() && pos > -1 && pos < data.size() - 1)
					.setHoverTexts("dialog.option.hover.down");
			addButton(8, x2, guiTop + 162, "availability.available")
					.setSize(80, 20)
					.setHoverTexts("dialog.option.hover.availability", select);
		}
		else if (option.optionType == OptionType.COMMAND_BLOCK) { // command
			addTextField(4, x0, guiTop + 84, 248, 20, option.command)
					.setHoverTexts("dialog.option.hover.command")
					.setMaxStringLength(Short.MAX_VALUE);
			addLabel(4, x0, guiTop + 110, "advMode.command");
			addLabel(5, x0, guiTop + 125, "advMode.nearestPlayer");
			addLabel(6, x0, guiTop + 140, "advMode.randomPlayer");
			addLabel(7, x0, guiTop + 155, "advMode.allPlayers");
			addLabel(8, x0, guiTop + 170, "dialog.commandoptionplayer");
		}
		addButton(66, guiLeft + 149, guiTop + 192, "gui.done")
				.setSize(80, 20)
				.setHoverTexts("hover.back");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 1: {
				option.optionType = OptionType.get(button.getValue());
				initGui();
				break;
			} // type
			case 2: {
				setSubGui(new SubGuiColorSelector(option.optionColor, new SubGuiColorSelector.ColorCallback() {
					@Override
					public void color(int colorIn) { LastColor = option.optionColor = colorIn; }

					@Override
					public void preColor(int colorIn) {  }
				}));
				break;
			} // color
			case 3: {
				if (option.optionType != OptionType.DIALOG_OPTION) { return; }
				setSubGui(new SubGuiDialogSelection(-1).setId(0));
				break;
			} // add dialog
			case 4: {
				if (option.optionType != OptionType.DIALOG_OPTION || select.getString().isEmpty() || !data.containsKey(select)) { return; }
				option.dialogs.remove(data.get(select));
				initGui();
				break;
			} // del dialog
			case 5: {
				if (option.optionType != OptionType.DIALOG_OPTION || select.getString().isEmpty() || !data.containsKey(select)) { return; }
				setSubGui(new SubGuiDialogSelection(data.get(select).dialogId).setId(1));
				break;
			} // edit dialog
			case 6: {
				if (option.optionType != OptionType.DIALOG_OPTION || select.getString().isEmpty() || !data.containsKey(select)) { return; }
				option.upPos(data.get(select).dialogId);
				initGui();
				break;
			} // up dialog
			case 7: {
				if (option.optionType != OptionType.DIALOG_OPTION || select.getString().isEmpty() || !data.containsKey(select)) { return; }
				option.downPos(data.get(select).dialogId);
				initGui();
				break;
			} // down dialog
			case 8: {
				if (select.getString().isEmpty() || !data.containsKey(select)) { return; }
				setSubGui(new SubGuiNpcAvailability(data.get(select).availability, parent));
				break;
			} // availability
			case 9: {
				if (option == null) { return; }
				option.iconId = button.getValue();
				button.texture = GuiDialogInteract.icons.get(option.iconId);
				break;
			} // icons
			case 66: {
				onClose();
				break;
			} // exit
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textfield) {
		if (textfield.id == 0) {
			if (textfield.isEmpty()) {
				option.title = "Talk";
				textfield.setValue(option.title);
			} else {
				option.title = textfield.getValue();
			}
		}
		else if (textfield.id == 4) {
			option.command = textfield.getValue();
		}
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiDialogSelection && ((SubGuiDialogSelection) subgui).selectedDialog != null) {
			SubGuiDialogSelection gui = (SubGuiDialogSelection) subgui;
			if (gui.id == 0) {
				option.addDialog(gui.selectedDialog.id);
				select = gui.selectedDialog.getKey();
			}
			else if (gui.id == 1 && !select.getString().isEmpty() && data.containsKey(select)) {
				option.replaceDialogIDs(data.get(select).dialogId, gui.selectedDialog.id); // edit
			}
		}
		initGui();
	}

	// New from Unofficial (BetaZavr)
	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (option.optionType != OptionType.DIALOG_OPTION || !scroll.hasSelected()) { return; }
		select = scroll.getNormalSelected();
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (option.optionType != OptionType.DIALOG_OPTION || select.getString().isEmpty()  || !data.containsKey(select)) { return; }
		setSubGui(new SubGuiDialogSelection(data.get(select).dialogId).setId(1));
	}

}
