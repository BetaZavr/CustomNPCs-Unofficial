package noppes.npcs.client.gui.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.model.animation.EmotionConfig;
import noppes.npcs.client.model.animation.EmotionFrame;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.AnimationController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAnimation;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketAnimationGet;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiSliderNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;

import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("unused")
public class GuiNpcEmotion extends GuiNPCInterface2
        implements ICustomScrollListener, IGuiData, ITextfieldListener, ISliderListener {

    public static final ResourceLocation etns = new ResourceLocation(CustomNpcs.MODID, "textures/gui/emotion/buttons.png");

    public EntityNPCInterface npcEmtn;
    public int elementType = 0; // 0 - eye, 1 - pupil, 2 - brow, 3 - mouth
    public boolean isRight = true;

    protected final String[] types = new String[] { "gui.small", "gui.normal", "gui.select" };
    protected final Map<String, EmotionConfig> dataEmtns = new TreeMap<>();
    protected final DataAnimation animation;
    protected boolean onlyPart = false;
    protected int toolType = 0; // 0 - rotation, 1 - offset, 2 - scale
    protected String selEmtn;
    protected EmotionFrame frame;
    protected GuiCustomScrollNop scroll;
    protected AnimationController aData;

    public GuiNpcEmotion(EntityNPCInterface npc) {
        super(npc, 4);
        setBackground("bgfilled.png");
        closeOnEsc = true;
        backGui = EnumGuiType.MainMenuAdvanced;

        animation = new DataAnimation(npc);
        animation.setBaseEmotionId(npc.animation.getBaseEmotionId());

        selEmtn = "";
        npcEmtn = Util.instance.copyToGUI(npc, player.level(), false);
        Packets.sendServer(new SPacketAnimationGet(npc.getId()));
    }

    @Override
    public void save() {

    }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) {

    }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) {

    }

    @Override
    public void setGuiData(CompoundTag compound) {

    }

    @Override
    public void mouseDragged(GuiSliderNop slider) {

    }

    @Override
    public void mousePressed(GuiSliderNop slider) {

    }

    @Override
    public void mouseReleased(GuiSliderNop slider) {

    }

    @Override
    public void unFocused(GuiTextFieldNop textField) {

    }
}
