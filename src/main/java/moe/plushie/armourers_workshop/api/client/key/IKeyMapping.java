package moe.plushie.armourers_workshop.api.client.key;

import java.util.Collection;

import net.minecraft.network.chat.Component;

@SuppressWarnings("unused")
public interface IKeyMapping {

    boolean consumeClick();

    Component name();

    IKeyCategory category();

    Collection<? extends IKeyModifier> modifiers();

}

