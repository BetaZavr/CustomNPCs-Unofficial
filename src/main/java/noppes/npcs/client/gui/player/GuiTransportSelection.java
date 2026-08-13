package noppes.npcs.client.gui.player;

import java.util.*;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketPlayerTransport;
import noppes.npcs.roles.RoleTransporter;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IScrollData;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

public class GuiTransportSelection extends GuiNPCInterface
        implements IScrollData, ICustomScrollListener {

   protected final ResourceLocation resource = getResource("smallbg.png");
   protected GuiCustomScrollNop scroll;

   // New from Unofficial (BetaZavr)
   protected final Map<Component, Integer> data = new LinkedHashMap<>();
   protected Map<ItemStack, Boolean> barterItems;
   protected boolean canTransport = true;
   protected int bxSize = 0;
   protected int bySize = 0;

   public GuiTransportSelection(EntityNPCInterface npc) {
      super(npc);
      drawDefaultBackground = false;
      imageWidth = 176;
      title = Component.empty();
   }

   @Override
   public void init() {
      super.init();
      guiLeft = (width - imageWidth) / 2;
      guiTop = (height - 222) / 2;
      Component title = Component.empty();
      TransportController tData = TransportController.getInstance();
      if (npc != null && npc.role instanceof RoleTransporter) {
         TransportLocation loc = ((RoleTransporter) npc.role).getLocation();
         if (loc != null) {
            title = Component.translatable(loc.category.title).append(": ").append(Component.translatable(loc.name));
         }
      }
      addLabel(0, guiLeft + (imageWidth - font.width(title)) / 2, guiTop + 8, title)
              .setSize(imageWidth - 10, 10);
      addButton(0, guiLeft + 10, guiTop + 192, "transporter.travel")
              .setSize(156, 20);
      if (scroll == null) { scroll = addScroll(0).setSize(156, 165); }
      List<Component> list = new ArrayList<>(data.keySet());
      add(scroll.setPos(guiLeft + 10, guiTop + 20)
              .setNormalList(list));
      if (list.size() * (font.lineHeight + 3) < 165) { scroll.setSize(156, 170).disabledSearch(); }
      else { scroll.enabledSearch().setSize(156, 165); }
      if (!data.isEmpty()) {
         List<Component> suffixes = new ArrayList<>();
         for (Component name : list) {
            MutableComponent sfx = Component.empty();
            TransportLocation loc = tData.getTransport(data.get(name));
            if (loc != null) {
               ChatFormatting color = ChatFormatting.GREEN;
               if (loc.money > 0 || !loc.inventory.isEmpty()) {
                  if (loc.money > 0) {
                     if (loc.money > PlayerData.get(player).game.getMoney()) { color = ChatFormatting.RED; }
                     sfx = Component.empty()
                             .append(Component.literal(Util.instance.getTextReducedNumber(loc.money, true, true, false)
                                             + CustomNpcs.displayCurrencies)
                                     .withStyle(color));
                  }
               }
               if (!loc.inventory.isEmpty()) {
                  color = ChatFormatting.GOLD;
                  Map<ItemStack, Boolean> items = Util.instance.getInventoryItemCount(player, loc.inventory);
                  for (ItemStack s : items.keySet()) {
                     if (!items.get(s)) { color = ChatFormatting.RED; break; }
                  }
                  sfx.append(Component.literal(" [").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal("I").withStyle(color))
                          .append(Component.literal("]").withStyle(ChatFormatting.GRAY));
               }
            }
            suffixes.add(sfx);
         }
         scroll.setSuffixes(suffixes);
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      if (button.id == 0 && data.containsKey(scroll.getNormalSelected())) {
         Packets.sendServer(new SPacketPlayerTransport(data.get(scroll.getNormalSelected())));
         onClose();
      }
   }

   @Override
   public void renderBackground(@Nonnull GuiGraphics graphics) {
      super.renderBackground(graphics);
      graphics.blit(resource, guiLeft, guiTop, 0, 0, 176, 222);
      barterItems = null;
      if (data.containsKey(scroll.getNormalSelected())) {
         TransportLocation select = TransportController.getInstance().getTransport(data.get(scroll.getNormalSelected()));
         if (select != null) {
            // barter & money
            if (bxSize > 0) {
               int w = bxSize + 13;
               int h = bySize + 18;
               int x = guiLeft + 176;
               int y = guiTop + 14;
               graphics.blit(resource, x, y, 176 - w, 0, w, h);
               graphics.blit(resource, x, y + h, 176 - w, 218, w, 4);
               x += 5;
               y += 4;
               if (!select.inventory.isEmpty()) {
                  graphics.drawString(font, Component.translatable("market.barter"), x, y, CustomNpcs.LableColor.getRGB(), false);
               }
               if (select.money > 0L) {
                  graphics.drawString(font, Util.instance.getTextReducedNumber(select.money, true, true, false)
                          + CustomNpcs.displayCurrencies, x, y + 32, CustomNpcs.LableColor.getRGB(), false);
               }
            }
            // items
            bxSize = 0;
            bySize = 0;
            if (!select.inventory.isEmpty()) {
               PoseStack matrixStack = graphics.pose();
               matrixStack.pushPose();
               barterItems = Util.instance.getInventoryItemCount(player, select.inventory);
               int slot = 0;
               canTransport = true;
               for (ItemStack stack : barterItems.keySet()) {
                  int u = guiLeft + imageWidth + 5 + (slot % 3) * 18;
                  int v = guiTop + 30 + (slot / 3) * 18;
                  graphics.blit(RESOURCE_SLOT, u, v, 0, 0, 18, 18);
                  if (canTransport) { canTransport = barterItems.get(stack); }
                  if (getButton(0) != null && getButton(0).isHoveredOrFocused()) {
                     graphics.fill(u + 1, v + 1, u + 17, v + 17, barterItems.get(stack) ? 0x8000FF00 : player.isCreative() ? 0x80FF6E00 : 0x80FF0000);
                  }
                  slot++;
               }
               float a = (float) slot / 3.0f;
               bxSize = (a >= 1.0f ? 3 : a >= 2.0f / 3.0f ? 2 : 1) * 18;
               bySize = (int) (Math.ceil(a) * 18.0d);
               matrixStack.popPose();
            }
            if (select.money > 0) { bySize += 14; }
         }
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      renderBackground(graphics);
      if (data.containsKey(scroll.getNormalSelected())) {
         TransportLocation select = TransportController.getInstance().getTransport(data.get(scroll.getNormalSelected()));
         if (select != null && !select.inventory.isEmpty()) {
            if (minecraft == null) { minecraft = Minecraft.getInstance(); }
            PoseStack matrixStack = graphics.pose();
            int slot = 0;
            for (ItemStack stack : barterItems.keySet()) {
               int u = guiLeft + imageWidth + 5 + (slot % 3) * 18;
               int v = guiTop + 31 + (slot / 3) * 18;
               matrixStack.pushPose();
               matrixStack.translate(u, v, 50.0f);
               graphics.renderItem(stack, 0, 0);
               graphics.renderItemDecorations(font, stack, 0, 0);
               matrixStack.popPose();
               if (isMouseHover(mouseX, mouseY, u, v, 18, 18)) {
                  setHoverText(stack.getTooltipLines(player, minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL));
               }
               slot++;
            }
         }
         GuiButtonNop button = getButton(0);
         if (button != null) {
            button.setIsEnabled(canTransport && select != null);
            if (!button.isEnabled() && button.isHoveredOrFocused()) {
               if (select == null) { setHoverText(Component.translatable("transporter.hover.not.select")); }
               else if (select.money > PlayerData.get(player).game.getMoney()) { setHoverText(Component.translatable("transporter.hover.not.money")); }
               else { setHoverText(Component.translatable("transporter.hover.not.item")); }
            }
         }
      }
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public void setData(Vector<String> dataList, Map<String, Integer> dataMap) {
      data.clear();
      for (String key : dataMap.keySet()) { data.put(Component.translatable(key), dataMap.get(key)); }
      init();
   }

   @Override
   public void setSelected(String selected) { }

   // New from Unofficial (BetaZavr)
   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      if (data.containsKey(scroll.getNormalSelected())) { init();}
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
      GuiButtonNop button = getButton(0);
      if (data.containsKey(scroll.getNormalSelected()) && button != null && button.isEnabled()) {
         onClose();
         Packets.sendServer(new SPacketPlayerTransport(data.get(scroll.getNormalSelected())));
      }
   }

}
