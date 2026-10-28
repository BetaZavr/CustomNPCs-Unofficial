package noppes.npcs.api.wrapper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.*;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.IPixelmonPlayerData;
import noppes.npcs.api.entity.data.IPlayerMail;
import noppes.npcs.api.entity.data.IPlayerMiniMap;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.overlay.IOverlay;
import noppes.npcs.api.wrapper.gui.CustomGuiWrapper;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.containers.ContainerCustomGui;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.*;
import noppes.npcs.packets.server.SPacketCustomNBT;
import noppes.npcs.packets.server.SPacketDimensionTeleport;
import noppes.npcs.packets.server.SPacketGuiOpen;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

public class PlayerWrapper<T extends Player> extends EntityLivingBaseWrapper<T> implements IPlayer<T> {

   private IContainer inventory;
   private Object pixelmonPartyStorage;
   private Object pixelmonPCStorage;
   private PlayerData data;

   public PlayerWrapper(T player) { super(player); }

   @Override
   public String getName() { return entity.getName().getString(); }

   @Override
   public String getDisplayName() { return entity.getDisplayName().getString(); }

   @Override
   public int getHunger() { return entity.getFoodData().getFoodLevel(); }

   @Override
   public void setHunger(int level) { entity.getFoodData().setFoodLevel(level); }

   @Override
   public boolean hasFinishedQuest(int id) { return getData().questData.hasFinishedQuest(id); }

   @Override
   public boolean hasActiveQuest(int id) {
      PlayerQuestData data = getData().questData;
      return data.activeQuests.containsKey(id);
   }

   @Override
   public IQuest[] getActiveQuests() {
      PlayerQuestData data = getData().questData;
      List<IQuest> quests = new ArrayList<>();
      for (int id : data.activeQuests.keySet()) {
         IQuest quest = QuestController.instance.quests.get(id);
         if (quest != null) {
            quests.add(quest);
         }
      }
      return quests.toArray(new IQuest[0]);
   }

   @Override
   public IQuest[] getFinishedQuests() {
      PlayerQuestData data = getData().questData;
      List<IQuest> quests = new ArrayList<>();
      for (int id : data.getFinishedQuest()) {
         IQuest quest = QuestController.instance.quests.get(id);
         if (quest != null) { quests.add(quest); }
      }
      return quests.toArray(new IQuest[0]);
   }

   @Override
   public void startQuest(int id) {
      Quest quest = QuestController.instance.quests.get(id);

      if (quest == null) { return; }
      PlayerQuestController.addActiveQuest(quest, entity, true);
   }

   @Override
   public void sendNotification(String title, String msg, int type) {
      if (type < 0 || type > 3) { throw new CustomNPCsException("Wrong type value given " + type); }
      if (entity instanceof ServerPlayer player) {
         Packets.send(player, new PacketAchievement(Component.translatable(title), Component.translatable(msg), type, new CompoundTag()));
      }
   }

   @Override
   public void finishQuest(int id) {
      Quest quest = QuestController.instance.quests.get(id);
      if (quest != null && entity instanceof ServerPlayer player && data.questData.finish(quest, player)) {
         Packets.send(player, new PacketAchievement(Component.translatable("quest.completed"), Component.translatable(quest.title), 2, new CompoundTag()));
         Packets.send(player, new PacketChat(Component.translatable("quest.completed").append(": ").append(Component.translatable(quest.title))));
      }
   }

   @Override
   public void stopQuest(int id) {
      Quest quest = QuestController.instance.quests.get(id);
      if (quest != null) {
         PlayerData data = getData();
         data.questData.activeQuests.remove(id);
         data.updateClient = true;
      }
   }

   @Override
   public void removeQuest(int id) {
      Quest quest = QuestController.instance.quests.get(id);
      if (quest != null) {
         PlayerData data = getData();
         data.questData.activeQuests.remove(id);
         data.questData.removeFinishedQuest(id);
         data.updateClient = true;
      }
   }

   @Override
   public boolean hasReadDialog(int id) {
      PlayerDialogData data = getData().dialogData;
      return data.has(id);
   }

