package noppes.npcs.client.gui.availability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.client.gui.select.SubGuiDialogSelection;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumAvailabilityDialog;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

// Change from Unofficial (BetaZavr)
public class SubGuiNpcAvailabilityDialog
		extends GuiNPCInterface
		implements ICustomScrollListener {

	protected final Availability availability;
	protected final Map<Component, EnumAvailabilityDialog> dataEnum = new HashMap<>();
	protected final Map<Component, Integer> dataIDs = new HashMap<>();
	protected GuiCustomScrollNop scroll;
	protected Component select = Component.empty();

	public SubGuiNpcAvailabilityDialog(Availability availabilityIn) {
		super();
		setBackground("menubg.png");
		imageWidth = 256;
		imageHeight = 217;

		availability = availabilityIn;
	}

	@Override
	public void initGui() {
		super.initGui();
		boolean isSelect = !select.getFormattedText().isEmpty();
		// title
		addLabel(0, guiLeft + 6, guiTop + 4, "availability.available.9")
				.setSize(imageWidth - 12, 12)
				.setCenter(imageWidth - 12);
		// exit
		addButton(66, guiLeft + 6, guiTop + 192, "gui.done")
				.setSize(70, 20)
				.setHoverTexts("hover.back");
		// data
		if (scroll == null) { scroll = addScroll(6).setSize(imageWidth - 12, imageHeight - 66); }
		dataIDs.clear();
		dataEnum.clear();
		for (int id : availability.dialogues.keySet()) {
			Component key = Component.literal("ID:" + id + " - ");
			Dialog dialog = DialogController.instance.dialogs.get(id);
			if (dialog == null) {
				key.append(Component.translatable("quest.notfound").withStyle(TextFormatting.DARK_RED));
			} else {
				key.append(Component.translatable(dialog.getCategory().getName() + "/").withStyle(TextFormatting.GRAY))
						.append(Component.literal(dialog.getName()).withStyle(TextFormatting.RESET))
						.append(Component.literal(" (").withStyle(TextFormatting.GRAY))
						.append(Component.translatable("availability." + availability.dialogues.get(id).name().toLowerCase()).withStyle(TextFormatting.BLUE))
						.append(Component.literal(")").withStyle(TextFormatting.GRAY));
			}
			dataIDs.put(key, id);
			dataEnum.put(key, availability.dialogues.get(id));
		}
		if (isSelect) {
			boolean found = false;
			for (Component line : dataIDs.keySet()) {
				if (line.getString().equals(select.getString())) {
					found= true;
					break;
				}
			}
			if (!found) {
				select = Component.empty();
				isSelect = false;
			}
		}
		scroll.setNormalList(new ArrayList<>(dataIDs.keySet()));
		if (isSelect) { scroll.setSelected(select); }
		add(scroll.setPos(guiLeft + 6, guiTop + 14));
		int p = 0;
		if (isSelect) { p = dataEnum.get(select).ordinal(); }
		// type
		addButton(0, guiLeft + 6, guiTop + imageHeight - 46, false, p, "availability.always", "availability.after", "availability.before")
				.setSize(50, 20)
				.setHoverTexts("availability.hover.enum.type");
		// select
		addButton(1, guiLeft + 58, guiTop + imageHeight - 46, "availability.select")
				.setSize(170, 20)
				.setHoverTexts("availability.hover.dialog");
		// del
		addButton(2, guiLeft + 230, guiTop + imageHeight - 46, "X")
				.setSize(20, 20)
				.setHoverTexts("availability.hover.remove");
		// extra
		addButton(3, guiLeft + imageWidth - 76, guiTop + 192, "availability.more")
				.setSize(70, 20)
				.setIsEnabled(isSelect)
				.setHoverTexts("availability.hover.more");
		updateGuiButtons();
	}


	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				if (!dataIDs.containsKey(select)) { return; }
				EnumAvailabilityDialog ead = EnumAvailabilityDialog.values()[button.getValue()];
				int id = dataIDs.get(select);
				availability.dialogues.put(id, ead);
				select = Component.literal("ID:" + id + " - ");
				Dialog dialog = DialogController.instance.dialogs.get(id);
				if (dialog == null) { select.append(Component.translatable("quest.notfound").withStyle(TextFormatting.DARK_RED)); }
				else {
					select.append(Component.translatable(dialog.getCategory().getName() + "/").withStyle(TextFormatting.GRAY))
							.append(Component.literal(dialog.getName()).withStyle(TextFormatting.RESET))
							.append(Component.literal(" (").withStyle(TextFormatting.GRAY))
							.append(Component.translatable("availability." + ead.name().toLowerCase()).withStyle(TextFormatting.BLUE))
							.append(Component.literal(")").withStyle(TextFormatting.GRAY));
				}
				initGui();
				break;
			}
			case 1: setSubGui(new SubGuiDialogSelection(select.getFormattedText().isEmpty() ? 0 : dataIDs.get(select))); break;
			case 2: {
				availability.dialogues.remove(dataIDs.get(select));
				select = Component.empty();
				initGui();
				break;
			}
			case 3: {
				save();
				initGui();
				break;
			}
			case 66 : onClose(); break;
		}
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		SubGuiDialogSelection selector = (SubGuiDialogSelection) subgui;
		if (selector.selectedDialog == null) { return; }
		if (!select.getFormattedText().isEmpty()) {
			availability.dialogues.remove(dataIDs.get(select));
		}
		select = Component.literal("ID:" + selector.selectedDialog.id + " - ");
		select.append(Component.translatable(selector.selectedCategory.getName() + "/").withStyle(TextFormatting.GRAY))
				.append(Component.literal(selector.selectedDialog.getName()).withStyle(TextFormatting.RESET))
				.append(Component.literal(" (").withStyle(TextFormatting.GRAY))
				.append(Component.translatable("availability.after").withStyle(TextFormatting.BLUE))
				.append(Component.literal(")").withStyle(TextFormatting.GRAY));
		availability.dialogues.put(selector.selectedDialog.id, EnumAvailabilityDialog.After);
		initGui();
	}



	// New from Unofficial (BetaZavr)
	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		select = scroll.getNormalSelected();
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		setSubGui(new SubGuiDialogSelection(dataIDs.get(select)));
	}

	@Override
	public void onClose() {
		super.onClose();
		for (int id : new ArrayList<>(availability.dialogues.keySet())) {
			if (availability.dialogues.get(id) == EnumAvailabilityDialog.Always) { availability.dialogues.remove(id); }
		}
	}

	@Override
	public void save() {
		if (!dataIDs.containsKey(select)) { return; }
		EnumAvailabilityDialog ead = EnumAvailabilityDialog.values()[getButton(0).getValue()];
		int id = dataIDs.get(select);
		if (ead != EnumAvailabilityDialog.Always) {
			availability.dialogues.put(id, ead);
			dataEnum.put(select, ead);
		}
		else { availability.dialogues.remove(id); }
		select = Component.empty();
	}

	private void updateGuiButtons() {
		int p = 0;
		getButton(1).setDisplayText("availability.selectdialog");
		Dialog dialog = null;
		boolean isSelect = !select.getFormattedText().isEmpty();
		if (isSelect) {
			dialog = DialogController.instance.dialogs.get(dataIDs.get(select));
			p = dataEnum.get(select).ordinal();
		}
		getButton(0).setDisplay(p)
				.setIsEnabled(isSelect);
		getButton(1).setIsEnabled(p != 0 || !isSelect)
				.setDisplayText(dialog == null ? "availability.select" : dialog.getName());
		getButton(2).setIsEnabled(isSelect);
	}

}
