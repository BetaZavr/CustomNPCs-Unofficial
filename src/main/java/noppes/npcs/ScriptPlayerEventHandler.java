package noppes.npcs;

import java.lang.reflect.*;
import java.util.*;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.BlockBanner;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.network.chat.Component;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.*;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.api.mixin.entity.IEntityLivingBaseIMixin;
import noppes.npcs.api.mixin.entity.player.IEntityPlayerMixin;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.api.mixin.tileentity.ITileEntityBanner;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.mixin.minecraftforge.event.entity.living.ILivingAttackEventMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.packets.client.PacketChatBubble;
import noppes.npcs.packets.client.PacketDetectHeldItem;
import noppes.npcs.packets.client.PacketItemUpdate;
import noppes.npcs.packets.server.SPacketContainerOpen;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.util.CustomNPCsScheduler;

import com.google.common.reflect.ClassPath;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketScoreboardObjective;
import net.minecraft.network.play.server.SPacketUpdateScore;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.world.GetCollisionBoxesEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.ItemEvent;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.api.item.ISpecBuilder;
import noppes.npcs.api.wrapper.BlockWrapper;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.client.ClientEventHandler;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.Resistances;
import noppes.npcs.items.ItemBoundary;
import noppes.npcs.items.ItemNbtBook;
import noppes.npcs.items.ItemScripted;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;

public class ScriptPlayerEventHandler {

	/**
	 * Using quotes in JavaScripts for any Forge event requires binding to these events;
	 * The method written by the Noppes team is not applicable for all cases (sometimes events simply did not work in JS);
	 * And there is also a conflict with mods written with the "Cleanroom" base;
	 * A manual method for implementing classes is written:
	 */
	private static final String[] pathsToForgeEventClasses = new String[] {
			"net.minecraftforge.event.AnvilUpdateEvent",
			"net.minecraftforge.event.AttachCapabilitiesEvent",
			"net.minecraftforge.event.CommandEvent",
			"net.minecraftforge.event.DifficultyChangeEvent",
			"net.minecraftforge.event.GameRuleChangeEvent",
			"net.minecraftforge.event.LootTableLoadEvent",
			"net.minecraftforge.event.RegistryEvent",
			"net.minecraftforge.event.ServerChatEvent",
			"net.minecraftforge.event.brewing.PlayerBrewedPotionEvent",
			"net.minecraftforge.event.brewing.PotionBrewEvent",
			"net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent",
			"net.minecraftforge.event.entity.EntityJoinWorldEvent",
			"net.minecraftforge.event.entity.EntityMobGriefingEvent",
			"net.minecraftforge.event.entity.EntityMountEvent",
			"net.minecraftforge.event.entity.EntityStruckByLightningEvent",
			"net.minecraftforge.event.entity.EntityTravelToDimensionEvent",
			"net.minecraftforge.event.entity.PlaySoundAtEntityEvent",
			"net.minecraftforge.event.entity.ProjectileImpactEvent",
			"net.minecraftforge.event.entity.ThrowableImpactEvent",
			"net.minecraftforge.event.entity.item.ItemEvent",
			"net.minecraftforge.event.entity.item.ItemExpireEvent",
			"net.minecraftforge.event.entity.item.ItemTossEvent",
			"net.minecraftforge.event.entity.living.AnimalTameEvent",
			"net.minecraftforge.event.entity.living.BabyEntitySpawnEvent",
			"net.minecraftforge.event.entity.living.EnderTeleportEvent",
			"net.minecraftforge.event.entity.living.LivingAttackEvent",
			"net.minecraftforge.event.entity.living.LivingDamageEvent",
			"net.minecraftforge.event.entity.living.LivingDeathEvent",
			"net.minecraftforge.event.entity.living.LivingDestroyBlockEvent",
			"net.minecraftforge.event.entity.living.LivingDropsEvent",
			"net.minecraftforge.event.entity.living.LivingEntityUseItemEvent",
			"net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent",
			"net.minecraftforge.event.entity.living.LivingEvent",
			"net.minecraftforge.event.entity.living.LivingExperienceDropEvent",
			"net.minecraftforge.event.entity.living.LivingFallEvent",
			"net.minecraftforge.event.entity.living.LivingHealEvent",
			"net.minecraftforge.event.entity.living.LivingHurtEvent",
			"net.minecraftforge.event.entity.living.LivingKnockBackEvent",
			"net.minecraftforge.event.entity.living.LivingPackSizeEvent",
			"net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent",
			"net.minecraftforge.event.entity.living.LivingSpawnEvent",
			"net.minecraftforge.event.entity.living.LootingLevelEvent",
			"net.minecraftforge.event.entity.living.PotionColorCalculationEvent",
			"net.minecraftforge.event.entity.living.ZombieEvent",
			"net.minecraftforge.event.entity.minecart.MinecartCollisionEvent",
			"net.minecraftforge.event.entity.minecart.MinecartEvent",
			"net.minecraftforge.event.entity.minecart.MinecartInteractEvent",
			"net.minecraftforge.event.entity.minecart.MinecartUpdateEvent",
			"net.minecraftforge.event.entity.player.AdvancementEvent",
			"net.minecraftforge.event.entity.player.AnvilRepairEvent",
			"net.minecraftforge.event.entity.player.ArrowLooseEvent",
			"net.minecraftforge.event.entity.player.ArrowNockEvent",
			"net.minecraftforge.event.entity.player.AttackEntityEvent",
			"net.minecraftforge.event.entity.player.BonemealEvent",
			"net.minecraftforge.event.entity.player.CriticalHitEvent",
			"net.minecraftforge.event.entity.player.EntityItemPickupEvent",
			"net.minecraftforge.event.entity.player.FillBucketEvent",
			"net.minecraftforge.event.entity.player.ItemFishedEvent",
			"net.minecraftforge.event.entity.player.PlayerContainerEvent",
			"net.minecraftforge.event.entity.player.PlayerDestroyItemEvent",
			"net.minecraftforge.event.entity.player.PlayerDropsEvent",
			"net.minecraftforge.event.entity.player.PlayerEvent",
			"net.minecraftforge.event.entity.player.PlayerFlyableFallEvent",
			"net.minecraftforge.event.entity.player.PlayerInteractEvent",
			"net.minecraftforge.event.entity.player.PlayerPickupXpEvent",
			"net.minecraftforge.event.entity.player.PlayerSetSpawnEvent",
			"net.minecraftforge.event.entity.player.PlayerSleepInBedEvent",
			"net.minecraftforge.event.entity.player.PlayerWakeUpEvent",
			"net.minecraftforge.event.entity.player.SleepingLocationCheckEvent",
			"net.minecraftforge.event.entity.player.SleepingTimeCheckEvent",
			"net.minecraftforge.event.entity.player.UseHoeEvent",
			"net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent",
			"net.minecraftforge.event.world.BlockEvent",
			"net.minecraftforge.event.world.ChunkDataEvent",
			"net.minecraftforge.event.world.ChunkEvent",
			"net.minecraftforge.event.world.ChunkWatchEvent",
			"net.minecraftforge.event.world.ExplosionEvent",
			"net.minecraftforge.event.world.NoteBlockEvent",
			"net.minecraftforge.event.world.WorldEvent",
			"net.minecraftforge.event.entity.EntityEvent",
			"net.minecraftforge.fml.common.gameevent.InputEvent",
			"net.minecraftforge.fml.common.gameevent.PlayerEvent",
			"net.minecraftforge.fml.common.gameevent.TickEvent",
			"net.minecraftforge.fluids.FluidEvent",
			"net.minecraftforge.client.event.sound.PlaySoundEvent",
			"net.minecraftforge.client.event.sound.PlaySoundSourceEvent",
			"net.minecraftforge.client.event.sound.PlayStreamingSourceEvent",
			"net.minecraftforge.client.event.sound.SoundEvent",
			"net.minecraftforge.client.event.sound.SoundLoadEvent",
			"net.minecraftforge.client.event.sound.SoundSetupEvent",
			"net.minecraftforge.client.event.ClientChatEvent",
			"net.minecraftforge.client.event.ClientChatReceivedEvent",
			"net.minecraftforge.client.event.ColorHandlerEvent",
			"net.minecraftforge.client.event.DrawBlockHighlightEvent",
			"net.minecraftforge.client.event.EntityViewRenderEvent",
			"net.minecraftforge.client.event.FOVUpdateEvent",
			"net.minecraftforge.client.event.GuiContainerEvent",
			"net.minecraftforge.client.event.GuiOpenEvent",
			"net.minecraftforge.client.event.GuiScreenEvent",
			"net.minecraftforge.client.event.InputUpdateEvent",
			"net.minecraftforge.client.event.ModelBakeEvent",
			"net.minecraftforge.client.event.MouseEvent",
			"net.minecraftforge.client.event.PlayerSPPushOutOfBlocksEvent",
			"net.minecraftforge.client.event.RenderBlockOverlayEvent",
			"net.minecraftforge.client.event.RenderGameOverlayEvent",
			"net.minecraftforge.client.event.RenderHandEvent",
			"net.minecraftforge.client.event.RenderItemInFrameEvent",
			"net.minecraftforge.client.event.RenderLivingEvent",
			"net.minecraftforge.client.event.RenderPlayerEvent",
			"net.minecraftforge.client.event.RenderSpecificHandEvent",
			"net.minecraftforge.client.event.RenderTooltipEvent",
			"net.minecraftforge.client.event.RenderWorldLastEvent",
			"net.minecraftforge.client.event.ScreenshotEvent",
			"net.minecraftforge.client.event.TextureStitchEvent"
	};

