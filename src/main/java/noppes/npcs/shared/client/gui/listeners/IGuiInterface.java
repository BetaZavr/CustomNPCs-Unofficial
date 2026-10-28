package noppes.npcs.shared.client.gui.listeners;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.shared.client.gui.components.*;

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public interface IGuiInterface {

    void add(IComponentGui element);

    void buttonEvent(GuiButtonNop button);

    boolean mouseButtonEvent(GuiButtonNop button, int mouseButton);

    GuiButtonNop getButton(int id);

    GuiMenuSideButton getSideButton(int id);

    GuiMenuTopButton getTopButton(int id);

    GuiTextFieldNop getTextField(int id);

    GuiLabel getLabel(int id);

    GuiSliderNop getSlider(int id);

    GuiCustomScrollNop getScroll(int id);

    IComponentGui getExtra(int id);

    void save();

    boolean hasSubGui();

    Screen getSubGui();

    int getWidth();

    int getHeight();

    Screen getParent();

    void subGuiClosed(Screen var1);

    GuiWrapper getWrapper();

    // New fields from Unofficial (BetaZavr)
    void drawHoverText(String text, Object... args);

    void drawWait(GuiGraphics graphics);

    List<Component> getHoverText();

    boolean isMouseHover(double mX, double mY, double px, double py, double pwidth, double pheight);

    void setHoverText(@Nullable List<Component> components);

    void setHoverText(Object... components);

    IComponentGui get(int id);

    GuiLabel addLabel(int id, int x, int y, Object label);

    GuiButtonNop addButton(int id, int x, int y, Object label);

    GuiButtonNop addButton(int id, int x, int y, boolean isBiDirectional,  int variant, Object... variants);

    GuiCheckBoxNop addCheckBox(int id, int x, int y, Object labelTrue, Object labelFalse, boolean selected);

    GuiMenuTopButton addTopButton(int id, int x, int y, Object label);

    GuiMenuTopIconButton addTopButton(int id, int x, int y, Object label, ItemStack stack);

    GuiMenuSideButton addSideButton(int id, int x, int y, Object label);

    GuiButtonYesNo addYesNo(int id, int x, int y, boolean isYes);

    GuiSliderNop addSlider(int id, int x, int y, float sliderValue);

    GuiTextFieldNop addTextField(int id, int x, int y, int width, int height, Object value);

    GuiCustomScrollNop addScroll(int id);

    GuiCustomScrollNop addScroll(int id, boolean isMultipleSelection);

    void extraEvent(Object extra);

    int getX();

    int getY();

    boolean doubleClicked(IComponentGui component);

}
