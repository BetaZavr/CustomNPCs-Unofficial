package noppes.npcs.entity.data;

import java.util.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.INbt;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.api.entity.data.IAnimation;
import noppes.npcs.api.entity.data.IEmotion;
import noppes.npcs.api.entity.data.INPCAnimation;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.client.model.animation.*;
import noppes.npcs.constants.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.*;

import javax.annotation.Nonnull;

public class DataAnimation implements INPCAnimation {

    public final LivingEntity entity;

    private final CustomAnimationHandler animationHandler;
    private final CustomEmotionHandler emotionHandler;

    // Tools
    public final Map<EnumParts, Boolean> showParts = new HashMap<>();
    public final Map<EnumParts, Boolean> showArmorParts = new HashMap<>();
    public final Map<EnumParts, Boolean> showAWParts = new HashMap<>();

    public DataAnimation(@Nonnull LivingEntity main) {
        entity = main;
        animationHandler = new CustomAnimationHandler(main);
        emotionHandler = new CustomEmotionHandler(main);
        showParts.put(EnumParts.HEAD, true);
        showParts.put(EnumParts.BODY, true);
        showParts.put(EnumParts.ARM_RIGHT, true);
        showParts.put(EnumParts.ARM_LEFT, true);
        showParts.put(EnumParts.LEG_RIGHT, true);
        showParts.put(EnumParts.LEG_LEFT, true);
        showParts.put(EnumParts.CUSTOM, true);

        showAWParts.put(EnumParts.HEAD, true);
        showAWParts.put(EnumParts.BODY, true);
        showAWParts.put(EnumParts.ARM_RIGHT, true);
        showAWParts.put(EnumParts.ARM_LEFT, true);
        showAWParts.put(EnumParts.LEG_RIGHT, true);
        showAWParts.put(EnumParts.LEG_LEFT, true);
        showAWParts.put(EnumParts.CUSTOM, true);

        showArmorParts.put(EnumParts.HEAD, true);
        showArmorParts.put(EnumParts.BODY, true);
        showArmorParts.put(EnumParts.ARM_RIGHT, true);
        showArmorParts.put(EnumParts.ARM_LEFT, true);
        showArmorParts.put(EnumParts.LEG_RIGHT, true);
        showArmorParts.put(EnumParts.LEG_LEFT, true);
        showArmorParts.put(EnumParts.FEET_RIGHT, true);
        showArmorParts.put(EnumParts.FEET_LEFT, true);
        showArmorParts.put(EnumParts.CUSTOM, true);
    }

    public void resetShowParts() {
        showParts.replaceAll((k, v) -> true);
        showArmorParts.replaceAll((k, v) -> true);
    }

    public void resetShowAWParts() {
        showAWParts.replaceAll((k, v) -> true);
    }

    public void reset() {
        stopAnimation();
        stopEmotion();
        if (entity == null || entity.level().isClientSide()) { return; }
        Packets.sendAll(new PacketCustomAnimationSet(entity instanceof Player, entity.level().dimension(), entity.getId(), entity.getUUID(), save(new CompoundTag())));
    }

