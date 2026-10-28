package moe.plushie.armourers_workshop.api.core.math;

@SuppressWarnings("unused")
public interface ITransform3f extends ITransform {

    boolean isIdentity();

    IVector3f translate();

    IVector3f rotation();

    IVector3f scale();

    IVector3f afterTranslate();

    IVector3f pivot();

}

