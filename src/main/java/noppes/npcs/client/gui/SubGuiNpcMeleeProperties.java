package noppes.npcs.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import noppes.npcs.api.constants.PotionEffectType;
import noppes.npcs.entity.data.DataMelee;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class SubGuiNpcMeleeProperties extends GuiBasic implements ITextfieldListener {

   protected static final Object[] potionNames;
   protected final DataMelee stats;

   static {
      List<Component> list = new ArrayList<>();
      list.add(Component.translatable("gui.none"));
      for(int i = 1; i < 33; ++i) {
         MobEffect pt = PotionEffectType.getMCType(i);
         if (pt != null) { list.add(Component.translatable(pt.getDescriptionId())); }
      }
      list.add(Component.translatable("block.minecraft.fire"));
      potionNames = list.toArray(new Component[0]);
   }

   public SubGuiNpcMeleeProperties(DataMelee statsIn) {
      super();
      setBackground("menubg.png");
      imageWidth = 256;
      imageHeight = 216;

      stats = statsIn;
   }

   @Override
   public void init() {
      super.init();
      // power
      addLabel(1, guiLeft + 5, guiTop + 15, "stats.meleestrength");
      addTextField(1, guiLeft + 105, guiTop + 10, 100, 18, stats.getStrength())
              .setMinMaxDefault(0, Integer.MAX_VALUE, 5)
              .setHoverTexts("stats.hover.attack.strength");
      // range
      addLabel(2, guiLeft + 5, guiTop + 45, "stats.meleerange");
      addTextField(2, guiLeft + 105, guiTop + 40, 100, 18, stats.getRange())
              .setMinMaxDefault(1.0d, 30.0d, 2.0d)
              .setHoverTexts("stats.hover.attack.range");
      // speed
      addLabel(3, guiLeft + 5, guiTop + 75, "stats.meleespeed");
      addTextField(3, guiLeft + 105, guiTop + 70, 100, 18, stats.getDelay())
              .setMinMaxDefault(1, 1000, 20)
              .setHoverTexts("stats.hover.attack.speed");
      // knockback
      addLabel(4, guiLeft + 5, guiTop + 105, "enchantment.minecraft.knockback");
      addTextField(4, guiLeft + 105, guiTop + 100, 100, 18, stats.getKnockback())
              .setMinMaxDefault(0, 4, 0)
              .setHoverTexts("stats.hover.attack.knockback");
      // effect
      addLabel(5, guiLeft + 5, guiTop + 135, "stats.meleeeffect");
      int effect = stats.getEffectType();
      addButton(5, guiLeft + 85, guiTop + 130, true, effect, potionNames)
              .setSize(100, 20)
              .setHoverTexts("stats.hover.attack.effects");
      if (stats.getEffectType() != 0) {
         addLabel(6, guiLeft + 5, guiTop + 165, "gui.time");
         addTextField(6, guiLeft + 85, guiTop + 160, 50, 18, stats.getEffectTime())
                 .setMinMaxDefault(1, 99999, 5)
                 .setHoverTexts("stats.hover.attack.effect");
         if (stats.getEffectType() != 1) {
            addLabel(7, guiLeft + 5, guiTop + 195, "stats.amplify");
            Object[] numbs = new Object[11];
            for (int i = 0; i < 11; i++) { numbs[i] = i; }
            addButton(7, guiLeft + 85, guiTop + 190, true, stats.getEffectStrength(), numbs)
                    .setSize(52, 20)
                    .setHoverTexts("stats.hover.effect.power");
         }
      }
      addButton(66, guiLeft + 164, guiTop + 192, "gui.done")
              .setSize(90, 20)
              .setHoverTexts("hover.back");
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 5: {
            int effect = button.getValue();
            if (effect == potionNames.length - 1) { effect = 666; }
            stats.setEffect(effect, stats.getEffectStrength(), stats.getEffectTime());
            init();
            break;
         }
         case 7: stats.setEffect(stats.getEffectType(), button.getValue(), stats.getEffectTime()); break;
         case 66: onClose(); break;
      }
   }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      switch (textField.id) {
         case 1: stats.setStrength(textField.getInteger());break;
         case 2: stats.setRange(textField.getInteger()); break;
         case 3: stats.setDelay(textField.getInteger()); break;
         case 4: stats.setKnockback(textField.getInteger()); break;
         case 6: stats.setEffect(stats.getEffectType(), stats.getEffectStrength(), textField.getInteger()); break;
      }
   }

}
