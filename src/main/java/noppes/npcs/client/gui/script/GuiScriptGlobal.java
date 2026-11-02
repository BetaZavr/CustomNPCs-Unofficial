package noppes.npcs.client.gui.script;

import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketOpenEditClientScript;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

public class GuiScriptGlobal extends GuiNPCInterface {

	protected final ResourceLocation resource = getResource("smallbg.png");

	public GuiScriptGlobal() {
		super();
		setBackground("smallbg.png");
		imageWidth = 176;
		imageHeight = 222;
	}

	@Override
	public void initGui() {
		super.initGui();
		for (int i = 0; i < 5; i++) {
			GuiButtonNop button = addButton(i, guiLeft + 38, guiTop + 20 + i * 30, Component.empty())
					.setSize(100, 20);
			switch (i) {
				case 1: button.setHoverTexts("script.hover.npcs");button.setDisplayText("NPC"); break;
				case 2: button.setHoverTexts("script.hover.forge").setDisplayText("Forge"); break;
				case 3: button.setHoverTexts("script.hover.potion").setDisplayText("gui.help.potions"); break;
				case 4: button.setHoverTexts("script.hover.client").setDisplayText("gui.client"); break;
				default: button.setHoverTexts("script.hover.players").setDisplayText("playerdata.players"); break;
			}
		}
	}

	@Override
	public void buttonEvent(GuiButtonNop guiButton) {
		switch (guiButton.id) {
			case 1: setScreen(new GuiScriptNPCs()); break;
			case 2: setScreen(new GuiScriptForge()); break;
			case 3: setScreen(new GuiScriptPotion()); break;
			case 4: Packets.sendServer(new SPacketOpenEditClientScript()); break;
			default: setScreen(new GuiScriptPlayers());
		}
	}

}
