package noppes.npcs.api.mixin.client.model.obj;

import net.minecraftforge.client.model.obj.ObjModel;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public interface IObjModelMixin {

    @SuppressWarnings("unused")
    int cnpcs$getColorMask();

    void cnpcs$setColorMask(int newColorMask);

    Map<String, ObjModel.ModelGroup> cnpcs$getParts();

    List<Vector3f> cnpcs$getNormals();

    void cnpcs$setNormals(List<Vector3f> newNormals);

}
