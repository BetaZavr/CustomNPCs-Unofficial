package noppes.npcs.client.gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCloneList;
import noppes.npcs.packets.server.SPacketCloneRemove;
import noppes.npcs.packets.server.SPacketGetServerCloneEntity;
import noppes.npcs.packets.server.SPacketToolMobSpawner;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiMenuTopButton;
import noppes.npcs.client.controllers.ClientCloneController;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import org.lwjgl.input.Keyboard;

public class GuiNpcMobSpawner extends GuiNPCInterface implements IGuiData, ICustomScrollListener {

	protected static int showingClones = 0;
	protected static int activeTab = 1;
	protected final BlockPos pos;
	protected final List<String> list = new ArrayList<>();
	protected GuiCustomScrollNop scroll;

	// New from Unofficial (BetaZavr)
	protected int sel = -1;
	public EntityLivingBase selectNpc;

	public GuiNpcMobSpawner(BlockPos posIn) {
		super();
		setBackground("menubg.png");
		imageWidth = 256;

		pos = posIn;
	}

	@Override
	public void initGui() {
		super.initGui();
		guiTop += 10;
		if (scroll == null) { scroll = addScroll(0).setSize(165, 210); }
		else { scroll.clear();}
		add(scroll.setPos(guiLeft + 4, guiTop + 4).setSelect(sel));
		// clones
		GuiMenuTopButton button = addTopButton(3, guiLeft + 4, guiTop - 17, "spawner.clones");
		button.active = showingClones == 0;
		// entities
		GuiMenuTopButton buttonEntities = addTopButton(4, button.getX() + button.getWidth(), button.getY(), "spawner.entities");
		buttonEntities.active = showingClones == 1;
		// server
		addTopButton(5, buttonEntities.getX() + buttonEntities.getWidth(), buttonEntities.getY(), "gui.server")
				.active = showingClones == 2;
		int x = guiLeft + 171;
		int y = guiTop + 6;
		addButton(1, x, y, "gui.spawn").setSize(82, 20);
		addButton(2, x, y + 142, "spawner.mobspawner")
				.setSize(82, 20);
		addButton(66, x, y + 164, "gui.done")
				.setSize(80, 20)
				.setHoverTexts("hover.exit");
		if (showingClones != 0 && showingClones != 2) { showEntities(); }
		else {
			for (int id = 0; id < 9; id++) {
				addSideButton(21 + id, guiLeft, guiTop + 3 + id * 21, Component.translatable("gui.tab").append(" " + (id + 1)))
						.active = activeTab == id + 1;
			}
			addButton(6, guiLeft + 170, guiTop + 30, "gui.remove")
					.setSize(82, 20);
			showClones();
		}
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		if (button.id > 20 && button.id < 31) {
			activeTab = button.id - 20;
			initGui();
			return;
		}
		switch (button.id) {
			case 1: {
				if (showingClones == 2) {
					String sel = scroll.getSelected();
					if (sel.isEmpty()) { return; }
					Packets.sendServer(new SPacketToolMobSpawner(true, false, pos, sel, activeTab, new NBTTagCompound()));
					onClose();
				} else {
					NBTTagCompound compound = getCompound();
					if (compound == null) { return; }
					Packets.sendServer(new SPacketToolMobSpawner(false, false, pos, "", 0, compound));
					onClose();
				}
				break;
			}
			case 2: {
				if (showingClones == 2) {
					String sel = scroll.getSelected();
					if (sel.isEmpty()) { return; }
					Packets.sendServer(new SPacketToolMobSpawner(true, true, pos, sel, activeTab, new NBTTagCompound()));
					onClose();
				} else {
					NBTTagCompound compound = getCompound();
					if (compound == null) { return; }
					Packets.sendServer(new SPacketToolMobSpawner(false, true, pos, "", 0, compound));
					onClose();
				}
				break;
			}
			case 3: selectNpc = null; showingClones = 0; initGui(); break;
			case 4: selectNpc = null; showingClones = 1; initGui(); break;
			case 5: selectNpc = null; showingClones = 2; initGui(); break;
			case 6: {
				if (scroll.hasSelected()) {
					String name = scroll.getSelected();
					if (!name.isEmpty()) {
						int next = scroll.getSelectedIndex() - 1;
						if (next < 0) { next = scroll.getList() == null || scroll.getList().size() <= 1 ? -1 : 0; }
						sel = next;
						selectNpc = null;
						scroll.clearSelection();
						if (showingClones == 2) {
							Packets.sendServer(new SPacketCloneRemove(name, activeTab));
							return;
						}
						ClientCloneController.Instance.removeClone(name, activeTab);
						initGui();
					}
				}
				break;
			} // delete
			case 66: onClose(); break;
		}
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		if (compound.hasKey("NPCData", 10)) {
			selectNpc = (EntityNPCInterface) EntityList.createEntityFromNBT(compound.getCompoundTag("NPCData"), player.world);
			return;
		}
		NBTTagList nbtList = compound.getTagList("List", 8);
		List<String> list = new ArrayList<>();
		for (int i = 0; i < nbtList.tagCount(); ++i) { list.add(nbtList.getStringTagAt(i)); }
		scroll.setList(list)
				.setSelect(sel);
	}

