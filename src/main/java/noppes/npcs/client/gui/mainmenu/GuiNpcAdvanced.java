package noppes.npcs.client.gui.mainmenu;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.api.constants.JobType;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.advanced.GuiNPCAdvancedLinkedNpc;
import noppes.npcs.client.gui.advanced.GuiNPCDialogNpcOptions;
import noppes.npcs.client.gui.advanced.GuiNPCFactionSetup;
import noppes.npcs.client.gui.advanced.GuiNPCLinesMenu;
import noppes.npcs.client.gui.advanced.GuiNPCMarks;
import noppes.npcs.client.gui.advanced.GuiNPCNightSetup;
import noppes.npcs.client.gui.advanced.GuiNPCScenes;
import noppes.npcs.client.gui.advanced.GuiNPCSoundsMenu;
import noppes.npcs.client.gui.animation.GuiNpcAnimation;
import noppes.npcs.client.gui.animation.GuiNpcEmotion;
import noppes.npcs.client.gui.roles.*;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumAnimationType;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.roles.RoleTrader;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.listeners.IGuiData;

public class GuiNpcAdvanced extends GuiNPCInterface2 implements IGuiData {

	protected boolean hasChanges = false;
	// New from Unofficial (BetaZavr)
	protected final DataAI ais;

	public GuiNpcAdvanced(EntityNPCInterface npc) {
		super(npc, 5);
		ais = npc.ais;
		Packets.sendServer(new SPacketMenuGet(EnumMenuType.AI));
		Packets.sendServer(new SPacketMenuGet(EnumMenuType.ADVANCED));
	}

