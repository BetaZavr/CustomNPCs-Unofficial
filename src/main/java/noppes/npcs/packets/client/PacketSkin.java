package noppes.npcs.packets.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.SkinUtil;
import noppes.npcs.client.util.CustomTexturesPayload;
import noppes.npcs.controllers.PlayerSkinController;
import noppes.npcs.controllers.data.SkinData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketSkin;
import noppes.npcs.shared.common.PacketBasic;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class PacketSkin extends PacketBasic {

    protected static final Gson gson = new Gson();
    protected static int channelId;
    private final int type;
    private final CompoundTag data;

    public PacketSkin(int typeIn, CompoundTag dataIn) {
        type = typeIn;
        data = dataIn;
    }

    public static void encode(PacketSkin msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.type);
        buf.writeNbt(msg.data);
    }

    public static PacketSkin decode(FriendlyByteBuf buf) { return new PacketSkin(buf.readInt(), buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        switch (type) {
            case 0: {
                GameProfile profile = player.getGameProfile();
                Property property = Iterables.getFirst(profile.getProperties().get("textures"), null);
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = ImmutableMap.of();
                if (property != null) {
                    try {
                        String json = new String(Base64.getDecoder().decode(property.getValue()), StandardCharsets.UTF_8);
                        CustomTexturesPayload ctp = (CustomTexturesPayload) gson.fromJson(json, MinecraftTexturesPayload.class);
                        map = ctp.textures;
                    }
                    catch (JsonParseException ignored) {}
                }
                CompoundTag compound = new CompoundTag();
                compound.putString("Player", player.getName().getString());
                compound.putUUID("UUID", player.getUUID());
                ListTag list = new ListTag();
                for (MinecraftProfileTexture.Type t : MinecraftProfileTexture.Type.values()) {
                    ResourceLocation location;
                    if (map.containsKey(t)) {
                        MinecraftProfileTexture mpt = map.get(t);
                        String sha1 = Hashing.sha1().hashUnencodedChars(mpt.getHash()).toString();
                        location = switch (t) {
                            case SKIN -> new ResourceLocation("skins/" + sha1);
                            case CAPE -> new ResourceLocation("capes/" + sha1);
                            case ELYTRA -> new ResourceLocation("elytra/" + sha1);
                        };
                        SkinData skinData = SkinData.create(t, location);
                        skinData.setIsDefault();
                        list.add(skinData.save());
                    }
                    else if (t == MinecraftProfileTexture.Type.SKIN) {
                        location = DefaultPlayerSkin.getDefaultSkin(UUIDUtil.getOrCreatePlayerUUID(profile));
                        SkinData skinData = SkinData.create(t, location);
                        skinData.setIsDefault();
                        list.add(skinData.save());
                    }
                }
                compound.put("Textures", list);
                Packets.sendServer(new SPacketSkin(compound));
                break;
            } // get skin
            case 1: {
                SkinUtil.resetSkin(PlayerSkinController.getInstance().loadPlayerSkin(data));
                break;
            } // set
        }
        CustomNpcs.debugData.end("Packets");
    }

}
