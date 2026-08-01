package noppes.npcs.client.gui.select;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.network.chat.Component;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogCategory;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.GuiSelectionListener;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

public class SubGuiDialogSelection extends GuiBasic implements ICustomScrollListener {

	protected final HashMap<String, DialogCategory> categoryData = new HashMap<>();
	protected final HashMap<String, Dialog> dialogData = new HashMap<>();
	protected GuiCustomScrollNop scrollCategories;
	protected GuiCustomScrollNop scrollDialogs;
	protected GuiSelectionListener listener;
	public DialogCategory selectedCategory;
	public Dialog selectedDialog;

	// New from Unofficial (BetaZavr)
	public int id;

	public SubGuiDialogSelection(int dialogId) {
		super();
		drawDefaultBackground = false;
		setBackground("menubg.png");
		imageWidth = 366;
		imageHeight = 226;

		selectedDialog = DialogController.instance.dialogs.get(dialogId);
		if (selectedDialog != null) { selectedCategory = selectedDialog.category; }
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		if (button.id == 2) {
			if (selectedDialog != null) { scrollDoubleClicked(null); }
			else { onClose(); }
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		int w = 177;
		int x0 = guiLeft + 5;
		int x1 = x0 + w + 2;
		int y = guiTop + 8;
		if (wrapper.parent instanceof GuiSelectionListener) { listener = (GuiSelectionListener) wrapper.parent; }
		addLabel(0, x0 + 3, y, Component.translatable("gui.categories").append(":"))
				.setSize(168, 10);
		addLabel(1, x1 + 3, y, Component.translatable("dialog.dialogs").append(":"))
				.setSize(168, 10);
		addButton(2, guiLeft + imageWidth - 16, y - 4, "x").setSize(12, 12);
		categoryData.clear();
		for (DialogCategory category : DialogController.instance.categories.values()) { categoryData.put(category.title, category); }
		dialogData.clear();
		if (selectedCategory != null) {
			for (Dialog dialog : selectedCategory.dialogs.values()) { dialogData.put(dialog.title, dialog); }
		}
		y += 10;
		if (scrollCategories == null) { scrollCategories = addScroll(0).setSize(w, imageHeight - 23); }
		scrollCategories.setList(new ArrayList<>(categoryData.keySet()));
		if (selectedCategory != null) { scrollCategories.setSelected(selectedCategory.title); }
		add(scrollCategories.setPos(x0, y));
		if (scrollDialogs == null) { scrollDialogs = addScroll(1).setSize(w, imageHeight - 23); }
		scrollDialogs.setList(new ArrayList<>(dialogData.keySet()));
		if (selectedDialog != null) { scrollDialogs.setSelected(selectedDialog.title); }
		add(scrollDialogs.setPos(x1, y));
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0) {
			selectedCategory = categoryData.get(scroll.getSelected());
			selectedDialog = null;
			scrollDialogs.clearSelection();
		}
		if (scroll.id == 1) {
			selectedDialog = dialogData.get(scroll.getSelected());
		}
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (selectedDialog != null) {
			if (listener != null) { listener.selected(selectedDialog.id, selectedDialog.title); }
			onClose();
		}
	}

	public SubGuiDialogSelection setId(int idIn) {
		id = idIn;
		return this;
	}

}
