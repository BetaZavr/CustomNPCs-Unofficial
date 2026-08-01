package noppes.npcs.client.gui.global;

import java.util.*;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.api.constants.OptionType;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogCategory;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;

public class GuiNpcManageDialogs extends GuiNPCInterface2 implements ICustomScrollListener {

	protected Map<Component, DialogCategory> categoryData = new LinkedHashMap<>();
	protected Map<Component, Dialog> dialogData = new LinkedHashMap<>();
	protected GuiCustomScrollNop scrollCategories;
	protected GuiCustomScrollNop scrollDialogs;

	// New from Unofficial (BetaZavr)
	protected static boolean sortByName = true;
	protected Component selectedCategory = Component.empty(); // OLD DialogCategory
	protected Component selectedDialog = Component.empty(); // OLD Dialog
	protected Dialog copyDialog = null;

	public GuiNpcManageDialogs(EntityNPCInterface npc) {
		super(npc);
		backGui = EnumGuiType.MainMenuGlobal;
		Packets.sendServer(new SPacketDialogCategoryGet());
	}

	@Override
	public void initGui() {
		super.initGui();
		categoryData.clear();
		dialogData.clear();
		DialogController dData = DialogController.instance;
		// category's
		for (DialogCategory category : dData.categories.values()) {
			Component key = Component.translatable(category.title);
			categoryData.put(key, category);
			if (selectedCategory.getFormattedText().isEmpty()) { selectedCategory = key; }
		}
		// dialogs
		LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
		if (!selectedCategory.getFormattedText().isEmpty()) {
			if (categoryData.containsKey(selectedCategory)) {
				Map<Component, Dialog> map = new LinkedHashMap<>();
				for (Dialog dialog : categoryData.get(selectedCategory).dialogs.values()) {
					boolean b = !dialog.text.isEmpty();
					Component key = Component.empty()
							.append(Component.literal("ID:" + dialog.id + " \"").withStyle(TextFormatting.GRAY))
							.append(Component.translatable(dialog.title).withStyle(TextFormatting.RESET))
							.append(Component.literal("\"").withStyle(TextFormatting.GRAY))
							.append(Component.literal(" (").withStyle(b ? TextFormatting.DARK_GREEN : TextFormatting.RED))
							.append(Component.translatable("quest.has." + b).withStyle(b ? TextFormatting.DARK_GREEN : TextFormatting.RED))
							.append(Component.literal(")").withStyle(b ? TextFormatting.DARK_GREEN : TextFormatting.RED));
					map.put(key, dialog);
				}
				for (Map.Entry<Component, Dialog> entry : getEntryList(map)) {
					dialogData.put(entry.getKey(), entry.getValue());
					if (selectedDialog.getFormattedText().isEmpty()) { selectedDialog = entry.getKey(); }
				}
				// Hover Text:
				if (!dialogData.isEmpty()) {
					int pos = 0;
					Map<String, Integer> nextDialogIDs = new TreeMap<>();
					for (Dialog dialog : dialogData.values()) {
						List<Component> hovers = new ArrayList<>();
						List<Component> activationDialogs = new ArrayList<>();
						List<Component> nextDialogs = new ArrayList<>();
						hovers.add(Component.translatable(dialog.title).append(Component.literal(":")));
						for (DialogOption option : dialog.options.values()) {
							if (option.optionType != OptionType.DIALOG_OPTION || option.dialogs.isEmpty()) { continue; }
							int i = 0;
							for (DialogOption.OptionDialogID od : option.dialogs) {
								nextDialogIDs.put(option.slot + "." + i, od.dialogId);
								i++;
							}
						}
						try {
							Set<Integer> dSet = dData.dialogs.keySet();
							for (int dialogId : dSet) {
								if (!dData.hasDialog(dialogId)) { continue; }
								Dialog d = dData.get(dialogId);
								for (DialogOption option : d.options.values()) {
									if (option.optionType != OptionType.DIALOG_OPTION || option.dialogs.isEmpty()) { continue; }
									int i = 0;
									for (DialogOption.OptionDialogID od : option.dialogs) {
										if (od.dialogId != dialog.id) { continue; }
										activationDialogs.add(Component.empty()
												.append(Component.literal("ID:" + d.id).withStyle(TextFormatting.GRAY))
												.append(Component.literal(" " + Component.translatable("gui.answer").getFormattedText()  + ": ").withStyle(TextFormatting.DARK_GRAY))
												.append(Component.literal(option.slot + "." + i).withStyle(TextFormatting.GRAY))
												.append(Component.literal(" " + d.category.getName() + "/").withStyle(TextFormatting.DARK_GRAY))
												.append(Component.literal(d.getName()).withStyle(TextFormatting.RESET)));
										i++;
									}
								}
								if (nextDialogIDs.containsValue(d.id)) {
									for (String k : nextDialogIDs.keySet()) {
										if (nextDialogIDs.get(k) != d.id) {
											continue;
										}
										nextDialogs.add(Component.empty()
												.append(Component.literal(Component.translatable("gui.answer").getFormattedText() + ": ").withStyle(TextFormatting.DARK_GRAY))
												.append(Component.literal(k + " ID:" + d.id).withStyle(TextFormatting.GRAY))
												.append(Component.literal(" " + d.category.getName() + "/").withStyle(TextFormatting.DARK_GRAY))
												.append(Component.literal(d.getName()).withStyle(TextFormatting.RESET)));
									}
								}
							}
						} catch (Exception e) { LogWriter.error(e); }
						if (!activationDialogs.isEmpty()) {
							hovers.add(Component.translatable("dialog.hover.act.1"));
							hovers.addAll(activationDialogs);
						}
						else { hovers.add(Component.translatable("dialog.hover.act.0")); }
						if (!nextDialogs.isEmpty()) {
							hovers.add(Component.translatable("dialog.hover.next.1"));
							hovers.addAll(nextDialogs);
						}
						else { hovers.add(Component.translatable("dialog.hover.next.0")); }
						hts.put(pos, hovers);
						pos++;
					}
				}
			}
			else {
				selectedCategory = Component.empty();
				selectedDialog = Component.empty();
			}
		}
		boolean hasCategory = !selectedCategory.getFormattedText().isEmpty();
		boolean hasDialog = !selectedDialog.getFormattedText().isEmpty();
		// scroll info
		addLabel(0, guiLeft + 8, guiTop + 4, "gui.categories");
		addLabel(1, guiLeft + 180, guiTop + 4, "dialog.dialogs");
		// dialog buttons
		int x = guiLeft + 350, y = guiTop + 8;
		addLabel(3, x + 2, y, "dialog.dialogs");
		addButton(13, x, y += 10, "selectServer.edit")
				.setSize(64, 15)
				.setIsEnabled(hasDialog)
				.setHoverTexts("manager.hover.dialog.edit", selectedDialog);
		addButton(12, x, y += 17, "gui.remove")
				.setSize(64, 15)
				.setIsEnabled(hasDialog)
				.setHoverTexts("manager.hover.dialog.del", selectedDialog);
		addButton(11, x, y += 17, "gui.add")
				.setSize(64, 15)
				.setIsEnabled(hasCategory)
				.setHoverTexts("manager.hover.dialog.add", selectedCategory);
		addButton(10, x, y += 21, "gui.copy")
				.setSize(64, 15)
				.setIsEnabled(hasCategory)
				.setHoverTexts("manager.hover.dialog.copy", selectedDialog);
		addButton(9, x, y += 17, "gui.paste")
				.setSize(64, 15)
				.setIsEnabled(copyDialog != null)
				.setHoverTexts("manager.hover.dialog.paste." + (copyDialog != null), (copyDialog != null ? copyDialog.getKey() : ""));
		GuiButtonNop checkBox = addCheckBox(14, x, y + 17, "gui.name", "ID", sortByName)
				.setSize(64, 15);
		checkBox.setHoverTexts(Component.translatable("hover.sort",
				Component.translatable("dialog.dialogs").getFormattedText(),
				checkBox.getMessage().getFormattedText()));
		// category buttons
		y = guiTop + 130;
		addLabel(2, x + 2, y, "gui.categories");
		// edit
		addButton(3, x, y += 10, "selectServer.edit")
				.setSize(64, 15)
				.setIsEnabled(hasCategory)
				.setHoverTexts("manager.hover.category.edit");
		// del
		addButton(2, x, y += 17, "gui.remove")
				.setSize(64, 15)
				.setIsEnabled(hasCategory)
				.setHoverTexts("manager.hover.category.del");
		// add
		addButton(1, x, y + 17, "gui.add")
				.setSize(64, 15)
				.setHoverTexts("manager.hover.category.add");
		if (scrollCategories == null) { scrollCategories = addScroll(0).setSize(170, imageHeight - 3); }
		scrollCategories.setNormalList(new ArrayList<>(categoryData.keySet()));
		if (hasCategory) { scrollCategories.setSelected(selectedCategory); }
		add(scrollCategories.setPos(guiLeft + 4, guiTop + 15));
		if (scrollDialogs == null) { scrollDialogs = addScroll(1).setSize(170, imageHeight - 3); }
		scrollDialogs.setUnsortedList(new ArrayList<>(dialogData.keySet()));
		if (hasDialog) { scrollDialogs.setSelected(selectedDialog); }
		scrollDialogs.setHoverTexts(hts);
		add(scrollDialogs.setPos(guiLeft + 176, guiTop + 15));
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 1: {
				setSubGui(new SubGuiEditText(1, Component.translatable("gui.new").getString()));
				break;
			} // create cat
			case 2: {
				if (!categoryData.containsKey(selectedCategory)) { return; }
				ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
					if (bo) {
						int catId = categoryData.get(selectedCategory).id;
						selectedCategory = Component.empty();
						selectedDialog = Component.empty();
						Packets.sendServer(new SPacketDialogCategoryRemove(catId));
					}
					NoppesUtil.openGUI(player, this);
				},
						Component.translatable(categoryData.get(selectedCategory).title).getParent(),
						Component.translatable("message.delete").getParent());
				setScreen(guiYesNo);
				break;
			} // del cat
			case 3: {
				if (!dialogData.containsKey(selectedDialog)) { return; }
				setSubGui(new SubGuiEditText(3, categoryData.get(selectedCategory).title));
				break;
			} // rename cat
			case 9: {
				if (copyDialog == null || !categoryData.containsKey(selectedCategory)) { return; }
				Dialog dialog = copyDialog.copy(null);
				dialog.id = -1;
				dialog.category = categoryData.get(selectedCategory);
				StringBuilder t = new StringBuilder(dialog.title);
				boolean has = true;
				while (has) {
					has = false;
					for (Dialog dia : dialog.category.dialogs.values()) {
						if (dia.id != dialog.id && dia.title.equalsIgnoreCase(t.toString())) {
							has = true;
							break;
						}
					}
					if (has) { t.append("_"); }
				}
				dialog.title = t.toString();
				boolean b = !dialog.text.isEmpty();
				selectedDialog = Component.empty()
						.append(Component.literal("ID:" + dialog.id + " \"").withStyle(TextFormatting.GRAY))
						.append(Component.translatable(dialog.title).withStyle(TextFormatting.RESET))
						.append(Component.literal("\"").withStyle(TextFormatting.GRAY))
						.append(Component.literal(" (").withStyle(b ? TextFormatting.DARK_GREEN : TextFormatting.RED))
						.append(Component.translatable("quest.has." + b).withStyle(b ? TextFormatting.DARK_GREEN : TextFormatting.RED))
						.append(Component.literal(")").withStyle(b ? TextFormatting.DARK_GREEN : TextFormatting.RED));
				Packets.sendServer(new SPacketDialogSave(categoryData.get(selectedCategory).id, dialog.save(new NBTTagCompound())));
				initGui();
				break;
			} // paste
			case 10: {
				if (!dialogData.containsKey(selectedDialog)) { return; }
				copyDialog = dialogData.get(selectedDialog);
				initGui();
				break;
			} // copy
			case 11: {
				setSubGui(new SubGuiEditText(11, Component.translatable("gui.new").getString()));
				break;
			} // create dialog
			case 12: {
				if (!dialogData.containsKey(selectedDialog)) { return; }
				ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
					if (bo) {
						Packets.sendServer(new SPacketDialogRemove(dialogData.get(selectedDialog).id));
						selectedDialog = Component.empty();
					}
					NoppesUtil.openGUI(player, this);
				},
						dialogData.get(selectedDialog).getKey().getParent(),
						Component.translatable("message.delete").getParent());
				setScreen(guiYesNo);
				break;
			} // del dialog
			case 13: {
				if (!dialogData.containsKey(selectedDialog)) { return; }
				setSubGui(new SubGuiDialogEdit(npc, dialogData.get(selectedDialog), this));
				break;
			} // edit dialog
			case 14: {
				sortByName = ((GuiCheckBoxNop) button).selected();
				initGui();
				break;
			} // sort dialog list
		}
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (!(subgui instanceof SubGuiEditText) || !((SubGuiEditText)subgui).cancelled) {
			if (subgui instanceof SubGuiEditText) {
				SubGuiEditText dialogEdit = (SubGuiEditText) subgui;
				if (dialogEdit.id == 1) {
					DialogCategory category = new DialogCategory();
					StringBuilder t = new StringBuilder(((SubGuiEditText) subgui).text[0]);
					boolean has = true;
					while (has) {
						has = false;
						for (DialogCategory cat : DialogController.instance.categories.values()) {
							if (category.id != cat.id && cat.title.equalsIgnoreCase(t.toString())) {
								has = true;
								break;
							}
						}
						if (has) { t.append("_"); }
					}
					category.title = t.toString();
					Packets.sendServer(new SPacketDialogCategorySave(category.save(new NBTTagCompound())));
				}
				if (dialogEdit.id == 3) {
					if (dialogEdit.text[0].isEmpty() || !categoryData.containsKey(selectedCategory)) { return; }
					DialogCategory category = categoryData.get(selectedCategory).copy();
					if (category.title.equals(((SubGuiEditText) subgui).text[0])) { return; }
					category.title = ((SubGuiEditText) subgui).text[0];
					StringBuilder t = new StringBuilder(((SubGuiEditText) subgui).text[0]);
					boolean has = true;
					while (has) {
						has = false;
						for (DialogCategory cat : DialogController.instance.categories.values()) {
							if (category.id != cat.id && cat.title.equalsIgnoreCase(t.toString())) {
								has = true;
								break;
							}
						}
						if (has) { t.append("_"); }
					}
					category.title = t.toString();
					selectedCategory = Component.translatable(category.title);
					Packets.sendServer(new SPacketDialogCategorySave(category.save(new NBTTagCompound())));
					initGui();
				}
				if (dialogEdit.id == 11) {
					if (((SubGuiEditText) subgui).text[0].isEmpty()) { return; }
					Dialog dialog = new Dialog(categoryData.get(selectedCategory));
					StringBuilder t = new StringBuilder(((SubGuiEditText) subgui).text[0]);
					boolean has = true;
					while (has) {
						has = false;
						for (Dialog dia : dialog.category.dialogs.values()) {
							if (dia.id != dialog.id && dia.title.equalsIgnoreCase(t.toString())) {
								has = true;
								break;
							}
						}
						if (has) { t.append("_"); }
					}
					dialog.title = t.toString();
					boolean b = !dialog.text.isEmpty();
					selectedDialog = Component.empty()
							.append(Component.literal("ID:" + dialog.id + " \"").withStyle(TextFormatting.GRAY))
							.append(Component.translatable(dialog.title).withStyle(TextFormatting.RESET))
							.append(Component.literal("\"").withStyle(TextFormatting.GRAY))
							.append(Component.literal(" (").withStyle(b ? TextFormatting.DARK_GREEN : TextFormatting.RED))
							.append(Component.translatable("quest.has." + b).withStyle(b ? TextFormatting.DARK_GREEN : TextFormatting.RED))
							.append(Component.literal(")").withStyle(b ? TextFormatting.DARK_GREEN : TextFormatting.RED));
					Packets.sendServer(new SPacketDialogSave(categoryData.get(selectedCategory).id, dialog.save(new NBTTagCompound())));
					initGui();
				}
			}
			if (subgui instanceof SubGuiDialogEdit) { initGui(); }
		}
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (!scroll.hasSelected()) { return; }
		if (scroll.id == 0) {
			if (selectedCategory.getString().equals(scroll.getSelected())) { return; }
			selectedCategory = scroll.getNormalSelected();
			selectedDialog = Component.empty();
			scrollDialogs.clearSelection();
		}
		else if (scroll.id == 1) {
			if (selectedDialog.getString().equals(scroll.getSelected())) { return; }
			selectedDialog = scroll.getNormalSelected();
		}
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (!selectedDialog.getString().isEmpty() && scroll.id == 1) { setSubGui(new SubGuiDialogEdit(npc, dialogData.get(selectedDialog), this)); }
	}

	@Override
	public void save() { GuiTextFieldNop.unfocus(); }

	// New from Unofficial (BetaZavr)
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (!hasSubGui()) {
			drawHorizontalLine(guiLeft + 348, guiLeft + 414, guiTop + 128, 0x80000000);
		}
	}

	private static List<Map.Entry<Component, Dialog>> getEntryList(Map<Component, Dialog> map) {
		List<Map.Entry<Component, Dialog>> list = new ArrayList<>(map.entrySet());
		list.sort((d_0, d_1) -> {
			if (sortByName) {
				String n_0 = Util.instance.deleteColor(Component.translatable(d_0.getValue().title).getString() + "_" + d_0.getValue().id).toLowerCase();
				String n_1 = Util.instance.deleteColor(Component.translatable(d_1.getValue().title).getString() + "_" + d_1.getValue().id).toLowerCase();
				return n_0.compareTo(n_1);
			} else {
				return Integer.compare(d_0.getValue().id, d_1.getValue().id);
			}
		});
		return list;
	}

}
