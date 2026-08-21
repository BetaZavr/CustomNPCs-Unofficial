package noppes.npcs.api.wrapper.gui;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.gui.ITexturedRect;
import noppes.npcs.containers.ContainerCustomGui;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiComponentUpdate;
import noppes.npcs.packets.client.PacketGuiData;

@OnlyIn(Dist.DEDICATED_SERVER)
public class CustomGuiWrapper
        extends GuiComponentsWrapper
        implements ICustomGui {

   private int id;
   private int width;
   private int height;
   private boolean pauseGame;
   private boolean closesOnEsc = true;
   private final CustomGuiTexturedRectWrapper background = new CustomGuiTexturedRectWrapper();
   private final GuiComponentsScrollableWrapper scrollingPanel;
   private ScriptContainer scriptHandler;
   private CustomGuiWrapper parent;
   private CustomGuiWrapper subgui;
   public EntityCustomNpc npc;

   // New from Unofficial (BetaZavr)
   public PermissionNode<Boolean> permission = null;

   public CustomGuiWrapper(IPlayer<?> player) {
      super(player);
      scrollingPanel = new GuiComponentsScrollableWrapper(this, player);
   }

   public CustomGuiWrapper(IPlayer<?> player, int idIn, int width, int height, boolean pauseGameIn) {
      this(player);
      id = idIn;
      setSize(width, height);
      pauseGame = pauseGameIn;
      scriptHandler = ScriptContainer.Current;
      background.setId(-1);
   }

   @Override
   public int getId() { return id; }

   @Override
   public int getWidth() {
      return width;
   }

   @Override
   public int getHeight() {
      return height;
   }

   public ScriptContainer getScriptHandler() {
      return scriptHandler;
   }

   @Override
   public void setSize(int widthIn, int heightIn) {
      width = widthIn;
      height = heightIn;
      if (background.getWidth() <= 0 || background.getHeight() <= 0) { background.setSize(widthIn, heightIn); }
   }

   @Override
   public void setDoesPauseGame(boolean pauseGameIn) { pauseGame = pauseGameIn; }

   @Override
   public void setClosesOnEsc(boolean closesOnEscIn) { closesOnEsc = closesOnEscIn; }

   public boolean getClosesOnEsc() { return closesOnEsc; }

   public boolean getDoesPauseGame() { return pauseGame; }

   @Override
   public void setBackgroundTexture(String resourceLocation) {
      background.texture = resourceLocation;
   }

   @SuppressWarnings("all")
   public String getBackgroundTexture() { return background.texture; }

   public ITexturedRect getBackgroundRect() {
      return background;
   }

   @Override
   public GuiComponentsScrollableWrapper getScrollingPanel() {
      return scrollingPanel;
   }

   @Override
   public void openSubGui(ICustomGui gui) {
      subgui = (CustomGuiWrapper) gui;
      subgui.parent = this;
      subgui.npc = npc;
      getRootGui().update();
   }

   @Override
   public CustomGuiWrapper getSubGuiWrapper() { return subgui; }

   @Override
   public CustomGuiWrapper closeSubGui() {
      if (subgui == null) {
         throw new CustomNPCsException("Current gui has no subgui");
      }
      CustomGuiWrapper gui = subgui;
      subgui = null;
      player.showCustomGui(getRootGui());
      return gui;
   }

   @Override
   public void close() {
      if (parent == null) {
         player.closeGui();
      }
      else {
         parent.subgui = null;
         getRootGui().update();
      }
   }

   @Override
   public CustomGuiWrapper getParentGui() {
      return parent;
   }

   @Override
   public CustomGuiWrapper getRootGui() {
      return parent == null ? this : parent.getRootGui();
   }

   @Override
   public CustomGuiWrapper getActiveGui() {
      return subgui == null ? this : subgui.getActiveGui();
   }

   @Override
   public IPlayer<?> getPlayer() { return player; }

   @Override
   public void update() {
      if (player instanceof ServerPlayer) {
         if (player.getMCEntity().containerMenu instanceof ContainerCustomGui) { Packets.send((ServerPlayer) player.getMCEntity(), new PacketGuiData(getRootGui().toNBT())); }
         ((ContainerCustomGui) player.getMCEntity().containerMenu).setGui(getActiveGui(), player.getMCEntity());
      }
   }

   @Override
   public void update(ICustomGuiComponent component) {
      if (player instanceof ServerPlayer && player.getMCEntity().containerMenu instanceof ContainerCustomGui) {
         Packets.send((ServerPlayer) player.getMCEntity(), new PacketGuiComponentUpdate(component.getUniqueID(), ((CustomGuiComponentWrapper) component).toNBT(new CompoundTag())));
      }
   }

   public ICustomGui of(CompoundTag tag) {
      id = tag.getInt("id");
      width = tag.getIntArray("size")[0];
      height = tag.getIntArray("size")[1];
      pauseGame = tag.getBoolean("pause");
      closesOnEsc = tag.getBoolean("closesOnEsc");
      background.fromNBT(tag.getCompound("backgroundRect"));
      setComponentNbt(tag.getCompound("components"));
      scrollingPanel.setComponentNbt(tag.getCompound("scrolling_components"));
      if (tag.contains("subgui")) {
         if (subgui == null) {
            subgui = new CustomGuiWrapper(player);
            subgui.of(tag.getCompound("subgui"));
         }
      } else {
         subgui = null;
      }
      return this;
   }

   public CompoundTag toNBT() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("id", id);
      tag.putIntArray("size", new int[]{width, height});
      tag.putBoolean("pause", pauseGame);
      tag.putBoolean("closesOnEsc", closesOnEsc);
      tag.put("backgroundRect", background.toNBT(new CompoundTag()));
      tag.put("components", getComponentNbt());
      tag.put("scrolling_components", scrollingPanel.getComponentNbt());
      if (parent == null) { tag.putInt("slotSize", getActiveGui().getSlots().size()); }
      if (subgui != null) { tag.put("subgui", subgui.toNBT()); }
      return tag;
   }

   @Override
   public ICustomGuiComponent getComponentUuid(UUID id) {
      ICustomGuiComponent comp;
      if (subgui != null) {
         comp = subgui.getComponentUuid(id);
         if (comp != null) { return comp; }
      }
      comp = super.getComponentUuid(id);
      return comp != null ? comp : scrollingPanel.getComponentUuid(id);
   }

   public boolean hasSubGui() { return subgui != null; }

   // New from Unofficial (BetaZavr)
   public PermissionNode<Boolean> getPermission() { return permission; }

}
