/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.sounds.SoundEvent
 */
package moe.plushie.armourers_workshop.api.registry;

import moe.plushie.armourers_workshop.api.core.IRegistryHolder;
import moe.plushie.armourers_workshop.api.registry.IEntryBuilder;
import net.minecraft.sounds.SoundEvent;

public interface ISoundEventBuilder<T extends SoundEvent>
extends IEntryBuilder<IRegistryHolder<T>> {
}

