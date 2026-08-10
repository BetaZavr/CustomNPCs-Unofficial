package noppes.npcs;

import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.entity.player.PlayerContainerEvent.Close;
import net.minecraftforge.event.entity.player.PlayerContainerEvent.Open;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.event.level.LevelEvent.CreateSpawnPosition;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.GenericEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent.ClientCustomPayloadEvent;
import noppes.npcs.api.IDamageSource;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.event.ItemEvent;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.api.item.ISpecBuilder;
import noppes.npcs.api.mixin.entity.ILivingEntityMixin;
import noppes.npcs.api.mixin.world.level.block.entity.ITileEntityBanner;
import noppes.npcs.api.wrapper.BlockWrapper;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.client.ClientEventHandler;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.Resistances;
import noppes.npcs.items.ItemBoundary;
import noppes.npcs.items.ItemNbtBook;
import noppes.npcs.items.ItemScripted;
import noppes.npcs.mixin.minecraftforge.event.entity.living.ILivingAttackEventMixin;
import noppes.npcs.mixin.minecraftforge.eventbus.IEventBusMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.*;
import noppes.npcs.packets.server.SPacketContainerOpen;
import noppes.npcs.packets.server.SPacketDimensionTeleport;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;
import org.jetbrains.annotations.NotNull;

public class ScriptPlayerEventHandler {

   public static Object temp;

