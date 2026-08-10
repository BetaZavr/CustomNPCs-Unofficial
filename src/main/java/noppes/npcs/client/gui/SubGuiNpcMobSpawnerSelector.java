package noppes.npcs.client.gui;

import java.awt.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.entity.data.role.IJobSpawner;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.controllers.ClientCloneController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCloneList;
import noppes.npcs.packets.server.SPacketGetServerCloneEntity;
import noppes.npcs.roles.data.JobSpawnerCloneData;
import noppes.npcs.roles.data.JobSpawnerNbtData;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiMenuTopButton;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;

public class SubGuiNpcMobSpawnerSelector extends GuiBasic
        implements IGuiData, ICustomScrollListener, ITextfieldListener {

   protected GuiCustomScrollNop scroll;
   public int activeTab = 1;

   // New from Unofficial (BetaZavr)
   private final EntityNPCInterface npc;
   public IJobSpawner.IJobSpawnerData spawnData;
   public boolean isDead;
   public int showingClones = 2;
   public LivingEntity select;

   public SubGuiNpcMobSpawnerSelector(IJobSpawner.IJobSpawnerData spawnDataIn) {
      super();
      setBackground("menubg.png");
      imageWidth = 256;

      npc = NoppesUtilServer.getEditingNpc(player);
      spawnData = spawnDataIn;
   }

   @Override
   public void init() {
      super.init();
      guiTop += 10;
      if (scroll == null) { scroll = addScroll(0).setSize(165, 188); }
      else { scroll.clear(); }
      add(scroll.setPos(guiLeft + 4, guiTop + 26));
      GuiMenuTopButton tab = addTopButton(3, guiLeft + 4, guiTop - 17, "spawner.clones");
      tab.active = showingClones == 0;
      tab = addTopButton(4, tab.getX() + tab.getWidth(), tab.getY(), "spawner.entities");
      tab.active = showingClones == 1;
      tab = addTopButton(5, tab.getX() + tab.getWidth(), tab.getY(), "gui.server");
      tab.active = showingClones == 2;
      if (showingClones == 0 || showingClones == 2) {
         for (int id = 1; id < 10; id++) {
            addSideButton(21 + id, guiLeft, guiTop + 4 + (id - 1) * 21, Component.translatable("gui.tab").append(" " + id))
                    .active = id == activeTab;
         }
         showClones();
      }
      else { showEntities(); }
      addButton(0, guiLeft + 171, guiTop + 170, "gui.done")
              .setSize(80, 20)
              .setHoverTexts("hover.exit");
      addButton(1, guiLeft + 171, guiTop + 192, "gui.cancel")
              .setSize(80, 20);
      if (spawnData != null) {
         addLabel(5, guiLeft + 170, guiTop + 153, Component.translatable("type.count").append(":"));
         addTextField(2, guiLeft + 216, guiTop + 148, 35, 20, "" + spawnData.getCount())
                 .setMinMaxDefault(0, 7, spawnData.getCount());
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      if (button.id > 20) {
         if (activeTab != button.id - 21) {
            activeTab = button.id - 21;
            init();
         }
         return;
      }
      switch (button.id) {
         case 0: onClose(); break;
         case 1: scroll.clear(); onClose(); break;
         case 3: {
            select = null;
            showingClones = 0;
            init();
            break;
         }
         case 4: {
            select = null;
            showingClones = 1;
            init();
            break;
         }
         case 5: {
            select = null;
            showingClones = 2;
            init();
            break;
         }
      }
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      if (compound.contains("NPCData", 10)) {
         Optional<Entity> entity = EntityType.create(compound.getCompound("NPCData"), player.level());
         select = entity.isPresent() && entity.get() instanceof LivingEntity living ? living : null;
         return;
      }
      ListTag nbtList = compound.getList("List", 8);
      List<String> list = new ArrayList<>();
      for (int i = 0; i < nbtList.size(); ++i) { list.add(nbtList.getString(i)); }
      scroll.setList(list);
      if (spawnData != null) {
         scroll.setSelected(Util.instance.deleteColor(spawnData.getTitle().getString()));
         resetEntity();
      }
   }

   public String getSelected() { return scroll.getSelected(); }

   private void showClones() {
      if (showingClones == 2) { Packets.sendServer(new SPacketCloneList(activeTab)); }
      else { scroll.setList(new ArrayList<>(ClientCloneController.Instance.getClones(activeTab))); }
   }

   // New from Unofficial (BetaZavr)
   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      if (select != null && minecraft.level != null) {
         int rot;
         int cursor;
         if (isMouseHover(mouseX, mouseY, guiLeft + 182, guiTop + 5, 59, 84)) {
            rot = 0;
            cursor = 0;
         } else {
            rot = (int) (3L * minecraft.level.getGameTime() % 360L);
            cursor = 1;
         }
         drawNpc(graphics, select, 210, 80, 1.0f, rot, 0, cursor);
      }
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      matrixStack.translate(0.0f, 0.0f, 1.0f);
      graphics.fill(guiLeft + 181, guiTop + 4, guiLeft + 242, guiTop + 90, new Color(0xFF808080).getRGB());
      graphics.fill(guiLeft + 182, guiTop + 5, guiLeft + 241, guiTop + 89, new Color(0xFF000000).getRGB());
      matrixStack.popPose();
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean bo = super.keyPressed(keyCode, scanCode, modifiers);
      if (!hasSubGui()) {
         if (minecraft == null) { minecraft = Minecraft.getInstance(); }
         if (keyCode == InputConstants.getKey("key.keyboard.up").getValue() ||
                 keyCode == InputConstants.getKey("key.keyboard.down").getValue() ||
                 keyCode == minecraft.options.keyUp.getKey().getValue() ||
                 keyCode == minecraft.options.keyDown.getKey().getValue()) {
            resetEntity();
         }
      }
      return bo;
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      if (!scroll.getSelected().isEmpty()) { resetEntity(); }
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { onClose(); }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      if (spawnData == null || textField.id != 2) { return; }
      spawnData.setCount(textField.getInteger());
   }

   public CompoundTag getCompound() {
      String sel = scroll.getSelected();
      if (sel.isEmpty()) { return null; }
      CompoundTag nbtEntity = null;
      if (showingClones == 0) {
         nbtEntity = ClientCloneController.Instance.getCloneData(player.createCommandSourceStack(), sel, activeTab);
         if (nbtEntity != null) { nbtEntity.putBoolean("ClientClone", true); }
      }
      else if (showingClones == 1) {
         for (Map.Entry<EntityType<? extends Entity>, Class<? extends Entity>> entry : EntityUtil.getAllEntitiesClasses(player.level()).entrySet()) {
            if (entry.getKey().getDescriptionId().equals(sel)) {
               Entity entity = entry.getKey().create(player.level());
               if (entity instanceof LivingEntity) { entity.saveAsPassenger(nbtEntity = new CompoundTag()); }
            }
         }
      }
      return nbtEntity;
   }

   private void resetEntity() {
      String sel = scroll.getSelected();
      if (scroll.getNormalSelected().getContents() instanceof TranslatableContents tr) { sel = tr.getKey(); }
      if (showingClones == 0) {
         CompoundTag npcNbt = ClientCloneController.Instance.getCloneData(player.createCommandSourceStack(), sel, activeTab);
         if (npcNbt != null) {
            Optional<Entity> entityO = EntityType.create(npcNbt, player.level());
            select = null;
            if (entityO.isPresent() && entityO.get() instanceof LivingEntity entity) {
               npcNbt.putBoolean("ClientClone", true);
               if (spawnData instanceof JobSpawnerNbtData jobData) { jobData.load(npcNbt); }
               else {
                  spawnData = new JobSpawnerNbtData(npc);
                  ((JobSpawnerNbtData) spawnData).load(npcNbt);
               }
               select = entity;
            }
         }
      } // client
      else if (showingClones == 1) {
         for (Map.Entry<EntityType<? extends Entity>, Class<? extends Entity>> entry : EntityUtil.getAllEntitiesClasses(player.level()).entrySet()) {
            if (entry.getKey().getDescriptionId().equals(sel)) {
               Entity entity = entry.getKey().create(player.level());
               if (entity instanceof LivingEntity) {
                  select = (LivingEntity) entity;
                  CompoundTag npcNbt = new CompoundTag();
                  entity.saveAsPassenger(npcNbt);
                  npcNbt.remove("ClientClone");
                  if (spawnData instanceof JobSpawnerNbtData jobData) {
                     jobData.load(npcNbt);
                  }
                  else {
                     spawnData = new JobSpawnerNbtData(npc);
                     ((JobSpawnerNbtData) spawnData).load(npcNbt);
                  }
               }
               return;
            }
         }
      } // mob
      else  {
         if (!(spawnData instanceof JobSpawnerCloneData)) { spawnData = new JobSpawnerCloneData(npc); }
         ((JobSpawnerCloneData) spawnData).setName(sel);
         ((JobSpawnerCloneData) spawnData).setTab(activeTab);
         Packets.sendServer(new SPacketGetServerCloneEntity(false, isDead, activeTab, sel));
      } // server
   }

   private void showEntities() {
      ArrayList<String> list = new ArrayList<>();
      List<Class<? extends Entity>> classes = new ArrayList<>();
      for (Map.Entry<EntityType<? extends Entity>, Class<? extends Entity>> entry : EntityUtil.getAllEntitiesClasses(player.level()).entrySet()) {
         if (entry.getKey().getDescriptionId().contains("entity." + CustomNpcs.MODID + ".")) { continue; }
         try {
            if (classes.contains(entry.getValue()) || !LivingEntity.class.isAssignableFrom(entry.getValue()) || Modifier.isAbstract(entry.getValue().getModifiers())) { continue; }
            Entity entity = entry.getKey().create(player.level());
            if (!(entity instanceof Mob)) { continue; }
            list.add(entry.getKey().getDescriptionId());
            classes.add(entry.getValue());
         } catch (Exception e) { LogWriter.error(e); }
      }
      scroll.setList(list);
   }

}
