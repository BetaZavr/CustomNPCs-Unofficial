package noppes.npcs.api.wrapper.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import noppes.npcs.api.entity.data.IAttributeModifier;
import noppes.npcs.api.entity.data.INpcAttribute;
import noppes.npcs.mixin.world.entity.ai.attributes.IRangedAttributeMixin;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.ValueUtil;

public class AttributeWrapper implements INpcAttribute {

    private final AttributeInstance attribute;
    private boolean custom;

    public AttributeWrapper(LivingEntity entity, String name, double baseValue, double minValue, double maxValue) {
        minValue = ValueUtil.min(minValue, maxValue);
        maxValue = ValueUtil.max(minValue, maxValue);
        RangedAttribute rangedAttribute = new RangedAttribute(name, ValueUtil.correctDouble(baseValue, minValue, maxValue), minValue, maxValue);
        attribute = entity.getAttributes().getInstance(rangedAttribute);
        custom = name.equals("generic.maxHealth") || name.equals("generic.knockbackResistance")
                || name.equals("generic.movementSpeed") || (name.equals("generic.armor")
                || name.equals("generic.armorToughness")) || (name.equals("generic.attackDamage")
                || name.equals("generic.attackSpeed")) || (name.equals("generic.luck")
                || name.equals("generic.reachDistance")) || (name.equals("forge.swimSpeed")
                || name.equals("zombie.spawn_reinforcements")) || name.equals("horse.jump_strength");
    }

    public AttributeWrapper(AttributeInstance mcAttribute) {
        attribute = mcAttribute;
        String name = getName();
        custom = name.equals("generic.maxHealth") || name.equals("generic.knockbackResistance")
                || name.equals("generic.movementSpeed") || (name.equals("generic.armor")
                || name.equals("generic.armorToughness")) || (name.equals("generic.attackDamage")
                || name.equals("generic.attackSpeed")) || (name.equals("generic.luck")
                || name.equals("generic.reachDistance")) || (name.equals("forge.swimSpeed")
                || name.equals("zombie.spawn_reinforcements")) || name.equals("horse.jump_strength");
    }

    @Override
    public IAttributeModifier addModifier(IAttributeModifier modifier) {
        if (hasModifier(modifier)) { return null; }
        attribute.addTransientModifier(modifier.getMCModifier());
        return getModifier(modifier.getId());
    }

    @Override
    public IAttributeModifier addModifier(String modifierName, double amount, int operation) {
        if (modifierName == null || modifierName.isEmpty() || hasModifier(modifierName)) { return null; }
        if (operation < 0) { operation *= -1; }
        AttributeModifier modifier = new AttributeModifier(modifierName, amount, AttributeModifier.Operation.values()[operation % AttributeModifier.Operation.values().length]);
        attribute.addTransientModifier(modifier);
        return getModifier(modifierName);
    }

    @Override
    public double getBaseValue() { return attribute.getBaseValue(); }

    @Override
    public double getMaxValue() {
        if (attribute.getAttribute() instanceof RangedAttribute attr) { return attr.getMaxValue(); }
        return 0.0d;
    }

    @Override
    public AttributeInstance getMCAttribute() { return attribute; }

    @Override
    public Attribute getMCBaseAttribute() { return attribute == null ? null : attribute.getAttribute(); }

    @Override
    public double getMinValue() {
        if (attribute.getAttribute() instanceof RangedAttribute attr) { return attr.getMinValue(); }
        return 0.0d;
    }

    @Override
    public IAttributeModifier getModifier(String uuidOrName) {
        if (uuidOrName == null || uuidOrName.isEmpty()) { return null; }
        AttributeModifier modifier = null;
        try {
            UUID uuid = UUID.fromString(uuidOrName);
            modifier = attribute.getModifier(uuid);
        }
        catch (Exception e) { LogWriter.error(e); }
        if (modifier == null) {
            for (AttributeModifier am : attribute.getModifiers()) {
                if (am.getName().equals(uuidOrName)) {
                    modifier = am;
                    break;
                }
            }
        }
        if (modifier != null) {
            return new AttributeModifierWrapper(this, modifier);
        }
        return null;
    }

