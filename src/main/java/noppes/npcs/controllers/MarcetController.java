package noppes.npcs.controllers;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.FMLCommonHandler;
import noppes.npcs.CustomNpcs;
import noppes.npcs.containers.NpcMiscInventory;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.*;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.handler.IMarcetHandler;
import noppes.npcs.controllers.data.Deal;
import noppes.npcs.controllers.data.DealMarkup;
import noppes.npcs.controllers.data.Marcet;
import noppes.npcs.controllers.data.MarcetSection;
import noppes.npcs.controllers.data.MarkupData;
import noppes.npcs.entity.data.DropSet;

public class MarcetController implements IMarcetHandler {

	private static MarcetController instance;
	public static MarcetController getInstance() {
		if (newInstance()) {
			MarcetController.instance = new MarcetController();
		}
		return MarcetController.instance;
	}
	public static boolean hasLocalServerData() {
		return FMLCommonHandler.instance().getMinecraftServerInstance() != null;
	}

	private static boolean newInstance() {
		if (MarcetController.instance == null) {
			return true;
		}
		File file = CustomNpcs.getWorldSaveDirectory();
		return file != null && !MarcetController.instance.filePath.equals(file.getAbsolutePath());
	}
	private String filePath;
    public final Map<Integer, Marcet> markets = new TreeMap<>();
	public final Map<Integer, Deal> deals = new TreeMap<>();

	public MarcetController() {
		MarcetController.instance = this;
		File file = CustomNpcs.getWorldSaveDirectory();
		if (file != null) {
			filePath = file.getAbsolutePath();
			load();
		}
	}

	@Override
	public Deal addDeal() {
		Deal deal = new Deal(getUnusedDealId());
		deals.put(deal.getId(), deal);
		return deal;
	}

	@Override
	public Marcet addMarcet() {
		Marcet marcet = new Marcet(getUnusedMarketId());
		markets.put(marcet.getId(), marcet);
		return markets.get(marcet.getId());
	}

	public DealMarkup getBuyData(Marcet marcet, Deal deal, int marcetLevel, int countIn) {
		DealMarkup dm = new DealMarkup();
		if (deal != null) { dm.set(deal); }
		if (marcet != null && !marcet.markup.isEmpty()) {
			MarkupData md = marcet.markup.get(marcetLevel);
			if (md == null) {
				for (Map.Entry<Integer, MarkupData> entry : marcet.markup.entrySet()) {
					if (md == null || entry.getKey() <= marcetLevel) { md = entry.getValue(); }
				}
			}
			dm.set(md, countIn);
		}
		return dm;
	}

	@Override
	public Deal getDeal(int dealId) { return deals.get(dealId); }

	@Override
	public int[] getDealIDs() {
		int[] arr = new int[deals.size()];
		int i = 0;
		for (Deal m : deals.values()) {
			arr[i] = m.getId();
			i++;
		}
		return arr;
	}

	@Override
	public Marcet getMarcet(int marcetId) {
		if (marcetId < 0 || !markets.containsKey(marcetId)) { return null; }
		return markets.get(marcetId);
	}

	@Override
	public Marcet getMarcet(String name) {
		for (Marcet m : markets.values()) {
			if (m.name.equals(name)) { return m; }
		}
		return null;
	}

	@Override
	public int[] getMarketIDs() {
		int[] arr = new int[markets.size()];
		int i = 0;
		for (Marcet m : markets.values()) {
			arr[i] = m.getId();
			i++;
		}
		return arr;
	}

	public NBTTagCompound getNBT() {
		NBTTagCompound compound = new NBTTagCompound();
		NBTTagList mList = new NBTTagList();
		for (Marcet marcet : markets.values()) {
			if (marcet == null) { continue; }
			NBTTagCompound nbtMarcet = marcet.save();
			mList.appendTag(nbtMarcet);
		}
		compound.setTag("Marcets", mList);
		NBTTagList dList = new NBTTagList();
		for (Deal deal : deals.values()) {
			if (deal == null) {
				continue;
			}
			NBTTagCompound nbtDeal = deal.save();
			dList.appendTag(nbtDeal);
		}
		compound.setTag("Deals", dList);
        int version = 1;
        compound.setInteger("Version", version);
		return compound;
	}

	public int getUnusedDealId() {
		int id = 0;
		while (deals.containsKey(id)) { id++; }
		return id;
	}

