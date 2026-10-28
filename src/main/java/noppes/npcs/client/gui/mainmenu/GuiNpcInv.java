package noppes.npcs.client.gui.mainmenu;

import java.util.*;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.entity.data.ICustomDrop;
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
   protected GuiCustomScrollNop scrollTemplates;
   protected GuiCustomScrollNop scrollDrops;
   protected DropsTemplate temp;

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
            CompoundTag compound = new CompoundTag();
            compound.putInt("InventoryType", 0);
            compound.putInt("DropType", inventory.dropType);
            compound.putString("GroupId", scrollTemplates.getSelected());
            compound.putInt("Pos", -1);
            compound.putInt("EntityId", npc.getId());
            Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
            break;
         } // add Drop in NPC
         case 2: {
            if (!scrollDrops.hasSelected()) { return; }
            DropSet selectedDrop = dropsData.get(scrollDrops.getNormalSelected());
            if (selectedDrop == null) { return; }
            CompoundTag compound = new CompoundTag();
            compound.putInt("InventoryType", 0);
            compound.putInt("DropType", inventory.dropType);
            compound.putString("GroupId", scrollTemplates.getSelected());
            compound.putInt("Pos", selectedDrop.pos);
            compound.putInt("EntityId", npc.getId());
            Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
            break;
         } // edit Drop in NPC
         case 3: {
            if (!scrollDrops.hasSelected()) { return; }
            DropSet selectedDrop = dropsData.get(scrollDrops.getNormalSelected());
            if (selectedDrop != null) {
               selectedDrop.item = ItemStack.EMPTY;
               Packets.sendServer(new SPacketNpcInvDropSetSave(selectedDrop.save()));
            }
            int pos = scrollDrops.getSelectedIndex();
            if (pos == 0) { pos = scrollDrops.getNormalList().size() - 1; } else { pos--; }
            scrollDrops.setSelected(pos);
            break;
         } // remove Drop in NPC
         case 10: {
            inventory.lootMode = (button.getValue() == 1);
            break;
         } // lootMode
      }
   }

   @Override
   public void init() {
      super.init();
      SubGuiDropEdit.parent = null;
      SubGuiDropEdit.parentContainer = EnumGuiType.MainMenuInv;
      SubGuiDropEdit.parentData = BlockPos.ZERO;
      // min xp
      addLabel(0,  guiLeft + 108, guiTop + 18, "inv.minExp")
              .setSize(66, 12);
      addTextField(0, guiLeft + 108, guiTop + 29, 60, 18, inventory.getExpMin())
              .setMinMaxDefault(0, Short.MAX_VALUE, 0)
              .setHoverTexts("inv.hover.drops.minxp");
      // max xp
      addLabel(1, guiLeft + 108, guiTop + 52, "inv.maxExp")
              .setSize(66, 12);
      addTextField(1, guiLeft + 108, guiTop + 63, 60, 18, inventory.getExpMax())
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
      temp = null;
      if (scrollTemplates == null) { scrollTemplates = addScroll(0); }
      if (scrollDrops == null) { scrollDrops = addScroll(1); }
      String dropName = "";
      if (scrollDrops != null && scrollDrops.hasSelected() && dropsData.get(scrollDrops.getNormalSelected()) != null) {
         dropName = dropsData.get(scrollDrops.getNormalSelected()).getItem().getDisplayName();
      }

      List<Component> templateList = new ArrayList<>();
      for (String name : DropController.getInstance().templates.keySet()) { templateList.add(Component.literal(name)); }

      // Templates
      addLabel(4, guiLeft + 176, guiTop + 27, "gui.templates")
              .setSize(96, 12)
              .setIsVisible(inventory.dropType != 0);
      add(scrollTemplates.setPos(guiLeft + 175, guiTop + 38)
              .setSize(inventory.dropType == 1 ? 238 : 99, 174)
              .setIsVisible(inventory.dropType != 0)
              .setNormalList(templateList)
              .setSelected(inventory.saveDropsName));

      // Drops
      addLabel(5, guiLeft + (inventory.dropType == 0 ? 176 : 277), guiTop + 27, "inv.drops")
              .setSize(inventory.dropType == 0 ? 236 : 138, 12)
              .setIsVisible(inventory.dropType != 1);
      LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
      List<ItemStack> stacks = new ArrayList<>();
      dropsData.clear();
      if (inventory.dropType != 1) {
         if (inventory.getDrops().length > 0) {
            int i = 0;
            for (ICustomDrop ids : inventory.getDrops()) {
               DropSet ds = (DropSet) ids;
               dropsData.put(ds.getKey(), ds);
               hts.put(i++, ds.getHover(false));
               stacks.add(ds.item);
            }
         }
      }
      add(scrollDrops.setPos(guiLeft + (inventory.dropType == 2 ? 276 : 175), guiTop + 38)
              .setSize(inventory.dropType == 2 ? 140 : 238, 157)
              .setUnsortedList(new ArrayList<>(dropsData.keySet()))
              .setHoverTexts(hts)
              .setStacks(stacks)
              .setIsVisible(inventory.dropType != 1));
      addButton(1, guiLeft + (inventory.dropType == 2 ? 277 : 176), guiTop + 197, "gui.add")
              .setSize(inventory.dropType == 2 ? 45 : 60, 15)
              .setIsVisible(inventory.dropType != 1)
              .setIsEnabled(dropsData.size() < CustomNpcs.MaxItemInDropsNPC)
              .setHoverTexts("inv.hover.add.drop", "" + CustomNpcs.MaxItemInDropsNPC);
      addButton(2, guiLeft + (inventory.dropType == 2 ? 371 : 300), guiTop + 197, "selectServer.edit")
              .setSize(inventory.dropType == 2 ? 45 : 60, 15)
              .setIsVisible(inventory.dropType != 1)
              .setIsEnabled(scrollDrops.hasSelected())
              .setHoverTexts("inv.hover.edit.drop", dropName);
      addButton(3, guiLeft + (inventory.dropType == 2 ? 324 : 238), guiTop + 197, "gui.remove")
              .setSize(inventory.dropType == 2 ? 45 : 60, 15)
              .setIsVisible(inventory.dropType != 1)
              .setIsEnabled(scrollDrops.hasSelected())
              .setHoverTexts("inv.hover.del.drop", dropName);
   }

   @Override
   protected void renderBg(@Nonnull GuiGraphics graphics, float partialTicks, int x, int y) {
      super.renderBg(graphics, partialTicks, x, y);
      for (int i = 0; i < 4; i++) { displayNpc.inventory.setArmor(i, npc.inventory.getArmor(i)); }
      displayNpc.inventory.setRightHand(npc.inventory.getRightHand());
      displayNpc.inventory.setProjectile(npc.inventory.getProjectile());
      displayNpc.inventory.setLeftHand(npc.inventory.getLeftHand());
      displayNpc.tickCount = npc.tickCount;
      drawNpc(graphics, displayNpc, 50, 84, 1.0f, 0, 0, 1);
   }

   @Override
   public void save() {
      Packets.sendServer(new SPacketMenuSave(EnumMenuType.INVENTORY, npc.inventory.save(new CompoundTag())));
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      if (scroll.hasSelected()) {
         if (scroll.id == 0) {
            if (scroll.getSelected().equals(inventory.saveDropsName)) {
               inventory.saveDropsName = "";
               scroll.setSelected(-1);
            }
            else { inventory.saveDropsName = scroll.getSelected(); }
         }
         init();
      }
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
      if (scroll.id == 1) {
         DropSet selectedDrop = dropsData.get(scrollDrops.getNormalSelected());
         if (selectedDrop != null) {
            CompoundTag compound = new CompoundTag();
            compound.putInt("InventoryType", 0);
            compound.putInt("DropType", inventory.dropType);
            compound.putString("GroupId", scrollTemplates.getSelected());
            compound.putInt("Pos", selectedDrop.pos);
            compound.putInt("EntityId", npc.getId());
            Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
         }
      }
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      if (compound.contains("NpcInv", 9)) {
         inventory.load(compound);
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

}
