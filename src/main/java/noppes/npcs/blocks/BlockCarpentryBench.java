package noppes.npcs.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import noppes.npcs.CustomTabs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.blocks.tiles.TileBlockAnvil;
import noppes.npcs.constants.EnumGuiType;

import javax.annotation.Nonnull;

public class BlockCarpentryBench extends BlockInterface {

	public static PropertyInteger ROTATION = PropertyInteger.create("rotation", 0, 3);

	public BlockCarpentryBench() {
		super(Material.WOOD);
		setName("npccarpentybench");
		setHardness(5.0f);
		setResistance(10.0f);
		setCreativeTab(CustomTabs.TOOLS);
		setSoundType(SoundType.WOOD);
	}

	@Override
	protected @Nonnull BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockCarpentryBench.ROTATION);
	}

	@Override
	public TileEntity createNewTileEntity(@Nonnull World world, int meta) { return new TileBlockAnvil(); }

	@Override
	public int getMetaFromState(@Nonnull IBlockState state) { return state.getValue(BlockCarpentryBench.ROTATION); }

	@Override
	@SuppressWarnings("deprecation")
	public @Nonnull IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(BlockCarpentryBench.ROTATION, (meta % 4));
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isFullCube(@Nonnull IBlockState state) { return false; }

	@Override
	@SuppressWarnings("deprecation")
	public boolean isOpaqueCube(@Nonnull IBlockState state) { return false; }

	@Override
	public boolean onBlockActivated(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer player, @Nonnull EnumHand hand, @Nonnull EnumFacing side, float hitX, float hitY, float hitZ) {
		if (!world.isRemote && player instanceof EntityPlayerMP) {
			NoppesUtilServer.openContainerGui((EntityPlayerMP) player, EnumGuiType.PlayerAnvil, (buffer) -> buffer.writeBlockPos(pos));
		}
		return true;
	}

	@Override
	public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase entity, @Nonnull ItemStack stack) {
		int var6 = MathHelper.floor(entity.rotationYaw / 90.0f + 0.5) & 0x3;
		world.setBlockState(pos, state.withProperty(BlockCarpentryBench.ROTATION, var6), 2);
	}

}
