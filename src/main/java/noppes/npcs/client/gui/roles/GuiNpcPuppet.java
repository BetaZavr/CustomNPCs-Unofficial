package noppes.npcs.client.gui.roles;

import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcPuppetSave;
import noppes.npcs.roles.JobPuppet;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.ValueUtil;

public class GuiNpcPuppet extends GuiNPCInterface
        implements ISliderListener, ICustomScrollListener, ITextfieldListener {

   protected final Screen parent;
   protected boolean isStart = true;
   protected GuiCustomScrollNop scroll;
   protected HashMap<Component, JobPuppet.PartConfig> data = new HashMap<>();
   protected Component selected = Component.empty();

   public GuiNpcPuppet(Screen parentIn, EntityCustomNpc npc) {
      super(npc);
      imageHeight = 230;
      imageWidth = 400;

      parent = parentIn;
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 29: data.get(selected).disabled = button.getValue() == 1; break;
         case 30: npc.puppet.whileStanding = ((GuiButtonYesNo) button).getBoolean(); break;
         case 31: npc.puppet.whileMoving = ((GuiButtonYesNo) button).getBoolean(); break;
         case 32: npc.puppet.whileAttacking = ((GuiButtonYesNo) button).getBoolean(); break;
         case 33: {
            npc.puppet.animate = ((GuiButtonYesNo) button).getBoolean();
            isStart = true;
            init();
            break;
         } // is animate
         case 34: npc.puppet.animationSpeed = button.getValue(); break;
         case 66: onClose(); break;
         case 67: isStart = true; init(); break;
         case 68: isStart = false; init(); break;
      }
   }

   @Override
   public void init() {
      super.init();
      int lId = 0;
      int x0 = guiLeft + 10;
      int w = 80;
      int x1 = x0 + w + 2;
      int y = guiTop + 14;
      addLabel(lId++, x0, y + 3, "puppet.standing")
              .setSize(w, 10)
              .setColor(CustomNpcs.MainColor.getRGB());
      addYesNo(30, x1, y, npc.puppet.whileStanding)
              .setSize(80, 16);
      addLabel(lId++, x0, (y += 18) + 3, "puppet.walking")
              .setSize(w, 10)
              .setColor(CustomNpcs.MainColor.getRGB());
      addYesNo(31, x1, y, npc.puppet.whileMoving)
              .setSize(80, 16);
      addLabel(lId++, x0, (y += 18) + 3, "puppet.attacking")
              .setSize(w, 10)
              .setColor(CustomNpcs.MainColor.getRGB());
      addYesNo(32, x1, y, npc.puppet.whileAttacking)
              .setSize(80, 16);
      addLabel(lId++, x0, (y += 18) + 3, "puppet.animation")
              .setSize(w, 10)
              .setColor(CustomNpcs.MainColor.getRGB());
      addYesNo(33, x1, y, npc.puppet.animate)
              .setSize(80, 16);
      y += 18;
      if (npc.puppet.animate) {
         Object[] numbs = new Object[8];
         for (int i = 1; i < 9; i++) { numbs[i - 1] = i; }
         addLabel(lId, x0 + 20, y + 3, Component.translatable("stats.speed").append(":"))
                 .setSize(w, 10)
                 .setColor(CustomNpcs.MainColor.getRGB());
         addButton(34, x1 + 14, y, true, npc.puppet.animationSpeed, numbs)
                 .setSize(80, 16);
      }
      y += 18;
      HashMap<Component, JobPuppet.PartConfig> dataIn = new HashMap<>();
      dataIn.put(Component.translatable("model.head"), isStart ? npc.puppet.head : npc.puppet.head2);
      dataIn.put(Component.translatable("model.body"), isStart ? npc.puppet.body : npc.puppet.body2);
      dataIn.put(Component.translatable("model.larm"), isStart ? npc.puppet.larm : npc.puppet.larm2);
      dataIn.put(Component.translatable("model.rarm"), isStart ? npc.puppet.rarm : npc.puppet.rarm2);
      dataIn.put(Component.translatable("model.lleg"), isStart ? npc.puppet.lleg : npc.puppet.lleg2);
      dataIn.put(Component.translatable("model.rleg"), isStart ? npc.puppet.rleg : npc.puppet.rleg2);
      data = dataIn;
      if (scroll == null) { scroll = addScroll(0).setSize(80, 126); }
      add(scroll.setPos(guiLeft + 10, y)
              .disabledSearch()
              .setNormalList(new ArrayList<>(dataIn.keySet())));
      if (selected != null) {
         scroll.setSelected(selected);
         if (scroll.hasSelected()) { addPartComponents(y, dataIn.get(selected)); }
      }
      addButton(66, guiLeft + imageWidth - 22, guiTop, "X")
              .setSize(20, 20);
      if (npc.puppet.animate) {
         int x = scroll.getX();
         y = scroll.getY() + scroll.getHeight() + 2;
         addButton(67, x, y, "gui.start")
                 .setSize(70, 16)
                 .setIsEnabled(!isStart);
         addButton(68, x + 72, y, "gui.end")
                 .setSize(70, 16)
                 .setIsEnabled(isStart);
      }
   }

   private void addPartComponents(int y, JobPuppet.PartConfig config) {
      if (config == null) { return; }
      int x0 = guiLeft + 94;
      int x1 = x0 + 12;
      int w = 120;
      int x2 = x1 + w + 3;
      addButton(29, x1, y, false, config.disabled ? 1 : 0, "gui.enabled", "gui.disabled")
              .setSize(80, 16);
      addLabel(10, x0, (y += 18) + 3, "X:")
              .setSize(12, 10)
              .setColor(CustomNpcs.MainColor.getRGB());
      GuiSliderNop slider = addSlider(10, x1, y, (config.rotationX + 1.0F) / 2.0F)
              .setSize(120, 16);
      float percent = slider.getValue() * 360.0F;
      addTextField(10, x2, y + 1, 35, 14, Math.round(percent * 10.0f) / 10.0f);
      addLabel(11, x0, (y += 18) + 3, "Y:")
              .setSize(12, 10)
              .setColor(CustomNpcs.MainColor.getRGB());
      slider = addSlider(11, x1, y, (config.rotationY + 1.0F) / 2.0F)
              .setSize(120, 16);
      percent = slider.getValue() * 360.0F;
      addTextField(11, x2, y + 1, 35, 14, Math.round(percent * 10.0f) / 10.0f);
      addLabel(12, x0, (y += 18) + 3, "Z:")
              .setSize(12, 10)
              .setColor(CustomNpcs.MainColor.getRGB());
      slider = addSlider(12, x1, y, (config.rotationZ + 1.0F) / 2.0F)
              .setSize(120, 16);
      percent = slider.getValue() * 360.0F;
      addTextField(12, x2, y + 1, 35, 14, Math.round(percent * 10.0f) / 10.0f);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      drawNpc(graphics, npc, 320, 200, 3.0F, 0, 0, 0);
   }

   // New from Unofficial (BetaZavr)
   @Override
   public void onClose() {
      super.onClose();
      Packets.sendServer(new SPacketNpcPuppetSave(npc.puppet.save(new CompoundTag())));
      if (minecraft != null) { minecraft.setScreen(parent); }
      else { NoppesUtil.requestOpenGUI(EnumGuiType.MainMenuAdvanced); }
   }

   @Override
   public void mouseDragged(GuiSliderNop slider) {
      float percent = slider.getValue() * 360.0F;
      slider.setString((int) percent + "°");
      if (getTextField(slider.id) != null) { getTextField(slider.id).setValue(Math.round(percent * 10.0f) / 10.0f); }
      JobPuppet.PartConfig part = data.get(selected);
      switch (slider.id) {
         case 10: part.rotationX = (slider.getValue() - 0.5F) * 2.0F; break;
         case 11: part.rotationY = (slider.getValue() - 0.5F) * 2.0F; break;
         case 12: part.rotationZ = (slider.getValue() - 0.5F) * 2.0F; break;
      }
      npc.refreshDimensions();
   }

   @Override
   public void mousePressed(GuiSliderNop slider) { }

   @Override
   public void mouseReleased(GuiSliderNop slider) { }

   @Override
   public void scrollClicked(GuiCustomScrollNop guiCustomScroll) {
      selected = guiCustomScroll.getNormalSelected();
      init();
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      JobPuppet.PartConfig part = data.get(selected);
      float value = ValueUtil.correctFloat(0.005556f * textField.getFloat() - 1.0f, -1.0f, 1.0f);
      switch (textField.id) {
         case 10: part.rotationX = value; break;
         case 11: part.rotationY = value; break;
         case 12: part.rotationZ = value; break;
      }
      if (getSlider(textField.id) != null) { getSlider(textField.id).setValue((value + 1.0F) / 2.0F); }
      npc.refreshDimensions();
   }

}
