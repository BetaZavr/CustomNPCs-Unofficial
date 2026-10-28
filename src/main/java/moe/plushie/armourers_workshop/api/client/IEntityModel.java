package moe.plushie.armourers_workshop.api.client;

import java.util.Collection;
import moe.plushie.armourers_workshop.api.client.model.IModelPart;

@SuppressWarnings("unused")
public interface IEntityModel<S> {

    IModelPart abi$getPartByName(String var1);

    Collection<? extends IModelPart> abi$allParts();

    interface Provider<S> {
        IEntityModel<S> abi$getEntityModel(S var1);
    }

}

