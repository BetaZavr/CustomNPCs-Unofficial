package noppes.npcs.client.gui.util;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.gui.INpcMenuGui;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;

import javax.annotation.Nonnull;

public abstract class GuiContainerNPCInterface2<T extends Container>
extends GuiContainerNPCInterface<T> implements INpcMenuGui {

	protected final @Nonnull GuiNpcMenu menuTabs;
	protected final ResourceLocation rightBackground = getResource("menubg.png");
	protected EnumGuiType backGui = EnumGuiType.MainMenuDisplay;
	public int menuYOffset;

	public GuiContainerNPCInterface2(EntityNPCInterface npc, T cont, Component titleIn) {
		this(npc, cont, titleIn, -1);
	}

	public GuiContainerNPCInterface2(EntityNPCInterface npc, T cont, Component titleIn, int activeMenu) {
		super(npc, cont, titleIn);
		drawDefaultBackground = false;
		menuYOffset = 0;
		xSize = 420;
		menuTabs = new GuiNpcMenu(this, activeMenu, npc);
	}

	@Override
	public void initGui() {
		super.initGui();
		if (!hasSubGui()) { menuTabs.initGui(guiLeft, guiTop + menuYOffset, xSize); }
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (!hasSubGui() && menuTabs.mouseClicked(mouseX, mouseY, mouseButton)) { return true; }
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	public void delete() {
		if (npc != null) { npc.delete(); }
		setScreen(null);
		mc.mouseHelper.grabMouseCursor();
	}

	@Override
	public void drawDefaultBackground() {
		super.drawDefaultBackground();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 256);
		mc.getTextureManager().bindTexture(rightBackground);
		drawTexturedModalRect(guiLeft + xSize - 200, guiTop, 56, 0, 200, 220);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		menuTabs.drawElements(mouseX, mouseY, partialTicks);
	}

	// New from Unofficial (BetaZavr)
	@Override
	public void onClose() {
		if (menuTabs.activeMenu != 1) {
			menuTabs.save();
			if (backGui != null && (npc != null || backGui != EnumGuiType.MainMenuDisplay)) {
				NoppesUtil.requestOpenGUI(backGui);
				return;
			}
		}
		super.onClose();
		if (menuTabs.activeMenu != 1 && npc == null && backGui == EnumGuiType.MainMenuDisplay) {
			CustomNpcs.proxy.openGui(player, EnumGuiType.NpcRemote);
		}
	}

	@Override
	public void setMenuData(boolean display, boolean stats, boolean ai, boolean inventory, boolean advanced) {
		menuTabs.permissions[0] = display;
		menuTabs.permissions[1] = stats;
		menuTabs.permissions[2] = ai;
		menuTabs.permissions[3] = inventory;
		menuTabs.permissions[4] = advanced;
    }

}