   private void doCraftQuest(ServerPlayer player, ItemStack crafting) {
      PlayerData pdata = PlayerData.get(player);
      PlayerQuestData playerdata = pdata.questData;
      // found in quests
      for (QuestData questData : playerdata.activeQuests.values()) {
         // check quest step
         if (questData.quest.step == 2 && questData.quest.questInterface.isCompleted(player)) { continue; }
         boolean bo = questData.quest.step == 1;
         // found tacks
         for (QuestObjective questObjective : questData.quest.getObjectives(player)) {
            if (questData.quest.step == 1 && !bo) { break; }
            bo = questObjective.isCompleted();
            // dimension
            if (!questObjective.dimension.toString().equals("minecraft:any") && !player.level().dimension().location().equals(questObjective.dimension)) { continue; }
            // craft only
            if (questObjective.getEnumType() != EnumQuestTask.CRAFT) { continue; }
            int size = 0;
            if (!NoppesUtilServer.isItemStackNull(crafting) && NoppesUtilPlayer.compareItems(questObjective.getItem().getMCItemStack(), crafting, questObjective.isIgnoreDamage(), questObjective.isItemIgnoreNBT())) { size = crafting.getCount(); }
            if (size == 0) { continue; }
            // add progress
            HashMap<ItemStack, Integer> crafted = questObjective.getCrafted(questData);
            int amount = 0;
            ItemStack key = questObjective.getItem().getMCItemStack();
            for (ItemStack inData : crafted.keySet()) {
               if (NoppesUtilPlayer.compareItems(questObjective.getItem().getMCItemStack(), inData, questObjective.isIgnoreDamage(), questObjective.isItemIgnoreNBT())) {
                  amount = crafted.get(inData);
                  key = inData;
                  break;
               }
            }
            if (amount >= questObjective.getMaxProgress()) { continue; }
            if (amount + size > questObjective.getMaxProgress()) { size = questObjective.getMaxProgress() - amount; }
            amount += size;
            crafted.put(key, amount);
            questObjective.setCrafted(questData, crafted);
            if (questData.quest.showProgressInWindow) {
               CompoundTag compound = new CompoundTag();
               compound.putInt("QuestID", questData.quest.id);
               compound.putString("Type", "craft");
               compound.putIntArray("Progress", new int[] { amount, questObjective.getMaxProgress() });
               compound.put("Item", crafting.save(new CompoundTag()));
               compound.putInt("MessageType", 0);
               Packets.send(player, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
            }
            if (questData.quest.showProgressInChat) {
               if (amount >= questObjective.getMaxProgress()) { player.sendSystemMessage(Component.translatable("quest.message.craft.1", crafting.getDisplayName().getString(), questData.quest.getTitle().getString())); }
               else { player.sendSystemMessage(Component.translatable("quest.message.craft.0", crafting.getDisplayName().getString(), "" + amount, "" + questObjective.getMaxProgress(), questData.quest.getTitle().getString())); }
            }

            pdata.updateClient = true;
            if (questObjective.isItemLeave()) {
               boolean ch = ItemStack.isSameItem(player.getInventory().getSelected(), crafting);
               crafting.split(size);
               player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, player.getInventory().selected, player.getInventory().getItem(player.getInventory().selected)));
               if (ch) {
                  CompoundTag nbtStack = new CompoundTag();
                  player.getInventory().getSelected().save(nbtStack);
                  Packets.send(player, new PacketDetectHeldItem(-1, nbtStack));
               }
            }
            playerdata.checkQuestCompletion(player, questData);
            playerdata.updateClient = true;
         }
      }
   }

   @SubscribeEvent
   public void cnpcPlayerTick(PlayerTickEvent event) {
      if (event.side != LogicalSide.SERVER || event.phase != Phase.START) { return; }
      CustomNpcs.debugData.start(event.player);
      ServerPlayer player = (ServerPlayer) event.player;
      PlayerData data = PlayerData.get(player);
      if (player.tickCount % 10 == 0) {
         EventHooks.onPlayerTick(data.scriptData);
         for(int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);
            if (!item.isEmpty() && item.getItem() == CustomItems.scripted_item) {
               ItemScriptedWrapper isw = (ItemScriptedWrapper) Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(item);
               EventHooks.onScriptItemUpdate(isw, player);
               if (isw.updateClient) {
                  isw.updateClient = false;
                  Packets.send(player, new PacketItemUpdate(i, isw.getMCNbt()));
               }
            }
         }
      }
      if (data.playerLevel != player.experienceLevel) {
         EventHooks.onPlayerLevelUp(data.scriptData, data.playerLevel - player.experienceLevel);
         data.playerLevel = player.experienceLevel;
      }
      data.timers.update();
      // New from Unofficial (BetaZavr)
      ResourceKey<Level> dimId = event.player.level().dimension();
      if (!data.game.dimID.equals(dimId)) {
         if (CustomNpcs.SetHomeDimension) {
            player.setRespawnPosition(dimId, player.blockPosition(), player.getYRot(), true, false);
         }
         data.game.dimID = event.player.level().dimension();
      }
      CustomNpcs.debugData.end(event.player);
   }

   @SubscribeEvent
   public void cnpcLeftClick(LeftClickBlock event) {
      if (!(event.getEntity() instanceof ServerPlayer) && event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) { return; }
      ServerPlayer player = (ServerPlayer) event.getEntity();
      CustomNpcs.debugData.start(player);
      if (event.getItemStack().getItem() == CustomItems.npcboundary) {
         ((ItemBoundary) event.getItemStack().getItem()).leftClick(event.getItemStack(), player);
         event.setCanceled(true);
      }
      else if (event.getItemStack().getItem() instanceof ISpecBuilder) {
         if (player.isCreative()) {
            ((ISpecBuilder) event.getItemStack().getItem()).leftClick(event.getItemStack(), player, event.getPos());
         }
         else { player.sendSystemMessage(Component.translatable("availability.permission")); }
         event.setCanceled(true);
      }
      else if (event.getItemStack().getItem() == CustomItems.teleporter) { event.setCanceled(true); }
      else {
         PlayerScriptData handler = PlayerData.get(player).scriptData;
         PlayerEvent.AttackEvent ev = new PlayerEvent.AttackEvent(handler.getPlayer(), 2, Objects.requireNonNull(NpcAPI.Instance()).getIBlock(event.getLevel(), event.getPos()));
         event.setCanceled(EventHooks.onPlayerAttack(handler, ev));
         if (event.getItemStack().getItem() == CustomItems.scripted_item && !event.isCanceled()) {
            ItemScriptedWrapper isw = ItemScripted.GetWrapper(event.getItemStack());
            ItemEvent.AttackEvent eve = new ItemEvent.AttackEvent(isw, handler.getPlayer(), 2, Objects.requireNonNull(NpcAPI.Instance()).getIBlock(event.getLevel(), event.getPos()));
            eve.setCanceled(event.isCanceled());
            event.setCanceled(EventHooks.onScriptItemAttack(isw, eve));
         }
      }
      CustomNpcs.debugData.end(player);
   }

   @SubscribeEvent
   public void cnpcRightClick(RightClickBlock event) {
      if (event.getEntity().level().isClientSide || event.getHand() != InteractionHand.MAIN_HAND || !(event.getLevel() instanceof ServerLevel)) { return; }
      ServerPlayer player = (ServerPlayer) event.getEntity();
      CustomNpcs.debugData.start(player);
      if (!(event.getItemStack().getItem() instanceof INPCToolItem)) {
         Entity deadTarget = Util.instance.getLookEntity(player, 4.0d, false);
         if (deadTarget != null && !deadTarget.isAlive() && deadTarget instanceof EntityNPCInterface npc) {
            DataInventory dataInv = npc.inventory;
            Container deadInventory = dataInv.deadLoot;
            if (deadInventory == null && dataInv.deadLoots != null && dataInv.deadLoots.containsKey(player)) { deadInventory = dataInv.deadLoots.get(player); }
            if (deadInventory != null) {
               NoppesUtilServer.setEditingNpc(player, (EntityNPCInterface) deadTarget);
               int size = deadInventory.getContainerSize();
               NoppesUtilServer.openContainerGui(player, EnumGuiType.DeadInventory, (buf) -> {
                  buf.writeInt(size);
                  buf.writeInt(-1);
               });
               event.setCanceled(true);
            }
         }
      } // NPC dead inventory
      if (!event.isCanceled()) {
         if (event.getItemStack().getItem() == CustomItems.nbt_book) {
            Entity target = Util.instance.getLookEntity(player, PlayerData.get(player).game.renderDistance, false);
            if (target != null) { ((ItemNbtBook) event.getItemStack().getItem()).entityEvent(player, target); }
            else { ((ItemNbtBook) event.getItemStack().getItem()).blockEvent(player, event.getPos()); }
            event.setCanceled(true);
         }
         else if (event.getItemStack().getItem() == CustomItems.npcboundary) {
            ((ItemBoundary) event.getItemStack().getItem()).rightClick(event.getItemStack(), player);
            event.setCanceled(true);
         }
         else if (event.getItemStack().getItem() instanceof ISpecBuilder) {
            if (event.getEntity().isCreative()) {
               ((ISpecBuilder) event.getItemStack().getItem()).rightClick(event.getItemStack(), player, event.getPos());
            }
            else { player.sendSystemMessage(Component.translatable("availability.permission")); }
            event.setCanceled(true);
         }
         else if (event.getItemStack().getItem() == CustomItems.teleporter) {
            event.setCanceled(true);
         }
         else {
            PlayerScriptData handler = PlayerData.get(player).scriptData;
            handler.hadInteract = true;
            PlayerEvent.InteractEvent ev = new PlayerEvent.InteractEvent(handler.getPlayer(), 2, Objects.requireNonNull(NpcAPI.Instance()).getIBlock(event.getLevel(), event.getPos()));
            event.setCanceled(EventHooks.onPlayerInteract(handler, ev));
            if (event.getItemStack().getItem() == CustomItems.scripted_item && !event.isCanceled()) {
               ItemScriptedWrapper isw = ItemScripted.GetWrapper(event.getItemStack());
               ItemEvent.InteractEvent eve = new ItemEvent.InteractEvent(isw, handler.getPlayer(), 2, Objects.requireNonNull(NpcAPI.Instance()).getIBlock(event.getLevel(), event.getPos()));
               event.setCanceled(EventHooks.onScriptItemInteract(isw, eve));
            }
         }
      }
      CustomNpcs.debugData.end(player);
   }

   @SubscribeEvent
   public void cnpcEntityInteract(EntityInteract event) {
      CustomNpcs.debugData.start(event.getEntity());
      if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) {
         if (event.getHand() == InteractionHand.MAIN_HAND &&
                 event.getItemStack().getItem() == CustomItems.nbt_book &&
                 event.getTarget() != null &&
                 !event.getTarget().getClass().getName().contains("minecraft") &&
                 !event.getTarget().getClass().getName().contains("noppes")) {
            ClientEventHandler.entityClientEvent(event);
            event.setCanceled(true);
         }
         CustomNpcs.debugData.end(event.getEntity());
         return;
      }
      if (event.getItemStack().getItem() == CustomItems.nbt_book) {
         ((ItemNbtBook) event.getItemStack().getItem()).entityEvent(player, event.getTarget());
         event.setCanceled(true);
      }
      else if (event.getItemStack().getItem() == CustomItems.wand && event.getTarget() instanceof Villager villager) {
         Packets.sendServer(new SPacketContainerOpen(EnumGuiType.MerchantAdd, (buffer) -> buffer.writeInt(villager.getId())));
         event.setCanceled(true);
      }
      else {
         PlayerScriptData handler = PlayerData.get(player).scriptData;
         PlayerEvent.InteractEvent ev = new PlayerEvent.InteractEvent(handler.getPlayer(), 1, Objects.requireNonNull(NpcAPI.Instance()).getIEntity(event.getTarget()));
         event.setCanceled(EventHooks.onPlayerInteract(handler, ev));
         if (event.getItemStack().getItem() == CustomItems.scripted_item && !event.isCanceled()) {
            ItemScriptedWrapper isw = ItemScripted.GetWrapper(event.getItemStack());
            ItemEvent.InteractEvent eve = new ItemEvent.InteractEvent(isw, handler.getPlayer(), 1, Objects.requireNonNull(NpcAPI.Instance()).getIEntity(event.getTarget()));
            event.setCanceled(EventHooks.onScriptItemInteract(isw, eve));
         }
      }
      CustomNpcs.debugData.end(player);
   }

   @SubscribeEvent
   public void cnpcRightClickItem(RightClickItem event) {
      if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) { return; }
      CustomNpcs.debugData.start(event.getEntity());
      if (event.getEntity().isCreative() && event.getEntity().isCrouching() && event.getItemStack().getItem() == CustomItems.scripted_item) {
         NoppesUtilServer.sendOpenGui((ServerPlayer) event.getEntity(), EnumGuiType.ScriptItem, null);
         event.setCanceled(true);
      }
      // New from Unofficial (BetaZavr)
      else if (!(event.getItemStack().getItem() instanceof INPCToolItem)) {
         Entity deadTarget = Util.instance.getLookEntity(player, 4.0d, false);
         if (deadTarget != null && !deadTarget.isAlive() && deadTarget instanceof EntityNPCInterface npc) {
            DataInventory dataInv = npc.inventory;
            Container deadInventory = dataInv.deadLoot;
            if (deadInventory == null && dataInv.deadLoots != null && dataInv.deadLoots.containsKey(player)) { deadInventory = dataInv.deadLoots.get(player); }
            if (deadInventory != null) {
               NoppesUtilServer.setEditingNpc(player, (EntityNPCInterface) deadTarget);
               int size = deadInventory.getContainerSize();
               NoppesUtilServer.openContainerGui(player, EnumGuiType.DeadInventory, (buf) -> {
                  buf.writeInt(size);
                  buf.writeInt(-1);
               });
               event.setCanceled(true);
            }
         }
      } // NPC dead inventory
      if (!event.isCanceled()) {
         if (event.getItemStack().getItem() instanceof ItemNbtBook) {
            PlayerData data = PlayerData.get(player);
            double distance = data.game.renderDistance;
            Entity target = Util.instance.getLookEntity(player, distance, false);
            if (target != null) {
               ((ItemNbtBook) event.getItemStack().getItem()).entityEvent(player, target);
               event.setCanceled(true);
            }
            else {
               Vec3 vec3d = player.getEyePosition(1.0F);
               Vec3 vec3d1 = player.getViewVector(1.0F);
               Vec3 vec3d2 = vec3d.add(vec3d1.x * distance, vec3d1.y * distance, vec3d1.z * distance);
               BlockHitResult result = player.level().clip(new ClipContext(vec3d, vec3d2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
               if (result.getType() == HitResult.Type.BLOCK) {
                  if (!player.level().getBlockState(result.getBlockPos()).isAir()) {
                     ((ItemNbtBook) event.getItemStack().getItem()).blockEvent(player, result.getBlockPos());
                     event.setCanceled(true);
                  }
               }
               else if (!player.getOffhandItem().isEmpty()) {
                  ((ItemNbtBook) event.getItemStack().getItem()).itemEvent(player);
                  event.setCanceled(true);
               }
            }
         } // Empty Click:
         if (event.getItemStack().getItem() instanceof ItemBoundary) {
            ((ItemBoundary) event.getItemStack().getItem()).rightClick(event.getItemStack(), player);
            event.setCanceled(true);
         }
         else {
            PlayerScriptData handler = PlayerData.get(player).scriptData;
            if (handler.hadInteract) { handler.hadInteract = false; }
            else {
               PlayerEvent.InteractEvent ev = new PlayerEvent.InteractEvent(handler.getPlayer(), 0, null);
               event.setCanceled(EventHooks.onPlayerInteract(handler, ev));
               if (event.getItemStack().getItem() == CustomItems.scripted_item && !event.isCanceled()) {
                  ItemScriptedWrapper isw = ItemScripted.GetWrapper(event.getItemStack());
                  ItemEvent.InteractEvent eve = new ItemEvent.InteractEvent(isw, handler.getPlayer(), 0, null);
                  event.setCanceled(EventHooks.onScriptItemInteract(isw, eve));
               }
            }
         }
      }
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcArrowLoose(ArrowLooseEvent event) {
      if (event.getEntity().level().isClientSide || !(event.getLevel() instanceof ServerLevel)) { return; }
      CustomNpcs.debugData.start(event.getEntity());
      PlayerScriptData handler = PlayerData.get(event.getEntity()).scriptData;
      PlayerEvent.RangedLaunchedEvent ev = new PlayerEvent.RangedLaunchedEvent(handler.getPlayer());
      event.setCanceled(EventHooks.onPlayerRanged(handler, ev));
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcBreak(BreakEvent event) {
      if (event.getPlayer().level().isClientSide || !(event.getLevel() instanceof ServerLevel)) { return; }
      CustomNpcs.debugData.start(event.getPlayer());
      PlayerScriptData handler = PlayerData.get(event.getPlayer()).scriptData;
      PlayerEvent.BreakEvent ev = new PlayerEvent.BreakEvent(handler.getPlayer(),
              Objects.requireNonNull(NpcAPI.Instance()).getIBlock((ServerLevel)event.getLevel(), event.getPos()), event.getExpToDrop());
      event.setCanceled(EventHooks.onPlayerBreak(handler, ev));
      event.setExpToDrop(ev.exp);
      CustomNpcs.debugData.end(event.getPlayer());
   }

   @SubscribeEvent
   public void cnpcBlockPlace(BlockEvent.EntityPlaceEvent event) {
      if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof ServerPlayer player)) {
         return;
      }
      CustomNpcs.debugData.start(event.getEntity());
      PlayerScriptData handler = PlayerData.get(player).scriptData;
      if (event.getPlacedBlock().getBlock() instanceof BannerBlock && player.getMainHandItem().getItem() instanceof BannerItem) {
         CompoundTag nbt = new CompoundTag();
         player.getMainHandItem().save(nbt);
         if (nbt.contains("BlockEntityTag", 10) && nbt.getCompound("BlockEntityTag").contains("FactionID", 3)) {
            BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
            if (blockEntity instanceof BannerBlockEntity tile) {
               ((ITileEntityBanner) tile).npcs$setFactionId(nbt.getCompound("BlockEntityTag").getInt("FactionID"));
            }
         }
      }
      PlayerEvent.PlaceEvent ev = new PlayerEvent.PlaceEvent(handler.getPlayer(), BlockWrapper.createNew(((ServerLevel) event.getLevel()).getLevel(), event.getPos(), event.getPlacedBlock()));
      event.setCanceled(EventHooks.onPlayerPlace(handler, ev));
      if (event.isCanceled()) {
         CompoundTag nbtStack = new CompoundTag();
         player.getMainHandItem().save(nbtStack);
         Packets.send(player, new PacketDetectHeldItem(player.getInventory().selected, nbtStack));
      }
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcItemToss(ItemTossEvent event) {
      if (!(event.getPlayer().level() instanceof ServerLevel)) { return; }
      CustomNpcs.debugData.start(event.getPlayer());
      PlayerData data = PlayerData.get(event.getPlayer());
      CustomNPCsScheduler.runTack(() -> {
         for (QuestData qd : data.questData.activeQuests.values()) {
            data.questData.checkQuestCompletion(event.getPlayer(), qd);
         }
      }, 150);
      event.setCanceled(EventHooks.onPlayerToss(data.scriptData, event.getEntity()));
      CustomNpcs.debugData.end(event.getPlayer());
   }

   @SubscribeEvent
   public void cnpcItemPickup(EntityItemPickupEvent event) {
      if (!(event.getEntity().level() instanceof ServerLevel)) { return; }
      CustomNpcs.debugData.start(event.getEntity());
      PlayerData data = PlayerData.get(event.getEntity());
      CustomNPCsScheduler.runTack(() -> {
         for (QuestData qd : data.questData.activeQuests.values()) {
            data.questData.checkQuestCompletion(event.getEntity(), qd);
         }
      }, 150);
      event.setCanceled(EventHooks.onPlayerPickUp(data.scriptData, event.getItem()));
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcPlayerContainerOpen(Open event) {
      if (!(event.getEntity().level() instanceof ServerLevel)) { return; }
      CustomNpcs.debugData.start(event.getEntity());
      EventHooks.onPlayerContainerOpen(PlayerData.get(event.getEntity()).scriptData, event.getContainer());
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcPlayerContainerClose(Close event) {
      if (!(event.getEntity().level() instanceof ServerLevel)) { return; }
      CustomNpcs.debugData.start(event.getEntity());
      EventHooks.onPlayerContainerClose(PlayerData.get(event.getEntity()).scriptData, event.getContainer());
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcLivingDeath(LivingDeathEvent event) {
      if (!(event.getEntity().level() instanceof ServerLevel)) { return; }
      CustomNpcs.debugData.start(event.getEntity());
      Entity source = NoppesUtilServer.getDamageSource(event.getSource());
      PlayerScriptData handler;
      if (event.getEntity() instanceof Player) {
         handler = PlayerData.get((Player)event.getEntity()).scriptData;
         EventHooks.onPlayerDeath(handler, event.getSource(), source);
      }
      if (source instanceof Player) {
         handler = PlayerData.get((Player)source).scriptData;
         EventHooks.onPlayerKills(handler, event.getEntity());
      }
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcLivingHurt(LivingHurtEvent event) {
      if (!(event.getEntity().level() instanceof ServerLevel)) { return; }
      CustomNpcs.debugData.start(event.getEntity());
      Entity source = NoppesUtilServer.getDamageSource(event.getSource());
      PlayerScriptData handler;
      if (event.getEntity() instanceof Player) {
         handler = PlayerData.get((Player)event.getEntity()).scriptData;
         PlayerEvent.DamagedEvent pevent = new PlayerEvent.DamagedEvent(handler.getPlayer(), source, event.getAmount(), event.getSource());
         boolean cancel = EventHooks.onPlayerDamaged(handler, pevent);
         event.setCanceled(cancel);
         if (pevent.clearTarget) {
            event.setCanceled(true);
            event.setAmount(0.0f);
         }
         else { event.setAmount(pevent.damage); }
      }
      if (source instanceof Player) {
         handler = PlayerData.get((Player)source).scriptData;
         PlayerEvent.DamagedEntityEvent pevent = new PlayerEvent.DamagedEntityEvent(handler.getPlayer(), event.getEntity(),
                 event.getAmount(), event.getSource());
         event.setCanceled(EventHooks.onPlayerDamagedEntity(handler, pevent));
         event.setAmount(pevent.damage);
      }
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent(priority = EventPriority.LOW)
   public void cnpcLivingAttack(LivingAttackEvent event) {
      if (!(event.getEntity().level() instanceof ServerLevel)) { return; }
      CustomNpcs.debugData.start(event.getEntity());
      Entity source = NoppesUtilServer.getDamageSource(event.getSource());
      Resistances.add(event.getSource() != null ? event.getSource().getMsgId() : "null");
      ((ILivingEntityMixin) event.getEntity()).npcs$setCurrentDamageSource(event.getSource());
      if (source instanceof Player player) {
         PlayerData data = PlayerData.get(player);
         PlayerScriptData handler = data.scriptData;
         ItemStack item = player.getMainHandItem();
         IEntity<?> target = Objects.requireNonNull(NpcAPI.Instance()).getIEntity(event.getEntity());
         IDamageSource damageSource = Objects.requireNonNull(NpcAPI.Instance()).getIDamageSource(event.getSource());
         PlayerEvent.AttackEvent ev = new PlayerEvent.AttackEvent(handler.getPlayer(), target, damageSource);
         event.setCanceled(EventHooks.onPlayerAttack(handler, ev));
         if (event.isCanceled() || ev.isCanceled()) { ((ILivingAttackEventMixin) event).setAmount(0.0f); }
         if (item.getItem() == CustomItems.scripted_item && !event.isCanceled()) {
            ItemScriptedWrapper isw = ItemScripted.GetWrapper(item);
            ItemEvent.AttackEvent eve = new ItemEvent.AttackEvent(isw, handler.getPlayer(), 1, target);
            eve.setCanceled(event.isCanceled());
            event.setCanceled(EventHooks.onScriptItemAttack(isw, eve));
         }
         if (!event.isCanceled()) {
            for (EntityNPCInterface npc : data.game.getMercenaries()) {
               if (!npc.isAttacking()) { npc.onAttack(event.getEntity()); }
            }
         }
      }
      if (event.getEntity() instanceof Player player && source instanceof LivingEntity entity && !event.isCanceled()) {
         PlayerData data = PlayerData.get(player);
         for (EntityNPCInterface npc : data.game.getMercenaries()) {
            if (!npc.isAttacking()) { npc.onAttack(entity); }
         }
      }
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcPlayerLoggedIn(PlayerLoggedInEvent event) {
      CustomNpcs.debugData.start(event.getEntity());
      CommonUtil.sendScriptErrorsTo(event.getEntity());
      if (!event.getEntity().level().isClientSide()) {
         ServerPlayer player = (ServerPlayer) event.getEntity();
         if (!ScriptController.Instance.getErrored().isEmpty()) {
            CustomNPCsScheduler.runTack(() -> player.sendSystemMessage(Component.translatable("command.script.logs.view")), 2500);
         }
         PlayerData data = PlayerData.get(player);
         EventHooks.onPlayerLogin(data.scriptData);
         PlayerSkinController.getInstance().logged(player);
         MinecraftServer server = event.getEntity().getServer();
         if (server != null) {
            for (ServerLevel level : server.getAllLevels()) {
               ServerScoreboard board = level.getScoreboard();
               for (String objective : Availability.scores) {
                  Objective so = board.getObjective(objective);
                  if (so != null) {
                     if (board.getObjectiveDisplaySlotCount(so) == 0) { player.connection.send(new ClientboundSetObjectivePacket(so, 0)); }
                     Score sco = board.getOrCreatePlayerScore(player.getScoreboardName(), so);
                     player.connection.send(new ClientboundSetScorePacket(ServerScoreboard.Method.CHANGE, so.getName(), sco.getOwner(), sco.getScore()));
                  }
               }
            }
         }
         player.inventoryMenu.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(@NotNull AbstractContainerMenu container, int slotInd, @NotNull ItemStack stack) {
               if (!player.level().isClientSide) {
                  PlayerQuestData playerdata = PlayerData.get(player).questData;
                  CustomNPCsScheduler.runTack(() -> {
                     for (QuestData data : playerdata.activeQuests.values()) {
                        for (QuestObjective obj : data.quest.getObjectives(player)) {
                           if (obj.getEnumType() != EnumQuestTask.ITEM) { continue; }
                           playerdata.checkQuestCompletion(player, data);
                        }
                     }
                  });
               }
            }
            @Override
            public void dataChanged(@NotNull AbstractContainerMenu container, int varToUpdate, int newValue) { }
         });
         SyncController.syncPlayer((ServerPlayer)event.getEntity());
         if (data.game.logPos != null) { // protection against remote measurements
            SPacketDimensionTeleport.teleportPlayer(player, data.game.logPosDimID, data.game.logPos[0], data.game.logPos[1],
                    data.game.logPos[2], player.getYRot(), player.getXRot());
         }
         data.game.dimID = player.level().dimension();
      }
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent
   public void cnpcPlayerLoggedOut(PlayerLoggedOutEvent event) {
      if (!(event.getEntity().level() instanceof ServerLevel)) { return; }
      CustomNpcs.debugData.start(event.getEntity());
      ServerPlayer player = (ServerPlayer) event.getEntity();
      PlayerData data = PlayerData.get(player);
      EventHooks.onPlayerLogout(data.scriptData);
      if (data.bankData.lastBank != null) {
         data.bankData.lastBank.save();
         data.bankData.lastBank = null;
      }
      if (player.level().dimension().location().getNamespace().equals(CustomNpcs.MODID)) { // protection against remote measurements
         data.game.logPos = new double[] { player.getX(), player.getY(), player.getZ() };
         data.game.logPosDimID = player.level().dimension();
         ServerLevel level = Objects.requireNonNull(player.getServer()).getLevel(Level.OVERWORLD);
         if (level != null) {
            BlockPos coords = NoppesUtilServer.getSafeTpPos(level, level.getSharedSpawnPos(), 1, 255);
            double x = coords.getX();
            double y = coords.getY();
            double z = coords.getZ();
            SPacketDimensionTeleport.teleportPlayer(player, level.dimension(), x, y, z, player.getYRot(), player.getXRot());
         }
      } else {
         data.game.logPos = null;
         data.game.logPosDimID = Level.OVERWORLD;
      }
      data.save(false);
      CustomNpcs.debugData.end(event.getEntity());
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void cnpcServerChat(ServerChatEvent event) {
      if (event.getPlayer() == EntityNPCInterface.ChatEventPlayer) { return; }
      CustomNpcs.debugData.start(event.getPlayer());
      if (event.getPlayer().level().isClientSide()) { KeyController.getInstance().save(); }
      else {
         ServerPlayer player = event.getPlayer();
         CustomNpcs.debugData.start(player);
         PlayerScriptData handler = PlayerData.get(player).scriptData;
         String message = event.getMessage().getString();
         PlayerEvent.ChatEvent ev = new PlayerEvent.ChatEvent(handler.getPlayer(), event.getMessage().getString());
         EventHooks.onPlayerChat(handler, ev);
         event.setCanceled(ev.isCanceled());
         if (!event.isCanceled()) {
            if (!message.equals(ev.message)) { event.setMessage(Component.empty().append(ForgeHooks.newChatWithLinks(ev.message))); }
            Packets.sendNearby(player.level(), player.blockPosition(), 32,
                    new PacketChatBubble(player.getId(), Component.translatable(ev.message), false));
         }
      }
      CustomNpcs.debugData.end(event.getPlayer());
   }

   private Set<Class<?>> getClasses(String packageName) {
      packageName = packageName.replace('.', '/');
      HashSet<String> urls = new HashSet<>();
      try {
         Module module = EntityEvent.class.getModule();
         Enumeration<URL> resources = module.getClassLoader().getResources(packageName);

         while(resources.hasMoreElements()) {
            URL url = resources.nextElement();
            String path = url.getPath();
            int i = path.indexOf(".jar");
            if (i > 0) {
               urls.add(path.substring(0, i + 4));
            }
         }
      } catch (Throwable ignored) {}
      String path;
      try {
         Enumeration<URL> resources = ClassLoader.getSystemClassLoader().getResources(packageName);
         while(resources.hasMoreElements()) {
            URL url = resources.nextElement();
            path = url.getPath();
            int i = path.indexOf(".jar");
            if (i > 0) { urls.add(path.substring(0, i + 4)); }
         }
      } catch (Throwable ignored) { }
      HashSet<Class<?>> classes = new HashSet<>();
      for (String url : urls) {
         path = url;
         try {
            JarFile file = new JarFile(new File(path));
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
               JarEntry entry = entries.nextElement();
               if (!entry.isDirectory() && entry.getName().startsWith(packageName)) {
                  String name = entry.getName().replace('/', '.');
                  try {
                     Class<?> c = Class.forName(name.substring(0, name.length() - 6));
                     if (Event.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers()) && Modifier.isPublic(c.getModifiers())) {
                        if (c.getDeclaredClasses().length > 0) {
                           classes.addAll(Arrays.asList(c.getDeclaredClasses()));
                        } else {
                           classes.add(c);
                        }
                     }

                     classes.add(c);
                  }
                  catch (Throwable ignored) {}
               }
            }
            file.close();
         }
         catch (Exception ignored) { }
      }
      return classes;
   }

   public ScriptPlayerEventHandler registerForgeEvents() {
      ForgeEventHandler.eventNames.clear();
      ForgeEventHandler handler = new ForgeEventHandler();
      try {
         Method m = handler.getClass().getMethod("forgeEntity", Event.class);
         Iterator<Class<?>> iteratorClasses = getClasses("net.minecraftforge.event.").iterator();
         Class<?> c;
         Dist side = Util.instance.getSide();
         while(iteratorClasses.hasNext()) {
            c = iteratorClasses.next();
            try {
               if (!GenericEvent.class.isAssignableFrom(c) && !EntityConstructing.class.isAssignableFrom(c) &&
                       !CreateSpawnPosition.class.isAssignableFrom(c) && !RenderTickEvent.class.isAssignableFrom(c) &&
                       !ClientTickEvent.class.isAssignableFrom(c) && !ClientCustomPayloadEvent.class.isAssignableFrom(c) && !ItemTooltipEvent.class.isAssignableFrom(c)) {
                  String eventName = ForgeEventHandler.getEventName(c);
                  String className = c.getName().toLowerCase();
                  if (side == Dist.DEDICATED_SERVER) {
                     if (!className.contains("client") &&
                             !className.contains("render") &&
                             !className.contains("itemtooltipevent") &&
                             !ForgeEventHandler.eventNames.containsKey(c)) {
                        ((IEventBusMixin) MinecraftForge.EVENT_BUS).invokeRegister(c, handler, m);
                        ForgeEventHandler.eventNames.put(c, eventName);
                     }
                  }
                  else if (!ForgeEventHandler.clientEventNames.containsKey(c)) {
                     ((IEventBusMixin) MinecraftForge.EVENT_BUS).invokeRegister(c, handler, m);
                     ForgeEventHandler.eventNames.put(c, eventName);
                     ForgeEventHandler.clientEventNames.put(c, eventName);
                  }
               }
            } catch (Throwable ignored) { }
         }
         if (PixelmonHelper.Enabled) {
            try {
               iteratorClasses = getClasses("com.pixelmonmod.pixelmon.api.events.").iterator();
               while(iteratorClasses.hasNext()) {
                  c = iteratorClasses.next();
                  ((IEventBusMixin) PixelmonHelper.EVENT_BUS).invokeRegister(c, handler, m);
                  ForgeEventHandler.eventNames.put(c, ForgeEventHandler.getEventName(c));
               }
            } catch (Throwable tPX) {
               LogWriter.except(tPX);
            }
         }
      } catch (Throwable t) {
         LogWriter.except(t);
      }

      return this;
   }

   // New from Unofficial (BetaZavr)
   @SubscribeEvent
   public void cnpcItemCraftedEvent(net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent event) {
      if (!(event.getEntity().level() instanceof ServerLevel)) { return; }
      ServerPlayer player = (ServerPlayer) event.getEntity();
      CustomNpcs.debugData.start(player);
      PlayerEvent.ItemCrafted craftEvent = new PlayerEvent.ItemCrafted(PlayerData.get(player).scriptData.getPlayer(),
              Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(event.getCrafting()),
              Objects.requireNonNull(NpcAPI.Instance()).getIContainer(event.getInventory()));
      EventHooks.onEvent(PlayerData.get(player).scriptData, EnumScriptType.ITEM_CRAFTED, craftEvent);
      if (!craftEvent.crafting.isEmpty()) { CustomNPCsScheduler.runTack(() -> doCraftQuest(player, craftEvent.crafting.getMCItemStack())); }
      CustomNpcs.debugData.end(player);
   }

   @SubscribeEvent
   public void cnpcItemFishedEvent(ItemFishedEvent event) {
      if (!(event.getEntity().level() instanceof ServerLevel)) { return; }
      ServerPlayer player = (ServerPlayer) event.getEntity();
      CustomNpcs.debugData.start(player);
      PlayerEvent.ItemFished fishedEvent = new PlayerEvent.ItemFished(PlayerData.get(player).scriptData.getPlayer(),
              event.getDrops(),
              event.getRodDamage());
      EventHooks.onEvent(PlayerData.get(player).scriptData, EnumScriptType.ITEM_FISHED, fishedEvent);
      NonNullList<ItemStack> drops = event.getDrops();
      for (int i = 0; i < drops.size() && i < fishedEvent.stacks.length; i++) {
         IItemStack iStack = fishedEvent.stacks[i];
         if (iStack == null) { drops.set(i, ItemStack.EMPTY); }
         else { drops.set(i, iStack.getMCItemStack()); }
      }
      CustomNpcs.debugData.end(player);
   }

   @SubscribeEvent
   @SuppressWarnings("all")
   public void cnpcLivingJumpEvent(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
      if (!(event.getEntity() instanceof Player player) ) { return; }
      if (player instanceof ServerPlayer sPlayer) {
         try {
         }
         catch (Exception e) { LogWriter.error(e); }
      }
      else {
         try {
         }
         catch (Exception e) { LogWriter.error(e); }
      }
   }

}
