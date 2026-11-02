package noppes.npcs.api.event;

import javax.annotation.Nonnull;

import net.minecraft.command.ICommand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import noppes.npcs.api.*;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IEntityLivingBase;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.data.IFaction;
import noppes.npcs.api.handler.data.IKeySetting;
import noppes.npcs.api.interfaces.EventName;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ContainerWrapper;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.constants.EnumScriptType;

public class PlayerEvent extends CustomNPCsEvent {

	public IPlayer<?> player;

	public PlayerEvent(IPlayer<?> playerIn) {
		super();
		player = playerIn;
	}

	@EventName(EnumScriptType.PLAY_SOUND)
	public static class PlayerSound extends PlayerEvent {

		public final String name;
		public final String resource;
		public final String category;
		public final boolean looping;
		public IPos pos;
		public float volume;
		public float pitch;

		public PlayerSound(IPlayer<?> player, String nameIn, String resourceIn, String categoryIn,
						   boolean loopingIn, double x, double y, double z, float volumeIn, float pitchIn) {
			super(player);
			name = nameIn;
			resource = resourceIn;
			category = categoryIn;
			looping = loopingIn;
			pos = API.getIPos(x, y, z);
			volume = volumeIn;
			pitch = pitchIn;
		}

	}

	@EventName(EnumScriptType.FACTION_UPDATE)
	public static class FactionUpdateEvent extends PlayerEvent {
		public IFaction faction;
		public boolean init;
		public int points;

		public FactionUpdateEvent(IPlayer<?> player, IFaction factionIn, int pointsIn, boolean initIn) {
			super(player);
			faction = factionIn;
			points = pointsIn;
			init = initIn;
		}
	}

	@Cancelable
	@EventName(EnumScriptType.CHAT)
	public static class ChatEvent extends PlayerEvent {
		public String message;

		public ChatEvent(IPlayer<?> player, String messageIn) {
			super(player);
			message = messageIn;
		}
	}

	@EventName(EnumScriptType.KEY_PRESSED)
	public static class KeyPressedEvent extends PlayerEvent {

		public final int key;
		public final boolean isAltPressed;
		public final boolean isCtrlPressed;
		public final boolean isMetaPressed;
		public final boolean isShiftPressed;
		public final String openGui;

		public KeyPressedEvent(IPlayer<?> player, int keyIn, boolean isCtrlPressedIn, boolean isAltPressedIn, boolean isShiftPressedIn,
							   boolean isMetaPressedIn, String openGuiIn) {
			super(player);
			key = keyIn;
			isCtrlPressed = isCtrlPressedIn;
			isAltPressed = isAltPressedIn;
			isShiftPressed = isShiftPressedIn;
			isMetaPressed = isMetaPressedIn;
			openGui = openGuiIn;
		}
	}

	@EventName(EnumScriptType.LEVEL_UP)
	public static class LevelUpEvent extends PlayerEvent {
		public int change;

		public LevelUpEvent(IPlayer<?> player, int changeIn) {
			super(player);
			change = changeIn;
		}
	}

	@EventName(EnumScriptType.LOGOUT)
	public static class LogoutEvent extends PlayerEvent {
		public LogoutEvent(IPlayer<?> player) { super(player); }
	}

	@EventName(EnumScriptType.LOGIN)
	public static class LoginEvent extends PlayerEvent {
		public LoginEvent(IPlayer<?> player) { super(player); }
	}

	@EventName(EnumScriptType.TIMER)
	public static class TimerEvent extends PlayerEvent {
		public int id;

		public TimerEvent(IPlayer<?> player, int idIn) {
			super(player);
			id = idIn;
		}
	}

	@Cancelable
	@EventName(EnumScriptType.DAMAGED)
	public static class DamagedEvent extends PlayerEvent {

		public boolean clearTarget = false;
		public float damage;
		public IDamageSource damageSource;
		public IEntity<?> source;

		public DamagedEvent(IPlayer<?> player, Entity sourceIn, float damageIn, DamageSource damagesourceIn) {
			super(player);
			source = API.getIEntity(sourceIn);
			damage = damageIn;
			damageSource = API.getIDamageSource(damagesourceIn);
		}
	}

	@EventName(EnumScriptType.KILL)
	public static class KilledEntityEvent extends PlayerEvent {
		public IEntityLivingBase<?> entity;

		public KilledEntityEvent(IPlayer<?> player, EntityLivingBase entityIn) {
			super(player);
			entity = (IEntityLivingBase<?>) API.getIEntity(entityIn);
		}
	}

