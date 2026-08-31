package noppes.npcs.client.gui.model;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.parts.ModelPartConfig;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;

public class GuiCreationScale extends GuiCreationScreenInterface implements ISliderListener, ICustomScrollListener {

   protected final List<EnumParts> data = new ArrayList<>();
   protected GuiCustomScrollNop scroll;
   protected static EnumParts selected = EnumParts.HEAD;

   public GuiCreationScale(EntityNPCInterface npc) {
      super(npc);
      active = 3;
      xOffset = 140;
   }

   @Override
   public void init() {
      super.init();
      if (scroll == null) { scroll = addScroll(0); }
      List<Component> list = new ArrayList<>();
      EnumParts[] parts = new EnumParts[]{EnumParts.HEAD, EnumParts.BODY, EnumParts.ARM_LEFT, EnumParts.ARM_RIGHT, EnumParts.LEG_LEFT, EnumParts.LEG_RIGHT};
      data.clear();
      for (EnumParts part : parts) {
         ModelPartConfig config;
         if (part == EnumParts.ARM_RIGHT) {
            config = playerdata.getPartConfig(EnumParts.ARM_LEFT);
            if (!config.notShared) { continue; }
         }
         if (part == EnumParts.LEG_RIGHT) {
            config = playerdata.getPartConfig(EnumParts.LEG_LEFT);
            if (!config.notShared) { continue; }
         }
         data.add(part);
         list.add(Component.translatable("part." + part.name));
      }
      add(scroll.setPos(guiLeft, guiTop + 46)
              .setUnsortedList(list)
              .setSize(120, imageHeight - 50)
              .disabledSearch());
      ModelPartConfig config = playerdata.getPartConfig(selected);
      int x0 = guiLeft + 122;
      int x1 = guiLeft + 172;
      int y = guiTop + 65;
      addLabel(10, x0, y + 5, "scale.width")
              .setColor(CustomNpcs.MainColor.getRGB());
      addSlider(10, x1, y, config.scaleX - 0.5F)
              .setSize(100, 20);
      y += 22;
      addLabel(11, x0, y + 5, "scale.height")
              .setColor(CustomNpcs.MainColor.getRGB());
      addSlider(11, x1, y, config.scaleY - 0.5F)
              .setSize(100, 20);
      y += 22;
      addLabel(12, x0, y + 5, "scale.depth")
              .setColor(CustomNpcs.MainColor.getRGB());
      addSlider(12, x1, y, config.scaleZ - 0.5F)
              .setSize(100, 20);
      if (selected == EnumParts.ARM_LEFT || selected == EnumParts.LEG_LEFT) {
         y += 22;
         addLabel(13, x0, y + 5, "scale.shared")
                 .setColor(CustomNpcs.MainColor.getRGB());
         addYesNo(13, x1, y, config.notShared)
                 .setSize(50, 20);
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      if (button.id == 13) {
         playerdata.getPartConfig(selected).notShared = ((GuiButtonYesNo) button).getBoolean();
         init();
      }
   }

   @Override
   public void mouseDragged(GuiSliderNop slider) {
      super.mouseDragged(slider);
      if (slider.id >= 10 && slider.id <= 12) {
         int percent = (int)(50.0F + slider.sliderValue * 100.0F);
         slider.setString(percent + "%");
         ModelPartConfig config = playerdata.getPartConfig(selected);
         if (slider.id == 10) {
            config.scaleX = slider.sliderValue + 0.5F;
         }

         if (slider.id == 11) {
            config.scaleY = slider.sliderValue + 0.5F;
         }

         if (slider.id == 12) {
            config.scaleZ = slider.sliderValue + 0.5F;
         }
         updateTranslate();
      }

   }

   private void updateTranslate() {
      for (EnumParts part : EnumParts.values()) {
         ModelPartConfig config = playerdata.getPartConfig(part);
         if (config != null) {
            if (part == EnumParts.HEAD) {
               config.setTranslate(0.0F, playerdata.getBodyY(), 0.0F);
            } else {
               ModelPartConfig leg;
               float x;
               float y;
               if (part == EnumParts.ARM_LEFT) {
                  leg = playerdata.getPartConfig(EnumParts.BODY);
                  x = (1.0F - leg.scaleX) * 0.25F + (1.0F - config.scaleX) * 0.075F;
                  y = playerdata.getBodyY() + (1.0F - config.scaleY) * -0.1F;
                  config.setTranslate(-x, y, 0.0F);
                  if (!config.notShared) {
                     ModelPartConfig arm = playerdata.getPartConfig(EnumParts.ARM_RIGHT);
                     arm.copyValues(config);
                  }
               } else if (part == EnumParts.ARM_RIGHT) {
                  leg = playerdata.getPartConfig(EnumParts.BODY);
                  x = (1.0F - leg.scaleX) * 0.25F + (1.0F - config.scaleX) * 0.075F;
                  y = playerdata.getBodyY() + (1.0F - config.scaleY) * -0.1F;
                  config.setTranslate(x, y, 0.0F);
               } else if (part == EnumParts.LEG_LEFT) {
                  config.setTranslate(config.scaleX * 0.125F - 0.113F, playerdata.getLegsY(), 0.0F);
                  if (!config.notShared) {
                     leg = playerdata.getPartConfig(EnumParts.LEG_RIGHT);
                     leg.copyValues(config);
                  }
               } else if (part == EnumParts.LEG_RIGHT) {
                  config.setTranslate((1.0F - config.scaleX) * 0.125F, playerdata.getLegsY(), 0.0F);
               } else if (part == EnumParts.BODY) {
                  config.setTranslate(0.0F, playerdata.getBodyY(), 0.0F);
               }
            }
         }
      }
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      if (scroll.hasSelected()) {
         selected = data.get(scroll.getSelectedIndex());
         init();
      }
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

}
