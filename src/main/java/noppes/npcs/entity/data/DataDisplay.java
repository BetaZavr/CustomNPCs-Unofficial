package noppes.npcs.entity.data;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.awt.*;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import nikedemos.markovnames.generators.MarkovGenerator;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.INPCDisplay;
import noppes.npcs.client.parts.ModelData;
import noppes.npcs.client.parts.ModelPartConfig;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.controllers.VisibilityController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.world.entity.IEntityMixin;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.util.ValueUtil;

public class DataDisplay implements INPCDisplay {

   EntityNPCInterface npc;
   public byte skinType = 0;
   public GameProfile playerProfile;

   protected Availability availability = new Availability();
   protected String name = "Noppes";
   protected String title = "";
   protected String url = "";
   protected String texture = CustomNpcs.MODID + ":textures/entity/humanmale/steve.png";
   protected String cloakTexture = "";
   protected String glowTexture = "";
   protected boolean disableLivingAnimation = false;
   protected int markovGeneratorId = 8;
   protected int markovGender = 0;
   protected int visible = 0;
   protected int showName = 0;
   protected int skinColor = new Color(0xFFFFFF).getRGB();
   protected byte hitboxState = 0;
   protected byte showBossBar = 0;
   protected BossBarColor bossColor;

   // New from Unofficial (GoodBird)
   protected boolean overlayGlowing = true;
   protected float modelSize = 5.0F;
   private int[] lineColors = new int[]{ 0xFF8D3800, 0xFFFEA53B, 0xFFAE5301 };

   // New from Unofficial (BetaZavr)
   protected float shadowSize = 1.0f;
   protected float width = 0.0f;
   protected float height = 0.0f;