	@Cancelable
	@EventName(EnumScriptType.DIED)
	public static class DiedEvent extends PlayerEvent {
		public IDamageSource damageSource;
		public IEntity<?> source;
		public String type;

		public DiedEvent(IPlayer<?> player, DamageSource damagesourceIn, Entity entityIn) {
			super(player);
			type = damagesourceIn.damageType;
			source = API.getIEntity(entityIn);
			damageSource = API.getIDamageSource(damagesourceIn);
		}
	}

	@Cancelable
	@EventName(EnumScriptType.DAMAGED_ENTITY)
	public static class DamagedEntityEvent extends PlayerEvent {
		public float damage;
		public IDamageSource damageSource;
		public IEntity<?> target;

		public DamagedEntityEvent(IPlayer<?> player, Entity targetIn, float damageIn, DamageSource damagesourceIn) {
			super(player);
			target = API.getIEntity(targetIn);
			damage = damageIn;
			damageSource = API.getIDamageSource(damagesourceIn);
		}
	}

	@Cancelable
	@EventName(EnumScriptType.RANGED_LAUNCHED)
	public static class RangedLaunchedEvent extends PlayerEvent {
		public RangedLaunchedEvent(IPlayer<?> player) { super(player); }
	}

	@EventName(EnumScriptType.CONTAINER_CLOSED)
	public static class ContainerClosed extends PlayerEvent {
		public IContainer container;

		public ContainerClosed(IPlayer<?> player, IContainer containerIn) {
			super(player);
			container = containerIn;
		}
	}

	@EventName(EnumScriptType.CONTAINER_OPEN)
	public static class ContainerOpen extends PlayerEvent {
		public IContainer container;

		public ContainerOpen(IPlayer<?> player, IContainer containerIn) {
			super(player);
			container = containerIn;
		}
	}

	@Cancelable
	@EventName(EnumScriptType.PICKUP)
	public static class PickUpEvent extends PlayerEvent {
		public IItemStack item;

		public PickUpEvent(IPlayer<?> player, IItemStack itemIn) {
			super(player);
			item = itemIn;
		}
	}

	@Cancelable
	@EventName(EnumScriptType.TOSS)
	public static class TossEvent extends PlayerEvent {
		public IItemStack item;

		public TossEvent(IPlayer<?> player, IItemStack itemIn) {
			super(player);
			item = itemIn;
		}
	}

	@Cancelable
	@EventName(EnumScriptType.BROKEN)
	public static class BreakEvent extends PlayerEvent {
		public IBlock block;
		public int exp;

		public BreakEvent(IPlayer<?> player, IBlock blockIn, int expIn) {
			super(player);
			block = blockIn;
			exp = expIn;
		}
	}

	@Cancelable
	@EventName(EnumScriptType.ATTACK)
	public static class AttackEvent extends PlayerEvent {

		public Object target;
		public int type;
		public final IDamageSource damageSource;

		public AttackEvent(IPlayer<?> player, int typeIn, Object targetIn) {
			super(player);
			type = typeIn;
			target = targetIn;
			damageSource = null;
		}

		public AttackEvent(IPlayer<?> player, IEntity<?> targetIn, DamageSource damageSourceIn) {
			super(player);
			type = 1;
			target = targetIn;
			damageSource = API.getIDamageSource(damageSourceIn);
		}

	}

	@Cancelable
	@EventName(EnumScriptType.INTERACT)
	public static class InteractEvent extends PlayerEvent {

		public Object target;
		public int type;

		public InteractEvent(IPlayer<?> player, int typeIn, Object targetIn) {
			super(player);
			type = typeIn;
			target = targetIn;
		}

	}

	@EventName(EnumScriptType.TICK)
	public static class UpdateEvent extends PlayerEvent {
		public UpdateEvent(IPlayer<?> player) { super(player); }
	}

	@EventName(EnumScriptType.INIT)
	public static class InitEvent extends PlayerEvent {
		public InitEvent(IPlayer<?> player) { super(player); }
	}

	// New from Unofficial (BetaZavr)
	@EventName(EnumScriptType.GUI_OPEN)
	public static class OpenGUI extends PlayerEvent {

		public String newGUI;
		public String oldGUI;

		public OpenGUI(IPlayer<?> player, String n, String o) {
			super(player);
			newGUI = n;
			oldGUI = o;
		}

	}

