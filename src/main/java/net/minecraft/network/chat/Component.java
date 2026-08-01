package net.minecraft.network.chat;

import net.minecraft.util.text.*;
import noppes.npcs.api.interfaces.IgnoreForAPI;
import noppes.npcs.api.mixin.util.text.IStyleMixin;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;

@IgnoreForAPI
public class Component {

    public static Component from(ITextComponent component) { return new Component(component); }

    public static Component empty() { return new Component("", true); }

    public static Component literal(String message) { return new Component(message, true); }

    public static Component translatable(String localisationKey) { return new Component(localisationKey, false); }

    public static Component translatable(String localisationKey, Object... objects) { return new Component(localisationKey, objects); }

    public static Component jsonToComponent(String json) {
        if (json == null || json.isEmpty()) { return Component.empty(); }
        try {
            ITextComponent component = ITextComponent.Serializer.jsonToComponent(json);
            return component == null ? Component.translatable(json) : new Component(component);
        }
        catch (Exception ignored) { }
        return Component.translatable(json);
    }

    public Component copy() {
        return new Component(parent.createCopy());
    }

    private final ITextComponent parent;

    public Component(String message, boolean isSimple) {
        if (isSimple || (message.contains("%") && !message.contains("%%"))) { parent = new TextComponentString(message); }
        else { parent = new TextComponentTranslation(message); }
    }

    public Component(String message, Object... objects) { parent = new TextComponentTranslation(message, objects); }

    public Component(ITextComponent label) { parent = label; }

    public @Nonnull ITextComponent setStyle(@Nonnull Style style) { return parent.setStyle(style); }

    public @Nonnull Style getStyle() { return parent.getStyle(); }

    public @Nonnull Component withColor(int color) {
        ((IStyleMixin) parent.getStyle()).npcs$setColor(color);
        return this;
    }

    public @Nonnull ITextComponent appendText(@Nonnull String text) { return parent.appendText(text); }

    public @Nonnull ITextComponent appendSibling(@Nonnull ITextComponent component) { return parent.appendSibling(component); }

    public @Nonnull String getUnformattedComponentText() { return parent.getUnformattedComponentText(); }

    public @Nonnull String getUnformattedText() { return parent.getUnformattedText(); }

    public @Nonnull String getFormattedText() { return parent.getFormattedText(); }

    public @Nonnull List<ITextComponent> getSiblings() { return parent.getSiblings(); }

    public @Nonnull ITextComponent createCopy() { return parent.createCopy(); }

    public @Nonnull Iterator<ITextComponent> iterator() { return parent.iterator(); }

    public @Nonnull Component append(@Nonnull String text) {
        parent.appendText(text);
        return this;
    }

    public @Nonnull Component append(@Nonnull ITextComponent component) {
        parent.appendSibling(component instanceof Component ? ((Component) component).parent : component);
        return this;
    }

    public @Nonnull Component append(@Nonnull Component component) {
        parent.appendSibling(component.parent);
        return this;
    }

    public @Nonnull String getString() {
        try {
            return Util.instance.deleteColor(parent.getFormattedText());
        } catch (Exception e) {
            if (parent instanceof TextComponentTranslation) {
                return Util.instance.deleteColor(((TextComponentTranslation) parent).getKey());
            }
        }
        return parent.getUnformattedText();
    }

    public Component withStyle(TextFormatting ... textFormats) {
        Style style = parent.getStyle();
        for (TextFormatting textFormatting : textFormats) {
            if (textFormatting == TextFormatting.RESET) {
                style.setObfuscated(false);
                style.setBold(false);
                style.setStrikethrough(false);
                style.setUnderlined(false);
                style.setItalic(false);
                style.setColor(TextFormatting.RESET);
            }
            else if (textFormatting == TextFormatting.OBFUSCATED) { style.setObfuscated(true); }
            else if (textFormatting == TextFormatting.BOLD) { style.setBold(true); }
            else if (textFormatting == TextFormatting.STRIKETHROUGH) { style.setStrikethrough(true); }
            else if (textFormatting == TextFormatting.UNDERLINE) { style.setUnderlined(true); }
            else if (textFormatting == TextFormatting.ITALIC) { style.setItalic(true); }
            else { style.setColor(textFormatting); }
        }
        parent.setStyle(style);
        return this;
    }

    public ITextComponent getContents() {
        List<ITextComponent> list = parent.getSiblings();
        if (list.isEmpty()) { return new TextComponentString(""); }
        return list.get(0);
    }

    public ITextComponent getParent() { return parent; }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (!(other instanceof Component)) { return false; }
        return getFormattedText().equals(((Component) other).getFormattedText());
    }

    @Override
    public int hashCode() { return getFormattedText().hashCode(); }

}
