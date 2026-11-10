package noppes.npcs.blocks.tiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import noppes.npcs.CustomBlocks;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.client.gui.util.quests.QuestInterface;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.util.CustomNPCsScheduler;

import javax.annotation.Nonnull;

public class TileWaypoint extends TileNpcEntity {

   public String name = "";
   private int ticks = 10;
   private final List<Player> recentlyChecked = new ArrayList<>();
   private List<Player> toCheck;
   public int range = 10;

   public TileWaypoint(BlockPos pos, BlockState state) {
      super(CustomBlocks.tile_waypoint, pos, state);
   }

   @Override
   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) { handleUpdateTag(Objects.requireNonNull(pkt.getTag())); }

   @Override
   public void handleUpdateTag(CompoundTag compound) { range = compound.getInt("range"); }

   @Override
   public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

   @Override
   public @Nonnull CompoundTag getUpdateTag() {
      CompoundTag compound = new CompoundTag();
      compound.putInt("x", worldPosition.getX());
      compound.putInt("y", worldPosition.getY());
      compound.putInt("z", worldPosition.getZ());
      compound.putInt("range", range);
      return compound;
   }

   public static void tick(Level level, BlockPos ignoredPos, BlockState ignoredState, TileWaypoint tile) {
      if (level.isClientSide || tile.name.isEmpty()) { return; }
      --tile.ticks;
      if (tile.ticks > 0) { return; }
      tile.ticks = 10;

      tile.toCheck = tile.getPlayerList(tile.range, tile.range, tile.range);
      tile.toCheck.removeAll(tile.recentlyChecked);
      List<Player> listMax = tile.getPlayerList(tile.range + 10, tile.range + 10, tile.range + 10);
      tile.recentlyChecked.retainAll(listMax);
      tile.recentlyChecked.addAll(tile.toCheck);
      if (tile.toCheck.isEmpty()) { return;}

      for (Player player : tile.toCheck) {
         PlayerData pdata = PlayerData.get(player);
         PlayerQuestData questData = pdata.questData;
         CustomNPCsScheduler.runTack(() -> {
            for (QuestData data : questData.activeQuests.values()) {
               if (data.quest.step == 2 && data.quest.questInterface.isCompleted(player)) { continue; }
               boolean bo = data.quest.step == 1;
               for (QuestObjective questObjective : data.quest.getObjectives(player)) {
                  if (data.quest.step == 1 && !bo) { break; }
                  // dimension
                  if (!questObjective.dimension.toString().equals("minecraft:any") && !player.level().dimension().location().equals(questObjective.dimension)) { continue; }
                  bo = questObjective.isCompleted();
                  if (questObjective.getEnumType() != EnumQuestTask.LOCATION || !questObjective.getTargetName().equals(tile.name)) { continue; }

                  QuestInterface quest = data.quest.questInterface;
                  if (!quest.setFound(data, tile.name)) { continue; }
                  if (data.quest.showProgressInWindow) {
                     CompoundTag compound = new CompoundTag();
                     compound.putInt("QuestID", data.quest.id);
                     compound.putString("Type", "location");
                     compound.putIntArray("Progress", new int[]{1, 1});
                     compound.putString("TargetName", questObjective.getTargetName());
                     Packets.send((ServerPlayer) player, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
                  }
                  if (data.quest.showProgressInChat) {
                     player.sendSystemMessage(Component.translatable("quest.message.location.1",
                             Component.translatable(questObjective.getTargetName()).getString(),
                             data.quest.getTitle()));
                  }
                  questData.checkQuestCompletion(player, data);
                  questData.updateClient = true;
               }
            }
         });
      }
   }

   private List<Player> getPlayerList(int x, int y, int z) {
      if (level == null) { return Collections.emptyList(); }
      return level.getEntitiesOfClass(Player.class, (new AABB(this.worldPosition, this.worldPosition.offset(1, 1, 1))).inflate(x, y, z));
   }

   @Override
   public void load(@Nonnull CompoundTag compound) {
      super.load(compound);
      name = compound.getString("LocationName");
      range = Math.max(2, compound.getInt("LocationRange"));
   }

   @Override
   public void saveAdditional(@Nonnull CompoundTag compound) {
      if (!name.isEmpty()) { compound.putString("LocationName", name); }
      compound.putInt("LocationRange", range);
      super.saveAdditional(compound);
   }

}
