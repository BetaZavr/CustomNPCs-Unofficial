package noppes.npcs.controllers.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.api.constants.OptionType;
import noppes.npcs.api.handler.data.IDialogOption;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.db.DatabaseColumn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DialogOption implements IDialogOption {

   public static class OptionDialogID {

      public int dialogId;
      public Availability availability;

      public OptionDialogID(int id) {
         dialogId = id;
         availability = new Availability();
      }

      public OptionDialogID(CompoundTag compound) {
         dialogId = compound.getInt("DialogId");
         availability = new Availability();
         availability.load(compound);
      }

      public CompoundTag getNBT() {
         CompoundTag compound = new CompoundTag();
         compound.putInt("DialogId", dialogId);
         availability.save(compound);
         return compound;
      }

      @Override
      public String toString() { return "OptionDialogID: " + dialogId + "; " + availability.toString(); }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) { return true; }
         if (obj instanceof OptionDialogID option) {
            return dialogId == option.dialogId && availability.equals(option.availability);
         }
         return false;
      }

   }

   @DatabaseColumn(name = "text", type = DatabaseColumn.Type.TEXT)
   public String title = "Talk";
   @DatabaseColumn(name = "color", type = DatabaseColumn.Type.SMALLINT)
   public int optionColor = 0xE0E0E0;
   @DatabaseColumn(name = "command", type = DatabaseColumn.Type.TEXT)
   public String command = "";
   @DatabaseColumn(name = "order", type = DatabaseColumn.Type.SMALLINT)
   public int slot = -1;

   /* OLD
   @DatabaseColumn(name = "id", type = DatabaseColumn.Type.INT)
   public int id = -1;
   @DatabaseColumn(name = "option", type = DatabaseColumn.Type.VARCHAR)
   public String option = "Talk";*/

   // New from Unofficial (BetaZavr)
   public int iconId = 0;
   public OptionType optionType = OptionType.DIALOG_OPTION;
   public final List<OptionDialogID> dialogs = new ArrayList<>();

   public void load(CompoundTag compound) {
      if (compound != null) {
         title = compound.getString("Title");
         optionColor = compound.getInt("DialogColor");
         command = compound.getString("DialogCommand");
         if (optionColor == 0) { optionColor = 0xE0E0E0; }

         // New from Unofficial (BetaZavr)
         optionType = OptionType.get(compound.getInt("OptionType"));
         iconId = compound.getInt("IconId");
         dialogs.clear();
         if (compound.contains("Dialog", 3)) { // OLD
            dialogs.add(new OptionDialogID(compound.getInt("Dialog")));
         } else if (compound.contains("Dialogs", 9)) {
            for (int i = 0; i < compound.getList("Dialogs", 10).size(); i++) {
               dialogs.add(new OptionDialogID(compound.getList("Dialogs", 10).getCompound(i)));
            }
         }
      }
   }

   public CompoundTag save() {
      CompoundTag compound = new CompoundTag();
      compound.putString("Title", title);
      compound.putInt("DialogColor", optionColor);
      compound.putString("DialogCommand", command);

      // New from Unofficial (BetaZavr)
      compound.putInt("OptionType", optionType.get());
      compound.putInt("IconId", iconId);
      ListTag list = new ListTag();
      for (OptionDialogID od : dialogs) { list.add(od.getNBT()); }
      compound.put("Dialogs", list);

      return compound;
   }

   public boolean hasDialogs() { return !dialogs.isEmpty() && optionType == OptionType.DIALOG_OPTION; }

   public @Nullable Dialog getDialog(Player player) {
      if (!hasDialogs() || player == null) { return null; }
      DialogController dData = DialogController.instance;
      for (OptionDialogID od : dialogs) {
         Dialog dialog = dData.get(od.dialogId);
         if (dialog != null && od.availability.isAvailable(player)) { return dialog; }
      }
      return null;
   }

   public boolean isAvailable(Player player) {
      if (optionType == OptionType.DISABLED) { return false; }
      if (optionType != OptionType.DIALOG_OPTION) { return true; }
      Dialog dialog = getDialog(player);
      return dialog != null && dialog.availability.isAvailable(player);
   }

   public int getOptionAvailable(Player player) {
      if (optionType == OptionType.DISABLED) { return 2; }
      if (!hasDialogs() || player == null) { return 0; }
      DialogController dData = DialogController.instance;
      for (OptionDialogID od : dialogs) {
         if (dData.hasDialog(od.dialogId) && od.availability.isAvailable(player)) {
            Dialog dialog = dData.get(od.dialogId);
            if (dialog != null && dialog.availability.isAvailable(player)) {
               return od.availability.isAvailable(player) ? 0 : 1;
            }
         }
      }
      return 2;
   }

   @Override
   public int getSlot() { return slot; }

   @Override
   public String getName() { return title; }

   @Override
   public int getType() { return optionType.get(); }

   // New from Unofficial (BetaZavr)
   public void replaceDialogIDs(int oldId, int newId) {
      List<OptionDialogID> newDialogs = new ArrayList<>();
      boolean added = false;
      for (OptionDialogID od : dialogs) {
         if (od.dialogId == oldId) {
            od.dialogId = newId;
            added = true;
         }
         newDialogs.add(od);
      }
      if (added) {
         dialogs.clear();
         dialogs.addAll(newDialogs);
      }
   }

   public void upPos(int dialogId) {
      List<OptionDialogID> newDialogs = new ArrayList<>();
      boolean added = false;
      OptionDialogID old = null;
      for (OptionDialogID od : dialogs) {
         if (od.dialogId == dialogId && old != null) {
            newDialogs.remove(old);
            newDialogs.add(od);
            newDialogs.add(old);
            added = true;
            continue;
         }
         old = od;
         newDialogs.add(od);
      }
      if (added) {
         dialogs.clear();
         dialogs.addAll(newDialogs);
      }
   }

   public void downPos(int dialogId) {
      List<OptionDialogID> newDialogs = new ArrayList<>();
      boolean added = false;
      OptionDialogID found = null;
      for (OptionDialogID od : dialogs) {
         if (od.dialogId == dialogId && found == null) {
            found = od;
            continue;
         }
         newDialogs.add(od);
         if (found != null && !added) {
            newDialogs.add(found);
            added = true;
         }
      }
      if (found != null && !added) {
         newDialogs.add(found);
         added = true;
      }
      if (added) {
         dialogs.clear();
         dialogs.addAll(newDialogs);
      }
   }

   public void addDialog(int dialogId) {
      OptionDialogID od = new OptionDialogID(dialogId);
      dialogs.add(od);
   }

   public DialogOption copy() {
      DialogOption newDO = new DialogOption();
      newDO.load(save());
      return newDO;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) { return true; }
      if (obj instanceof DialogOption option) {
         if (dialogs.size() != option.dialogs.size()) { return false; }
         for (int i = 0; i < dialogs.size(); i++) {
            if (!dialogs.get(i).equals(option.dialogs.get(i))) { return false; }
         }
         return title.equals(option.title) && command.equals(option.command) &&
                 optionColor == option.optionColor && iconId == option.iconId &&
                 optionType == option.optionType;
      }
      return false;
   }

}
