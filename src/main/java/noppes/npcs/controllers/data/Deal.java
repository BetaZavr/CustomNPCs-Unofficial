package noppes.npcs.controllers.data;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.ICustomDrop;
import noppes.npcs.api.handler.data.IAvailability;
import noppes.npcs.api.handler.data.IDeal;
import noppes.npcs.api.handler.data.IDropSetData;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.containers.NpcMiscInventory;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketDealUpdate;
import noppes.npcs.util.ValueUtil;

import java.util.*;

public class Deal implements IDeal, IDropSetData {

	public final static ResourceLocation defaultCaseOBJ = new ResourceLocation(CustomNpcs.MODID, "models/util/chest.obj");
	public static ResourceLocation defaultCaseTexture = new ResourceLocation("minecraft", "entity/chest/christmas");

	public Availability availability = new Availability();
	protected float chance = 1.0f; // 0.0 <-> 1.0
	protected int[] count = new int[] { 0, 0 };
	protected int id = -1;
	protected boolean ignoreDamage = false;
	protected boolean ignoreNBT = false;
	protected final NpcMiscInventory inventoryCurrency = new NpcMiscInventory(9);
	protected final NpcMiscInventory inventoryProduct = new NpcMiscInventory(1);
	protected int money = 0;
	protected int donat = 0;
	protected int type = 2; // 0: only bay; 1: only sell; 2: any
	protected int amount = 1;
	protected int rarityColor = 0;
	// Case
	protected final Map<Integer, DropSet> caseItems = new TreeMap<>();
	protected ResourceLocation caseObjModel = null;
	protected ResourceLocation caseSound = null;
	protected ResourceLocation caseTexture = null;
	protected boolean isCase = false;
	protected boolean caseInShow = false;
	protected int caseCount = 1;
	protected String caseName = "gui.default";
	protected String caseCommand = "";

	public boolean update;

	public Deal() { }

	public Deal(int idIn) { id = idIn; }

	public Deal copy() {
		Deal deal = new Deal(id);
		deal.availability = availability;
		deal.ignoreDamage = ignoreDamage;
		deal.ignoreNBT = ignoreNBT;
		for (int i = 0; i < 9; i++) { deal.inventoryCurrency.setInventorySlotContents(i, inventoryCurrency.getStackInSlot(i).copy()); }
		deal.inventoryProduct.setInventorySlotContents(0, inventoryProduct.getStackInSlot(0).copy());
		deal.type = type;
		deal.setRarityColor(rarityColor);
		deal.money = money;
		deal.donat = donat;
		deal.count[0] = count[0];
		deal.count[1] = count[1];
		deal.chance = chance;
		deal.amount = amount;

		deal.isCase = isCase;
		deal.caseInShow = caseInShow;
		deal.caseCount = caseCount;
		deal.caseName = caseName;
		deal.caseCommand = caseCommand;
		deal.caseObjModel = caseObjModel;
		deal.caseSound = caseSound;
		deal.caseTexture = caseTexture;
		Map<Integer, DropSet> items = new TreeMap<>(caseItems);
		deal.caseItems.clear();
		deal.caseItems.putAll(items);
		return deal;
	}

	@Override
	public int getAmount() { return amount; }

	@Override
	public IAvailability getAvailability() { return availability; }

	@Override
	public int getChance() { return (int) (chance * 100.0f); }

	@Override
	public IContainer getCurrency() { return Objects.requireNonNull(NpcAPI.Instance()).getIContainer(inventoryCurrency); }

	@Override
	public int getId() { return id; }

	@Override
	public boolean getIgnoreDamage() { return ignoreDamage; }

	@Override
	public boolean getIgnoreNBT() { return ignoreNBT; }

	@Override
	public int getMaxCount() { return count[1]; }

	@Override
	public IInventory getMCInventoryCurrency() { return inventoryCurrency; }

	@Override
	public IInventory getMCInventoryProduct() { return inventoryProduct; }

