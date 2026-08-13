package noppes.npcs.entity.data;

import java.nio.charset.StandardCharsets;
import java.util.*;

import noppes.npcs.client.model.part.ModelPartConfig;
import noppes.npcs.controllers.VisibilityController;
import noppes.npcs.shared.common.util.LogWriter;
import org.apache.commons.codec.binary.Base64;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.world.BossInfo;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.INPCDisplay;
import noppes.npcs.client.model.part.ModelData;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.ValueUtil;

public class DataDisplay implements INPCDisplay {

	EntityNPCInterface npc;
	public GameProfile playerProfile;
	public byte skinType = (byte) 0;

	private final Availability availability = new Availability();
	private BossInfo.Color bossColor = BossInfo.Color.PINK;
	private boolean disableLivingAnimation = false;
	private boolean isNormalModel = false;
	private byte hitboxState = 0;
	private byte showBossBar = 0;
	private int markovGender = 0;
	private int markovGeneratorId = new Random().nextInt(CustomNpcs.MARKOV_GENERATOR.length - 1);
	private int modelSize = 5;
	private int showName = 0;
	private int skinColor = 0xFFFFFF;
	private int visible = 0;
	private String cloakTexture = "";
	private String title = "";
	private String url = "";
	private String glowTexture = "";
	private String name;
	private String texture = CustomNpcs.MODID + ":textures/entity/humanmale/steve.png";
	public float shadowSize = 1.0f;
	public float width = 0.6f;
	public float height = 1.9f;

	// New from Unofficial (GoodBird)
	protected boolean overlayGlowing = true;

	public DataDisplay(EntityNPCInterface npcIn) {
		npc = npcIn;
		name = getRandomName();
		if (npc.isServerWorld()) {
			if ((new Random()).nextInt(10) == 0) {
				DataPeople p = DataPeople.get();
				name = p.name;
				title = p.title;
				if (!p.skin.isEmpty()) { texture = p.skin; }
			}
			else {
				markovGeneratorId = (new Random()).nextInt(10);
				name = getRandomName();
			}
		}
	}

	public Availability getAvailability() { return availability; }

	@Override
	public int getBossbar() {
		return showBossBar;
	}

	@Override
	public int getBossColor() {
		return bossColor.ordinal();
	}

	@Override
	public String getCapeTexture() {
		return cloakTexture;
	}

	@Override
	public int getHitboxState() { return hitboxState; }

	@Override
	public boolean isNormalModel() {
		return !isNormalModel;
	}
	
	@Override
	public boolean getHasLivingAnimation() {
		return !disableLivingAnimation;
	}

	public int getMarkovGender() {
		return markovGender;
	}

	public int getMarkovGeneratorId() {
		return markovGeneratorId;
	}

	@Override
	public String getModel() {
		if (!(npc instanceof EntityCustomNpc)) {
			return null;
		}
		ModelData modeldata = ((EntityCustomNpc) npc).modelData;
		if (modeldata.entityClass == null) {
			return null;
		}
		String name = modeldata.entityClass.getCanonicalName();
		for (EntityEntry ent : ForgeRegistries.ENTITIES.getValuesCollection()) {
			Class<? extends Entity> c = ent.getEntityClass();
			if (c.getCanonicalName().equals(name) && EntityLivingBase.class.isAssignableFrom(c)) {
				return Objects.requireNonNull(ent.getRegistryName()).toString();
			}
		}
		return null;
	}

