package noppes.npcs.client.gui.drop;

import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.entity.data.AttributeSet;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import javax.annotation.Nonnull;

public class SubGuiDropAttribute extends GuiNPCInterface implements ITextfieldListener {

    protected double[] values;
    public AttributeSet attribute;

    public SubGuiDropAttribute(AttributeSet attributeIn) {
        super();
        setBackground("companion_empty.png");
        closeOnEsc = true;
        imageWidth = 130;
        imageHeight = 122;

        attribute = attributeIn;
    }

    @Override
    public void buttonEvent(@Nonnull GuiButtonNop button) {
        switch (button.id) {
            case 70: attribute.setSlot(button.getValue() - 1); init(); break;
            case 66: onClose(); break;
        }
    }

    @Override
    public void init() {
        super.init();
        int lId = 80;
        int x = guiLeft + 5;
        int y = guiTop + 15;
        // name
        addLabel(lId++, x, y - 10, Component.translatable("gui.name").append(":"));
        addTextField(72, x + 1, y, 118, 14, attribute.getAttribute())
                .setHoverTexts(Component.translatable("drop.hover.attribute.name",
                        Component.translatable("attribute.name." + attribute.getAttribute()).getString()));
        // values
        values = new double[] { attribute.getMinValue(), attribute.getMaxValue() };
        addLabel(lId++, x, (y += 18) + 8, "type.value")
                .setSize(33, 10);
        addLabel(lId++, x + 35, y + 2, Component.translatable("gui.min").append(":"))
                .setSize(31, 10);
        addLabel(lId++, x + 35, y + 16, Component.translatable("gui.max").append(":"))
                .setSize(31, 10);

        String tied = Component.translatable("drop.tied.random").getString();
        if (attribute.parent.tiedToLevel) { tied = Component.translatable("drop.tied.level").getString(); }
        // min
        addTextField(73, x + 69, y, 50, 14, "" + values[0])
                .setMinMaxDefault(-4096.0d, 4096.0d, attribute.getMinValue())
                .setHoverTexts("drop.hover.attribute.values", tied);
        // max
        addTextField(74, x + 69, y += 15, 50, 14, "" + values[1])
                .setMinMaxDefault(-4096.0d, 4096.0d, attribute.getMaxValue())
                .setHoverTexts("drop.hover.attribute.values", tied);
        // slot
        Object[] slots = new String[7];
        for (int i = 0; i < 7; i++) { slots[i] = "attribute.slot." + i; }
        addLabel(lId++, x + 35, (y += 17) + 2, Component.translatable("drop.slot").append(":"))
                .setSize(31, 10);
        addButton(70, x + 68, y, false, attribute.slot + 1, slots)
                .setSize(52, 16)
                .setHoverTexts("drop.hover.attribute.slot");
        // chance
        addLabel(lId, x + 35, (y += 19) + 2, Component.translatable("drop.chance").append(":"));
        addTextField(75, x + 69, y, 50, 14, String.valueOf(attribute.getChance()))
                .setMinMaxDefault(0.0001d, 100.0d, attribute.getChance())
                .setHoverTexts("drop.hover.attribute.chance");
        // done
        addButton(66, x + 19, y + 17, "gui.done")
                .setSize(80, 16)
                .setIsEnabled(check())
                .setHoverTexts("hover.back");
    }

    @Override
    public void unFocused(GuiTextFieldNop textField) {
        switch (textField.id) {
            case 72: attribute.setAttribute(textField.getValue()); init(); break; // name
            case 73: values[0] = textField.getDouble(); attribute.setValues(values[0], values[1]); break; // value min
            case 74: values[1] = textField.getDouble(); attribute.setValues(values[0], values[1]); break; // value max
            case 75: attribute.setChance(textField.getDouble()); init(); break; // chance
        }
    }

    private boolean check() {
        return getTextField(72) != null && !getTextField(72).getValue().isEmpty();
    }

}
