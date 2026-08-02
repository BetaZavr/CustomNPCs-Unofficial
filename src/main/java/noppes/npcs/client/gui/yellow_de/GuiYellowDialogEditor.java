package noppes.npcs.client.gui.yellow_de;

import net.minecraft.init.Blocks;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.OptionType;
import noppes.npcs.api.handler.data.IDialogCategory;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.controllers.YDEController;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.global.SubGuiNpcDialogOption;
import noppes.npcs.client.gui.player.GuiDialogInteract;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.client.gui.yellow_de.data.*;
import noppes.npcs.client.gui.yellow_de.data.nodes.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogCategory;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class GuiYellowDialogEditor extends GuiBasic
        implements ICustomScrollListener, ITextfieldListener {

    public static YDEData YDE_DATA = YDEController.getInstance().getLevelData(ScriptController.getLevelKey());
    // window
    public YDELink selectLink;
    protected final List<IComponentGui> notScaledComponents = new ArrayList<>();
    protected float xMouse;
    protected float yMouse;
    protected float w;
    protected float h;
    protected float centerU;
    protected float centerV;
    public float guiScale;
    // back
    protected int space = 30;
    protected final int[] pos = new int[2];
    protected List<YDELink> links;
    // grid
    public YDEWindowNop hovered = null;
    protected boolean mouseOnGrid;
    protected double tempDx;
    protected double tempDy;
    // category
    protected Map<Component, DialogCategory> categoryData = new LinkedHashMap<>();
    public static @Nullable YDECategory category;
    protected Set<Integer> selects = new LinkedHashSet<>();
    protected boolean gridIsMoved;
    protected boolean lmbIsSelect;
    protected final int[] lmbSelect = new int[2];
    // tabs
    protected int tabW = 160;
    protected final @Nonnull GuiCustomWindowNop leftTab;
    protected final @Nonnull GuiCustomWindowNop rightTab;
    protected boolean hoverLeft = false;
    protected boolean hoverRight = false;
    protected GuiCustomScrollNop helper;

    public GuiYellowDialogEditor() {
        super();
        if (mc == null) { mc = Minecraft.getMinecraft(); }
        hoverIsGame = true;
        hoverFont = UtilYDE.FONT;

        YDE_DATA = YDEController.getInstance().getLevelData(ScriptController.getLevelKey());

        leftTab = new GuiCustomWindowNop(this, -1, -tabW, 0, tabW, height, Component.translatable("yde.categories"));
        leftTab.setCustomFont(UtilYDE.FONT);
        leftTab.colorLine = YDEController.leftTabColor;
        leftTab.isLock = false;
        leftTab.lock.setIsEnabled(false);
        leftTab.lock.setUV(232, 0, 24, 24);
        leftTab.lock.setX(leftTab.lock.getX() + 1);
        leftTab.lock.setY(leftTab.lock.getY() - 1);
        leftTab.exit.setDisplayText(Component.literal("x"));
        leftTab.exit.setColor(YDEController.textColor);
        leftTab.exit.setCustomFont(UtilYDE.FONT);
        leftTab.exit.setX(leftTab.exit.getX() + 1);
        leftTab.exit.setY(leftTab.exit.getY() - 1);

        rightTab = new GuiCustomWindowNop(this, -2, width, 0, tabW, height, Component.translatable("type.help"));
        rightTab.setCustomFont(UtilYDE.FONT);
        rightTab.isLock = false;
        rightTab.colorLine = YDEController.rightTabColor;
        rightTab.lock.setIsEnabled(false);
        rightTab.lock.setUV(232, 0, 24, 24);
        rightTab.lock.setX(rightTab.lock.getX() + 1);
        rightTab.lock.setY(rightTab.lock.getY() - 1);
        rightTab.exit.setDisplayText(Component.literal("x"));
        rightTab.exit.setColor(YDEController.textColor);
        rightTab.exit.setCustomFont(UtilYDE.FONT);
        rightTab.exit.setX(rightTab.exit.getX() + 1);
        rightTab.exit.setY(rightTab.exit.getY() - 1);

        rightTab.yde_scroll = new YDEScrollNop(this, 3, 14, tabW - 7, height - 16);
        rightTab.yde_scroll.setCustomFont(UtilYDE.FONT);
        rightTab.add(rightTab.yde_scroll);

        if (((YDEController.backColor >> 24) & 0xFF) != 192) {
            int alpha = 0xC0000000;
            YDEController.backColor = (YDEController.backColor & 0xFFFFFF) | alpha;
            YDEController.backHoverColor = (YDEController.backHoverColor & 0xFFFFFF) | alpha;
            YDEController.textColor = (YDEController.textColor & 0xFFFFFF) | alpha;
            YDEController.windowLineColor = (YDEController.windowLineColor & 0xFFFFFF) | alpha;
            YDEController.gridColor = (YDEController.gridColor & 0xFFFFFF) | alpha;
        }

        Packets.sendServer(new SPacketDialogCategoryGet());
    }

    @Override
    public void initGui() {
        if (hasSubGui()) { wrapper.subgui.initGui(); }
        // super initGui:
        guiScale = (float) scaledResolution.getScaleFactor();
        buttonList.clear();
        wrapper.initGui(mc, width, height);
        notScaledComponents.clear();
        // this initGui
        if (category == null) {
            if (DialogController.instance.getCategory(YDEController.getInstance().lastCategory) == null) {
                YDEController.getInstance().lastCategory = "";
                List<IDialogCategory> cats = DialogController.instance.categories();
                if (!cats.isEmpty()) { YDEController.getInstance().lastCategory = cats.get(0).getName(); }
            }
            category = YDE_DATA.getCategory(YDEController.getInstance().lastCategory);
        }
        YDEController.getInstance().lastCategory = category.category;
        links = YDE_DATA.getLinks(category.category);

        leftTab.imageHeight = (int) (height * guiScale);
        rightTab.imageHeight = (int) (height * guiScale);
        if (rightTab.yde_scroll != null) {
            rightTab.yde_scroll.height = rightTab.imageHeight / 2 - 18;
            rightTab.yde_scroll.initGui();
        }
        add(leftTab);
        add(rightTab);

        GuiCustomScrollNop scroll = leftTab.getScroll(0);
        Component selectedCategory = null;
        if (scroll == null) { scroll = leftTab.addScroll(0); }
        List<Component> categories = new ArrayList<>();
        categoryData.clear();
        for (DialogCategory c : DialogController.instance.categories.values()) {
            Component key = Component.empty()
                    .append(Component.literal("ID:" + c.id + " ").withStyle(TextFormatting.GRAY))
                    .append(Component.translatable(c.title));
            categories.add(key);
            categoryData.put(key, c);
            if (c.title.equals(category.category)) { selectedCategory = key; }
        }
        scroll.setCustomFont(UtilYDE.FONT);
        scroll.setPos(leftTab.getX() + 2, leftTab.getY() + 14);
        scroll.setSize(leftTab.imageWidth - 4, leftTab.imageHeight / 2 - 34);
        scroll.setNormalList(categories);
        scroll.setSelected(selectedCategory);
        scroll.border = YDEController.windowLineColor;
        leftTab.addButton(0, 4, leftTab.imageHeight / 2 - 18, "gui.add")
                .setCustomFont(UtilYDE.FONT)
                .setTexture(YDE_BUTTONS)
                .setDefBack(false)
                .setIsAnim(true)
                .setUV(0, 0, 200, 20)
                .setColor(YDEController.textColor)
                .setSize(40, 16);
        leftTab.addButton(1, 46, leftTab.imageHeight / 2 - 18, "gui.remove")
                .setCustomFont(UtilYDE.FONT)
                .setTexture(YDE_BUTTONS)
                .setDefBack(false)
                .setIsAnim(true)
                .setUV(0, 0, 200, 20)
                .setColor(YDEController.textColor)
                .setSize(40, 16)
                .setIsEnabled(scroll.hasSelected());
        leftTab.addButton(2, 88, leftTab.imageHeight / 2 - 18, "gui.edit")
                .setCustomFont(UtilYDE.FONT)
                .setTexture(YDE_BUTTONS)
                .setDefBack(false)
                .setIsAnim(true)
                .setUV(0, 0, 200, 20)
                .setColor(YDEController.textColor)
                .setSize(70, 16)
                .setIsEnabled(scroll.hasSelected());
        if (helper == null) { helper = addScroll(25).setIsVisible(false); }
        helper.setCustomFont(UtilYDE.FONT)
                .setIsSimpleSelect(true)
                .setHoverScale(2.0f, 1.0f)
                .disabledSearch();
        helper.border = YDEController.windowLineColor;
        add(helper);
        // category name
        YDE_DATA.check();
        // not scaled components
        Component t = category.getTitle();
        GuiLabel label = addLabel(0, (int) (((width - UtilYDE.FONT_HEADLINE.width(t)) * guiScale) / 4.0f), 0, t)
                .setCustomFont(UtilYDE.FONT_HEADLINE)
                .setColor(YDEController.textColor);
        notScaledComponents.add(label);
        GuiTextFieldNop textField = getTextField(0);
        boolean bo = textField != null && textField.isVisible();
        textField = addTextField(0, label.getX(), label.getY(), label.getWidth(), 20, category.category)
                .setIsVisible(bo);
        notScaledComponents.add(textField);
        notScaledComponents.add(leftTab);
        notScaledComponents.add(rightTab);
        notScaledComponents.add(helper);
        w = width * guiScale / 2.0f;
        h = height * guiScale / 2.0f;
        List<Integer> sls = new ArrayList<>(selects);
        selects.clear();
        for (YDENode node : YDE_DATA.nodes.values()) {
            if (!(node instanceof YDECategory) && node.category.equals(category.category)) {
                if (node instanceof YDEArea) { add(new YDEAreaNop(this, (YDEArea) node)); }
                else { add(new YDEWindowNop(this, node)); }
                if (sls.contains(node.id)) { selects.add(node.id); }
            }
        }
        int lastNode = YDEController.getInstance().lastNode.getOrDefault(category.category, -1);
        if (lastNode > -1) {
            sls = new ArrayList<>(selects);
            selects.clear();
            selects.add(lastNode);
            for (int id : sls) {
                if (id != lastNode) { selects.add(id); }
            }
        }
        setSelectNode(rightTab.yde_scroll.tabId);
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        if (!hasSubGui()) { mouseButtonEvent(null, button, 0); }
    }

    @Override
    public void drawDefaultBackground() {
        if (mc == null) { mc = Minecraft.getMinecraft(); }
        // Grid
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0f, 0.0f, -1.0f);
        // background
        drawRect(0, 0, (int) w, (int) h, YDEController.backColor);
        // center
        int gridColor = category == null || category.id < 0 ? YDEController.gridColorEmpty : YDEController.gridColor;
        UtilYDE.renderDot(new float[] { centerU, centerV }, 0.75f, false, gridColor);
        // lines
        float r = (float) ((gridColor >> 16) & 0xFF) / 255.0F;
        float g = (float) ((gridColor >> 8) & 0xFF) / 255.0F;
        float b = (float) (gridColor & 0xFF) / 255.0F;
        float step = 20.0f;
        if (category != null) {
            centerU = (int) (category.x + w / 2.0f);
            centerV = (int) (category.y + h / 2.0f);
            step *= category.getScale();
        }
        float t = 0.25f;
        int i = (int) Math.floor((centerV) / -step);
        int j = (int) Math.floor((centerU) / -step);
        float u1 = centerU + j * step;
        float v1 = centerV + i * step;
        // horizontal lines
        while (v1 < height * guiScale) {
            if (i % 10 == 0) {
                UtilYDE.fill(0, v1 - t, width * guiScale, v1 + t, r, g, b, i == 0 ? 1.0f : 0.8f);
            }
            else {
                UtilYDE.fill(0, v1 - t, width * guiScale, v1 + t, r, g, b, 0.35f);
            }
            v1 += step;
            i++;
        }
        // vertical lines
        while (u1 < width * guiScale) {
            if (j % 10 == 0) {
                UtilYDE.fill(u1 - t, 0, u1 + t, height * guiScale, r, g, b, j == 0 ? 1.0f : 0.8f);
            }
            else {
                UtilYDE.fill(u1 - t, 0, u1 + t, height * guiScale, r, g, b, 0.35f);
            }
            u1 += step;
            j++;
        }
        renderSelectedBorder();
        GlStateManager.popMatrix();

        // links
        if (!links.isEmpty()) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, -0.5f);
            // link dots
            for (YDELink link : links) {
                YDEWindowNop baskNode = get(link.back, YDEWindowNop.class);
                YDEWindowNop nextNode = get(link.next, YDEWindowNop.class);
                if (baskNode != null && nextNode != null) {
                    float[] point0 = null;
                    float[] point1 = null;
                    boolean turned = false;
                    int color = 0;
                    if (link.type == EnumYDEType.OPTION) {
                        point0 = new float[] { baskNode.getX() + baskNode.imageWidth, baskNode.getY() + baskNode.imageHeight / 2.0f };
                        point1 = new float[] { nextNode.getX(), nextNode.getY() + nextNode.imageHeight / 2.0f };
                        turned = baskNode.getX() + baskNode.imageWidth > nextNode.getX();
                        color = selectLink != link ? EnumYDEType.OPTION.color : 0xFFFFFFFF;
                    }
                    else if (link.type == EnumYDEType.DIALOG) {
                        point0 = new float[] { baskNode.getX() + baskNode.imageWidth, baskNode.getY() + baskNode.imageHeight / 2.0f };
                        point1 = new float[] { nextNode.getX(), nextNode.getY() + nextNode.imageHeight / 2.0f };
                        turned = baskNode.getX() + baskNode.imageWidth > nextNode.getX();
                        color = selectLink != link ? EnumYDEType.DIALOG.color : 0xFFFFFFFF;
                    }
                    else if (link.type == EnumYDEType.NPC) {
                        point0 = new float[] { baskNode.getX(), baskNode.getY() + baskNode.imageHeight * 0.8f };
                        point1 = new float[] { nextNode.getX() + nextNode.imageWidth, nextNode.getY() + nextNode.imageHeight / 2.0f };
                        turned = baskNode.getX() > nextNode.getX() + nextNode.imageWidth;
                        color = selectLink != link ? EnumYDEType.NPC.color : 0xFFFFFFFF;
                    }
                    else if (link.type == EnumYDEType.QUEST) {
                        point0 = new float[] { baskNode.getX() + baskNode.imageWidth / 2.0f, baskNode.getY() + baskNode.imageHeight };
                        point1 = new float[] { nextNode.getX() + nextNode.imageWidth / 2.0f, nextNode.getY() };
                        turned = baskNode.getY() + baskNode.imageHeight < nextNode.getY();
                        color = selectLink != link ? EnumYDEType.QUEST.color : 0xFFFFFFFF;
                    }
                    if (point0 != null) {
                        GlStateManager.pushMatrix();
                        float catScale = category != null ? category.getScale() : 1.0f;
                        GlStateManager.translate(centerU, centerV, 0.0f);
                        GlStateManager.scale(catScale, catScale, catScale);
                        UtilYDE.renderSpline(point0, point1, baskNode.isHovered() || nextNode.isHovered(), turned, color, 0.0f);
                        GlStateManager.popMatrix();
                    }
                }
                else {
                    if (YDE_DATA.nodes.get(link.back) == null || YDE_DATA.nodes.get(link.next) == null) { YDE_DATA.removeLink(link); } // not found
                    else {

                    } // another category
                }
            }
            GlStateManager.popMatrix();
        }
        if (selectLink != null) {
            if (Mouse.isButtonDown(0)) {
                GlStateManager.pushMatrix();

                GlStateManager.popMatrix();
            }
            else {
                if (selectLink.parent != null) {
                    LogWriter.info("TEST: "+selectLink.parent);
                }
                selectLink = null;
            }
        }

        // labels
        GlStateManager.pushMatrix();
        GlStateManager.translate(leftTab.getX() + leftTab.imageWidth + 8.0f, 0.0f, 0.0f);
        if (category != null) {
            // grid offset
            String label = "U: " + -category.x + "; V: " + -category.y;
            UtilYDE.FONT.draw(label, 0.0f, 0.0f, YDEController.textColor);
            // scale
            label = "x" + df2.format(category.getScale());
            UtilYDE.FONT.draw(label, 0.0f, UtilYDE.FONT.getHeight(), YDEController.textColor);
        }
        // mouse pos hover
        mouseOnGrid = (hovered == null || !hovered.isHovered()) && !leftTab.isHovered() && !hoverLeft && !rightTab.isHovered() && !hoverRight;
        if (mouseOnGrid) {
            for (IComponentGui component : new ArrayList<>(wrapper.components)) {
                if (component.isHovered()) {
                    mouseOnGrid = false;
                    break;
                }
            }
        }
        GlStateManager.popMatrix();
        if (category != null) {
            pos[0] = (int) ((xMouse - w / 2.0f - category.x) / category.getScale());
            pos[1] = (int) ((yMouse - h / 2.0f - category.y) / category.getScale());
            if (mouseOnGrid) {
                GlStateManager.pushMatrix();
                float xm = xMouse + 2.0f;
                float ym = yMouse + 11.0f;
                String label = pos[0] + "; " + pos[1];
                float f = 4 + UtilYDE.FONT.width(label);
                if (xm + f > rightTab.getX() - 5.0f) { xm -= xm - rightTab.getX() + 5.0f + f; }
                f = 4 + UtilYDE.FONT.getHeight();
                if (ym + f > h) { ym -= 19.0f; }
                GlStateManager.translate(xm, ym, 1.0f);
                drawRect(-2, -1,
                        UtilYDE.FONT.width(label) + 2, UtilYDE.FONT.getHeight() + 1, YDEController.backColor);
                UtilYDE.FONT.draw(label, 0, 0, YDEController.textColor);
                GlStateManager.popMatrix();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (links == null || helper == null) { return; }
        float scale = 2.0f / guiScale;
        xMouse = mouseX / scale;
        yMouse = mouseY / scale;
        GlStateManager.enableBlend();
        // tabs
        int tabHeight = leftTab.imageHeight / 2;
        int arrowHeight = (int) (25.0f * guiScale);
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
        // main
        float catScale = category != null ? category.getScale() : 1.0f;
        wrapper.mouseX = (int) (((hasSubGui() ? 0.0f : xMouse) - centerU) / catScale);
        wrapper.mouseY = (int) (((hasSubGui() ? 0.0f : yMouse) - centerV) / catScale);
        if (drawDefaultBackground) { drawDefaultBackground(); }
        // super

        hovered = null;
        GlStateManager.pushMatrix();
        GuiLabel label = getLabel(0);
        if (label != null) { label.render(mouseX, mouseY, partialTicks); }
        GuiTextFieldNop textField = getTextField(0);
        if (textField != null) { textField.render(mouseX, mouseY, partialTicks); }
        GlStateManager.translate(centerU, centerV, 0.0f);
        GlStateManager.scale(catScale, catScale, catScale);
        for (IComponentGui component : new ArrayList<>(wrapper.components)) {
            if (!notScaledComponents.contains(component)) {
                component.render(wrapper.mouseX, wrapper.mouseY, partialTicks);
            }
        }
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        if (hasSubGui()) {
            GlStateManager.translate(0.0F, 0.0F, 60.0F);
            wrapper.subgui.drawScreen(mouseX, mouseY, partialTicks);
            GlStateManager.translate(0.0F, 0.0F, -60.0F);
        }
        else {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0f, 0.0f, 0.0f);
            // left arrow
            if (leftTab.isYDEShow) {
                hoverLeft = xMouse >= leftTab.getX() + leftTab.imageWidth && xMouse < leftTab.getX() + leftTab.imageWidth + 7;
                leftTab.transferTo(0, 0);
            }
            else {
                hoverLeft = xMouse < 7.0f;
                leftTab.transferTo(ValueUtil.correctInt((int) (-leftTab.imageWidth + (hoverLeft ? -xMouse + 7.0f : 0.0f)), -tabW, 7 - tabW), 0);
            }
            if (leftTab.visible) {
                GlStateManager.pushMatrix();
                int c = hoverLeft ? YDEController.backHoverColor : YDEController.backColor;
                GlStateManager.translate(leftTab.getX() + leftTab.getWidth(), 0.0f, 1.0f);
                drawRect(0, 0, 6, tabHeight, 0xF0000000 | (c & 0xFFFFFF));
                GlStateManager.translate(0.0f, (tabHeight - arrowHeight / 2.0f) / 2.0f, 0.0f);
                GlStateManager.scale(0.5f, 0.5f, 0.5f);
                c = hoverLeft ? YDEController.backColor : YDEController.backHoverColor;
                drawHorizontalLine(-1, 10, 1, c);
                drawVerticalLine(10, 1, arrowHeight, c);
                drawHorizontalLine(-1, 10, arrowHeight, c);
                GlStateManager.popMatrix();

                GlStateManager.pushMatrix();
                GlStateManager.translate(leftTab.getX() + leftTab.getWidth() + 1.0f, (tabHeight - UtilYDE.FONT.getHeight()) / 2.0f, 1.5f);
                UtilYDE.FONT.draw(leftTab.isYDEShow ? "<" : ">", 0.0f, 0.0f, YDEController.textColor);
                GlStateManager.popMatrix();
            }
            // right arrow
            if (rightTab.isYDEShow) {
                hoverRight = xMouse >= rightTab.getX() - 6.0f && xMouse < rightTab.getX();
                rightTab.transferTo((int) (w - leftTab.imageWidth), 0);
            }
            else {
                hoverRight = xMouse > w - 8.0f;
                rightTab.transferTo(ValueUtil.correctInt((int) (w + (hoverRight ? -1.0f * (xMouse - w + 8.0f): 0.0f)), (int) (w - 7.0f), (int) w), 0);
            }
            if (rightTab.visible) {
                GlStateManager.pushMatrix();
                int c = hoverRight ? YDEController.backHoverColor : YDEController.backColor;
                GlStateManager.translate(rightTab.getX() - 6.0f, 0.0f, 1.0f);
                drawRect(0, 0, 6, tabHeight, 0xF0000000 | (c & 0xFFFFFF));
                GlStateManager.translate(0.0f, (tabHeight - arrowHeight / 2.0f) / 2.0f, 0.0f);
                GlStateManager.scale(0.5f, 0.5f, 0.5f);
                c = hoverRight ? YDEController.backColor : YDEController.backHoverColor;
                drawHorizontalLine(1, 12, 1, c);
                drawVerticalLine(1, 1, arrowHeight, c);
                drawHorizontalLine(1, 12, arrowHeight, c);
                GlStateManager.popMatrix();

                GlStateManager.pushMatrix();
                GlStateManager.translate(rightTab.getX() - 4.5f, (tabHeight - UtilYDE.FONT.getHeight()) / 2.0f, 1.5f);
                UtilYDE.FONT.draw(rightTab.isYDEShow ? ">" : "<", 0.0f, 0.0f, YDEController.textColor);
                GlStateManager.popMatrix();
            }
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();

        GlStateManager.translate(0.0f, 0.0f, 500.0f);
        for (IComponentGui component : new ArrayList<>(notScaledComponents)) {
            if (component == helper) { GlStateManager.translate(0.0f, 0.0f, 1.0f); }
            component.render((int) xMouse, (int) yMouse, partialTicks);
            if (component == helper) { GlStateManager.translate(0.0f, 0.0f, -1.0f); }
        }

        GlStateManager.popMatrix();
        if ((hoverIsGame || (CustomNpcs.ShowDescriptions && GuiBasic.showHoverText)) && !hoverText.isEmpty()) {
            if (!hoverIsGame) { hoverText.add(Component.translatable("hover.alt.h")); }
            GlStateManager.disableDepth();
            if (hoverFont == null) { drawHoveringText(toHoverText(), mouseX, mouseY, fontRenderer); }
            else {
                scale = 2.0f;
                if (guiScale > 1.0f) { scale = 1.0f / guiScale * 2.0f; }
                renderTooltipInternal(mouseX, ValueUtil.correctInt(mouseY, 16, height), this, hoverFont, hoverText, scale);
            }
            hoverText.clear();
        }
        if (rightTab.isHeadHovered() && rightTab.isYDEShow) {
            if (!helper.isVisible() || helper.type != 1) {
                helper.type = 1;
                Component h0 = Component.translatable("yde.help.multiple");
                Component h1 = Component.translatable("yde.help.sel.next");
                Component h2 = Component.translatable("yde.help.step.offset");
                Component h3 = Component.translatable("yde.help.hover.info.0");
                Component h4 = Component.translatable("yde.help.hover.info.1");
                Component h5 = Component.translatable("yde.help.extra");

                Component s0 = Component.literal("Ctrl").withStyle(TextFormatting.AQUA)
                        .append(Component.literal("+").withStyle(TextFormatting.WHITE))
                        .append(Component.translatable("yde.lmb").withStyle(TextFormatting.YELLOW));
                Component s1 = Component.translatable("yde.help.double").append(" ")
                        .append(Component.translatable("yde.lmb").withStyle(TextFormatting.YELLOW));
                Component s2 = Component.translatable("yde.help.hold").append(" ")
                        .append(Component.literal("Shift").withStyle(TextFormatting.AQUA));
                Component s3 = Component.translatable("yde.rmb").withStyle(TextFormatting.YELLOW);

                int w = ValueUtil.max(UtilYDE.FONT.width(h0) + UtilYDE.FONT.width(s0) + space,
                        UtilYDE.FONT.width(h1) + UtilYDE.FONT.width(s1) + space,
                        UtilYDE.FONT.width(h2) + UtilYDE.FONT.width(s2) + space,
                        UtilYDE.FONT.width(h5) + UtilYDE.FONT.width(s3) + space,
                        UtilYDE.FONT.width(h3), UtilYDE.FONT.width(h4));
                StringBuilder ig = new StringBuilder("―");
                while (UtilYDE.FONT.width(ig.toString()) < w) { ig.append("―"); }
                w = UtilYDE.FONT.width(ig.toString());
                Component ignore = Component.literal(ig.toString()).withStyle(TextFormatting.GRAY);
                List<Component> list = Lists.newArrayList(h0, h1, h2, h5, ignore, h3, h4);
                helper.setPos((int) (xMouse - w - 5), (int) (yMouse + 5))
                        .setSize(w + 2, (UtilYDE.FONT.getHeight() + 4) * list.size() + 4)
                        .setIsEnabled(false)
                        .setIsVisible(true)
                        .setUnsortedList(list)
                        .setSuffixes(Lists.newArrayList(s0, s1, s2, s3, Component.empty(),
                                Component.empty(), Component.empty()))
                        .setSelect(-1);
            }
        }
        else {
            helper.setIsVisible(helper.isVisible() &&
                    xMouse >= helper.getX() - 10 && xMouse <= helper.getX() + helper.getWidth() + 10 &&
                    yMouse >= helper.getY() - 10 && yMouse <= helper.getY() + helper.getHeight() + 10);
        }
        // LMB select
        if (lmbIsSelect) {
            if (Mouse.isButtonDown(0)) {
                lmbIsSelect = false;
                List<YDEWindowNop> tempSelect = getAreaSelect(pos[0], pos[1], lmbSelect[0], lmbSelect[1]);
                if (!tempSelect.isEmpty()) {
                    IComponentGui sel = getSelect();
                    if (sel != null) { unFocused(sel); }
                    selects.clear();
                    boolean isSelect = true;
                    for (YDEWindowNop win : tempSelect) {
                        if (isSelect) {
                            YDEController.getInstance().lastNode.put(category.category, win.id);
                            isSelect = false;
                        }
                        selects.add(win.id);
                    }
                    setSelectNode(0);
                }
            }
            else {
                int x = (int) (Math.min(lmbSelect[0], pos[0]) * category.getScale() + centerU - 1.0f);
                int y = (int) (Math.min(lmbSelect[1], pos[1]) * category.getScale() + centerV - 1.0f);
                int w = (int) ((Math.max(lmbSelect[0], pos[0]) - Math.min(lmbSelect[0], pos[0])) * category.getScale() + 2.75f);
                int h = (int) ((Math.max(lmbSelect[1], pos[1]) - Math.min(lmbSelect[1], pos[1])) * category.getScale() + 1.75f);
                renderBorder(x, y, w, h, YDEController.componentLineColor);
                boolean isSelect = true;
                for (YDEWindowNop win : getAreaSelect(pos[0], pos[1], lmbSelect[0], lmbSelect[1])) {
                    x = (int) (win.node.x * category.getScale() + centerU - 1.0f);
                    y = (int) (win.node.y * category.getScale() + centerV - 1.0f);
                    w = (int) (win.node.width * category.getScale() + 2.75f);
                    h = (int) (win.node.height * category.getScale() + 1.75f);
                    renderBorder(x, y, w, h, isSelect ? YDEController.selectLineColor : YDEController.hoverLineColor);
                    if (isSelect) { isSelect = false; }
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
        if (!hasSubGui()) {
            boolean bo = false;
            for (IComponentGui component : new ArrayList<>(notScaledComponents)) {
                if (component.mouseScrolled(mouseX, mouseY, scrolled)) { bo = true; }
            }
            if (!bo) { bo = wrapper.mouseScrolled(xMouse, yMouse, scrolled); }
            if (!bo && category != null) {
                float oldScale = category.getScale();
                float f0 = category.getScale() * (scrolled < 0.0f ? 0.1f : -0.1f);
                float newScale = ValueUtil.correctFloat(oldScale + f0, 0.1f, 1.0f);
                if (newScale != oldScale) {
                    float mouseGridX = (xMouse - centerU) / oldScale;
                    float mouseGridY = (yMouse - centerV) / oldScale;
                    float newCenterU = xMouse - mouseGridX * newScale;
                    float newCenterV = yMouse - mouseGridY * newScale;
                    category.x = (int) (newCenterU - w / 2.0f);
                    category.y = (int) (newCenterV - h / 2.0f);
                    category.setScale(newScale);
                }
                bo = true;
            }
            return bo;
        }
        return super.mouseScrolled(mouseX, mouseY, scrolled);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (!hasSubGui()) {
            if (category != null && mouseOnGrid && mouseButton == 0) {
                lmbIsSelect = true;
                lmbSelect[0] = pos[0];
                lmbSelect[1] = pos[1];
                return true;
            }
            if (hoverLeft) {
                mouseButtonEvent(leftTab.exit, 0);
                return true;
            } else if (hoverRight) {
                mouseButtonEvent(rightTab.exit, 0);
                return true;
            }
            boolean bo = false;
            for (IComponentGui component : new ArrayList<>(notScaledComponents)) {
                if (component.mouseClicked(mouseX, mouseY, mouseButton)) { bo = true; }
            }
            return bo || wrapper.mouseClicked(wrapper.mouseX, wrapper.mouseY, mouseButton);
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
        if (!hasSubGui()) {
            if (lmbIsSelect) { return true; }
            boolean bo = false;
            for (IComponentGui component : new ArrayList<>(notScaledComponents)) {
                if (component.mouseDragged(mouseX, mouseY, mouseButton, dx, dy)) { bo = true; }
            }
            if (!bo) { bo = wrapper.mouseDragged(xMouse, yMouse, mouseButton, dx, dy); }
            if (category != null && mouseOnGrid && !bo && mouseButton == 1) {
                tempDx += dx;
                tempDy += dy;
                int x = (int) (Math.floor(tempDx) * guiScale / 2.0d);
                int y = (int) (Math.floor(tempDy) * guiScale / 2.0d);
                if (x != 0 || y != 0) {
                    if (x != 0) {
                        category.x += x;
                        tempDx -= x / guiScale * 2.0d;
                        gridIsMoved = true;
                    }
                    if (y != 0) {
                        category.y += y;
                        tempDy -= y / guiScale * 2.0d;
                        gridIsMoved = true;
                    }
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (!hasSubGui()) {
            boolean bo = false;
            for (IComponentGui component : new ArrayList<>(notScaledComponents)) {
                if (component.mouseReleased(mouseX, mouseY, mouseButton)) { bo = true; }
            }
            if (category != null && mouseOnGrid && !bo && mouseButton == 1) {
                if (!gridIsMoved) { showExtraMenu(); }
                gridIsMoved = false;
            }
            return bo || wrapper.mouseReleased(xMouse, yMouse, mouseButton);
        }
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean keyPressed(char typedChar, int keyCode) {
        if (!hasSubGui() && !GuiBasic.isEscKey(keyCode)) {
            if (category != null) {
                if (hovered == null && isShiftKeyDown()) {
                    switch (keyCode) {
                        case Keyboard.KEY_A: addNewArea(); break;
                        case Keyboard.KEY_D: addNewDialog(); break;
                        case Keyboard.KEY_E: addNewOption(); break;
                        case Keyboard.KEY_Q: addNewQuest(); break;
                        case Keyboard.KEY_R: {
                            category.x = 0;
                            category.y = 0;
                            category.setScale(1.0f);
                            break;
                        } // reset category
                    }
                }
                if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_NUMPADCOMMA) {
                    if (selectLink != null) {
                        YDE_DATA.removeLink(selectLink);
                    }
                    else if (getSelect() instanceof YDEWindowNop) { removeNode((YDEWindowNop) getSelect()); }
                } // remove node
            } // hot keys
            for (IComponentGui component : new ArrayList<>(notScaledComponents)) {
                if (component.keyPressed(typedChar, keyCode)) { return true; }
            }
        }
        return super.keyPressed(typedChar, keyCode);
    }

    @Override
    public void onClose() {
        IComponentGui sel = getSelect();
        if (sel != null) { unFocused(sel); }
        super.onClose();
        NoppesUtil.requestOpenGUI(EnumGuiType.MainMenuGlobal);
    }

    @Override
    public void save() { YDEController.getInstance().save(); }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) {
//LogWriter.info("TEST: scroll: "+scroll.id);
        if (scroll.id == 0) {
            if (category != null && !category.category.equals(scroll.getSelected()) &&
                    categoryData.containsKey(scroll.getNormalSelected())) {
                category = YDE_DATA.getCategory(categoryData.get(scroll.getNormalSelected()).title);
                YDEController.getInstance().lastCategory = category == null ? "" : category.category;
                initGui();
            }
        } // select dialog category
        else if (category != null) {
            if (scroll.id == 3) {

            } // right dialog option
        }
    }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
        if (category != null) {
            if (scroll.id == 0) {
                setSubGui(new SubGuiEditText(1, category.category));
            } // rename dialog category
            if (scroll == helper) {
                helper.setIsVisible(false);
                switch (helper.getSelectedIndex()) {
                    case 0: addNewDialog(); break;
                    case 1: addNewOption(); break;
                    case 2: addNewArea(); break;
                    case 3: addNewQuest(); break;
                    case 5: {
                        category.x = 0;
                        category.y = 0;
                        category.setScale(1.0f);
                        break;
                    }
                    default: break;
                }
            } // ID 25
            else if (scroll.id == 3) {

            } // right dialog option
        }
    }

    @Override
    public void subGuiClosed(GuiScreen subgui) {
        if (category != null && subgui instanceof SubGuiEditText && !((SubGuiEditText) subgui).cancelled) {
            SubGuiEditText gui = (SubGuiEditText) subgui;
            if (gui.id == 0) {
                DialogCategory cat = new DialogCategory();
                StringBuilder t = new StringBuilder(gui.text[0]);
                boolean has = true;
                while (has) {
                    has = false;
                    for (DialogCategory c : DialogController.instance.categories.values()) {
                        if (category.categoryId != c.id && c.title.equalsIgnoreCase(t.toString())) {
                            has = true;
                            break;
                        }
                    }
                    if (has) { t.append("_"); }
                }
                cat.title = t.toString();
                category = YDE_DATA.getCategory(cat.title);
                Packets.sendServer(new SPacketDialogCategorySave(cat.save(new NBTTagCompound())));
            } // create dialog category
            if (gui.id == 1) {
                DialogCategory cat = DialogController.instance.getCategory(category.category);
                if (cat != null && !cat.title.equals(gui.text[0])) {
                    cat.title = gui.text[0];
                    StringBuilder t = new StringBuilder(gui.text[0]);
                    boolean has = true;
                    while (has) {
                        has = false;
                        for (DialogCategory c : DialogController.instance.categories.values()) {
                            if (category.id != c.id && c.title.equalsIgnoreCase(t.toString())) {
                                has = true;
                                break;
                            }
                        }
                        if (has) { t.append("_"); }
                    }
                    cat.title = t.toString();
                    category.category = cat.title;
                    Packets.sendServer(new SPacketDialogCategorySave(cat.save(new NBTTagCompound())));
                    initGui();
                }
            } // rename dialog category
        }
    }

    @Override
    public boolean doubleClicked(IComponentGui component) {
        if (component instanceof YDEWindowNop) {
            for (IComponentGui c : wrapper.components) {
                if (c instanceof YDEWindowNop) { ((YDEWindowNop) c).setIsFocused(false); }
            }
            CustomNPCsScheduler.runTack(() -> selectLinks((YDEWindowNop) component), 100);
            return true;
        }
        if (component instanceof GuiLabel && ((GuiLabel) component).id == 0) {
            GuiTextFieldNop textField = getTextField(0);
            if (textField != null) {
                ((GuiLabel) component).setIsVisible(false);
                textField.setIsVisible(true)
                        .setIsFocused(true);
                CustomNPCsScheduler.runTack(() ->
                                textField.mouseClicked(textField.getX() + 1, textField.getY() + 1, 0),
                        100);
            }
            return true;
        }
        return false;
    }

    @Override
    public void unFocused(GuiTextFieldNop textField) {
        if (category != null) {
            if (textField.id == 0) {
                if (!textField.getValue().isEmpty()) {
                    DialogCategory cat = DialogController.instance.getCategory(category.category);
                    if (cat != null && !cat.title.equals(textField.getValue())) {
                        cat.title = textField.getValue();
                        StringBuilder t = new StringBuilder(textField.getValue());
                        boolean has = true;
                        while (has) {
                            has = false;
                            for (DialogCategory c : DialogController.instance.categories.values()) {
                                if (cat.id != c.id && c.title.equalsIgnoreCase(t.toString())) {
                                    has = true;
                                    break;
                                }
                            }
                            if (has) { t.append("_"); }
                        }
                        cat.title = t.toString();
                        category.category = cat.title;
                        Packets.sendServer(new SPacketDialogCategorySave(cat.save(new NBTTagCompound())));
                        initGui();
                    }
                }
            }
        }
    }

    public boolean mouseButtonEvent(IComponentGui component, @Nullable GuiButtonNop button, int mouseButton) {
//LogWriter.info("TEST: buttonID: "+(button == null ? "null" : button.id)+"; mouseButton: "+mouseButton+"; component "+component);
        if (mouseButton == 0 && button != null) {
            switch (button.id) {
                case 0: {
                    if (component == null) {
                        setSubGui(new SubGuiEditText(0, Component.translatable("gui.new").getString()));
                        return true;
                    } // create dialog category
                    else if (component instanceof YDEScrollNop) {
                        setSelectNode(button.id);
                        return true;
                    }
                    break;
                }
                case 1: {
                    if (component == null) {
                        if (category != null) {
                            ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
                                if (agree) {
                                    int catId = category.categoryId;
                                    YDE_DATA.nodes.remove(category.id);
                                    category = null;
                                    leftTab.getScroll(0).setSelected("");
                                    Packets.sendServer(new SPacketDialogCategoryRemove(catId));
                                }
                                NoppesUtil.openGUI(player, this);
                            },
                                    category.getTitle().getParent(),
                                    Component.translatable("message.delete").getParent());
                            setScreen(guiYesNo);
                            return true;
                        } // remove dialog category
                    }
                    else if (component instanceof YDEScrollNop) {
                        setSelectNode(button.id);
                        return true;
                    }
                    break;
                }
                case 2: {
                    if (component == null) {
                        scrollDoubleClicked(leftTab.getScroll(0));
                        return true;
                    } // rename dialog category
                    else if (component instanceof YDEScrollNop) {
                        setSelectNode(button.id);
                        return true;
                    }
                    break;
                }
                case 3: {
                    if (component instanceof YDEScrollNop) {
                        return true;
                    }
                    break;
                }
                case 4: {
                    if (component instanceof YDEWindowNop && ((YDEWindowNop) component).node instanceof YDEOption) {
                        YDEOption yde_option = (YDEOption) ((YDEWindowNop) component).node;
                        setSubGui(new SubGuiColorSelector(yde_option.option.optionColor, new SubGuiColorSelector.ColorCallback() {
                            @Override
                            public void color(int colorIn) { SubGuiNpcDialogOption.LastColor = yde_option.option.optionColor = colorIn; }

                            @Override
                            public void preColor(int colorIn) {  }
                        }));
                        return true;
                    } // option color in node
                    else if (component instanceof YDEScrollNop) {
                        if (((YDEScrollNop) component).select instanceof YDEWindowNop) {
                            if (((YDEWindowNop) ((YDEScrollNop) component).select).node instanceof YDEOption) {
                                YDEOption yde_option = (YDEOption) ((YDEWindowNop) ((YDEScrollNop) component).select).node;
                                setSubGui(new SubGuiColorSelector(yde_option.option.optionColor, new SubGuiColorSelector.ColorCallback() {
                                    @Override
                                    public void color(int colorIn) { SubGuiNpcDialogOption.LastColor = yde_option.option.optionColor = colorIn; }

                                    @Override
                                    public void preColor(int colorIn) {  }
                                }));
                                return true;
                            } // option color
                        }
                    } // in right settings
                    break;
                }
                case 5: {
                    if (component instanceof YDEWindowNop && ((YDEWindowNop) component).node instanceof YDEOption) {
                        ((YDEOption) ((YDEWindowNop) component).node).option.optionType = OptionType.get(button.getValue());
                        initGui();
                        return true;
                    } // option type
                    else if (component instanceof YDEScrollNop) {
                        if (((YDEScrollNop) component).select instanceof YDEWindowNop) {
                            if (((YDEWindowNop) ((YDEScrollNop) component).select).node instanceof YDEOption) {
                                ((YDEOption) ((YDEWindowNop) ((YDEScrollNop) component).select).node).option.optionType = OptionType.get(button.getValue());
                                initGui();
                                return true;
                            }
                        }
                    }
                    break;
                }
                case 2500: {
                    if (component == null) {
                        if (button.equals(leftTab.exit)) { leftTab.isYDEShow = !leftTab.isYDEShow; }
                        else { rightTab.isYDEShow = !rightTab.isYDEShow; }
                        return true;
                    } // show / hide tabs
                    else if (component instanceof YDEWindowNop) {
                        removeNode((YDEWindowNop) component);
                        return true;
                    } // remove node / exit window
                    break;
                } // exit node / window
                case 2501: {
                    if (component instanceof YDEWindowNop) {
                        YDEWindowNop window = (YDEWindowNop) component;
                        window.isLock = !window.isLock;
                        if (window.isLock) {
                            button.txrX += button.txrW;
                            window.lock.layerColor = new Color(0xFFA0A000).getRGB();
                        }
                        else {
                            button.txrX -= button.txrW;
                            window.lock.layerColor = new Color(0xFFFFFF00).getRGB();
                        }
                        return true;
                    }
                    break;
                } // lock node / window
            }
        }
        return false;
    }

    private void removeNode(@Nonnull YDEWindowNop window) {
        Component mes;
        switch (window.node.type) {
            case CATEGORY: mes = Component.translatable("yde.delete.category", window.node.id, window.node.category); break;
            case DIALOG: mes = Component.translatable("yde.delete.dialog", ((YDEDialog) window.node).dialogId, ((YDEDialog) window.node).dialog.title); break;
            case NPC: mes = Component.translatable("yde.delete.npc", ((YDENpc) window.node).npcData.name, Component.translatable("menu.advanced").getString()); break;
            case OPTION: mes = Component.translatable("yde.delete.option", ((YDEOption) window.node).option.slot); break;
            case QUEST: mes = Component.translatable("yde.delete.quest", ((YDEQuest) window.node).questId); break;
            default: mes = Component.translatable("yde.delete.area", window.node.getTitle().getString()); break;
        }
        ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
            NoppesUtil.openGUI(player, this);
            if (agree) {
                GuiTextFieldNop.unfocus();
                GuiTextArea.unfocus();
                if (window.node instanceof YDEDialog) {
                    for (YDENode node : YDE_DATA.getToLinks(window.node.id)) {
                        List<YDENode> list = YDE_DATA.getFromLinks(node.id);
                        if (list.size() == 1 && list.get(0) == window.node) { YDE_DATA.nodes.remove(node.id); }
                    }
                }
                YDE_DATA.nodes.remove(window.node.id);
                if (category != null) {
                    if (window.node instanceof YDEDialog) { Packets.sendServer(new SPacketDialogRemove(((YDEDialog) window.node).dialogId)); }
                    else if (window.node instanceof YDEOption) {
                        noppes.npcs.controllers.data.Dialog dialog = DialogController.instance.get(((YDEOption) window.node).dialogId);
                        if (dialog != null) {
                            DialogOption option = dialog.options.get(((YDEOption) window.node).option.slot);
                            if (option != null) {
                                dialog.options.remove(option.slot);
                                if (dialog.id > -1) {
                                    Packets.sendServer(new SPacketDialogSave(category.categoryId, dialog.save(new NBTTagCompound())));
                                }
                            }
                        }
                    }
                }
                selects.remove(window.node.id);
            }
        },
                mes.getParent(),
                Component.translatable("message.delete").getParent());
        setScreen(guiYesNo);
    }

    private void renderSelectedBorder() {
        if (category == null || lmbIsSelect) { return; }
        int x;
        int y;
        int w;
        int h;
        boolean isSelect = true;
        for (int select : new ArrayList<>(selects)) {
            IComponentGui sel = get(select);
            if (sel instanceof YDEWindowNop) {
                x = sel.getX();
                y = sel.getY();
                w = ((YDEWindowNop) sel).imageWidth;
                h = ((YDEWindowNop) sel).imageHeight;
            }
            else if (sel instanceof YDEAreaNop) {
                x = sel.getX();
                y = sel.getY();
                w = sel.getWidth();
                h = sel.getHeight();
            }
            else { continue; }
            x = (int) (x * category.getScale() + centerU - 1.0f);
            y = (int) (y * category.getScale() + centerV - 1.0f);
            w = (int) (w * category.getScale() + 2.75f);
            h = (int) (h * category.getScale() + 1.75f);
            renderBorder(x, y, w, h, isSelect ? YDEController.selectLineColor : YDEController.hoverLineColor);
            if (isSelect) { isSelect = false; }
        }
    }

    private void renderBorder(int x, int y, int w, int h, int color) {
        if (category == null) { return; }
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        int s;
        int e;
        int step = 5;
        int u = (int) ((System.currentTimeMillis() % (step * 100L)) / (step * 10L)) - step;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        while (u < w) {
            s = x + ValueUtil.correctInt(u, 0, w);
            e = x + ValueUtil.correctInt(u + step, 0, w);
            buffer.pos(s, y, 0.0f).color(r, g, b, 1.0f).endVertex();
            buffer.pos(s, y + 0.5f, 0.0f).color(r, g, b, 1.0f).endVertex();
            buffer.pos(e, y + 0.5f, 0.0f).color(r, g, b, 1.0f).endVertex();
            buffer.pos(e, y, 0.0f).color(r, g, b, 1.0f).endVertex();
            u += 2 * step;
        }
        u = u - w - 2 * step;
        x += w;
        while (u < h) {
            s = y + ValueUtil.correctInt(u, 0, h);
            e = y + ValueUtil.correctInt(u + step, 0, h + 1);
            buffer.pos(x, s, 0.0f).color(r, g, b, 1.0f).endVertex();
            buffer.pos(x, e, 0.0f).color(r, g, b, 1.0f).endVertex();
            buffer.pos(x + 0.5f, e, 0.0f).color(r, g, b, 1.0f).endVertex();
            buffer.pos(x + 0.5f, s, 0.0f).color(r, g, b, 1.0f).endVertex();
            u += 2 * step;
        }
        u = -1 * (u - h) + step;
        y += h + 1;
        while (u > - w - step) {
            s = x + ValueUtil.correctInt(u, -w, 0);
            e = x + ValueUtil.correctInt(u + step, -w, 0);
            buffer.pos(s + 0.5f, y, 0.0f).color(r, g, b, 1.0f).endVertex();
            buffer.pos(s + 0.5f, y + 0.5f, 0.0f).color(r, g, b, 1.0f).endVertex();
            buffer.pos(e + 0.5f, y + 0.5f, 0.0f).color(r, g, b, 1.0f).endVertex();
            buffer.pos(e + 0.5f, y, 0.0f).color(r, g, b, 1.0f).endVertex();
            u -= 2 * step;
        }
        u += w + 2 * step;
        x -= w;
        while (u > - h - step) {
            s = y + ValueUtil.correctInt(u, - h, 0);
            e = y + ValueUtil.correctInt(u + step, -h, 0);
            buffer.pos(x, s, 0.0f).color(r, g, b, 1.0f).endVertex();
            buffer.pos(x, e, 0.0f).color(r, g, b, 1.0f).endVertex();
            buffer.pos(x + 0.5f, e, 0.0f).color(r, g, b, 1.0f).endVertex();
            buffer.pos(x + 0.5f, s, 0.0f).color(r, g, b, 1.0f).endVertex();
            u -= 2 * step;
        }
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public void movedSelectNodes(int addX, int addY) {
        for (int id : selects) {
            IComponentGui c = get(id);
            if (c instanceof YDEWindowNop) {
                ((YDEWindowNop) c).setIsFocused(true);
                c.moveTo(addX, addY);
            }
        }
    }

    private void addNewArea() {

    }

    private void addNewDialog() {
        if (category != null) {
            DialogCategory cat = DialogController.instance.getCategory(category.category);
            if (cat != null) {
                noppes.npcs.controllers.data.Dialog dialog = new noppes.npcs.controllers.data.Dialog(cat);
                StringBuilder t = new StringBuilder(Component.translatable("gui.new").getString());
                boolean has = true;
                while (has) {
                    has = false;
                    for (noppes.npcs.controllers.data.Dialog dia : dialog.category.dialogs.values()) {
                        if (dia.id != dialog.id && dia.title.equalsIgnoreCase(t.toString())) {
                            has = true;
                            break;
                        }
                    }
                    if (has) { t.append("_"); }
                }
                dialog.title = t.toString();
                Packets.sendServer(new SPacketDialogSave(category.categoryId, dialog.save(new NBTTagCompound())));

                IComponentGui component = getSelect();

                YDEDialog yde_dialog = YDE_DATA.createDialog(dialog);
                yde_dialog.x = (int) (Math.floor(pos[0] / 10.0f)) * 10;
                yde_dialog.y = (int) (Math.floor(pos[1] / 10.0f)) * 10;
                setActive(yde_dialog.id);
                if (component instanceof YDEWindowNop &&
                        ((YDEWindowNop) component).node instanceof YDEOption &&
                        ((YDEOption) ((YDEWindowNop) component).node).option.optionType == OptionType.DIALOG_OPTION) {
                    YDE_DATA.links.add(new YDELink(((YDEWindowNop) component).node.id, yde_dialog.id, EnumYDEType.OPTION));
                }
                initGui();
            }
        }
    }

    private void addNewOption() {
        if (category != null) {
            IComponentGui component = getSelect();
            DialogOption option = new DialogOption();
            YDEOption yde_option = null;
            if (component instanceof YDEWindowNop) {
                YDEDialog yde_dialog = null;
                if (((YDEWindowNop) component).node instanceof YDEDialog) { yde_dialog = (YDEDialog) ((YDEWindowNop) component).node; }
                else if (((YDEWindowNop) component).node instanceof YDEOption) {
                    List<YDENode> list = YDE_DATA.getToLinks(((YDEWindowNop) component).node.id);
                    if (list.size() == 1 && list.get(0) instanceof YDEDialog) { yde_dialog = (YDEDialog) list.get(0); }
                }
                if (yde_dialog != null) {
                    if (yde_dialog.dialog != null) {
                        option.slot = yde_dialog.dialog.options.size();
                        yde_dialog.dialog.options.put(option.slot, option);
                        option.optionColor = SubGuiNpcDialogOption.LastColor;
                        Packets.sendServer(new SPacketDialogSave(category.categoryId, yde_dialog.dialog.save(new NBTTagCompound())));
                    }
                    yde_option = YDE_DATA.createOption(category.category, option, yde_dialog.dialog);
                }
            }
            if (yde_option == null) { yde_option = YDE_DATA.createOption(category.category, option, null); }
            yde_option.x = (int) (Math.floor(pos[0] / 10.0f)) * 10;
            yde_option.y = (int) (Math.floor(pos[1] / 10.0f)) * 10;
            setActive(yde_option.id);
            initGui();
        }
    }

    private void addNewQuest() { }

    private void showExtraMenu() {
        if (category != null) {
            helper.type = 2;
            Component h0 = Component.translatable("yde.extra.add.dialog");
            Component h1 = Component.translatable("yde.extra.add.option");
            Component h2 = Component.translatable("yde.extra.add.quest");
            Component h3 = Component.translatable("yde.extra.add.area");

            Component h5 = Component.translatable("yde.extra.reset.grid");

            Component s0 = Component.literal("Shift").withStyle(TextFormatting.AQUA)
                    .append(Component.literal("+").withStyle(TextFormatting.WHITE)
                            .append(Component.literal("D").withStyle(TextFormatting.GOLD)));
            Component s1 = Component.literal("Shift").withStyle(TextFormatting.AQUA)
                    .append(Component.literal("+").withStyle(TextFormatting.WHITE)
                            .append(Component.literal("E").withStyle(TextFormatting.GOLD)));
            Component s2 = Component.literal("Shift").withStyle(TextFormatting.AQUA)
                    .append(Component.literal("+").withStyle(TextFormatting.WHITE)
                            .append(Component.literal("A").withStyle(TextFormatting.GOLD)));
            Component s3 = Component.literal("Shift").withStyle(TextFormatting.AQUA)
                    .append(Component.literal("+").withStyle(TextFormatting.WHITE)
                            .append(Component.literal("Q").withStyle(TextFormatting.GOLD)));

            Component s5 = Component.literal("Shift").withStyle(TextFormatting.AQUA)
                    .append(Component.literal("+").withStyle(TextFormatting.WHITE)
                            .append(Component.literal("R").withStyle(TextFormatting.GOLD)));

            int w0 = ValueUtil.max(UtilYDE.FONT.width(h0) + UtilYDE.FONT.width(s0) + space,
                    UtilYDE.FONT.width(h1) + UtilYDE.FONT.width(s1) + space,
                    UtilYDE.FONT.width(h2) + UtilYDE.FONT.width(s2) + space,
                    UtilYDE.FONT.width(h3) + UtilYDE.FONT.width(s3) + space,
                    UtilYDE.FONT.width(h5) + UtilYDE.FONT.width(s5) + space);
            StringBuilder ig = new StringBuilder("―");
            while (UtilYDE.FONT.width(ig.toString()) < w0) { ig.append("―"); }
            w0 = UtilYDE.FONT.width(ig.toString());
            Component ignore = Component.literal(ig.toString()).withStyle(TextFormatting.GRAY);
            int x = (int) (xMouse); // (int) (xMouse * 2.0f / guiScale);
            int y = (int) (yMouse) + 1; // (int) (yMouse * 2.0f / guiScale);

            if (x + w0 > (width * guiScale / 2.0f)) { x = (int) xMouse - w0 - 2; }

            LinkedHashMap<Integer, List<Component>> htm = new LinkedHashMap<>();
            Component posC = Component.translatable("yde.hover.extra.pos");
            List<Component> l0 = new ArrayList<>();
            Util.instance.putHovers(l0, Component.translatable("yde.hover.extra.add.dialog"), posC);
            htm.put(0, l0);
            List<Component> l1 = new ArrayList<>();
            Util.instance.putHovers(l1, Component.translatable("yde.hover.extra.add.option"), posC);
            htm.put(1, l1);
            List<Component> l2 = new ArrayList<>();
            Util.instance.putHovers(l2, Component.translatable("yde.hover.extra.add.area"), posC);
            htm.put(2, l2);
            List<Component> l3 = new ArrayList<>();
            Util.instance.putHovers(l3, Component.translatable("yde.hover.extra.add.quest"), posC);
            htm.put(3, l3);
            htm.put(4, new ArrayList<>());

            List<Component> l5 = new ArrayList<>();
            Util.instance.putHovers(l5, Component.translatable("yde.hover.extra.reset.grid"));
            htm.put(5, l5);

            List<Component> list = Lists.newArrayList(h0, h1, h2, h3, ignore, h5);
            helper.setPos(x, y)
                    .setSize(w0 + 2, (UtilYDE.FONT.getHeight() + 4) * list.size() + 4)
                    .setIsEnabled(true)
                    .setIsVisible(true)
                    .setUnsortedList(list)
                    .setSuffixes(Lists.newArrayList(s0, s1, s2, s3, Component.empty(), s5))
                    .setHoverTexts(htm)
                    .setIgnoreSelected(Lists.newArrayList(ignore))
                    .setSelect(-1);
        }
    }

    public void selectLinks(@Nullable YDEWindowNop window) {
        if (window != null) {
            window.setIsFocused(true);
            for (YDENode node : YDE_DATA.getFromLinks(window.id)) {
                YDEWindowNop nextWindow = get(node.id, YDEWindowNop.class);
                if (nextWindow != null) {
                    addSelect(window.id);
                    selectLinks(nextWindow);
                }
            }
        }
    }

    private IComponentGui getSelect() {
        for (int id : selects) {
            IComponentGui sel = get(id);
            if (sel instanceof YDEWindowNop || sel instanceof YDEAreaNop) { return sel; }
        }
        return null;
    }

    private List<YDEWindowNop> getAreaSelect(int x0, int y0, int x1, int y1) {
        int u0 = Math.min(x0, x1);
        int v0 = Math.min(y0, y1);
        int u1 = Math.max(x0, x1);
        int v1 = Math.max(y0, y1);
        List<YDEWindowNop> tempSelect = new ArrayList<>();
        for (IComponentGui component : wrapper.components) {
            if (component instanceof YDEWindowNop) {
                YDEWindowNop window = (YDEWindowNop) component;
                if (window.node.x < u1 && (window.node.x + window.node.width) > u0 &&
                        window.node.y < v1 && (window.node.y + window.node.height) > v0) {
                    tempSelect.add(window);
                }
            }
        }
        tempSelect.sort((w1, w2) -> {
            double d1 = distanceSqToRect(x1, y1, w1);
            double d2 = distanceSqToRect(x1, y1, w2);
            return Double.compare(d1, d2);
        });
        return tempSelect;
    }

    private double distanceSqToRect(int x, int y, YDEWindowNop window) {
        int closestX = clamp(x, window.node.x, window.node.x + window.node.width);
        int closestY = clamp(y, window.node.y, window.node.y + window.node.height);
        int dx = x - closestX;
        int dy = y - closestY;
        return (double) dx * dx + (double) dy * dy;
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    public void setActive(int id) {
        if (category != null) {
            IComponentGui sel = getSelect();
            if (sel != null && sel.getId() != id) { unFocused(sel); }
            selects.clear();
            YDEController.getInstance().lastNode.put(category.category, id);
            addSelect(id);
        }
    }

    public void addSelect(int id) {
        boolean setNewRightSelect = selects.isEmpty();
        selects.add(id);
        if (setNewRightSelect) { setSelectNode(0); }
    }

    private void setSelectNode(int tabId) {
        IComponentGui select = getSelect();
        rightTab.yde_scroll.initGui();
        if (category != null && select instanceof YDEWindowNop) {
            YDEWindowNop window = (YDEWindowNop) select;
            //LogWriter.info("TEST: t: "+tabId+"; w: "+window.id);
            int h0 = UtilYDE.FONT.getHeight() + 4;
            Component hover;
            if (window.node instanceof YDEDialog) {
                rightTab.yde_scroll.availability = ((YDEDialog) window.node).dialog.availability;
                rightTab.yde_scroll.addTopButton(0, 0, 0, "dialog.dialog")
                        .setCustomFont(UtilYDE.FONT)
                        .setTexture(YDE_BUTTONS)
                        .setDefBack(false)
                        .setIsAnim(true)
                        .setUV(0, 80, 200, 20)
                        .setColor(YDEController.textColor)
                        .setSize(50, h0)
                        .setIsFocused(tabId == 0);
                rightTab.yde_scroll.addTopButton(1, 0, 0, "availability.available")
                        .setIsEnabled(rightTab.yde_scroll.availability != null)
                        .setCustomFont(UtilYDE.FONT)
                        .setTexture(YDE_BUTTONS)
                        .setDefBack(false)
                        .setIsAnim(true)
                        .setUV(0, 80, 200, 20)
                        .setColor(YDEController.textColor)
                        .setSize(50, h0)
                        .setIsFocused(tabId == 1);
                rightTab.yde_scroll.addTopButton(2, 0, 0, "mailbox.write")
                        .setCustomFont(UtilYDE.FONT)
                        .setTexture(YDE_BUTTONS)
                        .setDefBack(false)
                        .setIsAnim(true)
                        .setUV(0, 80, 200, 20)
                        .setColor(YDEController.textColor)
                        .setSize(50, h0)
                        .setIsFocused(tabId == 2);
                int lId = 0;
                if (tabId == 0) {} // main
                if (tabId == 1) {

                    setRightTabAvailability(lId);
                } // availability
                else { } // mail
            }
            if (window.node instanceof YDEOption) {
                YDEOption yde_option = (YDEOption) window.node;
                if (tabId != 1) { rightTab.yde_scroll.availability = null; }
                rightTab.yde_scroll.addTopButton(0, 0, 0, "gui.answer")
                        .setCustomFont(UtilYDE.FONT)
                        .setTexture(YDE_BUTTONS)
                        .setDefBack(false)
                        .setIsAnim(true)
                        .setUV(0, 80, 200, 20)
                        .setColor(YDEController.textColor)
                        .setSize(50, h0)
                        .setIsFocused(tabId == 0);
                rightTab.yde_scroll.addTopButton(1, 0, 0, "availability.available")
                        .setIsEnabled(rightTab.yde_scroll.availability != null)
                        .setCustomFont(UtilYDE.FONT)
                        .setTexture(YDE_BUTTONS)
                        .setDefBack(false)
                        .setIsAnim(true)
                        .setUV(0, 80, 200, 20)
                        .setColor(YDEController.textColor)
                        .setSize(50, h0)
                        .setIsFocused(tabId == 1);
                int lId = 0;
                int lH = UtilYDE.FONT.getHeight() + 2;
                int w = rightTab.yde_scroll.width - 12;
                int y = 0;
                if (tabId == 0) {
                    // title
                    rightTab.yde_scroll.addLabel(lId++, 1, y, Component.translatable("dialog.dialog")
                                    .append(" ID:" + yde_option.dialogId + "; ").append(yde_option.getTitle()).append(":"))
                            .setCustomFont(UtilYDE.FONT)
                            .setColor(YDEController.textColor)
                            .setSize(w - 2, lH);
                    // name
                    rightTab.yde_scroll.addLabel(lId++, 1, y += lH, Component.translatable("gui.text").append(":"))
                            .setCustomFont(UtilYDE.FONT)
                            .setColor(YDEController.textColor)
                            .setSize(w - 2, lH);
                    hover = Component.translatable("dialog.option.hover.name");
                    Component c = Component.translatable(yde_option.option.title);
                    if (!Util.instance.equalsDeleteColor(yde_option.option.title, c.getString(), false)) {
                        hover.append("<br>").append(c);
                    }
                    rightTab.yde_scroll.addTextField(0, 1, y += lH, w, lH, yde_option.option.title)
                            .setCustomFont(UtilYDE.FONT)
                            .setColor(YDEController.textColor)
                            .setHoverTexts(hover);
                    // color
                    StringBuilder color = new StringBuilder(Integer.toHexString(yde_option.option.optionColor));
                    while (color.length() < 6) { color.insert(0, 0); }
                    rightTab.yde_scroll.addLabel(lId++, 1, y += lH + 2, Component.translatable("gui.color").append(":"))
                            .setCustomFont(UtilYDE.FONT)
                            .setColor(YDEController.textColor)
                            .setSize(w - 2, lH);
                    rightTab.yde_scroll.addButton(4, 0, y += lH, color)
                            .setSize(w / 3 * 2, lH)
                            .setTexture(YDE_BUTTONS)
                            .setDefBack(false)
                            .setIsAnim(true)
                            .setUV(0, 0, 200, 20)
                            .setCustomFont(UtilYDE.FONT)
                            .setColor(yde_option.option.optionColor)
                            .setHoverTexts("color.hover");
                    // type
                    rightTab.yde_scroll.addLabel(lId++, 1, y += lH + 2, Component.translatable("gui.type").append(":"))
                            .setCustomFont(UtilYDE.FONT)
                            .setColor(YDEController.textColor)
                            .setSize(w - 2, lH);
                    rightTab.yde_scroll.addButton(5, 0, y += lH, false, yde_option.option.optionType.get(), SubGuiNpcDialogOption.options)
                            .setSize(w / 3 * 2, lH)
                            .setTexture(YDE_BUTTONS)
                            .setDefBack(false)
                            .setIsAnim(true)
                            .setUV(0, 0, 200, 20)
                            .setCustomFont(UtilYDE.FONT)
                            .setHoverTexts("dialog.option.hover.type." + yde_option.option.optionType.get());
                    // icon
                    List<Object> list = new ArrayList<>();
                    list.add("");
                    for (ResourceLocation res : GuiDialogInteract.icons.values()) {
                        list.add(res.getResourcePath().substring(res.getResourcePath().lastIndexOf("/") + 1, res.getResourcePath().lastIndexOf(".")));
                    }
                    rightTab.yde_scroll.addLabel(lId++, 1, y += lH + 2, Component.translatable("dialog.icon").append(":"))
                            .setCustomFont(UtilYDE.FONT)
                            .setColor(YDEController.textColor)
                            .setSize(w - 2, lH);
                    rightTab.yde_scroll.addButton(3, 5, y += lH, false, yde_option.option.iconId, list.toArray(new Object[0]))
                            .setSize(lH * 2, lH * 2)
                            .setTexture(GuiDialogInteract.icons.get(yde_option.option.iconId))
                            .setDefBack(true)
                            .setUV(0, 0, 256, 256)
                            .setHoverTexts("dialog.option.hover.icon");
                    // options
                    if (yde_option.option.optionType == OptionType.DIALOG_OPTION) {
                        rightTab.yde_scroll.add(new GuiLineNop(this, lId++,
                                rightTab.yde_scroll.guiLeft + 1, rightTab.yde_scroll.guiTop + (y += lH * 2 + 2),
                                w, 0.5f, false,
                                YDEController.componentLineColor & 0xFFFFFF | 0xC0000000)
                                .setScale(0.5f));
                        rightTab.yde_scroll.addLabel(lId, 1, y += 2, Component.translatable("gui.options").append(":"))
                                .setCustomFont(UtilYDE.FONT)
                                .setColor(YDEController.textColor)
                                .setSize(w - 2, lH);

                        DialogController dData = DialogController.instance;
                        List<Component> keys = new ArrayList<>();
                        int pos = -1;
                        int i = 0;
                        DialogOption.OptionDialogID del = null;
                        for (DialogOption.OptionDialogID od : yde_option.option.dialogs) {
                            if (od.dialogId <= 0) { del = od; }
                            Component key;
                            noppes.npcs.controllers.data.Dialog d = dData.get(od.dialogId);
                            if (d == null) {
                                key = Component.empty()
                                        .append(Component.literal("ID: " + od.dialogId).withStyle(TextFormatting.GRAY))
                                        .append(Component.literal(" Dialog Not Found!").withStyle(TextFormatting.RED));
                            }
                            else { key = d.getKey(); }
                            keys.add(key);
                            if (key.getString().equals(rightTab.yde_scroll.scrollSelect.getString())) { pos = i; }
                            i++;
                        }
                        if (del != null) { yde_option.option.dialogs.remove(del); }
                        GuiCustomScrollNop scroll = rightTab.yde_scroll.addScroll(3)
                                .disabledSearch()
                                .setSize(129, 100)
                                .setPos(rightTab.yde_scroll.guiLeft + 1, rightTab.yde_scroll.guiTop + (y += lH))
                                .setHoverScale(guiScale, 1.0f)
                                .setUnsortedList(keys)
                                .setCustomFont(UtilYDE.FONT);
                        if (!rightTab.yde_scroll.scrollSelect.getString().isEmpty()) { scroll.setSelected(rightTab.yde_scroll.scrollSelect); }
                        // up
                        int x = scroll.width + 2;
                        int wB = scroll.height / 2;
                        rightTab.yde_scroll.addButton(9, x, y, "↑")
                                .setSize(lH, wB)
                                .setTexture(YDE_VERT_BUTTONS)
                                .setDefBack(false)
                                .setIsAnim(true)
                                .setUV(0, 0, 20, 64)
                                .setCustomFont(UtilYDE.FONT)
                                .setIsEnabled(scroll.hasSelected() && pos != 0)
                                .setHoverTexts("dialog.option.hover.up");
                        // down
                        rightTab.yde_scroll.addButton(10, x, y + wB, "↓")
                                .setSize(lH, scroll.height - wB)
                                .setTexture(YDE_VERT_BUTTONS)
                                .setDefBack(false)
                                .setIsAnim(true)
                                .setUV(0, 0, 20, 64)
                                .setCustomFont(UtilYDE.FONT)
                                .setIsEnabled(scroll.hasSelected() && pos > -1 && pos < keys.size() - 1)
                                .setHoverTexts("dialog.option.hover.down");
                        // add
                        Component addC = Component.translatable("gui.add");
                        Component removeC = Component.translatable("gui.remove");
                        Component editC = Component.translatable("gui.edit");
                        int tB = UtilYDE.FONT.width(addC) + UtilYDE.FONT.width(removeC) + UtilYDE.FONT.width(editC);
                        x = 0;
                        wB = (int) ((float) UtilYDE.FONT.width(addC) / (float) tB * 129.0f);
                        rightTab.yde_scroll.addButton(6, 0, y += scroll.height + 1, "gui.add")
                                .setSize(wB, lH)
                                .setTexture(YDE_BUTTONS)
                                .setDefBack(false)
                                .setIsAnim(true)
                                .setUV(0, 0, 200, 20)
                                .setCustomFont(UtilYDE.FONT)
                                .setHoverTexts("dialog.option.hover.add");
                        // remove
                        x += wB;
                        wB = (int) ((float) UtilYDE.FONT.width(removeC) / tB * 129.0f);
                        rightTab.yde_scroll.addButton(7, x, y, "gui.remove")
                                .setSize(wB, lH)
                                .setTexture(YDE_BUTTONS)
                                .setDefBack(false)
                                .setIsAnim(true)
                                .setUV(0, 0, 200, 20)
                                .setCustomFont(UtilYDE.FONT)
                                .setIsEnabled(scroll.hasSelected())
                                .setHoverTexts("dialog.option.hover.del");
                        //  edit
                        x += wB;
                        rightTab.yde_scroll.addButton(8, x, y, "gui.edit")
                                .setSize(tB - wB, lH)
                                .setTexture(YDE_BUTTONS)
                                .setDefBack(false)
                                .setIsAnim(true)
                                .setUV(0, 0, 200, 20)
                                .setCustomFont(UtilYDE.FONT)
                                .setIsEnabled(scroll.hasSelected())
                                .setHoverTexts("dialog.option.hover.edit");
                        rightTab.yde_scroll.add(new GuiLineNop(this, lId,
                                rightTab.yde_scroll.guiLeft + 1, rightTab.yde_scroll.guiTop + (y += lH + 2),
                                w, 0.5f, false,
                                YDEController.componentLineColor & 0xFFFFFF | 0xC0000000)
                                .setScale(0.5f));
                        rightTab.yde_scroll.addButton(11, 0, y + 2, "availability.available")
                                .setSize(w / 3 * 2, lH)
                                .setTexture(YDE_BUTTONS)
                                .setDefBack(false)
                                .setIsAnim(true)
                                .setUV(0, 0, 200, 20)
                                .setCustomFont(UtilYDE.FONT)
                                .setIsEnabled(scroll.hasSelected())
                                .setHoverTexts(Component.translatable("dialog.option.hover.availability",
                                        rightTab.yde_scroll.scrollSelect.getFormattedText()));
                        rightTab.yde_scroll.getTopButton(1).setIsEnabled(scroll.hasSelected());
                    }
                    else if (yde_option.option.optionType == OptionType.COMMAND_BLOCK) {
                        rightTab.yde_scroll.add(new GuiLineNop(this, lId++,
                                rightTab.yde_scroll.guiLeft + 1, rightTab.yde_scroll.guiTop + (y += lH * 2 + 2),
                                w, 0.5f, false,
                                YDEController.componentLineColor & 0xFFFFFF | 0xC0000000)
                                .setScale(0.5f));
                        rightTab.yde_scroll.addLabel(lId, 1, y += 2, Component.translatable(Blocks.COMMAND_BLOCK.getUnlocalizedName() + ".name").append(":"))
                                .setCustomFont(UtilYDE.FONT)
                                .setColor(YDEController.textColor)
                                .setSize(w - 2, lH);
                        rightTab.yde_scroll.addTextField(1, 1, y + lH, w, lH, yde_option.option.command)
                                .setCustomFont(UtilYDE.FONT)
                                .setColor(YDEController.textColor)
                                .setMaxStringLength(Short.MAX_VALUE)
                                .setHoverTexts(Component.translatable("command.hover.text", Short.MAX_VALUE));
                    }
                } // main
                else {
                    // title
                    rightTab.yde_scroll.addLabel(lId, 1, y, Component.translatable("dialog.dialog")
                                    .append(" ID:" + yde_option.dialogId + "; ").append(yde_option.getTitle()).append(":"))
                            .setCustomFont(UtilYDE.FONT)
                            .setColor(YDEController.textColor)
                            .setSize(w - 2, lH);

                    setRightTabAvailability(lId);
                } // availability
            }
        }
        rightTab.yde_scroll.reset();
        rightTab.yde_scroll.resetRoll();
        rightTab.yde_scroll.select = select;
        rightTab.yde_scroll.tabId = tabId;
    }

    private void setRightTabAvailability(int lId) {}

    public void removeSelect(int id) { selects.remove(id); }

    public boolean hasSelect(int id) { return selects.contains(id); }

    public void unFocused(IComponentGui component) {
        DialogController dData = DialogController.instance;
        if (component instanceof YDEWindowNop) {
            Dialog dialog = null;
            if (((YDEWindowNop) component).node instanceof YDEDialog && dData.hasDialog(((YDEDialog) ((YDEWindowNop) component).node).dialog.id)) {
                dialog = ((YDEDialog) ((YDEWindowNop) component).node).dialog;
            }
            if (((YDEWindowNop) component).node instanceof YDEOption && dData.hasDialog(((YDEOption) ((YDEWindowNop) component).node).dialogId)) {
                dialog = dData.get(((YDEOption) ((YDEWindowNop) component).node).dialogId);
            }
            if (dialog != null) { Packets.sendServer(new SPacketDialogSave(dialog.category.id, dialog.save(new NBTTagCompound()))); }
        }
    }

    public void unFocused(IComponentGui component, GuiTextFieldNop textField) {
        if (!textField.getValue().isEmpty()) {
            if (component instanceof YDEWindowNop) {
                if (((YDEWindowNop) component).node instanceof YDEDialog) {
                    ((YDEDialog) ((YDEWindowNop) component).node).dialog.title = textField.getValue();
                } // name
                if (((YDEWindowNop) component).node instanceof YDEOption) {
                    YDEOption yde_option = (YDEOption) ((YDEWindowNop) component).node;
                    switch (textField.id) {
                        case 0: yde_option.option.title = textField.getValue(); break; // name
                        case 1: {
                            if (yde_option.option.dialogs.size() == 1) {
                                yde_option.option.dialogs.get(0).dialogId = textField.getInteger();
                            }
                            break;
                        } // next dialog ID
                        case 2: yde_option.option.command = textField.getValue(); break; // command
                    }
                }
            }
        }
    }

    public void textUpdate(IComponentGui component, IComponentGui textEditor, String text) {
        if (!text.isEmpty()) {
            if (component instanceof YDEWindowNop) {
                if (((YDEWindowNop) component).node instanceof YDEDialog) {
                    if (textEditor instanceof GuiTextFieldNop) { ((YDEDialog) ((YDEWindowNop) component).node).dialog.title = text; } // name
                    if (textEditor instanceof GuiTextArea) { ((YDEDialog) ((YDEWindowNop) component).node).dialog.text = text; } // text
                }
                if (((YDEWindowNop) component).node instanceof YDEOption) { ((YDEOption) ((YDEWindowNop) component).node).option.title = text; } // name
            }
        }
    }

}