package noppes.npcs;

import java.util.*;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ListenableFutureTask;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandGive;
import net.minecraft.command.CommandTime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.api.wrapper.WrapperEntityData;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.controllers.VisibilityController;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemSoulstoneEmpty;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.packets.client.PacketGuiCloneOpen;
import noppes.npcs.packets.client.PacketGuiOpen;
import noppes.npcs.packets.client.PacketMarkData;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;

public class ServerEventsHandler {

	private void doFactionPoints(EntityPlayer player, EntityNPCInterface npc) { npc.faction.factions.addPoints(player); }

	private void doKillQuest(EntityPlayer player, EntityLivingBase entity, boolean forAll) {
		PlayerData pdata = PlayerData.get(player);
		PlayerQuestData playerdata = pdata.questData;
		String entityName = EntityList.getEntityString(entity);
		if (entity instanceof EntityPlayer) {
			entityName = "Player";
		}
		for (QuestData data : new ArrayList<>(playerdata.activeQuests.values())) {
			if (data.quest.step == 2 && data.quest.questInterface.isCompleted(player)) { continue; }
			boolean bo = data.quest.step == 1;
			for (IQuestObjective obj : data.quest.getObjectives((IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player))) {
				if (data.quest.step == 1 && !bo) {
					break;
				}
				bo = obj.isCompleted();
				if (((QuestObjective) obj).getEnumType() != EnumQuestTask.KILL
						&& ((QuestObjective) obj).getEnumType() != EnumQuestTask.AREAKILL) {
					continue;
				}
				String name = null;

				LogWriter.info("[DEBUG] \""+obj.getTargetName()+"\"; \""+entity.getName()+"\" / \""+entityName+"\"; "+
						obj.getTargetName().equals(entity.getName())+"; "+
						obj.getTargetName().equals(entityName)+"; "+
						(obj.isPartName() || obj.isAndTitle()));

				if (obj.getTargetName().equals(entity.getName())) { name = entity.getName(); }
				else if (obj.getTargetName().equals(entityName)) { name = entityName; }
				else if (obj.isPartName() || obj.isAndTitle()) {
					if (obj.isPartName()) {
						if (entity.getName().contains(obj.getTargetName())) {
							name = obj.getTargetName();
						} else {
                            assert entityName != null;
                            if (entityName.contains(obj.getTargetName())) {
                                name = obj.getTargetName();
                            }
                        }
					}
					if (name == null && obj.isAndTitle() && entity instanceof EntityNPCInterface) {
						EntityNPCInterface npc = (EntityNPCInterface) entity;
						String title = npc.display.getTitle();
						if (title.equals(obj.getTargetName())) {
							name = entity.getName();
						} else if (title.equals(entityName)) {
							name = entityName;
						}
						if (name == null && obj.isPartName()) {
							if (title.contains(obj.getTargetName())) {
								name = obj.getTargetName();
							} else if (title.contains(obj.getTargetName())) {
								name = obj.getTargetName();
							}
						}
					}
				}
				else { continue; }
				if (name == null) {
					continue;
				}
				if (obj.getType() == EnumQuestTask.AREAKILL.ordinal() && forAll) {
					int range = obj.getAreaRange();
					for (EntityPlayer pl : player.world.getEntitiesWithinAABB(EntityPlayer.class,
							new AxisAlignedBB(-range, -range, -range, range, range, range).offset(player.posX, player.posY, player.posZ),
							(e) -> e.getDistance(player) < range)) {
						if (pl != player) { doKillQuest(pl, entity, false); }
					}
				}
				HashMap<String, Integer> killed = ((QuestObjective) obj).getKilled(data); // in Data
				if (killed.containsKey(name) && killed.get(name) >= obj.getMaxProgress()) {
					continue;
				}
				int amount = 0;
				if (killed.containsKey(name)) {
					amount = killed.get(name);
				}
				amount++;
				killed.put(name, amount);
				((QuestObjective) obj).setKilled(data, killed);
				if (data.quest.showProgressInWindow) {
					NBTTagCompound compound = new NBTTagCompound();
					compound.setInteger("QuestID", data.quest.id);
					compound.setString("Type", "kill");
					compound.setIntArray("Progress", new int[] { amount, obj.getMaxProgress() });
					compound.setString("TargetName", new TextComponentTranslation("script.killed").getFormattedText()
							+ ": \"" + entity.getName() + "\"");
					Packets.send((EntityPlayerMP) player, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
				}
				if (data.quest.showProgressInChat) {
					if (amount >= obj.getMaxProgress()) {
						player.sendMessage(new TextComponentTranslation("quest.message.kill.1",
								new TextComponentTranslation(entity.getName()).getFormattedText(),
								data.quest.getTitle()));
					} else {
						player.sendMessage(new TextComponentTranslation("quest.message.kill.0",
								new TextComponentTranslation(entity.getName()).getFormattedText(), "" + amount,
								"" + obj.getMaxProgress(), data.quest.getTitle()));
					}
				}
				playerdata.checkQuestCompletion(player, data);
				playerdata.updateClient = true;
			}
		}
	}

	@SubscribeEvent
	public void cnpcEntityInteract(PlayerInteractEvent.EntityInteract event) {
		CustomNpcs.debugData.start(event.getEntityPlayer());
		ItemStack item = event.getEntityPlayer().getHeldItemMainhand();
		if (!item.isEmpty() && event.getHand() == EnumHand.MAIN_HAND && !event.getEntityPlayer().world.isRemote) {
			EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
			if (!CustomNpcs.OpsOnly || CommonUtil.isOp(player)) {
				if (item.getItem() == CustomItems.soulstoneEmpty && event.getTarget() instanceof EntityLivingBase) {
					((ItemSoulstoneEmpty) item.getItem()).store((EntityLivingBase) event.getTarget(), item, player);
					event.setCanceled(true);
				}
				else if (item.getItem() == CustomItems.wand) {
					if (event.getTarget() instanceof EntityVillager) {
						if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.EDIT_VILLAGER)) {
							event.setCanceled(true);
							NoppesUtilServer.setEditingNpc(player, null);
							NoppesUtilServer.openContainerGui(player, EnumGuiType.MerchantAdd, (buffer) -> buffer.writeInt(event.getTarget().getEntityId()));
						}
					}
					else if (event.getTarget() instanceof EntityNPCInterface) {
						if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_GUI)) {
							event.setCanceled(true);
							NoppesUtilServer.sendOpenGui(player, EnumGuiType.MainMenuDisplay, (EntityNPCInterface) event.getTarget());
						}
					}
				}
				else if (item.getItem() == CustomItems.cloner&& !(event.getTarget() instanceof EntityPlayer)) {
					NBTTagCompound compound = new NBTTagCompound();
					if (event.getTarget().writeToNBTAtomically(compound)) {
						String s = compound.getString("id");
						if (s.equals("minecraft:customnpcs.customnpc") || s.equals("minecraft:customnpcs:customnpc")) { compound.setString("id", CustomNpcs.MODID + ":customnpc"); }
						PlayerData data = PlayerData.get(player);
						ServerCloneController.Instance.cleanTags(compound);
						Packets.send(player, new PacketGuiCloneOpen(compound));
						data.cloned = compound;
						event.setCanceled(true);
					}
				}
				else if (item.getItem() == CustomItems.scripter && event.getTarget() instanceof EntityNPCInterface) {
					if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_GUI)) {
						event.setCanceled(true);
						NoppesUtilServer.setEditingNpc(player, (EntityNPCInterface) event.getTarget());
						Packets.send(player, new PacketGuiOpen(EnumGuiType.Script, BlockPos.ORIGIN));
					}
				}
				else if (item.getItem() == CustomItems.mount) {
					if (CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_MOUNTER)) {
						event.setCanceled(true);
						PlayerData.get(player).mounted = event.getTarget();
						Packets.send(player, new PacketGuiOpen(EnumGuiType.MobSpawnerMounter, BlockPos.ORIGIN));
					}
				}
			}
		}
		CustomNpcs.debugData.end(event.getEntityPlayer());
	}

	@SubscribeEvent
	public void cnpcLivingDeath(LivingDeathEvent event) {
		if (event.getEntityLiving().world.isRemote) { return; }
		CustomNpcs.debugData.start(event.getEntityLiving());
		Entity source = NoppesUtilServer.getDamageSource(event.getSource());
		if (source != null) {
			if (source instanceof EntityNPCInterface && event.getEntityLiving() != null) {
				EntityNPCInterface npc = (EntityNPCInterface) source;
				Line line = npc.advanced.getKillLine();
				if (line != null) { npc.saySurrounding(Line.formatTarget(line, event.getEntityLiving())); }
				EventHooks.onNPCKills(npc, event.getEntityLiving());
			}

			EntityPlayer player;
			if (source instanceof EntityPlayer) { player = (EntityPlayer) source; }
			else if (source instanceof EntityNPCInterface && ((EntityNPCInterface) source).getOwner() instanceof EntityPlayer) { player = (EntityPlayer) ((EntityNPCInterface) source).getOwner(); }
			else if (source instanceof EntityTameable && ((EntityTameable) source).getOwner() instanceof EntityPlayer) { player = (EntityPlayer)((EntityTameable) source).getOwner(); }
			else { player = null; }
			if (player != null && player.getServer() != null) {
				CustomNPCsScheduler.runTack(() ->  doKillQuest(player, event.getEntityLiving(), true));
				if (event.getEntity() instanceof EntityNPCInterface) {
					CustomNPCsScheduler.runTack(() ->  doFactionPoints(player, (EntityNPCInterface)event.getEntity()));
				}
			}
		}
		if (event.getEntityLiving() instanceof EntityPlayer) { PlayerData.get((EntityPlayer) event.getEntityLiving()).save(false); }
		CustomNpcs.debugData.end(event.getEntityLiving());
	}

	@SubscribeEvent
	public void cnpcEntityJoinWorld(EntityJoinWorldEvent event) {
		if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityPlayer)) { return; }
		CustomNpcs.debugData.start(event.getEntity());
		PlayerData.get((EntityPlayer) event.getEntity()).updateCompanion(event.getWorld());
		CustomNpcs.debugData.end(event.getEntity());
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public void cnpcAttachCapabilitiesEntity(AttachCapabilitiesEvent<Entity> event) {
		CustomNpcs.debugData.start(event.getObject());
		if (event.getObject() instanceof EntityPlayer) { PlayerData.register(event); }
		if (event.getObject() instanceof EntityLivingBase) { MarkData.register(event); }
		WrapperEntityData.register(event);
		CustomNpcs.debugData.end(event.getObject());
	}

	@SubscribeEvent
	public void cnpcAttachCapabilitiesItem(AttachCapabilitiesEvent<ItemStack> event) {
		CustomNpcs.debugData.start("Item");
		ItemStackWrapper.register(event);
		CustomNpcs.debugData.end("Item");
	}

	@SubscribeEvent
	public void cnpcSaveToFile(PlayerEvent.SaveToFile event) {
		CustomNpcs.debugData.start(event.getEntityPlayer());
		PlayerData.get(event.getEntityPlayer()).save(false);
		CustomNpcs.debugData.end(event.getEntityPlayer());
	}

	@SubscribeEvent
	public void cnpcStartTracking(PlayerEvent.StartTracking event) {
		if (event.getTarget() instanceof EntityLivingBase && !event.getTarget().world.isRemote) {
			EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
			CustomNpcs.debugData.start(player);
			if (event.getTarget() instanceof EntityNPCInterface) {
				EntityNPCInterface npc = (EntityNPCInterface) event.getTarget();
				npc.tracking.add(player.getEntityId());
				VisibilityController.checkIsVisible(npc, player);
			}
			MarkData data = MarkData.get((EntityLivingBase) event.getTarget());
			if (!data.marks.isEmpty()) {
				Packets.send(player, new PacketMarkData(event.getTarget().getEntityId(), data.getNBT()));
			}
			CustomNpcs.debugData.end(player);
		}
		CustomNpcs.debugData.end(event.getEntityPlayer());
	}

	@SubscribeEvent
	public void cnpcCommandEvent(CommandEvent event) {
		CustomNpcs.debugData.start(event.getSender());
		if (event.getSender() instanceof EntityPlayer) {
			noppes.npcs.api.event.PlayerEvent.CommandEvent ev = new noppes.npcs.api.event.PlayerEvent.CommandEvent(
					(IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity((Entity) event.getSender()),
					event.getCommand(),
					event.getParameters()
			);
			EventHooks.onEvent(PlayerData.get((EntityPlayer) event.getSender()).scriptData, EnumScriptType.SEND_COMMAND, ev);
			if (ev.isCanceled()) {
				event.setCanceled(true);
				CustomNpcs.debugData.end(event.getSender());
				return;
			}
		}
		if (event.getCommand() instanceof CommandGive) {
			if (!(event.getSender().getEntityWorld() instanceof WorldServer)) {
				CustomNpcs.debugData.end(event.getSender());
				return;
			}
			try {
				EntityPlayer player = CommandBase.getPlayer(Objects.requireNonNull(event.getSender().getServer()),  event.getSender(), event.getParameters()[0]);
				Objects.requireNonNull(player.getServer()).futureTaskQueue.add(ListenableFutureTask.create(Executors.callable(() -> {
					PlayerQuestData playerdata = PlayerData.get(player).questData;
					for (QuestData data : playerdata.activeQuests.values()) {
						for (QuestObjective obj : data.quest.getObjectives(player)) {
							if (obj.getType() != EnumQuestTask.ITEM.ordinal()) { continue; }
							playerdata.checkQuestCompletion(player, data);
							playerdata.updateClient = true;
						}
					}
				})));
			} catch (Exception e) {
				LogWriter.error("Error player check quest completion:", e);
			}
		}
		else if (event.getCommand() instanceof CommandTime) {
			try {
				List<EntityPlayerMP> players = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
				for (EntityPlayerMP playerMP : players) { VisibilityController.instance.onUpdate(playerMP); }
			} catch (Exception e) {
				LogWriter.error("Error player update visible NPC:", e);
			}
		}
		CustomNpcs.debugData.end(event.getSender());
	}

	@SubscribeEvent
	public void cnpcPopulateChunk(PopulateChunkEvent.Post event) {
		if (event.getWorld() instanceof WorldServer) {
			CustomNpcs.debugData.start(null);
			NPCSpawning.performWorldGenSpawning((WorldServer) event.getWorld(), event.getChunkX(), event.getChunkZ(), event.getRand());
			CustomNpcs.debugData.end(null);
		}
	}

	@SubscribeEvent
	public void cnpcSaveChunk(ChunkDataEvent.Save event) {
		CustomNpcs.debugData.start(null);
		for (ClassInheritanceMultiMap<Entity> map : event.getChunk().getEntityLists()) {
			for (Entity e : map) {
				if (e instanceof EntityLivingBase) {
					MarkData md = MarkData.get((EntityLivingBase) e);
					if (md.entity == null) { md.entity = (EntityLivingBase) e; }
					md.save();
				}
			}
		}
		CustomNpcs.debugData.end(null);
	}

	// New from Unofficial (GoodBird)
	@SubscribeEvent
	public void cnpcStartTracking(PlayerEvent.StopTracking event) {
		CustomNpcs.debugData.start(event.getEntity());
		if (event.getTarget() instanceof EntityNPCInterface) {
			((EntityNPCInterface) event.getTarget()).tracking.remove(event.getEntity().getEntityId());
		}
		CustomNpcs.debugData.end(event.getEntity());
	}

	/*
	@SubscribeEvent
	@SuppressWarnings("all")
	public void npcLivingJumpEvent(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
		if (!(event.getEntityLiving() instanceof EntityPlayer)) { return; }
		EntityPlayer player = (EntityPlayer) event.getEntityLiving();
		CustomNPCsScheduler.runTack(() -> {
			if (player instanceof EntityPlayerMP) {
				try {
					LogWriter.info("[DEBUG] "+player.world);
				}
				catch (Exception e) { LogWriter.error(e); }
			}
			else {
				try {
					LogWriter.info("[DEBUG] "+player.world);
				}
				catch (Exception e) { LogWriter.error(e); }
			}
		});
	}
	/**/

}
