package noppes.npcs.client.gui.questtypes;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.global.GuiNpcManageQuest;
import noppes.npcs.client.gui.global.SubGuiQuestObjectiveSelect;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.containers.ContainerNpcQuestTypeItem;
import noppes.npcs.controllers.BorderController;
import noppes.npcs.controllers.DimensionController;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketTeleportTo;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SubGuiNpcQuestTypeItem
        extends GuiContainerNPCInterface<ContainerNpcQuestTypeItem>
        implements ITextfieldListener {

   public static Screen parent;
   protected final Map<Integer, ResourceLocation> dataDimIDs = new HashMap<>();
   protected final QuestObjective task;
   protected final Slot slot;
   protected GuiCustomScrollNop scroll;

   public SubGuiNpcQuestTypeItem(ContainerNpcQuestTypeItem container, Inventory inv, Component titleIn) {
      super(NoppesUtilServer.getEditingNpc(Minecraft.getInstance().player), container, inv, titleIn);
      setBackground("inventorymenu.png");
      imageWidth = 176;
      imageHeight = 217;
      closeOnEsc = true;

      task = container.task;
      slot = container.slot;
   }

   @Override
   public void init() {
      super.init();
      int lId = 0;
      int x0 = guiLeft + 6;
      int x1 = guiLeft + imageWidth - 56;
      int y = guiTop + 3;
      // take items
      addLabel(lId++, x0, y + 2, "quest.takeitems")
              .setSize(113, 10);
      addYesNo(0, x1, y, task != null && task.isItemLeave())
              .setSize(50, 14)
              .setHoverTexts("quest.hover.edit.item.leave");
      // ignore damage
      addLabel(lId++, x0, (y += 15) + 2, "gui.ignoreDamage")
              .setSize(113, 10);
      addYesNo(1, x1, y, task == null || task.isIgnoreDamage())
              .setSize(50, 14)
              .setHoverTexts("quest.hover.edit.item.ign.dam");
      // ignore nbt
      addLabel(lId++, x0, (y += 15) + 2, "gui.ignoreNBT")
              .setSize(113, 10);
      addYesNo(2, x1, y, task == null || task.isItemIgnoreNBT())
              .setSize(50, 14)
              .setHoverTexts("quest.hover.edit.item.ign.nbt");
      // item amount
      addLabel(lId++, x0, (y += 15) + 2, "quest.itemamount")
              .setSize(113, 10);
      addTextField(1, x1 + 1, y + 1, 48, 12, task != null ? task.getMaxProgress() : 0)
              .setMinMaxDefault(0, 576, 1).setHoverTexts("quest.hover.edit.item.max", "576");
      // X
      Component compass = Component.translatable("quest.hover.compass");
      addLabel(lId++, x0, (y += 16) + 2, "X:")
              .setSize(12, 10);
      addTextField(10, x0 + 10, y, 38, 12, task != null ? task.pos.getX() : 0)
              .setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task != null ? task.pos.getX() : 0)
              .setHoverTexts(Component.translatable("quest.hover.compass.pos", "X").append(compass));
      // Y
      addLabel(lId++, x0 + 50, y + 2, "Y:")
              .setSize(12, 10);
      addTextField(11, x0 + 60, y, 38, 12, task != null ? task.pos.getY() : 0)
              .setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task != null ? task.pos.getY() : 0)
              .setHoverTexts(Component.translatable("quest.hover.compass.pos", "Y").append(compass));
      // Z
      addLabel(lId++, x0 + 100, y + 2, "Z:")
              .setSize(12, 10);
      addTextField(12, x0 + 110, y, 38, 12, task != null ? task.pos.getZ() : 0)
              .setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task != null ? task.pos.getZ() : 0)
              .setHoverTexts(Component.translatable("quest.hover.compass.pos", "Z").append(compass));
      // tp
      addButton(11, x0 + 150, y - 1, "tp")
              .setSize(14, 14)
              .setHoverTexts("hover.teleport");
      // R
      addLabel(lId++, x0, (y += 14) + 2, "R:")
              .setSize(12, 10);
      addTextField(14, x0 + 10, y, 25, 12, task != null ? task.rangeCompass : 0)
              .setMinMaxDefault(0, 64, task != null ? task.rangeCompass : 0)
              .setHoverTexts(Component.translatable("quest.hover.compass.range").append(compass));
      // N
      addLabel(lId++, x0 + 37, y + 2, "N:")
              .setSize(12, 10);
      addTextField(15, x0 + 47, y, 101, 12, task != null ? task.compassEntityName : "")
              .setHoverTexts(Component.translatable("quest.hover.compass.entity").append(compass));
      addButton(9, x0 + 150, y - 1, "")
              .setSize(14, 14)
              .setIsAnim(true)
              .setTexture(GuiBasic.ANIMATION_BUTTONS)
              .setUV(220, 96, 36, 36)
              .setHoverTexts("color.hover")
              .layerColor = task != null ? task.colorCompass | 0xFF000000 : -1;
      // dim ID
      addLabel(lId++, x0 + 21, (y += 16) + 2, "D:")
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
         if (task != null && dimIDs[i].equals(task.dimension.toString())) { p = i; }
         i++;
      }
      addButton(4, x0 + 29, y - 1, false, p, dimIDs)
              .setSize(74, 16)
              .setHoverTexts(Component.translatable("quest.hover.compass.dim", dimIDs[p]).append(compass));
      // region ID
      addLabel(lId, x0 + 105, y + 2, "P:")
              .setSize(12, 10);
      addTextField(9, x0 + 115, y, 30, 14, task != null ? task.regionID : "")
              .setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task != null ? task.regionID : 0)
              .setHoverTexts(Component.translatable("quest.hover.compass.reg", task != null ? task.regionID : 0).append(compass));
      // set player pos
      addButton(10, x0 + 147, y - 1, "S")
              .setSize(16, 16)
              .setHoverTexts(Component.translatable("quest.hover.compass.set").append(compass));
      // exit
      addButton(66, x0, y = guiTop + imageHeight - 25, "gui.back")
              .setSize(40, 20)
              .setHoverTexts("hover.back");
      // mini map point
      addCheckBox(5, x0 + 42, y + 2, "quest.set.minimap.point", null, task != null && task.isSetPointOnMiniMap())
              .setSize(imageWidth - 52, 16)
              .setHoverTexts("quest.hover.set.minimap.point");
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      if (task == null) { return; }
      switch (button.id) {
         case 0: task.setItemLeave(button.getValue() == 0); break;
         case 1: task.setItemIgnoreDamage(((GuiButtonYesNo) button).getBoolean()); break;
         case 2: task.setItemIgnoreNBT(((GuiButtonYesNo) button).getBoolean()); break;
         case 4: {
            if (!dataDimIDs.containsKey(button.getValue())) { return; }
            task.dimension = dataDimIDs.get(button.getValue());
            button.setHoverTexts(Component.translatable("quest.hover.compass.dim", "" + task.dimension).append(Component.translatable("quest.hover.compass")));
            break;
         } // dimension
         case 5: task.setPointOnMiniMap(((GuiCheckBoxNop) button).selected()); break;
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

   @Override
   protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int x, int y) {
      graphics.drawCenteredString(font,
              Component.translatable("quest.title." + (task.getEnumType() == EnumQuestTask.ITEM ? "item" : "craft")),
              guiLeft + imageWidth / 2, guiTop - 12, 0xFFFFFFFF);
      if (background != null) {
         PoseStack matrixStack = graphics.pose();
         matrixStack.pushPose();
         matrixStack.translate((float) guiLeft, (float) guiTop, 0.0F);
         matrixStack.scale(bgScale, bgScale, bgScale);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         graphics.blit(background, 0, 0, 0, 0, imageWidth - 4, imageHeight);
         graphics.blit(background, imageWidth - 4, 0, 252, 0, 4, imageHeight);
         graphics.blit(background, slot.x - 1, slot.y - 1, 7, 112, 18, 18);
         matrixStack.popPose();
      }
   }

   public void unFocused(GuiTextFieldNop textField) {
      if (task == null) { return; }
      switch (textField.id) {
         case 1: task.setMaxProgress(textField.getInteger()); break;
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
      task.setItem(slot.getItem());
      if (task.getItem().isEmpty()) {
         NoppesUtilServer.getEditingQuest(player).questInterface.removeTask(task);
         NoppesUtil.openGUI(player, GuiNpcManageQuest.Instance);
         return;
      }
      if (GuiNpcManageQuest.Instance.getSubGui() instanceof SubGuiQuestObjectiveSelect gui) { gui.onClose(); }
      setScreen(GuiNpcManageQuest.Instance);
   }

   @Override
   public void save() {
      task.setMaxProgress(getTextField(1).getInteger());
      for (QuestObjective taskObj : NoppesUtilServer.getEditingQuest(player).questInterface.tasks) {
         if (taskObj == task || taskObj.getEnumType() != task.getEnumType()) { continue; }
         if (NoppesUtilPlayer.compareItems(taskObj.getItemStack(), task.getItemStack(), task.isIgnoreDamage(), task.isItemIgnoreNBT())) {
            task.setItem(ItemStack.EMPTY);
            break;
         }
      }
   }

}
