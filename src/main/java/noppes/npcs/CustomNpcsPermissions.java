package noppes.npcs;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.events.PermissionGatherEvent.Nodes;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiClose;
import noppes.npcs.packets.client.PacketSync;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomPermissionHandler;
import noppes.npcs.util.NBTJsonUtil;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unchecked")
public class CustomNpcsPermissions {

   public static CustomPermissionHandler permissionHandler;
   public static final Map<PermissionNode<Boolean>, List<String>> permissions;

   public static final PermissionNode<Boolean> NPC_DELETE = new PermissionNode<>(CustomNpcs.MODID, "npc.delete", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".npc.delete", player));
   public static final PermissionNode<Boolean> NPC_CREATE = new PermissionNode<>(CustomNpcs.MODID, "npc.create", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".npc.create", player));
   public static final PermissionNode<Boolean> NPC_GUI = new PermissionNode<>(CustomNpcs.MODID, "npc.gui", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".npc.gui", player));
   public static final PermissionNode<Boolean> NPC_FREEZE = new PermissionNode<>(CustomNpcs.MODID, "npc.freeze", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".npc.freeze", player));
   public static final PermissionNode<Boolean> NPC_RESET = new PermissionNode<>(CustomNpcs.MODID, "npc.reset", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".npc.reset", player));
   public static final PermissionNode<Boolean> NPC_ADVANCED = new PermissionNode<>(CustomNpcs.MODID, "npc.advanced", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".npc.advanced", player));
   public static final PermissionNode<Boolean> NPC_DISPLAY = new PermissionNode<>(CustomNpcs.MODID, "npc.display", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".npc.display", player));
   public static final PermissionNode<Boolean> NPC_INVENTORY = new PermissionNode<>(CustomNpcs.MODID, "npc.inventory", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".npc.inventory", player));
   public static final PermissionNode<Boolean> NPC_STATS = new PermissionNode<>(CustomNpcs.MODID, "npc.stats", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".npc.stats", player));
   public static final PermissionNode<Boolean> NPC_CLONE = new PermissionNode<>(CustomNpcs.MODID, "npc.clone", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".npc.clone", player));
   public static final PermissionNode<Boolean> GLOBAL_LINKED = new PermissionNode<>(CustomNpcs.MODID, "global.linked", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.linked", player));
   public static final PermissionNode<Boolean> GLOBAL_PLAYERDATA = new PermissionNode<>(CustomNpcs.MODID, "global.playerdata", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.playerdata", player));
   public static final PermissionNode<Boolean> GLOBAL_BANK = new PermissionNode<>(CustomNpcs.MODID, "global.bank", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.bank", player));
   public static final PermissionNode<Boolean> GLOBAL_DIALOG = new PermissionNode<>(CustomNpcs.MODID, "global.dialog", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.dialog", player));
   public static final PermissionNode<Boolean> GLOBAL_QUEST = new PermissionNode<>(CustomNpcs.MODID, "global.quest", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.quest", player));
   public static final PermissionNode<Boolean> GLOBAL_FACTION = new PermissionNode<>(CustomNpcs.MODID, "global.faction", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.faction", player));
   public static final PermissionNode<Boolean> GLOBAL_TRANSPORT = new PermissionNode<>(CustomNpcs.MODID, "global.transport", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.transport", player));
   public static final PermissionNode<Boolean> GLOBAL_RECIPE = new PermissionNode<>(CustomNpcs.MODID, "global.recipe", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.recipe", player));
   public static final PermissionNode<Boolean> GLOBAL_NATURALSPAWN = new PermissionNode<>(CustomNpcs.MODID, "global.naturalspawn", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.naturalspawn", player));
   public static final PermissionNode<Boolean> SPAWNER_MOB = new PermissionNode<>(CustomNpcs.MODID, "spawner.mob", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".spawner.mob", player));
   public static final PermissionNode<Boolean> SPAWNER_CREATE = new PermissionNode<>(CustomNpcs.MODID, "spawner.create", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".spawner.create", player));
   public static final PermissionNode<Boolean> TOOL_MOUNTER = new PermissionNode<>(CustomNpcs.MODID, "tool.mounter", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".tool.mounter", player));
   public static final PermissionNode<Boolean> TOOL_PATHER = new PermissionNode<>(CustomNpcs.MODID, "tool.pather", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".tool.pather", player));
   public static final PermissionNode<Boolean> TOOL_SCRIPTER = new PermissionNode<>(CustomNpcs.MODID, "tool.scripter", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".tool.scripter", player));
   public static final PermissionNode<Boolean> TOOL_NBTBOOK = new PermissionNode<>(CustomNpcs.MODID, "tool.nbtbook", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".tool.nbtbook", player));
   public static final PermissionNode<Boolean> EDIT_VILLAGER = new PermissionNode<>(CustomNpcs.MODID, "edit.villager", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".edit.villager", player));
   public static final PermissionNode<Boolean> EDIT_BLOCKS = new PermissionNode<>(CustomNpcs.MODID, "edit.blocks", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".edit.blocks", player));
   public static final PermissionNode<Boolean> SOULSTONE_ALL = new PermissionNode<>(CustomNpcs.MODID, "soulstone.all", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".soulstone.all", player));
   public static final PermissionNode<Boolean> SCENES = new PermissionNode<>(CustomNpcs.MODID, "scenes", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".scenes", player));

   // New from Unofficial (GoodBird)
   public static final PermissionNode<Boolean> NPC_AI = new PermissionNode<>(CustomNpcs.MODID, "npc.ai", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".npc.ai", player));
   public static final PermissionNode<Boolean> ADMIN = new PermissionNode<>(CustomNpcs.MODID, "admin", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".admin", player));

   // New from Unofficial (BetaZavr)
   public static final PermissionNode<Boolean> TOOL_BUILDERS = new PermissionNode<>(CustomNpcs.MODID, "tool.builders", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".tool.builders", player));
   public static final PermissionNode<Boolean> TOOL_TELEPORTER = new PermissionNode<>(CustomNpcs.MODID, "tool.teleporter", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".tool.teleporter", player));
   public static final PermissionNode<Boolean> EDIT_PERMISSION = new PermissionNode<>(CustomNpcs.MODID, "edit.permission", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".edit.permission", player));
   public static final PermissionNode<Boolean> EDIT_CLIENT_SCRIPT = new PermissionNode<>(CustomNpcs.MODID, "edit.client.script", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".edit.client.script", player));
   public static final PermissionNode<Boolean> GLOBAL_MARKETS = new PermissionNode<>(CustomNpcs.MODID, "global.markets", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.markets", player));
   public static final PermissionNode<Boolean> GLOBAL_AUCTIONS = new PermissionNode<>(CustomNpcs.MODID, "global.auctions", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.auctions", player));
   public static final PermissionNode<Boolean> GLOBAL_MAIL = new PermissionNode<>(CustomNpcs.MODID, "global.mail", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.mail", player));
   public static final PermissionNode<Boolean> GLOBAL_ELEMENTS = new PermissionNode<>(CustomNpcs.MODID, "global.elements", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.elements", player));
   public static final PermissionNode<Boolean> GLOBAL_DUNGEONS = new PermissionNode<>(CustomNpcs.MODID, "global.dungeons", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".global.dungeons", player));
   public static final PermissionNode<Boolean> MONEY_MANAGER = new PermissionNode<>(CustomNpcs.MODID, "money.manager", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".money.manager", player));
   public static final PermissionNode<Boolean> DONAT_MANAGER = new PermissionNode<>(CustomNpcs.MODID, "donat.manager", PermissionTypes.BOOLEAN,
           (player, id, context) -> inData(CustomNpcs.MODID + ".donat.manager", player));


   @SubscribeEvent
   public void cnpcRegisterNodes(Nodes event) {
      if (!CustomNpcs.DisablePermissions) {
         CustomNpcs.debugData.start("Mod");
         LogManager.getLogger(CustomNpcsPermissions.class).info(CustomNpcs.MODNAME + " Permissions available:");
         Set<PermissionNode<Boolean>> nodes = permissions.keySet();
         for (PermissionNode<Boolean> node : nodes) {
            event.addNodes(node);
            LogManager.getLogger(CustomNpcsPermissions.class).info(node.getNodeName());
         }
         // New from Unofficial (BetaZavr)
         // load data
         File file = new File(CustomNpcs.getLevelSaveDirectory(), "permissions.json");
         if (file.exists()) {
            try {
               CompoundTag compound = NBTJsonUtil.LoadFile(file);
               for (String nodeName :  compound.getAllKeys()) {
                  boolean found = false;
                  ListTag list = compound.getList(nodeName, 8);
                  for (PermissionNode<Boolean> node : nodes) {
                     if (node.getNodeName().equals(nodeName)) {
                        permissions.get(node).clear();
                        for (int i = 0; i < list.size(); i++) { permissions.get(node).add(list.getString(i)); }
                        found = true;
                        break;
                     }
                  }
                  if (!found && nodeName.contains(".")) {
                     int j = nodeName.indexOf(".");
                     PermissionNode<Boolean> node = new PermissionNode<>(nodeName.substring(0, j), nodeName.substring(j + 1),
                             PermissionTypes.BOOLEAN, (player, id, context) -> inData(nodeName, player));
                     permissions.put(node, new ArrayList<>());
                     for (int i = 0; i < list.size(); i++) { permissions.get(node).add(list.getString(i)); }
                  }
               }
            }
            catch (Exception e) { LogWriter.error(e); }
         }
         else { save(); }
         CustomNpcs.debugData.end("Mod");
      }
   }

   @SubscribeEvent
   public void cnpcPermissionGatherEvent(PermissionGatherEvent.Handler event) {
      event.addPermissionHandler(CustomPermissionHandler.IDENTIFIER, CustomPermissionHandler::new);
   }

   public static void register(String permissionIn) {
      final String permission = permissionIn.toLowerCase();
      List<String> list = new ArrayList<>();
      list.add("All");
      list.add("Command Block");
      permissions.put(new PermissionNode<>(CustomNpcs.MODID, permission, PermissionTypes.BOOLEAN,
              (player, id, context) -> inData(CustomNpcs.MODID + "." + permission, player)), list);
      save();
   }

   public static boolean hasPermission(String permission) {
      for (PermissionNode<Boolean> node : permissions.keySet()) {
         if (node.getNodeName().equals(permission)) { return true; }
      }
      return false;
   }

   public static boolean hasPermission(ServerPlayer player, String permission) {
      for (PermissionNode<?> node : PermissionAPI.getRegisteredNodes()) {
         if (node.getNodeName().equals(permission)) {
            try {
               return hasPermission(player, (PermissionNode<Boolean>) node);
            }
            catch (Throwable ignored) { break; }
         }
      }
      return false;
   }

   public static boolean hasPermission(ServerPlayer player, PermissionNode<Boolean> permission) {
      if (permission == null) { return true; }
      if (CustomNpcs.OpsOnly && (player == null || !player.hasPermissions(4))) { return false; }
      return CustomNpcs.DisablePermissions ?
              PermissionAPI.getPermission(player, ADMIN) || PermissionAPI.getPermission(player, permission) :
              ADMIN.getDefaultResolver().resolve(player, player.getUUID()) ||
                      permission.getDefaultResolver().resolve(player, player.getUUID());
   }

   // New from Unofficial (BetaZavr)
   static {
      List<PermissionNode<Boolean>> list = new ArrayList<>();
      try {
         for (Field field : CustomNpcsPermissions.class.getDeclaredFields()) {
            if (field.getType() == PermissionNode.class) { list.add((PermissionNode<Boolean>) field.get(CustomNpcsPermissions.class)); }
         }
      }
      catch (Exception ignored) { }
      list.sort((o1, o2) -> o1.getNodeName().compareToIgnoreCase(o2.getNodeName()));
      Map<PermissionNode<Boolean>, List<String>> map = new LinkedHashMap<>();
      for (PermissionNode<Boolean> node : list) {
         map.put(node, new ArrayList<>());
         if (node != EDIT_PERMISSION &&
                 node != EDIT_CLIENT_SCRIPT &&
                 node != GLOBAL_ELEMENTS &&
                 node != DONAT_MANAGER &&
                 node != ADMIN) {
            map.get(node).add("All");
            map.get(node).add("Command Block");
         }
      }
      permissions = ImmutableMap.copyOf(map);
   }

   public static Boolean inData(String nodeName, @Nullable ServerPlayer player) {
      for (PermissionNode<Boolean> permission : new ArrayList<>(permissions.keySet())) {
         if (permission.getNodeName().equals(nodeName)) {
            if (permissions.get(permission).contains("All")) { return true; }
            if (player == null) { return permissions.get(permission).contains("Command Block"); }
            return permissions.get(permission).contains(player.getName().getString());
         }
      }
      return false;
   }

   private static void save() {
      try { NBTJsonUtil.SaveFile(new File(CustomNpcs.getLevelSaveDirectory(), "permissions.json"), getNBT()); }
      catch (Exception e) { LogWriter.error(e); }
   }

   private static CompoundTag getNBT() {
      CompoundTag compound = new CompoundTag();
      for (PermissionNode<Boolean> node : new ArrayList<>(permissions.keySet())) {
         ListTag list = new ListTag();
         for (String player : permissions.get(node)) {
            list.add(StringTag.valueOf(player));
         }
         compound.put(node.getNodeName(), list);
      }
      return compound;
   }

   @OnlyIn(Dist.CLIENT)
   public static void putToData(Map<Component, List<Component>> data) {
      data.clear();
      for (PermissionNode<Boolean> node : new ArrayList<>(permissions.keySet())) {
         List<Component> players = new ArrayList<>();
         for (String name : permissions.get(node)) { players.add(Component.literal(name)); }
         data.put(Component.translatable("permission." + node.getNodeName()), players);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static void set(CompoundTag compound) {
      ArrayList<PermissionNode<Boolean>> nodes = new ArrayList<>(permissions.keySet());
      for (String nodeName : compound.getAllKeys()) {
         for (PermissionNode<Boolean> node : nodes) {
            if (node.getNodeName().equals(nodeName)) {
               ListTag list = compound.getList(nodeName, 8);
               permissions.get(node).clear();
               for (int i = 0; i < list.size(); i++) { permissions.get(node).add(list.getString(i)); }
               break;
            }
         }
      }
   }

   public static void add(String nodeName, String playerName, ServerPlayer player) {
      if (!hasPermission(player, EDIT_PERMISSION)) {
         Packets.send(player, new PacketGuiClose());
         return;
      }
      if (playerName == null) { playerName = "Command Block"; }
      else if (playerName.isEmpty()) { playerName = "All"; }
      for (PermissionNode<Boolean> permission : new ArrayList<>(permissions.keySet())) {
         if (permission.getNodeName().equals(nodeName)) {
            if (!permissions.get(permission).contains(playerName)) {
               permissions.get(permission).add(playerName);
               save();
            }
            break;
         }
      }
      sendTo(player);
   }

   public static void remove(String nodeName, String playerName, ServerPlayer player) {
      if (!hasPermission(player, EDIT_PERMISSION)) {
         Packets.send(player, new PacketGuiClose());
         return;
      }
      if (playerName == null) { playerName = "Command Block"; }
      else if (playerName.isEmpty()) { playerName = "All"; }
      for (PermissionNode<Boolean> permission : new ArrayList<>(permissions.keySet())) {
         if (permission.getNodeName().equals(nodeName)) {
            if (permissions.get(permission).contains(playerName)) {
               permissions.get(permission).remove(playerName);
               save();
            }
            break;
         }
      }
      sendTo(player);
   }

   public static void sendTo(ServerPlayer player) {
      if (player == null) { return; }
      if (!hasPermission(player, EDIT_PERMISSION)) { Packets.send(player, new PacketGuiClose()); }
      else { Packets.send(player, new PacketSync(14, getNBT(), true)); }
   }

}