	public int getUnusedMarketId() {
		int id = 0;
		while (markets.containsKey(id)) { id++; }
		return id;
	}

	private void load() {
		CustomNpcs.debugData.start(null);
		File saveDir = CustomNpcs.getWorldSaveDirectory();
		if (saveDir == null || saveDir.toString().equals(".")) {
			CustomNpcs.debugData.end(null);
			return;
		}
		filePath = saveDir.getAbsolutePath();
		try {
			File file = new File(saveDir, "marcet.dat");
			if (file.exists()) {
				load(file);
			}
		} catch (Exception e) {
			try {
				File file2 = new File(saveDir, "marcet.dat_old");
				if (file2.exists()) {
					load(file2);
				}
			} catch (Exception er) { LogWriter.error(er); }
		}
		if (markets.isEmpty() || !markets.containsKey(0)) { loadDefaultMarcets(); }
		CustomNpcs.debugData.end(null);
	}

	private void load(File file) throws IOException {
		load(CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath())));
	}

	public void load(NBTTagCompound nbtFile) throws IOException {
		markets.clear();
		deals.clear();
		int v = nbtFile.getInteger("Version");
		if (v == 0) {
			Map<Integer, Map<Integer, List<Deal>>> marketDeals = new HashMap<>();
			if (nbtFile.hasKey("Deals", 9)) {
				for (int i = 0; i < nbtFile.getTagList("Deals", 10).tagCount(); ++i) {
					NBTTagCompound nbtDeal = nbtFile.getTagList("Deals", 10).getCompoundTagAt(i);
					Deal deal = loadDeal(nbtDeal);
					if (deal != null) {
						deals.put(deal.getId(), deal);
						int mId = nbtDeal.getInteger("MarcetID");
						if (!marketDeals.containsKey(mId)) { marketDeals.put(mId, new TreeMap<>()); }
						int tab = nbtDeal.getInteger("SectionID");
						if (!marketDeals.get(mId).containsKey(tab)) { marketDeals.get(mId).put(tab, new ArrayList<>()); }
						Deal d = deal.copy();
						d.updateNew();
						marketDeals.get(mId).get(tab).add(d);
					}
				}
			}
			if (nbtFile.hasKey("Marcets", 9)) {
				for (int i = 0; i < nbtFile.getTagList("Marcets", 10).tagCount(); ++i) {
					Marcet marcet = loadMarcet(nbtFile.getTagList("Marcets", 10).getCompoundTagAt(i));
					if (marcet != null) {
						markets.put(marcet.getId(), marcet);
						Map<Integer, List<Deal>> sections = marketDeals.get(marcet.getId());
						if (!sections.isEmpty()) {
							for (int tab : sections.keySet()) {
								if (!marcet.sections.containsKey(tab)) { marcet.sections.put(tab, new MarcetSection(tab)); }
								for (Deal d : sections.get(tab)) { marcet.sections.get(tab).deals.add(d); }
							}
						}
						
					}
				}
			}
		}
		else if (v == 1) {
			if (nbtFile.hasKey("Deals", 9)) {
				for (int i = 0; i < nbtFile.getTagList("Deals", 10).tagCount(); ++i) {
					Deal deal = loadDeal(nbtFile.getTagList("Deals", 10).getCompoundTagAt(i));
					if (deal != null) { deals.put(deal.getId(), deal); }
				}
			}
			if (nbtFile.hasKey("Marcets", 9)) {
				for (int i = 0; i < nbtFile.getTagList("Marcets", 10).tagCount(); ++i) {
					Marcet marcet = loadMarcet(nbtFile.getTagList("Marcets", 10).getCompoundTagAt(i));
					if (marcet != null) { markets.put(marcet.getId(), marcet); }
				}
			}
		}
	}

	public Deal loadDeal(NBTTagCompound nbtDeal) {
		if (nbtDeal == null || !nbtDeal.hasKey("DealID", 3) || nbtDeal.getInteger("DealID") < 0) { return null; }
		int id = nbtDeal.getInteger("DealID");
		if (deals.containsKey(id)) {
			deals.get(id).load(nbtDeal);
			for (Marcet market : markets.values()) {
				if (market.getDeal(id) != null) {
					market.getDeal(id).load(nbtDeal);
					market.updateNew();
				}
			}
			return deals.get(id);
		}
		Deal deal = new Deal(id);
		deal.load(nbtDeal);
		deals.put(deal.getId(), deal);
		return deal;
	}

	public void loadDefaultMarcets() {
		Marcet marcet = markets.containsKey(0) ? markets.get(0) : new Marcet(0);
		marcet.name = "Default Marcet";
		marcet.updateTime = 5;
		marcet.lastTime = System.currentTimeMillis();

		MarcetSection s0 = new MarcetSection(0);
		s0.setIcon(23);
		marcet.sections.clear();
		markets.put(marcet.getId(), marcet);

		Deal d0 = deals.containsKey(0) ? deals.get(0) : addDeal();
		d0.set(new ItemStack(Items.DIAMOND),
				new ItemStack[] { new ItemStack(Items.GOLD_INGOT, 10, 0), new ItemStack(Items.IRON_INGOT, 45, 0) });
		d0.setType(2);
		d0.setCount(2, 7);
		d0.setChance(0.1575f);
		s0.addDeal(0);
		Deal d1 = deals.containsKey(1) ? deals.get(1) : addDeal();
		d1.set(new ItemStack(Items.IRON_INGOT, 4, 0), new ItemStack[] { new ItemStack(Items.GOLD_INGOT) });
		d1.setType(2);
		d1.setChance(0.80f);
		s0.addDeal(1);
		marcet.sections.put(s0.getId(), s0);

		MarcetSection s1 = new MarcetSection(1);
		s1.setIcon(5);
		s1.name = "market.default.section.1";
		Deal d2 = deals.containsKey(2) ? deals.get(2) : addDeal();
		d2.set(new ItemStack(Blocks.COBBLESTONE, 16, 0), new ItemStack[0]);
		d2.setType(1);
		d2.setMoney(160);
		d2.setChance(0.955f);
		s1.addDeal(2);
		marcet.sections.put(s1.getId(), s1);

		MarcetSection s2 = new MarcetSection(2);
		s2.setIcon(29);
		s2.name = "market.default.section.2";
		Deal d3 = deals.containsKey(3) ? deals.get(3) : addDeal();
		d3.setIsCase(true);
		d3.setType(0);
		d3.setDonat(25);
		d3.setRarityColor(new Color(0xFFFF00).getRGB());
		Map<Integer, DropSet> caseItems = new TreeMap<>();
		DropSet ds0 = new DropSet(d3);
		ds0.setChance(100.0d);
		ds0.setAmount(16, 16);
		DropSet ds1 = new DropSet(d3);
		ds1.setChance(90.0d);
		ds1.setAmount(6, 8);
		NpcAPI api = NpcAPI.Instance();
		if (api != null) {
			ds0.setItem(api.getIItemStack(new ItemStack(Items.COOKED_CHICKEN, 1)));
			ds1.setItem(api.getIItemStack(new ItemStack(Items.COOKED_BEEF, 1)));
		}
		caseItems.put(0, ds0);
		caseItems.put(1, ds1);
		d3.setCaseItems(caseItems);
		d3.setShowInCase(true);
		s2.addDeal(3);
		marcet.sections.put(s2.getId(), s2);

		save();
	}

	public Marcet loadMarcet(NBTTagCompound nbtMarcet) {
		if (nbtMarcet == null || !nbtMarcet.hasKey("MarcetID", 3) || nbtMarcet.getInteger("MarcetID") < 0) { return null; }
		int id = nbtMarcet.getInteger("MarcetID");
		Marcet marcet;
		if (markets.containsKey(id)) { marcet = markets.get(id); }
		else { marcet = new Marcet(id); }
		marcet.load(nbtMarcet);
		markets.put(marcet.getId(), marcet);
		return markets.get(marcet.getId());
	}

	public int loadOld(NBTTagCompound nbttagcompound) {
		String marketName = nbttagcompound.getString("TraderMarket");
		if (!marketName.isEmpty()) {
			for (Marcet m : markets.values()) {
				if (m.name.equalsIgnoreCase(marketName)) {
					return m.getId();
				}
			}
		}
		Marcet marcet = addMarcet();
		if (!marketName.isEmpty()) {
			marcet.setName(marketName);
		}
		boolean ignoreDamage = nbttagcompound.getBoolean("TraderIgnoreDamage");
		boolean ignoreNBT = nbttagcompound.getBoolean("TraderIgnoreNBT");
		NpcMiscInventory inventoryCurrency = new NpcMiscInventory(36);
		NpcMiscInventory inventorySold = new NpcMiscInventory(18);
		inventoryCurrency.load(nbttagcompound.getCompoundTag("TraderCurrency"));
		inventorySold.load(nbttagcompound.getCompoundTag("TraderSold"));

		for (int i = 0; i < 18; i++) {
			if (inventorySold.getStackInSlot(i).isEmpty()) {
				continue;
			}
			ItemStack st0 = inventoryCurrency.getStackInSlot(i);
			ItemStack st1 = inventoryCurrency.getStackInSlot(i + 18);
			if (st0.isEmpty() && st1.isEmpty()) {
				continue;
			}
			Deal deal = addDeal();
			deal.set(inventorySold.getStackInSlot(i), new ItemStack[] { st0, st1 });
			deal.setIgnoreDamage(ignoreDamage);
			deal.setIgnoreNBT(ignoreNBT);
			marcet.sections.get(0).addDeal(deal.getId());
		}
		return marcet.getId();
	}

	@Override
	public void removeDeal(int dealId) {
        if (deals.containsKey(dealId)) {
			deals.remove(dealId);
            for (Marcet m : markets.values()) {
				for (MarcetSection ms : m.sections.values()) {
					for (Deal deal : ms.deals) {
						if (deal.getId() == dealId) {
							ms.deals.remove(deal);
							break;
						}
					}
				}
			}
		}
		save();
	}

	@Override
	public void removeMarcet(int marcetId) {
		if (marcetId < 0 || (marcetId != 0 && markets.size() <= 1)) { return; }
		if (!markets.containsKey(marcetId)) {
			if (marcetId == 0) { loadDefaultMarcets(); }
			return;
		}
		Marcet marcet = markets.get(marcetId);
		marcet.closeForAllPlayers();
		markets.remove(marcetId);
		if (marcetId == 0) { loadDefaultMarcets(); }
		save();
	}

	@SuppressWarnings("all")
	public void save() {
		try {
			// Save
			File saveDir = CustomNpcs.getWorldSaveDirectory();
			if (saveDir == null || saveDir.toString().equals(".")) { return; }
			File file = new File(saveDir, "marcet.dat_new");
			File file2 = new File(saveDir, "marcet.dat_old");
			File file3 = new File(saveDir, "marcet.dat");
			CompressedStreamTools.writeCompressed(getNBT(), Files.newOutputStream(file.toPath()));
			if (file2.exists()) { file2.delete(); }
			file3.renameTo(file2);
			if (file3.exists()) { file3.delete(); }
			file.renameTo(file3);
			if (file.exists()) { file.delete(); }
		} catch (Exception e) { LogWriter.error(e); }
	}

	public void sendTo(EntityPlayerMP player, int marcetID) {
		LogWriter.debug("CustomNpcs: Send marked data to \"" + player.getName() + "\"; marcetID: " + marcetID);
		if (markets.isEmpty() || !markets.containsKey(0)) { loadDefaultMarcets(); }
		if (markets.containsKey(marcetID)) {
			markets.get(marcetID).sendTo(player);
		} // market
		else if (marcetID < 0) {
			Map<Integer, Marcet> mapM = new HashMap<>(markets);
			Map<Integer, Deal> mapD = new HashMap<>(deals);
			Packets.send(player, new PacketClearMarcets());
			for (int id : mapD.keySet()) { Packets.send(player, new PacketDealData(mapD.get(id).save())); }
			for (int id : mapM.keySet()) { Packets.send(player, new PacketMarcetData(mapM.get(id).save())); }
			Packets.sendDelayed(player, new PacketUpdateMarcetGui(), 250);
		} // all
		else { Packets.send(player, new PacketMarcetRemove(marcetID)); } // not
	}

	public void update() {
		try {
			for (Marcet m : new ArrayList<>(markets.values())) { m.update(); }
		}
		catch (Throwable ignored) { }
		try {
			for (Deal d : new ArrayList<>(deals.values())) { d.update(); }
		}
		catch (Throwable ignored) { }
	}

	public void updateTime() {
		try {
			for (Marcet m : new ArrayList<>(markets.values())) { m.updateTime(); }
		}
		catch (Throwable ignored) { }
	}

	public void sendToAll() {
		if (CustomNpcs.Server != null) {
			for (EntityPlayerMP player : CustomNpcs.Server.getPlayerList().getPlayers()) {
				sendTo(player, -1);
			}
		}
	}

}