	@Override
	public int getMinCount() { return count[0]; }

	@Override
	public int getMoney() { return money; }

	@Override
	public int getDonat() { return donat; }

	@Override
	public String getName() {
		Component name = Component.empty();
		if (isCase) {
			name.append(Component.empty()
					.append(Component.translatable(caseName)).withStyle(count[1] != 0 && amount == 0 ? TextFormatting.DARK_RED : TextFormatting.RESET));
		}
		else {
			ItemStack stack = inventoryProduct.getStackInSlot(0);
			if (count[1] != 0 && amount == 0) {
				name.append(Component.empty().append(stack.getDisplayName())
						.append(Component.literal(" x" + stack.getCount()))
						.withStyle(TextFormatting.DARK_RED));
			} else {
				name.append(Component.empty()
						.append(stack.getDisplayName())
						.append(Component.literal(" x").withStyle(TextFormatting.RESET))
						.append(Component.literal("" + stack.getCount()).withStyle(TextFormatting.GOLD)));
			}
		}
		return name.getFormattedText();
	}

	@Override
	public IItemStack getProduct() { return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(inventoryProduct.getStackInSlot(0)); }

	public Component getSettingName() {
		ItemStack stack = inventoryProduct.getStackInSlot(0);
		Component keyName = Component.empty()
				.append(Component.literal("ID:" + id + " ").withStyle(TextFormatting.GRAY));
		if (isCase) {
			Component stackName = Component.empty()
					.append(Component.translatable(caseName));
			keyName.append(stackName.withStyle(inventoryCurrency.isEmpty() && money == 0 && donat == 0 ? TextFormatting.DARK_RED : TextFormatting.RESET));
		}
		else {
			if (stack.isEmpty()) { keyName.append(Component.translatable("type.empty").withStyle(TextFormatting.DARK_RED)); }
			else {
				Component stackName = Component.empty().append(stack.getDisplayName()).withStyle(TextFormatting.RESET);
				if (!stack.isEmpty()) {
					stackName.append(Component.literal(" x").withStyle(TextFormatting.GRAY))
							.append(Component.literal("" + stack.getCount()).withStyle(TextFormatting.GOLD));
				}
				keyName.append(stackName.withStyle(inventoryCurrency.isEmpty() && money == 0 && donat == 0 ? TextFormatting.DARK_RED : TextFormatting.RESET));
			}
		}
		return keyName;
	}

	@Override
	public int getType() { return isCase ? 0 : type; }

	public boolean isValid() {
		if (isCase) {
			return !caseItems.isEmpty() && !caseItems.get(0).getItem().isEmpty() && (money > 0 || donat > 0 || !inventoryCurrency.isEmpty());
		}
		else { return !inventoryProduct.getStackInSlot(0).isEmpty() && (money > 0 || donat > 0 || !inventoryCurrency.isEmpty()); }
	}

	public void loadData(NBTTagCompound compound) {
		load(compound);
		amount = compound.getInteger("Amount");
		id = compound.getInteger("DealID");
	}

	public void load(NBTTagCompound compound) {
		availability.load(compound.getCompoundTag("Availability"));
		ignoreDamage = compound.getBoolean("IgnoreDamage");
		ignoreNBT = compound.getBoolean("IgnoreNBT");
		inventoryCurrency.load(compound.getCompoundTag("Currency"));
		inventoryProduct.load(compound.getCompoundTag("Product"));
		type = compound.getInteger("Type");
		money = compound.getInteger("Money");
		donat = compound.getInteger("Donat");
		count = compound.getIntArray("Count");
		chance = compound.getFloat("Chance");
		id = compound.getInteger("DealID");
		setRarityColor(compound.getInteger("RarityColor"));

		isCase = compound.getBoolean("IsCase");
		caseInShow = compound.getBoolean("CaseInShow");
		caseCount = compound.getInteger("CaseCount");
		caseName = compound.getString("CaseName");
		caseCommand = compound.getString("CaseCommand");
		caseObjModel = null;
		if (compound.hasKey("CaseObjModel", 8)) { caseObjModel = new ResourceLocation(compound.getString("CaseObjModel")); }
		caseSound = null;
		if (compound.hasKey("CaseSound", 8)) { caseSound = new ResourceLocation(compound.getString("CaseSound")); }
		if (compound.hasKey("CaseTexture", 8)) { caseTexture = new ResourceLocation(compound.getString("CaseTexture")); }

		caseItems.clear();
		for (int i = 0; i < compound.getTagList("NpcInv", 10).tagCount(); i++) {
			DropSet ds = new DropSet(this);
			ds.load(compound.getTagList("NpcInv", 10).getCompoundTagAt(i));
			ds.pos = i;
			caseItems.put(i, ds);
		}
	}

