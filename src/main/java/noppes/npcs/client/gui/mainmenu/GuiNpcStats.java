package noppes.npcs.client.gui.mainmenu;

import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.EnumDifficulty;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.SubGuiNpcMeleeProperties;
import noppes.npcs.client.gui.SubGuiNpcProjectiles;
import noppes.npcs.client.gui.SubGuiNpcRangeProperties;
import noppes.npcs.client.gui.SubGuiNpcResistanceProperties;
import noppes.npcs.client.gui.SubGuiNpcRespawn;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataDisplay;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataStats;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuGet;
import noppes.npcs.packets.server.SPacketMenuSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class GuiNpcStats extends GuiNPCInterface2 implements ITextfieldListener, IGuiData {

	protected final DataStats stats;

	// New from Unofficial (BetaZavr)
	protected final DataAI ais;
	protected final DataDisplay display;
	protected final DataInventory inventory;

	public GuiNpcStats(EntityNPCInterface npc) {
		super(npc, 2);

		stats = npc.stats;
		display = npc.display;
		ais = npc.ais;
		inventory = npc.inventory;
		Packets.sendServer(new SPacketMenuGet(EnumMenuType.STATS));
		Packets.sendServer(new SPacketMenuGet(EnumMenuType.DISPLAY));
		Packets.sendServer(new SPacketMenuGet(EnumMenuType.AI));
		Packets.sendServer(new SPacketMenuGet(EnumMenuType.INVENTORY));
	}

	@Override
	public void initGui() {
		int level = stats.getLevel() - 1;
		int rarity = stats.getRarity();
		super.initGui();
		int lw = 78;
		int bw = 56;
		int x0 = guiLeft + 4;
		int x1 = x0 + lw + 2;
		int x2 = x1 + bw + 2;
		int x3 = x2 + lw + 2;
		int x4 = x3 + bw + 2;
		int x5 = x4 + lw + 2;
		int y = guiTop + 10;
		// health
		addLabel(0, x0, y + 5, "stats.health")
				.setSize(lw, 10);
		addTextField(0, x1 + 1, y, bw - 2, 18, (int) stats.maxHealth)
				.setMinMaxDefault(1, Integer.MAX_VALUE, 20)
				.setHoverTexts("stats.hover.max.health");
		// aggro
		addLabel(1, x2, y + 5, "stats.aggro")
				.setSize(lw, 10);
		addTextField(1, x3 + 1, y, bw - 2, 18, stats.aggroRange)
				.setMinMaxDefault(1, 512, 2)
				.setHoverTexts("stats.hover.aggro");
		// creature type
		addLabel(34, x4, y + 5, "stats.creaturetype")
				.setSize(lw, 10);
		addButton(8, x5, y, false, stats.getCreatureType(), "stats.normal", "stats.undead", "stats.arthropod")
				.setSize(bw, 20)
				.setHoverTexts("stats.hover.type");
		// respawn
		addLabel(2, x0, (y += 22) + 5, "stats.respawn");
		addButton(0, x1, y, "selectServer.edit")
				.setSize(bw, 20)
				.setHoverTexts("stats.hover.respawn");
		// melee properties
		Component hover = Component.translatable("stats.hover.melee");
		if (ais.aiDisabled) { hover.append(Component.translatable("hover.ai.disabled")); }
		else if (player.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
			hover.append(Component.translatable("guihint.npcmeleeprops.peaceful").withStyle(TextFormatting.RED));
		}
		addLabel(5, x0, (y += 22) + 5, "stats.meleeproperties")
				.setSize(lw, 10);
		addButton(2, x1, y, "selectServer.edit")
				.setSize(bw, 20)
				.setIsEnabled(!ais.aiDisabled)
				.setHoverTexts(hover);
		// ranged properties
		hover = Component.translatable("stats.hover.range");
		if (ais.aiDisabled) { hover.append(Component.translatable("hover.ai.disabled")); }
		else if (player.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
			hover.append(Component.translatable("guihint.npcrangedprops.peaceful").withStyle(TextFormatting.RED));
		}
		addLabel(6, x0, (y += 22) + 5, "stats.rangedproperties")
				.setSize(lw, 10);
		addButton(3, x1, y, "selectServer.edit")
				.setSize(bw, 20)
				.setIsEnabled(!ais.aiDisabled)
				.setHoverTexts(hover);
		// projectile properties
		hover = Component.translatable("stats.hover.arrow");
		if (ais.aiDisabled) { hover.append(Component.translatable("hover.ai.disabled")); }
		else if (player.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
			hover.append(Component.translatable("guihint.npcrangedprops.peaceful").withStyle(TextFormatting.RED));
		}
		addLabel(7, x2, y + 5, "stats.projectileproperties")
				.setSize(lw, 10);
		addButton(9, x3, y, "selectServer.edit")
				.setSize(bw, 20)
				.setIsEnabled(!ais.aiDisabled)
				.setHoverTexts(hover);
		// resistance
		addLabel(15, x0, (y += 34) + 5, MobEffects.RESISTANCE.getName())
				.setSize(lw, 10);
		addButton(15, x1, y, "selectServer.edit")
				.setSize(bw, 20)
				.setHoverTexts("stats.hover.resists");
		// fire immune
		addLabel(10, x0, (y += 34) + 5, "stats.fireimmune")
				.setSize(lw, 10);
		addYesNo(4, x1, y, npc.isImmuneToFire())
				.setSize(bw, 20)
				.setHoverTexts("stats.hover.resist.fire");
		// can drown
		addLabel(11, x2, y + 5, "stats.candrown")
				.setSize(lw, 10);
		addYesNo(5, x3, y, stats.canDrown)
				.setSize(bw, 20)
				.setHoverTexts("stats.hover.water");
		// regen health
		addLabel(14, x4, y + 5, "stats.regenhealth")
				.setSize(lw, 10);
		addTextField(14, x5 + 1, y, bw - 2, 20, stats.healthRegen)
				.setMinMaxDefault(0, Integer.MAX_VALUE, 1)
				.setHoverTexts("stats.hover.health.regen");
		// burn in sun
		addLabel(12, x0, (y += 22) + 5, "stats.burninsun")
				.setSize(lw, 10);
		addYesNo(6, x1, y, stats.burnInSun)
				.setSize(bw, 20)
				.setHoverTexts("stats.hover.resist.sun");
		// no fall damage
		addLabel(13, x2, y + 5, "stats.nofalldamage")
				.setSize(lw, 10);
		addYesNo(7, x3, y, stats.noFallDamage)
				.setSize(bw, 20)
				.setHoverTexts("stats.hover.fall");
		// combat regen
		addLabel(16, x4, y + 5, "stats.combatregen")
				.setSize(lw, 10);
		addTextField(16, x5 + 1, y, bw - 2, 20, stats.combatRegen)
				.setMinMaxDefault(0, Integer.MAX_VALUE, 0)
				.setHoverTexts("stats.hover.health.combat");
		// potion immune
		addLabel(17, x0, (y += 22) + 5, "stats.potionImmune")
				.setSize(lw, 10);
		addYesNo(17, x1, y, stats.potionImmune)
				.setSize(bw, 20)
				.setHoverTexts("stats.hover.potion");
		// cobweb affected
		addLabel(22, x2, y + 5, "ai.cobwebAffected")
				.setSize(lw, 10);
		addYesNo(22, x3, y, stats.ignoreCobweb)
				.setSize(bw, 20)
				.setHoverTexts("stats.hover.web");
		// New from Unofficial (BetaZavr)
		// level
		Object[] lvls = new Object[CustomNpcs.MaxLv];
		for (int g = 0; g < CustomNpcs.MaxLv; g++) { lvls[g] = "" + (g + 1); }
		addLabel(42, x2, guiTop + 37, "stats.level");
		addButton(41, x3, guiTop + 32, true, level, lvls)
				.setSize(56, 20)
				.setHoverTexts("stats.hover.level");
		// rarity
		addButton(43, x3, guiTop + 54, false, rarity, "stats.rarity.normal", "stats.rarity.elite", "stats.rarity.boss")
				.setSize(56, 20)
				.setHoverTexts("stats.hover.rarity");
		addLabel(44, x2, guiTop + 61, "stats.rarity");
		addLabel(45, x4, guiTop + 37, "stats.calmdown");
		addYesNo(44, x5, guiTop + 32, stats.calmdown)
				.setSize(56, 20)
				.setHoverTexts("stats.hover.battle");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: setSubGui(new SubGuiNpcRespawn(stats)); break;
			case 2: setSubGui(new SubGuiNpcMeleeProperties(stats.melee)); break;
			case 3: setSubGui(new SubGuiNpcRangeProperties(stats)); break;
			case 4: npc.setImmuneToFire(((GuiButtonYesNo) button).getBoolean()); break;
			case 5: stats.canDrown = ((GuiButtonYesNo) button).getBoolean(); break;
			case 6: stats.burnInSun = ((GuiButtonYesNo) button).getBoolean(); break;
			case 7: stats.noFallDamage = ((GuiButtonYesNo) button).getBoolean(); break;
			case 8: stats.setCreatureType(button.getValue()); break;
			case 9: setSubGui(new SubGuiNpcProjectiles(stats.ranged)); break;
			case 15: setSubGui(new SubGuiNpcResistanceProperties(npc, stats.resistances)); break;
			case 17: stats.potionImmune = ((GuiButtonYesNo) button).getBoolean(); break;
			case 22: stats.ignoreCobweb = ((GuiButtonYesNo) button).getBoolean(); break;
			// New from Unofficial (BetaZavr)
			case 40: save(); break;
			case 41: stats.setLevel(1 + button.getValue()); setBaseStats(); break;
			case 43: stats.setRarity(button.getValue()); setBaseStats(); break;
			case 44: stats.calmdown = (button.getValue() == 1); break;
		}
	}

	@Override
	public void save() {
		Packets.sendServer(new SPacketMenuSave(EnumMenuType.STATS, stats.save(new NBTTagCompound())));
		Packets.sendServer(new SPacketMenuSave(EnumMenuType.DISPLAY, display.save(new NBTTagCompound())));
		Packets.sendServer(new SPacketMenuSave(EnumMenuType.AI, ais.save(new NBTTagCompound())));
		Packets.sendServer(new SPacketMenuSave(EnumMenuType.INVENTORY, inventory.save(new NBTTagCompound())));
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		if (compound.hasKey("CreatureType", 3)) { stats.load(compound); }
		else if (compound.hasKey("MarkovGeneratorId", 3)) { display.load(compound); }
		else if (compound.hasKey("NpcInv", 9)) { inventory.load(compound); }
		else if (compound.hasKey("MovementType", 3)) { ais.load(compound); }
		initGui();
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 0: stats.maxHealth = textField.getInteger(); npc.heal((float) stats.maxHealth); break;
			case 1: stats.aggroRange = textField.getInteger(); break;
			case 14: stats.healthRegen = textField.getInteger(); break;
			case 16: stats.combatRegen = textField.getInteger(); break;
		}
	}

	// New from Unofficial (BetaZavr)
	public double getHP() {
		int[] corr = CustomNpcs.HealthNormal;
		if (stats.getRarity() == 1) { corr = CustomNpcs.HealthElite; }
		else if (stats.getRarity() == 2) { corr = CustomNpcs.HealthBoss; }
		double a = ((double) corr[0] - (double) corr[1]) / (1 - Math.pow(CustomNpcs.MaxLv, 2));
		double b = (double) corr[0] - a;
		double hp = Math.round(a * Math.pow(stats.getLevel(), 2) + b);
		if (hp <= 1.0d) { hp = 1.0d; }
		if (hp > 10000) { hp = Math.ceil(hp / 100.0d) * 100.0d; }
		else if (hp > 1000) { hp = Math.ceil(hp / 25.0d) * 25.0d; }
		else if (hp > 100) { hp = Math.ceil(hp / 10.0d) * 10.0d; }
		else if (hp > 50) { hp = Math.ceil(hp / 5.0d) * 5.0d; }
		else { hp = Math.ceil(hp); }
		if (hp > (double) corr[1]) { hp = corr[1]; }
		return hp;
	}

	public int getMellePower() {
		int[] corr = CustomNpcs.DamageNormal;
		if (stats.getRarity() == 1) { corr = CustomNpcs.DamageElite; }
		else if (stats.getRarity() == 2) { corr = CustomNpcs.DamageBoss; }
		double a = ((double) corr[0] - (double) corr[1]) / (1 - Math.pow(CustomNpcs.MaxLv, 2));
		double b = (double) corr[0] - a;
		return (int) Math.round(a * Math.pow(stats.getLevel(), 2) + b);
	}

	public int getRangePower() {
		int[] corr = CustomNpcs.DamageNormal;
		if (stats.getRarity() == 1) { corr = CustomNpcs.DamageElite; }
		else if (stats.getRarity() == 2) { corr = CustomNpcs.DamageBoss; }
		double a = ((double) corr[2] - (double) corr[3]) / (1 - Math.pow(CustomNpcs.MaxLv, 2));
		double b = (double) corr[2] - a;
		return (int) Math.round(a * Math.pow(stats.getLevel(), 2) + b);
	}

	public int[] getXP() {
		float[] corr = new float[] { (float) CustomNpcs.Experience[0], (float) CustomNpcs.Experience[1],
				(float) CustomNpcs.Experience[2], (float) CustomNpcs.Experience[3] };
		int lv = stats.getLevel();
		if (stats.getRarity() == 1) {
			corr[0] *= 1.75f;
			corr[1] *= 1.75f;
			corr[2] *= 1.75f;
			corr[3] *= 1.75f;
		}
		else if (stats.getRarity() == 2) {
			corr[0] *= 4.75f;
			corr[1] *= 4.75f;
			corr[2] *= 4.75f;
			corr[3] *= 4.75f;
		}
		int subMinLv = CustomNpcs.MaxLv / 3;
		int subMaxLv = CustomNpcs.MaxLv * 2 / 3;
		float subMinXP = corr[1] / 3.0f;
		float subMaxXP = corr[1] * 2.0f / 3.0f;
		float subMinXPM = corr[3] / 3.0f;
		float subMaxXPM = corr[3] * 2.0f / 3.0f;
		double a = ((subMaxXP - corr[1]) * (1 - subMinLv) - (corr[0] - subMinXP) * (subMaxLv - CustomNpcs.MaxLv))
				/ ((subMaxLv - CustomNpcs.MaxLv) * (Math.pow(subMinLv, 2) - 1)
				- (1 - subMinLv) * (Math.pow(CustomNpcs.MaxLv, 2) - Math.pow(subMaxLv, 2)));
		double b = (corr[0] - subMinXP + a * (Math.pow(subMinLv, 2) - 1)) / (1 - subMinLv);
		double c = corr[0] - a - b;
		int min = (int) (Math.pow(lv, 2) * a + lv * b + c);
		a = ((subMaxXPM - corr[3]) * (1 - subMinLv) - (corr[2] - subMinXPM) * (subMaxLv - CustomNpcs.MaxLv))
				/ ((subMaxLv - CustomNpcs.MaxLv) * (Math.pow(subMinLv, 2) - 1)
				- (1 - subMinLv) * (Math.pow(CustomNpcs.MaxLv, 2) - Math.pow(subMaxLv, 2)));
		b = (corr[2] - subMinXPM + a * (Math.pow(subMinLv, 2) - 1)) / (1 - subMinLv);
		c = corr[2] - a - b;
		int max = (int) (Math.pow(lv, 2) * a + lv * b + c);
		return new int[] { min, max };
	}

	private void setBaseStats() {
		int lv = stats.getLevel();
		int type = stats.getRarity();
		// Resistance and model size
		if (!CustomNpcs.RecalculateLR) { return; }
		int[] sizeModel = CustomNpcs.ModelRaritySize;
		int time = 180;
		float[] resist = new float[] { (float) CustomNpcs.ResistanceNormal[0] / 100.0f,
				(float) CustomNpcs.ResistanceNormal[1] / 100.0f, (float) CustomNpcs.ResistanceNormal[2] / 100.0f,
				(float) CustomNpcs.ResistanceNormal[3] / 100.0f };
		if (type == 2) {
			resist = new float[] { (float) CustomNpcs.ResistanceBoss[0] / 100.0f,
					(float) CustomNpcs.ResistanceBoss[1] / 100.0f, (float) CustomNpcs.ResistanceBoss[2] / 100.0f,
					(float) CustomNpcs.ResistanceBoss[3] / 100.0f };
		} else if (type == 1) {
			resist = new float[] { (float) CustomNpcs.ResistanceElite[0] / 100.0f,
					(float) CustomNpcs.ResistanceElite[1] / 100.0f, (float) CustomNpcs.ResistanceElite[2] / 100.0f,
					(float) CustomNpcs.ResistanceElite[3] / 100.0f };
			if (lv <= 30) { time = (int) (300.0d + (Math.round(((double) lv + 5.5d) / 10.0d) - 1.0d) * 60.0d); }
			else { time = 480; }
		} else {
			if (lv <= 30) { time = (int) (90.0d + (Math.round(((double) lv + 5.5d) / 10.0d) - 1.0d) * 30.0d); }
		}
		display.setSize(sizeModel[type]);
		stats.respawnTime = time;
		stats.resistances.data.put("mob", resist[0]);
		stats.resistances.data.put("arrow", resist[1]);
		stats.resistances.data.put("explosion", resist[2]);
		stats.resistances.data.put("knockback", resist[3]);
		// Health
		double hp = getHP();
		stats.maxHealth = (int) hp;
		npc.setHealth((float) hp);
		stats.setHealthRegen((int) (Math.ceil(hp / 50.0d) * 10.0d));
		// Power
		stats.melee.setStrength(getMellePower());
		stats.ranged.setStrength(getRangePower());
		stats.ranged.setAccuracy((int) Math.round((lv + 94.3333d) / 1.4667d));
		// Speed
		double sp = Math.round((0.000081d * Math.pow(lv, 2) - 0.07272d * lv + 22.072639d));
		stats.melee.setDelay((int) sp);
		stats.ranged.setDelay((int) sp, (int) sp * 2);
		ais.setWalkingSpeed((int) Math.ceil((sp - 7) / 3));
		// Experience
		int[] xp = getXP();
		inventory.setExp(xp[0], xp[1]);
		// renaming
		String rarity = "";
		if (type == 2) { rarity += ((char) 167) + "4Boss "; }
		else if (type == 1) { rarity += ((char) 167) + "6Elite "; }
		rarity += ((char) 167) + "7lv." + ((char) 167) + (lv <= CustomNpcs.MaxLv / 3 ? "2" : (float) lv <= (float) CustomNpcs.MaxLv / 1.5f ? "6" : "4") + lv;
		stats.setRarityTitle(rarity);
		initGui();
	}

}
