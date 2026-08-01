package noppes.npcs.client.gui.roles;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.api.entity.data.role.IJobSpawner;
import noppes.npcs.client.gui.SubGuiNpcMobSpawnerSelector;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.roles.JobSpawner;
import noppes.npcs.roles.data.JobSpawnerNbtData;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import org.lwjgl.input.Keyboard;

// Changed by Unofficial (BetaZavr)
public class GuiNpcSpawner extends GuiNPCInterface2
		implements IGuiData, ICustomScrollListener, ITextfieldListener {

	protected final JobSpawner job;
	protected GuiCustomScrollNop deadScroll;
	protected GuiCustomScrollNop aliveScroll;
	protected Entity select;
	protected boolean isDead = false;
	protected int slot = -1;

	public GuiNpcSpawner(EntityNPCInterface npc) {
		super(npc);
		job = (JobSpawner) npc.job;
		backGui = EnumGuiType.MainMenuAdvanced;
	}

	@Override
	public void initGui() {
		super.initGui();
		if (aliveScroll == null) { aliveScroll = addScroll(1).setSize(172, 101); }
		if (!isDead) {
			if (slot >= 0 && slot < aliveScroll.getList().size()) { aliveScroll.setSelect(slot); }
			else {
				slot = -1;
				aliveScroll.setSelect(-1);
				select = null;
			}
		}
		else { aliveScroll.setSelect(-1); }
		add(aliveScroll.setPos(guiLeft + 5, guiTop + 14));
		if (deadScroll == null) { deadScroll = addScroll(0).setSize(172, 101); }
		if (isDead) {
			if (slot >= 0 && slot < deadScroll.getList().size()) { deadScroll.setSelect(slot); }
			else {
				slot = -1;
				deadScroll.setSelect(-1);
				select = null;
			}
		} else { deadScroll.setSelect(-1); }
		add(deadScroll.setPos(guiLeft + 180, guiTop + 14));
		addLabel(1, guiLeft + 6, guiTop + 4, "spawner.list.0")
				.setHoverTexts(Component.translatable("spawner.hover.list.0")
						.append(Component.translatable("spawner.hover.list.2")));
		addLabel(2, guiLeft + 182, guiTop + 4, "spawner.list.1")
				.setHoverTexts(Component.translatable("spawner.hover.list.1")
						.append(Component.translatable("spawner.hover.list.2")));
		// Alive
		Component strAlive = Component.translatable("spawner.hover.sp.0");
		// add
		addButton(1, guiLeft + 5, guiTop + 116, "gui.add")
				.setSize(56, 20)
				.setHoverTexts(Component.translatable("spawner.hover.add").append(strAlive));
		// del
		addButton(2, guiLeft + 63, guiTop + 116, "gui.remove")
				.setSize(56, 20)
				.setIsEnabled(!isDead && slot >= 0)
				.setHoverTexts(Component.translatable("spawner.hover.del").append(strAlive));
		// edit mode
		addButton(3, guiLeft + 121, guiTop + 116, "advanced.editingmode")
				.setSize(56, 20)
				.setIsEnabled(!isDead && slot >= 0)
				.setHoverTexts(Component.translatable("spawner.hover.change").append(strAlive));
		// up
		addButton(4, guiLeft + 5, guiTop + 138, "type.up")
				.setSize(56, 20)
				.setIsEnabled(!isDead && slot >= 0 && slot >= 1)
				.setHoverTexts(Component.translatable("spawner.hover.up").append(strAlive));
		// down
		addButton(5, guiLeft + 63, guiTop + 138, "type.down")
				.setSize(56, 20)
				.setIsEnabled(!isDead && slot >= 0 && slot < (aliveScroll.getList().size() - 1))
				.setHoverTexts(Component.translatable("spawner.hover.down").append(strAlive));
		// clear
		addButton(6, guiLeft + 121, guiTop + 138, "gui.clear")
				.setSize(56, 20)
				.setIsEnabled(!aliveScroll.getList().isEmpty())
				.setHoverTexts(Component.translatable("spawner.hover.clear").append(strAlive));
		addLabel(4, guiLeft + 6, guiTop + 166, "type.offset");
		addLabel(5, guiLeft + 44, guiTop + 166, "X:");
		int[] set = job.getOffset(false);
		if (set == null) { set = new int[] { 0, 0, 0 }; }
		if (set.length != 3) {
			int[] ns = new int[] { 0, 0, 0 };
			System.arraycopy(set, 0, ns, 0, set.length);
			set = ns;
		}
		addTextField(0, guiLeft + 52, guiTop + 161, 35, 15, "" + set[0])
				.setMinMaxDefault(0, 5, set[0])
				.setHoverTexts(Component.translatable("spawner.hover.axis.offset", "X").append(strAlive));
		addLabel(6, guiLeft + 89, guiTop + 166, "Y:");
		addTextField(1, guiLeft + 97, guiTop + 161, 35, 15, "" + set[1])
				.setMinMaxDefault(0, 5, set[1])
				.setHoverTexts(Component.translatable("spawner.hover.axis.offset", "Y").append(strAlive));
		addLabel(7, guiLeft + 134, guiTop + 166, "Z:");
		addTextField(2, guiLeft + 142, guiTop + 161, 35, 15, "" + set[2])
				.setMinMaxDefault(0, 5, set[2])
				.setHoverTexts(Component.translatable("spawner.hover.axis.offset", "Z").append(strAlive));
		addCheckBox(7, guiLeft + 5, guiTop + 176, "spawner.despawn", null, job.getDespawnOnTargetLost(false))
				.setSize(170, 14)
				.setHoverTexts(Component.translatable("spawner.hover.des.tr.lost").append(strAlive));
		addLabel(8, guiLeft + 5, guiTop + 195, "spawner.type");
		addButton(8, guiLeft + 63, guiTop + 190, false, job.getSpawnType(false), "spawner.one", "spawner.all", "spawner.random")
				.setSize(55, 20)
				.setHoverTexts(Component.translatable("spawner.hover.type.0."+job.getSpawnType(false)).append(strAlive));
		// Dead
		Component strDead = Component.translatable("spawner.hover.sp.1");
		// add
		addButton(9, guiLeft + 180, guiTop + 116, "gui.add")
				.setSize(56, 20)
				.setHoverTexts(Component.translatable("spawner.hover.add").append(strDead));
		// del
		addButton(10, guiLeft + 238, guiTop + 116, "gui.remove")
				.setSize(56, 20)
				.setIsEnabled(isDead && slot >= 0)
				.setHoverTexts(Component.translatable("spawner.hover.del").append(strDead));
		// edit mode
		addButton(11, guiLeft + 296, guiTop + 116, "advanced.editingmode")
				.setSize(56, 20)
				.setIsEnabled(isDead && slot >= 0)
				.setHoverTexts(Component.translatable("spawner.hover.change").append(strDead));
		// up
		addButton(12, guiLeft + 180, guiTop + 138, "type.up")
				.setSize(56, 20)
				.setIsEnabled(isDead && slot >= 0 && slot >= 1)
				.setHoverTexts(Component.translatable("spawner.hover.up").append(strDead));
		// down
		addButton(13, guiLeft + 238, guiTop + 138, "type.down")
				.setSize(56, 20)
				.setIsEnabled(isDead && slot >= 0 && slot < (deadScroll.getList().size() - 1))
				.setHoverTexts(Component.translatable("spawner.hover.down").append(strDead));
		// clear
		addButton(14, guiLeft + 296, guiTop + 138, "gui.clear")
				.setSize(56, 20)
				.setIsEnabled(!deadScroll.getList().isEmpty())
				.setHoverTexts(Component.translatable("spawner.hover.clear").append(strDead));
		addLabel(9, guiLeft + 181, guiTop + 166, "type.offset");
		addLabel(10, guiLeft + 219, guiTop + 166, "X:");
		set = job.getOffset(true);
		if (set == null) {
			set = new int[] { 0, 0, 0 };
		}
		if (set.length != 3) {
			int[] ns = new int[] { 0, 0, 0 };
			System.arraycopy(set, 0, ns, 0, set.length);
			set = ns;
		}
		addTextField(3, guiLeft + 227, guiTop + 161, 35, 15, "" + set[0])
				.setMinMaxDefault(0, 5, set[0])
				.setHoverTexts(Component.translatable("spawner.hover.axis.offset", "X").append(strDead));
		addLabel(11, guiLeft + 264, guiTop + 166, "Y:");
		addTextField(4, guiLeft + 272, guiTop + 161, 35, 15, "" + set[1])
				.setMinMaxDefault(0, 5, set[1])
				.setHoverTexts(Component.translatable("spawner.hover.axis.offset", "Y").append(strDead));
		addLabel(12, guiLeft + 309, guiTop + 166, "Z:");
		addTextField(5, guiLeft + 317, guiTop + 161, 35, 15, "" + set[2])
				.setMinMaxDefault(0, 5, set[2])
				.setHoverTexts(Component.translatable("spawner.hover.axis.offset", "Z").append(strDead));
		addCheckBox(15, guiLeft + 180, guiTop + 176, "spawner.despawn", null, job.getDespawnOnTargetLost(true))
				.setSize(170, 14)
				.setHoverTexts(Component.translatable("spawner.hover.des.tr.lost").append(strDead));
		addLabel(13, guiLeft + 180, guiTop + 195, "spawner.type");
		addButton(16, guiLeft + 238, guiTop + 190, false, job.getSpawnType(true), "spawner.one", "spawner.all", "spawner.random")
				.setSize(55, 20)
				.setHoverTexts(Component.translatable("spawner.hover.type.1."+job.getSpawnType(true)).append(strDead));
		// Both
		Component strBoth = Component.translatable("spawner.hover.sp.2");
		addCheckBox(17, guiLeft + 357, guiTop + 161, "type.exact", null, job.exact)
				.setSize(98, 14)
				.setHoverTexts(Component.translatable("spawner.hover.exact").append(strBoth));
		addCheckBox(18, guiLeft + 357, guiTop + 176, "script.update", null, job.resetUpdate)
				.setSize(98, 14)
				.setHoverTexts(Component.translatable("spawner.hover.reset").append(strBoth));
		// cooldown
		addLabel(14, guiLeft + 358, guiTop + 132, "spawner.cooldown")
				.setIsVisible(!aliveScroll.getList().isEmpty());
		addTextField(6, guiLeft + 357, guiTop + 144, 55, 15, "" + job.getCooldown() / 50L)
				.setMinMaxDefault(0, 6000, (int) (job.getCooldown() / 50L))
				.setIsVisible(!aliveScroll.getList().isEmpty())
				.setHoverTexts("spawner.hover.cooldown");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 1: {
				isDead = false;
				slot = -1;
				setSubGui(getSelector());
				break;
			} // add alive
			case 2: {
				if (isDead) { return; }
				Packets.sendServer(new SPacketNpcJobSpawnerRemove(slot, false));
				break;
			} // del alive
			case 3: {
				if (isDead) { return; }
				setSubGui(getSelector());
				break;
			} // change alive
			case 4: {
				if (isDead || slot < 1) { return; }
				Packets.sendServer(new SPacketNpcJobSpawnerMove(slot, true, false));
				break;
			} // up alive
			case 5: {
				if (isDead || slot >= job.size(false) - 1) { return; }
				Packets.sendServer(new SPacketNpcJobSpawnerMove(slot, false, false));
				break;
			} // down alive
			case 6:  {
				if (isDead) { return; }
				Packets.sendServer(new SPacketNpcJobSpawnerRemove(-1, false));
				break;
			} // clear alive
			case 7: job.setDespawnOnTargetLost(false, ((GuiCheckBoxNop) button).selected()); break; // targetLost alive
			case 8: job.setSpawnType(false, button.getValue()); break; // type alive
			case 9: {
				isDead = true;
				slot = -1;
				setSubGui(getSelector());
				break;
			} // add Dead
			case 10: {
				if (!isDead) { return; }
				Packets.sendServer(new SPacketNpcJobSpawnerRemove(slot, true));
				break;
			} // del Dead
			case 11: {
				if (!isDead) { return; }
				setSubGui(getSelector());
				break;
			} // change Dead
			case 12: {
				if (!isDead || slot < 1) { return; }
				Packets.sendServer(new SPacketNpcJobSpawnerMove(slot, true, true));
				initGui();
				break;
			} // up Dead
			case 13: {
				if (!isDead || slot >= job.size(true)) { return; }
				Packets.sendServer(new SPacketNpcJobSpawnerMove(slot, false, true));
				break;
			} // down Dead
			case 14: Packets.sendServer(new SPacketNpcJobSpawnerRemove(-1, true)); break; // clear Dead
			case 15: job.setDespawnOnTargetLost(true, ((GuiCheckBoxNop) button).selected()); break; // targetLost Dead
			case 16: job.setSpawnType(true, button.getValue()); break; // type Dead
			case 17: job.exact = ((GuiCheckBoxNop) button).selected(); break; // exact
			case 18: job.resetUpdate = ((GuiCheckBoxNop) button).selected(); break; // resetUpdate
		}
	}

	@Override
	public void save() {
		NBTTagCompound compound = job.save(new NBTTagCompound());
		job.removeCompound(compound);
		Packets.sendServer(new SPacketNpcJobSave(compound));
	}

	@Override
	public void subGuiClosed(GuiScreen gui) {
		SubGuiNpcMobSpawnerSelector selector = (SubGuiNpcMobSpawnerSelector) gui;
		if (selector.showingClones == 2) {
			String selected = selector.getSelected();
			if (!selected.isEmpty()) { Packets.sendServer(new SPacketNpcJobSpawnerAdd(isDead, selected, selector.activeTab, new NBTTagCompound())); }
		}
		else {
			NBTTagCompound nbtNpc = selector.getCompound();
			if (nbtNpc != null && selector.spawnData instanceof JobSpawnerNbtData) {
				((JobSpawnerNbtData) selector.spawnData).load(nbtNpc);
				Packets.sendServer(new SPacketNpcJobSpawnerAdd(isDead, "", 0, ((JobSpawnerNbtData) selector.spawnData).save()));
			}
		}
		if (slot < 0) { slot = (isDead ? deadScroll : aliveScroll).getList().size(); }
		initGui();
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 0: job.getOffset(false)[0] = textField.getInteger(); break; // X alive
			case 1: job.getOffset(false)[1] = textField.getInteger(); break; // Y alive
			case 2: job.getOffset(false)[2] = textField.getInteger(); break; // Z alive
			case 3: job.getOffset(true)[0] = textField.getInteger(); break; // X dead
			case 4: job.getOffset(true)[1] = textField.getInteger(); break; // Y dead
			case 5: job.getOffset(true)[2] = textField.getInteger(); break; // Z dead
			case 6: job.setCooldown(textField.getInteger()); break; // cooldown
		}
	}

	// New from Unofficial (BetaZavr)
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (!hasSubGui()) {
			GlStateManager.pushMatrix();
			if (select != null) { drawNpc(select, 385, 92, 1.0f, (int) (3 * player.world.getTotalWorldTime() % 360), 0, 0); }
			GlStateManager.translate(0.0f, 0.0f, 1.0f);
			drawVerticalLine(guiLeft + 178, guiTop + 4, guiTop + imageHeight + 12, 0xFF404040);
			drawVerticalLine(guiLeft + 353, guiTop + 4, guiTop + imageHeight + 12, 0xFF404040);
			Gui.drawRect(guiLeft + 355, guiTop + 13, guiLeft + 416, guiTop + 99, 0xFF808080);
			Gui.drawRect(guiLeft + 356, guiTop + 14, guiLeft + 415, guiTop + 98, 0xFF000000);
			GlStateManager.popMatrix();
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean keyPressed(char typedChar, int keyCode) {
		boolean bo = super.keyPressed(typedChar, keyCode);
		if (keyCode == Keyboard.KEY_UP ||
				keyCode == Keyboard.KEY_DOWN ||
				keyCode == mc.gameSettings.keyBindForward.getKeyCode() ||
				keyCode == mc.gameSettings.keyBindBack.getKeyCode()) {
			resetEntity();
		}
		return bo;
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		slot = scroll.getSelectedIndex();
		isDead = scroll.id == 0;
		(isDead ? aliveScroll : deadScroll).setSelect(-1);
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { setSubGui(getSelector()); }

	@Override
	public void setGuiData(NBTTagCompound compound) {
		if (compound.hasKey("NPCData", 10)) {
			select = EntityList.createEntityFromNBT(compound.getCompoundTag("NPCData"), player.world);
			return;
		} // Entity set
		// job data
		if (compound.hasKey("SpawnerWhenAlive", 3)) { job.load(compound); }
		// Setts
		for (int j = 0; j < 2; j++) {
			boolean type = j == 0;
			List<Component> list = new ArrayList<>();
			for (int i = 0; i < job.size(type); i++) {
				IJobSpawner.IJobSpawnerData sd = job.get(type).get(i);
				if (sd != null) {
					Component key = Component.empty()
							.append(Component.literal((i + 1) + ": ").withStyle(TextFormatting.GRAY))
							.append(sd.getTitle());
					if (sd instanceof JobSpawnerNbtData) {
						key.append(Component.literal(((JobSpawnerNbtData) sd).isClientClone() ? " (Client)" : " (Mob)")
								.withStyle((((JobSpawnerNbtData) sd).isClientClone() ? TextFormatting.GREEN : TextFormatting.RED)));
					}
					else { key.append(Component.literal(" (Server)").withStyle(TextFormatting.AQUA)); }
					list.add(key);
				}
			}
			(type ? deadScroll : aliveScroll).setUnsortedList(list);
		}
		// Data
		if (compound.hasKey("SetDead", 1)) {
			isDead = compound.getBoolean("SetDead");
			slot = -1;
			(isDead ? deadScroll : aliveScroll).setSelect(slot);
		}
		if (compound.hasKey("SetPos", 3)) {
			slot = compound.getInteger("SetPos");
			(isDead ? deadScroll : aliveScroll).setSelect(slot);
		}
		initGui();
	}

	private void resetEntity() {
		String sel = (isDead ? deadScroll : aliveScroll).getSelected();
		ITextComponent content = (isDead ? deadScroll : aliveScroll).getNormalSelected().getContents();
		if (content instanceof TextComponentTranslation) { sel = ((TextComponentTranslation) content).getKey(); }
		if (sel.isEmpty()) {
			select = null;
			return;
		}
		IJobSpawner.IJobSpawnerData sd = job.get(isDead).get(slot);
		select = null;
		if (sd != null) {
			if (sd instanceof JobSpawnerNbtData) { select = sd.getEntity().getMCEntity(); } // client / mob
			else { Packets.sendServer(new SPacketGetServerCloneEntity(true, isDead, slot, "")); } // server
		}
	}

	private SubGuiNpcMobSpawnerSelector getSelector() {
		IJobSpawner.IJobSpawnerData sd = slot >= 0 && slot < 8 ? job.get(isDead).get(slot) : null;
		if (sd == null) { sd = new JobSpawnerNbtData(npc); }
		SubGuiNpcMobSpawnerSelector guiMSS = new SubGuiNpcMobSpawnerSelector(sd);
		guiMSS.isDead = isDead;
		return guiMSS;
	}

}