	private void showEntities() {
		list.clear();
		list.addAll(EntityUtil.getAllEntities(minecraft.world, false).keySet());
		scroll.setList(list).setSelect(sel);
	}

	private void showClones() {
		if (showingClones == 2) {
			Packets.sendServer(new SPacketCloneList(activeTab));
		} else {
			list.clear();
			list.addAll(ClientCloneController.Instance.getClones(activeTab));
			scroll.setList(list).setSelect(sel);
			resetEntity();
		}
	}

	// Client Clones or Vanilla Mobs
	private NBTTagCompound getCompound() {
		String sel = scroll.getSelected();
		if (sel.isEmpty()) { return null; }
		if (showingClones == 0) { return ClientCloneController.Instance.getCloneData(player, sel, activeTab); }
		// Vanilla Mobs
		if (minecraft != null && minecraft.world != null) {
			ResourceLocation loc = EntityUtil.getAllEntities(minecraft.world, false).get(sel);
			if (loc != null) {
				Entity entity = EntityList.createEntityByIDFromName(loc, minecraft.world);
				if (entity instanceof EntityLivingBase) {
					NBTTagCompound compound = new NBTTagCompound();
					entity.writeToNBTAtomically(compound);
					return compound;
				}
			}
		}
		return null;
	}

	// New from Unofficial (BetaZavr)
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (!hasSubGui() && player != null) {
			GlStateManager.pushMatrix();
			if (selectNpc != null) { drawNpc(selectNpc, 210, 130, 1.0f, (int) (3 * player.world.getTotalWorldTime() % 360), 0, 0); }
			GlStateManager.translate(0.0f, 0.0f, 1.0f);
			Gui.drawRect(guiLeft + 179, guiTop + 54, guiLeft + 242, guiTop + 142, new Color(0xFF808080).getRGB());
			Gui.drawRect(guiLeft + 180, guiTop + 55, guiLeft + 241, guiTop + 141, new Color(0xFF000000).getRGB());
			GlStateManager.popMatrix();
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean keyPressed(char typedChar, int keyCode) {
		if (!hasSubGui()) {
			if (keyCode == Keyboard.KEY_UP ||
					keyCode == Keyboard.KEY_DOWN ||
					keyCode == mc.gameSettings.keyBindForward.getKeyCode() ||
					keyCode == mc.gameSettings.keyBindBack.getKeyCode()) {
				resetEntity();
			}
		}
		return super.keyPressed(typedChar, keyCode);
	}

	private void resetEntity() {
		if (GuiNpcMobSpawner.showingClones == 0) { // client
			NBTTagCompound npcNbt = ClientCloneController.Instance.getCloneData(player, scroll.getSelected(), activeTab);
			if (npcNbt == null) { return; }
			Entity entity = EntityList.createEntityFromNBT(npcNbt, minecraft.world);
			if (entity instanceof EntityLivingBase) { selectNpc = (EntityLivingBase) entity; }
		}
		else if (GuiNpcMobSpawner.showingClones == 1) { // mob
			ResourceLocation loc = EntityUtil.getAllEntities(minecraft.world, false).get(scroll.getSelected());
			if (loc != null) {
				Entity entity = EntityList.createEntityByIDFromName(loc, minecraft.world);
				if (entity instanceof EntityLivingBase) { selectNpc = (EntityLivingBase) entity; }
			}
		}
		else { // server
			Packets.sendServer(new SPacketGetServerCloneEntity(false, false, activeTab, scroll.getSelected()));
		}
	}

    @Override
	public void scrollClicked(GuiCustomScrollNop scroll) { resetEntity(); }

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

}
