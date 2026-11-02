package noppes.npcs.client.gui.script;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.controllers.data.PlayerScriptData;

public class GuiScriptPlayers extends GuiScriptInterface {

	protected final PlayerScriptData script = new PlayerScriptData();

	public GuiScriptPlayers() {
		super(4);
		handler = script;
	}

	@Override
	public void save() {
		super.save();
		sendToServer(script.save(new NBTTagCompound()));
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		script.load(compound);
		super.setGuiData(compound);
	}

}
