package noppes.npcs.util;

import net.minecraft.nbt.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import noppes.npcs.constants.EnumParts;

import java.util.UUID;

public class ModData {

    private static CompoundTag exampleBlocks;
    private static CompoundTag exampleItems;
    private static CompoundTag exampleParticles;
    private static final String t = "                ";

    public static CompoundTag getExampleBlocks() {
        if (exampleBlocks == null) {
            exampleBlocks = new CompoundTag();
            ListTag listBlocks = new ListTag();
            listBlocks.add(getExampleBlock());
            listBlocks.add(getExampleFacingBlock());
            listBlocks.add(getExampleLiquid());
            listBlocks.add(getExampleChest());
            listBlocks.add(getExampleContainer());
            listBlocks.add(getExampleStairs());
            listBlocks.add(getExampleSlab());
            listBlocks.add(getExamplePortal());
            listBlocks.add(getExampleDoor());
            exampleBlocks.put("Blocks", listBlocks);
        }
        return exampleBlocks;
    }

    public static CompoundTag getExampleBlock() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "blockexample");
        compound.putByte("BlockType", (byte) 0);
        compound.putBoolean("IsLadder", false);
        compound.putBoolean("IsValidSpawn", true);

        CompoundTag nbtProperties = new CompoundTag();
        nbtProperties.putInt("lightLevel", 0);
        nbtProperties.putFloat("destroyTime", 5.0F);
        nbtProperties.putFloat("explosionResistance", 10.0f);
        nbtProperties.putString("sound", "STONE");
        compound.put("Properties", nbtProperties);

        ListTag aabb = new ListTag();
        aabb.add(DoubleTag.valueOf(0.0625d));
        aabb.add(DoubleTag.valueOf(0.0625d));
        aabb.add(DoubleTag.valueOf(0.0625d));
        aabb.add(DoubleTag.valueOf(0.9375d));
        aabb.add(DoubleTag.valueOf(0.9375d));
        aabb.add(DoubleTag.valueOf(0.9375d));
        compound.put("AABB", aabb);

        String sb = "Tags for creating a simple block:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- format of tag values must be respected;\n" +
                t + "1 key 'RegistryName'; type: 'String'; format: '\"value\"'; des - 'Required' Specified name for block registration;\n" +
                t + "2 key 'BlockType'; type: 'Byte'; format: '0b'<>'255b'; des - 'Required' Used to determine the block type during registration.;\n" +
                t + "3 key 'IsPassable'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Entity passes through the block without collision;\n" +
                t + "4 key 'IsLadder'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Block is a vertical ladder;\n" +
                t + "5 key 'IsValidSpawn'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Mobs may spawn on the block;\n" +
                t + "6 key 'ShowInCreative'; type: 'Boolean'; format: false='0b', true='1b'; default: '1b' (true); des - 'Can be excluded' Show this block in the creative inventory tab;\n" +
                t + "7 key 'Properties'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' Needed to set properties for your block; code body:\n" +
                t + " 7.01 key 'noCollission'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Avoid collision between entity and block\n" +
                t + " 7.02 key 'noOcclusion'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Is block occlusive and does it block the passage of light or fluid?\n" +
                t + " 7.03 key 'randomTicks'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Block randomly triggers a time tick event\n" +
                t + " 7.04 key 'dynamicShape'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Block's hitbox shape can change\n" +
                t + " 7.05 key 'ignitedByLava'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Block is ignited by nearby lava\n" +
                t + " 7.06 key 'liquid'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Block is liquid\n" +
                t + " 7.07 key 'forceSolidOn'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Force enable block hardness\n" +
                t + " 7.08 key 'forceSolidOff'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Force disable block hardness\n" +
                t + " 7.09 key 'isAir'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Block is Air\n" +
                t + " 7.10 key 'noParticlesOnBreak'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' No particles when block is destroyed\n" +
                t + " 7.11 key 'replaceable'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Block is replaced by another when the player attempts to place another block in the same place\n" +
                t + " 7.12 key 'noLootTable'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Block does not have a table of item drops when destroyed\n" +
                t + " 7.13 key 'requiresCorrectToolForDrops'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' \n" +
                t + " 7.14 key 'lightLevel'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; min: 0; max: 15; default: 0; des - 'Can be excluded' Block is a light source, where the value is the illumination range of the blocks around it\n" +
                t + " 7.15 key 'mapColor'; type: 'Integer' (has String type below); format: '-2147483648'<>'0'<>'2147483647'; default: '0' (black); des - 'Can be excluded' Hex block color for the minimap\n" +
                t + " 7.16 key 'friction'; type: 'Float'; format: '1.17549435E-38f'<>'0.000000f'<>'3.4028235e+38f'; min: '0.0f'; max: '1.0f'; default: '0.6f'; des - 'Can be excluded' Coefficient of friction of the block is responsible for the degree of braking of entities\n" +
                t + " 7.17 key 'speedFactor'; type: 'Float'; format: '1.17549435E-38f'<>'0.000000f'<>'3.4028235e+38f'; min: '0.05f'; max: '10.0f'; default: '1.0f'; des - 'Can be excluded' Multiplier for the speed of movement of entities in a block\n" +
                t + " 7.18 key 'jumpFactor'; type: 'Float'; format: '1.17549435E-38f'<>'0.000000f'<>'3.4028235e+38f'; min: '0.05f'; max: '10.0f'; default: '1.0f'; des - 'Can be excluded' Multiplier for the jump force of entities from a block\n" +
                t + " 7.19 key 'destroyTime'; type: 'Float'; format: '1.17549435E-38f'<>'0.000000f'<>'3.4028235e+38f'; min: '0.0f'; max: '6000.0f'; default: '0.0f'; des - 'Can be excluded' Time it takes for a player to destroy a block in seconds\n" +
                t + " 7.20 key 'explosionResistance'; 'Float'; format: '1.17549435E-38f'<>'0.000000f'<>'3.4028235e+38f'; min: '0.0f'; max: '3.4028235e+38f'; default: '0.0f'; des - 'Can be excluded' Value of resistance to the force of destruction of a block by an explosion\n" +
                t + " 7.21 key 'mapColor'; type: 'String' (has Integer type above); format: '\"value\"'; default: 'none' (black); des - 'Can be excluded' Minimap block color from standards. Options:\n" +
                t + " grass, sand, wool, fire, ice, metal, plant, snow, clay, dirt, stone, water, wood, quartz, color_light_blue, color_light_green, color_light_gray, color_orange, color_magenta, color_yellow, color_pink, color_gray, color_cyan, color_purple, color_blue,\n" +
                t + " color_brown, color_green, color_red, color_black, gold, diamond, lapis, emerald, podzol, nether, terracotta_light_blue, terracotta_light_green, terracotta_light_gray, terracotta_white, terracotta_orange, terracotta_magenta, terracotta_yellow,\n" +
                t + " terracotta_pink, terracotta_gray, terracotta_cyan, terracotta_purple, terracotta_blue, terracotta_brown, terracotta_green, terracotta_red, terracotta_black, crimson_nylium, crimson_stem, crimson_hyphae, warped_nylium, warped_stem, warped_hyphae,\n" +
                t + " warped_wart_block, deepslate, raw_iron, glow_lichen;\n" +
                t + " 7.22 key 'sound'; type: 'String' (has CompoundTag type below); format: '\"value\"'; default: 'stone'; des - 'Can be excluded' Sound settings for the standards block. Options:\n" +
                t + " wood, gravel, grass, lily_pad, stone, metal, glass, wool, sand, snow, powder_snow, ladder, anvil, slime_block, honey_block, wet_grass, coral_block, bamboo, bamboo_sapling, scaffolding, sweet_berry_bush, crop, hard_crop, vine, nether_wart, lantern,\n" +
                t + " stem, nylium, fungus, roots, shroomlight, weeping_vines, twisting_vines, soul_sand, soul_soil, basalt, wart_block, netherrack, nether_bricks, nether_sprouts, nether_ore, bone_block, netherite_block, ancient_debris, lodestone, chain, nether_gold_ore,\n" +
                t + " gilded_blackstone, candle, amethyst, amethyst_cluster, small_amethyst_bud, medium_amethyst_bud, large_amethyst_bud, tuff, calcite, dripstone_block, pointed_dripstone, copper, cave_vines, spore_blossom, azalea, flowering_azalea, moss_carpet, pink_petals,\n" +
                t + " moss, big_dripleaf, small_dripleaf, rooted_dirt, hanging_roots, azalea_leaves, sculk_sensor, sculk_catalyst, sculk, sculk_vein, sculk_shrieker, glow_lichen, deepslate, deepslate_bricks, deepslate_tiles, polished_deepslate, froglight, frogspawn,\n" +
                t + " mangrove_roots, muddy_mangrove_roots, mud, mud_bricks, packed_mud, hanging_sing, nether_wood_hanging_sing, bamboo_wood_hanging_sing, bamboo_wood, nether_wood, cherry_wood, cherry_sapling, cherry_leaves, cherry_wood_hanging_sing, chiseled_bookshelf,\n" +
                t + " suspicious_sand, suspicious_gravel, decorated_pot, decorated_pot_cracked;\n" +
                t + " 7.23 key 'sound'; type: 'CompoundTag' (has String type above); format: '{}'; default: 'not used'; des - 'Can be excluded' Create sound settings for the block:\n" +
                t + " 7.23.1 key 'volume'; type: 'Float'; format: '1.17549435E-38f'<>'0.000000f'<>'3.4028235e+38f'; min: '0.05f'; max: '5.0f'; default: '1.0f'; des - 'Can be excluded'\n" +
                t + " 7.23.2 key 'pitch'; type: 'Float'; format: '1.17549435E-38f'<>'0.000000f'<>'3.4028235e+38f'; min: '0.05f'; max: '5.0f'; default: '1.0f'; des - 'Can be excluded'\n" +
                t + " 7.23.3 key 'breakSound'; type: 'String'; format: '\"value\"'; default: 'empty'; des - 'Can be excluded' Registered key (name) of the sound event; example: 'minecraft:block.stone.break'\n" +
                t + " 7.23.4 key 'stepSound'; type: 'String'; format: '\"value\"'; default: 'empty'; des - 'Can be excluded' Registered key (name) of the sound event; example: 'block.metal.step'\n" +
                t + " 7.23.5 key 'placeSound'; type: 'String'; format: '\"value\"'; default: 'empty'; des - 'Can be excluded' Registered key (name) of the sound event; example: 'block.glass.place'\n" +
                t + " 7.23.6 key 'hitSound'; type: 'String'; format: '\"value\"'; default: 'empty'; des - 'Can be excluded' Registered key (name) of the sound event; example: 'block.wool.hit'\n" +
                t + " 7.23.7 key 'fallSound'; type: 'String'; format: '\"value\"'; default: 'empty'; des - 'Can be excluded' Registered key (name) of the sound event; example: 'block.sand.fall'\n" +
                t + " 7.24 key 'pushReaction'; type: 'String'; format: '\"value\"'; default: 'normal'; des - 'Can be excluded' Reaction used to push the block (piston, player, etc.). Options:\n" +
                t + " destroy, block, ignore, push_only, normal;\n" +
                t + " 7.25 key 'isValidSpawn'; type: 'String'; format: '\"value\"'; default: 'not used' (from above, illumination less than 14); des - 'Can be excluded' Permission to spawn entities near a block. Options:\n" +
                t + " always, never, ocelotOrParrot, polarBear, fireImmune, (EntityType registered key);\n" +
                t + " 7.26 key 'isRedstoneConductor'; type: 'String'; format: '\"value\"'; default: 'not used' (is collision shape full block); des - 'Can be excluded' Uses signaling Redstone liner. Options:\n" +
                t + " always, never;\n" +
                t + " 7.27 key 'isSuffocating'; type: 'String'; format: '\"value\"'; default: 'not used' (blocks motion and is collision shape full block); des - 'Can be excluded' Conditions under which an entity suffocates inside a block. Options:\n" +
                t + " never, pistonBase, shulkerBox;\n" +
                t + " 7.28 key 'isViewBlocking'; type: 'String'; format: '\"value\"'; default: 'not used' (blocks motion and is collision shape full block); des - 'Can be excluded' Conditions for blocking line of sight through a block for entities. Options:\n" +
                t + " always, never, snowLayers, pistonBase, shulkerBox;\n" +
                t + " 7.29 key 'hasPostProcess'; type: 'String'; format: '\"value\"'; default: 'not used' (false); des - 'Can be excluded' Conditions when post-processing of the render is required (for example, shaders). Options:\n" +
                t + " always;\n" +
                t + " 7.30 key 'emissiveRendering'; type: 'String'; format: '\"value\"'; default: 'not used' (false); des - 'Can be excluded' Conditions where the block will be equally bright regardless of the ambient lighting. Options:\n" +
                t + " always, sculkSensorBlock;\n" +
                t + " 7.31 key 'offsetType'; type: 'String'; format: '\"value\"'; default: 'not used' (none); des - 'Can be excluded' Texture offset relative to coordinates. Options:\n" +
                t + " xz, xyz, none;\n" +
                t + " 7.32 key 'instrument'; type: 'String'; format: '\"value\"'; default: 'not used' (harp); des - 'Can be excluded' Sound of the instrument when destroying or attacking a block. Options:\n" +
                t + " harp, basedrum, snare, hat, bass, flute, bell, guitar, chime, xylophone, iron_xylophone, cow_bell, didgeridoo, bit, banjo, pling, zombie, skeleton, creeper, dragon, wither_skeleton, piglin, custom_head;\n" +
                t + " 7.33 key 'lootFrom'; type: 'String'; format: '\"value\"'; default: 'not used' (empty); des - 'Can be excluded' Use a loot table from another block; example: 'minecraft:stone'\n" +
                t + " 7.34 key 'lootTable'; type: 'String'; format: '\"value\"'; default: 'not used' (empty); des - 'Can be excluded' Use the table specified by the registered key (name); example: 'minecraft:chests/spawn_bonus_chest'; look class: 'net.minecraft.world.level.storage.loot.BuiltInLootTables'\n" +
                t + " 7.35 key 'requiredFeatures'; type: 'ListTag'; format: '[]'; listType: 'CompoundTag'; tagFormat: '{}'; default: 'not used'; des - 'Can be excluded' A list of required features in the world for a block to exist in it:\n" +
                t + " 7.35.1 key 'UniverseId' in 'CompoundTag' option; type: 'String'; format: '\"value\"'; des - 'Required if specified' requirement ID\n" +
                t + " 7.35.2 key 'Name' in 'CompoundTag' option; type: 'String'; format: '\"value\"'; des - 'Required if specified' sign ID\n" +
                t + "8 key 'AABB'; type: 'ListTag'; format: '[]'; listType: 'Double'; tagFormat: '4.9e-324'<>'0.000000000'<>'1.7976931348623157e+308'; size: '6 min'; des - 'Can be excluded' Use this if you want to specify the exact dimensions of a block's hitbox:\n" +
                t + " [X min, Y min, Z min, X max, Y max, Z max]\n" +
                t + "9 key 'Property'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' Custom block state property for advanced block behavior:\n" +
                t + " 9.01 key 'Type'; type: 'Byte'; format: '1b'<>'4b'; des - 'Required if specified' Property type: 1=BooleanProperty, 3=IntegerProperty, 4=DirectionProperty (horizontal facing);\n" +
                t + " 9.02 key 'Name'; type: 'String'; format: '\"value\"'; des - 'Required if specified' Property name for blockstate; example: 'facing', 'powered', 'age';\n" +
                t + " 9.03 key 'Min'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; des - 'Required for Type=3' Minimum value for IntegerProperty;\n" +
                t + " 9.04 key 'Max'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; des - 'Required for Type=3' Maximum value for IntegerProperty;\n" +
                t + "- Note: When 'Property' is used, the block automatically creates blockstates. For Type=4 (Direction), hitbox rotates with facing (NORTH default). For Type=1 (Boolean), default is false. For Type=3 (Integer), default is Min value;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleFacingBlock() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "facingblockexample");
        compound.putByte("BlockType", (byte) 0);

        CompoundTag nbtProperties = new CompoundTag();
        nbtProperties.putBoolean("isAir", false);
        compound.put("Properties", nbtProperties);

        CompoundTag nbtProperty = new CompoundTag();
        nbtProperty.putByte("Type", (byte) 4);
        nbtProperty.putString("Name", "facing");
        compound.put("Property", nbtProperty);

        String sb = "Tags for creating a facing block:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'BlockType', 'IsPassable', 'IsLadder', 'IsValidSpawn', 'ShowInCreative', 'Properties', 'AABB' - see description of block 'blockexample';\n" +
                t + "8 key 'Property'; type: 'CompoundTag'; format: '{}'; des - 'Required' Block state property defining the facing direction:\n" +
                t + " 8.01 key 'Type'; type: 'Byte'; format: '4b'; des - 'Required' Must be 4 for DirectionProperty (horizontal facing);\n" +
                t + " 8.02 key 'Name'; type: 'String'; format: '\"value\"'; default: 'facing'; des - 'Required' Property name for blockstate; standard: 'facing';\n" +
                t + "- The block will automatically rotate its hitbox (AABB) based on facing direction (NORTH default, rotates for EAST/SOUTH/WEST);";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleLiquid() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "liquidexample");
        compound.putByte("BlockType", (byte) 1);
        compound.putBoolean("HasInGameRules", false);
        compound.putBoolean("AddCauldron", true);
        compound.putInt("SlopeFindDistance", 4);
        compound.putInt("DropOff", 1);
        compound.putInt("TickDelay", 5);
        compound.putFloat("Resistance", 100.0f);
        compound.putString("SoundAmbientFlowing", "block.water.ambient");
        compound.putString("SoundBucketFill", "item.bucket.fill");
        compound.putString("SoundBucketEmpty", "item.bucket.empty");
        compound.putString("ParticleUnderFluid", "underwater");
        compound.putString("ParticleDripParticle", "dripping_water");

        CompoundTag nbtProperties = new CompoundTag();
        nbtProperties.putBoolean("liquid", true);
        nbtProperties.putBoolean("replaceable", true);
        nbtProperties.putBoolean("noCollission", true);
        nbtProperties.putBoolean("noLootTable", true);
        nbtProperties.putFloat("explosionResistance", 2.0f);
        nbtProperties.putString("mapColor", "WATER");
        nbtProperties.putInt("mapColor", 0x822BD9);
        nbtProperties.putString("sound", "EMPTY");
        nbtProperties.putString("pushReaction", "DESTROY");
        compound.put("Properties", nbtProperties);

        CompoundTag nbtFluidType = new CompoundTag();
        nbtFluidType.putInt("tickRate", 5);
        nbtFluidType.putInt("slopeFindDistance", 4);
        nbtFluidType.putInt("levelDecreasePerBlock", 4);

        nbtFluidType.putInt("fogColor", 0xFF822BD9);
        nbtFluidType.putInt("tintColor", 0xFF822BD9);

        nbtFluidType.putInt("lightLevel", 5);
        nbtFluidType.putInt("density", 1100);
        nbtFluidType.putInt("viscosity", 900);
        nbtFluidType.putInt("temperature", 300);
        compound.put("FluidType", nbtFluidType);

        String sb = "Tags for creating a liquid block:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'BlockType', 'IsLadder', 'IsValidSpawn', 'Properties', 'AABB' - see description of block 'blockexample';\n" +
                t + "8 key 'HasInGameRules'; type: 'Boolean'; format: false='0b', true='1b'; default: 'uses water resolution'; des - 'Can be excluded' Create a flow permission for this fluid for the command block;\n" +
                t + "9 key 'AddCauldron'; type: 'Boolean'; format: false='0b', true='1b'; default: 'false'; des - 'Can be excluded' Add cauldron support for this fluid;\n" +
                t + "10 key 'SlopeFindDistance'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; min: 1; max: 16; default: 4; des - 'Can be excluded' Distance at which the fluid searches for a slope to flow down; maps to 'slopeFindDistance' in FluidType;\n" +
                t + "11 key 'DropOff'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; min: 1; max: 16; default: 1; des - 'Can be excluded' How many levels the fluid drops per block when flowing; maps to 'levelDecreasePerBlock' in FluidType;\n" +
                t + "12 key 'TickDelay'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; min: 1; max: 100; default: 5; des - 'Can be excluded' Delay in ticks between fluid updates; maps to 'tickRate' in FluidType;\n" +
                t + "13 key 'Resistance'; type: 'Float'; format: '1.17549435E-38f'<>'0.000000f'<>'3.4028235e+38f'; min: '0.0f'; max: '3.4028235e+38f'; default: '100.0f'; des - 'Can be excluded' Resistance of the fluid block to being pushed by entities; maps to 'explosionResistance' in FluidType;\n" +
                t + "14 key 'SoundAmbientFlowing'; type: 'String'; format: '\"value\"'; default: 'block.water.ambient'; des - 'Can be excluded' Registered key of the ambient flowing sound; example: 'block.water.ambient';\n" +
                t + "15 key 'SoundBucketFill'; type: 'String'; format: '\"value\"'; default: 'item.bucket.fill'; des - 'Can be excluded' Registered key of the bucket fill sound; example: 'item.bucket.fill';\n" +
                t + "16 key 'SoundBucketEmpty'; type: 'String'; format: '\"value\"'; default: 'item.bucket.empty'; des - 'Can be excluded' Registered key of the bucket empty sound; example: 'item.bucket.empty';\n" +
                t + "17 key 'ParticleUnderFluid'; type: 'String'; format: '\"value\"'; default: 'underwater'; des - 'Can be excluded' Particle type shown when under the fluid; example: 'underwater', 'bubble';\n" +
                t + "18 key 'ParticleDripParticle'; type: 'String'; format: '\"value\"'; default: 'dripping_water'; des - 'Can be excluded' Particle type for dripping from the fluid; example: 'dripping_water', 'dripping_lava';\n" +
                t + "19 key 'FluidType'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' Fluid type properties for Forge fluid system:\n" +
                t + " 19.01 key 'tickRate'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; min: 1; max: 100; default: 5; des - 'Can be excluded' Tick rate for fluid updates;\n" +
                t + " 19.02 key 'slopeFindDistance'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; min: 1; max: 16; default: 4; des - 'Can be excluded' Same as 'SlopeFindDistance' above;\n" +
                t + " 19.03 key 'levelDecreasePerBlock'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; min: 1; max: 16; default: 4; des - 'Can be excluded' Fluid level decrease per horizontal block;\n" +
                t + " 19.04 key 'fogColor'; type: 'Integer'; format: '0'<>'16777215'; default: 0xFFFFFF; des - 'Can be excluded' Fog color when submerged in fluid (hex);\n" +
                t + " 19.05 key 'tintColor'; type: 'Integer'; format: '0'<>'16777215'; default: 0xFFFFFF; des - 'Can be excluded' Tint color for the fluid texture (hex);\n" +
                t + " 19.06 key 'lightLevel'; type: 'Integer'; format: '0'<>'15'; default: 5; des - 'Can be excluded' Light level emitted by the fluid block;\n" +
                t + " 19.07 key 'density'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; default: 1100; des - 'Can be excluded' Fluid density in kg/m3;\n" +
                t + " 19.08 key 'viscosity'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; default: 900; des - 'Can be excluded' Fluid viscosity; higher = slower flow;\n" +
                t + " 19.09 key 'temperature'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; default: 300; des - 'Can be excluded' Fluid temperature in Kelvin; affects interactions;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleChest() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "chestexample");
        compound.putByte("BlockType", (byte) 2);
        compound.putBoolean("IsChest", true);
        compound.putInt("Size", 14);
        compound.putInt("GUIColor", 0x46AB86);
        compound.putString("Name", "Custom Chest");

        CompoundTag nbtProperties = new CompoundTag();
        nbtProperties.putString("sound", "WOOD");
        compound.put("Properties", nbtProperties);

        String sb = "Tags for creating a chest/container block:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'BlockType', 'IsLadder', 'IsValidSpawn', 'Properties', 'AABB' - see description of block 'blockexample';\n" +
                t + "8 key 'IsChest'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' If true, renders as a chest model with lid animation; if false, renders as a simple container block;\n" +
                t + "9 key 'Size'; type: 'Integer'; format: '1'<>'54'; default: 27; des - 'Can be excluded' Number of inventory slots (standard chest: 27, double chest: 54);\n" +
                t + "10 key 'GUIColor'; type: 'Integer'; format: '0'<>'16777215'; default: 0xC6C6C6; des - 'Can be excluded' Hex color for the GUI background; for container can be 'IntArray' [topColor, bottomColor];\n" +
                t + "11 key 'Name'; type: 'String'; format: '\"value\"'; default: 'Custom Chest'; des - 'Can be excluded' Display name for the chest/container GUI;\n" +
                t + "12 key 'SoundOpen'; type: 'String'; format: '\"value\"'; default: 'block.wooden_door.open'; des - 'Can be excluded' Registered key of the sound event for opening; example: 'block.wooden_door.open';\n" +
                t + "13 key 'SoundClose'; type: 'String'; format: '\"value\"'; default: 'block.wooden_door.close'; des - 'Can be excluded' Registered key of the sound event for closing; example: 'block.wooden_door.close';\n" +
                t + "- Note: 'AABB' is used only when 'IsChest' is false;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleContainer() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "containerexample");
        compound.putByte("BlockType", (byte) 2);
        compound.putInt("Size", 96);
        compound.putIntArray("GUIColor", new int[] { 0x00DC8C, 0xDC8000 });
        compound.putString("Name", "Custom Container");
        compound.putBoolean("IsOBJModel", true);

        CompoundTag nbtProperties = new CompoundTag();
        nbtProperties.putString("sound", "STONE");
        compound.put("Properties", nbtProperties);

        ListTag aabb = new ListTag();
        aabb.add(DoubleTag.valueOf(0.0625d));
        aabb.add(DoubleTag.valueOf(0.0d));
        aabb.add(DoubleTag.valueOf(0.0625d));
        aabb.add(DoubleTag.valueOf(0.9375d));
        aabb.add(DoubleTag.valueOf(1.0d));
        aabb.add(DoubleTag.valueOf(0.9375d));
        compound.put("AABB", aabb);

        String sb = "Tags for creating a container block (non-chest):\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'BlockType', 'IsLadder', 'IsValidSpawn', 'Properties', 'AABB' - see description of block 'blockexample';\n" +
                t + "8 key 'Size'; type: 'Integer'; format: '1'<>'96'; default: 27; des - 'Can be excluded' Number of inventory slots (max 96 for container);\n" +
                t + "9 key 'GUIColor'; type: 'IntArray'; format: '[int, int]'; default: '[56460, 14450688]'; des - 'Can be excluded' Two hex colors for GUI gradient [top, bottom];\n" +
                t + "10 key 'Name'; type: 'String'; format: '\"value\"'; default: 'Custom Container'; des - 'Can be excluded' Display name for the container GUI;\n" +
                t + "11 key 'IsOBJModel'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Use custom OBJ model instead of default container model;\n" +
                t + "12 key 'SoundOpen'; type: 'String'; format: '\"value\"'; default: 'block.wooden_door.open'; des - 'Can be excluded' Registered key of the sound event for opening;\n" +
                t + "13 key 'SoundClose'; type: 'String'; format: '\"value\"'; default: 'block.wooden_door.close'; des - 'Can be excluded' Registered key of the sound event for closing;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleStairs() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "stairsexample");
        compound.putByte("BlockType", (byte) 3);
        compound.putString("Planks", "oak_planks");

        CompoundTag nbtProperties = new CompoundTag();
        nbtProperties.putBoolean("isAir", false);
        nbtProperties.putString("sound", "STONE");
        compound.put("Properties", nbtProperties);

        String sb = "Tags for creating a stairs block:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'BlockType', 'IsLadder', 'IsValidSpawn', 'Properties', 'AABB' - see description of block 'blockexample';\n" +
                t + "8 key 'Planks'; type: 'String'; format: '\"value\"'; default: 'oak_planks'; des - 'Required' Base block registry name for texture and stair shape; example: 'oak_planks', 'minecraft:stone', 'minecraft:bricks';\n" +
                t + "- Stairs automatically support all orientations and inner/outer corner variants via block states;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleSlab() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "slabexample");
        compound.putByte("BlockType", (byte) 4);

        CompoundTag nbtProperties = new CompoundTag();
        nbtProperties.putBoolean("isAir", false);
        nbtProperties.putString("sound", "STONE");
        compound.put("Properties", nbtProperties);

        String sb = "Tags for creating a slab block:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'BlockType', 'IsLadder', 'IsValidSpawn', 'Properties', 'AABB' - see description of block 'blockexample';\n" +
                t + "- Slab automatically supports bottom/top/double placement via block states;\n" +
                t + "- No unique tags beyond standard block properties; all customization is done via 'Properties';";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExamplePortal() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "portalexample");
        compound.putByte("BlockType", (byte) 5);
        compound.putInt("DimensionID", 100);
        compound.putInt("HomeDimensionID", 0);

        CompoundTag nbtProperties = new CompoundTag();
        nbtProperties.putBoolean("isAir", false);
        nbtProperties.putFloat("explosionResistance", 2000.0f);
        nbtProperties.putString("sound", "PORTAL");
        compound.put("Properties", nbtProperties);

        CompoundTag nbtRender = new CompoundTag();
        nbtRender.putString("SpawnParticle", "CRIT");
        nbtRender.putInt("ChanceParticle", 10);
        nbtRender.putFloat("Transparency", 0.75f);
        compound.put("RenderData", nbtRender);

        String sb = "Tags for creating a portal block:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'BlockType', 'IsLadder', 'IsValidSpawn', 'Properties', 'AABB' - see description of block 'blockexample';\n" +
                t + "8 key 'DimensionID'; type: 'String'; format: '\"value\"'; default: 'minecraft:overworld'; des - 'Required' Target dimension ResourceLocation for teleportation; example: 'minecraft:the_nether', 'customnpcs:custom_dimension';\n" +
                t + "9 key 'HomeDimensionID'; type: 'String'; format: '\"value\"'; default: 'minecraft:overworld'; des - 'Can be excluded' Return dimension ResourceLocation (0 = Overworld, -1 = Nether, 1 = End); example: 'minecraft:overworld';\n" +
                t + "10 key 'RenderData'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' Visual effect settings for the portal:\n" +
                t + " 10.01 key 'SpawnParticle'; type: 'String'; format: '\"value\"'; default: 'CRIT'; des - 'Can be excluded' Particle type spawned inside portal; options: CRIT, PORTAL, ENCHANT, END_ROD, etc. (any registered SimpleParticleType);\n" +
                t + " 10.02 key 'Transparency'; type: 'Float'; format: '0.15f'<>'1.0f'; min: '0.15f'; max: '1.0f'; default: '0.5f'; des - 'Can be excluded' Portal block transparency (0.15 = nearly invisible, 1.0 = opaque); values clamped to range;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleDoor() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "doorexample");
        compound.putByte("BlockType", (byte) 6);

        CompoundTag nbtProperties = new CompoundTag();
        nbtProperties.putBoolean("ignitedByLava", true);
        nbtProperties.putFloat("destroyTime", 3.0f);
        nbtProperties.putFloat("explosionResistance", 3.0f);
        nbtProperties.putString("mapColor", "WOOD");
        nbtProperties.putString("instrument", "BASS");
        nbtProperties.putString("pushReaction", "DESTROY");
        compound.put("Properties", nbtProperties);

        CompoundTag blockSetType = new CompoundTag();
        blockSetType.putString("Name", "custom");
        blockSetType.putString("SoundType", "WOOD");
        blockSetType.putString("SoundDoorClose", "block.wooden_door.close");
        blockSetType.putString("SoundDoorOpen", "block.wooden_door.open");
        blockSetType.putString("SoundTrapDoorClose", "block.wooden_trapdoor.close");
        blockSetType.putString("SoundTrapDoorOpen", "block.wooden_trapdoor.open");
        blockSetType.putString("SoundPlateClickOff", "block.wooden_pressure_plate.click_off");
        blockSetType.putString("SoundPlateClickOn", "block.wooden_pressure_plate.click_on");
        blockSetType.putString("SoundButtonClickOff", "block.wooden_button.click_off");
        blockSetType.putString("SoundButtonClickOn", "block.wooden_button.click_on");
        blockSetType.putBoolean("CanOpenByHand", true);
        compound.put("BlockSetType", blockSetType);

        String sb = "Tags for creating a door block:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'BlockType', 'IsLadder', 'IsValidSpawn', 'Properties', 'AABB' - see description of block 'blockexample';\n" +
                t + "8 key 'BlockSetType'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' Sound and behavior settings for the door set:\n" +
                t + " 8.01 key 'Name'; type: 'String'; format: '\"value\"'; default: 'oak'; des - 'Can be excluded' Block set type name; if matches existing BlockSetType (iron, oak, etc.), uses predefined settings; otherwise creates custom type;\n" +
                t + " 8.02 key 'SoundType'; type: 'String'; format: '\"value\"'; default: 'WOOD'; des - 'Can be excluded' Base sound type for the door block; see 'sound' in blockexample for options; if invalid, defaults to WOOD;\n" +
                t + " 8.03 key 'SoundDoorClose'; type: 'String'; format: '\"value\"'; default: 'block.wooden_door.close'; des - 'Can be excluded' Registered key of the sound event for door closing;\n" +
                t + " 8.04 key 'SoundDoorOpen'; type: 'String'; format: '\"value\"'; default: 'block.wooden_door.open'; des - 'Can be excluded' Registered key of the sound event for door opening;\n" +
                t + " 8.05 key 'SoundTrapDoorClose'; type: 'String'; format: '\"value\"'; default: 'block.wooden_trapdoor.close'; des - 'Can be excluded' Registered key of the sound event for trapdoor closing;\n" +
                t + " 8.06 key 'SoundTrapDoorOpen'; type: 'String'; format: '\"value\"'; default: 'block.wooden_trapdoor.open'; des - 'Can be excluded' Registered key of the sound event for trapdoor opening;\n" +
                t + " 8.07 key 'SoundPlateClickOff'; type: 'String'; format: '\"value\"'; default: 'block.wooden_pressure_plate.click_off'; des - 'Can be excluded' Registered key of the sound event for pressure plate deactivation;\n" +
                t + " 8.08 key 'SoundPlateClickOn'; type: 'String'; format: '\"value\"'; default: 'block.wooden_pressure_plate.click_on'; des - 'Can be excluded' Registered key of the sound event for pressure plate activation;\n" +
                t + " 8.09 key 'SoundButtonClickOff'; type: 'String'; format: '\"value\"'; default: 'block.wooden_button.click_off'; des - 'Can be excluded' Registered key of the sound event for button release;\n" +
                t + " 8.10 key 'SoundButtonClickOn'; type: 'String'; format: '\"value\"'; default: 'block.wooden_button.click_on'; des - 'Can be excluded' Registered key of the sound event for button press;\n" +
                t + " 8.11 key 'CanOpenByHand'; type: 'Boolean'; format: false='0b', true='1b'; default: '1b' (true); des - 'Can be excluded' Whether the door can be opened by hand (false = requires redstone/power);";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleItems() {
        if (exampleItems == null) {
            exampleItems = new CompoundTag();
            ListTag listItems = new ListTag();
            listItems.add(getExampleItem());
            listItems.add(getExampleWeapon());
            listItems.add(getExampleTool());
            listItems.add(getExampleAxe());
            listItems.add(getExampleArmor());
            listItems.add(getExampleOBJArmor());
            listItems.add(getExampleShield());
            listItems.add(getExampleBow());
            listItems.add(getExampleFood());
            listItems.add(getExampleFishingRod());
            exampleItems.put("Items", listItems);

            ListTag listPotion = new ListTag();
            listPotion.add(getExamplePotion());
            exampleItems.put("Potions", listPotion);
        }
        return exampleItems;
    }

    public static CompoundTag getExampleItem() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "itemexample");
        compound.putByte("ItemType", (byte) 0);

        CompoundTag properties = new CompoundTag();
        properties.putInt("MaxStackSize", 64);
        compound.put("Properties", properties);

        String sb = "Tags for creating a simple item:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- format of tag values must be respected;\n" +
                t + "1 key 'RegistryName'; type: 'String'; format: '\"value\"'; des - 'Required' Specified name for item registration;\n" +
                t + "2 key 'ItemType'; type: 'Byte'; format: '0b'<>'255b'; des - 'Required' Used to determine the item type during registration;\n" +
                t + "3 key 'ShowInCreative'; type: 'Boolean'; format: false='0b', true='1b'; default: '1b' (true); des - 'Can be excluded' Show this item in the creative inventory tab;\n" +
                t + "4 key 'SpeedAttack'; type: 'Double'; format: '-1.7976931348623157E308'<>'0.0d'<>'1.7976931348623157E308'; default: '-2.4d'; des - 'Can be excluded' Attack speed modifier when used as a weapon; negative = slower;\n" +
                t + "5 key 'EntityDamage'; type: 'Double'; format: '-1.7976931348623157E308'<>'0.0d'<>'1.7976931348623157E308'; default: '0.0d'; des - 'Can be excluded' Attack damage when used as a weapon; if > 0, creates attribute modifiers;\n" +
                t + "6 key 'Enchantability'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; default: 10; des - 'Can be excluded' Enchantability level for enchanting table;\n" +
                t + "7 key 'RepairItem'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' ItemStack NBT for repair material; example: '{id:\"minecraft:iron_ingot\",Count:1b}';\n" +
                t + "8 key 'CollectionBlocks'; type: 'ListTag'; format: '[]'; listType: 'CompoundTag'; tagFormat: '{}'; default: 'not used'; des - 'Can be excluded' List of blocks with custom destroy speeds:\n" +
                t + " 8.01 key 'Name' in 'CompoundTag' option; type: 'String'; format: '\"value\"'; des - 'Required if specified' Block registry name; example: 'minecraft:stone';\n" +
                t + " 8.02 key 'Speed' in 'CompoundTag' option; type: 'Float'; format: '1.17549435E-38f'<>'0.000000f'<>'3.4028235e+38f'; des - 'Required if specified' Destroy speed for this block;\n" +
                t + "9 key 'CollectionBlockTags'; type: 'ListTag'; format: '[]'; listType: 'CompoundTag'; tagFormat: '{}'; default: 'not used'; des - 'Can be excluded' List of block tags with custom destroy speeds:\n" +
                t + " 9.01 key 'Name' in 'CompoundTag' option; type: 'String'; format: '\"value\"'; des - 'Required if specified' Block tag name; example: 'minecraft:mineable/pickaxe';\n" +
                t + " 9.02 key 'Speed' in 'CompoundTag' option; type: 'Float'; format: '1.17549435E-38f'<>'0.000000f'<>'3.4028235e+38f'; des - 'Required if specified' Destroy speed for blocks in this tag;\n" +
                t + "10 key 'Properties'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' Item properties:\n" +
                t + " 10.01 key 'MaxStackDamage'; type: 'Integer'; format: '0'<>'2147483647'; default: 'not used'; des - 'Can be excluded' Durability of the item; if set, item becomes damageable and MaxStackSize is ignored;\n" +
                t + " 10.02 key 'MaxStackSize'; type: 'Integer'; format: '1'<>'64'; default: 64; des - 'Can be excluded' Maximum stack size; only used if MaxStackDamage is not set;\n" +
                t + " 10.03 key 'Rarity'; type: 'String'; format: '\"value\"'; default: 'COMMON'; des - 'Can be excluded' Item rarity for color and tooltip. Options: COMMON, UNCOMMON, RARE, EPIC;\n" +
                t + " 10.04 key 'FireResistant'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Item is immune to fire and lava damage;\n" +
                t + " 10.05 key 'CanRepair'; type: 'Boolean'; format: false='0b', true='1b'; default: '1b' (true); des - 'Can be excluded' Whether the item can be repaired in an anvil; set false to disable repair;\n" +
                t + " 10.06 key 'RepairItem'; type: 'String'; format: '\"value\"'; default: 'not used'; des - 'Can be excluded' Item registry name used as craft remainder (e.g. 'minecraft:bucket');\n" +
                t + " 10.07 key 'requiredFeatures'; type: 'ListTag'; format: '[]'; listType: 'CompoundTag'; tagFormat: '{}'; default: 'not used'; des - 'Can be excluded' A list of required features in the world for an item to exist in it:\n" +
                t + "  10.07.1 key 'UniverseId' in 'CompoundTag' option; type: 'String'; format: '\"value\"'; des - 'Required if specified' requirement ID\n" +
                t + "  10.07.2 key 'Name' in 'CompoundTag' option; type: 'String'; format: '\"value\"'; des - 'Required if specified' sign ID;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleWeapon() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "weaponexample");
        compound.putByte("ItemType", (byte) 1);
        compound.putBoolean("ShowInCreative", true);
        compound.putDouble("SpeedAttack", -2.4d);

        ListTag list = new ListTag();
        CompoundTag collectionBlock = new CompoundTag();
        collectionBlock.putString("Name", "minecraft:cobweb");
        collectionBlock.putFloat("Speed", 15.0f);
        list.add(collectionBlock);
        compound.put("CollectionBlocks", list);

        list = new ListTag();
        CompoundTag collectionBlockTag = new CompoundTag();
        collectionBlockTag.putString("Name", "SWORD_EFFICIENT");
        collectionBlockTag.putFloat("Speed", 1.5f);
        list.add(collectionBlockTag);
        compound.put("CollectionBlockTags", list);

        CompoundTag tier = new CompoundTag();
        tier.putInt("MaxStackDamage", 2500);
        tier.putInt("HarvestLevel", 2);
        tier.putInt("Enchantability", 25);
        tier.putFloat("Efficiency", 6.0f);
        tier.putDouble("EntityDamage", 2.5d);
        tier.put("RepairItem", (new ItemStack(Blocks.GOLD_ORE)).save(new CompoundTag()));
        tier.putString("RepairItemTag", ItemTags.GOLD_ORES.location().toString());
        compound.put("Tier", tier);

        CompoundTag properties = new CompoundTag();
        properties.putInt("MaxStackSize", 1);
        properties.putString("Rarity", "RARE");
        properties.putBoolean("FireResistant", false);
        properties.putBoolean("CanRepair", true);
        compound.put("Properties", properties);

        String sb = "Tags for creating a weapon item:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'ItemType', 'ShowInCreative', 'SpeedAttack', 'CollectionBlocks', 'CollectionBlockTags', 'Properties' - see description of item 'itemexample';\n" +
                t + "8 key 'Tier'; type: 'CompoundTag'; format: '{}'; des - 'Required' Weapon tier properties:\n" +
                t + " 8.01 key 'MaxStackDamage'; type: 'Integer'; format: '0'<>'2147483647'; default: 250; des - 'Can be excluded' Durability of the weapon;\n" +
                t + " 8.02 key 'HarvestLevel'; type: 'Integer'; format: '0'<>'2147483647'; default: 0; des - 'Can be excluded' Mining level (0=wood, 1=stone, 2=iron, 3=diamond, 4=netherite);\n" +
                t + " 8.03 key 'Enchantability'; type: 'Integer'; format: '0'<>'2147483647'; default: 15; des - 'Can be excluded' Enchantability level; overrides root 'Enchantability';\n" +
                t + " 8.04 key 'Efficiency'; type: 'Float'; format: '0.0f'<>'3.4028235e+38f'; default: '2.0f'; des - 'Can be excluded' Mining speed multiplier;\n" +
                t + " 8.05 key 'EntityDamage'; type: 'Double'; format: '0.0d'<>'1.7976931348623157E308'; default: '0.0d'; des - 'Can be excluded' Attack damage bonus added to base sword damage;\n" +
                t + " 8.06 key 'RepairItem'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' ItemStack NBT for repair material;\n" +
                t + " 8.07 key 'RepairItemTag'; type: 'String'; format: '\"value\"'; default: 'minecraft:planks'; des - 'Can be excluded' Item tag used for repair if RepairItem is not set; example: 'minecraft:planks', 'minecraft:gold_ores';\n" +
                t + "- Note: 'EntityDamage' in 'Tier' is used for weapon damage. Root 'EntityDamage' is ignored for weapons;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleTool() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "toolexample");
        compound.putByte("ItemType", (byte) 2);
        compound.putDouble("SpeedAttack", -2.8d);
        compound.putString("ToolClass", "pickaxe");

        ListTag list = new ListTag();
        CompoundTag collectionBlock = new CompoundTag();
        collectionBlock.putString("Name", "minecraft:stone");
        collectionBlock.putFloat("Speed", 15.0f);
        list.add(collectionBlock);
        collectionBlock = new CompoundTag();
        collectionBlock.putString("Name", "minecraft:obsidian");
        collectionBlock.putFloat("Speed", 15.0f);
        list.add(collectionBlock);
        compound.put("CollectionBlocks", list);

        list = new ListTag();
        CompoundTag collectionBlockTag = new CompoundTag();
        collectionBlockTag.putString("Name", "needs_stone_tool");
        collectionBlockTag.putFloat("Speed", 1.5f);
        list.add(collectionBlockTag);
        compound.put("CollectionBlockTags", list);

        CompoundTag tier = new CompoundTag();
        tier.putInt("MaxStackDamage", 2000);
        tier.putInt("HarvestLevel", 3);
        tier.putInt("Enchantability", 25);
        tier.putFloat("Efficiency", 4.0f);
        tier.putDouble("EntityDamage", 0.0d);
        tier.put("RepairItem", (new ItemStack(Items.GOLD_NUGGET)).save(new CompoundTag()));
        tier.putString("RepairItemTag", "obsidian");
        compound.put("Tier", tier);

        CompoundTag properties = new CompoundTag();
        properties.putInt("MaxStackSize", 1);
        properties.putString("Rarity", "RARE");
        properties.putBoolean("FireResistant", false);
        properties.putBoolean("CanRepair", true);
        compound.put("Properties", properties);

        String sb = "Tags for creating a tool item:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'ItemType', 'ShowInCreative', 'CollectionBlocks', 'CollectionBlockTags', 'Properties' - see description of item 'itemexample';\n" +
                t + "8 key 'ToolClass'; type: 'String'; format: '\"value\"'; default: 'pickaxe'; des - 'Required' Tool class type. Options: pickaxe, axe, hoe, shovel;\n" +
                t + "9 key 'SpeedAttack'; type: 'Double'; format: '-1.7976931348623157E308'<>'0.0d'<>'1.7976931348623157E308'; default: '-2.8d'; des - 'Can be excluded' Attack speed modifier; negative = slower than default;\n" +
                t + "10 key 'Tier'; type: 'CompoundTag'; format: '{}'; des - 'Required' Tool tier properties:\n" +
                t + " 10.01 key 'MaxStackDamage'; type: 'Integer'; format: '0'<>'2147483647'; default: 250; des - 'Can be excluded' Durability of the tool;\n" +
                t + " 10.02 key 'HarvestLevel'; type: 'Integer'; format: '0'<>'2147483647'; default: 0; des - 'Can be excluded' Mining level (0=wood, 1=stone, 2=iron, 3=diamond, 4=netherite);\n" +
                t + " 10.03 key 'Enchantability'; type: 'Integer'; format: '0'<>'2147483647'; default: 15; des - 'Can be excluded' Enchantability level;\n" +
                t + " 10.04 key 'Efficiency'; type: 'Float'; format: '0.0f'<>'3.4028235e+38f'; default: '2.0f'; des - 'Can be excluded' Mining speed multiplier;\n" +
                t + " 10.05 key 'EntityDamage'; type: 'Double'; format: '0.0d'<>'1.7976931348623157E308'; default: '0.0d'; des - 'Can be excluded' Attack damage bonus;\n" +
                t + " 10.06 key 'RepairItem'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' ItemStack NBT for repair material;\n" +
                t + " 10.07 key 'RepairItemTag'; type: 'String'; format: '\"value\"'; default: 'minecraft:planks'; des - 'Can be excluded' Item tag used for repair if RepairItem is not set;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleAxe() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "axeexample");
        compound.putByte("ItemType", (byte) 2);
        compound.putDouble("SpeedAttack", -2.4d);
        compound.putString("ToolClass", "axe");
        compound.putBoolean("IsOBJModel", true);

        CompoundTag tier = new CompoundTag();
        tier.putInt("MaxStackDamage", 2200);
        tier.putInt("HarvestLevel", 2);
        tier.putInt("Enchantability", 28);
        tier.putFloat("Efficiency", 4.25f);
        tier.putDouble("EntityDamage", 5.0d);
        tier.put("RepairItem", (new ItemStack(Items.GOLD_NUGGET)).save(new CompoundTag()));
        tier.putString("RepairItemTag", "obsidian");
        compound.put("Tier", tier);

        CompoundTag properties = new CompoundTag();
        properties.putInt("MaxStackSize", 1);
        properties.putString("Rarity", "RARE");
        properties.putBoolean("FireResistant", false);
        properties.putBoolean("CanRepair", true);
        compound.put("Properties", properties);

        String sb = "Tags for creating an axe item:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'ItemType', 'ShowInCreative', 'ToolClass', 'SpeedAttack', 'CollectionBlocks', 'CollectionBlockTags', 'Tier', 'Properties' - see description of items 'itemexample' and 'toolexample';\n" +
                t + "- Set 'ToolClass' to 'axe' to create an axe;\n" +
                t + "- All other tags are identical to 'toolexample';";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleArmor() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "armorexample");
        compound.putByte("ItemType", (byte) 3);
        compound.putString("Material", "GOLD");

        compound.put("RepairItem", (new ItemStack(Items.GOLD_NUGGET)).save(new CompoundTag()));

        CompoundTag properties = new CompoundTag();
        properties.putInt("MaxStackSize", 1);
        properties.putString("RepairItem", "minecraft:gold_nugget");
        compound.put("Properties", properties);

        ListTag slots = new ListTag();
        CompoundTag part = new CompoundTag();
            part.putString("Slot", "HEAD");
            part.putInt("MaxStackDamage", 2250);
            part.putInt("Defense", 5);
            part.putFloat("Toughness", 2.2f);
            part.putInt("Enchantability", 22);
            part.putFloat("KnockbackResistance", 0.0F);
        slots.add(part);
        part = new CompoundTag();
            part.putString("Slot", "Chest");
            part.putInt("MaxStackDamage", 3100);
            part.putInt("Defense", 7);
            part.putFloat("Toughness", 3.5f);
            part.putInt("Enchantability", 25);
        part.putFloat("KnockbackResistance", 0.0F);
        slots.add(part);
        part = new CompoundTag();
            part.putString("Slot", "feet");
            part.putInt("MaxStackDamage", 1800);
            part.putInt("Defense", 4);
            part.putFloat("Toughness", 1.8f);
            part.putInt("Enchantability", 22);
            part.putFloat("KnockbackResistance", 0.0F);
        slots.add(part);
        compound.put("EquipmentSlots", slots);

        String sb = "Tags for creating an armor item:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'ItemType', 'ShowInCreative', 'Properties' - see description of item 'itemexample';\n" +
                t + "8 key 'Material'; type: 'String'; format: '\"value\"'; default: 'LEATHER'; des - 'Can be excluded' Armor material type. Options: LEATHER, CHAIN, IRON, GOLD, DIAMOND, TURTLE, NETHERITE;\n" +
                t + "9 key 'RepairItem'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' ItemStack NBT for repair material of all armor pieces;\n" +
                t + "10 key 'EquipmentSlots'; type: 'ListTag'; format: '[]'; listType: 'CompoundTag'; tagFormat: '{}'; des - 'Required' List of armor parts to create:\n" +
                t + " 10.01 key 'Slot'; type: 'String'; format: '\"value\"'; des - 'Required' Equipment slot. Options: HEAD, CHEST, LEGS, FEET;\n" +
                t + " 10.02 key 'MaxStackDamage'; type: 'Integer'; format: '0'<>'2147483647'; des - 'Can be excluded' Durability of this armor piece;\n" +
                t + " 10.03 key 'Defense'; type: 'Integer'; format: '0'<>'2147483647'; des - 'Can be excluded' Armor defense points;\n" +
                t + " 10.04 key 'Toughness'; type: 'Float'; format: '0.0f'<>'3.4028235e+38f'; des - 'Can be excluded' Armor toughness value;\n" +
                t + " 10.05 key 'Enchantability'; type: 'Integer'; format: '0'<>'2147483647'; des - 'Can be excluded' Enchantability level for this piece;\n" +
                t + " 10.06 key 'KnockbackResistance'; type: 'Float'; format: '0.0f'<>'3.4028235e+38f'; default: '0.0f'; des - 'Can be excluded' Knockback resistance (0.0=none, 1.0=full immunity);\n" +
                t + "11 key 'OBJData'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' OBJ model mesh assignments for custom armor rendering:\n" +
                t + " 11.01 key 'Head Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for head part;\n" +
                t + " 11.02 key 'Body Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for body part;\n" +
                t + " 11.03 key 'Arm Right Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for right arm;\n" +
                t + " 11.04 key 'Wrist Right Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for right wrist;\n" +
                t + " 11.05 key 'Arm Left Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for left arm;\n" +
                t + " 11.06 key 'Wrist Left Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for left wrist;\n" +
                t + " 11.07 key 'Belt Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for belt;\n" +
                t + " 11.08 key 'Leg Right Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for right leg;\n" +
                t + " 11.09 key 'Foot Right Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for right foot;\n" +
                t + " 11.10 key 'Leg Left Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for left leg;\n" +
                t + " 11.11 key 'Foot Left Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for left foot;\n" +
                t + " 11.12 key 'Boot Right Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for right boot;\n" +
                t + " 11.13 key 'Boot Left Mesh Names'; type: 'ListTag'; listType: 'String'; des - 'Can be excluded' Mesh names for left boot;\n" +
                t + "12 key 'Display'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' Camera transform overrides per slot and display context:\n" +
                t + " 12.01 key 'HEAD'/'CHEST'/'LEGS'/'FEET'; type: 'CompoundTag'; des - 'Can be excluded' Slot-specific transforms:\n" +
                t + "  12.01.1 key 'thirdperson_lefthand'/'thirdperson_righthand'/'firstperson_lefthand'/'firstperson_righthand'/'head'/'gui'/'ground'/'fixed'; type: 'CompoundTag'; des - 'Can be excluded' Display context:\n" +
                t + "   12.01.1.1 key 'rotation'; type: 'ListTag'; listType: 'Float'; size: 3; des - 'Can be excluded' [x, y, z] rotation in degrees;\n" +
                t + "   12.01.1.2 key 'translation'; type: 'ListTag'; listType: 'Float'; size: 3; des - 'Can be excluded' [x, y, z] translation;\n" +
                t + "   12.01.1.3 key 'scale'; type: 'ListTag'; listType: 'Float'; size: 3; des - 'Can be excluded' [x, y, z] scale;\n" +
                t + "- Note: Each armor piece is registered as a separate item with suffix _helmet, _chestplate, _leggings, _boots;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleOBJArmor() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "armorobjexample");
        compound.putByte("ItemType", (byte) 3);
        compound.putString("Material", "IRON");

        compound.put("RepairItem", (new ItemStack(Items.IRON_INGOT)).save(new CompoundTag()));

        CompoundTag properties = new CompoundTag();
        properties.putInt("MaxStackSize", 1);
        properties.putString("RepairItem", "minecraft:iron_ingot");
        compound.put("Properties", properties);

        ListTag slots = new ListTag();
        CompoundTag part = new CompoundTag();
            part.putString("Slot", "HEAD");
            part.putInt("MaxStackDamage", 2250);
            part.putInt("Defense", 5);
            part.putFloat("Toughness", 2.2f);
            part.putInt("Enchantability", 22);
            part.putFloat("KnockbackResistance", 0.0F);
        slots.add(part);
        part = new CompoundTag();
            part.putString("Slot", "Chest");
            part.putInt("MaxStackDamage", 3100);
            part.putInt("Defense", 7);
            part.putFloat("Toughness", 3.5f);
            part.putInt("Enchantability", 25);
            part.putFloat("KnockbackResistance", 0.0F);
        slots.add(part);
        part = new CompoundTag();
            part.putString("Slot", "LeGs");
            part.putInt("MaxStackDamage", 2700);
            part.putInt("Defense", 6);
            part.putFloat("Toughness", 2.6f);
            part.putInt("Enchantability", 23);
            part.putFloat("KnockbackResistance", 0.0F);
        slots.add(part);
        part = new CompoundTag();
            part.putString("Slot", "feet");
            part.putInt("MaxStackDamage", 1800);
            part.putInt("Defense", 4);
            part.putFloat("Toughness", 1.8f);
            part.putInt("Enchantability", 22);
            part.putFloat("KnockbackResistance", 0.0F);
        slots.add(part);
        compound.put("EquipmentSlots", slots);

        CompoundTag objData = new CompoundTag();
            ListTag meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.HEAD.name));
            objData.put("Head Mesh Names", meshes);

            meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.BODY.name));
            objData.put("Body Mesh Names", meshes);

            meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.ARM_RIGHT.name));
            objData.put("Arm Right Mesh Names", meshes);

            meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.WRIST_RIGHT.name));
            objData.put("Wrist Right Mesh Names", meshes);

            meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.ARM_LEFT.name));
            objData.put("Arm Left Mesh Names", meshes);

            meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.WRIST_LEFT.name));
            objData.put("Wrist Left Mesh Names", meshes);

            meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.BELT.name));
            objData.put("Belt Mesh Names", meshes);

            meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.LEG_RIGHT.name));
            objData.put("Leg Right Mesh Names", meshes);

            meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.FEET_RIGHT.name));
            objData.put("Foot Right Mesh Names", meshes);

            meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.LEG_LEFT.name));
            objData.put("Leg Left Mesh Names", meshes);

            meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.FEET_LEFT.name));
            objData.put("Foot Left Mesh Names", meshes);

            meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.FEET_LEFT.name));
            objData.put("Boot Left Mesh Names", meshes);

            meshes = new ListTag();
            meshes.add(StringTag.valueOf(EnumParts.FEET_RIGHT.name));
        objData.put("Boot Right Mesh Names", meshes);

        compound.put("OBJData", objData);

        CompoundTag display = new CompoundTag();
        for (int s = 0; s < 4; s++) {
            String slot = s == 0 ? "CHEST" : s == 1 ? "LEGS" : s == 2 ? "FEET" : "HEAD";
            CompoundTag cameraData = new CompoundTag();
            for (int i = 0; i < 8; i++) {
                String p;
                ListTag rotation = new ListTag();
                ListTag translation = new ListTag();
                ListTag scale = new ListTag();
                switch(i) {
                    case 0: { // THIRD_PERSON_LEFT_HAND
                        p = "thirdperson_lefthand";
                        switch(slot) {
                            case "CHEST": {
                                translation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.5f)); }
                                break;
                            }
                            case "LEGS": {
                                translation.add(FloatTag.valueOf(-0.15f));
                                translation.add(FloatTag.valueOf(0.35f));
                                translation.add(FloatTag.valueOf(0.5f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.65f)); }
                                break;
                            }
                            case "FEET": {
                                rotation.add(FloatTag.valueOf(90.0f));
                                rotation.add(FloatTag.valueOf(180.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(1.15f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.5f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.65f)); }
                                break;
                            }
                            default: {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(180.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(1.0f));
                                translation.add(FloatTag.valueOf(-0.375f));
                                translation.add(FloatTag.valueOf(0.5f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.5f)); }
                                break;
                            }
                        }
                        break;
                    }
                    case 1: { // THIRD_PERSON_RIGHT_HAND
                        p = "thirdperson_righthand";
                        switch(slot) {
                            case "CHEST": {
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.5f)); }
                                break;
                            }
                            case "LEGS": {
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.35f));
                                translation.add(FloatTag.valueOf(0.5f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.65f)); }
                                break;
                            }
                            case "FEET": {
                                rotation.add(FloatTag.valueOf(90.0f));
                                rotation.add(FloatTag.valueOf(180.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.5f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.65f)); }
                                break;
                            }
                            default: {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(180.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(-0.375f));
                                translation.add(FloatTag.valueOf(0.5f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.5f)); }
                                break;
                            }
                        }
                        break;
                    }
                    case 2: { // FIRST_PERSON_LEFT_HAND
                        p = "firstperson_lefthand";
                        switch(slot) {
                            case "CHEST": {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(280.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.57f));
                                translation.add(FloatTag.valueOf(0.1f));
                                translation.add(FloatTag.valueOf(-0.085f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.5f)); }
                                break;
                            }
                            case "LEGS": {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(280.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.65f));
                                translation.add(FloatTag.valueOf(0.4f));
                                translation.add(FloatTag.valueOf(-0.085f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.5f)); }
                                break;
                            }
                            case "FEET": {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(280.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.72f));
                                translation.add(FloatTag.valueOf(0.435f));
                                translation.add(FloatTag.valueOf(-0.585f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.85f)); }
                                break;
                            }
                            default: {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(280.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.57f));
                                translation.add(FloatTag.valueOf(-0.225f));
                                translation.add(FloatTag.valueOf(-0.085f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.5f)); }
                                break;
                            }
                        }
                        break;
                    }
                    case 3: { // FIRST_PERSON_RIGHT_HAND
                        p = "firstperson_righthand";
                        switch(slot) {
                            case "CHEST": {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(280.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.85f));
                                translation.add(FloatTag.valueOf(-0.1f));
                                translation.add(FloatTag.valueOf(0.2f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.6f)); }
                                break;
                            }
                            case "LEGS": {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(280.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.95f));
                                translation.add(FloatTag.valueOf(0.25f));
                                translation.add(FloatTag.valueOf(0.2f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.6f)); }
                                break;
                            }
                            case "FEET": {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(280.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.95f));
                                translation.add(FloatTag.valueOf(0.4f));
                                translation.add(FloatTag.valueOf(0.2f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.85f)); }
                                break;
                            }
                            default: {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(280.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.85f));
                                translation.add(FloatTag.valueOf(-0.5f));
                                translation.add(FloatTag.valueOf(0.2f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.6f)); }
                                break;
                            }
                        }
                        break;
                    }
                    case 4: { // HEAD
                        p = "head";
                        switch(slot) {
                            case "CHEST": {
                                rotation.add(FloatTag.valueOf(270.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(1.0f));
                                translation.add(FloatTag.valueOf(1.65f));
                                break;
                            }
                            case "LEGS": {
                                rotation.add(FloatTag.valueOf(270.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(1.0f));
                                translation.add(FloatTag.valueOf(1.0f));
                                break;
                            }
                            case "FEET": {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(180.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.925f));
                                translation.add(FloatTag.valueOf(0.4f));
                                break;
                            }
                            default: { break; }
                        }
                        break;
                    }
                    case 5: { // GUI
                        p = "gui";
                        switch(slot) {
                            case "CHEST": {
                                rotation.add(FloatTag.valueOf(30.0f));
                                rotation.add(FloatTag.valueOf(45.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.49f));
                                translation.add(FloatTag.valueOf(-0.41f));
                                translation.add(FloatTag.valueOf(0.0f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.9f)); }
                                break;
                            }
                            case "LEGS": {
                                rotation.add(FloatTag.valueOf(30.0f));
                                rotation.add(FloatTag.valueOf(45.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.05f));
                                translation.add(FloatTag.valueOf(0.0f));
                                break;
                            }
                            case "FEET": {
                                rotation.add(FloatTag.valueOf(30.0f));
                                rotation.add(FloatTag.valueOf(45.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.3f));
                                translation.add(FloatTag.valueOf(0.0f));
                                break;
                            }
                            default: {
                                rotation.add(FloatTag.valueOf(30.0f));
                                rotation.add(FloatTag.valueOf(45.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(-1.0f));
                                translation.add(FloatTag.valueOf(0.0f));
                                break;
                            }
                        }
                        break;
                    }
                    case 6: { // GROUND
                        p = "ground";
                        switch(slot) {
                            case "CHEST": {
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.5f)); }
                                break;
                            }
                            case "LEGS": {
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.25f));
                                translation.add(FloatTag.valueOf(0.5f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.6f)); }
                                break;
                            }
                            case "FEET": {
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.35f));
                                translation.add(FloatTag.valueOf(0.5f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.65f)); }
                                break;
                            }
                            default: {
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(-0.375f));
                                translation.add(FloatTag.valueOf(0.5f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.5f)); }
                                break;
                            }
                        }
                        break;
                    }
                    default: { // FIXED
                        p = "fixed";
                        switch(slot) {
                            case "CHEST": {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(180.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(-0.65f));
                                translation.add(FloatTag.valueOf(0.45f));
                                break;
                            }
                            case "LEGS": {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(180.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.05f));
                                translation.add(FloatTag.valueOf(0.475f));
                                break;
                            }
                            case "FEET": {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(180.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(0.2f));
                                translation.add(FloatTag.valueOf(0.475f));
                                break;
                            }
                            default: {
                                rotation.add(FloatTag.valueOf(0.0f));
                                rotation.add(FloatTag.valueOf(180.0f));
                                rotation.add(FloatTag.valueOf(0.0f));
                                translation.add(FloatTag.valueOf(0.5f));
                                translation.add(FloatTag.valueOf(-0.85f));
                                translation.add(FloatTag.valueOf(0.4f));
                                for (int l = 0; l < 3; l++) { scale.add(FloatTag.valueOf(0.75f)); }
                                break;
                            }
                        }
                        break;
                    }
                }
                CompoundTag transform = new CompoundTag();
                if (!rotation.isEmpty()) { transform.put("rotation", rotation); }
                if (!translation.isEmpty()) { transform.put("translation", translation); }
                if (!scale.isEmpty()) { transform.put("scale", scale); }
                cameraData.put(p, transform);
            }
            display.put(slot, cameraData);
        }
        compound.put("Display", display);

        String sb = "Tags for creating an OBJ armor item:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'ItemType', 'ShowInCreative', 'Material', 'RepairItem', 'EquipmentSlots', 'OBJData', 'Display', 'Properties' - see description of item 'armorexample';\n" +
                t + "- Set 'IsOBJModel' in 'Properties' to true for OBJ rendering;\n" +
                t + "- 'OBJData' mesh names map to custom OBJ model parts for each body slot;\n" +
                t + "- 'Display' transforms are especially important for OBJ armor to position correctly in hand/GUI;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleShield() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "shieldexample");
        compound.putByte("ItemType", (byte) 4);
        compound.putInt("Enchantability", 15);
        compound.put("RepairItem", (new ItemStack(Items.IRON_NUGGET)).save(new CompoundTag()));

        CompoundTag properties = new CompoundTag();
        properties.putInt("MaxStackDamage", 6500);
        properties.putInt("MaxStackSize", 1);
        compound.put("Properties", properties);

        String sb = "Tags for creating a shield item:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'ItemType', 'ShowInCreative', 'Properties' - see description of item 'itemexample';\n" +
                t + "8 key 'Enchantability'; type: 'Integer'; format: '0'<>'2147483647'; default: 0; des - 'Can be excluded' Shield enchantability level;\n" +
                t + "9 key 'RepairItem'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' ItemStack NBT for repair material;\n" +
                t + "- Note: 'Properties.MaxStackDamage' sets shield durability. Default in example: 6500;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleBow() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "bowexample");
        compound.putByte("ItemType", (byte) 5);
        compound.putInt("Enchantability", 2);
        compound.putDouble("EntityDamage", 2.0d);
        compound.putBoolean("SetFlame", false);
        compound.putFloat("CritChance", 0.25f);
        compound.putFloat("DrawstringSpeed", 20.0f);
        compound.put("RepairItem", (new ItemStack(Items.OAK_PLANKS)).save(new CompoundTag()));

        CompoundTag properties = new CompoundTag();
        properties.putInt("MaxStackDamage", 1250);
        properties.putInt("MaxStackSize", 1);
        compound.put("Properties", properties);

        String sb = "Tags for creating a bow item:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'ItemType', 'ShowInCreative', 'Properties' - see description of item 'itemexample';\n" +
                t + "8 key 'Enchantability'; type: 'Integer'; format: '0'<>'2147483647'; default: 1; des - 'Can be excluded' Bow enchantability level;\n" +
                t + "9 key 'RepairItem'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' ItemStack NBT for repair material;\n" +
                t + "10 key 'Bullet'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' ItemStack NBT for custom projectile; if not set, uses default arrow;\n" +
                t + "11 key 'SetFlame'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' All shots are flaming arrows (like Flame enchantment);\n" +
                t + "12 key 'CritChance'; type: 'Float'; format: '0.0f'<>'1.0f'; default: '0.0f'; des - 'Can be excluded' Chance for critical hit; 0.0=never, 1.0=always;\n" +
                t + "13 key 'EntityDamage'; type: 'Double'; format: '0.0d'<>'1.7976931348623157E308'; default: '2.0d'; des - 'Can be excluded' Base arrow damage; scales with draw time (full draw = 100% damage);\n" +
                t + "14 key 'DrawstringSpeed'; type: 'Float'; format: '0.0f'<>'3.4028235e+38f'; default: '30.0f'; des - 'Can be excluded' Bow draw speed; higher = faster full draw;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleFood() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "foodexample");
        compound.putByte("ItemType", (byte) 6);
        compound.putInt("UseDuration", 32);

        CompoundTag properties = new CompoundTag();
        properties.putInt("MaxStackSize", 32);
        compound.put("Properties", properties);

        CompoundTag foodData = new CompoundTag();
        foodData.putInt("Nutrition", 1);
        foodData.putFloat("Saturation", 0.1f);
        foodData.putBoolean("IsMeat", false);
        foodData.putBoolean("IsFastFood", false);
        foodData.putBoolean("AlwaysEdible", true);
        ListTag list = new ListTag();
        CompoundTag effect = new CompoundTag();
        effect.putString("Name", "fire_resistance");
        effect.putInt("Id", 12);
        effect.putInt("DurationTicks", 45);
        effect.putInt("Amplifier", 0);
        effect.putBoolean("Ambient", true);
        effect.putBoolean("ShowParticles", false);
        effect.putBoolean("ShowIcon", false);
        effect.putFloat("Probability", 0.15f);
        list.add(effect);
        foodData.put("Effects", list);
        compound.put("FoodData", foodData);

        String sb = "Tags for creating a food item:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'ItemType', 'ShowInCreative', 'Properties' - see description of item 'itemexample';\n" +
                t + "8 key 'UseDuration'; type: 'Integer'; format: '16'<>'1200'; min: 16; max: 1200; default: 32; des - 'Can be excluded' Ticks to eat the food; lower = faster eating; if food is fast, duration is halved;\n" +
                t + "9 key 'FoodData'; type: 'CompoundTag'; format: '{}'; des - 'Required' Food properties (also processed via 'Properties.FoodData'):\n" +
                t + " 9.01 key 'Nutrition'; type: 'Integer'; format: '0'<>'2147483647'; default: 3; des - 'Can be excluded' Hunger points restored;\n" +
                t + " 9.02 key 'Saturation'; type: 'Float'; format: '0.0f'<>'3.4028235e+38f'; default: '0.3f'; des - 'Can be excluded' Saturation restored;\n" +
                t + " 9.03 key 'IsMeat'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Whether the food is meat (affects wolf feeding, etc.);\n" +
                t + " 9.04 key 'AlwaysEdible'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Can be eaten even when hunger is full (like golden apple);\n" +
                t + " 9.05 key 'IsFastFood'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' Eats twice as fast (like dried kelp); halves UseDuration;\n" +
                t + " 9.06 key 'Effects'; type: 'ListTag'; format: '[]'; listType: 'CompoundTag'; tagFormat: '{}'; default: 'not used'; des - 'Can be excluded' Potion effects applied when eaten:\n" +
                t + "  9.06.1 key 'Name'; type: 'String'; format: '\"value\"'; des - 'Can be excluded' Effect registry name; example: 'minecraft:fire_resistance';\n" +
                t + "  9.06.2 key 'Id'; type: 'Integer'; format: '1'<>'33'; des - 'Can be excluded' Vanilla effect ID (1=speed, 2=slowness, ..., 33=darkness); used if Name is not set;\n" +
                t + "  9.06.3 key 'DurationTicks'; type: 'Integer'; format: '0'<>'2147483647'; default: 100; des - 'Can be excluded' Effect duration in ticks;\n" +
                t + "  9.06.4 key 'Amplifier'; type: 'Integer'; format: '0'<>'2147483647'; default: 0; des - 'Can be excluded' Effect level (0=I, 1=II, etc.);\n" +
                t + "  9.06.5 key 'Ambient'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b'; des - 'Can be excluded' Whether effect is ambient (beacon-like);\n" +
                t + "  9.06.6 key 'ShowParticles'; type: 'Boolean'; format: false='0b', true='1b'; default: '1b'; des - 'Can be excluded' Show effect particles;\n" +
                t + "  9.06.7 key 'ShowIcon'; type: 'Boolean'; format: false='0b', true='1b'; default: '1b'; des - 'Can be excluded' Show effect icon in inventory;\n" +
                t + "  9.06.8 key 'Probability'; type: 'Float'; format: '0.0f'<>'1.0f'; default: '1.0f'; des - 'Can be excluded' Chance to apply this effect; 1.0f = 100%;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleFishingRod() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "fishingrodexample");
        compound.putByte("ItemType", (byte) 8);
        compound.putInt("Enchantability", 5);
        compound.put("RepairItem", (new ItemStack(Items.STICK)).save(new CompoundTag()));
        compound.putInt("AddSpeedBonus", -1);
        compound.putInt("AddLuckBonus", 1);
        compound.putInt("FishingLineColor", 0xFF00EA);
        compound.putString("FishingHookTexture", "custom_fishing_hook");

        CompoundTag properties = new CompoundTag();
        properties.putInt("MaxStackDamage", 150);
        properties.putInt("MaxStackSize", 1);
        compound.put("Properties", properties);

        String sb = "Tags for creating a fishing rod item:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'ItemType', 'ShowInCreative', 'Properties' - see description of item 'itemexample';\n" +
                t + "8 key 'Enchantability'; type: 'Integer'; format: '0'<>'2147483647'; default: 1; des - 'Can be excluded' Fishing rod enchantability level;\n" +
                t + "9 key 'RepairItem'; type: 'CompoundTag'; format: '{}'; des - 'Can be excluded' ItemStack NBT for repair material;\n" +
                t + "10 key 'AddSpeedBonus'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; default: 0; des - 'Can be excluded' Bonus to fishing speed (added to Lure enchantment); negative = slower;\n" +
                t + "11 key 'AddLuckBonus'; type: 'Integer'; format: '-2147483648'<>'0'<>'2147483647'; default: 0; des - 'Can be excluded' Bonus to fishing luck (added to Luck of the Sea enchantment);\n" +
                t + "12 key 'FishingLineColor'; type: 'Integer'; format: '0'<>'16777215'; default: 0; des - 'Can be excluded' Hex color of the fishing line; 0xFF00EA = magenta;\n" +
                t + "13 key 'FishingHookTexture'; type: 'String'; format: '\"value\"'; default: 'not used'; des - 'Can be excluded' Custom texture name for the fishing hook; file: assets/customnpcs/textures/entity/{name}.png;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExamplePotion() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "potionexample");
        compound.putByte("ItemType", (byte) 7);

        CompoundTag properties = new CompoundTag();
        properties.putInt("MaxStackSize", 16);
        compound.put("Properties", properties);

        compound.putString("Category", "beneficial");
        compound.putBoolean("IsInstant", false);
        compound.putBoolean("VisibleInInventory", true);
        compound.putBoolean("VisibleInGui", true);
        compound.putInt("LiquidColor", 0xFFFFFF);

        compound.putInt("BaseDelay", 200);
        compound.putInt("Duration", 20);
        compound.put("CureItem", (new ItemStack(Items.CARROT)).save(new CompoundTag()));

        ListTag potionModifiers = new ListTag();
        potionModifiers.add(getExamplePotionModifier());
        compound.put("Modifiers", potionModifiers);

        String sb = "Tags for creating a custom potion effect:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- format of tag values must be respected;\n" +
                t + "1 key 'RegistryName'; type: 'String'; format: '\"value\"'; des - 'Required' Specified name for potion registration; creates 3 variants: {name}, {name}_long, {name}_strong;\n" +
                t + "2 key 'ItemType'; type: 'Byte'; format: '7b'; des - 'Required' Must be 7 for potion type;\n" +
                t + "3 key 'ShowInCreative'; type: 'Boolean'; format: false='0b', true='1b'; default: '1b' (true); des - 'Can be excluded' Show this potion in the creative inventory tab;\n" +
                t + "4 key 'Category'; type: 'String'; format: '\"value\"'; default: 'neutral'; des - 'Can be excluded' Potion effect category. Options: beneficial, harmful, neutral;\n" +
                t + "5 key 'LiquidColor'; type: 'Integer'; format: '0'<>'16777215'; default: 0; des - 'Can be excluded' Color of the potion liquid in GUI and particle effects (hex ARGB);\n" +
                t + "6 key 'IsInstant'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' If true, effect applies instantly (like Instant Health); BaseDelay is ignored;\n" +
                t + "7 key 'BaseDelay'; type: 'Integer'; format: '20'<>'2147483647'; min: 20; default: 200; des - 'Can be excluded' Base duration in ticks for the standard potion variant;\n" +
                t + "- Duration variants: POTION = BaseDelay, LONG = BaseDelay * 3 (min 60), STRONG = BaseDelay / 2 (min 20);\n" +
                t + "8 key 'CureItem'; type: 'CompoundTag'; format: '{}'; default: 'empty'; des - 'Can be excluded' ItemStack NBT that cures this effect (like milk bucket); example: '{id:\"minecraft:milk_bucket\",Count:1b}';\n" +
                t + "9 key 'Modifiers'; type: 'ListTag'; format: '[]'; listType: 'CompoundTag'; tagFormat: '{}'; default: 'not used'; des - 'Can be excluded' Attribute modifiers applied while the effect is active:\n" +
                t + " 9.01 key 'AttributeName'; type: 'String'; format: '\"value\"'; des - 'Required if specified' Attribute registry name; example: 'minecraft:generic.attack_damage';\n" +
                t + " 9.02 key 'AttributeDefValue'; type: 'Double'; format: '-1.7976931348623157E308'<>'0.0d'<>'1.7976931348623157E308'; des - 'Required if specified' Default value for the attribute;\n" +
                t + " 9.03 key 'AttributeMinValue'; type: 'Double'; format: '-1.7976931348623157E308'<>'0.0d'<>'1.7976931348623157E308'; des - 'Required if specified' Minimum allowed value for the attribute;\n" +
                t + " 9.04 key 'AttributeMaxValue'; type: 'Double'; format: '-1.7976931348623157E308'<>'0.0d'<>'1.7976931348623157E308'; des - 'Required if specified' Maximum allowed value for the attribute;\n" +
                t + " 9.05 key 'UUID'; type: 'String'; format: '\"value\"'; default: 'random UUID'; des - 'Can be excluded' Unique identifier for the modifier; if invalid, random UUID is generated;\n" +
                t + " 9.06 key 'Amount'; type: 'Double'; format: '-1.7976931348623157E308'<>'0.0d'<>'1.7976931348623157E308'; des - 'Required if specified' Modifier amount added to the attribute;\n" +
                t + " 9.07 key 'Operation'; type: 'Integer'; format: '0'<>'2'; default: 0; des - 'Can be excluded' Modifier operation: 0=ADDITION, 1=MULTIPLY_BASE, 2=MULTIPLY_TOTAL;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExamplePotionModifier() {
        CompoundTag compound = new CompoundTag();
        compound.putString("AttributeName", "generic.maxHealth");
        compound.putString("UUID", UUID.randomUUID().toString());
        compound.putDouble("AttributeDefValue", 5.0d);
        compound.putDouble("AttributeMinValue", -50.0d);
        compound.putDouble("AttributeMaxValue", 50.0d);
        compound.putDouble("Amount", 2.0d);
        compound.putInt("Operation", 2);

        String sb = "Tags for creating a potion attribute modifier:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- format of tag values must be respected;\n" +
                t + "- This tag is used inside the 'Modifiers' list of a potion;\n" +
                t + "1 key 'AttributeName'; type: 'String'; format: '\"value\"'; des - 'Required' Attribute registry name; example: 'minecraft:generic.attack_damage', 'minecraft:generic.movement_speed';\n" +
                t + "2 key 'AttributeDefValue'; type: 'Double'; format: '-1.7976931348623157E308'<>'0.0d'<>'1.7976931348623157E308'; default: '2.0d'; des - 'Required' Default value for the attribute;\n" +
                t + "3 key 'AttributeMinValue'; type: 'Double'; format: '-1.7976931348623157E308'<>'0.0d'<>'1.7976931348623157E308'; default: '0.0d'; des - 'Required' Minimum allowed value for the attribute;\n" +
                t + "4 key 'AttributeMaxValue'; type: 'Double'; format: '-1.7976931348623157E308'<>'0.0d'<>'1.7976931348623157E308'; default: '2048.0d'; des - 'Required' Maximum allowed value for the attribute;\n" +
                t + "5 key 'UUID'; type: 'String'; format: '\"value\"'; default: 'random UUID'; des - 'Can be excluded' Unique identifier for the modifier; if invalid or omitted, a random UUID is generated; example: 'CB3F55D3-645C-4F38-A497-9C13A33DB5CF';\n" +
                t + "6 key 'Amount'; type: 'Double'; format: '-1.7976931348623157E308'<>'0.0d'<>'1.7976931348623157E308'; default: '0.0d'; des - 'Required' Modifier amount added to the attribute base value;\n" +
                t + "7 key 'Operation'; type: 'Integer'; format: '0'<>'2'; default: 0; des - 'Can be excluded' Modifier operation. Options: 0=ADDITION (adds Amount to base), 1=MULTIPLY_BASE (adds Amount * base to base), 2=MULTIPLY_TOTAL (multiplies total by 1 + Amount);";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleParticles() {
        if (exampleParticles == null) {
            exampleParticles = new CompoundTag();
            ListTag listItems = new ListTag();
            listItems.add(getExampleParticle());
            listItems.add(getExampleOBJParticle());
            exampleParticles.put("Particles", listItems);
        }
        return exampleParticles;
    }

    public static CompoundTag getExampleParticle() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "PARTICLE_EXAMPLE");
        compound.putBoolean("OverrideLimiter", false);

        compound.putInt("ArgumentCount", 0);
        compound.putInt("MaxAge", 60);
        compound.putFloat("Gravity", 0.25f);
        compound.putFloat("Scale", 1.5f);
        ListTag motion = new ListTag();
        motion.add(DoubleTag.valueOf(0.2d));
        motion.add(DoubleTag.valueOf(0.1d));
        motion.add(DoubleTag.valueOf(0.2d));
        compound.put("StartMotion", motion);
        compound.putBoolean("IsRandomMotion", true);
        compound.putBoolean("NotMotionY", true);

        String sb = "Tags for creating a custom particle:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- format of tag values must be respected;\n" +
                t + "- Particles are loaded from 'custom_particles.js' file, from 'Particles' list;\n" +
                t + "1 key 'RegistryName'; type: 'String'; format: '\"value\"'; des - 'Required' Specified name for particle registration; example: 'PARTICLE_EXAMPLE';\n" +
                t + "2 key 'OverrideLimiter'; type: 'Boolean'; format: false='0b', true='1b'; default: '1b' (true); des - 'Can be excluded' If true, particle ignores the vanilla spawn limit;\n" +
                t + "3 key 'CreateAllFiles'; type: 'Boolean'; format: false='0b', true='1b'; default: '0b' (false); des - 'Can be excluded' If true, auto-generates all required resource files for this particle; removed after processing;\n" +
                t + "4 key 'Texture'; type: 'String'; format: '\"value\"'; default: 'not used'; des - 'Can be excluded' Texture name for the particle sprite; file: assets/customnpcs/textures/particle/{name}.png;\n" +
                t + "- Particle JSON: assets/customnpcs/particles/{name}.json (defines texture frames for animation);\n" +
                t + "- Uses MutableSpriteSet for animated texture cycling;";
        compound.putString("-Description", sb);
        return compound;
    }

    public static CompoundTag getExampleOBJParticle() {
        CompoundTag compound = new CompoundTag();
        compound.putString("RegistryName", "PARTICLE_OBJ_EXAMPLE");
        compound.putBoolean("ShouldIgnoreRange", false);
        compound.putInt("MaxAge", 60);
        compound.putFloat("Gravity", 1.0f / 3.0f);
        compound.putFloat("Scale", 1.0f);
        compound.putString("OBJModel", "ring");

        String sb = "Tags for creating a custom OBJ particle:\n" +
                t + "- key names must match exactly (even the case of the characters);\n" +
                t + "- Keys: 'RegistryName', 'OverrideLimiter', 'CreateAllFiles' - see description of particle 'particleexample';\n" +
                t + "4 key 'OBJData'; type: 'CompoundTag'; format: '{}'; des - 'Required' OBJ model data for 3D particle rendering:\n" +
                t + " 4.01 key 'Model'; type: 'String'; format: '\"value\"'; des - 'Required if specified' OBJ model file path; example: 'customnpcs:models/particle/my_particle.obj';\n" +
                t + " 4.02 key 'Texture'; type: 'String'; format: '\"value\"'; des - 'Required if specified' Texture file path for the OBJ model; example: 'customnpcs:textures/particle/my_particle.png';\n" +
                t + "- OBJ particles render as 3D models instead of billboard sprites;\n" +
                t + "- Supports the same animation and lifecycle as standard particles;";
        compound.putString("-Description", sb);
        return compound;
    }

}
