package noppes.npcs.packets.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import noppes.npcs.*;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.QuestEvent;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.mixin.entity.IEntityMixin;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.packets.client.PacketChat;

public class SPacketQuestCompletionCheck extends PacketServerBasic {

   protected static int channelId;
   private int questId;
   private ItemStack selectStack;

   public SPacketQuestCompletionCheck() { }

   public SPacketQuestCompletionCheck(int questIdIn, ItemStack selectStackIn) {
      questId = questIdIn;
      selectStack = selectStackIn;
   }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public List<CustomNpcsPermissions.Permission> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) { return true; }

   @Override
   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(questId);
      buf.writeItemStack(selectStack, false);
   }

   @Override
   public void decode(FriendlyByteBuf buf) {
      questId = buf.readInt();
      selectStack = buf.readItem();
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   @SuppressWarnings("ConstantConditions")
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      PlayerData data = PlayerData.get(player);
      PlayerQuestData playerdata = data.questData;
      QuestData questdata = playerdata.activeQuests.get(questId);
      if (questdata != null && questdata.quest.questInterface.isCompleted(player)) {
         Quest quest = questdata.quest;
         double baseChance = 1.0d;
         IAttributeInstance l = player.getEntityAttribute(SharedMonsterAttributes.LUCK);
         if (l != null && l.getAttributeValue() > 0.0d) {
            double luck = l.getAttributeValue();
            if (luck < 0) {
               luck *= -1;
               baseChance -= luck * luck * -0.005555d + luck * 0.255555d; // 1lv = 25%$ 10lv = 200%
            } else {
               baseChance += luck * luck * -0.005555d + luck * 0.255555d; // 1lv = 25%$ 10lv = 200%
            }
         }
         // Luck
         List<IItemStack> createRewardItems = new ArrayList<>();
         for (DropSet ds : quest.rewardItems.values()) {
            IItemStack stack = ds.createLoot(baseChance);
            if (!stack.isEmpty()) { createRewardItems.add(stack); }
         }
         List<IItemStack> itemRewards = new ArrayList<>();
         if (!createRewardItems.isEmpty() || !selectStack.isEmpty()) {
            switch (quest.rewardType) {
               case RANDOM_ONE: {
                  itemRewards.add(createRewardItems.get(player.getRNG().nextInt(createRewardItems.size())));
                  break;
               }
               case ONE_SELECT: {
                  if (!selectStack.isEmpty()) { itemRewards.add(Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(selectStack)); }
                  break;
               }
               default: { // ALL
                  itemRewards.addAll(createRewardItems);
                  break;
               }
            }
            createRewardItems.clear();
         }
         QuestEvent.QuestTurnedInEvent event = new QuestEvent.QuestTurnedInEvent(data.scriptData.getIPlayer(), quest, itemRewards);
         event.expReward = quest.rewardExp;
         event.moneyReward = quest.rewardMoney;
         event.factionOptions = quest.factionOptions;
         EventHooks.onQuestTurnedIn(data.scriptData, event);
         quest.questInterface.handleComplete(player); // take away items according to the tasks of the quest
         // Give out rewards:
         if (event.expReward > 0) {
            NoppesUtilServer.playSound(player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.1f, 0.5f * ((player.world.rand.nextFloat() - player.world.rand.nextFloat()) * 0.7f + 1.8f));
            player.addExperience(event.expReward);
         }
         if (event.moneyReward > 0) { data.game.addMoney(event.moneyReward); }
         event.factionOptions.addPoints(player);
         if (event.mail.isValid()) { PlayerDataController.instance.addPlayerMessage(player.getServer(), player.getName(), quest.mail); }
         for (IItemStack item : event.itemRewards) {
            if (item != null) {
               NoppesUtilServer.givePlayerItem(player, player, item.getMCItemStack());
            }
         }
         if (!event.command.isEmpty()) {
            FakePlayer cPlayer = EntityNPCInterface.CommandPlayer;
            ((IEntityMixin) cPlayer).setWorld(player.world);
            cPlayer.setPosition(player.posX, player.posY, player.posZ);
            NoppesUtilServer.runCommand(cPlayer, "QuestCompletion", quest.command, player);
         }
         PlayerQuestController.setQuestFinished(quest, player);
         Quest nextQuest = (QuestController.instance == null) ? null : QuestController.instance.quests.get(event.nextQuestId);
         if (nextQuest != null) {
            PlayerQuestController.addActiveQuest(quest.getNextQuest(), player, false);
         }
         Packets.send(player, new PacketAchievement(Component.translatable("quest.finished"), Component.translatable(quest.title), 2, new NBTTagCompound()));
         Packets.send(player, new PacketChat(Component.translatable("quest.finished").append(":").append(Component.translatable(quest.title))));
         if (!quest.getCompleteText().isEmpty()) {
            SPacketGuiOpen.sendOpenGui(player, EnumGuiType.QuestCompleteText, npc, new BlockPos(questId, 0, 0));
         }
      }
      CustomNpcs.debugData.end("Packets");
   }

}
