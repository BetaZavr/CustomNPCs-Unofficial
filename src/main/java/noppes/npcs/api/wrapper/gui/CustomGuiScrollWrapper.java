package noppes.npcs.api.wrapper.gui;

import java.util.Arrays;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.api.functions.gui.GuiComponentClicked;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IScroll;

public class CustomGuiScrollWrapper extends CustomGuiComponentWrapper implements IScroll {

	protected int[] selection = new int[0];
	protected String[] list;
	protected boolean multiSelect = false;
	protected boolean hasSearch = true;
	protected GuiComponentClicked<IScroll> onClick;
	protected GuiComponentClicked<IScroll> onDoubleClick;

	public CustomGuiScrollWrapper() { }

	public CustomGuiScrollWrapper(int id, int x, int y, int width, int height, String[] list) {
		setId(id);
		setPos(x, y);
		setSize(width, height);
		setList(list);
	}

	@Override
	public String[] getList() { return list; }

	@Override
	public CustomGuiScrollWrapper setList(String[] listIn) {
		list = listIn;
		return this;
	}

	@Override
	public int getDefaultSelection() {
		if (selection.length == 0) { return -1; }
		if (selection.length > 1) {
			throw new CustomNPCsException("You have multiple selections, use getSelection instead");
		}
		return selection[0];
	}

	@Override
	public CustomGuiScrollWrapper setDefaultSelection(int selectionIn) {
		selection = new int[] {selectionIn};
		return this;
	}

	@Override
	public int[] getSelection() { return selection; }

	@Override
	public CustomGuiScrollWrapper setSelection(int... selections) {
		if (selections == null) { selections = new int[0]; }
		selection = selections;
		return this;
	}

	@Override
	public String[] getSelectionList() {
		return selection.length == 0 ? new String[0] : Arrays.stream(selection)
				.filter((i) -> i >= 0 && i < list.length)
				.mapToObj((i) -> list[i])
				.toArray(String[]::new);
	}

	@Override
	public CustomGuiScrollWrapper setSelectionList(String... list) {
		selection = Arrays.stream(list).mapToInt(s -> Arrays.asList(list).indexOf(s)).toArray();
		return this;
	}

	@Override
	public boolean isMultiSelect() { return multiSelect; }

	@Override
	public CustomGuiScrollWrapper setMultiSelect(boolean multiSelectIn) {
		multiSelect = multiSelectIn;
		return this;
	}

	@Override
	public int getType() { return GuiComponentType.SCROLL.get(); }

	@Override
	public NBTTagCompound toNBT(NBTTagCompound compound) {
		super.toNBT(compound);
		compound.setIntArray("selection", selection);
		NBTTagList listTag = new NBTTagList();
		if (list != null) {
			for (String line : list) { listTag.appendTag(new NBTTagString(line == null ? "" : line)); }
		}
		compound.setTag("list", listTag);
		compound.setBoolean("multiSelect", multiSelect);
		compound.setBoolean("hasSearch", hasSearch);
		return compound;
	}

	@Override
	public CustomGuiComponentWrapper fromNBT(NBTTagCompound compound) {
		super.fromNBT(compound);
		setSelection(compound.getIntArray("selection"));
		NBTTagList list = compound.getTagList("list", 8);
		String[] values = new String[list.tagCount()];
		for (int i = 0; i < list.tagCount(); i++) { values[i] = list.getStringTagAt(i); }
		setList(values);
		setMultiSelect(compound.getBoolean("multiSelect"));
		setHasSearch(compound.getBoolean("hasSearch"));
		return this;
	}

	@Override
	public CustomGuiScrollWrapper setOnClick(GuiComponentClicked<IScroll> onClickIn) {
		onClick = onClickIn;
		return this;
	}

	@Override
	public CustomGuiScrollWrapper setOnDoubleClick(GuiComponentClicked<IScroll> onDoubleClickIn) {
		onDoubleClick = onDoubleClickIn;
		return this;
	}

	@Override
	public boolean getHasSearch() {
		return hasSearch;
	}

	@Override
	public CustomGuiScrollWrapper setHasSearch(boolean bo) {
		hasSearch = bo;
		return this;
	}

	public final void onClick(ICustomGui gui) {
		if (onClick != null) { onClick.onClick(gui, this); }
	}

	public final void onDoubleClick(ICustomGui gui) {
		if (onDoubleClick != null) { onDoubleClick.onClick(gui, this); }
	}

}
