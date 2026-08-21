package noppes.npcs.controllers.data;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.INbt;
import noppes.npcs.api.handler.data.IKeySetting;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.controllers.KeyController;
import noppes.npcs.mixin.client.IKeyMappingMixin;

public class KeyConfig implements IKeySetting {

    private Object parent;
    private int id;
    public String name = "key.custom.name";
    public String category = "key.custom.category";
    public int keyId = InputConstants.KEY_Z;
    public int modifer = 2; // 0-none, 1-Shift, 2-Ctrl, 3-Alt

    public KeyConfig(int idIn) {
        if (idIn < 0) { idIn *= -1; }
        id = idIn;
    }

    public void load(CompoundTag nbtKey) {
        name = nbtKey.getString("Name");
        category = nbtKey.getString("Category");
        id = nbtKey.getInt("ID");
        keyId = nbtKey.getInt("KeyID");
        if (keyId < 0) { keyId *= -1; }
        if (keyId < InputConstants.KEY_1 ||
                keyId == InputConstants.KEY_RCONTROL || keyId == InputConstants.KEY_LCONTROL ||
                keyId == InputConstants.KEY_RSHIFT || keyId == InputConstants.KEY_LSHIFT ||
                keyId == InputConstants.KEY_RALT || keyId == InputConstants.KEY_LALT) {
            keyId = InputConstants.KEY_Z;
        }
        modifer = nbtKey.getInt("ModiferType") % 4;
        if (modifer < 0) { modifer *= -1; }
    }

    public CompoundTag save() {
        CompoundTag nbtKey = new CompoundTag();
        nbtKey.putString("Name", name);
        nbtKey.putString("Category", category);
        nbtKey.putInt("KeyID", keyId);
        nbtKey.putInt("ModiferType", modifer);
        nbtKey.putInt("ID", id);
        return nbtKey;
    }

    @OnlyIn(Dist.CLIENT)
    public Object getMCKeyBinding() {
        if (parent == null) {
            try {
                Class<?> cls = Class.forName("net.minecraft.client.KeyMapping");
                parent = cls.getConstructor(String.class, int.class, String.class).newInstance("", 0, "");
            }
            catch (Exception ignored) { }
        }
        if (parent != null) {
            String oldName = ((IKeyMappingMixin) parent).getName();
            InputConstants.Key oldKey = ((IKeyMappingMixin) parent).getKey();
            if (oldKey.getValue() != keyId) {
                ClientProxy.removeKeyFromMAP(parent);
                InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyId);
                ((IKeyMappingMixin) parent).setKey(key);
                ((IKeyMappingMixin) parent).setDefaultKey(key);
            }
            ((IKeyMappingMixin) parent).setName(name);
            ((IKeyMappingMixin) parent).setCategory(category);
            if (!oldName.equals(name)) {
                IKeyMappingMixin.getAll().remove(oldName);
                ClientProxy.addKeyToAll(name, parent);
            }
            ClientProxy.tryAddKeyToMap(parent);
            IKeyMappingMixin.getCategories().add(category);
        }
        return parent;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KeyConfig key)) { return false; }
        if (obj == this) { return true; }
        if (id != key.id || keyId != key.keyId || modifer != key.modifer) { return false; }
        return name.equals(key.name) && category.equals(key.category);
    }

    @Override
    public String getCategory() { return category; }

    @Override
    public int getId() { return id; }

    @Override
    public int getKeyId() { return keyId; }

    @Override
    public int getModiferType() { return modifer; }

    @Override
    public String getName() { return name; }

    @Override
    public INbt getNbt() { return new NBTWrapper(save()); }

    public boolean isActive(int key, List<Integer> keyPress) {
        if (keyId != key) { return false; }
        // 0-none, 1-Shift, 2-Ctrl, 3-Alt
        return switch (modifer) {
            case 1 -> keyPress.contains(InputConstants.KEY_LSHIFT) || keyPress.contains(InputConstants.KEY_RSHIFT);
            case 2 -> keyPress.contains(InputConstants.KEY_LCONTROL) || keyPress.contains(InputConstants.KEY_RCONTROL);
            case 3 -> keyPress.contains(InputConstants.KEY_LALT) || keyPress.contains(InputConstants.KEY_RALT);
            default -> true;
        };
    }

    @Override
    public void setCategory(String name) {
        if (name == null || name.isEmpty()) { name = "key.custom.category"; }
        category = name;
        KeyController.getInstance().update(id);
    }

    @Override
    public void setKeyId(int keyIdIn) {
        if (keyIdIn < InputConstants.KEY_1) {
            throw new CustomNPCsException("Key ID:" + keyIdIn + " must be greater than " + InputConstants.KEY_1);
        }
        if (keyIdIn == InputConstants.KEY_RCONTROL || keyIdIn == InputConstants.KEY_LCONTROL ||
                keyIdIn == InputConstants.KEY_RSHIFT || keyIdIn == InputConstants.KEY_LSHIFT ||
                keyIdIn == InputConstants.KEY_RALT || keyIdIn == InputConstants.KEY_LALT) {
            throw new CustomNPCsException("Key ID:" + keyIdIn + " cannot be of type Ctrl, Alt or Shift");
        }
        keyId = keyIdIn;
        KeyController.getInstance().update(id);
    }

    @Override
    public void setModiferType(int type) {
        if (type < 0 || type > 3) {
            throw new CustomNPCsException("Modifer Type must be between 0 and 3");
        }
        modifer = type;
    }

    @Override
    public void setName(String nameIn) {
        if (nameIn == null || nameIn.isEmpty()) { nameIn = "key.custom.name"; }
        name = nameIn;
        KeyController.getInstance().update(id);
    }

    @Override
    public void setNbt(INbt nbt) { load(nbt.getMCNBT()); }

    @Override
    public String toString() {
        return "KeyConfig { ID: " + id + "; keyID: " + keyId + "; modiferType: " + modifer + ", name: \""
                + name + "\"; category: \"" + category + "\"}";
    }

}
