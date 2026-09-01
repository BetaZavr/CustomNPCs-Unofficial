package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.TextBlockClient;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketQuestCompletionCheck;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.listeners.ITopButtonListener;

import javax.annotation.Nonnull;

public class GuiQuestCompletion extends GuiNPCInterface implements ITopButtonListener {

   protected static final ResourceLocation resource = getResource("smallbg.png");
   protected final Quest quest;

   // New from Unofficial (BetaZavr)
   protected static final ResourceLocation bookGuiTextures = new ResourceLocation("textures/gui/book.png");
   protected TextBlockClient textBlockClient;
   protected int maxLine;
   protected int currentPage = 0;
   protected int hover;

   public GuiQuestCompletion(@Nonnull Quest questIn) {
      super();
      imageWidth = 176;
      imageHeight = 222;
      drawDefaultBackground = false;
      closeOnEsc = false;

      quest = questIn;
   }

   @Override
   public void init() {
      super.init();
      addLabel(0, guiLeft + 4, guiTop + 4, Component.translatable("questlog.completed")
              .append(" ")
              .append(Component.translatable(quest.getName())))
              .setSize(imageWidth - 8, 10);
      textBlockClient = new TextBlockClient(Component.translatable(quest.getCompleteText()).getString(), 170, true, npc, player);
      maxLine = 180 / font.lineHeight;
      addButton(0, guiLeft + 28 + (textBlockClient.lines.size() > maxLine ? 0 : 20), guiTop + imageHeight - 20, "quest.complete")
              .setSize(80, 16);
   }

   @Override
   public void buttonEvent(GuiButtonNop guiButton) {
      if (guiButton.id == 0) {
         Packets.sendServer(new SPacketQuestCompletionCheck(quest.getId(), ItemStack.EMPTY));
         onClose();
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      renderBackground(graphics);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      graphics.blit(resource, guiLeft, guiTop, 0, 0, imageWidth, imageHeight);
      graphics.hLine(guiLeft + 4, guiLeft + 170, guiTop + 13, CustomNpcResourceListener.DefaultTextColor | 0xFF000000);
      drawQuestText(graphics);
      hover = -1;
      if (textBlockClient.lines.size() * font.lineHeight > maxLine) {
         String page = (currentPage + 1) + "/" + ((int) Math.ceil((double) textBlockClient.lines.size() / (double) maxLine));
         graphics.drawString(font, page, guiLeft + 150 - font.width(page), guiTop + imageHeight - 20, CustomNpcResourceListener.DefaultTextColor);
         PoseStack matrixStack = graphics.pose();
         if (currentPage > 0) {
            matrixStack.pushPose();
            matrixStack.translate(guiLeft + 6, guiTop + imageHeight - 20, 0.0f);
            if (isMouseHover(mouseX, mouseY, guiLeft + 6, guiTop + imageHeight - 20, 18, 10)) { hover = 0; }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(bookGuiTextures, 0, 0, hover == 0 ? 26 : 3, 207, 18, 10);
            matrixStack.popPose();
         }
         if ((currentPage + 1) * maxLine < textBlockClient.lines.size()) {
            matrixStack.pushPose();
            matrixStack.translate(guiLeft + imageWidth - 24, guiTop + imageHeight - 20, 0.0f);
            if (isMouseHover(mouseX, mouseY, guiLeft + imageWidth - 24, guiTop + imageHeight - 20, 18, 10)) { hover = 1; }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(bookGuiTextures, 0, 0, hover == 1 ? 26 : 3, 194, 18, 10);
            matrixStack.popPose();
         }
      }
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

   // New from Unofficial (BetaZavr)
   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (!hasSubGui() && hover != -1) {
         if (hover == 1) { currentPage++; }
         else { currentPage--; }
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   private void drawQuestText(GuiGraphics graphics) {
      for (int i = currentPage * maxLine, j = 0; j < maxLine && i < textBlockClient.lines.size(); ++i, ++j) {
         graphics.drawString(font, textBlockClient.lines.get(i),
                 guiLeft + 4, guiTop + 16 + j * font.lineHeight, CustomNpcResourceListener.DefaultTextColor);
      }
   }

}