	@Override
	public void set(IItemStack product, IItemStack[] currencies) {
		if (product == null) { product = ItemStackWrapper.AIR; }
		ItemStack[] cs = new ItemStack[currencies == null ? 0 : currencies.length];
		if (currencies != null) {
			int i = 0;
			for (IItemStack stack : currencies) {
				cs[i] = stack.getMCItemStack();
				i++;
			}
		}
		set(product.getMCItemStack(), cs);
	}

	public void set(ItemStack product, ItemStack[] currency) {
		if (product == null) { product = ItemStack.EMPTY; }
		inventoryProduct.setInventorySlotContents(0, product);
		if (count[1] != 0 && count[1] >= count[0]) {
			amount = 0;
			if (chance >= (float) Math.random()) { amount = count[0] + (int) (Math.random() * (count[1] - count[0])); }
		}
		else { amount = 1; }
		inventoryCurrency.clear();
		if (currency != null) {
			for (int i = 0, j = 0; i < currency.length; i++) {
				if (currency[i] == null || currency[i].isEmpty()) { continue; }
				inventoryCurrency.setInventorySlotContents(j, currency[i]);
				j++;
			}
		}
	}

	@Override
	public void setAmount(int amountIn) {
		amount = ValueUtil.correctInt(amountIn, 0, Integer.MAX_VALUE);
		update = true;
	}

	public void setChance(float chanceIn) {
		chance = ValueUtil.correctFloat(chanceIn, 0f, 1.0f);
		update = true;
	}

	@Override
	public void setChance(int chance) { setChance(((float) chance) / 100.0f); }

	@Override
	public void setCount(int min, int max) {
		if (min < 0) { min *= -1; }
		if (max < 0) { max *= -1; }
		if (max < min) {
			int m = min;
			min = max;
			max = m;
		}
		count[0] = min;
		count[1] = max;
		update = true;
	}

	@Override
	public void setIgnoreDamage(boolean bo) {
		if (bo == ignoreDamage) { return; }
		ignoreDamage = bo;
		update = true;
	}

	@Override
	public void setIgnoreNBT(boolean bo) {
		if (bo == ignoreNBT) { return; }
		ignoreNBT = bo;
		update = true;
	}

	@Override
	public void setMoney(int moneyIn) {
		money = ValueUtil.correctInt(moneyIn, 0, Integer.MAX_VALUE);
		update = true;
	}

	@Override
	public void setDonat(int moneyIn) {
		donat = ValueUtil.correctInt(moneyIn, 0, Integer.MAX_VALUE);
		update = true;
	}

	@Override
	public void setProduct(IItemStack product) {
		if (product == null) { product = ItemStackWrapper.AIR; }
		inventoryProduct.setInventorySlotContents(0, product.getMCItemStack());
	}

	@Override
	public void setType(int typeIn) {
		if (typeIn < 0) { typeIn *= -1; }
		type = typeIn % 3;
		update = true;
	}

	@Override
	public int getRarityColor() { return rarityColor; }

