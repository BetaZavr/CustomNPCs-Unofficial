package noppes.npcs.client.gui.util;

import net.minecraft.init.Enchantments;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuClose;
import noppes.npcs.packets.server.SPacketNpcDelete;
import noppes.npcs.packets.server.SPacketPermissionMenuGet;
import noppes.npcs.shared.client.gui.components.GuiMenuTopButton;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;

import java.util.Arrays;

public class GuiNpcMenu implements GuiYesNoCallback {

	protected final IGuiInterface parent;
	protected final EntityNPCInterface npc;
	protected final Minecraft mc;
	protected GuiMenuTopButton[] topButtons = new GuiMenuTopButton[0];
	protected int activeMenu;

	// New from Unofficial (BetaZavr)
	public boolean[] permissions = new boolean[5];

	public GuiNpcMenu(IGuiInterface gui, int activeMenuIn, EntityNPCInterface npcIn) {
		parent = gui;
		activeMenu = activeMenuIn;
		npc = npcIn;
		mc = Minecraft.getMinecraft();
		Arrays.fill(permissions, true);
		Packets.sendServer(new SPacketPermissionMenuGet());
	}

	public GuiMenuTopButton[] getTopButtons() { return topButtons; }

	public void initGui(int guiLeft, int guiTop, int width) {
		Keyboard.enableRepeatEvents(true);
		if (npc != null) {
			GuiMenuTopButton display = new GuiMenuTopButton(parent, 1, "menu.display", guiLeft + 4, guiTop - 17) {
				@Override
				public void onClick(double x, double y) {
					save();
					activeMenu = 1;
					CustomNpcs.proxy.openGui(npc, EnumGuiType.MainMenuDisplay, null);
				}
			};
			display.setHoverTexts(Component.empty()
							.append(Component.translatable("gui.name").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.literal(npc.display.getName()).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)),
					Component.empty()
							.append(Component.translatable("gui.title").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": <").withStyle(TextFormatting.GRAY))
							.append(Component.literal(npc.display.getTitle()).withStyle(TextFormatting.RESET))
							.append(Component.literal(">;").withStyle(TextFormatting.GRAY)),
					Component.empty()
							.append(Component.translatable("display.model").withStyle(TextFormatting.GRAY))
							.append(Component.literal(" ").withStyle(TextFormatting.GRAY))
							.append(Component.translatable("display.size").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.literal("" + npc.display.getSize()).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)));
			GuiMenuTopButton stats = new GuiMenuTopButton(parent, 2, "menu.stats", display.getX() + display.getWidth(), guiTop - 17) {
				@Override
				public void onClick(double x, double y) {
					save();
					activeMenu = 2;
					CustomNpcs.proxy.openGui(npc, EnumGuiType.MainMenuStats, null);
				}
			};
			String str0;
			switch (npc.stats.spawnCycle) {
				case 0: str0 = "gui.yes"; break;
				case 1: str0 = "gui.day"; break;
				case 2: str0 = "gui.night"; break;
				case 4: str0 = "stats.naturally"; break;
				default: str0 = "gui.no"; break;
			}
			stats.setHoverTexts(Component.empty()
							.append(Component.translatable("stats.health").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.literal("" + npc.stats.maxHealth).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)),
					Component.empty()
							.append(Component.translatable("stats.aggro").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.literal("" + npc.stats.aggroRange).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)),
					Component.empty()
							.append(Component.translatable("stats.respawn").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.translatable(str0).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)),
					Component.empty()
							.append(Component.translatable("stats.meleeproperties").withStyle(TextFormatting.GRAY))
							.append(Component.literal(" ").withStyle(TextFormatting.GRAY))
							.append(Component.translatable("stats.meleestrength").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.translatable("" + npc.stats.melee.getStrength()).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)),
					Component.empty()
							.append(Component.translatable("stats.rangedproperties").withStyle(TextFormatting.GRAY))
							.append(Component.literal(" ").withStyle(TextFormatting.GRAY))
							.append(Component.translatable(Enchantments.POWER.getName()).withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.translatable("" + npc.stats.ranged.getStrength()).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)));
			GuiMenuTopButton ai = new GuiMenuTopButton(parent, 3, "menu.ai", stats.getX() + stats.getWidth(), guiTop - 17) {
				@Override
				public void onClick(double x, double y) {
					save();
					activeMenu = 3;
					CustomNpcs.proxy.openGui(npc, EnumGuiType.MainMenuAI, null);
				}
			};
			switch (npc.ais.onAttack) {
				case 0: str0 = "gui.retaliate"; break;
				case 1: str0 = "gui.panic"; break;
				case 2: str0 = "gui.retreat"; break;
				default: str0 = "gui.nothing"; break;
			}
			String str1;
			switch (npc.ais.getMovingType()) {
				case 0: str1 = "ai.standing"; break;
				case 1: str1 = "ai.wandering"; break;
				default: str1 = "ai.movingpath"; break;
			}
			ai.setHoverTexts(Component.empty()
							.append(Component.translatable("ai.enemyresponse").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.translatable(str0).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)),
					Component.empty()
							.append(Component.translatable("movement.type").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.translatable(str1).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)),
					Component.empty()
							.append(Component.translatable("stats.movespeed").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.literal("" + npc.ais.getWalkingSpeed()).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)));
			GuiMenuTopButton inv = new GuiMenuTopButton(parent, 4, "menu.inventory", ai.getX() + ai.getWidth(), guiTop - 17) {
				@Override
				public void onClick(double x, double y) {
					save();
					activeMenu = 4;
					NoppesUtil.requestOpenGUI(EnumGuiType.MainMenuInv);
				}
			};
			inv.setHoverTexts(Component.empty()
							.append(Component.translatable("quest.exp").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.literal("" + npc.inventory.getExpMin()).withStyle(TextFormatting.RESET))
							.append(Component.literal("/").withStyle(TextFormatting.GRAY))
							.append(Component.literal("" + npc.inventory.getExpMax()).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)),
					Component.empty()
							.append(Component.translatable("questlog.all.reward").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.literal("" + npc.inventory.drops.size()).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)));
			GuiMenuTopButton advanced = new GuiMenuTopButton(parent, 5, "menu.advanced", inv.getX() + inv.getWidth(), guiTop - 17) {
				@Override
				public void onClick(double x, double y) {
					save();
					activeMenu = 5;
					CustomNpcs.proxy.openGui(npc, EnumGuiType.MainMenuAdvanced, null);
				}
			};
			advanced.setHoverTexts(Component.empty()
							.append(Component.translatable("role.name").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(npc.role.getEnumType().name.copy().withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)),
					Component.empty()
							.append(Component.translatable("job.name").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(npc.job.getEnumType().name.copy().withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)),
					Component.empty()
							.append(Component.translatable("menu.factions").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.translatable(npc.getFaction().name).withStyle(TextFormatting.RESET))
							.append(Component.literal(";").withStyle(TextFormatting.GRAY)));
			GuiMenuTopButton global = new GuiMenuTopButton(parent, 6, "menu.global", advanced.getX() + advanced.getWidth(), guiTop - 17) {
				@Override
				public void onClick(double x, double y) {
					save();
					activeMenu = 6;
					CustomNpcs.proxy.openGui(npc, EnumGuiType.MainMenuGlobal, null);
				}
			};
			GuiMenuTopButton close = new GuiMenuTopButton(parent, 0, "X", guiLeft + width - 22, guiTop - 17) {
				@Override
				public void onClick(double x, double y) { GuiNpcMenu.this.close(); }
			};
			GuiMenuTopButton delete = new GuiMenuTopButton(parent, 66, Component.translatable("selectServer.delete"), guiLeft + width - 72, guiTop - 17) {
				@Override
				public void onClick(double x, double y) {
					GuiYesNo guiyesno = new GuiYesNo(GuiNpcMenu.this, "",
							Component.translatable("message.delete",
									npc.getDisplayName().getFormattedText()).getFormattedText(), 0);
					mc.displayGuiScreen(guiyesno);
				}
			};
			delete.setX(close.getX() - delete.getWidth());
			topButtons = new GuiMenuTopButton[] { display, stats, ai, inv, advanced, global, close, delete };
			for (GuiMenuTopButton button : topButtons) { button.active = button.id == activeMenu; }
		}
		else {
			GuiMenuTopButton close = new GuiMenuTopButton(parent, 0, "X", guiLeft + width - 22, guiTop - 17) {
				@Override
				public void onClick(double x, double y) { GuiNpcMenu.this.close(); }
			};
			topButtons = new GuiMenuTopButton[] { close };
		}
	}

	public void save() {
		GuiTextFieldNop.unfocus();
		parent.save();
	}

	private void close() {
		Keyboard.enableRepeatEvents(false);
		if (parent instanceof GuiContainerNPCInterface2<?>) { ((GuiContainerNPCInterface2<?>) parent).backGui = null; }
		else if (parent instanceof GuiNPCInterface2) { ((GuiNPCInterface2) parent).backGui = null; }
		if (parent != null) { parent.onClose(); }
		if (npc != null) {
			npc.reset();
			Packets.sendServer(new SPacketMenuClose());
		}
		else { CustomNpcs.proxy.openGui(mc.player, EnumGuiType.NpcRemote); }
	}

	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (mouseButton == 0) {
			for (GuiMenuTopButton button : topButtons) {
				if (button.mouseClicked(mouseX, mouseY, mouseButton)) {
					return true;
				}
			}
		}
		return false;
	}

	public void drawElements(int mouseX, int mouseY, float partialTicks) {
		for (GuiMenuTopButton button : getTopButtons()) { button.render(mouseX, mouseY, partialTicks); }
	}

	@Override
	public void confirmClicked(boolean flag, int id) {
		Minecraft mc = Minecraft.getMinecraft();
		if (flag) {
			Packets.sendServer(new SPacketNpcDelete());
			mc.displayGuiScreen(null);
			mc.setIngameFocus();
		}
		else { NoppesUtil.openGUI(mc.player, parent); }
	}

}
