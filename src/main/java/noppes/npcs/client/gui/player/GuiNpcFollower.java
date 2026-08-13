package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
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
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class GuiNpcFollower extends GuiContainerNPCInterface<ContainerNPCFollowerHire>
        implements IGuiData {

   protected final RoleFollower role;

   // New from Unofficial (BetaZavr)
   protected EntityNPCInterface displayNPC;

   public GuiNpcFollower(ContainerNPCFollowerHire container, Inventory inv, Component titleIn) {
      super(NoppesUtilServer.getEditingNpc(Minecraft.getInstance().player), container, inv, titleIn);
      setBackground("follower.png");

      role = (RoleFollower) npc.role;
      Packets.sendServer(new SPacketNpcRoleGet());
      // New from Unofficial (BetaZavr)
      displayNPC = Util.instance.copyToGUI(npc, player.level(), false);
   }

   @Override
   public void init() {
      super.init();
      int x = guiLeft + 12;
      int y = guiTop - 11;
      if (!role.infiniteDays) {
         for (int i = 0; i < 3; ++i) {
            if (role.rentalItems.getItem(i).isEmpty()) { continue; }
            addButton(i, x, y += 16, "follower.extend")
                    .setSize(60, 13)
                    .setHoverTexts("follower.hover.extend");
         }
      }
      if (role.rates.containsKey(3) && role.rentalMoney > 0) {
         addButton(3, x, guiTop + 53, "follower.extend")
                 .setSize(60, 13)
                 .setHoverTexts("follower.hover.extend");
      }
      x += 52;
      y = guiTop + 105;
      addButton(5, x, y, false, role.isFollowing ? 0 : 1, "follower.waiting", "follower.following")
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
   protected void renderLabels(@NotNull GuiGraphics graphics, int x, int y) {
      long time = (System.currentTimeMillis() - role.hiredTime) / 50L;
      graphics.drawString(font, Component.translatable("follower.health")
              .append(": " + npc.getHealth() + "/" + npc.getMaxHealth()), 62, 70, CustomNpcResourceListener.DefaultTextColor);
      if (!role.infiniteDays) {
         graphics.drawString(font, Component.translatable("follower.daysleft")
                 .append(" " + Util.instance.ticksToElapsedTime((role.getDays() * 28800L) - time, false, true, false)), 62, 82, CustomNpcResourceListener.DefaultTextColor);
      }
      graphics.drawString(font, Component.translatable("follower.lastday")
              .append(": " + Util.instance.ticksToElapsedTime(time, false, true, false)), 62, 94, CustomNpcResourceListener.DefaultTextColor);
   }

   @Override
   protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
      super.renderBg(graphics, partialTicks, mouseX, mouseY);
      int index = 0;
      if (!role.infiniteDays) {
         for (int slot = 0; slot < role.rentalItems.getContainerSize(); ++slot) {
            ItemStack itemstack = role.rentalItems.getItem(slot);
            if (!NoppesUtilServer.isItemStackNull(itemstack)) {
               int days = 1;
               if (role.rates.containsKey(slot)) { days = role.rates.get(slot); }
               int yOffset = index * 16;
               int x = guiLeft + 68;
               int y = guiTop + yOffset + 4;
               graphics.renderItem(itemstack, x + 11, y);
               graphics.renderItemDecorations(font, itemstack, x + 11, y);

               Component daysS = Component.empty()
                       .append(" = " + days + " ")
                       .append(Component.translatable(days == 1 ? "follower.day": "follower.days"));
               graphics.drawString(font, daysS, x + 27, y + 4,
                       CustomNpcResourceListener.DefaultTextColor);
               if (isMouseHover(mouseX, mouseY, x - guiLeft + 11, y - guiTop, 16, 16)) {
                  graphics.renderTooltip(font, getTooltipFromContainerItem(itemstack), itemstack.getTooltipImage(), itemstack, mouseX, mouseY);
               }
               ++index;
            }
         }
      }
      int size = role.inventory.getContainerSize();
      if (size > 0) {
         int s = (size == 2 || size == 4) ? 2 : 3;
         PoseStack matrixStack = graphics.pose();
         matrixStack.pushPose();
         matrixStack.translate(guiLeft + 172, guiTop + 135, 0.0f);
         graphics.blit(background, 3, 0, 118, 0, 58, 1);
         graphics.blit(background, 2, 1, 117, 1, 59, 1);
         graphics.blit(background, 1, 2, 116, 2, 60, 1);
         graphics.blit(background, 0, 3, 115, 3, 61, 82);
         graphics.blit(background, 0, 85, 115, 220, 61, 4);
         matrixStack.popPose();

         matrixStack.pushPose();
         matrixStack.translate(guiLeft + 173, guiTop + 141, 0.0f);
         for (int slotId = 0; slotId < size; slotId++) {
            graphics.blit(GuiBasic.RESOURCE_SLOT, (slotId % s) * 18, (slotId / s) * 18, 0, 0, 18, 18);
         }
         matrixStack.popPose();
      }
      if (role.rates.containsKey(3) && role.rentalMoney > 0) {
         int days = role.rates.get(3);
         Component daysS = Component.empty()
                 .append(Util.instance.getTextReducedNumber(role.rentalMoney, true, true, false))
                 .append(" " + CustomNpcs.displayCurrencies + " = " + days + " ")
                 .append(Component.translatable(days == 1 ? "follower.day": "follower.days"));
         graphics.drawString(font, daysS, guiLeft + 80, guiTop + 56, CustomNpcResourceListener.DefaultTextColor);
      }
      if (displayNPC != null) { drawNpc(graphics, displayNPC, 33, 131, 1.0f, 0, 0, 1); }
      else { drawNpc(graphics, 33, 131); }
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      for (int i = 0; i < 3; ++i) {
         if (getButton(i) == null) {
            getButton(i).setIsEnabled(player.isCreative() || Util.instance.canRemoveItems(player.getInventory().items, role.rentalItems.getItem(i), false, false));
         }
      }
      if (getButton(3) != null) {
         getButton(3).setIsEnabled(player.isCreative() || PlayerData.get(player).game.getMoney() >= role.rentalMoney);
      }
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      npc.role.load(compound);
      init();
   }

}