	@Override
	public void setRarityColor(int color) {
		color = color & 0x00FFFFFF;
		if (rarityColor != color) {
			rarityColor = color;
			update = true;
		}
	}

	@Override
	public boolean isCase() { return isCase; }

	@Override
	public void setIsCase(boolean isCaseIn) {
		isCase = isCaseIn;
		update = true;
	}

	@Override
	public int getCaseCount() { return caseCount; }

	@Override
	public void setCaseCount(int count) {
		if (caseItems.isEmpty()) { return; }
		count = ValueUtil.correctInt(count, 1, caseItems.size() - 1);
		if (caseCount != count) {
			caseCount = count;
			update = true;
		}
	}

	@Override
	public String getCaseName() { return caseName; }

	@Override
	public void setCaseName(String newName) {
		if (newName == null || newName.isEmpty()) { newName = "gui.default"; }
		if (!caseName.equals(newName)) {
			caseName = newName;
			update = true;
		}
	}

	@Override
	public String getCaseCommand() { return caseCommand; }

	@Override
	public void setCaseCommand(String command) {
		if (command == null) { command = ""; }
		if (!caseCommand.equals(command)) {
			caseCommand = command;
			update = true;
		}
	}

	@Override
	public ResourceLocation getCaseObjModel() { return caseObjModel != null ? caseObjModel : Deal.defaultCaseOBJ; }

	@Override
	public void setCaseObjModel(ResourceLocation objModel) {
		if ((caseObjModel != null && !caseObjModel.equals(objModel)) || (caseObjModel == null && objModel != null)) {
			caseObjModel = objModel;
			update = true;
		}
	}

	@Override
	public ResourceLocation getCaseSound() { return caseSound; }

	@Override
	public void setCaseSound(ResourceLocation sound) {
		if ((caseSound != null && !caseSound.equals(sound)) || (caseSound == null && sound != null)) {
			caseObjModel = sound;
			update = true;
		}
	}

	@Override
	public ResourceLocation getCaseTexture() { return caseTexture != null ? caseTexture : Deal.defaultCaseTexture; }

	@Override
	public void setCaseTexture(ResourceLocation texture) {
		if (texture != null && texture.getResourcePath().toLowerCase().endsWith(".png")) {
			texture = new ResourceLocation(texture.getResourceDomain(), texture.getResourcePath().substring(0, texture.getResourcePath().length() - 4));
		}
		if ((caseTexture != null && !caseTexture.equals(texture)) || (caseTexture == null && texture != null)) {
			caseTexture = texture;
			update = true;
		}
	}

	@Override
	public boolean showInCase() { return caseInShow; }

	@Override
	public void setShowInCase(boolean show) {
		if (caseInShow != show) {
			caseInShow = show;
			update = true;
		}
	}

	@Override
	public DropSet addCaseItem(IItemStack item, double chance) {
		return addCaseItem(item.getMCItemStack(), chance);
	}

	public DropSet addCaseItem(ItemStack item, double chance) {
		chance = ValueUtil.correctDouble(chance, 0.0001d, 100.0d);
		DropSet ds = new DropSet(this);
		ds.setInventorySlotContents(0, item);
		ds.chance = chance;
		ds.pos = caseItems.size();
		caseItems.put(ds.pos, ds);
		update = true;
		return ds;
	}

	@Override
	public ICustomDrop getCase(int slot) {
		if (slot < 0 || slot >= caseItems.size()) {
			throw new CustomNPCsException("Bad slot number: " + slot + " in " + caseItems.size() + " maximum");
		}
		return caseItems.get(slot);
	}

	@Override
	public IItemStack getCaseItem(int slot) {
		if (slot < 0 || slot >= caseItems.size()) {
			throw new CustomNPCsException("Bad slot number: " + slot + " in " + caseItems.size() + " maximum");
		}
		DropSet g = caseItems.get(slot);
		return g.getItem();
	}

