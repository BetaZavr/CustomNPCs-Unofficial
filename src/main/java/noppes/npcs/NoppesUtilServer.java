package noppes.npcs;

import io.netty.buffer.Unpooled;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PlayMessages;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.blocks.custom.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityProjectile;
import noppes.npcs.items.custom.*;
import noppes.npcs.mixin.minecraftforge.network.MixinNetworkConstants;
import noppes.npcs.mixin.network.IMixinOpenContainer;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.*;
import noppes.npcs.packets.server.SPacketGuiOpen;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

public class NoppesUtilServer {

   private static final HashMap<UUID, Quest> editingQuests = new HashMap<>();
   private static final HashMap<UUID, Quest> editingQuestsClient = new HashMap<>();

   public static void setEditingNpc(Player player, EntityNPCInterface npc) {
      PlayerData data = PlayerData.get(player);
      data.editingNpc = npc;
      if (npc != null && player instanceof ServerPlayer sPlayer) { Packets.send(sPlayer, new PacketNpcEdit(npc.getId())); }
   }

   public static EntityNPCInterface getEditingNpc(Player player) { return PlayerData.get(player).editingNpc; }

   public static void setEditingQuest(@Nonnull Player player, @Nonnull Quest quest) {
      if (player.level().isClientSide) { editingQuestsClient.put(player.getUUID(), quest); }
      else { editingQuests.put(player.getUUID(), quest); }
   }

   public static Quest getEditingQuest(@Nonnull Player player) {
      return player.level().isClientSide ? editingQuestsClient.get(player.getUUID()) : editingQuests.get(player.getUUID());
   }

   public static void openDialog(Player player, EntityNPCInterface npc, Dialog dia) {
      if (dia == null) { return; }
      Dialog dialog = dia.copy(player);
      PlayerData playerdata = PlayerData.get(player);
      if (EventHooks.onNPCDialog(npc, player, dialog)) {
         playerdata.dialogId = -1;
         return;
      }
      playerdata.dialogId = dialog.id;
      if (!(npc instanceof EntityDialogNpc) && dia.id >= 0) { Packets.sendDelayed((ServerPlayer) player, new PacketDialog(npc.getId(), dialog.id), 100); }
      else {
         dialog.hideNPC = true;
         Packets.send((ServerPlayer) player, new PacketDialogDummy(npc.getName().getString(), dialog.save(new CompoundTag())));
      }
      dia.factionOptions.addPoints(player);
      if (dialog.hasQuest()) { PlayerQuestController.addActiveQuest(dialog.getQuest(), player, false); }
      if (!dialog.command.isEmpty()) { runCommand(npc, npc.getName().getString(), dialog.command, player); }
      if (dialog.mail.isValid()) { PlayerDataController.instance.addPlayerMessage(player.getServer(), player.getName().getString(), dialog.mail); }
      // Change from Unofficial (BetaZavr)
      PlayerDialogData data = playerdata.dialogData;
      if (!data.has(dialog.id) && dialog.id >= 0) {
         data.read(dialog.id);
         playerdata.updateClient = true;
      }
      setEditingNpc(player, npc);
      // New from Unofficial (BetaZavr)
      CustomNPCsScheduler.runTack(() -> {
         for (QuestData qData : playerdata.questData.activeQuests.values()) {
            for (IQuestObjective obj : qData.quest.getObjectives(playerdata.scriptData.getPlayer())) {
               if (obj.getType() != EnumQuestTask.DIALOG.ordinal()) { continue; }
               playerdata.questData.checkQuestCompletion(player, qData);
            }
         }
      });
   }

   public static String runCommand(Entity executer, String name, String command, Player player) {
      return runCommand(executer.getCommandSenderWorld(), executer.blockPosition(), name, command, player, executer);
   }

