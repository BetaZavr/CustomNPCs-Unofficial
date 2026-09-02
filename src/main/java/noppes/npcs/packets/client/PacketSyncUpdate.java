package noppes.npcs.packets.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.shared.common.PacketBasic;
import noppes.npcs.util.BuilderData;

import java.util.ArrayList;

public class PacketSyncUpdate extends PacketBasic {

   protected static int channelId;
   private final int id;
   private final int type;
   private final CompoundTag data;

   public PacketSyncUpdate(int idIn, int typeIn, CompoundTag dataIn) {
      id = idIn;
      type = typeIn;
      data = dataIn;
   }

   public static void encode(PacketSyncUpdate msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.id);
      buf.writeInt(msg.type);
      buf.writeNbt(msg.data);
   }

   public static PacketSyncUpdate decode(FriendlyByteBuf buf) { return new PacketSyncUpdate(buf.readInt(), buf.readInt(), buf.readAnySizeNbt()); }

   @Override
   public int getChannelId() { return channelId; }

   @OnlyIn(Dist.CLIENT)
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      switch (type) {
         case 1: {
            Faction faction = new Faction();
            faction.load(data);
            FactionController.instance.factions.put(faction.id, faction);
            break;
         } // faction
         case 2: {
            QuestController qData = QuestController.instance;
            Quest quest = qData.get(data.getInt("Id"));
            if (quest != null) { quest.load(data); }
            else {
               QuestCategory category = QuestController.instance.categories.get(id);
               quest = new Quest(category);
               quest.load(data);
               QuestController.instance.quests.put(quest.id, quest);
               category.quests.put(quest.id, quest);
            }
            PlayerQuestData questData = PlayerData.get(player).questData;
            for (QuestData qd : new ArrayList<>(questData.activeQuests.values())) {
               if (qd.quest.id == quest.id) {
                  qd.reset(quest);
                  break;
               }
            }
            break;
         } // quest
         case 3: {
            QuestCategory category = new QuestCategory();
            category.load(data);
            QuestController.instance.categories.put(category.id, category);
            break;
         } // quest category
         case 4: {
            DialogController dData = DialogController.instance;
            Dialog dialog = dData.get(data.getInt("DialogId"));
            if (dialog != null) { dialog.load(data); }
            else {
               DialogCategory category = DialogController.instance.categories.get(id);
               dialog = new Dialog(category);
               dialog.load(data);
               DialogController.instance.dialogs.put(dialog.id, dialog);
               category.dialogs.put(dialog.id, dialog);
            }
            break;
         } // dialog
         case 5: {
            DialogCategory category = new DialogCategory();
            category.load(data);
            DialogController.instance.categories.put(category.id, category);
            break;
         } // dialog category
         case 6: {
            updateMinimap();
            break;
         } // minimap
         case 7: {
            BuilderData builder;
            if (SyncController.dataBuilder.containsKey(id)) { builder = SyncController.dataBuilder.get(id); }
            else { builder = new BuilderData(id, data.getInt("BuilderType")); }
            builder.read(data);
            break;
         } // builder data
         case 8: {
            KeyController.getInstance().loadKey(data);
            CustomNpcs.proxy.updateKeys();
            break;
         } // change or add IKeySetting
         case 9: {
            if (!data.isEmpty()) { AnimationController.getInstance().loadAnimation(data); }
            break;
         } // custom animation set
         case 10: {
            if (!data.isEmpty()) { AnimationController.getInstance().loadEmotion(data); }
            break;
         } // custom emotion set
         case 11: {
            DialogController.instance.getGuiSettings().load(data);
            break;
         } // dialog gui settings
         case 12: {
            if (data.contains("MailData", 9)) { PlayerData.get(player).mailData.load(data); }
            if (data.contains("LettersBeDeleted", 3)) { CustomNpcs.MailTimeWhenLettersWillBeDeleted = data.getInt("LettersBeDeleted"); }
            if (data.contains("LettersBeReceived", 11)) {
               int[] vs = data.getIntArray("LettersBeReceived");
               System.arraycopy(vs, 0, CustomNpcs.MailTimeWhenLettersWillBeReceived, 0, vs.length);
            }
            if (data.contains("CostSendingLetter", 11)) {
               int[] vs = data.getIntArray("CostSendingLetter");
               System.arraycopy(vs, 0, CustomNpcs.MailCostSendingLetter, 0, vs.length);
            }
            if (data.contains("SendToYourself", 1)) { CustomNpcs.MailSendToYourself = data.getBoolean("SendToYourself"); }
            break;
         } // new mail info on screen
         case 13: {
            PlayerData.get(player).factionData.load(data);
            break;
         } // faction data update
         case 14: {
            if (id < 0) { TransportController.getInstance().clear(); }
            else { TransportController.getInstance().loadCategory(data); }
            break;
         } // transport update
         case 15: {
            AnimationController.getInstance().loadEmotion(data);
            break;
         } // set custom emotion
         case 16: {
            CustomNpcs.TypeShowQuestCompass = id;
            break;
         } // TypeShowQuestCompass
      }
      CustomNpcs.debugData.end("Packets");
   }

   private void updateMinimap() {
      PlayerMiniMapData mm = PlayerData.get(player).minimap;
      //String modName = mm.modName;
      mm.load(data);
      /*int isChanged = 0;
      if (modName.endsWith("journeymap")) {
         try {
            Class<?> ws = Class.forName("journeymap.client.waypoint.WaypointStore");
            Class<?> wp = Class.forName("journeymap.client.model.Waypoint");
            Constructor<?> wc = null;
            for (Constructor<?> c : wp.getDeclaredConstructors()) {
               if (c.getParameterCount() == 12) {
                  Parameter[] ps = c.getParameters();
                  if (ps[0].getType() == String.class && ps[1].getType() == int.class
                          && ps[2].getType() == int.class && ps[3].getType() == int.class
                          && ps[4].getType() == boolean.class && ps[5].getType() == int.class
                          && ps[6].getType() == int.class && ps[7].getType() == int.class
                          && ps[8].getType().getSimpleName().equals("Type") && ps[9].getType() == String.class
                          && ps[10].getType() == Integer.class && ps[11].getType() == Collection.class) {
                     wc = c;
                     break;
                  }
               }
            }
            Field cacheField = ws.getDeclaredField("cache");
            Field groupCacheField = ws.getDeclaredField("groupCache");
            Field dimensionsField = ws.getDeclaredField("dimensions");
            Method load = null, remove = null;
            for (Method m : ws.getDeclaredMethods()) {
               if (m.getName().equals("load") && m.getParameterCount() == 2
                       && m.getParameters()[0].getType() == Collection.class
                       && m.getParameters()[1].getType() == boolean.class) {
                  load = m;
               }
               if (m.getName().equals("remove") && m.getParameterCount() == 1
                       && m.getParameters()[0].getType().getSimpleName().equals("Waypoint")) {
                  remove = m;
               }
            }

            if (!cacheField.isAccessible()) {
               cacheField.setAccessible(true);
            }
            if (!groupCacheField.isAccessible()) {
               groupCacheField.setAccessible(true);
            }
            if (!dimensionsField.isAccessible()) {
               dimensionsField.setAccessible(true);
            }
            Object waypointStore = ws.getEnumConstants()[0];

            // Clear OLD
            Set<Integer> dimensions = (Set<Integer>) dimensionsField.get(waypointStore);
            dimensions.clear();

            Cache<Long, Object> groupCache = (Cache<Long, Object>) groupCacheField.get(waypointStore);
            groupCache.invalidateAll();

            Cache<String, Object> cache = (Cache<String, Object>) cacheField.get(waypointStore);
            Map<String, Object> map = cache.asMap();
            for (String name : map.keySet()) {
               assert remove != null;
               remove.invoke(waypointStore, map.get(name));
            }
            cache.invalidateAll();

            // Create and Add new
            isChanged = 1;
            List<Object> waypoints = new ArrayList<>();
            for (MiniMapData mmd : mm.points) {
               ResourceKey<Level> dimID = mmd.dimIDs.get(0);
               Object t = null;
               for (Object enumType : wp.getClasses()[0].getEnumConstants()) {
                  if (t == null) {
                     t = enumType;
                  }
                  if (enumType.toString().equalsIgnoreCase(mmd.type)) {
                     t = enumType;
                     break;
                  }
               }
               if (t == null) {
                  continue;
               }
               int x = (int) mmd.pos.getX();
               int y = (int) mmd.pos.getY();
               int z = (int) mmd.pos.getZ();
               Color color = new Color(mmd.color);
               List<Integer> dim = new ArrayList<>();
               for (int dId : mmd.dimIDs) {
                  dim.add(dId);
               }
               assert wc != null;
               Object waypoint = wc.newInstance(mmd.name, x, y, z, mmd.isEnable, color.getRed(), color.getGreen(), color.getBlue(), t, "journeymap", dimID, dim);
               wp.getDeclaredMethod("setIcon", String.class).invoke(waypoint, mmd.icon);
               waypoints.add(waypoint);
            }
            assert load != null;
            load.invoke(waypointStore, waypoints, true);
         } catch (Exception e) {
            isChanged = 2;
         }
      } else if (modName.endsWith("xaerominimap")) {
         try {
            Class<?> xms = Class.forName("xaero.common.XaeroMinimapSession");
            Object minimapSession = xms.getDeclaredMethod("getCurrentSession").invoke(xms); // XaeroMinimapSession
            Object waypointsManager = xms.getDeclaredMethod("getWaypointsManager").invoke(minimapSession); // WaypointsManager

            Method getWaypointMap = waypointsManager.getClass().getDeclaredMethod("getWaypointMap");
            HashMap<String, Object> waypointMap = (HashMap<String, Object>) getWaypointMap
                    .invoke(waypointsManager);

            String mainContainerID = (String) waypointsManager.getClass()
                    .getDeclaredMethod("getAutoRootContainerID").invoke(waypointsManager);
            Object wwrc = waypointMap.get(mainContainerID);// WaypointWorldRootContainer
            Field fwwrc = wwrc.getClass().getDeclaredField("dimensionTypes");
            fwwrc.setAccessible(true);

            Int2ObjectMap<Object> dimensionTypes = (Int2ObjectMap<Object>) fwwrc.get(wwrc);
            boolean saveConfig = false;
            for (int dim : DimensionManager.getStaticDimensionIDs()) {
               if (!dimensionTypes.containsKey(dim)) {
                  World ret = DimensionManager.getWorld(dim, true);
                  if (ret == null) {
                     DimensionManager.initDimension(dim);
                     ret = DimensionManager.getWorld(dim);
                  }
                  dimensionTypes.put(dim, wwrc.getClass().getDeclaredMethod("createDimensionType", Level.class).invoke(wwrc, ret)); // WaypointDimensionTypeInfo
                  saveConfig = true;
               }
            }
            if (saveConfig) {
               wwrc.getClass().getDeclaredMethod("saveConfig").invoke(wwrc);
            }
            Class<?> xm = Class.forName("xaero.minimap.XaeroMinimap");
            Object instance = xm.getField("instance").get(xm);
            File parentFile = (File) xm.getDeclaredMethod("getWaypointsFolder").invoke(instance);

            String world_name = (String) mm.addData.get("xaero_world_name");
            if (world_name == null || world_name.isEmpty()) {
               HashMap<String, Object> dimMap = (HashMap<String, Object>) wwrc.getClass()
                       .getField("subContainers").get(wwrc);
               for (String k : dimMap.keySet()) {
                  world_name = (String) dimMap.get(k).getClass().getDeclaredMethod("getKey")
                          .invoke(dimMap.get(k));
               }
            }
            assert world_name != null;
            File worldDir = new File(parentFile, world_name);
            Gson gson = new Gson();

            Map<File, TempWaypointText> map = new HashMap<>();
            for (MiniMapData mmd : mm.points) {
               if (mmd.gsonData.containsKey("temporary") && gson.fromJson(mmd.gsonData.get("temporary"), boolean.class)) { continue; }
               int dimID = mmd.dimIDs[0];
               File dimDir = new File(worldDir, "dim%" + dimID);
               if (dimDir.exists() || dimDir.mkdirs()) {
                  File dimFile = new File(dimDir, "/waypoints.txt");
                  StringBuilder text = new StringBuilder();
                  StringBuilder endText = new StringBuilder();
                  if (!map.containsKey(dimFile)) {
                     if (!dimFile.exists()) {
                        text.append("#" + ((char) 10));
                        text.append("#waypoint:name:initials:x:y:z:color:disabled:type:set:rotate_on_tp:tp_yaw:visibility_type:destination" + ((char) 10));
                        text.append("#" + ((char) 10));
                     } else {
                        FileInputStream inputStream = new FileInputStream(dimFile);
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8))) {
                           boolean end = false;
                           for (String line = br.readLine(); line != null; line = br.readLine()) {
                              if (!end && line.indexOf("#") != 0) {
                                 end = true;
                                 continue;
                              }
                              if (line.indexOf("waypoint:") == 0) {
                                 continue;
                              }
                              if (end) {
                                 endText.append(line).append((char) 10);
                              } else {
                                 text.append(line).append((char) 10);
                              }
                           }
                        }
                     }
                     map.put(dimFile, new TempWaypointText(text.toString(), endText.toString()));
                  }
               }
            }
            for (File dimFile : map.keySet()) {
               Util.instance.saveFile(dimFile, map.get(dimFile).getString());
            }

            Object settings = xm.getDeclaredMethod("getSettings").invoke(instance); // ModSettings
            Method loadWaypointsFromAllSources = null;
            for (Method m : settings.getClass().getDeclaredMethods()) {
               if (m.getName().equals("loadWaypointsFromAllSources") && m.getParameterCount() == 1) {
                  loadWaypointsFromAllSources = m;
                  break;
               }
            }
            assert loadWaypointsFromAllSources != null;
            loadWaypointsFromAllSources.invoke(settings, waypointsManager);
         } catch (Exception e) {
            LogWriter.error("Error:", e);
            isChanged = 2;
         }
      } else if (modName.endsWith("voxelmap")) {
         try {
            Class<?> vm = Class.forName("com.mamiyaotaru.voxelmap.VoxelMap");
            Object instance = vm.getMethod("getInstance").invoke(vm);
            Object waypointManager = vm.getMethod("getWaypointManager").invoke(instance);
            List<Object> waypoints = (List<Object>) waypointManager.getClass().getMethod("getWaypoints").invoke(waypointManager);
            // Clear OLD
            waypoints.clear();

            // Create and Add new
            isChanged = 1;
            Class<?> wc = Class.forName("com.mamiyaotaru.voxelmap.util.Waypoint");
            Constructor<?> cw = wc.getConstructor(String.class, int.class, int.class, int.class, boolean.class,
                    float.class, float.class, float.class, String.class, String.class, TreeSet.class);
            for (MiniMapData mmd : mm.points) {
               int dimID = mmd.dimIDs[0];
               int x = (int) mmd.pos.getX();
               int y = (int) mmd.pos.getY();
               int z = (int) mmd.pos.getZ();
               Color color = new Color(mmd.color);
               TreeSet<Integer> dim = new TreeSet<>();
               for (int dId : mmd.dimIDs) {
                  dim.add(dId);
               }
               String worldName = mmd.gsonData.getOrDefault("voxel_world_name", "");
               if (worldName.isEmpty()) {
                  World ret = DimensionManager.getWorld(dimID, true);
                  if (ret == null) {
                     DimensionManager.initDimension(dimID);
                     ret = DimensionManager.getWorld(dimID);
                  }
                  worldName = ret.getProviderName();
               }
               Object waypoint = cw.newInstance(mmd.name, x, z, y, mmd.isEnable(), color.getRed() / 255.0f,
                       color.getGreen() / 255.0f, color.getBlue() / 255.0f, mmd.icon, worldName, dim);
               waypoints.add(waypoint);
            }
            waypointManager.getClass().getMethod("saveWaypoints").invoke(waypointManager);
            Method loadWaypoints = waypointManager.getClass().getDeclaredMethod("loadWaypoints");
            loadWaypoints.setAccessible(true);
            loadWaypoints.invoke(waypointManager);
            // remove any dimension points;
            Method med = waypointManager.getClass().getDeclaredMethod("enteredDimension", int.class);
            med.setAccessible(true);
            med.invoke(waypointManager, player.world.provider.getDimension());
         } catch (Exception e) {
            LogWriter.error("Error:", e);
            isChanged = 2;
         }
      }
      if (isChanged != 0) {
         player.sendMessage(new TextComponentTranslation("minimap.set.points." + isChanged,
                 ((char) 167) + "7" + modName));
      }*/
   }

}
