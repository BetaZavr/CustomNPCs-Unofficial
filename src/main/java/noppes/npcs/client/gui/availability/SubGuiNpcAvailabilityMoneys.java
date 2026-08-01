package noppes.npcs.client.gui.availability;

import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.constants.EnumAvailabilityMoney;
import noppes.npcs.constants.EnumAvailabilityScoreboard;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.AvailabilityMoneyData;

public class SubGuiNpcAvailabilityMoneys extends GuiNPCInterface implements ITextfieldListener  {

    protected final Availability availability;

    public SubGuiNpcAvailabilityMoneys(Availability availabilityIn) {
        super();
        setBackground("smallbg.png");
        imageWidth = 176;
        imageHeight = 122;
        closeOnEsc = true;

        availability = availabilityIn;
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        switch (button.id) {
            case 0: {
                AvailabilityMoneyData data;
                if (!availability.moneys.containsKey(EnumAvailabilityMoney.MONEY)) { availability.moneys.put(EnumAvailabilityMoney.MONEY,
                        data = new AvailabilityMoneyData(0, EnumAvailabilityScoreboard.BIGGER)); }
                else { data = availability.moneys.get(EnumAvailabilityMoney.MONEY); }
                data.type = EnumAvailabilityScoreboard.values()[button.getValue()];
                initGui();
                break;
            }
            case 1: {
                AvailabilityMoneyData data;
                if (!availability.moneys.containsKey(EnumAvailabilityMoney.DONAT)) { availability.moneys.put(EnumAvailabilityMoney.DONAT,
                        data = new AvailabilityMoneyData(0, EnumAvailabilityScoreboard.BIGGER)); }
                else { data = availability.moneys.get(EnumAvailabilityMoney.DONAT); }
                data.type = EnumAvailabilityScoreboard.values()[button.getValue()];
                initGui();
                break;
            }
            case 66: onClose(); break;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        int x0 = guiLeft + 6;
        int x1 = x0 + 114;
        int y = guiTop + 4;
        int lId = 0;
        AvailabilityMoneyData dataM = availability.moneys.get(EnumAvailabilityMoney.MONEY);
        AvailabilityMoneyData dataD = availability.moneys.get(EnumAvailabilityMoney.DONAT);
        Object[] types = new Object[EnumAvailabilityScoreboard.values().length];
        int m = 0;
        int d = 0;
        int i = 0;
        for (EnumAvailabilityScoreboard eas : EnumAvailabilityScoreboard.values()) {
            if (dataM != null && dataM.type == eas) { m = i; }
            if (dataD != null && dataD.type == eas) { d = i; }
            types[i++] = "availability." + eas.name().toLowerCase();
        }
        // title
        addLabel(lId++, x0 - 2, y, "availability.available.9")
                .setSize(imageWidth - 8, 12)
                .setCenter(imageWidth - 12);
        // money
        addLabel(lId++, x0, y += 16, Component.translatable("gui.money").append(":"))
                .setSize(imageWidth - 12, 12);
        addTextField(0, x0, (y += 12) + 1, 110, 18, dataM != null ? dataM.value : "")
                .setMinMaxDefault(-1, Integer.MAX_VALUE, dataM != null ? dataM.value : 0)
                .setHoverTexts("availability.hover.money.value");
        addButton(0, x1, y, false, m, types)
                .setSize(50, 20)
                .setHoverTexts("availability.hover.money.type");
        // donat
        addLabel(lId, x0, y += 24, Component.translatable("gui.donat").append(":"))
                .setSize(imageWidth - 12, 12);
        addTextField(1, x0, (y += 12) + 1, 110, 18, dataD != null ? dataD.value : "")
                .setMinMaxDefault(-1, Integer.MAX_VALUE, dataD != null ? dataD.value : 0)
                .setHoverTexts("availability.hover.donat.value");
        addButton(1, x1, y, false, d, types)
                .setSize(50, 20)
                .setHoverTexts("availability.hover.money.type");
        // exit
        addButton(66, x0, guiTop + imageHeight - 26, "gui.back")
                .setSize(80, 20)
                .setHoverTexts("hover.back");
    }

    @Override
    public void unFocused(GuiTextFieldNop textField) {
        EnumAvailabilityMoney type = textField.id == 1 ? EnumAvailabilityMoney.DONAT : EnumAvailabilityMoney.MONEY;
        if (textField.getValue().isEmpty() || textField.getInteger() < 0) { availability.moneys.remove(type); }
        else {
            AvailabilityMoneyData data;
            if (!availability.moneys.containsKey(type)) { availability.moneys.put(type,
                    data = new AvailabilityMoneyData(0, EnumAvailabilityScoreboard.BIGGER)); }
            else { data = availability.moneys.get(type); }
            data.value = textField.getInteger();
        }
        initGui();
    }

}
