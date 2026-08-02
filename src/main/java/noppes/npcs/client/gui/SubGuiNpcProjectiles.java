package noppes.npcs.client.gui;

import net.minecraft.init.Enchantments;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import noppes.npcs.api.constants.PotionEffectType;
import noppes.npcs.entity.data.DataRanged;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import java.util.ArrayList;
import java.util.List;

public class SubGuiNpcProjectiles extends GuiBasic implements ITextfieldListener {

	protected static final Object[] potionNames;
	protected final Object[] trailNames = new Object[] { "gui.none", "Smoke", "Portal", "Redstone", "Lightning", "LargeSmoke", "Magic", "Enchant" };
	protected final DataRanged stats;

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

	public SubGuiNpcProjectiles(DataRanged statsIn) {
		super();
		setBackground("menubg.png");
		imageWidth = 256;
		imageHeight = 216;

		stats = statsIn;
	}

	@Override
	public void initGui() {
		super.initGui();
		// attack strength / arrow damage
		addLabel(1, guiLeft + 5, guiTop + 15, MobEffects.STRENGTH.getName());
		addTextField(1, guiLeft + 45, guiTop + 10, 50, 18, stats.getStrength())
				.setMinMaxDefault(0, Integer.MAX_VALUE, 5)
				.setHoverTexts("stats.hover.attack.strength");
		// arrow knockback
		addLabel(2, guiLeft + 110, guiTop + 15, Enchantments.KNOCKBACK.getName());
		addTextField(2, guiLeft + 150, guiTop + 10, 50, 18, stats.getKnockback())
				.setMinMaxDefault(0, 3, 0)
				.setHoverTexts("stats.hover.attack.knockback");
		// arrow size
		addLabel(3, guiLeft + 5, guiTop + 45, "stats.size");
		addTextField(3, guiLeft + 45, guiTop + 40, 50, 18, stats.getSize())
				.setMinMaxDefault(5, 20, 10)
				.setHoverTexts("stats.hover.bullet.size");
		addLabel(4, guiLeft + 5, guiTop + 75, "stats.speed");
		addTextField(4, guiLeft + 45, guiTop + 70, 50, 18, stats.getSpeed())
				.setMinMaxDefault(1, 50, 10)
				.setHoverTexts("stats.hover.bullet.speed");
		// hasgravity
		addLabel(5, guiLeft + 5, guiTop + 105, "stats.hasgravity");
		addYesNo(0, guiLeft + 60, guiTop + 100, stats.getHasGravity()).setSize(60, 20)
				.setHoverTexts("stats.hover.gravity");
		if (!stats.getHasGravity()) {
			addButton(1, guiLeft + 140, guiTop + 100, false, stats.getAccelerate() ? 1 : 0, "gui.constant", "gui.accelerate")
					.setSize(60, 20)
					.setHoverTexts("stats.hover.accelerating");
		}
		// explosive
		addLabel(6, guiLeft + 5, guiTop + 135, "stats.explosive");
		addButton(3, guiLeft + 60, guiTop + 130, false, stats.getExplodeSize() % 4,
				"gui.none", "gui.small", "gui.medium", "gui.large")
				.setSize(60, 20)
				.setHoverTexts("stats.hover.explosion");
		int effect = stats.getEffectType();
		// ranged effect
		addLabel(7, guiLeft + 5, guiTop + 165, "stats.rangedeffect");
		addButton(4, guiLeft + 40, guiTop + 160, true, effect, potionNames)
				.setSize(100, 20)
				.setHoverTexts("stats.hover.attack.effects");
		if (stats.getEffectType() != 0) {
			addTextField(5, guiLeft + 140, guiTop + 160, 60, 18, stats.getEffectTime())
					.setMinMaxDefault(1, 99999, 5)
					.setHoverTexts("stats.hover.effect.time");
			if (stats.getEffectType() != 1) {
				addButton(10, guiLeft + 210, guiTop + 160, false,
						stats.getEffectStrength() % 2, "stats.regular", "stats.amplified")
						.setSize(40, 20)
						.setHoverTexts("stats.hover.effect.power");
			}
		}
		// trail
		addLabel(8, guiLeft + 5, guiTop + 195, "stats.trail");
		addButton(5, guiLeft + 60, guiTop + 190, false, stats.getParticle(), trailNames)
				.setSize(60, 20)
				.setHoverTexts("stats.hover.particle");
		addButton(7, guiLeft + 220, guiTop + 10, false, stats.getRender3D() ? 1 : 0, "2D", "3D")
				.setSize(30, 20)
				.setHoverTexts("stats.hover.bullet.3d");
		if (stats.getRender3D()) {
			// spin
			addLabel(10, guiLeft + 160, guiTop + 45, "stats.spin");
			addYesNo(8, guiLeft + 220, guiTop + 40, stats.getSpins())
					.setSize(30, 20)
					.setHoverTexts("stats.hover.bullet.rotate");
			// stick
			addLabel(11, guiLeft + 160, guiTop + 75, "stats.stick");
			addYesNo(9, guiLeft + 220, guiTop + 70, stats.getSticks())
					.setSize(30, 20)
					.setHoverTexts("stats.hover.bullet.cling");
		}
		// glows
		addButton(6, guiLeft + 140, guiTop + 190, false, stats.getGlows() ? 1 : 0, "stats.noglow", "stats.glows")
				.setSize(60, 20)
				.setHoverTexts("stats.hover.in.fire");
		addButton(66, guiLeft + 210, guiTop + 190, "gui.done")
				.setSize(40, 20)
				.setHoverTexts("hover.back");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				stats.setHasGravity(button.getValue() == 1);
				initGui();
				break;
			}
			case 1: stats.setAccelerate(button.getValue() == 1); break;
			case 2: break;
			case 3: stats.setExplodeSize(button.getValue());break;
			case 4: {
				int effect = button.getValue();
				stats.setEffect(effect, stats.getEffectStrength(), stats.getEffectTime());
				initGui();
				break;
			}
			case 5: stats.setParticle(button.getValue()); break;
			case 6: stats.setGlows(button.getValue() == 1); break;
			case 7: {
				stats.setRender3D(button.getValue() == 1);
				initGui();
				break;
			}
			case 8: stats.setSpins(button.getValue() == 1); break;
			case 9: stats.setSticks(button.getValue() == 1); break;
			case 10: stats.setEffect(stats.getEffectType(), button.getValue(), stats.getEffectTime()); break;
			case 66: onClose(); break;
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 1: stats.setStrength(textField.getInteger()); break;
			case 2: stats.setKnockback(textField.getInteger()); break;
			case 3: stats.setSize(textField.getInteger()); break;
			case 4: stats.setSpeed(textField.getInteger()); break;
			case 5: stats.setEffect(stats.getEffectType(), stats.getEffectStrength(), textField.getInteger()); break;
		}
	}

}
