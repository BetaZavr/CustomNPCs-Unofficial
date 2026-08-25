package noppes.npcs.client.gui.script;

import net.minecraft.nbt.CompoundTag;
import noppes.npcs.controllers.data.NpcScriptData;

public class GuiScriptPotion extends GuiScriptInterface {

    protected final NpcScriptData script = new NpcScriptData();

    public GuiScriptPotion() {
        super(7);
        handler = script;
    }

    @Override
    public void setGuiData(CompoundTag compound) {
        script.load(compound);
        super.setGuiData(compound);
    }

    @Override
    public void save() {
        super.save();
        sendToServer(script.save(new CompoundTag()));
    }

}
