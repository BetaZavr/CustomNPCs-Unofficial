package noppes.npcs.client.gui.animation;

import java.util.*;

import net.minecraft.network.chat.Component;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketAnimationGet;
import noppes.npcs.packets.server.SPacketAnimationSave;
import noppes.npcs.packets.server.SPacketEmotionSave;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.parts.ModelPartData;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.model.animation.EmotionConfig;
import noppes.npcs.client.model.animation.EmotionFrame;
import noppes.npcs.client.model.part.ModelEyeData;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.controllers.AnimationController;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAnimation;
import noppes.npcs.util.Util;
import noppes.npcs.util.CustomNPCsScheduler;

import javax.annotation.Nonnull;

public class GuiNpcEmotion extends GuiNPCInterface2
		implements ICustomScrollListener, IGuiData, ITextfieldListener, ISliderListener {

	public static final ResourceLocation etns = new ResourceLocation(CustomNpcs.MODID, "textures/gui/emotion/buttons.png");

	public EntityNPCInterface npcEmtn;
	public int elementType = 0; // 0 - eye, 1 - pupil, 2 - brow, 3 - mouth
	public boolean isRight = true;

	protected final Map<Component, EmotionConfig> dataEmtns = new LinkedHashMap<>();
	protected final DataAnimation animation;
	protected boolean onlyPart = false;
	protected int toolType = 0; // 0 - rotation, 1 - offset, 2 - scale
	protected Component selEmtn = Component.empty();
	protected EmotionFrame frame;
	protected ScaledResolution sw;
	protected ModelEyeData modelEye;
	protected GuiCustomScrollNop scroll;
	protected AnimationController aData;

	public GuiNpcEmotion(EntityNPCInterface npc) {
		super(npc, 4);
		setBackground("bgfilled.png");
		backGui = EnumGuiType.MainMenuAdvanced;

		animation = new DataAnimation(npc);
		animation.setBaseEmotionId(npc.animation.getBaseEmotionId());

		npcEmtn = Util.instance.copyToGUI(npc, player.world, false);
		Packets.sendServer(new SPacketAnimationGet(npc.getEntityId()));
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		EmotionConfig emtn = getEmtn();
		switch (button.id) {
			case 0: {
				setSubGui(new SubGuiEditText(1, Util.instance.deleteColor(Component.translatable("gui.new").getFormattedText())));
				break;
			} // create new
			case 1: {
				if (emtn != null && dataEmtns.containsKey(scroll.getNormalSelected())) {
					if (aData.removeEmotion(emtn.id)) { selEmtn = Component.empty(); }
					initGui();
				}
				break;
			} // del
			case 2: {
				GuiNpcAnimation.backColor = (GuiNpcAnimation.backColor == 0xFF000000 ? 0xFFFFFFFF : 0xFF000000);
				button.setColor(GuiNpcAnimation.backColor == 0xFF000000 ? 0xFF00FFFF : 0xFF008080);
				break;
			} // back color
			case 3: {
				if (emtn == null || !emtn.frames.containsKey(button.getValue())) { return; }
				frame = emtn.frames.get(button.getValue());
				initGui();
				break;
			} // select frame
			case 4: {
				if (emtn == null) { return; }
				frame = (EmotionFrame) emtn.addFrame(frame);
				initGui();
				break;
			} // add frame
			case 5: {
				if (frame != null) {
					ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
						if (agree && emtn != null && frame != null && emtn.frames.size() <= 1) {
							int f = frame.id - 1;
							if (f < 0) { f = 0; }
							emtn.removeFrame(frame);
							frame = emtn.frames.get(f);
							initGui();
						}
						NoppesUtil.openGUI(player, this);
					},
							Component.translatable("animation.clear.frame", "" + (frame.id + 1)).getParent(),
							Component.translatable("message.delete").getParent());
					setScreen(guiYesNo);
				}
				break;
			} // del frame
			case 6: {
				if (frame != null) {
					boolean isShift = GuiScreen.isShiftKeyDown();
					ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
						if (agree) {
							if (isShift) {
								if (emtn != null) {
									for (EmotionFrame f : emtn.frames.values()) { f.clear(); }
								}
							} else if (frame != null) { frame.clear(); }
							initGui();
						}
						NoppesUtil.openGUI(player, this);
					},
							Component.translatable("animation.clear.frame", "" + (frame.id + 1)).getParent(),
							Component.translatable("message.delete").getParent());
					setScreen(guiYesNo);
				}
				break;
			} // clear frame
			case 11: {
				if (emtn != null && frame != null) {
					frame.setSmooth(((GuiCheckBoxNop) button).selected());
					if (GuiScreen.isShiftKeyDown()) {
						for (EmotionFrame f : emtn.frames.values()) { f.setSmooth(frame.isSmooth()); }
					}
					resetEmtns();
				}
				break;
			} // smooth
			case 22: {
				if (elementType == 3) { return; }
				elementType = 3;
				initGui();
				break;
			} // element eye
			case 23: {
				if (toolType == 1) { return; }
				toolType = 1;
				initGui();
				break;
			} // tool pos
			case 24: {
				if (toolType == 0) { return; }
				toolType = 0;
				initGui();
				break;
			} // tool rot
			case 25: {
				if (toolType == 2) { return; }
				toolType = 2;
				initGui();
				break;
			} // tool scale
			case 26: {
				if (elementType == 0) { return; }
				elementType = 0;
				initGui();
				break;
			} // element eye
			case 27: {
				if (elementType == 1) { return; }
				elementType = 1;
				initGui();
				break;
			}// element pupil
			case 28: {
				if (elementType == 2) { return; }
				elementType = 2;
				initGui();
				break;
			} // element brow
			case 29: {
				isRight = button.getValue() == 0;
				initGui();
				break;
			} // element brow
			case 30: {
				if (frame == null) { return; }
				switch (toolType) {
					case 0: {
						switch (elementType) {
							case 0: frame.rotEye[isRight ? 0 : 1] = 0.0f; break;
							case 1: frame.rotPupil[isRight ? 0 : 1] = 0.0f; break;
							case 2: frame.rotBrow[isRight ? 0 : 1] = 0.0f; break;
							case 3: frame.rotMouth = 0.0f; break;
						}
						break;
					}
					case 1: {
						switch (elementType) {
							case 0: frame.offsetEye[isRight ? 0 : 2] = 0.0f; break;
							case 1: frame.offsetPupil[isRight ? 0 : 2] = 0.0f; break;
							case 2: frame.offsetBrow[isRight ? 0 : 2] = 0.0f; break;
							case 3: frame.offsetMouth[0] = 0.0f; break;
						}
						break;
					}
					case 2: {
						switch (elementType) {
							case 0: frame.scaleEye[isRight ? 0 : 2] = 1.0f; break;
							case 1: frame.scalePupil[isRight ? 0 : 2] = 1.0f; break;
							case 2: frame.scaleBrow[isRight ? 0 : 2] = 1.0f; break;
							case 3: frame.scaleMouth[0] = 1.0f; break;
						}
						break;
					}
				}
				initGui();
				break;
			} // reset X
			case 31: {
				if (frame == null) { return; }
				switch (toolType) {
					case 1:
						switch (elementType) {
							case 0: frame.offsetEye[isRight ? 1 : 3] = 0.0f; break;
							case 1: frame.offsetPupil[isRight ? 1 : 3] = 0.0f; break;
							case 2: frame.offsetBrow[isRight ? 1 : 3] = 0.0f; break;
							case 3: frame.offsetMouth[1] = 0.0f; break;
						}
						break;
					case 2:
						switch (elementType) {// 0 - eye, 1 - pupil, 2 - brow
							case 0: frame.scaleEye[isRight ? 1 : 3] = 1.0f; break;
							case 1: frame.scalePupil[isRight ? 1 : 3] = 1.0f; break;
							case 2: frame.scaleBrow[isRight ? 1 : 3] = 1.0f; break;
							case 3: frame.scaleMouth[1] = 1.0f; break;
						}
						break;
				}
				initGui();
				break;
			} // reset part set Y
			case 32: {
				if (modelEye == null) { return; }
				modelEye.type = (byte) button.getValue();
				break;
			} // type
			case 33: {
				if (animation == null || !dataEmtns.containsKey(selEmtn)) { return; }
				animation.setBaseEmotionId(dataEmtns.get(selEmtn).id);
				initGui();
				break;
			} // set base emotion
			case 34: {
				if (animation == null) { return; }
				animation.setBaseEmotionId(-1);
				initGui();
				break;
			} // del base emotion
			case 35: {
				if (frame != null) {
					frame.setBlink(((GuiCheckBoxNop) button).selected());
					initGui();
				}
				break;
			} // start blink
			case 36: {
				if (frame != null) {
					frame.setEndBlink(((GuiCheckBoxNop) button).selected());
					initGui();
				}
				break;
			} // end blink
			case 37: {
				if (emtn != null) {
					emtn.setCanBlink(((GuiCheckBoxNop) button).selected());
					initGui();
				}
				break;
			} // can blink
			case 38: {
				if (frame != null) {
					frame.setDisable(((GuiCheckBoxNop) button).selected());
					if (GuiScreen.isShiftKeyDown() && emtn != null) { // Shift pressed
						for (EmotionFrame f : emtn.frames.values()) {
							f.setDisable(frame.isDisabled());
						}
					}
					initGui();
				}
				break;
			} // disable pupil
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		sw = new ScaledResolution(mc);
		int lId = 0;
		int x = guiLeft + 8;
		int y = guiTop + 14;
		if (scroll == null) { scroll = addScroll(0).setSize(120, 177); }
		addLabel(lId++, x + 1, y - 10, "emotion.list");
		dataEmtns.clear();
		aData = AnimationController.getInstance();
		for (EmotionConfig ec : aData.getEmotions()) { dataEmtns.put(ec.getSettingName(), ec); }
		scroll.setUnsortedList(new ArrayList<>(dataEmtns.keySet()));
		scroll.setSelected(selEmtn);
		if (!dataEmtns.containsKey(selEmtn)) { selEmtn = Component.empty(); }
		if (selEmtn.getFormattedText().isEmpty() && !dataEmtns.isEmpty()) {
			for (Component key : dataEmtns.keySet()) {
				selEmtn = key;
				scroll.setSelected(selEmtn);
				frame = dataEmtns.get(key).frames.get(0);
				break;
			}
		}
		add(scroll.setPos(x, y));
		addButton(0, x, y + scroll.height + 21, "markov.generate")
				.setSize(59, 20)
				.setIsEnabled(dataEmtns.isEmpty());
		addButton(1, x + 62, y + scroll.height + 21, "gui.remove")
				.setSize(59, 20)
				.setIsEnabled(scroll.hasSelected());
		EmotionConfig emtn = getEmtn();
		if (emtn == null) { return; }
		if (frame == null) {
			frame = emtn.frames.get(0);
			if (frame == null) { return; }
		}
		int wX = guiLeft + 274;
		int wY = guiTop + 71;

		// Back color
		addButton(2, wX, wY - 13, "")
				.setSize(8, 8)
				.setColor(GuiNpcAnimation.backColor == 0xFF000000 ? 0xFF00FFFF : 0xFF008080)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0);
		// Tool type
		// tool pos
		addButton(23, wX + 10, wY - 16, "")
				.setSize(14, 14)
				.setColor(toolType == 1 ? 0xFFFF8080 : 0xFFFFFFFF)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 0, 24, 24)
				.setHoverTexts("animation.hover.tool.0");
		// tool rot
		addButton(24, wX + 26, wY - 16, "")
				.setSize(14, 14)
				.setColor(toolType == 0 ? 0xFF80FF80 : 0xFFFFFFFF)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(24, 0, 24, 24)
				.setHoverTexts("animation.hover.tool.1");
		// tool scale
		addButton(25, wX + 42, wY - 16, "")
				.setSize(14, 14)
				.setColor(toolType == 2 ? 0xFF8080FF : 0xFFFFFFFF)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(48, 0, 24, 24)
				.setHoverTexts("animation.hover.tool.2");
		// element eye
		addButton(26, wX + 78, wY - 16, "")
				.setSize(14, 14)
				.setColor(elementType == 0 ? 0xFFFF8080 : 0xFFFFFFFF)
				.setTexture(etns)
				.setDefBack(false)
				.setUV(0, 0, 24, 24);
		// element pupil
		addButton(27, wX + 94, wY - 16, "")
				.setSize(14, 14)
				.setColor(elementType == 1 ? 0xFF80FF80 : 0xFFFFFFFF)
				.setTexture(etns)
				.setDefBack(false)
				.setUV(24, 0, 24, 24);
		// element brow
		addButton(28, wX + 110, wY - 16, "")
				.setSize(14, 14)
				.setColor(elementType == 2 ? 0xFF8080FF : 0xFFFFFFFF)
				.setTexture(etns)
				.setDefBack(false)
				.setUV(48, 0, 24, 24);
		// element mouth
		addButton(22, wX + 126, wY - 16, "")
				.setSize(14, 14)
				.setColor(elementType == 3 ? 0xFFFFFF80 : 0xFFFFFFFF)
				.setTexture(etns)
				.setDefBack(false)
				.setUV(72, 0, 24, 24);
		addButton(32, wX + 58, wY - 48, true, (modelEye == null) ? 0 : modelEye.type, "gui.small", "gui.normal", "gui.select")
				.setSize(82, 14);
		addButton(33, wX, y - 10, "gui.set")
				.setSize(70, 14)
				.setIsEnabled(animation.getBaseEmotionId() != emtn.id);
		addButton(34, wX + 72, y - 10, "gui.remove")
				.setSize(70, 14)
				.setIsEnabled(animation.getBaseEmotionId() >= 0);
		addButton(29, wX + 58, wY - 32, false, isRight ? 0 : 1, "gui.right", "gui.left")
				.setSize(82, 14)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0);
		// Name
		x += scroll.width + 2;
		y = guiTop + 14;
		addLabel(lId++, x + 1, y - 10, Component.translatable("gui.name").append(":"));
		addTextField(0, x + 1, y, 135, 12, emtn.name);
		// Frame
		addLabel(lId++, x, (y += 26) - 10, "animation.frames");
		List<String> lFrames = new ArrayList<>();
		for (int i = 0; i < emtn.frames.size(); i++) { lFrames.add((i + 1) + "/" + emtn.frames.size()); }
		addButton(3, x, y, true, emtn.id, lFrames.toArray(new Object[0]))
				.setSize(60, 14)
				.setIsEnabled(emtn.frames.size() > 1);
		// add frame
		addButton(4, x + 62, y + 2, "")
				.setSize(10, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(96, 0, 24, 24);
		// del frame
		addButton(5, x + 74, y + 2, "")
				.setSize(10, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(72, 0, 24, 24)
				.setIsEnabled(emtn.frames.size() > 1);
		// clear frame
		addButton(6, x + 126, y + 2, "")
				.setSize(10, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(120, 0, 24, 24);
		addLabel(lId++, x, (y += 17) + 2, Component.translatable("gui.time").append(":"));
		addTextField(1, x + 45, y, 43, 12, "" + frame.getSpeed())
				.setMinMaxDefault(0, 3600, frame.getSpeed());
		addTextField(2, x + 92, y, 43, 12, "" + frame.getEndDelay())
				.setMinMaxDefault(0, 3600, frame.getEndDelay());
		addLabel(lId++, x, (y += 13) + 5, Component.translatable("gui.repeat").append(":"));
		if (emtn.repeatLast < 0) { emtn.repeatLast *= -1; }
		if (emtn.repeatLast < 0 || emtn.repeatLast > emtn.frames.size()) { emtn.repeatLast = emtn.frames.size(); }
		addTextField(3, x + 45, y + 3, 43, 12, "" + emtn.repeatLast)
				.setMinMaxDefault(0, emtn.frames.size(), emtn.repeatLast);
		addCheckBox(11, x + 92, y + 1, "gui.smooth", "gui.linearly", frame.isSmooth())
				.setSize(45, 14);
		addCheckBox(38, x, y += 16, "emotion.part.disable", null, frame.isDisabled())
				.setSize(140, 14);
		resetEmtns();
		addCheckBox(35, x, y += 15, "emotion.blink", "emotion.no.blink", frame.isBlink())
				.setSize(140, 14);
		y += 15;
		if (frame.isBlink()) {
			addCheckBox(36, x, y, "emotion.end.blink", null, frame.isEndBlink())
				.setSize(140, 14);
		}
		// Tool sliders
		int f = 18;
		float[] values;
		switch(elementType) {
			case 1: {
				switch(toolType) { // 0 - rotation, 1 - offset, 2 - scale
					case 1: values = new float[] { frame.offsetPupil[isRight ? 0 : 2], frame.offsetPupil[isRight ? 1 : 3] }; break;
					case 2: values = new float[] { frame.scalePupil[isRight ? 0 : 2], frame.scalePupil[isRight ? 1 : 3] }; break;
					default: values = new float[] { frame.rotPupil[isRight ? 0 : 1] }; break;
				}
				break;
			} // pupil
			case 2: {
				switch(toolType) {
					case 1: values = new float[] { frame.offsetBrow[isRight ? 0 : 2], frame.offsetBrow[isRight ? 1 : 3] }; break;
					case 2: values = new float[] { frame.scaleBrow[isRight ? 0 : 2], frame.scaleBrow[isRight ? 1 : 3] }; break;
					default: values = new float[] { frame.rotBrow[isRight ? 0 : 1] }; break;
				}
				break;
			} // brow
			case 3: {
				switch(toolType) {
					case 1: values = frame.offsetMouth; break;
					case 2: values = frame.scaleMouth; break;
					default: values = new float[] { frame.rotMouth }; break;
				}
				break;
			} // mouth
			default: {
				switch(toolType) {
					case 1: values = new float[] { frame.offsetEye[isRight ? 0 : 2], frame.offsetEye[isRight ? 1 : 3] }; break;
					case 2: values = new float[] { frame.scaleEye[isRight ? 0 : 2], frame.scaleEye[isRight ? 1 : 3] }; break;
					default: values = new float[] { frame.rotEye[isRight ? 0 : 1] }; break;
				}
				break;
			} // eye
		}
		y += 19;
		for (int i = 0; i < 2; i++) {
			if (toolType == 0 && i == 1) { break; }
			addLabel(lId++, x, y + i * f + 4, i == 0 ? "X:" : "Y:");
			float sliderData;
			double m, n;
			if (toolType == 1) { // offset
				m = isRight ? -1.0d : -1.5d;
				n = isRight ? 1.5d : 1.0d;
				sliderData = values[i] * 0.4f + (isRight ? 0.4f : 0.6f);
			} else if (toolType == 2) { // scale
				m = 0.0d;
				n = 2.0d;
				sliderData = values[i] * 0.5f;
			} else { // rotation
				m = -180.0d;
				n = 180.0d;
				sliderData = values[i] * 0.0027778f + 0.5f;
			}
			addSlider(i, x + 8, y + i * f, sliderData)
					.setSize(128, 8);
			addTextField(i + 5, x + 9, y + 9 + i * f, 56, 8, "" + values[i])
					.setMinMaxDefault(m, n, sliderData);
			addButton(30 + i, x + 67, y + 9 + i * f, "X")
					.setSize(8, 8)
					.setTexture(ANIMATION_BUTTONS)
					.setDefBack(false)
					.setIsAnim(true)
					.setShowShadow(false)
					.setColor(0xFFDC0000)
					.setUV(0, 96, 0, 0);
		}
		addCheckBox(37, x, y += 38, "emotion.can.blink", "emotion.can.no.blink", emtn.canBlink())
				.setSize(140, 14);
		resetEmtns();
		addLabel(lId, x, (y += 20) + 1, Component.translatable("ai.movement").append(":"));
		addTextField(7, x += 45,  y, 40, 12, "" + emtn.scaleMoveX)
				.setMinMaxDefault(0.05d, 1.25d, emtn.scaleMoveX)
				.setHoverTexts(Component.translatable("emotion.hover.scale.move", "X"));
		addTextField(8, x + 45,  y, 40, 12, "" + emtn.scaleMoveY)
				.setMinMaxDefault(0.05d, 1.25d, emtn.scaleMoveY)
				.setHoverTexts(Component.translatable("emotion.hover.scale.move", "Y"));
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (hasSubGui()) {
			getSubGui().drawScreen(mouseX, mouseY, partialTicks);
			return;
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
		npcEmtn.animation.updateTime();
		EmotionConfig emtn = getEmtn();
		if (emtn != null) {
			if (sw == null) { sw = new ScaledResolution(mc); }
			int wX = guiLeft + 274;
			int wY = guiTop + 71;
			int x = guiLeft + scroll.width + 10;
			drawGradientRect(wX - 1, wY - 1, wX + 141, wY + 141, 0xFF808080, 0xFF808080);

			drawVerticalLine(wX - 5, guiTop + 4, guiTop + imageHeight + 12, 0xFF808080);

			drawHorizontalLine(x - 2, x + 138, guiTop + 28, 0xFF808080);
			drawHorizontalLine(x - 2, x + 138, guiTop + 133, 0xFF808080);
			drawHorizontalLine(x - 2, x + 138, guiTop + 172, 0xFF808080);

			GlStateManager.pushMatrix();
			GL11.glEnable(GL11.GL_SCISSOR_TEST);
			int c = sw.getScaledWidth() < mc.displayWidth ? (int) Math.round((double) mc.displayWidth / (double) sw.getScaledWidth()) : 1;
			GL11.glScissor(wX * c, mc.displayHeight - (wY + 140) * c, 140 * c, 140 * c);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

			drawGradientRect(wX, wY, wX + 140, wY + 140, GuiNpcAnimation.backColor, GuiNpcAnimation.backColor);
			drawNpc(npcEmtn, 344, 544, 8.2f, 0, 0, 0);

			GL11.glDisable(GL11.GL_SCISSOR_TEST);
			GlStateManager.popMatrix();
		}
	}

	@Override
	public void onClose() {
		GuiTextFieldNop.unfocus();
		save();
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setBoolean("save", true);
		CustomNPCsScheduler.runTack(() -> Packets.sendServer(new SPacketAnimationSave(animation.save(new NBTTagCompound()))), 500);
		CustomNpcs.proxy.openGui(npc, EnumGuiType.MainMenuAdvanced, null);
	}

	@Override
	public void save() {
		EmotionConfig emtn = getEmtn();
		if (emtn != null) {
			Packets.sendServer(new SPacketEmotionSave(emtn.save()));
		}
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0) {
			if (selEmtn.getFormattedText().equals(scroll.getSelected())) { return; }
			save();
			selEmtn = scroll.getNormalSelected();
		}
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { initGui(); }

	@Override
	public void setGuiData(NBTTagCompound compound) {
		animation.load(compound);
		initGui();
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiEditText && !((SubGuiEditText) subgui).cancelled) {
			EmotionConfig newEmtn = aData.createNewEmtn();
			newEmtn.name = ((SubGuiEditText) subgui).text[0];
			selEmtn = newEmtn.getSettingName();
			Packets.sendServer(new SPacketEmotionSave(newEmtn.save()));
			initGui();
		} // add new
	}

	private EmotionConfig getEmtn() {
		if (!dataEmtns.containsKey(selEmtn)) {
			selEmtn = Component.empty();
			return null;
		}
		return dataEmtns.get(selEmtn);
	}

	private void resetEmtns() {
		EmotionConfig emtn = getEmtn();
		ModelPartData model = ((EntityCustomNpc) npcEmtn).modelData.getPartData(EnumParts.EYES);
		if (model instanceof ModelEyeData) { modelEye = (ModelEyeData) model; }
		if (emtn == null || npcEmtn == null) { return; }
		emtn.editFrame = onlyPart ? frame.id : -1;
		emtn.isEdit = true;
		npcEmtn.display.setName(npc.getName()+"_edit_emotion");
		npcEmtn.setHealth(npcEmtn.getMaxHealth());
		npcEmtn.deathTime = 0;
		npcEmtn.animation.tryRunEmotion(emtn);
	}

	@Override
	public void mouseDragged(GuiSliderNop slider) {
		float value;
		int pos = slider.id + (isRight ? 0 : 2);
		switch(elementType) {
			case 1: { // pupil
				switch(toolType) { // 0 - rotation, 1 - offset, 2 - scale
					case 1: {
						frame.offsetPupil[pos] = (value = slider.getValue() * 2.5f - (isRight ? 1.0f : 1.5f));
						break;
					}
					case 2: {
						frame.offsetPupil[pos] = (value = slider.getValue() * 2.0f);
						break;
					}
					default: {
						frame.rotPupil[isRight ? 0 : 1] = (value = slider.getValue() * 360.0f - 180.0f);
						break;
					}
				}
				break;
			}
			case 2: { // brow
				switch(toolType) {
					case 1: {
						frame.offsetBrow[pos] = (value = slider.getValue() * 2.5f - (isRight ? 1.0f : 1.5f));
						break;
					}
					case 2: {
						frame.scaleBrow[pos] = (value = slider.getValue() * 2.0f);
						break;
					}
					default: {
						frame.rotBrow[isRight ? 0 : 1] = (value = slider.getValue() * 360.0f - 180.0f);
						break;
					}
				}
				break;
			}
			case 3: { // mouth
				switch(toolType) {
					case 1: {
						frame.offsetMouth[slider.id] = (value = slider.getValue() * 2.5f - (isRight ? 1.0f : 1.5f));
						break;
					}
					case 2: {
						frame.scaleMouth[slider.id] = (value = slider.getValue() * 2.0f);
						break;
					}
					default: {
						frame.rotMouth = (value = slider.getValue() * 360.0f - 180.0f);
						break;
					}
				}
				break;
			}
			default: { // eye
				elementType = 0;
				switch(toolType) {
					case 1: {
						frame.offsetEye[pos] = (value = slider.getValue() * 2.5f - (isRight ? 1.0f : 1.5f));
						break;
					}
					case 2: {
						frame.scaleEye[pos] = (value = slider.getValue() * 2.0f);
						break;
					}
					default: {
						frame.rotEye[isRight ? 0 : 1] = (value = slider.getValue() * 360.0f - 180.0f);
						break;
					}
				}
				break;
			}
		}
		if (getTextField(5 + slider.id) != null) { getTextField(5 + slider.id).setValue("" + (Math.round(value * 1000.0f) / 1000.0f)); }
		resetEmtns();
	}

	@Override
	public void mousePressed(GuiSliderNop slider) { }

	@Override
	public void mouseReleased(GuiSliderNop slider) { }

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		EmotionConfig emtn = getEmtn();
		if (hasSubGui() || emtn == null) { return; }
		switch (textField.id) {
			case 0: {
				emtn.name = textField.getValue();
				selEmtn = emtn.getSettingName();
				initGui();
				break;
			} // renames
			case 1: {
				if (frame == null) { return; }
				frame.setSpeed(textField.getInteger());
				resetEmtns();
				break;
			} // speed
			case 2: {
				if (frame == null) { return; }
				frame.setEndDelay(textField.getInteger());
				resetEmtns();
				break;
			} // delay
			case 3: {
				break;
			} // repeat
			case 5: {
				float value = 0.0f;
				float data = (float) textField.getDouble();
				switch(elementType) {
					case 0: {
						switch (toolType) {
							case 1: {
								value = data / 3.0f + 0.5f;
								frame.offsetEye[isRight ? 0 : 2] = data;
								break;
							}
							case 2: {
								value = data * 0.5f;
								frame.scaleEye[isRight ? 0 : 2] = data;
								break;
							}
							default: {
								value = data * 0.0027778f + 0.5f;
								frame.rotEye[isRight ? 0 : 1] = data;
								break;
							}
						}
						break;
					} // Eye
					case 1: {
						switch (toolType) {
							case 1: {
								value = data / 3.0f + 0.5f;
								frame.offsetPupil[isRight ? 0 : 2] = data;
								break;
							}
							case 2: {
								value = data * 0.5f;
								frame.scalePupil[isRight ? 0 : 2] = data;
								break;
							}
							default: {
								value = data * 0.0027778f + 0.5f;
								frame.rotPupil[isRight ? 0 : 1] = data;
								break;
							}
						}
						break;
					} // Pupil
					case 2: {
						switch (toolType) {
							case 1: {
								value = data / 3.0f + 0.5f;
								frame.offsetBrow[isRight ? 0 : 2] = data;
								break;
							}
							case 2: {
								value = data * 0.5f;
								frame.scaleBrow[isRight ? 0 : 2] = data;
								break;
							}
							default: {
								value = data * 0.0027778f + 0.5f;
								frame.rotBrow[isRight ? 0 : 1] = data;
								break;
							}
						}
						break;
					} // Brow
					case 3: {
						switch (toolType) {
							case 1: {
								value = data / 3.0f + 0.5f;
								frame.offsetMouth[0] = data;
								break;
							}
							case 2: {
								value = data * 0.5f;
								frame.scaleMouth[0] = data;
								break;
							}
							default: {
								value = data * 0.0027778f + 0.5f;
								frame.rotMouth = data;
								break;
							}
						}
						break;
					} // Mouth
				}
				textField.setValue("" + (Math.round(data * 1000.0f) / 1000.0f));
				if (getSlider(0) != null) { getSlider(0).setValue(value); }
				resetEmtns();
				break;
			} // X
			case 6: {
				float value = 0.0f;
				float data = (float) textField.getDouble();
				switch(elementType) {
					case 0: {
                        if (toolType == 1) {
                            value = data / 3.0f + 0.5f;
                            frame.offsetEye[isRight ? 1 : 3] = data;
                        } else {
                            value = data * 0.5f;
                            frame.scaleEye[isRight ? 1 : 3] = data;
                        }
						break;
					} // Eye
					case 1: {
						if (toolType == 1) {
							value = data / 3.0f + 0.5f;
							frame.offsetPupil[isRight ? 1 : 3] = data;
						} else {
							value = data * 0.5f;
							frame.scalePupil[isRight ? 1 : 3] = data;
						}
						break;
					} // Pupil
					case 2: {
						if (toolType == 1) {
							value = data / 3.0f + 0.5f;
							frame.offsetBrow[isRight ? 1 : 3] = data;
						} else {
							value = data * 0.5f;
							frame.scaleBrow[isRight ? 1 : 3] = data;
						}
						break;
					} // Brow
					case 3: {
						if (toolType == 1) {
							value = data / 3.0f + 0.5f;
							frame.offsetMouth[1] = data;
						} else {
							value = data * 0.5f;
							frame.scaleMouth[1] = data;
						}
						break;
					} // Mouth
				}
				textField.setValue("" + (Math.round(data * 1000.0f) / 1000.0f));
				if (getSlider(1) != null) { getSlider(1).setValue(value); }
				resetEmtns();
				break;
			} // Y
			case 7: emtn.scaleMoveX = (float) textField.getDouble(); break; // scale move x
			case 8: emtn.scaleMoveY = (float) textField.getDouble(); break; // scale move y
		}
	}

}
