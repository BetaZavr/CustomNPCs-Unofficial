package moe.plushie.armourers_workshop.api.core;

@SuppressWarnings("unused")
public interface IDataSerializable {

    interface Mutable extends Immutable {
        void deserialize(IDataSerializer var1);
    }

    interface Immutable extends IDataSerializable {
        void serialize(IDataSerializer var1);
    }

}

