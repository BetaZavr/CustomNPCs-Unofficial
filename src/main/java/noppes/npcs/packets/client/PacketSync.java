package noppes.npcs.packets.client;

import java.util.ArrayList;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.client.gui.GuiNpcDimension;
import noppes.npcs.client.gui.global.GuiNpcManageQuest;
import noppes.npcs.client.gui.global.GuiNpcManageRecipes;
import noppes.npcs.client.gui.global.GuiPermissionsEdit;
import noppes.npcs.config.ConfigLoader;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.common.PacketBasic;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.data.RecipeCarpentry;

public class PacketSync extends PacketBasic {

   protected static int channelId;
   private final int type;
   private final CompoundTag data;
   private final boolean syncEnd;

   public PacketSync(int typeIn, CompoundTag dataIn, boolean syncEndIn) {
      type = typeIn;
      data = dataIn;
      syncEnd = syncEndIn;
   }

   public static void encode(PacketSync msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.type);
      buf.writeNbt(msg.data);
      buf.writeBoolean(msg.syncEnd);
   }

   public static PacketSync decode(FriendlyByteBuf buf) { return new PacketSync(buf.readInt(), buf.readNbt(), buf.readBoolean()); }

   @Override
   public int getChannelId() { return channelId; }

   @OnlyIn(Dist.CLIENT)
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      Minecraft mc = Minecraft.getInstance();
      PlayerData pd = PlayerData.get(player);
      switch (type) {
         case 1: {
            ListTag list = data.getList("Data", 10);
            for(int i = 0; i < list.size(); ++i) {
               Faction faction = new Faction();
               faction.load(list.getCompound(i));
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
            if (!data.isEmpty()) { pd.game.load(data); }
            break;
         } // gameData
         case 3: {
            if (!data.isEmpty()) {
               QuestCategory category;
               if (QuestController.instance.categoriesSync.containsKey(data.getInt("Slot"))) {
                  category = QuestController.instance.categoriesSync.get(data.getInt("Slot"));
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
            if (mc.screen instanceof GuiNpcManageQuest gui) { gui.init(); }
            break;
         } // quests
         case 4: {
            if (!data.isEmpty()) { pd.questData.load(data); }
            break;
         } // questData
         case 5: {
            if (!data.isEmpty()) {
               DialogCategory category;
               if (DialogController.instance.categoriesSync.containsKey(data.getInt("Slot"))) {
                  category = DialogController.instance.categoriesSync.get(data.getInt("Slot"));
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
            if (mc.screen instanceof GuiBasic gui) { gui.init(); }
            break;
         } // dialogs
         case 6: {
            if (!data.isEmpty()) { pd.overlay.load(data); }
            break;
         } // overlay data
         case 7: {
            RecipeController.getInstance().load();
            break;
         } // recipes
         case 8: {
            pd.setNBT(data);
            break;
         } // playerData
         case 9: {
            DimensionController.load(data);
            if (mc.screen instanceof GuiNpcDimension gui) { gui.setGuiData(data); }
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
            if (mc.screen instanceof GuiPermissionsEdit) { mc.screen.init(mc, mc.screen.width, mc.screen.height); }
            break;
         } // permissions
         case 15: {
            if (!data.isEmpty()) { pd.overlay.load(data); }
            break;
         } // overlayData
         case 16: break; // ItemScriptedModels
         case 17: {
            KeyController.getInstance().loadKeys(data);
            CustomNpcs.proxy.updateKeys();
            break;
         } // custom keys
         case 18: {
            CustomNpcs.proxy.syncRecipeManager();
            if (mc.screen instanceof GuiNpcManageRecipes gui) {
               gui.resetData();
               gui.init();
            }
            break;
         } // synchronized recipes
      }
      CustomNpcs.debugData.end("Packets");
   }

}