    public void setRotationAnglesAnimation(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, float partialTicks) {
        animationHandler.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, partialTicks);
        // set show parts
        if (animationHandler.currentFrame != null) {
            // show model parts
            for (PartConfig part : animationHandler.currentFrame.parts.values()) {
                showParts.put(part.getEnumType(), part.isShow());
            }
            // show armor
            showArmorParts.put(EnumParts.HEAD, animationHandler.currentFrame.showHelmet);
            showArmorParts.put(EnumParts.BODY, animationHandler.currentFrame.showBody);
            showArmorParts.put(EnumParts.ARM_RIGHT, animationHandler.currentFrame.showBody);
            showArmorParts.put(EnumParts.ARM_LEFT, animationHandler.currentFrame.showBody);
            showArmorParts.put(EnumParts.LEG_RIGHT, animationHandler.currentFrame.showLegs);
            showArmorParts.put(EnumParts.LEG_LEFT, animationHandler.currentFrame.showLegs);
            showArmorParts.put(EnumParts.FEET_RIGHT, animationHandler.currentFrame.showFeets);
            showArmorParts.put(EnumParts.FEET_LEFT, animationHandler.currentFrame.showFeets);
        }
    }

    public void stopAnimation() {
        if (animationHandler.activeAnimation != null) {
            if (animationHandler.activeAnimation.hasEmotion()) { stopEmotion(); }
            if (entity != null && !entity.level().isClientSide()) {
                Packets.sendAll(new PacketCustomAnimationStop(entity instanceof Player, entity.level().dimension(), entity.getId(), entity.getUUID()));
            }
        }
        if (!animationHandler.movementAnimation.isEmpty()) {
            animationHandler.movementAnimation.clear();
            Map<Integer, Integer> map = animationHandler.resetWalkAndStandAnimations();
            if (map == null) { map = new HashMap<>(); }
            if (entity != null && !entity.level().isClientSide()) {
                Packets.sendAll(new PacketCustomAnimationBaseSet(entity instanceof Player, entity.level().dimension(), entity.getId(), entity.getUUID(), map));
            }
        }
        animationHandler.stopAnimation();
    }

    public void stopEmotion() {
        if (entity != null && !entity.level().isClientSide() && emotionHandler.activeEmotion != null) {
            Packets.sendAll(new PacketCustomEmotionStop(entity instanceof Player, entity.level().dimension(), entity.getId(), entity.getUUID()));
        }
        emotionHandler.stopEmotion();
    }

    @Override
    public void clear() {
        emotionHandler.stopEmotion();
        animationHandler.clear();
    }

    public boolean hasAnimation(AnimationKind type) {
        return animationHandler.hasAnimation(type);
    }

    public void load(CompoundTag compound) {
        animationHandler.load(compound);
        emotionHandler.load(compound);
    }

    public CompoundTag save(CompoundTag compound) {
        animationHandler.save(compound);
        emotionHandler.save(compound);
        return compound;
    }

    public boolean isAnimated() {
        return animationHandler.isAnimated();
    }

    public void updateTime() {
        animationHandler.updateTime();
        emotionHandler.updateTime();
    }

    public boolean isAnimated(AnimationKind ... types) {
        return animationHandler.isAnimated(types);
    }

    // a new motion animation is selected
    public void resetWalkAndStandAnimations() {
        if (entity == null || entity.level().getGameTime() % 20 != 0) { return; }
        Map<Integer, Integer> map = animationHandler.resetWalkAndStandAnimations();
        if (!entity.level().isClientSide() && map != null) {
            Packets.sendAll(new PacketCustomAnimationBaseSet(entity instanceof Player, entity.level().dimension(), entity.getId(), entity.getUUID(), map));
        }
    }

    // used to select a new animation in EntityNPCInterface
    public AnimationConfig tryRunAnimation(AnimationKind type) {
        if (!CustomNpcs.ShowCustomAnimation) { return null; }
        AnimationConfig anim = animationHandler.selectAnimation(type);
        if (anim == null) { return null; }
        return tryRunAnimation(anim, type);
    }

    // (Player or NPC) -> this.reset(AnimationKind), or PacketHandlerClient, or Animation GUI
    public AnimationConfig tryRunAnimation(AnimationConfig anim, AnimationKind type) {
        if (entity == null) { return null; }
        if (anim != null && anim.frames.isEmpty()) { anim = null; }
        if (anim == null && animationHandler.activeAnimation != null && !animationHandler.activeAnimation.type.isMovement()) {
            stopAnimation();
            return null;
        }
        anim = animationHandler.tryRunAnimation(anim, type);
        if (entity.level().isClientSide()) { return anim; }
        if (anim == null) {
            Packets.sendAll(new PacketCustomAnimationStop(entity instanceof Player, entity.level().dimension(), entity.getId(), entity.getUUID()));
        }
        else {
            Packets.sendAll(new PacketCustomAnimationRun(entity instanceof Player, entity.level().dimension(), entity.getId(), entity.getUUID(), anim.id, type));
        }
        return anim;
    }

    public AnimationConfig getAnimation() { return animationHandler.activeAnimation; }

    public boolean getJump() { return animationHandler.isJump; }

    public void setJump(boolean bo) { animationHandler.isJump = bo; }

    public boolean getSwing() { return animationHandler.isSwing; }

    public void setSwing(boolean bo) { animationHandler.isSwing = bo; }

    public EnumAnimationStages getAnimationStage() { return animationHandler.stage; }

    public boolean getAnimationPartShow(int partId) {
        return animationHandler.currentFrame == null || !animationHandler.currentFrame.parts.containsKey(partId) || animationHandler.currentFrame.parts.get(partId).show;
    }

    public Float[] getAnimationPartData(int partId) { return animationHandler.rotationAngles.get(partId); }

    public void loadBaseAnimations(Map<Integer, Integer> map) { animationHandler.loadBaseAnimations(map); }

    public AnimationFrameConfig getPreFrame() { return animationHandler.preFrame; }

    public void addAnimation(AnimationKind type, int id) { animationHandler.addAnimation(type, id); }

    public boolean removeAnimation(AnimationKind type, int id) { return animationHandler.removeAnimation(type, id); }

    public boolean hasAnimation(AnimationKind type, int id) { return animationHandler.hasAnimation(type, id); }

    public boolean canSetBaseRotationAngles() { return animationHandler.canSetBaseRotationAngles(); }

    public boolean canBeAnimated() { return animationHandler.canBeAnimated(); }

    public ItemStack getCurrentHeldStack(boolean isMainHand) { return animationHandler.getCurrentHeldStack(isMainHand); }

    public int getAnimationCurrentFrameID() { return animationHandler.getAnimationCurrentFrameID(); }

    public int getAnimationNextFrameID() { return animationHandler.getAnimationNextFrameID(); }

    public int getAnimationTicks() { return animationHandler.getAnimationTicks(); }

    public int getAnimationSpeedTicks() { return animationHandler.getAnimationSpeedTicks(); }

    // Emotions
    public EmotionFrame getCurrentEmotionFrame() { return emotionHandler.currentFrame; }

    public EmotionConfig getActiveEmotion() { return emotionHandler.activeEmotion; }

    public int getBaseEmotionId() { return emotionHandler.baseEmotionId; }

    public void setBaseEmotionId(int emotionId) { emotionHandler.baseEmotionId = emotionId; }

    public long getStartEmotionTime() { return emotionHandler.startEmotionTime; }

    public Map<Integer, Float[]> getEmotionData() { return emotionHandler.rotationAngles; }

    @SuppressWarnings("all")
    public void setActiveEmotion(EmotionConfig emotion) { emotionHandler.activeEmotion = emotion; }

    public void tryRunEmotion(EmotionConfig emotion) {
        if (entity == null) { return; }
        if (emotion != null && emotion.frames.isEmpty()) { emotion = null; }
        if (emotion == null) {
            stopEmotion();
            return;
        }
        emotion = emotionHandler.tryRunEmotion(emotion);
        if (entity.level().isClientSide()) { return; }
        if (emotion == null) {
            Packets.sendAll(new PacketCustomEmotionStop(entity instanceof Player, entity.level().dimension(), entity.getId(), entity.getUUID()));
        }
        else {
            Packets.sendAll(new PacketCustomEmotionRun(entity instanceof Player, entity.level().dimension(), entity.getId(), entity.getUUID(), emotion.id));
        }
    }

    public boolean isEmoted() { return emotionHandler.isAnimated(); }

    public void setRotationAnglesEmotion(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, float partialTicks) {
        emotionHandler.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, partialTicks);
    }

    public boolean emotionIsDisableMoved() {
        if (emotionHandler.currentFrame == null) { return true; }
        return emotionHandler.currentFrame.disable;
    }

    public EmotionFrame getEmotionCurrentFrame() { return emotionHandler.currentFrame; }

    @Override
    public boolean hasAnimations(int animationType) {
        if (animationType >= 0 && animationType < AnimationKind.values().length) {
            return hasAnimation(AnimationKind.values()[animationType]);
        }
        throw new CustomNPCsException("Animation type must be between 0 and " + (AnimationKind.values().length - 1));
    }

    @Override
    public boolean hasAnimation(int animationType, int animationId) {
        if (animationType >= 0 && animationType < AnimationKind.values().length) {
            return hasAnimation(AnimationKind.values()[animationType], animationId);
        }
        throw new CustomNPCsException("Animation type must be between 0 and " + (AnimationKind.values().length - 1));
    }

    @Override
    public INbt getNbt() { return new NBTWrapper(save(new CompoundTag())); }

    @Override
    public void setNbt(INbt nbt) {
        if (nbt != null) { load(nbt.getMCNBT()); }
    }

    @Override
    public void update() {
        if (entity == null || entity.level().isClientSide()) { return; }
        Packets.sendAll(new PacketCustomAnimationSet(entity instanceof Player, entity.level().dimension(), entity.getId(), entity.getUUID(), save(new CompoundTag())));
    }

    @Override
    public void addAnimation(int animationType, int animationId) {
        if (animationType >= 0 && animationType < AnimationKind.values().length) {
            addAnimation(AnimationKind.values()[animationType], animationId);
            return;
        }
        throw new CustomNPCsException("Animation type must be between 0 and " + (AnimationKind.values().length - 1));
    }

    @Override
    public IEmotion getEmotion() { return emotionHandler.activeEmotion; }

    @Override
    public void startEmotion(int emotionId) {
        emotionHandler.tryRunEmotion(emotionId);
    }

    @Override
    public IAnimation[] getAnimations(int animationType) {
        if (animationType >= 0 && animationType < AnimationKind.values().length) {
            return animationHandler.getAnimations(AnimationKind.values()[animationType]);
        }
        throw new CustomNPCsException("Animation type must be between 0 and " + (AnimationKind.values().length - 1));
    }

    @Override
    public boolean removeAnimation(int animationType, int animationId) {
        if (animationType >= 0 && animationType < AnimationKind.values().length) {
            return removeAnimation(AnimationKind.values()[animationType], animationId);
        }
        throw new CustomNPCsException("Animation type must be between 0 and " + (AnimationKind.values().length - 1));
    }

    @Override
    public void removeAnimations(int animationType) {
        if (animationType >= 0 && animationType < AnimationKind.values().length) {
            animationHandler.removeAnimations(AnimationKind.values()[animationType]);
            return;
        }
        throw new CustomNPCsException("Animation type must be between 0 and " + (AnimationKind.values().length - 1));
    }

}
