package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.containers.ContainerNPCFollowerHire;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketFollowerExtend;
import noppes.npcs.packets.server.SPacketFollowerState;
import noppes.npcs.packets.server.SPacketNpcRoleGet;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class GuiNpcFollower extends GuiContainerNPCInterface<ContainerNPCFollowerHire>
        implements IGuiData {

   protected final RoleFollower role;

   // New from Unofficial (BetaZavr)
   protected static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/components.png");
   protected static final ResourceLocation background_2 = new ResourceLocation(CustomNpcs.MODID, "textures/gui/followerhire.png");
   protected EntityNPCInterface displayNPC;
   protected final int index;
   protected GuiLabel healthL;
   protected GuiLabel hireTimeL;
   protected GuiLabel timeL;
   protected GuiLabel moneyL;

   public GuiNpcFollower(ContainerNPCFollowerHire container, Inventory inv, Component titleIn) {
      super(NoppesUtilServer.getEditingNpc(Minecraft.getInstance().player), container, inv, titleIn);
      setBackground("follower.png");

      role = (RoleFollower) npc.role;
      // New from Unofficial (BetaZavr)
      imageHeight = role.inventory.getContainerSize() > 0 ? 230 : 146;
      int i = 0;
      if (role.infiniteDays) { i = 4; }
      else {
         for (int slot = 0; slot < role.rentalItems.getContainerSize(); ++slot) {
            ItemStack itemstack = role.rentalItems.getItem(slot);
            if (NoppesUtilServer.isItemStackNull(itemstack)) { i++; }
         }
         if (!role.rates.containsKey(3) || role.rentalMoney == 0) { i += 1; }
      }
      index = i;
      imageHeight -= 16 * index;
      Packets.sendServer(new SPacketNpcRoleGet());
      displayNPC = Util.instance.copyToGUI(npc, player.level(), false);
   }

   @Override
   public void init() {
      super.init();
      int lId = 0;
      int x = guiLeft + 7;
      int y = guiTop - 11;
      if (!role.infiniteDays) {
         for (int slot = 0; slot < 3; ++slot) {
            if (role.rentalItems.getItem(slot).isEmpty()) { continue; }
            addButton(slot, x, y += 16, "follower.extend")
                    .setSize(54, 14)
                    .setHoverTexts("follower.hover.extend");
            int days = role.rates.get(slot);
            addLabel(lId++, x + 79, y + 2, Component.literal("= " + days + " ")
                    .append(Component.translatable(days == 1 ? "follower.day": "follower.days")))
                    .setSize(90, 10)
                    .setHoverTexts(Component.translatable("gui.time")
                            .append(" ≈ ")
                            .append(Util.instance.ticksToElapsedTime(days * 28800L, false, true, false)));
         }
         if (role.rates.containsKey(3) && role.rentalMoney > 0) {
            addButton(3, x, y += 16, "follower.extend")
                    .setSize(54, 14)
                    .setHoverTexts("follower.hover.extend");
            moneyL = addLabel(lId++, x + 56, y + 2, Component.literal(Util.instance.getTextReducedNumber(role.rentalMoney, true, true, false))
                    .append(role.rentalIsDonat ? CustomNpcs.displayCurrencies : CustomNpcs.displayDonation));
            int days = role.rates.get(3);
            addLabel(lId++, moneyL.getX() + moneyL.getWidth() + 14, y + 2, Component.literal("= " + days + " ")
                    .append(Component.translatable(days == 1 ? "follower.day": "follower.days")))
                    .setSize(90 - moneyL.getWidth(), 10)
                    .setHoverTexts(Component.translatable("gui.time")
                            .append(" ≈ ")
                            .append(Util.instance.ticksToElapsedTime(days * 28800L, false, true, false)));
         }
      }
      // info
      x += 57;
      y = guiTop + 70 - index * 16;
      healthL = addLabel(lId++, x, y, "follower.health");
      if (!role.infiniteDays) { hireTimeL = addLabel(lId++, x, y += 10, "follower.days"); }
      else { hireTimeL = null; }
      timeL = addLabel(lId, x, y += 10, "follower.days");
      // buttons
      x--;
      addButton(5, x, y += 12, false, role.isFollowing ? 0 : 1, "follower.waiting", "follower.following")
              .setSize(50, 14)
              .setHoverTexts("follower.hover.move");
      addButton(6, x + 54, y, "follower.fire")
              .setSize(50, 14)
              .setHoverTexts("follower.hover.fire");
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 5: Packets.sendServer(new SPacketFollowerState(true)); break;
         case 6: Packets.sendServer(new SPacketFollowerState(false)); onClose(); break;
         default: Packets.sendServer(new SPacketFollowerExtend(button.id)); break;
      }
   }

   @Override
   protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
      if (healthL != null) {
         healthL.setMessage(Component.translatable("follower.health")
                 .append(": " + (int) Math.ceil(npc.getHealth() / npc.getMaxHealth() * 100) + "%"));
         healthL.setSize(108, 10);
      }
      long time = role.getCurrentTime() - role.hiredTime;
      if (hireTimeL != null) {
         hireTimeL.setMessage(Component.translatable("follower.daysleft")
                 .append(" " + Util.instance.ticksToElapsedTime(role.getDays() * 28800L - time, false, true, false)));
         hireTimeL.setSize(108, 10);
      }
      if (timeL != null) {
         timeL.setMessage(Component.translatable("follower.lastday")
                         .append(": " + Util.instance.ticksToElapsedTime(time, false, true, false)));
         timeL.setSize(108, 10);
      }
      int y = 71 - index * 16;
      graphics.drawString(font, Component.translatable("container.inventory"),
              8, y + 70, CustomNpcResourceListener.DefaultTextColor, false);
      graphics.drawString(font, Component.translatable("follower.bag"),
              imageWidth - 3, y + 70, CustomNpcResourceListener.DefaultTextColor, false);
   }

   @Override
   protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
      // background
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      matrixStack.translate(guiLeft, guiTop, 0.0f);
      graphics.blit(background, 0, 0, 0, 0, imageWidth, 4);
      matrixStack.translate(0.0f,  4.0f, 0.0f);
      if (index != 0) {
         int h = (4 - index) * 16;
         graphics.blit(background, 0, 0, 0, 4, imageWidth, h);
         matrixStack.translate(0.0f,  h, 0.0f);
      }
      graphics.blit(background, 0, 0, 0, 67, imageWidth, 79);
      matrixStack.popPose();
      if (!role.infiniteDays) {
         for (int slot = 0; slot < role.rentalItems.getContainerSize(); ++slot) {
            ItemStack itemstack = role.rentalItems.getItem(slot);
            GuiButtonNop button = getButton(slot);
            if (!NoppesUtilServer.isItemStackNull(itemstack) && button != null) {
               int x = button.getX() + button.getWidth() + 3;
               int y = button.getY() + 1;
               matrixStack.pushPose();
               matrixStack.translate(x - 1.0f, y - 1.0f, 0.0f);
               matrixStack.scale(0.8f, 0.8f, 0.8f);
               graphics.blit(GuiBasic.RESOURCE_SLOT, 0, 0, 0, 0, 18, 18);
               matrixStack.popPose();
               GuiBasic.renderStackInGUI(graphics, itemstack, x, y, 0.0f, 0.8f, null);
               if (isMouseHover(mouseX, mouseY, x, y, 13, 13)) {
                  graphics.renderTooltip(font, getTooltipFromContainerItem(itemstack), itemstack.getTooltipImage(), itemstack, mouseX, mouseY);
               }
            }
         }
         if (role.rates.containsKey(3) && role.rentalMoney > 0 && moneyL != null) {
            // icon
            matrixStack.pushPose();
            matrixStack.translate(moneyL.getX() + moneyL.getWidth() - 3.0f, moneyL.getY() - 2.0f, 0.0f);
            float sc = 16.0f / 250.f;
            matrixStack.scale(sc, sc, sc);
            graphics.blit(role.rentalIsDonat ? GuiBasic.DONAT : GuiBasic.MONEY, 0, 0, 0, 0, 256, 256);
            matrixStack.popPose();
         }
      }
      int invSize = role.inventory.getContainerSize();
      if (invSize > 0) {
         // down background (player inventory)
         matrixStack.pushPose();
         matrixStack.translate(guiLeft - 3.0f, guiTop + imageHeight - 88.0f, 0.0f);
         graphics.blit(background_2, 3, 0, 0, 75, 176, 91);
         matrixStack.popPose();
         // inv background
         int uSize = 8 + (invSize < 4 ? 18 : invSize == 4 ? 36 : 54);
         matrixStack.pushPose();
         matrixStack.translate(guiLeft + imageWidth - 4.0f, guiTop + imageHeight - 95.0f, 0.0f);
         graphics.blit(background, 3, 0, 179 - uSize, 0, uSize - 3, 1);
         graphics.blit(background, 2, 1, 178 - uSize, 1, uSize - 2, 1);
         graphics.blit(background, 1, 2, 177 - uSize, 2, uSize - 1, 1);
         graphics.blit(background, 0, 3, 176 - uSize, 3, uSize, 47);
         graphics.blit(background, 0, 50, 176 - uSize, 98, uSize, 47);
         matrixStack.popPose();
         // slots
         matrixStack.pushPose();
         matrixStack.translate(guiLeft - 1.0f, guiTop - 1.0f, 0.0f);
         for (int slotId = 0; slotId < invSize; slotId++) {
            Slot slot = menu.getSlot(slotId);
            graphics.blit(GuiBasic.RESOURCE_SLOT, slot.x, slot.y, 0, 0, 18, 18);
         }
         matrixStack.popPose();
      }
      // stats
      GuiButtonNop button = getButton(6);
      if (button != null) {
         matrixStack.pushPose();
         matrixStack.translate(button.getX() + button.getWidth() - 12.0f, button.getY() + button.getHeight() + 2.0f, 0.0f);
         matrixStack.scale(0.5f, 0.5f, 0.5f);
         graphics.blit(resource, 0, 0, 98, 48, 24, 24);
         matrixStack.popPose();
      }
      if (displayNPC != null) { drawNpc(graphics, displayNPC, 33, 131 - index * 16, 1.0f, 0, 0, 1); }
      else { drawNpc(graphics, 33, 131); }
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      GuiButtonNop button;
      for (int i = 0; i < 3; ++i) {
         button = getButton(i);
         if (button != null) {
            ItemStack stack = role.rentalItems.getItem(i);
            int count = Util.instance.inventoryItemCount(player, stack, null, false, false);
            button.setIsEnabled(player.isCreative() || count >= stack.getCount())
                    .setHoverTexts("follower.hover.extend",
                            ((MutableComponent) stack.getDisplayName())
                                    .append(" ")
                                    .append(Component.translatable("market.hover.item.amount", count)));
         }
      }
      button = getButton(3);
      if (button != null) {
         long count = role.rentalIsDonat ? PlayerData.get(player).game.getDonat() : PlayerData.get(player).game.getMoney();
         button.setIsEnabled(player.isCreative() || count >= role.rentalMoney)
                 .setHoverTexts("follower.hover.extend",
                         Component.translatable(role.rentalIsDonat ? "gui.donat" : "gui.money")
                         .append(" ")
                         .append(Component.translatable("market.hover.item.amount", count)));
      }
      // stats
      button = getButton(6);
      if (button != null && isMouseHover(mouseX, mouseY, button.getX() + button.getWidth() - 12.0f, button.getY() + button.getHeight() + 2.0f, 12, 12)) {
         setHoverText(Component.translatable("stats.health").append(": " + (int) npc.getMaxHealth()),
                 Component.translatable("stats.hover.attack.strength").append(": "),
                 Component.literal(" - ").append(Component.translatable("stats.meleeproperties")).append(": " + npc.stats.getMelee().getStrength()),
                 Component.literal(" - ").append(Component.translatable("stats.rangedproperties")).append(": " + npc.stats.getRanged().getStrength()),
                 Component.translatable("stats.movespeed").append(": " + npc.ais.getWalkingSpeed()));
      }
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      npc.role.load(compound);
      init();
   }

}