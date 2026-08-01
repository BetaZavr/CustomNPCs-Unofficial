package noppes.npcs.client.gui.roles;

import java.util.*;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.potion.Potion;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcJobSave;
import noppes.npcs.roles.JobHealer;
import noppes.npcs.roles.data.HealerSettings;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;


public class GuiNpcHealer
		extends GuiNPCInterface2
		implements ITextfieldListener, ICustomScrollListener {

	protected final JobHealer job;

	// New from Unofficial (BetaZavr)
	private final Map<Component, String> displays_0 = new HashMap<>();
	private final Map<Component, String> displays_1 = new HashMap<>(); // [display name, registry name] (0-options, 1-configured)
	private final Map<String, Integer> potions = new TreeMap<>(); // [registry name, registry ID]
	private int range = 8;
	private int speed = 10;
	private int amplifier = 0;
	private GuiCustomScrollNop options; // OLD name scroll1
	private GuiCustomScrollNop configured; // OLD name scroll2
	private byte type = (byte) 2;

	public GuiNpcHealer(EntityNPCInterface npc) {
		super(npc);

		backGui = EnumGuiType.MainMenuAdvanced;
		job = (JobHealer)npc.job;
		for (Potion p : Potion.REGISTRY) {
			potions.put(Component.translatable("effect." + p.getName().toLowerCase()).getFormattedText(), Potion.getIdFromPotion(p));
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		int x0 = guiLeft + 4;
		int y = guiTop + 14;
		if (options == null) { options = addScroll(0).setSize(172, 154); }
		add(options.setPos(x0, y));
		addLabel(11, x0 + 2, y - 10, "beacon.availableEffects");
		if (configured == null) { configured = addScroll(1).setSize(172, 154); }
		add(configured.setPos(guiLeft + 238, y));
		addLabel(12, guiLeft + 239, y - 10, "beacon.currentEffects");
		displays_0.clear();
		displays_1.clear();
		LinkedHashMap<Integer, List<Component>> htsO = new LinkedHashMap<>();
		LinkedHashMap<Integer, List<Component>> htsC = new LinkedHashMap<>();
		Component r = Component.translatable("gui.range").withStyle(TextFormatting.GRAY);
		Component s = Component.translatable("gui.repeatable").withStyle(TextFormatting.GRAY);
		Component b = Component.translatable("gui.blocks").withStyle(TextFormatting.GRAY);
		Component c = Component.translatable("gui.sec").withStyle(TextFormatting.GRAY);
		Component t = Component.translatable("gui.time").withStyle(TextFormatting.GRAY);
		Component p = Component.translatable("beacon.amplifier").withStyle(TextFormatting.GRAY);
		Component l = Component.translatable("parameter.level").withStyle(TextFormatting.GRAY);
		Component j = Component.translatable("gui.type").withStyle(TextFormatting.GRAY);
		Component u = Component.translatable("script.target").withStyle(TextFormatting.GRAY);
		for (String pointName : potions.keySet()) {
			int id = potions.get(pointName);
			Potion potion = Potion.getPotionById(id);
			Component name = Component.empty().append(Component.translatable(pointName)
					.withStyle(potion == null ? TextFormatting.LIGHT_PURPLE :
							potion.isBeneficial() ? TextFormatting.GREEN : TextFormatting.RED));
			if (!job.effects.containsKey(id)) { // has potion ID
				displays_0.put(name, pointName);
				htsO.put(htsO.size(), Collections.singletonList(Component.empty()
						.append(Component.literal("ID: "))
						.append(Component.literal("" + id).withStyle(TextFormatting.GOLD))));
			}
			else { // to setts
				HealerSettings hs = job.effects.get(id);
				String lv = "enchantment.level." + (hs.amplifier + 1);
				if (!Component.translatable(lv).getString().equals(lv)) { lv = Component.translatable(lv).getFormattedText(); }
				else { lv = "" + (hs.amplifier + 1); }
				displays_1.put(name.append(Component.literal(" " + lv).withStyle(TextFormatting.RESET)), pointName);
				Component f = Component.translatable(hs.type == (byte) 0 ? "faction.friendly" : hs.type == (byte) 1 ? "faction.unfriendly" : "spawner.all")
						.withStyle(hs.type == (byte) 0 ? TextFormatting.GREEN : TextFormatting.DARK_AQUA);
				Component h = Component.translatable(hs.isMassive ? "beacon.massive" : "beacon.not.massive")
						.withStyle(hs.isMassive ? TextFormatting.DARK_PURPLE : TextFormatting.YELLOW);
				List<Component> hovers = new ArrayList<>();
				hovers.add(Component.empty()
						.append(Component.literal("ID: "))
						.append(Component.literal("" + id).withStyle(TextFormatting.GOLD)));
				hovers.add(Component.empty()
						.append(r)
						.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
						.append(Component.literal("" + hs.range).withStyle(TextFormatting.YELLOW))
						.append(Component.literal(" ").withStyle(TextFormatting.GRAY))
						.append(b));
				hovers.add(Component.empty()
						.append(s)
						.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
						.append(Component.literal("" + (Math.round((double) hs.speed / 2.0d) / 10.0d)).withStyle(TextFormatting.AQUA))
						.append(Component.literal(" ").withStyle(TextFormatting.GRAY))
						.append(c));
				hovers.add(Component.empty()
						.append(t)
						.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
						.append(Component.literal("" + (Math.round((double) hs.time / 2.0d) / 10.0d)).withStyle(TextFormatting.GREEN))
						.append(Component.literal(" ").withStyle(TextFormatting.GRAY))
						.append(c));
				hovers.add(Component.empty()
						.append(p)
						.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
						.append(Component.literal("" + (hs.amplifier + 1)).withStyle(TextFormatting.RED))
						.append(Component.literal(" ").withStyle(TextFormatting.GRAY))
						.append(l));
				hovers.add(Component.empty()
						.append(j)
						.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
						.append(f));
				hovers.add(Component.empty()
						.append(u)
						.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
						.append(h));
				htsC.put(htsC.size(), hovers);
			}
		}
		options.setUnsortedList(new ArrayList<>(displays_0.keySet()));
		options.setHoverTexts(htsO);
		configured.setUnsortedList(new ArrayList<>(displays_1.keySet()));
		configured.setHoverTexts(htsC);
		Component toall = Component.translatable("beacon.hover.toall");
		y += 156;
		int x1 = x0 + 102;
		int x2 = x1 + 72;
		int x3 = x2 + 102;
		addLabel(1, x0, y + 5, "beacon.range")
				.setSize(100, 12);
		addTextField(1, x1, y, 40, 20, range)
				.setMinMaxDefault(1, 64, 16)
				.setHoverTexts(Component.translatable("beacon.hover.dist").append(toall));
		addLabel(2, x2, y + 5, "stats.speed")
				.setSize(100, 12);
		addTextField(2, x3, y, 40, 20, speed)
				.setMinMaxDefault(10, 72000, 20)
				.setHoverTexts(Component.translatable("beacon.hover.speed").append(toall));
		addLabel(3, x0, (y += 22) + 5, "beacon.affect")
				.setSize(100, 12);
		addButton(1, x1 - 39, y, false, type, "faction.friendly", "faction.unfriendly", "spawner.all")
				.setSize(80, 20)
				.setHoverTexts(Component.translatable("beacon.hover.type").append(toall));
		addLabel(4, x2, y + 5, "beacon.amplifier")
				.setSize(100, 12);
		String lv = "enchantment.level." + (amplifier + 1);
		if (!Component.translatable(lv).getString().equals(lv)) { lv = Component.translatable(lv).getFormattedText(); }
		else { lv = "" + (amplifier + 1); }
		addTextField(3, x3, y, 40, 20, amplifier + 1)
				.setMinMaxDefault(1, 4, 1)
				.setHoverTexts(Component.translatable("beacon.hover.power", lv).append(toall));
		addButton(11, guiLeft + 177, (y -= 165), ">")
				.setSize(61, 20)
				.setIsEnabled(options.hasSelected())
				.setHoverTexts("hover.add.element");
		addButton(12, guiLeft + 177, (y += 22), "<")
				.setSize(61, 20)
				.setIsEnabled(configured.hasSelected())
				.setHoverTexts("hover.del.element");
		addButton(13, guiLeft + 177, (y += 44), ">>")
				.setSize(61, 20)
				.setIsEnabled(!displays_0.isEmpty())
				.setHoverTexts("hover.add.all.element");
		addButton(14, guiLeft + 177, (y += 22), "<<")
				.setSize(61, 20)
				.setIsEnabled(!displays_1.isEmpty())
				.setHoverTexts("hover.del.all.element");
		addButton(0, guiLeft + 177, y + 33, "gui.edit")
				.setSize(61, 20)
				.setIsEnabled(configured.hasSelected())
				.setHoverTexts("beacon.hover.edit.element");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				if (displays_1.containsKey(configured.getNormalSelected())) {
					int id = potions.get(displays_1.get(configured.getNormalSelected()));
					if (job.effects.containsKey(id)) {
						setSubGui(new SubGuiNpcJobHealerSettings(job.effects.get(id)));
					}
				}
				break;
			} // edit HealerSettings
			case 1: type = (byte) button.getValue(); break; // faction type
			case 11: {
				if (displays_0.containsKey(options.getNormalSelected())) {
					GuiTextFieldNop.unfocus();
					int id = potions.get(displays_0.get(options.getNormalSelected()));
					HealerSettings hs = new HealerSettings(id, range, speed, amplifier, type);
					job.effects.put(id, hs);
					options.setSelect(-1);
					configured.setSelect(-1);
					initGui();
				}
				break;
			} // >
			case 12: {
				if (displays_1.containsKey(configured.getNormalSelected())) {
					job.effects.remove(potions.get(displays_1.get(configured.getNormalSelected())));
					options.setSelect(-1);
					configured.setSelect(-1);
					initGui();
				}
				break;
			} // <
			case 13: {
				GuiTextFieldNop.unfocus();
				job.effects.clear();
				for (Potion p : Potion.REGISTRY) {
					int id = Potion.getIdFromPotion(p);
					HealerSettings hs = new HealerSettings(id, range, speed, amplifier, type);
					job.effects.put(id, hs);
				}
				options.setSelect(-1);
				configured.setSelect(-1);
				initGui();
				break;
			} // >>
			case 14: {
				job.effects.clear();
				options.setSelect(-1);
				configured.setSelect(-1);
				initGui();
				break;
			} // <<
		}
	}

	@Override
	public void save() { Packets.sendServer(new SPacketNpcJobSave(job.save(new NBTTagCompound()))); }

	// New from Unofficial (BetaZavr)
	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiNpcJobHealerSettings && configured.hasSelected()) {
			Integer id = potions.get(displays_1.get(configured.getNormalSelected()));
			if (id == null) { return; }
			job.effects.put(id, ((SubGuiNpcJobHealerSettings) subgui).healerSettings);
			initGui();
		}
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) { initGui(); }

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0) {
			if (!options.hasSelected()) { return; }
			GuiTextFieldNop.unfocus();
			int id = potions.get(displays_0.get(options.getNormalSelected()));
			HealerSettings hs = new HealerSettings(id, range, speed, amplifier, type);
			job.effects.put(id, hs);
			options.setSelect(-1);
			configured.setSelect(-1);
			initGui();
		}
		else {
			if (!configured.hasSelected()) { return; }
			int id = potions.get(displays_1.get(configured.getNormalSelected()));
			if (!job.effects.containsKey(id)) { return; }
			setSubGui(new SubGuiNpcJobHealerSettings(job.effects.get(id)));
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 1: range = textField.getInteger(); break;
			case 2: speed = textField.getInteger(); break;
			case 3: amplifier = textField.getInteger() - 1; break;
		}
	}

}
