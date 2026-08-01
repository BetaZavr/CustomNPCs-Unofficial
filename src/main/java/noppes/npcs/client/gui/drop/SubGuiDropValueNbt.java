package noppes.npcs.client.gui.drop;

import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.entity.data.DropNbtSet;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.ITextChangeListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import javax.annotation.Nonnull;

public class SubGuiDropValueNbt  extends GuiNPCInterface implements ITextfieldListener, ITextChangeListener {

	protected static final Object[] tagIds = new Object[] { "tag.type.0", "tag.type.1", "tag.type.2", "tag.type.3", "tag.type.4",
			"tag.type.5", "tag.type.6", "tag.type.7", "tag.type.8", "tag.type.9", "tag.type.11" };
	protected static final Object[] tagListIds = new Object[] { "tag.type.3", "tag.type.5", "tag.type.6", "tag.type.8", "tag.type.11" };
	protected GuiTextArea textarea;
	public DropNbtSet tag;

	public SubGuiDropValueNbt(DropNbtSet tagIn) {
		super();
		setBackground("companion_empty.png");
		closeOnEsc = true;
		imageWidth = 172;
		imageHeight = 167;

		tag = tagIn;
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		switch (button.id) {
			case 66: onClose(); break;
			case 90: {
				tag.setType(Integer.parseInt(String.valueOf(tagIds[button.getValue()]).replace("tag.type.", "")));
				initGui();
				break;
			} // type
			case 92: {
				tag.setTypeList(Integer.parseInt(String.valueOf(tagListIds[button.getValue()]).replace("tag.type.", "")));
				initGui();
				break;
			} // list type
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		int lId = 100;
		// type value
		int t = tag.getType();
		int tl = tag.getTypeList();
		// name / type
		String name = tag.getPath();
		if (name.contains(".")) {
			while (name.contains(".")) { name = name.substring(name.indexOf(".") + 1); }
		}
		addLabel(lId++, guiLeft + 4, guiTop + 5, Component.translatable("drop.tag.type",
				name,
				Component.translatable("tag.type." + t).getFormattedText()))
				.setHoverTexts("drop.hover.tag.name");
		// path
		addTextField(93, guiLeft + 4, guiTop + 18, 163, 20, tag.getPath())
				.setHoverTexts("drop.hover.tag.path");
		// value
		addLabel(lId++, guiLeft + 4, guiTop + 40, "type.value");
		String[] textArr = tag.getValues();
		StringBuilder text = new StringBuilder();
		if (textArr.length > 0) { text = new StringBuilder(textArr[0]); }
		if (textArr.length > 1) {
			for (int i = 1; i < textArr.length; i++) { text.append("|").append(textArr[i]); }
		}
		textarea = new GuiTextArea(94, guiLeft + 4, guiTop + 53, 163, 65, text.toString())
				.setListener(this);
		textarea.active = true;
		if (t == 7 || t == 11) { textarea.setHoverTexts("drop.hover.tag.value.array", name); }
		else if (t == 9) { textarea.setHoverTexts("drop.hover.tag.value.list", name); }
		else { textarea.setHoverTexts("drop.hover.tag.value.normal", name); }
		add(textarea);
		// chance
		addLabel(lId, guiLeft + 56, guiTop + 125, "drop.chance");
		addTextField(95, guiLeft + 4, guiTop + 120, 50, 20, String.valueOf(tag.getChance()))
				.setMinMaxDefault(0.0001d, 100.0d, tag.getChance())
				.setHoverTexts("drop.hover.tag.chance");
		// type
		int posId = 0;
		for (int i = 0; i < tagIds.length; i++) {
			if (tagIds[i].equals("tag.type." + tag.getType())) { posId = i; }
		}
		name = ((char) 167) + "2" + name;
		addButton(90, guiLeft + 87, guiTop + 120, true, posId, tagIds)
				.setSize(80, 20)
				.setHoverTexts("drop.hover.tag.type", name, getValuesData(t));
		// list type
		int posListId = 0;
		for (int i = 0; i < tagListIds.length; i++) {
			if (tagListIds[i].equals("tag.type." + tag.getTypeList())) { posListId = i; }
		}
		addButton(92, guiLeft + 87, guiTop + 142, true, posListId, tagListIds)
				.setSize(80, 20)
				.setIsVisible(t == 9)
				.setHoverTexts("drop.hover.tag.listtype", name, getValuesData(tl));
		// done
		addButton(66, guiLeft + 4, guiTop + 142, "gui.done")
				.setSize(80, 20)
				.setIsEnabled(check())
				.setHoverTexts("hover.back");
	}

	@Override
	public void textUpdate(IComponentGui component, String text) { tag.setValues(text); }

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (textField.id == 93) { tag.setPath(textField.getValue()); } // path
		else if (textField.id == 95) { tag.setChance(textField.getDouble()); } // chance
		initGui();
	}

	private Component getValuesData(int t) {
		TextFormatting gn = TextFormatting.DARK_GREEN;
		TextFormatting r = TextFormatting.RED;
		TextFormatting gr = TextFormatting.GRAY;
		switch (t) {
			case 1: return Component.empty().append(Component.literal("" + Byte.MIN_VALUE).withStyle(gn))
					.append(Component.literal("<->").withStyle(gr))
					.append(Component.literal("" + Byte.MAX_VALUE).withStyle(r));
			case 2: return Component.empty().append(Component.literal("" + Short.MIN_VALUE).withStyle(gn))
					.append(Component.literal("<->").withStyle(gr))
					.append(Component.literal("" + Short.MAX_VALUE).withStyle(r));
			case 3: return Component.empty().append(Component.literal("" + Integer.MIN_VALUE).withStyle(gn))
					.append(Component.literal("<->").withStyle(gr))
					.append(Component.literal("" + Integer.MAX_VALUE).withStyle(r));
			case 4: return Component.empty().append(Component.literal("" + Long.MIN_VALUE).withStyle(gn))
					.append(Component.literal("<->").withStyle(gr))
					.append(Component.literal("" + Long.MAX_VALUE).withStyle(r));
			case 5: return Component.translatable("type.double", "77");
			case 6: return Component.translatable("type.double", "308");
			case 7: return Component.empty().append(Component.literal("array [v0, v1, ... vn] v_ = "))
					.append(Component.literal("" + Byte.MIN_VALUE).withStyle(gn))
					.append(Component.literal("<->").withStyle(gr))
					.append(Component.literal("" + Byte.MAX_VALUE).withStyle(r));
			case 8: return Component.translatable("type.string");
			case 9: return Component.translatable("type.list");
			case 11: return Component.empty().append(Component.literal("array [v0, v1, ... vn] v_ = "))
					.append(Component.literal("" + Integer.MIN_VALUE).withStyle(gn))
					.append(Component.literal("<->").withStyle(gr))
					.append(Component.literal("" + Integer.MAX_VALUE).withStyle(r));
			default: return Component.empty().append(Component.literal("true").withStyle(gn))
					.append(Component.literal(", ").withStyle(gr))
					.append(Component.literal("false").withStyle(r));
		}
	}

	private boolean check() {
		if (getTextField(93) == null || textarea == null) { return false; }
		if (getTextField(93).getValue().isEmpty() || textarea.getText().isEmpty()) { return false; }
		String vs = textarea.getText();
		if (vs.contains("|")) {
			for (String str : vs.split("\\|")) {
				String ch = tag.checkValue(str, tag.getType());
				if (ch == null) { return false; }
			}
		}
		else {
			String ch = tag.checkValue(vs, tag.getType());
			if (ch == null) { return false; }
		}
		return true;
	}

}
