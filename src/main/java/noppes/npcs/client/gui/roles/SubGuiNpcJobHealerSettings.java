package noppes.npcs.client.gui.roles;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.roles.data.HealerSettings;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class SubGuiNpcJobHealerSettings
        extends GuiNPCInterface
        implements ITextfieldListener {

    public HealerSettings healerSettings;

    public SubGuiNpcJobHealerSettings(HealerSettings settings) {
        super();
        setBackground("menubg.png");
        imageWidth = 171;
        imageHeight = 217;
        closeOnEsc = true;

        healerSettings = settings;
    }

    @Override
    public void init() {
        super.init();
        int x0 = guiLeft + 5;
        int x1 = guiLeft + 88;
        int x2 = guiLeft + 123;
        int y = 5;
        addLabel(1, x0, guiTop + y + 5, "beacon.range")
                .setSize(121, 12);
        addTextField(1, x2, guiTop + y, 45, 20, healerSettings.range)
                .setMinMaxDefault(1, 64, 16)
                .setHoverTexts("beacon.hover.dist");
        y += 24;
        addLabel(2, x0, guiTop + y + 5, "stats.speed")
                .setSize(121, 12);
        addTextField(2, x2, guiTop + y, 45, 20, healerSettings.speed)
                .setMinMaxDefault(10, 72000, 20)
                .setHoverTexts("beacon.hover.speed");
        y += 24;
        addLabel(3, x0, guiTop + y + 5, "beacon.amplifier")
                .setSize(86, 12);
        String lv = "enchantment.level." + (healerSettings.amplifier + 1);
        if (!Component.translatable(lv).getString().equals(lv)) { lv = Component.translatable(lv).getString(); }
        else { lv = "" + (healerSettings.amplifier + 1); }
        addTextField(3, x2, guiTop + y, 45, 20, healerSettings.amplifier + 1)
                .setMinMaxDefault(1, 10, healerSettings.amplifier + 1)
                .setHoverTexts(Component.translatable("beacon.hover.power", lv));
        y += 24;
        addLabel(4, x0, guiTop + y + 5, "gui.time")
                .setSize(86, 12);
        addTextField(4, x2, guiTop + y, 45, 20, healerSettings.time)
                .setMinMaxDefault(1, 72000, 1)
                .setHoverTexts("beacon.hover.time");
        y += 24;
        addLabel(5, x0, guiTop + y + 5, "beacon.affect")
                .setSize(121, 12);
        addButton(1, x1, guiTop + y, false, healerSettings.type, "faction.friendly", "faction.unfriendly", "spawner.all")
                .setSize(80, 20)
                .setHoverTexts("beacon.hover.type");
        y += 24;
        addLabel(6, x0, guiTop + y + 5, "beacon.applicability")
                .setSize(121, 12);
        addButton(2, x1, guiTop + y, false, healerSettings.isMassive ? 0 : 1, "beacon.massive", "beacon.not.massive")
                .setSize(80, 20)
                .setHoverTexts("beacon.hover.massive");
        y += 24;
        addCheckBox(3, x0, guiTop + y, "beacon.on.him.self", null, healerSettings.onHimself)
                .setSize(168, 15)
                .setHoverTexts("beacon.hover.on.him.self");
        y += 17;
        addCheckBox(4, x0, guiTop + y, "beacon.on.mobs", null, healerSettings.possibleOnMobs)
                .setSize(168, 15)
                .setHoverTexts("beacon.hover.on.mobs");
        addButton(66, guiLeft + 61, guiTop + imageHeight - 24, "gui.done")
                .setSize(45, 20)
                .setHoverTexts("hover.back");
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        switch (button.id) {
            case 1: healerSettings.type = (byte) button.getValue(); break;
            case 2: healerSettings.isMassive = button.getValue() == 0; break;
            case 3: healerSettings.onHimself = ((GuiCheckBoxNop) button).selected(); break;
            case 4: healerSettings.possibleOnMobs = ((GuiCheckBoxNop) button).selected(); break;
            case 66: onClose(); break;
        }
    }

    @Override
    public boolean keyPressed(int key, int key_1, int key_2) {
        boolean bo = super.keyPressed(key, key_1, key_2);
        if (key == InputConstants.KEY_ESCAPE) {
            onClose();
            bo = true;
        }
        return bo;
    }

    @Override
    public void unFocused(GuiTextFieldNop textField) {
        switch (textField.id) {
            case 1: healerSettings.range = textField.getInteger(); break;
            case 2: healerSettings.speed = textField.getInteger(); break;
            case 3: {
                healerSettings.amplifier = textField.getInteger() - 1;
                String lv = "enchantment.level." + (healerSettings.amplifier + 1);
                if (!Component.translatable(lv).getString().equals(lv)) { lv = Component.translatable(lv).getString(); }
                else { lv = "" + (healerSettings.amplifier + 1); }
                textField.setHoverTexts("beacon.hover.power", lv);
                break;
            }
            case 4: healerSettings.time = textField.getInteger(); break;
        }
    }

}
