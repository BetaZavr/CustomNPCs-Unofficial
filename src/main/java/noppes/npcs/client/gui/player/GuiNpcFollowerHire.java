package noppes.npcs.client.gui.player;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.containers.ContainerNPCFollowerHire;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketFollowerHire;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class GuiNpcFollowerHire extends GuiContainerNPCInterface<ContainerNPCFollowerHire> {

   protected final RoleFollower role;

   public GuiNpcFollowerHire(ContainerNPCFollowerHire container, Inventory inv, Component titleIn) {
      super(NoppesUtilServer.getEditingNpc(Minecraft.getInstance().player), container, inv, titleIn);
      setBackground("followerhire.png");

      role = (RoleFollower) npc.role;
   }

   @Override
   public void init() {
      super.init();
      int x = guiLeft + 26;
      int y = guiTop - 7;
      for (int i = 0; i < 3; ++i) {
         if (!role.rentalItems.getItem(i).isEmpty()) {
            addButton(i, x, y += 18, "follower.hire")
                 .setSize(50, 14);
         }
      }
      if (role.rates.containsKey(3) && role.rentalMoney > 0) {
         addButton(3, x, guiTop + 65, "follower.hire")
                 .setSize(50, 14);
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      Packets.sendServer(new SPacketFollowerHire(button.id));
      onClose();
   }

   @Override
   protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
      int l = (width - imageWidth) / 2;
      int i1 = (height - imageHeight) / 2;
      graphics.blit(background, l, i1, 0, 0, imageWidth, imageHeight);
      int index = 0;
      for(int slot = 0; slot < role.rentalItems.getContainerSize(); ++slot) {
         ItemStack itemstack = role.rentalItems.getItem(slot);
         if (!NoppesUtilServer.isItemStackNull(itemstack)) {
            int days = 1;
            if (role.rates.containsKey(slot)) { days = role.rates.get(slot); }
            int yOffset = index * 18;
            int x = guiLeft + 89;
            int y = guiTop + yOffset + 10;
            graphics.blit(GuiBasic.RESOURCE_SLOT, x - 1, y - 1, 0, 0, 18, 18);
            graphics.renderItem(itemstack, x, y);
            graphics.renderItemDecorations(font, itemstack, x, y);
            Component daysS = Component.empty()
                    .append(" = " + days + " ")
                    .append(Component.translatable(days == 1 ? "follower.day": "follower.days"));
            graphics.drawString(font, daysS, x + 16, y + 4,
                    CustomNpcResourceListener.DefaultTextColor,
                    false);
            if (isMouseHover(mouseX, mouseY, x, y, 16, 16)) {
               graphics.renderTooltip(font, itemstack, mouseX, mouseY);
            }
            ++index;
         }
      }
      if (role.rates.containsKey(3) && role.rentalMoney > 0) {
         int days = role.rates.get(3);
         Component daysS = Component.empty()
                 .append(Util.instance.getTextReducedNumber(role.rentalMoney, true, true, false))
                 .append(" " + CustomNpcs.displayCurrencies + " = " + days + " ")
                 .append(Component.translatable(days == 1 ? "follower.day": "follower.days"));
         graphics.drawString(font, daysS, guiLeft + 90, guiTop + 69,
                 CustomNpcResourceListener.DefaultTextColor,
                 false);
      }
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics,  int mouseX, int mouseY, float partialTicks) {
      for (int i = 0; i < 3; ++i) {
         if (getButton(i) != null) {
            getButton(i).setIsEnabled(player.isCreative() || Util.instance.canRemoveItems(player.getInventory().items, role.rentalItems.getItem(i), false, false));
         }
      }
      if (getButton(3) != null) {
         getButton(3).setIsEnabled(player.isCreative() || PlayerData.get(player).game.getMoney() >= role.rentalMoney);
      }
      for (int i = 0; i < 4; ++i) {
         if (getButton(i) != null && getButton(i).isHoveredOrFocused()) {
            List<Component> hover = new ArrayList<>();
            hover.add(Component.translatable("follower.hover.hire.info"));
            if (role.disableGui) { hover.add(Component.translatable("follower.hover.disable.gui").withStyle(ChatFormatting.GRAY)); }
            if (role.infiniteDays) { hover.add(Component.translatable("follower.hover.infinite")); }
            setHoverText(hover);
         }
      }
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

}
