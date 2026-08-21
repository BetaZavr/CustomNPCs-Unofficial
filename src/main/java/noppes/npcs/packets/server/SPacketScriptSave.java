package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.*;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.blocks.tiles.TileScriptedDoor;
import noppes.npcs.controllers.scripts.IScriptHandler;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SPacketScriptSave extends PacketServerBasic {

   protected static int channelId;
   private final int type;
   private final CompoundTag data;

   public SPacketScriptSave(int typeIn, CompoundTag dataIn) {
      type = typeIn;
      data = dataIn;
   }

   @Override
   public boolean toolAllowed(ItemStack item) {
      return item.getItem() == CustomItems.scripter || item.getItem() == CustomBlocks.scripted_door_item ||
              item.getItem() == CustomItems.wand || item.getItem() == CustomItems.scripted_item || item.getItem() == CustomBlocks.scripted_item;
   }

   @Override
   public boolean requiresNpc() { return type == 0; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.TOOL_SCRIPTER); }

   public static void encode(SPacketScriptSave msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.type);
      buf.writeNbt(msg.data);
   }

   public static SPacketScriptSave decode(FriendlyByteBuf buf) { return new SPacketScriptSave(buf.readInt(), buf.readNbt()); }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   public void handle() {
      CustomNpcs.debugData.start("Packets");
      IScriptHandler handler = null;
      switch (type) {
         case 0: {
            handler = npc.script;
            npc.script.load(data);
            npc.updateAI = true;
            break;
         } // NPC
         case 1: {
            PlayerData pd = PlayerData.get(player);
            BlockEntity tile = player.level().getBlockEntity(pd.scriptBlockPos);
            if (tile instanceof TileScripted script) {
               handler = script;
               script.setNBT(data);
               script.setChanged();
            }
            break;
         } // Block
         case 2: {
            if (player.isCreative()) {
               ItemScriptedWrapper wrapper = (ItemScriptedWrapper) Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(player.getMainHandItem());
               handler = wrapper;
               wrapper.setMCNbt(data);
               wrapper.saveScriptData();
               wrapper.updateClient = true;
               player.containerMenu.sendAllDataToRemote();
            }
            break;
         } // Item
         case 3: {
            ScriptController.Instance.setForgeScripts(data);
            handler = ScriptController.Instance.forgeScripts;
            break;
         } // Forge
         case 4: {
            ScriptController.Instance.setPlayerScripts(data);
            handler = ScriptController.Instance.playerScripts;
            break;
         } // Player
         case 5: {
            PlayerData pd = PlayerData.get(player);
            BlockEntity tile = player.level().getBlockEntity(pd.scriptBlockPos);
            if (tile instanceof TileScriptedDoor script) {
               handler = script;
               script.setNBT(data);
            }
            break;
         } // Door
         case 6: {
            if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.EDIT_CLIENT_SCRIPT)) {
               ScriptController.Instance.setClientScripts(data);
               handler = ScriptController.Instance.clientScripts;
            }
            else { warn(CustomNpcsPermissions.EDIT_CLIENT_SCRIPT.getNodeName()); }
            break;
         } // Client
         case 7: {
            ScriptController.Instance.setPotionScripts(data);
            handler = ScriptController.Instance.potionScripts;
            break;
         } // Potion
         case 8: {
            ScriptController.Instance.setNPCsScripts(data);
            handler = ScriptController.Instance.npcsScripts;
            break;
         } // all NPC's
      }
      SPacketScriptText.handlers.put(type, handler);
      CustomNpcs.debugData.end("Packets");
   }

}