	@EventName(EnumScriptType.ITEM_CRAFTED)
	public static class ItemCrafted extends PlayerEvent {

		public final IItemStack crafting;
		public final IInventory craftMatrix;

		public ItemCrafted(IPlayer<?> player, @Nonnull IItemStack craftingIn, IInventory craftMatrixIn) {
			super(player);
			crafting = craftingIn;
			craftMatrix = craftMatrixIn;
		}
	}

	@Cancelable
	@EventName(EnumScriptType.ITEM_FISHED)
	public static class ItemFished extends PlayerEvent {

		public int rodDamage;
		public IItemStack[] stacks;

		public ItemFished(IPlayer<?> player, NonNullList<ItemStack> drops, int rodDamageIn) {
			super(player);
			stacks = new IItemStack[drops.size()];
			for (int i = 0; i < drops.size(); i++) {
				stacks[i] = API.getIItemStack(drops.get(i));
			}
			rodDamage = rodDamageIn;
		}
	}

	@EventName(EnumScriptType.PACKAGE_FROM)
	public static class PlayerPackage extends PlayerEvent {
		public INbt nbt;
		public PlayerPackage(IPlayer<?> player, NBTTagCompound nbtMC) {
			super(player);
			nbt = new NBTWrapper(nbtMC);
		}
	}

	@EventName(EnumScriptType.MOUSE_MOVE)
	public static class MouseMoveEvent extends PlayerEvent {
		public boolean isAltPressed;
		public boolean isCtrlPressed;
		public boolean isMetaPressed;
		public boolean isShiftPressed;
		public int posX;
		public int posY;
		public int mouseX;
		public int mouseY;
		public int scrolled;

		public MouseMoveEvent(IPlayer<?> player, int x, int y, int dx, int dy, int scrolledIn,
							  boolean isCtrlPressedIn, boolean isAltPressedIn, boolean isShiftPressedIn, boolean isMetaPressedIn) {
			super(player);
			posX = x;
			posY = y;
			mouseX = dx;
			mouseY = dy;
			scrolled = scrolledIn;
			isCtrlPressed = isCtrlPressedIn;
			isAltPressed = isAltPressedIn;
			isShiftPressed = isShiftPressedIn;
			isMetaPressed = isMetaPressedIn;
		}
	}

	@EventName(EnumScriptType.KEY_ACTIVE)
	public static class KeyActive extends PlayerEvent {

		public IKeySetting key;
		public int id;

		public KeyActive(IPlayer<?> player, IKeySetting kb) {
			super(player);
			key = kb;
		}

	}

	@Cancelable
	@EventName(EnumScriptType.CUSTOM_TELEPORT)
	public static class CustomTeleport extends PlayerEvent {

		public IPos pos;
		public IPos portal;
		public int dimension;

		public CustomTeleport(IPlayer<?> player, IPos portalIn, IPos posIn, int dimensionID) {
			super(player);
			pos = posIn;
			portal = portalIn;
			dimension = dimensionID;
		}

	}

	@Cancelable
	@EventName(EnumScriptType.PLEASED)
	public static class PlaceEvent extends PlayerEvent {

		public IBlock block;

		public PlaceEvent(IPlayer<?> player, IBlock blockIn) {
			super(player);
			block = blockIn;
		}

	}

	@Cancelable
	@EventName(EnumScriptType.SEND_COMMAND)
	public static class CommandEvent extends PlayerEvent {
		public ICommand command;
		public String[] parameters;

		public CommandEvent(IPlayer<?> player, ICommand commandIn, String[] parametersIn) {
			super(player);
			command = commandIn;
			parameters = parametersIn;
		}
	}

	@EventName(EnumScriptType.GUI_SLOT_CHANGED)
	public static class SlotChangedItemStackEvent extends PlayerEvent {

		public final int slotIndex;
		public final IItemStack handStack;
		public final IContainer container;
		public IItemStack slotStack;

		public SlotChangedItemStackEvent(IPlayer<?> player, int slotIndexIn, ItemStack slotStackIn, ItemStack handStackIn, IInventory inventoryIn) {
			super(player);
			slotIndex = slotIndexIn;
			slotStack = API.getIItemStack(slotStackIn);
			handStack = API.getIItemStack(handStackIn);
			container = new ContainerWrapper(inventoryIn);
		}

		@SuppressWarnings("unused")
		public void setSlotStack(IItemStack slotStackIn) { slotStack = slotStackIn; }

	}

}
