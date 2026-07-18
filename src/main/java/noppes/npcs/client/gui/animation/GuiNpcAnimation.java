package noppes.npcs.client.gui.animation;

import java.util.*;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.client.model.animation.AnimationConfig;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.AnimationController;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAnimation;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketAnimationChange;
import noppes.npcs.packets.server.SPacketAnimationGet;
import noppes.npcs.packets.server.SPacketAnimationSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.util.Util;
import noppes.npcs.util.CustomNPCsScheduler;

import javax.annotation.Nonnull;

public class GuiNpcAnimation extends GuiNPCInterface
		implements ICustomScrollListener, IGuiData {

	protected static final Map<Component, AnimationKind> dataType = new LinkedHashMap<>();
	public static int backColor = 0xFF000000;

	protected GuiCustomScrollNop scrollType;
	protected GuiCustomScrollNop scrollAnimations;
	protected GuiCustomScrollNop scrollAllAnimations;

	protected boolean isChanged = true;
	protected final LinkedHashMap<Integer, List<Component>> typeHovers = new LinkedHashMap<>();
	protected final List<Component> dataAnimations = new ArrayList<>();
	protected final Map<Component, AnimationConfig> dataAllAnimations = new LinkedHashMap<>();

	protected final EntityNPCInterface npcAnim;
	protected final DataAnimation animation;
	protected @Nonnull Component selType = Component.empty();
	protected @Nonnull Component selAnim = Component.empty();
	protected @Nonnull Component selBaseAnim = Component.empty();
	protected AnimationController aData;

	static {
		dataType.put(Component.translatable("puppet." + AnimationKind.INIT.name().toLowerCase().replace("_", "")), AnimationKind.INIT);
		dataType.put(Component.translatable("puppet." + AnimationKind.JUMP.name().toLowerCase().replace("_", "")), AnimationKind.JUMP);
		dataType.put(Component.translatable("puppet." + AnimationKind.ATTACKING.name().toLowerCase().replace("_", "")), AnimationKind.ATTACKING);
		dataType.put(Component.translatable("puppet." + AnimationKind.SHOOT.name().toLowerCase().replace("_", "")), AnimationKind.SHOOT);
		dataType.put(Component.translatable("puppet." + AnimationKind.AIM.name().toLowerCase().replace("_", "")), AnimationKind.AIM);
		dataType.put(Component.translatable("puppet." + AnimationKind.SWING.name().toLowerCase().replace("_", "")), AnimationKind.SWING);
		dataType.put(Component.translatable("puppet." + AnimationKind.HIT.name().toLowerCase().replace("_", "")), AnimationKind.HIT);
		dataType.put(Component.translatable("puppet." + AnimationKind.DIES.name().toLowerCase().replace("_", "")), AnimationKind.DIES);
		dataType.put(Component.translatable("puppet." + AnimationKind.BASE.name().toLowerCase().replace("_", "")), AnimationKind.BASE);
		dataType.put(Component.translatable("puppet." + AnimationKind.INTERACT.name().toLowerCase().replace("_", "")), AnimationKind.INTERACT);
		dataType.put(Component.translatable("puppet." + AnimationKind.BLOCKED.name().toLowerCase().replace("_", "")), AnimationKind.BLOCKED);
		dataType.put(Component.translatable("puppet." + AnimationKind.STANDING.name().toLowerCase().replace("_", "")), AnimationKind.STANDING);
		dataType.put(Component.translatable("puppet." + AnimationKind.FLY_STAND.name().toLowerCase().replace("_", "")), AnimationKind.FLY_STAND);
		dataType.put(Component.translatable("puppet." + AnimationKind.WATER_STAND.name().toLowerCase().replace("_", "")), AnimationKind.WATER_STAND);
		dataType.put(Component.translatable("puppet." + AnimationKind.REVENGE_STAND.name().toLowerCase().replace("_", "")), AnimationKind.REVENGE_STAND);
		dataType.put(Component.translatable("puppet." + AnimationKind.WALKING.name().toLowerCase().replace("_", "")), AnimationKind.WALKING);
		dataType.put(Component.translatable("puppet." + AnimationKind.FLY_WALK.name().toLowerCase().replace("_", "")), AnimationKind.FLY_WALK);
		dataType.put(Component.translatable("puppet." + AnimationKind.WATER_WALK.name().toLowerCase().replace("_", "")), AnimationKind.WATER_WALK);
		dataType.put(Component.translatable("puppet." + AnimationKind.REVENGE_WALK.name().toLowerCase().replace("_", "")), AnimationKind.REVENGE_WALK);
	}

	public GuiNpcAnimation(EntityCustomNpc npcIn) {
		super(npcIn);
		setBackground("menubg.png");
		imageWidth = 420;
		imageHeight = 200;
		closeOnEsc = true;

		animation = new DataAnimation(npcIn);
		int i = 0;
		for (AnimationKind ak : dataType.values()) {
			List<Component> list = new ArrayList<>();
			Util.instance.putHovers(list, Component.translatable("animation.hover.anim." + ak.get()));
			typeHovers.put(i, list);
			i++;
		}
		npcAnim = Util.instance.copyToGUI(npc, player.world, false);
		npcAnim.display.setName(npc.getName()+"_animation");
		Packets.sendServer(new SPacketAnimationGet(npc.getEntityId()));
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		AnimationConfig anim = getAnim();
		switch (button.id) {
			case 0: {
				if (scrollType == null || !scrollType.hasSelected()) { return; }
				AnimationConfig newAnim = aData.createNewAnim();
				newAnim.name = Util.instance.deleteColor(selType.getFormattedText().replaceAll(" ", "_")+ "_" + newAnim.id);
				newAnim.type = dataType.get(selType);
				animation.addAnimation(newAnim.type, newAnim.id);
				selAnim = newAnim.getSettingName();
				isChanged = true;
				initGui();
				CustomNPCsScheduler.runTack(() -> setSubGui(new SubGuiEditAnimation(npc, newAnim, 4)), 50);
				break;
			} // add anim
			case 1: {
				if (anim == null) { return; }
				AnimationController aData = AnimationController.getInstance();
				anim = aData.copy(anim.id, dataType.get(selType));
				anim.name = anim.name + "_" + anim.id;
				selAnim = anim.getSettingName();
				selBaseAnim = anim.getSettingName();
				if (dataType.containsKey(selType)) { animation.addAnimation(dataType.get(selType), anim.id); }
				Packets.sendServer(new SPacketAnimationChange(2, anim.save()));
				Packets.sendServer(new SPacketAnimationChange(3, new NBTTagCompound()));
				isChanged = true;
				initGui();
				break;
			} // copy anim
			case 2: {
				if (anim != null) {
					AnimationConfig finalAnim = anim;
					ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
						if (agree) {
							if (dataType.containsKey(selType)) { animation.removeAnimation(dataType.get(selType), finalAnim.id); }
							AnimationController.getInstance().removeAnimation(finalAnim.id);
							NBTTagCompound delData = new NBTTagCompound();
							delData.setInteger("ID", finalAnim.id);
							Packets.sendServer(new SPacketAnimationChange(1, delData));
							isChanged = true;
							initGui();
						}
						NoppesUtil.openGUI(player, this);
					},
							anim.getSettingName().getParent(),
							Component.translatable("message.delete").getParent());
					setScreen(guiYesNo);
				}
				break;
			} // del anim
			case 3: {
				if (anim == null || !dataType.containsKey(selType)) { return; }
				setSubGui(new SubGuiEditAnimation(npc, anim, 4));
				break;
			} // edit
			case 4: {
				GuiNpcAnimation.backColor = GuiNpcAnimation.backColor == 0xFF000000 ? 0xFFFFFFFF : 0xFF000000;
				break;
			} // back color
		}
	}

	@Override
	public void save() { Packets.sendServer(new SPacketAnimationSave(animation.save(new NBTTagCompound()))); }

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (hasSubGui()) {
			getSubGui().drawScreen(mouseX, mouseY, partialTicks);
			return;
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLeft + 342.0f, guiTop + 9.0f, 0.0f);
		Gui.drawRect(-1, -1, 56, 91, 0xFFF080F0);
		Gui.drawRect(0, 0, 55, 90, GuiNpcAnimation.backColor);
		GlStateManager.popMatrix();
		AnimationConfig anim = getAnim();
		if (anim != null && !hasSubGui() && npcAnim != null) {
			npcAnim.animation.updateTime();
			npcAnim.chasingPosZ = npc.chasingPosZ;
			npcAnim.chasingPosY = npc.chasingPosY;
			npcAnim.chasingPosX = npc.chasingPosX;
			npcAnim.prevChasingPosZ = npc.prevChasingPosZ;
			npcAnim.prevChasingPosY = npc.prevChasingPosY;
			npcAnim.prevChasingPosX = npc.prevChasingPosX;
			npcAnim.ticksExisted = npc.ticksExisted;
			npcAnim.deathTime = 0;
			drawNpc(npcAnim, 369, 81, 1.0f, 0, 0, 1);
		}
		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLeft + 315.0f, guiTop - 2.0f, 0.0f);
		int color = 0xA0000000;
		int hoverButton = -1;
		for (int i = 0; i < 5; i++) {
			GlStateManager.translate(0.0f, 22.0f, 0.0f);
			int c = color;
			if (getButton(i) != null && getButton(i).isHoveredOrFocused()) {
				c = 0xA0FFFF00;
				hoverButton = i;
			}
			drawHorizontalLine(0, 0, 0, color);
			drawHorizontalLine(0, 1, 1, color);
			drawHorizontalLine(0, 2, 2, color);
			drawHorizontalLine(0, 3, 3, color);
			drawHorizontalLine(0, 0, 4, color);
			drawHorizontalLine(1, 14, 4, c);
			drawHorizontalLine(0, 3, 5, color);
			drawHorizontalLine(0, 2, 6, color);
			drawHorizontalLine(0, 1, 7, color);
			drawHorizontalLine(0, 0, 8, color);
		}
		GlStateManager.translate(11.0f, -10.0f, 0.0f);
		drawVerticalLine(0, 0, 2, color);
		drawVerticalLine(1, -1, 2, color);
		drawVerticalLine(2, -2, 2, color);
		drawVerticalLine(3, -3, 2, color);
		drawVerticalLine(4, 0, 2, color);
		if (hoverButton != -1) {
			drawVerticalLine(4, -75 + hoverButton * 22, 1, 0xA0FFFF00);
			if (hoverButton != 0) { drawVerticalLine(4, -75, -74 + hoverButton * 22, color); }
		}
		else { drawVerticalLine(4, -75, 1, color); }
		drawVerticalLine(5, -3, 2, color);
		drawVerticalLine(6, -2, 2, color);
		drawVerticalLine(7, -1, 2, color);
		drawVerticalLine(8, 0, 2, color);
		GlStateManager.popMatrix();
	}

	@Override
	public void initGui() {
		super.initGui();
		int x = guiLeft + 8;
		int y = guiTop + 14;
		if (scrollType == null) {
			scrollType = addScroll(0).setSize(120, 198)
					.setUnsortedList(new ArrayList<>(dataType.keySet()))
					.setHoverTexts(typeHovers);
		}
		add(scrollType.setPos(x, y));
		if (selType.getFormattedText().isEmpty()) {
			for (Component key : dataType.keySet()) {
				if (dataType.get(key) == AnimationKind.STANDING) {
					selType = key;
					break;
				}
			}
		}
		scrollType.setSelected(selType);
		addLabel(0, x + 1, y - 10, Component.translatable("animation.type", ""))
				.setSize(120, 10);
		x += 123;
		dataAnimations.clear();
		dataAllAnimations.clear();
		aData = AnimationController.getInstance();
		ArrayList<Component> allAnimations = Lists.newArrayList();
		LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
		int i = 0;
		AnimationKind type = dataType.get(selType);
		if (!selAnim.getFormattedText().isEmpty() && !selBaseAnim.getFormattedText().isEmpty() &&
				!selAnim.getString().equals(selBaseAnim.getString())) {
			selAnim = Component.empty();
		}
		for (AnimationConfig ac : aData.getAnimations()) {
			Component key = Component.empty()
					.append(ac.getSettingName().withStyle(type == ac.type ? TextFormatting.GREEN : TextFormatting.GRAY));
			if (animation.hasAnimation(type, ac.id)) { dataAnimations.add(key); }
			dataAllAnimations.put(key, ac);
			if (!selAnim.getFormattedText().isEmpty() && selAnim.getString().equals(key.getString())) {
				selAnim = key;
			}
			if (!selBaseAnim.getFormattedText().isEmpty() && selBaseAnim.getString().equals(key.getString())) {
				selBaseAnim = key;
			}
			allAnimations.add(key);
			List<Component> list = new ArrayList<>();
			list.add(Component.translatable(ac.name));
			list.add(Component.empty()
					.append(Component.translatable("gui.type").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.literal(ac.type.name()).withStyle(TextFormatting.RESET)));
			hts.put(i, list);
			i++;
		}
		if (scrollAnimations == null) {
			scrollAnimations = addScroll(1).setSize(120, 198)
					.setHoverTexts("animation.hover.anim.list");
		}
		add(scrollAnimations.setPos(x, y)
				.setUnsortedList(dataAnimations));

		if (selAnim.getFormattedText().isEmpty() && selBaseAnim.getFormattedText().isEmpty() && !scrollAnimations.getList().isEmpty()) {
			for (Component key : scrollAnimations.getNormalList()) {
				selAnim = key;
				selBaseAnim = key;
				break;
			}
		}
		if (!selAnim.getFormattedText().isEmpty()) {
			scrollAnimations.setSelected(selAnim);
			if (!scrollAnimations.hasSelected()) { selAnim = Component.empty(); }
			else { selAnim = scrollAnimations.getNormalSelected(); }
		}
		else { scrollAnimations.clearSelection(); }
		if (selBaseAnim.getFormattedText().isEmpty()) { selBaseAnim = selAnim; }

		x += 123;
		if (scrollAllAnimations == null) { scrollAllAnimations = addScroll(2).setSize(160, 110); }
		add(scrollAllAnimations.setPos(x, y + 88)
				.setUnsortedList(allAnimations)
				.setHoverTexts(hts));
		if (!selBaseAnim.getFormattedText().isEmpty()) { scrollAllAnimations.setSelected(selBaseAnim); }
		AnimationConfig anim = getAnim();
		addLabel(1, x + 1, y - 10, Component.translatable("movement.animation").append(":"))
				.setSize(120, 10);
		// create
		addButton(0, x, y, "markov.generate")
				.setSize(60, 20)
				.setHoverTexts("animation.hover.anim.create");
		// back color
		addButton(4, x + 148, y, false, backColor == 0xFF000000 ? 0 : 1, "b", "w")
				.setSize(10, 10)
				.setHoverTexts("animation.hover.color");
		// copy
		addButton(1, x, y += 22, "gui.copy")
				.setSize(60, 20)
				.setIsEnabled(anim != null)
				.setHoverTexts("animation.hover.anim.copy");
		// del
		boolean isOP = anim != null && (!anim.immutable || player.getName().startsWith("BetaZavr"));
		addButton(2, x, y += 22, "gui.remove")
				.setSize(60, 20)
				.setIsEnabled(isOP)
				.setHoverTexts("animation.hover.anim.del");
		// edit
		addButton(3, x, y + 22, "gui.edit")
				.setSize(60, 20)
				.setIsEnabled(isOP)
				.setHoverTexts("animation.hover.anim.edit");
		resetAnimation();
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0) {
			if (selType.getString().equals(scroll.getSelected())) { return; }
			selType = scroll.getNormalSelected();
			isChanged = true;
		} // animation Type
		else if (scroll.id == 1) {
			if (selAnim.getFormattedText().equals(scroll.getSelected())) { return; }
			selAnim = scroll.getNormalSelected();
			scrollAllAnimations.setSelected(selAnim);
			selBaseAnim = scrollAllAnimations.getNormalSelected();
			isChanged = true;
		} // animation in type
		else if (scroll.id == 2) {
			if (selBaseAnim.getFormattedText().equals(scroll.getSelected())) { return; }
			selBaseAnim = scroll.getNormalSelected();
			if (selBaseAnim.getFormattedText().equals(scrollAnimations.getSelected())) {
				scrollAnimations.setSelected(selBaseAnim);
				selAnim = scrollAnimations.getNormalSelected();
			}
			isChanged = true;
		} // animation in base
		if (isChanged) { initGui(); }
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		AnimationConfig anim;
		if (scroll.id == 1) {
			anim = getAnim();
			if (anim == null) { return; }
			AnimationKind type = dataType.get(selType);
			if (animation.hasAnimation(type, anim.id) && animation.removeAnimation(type, anim.id)) {
				isChanged = true;
				initGui();
			}
		}
		else if (scroll.id == 2 && dataAllAnimations.containsKey(selBaseAnim)) {
			anim = dataAllAnimations.get(selBaseAnim);
			if (anim == null) { return; }
			selAnim = anim.getSettingName();
			selBaseAnim = anim.getSettingName();
			AnimationKind type = dataType.get(selType);
			if (!animation.hasAnimation(type, anim.id)) {
				animation.addAnimation(type, anim.id);
				isChanged = true;
				initGui();
			}
		}
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		animation.load(compound);
		isChanged = true;
		initGui();
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiEditAnimation && ((SubGuiEditAnimation) subgui).anim != null) {
			selAnim = ((SubGuiEditAnimation) subgui).anim.getSettingName();
			selBaseAnim = selAnim;
			Packets.sendServer(new SPacketAnimationChange(2, ((SubGuiEditAnimation) subgui).anim.save()));
			isChanged = true;
			initGui();
		} // create new
	}

	@Override
	public void onClose() {
		NoppesUtil.requestOpenGUI(EnumGuiType.MainMenuAdvanced);
		super.onClose();
	}

	protected AnimationConfig getAnim() {
		if (!dataAnimations.contains(selAnim) && !selAnim.getFormattedText().isEmpty()) { selAnim = Component.empty(); }
		if (!selAnim.getFormattedText().isEmpty() && dataAllAnimations.containsKey(selAnim)) { return dataAllAnimations.get(selAnim); }
		if (selBaseAnim.getFormattedText().isEmpty() || !dataAllAnimations.containsKey(selBaseAnim)) { return null; }
		return dataAllAnimations.get(selBaseAnim);
	}

	protected void resetAnimation() {
		if (!isChanged) { return; }
		AnimationConfig anim = getAnim();
		if (anim == null || npcAnim == null) { return; }
		anim = anim.copy();
		anim.type = dataType.get(selType);
		npcAnim.animation.reset();
		npcAnim.animation.tryRunAnimation(anim, AnimationKind.EDITING_All);
		npcAnim.setHealth(npcAnim.getMaxHealth());
		npcAnim.deathTime = 0;
	}

}
