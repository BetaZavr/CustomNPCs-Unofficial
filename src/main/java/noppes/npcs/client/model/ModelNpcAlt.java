package noppes.npcs.client.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import noppes.npcs.api.util.IModelRenderer;
import noppes.npcs.client.model.animation.AddedPartConfig;
import noppes.npcs.client.model.animation.AnimationConfig;
import noppes.npcs.client.model.animation.AnimationStack;
import noppes.npcs.client.parts.AnimData;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.*;

@SuppressWarnings("unused")
public class ModelNpcAlt<T extends EntityCustomNpc> extends PlayerModel<T> {

    public static final AnimData editAnimDataSelect = new AnimData();
    public static final Map<Integer, Map<Integer, List<ModelRendererAlt>>> animAddedChildren = new HashMap<>(); // { animation ID, [part ID, list<ModelRender>]}

    public static void copyModelAngles(ModelRendererAlt source, ModelRendererAlt dest) {
        //dest.copyModelAngles(source);
    }

    protected ModelHeadwear bipedHeadwear_64;
    protected ModelHeadwear bipedHeadwear_128;
    protected ModelHeadwear bipedHeadwear_256;
    protected ModelHeadwear bipedHeadwear_512;
    protected ModelHeadwear bipedHeadwear_1024;
    protected ModelHeadwear bipedHeadwear_2048;
    protected ModelHeadwear bipedHeadwear_4096;

    protected ModelHeadwear bipedHeadwear_64_old;
    protected ModelHeadwear bipedHeadwear_128_old;
    protected ModelHeadwear bipedHeadwear_256_old;
    protected ModelHeadwear bipedHeadwear_512_old;
    protected ModelHeadwear bipedHeadwear_1024_old;
    protected ModelHeadwear bipedHeadwear_2048_old;
    protected ModelHeadwear bipedHeadwear_4096_old;
    protected ModelRendererAlt bipedCape;
    public AnimationStack rightStackData = new AnimationStack(7);
    public AnimationStack leftStackData = new AnimationStack(6);
    public boolean smallArmsIn;
    public boolean isClassicPlayer;

    public ModelNpcAlt(ModelPart modelPart, boolean isSlim) {
        super(modelPart, isSlim);
        smallArmsIn = isSlim;
    }

    public static void loadAnimationModel(AnimationConfig animation) {
        if (animation == null) { return; }
        animAddedChildren.remove(animation.id);
        if (!animation.addParts.isEmpty()) {
            if (!animAddedChildren.containsKey(animation.id)) { animAddedChildren.put(animation.id, new TreeMap<>()); }
            Map<Integer, List<ModelRendererAlt>> map = animAddedChildren.get(animation.id);
            map.clear();
            // create all
            for (int partID : animation.addParts.keySet()) {
                for (AddedPartConfig addedPartConfig : animation.addParts.get(partID)) {
                    if (!map.containsKey(addedPartConfig.parentPart)) { map.put(addedPartConfig.parentPart, new ArrayList<>()); }
                    map.get(addedPartConfig.parentPart).add(new ModelRendererAlt(addedPartConfig));
                }
            }
        }
    }

    public IModelRenderer getPart(int partId) {
        /*switch(partId) {
            case 0: return (IModelRenderer) head;
            case 1: return (IModelRenderer) leftArm;
            case 2: return (IModelRenderer) rightArm;
            case 3: return (IModelRenderer) body;
            case 4: return (IModelRenderer) leftLeg;
            case 5: return (IModelRenderer) rightLeg;
            case 6: return leftStackData;
            case 7: return rightStackData;
        }
        for (int i = 0; i < 6; i++) {
            ModelRendererAlt biped = (ModelRendererAlt) getPart(i);
            if (biped.childModels == null) { continue; }
            ModelRendererAlt child = getChildPart(biped.childModels, partId);
            if (child != null) { return child; }
        }*/
        return null;
    }

}
