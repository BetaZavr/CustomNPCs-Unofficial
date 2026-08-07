package noppes.npcs.client.parts;

import java.util.Random;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketEyeBlink;
import noppes.npcs.shared.common.util.ColorUtil;
import noppes.npcs.shared.common.util.NopVector2i;
import noppes.npcs.shared.common.util.NopVector3f;

public class ModelEyeData extends MpmPartData {

   public static final ResourceLocation RESOURCE = new ResourceLocation("moreplayermodels", "eyes");
   public static final ResourceLocation RESOURCE_LEFT = new ResourceLocation("moreplayermodels", "eyes_left");
   public static final ResourceLocation RESOURCE_RIGHT = new ResourceLocation("moreplayermodels", "eyes_right");
   private final Random r = new Random();
   public boolean glint = true;
   public NopVector3f browThickness = new NopVector3f(1.0F, 0.4F, 1.0F);
   public NopVector2i eyePos;
   public boolean mirror;
   public int eyeSize;
   public int skinType;
   public boolean useLidTexture;
   public NopVector3f lidColor;
   public NopVector3f browColor;
   public long blinkStart;
   public boolean disableBlink;

   public ModelEyeData() {
      eyePos = NopVector2i.ZERO;
      mirror = false;
      eyeSize = 0;
      skinType = 0;
      useLidTexture = false;
      lidColor = ColorUtil.colorToRgb(0xB4846D);
      browColor = ColorUtil.colorToRgb(0x5B4934);
      blinkStart = 0L;
      disableBlink = false;
      color = new NopVector3f[]{ ColorUtil.colorToRgb(0x7FB238),
              ColorUtil.colorToRgb(0xF7E9A3),
              ColorUtil.colorToRgb(0xA0A0FF),
              ColorUtil.colorToRgb(0xA7A7A7),
              ColorUtil.colorToRgb(0xA4A8B8),
              ColorUtil.colorToRgb(0x4040FF),
              ColorUtil.colorToRgb(0xD87F33),
              ColorUtil.colorToRgb(0xB24CD8),
              ColorUtil.colorToRgb(0x6699D8),
              ColorUtil.colorToRgb(0xE5E533),
              ColorUtil.colorToRgb(0x00D93A),
              ColorUtil.colorToRgb(0x7FCC19),
              ColorUtil.colorToRgb(0xF27FA5),
              ColorUtil.colorToRgb(0x999999),
              ColorUtil.colorToRgb(0x4C7F99),
              ColorUtil.colorToRgb(0x7F3FB2),
              ColorUtil.colorToRgb(0x334CB2),
              ColorUtil.colorToRgb(0x664C33),
              ColorUtil.colorToRgb(0x667F33),
              ColorUtil.colorToRgb(0x993333),
              ColorUtil.colorToRgb(0xFAEE4D),
              ColorUtil.colorToRgb(0x5CDBD5),
              ColorUtil.colorToRgb(0x4A80FF) }
      [r.nextInt(23)];
   }

   @Override
   public CompoundTag getNbt() {
      CompoundTag compound = super.getNbt();
      compound.putBoolean("Glint", glint);
      compound.putBoolean("UseLidTexture", useLidTexture);
      compound.putBoolean("Mirror", mirror);
      compound.putBoolean("DisableBlink", disableBlink);
      compound.putInt("SkinType", skinType);
      compound.putInt("EyeSize", eyeSize);
      compound.putInt("SkinColor", ColorUtil.rgbToColor(lidColor));
      compound.putInt("BrowColor", ColorUtil.rgbToColor(browColor));
      compound.putInt("PositionX", eyePos.x);
      compound.putInt("PositionY", eyePos.y);
      compound.putInt("BrowThickness", (int)(browThickness.y * 10.0F));
      return compound;
   }

   @Override
   public void setNbt(CompoundTag compound) {
      super.setNbt(compound);
      glint = compound.getBoolean("Glint");
      useLidTexture = compound.getBoolean("UseLidTexture");
      mirror = compound.getBoolean("Mirror");
      disableBlink = compound.getBoolean("DisableBlink");
      skinType = compound.getInt("SkinType");
      eyeSize = compound.getInt("EyeSize");
      lidColor = ColorUtil.colorToRgb(compound.getInt("SkinColor"));
      browColor = ColorUtil.colorToRgb(compound.getInt("BrowColor"));
      eyePos = new NopVector2i(compound.getInt("PositionX"), compound.getInt("PositionY"));
      browThickness = new NopVector3f(1.0F, (float)compound.getInt("BrowThickness") / 10.0F, 1.0F);
   }

   public void update(LivingEntity npc) {
      if (npc != null && npc.isAlive() && !disableBlink && !npc.level().isClientSide) {
         if (blinkStart < 0L) { ++blinkStart; }
         else if (blinkStart == 0L) {
            if (npc.isRemoved() || npc.isSleeping()) { return; }
            if (r.nextInt(150) == 1) {
               blinkStart = System.currentTimeMillis();
               Packets.sendNearby(npc, new PacketEyeBlink(npc.getId(), npc.level().dimension()));
            }
         }
         else if (System.currentTimeMillis() - blinkStart > 300L) { blinkStart = -20L; }
      }
   }

   @Override
   public ResourceLocation getUrlTexture() {
      ResourceLocation url = super.getUrlTexture();
      return url == null ? MissingTextureAtlasSprite.getLocation() : url;
   }

}
