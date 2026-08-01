package noppes.npcs.client.gui.mainmenu;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.SubGuiNpcMovement;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.constants.EnumNpcTactics;
import noppes.npcs.constants.EnumSeeTarget;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuGet;
import noppes.npcs.packets.server.SPacketMenuSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import java.awt.*;

public class GuiNpcAI extends GuiNPCInterface2 implements ITextfieldListener, IGuiData {

	protected static final Object[] tactics;
	protected static final Object[] directs;
	protected final DataAI ai;

	static {
		tactics = new Object[EnumNpcTactics.values().length];
		int i = 0;
		for (EnumNpcTactics tactic : EnumNpcTactics.values()) { tactics[i++] = "ai.tactic." + tactic.name().toLowerCase(); }
		directs = new Object[EnumSeeTarget.values().length];
		i = 0;
		for (EnumSeeTarget est : EnumSeeTarget.values()) { directs[i++] = "ai.direct." + est.name().toLowerCase(); }
	}

	public GuiNpcAI(EntityNPCInterface npc) {
		super(npc, 3);

		ai = npc.ais;
		Packets.sendServer(new SPacketMenuGet(EnumMenuType.AI));
	}

	@Override
	public void initGui() {
		super.initGui();
		int lId = 0;
		int w = 130;
		int x0 = guiLeft + 7;
		int x1 = x0 + w + 2;
		int y = guiTop + 10;
		int hStep = 22;
		// R1
		addLabel(lId++, x0, y + 7, "ai.enemyresponse")
				.setSize(w, 10);
		Component mess = Component.translatable("ai.hover.if.see")
				.append(Component.translatable("ai.hover.if.see." + npc.ais.onAttack));
		if (ai.aiDisabled) { mess.append(Component.translatable("hover.ai.disabled")); }
		addButton(0, x1, y, false, npc.ais.onAttack, "gui.retaliate", "gui.panic", "gui.retreat", "gui.nothing")
				.setSize(60, 20)
				.setIsEnabled(!ai.aiDisabled)
				.setHoverTexts(mess);
		addLabel(lId++, x0, (y += hStep) + 7, "ai.door")
				.setSize(w, 10);
		addButton(1, x1, y, false, npc.ais.doorInteract, "gui.break", "gui.open", "gui.disabled")
				.setSize(60, 20)
				.setHoverTexts("ai.hover.door");
		addLabel(lId++, x0, (y += hStep) + 7, "ai.swim")
				.setSize(w, 10);
		addYesNo(7, x1, y, npc.ais.canSwim)
				.setSize(60, 20)
				.setHoverTexts("ai.hover.water");
		addLabel(lId++, x0, (y += hStep) + 7, "ai.shelter")
				.setSize(w, 10);
		addButton(9, x1, y, false, npc.ais.findShelter, "gui.darkness", "gui.sunlight", "gui.disabled")
				.setSize(60, 20)
				.setHoverTexts("ai.hover.found.refuge");
		addLabel(lId++, x0, (y += hStep) + 7, "ai.clearlos")
				.setSize(w, 10);

		addButton(10, x1, y, false, npc.ais.directLOS.ordinal(), directs)
				.setSize(60, 20)
				.setIsEnabled(!ai.aiDisabled)
				.setHoverTexts(Component.translatable("ai.hover.found.target")
						.append("<br>").append(Component.translatable("ai.hover.direct."+npc.ais.directLOS.name().toLowerCase())));
		addLabel(lId++, x0, (y += hStep) + 7, "stats.attackInvisible")
				.setSize(w, 10);
		mess = Component.translatable("ai.hover.stealth");
		if (ai.aiDisabled) { mess.append(Component.translatable("hover.ai.disabled")); }
		addYesNo(23, x1, y, ai.attackInvisible)
				.setSize(60, 20)
				.setIsEnabled(!ai.aiDisabled)
				.setHoverTexts(mess);
		addLabel(lId++, x0, (y += hStep) + 7, "ai.movement")
				.setSize(w, 10);
		addButton(2, x1, y, "selectServer.edit")
				.setSize(60, 20)
				.setHoverTexts("ai.hover.set.walking");
		addLabel(lId++, x0, (y += hStep) + 7, "ai.disabled")
				.setSize(w, 10);
		addYesNo(25, x1, y, ai.aiDisabled)
				.setSize(60, 20)
				.setColor(ai.aiDisabled ? new Color(0xFFF02020).getRGB() : new Color(0xFF20F020).getRGB())
				.setHoverTexts("ai.hover.disabled");
		// R2
		y = guiTop + 10;
		x0 += w + 66;
		x1 = x0 + w + 2;
		addLabel(lId++, x0, y + 7, "ai.avoidwater")
				.setSize(w, 10);
		addYesNo(5, x1, y, ai.avoidsWater)
				.setSize(60, 20)
				.setHoverTexts("ai.hover.non.water");
		addLabel(lId++, x0, (y += hStep) + 7, "ai.return")
				.setSize(w, 10);
		addYesNo(6, x1, y, ai.returnToStart)
				.setSize(60, 20)
				.setHoverTexts("ai.hover.back.home");
		addLabel(lId++, x0, (y += hStep) + 7, "ai.leapattarget")
				.setSize(w, 10);
		mess = Component.translatable("ai.hover.jump");
		if (ai.aiDisabled) { mess.append(Component.translatable("hover.ai.disabled")); }
		addYesNo(15, x1, y, ai.canLeap)
				.setSize(60, 20)
				.setIsEnabled(!ai.aiDisabled && ai.onAttack == 0)
				.setHoverTexts(mess);
		addLabel(lId++, x0, (y += hStep) + 5, "ai.mountcontrol");
		addYesNo(22, x1, y, npc.ais.mountControl)
				.setSize(60, 20);
		addLabel(lId++, x0, (y += hStep) + 7, "ai.cansprint")
				.setSize(w, 10);
		addYesNo(16, x1, y, ai.canSprint)
				.setSize(60, 20)
				.setHoverTexts("ai.hover.run", "" + (int) ((double) npc.stats.aggroRange / 3.0d));
		addLabel(lId++, x0, (y += hStep) + 7, "ai.hurt.resistant.time")
				.setSize(w, 10);
		addTextField(4, x1 + 1, y + 1, 58, 18, (ai.getMaxHurtResistantTime() / 2) + "")
				.setMinMaxDefault(0, 100, ai.getMaxHurtResistantTime() / 2)
				.setHoverTexts("ai.hover.hurt.resistant.time");
		addLabel(lId++, x0, (y += hStep) + 7, "ai.can.be.collide")
				.setSize(w, 10);
		addYesNo(18, x1, y, ai.canBeCollide)
				.setSize(60, 20)
				.setHoverTexts("ai.hover.can.be.collide");
		addLabel(lId++, x0, (y += hStep) + 7, "ai.tacticalvariant")
				.setSize(w, 10);
		mess = Component.translatable("ai.hover.attack.type",
						Component.translatable("ai.tactic." + ai.tacticalVariant.name().toLowerCase()).getFormattedText())
				.append(Component.translatable("ai.hover.attack.type." + ai.tacticalVariant.name().toLowerCase()));
		if (ai.aiDisabled) { mess.append(Component.translatable("hover.ai.disabled")); }
		addButton(17, x1, y, false, ai.tacticalVariant.ordinal(), tactics)
				.setSize(60, 20)
				.setIsEnabled(!ai.aiDisabled && ai.onAttack == 0)
				.setHoverTexts(mess);
		if (ai.tacticalVariant != EnumNpcTactics.RUSH && ai.tacticalVariant != EnumNpcTactics.NONE) {
			String label;
			switch (ai.tacticalVariant) {
				case STAGGER: label = "gui.dodgedistance"; break;
				case ORBIT: label = "gui.orbitdistance"; break;
				case HIT_AND_RUN: label = "gui.fightifthisclose"; break;
				case COMMANDER: label = "gui.searchdistance"; break;
				case STALK: label = "gui.proximity"; break;
				default: label = "gui.engagedistance"; break;
			}
			addLabel(lId, x0, (y += hStep) + 7, label).setSize(w, 10);
			addTextField(3, x1 + 1, y + 1, 58, 18, ai.getTacticalRange() + "")
					.setMinMaxDefault(1, npc.stats.aggroRange, 5)
					.setHoverTexts("ai.hover.attack.range");
		}
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: ai.onAttack = button.getValue(); initGui(); break;
			case 1: ai.doorInteract = button.getValue(); break;
			case 2: setSubGui(new SubGuiNpcMovement(ai)); break;
			case 5: npc.ais.setAvoidsWater(button.getValue() == 1); break;
			case 6: ai.returnToStart = (button.getValue() == 1); break;
			case 7: ai.canSwim = (button.getValue() == 1); break;
			case 9: ai.findShelter = button.getValue(); break;
			case 10: {
				ai.setAttackLOS(button.getValue());
				button.setHoverTexts(Component.translatable("ai.hover.found.target")
						.append("<br>").append(Component.translatable("ai.hover.direct."+npc.ais.directLOS.name().toLowerCase())));
				break;
			}
			case 15: ai.canLeap = (button.getValue() == 1); break;
			case 16: ai.canSprint = (button.getValue() == 1); break;
			case 17: {
				ai.setTacticalType(button.getValue());
				initGui();
				break;
			}
			case 18: ai.canBeCollide = (button.getValue() == 1); break;
			case 22: ai.mountControl = ((GuiButtonYesNo) button).getBoolean(); break;
			case 23: ai.attackInvisible = ((GuiButtonYesNo) button).getBoolean(); break;
			case 25: {
				ai.aiDisabled = ((GuiButtonYesNo) button).getBoolean();
				if (ai.aiDisabled) {
					ai.onAttack = 0;
					ai.directLOS = EnumSeeTarget.NORMAL;
					ai.tacticalVariant = EnumNpcTactics.RUSH;
				}
				button.setColor(ai.aiDisabled ?
						new Color(0xFFF02020).getRGB() :
						new Color(0xFF20F020).getRGB());
				initGui();
				break;
			}
			case 66: onClose(); break;
		}
	}

	@Override
	public void save() { Packets.sendServer(new SPacketMenuSave(EnumMenuType.AI, ai.save(new NBTTagCompound()))); }

	@Override
	public void setGuiData(NBTTagCompound compound) {
		if (compound.hasKey("MovementType", 3)) {
			ai.load(compound);
			initGui();
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (textField.id == 3) { ai.setTacticalRange(textField.getInteger()); }
		else if (textField.id == 4) {
			ai.setMaxHurtResistantTime(textField.getInteger() * 2);
			if (textField.getInteger() * 2 != ai.getMaxHurtResistantTime()) { textField.setValue("" + ai.getMaxHurtResistantTime()); }
		}
	}

}
