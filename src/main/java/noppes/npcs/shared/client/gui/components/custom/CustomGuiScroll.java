package noppes.npcs.shared.client.gui.components.custom;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.chat.Component;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiScrollWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiScrollClick;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

public class CustomGuiScroll
        extends GuiCustomScrollNop
        implements IComponentCustomGui {

   protected final Object parent;
   public CustomGuiScrollWrapper component;

   public CustomGuiScroll(Object parentIn, final CustomGuiScrollWrapper componentIn) {
      super(parentIn, componentIn.getId(), componentIn.isMultiSelect());
      component = componentIn;
      listener = new ICustomScrollListener() {
         @Override
         public void scrollClicked(GuiCustomScrollNop scroll) {
            Packets.sendServer(new SPacketCustomGuiScrollClick(component.getUniqueID(), scroll.getSelectedIndex(), false));
         }
         @Override
         public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
            Packets.sendServer(new SPacketCustomGuiScrollClick(component.getUniqueID(), scroll.getSelectedIndex(), true));
         }
      };
      parent = parentIn;
      init();
   }

   public void init() {
      x = component.getPosX();
      y = component.getPosY();
      hasSearch = component.getHasSearch();
      setSize(component.getWidth(), component.getHeight());
      List<Component> list = new ArrayList<>();
      for (String line : component.getList()) { list.add(Component.translatable(line)); }
      setUnsortedList(list);
      if (!component.isMultiSelect() && component.getSelection().length <= 1 && component.getDefaultSelection() >= 0) {
         int defaultSelect = component.getDefaultSelection();
         if (defaultSelect < getList().size()) { setSelected(list.get(defaultSelect)); }
      }
      enabled = component.getEnabled();
      visible = component.getVisible();
      if (component.hasHoverText()) { hoverText = component.getHoverTextList(); }
   }

   @Override
   public void render(int mouseX, int mouseY, float partialTicks) {
      mouseInList = false;
      if (!visible) { return; }
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0F, 0.0F, (float) id);
      super.render(mouseX, mouseY, partialTicks);
      if (mouseInList && component.hasHoverText() && !hoverText.isEmpty()) {
         if (parent instanceof GuiCustom) { ((GuiCustom) parent).setHoverText(component.getHoverTextList()); }
         /*if (parent instanceof GuiCreationNewParts && ((GuiCreationNewParts) parent).listener != null) {
            ((GuiCreationNewParts) parent).listener.setHoverText(component.getHoverTextList());
         }*/
      }
      GlStateManager.popMatrix();
   }

   @Override
   public ICustomGuiComponent component() { return component; }

}
