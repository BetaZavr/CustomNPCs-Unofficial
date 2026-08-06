package noppes.npcs.client.gui;

import noppes.npcs.ai.EntityAIAnimation;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class SubGuiNpcMovement extends GuiBasic implements ITextfieldListener {

   protected final DataAI ai;

   public SubGuiNpcMovement(DataAI ais) {
      super();
      ai = ais;
      setBackground("menubg.png");

      imageWidth = 256;
      imageHeight = 216;
   }

   @Override
   public void init() {
      super.init();
      int lw = 98;
      int x0 = guiLeft + 6;
      int x1 = x0 + lw + 2;
      int y = guiTop + 6;
      addLabel(0, x0, y + 3, "movement.type")
              .setSize(lw, 10);
      addButton(0, x1, y, false, ai.getMovingType(), "ai.standing", "ai.wandering", "ai.movingpath")
              .setSize(100, 16)
              .setHoverTexts("ai.hover.walking.type");
      y += 18;
      addButton(15, x1, y, false, ai.movementType, "movement.ground", "movement.flying", "movement.swimming")
              .setSize(100, 16)
              .setHoverTexts("ai.hover.walking");
      addLabel(15, x0, y + 3, "movement.navigation")
              .setSize(lw, 10);
      if (ai.getMovingType() == 1) {
         addTextField(4, x1, y += 18, 60, 14, ai.walkingRange)
                 .setMinMaxDefault(0, 1000, 10)
                 .setHoverTexts("ai.hover.walking.range");
         addLabel(4, x0, y + 3, "gui.range");
         addTextField(10, x1, y += 18, 60, 14, ai.activeRange)
                 .setMinMaxDefault(32, 1000, 10);
         addLabel(10, x0, y + 3, "gui.active range");
         addYesNo(5, x1, y += 18, ai.npcInteracting)
                 .setSize(50, 16)
                 .setHoverTexts("ai.hover.interact");
         addLabel(5, x0, y + 3, "movement.wanderinteract");
         addYesNo(9, x1, y += 18, ai.movingPause)
                 .setSize(80, 16)
                 .setHoverTexts("ai.hover.moving.pause");
         addLabel(9, x0, y + 3, "movement.pauses");
      }
      else if (ai.getMovingType() == 0) {
         addLabel(17, x0, (y += 18) + 3, "spawner.posoffset")
                 .setSize(lw, 10);
         addLabel(7, x1, y + 3, "X:")
                 .setSize(12, 10);
         addTextField(7, x1 + 9, y + 1, 24, 14, ai.bodyOffsetX)
                 .setMinMaxDefault(0.0d, 10.0d, 5.0d)
                 .setHoverTexts("ai.hover.offset.x");
         addLabel(8, x1 + 36, y + 3, "Y:")
                 .setSize(12, 10);
         addTextField(8, x1 + 45, y + 1, 24, 14, ai.bodyOffsetY)
                 .setMinMaxDefault(0.0d, 20.0d, 5.0d)
                 .setHoverTexts("ai.hover.offset.y");
         addLabel(9, x1 + 72, y + 3, "Z:")
                 .setSize(12, 10);
         addTextField(9, x1 + 81, y + 1, 24, 14, ai.bodyOffsetZ)
                 .setMinMaxDefault(0.0d, 10.0d, 5.0d)
                 .setHoverTexts("ai.hover.offset.z");
         addButton(3, x1, y += 18, false, ai.animationType,
                 "stats.normal", "movement.sitting", "movement.lying", "movement.hug", "movement.sneaking",
                 "movement.dancing", "movement.aiming", "movement.crawling")
                 .setSize(100, 16)
                 .setHoverTexts("ai.hover.walking.anim");
         addLabel(3, x0, y + 3, "movement.animation")
                 .setSize(lw, 10);
         if (ai.animationType != 2) {
            addButton(4, x1, y += 18, false, ai.getStandingType(), "movement.body", "movement.manual", "movement.stalking", "movement.head")
                    .setSize(80, 16)
                    .setHoverTexts("ai.hover.rotation");
            addLabel(1, x0, y + 3, "movement.rotation")
                    .setSize(lw, 10);
         }
         else {
            addTextField(5, x1 + 19, (y += 18) + 1, 40, 14, ai.orientation)
                    .setMinMaxDefault(0, 359, 0)
                    .setHoverTexts("ai.hover.interact");
            addLabel(6, x0, y + 3, "movement.rotation")
                    .setSize(lw, 10);
            addLabel(5, guiLeft + 142, y + 3, "(0-359)");
         }
         if (ai.animationType != 2 && (ai.getStandingType() == 1 || ai.getStandingType() == 3)) {
            addTextField(5, x1 + 83, y + 1, 40, 14, ai.orientation)
                    .setMinMaxDefault(0, 359, 0)
                    .setHoverTexts("ai.hover.interact");
            addLabel(5, x1 + 126, y + 3, "(0-359)");
         }
      }
      if (ai.getMovingType() != 0) {
         addButton(12, x1, y += 18, false, EntityAIAnimation.getWalkingAnimationGuiIndex(ai.animationType),
                 "stats.normal", "movement.sneaking", "movement.aiming", "movement.dancing", "movement.crawling", "movement.hug")
                 .setSize(100, 16)
                 .setHoverTexts("ai.hover.walking.anim");
         addLabel(12, x0, y + 3, "movement.animation")
                 .setSize(lw, 10);
      }
      if (ai.getMovingType() == 2) {
         addButton(8, x1, y += 18, false, ai.movingPattern, "ai.looping", "ai.backtracking")
                 .setSize(80, 16)
                 .setHoverTexts("ai.hover.path.closed");
         addLabel(8, x0, y + 3, "movement.name")
                 .setSize(lw, 10);
         addYesNo(9, x1, y += 18, ai.movingPause)
                 .setSize(80, 16);
         addLabel(9, x0, y + 3, "movement.pauses")
                 .setSize(lw, 10);
      }
      addYesNo(13, x1, y += 18, ai.stopAndInteract)
              .setSize(60, 16)
              .setHoverTexts("ai.hover.stop.interact");
      addLabel(13, x0, y + 3, "movement.stopinteract")
              .setSize(lw, 10);
      addTextField(14, x1 + 1, (y += 18) + 1, 50, 14, ai.getWalkingSpeed())
              .setMinMaxDefault(0, 100, ai.getWalkingSpeed())
              .setHoverTexts("ai.hover.walking.speed");
      addLabel(14, x0, y + 3, "stats.movespeed")
              .setSize(lw, 10);
      addTextField(15, x1 + 1, (y += 18) + 1, 50, 14, ai.stepheight)
              .setMinMaxDefault(0.1d, 3.0d, ai.stepheight)
              .setHoverTexts("ai.hover.step.height");
      addLabel(16, x0, y + 3, "stats.stepheight")
              .setSize(lw, 10);
      addButton(66, guiLeft + 190, guiTop + 190, "gui.done")
              .setSize(60, 16)
              .setHoverTexts("hover.back");
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 0: {
            ai.setMovingType(button.getValue());
            if (ai.getMovingType() != 0) {
               ai.animationType = 0;
               ai.setStandingType(0);
               ai.bodyOffsetX = ai.bodyOffsetY = ai.bodyOffsetZ = 5.0F;
            }
            init();
            break;
         }
         case 3: {
            ai.animationType = button.getValue();
            init();
            break;
         }
         case 4: {
            ai.setStandingType(button.getValue());
            init();
            break;
         }
         case 5: ai.npcInteracting = button.getValue() == 1; break;
         case 8: ai.movingPattern = button.getValue(); break;
         case 9: ai.movingPause = button.getValue() == 1; break;
         case 12: {
            ai.animationType = switch (button.getValue()) {
               case 1 -> 4;
               case 2 -> 6;
               case 3 -> 5;
               case 4 -> 7;
               case 5 -> 3;
               default -> 0;
            };
            break;
         }
         case 13: ai.stopAndInteract = button.getValue() == 1; break;
         case 15: ai.movementType = button.getValue(); break;
         case 66: onClose(); break;
      }
   }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      switch (textField.id) {
         case 4: ai.walkingRange = textField.getInteger(); break;
         case 5: ai.orientation = textField.getInteger(); break;
         case 7: ai.bodyOffsetX = textField.getFloat(); break;
         case 8: ai.bodyOffsetY = textField.getFloat(); break;
         case 9: ai.bodyOffsetZ = textField.getFloat(); break;
         case 10: ai.activeRange = textField.getInteger(); break;
         case 14: ai.setWalkingSpeed(textField.getInteger()); break;
         case 15: ai.stepheight = textField.getFloat(); break;
      }
   }

}
