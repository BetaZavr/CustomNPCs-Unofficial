package noppes.npcs.api.gui;

import noppes.npcs.CustomNpcs;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.gui.subgui.AssetsGui;
import noppes.npcs.api.gui.subgui.AvailabilityGui;
import noppes.npcs.api.wrapper.gui.CustomGuiWrapper;
import noppes.npcs.api.wrapper.gui.GuiComponentsScrollableWrapper;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.data.DataDisplay;

import java.awt.*;

public class DisplayMenu extends MainMenuGui {

   public DisplayMenu(EntityCustomNpc npc, IPlayer<?> player) {
      super(0, npc, player);
      DataDisplay display = npc.display;
      GuiComponentsScrollableWrapper panel = this.gui.getScrollingPanel().init(6, 26, this.gui.getWidth() - 12, this.gui.getHeight() - 32);
      int y = 0;
      panel.addLabel(0, "gui.name", 0, y, 100, 8);
      ITextField nameTf = panel.addTextField(1, 0, y + 9, 320, 20).setText(display.getName()).setOnChange((gui, textfield) -> display.setName(textfield.getText()));
      panel.addTexturedButton(2, "", 322, y + 10, 19, 19, CustomNpcs.MODID + ":textures/gui/components.png", 22, 153).setTextureHoverOffset(19).setOnPress((gui, button) -> {
         display.setName(display.getRandomName());
         nameTf.setText(display.getName());
         gui.update(nameTf);
      });
      panel.addTexturedButton(3, "", 343, y + 9, 21, 21, CustomNpcs.MODID + ":textures/gui/components.png", 22, 109).setTextureHoverOffset(21).setOnPress((gui, button) -> this.openNameSubGui((CustomGuiWrapper)gui, display, player));
      y += 36;
      panel.addLabel(4, "gui.title", 0, y, 100, 8);
      panel.addTextField(5, 0, y + 9, 320, 20).setText(display.getTitle()).setOnChange((gui, textfield) -> display.setTitle(textfield.getText()));
      y += 36;
      panel.addLabel(6, "display.skin", 0, y, 100, 8);
      ITextField textureTf = panel.addTextField(7, 0, y + 9, 300, 20).setText(display.skinType == 0 ? display.getSkinTexture() : (display.skinType == 1 ? display.getSkinPlayer() : display.getSkinUrl())).setOnChange((gui, textfield) -> this.updateTexture(textfield, display));
      panel.addTexturedButton(8, "", 302, y + 9, 21, 21, CustomNpcs.MODID + ":textures/gui/components.png", 22, 109).setTextureHoverOffset(21).setOnPress((gui, button) -> {
         if (display.skinType == 0) {
            gui.openSubGui(AssetsGui.openTexture(display.getSkinTexture(), player, npc.wrappedNPC, (text) -> {
               display.setSkinTexture(text);
               textureTf.setText(text);
               gui.update(textureTf);
            }));
         }

      });
      panel.addButtonList(9, 324, y + 9, 67, 20).setValues("display.texture", "display.player", "display.url").setSelected(display.skinType).setOnPress((gui2, bb) -> {
         display.setSkinUrl("");
         display.setSkinPlayer(null);
         display.skinType = (byte)((IButtonList)bb).getSelected();
         textureTf.setText(display.skinType == 0 ? display.getSkinTexture() : (display.skinType == 1 ? display.getSkinPlayer() : display.getSkinUrl()));
         gui2.update(textureTf);
      });
      y += 36;
      panel.addLabel(10, "display.cape", 0, y, 100, 8);
      ITextField tfCloak = panel.addTextField(11, 0, y + 9, 320, 20).setText(display.getCapeTexture()).setOnChange((gui, textfield) -> display.setCapeTexture(textfield.getText()));
      panel.addTexturedButton(12, "", 322, y + 9, 21, 21, CustomNpcs.MODID + ":textures/gui/components.png", 22, 109).setTextureHoverOffset(21).setOnPress((gui, button) -> gui.openSubGui(AssetsGui.openCloakTexture(display.getCapeTexture(), player, npc.wrappedNPC, (text) -> {
         display.setCapeTexture(text);
         tfCloak.setText(text);
         gui.update(tfCloak);
      })));
      y += 36;
      panel.addLabel(13, "display.overlay", 0, y, 100, 8);
      ITextField tfOverlay = panel.addTextField(14, 0, y + 9, 320, 20).setText(display.getOverlayTexture()).setOnChange((gui, textfield) -> display.setOverlayTexture(textfield.getText()));
      panel.addTexturedButton(15, "", 322, y + 9, 21, 21, CustomNpcs.MODID + ":textures/gui/components.png", 22, 109).setTextureHoverOffset(21).setOnPress((gui, button) -> gui.openSubGui(AssetsGui.openTexture(display.getOverlayTexture(), player, npc.wrappedNPC, (text) -> {
         display.setOverlayTexture(text);
         tfOverlay.setText(text);
         gui.update(tfOverlay);
      })));
      y += 36;
      panel.addLabel(23, "display.size", 0, y + 5, 100, 8);
      panel.addTextField(24, 100, y, 60, 20)
              .setCharacterType(1)
              .setFloat(display.getSize())
              .setColor(display.getTint())
              .setMinMax(0, 50)
              .setOnFocusLost((gui, textfield) -> display.setSize(textfield.getInteger()));
      y += 26;
      panel.addLabel(16, "display.livingAnimation", 0, y + 5, 100, 8);
      panel.addButtonList(17, 172, y, 67, 20).setValues("gui.no", "gui.yes").setSelected(display.getHasLivingAnimation() ? 1 : 0).setOnPress((gui2, bb) -> display.setHasLivingAnimation(((IButtonList)bb).getSelected() == 1));
      y += 26;
      panel.addLabel(18, "display.tint", 0, y + 5, 100, 8);
      panel.addTextField(19, 100, y, 60, 20).setCharacterType(2).setInteger(display.getTint()).setColor(display.getTint()).setMinMax(0, new Color(0xFFFFFF).getRGB()).setOnFocusLost((gui, textfield) -> {
         display.setTint(textfield.getInteger());
         textfield.setColor(display.getTint());
         gui.update(textfield);
      });
      y += 26;
      panel.addLabel(20, "display.visible", 0, y + 5, 100, 8);
      panel.addButtonList(21, 100, y, 70, 20).setValues("gui.yes", "gui.no", "gui.partly").setSelected(display.getVisible()).setOnPress((gui2, bb) -> display.setVisible(((IButtonList)bb).getSelected()));
      panel.addButton(22, "availability.name", 172, y, 90, 20).setOnPress((gui2, bb) -> gui2.openSubGui(AvailabilityGui.open(display.getAvailability(), player)));
      y += 26;
      panel.addLabel(25, "display.bossbar", 0, y + 5, 100, 8);
      panel.addButtonList(26, 100, y, 100, 20).setValues("color.pink", "color.blue", "color.red", "color.green", "color.yellow", "color.purple", "color.white").setSelected(display.getBossColor()).setOnPress((gui2, bb) -> display.setBossColor(((IButtonList)bb).getSelected()));
      panel.addButtonList(27, 202, y, 120, 20).setValues("display.hide", "display.show", "display.showAttacking").setSelected(display.getBossbar()).setOnPress((gui2, bb) -> display.setBossbar(((IButtonList)bb).getSelected()));
   }

