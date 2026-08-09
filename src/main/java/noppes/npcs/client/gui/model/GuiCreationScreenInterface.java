package noppes.npcs.client.gui.model;

import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import noppes.npcs.api.event.ClientEvent;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.containers.ContainerLayer;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiSliderNop;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.mainmenu.GuiNpcDisplay;
import noppes.npcs.client.gui.model.GuiCreationParts.GuiPartEyes;
import noppes.npcs.client.model.part.ModelData;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

import javax.annotation.Nonnull;

public abstract class GuiCreationScreenInterface<C extends ContainerLayer> extends GuiContainerNPCInterface<C>
		implements ISliderListener {

	protected static float rotation = 0.5f;
	protected final boolean saving = false;
	protected boolean hasSaving = true;
	protected NBTTagCompound original;
	public static String Message = "";
	public EntityLivingBase entity;
	public EntityLivingBase showEntity;
	public ModelData playerdata;
	public int active = 0;
	public int xOffset = 140;

	public GuiCreationScreenInterface(EntityNPCInterface npc, C container) {
		super(npc, container, Component.empty());
		xSize = 400;
		ySize = 240;

		playerdata = ((EntityCustomNpc) npc).modelData;
		original = playerdata.save();
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		switch (button.id) {
			case 1: openGui(new GuiCreationEntities(npc, (ContainerLayer) inventorySlots)); break;
			case 2: {
				if (entity == null) { openGui(new GuiCreationParts(npc, (ContainerLayer) inventorySlots)); }
				else { openGui(new GuiCreationExtra(npc, (ContainerLayer) inventorySlots)); }
				break;
			}
			case 3: openGui(new GuiCreationScale(npc, (ContainerLayer) inventorySlots)); break;
			case 4: setSubGui(new SubGuiPresetSave(this, playerdata)); break;
			case 5: openGui(new GuiCreationLoad(npc, (ContainerLayer) inventorySlots)); break;
			case 6: openGui(new GuiCreationLayers(npc, (ContainerLayer) inventorySlots)); break;
			case 66: save(); NoppesUtil.openGUI(player, new GuiNpcDisplay(npc)); break;
		}
	}

	@Override
	public boolean keyPressed(char typedChar, int keyCode) {
		boolean bo = super.keyPressed(typedChar, keyCode);
		if (keyCode == Keyboard.KEY_ESCAPE) {
			NoppesUtil.openGUI(player, new GuiNpcDisplay(npc));
			return true;
		}
		return bo;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		drawPreview(mouseX, mouseY);
	}

	protected void drawPreview(int mouseX, int mouseY) {
		entity = playerdata.getEntity(npc);
		showEntity = entity;
		if (showEntity == null && npc != null || (showEntity == npc)) {
			showEntity = Util.instance.copyToGUI(npc, mc.world, false);
			((EntityNPCInterface) showEntity).display.setSize(5);
		}
		else { EntityUtil.Copy(npc, showEntity); }
		if (hasSubGui()) { return; }
		if (showEntity instanceof EntityNPCInterface) {
			if (showEntity.equals(npc)) {
				NBTTagCompound npcNbt = new NBTTagCompound();
				npc.writeEntityToNBT(npcNbt);
				npc.writeToNBTOptional(npcNbt);
				Entity e = EntityList.createEntityFromNBT(npcNbt, mc.world);
				if (!(e instanceof EntityNPCInterface)) {
					e = EntityList.createEntityByIDFromName(new ResourceLocation(CustomNpcs.MODID, "customnpc"), mc.world);
					if (e instanceof EntityNPCInterface) { e.readFromNBT(npcNbt); }
				}
				if (e instanceof EntityNPCInterface) {
					showEntity = (EntityNPCInterface) e;
				}
			}
			EntityNPCInterface npcIn = (EntityNPCInterface) showEntity;
			npcIn.ais.setStandingType(1);
			npcIn.ticksExisted = npc.ticksExisted;
			if (npcIn instanceof EntityCustomNpc && npc instanceof EntityCustomNpc
					&& ((EntityCustomNpc) npcIn).modelData != null
					&& ((EntityCustomNpc) npc).modelData != null) {
				((EntityCustomNpc) npcIn).modelData.entity = ((EntityCustomNpc) npc).modelData.entity;
			}
			npcIn.rotationYaw = 0;
			npcIn.prevRotationYaw = 0;
			npcIn.rotationYawHead = 0;
			npcIn.rotationPitch = 0;
			npcIn.prevRotationPitch = 0;
			npcIn.ais.orientation = 0;
			if (this instanceof GuiCreationParts && ((GuiCreationParts) this).getPart() instanceof GuiPartEyes) {
				npcIn.lookPos[0] = ValueUtil.correctInt(mouseX - guiLeft - 350, -45, 45);
				npcIn.lookPos[1] = ValueUtil.correctInt((mouseY - guiTop - 135) * -1, -45, 45);
			} else {
				npcIn.lookPos[0] = ValueUtil.correctInt(mouseX - guiLeft - xOffset - 200, -45, 45);
				npcIn.lookPos[1] = ValueUtil.correctInt((mouseY - guiTop - 100) * -1, -45, 45);
			}
			npcIn.display.setShowName(1);
			MarkData.get(npcIn).marks.clear();
		}
		if (this instanceof GuiCreationParts && ((GuiCreationParts) this).getPart() instanceof GuiPartEyes) {
			showEntity.ticksExisted = player.ticksExisted;
			int scale = new ScaledResolution(mc).getScaleFactor();
			GL11.glEnable(GL11.GL_SCISSOR_TEST);
			GL11.glScissor(guiLeft * scale, mc.displayHeight - (guiTop + ySize) * scale, xSize * scale, ySize * scale);
			drawNpc(showEntity, xOffset + 210, 425, 6.0f, 0, 0, 1);
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
		}
		else {
			drawNpc(showEntity, xOffset + 200, 200, 2.0f, (int) (GuiCreationScreenInterface.rotation * 360.0f - 180.0f),
					0,  this instanceof GuiCreationScale ? 1 : 0);
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		entity = playerdata.getEntity(npc);
		Keyboard.enableRepeatEvents(true);
		addButton(1, guiLeft + 62, guiTop, "gui.entity")
				.setSize(60, 20)
				.setHoverTexts("display.hover.part.entity");
		addButton(6, guiLeft, guiTop, "gui.layers")
				.setSize(60, 20)
				.setHoverTexts("display.hover.part.layers");
		if (entity == null) {
			addButton(2, guiLeft, guiTop + 23, "gui.parts")
					.setSize(60, 20)
					.setHoverTexts("display.hover.extra");
		}
		else if (!(entity instanceof EntityNPCInterface)) {
			GuiCreationExtra gui = new GuiCreationExtra(npc, (ContainerLayer) inventorySlots);
			gui.playerdata = playerdata;
			if (!gui.getData(entity).isEmpty()) {
				addButton(2, guiLeft, guiTop + 23, "gui.extra")
						.setSize(60, 20)
						.setHoverTexts("display.hover.parts");
			}
			else if (active == 2) {
				openGui(new GuiCreationEntities(npc, (ContainerLayer) inventorySlots));
				return;
			}
		}
		if (entity == null) {
			addButton(3, guiLeft + 62, guiTop + 23, "gui.scale")
					.setSize(60, 20)
					.setHoverTexts("display.hover.part.size");
		}
		if (hasSaving) {
			addButton(4, guiLeft, guiTop + ySize - 24, "gui.save")
					.setSize(60, 20)
					.setHoverTexts("display.hover.part.save");
			addButton(5, guiLeft + 62, guiTop + ySize - 24, "gui.load")
					.setSize(60, 20)
					.setHoverTexts("display.hover.part.load");
		}
		if (getButton(active) == null) {
			openGui(new GuiCreationEntities(npc, (ContainerLayer) inventorySlots));
			return;
		}
		getButton(active).setIsEnabled(false);
		addButton(66, guiLeft + xSize - 20, guiTop, "X")
				.setSize(20, 20)
				.setHoverTexts("hover.back");
		addLabel(0, guiLeft + 120, guiTop + ySize - 10, GuiCreationScreenInterface.Message)
				.setColor(0xFF0000)
				.setCenter(xSize - 120);
		addSlider(500, guiLeft + xOffset + 142, guiTop + 210, GuiCreationScreenInterface.rotation)
				.setSize(120, 20)
				.setHoverTexts("display.hover.part.rotate");
		if (showEntity instanceof EntityCustomNpc && playerdata != null){
			((EntityCustomNpc) showEntity).modelData.load(playerdata.save());
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (!saving) { return super.mouseClicked(mouseX, mouseY, mouseButton); }
		return false;
	}

	@Override
	public void mouseDragged(GuiSliderNop slider) {
		if (slider.id == 500) {
			GuiCreationScreenInterface.rotation = slider.sliderValue;
			slider.setString(Math.round((GuiCreationScreenInterface.rotation * 3600.0f)) / 10.0f);
		}
	}

	@Override
	public void mousePressed(GuiSliderNop slider) { }

	@Override
	public void mouseReleased(GuiSliderNop slider) { }

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
		super.onGuiClosed();
	}

	public void openGui(GuiScreen gui) {
		ClientEvent.NextToGuiCustomNpcs event = new ClientEvent.NextToGuiCustomNpcs(npc, this, gui);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.returnGui == null || event.isCanceled()) { return; }
		mc.displayGuiScreen(event.returnGui);
		if (mc.currentScreen == null) { mc.setIngameFocus(); }
	}

	@Override
	public void save() {
		Packets.sendServer(new SPacketMenuSave(EnumMenuType.DISPLAY, npc.display.save(new NBTTagCompound())));
		Packets.sendServer(new SPacketMenuSave(EnumMenuType.MODEL, playerdata.save()));
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		initGui();
	}

}
