package noppes.npcs.controllers.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.api.constants.OptionType;
import noppes.npcs.api.handler.data.IDialogOption;
import noppes.npcs.controllers.DialogController;

import javax.annotation.Nullable;

public class DialogOption implements IDialogOption {

	public static class OptionDialogID {

		public int dialogId;
		public Availability availability;

		public OptionDialogID(int id) {
			dialogId = id;
			availability = new Availability();
		}

		public OptionDialogID(NBTTagCompound compound) {
			dialogId = compound.getInteger("DialogId");
			availability = new Availability();
			availability.load(compound);
		}

		public NBTTagCompound getNBT() {
			NBTTagCompound compound = new NBTTagCompound();
			compound.setInteger("DialogId", dialogId);
			availability.save(compound);
			return compound;
		}

		public String toString() { return "OptionDialogID: " + dialogId + "; " + availability.toString(); }

	}

	public String title = "Talk";
	public int optionColor = 0xE0E0E0;
	public String command = "";
	public int slot = -1;

	/* OLD
	public int id = -1;
	public String option = "Talk";*/

	// New from Unofficial (BetaZavr)

	public int iconId = 0;
	public OptionType optionType = OptionType.DIALOG_OPTION;
	public final List<OptionDialogID> dialogs = new ArrayList<>();

	public void load(NBTTagCompound compound) {
		if (compound != null) {
			title = compound.getString("Title");
			optionColor = compound.getInteger("DialogColor");
			command = compound.getString("DialogCommand");
			if (optionColor == 0) { optionColor = 0xE0E0E0; }

			// New from Unofficial (BetaZavr)
			optionType = OptionType.get(compound.getInteger("OptionType"));
			iconId = compound.getInteger("IconId");
			dialogs.clear();
			if (compound.hasKey("Dialog", 3)) { // OLD
				dialogs.add(new OptionDialogID(compound.getInteger("Dialog")));
			} else if (compound.hasKey("Dialogs", 9)) {
				for (int i = 0; i < compound.getTagList("Dialogs", 10).tagCount(); i++) {
					dialogs.add(new OptionDialogID(compound.getTagList("Dialogs", 10).getCompoundTagAt(i)));
				}
			}
		}
	}

	public NBTTagCompound save() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setString("Title", title);
		compound.setInteger("DialogColor", optionColor);
		compound.setString("DialogCommand", command);

		// New from Unofficial (BetaZavr)
		compound.setInteger("OptionType", optionType.get());
		compound.setInteger("IconId", iconId);
		NBTTagList list = new NBTTagList();
		for (OptionDialogID od : dialogs) { list.appendTag(od.getNBT()); }
		compound.setTag("Dialogs", list);
		return compound;
	}

	public boolean hasDialogs() { return !dialogs.isEmpty() && optionType == OptionType.DIALOG_OPTION; }

	public @Nullable Dialog getDialog(EntityPlayer player) {
		if (!hasDialogs() || player == null) { return null; }
		DialogController dData = DialogController.instance;
		for (OptionDialogID od : dialogs) {
			Dialog dialog = dData.get(od.dialogId);
			if (dialog != null && od.availability.isAvailable(player)) { return dialog; }
		}
		return null;
	}

	public boolean isAvailable(EntityPlayer player) {
		if (optionType == OptionType.DISABLED) { return false; }
		if (!hasDialogs() || player == null) { return true; }
		Dialog dialog = getDialog(player); // inside there is a check for availability of OptionDialogID
		return dialog != null && dialog.availability.isAvailable(player);
	}

	public int getOptionAvailable(EntityPlayer player) {
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

	// New from BetaZavr
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

}
