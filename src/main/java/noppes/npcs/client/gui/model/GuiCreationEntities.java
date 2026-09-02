package noppes.npcs.client.gui.model;

import java.util.*;

import net.minecraft.client.renderer.entity.NPCRendererHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.old.*;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.util.ComponentOrderComparator;

public class GuiCreationEntities extends GuiCreationScreenInterface
		implements ICustomScrollListener {

	protected final Map<Component, EntityEntry> types;
	protected GuiCustomScrollNop scroll;
	protected boolean resetToSelected = true;

	public GuiCreationEntities(EntityNPCInterface npc) {
		super(npc);
		types = getAllEntities(false);
		active = 1;
		xOffset = 60;
	}

	@Override
	public void initGui() {
		super.initGui();
		add(new GuiButtonNop(this, 10, "Reset To NPC", guiLeft, guiTop + 46,
				button -> {
					playerdata.setEntity(null);
					npc.display.setSkinTexture(CustomNpcs.MODID + ":textures/entity/humanmale/steve.png");
					resetToSelected = true;
					npc.reset();
					npc.display.width = npc.baseWidth;
					npc.display.height = npc.baseHeight;
					initGui();
				}).setSize(120, 20));
		if (scroll == null) {
			List<Component> list = new ArrayList<>();
			LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
			for (Map.Entry<Component, EntityEntry> entry : types.entrySet()) {
				ResourceLocation loc = entry.getValue().getRegistryName();
				if (loc != null) {
					List<Component> hover = new ArrayList<>();
					if (loc.getResourceDomain().equals(CustomNpcs.MODID)) {
						hover.add(Component.translatable("entity.hover.customnpcs." + entry.getValue().getName()));
					}
					else if (loc.getResourceDomain().equals("minecraft")) {
						hover.add(Component.translatable("entity.hover.minecraft"));
					}
					else {
						hover.add(Component.translatable("entity.hover.in.mod"));
						hover.add(Component.literal(loc.getResourceDomain()));
					}
					list.add(entry.getKey());
					hts.put(hts.size(), hover);
				}
			}
			scroll = addScroll(0)
					.setUnsortedList(list)
					.setHoverTexts(hts);
		}
		int index = -1;
		int i = 0;
		for(Component component : scroll.getNormalList()) {
			EntityEntry type = types.get(component);
			if ((entity == null && type.getEntityClass() == EntityCustomNpc.class) ||
					(entity != null && type.getEntityClass() == entity.getClass())) {
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
				.setColor(CustomNpcs.MainColor.getRGB());
		add(new GuiButtonYesNo(this, 110, guiLeft + 260, guiTop, playerdata.simpleRender,
				(b) -> playerdata.simpleRender = ((GuiButtonYesNo)b).getBoolean()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (!scroll.hasSelected()) { playerdata.setEntity(null); }
		else {
			playerdata.setEntity((Class<? extends EntityLivingBase>) types.get(scroll.getNormalSelected()).getEntityClass());
			if (scroll.getNormalSelected().getContents() instanceof TextComponentTranslation &&
					((TextComponentTranslation) scroll.getNormalSelected().getContents()).getKey().contains("geckoaddon")) {
				npc.display.setSkinTexture(CustomNpcs.MODID + ":textures/entity/humanmale/steve.png");
			}
		}

		EntityLivingBase entity = playerdata.getEntity(npc);
		if (entity != null) {
			Render<Entity> mcRender = mc.getRenderManager().getEntityClassRenderObject(entity.getClass());
			if (mcRender instanceof RenderLivingBase<?>) {
				@SuppressWarnings("rawtypes")
				RenderLivingBase<EntityLivingBase> render = (RenderLivingBase) mcRender;
				if (!NPCRendererHelper.getTexture(render, entity).equals(TextureMap.LOCATION_MISSING_TEXTURE.toString())) {
					npc.display.setSkinTexture(NPCRendererHelper.getTexture(render, entity));
				}
			}
		}
		else { npc.display.setSkinTexture(CustomNpcs.MODID + ":textures/entity/humanmale/steve.png"); }
		npc.reset();
		npc.display.width = npc.baseWidth;
		npc.display.height = npc.baseHeight;
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

	public static Map<Component, EntityEntry> getAllEntities(boolean addVanillaDragon) {
		Map<Component, EntityEntry> data = new TreeMap<>(Comparator.comparing(
				(c) -> c.getString().toLowerCase(), new ComponentOrderComparator()
		));
        for (EntityEntry entry : ForgeRegistries.ENTITIES.getValuesCollection()) {
			try {
				Class<? extends Entity> cl = entry.getEntityClass();
				if (EntityLivingBase.class.isAssignableFrom(cl) &&
						(addVanillaDragon ||!EntityDragon.class.isAssignableFrom(cl))) {
					// old entities
					if (EntityNPCHumanMale.class.isAssignableFrom(cl) ||
							EntityNPCVillager.class.isAssignableFrom(cl) ||
							EntityNPCHumanFemale.class.isAssignableFrom(cl) ||
							EntityNPCDwarfMale.class.isAssignableFrom(cl) ||
							EntityNPCFurryMale.class.isAssignableFrom(cl) ||
							EntityNpcMonsterMale.class.isAssignableFrom(cl) ||
							EntityNpcMonsterFemale.class.isAssignableFrom(cl) ||
							EntityNpcSkeleton.class.isAssignableFrom(cl) ||
							EntityNPCDwarfFemale.class.isAssignableFrom(cl) ||
							EntityNPCFurryFemale.class.isAssignableFrom(cl) ||
							EntityNPCOrcMale.class.isAssignableFrom(cl) ||
							EntityNPCOrcFemale.class.isAssignableFrom(cl) ||
							EntityNPCElfMale.class.isAssignableFrom(cl) ||
							EntityNPCElfFemale.class.isAssignableFrom(cl) ||
							EntityNpcEnderchibi.class.isAssignableFrom(cl) ||
							EntityNpcNagaMale.class.isAssignableFrom(cl) ||
							EntityNpcNagaFemale.class.isAssignableFrom(cl) ||
							EntityNPCEnderman.class.isAssignableFrom(cl))
					{ continue; }
					
					ResourceLocation loc = entry.getRegistryName();
					if (loc == null) { continue; }
					Component name;
					if (loc.getResourceDomain().equals(CustomNpcs.MODID)) { name = Component.translatable("entity.customnpcs." + entry.getName()); }
					else { name = Component.translatable("entity." + entry.getName() + ".name"); }
					data.put(name, entry);
				}
			} catch (Exception ignored) {}
		}
		return data;
    }
	
}