   @Override
   public void showDialog(int id, String name) {
      Dialog dialog = DialogController.instance.dialogs.get(id);
      if (dialog == null) {
         throw new CustomNPCsException("Unknown Dialog id: " + id);
      }
      if (!dialog.availability.isAvailable(entity)) { return; }
      EntityDialogNpc npc = new EntityDialogNpc(getWorld().getMCLevel());
      npc.display.setName(name);
      EntityUtil.Copy(entity, npc);
      npc.dialogs = new int[] { id };
      NoppesUtilServer.openDialog(entity, npc, dialog);
   }

   @Override
   public void addFactionPoints(int faction, int points) {
      PlayerData data = getData();
      data.factionData.increasePoints(entity, faction, points);
      data.updateClient = true;
   }

   @Override
   public int getFactionPoints(int faction) { return getData().factionData.getFactionPoints(entity, faction); }

   @Override
   public float getRotation() { return entity.getYRot(); }

   @Override
   public void setRotation(float rotation) { entity.setYRot(rotation); }

   @Override
   public void message(String message) { entity.sendSystemMessage(Component.translatable(NoppesStringUtils.formatText(message, entity))); }

   @Override
   public int getGamemode() {
      if (entity instanceof ServerPlayer player) {
         return player.gameMode.getGameModeForPlayer().getId();
      }
      if (entity.isCreative()) { return 1; }
      if (entity.isSpectator()) { return 3; }
      return 0;
   }

   @Override
   public void setGamemode(int type) {
      if (entity instanceof ServerPlayer player) { player.setGameMode(GameType.byId(type)); }
   }

   @Override
   public int inventoryItemCount(IItemStack item) { return inventoryItemCount(item.getMCItemStack()); }

   @Override
   public int inventoryItemCount(String id) {
      Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
      if (item == null) { throw new CustomNPCsException("Unknown item id: " + id); }
      return inventoryItemCount(new ItemStack(item, 1));
   }

   @Override
   public IContainer getInventory() {
      if (inventory == null) { inventory = new ContainerWrapper(entity.getInventory()); }
      return inventory;
   }

   @Override
   public IItemStack getInventoryHeldItem() {
      return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(entity.containerMenu.getCarried());
   }

   @Override
   public boolean removeItem(IItemStack item, int amount) {
      int count = inventoryItemCount(item.getMCItemStack());
      if (amount > count) { return false; }
      if (count == amount) { removeAllItems(item); }
      else {
         for(int i = 0; i < entity.getInventory().getContainerSize(); ++i) {
            ItemStack is = entity.getInventory().getItem(i);
            if (isItemEqual(item.getMCItemStack(), is)) {
               if (amount < is.getCount()) {
                  is.split(amount);
                  break;
               }
               entity.getInventory().setItem(i, ItemStack.EMPTY);
               amount -= is.getCount();
            }
         }
      }
      updatePlayerInventory();
      return true;
   }

   @Override
   public boolean removeItem(String id, int amount) {
      Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
      if (item == null) { throw new CustomNPCsException("Unknown item id: " + id); }
      return removeItem(Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(new ItemStack(item, 1)), amount);
   }

