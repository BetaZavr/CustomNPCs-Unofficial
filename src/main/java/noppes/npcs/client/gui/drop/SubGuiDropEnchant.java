package noppes.npcs.client.gui.drop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.entity.data.EnchantSet;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;
import java.util.*;

public class SubGuiDropEnchant extends GuiNPCInterface implements ITextfieldListener {

    protected final Map<Component, Integer> enchIds = new TreeMap<>(Comparator.comparing(Component::getString));
    protected int[] levels;
    public EnchantSet enchant;

    @SuppressWarnings("deprecation")
    public SubGuiDropEnchant(EnchantSet enchantIn) {
        super();
        setBackground("companion_empty.png");
        closeOnEsc = true;
        imageWidth = 172;
        imageHeight = 167;

        enchant = enchantIn;
        for (Map.Entry<ResourceKey<Enchantment>, Enchantment> entry : BuiltInRegistries.ENCHANTMENT.entrySet()) {
            enchIds.put(Component.translatable(entry.getValue().getDescriptionId()),
                    BuiltInRegistries.ENCHANTMENT.getId(entry.getValue()));
        }
    }

    @Override
    public void buttonEvent(@Nonnull GuiButtonNop button) {
        switch (button.id) {
            case 50: {
                if (enchIds.containsKey(button.getVariants()[button.getValue()])) {
                    enchant.setEnchant(enchIds.get(button.getVariants()[button.getValue()]));
                    init();
                }
                break;
            }
            case 66: onClose(); break;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void init() {
        super.init();
        int lId = 60;
        int x = guiLeft + 5;
        int y = guiTop + 5;
        // select
        int posId = 0;
        int enchantmentId = BuiltInRegistries.ENCHANTMENT.getId(enchant.ench);
        Object[] es = enchIds.keySet().toArray();
        for (int i = 0; i < es.length; i++) {
            if (enchIds.get((Component) es[i]) == enchantmentId) {
                posId = i;
                break;
            }
        }
        addButton(50, x, y, true, posId, es)
                .setSize(120, 16)
                .setHoverTexts(Component.translatable("drop.hover.enchant.list", enchantmentId));
        x += 1;
        // levels
        levels = new int[] { enchant.getMinLevel(), enchant.getMaxLevel() };
        addLabel(lId++, x, (y += 18) + 8, "type.level")
                .setSize(33, 10);
        addLabel(lId++, x + 35, y + 2, Component.translatable("gui.min").append(":"))
                .setSize(31, 10);
        addLabel(lId++, x + 35, y + 16, Component.translatable("gui.max").append(":"))
                .setSize(31, 10);
        String tied = Component.translatable("drop.tied.random").getString();
        if (enchant.parent.tiedToLevel) { tied = Component.translatable("drop.tied.level").getString(); }
        // min
        addTextField(52, x + 68, y, 50, 14, "" + levels[0])
                .setMinMaxDefault(0, 100000, enchant.getMinLevel())
                .setHoverTexts(Component.translatable("drop.hover.enchant.levels", "" + enchant.ench.getMaxLevel(), tied));
        // max
        addTextField(53, x + 68, y += 16, 50, 14, "" + levels[1])
                .setMinMaxDefault(0, 100000, enchant.getMaxLevel())
                .setHoverTexts(Component.translatable("drop.hover.enchant.levels", "" + enchant.ench.getMaxLevel(), tied));
        // chance
        addLabel(lId, x + 35, (y += 18) + 2, Component.translatable("drop.chance").append(":"))
                .setSize(31, 10);
        addTextField(54, x + 68, y, 50, 14, String.valueOf(enchant.getChance()))
                .setMinMaxDefault(0.0001d, 100.0d, enchant.getChance())
                .setHoverTexts("drop.hover.enchant.chance");
        // done
        addButton(66, x + 19, y + 17, "gui.done")
                .setSize(80, 16)
                .setHoverTexts("hover.back");
    }

    @Override
    public void unFocused(GuiTextFieldNop textField) {
        switch (textField.id) {
            case 52: levels[0] = textField.getInteger(); enchant.setLevels(levels[0], levels[1]); init(); break; // level min
            case 53: levels[1] = textField.getInteger(); enchant.setLevels(levels[0], levels[1]); init(); break; // level max
            case 54: enchant.setChance(textField.getDouble()); init(); break; // chance
        }
    }

}