	@Override
	public float[] getModelScale(int part) {
		if (!(npc instanceof EntityCustomNpc)) {
			return new float[] { 1.0f, 1.0f, 1.0f };
		}
		ModelData modeldata = ((EntityCustomNpc) npc).modelData;
		ModelPartConfig model = null;
		if (part == 0) {
			model = modeldata.getPartConfig(EnumParts.HEAD);
		} else if (part == 1) {
			model = modeldata.getPartConfig(EnumParts.BODY);
		} else if (part == 2) {
			model = modeldata.getPartConfig(EnumParts.ARM_LEFT);
		} else if (part == 3) {
			model = modeldata.getPartConfig(EnumParts.ARM_RIGHT);
		} else if (part == 4) {
			model = modeldata.getPartConfig(EnumParts.LEG_LEFT);
		} else if (part == 5) {
			model = modeldata.getPartConfig(EnumParts.LEG_RIGHT);
		}
		if (model == null) {
			throw new CustomNPCsException("Unknown part: " + part);
		}
		return new float[] { model.scaleX, model.scaleY, model.scaleZ };
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOverlayTexture() {
		return glowTexture;
	}

	public String getRandomName() {
		return CustomNpcs.MARKOV_GENERATOR[markovGeneratorId].fetch(markovGender);
	}

	@Override
	public int getShadowType() {
		if (shadowSize < 0.5f) {
			return 0;
		}
		if (shadowSize < 1.0f) {
			return 1;
		}
		if (shadowSize < 1.5f) {
			return 2;
		}
		return 3;
	}

	@Override
	public int getShowName() {
		return showName;
	}

	@Override
	public int getSize() {
		return modelSize;
	}

	@Override
	public String getSkinPlayer() {
		return (playerProfile == null) ? "" : playerProfile.getName();
	}

	@Override
	public String getSkinTexture() {
		return texture;
	}

	@Override
	public String getSkinUrl() {
		return url;
	}

	@Override
	public int getTint() {
		return skinColor;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public int getVisible() {
		return visible;
	}

	public boolean hasVisibleOptions() { return CustomNpcs.EnableInvisibleNpcs && availability.hasOptions(); }

	public boolean isVisibleTo(EntityPlayerMP player) {
		if (visible == 1) { return !availability.isAvailable(player); }
		return true;
	}

	@Override
	public boolean isVisibleTo(IPlayer<?> player) {
		return npc.isInvisibleToPlayer(player.getMCEntity());
	}

	public void loadProfile() {
		if (playerProfile != null && !StringUtils.isNullOrEmpty(playerProfile.getName()) && npc.getServer() != null && (!playerProfile.isComplete() || !playerProfile.getProperties().containsKey("textures"))) {
			GameProfile gameprofile = npc.getServer().getPlayerProfileCache().getGameProfileForUsername(playerProfile.getName());
			if (gameprofile != null) {
				Property property = Iterables.getFirst(gameprofile.getProperties().get("textures"), null);
				if (property == null) {
					gameprofile = npc.getServer().getMinecraftSessionService().fillProfileProperties(gameprofile,
							true);
				}
				playerProfile = gameprofile;
			}
		}
	}

	public void load(NBTTagCompound compound) {
		setName(compound.getString("Name"));
		setMarkovGeneratorId(compound.getInteger("MarkovGeneratorId"));
		setMarkovGender(compound.getInteger("MarkovGender"));
		title = compound.getString("Title");
		int prevSkinType = skinType;
		String prevTexture = texture;
		String prevUrl = url;
		String prevPlayer = getSkinPlayer();
		url = compound.getString("SkinUrl");
		skinType = compound.getByte("UsingSkinUrl");
		texture = compound.getString("Texture");
		cloakTexture = compound.getString("CloakTexture");
		glowTexture = compound.getString("GlowTexture");
		if (compound.hasKey("OverlayGlowing")) { overlayGlowing = compound.getBoolean("OverlayGlowing"); }
		playerProfile = null;
		if (!url.isEmpty() && !url.startsWith("http")) {
			try {
				final String json = new String(Base64.decodeBase64(url), StandardCharsets.UTF_8);
				Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
				MinecraftTexturesPayload mtp = gson.fromJson(json, MinecraftTexturesPayload.class);
				MinecraftProfileTexture mpt = mtp.getTextures().get(MinecraftProfileTexture.Type.SKIN);
				if (!mpt.getUrl().isEmpty()) { url = mpt.getUrl(); }
			}
			catch (Exception e) { LogWriter.error(e); }
		}
		if (skinType == 1) {
			if (compound.hasKey("SkinUsername", 10)) {
				playerProfile = NBTUtil.readGameProfileFromNBT(compound.getCompoundTag("SkinUsername"));
			} else if (compound.hasKey("SkinUsername", 8) && !StringUtils.isNullOrEmpty(compound.getString("SkinUsername"))) {
				playerProfile = new GameProfile(null, compound.getString("SkinUsername"));
			}
			loadProfile();
		}
		modelSize = ValueUtil.correctInt(compound.getInteger("Size"), 1, 30);
		showName = compound.getInteger("ShowName");
		if (compound.hasKey("SkinColor")) {
			skinColor = compound.getInteger("SkinColor");
		}
		visible = compound.getInteger("NpcVisible");
		availability.load(compound.getCompoundTag("VisibleAvailability"));
		disableLivingAnimation = compound.getBoolean("NoLivingAnimation");
		hitboxState = compound.getByte("IsStatue");
		isNormalModel = compound.getBoolean("HasJoints");
		
		setBossbar(compound.getByte("BossBar"));
		setBossColor(compound.getInteger("BossColor"));
		if (prevSkinType != skinType || !texture.equals(prevTexture) || !url.equals(prevUrl) || !getSkinPlayer().equals(prevPlayer)) {
			npc.textureLocation = null;
		}
		npc.textureGlowLocation = null;
		npc.textureCloakLocation = null;
		npc.updateHitbox();
		if (compound.hasKey("ShadowSize", 5)) {
			shadowSize = ValueUtil.correctFloat(compound.getFloat("ShadowSize"), 0, 1.5f);
		} else {
			shadowSize = 1.0f;
		}
		if (compound.hasKey("HitBoxWidth", 5)) { width = ValueUtil.correctFloat(compound.getFloat("HitBoxWidth"), 0.0f, 5.0f); }
		if (compound.hasKey("HitBoxHeight", 5)) { height = ValueUtil.correctFloat(compound.getFloat("HitBoxHeight"), 0.0f, 10.0f); }
        if (hitboxState != (byte) 1 && (width != 0.0f || height != 0.0f)) {
        	if (npc.baseHeight > 0.0f && height > 0.0f) { npc.baseEyeHeight = npc.baseEyeHeight / npc.baseHeight * height; }
        	npc.baseWidth = width;
        	npc.baseHeight = height;
        	npc.updateHitbox();
		}
		VisibilityController.instance.trackNpc(npc);
	}

	@Override
	public void setBossbar(int type) {
		if (type == showBossBar) { return; }
		showBossBar = (byte) ValueUtil.correctInt(type, 0, 2);
		npc.bossInfo.setVisible(showBossBar == 1);
		npc.updateClient = true;
	}

	@Override
	public void setBossColor(int color) {
		if (color < 0 || color >= BossInfo.Color.values().length) {
			throw new CustomNPCsException("Invalid Boss Color: " + color);
		}
		bossColor = BossInfo.Color.values()[color];
		npc.bossInfo.setColor(bossColor);
	}

	@Override
	public void setCapeTexture(String texture) {
		if (cloakTexture.equals(texture)) { return; }
		cloakTexture = texture.toLowerCase();
		npc.textureCloakLocation = null;
		npc.updateClient = true;
	}

	@Override
	public void setHitboxState(int state) {
		state = ValueUtil.correctInt(state, 0,2);
		if (hitboxState == state) { return; }
		hitboxState = (byte) state;
		npc.updateClient = true;
	}

	@Override
	public void setNormalModel(boolean bo) {
		if (isNormalModel == bo) { return; }
		isNormalModel = bo;
		npc.updateClient = true;
	}

	@Override
	public void setHasLivingAnimation(boolean enabled) {
		disableLivingAnimation = !enabled;
		npc.updateClient = true;
	}

	public void setMarkovGender(int gender) {
		if (markovGender == gender) {
			return;
		}
		markovGender = ValueUtil.correctInt(gender, 0, 2);
	}

	public void setMarkovGeneratorId(int id) {
		if (markovGeneratorId == id) {
			return;
		}
		markovGeneratorId = ValueUtil.correctInt(id, 0, CustomNpcs.MARKOV_GENERATOR.length - 1);
	}

	@Override
	public void setModel(String id) {
		if (!(npc instanceof EntityCustomNpc)) {
			return;
		}
		ModelData modeldata = ((EntityCustomNpc) npc).modelData;
		if (id == null) {
			if (modeldata.entityClass == null) {
				return;
			}
			modeldata.entityClass = null;
        } else {
			ResourceLocation resource = new ResourceLocation(id);
			Entity entity = EntityList.createEntityByIDFromName(resource, npc.world);
			if (entity == null) {
				throw new CustomNPCsException("Failed to create an entity from given id: " + id);
			}
			modeldata.setEntityName(entity.getClass().getCanonicalName());
        }
        npc.updateClient = true;
    }

	@Override
	public void setModelScale(int part, float x, float y, float z) {
		if (!(npc instanceof EntityCustomNpc)) {
			return;
		}
		ModelData modeldata = ((EntityCustomNpc) npc).modelData;
		ModelPartConfig model = null;
		if (part == 0) {
			model = modeldata.getPartConfig(EnumParts.HEAD);
		} else if (part == 1) {
			model = modeldata.getPartConfig(EnumParts.BODY);
		} else if (part == 2) {
			model = modeldata.getPartConfig(EnumParts.ARM_LEFT);
		} else if (part == 3) {
			model = modeldata.getPartConfig(EnumParts.ARM_RIGHT);
		} else if (part == 4) {
			model = modeldata.getPartConfig(EnumParts.LEG_LEFT);
		} else if (part == 5) {
			model = modeldata.getPartConfig(EnumParts.LEG_RIGHT);
		}
		if (model == null) {
			throw new CustomNPCsException("Unknown part: " + part);
		}
		model.setScale(x, y, z);
		npc.updateClient = true;
	}

	@Override
	public void setName(String newName) {
		if (name.equals(newName)) {
			return;
		}
		name = newName;
		npc.bossInfo.setName(npc.getDisplayName());
		npc.updateClient = true;
	}

	@Override
	public void setOverlayTexture(String texture) {
		if (glowTexture.equals(texture)) {
			return;
		}
		glowTexture = texture;
		npc.textureGlowLocation = null;
		npc.updateClient = true;
	}

	@Override
	public void setShadowType(int type) {
		if (type < 0) {
			type *= -1;
		}
		switch (type % 4) {
			case 0:
				shadowSize = 0.0f;
				break;
			case 1:
				shadowSize = 0.5f;
				break;
			case 2:
				shadowSize = 1.0f;
				break;
			default:
				shadowSize = 1.5f;
				break;
		}
	}

	@Override
	public void setShowName(int type) {
		if (type == showName) {
			return;
		}
		showName = ValueUtil.correctInt(type, 0, 2);
		npc.updateClient = true;
	}

	@Override
	public void setSize(int size) {
		if (modelSize == size) {
			return;
		}
		modelSize = ValueUtil.correctInt(size, 1, 30);
		npc.updateClient = true;
	}

	@Override
	public void setSkinPlayer(String name) {
		if (name == null || name.isEmpty()) {
			playerProfile = null;
			skinType = 0;
		} else {
			playerProfile = new GameProfile(null, name);
			skinType = 1;
		}
		npc.updateClient = true;
	}

	@Override
	public void setSkinTexture(String newTexture) {
		if (newTexture == null || texture.equals(newTexture)) {
			return;
		}
		texture = newTexture.toLowerCase();
		npc.textureLocation = null;
		skinType = 0;
		npc.updateClient = true;
	}

	@Override
	public void setSkinUrl(String newURL) {
		if (url.equals(newURL)) { return; }
		url = newURL;
		if (url.isEmpty()) {
			skinType = 0;
		} else {
			skinType = 2;
		}
		npc.updateClient = true;
	}

	@Override
	public void setTint(int color) {
		if (color == skinColor) {
			return;
		}
		skinColor = color;
		npc.updateClient = true;
	}

	@Override
	public void setTitle(String newTitle) {
		if (title.equals(newTitle)) {
			return;
		}
		title = newTitle;
		npc.updateClient = true;
	}

	@Override
	public void setVisible(int type) {
		if (type == visible) { return; }
		visible = ValueUtil.correctInt(type, 0, 2);
		npc.updateClient = true;
	}

	public boolean showName() {
		return !npc.isKilled() && (showName == 0 || (showName == 2 && npc.isAttacking()));
	}

	public NBTTagCompound save(NBTTagCompound compound) {
		compound.setString("Name", name);
		compound.setInteger("MarkovGeneratorId", markovGeneratorId);
		compound.setInteger("MarkovGender", markovGender);
		compound.setString("Title", title);
		compound.setString("SkinUrl", url);
		compound.setString("Texture", texture);
		compound.setString("CloakTexture", cloakTexture);
		compound.setString("GlowTexture", glowTexture);
		compound.setBoolean("OverlayGlowing", overlayGlowing);
		compound.setByte("UsingSkinUrl", skinType);
		if (playerProfile != null) {
			NBTTagCompound displayNbt2 = new NBTTagCompound();
			NBTUtil.writeGameProfile(displayNbt2, playerProfile);
			compound.setTag("SkinUsername", displayNbt2);
		}
		compound.setInteger("Size", modelSize);
		compound.setInteger("ShowName", showName);
		compound.setInteger("SkinColor", skinColor);
		compound.setInteger("NpcVisible", visible);
		compound.setTag("VisibleAvailability", availability.save(new NBTTagCompound()));
		compound.setBoolean("NoLivingAnimation", disableLivingAnimation);
		compound.setByte("IsStatue", hitboxState);
		compound.setBoolean("HasJoints", isNormalModel);
		compound.setByte("BossBar", showBossBar);
		compound.setInteger("BossColor", bossColor.ordinal());
		compound.setBoolean("EnableInvisibleNpcs", CustomNpcs.EnableInvisibleNpcs);
		compound.setFloat("ShadowSize", shadowSize);
		compound.setFloat("HitBoxWidth", width);
		compound.setFloat("HitBoxHeight", height);
		return compound;
	}

	// New from Unofficial (GoodBird)
	public boolean isOverlayGlowing() { return overlayGlowing; }

	public void setOverlayGlowing(boolean glowing) { overlayGlowing = glowing; }

}
