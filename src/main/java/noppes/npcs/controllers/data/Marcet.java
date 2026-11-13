package noppes.npcs.controllers.data;

import java.util.*;

import com.google.common.base.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomEntities;
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

    public List<Player> listeners = new ArrayList<>();
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
        markup.put(0, new MarkupData(0, 0.0f, 0.04f, 1000));
        markup.put(1, new MarkupData(1, 0.0f, 0.04f, 2200));
        markup.put(2, new MarkupData(2, 0.0f, 0.04f, 5000));
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

    public void addListener(Player listener, boolean isServer) {
        for (Player pl : listeners) {
            if (listener == pl || pl.equals(listener)) { return; }
        }
        listeners.add(listener);
        if (isServer && listener instanceof ServerPlayer) { detectAndSendChanges(); }
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
        for (Player player : listeners) {
            if (!(player instanceof ServerPlayer)) { return; }
            CustomNPCsScheduler.runTack(player::closeContainer, 250);
        }
    }

    public Marcet copy(int newID) {
        Marcet marcet = new Marcet(newID > -1 ? newID : id);
        marcet.load(save());
        marcet.updateNew();
        return marcet;
    }

    public void detectAndSendChanges() {
        for (Player listener : listeners) {
            if (listener instanceof ServerPlayer) { sendTo((ServerPlayer) listener); }
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
        if (!sections.containsKey(section)) { return new IDeal[0]; }
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
                .append(Component.literal("ID:" + id + " ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable(name).withStyle(isEmpty() ? ChatFormatting.DARK_RED : ChatFormatting.RESET));
    }

    public boolean notHasListener(Player player) { return !listeners.contains(player); }

    public boolean isEmpty() { return getAllDeals().length == 0; }

    @Override
    public boolean isLimited() { return isLimited; }

    public boolean isValid() {
        if (sections.isEmpty()) { return false; }
        boolean hasDeals = false;
        for (MarcetSection ms : new ArrayList<>(sections.values())) {
            for (Deal deal : new ArrayList<>(ms.deals)) {
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

    public void load(CompoundTag compound) {
        id = compound.getInt("MarcetID");
        name = compound.getString("Name");
        isLimited = compound.getBoolean("IsLimited");
        showXP = compound.getBoolean("ShowXP");
        money = compound.getLong("Money");

        markup.clear();
        for (int i = 0; i < compound.getList("Markup", 10).size(); i++) {
            MarkupData md = new MarkupData(compound.getList("Markup", 10).getCompound(i));
            markup.put(md.level, md);
        }
        if (markup.isEmpty()) {
            markup.put(0, new MarkupData(0, 0.0f, 0.04f, 1000));
            markup.put(1, new MarkupData(1, 0.0f, 0.04f, 2200));
            markup.put(2, new MarkupData(2, 0.0f, 0.04f, 5000));
        }

        inventory.clear();
        for (int i = 0; i < compound.getList("Inventory", 10).size(); i++) {
            CompoundTag nbt = compound.getList("Inventory", 10).getCompound(i);
            inventory.put(ItemStack.of(nbt), nbt.getInt("TotalCount"));
        }

        sections.clear();
        Map<Integer, MarcetSection> newSec = new TreeMap<>();
        if (!compound.contains("Sections", 9) || compound.getList("Sections", 10).isEmpty()) {
            newSec.put(0, new MarcetSection(0));
        } else {
            for (int i = 0; i < compound.getList("Sections", 10).size(); i++) {
                CompoundTag nbt = compound.getList("Sections", 10).getCompound(i);
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

        limitedType = compound.getInt("LimitedType");
        updateTime = compound.getInt("UpdateTime");
        lastTime = compound.getLong("LastTime");
        nextTime = compound.getLong("NextTime");
        if (compound.contains("NpcLines", 10)) {
            lines.load(compound.getCompound("NpcLines"));
        }
    }

    public void removeInventoryItems(Map<ItemStack, Integer> items) {
        for (ItemStack stack : items.keySet()) {
            if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
            List<ItemStack> del = new ArrayList<>();
            for (ItemStack st : new ArrayList<>(inventory.keySet())) {
                if (NoppesUtilServer.isItemStackNull(st)) {
                    del.add(st);
                    continue;
                }
                if (NoppesUtilPlayer.compareItems(stack, st, false, false)) {
                    inventory.put(st, inventory.get(st) - items.get(stack));
                    if (inventory.get(st) <= 0) { del.add(st); }
                    break;
                }
            }
            for (ItemStack st : del) { inventory.remove(st); }
        }
    }

    public void removeListener(Player player, boolean isServer) {
        for (Player listener : new ArrayList<>(listeners)) {
            if (listener == player || listener.equals(player)) {
                if (isServer && listener instanceof ServerPlayer sPlayer) { Packets.send(sPlayer, new PacketMarcetClose(id)); }
                listeners.remove(listener);
                detectAndSendChanges();
                return;
            }
        }
    }

    public void sendTo(ServerPlayer player) { Packets.send(player, new PacketMarcetData(save())); }

    @Override
    public void setIsLimited(boolean limited) {
        if (isLimited == limited) { return; }
        isLimited = limited;
        if (limited) { updateNew(); }
    }

    @Override
    public void setName(String newName) { name = newName; }

    public void update() { // any 1.0 sec -> (MarcetController.update) ServerTickHandler / ServerTickEvent
        if (updateTime < 5L) { return; }
        if (lastTime <= System.currentTimeMillis() - 7200000L || lastTime + updateTime * 60000L < System.currentTimeMillis()) { updateNew(); }
    }

    @Override
    public void updateNew() {
        inventory.clear();
        lastTime = System.currentTimeMillis();
        if (lines != null && !lines.isEmpty() && CustomNpcs.Server != null) {
            for (ServerLevel level : CustomNpcs.Server.getAllLevels()) {
                List<? extends EntityNPCInterface> npcs = level.getEntities(CustomEntities.entityCustomNpc, this);
                for (EntityNPCInterface npc : npcs) {
                    if (!npc.isAlive()) { continue; }
                    npc.saySurrounding(lines.getLine(true));
                }
            }
        }
        money = (long) (Math.random() * 7500.0d);
        for (MarcetSection ms : new ArrayList<>(sections.values())) {
            for (Deal deal : new ArrayList<>(ms.deals)) {
                deal.updateNew();
                money += (long) ((double) (deal.getMoney()) * (coefficient + Math.random() * coefficient));
                for (IItemStack iStack : deal.getCurrency().getItems()) {
                    ItemStack stack = iStack.getMCItemStack();
                    if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
                    int count = (int) (((double) stack.getCount()) * (coefficient + Math.random() * coefficient));
                    boolean added = false;
                    for (ItemStack st : inventory.keySet()) {
                        if (!NoppesUtilServer.isItemStackNull(st) && NoppesUtilPlayer.compareItems(stack, st, false, false)) {
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

    @OnlyIn(Dist.CLIENT)
    public void updateTime() { // any 0.5 sec -> (MarcetController.updateTime) ClientTickHandler / ClientTickEvent
        if (nextTime < 0L) { nextTime = 0L; }
        else if (nextTime > 0L) {
            nextTime -= 500L;
            if (nextTime < 0) { nextTime = 0; }
        }
    }

    public CompoundTag save() {
        CompoundTag compound = new CompoundTag();
        compound.putInt("MarcetID", id);
        compound.putString("Name", name);
        compound.putBoolean("IsLimited", isLimited);
        compound.putBoolean("ShowXP", showXP);
        compound.putLong("Money", money);

        ListTag markups = new ListTag();
        synchronized(markup) {
            for (int level : new ArrayList<>(markup.keySet())) {
                MarkupData mp = markup.get(level);
                mp.level = level;
                markups.add(mp.getNBT());
            }
        }
        compound.put("Markup", markups);

        ListTag items = new ListTag();
        for (ItemStack stack : new ArrayList<>(inventory.keySet())) {
            CompoundTag nbt = new CompoundTag();
            stack.save(nbt);
            nbt.putInt("TotalCount", inventory.get(stack));
            items.add(nbt);
        }
        compound.put("Inventory", items);

        ListTag secs = new ListTag();
        for (MarcetSection ms : new ArrayList<>(sections.values())) { secs.add(ms.save()); }
        compound.put("Sections", secs);

        compound.putInt("LimitedType", limitedType);
        compound.putInt("UpdateTime", updateTime);
        compound.putLong("LastTime", lastTime);
        compound.putLong("NextTime", nextTime = (lastTime + updateTime * 60000L - System.currentTimeMillis()));
        compound.put("NpcLines", lines.save());
        return compound;
    }

    public void resetAllDeals() {
        MarcetController mData = MarcetController.getInstance();
        for (MarcetSection ms : new ArrayList<>(sections.values())) {
            for (Deal deal : new ArrayList<>(ms.deals)) {
                if (mData.deals.containsKey(deal.getId())) { deal.load(mData.deals.get(deal.getId()).saveData()); }
            }
        }
        updateNew();
    }

}
