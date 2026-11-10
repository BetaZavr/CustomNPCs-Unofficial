package noppes.npcs;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.UniversalBucket;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.blocks.*;
import noppes.npcs.blocks.custom.*;
import noppes.npcs.blocks.custom.tiles.CustomTileEntityChest;
import noppes.npcs.blocks.custom.tiles.CustomTileEntityPortal;
import noppes.npcs.blocks.tiles.*;
import noppes.npcs.client.CustomTileEntityItemStackRenderer;
import noppes.npcs.client.renderer.blocks.*;
import noppes.npcs.client.renderer.item.ItemCarpentryBenchRenderer;
import noppes.npcs.client.renderer.item.ItemMailboxRenderer;
import noppes.npcs.fluids.CustomFluid;
import noppes.npcs.items.*;
import noppes.npcs.items.custom.CustomFluidBottle;
import noppes.npcs.items.custom.CustomItem;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.ModData;
import noppes.npcs.util.NBTJsonUtil;
import noppes.npcs.util.Util;

import java.io.File;
import java.util.*;
import java.util.List;

public class CustomBlocks {

    public static Block border;
    public static Block builder;
    public static Block carpenty;
    public static Block copy;
    public static Block mailbox;
    public static Block redstoneBlock;
    public static Block scripted;
    public static Block scripted_door;
    public static Block waypoint;
    // items
    public static ItemNpcBlock redstone_item;
    public static ItemNpcBlock waypoint_item;
    public static ItemNpcBlock border_item;
    public static ItemNpcBlock scripted_item;
    public static ItemNpcBlock builder_item;
    public static ItemNpcBlock copy_item;
    public static Item carpentry_item;
    public static Item mailbox_item;
    public static ItemScriptedDoor scripted_door_item;

    // custom
    private static final List<Item> items = new ArrayList<>();
    public static final Map<ICustomElement, Item> customblocks = new HashMap<>();
    public static final Map<String, ICustomElement> customfluid = new TreeMap<>();
    public static final List<CustomBlockPortal> portals = new ArrayList<>();
    public static final List<CustomChest> chests = new ArrayList<>();
    public static NBTTagCompound registryNbt;

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        GameRegistry.registerTileEntity(TileRedstoneBlock.class, new ResourceLocation(CustomNpcs.MODID, "TileRedstoneBlock"));
        GameRegistry.registerTileEntity(TileBlockAnvil.class, new ResourceLocation(CustomNpcs.MODID, "TileBlockAnvil"));
        GameRegistry.registerTileEntity(TileMailbox.class, new ResourceLocation(CustomNpcs.MODID, "TileMailbox"));
        GameRegistry.registerTileEntity(TileWaypoint.class, new ResourceLocation(CustomNpcs.MODID, "TileWaypoint"));
        GameRegistry.registerTileEntity(TileScripted.class, new ResourceLocation(CustomNpcs.MODID, "TileNPCScripted"));
        GameRegistry.registerTileEntity(TileScriptedDoor.class, new ResourceLocation(CustomNpcs.MODID, "TileNPCScriptedDoor"));
        GameRegistry.registerTileEntity(TileBuilder.class, new ResourceLocation(CustomNpcs.MODID, "TileNPCBuilder"));
        GameRegistry.registerTileEntity(TileCopy.class, new ResourceLocation(CustomNpcs.MODID, "TileNPCCopy"));
        GameRegistry.registerTileEntity(TileBorder.class, new ResourceLocation(CustomNpcs.MODID, "TileNPCBorder"));
        GameRegistry.registerTileEntity(TileDoor.class, new ResourceLocation(CustomNpcs.MODID, "TileNPCDoor")); // Only Render
        GameRegistry.registerTileEntity(CustomTileEntityPortal.class, new ResourceLocation(CustomNpcs.MODID, "CustomTileEntityPortal"));
        GameRegistry.registerTileEntity(CustomTileEntityChest.class, new ResourceLocation(CustomNpcs.MODID, "CustomTileEntityChest"));

        List<Block> blocks = new ArrayList<>();
        List<String> names = new ArrayList<>();
        blocks.add(redstoneBlock = new BlockNpcRedstone());
        blocks.add(carpenty = new BlockCarpentryBench());
        blocks.add(mailbox = new BlockMailbox());
        blocks.add(waypoint = new BlockWaypoint());
        blocks.add(border = new BlockBorder());
        blocks.add(scripted = new BlockScripted());
        blocks.add(scripted_door = new BlockScriptedDoor());
        blocks.add(builder = new BlockBuilder());
        blocks.add(copy = new BlockCopy());
        for (Block bl : blocks) {
            names.add(Objects.requireNonNull(bl.getRegistryName()).toString());
        }

