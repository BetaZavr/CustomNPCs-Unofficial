package noppes.npcs.client.gui.availability;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketContainerOpen;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiSliderNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.ValueUtil;

// Change from Unofficial (BetaZavr)
public class SubGuiNpcAvailability
        extends GuiNPCInterface
        implements ITextfieldListener, ISliderListener {

   protected final Screen parent;
   protected final Availability availability;

   public SubGuiNpcAvailability(Availability availabilityIn, Screen gui) {
      super();
      setBackground("menubg.png");
      imageWidth = 256;
      imageHeight = 216;

      availability = availabilityIn;
      parent = gui;
   }

   @Override
   public void init() {
      super.init();
      int x = guiLeft + 6;
      int y = guiTop + 16;
      int h = 18;
      // title
      addLabel(1, x, guiTop + 4, "availability.available")
              .setCenter(imageWidth - 12);
      // colloquium 1
      addButton(0, x, y, "availability.selectdialog")
              .setSize(120, h)
              .setHoverTexts("availability.hover.selectdialog")
              .layerColor = availability.dialogues.isEmpty() ? 0xFFC0C0C0 : 0xFF00C000;
      addButton(1, x, y += h + 2, "availability.selectquest")
              .setSize(120, h)
              .setHoverTexts("availability.hover.selectquest")
              .layerColor = availability.quests.isEmpty() ? 0xFFC0C0C0 : 0xFF00C000;
      addButton(2, x, y += h + 2, "availability.selectfaction")
              .setSize(120, h)
              .setHoverTexts("availability.hover.selectfaction")
              .layerColor = availability.factions.isEmpty() ? 0xFFC0C0C0 : 0xFF00C000;

      addButton(8, x, y += h + 2, "availability.stack")
              .setSize(120, h)
              .setHoverTexts("availability.hover.stack")
              .layerColor = availability.stacks.isEmpty() ? 0xFFC0C0C0 : 0xFF00C000;
      addButton(10, x, y + h + 2, "availability.currency")
              .setSize(120, h)
              .setHoverTexts("availability.hover.currency")
              .layerColor = availability.moneys.isEmpty() ? 0xFFC0C0C0 : 0xFF00C000;
      // colloquium 2
      x += 124;
      y = guiTop + 16;
      addButton(3, x, y, "availability.selectscoreboard")
              .setSize(120, h)
              .setHoverTexts("availability.hover.selectscoreboard")
              .layerColor = availability.scoreboards.isEmpty() ? 0xFFC0C0C0 : 0xFF00C000;
      addButton(6, x, y += h + 2, "availability.selectnames")
              .setSize(120, h)
              .setHoverTexts("availability.hover.selectnames")
              .layerColor = availability.playerNames.isEmpty() ? 0xFFC0C0C0 : 0xFF00C000;
      addButton(7, x, y += h + 2, "availability.storeddata")
              .setSize(120, h)
              .setHoverTexts("availability.hover.storeddata")
              .layerColor = availability.storeddata.isEmpty() ? 0xFFC0C0C0 : 0xFF00C000;
      addButton(9, x, y + h + 2, "availability.region")
              .setSize(120, h)
              .setHoverTexts("availability.hover.region")
              .layerColor = availability.regions.isEmpty() ? 0xFFC0C0C0 : 0xFF00C000;
      // exit
      addButton(66, guiLeft + 82, guiTop + 192, "gui.done")
              .setSize(98, h)
              .setHoverTexts("hover.back");
      // day type
      x = guiLeft + 6;
      int x0 = x + 76;
      y = guiTop + 126;
      addLabel(50, x, y + 4, "availability.daytime")
              .setSize(74, 12);
      addButton(50, x0, y, false,
              availability.daytime[0] == availability.daytime[1] ? 1 : availability.daytime[0] == 18 && availability.daytime[1] == 6 ? 2 : availability.daytime[0] == 6 && availability.daytime[1] == 18 ? 3 : 1,
              "availability.own", "availability.always", "availability.night", "availability.day")
              .setSize(70, h)
              .setHoverTexts("availability.hover.daytime.0");
      // start day time
      addTextField(52, x0 + 75, y + 1, 40, h - 2, availability.daytime[0])
              .setMinMaxDefault(0, 23, availability.daytime[0])
              .setHoverTexts("availability.hover.daytime.1");
      // next day time
      addTextField(53, x0 + 120, y + 1, 40, h - 2, availability.daytime[1])
              .setMinMaxDefault(0, 23, availability.daytime[1])
              .setHoverTexts("availability.hover.daytime.2");
      // min player level
      addLabel(51, x, (y += 22) + 4, "availability.minlevel")
              .setSize(74, 12);
      addTextField(51, x0 + 1, y + 1, 68, h - 2, availability.minPlayerLevel)
              .setMinMaxDefault(0, Integer.MAX_VALUE, 0)
              .setHoverTexts("availability.hover.level");
      // GM
      addCheckBox(5, x0 + 75, y, "availability.type.only.gm", "availability.type.only.gm.false", availability.getGMOnly())
              .setSize(93, 18);
      // health
      addLabel(52, x, (y += 22) + 4, "availability.health")
              .setSize(74, 12);
      addButton(4, x0, y, false, availability.healthType, "availability.always", "availability.bigger", "availability.smaller")
              .setSize(70, h)
              .setHoverTexts("availability.hover.health.type");
      addSlider(5, x0 + 75, y, availability.health / 100.0f)
              .setSize(93, 18)
              .setIsVisible(availability.healthType != 0)
              .setHoverTexts("availability.hover.health");
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 0: setSubGui(new SubGuiNpcAvailabilityDialog(availability)); break;
         case 1: setSubGui(new SubGuiNpcAvailabilityQuest(availability)); break;
         case 2: setSubGui(new SubGuiNpcAvailabilityFaction(availability)); break;
         case 3: setSubGui(new SubGuiNpcAvailabilityScoreboard(availability)); break;
         case 4: {
            availability.healthType = button.getValue();
            if (getSlider(5) != null) { getSlider(5).setIsVisible(availability.healthType != 0); }
            break;
         } // health type
         case 5: availability.setGMOnly(((GuiCheckBoxNop) button).selected()); break;
         case 6: setSubGui(new SubGuiNpcAvailabilityNames(availability)); break;
         case 7: setSubGui(new SubGuiNpcAvailabilityStoredData(availability)); break;
         case 8: {
            SubGuiNpcAvailabilityItemStacks.parent = parent;
            SubGuiNpcAvailabilityItemStacks.setting = this;
            Packets.sendServer(new SPacketContainerOpen(EnumGuiType.AvailabilityStack, (b) -> b.writeNbt(availability.save(new CompoundTag()))));
            break;
         } // ItemStacks
         case 9: setSubGui(new SubGuiNpcAvailabilityRegions(availability)); break; // custom regions
         case 10: setSubGui(new SubGuiNpcAvailabilityMoneys(availability)); break; // moneys
         case 50: {
            availability.setDaytime(ValueUtil.correctInt(button.getValue() - 1, 0, 2));
            getTextField(52).setValue("" + availability.daytime[0]);
            getTextField(53).setValue("" + availability.daytime[1]);
            break;
         } // daytime
         case 66: onClose(); break;
      }
   }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      switch (textField.id) {
         case 51: availability.minPlayerLevel = textField.getInteger(); break;
         case 52: availability.daytime[0] = textField.getInteger(); break;
         case 53: availability.daytime[1] = textField.getInteger(); break;
      }
      availability.hasOptions();
   }

   // New from Unofficial (BetaZavr)
   @Override
   public void mouseDragged(GuiSliderNop slider) {
      availability.health = (int) (slider.sliderValue * 100.0f);
      slider.setString(availability.health + "%");
   }

   @Override
   public void mousePressed(GuiSliderNop slider) { }

   @Override
   public void mouseReleased(GuiSliderNop slider) { }

}
