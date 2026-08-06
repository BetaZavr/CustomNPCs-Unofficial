package noppes.npcs.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.controllers.ClientCloneController;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCloneList;
import noppes.npcs.packets.server.SPacketToolMounter;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiMenuTopButton;
import noppes.npcs.shared.client.gui.listeners.IGuiData;

public class GuiNpcMobSpawnerMounter extends GuiNPCInterface implements IGuiData {

   protected GuiCustomScrollNop scroll;
   protected final List<String> list = new ArrayList<>();
   protected static int showingClones = 0;
   protected int activeTab = 1;

   public GuiNpcMobSpawnerMounter() {
      super();
      setBackground("menubg.png");
      imageWidth = 256;
   }

   @Override
   public void init() {
      super.init();
      guiTop += 10;
      if (scroll == null) { scroll = addScroll(0).setSize(165, 210); }
      else { scroll.clear(); }
      add(scroll.setPos(guiLeft + 4, guiTop + 4));
      // clones
      GuiMenuTopButton button = addTopButton(3, guiLeft + 4, guiTop - 17, "spawner.clones");
      button.active = showingClones == 0;
      // entities
      button = addTopButton(4, button.getX() + button.getWidth(), button.getY(), "spawner.entities");
      button.active = showingClones == 1;
      // server
      addTopButton(5, button.getX() + button.getWidth(), button.getY(), "gui.server");
      button.active = showingClones == 2;
      // mount
      int x = guiLeft + 171;
      int y = guiTop + 6;
      addButton(1, x, y, "spawner.mount")
              .setSize(82, 20);
      // mountplayer
      addButton(2, x, y + 44, "spawner.mountplayer")
              .setSize(82, 20);
      addButton(66, x, y + 74, "gui.done")
              .setSize(80, 20)
              .setHoverTexts("hover.exit");
      // tabs
      if (showingClones != 0 && showingClones != 2) { showEntities(); }
      else {
         x = guiLeft;
         y = guiTop + 2;
         for (int i = 0; i < 9; i++) { addSideButton(21 + i, x, y + i * 21, Component.translatable("gui.tab").append(" " + i)); }
         getSideButton(20 + activeTab).active = true;
         showClones();
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 1: {
            String sel = scroll.getSelected();
            if (sel.isEmpty()) { return; }
            switch (showingClones) {
               case 0: Packets.sendServer(new SPacketToolMounter(0, ClientCloneController.Instance.getCloneData(player.createCommandSourceStack(), sel, activeTab))); break;
               case 1: Packets.sendServer(new SPacketToolMounter(2, sel, -1)); break;
               case 2: Packets.sendServer(new SPacketToolMounter(1, sel, activeTab)); break;
            }
            onClose();
            break;
         }
         case 2: Packets.sendServer(new SPacketToolMounter()); onClose(); break;
         case 3: showingClones = 0; init(); break;
         case 4: showingClones = 1; init(); break;
         case 5: showingClones = 2; init(); break;
         case 66: onClose(); break;
         default: {
            if (button.id > 20) {
               activeTab = button.id - 20;
               init();
            }
         }
      }
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      ListTag nbtList = compound.getList("List", 8);
      list.clear();
      for(int i = 0; i < nbtList.size(); ++i) { list.add(nbtList.getString(i)); }
      scroll.setList(list);
   }

   private void showEntities() {
      if (minecraft == null) { return; }
      list.clear();
      list.addAll(EntityUtil.getAllEntities(minecraft.level, false).keySet());
      scroll.setList(list);
   }

   private void showClones() {
      if (showingClones == 2) { Packets.sendServer(new SPacketCloneList(activeTab)); }
      else {
         list.clear();
         list.addAll(ClientCloneController.Instance.getClones(activeTab));
         scroll.setList(list);
      }
   }

}
