package noppes.npcs.controllers.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.MarkType;
import noppes.npcs.api.entity.data.IMark;
import noppes.npcs.api.handler.data.IAvailability;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketMarkData;
import org.jetbrains.annotations.NotNull;

public class MarkData implements ICapabilityProvider {

   public class Mark implements IMark {

      public Availability availability = new Availability();
      public MarkType type = MarkType.NONE;
      public boolean rotate = false;
      public boolean is3d = false;
      public int color = 0xFFED51;

      @Override
      public IAvailability getAvailability() { return availability; }

      @Override
      public int getColor() { return color; }

      @Override
      public void setColor(int colorIn) { color = colorIn; }

      @Override
      public int getType() { return type.get(); }

      public MarkType getEnumType() { return type; }

      @Override
      public void setType(int typeIn) {
         if (typeIn < 1) { typeIn *= -1; }
         typeIn %= MarkType.values().length;
         for (MarkType mt : MarkType.values()) {
            if (mt.get() == typeIn) { type = mt; }
         }
      }

      @Override
      public boolean is3D() { return is3d; }

      @Override
      public void set3D(boolean bo) { is3d = bo; }

      @Override
      public boolean isRotate() { return rotate; }

      @Override
      public void setRotate(boolean rotateIn) { rotate = rotateIn; }

      @Override
      public void update() { syncClients(); }

   }

   public static Capability<MarkData> MARKDATA_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
   private static final ResourceLocation CNPCS_CAPKEY = new ResourceLocation(CustomNpcs.MODID, "markdata");
   private static final String NBT_KEY = "cnpcmarkdata";
   protected static volatile boolean hasMarksInGame;

   public static void register(AttachCapabilitiesEvent<Entity> event) { event.addCapability(CNPCS_CAPKEY, new MarkData()); }

   public static MarkData get(LivingEntity entity) {
      MarkData data = entity.getCapability(MARKDATA_CAPABILITY, null).orElse(new MarkData());
      if (data.entity == null) {
         data.entity = entity;
         data.setNBT(entity.getPersistentData().getCompound(NBT_KEY));
      }
      return data;
   }

   public static  boolean hasMarksInGame() { return hasMarksInGame; }

   private final LazyOptional<MarkData> instance = LazyOptional.of(() -> this);

   private LivingEntity entity;
   public List<MarkData.Mark> marks = new ArrayList<>();

   public void setNBT(CompoundTag compound) {
      List<Mark> marksIn = new ArrayList<>();
      ListTag list = compound.getList("marks", 10);
      for(int i = 0; i < list.size(); ++i) {
         CompoundTag c = list.getCompound(i);
         MarkData.Mark m = new MarkData.Mark();
         m.setType(c.getInt("type"));
         m.color = c.getInt("color");
         m.availability.load(c.getCompound("availability"));
         m.rotate = c.getBoolean("rotate");
         m.is3d = c.getBoolean("is3d");
         marksIn.add(m);
      }
      marks = marksIn;
   }

   public CompoundTag getNBT() {
      CompoundTag compound = new CompoundTag();
      ListTag list = new ListTag();
      for (Mark m : marks) {
         CompoundTag c = new CompoundTag();
         c.put("availability", m.availability.save(new CompoundTag()));
         c.putInt("type", m.type.get());
         c.putInt("color", m.color);
         c.putBoolean("rotate", m.rotate);
         c.putBoolean("is3d", m.is3d);
         list.add(c);
      }
      compound.put("marks", list);
      return compound;
   }

   @Override
   public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, Direction facing) {
      return capability == MARKDATA_CAPABILITY ? instance.cast() : LazyOptional.empty();
   }

   public void save() { entity.getPersistentData().put(NBT_KEY, getNBT()); }

   public MarkData.Mark addMark(int type) {
      MarkData.Mark m = new MarkData.Mark();
      m.setType(type);
      marks.add(m);
      hasMarksInGame = true;
      if (!entity.level().isClientSide) { syncClients(); }
      return m;
   }

   public MarkData.Mark addMark(int type, int color) {
      MarkData.Mark m = new MarkData.Mark();
      m.setType(type);
      m.color = color;
      marks.add(m);
      hasMarksInGame = true;
      if (!entity.level().isClientSide) { syncClients(); }
      return m;
   }

   public MarkData.Mark getNewMark() { return new Mark(); }

   public void syncClients() {
      if (entity == null || entity.level().isClientSide()) { return; }
      Packets.sendAll(new PacketMarkData(entity.getId(), getNBT()));
   }

}
