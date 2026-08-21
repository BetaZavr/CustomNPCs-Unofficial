package noppes.npcs.potions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
import noppes.npcs.EventHooks;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.event.CustomPotionEvent;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.client.renderer.effects.CustomMobEffectRenderer;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;

// MobEffects; OLD: PotionType
public class CustomMobEffect extends MobEffect implements ICustomElement {

    protected final @Nonnull CompoundTag nbtData;
    protected final @Nonnull ItemStack cureItem;

    @SuppressWarnings("unused")
    public CustomMobEffect(@Nonnull MobEffectCategory category, @Nonnull ResourceLocation location, @Nonnull CompoundTag nbtPotion) {
        super(category, nbtPotion.getInt("LiquidColor"));
        nbtData = nbtPotion;

        if (nbtPotion.contains("CureItem", 10)) { cureItem = ItemStack.of(nbtPotion.getCompound("CureItem")); }
        else { cureItem = ItemStack.EMPTY; }

        if (nbtPotion.contains("Modifiers", 9)) {
            for (int i = 0; i < nbtPotion.getList("Modifiers", 10).size(); i++) {
                CompoundTag potionModifier = nbtPotion.getList("Modifiers", 10).getCompound(i);
                try {
                    double d = potionModifier.getDouble("AttributeDefValue");
                    double m = potionModifier.getDouble("AttributeMinValue");
                    double n = potionModifier.getDouble("AttributeMaxValue");
                    UUID uuid;
                    try { uuid = UUID.fromString(potionModifier.getString("UUID")); }
                    catch (Exception e) { uuid = UUID.randomUUID(); }
                    addAttributeModifier(new RangedAttribute(potionModifier.getString("AttributeName"),
                                    ValueUtil.correctDouble(d, m, n), ValueUtil.min(m, n), ValueUtil.max(m, n)),
                            uuid.toString(), potionModifier.getDouble("Amount"),
                            switch (potionModifier.getInt("Operation")) {
                                case 1 -> AttributeModifier.Operation.MULTIPLY_BASE;
                                case 2 -> AttributeModifier.Operation.MULTIPLY_TOTAL;
                                default -> AttributeModifier.Operation.ADDITION;
                            });
                }
                catch (Exception e) { LogWriter.error("Error create or added attribute modifier #" + i + " to custom potion: \"" + getCustomName() + "\"", e); }
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) { consumer.accept(new CustomMobEffectRenderer(this)); }

    @Override
    public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirectSource,
                                        @Nonnull LivingEntity entityLiving, int amplifier, double health) {
        EventHooks.onCustomPotionEvent(new CustomPotionEvent.AffectEntity(this, source, indirectSource,
                entityLiving, amplifier, health), EnumScriptType.POTION_AFFECT);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int step = nbtData.contains("Duration", 3) ? nbtData.getInt("Duration") : 10;
        boolean isReady = duration % step == 0;
        if (isReady || duration % 10 == 0) {
            CustomPotionEvent.IsReadyEvent event = new CustomPotionEvent.IsReadyEvent(this, isReady, duration, amplifier);
            EventHooks.onCustomPotionEvent(event, EnumScriptType.POTION_IS_READY);
            isReady = event.isReady;
        }
        return isReady;
    }

    @Override
    public void applyEffectTick(@Nullable LivingEntity entityLiving, int amplifier) {
        EventHooks.onCustomPotionEvent(new CustomPotionEvent.PerformEffect(this, entityLiving, amplifier), EnumScriptType.POTION_PERFORM);
    }

    @Override
    public void removeAttributeModifiers(@Nonnull LivingEntity entityLivingBaseIn, @Nonnull AttributeMap attributeMapIn, int amplifier) {
        super.removeAttributeModifiers(entityLivingBaseIn, attributeMapIn, amplifier);
        EventHooks.onCustomPotionEvent(new CustomPotionEvent.EndEffect(this, entityLivingBaseIn, amplifier), EnumScriptType.POTION_END);
    }

    @Override
    public String getCustomName() { return nbtData.getString("RegistryName"); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() {
        if (nbtData.contains("ItemType", 1)) { return nbtData.getByte("ItemType"); }
        return 7;
    }

    @Override
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

}
