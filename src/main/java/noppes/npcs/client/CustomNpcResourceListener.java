package noppes.npcs.client;

import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.select.ResourceSelection;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nonnull;

@SuppressWarnings("deprecation")
public class CustomNpcResourceListener implements IResourceManagerReloadListener {

	public static int DefaultTextColor = CustomNpcs.LableColor.getRGB();

	private void createTextureCache(IResourceManager resourceManager) {
		enlargeTexture("planks_oak", resourceManager);
		enlargeTexture("planks_big_oak", resourceManager);
		enlargeTexture("planks_birch", resourceManager);
		enlargeTexture("planks_jungle", resourceManager);
		enlargeTexture("planks_spruce", resourceManager);
		enlargeTexture("planks_acacia", resourceManager);
		enlargeTexture("iron_block", resourceManager);
		enlargeTexture("diamond_block", resourceManager);
		enlargeTexture("stone", resourceManager);
		enlargeTexture("gold_block", resourceManager);
		enlargeTexture("wool_colored_white", resourceManager);
	}

	private void enlargeTexture(String texture, IResourceManager resourceManager) {
        ResourceLocation location = new ResourceLocation("minecraft", "textures/cache/" + texture + ".png");
		IResource resource = null;
		try { resource = resourceManager.getResource(location); } catch (Exception e) { LogWriter.debug("Not found texture: \""+location+"\""); }
		if (!(resource instanceof TextureCache)) {
			new TextureCache(location).loadTexture(resourceManager);
		} else {
			((TextureCache) resource).setImage(new ResourceLocation("minecraft", "textures/blocks/" + texture + ".png"));
		}
	}

	public void onResourceManagerReload(@Nonnull IResourceManager resourceManager) {
		if (resourceManager instanceof SimpleReloadableResourceManager) {
			createTextureCache(resourceManager);
			SimpleReloadableResourceManager simplemanager = (SimpleReloadableResourceManager) resourceManager;
			FolderResourcePack pack = new FolderResourcePack(CustomNpcs.Dir);
			simplemanager.reloadResourcePack(pack);
			CustomNpcResourceListener.DefaultTextColor = CustomNpcs.LableColor.getRGB();
			ResourceSelection.resourcesData.clear();
			ResourceSelection.preload(".png");
		}
	}

}
