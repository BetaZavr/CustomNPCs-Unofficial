package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.global.GuiNpcManageMarkets;
import noppes.npcs.client.gui.player.GuiNPCTrader;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogCategory;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestCategory;
import noppes.npcs.shared.common.PacketBasic;

public class PacketSyncRemove extends PacketBasic {

   protected static int channelId;
   private int id;
   private int type;

   public PacketSyncRemove() { }

   public PacketSyncRemove(int idIn, int typeIn) {
      id = idIn;
      type = typeIn;
   }

   @Override
   public void decode(FriendlyByteBuf buf) {
      id = buf.readInt();
      type = buf.readInt();
   }

   @Override
   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(id);
      buf.writeInt(type);
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      Minecraft mc = Minecraft.getMinecraft();
      switch (type) {
         case 1: {
            FactionController.instance.factions.remove(id);
            break;
         } // faction
         case 2: {
            Quest quest = QuestController.instance.quests.remove(id);
            if (quest != null) { quest.category.quests.remove(id); }
            break;
         } // quest
         case 3: {
            QuestCategory category = QuestController.instance.categories.remove(id);
            if (category != null) { QuestController.instance.quests.keySet().removeAll(category.quests.keySet()); }
            break;
         } // quest category
         case 4: {
            Dialog dialog = DialogController.instance.dialogs.remove(id);
            if (dialog != null) { dialog.category.dialogs.remove(id); }
            break;
         } // dialog
         case 5: {
            DialogCategory category = DialogController.instance.categories.remove(id);
            if (category != null) { DialogController.instance.dialogs.keySet().removeAll(category.dialogs.keySet()); }
            break;
         } // dialog category
         case 6: {
            MarcetController.getInstance().removeMarcet(id);
            if (mc.currentScreen instanceof GuiNpcManageMarkets) { ((GuiNpcManageMarkets) mc.currentScreen).setGuiData(new NBTTagCompound()); }
            else if (mc.currentScreen instanceof GuiNPCTrader) { ((GuiNPCTrader) mc.currentScreen).setGuiData(new NBTTagCompound()); }
            break;
         } // marcet deal
         case 7: {
            MarcetController.getInstance().removeDeal(id);
            if (mc.currentScreen instanceof GuiNpcManageMarkets) { ((GuiNpcManageMarkets) mc.currentScreen).setGuiData(new NBTTagCompound()); }
            else if (mc.currentScreen instanceof GuiNPCTrader) { ((GuiNPCTrader) mc.currentScreen).setGuiData(new NBTTagCompound()); }
            break;
         } // marcet deal
         case 8: {
            KeyController.getInstance().removeKeySetting(id);
            CustomNpcs.proxy.updateKeys();
            break;
         } // IKeySetting
         case 9: {
            if (id < 0) { AnimationController.getInstance().clearAnimations(); }
            else { AnimationController.getInstance().removeAnimation(id); }
            break;
         } // custom animation
         case 10: {
            if (id < 0) { AnimationController.getInstance().clearEmotions(); }
            else { AnimationController.getInstance().removeEmotion(id); }
            break;
         } // custom emotion
      }
      CustomNpcs.debugData.end("Packets");
   }

}
