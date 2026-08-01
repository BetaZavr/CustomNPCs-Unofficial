package noppes.npcs.controllers.data;

import java.util.*;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.util.ValueUtil;

public class DealMarkup {

	protected MarkupData markupData;
	public ItemStack main = ItemStack.EMPTY;
	public int count = 0;
	public final Map<ItemStack, Integer> baseItems = new LinkedHashMap<>();
	public Map<ItemStack, Integer> buyItems = new LinkedHashMap<>();
	public Map<ItemStack, Integer> sellItems = new LinkedHashMap<>();
	public final Map<ItemStack, Boolean> baseHasPlayerItems = new LinkedHashMap<>();
	public Map<ItemStack, Boolean> buyHasPlayerItems = new LinkedHashMap<>();
	public Map<ItemStack, Boolean> sellHasPlayerItems = new LinkedHashMap<>();
	public long baseMoney = 0L;
	public long buyMoney = 0L;
	public long sellMoney = 0L;
	public long baseDonat = 0L;
	public long buyDonat = 0L;
	public boolean baseOneOfEach = true;
	public boolean buyOneOfEach = true;
	public boolean sellOneOfEach = true;
	public boolean ignoreNBT = false;
	public boolean ignoreDamage = false;
	public Deal deal = null;

	public void check(NonNullList<ItemStack> inventory) {
		baseHasPlayerItems.clear();
		buyHasPlayerItems.clear();
		sellHasPlayerItems.clear();
		for (ItemStack stack : baseItems.keySet()) {
			int count = 0;
			for (ItemStack s : inventory) {
				if (NoppesUtilServer.isItemStackNull(s)) { continue; }
				if (NoppesUtilPlayer.compareItems(stack, s, ignoreDamage, ignoreNBT)) { count += s.getCount(); }
			}
			baseHasPlayerItems.put(stack, count >= baseItems.get(stack));
			buyHasPlayerItems.put(stack, buyItems.containsKey(stack) && count >= buyItems.get(stack));
			sellHasPlayerItems.put(stack, sellItems.containsKey(stack) && count >= sellItems.get(stack));
		}
		buyOneOfEach = true;
		for (int count : buyItems.values()) {
			if (count > 1) {
				buyOneOfEach = false;
				break;
			}
		}
		sellOneOfEach = true;
		for (int count : sellItems.values()) {
			if (count > 1) {
				sellOneOfEach = false;
				break;
			}
		}
	}

	public void reset(boolean isBuy, float coff, float countIn) {
		coff = ValueUtil.correctFloat(coff, 0.0f, 6.0f);
		if (!baseItems.isEmpty()) {
			for (ItemStack stack : baseItems.keySet()) {
				int count = (int) ((float) baseItems.get(stack) * coff);
				if (count <= 0) { count = 1; }
				if (isBuy) { buyItems.put(stack, (int) (count * countIn)); }
				else if (!deal.isCase()) { sellItems.put(stack, (int) (count * countIn)); }
			}
		}
		for (ItemStack stack : buyItems.keySet()) {
			if (buyItems.get(stack) > 1) {
				if (isBuy) { buyOneOfEach = false; }
				else { sellOneOfEach = false; }
				break;
			}
		}
		if (isBuy) {
			buyMoney = (long) ((float) baseMoney * countIn * coff);
			buyDonat = (long) ((float) baseDonat * countIn * coff);
		}
		else { sellMoney = (long) ((float) baseMoney * countIn * coff); }
	}

	public void set(Deal dealIn) {
		deal = dealIn;
		main = dealIn.getProduct().getMCItemStack().copy();
		count = dealIn.getProduct().getStackSize();
		ignoreNBT = dealIn.getIgnoreNBT();
		ignoreDamage = dealIn.getIgnoreDamage();
		baseMoney = dealIn.getMoney();
		baseDonat = dealIn.getDonat();
		set(dealIn.getMCInventoryCurrency());
	}

	public void set(IInventory iContainer) {
		baseItems.clear();
		if (iContainer == null || !iContainer.isEmpty()) {
			Map<ItemStack, Integer> map = new HashMap<>();
			if (iContainer != null) {
				for (int slot = 0; slot < iContainer.getSizeInventory(); slot++) {
					ItemStack stack = iContainer.getStackInSlot(slot);
					if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
					boolean has = false;
					for (ItemStack s : map.keySet()) {
						if (NoppesUtilPlayer.compareItems(stack, s, ignoreDamage, ignoreNBT)) {
							has = true;
							map.put(s, map.get(s) + stack.getCount());
							baseOneOfEach = false;
							break;
						}
					}
					if (!has) {
						map.put(stack, stack.getCount());
						if (stack.getCount() > 1) { baseOneOfEach = false; }
					}
				}
			}
			List<Map.Entry<ItemStack, Integer>> list = new ArrayList<>(map.entrySet());
			list.sort((st_0, st_1) -> st_1.getValue().compareTo(st_0.getValue()));
			for (Map.Entry<ItemStack, Integer> entry : list) { baseItems.put(entry.getKey(), entry.getValue()); }
		}
	}

	public void set(MarkupData md, int countIn) {
		if (md == null) { return; }
		markupData = md;
		countIn = ValueUtil.correctInt(countIn, 1, 64);
		count = deal.getProduct().getStackSize() * countIn;
		buyItems.clear();
		sellItems.clear();
		buyMoney = baseMoney;
		sellMoney = baseMoney;
		buyDonat = baseDonat;
		reset(true, (100.0f + md.buy * 100.0f) / 100.0f, countIn);
		reset(false, (100.0f - md.sell * 100.0f) / 100.0f, countIn);
	}

}
