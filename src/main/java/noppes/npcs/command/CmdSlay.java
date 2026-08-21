package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;

public class CmdSlay {

   static Map<String, Class<?>> slayMap = new LinkedHashMap<>();

   private static Map<String, Class<?>> getSlay(Level level) {
      if (!slayMap.isEmpty()) { return slayMap; }
      slayMap.put("all", LivingEntity.class);
      slayMap.put("mobs", Monster.class);
      slayMap.put("animals", Animal.class);
      slayMap.put("items", ItemEntity.class);
      slayMap.put("xporbs", ExperienceOrb.class);
      slayMap.put("npcs", EntityNPCInterface.class);

      for (ResourceLocation resource : ForgeRegistries.ENTITY_TYPES.getKeys()) {
         EntityType<?> ent = ForgeRegistries.ENTITY_TYPES.getValue(resource);
         if (ent != null && ent.getCategory() != MobCategory.MISC) {
            String name = ent.getDescriptionId();
            try {
               Entity e = ent.create(level);
               if (e != null) {
                  e.remove(RemovalReason.DISCARDED);
                  Class<? extends Entity> cls = e.getClass();
                  if (!EntityNPCInterface.class.isAssignableFrom(cls) && LivingEntity.class.isAssignableFrom(cls)) {
                     slayMap.put(name.toLowerCase(), cls);
                  }
               }
            } catch (Throwable ignored) {}
         }
      }

      slayMap.remove("monster");
      slayMap.remove("mob");
      return slayMap;
   }

   public static LiteralArgumentBuilder<CommandSourceStack> register() {
       return Commands.literal("slay")
               .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
               .then(Commands.argument("type", StringArgumentType.word())
                       .then(Commands.argument("range", IntegerArgumentType.integer(1))
                               .executes((context) -> {
                                  ArrayList<Class<?>> toDelete = new ArrayList<>();
                                  boolean deleteNPCs = false;
                                  String delete = StringArgumentType.getString(context, "type");
                                  Class<?> cls = getSlay((context.getSource()).getLevel()).get(delete);
                                  if (cls != null) {
                                     toDelete.add(cls);
                                  }
                                  if (delete.equals("mobs")) {
                                     toDelete.add(Ghast.class);
                                     toDelete.add(EnderDragon.class);
                                  }
                                  if (delete.equals("npcs")) {
                                     deleteNPCs = true;
                                  }
                                  int count = 0;
                                  int range = IntegerArgumentType.getInteger(context, "range");
                                  AABB box = (new AABB((context.getSource()).getPosition(), (context.getSource()).getPosition().add(1.0D, 1.0D, 1.0D))).inflate(range, range, range);
                                  List<? extends Entity> list = (context.getSource()).getLevel().getEntitiesOfClass(LivingEntity.class, box);
                                  Iterator<? extends Entity> var9 = list.iterator();
                                  while(true) {
                                     Entity entity;
                                     do {
                                        do {
                                           do {
                                              if (!var9.hasNext()) {
                                                 if (toDelete.contains(ExperienceOrb.class)) {
                                                    list = (context.getSource()).getLevel().getEntitiesOfClass(ExperienceOrb.class, box);

                                                    for(var9 = list.iterator(); var9.hasNext(); ++count) {
                                                       entity = var9.next();
                                                       entity.setRemoved(RemovalReason.DISCARDED);
                                                    }
                                                 }
                                                 if (toDelete.contains(ItemEntity.class)) {
                                                    list = (context.getSource()).getLevel().getEntitiesOfClass(ItemEntity.class, box);

                                                    for(var9 = list.iterator(); var9.hasNext(); ++count) {
                                                       entity = var9.next();
                                                       entity.setRemoved(RemovalReason.DISCARDED);
                                                    }
                                                 }
                                                 int finalCount = count;
                                                 (context.getSource()).sendSuccess(() -> Component.translatable(finalCount + " entities deleted"), false);
                                                 return 1;
                                              }
                                              entity = var9.next();
                                           } while(entity instanceof Player);
                                        } while(entity instanceof TamableAnimal && ((TamableAnimal)entity).isTame());
                                     } while(entity instanceof EntityNPCInterface && !deleteNPCs);
                                     if (delete(entity, toDelete)) { ++count; }
                                  }
                               })
                       )
               );
   }

   private static boolean delete(Entity entity, ArrayList<Class<?>> toDelete) {
      Iterator<Class<?>> var2 = toDelete.iterator();

      Class<?> delete;
      do {
         do {
            if (!var2.hasNext()) {
               return false;
            }

            delete = var2.next();
         } while(delete == Animal.class && entity instanceof Horse);
      } while(!delete.isAssignableFrom(entity.getClass()));

      entity.setRemoved(RemovalReason.DISCARDED);
      return true;
   }

}
