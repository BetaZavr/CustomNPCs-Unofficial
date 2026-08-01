package noppes.npcs.client.gui.select;

import java.awt.*;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SubGuiTextureSelection extends ResourceSelection {

	protected final int type;
	protected final Entity displayEntity;
	public static boolean dark = false;
	protected TextureSelection textureSelection;

	/**
	 * @param parentIn parent Gui
	 * @param idIn id sub-gui
	 * @param npcIn customizable npc
	 * @param textureIn initial texture
	 * @param suffixIn file index
	 * @param typeIn 0:skin npc; 1:cape npc; 2:overlay npc; 3:any; 4:faction flag
	 */
	public SubGuiTextureSelection(GuiScreen parentIn, int idIn, @Nullable EntityNPCInterface npcIn, @Nonnull String textureIn, String suffixIn, int typeIn) {
		super(parentIn, idIn, npcIn, textureIn, suffixIn);
		scrollWidth = 258;

		if (npcIn != null) { displayEntity = Util.instance.copyToGUI(npcIn, npcIn.world, false); }
		else { displayEntity = null; }
		type = typeIn;
		if (textureIn.isEmpty() && selectDir == null) {
			switch (type) {
				case 1: selectDir = new ResourceLocation(CustomNpcs.MODID, "textures/cloak"); break;
				case 2: selectDir = new ResourceLocation(CustomNpcs.MODID, "textures/overlays"); break;
				case 3: selectDir = new ResourceLocation(CustomNpcs.MODID, "textures/gui"); break;
				default: selectDir = new ResourceLocation(CustomNpcs.MODID, "textures/entity/humanmale"); break;
			}
		}
	}

	@SuppressWarnings("unused")
	public SubGuiTextureSelection(GuiScreen parentIn, int idIn, EntityNPCInterface npc, @Nonnull String textureIn, String suffixIn, int typeIn, TextureSelection textureSelectionIn) {
		this(parentIn, idIn, npc, textureIn, suffixIn, typeIn);
		textureSelection = textureSelectionIn;
	}

	@Override
	public void initGui() {
		super.initGui();
		addCheckBox(3, guiLeft + scrollWidth - 7, guiTop + 5, null, null, dark)
				.setSize(12, 12)
				.setHoverTexts("texture.hover.dark");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		if (button.id == 3) {
			dark = ((GuiCheckBoxNop) button).selected();
			return;
		}
		if (button.id == 2 && npc != null && type >= 0 && type <= 2 && resource != null) {
			applyTexture(resource.toString());
			npc.textureLocation = null;
		}
		super.buttonEvent(button);
		if (button.id != 1 && button.id != 2 && button.id != 66) { onClose(); }
		if (wrapper.parent instanceof GuiScreen) {
			wrapper.parent.initGui();
		}
	}

	protected void applyTexture(String res) {
		switch (type) {
			case 1: {
				npc.display.setCapeTexture(res);
				if (displayEntity instanceof EntityNPCInterface) { ((EntityNPCInterface) displayEntity).display.setCapeTexture(res); }
				break;
			}
			case 2: {
				npc.display.setOverlayTexture(res);
				if (displayEntity instanceof EntityNPCInterface) { ((EntityNPCInterface) displayEntity).display.setOverlayTexture(res); }
				break;
			}
			default: {
				npc.display.setSkinTexture(res);
				if (displayEntity instanceof EntityNPCInterface) { ((EntityNPCInterface) displayEntity).display.setSkinTexture(res); }
				break;
			}
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		// background
		GlStateManager.pushMatrix();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		int x = guiLeft + 266;
		int y = guiTop + 6;
		int w = 80;
		if (type == 4) { // faction flag
			GlStateManager.translate(19.5f, 0.0f, 0.0f);
			w = 41;
		}
		drawRect(x - 1, y - 1, x + w + 1, y + 81, dark ?
				new Color(0xFFE0E0E0).getRGB() :
				new Color(0xFF202020).getRGB());
		drawRect(x, y, x + w, y + 80, dark ?
				new Color(0xFF000000).getRGB() :
				new Color(0xFFFFFFFF).getRGB());
		int g = 5;
		for (int u = 0; u < w / g; u++) {
			for (int v = 0; v < 80 / g; v++) {
				if (u % 2 == (v % 2 == 0 ? 1 : 0)) {
					drawRect(x + u * g, y + v * g, x + u * g + g, y + v * g + g, dark ?
							new Color(0xFF343434).getRGB() :
							new Color(0xFFCCCCCC).getRGB());
				}
			}
		}
		GlStateManager.popMatrix();
		// texture
		if (resource != null) {
			float scale = 80.0f / 256.0f;
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.translate(x, y, 0.0f);
			GlStateManager.scale(scale, scale, 1.0f);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			try {
				int tX = 0;
				int tY = 0;
				int tW = 256;
				int tH = 256;
				int tS = 256;
				if (type == 4) { // faction flag
					GlStateManager.translate(62.0f, 0.0f, 0.0f);
					GlStateManager.scale(3.3f, 2.0f, 1.0f);
					tX = 4;
					tY = 4;
					tW = 40;
					tH = 128;
				}
				GuiNpcUtil.drawTexturedModalRect(resource, tX, tY, tW, tH, tS);
			} catch (Exception e) { LogWriter.error("Error:", e); }
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		}
		if (npc != null && displayEntity != null && type >= 0 && type <= 2) {
			if (type == 0) {
				npc.textureLocation = resource;
				if (displayEntity instanceof EntityNPCInterface) { ((EntityNPCInterface) displayEntity).textureLocation = resource; }
			}
			int rot;
			float s = 1.25f;
			int mouse = 1;
			if (type == 0) { rot = minecraft.world == null ? 0 : (int) (3 * minecraft.world.getTotalWorldTime() % 360); }
			else if (type == 1) { rot = 215; }
			else { rot = 325; }
			if (npc.textureLocation != null) { drawNpc(displayEntity, x - guiLeft + w / 2, 178, s, rot, 0, mouse); }
		}
	}

    @Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		super.scrollClicked(scroll);
		if (!scroll.getNormalSelected().equals(back) && selectDir != null && selectedName(scroll).toLowerCase().endsWith(suffix)) {
			if (npc != null && type >= 0 && type <= 2 && resource != null) {
				applyTexture(resource.toString());
				npc.textureLocation = null;
			}
		}
	}

	// New from Unofficial (BetaZavr)
	@Override
	protected void cancel() {
		super.cancel();
		if (npc != null && type >= 0 && type <= 2) {
			switch (type) {
				case 1: {
					npc.display.setCapeTexture(baseResource);
					if (displayEntity instanceof EntityNPCInterface) { ((EntityNPCInterface) displayEntity).display.setCapeTexture(baseResource); }
					break;
				}
				case 2: {
					npc.display.setOverlayTexture(baseResource);
					if (displayEntity instanceof EntityNPCInterface) { ((EntityNPCInterface) displayEntity).display.setOverlayTexture(baseResource); }
					break;
				}
				default: {
					npc.display.setSkinTexture(baseResource);
					if (displayEntity instanceof EntityNPCInterface) { ((EntityNPCInterface) displayEntity).display.setSkinTexture(baseResource); }
					break;
				}
			}
			npc.textureLocation = null;
		}
	}

	@Override
	public void save() {
		if (textureSelection != null) { textureSelection.save(this); }
	}

	public interface TextureSelection {  void save(SubGuiTextureSelection subGuiTexture); }

}
