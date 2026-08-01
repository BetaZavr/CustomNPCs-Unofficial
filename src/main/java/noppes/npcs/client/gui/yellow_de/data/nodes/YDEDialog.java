package noppes.npcs.client.gui.yellow_de.data.nodes;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.yellow_de.data.EnumYDEType;
import noppes.npcs.client.gui.yellow_de.data.YDEData;
import noppes.npcs.client.gui.yellow_de.data.YDENode;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;

import java.util.ArrayList;

public class YDEDialog extends YDENode {

    public int dialogId;
    public Dialog dialog;

    public YDEDialog(YDEData parent, int idIn, String categoryIn, int dialogIdIn) {
        super(parent);
        type = EnumYDEType.DIALOG;

        id = idIn;
        category = categoryIn;
        dialogId = dialogIdIn;
        dialog = DialogController.instance.get(dialogId);
    }

    @Override
    public void load(NBTTagCompound compound) {
        super.load(compound);
        type = EnumYDEType.DIALOG;
        dialogId = compound.getInteger("DialogID");
        dialog = DialogController.instance.get(dialogId);
    }

    @Override
    public NBTTagCompound save() {
        NBTTagCompound compound = super.save();
        compound.setInteger("DialogID", dialogId);
        return compound;
    }

    @Override
    public Component getTitle() {
        if (dialog == null && dialogId > -1) { dialog = DialogController.instance.get(dialogId); }
        else if (dialog != null && dialog.id == -1) {
            for (Dialog d : new ArrayList<>(DialogController.instance.dialogs.values())) {
                if (d.title.equals(dialog.title)) {
                    dialog = d;
                    break;
                }
            }
        }
        if (id < 0) {
            parent.nodes.remove(-1);
            id = parent.getEmptyNodeId();
            parent.nodes.put(id, this);
        }
        dialogId = dialog != null ? dialog.id : -1;
        return Component.translatable("dialog.dialog").append("ID: " + dialogId);
    }

}