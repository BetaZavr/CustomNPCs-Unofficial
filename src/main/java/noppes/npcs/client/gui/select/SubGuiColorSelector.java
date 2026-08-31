package noppes.npcs.client.gui.select;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiSliderNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class SubGuiColorSelector
		extends GuiBasic
		implements ITextfieldListener, ISliderListener {

	protected static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID , "textures/gui/color.png");

	protected final BufferedImage bufferedimage;
	protected int colorX;
	protected int colorY;
	protected GuiTextFieldNop textfield;
	protected ColorCallback callback;
	public int color;

	// New from Unofficial Betazavr
	protected boolean hoverTexture;
	protected boolean hasAlpha = false;
	protected float alpha = 1.0f;
	protected int offsetX = 0;
	protected int offsetY = 0;
	protected GuiSliderNop alphaSlider;
	protected GuiTextFieldNop alphaField;
	public Object object;
	protected int xColorPos = 0;
	protected int yColorPos = 0;

	public SubGuiColorSelector(int colorIn) {
		super();
		imageWidth = 176;
		imageHeight = 222;
		color = colorIn;
		setBackground("smallbg.png");

		InputStream stream = null;
		BufferedImage buffer = null;
		try {
			IResource iresource = mc.getResourceManager().getResource(SubGuiColorSelector.resource);
			buffer = ImageIO.read(stream = iresource.getInputStream());
		}
		catch (IOException e) { LogWriter.error(e); }
		finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException ex) { LogWriter.error(ex); }
			}
		}
		bufferedimage = buffer;
	}

	public SubGuiColorSelector(int colorIn, ColorCallback callbackIn) {
		this(colorIn);
		callback = callbackIn;
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		if (button.id == 66) { onClose(); }
	}

	@Override
	public void drawDefaultBackground() {
		super.drawDefaultBackground();
		// background
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		mc.getTextureManager().bindTexture(SubGuiColorSelector.resource);
		drawTexturedModalRect(colorX, colorY, 0, 0, 120, 120);
		if (textfield == null) { return; }
		int x = textfield.getX() + textfield.getWidth() + 4;
		int y = textfield.getY();
		int c = new Color(0xFF808080).getRGB();
		drawRect(x - 1, y - 1, x + 41, y + 21, c);
		c = color;
		if (bufferedimage != null && hoverTexture) {
			try {
				c = new Color(bufferedimage.getRGB(xColorPos, yColorPos) & new Color(0xFFFFFF).getRGB()).getRGB();
				StringBuilder str = new StringBuilder(Integer.toHexString(c));
				while (str.length() < 6) { str.insert(0, "0"); }
				while (str.length() > 6) { str.deleteCharAt(0); }
				if (!textfield.isFocused()) { textfield.setValue(str.toString()); }
			}
			catch (Exception ignored) { }
		}
		else if (!textfield.isFocused()) { textfield.setValue(getColor()); }
		if (callback != null) {
			if (hasAlpha) { c = (int) (alpha * 255.0f) << 24 | c & 0x00FFFFFF; }
			callback.preColor(c);
		}
		float alpha = (float) (c >> 24 & 255) / 255.0F;
		if (alpha == 0.0f) { c += new Color(0xFF000000).getRGB(); }
		drawRect(x, y, x + 40, y + 20, c);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		hoverTexture = !(mouseX < colorX) && !(mouseX > (colorX + 117)) && !(mouseY < colorY) && !(mouseY > colorY + 117);
		xColorPos = (int)((double) mouseX - (double) guiLeft - 30.0D) * 4;
		yColorPos = (int)((double) mouseY - (double) guiTop - 50.0D) * 4;
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void initGui() {
		super.initGui();
		guiLeft += offsetX;
		guiTop += offsetY;
		colorX = guiLeft + 30;
		colorY = guiTop + 50;
		textfield = addTextField(0, guiLeft + 31, guiTop + 20, 70, 20, getColor())
				.setHoverTexts("color.hover")
				.setColor(color)
				.setIsFocused(true)
				.setMaxStringLength(hasAlpha ? 8 : 6);
		addButton(66, guiLeft + 112, guiTop + 198, "gui.done")
				.setSize(60, 20)
				.setHoverTexts("hover.back");
		if (hasAlpha) {
			alpha = (float)(color >> 24 & 255) / 255.0F;
			alphaSlider = addSlider(0, guiLeft + 30, guiTop + 173, alpha)
					.setSize(84, 14)
					.setHoverTexts("color.alpha");
			alphaField = addTextField(1, guiLeft + 117, guiTop + 170, 30, 20, "" + ((int) (alpha * 255.0f)))
					.setMinMaxDefault(0, 255, ((int) (alpha * 255.0f)));
			alphaField.setHoverTexts("color.alpha");
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (bufferedimage != null && hoverTexture) {
			try {
				setColor(bufferedimage.getRGB((int)(mouseX - (double)guiLeft - 30.0D) * 4, (int)(mouseY - (double)guiTop - 50.0D) * 4) & new Color(0xFFFFFF).getRGB());
				return true;
			}
			catch (Exception ignored) { }
		}
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void unFocused(GuiTextFieldNop textfield) {
		if (textfield.id == 0) {
			try { setColor(Integer.parseInt(textfield.getValue(), 16)); }
			catch (NumberFormatException e) { textfield.setValue(getColor()); }
		}
		else if (textfield.id == 1) {
			alpha = textfield.getInteger() / 255.0f;
			color = textfield.getInteger() << 24 | color & 0x00FFFFFF;
			if (alphaSlider != null) { alphaSlider.sliderValue = alpha; }
		}
	}

	public String getColor() {
		StringBuilder str = new StringBuilder(Integer.toHexString(color));
		while (str.length() < (hasAlpha ? 8 : 6)) { str.insert(0, "0"); }
		while (str.length() > (hasAlpha ? 8 : 6)) { str.deleteCharAt(0); }
		return str.toString();
	}

	private void setColor(int colorIn) {
		color = colorIn;
		if (hasAlpha) { color = (int) (alpha * 255.0f) << 24 | color & 0x00FFFFFF; }
		textfield.setValue(getColor());
		if (callback != null) { callback.color(color); }
	}

	// New from Unofficial Betazavr
	public interface ColorCallback {
		void color(int colorIn);
		void preColor(int colorIn);
	}

	@Override
	public void mouseDragged(GuiSliderNop slider) {
		alpha = slider.sliderValue;
		color = (int) (alpha * 255.0f) << 24 | color & 0x00FFFFFF;
		if (alphaField != null) { alphaField.setValue("" + (int) (alpha * 255.0f)); }
	}

	@Override
	public void mousePressed(GuiSliderNop slider) { }

	@Override
	public void mouseReleased(GuiSliderNop slider) { }

	public SubGuiColorSelector setOffsetX(int posX) {
		offsetX = posX;
		return this;
	}

	public SubGuiColorSelector setOffsetY(int posY) {
		offsetY = posY;
		return this;
	}

	public SubGuiColorSelector setIsAlpha() {
		hasAlpha = true;
		return this;
	}

	public Object getObject() { return object; }

}