   @Override
   public boolean giveItem(IItemStack item) {
      ItemStack mcItem = item.getMCItemStack();
      if (mcItem.isEmpty()) { return false; }
      boolean bo = entity.getInventory().add(mcItem.copy());
      if (bo) {
         NoppesUtilServer.playSound(entity, SoundEvents.ITEM_PICKUP, 0.2F, ((entity.getRandom().nextFloat() - entity.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
         updatePlayerInventory();
      }
      return bo;
   }

   @Override
   public boolean giveItem(String id, int amount) {
      Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
      if (item == null) { return false; }
      ItemStack mcStack = new ItemStack(item);
      IItemStack itemStack = Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(mcStack);
      itemStack.setStackSize(amount);
      return giveItem(itemStack);
   }

   @Override
   public void updatePlayerInventory() {
      entity.inventoryMenu.broadcastChanges();
      if (entity instanceof ServerPlayer player) {
         player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, player.getInventory().selected, player.getInventory().getItem(player.getInventory().selected)));
         PlayerQuestData questData = getData().questData;
         CustomNPCsScheduler.runTack(() -> {
            for (QuestData data : questData.activeQuests.values()) {
               for (QuestObjective obj : data.quest.getObjectives(entity)) {
                  if (obj.getEnumType() != EnumQuestTask.ITEM) { continue; }
                  questData.checkQuestCompletion(entity, data);
               }
            }
         });
      }
   }

   @Override
   public IBlock getSpawnPoint() {
      if (entity instanceof ServerPlayer player) {
         return Objects.requireNonNull(NpcAPI.Instance()).getIBlock(player.level(), player.getRespawnPosition());
      }
      return null;
   }

   @Override
   public void setSpawnPoint(IBlock block) {
      if (entity instanceof ServerPlayer player) {
         player.setRespawnPosition(getWorld().getMCLevel().dimension(), block.getPos().getMCBlockPos(), 0.0F, true, false);
      }
   }

   @Override
   public void setSpawnpoint(int x, int y, int z) {
      if (entity instanceof ServerPlayer player) {
         x = ValueUtil.correctInt(x, -30000000, 30000000);
         z = ValueUtil.correctInt(z, -30000000, 30000000);
         y = ValueUtil.correctInt(y, 0, 256);
         player.setRespawnPosition(getWorld().getMCLevel().dimension(),
                 new BlockPos(ValueUtil.correctInt(x, -30000000, 30000000),
                         ValueUtil.correctInt(y, player.level().getMinBuildHeight(), player.level().getMaxBuildHeight()),
                         ValueUtil.correctInt(z, -30000000, 30000000)),
                 0.0F, true, false);
      }
   }

   @Override
   public void resetSpawnpoint() {
      if (entity instanceof ServerPlayer player) {
         player.setRespawnPosition(getWorld().getMCLevel().dimension(), null, 0.0F, true, false);
      }
   }

   @Override
   public void removeAllItems(IItemStack item) {
      for(int i = 0; i < entity.getInventory().getContainerSize(); ++i) {
         ItemStack is = entity.getInventory().getItem(i);
         if (ItemStack.isSameItem(is, item.getMCItemStack())) { entity.getInventory().setItem(i, ItemStack.EMPTY); }
      }
   }

   @Override
   public boolean hasAdvancement(String achievement) {
      if (entity instanceof ServerPlayer player) {
         Advancement advancement = Objects.requireNonNull(player.getServer()).getAdvancements().getAdvancement(Objects.requireNonNull(ResourceLocation.tryParse(achievement)));
         if (advancement == null) { throw new CustomNPCsException("Advancement doesnt exist"); }
         AdvancementProgress progress = player.getServer().getPlayerList().getPlayerAdvancements(player).getOrStartProgress(advancement);
         return progress.isDone();
      }
      return false;
   }

   @SuppressWarnings("all")
   public boolean hasAchievement(String achievement) { return hasAdvancement(achievement); }

   @Override
   public int getExpLevel() { return entity.experienceLevel; }

   @Override
   public void setExpLevel(int level) { entity.giveExperienceLevels(level - entity.experienceLevel); }

   @Override
   public void setPosition(double x, double y, double z) {
      if (entity instanceof ServerPlayer player) {
         SPacketDimensionTeleport.teleportPlayer(player, entity.level().dimension(), x, y, z, entity.getYRot(), entity.getXRot());
      }
   }

   @Override
   public void setPos(IPos pos) { setPosition(pos.getX(), pos.getY(), pos.getZ()); }

   @Override
   public int getType() { return 1; }

   @Override
   public boolean typeOf(int type) {
      return type == 1 || super.typeOf(type);
   }

   @Override
   public boolean hasPermission(String permission) {
      if (entity instanceof ServerPlayer player) { return CustomNpcsPermissions.hasPermission(player, permission); }
      return false;
   }

   @Override
   public IPixelmonPlayerData getPixelmonData() {
      if (!PixelmonHelper.Enabled) { throw new CustomNPCsException("Pixelmon is not installed"); }
      return new IPixelmonPlayerData() {
         public Object getParty() {
            if (pixelmonPartyStorage == null) { pixelmonPartyStorage = PixelmonHelper.getParty(entity); }
            return pixelmonPartyStorage;
         }
         public Object getPC() {
            if (pixelmonPCStorage == null) { pixelmonPCStorage = PixelmonHelper.getPc(entity); }
            return pixelmonPCStorage;
         }
      };
   }

   @Override
   public ITimers getTimers() { return getData().timers; }

   @Override
   public void removeDialog(int id) {
      PlayerData data = getData();
      data.dialogData.dialogsRead.remove(id);
      data.updateClient = true;
   }

   @Override
   public void addDialog(int id) {
      PlayerData data = getData();
      data.dialogData.read(id);
      data.updateClient = true;
   }

   @Override
   public void closeGui() {
      if (entity instanceof ServerPlayer player) {
         entity.closeContainer();
         Packets.send(player, new PacketGuiClose(new CompoundTag()));
      }
   }

   @Override
   public int factionStatus(int factionId) {
      Faction faction = FactionController.instance.getFaction(factionId);
      if (faction == null) { throw new CustomNPCsException("Unknown faction: " + factionId); }
      return faction.playerStatus(this);
   }

   @Override
   public void kick(String message) {
      if (entity instanceof ServerPlayer player) {
         player.connection.disconnect(Component.translatable(message));
      }
   }

   @Override
   public boolean canQuestBeAccepted(int questId) { return PlayerQuestController.canQuestBeAccepted(entity, questId); }

   @Override
   public void showCustomGui(ICustomGui gui) {
      if (entity instanceof ServerPlayer player && gui != null) {
         NoppesUtilServer.openContainerGui(player, EnumGuiType.CustomGui, (buf) -> buf.writeNbt(((CustomGuiWrapper) gui).toNBT()));
         ((ContainerCustomGui) player.containerMenu).setGui((CustomGuiWrapper)gui, entity);
      }
   }

   @Override
   public ICustomGui getCustomGui() {
      return entity.containerMenu instanceof ContainerCustomGui ? ((ContainerCustomGui) entity.containerMenu).customGui : null;
   }

   @Override
   public void clearData() {
      PlayerData data = getData();
      data.setNBT(new CompoundTag());
      data.save(true);
   }

   @Override
   public IContainer getOpenContainer() {
      return Objects.requireNonNull(NpcAPI.Instance()).getIContainer(entity.containerMenu);
   }

   @Override
   public void playSound(String sound, float volume, float pitch) {
      if (sound != null && !sound.isEmpty()) {
         if (entity instanceof ServerPlayer player) {
            Packets.send(player, new PacketPlaySound(sound, SoundSource.PLAYERS,
                    player.getX(), player.getY(), player.getZ(), volume, pitch));
         }
         else {
            CustomNpcs.proxy.playSound(SoundSource.PLAYERS, sound, entity.getX(), entity.getY(), entity.getZ(), volume, pitch, false, false);
         }
      }
   }

   @Override
   public void playMusic(String sound, boolean background, boolean loops) {
      if (sound != null && !sound.isEmpty()) {
         if (entity instanceof ServerPlayer player) {
            Packets.send(player, new PacketPlayMusic(new ResourceLocation(NoppesUtilServer.validLocation(sound)), !background, loops));
         }
         else {
            CustomNpcs.proxy.playSound(SoundSource.MUSIC, sound, entity.getX(), entity.getY(), entity.getZ(), 1.0f, 1.0f, background, loops);
         }
      }
   }

   @Override
   public void sendMail(IPlayerMail mail) {
      PlayerDataController.instance.addPlayerMessage(entity.getServer(), entity.getName().getString(), (PlayerMail) mail);
   }

   @Override
   public void trigger(int id, Object... arguments) {
      EventHooks.onScriptTriggerEvent(PlayerData.get(entity).scriptData, id, getWorld(), getPos(), null, arguments);
   }

   private int inventoryItemCount(ItemStack stack) {
      int count = 0;
      for(int i = 0; i < entity.getInventory().getContainerSize(); ++i) {
         ItemStack is = entity.getInventory().getItem(i);
         if (isItemEqual(stack, is)) {
            count += is.getCount();
         }
      }
      return count;
   }

   private boolean isItemEqual(ItemStack stack, ItemStack other) {
      return !other.isEmpty() && stack.getItem() == other.getItem();
   }

   private PlayerData getData() {
      if (data == null) { data = PlayerData.get(entity); }
      return data;
   }

   // New from Unofficial (Goodbird)
   @Override
   public void showOverlay(IOverlay overlay) {
      if (overlay != null) {
         data.overlay.add(overlay.getId());
         if (entity instanceof ServerPlayer player) {
            Packets.send(player, new PacketOverlayShow(overlay.save().getMCNBT()));
         }
      }
   }

   @Override
   public void showSoundSelectionGUI() {
      if (entity instanceof ServerPlayer player) { Packets.send(player, new PacketSoundGUIOpen()); }
   }

   @Override
   public void hideOverlay(int id) {
      data.overlay.remove(id);
      if (entity instanceof ServerPlayer player) { Packets.send(player, new PacketOverlayHide(id)); }
   }

   @Override
   public void hideAllOverlays() {
      data.overlay.clearOverlays();
      if (entity instanceof ServerPlayer player) { Packets.send(player, new PacketHideAllOverlays()); }
   }

   @Override
   public IPlayerSkin getSkin() { return PlayerSkinController.getInstance().getData(entity.getUUID(), 0); }

   @Override
   public IPlayerSkin getSkin(int type) { return PlayerSkinController.getInstance().getData(entity.getUUID(), type); }

   @Override
   public void addMoney(long value) {
      getData().game.addMoney(value);
   }

   @Override
   public void cameraShakingPlay(int time, int amplitude, int type, boolean isFading) {
      if (time <= 1 || time > 1200) {
         throw new CustomNPCsException("Camera shake time should be between 1 and 1200 ticks. You have: " + time);
      }
      if (amplitude <= 1 || amplitude > 25) {
         throw new CustomNPCsException("Amplitude should be between 1 and 25 value. You have: " + amplitude);
      }
      if (type < 0 || type > 5) {
         throw new CustomNPCsException("Type should be between 0 and 5 value. You have: " + type);
      }
      //Server.sendData((EntityPlayerMP) entity, EnumPacketClient.PLAY_CAMERA_SHAKING, time, amplitude, type, isFading);
   }

   @Override
   public void cameraShakingStop() {
      //Server.sendData((EntityPlayerMP) entity, EnumPacketClient.STOP_CAMERA_SHAKING);
   }

   @Override
   public void completeQuest(int id) {
      Quest quest = QuestController.instance.quests.get(id);
      if (quest == null) { return; }
      PlayerData data = getData();
      data.questData.finish(quest, entity);
      data.questData.activeQuests.remove(id);
      if (entity instanceof ServerPlayer player) {
         Packets.send(player, new PacketAchievement(Component.translatable("quest.completed"), Component.translatable(quest.title), 2, new CompoundTag()));
         Packets.send(player, new PacketChat(Component.translatable("quest.completed").append(": ").append(Component.translatable(quest.title))));
         QuestData qData = new QuestData(quest);
         if (data.questData.activeQuests.containsKey(id)) { qData = data.questData.activeQuests.get(id); }
         quest.complete(player, qData);
      }
   }

   @Override
   public IContainer getBubblesInventory() {
      IContainer invBubbles = null;
      try {
         Class<?> apiBubbles = Class.forName("baubles.api.BaublesApi");
         for (Method m : apiBubbles.getDeclaredMethods()) {
            if (m.getName().equals("getBaubles")) {
               m.trySetAccessible();
               invBubbles = new ContainerWrapper((Container) m.invoke(apiBubbles, entity));
               break;
            }
         }
      }
      catch (Exception e) {
         throw new CustomNPCsException("Mod \"Bubbles\" - not found");
      }
      return invBubbles;
   }

   @Override
   public int[] getKeyPressed() {
      return getData().overlay.getKeyPressed();
   }

   @Override
   public String getLanguage() {
      return CustomNpcs.proxy.getLanguage(entity);
   }

   @Override
   public long getMoney() {
      return getData().game.getMoney();
   }

   @Override
   public int[] getMousePressed() {
      return getData().overlay.getMousePressed();
   }

   @Override
   public String getSkinType(int type) { return PlayerSkinController.getInstance().get(entity, type); }

   @Override
   public IScreenSize getWindowSize() {
      return data.overlay.getWindowSize();
   }

   @Override
   public boolean hasMousePress(int key) {
      return getData().overlay.hasMousePress(key);
   }

   @Override
   public boolean hasOrKeyPressed(int[] key) {
      return getData().overlay.hasOrKeysPressed(key);
   }

   @Override
   public int inventoryItemCount(IItemStack stack, boolean ignoreDamage, boolean ignoreNBT) {
      return Util.instance.inventoryItemCount(entity, stack.getMCItemStack(), null, ignoreDamage, ignoreNBT);
   }

   @Override
   public int inventoryItemCount(String id, int amount) {
      Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
      if (item == null) { throw new CustomNPCsException("Unknown item id: " + id); }
      return inventoryItemCount(new ItemStack(item, 1));
   }

   @Override
   public boolean isCompleteQuest(int id) {
      PlayerQuestData data = getData().questData;
      if (data.hasFinishedQuest(id)) { return true; }
      if (!data.activeQuests.containsKey(id)) { return false; }
      QuestData qData = data.activeQuests.get(id);
      if (qData.isCompleted) { return true; }
      Quest quest = (Quest) Objects.requireNonNull(NpcAPI.Instance()).getQuests().get(id);
      return quest.questInterface.isCompleted(getMCEntity());
   }

   @Override
   public boolean isMoved() { return getData().overlay.isMoved; }

   @Override
   public void playSound(int category, IPos pos, String sound, float volume, float pitch) {
      if (sound != null && !sound.isEmpty()) {
         if (entity instanceof ServerPlayer player) {
            Packets.send(player, new PacketPlaySound(sound, SoundSource.PLAYERS, pos.getX(), pos.getY(), pos.getZ(), volume, pitch));
         }
         else {
            CustomNpcs.proxy.playSound(SoundSource.PLAYERS, sound, pos.getX(), pos.getY(), pos.getZ(), volume, pitch, false, false);
         }
      }
   }

   @Override
   public void sendTo(INbt nbt) {
      CustomNPCsScheduler.runTack(() -> {
         if (entity instanceof ServerPlayer player) { Packets.send(player, new PacketCustomNBT(nbt.getMCNBT())); }
         else { Packets.sendServer(new SPacketCustomNBT(nbt.getMCNBT())); }
      }, 10);
   }

   @Override
   public void setMoney(long value) { getData().game.setMoney(value); }

   @Override
   public void setSkin(int type, int gender, int body, int bodyColor, int hair, int hairColor, int face, int eyesColor, int leg, int jacket, int shoes, int... peculiarities) {
      PlayerSkinController.getInstance().set(entity.getUUID().toString(), type, gender, body, bodyColor, hair, hairColor, face, eyesColor, leg, jacket, shoes, peculiarities);
   }

   @Override
   public void setSkinType(int type, String location) {
      PlayerSkinController.getInstance().set(entity.getUUID().toString(), location, type);
   }

   @Override
   public void stopSound(int category, String sound) {
      if (entity instanceof ServerPlayer player) {
         Packets.send(player, new PacketStopSound(new ResourceLocation(sound == null ? "" : sound), category < 0 ? -1 : category));
      }
      else { CustomNpcs.proxy.stopSound(category, sound); }
   }

   @Override
   public IEntity<?> getRidingEntity() {
      if (entity.getVehicle() == null) { return null; }
      return Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity.getVehicle());
   }

