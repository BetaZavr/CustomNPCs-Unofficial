package noppes.npcs.client.gui.script;

import net.minecraft.nbt.CompoundTag;
import noppes.npcs.controllers.data.ClientScriptData;

public class GuiScriptClient extends GuiScriptInterface {

    protected final ClientScriptData script = new ClientScriptData();

    public GuiScriptClient() {
        super(6);
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
