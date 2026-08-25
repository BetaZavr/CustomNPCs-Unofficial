package noppes.npcs;

import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagRegistry;
import net.minecraft.world.item.*;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.ForgeRegistries.Keys;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.blocks.BlockBorder;
import noppes.npcs.blocks.BlockBuilder;
import noppes.npcs.blocks.BlockCarpentryBench;
import noppes.npcs.blocks.BlockCopy;
import noppes.npcs.blocks.BlockMailbox;
import noppes.npcs.blocks.BlockNpcRedstone;
import noppes.npcs.blocks.BlockScripted;
import noppes.npcs.blocks.BlockScriptedDoor;
import noppes.npcs.blocks.BlockWaypoint;
import noppes.npcs.blocks.custom.*;
import noppes.npcs.blocks.tiles.TileBlockAnvil;
import noppes.npcs.blocks.tiles.TileBorder;
import noppes.npcs.blocks.tiles.TileBuilder;
import noppes.npcs.blocks.tiles.TileCopy;
import noppes.npcs.blocks.tiles.TileMailbox;
import noppes.npcs.blocks.tiles.TileRedstoneBlock;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.blocks.tiles.TileScriptedDoor;
import noppes.npcs.blocks.tiles.TileWaypoint;
import noppes.npcs.blocks.custom.tiles.CustomTileEntityChest;
import noppes.npcs.blocks.custom.tiles.CustomTileEntityPortal;
import noppes.npcs.fluids.CustomFluid;
import noppes.npcs.fluids.CustomFluidType;
import noppes.npcs.items.ItemNpcBlock;
import noppes.npcs.items.custom.CustomBottleItem;
import noppes.npcs.mixin.world.level.block.state.IBlockBehaviourPropertiesMixin;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.ModData;
import noppes.npcs.util.NBTJsonUtil;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.List;

@EventBusSubscriber(bus = Bus.MOD, modid = CustomNpcs.MODID)
public class CustomBlocks {

   public static Block redstone;
   public static Item redstone_item;
   public static Block mailbox;
   public static Item mailbox_item;
   public static Block mailbox2;
   public static Item mailbox2_item;
   public static Block mailbox3;
   public static Item mailbox3_item;
   public static Block waypoint;
   public static Item waypoint_item;
   public static Block border;
   public static Item border_item;
   public static Block scripted;
   public static Item scripted_item;
   public static Block scripted_door;
   public static Item scripted_door_item;
   public static Block builder;
   public static Item builder_item;
   public static Block copy;
   public static Item copy_item;
   public static Block carpenty;
   public static Item carpenty_item;

   public static BlockEntityType<TileBlockAnvil> tile_anvil;
   public static BlockEntityType<TileBorder> tile_border;
   public static BlockEntityType<TileBuilder> tile_builder;
   public static BlockEntityType<TileCopy> tile_copy;
   public static BlockEntityType<TileMailbox> tile_mailbox;
   public static BlockEntityType<TileRedstoneBlock> tile_redstoneblock;
   public static BlockEntityType<TileScripted> tile_scripted;
   public static BlockEntityType<TileScriptedDoor> tile_scripteddoor;
   public static BlockEntityType<TileWaypoint> tile_waypoint;
   // custom
   public static BlockEntityType<CustomTileEntityPortal> tile_custom_portal;
   public static BlockEntityType<CustomTileEntityChest> tile_custom_chest;
   public static final Map<ICustomElement, Item> customblocks = new LinkedHashMap<>();
   public static final Map<String, ICustomElement> customfluid = new TreeMap<>();
   public static CompoundTag registryNbt;

