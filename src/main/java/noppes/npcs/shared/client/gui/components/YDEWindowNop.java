package noppes.npcs.shared.client.gui.components;

import net.minecraft.init.Blocks;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.controllers.YDEController;
import noppes.npcs.client.gui.global.SubGuiNpcDialogOption;
import noppes.npcs.client.gui.yellow_de.GuiYellowDialogEditor;
import noppes.npcs.client.gui.yellow_de.data.*;
import noppes.npcs.client.gui.yellow_de.data.nodes.YDEDialog;
import noppes.npcs.client.gui.yellow_de.data.nodes.YDEOption;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import org.lwjgl.opengl.GL11;

public class YDEWindowNop extends GuiCustomWindowNop {

    protected static final ClientProxy.FontContainer AREA_FONT = new ClientProxy.FontContainer("JetBrainsMono", 12);

    public final YDENode node;
    public final GuiYellowDialogEditor listener;
    protected GuiButtonNop b0;
    protected GuiButtonNop b1;
    protected GuiButtonNop b2;
    protected GuiButtonNop b3;
    protected boolean isMoved;
    protected double tempDx;
    protected double tempDy;
    protected long lastClicked = 0L;

    public YDEWindowNop(GuiYellowDialogEditor gui, YDENode nodeIn) {
        super(gui, nodeIn.id, nodeIn.x, nodeIn.y, nodeIn.width, nodeIn.height, nodeIn.getTitle());
        node = nodeIn;
        listener = gui;

        exit.setX(exit.getX() - 2);
        exit.setY(exit.getY() - 1);
        exit.setCustomFont(UtilYDE.FONT)
                .setColor(YDEController.textColor)
                .setHoverTexts("yde.node.exit");
        exit.isScissor = false;

        lock.setX(lock.getX() - 2);
        lock.setY(lock.getY() - 1);
        lock.setCustomFont(UtilYDE.FONT)
                .setColor(YDEController.textColor)
                .setHoverTexts("yde.node.lock");
        lock.isScissor = false;

        hoverFont = UtilYDE.FONT;

        initGui();
    }