	@Override
	public DropSet[] getCaseItems() {
		DropSet[] dss = new DropSet[caseItems.size()];
		int i = 0;
		for (DropSet ds : caseItems.values()) {
			dss[i] = ds;
			i++;
		}
		return dss;
	}

	public void setCaseItems(Map<Integer, DropSet> items) {
		caseItems.clear();
		if (items != null) { caseItems.putAll(items); }
	}

	@Override
	public boolean removeCaseItem(ICustomDrop drop) {
		Map<Integer, DropSet> newDrop = new TreeMap<>();
		boolean del = false;
		int j = 0;
		for (int slot : caseItems.keySet()) {
			if (caseItems.get(slot) == drop) {
				del = true;
				continue;
			}
			newDrop.put(j, caseItems.get(slot));
			newDrop.get(j).pos = j;
			j++;
		}
		if (del) {
			caseItems.clear();
			caseItems.putAll(newDrop);
			update = true;
		}
		return del;
	}

	@Override
	public boolean removeCaseItem(int slot) {
		if (caseItems.containsKey(slot)) {
			caseItems.remove(slot);
			Map<Integer, DropSet> newDrop = new TreeMap<>();
			int j = 0;
			for (int s : caseItems.keySet()) {
				if (s == slot) {
					continue;
				}
				newDrop.put(j, caseItems.get(s));
				newDrop.get(j).pos = j;
				j++;
			}
			caseItems.clear();
			caseItems.putAll(newDrop);
			return update = true;
		}
		return false;
	}

	public void update() {
		if (update) {
			update = false;
			MarcetController mData = MarcetController.getInstance();
			NBTTagCompound nbt = saveData();
			for (Marcet marcet : mData.markets.values()) {
				if (marcet.getSection(id) == -1) { continue;}
				for (EntityPlayer listener : marcet.listeners) {
					if (listener instanceof EntityPlayerMP) {
						Packets.send((EntityPlayerMP) listener, new PacketDealUpdate(marcet.getId(), nbt));
					}
				}
			}
		}
	}

	@Override
	public void updateNew() {
		if (chance >= (float) Math.random()) {
			if (count[1] != 0 && count[1] >= count[0]) { amount = count[0] + (int) (Math.random() * (count[1] - count[0])); }
			else { amount = 1; }
		}
		else { amount = 0; }
	}

	public NBTTagCompound saveData() {
		NBTTagCompound compound = save();
		compound.setInteger("Amount", amount);
		return compound;
	}

	public NBTTagCompound save() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setTag("Availability", availability.save(new NBTTagCompound()));
		compound.setBoolean("IgnoreDamage", ignoreDamage);
		compound.setBoolean("IgnoreNBT", ignoreNBT);
		compound.setTag("Currency", inventoryCurrency.save());
		compound.setTag("Product", inventoryProduct.save());
		compound.setInteger("Type", type);
		compound.setIntArray("Count", count);
		compound.setFloat("Chance", chance);
		compound.setInteger("DealID", id);
		compound.setInteger("Money", money);
		compound.setInteger("Donat", donat);
		compound.setInteger("RarityColor", rarityColor);

		compound.setBoolean("IsCase", isCase);
		compound.setBoolean("CaseInShow", caseInShow);
		compound.setInteger("CaseCount", caseCount);
		compound.setString("CaseName", caseName);
		compound.setString("CaseCommand", caseCommand);
		if (caseObjModel != null) { compound.setString("CaseObjModel", caseObjModel.toString()); }
		if (caseSound != null) { compound.setString("CaseSound", caseSound.toString()); }
		if (caseTexture != null) { compound.setString("CaseTexture", caseTexture.toString()); }

		NBTTagList dropList = new NBTTagList();
		int s = 0;
		for (int slot : caseItems.keySet()) {
			if (caseItems.get(slot) == null) { continue; }
			if (caseItems.get(slot).pos != s) { caseItems.get(slot).pos = s; }
			dropList.appendTag(caseItems.get(slot).save());
			s++;
		}
		compound.setTag("NpcInv", dropList);

