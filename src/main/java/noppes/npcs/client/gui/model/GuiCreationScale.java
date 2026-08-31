package noppes.npcs.client.gui.model;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.model.part.ModelPartConfig;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiSliderNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;

import javax.annotation.Nonnull;

public class GuiCreationScale extends GuiCreationScreenInterface
		implements ISliderListener, ICustomScrollListener {

	protected static EnumParts selected = EnumParts.HEAD;
	protected final List<EnumParts> data = new ArrayList<>();
	protected GuiCustomScrollNop scroll;

	public GuiCreationScale(EntityNPCInterface npc) {
		super(npc);
		active = 3;
		xOffset = 140;
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		if (button.id == 13) {
            playerdata.getPartConfig(GuiCreationScale.selected).notShared = ((GuiButtonYesNo) button).getBoolean();
			initGui();
		}
		super.buttonEvent(button);
	}

	@Override
	public void initGui() {
		super.initGui();
		if (scroll == null) { scroll = addScroll(0); }
		List<Component> list = new ArrayList<>();
		EnumParts[] parts = { EnumParts.HEAD, EnumParts.BODY, EnumParts.ARM_LEFT, EnumParts.ARM_RIGHT, EnumParts.LEG_LEFT, EnumParts.LEG_RIGHT };
		data.clear();
		for (EnumParts part : parts) {
			Label_0210: {
				if (part == EnumParts.ARM_RIGHT) {
					ModelPartConfig config = playerdata.getPartConfig(EnumParts.ARM_LEFT);
					if (!config.notShared) { break Label_0210; }
				}
				if (part == EnumParts.LEG_RIGHT) {
					ModelPartConfig config = playerdata.getPartConfig(EnumParts.LEG_LEFT);
					if (!config.notShared) { break Label_0210; }
				}
				data.add(part);
				list.add(Component.translatable("part." + part.name));
			}
		}
		add(scroll.setPos(guiLeft, guiTop + 46)
				.setUnsortedList(list)
				.setSize(100, imageHeight - 74)
				.disabledSearch());
		ModelPartConfig config2 = playerdata.getPartConfig(GuiCreationScale.selected);
		int y = guiTop + 65;
		addLabel(10, guiLeft + 102, y + 5, "scale.width")
				.setColor(CustomNpcs.MainColor.getRGB());
		addSlider(10, guiLeft + 150, y, config2.scaleX - 0.5f)
				.setSize(100, 20)
				.setHoverTexts("hover.scale.x");
		addLabel(11, guiLeft + 102, (y += 22) + 5, "scale.height")
				.setColor(CustomNpcs.MainColor.getRGB());
		addSlider(11, guiLeft + 150, y, config2.scaleY - 0.5f)
				.setSize(100, 20)
				.setHoverTexts("hover.scale.y");
		addLabel(12, guiLeft + 102, (y += 22) + 5, "scale.depth")
				.setColor(CustomNpcs.MainColor.getRGB());
		addSlider(12, guiLeft + 150, y, config2.scaleZ - 0.5f)
				.setSize(100, 20)
				.setHoverTexts("hover.scale.z");
		if (GuiCreationScale.selected == EnumParts.ARM_LEFT || GuiCreationScale.selected == EnumParts.LEG_LEFT) {
			addLabel(13, guiLeft + 102, (y += 22) + 5, "scale.shared")
					.setColor(CustomNpcs.MainColor.getRGB());
			addYesNo(13, guiLeft + 150, y, config2.notShared)
					.setSize(50, 20)
					.setHoverTexts("display.hover.part.pattern");
		}
	}

	@Override
	public void mouseDragged(GuiSliderNop slider) {
		super.mouseDragged(slider);
		if (slider.id >= 10 && slider.id <= 12) {
			int percent = (int) (50.0f + slider.sliderValue * 100.0f);
			slider.setString(percent + "%");
			ModelPartConfig config = playerdata.getPartConfig(GuiCreationScale.selected);
			float value = slider.sliderValue + 0.5f;
			switch (slider.id - 10) {
				case 0: config.scaleX = value; break;
				case 1: config.scaleY = value; break;
				case 2: config.scaleZ = value; break;
			}

			updateTranslate();
		}
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (scroll.hasSelected()) {
			GuiCreationScale.selected = data.get(scroll.getSelectedIndex());
			initGui();
		}
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

	private void updateTranslate() {
		for (EnumParts part : EnumParts.values()) {
			ModelPartConfig config = playerdata.getPartConfig(part);
			if (config != null) {
				if (part == EnumParts.HEAD) { config.setTranslate(0.0f, playerdata.getBodyY(), 0.0f); }
				else if (part == EnumParts.ARM_LEFT) {
					ModelPartConfig body = playerdata.getPartConfig(EnumParts.BODY);
					float x = (1.0f - body.scaleX) * 0.25f + (1.0f - config.scaleX) * 0.075f;
					float y = playerdata.getBodyY() + (1.0f - config.scaleY) * -0.1f;
					config.setTranslate(-x, y, 0.0f);
					if (!config.notShared) {
						ModelPartConfig arm = playerdata.getPartConfig(EnumParts.ARM_RIGHT);
						arm.copyValues(config);
					}
				} else if (part == EnumParts.ARM_RIGHT) {
					ModelPartConfig body = playerdata.getPartConfig(EnumParts.BODY);
					float x = (1.0f - body.scaleX) * 0.25f + (1.0f - config.scaleX) * 0.075f;
					float y = playerdata.getBodyY() + (1.0f - config.scaleY) * -0.1f;
					config.setTranslate(x, y, 0.0f);
				} else if (part == EnumParts.LEG_LEFT) {
					config.setTranslate(config.scaleX * 0.125f - 0.113f, playerdata.getLegsY(), 0.0f);
					if (!config.notShared) {
						ModelPartConfig leg = playerdata.getPartConfig(EnumParts.LEG_RIGHT);
						leg.copyValues(config);
					}
				} else if (part == EnumParts.LEG_RIGHT) { config.setTranslate((1.0f - config.scaleX) * 0.125f, playerdata.getLegsY(), 0.0f); }
				else if (part == EnumParts.BODY) { config.setTranslate(0.0f, playerdata.getBodyY(), 0.0f); }
			}
		}
	}

}
