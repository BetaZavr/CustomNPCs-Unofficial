package noppes.npcs.client.gui.animation;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiNPCInterface;
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
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;
import java.util.*;

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
        dataType.put(Component.translatable("puppet." + AnimationKind.SITTING.name().toLowerCase().replace("_", "")), AnimationKind.SITTING);
        dataType.put(Component.translatable("puppet." + AnimationKind.SLEEPING.name().toLowerCase().replace("_", "")), AnimationKind.SLEEPING);
        dataType.put(Component.translatable("puppet." + AnimationKind.STANDING.name().toLowerCase().replace("_", "")), AnimationKind.STANDING);
        dataType.put(Component.translatable("puppet." + AnimationKind.SNEAK_STAND.name().toLowerCase().replace("_", "")), AnimationKind.SNEAK_STAND);
        dataType.put(Component.translatable("puppet." + AnimationKind.FLY_STAND.name().toLowerCase().replace("_", "")), AnimationKind.FLY_STAND);
        dataType.put(Component.translatable("puppet." + AnimationKind.WATER_STAND.name().toLowerCase().replace("_", "")), AnimationKind.WATER_STAND);
        dataType.put(Component.translatable("puppet." + AnimationKind.REVENGE_STAND.name().toLowerCase().replace("_", "")), AnimationKind.REVENGE_STAND);
        dataType.put(Component.translatable("puppet." + AnimationKind.WALKING.name().toLowerCase().replace("_", "")), AnimationKind.WALKING);
        dataType.put(Component.translatable("puppet." + AnimationKind.SNEAK_WALK.name().toLowerCase().replace("_", "")), AnimationKind.SNEAK_WALK);
        dataType.put(Component.translatable("puppet." + AnimationKind.FLY_WALK.name().toLowerCase().replace("_", "")), AnimationKind.FLY_WALK);
        dataType.put(Component.translatable("puppet." + AnimationKind.WATER_WALK.name().toLowerCase().replace("_", "")), AnimationKind.WATER_WALK);
        dataType.put(Component.translatable("puppet." + AnimationKind.REVENGE_WALK.name().toLowerCase().replace("_", "")), AnimationKind.REVENGE_WALK);
        dataType.put(Component.translatable("puppet." + AnimationKind.DIALOG.name().toLowerCase().replace("_", "")), AnimationKind.DIALOG);
    }

    public GuiNpcAnimation(@Nonnull EntityCustomNpc npcIn) {
        super(npcIn);
        setBackground("bgfilled.png");
        imageWidth = 420;
        imageHeight = 217;

        animation = new DataAnimation(npcIn);
        int i = 0;
        for (AnimationKind ak : dataType.values()) {
            List<Component> list = new ArrayList<>();
            Util.instance.putHovers(list, Component.translatable("animation.hover.anim." + ak.get()));
            typeHovers.put(i, list);
            i++;
        }
        npcAnim = Util.instance.copyToGUI(npc, player.level(), false);
        npcAnim.display.setName(npc.getName()+"_animation");
        Packets.sendServer(new SPacketAnimationGet(npc.getId()));
    }

    @Override
    public void buttonEvent(@Nonnull GuiButtonNop button) {
        AnimationConfig anim = getAnim();
        switch (button.id) {
            case 0: {
                if (scrollType == null || !scrollType.hasSelected()) { return; }
                AnimationConfig newAnim = aData.createNewAnim();
                newAnim.name = Util.instance.deleteColor(selType.getString().replaceAll(" ", "_")+ "_" + newAnim.id);
                newAnim.type = dataType.get(selType);
                animation.addAnimation(newAnim.type, newAnim.id);
                selAnim = newAnim.getSettingName();
                isChanged = true;
                init();
                CustomNPCsScheduler.runTack(() -> setSubGui(new SubGuiEditAnimation(npc, newAnim, 4)), 50);
                break;
            } // add anim
            case 1: {
                if (anim == null) { return; }
                AnimationController aData = AnimationController.getInstance();
                anim = aData.copy(anim.id, dataType.get(selType));
                selAnim = anim.getSettingName();
                selBaseAnim = anim.getSettingName();
                if (dataType.containsKey(selType)) { animation.addAnimation(dataType.get(selType), anim.id); }
                isChanged = true;
                init();
                break;
            } // copy anim
            case 2: {
                if (anim != null) {
                    AnimationConfig finalAnim = anim;
                    ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
                        if (agree) {
                            if (dataType.containsKey(selType)) { animation.removeAnimation(dataType.get(selType), finalAnim.id); }
                            AnimationController.getInstance().removeAnimation(finalAnim.id);
                            isChanged = true;
                            init();
                        }
                        NoppesUtil.openGUI(player, this);
                    },
                            anim.getSettingName(),
                            Component.translatable("message.delete"));
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
    public void save() { Packets.sendAll(new SPacketAnimationSave(animation.save(new CompoundTag()))); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (hasSubGui()) {
            getSubGui().render(graphics, mouseX, mouseY, partialTicks);
            return;
        }
        super.render(graphics, mouseX, mouseY, partialTicks);
        AnimationConfig anim = getAnim();
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(guiLeft + 342.0f, guiTop + 9.0f, 0.0f);
        graphics.fill(-1, -1, 56, 91, 0xFFF080F0);
        graphics.fill(0, 0, 55, 90, GuiNpcAnimation.backColor);
        matrixStack.popPose();
        if (anim != null && npcAnim != null) {
            npcAnim.animation.updateTime();
            npcAnim.chasingPosZ = npc.chasingPosZ;
            npcAnim.chasingPosY = npc.chasingPosY;
            npcAnim.chasingPosX = npc.chasingPosX;
            npcAnim.prevChasingPosZ = npc.prevChasingPosZ;
            npcAnim.prevChasingPosY = npc.prevChasingPosY;
            npcAnim.prevChasingPosX = npc.prevChasingPosX;
            npcAnim.tickCount = npc.tickCount;
            npcAnim.deathTime = 0;
            drawNpc(graphics, npcAnim, 369, 81, 1.0f, 0, 0, 1);
        }
        matrixStack.pushPose();
        matrixStack.translate(guiLeft + 315.0f, guiTop - 2.0f, 0.0f);
        int color = 0xA0000000;
        int hoverButton = -1;
        for (int i = 0; i < 5; i++) {
            matrixStack.translate(0.0f, 22.0f, 0.0f);
            int c = color;
            if (getButton(i) != null && getButton(i).isHoveredOrFocused()) {
                c = 0xA0FFFF00;
                hoverButton = i;
            }
            graphics.hLine(0, 0, 0, color);
            graphics.hLine(0, 1, 1, color);
            graphics.hLine(0, 2, 2, color);
            graphics.hLine(0, 3, 3, color);
            graphics.hLine(0, 0, 4, color);
            graphics.hLine(1, 14, 4, c);
            graphics.hLine(0, 3, 5, color);
            graphics.hLine(0, 2, 6, color);
            graphics.hLine(0, 1, 7, color);
            graphics.hLine(0, 0, 8, color);
        }
        matrixStack.translate(11.0f, -10.0f, 0.0f);
        graphics.vLine(0, 0, 2, color);
        graphics.vLine(1, -1, 2, color);
        graphics.vLine(2, -2, 2, color);
        graphics.vLine(3, -3, 2, color);
        graphics.vLine(4, 0, 2, color);
        if (hoverButton != -1) {
            graphics.vLine(4, -75 + hoverButton * 22, 1, 0xA0FFFF00);
            if (hoverButton != 0) { graphics.vLine(4, -75, -74 + hoverButton * 22, color); }
        }
        else { graphics.vLine(4, -75, 1, color); }
        graphics.vLine(5, -3, 2, color);
        graphics.vLine(6, -2, 2, color);
        graphics.vLine(7, -1, 2, color);
        graphics.vLine(8, 0, 2, color);
        matrixStack.popPose();
    }

    @Override
    public void init() {
        super.init();
        int x = guiLeft + 8;
        int y = guiTop + 14;
        if (scrollType == null) {
            scrollType = addScroll(0).setSize(120, 198)
                    .setUnsortedList(new ArrayList<>(dataType.keySet()))
                    .setHoverTexts(typeHovers);
        }
        add(scrollType.setPos(x, y));
        if (selType.getString().isEmpty()) {
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
        x += scrollType.getWidth() + 3;
        dataAnimations.clear();
        dataAllAnimations.clear();
        aData = AnimationController.getInstance();
        ArrayList<Component> allAnimations = Lists.newArrayList();
        LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
        int i = 0;
        AnimationKind type = dataType.get(selType);
        if (!selAnim.getString().isEmpty() && !selBaseAnim.getString().isEmpty() &&
                !Util.instance.deleteColor(selAnim.getString()).equals(Util.instance.deleteColor(selBaseAnim.getString()))) {
            selAnim = Component.empty();
        }
        for (AnimationConfig ac : aData.getAnimations()) {
            Component key = Component.empty()
                    .append(ac.getSettingName().withStyle(type == ac.type ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            if (animation.hasAnimation(type, ac.id)) { dataAnimations.add(key); }
            dataAllAnimations.put(key, ac);
            if (!selAnim.getString().isEmpty() && Util.instance.deleteColor(selAnim.getString()).equals(Util.instance.deleteColor(key.getString()))) {
                selAnim = key;
            }
            if (!selBaseAnim.getString().isEmpty() && Util.instance.deleteColor(selBaseAnim.getString()).equals(Util.instance.deleteColor(key.getString()))) {
                selBaseAnim = key;
            }
            allAnimations.add(key);
            List<Component> list = new ArrayList<>();
            list.add(Component.translatable(ac.name));
            list.add(Component.empty()
                    .append(Component.translatable("gui.type").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(ac.type.name()).withStyle(ChatFormatting.RESET)));
            hts.put(i, list);
            i++;
        }
        if (scrollAnimations == null) {
            scrollAnimations = addScroll(1).setSize(120, 198)
                    .setHoverTexts("animation.hover.anim.list");
        }
        add(scrollAnimations.setPos(x, y)
                .setUnsortedList(dataAnimations));
        addLabel(1, x + 1, y - 10, Component.translatable("animation.setts"))
                .setSize(120, 10);

        if (selAnim.getString().isEmpty() && selBaseAnim.getString().isEmpty() && !scrollAnimations.getList().isEmpty()) {
            for (Component key : scrollAnimations.getNormalList()) {
                selAnim = key;
                selBaseAnim = key;
                break;
            }
        }
        if (!selAnim.getString().isEmpty()) {
            scrollAnimations.setSelected(selAnim);
            if (!scrollAnimations.hasSelected()) { selAnim = Component.empty(); }
            else { selAnim = scrollAnimations.getNormalSelected(); }
        }
        else { scrollAnimations.clearSelection(); }
        if (selBaseAnim.getString().isEmpty()) { selBaseAnim = selAnim; }

        x += 123;
        if (scrollAllAnimations == null) { scrollAllAnimations = addScroll(2).setSize(160, 110); }
        add(scrollAllAnimations.setPos(x, y + 88)
                .setUnsortedList(allAnimations)
                .setHoverTexts(hts));
        if (!selBaseAnim.getString().isEmpty()) { scrollAllAnimations.setSelected(selBaseAnim); }
        AnimationConfig anim = getAnim();
        addLabel(2, x + 1, y - 10, Component.translatable("movement.animation").append(":"))
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
        boolean isOP = anim != null && !anim.immutable;
        addButton(2, x, y += 22, "gui.remove")
                .setSize(60, 20)
                .setIsEnabled(isOP)
                .setHoverTexts("animation.hover.anim.del");
        // edit
        addButton(3, x, y + 22, "gui.edit")
                .setSize(60, 20)
                .setIsEnabled(isOP || player.getName().getString().startsWith("BetaZavr"))
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
            if (selAnim.getString().equals(scroll.getSelected())) { return; }
            selAnim = scroll.getNormalSelected();
            scrollAllAnimations.setSelected(selAnim);
            selBaseAnim = scrollAllAnimations.getNormalSelected();
            isChanged = true;
        } // animation in type
        else if (scroll.id == 2) {
            if (selBaseAnim.getString().equals(scroll.getSelected())) { return; }
            selBaseAnim = scroll.getNormalSelected();
            if (selBaseAnim.getString().equals(scrollAnimations.getSelected())) {
                scrollAnimations.setSelected(selBaseAnim);
                selAnim = scrollAnimations.getNormalSelected();
            }
            isChanged = true;
        } // animation in base
        if (isChanged) { init(); }
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
                init();
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
                init();
            }
        }
    }

    @Override
    public void setGuiData(CompoundTag compound) {
        animation.load(compound);
        isChanged = true;
        init();
    }

    @Override
    public void subGuiClosed(Screen subgui) {
        if (subgui instanceof SubGuiEditAnimation gui && gui.anim != null) {
            selAnim = gui.anim.getSettingName();
            selBaseAnim = selAnim;
            Packets.sendAll(new SPacketAnimationChange(3, gui.anim.save()));
            isChanged = true;
            init();
        } // create new
    }

    @Override
    public void onClose() {
        NoppesUtil.requestOpenGUI(EnumGuiType.MainMenuAdvanced);
        super.onClose();
    }

    protected AnimationConfig getAnim() {
        if (!dataAnimations.contains(selAnim) && !selAnim.getString().isEmpty()) { selAnim = Component.empty(); }
        if (!selAnim.getString().isEmpty() && dataAllAnimations.containsKey(selAnim)) { return dataAllAnimations.get(selAnim); }
        if (selBaseAnim.getString().isEmpty() || !dataAllAnimations.containsKey(selBaseAnim)) { return null; }
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
