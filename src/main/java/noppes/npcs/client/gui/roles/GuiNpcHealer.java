package noppes.npcs.client.gui.roles;

import java.util.*;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
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
	private final Map<Component, Component> displays = new LinkedHashMap<>(); // [display name, registry name]
	private final Map<Component, ResourceLocation> potions = new TreeMap<>(Comparator.comparing(Component::getString)); // [registry name, registry ID]
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

		for (Potion potion : Potion.REGISTRY) {
			potions.put(Component.empty().append(Component.translatable(potion.getName())
					.withStyle(potion.isBeneficial() ? TextFormatting.GREEN : TextFormatting.RED)), potion.getRegistryName());
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
		displays.clear();
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
		for (Component name : potions.keySet()) {
			Potion potion = Potion.getPotionFromResourceLocation(potions.get(name).toString());
			if (potion == null) { continue; }
			int id = Potion.getIdFromPotion(potion);
			if (!job.effects.containsKey(id)) { // has potion ID
				htsO.put(htsO.size(), getHover(id, potions.get(name).toString()));
			}
			else { // to setts
				HealerSettings hs = job.effects.get(id);
				String lv = "enchantment.level." + (hs.amplifier + 1);
				if (!Component.translatable(lv).getString().equals(lv)) { lv = Component.translatable(lv).getFormattedText(); }
				else { lv = "" + (hs.amplifier + 1); }
				displays.put(name.copy().append(Component.literal(" " + lv).withStyle(TextFormatting.RESET)), name);
				List<Component> hovers = getHover(id, potions.get(name).toString());
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
						.append(Component.translatable(hs.type == (byte) 0 ? "faction.friendly" : hs.type == (byte) 1 ? "faction.unfriendly" : "spawner.all")
								.withStyle(hs.type == (byte) 0 ? TextFormatting.GREEN : TextFormatting.DARK_AQUA)));
				hovers.add(Component.empty()
						.append(u)
						.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
						.append(Component.translatable(hs.isMassive ? "beacon.massive" : "beacon.not.massive")
								.withStyle(hs.isMassive ? TextFormatting.DARK_PURPLE : TextFormatting.YELLOW)));
				htsC.put(htsC.size(), hovers);
			}
		}
		options.setUnsortedList(new ArrayList<>(potions.keySet()));
		options.setHoverTexts(htsO);
		configured.setUnsortedList(new ArrayList<>(displays.keySet()));
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
				.setIsEnabled(!potions.isEmpty())
				.setHoverTexts("hover.add.all.element");
		addButton(14, guiLeft + 177, (y += 22), "<<")
				.setSize(61, 20)
				.setIsEnabled(!displays.isEmpty())
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
				if (displays.containsKey(configured.getNormalSelected())) {
					Potion potion = Potion.getPotionFromResourceLocation(potions.get(displays.get(configured.getNormalSelected())).toString());
					if (potion == null) { return; }
					int id = Potion.getIdFromPotion(potion);
					if (job.effects.containsKey(id)) {
						setSubGui(new SubGuiNpcJobHealerSettings(job.effects.get(id)));
					}
				}
				break;
			} // edit HealerSettings
			case 1: type = (byte) button.getValue(); break; // faction type
			case 11: {
				if (potions.containsKey(options.getNormalSelected())) {
					GuiTextFieldNop.unfocus();
					Potion potion = Potion.getPotionFromResourceLocation(potions.get(options.getNormalSelected()).toString());
					if (potion == null) { return; }
					int id = Potion.getIdFromPotion(potion);
					HealerSettings hs = new HealerSettings(id, range, speed, amplifier, type);
					job.effects.put(id, hs);
					options.setSelected(-1);
					configured.setSelected(-1);
					initGui();
				}
				break;
			} // >
			case 12: {
				if (displays.containsKey(configured.getNormalSelected())) {
					Potion potion = Potion.getPotionFromResourceLocation(potions.get(displays.get(configured.getNormalSelected())).toString());
					if (potion == null) { return; }
					job.effects.remove(Potion.getIdFromPotion(potion));
					options.setSelected(-1);
					configured.setSelected(-1);
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
				options.setSelected(-1);
				configured.setSelected(-1);
				initGui();
				break;
			} // >>
			case 14: {
				job.effects.clear();
				options.setSelected(-1);
				configured.setSelected(-1);
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
			Potion potion = Potion.getPotionFromResourceLocation(potions.get(displays.get(configured.getNormalSelected())).toString());
			if (potion == null) { return; }
			int id = Potion.getIdFromPotion(potion);
			job.effects.put(id, ((SubGuiNpcJobHealerSettings) subgui).healerSettings);
			initGui();
		}
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) { initGui(); }

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0) {
			if (!scroll.hasSelected() || !potions.containsKey(scroll.getNormalSelected())) { return; }
			GuiTextFieldNop.unfocus();
			Potion potion = Potion.getPotionFromResourceLocation(potions.get(scroll.getNormalSelected()).toString());
			if (potion == null) { return; }
			HealerSettings hs = new HealerSettings(Potion.getIdFromPotion(potion), range, speed, amplifier, type);
			job.effects.put(hs.id, hs);
			options.setSelected(-1);
			configured.setSelected(-1);
			initGui();
		}
		else {
			if (!scroll.hasSelected() || !displays.containsKey(scroll.getNormalSelected())) { return; }
			Potion potion = Potion.getPotionFromResourceLocation(potions.get(displays.get(scroll.getNormalSelected())).toString());
			if (potion == null) { return; }
			int id = Potion.getIdFromPotion(potion);
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

	protected List<Component> getHover(int id, String regName) {
		List<Component> hover = new ArrayList<>();
		hover.add(Component.empty()
				.append(Component.literal("ID: "))
				.append(Component.literal("" + id).withStyle(TextFormatting.GOLD)));
		hover.add(Component.empty()
				.append(Component.translatable("gui.name"))
				.append(": ")
				.append(Component.literal(regName).withStyle(TextFormatting.DARK_AQUA)));
		return hover;
	}

}
