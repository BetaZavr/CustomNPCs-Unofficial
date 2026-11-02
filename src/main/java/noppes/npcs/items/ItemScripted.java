package noppes.npcs.items;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomTabs;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;

import javax.annotation.Nonnull;

public class ItemScripted extends Item {

	public static Map<Integer, String> Resources = new HashMap<>();

	public static ItemScriptedWrapper GetWrapper(ItemStack stack) {
		return (ItemScriptedWrapper) Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stack);
	}

	public ItemScripted() {
		setRegistryName(CustomNpcs.MODID, "scripted_item");
		setUnlocalizedName("scripted_item");
		maxStackSize = 1;
		setCreativeTab(CustomTabs.TOOLS);
		setHasSubtypes(true);
	}

	@Override
	public double getDurabilityForDisplay(@Nonnull ItemStack stack) {
		IItemStack iStack = Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stack);
		if (iStack instanceof ItemScriptedWrapper) {
			return 1.0 - ((ItemScriptedWrapper) iStack).durabilityValue;
		}
		return super.getDurabilityForDisplay(stack);
	}

	@Override
	public int getItemStackLimit(@Nonnull ItemStack stack) {
		IItemStack iStack = Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stack);
		if (iStack instanceof ItemScriptedWrapper) {
			return iStack.getMaxStackSize();
		}
		return super.getItemStackLimit(stack);
	}

	@Override
	public int getRGBDurabilityForDisplay(@Nonnull ItemStack stack) {
		IItemStack iStack = Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stack);
		if (!(iStack instanceof ItemScriptedWrapper)) {
			return super.getRGBDurabilityForDisplay(stack);
		}
		int color = ((ItemScriptedWrapper) iStack).durabilityColor;
		if (color >= 0) {
			return color;
		}
		return MathHelper.hsvToRGB((float) (Math.max(0.0f, (1.0 - getDurabilityForDisplay(stack))) / 3.0f), 1.0f,  1.0f);
	}

	@Override
	public boolean hitEntity(@Nonnull ItemStack stack, @Nonnull EntityLivingBase target, @Nonnull EntityLivingBase attacker) {
		return true;
	}

	@Override
	public boolean showDurabilityBar(@Nonnull ItemStack stack) {
		IItemStack iStack = Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stack);
		if (iStack instanceof ItemScriptedWrapper) {
			return ((ItemScriptedWrapper) iStack).durabilityShow;
		}
		return super.showDurabilityBar(stack);
	}

}
