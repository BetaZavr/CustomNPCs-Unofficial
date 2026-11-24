package noppes.npcs.client.gui.player;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.containers.ContainerNPCFollowerHire;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketFollowerHire;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.util.Util;

import java.util.ArrayList;
import java.util.List;

public class GuiNpcFollowerHire extends GuiContainerNPCInterface<ContainerNPCFollowerHire> {

	protected final RoleFollower role;

	// New from Unofficial (BetaZavr)
	protected static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/components.png");
	protected GuiLabel moneyL;

	public GuiNpcFollowerHire(EntityNPCInterface npc, ContainerNPCFollowerHire cont) {
		super(npc, cont, Component.empty());
		setBackground("followerhire.png");
		closeOnEsc = true;

		role = (RoleFollower) npc.role;
	}

	@Override
	public void initGui() {
		super.initGui();
		int lId = 0;
		int x = guiLeft + 26;
		int y = guiTop - 7;
		for (int slot = 0; slot < 3; ++slot) {
			if (!role.rentalItems.getStackInSlot(slot).isEmpty()) {
				addButton(slot, x, y += 18, "follower.hire")
						.setSize(54, 14);
				int days = role.rates.get(slot);
				addLabel(lId++, x + 79, y + 2, Component.literal("= " + days + " ")
						.append(Component.translatable(days == 1 ? "follower.day": "follower.days")))
						.setSize(66, 10)
						.setIsVisible(!role.infiniteDays)
						.setHoverTexts(Component.translatable("gui.time")
								.append(" ≈ ")
								.append(Util.instance.ticksToElapsedTime(days * 28800L, false, true, false)));
			}
		}
		if (role.rates.containsKey(3) && role.rentalMoney > 0) {
			addButton(3, x, y += 18, "follower.hire")
					.setSize(54, 14);
			moneyL = addLabel(lId++, x + 56, y + 2, Component.literal(Util.instance.getTextReducedNumber(role.rentalMoney, true, true, false))
					.append(role.rentalIsDonat ? CustomNpcs.displayCurrencies : CustomNpcs.displayDonation));
			int days = role.rates.get(3);
			addLabel(lId, moneyL.getX() + moneyL.getWidth() + 14, y + 2, Component.literal("= " + days + " ")
					.append(Component.translatable(days == 1 ? "follower.day": "follower.days")))
					.setSize(66 - moneyL.getWidth(), 10)
					.setIsVisible(!role.infiniteDays)
					.setHoverTexts(Component.translatable("gui.time")
							.append(" ≈ ")
							.append(Util.instance.ticksToElapsedTime(days * 28800L, false, true, false)));
		}
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		Packets.sendServer(new SPacketFollowerHire(button.id));
		onClose();
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		mc.getTextureManager().bindTexture(GuiBasic.RESOURCE_SLOT);
		for (int slot = 0; slot < role.rentalItems.getSizeInventory(); ++slot) {
			ItemStack itemstack = role.rentalItems.getStackInSlot(slot);
			GuiButtonNop button = getButton(slot);
			if (!NoppesUtilServer.isItemStackNull(itemstack) && button != null) {
				int x = button.getX() + button.getWidth() + 3;
				int y = button.getY() - 1;
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				drawTexturedModalRect(x - 1, y - 1, 0, 0, 18, 18);
				GuiBasic.renderStackInGUI(itemstack, x, y, 0.0f, 1.0f, null);
				if (isMouseHover(mouseX, mouseY, x, y, 16, 16)) {
					renderToolTip(itemstack, mouseX, mouseY);
					RenderHelper.enableGUIStandardItemLighting();
				}
			}
		}
		if (role.rates.containsKey(3) && role.rentalMoney > 0) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(moneyL.getX() + moneyL.getWidth() - 3.0f, moneyL.getY() - 2.0f, 0.0f);
			mc.getTextureManager().bindTexture(role.rentalIsDonat ? GuiBasic.DONAT : GuiBasic.MONEY);
			RenderHelper.enableGUIStandardItemLighting();
			float sc = 16.0f / 250.f;
			GlStateManager.scale(sc, sc, sc);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			drawTexturedModalRect(0, 0, 0, 0, 256, 256);
			GlStateManager.popMatrix();
		}
		// stats
		mc.getTextureManager().bindTexture(resource);
		GlStateManager.pushMatrix();
		RenderHelper.enableGUIStandardItemLighting();
		GlStateManager.translate(guiLeft + 8.0f, guiTop + 12.0f, 0.0f);
		GlStateManager.scale(0.5f, 0.5f, 0.5f);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		drawTexturedModalRect(0, 0, 98, 48, 24, 24);
		GlStateManager.popMatrix();
	}

    @Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		for (int i = 0; i < 3; ++i) {
			if (getButton(i) != null) {
				getButton(i).setIsEnabled(player.isCreative() || Util.instance.canRemoveItems(player.inventory.mainInventory, role.rentalItems.getStackInSlot(i), false, false));
			}
		}
		if (getButton(3) != null) {
			getButton(3).setIsEnabled(player.isCreative() || PlayerData.get(player).game.getMoney() >= role.rentalMoney);
		}
		for (int i = 0; i < 4; ++i) {
			if (getButton(i) != null && getButton(i).isHoveredOrFocused()) {
				List<Component> hover = new ArrayList<>();
				hover.add(Component.translatable("follower.hover.hire.info"));
				if (role.disableGui) { hover.add(Component.translatable("follower.hover.disable.gui").withStyle(TextFormatting.GRAY)); }
				if (role.infiniteDays) { hover.add(Component.translatable("follower.hover.infinite")); }
				setHoverText(hover);
			}
		}
		// stats
		if (isMouseHover(mouseX, mouseY, guiLeft + 8, guiTop + 12, 12, 12)) {
			setHoverText(Component.translatable("stats.health").append(": " + (int) npc.getMaxHealth()),
					Component.translatable("stats.hover.attack.strength").append(": "),
					Component.literal(" - ").append(Component.translatable("stats.meleeproperties")).append(": " + npc.stats.getMelee().getStrength()),
					Component.literal(" - ").append(Component.translatable("stats.rangedproperties")).append(": " + npc.stats.getRanged().getStrength()),
					Component.translatable("stats.movespeed").append(": " + npc.ais.getWalkingSpeed()));
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

}
