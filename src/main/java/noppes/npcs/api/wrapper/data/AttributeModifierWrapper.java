package noppes.npcs.api.wrapper.data;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import noppes.npcs.api.entity.data.IAttributeModifier;
import noppes.npcs.api.entity.data.INpcAttribute;
import noppes.npcs.mixin.world.entity.ai.attributes.IAttributeModifierMixin;

import java.util.UUID;

public class AttributeModifierWrapper implements IAttributeModifier {

    private final INpcAttribute parent;
    private final AttributeModifier modifer;

    public AttributeModifierWrapper(INpcAttribute attribute, AttributeModifier modiferIn) {
        modifer = modiferIn;
        parent = attribute;
    }

    @Override
    public double getAmount() { return modifer.getAmount(); }

    @Override
    public String getId() { return modifer.getId().toString(); }

    @Override
    public AttributeModifier getMCModifier() { return modifer; }

    @Override
    public String getName() { return modifer.getName(); }

    @Override
    public int getOperation() { return modifer.getOperation().ordinal(); }

    @Override
    public IAttributeModifier setAmount(double amount) {
        if (parent == null) {
            ((IAttributeModifierMixin) modifer).setAmount(amount);
            return this;
        }
        AttributeModifier newModifier = new AttributeModifier(modifer.getId(), modifer.getName(), amount, modifer.getOperation());
        parent.getMCAttribute().removeModifier(modifer);
        parent.getMCAttribute().addTransientModifier(newModifier);
        return parent.getModifier(newModifier.getName());
    }

    @Override
    public IAttributeModifier setName(String name) {
        if (parent == null) {
            try { ((IAttributeModifierMixin) modifer).setId(UUID.fromString(name)); } catch (Exception ignored) {}
            return this;
        }
        AttributeModifier newModifier = new AttributeModifier(modifer.getId(), name, modifer.getAmount(), modifer.getOperation());
        parent.getMCAttribute().removeModifier(modifer);
        parent.getMCAttribute().addTransientModifier(newModifier);
        return parent.getModifier(newModifier.getName());
    }

    @Override
    public void setOperation(int operation) {
        ((IAttributeModifierMixin) modifer).setOperation(AttributeModifier.Operation.values()[operation % AttributeModifier.Operation.values().length]);
    }

    @Override
    public String toString() {
        return modifer.toString().replace("AttributeModifier", "AttributeModifierWrapper");
    }

}
