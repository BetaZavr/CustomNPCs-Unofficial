package noppes.npcs.client.gui.player;

import net.minecraft.client.gui.recipebook.GuiRecipeBook;
import net.minecraft.client.gui.recipebook.IRecipeShownListener;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.containers.ContainerCarpentryBench;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

import javax.annotation.Nonnull;

// Changed by Unofficial (BetaZavr)
// net.minecraft.client.gui.inventory.GuiInventory
public class GuiNpcCarpentryBench
extends GuiContainerNPCInterface<ContainerCarpentryBench>
implements IRecipeShownListener {

	protected static final ResourceLocation resource = getResource("carpentry.png");
	protected GuiButtonNop button;

	protected final ResourceLocation buttonTexture = new ResourceLocation("minecraft", "textures/gui/container/crafting_table.png");
	protected final ContainerCarpentryBench menu;
	// from GuiCrafting
	protected final GuiRecipeBook recipeBookGui = new GuiRecipeBook();
	protected boolean widthTooNarrow;

	public GuiNpcCarpentryBench(ContainerCarpentryBench container) {
		super(null, container, Component.empty());
		ySize = 180;

		menu = container;
		allowUserInput = false;
	}

	@Override
	public void initGui() {
		super.initGui();
		widthTooNarrow = width < 379;
		recipeBookGui.func_194303_a(width, height, mc, widthTooNarrow, menu.craftMatrix);
		guiLeft = recipeBookGui.updateScreenPosition(widthTooNarrow, width, xSize);
		button = new GuiButtonNop(this, 10, "", guiLeft + 5, height / 2 - 49, (b) -> {
			recipeBookGui.initVisuals(widthTooNarrow, menu.craftMatrix);
			recipeBookGui.toggleVisibility();
			guiLeft = recipeBookGui.updateScreenPosition(widthTooNarrow, width, xSize);
			b.setX(guiLeft + 5);
			b.setY(height / 2 - 49);
		})
				.setSize(20, 19)
				.setTexture(buttonTexture)
				.setUV(0, 168, 20 ,19);
		button.isSimple = true;
		add(button);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		GlStateManager.pushMatrix();
		mc.getTextureManager().bindTexture(resource);
		menu.checkPos(recipeBookGui.isVisible());
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		fontRenderer.drawString(Component.translatable("tile.npccarpentybench.name").getString(), guiLeft + 4, guiTop + 4, CustomNpcResourceListener.DefaultTextColor);
		fontRenderer.drawString(Component.translatable("container.inventory").getString(), guiLeft + 4, guiTop + 87, CustomNpcResourceListener.DefaultTextColor);
		GlStateManager.popMatrix();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (recipeBookGui.isVisible() && widthTooNarrow) {
			drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
			recipeBookGui.render(mouseX, mouseY, partialTicks);
		}
		else {
			super.drawScreen(mouseX, mouseY, partialTicks);
			recipeBookGui.render(mouseX, mouseY, partialTicks);
			recipeBookGui.renderGhostRecipe(guiLeft - 77, guiTop, true, partialTicks);
		}
		renderHoveredToolTip(mouseX, mouseY);
		recipeBookGui.renderTooltip(guiLeft, guiTop, mouseX, mouseY);
	}

	@Override
	public @Nonnull GuiRecipeBook func_194310_f() { return recipeBookGui; }

	@Override
	protected void handleMouseClick(@Nonnull Slot slotIn, int slotId, int mouseButton, @Nonnull ClickType type) {
		super.handleMouseClick(slotIn, slotId, mouseButton, type);
		recipeBookGui.slotClicked(slotIn);
	}

	@Override
	protected boolean hasClickedOutside(int mouseX, int mouseY, int left, int top) {
		boolean flag = mouseX < left || mouseY < top || mouseX >= left + xSize || mouseY >= top + ySize;
		return recipeBookGui.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, xSize, ySize) && flag;
	}

	@Override
	protected boolean isPointInRegion(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
		return (!widthTooNarrow || !recipeBookGui.isVisible()) && super.isPointInRegion(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
	}

	@Override
	public boolean keyPressed(char typedChar, int keyCode) {
		if (!hasSubGui() && recipeBookGui.keyPressed(typedChar, keyCode)) { return true; }
		return super.keyPressed(typedChar, keyCode);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (!hasSubGui() && (widthTooNarrow || recipeBookGui.isVisible()) &&
				recipeBookGui.mouseClicked((int) mouseX, (int) mouseY, mouseButton)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void onGuiClosed() {
		recipeBookGui.removed();
		super.onGuiClosed();
	}

	@Override
	public void recipesUpdated() { recipeBookGui.recipesUpdated();}

    @Override
	public void updateScreen() {
		super.updateScreen();
		recipeBookGui.tick();
	}

}
