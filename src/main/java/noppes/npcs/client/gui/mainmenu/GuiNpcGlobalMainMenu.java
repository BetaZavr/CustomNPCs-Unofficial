package noppes.npcs.client.gui.mainmenu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.global.*;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.yellow_de.data.UtilYDE;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketPermissionGlobalGet;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

import java.util.Arrays;

public class GuiNpcGlobalMainMenu extends GuiNPCInterface2 {

   // New from Unofficial (BetaZavr)
   protected boolean[] permissions = new boolean[16];
   protected boolean isAdmin = false;
   protected boolean addedPermissions = false;

   public GuiNpcGlobalMainMenu(EntityNPCInterface npc) {
      super(npc, 6);
      Arrays.fill(permissions, true);

      Packets.sendServer(new SPacketPermissionGlobalGet());
   }

   @Override
   public void init() {
      super.init();
      if (!addedPermissions) { return; }
      int r0 = guiLeft + 75;
      int r1 = guiLeft + 240;
      int y = guiTop + 10;
      String notEdit = "hover.not.edit";
      boolean enabled = permissions[0] || isAdmin;
      boolean isWip = player.getName().getString().startsWith("BetaZavr");
      addButton(2, r0, y, "global.banks")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.banks", !enabled ? notEdit : null);
      enabled = permissions[1] || isAdmin;
      addButton(3, r0, (y += 22), "menu.factions")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.factions", !enabled ? notEdit : null);
      enabled = permissions[2] || isAdmin;
      addButton(4, r0, (y += 22), "dialog.dialogs")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.dialogs", !enabled ? notEdit : null);
      addButton(5, r0 + 120, y, "GUI")
              .setSize(20, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.dialogs.gui", !enabled ? notEdit : null);
      enabled = (permissions[2] && permissions[3]) || isAdmin;
      addButton(20, r0 + 120, y + 22, "global.game.edit")
              .setSize(44, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.game.edit", !enabled ? notEdit : null, "gui.wip")
              .layerColor = 0xFFF00000;
      enabled = permissions[3] || isAdmin;
      addButton(11, r0, (y += 22), "quest.quests")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.quests", !enabled ? notEdit : null);
      enabled = permissions[4] || isAdmin;
      addButton(12, r0, (y += 22), "global.transport")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.transports", !enabled ? notEdit : null);
      enabled = permissions[5] || isAdmin;
      addButton(13, r0, (y += 22), "global.playerdata")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.playerdatas", !enabled ? notEdit : null);
      enabled = permissions[6] || isAdmin;
      addButton(14, r0, (y += 22), "global.recipes")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.recipes", !enabled ? notEdit : null);
      enabled = permissions[7] || isAdmin;
      addButton(15, r0, (y += 22), Component.translatable("global.naturalspawn")
              .append(" ")
              .append(Component.translatable("gui.deprecated")))
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.naturalspawns", !enabled ? notEdit : null);
      enabled = permissions[8] || isAdmin;
      addButton(16, r0, y + 22, "global.linked")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.linkeds", !enabled ? notEdit : null);
      // New from Unofficial (BetaZavr)
      y = guiTop + 10;
      enabled = permissions[9] || isAdmin;
      addButton(17, r1, y, "global.market")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.markets", !enabled ? notEdit : null);
      enabled = permissions[10] || isAdmin && isWip;
      addButton(18, r1, (y += 22), "global.auctions")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.auctions", !enabled ? notEdit : null);
      enabled = permissions[11] || isAdmin;
      addButton(19, r1, y += 22, "global.mail")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.mail", !enabled ? notEdit : null);
      enabled = permissions[12] || isAdmin && isWip;
      addButton(21, r1, y += 22, "global.elements")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.elements", !enabled ? notEdit : null, "gui.wip")
              .layerColor = 0xFF808080;
      enabled = permissions[13] || isAdmin && isWip;
      addButton(22, r1, y += 22, "global.dungeons")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.dungeons", !enabled ? notEdit : null, "gui.wip")
              .layerColor = 0xFFF00000;
      enabled = permissions[15] || isAdmin;
      addButton(23, r1, y += 22, "global.drop.templates")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.drop.templates", !enabled ? notEdit : null);
      y += 44;
      enabled = permissions[14] || isAdmin;
      addButton(26, r1, y + 22, "global.permissions")
              .setSize(110, 20)
              .setIsEnabled(enabled)
              .setHoverTexts("global.hover.permissions", !enabled ? notEdit : null);
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 2: NoppesUtil.requestOpenGUI(EnumGuiType.ManageBanks); break;
         case 3: NoppesUtil.requestOpenGUI(EnumGuiType.ManageFactions); break;
         case 4: NoppesUtil.requestOpenGUI(EnumGuiType.ManageDialogs); break;
         case 5: NoppesUtil.openGUI(player, new GuiNpcDialogGuiSettings(npc)); break;
         case 11: NoppesUtil.requestOpenGUI(EnumGuiType.ManageQuests); break;
         case 12: NoppesUtil.requestOpenGUI(EnumGuiType.ManageTransport, new BlockPos(-1, -1, 0)); break;
         case 13: NoppesUtil.openGUI(player, new GuiNpcManagePlayerData(npc)); break;
         case 14: NoppesUtil.requestOpenGUI(EnumGuiType.ManageRecipes, BlockPos.ZERO); break;
         case 15: NoppesUtil.openGUI(player, new GuiNpcNaturalSpawns(npc)); break;
         case 16: NoppesUtil.requestOpenGUI(EnumGuiType.ManageLinked); break;
         case 17: NoppesUtil.openGUI(player, new GuiNpcManageMarkets(npc)); break;
         case 18: NoppesUtil.openGUI(player, new GuiNpcManageAuctions(npc)); break;
         case 19: NoppesUtil.requestOpenGUI(EnumGuiType.ManageMail); break;
         case 20: NoppesUtil.requestOpenGUI(EnumGuiType.ManageGame); break;
         case 21: NoppesUtil.requestOpenGUI(EnumGuiType.ManageCustomElements); break;
         case 22: NoppesUtil.requestOpenGUI(EnumGuiType.ManageDungeons); break;
         case 23: NoppesUtil.requestOpenGUI(EnumGuiType.ManageDropTemplates); break;

         case 26: NoppesUtil.requestOpenGUI(EnumGuiType.PermissionsEdit); break;
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      GuiButtonNop buttonG = getButton(20); // game.edit
      if (buttonG != null && buttonG.isEnabled()) {
         // dialogs
         GuiButtonNop buttonD = getButton(4);
         PoseStack matrixStack = graphics.pose();
         matrixStack.pushPose();
         float[] p1 = new float[] { buttonG.getX(), buttonG.getY() + buttonG.getHeight() / 2.0f };
         if (buttonD != null) {
            boolean hovered = buttonG.isHoveredOrFocused() || buttonD.isHoveredOrFocused();
            float[] p0 = new float[] { buttonD.getX() + buttonD.getWidth(), buttonD.getY() + buttonD.getHeight() / 2.0f };
            UtilYDE.renderDot(graphics, p0, 0.5f, hovered, 0x184EB0);
            UtilYDE.renderDot(graphics, p1, 0.5f, hovered, 0x184EB0);
            UtilYDE.renderSpline(graphics, p0, p1, hovered, false, 0x184EB0, 0.0f);
         }
         // quests
         GuiButtonNop buttonQ = getButton(11);
         if (buttonQ != null) {
            boolean hovered = buttonG.isHoveredOrFocused() || buttonQ.isHoveredOrFocused();
            float[] p0 = new float[] { buttonQ.getX() + buttonQ.getWidth(), buttonQ.getY() + buttonQ.getHeight() / 2.0f };
            UtilYDE.renderDot(graphics, p0, 0.5f, hovered, 0xAEB018);
            UtilYDE.renderDot(graphics, p1, 0.5f, hovered, 0xAEB018);
            UtilYDE.renderSpline(graphics, p0, p1, hovered, false, 0xAEB018, 0.0f);
         }
         matrixStack.popPose();
      }
   }

   @Override
   public void save() {}

   public void setMenuData(boolean banks, boolean factions, boolean dialogs, boolean quests, boolean transports,
                           boolean playersData, boolean recipes, boolean naturalSpawns, boolean linkeds, boolean markets,
                           boolean auctions, boolean mails, boolean elements, boolean dungeons, boolean permission, boolean dropTemplates) {
      permissions[0] = banks;
      permissions[1] = factions;
      permissions[2] = dialogs;
      permissions[3] = quests;
      permissions[4] = transports;
      permissions[5] = playersData;
      permissions[6] = recipes;
      permissions[7] = naturalSpawns;
      permissions[8] = linkeds;
      permissions[9] = markets;
      permissions[10] = auctions;
      permissions[11] = mails;
      permissions[12] = elements;
      permissions[13] = dungeons;
      permissions[14] = permission;
      permissions[15] = dropTemplates;
      addedPermissions = true;
      init();
   }

}