        // Custom Blocks
        File blocksFile = new File(CustomNpcs.Dir, "custom_blocks.js");
        NBTTagCompound nbtBlocks = getBlocksNbt(blocksFile);
        boolean resave = nbtBlocks.getBoolean("resave");
        nbtBlocks.removeTag("resave");

        for (int i = 0; i < nbtBlocks.getTagList("Blocks", 10).tagCount(); i++) {
            NBTTagCompound nbtBlock = nbtBlocks.getTagList("Blocks", 10).getCompoundTagAt(i);
            if (!nbtBlock.hasKey("RegistryName", 8) || !nbtBlock.hasKey("BlockType", 1)
                    || nbtBlock.getString("RegistryName").isEmpty() || nbtBlock.getByte("BlockType") < (byte) 0
                    || nbtBlock.getByte("BlockType") > (byte) 6) {
                LogWriter.error("Attempt to load block pos: " + i + "; name: \"" + nbtBlock.getString("RegistryName") + "\" - failed");
                continue;
            }
            String name = nbtBlock.getString("RegistryName");
            String valid = NoppesUtilServer.validPath(name);
            if (!name.equals(valid)) {
                nbtBlock.setString("RegistryName", valid);
                resave = true;
            }

            String location = CustomNpcs.MODID + ":custom_" + valid;
            String addLocation = CustomNpcs.MODID + ":custom_double_" + valid;
            registryNbt = nbtBlock;
            switch (nbtBlock.getByte("BlockType")) {
                case (byte) 1: {
                    CustomFluid fluid = new CustomFluid(nbtBlock);
                    if (FluidRegistry.isFluidRegistered(fluid.getName())) {
                        LogWriter.error("Attempt to load a registered fluid \"" + fluid.getName() + "\"");
                        continue;
                    }
                    FluidRegistry.registerFluid(fluid);
                    FluidRegistry.addBucketForFluid(fluid);
                    registryBlock(location, new CustomBlockLiquid(fluid, Material.WATER, nbtBlock), names, blocks, nbtBlock.getBoolean("CreateDefaultFiles"));
                    LogWriter.debug("Load Custom Fluid: \"" + location + "\"");
                    customfluid.put(location, fluid);
                    if (nbtBlock.getBoolean("AddCauldron")) {
                        Block cauldron = new CustomCauldron(fluid, nbtBlock);
                        registryBlock(CustomNpcs.MODID + ":custom_cauldron_" + valid, cauldron, names, blocks, false);
                    }
                    break;
                } // Liquid
                case (byte) 2: {
                    if (nbtBlock.hasKey("SoundOpen", 8)) {
                        String n = nbtBlock.getString("SoundOpen");
                        String v = NoppesUtilServer.validPath(n);
                        if (!n.equals(v)) {
                            nbtBlock.setString("SoundOpen", v);
                            resave = true;
                        }
                    }
                    if (nbtBlock.hasKey("SoundClose", 8)) {
                        String n = nbtBlock.getString("SoundClose");
                        String v = NoppesUtilServer.validPath(n);
                        if (!n.equals(v)) {
                            nbtBlock.setString("SoundClose", v);
                            resave = true;
                        }
                    }
                    registryBlock(location, new CustomChest(CustomItem.getMaterial(nbtBlock.getString("Material")), nbtBlock),
                            names, blocks, nbtBlock.getBoolean("CreateDefaultFiles"));
                    break;
                } // Chest
                case (byte) 3: {
                    registryBlock(location, new CustomBlockStairs(nbtBlock), names, blocks, nbtBlock.getBoolean("CreateDefaultFiles"));
                    break;
                } // Stairs
                case (byte) 4: {
                    CustomBlockSlab.CustomBlockSlabDouble addblock = new CustomBlockSlab.CustomBlockSlabDouble(nbtBlock);
                    CustomBlockSlab.CustomBlockSlabSingle block = new CustomBlockSlab.CustomBlockSlabSingle(nbtBlock, addblock);
                    addblock.setSingle(block);
                    registryBlock(location, block, names, blocks, nbtBlock.getBoolean("CreateDefaultFiles"));
                    registryBlock(addLocation, addblock, names, blocks, nbtBlock.getBoolean("CreateDefaultFiles"));
                    break;
                } // Slab
                case (byte) 5: {
                    registryBlock(location, new CustomBlockPortal(CustomItem.getMaterial(nbtBlock.getString("Material")), nbtBlock),
                            names, blocks, nbtBlock.getBoolean("CreateDefaultFiles"));
                    break;
                } // Portal
                case (byte) 6: {
                    registryBlock(location, new CustomDoor(CustomItem.getMaterial(nbtBlock.getString("Material")), nbtBlock),
                            names, blocks, nbtBlock.getBoolean("CreateDefaultFiles"));
                    break;
                } // Door
                default: {
                    registryBlock(location, new CustomBlock(CustomItem.getMaterial(nbtBlock.getString("Material")), nbtBlock),
                            names, blocks, nbtBlock.getBoolean("CreateDefaultFiles"));
                } // Simple
            }
            if (nbtBlock.getBoolean("CreateDefaultFiles")) {
                nbtBlock.removeTag("CreateDefaultFiles");
                resave = true;
            }
        }
        if (resave) { Util.instance.saveFile(blocksFile, nbtBlocks); }
        registryNbt = null;
        event.getRegistry().registerAll(blocks.toArray(new Block[0]));
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        items.add(scripted_door_item = new ItemScriptedDoor(scripted_door));
        items.add(carpentry_item = createItem(carpenty));
        items.add(mailbox_item = createItem(mailbox).setHasSubtypes(true));
        items.add(redstone_item = createItem(redstoneBlock));
        items.add(waypoint_item = createItem(waypoint));
        items.add(border_item = createItem(border));
        items.add(builder_item = createItem(builder));
        items.add(copy_item = createItem(copy));
        items.add(CustomTabs.BLOCKS.item = scripted_item = createItem(scripted));
        // Blocks
        for (Map.Entry<ICustomElement, Item> entry : customblocks.entrySet()) {
            if (entry.getValue() != null) {
                items.add(entry.getValue());
            }
        }
        // Fluids
        for (Map.Entry<String, ICustomElement> entry : customfluid.entrySet()) {
            if (entry.getValue() != null) {
                CustomFluid fluid = (CustomFluid) entry.getValue();
                Item bottle = new CustomFluidBottle(fluid, fluid.nbtData);
                items.add(bottle);
            }
        }
        for (Item it : event.getRegistry()) {
            if (it instanceof UniversalBucket) { it.setCreativeTab(CustomTabs.BLOCKS); }
        }
        event.getRegistry().registerAll(items.toArray(new Item[0]));
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        // Blocks
        ModelLoader.setCustomStateMapper(mailbox, new StateMap.Builder().ignore(BlockMailbox.ROTATION, BlockMailbox.TYPE).build());
        ModelLoader.setCustomStateMapper(scripted_door, new StateMap.Builder().ignore(BlockDoor.POWERED).build());
        ModelLoader.setCustomStateMapper(builder, new StateMap.Builder().ignore(BlockBuilder.ROTATION).build());
        ModelLoader.setCustomStateMapper(carpenty, new StateMap.Builder().ignore(BlockCarpentryBench.ROTATION).build());
        ModelLoader.setCustomModelResourceLocation(redstone_item, 0, new ModelResourceLocation(Objects.requireNonNull(redstoneBlock.getRegistryName()), "inventory"));
        ModelLoader.setCustomModelResourceLocation(mailbox_item, 0, new ModelResourceLocation(Objects.requireNonNull(mailbox.getRegistryName()), "inventory"));
        ModelLoader.setCustomModelResourceLocation(mailbox_item, 1, new ModelResourceLocation(mailbox.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(mailbox_item, 2, new ModelResourceLocation(mailbox.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(waypoint_item, 0, new ModelResourceLocation(Objects.requireNonNull(waypoint.getRegistryName()), "inventory"));
        ModelLoader.setCustomModelResourceLocation(border_item, 0, new ModelResourceLocation(Objects.requireNonNull(border.getRegistryName()), "inventory"));
        ModelLoader.setCustomModelResourceLocation(scripted_item, 0, new ModelResourceLocation(Objects.requireNonNull(scripted.getRegistryName()), "inventory"));
        ModelLoader.setCustomModelResourceLocation(scripted_door_item, 0, new ModelResourceLocation(Objects.requireNonNull(scripted_door.getRegistryName()), "inventory"));
        ModelLoader.setCustomModelResourceLocation(builder_item, 0, new ModelResourceLocation(Objects.requireNonNull(builder.getRegistryName()), "inventory"));
        ModelLoader.setCustomModelResourceLocation(copy_item, 0, new ModelResourceLocation(Objects.requireNonNull(copy.getRegistryName()), "inventory"));
        ModelLoader.setCustomModelResourceLocation(carpentry_item, 0, new ModelResourceLocation(Objects.requireNonNull(carpenty.getRegistryName()), "inventory"));
        for (ICustomElement element : customblocks.keySet()) {
            if (element instanceof Block) {
                Block block = (Block) element;
                if (block instanceof CustomBlockPortal) {
                    ModelLoader.setCustomStateMapper(block, new StateMap.Builder().ignore(CustomBlockPortal.TYPE).build());
                } else if (block instanceof CustomDoor) {
                    ModelLoader.setCustomStateMapper(block, new StateMap.Builder().ignore(BlockDoor.POWERED).build());
                } else if (block instanceof CustomBlockLiquid) {
                    ModelLoader.setCustomStateMapper(block, new StateMap.Builder().ignore(BlockFluidBase.LEVEL).build());
                }
                ModelLoader.setCustomModelResourceLocation(customblocks.get(element), 0, new ModelResourceLocation(Objects.requireNonNull(block.getRegistryName()), "inventory"));
            }
        }
        // Items
        ModelLoader.setCustomModelResourceLocation(scripted_door_item, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcscripteddoortool", "inventory"));
        // Render Tiles
        ClientRegistry.bindTileEntitySpecialRenderer(TileBlockAnvil.class, new BlockCarpentryBenchRenderer<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileMailbox.class, new BlockMailboxRenderer<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileScripted.class, new BlockScriptedRenderer<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileDoor.class, new BlockDoorRenderer<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileCopy.class, new BlockCopyRenderer<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileBorder.class, new BlockBorderRenderer<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileRedstoneBlock.class, new BlockNpcRedstoneRenderer<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileWaypoint.class, new BlockWaypointRenderer<>());
        // custom
        ClientRegistry.bindTileEntitySpecialRenderer(CustomTileEntityPortal.class, new BlockPortalRenderer<>());
        ClientRegistry.bindTileEntitySpecialRenderer(CustomTileEntityChest.class, new BlockChestRenderer<>());
        // Item Block model renders
        carpentry_item.setTileEntityItemStackRenderer(new ItemCarpentryBenchRenderer());
        mailbox_item.setTileEntityItemStackRenderer(new ItemMailboxRenderer());
        for (Item item : items) {
            if (item instanceof ItemNpcBlock) {
                item.setTileEntityItemStackRenderer(((ItemNpcBlock) item).getBlock() instanceof CustomBlockPortal ?
                        CustomTileEntityItemStackRenderer.itemPortalRenderer :
                        CustomTileEntityItemStackRenderer.itemRenderer);
            }
            if (item instanceof CustomFluidBottle) {
                ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory"));
            }
        }
        items.clear();
    }

    private static NBTTagCompound getBlocksNbt(File file) {
        NBTTagCompound compound = ModData.getExampleBlocks().copy();
        NBTTagCompound nbtInFile = new NBTTagCompound();
        try { if (file.exists()) { nbtInFile = NBTJsonUtil.LoadFile(file); } }
        catch (Exception e) { LogWriter.error("Try Load " + file.getName() + ": ", e); }

        List<String> names = new ArrayList<>();
        NBTTagList listInFile = nbtInFile.getTagList("Blocks", 10);
        NBTTagList listBlocks = compound.getTagList("Blocks", 10);
        NBTTagList exampleBlocks = listBlocks.copy();
        boolean resave = false;
        for (int i = 0; i < listInFile.tagCount(); i++) {
            NBTTagCompound nbtBlock = listInFile.getCompoundTagAt(i);
            String name = nbtBlock.getString("RegistryName");
            boolean isExample = false;
            for (int j = 0; j < exampleBlocks.tagCount(); j++) {
                if (name.equals(exampleBlocks.getCompoundTagAt(j).getString("RegistryName"))) {
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
                nbtBlock.setString("RegistryName", name);
                resave = true;
            }
            names.add(name);
            if (!isExample) { listBlocks.appendTag(nbtBlock); }
        }
        compound.setBoolean("resave", resave);
        return compound;
    }

    public static ItemNpcBlock createItem(Block block) { return new ItemNpcBlock(block); }

    private static void registryBlock(String location, Block block, List<String> names, List<Block> blocks, boolean defFiles) {
        if (names.contains(location) || Block.getIdFromBlock(block) > 0 || !(block instanceof ICustomElement)) {
            LogWriter.error("Attempt to load a registered block \"" + location + "\"");
            return;
        }
        boolean isExample = location.endsWith(":custom_blockexample") || location.endsWith(":custom_liquidexample") ||
                location.endsWith(":custom_facingblockexample") || location.endsWith(":custom_stairsexample") ||
                location.endsWith(":custom_slabexample") || location.endsWith(":custom_portalexample") || location.endsWith(":custom_chestexample")
                || location.endsWith(":custom_containerexample") || location.endsWith(":custom_doorexample");
        if ((isExample || defFiles) && !(block instanceof CustomBlockSlab.CustomBlockSlabDouble)) {
            CustomNpcs.proxy.createAllFiles((ICustomElement) block);
        }
        LogWriter.info("Load Custom Block \"" + location + "\"");
        customblocks.put((ICustomElement) block, createItem(block));
        names.add(location);
        blocks.add(block);
        if (location.endsWith(":custom_blockexample") || CustomTabs.BLOCKS.item == scripted_item) {
            CustomTabs.BLOCKS.item = customblocks.get((ICustomElement) block);
        }
        if (block instanceof CustomBlockPortal) { portals.add((CustomBlockPortal) block); }
        else if (block instanceof CustomChest) { chests.add((CustomChest) block); }
    }

}
