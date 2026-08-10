package noppes.npcs.roles.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.INbt;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.data.role.IJobSpawner;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class JobSpawnerCloneData implements IJobSpawner.IJobSpawnerData {

    protected final @Nonnull EntityNPCInterface parent;
    protected int count = 1;
    protected int tab = 0;
    protected String name = "";
    protected Component title = null;

    public JobSpawnerCloneData(@Nonnull EntityNPCInterface npc) { parent = npc; }

    @Override
    public Component getTitle() {
        if (title == null) {
            CompoundTag compound = ServerCloneController.Instance.getCloneData(null, name, tab);
            if (compound != null) {
                ServerCloneController.Instance.cleanTags(compound);
                Optional<Entity> entityO = EntityType.create(compound, parent.level());
                title = entityO.map(Entity::getName).orElse(Component.literal(compound.getString("id")));
            }
            else { title = Component.literal("NotFound"); }
        }
        return title;
    }

    @Override
    public int getCount() { return count; }

    @Override
    public void setCount(int countIn) { count = ValueUtil.correctInt(countIn, 1, 7); }

    @Override
    public INbt getNbt() { return new NBTWrapper(save()); }

    @Override
    public IEntity<?> getEntity() {
        CompoundTag compound = ServerCloneController.Instance.getCloneData(null, name, tab);
        if (compound != null) {
            ServerCloneController.Instance.cleanTags(compound);
            Optional<Entity> entityO = EntityType.create(compound, parent.level());
            return entityO.map(entity -> Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity)).orElse(null);
        }
        return null;
    }

    @Override
    public boolean isValid() { return ServerCloneController.Instance.getCloneData(null, name, tab) != null; }

    @Override
    public void setNbt(INbt nbt) {
        if (nbt != null) { load(nbt.getMCNBT()); }
    }

    public void load(@Nonnull CompoundTag nbt) {
        tab = nbt.getInt("tab");
        name = nbt.getString("name");
    }

    public @Nonnull CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("tab", tab);
        nbt.putString("name", name);
        return nbt;
    }

    @Override
    public String toString() { return "JobSpawnerCloneData{ Name: \"" + getTitle().getString() + "\", tab: " + tab + ", name: " + name + "}"; }

    public String getName() { return name; }

    public void setName(String newName) {
        String checkName = Util.instance.sanitizeFilename(newName);
        if (!checkName.equals(newName)) { throw new CustomNPCsException("Name must contain characters allowed for the file"); }
        name = checkName;
    }

    public int getTab() { return tab; }

    public void setTab(int activeTab) {
        if (activeTab < 0 || activeTab > 9) { throw new CustomNPCsException("Tabs 0 to 9 are available."); }
        tab = activeTab;
    }

}
