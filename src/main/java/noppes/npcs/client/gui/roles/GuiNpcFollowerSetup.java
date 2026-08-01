package noppes.npcs.client.gui.roles;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerNPCFollowerSetup;
import noppes.npcs.containers.NpcMiscInventory;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcRoleSave;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class GuiNpcFollowerSetup
		extends GuiContainerNPCInterface2<ContainerNPCFollowerSetup> implements ITextfieldListener {

	protected final RoleFollower role;

	public GuiNpcFollowerSetup(EntityNPCInterface npc, ContainerNPCFollowerSetup container) {
		super(npc, container, Component.empty());
		setBackground("followersetup.png");
		ySize = 200;
		backGui = EnumGuiType.MainMenuAdvanced;

		role = (RoleFollower) npc.role;
	}

	@Override
	public void initGui() {
		super.initGui();
		int x = guiLeft + 66;
		int y = guiTop + 39;
		int lId = 0;
		// item days
		addLabel(lId++, x - 22, y - 12, Component.translatable("follower.hire").append(":"))
				.setSize(48, 10);
		int days;
		for (int i = 0; i < 3; ++i) {
			days = role.rates.getOrDefault(i, 1);
			addLabel(lId++, x - 39, y + i * 25 + 4, "#" + (i + 1))
					.setSize(15, 10);
			addTextField(i, x, y + i * 25, 24, 16, "" + days)
					.setMinMaxDefault(1, Integer.MAX_VALUE, days)
					.setHoverTexts(Component.translatable("follower.hover.days", "" + (i + 1)));
		}
		x += 34;
		y -= 33;
		// messages
		addTextField(3, x, y, 286, 16, role.dialogHire)
				.setHoverTexts("follower.hover.mes.hire");
		addTextField(4, x, y += 19, 286, 16, role.dialogFarewell)
				.setHoverTexts("follower.hover.mes.let.go");
		addTextField(5, x, y += 19, 286, 16, role.dialogFired)
				.setHoverTexts("follower.hover.mes.fired");
		// type
		addCheckBox(7, x, y += 19, "follower.infiniteDays", null, role.infiniteDays)
				.setSize(286, 14)
				.setHoverTexts("follower.hover.infinite");
		addCheckBox(8, x, y += 16, "follower.guiDisabled", null, role.disableGui)
				.setSize(286, 14)
				.setHoverTexts("follower.hover.disable.gui");
		addCheckBox(9, x, y += 16, "follower.allowSoulstone", null, !role.refuseSoulStone)
				.setSize(286, 14)
				.setHoverTexts(Component.translatable("follower.hover.soulstone",
						Component.translatable("item.customnpcs.npcsoulstoneempty").getFormattedText()));
		// money
		addLabel(lId++, x += 73, y += 19, Component.translatable("follower.hire").append(":"))
				.setSize(72, 10);
		addLabel(lId++, x + 74, y, Component.translatable("gui.money").append(":"));
		addLabel(lId++, x, (y += 11)+ 4, "#4")
				.setSize(15, 10);
		days = role.rates.getOrDefault(3, 1);
		addTextField(7, x + 35, y + 1, 24, 16, "" + days)
				.setMinMaxDefault(1, Integer.MAX_VALUE, days)
				.setHoverTexts(Component.translatable("follower.hover.days", "4"));
		addTextField(6, x + 74, y + 1, 60, 16, "" + role.rentalMoney)
				.setMinMaxDefault(0L, 9999999999L, role.rentalMoney)
				.setHoverTexts("follower.hover.money");
		addLabel(lId++, x + 136, y + 4, CustomNpcs.displayCurrencies);
		// inventory
		addLabel(lId++, x, y += 20, Component.translatable("inv.inventory").append(":"));
		addLabel(lId, x, (y += 13) + 4, "gui.things")
				.setSize(33, 10);
		addTextField(8, x + 35, y, 24, 16, "" + role.inventory.getSizeInventory())
				.setMinMaxDefault(0L, 9L, role.inventory.getSizeInventory())
				.setHoverTexts("follower.hover.inventory");
		addButton(10, x, y + 19, "remote.reset")
				.setSize(100, 20)
				.setHoverTexts("follower.hover.reset");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 7: role.infiniteDays = ((GuiCheckBoxNop) button).selected(); break;
			case 8: role.disableGui = ((GuiCheckBoxNop) button).selected(); break;
			case 9: role.refuseSoulStone = !((GuiCheckBoxNop) button).selected(); break;
			case 10: role.killed(); break;
		}
	}

	@Override
	public void save() {
		for (int i = 0; i < 3; i++) {
			ItemStack itemstack = i < role.inventory.getSizeInventory() ? role.inventory.getStackInSlot(i) : ItemStack.EMPTY;
			if (!NoppesUtilServer.isItemStackNull(itemstack) && !role.rates.containsKey(i) && getTextField(i) != null) {
				role.rates.put(i, getTextField(i).getInteger());
			}
		}
		if (role.rentalMoney > 0 && !role.rates.containsKey(3) && getTextField(7) != null) {
			role.rates.put(3, getTextField(7).getInteger());
		}
		Packets.sendServer(new SPacketNpcRoleSave(role.save(new NBTTagCompound())));
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 0: role.rates.put(0, textField.getInteger()); break;
			case 1: role.rates.put(1, textField.getInteger()); break;
			case 2: role.rates.put(2, textField.getInteger()); break;
			case 3: role.dialogHire = textField.getValue(); break;
			case 4: role.dialogFarewell = textField.getValue(); break;
			case 5: role.dialogFired = textField.getValue(); break;
			case 6: role.rentalMoney = textField.getInteger(); break;
			case 7: role.rates.put(3, textField.getInteger()); break;
			case 8: {
				int size = role.disableGui ? 0 : textField.getInteger();
				if (role.inventory.getSizeInventory() != size) { role.inventory = new NpcMiscInventory(size); }
				textField.setValue(size);
			}
		}
	}

}
