package noppes.npcs.api.handler;

import noppes.npcs.api.IPos;
import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.handler.data.IBorder;
import noppes.npcs.controllers.data.Zone3D;

import java.util.List;

@SuppressWarnings("unused")
public interface IBorderHandler {

    IBorder createNew(@ParamName("dimensionId") String dimensionId, @ParamName("pos") IPos pos);

    IBorder[] getAllRegions();

    IBorder getRegion(@ParamName("regionId") int regionId);

    IBorder[] getRegions(@ParamName("dimensionId") String dimensionId);

    boolean removeRegion(@ParamName("regionId") int regionId);

    List<Zone3D> getNearestRegions(@ParamName("dimensionId") String dimensionId,
                                   @ParamName("x") double x, @ParamName("y") double y, @ParamName("z") double z,
                                   @ParamName("distance") double distance);

}