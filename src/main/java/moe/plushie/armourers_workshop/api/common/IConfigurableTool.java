package moe.plushie.armourers_workshop.api.common;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public interface IConfigurableTool {

    void createToolProperties(Consumer<IConfigurableToolProperty<?>> var1);

}

