package noppes.npcs.client.gui.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.entity.data.ICustomDrop;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.drop.SubGuiDropEdit;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface2;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.containers.ContainerNPCInv;
import noppes.npcs.controllers.DropController;
import noppes.npcs.controllers.data.DropsTemplate;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

// Changed from Unofficial (BetaZavr)
public class GuiNpcInv extends GuiContainerNPCInterface2<ContainerNPCInv>
        implements ICustomScrollListener, IGuiData, ITextfieldListener {

   protected final ContainerNPCInv container;
   protected final Map<Component, DropSet> dropsData = new HashMap<>();
   protected final EntityNPCInterface displayNpc;
   protected final DataInventory inventory;
   protected GuiCustomScrollNop scrollTemplate;
   protected GuiCustomScrollNop scrollDrops;
   protected DropsTemplate temp;
   protected int groupId = 0;

   public GuiNpcInv(ContainerNPCInv containerIn, Inventory inv, Component titleIn) {
      super(NoppesUtilServer.getEditingNpc(Minecraft.getInstance().player), containerIn, inv, titleIn, 4);
      setBackground("npcinv.png");
      imageHeight = 200;

      container = containerIn;
      displayNpc = Util.instance.copyToGUI(npc, player.level(), false);
      inventory = npc.inventory;
      Packets.sendServer(new SPacketMenuGet(EnumMenuType.INVENTORY));
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 0: {
            inventory.dropType = button.getValue();
            init();
            break;
         } // lootMode
         case 1: {
            SubGuiDropEdit.parent = null;
            SubGuiDropEdit.parentContainer = EnumGuiType.MainMenuInv;
            SubGuiDropEdit.parentData = BlockPos.ZERO;
            CompoundTag compound = new CompoundTag();
            compound.putInt("InventoryType", 0);
            compound.putInt("DropType", inventory.dropType);
            compound.putInt("GroupId", groupId);
            compound.putInt("Pos", -1);
            compound.putInt("EntityId", npc.getId());
            Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
            break;
         } // add Drop in NPC
         case 2: {
            if (!scrollDrops.hasSelected()) { return; }
            SubGuiDropEdit.parent = null;
            SubGuiDropEdit.parentContainer = EnumGuiType.MainMenuInv;
            SubGuiDropEdit.parentData = BlockPos.ZERO;
            CompoundTag compound = new CompoundTag();
            compound.putInt("InventoryType", 0);
            compound.putInt("DropType", inventory.dropType);
            compound.putInt("GroupId", groupId);
            compound.putInt("Pos", scrollDrops.getSelectedIndex());
            compound.putInt("EntityId", npc.getId());
            Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
            break;
         } // edit Drop in NPC
         case 3: {
            if (!scrollDrops.hasSelected()) { return; }
            CompoundTag compound = new CompoundTag();
            compound.put("Item", ItemStack.EMPTY.save(new CompoundTag()));
            Packets.sendServer(new SPacketNpcInvDropSetSave(inventory.dropType, groupId, scrollDrops.getSelectedIndex(), compound));
            break;
         } // remove Drop in NPC
         case 4: {
            setSubGui(new SubGuiEditText(1, Component.translatable("gui.new").getString()));
            break;
         } // create template
         case 5: {
            if (scrollTemplate == null || !scrollTemplate.hasSelected() || temp == null) { return; }
            String name = scrollTemplate.getSelected();
            DropsTemplate dt = DropsTemplate.from(temp);
            DropController dData = DropController.getInstance();
            while (dData.templates.containsKey(name)) { name += "_"; }
            inventory.saveDropsName = name;
            dData.templates.put(inventory.saveDropsName, dt);
            temp = dt;
            init();
            break;
         } // copy template
         case 6: {
            setSubGui(new SubGuiEditText(2, inventory.saveDropsName));
            break;
         } // edit template name
         case 7: {
            if (scrollTemplate == null || !scrollTemplate.hasSelected()) { return; }
            ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
               if (agree) {
                  if (scrollTemplate == null || !scrollTemplate.hasSelected()) { return; }
                  DropController.getInstance().templates.remove(scrollTemplate.getSelected());
                  Packets.sendServer(new SPacketDropTemplateRemove(scrollTemplate.getSelected()));
                  init();
               }
               NoppesUtil.openGUI(player, this);
            },
                    scrollTemplate.getNormalSelected(),
                    Component.translatable("message.delete"));
            setScreen(guiYesNo);
         } // del template
         case 8: {
            groupId = button.getValue();
            init();
            break;
         } // group ID
         case 9: {
            groupId = temp.groups.size();
            temp.groups.put(groupId, new TreeMap<>());
            init();
            break;
         } // add Drop
         case 10: {
            inventory.lootMode = (button.getValue() == 1);
            break;
         } // lootMode
         case 11: {
            if (temp == null) { return; }
            Map<Integer, DropSet> parent = temp.groups.get(groupId);
            Map<Integer, DropSet> newGroup = new TreeMap<>();
            for (int id : parent.keySet()) {
               DropSet ds = new DropSet(inventory);
               ds.load(parent.get(id).save());
               newGroup.put(id, ds);
            }
            groupId = temp.groups.size();
            temp.groups.put(groupId, newGroup);
            init();
            break;
         } // copy Group
         case 12: {
            if (temp == null || temp.groups.isEmpty() || groupId <= 0) { return; }
            if (temp.groups.get(groupId).isEmpty()) {
               temp.removeGroup(groupId);
               groupId--;
               init();
            } else {
               ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
                  if (agree) {
                     if (temp == null || temp.groups.isEmpty()) { return; }
                     temp.removeGroup(groupId);
                     groupId--;
                     init();
                  }
                  NoppesUtil.openGUI(player, this);
               },
                       Component.translatable("gui.group").append(" ID: " + groupId),
                       Component.translatable("message.delete"));
               setScreen(guiYesNo);
            }
            break;
         } // del Group
      }
   }

   @Override
   public void init() {
      super.init();
      // min xp
      addLabel(0,  guiLeft + 108, guiTop + 18, "inv.minExp")
              .setSize(66, 12);
      addTextField(0, guiLeft + 108, guiTop + 29, 60, 20, inventory.getExpMin())
              .setMinMaxDefault(0, Short.MAX_VALUE, 0)
              .setHoverTexts("inv.hover.drops.minxp");
      // max xp
      addLabel(1, guiLeft + 108, guiTop + 52, "inv.maxExp")
              .setSize(66, 12);
      addTextField(1, guiLeft + 108, guiTop + 63, 60, 20, inventory.getExpMax())
              .setMinMaxDefault(0, Short.MAX_VALUE, 0)
              .setHoverTexts("inv.hover.drops.maxxp");
      // xp loot mode
      addButton(10, guiLeft + 107, guiTop + 88, false, inventory.lootMode ? 1 : 0, "stats.normal", "inv.auto")
              .setSize(62, 20)
              .setHoverTexts("inv.hover.auto.xp");
      // drop type
      addLabel(2, guiLeft + 176, guiTop + 5, "inv.npcInventory")
              .setSize(82, 12);
      addLabel(3, guiLeft + 8, guiTop + 101, "inv.inventory")
              .setSize(166, 12);
      addButton(0, guiLeft + 260, guiTop + 4, false, inventory.dropType, "inv.use.drops.0", "inv.use.drops.1", "inv.use.drops.2")
              .setSize(100, 20)
              .setHoverTexts("inv.hover.drops.type");
      // max amount
      addTextField(2, guiLeft + 364, guiTop + 5, 48, 18, "" + inventory.limitation)
              .setMinMaxDefault(0, 128, inventory.limitation)
              .setHoverTexts("inv.hover.drops.amount");
      // data
      dropsData.clear();
      temp = null;
      String dropName = "";
      if (scrollDrops != null && scrollDrops.hasSelected() && dropsData.get(scrollDrops.getNormalSelected()) != null) {
         dropName = dropsData.get(scrollDrops.getNormalSelected()).getItem().getDisplayName();
      }
      LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
      List<ItemStack> stacks = new ArrayList<>();
      List<Component> list = new ArrayList<>();
      if (inventory.dropType == 0) {
         if (inventory.getDrops().length > 0) {
            int i = 0;
            for (ICustomDrop ids : inventory.getDrops()) {
               DropSet ds = (DropSet) ids;
               if (ds.pos != i) { ds.pos = i; }
               Component key = ds.getKey();
               dropsData.put(key, ds);
               list.add(key);
               hts.put(i++, ds.getHover(false));
               stacks.add(ds.item);
            }
         }
         if (scrollDrops == null) { scrollDrops = addScroll(1).setSize(238, 157); }
         add(scrollDrops.setPos(guiLeft + 175, guiTop + 38)
                 .setUnsortedList(list)
                 .setHoverTexts(hts)
                 .setStacks(stacks));
         addLabel(4, guiLeft + 176, guiTop + 27, "inv.drops")
                 .setSize(236, 12);
         addButton(1, guiLeft + 175, guiTop + 197, "gui.add")
                 .setSize(60, 15)
                 .setIsEnabled(dropsData.size() < CustomNpcs.MaxItemInDropsNPC)
                 .setHoverTexts("inv.hover.add.drop", "" + CustomNpcs.MaxItemInDropsNPC);
         addButton(2, guiLeft + 240, guiTop + 197, "selectServer.edit")
                 .setSize(60, 15)
                 .setIsEnabled(scrollDrops.hasSelected())
                 .setHoverTexts("inv.hover.edit.drop", dropName);
         addButton(3, guiLeft + 305, guiTop + 197, "gui.remove")
                 .setSize(60, 15)
                 .setIsEnabled(scrollDrops.hasSelected())
                 .setHoverTexts("inv.hover.del.drop", dropName);
      }
      else if (inventory.dropType == 1) {
         addLabel(4, guiLeft + 176, guiTop + 27, "gui.templates")
                 .setSize(96, 12);
         if (scrollTemplate == null) { scrollTemplate = addScroll(0).setSize(98, 140); }
         add(scrollTemplate.setPos(guiLeft + 175, guiTop + 38)
                 .setList(new ArrayList<>(DropController.getInstance().templates.keySet())));
         if (DropController.getInstance().templates.containsKey(inventory.saveDropsName)) {
            temp = DropController.getInstance().templates.get(inventory.saveDropsName);
            scrollTemplate.setSelected(inventory.saveDropsName);
            if (temp.groups.containsKey(groupId)) {
               int i = 0;
               for (DropSet ds : temp.groups.get(groupId).values()) {
                  if (ds.pos != i) { ds.pos = i; }
                  Component key = ds.getKey();
                  dropsData.put(key, ds);
                  list.add(key);
                  hts.put(i++, ds.getHover(false));
                  stacks.add(ds.item);
               }
            }
         }
         else { groupId = 0; }
         addButton(4, guiLeft + 175, guiTop + 180, "gui.add")
                 .setSize(48, 15)
                 .setHoverTexts("inv.hover.new.template");
         addButton(5, guiLeft + 175, guiTop + 197, "gui.copy")
                 .setSize(48, 15)
                 .setIsEnabled(!inventory.saveDropsName.isEmpty() && !inventory.saveDropsName.equals("default"))
                 .setHoverTexts("inv.hover.copy.template");
         addButton(6, guiLeft + 225, guiTop + 180, "selectServer.edit")
                 .setSize(48, 15)
                 .setIsEnabled(!inventory.saveDropsName.isEmpty() && !inventory.saveDropsName.equals("default"))
                 .setHoverTexts("inv.hover.rename.template");
         addButton(7, guiLeft + 225, guiTop + 197, "gui.remove")
                 .setSize(48, 15)
                 .setIsEnabled(!inventory.saveDropsName.isEmpty() && !inventory.saveDropsName.equals("default"))
                 .setHoverTexts("inv.hover.del.template");
         addLabel(5, guiLeft + 277, guiTop + 30, "gui.groups")
                 .setSize(67, 12);
         int g = 1;
         if (temp != null && !temp.groups.isEmpty()) { g = temp.groups.size(); }
         Object[] l = new String[g];
         for (int i = 0; i < g; i++) { l[i] = (i + 1)+ " / " + g; }
         addButton(8, guiLeft + 346, guiTop + 27, true, groupId, l)
                 .setSize(70, 15)
                 .setHoverTexts("inv.hover.group.id");
         if (scrollDrops == null) { scrollDrops = addScroll(1).setSize(140, 117); }
         add(scrollDrops.setPos(guiLeft + 276, guiTop + 44)
                 .setUnsortedList(list)
                 .setHoverTexts(hts)
                 .setStacks(stacks));
         // Stacks
         addButton(1, guiLeft + 330, guiTop + 163, "gui.add")
                 .setSize(48, 15)
                 .setIsEnabled(!inventory.saveDropsName.isEmpty())
                 .setHoverTexts("inv.hover.add.drop", "9000");
         addButton(2, guiLeft + 330, guiTop + 180, "selectServer.edit")
                 .setSize(48, 15)
                 .setIsEnabled(scrollDrops.hasSelected())
                 .setHoverTexts("inv.hover.edit.drop", dropName);
         addButton(3, guiLeft + 330, guiTop + 197, "gui.remove")
                 .setSize(48, 15)
                 .setIsEnabled(scrollDrops.hasSelected() && !(inventory.saveDropsName.equals("default") && scrollDrops.getSelectedIndex() < 4))
                 .setHoverTexts("inv.hover.del.drop", dropName);
         // Groups
         addButton(9, guiLeft + 277, guiTop + 163, "gui.add")
                 .setSize(48, 15)
                 .setHoverTexts("inv.hover.add.group");
         addButton(11, guiLeft + 277, guiTop + 180, "gui.copy")
                 .setSize(48, 15)
                 .setIsEnabled(temp != null && temp.groups.containsKey(groupId))
                 .setHoverTexts("inv.hover.copy.group");
         addButton(12, guiLeft + 277, guiTop + 197, "gui.remove")
                 .setSize(48, 15)
                 .setIsEnabled(temp != null && temp.groups.containsKey(groupId) && groupId > 0)
                 .setHoverTexts("inv.hover.del.group");
      }
      else {
         addLabel(4, guiLeft + 176, guiTop + 27, "gui.templates")
                 .setSize(96, 12);
         addLabel(5, guiLeft + 277, guiTop + 27, "inv.drops")
                 .setSize(138, 12);
         if (inventory.getDrops().length > 0) {
            int i = 0;
            for (ICustomDrop ids : inventory.getDrops()) {
               DropSet ds = (DropSet) ids;
               if (ds.pos != i) { ds.pos = i; }
               Component key = ds.getKey();
               dropsData.put(key, ds);
               list.add(key);
               hts.put(i++, ds.getHover(false));
               stacks.add(ds.item);
            }
         }
         if (scrollTemplate == null) { scrollTemplate = addScroll(0).setSize(98, 174); }
         add(scrollTemplate.setPos(guiLeft + 175, guiTop + 38)
                 .setList(new ArrayList<>(DropController.getInstance().templates.keySet())));
         if (DropController.getInstance().templates.containsKey(inventory.saveDropsName)) {
            temp = DropController.getInstance().templates.get(inventory.saveDropsName);
            scrollTemplate.setSelected(inventory.saveDropsName);
         }
         else { groupId = 0; }
         if (scrollDrops == null) { scrollDrops = addScroll(1).setSize(140, 157); }
         add(scrollDrops.setPos(guiLeft + 276, guiTop + 38)
                 .setUnsortedList(list)
                 .setHoverTexts(hts)
                 .setStacks(stacks));
         addButton(1, guiLeft + 277, guiTop + 197, "gui.add")
                 .setSize(45, 15)
                 .setIsEnabled(dropsData.size() < CustomNpcs.MaxItemInDropsNPC)
                 .setHoverTexts("inv.hover.add.drop", "" + CustomNpcs.MaxItemInDropsNPC);
         addButton(2, guiLeft + 324, guiTop + 197, "selectServer.edit")
                 .setSize(45, 15)
                 .setIsEnabled(scrollDrops.hasSelected())
                 .setHoverTexts("inv.hover.edit.drop", dropName);
         addButton(3, guiLeft + 371, guiTop + 197, "gui.remove")
                 .setSize(45, 15)
                 .setIsEnabled(scrollDrops.hasSelected())
                 .setHoverTexts("inv.hover.del.drop", dropName);
      }
   }

   @Override
   protected void renderBg(@Nonnull GuiGraphics graphics, float partialTicks, int x, int y) {
      super.renderBg(graphics, partialTicks, x, y);
      //PoseStack matrixPose = graphics.pose();
      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
      RenderSystem.setShaderTexture(0, GuiBasic.RESOURCE_SLOT);
      for (int id = 4; id <= 8; ++id) {
         Slot slot = container.getSlot(id);
         /*if (id > 6 && ArmourersWorkshopClientApi.getSkinRenderHandler() != null) {
            graphics.blit(GuiNPCInterface.RESOURCE_SLOT, guiLeft + slot.x - 1, guiTop + slot.y - 1, 0, 0, 18, 18);
            if (!slot.hasItem()) {
               matrixPose.pushPose();
               matrixPose.translate(guiLeft + slot.x, guiTop + slot.y, 0.0f);
               matrixPose.scale(0.0625f, 0.0625f, 0.0625f);
               graphics.blit(id == 7 ? ArmourersWorkshopUtil.getInstance().slotOutfit : ArmourersWorkshopUtil.getInstance().slotWings, 0, 0, 0, 0, 256, 256);
               matrixPose.popPose();
            }
         }
         else if (slot.hasItem()) {*/
            graphics.blit(GuiBasic.RESOURCE_SLOT, guiLeft + slot.x - 1, guiTop + slot.y - 1, 0, 0, 18, 18);
         //}
      }
      int color = new Color(0xFF606060).getRGB();
      if (inventory.dropType != 0) { graphics.vLine(guiLeft + 274, guiTop + 26, guiTop + imageHeight + 12, color); }
      if (inventory.dropType == 1) { graphics.vLine(guiLeft + 327, guiTop + 162, guiTop + imageHeight + 12, color); }
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      for (int i = 0; i < 4; i++) { displayNpc.inventory.setArmor(i, npc.inventory.getArmor(i)); }
      displayNpc.inventory.setRightHand(npc.inventory.getRightHand());
      displayNpc.inventory.setProjectile(npc.inventory.getProjectile());
      displayNpc.inventory.setLeftHand(npc.inventory.getLeftHand());
      displayNpc.tickCount = npc.tickCount;
      drawNpc(graphics, displayNpc, 52, 84, 1.0f, 0, 0, 0);
   }

   @Override
   public void save() {
      saveTemplate();
      Packets.sendServer(new SPacketMenuSave(EnumMenuType.INVENTORY, npc.inventory.save(new CompoundTag())));
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      if (compound.contains("NpcInv", 9)) {
         inventory.load(compound);
         init();
      }
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      if (scroll.hasSelected()) {
         if (scroll.id == 0) {
            saveTemplate();
            if (scroll.getSelected().equals(inventory.saveDropsName)) {
               inventory.saveDropsName = "";
               scroll.setSelected(-1);
            }
            else { inventory.saveDropsName = scroll.getSelected(); }
            Packets.sendServer(new SPacketMenuSave(EnumMenuType.INVENTORY, npc.inventory.save(new CompoundTag())));
            if (inventory.dropType == 1) { groupId = 0; }
         }
         init();
      }
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
      if (scroll.id == 1) {
         if (dropsData.get(scrollDrops.getNormalSelected()) != null) {
            saveTemplate();
            SubGuiDropEdit.parent = null;
            SubGuiDropEdit.parentContainer = EnumGuiType.MainMenuInv;
            SubGuiDropEdit.parentData = BlockPos.ZERO;
            CompoundTag compound = new CompoundTag();
            compound.putInt("InventoryType", 0);
            compound.putInt("DropType", inventory.dropType);
            compound.putInt("GroupId", groupId);
            compound.putInt("Pos", scrollDrops.getSelectedIndex());
            compound.putInt("EntityId", npc.getId());
            Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
         }
      }
   }

   @Override
   public void subGuiClosed(Screen subgui) {
      if (subgui instanceof SubGuiEditText gui && !gui.cancelled) {
         DropController dData = DropController.getInstance();
         String name = gui.text[0];
         if (gui.id == 1) {
            while (dData.templates.containsKey(name)) { name += "_"; }
            inventory.saveDropsName = name;
            dData.templates.put(inventory.saveDropsName, new DropsTemplate());
            CompoundTag nbtTemplate = new CompoundTag();
            nbtTemplate.putString("Name", name);
            nbtTemplate.put("Groups", dData.templates.get(inventory.saveDropsName).getNBT());
            Packets.sendServer(new SPacketDropTemplateSave(nbtTemplate));
         } // create template
         else if (gui.id == 2) {
            if (name == null || name.equals(inventory.saveDropsName) || dData.templates.containsKey(name) || !dData.templates.containsKey(inventory.saveDropsName)) { return; }
            dData.templates.put(name, dData.templates.get(inventory.saveDropsName));
            dData.templates.remove(inventory.saveDropsName);
            Packets.sendServer(new SPacketDropTemplateRemove(inventory.saveDropsName));
            CompoundTag nbtTemplate = new CompoundTag();
            nbtTemplate.putString("Name", name);
            nbtTemplate.put("Groups", dData.templates.get(name).getNBT());
            Packets.sendServer(new SPacketDropTemplateSave(nbtTemplate));
         } // rename template
         init();
      }
   }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      switch(textField.id) {
         case 0: inventory.setExp(textField.getInteger(), getTextField(1).getInteger()); break;
         case 1: inventory.setExp(getTextField(0).getInteger(), textField.getInteger()); break;
         case 2: inventory.limitation = getTextField(2).getInteger(); break;
      }
   }

   private void saveTemplate() {
      if (inventory.dropType == 1 && inventory.saveDropsName != null && !inventory.saveDropsName.isEmpty()) {
         DropController.getInstance().sendToServer(inventory.saveDropsName);
      }
   }

}
