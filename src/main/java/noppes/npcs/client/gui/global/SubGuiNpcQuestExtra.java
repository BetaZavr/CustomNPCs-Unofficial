package noppes.npcs.client.gui.global;

import java.awt.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import noppes.npcs.client.gui.player.GuiLog;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.shared.client.gui.GuiTextAreaScreen;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.select.SubGuiNPCSelection;
import noppes.npcs.client.gui.select.SubGuiTextureSelection;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityNPCInterface;

import javax.annotation.Nonnull;

// New from Unofficial (BetaZavr)
public class SubGuiNpcQuestExtra extends GuiNPCInterface implements ITextfieldListener {

    protected static final ResourceLocation SHEET = new ResourceLocation(CustomNpcs.MODID, "textures/quest/log/q_log_3.png");
    protected static final ResourceLocation TABS = new ResourceLocation(CustomNpcs.MODID, "textures/quest/log/q_log_4.png");

    protected EntityNPCInterface showNpc;
    public Quest quest;

    public SubGuiNpcQuestExtra(Quest q) {
        super();
        setBackground("menubg.png");
        imageWidth = 256;
        imageHeight = 217;

        quest = q;
        showNpc = quest.completer.getNpc();
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        switch (button.id) {
            case 0: setSubGui(new SubGuiTextureSelection(this, 0, showNpc, quest.icon.toString(), ".png", 3)); break; // icon select
            case 1: quest.completion = EnumQuestCompletion.values()[button.getValue()]; break; // completion type
            case 2: setSubGui(new SubGuiNPCSelection(quest.completer.getNpc())); break; // select npc
            case 3: setSubGui(new SubGuiTextureSelection(this, 1, showNpc, quest.texture == null ? "" : quest.texture.toString(), ".png", 3)); break; // texture select
            case 4: setSubGui(new GuiTextAreaScreen(0, quest.rewardText)); break; // reward text
            case 5: {
                quest.extraButton = button.getValue();
                init();
                break;
            } // extra button type
            case 6: setSubGui(new GuiTextAreaScreen(1, quest.extraButtonText)); break; // extra button hover text
            case 7: quest.showProgressInChat = ((GuiCheckBoxNop) button).selected(); break;
            case 8: quest.showProgressInWindow = ((GuiCheckBoxNop) button).selected(); break;
            case 9: quest.completer.setStrict(((GuiCheckBoxNop) button).selected()); break;
            case 66: onClose(); break;
        }
    }

    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics) {
        super.renderBackground(graphics);
        int u = guiLeft + 182;
        int v = guiTop + 97;
        if (getButton(2) != null) {
            u = getButton(2).getX() + getButton(2).getWidth() + 7;
            v = getButton(2).getY() + 2;
        }
        PoseStack matrixStack = graphics.pose();
        RenderSystem.enableBlend();
        // Back on NPC
        int color = new Color(0xFF404040).getRGB();
        matrixStack.pushPose();
        matrixStack.translate(u + 5.0f, v + 3.0f, 1.0f);
        graphics.fill(-6, -6, 61, 61, color);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(SHEET, -5, -5, 34, 54, 65, 65);
        matrixStack.popPose();

        if (showNpc != null && !hasSubGui()) {
            // NPC
            matrixStack.pushPose();
            graphics.enableScissor((u + 10), (v + 11), (u + 54), (v + 44));
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            matrixStack.translate(0.0f, 0.0f, 10.0f);
            String modelName;
            if (showNpc.display.getModel() != null) { modelName = showNpc.display.getModel(); }
            else { modelName = EntityType.getKey(showNpc.getType()).toString(); }
            float[] offsets = GuiLog.preDrawEntity(modelName, showNpc);
            drawNpc(graphics, showNpc, 218 + (int) offsets[0],
                    149 + (int) offsets[1],
                    offsets[2], 30, -5, 1);
            graphics.disableScissor();
            matrixStack.popPose();

            // Fase
            RenderSystem.enableBlend();
            matrixStack.pushPose();
            RenderSystem.disableDepthTest();
            matrixStack.translate(u + 1.0f, v + 1.0f, 150.0f);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            graphics.blit(TABS, 0, 0, 193, 0, 63, 52);
            matrixStack.popPose();

            // Name
            Component name = Component.literal(quest.completer.getName());
            u += 1;
            v += 51;
            matrixStack.pushPose();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            matrixStack.translate(0.0f, 0.0f, 200.0f);
            GuiButtonNop.renderString(graphics, name, u, v, u + 63, v +10,
                    CustomNpcs.QuestLogColor.getRGB(), false, true, null);
            matrixStack.popPose();
        }
        // script button
        if (quest.extraButton > 0 && !hasSubGui()) {
            u = guiLeft + 98;
            v = guiTop + 134;
            if (getButton(5) != null) {
                u = getButton(5).getX() - 12;
                v = getButton(5).getY() + 3;
            }
            matrixStack.pushPose();
            matrixStack.translate(u, v, 100.0f);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            graphics.blit(SHEET, -1, -1, 34, 20, 11, 11);
            graphics.blit(TABS, 0, 0, 116 + quest.extraButton * 9, 0, 9, 9);
            matrixStack.popPose();
        }
        // quest icon
        u = guiLeft + 214;
        v = guiTop + 4;
        if (getButton(0) != null) {
            u = getButton(0).getX() + getButton(0).getWidth() + 5;
            v = getButton(0).getY() - 1;
        }
        matrixStack.pushPose();

        matrixStack.translate(u + 1.0f, v + 1.0f, 1.0f);
        graphics.fill(-1, -1,  33, 33, color);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(SHEET, 0, 0, 34, 54, 32, 32);
        matrixStack.popPose();

        if (quest.icon != null) {
            matrixStack.pushPose();
            matrixStack.translate(u + 1.0f, v + 1.0f, 1.0f);
            matrixStack.scale(0.125f, 0.125f, 1.0f);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            graphics.blit(quest.icon, 0, 0, 0, 0, 256, 256);
            matrixStack.popPose();
        }

        // quest texture
        u = guiLeft + 214;
        v = guiTop + 38;
        if (getButton(3) != null) {
            u = getButton(3).getX() + getButton(3).getWidth() + 5;
            v = getButton(3).getY() - 1;
        }
        matrixStack.pushPose();
        matrixStack.translate(u + 1.0f, v + 1.0f, 1.0f);
        graphics.fill(-1, -1, 33, 33, color);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(SHEET, 0, 0, 34, 54, 32, 32);
        matrixStack.popPose();

        if (quest.texture != null) {
            matrixStack.pushPose();
            matrixStack.translate(u + 1.0f, v + 1.0f, 1.0f);
            matrixStack.scale(0.125f, 0.125f, 1.0f);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            graphics.blit(quest.texture, 0, 0, 0, 0, 256, 256);
            matrixStack.popPose();
        }
    }

    @Override
    public void init() {
        super.init();
        int x0 = guiLeft + 5;
        int x1 = x0 + 110;
        int x2 = x1 + 34;
        int y = guiTop + 5;
        int lId = 0;
        // icon
        addLabel(lId++, x0, y + 2, "quest.icon")
                .setSize(142, 10);
        addButton(0, x2, y, "availability.select")
                .setSize(60, 14)
                .setHoverTexts("quest.hover.edit.quest.icon.sel");
        addTextField(0, x0, y += 16, 203, 16, quest.icon.toString())
                .setHoverTexts("quest.hover.edit.quest.icon.path");
        // texture description
        addLabel(lId++, x0, (y += 18) + 2, "quest.texture")
                .setSize(142, 10);
        addButton(3, x2, y, "availability.select")
                .setSize(60, 14)
                .setHoverTexts("quest.hover.edit.quest.texture.sel");
        addTextField(1, x0, y += 16, 203, 16, quest.texture)
                .setHoverTexts("quest.hover.edit.quest.texture.path");
        // completion npc
        addButton(1, x0, y += 19, false, quest.completion.ordinal(), "quest.npc", "quest.instant")
                .setSize(108, 14)
                .setHoverTexts("quest.hover.edit.quest.completion");
        addButton(2, x1, y, "availability.select")
                .setSize(60, 14)
                .setHoverTexts("quest.hover.edit.quest.completion.npc");
        addCheckBox(9, guiLeft + 5, y += 18, "quest.completer.strict.true", "quest.completer.strict.false", quest.completer.isStrict())
                .setSize(170, 14)
                .setHoverTexts("quest.hover.completer.strict");
        // reward text
        addLabel(lId++, guiLeft + 5, (y += 16) + 2, "quest.questrewardtext")
                .setSize(108, 10);
        addButton(4, x1, y, quest.rewardText.isEmpty() ? "selectServer.edit" : "advanced.editing mode")
                .setSize(60, 14)
                .setHoverTexts("quest.hover.edit.reward.text");
        // extra button
        addLabel(lId++, guiLeft + 5, (y += 16) + 2, "quest.extra.button.type")
                .setSize(108, 10);
        addButton(5, x1, y, true, quest.extraButton, "gui.none", "1", "2", "3", "4", "5")
                .setSize(60, 14)
                .setHoverTexts("quest.hover.extra.button.type", EnumScriptType.QUEST_LOG_BUTTON.function);
        // extra button text
        addLabel(lId, guiLeft + 5, (y += 16) + 2, "quest.extra.button.text")
                .setSize(108, 10);
        addButton(6, x1, y, "selectServer.edit")
                .setSize(60, 14)
                .setIsEnabled(quest.extraButton > 0).setHoverTexts("quest.hover.extra.button.text");
        // progress in chat / window
        addCheckBox(7, x0, (y += 17), "quest.show.progress.in.chat", null, quest.showProgressInChat)
                .setSize(242, 14)
                .setHoverTexts("quest.hover.show.in.chat");
        addCheckBox(8, x0, y + 16, "quest.show.progress.in.window", null, quest.showProgressInWindow)
                .setSize(242, 14)
                .setHoverTexts("quest.hover.show.in.window");
        // exit
        addButton(66, x0, guiTop + imageHeight - 19, "gui.done")
                .setSize(60, 14)
                .setHoverTexts("hover.back");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        boolean bo = super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!hasSubGui()) {
            int u = guiLeft + 214, v = guiTop + 5;
            if (getButton(0) != null) {
                u = getButton(0).getX() + getButton(0).getWidth() + 6;
                v = getButton(0).getY();
            }
            if (isMouseHover(mouseX, mouseY, u, v, 32, 32)) {
                setSubGui(new SubGuiTextureSelection(this, 0, showNpc, quest.icon.toString(), ".png", 3));
                return bo;
            }
            v = guiTop + 37;
            if (getButton(3) != null) {
                u = getButton(3).getX() + getButton(3).getWidth() + 6;
                v = getButton(3).getY();
            }
            if (isMouseHover(mouseX, mouseY, u, v, 32, 32)) {
                setSubGui(new SubGuiTextureSelection(this,1, showNpc, quest.texture == null ? "" : quest.texture.toString(), ".png", 3));
                return bo;
            }
            if (isMouseHover(mouseX, mouseY, guiLeft + 182, guiTop + 95, 65, 65)) {
                setSubGui(new SubGuiNPCSelection(quest.completer.getNpc()));
            }
        }
        return bo;
    }

    @Override
    public void subGuiClosed(Screen subgui) {
        if (subgui instanceof GuiTextAreaScreen gui) {
            if (gui.id == 0) { quest.rewardText = gui.text; }
            else if (gui.id == 1) { quest.extraButtonText = gui.text; }
            init();
        }
        else if (subgui instanceof SubGuiTextureSelection gui) {
            if (gui.id == 0) {
                quest.icon = gui.resource;
                if (quest.icon == null) {
                    quest.icon = new ResourceLocation(CustomNpcs.MODID, "textures/quest icon/q_0.png");
                }
            } else {
                quest.texture = gui.resource;
            }
            init();
        } else if (subgui instanceof SubGuiNPCSelection gui) {
            if (gui.selectEntity == null) {
                return;
            }
            Entity entity = player.level().getEntity(gui.selectEntity.getId());
            if (entity instanceof EntityNPCInterface cNpc) {
                quest.completer.reset(cNpc);
                showNpc = quest.completer.getNpc();
                init();
            }

        }
    }

    @Override
    public void unFocused(GuiTextFieldNop textField) {
        if (textField.id == 0) {
            if (textField.getValue().isEmpty()) { quest.icon = new ResourceLocation(CustomNpcs.MODID, "textures/quest icon/q_0.png"); }
            else { quest.icon = new ResourceLocation(textField.getValue()); }
            textField.setValue(quest.icon.toString());
        }
        else if (textField.id == 1) {
            if (textField.getValue().isEmpty()) { quest.texture = null; }
            else { quest.texture = new ResourceLocation(textField.getValue()); }
            textField.setValue(quest.texture == null ? "" : quest.texture.toString());
        }
    }

}
