package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

import javax.annotation.Nonnull;

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

    public SubGuiNpcAuctionCreateLot(ContainerAuctionCreateLot container, Inventory inv, Component ignoredTitle) {
        super(container, inv, Component.empty());
        background = new ResourceLocation(CustomNpcs.MODID, "textures/gui/auctions/background_" + container.auction.getTextureType() + ".png");
        widthTexture = 256;
        heightTexture = 256;
        imageWidth = 256;
        imageHeight = 203;

        slots = new ResourceLocation(CustomNpcs.MODID, "textures/gui/auctions/inventory_" + container.auction.getTextureType() + ".png");
    }

    @Override
    public void init() {
        super.init();
        slotPos[0] = guiLeft + 176;
        slotPos[1] = guiTop + 101;
        int lId = 0;
        int x0 = guiLeft + 5;
        int w0 = 80;
        int x1 = x0 + w0 + 2;
        int y = guiTop + 4;
        int color = CustomNpcs.MainColor.getRGB();
        Component title = Component.translatable("auction.create").append(". ").withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable("auction.house",
                                Util.instance.getOldFormattedText(Component.translatable(menu.auction.name).withStyle(ChatFormatting.RESET)))
                        .withStyle(ChatFormatting.YELLOW));
        addLabel(lId++, x0 + (imageWidth - font.width(title.getString())) / 2, y, title)
                .setSize(imageWidth - 10, 10)
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
                .setSize(imageWidth - 10, 10)
                .setColor(color);
        addTextField(2, x0 + 1, y += 12, imageWidth - 12, 12, menu.lot.getDescription())
                .setHoverTexts("auction.hover.description")
                .setMaxStringLength(1000);
        // inventory
        addLabel(lId++, x0, y += 16, "container.inventory")
                .setSize(imageWidth - 10, 10)
                .setColor(color);
        // commission
        addLabel(lId, x0 + 167, y + 32, Component.translatable("auction.manage.commission").append(":"))
                .setSize(imageWidth - 10, 10)
                .setColor(color);
        // max count
        int x = guiLeft + imageWidth - 65;
        y = guiTop + imageHeight - 22;
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
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        super.renderBg(graphics, partialTicks, mouseX, mouseY);
        // slots
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
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        Slot slot = menu.getSlot(0);
        matrixStack.translate( guiLeft + slot.x - 2, guiTop + slot.y - 2, 0);
        graphics.blit(slots, 0, 0, 0, 0, 164, 78);
        matrixStack.popPose();
        matrixStack.pushPose();
        // price slot
        matrixStack.translate(slotPos[0], slotPos[1], 0);
        graphics.blit(slots, 0, 0, 0, 58, 18, 20);
        graphics.blit(slots, 18, 0, 162, 58, 2, 20);
        matrixStack.popPose();
        // forbidden items:
        IForbiddenItem[] fis = menu.auction.getForbiddenItems();
        heldIsForbidden = false;
        if (fis.length > 0) {
            for (Slot s : menu.slots) {
                if (s.hasItem()) {
                    for (IForbiddenItem fi : fis) {
                        if (NoppesUtilPlayer.compareItems(s.getItem(), fi.getItem().getMCItemStack(), fi.isIgnoreDamage(), fi.isIgnoreNBT())) {
                            matrixStack.pushPose();
                            matrixStack.translate( guiLeft + s.x - 1.0f, guiTop + s.y - 1.0f, 300.0f);
                            graphics.blit(slots, 0, 0, 164, 0, 18, 18);
                            matrixStack.popPose();
                        }
                    }
                }
            }
            ItemStack heldStack = player.inventoryMenu.getCarried();
            if (!heldStack.isEmpty()) {
                for (IForbiddenItem fi : fis) {
                    if (NoppesUtilPlayer.compareItems(heldStack, fi.getItem().getMCItemStack(), fi.isIgnoreDamage(), fi.isIgnoreNBT())) {
                        // held
                        matrixStack.pushPose();
                        matrixStack.translate(mouseX - 8.0f, mouseY - 8.0f, 400.0f);
                        graphics.blit(slots, 0, 0, 164, 0, 18, 18);
                        matrixStack.popPose();
                        // slot
                        matrixStack.pushPose();
                        matrixStack.translate(slotPos[0] + 1.0f, slotPos[1] + 1.0f, 300.0f);
                        graphics.blit(slots, 0, 0, 164, 0, 18, 18);
                        matrixStack.popPose();
                        heldIsForbidden = true;
                        break;
                    }
                }
            }
        }
        if (isMouseHover(mouseX, mouseY, slotPos[0], slotPos[1], 16, 16)) {
            matrixStack.pushPose();
            matrixStack.translate(slotPos[0] + 2.0f, slotPos[1] + 2.0f, 0.0f);
            graphics.fill(0, 0, 16, 16, 0x80FFFFFF);
            matrixStack.popPose();
            if (heldIsForbidden) { setHoverText("auction.hover.err.item"); }
            else if (!menu.lot.getStack().isEmpty()) {
                setHoverText(menu.lot.getStack().getTooltipLines(player, minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL));
            }
        }
        if (!menu.lot.getStack().isEmpty()) {
            GuiBasic.renderStackInGUI(graphics, menu.lot.getStack(), slotPos[0] + 2.0f, slotPos[1] + 2.0f, 0.0f, 1.0f, null);
        }
        // done + currency
        GuiButtonNop button = getButton(2);
        String currency = menu.lot.getCurrencyType() ? CustomNpcs.displayCurrencies : CustomNpcs.displayDonation;
        ResourceLocation resource = menu.lot.getCurrencyType() ? GuiBasic.MONEY : GuiBasic.DONAT;
        if (button != null && prise != null) {
            MutableComponent hover = Component.translatable("auction.hover.create",
                    Util.instance.getOldFormattedText(Component.translatable(menu.auction.getName())));
            PlayerGameData gameData = PlayerData.get(player).game;
            canCreate = true;
            if ((menu.lot.getCurrencyType() ? gameData.getMoney() : gameData.getDonat()) < commission) {
                hover.append("<br>")
                        .append(Component.translatable("market.hover.notbuy." + (menu.lot.getCurrencyType() ? "3" : "7")))
                        .append(ChatFormatting.RED + ": " + ChatFormatting.GOLD + commission + ChatFormatting.RESET + currency);
                if (player.isCreative()) {
                    hover.append(ChatFormatting.RED + ". [")
                            .append(Component.translatable("gui.allowed").withStyle(ChatFormatting.RESET))
                            .append(ChatFormatting.RED + "]");
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
            matrixStack.pushPose();
            matrixStack.translate(prise.getX() + prise.getWidth() + 12, prise.getY() + 1, 0);
            graphics.drawString(font, currency, 0, 0, CustomNpcs.MainColor.getRGB());
            matrixStack.pushPose();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            matrixStack.translate( 4.5f, -1.5f, 0.0f);
            float scale = 12.0f / 256.0f;
            matrixStack.scale(scale, scale, scale);
            graphics.blit(resource, 0, 0, 0, 0, 256, 256);
            matrixStack.popPose();
            matrixStack.popPose();
        }
        // commission
        currency = commission + " " + currency;
        matrixStack.pushPose();
        matrixStack.translate(guiLeft + 172.0f, guiTop + 132.0f, 0.0f);
        graphics.drawString(font, currency, 0, 0, CustomNpcs.MainColor.getRGB());
        matrixStack.pushPose();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        matrixStack.translate(font.width(currency), -1.5f, 0.0f);
        float scale = 12.0f / 256.0f;
        matrixStack.scale(scale, scale, scale);
        graphics.blit(resource, 0, 0, 0, 0, 256, 256);
        matrixStack.popPose();
        matrixStack.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (!heldIsForbidden) {
            if (isMouseHover(mouseX, mouseY, slotPos[0], slotPos[1], 16, 16)) {
                ItemStack stack = menu.lot.getStack();
                ItemStack heldStack = player.inventoryMenu.getCarried();
                if (mouseButton == 0) { menu.lot.setStack(heldStack.copy()); }
                else if (mouseButton == 1) {
                    if (stack.isEmpty()) {
                        menu.lot.setStack(heldStack.copy());
                        menu.lot.getStack().setCount(1);
                    }
                    else if (stack.getCount() < stack.getMaxStackSize() &&
                            (heldStack.isEmpty() ||
                                    NoppesUtilPlayer.compareItems(stack, player.inventoryMenu.getCarried(), true, true))) {
                        menu.lot.getStack().grow(1);
                    }
                }
                else if (mouseButton == 2 && !stack.isEmpty()) { menu.lot.getStack().setCount(1); }
                return true;
            }
            if (Screen.hasShiftDown()) {
                Slot hoveredSlot = getSlotUnderMouse();
                if (hoveredSlot != null && !hoveredSlot.getItem().isEmpty()) {
                    menu.lot.setStack(hoveredSlot.getItem().copy());
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
                if (NoppesUtilPlayer.compareItems(select, player.inventoryMenu.getCarried(), true, true)) {
                    size += player.inventoryMenu.getCarried().getCount();
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
                        countIn = Math.min(countIn, menu.auction.getMaxLotsInPlayer() - menu.auction.getCountPlayerLots(player.getName().getString()));
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
