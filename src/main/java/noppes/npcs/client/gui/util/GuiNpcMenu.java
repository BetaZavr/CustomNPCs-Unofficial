package noppes.npcs.client.gui.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuClose;
import noppes.npcs.packets.server.SPacketNpcDelete;
import noppes.npcs.packets.server.SPacketPermissionMenuGet;
import noppes.npcs.shared.client.gui.components.GuiMenuTopButton;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

import java.util.Arrays;

public class GuiNpcMenu {

   protected final IGuiInterface parent;
   protected final EntityNPCInterface npc;
   protected final Minecraft mc;
   protected GuiMenuTopButton[] topButtons = new GuiMenuTopButton[0];
   protected int activeMenu;

   // New from Unofficial (BetaZavr)
   public boolean[] permissions = new boolean[5];

   public GuiNpcMenu(IGuiInterface parentIn, int activeMenuIn, EntityNPCInterface npcIn) {
      parent = parentIn;
      npc = npcIn;
      activeMenu = activeMenuIn;
      mc = Minecraft.getInstance();
      Arrays.fill(permissions, true);
      Packets.sendServer(new SPacketPermissionMenuGet());
   }

   public void init(int guiLeft, int guiTop, int width) {
      if (npc != null) {
         GuiMenuTopButton display = new GuiMenuTopButton(parent, 1, "menu.display", guiLeft + 4, guiTop - 17) {
            @Override
            public void onClick(double x, double y) {
               save();
               activeMenu = 1;
               NoppesUtilServer.setEditingNpc(Minecraft.getInstance().player, npc);
               CustomNpcs.proxy.openGui(npc, EnumGuiType.MainMenuDisplay, null);
            }
         };
         display.setHoverTexts(Component.empty()
                         .append(Component.translatable("gui.name").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(npc.display.getName()).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)),
                 Component.empty()
                         .append(Component.translatable("gui.title").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": <").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(npc.display.getTitle()).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(">;").withStyle(ChatFormatting.GRAY)),
                 Component.empty()
                         .append(Component.translatable("display.model").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                         .append(Component.translatable("display.size").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal("" + npc.display.getSize()).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)));
         GuiMenuTopButton stats = new GuiMenuTopButton(parent, 2, "menu.stats", display.getX() + display.getWidth(), guiTop - 17) {
            @Override
            public void onClick(double x, double y) {
               save();
               activeMenu = 2;
               NoppesUtilServer.setEditingNpc(Minecraft.getInstance().player, npc);
               CustomNpcs.proxy.openGui(npc, EnumGuiType.MainMenuStats, null);
            }
         };
         String str0 = switch (npc.stats.spawnCycle) {
            case 0 -> "gui.yes";
            case 1 -> "gui.day";
            case 2 -> "gui.night";
            case 4 -> "stats.naturally";
            default -> "gui.no";
         };
         stats.setHoverTexts(Component.empty()
                         .append(Component.translatable("stats.health").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal("" + npc.stats.maxHealth).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)),
                 Component.empty()
                         .append(Component.translatable("stats.aggro").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal("" + npc.stats.aggroRange).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)),
                 Component.empty()
                         .append(Component.translatable("stats.respawn").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.translatable(str0).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)),
                 Component.empty()
                         .append(Component.translatable("stats.meleeproperties").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                         .append(Component.translatable("stats.meleestrength").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.translatable("" + npc.stats.melee.getStrength()).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)),
                 Component.empty()
                         .append(Component.translatable("stats.rangedproperties").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                         .append(Component.translatable("enchantment.minecraft.power").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.translatable("" + npc.stats.ranged.getStrength()).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)));
         GuiMenuTopButton ai = new GuiMenuTopButton(parent, 3, "menu.ai", stats.getX() + stats.getWidth(), guiTop - 17) {
            @Override
            public void onClick(double x, double y) {
               save();
               activeMenu = 3;
               NoppesUtilServer.setEditingNpc(Minecraft.getInstance().player, npc);
               CustomNpcs.proxy.openGui(npc, EnumGuiType.MainMenuAI, null);
            }
         };
         str0 = switch (npc.ais.onAttack) {
            case 0 -> "gui.retaliate";
            case 1 -> "gui.panic";
            case 2 -> "gui.retreat";
            default -> "gui.nothing";
         };
         String str1 = switch (npc.ais.getMovingType()) {
            case 0 -> "ai.standing";
            case 1 -> "ai.wandering";
            default -> "ai.movingpath";
         };
         ai.setHoverTexts(Component.empty()
                         .append(Component.translatable("ai.enemyresponse").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.translatable(str0).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)),
                 Component.empty()
                         .append(Component.translatable("movement.type").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.translatable(str1).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)),
                 Component.empty()
                         .append(Component.translatable("stats.movespeed").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal("" + npc.ais.getWalkingSpeed()).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)));
         GuiMenuTopButton inv = new GuiMenuTopButton(parent, 4, "menu.inventory", ai.getX() + ai.getWidth(), guiTop - 17) {
            @Override
            public void onClick(double x, double y) {
               save();
               activeMenu = 4;
               NoppesUtil.requestOpenGUI(EnumGuiType.MainMenuInv);
            }
         };
         inv.setHoverTexts(Component.empty()
                         .append(Component.translatable("quest.exp").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal("" + npc.inventory.getExpMin()).withStyle(ChatFormatting.RESET))
                         .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal("" + npc.inventory.getExpMax()).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)),
                 Component.empty()
                         .append(Component.translatable("questlog.all.reward").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal("" + npc.inventory.drops.size()).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)));
         GuiMenuTopButton advanced = new GuiMenuTopButton(parent, 5, "menu.advanced", inv.getX() + inv.getWidth(), guiTop - 17) {
            @Override
            public void onClick(double x, double y) {
               save();
               activeMenu = 5;
               NoppesUtilServer.setEditingNpc(Minecraft.getInstance().player, npc);
               CustomNpcs.proxy.openGui(npc, EnumGuiType.MainMenuAdvanced, null);
            }
         };
         advanced.setHoverTexts(Component.empty()
                         .append(Component.translatable("role.name").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(npc.role.getEnumType().name.copy().withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)),
                 Component.empty()
                         .append(Component.translatable("job.name").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(npc.job.getEnumType().name.copy().withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)),
                 Component.empty()
                         .append(Component.translatable("menu.factions").withStyle(ChatFormatting.GRAY))
                         .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                         .append(Component.translatable(npc.getFaction().name).withStyle(ChatFormatting.RESET))
                         .append(Component.literal(";").withStyle(ChatFormatting.GRAY)));
         GuiMenuTopButton global = new GuiMenuTopButton(parent, 6, "menu.global", advanced.getX() + advanced.getWidth(), guiTop - 17) {
            @Override
            public void onClick(double x, double y) {
               save();
               activeMenu = 6;
               NoppesUtilServer.setEditingNpc(Minecraft.getInstance().player, npc);
               CustomNpcs.proxy.openGui(npc, EnumGuiType.MainMenuGlobal, null);
            }
         };
         GuiMenuTopButton close = new GuiMenuTopButton(parent, 0, "X", guiLeft + width - 22, guiTop - 17) {
            @Override
            public void onClick(double x, double y) { close(); }
         };
         GuiMenuTopButton delete = new GuiMenuTopButton(parent, 66, "gui.remove", guiLeft + width - 72, guiTop - 17) {
            @Override
            public void onClick(double x, double y) {
               ConfirmScreen guiYesNo = new ConfirmScreen(GuiNpcMenu.this::accept, Component.empty(), Component.translatable("message.delete", npc.getDisplayName().getString()));
               mc.setScreen(guiYesNo);
            }
         };
         delete.setX(close.getX() - delete.getWidth());
         topButtons = new GuiMenuTopButton[] { display, stats, ai, inv, advanced, global, close, delete };
         for (GuiMenuTopButton button : topButtons) { button.setIsFocused(button.id == activeMenu); }
      }
      else {
         GuiMenuTopButton close = new GuiMenuTopButton(parent, 0, "X", guiLeft + width - 22, guiTop - 17) {
            @Override
            public void onClick(double x, double y) { close(); }
         };
         topButtons = new GuiMenuTopButton[] { close };
      }
   }

   public void save() {
      GuiTextFieldNop.unfocus();
      parent.save();
   }

   private void close() {
      if (parent instanceof GuiContainerNPCInterface2<?> gui) { gui.backGui = null; }
      else if (parent instanceof GuiNPCInterface2 gui) { gui.backGui = null; }
      ((Screen) parent).onClose();
      if (npc != null) {
         npc.reset();
         Packets.sendServer(new SPacketMenuClose());
      }
      else { CustomNpcs.proxy.openGui(mc.player, EnumGuiType.NpcRemote); }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (mouseButton == 0 && npc != null) {
         for (GuiMenuTopButton button : topButtons) {
            if (button.mouseClicked(mouseX, mouseY, mouseButton)) {
               return true;
            }
         }
      }
      return false;
   }

   public void drawElements(GuiGraphics graphics, int x, int y, float partialTicks) {
      for (GuiMenuTopButton button : topButtons) {
         button.render(graphics, x, y, partialTicks);
      }
   }

   public void accept(boolean flag) {
      Minecraft mc = Minecraft.getInstance();
      if (flag) {
         Packets.sendServer(new SPacketNpcDelete());
         mc.setScreen(null);
         mc.mouseHandler.grabMouse();
      }
      else { NoppesUtil.openGUI(mc.player, parent); }
   }

}