   @Override
   public IEntity<?> getLookingEntity() {
      Entity target = Util.instance.getLookEntity(entity, null, true);
      return target == null ? null : Objects.requireNonNull(NpcAPI.Instance()).getIEntity(target);
   }

   @Override
   public IBlock getLookingBlock() {
      IRayTrace rt = rayTraceBlock(data.game.blockReachDistance, false, false);
      if (rt.getBlock() == null) { return null; }
      return rt.getBlock();
   }

   @Override
   public double getBlockReachDistance() { return data.game.blockReachDistance; }

   @Override
   public void showMarket(int marcetId) {
      if (entity instanceof ServerPlayer player) {
         Marcet market = MarcetController.getInstance().getMarcet(marcetId);
         if (market != null) { NoppesUtilServer.openContainerGui(player, EnumGuiType.PlayerTrader, (buffer) -> buffer.writeInt(marcetId)); }
      }
   }

   @Override
   public void showAuction(int auctionId) {
      if (entity instanceof ServerPlayer player) {
         AuctionHouse auction = AuctionController.getInstance().getAuction(auctionId);
         if (auction != null) {
            auction.addPlayer(player);
            SPacketGuiOpen.sendOpenGui(player, EnumGuiType.PlayerAuction,
                    null, new BlockPos(auctionId, 0, 0));
         }
      }
   }

   @Override
   public IScreenSize getScreenSize() {
      PlayerData data = PlayerData.get(entity);
      return data.overlay.screenSize;
   }

   @Override
   public void showBank(int bankId) {
      if (entity instanceof ServerPlayer player) {
         Bank bank = BankController.getInstance().getBank(bankId);
         if (bank != null) {
            Packets.send(player, new PacketBankClearPos());
            data.bankData.get(bankId).openToPlayer(player, 0, 0, 0, 1);
         }
      }
   }

   @Override
   public IPlayerMiniMap getMiniMapData() { return data.minimap; }

}
