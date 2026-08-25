package noppes.npcs.client.layer.block;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.ILayerBlockModel;
import noppes.npcs.api.INbt;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.BlockWrapper;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class LayerBlockModel implements ILayerBlockModel {

    protected @Nonnull final TileScripted tile;
    // item
    protected ItemStackWrapper itemModel = ItemStackWrapper.AIR;
    // block
    protected BlockWrapper blockModel = BlockWrapper.AIR;
    // obg
    protected ResourceLocation objModel = null;
    protected List<String> objVisibleMeshes = new ArrayList<>();
    protected Map<String, ResourceLocation> objMaterialsReplase = new HashMap<>();
    // box
    protected AABB aabb = Shapes.block().bounds();

    protected float[] offsetAxis = new float[] { 0.0f, 0.0f, 0.0f };
    protected float[] scaleAxis = new float[] { 1.0f, 1.0f, 1.0f };
    protected float[] rotateAxis = new float[] { 0.0f, 0.0f, 0.0f };
    protected byte[] isRotate = new byte[] { (byte) 0, (byte) 0, (byte) 0 };
    protected int id = 0;
    protected int rotateSpeed = 1;

    public LayerBlockModel(@Nonnull TileScripted tileIn) { tile = tileIn; }

    public LayerBlockModel(int idIn, ItemStack stack, @Nonnull TileScripted tile) {
        this(tile);
        id = idIn;
        itemModel = (ItemStackWrapper) Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stack);
    }

    @Override
    public IItemStack getItemModel() { return itemModel; }

    @Override
    public IBlock getBlockModel() { return blockModel; }

    @Override
    public INbt getNbt() {
        CompoundTag nbtLayer = new CompoundTag();
        // models
        if (!itemModel.isEmpty()) {
            nbtLayer.put("ItemModel", itemModel.getMCItemStack().save(new CompoundTag()));
        }
        if (!blockModel.isEmpty()) {
            nbtLayer.put("BlockModel", blockModel.save());
        }
        // OBJ
        if (objModel != null) { nbtLayer.putString("OBJModel", objModel.toString()); }
        ListTag ovm = new ListTag();
        for (String mesh : objVisibleMeshes) { ovm.add(StringTag.valueOf(mesh)); }
        nbtLayer.put("OBJVisibleMeshes", ovm);
        ListTag omr = new ListTag();
        for (Map.Entry<String, ResourceLocation> entry : objMaterialsReplase.entrySet()) {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("K", entry.getKey());
            nbt.putString("V", entry.getValue().toString());
            ovm.add(nbt);
        }
        nbtLayer.put("OBJMaterialsReplase", omr);
        // box
        ListTag box = new ListTag();
        box.add(DoubleTag.valueOf(aabb.minX));
        box.add(DoubleTag.valueOf(aabb.minY));
        box.add(DoubleTag.valueOf(aabb.minZ));
        box.add(DoubleTag.valueOf(aabb.maxX));
        box.add(DoubleTag.valueOf(aabb.maxY));
        box.add(DoubleTag.valueOf(aabb.maxZ));
        nbtLayer.put("AABB", box);
        // rotate
        ListTag ra = new ListTag();
        for (float f : rotateAxis) { ra.add(FloatTag.valueOf(f)); }
        nbtLayer.put("RotateAxis", ra);
        // offset
        ListTag oa = new ListTag();
        for (float f : offsetAxis) { oa.add(FloatTag.valueOf(f)); }
        nbtLayer.put("OffsetAxis", oa);
        // scale
        ListTag sa = new ListTag();
        for (float f : scaleAxis) { sa.add(FloatTag.valueOf(f)); }
        nbtLayer.put("ScaleAxis", sa);
        // main
        nbtLayer.putByteArray("isRotate", isRotate);
        nbtLayer.putInt("Id", id);
        nbtLayer.putInt("Speed", rotateSpeed);
        return new NBTWrapper(nbtLayer);
    }

    @Override
    public void setNbt(INbt nbt) {
        CompoundTag nbtLayer = nbt.getMCNBT();
        // models
        if (nbtLayer.contains("ItemModel", 10)) {
            itemModel = (ItemStackWrapper) Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(ItemStack.of(nbtLayer.getCompound("ItemModel")));
        }
        if (nbtLayer.contains("BlockModel", 10)) {
            blockModel = BlockWrapper.of(nbt.getMCNBT().getCompound("BlockModel"));
        }
        // OBJ
        if (nbtLayer.contains("OBJModel", 8)) { objModel = new ResourceLocation(nbtLayer.getString("OBJModel")); }
        objVisibleMeshes.clear();
        ListTag ovm = nbtLayer.getList("OBJVisibleMeshes", 8);
        for (int i = 0; i < ovm.size(); i++) { objVisibleMeshes.add(ovm.getString(i)); }
        objMaterialsReplase.clear();
        ListTag omr = nbtLayer.getList("OBJVisibleMeshes", 10);
        for (int i = 0; i < omr.size(); i++) {
            CompoundTag mcNbt = omr.getCompound(i);
            objMaterialsReplase.put(mcNbt.getString("K"), new ResourceLocation(NoppesUtilServer.validLocation(mcNbt.getString("V"))));
        }
        // box
        ListTag box = nbtLayer.getList("AABB", 6);
        aabb = new AABB(!box.isEmpty() ? box.getDouble(0) : aabb.minX,
                box.size() > 1 ? box.getDouble(1) : aabb.minY,
                box.size() > 2 ? box.getDouble(2) : aabb.minZ,
                box.size() > 3 ? box.getDouble(3) : aabb.maxX,
                box.size() > 4 ? box.getDouble(4) : aabb.maxY,
                box.size() > 5 ? box.getDouble(5) : aabb.maxZ);
        // rotate
        if (nbtLayer.getList("RotateAxis", 5).size() == 3) {
            for (int i = 0; i < nbtLayer.getList("RotateAxis", 5).size(); i++) {
                rotateAxis[i] = nbtLayer.getList("RotateAxis", 5).getFloat(i);
            }
        }
        // offset
        if (nbtLayer.getList("OffsetAxis", 5).size() == 3) {
            for (int i = 0; i < nbtLayer.getList("OffsetAxis", 5).size(); i++) {
                offsetAxis[i] = nbtLayer.getList("OffsetAxis", 5).getFloat(i);
            }
        }
        // scale
        if (nbtLayer.getList("ScaleAxis", 5).size() == 3) {
            for (int i = 0; i < nbtLayer.getList("ScaleAxis", 5).size(); i++) {
                scaleAxis[i] = nbtLayer.getList("ScaleAxis", 5).getFloat(i);
            }
        }
        // main
        if (nbtLayer.getByteArray("isRotate").length == 3) { isRotate = nbtLayer.getByteArray("isRotate"); }
        id = nbtLayer.contains("Pos", 3) ? nbtLayer.getInt("Pos") : nbtLayer.getInt("Id");
        setRotateSpeed(nbtLayer.getInt("Speed"));
    }

    @Override
    public @Nullable String getOBJModel() { return objModel == null ? null : objModel.toString(); }

    @Override
    public List<String> getOBJVisibleMeshes() { return objVisibleMeshes; }

    @Override
    public Map<String, ResourceLocation> getOBJMaterialsReplase() { return objMaterialsReplase; }

    @Override
    public float getOffset(int axis) {
        if (axis < 0) { axis *= -1; }
        return offsetAxis[axis % 3];
    }

    @Override
    public int getId() { return id; }

    public void setId(int newId) {
        id = ValueUtil.onlyPositiveInt(newId, Integer.MAX_VALUE);
        tile.needsClientUpdate = true;
    }

    @Override
    public float getRotation(int axis) {
        if (axis < 0) { axis *= -1; }
        return rotateAxis[axis % 3];
    }

    @Override
    public int getRotateSpeed() { return rotateSpeed; }

    @Override
    public float getScale(int axis) {
        if (axis < 0) { axis *= -1; }
        return scaleAxis[axis % 3];
    }

    @Override
    public boolean isRotate(int axis) {
        if (axis < 0) { axis *= -1; }
        return isRotate[axis % 3] == (byte) 1;
    }

    @Override
    public void setIsRotate(boolean x, boolean y, boolean z) {
        isRotate[0] = x ? (byte) 1 : (byte) 0;
        isRotate[1] = y ? (byte) 1 : (byte) 0;
        isRotate[2] = z ? (byte) 1 : (byte) 0;
        tile.needsClientUpdate = true;
    }

    @Override
    public void setItemModel(IItemStack iStack) {
        itemModel = (ItemStackWrapper) iStack;
        blockModel = BlockWrapper.AIR;
        objModel = null;
        objVisibleMeshes = new ArrayList<>();
        objMaterialsReplase = new HashMap<>();
        tile.needsClientUpdate = true;
    }

    @Override
    public void setBlockModel(IBlock iBlock) {
        itemModel = ItemStackWrapper.AIR;
        blockModel = (BlockWrapper) iBlock;
        objModel = null;
        objVisibleMeshes = new ArrayList<>();
        objMaterialsReplase = new HashMap<>();
        tile.needsClientUpdate = true;
    }

    @Override
    public void setOBJModel(String path) {
        itemModel = ItemStackWrapper.AIR;
        blockModel = BlockWrapper.AIR;
        objModel = new ResourceLocation(NoppesUtilServer.validLocation(path));
        objVisibleMeshes = new ArrayList<>();
        objMaterialsReplase = new HashMap<>();
        tile.needsClientUpdate = true;
    }

    @Override
    public void setOBJModel(String path, List<String> meshes,  Map<String, ResourceLocation> materials) {
        itemModel = ItemStackWrapper.AIR;
        blockModel = BlockWrapper.AIR;
        objModel = new ResourceLocation(NoppesUtilServer.validLocation(path));
        objVisibleMeshes = meshes;
        objMaterialsReplase = materials;
        tile.needsClientUpdate = true;
    }

    @Override
    public void setOffset(float x, float y, float z) {
        offsetAxis[0] = x;
        offsetAxis[1] = y;
        offsetAxis[2] = z;
        tile.needsClientUpdate = true;
    }

    @Override
    public void setRotation(float x, float y, float z) {
        x %= 360.0f;
        y %= 360.0f;
        z %= 360.0f;
        if (x < 0.0f) { x += 360.0f; }
        if (y < 0.0f) { y += 360.0f; }
        if (z < 0.0f) { z += 360.0f; }
        rotateAxis[0] = ValueUtil.correctFloat(x, 0.0f, 359.9999f);
        rotateAxis[1] = ValueUtil.correctFloat(y, 0.0f, 359.9999f);
        rotateAxis[2] = ValueUtil.correctFloat(z, 0.0f, 359.9999f);
        tile.needsClientUpdate = true;
    }

    @Override
    public void setRotateSpeed(int speed) {
        rotateSpeed = ValueUtil.correctInt(speed, 1, 7);
        tile.needsClientUpdate = true;
    }

    @Override
    public void setScale(float x, float y, float z) {
        scaleAxis[0] = ValueUtil.correctFloat(x, -10.0f, 10.0f);
        scaleAxis[1] = ValueUtil.correctFloat(y, -10.0f, 10.0f);
        scaleAxis[2] = ValueUtil.correctFloat(z, -10.0f, 10.0f);
        tile.needsClientUpdate = true;
    }

    @Override
    public AABB getBoundingBox() { return aabb; }

    @Override
    public void setBoundingBox(AABB newAABB) {
        aabb = newAABB;
        tile.needsClientUpdate = true;
    }

    @Override
    public void setBoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        aabb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        tile.needsClientUpdate = true;
    }

}
