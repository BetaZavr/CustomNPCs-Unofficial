package noppes.npcs.client.gui.roles;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.player.companion.GuiNpcCompanionTalents;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.constants.EnumCompanionStage;
import noppes.npcs.constants.EnumCompanionTalent;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcRoleCompanionUpdate;
import noppes.npcs.packets.server.SPacketNpcRoleSave;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class SubGuiNpcCompanion extends GuiNPCInterface2
        implements ITextfieldListener, ISliderListener {

   protected final List<GuiNpcCompanionTalents.GuiTalent> talents = new ArrayList<>();
   protected final RoleCompanion role;

   public SubGuiNpcCompanion(EntityNPCInterface npc) {
      super(npc);

      backGui = EnumGuiType.MainMenuAdvanced;
      role = (RoleCompanion) npc.role;
   }

   @Override
   public void init() {
      super.init();
      talents.clear();
      int x0 = guiLeft + 4;
      int x1 = x0 + 66;
      int x2 = x1 + 92;
      int x3 = x0 + 29;
      int y = guiTop + 5;
      // stage
      addButton(0, x1, y, false, role.stage.ordinal(),
              EnumCompanionStage.BABY.name, EnumCompanionStage.CHILD.name, EnumCompanionStage.TEEN.name,
              EnumCompanionStage.ADULT.name, EnumCompanionStage.FULLGROWN.name)
              .setSize(90, 20);
      addLabel(0, x0, y + 5, Component.translatable("companion.stage").append(":"))
              .setSize(88, 20);
      addButton(1, x2, y, "gui.update")
              .setSize(90, 20);
      // age
      addYesNo(2, x1, y += 22, role.canAge)
              .setSize(90, 20);
      addLabel(2, x0, y + 5, Component.translatable("companion.age").append(":"))
              .setSize(88, 20);
      if (role.canAge) {
         addTextField(2, x2 + 1, y + 1, 90, 18, role.ticksActive)
                 .setMinMaxDefault(0, Integer.MAX_VALUE, 0);
      }
      // talents
      talents.add(new GuiNpcCompanionTalents.GuiTalent(role, EnumCompanionTalent.INVENTORY, x0 += 2, y += 26));
      addSlider(10, x3, y + 2, (float)role.getExp(EnumCompanionTalent.INVENTORY) / 5000.0F)
              .setSize(100, 20);
      talents.add(new GuiNpcCompanionTalents.GuiTalent(role, EnumCompanionTalent.ARMOR, x0, y += 26));
      addSlider(11, x3, y + 2, (float)role.getExp(EnumCompanionTalent.ARMOR) / 5000.0F)
              .setSize(100, 20);
      talents.add(new GuiNpcCompanionTalents.GuiTalent(role, EnumCompanionTalent.SWORD, x0, y += 26));
      addSlider(12, x3, y + 2, (float)role.getExp(EnumCompanionTalent.SWORD) / 5000.0F)
              .setSize(100, 20);
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      for (GuiNpcCompanionTalents.GuiTalent gui : talents) { gui.init(minecraft, width, height); }
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 0: {
            role.matureTo(EnumCompanionStage.values()[button.getValue()]);
            if (role.canAge) { role.ticksActive = role.stage.matureAge; }
            init();
            break;
         }
         case 1: Packets.sendServer(new SPacketNpcRoleCompanionUpdate(role.stage)); break;
         case 2: role.canAge = ((GuiButtonYesNo) button).getBoolean(); init(); break;
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      for (GuiNpcCompanionTalents.GuiTalent talent : new ArrayList<>(talents)) { talent.render(graphics, mouseX, mouseY, partialTicks); }
   }

   @Override
   public void save() { Packets.sendServer(new SPacketNpcRoleSave(role.save(new CompoundTag()))); }

   @Override
   public void mouseDragged(GuiSliderNop slider) {
      if (slider.getValue() <= 0.0F) {
         slider.setMessage(Component.translatable("gui.disabled"));
         role.talents.remove(EnumCompanionTalent.values()[slider.id - 10]);
      }
      else {
         slider.setMessage(Component.translatable((int)(slider.getValue() * 5000.0F) + " exp"));
         role.setExp(EnumCompanionTalent.values()[slider.id - 10], (int)(slider.getValue() * 50.0F) * 100);
      }
   }

   @Override
   public void mousePressed(GuiSliderNop slider) { }

   @Override
   public void mouseReleased(GuiSliderNop slider) { }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      if (textField.id == 2) { role.ticksActive = textField.getInteger(); }
   }

}
