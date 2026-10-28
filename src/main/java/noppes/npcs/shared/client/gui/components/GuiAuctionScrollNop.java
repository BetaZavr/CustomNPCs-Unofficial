package noppes.npcs.shared.client.gui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.data.AuctionHouse;
import noppes.npcs.controllers.data.AuctionLot;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.util.EnumAlignText;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class GuiAuctionScrollNop extends GuiCustomScrollNop {

    protected AuctionHouse auction;
    public int currentPage = 0;
    public int totalPages = 1;

    public int center = width / 2;
    public int buttonWidth = center / 4;
    public int leftName;
    public int leftBid;
    public int leftBuyout;
    public int leftOwner;
    public int leftTime;

    protected final Map<Integer, List<AuctionLine>> data = new TreeMap<>();
    public EnumAuctionSortType lastSortedType;
    public boolean lastSortedAscending = false;
    public String search = "";
    public AuctionLot selectedLot = null;

    public GuiAuctionScrollNop(Object parent, AuctionHouse auctionIn) {
        super(parent, 0);
        colorBackS = 0x00000000;
        colorBackE = 0x00000000;
        hasSearch = false;
        textField.setIsVisible(false);
        stacks = new ArrayList<>(Collections.singleton(ItemStack.EMPTY));

        auction = auctionIn;
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // borders
        int color = CustomNpcs.LableColor.getRGB();
        PoseStack matrixStack = graphics.pose();

        matrixStack.pushPose();
        int s0 = x + 1;
        int s1 = x + width - 2;
        graphics.hLine(s0, s1, y, color); // up scroll
        graphics.hLine(s0, s1, y + lineHeight, color); // up tile
        graphics.hLine(s0, s1, y + height - 17, color); // down buttons
        graphics.hLine(s0, s1, y + height, color); // down scroll
        s0 = y - 1;
        s1 = y + height + 1;
        graphics.vLine(x, s0, s1, color); // left scroll
        graphics.vLine(x + width - 1, s0, s1, color); // left scroll
        if (listener instanceof IGuiInterface) {
            GuiLabel label = ((IGuiInterface) listener).getLabel(500);
            if (label != null && label.isVisible()) {
                GuiButtonNop button = ((IGuiInterface) listener).getButton(305);
                if (button != null) {
                    graphics.vLine(button.getX() - 2, button.getY() - 4, button.getY() + 13, color);
                }
            }
        }
        graphics.vLine(leftBid - 1, s0, s1, color); // bid
        graphics.vLine(leftBuyout - 1, s0, s1, color); // buyout
        graphics.vLine(leftOwner - 1, s0, s1, color); // owner
        graphics.vLine(leftTime - 1, s0, s1, color); // time
        matrixStack.popPose();
        // main
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void drawItems(GuiGraphics graphics) {
        List<AuctionLine> listIn = data.get(currentPage);
        if (listIn == null) { return; }
        PoseStack matrixStack = graphics.pose();
        int displayIndex = 0;
        int alpha = 255 << 24;
        int left = x + 15;
        for(int i = 0; i < listIn.size(); ++i) {
            int top = lineHeight * (displayIndex + 1) + 3 - scrollY;
            int bottom = top + lineHeight - 3;
            ++displayIndex;
            if (top < 4 || bottom > height) { continue; }
            top += y;
            bottom += y;
            AuctionLine aLine = listIn.get(i);
            int color = (i == hover ? CustomNpcs.HoverColor.getRGB() : CustomNpcs.MainColor.getRGB())| alpha;
            GuiButtonNop.renderString(graphics, aLine.name, leftName + 3, top, leftBid - 2, bottom, color, true, EnumAlignText.LEFT, false, customFont);
            GuiButtonNop.renderString(graphics, aLine.bid, leftBid + 1, top, leftBuyout - 2, bottom, color, true, EnumAlignText.LEFT, false, customFont);
            GuiButtonNop.renderString(graphics, aLine.buyout, leftBuyout + 1, top, leftOwner - 2, bottom, color, true, EnumAlignText.LEFT, false, customFont);
            GuiButtonNop.renderString(graphics, aLine.owner, leftOwner + 1, top, leftTime - 2, bottom, color, true, EnumAlignText.LEFT, false, customFont);
            //time
            long time = ValueUtil.correctLong(aLine.lot.getCreationDate() + aLine.lot.getDuration() - System.currentTimeMillis(), 0, Long.MAX_VALUE);
            GuiButtonNop.renderString(graphics, Component.literal(Util.instance.ticksToElapsedTime(time / 50L, false, true, false)),
                    leftTime + 1, top, x + width - 2, bottom, color, true, EnumAlignText.LEFT, false, customFont);
            if (hover == i) {
                int r = left + width - 17;
                matrixStack.pushPose();
                matrixStack.translate(left - 2.0f, top - 3.0f, 0.0f);
                if (customFont != null) {
                    int c = border != 0xFF000000 ? border : -1;
                    matrixStack.scale(0.5f, 0.5f, 0.5f);
                    r = (int) ((float) (r - left) * scaleX) + 1;
                    int h = (int) (lineHeight * 2.0f * scaleY) + 2;
                    graphics.fill(0, 3, r, h, (c & 0xFFFFFF) | 0x60000000);
                    graphics.vLine(0, 3, h, c);
                    graphics.vLine(r, 3, h, c);
                    graphics.hLine(0, r, 3, c);
                    graphics.hLine(0, r, h, c);
                }
                else {
                    graphics.vLine(0, 0, lineHeight + 1, -1);
                    graphics.vLine(r - left + 2, 0, lineHeight + 1, -1);
                    graphics.hLine(1, r - left + 2, 1, -1);
                    graphics.hLine(1, r - left + 2, lineHeight, -1);
                }
                matrixStack.popPose();
            }
        }
        if (mouseInList && listener instanceof IGuiInterface) {
            IGuiInterface gui = (IGuiInterface) listener;
            if (!gui.hasSubGui() && hover >= 0 && hover < listIn.size()) {
                AuctionLine aLine = listIn.get(hover);
                if (aLine != null) { gui.setHoverText(aLine.description); }
            }
        }
    }

    @Override
    protected void drawStacks(@Nonnull GuiGraphics graphics) {
        List<AuctionLine> listIn = data.get(currentPage);
        if (listIn == null) { return; }
        int displayIndex = 0;
        for (AuctionLine aLine : listIn) {
            int k = lineHeight * (displayIndex +1) + 4 - scrollY;
            displayIndex++;
            if (k < 4 || k + 10 > height) { continue; }
            GuiBasic.renderStackInGUI(graphics, aLine.stack, x + 1, y + k - 2.5f, 300.0f, 0.75f, "");
        }
    }

    @Override
    protected int getMouseOver(int mouseX, int mouseY) {
        List<AuctionLine> listIn = data.get(currentPage);
        if (listIn == null) { return -1; }
        mouseX -= x;
        mouseY -= y;
        if (mouseX >= 4 && mouseX < width - 4 && mouseY >= 1 + lineHeight && mouseY < height - 17) {
            int displayIndex = 0;
            for(int index = 0; index < listIn.size(); ++index) {
                if (mouseInOption(mouseX, mouseY, displayIndex)) { return index; }
                ++displayIndex;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseInOption(int mouseX, int mouseY, int displayIndex) {
        int xOffset = scrollHeight < height - 2 ? 10 : 0;
        int posX = 4;
        int posY = lineHeight * (displayIndex + 1) + 4 - scrollY;
        if (posY < 4 || posY + 10 > height) { return false; }
        return mouseX >= posX - 1 && mouseX < width - 2 - xOffset && mouseY >= posY - 3 && mouseY < posY + lineHeight - 2;
    }

    @Override
    public GuiCustomScrollNop setSize(int widthIn, int heightIn) {
        height = heightIn;
        width = widthIn;

        leftName = x + 12;
        center = (int) ((float) width / 2.25f);
        buttonWidth = (width - center) / 4;
        leftBid = x + center;
        leftBuyout = leftBid + buttonWidth;
        leftOwner = leftBid + buttonWidth * 2;
        leftTime = leftBid + buttonWidth * 3;
        buttonWidth -= 3;

        listSize = (int) ((double) (height - lineHeight - 18) / (double) lineHeight);
        listHeight = lineHeight * listSize;
        if (listHeight > 0) { scrollHeight = (int)((double)(height - 2) / (double) listHeight * (double)(height - 2)); }
        else { scrollHeight = Integer.MAX_VALUE; }
        maxScrollY = 0;
        resetRoll();
        return this;
    }

    @Override
    protected void reset() {
        listSize = ValueUtil.correctInt((int) (Math.floor((float) (height - 18) / (float) lineHeight)) - 1, 1, Integer.MAX_VALUE);
        setSize(width, height);
        scrollY = 0;
        if (selected >= 0 && selected >= listSize) { selected = -1; }
        selectedList.clear();
        lastClickedItem = -1;
    }

    public GuiCustomScrollNop init(@Nonnull AuctionHouse auctionIn, int xIn, int yIn, int widthIn, int heightIn) {
        auction = auctionIn;
        x = xIn;
        y = yIn;
        leftName = x + 12;
        setSize(widthIn, heightIn);
        if (currentPage >= totalPages) { currentPage = totalPages - 1; }
        if (data.isEmpty()) { filterAndSort(EnumAuctionSortType.DATE, true); }
        if (listener instanceof IGuiInterface gui) {
            GuiButtonNop button = gui.getButton(300);
            ResourceLocation bt = new ResourceLocation(CustomNpcs.MODID, "textures/gui/auctions/buttons_" + auction.getTextureType() + ".png");
            int yPos = y + height - 15;
            // sort name
            if (button == null) { button = gui.addButton(300, x + 2, yPos, "gui.sort").setHoverTexts("auction.hover.sort"); }
            List<AuctionLine> listIn = data.get(currentPage);
            button.setSize(buttonWidth, 14)
                    .setIsEnabled(listIn != null && !listIn.isEmpty())
                    .setTexture(bt)
                    .setUV(0, 0, 256, 28)
                    .setIsAnim(true);
            button.setX(x + 2);
            button.setY(yPos);
            // page number
            int w = ValueUtil.correctInt(leftBid - x - buttonWidth + 2, 0, 82);
            int lId = 500;
            GuiLabel label = gui.getLabel(lId);
            if (label == null) { label = gui.addLabel(lId, leftBid - w + 18, yPos + 1, (currentPage + 1) + "/" + totalPages); }
            label.setSize(w - 40, 10)
                    .setColor(CustomNpcs.MainColor.getRGB())
                    .setAlignCenter()
                    .setIsVisible(w > 50 && totalPages > 1);
            label.setX(leftBid - w + 20);
            label.setY(yPos + 1);
            // pre page
            button = gui.getButton(305);
            if (button == null) { button = gui.addButton(305, leftBid - w, yPos + 2, ""); }
            button.setIsBookTexture(2)
                    .setIsVisible(w > 50 && totalPages > 1 && currentPage > 1);
            button.setX(leftBid - w);
            button.setY(yPos + 2);
            // next page
            button = gui.getButton(306);
            if (button == null) { button = gui.addButton(306, leftBid - 21, yPos + 2, ""); }
            button.setIsBookTexture(1)
                    .setIsVisible(w > 50 && totalPages > 1 && currentPage < totalPages - 1);
            button.setX(leftBid - 21);
            button.setY(yPos + 2);
            // sort cost
            button = gui.getButton(301);
            if (button == null) { button = gui.addButton(301, leftBid + 1, yPos, "gui.sort"); }
            button.setSize(buttonWidth, 14)
                    .setIsEnabled(listIn != null && !listIn.isEmpty())
                    .setTexture(bt)
                    .setUV(0, 0, 256, 28)
                    .setIsAnim(true)
                    .setHoverTexts("auction.hover.sort");
            button.setX(leftBid + 1);
            button.setY(yPos);
            // sort cost
            button = gui.getButton(302);
            if (button == null) { button = gui.addButton(302, leftBuyout + 1, yPos, "gui.sort"); }
            button.setSize(buttonWidth, 14)
                    .setIsEnabled(listIn != null && !listIn.isEmpty())
                    .setTexture(bt)
                    .setUV(0, 0, 256, 28)
                    .setIsAnim(true)
                    .setHoverTexts("auction.hover.sort");
            button.setX(leftBuyout + 1);
            button.setY(yPos);
            // sort owner
            button = gui.getButton(303);
            if (button == null) { button = gui.addButton(303, leftOwner + 1, yPos, "gui.sort"); }
            button.setSize(buttonWidth, 14)
                    .setIsEnabled(listIn != null && !listIn.isEmpty())
                    .setTexture(bt)
                    .setUV(0, 0, 256, 28)
                    .setIsAnim(true)
                    .setHoverTexts("auction.hover.sort");
            button.setX(leftOwner + 1);
            button.setY(yPos);
            // sort time
            button = gui.getButton(304);
            if (button == null) { button = gui.addButton(304, leftTime + 1, yPos, "gui.sort"); }
            button.setSize((width - center) - 3 * (buttonWidth + 3) - 1, 14)
                    .setIsEnabled(listIn != null && !listIn.isEmpty())
                    .setTexture(bt)
                    .setUV(0, 0, 256, 28)
                    .setIsAnim(true)
                    .setHoverTexts("auction.hover.sort");
            button.setX(leftTime + 1);
            button.setY(yPos);

            // title name
            lId++;
            label = gui.getLabel(lId);
            if (label == null) { label = gui.addLabel(lId, x, y + 2, Component.translatable("gui.name").append(":")); }
            label.setSize(leftBid - leftName + 12, 10)
                    .setColor(CustomNpcs.MainColor.getRGB())
                    .setAlignCenter();
            label.setX(x);
            label.setY(y + 2);
            // title bid
            lId++;
            label = gui.getLabel(lId);
            if (label == null) { label = gui.addLabel(lId, leftBid, y + 2, Component.translatable("auction.bid").append(":")); }
            label.setSize(leftBuyout - leftBid, 10)
                    .setColor(CustomNpcs.MainColor.getRGB())
                    .setAlignCenter();
            label.setX(leftBid);
            label.setY(y + 2);
            // title buyout
            lId++;
            label = gui.getLabel(lId);
            if (label == null) { label = gui.addLabel(lId, leftBuyout, y + 2, Component.translatable("auction.buyout").append(":")); }
            label.setSize(leftOwner - leftBuyout, 10)
                    .setColor(CustomNpcs.MainColor.getRGB())
                    .setAlignCenter();
            label.setX(leftBuyout);
            label.setY(y + 2);
            // title owner
            lId++;
            label = gui.getLabel(lId);
            if (label == null) { label = gui.addLabel(lId, leftOwner, y + 2, Component.translatable("bank.owner").append(":")); }
            label.setSize(leftTime - leftOwner, 10)
                    .setColor(CustomNpcs.MainColor.getRGB())
                    .setAlignCenter();
            label.setX(leftOwner);
            label.setY(y + 2);
            // title time
            lId++;
            label = gui.getLabel(lId);
            if (label == null) { label = gui.addLabel(lId, leftTime, y + 2, Component.translatable("gui.time").append(":")); }
            label.setSize(x + width - leftTime, 10)
                    .setColor(CustomNpcs.MainColor.getRGB())
                    .setAlignCenter();
            label.setX(leftTime);
            label.setY(y + 2);
        }
        return this;
    }

    public void buttonEvent(int id) {
        switch (id) {
            case 300: filterAndSort(EnumAuctionSortType.NAMES, lastSortedType != EnumAuctionSortType.NAMES || !lastSortedAscending); break;
            case 301: filterAndSort(EnumAuctionSortType.OWNERS, lastSortedType != EnumAuctionSortType.OWNERS || !lastSortedAscending); break;
            case 302: filterAndSort(EnumAuctionSortType.COST_BID, lastSortedType != EnumAuctionSortType.COST_BID || !lastSortedAscending); break;
            case 303: filterAndSort(EnumAuctionSortType.COST_BUY, lastSortedType != EnumAuctionSortType.COST_BUY || !lastSortedAscending); break;
            case 304: filterAndSort(EnumAuctionSortType.DATE, lastSortedType != EnumAuctionSortType.DATE || !lastSortedAscending); break;
            case 305: prePage(); break;
            case 306: nextPage(); break;
        }
    }

    public void filterAndSort(@Nullable EnumAuctionSortType sortedTypeIn, boolean sortedAscending) {
        data.clear();
        // create filtered
        List<AuctionLine> filteredLots = new ArrayList<>();
        Player player = Minecraft.getInstance().player;
        String[] searchIn = search.split(" ");
        for (AuctionLot lot : auction.getRawLots()) {
            boolean added = search.isEmpty();
            if (!added) {
                for (String part : searchIn) {
                    if (lot.getStack().getDisplayName().getString().contains(part)) {
                        added = true;
                        break;
                    }
                }
            }
            if (added) { filteredLots.add(new AuctionLine(player, lot)); }
        }
        // sorted
        EnumAuctionSortType sortedType = sortedTypeIn == null ? EnumAuctionSortType.DATE : sortedTypeIn;
        filteredLots.sort((a, b) -> {
            int cmp = switch (sortedType) {
                case NAMES -> a.name.getString().compareToIgnoreCase(b.name.getString());
                case OWNERS -> a.owner.getString().compareToIgnoreCase(b.owner.getString());
                case COST_BID -> Long.compare(a.lot.getCurrentBidPrice(), b.lot.getCurrentBidPrice());
                case COST_BUY -> Long.compare(a.lot.getBuyoutPrice(), b.lot.getBuyoutPrice());
                default -> Long.compare(a.lot.getCreationDate() + a.lot.getDuration(),
                        b.lot.getCreationDate() + b.lot.getDuration()); // DATE
            };
            return sortedAscending ? cmp : -cmp;
        });
        lastSortedType = sortedType;
        lastSortedAscending = sortedAscending;
        totalPages = (int) Math.max(1.0d, Math.ceil((double) filteredLots.size() / (double) listSize));
        currentPage = Math.min(currentPage, totalPages - 1);
        int i = 0, page = 0;
        for (AuctionLine line: filteredLots) {
            if (!data.containsKey(i)) { data.put(i, new ArrayList<>()); }
            data.get(page).add(line);
            i++;
            if (i >= listSize) { i = 0; page++; }
        }
    }

    protected void prePage() {
        if (currentPage > 0) {
            currentPage--;
            init(auction, x, y, width, height);
        }
    }

    protected void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            init(auction, x, y, width, height);
        }
    }

    @SuppressWarnings("unused")
    public @Nullable List<AuctionLine> getVisibleList() { return data.get(currentPage); }

    public enum EnumAuctionSortType { NAMES, OWNERS, COST_BID, COST_BUY, DATE }

    public  class AuctionLine {

        public final ItemStack stack;
        public final MutableComponent name;
        public final Component bid;
        public final Component buyout;
        public final Component owner;
        public final List<Component> description;
        public final AuctionLot lot;

        public AuctionLine(@Nullable Player player, @Nonnull AuctionLot lotIn) {
            stack = lotIn.getStack();
            name = Component.empty().append(stack.getDisplayName());
            if (stack.getCount() > 1) { name.append(" x" + stack.getCount()).withStyle(ChatFormatting.DARK_GREEN); }
            if (minecraft == null) { minecraft = Minecraft.getInstance(); }
            description = stack.getTooltipLines(player, minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
            String moneyType = lotIn.getCurrencyType() ? CustomNpcs.displayCurrencies : CustomNpcs.displayDonation;
            bid = Component.literal(auction.calculateNextBid(lotIn.getCurrentBidPrice()) + "").append(moneyType);
            buyout = Component.literal(lotIn.getBuyoutPrice() + "").append(moneyType);
            owner = Component.literal(lotIn.getOwnerName()).withStyle(ChatFormatting.GRAY);
            lot = lotIn;
        }

    }

}
