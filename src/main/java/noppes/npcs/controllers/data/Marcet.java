package noppes.npcs.controllers.data;

import java.util.*;

import com.google.common.base.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.handler.data.IDeal;
import noppes.npcs.api.handler.data.IMarcet;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketMarcetClose;
import noppes.npcs.packets.client.PacketMarcetData;
import noppes.npcs.roles.RoleTrader;
import noppes.npcs.util.CustomNPCsScheduler;

public class Marcet implements IMarcet, Predicate<EntityNPCInterface> {

	public final Map<Integer, MarkupData> markup = new TreeMap<>();
	public final Map<ItemStack, Integer> inventory = new HashMap<>();
	public final Map<Integer, MarcetSection> sections = new TreeMap<>(); // [TabID, Section]

	public List<EntityPlayer> listeners = new ArrayList<>();
	public Lines lines = new Lines();
	public String name = "Market";
	private int id;
	public boolean isLimited = false;
	public boolean showXP = false;
	public int updateTime = 0;
	public int limitedType = 0;
	public long lastTime;
	public long nextTime;
	public long money = 0;
	public double coefficient = 5.0d;

	public Marcet(int idIn) {
		id = idIn;
		markup.put(0, new MarkupData(0, 0.0f, 0.0f, 1000));
		markup.put(1, new MarkupData(1, 0.0f, 0.0f, 2200));
		markup.put(2, new MarkupData(2, -0.0f, 0.0f, 5000));
		sections.put(0, new MarcetSection(0));
		updateNew();
	}

	public void addInventoryItems(Map<ItemStack, Integer> items) {
		for (ItemStack stack : items.keySet()) {
			if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
			boolean added = false;
			List<ItemStack> del = new ArrayList<>();
			for (ItemStack st : inventory.keySet()) {
				if (NoppesUtilServer.isItemStackNull(st)) {
					del.add(st);
					continue;
				}
				if (NoppesUtilPlayer.compareItems(stack, st, false, false)) {
					inventory.put(st, inventory.get(st) + items.get(stack));
					added = true;
					break;
				}
			}
			for (ItemStack st : del) { inventory.remove(st); }
			if (!added) { inventory.put(stack, items.get(stack)); }
		}
	}

	public void addListener(EntityPlayer listener, boolean isServer) {
		for (EntityPlayer pl : listeners) {
			if (listener == pl || pl.equals(listener)) { return; }
		}
		listeners.add(listener);
		if (isServer && listener instanceof EntityPlayerMP) { detectAndSendChanges(); }
	}

	@Override
	public boolean apply(EntityNPCInterface npc) {
		if (npc == null || !(npc.role instanceof RoleTrader)) { return false; }
		return ((RoleTrader) npc.role).getMarket() == null ?
				((RoleTrader) npc.role).getMarketID() == getId() :
				((RoleTrader) npc.role).getMarket().getId() == getId();
	}

	public void closeForAllPlayers() { // server only
		if (listeners == null) { return; }
		for (EntityPlayer player : listeners) {
			if (!(player instanceof EntityPlayerMP)) { return; }
			CustomNPCsScheduler.runTack(player::closeScreen, 250);
		}
	}

	public Marcet copy(int newID) {
		Marcet marcet = new Marcet(newID > -1 ? newID : id);
		marcet.load(save());
		marcet.updateNew();
		return marcet;
	}

	public void detectAndSendChanges() {
		for (EntityPlayer listener : listeners) {
			if (listener instanceof EntityPlayerMP) { sendTo((EntityPlayerMP) listener); }
		}
	}

	@Override
	public IDeal[] getAllDeals() {
		List<IDeal> list = new ArrayList<>();
		for (MarcetSection ms : sections.values()) { list.addAll(ms.deals); }
		return list.toArray(new IDeal[0]);
	}

	public Deal getDeal(int dealID) {
		for (MarcetSection ms : sections.values()) {
			for (Deal deal : ms.deals) {
				if (deal.getId() == dealID) { return deal; }
			}
		}
		return null;
	}

	@Override
	public IDeal[] getDeals(int section) {
		if (!sections.containsKey(section)) {
			return new IDeal[0];
		}
		return sections.get(section).deals.toArray(new IDeal[0]);
	}

	@Override
	public int getId() { return id; }

	@Override
	public String getName() { return name; }