    @Override
    public void initGui() {
        super.initGui();
        add(exit);
        add(lock);
        int w = imageWidth - 6;
        int h0 = UtilYDE.FONT.getHeight() + 2;
        int y = h0;
        if (node instanceof YDEDialog) {
            YDEDialog yde_dialog = (YDEDialog) node;
            if (yde_dialog.dialog == null) { yde_dialog.dialog = new Dialog(DialogController.instance.getCategory(node.category)); }
            addTextField(0, 3, 21, w, y, yde_dialog.dialog.title)
                    .setColor(YDEController.textColor)
                    .setCustomFont(UtilYDE.FONT);
            GuiTextArea compArea;
            add(compArea = new GuiTextArea(0, guiLeft + 4, guiTop + (y += 32), w - 2, imageHeight - y - 4, yde_dialog.dialog.text)
                    .setColor(YDEController.textColor)
                    .setCustomFont(AREA_FONT));
            compArea.isYDE = true;
            // link buttons
            // -> options
            b0 = addButton(0, imageWidth - 4, imageHeight / 2 - 4, "")
                    .setSize(7, 7)
                    .setTexture(INFO)
                    .setDefBack(false)
                    .setIsAnim(true)
                    .setUV(0, 18, 14, 14)
                    .setHoverTexts("yde.hover.node.dialog.next.option");
            // <- back to options
            b1 = addButton(1, -4, imageHeight / 2 - 4, "")
                    .setSize(7, 7)
                    .setTexture(INFO)
                    .setDefBack(false)
                    .setIsAnim(true)
                    .setUV(0, 18, 14, 14)
                    .setHoverTexts("yde.hover.node.dialog.back.option");
            // -> npc
            b2 = addButton(2, -4, (int) (imageHeight * 0.8f - 4.0f), "")
                    .setSize(7, 7)
                    .setTexture(INFO)
                    .setDefBack(false)
                    .setIsAnim(true)
                    .setUV(0, 18, 14, 14)
                    .setHoverTexts("yde.hover.node.dialog.npc");
            // -> quest
            b3 = addButton(3, imageWidth / 2 - 4, imageHeight - 4, "")
                    .setSize(7, 7)
                    .setTexture(INFO)
                    .setDefBack(false)
                    .setIsAnim(true)
                    .setUV(0, 18, 14, 14)
                    .setHoverTexts("yde.hover.node.dialog.quest");
        }
        else if (node instanceof YDEOption) {
            YDEOption yde_option = (YDEOption) node;
            // name
            addTextField(0, 3, y += 10, w, h0, yde_option.option.title)
                    .setColor(YDEController.textColor)
                    .setCustomFont(UtilYDE.FONT)
                    .setHoverTexts("dialog.option.hover.name");
            // color
            StringBuilder color = new StringBuilder(Integer.toHexString(yde_option.option.optionColor));
            while (color.length() < 6) { color.insert(0, 0); }
            int wB = (imageWidth - 8) / 3;
            addButton(4, 2, y += h0 * 2, color.toString())
                    .setSize(wB, 12)
                    .setTexture(YDE_BUTTONS)
                    .setDefBack(false)
                    .setIsAnim(true)
                    .setUV(0, 0, 200, 20)
                    .setCustomFont(UtilYDE.FONT)
                    .setColor(yde_option.option.optionColor)
                    .setHoverTexts("color.hover");
            // type
            addButton(5, 4 + wB, y, false, yde_option.option.optionType.get(), SubGuiNpcDialogOption.options)
                    .setSize(wB, 12)
                    .setTexture(YDE_BUTTONS)
                    .setDefBack(false)
                    .setIsAnim(true)
                    .setUV(0, 0, 200, 20)
                    .setCustomFont(UtilYDE.FONT)
                    .setHoverTexts("dialog.option.hover.type." + yde_option.option.optionType.get());
            switch (yde_option.option.optionType) {
                case DIALOG_OPTION: {
                    if (yde_option.option.dialogs.size() == 1) {
                        DialogOption.OptionDialogID subOption = yde_option.option.dialogs.get(0);
                        addTextField(1, 3, y + h0 * 2, wB, h0, subOption.dialogId)
                                .setColor(YDEController.textColor)
                                .setMinMaxDefault(0, Integer.MAX_VALUE, subOption.dialogId)
                                .setCustomFont(UtilYDE.FONT)
                                .setHoverTexts("dialog.option.hover.dialog");
                    }
                    if (yde_option.option.dialogs.size() > 1) {
                        StringBuilder ids = new StringBuilder();
                        for (DialogOption.OptionDialogID subOption : yde_option.option.dialogs) {
                            if (ids.length() > 0) { ids.append(", "); }
                            ids.append(subOption.dialogId);
                        }
                        addLabel(1, 5 + wB, y + h0 * 2, ids.toString())
                                .setSize(w, 10)
                                .setCustomFont(UtilYDE.FONT)
                                .setHoverTexts("dialog.option.hover.dialogs");
                    }
                    break;
                }
                case COMMAND_BLOCK: {
                    addTextField(2, 3, y + h0 * 2, w, h0, yde_option.option.command)
                            .setColor(YDEController.textColor)
                            .setCustomFont(UtilYDE.FONT)
                            .setHoverTexts("dialog.option.hover.command");
                    break;
                }
            }
            // -> next dialog
            b0 = addButton(0, imageWidth - 4, imageHeight / 2 - 4, "")
                    .setSize(7, 7)
                    .setTexture(INFO)
                    .setDefBack(false)
                    .setIsAnim(true)
                    .setUV(0, 18, 14, 14)
                    .setHoverTexts("yde.hover.node.option.next.dialog");
            // <- back to dialog
            b1 = addButton(1, -4, imageHeight / 2 - 4, "")
                    .setSize(7, 7)
                    .setTexture(INFO)
                    .setDefBack(false)
                    .setIsAnim(true)
                    .setUV(0, 18, 14, 14)
                    .setHoverTexts("yde.hover.node.option.back.dialog");
        }
        else if (node.type == EnumYDEType.QUEST) {
            // <- back to dialog
            b0 = addButton(0, imageWidth / 2 - 4, -4, "")
                    .setSize(7, 7)
                    .setTexture(INFO)
                    .setDefBack(false)
                    .setIsAnim(true)
                    .setUV(0, 18, 14, 14)
                    .setHoverTexts("yde.hover.node.quest.back.dialog");
        }
        else if (node.type == EnumYDEType.NPC) {
            // <- back to dialog
            b0 = addButton(0, imageWidth - 4,  imageHeight / 2 - 4, "")
                    .setSize(7, 7)
                    .setTexture(INFO)
                    .setDefBack(false)
                    .setIsAnim(true)
                    .setUV(0, 18, 14, 14)
                    .setHoverTexts("yde.hover.node.npc.back.dialog");
        }
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        if (isHovered && visible) { listener.mouseButtonEvent(this, button, 0); }
    }

    @Override
    public boolean mouseButtonEvent(GuiButtonNop button, int mouseButton) {
        if (isHovered && visible) { return listener.mouseButtonEvent(this, button, mouseButton); }
        return false;
    }