   public static String runCommand(final Level level, BlockPos pos, String name, String command, Player player, Entity executer) {
      if (!Objects.requireNonNull(level.getServer()).isCommandBlockEnabled()) {
         CommonUtil.NotifyOPs("Cant run commands if CommandBlocks are disabled");
         LogWriter.warn("Cant run commands if CommandBlocks are disabled");
         return "Cant run commands if CommandBlocks are disabled";
      }
      if (player != null) { command = command.replace("@dp", player.getName().getString()); }
      command = command.replace("@npc", name);
      MutableComponent output = Component.empty();
      CommandSource iCommandSender = getCommandSource(level, output);
      int permLvl = CustomNpcs.NpcUseOpCommands ? 4 : 2;
      Vec3 point = new Vec3((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D);
      CommandSourceStack commandSource = new CommandSourceStack(iCommandSender, point, Vec2.ZERO, (ServerLevel)level, permLvl, "@CustomNPCs-" + name, Component.literal("@CustomNPCs-" + name), level.getServer(), executer) {
         @Override
         public void sendFailure(@Nonnull Component text) {
            super.sendFailure(text);
            CommonUtil.NotifyOPs(text, false);
         }
      };
      Commands iCommandManager = level.getServer().getCommands();
      iCommandManager.performPrefixedCommand(commandSource, command);
      return output.getString().isEmpty() ? null : output.getString();
   }

   private static @Nonnull CommandSource getCommandSource(Level level, MutableComponent output) {
      CommandSource iCommandSender;
      try {
         iCommandSender = new RconConsoleSource(Objects.requireNonNull(level.getServer())) {

            public void sendSystemMessage(@Nonnull Component component) { output.append(component); }

            public boolean shouldInformAdmins() { return level.getGameRules().getBoolean(GameRules.RULE_COMMANDBLOCKOUTPUT); }

         };
      }
      catch (Exception var12) {
         iCommandSender = new CommandSource() {

            public void sendSystemMessage(@Nonnull Component component) { output.append(component); }

            public boolean acceptsSuccess() { return true; }

            public boolean shouldInformAdmins() { return level.getGameRules().getBoolean(GameRules.RULE_COMMANDBLOCKOUTPUT); }

            public boolean acceptsFailure() { return true; }

         };
      }
      return iCommandSender;
   }

   public static void sendOpenGui(ServerPlayer player, EnumGuiType gui, EntityNPCInterface npc) {
      SPacketGuiOpen.sendOpenGui(player, gui, npc, BlockPos.ZERO);
   }

   private static MenuType<?> getType(EnumGuiType gui) {
      return switch (gui) {
         case PlayerAnvil -> CustomContainer.container_carpentrybench;
         case CustomGui -> CustomContainer.container_customgui;
         case PlayerBank -> CustomContainer.container_bank;
         case PlayerMailOpen -> CustomContainer.container_mail;
         case MainMenuInv -> CustomContainer.container_inv;
         case CompanionInv -> CustomContainer.container_companion;
         case PlayerTrader -> CustomContainer.container_trader;
         case PlayerFollower, PlayerFollowerHire -> CustomContainer.container_followerhire;
         case SetupTraderDeal -> CustomContainer.container_tradersetup;
         case SetupDrop -> CustomContainer.container_dropsetup;
         case SetupFollower -> CustomContainer.container_followersetup;
         case SetupItemGiver -> CustomContainer.container_itemgiver;
         case ManageBanks -> CustomContainer.container_managebanks;
         case ManageRecipes -> CustomContainer.container_managerecipes;
         case MerchantAdd -> CustomContainer.container_merchantadd;
         // New from Unofficial (BetaZavr)
         case QuestTypeItem -> CustomContainer.container_questtypeitem;
         case ManageTransport -> CustomContainer.container_managetransport;
         case AvailabilityStack -> CustomContainer.container_availability_item;
         case CustomChest -> CustomContainer.container_custom_chest;
         case DeadInventory -> CustomContainer.container_npc_dead;
         case BuilderTool, ReplaceTool, RemoverTool -> CustomContainer.container_builder;
         default -> null;
      };
   }

   public static void openContainerGui(ServerPlayer player, final EnumGuiType gui, Consumer<FriendlyByteBuf> extraDataWriter) {
      if (!gui.hasContainer) { return; }
      final MenuType<?> type = getType(gui);
      try {
         final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
         extraDataWriter.accept(buffer);
         openScreen(player, new MenuProvider() {
            @Nullable
            public AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory inv, @Nonnull Player player) {
               if (type != null) {
                  try {
                     return type.create(containerId, inv, buffer);
                  } catch (Exception e) { LogWriter.error(e); }
               }
               return null;
            }
            public @Nonnull Component getDisplayName() { return Component.literal(gui.name()); }
         }, extraDataWriter);
      }
      catch (Exception e) { LogWriter.error(e); }
   }

