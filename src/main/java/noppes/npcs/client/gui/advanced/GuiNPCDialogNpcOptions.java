package noppes.npcs.client.gui.advanced;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.select.SubGuiDialogSelection;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.GuiSelectionListener;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import org.lwjgl.glfw.GLFW;

public class GuiNPCDialogNpcOptions
        extends GuiNPCInterface2
        implements GuiSelectionListener, IGuiData, ICustomScrollListener {

   protected int selectedSlot;

   // New from Unofficial BetaZavr
   private final HashMap<Integer, CompoundTag> data = new HashMap<>(); // slotID, dialogData
   private GuiCustomScrollNop scroll;
   private int error = 0;

   public GuiNPCDialogNpcOptions(EntityNPCInterface npc) {
      super(npc);
      drawDefaultBackground = true;

      backGui = EnumGuiType.MainMenuAdvanced;
      Packets.sendServer(new SPacketNpcDialogsGet());
   }

   @Override
   public void init() {
      super.init();
      List<Component> dialogs = new ArrayList<>();
      for (int slot : data.keySet()) {
         CompoundTag nbt = data.get(slot);
         Component key = Component.empty()
                 .append(Component.literal((slot + 1) + "; "))
                 .append(Component.literal("ID:" + nbt.getInt("Id") + " - ").withStyle(ChatFormatting.GRAY))
                 .append(Component.literal(nbt.getString("Category") + "/").withStyle(ChatFormatting.DARK_GRAY))
                 .append(Component.literal(nbt.getString("Title")).withStyle(ChatFormatting.RESET));
         dialogs.add(key);
      }
      if (scroll == null) { scroll = addScroll(0).setSize(210, 196); }
      scroll.setUnsortedList(dialogs);
      if (selectedSlot >= 0 && data.containsKey(selectedSlot)) { scroll.setSelected(selectedSlot); }
      else {
         selectedSlot = -1;
         scroll.setSelected(-1);
      }
      add(scroll.setPos(guiLeft + 5, guiTop + 14));
      // add
      addButton(1, guiLeft + 220, guiTop + 14, "gui.add")
              .setSize(64, 20)
              .setHoverTexts("dialog.hover.add");
      // del
      addButton(2, guiLeft + 220, guiTop + 36, "gui.remove")
              .setSize(64, 20)
              .setIsEnabled(selectedSlot >= 0)
              .setHoverTexts("dialog.hover.del");
      // edit
      addButton(3, guiLeft + 220, guiTop + 58, "advanced.editingmode")
              .setSize(64, 20)
              .setIsEnabled(selectedSlot >= 0)
              .setHoverTexts("dialog.hover.change");
      // up pos
      addButton(4, guiLeft + 220, guiTop + 102, "type.up")
              .setSize(64, 20)
              .setIsEnabled(selectedSlot >= 0 && selectedSlot >= 1)
              .setHoverTexts("dialog.hover.up");
      // down pos
      addButton(5, guiLeft + 220, guiTop + 124, "type.down")
              .setSize(64, 20)
              .setIsEnabled(selectedSlot >= 0 && selectedSlot < (data.size() - 1))
              .setHoverTexts("dialog.hover.down");
      // help
      addLabel(6, guiLeft + 230, guiTop + 150, "type.help")
              .setBackColor(0x80808080)
              .setBorderColor(0x80808080)
              .setHoverTexts("dialog.hover.info");
      addLabel(7, guiLeft + 6, guiTop + 4, Component.translatable("dialog.dialogs").append(Component.literal(":")))
              .setSize(207, 10);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (error > 0) {
         if (scroll != null) {
            scroll.colorBackS = 0xC0A00000;
            scroll.colorBackE = 0xC0A00000;
         }
         error--;
         if (error <= 0 && scroll != null) {
            scroll.colorBackS = 0xC0101010;
            scroll.colorBackE = 0xC0101010;
         }
      }
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 1: {
            selectedSlot = -1;
            setSubGui(new SubGuiDialogSelection(-1));
            break;
         } // add
         case 2: {
            data.clear();
            Packets.sendServer(new SPacketDialogOptionRemove(selectedSlot));
            selectedSlot = -1;
            init();
            break;
         } // del
         case 3: {
            if (data.containsKey(selectedSlot)) {
               setSubGui(new SubGuiDialogSelection(data.get(selectedSlot).getInt("Id")));
            }
            break;
         } // change
         case 4: {
            if (selectedSlot > 0) {
               Packets.sendServer(new SPacketDialogOptionMove(selectedSlot, true));
               selectedSlot--;
               init();
            }
            break;
         } // up
         case 5: {
            if (selectedSlot < data.size()) {
               Packets.sendServer(new SPacketDialogOptionMove(selectedSlot, false));
               selectedSlot++;
               init();
            }
            break;
         } // down
      }
   }

   @Override
   public void save() {}

   @Override
   public void setGuiData(CompoundTag compound) {
      if (compound.contains("Slot", 3)) {
         data.put(compound.getInt("Slot"), compound);
      }
      init();
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      selectedSlot = scroll.getSelectedIndex();
      init();
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
      if (!data.containsKey(selectedSlot)) { return; }
      setSubGui(new SubGuiDialogSelection(data.get(selectedSlot).getInt("Id")));
   }

   @Override
   public void selected(int id, String name) {
      if (selectedSlot < 0) { selectedSlot = data.size(); }
      for (int slot : data.keySet()) {
         if (selectedSlot == slot) { continue; }
         if (data.get(slot).getInt("Id") == id) {
            error = 60;
            player.sendSystemMessage(Component.empty()
                    .append(CustomNpcs.prefix)
                    .append(Component.translatable("dialog.dialog").withStyle(ChatFormatting.RESET))
                    .append(Component.literal(" ID:" + id).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" \"" + name + "\"").withStyle(ChatFormatting.RESET))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.RED))
                    .append(Component.translatable("trader.busy").withStyle(ChatFormatting.RED))
            );
            return;
         }
      }
      Packets.sendServer(new SPacketNpcDialogSet(selectedSlot, id));
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!hasSubGui() && (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_KP_DECIMAL) &&
              selectedSlot > -1 && data.containsKey(selectedSlot)) {
         data.clear();
         Packets.sendServer(new SPacketDialogOptionRemove(selectedSlot));
         selectedSlot = -1;
         init();
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

}
