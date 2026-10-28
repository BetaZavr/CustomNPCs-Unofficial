package noppes.npcs;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.ForgeRegistries.Keys;
import noppes.npcs.blocks.custom.tiles.CustomTileEntityChest;
import noppes.npcs.containers.*;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;

@EventBusSubscriber(bus = Bus.MOD, modid = CustomNpcs.MODID)
public class CustomContainer {

   public static MenuType<ContainerCarpentryBench> container_carpentrybench;
   public static MenuType<ContainerCustomGui> container_customgui;
   public static MenuType<ContainerMail> container_mail;
   public static MenuType<ContainerManageBanks> container_managebanks;
   public static MenuType<ContainerManageRecipes> container_managerecipes;
   public static MenuType<ContainerMerchantAdd> container_merchantadd;
   public static MenuType<ContainerNPCBank> container_bank;
   public static MenuType<ContainerNPCCompanion> container_companion;
   public static MenuType<ContainerNPCFollowerHire> container_follower;
   public static MenuType<ContainerNPCFollowerHire> container_followerhire;
   public static MenuType<ContainerNPCFollowerSetup> container_followersetup;
   public static MenuType<ContainerNPCInv> container_inv;
   public static MenuType<ContainerNpcItemGiver> container_itemgiver;
   public static MenuType<ContainerNPCTrader> container_trader;
   public static MenuType<ContainerNPCTraderSetup> container_tradersetup;
   public static MenuType<ContainerNPCDropSetup> container_dropsetup;
   // New from Unofficial (BetaZavr)
   public static MenuType<ContainerNpcQuestTypeItem> container_questtypeitem;
   public static MenuType<ContainerNPCTransports> container_managetransport;
   public static MenuType<ContainerNpcAvailabilityItem> container_availability_item;
   public static MenuType<ContainerBuilderSettings> container_builder;
   public static MenuType<ContainerChestCustom> container_custom_chest;
   public static MenuType<ContainerDead> container_npc_dead;
   public static MenuType<ContainerAuctionCreateLot> container_auction_create_lot;
   public static MenuType<ContainerAuctionForbiddenItem> container_auction_forbidden_item;

