package noppes.npcs.client.gui.global;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.CustomEntities;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.client.gui.select.SubGuiTextureSelection;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.DialogGuiSettings;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketDialogGuiSettings;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;

public class GuiNpcDialogGuiSettings
        extends GuiNPCInterface2
        implements ISliderListener, ITextfieldListener {

    protected final ResourceLocation[] resources = {
            new ResourceLocation(CustomNpcs.MODID, "textures/gui/screens/overworld.png"),
            new ResourceLocation(CustomNpcs.MODID, "textures/gui/screens/netherworld.png"),
            new ResourceLocation(CustomNpcs.MODID, "textures/gui/screens/endworld.png")
    };
    protected final DialogGuiSettings guiSettings;
    protected int dialogHeight;
    protected final EntityNPCInterface dialogNpc;
    protected int screenID = 0;

    public GuiNpcDialogGuiSettings(EntityNPCInterface npc) {
        super(npc);

        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        guiSettings = DialogController.instance.getGuiSettings();
        backGui = EnumGuiType.MainMenuGlobal;
        dialogNpc = Util.instance.copyToGUI(npc != null ? npc : new EntityCustomNpc(CustomEntities.entityCustomNpc, player.level()), player.level(), false);
        dialogNpc.display.setVisible(0);
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        switch (button.id) {
            case 1: guiSettings.setType(button.getValue()); init(); break;
            case 2: guiSettings.npcInLeft = ((GuiCheckBoxNop) button).selected(); init(); break;
            case 3: guiSettings.showNPC = ((GuiCheckBoxNop) button).selected(); init(); break;
            case 4: {
                setSubGui(new SubGuiColorSelector(guiSettings.backWindowColor, new SubGuiColorSelector.ColorCallback() {
                    @Override
                    public void color(int colorIn) { guiSettings.backWindowColor = colorIn; }
                    @Override
                    public void preColor(int colorIn) { guiSettings.backWindowColor = colorIn; }
                }).setIsAlpha().setOffsetX(-100));
                break;
            } // window color
            case 5: {
                setSubGui(new SubGuiColorSelector(guiSettings.backBorderColor, new SubGuiColorSelector.ColorCallback() {
                    @Override
                    public void color(int colorIn) { guiSettings.backBorderColor = colorIn; }
                    @Override
                    public void preColor(int colorIn) { guiSettings.backBorderColor = colorIn; }
                }).setIsAlpha().setOffsetX(-100));
                break;
            } // background color
            case 6: setSubGui(new SubGuiTextureSelection(this, 0, npc, guiSettings.getBackgroundTexture() == null ? "" : guiSettings.getBackgroundTexture().toString(), ".png", 3)); break;
            case 7: setSubGui(new SubGuiTextureSelection(this, 1, npc, guiSettings.getWindowTexture() == null ? "" : guiSettings.getWindowTexture().toString(), ".png", 3)); break;
            case 8: guiSettings.setShowVerticalLines(button.getValue()); break;
            case 9: {
                setSubGui(new SubGuiColorSelector(guiSettings.linesColor, new SubGuiColorSelector.ColorCallback() {
                    @Override
                    public void color(int colorIn) { guiSettings.linesColor = colorIn; }
                    @Override
                    public void preColor(int colorIn) { guiSettings.linesColor = colorIn; }
                }).setIsAlpha().setOffsetX(-100));
                break;
            } // lines color
            case 10: guiSettings.setShowHorizontalLines(button.getValue()); break;
            case 11: {
                setSubGui(new SubGuiColorSelector(guiSettings.scrollLineColor, new SubGuiColorSelector.ColorCallback() {
                    @Override
                    public void color(int colorIn) { guiSettings.scrollLineColor = colorIn; }
                    @Override
                    public void preColor(int colorIn) { guiSettings.scrollLineColor = colorIn; }
                }).setIsAlpha().setOffsetX(-100));
                break;
            } // scroll line color
            case 12: {
                setSubGui(new SubGuiColorSelector(guiSettings.hoverLineColor, new SubGuiColorSelector.ColorCallback() {
                    @Override
                    public void color(int colorIn) { guiSettings.hoverLineColor = colorIn; }
                    @Override
                    public void preColor(int colorIn) { guiSettings.hoverLineColor = colorIn; }
                }).setIsAlpha().setOffsetX(-100));
                break;
            } // hover line color
            case 13: {
                setSubGui(new SubGuiColorSelector(guiSettings.selectOptionLeftColor, new SubGuiColorSelector.ColorCallback() {
                    @Override
                    public void color(int colorIn) { guiSettings.selectOptionLeftColor = colorIn; }
                    @Override
                    public void preColor(int colorIn) { guiSettings.selectOptionLeftColor = colorIn; }
                }).setIsAlpha().setOffsetX(-100));
                break;
            } // select back color
            case 14: {
                setSubGui(new SubGuiColorSelector(guiSettings.pointerColor, new SubGuiColorSelector.ColorCallback() {
                    @Override
                    public void color(int colorIn) { guiSettings.pointerColor = colorIn; }
                    @Override
                    public void preColor(int colorIn) { guiSettings.pointerColor = colorIn; }
                }).setIsAlpha().setOffsetX(-100));
                break;
            } // pointer color
            case 15: {
                setSubGui(new SubGuiColorSelector(guiSettings.sliderColor, new SubGuiColorSelector.ColorCallback() {
                    @Override
                    public void color(int colorIn) { guiSettings.sliderColor = colorIn; }
                    @Override
                    public void preColor(int colorIn) { guiSettings.sliderColor = colorIn; }
                }).setIsAlpha().setOffsetX(-100));
                break;
            } // slider color
            case 66: onClose(); break;
            case 100: screenID = button.getValue() - 1; break;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        PoseStack matrixStack = graphics.pose();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        matrixStack.pushPose();
        // window:
        int x = guiLeft + 199;
        int y = guiTop + 7;
        matrixStack.translate(x, y, 1.0f);
        matrixStack.scale(0.5f, 0.5f, 0.5f);
        graphics.fillGradient(-1, -1, 428, 241, 0xFF404040, 0xFF404040);
        graphics.enableScissor(x, y, x + 213, y + 120);
        // background
        graphics.fillGradient(0, 0, 427, 240, 0xFFC6C6C6, 0xFFC6C6C6);
        if (screenID > -1 && screenID < 3) {
            matrixStack.pushPose();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            matrixStack.scale(1.66796875f, 0.9375f, 1.0f);
            graphics.blit(resources[screenID], 0, 0, 0, 0, 256, 256);
            matrixStack.popPose();
        }
        // dialog gui:
        // background
        int vType = guiSettings.getShowVerticalLines();
        int hType = guiSettings.getShowVerticalLines();
        int blurringLine = (int) (guiSettings.dialogWidth * guiSettings.getBlurringLine());
        if (guiSettings.backTexture != null) {
            matrixStack.pushPose();
            matrixStack.scale(1.66796875f, 0.9375f, 1.0f);
            graphics.blit(guiSettings.backTexture, 0, 0, 0, 0, 256, 256);
            matrixStack.popPose();
        }
        if (guiSettings.windowTexture != null) {
            matrixStack.pushPose();
            matrixStack.translate(guiSettings.guiLeft, 0.0f, 0.0f);
            matrixStack.scale(guiSettings.dialogWidth / 427.0f * 1.66796875f, 0.9375f, 1.0f);
            graphics.blit(guiSettings.windowTexture, 0, 0, 0, 0, 256, 256);
            matrixStack.popPose();
        }
        switch (guiSettings.getType()) {
            case 0: {
                // dialogs
                int left = guiSettings.guiLeft + blurringLine;
                if (blurringLine > 0) { fill(graphics, guiSettings.guiLeft, 0, left, 240, guiSettings.backBorderColor, guiSettings.backWindowColor); }
                graphics.fill(left, 0, 427, 240, guiSettings.backWindowColor);
                // border
                graphics.fill(0, 0, guiSettings.guiLeft, 240, guiSettings.backBorderColor);
                // lines
                if (vType != 0) {
                    int lX = guiSettings.guiLeft - 1;
                    if (hType == 2) {
                        graphics.vLine(lX, 0, dialogHeight - 2, guiSettings.linesColor);
                        graphics.vLine(lX, dialogHeight - 2, 238, guiSettings.linesColor);
                        lX = guiSettings.guiLeft - 3;
                    }
                    else if (hType == 1) {
                        graphics.vLine(lX, 0, 238, guiSettings.linesColor);
                        lX = guiSettings.guiLeft - 3;
                    }
                    if (vType == 2) { graphics.vLine(lX, 1, 238, guiSettings.linesColor); }
                }
                if (hType == 2) {
                    graphics.hLine(guiSettings.guiLeft, 424, dialogHeight - 3, guiSettings.linesColor);
                    graphics.hLine(guiSettings.guiLeft, 424, dialogHeight - 1, guiSettings.linesColor);
                }
                else if (hType == 1) { graphics.hLine(guiSettings.guiLeft, 424, dialogHeight - 2, guiSettings.linesColor); }
                break;
            } // right
            case 1: {
                // dialogs
                int left = guiSettings.guiLeft + blurringLine;
                int right = guiSettings.guiRight - blurringLine;
                if (blurringLine > 0) { fill(graphics, guiSettings.guiLeft, 0, left, 240, guiSettings.backBorderColor, guiSettings.backWindowColor); }
                graphics.fill(left, 0, right, 240, guiSettings.backWindowColor);
                if (blurringLine > 0) { fill(graphics, right, 0, guiSettings.guiRight, 240, guiSettings.backWindowColor, guiSettings.backBorderColor); }
                // border
                graphics.fill(0, 0, guiSettings.guiLeft, 240, guiSettings.backBorderColor);
                graphics.fill(guiSettings.guiRight, 0, 427, 240, guiSettings.backBorderColor);
                // lines
                if (vType != 0) {
                    int lX = guiSettings.guiLeft - 1;
                    int rX = guiSettings.guiRight + 1;
                    if (hType == 2) {
                        // left
                        graphics.vLine(lX, 0, dialogHeight - 2, guiSettings.linesColor);
                        graphics.vLine(lX, dialogHeight - 2, 238, guiSettings.linesColor);
                        // right
                        graphics.vLine(rX, 0, dialogHeight - 2, guiSettings.linesColor);
                        graphics.vLine(rX, dialogHeight - 2, 238, guiSettings.linesColor);
                        lX = guiSettings.guiLeft - 3;
                        rX += 2;
                    }
                    else if (hType == 1) {
                        // left
                        graphics.vLine(lX, 0, 238, guiSettings.linesColor);
                        // right
                        graphics.vLine(rX, 0, 238, guiSettings.linesColor);
                        lX = guiSettings.guiLeft - 3;
                        rX += 2;
                    }
                    if (vType == 2) {
                        // left
                        graphics.vLine(lX, 0, 238, guiSettings.linesColor);
                        // right
                        graphics.vLine(rX, 0, 238, guiSettings.linesColor);
                    }
                }
                if (hType == 2) {
                    graphics.hLine(guiSettings.guiLeft, guiSettings.guiRight, dialogHeight - 3, guiSettings.linesColor);
                    graphics.hLine(guiSettings.guiLeft, guiSettings.guiRight, dialogHeight - 1, guiSettings.linesColor);
                }
                else if (hType == 1) { graphics.hLine(guiSettings.guiLeft, guiSettings.guiRight, dialogHeight - 2, guiSettings.linesColor); }
                break;
            } // center
            case 2: {
                // dialogs
                int right = guiSettings.guiRight - blurringLine;
                graphics.fill(0, 0, right, 240, guiSettings.backWindowColor);
                if (blurringLine > 0) { fill(graphics, right, 0, guiSettings.dialogWidth, 240, guiSettings.backWindowColor, guiSettings.backBorderColor); }
                // border
                graphics.fill(guiSettings.dialogWidth, 0, 427, 240, guiSettings.backBorderColor);
                // lines
                if (vType != 0) {
                    int rX = guiSettings.guiRight + 1;
                    if (hType == 2) {
                        graphics.vLine(rX, 0, dialogHeight - 2, guiSettings.linesColor);
                        graphics.vLine(rX, dialogHeight - 2, 238, guiSettings.linesColor);
                        rX += 2;
                    }
                    else if (hType == 1) {
                        graphics.vLine(rX, 0, 238, guiSettings.linesColor);
                        rX += 2;
                    }
                    if (vType == 2) {
                        graphics.vLine(rX, 0, 238, guiSettings.linesColor);
                    }
                }
                if (hType == 2) {
                    graphics.hLine(guiSettings.guiLeft, guiSettings.guiRight, dialogHeight - 3, guiSettings.linesColor);
                    graphics.hLine(guiSettings.guiLeft, guiSettings.guiRight, dialogHeight - 1, guiSettings.linesColor);
                }
                else if (hType == 1) { graphics.hLine(guiSettings.guiLeft, guiSettings.guiRight, dialogHeight - 2, guiSettings.linesColor); }
                break;
            } // left
        }
        // Dialog text lines
        graphics.fill(guiSettings.guiLeft + 44, dialogHeight + 2, guiSettings.guiRight - 12, dialogHeight + 23, guiSettings.selectOptionLeftColor);
        graphics.hLine(guiSettings.guiLeft + 4, guiSettings.guiRight - 13, dialogHeight + 1, guiSettings.hoverLineColor);
        graphics.hLine(guiSettings.guiLeft + 4, guiSettings.guiRight - 13, dialogHeight + 23, guiSettings.hoverLineColor);
        graphics.hLine(guiSettings.guiLeft + 4, guiSettings.guiRight - 13, dialogHeight + 46, guiSettings.scrollLineColor);
        graphics.hLine(guiSettings.guiLeft + 4, guiSettings.guiRight - 13, dialogHeight + 70, guiSettings.scrollLineColor);
        // scroll bar
        graphics.fill(guiSettings.guiRight - 11, 1, guiSettings.guiRight - 1, 50, guiSettings.sliderColor);
        graphics.fill(guiSettings.guiRight - 11, dialogHeight + 1, guiSettings.guiRight - 1, dialogHeight + 35, guiSettings.sliderColor);
        // text
        x = 1 + guiSettings.guiLeft / 2;
        matrixStack.pushPose();
        matrixStack.scale(2.0f, 2.0f, 1.0f);
        graphics.drawString(font, getTempLine("Noppes: Dialog text"), x, 1, 0xE0E0E0);
        matrixStack.popPose();

        matrixStack.pushPose();
        matrixStack.scale(2.0f, 2.0f, 1.0f);
        y = 2 + dialogHeight / 2;
        graphics.drawString(font, getTempLine("  -> 1 Option"), x, y, 0xE0E0E0);
        graphics.drawString(font, getTempLine("    * 2 Option"), x, y += 12, 0xE0E0E0);
        graphics.drawString(font, getTempLine("    * 2 Option"), x, y + 12, 0xE0E0E0);
        matrixStack.popPose();

        matrixStack.popPose(); // <- line 144
        // NPC
        if (!hasSubGui() && guiSettings.showNPC && dialogNpc != null) {
            matrixStack.pushPose();
            matrixStack.translate(0.0f, 0.0f, 200.0f);
            drawNpc(graphics, dialogNpc,
                    199 + ((guiSettings.guiLeft + guiSettings.npcPosX) / 2), 7 + ((guiSettings.npcPosY) / 2),
                    guiSettings.getNpcScale() / 2.0f, guiSettings.getType() == 2 || (guiSettings.getType() == 1 && !guiSettings.npcInLeft) ? 40 : -40,
                    -15, 3);
            matrixStack.popPose();
        }
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void init() {
        super.init();
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        guiSettings.init(427.0d, 240.0d);

        int windowWidth = minecraft.getWindow().getGuiScaledWidth();
        int windowHeight = minecraft.getWindow().getGuiScaledHeight();

        width = windowWidth;
        height = windowHeight;
        dialogHeight = 240 - guiSettings.optionHeight;

        int x0 = guiLeft + 5;
        int x1 = guiLeft + 50;
        int x2 = guiLeft + 200;
        int y = guiTop + 4;
        int lId = 0;
        // type
        addLabel(lId++, x0, y + 1, "gui.type")
                .setSize(43, 12);
        addButton(1, x1, y, true, guiSettings.getType(), "gui.left", "gui.center", "gui.right")
                .setSize(60, 14)
                .setHoverTexts("dialog.gui.settings.hover.place");
        // npc in left
        addCheckBox(2, x1 + 63, y - 1, "gui.left", "gui.right", guiSettings.npcInLeft)
                .setSize(83, 14)
                .setIsVisible(guiSettings.getType() == 1)
                .setHoverTexts("dialog.gui.settings.hover.in.left");
        // width
        addLabel(lId++, x0, (y += 16) + 1, "scale.width")
                .setSize(43, 12);
        addSlider(0, x1, y + 3, 2.0f * (float) guiSettings.getWidth() - 0.8f)
                .setSize(108, 8)
                .setHoverTexts("dialog.gui.settings.hover.width");
        addTextField(0, x1 + 110, y, 30, 13, (int) (guiSettings.getWidth() * 100.0d))
                .setMinMaxDefault(40, 90, (int) (guiSettings.getWidth() * 100.0d))
                .setHoverTexts("dialog.gui.settings.hover.width");
        addLabel(lId++, x1 + 142, y + 1, "%")
                .setSize(7, 12);
        // option height
        addLabel(lId++, x0, (y += 15) + 1, "schematic.height")
                .setSize(43, 12);
        addSlider(1, x1, y + 3, 1.666667f * (float) guiSettings.getOptionHeight() - 0.25f)
                .setSize(108, 8)
                .setHoverTexts("dialog.gui.settings.hover.height");
        addTextField(1, x1 + 110, y, 30, 13, (int) (guiSettings.getOptionHeight() * 100.0d))
                .setMinMaxDefault(15, 75, (int) (guiSettings.getOptionHeight() * 100.0d))
                .setHoverTexts("dialog.gui.settings.hover.height");
        addLabel(lId++, x1 + 142, y + 1, "%")
                .setSize(7, 12);
        // npc
            // show
        addLabel(lId++, x0, (y += 18) + 1, "NPC: ")
                .setSize(43, 12);
        addCheckBox(3, x1, y, "availability.visible", "availability.invisible", guiSettings.showNPC)
                .setSize(83, 14)
                .setHoverTexts("dialog.gui.settings.hover.visible");
            // pos x
        addLabel(lId++, x1 - 12, (y += 15) + 1, "X: ")
                .setSize(43, 12);
        float[] pos = guiSettings.getNpcPos();
        addSlider(2, x1, y + 3, pos[0] + 0.5f)
                .setSize(108, 8)
                .setHoverTexts("dialog.gui.settings.hover.pos", "X");
        addTextField(2, x1 + 110, y, 30, 13, (int) (100.0f * pos[0]))
                .setMinMaxDefault(-50, 50, (int) (100.0f * pos[0]))
                .setHoverTexts("dialog.gui.settings.hover.pos", "X");
        addLabel(lId++, x1 + 142, y + 1, "%")
                .setSize(7, 12);
            // pos y
        addLabel(lId++, x1 - 12, (y += 15) + 1, "Y: ")
                .setSize(43, 12);
        addSlider(3, x1, y + 3, 2.0f * pos[1] + 0.5f)
                .setSize(108, 8)
                .setHoverTexts("dialog.gui.settings.hover.pos", "Y");
        addTextField(3, x1 + 110, y, 30, 13, Math.round(100.0f * pos[1]))
                .setMinMaxDefault(-25, 25, Math.round(100.0f * pos[1]))
                .setHoverTexts("dialog.gui.settings.hover.pos", "Y");
        addLabel(lId++, x1 + 142, y + 1, "%")
                .setSize(7, 12);
            // scale
        addLabel(lId++, x0, (y += 15) + 1, "model.scale")
                .setSize(43, 12);
        addSlider(4, x1, y + 3, 0.206186f * guiSettings.getNpcScale() - 0.030928f)
                .setSize(108, 8)
                .setHoverTexts("dialog.gui.settings.hover.scale");
        addTextField(4, x1 + 110, y, 30, 13, Math.round(100.0f * guiSettings.getNpcScale()))
                .setMinMaxDefault(15, 500, Math.round(100.0f * guiSettings.getNpcScale()))
                .setHoverTexts("dialog.gui.settings.hover.scale");
        addLabel(lId++, x1 + 142, y + 1, "%")
                .setSize(7, 12);
        // blurring line
        addLabel(lId++, x0, (y += 15) + 1, "gui.conversion")
                .setSize(43, 12);
        addSlider(5, x1, y + 3, 4.0f * guiSettings.getBlurringLine())
                .setSize(108, 8)
                .setHoverTexts("dialog.gui.settings.hover.blurring.line");
        addTextField(5, x1 + 110, y, 30, 13, Math.round(100.0f * guiSettings.getBlurringLine()))
                .setMinMaxDefault(0, 25, Math.round(100.0f * guiSettings.getBlurringLine()))
                .setHoverTexts("dialog.gui.settings.hover.blurring.line");
        addLabel(lId++, x1 + 142, y + 1, "%")
                .setSize(7, 12);
        // background color
        addLabel(lId++, x0, (y += 16) + 1, "gui.background")
                .setSize(43, 12);
        addTextField(6, x1, y, 146, 13, guiSettings.getBackgroundTexture() == null ? "" : guiSettings.getBackgroundTexture())
                .setHoverTexts("dialog.gui.settings.hover.background");
        addButton(6, x2, y - 1, "mco.template.button.select")
                .setSize(50, 15);
        add(new GuiColorButtonNop(this, 5, x2 + 52, y - 1, guiSettings.backBorderColor)
                .setSize(30, 15)
                .setHoverTexts("dialog.gui.settings.hover.back.color"));
        addButton(100, guiLeft + imageWidth - 67, y, true, screenID + 1, "over", "nether", "end")
                .setSize(60, 14)
                .setHoverTexts("dialog.gui.settings.hover.place");
        // window color
        addLabel(lId++, x0, (y += 15) + 1, "gui.texture")
                .setSize(43, 12);
        addTextField(7, x1, y, 146, 13, guiSettings.getWindowTexture() == null ? "" : guiSettings.getWindowTexture())
                .setHoverTexts("dialog.gui.settings.hover.window");
        addButton(7, x2, y - 1, "mco.template.button.select")
                .setSize(50, 15);
        add(new GuiColorButtonNop(this, 4, x2 + 52, y - 1, guiSettings.backWindowColor)
                .setSize(30, 15)
                .setHoverTexts("dialog.gui.settings.hover.win.color"));
        // colors
        x1 += 30;
            // lines
        addLabel(lId++, x0, (y += 15) + 2, Component.translatable("gui.color").append(Component.literal(":")))
                .setSize(43, 12);
        addLabel(lId++, x0, (y += 15) + 1, "dialog.gui.settings.line.v")
                .setSize(73, 12);
        addButton(8, x1, y, true, guiSettings.getShowVerticalLines(), 0, 1, 2)
                .setSize(35, 14)
                .setHoverTexts("dialog.gui.settings.hover.line.v.type");
        add(new GuiColorButtonNop(this, 9, x1 + 37, y + 7, guiSettings.linesColor)
                .setSize(30, 15)
                .setHoverTexts("dialog.gui.settings.hover.line.color"));
        addLabel(lId++, x0, (y += 15) + 1, "dialog.gui.settings.line.h")
                .setSize(73, 12);
        addButton(10, x1, y, true, guiSettings.getShowHorizontalLines(), 0, 1, 2)
                .setSize(35, 14)
                .setHoverTexts("dialog.gui.settings.hover.line.h.type");
            // options
        x0 = x1 + 69;
        x1 = x0 + 75;
        y -= 30;
        addLabel(lId++, x0, y + 1, "dialog.gui.settings.line.option.slot")
                .setSize(73, 12);
        add(new GuiColorButtonNop(this, 11, x1, y, guiSettings.scrollLineColor)
                .setSize(30, 15)
                .setHoverTexts("dialog.gui.settings.hover.line.option.slot"));
        addLabel(lId++, x0, (y += 15) + 1, "dialog.gui.settings.line.option.select")
                .setSize(73, 12);
        add(new GuiColorButtonNop(this, 12, x1, y, guiSettings.hoverLineColor)
                .setSize(30, 15)
                .setHoverTexts("dialog.gui.settings.hover.line.option.select"));
        addLabel(lId++, x0, (y += 15) + 1, "dialog.gui.settings.option.back")
                .setSize(73, 12);
        add(new GuiColorButtonNop(this, 13, x1, y, guiSettings.selectOptionLeftColor)
                .setSize(30, 15)
                .setHoverTexts("dialog.gui.settings.hover.option.back"));
            // select
        x0 = x1 + 32;
        x1 = x0 + 75;
        y -= 30;
        addLabel(lId++, x0, y + 1, "dialog.gui.settings.pointer.color")
                .setSize(73, 12);
        add(new GuiColorButtonNop(this, 14, x1, y, guiSettings.pointerColor)
                .setSize(30, 15)
                .setHoverTexts("dialog.gui.settings.hover.pointer.color"));
        addLabel(lId, x0, (y += 15) + 1, "dialog.gui.settings.slider.color")
                .setSize(73, 12);
        add(new GuiColorButtonNop(this, 15, x1, y, guiSettings.sliderColor)
                .setSize(30, 15)
                .setHoverTexts("dialog.gui.settings.slider.color"));
    }

    @Override
    public void mouseDragged(GuiSliderNop slider) {
        switch (slider.id) {
            case 0: {
                guiSettings.setWidth(Math.ceil((0.5f * slider.getValue() + 0.4f) * 100.0f) / 100.0f);
                if (getTextField(0) != null) { getTextField(0).setValue("" + Math.round(guiSettings.getWidth() * 100.0d)); }
                break;
            } // width
            case 1: {
                guiSettings.setOptionHeight(Math.round((0.6f * slider.getValue() + 0.15f) * 100.0d) / 100.0d);
                if (getTextField(1) != null) { getTextField(1).setValue("" + Math.round(guiSettings.getOptionHeight() * 100.0d)); }
                break;
            } // option height
            case 2: {
                float[] pos = guiSettings.getNpcPos();
                pos[0] = Math.round((slider.getValue() - 0.5f) * 100.0f) / 100.0f;
                guiSettings.setNpcPos(pos[0], pos[1]);
                if (getTextField(2) != null) { getTextField(2).setValue("" + Math.round(100.0f * pos[0])); }
                break;
            } // npc pos x
            case 3: {
                float[] pos = guiSettings.getNpcPos();
                pos[1] = Math.round((0.5f * slider.getValue() - 0.25f) * 100.0f) / 100.0f;
                guiSettings.setNpcPos(pos[0], pos[1]);
                if (getTextField(3) != null) { getTextField(3).setValue("" + Math.round(100.0f * pos[1])); }
                break;
            } // npc pos y
            case 4: {
                guiSettings.setNpcScale(4.85f * slider.getValue() + 0.15f);
                if (getTextField(4) != null) { getTextField(4).setValue("" + Math.round(100.0f * guiSettings.getNpcScale())); }
                break;
            } // npc scale
            case 5: {
                guiSettings.setBlurringLine(slider.getValue() / 4.0f);
                if (getTextField(5) != null) { getTextField(5).setValue("" + Math.round(100.0f * guiSettings.getBlurringLine())); }
                break;
            } // blurring line
        }
        guiSettings.init(427, 240);
        dialogHeight = 240 - guiSettings.optionHeight;
    }

    @Override
    public void unFocused(GuiTextFieldNop textField) {
        switch (textField.id) {
            case 0: {
                guiSettings.setWidth(textField.getInteger() / 100.0d);
                if (getSlider(0) != null) { getSlider(0).setValue(2.0f * (float) guiSettings.getWidth() - 0.8f); }
                break;
            } // width
            case 1: {
                float[] pos = guiSettings.getNpcPos();
                pos[1] = 0.5f * textField.getInteger() - 0.25f;
                guiSettings.setOptionHeight(textField.getInteger() / 100.0f);
                if (getSlider(1) != null) { getSlider(1).setValue(1.666667f * (float) guiSettings.getOptionHeight() - 0.25f); }
                break;
            } // option height
            case 2: {
                float[] pos = guiSettings.getNpcPos();
                pos[0] = 0.01f * textField.getInteger();
                guiSettings.setNpcPos(pos[0], pos[1]);
                if (getSlider(2) != null) { getSlider(2).setValue(pos[0] + 0.5f); }
                break;
            } // npc pos x
            case 3: {
                float[] pos = guiSettings.getNpcPos();
                pos[1] = 0.01f * (float) textField.getInteger();
                guiSettings.setNpcPos(pos[0], pos[1]);
                if (getSlider(3) != null) { getSlider(3).setValue(2.0f * pos[1] + 0.5f); }
                break;
            } // npc pos y
            case 4: {
                guiSettings.setNpcScale((float) textField.getInteger() / 100.0f);
                if (getSlider(4) != null) { getSlider(4).setValue(0.206186f * guiSettings.getNpcScale() - 0.030928f); }
                break;
            } // npc scale
            case 5: {
                guiSettings.setBlurringLine((float) textField.getInteger() / 100.0f);
                if (getSlider(5) != null) { getSlider(5).setValue(4.0f * guiSettings.getBlurringLine()); }
                break;
            } // blurring line
            case 6: {
                if (textField.isEmpty()) { guiSettings.backTexture = null; }
                else { guiSettings.backTexture = new ResourceLocation(textField.getValue()); }
                break;
            } // background texture
            case 7: {
                if (textField.isEmpty()) { guiSettings.windowTexture = null; }
                else { guiSettings.windowTexture = new ResourceLocation(textField.getValue()); }
                break;
            } // window texture
        }
        guiSettings.init(427, 240);
        dialogHeight = 240 - guiSettings.optionHeight;
    }

    @Override
    public void mousePressed(GuiSliderNop slider) { }

    @Override
    public void mouseReleased(GuiSliderNop slider) { }

    @Override
    public void subGuiClosed(Screen subgui) {
        if (subgui instanceof SubGuiTextureSelection tGui) {
            if (tGui.id == 0) {
                guiSettings.backTexture = tGui.resource;
            } // Background
            else if (tGui.id == 1) {
                guiSettings.windowTexture = tGui.resource;
            } // window
            init();
        }
    }

    @Override
    public void save() { Packets.sendServer(new SPacketDialogGuiSettings(guiSettings.save())); }

    private String getTempLine(String line) {
        if (font.width(line) > guiSettings.dialogWidth / 2 - 7) {
            while (font.width(line + "...") > guiSettings.dialogWidth / 2 - 7) { line = line.substring(0, line.length() - 1); }
            line += "...";
        }
        return line;
    }

}