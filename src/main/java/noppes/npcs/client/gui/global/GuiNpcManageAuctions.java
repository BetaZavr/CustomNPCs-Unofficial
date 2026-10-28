package noppes.npcs.client.gui.global;

import java.util.*;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.handler.data.IForbiddenItem;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketAuctionSave;
import noppes.npcs.packets.server.SPacketAuctionsGet;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.controllers.data.AuctionHouse;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

public class GuiNpcManageAuctions extends GuiNPCInterface2
        implements ICustomScrollListener, ITextfieldListener, ISliderListener {

    protected final Map<Component, AuctionHouse> dataAH = new HashMap<>();
    protected final Map<Component, IForbiddenItem> dataFI = new HashMap<>();
    protected GuiCustomScrollNop scrollAuctions;
    protected GuiCustomScrollNop scrollForbiddenItems;
    protected AuctionHouse select;

    public GuiNpcManageAuctions(EntityNPCInterface npc) {
        super(npc);
        imageHeight = 200;

        backGui = EnumGuiType.MainMenuGlobal;
        Packets.sendServer(new SPacketAuctionsGet());
    }

    @Override
    public void init() {
        super.init();
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        int lId = 0;
        int wB = 54;
        int w0 = wB * 2 + 2;
        int lW = 50;
        int w1 = 100;
        int w2 = lW + w1 + 4;
        int x0 = guiLeft + 5;
        int x1 = x0 + w0 + 2;
        int x2 = x1 + lW + 2;
        int x3 = x1 + w2 + 1;
        int y = guiTop + 14;
        if (scrollAuctions == null) {
            scrollAuctions = addScroll(0)
                    .setSize(w0, imageHeight + 2).disabledSearch();
        }
        if (scrollForbiddenItems == null) {
            scrollForbiddenItems = addScroll(1)
                    .setSize(imageWidth - x3 + guiLeft - 5, imageHeight - 92);
        }

        // Houses scroll
        addLabel(lId++, x0 + 1, y - 10, Component.translatable("auction.manage.houses").append(":"))
                .setSize(w1, 10);
        dataAH.clear();
        Component keySel = scrollAuctions.getNormalSelected();
        AuctionController aData = AuctionController.getInstance();
        for (AuctionHouse house : aData.auctionHouses.values()) {
            Component key = house.getKey();
            dataAH.put(key, house);
            if (select == null) {
                select = house;
                keySel = key;
            }
        }
        add(scrollAuctions.setPos(x0, y)
                .setNormalList(new ArrayList<>(dataAH.keySet()))
                .setSelected(keySel));
        dataFI.clear();
        if (select != null) {
            List<Component> list = new ArrayList<>();
            List<Component> suffixes = new ArrayList<>();
            List<ItemStack> stacks = new ArrayList<>();
            LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
            for (IForbiddenItem fi : select.getForbiddenItems()) {
                List<Component> hover = fi.getItem().getMCItemStack().getTooltipLines(player, minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
                hts.put(list.size(), hover);
                MutableComponent sfx = Component.empty();
                if (fi.isIgnoreDamage() || fi.isIgnoreNBT()) {
                    if (fi.isIgnoreDamage()) { sfx.append(ChatFormatting.RESET + "|" + ChatFormatting.RED + "D"); }
                    if (fi.isIgnoreNBT()) { sfx.append(ChatFormatting.RESET + "|" + ChatFormatting.GREEN + "T"); }
                    sfx.append(ChatFormatting.RESET + "|");
                }
                suffixes.add(sfx);
                Component key = Component.literal(fi.getItem().getDisplayName());
                list.add(key);
                stacks.add(fi.getItem().getMCItemStack());
                dataFI.put(key, fi);
            }
            scrollForbiddenItems.setUnsortedList(list)
                    .setStacks(stacks)
                    .setSuffixes(suffixes)
                    .setHoverTexts(hts);
        }
        add(scrollForbiddenItems.setPos(x3, y));
        // Buttons
        addButton(0, x0, y + scrollAuctions.getHeight() + 2, "gui.add")
                .setSize(wB, 16)
                .setIsEnabled(aData.auctionHouses.size() < 16)
                .setHoverTexts("auction.manage.hover.add.home");
        addButton(1, x0 + wB + 2, y + scrollAuctions.getHeight() + 2, "gui.remove")
                .setSize(wB, 16)
                .setIsEnabled(select != null && aData.auctionHouses.size() > 1)
                .setHoverTexts("auction.manage.hover.del.home");
        // Forbidden items
        addLabel(lId++, x3 + 1, y - 10, Component.translatable("auction.manage.forbidden.items").append(":"))
                .setSize(scrollForbiddenItems.getWidth() - 2, 10);
        wB = (scrollForbiddenItems.getWidth() - 2) / 2;
        addButton(2, x3, y + scrollForbiddenItems.getHeight() + 2, "gui.add")
                .setSize(wB, 16)
                .setIsEnabled(select != null)
                .setHoverTexts("auction.manage.hover.add.item");
        addButton(3, x3 + wB + 2, y + scrollForbiddenItems.getHeight() + 2, "gui.remove")
                .setSize(wB, 16)
                .setIsEnabled(select != null && scrollForbiddenItems.hasSelected())
                .setHoverTexts("auction.manage.hover.del.item");

        // Settings form
        addLabel(lId++, x1, y - 10, "auction.manage.settings")
                .setSize(w2, 10);
        Component label = Component.translatable("gui.name").append(":");
        int wLabel = font.width(label);
        int x = wLabel < lW ? x1 + lW - wLabel : x1;
        addLabel(lId++, x, y + 2, Component.translatable("gui.name").append(":"))
                .setSize(Math.min(wLabel, lW), 10);
        addTextField(0, x2, y += 1, w1, 14, select != null ? select.getName() : "Default Auction")
                .setIsEnabled(select != null)
                .setHoverTexts("auction.manage.hover.name");
        // Commission
        label = Component.translatable("auction.manage.commission").append(":");
        wLabel = font.width(label);
        x = wLabel < lW ? x1 + lW - wLabel : x1;
        addLabel(lId++, x, (y += 18) + 9, label)
                .setSize(Math.min(wLabel, lW), 10);
        float min = select != null ? (float) select.getCommission()[0] : 2.5f;
        GuiTextFieldNop tf_1 = addTextField(1, x2, y, 40, 14, min)
                .setMinMaxDefault(0.5d, 15.0d, min)
                .setIsEnabled(select != null)
                .setHoverTexts("auction.manage.hover.commission",
                        "auction.manage.hover.commission.min",
                        "auction.manage.hover.commission.info");
        float max = select != null ? (float) select.getCommission()[1] : 2.5f;
        GuiTextFieldNop tf_2 = addTextField(2, x2, y + 16, 40, 14, max)
                .setMinMaxDefault(0.5d, 15.0d, max)
                .setIsEnabled(select != null)
                .setHoverTexts("auction.manage.hover.commission",
                        "auction.manage.hover.commission.max",
                        "auction.manage.hover.commission.info");
        GuiSliderNop s_1 = addSlider(1, x2 + 43, y, 0.068966f * min - 0.034483f)
                .setSize(58, 14)
                .setHoverTexts("auction.manage.hover.commission",
                        "auction.manage.hover.commission.min",
                        "auction.manage.hover.commission.info");
        GuiSliderNop s_2 = addSlider(2, x2 + 43, y += 16, 0.068966f * max - 0.034483f)
                .setSize(58, 14)
                .setHoverTexts("auction.manage.hover.commission",
                        "auction.manage.hover.commission.max",
                        "auction.manage.hover.commission.info");
        if (select != null) {
            tf_1.setValue(Math.round(min * 10.0f) / 10.0f);
            tf_2.setValue(Math.round(max * 10.0f) / 10.0f);
            select.setCommission(min, max);
            resetCommissions(s_1, s_2);
        }
        // Duration
        label = Component.translatable("auction.manage.duration").append(":");
        wLabel = font.width(label);
        x = wLabel < lW ? x1 + lW - wLabel : x1;
        addLabel(lId++, x, (y += 18) + 9, label)
                .setSize(Math.min(wLabel, lW), 10);
        long minL = select != null ? select.getDelays()[0] / 60000L : 30L;
        tf_1 = addTextField(3, x2, y, 40, 14, minL)
                .setMinMaxDefault(10L, 3L * 24L * 60L, minL)
                .setIsEnabled(select != null)
                .setHoverTexts("auction.manage.hover.duration",
                        "auction.manage.hover.duration.min",
                        "auction.manage.hover.duration.info");
        long maxL = select != null ? select.getDelays()[1] / 60000L : 24L * 60L;
        tf_2 = addTextField(4, x2, y + 16, 40, 14, maxL)
                .setMinMaxDefault(10L, 3L * 24L * 60L, maxL)
                .setIsEnabled(select != null)
                .setHoverTexts("auction.manage.hover.duration",
                        "auction.manage.hover.duration.max",
                        "auction.manage.hover.duration.info");
        s_1 = addSlider(3, x2 + 43, y, 0.000232f * (float) minL - 0.002320f)
                .setSize(58, 14)
                .setHoverTexts("auction.manage.hover.duration",
                        "auction.manage.hover.duration.min",
                        "auction.manage.hover.duration.info");
        s_2 = addSlider(4, x2 + 43, y += 16, 0.000232f * (float) maxL - 0.002320f)
                .setSize(58, 14)
                .setHoverTexts("auction.manage.hover.duration",
                        "auction.manage.hover.duration.max",
                        "auction.manage.hover.duration.info");
        if (select != null) {
            tf_1.setValue(minL);
            tf_2.setValue(maxL);
            select.setDelays(minL * 60000L, maxL * 60000L);
            resetDelays(s_1, s_2);
        }
        // Bidding
        label = Component.translatable("auction.bid").append(":");
        wLabel = font.width(label);
        x = wLabel < lW ? x1 + lW - wLabel : x1;
        addLabel(lId++, x, (y += 18) + 2, label)
                .setSize(Math.min(wLabel, lW), 10);
        min = select != null ? (float) select.getBidRaise() : 4.0f;
        addTextField(5, x2, y, 40, 14, min)
                .setMinMaxDefault(2.0d, 10.0d, min)
                .setIsEnabled(select != null)
                .setHoverTexts("auction.manage.hover.bid.raise");
        addSlider(5, x2 + 43, y, 0.125f * min - 0.25f)
                .setSize(58, 14)
                .setHoverTexts("auction.manage.hover.bid.raise");
        // Initial Bid
        label = Component.translatable("auction.manage.init.bid").append(":");
        wLabel = font.width(label);
        x = wLabel < lW ? x1 + lW - wLabel : x1;
        addLabel(lId++, x, (y += 18) + 2, label)
                .setSize(Math.min(wLabel, lW), 10);
        min = select != null ? (float) select.getInitialBid() : 5.0f;
        addTextField(6, x2, y, 40, 14, min)
                .setMinMaxDefault(5.0d, 50.0d, min)
                .setIsEnabled(select != null)
                .setHoverTexts("auction.manage.hover.initial.bid");
        addSlider(6, x2 + 43, y, 0.022222f * min - 0.111111f)
                .setSize(58, 14)
                .setHoverTexts("auction.manage.hover.initial.bid");
        // Max Lots
        label = Component.translatable("auction.manage.max.lots").append(":");
        wLabel = font.width(label);
        x = wLabel < lW ? x1 + lW - wLabel : x1;
        addLabel(lId++, x, (y += 18) + 2, label)
                .setSize(Math.min(wLabel, lW), 10);
        minL = select != null ? select.getMaxLotsInPlayer() : 10;
        addTextField(7, x2, y, 40, 14, minL)
                .setMinMaxDefault(0, 1000, select != null ? select.getMaxLotsInPlayer() : 50)
                .setIsEnabled(select != null)
                .setHoverTexts("auction.manage.hover.max.lots");
        addSlider(7, x2 + 43, y, 0.01f * (float) minL)
                .setSize(58, 14)
                .setHoverTexts("auction.manage.hover.max.lots");
        // is money
        label = Component.translatable("gui.type").append(" (")
                .append(Component.translatable("availability.currency")).append("):");
        wLabel = font.width(label);
        x = wLabel < lW ? x1 + lW - wLabel : x1;
        addLabel(lId++, x, (y += 18) + 2, label)
                .setSize(Math.min(wLabel, lW), 10);
        addButton(4, x2 - 1, y - 1, false, select != null ? select.getCurrencyType() : 0, "auction.manage.all", "auction.manage.only.money", "auction.manage.only.donate")
                .setSize(102, 16)
                .setIsEnabled(select != null)
                .setHoverTexts("auction.manage.hover.money.type");
        // game GUI
        label = Component.translatable("gui.type").append(" (")
                .append(Component.translatable("gui.texture")).append("):");
        wLabel = font.width(label);
        x = wLabel < lW ? x1 + lW - wLabel : x1;
        addLabel(lId, x, (y += 18) + 2, label)
                .setSize(Math.min(wLabel, lW), 10);
        addButton(5, x2 - 1, y - 1, false, select != null ? select.getTextureType() : 0, "0", "1", "2")
                .setSize(42, 16)
                .setHoverTexts("auction.manage.hover.texture.type");
    }

    @Override
    public void buttonEvent(@Nonnull GuiButtonNop button) {
        AuctionController aData = AuctionController.getInstance();
        switch (button.id) {
            case 0: {
                save();
                AuctionHouse auction = aData.addAuction();
                if (auction != null) {
                    select = auction;
                    init();
                }
                break;
            }  // add auction
            case 1:{
                if (select != null && aData.auctionHouses.size() > 1) {
                    aData.removeAuction(select.getId());
                    select = null;
                    init();
                }
                break;
            } // delete auction
            case 2: {
                if (select != null) {
                    NoppesUtil.requestOpenGUI(EnumGuiType.ManageAuctionForbiddenItem, new BlockPos(select.getId(), -1, 0)); break;
                }
                break;
            }  // add forbidden item
            case 3:{
                if (select != null && aData.auctionHouses.size() > 1) {
                    aData.removeAuction(select.getId());
                    select = null;
                    init();
                }
                break;
            } // delete forbidden item
            case 4:{
                if (select != null) {
                    select.setCurrencyType(button.getValue());
                    init();
                }
                break;
            } // currency type
            case 5:{
                if (select != null) {
                    select.setTextureType(button.getValue());
                    init();
                }
                break;
            } // texture type
            case 66: onClose(); break;
        }
    }

    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics) {
        super.renderBackground(graphics);
        if (select != null) {
            int type = select.getTextureType();
            ResourceLocation bg = new ResourceLocation(CustomNpcs.MODID, "textures/gui/auctions/background_" + type + ".png");
            ResourceLocation bt = new ResourceLocation(CustomNpcs.MODID, "textures/gui/auctions/buttons_" + type + ".png");
            ResourceLocation st = new ResourceLocation(CustomNpcs.MODID, "textures/gui/auctions/inventory_" + type + ".png");
            // background
            PoseStack matrixStack = graphics.pose();
            matrixStack.pushPose();
            matrixStack.translate(scrollForbiddenItems.x, guiTop + imageHeight - 58, 0);
            matrixStack.scale(0.5f, 0.5f, 0.5f);
            graphics.blit(bg, 0, 0, 0, 0, 143, 70);
            graphics.blit(bg, 0, 70, 0, 186, 143, 70);
            graphics.blit(bg, 143, 0, 113, 0, 143, 70);
            graphics.blit(bg, 143, 70, 113, 186, 143, 70);
            // inventory
                matrixStack.pushPose();
                matrixStack.translate(5.0f, 5.0f, 0);
                matrixStack.scale(1.25f, 1.25f, 1.25f);
                graphics.blit(st, 0, 0, 0, 0, 164, 78);
                matrixStack.popPose();

            // buttons
                matrixStack.pushPose();
                matrixStack.translate(110, 106, 0);
                graphics.blit(bt, 0, 0, 0, 112, 40, 28);
                graphics.blit(bt, 40, 0, 88, 112, 40, 28);
                matrixStack.translate(90, 0, 0);
                graphics.blit(bt, 0, 0, 128, 112, 40, 28);
                graphics.blit(bt, 40, 0, 216, 112, 40, 28);
                matrixStack.popPose();
            matrixStack.popPose();

            if (isMouseHover(wrapper.mouseX, wrapper.mouseY, scrollForbiddenItems.x, guiTop + imageHeight - 58, 143, 70)) {
                setHoverText("auction.manage.hover.gui.variant");
            }
        }
    }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) {
        select = dataAH.get(scroll.getNormalSelected());
        init();
    }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
        if (scroll.id == 1 && dataFI.containsKey(scroll.getNormalSelected())) {
            NoppesUtil.requestOpenGUI(EnumGuiType.ManageAuctionForbiddenItem, new BlockPos(select.getId(), scroll.getSelectedIndex(), 0));
        }
    }

    @Override
    public void save() {
        if (scrollAuctions.hasSelected() && dataAH.containsKey(scrollAuctions.getNormalSelected())) {
            AuctionHouse auction = dataAH.get(scrollAuctions.getNormalSelected());
            Packets.sendServer(new SPacketAuctionSave(auction.getId(), auction.save()));
        }
    }

    @Override
    public void unFocused(GuiTextFieldNop textField) {
        GuiSliderNop s_1 = getSlider(textField.id);
        GuiSliderNop s_2;
        GuiTextFieldNop tf;
        switch (textField.id) {
            case 0: {
                if (select != null) { select.setName(textField.getValue()); }
                break;
            } // Name
            case 1: {
                tf = getTextField(2);
                s_1 = getSlider(3);
                s_2 = getSlider(4);
                if (select != null && tf != null && s_1 != null && s_2 != null) {
                    double min = Math.min(textField.getDouble(), tf.getDouble());
                    double max = Math.max(textField.getDouble(), tf.getDouble());
                    textField.setValue(Math.round(min * 10.0f) / 10.0f);
                    tf.setValue(Math.round(max * 10.0f) / 10.0f);
                    select.setCommission(min, max);
                    resetCommissions(s_1, s_2);
                }
                break;
            } // Commission Min
            case 2: {
                tf = getTextField(1);
                s_1 = getSlider(3);
                s_2 = getSlider(4);
                if (select != null && tf != null && s_1 != null && s_2 != null) {
                    double min = Math.min(textField.getDouble(), tf.getDouble());
                    double max = Math.max(textField.getDouble(), tf.getDouble());
                    tf.setValue(Math.round(min * 10.0f) / 10.0f);
                    textField.setValue(Math.round(max * 10.0f) / 10.0f);
                    select.setCommission(min, max);
                    resetCommissions(s_1, s_2);
                }
                break;
            } // Commission Max
            case 3: {
                tf = getTextField(4);
                s_1 = getSlider(3);
                s_2 = getSlider(4);
                if (select != null && tf != null && s_1 != null && s_2 != null) {
                    long min = Math.min(textField.getLong(), tf.getLong()) * 60000L;
                    long max = Math.max(textField.getLong(), tf.getLong()) * 60000L;
                    textField.setValue(min / 60000L);
                    tf.setValue(max / 60000L);
                    select.setDelays(min, max);
                    resetDelays(s_1, s_2);
                }
                break;
            } // Duration Min
            case 4: {
                tf = getTextField(3);
                s_1 = getSlider(3);
                s_2 = getSlider(4);
                if (select != null && tf != null && s_1 != null && s_2 != null) {
                    long min = Math.min(textField.getLong(), tf.getLong()) * 60000L;
                    long max = Math.max(textField.getLong(), tf.getLong()) * 60000L;
                    tf.setValue(min / 60000L);
                    textField.setValue(max / 60000L);
                    select.setDelays(min, max);
                    resetDelays(s_1, s_2);
                }
                break;
            } // Duration Max
            case 5: {
                if (select != null && s_1 != null) {
                    select.setBidRaise(textField.getDouble());
                    textField.setValue(Math.round(select.getBidRaise() * 10.0f) / 10.0f);
                    resetBidRaise(s_1);
                }
                break;
            } // Bid Raise
            case 6: {
                if (select != null && s_1 != null) {
                    select.setInitialBid(textField.getDouble());
                    textField.setValue(Math.round(select.getInitialBid() * 10.0f) / 10.0f);
                    resetInitialBid(s_1);
                }
                break;
            } // Initial Bid
            case 7: {
                if (select != null && s_1 != null) {
                    select.setMaxLotsInPlayer(textField.getInteger());
                    textField.setValue(select.getMaxLotsInPlayer());
                    resetMaxLotsInPlayer(s_1);
                }
                break;
            } // Max Bid Time
        }
    }

    @Override
    public void mouseDragged(GuiSliderNop slider) {
        if (select != null) {
            GuiTextFieldNop tf_1 = getTextField(slider.id);
            GuiTextFieldNop tf_2;
            GuiSliderNop s;
            switch (slider.id) {
                case 1: {
                    s = getSlider(2);
                    tf_1 = getTextField(1);
                    tf_2 = getTextField(2);
                    if (s != null && tf_1 != null && tf_2 != null) {
                        float v_0 = 14.5f * slider.getValue() + 0.5f;
                        float v_1 = tf_2.getFloat();
                        select.setCommission(v_0, v_1);
                        tf_1.setValue(Math.round(select.getCommission()[0] * 10.0d) / 10.0d);
                        tf_2.setValue(Math.round(select.getCommission()[1] * 10.0d) / 10.0d);
                        resetCommissions(slider, s);
                    }
                    break;
                } // Commission Min
                case 2: {
                    s = getSlider(1);
                    tf_1 = getTextField(1);
                    tf_2 = getTextField(2);
                    if (s != null && tf_1 != null && tf_2 != null) {
                        float v_0 = 14.5f * slider.getValue() + 0.5f;
                        float v_1 = tf_1.getFloat();
                        select.setCommission(v_1, v_0);
                        tf_1.setValue(Math.round(select.getCommission()[0] * 10.0d) / 10.0d);
                        tf_2.setValue(Math.round(select.getCommission()[1] * 10.0d) / 10.0d);
                        resetCommissions(s, slider);
                    }
                    break;
                } // Commission Max
                case 3: {
                    s = getSlider(4);
                    tf_1 = getTextField(3);
                    tf_2 = getTextField(4);
                    if (s != null && tf_1 != null && tf_2 != null) {
                        long v_0 = (long) (4310.0f * slider.getValue() + 10.0f) * 60000L;
                        long v_1 = tf_2.getLong() * 60000L;
                        select.setDelays(v_0, v_1);
                        tf_1.setValue(select.getDelays()[0] / 60000L);
                        tf_2.setValue(select.getDelays()[1] / 60000L);
                        resetDelays(slider, s);
                    }
                    break;
                } // Duration Min
                case 4: {
                    s = getSlider(3);
                    tf_1 = getTextField(3);
                    tf_2 = getTextField(4);
                    if (s != null && tf_1 != null && tf_2 != null) {
                        long v_0 = (long) (4310.0f * slider.getValue() + 10.0f) * 60000L;
                        long v_1 = tf_1.getLong() * 60000L;
                        select.setDelays(v_1, v_0);
                        tf_1.setValue(select.getDelays()[0] / 60000L);
                        tf_2.setValue(select.getDelays()[1] / 60000L);
                        resetDelays(s, slider);
                    }
                    break;
                } // Duration Max
                case 5: {
                    if (tf_1 != null) {
                        select.setBidRaise(8.0f * slider.getValue() + 2.0f);
                        tf_1.setValue(Math.round(select.getBidRaise() * 10.0d) / 10.0d);
                        resetBidRaise(slider);
                    }
                    break;
                } // Bid Raise
                case 6: {
                    if (tf_1 != null) {
                        select.setInitialBid(45.0f * slider.getValue() + 5.0f);
                        tf_1.setValue(Math.round(select.getInitialBid() * 10.0d) / 10.0d);
                        resetInitialBid(slider);
                    }
                    break;
                } // Initial Bid
                case 7: {
                    if (tf_1 != null) {
                        select.setMaxLotsInPlayer((int) (100.0f * slider.getValue()));
                        tf_1.setValue(select.getMaxLotsInPlayer());
                        resetMaxLotsInPlayer(slider);
                    }
                    break;
                } // Max Bid Time
            }
        }
    }

    @Override
    public void mousePressed(GuiSliderNop slider) { }

    @Override
    public void mouseReleased(GuiSliderNop slider) { }

    private void resetCommissions(GuiSliderNop s_1, GuiSliderNop s_2) {
        if (select != null && s_1 != null && s_2 != null) {
            s_1.setValue(0.068966f * (float) select.getCommission()[0] - 0.034483f);
            s_2.setValue(0.068966f * (float) select.getCommission()[1] - 0.034483f);
            s_1.setMessage(Component.literal((Math.round((float) select.getCommission()[0] * 10.0f) / 10.0f) + "%"));
            s_2.setMessage(Component.literal((Math.round((float) select.getCommission()[1] * 10.0f) / 10.0f) + "%"));
        }
    }

    private void resetDelays(GuiSliderNop s_1, GuiSliderNop s_2) {
        if (select != null && s_1 != null && s_2 != null) {
            s_1.setValue(0.000232f * (float) (select.getDelays()[0] / 60000L) - 0.002320f);
            s_2.setValue(0.000232f * (float) (select.getDelays()[1] / 60000L) - 0.002320f);
            s_1.setMessage(Component.literal(
                    Util.instance.ticksToElapsedTime(select.getDelays()[0] / 50L, false, false, false)));
            s_2.setMessage(Component.literal(
                    Util.instance.ticksToElapsedTime(select.getDelays()[1] / 50L, false, false, false)));
        }
    }

    private void resetBidRaise(GuiSliderNop slider) {
        if (select != null && slider != null) {
            slider.setValue(0.125f * (float) select.getBidRaise() - 0.25f);
            slider.setMessage(Component.literal((Math.round((float) select.getBidRaise() * 10.0f) / 10.0f) + "%"));
        }
    }

    private void resetInitialBid(GuiSliderNop slider) {
        if (select != null && slider != null) {
            slider.setValue(0.022222f * (float) select.getInitialBid() - 0.111111f);
            String cur;
            if (select.getCurrencyType() == 1) { cur = CustomNpcs.displayCurrencies; }
            else if (select.getCurrencyType() == 2) { cur = CustomNpcs.displayDonation; }
            else { cur = CustomNpcs.displayCurrencies + " | " + CustomNpcs.displayDonation; }
            slider.setMessage(Component.literal((Math.round((float) select.getInitialBid() * 10.0f) / 10.0f) + "% (" + cur + ")"));
        }
    }

    private void resetMaxLotsInPlayer(GuiSliderNop slider) {
        if (select != null && slider != null) {
            slider.setValue(0.01f * (float) select.getMaxLotsInPlayer());
            slider.setMessage(Component.literal(select.getMaxLotsInPlayer() + ""));
        }
    }

}
