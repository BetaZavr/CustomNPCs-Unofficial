package noppes.npcs.client.gui.player;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.handler.data.IForbiddenItem;
import noppes.npcs.containers.ContainerAuctionCreateLot;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerGameData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketAuctionCreateLot;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.GuiBasicContainer;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

public class SubGuiNpcAuctionCreateLot extends GuiBasicContainer<ContainerAuctionCreateLot>
		implements ISliderListener, ITextfieldListener {

	public static GuiNpcAuction parent;
	protected final ResourceLocation slots;
	protected ItemStack lastStack = ItemStack.EMPTY;
	protected int commission = 0;
	protected boolean canCreate;
	protected int count = 1;
	// prise
	protected GuiTextFieldNop prise;
	protected boolean costIsSet = false;
	protected int[] slotPos = new int[2];
	protected boolean heldIsForbidden = false;

	public SubGuiNpcAuctionCreateLot(ContainerAuctionCreateLot container) {
		super(container, Component.empty());
		background = new ResourceLocation(CustomNpcs.MODID, "textures/gui/auctions/background_" + container.auction.getTextureType() + ".png");
		widthTexture = 256;
		heightTexture = 256;
		xSize = 256;
		ySize = 203;

		slots = new ResourceLocation(CustomNpcs.MODID, "textures/gui/auctions/inventory_" + container.auction.getTextureType() + ".png");
	}

	@Override
	public void initGui() {
		super.initGui();
		slotPos[0] = guiLeft + 176;
		slotPos[1] = guiTop + 101;
		int lId = 0;
		int x0 = guiLeft + 5;
		int w0 = 80;
		int x1 = x0 + w0 + 2;
		int y = guiTop + 4;
		int color = CustomNpcs.MainColor.getRGB();
		Component title = Component.translatable("auction.create").append(". ").withStyle(TextFormatting.YELLOW)
				.append(Component.translatable("auction.house",
						Component.translatable(menu.auction.name).withStyle(TextFormatting.RESET).getFormattedText())
				.withStyle(TextFormatting.YELLOW));
		addLabel(lId++, x0 + (xSize - minecraft.fontRenderer.getStringWidth(title.getString())) / 2, y, title)
				.setSize(xSize - 10, 10)
				.setColor(color);
		// price
		addLabel(lId++, x0, y += 11, Component.translatable("auction.buyout.price").append(":"))
				.setSize(w0, 10)
				.setColor(color);
		prise = addTextField(0, x1 + 1, y, w0, 12, menu.lot.getBuyoutPrice())
				.setMinMaxDefault(0, Integer.MAX_VALUE, menu.lot.getBuyoutPrice())
				.setMaxStringLength(12);
		// duration
		addLabel(lId++, x0, y += 16, Component.translatable("gui.time").append(" (")
				.append(Component.translatable("mailbox.minutes")).append("):"))
				.setSize(w0, 10)
				.setColor(color);
		long duration = menu.lot.getDuration() / 60000L;
		float a = -1.0f / (float) ((menu.auction.getDelays()[0] - menu.auction.getDelays()[1]) / 60000L);
		float b = a * (float) (menu.auction.getDelays()[0] / -60000L);
		addTextField(1, x1 + 1, y, 50, 12, duration)
				.setMinMaxDefault(menu.auction.getDelays()[0] / 60000L, menu.auction.getDelays()[1] / 60000L, duration)
				.setMaxStringLength(12)
				.setHoverTexts("auction.manage.hover.duration");
		addSlider(0, x1 + 54, y + 1, a * duration - b)
				.setSize(110, 10)
				.setHoverTexts("auction.manage.hover.duration");
		commission = menu.auction.calculateCommission(menu.lot.getBuyoutPrice(), menu.lot.getDuration());
		// can buyout
		addCheckBox(0, x0, y += 15, "auction.can.buyout.true", "auction.can.buyout.false", menu.lot.canBuyout())
				.setSize(w0, 14)
				.setColor(color)
				.setHoverTexts("auction.hover.can.buyout");
		// currency type
		boolean bo = menu.lot.getCurrencyType();
		if (menu.auction.getCurrencyType() == 1) { bo = true; }
		else if (menu.auction.getCurrencyType() == 2) { bo = false; }
		addCheckBox(1, x1, y,
				Component.translatable("market.currency").append(" ").append(Component.translatable("gui.money")),
				Component.translatable("market.currency").append(" ").append(Component.translatable("gui.donat")),
				bo)
				.setIsEnabled(menu.auction.getCurrencyType() == 0)
				.setColor(color);
		// description
		addLabel(lId++, x0, y += 15, Component.translatable("auction.description").append(":"))
				.setSize(xSize - 10, 10)
				.setColor(color);
		addTextField(2, x0 + 1, y += 12, xSize - 12, 12, menu.lot.getDescription())
				.setHoverTexts("auction.hover.description")
				.setMaxStringLength(1000);
		// inventory
		addLabel(lId++, x0, y += 16, "container.inventory")
				.setSize(xSize - 10, 10)
				.setColor(color);
		// commission
		addLabel(lId, x0 + 167, y + 32, Component.translatable("auction.manage.commission").append(":"))
				.setSize(xSize - 10, 10)
				.setColor(color);
		// max count
		int x = guiLeft + xSize - 65;
		y = guiTop + ySize - 22;
		addCheckBox(3, x - 52, y + 1, "gui.max", null, false)
				.setSize(50, 14)
				.setColor(color)
				.setIsVisible(!menu.lot.getStack().isEmpty())
				.setHoverTexts(Component.translatable("auction.hover.create.max", count));
		/* TODO: addTextField Count */
		// done
		ResourceLocation bt = new ResourceLocation(CustomNpcs.MODID, "textures/gui/auctions/buttons_" + menu.auction.getTextureType() + ".png");
		addButton(2, x, y, "gui.done")
				.setSize(60, 16)
				.setTexture(bt)
				.setUV(0, 0, 256, 28)
				.setIsAnim(true);
		addButton(66, x0, y, "gui.cancel")
				.setSize(60, 16)
				.setTexture(bt)
				.setUV(0, 0, 256, 28)
				.setIsAnim(true)
				.setHoverTexts("hover.back");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: menu.lot.setCanBuyout(((GuiCheckBoxNop) button).selected()); break;
			case 1: {
				boolean bo = ((GuiCheckBoxNop) button).selected();
				if (menu.auction.getCurrencyType() == 1) { bo = true; }
				else if (menu.auction.getCurrencyType() == 2) { bo = false; }
				menu.lot.setCurrencyType(bo);
				break;
			} // currency type
			case 2: {
				if (canCreate || (player.isCreative() && !menu.lot.getStack().isEmpty() && menu.lot.getBuyoutPrice() > 0)) {
					onClose();
					CustomNPCsScheduler.runTack(() -> Packets.sendServer(new SPacketAuctionCreateLot(menu.auction.getId(), menu.lot, count)), 250);
				}
				break;
			} // create
			case 3: resetCount(); break; // create max
			case 66: onClose(); break;
		}
	}

	@Override
	public void onClose() {
		super.onClose();
		parent.createLot = false;
		CustomNpcs.proxy.openGui(player, parent);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		// slots
		minecraft.getTextureManager().bindTexture(slots);
		// lot item
		if (prise != null) {
			boolean bo = NoppesUtilPlayer.compareItems(menu.lot.getStack(), lastStack, true, true);
			if (!bo || menu.lot.getStack().getCount() != lastStack.getCount()) {
				lastStack = menu.lot.getStack().copy();
				if (lastStack.isEmpty()) { costIsSet = false; }
				if (!costIsSet) {
					menu.lot.setBuyoutPrice(AuctionController.getInstance().getAveragePrice(lastStack));
					prise.setValue(menu.lot.getBuyoutPrice());
					commission = menu.auction.calculateCommission(menu.lot.getBuyoutPrice(), menu.lot.getDuration());
				}
				resetCount();
			}
		}
		GlStateManager.pushMatrix();
		Slot slot = menu.getSlot(0);
		GlStateManager.translate( guiLeft + slot.xPos - 2, guiTop + slot.yPos - 2, 0);
		drawTexturedModalRect(0, 0, 0, 0, 164, 78);
		GlStateManager.popMatrix();
		GlStateManager.pushMatrix();
		// price slot
		GlStateManager.translate(slotPos[0], slotPos[1], 0);
		drawTexturedModalRect(0, 0, 0, 58, 18, 20);
		drawTexturedModalRect(18, 0, 162, 58, 2, 20);
		GlStateManager.popMatrix();
		// forbidden items:
		IForbiddenItem[] fis = menu.auction.getForbiddenItems();
		heldIsForbidden = false;
		if (fis.length > 0) {
			for (Slot s : menu.inventorySlots) {
				if (s.getHasStack()) {
					GlStateManager.pushMatrix();
					GlStateManager.translate( guiLeft - 1.0f, guiTop - 1.0f, 0.0f);
					for (IForbiddenItem fi : fis) {
						if (NoppesUtilPlayer.compareItems(s.getStack(), fi.getItem().getMCItemStack(), fi.isIgnoreDamage(), fi.isIgnoreNBT())) {
							drawTexturedModalRect(s.xPos, s.yPos, 164, 0, 18, 18);
						}
					}
					GlStateManager.popMatrix();
				}
			}
			ItemStack heldStack = player.inventory.getItemStack();
			if (!heldStack.isEmpty()) {
				for (IForbiddenItem fi : fis) {
					if (NoppesUtilPlayer.compareItems(heldStack, fi.getItem().getMCItemStack(), fi.isIgnoreDamage(), fi.isIgnoreNBT())) {
						// held
						GlStateManager.pushMatrix();
						GlStateManager.translate(mouseX - 8.0f, mouseY - 8.0f, 400.0f);
						drawTexturedModalRect(0, 0, 164, 0, 18, 18);
						GlStateManager.popMatrix();
						// slot
						GlStateManager.pushMatrix();
						GlStateManager.translate(slotPos[0] + 1.0f, slotPos[1] + 1.0f, 300.0f);
						drawTexturedModalRect(0, 0, 164, 0, 18, 18);
						GlStateManager.popMatrix();
						heldIsForbidden = true;
						break;
					}
				}
			}
		}
		if (isMouseHover(mouseX, mouseY, slotPos[0], slotPos[1], 16, 16)) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(slotPos[0] + 2.0f, slotPos[1] + 2.0f, 0.0f);
			drawRect(0, 0, 16, 16, 0x80FFFFFF);
			GlStateManager.popMatrix();
			if (heldIsForbidden) { setHoverText("auction.hover.err.item"); }
			else if (!menu.lot.getStack().isEmpty()) {
				setHoverText(menu.lot.getStack().getTooltip(player,
					minecraft.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL));
			}
		}
		if (!menu.lot.getStack().isEmpty()) {
			GuiBasic.renderStackInGUI(menu.lot.getStack(), slotPos[0] + 2.0f, slotPos[1] + 2.0f, 0.0f, 1.0f, null);
		}
		// done + currency
		GuiButtonNop button = getButton(2);
		String currency = menu.lot.getCurrencyType() ? CustomNpcs.displayCurrencies : CustomNpcs.displayDonation;
		ResourceLocation resource = menu.lot.getCurrencyType() ? GuiBasic.MONEY : GuiBasic.DONAT;
		if (button != null && prise != null) {
			Component hover = Component.translatable("auction.hover.create",
					Component.translatable(menu.auction.getName()).getFormattedText());
			PlayerGameData gameData = PlayerData.get(player).game;
			canCreate = true;
			if ((menu.lot.getCurrencyType() ? gameData.getMoney() : gameData.getDonat()) < commission) {
				hover.append("<br>")
						.append(Component.translatable("market.hover.notbuy." + (menu.lot.getCurrencyType() ? "3" : "7")))
						.append(TextFormatting.RED + ": " + TextFormatting.GOLD + commission + TextFormatting.RESET + currency);
				if (player.isCreative()) {
					hover.append(TextFormatting.RED + ". [")
							.append(Component.translatable("gui.allowed").withStyle(TextFormatting.RESET))
							.append(TextFormatting.RED + "]");
				}
				canCreate = false;
			}
			if (menu.lot.getBuyoutPrice() == 0) {
				hover.append("<br>").append(Component.translatable("auction.hover.create.err.price"));
				canCreate = false;
			}
			if (menu.lot.getStack().isEmpty()) {
				hover.append("<br>").append(Component.translatable("auction.hover.create.err.item"));
				canCreate = false;
			}
			button.setIsEnabled(player.isCreative() || canCreate)
					.setHoverTexts(hover);
			prise.setHoverTexts(Component.translatable("auction.hover.buyout", 0, currency));
			// currency
			GlStateManager.pushMatrix();
			GlStateManager.translate(prise.getX() + prise.getWidth() + 12, prise.getY() + 1, 0);
			font.drawString(currency, 0, 0, CustomNpcs.MainColor.getRGB());
				GlStateManager.pushMatrix();
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				GlStateManager.translate( 4.5f, -1.5f, 0.0f);
				float scale = 12.0f / 256.0f;
				GlStateManager.scale(scale, scale, scale);
				minecraft.getTextureManager().bindTexture(resource);
				drawTexturedModalRect(0, 0, 0, 0, 256, 256);
				GlStateManager.popMatrix();
			GlStateManager.popMatrix();
		}
		// commission
		currency = commission + " " + currency;
		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLeft + 172.0f, guiTop + 132.0f, 0.0f);
		font.drawString(currency, 0, 0, CustomNpcs.MainColor.getRGB());
		GlStateManager.pushMatrix();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		GlStateManager.translate((float) font.getStringWidth(currency), -1.5f, 0.0f);
		float scale = 12.0f / 256.0f;
		GlStateManager.scale(scale, scale, scale);
		minecraft.getTextureManager().bindTexture(resource);
		drawTexturedModalRect(0, 0, 0, 0, 256, 256);
		GlStateManager.popMatrix();
		GlStateManager.popMatrix();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (!heldIsForbidden) {
			if (isMouseHover(mouseX, mouseY, slotPos[0], slotPos[1], 16, 16)) {
				ItemStack stack = menu.lot.getStack();
				ItemStack heldStack = player.inventory.getItemStack();
				if (mouseButton == 0) { menu.lot.setStack(heldStack.copy()); }
				else if (mouseButton == 1) {
					if (stack.isEmpty()) {
						menu.lot.setStack(heldStack.copy());
						menu.lot.getStack().setCount(1);
					}
					else if (stack.getCount() < stack.getMaxStackSize() &&
							(heldStack.isEmpty() ||
									NoppesUtilPlayer.compareItems(stack, player.inventory.getItemStack(), true, true))) {
						menu.lot.getStack().grow(1);
					}
				}
				else if (mouseButton == 2 && !stack.isEmpty()) { menu.lot.getStack().setCount(1); }
				return true;
			}
			if (GuiScreen.isShiftKeyDown()) {
				Slot hoveredSlot = getSlotUnderMouse();
				if (hoveredSlot != null && !hoveredSlot.getStack().isEmpty()) {
					menu.lot.setStack(hoveredSlot.getStack().copy());
				}
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void mouseDragged(GuiSliderNop slider) {
		float a = (float) ((menu.auction.getDelays()[1] - menu.auction.getDelays()[0]) / 60000L);
		float b = (float) (menu.auction.getDelays()[0] / 60000L);
		menu.lot.setDuration((long) (a * slider.getValue() + b) * 60000L);
		commission = menu.auction.calculateCommission(menu.lot.getBuyoutPrice(), menu.lot.getDuration());
		slider.setMessage(Component.literal(
				Util.instance.ticksToElapsedTime(menu.lot.getDuration() / 50L, false, false, false)));
		GuiTextFieldNop tf = getTextField(1);
		if (tf != null) { tf.setValue(menu.lot.getDuration() / 60000L); }
	}

	@Override
	public void mousePressed(GuiSliderNop slider) { }

	@Override
	public void mouseReleased(GuiSliderNop slider) { }

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 0: {
				menu.lot.setBuyoutPrice(textField.getInteger());
				commission = menu.auction.calculateCommission(menu.lot.getBuyoutPrice(), menu.lot.getDuration());
				costIsSet = true;
				break;
			}
			case 1: {
				GuiSliderNop slider = getSlider(0);
				menu.lot.setDuration(textField.getLong() * 60000L);
				commission = menu.auction.calculateCommission(menu.lot.getBuyoutPrice(), menu.lot.getDuration());
				if (slider != null) {
					long duration = menu.lot.getDuration() / 60000L;
					float a = -1.0f / (float) ((menu.auction.getDelays()[0] - menu.auction.getDelays()[1]) / 60000L);
					float b = a * (float) (menu.auction.getDelays()[0] / -60000L);
					slider.setValue(a * duration + b);
					slider.setMessage(Component.literal(
							Util.instance.ticksToElapsedTime(menu.lot.getDuration() / 50L, false, false, false)));
				}
				break;
			} // duration
			case 2: menu.lot.setDescription(textField.getValue()); break;
		}
	}

	private void resetCount() {
		count = 1;
		GuiButtonNop button = getButton(3);
		if (button != null) {
			ItemStack select = menu.lot.getStack();
			button.setIsVisible(!select.isEmpty());
			if (button.isVisible() && ((GuiCheckBoxNop) button).selected() && !select.isEmpty()) {
				// stack size
				int size = Util.instance.inventoryItemCount(player, select, null, true, true);
				if (NoppesUtilPlayer.compareItems(select, player.inventory.getItemStack(), true, true)) {
					size += player.inventory.getItemStack().getCount();
				}
				int countIn = (int) Math.floor((float) size / (float) select.getCount());
				if (!player.isCreative()) {
					// currency
					if (countIn > 1) {
						PlayerGameData gameData = PlayerData.get(player).game;
						double currency = (menu.lot.getCurrencyType() ? gameData.getMoney() : gameData.getDonat());
						countIn = Math.min(countIn, (int) Math.floor(currency / (double) (menu.lot.getBuyoutPrice() + commission)));
					}
					// lots
					if (countIn > 1 && menu.auction.getMaxLotsInPlayer() > 0) {
						countIn = Math.min(countIn, menu.auction.getMaxLotsInPlayer() - menu.auction.getCountPlayerLots(player.getName()));
					}
				}
				count = Math.max(1, countIn);
			}
			button.setHoverTexts(Component.translatable("auction.hover.create.max", count));
		}
		button = getButton(2);
		if (button != null) {
			if (count > 1) { button.setMessage(Component.translatable("gui.done").append(" x" + count)); }
			else { button.setMessage(Component.translatable("gui.done")); }
		}
	}

}
