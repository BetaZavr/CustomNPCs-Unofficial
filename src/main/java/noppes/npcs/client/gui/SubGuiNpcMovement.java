package noppes.npcs.client.gui;

import noppes.npcs.ai.EntityAIAnimation;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class SubGuiNpcMovement extends GuiBasic implements ITextfieldListener {

	protected final DataAI ai;

	public SubGuiNpcMovement(DataAI ais) {
		super();
		ai = ais;
		setBackground("menubg.png");

		imageWidth = 256;
		imageHeight = 216;
	}

	@Override
	public void initGui() {
		super.initGui();
		int x0 = guiLeft + 4;
		int x1 = x0 + 76;
		int y = guiTop + 4;
		addLabel(0, x0, y + 5, "movement.type");
		addButton(0, x1, y, false, ai.getMovingType(), "ai.standing", "ai.wandering", "ai.movingpath")
				.setSize(100, 20)
				.setHoverTexts("ai.hover.walking.type");
		y += 22;
		addButton(15, x1, y, false, ai.movementType, "movement.ground", "movement.flying", "movement.swimming")
				.setSize(100, 20)
				.setHoverTexts("ai.hover.walking");
		addLabel(15, x0, y + 5, "movement.navigation");
		if (ai.getMovingType() == 1) {
			addTextField(4, x1 += 20, y += 22, 60, 20, ai.walkingRange)
					.setMinMaxDefault(0, 1000, 10)
					.setHoverTexts("ai.hover.walking.range");
			addLabel(4, x0, y + 5, "gui.range");
			addTextField(10, x1, y += 22, 60, 20, ai.activeRange)
					.setMinMaxDefault(32, 1000, 10);
			addLabel(10, x0, y + 5, "gui.active range");
			addYesNo(5, x1, y += 22, ai.npcInteracting)
					.setSize(50, 20)
					.setHoverTexts("ai.hover.interact");
			addLabel(5, x0, y + 5, "movement.wanderinteract");
			addYesNo(9, x1 - 20, y += 22, ai.movingPause)
					.setSize(80, 20)
					.setHoverTexts("ai.hover.moving.pause");
			addLabel(9, x0, y + 5, "movement.pauses");
		}
		else if (ai.getMovingType() == 0) {
			addLabel(17, x0, y + 27, "spawner.posoffset");
			addLabel(7, guiLeft + 89, y + 27, "X:");
			addTextField(7, x1 + 19, y += 22, 24, 20, ai.bodyOffsetX)
					.setMinMaxDefault(0, 10, 5)
					.setHoverTexts("ai.hover.offset.x");
			addLabel(8, guiLeft + 125, y + 5, "Y:");
			addTextField(8, guiLeft + 135, y, 24, 20, ai.bodyOffsetY)
					.setMinMaxDefault(0, 100, 5)
					.setHoverTexts("ai.hover.offset.y");
			addLabel(9, guiLeft + 161, y + 5, "Z:");
			addTextField(9, guiLeft + 171, y, 24, 20, ai.bodyOffsetZ)
					.setMinMaxDefault(0, 10, 5)
					.setHoverTexts("ai.hover.offset.z");
			addButton(3, x1, y += 22, false, ai.animationType,
					"stats.normal", "movement.sitting", "movement.lying", "movement.hug", "movement.sneaking",
					"movement.dancing", "movement.aiming", "movement.crawling").setSize(100, 20)
					.setHoverTexts("ai.hover.walking.anim");
			addLabel(3, x0, y + 5, "movement.animation");
			if (ai.animationType != 2) {
				addButton(4, x1, y += 22, false, ai.getStandingType(), "movement.body", "movement.manual", "movement.stalking", "movement.head")
						.setSize(80, 20)
						.setHoverTexts("ai.hover.rotation");
				addLabel(1, x0, y + 5, "movement.rotation");
			}
			else {
				addTextField(5, x1 + 19, y += 22, 40, 20, ai.orientation)
						.setMinMaxDefault(0, 359, 0)
						.setHoverTexts("ai.hover.interact");
				addLabel(6, x0, y + 5, "movement.rotation");
				addLabel(5, guiLeft + 142, y + 5, "(0-359)");
			}
			if (ai.animationType != 2 && (ai.getStandingType() == 1 || ai.getStandingType() == 3)) {
				addTextField(5, guiLeft + 165, y, 40, 20, ai.orientation)
						.setMinMaxDefault(0, 359, 0)
						.setHoverTexts("ai.hover.interact");
				addLabel(5, guiLeft + 207, y + 5, "(0-359)");
			}
		}
		if (ai.getMovingType() != 0) {
			addButton(12, x1, y += 22, false, EntityAIAnimation.getWalkingAnimationGuiIndex(ai.animationType),
					"stats.normal", "movement.sneaking", "movement.aiming", "movement.dancing", "movement.crawling", "movement.hug")
					.setSize(100, 20)
					.setHoverTexts("ai.hover.walking.anim");
			addLabel(12, x0, y + 5, "movement.animation");
		}
		if (ai.getMovingType() == 2) {
			x1 = guiLeft + 80;
			addButton(8, x1, y += 22, false, ai.movingPattern, "ai.looping", "ai.backtracking")
					.setSize(80, 20)
					.setHoverTexts("ai.hover.path.closed");
			addLabel(8, x0, y + 5, "movement.name");
			addYesNo(9, x1, y += 22, ai.movingPause).setSize(80, 20);
			addLabel(9, x0, y + 5, "movement.pauses");
		}
		x1 = guiLeft + 100;
		addYesNo(13, x1, y += 22, ai.stopAndInteract)
				.setSize(50, 20)
				.setHoverTexts("ai.hover.stop.interact");
		addLabel(13, x0, y + 5, "movement.stopinteract");
		addTextField(14, x1 - 20, y += 22, 50, 18, ai.getWalkingSpeed())
				.setMinMaxDefault(0, 100, ai.getWalkingSpeed())
				.setHoverTexts("ai.hover.walking.speed");
		addLabel(14, x0, y + 5, "stats.movespeed");
		addTextField(15, x1 - 20, y += 22, 50, 18, ai.stepheight)
				.setMinMaxDefault(0.1d, 3.0d, ai.stepheight)
				.setHoverTexts("ai.hover.step.height");
		addLabel(16, x0, y + 5, "stats.stepheight");
		addButton(66, guiLeft + 190, guiTop + 190, "gui.done")
				.setSize(60, 20)
				.setHoverTexts("hover.back");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				ai.setMovingType(button.getValue());
				if (ai.getMovingType() != 0) {
					ai.animationType = 0;
					ai.setStandingType(0);
					ai.bodyOffsetX = ai.bodyOffsetY = ai.bodyOffsetZ = 5.0F;
				}
				initGui();
				break;
			}
			case 3: {
				ai.setAnimation(button.getValue());
				initGui();
				break;
			}
			case 4: {
				ai.setStandingType(button.getValue());
				initGui();
				break;
			}
			case 5: ai.npcInteracting = button.getValue() == 1; break;
			case 8: ai.movingPattern = button.getValue(); break;
			case 9: ai.movingPause = button.getValue() == 1; break;
			case 12: {
				switch (button.getValue()) {
					case 1: ai.animationType = 4; break;
					case 2: ai.animationType = 6; break;
					case 3: ai.animationType = 5; break;
					case 4: ai.animationType = 7; break;
					case 5: ai.animationType = 3; break;
					default: ai.animationType = 0; break;
				}
				break;
			}
			case 13: ai.stopAndInteract = button.getValue() == 1; break;
			case 15: ai.movementType = button.getValue(); break;
			case 66: onClose(); break;
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 4: ai.walkingRange = textField.getInteger(); break;
			case 5: ai.orientation = textField.getInteger(); break;
			case 7: ai.bodyOffsetX = textField.getFloat(); break;
			case 8: ai.bodyOffsetY = textField.getFloat(); break;
			case 9: ai.bodyOffsetZ = textField.getFloat(); break;
			case 10: ai.activeRange = textField.getInteger(); break;
			case 14: ai.setWalkingSpeed(textField.getInteger()); break;
			case 15: ai.stepheight = textField.getFloat(); break;
		}
	}

}