   public static void openScreen(ServerPlayer player, MenuProvider containerSupplier, Consumer<FriendlyByteBuf> extraDataWriter) {
      if (!player.level().isClientSide) {
         player.doCloseContainer();
         player.nextContainerCounter();
         int openContainerId = player.containerCounter;
         FriendlyByteBuf extraData = new FriendlyByteBuf(Unpooled.buffer());
         extraDataWriter.accept(extraData);
         extraData.readerIndex(0);
         FriendlyByteBuf output = new FriendlyByteBuf(Unpooled.buffer());
         output.writeVarInt(extraData.readableBytes());
         output.writeBytes(extraData);
         if (output.readableBytes() < 1) {
            throw new IllegalArgumentException("Invalid PacketBuffer for openGui, found " + output.readableBytes() + " bytes");
         }
         AbstractContainerMenu c = containerSupplier.createMenu(openContainerId, player.getInventory(), player);
         if (c != null) {
            MenuType<?> type = c.getType();
            PlayMessages.OpenContainer msg = IMixinOpenContainer.OpenContainer(type, openContainerId, containerSupplier.getDisplayName(), output);
            MixinNetworkConstants.getPlayChannel().sendTo(msg, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            player.containerMenu = c;
            player.initMenu(player.containerMenu);
            MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, c));
         }
      }
   }

   public static void sendScrollData(ServerPlayer player, Map<String, Integer> map) {
      UUID id = UUID.randomUUID();
      TreeMap<Integer, Map<String, Integer>> content = new TreeMap<>();
      FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
      buf.writeUUID(id);
      buf.writeInt(0);
      buf.writeInt(map.size());
      buf.writeInt(map.size());
      Map<String, Integer> part = new HashMap<>();
      for (Map.Entry<String, Integer> e : map.entrySet()) {
         buf.writeUtf(e.getKey());
         buf.writeInt(e.getValue());
         if (buf.array().length > 65536) {
            content.put(content.size(), part);
            buf.clear();
            buf.writeInt(content.size() + 1);
            buf.writeInt(map.size());
            buf.writeInt(map.size());
            buf.writeUtf(e.getKey());
            buf.writeInt(e.getValue());
            part = new HashMap<>();
         }
         part.put(e.getKey(), e.getValue());
      }
      if (!part.isEmpty()) { content.put(content.size(), part); }
      if (content.isEmpty()) { Packets.send(player, new PacketGuiScrollData(new HashMap<>(), id, 0, 0)); }
      else {
         for (Map.Entry<Integer, Map<String, Integer>> e : content.entrySet()) {
            Packets.send(player, new PacketGuiScrollData(e.getValue(), id, e.getKey(), content.size() - 1));
         }
      }
   }

   public static void sendScrollData(ServerPlayer player, List<String> list) {
      UUID id = UUID.randomUUID();
      TreeMap<Integer, Vector<String>> content = new TreeMap<>();
      FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
      buf.writeUUID(id);
      buf.writeInt(0);
      buf.writeInt(list.size());
      buf.writeInt(list.size());
      Vector<String> part = new Vector<>();
      for (String s : list) {
         buf.writeUtf(s);
         if (buf.array().length > 65536) {
            content.put(content.size(), part);
            buf.clear();
            buf.writeInt(content.size() + 1);
            buf.writeInt(list.size());
            buf.writeInt(list.size());
            buf.writeUtf(s);
            part = new Vector<>();
         }
         part.add(s);
      }
      if (!part.isEmpty()) { content.put(content.size(), part); }
      if (content.isEmpty()) { Packets.send(player, new PacketGuiScrollList(new Vector<>(), id, 0, 0)); }
      else {
         for (Map.Entry<Integer, Vector<String>> e : content.entrySet()) {
            Packets.send(player, new PacketGuiScrollList(e.getValue(), id, e.getKey(), content.size() - 1));
         }
      }
   }

   public static void sendGuiError(ServerPlayer player, int i) {
      Packets.send(player, new PacketGuiError(i, new CompoundTag()));
   }

   public static void sendGuiClose(ServerPlayer player, CompoundTag comp) {
      Packets.send(player, new PacketGuiClose(comp));
   }

   public static void givePlayerItem(Entity entity, Player player, ItemStack item) {
      if (!entity.level().isClientSide && item != null && !item.isEmpty()) {
         item = item.copy();
         float f = 0.7F;
         double d = (double)(entity.level().random.nextFloat() * f) + (double)(1.0F - f);
         double d1 = (double)(entity.level().random.nextFloat() * f) + (double)(1.0F - f);
         double d2 = (double)(entity.level().random.nextFloat() * f) + (double)(1.0F - f);
         ItemEntity entityItem = new ItemEntity(entity.level(), entity.getX() + d, entity.getY() + d1, entity.getZ() + d2, item);
         entityItem.setPickUpDelay(2);
         entity.level().addFreshEntity(entityItem);
         if (player.getInventory().add(item)) {
            entity.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            player.take(entityItem, item.getCount());
            PlayerQuestData playerdata = PlayerData.get(player).questData;
            CustomNPCsScheduler.runTack(() -> {
               for (QuestData data : playerdata.activeQuests.values()) {
                  for (IQuestObjective obj : data.quest.getObjectives(player)) {
                     if (obj.getType() != EnumQuestTask.ITEM.ordinal()) { continue; }
                     playerdata.checkQuestCompletion(player, data);
                  }
               }
            });
            if (item.getCount() <= 0) { entityItem.remove(RemovalReason.DISCARDED); }
         }
      }
   }

   public static BlockPos getClosePos(BlockPos origin, Level level) {
      for(int x = -1; x < 2; ++x) {
         for(int z = -1; z < 2; ++z) {
            for(int y = 2; y >= -2; --y) {
               BlockPos pos = origin.offset(x, y, z);
               BlockState state = level.getBlockState(pos.above());
               if (state.isRedstoneConductor(level, pos) && level.isEmptyBlock(pos.above()) && level.isEmptyBlock(pos.above(2))) {
                  return pos.above();
               }
            }
         }
      }
      return level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, origin);
   }

   public static void playSound(LivingEntity entity, SoundEvent sound, float volume, float pitch) {
      entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.NEUTRAL, volume, pitch);
   }

   public static void playSound(Level level, BlockPos pos, SoundEvent sound, SoundSource cat, float volume, float pitch) {
      level.playSound(null, pos, sound, cat, volume, pitch);
   }

   public static Player getPlayer(MinecraftServer minecraftserver, UUID id) {
      List<ServerPlayer> list = minecraftserver.getPlayerList().getPlayers();
      for (ServerPlayer player : list) {
         if (id.equals(player.getUUID())) { return player; }
      }
      return null;
   }

   public static Entity getDamageSource(DamageSource damagesource) {
      Entity entity = damagesource.getEntity();
      if (entity == null) { entity = damagesource.getDirectEntity(); }
      if (entity instanceof EntityProjectile projectile && projectile.getOwner() instanceof LivingEntity) { entity = projectile.getOwner(); }
      else if (entity instanceof ThrowableProjectile throwable) { entity = throwable.getOwner(); }
      if (entity == null && damagesource.getEntity() != null) { entity = damagesource.getEntity(); }
      return entity;
   }

   public static boolean isItemStackNull(ItemStack is) { return is == null || is.isEmpty(); }

   // New from Unofficial BetaZavr
   public static CompoundTag setNpcDialog(int slot, int dialogId, Player player) {
      EntityNPCInterface npc = getEditingNpc(player);
      if (npc == null || !DialogController.instance.hasDialog(dialogId)) { return new CompoundTag(); }
      if (slot >= 0 && slot < npc.dialogs.length) { npc.dialogs[slot] = dialogId; } // change
      else {
         int[] newIDs = new int[npc.dialogs.length + 1];
         System.arraycopy(npc.dialogs, 0, newIDs, 0, npc.dialogs.length);
         slot = npc.dialogs.length;
         newIDs[slot] = dialogId;
         npc.dialogs = newIDs;
      } // add
      Dialog dialog = DialogController.instance.get(dialogId);
      dialog.addNpc(slot, npc);
      CompoundTag compound = new CompoundTag();
      compound.putInt("Id", dialog.id);
      compound.putInt("Slot", slot);
      compound.putString("Category", dialog.category.title);
      compound.putString("Title", dialog.title);
      return compound;
   }

   public static void sendNpcDialogs(ServerPlayer player) {
      EntityNPCInterface npc = getEditingNpc(player);
      if (npc != null) {
         int slot = 0;
         for (int dialogId : npc.dialogs) {
            if (!DialogController.instance.hasDialog(dialogId)) { continue; }
            Dialog d = DialogController.instance.get(dialogId);
            CompoundTag compound = new CompoundTag();
            compound.putInt("Id", d.id);
            compound.putInt("Slot", slot);
            compound.putString("Category", d.category.title);
            compound.putString("Title", d.title);
            Packets.send(player, new PacketGuiData(compound));
            slot++;
         }
      }
   }

   public static void sendExtraData(ServerPlayer player, EntityNPCInterface npc, EnumGuiType gui) {
      if (npc != null && npc.role.getEnumType() != RoleType.NONE &&
              (gui == EnumGuiType.PlayerFollower ||
                      gui == EnumGuiType.PlayerFollowerHire ||
                      gui == EnumGuiType.PlayerTrader ||
                      gui == EnumGuiType.PlayerTransporter)) {
         CompoundTag comp = new CompoundTag();
         npc.role.save(comp);
         Packets.send(player, new PacketNpcRole(npc.getId(), comp));
      }
   }

   public static void createAllItemFiles(ICustomElement customitem) {
      String name = customitem.getCustomName().toLowerCase();
      String fileName = "custom_" + customitem.getCustomName().toLowerCase();

      File modelsDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/models/item");
      File modelsObjDir = new File(modelsDir, "obj");
      File armorDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/models/armor");
      File armorObjDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/models/armor");
      if ((modelsDir.exists() || modelsDir.mkdirs()) &&
              (armorDir.exists() || armorDir.mkdirs()) &&
              (armorObjDir.exists() || armorObjDir.mkdirs()) &&
              (modelsObjDir.exists() || modelsObjDir.mkdirs())) {
          // Models
         File itemModel = new File(modelsDir, fileName + ".json");
         Map<File, String> modelDatas = new HashMap<>();
         if (customitem.getCustomNbt().getBoolean("IsOBJModel")) {
            File objFile = new File(modelsObjDir, name + ".obj");
            File mtlFile = new File(modelsObjDir, name + ".mtl");
            if (!itemModel.exists() || !objFile.exists() || !mtlFile.exists()) {
               modelDatas.put(itemModel, getDataFile("imas.dat", fileName, name));
               modelDatas.put(objFile, getDataFile("ima_o.dat", fileName, name));
               modelDatas.put(mtlFile, getDataFile("ima_m.dat", fileName, name));
            }
         }
         else {
            switch (customitem.getElementType()) {
               case (byte) 1: {
                  if (!itemModel.exists()) { modelDatas.put(itemModel, getDataFile("imw.dat", fileName, name)); }
                  break;
               } // Weapon
               case (byte) 2: {
                  if (!itemModel.exists()) { modelDatas.put(itemModel, getDataFile("imt.dat", fileName, name)); }
                  break;
               } // Tool
               case (byte) 3: {
                  String slot = ((CustomArmor) customitem).getType().getName().toLowerCase();
                  if (((CustomArmor) customitem).objModel != null) {
                     File objFile = new File(armorObjDir, name + ".obj");
                     File mtlFile = new File(armorObjDir, name + ".mtl");
                     if (!itemModel.exists() || !objFile.exists() || !mtlFile.exists()) {
                        modelDatas.put(itemModel, getDataFile("imro.dat", fileName, name + "_" + slot));
                        modelDatas.put(objFile, getDataFile("am_o.dat", fileName, name));
                        modelDatas.put(mtlFile, getDataFile("am_m.dat", fileName, name));
                     }
                  }
                  else {
                     File ironDarkerTrimFile = new File(modelsDir, fileName + "_" + slot + "_iron_darker_trim.json");
                     File quartzTrimFile = new File(modelsDir, fileName + "_" + slot + "_quartz_trim.json");
                     File netheriteTrimFile = new File(modelsDir, fileName + "_" + slot + "_netherite_trim.json");
                     File redstoneTrimFile = new File(modelsDir, fileName + "_" + slot + "_redstone_trim.json");
                     File amethystTrimFile = new File(modelsDir, fileName + "_" + slot + "_amethyst_trim.json");
                     File goldTrimFile = new File(modelsDir, fileName + "_" + slot + "_gold_trim.json");
                     File lapisTrimFile = new File(modelsDir, fileName + "_" + slot + "_lapis_trim.json");
                     File emeraldTrimFile = new File(modelsDir, fileName + "_" + slot + "_emerald_trim.json");
                     File copperTrimFile = new File(modelsDir, fileName + "_" + slot + "_copper_trim.json");
                     if (!itemModel.exists() ||
                             !ironDarkerTrimFile.exists() || !quartzTrimFile.exists() ||
                             !netheriteTrimFile.exists() || !redstoneTrimFile.exists() ||
                             !amethystTrimFile.exists() || !goldTrimFile.exists() ||
                             !lapisTrimFile.exists() || !emeraldTrimFile.exists() || !copperTrimFile.exists()) {
                        modelDatas.put(itemModel, getDataFile("imr.dat", fileName, name).replace("{slot}", slot));
                        String data = getDataFile("imrp.dat", fileName, null);
                        modelDatas.put(ironDarkerTrimFile, data.replace("{name}", "iron_darker").replace("{slot}", slot));
                        modelDatas.put(quartzTrimFile, data.replace("{name}", "quartz").replace("{slot}", slot));
                        modelDatas.put(netheriteTrimFile, data.replace("{name}", "netherite").replace("{slot}", slot));
                        modelDatas.put(redstoneTrimFile, data.replace("{name}", "redstone").replace("{slot}", slot));
                        modelDatas.put(amethystTrimFile, data.replace("{name}", "amethyst").replace("{slot}", slot));
                        modelDatas.put(goldTrimFile, data.replace("{name}", "gold").replace("{slot}", slot));
                        modelDatas.put(lapisTrimFile, data.replace("{name}", "lapis").replace("{slot}", slot));
                        modelDatas.put(emeraldTrimFile, data.replace("{name}", "emerald").replace("{slot}", slot));
                        modelDatas.put(copperTrimFile, data.replace("{name}", "copper").replace("{slot}", slot));
                     }
                  }
                  break;
               } // Armor
               case (byte) 4: {
                  File blockingFile = new File(modelsDir, fileName + "_blocking.json");
                  if (!itemModel.exists() || !blockingFile.exists()) {
                     modelDatas.put(itemModel, getDataFile("imsb.dat", fileName, name));
                     modelDatas.put(blockingFile, getDataFile("ims.dat", fileName, name));
                  }
                  break;
               } // Shield
               case (byte) 5: {
                  File pulling_0_File = new File(modelsDir, fileName + "_pulling_0.json");
                  File pulling_1_File = new File(modelsDir, fileName + "_pulling_1.json");
                  File pulling_2_File = new File(modelsDir, fileName + "_pulling_2.json");
                  if (!itemModel.exists() || !pulling_0_File.exists() ||
                          !pulling_1_File.exists() || !pulling_2_File.exists()) {
                     modelDatas.put(itemModel, getDataFile("imb.dat", fileName, name));
                     String jsonModel = getDataFile("imbp.dat", fileName, name);
                     modelDatas.put(pulling_0_File, jsonModel.replace("{num}", "0"));
                     modelDatas.put(pulling_1_File, jsonModel.replace("{num}", "1"));
                     modelDatas.put(pulling_2_File, jsonModel.replace("{num}", "2"));
                  }
                  break;
               } // Bow
               case (byte) 7: {
                  break;
               } // Potion
               case (byte) 8: {
                  File castFile = new File(modelsDir, fileName + "_cast.json");
                  if (!itemModel.exists() || !castFile.exists()) {
                     modelDatas.put(itemModel, getDataFile("imf.dat", fileName, name));
                     modelDatas.put(castFile, getDataFile("imfc.dat", fileName, name));
                  }
                  break;
               } // Fishing Rod
               default: {
                  if (!itemModel.exists()) { modelDatas.put(itemModel, getDataFile("im.dat", fileName, name)); }
                  break;
               } // 0: Simple
            }
         }
         // Write
         for (Map.Entry<File, String> entry: modelDatas.entrySet()) {
            if (Util.instance.saveFile(entry.getKey(), entry.getValue())) {
               LogWriter.debug("Create Default Item Model for \"" + name + "\" item. File: " + entry.getKey().getName());
            }
         }
      }
   }

   public static void createAllBlockFiles(ICustomElement customblock) {
      String name = customblock.getCustomName();
      String fileName = "custom_" + name.toLowerCase();
      File blockStatesDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/blockstates");
      File blockModelsDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/models/block");
      File blockObjModelsDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/models/block/obj");
      File itemModelsDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/models/item");

      if ((blockStatesDir.exists() || blockStatesDir.mkdirs()) &&
              (blockModelsDir.exists() || blockModelsDir.mkdirs()) &&
              (itemModelsDir.exists() || itemModelsDir.mkdirs()) &&
              (blockObjModelsDir.exists() || blockObjModelsDir.mkdirs())) {
          // Standard orientable base block:
         File orientable = new File(blockModelsDir, "orientable.json");
         if (!orientable.exists() && Util.instance.saveFile(orientable, Util.instance.getDataFile("ort.dat"))) {
            LogWriter.debug("Create Orientable Block Model for \"orientable\" block");
         }
         // Standard chest base block:
         File chestFile = new File(blockModelsDir, "chest.json");
         if (!chestFile.exists() && Util.instance.saveFile(chestFile, Util.instance.getDataFile("jch.dat"))) {
            LogWriter.debug("Create Chest Block Model for \"custom chest\" block");
         }

         File blockstate = new File(blockStatesDir, fileName + ".json"); // state
         File blockModel = new File(blockModelsDir, fileName + ".json"); // block model
         File itemFile = new File(itemModelsDir, fileName + ".json"); // item model
         Map<File, String> stateDatas = new HashMap<>();
         Map<File, String> modelDatas = new HashMap<>();
         if (customblock.getCustomNbt().getBoolean("IsOBJModel")) {
            File objFile = new File(blockObjModelsDir, fileName + ".obj");
            File mtlFile = new File(blockObjModelsDir, fileName + ".mtl");
            if (!blockstate.exists() || !itemFile.exists() || !blockModel.exists() || !objFile.exists() || !mtlFile.exists()) {
               stateDatas.put(blockstate, getDataFile("jb.dat", fileName, name));
               modelDatas.put(blockModel, getDataFile("bmo.dat", fileName, name));
               modelDatas.put(objFile, getDataFile("bmc_o.dat", fileName, name));
               modelDatas.put(mtlFile, getDataFile("bmc_m.dat", fileName, name));
               modelDatas.put(itemFile, getDataFile("bmio.dat", fileName, name));
            }
         }
         else {
            switch (customblock.getElementType()) {
               case 1: {
                  blockstate = new File(blockStatesDir, fileName + ".json");
                  File bucketFile = new File(itemModelsDir, fileName + "_bucket.json");
                  File bottleFile = new File(itemModelsDir, fileName + "_bottle.json");
                  if (!blockstate.exists() || !blockModel.exists() ||
                          !bucketFile.exists() || !bottleFile.exists()) {
                     stateDatas.put(blockstate, getDataFile("jlq.dat", fileName, name));
                     modelDatas.put(blockModel, getDataFile("bml.dat", fileName, name));
                     String fileData = getDataFile("iml.dat", fileName, name);
                     modelDatas.put(bucketFile, fileData.replace("{part}", "bucket"));
                     modelDatas.put(bottleFile, fileData.replace("{part}", "bottle"));
                  }
                  if (customblock.getCustomNbt().getBoolean("AddCauldron")) {
                     File cauldronStateFile = new File(blockStatesDir, fileName + "_cauldron.json");
                     File fullFile = new File(blockModelsDir, fileName + "_cauldron_full.json");
                     File level1File = new File(blockModelsDir, fileName + "_cauldron_level1.json");
                     File level2File = new File(blockModelsDir, fileName + "_cauldron_level2.json");
                     if (!cauldronStateFile.exists() || !fullFile.exists() || !level1File.exists() || !level2File.exists()) {
                        stateDatas.put(cauldronStateFile, getDataFile("jlqc.dat", fileName, name));
                        modelDatas.put(fullFile, getDataFile("bmlc.dat", fileName, name).replace("{type}", "_full"));
                        modelDatas.put(level1File, getDataFile("bmlc.dat", fileName, name).replace("{type}", "_level1"));
                        modelDatas.put(level2File, getDataFile("bmlc.dat", fileName, name).replace("{type}", "_level2"));
                     }
                  }
                  break;
               } // Liquid
               case 2: {
                  if (!blockstate.exists() || !blockModel.exists() || !itemFile.exists()) {
                     boolean isChest = ((CustomChest) customblock).isChest;
                     stateDatas.put(blockstate, getDataFile("jb" + (isChest ? "h" : "") + ".dat", fileName, name));
                     modelDatas.put(blockModel, getDataFile("bm" + (isChest ? "h" : "") + ".dat", fileName, name));
                     modelDatas.put(itemFile, getDataFile((isChest ? "imh" : "bmi") + ".dat", fileName, name));
                  }
                  break;
               } // Chest
               case 3: {
                  File innerFile = new File(blockModelsDir, fileName + "_inner.json");
                  File outerFile = new File(blockModelsDir, fileName + "_outer.json");
                  if (!blockstate.exists() || !blockModel.exists() || !itemFile.exists() || !innerFile.exists() || !outerFile.exists()) {
                     stateDatas.put(blockstate, getDataFile("jbs.dat", fileName, name));
                     String data = getDataFile("bms.dat", fileName, name);
                     modelDatas.put(blockModel, data.replace("{type}", ""));
                     modelDatas.put(innerFile, data.replace("{type}", "inner_"));
                     modelDatas.put(outerFile, data.replace("{type}", "outer_"));
                     modelDatas.put(itemFile, getDataFile("bmi.dat", fileName, name));
                  }
                  break;
               } // Stairs
               case 4: {
                  File slabFile = new File(blockModelsDir, fileName + "_slab.json");
                  File topFile = new File(blockModelsDir, fileName + "_slab_top.json");
                  if (!blockstate.exists() || !blockModel.exists() || !itemFile.exists() || !slabFile.exists() || !topFile.exists()) {
                     stateDatas.put(blockstate, getDataFile("jss.dat", fileName, name));
                     String data = getDataFile("bmss.dat", fileName, name);
                     modelDatas.put(blockModel, getDataFile("bmfc.dat", fileName, name)); // double
                     modelDatas.put(slabFile, data.replace("{type}", ""));
                     modelDatas.put(topFile, data.replace("{type}", "_top"));
                     modelDatas.put(itemFile, getDataFile("bmi.dat", fileName + "_slab", name));
                  }
                  break;
               } // Slab
               case 5: {
                  if (!blockstate.exists() || !blockModel.exists() || !itemFile.exists()) {
                     stateDatas.put(blockstate, getDataFile("jbp.dat", fileName, name));
                     modelDatas.put(blockModel, getDataFile("bmp.dat", name, name));
                     modelDatas.put(itemFile, getDataFile("imp.dat", fileName, name));
                  }
                  break;
               } // Portal
               case 6: {
                  File bottomLeftFile = new File(blockModelsDir, fileName + "_bottom_left.json");
                  File bottomLeftOpenFile = new File(blockModelsDir, fileName + "_bottom_left_open.json");
                  File bottomRightFile = new File(blockModelsDir, fileName + "_bottom_right.json");
                  File bottomRightOpenFile = new File(blockModelsDir, fileName + "_bottom_right_open.json");
                  File topLeftFile = new File(blockModelsDir, fileName + "_top_left.json");
                  File topLeftOpenFile = new File(blockModelsDir, fileName + "_top_left_open.json");
                  File topRightFile = new File(blockModelsDir, fileName + "_top_right.json");
                  File topRightOpenFile = new File(blockModelsDir, fileName + "_top_right_open.json");
                  if (!blockstate.exists() || !itemFile.exists() ||
                          !bottomLeftFile.exists() || !bottomLeftOpenFile.exists() ||
                          !bottomRightFile.exists() || !bottomRightOpenFile.exists() ||
                          !topLeftFile.exists() || !topLeftOpenFile.exists() ||
                          !topRightFile.exists() || !topRightOpenFile.exists()) {
                     stateDatas.put(blockstate, getDataFile("jbd.dat", fileName, name));
                     String data = getDataFile("bmd.dat", fileName, name);
                     modelDatas.put(bottomLeftFile, data.replace("{type}", "_bottom_left"));
                     modelDatas.put(bottomLeftOpenFile, data.replace("{type}", "_bottom_left_open"));
                     modelDatas.put(bottomRightFile, data.replace("{type}", "_bottom_right"));
                     modelDatas.put(bottomRightOpenFile, data.replace("{type}", "_bottom_right_open"));
                     modelDatas.put(topLeftFile, data.replace("{type}", "_top_left"));
                     modelDatas.put(topLeftOpenFile, data.replace("{type}", "_top_left_open"));
                     modelDatas.put(topRightFile, data.replace("{type}", "_top_right"));
                     modelDatas.put(topRightOpenFile, data.replace("{type}", "_top_right_open"));
                     modelDatas.put(itemFile, getDataFile("bmid.dat", fileName, name));
                  }
                  break;
               } // Door
               default: {
                  if (!blockstate.exists() || !blockModel.exists() || !itemFile.exists()) {
                     if (customblock instanceof CustomBlock block && block.hasProperty()) {
                        CompoundTag data = customblock.getCustomNbt().getMCNBT().getCompound("Property");
                        String state = getDataFile("jpr.dat", fileName, name);
                        StringBuilder variants = new StringBuilder();
                        if (block.BO != null) {
                           variants.append("    \"").append(data.getString("Name")).append("=true\": { \"model\": \"").append(CustomNpcs.MODID).append(":block/").append(fileName).append("_true\" },").append((char) 10);
                           variants.append("    \"").append(data.getString("Name")).append("=false\": { \"model\": \"").append(CustomNpcs.MODID).append(":block/").append(fileName).append("_false\" }");
                           stateDatas.put(blockstate, state.replace("{type}", "Boolean").replace("{variants}", variants.toString()));
                           modelDatas.put(blockModel, getDataFile("bm.dat", fileName, name));
                        } // boolean
                        else if (block.INT != null) {
                           for (int i = data.getInt("Min"); i <= data.getInt("Max"); i++) {
                              variants.append("    \"").append(data.getString("Name")).append("=").append(i).append("\": { \"model\": \"").append(CustomNpcs.MODID).append(":block/").append(fileName).append("_").append(i).append("\" }");
                              if (i < data.getInt("Max") - 1) { variants.append(",").append((char) 10); }
                           }
                           stateDatas.put(blockstate, state.replace("{type}", "Integer").replace("{variants}", variants.toString()));
                           modelDatas.put(blockModel, getDataFile("bm.dat", fileName, name));
                        } // int
                        else if (block.FACING != null) {
                           int i = 0;
                           for (Direction ef : Direction.values()) {
                              if (ef == Direction.DOWN || ef == Direction.UP) { continue; }
                              variants.append("    \"").append(data.getString("Name")).append("=").append(ef.getName()).append("\": { \"model\": \"").append(CustomNpcs.MODID).append(":block/").append(fileName).append("\"");
                              if (ef == Direction.SOUTH) { variants.append(", \"y\": 180"); }
                              else if (ef == Direction.WEST) { variants.append(", \"y\": 270"); }
                              else if (ef == Direction.EAST) { variants.append(", \"y\": 90"); }
                              variants.append(" }");
                              if (i < 3) { variants.append(",").append((char) 10); }
                              i++;
                           }
                           stateDatas.put(blockstate, state.replace("{type}", "Fasing").replace("{variants}", variants.toString()));
                           modelDatas.put(blockModel, getDataFile("bmf.dat", fileName, name));
                        } // facing
                     }
                     else {
                        stateDatas.put(blockstate, getDataFile("jb.dat", fileName, name));
                        modelDatas.put(blockModel, getDataFile("bm.dat", fileName, name));
                     }
                     modelDatas.put(itemFile, getDataFile("bmi.dat", fileName, name));
                  }
               }
            }
         }
         // Write
         for (Map.Entry<File, String> entry : stateDatas.entrySet()) {
            if (Util.instance.saveFile(entry.getKey(), entry.getValue())) {
               LogWriter.debug("Create Default Blockstate for \"" + entry.getKey().getName() + "\" block");
            }
         }
         for (Map.Entry<File, String> entry : modelDatas.entrySet()) {
            if (Util.instance.saveFile(entry.getKey(), entry.getValue())) {
               LogWriter.debug("Create Default Block Model for \"" + entry.getKey().getName() + "\" variant");
            }
         }
      }
   }

   public static String getDataFile(String data, String fileName, String name) {
      String fileData = Util.instance.getDataFile(data).replace("{mod_id}", CustomNpcs.MODID);
      if (fileName != null && !fileName.isEmpty()) { fileData = fileData.replace("{file_name}", fileName); }
      if (name != null && !name.isEmpty()) { fileData = fileData.replace("{name}", name); }
      return fileData;
   }

   public static BlockPos getSafeTpPos(Level level, BlockPos tpPos, int yMax, int yMin) {
      Function<BlockPos, Boolean> isSafeSpot = (p) -> {
         BlockState s = level.getBlockState(p);
         BlockPos pu = p.above();
         BlockState su = level.getBlockState(pu);
         return (level.isEmptyBlock(p) && level.isEmptyBlock(pu)) || (s.canOcclude() && su.canOcclude());
      };

      BlockPos pos = new BlockPos(tpPos.getX(), tpPos.getY(), tpPos.getZ());
      while (pos.getY() <= yMax && !isSafeSpot.apply(pos)) { pos = pos.above(); }
      if (isSafeSpot.apply(pos)) { return pos; }

      pos = new BlockPos(tpPos.getX(), tpPos.getY(), tpPos.getZ());
      while (pos.getY() > yMin && !isSafeSpot.apply(pos)) { pos = pos.below(); }
      if (isSafeSpot.apply(pos)) { return pos; }

      return tpPos;
   }

   public static String validLocation(String location) {
      if (location.contains(":")) {
         String domain = validNamespace(location.substring(0, location.indexOf(":")));
         String path = validPath(location.substring(location.indexOf(":") + 1));
         location = domain + ":" + path;
      }
      else { location = validPath(location.substring(location.indexOf(":") + 1)); }
      return location;
   }

   public static String validNamespace(String path) {
      StringBuilder valid = new StringBuilder();
      boolean isChange = false;
      for (char ch : path.toCharArray()) {
         if (ResourceLocation.validNamespaceChar(ch)) { valid.append(ch); }
         else {
            if (Character.isUpperCase(ch)) {
               char lowerCh = Character.toLowerCase(ch);
               if (lowerCh >= 'a' && lowerCh <= 'z') { valid.append(lowerCh); }
               else { valid.append('_'); }
            }
            else { valid.append('_'); }
            isChange = true;
         }
      }
      while (valid.length() < 2) {
         valid.append("_");
         isChange = true;
      }
      if (isChange) { return valid.toString(); }
      return path;
   }

   public static String validPath(String path) {
      StringBuilder valid = new StringBuilder();
      boolean isChange = false;
      for (char ch : path.toCharArray()) {
         if (ResourceLocation.validPathChar(ch)) { valid.append(ch); }
         else {
            if (Character.isUpperCase(ch)) {
               char lowerCh = Character.toLowerCase(ch);
               if (lowerCh >= 'a' && lowerCh <= 'z') { valid.append(lowerCh); }
               else { valid.append('_'); }
            }
            else { valid.append('_'); }
            isChange = true;
         }
      }
      while (valid.length() < 2) {
         valid.append("_");
         isChange = true;
      }
      if (isChange) { return valid.toString(); }
      return path;
   }

}
