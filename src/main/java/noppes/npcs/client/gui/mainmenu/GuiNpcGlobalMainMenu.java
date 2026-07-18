package noppes.npcs.client.gui.mainmenu;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.global.GuiNpcManageMarkets;
import noppes.npcs.client.gui.global.GuiNpcDialogGuiSettings;
import noppes.npcs.client.gui.global.GuiNpcManagePlayerData;
import noppes.npcs.client.gui.global.GuiNpcNaturalSpawns;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.yellow_de.data.UtilYDE;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketPermissionGlobalGet;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class GuiNpcGlobalMainMenu extends GuiNPCInterface2 {

	// New from Unofficial (BetaZavr)
	public boolean[] permissions = new boolean[12];

	public GuiNpcGlobalMainMenu(EntityNPCInterface npc) {
		super(npc, 6);
		Arrays.fill(permissions, true);
		Packets.sendServer(new SPacketPermissionGlobalGet());
	}

	@Override
	public void initGui() {
		super.initGui();
		int r0 = guiLeft + 75;
		int r1 = guiLeft + 240;
		int y = guiTop + 10;
		String notEdit = "hover.not.edit";
		addButton(2, r0, y, "global.banks")
				.setSize(110, 20)
				.setHoverTexts("global.hover.banks", !permissions[0] ? notEdit : null);
		addButton(3, r0, (y += 22), "menu.factions")
				.setSize(110, 20)
				.setHoverTexts("global.hover.factions", !permissions[1] ? notEdit : null);
		addButton(4, r0, (y += 22), "dialog.dialogs")
				.setSize(110, 20)
				.setHoverTexts("global.hover.dialogs", !permissions[2] ? notEdit : null);
		addButton(5, r0 + 112, y, "GUI")
				.setSize(20, 20)
				.setHoverTexts("global.hover.dialogs.gui", !permissions[2] ? notEdit : null);
		addButton(20, r0 + 120, y + 22, "global.game.edit")
				.setSize(44, 20)
				.setIsEnabled(player.getName().contains("BetaZavr"))
				.setHoverTexts("global.hover.game.edit", "gui.wip", !permissions[2] || !permissions[3] ? notEdit : null);
		addButton(11, r0, (y += 22), "quest.quests")
				.setSize(110, 20)
				.setHoverTexts("global.hover.quests", !permissions[3] ? notEdit : null);
		addButton(12, r0, (y += 22), "global.transport")
				.setSize(110, 20)
				.setHoverTexts("global.hover.transports", !permissions[4] ? notEdit : null);
		addButton(13, r0, (y += 22), "global.playerdata")
				.setSize(110, 20)
				.setHoverTexts("global.hover.playerdatas", !permissions[5] ? notEdit : null);
		addButton(14, r0, (y += 22), "global.recipes")
				.setSize(110, 20)
				.setHoverTexts("global.hover.recipes", !permissions[6] ? notEdit : null);
		addButton(15, r0, (y += 22), Component.translatable("global.naturalspawn")
				.append(" ")
				.append(Component.translatable("gui.deprecated")))
				.setSize(110, 20)
				.setHoverTexts("global.hover.naturalspawns", !permissions[7] ? notEdit : null);
		addButton(16, r0, y + 22, "global.linked")
				.setSize(110, 20)
				.setHoverTexts("global.hover.linkeds", !permissions[8] ? notEdit : null);
		// New from Unofficial (BetaZavr)
		y = guiTop + 10;
		addButton(17, r1, y, "global.market")
				.setSize(110, 20)
				.setHoverTexts("global.hover.markets", !permissions[9] ? notEdit : null);
		addButton(18, r1, (y += 22), "global.auctions")
				.setSize(110, 20)
				.setIsEnabled(false)
				.setHoverTexts("global.hover.auctions", !permissions[10] ? notEdit : null, "gui.wip");
		addButton(19, r1, y + 22, "global.mail")
				.setSize(110, 20)
				.setIsEnabled(false)
				.setHoverTexts("global.hover.mail", !permissions[11] ? notEdit : null);
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		switch (button.id) {
			case 2: NoppesUtil.requestOpenGUI(EnumGuiType.ManageBanks); break;
			case 3: NoppesUtil.requestOpenGUI(EnumGuiType.ManageFactions); break;
			case 4: NoppesUtil.requestOpenGUI(EnumGuiType.ManageDialogs); break;
			case 5: NoppesUtil.openGUI(player, new GuiNpcDialogGuiSettings(npc)); break;
			case 11: NoppesUtil.requestOpenGUI(EnumGuiType.ManageQuests); break;
			case 12: NoppesUtil.requestOpenGUI(EnumGuiType.ManageTransport, new BlockPos(-1, -1, 0)); break;
			case 13: NoppesUtil.openGUI(player, new GuiNpcManagePlayerData(npc)); break;
			case 14: NoppesUtil.requestOpenGUI(EnumGuiType.ManageRecipes, new BlockPos(3, 0, 0)); break;
			case 15: NoppesUtil.openGUI(player, new GuiNpcNaturalSpawns(npc)); break;
			case 16: NoppesUtil.requestOpenGUI(EnumGuiType.ManageLinked); break;
			case 17: NoppesUtil.openGUI(player, new GuiNpcManageMarkets(npc)); break;
			case 18: break; // Auctions
			case 19: NoppesUtil.requestOpenGUI(EnumGuiType.ManageMail); break;
			case 20: NoppesUtil.requestOpenGUI(EnumGuiType.ManageGame); break;
		}
	}


	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		GuiButtonNop buttonG = getButton(20); // game.edit
		if (buttonG != null && buttonG.isEnabled()) {
			// dialogs
			GuiButtonNop buttonD = getButton(4);
			GlStateManager.pushMatrix();
			float[] p1 = new float[] { buttonG.getX(), buttonG.getY() + buttonG.getHeight() / 2.0f };
			if (buttonD != null) {
				boolean hovered = buttonG.isHoveredOrFocused() || buttonD.isHoveredOrFocused();
				float[] p0 = new float[] { buttonD.getX() + buttonD.getWidth(), buttonD.getY() + buttonD.getHeight() / 2.0f };
				UtilYDE.renderDot(p0, 0.5f, hovered, 0x184EB0);
				UtilYDE.renderDot(p1, 0.5f, hovered, 0x184EB0);
				UtilYDE.renderSpline(p0, p1, hovered, false, 0x184EB0, 0.0f);
			}
			// quests
			GuiButtonNop buttonQ = getButton(11);
			if (buttonQ != null) {
				boolean hovered = buttonG.isHoveredOrFocused() || buttonQ.isHoveredOrFocused();
				float[] p0 = new float[] { buttonQ.getX() + buttonQ.getWidth(), buttonQ.getY() + buttonQ.getHeight() / 2.0f };
				UtilYDE.renderDot(p0, 0.5f, hovered, 0xAEB018);
				UtilYDE.renderDot(p1, 0.5f, hovered, 0xAEB018);
				UtilYDE.renderSpline(p0, p1, hovered, false, 0xAEB018, 0.0f);
			}
			GlStateManager.popMatrix();
		}
	}

	@Override
	public void save() { }

	public void setMenuData(boolean banks, boolean factions, boolean dialogs, boolean quests, boolean transports,
							boolean playersData, boolean recipes, boolean naturalSpawns, boolean linkeds, boolean markets,
							boolean auctions, boolean mails) {
		permissions[0] = banks;
		permissions[1] = factions;
		permissions[2] = dialogs;
		permissions[3] = quests;
		permissions[4] = transports;
		permissions[5] = playersData;
		permissions[6] = recipes;
		permissions[7] = naturalSpawns;
		permissions[8] = linkeds;
		permissions[9] = markets;
		permissions[10] = auctions;
		permissions[11] = mails;
		initGui();
	}

}
