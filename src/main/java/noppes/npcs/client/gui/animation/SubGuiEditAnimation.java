package noppes.npcs.client.gui.animation;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.minecraft.client.renderer.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.client.ClientEventHandler;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketAnimationChange;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.client.model.animation.*;
import noppes.npcs.constants.EnumAnimationStages;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.select.SubGuiSelectItemStack;
import noppes.npcs.client.gui.select.SubGuiSoundSelection;
import noppes.npcs.client.model.ModelNpcAlt;
import noppes.npcs.controllers.AnimationController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

public class SubGuiEditAnimation extends GuiNPCInterface
		implements ISliderListener, ICustomScrollListener, ITextfieldListener {

	// data
	public static int meshType = 1;
	public static boolean showHitBox = true;
	public AnimationConfig anim;
	public AnimationFrameConfig frame;
	public PartConfig part;
	public AddedPartConfig addedPartConfig;
	public AnimationDamageHitbox hitbox;
	protected GuiCustomScrollNop scrollParts;
	protected GuiCustomScrollNop scrollHitboxes;
	protected int blockType;
	protected int blockSize;
	protected int toolType;
	protected int waitKey;
	protected int waitKeyID;
	protected boolean onlyCurrentPart;
	protected boolean hovered;
	protected boolean hasExtend;
	protected boolean hoverRight;
	protected boolean hoverLeft;
	protected boolean isHitbox = false;
	protected boolean isMotion = false;
	protected boolean isChanged = true;
	protected final EntityNPCInterface npcAnim;
	protected final EntityNPCInterface npcPart;
	protected final Vec3d basePos;
	protected final Object[] blockNames;
	protected final Object[] blockSizes;
	// display
	protected int workU;
	protected int workV;
	protected int workS;
	protected int winU;
	protected int winV;
	protected int winW;
	protected int winH;
	protected int mousePressId = -1;
	protected double mousePressX = 0.0d;
	protected double mousePressY = 0.0d;
	protected final float[] dispRot = new float[] { 45.0f, 345.0f, 345.0f };
	protected final float[] dispPos = new float[] { 0.0f, 0.0f, 0.0f };
	protected float dispScale = 1.0f;
	protected float winScale;
	protected float offsetY;
	protected final Map<Component, PartConfig> dataParts = new LinkedHashMap<>();
	protected final Map<Component, AnimationDamageHitbox> dataHitboxes = new LinkedHashMap<>();
	protected final List<Entity> environmentEntitys= new ArrayList<>();
	protected final Map<BlockPos, IBlockState> environmentStates = new HashMap<>();

	protected GuiCustomWindowNop partNames;
	protected GuiCustomWindowNop tools;
	protected GuiCustomWindowNop hitboxes;
	protected double w = -1.0d;
	protected double h = -1.0d;
	protected float baseRotation = 0.0f;
	protected final int id;


	public SubGuiEditAnimation(EntityNPCInterface npc, AnimationConfig animation, int animId) {
		super(npc);
		id = animId;
		imageWidth = 240;
		imageHeight = 427;

		anim = animation;
		frame = anim.frames.get(0);
		setPart(frame.parts.get(3));
		setHitbox(frame.damageHitboxes.get(0));
		waitKey = 0;

		// Display
		toolType = 0; // 0 - rotation, 1 - offset, 2 - scale
		blockType = 0; // 0 - environment, 1 - non, 2 - stone, 3 - stairs, 4 - stone_slab, 5 - carpet
		blockSize = 2; // 0 - x1, 1 - x3, 2 - x5, 3 - x7, 4 - x9
		winScale = 1.0f;
		setEnvironment();
		onlyCurrentPart = false;

		baseRotation = npc.rotationYaw;
		basePos = new Vec3d (npc.posX, npc.posY, npc.posZ);
		npcAnim = Util.instance.copyToGUI(npc, player.world, true);
		npcAnim.display.setName(npc.getName()+"_animation");

		npcPart = Util.instance.copyToGUI(npc, player.world, true);
		npcPart.display.setName(npc.getName()+"_anim_part");
		GuiButtonNop b = new GuiButtonNop(this, 47, "", 0, 0, null);
		buttonEvent(b);
		buttonEvent(b);

		blockNames = new Component[6];
		blockNames[0] = Component.translatable("gui.environment");
		blockNames[1] = Component.translatable("gui.none");
		for (int i = 0; i < 4; i++) {
			Block block;
			switch (i) {
				case 1: block = Blocks.STONE_STAIRS; break;
				case 2: block = Blocks.STONE_SLAB; break;
				case 3: block = Blocks.CARPET; break;
				default: block = Blocks.STONE; break;
			}
			blockNames[i + 2] = Component.literal(new ItemStack(block).getDisplayName());
		}
		blockSizes = new Component[] { Component.literal("x1"),
				Component.literal("x3"),
				Component.literal("x5"),
				Component.literal("x7"),
				Component.literal("x9") };

		ModelNpcAlt.editAnimDataSelect.red = (float) (CustomNpcs.colorAnimHoverPart >> 16 & 255) / 255.0F;
		ModelNpcAlt.editAnimDataSelect.green = (float) (CustomNpcs.colorAnimHoverPart >> 8 & 255) / 255.0F;
		ModelNpcAlt.editAnimDataSelect.blue = (float) (CustomNpcs.colorAnimHoverPart & 255) / 255.0F;
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				blockType = button.getValue();
				if (getButton(16) != null) { getButton(16).setIsEnabled(blockType != 1); }
				break;
			} // block place
			case 1: blockSize = button.getValue(); break; // block size
			case 2: {
				GuiNpcAnimation.backColor = (GuiNpcAnimation.backColor == 0xFF000000 ? 0xFFFFFFFF : 0xFF000000);
				getLabel(50).setColor(GuiNpcAnimation.backColor);
				button.setColor(GuiNpcAnimation.backColor == 0xFF000000 ? 0xFF00FFFF : 0xFF008080);
				break;
			} // back color
			case 3: {
				if (anim == null || part == null || !anim.frames.containsKey(button.getValue()) || anim.frames.get(button.getValue()).id == -1) { return; }
				frame = anim.frames.get(button.getValue());
				setPart(frame.parts.get(part.id));
				setHitbox(frame.damageHitboxes.get(0));
				anim.editTick = 0;
				anim.editFrame = frame.id;
				initGui();
				break;
			} // select frame
			case 4: {
				if (anim == null) { return; }
				if (GuiScreen.isShiftKeyDown()) { // Shift pressed
					SubGuiEditText subgui = new SubGuiEditText(0, "" + (anim.frames.size() + 1));
					subgui.numbersOnly = new int[] { 0, anim.frames.size() + 1, anim.frames.size() + 1 };
					setSubGui(subgui);
				} else {
					frame = (AnimationFrameConfig) anim.addFrame(-1, frame);
					setPart(frame.parts.get(part.id));
					initGui();
				}
				break;
			} // add new (copy) frame
			case 5: {
				if (frame == null) { return; }
				ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
					if (agree) {
						if (anim == null || frame == null || anim.frames.size() <= 1) { return; }
						int f = frame.id - 1;
						if (f < 0) { f = 0; }
						anim.removeFrame(frame);
						frame = anim.frames.get(f);
						setPart(frame.parts.get(part.id));
						setHitbox(frame.damageHitboxes.get(0));
						initGui();
					}
					NoppesUtil.openGUI(player, this);
				},
						Component.translatable("animation.clear.frame", "" + (frame.id + 1)).getParent(),
						Component.translatable("message.delete").getParent());
				setScreen(guiYesNo);
				break;
			} // remove frame
			case 6: {
				if (frame!= null) {
					ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
						if (agree) {
							if (GuiScreen.isShiftKeyDown()) {
								if (anim != null) {
									for (AnimationFrameConfig f : anim.frames.values()) {
										for (PartConfig p : f.parts.values()) { p.clear(); }
									}
									initGui();
								}
							} // clear all frames
							else {
								if (frame != null) {
									for (PartConfig p : frame.parts.values()) { p.clear(); }
									initGui();
								}
							} // clear frame
						}
						NoppesUtil.openGUI(player, this);
					},
							Component.translatable("animation.clear.frame", "" + (frame.id + 1)).getParent(),
							Component.translatable("gui.clearMessage").getParent());
					setScreen(guiYesNo);
				}
				break;
			} // clear frame
			case 7: {
				if (isHitbox) {
					if (hitbox == null) { return; }
					hitbox = new AnimationDamageHitbox(frame.damageHitboxes.size());
					hitbox.clear();
					frame.damageHitboxes.put(hitbox.id, hitbox);
					initGui();
				}
				else {
					if (isMotion || anim == null || part == null || part.id == 6 || part.id == 7) { return; }
					SubGuiEditAddPart sGui = new SubGuiEditAddPart(this, npc, npcPart, null, null);
					sGui.addPart.parentPart = part.id;
					setSubGui(sGui);
				}
				break;
			} // add new part / damage hitbox
			case 8: {
				if (isHitbox) {
					if (hitbox == null) { return; }
					if (frame.damageHitboxes.size() == 1) { hitbox.clear(); }
					else {
						int id = hitbox.id;
						frame.damageHitboxes.remove(id);
						hitbox = frame.damageHitboxes.get(id - 1);
					}
					initGui();
				}
				else {
					if (isMotion || anim == null || addedPartConfig == null || part == null) { return; }
					ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
						if (agree) {
							if (anim == null || addedPartConfig == null || part == null) { return; }
							int f = addedPartConfig.id - 1;
							if (f < 0) { f = 0; }
							anim.removeAddedPart(addedPartConfig);
							frame = anim.frames.get(frame.id);
							setPart(frame.parts.get(f));
							initGui();
						}
						NoppesUtil.openGUI(player, this);
					},
							Component.translatable("animation.clear.part", "" + (part.id + 1), scrollParts.getSelected()).getParent(),
							Component.translatable("message.delete").getParent());
					setScreen(guiYesNo);
				}
				break;
			} // del part
			case 9: {
				if (part != null) {
					ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
						if (agree) {
							if (GuiScreen.isShiftKeyDown()) {
								if (anim != null && part != null) {
									for (AnimationFrameConfig f : anim.frames.values()) { f.parts.get(part.id).clear(); }
									initGui();
								}
							} // clear all part
							else {
								if (part != null) {
									part.clear();
									initGui();
								}
							} // clear part
						}
						NoppesUtil.openGUI(player, this);
					},
							Component.translatable("animation.clear.part", "" + (part.id + 1), scrollParts.getSelected()).getParent(),
							Component.translatable("gui.clearMessage").getParent());
					setScreen(guiYesNo);
				}
				break;
			} // clear part
			case 10: {
				if (anim == null || part == null) { return; }
				part.setDisable(((GuiCheckBoxNop) button).selected());
				if (GuiScreen.isShiftKeyDown()) {
					for (AnimationFrameConfig f : anim.frames.values()) { f.parts.get(part.id).setDisable(part.isDisable()); }
				}
				button.setHoverTexts(Component.translatable("animation.hover.part.disabled." + !part.isDisable())
						.append(Component.translatable("animation.hover.shift.1")));
				resetAnimation();
				break;
			} // disabled part
			case 11: {
				if (anim == null || frame == null) { return; }
				frame.setSmooth(((GuiCheckBoxNop) button).selected());
				if (GuiScreen.isShiftKeyDown()) { // Shift pressed
					for (AnimationFrameConfig f : anim.frames.values()) { f.setSmooth(frame.isSmooth()); }
				}
				button.setHoverTexts("animation.hover.smooth." + frame.isSmooth());
				resetAnimation();
				break;
			} // smooth
			case 12: setSubGui(new SubGuiColorSelector(CustomNpcs.colorAnimHoverPart)); break; // color hover
			case 13: {
				if (meshType == 0) {
					meshType = -1;
					button.setColor(0xFF360C1C);
				} else {
					meshType = 0;
					button.setColor(0xFFD93070);
				}
				if (getButton(14) != null) { getButton(14).setColor(0xFF1A0C36); }
				if (getButton(15) != null) { getButton(15).setColor(0xFF0C3620); }
				if (getButton(16) != null) { getButton(16).setColor(0xFF35360C); }
				break;
			} // reset mesh
			case 14: {
				if (meshType == 1) {
					meshType = -1;
					button.setColor(0xFF1A0C36);
				} else {
					meshType = 1;
					button.setColor(0xFF6830D9);
				}
				if (getButton(13) != null) { getButton(13).setColor(0xFF360C1C); }
				if (getButton(15) != null) { getButton(15).setColor(0xFF0C3620); }
				if (getButton(16) != null) { getButton(16).setColor(0xFF35360C); }
				break;
			} // xz mesh
			case 15: {
				if (meshType == 2) {
					meshType = -1;
					button.setColor(0xFF0C3620);
				} else {
					meshType = 2;
					button.setColor(0xFF30D980);
				}
				if (getButton(13) != null) { getButton(13).setColor(0xFF360C1C); }
				if (getButton(14) != null) { getButton(14).setColor(0xFF1A0C36); }
				if (getButton(16) != null) { getButton(16).setColor(0xFF35360C); }
				break;
			} // xy mesh
			case 16: {
				if (meshType == 3) {
					meshType = -1;
					button.setColor(0xFF35360C);
				} else {
					meshType = 3;
					button.setColor(0xFFD7D930);
				}
				if (getButton(13) != null) { getButton(13).setColor(0xFF360C1C); }
				if (getButton(14) != null) { getButton(14).setColor(0xFF1A0C36); }
				if (getButton(15) != null) { getButton(15).setColor(0xFF0C3620); }
				break;
			} // xy mesh
			case 17: {
				showHitBox = !showHitBox;
				button.setColor(showHitBox ? 0 : 0xFF808080);
				break;
			} // show NPC hitbox
			case 18: dispScale = 1.0f; break; // display reset scale
			case 19: for (int i = 0; i < 3; i++) { dispPos[i] = 0.0f; } break; // display reset pos
			case 20: {
				dispRot[0] = 45.0f;
				dispRot[1] = 345.0f;
				dispRot[2] = 345.0f;
				break;
			} // display reset rot
			case 21: {
				if (isHitbox || isMotion) { return; }
				onlyCurrentPart = !onlyCurrentPart;
				button.setUV(onlyCurrentPart ? 144 : 188, button.txrY, button.txrW, button.txrH);
				resetAnimation();
				break;
			} // show only current part or animation
			case 22: {
				if (anim == null || part == null) { return; }
				part.setShow(((GuiCheckBoxNop) button).selected());
				if (GuiScreen.isShiftKeyDown()) { // Shift pressed
					for (AnimationFrameConfig f : anim.frames.values()) { f.parts.get(part.id).setShow(part.isShow()); }
				}
				button.setHoverTexts(Component.translatable("animation.hover.part.show." + part.isShow())
						.append(Component.translatable("animation.hover.shift.1")));
				resetAnimation();
				break;
			} // show part in frame
			case 23: {
				if (toolType == 1) { return; }
				GuiTextFieldNop.unfocus();
				toolType = 1;
				initGui();
				break;
			} // select tool pos
			case 24: {
				if (toolType == 0) { return; }
				GuiTextFieldNop.unfocus();
				toolType = 0;
				initGui();
				break;
			} // select tool rot
			case 25: {
				if (toolType == 2) { return; }
				GuiTextFieldNop.unfocus();
				toolType = 2;
				initGui();
				break;
			} // select tool scale
			case 26: {
				ModelNpcAlt.editAnimDataSelect.showArmor = !ModelNpcAlt.editAnimDataSelect.showArmor;
				button.setColor(ModelNpcAlt.editAnimDataSelect.showArmor ? 0xFFFF7200 : 0xFF6F3200);
				break;
			} // show armor
			case 27: {
				if (frame == null) { return; }
				setSubGui(new SubGuiSoundSelection(this, 0, npc, frame.getStartSound()));
				break;
			} // select sound
			case 28: {
				if (frame == null) { return; }
				frame.setStartSound("");
				break;
			} // remove sound
			case 29: {
				if (partNames == null) { showPartNames(); }
				GuiCustomWindowNop window = get(partNames.id, GuiCustomWindowNop.class);
				if (window != null) {
					window.visible = true;
					button.setIsEnabled(false);
				}
				break;
			} // show parts
			case 30: {
				if (isHitbox) {
					if (hitbox != null) {
						if (toolType == 1) { hitbox.offset[0] = 0.0f; }
						else { hitbox.scale[0] = 1.0f; }
						initGui();
					}
				} else if (isMotion) {
					frame.motions[0] = 0.0f;
					initGui();
				} else if (part != null) {
					switch (toolType) {
						case 0: part.rotation[0] = 0.0f; break;
						case 1: part.offset[0] = 0.0f; break;
						case 2: part.scale[0] = 1.0f; break;
					}
					initGui();
				}
				break;
			} // reset part set X
			case 31: {
				if (isHitbox) {
					if (hitbox != null) {
						if (toolType == 1) { hitbox.offset[1] = 0.0f; }
						else { hitbox.scale[1] = 1.0f; }
						initGui();
					}
				} else if (isMotion) {
					frame.motions[1] = 0.0f;
					initGui();
				} else if (part != null) {
					switch (toolType) {
						case 0: part.rotation[1] = 0.0f; break;
						case 1: part.offset[1] = 0.0f; break;
						case 2: part.scale[1] = 1.0f; break;
					}
					initGui();
				}
				break;
			} // reset part set Y
			case 32: {
				if (isHitbox) {
					if (hitbox != null) {
						if (toolType == 1) { hitbox.offset[2] = 0.0f; }
						else { hitbox.scale[2] = 1.0f; }
						initGui();
					}
				} else if (isMotion) {
					frame.motions[2] = 0.0f;
					initGui();
				} else if (part != null) {
					switch (toolType) {
						case 0: part.rotation[2] = 0.0f; break;
						case 1: part.offset[2] = 0.0f; break;
						case 2: part.scale[2] = 1.0f; break;
					}
					initGui();
				}
				break;
			} // reset part set Z
			case 33: {
				part.rotation[3] = 0.0f;
				initGui();
				break;
			} // reset part set X1 rot
			case 34: {
				part.rotation[4] = 0.0f;
				initGui();
				break;
			} // reset part set Y1 rot
			case 35: {
				if (tools == null) { showTools(); }
				GuiCustomWindowNop window = get(tools.id, GuiCustomWindowNop.class);
				if (window != null) {
					window.visible = true;
					button.setIsEnabled(false);
				}
				break;
			} // show window tools
			case 36: {
				if (frame == null) { return; }
				frame.isNowDamage = ((GuiCheckBoxNop) button).selected();
				break;
			} // is now damage
			case 37: {
				if (frame == null) { return; }
				frame.setHoldRightStackType(button.getValue());
				int type = frame.getHoldRightStackType();
				if (type == 2) { type = 3; }
				else if (type == 3) { type = 4; }
				button.setHoverTexts("animation.hover.stack.type."+type);
				resetAnimation();
				break;
			} // right stack type
			case 38: {
				if (frame == null) { return; }
				frame.setHoldLeftStackType(button.getValue());
				int type = frame.getHoldLeftStackType();
				if (type == 3) { type = 4; }
				button.setHoverTexts("animation.hover.stack.type."+type);
				resetAnimation();
				break;
			} // left stack type
			case 39: {
				if (frame == null) { return; }
				frame.showMainHand = !frame.showMainHand;
				initGui();
				break;
			} // show main hand
			case 40: {
				if (frame == null) { return; }
				frame.showOffHand = !frame.showOffHand;
				initGui();
				break;
			} // show offhand
			case 41: {
				if (frame == null) { return; }
				frame.showHelmet = !frame.showHelmet;
				initGui();
				break;
			} // show helmet
			case 42: {
				if (frame == null) { return; }
				frame.showBody = !frame.showBody;
				initGui();
				break;
			} // show body
			case 43: {
				if (frame == null) { return; }
				frame.showLegs = !frame.showLegs;
				initGui();
				break;
			} // show legs
			case 44: {
				if (frame == null) { return; }
				frame.showFeets = !frame.showFeets;
				initGui();
				break;
			} // show feet's
			case 45: {
				showHitBoxes();
				GuiCustomWindowNop window = get(hitboxes.id, GuiCustomWindowNop.class);
				if (window != null) {
					window.visible = true;
					button.setIsEnabled(false);
				}
				break;
			} // show window hitbox
			case 46: {
				if (ModelNpcAlt.editAnimDataSelect.alpha >= 1.0f) {
					ModelNpcAlt.editAnimDataSelect.alpha = 0.25f;
					button.setColor(0xFF787758);
				} else {
					ModelNpcAlt.editAnimDataSelect.alpha = 1.0f;
					button.setColor(0xFFFFFEBF);
				}
				break;
			} // show alpha // show armor
			case 47: {
				if (baseRotation == 0.0f) { return; }
				if (npcAnim.rotationYaw != baseRotation) {
					npcAnim.rotationYaw = baseRotation;
					npcAnim.prevRotationYaw = baseRotation;
					npcAnim.rotationYawHead = npc.rotationYawHead;
					npcAnim.prevRotationYawHead = npc.prevRotationYawHead;
					npcPart.rotationYaw = baseRotation;
					npcPart.prevRotationYaw = baseRotation;
					npcPart.rotationYawHead = npc.rotationYawHead;
					npcPart.prevRotationYawHead = npc.prevRotationYawHead;
					button.setColor(0xFF96FFC0);
				} else {
					npcAnim.rotationYaw = 0.0f;
					npcAnim.prevRotationYaw = 0.0f;
					npcAnim.rotationYawHead = 0.0f;
					npcAnim.prevRotationYawHead = 0.0f;
					npcPart.rotationYaw = 0.0f;
					npcPart.prevRotationYaw = 0.0f;
					npcPart.rotationYawHead = 0.0f;
					npcPart.prevRotationYawHead = 0.0f;
					button.setColor(0xFF426C53);
				}
				break;
			} // reset NPC rotation
			case 48: {
				isMotion = true;
				button.setIsEnabled(false);
				if (toolType != 1) { toolType = 1; }

				scrollHitboxes.clearSelection();
				setHitbox(null);

				scrollParts.clearSelection();
				setPart(null);
				setHitbox(null);

				onlyCurrentPart = true;
				if (getButton(21) != null) {
					GuiButtonNop b = getButton(21);
					b.setUV(144, b.txrY, b.txrW, b.txrH);
				}
				resetAnimation();

				initGui();
				break;
			} // show motion type
			case 49: {
				dispRot[0] = 0.0f;
				dispRot[1] = 0.0f;
				dispRot[2] = 0.0f;
				break;
			} // align xy
			case 50: {
				dispRot[0] = 90.0f;
				dispRot[1] = 0.0f;
				dispRot[2] = 0.0f;
				break;
			} // align zy
			case 51: {
				dispRot[0] = 0.0f;
				dispRot[1] = 270.0f;
				dispRot[2] = 0.0f;
				break;
			} // align xz
			case 52: {
				displayRotate(180, 0);
				break;
			} // align revers
			case 53: {
				anim.editTick = button.getValue();
				resetAnimation();
				break;
			} // set animation part tick
			case 66: onClose(); break; // exit
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (hasSubGui()) {
			getSubGui().drawScreen(mouseX, mouseY, partialTicks);
			return;
		}
		if (w < 0 || h < 0) {
			ScaledResolution sw = new ScaledResolution(mc);
			w = sw.getScaledWidth();
			h = sw.getScaledHeight();
		}
		if (waitKey != 0) { waitKey--; }
		for (int i = 0; i < 2; i++) {
			EntityNPCInterface dNpc = (i == 0 ? npcAnim : npcPart);
			if (dNpc == null) { continue; }
			dNpc.animation.updateTime();
			dNpc.chasingPosZ = npc.chasingPosZ;
			dNpc.chasingPosY = npc.chasingPosY;
			dNpc.chasingPosX = npc.chasingPosX;
			dNpc.prevChasingPosZ = npc.prevChasingPosZ;
			dNpc.prevChasingPosY = npc.prevChasingPosY;
			dNpc.prevChasingPosX = npc.prevChasingPosX;
			dNpc.ticksExisted = npc.ticksExisted;
		}
		for (Entity e : environmentEntitys) { e.ticksExisted = npc.ticksExisted; }
		if (getDisplayNpc() == null) { onClose(); }
		EntityNPCInterface showNPC = getDisplayNpc();
		// Frame id
		if (getLabel(50) != null) {
			GuiLabel label = getLabel(50);
			int cId = showNPC.animation.getAnimationCurrentFrameID();
			int nId = showNPC.animation.getAnimationNextFrameID();
			Component frame = Component.translatable("animation.frame", " ID: ")
					.append(Component.literal("" + cId).withStyle(TextFormatting.GOLD));
			if (cId != nId) {
				frame.append(Component.literal(" -> ").withStyle(TextFormatting.RESET))
						.append(Component.literal("" + nId).withStyle(TextFormatting.GOLD));
			}
			label.setMessage(frame);
		}
		if (getLabel(51) != null) {
			GuiLabel label = getLabel(51);
			EnumAnimationStages stage = showNPC.animation.getAnimationStage();
			int t = showNPC.animation.getAnimationTicks();
			int s = showNPC.animation.getAnimationSpeedTicks();

			Component data = Component.empty()
					.append(Component.literal("" + t).withStyle(TextFormatting.GOLD))
					.append(Component.literal("/").withStyle(TextFormatting.RESET))
					.append(Component.literal("" + s).withStyle(TextFormatting.GOLD))
					.append(" ");
			switch (stage) {
				case Started: data.append(Component.literal("Started").withStyle(TextFormatting.GREEN)); break;
				case Looping: data.append(Component.literal("Looping").withStyle(TextFormatting.AQUA)); break;
				case Run: data.append(Component.literal("Run").withStyle(TextFormatting.YELLOW)); break;
				case Ending: data.append(Component.literal("Ending").withStyle(TextFormatting.LIGHT_PURPLE)); break;
				case Waiting: data.append(Component.literal("Waiting").withStyle(TextFormatting.RED)); break;
			}
			label.setMessage(data);
			label.setX(workU + workS / 2 - 13 - label.getWidth());
		}
		// display data
		if (Mouse.isButtonDown(mousePressId)) {
			float x = mouseX - (float) mousePressX;
			float y = mouseY - (float) mousePressY;
			if (x != 0 || y != 0) {
				if (mousePressId == 0) { displayOffset(x, y); } // LMB
				else if (mousePressId == 1) { displayRotate(x, -y); } // RMB
				mousePressX = mouseX;
				mousePressY = mouseY;
			}
		}
		else { mousePressId = -1; }
		hovered = isMouseHover(mouseX, mouseY, workU + 1, workV + 1, workS - 2, workS - 2);
		if (hovered && (tools == null || !tools.isHovered()) && (partNames == null || !partNames.isHovered())) {
			int dWheel = Mouse.getDWheel();
			if (dWheel != 0) {
				dispScale += dispScale * (dWheel < 0 ? 0.1f : -0.1f);
				if (dispScale < 0.5f) { dispScale = 0.5f; }
				else if (dispScale > 10.0f) { dispScale = 10.0f; }
				dispScale = (float) (Math.round(dispScale * 20.0d) / 20.0d);
				if (dispScale == 0.95f || dispScale == 1.05f) { dispScale = 1.0f; }
			}
		}
		// back place
		GlStateManager.pushMatrix();
		GlStateManager.translate(0.0f, 0.0f, -300.0f);
		int color = new Color(0xFFC6C6C6).getRGB();
		drawGradientRect(winU + 1, winV + 1, winU + winW - 1, winV + winH - 1, color, color);
		color = new Color(0xFF000000).getRGB();
		drawHorizontalLine(winU + 1, winU + winW - 2, winV, color);
		drawVerticalLine(winU, winV, winV + winH - 1, color);
		drawVerticalLine(winU + winW - 1, winV, winV + winH - 1, color);
		drawHorizontalLine(winU + 1, winU + winW - 2, winV + winH - 1, color);
		// work place
		color = GuiNpcAnimation.backColor == 0xFF000000 ?
				new Color(0xFFF080F0).getRGB() :
				new Color(0xFFF020F0).getRGB();
		drawGradientRect(workU, workV, workU + workS, workV + workS, color, color);
		// Slots
		int y = 86 + (hasExtend ? 14 : 0);
		GlStateManager.pushMatrix();
		mc.getTextureManager().bindTexture(RESOURCE_SLOT);
		GlStateManager.translate(winU + 3, winV + y, 0.0f);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		drawTexturedModalRect(0, 0, 0, 0, 18, 18); // right
		IItemStack stack;
		switch(frame.getHoldRightStackType()) {
			case 1: stack = npc.inventory.getProjectile(); break;
			case 2: stack = npc.inventory.getLeftHand(); break;
			case 3: stack = frame.getHoldRightStack(); break;
			case 4: stack = npc.inventory.getArmor(0); break;
			case 5: stack = npc.inventory.getArmor(1); break;
			case 6: stack = npc.inventory.getArmor(2); break;
			case 7: stack = npc.inventory.getArmor(3); break;
			default: stack = npc.inventory.getRightHand(); break;
		}
		hoverRight = isMouseHover(mouseX, mouseY, winU + 3, winV + y + 1, 16, 16);
		if (hoverRight) {
			Gui.drawRect(1, 1, 17, 17, new Color(0x80FFFFFF).getRGB());
			if (stack != null && !stack.isEmpty()) {
				List<String> list = stack.getMCItemStack().getTooltip(player, mc.gameSettings.advancedItemTooltips ? TooltipFlags.ADVANCED : TooltipFlags.NORMAL);
				if (!list.isEmpty()) { setHoverText(list); }
			}
		}
		if (stack != null && !stack.isEmpty()) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(1.0f, 1.0f, 0.0f);
			RenderHelper.enableStandardItemLighting();
			mc.getRenderItem().renderItemAndEffectIntoGUI(stack.getMCItemStack(), 0, 0);
			RenderHelper.disableStandardItemLighting();
			GlStateManager.popMatrix();
		}
		y += 20;
		mc.getTextureManager().bindTexture(RESOURCE_SLOT);
		GlStateManager.translate(0.0f, 20.0f, 0.0f);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		drawTexturedModalRect(0, 0, 0, 0, 18, 18); // left
		switch(frame.getHoldLeftStackType()) {
			case 1: stack = npc.inventory.getProjectile(); break;
			case 2: stack = npc.inventory.getRightHand(); break;
			case 3: stack = frame.getHoldLeftStack(); break;
			case 4: stack = npc.inventory.getArmor(0); break;
			case 5: stack = npc.inventory.getArmor(1); break;
			case 6: stack = npc.inventory.getArmor(2); break;
			case 7: stack = npc.inventory.getArmor(3); break;
			default: stack = npc.inventory.getLeftHand(); break;
		}
		hoverLeft = isMouseHover(mouseX, mouseY, winU + 3, winV + y + 1, 16, 16);
		if (hoverLeft) {
			Gui.drawRect(1, 1, 17, 17, new Color(0x80FFFFFF).getRGB());
			if (stack != null && !stack.isEmpty()) {
				List<String> list = stack.getMCItemStack().getTooltip(player, mc.gameSettings.advancedItemTooltips ? TooltipFlags.ADVANCED : TooltipFlags.NORMAL);
				if (!list.isEmpty()) { setHoverText(list); }
			}
		}
		if (stack != null && !stack.isEmpty()) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(1.0f, 1.0f, 0.0f);
			RenderHelper.enableStandardItemLighting();
			mc.getRenderItem().renderItemAndEffectIntoGUI(stack.getMCItemStack(), 0, 0);
			RenderHelper.disableStandardItemLighting();
			GlStateManager.popMatrix();
		}
		GlStateManager.popMatrix();
		// Lines
		y -= 80 + (hasExtend ? 14 : 0);
		color = new Color(0xFF000000).getRGB();
		for (int i = 0; i < 17; i++) { // name -> work
			drawHorizontalLine(winU + 4 + i * 8, winU + 8 + i * 8, winV + y, color);
		}
		y += 23;
		for (int i = 0; i < 17; i++) { // work -> frame
			drawHorizontalLine(winU + 4 + i * 8, winU + 8 + i * 8, winV + y, color);
		}
		y += 76 + (hasExtend ? 14 : 0);
		for (int i = 0; i < 17; i++) { // frame -> part
			drawHorizontalLine(winU + 4 + i * 8, winU + 8 + i * 8, winV + y, color);
		}
		y += 38;
		for (int i = 0; i < 17; i++) { // part -> chance
			drawHorizontalLine(winU + 4 + i * 8, winU + 8 + i * 8, winV + y, color);
		}
		y += 17;
		for (int i = 0; i < 17; i++) { // chance -> sound
			drawHorizontalLine(winU + 4 + i * 8, winU + 8 + i * 8, winV + y, color);
		}
		y += 26;
		for (int i = 0; i < 17; i++) { // sound -> emotion
			drawHorizontalLine(winU + 4 + i * 8, winU + 8 + i * 8, winV + y, color);
		}
		y += 26;
		for (int i = 0; i < 17; i++) { // emotion -> equipment
			drawHorizontalLine(winU + 4 + i * 8, winU + 8 + i * 8, winV + y, color);
		}
		y += 25;
		for (int i = 0; i < 17; i++) { // equipment -> end
			drawHorizontalLine(winU + 4 + i * 8, winU + 8 + i * 8, winV + y, color);
		}
		GlStateManager.popMatrix();

		if (!hasSubGui()) {
			GlStateManager.pushMatrix();
			GL11.glEnable(GL11.GL_SCISSOR_TEST);
			int c = w < mc.displayWidth ? (int) Math.round((double) mc.displayWidth / w) : 1;
			GL11.glScissor((workU + 1) * c, mc.displayHeight - (workV + workS - 1) * c, (workS - 2) * c, (workS - 2) * c);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			drawWork();
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
			GlStateManager.popMatrix();

			GlStateManager.pushMatrix();
				GlStateManager.translate(0.0f, 0.0f, 950.0f);
				// axis xyz vector
				GlStateManager.pushMatrix();
					GlStateManager.translate(workU + 12.5f, workV + 12.0f, dispScale > 1.0f ? -240.0f : 0.0f);
					if (dispRot[0] != 0.0f) {
						GlStateManager.rotate(dispRot[0], 0.0f, 1.0f, 0.0f);
					}
					if (dispRot[1] != 0.0f) {
						GlStateManager.rotate(dispRot[1], 0.0f, 0.0f, 1.0f);
					}
					if (dispRot[2] != 0.0f) {
						GlStateManager.rotate(dispRot[2], 1.0f, 0.0f, 0.0f);
					}
					GlStateManager.pushMatrix();
						GlStateManager.rotate(90.0f, 0.0f, 1.0f, 0.0f);
						GlStateManager.translate(0.0f, 0.0f, 0.5f);
						drawCRect(-10.5, -0.5d, -0.5d, 0.5d, new Color(0xFF0000FF).getRGB());
					GlStateManager.popMatrix();
					GlStateManager.pushMatrix();
						drawCRect(-10.5, -0.5d, -0.5d, 0.5d, new Color(0xFFFF0000).getRGB());
						drawCRect(-0.5d, -10.5, 0.5d, -0.5d, new Color(0xFF00D000).getRGB());
						color = GuiNpcAnimation.backColor == 0xFF000000 ?
								new Color(0xFFFFFFFF).getRGB() :
								new Color(0xFF000000).getRGB();
						drawCRect(-0.5d, -0.5d, 0.5d, 0.5d, color);
					GlStateManager.popMatrix();
				GlStateManager.popMatrix();
				// display info
				GlStateManager.pushMatrix();
					GlStateManager.translate(workU, workV, 0.0f);
					String ts = "x" + dispScale;
					fontRenderer.drawString(ts, workS - 11 - fontRenderer.getStringWidth(ts), 1, color, false);
					ts = (int) dispRot[0] + "" + ((char) 176) + "/" + (int) dispRot[1] + ((char) 176) + "/" + (int) dispRot[2] + ((char) 176);
					fontRenderer.drawString(ts, workS - 11 - fontRenderer.getStringWidth(ts), workS - 10, color, false);
					ts = (int) dispPos[0] + "/" + (int) dispPos[1];
					fontRenderer.drawString(ts, 11, workS - 10, color, false);
				GlStateManager.popMatrix();
			GlStateManager.popMatrix();
		}
		else {
			GlStateManager.pushMatrix();
			GlStateManager.translate(0.0f, 0.0f, 1.0f);
			Gui.drawRect(workU + 1, workV + 1, workU + workS - 1, workV + workS - 1, GuiNpcAnimation.backColor);
			GlStateManager.popMatrix();
		}
		GlStateManager.pushMatrix();
			GlStateManager.translate(0.0f, 0.0f, 975.0f);
			super.drawScreen(mouseX, mouseY, partialTicks);
		GlStateManager.popMatrix();
		getButton(7).setIsEnabled(!isMotion && ((isHitbox && hitbox != null) || (anim != null && part != null && part.id != 6 && part.id != 7)));
		getButton(8).setIsEnabled(!isMotion && ((isHitbox && hitbox != null) || (anim != null && addedPartConfig != null && part != null && part.id > 7)));
		if (hasSubGui() || !CustomNpcs.ShowDescriptions) { return; }
		drawHoverText(null);
	}

	@Override
	public void initGui() {
		super.initGui();
		if (frame.id < 0) {
			frame = anim.frames.get(0);
			if (frame.id < 0) { frame.id = 0; }
		}
		if (hitbox.id < 0) {
			hitbox = frame.damageHitboxes.get(0);
			if (hitbox.id < 0) { hitbox.id = 0; }
		}
		if (scrollParts == null) {
			scrollParts = addScroll(0).setSize(67, 112).setHoverTexts("animation.hover.part.sel");
		}
		ScaledResolution sw = new ScaledResolution(mc);
		if (w > 0.0d && h > 0.0d) {
			if (partNames != null) {
				double left = 1.0d / w * partNames.getX();
				double top = 1.0d / h * partNames.getY();
				partNames.transferTo((int) ((double) sw.getScaledWidth() * left),
						(int) ((double) sw.getScaledHeight() * top));
				scrollParts.setPos(partNames.getX() + 4, partNames.getY() + 12);
			}
			if (tools != null) {
				double left = 1.0d / w * tools.getX();
				double top = 1.0d / h * tools.getY();
				tools.transferTo((int) ((double) sw.getScaledWidth() * left),
						(int) ((double) sw.getScaledHeight() * top));
			}
		}
		w = sw.getScaledWidth();
		h = sw.getScaledHeight();
		if ((sw.getScaledWidth() - 144) < sw.getScaledHeight() - 8) {
			workS = sw.getScaledWidth() - 144;
			winU = 0;
			winW = sw.getScaledWidth();
			winV = (sw.getScaledHeight() - workS - 8) / 2;
			winH = workS + 8;
		}
		else {
			workS = sw.getScaledHeight() - 8;
			winU = (sw.getScaledWidth() - workS - 144) / 2;
			winW = workS + 144;
			winV = 0;
			winH = sw.getScaledHeight();
		}
		workU = winU + 140;
		workV = winV + 4;
		winScale = (float) workS / 100.0f;
		int x = winU + 3;
		int y = winV + 12;
		int lId = 0;
		// common to settings
		addLabel(lId++, workU - 6, workV, Component.literal("?").withStyle(TextFormatting.UNDERLINE, TextFormatting.BOLD))
				.setColor(0xFF000000)
				.setHoverTexts("animation.hover.help");
		// name
		addLabel(lId++, x, y - 10, Component.translatable("gui.name").append(":"));
		addTextField(11, x, y, 125, 12, anim.name);

		// work place
		// animation frame init
		GuiLabel label = addLabel(lId++, workU + workS / 2, workV + 3, anim.getSettingName())
				.setColor(GuiNpcAnimation.backColor == 0xFF000000 ? 0xFFFFFFFF : 0xFF000000);
		label.setCenter(label.getWidth() / 2);

		// type
		addLabel(lId++, x, (y += 26) - 10, "animation.place");
		addButton(0, x, y, true, blockType, blockNames)
				.setSize(105, 10)
				.setHoverTexts("animation.hover.block.type");
		// size
		addButton(1, x + 107, y, false, blockSize, blockSizes)
				.setSize(17, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("animation.hover.block.size");
		// back color
		addButton(2, workU + 2, workV + 23, "")
				.setSize(8, 8)
				.setColor(GuiNpcAnimation.backColor == 0xFF000000 ? new Color(0xFF00FFFF).getRGB() : new Color(0xFF008080).getRGB())
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("animation.hover.color");
		addButton(26, workU + 2, workV + 31, "")
				.setSize(8, 8)
				.setColor(ModelNpcAlt.editAnimDataSelect.showArmor ? new Color(0xFFFF7200).getRGB() : new Color(0xFF6F3200).getRGB())
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("animation.hover.show.armor");
		addButton(46, workU + 2, workV + 39, "")
				.setSize(8, 8)
				.setColor(ModelNpcAlt.editAnimDataSelect.alpha >= 1.0f ? new Color(0xFFFFFEBF).getRGB() : new Color(0xFF787758).getRGB())
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("animation.hover.show.alpha");
		addButton(47, workU + 2, workV + 47, "")
				.setSize(8, 8)
				.setColor(baseRotation == 0.0f || baseRotation == npcAnim.rotationYaw ? new Color(0xFF96FFC0).getRGB() : new Color(0xFF426C53).getRGB())
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("animation.hover.base.rot");
		// frame
		addLabel(lId++, x, (y += 23) - 10, "animation.frames");
		Object[] lFrames = new Object[anim.frames.size()];
		for (int i = 0; i < anim.frames.size(); i++) { lFrames[i] = i + "/" + (anim.frames.size() - 1); }
		addButton(3, x, y, true, frame.id, lFrames)
				.setSize(60, 10)
				.setHoverTexts("animation.hover.frame", "" + (frame.id + 1));
		// add frame
		addButton(4, x + 106, y - 10, "")
				.setSize(10, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 24, 24)
				.setHoverTexts("animation.hover.frame.add");
		// del frame
		addButton(5, x + 116, y - 10, "")
				.setSize(10, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(72, 0, 24, 24)
				.setIsEnabled(anim.frames.size() > 1)
				.setHoverTexts("animation.hover.frame.del");
		// frame smooth animated
		addCheckBox(11, x + 62, y - 2, "gui.smooth", "gui.linearly", frame.isSmooth())
				.setSize(74, 12)
				.setHoverTexts("animation.hover.smooth." + frame.isSmooth());
		// clear frame
		addButton(6, x + 126, y - 10, "")
				.setSize(10, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(120, 0, 24, 24)
				.setHoverTexts(Component.translatable("animation.hover.reset.frame")
						.append(Component.translatable("animation.hover.shift.0")));
		// frame times
		addLabel(lId++, x, (y += 12) + 2, Component.translatable("gui.time").append(":"));
		addTextField(1, x + 35, y, 48, 12, "" + frame.getSpeed())
				.setMinMaxDefault(0, 3600, frame.getSpeed())
				.setHoverTexts("animation.hover.part.ticks");
		addTextField(2, x + 87, y, 48, 12, "" + frame.getEndDelay())
				.setMinMaxDefault(0, 3600, frame.getEndDelay())
				.setHoverTexts("animation.hover.part.delay");
		// frame repeat
		hasExtend = false;
		if (anim.type == AnimationKind.DIES || anim.type == AnimationKind.JUMP) {
			hasExtend = true;
			addLabel(lId++, x, (y += 14) + 2, Component.translatable("gui.repeat").append(":"));
			if (anim.repeatLast < 0) { anim.repeatLast *= -1; }
			if (anim.repeatLast > anim.frames.size()) { anim.repeatLast = anim.frames.size(); }
			addTextField(0, x + 87, y, 48, 12, "" + anim.repeatLast)
					.setMinMaxDefault(0, anim.frames.size(), anim.repeatLast)
					.setHoverTexts("animation.hover.anim.repeat");
		}
		// has damage hitbox
		if (anim.type == AnimationKind.ATTACKING) {
			hasExtend = true;
			addCheckBox(36, x, y += 14, "animation.now.attack", "animation.notyet.attack", frame.isNowDamage())
					.setSize(136, 12)
					.setHoverTexts("animation.hover.now.attack");
		}
		// show stack in right hand
		addLabel(lId++, x + 20, y += 13, "animation.hold.right");
		int type = frame.getHoldRightStackType();
		if (type == 2) { type = 3; }
		else if (type == 3) { type = 4; }
		addButton(37, x + 20, y += 9, true, frame.getHoldRightStackType(),
				"animation.stack.type.0", "animation.stack.type.1", "animation.stack.type.3", "animation.stack.type.4",
				"animation.stack.type.5", "animation.stack.type.6", "animation.stack.type.7", "animation.stack.type.8")
				.setSize(116, 10)
				.setHoverTexts("animation.hover.stack.type."+type);
		// show stack in left hand
		addLabel(lId++, x + 20, y += 10, "animation.hold.left");
		type = frame.getHoldLeftStackType();
		if (type == 3) { type = 4; }
		addButton(38, x + 20, y += 9, true, frame.getHoldLeftStackType(),
				"animation.stack.type.0", "animation.stack.type.1", "animation.stack.type.3", "animation.stack.type.4",
				"animation.stack.type.5", "animation.stack.type.6", "animation.stack.type.7", "animation.stack.type.8")
				.setSize(116, 10)
				.setHoverTexts("animation.hover.stack.type."+type);
		// Part
		addLabel(lId++, x, y += 13, isMotion ? "animation.motion" :  isHitbox ? "animation.hitbox" : "animation.parts");
		// show part names
		addButton(29, workU + 2, y, "")
				.setSize(8, 8)
				.setColor(CustomNpcs.colorAnimHoverPart + 0xFF000000)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(232, 0, 24, 24)
				.setIsEnabled(partNames == null || !partNames.visible)
				.setHoverTexts("animation.hover.show.parts");
		// show hitbox names
		if (anim.type == AnimationKind.ATTACKING) {
			addButton(45, workU + 2, y + 12, "")
					.setSize(8, 8)
					.setColor(0xFFFAF700)
					.setTexture(ANIMATION_BUTTONS)
					.setDefBack(false)
					.setIsAnim(true)
					.setUV(232, 0, 24, 24)
					.setIsEnabled(hitboxes == null || !hitboxes.visible)
					.setHoverTexts("animation.hover.show.hitboxes");
		}
		// scrolls data set
		dataParts.clear();
		List<Component> lParts = new ArrayList<>();
		for (int id : frame.parts.keySet()) {
			PartConfig ps = frame.parts.get(id);
			Component key = Component.translatable(ps.name);
			dataParts.put(key, ps);
			lParts.add(key);
		}
		scrollParts.setUnsortedList(lParts);
		dataHitboxes.clear();
		if (scrollHitboxes == null) { scrollHitboxes = addScroll(1).setSize(112, 112); }
		List<Component> lHitboxes = new ArrayList<>();
		int i = 0;
		LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
		for (int id : frame.damageHitboxes.keySet()) {
			AnimationDamageHitbox aDH = frame.damageHitboxes.get(id);
			Component key = aDH.getKey();
			dataHitboxes.put(key, aDH);
			lHitboxes.add(key);
			hts.put(i, aDH.getHoverKey());
			i++;
		}
		scrollHitboxes.setUnsortedList(lHitboxes)
				.setHoverTexts(hts);
		if (isHitbox && toolType == 0) { toolType = 1; }
		// add part / hitbox
		addButton(7, x + 106, y, "")
				.setSize(10, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(96, 0, 24, 24)
				.setHoverTexts(isHitbox ? "animation.hover.hitbox.add" : isMotion ? "animation.hover.not.in.motion" : "animation.hover.part.add");
		// del part / hitbox
		addButton(8, x + 116, y, "")
				.setSize(10, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(72, 0, 24, 24)
				.setHoverTexts(isHitbox ? "animation.hover.hitbox.add" : isMotion ? "animation.hover.not.in.motion" : "animation.hover.part.del");
		// clear part
		addButton(9, x + 126, y, "")
				.setSize(10, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(120, 0, 24, 24)
				.setIsEnabled(!isMotion && !isHitbox)
				.setHoverTexts(Component.translatable("animation.hover.reset.part")
						.append(Component.translatable("animation.hover.shift.0")));
		// used part in frame
		addCheckBox(10, x, y += 10, "gui.disabled", "gui.enabled", part.isDisable())
				.setSize(67, 14)
				.setIsEnabled(!isMotion && !isHitbox)
				.setHoverTexts(Component.translatable("animation.hover.part.disabled." + !part.isDisable())
						.append(Component.translatable("animation.hover.shift.1")));
		// show part in frame
		addCheckBox(22, x + 69, y, "gui.show", "gui.noshow", part.isShow())
				.setSize(67, 14)
				.setIsEnabled(!isMotion && !isHitbox)
				.setHoverTexts(Component.translatable("animation.hover.part.show." + part.isShow())
						.append(Component.translatable("animation.hover.shift.1")));
		// display color hover
		StringBuilder color = new StringBuilder(Integer.toHexString(CustomNpcs.colorAnimHoverPart));
		while (color.length() < 6) { color.insert(0, "0"); }
		addButton(12, x, y += 15, color.toString())
				.setSize(67, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setIsEnabled(!isMotion && !isHitbox)
				.setColor(CustomNpcs.colorAnimHoverPart)
				.setHoverTexts("animation.hover.part.color")
				.setShowShadow(false);
		// Chance
		float ch = Math.round(anim.chance * 100000.0f) / 1000.0f;
		addLabel(lId++, x, (y += 14) + 1, Component.translatable("drop.chance").append(":"));
		addTextField(10, x + 28, y, 40, 12, String.valueOf(ch))
				.setMinMaxDefault(0.0f, 100.0f, ch)
				.setHoverTexts("animation.hover.chance");
		addLabel(lId++, x + 72, y + 1, "%");
		// Sound Settings
		addLabel(lId++, x, y += 16, Component.translatable("advanced.sounds").append(":"));
		addTextField(3, x, y + 10, 135, 12, frame.getStartSound())
				.setHoverTexts("animation.hover.sound");
		addButton(27, x + 135 - 17, y, "S")
				.setSize(8, 8)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setColor(0xFFDC0000)
				.setHoverTexts("animation.hover.select.sound")
				.setShowShadow(false);
		addButton(28, x + 135 - 8, y, "X")
				.setSize(8, 8)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setColor(0xFFDC0000)
				.setHoverTexts("animation.hover.del.sound")
				.setShowShadow(false);
		// Emotion data
		addLabel(lId++, x, y += 26, Component.translatable("advanced.emotion").append(":"));
		addTextField(4, x, y + 10, 48, 12, "" + frame.getStartEmotion())
				.setMinMaxDefault(0, AnimationController.getInstance().getUnusedEmtnId() - 1, frame.getStartEmotion())
				.setHoverTexts("animation.hover.emotion.id");

		// show equipment mainhand
		addLabel(lId, x, y += 26, "animation.show.stacks");
		addButton(39, x + 2, y += 10, "")
				.setSize(12, 12)
				.setTexture(ANIMATION_BUTTONS_SLOTS)
				.setDefBack(false)
				.setUV(0, frame.showMainHand ? 0 : 96, 24, 24)
				.setColor(0xFFDC0000)
				.setHoverTexts("animation.hover.0")
				.setShowShadow(false);
		// show equipment offhand
		addButton(40, x + 16, y, "")
				.setSize(12, 12)
				.setTexture(ANIMATION_BUTTONS_SLOTS)
				.setDefBack(false)
				.setUV(24, frame.showOffHand ? 0 : 96, 24, 24)
				.setColor(0xFFDC0000)
				.setHoverTexts("animation.hover.1")
				.setShowShadow(false);
		// show equipment helmet
		addButton(41, x + 30, y, "")
				.setSize(12, 12)
				.setTexture(ANIMATION_BUTTONS_SLOTS)
				.setDefBack(false)
				.setUV(48, frame.showHelmet ? 0 : 96, 24, 24)
				.setColor(0xFFDC0000)
				.setHoverTexts("animation.hover.2")
				.setShowShadow(false);
		// show equipment body
		addButton(42, x + 44, y, "")
				.setSize(12, 12)
				.setTexture(ANIMATION_BUTTONS_SLOTS)
				.setDefBack(false)
				.setUV(72, frame.showBody ? 0 : 96, 24, 24)
				.setColor(0xFFDC0000)
				.setHoverTexts("animation.hover.3")
				.setShowShadow(false);
		// show equipment legs
		addButton(43, x + 58, y, "")
				.setSize(12, 12)
				.setTexture(ANIMATION_BUTTONS_SLOTS)
				.setDefBack(false)
				.setUV(96, frame.showLegs ? 0 : 96, 24, 24)
				.setColor(0xFFDC0000)
				.setHoverTexts("animation.hover.4")
				.setShowShadow(false);
		// show equipment feet's
		addButton(44, x + 72, y, "")
				.setSize(12, 12)
				.setTexture(ANIMATION_BUTTONS_SLOTS)
				.setDefBack(false)
				.setUV(120, frame.showFeets ? 0 : 96, 24, 24)
				.setColor(0xFFDC0000)
				.setHoverTexts("animation.hover.5")
				.setShowShadow(false);
		// exit
		addButton(66, x, winV + winH - 12, "gui.back")
				.setSize(50, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("hover.back");
		// work place
		Component hAgain= Component.translatable("animation.hover.again");
		// simple mesh
		addButton(13, workU + 25, workV + 2, "")
				.setSize(8, 8)
				.setColor(meshType == 0 ? 0xFFD93070 : 0xFF360C1C)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts(Component.translatable("animation.hover.mesh.0").append(hAgain));
		// xz mesh
		addButton(14, workU + 34, workV + 2, "")
				.setSize(8, 8)
				.setColor(meshType == 1 ? 0xFF6830D9 : 0xFF1A0C36)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts(Component.translatable("animation.hover.mesh.1").append(hAgain));
		// xy mesh
		addButton(15, workU + 43, workV + 2, "")
				.setSize(8, 8)
				.setColor(meshType == 2 ? 0xFF30D980 : 0xFF0C3620)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts(Component.translatable("animation.hover.mesh.2").append(hAgain));
		// zy mesh
		addButton(16, workU + 52, workV + 2, "")
				.setSize(8, 8)
				.setColor(meshType == 3 ? 0xFFD7D930 : 0xFF35360C)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts(Component.translatable("animation.hover.mesh.3").append(hAgain));
		// show hit box
		addButton(17, workU + 61, workV + 2, "")
				.setSize(8, 8)
				.setColor(showHitBox ? 0 : 0xFF808080)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts(Component.translatable("animation.hover.hitbox").append(hAgain));
		// align xy
		addButton(49, workU + 79, workV + 2, "x")
				.setSize(8, 8)
				.setColor(0xFF8555BA)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("animation.hover.align.xy");
		// align zy
		addButton(50, workU + 79, workV + 11, "z")
				.setSize(8, 8)
				.setColor(0xFF8555BA)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("animation.hover.align.zy");
		// align xz
		addButton(51, workU + 88, workV + 2, "y")
				.setSize(8, 8)
				.setColor(0xFF8555BA)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("animation.hover.align.xz");
		// align revers
		addButton(52, workU + 97, workV + 11, "r")
				.setSize(8, 8)
				.setColor(0xFF557DBA)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("animation.hover.align.revers");
		// reset scale
		addButton(18, workU + workS - 10, workV + 2, "")
				.setSize(8, 8)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("animation.hover.reset.scale");
		// reset pos
		addButton(19, workU + 2, workV + workS - 10, "")
				.setSize(8, 8)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("animation.hover.reset.pos");
		// reset rot
		addButton(20, workU + workS - 10, workV + workS - 10, "")
				.setSize(8, 8)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(0, 96, 0, 0)
				.setHoverTexts("animation.hover.reset.rot");
		// part or anim
		addButton(21, workU + workS / 2 - 11, workV + workS - 12, "")
				.setSize(18, 10)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setUV(onlyCurrentPart ? 144 : 188, 0, 44, 24)
				.setHoverTexts(Component.translatable("animation.hover.work." + onlyCurrentPart,
						((char) 167) + "6" + (frame != null ? frame.id + 1 : -1)));
		y = workV + workS - 74;
		// animation init data
		addLabel(50, workU + workS / 2 + 10, workV + workS - 12, "0")
				.setColor(GuiNpcAnimation.backColor == 0xFF000000 ? 0xFFFFFFFF : 0xFF000000);
		addLabel(51, workU + workS / 2 - 13, workV + workS - 12, "0")
				.setColor(GuiNpcAnimation.backColor == 0xFF000000 ? 0xFFFFFFFF : 0xFF000000);
		// set animation part tick
		addButton(53, workU + workS / 2 - 56, workV + workS - 24, true, 0, "")
				.setSize(108, 10)
				.setHoverTexts("animation.hover.part.all.ticks")
				.setIsVisible(onlyCurrentPart);
		// show tools
		addButton(35, workU + 5, y, "")
				.setSize(8, 8)
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setIsEnabled(tools == null || !tools.visible)
				.setUV(232, 0, 24, 24)
				.setHoverTexts("animation.hover.show.tools");
		// tool pos
		addButton(23, workU + 2, y += 10, "")
				.setSize(14, 14)
				.setColor(toolType == 1 ? new Color(0xFFFF4040).getRGB() : new Color(0xFFFFFFFF).getRGB())
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setIsEnabled(tools == null || !tools.visible)
				.setUV(0, 0, 24, 24)
				.setHoverTexts("animation.hover.tool.0");
		// tool rot
		addButton(24, workU + 2, y += 16, "")
				.setSize(14, 14)
				.setColor(toolType == 0 ? new Color(0xFF40FF40).getRGB() : new Color(0xFFFFFFFF).getRGB())
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setIsEnabled(!isHitbox && !isMotion)
				.setUV(24, 0, 24, 24)
				.setHoverTexts("animation.hover.tool.1");
		// tool scale
		addButton(25, workU + 2, y + 16, "")
				.setSize(14, 14)
				.setColor(toolType == 2 ? new Color(0xFF4040FF).getRGB() : new Color(0xFFFFFFFF).getRGB())
				.setTexture(ANIMATION_BUTTONS)
				.setDefBack(false)
				.setIsAnim(true)
				.setIsEnabled(!isMotion)
				.setUV(48, 0, 24, 24)
				.setHoverTexts("animation.hover.tool.2");
		resetAnimation();
		// Parts window
		boolean vPN = partNames == null || partNames.visible;
		showPartNames();
		partNames.visible = vPN;
		partNames.objs = new Object[] { part, toolType };
		// Tool window
		boolean vT = tools == null || tools.visible;
		showTools();
		tools.visible = vT;
		if (anim.type == AnimationKind.ATTACKING) {
			boolean vH = hitboxes == null || hitboxes.visible;
			showHitBoxes();
			hitboxes.visible = vH;
		}
		if (ModelNpcAlt.editAnimDataSelect.part != (part == null ? -1 : part.id)) { setPart(part); }
	}

	private void showHitBoxes() {
		if (hitboxes == null) {
			hitboxes = new GuiCustomWindowNop(this, 2, workU + 18, workV + 12, 120, 118,
					Component.translatable("gui.hitboxes", ":"))
					.addClose((window) -> getButton(45).setIsEnabled(get(2, GuiCustomWindowNop.class) != null));
			hitboxes.widthTexture = 256;
			hitboxes.heightTexture = 256;
			hitboxes.setColorLine(0xFAF700);
			hitboxes.add(scrollHitboxes);
		}
		hitboxes.setPoint(getButton(45));
		scrollHitboxes.setPos(hitboxes.getX() + 4, hitboxes.getY() + 12);
		add(hitboxes);
	}

	private void showTools() {
		int f = 11, h = 0;
		int x = workU + 18;
		int y = workV + workS - 65;
		boolean notNormal = toolType == 0 && part != null && ((addedPartConfig != null && !addedPartConfig.isNormal) || (part.id >= 1 && part.id <= 5));
		if (isHitbox) { notNormal = false; }
		y += notNormal ? -11 : 0;
		if (tools != null) {
			x = tools.getX();
			y = tools.getY();
			h = tools.imageHeight;
		}
		tools = new GuiCustomWindowNop(this, 1, x, y, 146, notNormal ? 60 : 38, Component.translatable("gui.tools").append(":"))
				.addClose((window) -> getButton(35).setIsEnabled(get(1, GuiCustomWindowNop.class) != null));
		tools.widthTexture = 256;
		tools.heightTexture = 256;
		x += 4;
		y += 13;
		for (int i = 0; i < 3; i++) {
			tools.addLabel(i, x, y + i * f,
					i == 0 ? (toolType == 1 && isHitbox || isMotion ? "D:" : "X:") :
							i == 1 ? (toolType == 1 && isHitbox || isMotion ? "H:" : "Y:") :
									(toolType == 1 && isHitbox || isMotion ? "W:" : "Z:"));
			float[] values;
			if (isHitbox && hitbox != null) { values = toolType == 1 ? hitbox.offset : hitbox.scale; }
			else if (isMotion && frame != null) {
				toolType = 1;
				values = frame.motions;
			}
			else { values = toolType == 0 ? part.rotation : toolType == 1 ? part.offset : part.scale; }
			float[] textToFields = new float[values.length];
			float[] sliderValues = new float[values.length];
			switch (toolType) {
				case 1: { // o
					if (i == 2 && (isHitbox || isMotion)) {
						textToFields[i] = Math.round(values[i] * 180000.0f / Math.PI) / 1000.0f;
						sliderValues[i] = ValueUtil.correctFloat(values[i] * 0.159155f + 0.5f, 0.0f, 1.0f);
					}
					else {
						textToFields[i] = Math.round(values[i] * 1000.0f) / 1000.0f;
						if (isMotion) {
							if (i == 0) { sliderValues[i] = ValueUtil.correctFloat(values[i] * 2.0f / 3.0f, 0.0f, 1.0f); }
							else { sliderValues[i] = ValueUtil.correctFloat(values[i] / 3.0f + 0.5f, 0.0f, 1.0f); }
						} else if (isHitbox) {
							if (i == 0) { sliderValues[i] = ValueUtil.correctFloat(values[i] * 0.1f, 0.0f, 1.0f); }
							else { sliderValues[i] = ValueUtil.correctFloat(values[i] * 0.05f + 0.5f, 0.0f, 1.0f); }
						}
						else { sliderValues[i] = ValueUtil.correctFloat(values[i] * 0.1f + 0.5f, 0.0f, 1.0f); }
					}
					break;
				}
				case 2: { // s
					textToFields[i] = Math.round(values[i] * 1000.0f) / 1000.0f;
					sliderValues[i] = ValueUtil.correctFloat(values[i] * 0.2f, 0.0f, 1.0f);
					break;
				}
				default: { // r
					textToFields[i] = Math.round(values[i] * 180000.0f / Math.PI) / 1000.0f;
					sliderValues[i] = ValueUtil.correctFloat(values[i] * 0.159155f + 0.5f, 0.0f, 1.0f);
					break;
				}
			}
			tools.addSlider(i, x + 9, y + i * f, sliderValues[i])
					.setSize(75, 8)
					.setHoverTexts("animation.hover." + (toolType == 0 ? "rotation" : toolType == 1 ? "offset" : "scale"),
							i == 0 ? "X" : i == 1 ? "Y" : "Z");
			double m = -180.0d, n = 180.0d;
			if (toolType == 1) {
				if (isMotion) { // D H W
					if (i == 0) {
						m = 0.0d;
						n = 1.5d;
					} if (i == 1) {
						m = -1.5d;
						n = 1.5d;
					}
				}
				else if (isHitbox) { // D H W
					if (i == 0) {
						m = 0.0d;
						n = 10.0d;
					} if (i == 1) {
						m = -10.0d;
						n = 10.0d;
					}
				}
				else {
					m = -5.0d;
					n = 5.0d;
				}
			}
			else if (toolType == 2) {
				m = 0.0d;
				n = 5.0d;
			}
			tools.addTextField(i + 5, x + 86, y + i * f, 42, 8, "" + textToFields[i])
					.setMinMaxDefault(m, n, textToFields[i])
					.setHoverTexts("animation.hover." + (toolType == 0 ? "rotation" : toolType == 1 ? "offset" : "scale"),
							i == 0 ? "X" : i == 1 ? "Y" : "Z");
			tools.addButton(30 + i, x + 130, y + i * f, "X")
					.setSize(8, 8)
					.setTexture(ANIMATION_BUTTONS)
					.setDefBack(false)
					.setIsAnim(true)
					.setUV(0, 96, 0, 0)
					.setColor(0xFFDC0000)
					.setHoverTexts("animation.hover.reset." + toolType, i == 0 ? "X" : i == 1 ? "Y" : "Z")
					.setShowShadow(false);
		}
		if (!isHitbox && notNormal) {
			y += 33;
			tools.addLabel(3, x, y, "X1:");
			float sliderValue = part.rotation[3] * 0.159155f + 0.5f;
			float textToFields = Math.round(part.rotation[3] * 180000.0f / Math.PI) / 1000.0f;
			tools.addSlider(3, x + 9, y, sliderValue)
					.setSize(75, 8)
					.setHoverTexts("animation.hover.rotation", "X 1");
			tools.addTextField(8, x + 86, y, 42, 8, "" + textToFields)
					.setMinMaxDefault(-180.0d, 180.0d, textToFields)
					.setHoverTexts("animation.hover.rotation", "X 1");
			tools.addButton(33, x + 130, y, "X")
					.setSize(8, 8)
					.setTexture(ANIMATION_BUTTONS)
					.setDefBack(false)
					.setIsAnim(true)
					.setUV(0, 96, 0, 0)
					.setShowShadow(false)
					.setColor(0xFFDC0000)
					.setHoverTexts("animation.hover.reset.0", "X 1");
			y += 11;
			tools.addLabel(4, x, y, "Y1:");
			sliderValue = part.rotation[4] * 0.318310f + 0.5f;
			textToFields = Math.round(part.rotation[4] * 180000.0f / Math.PI) / 1000.0f;
			tools.addSlider(4, x + 9, y, sliderValue)
					.setSize(75, 8)
					.setHoverTexts("animation.hover.rotation", "Y 1");
			tools.addTextField(9, x + 86, y, 42, 8, "" + textToFields)
					.setMinMaxDefault(-90.0d, 90.0d, textToFields)
					.setHoverTexts("animation.hover.rotation", "Y 1");
			tools.addButton(34, x + 130, y, "X")
					.setSize(8, 8)
					.setTexture(ANIMATION_BUTTONS)
					.setDefBack(false)
					.setIsAnim(true)
					.setUV(0, 96, 0, 0)
					.setShowShadow(false)
					.setColor(0xFFDC0000)
					.setHoverTexts("animation.hover.reset.0", "Y 1");
			if (h != 72) { tools.moveTo(0, -11); }
		}
		else if (h != 50) { tools.moveTo(0, 11); }
		switch(toolType) {
			case 1: {
				tools.setPoint(getButton(23));
				tools.setColorLine(0xFF8080);
				break;
			}
			case 2: {
				tools.setPoint(getButton(25));
				tools.setColorLine(0x8080FF);
				break;
			}
			default: {
				tools.setPoint(getButton(24));
				tools.setColorLine(0x80FF80);
				break;
			}
		}
		if (getButton(35) != null) { getButton(35).setColor(tools.getColorLine() + 0xFF000000); }
		add(tools);
	}

	private void showPartNames() {
		if (partNames == null) {
			partNames = new GuiCustomWindowNop(this, 0, workU + workS - 78, workV + 12, 75, 129, Component.translatable("gui.parts").append(":"))
					.addClose((window) -> getButton(29).setIsEnabled(get(0, GuiCustomWindowNop.class) != null));
			partNames.widthTexture = 256;
			partNames.heightTexture = 256;
			partNames.setColorLine(CustomNpcs.colorAnimHoverPart);
			partNames.add(scrollParts.setSelect(part.id));
			partNames.addButton(48, partNames.getX() + 4, partNames.getY() + 125, "ai.movement")
					.setSize(67, 12);
		}
		partNames.setPoint(getButton(29));
		if (scrollParts != null) { scrollParts.setPos(partNames.getX() + 4,partNames.getY() + 12); }
		if (partNames.getButton(48) != null) {
			GuiButtonNop b = partNames.getButton(48);
			b.setX(partNames.getX() + 4);
			b.setY(partNames.getY() + 125);
		}
		add(partNames);
	}

	private boolean isPressAndKey(int type, int id) {
		if (waitKey > 0 && waitKeyID == id) { return false; }
		boolean isPress = type == 0 ? isAltKeyDown() : type == 1 ? isCtrlKeyDown() : isShiftKeyDown() && Keyboard.isKeyDown(id);
		if (isPress) {
			waitKey = 30;
			waitKeyID = id;
		}
		return isPress;
	}

	@Override
	public boolean keyPressed(char typedChar, int keyCode) {
		if (!hasSubGui()) {
			if (keyCode == Keyboard.KEY_ESCAPE) {
				onClose();
				return true;
			}
			// tool pos - Alt + Q
			if (isPressAndKey(0, Keyboard.KEY_Q) && toolType != 1) {
				toolType = 1;
				playButtonClick();
				initGui();
				return true;
			}
			// tool rot - Alt + W
			if (isPressAndKey(0, Keyboard.KEY_W) && toolType != 0) {
				toolType = 0;
				playButtonClick();
				initGui();
				return true;
			}
			// tool rot - Alt + E
			if (isPressAndKey(0, Keyboard.KEY_E) && toolType != 2) {
				toolType = 2;
				playButtonClick();
				initGui();
				return true;
			}
			// play stop - Alt + P
			if (isPressAndKey(0, Keyboard.KEY_P)) {
				onlyCurrentPart = !onlyCurrentPart;
				if (getButton(21) != null) {
					GuiButtonNop b = getButton(21);
					b.setUV(onlyCurrentPart ? 144 : 188, b.txrY, b.txrW, b.txrH);
				}
				playButtonClick();
				isChanged = true;
				initGui();
				return true;
			}
			// reset scale - Alt + S
			if (isPressAndKey(0, Keyboard.KEY_S)) {
				dispScale = 1.0f;
				playButtonClick();
				return true;
			}
			// reset rot - Alt + R
			if (isPressAndKey(0, Keyboard.KEY_R)) {
				dispRot[0] = 45.0f;
				dispRot[1] = 345.0f;
				dispRot[2] = 345.0f;
				playButtonClick();
				return true;
			}
			// reset pos - Alt + O
			if (isPressAndKey(0, Keyboard.KEY_O)) {
				for (int j = 0; j < 3; j++) { dispPos[j] = 0.0f; }
				playButtonClick();
				return true;
			}
			// align xy Shift + X
			if (isPressAndKey(2, Keyboard.KEY_X)) {
				dispRot[0] = 0.0f;
				dispRot[1] = 0.0f;
				dispRot[2] = 0.0f;
				playButtonClick();
				return true;
			}
			// align xz Shift + Y
			if (isPressAndKey(2, Keyboard.KEY_Y)) {
				dispRot[0] = 0.0f;
				dispRot[1] = 270.0f;
				dispRot[2] = 0.0f;
				playButtonClick();
				return true;
			}
			// align zy Shift + Z
			if (isPressAndKey(2, Keyboard.KEY_Z)) {
				dispRot[0] = 90.0f;
				dispRot[1] = 0.0f;
				dispRot[2] = 0.0f;
				playButtonClick();
				return true;
			}
			// align revers Shift + R
			if (isPressAndKey(2, Keyboard.KEY_R)) {
				displayRotate(180, 0);
				playButtonClick();
				return true;
			}
		}
		return super.keyPressed(typedChar, keyCode);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		boolean bo = super.mouseClicked(mouseX, mouseY, mouseButton);
		if (!hasSubGui()) {
			if (hoverRight && frame.getHoldRightStackType() == 3) {
				IItemStack stack;
				switch(frame.getHoldRightStackType()) {
					case 1: stack = npc.inventory.getProjectile(); break;
					case 2: stack = npc.inventory.getLeftHand(); break;
					case 3: stack = frame.getHoldRightStack(); break;
					default: stack = npc.inventory.getRightHand(); break;
				}
				setSubGui(new SubGuiSelectItemStack(0, stack==null || stack.isEmpty() ? ItemStack.EMPTY : stack.getMCItemStack()));
			}
			else if (hoverLeft && frame.getHoldLeftStackType() == 3) {
				IItemStack stack;
				switch(frame.getHoldLeftStackType()) {
					case 1: stack = npc.inventory.getProjectile(); break;
					case 2: stack = npc.inventory.getRightHand(); break;
					case 3: stack = frame.getHoldLeftStack(); break;
					default: stack = npc.inventory.getLeftHand(); break;
				}
				setSubGui(new SubGuiSelectItemStack(1, stack==null || stack.isEmpty() ? ItemStack.EMPTY : stack.getMCItemStack()));
			}
			else if (!bo && (mouseButton == 0 || mouseButton == 1) && hovered) {
				mousePressId = mouseButton;
				mousePressX = mouseX;
				mousePressY = mouseY;
			}
		}
		return bo;
	}

	@Override
	public void mouseDragged(GuiSliderNop slider) {
		isChanged = true;
		float value = 0.0f;
		float pi = (float) Math.PI;
		if (isHitbox) {
			if (hitbox == null || toolType == 0) { return; }
			if (toolType == 1) {
				if (slider.id == 0) {
					hitbox.offset[0] = 10.0f * slider.sliderValue;
					value = Math.round(hitbox.offset[0] * 1000.0f) / 1000.0f;
				}
				else if (slider.id == 1) {
					hitbox.offset[1] = 20.0f * slider.sliderValue - 10.0f;
					value = Math.round(hitbox.offset[1] * 1000.0f) / 1000.0f;
				}
				else if (slider.id == 2) {
					hitbox.offset[2] = 2.0f * pi * slider.sliderValue - pi;
					value = Math.round(360.0f * slider.sliderValue - 180.0f);
				}
			}
			else {
				value = Math.round(5000.0f * slider.sliderValue) / 1000.0f;
				hitbox.scale[slider.id] = value;
			}
			if (tools.getTextField(5 + slider.id) != null) { tools.getTextField(5 + slider.id).setValue("" + value); }
			return;
		}
		if (isMotion) {
			if (toolType != 1) { return; }
			if (slider.id == 0) {
				value = Math.round(slider.sliderValue * 1500.0f) / 1000.0f;
				frame.motions[0] = value;
			} else if (slider.id == 1) {
				value = Math.round((slider.sliderValue * 3.0f - 1.5f) * 1000.0f) / 1000.0f;
				frame.motions[1] = value;
			} else if (slider.id == 2) {
				value = Math.round(360.0f * slider.sliderValue - 180.0f);
				frame.motions[2] = 2.0f * pi * slider.sliderValue - pi;
			}
			if (tools.getTextField(5 + slider.id) != null) { tools.getTextField(5 + slider.id).setValue("" + value); }
			return;
		}
		if (part == null) { return; }
		if (slider.id == 3 || slider.id == 4) {
			if (toolType != 0) { return; }
			part.rotation[slider.id] = (2.0f * pi * slider.sliderValue - pi) / (slider.id == 4 ? 2.0f : 1.0f);
			value = Math.round(360.0f * slider.sliderValue - 180.0f) / (slider.id == 4 ? 2.0f : 1.0f);
		} else {
			switch (toolType) {
				case 0: { // r
					part.rotation[slider.id] = 2.0f * pi * slider.sliderValue - pi;
					value = Math.round(360.0f * slider.sliderValue - 180.0f);
					break;
				}
				case 1: { // o
					part.offset[slider.id] = slider.sliderValue * 10.0f - 5.0f;
					value = Math.round(part.offset[slider.id] * 1000.0f) / 1000.0f;
					break;
				}
				case 2: { // s
					part.scale[slider.id] = slider.sliderValue * 5.0f;
					value = Math.round(part.scale[slider.id] * 1000.0f) / 1000.0f;
					break;
				}
			}
		}
		if (tools.getTextField(5 + slider.id) != null) { tools.getTextField(5 + slider.id).setValue("" + value); }
		resetAnimation();
	}

	@Override
	public void mousePressed(GuiSliderNop slider) { }

	@Override
	public void mouseReleased(GuiSliderNop slider) { }

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		GuiTextFieldNop.unfocus();
		 if (isMotion) {
			 isMotion = false;
			 partNames.getButton(48).setIsEnabled(true);
		 }
		 if (scroll.id == 0) {
			 if (anim.type == AnimationKind.ATTACKING) { setHitbox(null); }
			 if (part.id == dataParts.get(scroll.getNormalSelected()).id) { return; }
			 setPart(dataParts.get(scroll.getNormalSelected()));
			 initGui();
		 }
		 else if (scroll.id == 1 && anim.type == AnimationKind.ATTACKING) {
			 setHitbox(dataHitboxes.get(scroll.getNormalSelected()));
			 if (scrollParts != null) { scrollParts.setSelect(-1); }
			 initGui();
		 }
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

	private void setEnvironment() {
		environmentEntitys.clear();
		environmentStates.clear();
		if (npc == null || npc.world == null) { return; }
		offsetY = 0.0f;
		if (npc.posY != Math.floor(npc.posY)) { offsetY = (float) ((npc.posY - Math.round(npc.posY)) * 16.0f); }
		for (int y = -4; y <= 4; y++) {
			for (int x = -4; x <= 4; x++) {
				for (int z = -4; z <= 4; z++) {
					double yP = npc.posY + y - 1;
					if (npc.posY != Math.floor(npc.posY)) { yP = Math.ceil(npc.posY) + y - 1; }
					BlockPos posWorld = new BlockPos(npc.posX + x, yP, npc.posZ + z);
					IBlockState state = npc.world.getBlockState(posWorld);
					BlockPos pos = new BlockPos(x, y, z);
					environmentStates.put(pos, state);
				}
			}
		}
		List<Entity> entities = new ArrayList<>();
		try { entities = npc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
						.offset(npc.getPosition())
						.grow(4.55d, 4.55d, 4.55d)); }
		catch (Exception ignored) { }
		for (Entity e : entities) {
			if (e.equals(npc)) { continue; }
			NBTTagCompound nbt = new NBTTagCompound();
			Entity le;
			if (e instanceof EntityNPCInterface) { le = Util.instance.copyToGUI((EntityNPCInterface) e, player.world, true); }
			else {
				e.writeToNBTAtomically(nbt);
				le = EntityList.createEntityFromNBT(nbt, npc.world);
			}
			if (le != null) {
				le.posX -= npc.posX;
				le.posY -= npc.posY;
				le.posZ -= npc.posZ;
				le.rotationYaw = e.rotationYaw;
				le.prevRotationYaw = e.rotationYaw;
				le.rotationPitch = e.rotationPitch;
				le.prevRotationPitch = e.rotationPitch;
				environmentEntitys.add(le);
			}
		}
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiColorSelector) {
			CustomNpcs.colorAnimHoverPart = ((SubGuiColorSelector) subgui).color;
			ModelNpcAlt.editAnimDataSelect.red = (float) (CustomNpcs.colorAnimHoverPart >> 16 & 255) / 255.0F;
			ModelNpcAlt.editAnimDataSelect.green = (float) (CustomNpcs.colorAnimHoverPart >> 8 & 255) / 255.0F;
			ModelNpcAlt.editAnimDataSelect.blue = (float) (CustomNpcs.colorAnimHoverPart & 255) / 255.0F;
			partNames.setColorLine(CustomNpcs.colorAnimHoverPart);
			initGui();
		}
		if (subgui instanceof SubGuiEditAddPart) {
			SubGuiEditAddPart gui = (SubGuiEditAddPart) subgui;
			if (gui.isNew) {
				// remove old
				for (int parentID : gui.animation.addParts.keySet()) {
					for (AddedPartConfig apc : gui.animation.addParts.get(parentID)) {
						if (apc.id == -1) {
							gui.animation.addParts.get(parentID).remove(apc);
							if (gui.animation.addParts.get(parentID).isEmpty()) { gui.animation.addParts.remove(parentID); }
							break;
						}
					}
				}
				for (AnimationFrameConfig frame : gui.animation.frames.values()) { frame.parts.remove(-1); }
				// create
				if (gui.isSave) {
					int id = 8;
					for (int parentID : anim.addParts.keySet()) {
						for (AddedPartConfig apc : anim.addParts.get(parentID)) {
							if (id < apc.id) { id = apc.id + 1; }
						}
					}
					gui.addPart.id = id;
					gui.part.id = id;
					if (!anim.addParts.containsKey(gui.addPart.parentPart)) { anim.addParts.put(gui.addPart.parentPart, new ArrayList<>()); }
					anim.addParts.get(gui.addPart.parentPart).add(gui.addPart);
					for (AnimationFrameConfig frame : anim.frames.values()) { frame.parts.put(id, gui.part.copy()); }
					ModelNpcAlt.loadAnimationModel(anim);
					initGui();
				}
			}
		}
		if (subgui instanceof SubGuiSoundSelection) {
			if (frame != null) {
				frame.setStartSound(((SubGuiSoundSelection) subgui).resource);
				initGui();
			}
		}
		if (subgui instanceof SubGuiEditText) {
			SubGuiEditText guiText = (SubGuiEditText) subgui;
			if (guiText.id == 0) {
				try {
					int pos = Integer.parseInt(guiText.text[0]) - 1;
					if (pos < 0) { pos = 0; } else if (pos > anim.frames.size()) { pos = anim.frames.size(); }
					frame = (AnimationFrameConfig) anim.addFrame(pos, frame);
					setPart(frame.parts.get(part.id));
					initGui();
				} catch (Exception e) { LogWriter.error(e); }
			}
		}
		if (subgui instanceof SubGuiSelectItemStack) {
			SubGuiSelectItemStack guiStack = (SubGuiSelectItemStack) subgui;
			if (guiStack.id == 0) { frame.setHoldRightStack(guiStack.stack); }
			else { frame.setHoldLeftStack(guiStack.stack); }
			isChanged = true;
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (hasSubGui() || anim == null) { return; }
		switch (textField.id) {
			case 0: {
				if (anim.repeatLast != textField.getInteger()) {
					anim.setRepeatLast(textField.getInteger());
					isChanged = true;
					resetAnimation();
				}
				break;
			} // repeatLast
			case 1: {
				if (frame != null && frame.speed != textField.getInteger()) {
					frame.setSpeed(textField.getInteger());
					isChanged = true;
					resetAnimation();
				}
				break;
			} // speed
			case 2: {
				if (frame != null && frame.getEndDelay() != textField.getInteger()) {
					frame.setEndDelay(textField.getInteger());
					isChanged = true;
					resetAnimation();
				}
				break;
			} // delay
			case 5: {
				isChanged = true;
				float sliderValue = 0.5f;
				if (isHitbox) {
					if (hitbox == null || toolType == 0) { return; }
					if (toolType == 1) {
						hitbox.offset[0] = (float) textField.getDouble();
						sliderValue = (float) textField.getDouble() * 0.1f; // 0 -> 10
					} else {
						hitbox.scale[0] = (float) textField.getDouble();
						sliderValue = (float) textField.getDouble() * 0.2f; // 0 -> 5
					}
					textField.setValue("" + (float) Math.round(textField.getDouble() * 1000.0d) / 1000.0f);
					if (tools.getSlider(0) != null) { tools.getSlider(0).setSliderValue(sliderValue); }
					initGui();
					return;
				}
				if (isMotion) {
					if (frame == null || toolType != 1) { return; }
					frame.motions[0] = (float) textField.getDouble();
					sliderValue = (float) textField.getDouble() * 2.0f / 3.0f; // 0 -> 1.5
					textField.setValue("" + (float) Math.round(textField.getDouble() * 1000.0d) / 1000.0f);
					if (tools.getSlider(0) != null) { tools.getSlider(0).setSliderValue(sliderValue); }
					initGui();
					return;
				}
				PartConfig p = (PartConfig) partNames.objs[0];
				int tType = (int) partNames.objs[1];
				if (part == null || !part.equals(p) || tType != toolType) { return; }
				switch (toolType) {
					case 0: { // r
						p.rotation[0] = (float) Math.toRadians(textField.getDouble());
						sliderValue = (float) textField.getDouble() * 0.002778f + 0.5f;
						break;
					}
					case 1: { // o
						p.offset[0] = (float) textField.getDouble();
						sliderValue = (float) textField.getDouble() * 0.1f + 0.5f;
						break;
					}
					case 2: { // s
						p.scale[0] = (float) textField.getDouble();
						sliderValue = (float) textField.getDouble() * 0.2f;
						break;
					}
				}
				textField.setValue("" + (float) (Math.round(textField.getDouble() * 1000.0d) / 1000.0d));
				if (tools.getSlider(0) != null) { tools.getSlider(0).setSliderValue(sliderValue); }
				resetAnimation();
				break;
			} // X
			case 6: {
				isChanged = true;
				float sliderValue = 0.0f;
				if (isHitbox) {
					if (hitbox == null || toolType == 0) { return; }
					if (toolType == 1) {
						hitbox.offset[1] = (float) textField.getDouble();
						sliderValue = (float) textField.getDouble() * 0.05f + 0.5f; // -10.0 -> 10.0
					} else {
						hitbox.scale[1] = (float) textField.getDouble();
						sliderValue = (float) textField.getDouble() * 0.2f; // 0.0 -> 5.0
					}
					textField.setValue("" + (float) Math.round(textField.getDouble() * 1000.0d) / 1000.0f);
					if (tools.getSlider(1) != null) { tools.getSlider(1).setSliderValue(sliderValue); }
					return;
				}
				if (isMotion) {
					if (frame == null || toolType != 1) { return; }
					frame.motions[1] = (float) textField.getDouble();
					sliderValue = (float) textField.getDouble() / 3.0f + 0.5f; // -1.5 -> 1.5
					textField.setValue("" + (float) Math.round(frame.motions[1] * 1000.0d) / 1000.0f);
					if (tools.getSlider(1) != null) { tools.getSlider(1).setSliderValue(sliderValue); }
					initGui();
					return;
				}
				PartConfig p = (PartConfig) partNames.objs[0];
				int tType = (int) partNames.objs[1];
				if (part == null || !part.equals(p) || tType != toolType) { return; }
				switch (toolType) {
					case 0: {
						p.rotation[1] = (float) Math.toRadians(textField.getDouble());
						sliderValue = (float) textField.getDouble() * 0.002778f + 0.5f;
						break;
					}
					case 1: {
						p.offset[1] = (float) textField.getDouble();
						sliderValue = (float) textField.getDouble() * 0.1f + 0.5f;
						break;
					}
					case 2: {
						p.scale[1] = (float) textField.getDouble();
						sliderValue = (float) textField.getDouble() * 0.2f;
						break;
					}
				}
				textField.setValue("" + (float) (Math.round(textField.getDouble() * 1000.0d) / 1000.0d));
				if (tools.getSlider(1) != null) { tools.getSlider(1).setSliderValue(sliderValue); }
				resetAnimation();
				break;
			} // Y
			case 7: {
				isChanged = true;
				if (isHitbox) {
					if (hitbox == null || toolType == 0) { return; }
					float sliderValue;
					if (toolType == 1) {
						hitbox.offset[2] = (float) Math.toRadians(textField.getDouble());
						sliderValue = (float) textField.getDouble() * 0.002778f + 0.5f;
					}
					else {
						hitbox.scale[2] = (float) textField.getDouble();
						sliderValue = (float) textField.getDouble() * 0.2f;
					}
					textField.setValue("" + (float) Math.round(textField.getDouble() * 1000.0d) / 1000.0f);
					if (tools.getSlider(2) != null) { tools.getSlider(2).setSliderValue(sliderValue); }
					return;
				}
				if (isMotion) {
					if (frame == null || toolType != 1) { return; }
					frame.motions[2] = (float) Math.toRadians(textField.getDouble());
					float sliderValue = (float) textField.getDouble() * 0.002778f + 0.5f;
					textField.setValue("" + (float) Math.round(textField.getDouble() * 1000.0d) / 1000.0f);
					if (tools.getSlider(2) != null) { tools.getSlider(2).setSliderValue(sliderValue); }
					initGui();
					return;
				}
				PartConfig p = (PartConfig) partNames.objs[0];
				int tType = (int) partNames.objs[1];
				if (part == null || !part.equals(p) || tType != toolType) { return; }
				float value = 0.0f;
				switch (toolType) {
					case 0: {
						p.rotation[2] = (float) Math.toRadians(textField.getDouble());
						value = (float) textField.getDouble() * 0.002778f + 0.5f;
						break;
					}
					case 1: {
						p.offset[2] = (float) textField.getDouble();
						value = (float) textField.getDouble() * 0.1f + 0.5f;
						break;
					}
					case 2: {
						p.scale[2] = (float) textField.getDouble();
						value = (float) textField.getDouble() * 0.2f;
						break;
					}
				}
				textField.setValue("" + (float) (Math.round(textField.getDouble() * 1000.0d) / 1000.0d));
				if (tools.getSlider(2) != null) { tools.getSlider(2).setSliderValue(value); }
				resetAnimation();
				break;
			} // Z
			case 8: {
				if (isHitbox || isMotion ) { initGui(); return; }
				isChanged = true;
				PartConfig p = (PartConfig) partNames.objs[0];
				int tType = (int) partNames.objs[1];
				if (part == null || !part.equals(p) || tType != toolType) { return; }
				p.rotation[3] = (float) Math.toRadians(textField.getDouble());
				float value = (float) textField.getDouble() * 0.002778f + 0.5f;
				textField.setValue("" + (float) (Math.round(textField.getDouble() * 1000.0d) / 1000.0d));
				if (tools.getSlider(3) != null) { tools.getSlider(3).setSliderValue(value); }
				resetAnimation();
				break;
			} // X1
			case 9: {
				if (isHitbox || isMotion ) { initGui(); return; }
				isChanged = true;
				PartConfig p = (PartConfig) partNames.objs[0];
				int tType = (int) partNames.objs[1];
				if (part == null || !part.equals(p) || tType != toolType) { return; }
				p.rotation[4] = (float) Math.toRadians(textField.getDouble());
				float value = (float) textField.getDouble() * 0.005556f + 0.5f;
				textField.setValue("" + (float) (Math.round(textField.getDouble() * 1000.0d) / 1000.0d));
				if (tools.getSlider(4) != null) { tools.getSlider(4).setSliderValue(value); }
				resetAnimation();
				break;
			} // Y1
			case 10: {
				anim.chance = (float) Math.round(textField.getDouble() * 1000.0d) / 100000.0f;
				textField.setValue("" + (anim.chance * 100.0f));
				isChanged = true;
				resetAnimation();
				break;
			} // chance
			case 11: {
				anim.name = textField.getValue();
				break;
			} // name
		}
	}

	@Override
	public void save() {
		if (anim != null) { Packets.sendServer(new SPacketAnimationChange(2, anim.save())); }
		Packets.sendServer(new SPacketAnimationChange(3, new NBTTagCompound()));
	}

	private void setPart(PartConfig partConfig) {
		GuiTextFieldNop.unfocus();
		if (partConfig != null) { part = frame.parts.get(partConfig.id); }
		ModelNpcAlt.editAnimDataSelect.part = part == null || isHitbox || isMotion ? -1 : part.id;
		if (part != null && part.id > 7) { addedPartConfig = anim.getAddedPart(part.id); }
		else { addedPartConfig = null; }
		if (tools != null && tools.visible) { showTools(); }
	}

	private void setHitbox(AnimationDamageHitbox hitboxConfig) {
		GuiTextFieldNop.unfocus();
		if (hitboxConfig != null) {
			hitbox = frame.damageHitboxes.get(hitboxConfig.id);
			if (scrollHitboxes != null && scrollHitboxes.hasSelected()) {
				isHitbox = true;
				ModelNpcAlt.editAnimDataSelect.part = -1;
				onlyCurrentPart = true;
				if (getButton(21) != null) {
					GuiButtonNop button = getButton(21);
					button.setUV(144, button.txrY, button.txrW, button.txrH);
				}

			}
		} else {
			isHitbox = false;
			if (scrollHitboxes != null) { scrollHitboxes.clearSelection(); }
		}
		if (tools != null && tools.visible) { showTools(); }
	}

	private void displayOffset(float x, float y) {
		for (int i = 0; i < 2; i++) {
			dispPos[i] += (i == 0 ? x : y);
			if (dispPos[i] > workS * dispScale) {
				dispPos[i] = workS * dispScale;
			} else if (dispPos[i] < -workS * dispScale) {
				dispPos[i] = -workS * dispScale;
			}
		}
	}

	private void displayRotate(float x, float y) {
		dispRot[0] += x;
		dispRot[1] += (float) (Math.cos(dispRot[0] * Math.PI / 180.0f) * y);
		dispRot[2] += (float) (Math.sin(dispRot[0] * Math.PI / 180.0f) * y);
		for (int i = 0; i < 3; i++) {
			if (dispRot[i] > 360.0f) {
				dispRot[i] -= 360.0f;
			} else if (dispRot[i] < 0.0f) {
				dispRot[i] += 360.0f;
			}
		}
	}

	private void drawCRect(double left, double top, double right, double bottom, int color) {
		float f3 = (float) (color >> 24 & 255) / 255.0F;
		float f = (float) (color >> 16 & 255) / 255.0F;
		float f1 = (float) (color >> 8 & 255) / 255.0F;
		float f2 = (float) (color & 255) / 255.0F;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.color(f, f1, f2, f3);
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
		// front
		bufferbuilder.pos(left, bottom, 0.0D).endVertex();
		bufferbuilder.pos(right, bottom, 0.0D).endVertex();
		bufferbuilder.pos(right, top, 0.0D).endVertex();
		bufferbuilder.pos(left, top, 0.0D).endVertex();
		// left
		bufferbuilder.pos(left, bottom, -1.0D).endVertex();
		bufferbuilder.pos(left, bottom, 0.0D).endVertex();
		bufferbuilder.pos(left, top, 0.0D).endVertex();
		bufferbuilder.pos(left, top, -1.0D).endVertex();
		// back
		bufferbuilder.pos(right, bottom, -1.0D).endVertex();
		bufferbuilder.pos(left, bottom, -1.0D).endVertex();
		bufferbuilder.pos(left, top, -1.0D).endVertex();
		bufferbuilder.pos(right, top, -1.0D).endVertex();
		// right
		bufferbuilder.pos(right, bottom, 0.0D).endVertex();
		bufferbuilder.pos(right, bottom, -1.0D).endVertex();
		bufferbuilder.pos(right, top, -1.0D).endVertex();
		bufferbuilder.pos(right, top, 0.0D).endVertex();
		// top
		bufferbuilder.pos(left, top, 0.0D).endVertex();
		bufferbuilder.pos(right, top, 0.0D).endVertex();
		bufferbuilder.pos(right, top, -1.0D).endVertex();
		bufferbuilder.pos(left, top, -1.0D).endVertex();
		// bottom
		bufferbuilder.pos(left, bottom, -1.0D).endVertex();
		bufferbuilder.pos(right, bottom, -1.0D).endVertex();
		bufferbuilder.pos(right, bottom, 0.0D).endVertex();
		bufferbuilder.pos(left, bottom, 0.0D).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}

	/**
	 * @param type 0:X; 1:Y; 2:Z
	 */
	private void drawLine(double x, double y, double z, double dist, int type, float red, float green, float blue) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.glLineWidth(2.0f);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		if (type == 0 || type < 0) {
			buffer.pos(x - dist, y, z).color(red, green, blue, 1.0f).endVertex();
			buffer.pos(x + dist, y, z).color(red, green, blue, 1.0f).endVertex();
		}
		if (type == 1 || type < 0) {
			buffer.pos(x, y - dist, z).color(red, green, blue, 1.0f).endVertex();
			buffer.pos(x, y + dist, z).color(red, green, blue, 1.0f).endVertex();
		}
		if (type == 2 || type < 0) {
			buffer.pos(x, y, z - dist).color(red, green, blue, 1.0f).endVertex();
			buffer.pos(x, y, z + dist).color(red, green, blue, 1.0f).endVertex();
		}
		tessellator.draw();
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	private void drawSizeLine(double x0, double z0, double x1, double y1, double z1, float size, float red, float green, float blue) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.glLineWidth(size);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(x0, 0.0, z0).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(x1, y1, z1).color(red, green, blue, 1.0f).endVertex();
		tessellator.draw();
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	private void drawCircle(double radius, float size, float red, float green, float blue) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.glLineWidth(size);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);

		buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

		for (int i = 0; i <= 32; ++i) {
			double angle = (double) i / 32.0d * Math.PI * 2;
			buffer.pos(0.0 + Math.cos(angle) * radius, 0.0, 0.0 + Math.sin(angle) * radius).color(red, green, blue, 1.0F).endVertex();
		}

		tessellator.draw();

		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	private void drawArrow(double radius, float size, float red, float green, float blue) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.glLineWidth(size);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

		buffer.pos(0.0d, 0.0d, 0.0d).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(0.0d, 0.0d, radius).color(red, green, blue, 1.0f).endVertex();

		buffer.pos(0.0d, 0.0d, radius).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(radius * 0.05d, 0.0d, radius * 0.75d).color(red, green, blue, 1.0f).endVertex();

		buffer.pos(0.0d, 0.0d, radius).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(radius * -0.05d, 0.0d, radius * 0.75d).color(red, green, blue, 1.0f).endVertex();

		buffer.pos(0.0d, 0.0d, radius).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(0.0d, radius * 0.05d, radius * 0.75d).color(red, green, blue, 1.0f).endVertex();

		buffer.pos(0.0d, 0.0d, radius).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(0.0d, radius * -0.05d, radius * 0.75d).color(red, green, blue, 1.0f).endVertex();

		tessellator.draw();
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	private void drawWork() {
		// work place
		GlStateManager.pushMatrix();
		GlStateManager.translate(0.0f, 0.0f, -300.0f);
		Gui.drawRect(workU + 1, workV + 1, workU + workS - 1, workV + workS - 1, GuiNpcAnimation.backColor);
		GlStateManager.popMatrix();

		// blocks
		GlStateManager.pushMatrix();
		postRender();
		IBlockState state;
		switch (blockType) {
			case 1: state = Blocks.AIR.getDefaultState(); break;
			case 3: state = Blocks.STONE_STAIRS.getDefaultState(); break;
			case 4: state = Blocks.STONE_SLAB.getDefaultState(); break;
			case 5: state = Blocks.CARPET.getDefaultState(); break;
			default: state = Blocks.STONE.getDefaultState(); break;
		}
		mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(516, 0.1F);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		float ytr = offsetY;
		GlStateManager.translate(-8.0f * winScale, ytr * winScale, 8.0f * winScale);
		GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f);
		GlStateManager.scale(-16.0f, -16.0f, -16.0f);
		GlStateManager.scale(winScale, winScale, winScale);
		int yH = blockType == 0 ? blockSize : 0;
		for (int y = -yH; y <= yH; y++) {
			for (int x = -blockSize; x <= blockSize; x++) {
				for (int z = -blockSize; z <= blockSize; z++) {
					BlockPos pos = new BlockPos(x, y + (blockSize == 0 ? 0 : 1), z);
					if (blockType == 0) {
						IBlockState s = environmentStates.get(pos);
						if (s != null) { state = s; }
					}
					if (!(state.getBlock() instanceof BlockAir)) {
						GlStateManager.pushMatrix();
						GlStateManager.translate(x, y - (blockSize == 0 || blockType > 1 ? 1 : 0), z);
						if (blockType == 4) {
							GlStateManager.translate(0.0f, 0.5f, 0.0f);
						} else if (blockType == 5) {
							GlStateManager.translate(0.0f, 0.9375f, 0.0f);
						}
						ClientEventHandler.renderBlock(state);
						GlStateManager.popMatrix();
					}
				}
			}
		}

		GlStateManager.pushMatrix();
		GlStateManager.translate(0.0f, 0.0f, -1.0f);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		if (meshType == 0) {
			drawLine(0.0d, 0.0d, 0.0d, 10.0d, 0, 1.0f, 0.0f, 0.0f);
			drawLine(0.0d, 0.0d, 0.0d, 10.0d, 1, 0.0f, 1.0f, 0.0f);
			drawLine(0.0d, 0.0d, 0.0d, 10.0d, 2, 0.0f, 0.0f, 1.0f);
		}
		else if (meshType == 1) {
			drawLine(0.0d, 0.0d, -11.0d, 11.0d, 0, 1.0f, 1.0f, 1.0f);
			drawLine(-11.0d, 0.0d, 0.0d, 11.0d, 2, 1.0f, 1.0f, 1.0f);
			for (int i = -10; i <= 11; i++) {
				drawLine(0.0d, 0.0d, i, 11.0d, 0, 1.0f, 1.0f, 1.0f);
				drawLine(i, 0.0d, 0.0d, 11.0d, 2, 1.0f, 1.0f, 1.0f);
			}
		}
		else if (meshType == 2) {
			drawLine(0.0d, -11.0d, 0.0d, 11.0d, 0, 1.0f, 1.0f, 1.0f);
			drawLine(-11.0d, 0.0d, 0.0d, 11.0d, 1, 1.0f, 1.0f, 1.0f);
			for (int i = -10; i <= 11; i++) {
				drawLine(0.0d, i, 0.0d, 11.0d, 0, 1.0f, 1.0f, 1.0f);
				drawLine(i, 0.0d, 0.0d, 11.0d, 1, 1.0f, 1.0f, 1.0f);
			}
		}
		else if (meshType == 3) {
			drawLine(0.0d, 0.0d, -11.0d, 11.0d, 1, 1.0f, 1.0f, 1.0f);
			drawLine(0.0d, -11.0d, 0.0d, 11.0d, 2, 1.0f, 1.0f, 1.0f);
			for (int i = -10; i <= 11; i++) {
				drawLine(0.0d, 0.0d, i, 11.0d, 1, 1.0f, 1.0f, 1.0f);
				drawLine(0.0d, i, 0.0d, 11.0d, 2, 1.0f, 1.0f, 1.0f);
			}
		}
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();

		// npc
		GlStateManager.enableAlpha();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableLighting();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

		GlStateManager.enableBlend();
		GlStateManager.enableColorMaterial();
		GlStateManager.translate(0.5f, 0.0f, -0.5f);
		mc.getRenderManager().playerViewY = 180.0f;
		EntityNPCInterface showNPC = getDisplayNpc();
		if (npc.ais.bodyOffsetX != 5.0f) {
			float f = (npc.ais.bodyOffsetX - 5.0f) / 10.0f;
			if (f == 0.5f) { f -= 1.0f; }
			GlStateManager.translate(f, 0.0f, 0.0f); }
		if (npc.ais.bodyOffsetZ != 5.0f) {
			float f = (npc.ais.bodyOffsetZ - 5.0f) / 10.0f;
			if (f == 0.5f) { f -= 1.0f; }
			GlStateManager.translate(0.0f, 0.0f, f);
		}
		if (showHitBox) {
			float w = npc.width / 2;
			AxisAlignedBB col= npc.getCollisionBoundingBox();
			if (col == null) { col = new AxisAlignedBB(-w, 0.0, -w, w, npc.height, w); }
			GlStateManager.glLineWidth(1.0F);
			GlStateManager.disableTexture2D();
			RenderGlobal.drawSelectionBoundingBox(col, 1.0f, 1.0f, 1.0f, 1.0f);
			GlStateManager.enableTexture2D();
		}

		// Damage hitboxes
		if (anim.type == AnimationKind.ATTACKING && frame.isNowDamage() && !frame.damageHitboxes.isEmpty()) {
			GlStateManager.glLineWidth(2.0F);
			GlStateManager.disableTexture2D();
			int i = 0;
			for (AxisAlignedBB aabb : anim.getDamageHitboxes(showNPC, frame.id)) {
				float r = 0.75f;
				float g = 0.5f;
				float b = 0.5f;
				float s = 1.0f;
				if (scrollHitboxes.getSelectedIndex() == i) {
					r = 1.0f;
					g = 0.0f;
					b = 0.25f;
					s = 2.0f;
				}
				if (scrollHitboxes.getSelectedIndex() == i) {
					r = 0.875f;
					g = 0.0f;
					b = 0.875f;
					s = 3.0f;
				}
				aabb = aabb.offset(-showNPC.posX, -showNPC.posY, -showNPC.posZ);
				GlStateManager.glLineWidth(s);
				RenderGlobal.drawSelectionBoundingBox(aabb, r, g, b, 1.0f);

				Vec3d center = aabb.getCenter();
				for (int j = 0; j < 3; j++) {
					AnimationDamageHitbox adh = frame.damageHitboxes.get(i);
					s = 0.1f;
					if (adh != null) {s *= adh.scale[j]; }
					drawLine(center.x, center.y, center.z, ValueUtil.correctDouble(s, 0.025d, 0.25d), j, b, r, g);
				}

				if (scrollHitboxes.getSelectedIndex() == i) {
					r = 0.5f;
					g = 0.5f;
					b = 0.5f;
					s = 1.0f;
					if (tools.getSlider(0).isHovered() || tools.getTextField(5).isHovered() || tools.getTextField(5).isFocused() || tools.getButton(30).isHoveredOrFocused()) {
						r = 0.825f;
						g = 0.625f;
						b = 0.195f;
						s = 3.0f;
					}
					// distance
					drawSizeLine(0.0f, 0.0f, center.x, 0.0f, center.z, s, r, g, b);

					r = 0.5f;
					g = 0.5f;
					b = 0.5f;
					s = 1.0f;
					if (tools.getSlider(1).isHovered() || tools.getTextField(6).isHovered() || tools.getTextField(6).isFocused() || tools.getButton(31).isHoveredOrFocused()) {
						r = 0.825f;
						g = 0.625f;
						b = 0.195f;
						s = 3.0f;
					}
					// height
					drawSizeLine(center.x, center.z, center.x, center.y, center.z, s, r, g, b);

					// round radius
					r = 0.5f;
					g = 0.5f;
					b = 0.5f;
					s = 1.0f;
					if (tools.getSlider(2).isHovered() || tools.getTextField(7).isHovered() || tools.getTextField(7).isFocused() || tools.getButton(32).isHoveredOrFocused()) {
						r = 0.825f;
						g = 0.625f;
						b = 0.195f;
						s = 3.0f;
					}
					drawCircle(Math.sqrt(Math.pow(center.x, 2.0d) + Math.pow(center.z, 2.0d)), s, r, g, b);
				}
				i++;
			}
			GlStateManager.enableTexture2D();
		}

		// Motion
		if (frame.motions[0] != 0.0f || frame.motions[1] != 0.0f || frame.motions[2] != 0.0f) {

			double radYaw = Math.toRadians(showNPC.rotationYaw) + frame.motions[2];

			double x = Math.sin(radYaw) * -frame.motions[0];
			double y = frame.motions[1];
			double z = Math.cos(radYaw) * frame.motions[0];

			float r = 0.5f;
			float g = 0.5f;
			float b = 0.75f;
			float s = 2.0f;
			if (isMotion) {
				b = 0.5f;
				if (tools.getSlider(0).isHovered() || tools.getTextField(5).isHovered() || tools.getTextField(5).isFocused() || tools.getButton(30).isHoveredOrFocused()) {
					r = 0.825f;
					g = 0.625f;
					b = 0.195f;
					s = 3.0f;
				}
				// distance
				drawSizeLine(0.0f, 0.0f, x, 0.0f, z, s, r, g, b);

				r = 0.5f;
				g = 0.5f;
				b = 0.5f;
				s = 1.0f;
				if (tools.getSlider(1).isHovered() || tools.getTextField(6).isHovered() || tools.getTextField(6).isFocused() || tools.getButton(31).isHoveredOrFocused()) {
					r = 0.825f;
					g = 0.625f;
					b = 0.195f;
					s = 3.0f;
				}
				// height
				drawSizeLine(x, z, x, y, z, s, r, g, b);

				// round radius
				r = 0.5f;
				g = 0.5f;
				b = 0.5f;
				s = 1.0f;
				if (tools.getSlider(2).isHovered() || tools.getTextField(7).isHovered() || tools.getTextField(7).isFocused() || tools.getButton(32).isHoveredOrFocused()) {
					r = 0.825f;
					g = 0.625f;
					b = 0.195f;
					s = 3.0f;
				}
				drawCircle(Math.sqrt(Math.pow(x, 2.0d) + Math.pow(z, 2.0d)), s, r, g, b);

				r = 0.25f;
				g = 0.25f;
				b = 0.875f;
				s = 3.0f;
			}
			double radius = Math.sqrt(Math.pow(x, 2.0d) + Math.pow(y, 2.0d) + Math.pow(z, 2.0d));

			GlStateManager.pushMatrix();
			GlStateManager.rotate((float) Math.toDegrees(-radYaw), 0.0f, 1.0f, 0.0f);
			GlStateManager.rotate((float) Math.toDegrees(-Math.atan2(frame.motions[1], frame.motions[0])), 1.0f, 0.0f, 0.0f);
			drawArrow(radius, s, r, g, b);

			GlStateManager.popMatrix();
		}

		// model mesh rotation axes:

		ModelNpcAlt.editAnimDataSelect.displayNpc = showNPC;
		float r = showNPC.rotationYaw < 0.0f ? showNPC.rotationYaw + 360.0f : showNPC.rotationYaw;
		float oy = 0.0f;
		showNPC.currentAnimation = npc.currentAnimation;
		mc.getRenderManager().renderEntity(showNPC, 0.0, oy, 0.0, 0.0f, r / -45.0f + 8.0f, false);

		if (blockType == 0) {
			for (Entity e : environmentEntitys) {
				int x = Math.abs((int) Math.round(e.posX));
				int y = Math.abs((int) Math.round(e.posY));
				int z = Math.abs((int) Math.round(e.posZ));
				int d = x;
				if (d < y) { d = y; }
				if (d < z) { d = z; }
				if (d > blockSize) { continue; }
				GlStateManager.pushMatrix();
				mc.getRenderManager().renderEntity(e, e.posX, e.posY, e.posZ, 0.0f, 0.0f, false);
				GlStateManager.popMatrix();
			}
		}

		GlStateManager.disableRescaleNormal();
		GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GlStateManager.disableTexture2D();
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GlStateManager.disableLighting();
		GlStateManager.popMatrix();
	}

	public EntityNPCInterface getDisplayNpc() { return onlyCurrentPart ? npcPart : npcAnim; }

	private void playButtonClick() {
		mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	private void postRender() {
		GlStateManager.translate(workU + workS / 2.0f, workV + workS / 2.0f, 100.0f * dispScale); // center
		GlStateManager.translate(dispPos[0], dispPos[1], 0.0f);
		GlStateManager.rotate(dispRot[0], 0.0f, 1.0f, 0.0f);
		GlStateManager.rotate(dispRot[1], 1.0f, 0.0f, 0.0f);
		GlStateManager.rotate(dispRot[2], 0.0f, 0.0f, 1.0f);
		GlStateManager.scale(dispScale, dispScale, dispScale);
		GlStateManager.translate(0.0f, 25.0f, 0.0f);
	}

	private void resetAnimation() {
		if (!isChanged || anim == null || frame == null || npcAnim == null) { return; }
		npcAnim.posX = basePos.x;
		npcAnim.posY = basePos.y;
		npcAnim.posZ = basePos.z;
		npcPart.posX = basePos.x;
		npcPart.posY = basePos.y;
		npcPart.posZ = basePos.z;

		npcAnim.animation.reset();
		npcAnim.animation.tryRunAnimation(anim, AnimationKind.EDITING_All);
		npcAnim.setHealth(npcAnim.getMaxHealth());
		npcAnim.deathTime = 0;

		npcPart.animation.reset();
		npcPart.animation.tryRunAnimation(anim, AnimationKind.EDITING_PART);
		npcPart.setHealth(npcPart.getMaxHealth());
		npcPart.deathTime = 0;
		if (getButton(53) != null) {
			getButton(53).setIsVisible(onlyCurrentPart);
			if (onlyCurrentPart) {
				int s = npcPart.animation.getAnimationSpeedTicks();
				Object[] ticks = new Component[s + 1];
				for (int i = 0; i <= s; i++) { ticks[i] = Component.literal(i + "/" + s); }
				getButton(53).setVariants(ticks);
			}
		}
	}


}
