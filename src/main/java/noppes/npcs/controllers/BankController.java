package noppes.npcs.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.containers.ContainerManageBanks;
import noppes.npcs.containers.ContainerNPCBank;
import noppes.npcs.controllers.data.Bank;
import noppes.npcs.packets.server.SPacketBankGet;
import noppes.npcs.packets.server.SPacketBanksGet;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;

public class BankController {

   protected final List<Bank> banks = new ArrayList<>();
   protected static BankController instance;

   public BankController() {
      loadBanksData();
      if (banks.isEmpty()) {
         Bank bank = new Bank();
         bank.id = 0;
         bank.name = "Default Bank";
         banks.add(bank);
      }
   }

   public static BankController getInstance() {
      if (instance == null) { instance = new BankController(); }
      return instance;
   }

   private void loadBanksData() {
      CustomNpcs.debugData.start(null);
      File saveDir = CustomNpcs.getLevelSaveDirectory();
      if (saveDir != null) {
         LogWriter.info("Loading Banks");
         try {
            File file = new File(saveDir, "bank.dat");
            if (file.exists()) { loadBanksData(file); }
            else { save(); }
         } catch (Exception e) {
            try {
               File file = new File(saveDir, "bank.dat_old");
               if (file.exists()) { loadBanksData(file); }
               else { save(); }
            } catch (Exception ex) { LogWriter.error(ex); }
         }
      }
      CustomNpcs.debugData.end(null);
   }

   private void loadBanksData(File file) throws IOException {
      loadBanks(NbtIo.readCompressed(new FileInputStream(file)));
   }

   public void loadBanks(CompoundTag compound) {
      List<Bank> banksIn = new ArrayList<>();
      ListTag list = compound.getList("Data", 10);
      for (int i = 0; i < list.size(); ++i) {
         CompoundTag nbtBank = list.getCompound(i);
         Bank bank = new Bank();
         bank.load(nbtBank);
         banksIn.add(bank);
      }
      banks.clear();
      banks.addAll(banksIn);
   }

   public void loadBank(CompoundTag nbtBank) {
      int bankId = nbtBank.getInt("BankID");
      if (nbtBank.contains("BankID", 3) && bankId >= 0) {
         Bank bank = null;
         for (Bank b : banks) {
            if (b.id == bankId) {
               bank = b;
               break;
            }
         }
         if (bank == null) { bank = addNewBank(); }
         bank.load(nbtBank);
         // delete OLD
         if (bank.isPublic) {
            File datasDir = CustomNpcs.getLevelSaveDirectory("playerdata");
            if (datasDir != null) {
               File[] list = datasDir.listFiles();
               if (list != null) {
                  for (File playerDir : list) {
                     if (playerDir.isDirectory()) { Util.instance.removeFile(new File(playerDir, "banks/"+bank.id+".dat")); }
                  }
               }
            }
         }
         else { Util.instance.removeFile(CustomNpcs.getLevelSaveDirectory("banks/"+bank.id+".dat")); }
         if (CustomNpcs.Server != null) {
            for (ServerPlayer player : CustomNpcs.Server.getPlayerList().getPlayers()) {
               if (player.containerMenu instanceof ContainerManageBanks container) {
                  SPacketBanksGet.sendBankDataAll(player); // scroll data
                  if (container.isBank(bankId)) { SPacketBankGet.sendBank(player, bank, container.ceil); } // manage banks
               }
               else if (player.containerMenu instanceof ContainerNPCBank container && container.data.bank.id == bankId) { player.closeContainer(); } // bank
            }
         }
         save();
      }
   }

   public CompoundTag getNBT() {
      ListTag list = new ListTag();
      for (Bank bank : banks) { list.add(bank.save()); }
      CompoundTag compound = new CompoundTag();
      compound.put("Data", list);
      return compound;
   }

   public Bank getBank(int bankId) {
      for (Bank bank : banks) {
         if (bank.id == bankId) { return bank; }
      }
      return null;
   }

   public void save() {
      if (CustomNpcs.Server == null) { return; }
      try {
         File saveDir = CustomNpcs.getLevelSaveDirectory();
         File file = new File(saveDir, "bank.dat_new");
         File file1 = new File(saveDir, "bank.dat_old");
         File file2 = new File(saveDir, "bank.dat");
         NbtIo.writeCompressed(getNBT(), new FileOutputStream(file));
         if (file1.exists() && !file1.delete()) { LogWriter.debug("Error delete \"" + file1.getName() + "\" file"); }
         if (!file2.renameTo(file1) || (file2.exists() && !file2.delete())) { LogWriter.debug("Error delete or rename \"" + file2.getName() + "\" file"); }
         if (!file.renameTo(file2) || (file.exists() && !file.delete())) { LogWriter.debug("Error delete or rename \"" + file.getName() + "\" file"); }
      } catch (Exception e) {
         LogWriter.error(e);
      }
   }

   public int getUnusedId() {
      int id = 0;
      while (getBank(id) != null) { id++; }
      return id;
   }

   public Bank addNewBank() {
      Bank bank = new Bank();
      bank.id = getUnusedId();
      while (true) {
         boolean isBreak = true;
         for (Bank b : banks) {
            if (b.name.equals(bank.name)) {
               isBreak = false;
               break;
            }
         }
         if (isBreak) { break; }
         else { bank.name += "_"; }
      }
      banks.add(bank);
      return bank;
   }

   public void removeBank(int bankId) {
      for (Bank bank : banks) {
         if (bank.id == bankId) {
            if (banks.remove(bank) && CustomNpcs.Server != null) {
               Util.instance.removeFile(CustomNpcs.getLevelSaveDirectory("banks/"+bank.id+".dat"));
               File datasDir = CustomNpcs.getLevelSaveDirectory("playerdata");
               if (datasDir != null) {
                  File[] list = datasDir.listFiles();
                  if (list != null) {
                     for (File playerDir : list) {
                        if (!playerDir.isDirectory()) { continue; }
                        Util.instance.removeFile(new File(playerDir, "banks/"+bank.id+".dat"));
                     }
                  }
               }
            }
            break;
         }
      }
      if (CustomNpcs.Server != null) {
         for (ServerPlayer pl : CustomNpcs.Server.getPlayerList().getPlayers()) {
            if (pl.containerMenu instanceof ContainerManageBanks container) {
               SPacketBanksGet.sendBankDataAll(pl); // scroll data
               if (container.isBank(bankId)) { SPacketBankGet.sendBank(pl, new Bank(), 0); } // manage banks
            }
            else if (pl.containerMenu instanceof ContainerNPCBank container && container.data.bank.id == bankId) { pl.closeContainer(); } // bank
         }
      }
      save();
   }

   // New from Unofficial (BetaZavr)
   public void update() { // every 1 min --> ServerTickHandler.cnpcServerTick()
      if (CustomNpcs.Server != null) {
         for (Bank bank : banks) { bank.freeUpMemory(); }
      }
   }

   public List<Bank> getBanks() { return new ArrayList<>(banks); }

}
