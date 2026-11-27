package noppes.npcs.client.model.part;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.constants.EnumParts;

public class ModelRendererPartOBJ extends ModelRenderer {

    protected final float x;
    protected final float y;
    protected final float z;
    protected EnumParts part;
    protected ParameterizedModel model;
    public boolean smallArms = false;

    public ModelRendererPartOBJ(ModelBase model, EnumParts partIn, float xIn, float yIn, float zIn) {
        super(model);
        x = xIn;
        y = yIn;
        z = zIn;
        part = partIn;
    }

    @Override
    public void render(float scale) {
        if (model != null && model.objModel != null) {
            GlStateManager.pushMatrix();
            float addX = 0.0f;
            if (smallArms && (part == EnumParts.ARM_LEFT_FULL || part == EnumParts.ARM_RIGHT_FULL)) {
                GlStateManager.scale(0.75f, 1.0f, 1.0f);
                addX = part == EnumParts.ARM_LEFT_FULL ? -0.0175f : 0.0175f;
            }
            if ((x + addX) != 0.0f || y != 0.0f || z != 0.0f) {
                GlStateManager.translate(x + addX, y, z);
            } // offset relative to model

            GlStateManager.rotate(180.0f, 1.0f, 0.0f, 0.0f);
            // texture
            ModelBuffer.render(model); // display in game
            GlStateManager.popMatrix();
        }
    }

    public void setOBJModel(ParameterizedModel modelIn) { model = modelIn; }

}
