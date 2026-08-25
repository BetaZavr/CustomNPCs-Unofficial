package noppes.npcs.client.gui;

import java.awt.*;
import java.util.*;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.util.CustomNPCsScheduler;

public class GuiNpcRemoteEditor
        extends GuiNPCInterface
        implements IGuiData, ICustomScrollListener {

   protected GuiCustomScrollNop scroll;

   // New from Unofficial (BetaZavr)
   protected static boolean all = false;
   protected final HashMap<Component, Integer> dataIDs = new HashMap<>();
   public Entity selectEntity;

   public GuiNpcRemoteEditor() {
      super();
      setBackground("menubg.png");
      imageWidth = 256;

      Packets.sendServer(new SPacketRemoteNpcsGet(all));
   }

   @Override
   public void init() {
      super.init();
      if (scroll == null) { scroll = addScroll(0).setSize(165, 208); }
      add(scroll.setPos(guiLeft + 4, guiTop + 4));
      // title
      title = Component.translatable("remote.title");
      // edit
      int x = guiLeft + 170;
      int y = guiTop + 4;
      addButton(0, x, y, "selectServer.edit")
              .setSize(82, 18)
              .setIsEnabled(selectEntity != null && !(selectEntity instanceof Player))
              .setHoverTexts("wand.hover.edit");
      // del
      addButton(1, x, y += 20, "selectServer.delete")
              .setSize(82, 18)
              .setIsEnabled(selectEntity != null)
              .setHoverTexts("wand.hover.del");
      // reset
      addButton(2, x, y += 20, "gui.reset")
              .setSize(82, 18)
              .setIsEnabled(selectEntity instanceof EntityNPCInterface)
              .setHoverTexts("wand.hover.reset");
      // tp
      addButton(4, x, y + 20, "remote.tp")
              .setSize(82, 18)
              .setIsEnabled(selectEntity != null)
              .setHoverTexts("wand.hover.tp");
      // reset all
      addButton(5, x, y = guiTop + 174, "remote.resetall")
              .setSize(82, 18)
              .setHoverTexts("wand.hover.resetall");
      // freeze
      addButton(3, x, y + 20, "remote.freeze")
              .setSize(82, 18)
              .setHoverTexts("wand.hover.freeze");
      // New from Unofficial (BetaZavr)
      // all entities
      addCheckBox(6, x, guiTop + 87, Component.empty(), null, GuiNpcRemoteEditor.all)
              .setSize(12, 12)
              .setHoverTexts("wand.hover.showall");
      // global
      addSideButton(7, guiLeft + imageWidth, guiTop + 8, "menu.global")
              .setIsRight(true)
              .setHoverTexts("display.hover.menu.global");
   }

   @Override
   public void buttonEvent(GuiButtonNop guiButton) {
      switch (guiButton.id) {
         case 0: tryEditEntity(); break; // edit entity
         case 1: {
            if (!dataIDs.containsKey(scroll.getNormalSelected()) || minecraft == null || minecraft.level == null) { return; }
            ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
               if (bo) { Packets.sendServer(new SPacketRemoteNpcDelete(dataIDs.get(scroll.getNormalSelected()), all)); }
               NoppesUtil.openGUI(player, this);
            }, Component.empty(), Component.translatable("message.delete"));
            setScreen(guiYesNo);
            break;
         } // remove entity
         case 2: {
            if (!dataIDs.containsKey(scroll.getNormalSelected()) || minecraft == null || minecraft.level == null) { return; }
            Packets.sendServer(new SPacketRemoteNpcReset(dataIDs.get(scroll.getNormalSelected())));
            Entity entity = player.level().getEntity(dataIDs.get(scroll.getNormalSelected()));
            if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface)entity).reset(); }
            break;
         } // reset
         case 3: Packets.sendServer(new SPacketRemoteFreeze()); break; // freeze
         case 4: {
            if (!dataIDs.containsKey(scroll.getNormalSelected()) || minecraft == null || minecraft.level == null) { return; }
            Packets.sendServer(new SPacketRemoteNpcTp(dataIDs.get(scroll.getNormalSelected())));
            onClose();
            CustomNPCsScheduler.runTack(() -> Packets.sendServer(new SPacketRemoteNpcsGet(all)), 250);
            break;
         } // tp
         case 5: {
            for (int ids : dataIDs.values()) {
               Packets.sendServer(new SPacketRemoteNpcReset(ids));
               Entity entity = player.level().getEntity(ids);
               if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface) entity).reset(); }
            }
            break;
         } // reset all
         case 6: {
            GuiNpcRemoteEditor.all = ((GuiCheckBoxNop) guiButton).selected();
            Packets.sendServer(new SPacketRemoteNpcsGet(all));
            break;
         } // change all type
         case 7: {
            NoppesUtilServer.setEditingNpc(player, null);
            CustomNpcs.proxy.openGui(NoppesUtilServer.getEditingNpc(player), EnumGuiType.MainMenuGlobal, null);
            break;
         } // global tab
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!hasSubGui()) {
         PoseStack matrixStack = graphics.pose();
         matrixStack.pushPose();
         int u = guiLeft + 191;
         int v = guiTop + 85;
         matrixStack.translate(0.0f, 0.0f, 1.0f);
         graphics.fill(u, v, u + 61, v + 86, new Color(0xFF808080).getRGB());
         graphics.fill(u + 1, v + 1, u + 60, v + 85, new Color(0xFF000000).getRGB());
         if (selectEntity != null) {
            int yaw = (int) (3 * player.level().getGameTime() % 360);
            u -= guiLeft - 30;
            v -= guiTop - 77;
            if (selectEntity instanceof ItemEntity) { v -= 18; }
            drawNpc(graphics, selectEntity, u, v, 1.0f, yaw, 0, 1);
         }
         matrixStack.popPose();
         if (GuiBasic.showHoverText && isMouseHover(mouseX, mouseY, guiLeft + 191, guiTop + 85, 61, 86)) {
            setHoverText("wand.hover.entity");
         }
      }
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

   private void tryEditEntity() {
      if (!dataIDs.containsKey(scroll.getNormalSelected()) || minecraft == null || minecraft.level == null) { return; }
      Entity entity = minecraft.level.getEntity(dataIDs.get(scroll.getNormalSelected()));
      if (entity instanceof EntityNPCInterface) {
         Packets.sendServer(new SPacketRemoteMenuOpen(dataIDs.get(scroll.getNormalSelected())));
         return;
      }
      if (entity instanceof Villager villager && !villager.getOffers().isEmpty()) {
         Packets.sendServer(new SPacketVillagerMenuOpen(dataIDs.get(scroll.getNormalSelected())));
         return;
      }
      if (entity != null) {
         GuiNbtBook gui = new GuiNbtBook(entity.blockPosition());
         CompoundTag data = new CompoundTag();
         entity.save(data);
         CompoundTag compound = new CompoundTag();
         compound.putInt("EntityId", entity.getId());
         compound.put("Data", data);
         gui.setGuiData(compound);
         setScreen(gui);
      }
   }

   public void setSelected(String selected) { getButton(3).setDisplayText(selected); } // freeze

   // New from Unofficial (BetaZavr)
   @Override
   public void setGuiData(CompoundTag compound) {
      ListTag nbtList = compound.getList("Data", 10);
      dataIDs.clear();
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      if (minecraft.level == null) { return; }
      List<Component> list = new ArrayList<>();
      LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
      for (int i = 0; i < nbtList.size(); ++i) {
         CompoundTag nbt = nbtList.getCompound(i);
         int id = nbt.getInt("Id");
         MutableComponent name = Component.Serializer.fromJson(nbt.getString("Name"));
         if (name == null) { name = Component.literal("not_name"); }
         ChatFormatting type = switch (nbt.getInt("Type")) {
            case 1 -> ChatFormatting.GREEN; // friendly
            case 2 -> ChatFormatting.RED; // aggressive
            case 3 -> ChatFormatting.YELLOW; // neutral
            case 4 -> ChatFormatting.AQUA; // player
            default -> ChatFormatting.GRAY; // not living
         };
         Component distance = Component.literal(df.format(nbt.getFloat("Distance"))).withStyle(ChatFormatting.GOLD);
         MutableComponent key = Component.empty()
                 .append(Component.literal("ID:" + id + " ").withStyle(type))
                 .append(name.copy().withStyle(ChatFormatting.RESET))
                 .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                 .append(distance)
                 .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
         list.add(key);
         dataIDs.put(key, id);

         List<Component> hoverList = new ArrayList<>();
         hoverList.add(Component.literal("Name: ").withStyle(ChatFormatting.GRAY)
                 .append(name.copy().withStyle(ChatFormatting.WHITE)));
         hoverList.add(Component.literal("Entity ID: ").withStyle(ChatFormatting.GRAY)
                 .append(Component.literal("" + id).withStyle(type)));
         hoverList.add(Component.literal("Distance to: ").withStyle(ChatFormatting.GRAY)
                 .append(distance)
                 .append(Component.literal(" blocks").withStyle(ChatFormatting.GRAY)));
         hoverList.add(Component.literal("Class Type: ").withStyle(ChatFormatting.GRAY)
                 .append(Component.literal(nbt.getString("Class")).withStyle(ChatFormatting.WHITE)));
         hts.put(i, hoverList);
      }
      scroll.setUnsortedList(list);
      scroll.setHoverTexts(hts);
      resetEntity();
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) { resetEntity(); }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { tryEditEntity(); }

   private void resetEntity() {
      selectEntity = null;
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      if (minecraft.level != null && dataIDs.containsKey(scroll.getNormalSelected())) {
         selectEntity = minecraft.level.getEntity(dataIDs.get(scroll.getNormalSelected()));
         if (selectEntity == null) {
            Packets.sendServer(new SPacketRemoteNpcsEntity(dataIDs.get(scroll.getNormalSelected())));
         }
      }
      if (selectEntity != null) { init(); }
   }

}
