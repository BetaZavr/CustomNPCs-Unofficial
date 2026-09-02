package noppes.npcs.client.gui.questtypes;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.gui.IDimensionGetter;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.global.GuiNpcManageQuest;
import noppes.npcs.client.gui.global.SubGuiQuestObjectiveSelect;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.BorderController;
import noppes.npcs.controllers.DimensionController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketDimensionsGet;
import noppes.npcs.packets.server.SPacketTeleportTo;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import java.util.*;

// Change from Unofficial (BetaZavr)
public class SubGuiNpcQuestTypeLocation
        extends GuiNPCInterface
        implements ITextfieldListener, IDimensionGetter {

   protected Screen parent;
   protected final QuestObjective task;
   protected final Map<Integer, ResourceLocation> dataDimIDs = new HashMap<>();

   public SubGuiNpcQuestTypeLocation(EntityNPCInterface npcIn, QuestObjective taskObj, Screen gui) {
      super(npcIn);
      setBackground("menubg.png");
      title = Component.translatable("quest.title.location");
      imageWidth = 256;
      imageHeight = 216;
      closeOnEsc = true;

      parent = gui;
      task = taskObj;
      Packets.sendServer(new SPacketDimensionsGet());
   }

   @Override
   public void init() {
      super.init();
      int lId = 0;
      int x = guiLeft + 6;
      int y = guiTop + 50;
      // target
      addLabel(lId++, x, y, "quest.loct.block")
              .setSize(imageWidth - 12, 10);
      addTextField(0, x, y += 12, 244, 18, task.getTargetName())
              .setHoverTexts("quest.hover.edit.kill.name");
      // X
      addLabel(lId++, x, y += 24, "quest.task.pos.set")
              .setSize(imageWidth - 12, 10);
      addLabel(lId++, x, (y += 12) + 2, "X:")
              .setSize(12, 10);
      Component compass = Component.translatable("quest.hover.compass");
      addTextField(10, x + 10, y, 40, 14, task.pos.getX())
              .setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task.pos.getX())
              .setHoverTexts(Component.translatable("quest.hover.compass.pos", "X").append(compass));
      // Y
      addLabel(lId++, x + 63, y + 2, "Y:")
              .setSize(12, 10);
      addTextField(11, x + 73, y, 40, 14, task.pos.getY())
              .setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task.pos.getY())
              .setHoverTexts(Component.translatable("quest.hover.compass.pos", "Y").append(compass));
      // Z
      addLabel(lId++, x + 127, y + 2, "Z:")
              .setSize(12, 10);
      addTextField(12, x + 137, y, 40, 14, task.pos.getZ())
              .setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task.pos.getZ())
              .setHoverTexts(Component.translatable("quest.hover.compass.pos", "Z").append(compass));
      // R
      addLabel(lId++, x + 189, y + 2, "R:")
              .setSize(12, 10);
      addTextField(14, x + 199, y, 45, 14, task.rangeCompass)
              .setMinMaxDefault(0, 64, task.rangeCompass)
              .setHoverTexts(Component.translatable("quest.hover.compass.range").append(compass));
      // dim ID
      addLabel(lId++, x, (y += 18) + 2, "D:")
              .setSize(12, 10);
      int p = 0, i = 1;
      dataDimIDs.clear();
      List<String> dimMap = DimensionController.getLineKeys();
      Object[] dimIDs = new Object[dimMap.size() + 1];
      dimIDs[0] = "minecraft:any";
      dataDimIDs.put(0, new ResourceLocation("minecraft:any"));
      for (String line : dimMap) {
         dimIDs[i] = line;
         dataDimIDs.put(i, new ResourceLocation(line));
         if (dimIDs[i].equals(task.dimension.toString())) { p = i; }
         i++;
      }
      addButton(4, x + 9, y - 1, false, p, dimIDs)
              .setSize(180, 16)
              .setHoverTexts(Component.translatable("quest.hover.compass.dim", dimIDs[p]).append(compass));
      // region ID
      addLabel(lId++, x, (y += 17) + 2, "P:")
              .setSize(12, 10);
      addTextField(9, x + 10, y, 32, 14, task.regionID)
              .setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task.regionID)
              .setHoverTexts(Component.translatable("quest.hover.compass.reg", task.regionID).append(compass));
      // N
      addLabel(lId, x + 44, y + 2, "N:")
              .setSize(12, 10);
      addTextField(15, x + 54, y, 175, 14, task.compassEntityName)
              .setHoverTexts(Component.translatable("quest.hover.compass.entity").append(compass));
      addButton(9, x + 231, y, "")
              .setSize(14, 14)
              .setIsAnim(true)
              .setTexture(GuiBasic.ANIMATION_BUTTONS)
              .setUV(220, 96, 36, 36)
              .setHoverTexts("color.hover")
              .layerColor = task.colorCompass | 0xFF000000;
      // mini map point
      addCheckBox(5, x, y += 17, "quest.set.minimap.point", null, task.isSetPointOnMiniMap())
              .setSize(205, 16)
              .setHoverTexts("quest.hover.set.minimap.point");
      // tp
      addButton(11, x + 207, y, "TP")
              .setSize(20, 16)
              .setHoverTexts("hover.teleport");
      // set player pos
      addButton(10, x + 229, y, "S")
              .setSize(16, 16)
              .setHoverTexts(Component.translatable("quest.hover.compass.set").append(compass));
      // exit
      addButton(66, x, guiTop + imageHeight - 25, "gui.back")
              .setSize(98, 20)
              .setHoverTexts("hover.back");
   }

   public void buttonEvent(GuiButtonNop guiButton) {
      if (task == null) { return; }
      switch (guiButton.id) {
         case 4: {
            if (!dataDimIDs.containsKey(guiButton.getValue())) { return; }
            task.dimension = dataDimIDs.get(guiButton.getValue());
            guiButton.setHoverTexts(Component.translatable("quest.hover.compass.dim", "" + task.dimension).append(Component.translatable("quest.hover.compass")));
            break;
         } // dimension
         case 5: task.setPointOnMiniMap(((GuiCheckBoxNop) guiButton).selected()); break;
         case 9: {
            setSubGui(new SubGuiColorSelector(task.colorCompass, new SubGuiColorSelector.ColorCallback() {
               @Override
               public void color(int colorIn) {
                  task.setCompassColor(colorIn);
                  init();
               }
               @Override
               public void preColor(int colorIn) {
                  task.setCompassColor(colorIn);
               }
            }));
            break;
         } // TP
         case 10: {
            task.pos = player.blockPosition();
            task.dimension = player.level().dimension().location();
            init();
            break;
         } // set player pos
         case 11: Packets.sendServer(new SPacketTeleportTo(ResourceKey.create(Registries.DIMENSION, task.dimension), task.pos)); break;
         case 66: onClose(); break;
      }
   }

   public void unFocused(GuiTextFieldNop textField) {
      if (task == null) { return; }
      switch (textField.id) {
         case 0: task.setTargetName(textField.getValue()); break;
         case 2: task.setAreaRange(textField.getInteger()); break;
         case 9: {
            if (!BorderController.getInstance().regions.containsKey(textField.getInteger())) {
               textField.setValue("" + textField.def);
               return;
            }
            task.regionID = textField.getInteger();
            textField.setHoverTexts(Component.translatable("quest.hover.compass.reg", "" + task.regionID).append(Component.translatable("quest.hover.compass")));
            break;
         }
         case 10: task.pos = new BlockPos(textField.getInteger(), task.pos.getY(), task.pos.getZ()); break;
         case 11: task.pos = new BlockPos(task.pos.getX(), textField.getInteger(), task.pos.getZ()); break;
         case 12: task.pos = new BlockPos(task.pos.getX(), task.pos.getY(), textField.getInteger()); break;
         case 14: task.rangeCompass = textField.getInteger(); break;
         case 15: task.compassEntityName = textField.getValue(); break;
      }
   }

   @Override
   public void onClose() {
      super.onClose();
      if (task.getTargetName().isEmpty()) {
         NoppesUtilServer.getEditingQuest(player).questInterface.removeTask(task);
         NoppesUtil.openGUI(player, GuiNpcManageQuest.Instance);
         return;
      }
      if (GuiNpcManageQuest.Instance.getSubGui() instanceof SubGuiQuestObjectiveSelect gui) { gui.onClose(); }
      setScreen(GuiNpcManageQuest.Instance);
   }

   @Override
   public void save() {
      task.setTargetName(getTextField(0).getValue());
      for (QuestObjective taskObj : NoppesUtilServer.getEditingQuest(player).questInterface.tasks) {
         if (taskObj == task || taskObj.getEnumType() != EnumQuestTask.LOCATION) {
            continue;
         }
         if (taskObj.getTargetName().equals(task.getTargetName())) {
            getTextField(0).setValue("");
            task.setTargetName("");
            break;
         }
      }
   }

   @Override
   public void resetDimension() { init(); }

}