   private void updateTexture(ITextField textfield, DataDisplay display) {
      if (!textfield.getText().isBlank()) {
         if (display.skinType == 2) {
            display.setSkinUrl(textfield.getText());
         } else if (display.skinType == 1) {
            display.setSkinPlayer(textfield.getText());
         } else {
            display.setSkinTexture(textfield.getText());
         }
      }
   }

   private void openNameSubGui(CustomGuiWrapper gui, DataDisplay display, IPlayer<?> player) {
      ITextField nameTf = (ITextField)gui.getScrollingPanel().getComponent(1);
      CustomGuiWrapper subgui = new CustomGuiWrapper(player);
      subgui.setBackgroundTexture(CustomNpcs.MODID + ":textures/gui/components.png");
      subgui.setSize(200, 110);
      subgui.getBackgroundRect().setSize(200, 110);
      subgui.getBackgroundRect().setTextureOffset(0, 0);
      subgui.getBackgroundRect().setRepeatingTexture(64, 64, 4);
      IButton b = subgui.addTexturedButton(666, "X", 186, 0, 14, 14, CustomNpcs.MODID + ":textures/gui/components.png", 0, 64);
      b.getTextureRect().setRepeatingTexture(64, 22, 3);
      b.setTextureHoverOffset(22).setHoverText("gui.close");
      b.setOnPress((guii, bb) -> guii.close());
      subgui.addLabel(0, "gui.name", 4, 6, 100, 8);
      ITextField nameTf2 = subgui.addTextField(1, 4, 16, 192, 20).setText(display.getName());
      nameTf2.setOnChange((gui2, textfield) -> {
         display.setName(textfield.getText());
         nameTf.setText(display.getName());
      });
      subgui.addButtonList(2, 4, 37, 192, 20).setValues("markov.roman.name", "markov.japanese.name", "markov.slavic.name", "markov.welsh.name", "markov.sami.name", "markov.oldNorse.name", "markov.ancientGreek.name", "markov.aztec.name", "markov.classicCNPCs.name", "markov.spanish.name").setSelected(display.getMarkovGeneratorId()).setOnPress((gui2, bb) -> display.setMarkovGeneratorId(((IButtonList)bb).getSelected()));
      subgui.addButtonList(3, 4, 58, 192, 20).setValues("markov.gender.either", "markov.gender.male", "markov.gender.female").setSelected(display.getMarkovGender()).setOnPress((gui2, bb) -> display.setMarkovGender(((IButtonList)bb).getSelected()));
      b = subgui.addButton(4, "markov.generate", 30, 82, 140, 22);
      b.getTextureRect().setTexture(CustomNpcs.MODID + ":textures/gui/components.png").setRepeatingTexture(64, 22, 3).setTextureOffset(0, 64);
      b.setOnPress((gui2, bb) -> {
         display.setName(display.getRandomName());
         nameTf.setText(display.getName());
         nameTf2.setText(display.getName());
         gui.update();
      });
      gui.openSubGui(subgui);
   }
}
