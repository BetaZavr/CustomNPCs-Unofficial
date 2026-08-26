package noppes.npcs.client.gui.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.client.gui.mainmenu.GuiNpcDisplay;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.parts.ModelData;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiSliderNop;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;

public abstract class GuiCreationScreenInterface extends GuiNPCInterface implements ISliderListener {

   protected static float rotation = 0.5F;
   protected boolean saving = false;
   protected boolean hasSaving = true;
   protected CompoundTag original;
   protected final Player player;
   public int active = 0;
   public int xOffset;
   public static String Message = "";
   public LivingEntity entity;
   public ModelData playerdata;

   public GuiCreationScreenInterface(EntityNPCInterface npc) {
      super(npc);
      playerdata = ((EntityCustomNpc)npc).modelData;
      original = playerdata.save();
      imageWidth = 400;
      imageHeight = 240;
      xOffset = 140;
      player = Minecraft.getInstance().player;
      drawDefaultBackground = true;
   }

   @Override
   public void init() {
      super.init();
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      entity = playerdata.getEntity(npc);
      add(new GuiButtonNop(this, 1, "gui.entity", guiLeft + 62, guiTop,
              button -> openGui(new GuiCreationEntities(npc)))
              .setSize(60, 20));
      if (entity == null) {
         add(new GuiButtonNop(this, 2, "gui.parts", guiLeft, guiTop + 23,
                 button -> {
                    if (npc instanceof EntityCustomNpc customNpc) { openGui(new GuiCreationParts(customNpc)); }
                    // save(); Packets.sendServer(new SPacketOpenParts());
                 }).setSize(60, 20));
      }
      else if (!(entity instanceof EntityNPCInterface)) {
         GuiCreationExtra gui = new GuiCreationExtra(npc);
         gui.playerdata = playerdata;
         if (!gui.getData(entity).isEmpty()) {
            add(new GuiButtonNop(this, 2, "gui.scale", guiLeft, guiTop + 23,
                    button -> openGui(new GuiCreationExtra(npc))).setSize(60, 20));
         }
         else if (active == 2) {
            minecraft.setScreen(new GuiCreationEntities(npc));
            return;
         }
      }
      if (entity == null) {
         add(new GuiButtonNop(this, 3, "gui.scale", guiLeft + 62, guiTop + 23,
                 button -> openGui(new GuiCreationScale(npc))).setSize(60, 20));
      }
      if (hasSaving) {
         add(new GuiButtonNop(this, 4, "gui.save", guiLeft, guiTop + imageHeight - 24,
                 button -> setSubGui(new GuiPresetSave(playerdata))).setSize(60, 20));
         add(new GuiButtonNop(this, 5, "gui.load", guiLeft + 62, guiTop + imageHeight - 24,
                 button -> openGui(new GuiCreationLoad(npc))).setSize(60, 20));
      }
      if (getButton(active) == null) { openGui(new GuiCreationEntities(npc)); }
      else {
         getButton(active).active = false;
         add(new GuiButtonNop(this, 66, "X", guiLeft + imageWidth - 20, guiTop, button -> {
            save();
            NoppesUtil.openGUI(player, new GuiNpcDisplay(npc));
         }).setSize(20, 20));
         addLabel(0, guiLeft + 120, guiTop + imageHeight - 10, Message)
                 .setSize(imageWidth - 120, 20)
                 .setColor(CustomNpcs.MainColor.getRGB());
         addSlider(500, guiLeft + xOffset + 142, guiTop + 210, rotation)
                 .setSize(120, 20);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (!saving) {
         super.mouseClicked(mouseX, mouseY, mouseButton);
      }
      return true;
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      if (!hasSubGui() || getSubGui() instanceof SubGuiColorSelector) {
         entity = playerdata.getEntity(npc);
         EntityUtil.Copy(npc, entity);
         drawNpc(graphics, npc, xOffset + 200, 200, 2.0F, (int) (-rotation * 360.0F - 180.0F), 0, 0);
      }
   }

   @Override
   public void onClose() {
      super.onClose();
      NoppesUtil.requestOpenGUI(EnumGuiType.MainMenuDisplay);
   }

   @Override
   public void save() {
      CompoundTag newCompound = playerdata.save();
      Packets.sendServer(new SPacketMenuSave(EnumMenuType.DISPLAY, npc.display.save(new CompoundTag())));
      Packets.sendServer(new SPacketMenuSave(EnumMenuType.MODEL, newCompound));
   }

   public void openGui(Screen gui) {
      if (minecraft != null) { minecraft.setScreen(gui); }
   }

   @Override
   public void subGuiClosed(Screen subgui) {
      init();
   }

   @Override
   public void mouseDragged(GuiSliderNop slider) {
      if (slider.id == 500) {
         rotation = slider.sliderValue;
         slider.setString((int)(rotation * 360.0F));
      }
   }

   @Override
   public void mousePressed(GuiSliderNop slider) { }

   @Override
   public void mouseReleased(GuiSliderNop slider) { }

}