	public int getSection(int dealID) {
		for (MarcetSection ms : sections.values()) {
			for (Deal deal : ms.deals) {
				if (deal.getId() == dealID) { return ms.getId(); }
			}
		}
		return -1;
	}

	public Component getSettingName() {
		return Component.empty()
				.append(Component.literal("ID:" + id + " ").withStyle(TextFormatting.GRAY))
				.append(Component.translatable(name).withStyle(isEmpty() ? TextFormatting.DARK_RED : TextFormatting.RESET));
	}

	public boolean notHasListener(EntityPlayer player) { return !listeners.contains(player); }

	public boolean isEmpty() { return getAllDeals().length == 0; }

	@Override
	public boolean isLimited() { return isLimited; }

	public boolean isValid() {
		if (sections.isEmpty()) { return false; }
		boolean hasDeals = false;
		for (MarcetSection ms : sections.values()) {
			for (Deal deal : ms.deals) {
				if (deal.isValid()) {
					hasDeals = true;
					continue;
				}
				if (deal.getProduct().getMCItemStack() == null || deal.getProduct().getMCItemStack().getItem() == Items.AIR) { return false; }
				if (deal.getMoney() == 0 && deal.getCurrency().isEmpty()) { return false; }
			}
		}
		return hasDeals;
	}

	public void load(NBTTagCompound compound) {
		id = compound.getInteger("MarcetID");
		name = compound.getString("Name");
		isLimited = compound.getBoolean("IsLimited");
		showXP = compound.getBoolean("ShowXP");
		money = compound.getLong("Money");

		markup.clear();
		for (int i = 0; i < compound.getTagList("Markup", 10).tagCount(); i++) {
			MarkupData md = new MarkupData(compound.getTagList("Markup", 10).getCompoundTagAt(i));
			markup.put(md.level, md);
		}
		if (markup.isEmpty()) {
			markup.put(0, new MarkupData(0, 0.15f, 0.80f, 1000));
			markup.put(1, new MarkupData(1, 0.0f, 0.45f, 2200));
			markup.put(2, new MarkupData(2, -0.05f, 0.0f, 5000));
		}

		inventory.clear();
		for (int i = 0; i < compound.getTagList("Inventory", 10).tagCount(); i++) {
			NBTTagCompound nbt = compound.getTagList("Inventory", 10).getCompoundTagAt(i);
			inventory.put(new ItemStack(nbt), nbt.getInteger("TotalCount"));
		}

		sections.clear();
		Map<Integer, MarcetSection> newSec = new TreeMap<>();
		if (!compound.hasKey("Sections", 9) || compound.getTagList("Sections", 10).tagCount() == 0) {
			newSec.put(0, new MarcetSection(0));
		}
		else {
			for (int i = 0; i < compound.getTagList("Sections", 10).tagCount(); i++) {
				NBTTagCompound nbt = compound.getTagList("Sections", 10).getCompoundTagAt(i);
				MarcetSection ms = MarcetSection.create(nbt);
				newSec.put(ms.getId(), MarcetSection.create(nbt));
			}
			// Sorting
			Map<Integer, MarcetSection> sec = new TreeMap<>();
			int i = 0;
			for (MarcetSection ms : newSec.values()) {
				sec.put(i, ms);
				i++;
			}
			newSec = sec;
		}
		sections.putAll(newSec);

		limitedType = compound.getInteger("LimitedType");
		updateTime = compound.getInteger("UpdateTime");
		lastTime = compound.getLong("LastTime");
		nextTime = compound.getLong("NextTime");
		if (compound.hasKey("NpcLines", 10)) { lines.load(compound.getCompoundTag("NpcLines")); }
	}

	public void removeInventoryItems(Map<ItemStack, Integer> items) {
		for (ItemStack stack : items.keySet()) {
			if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
			List<ItemStack> del = new ArrayList<>();
			for (ItemStack st : inventory.keySet()) {
				if (NoppesUtilServer.isItemStackNull(st)) {
					del.add(st);
					continue;
				}
				if (NoppesUtilPlayer.compareItems(stack, st, false, false)) {
					inventory.put(st, inventory.get(st) - items.get(stack));
					if (inventory.get(st) <= 0) {
						del.add(st);
					}
					break;
				}
			}
			for (ItemStack st : del) { inventory.remove(st); }
		}
	}

	public void removeListener(EntityPlayer player, boolean isServer) {
		for (EntityPlayer listener : listeners) {
			if (listener == player || listener.equals(player)) {
				if (isServer && listener instanceof EntityPlayerMP) { Packets.send((EntityPlayerMP) listener, new PacketMarcetClose(id)); }
				listeners.remove(listener);
				detectAndSendChanges();
				return;
			}
		}
	}

