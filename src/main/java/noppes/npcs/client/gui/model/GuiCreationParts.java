package noppes.npcs.client.gui.model;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.parts.ModelPartData;
import noppes.npcs.client.gui.select.SubGuiTextureSelection;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.client.model.part.LayerModel;
import noppes.npcs.client.model.part.ModelEyeData;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.containers.ContainerLayer;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketItemChange;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;

public class GuiCreationParts extends GuiCreationScreenInterface<ContainerLayer>
		implements ITextfieldListener, ICustomScrollListener, ISliderListener {

	public class GuiPart {

		protected Object[] types;
		protected ModelPartData data;
		protected EnumParts part;
		protected boolean canBeDeleted;
		protected boolean hasPlayerOption;
		protected boolean noPlayerTypes;
		protected final GuiCreationParts parent;
		public int patterns;

		public GuiPart(EnumParts partIn, GuiCreationParts parentIn) {
			super();

			parent = parentIn;
			patterns = 0;
			types = new String[] { "gui.none" };
			hasPlayerOption = true;
			noPlayerTypes = false;
			canBeDeleted = true;
			part = partIn;
			data = playerdata.getPartData(partIn);
		}

		public boolean buttonEvent(@Nonnull GuiButtonNop button) {
			switch (button.id) {
				case 20: {
					int i = button.getValue();
					if (i == 0 && canBeDeleted) { playerdata.removePart(part); }
					else {
						data = playerdata.getOrCreatePart(part);
						data.pattern = 0;
						data.setType(i - 1);
					}
					parent.initGui();
					return true;
				}
				case 21: {
					if (data != null && button instanceof GuiButtonYesNo) { data.playerTexture = ((GuiButtonYesNo) button).getBoolean(); }
					parent.initGui();
					return true;
				}
				case 22: {
					data.pattern = (byte) button.getValue();
					parent.initGui();
					return true;
				}
				case 23: {
					setSubGui(new SubGuiModelColor(GuiCreationParts.this, data.color, color -> data.color = color));
					parent.initGui();
					return true;
				}
			}
			return false;
		}

		public int initGui() {
			data = playerdata.getPartData(part);
			int x0 = guiLeft + 123;
			int x1 = guiLeft + 175;
			int y = guiTop + 50;
			if (data != null || !noPlayerTypes) {
				addLabel(20, x0, y + 5, "gui.type")
						.setColor(CustomNpcs.MainColor.getRGB());
				addButton(20, x1, y, true, (data == null) ? 0 : (data.type + 1), types)
						.setSize(100, 20)
						.setHoverTexts("display.hover.part.type");
				y += 25;
			}
			if (data != null && hasPlayerOption) {
				addLabel(21, x0, y + 5, "gui.playerskin")
						.setColor(CustomNpcs.MainColor.getRGB());
				addYesNo(21, x1, y, data.playerTexture)
						.setHoverTexts("display.hover.part.skin");
				y += 25;
			}
			if (data != null && !data.playerTexture) {
				addLabel(23, x0, y + 5, "gui.color")
						.setColor(CustomNpcs.MainColor.getRGB());
				addButton(23, x1, y, data.color)
						.setHoverTexts("display.hover.part.color");
				y += 25;
			}
			return y;
		}

		public GuiPart noPlayerOptions() { hasPlayerOption = false; return this; }

		public GuiPart noPlayerTypes() { noPlayerTypes = true; return this; }

		public GuiPart setTypes(String[] typesIn) { types = typesIn; return this; }

		public void subGuiClosed(GuiScreen subgui) { }

	}

	class GuiPartBeard extends GuiPart {

		public GuiPartBeard(GuiCreationParts parentIn) {
			super(EnumParts.BEARD, parentIn);
			noPlayerTypes().types = new String[] { "gui.none", "1", "2", "3", "4" };
		}

	}

	class GuiPartClaws extends GuiPart {

		public GuiPartClaws(GuiCreationParts parentIn) {
			super(EnumParts.CLAWS, parentIn);
			types = new String[] { "gui.none", "gui.show" };
		}

		@Override
		public int initGui() {
			int y = super.initGui();
			if (data == null) { return y; }
			addLabel(22, guiLeft + 123, y + 5, "gui.pattern")
					.setColor(CustomNpcs.MainColor.getRGB());
			addButton(22, guiLeft + 175, y, true, data.pattern, "gui.both", "gui.left", "gui.right")
					.setSize(100, 20)
					.setHoverTexts("display.hover.part.pattern");
			return y;
		}

	}

	class GuiPartEyes extends GuiPart {
		
		private final ModelEyeData eyes;

		public GuiPartEyes(GuiCreationParts parentIn) {
			super(EnumParts.EYES, parentIn);
			types = new String[] { "gui.none", "gui.small", "gui.normal", "gui.select" };
			noPlayerOptions();
			canBeDeleted = false;
			eyes = (ModelEyeData) data;
		}

		@Override
		public boolean buttonEvent(@Nonnull GuiButtonNop button) {
			switch (button.id) {
				case 23: setSubGui(new SubGuiModelColor(GuiCreationParts.this, eyes.eyeColor[1], color -> eyes.eyeColor[1] = color)); return true;
				case 24: setSubGui(new SubGuiModelColor(GuiCreationParts.this, eyes.eyeColor[0], color -> eyes.eyeColor[0] = color)); return true;
				case 25: setSubGui(new SubGuiModelColor(GuiCreationParts.this, eyes.pupilColor[0], color -> eyes.pupilColor[0] = color)); return true;
				case 26: setSubGui(new SubGuiModelColor(GuiCreationParts.this, eyes.pupilColor[1], color -> eyes.pupilColor[1] = color)); return true;
				case 27: setSubGui(new SubGuiModelColor(GuiCreationParts.this, eyes.browColor[1], color -> eyes.browColor[1] = color)); return true;
				case 28: setSubGui(new SubGuiModelColor(GuiCreationParts.this, eyes.browColor[0], color -> eyes.browColor[0] = color)); return true;
				case 29: eyes.browThickness = button.getValue(); return true;
				case 30: setSubGui(new SubGuiModelColor(GuiCreationParts.this, eyes.skinColor, color -> eyes.skinColor = color)); return true;
				case 31: eyes.closed = button.getValue(); return true;
				case 32: eyes.eyePos = button.getValue() - 1; return true;
				case 33: eyes.glint = ((GuiButtonYesNo) button).getBoolean(); return true;
				case 34: setSubGui(new SubGuiTextureSelection(GuiCreationParts.this, 0, null, eyes.eyeRight.toString(), "png", 5)); return true;
				case 35: setSubGui(new SubGuiTextureSelection(GuiCreationParts.this, 1, null, eyes.eyeLeft.toString(), "png", 5)); return true;
				case 36: setSubGui(new SubGuiTextureSelection(GuiCreationParts.this, 2, null, eyes.pupilRight.toString(), "png", 5)); return true;
				case 37: setSubGui(new SubGuiTextureSelection(GuiCreationParts.this, 3, null, eyes.pupilLeft.toString(), "png", 5)); return true;
				case 38: setSubGui(new SubGuiTextureSelection(GuiCreationParts.this, 4, null, eyes.browRight.toString(), "png", 5)); return true;
				case 39: setSubGui(new SubGuiTextureSelection(GuiCreationParts.this, 5, null, eyes.browLeft.toString(), "png", 5)); return true;
				case 40: eyes.reset(); parent.initGui(); return true;
				case 41: eyes.activeRight = ((GuiButtonYesNo) button).getBoolean(); return true;
				case 42: eyes.activeLeft = ((GuiButtonYesNo) button).getBoolean(); return true;
				case 43: eyes.activeCenter = ((GuiButtonYesNo) button).getBoolean(); return true;
				case 44: setSubGui(new SubGuiModelColor(GuiCreationParts.this, eyes.centerColor, color -> eyes.centerColor = color)); return true;
				case 45:
					setSubGui(new SubGuiModelColor(GuiCreationParts.this, eyes.eyeColor[0], color -> {
						eyes.eyeColor[0] = color;
						eyes.eyeColor[1] = color;
					}));
					return true;
				case 46:
					setSubGui(new SubGuiModelColor(GuiCreationParts.this, eyes.pupilColor[0], color -> {
						eyes.pupilColor[0] = color;
						eyes.pupilColor[1] = color;
					}));
					return true;
				case 47:
					setSubGui(new SubGuiModelColor(GuiCreationParts.this, eyes.browColor[0], color -> {
						eyes.browColor[0] = color;
						eyes.browColor[1] = color;
					}));
					return true;
				default: return super.buttonEvent(button);
			}
		}

		@Override
		public int initGui() {
			int y = super.initGui(); // button IDs: 20 ... 23 
			if (data != null && eyes.isEnabled()) {
				int x0 = guiLeft + 123;
				int x1 = guiLeft + 175;
				y = guiTop + 50;
				getLabel(20).setY(y + 3);
				GuiButtonNop button = getButton(20);
				button.setX(x1);
				button.setY(y);
				button.setHeight(14);
				if (eyes.type != -1) {
					addButton(40, x1 + 104, y, "RND")
							.setSize(31, 14)
							.setHoverTexts("display.hover.part.rnd");
				}
				// eye color
				y += 16;
				// left
				getLabel(23).setMessage("eye.color.0");
				getLabel(23).setY(y + 3);
				button = getButton(23).setSize(40, 14);
				button.setX(x1);
				button.setY(y);
				getButton(23).setHoverTexts("display.hover.part.eye.color.r").setColor(eyes.eyeColor[1]);
				addButton(45, x1 + 42, y, "-")
						.setSize(18, 14)
						.setHoverTexts("display.hover.part.eye.color");
				// right
				addButton(24, x1 + 62, y, eyes.eyeColor[0])
						.setSize(40, 14)
						.setColor(eyes.eyeColor[0])
						.setHoverTexts("display.hover.part.eye.color.l");
				if (data.type == 2) {
					addButton(34, x1 + 104, y, "EL")
							.setSize(14, 14)
							.setHoverTexts("display.hover.part.eye.txr.r");
					addButton(35, x1 + 120, y, "ER")
							.setSize(14, 14)
							.setHoverTexts("display.hover.part.eye.txr.l");
				}
				// pupil color
				// left
				addLabel(25, x0, (y += 16) + 3, "eye.color.1")
						.setColor(CustomNpcs.MainColor.getRGB());
				addButton(25, x1, y, eyes.pupilColor[0])
						.setSize(40, 14)
						.setColor(eyes.pupilColor[0])
						.setHoverTexts("display.hover.part.pupil.color.r");
				addButton(46, x1 + 42, y, "-")
						.setSize(18, 14)
						.setHoverTexts("display.hover.part.pupil.color");
				// right
				addButton(26, x1 + 62, y, eyes.pupilColor[1])
						.setSize(40, 14)
						.setColor(eyes.pupilColor[1])
						.setHoverTexts("display.hover.part.pupil.color.l");
				if (data.type == 2) {
					addButton(36, x1 + 104, y, "PL")
							.setSize(14, 14)
							.setHoverTexts("display.hover.part.pupil.txr.r");
					addButton(37, x1 + 120, y, "PR")
							.setSize(14, 14)
							.setHoverTexts("display.hover.part.pupil.txr.l");
				}
				// center
				addLabel(44, x0, (y += 16) + 3, "eye.color.2")
						.setColor(CustomNpcs.MainColor.getRGB());
				addButton(44, x1, y, eyes.centerColor)
						.setSize(102, 14)
						.setColor(eyes.centerColor);
				addYesNo(43, x1 + 104, y, eyes.activeCenter)
						.setSize(20, 14)
						.setHoverTexts("display.hover.part.center.active");
				// brow color
				// left
				addLabel(27, x0, (y += 16) + 3, "eye.color.3")
						.setColor(CustomNpcs.MainColor.getRGB());
				addButton(27, x1 + 62, y, eyes.browColor[1])
						.setSize(40, 14)
						.setColor(eyes.browColor[1])
						.setHoverTexts("display.hover.part.brow.color.r");
				addButton(47, x1 + 42, y, "-")
						.setSize(18, 14)
						.setHoverTexts("display.hover.part.brow.color");
				// right
				addButton(28, x1, y,  eyes.browColor[0])
						.setSize(40, 14)
						.setColor(eyes.browColor[0])
						.setHoverTexts("display.hover.part.brow.color.l");
				if (data.type == 2) {
					addButton(38, x1 + 104, y, "BL")
							.setSize(14, 14)
							.setHoverTexts("display.hover.part.brow.txr.r");
					addButton(39, x1 + 120, y, "BR")
							.setSize(14, 14)
							.setHoverTexts("display.hover.part.brow.txr.l");
				}
				// brow size
				addLabel(29, x0, (y += 16) + 3, "eye.brow")
						.setColor(CustomNpcs.MainColor.getRGB());
				addButton(29, x1, y, false, eyes.browThickness, "0", "1", "2", "3", "4", "5", "6", "7", "8")
						.setSize(102, 14)
						.setHoverTexts("display.hover.part.brow.size");
				// skin color
				addLabel(30, x0, (y += 16) + 3, "eye.lid")
						.setColor(CustomNpcs.MainColor.getRGB());
				addButton(30, x1, y, eyes.skinColor)
						.setSize(100, 14)
						.setColor(eyes.skinColor)
						.setHoverTexts("display.hover.part.skin.color");
				// both eyes
				addLabel(22, x0, (y += 16) + 3, "gui.draw")
						.setColor(CustomNpcs.MainColor.getRGB());
				addButton(22, x1, y, false, data.pattern, "gui.both", "gui.left", "gui.right")
						.setSize(102, 14)
						.setHoverTexts("display.hover.part.pattern");
				// closed
				addLabel(31, x0, (y += 16) + 3, "eye.closed")
						.setColor(CustomNpcs.MainColor.getRGB());
				addButton(31, x1, y, false, eyes.closed, "gui.none", "gui.both", "gui.left", "gui.right")
						.setSize(102, 14)
						.setHoverTexts("display.hover.part.closed");
				addYesNo(41, x1 + 104, y, eyes.activeRight)
						.setSize(20, 14)
						.setHoverTexts("display.hover.part.eye.active");
				addYesNo(42, x1 + 126, y, eyes.activeLeft)
						.setSize(20, 14)
						.setHoverTexts("display.hover.part.eye.active");
				// vertical pos
				y += 16;
				addLabel(32, x0, y + 3, "gui.position")
						.setColor(CustomNpcs.MainColor.getRGB());
				addButton(32, x1, y, false, eyes.eyePos + 1,
						Component.translatable("gui.down").append(" x2"),
						"gui.down", "gui.normal", "gui.up",
						Component.translatable("gui.up").append(" x2"))
						.setSize(102, 14)
						.setHoverTexts("display.hover.part.pos");
				// glint
				y += 16;
				addLabel(33, x0, y + 3, "eye.glint")
						.setColor(CustomNpcs.MainColor.getRGB());
				addYesNo(33, x1, y, eyes.glint)
						.setSize(102, 14)
						.setHoverTexts("display.hover.part.glint");
			}
			return y;
		}
		
		@Override
		public void subGuiClosed(GuiScreen subgui) {
			if (subgui instanceof SubGuiTextureSelection) {
				SubGuiTextureSelection tGui = (SubGuiTextureSelection) subgui;
				switch (tGui.id) {
					case 0: eyes.eyeRight = tGui.resource; break;
					case 1: eyes.eyeLeft = tGui.resource; break;
					case 2: eyes.pupilRight = tGui.resource; break;
					case 3: eyes.pupilLeft = tGui.resource; break;
					case 4: eyes.browRight = tGui.resource; break;
					case 5: eyes.browLeft = tGui.resource; break;
				}
			}
		}
		
	}

	class GuiPartHair extends GuiPart {

		public GuiPartHair(GuiCreationParts parentIn) {
			super(EnumParts.HAIR, parentIn);
			noPlayerTypes().types = new String[] { "gui.none", "1", "2", "3", "4" };
		}

	}

	class GuiPartHorns extends GuiPart {

		public GuiPartHorns(GuiCreationParts parentIn) {
			super(EnumParts.HORNS, parentIn);
			types = new String[] { "gui.none", "horns.bull", "horns.antlers", "horns.antenna" };
		}

		@Override
		public int initGui() {
			int y = super.initGui();
			if (data != null && data.type == 2) {
				addLabel(22, guiLeft + 123, y + 5, "gui.pattern")
						.setColor(CustomNpcs.MainColor.getRGB());
				addButton(22, guiLeft + 175, y, true, data.pattern, "1", "2")
						.setSize(100, 20)
						.setHoverTexts("display.hover.part.pattern");
			}
			return y;
		}

	}

	class GuiPartLegs extends GuiPart {

		public GuiPartLegs(GuiCreationParts parentIn) {
			super(EnumParts.LEGS, parentIn);
			types = new String[] { "gui.none", "gui.normal", "legs.naga", "legs.spider", "legs.horse", "legs.mermaid", "legs.digitigrade" };
			canBeDeleted = false;
		}

		@Override
		public boolean buttonEvent(@Nonnull GuiButtonNop button) {
			if (button.id == 20) {
				int i = button.getValue();
				data.playerTexture = (i <= 1);
				return true;
			}
			super.buttonEvent(button);
			return false;
		}

		@Override
		public int initGui() {
			hasPlayerOption = (data.type == 1 || data.type == 5);
			return super.initGui();
		}

	}

	class GuiPartParticles extends GuiPart {

		public GuiPartParticles(GuiCreationParts parentIn) {
			super(EnumParts.PARTICLES, parentIn);
			types = new String[] { "gui.none", "1", "2" };
		}

		@Override
		public int initGui() { return super.initGui(); }

	}

	class GuiPartSnout extends GuiPart {
		public GuiPartSnout(GuiCreationParts parentIn) {
			super(EnumParts.SNOUT, parentIn);
			types = new String[] { "gui.none", "snout.small", "snout.medium", "snout.large", "snout.bunny", "snout.beak" };
		}
	}

	class GuiPartTail extends GuiPart {
		public GuiPartTail(GuiCreationParts parentIn) {
			super(EnumParts.TAIL, parentIn);
			types = new String[] { "gui.none", "part.tail", "tail.dragon", "tail.horse", "tail.squirrel", "tail.fin", "tail.rodent", "tail.bird", "tail.fox" };
		}

		@Override
		public int initGui() {
			data = playerdata.getPartData(part);
			hasPlayerOption = (data != null && (data.type == 0 || data.type == 1 || data.type == 6 || data.type == 7));
			int y = super.initGui();
			if (data != null && data.type == 0) {
				addLabel(22, guiLeft + 123, y + 5, "gui.pattern")
						.setColor(CustomNpcs.MainColor.getRGB());
				addButton(22, guiLeft + 175, y, true, data.pattern, "1", "2")
						.setSize(100, 20)
						.setHoverTexts("display.hover.part.pattern");
			}
			return y;
		}
	}

	class GuiPartWings extends GuiPart {

		public GuiPartWings(GuiCreationParts parentIn) {
			super(EnumParts.WINGS, parentIn);
			setTypes(new String[] { "gui.none", "1", "2", "3", "4" });
		}

		@Override
		public int initGui() { return super.initGui(); }

	}

	class GuiPartLayers extends GuiPart {

		private final ContainerLayer cont;

		public GuiCustomScrollNop scrollIn;
		public int selectPos = 0;
		public Map<Integer, EnumParts> partNames = new LinkedHashMap<>();

        public GuiPartLayers(GuiCreationParts parentIn) {
			super(EnumParts.CUSTOM_LAYERS, parentIn);
			cont = (ContainerLayer) inventorySlots;
			partNames.put(0, EnumParts.HEAD);
			partNames.put(1, EnumParts.BODY);
			partNames.put(2, EnumParts.ARM_RIGHT);
			partNames.put(3, EnumParts.ARM_LEFT);
			partNames.put(4, EnumParts.LEG_RIGHT);
			partNames.put(5, EnumParts.LEG_LEFT);
			partNames.put(6, EnumParts.BELT);
			partNames.put(7, EnumParts.WRIST_RIGHT);
			partNames.put(8, EnumParts.WRIST_LEFT);
			partNames.put(9, EnumParts.FEET_RIGHT);
			partNames.put(10, EnumParts.FEET_LEFT);
			LayerModel lm = playerdata.getLayerModel(selectPos);
			if (lm != null) {
				Packets.sendServer(new SPacketItemChange("ContainerLayer", 0, lm.getStack()));
			}
		}

		@Override
		public boolean buttonEvent(@Nonnull GuiButtonNop button) {
			switch (button.id) {
				case 21: {
					if (cont == null) { return true; }
					selectPos = playerdata.addNewLayer();
					cont.getSlot(0).putStack(ItemStack.EMPTY);
					Packets.sendServer(new SPacketItemChange("ContainerLayer", 0, ItemStack.EMPTY));
					return true;
				} // add new item layer
				case 22: {
					selectPos = playerdata.removeLayer(selectPos);
					LayerModel lm = playerdata.getLayerModel(selectPos);
					ItemStack stack;
					if (lm == null) {
						stack = ItemStack.EMPTY;
						cont.getSlot(0).putStack(ItemStack.EMPTY);
					}
					else {
						stack = lm.getStack();
						cont.getSlot(0).putStack(lm.getStack());
					}
					Packets.sendServer(new SPacketItemChange("ContainerLayer", 0, stack));
					parent.initGui();
					return true;
				} // remove item layer
				case 23: {
					if (toolType == 1) { return true; }
					GuiTextFieldNop.unfocus();
					toolType = 1;
					parent.initGui();
					return true;
				} // select tool pos
				case 24: {
					if (toolType == 0) { return true; }
					GuiTextFieldNop.unfocus();
					toolType = 0;
					parent.initGui();
					return true;
				} // select tool rot
				case 25: {
					if (toolType == 2) { return true; }
					GuiTextFieldNop.unfocus();
					toolType = 2;
					parent.initGui();
					return true;
				} // select tool scale
				case 26: {
					resetAxis(0);
					if (showEntity instanceof EntityCustomNpc && playerdata != null){ ((EntityCustomNpc) showEntity).modelData.load(playerdata.save()); }
					return true;
				} // reset X
				case 27: {
					resetAxis(1);
					if (showEntity instanceof EntityCustomNpc && playerdata != null){ ((EntityCustomNpc) showEntity).modelData.load(playerdata.save()); }
					return true;
				} // reset Y
				case 28: {
					resetAxis(2);
					if (showEntity instanceof EntityCustomNpc && playerdata != null){ ((EntityCustomNpc) showEntity).modelData.load(playerdata.save()); }
					return true;
				} // reset Z
				case 29: {
					LayerModel lm = playerdata.getLayerModel(selectPos);
					if (lm == null) { return true; }
					lm.part = partNames.get(button.getValue());
					if (showEntity instanceof EntityCustomNpc && playerdata != null) { ((EntityCustomNpc) showEntity).modelData.load(playerdata.save()); }
					return true;
				} // reset part
				default: return super.buttonEvent(button);
			}
		}

		@Override
		public int initGui() {
			GuiTextFieldNop.unfocus();
			super.initGui();
			for (int i = 20; i < 24; i++) {
				if (getLabel(i) != null) { getLabel(i).setIsVisible(false); }
				if (getButton(i) != null) { getButton(i).setIsVisible(false); }
			}
			int x0 = guiLeft + 123;
			int y = guiTop;
			scrollIn = new GuiCustomScrollNop(parent, 1)
					.setSize(100, 127);
			LayerModel lm = playerdata.getLayerModel(selectPos);
			ItemStack stack;
			if (lm == null) {
				stack = cont.getSlot(0).getStack();
				if (!stack.isEmpty()) {
					selectPos = playerdata.addNewLayer();
					lm = playerdata.getLayerModel(selectPos);
					lm.setStack(stack);
				} else {
					selectPos = -1;
					stack = ItemStack.EMPTY;
				}
			}
			else { stack = lm.getStack(); }
			Packets.sendServer(new SPacketItemChange("ContainerLayer", 0, stack));
			scrollIn.setUnsortedList(playerdata.getLayerKeys());
			scrollIn.setSelect(selectPos);
			addLabel(20, x0, y, "part.layers.info.0")
					.setColor(CustomNpcs.MainColor.getRGB());
			int y1 = y + 141;
			addButton(21, x0, y1, "gui.add")
					.setSize(49, 20)
					.setIsEnabled(playerdata.isNoEmptyLayer());
			addButton(22, x0 + 51, y1, "gui.remove")
					.setSize(49, 20)
					.setIsEnabled(selectPos != -1);
			if (lm != null) {
				int x1 = x0 + 104;
				String objModel = lm.getOBJ() == null ? "" : lm.getOBJ().toString();
				y1 = y;
				addLabel(19, x1 + 1, y1, "OBJ Model path:");
				getLabel(19).setColor(CustomNpcs.MainColor.getRGB());
				add(new GuiTextFieldNop(parent, 25, x1, y1 += 12, 150, 16, objModel)
						.setHoverTexts("display.hover.layer.obj"));
				addLabel(22, x1 + 1, (y1 += 19) + 2, "Tool type:")
						.setColor(CustomNpcs.MainColor.getRGB());
				if (!lm.getStack().isEmpty() || lm.getOBJ() != null) {
					// tool pos
					addButton(23,x1 + 50, y1, "")
							.setSize(14, 14)
							.setTexture(GuiNPCInterface.ANIMATION_BUTTONS)
							.setDefBack(false)
							.setIsAnim(true)
							.setUV(0, 0, 24, 24)
							.setColor(toolType == 1 ?  new Color(0xFFFF4040).getRGB() :  new Color(0xFFFFFFFF).getRGB())
							.setHoverTexts("animation.hover.tool.0");
					// tool rot
					addButton(24, x1 + 66, y1, "")
							.setSize(14, 14)
							.setTexture(GuiNPCInterface.ANIMATION_BUTTONS)
							.setDefBack(false)
							.setIsAnim(true)
							.setUV(24, 0, 24, 24)
							.setColor(toolType == 0 ?  new Color(0xFF40FF40).getRGB() :  new Color(0xFFFFFFFF).getRGB())
							.setHoverTexts("animation.hover.tool.1");
					// tool scale
					addButton(25, x1 + 82, y1, "")
							.setSize(14, 14)
							.setTexture(GuiNPCInterface.ANIMATION_BUTTONS)
							.setDefBack(false)
							.setIsAnim(true)
							.setUV(48, 24, 24, 24)
							.setColor(toolType == 2 ?  new Color(0xFF4040FF).getRGB() :  new Color(0xFFFFFFFF).getRGB())
							.setHoverTexts("animation.hover.tool.2");
					int f = 11;
					int id;
					y1 += 16;
					for (int i = 0; i < 3; i++) { // 26 ... 28
						id = i + 26;
						addLabel(id, x1 + 1, y1 + i * f, i == 0 ? "X:" : i == 1 ? "Y:" : "Z:")
								.setColor(CustomNpcs.MainColor.getRGB());
						float v;
						float s;
						float max;
						float min;
						switch (toolType) {
							case 0: {
								v = lm.rotation[i];
								s = 0.002778f * lm.rotation[i] + 0.5f;
								min = -180.0f;
								max = 180.0f;
								break;
							}
							case 1: {
								v = lm.offset[i];
								s = 0.2f * lm.offset[i] + 0.5f;
								min = -10.0f;
								max = 10.0f;
								break;
							}
							default: {
								v = lm.scale[i];
								s = 0.2f * lm.scale[i];
								min = 0.0f;
								max = 5.0f;
								break;
							}
						}
						String hover = "animation.hover." + (toolType == 0 ? "rotation" : toolType == 1 ? "offset" : "scale");
						add(new GuiSliderNop(parent, id, x1 + 9, y1 + i * f, s)
								.setSize(78, 8)
								.setHoverTexts(hover, i == 0 ? "X" : i == 1 ? "Y" : "Z"));
						add(new GuiTextFieldNop(parent, id, x1 + 89, y1 + i * f, 42, 8, df.format(v))
								.setMinMaxDefault(min, max, v)
								.setHoverTexts(hover, i == 0 ? "X" : i == 1 ? "Y" : "Z"));
						addButton(id, x1 + 133, y1 + i * f, "X")
								.setSize(8, 8)
								.setTexture(GuiNPCInterface.ANIMATION_BUTTONS)
								.setDefBack(false)
								.setIsAnim(true)
								.setUV(0, 96, 0, 0)
								.setColor(0xFFDC0000)
								.setShowShadow(false)
								.setHoverTexts("animation.hover.reset." + toolType, i == 0 ? "X" : i == 1 ? "Y" : "Z");
					}  // 26 ... 28
					y1 += f * 3 + 2;
				}
				int pos = 0;
				Object[] names = new String[partNames.size()];
				for (int i : partNames.keySet()) {
					if (partNames.get(i) == lm.part) { pos = i; }
					names[i] = "part." + partNames.get(i).name;
				}
				addButton(29, x1, y1, true, pos, names)
						.setSize(78, 14)
						.setHoverTexts("display.hover.layer.type");
			}
			y += 12;
			add(scrollIn.setPos(x0, y));
			if (showEntity instanceof EntityCustomNpc && playerdata != null){ ((EntityCustomNpc) showEntity).modelData.load(playerdata.save()); }
			return y;
		}

	}

	private static final DecimalFormat df = new DecimalFormat("#.####");
	private static int selected = 0;
	private final GuiPart[] parts;
	private GuiCustomScrollNop scroll;
	private boolean isCheck = false;
	private int waitKey;
	private int waitKeyID;
	private int toolType; // 0 - rotation, 1 - offset, 2 - scale

	public GuiCreationParts(EntityNPCInterface npc, ContainerLayer container) {
		super(npc, container);
		parts = new GuiPart[] {
				new GuiPart(EnumParts.EARS, this).setTypes(new String[] { "gui.none", "gui.normal", "ears.bunny" }),
				new GuiPartHorns(this),
				new GuiPartHair(this),
				new GuiPart(EnumParts.MOHAWK, this).setTypes(new String[] { "gui.none", "1", "2" }).noPlayerOptions(),
				new GuiPartSnout(this),
				new GuiPartBeard(this),
				new GuiPart(EnumParts.FIN, this).setTypes(new String[] { "gui.none", "fin.shark", "fin.reptile" }),
				new GuiPart(EnumParts.BREASTS, this).setTypes(new String[] { "gui.none", "1", "2", "3" }).noPlayerOptions(),
				new GuiPartWings(this),
				new GuiPartClaws(this),
				new GuiPart(EnumParts.SKIRT, this).setTypes(new String[] { "gui.none", "gui.normal" }), new GuiPartLegs(this),
				new GuiPartTail(this),
				new GuiPartEyes(this),
				new GuiPartParticles(this),
				new GuiPartLayers(this) };
		active = 2;
		closeOnEsc = false;
		Arrays.sort(parts, (o1, o2) -> {
			String s1 = Component.translatable("part." + o1.part.name).getString();
			String s2 = Component.translatable("part." + o2.part.name).getString();
			return s1.compareToIgnoreCase(s2);
		});
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		if (getPart() != null && getPart().buttonEvent(button)) { return; }
		super.buttonEvent(button);
	}
	
	protected GuiPart getPart() { return parts[GuiCreationParts.selected]; }

	@Override
	public void drawDefaultBackground() {
		super.drawDefaultBackground();
		GuiPart part = getPart();
		if (part instanceof GuiPartLayers) {
			GlStateManager.pushMatrix();
			GlStateManager.translate( guiLeft + 121.0f, guiTop + 163.0f, 0.0f);
			mc.getTextureManager().bindTexture(GuiNPCInterface.RESOURCE_SLOT);
			int x;
			int y;
			for (Slot slot : ((GuiPartLayers) part).cont.inventorySlots) {
				if (slot.slotNumber == 0) {
					x = 164;
					y = 0;
				}
				else {
					x = ((slot.slotNumber - 1) % 9) * 18;
					y = ((slot.slotNumber - 1) / 9) * 18;
				}
				drawTexturedModalRect(x, y, 0, 0, 18, 18);
			}
			GlStateManager.popMatrix();
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (waitKey != 0) { waitKey--; }
		GuiPart part = getPart();
		if (!isCheck && part instanceof GuiPartLayers && getButton(21) != null) {
			CustomNPCsScheduler.runTack(() -> {
				isCheck = true;
				getButton(21).setIsEnabled(playerdata.isNoEmptyLayer()); // add
				LayerModel lm = playerdata.getLayerModel(((GuiPartLayers) getPart()).selectPos);
				GuiCustomScrollNop scrollIn = ((GuiPartLayers) part).scrollIn;
				ItemStack stack = ((GuiPartLayers) part).cont.getSlot(0).getStack();
				if (lm == null) {
					if (scrollIn.hasSelected() || !stack.isEmpty()) { initGui(); }
				}
				else if (lm.getStack() != stack) {
					lm.setStack(((GuiPartLayers) part).cont.getSlot(0).getStack());
					initGui();
				}
				isCheck = false;
			});
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		if (entity != null) { openGui(new GuiCreationExtra(npc, (ContainerLayer) inventorySlots)); return; }
		if (scroll == null) {
			List<Component> list = new ArrayList<>();
			for (GuiPart part : parts) { list.add(Component.translatable("part." + part.part.name)); }
			scroll = addScroll(0).setUnsortedList(list);
		}
		add(scroll.setPos(guiLeft, guiTop + 46)
				.setSize(121, ySize - 74));
		if (getPart() != null) {
			scroll.setSelected(Component.translatable("part." + getPart().part.name));
			getPart().initGui();
		}
		if (inventorySlots instanceof ContainerLayer) {
			boolean bo = getPart() instanceof GuiPartLayers;
			int x;
			int y;
			for (Slot slot : inventorySlots.inventorySlots) {
				if (slot.slotNumber == 0) {
					x = 164;
					y = 0;
				}
				else {
					x = ((slot.slotNumber - 1) % 9) * 18;
					y = ((slot.slotNumber - 1) / 9) * 18;
					if ((slot.slotNumber - 1) < 9) { y += 54;} else if ((slot.slotNumber - 1) != 36) { y -= 18;}
				}
				slot.xPos = bo ? 122 + x : -5000;
				slot.yPos = bo ? 164 + y : -5000;
			}
		}
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0 && scroll.hasSelected()) {
			GuiCreationParts.selected = scroll.getSelectedIndex();
			initGui();
		}
		if (scroll.id == 1 && getPart() instanceof GuiPartLayers) {
			GuiPartLayers part = (GuiPartLayers) getPart();
			part.selectPos = scroll.getSelectedIndex();
			LayerModel lm = playerdata.getLayerModel(part.selectPos);
			if (lm == null) { part.cont.getSlot(0).putStack(ItemStack.EMPTY); }
			else { part.cont.getSlot(0).putStack(lm.getStack()); }
			part.initGui();
		}
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }
	
	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (getPart() != null) { getPart().subGuiClosed(subgui); }
		initGui();
	}

	@Override
	public boolean keyPressed(char typedChar, int keyCode) {
		if (!hasSubGui() && getPart() instanceof GuiPartLayers) {
			// tool pos - Alt + Q
			if (isPressAndKey(Keyboard.KEY_Q) && toolType != 1) {
				toolType = 1;
				playButtonClick();
				initGui();
				return true;
			}
			// tool rot - Alt + W
			if (isPressAndKey(Keyboard.KEY_W) && toolType != 0) {
				toolType = 0;
				playButtonClick();
				initGui();
				return true;
			}
			// tool rot - Alt + E
			if (isPressAndKey(Keyboard.KEY_E) && toolType != 2) {
				toolType = 2;
				playButtonClick();
				initGui();
				return true;
			}
		}
		return super.keyPressed(typedChar, keyCode);
	}

	private void playButtonClick() { mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)); }

	private boolean isPressAndKey(int keyCode) {
		if (waitKey > 0 && waitKeyID == keyCode) { return false; }
		boolean isPress = isAltKeyDown() && Keyboard.isKeyDown(keyCode);
		if (isPress) { waitKey = 30; waitKeyID = keyCode; }
		return isPress;
	}

	@Override
	public void mouseDragged(GuiSliderNop slider) {
		if (getPart() instanceof GuiPartLayers) {
			switch (slider.id) {
				case 26: sliderMoved(0, slider.sliderValue); break; // X
				case 27: sliderMoved(1, slider.sliderValue); break; // Y
				case 28: sliderMoved(2, slider.sliderValue); break; // Z
				default: super.mouseDragged(slider); break;
			}
			if (showEntity instanceof EntityCustomNpc && playerdata != null){ ((EntityCustomNpc) showEntity).modelData.load(playerdata.save()); }
			return;
		}
		super.mouseDragged(slider);
	}

	@Override
	public void mousePressed(GuiSliderNop slider) { }

	@Override
	public void mouseReleased(GuiSliderNop slider) { }

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (getPart() instanceof GuiPartLayers) {
			switch (textField.id) {
				case 25: {
					LayerModel lm = playerdata.getLayerModel(((GuiPartLayers) getPart()).selectPos);
					if (lm == null) { return; }
					lm.setOBJ(textField.getValue());
					if (lm.getOBJ() != null) { textField.setValue(lm.getOBJ().toString()); }
					getPart().initGui();
					break;
				} // objModel
				case 26: textField.setValue(textFieldChanged(0, (float) textField.getDouble())); break; // X
				case 27: textField.setValue(textFieldChanged(1, (float) textField.getDouble())); break; // Y
				case 28: textField.setValue(textFieldChanged(2, (float) textField.getDouble())); break; // Z
			}
			if (showEntity instanceof EntityCustomNpc && playerdata != null){
				((EntityCustomNpc) showEntity).modelData.load(playerdata.save());
			}
		}
	}

	private void sliderMoved(int id, float sliderValue) {
		LayerModel lm = playerdata.getLayerModel(((GuiPartLayers) getPart()).selectPos);
		if (lm == null) { return; }
		switch (toolType) {
			case 0: {
				lm.rotation[id] = 360.0f * sliderValue - 180.0f;
				if (getTextField(26 + id) != null) { getTextField(26 + id).setValue(df.format(lm.rotation[id])); }
				break;
			} // rotation
			case 1: {
				lm.offset[id] = 5.0f * sliderValue - 2.5f;
				if (getTextField(26 + id) != null) { getTextField(26 + id).setValue(df.format(lm.offset[id])); }
				break;
			} // offset
			default: {
				lm.scale[id] = 5.0f * sliderValue;
				if (getTextField(26 + id) != null) { getTextField(26 + id).setValue(df.format(lm.scale[id])); }
				break;
			} // scale
		}
	}

	private String textFieldChanged(int id, float textValue) {
		LayerModel lm = playerdata.getLayerModel(((GuiPartLayers) getPart()).selectPos);
		String text = "" + textValue;
		if (lm == null) { return text; }
		switch (toolType) {
			case 0: {
				while (textValue < -180.0f) { textValue += 180.0f; }
				while (textValue > 180.0f) { textValue -= 180.0f; }
				lm.rotation[id] = textValue;
				if (getSlider(26 + id) != null) { getSlider(26 + id).setSliderValue(0.002778f * lm.rotation[id] + 0.5f); }
				text = df.format(lm.rotation[id]);
				break;
			} // rotation
			case 1: {
				lm.offset[id] = ValueUtil.correctFloat(textValue, -10.0f, 10.0f);
				if (getSlider(26 + id) != null) { getSlider(26 + id).setSliderValue(0.2f * lm.offset[id] + 0.5f); }
				text = df.format(lm.offset[id]);
				break;
			} // offset
			default: {
				lm.scale[id] = ValueUtil.correctFloat(textValue, 0.0f, 5.0f);
				if (getSlider(26 + id) != null) { getSlider(26 + id).setSliderValue(0.2f * lm.scale[id]); }
				text = df.format(lm.scale[id]);
				break;
			} // scale
		}
		return text;
	}

	private void resetAxis(int id) {
		LayerModel lm = playerdata.getLayerModel(((GuiPartLayers) getPart()).selectPos);
		if (lm == null) { return; }
		switch (toolType) {
			case 0: {
				lm.rotation[id] = 0.0f;
				if (getTextField(26 + id) != null) { getTextField(26 + id).setValue("0"); }
				if (getSlider(26 + id) != null) { getSlider(26 + id).setSliderValue(0.002778f * lm.rotation[id] + 0.5f); }
				break;
			} // rotation
			case 1: {
				lm.offset[id] = 0.0f;
				if (getTextField(26 + id) != null) { getTextField(26 + id).setValue("0"); }
				if (getSlider(26 + id) != null) { getSlider(26 + id).setSliderValue(0.2f * lm.offset[id] + 0.5f); }
				break;
			} // offset
			default: {
				lm.scale[id] = 1.0f;
				if (getTextField(26 + id) != null) { getTextField(26 + id).setValue("1"); }
				if (getSlider(26 + id) != null) { getSlider(26 + id).setSliderValue(0.2f * lm.scale[id]); }
				break;
			} // scale
		}
	}

}
