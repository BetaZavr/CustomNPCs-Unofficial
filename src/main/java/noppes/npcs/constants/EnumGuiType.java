package noppes.npcs.constants;

import net.minecraft.resources.ResourceLocation;
import noppes.npcs.CustomNpcs;

public enum EnumGuiType {

   MainMenuDisplay,
   MainMenuInv(true),
   MainMenuStats,
   ManageFactions,
   MainMenuAdvanced,
   MainMenuGlobal,
   MainMenuAI,
   ManageTransport(true),
   ManageBanks(true),
   ManageDialogs,
   ManageQuests,
   ManageRecipes(true),
   ManageLinked,
   PlayerFollower(true),
   PlayerFollowerHire(true),
   PlayerBank(true),
   PlayerMailbox,
   PlayerMailOpen(true),
   PlayerTrader(true), // int marcetId
   PlayerAnvil(true),
   SetupItemGiver(true),
   SetupTrader(true),
   SetupTraderDeal(true),
   SetupFollower(true),
   SetupDrop(true),
   PlayerTransporter,
   RedstoneBlock,
   SetupTransporter,
   MobSpawner,
   SetupBank(true),
   NpcRemote,
   MovingPath,
   MobSpawnerAdd,
   Waypoint,
   MerchantAdd(true),
   MobSpawnerMounter,
   NpcDimensions,
   Border,
   Portal,
   Script,
   ScriptBlock,
   ScriptDoor,
   Companion,
   CompanionInv(true),
   CompanionTalent,
   CompanionTrader,
   BuilderBlock,
   CopyBlock,
   ScriptPlayers,
   ScriptItem,
   NbtBook,
   CustomGui(true),
   // New from Unofficial (BetaZavr)
   ManageMail,
   ManageGame,
   AvailabilityStack(true),
   EditClientScript,
   PermissionsEdit,
   BoundarySetting, // BlockPos
   BuilderTool(true), // npc id; BlockPos(buildId, type, 0)
   RemoverTool(true), // npc id; BlockPos(buildId, type, 0)
   ReplaceTool(true), // npc id; BlockPos(buildId, type, 0)
   PlacerTool, // BlockPos(buildId, type, 0)
   SaverTool, // BlockPos(buildId, type, 0)
   QuestTypeItem(true),
   MoneyBag,
   DimensionSetting(true),
   DeadInventory(true),
   QuestLog,
   QuestCompleteText,
   QuestChooseReward,
   CreationParts,
   CustomChest(true),
   ManageCustomElements,
   ManageDungeons;

   public boolean hasContainer;
   public final ResourceLocation resource;

   EnumGuiType() {
      hasContainer = false;
      resource = new ResourceLocation(CustomNpcs.MODID, "gui" + ordinal());
   }

   EnumGuiType(boolean hasContainerIn) {
      this();
      hasContainer = hasContainerIn;
   }

   @SuppressWarnings("unused")
   public static EnumGuiType getEnum(ResourceLocation location) {
      for (EnumGuiType type : values()) {
         if (type.resource.equals(location)) { return type; }
      }
      return null;
   }

}
