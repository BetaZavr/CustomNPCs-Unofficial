package noppes.npcs.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.gui.IDimensionGetter;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.DimensionController;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketDimensionDelete;
import noppes.npcs.packets.server.SPacketDimensionRestore;
import noppes.npcs.packets.server.SPacketDimensionTeleport;
import noppes.npcs.packets.server.SPacketDimensionsGet;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;

public class GuiNpcDimension extends GuiNPCInterface
        implements IDimensionGetter, IGuiData, ICustomScrollListener {

   protected final HashMap<Component, ResourceKey<Level>> data = new HashMap<>();
   protected GuiCustomScrollNop scroll;

   public GuiNpcDimension() {
      super();
      setBackground("menubg.png");
      imageWidth = 256;

      Packets.sendServer(new SPacketDimensionsGet());
   }

   @Override
   public void init() {
      super.init();
      int sw = 184;
      int x0 = guiLeft + 5;
      int x1 = x0 + sw + 2;
      int y = guiTop + 4;
      if (scroll == null) { scroll = addScroll(0).setSize(sw, 199); }
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      if (!scroll.hasSelected()) {
         for (Component key : data.keySet()) {
            if (data.get(key).equals(minecraft.player != null ?
                    minecraft.player.level().dimension() : Level.OVERWORLD)) { scroll.setSelected(key); }
         }
      }
      // title
      addLabel(0, x0, y, "gui.dimensions")
              .setSize(imageWidth - 10, 10)
              .setCenter(imageWidth - 10);
      // scroll
      add(scroll.setPos(x0, y += 10));
      ResourceKey<Level> id = data.getOrDefault(scroll.getNormalSelected(), Level.OVERWORLD);
      // tp to
      addButton(4, x1, y, "TP")
              .setSize(60, 20)
              .setIsEnabled(scroll.hasSelected() &&
                      !DimensionController.isDelete(id))
              .setHoverTexts("dimensions.hover.tp");
      // settings
      addButton(1, x1, y += 22, "gui.settings")
              .setSize(60, 20)
              .setIsEnabled(scroll.hasSelected() &&
                      DimensionController.has(id) &&
                      !DimensionController.isDelete(id))
              .setHoverTexts("dimensions.hover.settings");
      // add
      addButton(2, x1, y += 44, "gui.add")
              .setSize(60, 20)
              .setHoverTexts("dimensions.hover.add");
      // del
      addButton(3, x1, y + 22,
              DimensionController.isDelete(id) ? "gui.restore" : "gui.remove")
              .setSize(60, 20)
              .setIsEnabled(scroll.hasSelected() &&
                      !id.location().getPath().equals("custom_dimension") &&
                      id.location().getNamespace().equals(CustomNpcs.MODID) &&
                      DimensionController.has(id))
              .setHoverTexts("dimensions.hover.del");
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 1: {
            player.sendSystemMessage(Component.translatable("gui.wip"));
            if (data.containsKey(scroll.getNormalSelected())) {
               ResourceKey<Level> id = data.get(scroll.getNormalSelected());
               if (DimensionController.has(id)) {
                  FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
                  buffer.writeResourceKey(id);
                  CustomNpcs.proxy.openGui(null, EnumGuiType.DimensionSetting, buffer);
               }
            }
            break;
         } // settings
         case 2: {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeInt(0);
            CustomNpcs.proxy.openGui(null, EnumGuiType.DimensionSetting, buffer);
            break;
         } // add
         case 3: {
            if (data.containsKey(scroll.getNormalSelected())) {
               player.sendSystemMessage(Component.translatable("gui.wip"));
               ResourceKey<Level> id = data.get(scroll.getNormalSelected());
               if (DimensionController.has(id) && !DimensionController.isDelete(id)) {
                  ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
                     if (agree) {
                        Packets.sendServer(new SPacketDimensionDelete(id));
                     }
                     NoppesUtil.openGUI(player, this);
                  },
                          Component.literal("ID: " + id),
                          Component.translatable("message.delete"));
                  setScreen(guiYesNo);
               }
               else {
                  Packets.sendServer(new SPacketDimensionRestore(id));
               }
            }
            break;
         } // remove
         case 4: tp(); break;
      }
   }

   @Override
   public void resetDimension() { init(); }

   // New from Unofficial (BetaZavr)
   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) { init(); }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { tp(); }

   @Override
   public void setGuiData(CompoundTag compound) {
      data.clear();
      ListTag dimsData = compound.getList("Data", 10);
      List<Component> list = new ArrayList<>();
      List<Component> suffixes = new ArrayList<>();
      for (int i = 0; i < dimsData.size(); i++) {
         CompoundTag nbt = dimsData.getCompound(i);
         boolean isDel = nbt.getBoolean("deleted");
         boolean isLoad = nbt.getBoolean("loaded");
         ChatFormatting color = isDel ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY;
         ResourceKey<Level> id = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(NoppesUtilServer.validLocation(nbt.getString("name"))));
         Component key = Component.empty()
                 .append(Component.literal("\"").withStyle(color))
                 .append(Component.translatable(nbt.getString("name")).withStyle(isDel ? ChatFormatting.GRAY : ChatFormatting.RESET))
                 .append(Component.literal("\"").withStyle(color));
         list.add(key);
         data.put(key, id);
         boolean isMC = Level.OVERWORLD.equals(id) || Level.NETHER.equals(id) || Level.END.equals(id);
         Component sfx = Component.empty()
                 .append(Component.literal(isMC ? "MC" : "Mod").withStyle(isMC ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                 .append(Component.literal(".").withStyle(ChatFormatting.GRAY))
                 .append(Component.literal(isDel ? "delete" : isLoad ? "loaded" : "unloaded")
                         .withStyle(isDel ? ChatFormatting.DARK_RED : isLoad ? ChatFormatting.GREEN : ChatFormatting.GRAY));
         suffixes.add(sfx);
      }
      scroll.setUnsortedList(list)
              .setSuffixes(suffixes);
      init();
   }

   private void tp() {
      if (data.containsKey(scroll.getNormalSelected())) {
         Packets.sendServer(new SPacketDimensionTeleport(data.get(scroll.getNormalSelected())));
         onClose();
      }
   }

}
