package noppes.npcs.client.gui;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.containers.ContainerMerchantAdd;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMerchantSetSlot;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.GuiBasicContainer;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

import java.util.Objects;

@SideOnly(Side.CLIENT)
public class GuiMerchantAdd extends GuiBasicContainer<ContainerMerchantAdd> {

	@SideOnly(Side.CLIENT)
	protected static class MerchantButton extends GuiButtonNop {

		protected final boolean forward;

		public MerchantButton(IGuiInterface gui, int buttonId, int x, int y, boolean isForward) {
			super(gui, buttonId, "", x, y, null);
			setSize(12, 19);
			forward = isForward;
		}

		@Override
		public void renderWidget(int mouseX, int mouseY, float partialTicks) {
			if (visible) {
				Minecraft mc = Minecraft.getMinecraft();
				mc.getTextureManager().bindTexture(GuiMerchantAdd.merchantGuiTextures);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				boolean flag = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
				int k = 0;
				int l = 176;
				if (!enabled) { l += width * 2; }
				else if (flag) { l += width; }
				if (!forward) { k += height; }
				drawTexturedModalRect(x, y, l, k, width, height);
			}
		}
	}

	protected static final ResourceLocation merchantGuiTextures = new ResourceLocation("textures/gui/container/villager.png");

	protected int currentRecipeIndex;
	protected MerchantButton nextRecipeButtonIndex;
	protected MerchantButton previousRecipeButtonIndex;

	public GuiMerchantAdd(ContainerMerchantAdd containerIn) {
		super(containerIn, Component.translatable("entity.Villager.name"));
	}

	@Override
	public void initGui() {
		super.initGui();
		int guiLeft = (width - xSize) / 2;
		int guiTop = (height - ySize) / 2;
		add(nextRecipeButtonIndex = new MerchantButton(this, 1, guiLeft + 120 + 27, guiTop + 24 - 1, true));
		add(previousRecipeButtonIndex = new MerchantButton(this, 2, guiLeft + 36 - 19, guiTop + 24 - 1, false));
		addButton(4, guiLeft + xSize, guiTop + 20, "gui.remove")
				.setSize(60, 20);
		addButton(5, guiLeft + xSize, guiTop + 50, "gui.add")
				.setSize(60, 20);
		nextRecipeButtonIndex.enabled = false;
		previousRecipeButtonIndex.enabled = false;
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		boolean flag = false;
		if (button == nextRecipeButtonIndex) {
			++currentRecipeIndex;
			flag = true;
		} else if (button == previousRecipeButtonIndex) {
			--currentRecipeIndex;
			flag = true;
		}
		if (button.id == 4 && menu.trader != null) {
			MerchantRecipeList merchantrecipelist = menu.trader.getRecipes(minecraft.player);
            if (merchantrecipelist != null && currentRecipeIndex < merchantrecipelist.size()) {
				merchantrecipelist.remove(currentRecipeIndex);
				if (currentRecipeIndex > 0) { --currentRecipeIndex; }
				Packets.sendServer(new SPacketMerchantSetSlot(merchantrecipelist));
			}
		}
		if (button.id == 5 && menu.trader != null) {
			ItemStack item1 = inventorySlots.getSlot(0).getStack();
			ItemStack item2 = inventorySlots.getSlot(1).getStack();
			ItemStack sold = inventorySlots.getSlot(2).getStack();
            item1 = item1.copy();
            sold = sold.copy();
            item2 = item2.copy();
            MerchantRecipe recipe = new MerchantRecipe(item1, item2, sold);
            recipe.increaseMaxTradeUses(2147483639);
            MerchantRecipeList merchantrecipelist = menu.trader.getRecipes(minecraft.player);
			if (merchantrecipelist == null) { return; }
			merchantrecipelist.add(recipe);
			Packets.sendServer(new SPacketMerchantSetSlot(merchantrecipelist));
        }
		if (flag) {
			PacketBuffer packetbuffer = new PacketBuffer(Unpooled.buffer());
			packetbuffer.writeInt(currentRecipeIndex);
			Objects.requireNonNull(minecraft.getConnection()).sendPacket(new CPacketCustomPayload("MC|TrSel", packetbuffer));
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		minecraft.getTextureManager().bindTexture(GuiMerchantAdd.merchantGuiTextures);
		int k = (width - xSize) / 2;
		int l = (height - ySize) / 2;
		drawTexturedModalRect(k, l, 0, 0, xSize, ySize);
		if (menu.trader != null) {
			MerchantRecipeList merchantrecipelist = menu.trader.getRecipes(minecraft.player);
			if (merchantrecipelist != null && !merchantrecipelist.isEmpty()) {
				int i1 = currentRecipeIndex;
				MerchantRecipe merchantrecipe = merchantrecipelist.get(i1);
				if (merchantrecipe.isRecipeDisabled()) {
					minecraft.getTextureManager().bindTexture(GuiMerchantAdd.merchantGuiTextures);
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
					GlStateManager.disableLighting();
					drawTexturedModalRect(guiLeft + 83, guiTop + 21, 212, 0, 28, 21);
					drawTexturedModalRect(guiLeft + 83, guiTop + 51, 212, 0, 28, 21);
				}
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int par1, int par2) {
		fontRenderer.drawString(title.getFormattedText(),
				xSize / 2 - fontRenderer.getStringWidth(title.getFormattedText()) / 2, 6, CustomNpcResourceListener.DefaultTextColor);
		fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, CustomNpcResourceListener.DefaultTextColor);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (menu.trader != null) {
			MerchantRecipeList merchantrecipelist = menu.trader.getRecipes(minecraft.player);
			if (merchantrecipelist != null && !merchantrecipelist.isEmpty()) {
				int k = (width - xSize) / 2;
				int l = (height - ySize) / 2;
				int i1 = currentRecipeIndex;
				MerchantRecipe merchantrecipe = merchantrecipelist.get(i1);
				GuiBasic.renderStackInGUI(merchantrecipe.getItemToBuy(), k + 36, l + 24, 0.0f, 1.0f, "");
				GuiBasic.renderStackInGUI(merchantrecipe.getSecondItemToBuy(), k + 62, l + 24, 0.0f, 1.0f, "");
				GuiBasic.renderStackInGUI(merchantrecipe.getItemToSell(), k + 120, l + 24, 0.0f, 1.0f, "");
				if (isPointInRegion(36, 24, 16, 16, mouseX, mouseY)) { renderToolTip(merchantrecipe.getItemToBuy(), mouseX, mouseY); }
				else if (isPointInRegion(62, 24, 16, 16, mouseX, mouseY)) { renderToolTip(merchantrecipe.getSecondItemToBuy(), mouseX, mouseY); }
				else if (isPointInRegion(120, 24, 16, 16, mouseX, mouseY)) { renderToolTip(merchantrecipe.getItemToSell(), mouseX, mouseY); }

				GlStateManager.enableLighting();
				GlStateManager.enableDepth();
			}
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (menu.trader != null && nextRecipeButtonIndex != null && previousRecipeButtonIndex != null) {
			MerchantRecipeList merchantrecipelist = menu.trader.getRecipes(Minecraft.getMinecraft().player);
			if (merchantrecipelist != null) {
				nextRecipeButtonIndex.setIsEnabled(currentRecipeIndex < merchantrecipelist.size() - 1);
				previousRecipeButtonIndex.setIsEnabled(currentRecipeIndex > 0);
			}
		}
	}

}
