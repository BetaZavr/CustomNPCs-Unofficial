package noppes.npcs.client.gui.global;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMailsGet;
import noppes.npcs.packets.server.SPacketMailsSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import javax.annotation.Nonnull;

public class GuiNpcManageMail extends GuiNPCInterface2
		implements IGuiData, ITextfieldListener {

	public GuiNpcManageMail(EntityNPCInterface npc) {
		super(npc);

		backGui = EnumGuiType.MainMenuGlobal;
		Packets.sendServer(new SPacketMailsGet());
	}

	@Override
	public void initGui() {
		super.initGui();
		int lId = 0;
		int x0 = guiLeft + 10;
		int x1 = x0 + 80;
		int y = guiTop + 20;
		addLabel(lId++, x0, y + 5, "mail.time.days");
		addLabel(lId++, x1 + 65, y + 5, "follower.days");
		addTextField(0, x1, y, 60, 20, "" + CustomNpcs.MailTimeWhenLettersWillBeDeleted)
				.setMinMaxDefault(0, 60, CustomNpcs.MailTimeWhenLettersWillBeDeleted)
				.setHoverTexts("mail.hover.deleted.time");
		addLabel(lId++, x0, (y += 24) + 5, "mail.time.rec");
		int[] vd = CustomNpcs.MailTimeWhenLettersWillBeReceived;
		if (vd[0] > vd[1]) {
			int m = vd[0];
			vd[0] = vd[1];
			vd[1] = m;
		}
		addLabel(lId++, x0, (y += 16) + 5, "gui.min");
		addLabel(lId++, x1 + 65, y + 5, "gui.sec");
		addTextField(1, x1, y, 60, 20, "" + vd[0])
				.setMinMaxDefault(1, 3600, vd[0])
				.setHoverTexts("mail.hover.min.send.time");
		addLabel(lId++, x0, (y += 24) + 5, "gui.max");
		addLabel(lId++, x1 + 65, y + 5, "gui.sec");
		addTextField(2, x1, y, 60, 20, "" + vd[1])
				.setMinMaxDefault(1, 3600, vd[1])
				.setHoverTexts("mail.hover.max.send.time");
		addLabel(lId++, x0, (y += 24) + 5, "mail.time.costs");
		addLabel(lId++, x0, (y += 16) + 5, "mail.time.cost.0");
		addLabel(lId++, x1 + 65, y + 5, CustomNpcs.displayCurrencies);
		Component hoverCost = Component.translatable("mail.hover.cost");
		addTextField(3, x1, y, 60, 20, "" + CustomNpcs.MailCostSendingLetter[0])
				.setMinMaxDefault(0, Integer.MAX_VALUE, CustomNpcs.MailCostSendingLetter[0])
				.setHoverTexts(Component.translatable("mail.hover.cost.0").append(hoverCost));
		int x2 = x1 + 120, x3 = x2 + 80;
		addLabel(lId++, x2, y + 5, "mail.time.cost.1");
		addLabel(lId++, x3 + 65, y + 5, CustomNpcs.displayCurrencies);
		addTextField(4, x3, y, 60, 20, "" + CustomNpcs.MailCostSendingLetter[1])
				.setMinMaxDefault(0, Integer.MAX_VALUE, CustomNpcs.MailCostSendingLetter[1])
				.setHoverTexts(Component.translatable("mail.hover.cost.1").append(hoverCost));
		addLabel(lId++, x0, (y += 24) + 5, "mail.time.cost.2");
		addLabel(lId++, x1 + 65, y + 5, CustomNpcs.displayCurrencies);
		addTextField(5, x1, y, 60, 20, "" + CustomNpcs.MailCostSendingLetter[2])
				.setMinMaxDefault(0, Integer.MAX_VALUE, CustomNpcs.MailCostSendingLetter[2])
				.setHoverTexts(Component.translatable("mail.hover.cost.2").append(hoverCost));
		addLabel(lId++, x0, (y += 24) + 5, "mail.time.cost.3");
		addLabel(lId++, x1 + 65, y + 5, "%");
		addTextField(6, x1, y, 60, 20, "" + CustomNpcs.MailCostSendingLetter[3])
				.setMinMaxDefault(0, 100, CustomNpcs.MailCostSendingLetter[3])
				.setHoverTexts(Component.translatable("mail.hover.cost.3").append(hoverCost));
		addLabel(lId++, x2, y + 5, "mail.time.cost.4");
		addLabel(lId, x3 + 65, y + 5, "%");
		addTextField(7, x3, y, 60, 20, "" + CustomNpcs.MailCostSendingLetter[4])
				.setMinMaxDefault(0, 100, CustomNpcs.MailCostSendingLetter[4])
				.setHoverTexts(Component.translatable("mail.hover.cost.4").append(hoverCost));
		addCheckBox(0, x0, y + 24, "mail.send.yourself", null, CustomNpcs.MailSendToYourself)
				.setSize(200, 14);
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		if (button.id == 0) { CustomNpcs.MailSendToYourself = ((GuiCheckBoxNop) button).selected(); }
	}

	@Override
	public void save() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("LettersBeDeleted", CustomNpcs.MailTimeWhenLettersWillBeDeleted);
		compound.setIntArray("LettersBeReceived", CustomNpcs.MailTimeWhenLettersWillBeReceived);
		compound.setIntArray("CostSendingLetter", CustomNpcs.MailCostSendingLetter);
		compound.setBoolean("SendToYourself", CustomNpcs.MailSendToYourself);
		Packets.sendServer(new SPacketMailsSave(compound));
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		int[] vs = compound.getIntArray("LettersBeReceived");
        System.arraycopy(vs, 0, CustomNpcs.MailTimeWhenLettersWillBeReceived, 0, vs.length);
		vs = compound.getIntArray("CostSendingLetter");
        System.arraycopy(vs, 0, CustomNpcs.MailCostSendingLetter, 0, vs.length);
		initGui();
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (!textField.isInteger()) { initGui(); return; }
		switch (textField.id) {
			case 0: {
				int v = textField.getInteger();
				if (v == 0) { v = 1; }
				CustomNpcs.MailTimeWhenLettersWillBeDeleted = v;
				break;
			}
			case 1: {
				if (getTextField(2) == null) {return; }
				GuiTextFieldNop textField2 = getTextField(2);
				int[] vd = new int[] { textField.getInteger(), textField2.getInteger() };
				if (vd[0] > vd[1]) {
					int m = vd[0];
					vd[0] = vd[1];
					vd[1] = m;
				}
				CustomNpcs.MailTimeWhenLettersWillBeReceived[0] = vd[0];
				CustomNpcs.MailTimeWhenLettersWillBeReceived[1] = vd[1];
				textField.setValue("" + vd[0]);
				textField2.setValue("" + vd[1]);
				break;
			}
			case 2: {
				if (getTextField(1) == null) { return; }
				GuiTextFieldNop textField1 = getTextField(1);
				int[] vd = new int[] { textField1.getInteger(), textField.getInteger() };
				if (vd[0] > vd[1]) {
					int m = vd[0];
					vd[0] = vd[1];
					vd[1] = m;
				}
				CustomNpcs.MailTimeWhenLettersWillBeReceived[0] = vd[0];
				CustomNpcs.MailTimeWhenLettersWillBeReceived[1] = vd[1];
				textField1.setValue("" + vd[0]);
				textField.setValue("" + vd[1]);
				break;
			}
			case 3: case 4: case 5: case 6: case 7: {
				CustomNpcs.MailCostSendingLetter[textField.id - 3] = textField.getInteger();
				break;
			}
		}
	}

}