   public DataDisplay(EntityNPCInterface npcIn) {
      bossColor = BossBarColor.PINK;
      npc = npcIn;
      if (!npc.isClientSide()) {
         if (npc.getRandom().nextInt(10) == 0) {
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

   public String getRandomName() { return MarkovGenerator.fetch(markovGeneratorId, markovGender); }

   public CompoundTag save(CompoundTag compound) {
      compound.putString("Name", name);
      compound.putInt("MarkovGeneratorId", markovGeneratorId);
      compound.putInt("MarkovGender", markovGender);
      compound.putString("Title", title);
      compound.putString("SkinUrl", url);
      compound.putString("Texture", texture);
      compound.putString("CloakTexture", cloakTexture);
      compound.putString("GlowTexture", glowTexture);
      compound.putBoolean("OverlayGlowing", overlayGlowing);
      compound.putByte("UsingSkinUrl", skinType);
      if (playerProfile != null) {
         CompoundTag nbt = new CompoundTag();
         NbtUtils.writeGameProfile(nbt, playerProfile);
         compound.put("SkinUsername", nbt);
      }
      compound.putInt("ShowName", showName);
      compound.putInt("SkinColor", skinColor);
      compound.putInt("NpcVisible", visible);
      compound.put("VisibleAvailability", availability.save(new CompoundTag()));
      compound.putBoolean("NoLivingAnimation", disableLivingAnimation);
      compound.putByte("IsStatue", hitboxState);
      compound.putByte("BossBar", showBossBar);
      compound.putInt("BossColor", bossColor.ordinal());

      // New from Unofficial (GoodBird)
      compound.putFloat("Size", modelSize);
      compound.putIntArray("LineColors", lineColors);

      // New from Unofficial (BetaZavr)
      compound.putFloat("ShadowSize", shadowSize);
      compound.putFloat("HitBoxWidth", width);
      compound.putFloat("HitBoxHeight", height);

      return compound;
   }

   public void load(CompoundTag compound) {
      setName(compound.getString("Name"));
      setMarkovGeneratorId(compound.getInt("MarkovGeneratorId"));
      setMarkovGender(compound.getInt("MarkovGender"));
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
      if (compound.contains("OverlayGlowing")) { overlayGlowing = compound.getBoolean("OverlayGlowing"); }
      playerProfile = null;
      if (skinType == 1) {
         if (compound.contains("SkinUsername", 10)) {
            playerProfile = NbtUtils.readGameProfile(compound.getCompound("SkinUsername"));
         } else if (compound.contains("SkinUsername", 8) && !StringUtil.isNullOrEmpty(compound.getString("SkinUsername"))) {
            playerProfile = new GameProfile(null, compound.getString("SkinUsername"));
         }
         loadProfile();
      }
      showName = compound.getInt("ShowName");
      if (compound.contains("SkinColor")) {
         skinColor = compound.getInt("SkinColor");
      }
      visible = compound.getInt("NpcVisible");
      availability.load(compound.getCompound("VisibleAvailability"));
      VisibilityController.instance.trackNpc(npc);
      disableLivingAnimation = compound.getBoolean("NoLivingAnimation");
      hitboxState = compound.getByte("IsStatue");
      setBossbar(compound.getByte("BossBar"));
      setBossColor(compound.getInt("BossColor"));
      if (prevSkinType != skinType || !texture.equals(prevTexture) || !url.equals(prevUrl) || !getSkinPlayer().equals(prevPlayer)) { npc.textureLocation = null; }
      npc.textureGlowLocation = null;
      npc.textureCloakLocation = null;
      npc.refreshDimensions();

      // New from Unofficial (GoodBird)
      if (compound.contains("Size", Tag.TAG_ANY_NUMERIC)) {
         modelSize = ValueUtil.onlyPositiveFloat(compound.getFloat("Size"), Float.MAX_VALUE);
      }
      if (compound.contains("Size", Tag.TAG_INT_ARRAY)) { lineColors = compound.getIntArray("LineColors"); }

      // New from Unofficial (BetaZavr)
      if (compound.contains("ShadowSize", 5)) { shadowSize = ValueUtil.correctFloat(compound.getFloat("ShadowSize"), 0, 1.5f); } else { shadowSize = 1.0f; }
      if (compound.contains("HitBoxWidth", 5)) { width = ValueUtil.correctFloat(compound.getFloat("HitBoxWidth"), 0.0f, 5.0f); }
      if (compound.contains("HitBoxHeight", 5)) { height = ValueUtil.correctFloat(compound.getFloat("HitBoxHeight"), 0.0f, 10.0f); }
      if (hitboxState != (byte) 1 && (width != 0.0f || height != 0.0f)) {
         boolean fixed = ((IEntityMixin) npc).getDimensions().fixed;
         ((IEntityMixin) npc).setDimensions(new EntityDimensions(width, height, fixed));
      }
   }

   public void loadProfile() {
      if (playerProfile != null && !StringUtil.isNullOrEmpty(playerProfile.getName())) {
         if (npc.getServer() == null) { SkullBlockEntity.updateGameprofile(playerProfile, (profile) -> playerProfile = profile); }
         else { playerProfile = getGameprofile(npc.getServer(), playerProfile); }
      }
   }

   private static GameProfile getGameprofile(MinecraftServer server, @Nullable GameProfile profile) {
      try {
         if (profile != null && !StringUtil.isNullOrEmpty(profile.getName()) && (!profile.isComplete() || !profile.getProperties().containsKey("textures")) && server.getProfileCache() != null) {
            GameProfile gameprofile = server.getProfileCache().get(profile.getName()).orElse(null);
            if (gameprofile == null) {
               return profile;
            } else {
               Property property = Iterables.getFirst(gameprofile.getProperties().get("textures"), null);
               if (property == null) {
                  gameprofile = server.getSessionService().fillProfileProperties(gameprofile, true);
               }
               return gameprofile;
            }
         } else {
            return profile;
         }
      } catch (Exception var4) {
         return profile;
      }
   }

   public boolean showName() {
      if (npc.isKilled()) { return false; }
      return showName == 0 || showName == 2 && npc.isAttacking();
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public void setName(String nameIn) {
      if (!name.equals(nameIn)) {
         name = nameIn;
         npc.bossInfo.setName(npc.getDisplayName());
         npc.updateClient = true;
      }
   }

   @Override
   public int getShowName() {
      return showName;
   }

   @Override
   public void setShowName(int type) {
      if (type != showName) {
         showName = ValueUtil.correctInt(type, 0, 2);
         npc.updateClient = true;
      }
   }

   public int getMarkovGender() {
      return markovGender;
   }

   public void setMarkovGender(int gender) {
      if (markovGender != gender) {
         markovGender = ValueUtil.correctInt(gender, 0, 2);
      }
   }

   public int getMarkovGeneratorId() {
      return markovGeneratorId;
   }

   public void setMarkovGeneratorId(int id) {
      if (markovGeneratorId != id) {
         markovGeneratorId = ValueUtil.correctInt(id, 0, 9);
      }
   }

   @Override
   public String getTitle() { return title; }

   @Override
   public void setTitle(String titleIn) {
      if (!title.equals(titleIn)) {
         title = titleIn;
         npc.updateClient = true;
      }
   }

   @Override
   public String getSkinUrl() { return url; }

   @Override
   public void setSkinUrl(String urlIn) {
      if (!url.equals(urlIn)) {
         url = urlIn;
         if (url.isEmpty()) { skinType = 0; }
         else { skinType = 2; }
         npc.updateClient = true;
      }
   }

   @Override
   public String getSkinPlayer() {
      return playerProfile == null ? "" : playerProfile.getName();
   }

   @Override
   public void setSkinPlayer(String name) {
      if (name != null && !name.isEmpty()) {
         playerProfile = new GameProfile(null, name);
         skinType = 1;
      } else {
         playerProfile = null;
         skinType = 0;
      }
      npc.updateClient = true;
   }

   @Override
   public String getSkinTexture() { return NoppesStringUtils.cleanResource(texture); }

   @Override
   public void setSkinTexture(String textureIn) {
      if (textureIn != null && !texture.equals(textureIn)) {
         texture = NoppesStringUtils.cleanResource(textureIn);
         npc.textureLocation = null;
         skinType = 0;
         npc.updateClient = true;
      }
   }

   @Override
   public String getOverlayTexture() { return NoppesStringUtils.cleanResource(glowTexture); }

   @Override
   public void setOverlayTexture(String textureIn) {
      if (!glowTexture.equals(textureIn)) {
         glowTexture = NoppesStringUtils.cleanResource(textureIn);
         npc.textureGlowLocation = null;
         npc.updateClient = true;
      }
   }

   @Override
   public String getCapeTexture() {
      return NoppesStringUtils.cleanResource(cloakTexture);
   }

   @Override
   public void setCapeTexture(String textureIn) {
      if (!cloakTexture.equals(textureIn)) {
         cloakTexture = NoppesStringUtils.cleanResource(textureIn);
         npc.textureCloakLocation = null;
         npc.updateClient = true;
      }
   }

   @Override
   public boolean getHasLivingAnimation() {
      return !disableLivingAnimation;
   }

   @Override
   public void setHasLivingAnimation(boolean enabled) {
      disableLivingAnimation = !enabled;
      npc.updateClient = true;
   }

   @Override
   public int getBossbar() { return showBossBar; }

   @Override
   public void setBossbar(int type) {
      if (type != showBossBar) {
         showBossBar = (byte)ValueUtil.correctInt(type, 0, 2);
         npc.bossInfo.setVisible(showBossBar == 1);
         npc.updateClient = true;
      }
   }

   @Override
   public int getBossColor() { return bossColor.ordinal(); }

   @Override
   public void setBossColor(int color) {
      if (color < 0 || color >= BossBarColor.values().length) { throw new CustomNPCsException("Invalid Boss Color: " + color); }
      bossColor = BossBarColor.values()[color];
      npc.bossInfo.setColor(bossColor);
   }

   @Override
   public int getVisible() { return visible; }

   @Override
   public void setVisible(int type) {
      if (type != visible) {
         visible = ValueUtil.correctInt(type, 0, 2);
         npc.updateClient = true;
      }
   }

   @Override
   public float getSize() { return modelSize; }

   @Override
   public void setSize(float size) {
      if (modelSize != size) {
         modelSize = ValueUtil.onlyPositiveFloat(size, Float.MAX_VALUE);
         npc.updateClient = true;
      }
   }

   @Override
   public void setModelScale(int part, float x, float y, float z) {
      ModelData modeldata = ((EntityCustomNpc)npc).modelData;
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
      } else {
         model.setScale(x, y, z);
         npc.updateClient = true;
      }
   }

   @Override
   public float[] getModelScale(int part) {
      ModelData modeldata = ((EntityCustomNpc)npc).modelData;
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
      } else {
         return new float[]{model.scaleX, model.scaleY, model.scaleZ};
      }
   }

   @Override
   public int getTint() {
      return skinColor;
   }

   @Override
   public void setTint(int color) {
      if (color != skinColor) {
         skinColor = color;
         npc.updateClient = true;
      }
   }

   @Override
   public void setModel(String id) {
      ModelData modeldata = ((EntityCustomNpc)npc).modelData;
      if (id == null) {
         if (modeldata.getEntityName() == null) {
            return;
         }
         modeldata.setEntity(null);
      } else {
         ResourceLocation resource = ResourceLocation.tryParse(id);
         EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(resource);
         if (type == null) {
            throw new CustomNPCsException("Unknown entity id: " + id);
         }
         modeldata.setEntity(resource);
      }
      npc.updateClient = true;

   }

   @Override
   public String getModel() {
      if (!(npc instanceof EntityCustomNpc)) { return null; }
      ModelData modeldata = ((EntityCustomNpc) npc).modelData;
      return modeldata.getEntityName() == null ? null : modeldata.getEntityName().toString();
   }

   @Override
   public byte getHitboxState() { return hitboxState; }

   @Override
   public void setHitboxState(byte state) {
      if (hitboxState != state) {
         hitboxState = state;
         npc.updateClient = true;
      }
   }

   @Override
   public boolean isVisibleTo(IPlayer<?> playerIn) { return isVisibleTo(playerIn.getMCEntity()); }

   public boolean isVisibleTo(Player player) {
      if (visible == 1) { return !availability.isAvailable(player); }
      return availability.isAvailable(player);
   }

   // New from Unofficial (GoodBird)
   @Override
   public boolean isOverlayGlowing() { return overlayGlowing; }

   @Override
   public void setOverlayGlowing(boolean glowing) { overlayGlowing = glowing; }

   @Override
   public int[] getLineColors() { return lineColors; }

   @Override
   public void setLineColors(int color1, int color2, int color3) { lineColors = new int[]{color1, color2, color3}; }

   // New from Unofficial (BetaZavr)
   @Override
   public float[] getDimensions() { return new float[] { width, height }; }

   @Override
   public void setDimensions(float widthIn, float heightIn) {
      if (widthIn < 0 || heightIn < 0) { throw new CustomNPCsException("Width or height must be greater than 0. Now width: " + widthIn + "; height: " + heightIn); }
      if (widthIn > 7.5f || heightIn > 15.0f) { throw new CustomNPCsException("Width must be less than 7.5 or height must be less than 15. Now width: " + widthIn + "; height: " + heightIn); }
      width = widthIn;
      height = heightIn;
      if (hitboxState != (byte) 1 && (width != 0.0f || height != 0.0f)) {
         boolean fixed = ((IEntityMixin) npc).getDimensions().fixed;
         ((IEntityMixin) npc).setDimensions(new EntityDimensions(width, height, fixed));
      }
   }

   public float getShadowSize() { return shadowSize; }

   @Override
   public int getShadowType() {
      if (shadowSize < 0.5f) { return 0; }
      if (shadowSize < 1.0f) { return 1; }
      if (shadowSize < 1.5f) { return 2; }
      return 3;
   }

   @Override
   public void setShadowType(int type) {
      if (type < 0) { type *= -1; }
      shadowSize = switch (type % 4) {
         case 0 -> 0.0f;
         case 1 -> 0.5f;
         case 2 -> 1.0f;
         default -> 1.5f;
      };
   }

}
