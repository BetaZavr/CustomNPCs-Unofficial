package noppes.npcs.client.gui.roles;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface2;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerNPCFollowerSetup;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcRoleSave;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import org.jetbrains.annotations.NotNull;

public class GuiNpcFollowerSetup
        extends GuiContainerNPCInterface2<ContainerNPCFollowerSetup> implements ITextfieldListener {

   protected final RoleFollower role;

   public GuiNpcFollowerSetup(ContainerNPCFollowerSetup container, Inventory inv, Component titleIn) {
      super(NoppesUtilServer.getEditingNpc(Minecraft.getInstance().player), container, inv, titleIn);
      setBackground("followersetup.png");
      imageHeight = 200;

      backGui = EnumGuiType.MainMenuAdvanced;
      role = (RoleFollower) npc.role;
   }

   @Override
   public void init() {
      super.init();
      int x = guiLeft + 66;
      int y = guiTop + 39;
      int lId = 0;
      // item days
      addLabel(lId++, x - 22, y - 12, Component.translatable("follower.hire").append(":"))
              .setSize(48, 10);
      int days;
      for (int i = 0; i < 3; ++i) {
         days = role.rates.getOrDefault(i, 1);
         addLabel(lId++, x - 39, y + i * 25 + 4, "#" + (i + 1))
                 .setSize(15, 10);
         addTextField(i, x, y + i * 25, 24, 16, "" + days)
                 .setMinMaxDefault(1, Integer.MAX_VALUE, days)
                 .setIsEnabled(!role.infiniteDays)
                 .setHoverTexts(Component.translatable("follower.hover.days", "" + (i + 1)));
      }
      x += 34;
      y -= 33;
      // messages
      Component jsonMess = Component.translatable("follower.hover.mes.json");
      addTextField(3, x, y, 286, 12, role.dialogHire)
              .setHoverTexts(Component.translatable("follower.hover.mes.hire").append(jsonMess));
      addTextField(4, x, y += 15, 286, 12, role.dialogFarewell)
              .setHoverTexts(Component.translatable("follower.hover.mes.let.go").append(jsonMess));
      addTextField(5, x, y += 15, 286, 12, role.dialogFired)
              .setHoverTexts(Component.translatable("follower.hover.mes.fired").append(jsonMess));
      addTextField(9, x, y += 15, 286, 12, role.dialogHired)
              .setHoverTexts(Component.translatable("follower.hover.mes.hired").append(jsonMess));
      x--;
      // type
      addCheckBox(7, x, y += 15, "follower.infiniteDays", null, role.infiniteDays)
              .setSize(286, 12)
              .setHoverTexts("follower.hover.infinite");
      addCheckBox(8, x, y += 14, "follower.guiDisabled", null, role.disableGui)
              .setSize(286, 12)
              .setHoverTexts("follower.hover.disable.gui");
      addCheckBox(9, x, y += 14, "follower.allowSoulstone", null, !role.refuseSoulStone)
              .setSize(286, 12)
              .setHoverTexts(Component.translatable("follower.hover.soulstone",
                      Component.translatable("item.customnpcs.npcsoulstoneempty").getString()));
      // money
      addLabel(lId++, x += 73, y += 19, Component.translatable("follower.hire").append(":"))
              .setSize(72, 10);
      addCheckBox(11, x + 74, y - 3,
              Component.translatable("gui.donat").append(":"),
              Component.translatable("gui.money").append(":"), role.rentalIsDonat)
              .setSize(70, 12);
      addLabel(lId++, x, (y += 11)+ 4, "#4")
              .setSize(15, 10);
      days = role.rates.getOrDefault(3, 1);
      addTextField(7, x + 35, y + 1, 24, 16, "" + days)
              .setMinMaxDefault(1, Integer.MAX_VALUE, days)
              .setIsEnabled(!role.infiniteDays)
              .setHoverTexts(Component.translatable("follower.hover.days", "4"));
      addTextField(6, x + 74, y + 1, 60, 16, "" + role.rentalMoney)
              .setMinMaxDefault(0L, 9999999999L, role.rentalMoney)
              .setHoverTexts("follower.hover.money");
      addLabel(lId++, x + 136, y + 4, role.rentalIsDonat ? CustomNpcs.displayDonation : CustomNpcs.displayCurrencies);
      // inventory
      addLabel(lId++, x, y += 20, Component.translatable("inv.inventory").append(":"));
      addLabel(lId, x, (y += 13) + 4, "gui.things")
              .setSize(33, 10);
      addTextField(8, x + 35, y, 24, 16, "" + role.inventory.getContainerSize())
              .setMinMaxDefault(0L, 9L, role.inventory.getContainerSize())
              .setIsEnabled(!role.disableGui)
              .setHoverTexts("follower.hover.inventory");
      addButton(10, x, y + 19, "remote.reset")
              .setSize(100, 20)
              .setHoverTexts("follower.hover.reset");
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 7: role.infiniteDays = ((GuiCheckBoxNop) button).selected(); init(); break;
         case 8: role.disableGui = ((GuiCheckBoxNop) button).selected(); init(); break;
         case 9: role.refuseSoulStone = !((GuiCheckBoxNop) button).selected(); break;
         case 10: role.killed(); break;
         case 11: role.rentalIsDonat = ((GuiCheckBoxNop) button).selected(); init(); break;
      }
   }

   @Override
   protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
      super.renderBg(graphics, partialTicks, mouseX, mouseY);
      // icon
      GuiTextFieldNop textField = getTextField(6);
      if (textField != null) {
         PoseStack matrixStack = graphics.pose();
         matrixStack.pushPose();
         matrixStack.translate(textField.getX() + textField.getWidth() + 14, textField.getY(), 0.0f);
         float sc = 16.0f / 250.f;
         matrixStack.scale(sc, sc, sc);
         graphics.blit(role.rentalIsDonat ? GuiBasic.DONAT : GuiBasic.MONEY, 0, 0, 0, 0, 256, 256);
         matrixStack.popPose();
      }
   }

   @Override
   public void save() {
      for (int i = 0; i < 3; i++) {
         ItemStack itemstack = i < role.rentalItems.getContainerSize() ? role.rentalItems.getItem(i) : ItemStack.EMPTY;
         if (!NoppesUtilServer.isItemStackNull(itemstack) && !role.rates.containsKey(i) && getTextField(i) != null) {
            role.rates.put(i, getTextField(i).getInteger());
         }
      }
      if (role.rentalMoney > 0 && !role.rates.containsKey(3) && getTextField(7) != null) {
         role.rates.put(3, getTextField(7).getInteger());
      }
      Packets.sendServer(new SPacketNpcRoleSave(role.save(new CompoundTag())));
   }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      switch (textField.id) {
         case 0: role.rates.put(0, textField.getInteger()); init(); break;
         case 1: role.rates.put(1, textField.getInteger()); init(); break;
         case 2: role.rates.put(2, textField.getInteger()); init(); break;
         case 3: role.dialogHire = textField.getValue(); break;
         case 4: role.dialogFarewell = textField.getValue(); break;
         case 5: role.dialogFired = textField.getValue(); break;
         case 6: role.rentalMoney = textField.getInteger(); break;
         case 7: role.rates.put(3, textField.getInteger()); init(); break;
         case 8: {
            int size = role.disableGui ? 0 : textField.getInteger();
            if (role.inventory.getContainerSize() != size) { role.inventory = new NpcMiscInventory(size); }
            textField.setValue(size);
         }
      }
   }

}
