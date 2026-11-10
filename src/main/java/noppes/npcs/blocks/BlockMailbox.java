package noppes.npcs.blocks;

import java.util.ArrayList;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import noppes.npcs.CustomTabs;
import noppes.npcs.blocks.tiles.TileMailbox;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiOpen;

import javax.annotation.Nonnull;

public class BlockMailbox extends BlockInterface {

	protected static final AxisAlignedBB AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.375D, 1.0D);
	public static PropertyInteger ROTATION = PropertyInteger.create("rotation", 0, 3);
	public static PropertyInteger TYPE = PropertyInteger.create("type", 0, 2);

	public BlockMailbox() {
		super(Material.IRON);
		setName("npcmailbox");
		setSoundType(SoundType.METAL);
		setHardness(5.0f);
		setResistance(10.0f);
		setCreativeTab(CustomTabs.TOOLS);
	}

	@Override
	protected @Nonnull BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockMailbox.TYPE, BlockMailbox.ROTATION);
	}

	@Override
	public TileEntity createNewTileEntity(@Nonnull World world, int meta) { return new TileMailbox(); }

	@Override
	public int damageDropped(@Nonnull IBlockState state) {
		return state.getValue(BlockMailbox.TYPE);
	}

	@Override
	@SuppressWarnings("deprecation")
	public @Nonnull ArrayList<ItemStack> getDrops(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull IBlockState state, int fortune) {
		ArrayList<ItemStack> ret = new ArrayList<>();
		int damage = state.getValue(BlockMailbox.TYPE);
		ret.add(new ItemStack(this, 1, damage));
		return ret;
	}

	@Override
	public int getMetaFromState(@Nonnull IBlockState state) {
		return state.getValue(BlockMailbox.ROTATION) | state.getValue(BlockMailbox.TYPE) << 2;
	}

	@Override
	@SuppressWarnings("deprecation")
	public @Nonnull IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(BlockMailbox.TYPE, ((meta >> 2) % 3))
				.withProperty(BlockMailbox.ROTATION, ((meta | 0x4) % 4));
	}

	@Override
	public void getSubBlocks(@Nonnull CreativeTabs par2CreativeTabs, @Nonnull NonNullList<ItemStack> par3List) {
		par3List.add(new ItemStack(this, 1, 0));
		par3List.add(new ItemStack(this, 1, 1));
		par3List.add(new ItemStack(this, 1, 2));
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isFullCube(@Nonnull IBlockState state) {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isOpaqueCube(@Nonnull IBlockState state) {
		return false;
	}

	@Override
	public boolean onBlockActivated(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer player, @Nonnull EnumHand hand, @Nonnull EnumFacing side, float hitX, float hitY, float hitZ) {
		if (!world.isRemote) {
			Packets.send((EntityPlayerMP) player, new PacketGuiOpen(EnumGuiType.PlayerMailbox, pos));
		}
		return true;
	}

	@Override
	public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase entity, @Nonnull ItemStack stack) {
		int l = MathHelper.floor(entity.rotationYaw * 4.0f / 360.0f + 0.5) & 0x3;
		int type = stack.getItemDamage();
		world.setBlockState(pos, state.withProperty(BlockMailbox.TYPE, type).withProperty(BlockMailbox.ROTATION, (l % 4)), 2);
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileMailbox) { ((TileMailbox) tile).setModel(type); }
	}

	@SuppressWarnings("deprecation")
	@Override
	public @Nonnull AxisAlignedBB getBoundingBox(@Nonnull IBlockState state, @Nonnull IBlockAccess source, @Nonnull BlockPos pos) {
		return AABB;
	}

}
