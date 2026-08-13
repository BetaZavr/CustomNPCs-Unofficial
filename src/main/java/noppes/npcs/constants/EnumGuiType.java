package noppes.npcs.constants;

import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;

public enum EnumGuiType
{

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
	ManageRecipes(true), // BlockPos (x = size)
	ManageLinked,
	PlayerFollowerHire(true),
	PlayerFollower(true),
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
	QuestTypeItem,
	MoneyBag,
	CustomChest(true),
	DimensionSetting(true),
	CustomContainer(true),
	DeadInventory(true),
	QuestLog,
	QuestCompleteText,
	QuestChooseReward,
	CreationParts,
	ManageCustomElements;

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
