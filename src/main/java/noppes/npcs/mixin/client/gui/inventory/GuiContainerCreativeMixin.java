package noppes.npcs.mixin.client.gui.inventory;

import io.netty.buffer.Unpooled;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.ClientEventHandler;
import noppes.npcs.constants.EnumGuiType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiContainerCreative.class, priority = 498)
public class GuiContainerCreativeMixin {

    @Final @Shadow private static ResourceLocation CREATIVE_INVENTORY_TABS;
    @Shadow private static int selectedTabIndex;

    @Inject(method = "initGui", at = @At("TAIL"))
    public void npcs$initGui(CallbackInfo ci) {
        if (CustomNpcs.InventoryGuiEnabled) {
            GuiContainerCreative parent = (GuiContainerCreative) (Object) this;
            int x = parent.getGuiLeft() - 30;
            int y = parent.getGuiTop() + 4;
            parent.buttonList.add(new GuiButtonImage(150, x, y, 32, 28, 0, 128, 32, CREATIVE_INVENTORY_TABS));
            parent.buttonList.add(new GuiButtonImage(151, x, y + 28, 32, 28, 0, 128, 32, CREATIVE_INVENTORY_TABS));
        }
    }

    @Inject(
            method = "drawScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Arrays;copyOfRange([Ljava/lang/Object;II)[Ljava/lang/Object;",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/inventory/GuiContainerCreative;drawDefaultBackground()V"),
                    to = @At(value = "INVOKE", target = "Ljava/util/Arrays;copyOfRange([Ljava/lang/Object;II)[Ljava/lang/Object;", ordinal = 0)
            )
    )
    public void npcs$drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GuiContainerCreative parent = (GuiContainerCreative) (Object) this;
        if (CustomNpcs.InventoryGuiEnabled) {
            int x = parent.getGuiLeft() - 30;
            int y = parent.getGuiTop() + 4;

            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.color(2.0f, 2.0f, 2.0f, 1.0f);

            GlStateManager.pushMatrix();
            parent.mc.getTextureManager().bindTexture(CREATIVE_INVENTORY_TABS);
            GlStateManager.translate(x, y + 28, 0.0f);
            GlStateManager.rotate(-90.0f, 0.0f, 0.0f, 1.0f);
            int mx = mouseX - x;
            int my = mouseY - y;
            if (mx > 0 && mx <= 32 && my > 0 && my <= 28) { parent.drawTexturedModalRect(0, 2, 28, 32, 28, 32); }
            else { parent.drawTexturedModalRect(0, 0, 28, 0, 28, 30); }
            GlStateManager.translate(-28.0f, 0.0f, 0.0f);
            my -= 28;
            if (mx > 0 && mx <= 32 && my > 0 && my <= 28) { parent.drawTexturedModalRect(0, 2, 28, 32, 28, 32); }
            else { parent.drawTexturedModalRect(0, 0, 28, 0, 28, 30); }
            GlStateManager.popMatrix();

            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            parent.mc.getTextureManager().bindTexture(ClientEventHandler.COMPASS_ICONS[(int) (31L - (System.currentTimeMillis() / 100L) % 32L)]);
            GlStateManager.translate(x + 10, y + 6, 0.0f);
            float s = 16.0f / 256.0f;
            GlStateManager.scale(s, s, s);
            parent.drawTexturedModalRect(0, 0, 0, 0, 256, 256);
            GlStateManager.translate(0.0f, 28.0f / s, 0.0f);
            parent.mc.getTextureManager().bindTexture(new ResourceLocation("textures/items/book_normal.png"));
            parent.drawTexturedModalRect(0, 0, 0, 0, 256, 256);
            GlStateManager.popMatrix();
            if (mouseX > x + 2 && mouseX <= x + 30) {
                if (mouseY > y && mouseY <= y + 28) {
                    parent.drawHoveringText(Component.translatable("quest.hover.compass.settings").getFormattedText(), mouseX, mouseY);
                }
                if (mouseY > y + 28 && mouseY <= y + 56) {
                    parent.drawHoveringText(Component.translatable("key.quest.log").getFormattedText(), mouseX, mouseY);
                }
            }
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.enableBlend();
        }
        if (selectedTabIndex == 11) {
            ClientEventHandler.renderBalance(parent, mouseX, mouseY,
                    parent.getGuiLeft() + 125,
                    parent.getGuiTop() + 33);
        }
    }

    @Inject(method = "actionPerformed", at = @At("TAIL"))
    protected void npcs$actionPerformed(GuiButton button, CallbackInfo ci) {
        if (CustomNpcs.InventoryGuiEnabled) {
            if (button.id == 150) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeInt(2);
                CustomNpcs.proxy.openGui(null, EnumGuiType.QuestLog, buf);
            }
            else if (button.id == 151) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeInt(0);
                CustomNpcs.proxy.openGui(null, EnumGuiType.QuestLog, buf);
            }
        }
    }

}
