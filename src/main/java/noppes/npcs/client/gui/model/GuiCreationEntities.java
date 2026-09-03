package noppes.npcs.client.gui.model;

import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomEntities;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.common.util.ComponentOrderComparator;

public class GuiCreationEntities extends GuiCreationScreenInterface implements ICustomScrollListener {

   protected final Map<Component, EntityType<? extends Entity>> types;
   protected GuiCustomScrollNop scroll;
   protected boolean resetToSelected = true;

   public GuiCreationEntities(EntityNPCInterface npc) {
      super(npc);

      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      types = getAllEntities(minecraft.level, false);
      active = 1;
      xOffset = 60;
   }

   @Override
   public void init() {
      super.init();
      add(new GuiButtonNop(this, 10, "Reset To NPC", guiLeft, guiTop + 46,
              button -> {
                 playerdata.setEntity(null);
                 npc.display.setSkinTexture(CustomNpcs.MODID + ":textures/entity/humanmale/steve.png");
                 resetToSelected = true;
                 npc.reset();
                 init();
              }).setSize(120, 20));
      if (scroll == null) {
         List<Component> list = new ArrayList<>();
         LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
         for (Map.Entry<Component, EntityType<? extends Entity>> entry : types.entrySet()) {
            String line = entry.getValue().getDescriptionId();
            List<Component> hover = new ArrayList<>();
            if (line.startsWith("entity.customnpcs.")) {
               hover.add(Component.translatable(line.replace("entity.customnpcs.", "entity.hover.customnpcs.")));
            }
            else if (line.startsWith("entity.minecraft.")) {
               hover.add(Component.translatable("entity.hover.minecraft"));
            }
            else {
               hover.add(Component.translatable("entity.hover.in.mod"));
               hover.add(Component.literal(line.substring(7, line.indexOf(".", 7))));
            }
            list.add(entry.getKey());
            hts.put(hts.size(), hover);
         }
         scroll = addScroll(0)
                 .setUnsortedList(list)
                 .setHoverTexts(hts);
      }
      int index = -1;
      int i = 0;
      for(Component component : scroll.getNormalList()) {
         EntityType<?> type = types.get(component);
         if ((entity == null && type == CustomEntities.entityCustomNpc) || (entity != null && type == entity.getType())) {
            index = i;
            break;
         }
         i++;
      }
      if (index >= 0) { scroll.setSelected(index); }
      else { scroll.setSelected("entity." + CustomNpcs.MODID + ".customnpc"); }

      if (resetToSelected) {
         scroll.scrollTo(scroll.getSelected());
         resetToSelected = false;
      }
      add(scroll.setPos(guiLeft, guiTop + 68)
              .setSize(120, imageHeight - 96));
      addLabel(110, guiLeft + 124, guiTop + 5, "gui.simpleRenderer")
              .setSize(134, 10)
              .setColor(CustomNpcs.MainColor.getRGB());
      add(new GuiButtonYesNo(this, 110, guiLeft + 260, guiTop, playerdata.simpleRender,
              (b) -> playerdata.simpleRender = ((GuiButtonYesNo)b).getBoolean()));
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      if (!scroll.hasSelected()) { playerdata.setEntity(null); }
      else {
         playerdata.setEntity(ForgeRegistries.ENTITY_TYPES.getKey(types.get(scroll.getNormalSelected())));
         if (scroll.getNormalSelected().getContents() instanceof TranslatableContents trComp && trComp.getKey().contains("geckoaddon")) {
            npc.display.setSkinTexture(CustomNpcs.MODID + ":textures/entity/humanmale/steve.png");
         }
      }
      Entity entity = playerdata.getEntity(npc);
      if (entity != null) {
         if (minecraft == null) { minecraft = Minecraft.getInstance(); }
         EntityRenderer<? super Entity> render = minecraft.getEntityRenderDispatcher().getRenderer(entity);
         try {
            if (render instanceof LivingEntityRenderer && !render.getTextureLocation(entity).toString().equals("minecraft:missingno")) {
               npc.display.setSkinTexture(render.getTextureLocation(entity).toString());
            }
         }
         catch (Exception e) { npc.display.setSkinTexture(CustomNpcs.MODID + ":textures/entity/humanmale/steve.png"); }
      }
      else {
         npc.display.setSkinTexture(CustomNpcs.MODID + ":textures/entity/humanmale/steve.png");
      }
      npc.reset();
      init();
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

   public static Map<Component, EntityType<? extends Entity>> getAllEntities(Level level, boolean addVanillaDragon) {
      Map<Component, EntityType<? extends Entity>> data = new TreeMap<>(Comparator.comparing(
              (c) -> c.getString().toLowerCase(), new ComponentOrderComparator()
      ));
      for (EntityType<?> ent : ForgeRegistries.ENTITY_TYPES.getValues()) {
         try {
            Entity e = ent.create(level);
            if (e != null) {
               if (LivingEntity.class.isAssignableFrom(e.getClass()) &&
                       (addVanillaDragon || !EnderDragon.class.isAssignableFrom(e.getClass())))
               { data.put(Component.translatable(ent.getDescriptionId()), ent); }
            }
         } catch (Exception ignored) {}
      }
      return data;
   }

}
