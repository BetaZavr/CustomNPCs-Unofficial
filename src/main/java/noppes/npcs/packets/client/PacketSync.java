package noppes.npcs.packets.client;

import java.util.ArrayList;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NBTTags;
import noppes.npcs.client.gui.GuiNpcDimension;
import noppes.npcs.client.gui.global.GuiNpcManageDialogs;
import noppes.npcs.client.gui.global.GuiNpcManageQuest;
import noppes.npcs.client.gui.player.GuiLog;
import noppes.npcs.client.gui.global.GuiNpcManageRecipes;
import noppes.npcs.client.gui.global.GuiPermissionsEdit;
import noppes.npcs.config.ConfigLoader;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.items.ItemScripted;
import noppes.npcs.shared.common.PacketBasic;

public class PacketSync extends PacketBasic {

   protected static int channelId;
   private int type;
   private NBTTagCompound data;
   private boolean syncEnd;

   public PacketSync() { }

   public PacketSync(int typeIn, NBTTagCompound dataIn, boolean syncEndIn) {
      type = typeIn;
      data = dataIn;
      syncEnd = syncEndIn;
   }

   @Override
   public void decode(FriendlyByteBuf buf) {
      type = buf.readInt();
      data = buf.readNbt();
      syncEnd = buf.readBoolean();
   }

   @Override
   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(type);
      buf.writeNbt(data);
      buf.writeBoolean(syncEnd);
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      Minecraft mc = Minecraft.getMinecraft();
      switch (type) {
         case 1: {
            NBTTagList list = data.getTagList("Data", 10);
            for(int i = 0; i < list.tagCount(); ++i) {
               Faction faction = new Faction();
               faction.load(list.getCompoundTagAt(i));
               FactionController.instance.factionsSync.put(faction.id, faction);
            }
            if (syncEnd) {
               FactionController.instance.factions.clear();
               FactionController.instance.factions.putAll(FactionController.instance.factionsSync);
               FactionController.instance.factionsSync.clear();
            }
            break;
         } // factions
         case 2: {
            if (!data.getKeySet().isEmpty()) { CustomNpcs.proxy.getPlayerData(player).game.load(data); }
            break;
         } // gameData
         case 3: {
            if (!data.getKeySet().isEmpty()) {
               QuestCategory category;
               if (QuestController.instance.categoriesSync.containsKey(data.getInteger("Slot"))) {
                  category = QuestController.instance.categoriesSync.get(data.getInteger("Slot"));
               }
               else { category = new QuestCategory(); }
               category.load(data);
               QuestController.instance.categoriesSync.put(category.id, category);
            }
            if (syncEnd) {
               TreeMap<Integer, Quest> map = new TreeMap<>();
               for (QuestCategory category : QuestController.instance.categoriesSync.values()) {
                  for (Quest quest : category.quests.values()) { map.put(quest.id, quest); }
               }
               QuestController.instance.categories.clear();
               QuestController.instance.categories.putAll(QuestController.instance.categoriesSync);
               QuestController.instance.quests.clear();
               QuestController.instance.quests.putAll(map);
               QuestController.instance.categoriesSync.clear();
            }
            if (mc.currentScreen instanceof GuiNpcManageQuest) { mc.currentScreen.initGui(); }
            break;
         } // quests
         case 4: {
            if (!data.getKeySet().isEmpty()) { CustomNpcs.proxy.getPlayerData(player).questData.load(data); }
            if (mc.currentScreen instanceof GuiLog) { mc.currentScreen.initGui(); }
            break;
         } // questData
         case 5: {
            if (!data.getKeySet().isEmpty()) {
               DialogCategory category;
               if (DialogController.instance.categoriesSync.containsKey(data.getInteger("Slot"))) {
                  category = DialogController.instance.categoriesSync.get(data.getInteger("Slot"));
               }
               else { category = new DialogCategory(); }
               category.load(data);
               DialogController.instance.categoriesSync.put(category.id, category);
            }
            if (syncEnd) {
               TreeMap<Integer, Dialog> map = new TreeMap<>();
               for (DialogCategory category : DialogController.instance.categoriesSync.values()) {
                  for (Dialog dialog : category.dialogs.values()) { map.put(dialog.id, dialog); }
               }
               DialogController.instance.categories.clear();
               DialogController.instance.categories.putAll(DialogController.instance.categoriesSync);
               DialogController.instance.dialogs.clear();
               DialogController.instance.dialogs.putAll(map);
               DialogController.instance.categoriesSync.clear();
            }
            if (mc.currentScreen instanceof GuiNpcManageDialogs) { mc.currentScreen.initGui(); }
            break;
         } // dialogs
         case 6: {
            if (!data.getKeySet().isEmpty()) { CustomNpcs.proxy.getPlayerData(player).overlay.load(data); }
            break;
         } // overlay data
         case 7: {
            //RecipeController.instance.load();
            break;
         } // recipes
         case 8: {
            CustomNpcs.proxy.getPlayerData(player).setNBT(data);
            break;
         } // playerData
         case 9: {
            DimensionController.load(data);
            if (mc.currentScreen instanceof GuiNpcDimension) { ((GuiNpcDimension) mc.currentScreen).setGuiData(data); }
            break;
         } // dimensions
         case 10: {
            DialogController.instance.getGuiSettings().load(data);
            break;
         } // dialog gui settings
         case 11: {
            RecipeController rData = RecipeController.getInstance();
            if (syncEnd) { rData.reloadGlobalRecipes(); }
            else {
               RecipeCarpentry recipe = RecipeCarpentry.create(data);
               if (!rData.syncRecipes.containsKey(recipe.getGroup())) { rData.syncRecipes.put(recipe.getGroup(), new ArrayList<>()); }
               rData.syncRecipes.get(recipe.getGroup()).add(recipe);
            }
            break;
         } // global recipes
         case 12: {
            RecipeController rData = RecipeController.getInstance();
            if (syncEnd) { rData.reloadAnvilRecipes(); }
            else {
               RecipeCarpentry recipe = RecipeCarpentry.create(data);
               if (!rData.syncRecipes.containsKey(recipe.getGroup())) { rData.syncRecipes.put(recipe.getGroup(), new ArrayList<>()); }
               rData.syncRecipes.get(recipe.getGroup()).add(recipe);
            }
            break;
         } // mod recipes
         case 13: {
            ConfigLoader.load(data);
            break;
         } // mod data
         case 14: {
            CustomNpcsPermissions.set(data);
            if (mc.currentScreen instanceof GuiPermissionsEdit) { mc.currentScreen.setWorldAndResolution(mc, mc.currentScreen.width, mc.currentScreen.height); }
            break;
         } // permissions
         case 15: {
            if (!data.getKeySet().isEmpty()) { CustomNpcs.proxy.getPlayerData(player).overlay.load(data); }
            break;
         } // overlayData
         case 16: {
            if (player.getServer() == null) {
               ItemScripted.Resources = NBTTags.getIntegerStringMap(data.getTagList("List", 10));
            }
            CustomNpcs.proxy.reloadItemTextures();
            break;
         } // ItemScriptedModels
         case 17: {
            KeyController.getInstance().loadKeys(data);
            CustomNpcs.proxy.updateKeys();
            break;
         } // custom keys
         case 18: {
            CustomNpcs.proxy.syncRecipeManager();
            if (mc.currentScreen instanceof GuiNpcManageRecipes) {
               ((GuiNpcManageRecipes) mc.currentScreen).resetData();
               mc.currentScreen.initGui();
            }
            break;
         } // synchronized recipes
      }
      CustomNpcs.debugData.end("Packets");
   }

}
