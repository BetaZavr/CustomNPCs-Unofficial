package noppes.npcs.client.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.resources.IResource;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.relauncher.Side;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.model.ModelBipedArmorOBJ;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.items.custom.CustomArmorOBJ;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class CustomArmorOBJData {

    protected static Map<EntityEquipmentSlot, ModelBipedArmorOBJ> ARMOR_MODELS = new HashMap<>();

    protected final ImmutableMap<EntityEquipmentSlot, List<String>> visibleMeshesData;
    protected final ImmutableMap<EntityEquipmentSlot, ParameterizedModel> stackModelData; // default models
    protected final Map<EnumParts, ParameterizedModel> partModelData; // default models
    protected final Map<String, ParameterizedModel> partModelCash = new HashMap<>(); // custom models
    protected final ImmutableMap<EntityEquipmentSlot, Map<ItemCameraTransforms.TransformType, IModelState>> cameraData;
    protected final ImmutableMap<String, ResourceLocation> mainTextures;

    public final ImmutableMap<EnumParts, List<String>> allMeshNamesInModel;
    public final ResourceLocation objModel; // OBJ model

    public CustomArmorOBJData(NBTTagCompound nbtData) {
        allMeshNamesInModel = loadMeshNames(nbtData.getCompoundTag("OBJData"));
        objModel = new ResourceLocation(CustomNpcs.MODID, "models/armor/" + NoppesUtilServer.validNamespace(nbtData.getString("RegistryName")) + ".obj");
        Map<EntityEquipmentSlot, List<String>> visibleMeshesMap = new HashMap<>();
        Map<String, ResourceLocation> textureMap = new HashMap<>();
        Map<EntityEquipmentSlot, ParameterizedModel> stackMap = new HashMap<>();
        Map<EnumParts, ParameterizedModel> partMap = new HashMap<>();
        Map<EntityEquipmentSlot, Map<ItemCameraTransforms.TransformType, IModelState>> cameraMap = new HashMap<>();
        if (Util.instance.getSide() == Side.CLIENT) {
            // mainTextures
            ResourceLocation location = new ResourceLocation(objModel.getResourceDomain(), objModel.getResourcePath().replace(".obj", ".mtl"));
            Pattern WHITE_SPACE = Pattern.compile("\\s+");
            String material = "";
            try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location)) {
                try (BufferedReader mtlReader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String currentLine;
                    while ((currentLine = mtlReader.readLine()) != null) {
                        if (!currentLine.isEmpty() && !currentLine.startsWith("#")) {
                            String[] fields = WHITE_SPACE.split(currentLine, 2);
                            String key = fields[0];
                            String data = fields[1];
                            if (key.equalsIgnoreCase("newmtl")) { material = data; }
                            else if (key.equalsIgnoreCase("map_Kd") && !material.isEmpty()) {
                                if (data.contains(" ")) {
                                    String[] mapStrings = WHITE_SPACE.split(data);
                                    String texturePath = mapStrings[mapStrings.length - 1];
                                    if (!texturePath.toLowerCase().endsWith(".png")) { texturePath += ".png"; }
                                    textureMap.put(material, new ResourceLocation(NoppesUtilServer.validLocation(texturePath)));
                                }
                                else {
                                    if (!data.toLowerCase().endsWith(".png")) { data += ".png"; }
                                    textureMap.put(material, new ResourceLocation(NoppesUtilServer.validLocation(data)));
                                }
                                material = "";
                            }
                        }
                    }
                }
                catch (Exception ignored) {}
            }
            catch (Exception e) { LogWriter.error(e); }
            // camera transformations
            cameraMap = createCameraData(nbtData);
        }
        cameraData = ImmutableMap.copyOf(cameraMap);
        mainTextures = ImmutableMap.copyOf(textureMap);
        if (Util.instance.getSide() == Side.CLIENT) {
            // item models
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                if (slot != EntityEquipmentSlot.MAINHAND && slot != EntityEquipmentSlot.OFFHAND) {
                    List<String> visibleMeshes = new ArrayList<>();
                    if (slot == EntityEquipmentSlot.HEAD) {
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.HEAD));
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.MOHAWK));
                    }
                    else if (slot == EntityEquipmentSlot.CHEST) {
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.BODY));
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.ARM_LEFT));
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.ARM_RIGHT));
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.WRIST_LEFT));
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.WRIST_RIGHT));
                    }
                    else if (slot == EntityEquipmentSlot.LEGS) {
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.BELT));
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.LEG_LEFT));
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.LEG_RIGHT));
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.FEET_LEFT));
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.FEET_RIGHT));
                    }
                    else if (slot == EntityEquipmentSlot.FEET) {
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.FEET_LEFT));
                        visibleMeshes.addAll(allMeshNamesInModel.get(EnumParts.FEET_RIGHT));
                    }
                    visibleMeshesMap.put(slot, ImmutableList.copyOf(visibleMeshes));
                }
            }
            // part models
            partMap = loadDefaultParts();
        }
        visibleMeshesData = ImmutableMap.copyOf(visibleMeshesMap);
        partModelData = partMap;
        if (Util.instance.getSide() == Side.CLIENT) {
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                if (slot != EntityEquipmentSlot.MAINHAND && slot != EntityEquipmentSlot.OFFHAND) {
                    stackMap.put(slot, loadStackModel(visibleMeshesData.get(slot), null, slot));
                }
            }
        }
        stackModelData = ImmutableMap.copyOf(stackMap);
    }

    public static ModelBipedArmorOBJ getArmorModel(EntityEquipmentSlot slot) {
        ModelBipedArmorOBJ model = ARMOR_MODELS.get(slot);
        if (model == null) { ARMOR_MODELS.put(slot, model = new ModelBipedArmorOBJ(slot)); }
        return model;
    }

    private Map<EnumParts, ParameterizedModel> loadDefaultParts() {
        Map<EnumParts, ParameterizedModel> map = new HashMap<>();
        List<String> list = new ArrayList<>(allMeshNamesInModel.get(EnumParts.HEAD));
        list.addAll(allMeshNamesInModel.get(EnumParts.MOHAWK));
        map.put(EnumParts.HEAD, loadPartModel(list, null));

        map.put(EnumParts.BODY, loadPartModel(allMeshNamesInModel.get(EnumParts.BODY), null));

        map.put(EnumParts.ARM_RIGHT, loadPartModel(allMeshNamesInModel.get(EnumParts.ARM_RIGHT), null));
        map.put(EnumParts.WRIST_RIGHT, loadPartModel(allMeshNamesInModel.get(EnumParts.WRIST_RIGHT), null));
        list = new ArrayList<>(allMeshNamesInModel.get(EnumParts.ARM_RIGHT));
        list.addAll(allMeshNamesInModel.get(EnumParts.WRIST_RIGHT));
        map.put(EnumParts.ARM_RIGHT_FULL, loadPartModel(list, null));

        map.put(EnumParts.ARM_LEFT, loadPartModel(allMeshNamesInModel.get(EnumParts.ARM_LEFT), null));
        map.put(EnumParts.WRIST_LEFT, loadPartModel(allMeshNamesInModel.get(EnumParts.WRIST_LEFT), null));
        list = new ArrayList<>(allMeshNamesInModel.get(EnumParts.ARM_LEFT));
        list.addAll(allMeshNamesInModel.get(EnumParts.WRIST_LEFT));
        map.put(EnumParts.ARM_LEFT_FULL, loadPartModel(list, null))
        ;
        map.put(EnumParts.BELT, loadPartModel(allMeshNamesInModel.get(EnumParts.BELT), null));

        map.put(EnumParts.LEG_RIGHT, loadPartModel(allMeshNamesInModel.get(EnumParts.LEG_RIGHT), null));
        map.put(EnumParts.FOOT_RIGHT, loadPartModel(allMeshNamesInModel.get(EnumParts.FOOT_RIGHT), null));
        list = new ArrayList<>(allMeshNamesInModel.get(EnumParts.LEG_RIGHT));
        list.addAll(allMeshNamesInModel.get(EnumParts.FOOT_RIGHT));
        map.put(EnumParts.LEG_RIGHT_FULL, loadPartModel(list, null));

        map.put(EnumParts.LEG_LEFT, loadPartModel(allMeshNamesInModel.get(EnumParts.LEG_LEFT), null));
        map.put(EnumParts.FOOT_LEFT, loadPartModel(allMeshNamesInModel.get(EnumParts.FOOT_LEFT), null));
        list = new ArrayList<>(allMeshNamesInModel.get(EnumParts.LEG_LEFT));
        list.addAll(allMeshNamesInModel.get(EnumParts.FOOT_LEFT));
        map.put(EnumParts.LEG_LEFT_FULL, loadPartModel(allMeshNamesInModel.get(EnumParts.LEG_LEFT), null));

        map.put(EnumParts.FEET_RIGHT, loadPartModel(allMeshNamesInModel.get(EnumParts.FEET_RIGHT), null));
        map.put(EnumParts.FEET_LEFT, loadPartModel(allMeshNamesInModel.get(EnumParts.FEET_LEFT), null));
        return map;
    }

    private ImmutableMap<EnumParts, List<String>> loadMeshNames(NBTTagCompound data) {
        Map<EnumParts, List<String>> map = new HashMap<>();
        NBTTagList tagList = data.getTagList("Head Mesh Names", 8);
        List<String> listHead = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listHead.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.HEAD, listHead);
        map.put(EnumParts.MOHAWK, listHead);
        tagList = data.getTagList("Body Mesh Names", 8);
        List<String> listBody = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listBody.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.BODY, listBody);
        tagList = data.getTagList("Arm Right Mesh Names", 8);
        List<String> listArmRight = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listArmRight.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.ARM_RIGHT, listArmRight);
        tagList = data.getTagList("Wrist Right Mesh Names", 8);
        List<String> listWristRight = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listWristRight.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.WRIST_RIGHT, listWristRight);
        tagList = data.getTagList("Arm Left Mesh Names", 8);
        List<String> listArmLeft = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listArmLeft.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.ARM_LEFT, listArmLeft);
        tagList = data.getTagList("Wrist Left Mesh Names", 8);
        List<String> listWristLeft = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listWristLeft.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.WRIST_LEFT, listWristLeft);
        tagList = data.getTagList("Belt Mesh Names", 8);
        List<String> listBelt = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listBelt.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.BELT, listBelt);
        tagList = data.getTagList("Leg Right Mesh Names", 8);
        List<String> listLegRight = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listLegRight.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.LEG_RIGHT, listLegRight);
        tagList = data.getTagList("Foot Right Mesh Names", 8);
        List<String> listFootRight = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listFootRight.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.FOOT_RIGHT, listFootRight);
        tagList = data.getTagList("Leg Left Mesh Names", 8);
        List<String> listLegLeft = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listLegLeft.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.LEG_LEFT, listLegLeft);
        tagList = data.getTagList("Foot Left Mesh Names", 8);
        List<String> listFootLeft = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listFootLeft.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.FOOT_LEFT, listFootLeft);
        tagList = data.getTagList("Boot Right Mesh Names", 8);
        List<String> listBootRight = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listBootRight.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.FEET_RIGHT, listBootRight);
        tagList = data.getTagList("Boot Left Mesh Names", 8);
        List<String> listBootLeft = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) { listBootLeft.add(tagList.getStringTagAt(i)); }
        map.put(EnumParts.FEET_LEFT, listBootLeft);
        return ImmutableMap.copyOf(map);
    }

    private ParameterizedModel loadPartModel(List<String> visibleMeshes, Map<String, ResourceLocation> materialTextures) {
        ParameterizedModel iModel = new ParameterizedModel(objModel, visibleMeshes, materialTextures, false, 0, true);
        iModel.load(null);
        return iModel;
    }

    private ParameterizedModel loadStackModel(List<String> visibleMeshes, Map<String, ResourceLocation> materialTextures, EntityEquipmentSlot slot) {
        ParameterizedModel iModel = new ParameterizedModel(objModel, visibleMeshes, materialTextures, false, 0, false);
        iModel.load(getGUITransforms(slot));
        iModel.isItemStack = true;
        return iModel;
    }

    @SuppressWarnings("deprecation")
    private Map<EntityEquipmentSlot, Map<ItemCameraTransforms.TransformType, IModelState>> createCameraData(NBTTagCompound nbtData) {
        Map<EntityEquipmentSlot, Map<ItemCameraTransforms.TransformType, IModelState>> map = new HashMap<>();
        NBTTagCompound head = nbtData.getCompoundTag("Display").getCompoundTag("HEAD");
        NBTTagCompound chest = nbtData.getCompoundTag("Display").getCompoundTag("CHEST");
        NBTTagCompound legs = nbtData.getCompoundTag("Display").getCompoundTag("LEGS");
        NBTTagCompound feet = nbtData.getCompoundTag("Display").getCompoundTag("FEET");
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (slot != EntityEquipmentSlot.MAINHAND && slot != EntityEquipmentSlot.OFFHAND) {
                for (ItemCameraTransforms.TransformType transformType : ItemCameraTransforms.TransformType.values()) {
                    Vector3f rotation = new Vector3f();
                    Vector3f translation = new Vector3f();
                    Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);
                    switch(transformType) {
                        case THIRD_PERSON_LEFT_HAND: {
                            switch(slot) {
                                case CHEST: {
                                    if (!chest.hasKey("thirdperson_lefthand", 10)) {
                                        translation.z = 0.5f;
                                        scale.x = 0.5f;
                                        scale.y = 0.5f;
                                        scale.z = 0.5f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompoundTag("thirdperson_lefthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case LEGS: {
                                    if (!legs.hasKey("thirdperson_lefthand", 10)) {
                                        translation.x = -0.15f;
                                        translation.y = 0.35f;
                                        translation.z = 0.5f;
                                        scale.x = 0.65f;
                                        scale.y = 0.65f;
                                        scale.z = 0.65f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompoundTag("thirdperson_lefthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case FEET: {
                                    if (!feet.hasKey("thirdperson_lefthand", 10)) {
                                        rotation.x = 90.0f;
                                        rotation.y = 180.0f;
                                        translation.x = 1.15f;
                                        translation.y = 0.5f;
                                        translation.z = 0.5f;
                                        scale.x = 0.65f;
                                        scale.y = 0.65f;
                                        scale.z = 0.65f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompoundTag("thirdperson_lefthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                default: {
                                    if (!head.hasKey("thirdperson_lefthand", 10)) {
                                        rotation.y = 180.0f;
                                        translation.x = 1.0f;
                                        translation.y = -0.375f;
                                        translation.z = 0.5f;
                                        scale.x = 0.5f;
                                        scale.y = 0.5f;
                                        scale.z = 0.5f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, head.getCompoundTag("thirdperson_lefthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        case THIRD_PERSON_RIGHT_HAND: {
                            switch(slot) {
                                case CHEST: {
                                    if (!chest.hasKey("thirdperson_righthand", 10)) {
                                        translation.x = 0.5f;
                                        translation.z = 0.5f;
                                        scale.x = 0.5f;
                                        scale.y = 0.5f;
                                        scale.z = 0.5f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompoundTag("thirdperson_righthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case LEGS: {
                                    if (!legs.hasKey("thirdperson_righthand", 10)) {
                                        translation.x = 0.5f;
                                        translation.y = 0.35f;
                                        translation.z = 0.5f;
                                        scale.x = 0.65f;
                                        scale.y = 0.65f;
                                        scale.z = 0.65f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompoundTag("thirdperson_righthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case FEET: {
                                    if (!feet.hasKey("firstperson_righthand", 10)) {
                                        rotation.x = 90.0f;
                                        rotation.y = 180.0f;
                                        translation.x = 0.5f;
                                        translation.y = 0.5f;
                                        translation.z = 0.5f;
                                        scale.x = 0.65f;
                                        scale.y = 0.65f;
                                        scale.z = 0.65f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompoundTag("firstperson_righthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                default: {
                                    if (!head.hasKey("thirdperson_righthand", 10)) {
                                        rotation.y = 180.0f;
                                        translation.x = 0.5f;
                                        translation.y = -0.375f;
                                        translation.z = 0.5f;
                                        scale.x = 0.5f;
                                        scale.y = 0.5f;
                                        scale.z = 0.5f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, head.getCompoundTag("thirdperson_righthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        case FIRST_PERSON_LEFT_HAND: {
                            switch(slot) {
                                case CHEST: {
                                    if (!chest.hasKey("firstperson_lefthand", 10)) {
                                        rotation.y = 280.0f;
                                        translation.x = 0.57f;
                                        translation.y = 0.1f;
                                        translation.z = -0.085f;
                                        scale.x = 0.5f;
                                        scale.y = 0.5f;
                                        scale.z = 0.5f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompoundTag("firstperson_lefthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case LEGS: {
                                    if (!legs.hasKey("firstperson_lefthand", 10)) {
                                        rotation.y = 280.0f;
                                        translation.x = 0.65f;
                                        translation.y = 0.4f;
                                        translation.z = -0.085f;
                                        scale.x = 0.5f;
                                        scale.y = 0.5f;
                                        scale.z = 0.5f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompoundTag("firstperson_lefthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case FEET: {
                                    if (!feet.hasKey("firstperson_lefthand", 10)) {
                                        rotation.y = 280.0f;
                                        translation.x = 0.72f;
                                        translation.y = 0.435f;
                                        translation.z = -0.585f;
                                        scale.x = 0.85f;
                                        scale.y = 0.85f;
                                        scale.z = 0.85f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompoundTag("firstperson_lefthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                default: {
                                    if (!head.hasKey("firstperson_lefthand", 10)) {
                                        rotation.y = 280.0f;
                                        translation.x = 0.57f;
                                        translation.y = -0.225f;
                                        translation.z = -0.085f;
                                        scale.x = 0.5f;
                                        scale.y = 0.5f;
                                        scale.z = 0.5f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, head.getCompoundTag("firstperson_lefthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        case FIRST_PERSON_RIGHT_HAND: {
                            switch(slot) {
                                case CHEST: {
                                    if (!chest.hasKey("firstperson_righthand", 10)) {
                                        rotation.y = 280.0f;
                                        translation.x = 0.85f;
                                        translation.y = -0.1f;
                                        translation.z = 0.2f;
                                        scale.x = 0.6f;
                                        scale.y = 0.6f;
                                        scale.z = 0.6f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompoundTag("firstperson_righthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case LEGS: {
                                    if (!legs.hasKey("firstperson_righthand", 10)) {
                                        rotation.y = 280.0f;
                                        translation.x = 0.95f;
                                        translation.y = 0.25f;
                                        translation.z = 0.2f;
                                        scale.x = 0.6f;
                                        scale.y = 0.6f;
                                        scale.z = 0.6f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompoundTag("firstperson_righthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case FEET: {
                                    if (!feet.hasKey("firstperson_righthand", 10)) {
                                        rotation.y = 280.0f;
                                        translation.x = 0.95f;
                                        translation.y = 0.4f;
                                        translation.z = 0.2f;
                                        scale.x = 0.85f;
                                        scale.y = 0.85f;
                                        scale.z = 0.85f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompoundTag("firstperson_righthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                default: {
                                    if (!head.hasKey("firstperson_righthand", 10)) {
                                        rotation.y = 280.0f;
                                        translation.x = 0.85f;
                                        translation.y = -0.5f;
                                        translation.z = 0.2f;
                                        scale.x = 0.6f;
                                        scale.y = 0.6f;
                                        scale.z = 0.6f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, head.getCompoundTag("firstperson_righthand"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        case HEAD: {
                            switch(slot) {
                                case CHEST: {
                                    if (!chest.hasKey("head", 10)) {
                                        rotation.x = 270.0f;
                                        translation.x = 0.5f;
                                        translation.y = 1.0f;
                                        translation.z = 1.65f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompoundTag("head"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case LEGS: {
                                    if (!legs.hasKey("head", 10)) {
                                        rotation.x = 270.0f;
                                        translation.x = 0.5f;
                                        translation.y = 1.0f;
                                        translation.z = 1.0f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompoundTag("head"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case FEET: {
                                    if (!feet.hasKey("head", 10)) {
                                        rotation.y = 180.0f;
                                        translation.x = 0.5f;
                                        translation.y = 0.925f;
                                        translation.z = 0.4f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompoundTag("head"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                default: { break; }
                            }
                            break;
                        }
                        case GUI: {
                            switch(slot) {
                                case CHEST: {
                                    if (!chest.hasKey("gui", 10)) {
                                        rotation.x = 30.0f;
                                        rotation.y = 45.0f;
                                        translation.x = 0.49f;
                                        translation.y = -0.41f;
                                        scale.x = 0.9f;
                                        scale.y = 0.9f;
                                        scale.z = 0.9f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompoundTag("gui"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case LEGS: {
                                    if (!legs.hasKey("gui", 10)) {
                                        rotation.x = 30.0f;
                                        rotation.y = 45.0f;
                                        translation.x = 0.5f;
                                        translation.y = 0.05f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompoundTag("gui"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case FEET: {
                                    if (!feet.hasKey("gui", 10)) {
                                        rotation.x = 30.0f;
                                        rotation.y = 45.0f;
                                        translation.x = 0.5f;
                                        translation.y = 0.3f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompoundTag("gui"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                default: {
                                    if (!head.hasKey("gui", 10)) {
                                        rotation.x = 30.0f;
                                        rotation.y = 45.0f;
                                        translation.x = 0.5f;
                                        translation.y = -1.0f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, head.getCompoundTag("gui"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        case GROUND: {
                            switch(slot) {
                                case CHEST: {
                                    if (!chest.hasKey("ground", 10)) {
                                        translation.x = 0.5f;
                                        translation.z = 0.5f;
                                        scale.x = 0.5f;
                                        scale.y = 0.5f;
                                        scale.z = 0.5f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompoundTag("ground"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case LEGS: {
                                    if (!legs.hasKey("ground", 10)) {
                                        translation.x = 0.5f;
                                        translation.y = 0.25f;
                                        translation.z = 0.5f;
                                        scale.x = 0.6f;
                                        scale.y = 0.6f;
                                        scale.z = 0.6f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompoundTag("ground"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case FEET: {
                                    if (!feet.hasKey("ground", 10)) {
                                        translation.x = 0.5f;
                                        translation.y = 0.35f;
                                        translation.z = 0.5f;
                                        scale.x = 0.65f;
                                        scale.y = 0.65f;
                                        scale.z = 0.65f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompoundTag("ground"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                default: {
                                    if (!head.hasKey("ground", 10)) {
                                        translation.x = 0.5f;
                                        translation.y = -0.375f;
                                        translation.z = 0.5f;
                                        scale.x = 0.5f;
                                        scale.y = 0.5f;
                                        scale.z = 0.5f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, head.getCompoundTag("ground"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        case FIXED: {
                            switch(slot) {
                                case CHEST: {
                                    if (!chest.hasKey("fixed", 10)) {
                                        rotation.y = 180.0f;
                                        translation.x = 0.5f;
                                        translation.y = -0.65f;
                                        translation.z = 0.45f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, chest.getCompoundTag("fixed"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case LEGS: {
                                    if (!legs.hasKey("fixed", 10)) {
                                        rotation.y = 180.0f;
                                        translation.x = 0.5f;
                                        translation.y = 0.05f;
                                        translation.z = 0.475f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, legs.getCompoundTag("fixed"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                case FEET: {
                                    if (!feet.hasKey("fixed", 10)) {
                                        rotation.y = 180.0f;
                                        translation.x = 0.5f;
                                        translation.y = 0.2f;
                                        translation.z = 0.475f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, feet.getCompoundTag("fixed"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                                default: {
                                    if (!head.hasKey("fixed", 10)) {
                                        rotation.y = 180.0f;
                                        translation.x = 0.5f;
                                        translation.y = -0.85f;
                                        translation.z = 0.4f;
                                        scale.x = 0.75f;
                                        scale.y = 0.75f;
                                        scale.z = 0.75f;
                                    } else {
                                        Vector3f[] data = setOptional(rotation, translation, scale, head.getCompoundTag("fixed"));
                                        rotation = data[0];
                                        translation = data[1];
                                        scale = data[2];
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        default: { break; } // NONE
                    }
                    if (!map.containsKey(slot)) { map.put(slot, new HashMap<>()); }
                    map.get(slot).put(transformType, TRSRTransformation.from(new ItemTransformVec3f(rotation, translation, scale)));
                }
            }
        }
        return map;
    }

    private Vector3f[] setOptional(Vector3f rotation, Vector3f translation, Vector3f scale, NBTTagCompound compound) {
        if (compound.hasKey("rotation", 9)) {
            NBTTagList list = compound.getTagList("rotation", 5);
            if (list.tagCount() > 0) { rotation.x = list.getFloatAt(0); }
            if (list.tagCount() > 1) { rotation.y = list.getFloatAt(1); }
            if (list.tagCount() > 2) { rotation.z = list.getFloatAt(2); }
        }
        if (compound.hasKey("translation", 9)) {
            NBTTagList list = compound.getTagList("translation", 5);
            if (list.tagCount() > 0) { translation.x = list.getFloatAt(0); }
            if (list.tagCount() > 1) { translation.y = list.getFloatAt(1); }
            if (list.tagCount() > 2) { translation.z = list.getFloatAt(2); }
        }
        if (compound.hasKey("scale", 9)) {
            NBTTagList list = compound.getTagList("scale", 5);
            if (list.tagCount() > 0) { scale.x = list.getFloatAt(0); }
            if (list.tagCount() > 1) { scale.y = list.getFloatAt(1); }
            if (list.tagCount() > 2) { scale.z = list.getFloatAt(2); }
        }
        return new Vector3f[] { rotation, translation, scale };
    }

    public boolean isValid() { return objModel != null; }

    public @Nonnull IModelState getGUITransforms(@Nonnull EntityEquipmentSlot slot) {
        if (cameraData.containsKey(slot)) { return cameraData.get(slot).getOrDefault(ItemCameraTransforms.TransformType.GUI, TRSRTransformation.identity()); }
        return TRSRTransformation.identity();
    }

    public @Nullable IBakedModel getIBakedModel(@Nonnull EntityEquipmentSlot slot) {
        ParameterizedModel model = stackModelData.get(slot);
        if (model != null && model.objModel != null) { return model.getIBakedModel(); }
        return null;
    }

    public @Nullable ParameterizedModel getParameterizedModel(@Nonnull EnumParts part, @Nullable ItemStack stack,
                                                    @Nullable ResourceLocation mainLocation) {
        if (!isValid()) { return null; }
        ParameterizedModel model = null;
        boolean textureInStack = stack != null && stack.getItem().getRegistryName() != null &&
                stack.hasTagCompound() && stack.getTagCompound() != null &&
                stack.getTagCompound().hasKey("OBJTextures", 10);
        if (!textureInStack) { model = partModelData.get(part); }
        else {
            String key = getTextureKey(stack, mainLocation);
            if (!key.isEmpty()) {
                model = partModelCash.get(key);
                if (model == null) {
                    Map<String, ResourceLocation> materialTextures = new HashMap<>(mainTextures);
                    NBTTagCompound compound = stack.getTagCompound().getCompoundTag("OBJTextures");
                    for (String k : compound.getKeySet()) {
                        if (!k.isEmpty()) {
                            NBTBase tag = compound.getTag(k);
                            if (tag instanceof NBTTagString && !((NBTTagString) tag).getString().isEmpty()) {
                                materialTextures.put(k, new ResourceLocation(NoppesUtilServer.validLocation(((NBTTagString) tag).getString())));
                            }
                        }
                    }
                    model = loadPartModel(allMeshNamesInModel.get(part), materialTextures);
                    if (model.objModel != null) { partModelCash.put(key, model); }
                }
            }
        }
        return model;
    }

    public @Nullable ParameterizedModel getStackModel(@Nonnull ItemStack stack) {
        if (!(stack.getItem() instanceof CustomArmorOBJ) || !isValid()) { return null; }
        ParameterizedModel model = null;
        CustomArmorOBJ armor = (CustomArmorOBJ) stack.getItem();
        boolean textureInStack = stack.hasTagCompound() && stack.getTagCompound() != null && stack.getTagCompound().hasKey("OBJTextures", 10);
        if (!textureInStack || stack.getItem().getRegistryName() == null) { model = stackModelData.get(armor.getEquipmentSlot()); }
        else {
            String key = getTextureKey(stack, null);
            if (!key.isEmpty()) {
                model = partModelCash.get(key);
                if (model == null) {
                    Map<String, ResourceLocation> materialTextures = new HashMap<>(mainTextures);
                    NBTTagCompound compound = stack.getTagCompound().getCompoundTag("OBJTextures");
                    for (String k : compound.getKeySet()) {
                        if (!k.isEmpty()) {
                            NBTBase tag = compound.getTag(k);
                            if (tag instanceof NBTTagString && !((NBTTagString) tag).getString().isEmpty()) {
                                materialTextures.put(k, new ResourceLocation(NoppesUtilServer.validLocation(((NBTTagString) tag).getString())));
                            }
                        }
                    }
                    model = loadStackModel(visibleMeshesData.get(armor.getEquipmentSlot()), materialTextures, armor.getEquipmentSlot());
                    if (model.objModel != null) { partModelCash.put(key, model); }
                }
            }
        }
        return model;
    }

    private @Nonnull String getTextureKey(ItemStack stack, ResourceLocation mainLocation) {
        if (stack.getItem().getRegistryName() == null) { return ""; }
        StringBuilder key = new StringBuilder(stack.getItem().getRegistryName().toString());
        if (stack.hasTagCompound() && stack.getTagCompound() != null && stack.getTagCompound().hasKey("OBJTextures", 10)) {
            NBTTagCompound compound = stack.getTagCompound().getCompoundTag("OBJTextures");
            for (String k : new ArrayList<>(compound.getKeySet())) {
                if (!k.isEmpty()) {
                    NBTBase tag = compound.getTag(k);
                    if (tag instanceof NBTTagString && !((NBTTagString) tag).getString().isEmpty() ) {
                        key.append("_").append(k).append("=").append(NoppesUtilServer.validLocation(((NBTTagString) tag).getString()));
                    }
                }
            }
            if (key.length() == 0) { stack.getTagCompound().removeTag("OBJTextures"); }
        }
        else { key.append("_").append("all").append("=").append(mainLocation); }
        return key.toString();
    }


}
