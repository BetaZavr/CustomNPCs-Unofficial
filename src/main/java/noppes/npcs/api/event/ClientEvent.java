package noppes.npcs.api.event;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Cancelable;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;

@OnlyIn(Dist.CLIENT)
public class ClientEvent extends CustomNPCsEvent {

    public EntityNPCInterface npc;
    public Screen returnGui;

    public ClientEvent(EntityNPCInterface npcIn, Screen returnGuiIn) {
        super();
        npc = npcIn;
        returnGui = returnGuiIn;
    }

    @Cancelable
    public static class PreGetGuiCustomNpcs extends ClientEvent {

        public EnumGuiType guiType;
        public FriendlyByteBuf buffer;

        public PreGetGuiCustomNpcs(EntityNPCInterface npc, EnumGuiType gui, FriendlyByteBuf bufIn) {
            super(npc, null);
            guiType = gui;
            buffer = bufIn;
        }

    }

    @Cancelable
    public static class PostGetGuiCustomNpcs extends ClientEvent {

        public EnumGuiType guiType;
        public FriendlyByteBuf buffer;

        public PostGetGuiCustomNpcs(EntityNPCInterface npc, EnumGuiType gui, FriendlyByteBuf bufIn, Screen returnGuiIn) {
            super(npc, returnGuiIn);
            guiType = gui;
            buffer = bufIn;
        }

    }

    @Cancelable
    public static class NextToGuiCustomNpcs extends ClientEvent {

        public Screen parent;

        public NextToGuiCustomNpcs(EntityNPCInterface npc, Screen parentIn, Screen returnGuiIn) {
            super(npc, returnGuiIn);
            parent = parentIn;
        }

    }

    @Cancelable
    public static class SubGuiCustomNpcs extends ClientEvent {

        public Screen oldSubGui;

        public SubGuiCustomNpcs(EntityNPCInterface npc, Screen newSubGuiIn, Screen oldSubGuiIn) {
            super(npc, newSubGuiIn);
            oldSubGui = oldSubGuiIn;
        }

    }

}
