package noppes.npcs.client.gui.mainmenu;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.availability.SubGuiNpcAvailability;
import noppes.npcs.client.gui.SubGuiNpcName;
import noppes.npcs.client.gui.select.SubGuiTextureSelection;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.client.model.part.ModelData;
import noppes.npcs.client.model.part.ModelPartConfig;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuGet;
import noppes.npcs.packets.server.SPacketMenuSave;
import noppes.npcs.packets.server.SPacketNpRandomNameSet;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import java.awt.*;
import java.text.DecimalFormat;

public class GuiNpcDisplay extends GuiNPCInterface2 implements ITextfieldListener, IGuiData {

	protected final DataDisplay display;

	// New from Unofficial (BetaZavr)
	protected float baseHitBoxWidth;
	protected float baseHitBoxHeight;
	protected final DecimalFormat df = new DecimalFormat("#.#");
	
	public GuiNpcDisplay(EntityNPCInterface npc) {
		super(npc, 1);
		backGui = null;

		display = npc.display;
		baseHitBoxWidth = npc.baseWidth;
		baseHitBoxHeight = npc.baseHeight;
		if (npc instanceof EntityCustomNpc && display.getModel() != null) {
			EntityLivingBase model = ((EntityCustomNpc) npc).modelData.getEntity(npc);
			if (model != null) {
				baseHitBoxWidth = model.width;
				baseHitBoxHeight = model.height;
			}
		}
		Packets.sendServer(new SPacketMenuGet(EnumMenuType.DISPLAY));
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: display.setShowName(button.getValue()); break;
			case 1: NoppesUtil.requestOpenGUI(EnumGuiType.CreationParts); break;
			case 2: {
				display.setSkinUrl("");
				display.setSkinPlayer(null);
				display.skinType = (byte) button.getValue();
				initGui();
				break;
			}
			case 3: setSubGui(new SubGuiTextureSelection(this, 0, npc, npc.display.getSkinTexture(), ".png", 0)); break;
			case 4: display.setShadowType(button.getValue()); break;
			case 5: display.setHasLivingAnimation(button.getValue() == 0); break;
			case 7: display.setVisible(button.getValue()); initGui(); break;
			case 8: setSubGui(new SubGuiTextureSelection(this, 1, npc, npc.display.getCapeTexture(), ".png", 1)); break;
			case 9: setSubGui(new SubGuiTextureSelection(this, 1, npc, npc.display.getOverlayTexture(), ".png", 2)); break;
			case 10: display.setBossbar(button.getValue()); break;
			case 12: { display.setBossColor(button.getValue()); break; }
			case 13: display.setHitboxState((byte) button.getValue()); break;
			case 14: Packets.sendServer(new SPacketNpRandomNameSet(display.getMarkovGeneratorId(), display.getMarkovGender())); break;
			case 15: setSubGui(new SubGuiNpcName(display)); break;
			case 16: setSubGui(new SubGuiNpcAvailability(display.getAvailability(), this)); break;
			case 18: display.setOverlayGlowing(((GuiButtonYesNo) button).getBoolean()); break;
		}
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) { initGui(); }

	@Override
	public void initGui() {
		super.initGui();
		int lID = 0;
		int x0 = guiLeft + 5;
		int x1 = guiLeft + 50;
		int x2 = guiLeft + 260;
		int w = 43;
		int y = guiTop + 4;
		addLabel(lID++, x0, y + 5, "gui.name")
				.setSize(w, 10);
		addTextField(0, x1 + 1, y, 206, 20, display.getName())
				.setHoverTexts("display.hover.name");
		addButton(0, x2 + 46, y, false, display.getShowName(), "display.show", "display.hide", "display.showAttacking")
				.setSize(110, 20)
				.setHoverTexts("display.hover.show.name");
		addButton(14, x2, y, "↻")
				.setSize(20, 20)
				.setHoverTexts("display.hover.random.name");
		addButton(15, x2 + 22, y, "⋮")
				.setSize(20, 20)
				.setHoverTexts("display.hover.group.name");
		y += 23;
		addLabel(lID++, x0, y + 5, "gui.title")
				.setSize(w, 10);
		addTextField(11, x1 + 1, y, 206, 20, display.getTitle())
				.setHoverTexts("display.hover.subname");
		y += 23;
		addLabel(lID++, x0, y + 5, "display.model")
				.setSize(w, 10);
		addButton(1, x1, y, "selectServer.edit")
				.setSize(110, 20)
				.setHoverTexts("display.hover.set.model");
		int tw = font.getStringWidth(Component.translatable("display.size").getFormattedText());
		int x3 = x2 - 2 - tw;
		addLabel(lID++, x3, y + 5, "display.size")
				.setSize(tw + 1, 10);
		addTextField(2, x2 + 1, y, 40, 20, display.getSize())
				.setMinMaxDefault(1, 30, 5)
				.setHoverTexts("display.hover.size");
		addLabel(lID++, x2 + 43, y + 5, "(1-30)")
				.setSize(w, 10);
		y += 23;
		addLabel(lID++, x0, y + 5, "display.texture")
				.setSize(w, 10);
		addTextField(3, x1 + 1, y, 206, 20, display.skinType == 0 ? display.getSkinTexture() : display.skinType == 1 ? display.getSkinPlayer() : display.getSkinUrl())
				.setHoverTexts("display.hover.skin." + display.skinType);
		addButton(3, x2, y, "mco.template.button.select")
				.setSize(60, 20)
				.setIsEnabled(display.skinType == 0)
				.setHoverTexts("display.hover.texture.set");
		addButton(2, x2 + 62, y, false, display.skinType, "display.texture", "display.player", "display.url")
				.setSize(60, 20)
				.setHoverTexts("display.hover.texture.type." + display.skinType);
		y += 23;
		addLabel(lID++, x0, y + 5, "display.cape")
				.setSize(w, 10);
		addTextField(8, x1 + 1, y, 206, 20, display.getCapeTexture())
				.setHoverTexts("display.hover.cloak");
		addButton(8, x2, y, "display.selectTexture")
				.setSize(60, 20)
				.setHoverTexts("display.hover.texture.cloak");
		y += 23;
		addLabel(lID++, x0, y + 5, "display.overlay")
				.setSize(w, 10);
		addTextField(9, x1 + 1, y, 206, 20, display.getOverlayTexture())
				.setHoverTexts("display.hover.eyes");
		addButton(9, x2, y, "mco.template.button.select")
				.setSize(60, 20)
				.setHoverTexts("display.hover.texture.eyes");
		addLabel(lID++, x2 + 63, y + 5, "display.isglowing")
				.setSize(54, 10);
		addYesNo(18, x2 + 116, y, display.isOverlayGlowing())
				.setSize(40, 20)
				.setHoverTexts("display.hover.glint");
		y += 23;
		addLabel(lID++, x0, y + 5, "display.livingAnimation")
				.setSize(w, 10);
		addYesNo(5, x1, y, display.getHasLivingAnimation())
				.setSize(40, 20)
				.setHoverTexts("display.hover.animation");
		tw = font.getStringWidth(Component.translatable("display.tint").getFormattedText());
		x3 = x2 - 2 - tw;
		addLabel(lID++, x3, y + 5, "display.tint")
				.setSize(tw + 1, 10);
		StringBuilder color = new StringBuilder(Integer.toHexString(display.getTint()));
		while (color.length() < 6) { color.insert(0, "0"); }
		addTextField(6, x2 + 1, y, 60, 20, color.toString())
				.setColor(display.getTint())
				.setHoverTexts("display.hover.color");
		// New from Unofficial (BetaZavr)
		addLabel(lID++, x2 + 63, y + 5, "display.shadow")
				.setSize(w, 10);
		addButton(4, x2 + 116, y, false, display.getShadowType(),
				"0%", "50%", "100%", "150%")
				.setSize(40, 20)
				.setHoverTexts("display.hover.shadow");
		// Next normal
		y += 23;
		addLabel(lID++, x0, y + 5, "display.visible")
				.setSize(w, 10);
		addButton(7, x1, y, false, display.getVisible(), "gui.yes", "gui.no", "gui.partly")
				.setSize(60, 20)
				.setHoverTexts("display.hover.visible");
		addButton(16, x1 + 62, y,"availability.name")
				.setSize(60, 20)
				.setIsEnabled(CustomNpcs.EnableInvisibleNpcs && display.getVisible() == 1)
				.setHoverTexts("display.hover.visible." + (CustomNpcs.EnableInvisibleNpcs ? 1 : 0));
		tw = font.getStringWidth(Component.translatable("display.hitbox").getFormattedText());
		x3 = x2 - 2 - tw;
		addLabel(lID++, x3, y + 5, "display.hitbox")
				.setSize(tw + 1, 10);
		addButton(13, x2, y, true, display.getHitboxState(), "stats.normal", "gui.none", "hair.solid")
				.setSize(70, 20)
				.setHoverTexts("display.hover.interactable");
		// New from Unofficial (BetaZavr)
		addLabel(lID++, x2 + 72, y + 5, "W:")
				.setSize(12, 10);
		float hitBoxWidth = npc.width;
		float hitBoxHeight = npc.height;
		if (npc instanceof EntityCustomNpc) {
			float scaleHead = 1.0f, scaleBody = 1.0f;
			if (display.getModel() != null) {
				ModelData modeldata = ((EntityCustomNpc) npc).modelData;
				ModelPartConfig model = modeldata.getPartConfig(EnumParts.HEAD);
				scaleHead = Math.max(model.scaleX, model.scaleZ);
				model = modeldata.getPartConfig(EnumParts.BODY);
				scaleBody = Math.max(model.scaleX, model.scaleZ);
				EntityLivingBase modelEntity = modeldata.getEntity(npc);
				if (modelEntity != null) {
					hitBoxWidth = modelEntity.width;
					hitBoxHeight = modelEntity.height;
				}
			}
			hitBoxWidth *= Math.max(scaleHead, scaleBody);
			hitBoxWidth = hitBoxWidth / 5.0f * display.getSize();
			hitBoxHeight = hitBoxHeight / 5.0f * display.getSize();
		}
		addTextField(12, x2 + 81, y, 30, 20, df.format(display.width))
				.setMinMaxDefault(-1.0d, 7.5d, display.width)
				.setHoverTexts("display.hover.hitbox.width", ("" + Math.round(baseHitBoxWidth * 1000.0) / 1000.0).replace(".", ","), ("" + Math.round(hitBoxWidth * 1000.0) / 1000.0).replace(".", ","));
		addLabel(lID++, x2 + 115, y + 5, "H:")
				.setSize(12, 10);
		addTextField(13, x2 + 125, y, 30, 20, df.format(display.height))
				.setMinMaxDefault(-1.0d, 15.0d, display.height)
				.setHoverTexts("display.hover.hitbox.height", ("" + Math.round(baseHitBoxHeight * 1000.0) / 1000.0).replace(".", ","), ("" + Math.round(hitBoxHeight * 1000.0) / 1000.0).replace(".", ","));
		// Next normal
		y += 23;
		addLabel(lID++, x0, y + 5, "display.bossbar")
				.setSize(w, 10);
		addButton(10, x1, y, false, display.getBossbar(),
				"display.hide", "display.show", "display.showAttacking")
				.setHoverTexts("display.hover.boss.bar")
				.setSize(110, 20);
		tw = font.getStringWidth(Component.translatable("gui.color").getFormattedText());
		x3 = x2 - 2 - tw;
		addLabel(lID, x3, y + 5, "gui.color")
				.setSize(tw + 1, 10);
		addButton(12, x2, y, true, display.getBossColor(),
				"color.pink", "color.blue", "color.red", "color.green", "color.yellow", "color.purple", "color.white")
				.setSize(70, 20)
				.setHoverTexts("display.hover.bar.color");
	}

	@Override
	public void save() {
		if (display.skinType == 1) { display.loadProfile(); }
		npc.textureLocation = null;
		mc.renderGlobal.onEntityRemoved(npc);
		mc.renderGlobal.onEntityAdded(npc);
		Packets.sendServer(new SPacketMenuSave(EnumMenuType.DISPLAY, display.save(new NBTTagCompound())));
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		if (compound.hasKey("MarkovGeneratorId", 3)) {
			display.load(compound);
			baseHitBoxWidth = npc.baseWidth;
			baseHitBoxHeight = npc.baseHeight;
			initGui();
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textfield) {
		switch (textfield.id) {
			case 0: {
				if (!textfield.isEmpty()) { display.setName(textfield.getValue()); }
				textfield.setValue(display.getName());
				break;
			}
			case 2: display.setSize(textfield.getInteger()); break;
			case 3: {
				if (display.skinType == 2) { display.setSkinUrl(textfield.getValue()); }
				else if (display.skinType == 1) { display.setSkinPlayer(textfield.getValue()); }
				else { display.setSkinTexture(textfield.getValue()); }
				break;
			}
			case 6: {
				int color;
				try { color = Integer.parseInt(textfield.getValue(), 16); }
				catch (NumberFormatException e) { color = new Color(0xFFFFFF).getRGB(); }
				display.setTint(color);
				textfield.setTextColor(display.getTint());
				break;
			}
			case 8: display.setCapeTexture(textfield.getValue()); break;
			case 9: display.setOverlayTexture(textfield.getValue()); break;
			case 11: display.setTitle(textfield.getValue()); break;
			case 12: {
				float f = (float) textfield.getDouble();
				display.width = f < 0.0f ? baseHitBoxWidth : f;
				if (f < 0.0f) { textfield.setValue(df.format(display.width)); }
				break;
			}
			case 13: {
				float f = (float) textfield.getDouble();
				display.height = f < 0.0f ? baseHitBoxHeight : f;
				if (f < 0.0f) { textfield.setValue(df.format(display.height)); }
				break;
			}
		}
	}

}
