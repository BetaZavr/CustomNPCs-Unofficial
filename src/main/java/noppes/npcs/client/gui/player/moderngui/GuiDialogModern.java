package noppes.npcs.client.gui.player.moderngui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.OptionType;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.TextBlockClient;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.client.IMouseHandlerMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketDialogSelected;
import noppes.npcs.packets.server.SPacketQuestCompletionCheckAll;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.listeners.IGuiClose;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

public class GuiDialogModern
        extends GuiNPCInterface
        implements IGuiClose {

    public static final ResourceLocation DECOMPOSED = new ResourceLocation(CustomNpcs.MODID, "textures/gui/dialog_menu_decomposed.png");

    private List<Integer> options = new ArrayList<>();
    private boolean isGrabbed = false;
    private int selected = -1;
    private Dialog dialog;
    // Display
    protected final EntityNPCInterface dialogNpc;
    protected float wScale = 1.0F;
    protected float hScale = 1.0F;

    public GuiDialogModern(EntityNPCInterface npc, Dialog dialogIn) {
        super(npc);
        imageHeight = 238;

        dialog = dialogIn;
        appendDialog(dialog);

        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        if (npc instanceof EntityDialogNpc) { dialogNpc = null; }
        else { dialogNpc = Util.instance.copyToGUI(npc, minecraft.level, false); }
    }

    @Override
    public void init() {
        super.init();
        isGrabbed = false;
        grabMouse(dialog.showWheel);
        guiTop = height - imageHeight;

        wScale = (float) width / 960.0F;
        hScale = (float) height / 509.0F;
    }

    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics) {
        graphics.fillGradient(0, 0, width, height, 0x66000000, 0x66000000);
        if (!dialog.hideNPC && dialogNpc != null) {
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
        int textBlockWidth = (int)(700.0 * wScale);
        int lineCount = getLineCount(dialog.text, textBlockWidth);
        int gap = Math.max(16, Math.min((int)(2.6f * (float)lineCount), 32));
        int textPartHeight = 26 + lineCount * ClientProxy.Font.height(null) + 2 * gap;

        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(0.0, 0.5, 2000.0D);
        graphics.fillGradient(0, height - textPartHeight, width, height, 0x99000000, 0x99000000);
        drawLine(graphics, 23, height - textPartHeight + 23, width - 23);

        matrixStack.scale(1.5f, 1.5f, 1.0f);
        graphics.drawString(font, npc.getDisplayName(), 31, (int)((double)(height - textPartHeight + 5) / 1.5), -1);
        matrixStack.scale(0.6666667f, 0.6666667f, 1.0f);
        drawTextBlock(graphics, dialog.text, (width - textBlockWidth) / 2, height - textPartHeight + 23 + 3 + gap, textBlockWidth);
        selected = -1;
        matrixStack.scale(wScale, wScale, wScale);

        int accumulatedHeight = 0;
        for (int i = 0; i < options.size(); ++i) {
            int optionNum = options.get(i);
            DialogOption option = dialog.options.get(optionNum);
            int optionHeight = 220 + accumulatedHeight;
            String[] titleLines = option.title.split("\\\\n");
            int optionBackHeight = titleLines.length * 9 + (titleLines.length + 1) * 2;
            if ((double)mouseX >= 723.0 * wScale && (double)mouseX <= 946.0 * wScale && (double)mouseY >= (double)optionHeight * wScale && (double)mouseY <= (double)(optionHeight + 13) * wScale) {
                selected = i;
            }
            RenderSystem.enableBlend();
            graphics.blit(DECOMPOSED, 723, optionHeight, 0.0f, i == selected ? (float)optionBackHeight : 0.0f, 223, optionBackHeight, 256, (int)(256.0f * (float)optionBackHeight / 13.0f));
            RenderSystem.disableBlend();
            if (getQuestByOptionId(optionNum) != null) {
                graphics.drawString(font, "!", 727, optionHeight + 3, 7792731);
            } else {
                graphics.drawString(font, ">", 727, optionHeight + 3, -1);
            }
            int lineOffset = 0;
            for (String line : titleLines) {
                if (line.isEmpty()) continue;
                graphics.drawString(font, line, 735, optionHeight + 3 + lineOffset, option.optionColor);
                lineOffset += 12;
            }
            accumulatedHeight += 19 + lineOffset;
        }
        matrixStack.popPose();
    }

    @Override
    public boolean keyPressed(int key, int key_1, int key_2) {
        if (GuiBasic.isEnterKey(key) && (selected == -1 && options.isEmpty() || selected >= 0)) {
            handleDialogSelection();
        }
        if (closeOnEsc && (isEscKey(key) || isInventoryKey(key))) {
            Packets.sendServer(new SPacketDialogSelected(dialog.id, -1));
            onClose();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if ((selected == -1 && options.isEmpty() || selected >= 0) && mouseButton == 0) { handleDialogSelection(); }
        return super.mouseClicked(mouseX / wScale, mouseY / wScale, mouseButton);
    }

    @Override
    public void setClose(CompoundTag data) { grabMouse(false); }

    @Override
    public void save() {
        grabMouse(false);
        Packets.sendServer(new SPacketQuestCompletionCheckAll());
    }

    public Quest getQuestByOptionId(int id) {
        DialogOption option = dialog.options.get(id);
        if (option != null) {
            Dialog d = option.getDialog(player);
            if (d != null && d.hasQuest()) { return d.getQuest(); }
        }
        return null;
    }

    public void drawLine(GuiGraphics graphics, int x, int y, int width) {
        if (npc.display.getLineColors() == null || npc.display.getLineColors().length != 3) {
            npc.display.setLineColors(0xFF8D3800, 0xFFFEA53B, 0xFFAE5301);
        }
        graphics.fill(x, y, width, y + 1, npc.display.getLineColors()[0]);
        graphics.fill(x, y + 1, width, y + 2, npc.display.getLineColors()[1]);
        graphics.fill(x, y + 2, width, y + 3, npc.display.getLineColors()[2]);
    }

    private void handleDialogSelection() {
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        int optionId = -1;
        if (dialog.showWheel) { optionId = selected; }
        else if (!options.isEmpty()) { optionId = options.get(selected); }
        if (getQuestByOptionId(optionId) == null) { Packets.sendServer(new SPacketDialogSelected(dialog.id, optionId)); }
        else { minecraft.setScreen(new GuiQuestModern(npc, getQuestByOptionId(optionId), dialog, optionId)); }
        if (dialog != null && !dialog.notHasOtherOptions() && !options.isEmpty()) {
            DialogOption option = dialog.options.get(optionId);
            if (option != null && option.optionType == OptionType.DIALOG_OPTION) { NoppesUtil.clickSound(); }
            else if (closeOnEsc) { onClose(); }
        }
        else if (closeOnEsc) { onClose(); }
    }

    public void drawTextBlock(GuiGraphics graphics, String text, int x, int y, int width) {
        TextBlockClient block = new TextBlockClient(null, text, width, -1, null, player, npc);
        int count = 0;
        for (Component line : block.lines) {
            int height = y + count * ClientProxy.Font.height(null);
            graphics.drawCenteredString(font, line, x + width / 2, height, -1);
            ++count;
        }
    }

    public int getLineCount(String text, int width) {
        TextBlockClient block = new TextBlockClient(null, text, width, -1, null, player, npc);
        return block.lines.size();
    }

    public void appendDialog(Dialog dialogIn) {
        closeOnEsc = !dialog.disableEsc;
        dialog = dialogIn;
        options = new ArrayList<>();
        MusicController.Instance.stopSound(null, SoundSource.VOICE);
        if (dialog.sound != null) {
            CustomNPCsScheduler.runTack(() ->
                    MusicController.Instance.playSoundDialog(SoundSource.VOICE, dialog.sound, npc.blockPosition(), 1.0F, 1.0F),
                    50);
        }
        for (int slot : dialog.options.keySet()) {
            DialogOption option = dialog.options.get(slot);
            if (option == null || !option.isAvailable(player)) continue;
            options.add(slot);
        }
        grabMouse(dialog.showWheel);
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

