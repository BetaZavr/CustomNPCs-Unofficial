package noppes.npcs.client.gui.questtypes;

import java.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.gui.IDimensionGetter;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.global.GuiNpcManageQuest;
import noppes.npcs.client.gui.global.SubGuiQuestObjectiveSelect;
import noppes.npcs.client.gui.model.GuiCreationEntities;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.client.gui.select.SubGuiDialogSelection;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.BorderController;
import noppes.npcs.controllers.DimensionController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketTeleportTo;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;

// Change from Unofficial (BetaZavr)
public class SubGuiNpcQuestTypeKill
        extends GuiNPCInterface
        implements ITextfieldListener, ICustomScrollListener, IDimensionGetter {

   protected Screen parent;
   protected final QuestObjective task;
   protected final Map<Integer, ResourceLocation> dataDimIDs = new HashMap<>();
   protected final Map<Component, Entity> dataEntities = new HashMap<>();
   protected GuiCustomScrollNop scroll;
   protected final Map<Component, EntityType<? extends Entity>> types;

   public SubGuiNpcQuestTypeKill(EntityNPCInterface npcIn, QuestObjective taskObj, Screen gui) {
      super(npcIn);
      setBackground("menubg.png");
      imageWidth = 356;
      imageHeight = 216;
      closeOnEsc = true;
      parent = gui;

      task = taskObj;
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      types = GuiCreationEntities.getAllEntities(minecraft.level, true);
   }

   @Override
   public void init() {
      super.init();
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      int lId = 0;
      int x0 = guiLeft + 6;
      int y = guiTop + 6;
      int w = 210;
      // is part name
      addCheckBox(1, x0, y, "quest.kill.part.name", null, task.isPartName())
              .setSize(208, 14)
              .setHoverTexts("quest.hover.part.name");
      // check title
      addCheckBox(2, x0, y += 16, Component.translatable("quest.kill.add.title", Component.translatable("gui.title")), null, task.isAndTitle())
              .setSize(208, 14)
              .setHoverTexts("quest.hover.add.title", Component.translatable("gui.title"));
      // info
      addLabel(lId++, x0, y += 16, "quest.npc.to")
              .setSize(w, 10);
      addLabel(lId++, x0, y += 12, "quest.player.to")
              .setSize(w, 10);
      // target
      addTextField(0, x0, y += 14, 180, 14, task.getTargetName())
              .setHoverTexts("quest.hover.edit.kill.name");
      // max progress
      addTextField(1, x0 + 184, y, 27, 14, task.getMaxProgress())
              .setMinMaxDefault(1, Integer.MAX_VALUE, 1).setHoverTexts("quest.hover.edit.kill.value", "" + Integer.MAX_VALUE);
      // entities list
      ArrayList<Component> list = new ArrayList<>();
      LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
      int i = 1;
      list.add(Component.literal("Player").withStyle(ChatFormatting.AQUA));
      // player type
      hts.put(0, Collections.singletonList(Component.literal("Any player").withStyle(ChatFormatting.GRAY)));
      // nearest npc
      dataEntities.clear();
      List<EntityNPCInterface> npcs = player.level().getEntitiesOfClass(EntityNPCInterface.class, new AABB(player.blockPosition()).inflate(32.0d));
      TreeMap<Float, EntityNPCInterface> map = new TreeMap<>();
      for (EntityNPCInterface npc : npcs) {
         float distance = (float) player.distanceToSqr(npc);
         while (map.containsKey(distance)) { distance += 0.0001f; }
         map.put(distance, npc);
      }
      for (Float distance : map.keySet()) {
         Component name = map.get(distance).getName();
         Component key = name.copy().withStyle(ChatFormatting.GREEN);
         boolean added = true;
         for (Component line : list) {
            if (line.getString().equals(key.getString())) {
               added = false;
               break;
            }
         }
         if (!added) { continue; }
         list.add(key);
         dataEntities.put(key, map.get(distance));
         ArrayList<Component> hoverList = new ArrayList<>();
         hoverList.add(Component.empty()
                 .append(Component.literal("NPC name: \"").withStyle(ChatFormatting.GRAY))
                 .append(name.copy().withStyle(ChatFormatting.RESET))
                 .append(Component.literal("\"").withStyle(ChatFormatting.GRAY)));
         hoverList.add(Component.empty()
                 .append(Component.literal("Distance of: ").withStyle(ChatFormatting.GRAY))
                 .append(Component.literal(df.format(distance)).withStyle(ChatFormatting.GOLD)));
         hts.put(i++, hoverList);
      }
      // registry entity names
      if (minecraft.level != null) {
         for (Map.Entry<Component, EntityType<? extends Entity>> entry : types.entrySet()) {
            String line = entry.getValue().getDescriptionId();
            List<Component> hover = new ArrayList<>();
            if (line.startsWith("entity.customnpcs.")) {
               hover.add(Component.translatable(line.replace("entity.customnpcs.", "entity.hover.customnpcs.")));
            }
            else if (line.startsWith("entity.minecraft.")) {
               hover.add(Component.translatable("entity.hover.minecraft"));
            }
            else {
               hover.add(Component.translatable("entity.hover.in.mod"));
               hover.add(Component.literal(line.substring(7, line.indexOf(".", 7))));
            }
            list.add(entry.getKey());
            dataEntities.put(entry.getKey(), entry.getValue().create(minecraft.level));
            hts.put(i++, hover);
         }
      }
      // gui elements
      if (scroll == null) { scroll = addScroll(0); }
      add(scroll.setPos(guiLeft + 220, guiTop + 14)
              .setSize(130, 198)
              .setUnsortedList(list)
              .setHoverTexts(hts)
      );
      scroll.setSelected(task.getTargetName());
      // exit
      addButton(66, x0, guiTop + imageHeight - 21, "gui.back")
              .setSize(98, 16)
              .setHoverTexts("hover.back");
      // range to area kill
      if (task.getEnumType() == EnumQuestTask.AREAKILL) {
         addLabel(lId++, x0, (y += 18) + 3, "gui.searchdistance")
              .setSize(112, 10);
         addTextField(2, x0 + 114, y, 40, 14, task.getAreaRange())
                 .setMinMaxDefault(3, 32, task.getAreaRange()).setHoverTexts("quest.hover.area.range");
         y += 2;
      }
      // X
      addLabel(lId++, x0, y += 17, "quest.task.pos.set")
              .setSize(w, 10);
      addLabel(lId++, x0, (y += 12) + 2, "X:")
              .setSize(12, 10);
      Component compass = Component.translatable("quest.hover.compass");
      addTextField(10, x0 + 10, y, 42, 14, task.pos.getX())
              .setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task.pos.getX())
              .setHoverTexts(Component.translatable("quest.hover.compass.pos", "X").append(compass));
      // Y
      addLabel(lId++, x0 + 54, y + 2, "Y:")
              .setSize(12, 10);
      addTextField(11, x0 + 64, y, 42, 14, task.pos.getY())
              .setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task.pos.getY())
              .setHoverTexts(Component.translatable("quest.hover.compass.pos", "Y").append(compass));
      // Z
      addLabel(lId++, x0 + 108, y + 2, "Z:")
              .setSize(12, 10);
      addTextField(12, x0 + 118, y, 42, 14, task.pos.getZ())
              .setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task.pos.getZ())
              .setHoverTexts(Component.translatable("quest.hover.compass.pos", "Z").append(compass));
      // R
      addLabel(lId++, x0 + 162, y + 2, "R:")
              .setSize(12, 10);
      addTextField(14, x0 + 172, y, 39, 14, task.rangeCompass)
              .setMinMaxDefault(0, 64, task.rangeCompass)
              .setHoverTexts(Component.translatable("quest.hover.compass.range").append(compass));
      // dim ID
      addLabel(lId++, x0, (y += 17) + 2, "D:")
              .setSize(12, 10);
      int p = 0;
      i = 1;
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
      addButton(4, x0 + 10, y - 1, false, p, dimIDs)
              .setSize(w - 8, 16)
              .setHoverTexts(Component.translatable("quest.hover.compass.dim", dimIDs[p]).append(compass));
      // region ID
      addLabel(lId++, x0, (y += 17) + 2, "P:")
              .setSize(12, 10);
      addTextField(9, x0 + 10, y, 41, 14, task.regionID)
              .setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task.regionID)
              .setHoverTexts(Component.translatable("quest.hover.compass.reg", task.regionID).append(compass));
      // N
      addLabel(lId, x0 + 54, y + 2, "N:")
              .setSize(12, 10);
      addTextField(15, x0 + 65, y, 131, 14, task.compassEntityName)
              .setHoverTexts(Component.translatable("quest.hover.compass.entity").append(compass));
      addButton(9, x0 + 198, y, "")
              .setSize(14, 14)
              .setIsAnim(true)
              .setTexture(GuiBasic.ANIMATION_BUTTONS)
              .setUV(220, 96, 36, 36)
              .setHoverTexts("color.hover")
              .layerColor = task.colorCompass | 0xFF000000;
      // mini map point
      addCheckBox(5, x0, y += 16, "quest.set.minimap.point", null, task.isSetPointOnMiniMap())
              .setSize(150, 16)
              .setHoverTexts("quest.hover.set.minimap.point");
      // tp
      addButton(11, x0, y += 16, "TP")
              .setSize(20, 16)
              .setHoverTexts("hover.teleport");
      // set player pos
      addButton(10, x0 + 22, y, "S")
              .setSize(16, 16)
              .setHoverTexts(Component.translatable("quest.hover.compass.set").append(compass));
   }

   public void buttonEvent(GuiButtonNop guiButton) {
      if (task == null) { return; }
      switch (guiButton.id) {
         case 1: setSubGui(new SubGuiDialogSelection(task.getTargetID())); break;
         case 2: task.setAndTitle(((GuiCheckBoxNop) guiButton).selected()); break;
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

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      graphics.drawCenteredString(font,
              Component.translatable("quest.title." + (task.getEnumType() == EnumQuestTask.KILL ? "kill" : "area")),
              guiLeft + imageWidth / 2, guiTop - 12, 0xFFFFFFFF);
      Entity entity = null;
      if (scroll.getHover() != -1) { entity = dataEntities.get(scroll.getNormalList().get(scroll.getHover())); }
      else if (scroll.hasSelected()) { entity = dataEntities.get(scroll.getNormalSelected()); }
      if (entity != null) {
         drawNpc(graphics, entity, 190,208,1.0f,(int) (3 * player.level().getGameTime() % 360),0,1);
      }
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      String name = Util.instance.deleteColor(scroll.getSelected());
      getTextField(0).setValue(name);
      // clear point
      task.dimension = player.level().dimension().location();
      task.pos = BlockPos.ZERO;
      task.compassEntityName = "";
      task.entityClass = "";
      task.regionID = BorderController.getInstance().getRegionID(task.dimension.toString(), player);
      task.setAreaRange(5);
      if (dataEntities.containsKey(scroll.getNormalSelected())) {
         Entity entity = dataEntities.get(scroll.getNormalSelected());
         task.entityClass = entity.getClass().getSimpleName();
         if (!entity.blockPosition().equals(BlockPos.ZERO)) {
            task.dimension = entity.level().dimension().location();
            task.pos = entity.blockPosition();
            task.compassEntityName = name;
            int range = 5;
            if (entity instanceof EntityNPCInterface npcIn) {
               if (npcIn.ais.getMovingType() == 1) { range = npcIn.ais.getWanderingRange(); }
               else if (npcIn.ais.getMovingType() == 2) {
                  int xm = Integer.MAX_VALUE, xn = Integer.MIN_VALUE;
                  int ym = Integer.MAX_VALUE, yn = Integer.MIN_VALUE;
                  int zm = Integer.MAX_VALUE, zn = Integer.MIN_VALUE;
                  for (int[] pos : npcIn.ais.getMovingPath()) {
                     if (xm > pos[0]) { xm = pos[0]; }
                     if (xn < pos[0]) { xn = pos[0]; }
                     if (ym > pos[1]) { ym = pos[1]; }
                     if (yn < pos[1]) { yn = pos[1]; }
                     if (zm > pos[2]) { zm = pos[2]; }
                     if (zn < pos[2]) { zn = pos[2]; }
                  }
                  if (xm != Integer.MAX_VALUE) {
                     if (xm == xn) { task.pos = new BlockPos(xm, ym, zm); } // One pos
                     else {
                        task.pos = new BlockPos(xm + (xn - xm) / 2, ym + (yn - ym) / 2, zm + (zn - zm) / 2);
                        range = 5 + Math.max(xn - xm, Math.max(yn - ym, zn - zm)) / 2;
                     }
                  }
               }
            }
            task.regionID = BorderController.getInstance().getRegionID(task.dimension.toString(), task.pos);
            task.setAreaRange(Math.max(range, 32));
         }
      } // set point of entity
      task.setTargetName(getTextField(0).getValue());
      task.setMaxProgress(getTextField(1).getInteger());
      init();
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

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
      task.setMaxProgress(getTextField(1).getInteger());
   }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      if (task == null) { return; }
      switch (textField.id) {
         case 0: task.setTargetName(textField.getValue()); break;
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
   public void resetDimension() { init(); }

}
