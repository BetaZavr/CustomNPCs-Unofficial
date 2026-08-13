package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.handler.data.IDeal;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.GuiTooltipUtils;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.containers.ContainerNPCTrader;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.data.*;
import noppes.npcs.mixin.client.IMouseHandlerMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextChangeListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.*;

// Changed from Unofficial (BetaZavr)
@OnlyIn(Dist.CLIENT)
public class GuiNPCTrader extends GuiContainerNPCInterface<ContainerNPCTrader>
        implements IGuiData, ITextfieldListener, ITextChangeListener {

   public static final ResourceLocation BUTTONS = new ResourceLocation(CustomNpcs.MODID, "textures/gui/trade/buttons.png");
   public static final ResourceLocation HOVERS = new ResourceLocation(CustomNpcs.MODID, "textures/gui/trade/hovers.png");
   public static final ResourceLocation INV = new ResourceLocation(CustomNpcs.MODID, "textures/gui/trade/player_inventory.png");
   public static final ResourceLocation SCROLL = new ResourceLocation(CustomNpcs.MODID, "textures/gui/trade/scroll.png");
   public static final ResourceLocation ICONS = new ResourceLocation(CustomNpcs.MODID, "textures/gui/trade/sections.png");
   public static Marcet marcet;
   protected static Component search = Component.empty();
   protected static boolean isIdSort = true;
   protected static int section = -1;
   protected static final Comparator<Deal> comparator = (t1, t2) -> {
      if (isIdSort) {
         Map<Integer, Integer> indexMap = new HashMap<>();
         int i = 0;
         for (IDeal iDeal : GuiNPCTrader.marcet.getDeals(GuiNPCTrader.section)) { indexMap.put(iDeal.getId(), i++); }
         return Integer.compare(indexMap.getOrDefault(t1.getId(), Integer.MAX_VALUE), indexMap.getOrDefault(t2.getId(), Integer.MAX_VALUE));
      }
      else { return t1.getName().compareToIgnoreCase(t2.getName()); }
   };

   protected final Map<Integer, Deal> data = new LinkedHashMap<>();
   protected DealMarkup selectDealData;

   protected List<Integer> canBuy = new ArrayList<>();
   protected List<Integer> canSell = new ArrayList<>();
   protected int count = 1;

   // display
   protected boolean wait = false;
   protected int invPosX;
   protected int invPosY;
   // scroll
   protected boolean isScrolled;
   protected int scrollWidth;
   protected int scrollHMax;
   protected int scrollBMax;
   protected int scrollHeight;
   protected int scrollBHeight;
   protected int scrollY;
   protected int scrollMaxY;
   // hovers
   protected List<Component> hovers = new ArrayList<>();
   protected boolean isHovered;
   protected int hoverHeightMax;
   protected int hoverHMax;
   protected int hoverBMax;
   protected int hoverHeight;
   protected int hoverBHeight;
   protected int hoverY;
   protected int hoverMaxY;
   // model rotate
   protected Map<String, ResourceLocation> materialTextures = new HashMap<>();
   protected ParameterizedModel CHEST_FULL;
   protected float rotateX = 0.0f;
   protected float rotateZ = 0.0f;

   // Tabs
   protected int ceilHeight = 0;
   protected int ceilList = -1;

   public GuiNPCTrader(ContainerNPCTrader container, Inventory inv, Component titleIn) {
      super(NoppesUtilServer.getEditingNpc(Minecraft.getInstance().player), container, inv, titleIn);
      drawDefaultBackground = true;
      closeOnEsc = true;
      hoverIsGame = true;

      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      imageWidth = minecraft.getWindow().getGuiScaledWidth();
      imageHeight = minecraft.getWindow().getGuiScaledHeight();
      marcet = container.marcet;
   }

   @Override
   public void buttonEvent(@Nonnull GuiButtonNop button) {
      if (button instanceof TradeButtonBiDirectional) {
         count = button.getValue() + 1;
         init();
         return;
      }
      if (button instanceof SectionButton) {
         int s = button.id - 20 + ceilList * ceilHeight;
         if (button.id >= 20 && s != section) {
            section = s;
            selectDealData = null;
            scrollY = 0;
            hoverY = 0;
            rotateX = 0.0f;
            rotateZ = 0.0f;
            count = 1;
            init();
         }
         return;
      }
      if (button instanceof TradeButton tradeB) {
         if ((tradeB.deal.getAmount() > 0 || player.isCreative()) && (selectDealData == null || selectDealData.deal.getId() != tradeB.deal.getId())) {
            selectDealData = tradeB.dm;
            hoverY = 0;
            rotateX = 0.0f;
            rotateZ = 0.0f;
            count = 1;
            init();
         }
         return;
      } // select deal
      switch (button.id) {
         case 0: Packets.sendServer(new SPacketTraderMarketBuy(marcet.getId(), selectDealData.deal.getId(), npc == null ? -1 : npc.getId(), count)); break; // buy
         case 1: Packets.sendServer(new SPacketTraderMarketSell(marcet.getId(), selectDealData.deal.getId(), npc == null ? -1 : npc.getId(), count)); break; // Sell
         case 2: Packets.sendServer(new SPacketTraderMarketReset(marcet.getId())); break; // Reset
         case 3: {
            if (ceilList <= 0) { return; }
            ceilList--;
            init();
            return;
         } // up
         case 4: {
            if (ceilList >= Math.floor((double) marcet.sections.size() / 5.0d)) { return; }
            ceilList++;
            init();
            return;
         } // down
         case 11: {
            isIdSort = ((GuiCheckBoxNop) button).selected();
            init();
            return;
         } // sort type
      }
      wait = true;
      init();
   }

   @Override
   public void renderBg(@Nonnull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
      super.renderBg(graphics, partialTicks, mouseX, mouseY);
      RenderSystem.enableBlend();
      PoseStack matrixStack = graphics.pose();
      int w;
      int h = (250 - scrollHeight) / 2;
      String text;
      // update / money pos
      graphics.blit(INV, invPosX, 0, 0, 142, 178, 24);
      // player inventory:
      graphics.blit(INV, invPosX, invPosY, 0, 0, 178, 118);
      // Scroll
      int s = 0;
      int v;
      for (int i = 0; i < scrollHMax; i++) {
         if (i == 0) { v = 0; } else { v = 3 + h; }
         graphics.blit(SCROLL, 0, i * scrollHeight, 0, v, scrollWidth, scrollHeight); // left
         graphics.blit(SCROLL, scrollWidth, i * scrollHeight, 214 - scrollWidth, v, scrollWidth, scrollHeight); // right
         s += scrollHeight;
      }
      // end
      h = imageHeight - s;
      graphics.blit(SCROLL, 0, imageHeight - h, 0, 256 - h, scrollWidth, h); // left
      graphics.blit(SCROLL, scrollWidth, imageHeight - h, 214 - scrollWidth, 256 - h, scrollWidth, h); // right
      // bar
      if (scrollMaxY > 0) {
         matrixStack.pushPose();
         matrixStack.translate(scrollWidth * 2 - 14, 4.0f, 0.0f);
         matrixStack.scale(0.5f, 0.5f, 1.0f);
         graphics.blit(SCROLL, 0, 0, 236, 0, 20, 20);
         graphics.blit(SCROLL, 0, (imageHeight - 61) * 2, 236, 236, 20, 20);
         matrixStack.scale(1.0f, 2.0f, 1.0f);
         s = 10;
         h = (216 - scrollBHeight) / 2;
         for (int i = 0; i < scrollBMax; i++) {
            if (i == 0) { v = 20; } else { v = h; }
            graphics.blit(SCROLL, 0, 10 + i * scrollBHeight, 236, v, 20, scrollBHeight); // bar
            s += scrollBHeight;
         }
         h = 28;
         graphics.blit(SCROLL, 0, s, 236, 236 - scrollHeight, 20, scrollHeight - h); // bar
         matrixStack.popPose();
      }
      // place of sale
      graphics.blit(BUTTONS, 4, imageHeight - 46, 0, 214, scrollWidth - 4, 42);
      graphics.blit(BUTTONS, scrollWidth, imageHeight - 46, 260 - scrollWidth, 214, scrollWidth - 4, 42);
      // hover deal
      if (selectDealData != null && selectDealData.deal != null) {
         int x = imageWidth - 155;
         int y = 24;
         graphics.blit(HOVERS, x, y, 0, 0, 132, 50);
         if (hoverHeightMax >= 24) {
            y += 50;
            s = 0;
            for (int i = 0; i < hoverHMax; i++) {
               if (i == 0) { v = 50; } else { v = 53; }
               graphics.blit(HOVERS, x, y + i * hoverHeight, 0, v, 132, hoverHeight);
               s += hoverHeight;
            }
            // end
            h = hoverHeightMax - s;
            graphics.blit(HOVERS, x, invPosY - 24 - h, 0, 256 - h, 132, h);
            // bar
            if (hoverMaxY > 0) {
               matrixStack.pushPose();
               matrixStack.translate(x + 120.0f, y + 2, 0.0f);
               matrixStack.scale(0.5f, 0.5f, 1.0f);
               graphics.blit(SCROLL, 0, 0, 236, 0, 20, 20);
               graphics.blit(SCROLL, 0, (hoverHeightMax - 14) * 2, 236, 236, 20, 20);
               if (hoverHeightMax > 24) {
                  matrixStack.scale(1.0f, 2.0f, 1.0f);
                  s = 10;
                  h = (216 - hoverBHeight) / 2;
                  for (int i = 0; i < hoverBMax; i++) {
                     if (i == 0) { v = 20; } else { v = h; }
                     graphics.blit(SCROLL, 0, 10 + i * hoverBHeight, 236, v, 20, hoverBHeight); // bar
                     s += hoverBHeight;
                  }
                  h = hoverHeightMax % 2 != 0 ? 9 : 10;
                  graphics.blit(SCROLL, 0, s, 236, 236 - hoverHeight, 20, hoverHeight - h); // bar
               }
               matrixStack.popPose();
            }
         }
      }
      // Market level
      PlayerData data = PlayerData.get(player);
      if (marcet.showXP) {
         graphics.blit(INV, imageWidth - 100, invPosY - 24, 0, 118, 100, 24);
         MarkupData md = data.game.getMarkupData(marcet.getId());
         MarkupData mm = marcet.markup.get(md.level);
         if (md.xp > 0) {
            float f0 = 0.0f;
            if (md.xp >= mm.xp) { s = 96; }
            else {
               float f1 = (float) md.xp / (float) mm.xp;
               s = (int) (96.0d * f1);
               f0 = 1.0f - f1;
            }
            RenderSystem.setShaderColor(f0, 1.0f, 0.0f, 1.0f);
            graphics.blit(INV, imageWidth - 2 - s, imageHeight - 142, 198 - s, 118, s, 24);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
         }
         String lv = "enchantment.level." + (md.level + 1);
         if (!Component.translatable(lv).getString().equals(lv)) { lv = Component.translatable(lv).getString(); }
         else { lv = "" + (md.level + 1); }
         graphics.drawString(font, lv, imageWidth - 6 - (float) font.width(lv), imageHeight - 131, CustomNpcs.MainColor.getRGB(), true);
         if (isMouseHover(mouseX, mouseY, imageWidth - 100, imageHeight - 142, 100, 24)) {
            setHoverText(Component.translatable("market.hover.you.level", "" + (md.level + 1),
                    "" + Math.min(md.xp, mm.xp), "" + mm.xp,
                    (mm.buy <= 0.0f ? ChatFormatting.GREEN: ChatFormatting.RED) + "" + (int) (mm.buy * 100.0f),
                    (mm.sell < 0.0f ? ChatFormatting.RED: ChatFormatting.GREEN) + "" + (int) (mm.sell * 100.0f)));
         } // hover market xp
      }

      // name
      if (marcet.getName().isEmpty()) { text = Util.instance.getOldFormattedText(Component.translatable("role.trader")); }
      else { text = Util.instance.getOldFormattedText(Component.translatable(marcet.getName())); }
      w = ClientProxy.Font.width(text) / 2;
      ClientProxy.Font.draw(graphics, text, scrollWidth - w + 13, 2, CustomNpcs.MainColor.getRGB());
      // update
      if (marcet.updateTime > 0) {
         ChatFormatting color = ChatFormatting.RESET;
         if (marcet.nextTime <= 60000 && marcet.nextTime % 1000 < 500) { color = ChatFormatting.GOLD; }
         else if (marcet.nextTime <= 10000) { color = marcet.nextTime % 1000 < 500 ? ChatFormatting.GOLD : ChatFormatting.RED; }
         text = Util.instance.getOldFormattedText(Component.translatable("market.uptime", color + Util.instance.ticksToElapsedTime(marcet.nextTime / 50, false, false, false)));
         w = ClientProxy.Font.width(text);
         ClientProxy.Font.draw(graphics, text, invPosX + 3, 2, CustomNpcs.MainColor.getRGB());
         if (marcet.nextTime <= 0) { Packets.sendServerDelayed(new SPacketMarketTime(marcet.getId()), this, 2500); }
         if (isMouseHover(mouseX, mouseY, invPosX, 0, w, 24)) {
            setHoverText("market.hover.update");
         } // hover update time
      }
      // marcet money
      text = Util.instance.getTextReducedNumber(marcet.money, true, true, false) + CustomNpcs.displayCurrencies;
      w = ClientProxy.Font.width(text);
      ClientProxy.Font.draw(graphics, text, imageWidth - w - 15, 2, CustomNpcs.MainColor.getRGB());
      if (isMouseHover(mouseX, mouseY, imageWidth - w - 14, 0, w, 24)) {
         setHoverText(Component.translatable("market.hover.currency.1", marcet.money, CustomNpcs.displayCurrencies));
      }
      matrixStack.pushPose();
      matrixStack.translate(imageWidth - 15, 0, 1.0f);
      matrixStack.scale(0.0625f, 0.0625f, 0.0625f);
      graphics.blit(GuiBasic.MONEY, 0, 0, 0, 0, 256, 256);
      matrixStack.popPose();
      // money
      int x = invPosX + 4;
      int y = invPosY + 9;
      text = Util.instance.getOldFormattedText(Component.translatable("questlog.rewardmoney", data.game.getTextMoney(), CustomNpcs.displayCurrencies));
      ClientProxy.Font.draw(graphics, text, x, y, CustomNpcs.MainColor.getRGB());
      w = ClientProxy.Font.width(text);
      matrixStack.pushPose();
      matrixStack.translate(x + w, y - 2.0f, 1.0f);
      matrixStack.scale(0.0625f, 0.0625f, 0.0625f);
      graphics.blit(GuiBasic.MONEY, 0, 0, 0, 0, 256, 256);
      matrixStack.popPose();
      if (isMouseHover(mouseX, mouseY, x, y, w + 16, 16)) {
         setHoverText(Component.translatable("inventory.hover.currency").append(" " + data.game.getMoney()));
      } // hover money
      // donat
      text = Util.instance.getOldFormattedText(Component.translatable("questlog.rewarddonat", data.game.getTextDonat(), CustomNpcs.displayDonation));
      w = ClientProxy.Font.width(text);
      x = imageWidth - 18 - w;
      ClientProxy.Font.draw(graphics, text, x, y, CustomNpcs.MainColor.getRGB());
      matrixStack.pushPose();
      matrixStack.translate(x + w, y - 2.0f, 1.0f);
      matrixStack.scale(0.0625f, 0.0625f, 0.0625f);
      graphics.blit(GuiBasic.DONAT, 0, 0, 0, 0, 256, 256);
      matrixStack.popPose();
      if (isMouseHover(mouseX, mouseY, x, y, w + 16, 16)) {
         setHoverText(Component.translatable("inventory.hover.donat").append(" " + data.game.getDonat()));
      } // hover donat
      // search icon
      matrixStack.pushPose();
      matrixStack.translate(5.0f, imageHeight - 25.0f, 0.0f);
      matrixStack.scale(0.833333f, 0.833333f, 0.833333f);
      graphics.blit(ICONS, 0, 0, 0, 216, 24, 24);
      matrixStack.popPose();
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (marcet == null) { onClose(); return; }
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      super.render(graphics, mouseX, mouseY, partialTicks);
      PoseStack matrixStack = graphics.pose();
      if (!hovers.isEmpty()) {
         int i = 0;
         int x = imageWidth - 152;
         int y;
         matrixStack.pushPose();
         graphics.enableScissor(x, 77, x + 128, 72 + hoverHeightMax);
         for (Component hover : hovers) {
            y = 77 + hoverY + i * (minecraft.font.lineHeight + 1);
            if (y >= 77 - minecraft.font.lineHeight && y < 71 + hoverHeightMax) { graphics.drawString(minecraft.font, hover, x, y, CustomNpcs.MainColor.getRGB()); }
            if (y >= 71 + hoverHeightMax) { break; }
            i++;
         }
         graphics.disableScissor();
         matrixStack.popPose();
      }
      RenderSystem.enableBlend();
      if (scrollMaxY != 0) {
         float f0 = (float) -scrollY / (float) scrollMaxY * (float) (imageHeight - 81);
         matrixStack.pushPose();
         matrixStack.translate(scrollWidth * 2.0f - 14.5f, 4.0f + f0, 0.0f);
         matrixStack.scale(0.5f, 0.5f, 0.5f);
         graphics.blit(SCROLL, 0, 0, 214, 0, 22, 60);
         matrixStack.popPose();
      }
      if (hoverMaxY != 0) {
         //isHovered = isMouseHover(mouseX, mouseY, imageWidth - 35, 76, 10, hoverHeightMax - 4);
         float f0 = (float) -hoverY / (float) hoverMaxY * (float) (hoverHeightMax - 20);
         matrixStack.pushPose();
         matrixStack.translate(imageWidth - 35.0f, 76.0f + f0, 0.0f);
         matrixStack.scale(0.5f, 0.5f, 0.5f);
         graphics.blit(SCROLL, 0, 0, 214, 0, 22, 16);
         graphics.blit(SCROLL, 0, 16, 214, 44, 22, 16);
         matrixStack.popPose();
      }
      if (selectDealData != null && selectDealData.deal != null) {
         matrixStack.pushPose();
         matrixStack.translate(imageWidth - 89.0f, 49.0f, 50.0f);
         if (rotateX == 0 && rotateZ == 0 && minecraft.level != null) {
            matrixStack.mulPose(Axis.XP.rotationDegrees(-30.0f));
            matrixStack.mulPose(Axis.YP.rotationDegrees((float) (System.currentTimeMillis() % 36000) / -20.0f));
         }
         else {
            matrixStack.mulPose(Axis.XP.rotationDegrees(-30.0f + rotateX));
            matrixStack.mulPose(Axis.ZP.rotationDegrees(rotateZ));
         }
         if (selectDealData.deal.isCase()) {
            matrixStack.translate(-16.0f, -8.0f, 0.0f);
            matrixStack.scale(32.0f, -32.0f, 32.0f);
            ModelBuffer.render(CHEST_FULL, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
         }
         else {
            if (!selectDealData.deal.getProduct().isEmpty()) {
               ItemStack stack = selectDealData.deal.getProduct().getMCItemStack();
               matrixStack.mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
               matrixStack.scale(32.0f, 32.0f, 32.0f);
               BakedModel bakedmodel = minecraft.getItemRenderer().getModel(stack, minecraft.level, player, 0);
               minecraft.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, matrixStack, graphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedmodel);
            }
         }
         matrixStack.popPose();
      }
   }

   @Override
   public void init() {
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      // window
      if (imageWidth != minecraft.getWindow().getGuiScaledWidth() || imageHeight != minecraft.getWindow().getGuiScaledHeight()) {
         scrollY = 0;
         hoverY = 0;
      }
      imageWidth = minecraft.getWindow().getGuiScaledWidth();
      imageHeight = minecraft.getWindow().getGuiScaledHeight();
      menu.reset(imageWidth, imageHeight);
      boolean focus = getTextField(0) != null && getTextField(0).isFocused();
      super.init();
      invPosX = imageWidth - 178;
      invPosY = imageHeight - 118;
      scrollWidth = ValueUtil.correctInt(214, 0, imageWidth - 202) / 2;
      if (imageHeight <= 512) {
         scrollHMax = 1;
         scrollBMax = 1;
         scrollHeight = imageHeight / 2;
         scrollBHeight = (imageHeight - 85) / 2;
      }
      else {
         scrollHMax = (int) Math.ceil(((float) imageHeight - 10.0f) / 216.0f);
         scrollHeight = (int) Math.ceil(((float) imageHeight - 6.0f) / (float) scrollHMax);
         scrollHMax--;
         scrollBMax = (int) Math.ceil(((float) imageHeight - 85.0f) / 200.0f);
         scrollBHeight = (int) Math.ceil(((float) imageHeight - 85) / (float) scrollHMax);
         scrollBMax--;
      }
      hoverHeightMax = invPosY - 98;
      if (hoverHeightMax <= 206) {
         hoverHMax = 1;
         hoverBMax = 1;
         hoverHeight = (hoverHeightMax - 4) / 2;
         hoverBHeight = (hoverHeightMax - 24) / 2;
      }
      else {
         hoverHMax = (int) Math.ceil(((float) hoverHeightMax - 4.0f) / 216.0f);
         hoverHeight = (int) Math.ceil(((float) hoverHeightMax - 4.0f) / (float) hoverHMax);
         hoverHMax--;
         hoverBMax = (int) Math.ceil(((float) hoverHeightMax - 24.0f) / 200.0f);
         hoverBHeight = (int) Math.ceil(((float) hoverHeightMax - 24.0f) / (float) hoverHMax);
         hoverBMax--;
      }
      ceilHeight = (int) Math.floor(((float) imageHeight - 36.0f) / 24.0f);
      // gm buttons
      addButton(2, invPosX, invPosY - 22, "remote.reset")
              .setSize(76, 20)
              .setIsVisible(player.isCreative())
              .setHoverTexts("market.hover.reset");
      // section tabs
      SectionButton tab;
      if (ceilList < 0) {
         ceilList = 0;
         section = 0;
      }
      if (marcet.sections.size() > 1) {
         int offsetY = 4;
         if (marcet.sections.size() > ceilHeight) {
            if (ceilList > 0 && section != ceilList) {
               add(new SectionButton(this, 3, null, scrollWidth * 2 + 3, imageHeight - 16));
            } // down | next
            if (ceilList < Math.floor((double) marcet.sections.size() / (double) ceilHeight)) {
               add(new SectionButton(this, 4, null, scrollWidth * 2 + 3, 7));
            } // up | back
            offsetY += 14;
         }
         int id;
         for (int i = 0; i < ceilHeight && (i + ceilList * ceilHeight) < marcet.sections.size(); i++) {
            id = i + ceilList * ceilHeight;
            add(tab = new SectionButton(this, 20 + i, marcet.sections.get(id), scrollWidth * 2 - 1, offsetY + i * 24)
                    .setHoverTexts(Component.empty()
                    .append(Component.translatable("market.hover.section").withStyle(ChatFormatting.GRAY))
                    .append("<br>").append(marcet.sections.get(id).getName())));
            if (i + ceilList * ceilHeight == section) { tab.active = true; }
         }
      }
      // section deals
      int level = PlayerData.get(player).game.getMarcetLevel(marcet.getId());
      List<Deal> dealInTrade = new ArrayList<>();
      List<Deal> caseInTrade = new ArrayList<>();
      List<Deal> dealNotTrade = new ArrayList<>();
      List<Deal> caseNotTrade = new ArrayList<>();
      MarcetController mData = MarcetController.getInstance();
      MarcetSection ms = marcet.sections.get(section);
      String s = search.getString().toLowerCase();
      if (ms != null && !ms.deals.isEmpty()) {
         for (Deal deal : ms.deals) {
            if (!s.isEmpty() && !deal.getName().toLowerCase().contains(s)) { continue; }
            if (deal.getMaxCount() != 0 && deal.getAmount() == 0) {
               if (deal.isCase()) { caseNotTrade.add(deal); }
               else { dealNotTrade.add(deal); }
            }
            else {
               if (deal.isCase()) { caseInTrade.add(deal); }
               else { dealInTrade.add(deal); }
            }
         }
      }
      dealInTrade.sort(comparator);
      caseInTrade.sort(comparator);
      dealNotTrade.sort(comparator);
      caseNotTrade.sort(comparator);
      data.clear();
      for (Deal deal : caseInTrade) { data.put(deal.getId(), deal); }
      for (Deal deal : caseNotTrade) { data.put(deal.getId(), deal); }
      for (Deal deal : dealInTrade) { data.put(deal.getId(), deal); }
      for (Deal deal : dealNotTrade) { data.put(deal.getId(), deal); }
      if (data.isEmpty()) { scrollMaxY = 0; }
      else { scrollMaxY = ValueUtil.correctInt(data.size() * 28 - imageHeight + 62, 0, Integer.MAX_VALUE); }
      int i = 0;
      for (Deal deal : data.values()) {
         add(new TradeButton(this, deal, level, 5, 15 + i * 28, (scrollWidth * 2) - (scrollMaxY == 0 ? 9 : 22), dealInTrade.contains(deal) || caseInTrade.contains(deal)));
         i++;
         if ((selectDealData == null || selectDealData.deal == null) && (player.isCreative() || deal.getMaxCount() > 0 && deal.getAmount() > 0)) { selectDealData = mData.getBuyData(marcet, deal, level, count); }
      }
      if (selectDealData != null && selectDealData.deal != null) {
         selectDealData = mData.getBuyData(marcet, selectDealData.deal, level, count);
         boolean found = false;
         for (Deal deal : data.values()) {
            if (deal.getId() == selectDealData.deal.getId()) {
               found = true;
               break;
            }
         }
         if (found) { selectDealData.check(player.getInventory().items); }
         else {
            selectDealData = null;
            scrollY = 0;
            hoverY = 0;
            rotateX = 0.0f;
            rotateZ = 0.0f;
         }
         hovers.clear();
         List<Component> temp = new ArrayList<>();
         if (selectDealData.deal.isCase()) {
            materialTextures.put("#material", selectDealData.deal.getCaseTexture());
            if (selectDealData.deal.showInCase() || player.isCreative())
            { selectDealData.deal.putHoverCaseItems(temp, minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL); }
            CHEST_FULL = ModelBuffer.getParameterizedModel(selectDealData.deal.getCaseObjModel(), null, materialTextures, true, 0);
         }
         else if (!selectDealData.deal.getProduct().isEmpty()) {
            temp.addAll(selectDealData.deal.getProduct().getMCItemStack().getTooltipLines(player, minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL));
         }
         if (!temp.isEmpty()) {
            String lastColor;
            int w = 116;
            for (Component cpt : temp) {
               String line = Util.instance.getOldFormattedText(cpt);
               if (minecraft.font.width(line) < w) { hovers.add(cpt); }
               else {
                  lastColor = "";
                  StringBuilder l = new StringBuilder();
                  for (int j = 0; j < line.length(); j++) {
                     char c = line.charAt(j);
                     try {
                        if ((int) c == 167) { lastColor = c + "" + line.charAt(j + 1); }
                     }
                     catch (Exception ignored) { }
                     if (minecraft.font.width(l.toString() + c) > w) {
                        hovers.add(Component.literal(l.toString()));
                        l = new StringBuilder(lastColor + c);
                        lastColor = "";
                     }
                     else { l.append(c); }
                  }
                  if (!l.isEmpty()) { hovers.add(Component.literal(l.toString())); }
               }
            }
         }
         if (hovers.isEmpty()) { hoverMaxY = 0; }
         else { hoverMaxY = ValueUtil.correctInt(hovers.size() * (minecraft.font.lineHeight + 1) - hoverHeightMax + 4, 0, Integer.MAX_VALUE); }
      }
      // buy
      int x = scrollWidth;
      int y = imageHeight - 45;
      boolean enableBuy = selectDealData != null && selectDealData.deal != null && selectDealData.deal.getType() != 1;
      GuiButtonNop buyButton = addButton(0, x, y, Component.literal("   ").append(Component.translatable("gui.buy")))
              .setSize(scrollWidth - 5, 20)
              .setTexture(BUTTONS)
              .setUV(0, 144, 128, 20)
              .setIsEnabled(enableBuy);
      buyButton.isSimple = true;
      canBuy.clear();
      if (enableBuy) {
         if (wait || selectDealData.deal.getType() == 1) { canBuy.add(1); }
         if (selectDealData.deal.getAmount() <= 0 && selectDealData.deal.getMaxCount() <= 0) { canBuy.add(6); }
         if (!selectDealData.deal.availability.isAvailable(player)) { canBuy.add(2); }
         PlayerData pd = PlayerData.get(player);
         if (selectDealData.buyMoney > 0 && pd.game.getMoney() < selectDealData.buyMoney) { canBuy.add(3); }
         if (selectDealData.buyDonat > 0 && pd.game.getDonat() < selectDealData.buyDonat) { canBuy.add(7); }
         if (!Util.instance.canRemoveItems(player.getInventory().items, selectDealData.buyItems, selectDealData.ignoreDamage, selectDealData.ignoreNBT)) { canBuy.add(4); }
         if (!selectDealData.deal.isCase() && !Util.instance.canAddItemAfterRemoveItems(player.getInventory().items, selectDealData.main, selectDealData.buyItems, selectDealData.ignoreDamage, selectDealData.ignoreNBT)) { canBuy.add(5); }
         Map<ItemStack, Integer> mainItem = new LinkedHashMap<>();
         mainItem.put(selectDealData.main, selectDealData.count);
         if (marcet.isLimited && !selectDealData.deal.isCase() && !Util.instance.canRemoveItems(marcet.inventory, mainItem, selectDealData.ignoreDamage, selectDealData.ignoreNBT)) { canBuy.add(8); }
         if (buyButton.isActive()) {
            if (!player.isCreative()) { buyButton.setIsEnabled(canBuy.isEmpty()); }
            if (!canBuy.isEmpty()) { buyButton.layerColor = 0xFF800000; }
         }
      }
      // sell
      boolean enableSell = selectDealData != null && selectDealData.deal != null  && selectDealData.deal.getType() != 0;
      GuiButtonNop sellButton = addButton(1, x, y + 20, Component.translatable("gui.sell").append("   "))
              .setSize(scrollWidth - 5, 20)
              .setTexture(BUTTONS)
              .setUV(128, 144, 128, 20)
              .setIsEnabled(enableSell);
      sellButton.isSimple = true;
      canSell.clear();
      if (enableSell) {
         if (wait) { canSell.add(1); }
         if (!selectDealData.deal.availability.isAvailable(player)) { canSell.add(2); }
         Map<ItemStack, Integer> mainItem = new HashMap<>();
         mainItem.put(selectDealData.main, selectDealData.count);
         if (!selectDealData.main.isEmpty()  && !Util.instance.canRemoveItems(player.getInventory().items, mainItem,  selectDealData.ignoreDamage, selectDealData.ignoreNBT)) { canSell.add(3); }
         if (marcet.isLimited) {
            if (selectDealData.sellMoney > marcet.money) { canSell.add(4); }
            if (!selectDealData.sellItems.isEmpty() && !Util.instance.canRemoveItems(marcet.inventory,  selectDealData.sellItems, selectDealData.ignoreDamage, selectDealData.ignoreNBT)) { canSell.add(5); }
         }
         if (selectDealData.deal.getMaxCount() == 0 && selectDealData.deal.getAmount() <= 0 && selectDealData.deal.getType() != 1) { canSell.add(6); }
         if (sellButton.isActive()) {
            if (!player.isCreative()) { sellButton.setIsEnabled(canSell.isEmpty()); }
            if (!canSell.isEmpty()) { sellButton.layerColor = 0xFF800000; }
         }
      }
      // prise buttons
      if (selectDealData != null && selectDealData.deal != null) {
         List<Component> hoverBuy = new ArrayList<>();
         List<Component> hoverSell = new ArrayList<>();
         if (selectDealData.deal.getAmount() > 0 || (minecraft.player != null && minecraft.player.isCreative())) {
            if (!canBuy.isEmpty()) {
               for (int id : canBuy) { hoverBuy.add(Component.translatable("market.hover.notbuy." + id)); }
            }
            if (!canSell.isEmpty()) {
               for (int id : canSell) { hoverSell.add(Component.translatable("market.hover.notsell." + id)); }
            }
            if (!canBuy.isEmpty() || selectDealData.deal.getAmount() <= 0) { hoverBuy.add(Component.translatable("gui.allowed")); }
            if (!canSell.isEmpty()) { hoverSell.add(Component.translatable("gui.allowed")); }
            // buy hover info
            if (!selectDealData.buyItems.isEmpty()) {
               hoverBuy.add(Component.translatable("market.hover.item.buy"));
               for (ItemStack curr : selectDealData.buyItems.keySet()) {
                  hoverBuy.add(((MutableComponent) curr.getHoverName())
                          .append(Component.literal(" x").withStyle(ChatFormatting.GRAY)
                                  .append(Component.literal(selectDealData.buyItems.get(curr) + " ").withStyle(ChatFormatting.GOLD))));
               }
            }
            if (selectDealData.buyMoney > 0) { hoverBuy.add(Component.translatable("market.hover.currency.buy", selectDealData.buyMoney, CustomNpcs.displayCurrencies)); }
            if (selectDealData.buyDonat > 0) { hoverBuy.add(Component.translatable("market.hover.donat.buy", selectDealData.buyDonat, CustomNpcs.displayDonation)); }
            // sell hover info
            if (!selectDealData.sellItems.isEmpty()) {
               hoverSell.add(Component.translatable("market.hover.item.sell"));
               for (ItemStack curr : selectDealData.sellItems.keySet()) {
                  hoverSell.add(((MutableComponent) curr.getHoverName())
                          .append(Component.literal(" x").withStyle(ChatFormatting.GRAY)
                                  .append(Component.literal(selectDealData.sellItems.get(curr) + " ").withStyle(ChatFormatting.GOLD))));
               }
            }
            if (selectDealData.sellMoney > 0) { hoverSell.add(Component.translatable("market.hover.currency.sell", selectDealData.sellMoney, CustomNpcs.displayCurrencies)); }
         }
         if (selectDealData.deal.getType() != 1) { buyButton.setHoverTexts(hoverBuy); }
         if (selectDealData.deal.getType() != 0) { sellButton.setHoverTexts(hoverSell); }
      }
      add(new TradeButtonBiDirectional(this, 6, y, scrollWidth - 5));
      addCheckBox(11, 3, 3, "type.id", "N", isIdSort)
              .setSize(26, 12)
              .setHoverTexts(Component.translatable("hover.sort", Component.translatable("market.deals").getString(), Component.translatable(isIdSort ? "type.id" : "gui.name")));
      add(new MarcetTextField(this, 28, imageHeight - 19, scrollWidth - 31)
                 .setHoverTexts("market.hover.is.search"));
      getTextField(0).setFocused(focus);
   }

   @Override
   public boolean keyPressed(int key, int key_1, int key_2) {
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      if (!hasSubGui()) {
         if (key == InputConstants.getKey("key.keyboard.up").getValue() || key == minecraft.options.keyUp.getKey().getValue()) {
            if (!hovers.isEmpty() && isMouseHover(wrapper.mouseX, wrapper.mouseY, imageWidth - 153, 76, 128, hoverHeightMax - 4)) {
               hoverY = ValueUtil.correctInt(hoverY + minecraft.font.lineHeight + 1, -hoverMaxY, 0);
               return true;
            } else {
               scrollY = ValueUtil.correctInt(scrollY + 28, -scrollMaxY, 0);
            }
         } else if (key == InputConstants.getKey("key.keyboard.down").getValue() || key == minecraft.options.keyDown.getKey().getValue()) {
            if (!hovers.isEmpty() && isMouseHover(wrapper.mouseX, wrapper.mouseY, imageWidth - 153, 76, 128, hoverHeightMax - 4)) {
               hoverY = ValueUtil.correctInt(hoverY - minecraft.font.lineHeight - 1, -hoverMaxY, 0);
               return true;
            } else {
               scrollY = ValueUtil.correctInt(scrollY - 28, -scrollMaxY, 0);
            }
         }
      }
      return super.keyPressed(key, key_1, key_2);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
      if (isMouseHover(mouseX, mouseY, 3, 3, scrollWidth * 2 - 6, imageHeight - 50)) {
         scrollY = ValueUtil.correctInt(scrollY + (int) (scrolled * 28.0d), -scrollMaxY, 0);
      }
      else if (isMouseHover(mouseX, mouseY, imageWidth - 153, 76, 128, hoverHeightMax - 4)) {
         if (minecraft == null) { minecraft = Minecraft.getInstance(); }
         hoverY = ValueUtil.correctInt(hoverY + (int) (scrolled * ((double) minecraft.font.lineHeight + 1.0d)), -hoverMaxY, 0);
      }
      return super.mouseScrolled(mouseX, mouseY, scrolled);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (mouseButton == 0) {
         isScrolled = isMouseHover(mouseX, mouseY, scrollWidth * 2 - 14, 4, 10, imageHeight - 51);
         if (isScrolled) {
            double yPos = ValueUtil.correctDouble(mouseY, 20.0d, imageHeight - 71.0d) - 20.0d;
            scrollY = ValueUtil.correctInt((int) (yPos / (imageHeight - 91.0d) * -scrollMaxY), -scrollMaxY, 0);
         }
         isHovered = isMouseHover(mouseX, mouseY, imageWidth - 35, 76, 10, hoverHeightMax - 4);
         if (isHovered) {
            double yPos = ValueUtil.correctDouble(mouseY, 86.0d, 61.0d + hoverHeightMax) - 86.0d;
            hoverY = ValueUtil.correctInt((int) (yPos / (hoverHeightMax - 26.0d) * -hoverMaxY), -hoverMaxY, 0);
         }
      }
      return super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   @Override
   public boolean mouseDragged(double x, double y, int mouseButton, double dx, double dy) {
      if (((IMouseHandlerMixin) Minecraft.getInstance().mouseHandler).getActiveButton() == 0) {
         if (isScrolled) {
            double yPos = ValueUtil.correctDouble(y, 20.0d, imageHeight - 71.0d) - 20.0d;
            scrollY = ValueUtil.correctInt((int) (yPos / (imageHeight - 91.0d) * -scrollMaxY), -scrollMaxY, 0);
         } else if (isHovered) {
            double yPos = ValueUtil.correctDouble(wrapper.mouseY, 86.0d, 61.0d + hoverHeightMax) - 86.0d;
            hoverY = ValueUtil.correctInt((int) (yPos / (hoverHeightMax - 26.0d) * -hoverMaxY), -hoverMaxY, 0);
         }
      }
      return super.mouseDragged(x, y, mouseButton, dx, dy);
   }

   @Override
   public void save() {
      if (marcet != null) { Packets.sendServer(new SPacketTraderLivePlayer(marcet.getId())); }
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      wait = false;
      marcet = MarcetController.getInstance().getMarcet(marcet.getId());
      menu.marcet = marcet;
      init();
   }

   @Override
   public void unFocused(GuiTextFieldNop textField) { }

   @Override
   public void textUpdate(IComponentGui component, String text) {
      search = Component.literal(text);
      init();
   }

   @OnlyIn(Dist.CLIENT)
   public static class TradeButton extends GuiButtonNop {

      protected static final Random rnd = new Random();
      protected final Minecraft mc = Minecraft.getInstance();
      protected final Deal deal;
      protected final DealMarkup dm;
      protected final boolean inTrade;
      // case
      protected ResourceLocation objCase;
      protected Map<String, ResourceLocation> materialTextures = new HashMap<>();
      protected boolean type;
      protected boolean start;
      protected int rncd;
      // hovers
      protected final List<Component> hoverMain = new ArrayList<>();
      protected final List<Component> hoverPrise = new ArrayList<>();

      protected ParameterizedModel CHEST_FULL;
      protected ParameterizedModel CHEST_BODY;
      protected ParameterizedModel CHEST_TOP;

      public TradeButton(GuiNPCTrader gui, Deal dealIn, int level, int x, int y, int w, boolean inTradeIn) {
         super(gui, dealIn.getId(), dealIn.getName(), x, y, null);
         texture = BUTTONS;
         deal = dealIn;
         txrW = 256;
         txrH = 28;
         width = w;
         height = 28;

         dm = MarcetController.getInstance().getBuyData(marcet, deal, level, gui.count);
         if (!deal.isCase()) { renderStack = dm.main; }
         inTrade = inTradeIn;
         // product info
         if (deal.isCase()) {
            hoverMain.add(Component.translatable("market.hover.case"));
            hoverMain.add(Component.translatable("market.deal.case.count", deal.getCaseCount()));
            if (!deal.showInCase()) { hoverMain.add(Component.translatable("market.case.show.false").withStyle(ChatFormatting.RED)); }
            if (deal.showInCase() || (mc.player != null && mc.player.isCreative()))
            { deal.putHoverCaseItems(hoverMain, TooltipFlag.NORMAL); }
         }
         else {
            hoverMain.add(Component.translatable("market.hover.product"));
            hoverMain.add(((MutableComponent) dm.main.getHoverName())
                    .append(Component.literal(" x").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(dm.count + " ").withStyle(ChatFormatting.GOLD))
                    .append(Component.translatable("market.hover.item." + (deal.getMaxCount() > 0 ? deal.getAmount() == 0 ? "not" : "amount" : "infinitely"), "" + deal.getAmount()))));
         }
         if (deal.getAmount() > 0 || (mc.player != null && mc.player.isCreative())) {
            if (deal.getAmount() <= 0) { hoverPrise.add(Component.translatable("gui.allowed")); }
            // buy hover info
            if (!dm.buyItems.isEmpty()) {
               hoverPrise.add(Component.translatable("market.hover.item.buy"));
               for (ItemStack curr : dm.buyItems.keySet()) {
                  hoverPrise.add(((MutableComponent) curr.getHoverName())
                          .append(Component.literal(" x").withStyle(ChatFormatting.GRAY)
                                  .append(Component.literal(dm.buyItems.get(curr) + " ").withStyle(ChatFormatting.GOLD))));
               }
            }
            if (dm.buyMoney > 0) { hoverPrise.add(Component.translatable("market.hover.currency.buy", dm.buyMoney, CustomNpcs.displayCurrencies)); }
            if (dm.buyDonat > 0) { hoverPrise.add(Component.translatable("market.hover.donat.buy", dm.buyDonat, CustomNpcs.displayDonation)); }
            // sell hover info
            if (!dm.sellItems.isEmpty()) {
               hoverPrise.add(Component.translatable("market.hover.item.sell"));
               for (ItemStack curr : dm.sellItems.keySet()) {
                  hoverPrise.add(((MutableComponent) curr.getHoverName())
                          .append(Component.literal(" x").withStyle(ChatFormatting.GRAY)
                                  .append(Component.literal(dm.sellItems.get(curr) + " ").withStyle(ChatFormatting.GOLD))));
               }
            }
            if (dm.sellMoney > 0) { hoverPrise.add(Component.translatable("market.hover.currency.sell", dm.sellMoney, CustomNpcs.displayCurrencies)); }
         }
         // case model
         rncd = rnd.nextInt(10000);
         objCase = deal.getCaseObjModel();
         if (objCase != null) {
            try {
               Optional<Resource> res = mc.getResourceManager().getResource(objCase);
               if (res.isPresent()) { objCase = Deal.defaultCaseOBJ; } else { objCase = null; }
            }
            catch (Exception e) { objCase = null; }
         }
         materialTextures.put("#material", deal.getCaseTexture());
         CHEST_FULL = ModelBuffer.getParameterizedModel(objCase, null, materialTextures, true, 0);
         CHEST_BODY = ModelBuffer.getParameterizedModel(objCase, List.of("body"), materialTextures, true, 0);
         CHEST_TOP = ModelBuffer.getParameterizedModel(objCase, List.of("top"), materialTextures, true, 0);
      }

      @Override
      public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         if (!visible) { return; }
         RenderSystem.enableBlend();
         GuiNPCTrader parent = (GuiNPCTrader) listener;
         int y = getY() + parent.scrollY;
         isHovered = mouseY > 14 && mouseY < parent.imageHeight - 47 && mouseX >= getX() && mouseY >= y && mouseX < getX() + width && mouseY < y + height;
         if (isHovered) {
            hoverText.clear();
            hoverText.addAll(hoverMain);
         }
         int x = getX();
         if (y + height < 15 || y > parent.imageHeight - 48) { return; }
         y = getY();

         graphics.enableScissor(4, 15, parent.scrollWidth * 2, parent.imageHeight - 47);

         PoseStack matrixStack = graphics.pose();
         matrixStack.pushPose();
         matrixStack.translate(0, parent.scrollY, 0);

         matrixStack.pushPose();
         boolean isPrefabricated = txrW == 0;
         float scaleH = height / (float) txrH;
         float scaleW = isPrefabricated ? scaleH : width / (float) txrW;
         matrixStack.scale(scaleW, scaleH, 1.0f);
         matrixStack.translate(x / scaleW, y / scaleH, 0.0f);
         graphics.blit(texture, 0, 0, txrX, txrY + getState(inTrade) * txrH, txrW, txrH);
         matrixStack.popPose();

         // rarity color
         if (deal.getRarityColor() != 0) { graphics.fillGradient(x + 2, y + 2, x + width - 2, y + height - 2, 0x0, deal.getRarityColor() | 0x80000000); }
         // case obj model
         if (deal.isCase() && objCase != null) {
            matrixStack.pushPose();
            matrixStack.translate(x + 16.0f, y + 8.5f, 16.0f);
            if ((System.currentTimeMillis() + rncd) % 10000 < 2000 || isHovered && isMouseHover(mouseX, mouseY, x + 1, y + parent.scrollY + 2, 32, 22)) {
               float i = (float) ((System.currentTimeMillis() + rncd) % 2000);
               if (!start) {
                  matrixStack.mulPose(Axis.XP.rotationDegrees(-15.0f));
                  matrixStack.mulPose(Axis.YP.rotationDegrees(-75.0f));
                  matrixStack.scale(16.0f, -16.0f, 16.0f);
                  ModelBuffer.render(CHEST_FULL, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                  if (i >= 1980) { start = true; }
               }
               else {
                  if (i <= 20) { type = rnd.nextFloat() < 0.5f; }
                  float rot;
                  if (type) {
                     if (i < 600) { rot = 0.033333f * i; }
                     else if (i < 1700) { rot = - 0.027273f * i + 36.363636f; }
                     else { rot = 0.033333f * i - 66.666666f; }
                     matrixStack.mulPose(Axis.XP.rotationDegrees(-15.0f));
                     matrixStack.mulPose(Axis.YP.rotationDegrees(-75.0f + rot));
                     matrixStack.scale(16.0f, -16.0f, 16.0f);
                     ModelBuffer.render(CHEST_FULL, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                  }
                  else {
                     matrixStack.mulPose(Axis.XP.rotationDegrees(-15.0f));
                     matrixStack.mulPose(Axis.YP.rotationDegrees(-75.0f));
                     matrixStack.scale(16.0f, -16.0f, 16.0f);
                     ModelBuffer.render(CHEST_BODY, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                     if (i < 1500) { rot = 0.016667f * i; }
                     else if (i < 1900) { rot = 25.0f; }
                     else { rot = -0.25f * i + 500.0f; }
                     matrixStack.pushPose();
                     matrixStack.mulPose(Axis.ZP.rotationDegrees(rot));
                     ModelBuffer.render(CHEST_TOP, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                     matrixStack.popPose();
                  }
               }
            }
            else {
               matrixStack.mulPose(Axis.XP.rotationDegrees(-15.0f));
               matrixStack.mulPose(Axis.YP.rotationDegrees(-75.0f));
               matrixStack.scale(16.0f, -16.0f, 16.0f);
               ModelBuffer.render(CHEST_FULL, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            matrixStack.popPose();
         }
         if (renderStack != null && !renderStack.isEmpty()) {
            graphics.blit(ICONS, x + 6, y + 2, 0, getState(true) * 24, 24, 24);
            if (!inTrade) { RenderSystem.setShaderColor(0.4F, 0.4F, 0.4F, 1.0F); }
            graphics.renderItem(renderStack, x + 10, y + 6);
            graphics.renderItemDecorations(mc.font, renderStack, x + 10, y + 6, null);
            if (!inTrade) { RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); }
            if (isHovered && isMouseHover(mouseX, mouseY, x + 6, y + parent.scrollY + 3, 22, 22)) {
               hoverText.clear();
               hoverText.addAll(renderStack.getTooltipLines(mc.player, mc.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL));
            }
         }
         // money and barter
         int mw = 0;
         if (deal.getAmount() > 0 || (mc.player != null && mc.player.isCreative())) {
            // money
            MutableComponent money = Component.empty();
            if (dm.sellMoney > 0) {
               money.append(Component.literal("↑").withStyle(ChatFormatting.YELLOW))
                       .append(Component.literal(Util.instance.getTextReducedNumber(dm.sellMoney, true, true, false)).withStyle(ChatFormatting.RESET));
            }
            if (dm.buyMoney > 0) {
               if (!money.getString().isEmpty()) { money.append(" "); }
               money.append(Component.literal("↓").withStyle(ChatFormatting.GREEN))
                       .append(Component.literal(Util.instance.getTextReducedNumber(dm.buyMoney, true, true, true)).withStyle(ChatFormatting.RESET));
            }
            MutableComponent donat = Component.empty();
            if (dm.buyDonat > 0) {
               if (!donat.getString().isEmpty()) { donat.append(" "); }
               donat.append(Component.literal("↓").withStyle(ChatFormatting.BLUE))
                       .append(Component.literal(Util.instance.getTextReducedNumber(dm.buyDonat, true, true, false)).withStyle(ChatFormatting.RESET));
            }
            int mt = 0;
            boolean hasM = !money.getString().isEmpty();
            boolean hasD = !donat.getString().isEmpty();
            if (hasM && hasD) {
               x = getX() + width - 14;
               y = getY() + 3;
               mt = 1;
               mw = mc.font.width(money);
               if (System.currentTimeMillis() % 4000 < 2000) { mt = 2; mw = mc.font.width(donat); }
            }
            else if (hasM || hasD) {
               x = getX() + width - 14;
               y = getY() + 3;
               if (hasM) { mt = 1; mw = mc.font.width(money); }
               if (hasD) { mt = 2; mw = mc.font.width(donat); }
            }
            // draw prise info
            if (mt != 0) {
               x -= mw;
               graphics.drawString(mc.font, mt == 1 ? money : donat, x, y, CustomNpcs.MainColor.getRGB() | 0xFF000000);
               matrixStack.pushPose();
               matrixStack.translate(x + mw - 2, y - 4, 0.0f);
               matrixStack.scale(0.0625f, 0.0625f, 0.0625f);
               graphics.blit(mt == 1 ? GuiBasic.MONEY : GuiBasic.DONAT, 0, 0, 0, 0, 256, 256);
               matrixStack.popPose();
               if (isHovered && isMouseHover(mouseX, mouseY, x, y + parent.scrollY, mw + 14, 10)) {
                  hoverText.clear();
                  hoverText.addAll(hoverPrise);
               }
            }
            // barter
            if (!dm.buyItems.isEmpty()) {
               float sc = 1.0f;
               int size = dm.buyItems.size();
               mw = size * 16;
               if (width - 34 < mw) { sc = (width - 34.0f) / (float) mw; }
               float s = 0.666666f * sc;
               // slots
               matrixStack.pushPose();
               matrixStack.translate(getX() + width - 2 - mw * sc, getY() + height - 2.0f - 16.0f * sc, 0.0f);
               matrixStack.scale(s, s, s);
               for (int i = 0; i < size; i++) {
                  graphics.blit(ICONS, i * 24, 0, 0, 0, 24, 24);
               }
               matrixStack.popPose();
               s = 0.875f * sc;
               // stacks
               matrixStack.pushPose();
               x = (int) (getX() + width - 1 - mw * sc);
               y = (int) (getY() + height - 1.0f - 16.0f * sc);
               matrixStack.translate(x, y, 0.0f);
               matrixStack.scale(s, s, s);
               int i = 0;
               List<Component> hovers;
               for (ItemStack stack : dm.buyItems.keySet()) {
                  graphics.renderItem(stack, i * 18, 0);

                  matrixStack.pushPose();
                  String sCount = Util.instance.getTextReducedNumber(dm.buyItems.get(stack), true, true, false);
                  matrixStack.translate(i * 18 + 17, 16.0F, 200.0F);
                  matrixStack.scale(0.75f, 0.75f, 0.75f);
                  graphics.drawString(mc.font, sCount, -mc.font.width(sCount), -7.5f, CustomNpcs.MainColor.getRGB(), true);
                  matrixStack.popPose();

                  if (isHovered && isMouseHover(mouseX, mouseY, x + (i * 18) * s, y + parent.scrollY, 16.0f * s, 16.0f * s)) {
                     hoverText.clear();
                     hovers = stack.getTooltipLines(mc.player, mc.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
                     if (hovers.get(0) instanceof Component) {
                        hovers.set(0, Component.literal(hovers.get(0).getString()).append(ChatFormatting.RESET + " x" + sCount));
                     }
                     hoverText.addAll(hovers);
                  }
                  i++;
               }
               matrixStack.popPose();
            }
         }
         // name
         matrixStack.popPose();

         matrixStack.pushPose();
         x = getX() + 36;
         y = getY() + 2 + parent.scrollY;
         renderString(graphics, getMessage(), x, y, x + width - 39 - mw, y + 10,
                 CustomNpcs.MainColor.getRGB() | 0xFF000000, true, false, customFont);
         matrixStack.popPose();

         graphics.disableScissor();
      }

      public int getState(boolean tradeIn) {
         if (!tradeIn) { return 2; }
         if (!listener.hasSubGui()) {
            try {
               if (((GuiNPCTrader) listener).selectDealData.deal.equals(deal)) { return 1; }
            }
            catch (Exception ignored) { }
            if (isHovered && !listener.hasSubGui()) {
               return ((IMouseHandlerMixin) mc.mouseHandler).getActiveButton() == 0 ? 2 : 1;
            }
         }
         return 0;
      }

      public boolean isMouseHover(double mX, double mY, double px, double py, double pWidth, double pHeight) {
         return mX >= px && mY >= py && mX < (px + pWidth) && mY < (py + pHeight);
      }

      @Override
      public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
         if (active && visible && isValidClickButton(mouseButton) && isHovered) {
            playDownSound(mc.getSoundManager());
            onClick(mouseX, mouseY);
            return true;
         }
         return false;
      }

   }

   @OnlyIn(Dist.CLIENT)
   public static class SectionButton extends GuiMenuSideButton {

      public SectionButton(GuiNPCTrader gui, int id, MarcetSection sectionIn, int x, int y) {
         super(gui, id, Component.empty(), x, y);
         setWidth(sectionIn == null ? 16 : 24);
         setHeight(sectionIn == null ? 9 : 24);
         texture = ICONS;
         if (sectionIn != null) {
            txrX = (sectionIn.getIcon() % 10) * 24;
            txrY = (int) Math.floor((float) sectionIn.getIcon() / 10.0f) * 72;
         } else {
            txrX = 240;
            txrY = id == 3 ? 27 : 0;
         }
      }

      @Override
      public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         if (!visible) { return; }
         isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
         int state = 0;
         boolean lbm = ((IMouseHandlerMixin) Minecraft.getInstance().mouseHandler).getActiveButton() == 0;
         if (isHoveredOrFocused() && !listener.hasSubGui()) { state = (lbm ? 2 : 1) * height; }
         else if (active) { state = height; }
         graphics.blit(texture, getX(), getY(), txrX, txrY + state, width, height);

         if (isHovered && !hoverText.isEmpty()) {
            if (listener != null) { listener.setHoverText(hoverText); }
            else { GuiTooltipUtils.renderTooltip(graphics, Minecraft.getInstance().font, hoverText, Optional.empty(), mouseX, mouseY); }
         }
      }

      @Override
      public SectionButton setHoverTexts(Object... components) {
         super.setHoverTexts(components);
         return this;
      }

   }

   @OnlyIn(Dist.CLIENT)
   public static class TradeButtonBiDirectional extends GuiButtonBiDirectional {

      public TradeButtonBiDirectional(GuiNPCTrader gui, int x, int y, int w) {
         super(gui, 0, x, y, 0, new Object[1]);
         texture = BUTTONS;
         txrY = 84;
         txrW = 256;
         txrH = 20;
         width = w;

         display = new Component[64];
         for (int i = 0; i < 64; i++) { display[i] = Component.literal("" + (i + 1)); }
         displayValue = gui.count - 1;
         if (displayValue < display.length) { setDisplayText(display[displayValue]); }
      }

      @Override
      public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         if (!visible) { return; }
         isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
         hoverL = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + 20 && mouseY < getY() + height;
         hoverR = !hoverL && mouseX >= getX() + width - 19 && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
         PoseStack matrixStack = graphics.pose();

         boolean lmb = ((IMouseHandlerMixin) Minecraft.getInstance().mouseHandler).getActiveButton() == 0;
         int stateL = !active ? 40 : hoverL ? (display.length > 1 ? lmb ? 40 : 20 : 0) : 0;
         int stateR = !active ? 40 : hoverR ? (display.length > 1 ? lmb ? 40 : 20 : 0) : 0;
         int state = !active ? 40 : isHovered && display.length > 1 ? 20 : 0;
         int wl = (width - 38) / 2;
         int wr = width - 39 - wl;

         matrixStack.pushPose();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         matrixStack.translate(getX(), getY(), 0.0f);
         graphics.blit(BUTTONS, 0, 0, 0, txrY + stateL, 19, 20);
         graphics.blit(BUTTONS, width - 20, 0, 256 - 19, txrY + stateR, 19, 20);
         graphics.blit(BUTTONS, 19, 0, 19, txrY + state, wl, 20);
         graphics.blit(BUTTONS, 19 + wl, 0, 236 - wr, txrY + state,  wr, 20);
         matrixStack.popPose();

         Component mes = getMessage();
         if (isHovered) { mes = MutableComponent.create(mes.getContents()).withStyle(ChatFormatting.UNDERLINE); }
         renderString(graphics, mes, getX() + 11, getY(), getX() + getWidth() - 11, getY() + getHeight(),
                 getFGColor() | Mth.ceil(alpha * 255.0F) << 24, showShadow, true, customFont);
         if (isHovered && !hoverText.isEmpty()) {
            if (listener != null) { listener.setHoverText(hoverText); }
            else { GuiTooltipUtils.renderTooltip(graphics, Minecraft.getInstance().font, hoverText, Optional.empty(), mouseX, mouseY); }
         }
      }

   }

   @OnlyIn(Dist.CLIENT)
   public static class MarcetTextField extends GuiTextFieldNop {

      public MarcetTextField(GuiNPCTrader gui, int x, int y, int widthIn) {
         super(gui, 0, x, y, widthIn, 18, GuiNPCTrader.search);
         setBordered(false);
      }

      @Override
      public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         if (!enabled || !visible) { return; }
         setTextColor(getTextColor());
         int x = getX() - 3;
         int y = getY() - 6;
         int w = width + 6;
         isHovered = mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + height + 2;

         int w0 = w / 2;
         int w1 = w - w0;
         int state = isFocused() || !isHovered ? 56 : 0;
         graphics.blit(BUTTONS, x, y, 0, state, w0, 10); // left
         graphics.blit(BUTTONS, x, y + 10, 0, state + 18, w0, 10); // left down
         graphics.blit(BUTTONS, x + w0, y, 256 - w1, state, w1, 10); // right up
         graphics.blit(BUTTONS, x + w0, y + 10, 256 - w1, state + 18, w1, 10); // right down
         super.renderWidget(graphics, mouseX, mouseY, partialTicks);
         if (isHovered && !hoverText.isEmpty() && listener != null) { listener.setHoverText(hoverText); }
      }

   }

}