    public void drawDefaultBackground() {
        GlStateManager.pushMatrix();
        GlStateManager.translate(guiLeft, guiTop, -1.0f);
        GlStateManager.pushMatrix();
        // background
        GlStateManager.scale(0.5f * bgScale, 0.5f * bgScale, 0.5f * bgScale);
        int w = imageWidth * 2;
        int h = imageHeight * 2;
        int color = isEnabled() && (isHovered || focused) ? YDEController.backHoverColor : YDEController.backColor;
        drawRect(0, 0, w, h, color);
        color = isEnabled() && (isHovered || isFocused()) ? YDEController.backColor : YDEController.windowLineColor;
        drawHorizontalLine(1, w - 2, 1, color);
        drawVerticalLine(1, 1, h - 2, color);
        drawVerticalLine(w - 2, 1, h - 2, color);
        drawHorizontalLine(1, w - 2, h - 2, color);
        // dots
        if (node.type == EnumYDEType.DIALOG) {
            b0.layerColor = !isEnabled() ? EnumYDEType.DIALOG.disableColor : isHovered || focused ? EnumYDEType.DIALOG.hoverColor : EnumYDEType.DIALOG.color;
            b1.layerColor = !isEnabled() ? EnumYDEType.OPTION.disableColor : isHovered || focused ? EnumYDEType.OPTION.hoverColor : EnumYDEType.OPTION.color;
            b2.layerColor = !isEnabled() ? EnumYDEType.NPC.disableColor : isHovered || focused ? EnumYDEType.NPC.hoverColor : EnumYDEType.NPC.color;
            b3.layerColor = !isEnabled() ? EnumYDEType.QUEST.disableColor : isHovered || focused ? EnumYDEType.QUEST.hoverColor : EnumYDEType.QUEST.color;
        }
        else if (node.type == EnumYDEType.OPTION) {
            b0.layerColor = !isEnabled() ? EnumYDEType.OPTION.disableColor : isHovered || focused ? EnumYDEType.OPTION.hoverColor : EnumYDEType.OPTION.color;
            b1.layerColor = !isEnabled() ? EnumYDEType.DIALOG.disableColor : isHovered || focused ? EnumYDEType.DIALOG.hoverColor : EnumYDEType.DIALOG.color;
        }
        else if (node.type == EnumYDEType.QUEST) {
            b0.layerColor = !isEnabled() ? EnumYDEType.QUEST.disableColor : isHovered || focused ? EnumYDEType.QUEST.hoverColor : EnumYDEType.QUEST.color;
        }
        else if (node.type == EnumYDEType.NPC) {
            b0.layerColor = !isEnabled() ? EnumYDEType.NPC.disableColor : isHovered || focused ? EnumYDEType.NPC.hoverColor : EnumYDEType.NPC.color;
        }
        // head
        color = !isEnabled() ? node.type.disableColor : isHovered || focused ? node.type.hoverColor : node.type.color;
        GlStateManager.translate(3.0f, 3.0f, 0.0f);
        float r = (color >> 16) / 255.0f;
        float g = (color >> 8) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float right = w - 6;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(6.0f, 0.0f, 0.0f).color(r, g, b, 1.0f).endVertex();
        buffer.pos(0.0f, 6.0f, 0.0f).color(r, g, b, 0.75f).endVertex();
        buffer.pos(right, 6.0f, 0.0f).color(r, g, b, 0.75f).endVertex();
        buffer.pos(right - 6.0f, 0.0f, 0.0f).color(r, g, b, 1.0f).endVertex();
        buffer.pos(0.0f, 6.0f, 0.0f).color(r, g, b, 0.75f).endVertex();
        buffer.pos(0.0f, 19.0f, 0.0f).color(r, g, b, 0.25f).endVertex();
        buffer.pos(right, 19.0f, 0.0f).color(r, g, b, 0.25f).endVertex();
        buffer.pos(right, 6.0f, 0.0f).color(r, g, b, 0.75f).endVertex();

        tessellator.draw();

        GlStateManager.popMatrix();
        // title
        if (title != null && !title.getString().isEmpty()) { UtilYDE.FONT.draw(title, 3, 2, YDEController.textColor); }
        int h0 = UtilYDE.FONT.getHeight() + 2;
        float y = 11.5f;
        if (node.type == EnumYDEType.DIALOG) {
            // name
            UtilYDE.FONT.draw(Component.translatable("gui.name").append(":"), 3, y, YDEController.textColor);
            // text
            UtilYDE.FONT.draw(Component.translatable("gui.text").append(":"), 3, y + h0 * 2, YDEController.textColor);
        }
        else if (node instanceof YDEOption) {
            YDEOption yde_option = (YDEOption) node;
            // name
            UtilYDE.FONT.draw(Component.translatable("gui.answer").append(":"), 3, y, YDEController.textColor);
            // color
            UtilYDE.FONT.draw(Component.translatable("gui.color").append(":"), 3, y += h0 * 2, YDEController.textColor);
            int wB = (imageWidth - 8) / 3;
            // type
            UtilYDE.FONT.draw(Component.translatable("gui.type").append(":"), 5 + wB, y, YDEController.textColor);
            // extra
            y += h0 * 2;
            switch (yde_option.option.optionType) {
                case DIALOG_OPTION: {
                    if (!yde_option.option.dialogs.isEmpty()) {
                        UtilYDE.FONT.draw(Component.literal("ID" + (yde_option.option.dialogs.size() > 1 ? "s" : "") + ":"), 3, y, YDEController.textColor);
                    }
                    break;
                }
                case COMMAND_BLOCK: {
                    UtilYDE.FONT.draw(Component.translatable(Blocks.COMMAND_BLOCK.getUnlocalizedName() + ".name").append(":"), 3, y, YDEController.textColor);
                    break;
                }
            }
        }
        GlStateManager.translate(-2, -2, 0.0f);
        GlStateManager.popMatrix();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0f, 0.0f, (float) id / 10000.0f);
        super.render(mouseX, mouseY, partialTicks);
        isHeadHovered = isHovered && isMouseHover(mouseX, mouseY, getX(), guiTop, imageWidth, 11);
        if (isHovered) { listener.hovered = this; }
        GlStateManager.popMatrix();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        boolean bo = super.mouseClicked(mouseX, mouseY, mouseButton);
        if (isHovered && !bo) {
            bo = listener.mouseButtonEvent(this, null, mouseButton);
        }
        return bo;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (focused) {
            if (isHovered || !GuiScreen.isShiftKeyDown()) { focused = false; }
        }
        else { focused = isHovered; }
        boolean bo = super.mouseReleased(mouseX, mouseY, mouseButton);
        if (isHovered && mouseButton == 0) {
            if (isMoved) { isMoved = false; }
            else {
                if (GuiScreen.isCtrlKeyDown()) {
                    if (listener.hasSelect(node.id)) { listener.removeSelect(node.id); } else { listener.addSelect(node.id); }
                }
                else { listener.setActive(node.id); }
                if (lastClicked + 500L > System.currentTimeMillis()) {
                    lastClicked = 0L;
                    return listener.doubleClicked(this);
                }
                else { lastClicked = System.currentTimeMillis(); }
                bo = true;
            }
        }
        return bo;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
        boolean bo = wrapper.mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
        if (isHovered && !bo && mouseButton == 0 && listener.selectLink == null && !isLock && listener.hasSelect(id)) {
            tempDx += dx;
            tempDy += dy;
            int x = (int) (Math.floor(tempDx) * listener.guiScale /
                    (GuiYellowDialogEditor.category != null ? GuiYellowDialogEditor.category.getScale() : 1.0f) / 2.0d);
            int y = (int) (Math.floor(tempDy) * listener.guiScale /
                    (GuiYellowDialogEditor.category != null ? GuiYellowDialogEditor.category.getScale() : 1.0f)/ 2.0d);
            int stepX = 1;
            int stepY = 1;
            if (GuiScreen.isShiftKeyDown()) {
                if (getX() % 10 != 0) { x = -(getX() % 10); }
                else {
                    stepX = 10;
                    x = (int) (Math.floor((float) x / 10.0f));
                }
                if (getY() % 10 != 0) { y = -(getY() % 10); }
                else {
                    stepY = 10;
                    y = (int) (Math.floor((float) y / 10.0f));
                }
            }
            if (x != 0 || y != 0) {
                x *= stepX;
                y *= stepY;
                if (x != 0) { tempDx -= x / listener.guiScale *
                        (GuiYellowDialogEditor.category != null ? GuiYellowDialogEditor.category.getScale() : 1.0f) * 2.0d; }
                if (y != 0) { tempDy -= y / listener.guiScale *
                        (GuiYellowDialogEditor.category != null ? GuiYellowDialogEditor.category.getScale() : 1.0f) * 2.0d; }
                listener.movedSelectNodes(x, y);
                isMoved = true;
            }
            return true;
        }
        return false;
    }

    @Override
    public void moveTo(int addX, int addY) {
        super.moveTo(addX, addY);
        node.x = guiLeft;
        node.y = guiTop;
    }

    @Override
    public void unFocused(GuiTextFieldNop textField) { listener.unFocused(this, textField); }

    @Override
    public void textUpdate(IComponentGui textEditor, String text) {
        listener.textUpdate(this, textEditor, text);
    }

    @Override
    public boolean isFocused() { return focused; }

    @Override
    public boolean isHovered() { return isHovered; }

    @Override
    public void tick() { }
}