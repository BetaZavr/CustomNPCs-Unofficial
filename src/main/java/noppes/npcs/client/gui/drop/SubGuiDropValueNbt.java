package noppes.npcs.client.gui.drop;

import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.entity.data.DropNbtSet;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.ITextChangeListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import javax.annotation.Nonnull;

public class SubGuiDropValueNbt  extends GuiNPCInterface implements ITextfieldListener, ITextChangeListener {

    protected static final Object[] tagIds = new Object[] { "tag.type.0", "tag.type.1", "tag.type.2", "tag.type.3", "tag.type.4",
            "tag.type.5", "tag.type.6", "tag.type.7", "tag.type.8", "tag.type.9", "tag.type.11", "tag.type.12" };
    protected static final Object[] tagListIds = new Object[] { "tag.type.0", "tag.type.1", "tag.type.2", "tag.type.3", "tag.type.4",
            "tag.type.5", "tag.type.6", "tag.type.7", "tag.type.8", "tag.type.11", "tag.type.12" };
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
                int type = button.getValue();
                if (type > 9) { type++; }
                tag.setType(type);
                init();
                break;
            } // type
            case 92: {
                int type = button.getValue();
                if (type > 8) { type += 2; }
                tag.setTypeList(type);
                init();
                break;
            } // list type
        }
    }

    @Override
    public void init() {
        super.init();
        int lId = 100;
        // type value
        int t = tag.getType();
        int x = guiLeft + 5;
        int y = guiTop + 15;
        // name / type
        String name = tag.getPath();
        if (name.contains(".")) { name = name.substring(name.lastIndexOf(".") + 1); }
        if (name.isEmpty()) { name = "null"; }
        name = ((char) 167) + "2" + name;
        addLabel(lId++, x, y - 10, Component.translatable("drop.tag.type",
                name,
                Component.translatable("tag.type." + t).getString()))
                .setSize(162, 10)
                .setHoverTexts("drop.hover.tag.name");
        // path
        addTextField(93, x, y, 163, 14, tag.getPath())
                .setHoverTexts("drop.hover.tag.path");
        // value
        addLabel(lId++, x, (y += 26) - 10, "type.value");
        String[] textArr = tag.getValues();
        StringBuilder text = new StringBuilder();
        if (textArr.length > 0) { text = new StringBuilder(textArr[0]); }
        if (textArr.length > 1) {
            for (int i = 1; i < textArr.length; i++) { text.append("|").append(textArr[i]); }
        }
        textarea = new GuiTextArea(94, x, y, 163, 84, text.toString())
                .setListener(this)
                .setHoverTexts(Component.translatable("drop.hover.tag.value", name)
                        .append(Component.translatable("drop.hover.tag.value." + t)));
        textarea.active = true;
        y += textarea.getHeight() + 3;

        add(textarea);
        // chance
        addLabel(lId, x, y + 2, Component.translatable("drop.chance").append(":"))
                .setSize(27, 10);
        addTextField(95, x + 29, y + 1, 50, 14, String.valueOf(tag.getChance()))
                .setMinMaxDefault(0.0001d, 100.0d, tag.getChance())
                .setHoverTexts("drop.hover.tag.chance");
        // type
        int posId = 0;
        for (int i = 0; i < tagIds.length; i++) {
            if (tagIds[i].equals("tag.type." + tag.getType())) { posId = i; }
        }
        addButton(90, x + 82, y, true, posId, tagIds)
                .setSize(80, 16)
                .setHoverTexts(Component.translatable("drop.hover.tag.type", name));
        // list type
        int posListId = 0;
        for (int i = 0; i < tagListIds.length; i++) {
            if (tagListIds[i].equals("tag.type." + tag.getTypeList())) { posListId = i; }
        }
        addButton(92, x + 82, y += 18, true, posListId, tagListIds)
                .setSize(80, 16)
                .setIsVisible(t == 9)
                .setHoverTexts(Component.translatable("drop.hover.tag.listtype", name));
        // done
        addButton(66, x, y, "gui.done")
                .setSize(80, 16)
                .setIsEnabled(check())
                .setHoverTexts("hover.back");
    }

    @Override
    public void textUpdate(IComponentGui component, String text) { tag.setValues(text); }

    @Override
    public void unFocused(GuiTextFieldNop textField) {
        if (textField.id == 93) { tag.setPath(textField.getValue()); } // path
        else if (textField.id == 95) { tag.setChance(textField.getDouble()); } // chance
        init();
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
            if (ch != null) { return true; }
        }
        return true;
    }

}
