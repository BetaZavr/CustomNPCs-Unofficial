package noppes.npcs.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.ITextComponent;

public class ConfirmScreen extends GuiYesNo {

    protected static final ITextComponent GUI_YES = Component.translatable("gui.yes").getParent();
    protected static final ITextComponent GUI_NO = Component.translatable("gui.no").getParent();
    @FunctionalInterface
    public interface BooleanConsumer {
        void accept(boolean value);

        default BooleanConsumer andThen(BooleanConsumer after) {
            java.util.Objects.requireNonNull(after);
            return (t) -> {
                accept(t);
                after.accept(t);
            };
        }
    }

    protected final BooleanConsumer callback;

    public ConfirmScreen(BooleanConsumer callbackIn, ITextComponent messageLine1In, ITextComponent messageLine2In) {
        this(callbackIn, messageLine1In, messageLine2In, GUI_YES, GUI_NO);
    }

    public ConfirmScreen(BooleanConsumer callbackIn, ITextComponent messageLine1In, ITextComponent messageLine2In, ITextComponent yesButtonIn, ITextComponent noButtonIn) {
        super(null,
                text(messageLine1In),
                text(messageLine2In),
                text(yesButtonIn == null ? GUI_YES : yesButtonIn),
                text(noButtonIn == null ? GUI_NO : noButtonIn),
                0);
        callback = callbackIn;
    }

    private static String text(ITextComponent component) { return component == null ? "" : component.getFormattedText(); }

    @Override
    protected void actionPerformed(GuiButton button) { callback.accept(button.id == 0); }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) { callback.accept(false); }
    }

}
