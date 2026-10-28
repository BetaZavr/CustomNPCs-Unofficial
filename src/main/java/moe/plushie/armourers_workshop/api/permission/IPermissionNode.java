package moe.plushie.armourers_workshop.api.permission;

import moe.plushie.armourers_workshop.api.common.IGameProfile;
import moe.plushie.armourers_workshop.api.core.IResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("unused")
public interface IPermissionNode {

    Component name();

    Component description();

    IResourceKey registryName();

    boolean resolve(IGameProfile var1, IPermissionContext var2);

    boolean resolve(Player var1, IPermissionContext var2);

}