		return compound;
	}

	@SideOnly(Side.CLIENT)
	public void putHoverCaseItems(List<Component> hovers, ITooltipFlag.TooltipFlags type) {
		for (int pos: caseItems.keySet()) {
			DropSet dropSet = caseItems.get(pos);
			Component line = Component.empty()
					.append(Component.literal(type == ITooltipFlag.TooltipFlags.ADVANCED ? pos + ": \"" : "- \"").withStyle(TextFormatting.GRAY))
					.append(Component.literal(dropSet.item.getDisplayName()).withStyle(TextFormatting.RESET))
					.append(Component.literal("\" x").withStyle(TextFormatting.GRAY));
			if (dropSet.amount[0] == dropSet.amount[1]) { line.append(Component.literal("" + dropSet.amount[0]).withStyle(TextFormatting.GOLD)); }
			else {
				line.append(Component.literal("[").withStyle(TextFormatting.GRAY))
						.append(Component.literal("" + dropSet.amount[0]).withStyle(TextFormatting.GOLD))
						.append(Component.literal("...").withStyle(TextFormatting.GRAY))
						.append(Component.literal("" + dropSet.amount[1]).withStyle(TextFormatting.GOLD))
						.append(Component.literal("]").withStyle(TextFormatting.GRAY));
			}
			if (type == ITooltipFlag.TooltipFlags.ADVANCED) {
				double ch = Math.round(dropSet.chance * 10.0d) / 10.d;
				String chance = String.valueOf(ch).replace(".", ",");
				if (ch == (int) ch) { chance = String.valueOf((int) ch); }
				chance += "%";
				line.append(Component.literal("; ").withStyle(TextFormatting.GRAY))
						.append(Component.translatable("drop.chance").append(": " + chance).withStyle(TextFormatting.GRAY));
				if (!dropSet.enchants.isEmpty()) {
					line.append(Component.literal(" |").withStyle(TextFormatting.GRAY))
							.append(Component.literal("E").withStyle(TextFormatting.AQUA));
				}
				if (!dropSet.attributes.isEmpty()) {
					line.append(Component.literal(" |").withStyle(TextFormatting.GRAY))
							.append(Component.literal("A").withStyle(TextFormatting.GREEN));
				}
				if (!dropSet.tags.isEmpty()) {
					line.append(Component.literal(" |").withStyle(TextFormatting.GRAY))
							.append(Component.literal("T").withStyle(TextFormatting.RED));
				}
			}
			line.append(Component.literal(";").withStyle(TextFormatting.GRAY));
			hovers.add(line);
		}
	}

	public Collection<ItemStack> createCaseItems(double baseChance) {
		List<ItemStack> stacks = new ArrayList<>();
		if (caseItems.isEmpty()) { return stacks; }
		Map<ItemStack, Double> map = new LinkedHashMap<>();
		float rnd = (float) Math.random() * 100.0f;
		double max = -1.0d;
		DropSet maxDS = null;
		for (DropSet ds : new ArrayList<>(caseItems.values())) {
			if (rnd <= ds.chance) { map.put(ds.createMCLoot(baseChance), ds.chance); }
			if (max < ds.chance) {
				max = ds.chance;
				maxDS = ds;
			}
		}
		if (map.isEmpty() && maxDS != null) { map.put(maxDS.createMCLoot(baseChance), maxDS.chance); }
		List<Map.Entry<ItemStack, Double>> entries = new ArrayList<>(map.entrySet());
		entries.sort(Comparator.comparingDouble(Map.Entry::getValue));
		int i = 0;
		for (Map.Entry<ItemStack, Double> entry : entries) {
			stacks.add(entry.getKey());
			i++;
			if (i == caseCount) { break; }
		}
		return stacks;
	}

	@Override
	public int getNpcLevel() { return 1; }

	@Override
	public boolean removeDrop(DropSet dropSet) { return removeCaseItem(dropSet); }

}
