package noppes.npcs.api.constants;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleBank;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleDialog;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.roles.RoleInterface;
import noppes.npcs.roles.RolePostman;
import noppes.npcs.roles.RoleTrader;
import noppes.npcs.roles.RoleTransporter;

public enum RoleType {

	NONE("none", 0, false),
	TRADER("trader", 1, true),
	FOLLOWER("mercenary", 2, true),
	BANK("bank", 3, true),
	TRANSPORTER("transporter", 4, true),
	MAILMAN("mailman", 5, false),
	COMPANION("companion", 6, true),
	DIALOG("dialog", 7, true);

	public static RoleType get(int id) {
		for (RoleType er : RoleType.values()) {
			if (er.type == id) {
				return er;
			}
		}
		return RoleType.NONE;
	}

	public static Object[] getNames() {
		List<Component> list = new ArrayList<>();
		for (RoleType er : RoleType.values()) {
			if (er == COMPANION) { list.add(Component.from(er.name.createCopy()).append(" (WIP)")); }
			else { list.add(er.name); }
		}
		return list.toArray(new Component[0]);
	}

	private final int type;
	public final Component name;
	public final boolean hasSettings;

	RoleType(String named, int t, boolean hasSet) {
		type = t;
		name = Component.translatable("role." + named);
		hasSettings = hasSet;
	}

	public int get() {
		return this.type;
	}

	public void setToNpc(EntityNPCInterface npc) {
		switch (this) {
			case NONE: npc.role = new RoleInterface(npc); break;
			case TRADER: npc.role = new RoleTrader(npc); break;
			case FOLLOWER: npc.role = new RoleFollower(npc); break;
			case BANK: npc.role = new RoleBank(npc); break;
			case TRANSPORTER: npc.role = new RoleTransporter(npc); break;
			case MAILMAN: npc.role = new RolePostman(npc); break;
			case COMPANION: npc.role = new RoleCompanion(npc); break;
			case DIALOG: npc.role = new RoleDialog(npc); break;
		}
	}

}
