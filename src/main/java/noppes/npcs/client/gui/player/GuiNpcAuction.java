package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.controllers.data.AuctionHouse;
import noppes.npcs.controllers.data.AuctionLot;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class GuiNpcAuction extends GuiBasic {

    protected final float SCALE = 0.95f;
    protected GuiAuctionScrollNop scroll;
    protected AuctionHouse auction;
    protected final ResourceLocation buttons;
    public boolean createLot;

    public GuiNpcAuction(AuctionHouse auctionIn) {
        super();
        SubGuiNpcAuctionCreateLot.parent = this;

        auction = auctionIn.copy();
        int type = auction.getTextureType();
        background = new ResourceLocation(CustomNpcs.MODID, "textures/gui/auctions/background_" + type + ".png");
        buttons = new ResourceLocation(CustomNpcs.MODID, "textures/gui/auctions/buttons_" + type + ".png");
    }

    @Override
    public void init() {
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        imageWidth = (int) ((float) minecraft.getWindow().getGuiScaledWidth() * SCALE);
        imageHeight = (int) ((float) minecraft.getWindow().getGuiScaledHeight() * SCALE);

        guiLeft = (width - imageWidth) / 2;
        guiTop = (height - imageHeight) / 2;
        wrapper.init(minecraft, width, height);
        setFocused(!hasSubGui());

        int x = guiLeft;
        int y = guiTop + 3;
        Component title = Component.translatable("auction.house",
                Util.instance.getOldFormattedText(Component.translatable(auction.name).withStyle(ChatFormatting.RESET)))
                .withStyle(ChatFormatting.YELLOW);
        addLabel(0, x + (imageWidth - font.width(title)) / 2, y, title)
                .setSize(imageWidth - 10, 10)
                .setColor(CustomNpcs.MainColor.getRGB());
        x += 5;
        if (scroll == null) { scroll = new GuiAuctionScrollNop(this, auction); }
        scroll.init(auction, guiLeft + 5, guiTop + 14,
                imageWidth - 10, imageHeight - 46);
        add(scroll);
        y = scroll.getY() + scroll.height + 2;
        // found
        ResourceLocation bt = new ResourceLocation(CustomNpcs.MODID, "textures/gui/auctions/buttons_" + auction.getTextureType() + ".png");
        int w = (int) (imageWidth * 0.35f);
        addTextField(20, x + 1, y + 1, w, 12, "")
                .setMaxStringLength(20);
        int wB = (imageWidth - w - 26) / 4;
        addButton(50, x + w + 4, y, "gui.found")
                .setSize(wB, 14)
                .setTexture(bt)
                .setUV(0, 0, 256, 28)
                .setIsAnim(true);
        // pay
        addButton(51, x + w + wB + 12, y, "auction.bid")
                .setSize(wB, 14)
                .setIsEnabled(scroll.selectedLot != null && auction.clientAvailabilityBuyout)
                .setTexture(bt)
                .setUV(0, 112, 128, 28)
                .setAlignLeft()
                .setIsAnim(true);
        addButton(52, x + w + (wB + 2) * 2 + 10, y, "auction.buyout")
                .setSize(wB, 14)
                .setIsEnabled(scroll.selectedLot != null && auction.clientAvailabilityBuyout && scroll.selectedLot.canBuyout())
                .setTexture(bt)
                .setUV(128, 112, 128, 28)
                .setAlignLeft()
                .setIsAnim(true);
        // create
        addButton(53, x + w + (wB + 2) * 3 + 10, y, "auction.create")
                .setSize(wB, 14)
                .setIsEnabled(auction.clientAvailabilityLot)
                .setTexture(bt)
                .setUV(0, 0, 256, 28)
                .setIsAnim(true);
        // balance
        if (auction.getCurrencyType() == 0 || auction.getCurrencyType() == 1) {
            long value = PlayerData.get(player).game.getMoney();
            String money = Util.instance.getOldFormattedText(Component.translatable("gui.money")
                    .append(": " + Util.instance.getTextReducedNumber(value, true, true, true))
                    .append(CustomNpcs.displayCurrencies));
            addLabel(1, x + imageWidth - (auction.getCurrencyType() == 0 ? 140 : 70), y + 16, money)
                    .setSize(w + 2, 10)
                    .setColor(CustomNpcs.MainColor.getRGB())
                    .setHoverTexts(Component.translatable("inventory.hover.currency")
                            .append(" " + value + CustomNpcs.displayCurrencies));
        }
        if (auction.getCurrencyType() == 0 || auction.getCurrencyType() == 2) {
            long value = PlayerData.get(player).game.getDonat();
            String donat = Util.instance.getOldFormattedText(Component.translatable("gui.donat")
                    .append(": " + Util.instance.getTextReducedNumber(PlayerData.get(player).game.getDonat(), true, true, true))
                    .append(CustomNpcs.displayDonation));
            addLabel(2, x + imageWidth - 70, y + 16, donat)
                    .setSize(w + 2, 10)
                    .setColor(CustomNpcs.MainColor.getRGB())
                    .setHoverTexts(Component.translatable("inventory.hover.donat")
                            .append(" " + value + CustomNpcs.displayDonation));
        }
    }

    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics) {
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate((float) guiLeft, (float) guiTop, 0.0F);

        matrixStack.scale(bgScale, bgScale, bgScale);

        int maxRow = ValueUtil.correctInt((int) Math.ceil((float) imageHeight / (float) (256 - 2 * borderTexture)), 2, 10);
        int maxCol = ValueUtil.correctInt((int) Math.ceil((float) imageWidth / (float) (256 - 2 * borderTexture)), 2, 10);
        int tileWidth = imageWidth / maxCol;
        int tileHeight = imageHeight / maxRow;
        int lastTileWidth = imageWidth - tileWidth * (maxCol - 1);
        int lastTileHeight = imageHeight - tileHeight * (maxRow - 1);

        int uOffset = (256 - 2 * borderTexture - tileWidth) / 2;
        int uMax = 256 - lastTileWidth;
        int vOffset = (256 - 2 * borderTexture - tileHeight) / 2;
        int vMax = 256 - lastTileHeight;

        for (int col = 0; col < maxCol; ++col) {
            for (int row = 0; row < maxRow; ++row) {
                graphics.blit(background, col * tileWidth,
                        row * tileHeight,
                        col == 0 ? 0 : col == maxCol - 1 ? uMax : uOffset,
                        row == 0 ? 0 : row == maxRow - 1 ? vMax : vOffset,
                        col == maxCol - 1 ? lastTileWidth : tileWidth,
                        row == maxRow - 1 ? lastTileHeight : tileHeight);
            }
        }

        matrixStack.popPose();
        GuiButtonNop b0 = getButton(50);
        GuiTextFieldNop tf = getTextField(20);
        if (b0 != null && tf != null) { b0.setIsEnabled(!tf.getValue().isEmpty()); }
        GuiLabel label = getLabel(1);
        float scale = 12.0f / 256.0f;
        if (label != null) {
            matrixStack.pushPose();
            float w = font.width(label.getMessage()) + 1.0f;
            matrixStack.translate((float) label.getX() + w, (float) guiTop + imageHeight - 14.5f, 0.0F);
            matrixStack.scale(scale, scale, scale);
            graphics.blit(MONEY, 0, 0, 0, 0, 256, 256);
            matrixStack.popPose();
        }
        label = getLabel(2);
        if (label != null) {
            matrixStack.pushPose();
            float w = font.width(label.getMessage()) + 1.0f;
            matrixStack.translate((float) label.getX() + w, (float) guiTop + imageHeight - 14.5f, 0.0F);
            matrixStack.scale(scale, scale, scale);
            graphics.blit(DONAT, 0, 0, 0, 0, 256, 256);
            matrixStack.popPose();
        }
    }

    @Override
    public void buttonEvent(@Nonnull GuiButtonNop button) {
        if (button.id > 299 && button.id < 310) {
            scroll.buttonEvent(button.id);
            return;
        } // filtering
        switch (button.id) {
            case 50: {
                GuiTextFieldNop tf = getTextField(20);
                if (tf != null && scroll != null) {
                    scroll.search = tf.getValue();
                    scroll.filterAndSort(scroll.lastSortedType, scroll.lastSortedAscending);
                    scroll.init(auction, guiLeft + 5, guiTop + 14,
                            imageWidth - 10, imageHeight - 46);
                }
                break;
            } // found
            case 51: {
                if (scroll.hasSelected()) {
                    Packets.sendServer(new SPacketAuctionBid(auction.getId(), scroll.selectedLot.getCreationDate()));
                }
                break;
            } // bid
            case 52: {
                if (scroll.hasSelected()) {
                    Packets.sendServer(new SPacketAuctionBuyout(auction.getId(), scroll.selectedLot.getCreationDate()));
                }
                break;
            } // buyout
            case 53: {
                createLot = true;
                Packets.sendServer(new SPacketContainerOpen(EnumGuiType.PlayerAuctionCreateLot,
                        (b) -> b.writeBlockPos(new BlockPos(auction.getId(), 0, 0))));
                break;
            } // create lot
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        if (!createLot) { Packets.sendServer(new SPacketAuctionClose(auction.getId())); }
    }

    public void update() {
        auction = AuctionController.getInstance().getAuction(auction.getId()).copy(); // update all
        init();
    }

    public void updateLot(long lotId) {
        // Get the updated auction data from controller (already updated by packet handler)
        AuctionHouse serverAuction = AuctionController.getInstance().getAuction(auction.getId());
        if (serverAuction == null) { return; }

        AuctionLot serverLot = (AuctionLot) serverAuction.getLot(lotId);
        AuctionLot localLot = (AuctionLot) auction.getLot(lotId);

        if (serverLot != null && localLot == null) {
            AuctionLot newLot = new AuctionLot(0);
            newLot.load(serverLot.save());
            auction.addLot(newLot);
        }
        else if (serverLot != null) { localLot.load(serverLot.save()); }
        else if (localLot != null) { auction.removeLot(lotId); }

        // Refresh the scroll display
        scroll.filterAndSort(scroll.lastSortedType, scroll.lastSortedAscending);
        scroll.init(auction, guiLeft + 5, guiTop + 14,
                imageWidth - 10, imageHeight - 46);
    }

}
