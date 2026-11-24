package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.containers.ContainerNPCFollowerHire;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketFollowerHire;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class GuiNpcFollowerHire extends GuiContainerNPCInterface<ContainerNPCFollowerHire> {

   protected final RoleFollower role;

   // New from Unofficial (BetaZavr)
   protected static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/components.png");
   protected GuiLabel moneyL;

   public GuiNpcFollowerHire(ContainerNPCFollowerHire container, Inventory inv, Component titleIn) {
      super(NoppesUtilServer.getEditingNpc(Minecraft.getInstance().player), container, inv, titleIn);
      setBackground("followerhire.png");

      role = (RoleFollower) npc.role;
   }

   @Override
   public void init() {
      super.init();
      int lId = 0;
      int x = guiLeft + 26;
      int y = guiTop - 7;
      for (int slot = 0; slot < 3; ++slot) {
         if (!role.rentalItems.getItem(slot).isEmpty()) {
            addButton(slot, x, y += 18, "follower.hire")
                 .setSize(54, 14);
            int days = role.rates.get(slot);
            addLabel(lId++, x + 79, y + 2, Component.literal("= " + days + " ")
                    .append(Component.translatable(days == 1 ? "follower.day": "follower.days")))
                    .setSize(66, 10)
                    .setIsVisible(!role.infiniteDays)
                    .setHoverTexts(Component.translatable("gui.time")
                            .append(" ≈ ")
                            .append(Util.instance.ticksToElapsedTime(days * 28800L, false, true, false)));
         }
      }
      if (role.rates.containsKey(3) && role.rentalMoney > 0) {
         addButton(3, x, y += 18, "follower.hire")
                 .setSize(54, 14)
                 .setHoverTexts("follower.hover.extend");
         moneyL = addLabel(lId++, x + 56, y + 2, Component.literal(Util.instance.getTextReducedNumber(role.rentalMoney, true, true, false))
                 .append(role.rentalIsDonat ? CustomNpcs.displayCurrencies : CustomNpcs.displayDonation));
         int days = role.rates.get(3);
         addLabel(lId, moneyL.getX() + moneyL.getWidth() + 14, y + 2, Component.literal("= " + days + " ")
                 .append(Component.translatable(days == 1 ? "follower.day": "follower.days")))
                 .setSize(66 - moneyL.getWidth(), 10)
                 .setIsVisible(!role.infiniteDays)
                 .setHoverTexts(Component.translatable("gui.time")
                         .append(" ≈ ")
                         .append(Util.instance.ticksToElapsedTime(days * 28800L, false, true, false)));
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
      for(int slot = 0; slot < role.rentalItems.getContainerSize(); ++slot) {
         ItemStack itemstack = role.rentalItems.getItem(slot);
         GuiButtonNop button = getButton(slot);
         if (!NoppesUtilServer.isItemStackNull(itemstack) && button != null) {
            int x = button.getX() + button.getWidth() + 3;
            int y = button.getY() - 1;
            graphics.blit(GuiBasic.RESOURCE_SLOT, x - 1, y - 1, 0, 0, 18, 18);
            GuiBasic.renderStackInGUI(graphics, itemstack, x, y, 0.0f, 1.0f, null);
            if (isMouseHover(mouseX, mouseY, x, y, 16, 16)) {
               graphics.renderTooltip(font, itemstack, mouseX, mouseY);
            }
         }
      }
      PoseStack matrixStack = graphics.pose();
      if (role.rates.containsKey(3) && role.rentalMoney > 0) {
         // icon
         matrixStack.pushPose();
         matrixStack.translate(moneyL.getX() + moneyL.getWidth() - 3.0f, moneyL.getY() - 2.0f, 0.0f);
         float sc = 16.0f / 250.f;
         matrixStack.scale(sc, sc, sc);
         graphics.blit(role.rentalIsDonat ? GuiBasic.DONAT : GuiBasic.MONEY, 0, 0, 0, 0, 256, 256);
         matrixStack.popPose();
      }
      // stats
      matrixStack.pushPose();
      matrixStack.translate(guiLeft + 8.0f, guiTop + 12.0f, 0.0f);
      matrixStack.scale(0.5f, 0.5f, 0.5f);
      graphics.blit(resource, 0, 0, 98, 48, 24, 24);
      matrixStack.popPose();
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
      // stats
      if (isMouseHover(mouseX, mouseY, guiLeft + 8, guiTop + 12, 12, 12)) {
         setHoverText(Component.translatable("stats.health").append(": " + (int) npc.getMaxHealth()),
                 Component.translatable("stats.hover.attack.strength").append(": "),
                 Component.literal(" - ").append(Component.translatable("stats.meleeproperties")).append(": " + npc.stats.getMelee().getStrength()),
                 Component.literal(" - ").append(Component.translatable("stats.rangedproperties")).append(": " + npc.stats.getRanged().getStrength()),
                 Component.translatable("stats.movespeed").append(": " + npc.ais.getWalkingSpeed()));
      }
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

}