    @Override
    public IAttributeModifier[] getModifiers() {
        Collection<AttributeModifier> col = attribute.getModifiers();
        IAttributeModifier[] modifiers = new IAttributeModifier[col.size()];
        int i = 0;
        for (AttributeModifier am : col) {
            modifiers[i] = new AttributeModifierWrapper(this, am);
            i++;
        }
        return modifiers;
    }

    @Override
    public IAttributeModifier[] getModifiersByOperation(int operation) {
        if (operation < 0) { operation *= -1; }
        Collection<AttributeModifier> col = attribute.getModifiers(AttributeModifier.Operation.values()[operation % AttributeModifier.Operation.values().length]);
        IAttributeModifier[] modifiers = new IAttributeModifier[col.size()];
        int i = 0;
        for (AttributeModifier am : col) { modifiers[i++] = new AttributeModifierWrapper(this, am); }
        return modifiers;
    }

    @Override
    public String getName() { return attribute.getAttribute().getDescriptionId(); }

    @Override
    public double getTotalValue() { return attribute.getValue(); }

    @Override
    public boolean hasModifier(IAttributeModifier modifier) {
        if (modifier == null) { return false; }
        boolean has = attribute.hasModifier(modifier.getMCModifier());
        if (has) { return true; }
        for (AttributeModifier am : attribute.getModifiers()) {
            if (am.getId().equals(modifier.getMCModifier().getId())) { return true; }
        }
        return false;
    }

    @Override
    public boolean hasModifier(String uuidOrName) {
        if (uuidOrName == null || uuidOrName.isEmpty()) { return false; }
        boolean has = false;
        try {
            UUID uuid = UUID.fromString(uuidOrName);
            has = attribute.getModifier(uuid) != null;
        } catch (Exception e) { LogWriter.error(e); }
        if (has) { return true; }
        for (AttributeModifier am : attribute.getModifiers()) {
            if (am.getName().equals(uuidOrName)) { return true; }
        }
        return false;
    }

    @Override
    public boolean isCustom() { return custom; }

    @Override
    public void removeAllModifiers() {
        List<AttributeModifier> list = new ArrayList<>(attribute.getModifiers());
        for (AttributeModifier am : list) { attribute.removeModifier(am); }
    }

    @Override
    public boolean removeModifier(IAttributeModifier modifier) {
        if (modifier == null) { return false; }
        if (hasModifier(modifier)) {
            attribute.removeModifier(modifier.getMCModifier());
            boolean has = hasModifier(modifier);
            if (!has) { return true; }
            for (AttributeModifier am : attribute.getModifiers()) {
                if (am.getId().equals(modifier.getMCModifier().getId())) {
                    attribute.removeModifier(am);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeModifier(String uuid) { return removeModifier(getModifier(uuid)); }

    @Override
    public void setBaseValue(double baseValue) { attribute.setBaseValue(baseValue); }

    @Override
    public void setMaxValue(double maxValue) {
        if (attribute.getAttribute() instanceof RangedAttribute attr) {
            double minValue = attr.getMinValue();
            minValue = ValueUtil.min(minValue, maxValue);
            maxValue = ValueUtil.max(minValue, maxValue);
            ((IRangedAttributeMixin) attr).setMinValue(minValue);
            ((IRangedAttributeMixin) attr).setMaxValue(maxValue);
        }
    }

    @Override
    public void setMinValue(double minValue) {
        if (attribute.getAttribute() instanceof RangedAttribute attr) {
            double maxValue = attr.getMaxValue();
            minValue = ValueUtil.min(minValue, maxValue);
            maxValue = ValueUtil.max(minValue, maxValue);
            ((IRangedAttributeMixin) attr).setMinValue(minValue);
            ((IRangedAttributeMixin) attr).setMaxValue(maxValue);
        }
    }

    public void setCustom(boolean bo) { custom = bo; }

}