	public void sendTo(EntityPlayerMP player) { Packets.send(player, new PacketMarcetData(save())); }

	@Override
	public void setIsLimited(boolean limited) {
		if (isLimited == limited) { return; }
		isLimited = limited;
		if (limited) { updateNew(); }
	}

	@Override
	public void setName(String nameIn) { name = nameIn; }

	public void update() { // any 1.0 sec -> (MarcetController.update) ServerTickHandler / ServerTickEvent
		if (updateTime < 5L) { return; }
		if (lastTime <= System.currentTimeMillis() - 7200000L || lastTime + updateTime * 60000L < System.currentTimeMillis()) { updateNew(); }
	}

	@Override
	public void updateNew() {
		inventory.clear();
		lastTime = System.currentTimeMillis();
		if (lines != null && !lines.isEmpty() && CustomNpcs.Server != null) {
			for (WorldServer world : CustomNpcs.Server.worlds) {
				List<EntityNPCInterface> npcs = world.getEntities(EntityNPCInterface.class, this);
				for (EntityNPCInterface npc : npcs) {
					if (!npc.isEntityAlive()) { continue; }
					npc.saySurrounding(lines.getLine(true));
				}
			}
		}
		money = (long) (Math.random() * 7500.0d);
		for (MarcetSection ms : sections.values()) {
			for (Deal deal : ms.deals) {
				deal.updateNew();
				money += (long) ((double) (deal.getMoney()) * (coefficient + Math.random() * coefficient));
				for (IItemStack iStack : deal.getCurrency().getItems()) {
					ItemStack stack = iStack.getMCItemStack();
					if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
					int count = (int) (((double) stack.getCount()) * (coefficient + Math.random() * coefficient));
					boolean added = false;
					for (ItemStack st : inventory.keySet()) {
						if (NoppesUtilServer.isItemStackNull(st)) { continue; }
						if (NoppesUtilPlayer.compareItems(stack, st, false, false)) {
							inventory.put(st, inventory.get(st) + count);
							added = true;
							break;
						}
					}
					if (!added) { inventory.put(stack, count); }
				}
			}
		}
		detectAndSendChanges();
	}

	@SideOnly(Side.CLIENT)
	public void updateTime() { // any 0.5 sec -> (MarcetController.updateTime) ClientTickHandler / ClientTickEvent
		if (nextTime < 0L) { nextTime = 0L; }
		else if (nextTime > 0L) {
			nextTime -= 500L;
			if (nextTime < 0) { nextTime = 0; }
		}
	}

	public NBTTagCompound save() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("MarcetID", id);
		compound.setString("Name", name);
		compound.setBoolean("IsLimited", isLimited);
		compound.setBoolean("ShowXP", showXP);
		compound.setLong("Money", money);

		NBTTagList markupList = new NBTTagList();
		for (int level : markup.keySet()) {
			MarkupData mp = markup.get(level);
			mp.level = level;
			markupList.appendTag(mp.getNBT());
		}
		compound.setTag("Markup", markupList);

		NBTTagList items = new NBTTagList();
		for (ItemStack stack : inventory.keySet()) {
			NBTTagCompound nbt = new NBTTagCompound();
			stack.writeToNBT(nbt);
			nbt.setInteger("TotalCount", inventory.get(stack));
			items.appendTag(nbt);
		}
		compound.setTag("Inventory", items);

		NBTTagList secs = new NBTTagList();
		for (MarcetSection ms : sections.values()) {
			secs.appendTag(ms.save());
		}
		compound.setTag("Sections", secs);

		compound.setInteger("LimitedType", limitedType);
		compound.setInteger("UpdateTime", updateTime);
		compound.setLong("LastTime", lastTime);
		compound.setLong("NextTime", nextTime = (lastTime + updateTime * 60000L - System.currentTimeMillis()));
		compound.setTag("NpcLines", lines.save());
		return compound;
	}

	public void resetAllDeals() {
		MarcetController mData = MarcetController.getInstance();
		for (MarcetSection ms : sections.values()) {
			for (Deal deal : ms.deals) {
				if (mData.deals.containsKey(deal.getId())) { deal.load(mData.deals.get(deal.getId()).save()); }
			}
		}
		updateNew();
	}

}
