package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.global.GuiNpcManagePlayerData;
import noppes.npcs.client.gui.global.SubGuiEditBankAccess;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.constants.EnumPlayerData;
import noppes.npcs.containers.ContainerNPCBank;
import noppes.npcs.controllers.data.Bank;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.mixin.client.IMouseHandlerMixin;
import noppes.npcs.mixin.client.gui.screens.inventory.IAbstractContainerScreenMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class GuiNPCBankChest extends GuiContainerNPCInterface<ContainerNPCBank> {

   protected final ResourceLocation bg = new ResourceLocation(CustomNpcs.MODID, "textures/gui/extrasmallbg.png");
   public static final ResourceLocation INVENTORY_ITEMS = new ResourceLocation(CustomNpcs.MODID, "textures/gui/bank/inventory.png");
   public static final ResourceLocation INVENTORY_EMPTY = new ResourceLocation(CustomNpcs.MODID, "textures/gui/bank/empty.png");
   public static final ResourceLocation SAFE = new ResourceLocation(CustomNpcs.MODID, "textures/gui/bank/safe.png");

   public static double startXMouse = 0;
   public static double startYMouse = 0;

   protected final boolean isMany;
   protected boolean isOwner;
   public boolean isWait;
   public int ceilsUpdate;
   public int ceilPos;

   //scrolling
   protected boolean isScrolling = false;
   protected int scrollBarY;
   public int scrollMax = 0;
   public int scrollY;

   // currency to open or upgrade
   protected boolean canUpgrade;
   protected ItemStack stack;
   protected int money;
   protected int donat;

   @Nullable
   protected Slot hoverSlot;

   public GuiNPCBankChest(ContainerNPCBank container, Inventory inv, Component ignoredTitle) {
      super(NoppesUtilServer.getEditingNpc(Minecraft.getInstance().player), container, inv, Component.empty());
      hoverIsGame = true;
      if (container.items.getContainerSize() > 0) {
         background = INVENTORY_ITEMS;
         imageWidth = 190;
         imageHeight = 238;
      } else {
         background = INVENTORY_EMPTY;
         imageWidth = 178;
         imageHeight = 143;
      }
      isMany = container.items.getContainerSize() > 45;
      if (isMany) { scrollMax = (int) (Math.max(0.0d, Math.ceil((double) container.items.getContainerSize() / 9.0d)) * 18.0d - 90.0d); }
      scrollY = menu.scrollY;
      ceilPos = ValueUtil.correctInt(menu.ceilPos, 0, menu.data.bank.ceilSettings.size() - 5);
      ceilsUpdate = menu.ceilsUpdate;
      resetRow(0);
   }

   @Override
   public void init() {
      super.init();
      int color = CustomNpcs.MainColor.getRGB();
      // name
      MutableComponent title = Component.translatable("bank.name" + (menu.data.bank.isPublic ? ".public" : ""), ": ");
      if (ContainerNPCBank.editPlayerBankData != null && ContainerNPCBank.editPlayerBankData.isEmpty()) {
         title = Component.translatable("bank.name.player", ContainerNPCBank.editPlayerBankData, ": ");
      }
      title.append(Component.translatable(menu.data.bank.name).withStyle(ChatFormatting.BOLD));
      addLabel(0, guiLeft + 5, guiTop + 5, title)
              .setSize(imageWidth - 10, 10)
              .setColor(color);
      // currency to open or upgrade
      stack = ItemStack.EMPTY;
      money = 0;
      donat = 0;
      Bank.CeilSettings cs = menu.data.bank.ceilSettings.get(menu.ceil);
      isOwner = player.isCreative() || !menu.data.bank.isPublic || menu.data.bank.owner.isEmpty() || player.getName().getString().equals(menu.data.bank.owner);
      canUpgrade = false;
      if (menu.items.getContainerSize() == 0) {
         stack = cs.openStack;
         money = cs.openMoney;
         donat = cs.openDonat;
         canUpgrade = false;
      }
      else if (menu.items.getContainerSize() < cs.maxCells && !cs.upgradeStack.isEmpty()) {
         stack = cs.upgradeStack;
         money = cs.upgradeMoney;
         donat = cs.upgradeDonat;
         canUpgrade = true;
      }
      // open and upgrade buttons
      Slot slot = menu.items.getContainerSize() < menu.slots.size() ? menu.getSlot(menu.items.getContainerSize()) : null;
      int u = (width - imageWidth) / 2 - 8;
      int v = (height - imageHeight) / 2;
      if (slot != null) {
         // not owner
         if (isOwner && (!stack.isEmpty() || money > 0 || donat > 0)) {
            int x = u + slot.x + 61 + (stack.isEmpty() ? 0 : 21);
            int y = v + slot.y - 22; // upgrade button up + right or center
            boolean showButton = player.isCreative() || canUpgrade ? menu.items.getContainerSize() < cs.maxCells : menu.ceil < menu.data.bank.ceilSettings.size();
            addButton(0, x, y, canUpgrade ? "bank.upgrade" : "bank.unlock")
                    .setSize(51, 18)
                    .setIsEnabled(showButton);
            // size -> open / upgrade
            if (canUpgrade) {
               int s = cs.maxCells - menu.items.getContainerSize();
               if (s > 0) {
                  int p = 0;
                  List<Object> list = new ArrayList<>();
                  list.add("1");
                  list.add("5");
                  list.add("10");
                  list.add("20");
                  list.add("gui.max");
                  if (ceilsUpdate == 5) { p = 1; }
                  else if (ceilsUpdate == 10) { p = 2; }
                  else if (ceilsUpdate == 20) { p = 3; }
                  else if (ceilsUpdate != 1) { p = 4; ceilsUpdate = s; }
                  addButton(14, x + 52, y, true, p , list.toArray(new Object[0]))
                          .setSize(37, 18)
                          .setIsEnabled(showButton)
                          .setIsVisible(cs.maxCells - menu.items.getContainerSize() > 0);
               }
            }
            if (money > 0) {
               addLabel(1, guiLeft + 21, y + (donat > 0 ? -1 : 4), Util.instance.getTextReducedNumber(money, true, true, false) + CustomNpcs.displayCurrencies)
                       .setSize(38, 10)
                       .setColor(color);
            }
            if (donat > 0) {
               addLabel(2, guiLeft + 21, y + (money > 0 ? 11 : 4), Util.instance.getTextReducedNumber(money, true, true, false) + CustomNpcs.displayCurrencies)
                       .setSize(38, 10)
                       .setColor(color);
            }
         }
         else {
            addLabel(10, u + slot.x + 6, v + slot.y - 18, "")
                    .setSize(162, 10)
                    .setColor(color);
         }
         // clear items
         GuiButtonNop clearButton = new GuiButtonNop(this, 10, "", u + slot.x + 171, v + slot.y + 59,
                 (button) -> {
                    if (!menu.items.isEmpty()) {
                       ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
                          if (bo) {
                             Packets.sendServer(new SPacketBankClearCeil(menu.data.bank.id, menu.ceil, ceilPos, ceilsUpdate));
                          }
                          NoppesUtil.openGUI(player, this);
                       },
                               Component.translatable("bank.name", ": ")
                                       .append(Component.translatable(menu.data.bank.name).withStyle(ChatFormatting.BOLD))
                                       .append("; ")
                                       .append(Component.translatable("gui.ceil", " #" + ((char) 167) + "l" +(menu.ceil + 1))),
                               Component.translatable("message.bank.del.items"));
                       setScreen(guiYesNo);
                    }
                 })
                 .setSize(14, 14)
                 .setTexture(INVENTORY_ITEMS)
                 .setUV(190, 20, 24, 24)
                 .setDefBack(false)
                 .setIsVisible(isOwner && menu.items.getContainerSize() > 0)
                 .setIsEnabled(!menu.items.isEmpty())
                 .setHoverTexts("bank.hover.clear.slots");
         add(clearButton);
      }
      // creative manager
      if (player.isCreative()) {
         int x = guiLeft + imageWidth + 7;
         int y = guiTop + 15;
         addButton(11, x, y, "bank.lock")
                 .setSize(80, 18)
                 .setIsEnabled(menu.items.getContainerSize() > 0 && !cs.openStack.isEmpty())
                 .setHoverTexts("bank.hover.lock");
         int size = Math.max(menu.data.bank.ceilSettings.get(menu.ceil).startCells, menu.items.getContainerSize() - ceilsUpdate);
         addButton(12, x, y += 21, "bank.regrade")
                 .setSize(80, 18)
                 .setIsEnabled(menu.items.getContainerSize() > cs.startCells)
                 .setHoverTexts(Component.translatable("bank.hover.regrade", ((char) 167) + "6" + (menu.items.getContainerSize() - size)));
         addButton(13, x, y + 21, "gui.reset")
                 .setSize(80, 18)
                 .setIsEnabled(!menu.items.isEmpty() || menu.items.getContainerSize() != cs.startCells)
                 .setHoverTexts("bank.hover.reset");
      }
      // ceil tabs
      if (ceilPos < 0) { ceilPos = 0; }
      GuiSafeButton tab;
      if (menu.data.bank.ceilSettings.size() > 1) {
         int ceil;
         int i;
         int x = guiLeft - 34;
         int y = guiTop + 16;
         for (i = 0; i < 5; i++) {
            ceil = i + ceilPos;
            if (ceil < menu.data.bank.ceilSettings.size()) {
               tab = new GuiSafeButton(this, 3 + i, (1 + i + ceilPos) + ">", x, y + 11 + i * 14, 30, 14, ceil);
               tab.setHoverTexts(Component.translatable("bank.hover.ceil." + (i + ceilPos * 5 == menu.ceil), "" + (ceil + 1)));
               add(tab);
               // stop if this ceil is not open
               NpcMiscInventory inv = menu.data.get(ceil);
               if (inv == null || inv.getContainerSize() == 0 || ceil >= menu.data.bank.ceilSettings.size()) { break; }
            }
         }
         if (ceilPos > 0) {
            tab = new GuiSafeButton(this, 1, "" + ((char) 708), x, y, 30, 11, -1);
            tab.setHoverTexts("bank.hover.up").setSize(30, 14);
            add(tab);
         }
         if (i == 5 && ceilPos + i < menu.data.bank.ceilSettings.size()) {
            tab = new GuiSafeButton(this, 2, "" + ((char) 709), x, y + 81, 30, 11, -1);
            tab.setHoverTexts("bank.hover.down").setSize(30, 14);
            add(tab);
         }
      }
      // lock
      if (menu.data.bank.isPublic) {
         addButton(9, (width - imageWidth) / 2 - 29, (height - imageHeight) / 2 + 109, "")
                 .setTexture(AbstractWidget.WIDGETS_LOCATION) // lock
                 .setUV(menu.data.bank.owner.isEmpty() ? 20 : 0, 146, 20, 20)
                 .setSize(20, 20)
                 .setIsVisible(isOwner)
                 .setHoverTexts("bank.hover.settings");
      }
      resetRow(0);
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      if (isWait) { return; }
      if (button.id > 2 && button.id < 8) {
         if (menu.ceil != ((GuiSafeButton) button).ceil) {
            onClose();
            Packets.sendServer(new SPacketBankOpen(menu.data.bank.id, ((GuiSafeButton) button).ceil, ceilPos, 0, ceilsUpdate));
         }
         return;
      } // open ceil
      switch (button.id) {
         case 0: {
            if (menu.items.getContainerSize() == menu.data.bank.ceilSettings.get(menu.ceil).maxCells) {
               ceilsUpdate = 1;
            }
            Packets.sendServer(new SPacketBankUpgrade(menu.data.bank.id, menu.ceil, ceilsUpdate, scrollY, ceilPos));
            isWait = true;
            break;
         } // open or upgrade
         case 1: {
            ceilPos--;
            if (ceilPos < 0) { ceilPos = 0; }
            init();
            break;
         } // up
         case 2: {
            ceilPos++;
            if (ceilPos > menu.data.bank.ceilSettings.size() - 5) { ceilPos = menu.data.bank.ceilSettings.size() - 5; }
            init();
            break;
         } // down
         case 9: {
            if (menu.data.bank.owner.isEmpty() && !player.isCreative()) { return; }
            setSubGui(new SubGuiEditBankAccess(menu.data.bank));
            break;
         } // settings
         case 11: {
            Packets.sendServer(new SPacketBankLock(menu.data.bank.id));
            isWait = true;
            break;
         } // lock
         case 12: {
            if (menu.items.getContainerSize() > menu.data.bank.ceilSettings.get(menu.ceil).startCells) {
               Packets.sendServer(new SPacketBankRegrade(menu.data.bank.id, scrollY, ceilPos, ceilsUpdate));
               isWait = true;
            }
            break;
         } // regrade
         case 13: {
            Packets.sendServer(new SPacketBankResetCeil(menu.data.bank.id, ceilPos, ceilsUpdate));
            isWait = true;
            break;
         } // reset
         case 14: {
            try {
               ceilsUpdate = Integer.parseInt(button.getMessage().getString());
            }
            catch (Exception ignored) {
               ceilsUpdate = 0;
            }
            init();
            //Packets.sendServer(new SPacketBankUpgrade(menu.data.bank.id, menu.ceil, ceilsUpdate, scrollY, ceilPos));
            break;
         } // upgrade all
      }
   }

   @Override
   protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
      super.renderBg(graphics, partialTicks, mouseX, mouseY);
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      int u = (width - imageWidth) / 2 - 8;
      int v = (height - imageHeight) / 2;
      RenderSystem.enableBlend();
      PoseStack matrixStack = graphics.pose();
      // safe
      matrixStack.pushPose();
      matrixStack.translate((float) u - 29.0f, (float) v + 14.0f, 0.0f);
      matrixStack.scale(0.5f, 0.5f, 1.0f);
      graphics.blit(SAFE, 0, 0, 0, 0, 70, 194);
      matrixStack.popPose();
      // scroll bar
      if (menu.items.getContainerSize() > 0) {
         if (!isMany) { RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1.0F); }
         // place
         matrixStack.pushPose();
         matrixStack.translate(u + 181.5, v + 18, 0.0f);
         matrixStack.scale(0.5f, 0.5f, 1.0f);
         graphics.blit(INVENTORY_ITEMS, 0, 0, 236, 0, 20, 170); // up
         graphics.blit(INVENTORY_ITEMS, 0, 170, 236, 86, 20, 170); // down
         matrixStack.popPose();
         // bar
         matrixStack.pushPose();
         matrixStack.translate(u + 181.5f, v + (isMany ? 14.5f + scrollBarY : 33.0f), 0.0f);
         matrixStack.scale(0.5f, 0.5f, 1.0f);
         graphics.blit(INVENTORY_ITEMS, 0, -30, 215, 0, 20, 30); // up
         graphics.blit(INVENTORY_ITEMS, 0, 0, 215, 30, 20, 31); // down
         matrixStack.popPose();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
      // Slots
      hoverSlot = null;
      if (!hasSubGui() && menu.items.getContainerSize() > 0) {
         int x = guiLeft + 7, xs;
         int y = guiTop + 20, ys;
         graphics.enableScissor(x, y, x + 162, y + 90);
         for (int s = 0; s < menu.items.getContainerSize(); s++) {
            Slot slot = menu.slots.get(s);
            xs = x + (s % 9) * 18;
            ys = y - scrollY + (int) (Math.floor((double) s / 9.0d) * 18.0d);
            if (ys > y - 17 && ys < y + 90) {
               graphics.blit(INVENTORY_ITEMS, xs, ys, 191, 1, 18, 18);
               xs += 1;
               ys += 1;
               if (slot.isActive()) { renderSlot(graphics, slot, xs, ys); }
               int yh = ys;
               int hy = 16;
               if (ys < y) { yh = y; hy = 16 - y + ys; }
               else if (ys > y + 72) { hy = Math.min(16, 90 - ys + y); }
               if (isMouseHover(mouseX, mouseY, xs, yh, 16, hy)) {
                  hoverSlot = slot;
                  if (hoverSlot.isHighlightable()) {
                     renderSlotHighlight(graphics, xs, ys, 0, getSlotColor(s));
                     RenderSystem.enableBlend();
                  }
               }
            }
         }
         graphics.disableScissor();
      }
      // upgrade / new tab
      if (!hasSubGui() && isOwner && (!stack.isEmpty() || money > 0 || donat > 0) &&
              menu.items.getContainerSize() < menu.slots.size()) {
         Slot slot = menu.getSlot(menu.items.getContainerSize());
         if (!stack.isEmpty()) {
            int xs = u + slot.x + 61;
            int ys = v + slot.y - 22;
            // background
            graphics.blit(INVENTORY_ITEMS, xs - 1, ys - 1, 190, 0, 20, 20);
            xs += 1;
            ys += 1;
            // item
            graphics.renderItem(stack, xs, ys);
            graphics.renderItemDecorations(font, stack, xs, ys);
            // info
            if (isMouseHover(mouseX, mouseY, xs, ys, 16, 16)) {
               List<Component> hover = new ArrayList<>();
               hover.add(Component.translatable("bank." + (canUpgrade ? "upg" : "tab") + ".cost.info"));
               hover.add(Component.literal("<br>"));
               hover.addAll(getTooltipFromContainerItem(stack));
               setHoverText(hover.toArray());
            }
         }
         if (money > 0 || donat > 0) {
            matrixStack.pushPose();
            matrixStack.translate(u + slot.x + 4, v + slot.y - (money > 0 && donat > 0 ? 27 : 22), 0.0f);
            float s = 16.0f / 250.f;
            matrixStack.scale(s, s, s);
            graphics.blit(GuiBasic.MONEY, 0, 0, 0, 0, 256, 256);
            if (donat > 0) {
               if (money > 0) { matrixStack.translate(0.0f, 192.0f, 0.0f); }
               graphics.blit(GuiBasic.DONAT, 0, 0, 0, 0, 256, 256);
            }
            matrixStack.popPose();
            if (money > 0 && isMouseHover(mouseX, mouseY, u + slot.x + 4, v + slot.y - (donat > 0 ? 25 : 20), 53, 12)) {
               setHoverText("bank.hover." + (canUpgrade ? "upgrade" : "open") + ".money");
            }
            if (donat > 0 && isMouseHover(mouseX, mouseY, u + slot.x + 4, v + slot.y - (money > 0 ? 13 : 20), 53, 12)) {
               setHoverText("bank.hover." + (canUpgrade ? "upgrade" : "open") + ".donat");
            }
         }
      }
      // creative manager
      if (player.isCreative()) {
         int x = guiLeft + imageWidth + 2;
         int y = guiTop + 10;
         graphics.blit(bg, x, y, 0, 0, 45, 71);
         graphics.blit(bg, x + 45, y, 131, 0, 45, 71);
      }
      // info
      if (getLabel(10) != null) {
         int i = menu.items.getCountEmpty();
         float f0 = menu.items.getContainerSize() == 0 ? 0.0f : (float) i / (float) menu.items.getContainerSize();
         Component text;
         if (menu.items.getContainerSize() == 0) { text = Component.translatable("bank.slots.empty"); }
         else { text = Component.translatable("bank.slots.info",
                 (f0 < 0.2 ? ((char) 167) + "c": f0 < 0.85 ? ((char) 167) + "e": "") + i,
                 "" + menu.items.getContainerSize()); }
         getLabel(10).setMessage(text);
      }
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      if (startXMouse != 0 && startYMouse != 0) {
         GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(),
                 startXMouse * minecraft.getWindow().getGuiScale(),
                 startYMouse * minecraft.getWindow().getGuiScale());
         startXMouse = 0;
         startYMouse = 0;
      }
      if (isWait) { drawWait(graphics); }
      // clear all items
      if (getButton(10) != null) { getButton(10).setIsEnabled(!menu.items.isEmpty()); }
      // background
      super.render(graphics, mouseX, mouseY, partialTicks);
      if (hoveredSlot == null && hoverSlot != null) { hoveredSlot = hoverSlot; }
      // open or upgrade check
      GuiButtonNop button = getButton(0);
      if (button != null && button.isVisible()) {
         List<Component> hover = new ArrayList<>();
         hover.add(Component.translatable("bank.hover.update." + canUpgrade,
                 ((char)167) + "6" + ceilsUpdate, ((char)167) + "6" + menu.items.getContainerSize(), ((char)167) + "6" + menu.data.bank.ceilSettings.get(menu.ceil).maxCells));
         boolean bo = true;
         if (!canUpgrade && menu.ceil > 0) {
            Bank.CeilSettings cs = menu.data.bank.ceilSettings.get(menu.ceil - 1);
            NpcMiscInventory invPre = menu.data.get(menu.ceil - 1);
            if (!cs.isFree && (invPre == null || invPre.getContainerSize() != cs.maxCells)) {
               if (!player.isCreative()) { hover.add(Component.translatable("gui.allowed")); bo = false; }
               hover.add(Component.translatable("bank.hover.update.not.open", "" + menu.ceil));
            }
         }
         if (!stack.isEmpty()) {
            Map<ItemStack, Integer> items = new HashMap<>();
            items.put(stack, stack.getCount() * ceilsUpdate);
            if (!Util.instance.canRemoveItems(player.inventoryMenu.getItems(), items, false, false)) {
               if (bo && !player.isCreative()) { hover.add(Component.translatable("gui.allowed")); bo = false; }
               hover.add(Component.translatable("hover.operation.not.items"));
               hover.add(Component.literal(stack.getHoverName().getString())
                       .append(Component.literal(" x" + stack.getCount() * ceilsUpdate).withStyle(ChatFormatting.DARK_RED)));
            }
         }
         PlayerData data = PlayerData.get(player);
         if (money > 0 && data.game.getMoney() < money * (long) ceilsUpdate) {
            if (bo && !player.isCreative()) { hover.add(Component.translatable("gui.allowed")); bo = false; }
            hover.add(Component.translatable("hover.operation.not.money"));
            hover.add(Component.literal(money * ceilsUpdate + CustomNpcs.displayCurrencies).withStyle(ChatFormatting.DARK_RED));
         }
         if (donat > 0 && data.game.getDonat() < donat * (long) ceilsUpdate) {
            if (bo && !player.isCreative()) { hover.add(Component.translatable("gui.allowed")); bo = false; }
            hover.add(Component.translatable("hover.operation.not.donat"));
            hover.add(Component.literal(donat * ceilsUpdate + CustomNpcs.displayDonation).withStyle(ChatFormatting.DARK_RED));
         }
         button.setIsEnabled(bo || player.isCreative());
         if (button.isHovered()) { setHoverText(hover); }
      }
      if (!hoverText.isEmpty()) { drawHoverText(null); }
      else { renderTooltip(graphics, mouseX, mouseY); }
   }

   @Override
   public void subGuiClosed(Screen subgui) {
      if (subgui instanceof SubGuiEditBankAccess gui) {
         boolean isChanged = false;
         if (menu.data.bank.isChanging != gui.isChanging) {
            menu.data.bank.isChanging = gui.isChanging;
            isChanged = true;
         }
         if (!menu.data.bank.owner.equals(gui.owner)) {
            menu.data.bank.owner = gui.owner;
            isChanged = true;
         }
         if (gui.names.size() != menu.data.bank.access.size()) {
            menu.data.bank.access.clear();
            menu.data.bank.access.addAll(gui.names);
            isChanged = true;
         }
         else {
            for (String name : gui.names) {
               if (menu.data.bank.access.contains(name)) { continue; }
               menu.data.bank.access.clear();
               menu.data.bank.access.addAll(gui.names);
               isChanged = true;
               break;
            }
         }
         if (isChanged) {
            isWait = true;
            menu.data.setChanged();
         }
      }
   }

   @Override
   public boolean keyPressed(int key, int key_1, int key_2) {
      if (isWait) { return false; }
      if (!hasSubGui() && isMany) {
         if (GuiBasic.isUpKey(key)) {
            resetRow(-18);
            return true;
         }
         if (GuiBasic.isDownKey(key)) {
            resetRow(+18);
            return true;
         }
      }
      return super.keyPressed(key, key_1, key_2);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (isWait) { return false; }
      isScrolling = false;
      if (!hasSubGui() && isMany) {
         double u = (double) guiLeft + (double) imageWidth - 17.0d;
         double v = ((double) height - (double) imageHeight) / 2.0d + 17.5d;
         if (isMouseHover(mouseX, mouseY, u, v, 10, 170.5d)) {
            isScrolling = true;
            mouseY -= 19.0d;
            if (mouseY <= 15.0d) { scrollY = 0; }
            else if (mouseY >= 155.0d) { scrollY = scrollMax; }
            else {
               mouseY -= 15.0d;
               scrollY = (int) (mouseY / 135.0d * (double) scrollMax);
            }
            resetRow(0);
            return true;
         }
      }
      return super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
      if (isWait) { return false; }
      if (!hasSubGui() && isScrolling && mouseButton == 0) {
         double y0 = ((double) height - (double) imageHeight) / 2.0d + 18.0d;
         double y1 = y0 + 170.0d;
         if (mouseY >= y0 && mouseY < y1) {
            mouseY -= 19.0d;
            if (mouseY <= 15.0d) { scrollY = 0; }
            else if (mouseY >= 155.0d) { scrollY = scrollMax; }
            else {
               mouseY -= 15.0d;
               scrollY = (int) (mouseY / 135.0d * (double) scrollMax);
            }
            resetRow(0);
            return true;
         }
      }
      return super.mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
      if (isWait) { return false; }
      if (!hasSubGui() && scrolled != 0) {
         int u = (width - imageWidth) / 2 - 37;
         int v = (height - imageHeight) / 2 + 14;
         if (isMouseHover(mouseX, mouseY, u, v, 70, 194)) {
            if ((scrolled > 0 && getButton(1) != null && getButton(1).isVisible()) ||
                    (scrolled < 0 && getButton(2) != null && getButton(2).isVisible())) {
               v = ValueUtil.correctInt(ceilPos - (int) scrolled, 0, menu.data.bank.ceilSettings.size() - 5);
               if (v < 0) { v = 0; }
               if (ceilPos != v) {
                  ceilPos = v;
                  init();
               }
            }
            return true;
         }
         if (isMany) {
            resetRow(scrolled > 0 ? -6 : 6);
            return true;
         }
      }
      return super.mouseScrolled(mouseX, mouseY, scrolled);
   }

   @Override
   public Slot findSlot(double mouseX, double mouseY, Slot foundSlot) {
      if (isWait) { return foundSlot; }
      if (menu.items.getContainerSize() > 0) {
         int x = guiLeft + 7, xs;
         int y = guiTop + 20, ys;
         for (int s = 0; s < menu.items.getContainerSize(); s++) {
            Slot slot = menu.slots.get(s);
            xs = x + (s % 9) * 18;
            ys = y - scrollY + (int) (Math.floor((double) s / 9.0d) * 18.0d);
            if (ys > y - 17 && ys < y + 90) {
               xs += 1;
               ys += 1;
               int yh = ys;
               int hy = 16;
               if (ys < y) { yh = y; hy = 16 - y + ys; }
               else if (ys > y + 72) { hy = Math.min(16, 90 - ys + y); }
               if (isMouseHover(mouseX, mouseY, xs, yh, 16, hy)) { return isWait ? null : slot; }
            }
         }
      }
      return foundSlot;
   }

   @Override
   public void onClose() {
      super.onClose();
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      startXMouse = wrapper.mouseX;
      startYMouse = wrapper.mouseY;
      if (ContainerNPCBank.editPlayerBankData != null && !ContainerNPCBank.editPlayerBankData.isEmpty()) {
         GuiNpcManagePlayerData gui = new GuiNpcManagePlayerData(npc);
         gui.selection = EnumPlayerData.Bank;
         gui.selectedPlayer = Component.literal(ContainerNPCBank.editPlayerBankData);
         NoppesUtil.openGUI(player, gui);
         Packets.sendServer(new SPacketPlayerDataGet(EnumPlayerData.Bank, ContainerNPCBank.editPlayerBankData));
         ContainerNPCBank.editPlayerBankData = null;
      }
   }

   private void renderSlot(@NotNull GuiGraphics graphics, Slot slotIn, int xPos, int yPos) {
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      Slot clickedSlot = ((IAbstractContainerScreenMixin) this).getClickedSlot();
      ItemStack draggingItem = ((IAbstractContainerScreenMixin) this).getDraggingItem();
      boolean isSplittingStack = ((IAbstractContainerScreenMixin) this).getIsSplittingStack();
      int quickCraftingType = ((IAbstractContainerScreenMixin) this).getQuickCraftingType();

      ItemStack itemstack = slotIn.getItem();
      boolean hovering = false;
      boolean flag1 = slotIn == clickedSlot && !draggingItem.isEmpty() && !isSplittingStack;
      ItemStack itemstack1 = menu.getCarried();
      String s = null;
      if (slotIn == clickedSlot && !draggingItem.isEmpty() && isSplittingStack && !itemstack.isEmpty()) {
         itemstack = itemstack.copyWithCount(itemstack.getCount() / 2);
      }
      else if (isQuickCrafting && quickCraftSlots.contains(slotIn) && !itemstack1.isEmpty()) {
         if (quickCraftSlots.size() == 1) { return; }
         if (AbstractContainerMenu.canItemQuickReplace(slotIn, itemstack1, true) && menu.canDragTo(slotIn)) {
            hovering = true;
            int k = Math.min(itemstack1.getMaxStackSize(), slotIn.getMaxStackSize(itemstack1));
            int l = slotIn.getItem().isEmpty() ? 0 : slotIn.getItem().getCount();
            int i1 = AbstractContainerMenu.getQuickCraftPlaceCount(quickCraftSlots, quickCraftingType, itemstack1) + l;
            if (i1 > k) {
               i1 = k;
               s = ChatFormatting.YELLOW.toString() + k;
            }
            itemstack = itemstack1.copyWithCount(i1);
         } else {
            quickCraftSlots.remove(slotIn);
            recalculateQuickCraftRemaining();
         }
      }
      graphics.pose().pushPose();
      graphics.pose().translate(0.0F, 0.0F, 100.0F);
      if (itemstack.isEmpty() && slotIn.isActive()) {
         Pair<ResourceLocation, ResourceLocation> pair = slotIn.getNoItemIcon();
         if (pair != null) {
            TextureAtlasSprite textureatlassprite = minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());
            graphics.blit(xPos, yPos, 0, 16, 16, textureatlassprite);
            flag1 = true;
         }
      }
      if (!flag1) {
         if (hovering) { graphics.fill(xPos, yPos, xPos + 16, yPos + 16, 0x80FFFFFF); }
         graphics.renderItem(itemstack, xPos, yPos, slotIn.x + slotIn.y * imageWidth);
         graphics.renderItemDecorations(font, itemstack, xPos, yPos, s);
      }
      graphics.pose().popPose();
   }

   private void recalculateQuickCraftRemaining() {
      ItemStack itemstack = this.menu.getCarried();
      int quickCraftingType = ((IAbstractContainerScreenMixin) this).getQuickCraftingType();
      if (!itemstack.isEmpty() && this.isQuickCrafting) {
         if (quickCraftingType == 2) {
            ((IAbstractContainerScreenMixin) this).setQuickCraftingRemainder(itemstack.getMaxStackSize());
         } else {
            ((IAbstractContainerScreenMixin) this).setQuickCraftingRemainder(itemstack.getCount());

            for(Slot slot : quickCraftSlots) {
               ItemStack itemstack1 = slot.getItem();
               int i = itemstack1.isEmpty() ? 0 : itemstack1.getCount();
               int j = Math.min(itemstack.getMaxStackSize(), slot.getMaxStackSize(itemstack));
               int k = Math.min(AbstractContainerMenu.getQuickCraftPlaceCount(quickCraftSlots, quickCraftingType, itemstack) + i, j);
               int quickCraftingRemainder = ((IAbstractContainerScreenMixin) this).getQuickCraftingRemainder();
               ((IAbstractContainerScreenMixin) this).setQuickCraftingRemainder(quickCraftingRemainder - (k - i));
            }

         }
      }
   }

   private void resetRow(int step) {
      if (!isMany) { return; }
      scrollY += step;
      if (scrollY < 0) { scrollY = 0; }
      else if (scrollY > scrollMax) { scrollY = scrollMax; }
      scrollBarY = (int) (((float) height - (float) imageHeight) / 2.0f + 17.0f + (float) scrollY / (float) scrollMax * 141.0f);
   }

   public static class GuiSafeButton extends GuiButtonNop {

      protected GuiNPCBankChest listener;
      protected final int ceil;
      protected final boolean isCeil;
      protected long ticks;
      protected long lastTick = System.currentTimeMillis();

      public GuiSafeButton(GuiNPCBankChest gui, int buttonId, Object label, int x, int y, int w, int h, int ceilIn) {
         super(gui, buttonId, label, x, y, null);
         width = w;
         height = h;
         listener = gui;

         isCeil = h == 14;
         ceil = ceilIn;
         txrX = 70;
         txrY = isCeil ? 0 : 28;
         txrW = w * 2;
         txrH = h * 2;
         ticks = listener.menu.ceil == ceil ? 500 : 0;
      }

      @Override
      public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         if (!visible) { return; }
         Minecraft mc = Minecraft.getInstance();
         isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         RenderSystem.enableBlend();
         PoseStack matrixStack = graphics.pose();
         // animation
         matrixStack.pushPose();
         matrixStack.translate(getX(), getY(), 0);
         matrixStack.scale(0.5f, 0.5f, 1.0f);
         // door
         if (isCeil) { drawDoor(graphics, listener.menu.ceil == ceil || isHovered); }
         else {
            if (isHovered) {
               float f = ((IMouseHandlerMixin) mc.mouseHandler).getActiveButton() == 0 ? 0.5f : 0.75f;
               RenderSystem.setShaderColor(f, f, f, alpha);
            }
            graphics.blit(SAFE, 0, 0, txrX, txrY, txrW, txrH);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         } // up or down
         matrixStack.popPose();
         // text
         matrixStack.pushPose();
         Component mes = getMessage();
         renderString(graphics, getMessage(), getX() + (isCeil ? - mc.font.width(mes) : 2), getY(),
                 getX() + (isCeil ? 0 : getWidth() - 2), getY() + getHeight(),
                 getFGColor() | Mth.ceil(alpha * 255.0F) << 24, showShadow, true, customFont);
         matrixStack.popPose();
      }

      private void drawDoor(GuiGraphics graphics, boolean isOpen) {
         // inside
         float f0 = 0.0f;
         NpcMiscInventory inv = listener.menu.data.get(ceil);
         if (inv != null) {
            f0 = inv.getContainerSize() == 0 ? 1.0f : (float) inv.getCountEmpty() / (float) inv.getContainerSize();
         }
         int y = f0 >= 0.95f ? 106 : f0 <= 0.2f ? 50 : 78;
         graphics.blit(SAFE, 0, 0, txrX, y, txrW, txrH);
         double d0 = (double) ticks;
         PoseStack matrixStack = graphics.pose();
         if (d0 < 0) { ticks = 0; d0 = 0; }
         if (d0 == 0.0d) { graphics.blit(SAFE, 0, 0, txrX, txrY, txrW, txrH); }
         else if (d0 < 500.0d) {
            double d1 = Math.sin(Math.toRadians(90 * d0 / 500.0d));
            if (d1 < 0.5d) {
               f0 = (float) ValueUtil.correctDouble(-4.7d * d1 + 4.0d, 1.65d, 4.0d);
               double f1 = ValueUtil.correctDouble(85.714286d * Math.pow(d1, 3.0d) + 7.142857d * Math.pow(d1, 2.0d) + 17.0d * d1,
                       0.0d, 21.0f);
               matrixStack.scale(f0, 1.0f, 1.0f);
               matrixStack.translate(f1, -4.0d, 1.0d);
               graphics.blit(SAFE, 0, 0, 136, 0, 20, 41);
               matrixStack.pushPose();
               matrixStack.scale(2.0f * (float) d1, 1.0f, 1.0f);
               graphics.blit(SAFE, -6, 0, 130, 0, 6, 41);
               matrixStack.popPose();
            }
            else {
               f0 = (float) ValueUtil.correctDouble(-1.7329d * d1 + 2.7329d, 1.0d, 1.86645d);
               double f1 = ValueUtil.correctDouble(38.50667d * Math.pow(d1, 3.0d) - 28.824 * Math.pow(d1, 2.0d) + 29.395333d * d1 - 1.078d,
                       11.227d, 38.0d);
               matrixStack.scale(f0, 1.0f, 1.0f);
               matrixStack.translate(f1, -4.0f, 1.0f);
               graphics.blit(SAFE, 0, 0, 130, 0, 26, 41);
            }
         }
         else {
            matrixStack.translate(38.0f, -4.0d, 1.0d);
            graphics.blit(SAFE, 0, 0, 130, 0, 26, 41);
         }
         long l = System.currentTimeMillis() - lastTick;
         ticks = ValueUtil.correctLong(ticks + (isOpen ? l : -l), 0L, 500L);
         lastTick = System.currentTimeMillis();
      }

   }

}
