package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tileentity.TileEntity;
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
   private int type;
   private NBTTagCompound data;

   public SPacketScriptSave() { }

   public SPacketScriptSave(int typeIn, NBTTagCompound dataIn) {
      type = typeIn;
      data = dataIn;
   }

   @Override
   public boolean toolAllowed(ItemStack item) {
      return item.getItem() == CustomItems.scripter || item.getItem() == CustomBlocks.scripted_door_item ||
              item.getItem() == CustomItems.wand || item.getItem() == CustomItems.scripter_item || item.getItem() == CustomBlocks.scripted_item;
   }

   @Override
   public boolean requiresNpc() { return type == 0; }

   @Override
   public List<CustomNpcsPermissions.Permission> getPermission() { return Collections.singletonList(CustomNpcsPermissions.TOOL_SCRIPTER); }

   @Override
   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(type);
      buf.writeNbt(data);
   }

   @Override
   public void decode(FriendlyByteBuf buf) {
      type = buf.readInt();
      data = buf.readNbt();
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
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
            TileEntity tile = player.world.getTileEntity(pd.scriptBlockPos);
            if (tile instanceof TileScripted) {
               handler = (TileScripted) tile;
               ((TileScripted) tile).setNBT(data);
               tile.markDirty();
            }
            break;
         } // Block
         case 2: {
            if (player.isCreative()) {
               ItemScriptedWrapper wrapper = (ItemScriptedWrapper) Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(player.getHeldItemMainhand());
               handler = wrapper;
               wrapper.setMCNbt(data);
               wrapper.saveScriptData();
               wrapper.updateClient = true;
               player.openContainer.detectAndSendChanges();
            }
            break;
         } // Item
         case 3: {
            ScriptController.Instance.forgeScripts.load(data);
            handler = ScriptController.Instance.forgeScripts;
            break;
         } // Forge
         case 4: {
            ScriptController.Instance.playerScripts.load(data);
            handler = ScriptController.Instance.playerScripts;
            break;
         } // Player
         case 5: {
            PlayerData pd = PlayerData.get(player);
            TileEntity tile = player.world.getTileEntity(pd.scriptBlockPos);
            if (tile instanceof TileScriptedDoor) {
               handler = (TileScriptedDoor) tile;
               ((TileScriptedDoor) tile).setNBT(data);
            }
            break;
         } // Door
         case 6: {
            if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.EDIT_CLIENT_SCRIPT)) {
               ScriptController.Instance.clientScripts.load(data);
               handler = ScriptController.Instance.clientScripts;
            }
            else { warn(CustomNpcsPermissions.EDIT_CLIENT_SCRIPT.getNodeName()); }
            break;
         } // Client
         case 7: {
            ScriptController.Instance.potionScripts.load(data);
            handler = ScriptController.Instance.potionScripts;
            break;
         } // Potion
         case 8: {
            ScriptController.Instance.npcsScripts.load(data);
            handler = ScriptController.Instance.npcsScripts;
            break;
         } // all NPC's
      }
      SPacketScriptText.handlers.put(type, handler);
      CustomNpcs.debugData.end("Packets");
   }

}
