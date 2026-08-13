package noppes.npcs.packets.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.BankController;
import noppes.npcs.controllers.data.Bank;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketBasic;

public class PacketBankSave extends PacketBasic {

    protected static int channelId;
    private final CompoundTag data;

    public PacketBankSave(CompoundTag dataIn) { data = dataIn; }

    public static void encode(PacketBankSave msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static PacketBankSave decode(FriendlyByteBuf buf) { return new PacketBankSave(buf.readAnySizeNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        int id = data.getInt("BankID");
        if (id >= 0) {
            Bank bank = BankController.getInstance().getBank(id);
            if (bank == null) { bank = BankController.getInstance().addNewBank(); }
            bank.load(data);
            PlayerData.get(player).bankData.lastBank = null;
        }
        CustomNpcs.debugData.end("Packets");
    }

}