	private void doCraftQuest(EntityPlayerMP player, ItemStack crafting) {
		PlayerData pdata = PlayerData.get(player);
		PlayerQuestData playerdata = pdata.questData;
		for (QuestData data : playerdata.activeQuests.values()) {
			if (data.quest.step == 2 && data.quest.questInterface.isCompleted(player)) { continue; }
			boolean bo = data.quest.step == 1;
			for (QuestObjective obj : data.quest.getObjectives((IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player))) {
				if (data.quest.step == 1 && !bo) { break; }
				bo = obj.isCompleted();
				if (obj.getEnumType() != EnumQuestTask.CRAFT) { continue; }
				int size = 0;
				if (!NoppesUtilServer.isItemStackNull(crafting) && NoppesUtilPlayer.compareItems(
						obj.getItem().getMCItemStack(), crafting, obj.isIgnoreDamage(), obj.isItemIgnoreNBT())) {
					size = crafting.getCount();
				}
				if (size == 0) { continue; }
				HashMap<ItemStack, Integer> crafted = obj.getCrafted(data);
				int amount = 0;
				ItemStack key = obj.getItem().getMCItemStack();
				for (ItemStack inData : crafted.keySet()) {
					if (NoppesUtilPlayer.compareItems(obj.getItem().getMCItemStack(), inData, obj.isIgnoreDamage(),
							obj.isItemIgnoreNBT())) {
						amount = crafted.get(inData);
						key = inData;
						break;
					}
				}
				if (amount >= obj.getMaxProgress()) { continue; }
				if (amount + size > obj.getMaxProgress()) { size = obj.getMaxProgress() - amount; }
				amount += size;
				crafted.put(key, amount);
				obj.setCrafted(data, crafted);
				if (data.quest.showProgressInWindow) {
					NBTTagCompound compound = new NBTTagCompound();
					compound.setInteger("QuestID", data.quest.id);
					compound.setString("Type", "craft");
					compound.setIntArray("Progress", new int[] { amount, obj.getMaxProgress() });
					compound.setTag("Item", crafting.writeToNBT(new NBTTagCompound()));
					compound.setInteger("MessageType", 0);
					Packets.send(player, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
				}
				if (data.quest.showProgressInChat) {
					if (amount >= obj.getMaxProgress()) {
						player.sendMessage(new TextComponentTranslation("quest.message.craft.1",
								crafting.getDisplayName(), data.quest.getTitle()));
					} else {
						player.sendMessage(
								new TextComponentTranslation("quest.message.craft.0", crafting.getDisplayName(),
										"" + amount, "" + obj.getMaxProgress(), data.quest.getTitle()));
					}
				}
				pdata.updateClient = true;
				if (obj.isItemLeave()) {
					boolean ch = player.inventory.getItemStack().isItemEqual(crafting);
					crafting.splitStack(size);
					player.openContainer.detectAndSendChanges();
					if (ch) {
						NBTTagCompound nbtStack = new NBTTagCompound();
						player.inventory.getItemStack().writeToNBT(nbtStack);
						Packets.send(player, new PacketDetectHeldItem(-1, nbtStack));
					}
				}
				playerdata.checkQuestCompletion(player, data);
				playerdata.updateClient = true;
			}
		}
	}

	@SubscribeEvent
	public void cnpcServerTick(TickEvent.PlayerTickEvent event) {
		if (event.side != Side.SERVER || event.phase != TickEvent.Phase.START) { return; }
		CustomNpcs.debugData.start(event.player);
		EntityPlayerMP player = (EntityPlayerMP) event.player;
		PlayerData data = PlayerData.get(player);
		if (player.ticksExisted % 10 == 0) {
			EventHooks.onPlayerTick(new PlayerEvent.UpdateEvent(data.getIPlayer()));
			for (int i = 0; i < player.inventory.getSizeInventory(); ++i) {
				ItemStack item = player.inventory.getStackInSlot(i);
				if (!item.isEmpty() && item.getItem() == CustomItems.scripter_item) {
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
			EventHooks.onPlayerLevelUp(new PlayerEvent.LevelUpEvent(data.getIPlayer(), data.playerLevel - player.experienceLevel));
			data.playerLevel = player.experienceLevel;
		}
		data.timers.update();
		// New from Unofficial (BetaZavr)
		int dimId = event.player.world.provider.getDimension();
		if (data.game.dimID != dimId) {
			if (CustomNpcs.SetHomeDimension) {
				player.setSpawnDimension(dimId);
				player.setSpawnPoint(player.getPosition(), true);
				player.setSpawnChunk(player.getPosition(), true, dimId);
				player.bedLocation = player.getPosition();
				((IEntityPlayerMixin) player).npcs$setSpawnPos(player.getPosition());
			}
			data.game.dimID = event.player.world.provider.getDimension();
		}
		CustomNpcs.debugData.end(event.player);
	}

	@SubscribeEvent
	public void cnpcLeftClick(PlayerInteractEvent.LeftClickBlock event) {
		if (!(event.getEntityPlayer() instanceof EntityPlayerMP) && event.getHand() != EnumHand.MAIN_HAND || event.getWorld().isRemote) { return; }
		EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
		CustomNpcs.debugData.start(player);
		if (event.getItemStack().getItem() == CustomItems.npcboundary) {
			((ItemBoundary) event.getItemStack().getItem()).leftClick(event.getItemStack(), player);
			event.setCanceled(true);
		}
		else if (event.getItemStack().getItem() instanceof ISpecBuilder) {
			if (player.isCreative()) {
				((ISpecBuilder) event.getItemStack().getItem()).leftClick(event.getItemStack(), player, event.getPos());
			}
			else { player.sendMessage(Component.translatable("availability.permission").getParent()); }
			event.setCanceled(true);
		}
		else if (event.getItemStack().getItem() == CustomItems.teleporter) { event.setCanceled(true); }
		else {
			IPlayer<?> iPlayer = PlayerData.get(player).getIPlayer();
			PlayerEvent.AttackEvent ev = new PlayerEvent.AttackEvent(iPlayer, 2, Objects.requireNonNull(NpcAPI.Instance()).getIBlock(event.getWorld(), event.getPos()));
			event.setCanceled(EventHooks.onPlayerAttack( ev));
			if (event.getItemStack().getItem() == CustomItems.scripter_item && !event.isCanceled()) {
				ItemScriptedWrapper isw = ItemScripted.GetWrapper(event.getItemStack());
				ItemEvent.AttackEvent itemEvent = new ItemEvent.AttackEvent(isw, iPlayer, 2, Objects.requireNonNull(NpcAPI.Instance()).getIBlock(event.getWorld(), event.getPos()));
				itemEvent.setCanceled(event.isCanceled());
				event.setCanceled(EventHooks.onScriptItemAttack(isw, itemEvent));
			}
		}
		CustomNpcs.debugData.end(player);
	}

	@SubscribeEvent
	public void cnpcRightClick(PlayerInteractEvent.RightClickBlock event) {
		if (event.getHand() != EnumHand.MAIN_HAND || event.getWorld().isRemote) { return; }
		EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
		CustomNpcs.debugData.start(player);
		if (!(event.getItemStack().getItem() instanceof INPCToolItem)) {
			Entity deadTarget = Util.instance.getLookEntity(player, 4.0d, false);
			if (deadTarget != null && !deadTarget.isEntityAlive() && deadTarget instanceof EntityNPCInterface) {
				DataInventory dataInv = ((EntityNPCInterface) deadTarget).inventory;
				IInventory deadInventory = dataInv.deadLoot;
				if (deadInventory == null && dataInv.deadLoots != null && dataInv.deadLoots.containsKey(player)) { deadInventory = dataInv.deadLoots.get(player); }
				if (deadInventory != null) {
					NoppesUtilServer.setEditingNpc(player, (EntityNPCInterface) deadTarget);
					int size = deadInventory.getSizeInventory();
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
				if (player.isCreative()) {
					((ISpecBuilder) event.getItemStack().getItem()).rightClick(event.getItemStack(), player, event.getPos());
				}
				else { player.sendMessage(Component.translatable("availability.permission").getParent()); }
				event.setCanceled(true);
			}
			else if (event.getItemStack().getItem() == CustomItems.teleporter) {
				event.setCanceled(true);
			}
			else {
				IPlayer<?> iPlayer = PlayerData.get(player).getIPlayer();
				event.setCanceled(EventHooks.onPlayerInteract(true, new PlayerEvent.InteractEvent(iPlayer,
						2, Objects.requireNonNull(NpcAPI.Instance()).getIBlock(event.getWorld(), event.getPos()))));
				if (event.getItemStack().getItem() == CustomItems.scripter_item && !event.isCanceled()) {
					ItemScriptedWrapper isw = ItemScripted.GetWrapper(event.getItemStack());
					event.setCanceled(EventHooks.onScriptItemInteract(isw, new ItemEvent.InteractEvent(isw, iPlayer,
							2, Objects.requireNonNull(NpcAPI.Instance()).getIBlock(event.getWorld(), event.getPos()))));
				}
			}
		}
		CustomNpcs.debugData.end(player);
	}

	@SubscribeEvent
	public void cnpcEntityInteract(PlayerInteractEvent.EntityInteract event) {
		CustomNpcs.debugData.start(event.getEntityPlayer());
		if (event.getWorld().isRemote || event.getHand() != EnumHand.MAIN_HAND) {
			if (event.getHand() == EnumHand.MAIN_HAND &&
					event.getItemStack().getItem() == CustomItems.nbt_book &&
					event.getTarget() != null &&
					!event.getTarget().getClass().getName().contains("minecraft") &&
					!event.getTarget().getClass().getName().contains("noppes")) {
				ClientEventHandler.entityClientEvent(event);
				event.setCanceled(true);
			}
			CustomNpcs.debugData.end(event.getEntityPlayer());
			return;
		}
		EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
		if (event.getItemStack().getItem() == CustomItems.nbt_book) {
			((ItemNbtBook) event.getItemStack().getItem()).entityEvent(player, event.getTarget());
			event.setCanceled(true);
		}
		else if (event.getItemStack().getItem() == CustomItems.wand && event.getTarget() instanceof EntityVillager) {
			Packets.sendServer(new SPacketContainerOpen(EnumGuiType.MerchantAdd, (buffer) -> buffer.writeInt(event.getTarget().getEntityId())));
			event.setCanceled(true);
		}
		else {
			IPlayer<?> iPlayer = PlayerData.get(player).getIPlayer();
			event.setCanceled(EventHooks.onPlayerInteract(true, new PlayerEvent.InteractEvent(iPlayer, 1,
					Objects.requireNonNull(NpcAPI.Instance()).getIEntity(event.getTarget()))));
			if (event.getItemStack().getItem() == CustomItems.scripter_item && !event.isCanceled()) {
				ItemScriptedWrapper isw = ItemScripted.GetWrapper(event.getItemStack());
				event.setCanceled(EventHooks.onScriptItemInteract(isw, new ItemEvent.InteractEvent(isw, iPlayer,
						1, Objects.requireNonNull(NpcAPI.Instance()).getIEntity(event.getTarget()))));
			}
		}
		CustomNpcs.debugData.end(player);
	}

	@SubscribeEvent
	public void cnpcRightClickItem(PlayerInteractEvent.RightClickItem event) {
		if (!(event.getEntityPlayer() instanceof EntityPlayerMP) ||
				event.getWorld().isRemote ||
				event.getHand() != EnumHand.MAIN_HAND) { return; }
		EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
		CustomNpcs.debugData.start(player);
		if (event.getItemStack().getItem() == CustomItems.scripter_item &&
				player.isCreative() && player.isSneaking() &&
				(!CustomNpcs.OpsOnly || CommonUtil.isOp(player)) &&
				CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_SCRIPTER)) {
			NoppesUtilServer.sendOpenGui(player, EnumGuiType.ScriptItem, null);
			event.setCanceled(true);
		}
		// New from Unofficial (BetaZavr)
		else if (!(event.getItemStack().getItem() instanceof INPCToolItem)) {
			Entity deadTarget = Util.instance.getLookEntity(player, 4.0d, false);
			if (deadTarget != null && !deadTarget.isEntityAlive() && deadTarget instanceof EntityNPCInterface) {
				DataInventory dataInv = ((EntityNPCInterface) deadTarget).inventory;
				IInventory deadInventory = dataInv.deadLoot;
				if (deadInventory == null && dataInv.deadLoots != null && dataInv.deadLoots.containsKey(player)) { deadInventory = dataInv.deadLoots.get(player); }
				if (deadInventory != null) {
					NoppesUtilServer.setEditingNpc(player, (EntityNPCInterface) deadTarget);
					int size = deadInventory.getSizeInventory();
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
				double d0 = data.game.renderDistance;
				Entity target = Util.instance.getLookEntity(player, d0, false);
				if (target != null) {
					((ItemNbtBook) event.getItemStack().getItem()).entityEvent(player, target);
					event.setCanceled(true);
				}
				else {
					Vec3d vec3d = player.getPositionEyes(1.0f);
					Vec3d vec3d2 = player.getLook(1.0f);
					Vec3d vec3d3 = vec3d.addVector(vec3d2.x * d0, vec3d2.y * d0, vec3d2.z * d0);
					RayTraceResult result = player.world.rayTraceBlocks(vec3d, vec3d3, false, false, false);
					if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK) {
						((ItemNbtBook) event.getItemStack().getItem()).blockEvent(player, result.getBlockPos());
						event.setCanceled(true);
					}
					else if (!player.getHeldItemOffhand().isEmpty()) {
						((ItemNbtBook) event.getItemStack().getItem()).itemEvent(player);
						event.setCanceled(true);
					}
				}
			} // Empty Click:
			else if (event.getItemStack().getItem() instanceof ItemBoundary) {
				((ItemBoundary) event.getItemStack().getItem()).rightClick(event.getItemStack(), player);
				event.setCanceled(true);
			}
			else if (event.getItemStack().getItem() instanceof ISpecBuilder) {
				((ISpecBuilder) event.getItemStack().getItem()).rightClick(event.getItemStack(), player, event.getPos());
				event.setCanceled(true);
			}
			else {
				IPlayer<?> iPlayer = PlayerData.get(player).getIPlayer();
				BaseScriptData handler = EventHooks.getPlayerHandler();
				if (handler.hadInteract) { handler.hadInteract = false; }
				else {
					event.setCanceled(EventHooks.onPlayerInteract(false, new PlayerEvent.InteractEvent(iPlayer, 0, null)));
					if (event.getItemStack().getItem() == CustomItems.scripter_item && !event.isCanceled()) {
						ItemScriptedWrapper isw = ItemScripted.GetWrapper(event.getItemStack());
						event.setCanceled(EventHooks.onScriptItemInteract(isw, new ItemEvent.InteractEvent(isw, iPlayer, 0, null)));
					}
				}
			}
		}
		CustomNpcs.debugData.end(player);
	}

    @SubscribeEvent
	public void cnpcArrowLoose(ArrowLooseEvent event) {
		if (event.getEntityPlayer().world.isRemote || !(event.getWorld() instanceof WorldServer)) { return; }
		CustomNpcs.debugData.start(event.getEntityPlayer());
		event.setCanceled(EventHooks.onPlayerRanged(new PlayerEvent.RangedLaunchedEvent(PlayerData.get(event.getEntityPlayer()).getIPlayer())));
		CustomNpcs.debugData.end(event.getEntityPlayer());
	}

	@SubscribeEvent
	public void cnpcBreak(BreakEvent event) {
		if (event.getPlayer().world.isRemote || !(event.getWorld() instanceof WorldServer)) { return; }
		CustomNpcs.debugData.start(event.getPlayer());
		PlayerEvent.BreakEvent breakEvent = new PlayerEvent.BreakEvent(PlayerData.get(event.getPlayer()).getIPlayer(),
				Objects.requireNonNull(NpcAPI.Instance()).getIBlock(event.getWorld(), event.getPos()), event.getExpToDrop());
		event.setCanceled(EventHooks.onPlayerBreak(breakEvent));
		event.setExpToDrop(breakEvent.exp);
		CustomNpcs.debugData.end(event.getPlayer());
	}

	@SubscribeEvent
	public void cnpcBlockPlace(EntityPlaceEvent event) {
		if (event.getWorld().isRemote || !(event.getWorld() instanceof WorldServer) || !(event.getEntity() instanceof EntityPlayerMP)) {
			return;
		}
		CustomNpcs.debugData.start(event.getEntity());
		EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
		if (event.getPlacedBlock().getBlock() instanceof BlockBanner && player.getHeldItemMainhand().getItem() instanceof ItemBanner) {
			NBTTagCompound nbt = player.getHeldItemMainhand().getTagCompound();
			if (nbt != null && nbt.hasKey("BlockEntityTag", 10)
					&& nbt.getCompoundTag("BlockEntityTag").hasKey("FactionID", 3)) {
				TileEntity tile = event.getWorld().getTileEntity(event.getPos());
				if (tile instanceof TileEntityBanner) {
					((ITileEntityBanner) tile).npcs$setFactionId(nbt.getCompoundTag("BlockEntityTag").getInteger("FactionID"));
				}
			}
		}
		event.setCanceled(EventHooks.onPlayerPlace(new PlayerEvent.PlaceEvent(PlayerData.get(player).getIPlayer(),
				BlockWrapper.createNew(event.getWorld(), event.getPos(), event.getPlacedBlock()))));
		if (event.isCanceled()) {
			NBTTagCompound nbtStack = new NBTTagCompound();
			player.getHeldItemMainhand().writeToNBT(nbtStack);
			Packets.send(player, new PacketDetectHeldItem(player.inventory.currentItem, nbtStack));
		}
		CustomNpcs.debugData.end(event.getEntity());
	}

	@SubscribeEvent
	public void cnpcItemToss(ItemTossEvent event) {
		if (event.getPlayer().world.isRemote) { return; }
		CustomNpcs.debugData.start(event.getPlayer());
		PlayerData data = PlayerData.get(event.getPlayer());
		CustomNPCsScheduler.runTack(() -> {
			for (QuestData qd : data.questData.activeQuests.values()) {
				data.questData.checkQuestCompletion(event.getPlayer(), qd);
			}
		}, 150);
		event.setCanceled(EventHooks.onPlayerToss(new PlayerEvent.TossEvent(data.getIPlayer(),
				Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(event.getEntityItem().getItem()))));
		CustomNpcs.debugData.end(event.getPlayer());
	}

	@SubscribeEvent
	public void cnpcItemPickup(EntityItemPickupEvent event) {
		if (event.getEntityPlayer().world.isRemote) { return; }
		CustomNpcs.debugData.start(event.getEntityPlayer());
		PlayerData data = PlayerData.get(event.getEntityPlayer());
		CustomNPCsScheduler.runTack(() -> {
			for (QuestData qd : data.questData.activeQuests.values()) {
				data.questData.checkQuestCompletion(event.getEntityPlayer(), qd);
			}
		}, 150);
		event.setCanceled(EventHooks.onPlayerPickUp(new PlayerEvent.PickUpEvent(data.getIPlayer(),
				Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(event.getItem().getItem()))));
		CustomNpcs.debugData.end(event.getEntityPlayer());
	}

	@SubscribeEvent
	public void cnpcPlayerContainerOpen(PlayerContainerEvent.Open event) {
		if (event.getEntityPlayer().world.isRemote) { return; }
		CustomNpcs.debugData.start(event.getEntityPlayer());
		EventHooks.onPlayerContainerOpen(new PlayerEvent.ContainerOpen(PlayerData.get(event.getEntityPlayer()).getIPlayer(),
				Objects.requireNonNull(NpcAPI.Instance()).getIContainer(event.getContainer())));
		CustomNpcs.debugData.end(event.getEntityPlayer());
	}

	@SubscribeEvent
	public void cnpcPlayerContainerClose(PlayerContainerEvent.Close event) {
		if (event.getEntityPlayer().world.isRemote) { return; }
		CustomNpcs.debugData.start(event.getEntityPlayer());
		EventHooks.onPlayerContainerClose(new PlayerEvent.ContainerClosed(PlayerData.get(event.getEntityPlayer()).getIPlayer(),
				Objects.requireNonNull(NpcAPI.Instance()).getIContainer(event.getContainer())));
		CustomNpcs.debugData.end(event.getEntityPlayer());
	}

	@SubscribeEvent
	public void cnpcLivingDeathEvent(LivingDeathEvent event) {
		if (event.getEntityLiving().world.isRemote) { return; }
		CustomNpcs.debugData.start(event.getEntityLiving());
		Entity source = NoppesUtilServer.getDamageSource(event.getSource());
		if (event.getEntityLiving() instanceof EntityPlayer) {
			EventHooks.onPlayerDeath(new PlayerEvent.DiedEvent(PlayerData.get((EntityPlayer) event.getEntityLiving()).getIPlayer(),
					event.getSource(), source));
		}
		if (source instanceof EntityPlayer) {
			EventHooks.onPlayerKills(new PlayerEvent.KilledEntityEvent(PlayerData.get((EntityPlayer) source).getIPlayer(), event.getEntityLiving()));
		}
		CustomNpcs.debugData.end(event.getEntityLiving());
	}

	@SubscribeEvent
	public void cnpcLivingHurtEvent(LivingHurtEvent event) {
		if (event.getEntityLiving().world.isRemote) { return; }
		CustomNpcs.debugData.start(event.getEntityLiving());
		Entity source = NoppesUtilServer.getDamageSource(event.getSource());
		if (event.getEntityLiving() instanceof EntityPlayer) {
			PlayerEvent.DamagedEvent pevent = new PlayerEvent.DamagedEvent(PlayerData.get((EntityPlayer) event.getEntityLiving()).getIPlayer(), source, event.getAmount(), event.getSource());
			boolean cancel = EventHooks.onPlayerDamaged(pevent);
			event.setCanceled(cancel);
			if (pevent.clearTarget) {
				event.setCanceled(true);
				event.setAmount(0.0f);
			}
			else { event.setAmount(pevent.damage); }
		}
		if (source instanceof EntityPlayer) {
			PlayerEvent.DamagedEntityEvent pevent = new PlayerEvent.DamagedEntityEvent(PlayerData.get((EntityPlayer) source).getIPlayer(),
					event.getEntityLiving(), event.getAmount(), event.getSource());
			event.setCanceled(EventHooks.onPlayerDamagedEntity(pevent));
			event.setAmount(pevent.damage);
		}
		CustomNpcs.debugData.end(event.getEntityLiving());
	}

	@SubscribeEvent
	public void cnpcLivingAttackEvent(LivingAttackEvent event) {
		if (event.getEntityLiving().world.isRemote) { return; }
		CustomNpcs.debugData.start(event.getEntityLiving());
		Entity source = NoppesUtilServer.getDamageSource(event.getSource());
		Resistances.add(event.getSource() != null ? event.getSource().damageType : "null");
		((IEntityLivingBaseIMixin) event.getEntityLiving()).npcs$setCurrentDamageSource(event.getSource());
		if (source instanceof EntityPlayer) {
			PlayerData data = PlayerData.get((EntityPlayer) source);
			ItemStack item = ((EntityPlayer) source).getHeldItemMainhand();
			IEntity<?> target = Objects.requireNonNull(NpcAPI.Instance()).getIEntity(event.getEntityLiving());
			event.setCanceled(EventHooks.onPlayerAttack(new PlayerEvent.AttackEvent(data.getIPlayer(), target, event.getSource())));
			if (event.isCanceled()) { ((ILivingAttackEventMixin) event).setAmount(0.0f); }
			if (item.getItem() == CustomItems.scripter_item && !event.isCanceled()) {
				ItemScriptedWrapper isw = ItemScripted.GetWrapper(item);
				ItemEvent.AttackEvent itemEvent = new ItemEvent.AttackEvent(isw, data.getIPlayer(), target, event.getSource());
				itemEvent.setCanceled(event.isCanceled());
				event.setCanceled(EventHooks.onScriptItemAttack(isw, itemEvent));
			}
			if (!event.isCanceled()) {
				for (EntityNPCInterface npc : data.game.getMercenaries()) {
					if (!npc.isAttacking()) { npc.setAttackTarget(event.getEntityLiving()); }
				}
			}
		}
		if (event.getEntityLiving() instanceof EntityPlayer && source instanceof EntityLivingBase && !event.isCanceled()) {
			PlayerData data = PlayerData.get((EntityPlayer) event.getEntityLiving());
			for (EntityNPCInterface npc : data.game.getMercenaries()) {
				if (!npc.isAttacking()) {
					npc.setAttackTarget((EntityLivingBase) source);
				}
			}
		}
		CustomNpcs.debugData.end(event.getEntityLiving());
	}

	@SubscribeEvent
	public void cnpcPlayerLoginEvent(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
		CustomNpcs.debugData.start(event.player);
		CommonUtil.sendScriptErrorsTo(event.player);
		if (!event.player.world.isRemote) {
			EntityPlayerMP player = (EntityPlayerMP) event.player;
			if (!ScriptController.Instance.getErrored().isEmpty()) {
				CustomNPCsScheduler.runTack(() -> player.sendMessage(Component.translatable("command.script.logs.view").getParent()), 2500);
			}
			PlayerData data = PlayerData.get(player);
			EventHooks.onPlayerLogin(new PlayerEvent.LoginEvent(data.getIPlayer()));
			PlayerSkinController.getInstance().logged(player);
			MinecraftServer server = player.getServer();
			if (server != null) {
				for (WorldServer world : server.worlds) {
					ServerScoreboard board = (ServerScoreboard) world.getScoreboard();
					for (String objective : Availability.scores) {
						ScoreObjective so = board.getObjective(objective);
						if (so != null) {
							if (board.getObjectiveDisplaySlotCount(so) == 0) {
								player.connection.sendPacket(new SPacketScoreboardObjective(so, 0));
							}
							Score sco = board.getOrCreateScore(player.getName(), so);
							player.connection.sendPacket(new SPacketUpdateScore(sco));
						}
					}
				}
			}
			player.inventoryContainer.addListener(new IContainerListener() {
				@Override
				public void sendAllContents(@Nonnull Container containerToSend, @Nonnull NonNullList<ItemStack> itemsList) {}
				@Override
				public void sendAllWindowProperties(@Nonnull Container containerIn, @Nonnull IInventory inventory) {}
				@Override
				public void sendSlotContents(@Nonnull Container containerToSend, int slotInd, @Nonnull ItemStack stack) {
					if (player.world.isRemote) { return; }
					for (QuestData qd : data.questData.activeQuests.values()) {
						for (IQuestObjective obj : qd.quest.getObjectives((IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player))) {
							if (obj.getType() != 0) {
								continue;
							}
							data.questData.checkQuestCompletion(player, qd);
						}
					}
				}
				@Override
				public void sendWindowProperty(@Nonnull Container containerIn, int varToUpdate, int newValue) {}
			});
			SyncController.syncPlayer(player);
		}
		CustomNpcs.debugData.end(event.player);
	}

	@SubscribeEvent
	public void cnpcPlayerLoggedOut(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.player == EntityNPCInterface.ChatEventPlayer) { return; }
		CustomNpcs.debugData.start(event.player);
		EntityPlayerMP player = (EntityPlayerMP) event.player;
		PlayerData data = PlayerData.get(player);
		EventHooks.onPlayerLogout(new PlayerEvent.LogoutEvent(data.getIPlayer()));
		if (data.bankData.lastBank != null) {
			data.bankData.lastBank.save();
			data.bankData.lastBank = null;
		}
		data.save(false);
		PlayerScriptData.removePlayer(player);
		CustomNpcs.debugData.end(event.player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void cnpcServerChat(ServerChatEvent event) {
		if (event.getPlayer() == EntityNPCInterface.ChatEventPlayer) { return; }
		CustomNpcs.debugData.start(event.getPlayer());
		if (event.getPlayer().world.isRemote) { KeyController.getInstance().save(); }
		else {
			EntityPlayerMP player = event.getPlayer();
			String message = event.getMessage();
			PlayerEvent.ChatEvent chatEvent = new PlayerEvent.ChatEvent(PlayerData.get(player).getIPlayer(), event.getMessage());
			event.setCanceled(EventHooks.onPlayerChat(chatEvent));
			if (!event.isCanceled()) {
				if (!message.equals(chatEvent.message)) { event.setComponent(Component.empty().append(ForgeHooks.newChatWithLinks(chatEvent.message)).getParent()); }
				Packets.sendNearby(player.world, player.getPosition(), 32,
						new PacketChatBubble(player.getEntityId(), Component.translatable(chatEvent.message), false));
			}
		}
		CustomNpcs.debugData.end(event.getPlayer());
	}

	@SuppressWarnings("UnstableApiUsage")
	public ScriptPlayerEventHandler registerForgeEvents(Side side) {
		ForgeEventHandler handler = new ForgeEventHandler();
		LogWriter.info("CustomNpcs: Start load Forge Events:");
		CustomNpcs.debugData.start(null);
		ForgeEventHandler.eventNames.clear();
		List<Class<?>> listClasses = new ArrayList<>();
		try {
			// Get Maim mod Method for All Events
			Method m = handler.getClass().getMethod("forgeEntity", Event.class);
			Method register = EventBus.class.getDeclaredMethod("register", Class.class, Object.class, Method.class, ModContainer.class);
			register.setAccessible(true);
			// Get Registration Method for Event Methods
			for (String forgeEventClassPath : pathsToForgeEventClasses) {
				Class<?> event;
				try { event = Class.forName(forgeEventClassPath); } catch (ClassNotFoundException e) { continue; }
				if (!listClasses.contains(event)) { listClasses.add(event); }
			}
			int eventSize = pathsToForgeEventClasses.length;
			LogWriter.debug("Manually found " + listClasses.size() + " / " + eventSize + " classes of Forge events");

			ClassPath loader = ClassPath.from(this.getClass().getClassLoader());
			// Get all loaded Forge event classes
			List<ClassPath.ClassInfo> list = new ArrayList<>(loader.getTopLevelClassesRecursive("net.minecraftforge.event"));
			list.addAll(loader.getTopLevelClassesRecursive("net.minecraftforge.fml.common"));
			if (eventSize < list.size()) { eventSize = list.size(); }

			for (ClassPath.ClassInfo info : list) {
				String forgeEventClassPath = info.getName();
				if (forgeEventClassPath.startsWith("net.minecraftforge.event.terraingen")) { continue; }
				try {
					Class<?> event = Class.forName(forgeEventClassPath);
					if (!listClasses.contains(event)) { listClasses.add(event); }
				} catch (Throwable ignored) { }
			}
			if (eventSize < listClasses.size()) { eventSize = listClasses.size(); }
			LogWriter.debug("Total of " + listClasses.size() + " / " + eventSize + " classes of Forge events");

			// Not Assing List
			List<Class<?>> notAssingException = new ArrayList<>();
			notAssingException.add(GenericEvent.class);
			notAssingException.add(EntityEvent.EntityConstructing.class);
			notAssingException.add(WorldEvent.PotentialSpawns.class);

			List<Class<?>> isClientEvents = new ArrayList<>();
			isClientEvents.add(ItemTooltipEvent.class);
			isClientEvents.add(GetCollisionBoxesEvent.class);
			isClientEvents.add(TickEvent.RenderTickEvent.class);
			isClientEvents.add(TickEvent.ClientTickEvent.class);
			isClientEvents.add(FMLNetworkEvent.ClientCustomPacketEvent.class);
			// Set the main method of the mod for each event
			boolean threadIsClient = Thread.currentThread().getName().toLowerCase().contains("client");
			for (Class<?> infoClass : listClasses) {
				boolean isClient = false;
				Class<?> debugClass = null;
				try {
					List<Class<?>> classes = new ArrayList<>(Arrays.asList(infoClass.getDeclaredClasses()));
					if (classes.isEmpty()) {
						classes.add(infoClass);
					}

					// Registering events from classes
					for (Class<?> c : classes) {
						debugClass = c;
						// Check
						boolean canAdd = true;
						for (Class<?> nae : notAssingException) {
							if (nae.isAssignableFrom(c)) {
								canAdd = false;
								break;
							}
						}
						isClient = c.getName().startsWith("net.minecraftforge.client.event");
						for (Class<?> nae : isClientEvents) {
							if (nae.isAssignableFrom(c)) {
								isClient = true;
								break;
							}
						}
						if ((side == Side.SERVER && isClient) || !canAdd || !Event.class.isAssignableFrom(c)
								|| Modifier.isAbstract(c.getModifiers()) || !Modifier.isPublic(c.getModifiers())
								|| ForgeEventHandler.eventNames.containsKey(c)) {
							continue;
						}
						// Put Name
						String eventName = ForgeEventHandler.getEventName(c);
						if (!isClient) {
							isClient = eventName.toLowerCase().contains("client") || eventName.toLowerCase().contains("render");
						}
						if (ForgeEventHandler.eventNames.containsValue(eventName)) { continue; }
						if (!isClient) {
							ForgeEventHandler.eventNames.put(c, eventName);
							ForgeEventHandler.clientEventNames.put(c, eventName);
							register.invoke(MinecraftForge.EVENT_BUS, c, handler, m, CustomNpcs.mod);
						}
						else {
							ForgeEventHandler.clientEventNames.put(c, eventName);
							if (threadIsClient) { register.invoke(MinecraftForge.EVENT_BUS, c, handler, m, CustomNpcs.mod); }
						}
						LogWriter.debug("Add Forge "+(isClient ? "client" : "common")+" Event " +c.getName());
					}
				} catch (Exception t) {
					LogWriter.error("[" + side + "] CustomNpcs Error Register Forge " + (isClient ? "client" : "server")
							+ " Event: " + infoClass.getSimpleName()
							+ (debugClass != null ? "; subClass: " + debugClass.getSimpleName() : ""), t);
				}
			}
			if (PixelmonHelper.Enabled) {
				try {
					Field f = ClassLoader.class.getDeclaredField("classes");
					f.setAccessible(true);
					ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
					@SuppressWarnings("unchecked")
					List<Class<?>> classes2 = new ArrayList<>((Collection<? extends Class<?>>) f.get(classLoader));
					for (Class<?> c2 : classes2) {
						if (c2.getName().startsWith("com.pixelmonmod.pixelmon.api.events")
								&& Event.class.isAssignableFrom(c2) && !Modifier.isAbstract(c2.getModifiers())
								&& Modifier.isPublic(c2.getModifiers())) {
							if (ForgeEventHandler.eventNames.containsKey(c2)) { continue; }
							// Put Name
							String eventName = ForgeEventHandler.getEventName(c2);
							if (ForgeEventHandler.eventNames.containsValue(eventName)) { continue; }
							// Add
							register.invoke(PixelmonHelper.EVENT_BUS, c2, handler, m, CustomNpcs.mod);
							ForgeEventHandler.eventNames.put(c2, eventName);
							LogWriter.debug("Add Pixelmon Event[" + ForgeEventHandler.eventNames.size() + "]; " + c2.getName());
						}
					}
				} catch (Exception e) { LogWriter.error(e); }
			}
		}
		catch (Exception e) {
			LogWriter.error(e);
		}
		LogWriter.info("CustomNpcs: Registered [Client:" + ForgeEventHandler.clientEventNames.size() + "; Server: " + ForgeEventHandler.eventNames.size() + "] Forge Events out of [" + listClasses.size() + "] classes");
		CustomNpcs.debugData.end(null);
		return this;
	}

	// New from Unofficial (BetaZavr)
	@SubscribeEvent
	public void cnpcItemCrafted(ItemCraftedEvent event) {
		if (event.player.world.isRemote) { return; }
		CustomNpcs.debugData.start(event.player);
		EntityPlayerMP player = (EntityPlayerMP) event.player;
		PlayerEvent.ItemCrafted craftEvent = new PlayerEvent.ItemCrafted(PlayerData.get(event.player).getIPlayer(),
				Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(event.crafting),
				event.craftMatrix);
		EventHooks.onEvent(EventHooks.getPlayerHandler(), EnumScriptType.ITEM_CRAFTED, craftEvent);
		if (!craftEvent.crafting.isEmpty()) {
			CustomNPCsScheduler.runTack(() -> doCraftQuest(player, craftEvent.crafting.getMCItemStack()));
		}
		CustomNpcs.debugData.end(event.player);
	}

	@SubscribeEvent
	public void cnpcItemFishedEvent(ItemFishedEvent event) {
		if (event.getEntityPlayer().world.isRemote) { return; }
		EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
		CustomNpcs.debugData.start(player);
		PlayerEvent.ItemFished fishedEvent = new PlayerEvent.ItemFished(PlayerData.get(player).getIPlayer(),
				event.getDrops(),
				event.getRodDamage());
		EventHooks.onEvent(EventHooks.getPlayerHandler(), EnumScriptType.ITEM_FISHED, fishedEvent);
		NonNullList<ItemStack> drops = event.getDrops();
		for (int i = 0; i < drops.size() && i < fishedEvent.stacks.length; i++) {
			IItemStack iStack = fishedEvent.stacks[i];
			if (iStack == null) { drops.set(i, ItemStack.EMPTY); }
			else { drops.set(i, iStack.getMCItemStack()); }
		}
		CustomNpcs.debugData.end(event.getEntityPlayer());
	}

}