   /** Subsequence:
    * 0: minecraft:sound_event
    * 1: minecraft:fluid
    * 2: minecraft:block
    * 3: minecraft:attribute
    * 4: minecraft:mob_effect
    * 5: minecraft:particle_type
    * 6: minecraft:item
    * 7: minecraft:entity_type
    * 8: minecraft:sensor_type
    * 9: minecraft:memory_module_type
    * 10: minecraft:potion
    * 11: minecraft:enchantment
    * 12: minecraft:block_entity_type
    * 13: minecraft:painting_variant
    * 14: minecraft:stat_type
    * 15: minecraft:chunk_status
    * 16: minecraft:menu
    * 17: minecraft:recipe_type
    * 18: minecraft:recipe_serializer
    * 19: minecraft:command_argument_type
    * 20: minecraft:villager_profession
    * 21: minecraft:point_of_interest_type
    * 22: minecraft:schedule
    * 23: minecraft:activity
    * 24: minecraft:worldgen/carver
    * 25: minecraft:worldgen/feature
    * 26: minecraft:worldgen/block_state_provider_type
    * 27: minecraft:worldgen/foliage_placer_type
    * 28: minecraft:worldgen/tree_decorator_type
    * 29: forge:biome_modifier_serializers
    * 30: forge:display_contexts
    * 31: forge:entity_data_serializers
    * 32: forge:fluid_type
    * 33: forge:global_loot_modifier_serializers
    * 34: forge:holder_set_type
    * 35: forge:structure_modifier_serializers
    * 36: minecraft:worldgen/biome
    */
   @SuppressWarnings("unchecked")
   @SubscribeEvent
   public static void registerBlocks(RegisterEvent event) {
      if (event.getForgeRegistry() == null) { return; }
      CustomNpcs.debugData.start("Mod");
      if (event.getRegistryKey() == Keys.FLUIDS) {
         File blocksFile = new File(CustomNpcs.Dir, "custom_blocks.js");
         CompoundTag nbtBlocks = getBlocksNbt(blocksFile);
         boolean resave = nbtBlocks.getBoolean("resave");
         nbtBlocks.remove("resave");

         for (int i = 0; i < nbtBlocks.getList("Blocks", 10).size(); i++) {
            CompoundTag nbtBlock = nbtBlocks.getList("Blocks", 10).getCompound(i);
            if (!nbtBlock.contains("RegistryName", 8) || !nbtBlock.contains("BlockType", 1)
                    || nbtBlock.getString("RegistryName").isEmpty() || nbtBlock.getByte("BlockType") < (byte) 0
                    || nbtBlock.getByte("BlockType") > (byte) 6) {
               LogWriter.error("Attempt to load block pos: " + i + "; name: \"" + nbtBlock.getString("RegistryName") + "\" - failed");
               continue;
            }
            if (nbtBlock.getByte("BlockType") == (byte) 1) {
               String preName = "custom_fluid_" + nbtBlock.getString("RegistryName");
               String name = NoppesUtilServer.validPath(preName);
               if (!preName.equals(name)) {
                  nbtBlock.putString("RegistryName", name);
                  resave = true;
               }
               if (!nbtBlock.contains("Properties", 10)) {
                  nbtBlock.put("Properties", new CompoundTag());
                  resave = true;
               }
               if (nbtBlock.contains("IsOBJModel", 1)) {
                  nbtBlock.remove("IsOBJModel");
                  resave = true;
               }
               CompoundTag propertyNbt = nbtBlock.getCompound("Properties");
               if (!propertyNbt.contains("liquid", 1) || !propertyNbt.getBoolean("liquid")) {
                  propertyNbt.putBoolean("liquid", true);
                  resave = true;
               }
               if (!propertyNbt.contains("replaceable", 1) || !propertyNbt.getBoolean("replaceable")) {
                  propertyNbt.putBoolean("replaceable", true);
                  resave = true;
               }
               if (!propertyNbt.contains("noCollission", 1) || !propertyNbt.getBoolean("noCollission")) {
                  propertyNbt.putBoolean("noCollission", true);
                  resave = true;
               }
               if (!propertyNbt.contains("noLootTable", 1) || !propertyNbt.getBoolean("noLootTable")) {
                  propertyNbt.putBoolean("noLootTable", true);
                  resave = true;
               }
               if (!propertyNbt.contains("sound", 8)) {
                  propertyNbt.putString("sound", "empty");
                  resave = true;
               }
               if (!propertyNbt.contains("pushReaction", 8)) {
                  propertyNbt.putString("pushReaction", "DESTROY");
                  resave = true;
               }
               ResourceLocation location = new ResourceLocation(CustomNpcs.MODID, name);
               CustomFluidType fluidType = CustomFluidTypes.getFluidType(location);
               if (fluidType != null) { // register fluid
                  registryNbt = nbtBlock;
                  CompoundTag nbtType = nbtBlock.getCompound("FluidType");
                  CompoundTag nbtProperties = nbtBlock.getCompound("Properties");
                  int tickRate = nbtType.contains("tickRate", 3) ? nbtType.getInt("tickRate") : 5;
                  int slopeFindDistance = nbtType.contains("slopeFindDistance", 3) ? nbtType.getInt("slopeFindDistance") : 4;
                  int levelDecreasePerBlock = nbtType.contains("levelDecreasePerBlock", 3) ? nbtType.getInt("levelDecreasePerBlock") : 1;
                  float explosionResistance = Math.max(0.0F, nbtProperties.contains("explosionResistance", 5) ? nbtProperties.getFloat("explosionResistance") : 1.0f);
                  CustomFluid source = new CustomFluid.Source(location, nbtBlock, fluidType,
                          slopeFindDistance, levelDecreasePerBlock, explosionResistance, tickRate);
                  CustomFluid flowing = new CustomFluid.Flowing(new ResourceLocation(CustomNpcs.MODID, "flowing_" + location.getPath()), nbtBlock,
                          fluidType, slopeFindDistance, levelDecreasePerBlock, explosionResistance, tickRate);
                  BucketItem bucket = new BucketItem(() -> source, (new Item.Properties()).craftRemainder(Items.BUCKET).stacksTo(1));
                  CustomBottleItem bottle = new CustomBottleItem(source, (new Item.Properties()).craftRemainder(Items.GLASS_BOTTLE).stacksTo(LayeredCauldronBlock.MAX_FILL_LEVEL));
                  CustomBlockLiquid block = new CustomBlockLiquid(location, () -> source, getProperty(nbtBlock), nbtBlock);
                  flowing.setLinks(source, flowing, block, bucket, bottle);
                  source.setLinks(source, flowing, block, bucket, bottle);
                  event.getForgeRegistry().register(flowing.getLocation(), flowing);
                  event.getForgeRegistry().register(source.getLocation(), source);
                  LogWriter.debug("Load Custom Fluid: \"" + location + "\"");
                  customfluid.put(location.toString(), source);
               }
            } // Liquid
         }
         registryNbt = null;
         if (resave) { Util.instance.saveFile(blocksFile, nbtBlocks); }
      } // 1
      if (event.getRegistryKey() == Keys.BLOCKS) {
         event.getForgeRegistry().register(CustomNpcs.MODID + ":npcredstoneblock", redstone = new BlockNpcRedstone());
         event.getForgeRegistry().register(CustomNpcs.MODID + ":npcmailbox", mailbox = new BlockMailbox(0));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":npcmailbox2", mailbox2 = new BlockMailbox(1));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":npcmailbox3", mailbox3 = new BlockMailbox(2));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":npcwaypoint", waypoint = new BlockWaypoint());
         event.getForgeRegistry().register(CustomNpcs.MODID + ":npcborder", border = new BlockBorder());
         event.getForgeRegistry().register(CustomNpcs.MODID + ":npcscripted", scripted = new BlockScripted());
         event.getForgeRegistry().register(CustomNpcs.MODID + ":npcscripteddoor", scripted_door = new BlockScriptedDoor());
         event.getForgeRegistry().register(CustomNpcs.MODID + ":npcbuilderblock", builder = new BlockBuilder());
         event.getForgeRegistry().register(CustomNpcs.MODID + ":npccopyblock", copy = new BlockCopy());
         event.getForgeRegistry().register(CustomNpcs.MODID + ":npccarpentybench", carpenty = new BlockCarpentryBench());

         // Custom Blocks
         List<String> names = new ArrayList<>();
         names.add(CustomNpcs.MODID + ":npcredstoneblock");
         names.add(CustomNpcs.MODID + ":npcmailbox");
         names.add(CustomNpcs.MODID + ":npcmailbox2");
         names.add(CustomNpcs.MODID + ":npcmailbox3");
         names.add(CustomNpcs.MODID + ":npcwaypoint");
         names.add(CustomNpcs.MODID + ":npcborder");
         names.add(CustomNpcs.MODID + ":npcscripted");
         names.add(CustomNpcs.MODID + ":npcscripteddoor");
         names.add(CustomNpcs.MODID + ":npcbuilderblock");
         names.add(CustomNpcs.MODID + ":npccopyblock");
         names.add(CustomNpcs.MODID + ":npccarpentybench");
         File blocksFile = new File(CustomNpcs.Dir, "custom_blocks.js");
         CompoundTag nbtBlocks = getBlocksNbt(blocksFile);
         boolean resave = nbtBlocks.getBoolean("resave");
         nbtBlocks.remove("resave");

         for (int i = 0; i < nbtBlocks.getList("Blocks", 10).size(); i++) {
            CompoundTag nbtBlock = nbtBlocks.getList("Blocks", 10).getCompound(i);
            if (!nbtBlock.contains("RegistryName", 8) || !nbtBlock.contains("BlockType", 1)
                    || nbtBlock.getString("RegistryName").isEmpty() || nbtBlock.getByte("BlockType") < (byte) 0
                    || nbtBlock.getByte("BlockType") > (byte) 6) {
               LogWriter.error("Attempt to load block pos: " + i + "; name: \"" + nbtBlock.getString("RegistryName") + "\" - failed");
               continue;
            }
            String preName = nbtBlock.getString("RegistryName");
            String name = NoppesUtilServer.validPath(preName);
            if (!preName.equals(name)) {
               nbtBlock.putString("RegistryName", name);
               resave = true;
            }
            String location = CustomNpcs.MODID + ":custom_" + name;
            if (!nbtBlock.contains("Properties", 10)) {
               nbtBlock.put("Properties", new CompoundTag());
               resave = true;
            }
            if (nbtBlock.getByte("BlockType") != (byte) 1 &&
                    nbtBlock.getCompound("Properties").contains("liquid", 1) &&
                    nbtBlock.getCompound("Properties").getBoolean("liquid")) {
               nbtBlock.getCompound("Properties").putBoolean("liquid", false);
               resave = true;
            }
            registryNbt = nbtBlock;
            BlockBehaviour.Properties prop = getProperty(nbtBlock);
            switch (nbtBlock.getByte("BlockType")) {
               case (byte) 1: {
                  if (name.equals("liquidexample") && !nbtBlock.contains("-Description", 8)) {
                     nbtBlock.putString("-Description", ModData.getExampleLiquid().getString("-Description"));
                     resave = true;
                  }
                  ICustomElement element = customfluid.get(CustomNpcs.MODID + ":custom_fluid_" + name);
                  if (element instanceof CustomFluid fluid && fluid.getBlock() != null) {
                     registryBlock(location, fluid.getBlock(), names, nbtBlock.getBoolean("CreateDefaultFiles"), event.getForgeRegistry());
                     registryBlock(location + "_cauldron",
                             new CustomCauldronBlock(BlockBehaviour.Properties.copy(Blocks.WATER_CAULDRON),
                                     CauldronInteraction.newInteractionMap(), fluid, nbtBlock),
                             names, nbtBlock.getBoolean("CreateDefaultFiles"), event.getForgeRegistry());

                  }
                  break;
               } // Liquid
               case (byte) 2: {
                  if (nbtBlock.contains("SoundOpen", 8)) {
                     String n = nbtBlock.getString("SoundOpen");
                     String v = NoppesUtilServer.validPath(n);
                     if (!n.equals(v)) {
                        nbtBlock.putString("SoundOpen", v);
                        resave = true;
                     }
                  }
                  if (nbtBlock.contains("SoundClose", 8)) {
                     String n = nbtBlock.getString("SoundClose");
                     String v = NoppesUtilServer.validPath(n);
                     if (!n.equals(v)) {
                        nbtBlock.putString("SoundClose", v);
                        resave = true;
                     }
                  }
                  if (nbtBlock.contains("-Description", 8)) {
                     if (name.equals("chestexample")) {
                        nbtBlock.putString("-Description", ModData.getExampleChest().getString("-Description"));
                        nbtBlock.putBoolean("IsChest", true);
                     }
                     else {
                        nbtBlock.putString("-Description", ModData.getExampleContainer().getString("-Description"));
                        nbtBlock.remove("IsChest");
                     }
                     resave = true;
                  }
                  registryBlock(location, new CustomChest(prop, nbtBlock), names, nbtBlock.getBoolean("CreateDefaultFiles"), event.getForgeRegistry());
                  break;
               } // Chest
               case (byte) 3: {
                  if (name.equals("stairsexample") && !nbtBlock.contains("-Description", 8)) {
                     nbtBlock.putString("-Description", ModData.getExampleStairs().getString("-Description"));
                     resave = true;
                  }
                  Block planks = null;
                  if (nbtBlock.contains("Planks", 8)) {
                     planks = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(nbtBlock.getString("Planks")));
                  }
                  if (planks == null) { planks = Blocks.OAK_PLANKS; }
                  registryBlock(location, new CustomBlockStairs(planks.defaultBlockState(), prop, nbtBlock), names,
                          nbtBlock.getBoolean("CreateDefaultFiles"), event.getForgeRegistry());
                  break;
               } // Stairs
               case (byte) 4: {
                  if (name.equals("slabexample") && !nbtBlock.contains("-Description", 8)) {
                     nbtBlock.putString("-Description", ModData.getExampleSlab().getString("-Description"));
                     resave = true;
                  }
                  registryBlock(location, new CustomBlockSlab(prop, nbtBlock), names,
                          nbtBlock.getBoolean("CreateDefaultFiles"), event.getForgeRegistry());
                  break;
               } // Slab
               case (byte) 5: {
                  if (nbtBlock.contains("DimensionID", 8)) {
                     String n = nbtBlock.getString("DimensionID");
                     String v = NoppesUtilServer.validPath(n);
                     if (!n.equals(v)) {
                        nbtBlock.putString("DimensionID", v);
                        resave = true;
                     }
                  }
                  if (nbtBlock.contains("HomeDimensionID", 8)) {
                     String n = nbtBlock.getString("HomeDimensionID");
                     String v = NoppesUtilServer.validPath(n);
                     if (!n.equals(v)) {
                        nbtBlock.putString("HomeDimensionID", v);
                        resave = true;
                     }
                  }
                  if (nbtBlock.contains("RenderData", 10) && nbtBlock.getCompound("RenderData").contains("SpawnParticle")) {
                     String n = nbtBlock.getCompound("RenderData").getString("SpawnParticle");
                     String v = NoppesUtilServer.validPath(n);
                     if (!n.equals(v)) {
                        nbtBlock.getCompound("RenderData").putString("SpawnParticle", v);
                        resave = true;
                     }
                  }
                  if (name.equals("portalexample") && !nbtBlock.contains("-Description", 8)) {
                     nbtBlock.putString("-Description", ModData.getExamplePortal().getString("-Description"));
                     resave = true;
                  }
                  registryBlock(location, new CustomBlockPortal(prop, nbtBlock), names,
                          nbtBlock.getBoolean("CreateDefaultFiles"), event.getForgeRegistry());
                  break;
               } // Portal
               case (byte) 6: {
                  if (name.equals("doorexample") && !nbtBlock.contains("-Description", 8)) {
                     nbtBlock.putString("-Description", ModData.getExampleDoor().getString("-Description"));
                     resave = true;
                  }
                  registryBlock(location, new CustomDoor(prop, getSetType(nbtBlock), nbtBlock), names,
                          nbtBlock.getBoolean("CreateDefaultFiles"), event.getForgeRegistry());
                  break;
               } // Door
               default: {
                  if (name.equals("blockexample") && nbtBlock.contains("-Description", 8)) {
                     nbtBlock.putString("-Description", ModData.getExampleBlock().getString("-Description"));
                     resave = true;
                  }
                  registryBlock(location, new CustomBlock(prop, nbtBlock), names,
                          nbtBlock.getBoolean("CreateDefaultFiles"), event.getForgeRegistry());
                  break;
               } // Simple
            }
            if (nbtBlock.getBoolean("CreateDefaultFiles")) {
               nbtBlock.remove("CreateDefaultFiles");
               resave = true;
            }
         }
         if (resave) { Util.instance.saveFile(blocksFile, nbtBlocks); }
         registryNbt = null;
         // Sorting:
         List<Map.Entry<ICustomElement, Item>> entries = new ArrayList<>(customblocks.entrySet());
         entries.sort(Comparator.comparing((Map.Entry<ICustomElement, Item> entry) -> entry.getKey().getElementType())
                 .thenComparing((entry) -> entry.getKey().getCustomName()));
         customblocks.clear();
         for (Map.Entry<ICustomElement, Item> entry : entries) { customblocks.put(entry.getKey(), entry.getValue()); }
      } // 2
      if (event.getRegistryKey() == Keys.BLOCK_ENTITY_TYPES) {
         event.getForgeRegistry().register(CustomNpcs.MODID + ":tileblockanvil", tile_anvil = (BlockEntityType<TileBlockAnvil>) createTile(TileBlockAnvil::new, carpenty));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":tilenpcborder", tile_border = (BlockEntityType<TileBorder>) createTile(TileBorder::new, border));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":tilenpcbuilder", tile_builder = (BlockEntityType<TileBuilder>) createTile(TileBuilder::new, builder));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":tilenpccopy", tile_copy = (BlockEntityType<TileCopy>) createTile(TileCopy::new, copy));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":tilemailbox", tile_mailbox = (BlockEntityType<TileMailbox>) createTile(TileMailbox::new, mailbox, mailbox2, mailbox3));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":tileredstoneblock", tile_redstoneblock = (BlockEntityType<TileRedstoneBlock>) createTile(TileRedstoneBlock::new, redstone));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":tilenpcscripted", tile_scripted = (BlockEntityType<TileScripted>) createTile(TileScripted::new, scripted));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":tilenpcscripteddoor", tile_scripteddoor = (BlockEntityType<TileScriptedDoor>) createTile(TileScriptedDoor::new, scripted_door));
         event.getForgeRegistry().register(CustomNpcs.MODID + ":tilewaypoint", tile_waypoint = (BlockEntityType<TileWaypoint>) createTile(TileWaypoint::new, waypoint));
         // custom
         List<CustomBlockPortal> portals = new ArrayList<>();
         List<CustomChest> chests = new ArrayList<>();
         for (ICustomElement element : customblocks.keySet()) {
            if (element instanceof CustomBlockPortal portal) { portals.add(portal); }
            else if (element instanceof CustomChest chest) { chests.add(chest); }
         }
         if (!portals.isEmpty()) {
            event.getForgeRegistry().register(CustomNpcs.MODID + ":tilecustomportal",
                    tile_custom_portal = (BlockEntityType<CustomTileEntityPortal>) createTile(CustomTileEntityPortal::new, portals.toArray(new Block[0])));
         }
         if (!chests.isEmpty()) {
            event.getForgeRegistry().register(CustomNpcs.MODID + ":tilecustomchest",
                    tile_custom_chest = (BlockEntityType<CustomTileEntityChest>) createTile(CustomTileEntityChest::new, chests.toArray(new Block[0])));
         }
      } // 12
      CustomNpcs.debugData.end("Mod");
   }

   public static CompoundTag getBlocksNbt(File file) {
      CompoundTag compound = ModData.getExampleBlocks().copy();
      ListTag listBlocks = compound.getList("Blocks", 10);

      CompoundTag nbtInFile = new CompoundTag();
      boolean resave = false;
      try { if (file.exists()) { nbtInFile = NBTJsonUtil.LoadFile(file); } }
      catch (Exception e) { LogWriter.error("Try Load " + file.getName() + ": ", e); }

      List<String> names = new ArrayList<>();
      ListTag listInFile = nbtInFile.getList("Blocks", 10);
      ListTag exampleBlocks = listBlocks.copy();
      for (int i = 0; i < listInFile.size(); i++) {
         CompoundTag nbtBlock = listInFile.getCompound(i);
         String name = nbtBlock.getString("RegistryName");
         boolean isExample = false;
         for (int j = 0; j < exampleBlocks.size(); j++) {
            if (name.equals(exampleBlocks.getCompound(j).getString("RegistryName"))) {
               isExample = true;
               break;
            }
         }
         if (names.contains(name)) {
            if (isExample) {
               name = name.replace("example", "custom");
               isExample = false;
            }
            while (names.contains(name)) { name += "_"; }
            nbtBlock.putString("RegistryName", name);
            resave = true;
         }
         names.add(name);
         if (!isExample) { listBlocks.add(nbtBlock); }
      }
      compound.putBoolean("resave", resave);
      return compound;
   }

   private static void registryBlock(String location, Block block, List<String> names, boolean defFiles, IForgeRegistry<Block> registry) {
      if (names.contains(location) || Block.getId(block.defaultBlockState()) > 0 || !(block instanceof ICustomElement element)) {
         LogWriter.error("Attempt to load a registered block \"" + location + "\"");
         return;
      }
      boolean isExample = location.endsWith(":custom_blockexample") || location.endsWith(":custom_liquidexample") ||
              location.endsWith(":custom_facingblockexample") || location.endsWith(":custom_stairsexample") ||
              location.endsWith(":custom_slabexample") || location.endsWith(":custom_portalexample") || location.endsWith(":custom_chestexample")
              || location.endsWith(":custom_containerexample") || location.endsWith(":custom_doorexample");
      if (isExample || defFiles) { CustomNpcs.proxy.createAllFiles((ICustomElement) block); }
      LogWriter.info("Load Custom Block \"" + location + "\"");
      if (!(block instanceof CustomBlockLiquid)) {
         if (block instanceof CustomDoor door) { customblocks.put(element, new DoubleHighBlockItem(door, new Properties())); }
         else { customblocks.put(element, new ItemNpcBlock(block, new Properties())); }
      }
      names.add(location);
      registry.register(location, block);
   }

   @SuppressWarnings("deprecation")
   public static BlockBehaviour.Properties getProperty(CompoundTag nbtBlock) {
      BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();
      CompoundTag nbtProperties = nbtBlock.getCompound("Properties");

      if (nbtProperties.contains("noCollission", 1) && nbtProperties.getBoolean("noCollission")) { properties.noCollission(); }
      if (nbtProperties.contains("noOcclusion", 1) && nbtProperties.getBoolean("noOcclusion")) { properties.noOcclusion(); }
      if (nbtProperties.contains("randomTicks", 1) && nbtProperties.getBoolean("randomTicks")) { properties.randomTicks(); }
      if (nbtProperties.contains("dynamicShape", 1) && nbtProperties.getBoolean("dynamicShape")) { properties.dynamicShape(); }
      if (nbtProperties.contains("ignitedByLava", 1) && nbtProperties.getBoolean("ignitedByLava")) { properties.ignitedByLava(); }
      if (nbtProperties.contains("liquid", 1) && nbtProperties.getBoolean("liquid")) { properties.liquid(); }
      if (nbtProperties.contains("forceSolidOn", 1) && nbtProperties.getBoolean("forceSolidOn")) { properties.forceSolidOn(); }
      if (nbtProperties.contains("forceSolidOff", 1) && nbtProperties.getBoolean("forceSolidOff")) { properties.forceSolidOff(); }
      if (nbtProperties.contains("isAir", 1) && nbtProperties.getBoolean("isAir")) { properties.air(); }
      if (nbtProperties.contains("noParticlesOnBreak", 1) && nbtProperties.getBoolean("noParticlesOnBreak")) { properties.noParticlesOnBreak(); }
      if (nbtProperties.contains("replaceable", 1) && nbtProperties.getBoolean("replaceable")) { properties.replaceable(); }
      if (nbtProperties.contains("noLootTable", 1) && nbtProperties.getBoolean("noLootTable")) { properties.noLootTable(); }
      if (nbtProperties.contains("requiresCorrectToolForDrops", 1) && nbtProperties.getBoolean("requiresCorrectToolForDrops")) { properties.requiresCorrectToolForDrops(); }

      if (nbtProperties.contains("lightLevel", 3)) { properties.lightLevel((state) -> nbtProperties.getInt("lightLevel")); }

      if (nbtProperties.contains("friction", 5)) { properties.friction(ValueUtil.correctFloat(nbtProperties.getFloat("friction"), 0.0f, 1.0f)); }
      if (nbtProperties.contains("speedFactor", 5)) { properties.speedFactor(ValueUtil.correctFloat(nbtProperties.getFloat("speedFactor"), 0.05f, 10.0f)); }
      if (nbtProperties.contains("jumpFactor", 5)) { properties.jumpFactor(ValueUtil.correctFloat(nbtProperties.getFloat("jumpFactor"), 0.05f, 10.0f)); }
      if (nbtProperties.contains("destroyTime", 5)) { properties.destroyTime(nbtProperties.getFloat("destroyTime")); }
      if (nbtProperties.contains("explosionResistance", 5)) { properties.explosionResistance(nbtProperties.getFloat("explosionResistance")); }

      if (nbtProperties.contains("mapColor", 8)) { properties.mapColor(getMapColor(nbtProperties.getString("mapColor").toLowerCase())); }
      if (nbtProperties.contains("mapColor", 3)) {
         try {
            Constructor<MapColor> constructor = MapColor.class.getDeclaredConstructor(int.class, int.class);
            constructor.trySetAccessible();
            properties.mapColor(constructor.newInstance(62, nbtProperties.getInt("mapColor")));
         }
         catch (Exception e) { LogWriter.error(e); }
      }

      if (nbtProperties.contains("sound", 8)) { properties.sound(getSound(nbtProperties.getString("sound").toLowerCase())); }
      if (nbtProperties.contains("sound", 10)) {
         CompoundTag nbt = nbtProperties.getCompound("sound");
         SoundEvent breakSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(nbt.getString("breakSound")));
         if (breakSound == null) { breakSound = SoundEvents.EMPTY; }
         SoundEvent stepSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(nbt.getString("stepSound")));
         if (stepSound == null) { stepSound = SoundEvents.EMPTY; }
         SoundEvent placeSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(nbt.getString("placeSound")));
         if (placeSound == null) { placeSound = SoundEvents.EMPTY; }
         SoundEvent hitSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(nbt.getString("hitSound")));
         if (hitSound == null) { hitSound = SoundEvents.EMPTY; }
         SoundEvent fallSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(nbt.getString("fallSound")));
         if (fallSound == null) { fallSound = SoundEvents.EMPTY; }
         properties.sound(new SoundType(nbt.contains("volume", 5) ? ValueUtil.correctFloat(nbt.getFloat("volume"), 0.05f, 5.0f) : 1.0f,
                 nbt.contains("pitch", 5) ? ValueUtil.correctFloat(nbt.getFloat("volume"), 0.05f, 5.0f) : 1.0f,
                 breakSound,
                 stepSound,
                 placeSound,
                 hitSound,
                 fallSound));
      }
      if (nbtProperties.contains("pushReaction", 8)) { properties.pushReaction(getPushReaction(nbtProperties.getString("pushReaction").toLowerCase())); }

      if (nbtProperties.contains("isValidSpawn", 8)) {
         switch (nbtProperties.getString("isValidSpawn").toLowerCase()) {
            case "always" -> properties.isValidSpawn((state, level, pos, entityType) -> true);
            case "never" -> properties.isValidSpawn((state, level, pos, entityType) -> false);
            case "ocelotorparrot" -> properties.isValidSpawn((state, level, pos, entityType) -> entityType == EntityType.OCELOT || entityType == EntityType.PARROT);
            case "polarbear" -> properties.isValidSpawn((state, level, pos, entityType) -> entityType == EntityType.POLAR_BEAR);
            case "fireimmune" -> properties.isValidSpawn((state, level, pos, entityType) -> entityType.fireImmune());
            default -> properties.isValidSpawn((state, level, pos, entityType) -> {
               String name = NoppesUtilServer.validPath(nbtProperties.getString("isValidSpawn"));
               EntityType<?> eType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(name));
               return eType != null ? entityType == eType : state.isFaceSturdy(level, pos, Direction.UP) && state.getLightEmission(level, pos) < 14;
            });
         }
      }
      if (nbtProperties.contains("isRedstoneConductor", 8)) {
         switch (nbtProperties.getString("isRedstoneConductor").toLowerCase()) {
            case "always" -> properties.isRedstoneConductor((state, level, pos) -> true);
            case "never" -> properties.isRedstoneConductor((state, level, pos) -> false);
         }
      }
      if (nbtProperties.contains("isSuffocating", 8)) {
         switch (nbtProperties.getString("isSuffocating").toLowerCase()) {
            case "never" -> properties.isSuffocating((state, level, pos) -> false);
            case "pistonbase" -> properties.isSuffocating((state, level, pos) -> !state.getValue(PistonBaseBlock.EXTENDED));
            case "shulkerbox" -> properties.isSuffocating((state, level, pos) -> {
               if (level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity tile) {
                  return tile.isClosed();
               }
               return true;
            });
         }
      }
      if (nbtProperties.contains("isViewBlocking", 8)) {
         switch (nbtProperties.getString("isViewBlocking").toLowerCase()) {
            case "always" -> properties.isViewBlocking((state, level, pos) -> true);
            case "never" -> properties.isViewBlocking((state, level, pos) -> false);
            case "snowlayers" -> properties.isViewBlocking((state, level, pos) -> state.getValue(SnowLayerBlock.LAYERS) >= 8);
            case "pistonbase" -> properties.isViewBlocking((state, level, pos) -> !state.getValue(PistonBaseBlock.EXTENDED));
            case "shulkerbox" -> properties.isViewBlocking((state, level, pos) -> {
               if (level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity tile) {
                  return tile.isClosed();
               }
               return true;
            });
         }
      }
      if (nbtProperties.contains("hasPostProcess", 8) && nbtProperties.getString("hasPostProcess").equalsIgnoreCase("always")) {
         properties.hasPostProcess((state, level, pos) -> true);
      }
      if (nbtProperties.contains("emissiveRendering", 8)) {
         switch (nbtProperties.getString("emissiveRendering").toLowerCase()) {
            case "always" -> properties.emissiveRendering((state, level, pos) -> true);
            case "sculksensorblock" -> properties.emissiveRendering((state, level, pos) -> SculkSensorBlock.getPhase(state) == SculkSensorPhase.ACTIVE);
         }
      }
      if (nbtProperties.contains("offsetType", 8)) {
         switch (nbtProperties.getString("offsetType").toLowerCase()) {
            case "xz" -> properties.offsetType(BlockBehaviour.OffsetType.XZ);
            case "xyz" -> properties.offsetType(BlockBehaviour.OffsetType.XYZ);
            default ->  properties.offsetType(BlockBehaviour.OffsetType.NONE);
         }
      }
      if (nbtProperties.contains("instrument", 8)) {
         switch (nbtProperties.getString("instrument").toLowerCase()) {
            case "harp" -> properties.instrument(NoteBlockInstrument.HARP);
            case "basedrum" -> properties.instrument(NoteBlockInstrument.BASEDRUM);
            case "snare" -> properties.instrument(NoteBlockInstrument.SNARE);
            case "hat" -> properties.instrument(NoteBlockInstrument.HAT);
            case "bass" -> properties.instrument(NoteBlockInstrument.BASS);
            case "flute" -> properties.instrument(NoteBlockInstrument.FLUTE);
            case "bell" -> properties.instrument(NoteBlockInstrument.BELL);
            case "guitar" -> properties.instrument(NoteBlockInstrument.GUITAR);
            case "chime" -> properties.instrument(NoteBlockInstrument.CHIME);
            case "xylophone" -> properties.instrument(NoteBlockInstrument.XYLOPHONE);
            case "iron_xylophone" -> properties.instrument(NoteBlockInstrument.IRON_XYLOPHONE);
            case "cow_bell" -> properties.instrument(NoteBlockInstrument.COW_BELL);
            case "didgeridoo" -> properties.instrument(NoteBlockInstrument.DIDGERIDOO);
            case "bit" -> properties.instrument(NoteBlockInstrument.BIT);
            case "banjo" -> properties.instrument(NoteBlockInstrument.BANJO);
            case "pling" -> properties.instrument(NoteBlockInstrument.PLING);
            case "zombie" -> properties.instrument(NoteBlockInstrument.ZOMBIE);
            case "skeleton" -> properties.instrument(NoteBlockInstrument.SKELETON);
            case "creeper" -> properties.instrument(NoteBlockInstrument.CREEPER);
            case "dragon" -> properties.instrument(NoteBlockInstrument.DRAGON);
            case "wither_skeleton" -> properties.instrument(NoteBlockInstrument.WITHER_SKELETON);
            case "piglin" -> properties.instrument(NoteBlockInstrument.PIGLIN);
            case "custom_head" -> properties.instrument(NoteBlockInstrument.CUSTOM_HEAD);
         }
      }
      if (nbtProperties.contains("lootFrom", 8)) {
         properties.lootFrom(() -> {
            @Nullable Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(nbtProperties.getString("lootFrom")));
            return block != null ? block : Blocks.AIR;
         });
      }
      if (nbtProperties.contains("lootTable", 8)) {
         ((IBlockBehaviourPropertiesMixin) properties).setDrops(new ResourceLocation(nbtProperties.getString("lootTable")));
      }

      if (nbtProperties.contains("requiredFeatures", 9)) {
         ListTag list = nbtProperties.getList("requiredFeatures", 10);
         List<FeatureFlag> featureFlags = new ArrayList<>();
         for (int i = 0; i < list.size(); i++) {
            CompoundTag nbt = list.getCompound(i);
            if (nbt.contains("UniverseId", 8) && nbt.contains("Name", 8)) {
               FeatureFlagRegistry.Builder b = new FeatureFlagRegistry.Builder(nbt.getString("UniverseId"));
               featureFlags.add(b.create(new ResourceLocation(CustomNpcs.MODID, nbt.getString("Name"))));
            }
         }
         if (!featureFlags.isEmpty()) { properties.requiredFeatures(featureFlags.toArray(new FeatureFlag[0])); }
      }

      return properties;
   }

   private static BlockSetType getSetType(CompoundTag nbtBlock) {
      CompoundTag blockSetType = nbtBlock.getCompound("BlockSetType");
      if (blockSetType.contains("Name", 8)) {
         String name = blockSetType.getString("Name");
         Optional<BlockSetType> bst = BlockSetType.values().filter(setType -> setType.name().equals(name)).findAny();
         if (bst.isPresent()) { return bst.get(); }

         SoundType soundType = getSound(blockSetType.getString("SoundType"));
         if (soundType == SoundType.EMPTY) { soundType = SoundType.WOOD; }
         SoundEvent doorClose = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(blockSetType.getString("SoundDoorClose")));
         if (doorClose == null) { doorClose = SoundEvents.WOODEN_DOOR_CLOSE; }
         SoundEvent doorOpen = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(blockSetType.getString("SoundDoorOpen")));
         if (doorOpen == null) { doorOpen = SoundEvents.WOODEN_DOOR_OPEN; }
         SoundEvent trapDoorClose = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(blockSetType.getString("SoundTrapDoorClose")));
         if (trapDoorClose == null) { trapDoorClose = SoundEvents.WOODEN_TRAPDOOR_CLOSE; }
         SoundEvent trapDoorOpen = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(blockSetType.getString("SoundTrapDoorOpen")));
         if (trapDoorOpen == null) { trapDoorOpen = SoundEvents.WOODEN_TRAPDOOR_OPEN; }
         SoundEvent plateClickOff = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(blockSetType.getString("SoundPlateClickOff")));
         if (plateClickOff == null) { plateClickOff = SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_OFF; }
         SoundEvent plateClickOn = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(blockSetType.getString("SoundPlateClickOn")));
         if (plateClickOn == null) { plateClickOn = SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON; }
         SoundEvent buttonClickOff = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(blockSetType.getString("SoundButtonClickOff")));
         if (buttonClickOff == null) { buttonClickOff = SoundEvents.WOODEN_BUTTON_CLICK_OFF; }
         SoundEvent buttonClickOn = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(blockSetType.getString("SoundButtonClickOn")));
         if (buttonClickOn == null) { buttonClickOn = SoundEvents.WOODEN_BUTTON_CLICK_ON; }
         return new BlockSetType(name,
                 !blockSetType.contains("CanOpenByHand", 1) || blockSetType.getBoolean("CanOpenByHand"),
                 soundType, doorClose, doorOpen, trapDoorClose, trapDoorOpen, plateClickOff, plateClickOn, buttonClickOff, buttonClickOn);
      }
      return BlockSetType.OAK;
   }

   @SuppressWarnings("ConstantConditions")
   private static BlockEntityType<?> createTile(BlockEntitySupplier<?> factoryIn, Block... blocks) {
      return Builder.of(factoryIn, blocks).build(null);
   }

   private static MapColor getMapColor(@Nonnull String name) {
      return switch (name) {
         case "grass" -> MapColor.GRASS;
         case "sand" -> MapColor.SAND;
         case "wool" -> MapColor.WOOL;
         case "fire" -> MapColor.FIRE;
         case "ice" -> MapColor.ICE;
         case "metal" -> MapColor.METAL;
         case "plant" -> MapColor.PLANT;
         case "snow" -> MapColor.SNOW;
         case "clay" -> MapColor.CLAY;
         case "dirt" -> MapColor.DIRT;
         case "stone" -> MapColor.STONE;
         case "water" -> MapColor.WATER;
         case "wood" -> MapColor.WOOD;
         case "quartz" -> MapColor.QUARTZ;
         case "color_light_blue" -> MapColor.COLOR_LIGHT_BLUE;
         case "color_light_green" -> MapColor.COLOR_LIGHT_GREEN;
         case "color_light_gray" -> MapColor.COLOR_LIGHT_GRAY;
         case "color_orange" -> MapColor.COLOR_ORANGE;
         case "color_magenta" -> MapColor.COLOR_MAGENTA;
         case "color_yellow" -> MapColor.COLOR_YELLOW;
         case "color_pink" -> MapColor.COLOR_PINK;
         case "color_gray" -> MapColor.COLOR_GRAY;
         case "color_cyan" -> MapColor.COLOR_CYAN;
         case "color_purple" -> MapColor.COLOR_PURPLE;
         case "color_blue" -> MapColor.COLOR_BLUE;
         case "color_brown" -> MapColor.COLOR_BROWN;
         case "color_green" -> MapColor.COLOR_GREEN;
         case "color_red" -> MapColor.COLOR_RED;
         case "color_black" -> MapColor.COLOR_BLACK;
         case "gold" -> MapColor.GOLD;
         case "diamond" -> MapColor.DIAMOND;
         case "lapis" -> MapColor.LAPIS;
         case "emerald" -> MapColor.EMERALD;
         case "podzol" -> MapColor.PODZOL;
         case "nether" -> MapColor.NETHER;
         case "terracotta_light_blue" -> MapColor.TERRACOTTA_LIGHT_BLUE;
         case "terracotta_light_green" -> MapColor.TERRACOTTA_LIGHT_GREEN;
         case "terracotta_light_gray" -> MapColor.TERRACOTTA_LIGHT_GRAY;
         case "terracotta_white" -> MapColor.TERRACOTTA_WHITE;
         case "terracotta_orange" -> MapColor.TERRACOTTA_ORANGE;
         case "terracotta_magenta" -> MapColor.TERRACOTTA_MAGENTA;
         case "terracotta_yellow" -> MapColor.TERRACOTTA_YELLOW;
         case "terracotta_pink" -> MapColor.TERRACOTTA_PINK;
         case "terracotta_gray" -> MapColor.TERRACOTTA_GRAY;
         case "terracotta_cyan" -> MapColor.TERRACOTTA_CYAN;
         case "terracotta_purple" -> MapColor.TERRACOTTA_PURPLE;
         case "terracotta_blue" -> MapColor.TERRACOTTA_BLUE;
         case "terracotta_brown" -> MapColor.TERRACOTTA_BROWN;
         case "terracotta_green" -> MapColor.TERRACOTTA_GREEN;
         case "terracotta_red" -> MapColor.TERRACOTTA_RED;
         case "terracotta_black" -> MapColor.TERRACOTTA_BLACK;
         case "crimson_nylium" -> MapColor.CRIMSON_NYLIUM;
         case "crimson_stem" -> MapColor.CRIMSON_STEM;
         case "crimson_hyphae" -> MapColor.CRIMSON_HYPHAE;
         case "warped_nylium" -> MapColor.WARPED_NYLIUM;
         case "warped_stem" -> MapColor.WARPED_STEM;
         case "warped_hyphae" -> MapColor.WARPED_HYPHAE;
         case "warped_wart_block" -> MapColor.WARPED_WART_BLOCK;
         case "deepslate" -> MapColor.DEEPSLATE;
         case "raw_iron" -> MapColor.RAW_IRON;
         case "glow_lichen" -> MapColor.GLOW_LICHEN;
         default -> MapColor.NONE;
      };
   }

   private static SoundType getSound(@Nonnull String name) {
      return switch (name) {
         case "wood" -> SoundType.WOOD;
         case "gravel" -> SoundType.GRAVEL;
         case "grass" -> SoundType.GRASS;
         case "lily_pad" -> SoundType.LILY_PAD;
         case "stone" -> SoundType.STONE;
         case "metal" -> SoundType.METAL;
         case "glass" -> SoundType.GLASS;
         case "wool" -> SoundType.WOOL;
         case "sand" -> SoundType.SAND;
         case "snow" -> SoundType.SNOW;
         case "powder_snow" -> SoundType.POWDER_SNOW;
         case "ladder" -> SoundType.LADDER;
         case "anvil" -> SoundType.ANVIL;
         case "slime_block" -> SoundType.SLIME_BLOCK;
         case "honey_block" -> SoundType.HONEY_BLOCK;
         case "wet_grass" -> SoundType.WET_GRASS;
         case "coral_block" -> SoundType.CORAL_BLOCK;
         case "bamboo" -> SoundType.BAMBOO;
         case "bamboo_sapling" -> SoundType.BAMBOO_SAPLING;
         case "scaffolding" -> SoundType.SCAFFOLDING;
         case "sweet_berry_bush" -> SoundType.SWEET_BERRY_BUSH;
         case "crop" -> SoundType.CROP;
         case "hard_crop" -> SoundType.HARD_CROP;
         case "vine" -> SoundType.VINE;
         case "nether_wart" -> SoundType.NETHER_WART;
         case "lantern" -> SoundType.LANTERN;
         case "stem" -> SoundType.STEM;
         case "nylium" -> SoundType.NYLIUM;
         case "fungus" -> SoundType.FUNGUS;
         case "roots" -> SoundType.ROOTS;
         case "shroomlight" -> SoundType.SHROOMLIGHT;
         case "weeping_vines" -> SoundType.WEEPING_VINES;
         case "twisting_vines" -> SoundType.TWISTING_VINES;
         case "soul_sand" -> SoundType.SOUL_SAND;
         case "soul_soil" -> SoundType.SOUL_SOIL;
         case "basalt" -> SoundType.BASALT;
         case "wart_block" -> SoundType.WART_BLOCK;
         case "netherrack" -> SoundType.NETHERRACK;
         case "nether_bricks" -> SoundType.NETHER_BRICKS;
         case "nether_sprouts" -> SoundType.NETHER_SPROUTS;
         case "nether_ore" -> SoundType.NETHER_ORE;
         case "bone_block" -> SoundType.BONE_BLOCK;
         case "netherite_block" -> SoundType.NETHERITE_BLOCK;
         case "ancient_debris" -> SoundType.ANCIENT_DEBRIS;
         case "lodestone" -> SoundType.LODESTONE;
         case "chain" -> SoundType.CHAIN;
         case "nether_gold_ore" -> SoundType.NETHER_GOLD_ORE;
         case "gilded_blackstone" -> SoundType.GILDED_BLACKSTONE;
         case "candle" -> SoundType.CANDLE;
         case "amethyst" -> SoundType.AMETHYST;
         case "amethyst_cluster" -> SoundType.AMETHYST_CLUSTER;
         case "small_amethyst_bud" -> SoundType.SMALL_AMETHYST_BUD;
         case "medium_amethyst_bud" -> SoundType.MEDIUM_AMETHYST_BUD;
         case "large_amethyst_bud" -> SoundType.LARGE_AMETHYST_BUD;
         case "tuff" -> SoundType.TUFF;
         case "calcite" -> SoundType.CALCITE;
         case "dripstone_block" -> SoundType.DRIPSTONE_BLOCK;
         case "pointed_dripstone" -> SoundType.POINTED_DRIPSTONE;
         case "copper" -> SoundType.COPPER;
         case "cave_vines" -> SoundType.CAVE_VINES;
         case "spore_blossom" -> SoundType.SPORE_BLOSSOM;
         case "azalea" -> SoundType.AZALEA;
         case "flowering_azalea" -> SoundType.FLOWERING_AZALEA;
         case "moss_carpet" -> SoundType.MOSS_CARPET;
         case "pink_petals" -> SoundType.PINK_PETALS;
         case "moss" -> SoundType.MOSS;
         case "big_dripleaf" -> SoundType.BIG_DRIPLEAF;
         case "small_dripleaf" -> SoundType.SMALL_DRIPLEAF;
         case "rooted_dirt" -> SoundType.ROOTED_DIRT;
         case "hanging_roots" -> SoundType.HANGING_ROOTS;
         case "azalea_leaves" -> SoundType.AZALEA_LEAVES;
         case "sculk_sensor" -> SoundType.SCULK_SENSOR;
         case "sculk_catalyst" -> SoundType.SCULK_CATALYST;
         case "sculk" -> SoundType.SCULK;
         case "sculk_vein" -> SoundType.SCULK_VEIN;
         case "sculk_shrieker" -> SoundType.SCULK_SHRIEKER;
         case "glow_lichen" -> SoundType.GLOW_LICHEN;
         case "deepslate" -> SoundType.DEEPSLATE;
         case "deepslate_bricks" -> SoundType.DEEPSLATE_BRICKS;
         case "deepslate_tiles" -> SoundType.DEEPSLATE_TILES;
         case "polished_deepslate" -> SoundType.POLISHED_DEEPSLATE;
         case "froglight" -> SoundType.FROGLIGHT;
         case "frogspawn" -> SoundType.FROGSPAWN;
         case "mangrove_roots" -> SoundType.MANGROVE_ROOTS;
         case "muddy_mangrove_roots" -> SoundType.MUDDY_MANGROVE_ROOTS;
         case "mud" -> SoundType.MUD;
         case "mud_bricks" -> SoundType.MUD_BRICKS;
         case "packed_mud" -> SoundType.PACKED_MUD;
         case "hanging_sing" -> SoundType.HANGING_SIGN;
         case "nether_wood_hanging_sing" -> SoundType.NETHER_WOOD_HANGING_SIGN;
         case "bamboo_wood_hanging_sing" -> SoundType.BAMBOO_WOOD_HANGING_SIGN;
         case "bamboo_wood" -> SoundType.BAMBOO_WOOD;
         case "nether_wood" -> SoundType.NETHER_WOOD;
         case "cherry_wood" -> SoundType.CHERRY_WOOD;
         case "cherry_sapling" -> SoundType.CHERRY_SAPLING;
         case "cherry_leaves" -> SoundType.CHERRY_LEAVES;
         case "cherry_wood_hanging_sing" -> SoundType.CHERRY_WOOD_HANGING_SIGN;
         case "chiseled_bookshelf" -> SoundType.CHISELED_BOOKSHELF;
         case "suspicious_sand" -> SoundType.SUSPICIOUS_SAND;
         case "suspicious_gravel" -> SoundType.SUSPICIOUS_GRAVEL;
         case "decorated_pot" -> SoundType.DECORATED_POT;
         case "decorated_pot_cracked" -> SoundType.DECORATED_POT_CRACKED;
         default -> SoundType.EMPTY;
      };
   }

   private static PushReaction getPushReaction(@Nonnull String name) {
      return switch (name) {
         case "destroy" -> PushReaction.DESTROY;
         case "block" -> PushReaction.BLOCK;
         case "ignore" -> PushReaction.IGNORE;
         case "push_only" -> PushReaction.PUSH_ONLY;
         default -> PushReaction.NORMAL;
      };
   }

   public static Rarity getRrarity(@Nonnull String rarity) {
      return switch (rarity.toLowerCase()) {
         case "uncommon" -> Rarity.UNCOMMON;
         case "rare" -> Rarity.RARE;
         case "epic" -> Rarity.EPIC;
         default -> Rarity.COMMON;
      };
   }

}
