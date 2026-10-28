package noppes.npcs.api.constants;

import net.minecraft.network.chat.Component;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.*;

import java.util.ArrayList;
import java.util.List;

public enum RoleType {

   NONE("none", 0, false),
   TRADER("trader", 1, true),
   FOLLOWER("mercenary", 2, true),
   BANK("bank", 3, true),
   TRANSPORTER("transporter", 4, true),
   MAILMAN("mailman", 5, false),
   COMPANION("companion", 6, true),
   DIALOG("dialog", 7, true),
   AUCTIONEER("auctioneer", 8, true);

   public static RoleType get(int id) {
      for (RoleType er : RoleType.values()) {
         if (er.type == id) { return er; }
      }
      return RoleType.NONE;
   }

   public static Object[] getNames() {
      List<Component> list = new ArrayList<>();
      for (RoleType er : RoleType.values()) {
         if (er == COMPANION) { list.add(er.name.copy().append(" (WIP)")); }
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

   public int get() { return type; }

   public void setToNpc(EntityNPCInterface npc) {
      npc.role = switch (this) {
         case TRADER -> new RoleTrader(npc);
         case FOLLOWER -> new RoleFollower(npc);
         case BANK -> new RoleBank(npc);
         case TRANSPORTER -> new RoleTransporter(npc);
         case MAILMAN -> new RolePostman(npc);
         case COMPANION -> new RoleCompanion(npc);
         case DIALOG -> new RoleDialog(npc);
         case AUCTIONEER -> new RoleAuctioneer(npc);
         default -> RoleInterface.NONE;
      };
   }

}
