package noppes.npcs.controllers.scripts;

import noppes.npcs.api.block.IBlock;

public interface IScriptBlockHandler extends IScriptHandler {

   IBlock getBlock();

}
