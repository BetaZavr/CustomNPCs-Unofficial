package noppes.npcs.controllers.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.IPlayerMail;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;

// Changed by Unofficial (BetaZavr)
public class PlayerMail implements IPlayerMail, Container {

   public final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
   public CompoundTag message = new CompoundTag();
   public long timeWhenReceived = System.currentTimeMillis();
   public long timeWillCome = 0L;
   public int questId = -1;
   public int ransom = 0;
   public int money = 0;
   public int donat = 0;
   public boolean beenRead = false;
   public boolean returned = false;
   public String sender = "";
   public String title = "";

   @Override
   public void clearContent() {
      title = "";
      sender = "";
      beenRead = false;
      returned = false;
      questId = -1;
      items.clear();
      money = 0;
      donat = 0;
      ransom = 0;
      timeWhenReceived = System.currentTimeMillis();
      for (String key : message.getAllKeys()) { message.remove(key); }
   }

   public void load(CompoundTag compound) {
      title = compound.getString("Subject");
      sender = compound.getString("Sender");
      message = compound.getCompound("Message");
      beenRead = compound.getBoolean("BeenRead");
      returned = compound.getBoolean("Returned");
      timeWillCome = compound.getLong("TimeWillCome");
      timeWhenReceived = compound.getLong("TimeWhenReceived");
      if (compound.contains("MailQuest")) { questId = compound.getInt("MailQuest"); }
      items.clear();
      ListTag list = compound.getList("MailItems", 10);
      for (int i = 0; i < list.size(); ++i) {
         CompoundTag nbt = list.getCompound(i);
         int j = nbt.getByte("Slot") & 0xFF;
         if (j < items.size()) { items.set(j, ItemStack.of(nbt)); }
      }
      money = compound.getInt("Money");
      donat = compound.getInt("Donat");
      ransom = compound.getInt("Ransom");
   }

   public CompoundTag save() {
      CompoundTag compound = new CompoundTag();
      compound.putString("Subject", title);
      compound.putString("Sender", sender);
      compound.put("Message", message);
      compound.putBoolean("BeenRead", beenRead);
      compound.putBoolean("Returned", returned);
      compound.putLong("TimeWillCome", timeWillCome);
      compound.putLong("TimeWhenReceived", timeWhenReceived);
      compound.putInt("MailQuest", questId);
      if (hasQuest()) { compound.putString("MailQuestTitle", getQuest().getTitle().getString()); }
      ListTag list = new ListTag();
      for (int i = 0; i < items.size(); ++i) {
         if (!(items.get(i)).isEmpty()) {
            CompoundTag nbt = new CompoundTag();
            nbt.putByte("Slot", (byte) i);
            (items.get(i)).save(nbt);
            list.add(nbt);
         }
      }
      compound.put("MailItems", list);
      compound.putInt("Money", money);
      compound.putInt("Donat", donat);
      compound.putInt("Ransom", ransom);
      return compound;
   }

   public boolean isValid() { return !title.isEmpty() && !message.isEmpty() && !sender.isEmpty(); }

   public boolean hasQuest() { return getQuest() != null; }

   @Override
   public Quest getQuest() { return QuestController.instance != null ? QuestController.instance.quests.get(questId) : null; }

   @Override
   public void setQuest(int id) {
      if (id < 0) { throw new CustomNPCsException("Quest id is lower than 0"); }
      questId = id;
   }

   @Override
   public int getContainerSize() { return items.size(); }

   @Override
   public @Nonnull ItemStack getItem(int slotId) { return items.get(slotId); }

   @Override
   public @Nonnull ItemStack removeItem(int slotId, int count) {
      ItemStack itemstack = ContainerHelper.removeItem(items, slotId, count);
      if (!itemstack.isEmpty()) { setChanged(); }
      return itemstack;
   }

   @Override
   public @Nonnull ItemStack removeItemNoUpdate(int slotId) { return items.set(slotId, ItemStack.EMPTY); }

   @Override
   public void setItem(int slotId, @Nonnull ItemStack stack) {
      items.set(slotId, stack);
      if (stack.getCount() > getMaxStackSize()) { stack.setCount(getMaxStackSize()); }
      setChanged();
   }

   @Override
   public void setChanged() { }

   @Override
   public boolean stillValid(@Nonnull Player player) { return true; }

   @Override
   public void startOpen(@Nonnull Player player) { }

   @Override
   public void stopOpen(@Nonnull Player player) { }

   @Override
   public boolean canPlaceItem(int slotId, @Nonnull ItemStack itemStack) { return true; }

   @Override
   public boolean isEmpty() {
      for(int slot = 0; slot < getContainerSize(); ++slot) {
         if (!NoppesUtilServer.isItemStackNull(getItem(slot))) { return false; }
      }
      return true;
   }

   @Override
   public String getSender() { return sender; }

   @Override
   public void setSender(String senderIn) { sender = senderIn == null ? "" : senderIn; }

   @Override
   public String getSubject() { return title; }

   @Override
   public void setSubject(String titleIn) { title = titleIn == null ? "" : titleIn; }

   @Override
   public List<String> getText() {
      List<String> list = new ArrayList<>();
      ListTag pages = message.getList("pages", 8);
      for(int i = 0; i < pages.size(); ++i) { list.add(pages.getString(i)); }
      return list;
   }

   @Override
   public void setText(String ... pages) {
      ListTag list = new ListTag();
      if (pages != null) {
         for(String page : pages) { list.add(StringTag.valueOf(page)); }
      }
      message.put("pages", list);
   }

   @Override
   public IContainer getContainer() { return Objects.requireNonNull(NpcAPI.Instance()).getIContainer(this); }

   @Override
   public int getMoney() { return money; }

   @Override
   public void setMoney(int moneyIn) { money = ValueUtil.correctInt(moneyIn, 0, Integer.MAX_VALUE); }

   @Override
   public int getDonat() { return donat; }

   @Override
   public void setDonat(int donatIn) { donat = ValueUtil.correctInt(donatIn, 0, Integer.MAX_VALUE); }

   @Override
   public int getRansom() { return ransom; }

   @Override
   public void setRansom(int moneyIn) { ransom = ValueUtil.correctInt(moneyIn, 0, Integer.MAX_VALUE); }

   public boolean isReturned() { return returned; }

   public PlayerMail copy() {
      PlayerMail mail = new PlayerMail();
      mail.load(save());
      return mail;
   }

}
