package noppes.npcs.client.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.GlStateManager;
import noppes.npcs.CustomNpcs;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.Arrays;

public class GlCache {

    private static final Int2ObjectOpenHashMap<int[]> SIZES = new Int2ObjectOpenHashMap<>();
    private static final int[] BOUND = new int[32];
    private static int activeUnit;

    static { Arrays.fill(BOUND, -1); }

    public static void onBind(int id) { BOUND[activeUnit] = id; }

    public static void onActiveTexture(int unit) {
        int index = unit - GL13.GL_TEXTURE0;
        if (index >= 0 && index < BOUND.length) { activeUnit = index; }
    }

    public static int boundTexture() {
        if (!CustomNpcs.FastNpcRender || BOUND[activeUnit] < 0) { return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D); }
        return BOUND[activeUnit];
    }

    public static void bindTexture(int id) {
        if (CustomNpcs.FastNpcRender) {
            GlStateManager.bindTexture(id);
            return;
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
    }

    public static int[] boundTextureSize() {
        if (!CustomNpcs.FastNpcRender) { return querySize(); }
        int id = boundTexture();
        int[] size = SIZES.get(id);
        if (size == null) {
            size = querySize();
            SIZES.put(id, size);
        }
        return size;
    }

    public static void forget(int id) {
        SIZES.remove(id);
        for (int i = 0; i < BOUND.length; i++) {
            if (BOUND[i] == id) { BOUND[i] = -1; }
        }
    }

    private static int[] querySize() {
        return new int[] { GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH),
                GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT) };
    }

}
