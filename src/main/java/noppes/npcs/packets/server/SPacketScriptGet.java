package noppes.npcs.packets.server;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.*;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.blocks.tiles.TileScriptedDoor;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiData;
import noppes.npcs.packets.client.PacketScriptConsole;
import noppes.npcs.packets.client.PacketScriptText;
import noppes.npcs.util.Util;

public class SPacketScriptGet extends PacketServerBasic {

   protected static int channelId;
   private final int type;

   public SPacketScriptGet(int typeIn) { type = typeIn; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) {
      return item.getItem() == CustomItems.scripted_item || item.getItem() == CustomItems.scripter || item.getItem() == CustomItems.wand ||
              item.getItem() == CustomBlocks.scripted_door_item || item.getItem() == CustomBlocks.scripted_item;
   }

   @Override
   public boolean requiresNpc() { return type == 0; }

   public static void encode(SPacketScriptGet msg, FriendlyByteBuf buf) { buf.writeInt(msg.type); }

   public static SPacketScriptGet decode(FriendlyByteBuf buf) { return new SPacketScriptGet(buf.readInt()); }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      CompoundTag compound = new CompoundTag();
      switch (type) {
         case 0: {
            npc.script.save(compound);
            compound.put("Methods", NBTTags.nbtStringList(Arrays.stream(EnumScriptType.npcScripts).map((type) -> type.function).collect(Collectors.toList())));
            break;
         } // NPC
         case 1: {
            PlayerData data = PlayerData.get(player);
            BlockEntity tile = player.level().getBlockEntity(data.scriptBlockPos);
            if (tile instanceof TileScripted) {
               ((TileScripted)tile).save(compound);
               compound.put("Methods", NBTTags.nbtStringList(Arrays.stream(EnumScriptType.blockScripts).map((type) -> type.function).collect(Collectors.toList())));
            }
            break;
         } // Block
         case 2: {
            ItemScriptedWrapper iw = (ItemScriptedWrapper) Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(player.getMainHandItem());
            compound = iw.getMCNbt();
            compound.put("Methods", NBTTags.nbtStringList(Arrays.stream(EnumScriptType.itemScripts).map((type) -> type.function).collect(Collectors.toList())));
            break;
         } // Item
         case 3: {
            ScriptController.Instance.forgeScripts.save(compound);
            compound.put("Methods", NBTTags.nbtStringList(new ArrayList<>(ForgeEventHandler.eventNames.values())));
            break;
         } // Forge
         case 4: {
            ScriptController.Instance.playerScripts.save(compound);
            compound.put("Methods", NBTTags.nbtStringList(Arrays.stream(EnumScriptType.playerScripts).map((type) -> type.function).collect(Collectors.toList())));
            break;
         } // Player
         case 5: {
            PlayerData data = PlayerData.get(player);
            BlockEntity tile = player.level().getBlockEntity(data.scriptBlockPos);
            if (tile instanceof TileScriptedDoor) {
               ((TileScriptedDoor)tile).getNBT(compound);
               compound.put("Methods", NBTTags.nbtStringList(Arrays.stream(EnumScriptType.doorScripts).map((type) -> type.function).collect(Collectors.toList())));
            }
            break;
         } // Door
         case 6: {
            ScriptController.Instance.clientScripts.save(compound);
            compound.put("Methods", NBTTags.nbtStringList(new ArrayList<>(ForgeEventHandler.clientEventNames.values())));
            break;
         } // Client
         case 7: {
            ScriptController.Instance.potionScripts.save(compound);
            compound.put("Methods", NBTTags.nbtStringList(Arrays.stream(EnumScriptType.potionScripts).map((type) -> type.function).collect(Collectors.toList())));
            break;
         } // Potion
         case 8: {
            ScriptController.Instance.npcsScripts.save(compound);
            compound.put("Methods", NBTTags.nbtStringList(Arrays.stream(EnumScriptType.npcScripts).map((type) -> type.function).collect(Collectors.toList())));
            break;
         } // all NPC's
      }
      if (compound.contains("Methods", ListTag.TAG_LIST)) {
         compound.put("Languages", ScriptController.Instance.nbtLanguages(type == 6));
         compound.putString("DirPath", ScriptController.Instance.dir.getAbsolutePath());
         // collect and clear scripts and consoles
         Map<Integer, List<String>> mapScripts = new TreeMap<>();
         Map<Integer, Map<Long, List<String>>> mapConsoles = new TreeMap<>();
         ListTag scripts = compound.getList("Scripts", 10);
         for (int i = 0; i < scripts.size(); i++) {
            CompoundTag scriptNbt = scripts.getCompound(i);
            // Script
            if (scriptNbt.contains("Script", 8)) { mapScripts.put(i, Util.instance.getStringData(scriptNbt.getString("Script"))); }
            else {
               mapScripts.put(i, new ArrayList<>());
               ListTag list = scriptNbt.getList("Script", 8);
               for (int k = 0; k < list.size(); k++) { mapScripts.get(i).add(list.getString(k)); }
            }
            scriptNbt.put("Script", new ListTag());
            // Console
            ListTag consoles = scriptNbt.getList("Console", 10);
            for (int j = 0; j < consoles.size(); j++) {
               if (!mapConsoles.containsKey(i)) { mapConsoles.put(i, new LinkedHashMap<>()); }
               CompoundTag errorNbt = consoles.getCompound(j);
               long time = errorNbt.getLong("Long");
               if (errorNbt.contains("String", 8)) {
                  mapConsoles.get(i).put(time, Util.instance.getStringData(errorNbt.getString("String")));
               }
               else {
                  mapConsoles.get(i).put(time, new ArrayList<>());
                  ListTag list = errorNbt.getList("String", 8);
                  for (int k = 0; k < list.size(); k++) { mapConsoles.get(i).get(time).add(list.getString(k)); }
               }
               errorNbt.put("String", new ListTag());
            }
         }
         Packets.send(player, new PacketGuiData(compound));
         for (int tab : mapScripts.keySet()) {
            List<String> scriptStrings = mapScripts.get(tab);
            int i = 0;
            for (String part : scriptStrings) {
               Packets.send(player, new PacketScriptText(tab, i++, scriptStrings.size(), part, false));
            }
         }
         for (int tab : mapConsoles.keySet()) {
            for (long time : mapConsoles.get(tab).keySet()) {
               List<String> consoleStrings = mapConsoles.get(tab).get(time);
               int i = 0;
               for (String part : consoleStrings) {
                  Packets.send(player, new PacketScriptConsole(tab, time, i++, consoleStrings.size(), part, false));
               }
            }
         }
      }
      CustomNpcs.debugData.end("Packets");
   }

}