	@Override
	public void initGui() {
		super.initGui();
		int lId = 0;
		int x = guiLeft + 15;
		int x1 = guiLeft + 85;
		int y = guiTop + 8;
		// line 1
		addLabel(lId++, x, y + 5, "role.name");
		addButton(3, x + 230, y, "selectServer.edit")
				.setSize(60, 20)
				.setIsEnabled(!ais.aiDisabled && npc.role.getEnumType().hasSettings);
		if (ais.aiDisabled) { getButton(3).setHoverTexts("hover.ai.disabled"); }
		Component mess = Component.translatable("advanced.menu.hover.role." + npc.role.getType());
		if (ais.aiDisabled) { mess.append(Component.translatable("hover.ai.disabled")); }
		addButton(8, x + 70, y, true, npc.role.getType(), RoleType.getNames())
				.setSize(155, 20)
				.setIsEnabled(!ais.aiDisabled)
				.setHoverTexts(mess);
		// line 2
		addLabel(lId++, x, (y += 22) + 5, "job.name");
		int id = npc.job.getType();
		mess = Component.translatable("advanced.menu.hover.job." + id);
		if (id > 9) { id--; } // puppet is now separate
		if (ais.aiDisabled) { mess.append(Component.translatable("hover.ai.disabled")); }
		addButton(5, x + 70, y, true, id, JobType.getNames())
				.setSize(155, 20)
				.setIsEnabled(!ais.aiDisabled)
				.setHoverTexts(mess);
		addButton(4, x + 230, y, "selectServer.edit")
				.setSize(60, 20)
				.setIsEnabled(!ais.aiDisabled && npc.job.getEnumType().hasSettings
						&& npc.job.getType() != 0 && npc.job.getType() != 8 && npc.job.getType() != 10)
				.setHoverTexts(ais.aiDisabled ? "hover.ai.disabled" : null);

		// New from Unofficial (BetaZavr): line 3
		addLabel(lId++, x, (y += 22) + 5, "movement.animation");
		boolean bo;
		switch (npc.advanced.animationType) {
			case CUSTOM: bo = npc instanceof EntityCustomNpc && ((EntityCustomNpc) npc).modelData.entity == null; break;
			case PUPPET: bo = true; break;
			default: bo = false; break;
		}
		Component hoverAnim;
		switch (npc.advanced.animationType) {
			case CUSTOM: hoverAnim = Component.translatable("advanced.menu.hover.anim.custom"); break;
			case PUPPET: hoverAnim = Component.translatable("advanced.menu.hover.anim.puppet"); break;
			default: hoverAnim = Component.translatable("advanced.menu.hover.anim.none"); break;
		}
		addButton(2, x + 70, y, true, npc.advanced.animationType.ordinal(), EnumAnimationType.getNames())
				.setSize(155, 20)
				.setIsEnabled(!ais.aiDisabled)
				.setHoverTexts(hoverAnim);
		addButton(1, x + 230, y, "selectServer.edit")
				.setSize(60, 20)
				.setIsEnabled(!ais.aiDisabled && bo);
		// New from Unofficial (BetaZavr): line 4
		addLabel(lId, x, (y += 22) + 5, "advanced.emotion");
		addButton(17, x + 230, y, "selectServer.edit")
				.setSize(60, 20)
				.setIsEnabled(false) // eye
				.setHoverTexts(Component.translatable("animation.hover.eye",
						Component.translatable("gui.help.general").getFormattedText(),
						Component.translatable("selectServer.edit").getFormattedText()), "gui.wip");
		addButton(7, x, y += 24, "advanced.lines")
				.setSize(195, 20)
				.setHoverTexts("advanced.menu.hover.says");
		addButton(9, x1 += 126, y, "menu.factions")
				.setSize(195, 20)
				.setHoverTexts("advanced.menu.hover.faction");
		addButton(10, x, y += 22, "dialog.dialogs")
				.setSize(195, 20)
				.setHoverTexts("advanced.menu.hover.dialogs");
		addButton(11, x1, y, "advanced.sounds")
				.setSize(195, 20)
				.setHoverTexts("advanced.menu.hover.sounds");
		addButton(12, x, y += 22, "advanced.night")
				.setSize(195, 20)
				.setHoverTexts("advanced.menu.hover.night");
		addButton(13, x1, y, "global.linked")
				.setSize(195, 20)
				.setHoverTexts("advanced.menu.hover.lines");
		mess = Component.translatable("advanced.menu.hover.scenes");
		if (ais.aiDisabled) { mess.append(Component.translatable("hover.ai.disabled")); }
		addButton(14, x, y += 22, "advanced.scenes")
				.setSize(195, 20)
				.setIsEnabled(!ais.aiDisabled)
				.setHoverTexts(mess);
		addButton(15, x1, y, "advanced.marks")
				.setSize(195, 20)
				.setHoverTexts("advanced.menu.hover.marks");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		if (button.id != 5 && button.id != 8) { save(); }
		switch (button.id) {
			case 1: {
				switch (npc.advanced.animationType) {
					case PUPPET: NoppesUtil.openGUI(player, new GuiNpcPuppet(this, (EntityCustomNpc) npc)); break;
					case CUSTOM: NoppesUtil.openGUI(player, new GuiNpcAnimation((EntityCustomNpc) npc)); break;
				}
				break;
			} // edit animation
			case 2: {
				hasChanges = true;
				npc.advanced.setAnimationType(button.getValue());
				Packets.sendServer(new SPacketMenuSave(EnumMenuType.ADVANCED, npc.advanced.save(new NBTTagCompound())));
				initGui();
				break;
			} // set animation type
			case 3: Packets.sendServer(new SPacketNpcRoleGet()); break;
			case 4: Packets.sendServer(new SPacketNpcJobGet()); break;
			case 5: {
				hasChanges = true;
				int id = button.getValue();
				if (id > 8) { id ++; } // puppet is now separate
				npc.advanced.setJob(id);
				Packets.sendServer(new SPacketMenuSave(EnumMenuType.ADVANCED, npc.advanced.save(new NBTTagCompound())));
				initGui();
				break;
			} // set job
			case 7: NoppesUtil.openGUI(player, new GuiNPCLinesMenu(npc)); break;
			case 8: {
				hasChanges = true;
				npc.advanced.setRole(button.getValue());
				Packets.sendServer(new SPacketMenuSave(EnumMenuType.ADVANCED, npc.advanced.save(new NBTTagCompound())));
				getButton(3).setIsEnabled(npc.role.getType() != 0 && npc.role.getType() != 5);
				initGui();
				break;
			} // set role
			case 9: NoppesUtil.openGUI(player, new GuiNPCFactionSetup(npc)); break;
			case 10: NoppesUtil.openGUI(player, new GuiNPCDialogNpcOptions(npc)); break;
			case 11: NoppesUtil.openGUI(player, new GuiNPCSoundsMenu(npc)); break;
			case 12: NoppesUtil.openGUI(player, new GuiNPCNightSetup(npc)); break;
			case 13: NoppesUtil.openGUI(player, new GuiNPCAdvancedLinkedNpc(npc)); break;
			case 14: NoppesUtil.openGUI(player, new GuiNPCScenes(npc)); break;
			case 15: NoppesUtil.openGUI(player, new GuiNPCMarks(npc, this)); break;
			case 18: NoppesUtil.openGUI(player, new GuiNpcEmotion((EntityCustomNpc) npc)); break; // Emotion Settings
		}
	}