   @SubscribeEvent
   public static void registerContainer(RegisterEvent event) {
      if (event.getRegistryKey() == Keys.MENU_TYPES && event.getForgeRegistry() != null) {
         CustomNpcs.debugData.start("Mod");
         // MenuType.create(int windowId, Inventory playerInv, FriendlyByteBuf extraData)
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_carpentrybench", container_carpentrybench = createContainer((containerId, inv, buffer) -> new ContainerCarpentryBench(containerId, inv, buffer.readBlockPos())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_customgui", container_customgui = createContainer((containerId, inv, buffer) -> new ContainerCustomGui(containerId, buffer.readNbt())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_mail", container_mail = createContainer((containerId, inv, buffer) -> new ContainerMail(containerId, inv, buffer.readBoolean(), buffer.readBoolean())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_managebanks", container_managebanks = createContainer((containerId, inv, buffer) -> new ContainerManageBanks(containerId, inv)));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_merchantadd", container_merchantadd = createContainer((containerId, inv, buffer) -> new ContainerMerchantAdd(containerId, inv, buffer.readInt())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_companion", container_companion = createContainer((containerId, inv, buffer) -> new ContainerNPCCompanion(containerId, inv, buffer.readInt())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_follower", container_follower = createContainer((containerId, inv, buffer) -> new ContainerNPCFollowerHire(containerId, inv, buffer.readInt(), buffer.readBlockPos())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_followerhire", container_followerhire = createContainer((containerId, inv, buffer) -> new ContainerNPCFollowerHire(containerId, inv, buffer.readInt(), buffer.readBlockPos())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_followersetup", container_followersetup = createContainer((containerId, inv, buffer) -> new ContainerNPCFollowerSetup(containerId, inv, buffer.readInt())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_inv", container_inv = createContainer((containerId, inv, buffer) -> new ContainerNPCInv(containerId, inv, buffer.readInt())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_itemgiver", container_itemgiver = createContainer((containerId, inv, buffer) -> new ContainerNpcItemGiver(containerId, inv, buffer.readInt())));

         // New Unofficial (Goodbird)
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_managerecipes", container_managerecipes = createContainer((containerId, inv, buffer) -> new ContainerManageRecipes(containerId, inv)));

         // New from Unofficial (BetaZavr)
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_questtypeitem", container_questtypeitem = createContainer((containerId, inv, buffer) -> new ContainerNpcQuestTypeItem(containerId, inv, buffer.readInt())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_managetransport", container_managetransport = createContainer((containerId, inv, buffer) -> {
            buffer.readInt(); // npc id
            return new ContainerNPCTransports(containerId, inv, buffer.readBlockPos());
         }));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_availability_item", container_availability_item = createContainer((containerId, inv, buffer) -> new ContainerNpcAvailabilityItem(containerId, inv, buffer.readNbt())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_trader", container_trader = createContainer((containerId, inv, buffer) -> new ContainerNPCTrader(containerId, inv, NoppesUtilServer.getEditingNpc(inv.player), buffer.readInt())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_tradersetup", container_tradersetup = createContainer((containerId, inv, buffer) -> new ContainerNPCTraderSetup(containerId, inv, buffer.readInt(), buffer.readBlockPos())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_bank", container_bank = createContainer((containerId, inv, buffer) -> {
            CompoundTag compound = buffer.readNbt();
            return new ContainerNPCBank(containerId, inv, compound != null ? compound : new CompoundTag());
         }));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_dropsetup", container_dropsetup = createContainer((containerId, inv, buffer) -> {
            CompoundTag compound = buffer.readNbt();
            return new ContainerNPCDropSetup(containerId, inv, compound != null ? compound : new CompoundTag());
         }));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_builder", container_builder = createContainer((containerId, inv, buffer) -> {
            buffer.readInt(); // npc id
            return new ContainerBuilderSettings(containerId, inv, buffer.readBlockPos());
         }));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_custom_chest", container_custom_chest = createContainer((containerId, inv, buffer) -> {
            BlockPos pos = buffer.readBlockPos();
            if (inv.player.level().getBlockEntity(pos) instanceof CustomTileEntityChest chest) {
               return new ContainerChestCustom(containerId, inv, chest);
            }
            return null;
         }));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_npc_dead", container_npc_dead = createContainer((containerId, inv, buffer) -> {
            int sizeInventory = buffer.readInt();
            int pos = buffer.readInt();
            EntityNPCInterface npc = PlayerData.get(inv.player).editingNpc;
            Container deadInventory = null;
            Component name = inv.player.getName();
            if (npc != null && !npc.isAlive()) {
               DataInventory dataInv = npc.inventory;
               deadInventory = dataInv.deadLoot;
               if (pos > -1 && dataInv.deadLoots != null && !dataInv.deadLoots.isEmpty()) {
                  if (dataInv.deadLoots.size() == 1) {
                     for (LivingEntity e : dataInv.deadLoots.keySet()) {
                        if (!(e instanceof Player) && !e.getName().getString().equals(npc.getName().getString())) {
                           deadInventory = dataInv.deadLoots.get(e);
                        }
                     }
                     pos = 0;
                  } else {
                     int i = 0;
                     for (LivingEntity e : dataInv.deadLoots.keySet()) {
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
               else if (deadInventory == null && dataInv.deadLoots != null && dataInv.deadLoots.containsKey(inv.player)) {
                  deadInventory = dataInv.deadLoots.get(inv.player);
               }
            }
            if (deadInventory == null) { deadInventory = new SimpleContainer(sizeInventory); }
            return new ContainerDead(containerId, inv, deadInventory, name, pos);
         }));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_auction_create_lot", container_auction_create_lot = createContainer((containerId, inv, buffer) -> new ContainerAuctionCreateLot(containerId, inv, buffer.readBlockPos())));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":container_auction_forbidden_item", container_auction_forbidden_item = createContainer((containerId, inv, buffer) -> new ContainerAuctionForbiddenItem(containerId, inv, buffer.readBlockPos())));
         CustomNpcs.debugData.end("Mod");
      }
   }

   private static <T extends AbstractContainerMenu> MenuType<T> createContainer(IContainerFactory<T> factoryIn) {
       return new MenuType<>(factoryIn, FeatureFlags.VANILLA_SET);
   }

}
