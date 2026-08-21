package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.player.GuiDialogInteract;
import noppes.npcs.client.gui.player.moderngui.GuiDialogModern;
import noppes.npcs.client.gui.player.moderngui.GuiQuestModern;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.PacketBasic;

public class PacketDialog extends PacketBasic {

   protected static int channelId;
   private final int entityId;
   private final int dialogId;

   public PacketDialog(int entityIdIn, int dialogIdIn) {
      entityId = entityIdIn;
      dialogId = dialogIdIn;
   }

   public static void encode(PacketDialog msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.entityId);
      buf.writeInt(msg.dialogId);
   }

   public static PacketDialog decode(FriendlyByteBuf buf) { return new PacketDialog(buf.readInt(), buf.readInt()); }

   @Override
   public int getChannelId() { return channelId; }

   @OnlyIn(Dist.CLIENT)
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) { return; }
      Entity entity = level.getEntity(entityId);
      if (entity instanceof EntityNPCInterface npc) {
         Dialog dialog = DialogController.instance.dialogs.get(dialogId);
         openDialog(dialog, npc, player);
      }
      CustomNpcs.debugData.end("Packets");
   }

   public static void openDialog(Dialog dialog, EntityNPCInterface npc, Player player) {
      Screen gui = Minecraft.getInstance().screen;
      if (gui instanceof GuiDialogInteract dia) { dia.appendDialog(dialog); }
      else if (CustomNpcs.EnableNewDialogSystem) {
         if (!(gui instanceof GuiQuestModern) && dialog.hasQuest()) { CustomNpcs.proxy.openGui(player, new GuiQuestModern(npc, dialog.getQuest(), dialog, -2)); }
         else { CustomNpcs.proxy.openGui(player, new GuiDialogModern(npc, dialog)); }
      }
      else { CustomNpcs.proxy.openGui(player, new GuiDialogInteract(npc, dialog)); }
   }

}