	@Override
	public void save() {
		if (hasChanges) {
			Packets.sendServer(new SPacketMenuSave(EnumMenuType.ADVANCED, npc.advanced.save(new NBTTagCompound())));
			hasChanges = false;
			// New from Unofficial (BetaZavr)
			Packets.sendServer(new SPacketNpcRoleSave(npc.role.save(new NBTTagCompound())));
			Packets.sendServer(new SPacketNpcJobSave(npc.job.save(new NBTTagCompound())));
			Packets.sendServer(new SPacketNpcPuppetSave(npc.puppet.save(new NBTTagCompound())));
		}
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		if (compound.hasKey("RoleData")) {
			npc.role.load(compound);
			switch (npc.role.getEnumType()) {
				case TRADER: setSubGui(new SubGuiNpcSelectTrader(((RoleTrader) npc.role).getMarketID())); break;
				case FOLLOWER: NoppesUtil.requestOpenGUI(EnumGuiType.SetupFollower); break;
				case BANK: setScreen(new GuiNpcBankSetup(npc)); break;
				case TRANSPORTER: setScreen(new GuiNpcTransporter(npc)); break;
				case COMPANION: setScreen(new GuiNpcCompanion(npc)); break;
				case DIALOG: NoppesUtil.openGUI(player, new GuiRoleDialog(npc)); break;
			}
		}
		else if (compound.hasKey("JobData")) {
			npc.job.load(compound);
			switch (npc.job.getEnumType()) {
				case BARD: NoppesUtil.openGUI(player, new GuiNpcBard(npc)); break;
				case HEALER: NoppesUtil.openGUI(player, new GuiNpcHealer(npc)); break;
				case GUARD: NoppesUtil.openGUI(player, new GuiNpcGuard(npc)); break;
				case ITEM_GIVER: NoppesUtil.requestOpenGUI(EnumGuiType.SetupItemGiver); break;
				case FOLLOWER: NoppesUtil.openGUI(player, new GuiNpcFollowerJob(npc)); break;
				case SPAWNER: NoppesUtil.openGUI(player, new GuiNpcSpawner(npc)); break;
				case CONVERSATION: NoppesUtil.openGUI(player, new GuiNpcConversation(npc)); break;
				case PUPPET: NoppesUtil.openGUI(player, new GuiNpcPuppet(this, (EntityCustomNpc) npc)); break;
				case FARMER: NoppesUtil.openGUI(player, new GuiJobFarmer(npc)); break;
			}
		}
		else if (compound.hasKey("NpcInteractLines", 10)) { npc.advanced.load(compound); }
		else if (compound.hasKey("NpcInv", 9)) { npc.inventory.load(compound); }
		else if (compound.hasKey("MovementType", 3)) { ais.load(compound); }
		initGui();
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiNpcSelectTrader) {
			hasChanges = true;
			((RoleTrader) npc.role).setMarket(((SubGuiNpcSelectTrader) subgui).id);
			save();
		}
	}

}
