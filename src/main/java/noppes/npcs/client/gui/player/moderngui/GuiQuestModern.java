package noppes.npcs.client.gui.player.moderngui;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.entity.data.ICustomDrop;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.TextBlockClient;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.client.IMouseHandlerMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketDialogSelected;
import noppes.npcs.packets.server.SPacketQuestCompletionCheckAll;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.listeners.IGuiClose;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

public class GuiQuestModern
        extends GuiNPCInterface
        implements IGuiClose {

    protected boolean isGrabbed = false;
    protected final Dialog prevDialog;
    protected final int optionId;
    protected final Quest quest;

    // Display
    protected float wScale = 1.0F;
    protected float hScale = 1.0F;
    protected final EntityNPCInterface dialogNpc;

    // ITEM, DIALOG, KILL, LOCATION, AREAKILL, MANUAL, CRAFT
    Component[] questType = new Component[] {
            Component.translatable("questgui.bringitems"),
            Component.translatable("questgui.readdialog"),
            Component.translatable("questgui.killmobs"),
            Component.translatable("questgui.findlocation"),
            Component.translatable("questgui.defeat") };

    public GuiQuestModern(EntityNPCInterface npc, Quest questIn, Dialog prevDialogIn, int optionIdIn) {
        super(npc);
        imageHeight = 238;

        prevDialog = prevDialogIn;
        quest = questIn;
        optionId = optionIdIn;

        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        if (npc instanceof EntityDialogNpc) { dialogNpc = null; }
        else { dialogNpc = Util.instance.copyToGUI(npc, minecraft.level, false); }
    }

    @Override
    public void init() {
        super.init();
        isGrabbed = false;
        grabMouse(false);
        guiTop = height - imageHeight;
        addButton(0, 720, 326, "")
                .setTexture(GuiDialogModern.DECOMPOSED)
                .setUV(36, 26, 78, 20);
        addButton(1, 812, 326, "")
                .setTexture(GuiDialogModern.DECOMPOSED)
                .setUV(36, 26, 78, 20);

        wScale = (float) width / 960.0F;
        hScale = (float) height / 509.0F;
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        if (button.id == 0) {
            Packets.sendServer(new SPacketDialogSelected(prevDialog.id, -1));
            onClose();
        }
        else if (button.id == 1) {
            if (optionId != -2) { Packets.sendServer(new SPacketDialogSelected(prevDialog.id, optionId)); }
            else { CustomNpcs.proxy.openGui(player, new GuiDialogModern(npc, prevDialog)); }
        }
    }

    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics) {
        graphics.fillGradient(0, 0, width, height, 0x66000000, 0x66000000);
        if (dialogNpc != null) {
            drawNpc(graphics, dialogNpc,
                    -210 + (int) (300.0F * (1.0F - wScale)),
                    350 - (int)(100.0F * (1.0F - hScale)),
                    9.5F * hScale,
                    -10, 0,
                    0);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int textBlockWidth = 700;
        String takeQuestString = Component.translatable("questgui.doyouaccept").getString();
        int lineCount = getLineCount(takeQuestString, textBlockWidth);
        int gap = Math.max(16, Math.min((int)(2.6f * (float)lineCount), 32));
        int textPartHeight = 26 + lineCount * ClientProxy.Font.height(null) + 2 * gap;

        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(0.0F, 0.5F, 2000.0F);
        graphics.fillGradient(0, height - textPartHeight, width, height, 0x99000000, 0x99000000);
        drawLine(graphics, 23, height - textPartHeight + 23, width - 23);

        matrixStack.scale(1.5f, 1.5f, 1.0f);
        graphics.drawString(font, npc.getDisplayName(), 31, (int)((double)(height - textPartHeight + 5) / 1.5), -1);
        matrixStack.scale(0.6666667f, 0.6666667f, 1.0f);
        drawTextBlock(graphics, takeQuestString, (width - textBlockWidth) / 2, height - textPartHeight + 23 + 3 + gap, textBlockWidth, -1);
        matrixStack.scale(wScale, wScale, wScale);

        StringBuilder objectiveString = new StringBuilder();
        for (IQuestObjective objective : quest.questInterface.getObjectives(player)) {
            if (objective == null) { continue; }
            objectiveString.append("- ")
                    .append(questType[objective.getType()])
                    .append(": ")
                    .append(objective.getText())
                    .append("\n");
        }

        int questLineCount = getLineCount(quest.logText, 180);
        int objectivesLineCount = getLineCount(objectiveString.toString(), 180);
        int topToTextBottom = 78 + questLineCount * ClientProxy.Font.height(null) + 20;
        int topToObjectivesBottom = topToTextBottom + 19 + objectivesLineCount * ClientProxy.Font.height(null) + 14;
        int rewardCount = 0;

        List<Integer> facIDs = quest.factionOptions.getIDs();
        for (ICustomDrop reward : quest.getRewards()) {
            if (!reward.getItem().isEmpty()) { ++rewardCount; }
        }

        int topToRewardsBottom = topToObjectivesBottom + (rewardCount == 0 ? 0 : 49);
        int topToExpBottom = topToRewardsBottom + (quest.rewardExp == 0 ? 0 : 12);
        int topToFactionBottom = topToExpBottom + facIDs.size() * 15;
        int questBlockHeight = topToFactionBottom + 28;
        graphics.fillGradient(675, 40, 935, questBlockHeight, 0xBB000000, -0xBB000000);
        matrixStack.scale(1.5f, 1.5f, 1.0f);
        graphics.drawString(font, quest.getName(), 461, 33, -1);
        matrixStack.scale(0.6666667f, 0.6666667f, 1.0f);
        drawLine(graphics, 686, 66, 924);

        drawTextBlock(graphics, quest.logText, 715, 80, 180, 0xB8B8B8);
        graphics.drawString(font, Component.translatable("questgui.objectives"), 690, topToTextBottom, -1);
        drawLeftAllignedTextBlock(graphics, objectiveString.toString(), 705, topToTextBottom + 12, 180, 0xB8B8B8);
        if (rewardCount != 0) {
            graphics.drawString(font, Component.translatable("questgui.rewards"), 690, topToObjectivesBottom, -1);
        }
        for (int i = 0; i < quest.rewardItems.size(); ++i) {
            ItemStack rewardStack = quest.rewardItems.get(i).item;
            if (rewardStack == null || rewardStack.isEmpty()) continue;

            graphics.blit(GuiDialogModern.DECOMPOSED, 690 + 26 * i, topToObjectivesBottom + 16, 0, 27, 24, 24);
            graphics.renderItem(rewardStack, 694 + 26 * i, topToObjectivesBottom + 20);
            graphics.renderItemDecorations(font, rewardStack, 694 + 26 * i, topToObjectivesBottom + 20, "" + rewardStack.getCount());

        }

        if (quest.rewardExp != 0) {
            graphics.drawString(font, Component.translatable("questgui.experience"), 690, topToRewardsBottom, 0xB8B8B8);
            int expPosX = 690 + ClientProxy.Font.width(Component.translatable("questgui.experience") + " ");
            graphics.drawString(font, "" + quest.rewardExp, expPosX, topToRewardsBottom, -1);
            int expSymbolPosX = expPosX + ClientProxy.Font.width(quest.rewardExp + "  ");
            graphics.blit(GuiDialogModern.DECOMPOSED, expSymbolPosX, topToRewardsBottom, 26, 27, 8, 8);
        }

        int facIDIndex = 0;
        for (int factionId : facIDs) {
            String fac1Name = FactionController.instance.getFaction(factionId).getName();
            String fac1Color = quest.factionOptions.get(factionId).decreaseFactionPoints ? ChatFormatting.RED + "-" : ChatFormatting.AQUA + "a+";
            int fac1Point = quest.factionOptions.get(factionId).factionPoints;
            graphics.drawString(font, fac1Name + "  " + fac1Color + fac1Point, 690, topToExpBottom + facIDIndex * 12, 0xFFB8B8B8);
            facIDIndex++;
        }
        GuiButtonNop button = getButton(0);
        if (button != null) {
            button.setY(topToFactionBottom);
            button.render(graphics, mouseX, mouseY, partialTicks);
        }
        button = getButton(1);
        if (button != null) {
            button.setY(topToFactionBottom);
            button.render(graphics, mouseX, mouseY, partialTicks);
        }

        Component reject = Component.translatable("questgui.reject");
        graphics.drawString(font, reject, 759 - ClientProxy.Font.width(reject) / 2, topToFactionBottom + 6, -1);
        Component accept = Component.translatable("questgui.accept");
        graphics.drawString(font, accept, 853 - ClientProxy.Font.width(accept) / 2, topToFactionBottom + 6, -1);
        matrixStack.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return super.mouseClicked(mouseX / wScale, mouseY / wScale, mouseButton);
    }

    @Override
    public void save() {
        grabMouse(false);
        Packets.sendServer(new SPacketQuestCompletionCheckAll());
    }

    @Override
    public void setClose(CompoundTag data) { grabMouse(false); }

    public void drawLine(GuiGraphics graphics, int x, int y, int width) {
        if (npc.display.getLineColors() == null || npc.display.getLineColors().length != 3) {
            npc.display.setLineColors(0xFF8D3800, 0xFFFEA53B, 0xFFAE5301);
        }
        graphics.fill(x, y, width, y + 1, npc.display.getLineColors()[0]);
        graphics.fill(x, y + 1, width, y + 2, npc.display.getLineColors()[1]);
        graphics.fill(x, y + 2, width, y + 3, npc.display.getLineColors()[2]);
    }

    public void drawTextBlock(GuiGraphics stack, String text, int x, int y, int width, int color) {
        TextBlockClient block = new TextBlockClient(null, text, width, -1, null, player, npc);
        int count = 0;
        for (Component line : block.lines) {
            int height = y + count * ClientProxy.Font.height(null);
            stack.drawCenteredString(font, line, x + width / 2, height, color);
            ++count;
        }
    }

    public void drawLeftAllignedTextBlock(GuiGraphics stack, String text, int x, int y, int width, int color) {
        TextBlockClient block = new TextBlockClient(null, text, width, -1, null, player, npc);
        int count = 0;
        for (Component line : block.lines) {
            int height = y + count * ClientProxy.Font.height(null);
            stack.drawString(font, line, x, height, color);
            ++count;
        }
    }

    public int getLineCount(String text, int width) {
        TextBlockClient block = new TextBlockClient(null, text, width, -1, null, player, npc);
        return block.lines.size();
    }

    public void grabMouse(boolean grab) {
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        if (grab && !isGrabbed) {
            IMouseHandlerMixin mouse = (IMouseHandlerMixin) minecraft.mouseHandler;
            mouse.setGrabbed(false);
            mouse.setX(0.0D);
            mouse.setY(0.0D);
            InputConstants.grabOrReleaseMouse(minecraft.getWindow().getWindow(), 212995, 0.0D, 0.0D);
            isGrabbed = true;
        }
        else if (!grab && isGrabbed) {
            minecraft.mouseHandler.releaseMouse();
            isGrabbed = false;
        }
    }

}

