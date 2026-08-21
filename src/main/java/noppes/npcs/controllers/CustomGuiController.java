package noppes.npcs.controllers;

import net.minecraft.world.entity.player.Player;
import noppes.npcs.api.event.CustomGuiEvent;
import noppes.npcs.api.wrapper.WrapperNpcAPI;
import noppes.npcs.api.wrapper.gui.CustomGuiWrapper;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.containers.ContainerCustomGui;

public class CustomGuiController {

   static boolean checkGui(CustomGuiEvent event) {
      Player player = event.player.getMCEntity();
      if (!(player.containerMenu instanceof ContainerCustomGui)) { return false; }
      else { return ((ContainerCustomGui)player.containerMenu).customGui.getId() == event.gui.getId(); }
   }

   public static void onButton(CustomGuiEvent.ButtonEvent event) {
      Player player = event.player.getMCEntity();
      if (checkGui(event)) {
         CustomGuiWrapper gui = getOpenGui(player);
         if (gui != null && gui.getScriptHandler() != null) {
            ((CustomGuiWrapper) event.gui).getScriptHandler().run(EnumScriptType.CUSTOM_GUI_BUTTON.function, event);
         }
      }
      WrapperNpcAPI.EVENT_BUS.post(event);
   }

   public static void onQuickCraft(CustomGuiEvent.SlotEvent event) {
      Player player = event.player.getMCEntity();
      if (checkGui(event)) {
         CustomGuiWrapper gui = getOpenGui(player);
         if (gui != null && gui.getScriptHandler() != null) {
            ((CustomGuiWrapper) event.gui).getScriptHandler().run(EnumScriptType.CUSTOM_GUI_SLOT.function, event);
         }
      }
      WrapperNpcAPI.EVENT_BUS.post(event);
   }

   public static void onScrollClick(CustomGuiEvent.ScrollEvent event) {
      Player player = event.player.getMCEntity();
      if (checkGui(event)) {
         CustomGuiWrapper gui = getOpenGui(player);
         if (gui != null && gui.getScriptHandler() != null) {
            ((CustomGuiWrapper) event.gui).getScriptHandler().run(EnumScriptType.CUSTOM_GUI_SCROLL.function, event);
         }
      }
      WrapperNpcAPI.EVENT_BUS.post(event);
   }

   public static boolean onSlotClick(CustomGuiEvent.SlotClickEvent event) {
      Player player = event.player.getMCEntity();
      if (checkGui(event)) {
         CustomGuiWrapper gui = getOpenGui(player);
         if (gui != null && gui.getScriptHandler() != null) {
            ((CustomGuiWrapper) event.gui).getScriptHandler().run(EnumScriptType.CUSTOM_GUI_SLOT_CLICKED.function, event);
         }
      }
      return WrapperNpcAPI.EVENT_BUS.post(event);
   }

   public static void onClose(CustomGuiEvent.CloseEvent event) {
      Player player = event.player.getMCEntity();
      if (checkGui(event)) {
         CustomGuiWrapper gui = getOpenGui(player);
         if (gui != null && gui.getScriptHandler() != null) {
            ((CustomGuiWrapper) event.gui).getScriptHandler().run(EnumScriptType.CUSTOM_GUI_CLOSED.function, event);
         }
      }
      WrapperNpcAPI.EVENT_BUS.post(event);
   }

   public static CustomGuiWrapper getOpenGui(Player player) {
      return player.containerMenu instanceof ContainerCustomGui ? ((ContainerCustomGui) player.containerMenu).customGui : null;
   }

   public static void onKeyPressed(CustomGuiEvent.KeyPressedEvent event) {
      Player player = event.player.getMCEntity();
      if (checkGui(event)) {
         CustomGuiWrapper gui = getOpenGui(player);
         if (gui != null && gui.getScriptHandler() != null) {
            ((CustomGuiWrapper) event.gui).getScriptHandler().run(EnumScriptType.KEY_GUI_UP.function, event);
         }
      }
      WrapperNpcAPI.EVENT_BUS.post(event);
   }

}
