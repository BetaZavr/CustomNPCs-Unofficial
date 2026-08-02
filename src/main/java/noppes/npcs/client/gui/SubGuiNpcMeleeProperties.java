package noppes.npcs.client.gui;

import net.minecraft.init.Enchantments;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import noppes.npcs.api.constants.PotionEffectType;
import noppes.npcs.entity.data.DataMelee;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import java.util.ArrayList;
import java.util.List;

public class SubGuiNpcMeleeProperties extends GuiBasic implements ITextfieldListener {

	protected static final Object[] potionNames;
	protected final DataMelee stats;

	static {
		List<String> list = new ArrayList<>();
		list.add("gui.none");
		for(PotionEffectType ept : PotionEffectType.values()) {
			Potion pt = PotionEffectType.getMCType(ept.get());
			if (pt != null && pt.getRegistryName() != null) { list.add(pt.getName()); }
		}
		list.add(Blocks.FIRE.getUnlocalizedName() + ".name");
		potionNames = list.toArray(new String[0]);
	}

	public SubGuiNpcMeleeProperties(DataMelee statsIn) {
		super();
		setBackground("menubg.png");
		imageWidth = 256;
		imageHeight = 216;

		stats = statsIn;
	}

	@Override
	public void initGui() {
		super.initGui();
		// power
		addLabel(1, guiLeft + 5, guiTop + 15, "stats.meleestrength");
		addTextField(1, guiLeft + 105, guiTop + 10, 100, 18, stats.getStrength())
				.setMinMaxDefault(0, Integer.MAX_VALUE, 5)
				.setHoverTexts("stats.hover.attack.strength");
		// range
		addLabel(2, guiLeft + 5, guiTop + 45, "stats.meleerange");
		addTextField(2, guiLeft + 105, guiTop + 40, 100, 18, stats.getRange())
				.setMinMaxDefault(1, 30, 2)
				.setHoverTexts("stats.hover.attack.range");
		// speed
		addLabel(3, guiLeft + 5, guiTop + 75, "stats.meleespeed");
		addTextField(3, guiLeft + 105, guiTop + 70, 100, 18, stats.getDelay())
				.setMinMaxDefault(1, 1000, 20)
				.setHoverTexts("stats.hover.attack.speed");
		// knockback
		addLabel(4, guiLeft + 5, guiTop + 105, Enchantments.KNOCKBACK.getName());
		addTextField(4, guiLeft + 105, guiTop + 100, 100, 18, stats.getKnockback())
				.setMinMaxDefault(0, 4, 0)
				.setHoverTexts("stats.hover.attack.knockback");
		// effect
		addLabel(5, guiLeft + 5, guiTop + 135, "stats.meleeeffect");
		int effect = stats.getEffectType();
		addButton(5, guiLeft + 85, guiTop + 130, true, effect, potionNames)
				.setSize(100, 20)
				.setHoverTexts("stats.hover.attack.effects");
		if (stats.getEffectType() != 0) {
			addLabel(6, guiLeft + 5, guiTop + 165, "gui.time");
			addTextField(6, guiLeft + 85, guiTop + 160, 50, 18, stats.getEffectTime())
					.setMinMaxDefault(1, 99999, 5)
					.setHoverTexts("stats.hover.attack.effect");
			if (stats.getEffectType() != 1) {
				addLabel(7, guiLeft + 5, guiTop + 195, "stats.amplify");
				Object[] numbs = new Object[11];
				for (int i = 0; i < 11; i++) { numbs[i] = i; }
				addButton(7, guiLeft + 85, guiTop + 190, true, stats.getEffectStrength(), numbs)
						.setSize(52, 20)
						.setHoverTexts("stats.hover.effect.power");
			}
		}
		addButton(66, guiLeft + 164, guiTop + 192, "gui.done")
				.setSize(90, 20)
				.setHoverTexts("hover.back");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 5: {
				stats.setEffect(button.getValue(), stats.getEffectStrength(), stats.getEffectTime());
				initGui();
				break;
			}
			case 7: stats.setEffect(stats.getEffectType(), button.getValue(), stats.getEffectTime()); break;
			case 66: onClose(); break;
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 1: stats.setStrength(textField.getInteger());break;
			case 2: stats.setRange(textField.getInteger()); break;
			case 3: stats.setDelay(textField.getInteger()); break;
			case 4: stats.setKnockback(textField.getInteger()); break;
			case 6: stats.setEffect(stats.getEffectType(), stats.getEffectStrength(), textField.getInteger()); break;
		}
	}

}
