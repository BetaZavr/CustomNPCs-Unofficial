package noppes.npcs;

import java.lang.reflect.Field;
import java.util.*;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.stats.RecipeBook;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;
import net.minecraftforge.registries.RegistryManager;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.handler.data.INpcRecipe;
import noppes.npcs.blocks.custom.tiles.CustomTileEntityChest;
import noppes.npcs.client.model.animation.AnimationConfig;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.*;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.mixin.entity.player.IEntityPlayerMPMixin;
import noppes.npcs.mixin.stats.IRecipeBookMixin;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nullable;

public class CommonProxy implements IGuiHandler {

	public static final Map<EntityPlayer, Availability> availabilityStacks = new HashMap<>();
	private static Field availabilityMapField;

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) { return null; }

	public static Container getContainer(EnumGuiType gui, EntityPlayer player, FriendlyByteBuf buffer) {
		EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
		switch (gui) {
			case AvailabilityStack: { return new ContainerAvailabilityInv(player); }
			case CustomContainer: {
				TileEntity tile = player.world.getTileEntity(BlockPos.fromLong(buffer.readLong()));
				if (tile instanceof CustomTileEntityChest) {
					return ((CustomTileEntityChest) tile).createContainer(player.inventory, player);
				}
				return null;
			}
			case CustomChest: {
				return new ContainerCustomChest(player, buffer.readInt());
			}
			case MainMenuInv: {
				return new ContainerNPCInv(npc, player);
			}
			case ManageTransport: {
				return new ContainerNPCTransports(player, buffer.readBlockPos());
			}
			case PlayerAnvil: {
				return new ContainerCarpentryBench(player.inventory, player.world, buffer.readBlockPos());
			}
			case PlayerBank: {
				return new ContainerNPCBank(player, buffer.readAnySizeNbt());
			}
			case PlayerFollowerHire: {
				return new ContainerNPCFollowerHire(player, buffer.readInt(), buffer.readBlockPos());
			}
			case PlayerTrader: {
				return new ContainerNPCTrader(player, npc, buffer.readInt());
			}
			case SetupItemGiver: {
				return new ContainerNpcItemGiver(npc, player);
			}
			case SetupTraderDeal: { // Change
				MarcetController mData = MarcetController.getInstance();
				buffer.readInt(); // npc id
				BlockPos ids = buffer.readBlockPos();
				int marcetId = ids.getX();
				int dealId = ids.getY();
				Marcet marcet = mData.getMarcet(marcetId);
				if (marcet == null) { marcet = new Marcet(marcetId); }
				Deal deal = mData.getDeal(dealId);
				if (deal == null) { deal = new Deal(dealId); }
				return new ContainerNPCTraderSetup(marcet, deal, player);
			}
			case SetupFollower: {
				return new ContainerNPCFollowerSetup(npc, player);
			}
			case QuestTypeItem: {
				return new ContainerNpcQuestTypeItem(player, buffer.readInt());
			}
			case ManageRecipes: {
				return new ContainerManageRecipes(player);
			} // Change
			case ManageBanks: {
				return new ContainerManageBanks(player);
			}
			case MerchantAdd: {
				return new ContainerMerchantAdd(player, buffer.readInt());
			}
			case PlayerMailOpen: {
				return new ContainerMail(player, buffer.readBoolean(), buffer.readBoolean());
			}
			case CompanionInv: {
				return new ContainerNPCCompanion(npc, player);
			}
			case DeadInventory: {
				int sizeInventory = buffer.readInt();
				int pos = buffer.readInt();
				npc = PlayerData.get(player).editingNpc;
				IInventory deadInventory = null;
				String name = player.getName();
				if (npc != null && !npc.isEntityAlive()) {
					DataInventory dataInv = npc.inventory;
					deadInventory = dataInv.deadLoot;
					if (pos > -1 && dataInv.deadLoots != null && !dataInv.deadLoots.isEmpty()) {
						if (dataInv.deadLoots.size() == 1) {
							for (EntityLivingBase e : dataInv.deadLoots.keySet()) {
								if (!(e instanceof EntityPlayer) && !e.getName().equals(npc.getName())) {
									deadInventory = dataInv.deadLoots.get(e);
								}
							}
							pos = 0;
						} else {
							int i = 0;
							for (EntityLivingBase e : dataInv.deadLoots.keySet()) {
								if (i != pos) {
									i++;
									continue;
								}
								name = e.getName();
								deadInventory = dataInv.deadLoots.get(e);
								break;
							}
						}
					}
					else if (deadInventory == null && dataInv.deadLoots != null && dataInv.deadLoots.containsKey(player)) {
						deadInventory = dataInv.deadLoots.get(player);
					}
				}
				if (deadInventory == null) { deadInventory = new InventoryBasic("NPC Loot", true, sizeInventory); }
				return new ContainerDead(player, deadInventory, name, pos);
			}
			case BuilderTool:
			case ReplaceTool:
			case RemoverTool: {
				buffer.readInt(); // npc id
				return new ContainerBuilderSettings(player, buffer.readBlockPos());
			}
			case CreationParts: {
				if (npc instanceof EntityCustomNpc) { return new ContainerLayer(player); }
			}
			case SetupDrop: { return new ContainerNPCDropSetup(player, buffer.readAnySizeNbt()); }
			case CustomGui: { return new ContainerCustomGui(buffer.readAnySizeNbt()); }
		}
		return null;
	}

	public EntityPlayer getPlayer() { return null; }

	public PlayerData getPlayerData(EntityPlayer player) {
		if (player == null) { return null; }
		return PlayerData.get(player);
	}

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if (ID > EnumGuiType.values().length) { return null; }
		EnumGuiType gui = EnumGuiType.values()[ID];
		FriendlyByteBuf buffer = new FriendlyByteBuf();
		buffer.writeBlockPos(new BlockPos(x, y, z));
		return getContainer(gui, player, buffer);
	}

	public void load() { }

	public void openGui(EntityPlayer player, Object guiscreen) { }

	public void openGui(EntityNPCInterface npc, EnumGuiType gui, FriendlyByteBuf buffer) { }

	public void postload() { }

	public void preload() { }

	public void reloadItemTextures() { }

	public void spawnParticle(EntityLivingBase player, String string, Object... ob) { }

	public void spawnParticle(EnumParticleTypes type, double x, double y, double z, double motionX, double motionY, double motionZ, float scale) { }

	// New from Unofficial (BetaZavr)
	public void updateKeys() { }

	public String getTranslateLanguage(EntityPlayer player) {
		String lang = getLanguage(player);
		if (lang.contains("_")) { lang = lang.substring(0, lang.indexOf("_")); }
		return lang;
	}

	public String getLanguage(EntityPlayer player) {
		if (player instanceof EntityPlayerMP) { return ((IEntityPlayerMPMixin) player).getLanguage(); }
		return "en_en";
	}

	public void loadAnimationModel(AnimationConfig animation) { }

	public void updatePlayerPos() { }

	public void createAllFiles(ICustomElement customElement) {
		if (customElement instanceof Block) { NoppesUtilServer.createBlockFiles(customElement); }
		if (customElement instanceof Item) { NoppesUtilServer.createItemFiles(customElement); }
	}

	public void playSound(SoundCategory category, String sound, double x, double y, double z, float volume, float pitch, boolean streaming, boolean looping) {  }

	public void stopSound(int category, String sound) { }

	public @Nullable World overworld() {
		if (CustomNpcs.Server != null) { return CustomNpcs.Server.getWorld(0); }
		return null;
	}

	public IForgeRegistry<IRecipe> getRecipeManager() { return RegistryManager.ACTIVE.getRegistry(IRecipe.class); }

	public void syncRecipeManager() {
		IForgeRegistry<IRecipe> manager = CustomNpcs.proxy.getRecipeManager();
		if (!(manager instanceof ForgeRegistry)) { return; }

		ForgeRegistry<IRecipe> forgeRegistry = (ForgeRegistry<IRecipe>) manager;
		IForgeRegistryModifiable<IRecipe> modifiable = (IForgeRegistryModifiable<IRecipe>) manager;
		RecipeController rData = RecipeController.getInstance();

		forgeRegistry.unfreeze();

		try {
			// 1. Collecting RecipeCarpentry items to remove
			List<IRecipe> toRemove = new ArrayList<>();
			for (IRecipe recipe : manager.getValuesCollection()) {
				if (recipe instanceof RecipeCarpentry) {
					RecipeCarpentry carpentry = (RecipeCarpentry) recipe;
					String name = carpentry.getMCId().getResourcePath();
					if (!rData.containsName(name) || !carpentry.isValid()) { toRemove.add(carpentry); }
				}
			}

			// 2. Delete and release the ID (to prevent ID leaks due to a bug in Forge)
			for (IRecipe recipe : toRemove) {
				ResourceLocation key = recipe.getRegistryName();
				int id = forgeRegistry.getID(key);
				modifiable.remove(key);
				if (id >= 0) { clearAvailabilityMapId(id); }
			}

			// 3. We register only current RecipeCarpentry
			for (int i = 0; i < 2; i++) {
				for (INpcRecipe iRecipe : (i == 0 ? rData.getAllGlobalRecipes() : rData.getAllAnvilRecipes())) {
					RecipeCarpentry carpentry = (RecipeCarpentry) iRecipe;
					if (carpentry.isValid()) { manager.register(carpentry); }
				}
			}
		}
		finally { forgeRegistry.freeze(); }

		// 4. Synchronizing the recipe book for all players
		if (CustomNpcs.Server != null) {
			for (EntityPlayerMP player : CustomNpcs.Server.getPlayerList().getPlayers()) { syncRecipe(player.getRecipeBook()); }
		}
	}

	private static void clearAvailabilityMapId(int id) {
		try {
			if (availabilityMapField == null) {
				availabilityMapField = ForgeRegistry.class.getDeclaredField("availabilityMap");
				availabilityMapField.setAccessible(true);
			}
			IForgeRegistry<IRecipe> manager = CustomNpcs.proxy.getRecipeManager();
			BitSet map = (BitSet) availabilityMapField.get(manager);
			if (map != null) {
				map.clear(id);
			}
		}
		catch (Exception e) { LogWriter.error(e); }
	}

	protected void syncRecipe(RecipeBook book) {
		IForgeRegistry<IRecipe> manager = CustomNpcs.proxy.getRecipeManager();
		List<IRecipe> recipes = new ArrayList<>(manager.getValuesCollection());
		BitSet known = ((IRecipeBookMixin) book).getKnown();
		BitSet highlight = ((IRecipeBookMixin) book).getHighlight();
		for (int i = known.nextSetBit(0); i >= 0; i = known.nextSetBit(i + 1)) {
			IRecipe recipe = getRecipe(i);
			if (recipe == null || !recipes.contains(recipe)) { known.clear(i); }
		}
		for (int i = highlight.nextSetBit(0); i >= 0; i = highlight.nextSetBit(i + 1)) {
			IRecipe recipe = getRecipe(i);
			if (recipe == null || !recipes.contains(recipe)) { highlight.clear(i); }
		}
		RecipeController rData = RecipeController.getInstance();
		for (int i = 0; i < 2; i++) {
			for (INpcRecipe npcRecipe : (i == 0 ? rData.getAllGlobalRecipes() : rData.getAllAnvilRecipes())) {
				if (npcRecipe.isKnown()) { book.unlock((IRecipe) npcRecipe); }
			}
		}
	}

	protected static @Nullable IRecipe getRecipe(int id) {
		IRecipe recipe = CraftingManager.REGISTRY.getObjectById(id);
		if (recipe == null) { recipe = ((ForgeRegistry<IRecipe>) ForgeRegistries.RECIPES).getValue(id); }
		return recipe;
	}

	public @Nullable World getOverWorld() {
		if (CustomNpcs.Server != null) { return CustomNpcs.Server.getWorld(0); }
		return null;
	}
}
